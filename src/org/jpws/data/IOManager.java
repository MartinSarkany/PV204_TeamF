/*
 *  IOManager in org.jpws.data
 *  file: IOManager.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 05.12.2007
 *  Version
 * 
 *  Copyright (c) 2007 by Wolfgang Keller, Munich, Germany
 * 
 This program is not freeware software but copyright protected to the author(s)
 stated above. However, you can use, redistribute and/or modify it under the terms 
 of the GNU General Public License as published by the Free Software Foundation, 
 version 2 of the License.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 Place - Suite 330, Boston, MA 02111-1307, USA, or go to
 http://www.gnu.org/copyleft/gpl.html.
 */

package org.jpws.data;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jpws.front.ActionHandler;
import org.jpws.front.DisplayManager;
import org.jpws.front.GUIService;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsFileFactory;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.exception.ApplicationFailureException;
import org.jpws.pwslib.exception.InvalidPassphraseException;
import org.jpws.pwslib.exception.PasswordSafeException;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.UUID;
import org.jpws.pwslib.persist.AbstractFTPAdapter;
import org.jpws.pwslib.persist.ApplicationAdapter;
import org.jpws.pwslib.persist.DefaultFilesystemAdapter;
import org.jpws.pwslib.persist.DefaultUrlAdapter;

/** IOManager has two principal functions: A) it regulates access to file IO on a
 *  generic level in order to prevent destructive conflicts, and B) it serves as 
 *  a cache for PWS database files and regulates access to their persistent states 
 *  (external files). In both cases IOManager prevents conflicts in a model 
 *  that differentiates between reading and writing access. Writing access demands
 *  exclusiveness while reading may share with other parallel reading access.
 * 
 * <p>The strategy to arrive at its functionality is to implement a proprietary locking
 * mechanism for incoming and outgoing IO-streams and supply subclasses for 
 * <code>ContextFile</code> and <code>PwsFile</code> which reorganise IO-functions
 * to check against the locking mechanism.
 * 
 *  <p>In order to function properly IOManager requires the application to use only 
 *  its <code>ContextFile</code> derivate for purposes of file-IO. This is achieved
 *  by various methods of <code>makeLocalContextFile()</code> branding or the generic 
 *  <code>makeContextFile(ApplicationAdapter, String)</code>. The most simple way to
 *  secure IOManager functionality is to call <code>getInpuStream(ContextFile)</code> and
 *  <code>getOutPutStream(ContextFile)</code> for ANY type of <code>ContextFile</code>.
 *  Additional service are the <cope>getTemporaryFile()</code> methods which also render
 *  access controlled files.
 * 
 * @author Wolfgang Keller
 *
 */
public class IOManager
{
   /** Waiting time for basic IO access (open file) when encountering access conflicts; in seconds. */ 
   private static final int IOSUCCESS_WAIT_SEC = 5;

   /** This is a map from realm ContextFile into realm IOM_PwsFile, wrapped into a weak reference. */
   private static HashMap<ContextFile, IOM_WeakReference> dbList = new HashMap<ContextFile, IOM_WeakReference>();
   
   /** This is a map from realm ContextFile into realm FileIOMarker. */
   private static HashMap<ContextFile, FileIOMarker> fList = new HashMap<ContextFile, FileIOMarker>();

   /** This is a list of context files to be deleted upon termination of IOManager. */
   private static ArrayList<ContextFile> delList = new ArrayList<ContextFile>();

   private static ApplicationAdapter localFileAdapter = DefaultFilesystemAdapter.get();
   private static ApplicationAdapter urlAdapter = DefaultUrlAdapter.get();
   private static ApplicationAdapter ftpAdapter = new FTPAdapter();
   private static ReferenceQueue<IOM_WeakReference> refQueue = new ReferenceQueue<IOM_WeakReference>();
   private static Thread wasteService; 
   private static MonitorPanel monitorPanel;

   static 
   {
      wasteService = new Thread( new WasteBucketService(), "IO-Manager Waste Bucket" );
      wasteService.setDaemon( true );
      wasteService.start();
   }
   
   /**
    * Inserts a new mapping from ContextFile to IOM_PwsFile in the
    * IO-cache and locks the file in IO-context. (The referenced 
    * object is kept as a weak reference.)
    * 
    * @param f ContextFile
    * @param dbf IOM_PwsFile
    */
   private static void putMapping ( ContextFile f, IOM_PwsFile dbf )
   {
      IOM_WeakReference ref;
      String path;
      
      if ( f == null | dbf == null )
         throw new NullPointerException();
      
      if ( !(f instanceof IOM_ContextFile) )
          f = new IOM_ContextFile(f);
      
      synchronized ( dbList )
      {
         // create new table entry for parameter f 
         ref = new IOM_WeakReference( f, dbf );
         dbList.put( f, ref );
         path = dbf.getFilePath();
         Log.log( 7, "(IOManager) inserting cache database mapping: " + path 
                  + " for " + dbf.getUUID().toString() );

         // install any Context dependent FILE LOCK
         try { dbf.getApplication().lockFileAccess( path ); } 
         catch (IOException e) 
         { e.printStackTrace(); }

         // perform some display in case there is a monitor panel out
         if ( monitorPanel != null ) {
      	   monitorPanel.update();
         }
      }
   }
   
