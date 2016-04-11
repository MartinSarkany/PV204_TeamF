/*
 *  PwsFileSocket in org.jpws.data
 *  file: PwsFileSocket.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 16.11.2005
 *  Version
 * 
 *  Copyright (c) 2011 by Wolfgang Keller, Munich, Germany
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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeListener;

import org.jpws.front.GUIService;
import org.jpws.front.Global;
import org.jpws.front.util.RecentList;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.crypto.PwsCipher;
import org.jpws.pwslib.crypto.TwofishCipher;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.HeaderFieldList;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsFileEvent;
import org.jpws.pwslib.data.PwsFileHeaderV3;
import org.jpws.pwslib.data.PwsFileListener;
import org.jpws.pwslib.data.PwsIgDupRecordList;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsRawField;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.data.PwsRecordList;
import org.jpws.pwslib.exception.DuplicateEntryException;
import org.jpws.pwslib.exception.NoSuchRecordException;
import org.jpws.pwslib.exception.PasswordSafeException;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.UUID;
import org.jpws.pwslib.order.DefaultRecordWrapper;
import org.jpws.pwslib.order.OrderedRecordList;
import org.jpws.pwslib.order.OrderedRecordList.RecordSelector;
import org.jpws.pwslib.persist.ApplicationAdapter;

/**
 * A containment structure for a <code>PwsFile</code> to deal with backend tasks.
 * <p>This class serves several primary purposes: 
 * <p>A) synchronize multi-threaded operations on a database, 
 * <br>B) organise a sorted and filtered representation of the record list
 * <br>C) make available several backend operations not present on the lean 
 * database interface
 * <br>D) organise automatic backup copies of the database
 * <br>E) implement functionality to dispatch events about property changes of 
 * the container or file events (modify event)   
 * <br>F) implement file specific persistent options interface (<code>getOptions()</code>)
 *    
 *  <P>This class dispatches events of type <code>ChangeEvent</code>. The static member
 *  class of this class or the Java class <code>javax.swing.event.ChangeEvent</code> can be used 
 *  to receive  events. Only the member class, however, allows to identify the modified container
 *  property or event type. The Java interface  <code>javax.swing.event.ChangeListener</code> is 
 *  used for listeners. There are two event types dispatched from this class:
 *  <BR>MODIFY_EVENT : dispatched on each modification of the content or definition of the database;
 *                     also dispatched when the file becomes "unmodified" (e.g. save performed)
 *  <BR>FILTER_MODE : dispatched when property "getFilterStatus()" changes to a new state
 *  <BR>As a general rule, all event types are initially dispatched upon registration of a listener.  
 */
public class PwsFileSocket implements OptionChangeListener
{
   /** Property identifier for filtering mode. */
   public static final int FILTER_MODE = 15;
   /** Identifier for a modify event. */
   public static final int MODIFY_EVENT = 16;

   // Filter Mode values
   public static final int FILTER_OFF = 0;
   public static final int FILTER_EXPIRING = 1;
   public static final int FILTER_IMPORTED = 2;
   public static final int FILTER_MODIFIED = 3;
   public static final int FILTER_INVALID = 4;
   
   public static final int HEADERFIELD_DBNAME = PwsFileHeaderV3.FILE_NAME_TYPE;
   public static final int HEADERFIELD_DBDESCRIPT = PwsFileHeaderV3.FILE_DESCRIPTION_TYPE;
   public static final int HEADERFIELD_TREEINFO = PwsFileHeaderV3.TREE_DISPLAY_TYPE;
   
   private static final String FILEOPTION_TOKEN = "JPWS-OPTIONS=";
   private static final String FILEMODIFY_NUMBER = "FILEMODIFYNUMBER";

   private Preferences          systemPrefs = Preferences.userRoot().node( "/org/jpws/data/fileoptions" );
   private ArrayList<ChangeListener>  changeListeners = new ArrayList<ChangeListener>();
   protected BackupManager      backupManager = new BackupManager();
   private FileListener         fileListener;
   private FileOptions          majorOptions;
   private FileOptions          minorOptions;
   private String               optionString;
   private URL                  url;
   private PwsCipher			sysPrefCipher;

   private int                  filterOption;
   private int                  fileModNumber;
   private int                  reportedFilterMode;
   private long                 lastSaveTime;
   private long                 expireScope;
   private boolean              isOpen;
   private boolean              modified;
   private boolean              eventPause;
   private boolean              findFilterActive;
   protected boolean            searchCaseSensitive;
   protected boolean            searchWholeWords;

