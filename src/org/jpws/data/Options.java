/*
 *  Options in org.jpws.front
 *  file: Options.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 29.06.2005
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

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jpws.front.GUIService;
import org.jpws.front.Global;
import org.jpws.front.PwsFileContainer;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.exception.ApplicationFailureException;
import org.jpws.pwslib.global.Log;

/**
 *  Static singleton class to provide and persistently store program options.
 *  <p>This class implements the <code>PersistentOptions</code> interface
 *  although the static methods don't allow its declaration.
 */

public class Options 
{
   public static byte[] randomSeed;
   public static boolean wasReset = false;

   private static Preferences systemUserPrefs = Preferences.userRoot().node( "/org/jpws/data/options" );
   private static ArrayList<OptionChangeListener> listeners = new ArrayList<OptionChangeListener>();
   private static Properties  defaults = new Properties();
   private static OptionBag   obag = new OptionBag( defaults );
   private static ContextFile optionFile;
   private static ContextFile persistFile;

   // this STATIC sets the default values for user selectable options
   static {
      String hstr;
      
      // bools general
      defaults.setProperty( "useRecentList", "true" );
      defaults.setProperty( "useUndoRedo", "true" );
      defaults.setProperty( "reopenFile", "true" );
      defaults.setProperty( "autoflush", "false" );
      defaults.setProperty( "monitorSystem", "false" );
      defaults.setProperty( "rememberScreen", "true" );
      defaults.setProperty( "expiryCheck", "false" );
      defaults.setProperty( "checkProjectNews", "true" );
      defaults.setProperty( "storeMinorChanges", "false" );
      defaults.setProperty( "useDataMirrors", "true" );
      defaults.setProperty( "minToTrayIcon", "false" );
      defaults.setProperty( "restrictAccelerators", "true" );
      defaults.setProperty( "seasonalDeco", "true" );

      // bools confirm
      defaults.setProperty( "confirmSave", "false" );
      defaults.setProperty( "confirmBackup", "true" );
      defaults.setProperty( "confirmRevert", "true" );
      defaults.setProperty( "confirmCopyClipboard", "true" );
      defaults.setProperty( "confirmUpdatePass", "true" );
      defaults.setProperty( "confirmDeleteRecord", "true" );

      // bools security
      defaults.setProperty( "lockIdleState", "false" );
      defaults.setProperty( "lockMinimize", "false" );
      defaults.setProperty( "clearClipboard", "true" );
      defaults.setProperty( "autoBackup", "true" );
      defaults.setProperty( "createFileCheck", "false" );
      defaults.setProperty( "openPassEdit", "false" );
      defaults.setProperty( "useContainerLockedView", "true" );
      defaults.setProperty( "allowFTPcreate", "false" );
      
      // bools display 
      defaults.setProperty( "autoMinimize", "false" );
      defaults.setProperty( "useFavourites", "true" );
      defaults.setProperty( "useDisplay", "true" );
      defaults.setProperty( "treeUsername", "true" );
      defaults.setProperty( "useTableColors", "true" );
      defaults.setProperty( "autoExpandTree", "false" );
      defaults.setProperty( "editFullNotes", "false" );
      defaults.setProperty( "editLineWrap", "false" );
      defaults.setProperty( "editActiveHistory", "true" );
      defaults.setProperty( "logicalFilenames", "true" );
      
      // bools main toolbar
      defaults.setProperty( "toolbar.new", "true" );
      defaults.setProperty( "toolbar.open", "true" );
      defaults.setProperty( "toolbar.save", "true" );
      defaults.setProperty( "toolbar.copypass", "true" );
      defaults.setProperty( "toolbar.copyuser", "true" );
      defaults.setProperty( "toolbar.webstart.record", "true" );
      defaults.setProperty( "toolbar.filter-favourites", "true" );
      defaults.setProperty( "toolbar.add", "true" );
      defaults.setProperty( "toolbar.edit", "true" );
      defaults.setProperty( "toolbar.delete", "true" );
      defaults.setProperty( "toolbar.toggle-desktop", "true" );
      
      // bools reports
      defaults.setProperty( "useEntryOnBrowse", "false" );
      defaults.setProperty( "useEntryOnOpen", "false" );
      
      // strings
//      defaults.setProperty( "lookAndFeel", Global.LAF_NATIVE_OPTION );
      defaults.setProperty( "locale", "en" );
      hstr = System.getProperty("user.language").toLowerCase();
      if ( hstr.equals( "de" ) || hstr.equals( "es" ) ) {
         defaults.setProperty( "locale", hstr );
      }
      
      // integers
      defaults.setProperty( "defaultViewType", String.valueOf(PwsFileContainer.TREE_VIEW) );
      defaults.setProperty( "maxIdleTime", String.valueOf( Global.DEFAULT_MAXIDLE ) );
      defaults.setProperty( "maxAutoMinTime", String.valueOf( Global.DEFAULT_MAXIDLE ) );
      defaults.setProperty( "clipboardTime", String.valueOf( Global.DEFAULT_MAXCLIPBOARD ) );
      defaults.setProperty( "expireScope", String.valueOf( Global.DEFAULT_EXPIRESCOPE ) );
      defaults.setProperty( "maxUndoEntries", String.valueOf( Global.DEFAULT_MAXUNDO ) );
      defaults.setProperty( "autoBackupFiles", String.valueOf( Global.DEFAULT_AUTOBACKUPS ) );
      defaults.setProperty( "usedEntryListLength", String.valueOf( Global.DEFAULT_USEDENTRYLISTLENGTH ) );
      defaults.setProperty( "newFileSecurity", String.valueOf( Global.DEFAULT_FILESECURITYLOOPS ) );
      defaults.setProperty( "viewCurtainTime", String.valueOf( Global.DEFAULT_CURTAIN_MINUTES ) );
   }  // static
   