   /**
    * Removes a mapping from ContextFile to IOM_PwsFile in the
    * IO-cache and unlocks file in IO-context. 
    * 
    * @param f ContextFile (may be <b>null</b>)
    * @throws IOException 
    */
   private static void removeMapping ( ContextFile f ) 
   {
       IOM_WeakReference ref;
       IOM_PwsFile dbf;
       
       synchronized ( dbList )
       {
           // remove table entry for parameter
           ref = dbList.remove(f);
           if ( ref != null ) 
           {
        	  String path = f.getFilepath();
              dbf = (IOM_PwsFile)ref.get();
              if ( dbf != null ) {
            	  Log.log( 7, "(IOManager) removing cache database mapping: " 
             		   + path + " for " + dbf.getUUID() );
              } else {
                  Log.log( 7, "(IOManager) removed cache database mapping (queue event) for: "
                		.concat( path ));
              }

              // remove any Context dependent FILE LOCK
              try { f.getAdapter().unlockFileAccess( f.getFilepath() ); } 
              catch (IOException e) 
              { e.printStackTrace(); }
              
              // perform some display in case there is a monitor panel out
              if ( monitorPanel != null ) {
           	   monitorPanel.update();
              }
           }
       }
   }
   
   /**
    * Moves the mapping of oldKey->DB to newKey->DB. Does nothing if 
    * such a mapping does not exist or newKey == <b>null</b>.
    *  
    * @param oldKey ContextFile key of mapping to database 
    * @param newKey ContextFile new key for mapping to database
   private static void modifyMapping ( ContextFile oldKey, ContextFile newKey )
   {
      IOM_PwsFile db;
      
      if ( newKey != null && !newKey.equals( oldKey ) && 
           (db = (IOM_PwsFile)getDatabase( oldKey )) != null )

      {
         Log.log( 7, "(IOManager) modifying database key, OLD = " + oldKey.getFilepath() 
               + ", NEW = " + newKey.getFilepath() );
         removeMapping( oldKey );
         putMapping( newKey, db );
      }
   }
    */