   protected PwsFile            dbf;
   private String               findFilterText;
   private OrderedRecordList    sortList;
   private String               logicalName;
   
/**
 *   Create a containment for the parameter <code>PwsFile</code>.
 */
public PwsFileSocket ( PwsFile file )
{
   if ( file == null )
      throw new NullPointerException();
   
   dbf = file;
   init();
   
// THIS MAY BE SWITCHED ON FOR TESTING OF FILE STATE REPORTING   
//   addChangeListener( new EventReporter() );
}

private void init ()
{
   setEventPause(true);

   logicalName = getHeaderValue( HEADERFIELD_DBNAME );
   majorOptions = new FileOptions(true);
   minorOptions = new FileOptions(Options.isOptionSet("storeMinorChanges"));
   
   getUrl();
   initFileOptions();
   initSortLists();
   
   int h = Options.getIntOption( "findTextOpt" );
   searchCaseSensitive = (h & 1) == 1;
   searchWholeWords = (h & 2) == 2;
   Options.addChangeListener( this );
   
   // listener to database file
   fileListener = new FileListener();
   dbf.addFileListener( fileListener );

   setExpireScope(Options.getLongOption( "expireScope" ));
   lastSaveTime = System.currentTimeMillis();
   isOpen = true;

   backupManager.refresh( getContextFile() );
   setEventPause(false);
}  // init

/** Retrieves data for the two file option objects (<code>FileOption</code>)
 * in this socket. These are MINOR and MAJOR options. (This does not update 
 * affiliated service structures.) 
 */
private void initFileOptions ()
{
   int end;

   // load MAJOR option string from database
   optionString = dbf.getUserOptions();
//   Log.debug(10, "(PwsFileSocket.initFileOptions) - Initial DBF UserOptions: [" + optionString + "]"  );

   String token = FILEOPTION_TOKEN;
   int start = optionString.indexOf( token );
   if ( start > -1 ) {
      try { 
         // isolate major options string 
         end = Util.CSV.searchQuoteEnd( optionString, start + token.length() +1 );
         String optstr = optionString.substring( start + token.length(), end );
         optstr = Util.CSV.unquoteText( optstr );
         Log.debug(6, "(PwsFileSocket.initFileOptions) -- Major File Options unquoted: [" + optstr + "]" );
         
         // initialise major option bag 
         majorOptions.load( optstr );

      } catch ( Exception e ) {
         end = -1;
         e.printStackTrace();
         GUIService.warningMessage( null, null, 
               "<html>Encountered corrupted file options in: " 
               + getFileName() + "<br>" + e );
      }

      // cut away JPWS-OPTIONS from dbf user options string (memorise)
      String hstr = optionString.substring( 0, start );
      if ( end != -1 ) {
         hstr += optionString.substring( end );
      }
      optionString = hstr;
//      Log.debug(10, "(PwsFileSocket.initFileOptions) - Remaining DBF UserOptions: [" + optionString + "]" );
   }
   
   // override last-used-entries from a special database value
   String hstr = dbf.getRecentUsedEntries();
   if ( hstr != null ) {
  	 boolean overwritten = majorOptions.setOption("lastUsedEntries", hstr);
  	 if (overwritten) {
  		Log.debug(3, "-- Last-Used-Entries value overwritten with DBF header field: [" + hstr + "]" );
  	 }
   }
   
   majorOptions.resetModified();

   // create cipher for minor option store in system preferences
   if ( sysPrefCipher == null ) {
	   try {
		   // obtain key material from major options or create new if unavailable
		   String cipherKey = majorOptions.getOption("SYSPREFCIPHERKEY");
		   byte[] ckey;

		   if ( cipherKey.isEmpty() ) {
			   // create a new cipher key and store in major options
			   ckey = Util.getCryptoRand().nextBytes(32);
			   cipherKey = Util.bytesToHex(ckey);
			   majorOptions.setOption("SYSPREFCIPHERKEY", cipherKey);

		   } else {
			   // read the stored cipher key 
			   ckey = Util.hexToBytes(cipherKey);
		   }
	
		   // create cipher instance
		   sysPrefCipher = new TwofishCipher(ckey);
	       Log.debug(10, "(PwsFileSocket.initFileOptions) - Cipher for SYSTEM PREFERENCES created [" + cipherKey + "]" );
		   
	   } catch ( Exception e ) {
		   e.printStackTrace();
	       Log.debug(10, "(PwsFileSocket.initFileOptions) *** FAILED: cipher creation for SYSTEM PREFERENCES ***" );
	   }
   }
   
   // initialise the MINOR option bag (various sources)
   retrieveMinorOptions();
   minorOptions.resetModified();

   // initialise data structures 
   fileModNumber = majorOptions.getIntOption(FILEMODIFY_NUMBER);
}  // initFileOptions

/** Transfers the value of the given option key from MAYOR to MINOR options.
 * 
 * @param token String
 */
private void transferMajorToMinor (String token) {
	String hstr = majorOptions.getOption(token);
	minorOptions.setOption(token, hstr);
}

/** Retrieves MINOR file options from file data or system preferences 
 * (depending on global options and circumstance).
 */
private void retrieveMinorOptions() {

   // read property list from local memory
   String key = getUUID().toHexString().toUpperCase();
   String value = systemPrefs.get(key, "");

   if ( value.isEmpty() )
   Log.debug(6, "(PwsFileSocket.retrieveMinorOptions) SYSTEM PREFERENCES value was unavailable ");
   if ( sysPrefCipher == null  )
   Log.debug(6, "(PwsFileSocket.retrieveMinorOptions) SYS PREF cipher not set up");
   
   // decrypt value with system pref cipher
   if ( !value.isEmpty() && sysPrefCipher != null ) {
	   try {
		   byte[] buf = Util.hexToBytes(value);
		   buf = sysPrefCipher.decrypt(buf);
		   int len = Util.readIntLittle(buf, 0);
		   value = new String( buf, 4, len, "UTF-8");
		   
		   // load minor options
		   minorOptions.load(value);

	   } catch ( Exception e ) {
		   e.printStackTrace();
	       Log.debug(10, "(PwsFileSocket.retrieveMinorOptions) *** FAILED: decryption of SYSTEM PREFERENCES value: "
	    		   .concat(value));
	   }
   }
   
	// take minor options from mayor options if mayor store-time is younger 
    // or minor is empty
    boolean mayorVersionIsYounger = minorOptions.getStoreTime() < majorOptions.getStoreTime();
	if ( mayorVersionIsYounger ) {
		minorOptions.clear();
		transferMajorToMinor("lastUsedEntries");
		transferMajorToMinor("recentFindTexts");
		transferMajorToMinor("viewType");
		transferMajorToMinor("favouriteViewType");
		transferMajorToMinor("importfile.recent");
		transferMajorToMinor("import.targetgroup");
		transferMajorToMinor("import.charset");
		transferMajorToMinor("import.format");
		transferMajorToMinor("export.charset");
		transferMajorToMinor("export.format");
		transferMajorToMinor("backupPath");
		transferMajorToMinor("treeExpansionString");
		transferMajorToMinor("selectedRecord");
		transferMajorToMinor("editor-floatingbar-active");
		transferMajorToMinor("userColumnModel");
		Rectangle bounds = majorOptions.getBounds("dialogFloatingBarBounds");
		minorOptions.setBounds("dialogFloatingBarBounds", bounds);
		Log.debug(5, "(PwsFileSocket.retrieveMinorOptions) minor file options taken from mayor options, key="
				   + key );
	} else {
		Log.debug(5, "(PwsFileSocket.retrieveMinorOptions) minor file options loaded from SYSTEM PREFS:\n   "
				   + key + " = [" + value + "]");
	}
}

/** Creates new sorting objects for this container and reloads the database.
 */
private void initSortLists ()
{
   RecordFilter filter = new RecordFilter();
   sortList = new OrderedRecordList( dbf );
   sortList.setRecordSelector(filter);
   sortList.loadDatabase( dbf, Options.getLongOption( "expireScope" ) );
}

/**
 *  Adds a change listener to this container.
 * 
 * @param li javax.swing.event.ChangeListener
 * @return <b>true</b> if and only if the parameter object was added to 
 *         the list of listeners 
 */
public boolean addChangeListener ( ChangeListener li )
{
   synchronized( changeListeners ) {
      if ( !changeListeners.contains( li ) ) {
         changeListeners.add( li );
         li.stateChanged( new ChangeEvent( this, FILTER_MODE ) );
         li.stateChanged( new ChangeEvent( this, MODIFY_EVENT ) );
         return true;
      }
      return false;
   }
}

/**
 *  Removes a change listener from this container.
 * 
 * @param li javax.swing.event.ChangeListener
 */
public void removeChangeListener ( ChangeListener li )
{
   synchronized( changeListeners ) { 
	   changeListeners.remove( li ); 
   }
}

/**
 * Fires an event of the specified type to the list of registered change listeners.
 * 
 * @param property type of event to be issued
 */
@SuppressWarnings("unchecked")
protected void fireChangeEvent ( int property )
{
   if ( eventPause ) return;
   
   synchronized ( changeListeners ) {
	   ArrayList<ChangeListener> clients = (ArrayList<ChangeListener>)
			                     changeListeners.clone();
      ChangeEvent evt = new ChangeEvent( this, property );
      for ( ChangeListener i : clients )
         i.stateChanged( evt );
   }
}

/**
 * This is called by PwsFileSocket after it has processed a PWS file event. This 
 * method  is meant for subclasses to participate in event handling. Does nothing
 * in PwsFileSocket.
 * 
 * @param evt PwsFileEvent
 */
protected void processFileEvent ( PwsFileEvent evt )
{
}

protected void setRecordFilter ( RecordFilter filter )
{
   sortList.setRecordSelector( filter );
   sortList.refresh();
}

/** Whether the container has not been closed. */
public boolean isOpen ()
{
   return isOpen;
}

/** Whether the database has a persistency state definition. */
public boolean hasPersistentFile ()
{
   return dbf.hasResource();
}

/** The character encoding used in this database. */
public String getCharset ()
{
   return dbf.getCharset();
}

/** The persistent file format used in this database as a mnemonic text string. 
  * @since 0-4-0
  */
public String getFileFormatText ()
{
   return getFileFormatText( dbf.getFormatVersion() );
}

/** 
 * Returns the parameter persistent file format as a mnemonic text string. 
 * 
 * @param format
 * @return String
 * @since 0-4-0
*/
public static final String getFileFormatText ( int format )
{
   if ( format > 0 & format <= Global.FILEVERSION_LATEST )
      return "PW" + format;
   return null;
}

/** The persistent file format used in this database as a mnemonic text string. 
 * @since 0-6-0
 */
public String getFullFileFormatText ()
{
   Dimension fo = dbf.getFileFormat();
   return fo.width + "." + fo.height;
}

/** The persistent file format used in this database. 
 * 
 * @return int file format code
 * @since 0-4-0
 */
public int getFileFormat ()
{
   return dbf.getFormatVersion();
}

/** The full persistent file format used in this database. 
 * 
 * @return Dimension file format code, width = major, height = minor
 * @since 0-6-0
 */
public Dimension getFullFileFormat ()
{
   return dbf.getFileFormat();
}

/** Sets a new file path for this socket's database.
 *  
 * @param path the name for the file, may be <b>null</b> but not empty
 * @since 0-5-0
 */ 
public void setFilePath ( String path )
{
   synchronized ( dbf )
   {
      dbf.setFilePath( path );
      backupManager.refresh( getContextFile() );
   }
}

/** Sets the format version of the persistent state of this file.
 * @param format int format code as from Global
 * @since 0-4-0
 */
public void setFileFormat ( int format )
{
   dbf.setFormatVersion( format );
}

/** The number of unknown or extra fields in this database.
 * @since 0-4-0
 */ 
public int getUKFCount ()
{
   return dbf.getUnknownFieldCount();
}

/** The total data size of all unknown or extra fields in this database.
 *  @return long unknown data size
 * @since 0-4-0
 */ 
public long getUKFSize ()
{
   return dbf.getUnknownFieldSize();
}

/** Returns MAJOR file options.
 * 
 * @return <code>PersistentOptions</code>
 */
public PersistentOptions getMajorOptions ()
{
   return majorOptions;
}

/** Returns MINOR file options. These options are meant to not necessarily
 * constitute a file modification event when modified.
 * <p>Minor file options are not necessarily stored into the database, they 
 * may also persist on a local platform basis. The user can choose in a progam
 * option on where minor options are to be stored. 
 * 
 * @return <code>PersistentOptions</code>
 */
public PersistentOptions getMinorOptions ()
{
   return minorOptions;
}

/** Updates the specified record in this file if and only if it is already
 *  an element of this file and it is semantically valid.
 * 
 *  @param rec record to be updated in database 
 *  @throws NoSuchRecordException if the parameter record is unknown
 *  @throws IllegalArgumentException if the record is not valid
 */
public void updateRecord ( PwsRecord rec ) throws NoSuchRecordException
{
   synchronized ( dbf ) {
      dbf.updateRecordValid( rec );
   }
}

/** Updates the specified record in this file if and only if it is already
 *  an element of this file, but without controling record validity.
 * 
 *  @param rec record to be updated in database 
 *  @throws NoSuchRecordException if the parameter record is unknown
 */
public void updateRecordRelaxed ( PwsRecord rec ) throws NoSuchRecordException
{
   synchronized ( dbf ) {
      dbf.updateRecord( rec ); 
   }
}



/** Updates a set of records in this file as given by the parameter record list
 *  and returns unknown elements in another list.
 * 
 *  @param list <code>PwsRecordList</code>
 *  @return  <code>PwsRecordList</code> list of unknown records 
 *           (subset of input list)
 * @since 0-5-0
 */
public PwsRecordList updateRecordList ( PwsRecordList list ) 
{
   synchronized ( dbf ) {
      return dbf.updateRecordList( list );
   }
}

/** Updates a set of records, represented by an array of record wrappers, 
 *  in this file and returns unknown elements in another list.
 * 
 * @param records array of <code>DefaultRecordWrapper</code>
 * @return  <code>PwsRecordList</code> list of unknown records 
 *          (subset of input list)
 * @throws DuplicateEntryException 
 * @since 0-5-0
 */
public PwsRecordList updateRecordList ( DefaultRecordWrapper[] records ) 
		throws DuplicateEntryException 
{
   synchronized ( dbf ) {
      return dbf.updateRecordList( new PwsRecordList( records ) );
   }
}

/** Adds a record to this file. 
 * 
 * @param rec
 * @throws DuplicateEntryException if this record already exists in this file
 * @throws IllegalArgumentException if this record is not valid
 */
public void addRecord ( PwsRecord rec ) throws DuplicateEntryException
{
   synchronized ( dbf ) {
      dbf.addRecordValid( rec );
   }
}

/** Adds a record to this file without control of its
 * semantical validity. There is, however, control of uniqueness of its UUID. 
 * 
 * @param rec
 * @throws DuplicateEntryException if this record already exists in this file
 * @throws IllegalArgumentException if this record is not valid
 */
public void addRecordRelaxed ( PwsRecord rec ) throws DuplicateEntryException
{
   synchronized ( dbf ) {
      dbf.addRecord( rec );
   }
}

/** Returns a duplicate record of the parameter record. 
 *  The resulting record is a clone with a new UUID value
 *  and thus takes on a different identity. Create and 
 *  modify time fields are set to the current system time. 
 *  
 * @param rec record to be duplicated
 * @return <code>PwsRecord</code>
 */
public static PwsRecord duplicateRecord ( PwsRecord rec )
{
   PwsRecord r2 = rec.copy();
   long t = System.currentTimeMillis();
   r2.setCreateTime( t );
   r2.setModifyTime( t );
   return r2;
}

/** Returns a set of record duplicates. The duplicates bear a new UUID
 *  and have create and modify time set to current time. 
 * 
 * @param records <code>DefaultRecordWrapper[]</code> 
 * @return DefaultRecordWrapper[]
 * @since 0-5-0
 */
public static DefaultRecordWrapper[] duplicateRecords ( DefaultRecordWrapper[] records )
{
   // create array of duplicated records
   DefaultRecordWrapper[] duplRecs = new DefaultRecordWrapper[ records.length ];
   for ( int i = 0; i < records.length; i++ ) {
	  DefaultRecordWrapper wrap = records[i];
      PwsRecord rec = duplicateRecord( wrap.getRecord() );
      duplRecs[i] = new DefaultRecordWrapper( rec, wrap.getLocale() );
   }
   return duplRecs;
}

/**
 * Returns an array of record duplicates corresponding to the set of records
 * which is defined by the <code>group</code> parameter. (The resulting
 * records will bear a new UUID and updated create and modified time.)
 *  
 * @param group record group to be duplicated
 * @return array of <code>PwsRecord</code>
 * @since 0-5-0 
 */
public PwsRecord[] groupDuplicates ( String group )
{
   DefaultRecordWrapper[] recs = getGroupRecords( group );
   PwsRecord[] dups = new PwsRecord[recs.length];
   for ( int i = 0; i < recs.length; i++ ) {
	  PwsRecord record = duplicateRecord( recs[i].getRecord() );
      dups[i] = record;
   }
   return dups;
}

/**
 * Assigns a single new GROUP value to all elements of the parameter record
 * array. The assigned value is the second parameter's value if it
 * is NOT already present as GROUP in this container, otherwise a variant 
 * of this value is created and used. 
 * 
 * @param recs PwsRecord[] records to be modified
 * @param group base GROUP value (for eventual variant group value)
 * @return actually assigned group value
 * @since 0-6-0
 */
public String createGroupVariant( PwsRecord[] recs, String group )
{
   String newName = getNewGroupName( group );
   for ( int i = 0; i < recs.length; i++ ) {
      PwsRecord rec = recs[i];
      String hstr = rec.getGroup().substring( group.length() );
      rec.setGroup( newName + hstr );
   }
   return newName;
}

/** Returns a unique GROUP value which is not yet contained in this file.
 *  If the parameter value is not contained, the parameter itself is returned.
 *  Otherwise a name is created derived from the parameter by appending 
 *  a dash and a number.
 *  
 *  @param group String initial group name
 *  @return String new group name (extended)
 */  
public String getNewGroupName( String group )
{
   String hstr = group;
   int i = 0;
   while ( dbf.containsGroup( hstr ) ) {
      hstr = group + "-" + ++i;
   }
   return hstr;
}  // getNewGroupName

/**
 * Stores the two file option objects (MINOR and MAYOR options) into their 
 * appropriate destinations, database or local system preferences. The UUID
 * parameter gives the file reference. If null this socket's UUID value is
 * taken. If not null, minor options are always stored to local preferences
 * regardless of whether they have been modified.
 * 
 * @param fileUUID <code>UUID</code> key value for mapping or null for default
 * @since 0-4-0
 * @since 0-5-0 protected (private before)
 */
protected void storeFileOptions ( UUID fileUUID )
{
//   System.out.println( "- starting STORE FILE OPTIONS (file socket)" );
   
   // collect option values from data structures (this class)
   majorOptions.setIntOption( FILEMODIFY_NUMBER, getFileModNumber() );

   // store MINOR into MAYOR options if conditions met
   boolean storeMinors = Options.isOptionSet("storeMinorChanges"); 
   if ( minorOptions.isModified() || fileUUID != null ) {
	   if (  storeMinors || isModified() ) {
		   majorOptions.addBag( minorOptions  );
		   Log.log(5, "(PwsFileSocket.storeFileOptions) minor file options put into mayor");
		   
		   // store special database header field for last-used-entries
		   String hstr = minorOptions.getOption("lastUsedEntries");
		   dbf.setRecentUsedEntries( hstr );
	   }
	
	   // store MINOR into local system properties if conditions met
	   if ( !storeMinors ) {
		   storeMinorOptionsToLocalMemory(fileUUID);
	   }
   }

   // store MAJOR options into database
   storeMajorOptionsIntoFile();
}  // storeFileOptions

private void storeMajorOptionsIntoFile () {
   if ( majorOptions.isModified() ) {
	   String outstr = optionString.trim();
	   if ( !majorOptions.isEmpty() ) {
	      // convert Properties into externalized string
	      String hstr = majorOptions.toString();
	
	      // remove unnecessary comment lines
	      while ( hstr.charAt( 0 ) == '#' )
	         hstr = hstr.substring( hstr.indexOf( '\n' ) + 1 );
	      Log.debug(5, "(PwsFileSocket.storeFileOptions) +++ mayor file options saved to database:\n   ["
		 		    + hstr + "]");
	      
	      // create our JPWS options text
	      hstr = Util.CSV.quoteText( hstr );
	      outstr += " " + FILEOPTION_TOKEN + hstr;
	   }
	   Log.debug(8, "(PwsFileSocket.storeFileOptions) --> (save) file USER options: [" + outstr + "]" );          
	   dbf.setUserOptions( outstr );
   }
}

/** Store minor file options into local system preferences with the given UUID
 * value as key. If the parameter is null, this file socket's UUID value is
 * taken.
 * 
 * @param fileUUID <code>UUID</code> key value for mapping or null for default
 */
private void storeMinorOptionsToLocalMemory( UUID fileUUID ) {
   if ( fileUUID == null ) {
	   fileUUID = getUUID();
   }
   String key = fileUUID.toHexString().toUpperCase();
   String value = minorOptions.toString();
   
   // remove unnecessary comment lines
   while ( value.charAt( 0 ) == '#' )
      value = value.substring( value.indexOf( '\n' ) + 1 );

   // encrypt value with key from major options
   if ( sysPrefCipher != null ) {
	   try {
		  int blockSz = sysPrefCipher.getBlockSize(); 
		  byte[] vbuf = value.getBytes("UTF-8");
		  byte[] buf = new byte[ (vbuf.length + 4) / blockSz * blockSz + blockSz ];
		  Util.writeIntLittle(vbuf.length, buf, 0);
		  System.arraycopy(vbuf, 0, buf, 4, vbuf.length);
		  buf = sysPrefCipher.encrypt(buf);
		  String encval = Util.bytesToHex(buf); 
		   
		  systemPrefs.put( key, encval );
		  Log.debug(5, "(PwsFileSocket.storeMinorOptionsToLocalMemory) +++ minor file options saved to SYSTEM PREFS:\n   "
				   + key + " = [" + value + "]");
		 
	   } catch ( Exception e ) {
		  e.printStackTrace();
	      Log.debug(10, "(PwsFileSocket.storeMinorOptionsToLocalMemory) *** FAILED: encryption of SYSTEM PREFERENCES value: "
	    		   .concat(value));
	   }
   } else {
      Log.debug(10, "(PwsFileSocket.storeMinorOptionsToLocalMemory) *** FAILED storing minor options to SYSTEM PREFERENCES, no cipher!");
   }
}
   
   

/**
 * Saves the datafile of this container, provided it is in a modified state.
 * If there is no persistent state defined for this file,  
 * <code>UndefinedPersistencyException</code> is thrown.
 * 
 * @throws JPWS_Exception, PasswordSafeException, IOException       
 */
public void saveFile () throws JPWS_Exception, IOException
{
   if ( isModified() )
   synchronized (dbf ) 
   {
//      System.out.println( "- starting SAVE FILE" ); 
      if ( !hasPersistentFile() )
         throw new UndefinedPersistencyException();
      
//      System.out.println( "- SAVE FILE, mark 1" ); 
      ContextFile cf = getContextFile();
      
      // if AUTOBACKUP attempt creating a backup copy of the previous persistent state
//         System.out.println( "- SAVE FILE, mark 2" ); 
      if ( Options.isOptionSet("autoBackup") ) {
         backupManager.performBackup( this );
      }
         
      // save file options
      storeFileOptions(null);
         
      // save PWS file
      Log.log( 6, "(PwsFileSocket.saveFile) - starting to SAVE DBF: ".concat(cf.getFilepath()) ); 
      IOManager.saveRealDatabase( dbf );
//         System.out.println( "- DBF SAVED" ); 

//         fileSaved();
   }
}  // saveFile

/** Saves the file in an emergency modus, if it is modified. 
 * 
 *  @return boolean whether the file was saved or did not need to be saved
 */
public boolean emergencySave ()
{
   if ( !isModified() ) return true;
   
   synchronized (dbf) {
	  System.out.println( "# starting EMERGENCY SAVE" ); 
	
	  // check whether file is new (without persistent state definition)
	  boolean isNew = !dbf.hasResource();
	   
	  // prevent exhausting event digestion 
	  setEventPause( true );
	   
	  try { 
	     // if file is new, create a default filepath 
	     if ( isNew ) {
	        // define a new emergency file for the PwsFile
	        String path = IOManager.getTemporaryFile(".dat", Global.backDir).getFilepath();
	        dbf.setApplication( IOManager.getLocalFileAdapter() );
	        dbf.setFilePath( path );
	        dbf = IOManager.getRegisteredDatabase( dbf );
	     }
	      
	     // attempt to save the file
	     saveFile(); 
	      
	  } catch ( Exception e ) {
	     System.out.println( "# *** EMERGENCY SAVE FAULT !! ".concat( e.toString() ) );
	     return false;
	  }
	   
	  System.out.println( "# EMERGENCY DATABASE SAVED ok: ".concat( getFilePath() ) );
   }
   return true;
}  // emergencySave

/**
 * Makes a copy of this container's datafile to a specified destination file
 * and assumes the resulting file as persistent state of this container. 
 * (Assumption is only adopted when save was successful.) UUID of the target
 * may change according to the "preserveUUID" parameter.
 *  
 * @param target <code>ContextFile</code> target file definition
 * @param preserveUUID boolean if <b>true</b> the copy's UUID is the same as of 
 *        this container, otherwise a new random value
 */
public void saveAs ( ContextFile target, boolean preserveUUID ) 
   throws JPWS_Exception, IOException
{
   UUID fid;
   
   synchronized ( dbf )
   {
      // perform a copy to target
      fid = saveCopyIntern( target, null, preserveUUID );
   
      // modify the file's persistent state definition
      dbf.setApplication( target.getAdapter() );
      setFilePath( target.getFilepath() );
      if ( fid != null )
         dbf.setUUID( fid );

      fileSaved();
   }
}  // saveAs

/**
 * Writes a copy of this container's datafile to the specified file
 * destination. This container remains unchanged; the copy will
 * carry a new UUID identifier according to the "preserveUUID" value.
 * 
 * @param target <code>ContextFile</code> target file definition
 * @param pass <code>PwsPassphrase</code> an optional passphrase for the copy; 
 *        if <b>null</b> the current passphrase of this container is used
 * @param preserveUUID boolean if <b>true</b> the copy's UUID is the same as of 
 *        this container, otherwise a new random value
 */
public void saveCopy ( ContextFile target, PwsPassphrase pass, boolean preserveUUID ) 
   throws IO_OverrunException, IOException
{
   saveCopyIntern( target, pass, preserveUUID );
}  // saveCopy

/**
 * Writes a copy of this container's datafile to the specified file
 * destination. This container remains unchanged.
 * 
 * @param target <code>ContextFile</code> target file definition
 * @param pass <code>PwsPassphrase</code> optional passphrase for the copy; 
 *        if <b>null</b> the passphrase of this container is used
 * @param preserveUUID boolean if <b>true</b> the UUID will be the same
 *        in the copy otherwise a new one is created
 * @return <code>UUID</code> the file identifier value of the copy 
 *        or <b>null</b> if unavailable
 */
private UUID saveCopyIntern ( ContextFile target, PwsPassphrase pass, boolean preserveUUID ) 
   throws IO_OverrunException, IOException
{
   if ( target == null )
      throw new NullPointerException();
   
   synchronized (dbf )
   {
      // invoke save
  	  UUID fid = preserveUUID ? null : new UUID();
      storeFileOptions(fid);
      IOManager.saveCopyDatabase( dbf, target, pass, fid ); 
      return fid;
   }
}  // saveCopyIntern

/** Performs to delete a group of file entries. The parameter must denote
 *  exactly an existing group. If this group contains other groups,
 *  the delete action extends to all elements of all subgroups. If a 
 *  FILTER setting is active, the delete action extends only to the filtered
 *  view of the database.
 *  
 *  @param group String, a PWS normalized GROUP value
 *  @return <code>DefaultRecordWrapper[]</code> array of the deleted records (empty array 
 *          if group is void)
 *          
 * @since 0-5-0 modified return type
 */
public DefaultRecordWrapper[] deleteGroup ( String group )
{
   DefaultRecordWrapper[] recs;
   
   recs = getGroupRecords( group );
   if ( recs.length != 0 )
      deleteEntries( recs );
   return recs;
}  // deleteGroup

/**
 * Performs deleting any set of file entries.
 * Does nothing if the selection is empty.
 *  
 * @param set array of <code>DefaultRecordWrapper</code>, may be <b>null</b>
 */
public void deleteEntries ( DefaultRecordWrapper[] set )
{
   if ( set != null && set.length > 0 )
   synchronized ( dbf ) {
	  PwsRecordList list = new PwsIgDupRecordList( set );
      dbf.removeRecordList( list );
   }
}  // deleteEntries

/**
 * Deletes a record from this file.
 * Does nothing if the parameter is void.
 *  
 * @param rec <code>PwsRecord</code> to be deleted, may be <b>null</b>
 * @since 0-6-0
 */
public void deleteRecord ( PwsRecord rec )
{
   if ( rec != null )
   synchronized ( dbf ) {
      // remove from database
      dbf.removeRecord( rec );
   }
}  // deleteRecord

/**
 * Assigns a new GROUP value to any set of records, replacing previous assignments.
 * Does nothing if the selection is empty.
 *  
 * @param set array of <code>DefaultRecordWrapper</code>, may be <b>null</b>
 * @param group a String value for the GROUP record field (may be <b>null</b> 
 *        which is equivalent to empty)
 * @param keepPaths         
 * @return <code>DefaultRecordWrapper[]</code> list of records 
 *        actually moved or <b>null</b> if no operation              
 */
public DefaultRecordWrapper[] moveEntries ( DefaultRecordWrapper[] set, String group, boolean keepPaths )
{
   if ( set == null || set.length == 0 ) return set;
   
   synchronized ( dbf ) {
      return dbf.moveRecords( set, group, keepPaths );
   }
}  // moveEntries

/** Performs to incorporate a set of <code>PwsRecord</code>s, contained in
 *  the parameter record list, into this database. A conflict solving strategy
 *  may be specified in the <code>modus</code> parameter, which come into effect
 *  when a record of the parameter list has an identity which is already contained
 *  in this file.
 *  <p>Records incorporated through this method will be marked as IMPORTED for
 *  the lifetime of this container instance.
 *  
 *  @param list
 *  @param modus conflict solving strategy (0 = exclude); option values are to
 *         taken from <code>PwsRecordList</code> 
 *  @param allowInvalids determines whether invalid records of the source are
 *         excluded (=false) or considered candidates for inclusion (=true)     
 *  @return  <code>PwsRecordList</code> containing records which were NOT MERGED due to conflict      
 */
public PwsRecordList mergeDatabase ( PwsRecordList list, int modus, boolean allowInvalids )
{
   if ( list == null )
      throw new NullPointerException();
   
   synchronized ( dbf )
   {
      return dbf.merge( list, modus, allowInvalids );
   }
}  // mergeDatabase

/**
 * Replaces the database content of this container by the content of the
 * parameter record list. If the given parameter is of type <code>PwsFile</code>
 * then format version and header fields are also replaced.
 * 
 * @param list <code>PwsRecordList</code>
 */
public void substituteContent ( PwsRecordList list ) throws PasswordSafeException
{
   synchronized ( dbf )
   {
      // replace options
      if ( list instanceof PwsFile ) {
    	 boolean oldPause = dbf.getEventPause();
         dbf.setEventPause( true );
         PwsFile file = (PwsFile)list;
         dbf.setFormatVersion( file.getFormatVersion() );
         dbf.setSecurityLoops( file.getSecurityLoops() );
         dbf.setHeaderFields( file.getHeaderFields() );
         dbf.setPassphrase( file.getPassphrase() );
         initFileOptions();
         dbf.setEventPause( oldPause );
      }

      // substitute dbf (conserve filepath) and clear old file
      dbf.clear();
      addRecordList( list );
   }
}  // substituteContent

/** Adds a list of records, represented by an array of record wrappers, into 
 * this database. The first occurrence of a duplicate entry will break 
 * execution of the inclusion.
 * 
 * @param records array of <code>DefaultRecordWrapper</code>
 * @throws DuplicateEntryException
 * @throws IllegalArgumentException if any input record is invalid
 * @since 0-5-0
 */
public void addRecordList ( DefaultRecordWrapper[] records ) throws PasswordSafeException
{
   addRecordList( new PwsRecordList( records ) );
}  // addRecordList

/** Adds a list of records into this database. The first occurrence of a duplicate entry
 *  will break execution of the inclusion.
 * 
 * @param list <code>PwsRecordList</code>
 * @throws DuplicateEntryException
 * @throws IllegalArgumentException if any input record is invalid
 */
public void addRecordList ( PwsRecordList list ) throws PasswordSafeException
{
   synchronized ( dbf )
   {
      dbf.addRecordList( list );
   }
}  // addRecordList

/** Removes a list of records from this file. 
 * 
 * @param list <code>PwsRecordList</code>
 * @throws PasswordSafeException
 */
public void deleteRecordList ( PwsRecordList list ) 
{
   synchronized ( dbf )
   {
      dbf.removeRecordList( list );
   }
}  // deleteRecordList

/** Whether the database was modified. 
 */
public boolean isModified ()
{
   return modified || (dbf != null && dbf.isModified()) || 
		   majorOptions.isModified() ||
          (minorOptions.isModified() && Options.isOptionSet( "storeMinorChanges" ));
}

/** Resets the modify marker for this container and database to UNMODIFIED. 
 */
public void resetModified ()
{
   modified = false;
   dbf.resetModified();
   minorOptions.resetModified();
   majorOptions.resetModified();
   // dispatch modify event to update display
   // TODO should introduce special event for only display change?
   fireChangeEvent( MODIFY_EVENT );
//   reportPropertyChange( MODIFY_EVENT );
}

/** Sets this container to take on the MODIFIED state. 
 * @since 0-5-0
 */ 
public void setModified ()
{
//    Log.log(8, "(PwsFileSocket.setModified)");
    
   // on up-flank increase file modify number
   if ( !modified ) {
      fileModNumber++;
      modified = true;
   }
   reportPropertyChange( MODIFY_EVENT );
}

/** Sets the time marker for when the enclosed database has been last saved.
 * 
 * @param time long epoch milliseconds
 */
public void setLastSaveTime ( long time ) {
    lastSaveTime = time;
}

/** Time when the enclosed database was last saved.
 * 
 * @return long epoch milliseconds
 */
public long getLastSaveTime () {
    return lastSaveTime;
}

/** Sets whether events on this socket or from underlying objects will 
 * be processed and handed out to listeners.
 * 
 * @param v if <b>true</b> no events are processed 
 * @since 0-5-0
 */
public void setEventPause ( boolean v )
{
   eventPause = v;
   dbf.setEventPause( v );
}

/** Whether this container currently does not dispatch events due to
 * internal blocking (event grouping).
 * 
 * @return boolean
 */
public boolean isEventPaused ()
{
   return eventPause;
}

/**
 * Whether this database harbours invalid records.
 * @since 0-6-0
 */
public boolean hasInvalidRecs ()
{
   return dbf.hasInvalidRecs();
}

/** Whether the persistent state of this database can be written to its destined 
 *  data medium. (This function might block for a reply of the IO-context, depending
 *  on the ApplicationAdapter activated.) 
 *  
 *  @return <b>true</b> if and only if there is a persistent state defined and
 *          its IO-context permits writing the file at the time when this method
 *          is called 
 */
public boolean canWrite () throws IOException
{
   return hasPersistentFile() && dbf.getApplication().canWrite( dbf.getFilePath() );
}

/**
 * Attempts to close this container for operations. Saves the persistent state 
 * of the database if modified. 
 * 
 * @param save whether a SAVE operation should be performed if DB is modified
 * @throws JPWS_Exception, PasswordSafeException, IOException
 */
public void close ( boolean save ) throws JPWS_Exception, IOException
{
   // control open file
   if ( isOpen ) {
	  if ( save ) {
	      saveFile();
	  } else if ( minorOptions.isModified() && 
			     !Options.isOptionSet("storeMinorChanges") ) {
		  storeMinorOptionsToLocalMemory(null);
	  }
   }
   
   isOpen = false;
   changeListeners.clear();
   if ( dbf != null ) {
      dbf.removeFileListener( sortList );
      sortList = null;
      Options.removeChangeListener( this );
      dbf.removeFileListener( fileListener );
      dbf = null;
   }
}  // close


/** Clears away all non-canonical fields from this database, including unknown header 
 * fields. 
 * @since 0-4-0
 */
public void clearUnknownFields ()
{
   synchronized ( dbf ) {
      dbf.clearUnknownFields();
   }
}

/**
 * Returns the size of the persistent state of this database.
 * @return number of bytes
 * @since 0-4-0
 */
public long getStoredSize ()
{
   synchronized ( dbf )
   {
      return dbf.getBlockedDataSize( dbf.getFormatVersion() );
   }
}

/**
 * Returns the last-saved time of the persistent state of this database
 * or zero if this information could not be obtained.
 * 
 * @return long time in milliseconds
 * @since 0-5-0
 */
public long getStoreTime ()
{
   synchronized ( dbf ) {
      try {
         return dbf.getApplication().getModifiedTime( dbf.getFilePath() );
      } catch ( Exception e ) {
         return 0;
      }
   }
}

/** Returns a list of context-files representing the actually
 * existing auto-backup file of this database.
 * 
 * @return <code>List</code> of <code>ContextFile</code>
 * @since 0-5-0
 */  
public List<ContextFile> getAutoBackups ()
{
   return backupManager.getBackupList();
}

/**
 *  Attempts to find the next record position in the ordered list of this
 *  container whose record contains the search text as specified.
 * 
 * @param index bounding start position, first record investigated is index + 1
 * @param text search text
 * @param cs whether search is case sensitive
 * @param wd whether search looks for whole words only
 * 
 * @return index position in ordered list or -1 if result is void
 */
public int findMatching ( int index, String text, 
      boolean cs, boolean wd )
{
   if ( text != null && text.length() != 0 )
   synchronized ( dbf ) {
      for ( int i = index+1; i < sortList.size(); i++ ) {
         if ( sortList.getItemAt( i ).hasText( text, cs, wd ) )
            return i;
      }
   }
   return -1;
}

/** Returns the <code>PwsFile</code> which is the subject of this container. */
public PwsFile getPwsFile ()
{
   return dbf;
}

/** Returns the <code>OrderedRecordList</code> active for the 
 *  database content of this container. */
 public OrderedRecordList getOrderedList ()
{
   return sortList;
}

/** The filename of the filepath of this database. Renders "?" if none is defined. 
 */
public String getFileName ()
{
   String hstr, hstr2;
   
   hstr = "?";
   if ( dbf != null && (hstr2 = dbf.getFilePath()) != null )
      hstr = Util.fileNameOfPath( hstr2 );
   return hstr;
}

/** Returns the logical database name if it is defined and <code>getFileName()</code>
 *  otherwise.  
 * @since 0-5-0
 */
public String getDatabaseName ()
{
   String hstr;
   
   // use logical filename if corresponding user option is set and
   // such a name is actually available
   if ( !Options.isOptionSet( "logicalFilenames" ) || (hstr = logicalName) == null )
      hstr = getFileName();
   return hstr;
}

/** Returns the UUID identifier of this database.
 *  @since 0-4-0
 */
public UUID getUUID ()
{
   return dbf == null ? null : dbf.getUUID();
}

/** Returns the filepath defined for this database or <b>null</b> if none is
 *  defined.
 */
public String getFilePath ()
{
   return dbf.getFilePath();
}

/**
 * Returns the record (clone) with the specified Record-ID.
 * 
 * @param recID the Record-ID of the requested record
 * @return the requested <code>PwsRecord</code> or <b>null</b> if the record 
 *         is unknown in this file
 */
public PwsRecord getRecord ( UUID id )
{
   return dbf.getRecord( id );
}

public PwsRecord getRecordAt ( int index )
{
   return getOrderedList().getItemAt( index ).getRecord();
}

/** Returns the number of records in this database. */ 
public int getRecordCount ()
{
   return dbf.size();
}

/** Returns the number of records in this database subject to the current
 * filter setting. */ 
public int getFilteredSize ()
{
   return sortList.size();
}

/**
 * Whether the text find filter is active on this socket.
 * (See setTextFindFilter())
 * 
 * @return boolean
 */
public boolean isTextFindFilterActive ()
{
   return findFilterActive;
}

/** Returns the application adapter (IO context) defined for this database or
 * <b>null</b> if none is defined.
 */
public ApplicationAdapter getApplication ()
{
   return dbf.getApplication();
}

/** Returns the URL specification for the persistent file of this container or 
 *  <b>null</b> if that is undefined or erroneous. */
public URL getUrl () 
{
   String path;
   
   if ( url != null )
      return url;
   
   if ( (path = getFilePath()) == null )
      return null;

   try { 
      url = Util.makeFileURL( path );
//      System.out.println( "-- (PwsFileSocket) make file URL: ".concat( url.toString() ) );
      return url;
   }
   catch ( IOException e )
   {
      System.out.println("*** Illegal Filepath: " + path );
      System.out.println( e );
      return null;
   }
}

/**
 * Returns the <code>ContextFile</code> applicable to represent the
 * persistent state of this container, or <b>null</b> if no persistent state
 * is defined.
 * 
 * @return <code>ContextFile</code> or <b>null</b>
 * @since 0-6-0
 */
public ContextFile getContextFile ()
{
   if ( !hasPersistentFile() )
      return null;
   
   return IOManager.makeContextFile( getApplication(), getFilePath() );
}

/**
 * Returns the actual value of a data field assigned to the database header area.
 * 
 * @param name field identifier (0..255)
 * @return String field value or <b>null</b> if this field is unassigned in this
 *         database
 * @since 0-5-0
 */
public String getHeaderValue ( int name )
{
   PwsRawField fld;
   
   synchronized ( dbf )
   {
      fld = dbf.getHeaderFields().getField( name );
      return fld == null ? null : fld.getString( "utf-8" );
   }
}

/**
 * Returns a shallow clone of the database's header field list.
 * 
 * @return <code>HeaderFieldList</code>
 * @since 0-5-0
 */
public HeaderFieldList getHeaderFields ()
{
   synchronized ( dbf )
   {
      return (HeaderFieldList)dbf.getHeaderFields().clone();
   }
}

/** Replaces the current header field list of this database with a 
 * shallow copy of the parameter field list.
 *   
 *  @param list <code>HeaderFieldList</code>, if <b>null</b> nothing happens
 *  @since 0-5-0
 */
public void setHeaderFields ( HeaderFieldList list )
{
   String hstr;
   
   synchronized ( dbf )
   {
      logicalName = null;
      if ( list != null )
      {
         // get (modified) database name 
         hstr = list.getStringValue( HEADERFIELD_DBNAME );
         logicalName = hstr.length() == 0 ? null : hstr;
      }

      dbf.setHeaderFields( list ); 
   }
}

/** Iterator over all records of the underlying PWS database (unfiltered).
 * Operations of this iterator are synchronised.  
 * 
 * @return <code>Iterator&lt;PwsRecord&gt;</code>
 */
public Iterator<PwsRecord> iterator () {
	return new SynchronizedFileIterator();
}

/**
 * Sets the value of a data field which gets assigned to the database header area.
 * <b>null</b> or empty string (equivalent) clear and unassign the field.
 * 
 * @param name field identifier (0..255)
 * @param value String new field value or <b>null</b> 
 * @since 0-5-0
 */
public void setHeaderField ( int name, String value )
{
   byte[] data;
   
   synchronized ( dbf )
   {
      try { 
         if ( value == null || value.length() == 0 )
            dbf.getHeaderFields().removeField( name );
         else
         {
            // check for modified database name 
            // (must do before entry because of ensuing event handling references)
            if ( name == HEADERFIELD_DBNAME )
               logicalName = value;

            data = value.getBytes( "utf-8" ); 
            dbf.getHeaderFields().setField( new PwsRawField( name, data ) );
         }
      }
      catch ( UnsupportedEncodingException e ) {}
   }
}

/** Sets the amount of security loops (access method during file opening)
 *  for this database.
 *  
 * @param loops amount of calculation loops
 * @since 0-5-0
 */
public void setSecurityLoops ( int loops )
{
   synchronized ( dbf )
   {
      dbf.setSecurityLoops( loops );
   }   
}

/**
 * Returns the amount of security loops (access method during file opening)
 * currently set for this database.
 *  
 * @return int security calculation loop
 * @since 0-5-0
 */
public int getSecurityLoops ()
{
   return dbf.getSecurityLoops();
}

/** Returns the encryption passphrase used on this file. 
 * 
 * @return <code>PwsPassphrase</code> or <b>null</b> if no passphrase is defined
 */
public PwsPassphrase getPassphrase ()
{
   return dbf.getPassphrase();
}

/** Sets the encryption passphrase (user view) for this file socket. */ 
public void setPassphrase ( PwsPassphrase pass )
{
   synchronized ( dbf )
   {
      dbf.setPassphrase( pass );
   }
}

/**
 * Returns the set of records that belong to a GROUP value of the display list.
 * The records of subgroups are included.
 * If the display is restricted by a FILTER setting, then the returned set is
 * restricted accordingly. If the selection is empty, an empty array is returned.
 *  
 * @param group complete existing group name 
 * @return set of record wrappers <code>DefaultRecordWrapper</code>
 */
public DefaultRecordWrapper[] getGroupRecords ( String group )
{
   synchronized ( dbf )
   {
      return getOrderedList().getGroup( group, true );
   }
}

/**
 * Returns the set of records belonging to a GROUP value, including all
 * records of subgroups. This works independent of any FILTER setting 
 * and pulls directly from the plain database.
 * If the selection is empty, an empty array is returned. This returns 
 * fresh clones of database records.
 *  
 * @param group complete existing group name 
 * @return set of record wrappers <code>DefaultRecordWrapper</code>
 * @since 0-5-0
 */
public DefaultRecordWrapper[] getAbsoluteGroupRecords ( String group )
{
   DefaultRecordWrapper recs[];
   Iterator<?> it;
   int i;
   
   synchronized ( dbf )
   {
      recs = new DefaultRecordWrapper[ dbf.getGrpRecordCount( group, true ) ];
      for ( it = dbf.getGroupedRecords( group, true ), i = 0; it.hasNext(); i++ )
         recs[ i ] = new DefaultRecordWrapper( (PwsRecord)it.next(), null );
      return recs;
   }
}

/**
 * Returns the set of records which carry an "IMPORTED" marker.
 *   
 * @param cut PwsRecordList if not <b>null</b> the result will be an 
 *        intersection with this parameter  
 * @return DefaultRecordWrapper[]
 * @since 0-5-0
 */
public DefaultRecordWrapper[] getImportedRecords ( PwsRecordList cut )
{
   ArrayList<PwsRecord> list;
   
   synchronized ( dbf ) {
      list = new ArrayList<PwsRecord>();
      for ( Iterator<?> it = dbf.iterator(); it.hasNext(); ) {
    	 PwsRecord rec = (PwsRecord) it.next();
         if ( rec.getImportStatus() != 0 &&
            ( cut == null || cut.contains( rec ) ) ) {
        	 
            list.add( rec );
         }
      }
   }   
   PwsRecord[] arr = new PwsRecord[ list.size() ];
   list.toArray( arr );
   return DefaultRecordWrapper.makeWrappers( arr, null );
}

/** Returns an ordered list of the GROUP field values active in this container.
 *  Only group names are shown which there are entries in the database. (Ordering 
 *  follows collation rules of the actual VM default locale. This always returns
 *  not <b>null</b>.)
 *  
 *  @return java.util.List of strings
 */
public List<String> getGroupList ()
{
   return dbf.getGroupList();
}

/** Returns the number of actually used groups in this database.
 *  @since 0-4-0
 */ 
public int getGroupCount ()
{
   return dbf.getGroupCount();
}

/** A 32 byte signature (SHA-256 digest) value of the user data content of this database.
 * 
 *  @return byte[] of 32 bytes length
 *  @since 0-4-0
 */ 
public byte[] getSignature ()
{
   synchronized ( dbf )
   { return dbf.getSignature(); }
}

/** Called when the file is updated. This method MUST be called when overridden
 *  by a subclass!
 */
protected void fileUpdated ()
{
   setModified();
}

/** Called when the file has been saved. This method MUST be called when 
 * overridden by a subclass!
 */
protected void fileSaved ()
{
   if ( eventPause ) return;
   
   // correct the MODIFIED filter content
   setLastSaveTime( System.currentTimeMillis() );
   if ( getFilterStatus() == FILTER_MODIFIED ) {
      sortList.refresh();
   }
   resetModified();
}

/** Sets the filter status for the ordered record list of this database to a 
 *  specified modus. 
 *  (Does nothing if the current status already corresponds to the parameter.)
 * 
 * @param status new filter status
 */  
public void setFilterStatus ( int status )
{
   if ( status == filterOption ) return;

   filterOption = status;
   sortList.refresh();
   reportPropertyChange( FILTER_MODE );
}

/** Returns the current filter status for the ordered record list of this 
 *  container. */
public int getFilterStatus ()
{
   return filterOption;
}

/**
 * Whether this container currently applies some record list filtering.
 * This covers both FILTER MODUS setting and QUICK FIND status.
 * 
 * @return boolean <b>true</b> if and only if <code>getFilterStatus() != FILTER_OFF</code>
 *         OR <code>isTextFindFilterActive() == true</code>
 */
public boolean isFiltering ()
{
   return getFilterStatus() != FILTER_OFF || isTextFindFilterActive();
}

/**
 * Modifies the current filtering of the record list of this socket
 * so that it performs a cutset with the results of a text search in
 * the database. The text search and filtering is triggered off by 
 * this method and the condition remains active until this method is 
 * called again with a different search text or <b>null</b> for 
 * disengagement. 
 *  
 * @param text String search text or <b>null</b> to disengage
 */
public synchronized void setTextFindFilter ( String text ) {
   if ( text == null || text.length() == 0 ) {
      findFilterActive = false;
      findFilterText = null;
   } else {
      // mark find-filter-active for this socket 
      findFilterText = text;
      findFilterActive = true;
   }

   // resort record list and issue event "Filter Mode Modified"
   sortList.refresh();
}

/**
 * Resorts the current filtered list on this database.
 * @since 0-6-0
 *
 */
public synchronized void refreshFilter ()
{
   sortList.refresh();
}

/** This returns the text of the currently active find filter setup (search text)
 * or <b>null</b> if no find filter is active.
 * 
 * @return String search text or <b>null</b>
 */  
public String getFindFilterText ()
{
   return findFilterText;
}

/** Sets the active time scope for evaluating EXPIRY status of records.
 *  This recalculates the status of all records.
 * 
 *  @param time time period in milliseconds
 */
public void setExpireScope ( long time )
{
   expireScope = Math.max(0, time);
   sortList.setExpireScope( expireScope );
}

/** Returns the active time scope for evaluating EXPIRY status of records.
 * 
 * @return long milliseconds
 */
public long getExpireScope () {
	return expireScope;
}

/** Modifies the GROUP field value of a group of records. This renames
 *  only the specified SINGLE ELEMENT (last element) of a complex group name. 
 *  This does nothing if the record set defined by the parameter value is empty.
 *  !! This is NOT sensitive to FILTER settings. !!
 *  
 *  @param group String to identify an existing record group 
 *  @param newname String new name for the last element in the specified
 *         GROUP field value
 *  @return DefaultRecordWrapper[] the set of records actually modified  
 *          (empty array if operation is void)
 *               
 * @since 0-5-0 modified return type
 */
public DefaultRecordWrapper[] renameGroup ( String group, String newname )
{
   DefaultRecordWrapper[] recs;
   String trunk;
   
   synchronized ( dbf )
   {
      recs = new DefaultRecordWrapper[0];
      
      // do not operate if such a group does not exist
      if ( dbf.getGrpRecordCount( group, true ) > 0 )
      {
         // analyse group text -> get ancestor chain
         trunk = groupAncestors( group );
         
         // perform modification
         if ( newname != null )
            recs = dbf.renameGroup( group, trunk.concat( newname ) ).toRecordWrappers( null );
      }
      return recs;
   }
}  // renameGroup

/** Returns the ancestor group name of the given group name or empty string
 *  if there are no ancestors.
 *  
 * @param group PwsRecord group value
 * @return ancestor group value
 * @since 0-6-0 public (private before)
 */
public static String groupAncestors ( String group )
{
   String trunk;
   int i;
   
   trunk = "";
   if ( (i = group.lastIndexOf( '.' )) > -1 )
      trunk = group.substring( 0, i+1 );
   return trunk;
}

/** Moves a group of records (including subgroups) to a new group destination. 
 *  May be seen as a GROUP rename function with the power of shifting the group 
 *  to a new location in the tree. If a FILTER setting is active, this function
 *  extends only to the filtered view of the database.
 *  
 *  @param group existing GROUP name
 *  @param target new group name
 *  @param replace boolean true == entire group name will be replaced by new group name
 *                 false == last name element of <code>group</code> will be preserved  
 *  @since 0-6-0 additional parameter, extended semantics               
 */
public void moveGroup ( String group, String target, boolean replace )
{
   DefaultRecordWrapper[] recs;
   PwsRecord rec;
   String recgrp, targetGroup, lastElement, hstr;
   int i, trunkLength;
   
   if ( target != null )
   synchronized ( dbf )
   {
      targetGroup = target;
      if ( !replace )
      {
         hstr = groupAncestors( group );
         lastElement = group.substring( hstr.length() );
         if ( lastElement.length() > 0 )
            targetGroup = target + "." + lastElement;
      }
      
      // ensure that target group name is unique
      // except if source group is root (entire file content)
      if ( group.length() > 0 )
         targetGroup = getNewGroupName( targetGroup );
      
      trunkLength = group.length();
      recs = getGroupRecords( group );
      for ( i = 0; i < recs.length; i++ )
      {
         rec = recs[ i ].getRecord();
         recgrp = rec.getGroup();
         hstr = targetGroup + "." + 
                (recgrp != null ? recgrp.substring( trunkLength ) : "");
         rec.setGroup( hstr );
         try { updateRecordRelaxed( rec ); }
         catch ( NoSuchRecordException e )
         {
            throw new IllegalStateException( "PwsFileSocket.moveGroup():\r\n" + e );
         }
      }
   }
}  // moveGroup

public boolean containsRecord ( PwsRecord rec )
{
   synchronized ( dbf )
   {
      return dbf.contains( rec );
   }
}

public boolean containsRecord ( UUID id )
{
   synchronized ( dbf )
   {
      return dbf.contains( id );
   }
}

/** This local method reports a container property change. This will issue a
 *  change event of the container provided the value of the specified property 
 *  has changed since last reporting. (It is heavily used by constitutive submodules 
 *  like the view managers.)
 *  
 *  @param property one of MODIFY_EVENT, MODIFY_STATUS, FILTER_MODE (other values ignored)
 */  
protected void reportPropertyChange ( int property )
{
   if ( eventPause ) return;
   
   switch ( property )
   {
   case FILTER_MODE:
	  int i = getFilterStatus();  
      if ( i != reportedFilterMode ) {
         fireChangeEvent( FILTER_MODE );
      }
      reportedFilterMode = i;
      break;

   case MODIFY_EVENT:
      // always dispatch modify event
      fireChangeEvent( MODIFY_EVENT );
   }
}

/** Deletes the persistent states of all registered auto-backup 
 * files of this database.
 * @since 0-5-0
 */
public void eraseBackups ()
{
   backupManager.eraseFiles();
}

//*********** IMPLEMENTS OptionChangeListener  **************

@Override
public void optionChanged ( OptionChangeEvent e )
{
   String name = e.getOptionName();
   if ( name.equals( "findTextOpt" ) )  {
      int h = Options.getIntOption( "findTextOpt" );
      searchCaseSensitive = (h & 1) == 1;
      searchWholeWords = (h & 2) == 2;

   } else if ( name.equals( "expireScope" ) ) {
      setExpireScope( Options.getLongOption( "expireScope" ) );
   
   } else if ( name.equalsIgnoreCase( "storeMinorChanges") ) {
	   Log.debug(10,"(PwsFileSocket.optionChanged) -- NEW OPTION: storeMinorChanges == " + e.getNewValue());
	   boolean value = e.getNewValue().equals("true");
	   boolean oldValue = e.getOldValue().equals("true");
	   
	   // set the "strike-through" quality of MINOR options following storeMinorChanges 
	   minorOptions.setStrikeThrough(value);

	   // on the rising flank of the store option
	   if ( value & !oldValue ) {
		   Log.log(10,"(PwsFileSocket.optionChanged) adding minor options to mayor");
		   majorOptions.addBag( minorOptions );
	   }
	   // on the falling flank of the store option
	   else if ( !value & oldValue ) {
		   Log.log(10,"(PwsFileSocket.optionChanged) setting minor options modified");
		   minorOptions.setModified();
	   }
   }
}

/**
 * This class performs a selection criterion for the filter list of this 
 * container by evaluating records of the database as accepted / not-accepted. 
 * The selection refers to the actual FILTER_MODE of the container and evaluates 
 * corresponding record properties.
 */
protected class RecordFilter implements RecordSelector
{
   