   /** Initialises this service with attempting to load the parameter file.
    *  If the parameter file cannot be opened, a standard file is assumed
    *  instead.
    * 
    * @param file specifies the JPWS options file to load;
    *        may  be <b>null</b>
    */
   public static void init ( ContextFile file )
   {
      ByteArrayInputStream bin;
      InputStream in;
      ContextFile stddFile, f;
      byte[] buffer;
      int i, length;
      
      f = null;
      stddFile = IOManager.makeLocalContextFile( Global.applHomeDir, 
                 Global.OPTIONFILENAME );
      optionFile = IOManager.makeContextFile(file);

      // try to load option values from application file
      for ( i = 0; i < 2; i++ ) {
         try {
            f = i == 0 ? file : stddFile;
            if ( f != null ) {
               // read file content into buffer
               System.out.println( "-- attempting OPTION FILE: " + f.getFilepath() );
               length = (int)f.length();
               buffer = new byte[ length ];
               in = f.getInputStream();
               in.read( buffer );
               in.close();
               persistFile = f;
               
               // create SHA-1 over file content (used for random generators)
               randomSeed = org.jpws.pwslib.global.Util.fingerPrint( buffer );

               // transform buffer into de-scattered form
               Util.scatter( buffer, length, false );
//               System.out.println();
//               System.out.println( new String( buffer ) );
//               System.out.println();
               
               // load properties
               bin = new ByteArrayInputStream( buffer );
               obag.load( bin, "UTF-8" );
               
               // control validity of dataset
               if ( !getOption( "optionkind" ).equals( "0002" ) ) {
                  reset();
                  System.out.println( "*** OPTIONS INVALID: " + f.getFilepath() );
                  if ( i == 1 ) {
                     System.out.println( "- assuming option defaults instead" );
                  }
                  continue;
               }
               break;
            }

         } catch ( IOException e ) {
            System.err.println("*** CANNOT LOAD PROGRAM OPTIONS under : " + f.getFilepath() ); 
            System.err.println( e );
            if ( f.equals( stddFile ) ) break;
         }
      }
      
      // read some essential values from the system user Preferences
      // only in PORTABLE modus
      if ( Global.isPortable() ) {
         retrieveSystemPreferences();
      }
   }  // init
   