   /**
    * Returns the corresponding <code>IOM_PwsFile</code> object to the given key,
    * or <b>null</b> if such a mapping does not effectively exist. (Mappings may get 
    * lost spontaneously when there are no more strong references to a database.) 
    *  
    * @param f ContextFile the key
    * @return IOM_PwsFile referenced database object or <b>null</b>
    */
   private static IOM_PwsFile getMapping ( ContextFile f )
   {
      if ( f == null )
         throw new NullPointerException();
      
      synchronized ( dbList ) {
    	 IOM_WeakReference ref = dbList.get( f );
         if ( ref == null ) return null;
         
         IOM_PwsFile dbf = (IOM_PwsFile)ref.get();
         return dbf;
      }
   }
   
/**
 * Returns an open PWS database by directly addressing the underlying 
 * file format library and ignoring an already open cached instance of 
 * that file.
 * The denoted file is accessed
 * under the assumption that there already exists a persistent state 
 * of that database as a file.
 *  
 * @param f <code>ContextFile</code> identifier for a database
 * @param pass <code>PwsPassphrase</code>
 * @return the opened original <code>PwsFile</code> object 
 *
 * @throws NullPointerException if any param is undefined 
 * @throws FileNotFoundException if the specified file was not found or
 *         access was denied
 * @throws InvalidPassphraseException if file could not be opened with passphrase
 * @throws ApplicationFailureException if IO-context does not
 *         render an input stream
 * @throws IOException if an IO-error occurred
 */   
public static PwsFile getDirectAccessDatabase ( ContextFile f, PwsPassphrase pass ) 
       throws IOException, PasswordSafeException
{
   Log.log( 7, "(IOManager.getDirectAccessDatabase) - enter request for: ".concat( f.getFilepath() ) );

   // convert context file to our type
   if ( !(f instanceof IOM_ContextFile) )
      f = new IOM_ContextFile( f );
   
   return PwsFileFactory.loadFile( f, pass );
}   
   
/**
 * Returns an open PWS database corresponding to the parameter context file.
 * This method prefers to return a database from the cache of IOManager;
 * if unavailable the denoted file is opened by use of the parameter
 * passphrase under the assumption that there already exists a persistent state 
 * of that database as a file.
 * The returned database may already be in use by some other application task.
 * All operations on the database MUST therefore be synchronised on the
 * returned object. 
 *  
 * @param f <code>ContextFile</code> identifier for a database
 * @param pass
 * @return the opened, fully operable <code>PwsFile</code> object 
 *
 * @throws NullPointerException if any param is undefined 
 * @throws FileNotFoundException if the specified file was not found or
 *         access was denied
 * @throws InvalidPassphraseException if file could not be opened with passphrase
 * @throws ApplicationFailureException if IO-context does not
 *         render an input stream
 * @throws IOException if an IO-error occurred
 */   
public static PwsFile getOpenDatabase ( ContextFile f, PwsPassphrase pass ) 
throws IOException, PasswordSafeException
{
   IOM_PwsFile dbf;
   
   Log.log( 7, "(IOManager.getOpenDatabase) - enter request for: ".concat( f.getFilepath() ) );

   // convert context file to our type
   if ( !(f instanceof IOM_ContextFile) )
      f = new IOM_ContextFile( f );
   
   // look if database is already open
   if ( (dbf = (IOM_PwsFile)getDatabase( f )) == null )
   {
      // open database and register in map
      Log.log( 7, "(IOManager.getOpenDatabase) - loading/returning new database from FACTORY: ".concat( f.getFilepath() ) );
      dbf = new IOM_PwsFile( PwsFileFactory.loadFile( f, pass ) );
      putMapping( f, dbf ); 
   }
   else
      Log.log( 7, "(IOManager.getOpenDatabase) - returning CACHED database: ".concat( f.getFilepath() ) );
   return dbf;
}

/**
 * Returns an open PWS database from the cache of IOManager
 * or <b>null</b> if not listed.
 * The returned database may also be in use by some other application task.
 * All operations on the database MUST therefore be synchronised on the
 * returned object. 
 * 
 * @param f <code>ContextFile</code> identifier for a database; may be <b>null</b>
 * @return the opened, fully operable <code>PwsFile</code> object
 *         or <b>null</b> if not available
 */
public static PwsFile getDatabase ( ContextFile f )
{
   IOM_PwsFile file;
   
   if ( f == null )
      return null;
   
   // look if database exists in cache
   file = getMapping( f );
   Log.debug( 7, "(IOManager.getDatabase) lookup database was " + Boolean.toString( file != null ).toUpperCase() 
         + " for: " + f.getFilepath() );
   return file;
}

/**
 * Whether the parameter context file denotes a database which is inheld
 * within the IO db-cache.
 * 
 * @param f <code>ContextFile</code> identifier for a database
 * @return boolean <b>true</b> if and only if there is a database with the
 *         given resource definition listed in the IO-cache
 */
public static boolean hasDatabase ( ContextFile f )
{
   return getMapping( f ) != null;
}

/**
 * Saves a database absolutely which was registered before in the IO-Manager's cache.
 * Note that this method does not function if the parameter database has
 * not been registered before into the IO-manager via <code>getRegisteredDatabase()</code>
 * or <code>getOpenDatabase()</code>. 
 * 
 * @param db <code>PwsFile</code> database to save
 * 
 * @throws IllegalArgumentException if db has no persistent state defined        
 * @throws IllegalStateException if db is unknown in cache or not equal to cached instance       
 *          
 */
public static void saveRealDatabase ( PwsFile db ) throws IOException, ApplicationFailureException
{
   IOM_PwsFile db2;
   
   if ( !db.hasResource() )
      throw new IllegalArgumentException( "cannot save DB without resource definition" );
   
   // control for existence
   if ( (db2 = (IOM_PwsFile)getDatabase( db.getContextFile() )) == null )
   {
      throw new IllegalStateException( "database unknown in IO-cache: ".concat( db.getFilePath() ) ); 
   }
   
   // control identity
   if ( db2 != db )
   {
      throw new IllegalStateException( "database not same as in IO-cache: ".concat( db.getFilePath() ) ); 
   }
   
   Log.log( 5, "(IOManager) *** save absolute database: ".concat( db.getUUID().toString() )); 
   db2.saveIntern();
}

/**
 * Creates a copy of a database (absolutely) at a given external place. 
 * This method also functions if the parameter database has not been 
 * registered before into the IO-manager. 
 * 
 * @param db <code>PwsFile</code> database to copy
 * @param target <code>ContextFile</code> destination file specification
 * @param pass <code>PwsPassphrase</code> password used on the copy;
 *        may be <b>null</b> in which case the original's password is used
 * @param fileUUID <code>UUID</code> if not <b>null</b> the copy will assume
 *        this UUID value, otherwise keep the existing UUID  
 * @return UUID the uuid identifier of the target database (copy)
 * @throws IllegalArgumentException if db has no persistent state defined        
 * @throws IllegalStateException if db is unknown in cache or not equal to cached instance       
 *          
 */
public static void saveCopyDatabase ( PwsFile db, ContextFile target, 
									  PwsPassphrase pass, UUID fileUUID ) 
      throws IOException, ApplicationFailureException
{
   IOM_PwsFile db2;
   UUID uid;
   
   Log.log( 7, "(IOManager) enter saveCopyDatabase() for ".concat( db.getUUID().toString() ) );
   if ( target == null )
      throw new NullPointerException( "target file == null" );
   
   // control for target permission
   checkReservedFiles( target );
   
   if ( pass == null ) {
      pass = db.getPassphrase();
   }
   
   // COPY WITH IO-LOGGING
   
   // IOManager control of valid write access
   if ( !mayIO_Perform( target, false ) )
      throw new OperationLockedException( "IOManager: IO conflict database (writing)" );
   
   // mark Output IO operation 
   markIO( target, false, true );
   
   // if database is known in cache, take cached instance
   if ( (db2 = (IOM_PwsFile)getDatabase( db.getContextFile() )) != null )
      db = db2;
   
   try {
      // branch after class of file
      if ( db instanceof IOM_PwsFile ) {
         // save file copy VIA internal classes' method 
         uid = ((IOM_PwsFile)db).saveCopyIntern( target, pass, fileUUID );

      } else {
         // save file copy VIA original method 
         uid = db.saveCopy( target, pass, 0, fileUUID );
         Log.log( 5, "(IOManager) *** saved database copy in ORIGINAL mode: (copy UUID = " 
                  + uid.toString() + ") at " + target.getFilepath() ); 
      }

   } finally {
      // unmark Output IO operation 
      markIO( target, false, false );
   }
}

/**
 * Saves a database conditionally which was registered before in the IO-Manager's cache.
 * Other than "saveRealDatabase()", this method performs a save on persistent state only
 * if the given database has no representation in the display manager. 
 * Note that this method does not function if the parameter database has
 * not been registered before into the IO-manager via <code>getRegisteredDatabase()</code>
 * or <code>getOpenDatabase()</code>. 
 * 
 * @param db <code>PwsFile</code> database to save
 * 
 * @throws IllegalArgumentException if db has no persistent state defined        
 * @throws IllegalStateException if db is unknown in cache or not equal to cached instance       
 *          
 */
public static void saveDatabase ( PwsFile db ) throws IOException, ApplicationFailureException
{
   Log.log( 7, "(IOManager) enter saveDatabase()" );
   if ( !DisplayManager.hasDatabase( db.getUUID() ) )
      saveRealDatabase( db );
   else
      Log.log( 5, "(IOManager) *** save database IGNORED (because of display instance): "
            .concat( db.getUUID().toString() )); 
}

/**
 * Attempts to insert the parameter database into the 
 * IOManager database cache. This is systematically required
 * for newly created databases! If the database, identified by
 * its context file, is already a member of the cache,
 * nothing is done. Otherwise the parameter database is inserted.
 * In both cases the internal cached instance of <code>dbf</code>
 * is returned (which may be different to the given file). 
 *  
 * @param dbf <code>PwsFile</code> file to register into IOManager cache
 *        (file must have a persistent state defined)
 * @return <code>PwsFile</code> the cached version of the given database
 */
public static PwsFile getRegisteredDatabase ( PwsFile dbf ) throws IOException

{
   // control if database has an external file defined
   Log.log( 7, "(IOManager.getRegisteredDatabase) enter " );
   ContextFile file = dbf.getContextFile();
   if ( file == null ) {
      throw new IllegalArgumentException( "database must have persistent state defined" );
   }
      
   // if such a file is already listed, return it
   PwsFile cacheFile = getDatabase( file );
   if ( cacheFile != null ) {
      Log.log( 7, "(IOManager.getRegisteredDatabase) - returning CACHED database: ".concat( file.getFilepath() ) );

   // otherwise create internal file + insert into cache
   } else {
	  cacheFile = cacheDatabase(dbf);
   }
   return cacheFile;
}


/**
 * Inserts the parameter database into the 
 * IOManager database cache. This is systematically required
 * for newly created databases! If the database, identified by
 * its context file, is already a member of the cache,
 * the existing is replaced by the given one. 
 * The internal cached instance of <code>dbf</code>
 * is returned, which may be different to the given file. 
 *  
 * @param dbf <code>PwsFile</code> file to insert into IOManager cache
 *        (file must have a persistent state defined)
 * @return <code>PwsFile</code> the cached version of the given database
 * @throws IllegalArgumentException if given file has no resource definition
 */
public static PwsFile cacheDatabase ( PwsFile dbf ) 
{
   IOM_PwsFile cacheFile;
   
   // control if database has an external file defined
   if ( !dbf.hasResource() )
      throw new IllegalArgumentException( "database must have persistent state defined" );
      
   // create new cache-type database instance if necessary
   if ( dbf instanceof IOM_PwsFile ) {
      Log.log( 5, "(IOManager.cacheDatabase) caching + returning the given original IOM_PwsFile" );
      cacheFile = (IOM_PwsFile)dbf;
   } else {
      Log.log( 5, "(IOManager.cacheDatabase) caching + returning a new IOM_PwsFile" );
      cacheFile = new IOM_PwsFile( dbf ); 
   }
   
   // insert database into cache + return
   putMapping( dbf.getContextFile(), cacheFile );
   return cacheFile;
}

/**
 * Returns a new and empty instance of <code>PwsFile</code> in an IO-Manager 
 * version of the class (subclass).
 *  
 * @return PwsFile empty database
 */
public static PwsFile getNewDatabase ()
{
   Log.log( 7, "(IOManager) creating empty new IOM_PwsFile for EXTERN use" );
   return new IOM_PwsFile();
}

public static ApplicationAdapter getLocalFileAdapter ()
{
   return localFileAdapter;
}

public static ApplicationAdapter getFTPAdapter ()
{
   return ftpAdapter;
}

public static ApplicationAdapter getURLAdapter ()
{
   return urlAdapter;
}


/**
 * Creates a <code>ContextFile</code> from its constituent parameters.
 * Objects returned by this method are enabled for IOManager control 
 * of streaming conflicts (i.e. returned objects may throw exceptions
 * of type <code>IOManager.OperationLockedException</code>). 
 *  
 * @param adapter
 * @param filepath
 * @return <code>ContextFile</code>
 */
public static ContextFile makeContextFile ( ApplicationAdapter adapter, String filepath )
{
   return new IOM_ContextFile( adapter, filepath );
}

/**
 * Creates a <code>ContextFile</code> in the local file system 
 * from a java.io.File parameter.
 * Objects returned by this method are enabled for IOManager control 
 * of streaming conflicts (i.e. returned objects may throw exceptions
 * of type <code>IOManager.OperationLockedException</code>). 
 * If the parameter is <b>null</b> then <b>null</b> is returned.
 *  
 * @param file <code>java.io.File</code>; may be <b>null</b>
 * @return <code>ContextFile</code> or <b>null</b>
 */
public static ContextFile makeLocalContextFile ( File file )
{
   if ( file == null )
      return null;
   return makeLocalContextFile( file.getAbsolutePath() );
}

/**
 * Creates a <code>ContextFile</code> of the local files context and
 * a file path parameter.
 * Objects returned by this method are enabled for IOManager control 
 * of streaming conflicts (i.e. returned objects may throw exceptions
 * of type <code>IOManager.OperationLockedException</code>). 
 *  
 * @param filepath
 * @return <code>ContextFile</code>
 */
public static ContextFile makeLocalContextFile ( String filepath )
{
   return new IOM_ContextFile( localFileAdapter, filepath.replaceAll( "\\\\", "/" ) );
}

/**
 * Creates a <code>ContextFile</code> of the local files context and
 * two file path parameters, comprising a parent pathname and a child
 * pathname.
 * Objects returned by this method are enabled for IOManager control 
 * of streaming conflicts (i.e. returned objects may throw exceptions
 * of type <code>IOManager.OperationLockedException</code>). 
 *  
 * @param parent String first part pathname (e.g. directory); may be <b>null</b> for ignore
 * @param child String second part pathname (e.g. filename)
 * @return <code>ContextFile</code>
 */
public static ContextFile makeLocalContextFile ( String parent, String child )
{
   String filepath;
   
   filepath = new File ( parent, child ).getAbsolutePath();
   return makeLocalContextFile( filepath );
}

public static ContextFile makeLocalContextFile ( File parent, String child )
{
   return makeLocalContextFile( parent.getAbsolutePath(), child );
}

/**
 * Returns a context file in the local file system which may function as a 
 * temporary application file.
 * (Files returned here are NOT automatically removed when IOManager exits.)
 *  
 * @return <code>ContextFile</code> definition for a temporary file
 */
public static ContextFile getTemporaryFile () throws IOException
{
   return getTemporaryFile( null, null );
}

/**
 * Returns a context file in the local file system which may function as a 
 * temporary application file.
 * (Files returned here are NOT automatically removed when IOManager exits.)
 *
 * @param suffix String determines the file extention naming of the 
 *        returned file; may be <b>null</b> in which case ".tmp" is used  
 * @return
 */
public static ContextFile getTemporaryFile ( String suffix ) throws IOException
{
   return getTemporaryFile( suffix, null );
}

/**
 * Returns a context file in the local file system which may function as a 
 * temporary application file.
 * (Files returned here are NOT automatically removed when IOManager exits.)
 *
 * @param suffix String determines the file extention naming of the 
 *        returned file; may be <b>null</b> in which case ".tmp" is used  
 * @param dir target directory (local); may be <b>null</b> for default     
 * @return
 */
public static ContextFile getTemporaryFile ( String suffix, File dir ) throws IOException
{
   if ( dir != null )
      Util.ensureDirectory( dir, null );
   
   File f = File.createTempFile( "jpws-", suffix, dir );
   Log.debug( 9, "(IOManager.getTemporaryFile) creating temporary file: ".concat(
         f.getAbsolutePath() ));
   return makeLocalContextFile( f );
}

/** Returns the TEMP directory of the system or <b>null</b> if 
 * not available.
 * 
 * @return File directory or null
 */
public static File getTempDir () {
	String path = System.getProperty("java.io.tmpdir");
	File dir = new File(path);
	return dir.isDirectory() ? dir : null;
}
/**
 * Context files may be registered here for deletion of their persistent
 * state when JPWS application terminates.
 *  
 * @param f <code>ContextFile</code>
 */
public static void deleteOnExit ( ContextFile f )
{
   if ( f != null )
   synchronized ( delList ) {
	   
      if ( !delList.contains( f ) ) {
    	 // add to delete-on-exit file list
         delList.add( f );

         // perform display update if there is a monitor panel active
         if ( monitorPanel != null ) {
      	   monitorPanel.update();
         }
      }
   }
}

/**
 * Registers a local file for deletion when JPWS application terminates.
 *  
 * @param filepath <code>String </code> file denomination
 */
public static void deleteOnExit( String filepath ) 
{
   ContextFile cf = makeLocalContextFile(filepath);
   deleteOnExit(cf);
}

/**
 * Registers a local file for deletion when JPWS application terminates.
 *  
 * @param f <code>File</code>
 */
public static void deleteOnExit(File file) {
	deleteOnExit(file.getAbsolutePath());
}

/**
 * Throws an OperationLockedException if the parameter file denotes
 * a file which is a reserved member of the IO-cache.
 *  
 * @param f
 * @throws OperationLockedException
 */
private static void checkReservedFiles ( ContextFile f ) throws OperationLockedException
{
   if ( hasDatabase( f ) )
      throw new OperationLockedException( "IOManager: IO conflict (reserved file requested)" );
}

/**
 * Terminates functionality of IOManager.
 */
public static void exit ()
{
   // erase files noted in the delete-list
   synchronized ( delList )
   {
      for ( Iterator<ContextFile> it = delList.iterator(); it.hasNext(); ) {
         try { 
        	 ContextFile f=it.next();
            if ( f.exists() && f.delete() )
               Log.debug( 5, "(IOManager.exit) deleted scheduled file: " + f.getFilepath() );

         } catch ( Exception e ) {
            System.out.println( "*** IO-MANAGER: ERROR REMOVING FILE ***" );
            e.printStackTrace();
         }
      }
   }
}

/** Opens a new IO-Manager controlled input stream for the given context file. 
 * 
 * @param f ContextFile any context file
 * @return InputStream
 * @throws IOException
 */
public static InputStream getInputStream ( ContextFile f ) throws IOException
{
   // convert context file to our type
   if ( !(f instanceof IOM_ContextFile) )
      f = new IOM_ContextFile( f );
   
   return f.getInputStream();
}

/**
 * Whether the parameter context file is permitted to perform the given
 * IO-operation. Write access is generally disallowed for open databases.
 * 
 * @param f ContextFile the external file in question
 * @param reading boolean <b>true</b> == read access, <b>false</b> == write access
 * @return boolean <b>true</> if and only if the requested operation is permitted
 */
public static boolean access_allowed ( ContextFile f, boolean reading )
{
   return mayIO_Perform( f, reading ) && (reading || !hasDatabase( f ));  
}

private static boolean mayIO_Perform ( ContextFile f, boolean reading )
{
   if ( f == null )
      throw new NullPointerException();
   
   FileIOMarker marker = fList.get( f );
   if ( marker != null ) {
      return reading ? marker.writer == 0 : 
    	               (marker.readers == 0 & marker.writer == 0);
   }
   return true;
}

private static void markIO ( ContextFile f, boolean reading, boolean adding )
{
   if ( f == null )
      throw new NullPointerException();
   
   // ensure marker is mapped to file in fList
   FileIOMarker marker = fList.get( f );
   if ( marker == null ) {
      marker = new FileIOMarker();
      fList.put( f, marker );
   }

   // update marker
   if ( reading ) {
      marker.readers = Math.max(0, marker.readers + (adding ? 1 : -1));
   } else {
      marker.writer = adding ? 1 : 0;
   }

   // perform some display in case there is a monitor panel out
   if ( monitorPanel != null ) {
	   monitorPanel.update();
   }
}

/** Returns a JPanel with a text area displaying the current control contents
 * of the IOManager.
 * 
 * @return JPanel
 */
public static MonitorPanel getMonitorPanel () {
	if( monitorPanel == null ) {
		monitorPanel = new MonitorPanel();
	}
	return monitorPanel;
}

/** Opens a new IO-Manager controlled output stream for the given context file. 
 * 
 * @param f ContextFile any context file
 * @return OutputStream
 * @throws IOException
 */
public static OutputStream getOutputStream ( ContextFile f ) throws IOException
{
   // convert context file to our type
   if ( !(f instanceof IOM_ContextFile) )
      f = new IOM_ContextFile( f );
   
   return f.getOutputStream();
}

public static ApplicationAdapter getDefaultAdapter() {
    return localFileAdapter;
}

// *************  INNER CLASSES  *******************

private static class FileIOMarker
{
   int readers;
   int writer;
}

private static class IOM_ContextFile extends ContextFile
{