   @Override
public boolean acceptEntry ( DefaultRecordWrapper record )
   {
      PwsRecord rec;
      boolean basic;
      
      rec = record.getRecord();
      switch ( filterOption )
      {
      case FILTER_EXPIRING :
         basic = rec.willExpire( System.currentTimeMillis() + 
                 Options.getLongOption( "expireScope" ));
         break;

      case FILTER_IMPORTED :
         basic = rec.getImportStatus() == PwsRecord.IMPORTED ||
                 rec.getImportStatus() == PwsRecord.IMPORTED_CONFLICT; 
         break;

      case FILTER_MODIFIED :
         basic = rec.getModifiedTime() > getLastSaveTime(); 
         break;

      case FILTER_INVALID :
         basic = !rec.isValid(); 
         break;

      case FILTER_OFF :
         basic = true; 
         break;

      default: basic = false;
      }
      
      return basic && findFilterOk( record );
   }
   
   public boolean findFilterOk ( DefaultRecordWrapper record ) {
      return !findFilterActive || record.hasText( findFilterText, 
            searchCaseSensitive, searchWholeWords );
   }
}

/**
 * This is a listener to the loaded PwsFile. We use it to get notice about
 * and react to file modifications or status changes.
 *  
 */
private class FileListener implements PwsFileListener
{
   @Override
public void fileStateChanged ( PwsFileEvent evt )
   {
      switch ( evt.getType() )
      {
      case PwsFileEvent.LIST_SAVED:
         fileSaved();
         break;
         
      case PwsFileEvent.TARGET_ALTERED:
         url = null;
//         System.out.println( "*** TARGET ALTERED for ".concat( getFilePath() ) );
         
      default:
//      case PwsFileEvent.LIST_CLEARED:
//      case PwsFileEvent.LIST_UPDATED:
//      case PwsFileEvent.RECORD_ADDED:
//      case PwsFileEvent.RECORD_REMOVED:
//      case PwsFileEvent.RECORD_UPDATED:
//      case PwsFileEvent.PASSPHRASE_ALTERED:
//      case PwsFileEvent.CONTENT_ALTERED:
  		fileUpdated();
      }
      
      processFileEvent( evt );
   }
}

/** Class for events which are thrown to indicate alterations in the enclosing
 *  class'es operation properties. This is a subclass of
 *  <code>javax.swing.event.ChangeEvent</code>, so listeners can either be contented
 *  by the common simple change listener or take reference to extended
 *  information about the changed property by type-casting received
 *  event instances to this class.    
 *  <p>Listeners must register with <code>addChangeListener()</code>.
 *  
 */ 
public static class ChangeEvent extends javax.swing.event.ChangeEvent
{  
   private int changeProperty;