   /** This resets Options to the default values. */
   public static void reset ()
   {
      wasReset = true;
      obag.clear();
   }
   
   /** Saves the actual content of these options to a persistent file.
    */
   public static void save ()
   {
      ContextFile stddFile;
      
      if ( !isModified() ) return;
      
      obag.setOption( "optionkind", "0002" );
      obag.setHeadText( "JPASSWORDS PREFERENCES V. 0002" );
      stddFile = IOManager.makeLocalContextFile( Global.applHomeDir, Global.OPTIONFILENAME );
      
      // store properties into array buffer
      byte[] buffer = obag.toByteArray( "UTF-8" );

      // transform buffer into scattered version
      Util.scatter( buffer, buffer.length, true );
      
      // try to write option values to application file
      for ( int i = 0; i < 2; i++ ) {
         try {
            // store transformed buffer onto file
        	ContextFile f = i == 0 ? optionFile : stddFile; 
            if ( f != null ) {
               Log.log( 5, "(Options.save) saving Options to ".concat( f.getFilepath() ) );
               OutputStream out = f.getOutputStream();
               out.write( buffer );
               out.close();
               persistFile = f;
               obag.resetModified();
               break;
            }

         } catch ( IOException e ) {
            GUIService.failureMessage( "msg.failsaveprefs", e );
            if ( optionFile != null && optionFile.equals( stddFile ) )
               break;
         }
      }
      
      // after file output, condense some values for the system Preferences mechanism
      // only in PORTABLE modus
      if ( Global.isPortable() ) {
         saveSystemPreferences();
      }
   }  // save
   
   /** Saves some essential values reflecting user choices to the Java Preferences scheme.
    */
   private static void saveSystemPreferences ()
   {
      // mainframe bounds
      transferOptionToSystemPrefs( "mainframe-framedim" );
      transferOptionToSystemPrefs( "mainframe-framepos" );
      
      // remember screen positions
      transferOptionToSystemPrefs( "rememberScreen" );
      
      // locking to IDLE state and minimise
      transferOptionToSystemPrefs( "lockIdleState" );
      transferOptionToSystemPrefs( "lockMinimize" );
      transferOptionToSystemPrefs( "autoMinimize" );
      
      // darkness
      transferOptionToSystemPrefs( "useContainerLockedView" );
      
      // system tray usage
      transferOptionToSystemPrefs( "minToTrayIcon" );
      
      // browser path
      transferOptionToSystemPrefs( "browserApplication" );
      
      // GUI-style (LAF class)
      transferOptionToSystemPrefs( "lookAndFeel" );
      
      // Application standard fonts
      transferOptionToSystemPrefs( "Font.menu" );
      transferOptionToSystemPrefs( "Font.data" );
      transferOptionToSystemPrefs( "Font.notes" );
      transferOptionToSystemPrefs( "Font.password" );
      transferOptionToSystemPrefs( "Font.display" );
      transferOptionToSystemPrefs( "Font.control" );
      transferOptionToSystemPrefs( "Font.tooltip" );

      // save data
      try { systemUserPrefs.flush(); }
      catch ( BackingStoreException e ) {
         e.printStackTrace();
      }
   }

   /** Reads some essential values reflecting user choices from the Java 
    * Preferences scheme. This serves the PORTABLE MODUS settings.
    */
   private static void retrieveSystemPreferences ()
   {
      // mainframe bounds
      transferOptionFromSystemPrefs( "mainframe-framedim" );
      transferOptionFromSystemPrefs( "mainframe-framepos" );
      
      // remember screen positions
      transferOptionFromSystemPrefs( "rememberScreen" );
      
      // locking to IDLE state and minimise
      transferOptionFromSystemPrefs( "lockIdleState" );
      transferOptionFromSystemPrefs( "lockMinimize" );
      transferOptionFromSystemPrefs( "autoMinimize" );
      
      // darkness
      transferOptionFromSystemPrefs( "useContainerLockedView" );
      
      // system tray usage
      transferOptionFromSystemPrefs( "minToTrayIcon" );
      
      // browser path
      transferOptionFromSystemPrefs( "browserApplication" );
      
      // GUI-style (LAF class)
      transferOptionFromSystemPrefs( "lookAndFeel" );
      
      // Application standard fonts
      transferOptionFromSystemPrefs( "Font.menu" );
      transferOptionFromSystemPrefs( "Font.data" );
      transferOptionFromSystemPrefs( "Font.notes" );
      transferOptionFromSystemPrefs( "Font.password" );
      transferOptionFromSystemPrefs( "Font.display" );
      transferOptionFromSystemPrefs( "Font.control" );
      transferOptionFromSystemPrefs( "Font.tooltip" );
   }