    private class CFInputStream extends FilterInputStream
   {
      private boolean closed;

      public CFInputStream ( InputStream in )
      {
         super( in );
      }

      @Override
	public void close () throws IOException
      {
         synchronized ( IOM_ContextFile.this )
         {
            try { 
            	super.close(); 
            } finally {
                if ( !closed ) {
                   // unmark this InputStream for IO operation 
                   markIO( IOM_ContextFile.this, true, false );
                   closed = true;
                }
            }
         }
      }
   }

   private class CFOutputStream extends FilterOutputStream
   {
      private boolean closed;
   
      public CFOutputStream ( OutputStream out ) {
         super( out );
      }
   
      @Override
      public void close () throws IOException {
         synchronized ( IOM_ContextFile.this )
         {
            try { 
            	super.close(); 
            } finally {
               if ( !closed ) {
                  // unmark this OutputStream for IO operation 
                  markIO( IOM_ContextFile.this, false, false );
                  closed = true;
               }
            }
         }
      }
   }


   /**
    * @param adapter
    * @param filepath
    */
   public IOM_ContextFile ( ApplicationAdapter adapter, String filepath )
   {
      super( adapter, filepath );
   }

   /**
    * @param adapter
    * @param filepath
    */
   public IOM_ContextFile ( ContextFile f )
   {
      super( f.getAdapter(), f.getFilepath() );
   }