   public ChangeEvent ( Object source, int property )
   {
      super( source );
      changeProperty = property;
   }
   
   /** The changed property of the referred file-container. The file-container 
    * is obtainable by <code>getSource()</code>.
    * 
    * @return int property code
    */
   public int getState ()
   {
      return changeProperty;
   }
}

/**
 * This is only for testing purpose to get notice of container property changes.
 * Nota! This reacts only to the properties of PwsFileSocket.  
 */
@SuppressWarnings("unused")
private static class EventReporter implements ChangeListener
{

   @Override
public void stateChanged ( javax.swing.event.ChangeEvent e )
   {
      PwsFileSocket ct;
      String hstr, valstr;
      int prop, val;
      
      prop = ((ChangeEvent)e).getState();
      ct = (PwsFileSocket)e.getSource();
      hstr = null; 
      valstr = "?";
      
      if ( prop == FILTER_MODE )
      {
         val = ct.getFilterStatus();
         hstr = "FILTER MODE";
         switch ( val )
         {
         case FILTER_OFF: valstr = "FILTER OFF";
         break;
         case FILTER_EXPIRING: valstr = "FILTER EXPIRING";
         break;
         case FILTER_IMPORTED: valstr = "FILTER IMPORTED";
         break;
         case FILTER_MODIFIED: valstr = "FILTER MODIFIED";
         break;
         }
      }
      else if ( prop == MODIFY_EVENT )
      {
         hstr = "MODIFY_EVENT";
         valstr = ct.isModified() ? "DB MODIFIED" :"DB UNMODIFIED";   
      }
      
      if ( hstr != null )
         Log.debug( 5, "+++ New Socket State: " + hstr + " == " + valstr ); 
   }
}  // class EventReporter

/**
 * Organises automatic backups for PwsFiles in PwsFileSocket objects.
 * May also be used to gain information on stored auto-backup files
 * of a database.
 * 
 * @since 0-5-0
 * @since 0-6-0 modified status and parameters for public use (private before)
 */
public static class BackupManager 
{
   private ArrayList<ContextFile>   backups = new ArrayList<ContextFile>();
   private boolean                  active = true;