   private static void transferOptionFromSystemPrefs ( String key )
   {
      setOption( key, systemUserPrefs.get( key, Options.getOption( key ) ));
   }
   
   private static void transferOptionToSystemPrefs ( String key )
   {
      systemUserPrefs.put( key, getOption( key ) );
   }
   
   /** Saves a blank version of JPWS INI-file to a persistent file.
    * @throws IOException 
    * @throws ApplicationFailureException 
    */
   public static void saveNew ( ContextFile f ) throws IOException
   {
      if ( f == null )
         throw new NullPointerException();
      
      // set minimum values
      OptionBag bag = new OptionBag();
      bag.setOption( "optionkind", "0002" );
      bag.setHeadText( "JPASSWORDS PREFERENCES V. 0002" );
      byte[] buffer = bag.toByteArray( "UTF-8" );
      Util.scatter( buffer, buffer.length, true );
      
      // write file
      OutputStream out = IOManager.getOutputStream( f );
      out.write( buffer );
      out.close();
   }
   
   /** Whether the specified (boolean) option is set to "true". */
   public static boolean isOptionSet ( String token )
   {
      return obag.isOptionSet( token );
   }

   /** Whether options have been modified since last 
    *  loading or saving.
    * @return boolean
    */
   public static boolean isModified ()
   {
      return obag.isModified();
   }
   
   /** Sets the default option mapping for a given token. 
    * @since 0-5-0
    */ 
   public static void setDefaultOption ( String token, String value )
   {
      defaults.setProperty( token, value );
   }
   
   /** Sets a boolean value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned boolean value
    */
   public static boolean setOption ( String token, boolean value )
   {
      return setOption( token, Boolean.toString( value ) );
   }

   /** Sets a string value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned string value, may be <b>null</b> to clear
    */
   public static boolean setOption ( String token, String value )
   {
      String oldValue = obag.getProperty( token );
      boolean mod = obag.setOption( token, value );
      
      // fire option change event if and only if 
      // the mapping token -> value has changed
      if ( mod )
         fireChangeEvent( token, oldValue, value );

      return mod;
   }

   /** Returns the mapped option string value or empty string if the option is
    *  undefined.
    * 
    * @param token the option name
    * @return option value string or "" if undefined
    */
   public static String getOption ( String token )
   {
      return obag.getOption( token );
   }
   
   /** Returns the persistent file defined to hold the 
    * values of this option bag.
    * 
    * @return ContextFile or <b>null</b> if no file is defined
    * @since 0-5-0
    */ 
   public static ContextFile getPersistentFile ()
   {
      return persistFile;
   }
   
   
   
   /** Returns the mapped option integer value or 0 if the option is
    *  undefined.
    * @param token the option name
    * @return int
    */
   public static int getIntOption ( String token )
   {
      return obag.getIntOption( token );
   }  // getIntOption

   /** Returns the mapped option long integer value or 0 if the option is
    *  undefined.
    * @param token the option name
    * @return long
    */
   public static long getLongOption ( String token )
   {
      return obag.getLongOption( token );
   }  // getLongOption

   /** Sets an integer value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned int value
    */
   public static boolean setIntOption ( String token, int value )
   {
      return setOption( token, Integer.toString( value ) );
   }

   /** Sets a long integer value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned long value
    */
   public static boolean setLongOption ( String token, long value )
   {
      return setOption( token, Long.toString( value ) );
   }
   