   @Override
public synchronized InputStream getInputStream () throws IOException, ApplicationFailureException
   {
       OperationLockedException lockExc; 
       InputStream s;
       int loop;
      
       // perform loops over the IO-permission request until time-out
       // (loops in a one second interval)
       loop = 0;
       do {
          try {
             // IOManager control of valid read access
             if ( !mayIO_Perform( this, true ) )
                throw new OperationLockedException( "IOManager: IO conflict file (reading)" );
             lockExc = null;
          }
          catch ( OperationLockedException e ) 
          {
              lockExc = e;
              Util.sleep( 1000 );
          }
       } while ( lockExc != null & ++loop < IOSUCCESS_WAIT_SEC  );
       
       // throw the exception if no IO permission after wait time
       if ( lockExc != null )
       {
           throw lockExc;
       }
       
      // create our own output stream (to catch the close operation for lock release)
      s = new CFInputStream( super.getInputStream() );
      
      // mark InputStream for IO operation
      markIO( this, true, true );
      return s;
   }

   @Override
public synchronized OutputStream getOutputStream () throws IOException, ApplicationFailureException
   {
      OperationLockedException lockExc = null; 
      OutputStream s;
      int loop;
      
      // IOManager control of illegal file access
      checkReservedFiles( this );
      
      // perform loops over the IO-permission request until time-out
      // (loops in a one second interval)
      loop = 0;
      do {
         try {
             // IOManager control of valid write access
             if ( !mayIO_Perform( this, false ) )
                throw new OperationLockedException( "IOManager: IO conflict file (writing)" );

         } catch ( OperationLockedException e ) {
             lockExc = e;
             Util.sleep( 1000 );
         }
      } while ( lockExc != null & ++loop < IOSUCCESS_WAIT_SEC  );
      
      // throw the exception if no IO permission after wait time
      if ( lockExc != null ) {
          throw lockExc;
      }
      
      // create our own output stream (to catch the close operation for lock release)
      s = new CFOutputStream( super.getOutputStream() );
      
      // mark OutputStream for IO operation 
      markIO( this, false, true );
      return s;
   }