   /** Discards any previous content and gathers new information
    * about the stored auto-backup copies of the parameter 
    * database file.
    */
   public void refresh ( ContextFile f )
   {
      ApplicationAdapter adapter;
      String paths[], path, filePath, trunk, hstr;
      long time;
      int i, index;

      // erase BackupManager memory list
      backups.clear();
//      System.out.println( "-- clearing Backup Memory (refresh)" );
      
      if ( f == null )
         return;
      
      adapter = f.getAdapter();
      filePath = trunk = f.getFilepath();
      if ( (i = trunk.lastIndexOf( '.' )) != -1 )
         trunk = trunk.substring( 0, i );
      
      // obtain list of filepaths from IO-context which start with basic filename
      try { 
         paths = adapter.list( trunk, Global.DEFAULT_BACKUPEXTENTION, false );
         if ( paths == null )
         {
            active = false;
            return;
         }
      }
      catch ( IOException e )
      {
         System.out.println( "*** ERROR in Backup Manager (refresh): \r\n" + e );
         active = false;
         return;
      }
      
      // analyse resulting paths for files of the auto-backup brand
      // and read them into BackupManager memory list
      for ( i = 0; i < paths.length; i++ )
      {
         path = paths[ i ];
         index = path.lastIndexOf( "SEC-" );
         // filter out non-belonging files
         if ( index == trunk.length()+1 && !path.equals( filePath ) )
         {
            try {
               hstr = path.substring( index+4, index+19 );
               time = Util.timeFromString( hstr, null );
               // make sure this really was a valid time-marked file
               if ( time != -1 )
               {
                  sortIn( IOManager.makeContextFile( adapter, path ) );
               }
            }
            catch ( Exception e )
            {}
         }
      }
   }  // refresh