   /** Returns a singleton instance of the <code>PersistentOptions</code> 
    * interface which directs all calls to the static <code>Options</code>
    * class.
    * 
    * @return <code>PersistentOptions</code>
    * @since 0-5-0
    */
   public static PersistentOptions getOptions ()
   {
      return obag;
   }

   /** Sets a bounds object (e.g. from a window or dialog) to be
    * associated with the token string.
    * 
    * @param token the option name
    * @param bounds <code>Rectangle</code>
    * @since 0-5-0
    */
   public static boolean setBounds ( String token, Rectangle bounds )
   {
      boolean mod = obag.setBounds( token, bounds );
      return mod;
   }
   
   /** Returns the mapped bounds object (e.g. for a window) or 
    * <b>null</b> if this option is undefined.
    *  
    * @param token the option name
    * @return <code>Rectangle</code> specifying position and size of a window
    * @throws NullPointerException if parameter is <b>null</b> 
    * @since 0-5-0
    */
   public static Rectangle getBounds ( String token )
   {
      return obag.getBounds( token );
   }
   
   /**
    * Stores a list of string representations of objects
    * in options. The <code>toString()</code> method is used on each 
    * object to derive its representation value. (NOTE:
    * this is not necessarily a serialization of objects themselves!)    
    * 
    * @param name String name of the list to store
    * @param ls List list of objects, use <b>null</b> to clear
    * @since 0-6-0
    */
   public static boolean setStringList ( String name, List<Object> ls )
   {
      return obag.setStringList( name, ls );
   }
   
   /**
    * Retrieves a list of strings that has been previously stored
    * into options by <code>setStringList()</code>. If no value was
    * found for <b>name</b>, an empty list is returned.
    * 
    * @param name String name of the list
    * @return List a list of retrieved String values (may be empty)
    * @since 0-6-0
    */
   public static List<String> getStringList ( String name )
   {
      return obag.getStringList( name );
   }
   
   /** Returns the STORETIME value from this option set's METADATA.
    * 
    * @return long time in milliseconds or 0 if undefined
    */
   public static long getStoreTime() {
		return obag.getStoreTime();
	}

   /** Puts the system's browser application path into Options, respecting
    *  additionally measures to service the browser paths recent list.
    *  (The value tokens are: "browserApplication" for actual browser path
    *  and "browserAppRecents" for the browser path recent list.) 
    *  
    * @param path actual browser path
    * @param prevPath browser path previous to the actual path (may be <b>null</b>)
    * @since 0-5-0
   public static void setBrowserApplication ( String path, String prevPath )
   {
      RecentList rlist;
      
      Options.setOption( "browserApplication", path );
      if ( new ContextFile( path ).isFile() )
      {
         rlist = new RecentList();
         rlist.setContent( Options.getOption( "browserAppRecents" ) );
         if ( prevPath != null )
            rlist.pushRecent( prevPath );
         if ( path.length() != 0 )
            rlist.pushRecent( path );
         Options.setOption( "browserAppRecents", rlist.getStringContent() );
      }
   }
    */ 
   
   /** Returns the value of the system's actual browser application path or the 
    * empty string if undefined. (Option token: "browserApplication")
   public static String getBrowserApplication ()
   {
      return getOption( "browserApplication" ); 
   }
    */ 

   /**
    * Adds a listener to modifications of option - value mappings.
    * @param li <code>OptionChangeListener</code>
    * @since 0-6-0
    */
   public static void addChangeListener ( OptionChangeListener li ) {
      if ( !listeners.contains( li ) )
         listeners.add( li );
   }
   
   /**
    * Removes a listener to modifications of option - value mappings.
    * @param li <code>OptionChangeListener</code>
    * @since 0-6-0
    */
   public static void removeChangeListener ( OptionChangeListener li ) {
      listeners.remove( li );
   }
   
   private static void fireChangeEvent ( String name,  String oldValue, String value ) {
      OptionChangeEvent evt = new OptionChangeEvent(Options.class, name, oldValue, value);
      for ( int i = 0; i < listeners.size(); i++ ) {
         listeners.get( i ).optionChanged( evt );
      }
   }
   
private Options ()
{}

}