   @Override
public boolean canRead () throws IOException
   {
      return super.canRead() && mayIO_Perform( this, true );
   }

   @Override
public boolean canWrite () throws IOException
   {
      return super.canWrite() && mayIO_Perform( this, false );
   }

@Override
public boolean delete() throws IOException {
    if ( !access_allowed(this, false) ) {
        throw new OperationLockedException("file delete was blocked; it may be in use by some other task");
    }
    return super.delete();
}
   
}

@SuppressWarnings("serial")
public static class OperationLockedException extends IOException
{
   public OperationLockedException ()
   {
      super();
   }

   public OperationLockedException ( String s )
   {
      super( s );
   }
}

/** The WasteBucketService runs in a daemon loop and does some administration work
 * when weak references fall off from our cached database list. It keeps 
 * "dbList" tidy and takes care to revoke OS-based file-lockings for these events.
 *  
 */
private static class WasteBucketService implements Runnable
{
   

   @Override
   public void run ()
   {
      do {
         try {
        	IOM_WeakReference ref = (IOM_WeakReference)refQueue.remove(); 
        	String path = ref.key.getFilepath();
            Log.debug( 7, "(IOManager) waste bucket REFERENCE entry for ".concat( path ));
            removeMapping( ref.key );
//            synchronized ( dbList )
//            {
//               // release OS-borne file lock
//               try { ref.key.getAdapter().unlockFileAccess( path ); }
//               catch ( IOException e1 )
//               {
//                  e1.printStackTrace();
//               }
//               
//               // remove mapping in IO-cache
//               dbList.remove( ref.key );
//               Log.log( 7, "(IOManager) removed cache database mapping (queue event) for: ".concat( path ));
//            }
         }
         catch ( InterruptedException e )
         {}

      } while ( true );
   }
};

@SuppressWarnings("rawtypes")
private static class IOM_WeakReference extends WeakReference
{
   ContextFile key;