   private void sortIn ( ContextFile f )
   {
      ContextFile obj;
      int i, index;
      
      synchronized ( backups )
      {
         index = backups.size();
         for ( i = 0; i < backups.size(); i++ )
         {
            obj = backups.get( i );
            if ( obj.getFilepath().compareTo( f.getFilepath() ) <= 0 )
            {
               index = i;
               break;
            }
         }

         backups.add( index, f );
//         System.out.println( "-- added BACKUP MANAGER entry (at pos " + index + "): " + f.getFilepath() ); 
      }
   }
   
   /**
    * Removes that number of entries in the backups list AND
    * on the persistent medium to ensure the list is at maximum 
    * of the parameter size. 
    * Entries which are expected to be oldest are preferrably
    * deleted.
    * 
    * @param size maximum size of the list after execution 
    */
   private void truncate ( int size ) throws IOException
   {
      ContextFile e;
      int index;
      
      synchronized ( backups )
      {
         while ( backups.size() > size )
         {
            // find oldest entry in list
            index = backups.size() - 1;
            e = backups.get( index );
            
            // remove entry from external medium
            if ( !e.delete() )
               throw new IOException( "unable to remove file: " + e.getFilepath() );
            
            // remove entry from list
            backups.remove( index );
//            System.out.println( "-- removed BACKUP MANAGER entry (at pos " + index + "): " + e.getFilepath() ); 
         }
      }
   }
   