   /**
    * @param 
    * @param 
    */
   @SuppressWarnings("unchecked")
   public IOM_WeakReference ( ContextFile key, IOM_PwsFile dbf )
   {
      super( dbf, refQueue );
      this.key = key;
   }
}

private static class IOM_PwsFile extends PwsFile
{
   /** Constructs an empty PwsFile instance.
    */ 
   public IOM_PwsFile ()
   {}
   
   /** Constructs an instance by sharing content and identity
    * of the parameter PwsFile.
    * 
    * @param f PwsFile
    */ 
   public IOM_PwsFile ( PwsFile f )
   {
      replaceFrom( f );
      if ( !f.isModified() )
         resetModified();
   }
   
   /** IO-Manager owned save performance. */
   public synchronized void saveIntern () throws IOException, ApplicationFailureException
   {
      ContextFile cf = getContextFile(); 

      // IOManager control of valid write access
      if ( !mayIO_Perform( cf, false ) )
         throw new OperationLockedException( "IOManager: IO conflict database (writing)" );
      
      // mark Output IO operation 
      markIO( cf, false, true );
      
      try {
         // save file to output stream
         super.save();
         Log.debug( 5, "(IOManager) *** database saved (INTERN) to: ".concat( getFilePath() ));

      } catch ( IOException e ) { 
    	  throw e; 
      } finally {
         // unmark Output IO operation 
         markIO( cf, false, false );
      }
   }

   public UUID saveCopyIntern ( ContextFile target, PwsPassphrase pass, UUID fileUUID )
   throws IOException
   {
      UUID uid = super.saveCopy( target, pass, 0, fileUUID );
      Log.log( 5, "(IOManager) *** database copy (INTERN): (copy UUID = " + uid.toString() 
            + ") at " + target.getFilepath() ); 
      return uid;
   }

   // SHUNNED METHODS
   