   /** The path name of the next backup copy of the given database file. 
    * 
    * @param path String original file path
    * @param ftime file modification time (may be 0 for current time)
    * @return
    * @throws IOException
    */
   private String targetPath ( String path, long ftime ) throws IOException
   {
      String targetPath, timeStr;
      int i;
      
      // create time appendix
      if ( ftime == 0 )
         ftime = System.currentTimeMillis();

      timeStr = Util.standardTimeString( ftime, Util.GMT );
      timeStr = Util.substituteText( timeStr, ":", "" );
      timeStr = Util.substituteText( timeStr, "-", "" );
      timeStr = Util.substituteText( timeStr, " ", "-" );

      // strip extention from path
      if ( (i = path.lastIndexOf( '.' )) != -1 )
         path = path.substring( 0, i );
      
      // construct target path
      targetPath = path + " SEC-" + timeStr + Global.DEFAULT_BACKUPEXTENTION;
      return targetPath;
   }
   
   /** 
    * Creates a backup copy of the parameter socket's database file according to
    * the regulations set up by the user. This includes maintaining the correct size
    * of the total backup copies for this database.
    * 
    * @return boolean true if and only if a copy has been created
    *
    */
   public boolean performBackup ( PwsFileSocket socket )
   {
      ContextFile target, thisFile;
      ApplicationAdapter adapter;
      String targetPath, path;
      int max;

      Log.log( 3, "(PwsFileSocket.BackupManager) - starting PERFORM AUTO-BACKUP: ".concat( socket.getDatabaseName() )); 

      // verify basic conditions
      if ( !active ||
           (path = socket.getFilePath()) == null ||
           (adapter = socket.getApplication()) == null )
         return false;
      
      // identify current file and check for minimum size
      thisFile = IOManager.makeContextFile( adapter, path );
      target = null;
      if ( thisFile.length() <= 0 )
         return false;

      try {
         // create backup target name and context file
         targetPath = targetPath( path, socket.dbf.lastModified() );
         target = IOManager.makeContextFile( adapter, targetPath );
   
         // verify writing conditions
         if ( !target.canWrite() )
            return false;
   
         // control backup array (removes exceeding backup entries)
         max = Math.max( 0, Options.getIntOption( "autoBackupFiles" ) - 1 );
         truncate( max );
         
         // create backup file
//         System.out.println( "-- creating BACKUP copy: " + targetPath );
         thisFile.copyTo( target );
//         System.out.println( "-- stream copy of BACKUP done" );

         // register entry in backup list
         sortIn( target );
         return true;
      }
      catch ( Exception e )
      {
         System.out.println( "*** ERROR in Backup Manager (perform): \r\n" + e );
         return false;
      }
   }

   /** Erases all listed backup files (including their persistent states). */
   public void eraseFiles ()
   {
      try { truncate( 0 ); }
      catch ( IOException e )
      {}
   }
   
   /**
    * Returns a list of <code>ContextFile</code> objects representing the
    * actually stored auto-backup copies of the enclosing object's database
    * file. (The list is a shallow clone of the internal list.)
    * 
    * @return <code>List</code> of <code>ContextFile</code> sorted descending
    *         on the file modification times
    */
   @SuppressWarnings("unchecked")
   public List<ContextFile> getBackupList ()
   {
      return (List<ContextFile>) backups.clone();
   }
   
   /** Returns the youngest backup file from this manager's domain
    *  or <b>null</b> if no backup is available. */
   public ContextFile getTopBackup ()
   {
      return (backups.size() > 0 ? backups.get( 0 ) : null);
   }
   
}  // class BackupManager

/**
 * An OptionBag which alters the MODIFIED state of the enclosing container
 * when content of the bag is altered.
 * 
 * @since 0-5-0
 */
private class FileOptions extends OptionBag
{
	private boolean strikeThrough;
	

	/** Creates a file-options instance. 
	 * 
	 * @param strikeThrough boolean true == modify strike-through to enclosing file
	 */
	public FileOptions ( boolean strikeThrough )
	{
		this.strikeThrough = strikeThrough;
	}
	
	/** Whether modifications on this option bag strike through as modification
	 * of the enclosing file socket.
	 * 
	 * @return boolean true == strike through
	 */
	public boolean isStrikeThrough() {
		return strikeThrough;
	}


	/** Whether modifications on this option bag strike through as modification
	 * of the enclosing file socket.
	 * 
	 * @param strikeThrough boolean true == strike through
	 */
	public void setStrikeThrough (boolean strikeThrough) {
		this.strikeThrough = strikeThrough;
	}

   @Override
   public boolean setIntOption ( String token, int value )
   {
      boolean mod = super.setIntOption( token, value );
      if ( mod && strikeThrough )
         setModified();
      return mod;
   }

   @Override
   public boolean setLongOption ( String token, long value )
   {
      boolean mod = super.setLongOption( token, value );
      if ( mod && strikeThrough )
         setModified();
      return mod;
   }

   @Override
   public boolean setOption ( String token, boolean value )
   {
      boolean mod = super.setOption( token, value );
      if ( mod && strikeThrough )
         setModified();
      return mod;
   }

   @Override
   public boolean setOption ( String token, String value )
   {
      boolean mod = super.setOption( token, value );
      if ( mod && strikeThrough ) 
         setModified();
      return mod;
   }

   @Override
   public boolean setProperty ( String token, String value )
   {
      boolean mod = super.setProperty( token, value );
      if ( mod && strikeThrough )
         setModified();
      return mod;
   }

	@Override
	public void setModified() {
		super.setModified();
		if ( strikeThrough ) {
			PwsFileSocket.this.setModified();
		}
	}
}

public class PwsFileRecentList extends RecentList 
{
	private FileOptions options; 
	private String token;
	
	public PwsFileRecentList (int max, String token, boolean minor) {
		this(null, max, token, minor);
	}

	public PwsFileRecentList(String command, int max, String token, boolean minor) {
		super(command, max);
		if ( token == null || token.isEmpty() ) 
			throw new IllegalArgumentException("token is null or empty");

		options = minor ? minorOptions : majorOptions;
		this.token = token;
		super.setContent(options.getOption(token));
		super.resetModified();
	}

	@Override
	public void setContent (String list) {
		super.setContent(list);
		transferToOptions();
	}

	@Override
	public void setContent (Object[] objs) {
		super.setContent(objs);
		transferToOptions();
	}

	@Override
	public void clear() {
		int size = getSize();
		super.clear();
		if ( size > 0 ) {
			transferToOptions();
		}
	}

	@Override
	public void setMaxEntries (int value) {
		int max = getMaxEntries();
		super.setMaxEntries(value);
		if ( max > getMaxEntries() ) {
			transferToOptions();
		}
	}

	@Override
	public boolean removeRecent (Object value) {
		boolean mod = super.removeRecent(value);
		if ( mod ) {
			transferToOptions();
		}
		return mod;
	}

	@Override
	public boolean pushRecent (Object value) {
		boolean mod = super.pushRecent(value);
		if ( mod ) {
			transferToOptions();
		}
		return mod;
	}

	@Override
	public void replaceRecent (Object oldObj, Object newObj) {
		super.replaceRecent(oldObj, newObj);
		transferToOptions();
	}

	private void transferToOptions() {
		String value = getStringContent();
		options.setOption(token, value);
		Log.debug(10, "(PwsFileRecentList.transferToOptions) transferring content for: " + token); 
	}
}



/**
 * Returns a text sequence stating user and host of most recent 
 * modifying access to the file. If this information is not available
 * in this file, the empty string is returned.
 * 
 * @return String last usage information (may be empty) 
 */
public String getLastUserText() {
    String text;
    String hstr1 = getHeaderValue( PwsFileHeaderV3.OPERATOR_TYPE );
    String hstr2 = getHeaderValue( PwsFileHeaderV3.HOST_TYPE );
    
    if ( hstr1 == null & hstr2 == null ) {
        text = "";
    } else {
        text = (hstr1 == null ? "??" : hstr1) + " " +
               ResourceLoader.getDisplay( "label.on" ) + " " +
               (hstr2 == null ? "??" : hstr2);
    }
    return text;
}

/**
 * Returns the name of the most recent used program to modify
 * this file.
 * 
 * @return String last usage program name (may be empty)
 */
public String getLastProgram() {
    String text = getHeaderValue( PwsFileHeaderV3.APPLICATION_TYPE );
    text = text == null ? "" : text;
    return text;
}

/** Returns the file modify number as stored in a file header field.
 *  
 * @return int modify number
 */
public int getFileModNumber() {
    return fileModNumber;
}

private class SynchronizedFileIterator implements Iterator<PwsRecord> {
	private Iterator<PwsRecord> it;
	private PwsRecord next;
	private PwsRecord record;

	public SynchronizedFileIterator () {
		synchronized ( dbf ) {
			it = dbf.iterator();
			if ( it.hasNext() ) {
			   next = it.next();
			}
		}
	}
	
	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public PwsRecord next() {
		record = next;
		synchronized ( dbf ) {
			next = it.hasNext() ? it.next() : null; 
		}
		return record;
	}

	@Override
	public void remove() {
		deleteRecord(record);
	}
}

}