   @Override
   public void save () throws IOException, ApplicationFailureException
   {
      throw new UnsupportedOperationException( "IO must perform through IO-Manager");
   }

   @Override
   public UUID saveCopy ( ContextFile target, PwsPassphrase pass, int format, UUID fileUUID )
		throws IOException, ApplicationFailureException
   {
      throw new UnsupportedOperationException( "IO must perform through IO-Manager");
   }

   @Override
   public UUID saveCopy ( ContextFile target, PwsPassphrase pass )
		throws IOException, ApplicationFailureException
   {
      throw new UnsupportedOperationException( "IO must perform through IO-Manager");
   }

   @Override
   public UUID saveCopy ( ContextFile target ) throws IOException, ApplicationFailureException
   {
      throw new UnsupportedOperationException( "IO must perform through IO-Manager");
   }

   @Override
   protected void finalize () throws Throwable
   {
      String key = getFilePath();
      if ( key == null ) key = "- noname -";
      Log.debug( 9, "--- finalizing IO-MANAGER database structure: ".concat( key ) );

      super.finalize();
   }

   @Override
   public ContextFile getContextFile ()
   {
      ContextFile cf = super.getContextFile();
      if ( cf != null && !(cf instanceof IOM_ContextFile) ) {
         cf = new IOM_ContextFile( cf );
      }
      return cf;
   }

    @Override
    public synchronized void setFilePath( String path ) 
    {
        ContextFile newFile;
        
        if ( path == null ) {
           newFile = null;
        } else {
           ApplicationAdapter adp = getApplication();
           if ( adp == null ) {
        	   adp = getDefaultAdapter();
           }
           newFile = new IOM_ContextFile( adp, path );
        }
        
        setPersistentFile( newFile );
    }
    
    @Override
    public synchronized void setPersistentFile( ContextFile f ) 
    {
        ContextFile newFile = f;
        ContextFile oldFile = getContextFile();
        super.setPersistentFile(f);

        // remove from table if new path is void
        if ( oldFile != null && !oldFile.equals(newFile) ) {
            removeMapping( oldFile );
        }
        // insert new reference into table 
        if ( newFile != null && !newFile.equals(oldFile) ) {
            putMapping( newFile, this );
        }
    }

}

/** This completes the abstract <code>AbstractFTPAdapter</code> of the PWSLIB 
 * library with a function to gain user login into FTP account.
 */
private static class FTPAdapter extends AbstractFTPAdapter
{
   
   @Override
   public String getUserLogin ( String domain )
   {
      String title = ResourceLoader.getDisplay( "dlg.connect.ftplogin" );
      String hstr = ResourceLoader.getDisplay( "msg.connect.ftplogin.request" );
      hstr = Util.substituteText( hstr, "$domain", domain );
      
      return GUIService.loginInput( null, title, hstr );
   }
}

public static class MonitorPanel extends JPanel 
{
	JTextArea ta = new JTextArea();
	JScrollPane scrp = new JScrollPane(ta);
	
	
	public MonitorPanel () {
		init();
		update();
	}
	
	private void init () {
		setLayout( new BorderLayout() );
		add(scrp);
		ta.setEditable(false);
	}
	
	public void update () {
		final StringBuffer sbuf = new StringBuffer();
		
		if ( !dbList.isEmpty() ) {
			// add all registered databases
			sbuf.append("\nRegistered PwsFiles\n\n");
			for (IOM_WeakReference wref : dbList.values()) {
				IOM_PwsFile file = (IOM_PwsFile)wref.get();
				if ( file != null ) {
					sbuf.append("   ");
					sbuf.append(file.getUUID());
					sbuf.append("  ");
					sbuf.append(file.getFilePath());
					sbuf.append("\n");
				}
			}
		}

		sbuf.append("\nIO File Markers\n\n");
		// add all registered databases
		for (Entry<ContextFile, FileIOMarker> entry : fList.entrySet()) {
			FileIOMarker ma = entry.getValue();
			sbuf.append("   rea ".concat(String.valueOf(ma.readers)));
			sbuf.append(", wrt ".concat(String.valueOf(ma.writer)));
			sbuf.append("  ");
			sbuf.append(entry.getKey().getFilepath());
			sbuf.append("\n");
		}
		
		if ( !delList.isEmpty() ) {
			// add all delete-on-exit registered files
			sbuf.append("\nDelete-On-Exit\n\n");
			for ( ContextFile f : delList ) {
				sbuf.append("  ");
				sbuf.append(f.getFilepath());
				sbuf.append("\n");
			}
		}

		// ActionHandler Thread Pool statistics
		sbuf.append("\n");
		sbuf.append(ActionHandler.getThreadPoolStatistics());
		sbuf.append("\n");
		
		if ( !isDisplayable() ) {
			// set text content
			ta.setText(sbuf.toString());
		} else {
		
			// execute panel update on EDT
			Runnable run = new Runnable() {
				@Override
				public void run() {
					ta.setText(sbuf.toString());
				}
			};
			ActionHandler.executeOnEDT(run);
		}
	}
	
}

/** Returns a context file under the control of the IOManager.
 * 
 * @param file ContextFile
 * @return ContextFile
 */
public static ContextFile makeContextFile(ContextFile file) {
	if ( !(file instanceof IOM_ContextFile) ) 
		return new IOM_ContextFile(file);
	return file;
}

}
