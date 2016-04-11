/*
 *  Global in org.jpws.front
 *  file: Global.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 03.09.2004
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

package org.jpws.front;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Timer;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import org.jpws.data.IOManager;
import org.jpws.data.LoggingPrintStream;
import org.jpws.data.OptionChangeEvent;
import org.jpws.data.OptionChangeListener;
import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket;
import org.jpws.front.DatabaseHandler.FileAccessModus;
import org.jpws.front.edit.EditorDialog;
import org.jpws.front.util.DateTimeFormat;
import org.jpws.front.util.DateTimeFormat.DateFormat;
import org.jpws.front.util.DateTimeFormat.TimeFormat;
import org.jpws.front.util.RecentList;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.TimePulseListener;
import org.jpws.front.util.TimeSlicer;
import org.jpws.front.util.Util;
import org.jpws.pwslib.crypto.CryptoRandom;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.persist.ApplicationAdapter;

/**
 *  Global variables and functions.
 */
public final class Global
{
   /** Name element for program version (release version) */
   public static final String APPLICATION_VERSION = "0.7.1";
   /** Technical version ID of this program  */
   public static final long BUILDVERSION = 701000L;   
   /** Switch to debugging enabled program features */
   private static final boolean IS_DEBUG = false;
   /** Actual debugging and logging level for the Log module */
   public static final int DEBUG_LEVEL = IS_DEBUG ? 10 : 1;   

   /** Milliseconds of a day */
   public static final long DAY = 86400000;  
   /** Milliseconds of an hour */
   public static final long HOUR = 3600000;   
   /** Milliseconds of a minute */
   public static final long MINUTE = 60000;   

   public static final String OS = System.getProperty("os.name","").toLowerCase(); 
   private static final String UNIX_OPTIONFILENAME = ".jpws.ini";
   private static final String WINDOWS_OPTIONFILENAME = "jpws.ini";
   public static final String OPTIONFILENAME = 
                              isUnixDerivate() ? UNIX_OPTIONFILENAME : WINDOWS_OPTIONFILENAME;
   public static final String DEFAULT_FILEEXTENTION = ".dat"; 
   public static final String DEFAULT_PWSFILEEXTENTION = ".psafe3"; 
   public static final String DEFAULT_BACKUPEXTENTION = ".bak"; 
   public static final String DEFAULT_APPLHOMEDIR = ".jpws"; 
   public static final String DEFAULT_BACKUPDIR = "backup"; 
   public static final String DEFAULT_MIRRORDIR = "mirror"; 
   public static final String DEFAULT_FILEDIR = "files"; 
   /** Display title, including version, of this program. */
   public static final String APPLICATION_TITLE = "JPasswords - " + APPLICATION_VERSION;
   /** Name element for program version (release version) */
   public static final String APPL_VERSION_PRG = APPLICATION_VERSION.replace('.', '-').replace(' ', '-');
   public static final String EXE_PROGRAMNAME_1 = "jpws.exe"; 
   public static final String EXE_PROGRAMNAME_2 = "jpws" + "-" + APPL_VERSION_PRG + ".exe"; 
   public static final String JAR_PROGRAMNAME_1 = "jpws.jar"; 
   public static final String JAR_PROGRAMNAME_2 = "jpws" + "-" + APPL_VERSION_PRG + ".jar"; 
   public static final String LAF_NATIVE_OPTION = "** NATIVE **"; 
   public static final String LAF_CROSSPLATFORM_OPTION = "** CROSS-PLATFORM **";
   public static final String DEFAULT_LOOK_AND_FEEL = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
   public static final String SERVER_PARCEL_URL = "http://jpws.sourceforge.net/support/jpwsinfo.properties";
   public static final String[] SUPPORTED_LANGUAGES = { "en", "de", "es" };

   public static final int PARCEL_CHECK_DAYS = 1;
   public static final int BASIC_SECURITY_LOOPS = 2048;  
   public static final int DEFAULT_FILESECURITYLOOPS = 20480;  
   public static final int MIRROR_CHECKING_PERIOD = 2 * 60;  // 2 minutes (in seconds)  
   public static final int RECENTFIND_LIST_LENGTH = 64;  
   public static final long DEFAULT_MAXIDLE = 600000;  // 10 minutes 
   public static final long DEFAULT_MAXCLIPBOARD = 30000;  // 30 seconds 
   public static final long DEFAULT_EXPIRESCOPE = 20 * DAY;  // 20 days
   public static final int DEFAULT_MAXUNDO = 25;  // max entries in UNDO/REDO list
   public static final int DEFAULT_AUTOBACKUPS = 10;  // max auto-backups of modified databases
   public static final int DEFAULT_USEDENTRYLISTLENGTH = 10;  // default length of RECENT USED ENTRIES list
   public static final int DEFAULT_CURTAIN_MINUTES = 5;
   public static final int FILEVERSION_1 = org.jpws.pwslib.global.Global.FILEVERSION_1;
   public static final int FILEVERSION_2 = org.jpws.pwslib.global.Global.FILEVERSION_2;
   public static final int FILEVERSION_3 = org.jpws.pwslib.global.Global.FILEVERSION_3;
   public static final int FILEVERSION_LATEST = org.jpws.pwslib.global.Global.FILEVERSION_LATEST_MAJOR;

   public static Locale                 locale;
   public static String[]               commandlineArgs;
   public static Clipboard              clipboard;
   public static ActionListener         mainActionListener;
   public static PwsafeJ                mainFrame;  
   public static File                   currentDir;
   public static File                   backDir;
   public static File                   exchangeDir;
   public static File                   programDir;
   public static File                   portableDir;
   public static File                   applHomeDir;
   public static File                   defaultFileDir;
   public static File                   mirrorDir;

   public static RecentList             recentFiles;
   public static RecentList             recentFinds;
   public static RecentList             randomMirrors  = new RecentList(1024);
   public static PwsPassphrasePolicy    passphrasePolicy;
   public static PwsPassphrasePolicy    generatorPolicy;
   public static FileMemory             exportFileMem = new FileMemory(5);
   public static Set<KeyStroke> 		forbiddenKeys;

//   private static OptionListener        optionListener = new OptionListener();
   private static ServiceDemon          serviceDemon;
   private static ShutdownThread        shutdownThread = new ShutdownThread();
   private static LoggingPrintStream    logPrinter = new LoggingPrintStream( System.out );
   private static Timer                 timer = new Timer();
   private static TimeSlicer            blinkPulse = new TimeSlicer( 6, 3 );
   private static DateTimeFormat        dateTimeFormat = new DateTimeFormat();
   private static HashMap<String,Object>    criticalMap = new HashMap<String,Object>();
   private static Hashtable<String,String>  activeDialogs = new Hashtable<String,String>();
   private static String                startupFilepath;
   private static long                  maxClipboardTime;
   private static long                  maxIdleTime;
   private static long                  maxAutoMinTime;
   private static long                  clipboardTime;

   private static boolean               hasClipboardTransfer;
   private static boolean               optionsRead;
   private static boolean               isWindows;
   private static boolean               isPortable;
   private static boolean               isShutdown;
   private static boolean               isInited;

   static {
	   HashSet<KeyStroke> keyset = new HashSet<KeyStroke>();
	   String collect = "DELETE ENTER SPACE ESCAPE UP DOWN LEFT RIGHT KP_UP KP_DOWN KP_LEFT KP_RIGHT PAGE_UP PAGE_DOWN HOME END INSERT BACK_SPACE PRINTSCREEN SCROLL_LOCK CAPS_LOCK PAUSE NUM_LOCK TAB SHIFT CONTROL ALT ALT_GRAPH WINDOWS META CONTEXT_MENU DEAD_ACUTE DEAD_GRAVE";
	   for (String key : collect.split(" ")) {
		   KeyStroke stroke = KeyStroke.getKeyStroke(key);
		   keyset.add(stroke); 
	   }
	   forbiddenKeys = Collections.unmodifiableSet(keyset);
   }
   
   /**
    * The calls in this method are performed at program start *after*
    * GUI has been established. You can think of it as a functional 
    * startup batch. 
    */
   public static void autorun ()
   {
      String hstr;
      long time, lastCheckTime;
      long actualTime = System.currentTimeMillis();

      // TEST DATE-TIME 
//      DateTimeFormat.test_output();
      
      // welcome message
      if ( (time = (long)Options.getIntOption( "lastAccessTime" ) * 1000) > 0 ) {
         hstr = ResourceLoader.getDisplay("msg.welcome") + " " + getLocalDateTime(time);
         setStatusText( hstr );
      }

      // check for Options reset
      if ( Options.wasReset ) {
         GUIService.warningMessage( null, null, "msg.warning.optionsreset" );
      }
      
      // start any commandline start file
      if ( startupFilepath != null ) {
         DatabaseHandler.openFileToShelf( startupFilepath );
      }
      
      // or offer opening of most recent database
      else if ( Options.isOptionSet( "reopenFile" ) ) {
    	  DatabaseHandler.reopenFileFromRecents();
      }
      
      // open random mirror files (unsaved new dbs)
      Service.controlRandomMirrors();
      
      // perform check for online news (project server; period: 3 days) 
      lastCheckTime = Options.getLongOption( "lastProjectCheckTime" );
      if ( Options.isOptionSet( "checkProjectNews" ) && 
           (actualTime - lastCheckTime > PARCEL_CHECK_DAYS * Global.DAY ) )
      {
         Runnable run = new Runnable() {
            @Override
			public void run () {
               Service.controlProjectNews( false );
            }
         };
         ActionHandler.startTaskDelayed( run, 2 * MINUTE );
      }
   }
   
   /** Returns the (best guess) for the location of the program files.
    * @return String directory path of application in canonical form
    * @since 0-5-0
    */   
   private static String getProgramPath ()
   {
      String path, hstr;
      int i;
      char sep;
      
      if ( (path = System.getProperty( "java.class.path" )) != null ) {
          System.out.println( "# CLASS PATH: [" + path + "]");
          System.out.println( "# VM Name: " + System.getProperty( "java.vm.name" ) );
          System.out.println( "# VM Vendor: " + System.getProperty( "java.vm.vendor" ) );
          
         // extract first path string
         sep = isWindows() ? ';' : ':';
         if ( (i = path.indexOf(sep)) != -1 )
            path = path.substring( 0, i );
         path = path.trim();
         
         // get canonical value 
         if ( path != null && path.length() > 0 ) {
            // get canonical (this may resolve a symbolic link of caller)
            try
            { path = new File( path ).getCanonicalPath(); }
            catch ( IOException e )
            { e.printStackTrace(); }
         
            // take parent dir if path denotes JAR or EXE file
            hstr = path.toLowerCase();
            if ( hstr.endsWith(".jar") || hstr.endsWith( ".exe" ) )
               path = Util.pathNameOfPath( path );
         }
      }
      
      // if no path found in java.class.path then take user.dir 
      if ( path == null || path.length() == 0 || !new File( path ).isDirectory() ) {
         path = System.getProperty("user.dir");
         System.out.println( "# *** no valid class path found; assuming USER DIR as program dir ***");
      }

      Log.debug( 8, "(Global.getProgramPath)--- rendered path: [" + path + "]");
      return path;
   }

   /** Investigates the program directory, or optional the given
    * specific directory, if there exists either
    * a Unix or a Windows option file and returns it. 
    * If the given path does not denote an existing file/dir,
    * the program directory is investigated instead. If it denotes
    * an existing directory, it is investigated. If it denotes
    * an existing file, it is assumed as PORTABLE ROOT INI file
    * and returned!
    * <p>Assumes valid: <code>Global.programDir</code>
    * 
    * @param path String filepath denoting a directory or a specific
    *        JPWS INI file; if <b>null</b> or does not denote and
    *        existing structure, the program directory is 
    *        used instead 
    * 
    * @return File existing option file (canonical form) or <b>null</b> otherwise
    * @since 0-5-0; 0-6-0 modified
   private static File getPortableInitFile ( String path )
   {
      File f, searchDir;

      searchDir = null;

      // see if we have a specific file/directory to search
      // (only with "/p" switch set)
      try {
         if ( path != null && !path.isEmpty() && hasCLArgument( "/p" ) )
         {
            if ( (f = new File( path )).isFile() )
               return f.getCanonicalFile();
            
            searchDir = f;
         }
      }
      catch ( Exception e )
      { e.printStackTrace(); }
      
      // investigate the search directory
      if ( searchDir == null || !searchDir.isDirectory() )
         searchDir = programDir;

      Log.debug( 5, "(Global.getPortableInitFile) searching INI-FILE in directory: "
            .concat( searchDir.getPath()  ) );
      return getNormalInitFile( searchDir.getAbsolutePath() );
   }
*/


   /** Investigates the parameter path whether it holds an existing
    *  JPWS init file (program options). If the file exists 
    * it is returned, otherwise <b>null</b>. The file can be either
    * a Unix or a Windows option file (where the type of the current
    * session type (Windows or Unix) is preferred).
    * <p>Detail: if the path is <b>null</b> or empty, <b>null</b> is
    * returned. If <code>path</code> signifies a directory, 
    * that directory is searched. Otherwise a specific file is assumed.
    *   
    *
    * @param File iniPath; specific ini-file or directory containing
    *        the ini-file; may be <b>null</b>
    * @return File, existing option file (canonical form) or <b>null</b> 
    *         if that file wasn't found
    * @since 0-6-0
    */
   private static File findInitFile ( File iniPath )
   {
      File unixIni, windowsIni;

      try {
         // evaluate a parameter path
         if ( iniPath != null )
         {
            // may find the specific file
            if ( iniPath.isFile() )
               return iniPath.getCanonicalFile();

            // may search the given directory
            if ( iniPath.isDirectory() )
            {
               // investigate a directory
               unixIni = new File( iniPath, UNIX_OPTIONFILENAME ).getCanonicalFile();
               windowsIni = new File( iniPath, WINDOWS_OPTIONFILENAME ).getCanonicalFile();

               // first option test
               if ( isWindows() && windowsIni.isFile() )
                  return windowsIni;
               if ( isUnixDerivate() && unixIni.isFile() )
                  return unixIni;
               
               // second option test
               if ( windowsIni.isFile() )
                  return windowsIni;
               if ( unixIni.isFile() )
                  return unixIni;
            }
         }
      }
      catch ( IOException e )
      { e.printStackTrace(); }
      return null;
   }
   
   /** Marks a random mirror filepath for global persistence.
    * 
    * @param file ContextFile mirror file definition
    */
   public static void addRandomMirror ( ContextFile file )
   {
      if ( file != null )
      {
         String name = file.getFileName();
         randomMirrors.pushRecent( name );
         Options.setOption( "randomMirrorNames", randomMirrors.getStringContent() );
         Log.log(5, "(Global.addRandomMirror) adding random mirror: ".concat(file.getFilepath()) );
      }
   }

   /** Deletes the parameter file on the medium and removes it from global 
    * random mirror list. (Random mirrors are created for databases without
    * a filepath definition.)
    * 
    * @param file ContextFile mirror file definition
    * @throws IOException 
    */
   public static void removeRandomMirror ( ContextFile file ) throws IOException
   {
      if ( file != null )
      {
         // remove the persistent file 
         file.delete();

         // remove the list entry (file name only)
//         String path = file.getFilepath();
         randomMirrors.removeRecent( file.getFileName() );
         Options.setOption( "randomMirrorNames", randomMirrors.getStringContent() );
         Log.log(5, "(Global.removeRandomMirror) removing random mirror: ".concat(file.getFilepath()) );
      }
   }

   /** Returns an iterator over all registered random mirror names
    * (i.e. of mirrors for new databases without path definition).
    * The returned names are file names without path elements.
    *  
    * @return <code>Iterator</code> of String
    */
   @SuppressWarnings("unchecked")
   public static Iterator<String> getRandomMirrorNames () 
   {
       return (Iterator<String>)((RecentList)randomMirrors.clone()).iterator();
   }
   
   /** A human readable text expression for the default locale.
    * @since 0-5-0
    */
   public static String getLocaleString ()
   {
      String hstr;
      
      hstr = Locale.getDefault().getDisplayLanguage( Locale.ENGLISH ) + ", " +
             Locale.getDefault().getDisplayCountry( Locale.ENGLISH );
      return hstr;
   }
   
   /** Returns the localised date and time text for the parameter
    * universal (epoch) time value.
    * 
    * @param time long milliseconds time value
    * @return String local date-time text
    */
   public static String getLocalDateTime ( long time )
   {
      return dateTimeFormat.renderDateTime(time, null);
   }

   /** Returns the localised date text for the parameter
    * universal (epoch) time value.
    * 
    * @param time long milliseconds time value
    * @return String local date text
    */
   public static String getLocalDate ( long time )
   {
      return dateTimeFormat.renderDate(time, null);
   }

   /** Returns the localised time text for the parameter
    * universal (epoch) time value.
    * 
    * @param time long milliseconds time value
    * @return String local time text
    */
   public static String getLocalTime ( long time )
   {
      return dateTimeFormat.renderTime(time, null);
   }

   /** Retrieves user options for date and time formatting
    * and installs variable "dateFormatOption" correspondingly.
    * (This variable is program-wide responsible for formatting of 
    * date and time display.)
    */
   private static void setDateTimeFormat_after_Options () {
       // retrieve date format from options
       String name = Options.getOption("dateFormatOption");
       DateFormat dfo = DateFormat.forName(name);
       if ( dfo == null )
          dfo = DateFormat.VM_default; 
       
       // retrieve time format from options
       name = Options.getOption("timeFormatOption");
       TimeFormat tfo = TimeFormat.forName(name);
       if ( tfo == null )
          tfo = TimeFormat.VM_default; 

       dateTimeFormat = new DateTimeFormat( dfo, tfo );  
   }
   
   /** This method searches for an existing INI file (program options)
    * and returns its definition if found. This method also determines
    * whether the application is running in NORMAL or in PORTABLE modus.
    * The search for the INI file takes place in different locations 
    * depending on the active program "run modus"; it also takes into account
    * the path of a "/o" program parameter.
    * <p>Assumes valid: <code>Global.programDir</code>
    *  
    * @return <code>File</code> existing INI file or <b>null</b> if
    *         not found
    * @throws IOException 
    */
   private static File getApplInitFile () throws IOException
   {
      File ini, portIni, optIni, oFile, applHome;
      String path;
      
      // test for PORTABLE MODUS of application
      portIni = findInitFile( programDir );
      isPortable = !hasCLArgument( "/n" ) && (hasCLArgument( "/p" ) ||
                   portIni != null);

      // test for existing user opted file ("/o")
      // or investigate any opted directory
      if ( !(path = extractCLArgument( "/o" )).isEmpty() )
      {
         // if user opted for INI, return what we find (or not)
         oFile = new File( path );
         optIni = oFile.isFile() ? oFile.getCanonicalFile() : 
                  oFile.isDirectory() ? findInitFile( oFile ) : null;
         return optIni;
      }
               
      // return the file in PORTABLE modus
      if ( isPortable )
      {
         ini = portIni;
      }
      // search the file in NORMAL modus (application home)
      else
      {
         // search host default location 
         applHome = new File( getUserHome(), DEFAULT_APPLHOMEDIR );
         ini = findInitFile( applHome );
      }
      return ini;
   }
   

   /** This method prepares the system for run modus
    * "PORTABLE" (as opposed to "NORMAL"). It defines and
    * ensures existence of the PORTABLE ROOT directory 
    * (<code>portableDir</code>). It also defines the 
    * application's INI file, if given undefined.
    * <p>Assumes valid: <code>Global.programDir</code>
    * 
    * @param iniFile a specific JPWS INI file or <b>null</b> if missing
    * @return File the valid INI file definition for the PORTABLE modus
    * @throws IOException 
    */
   private static File preparePortableModus ( File iniFile )
   {
      // ensure values for INI-file and PORTABLE ROOT (default if definition missing)
      if ( iniFile == null )
      {
         // this will activate the program directory and create the INI file there
         if ( portableDir == null )
            portableDir = programDir;
         
         iniFile = new File( portableDir, Global.OPTIONFILENAME );
      }
      else
      {
         // this will use a PORTABLE ROOT analysed from the given INI file 
         // if possible or the program dir otherwise
         if ( (portableDir = iniFile.getParentFile()) == null )
            portableDir = programDir;
      }
  
      if ( !Util.ensureDirectory( portableDir, null ) )
      {
         System.out.println( "# *** FATAL ERROR: Could not create/verify PORTABLE directory: "
               .concat( portableDir.getPath() ));
         System.exit( -1 );
      }
      System.out.println( "# PORTABLE modus detected: " + portableDir.getAbsolutePath() );
      return iniFile;
   }
   
   /** Ensures existence of basic and default application structures
    * like e.g. standard directories. 
    */
   private static void ensureApplStructure ()
   {
      // ensure backup directory and implicit application home
      File dir = new File( applHomeDir, DEFAULT_BACKUPDIR );
      Util.ensureDirectory( dir, null );
      // ensure default files directory
      dir = new File( applHomeDir, DEFAULT_FILEDIR );
      Util.ensureDirectory( dir, null );
   }
   
   @SuppressWarnings("unused")
   public static void init ( String[] args )
   {
      List<String> list;
      String hstr, path, country, language;
      File oFile, iniFile;
      int i;
      
      // install our own console-output system
      System.setOut( logPrinter );
      System.setErr( logPrinter );
      
      // greeting message
      System.out.println( "# " + APPLICATION_TITLE );
      System.err.println( "# JPWS-F start, " + System.currentTimeMillis()/1000 + " " +
            (DEBUG_LEVEL > 1 ? Util.standardTimeString( System.currentTimeMillis() ) : ""));
//      System.out.println( "# TimeZone: " + TimeZone.getDefault().getDisplayName() ); 
      //System.out.println("   ->conditions");
      
      // Logging setup
      setupLogging();
      
      // control compliance of running Java Virtual Machine
      controlJavaVersion();
      
      // get start arguments
      list = new ArrayList<String>();
      String cmdLine = "";
      for ( i = 0; i < args.length; i++ ) {
         if ( (hstr = args[i]) != null && hstr.length() > 0 ) {
            list.add( hstr );
            cmdLine += hstr + " ";
         }
      }
      commandlineArgs = new String[ list.size() ];
      list.toArray( commandlineArgs );
      if ( commandlineArgs.length > 0 )
          System.out.println("# ARGS = " + cmdLine);
      
      isWindows = OS.indexOf("windows") > -1;
      
      // detect program start directory
      programDir = new File( getProgramPath() );
      System.out.println( "# Program Path = ".concat( programDir.getAbsolutePath() ));
      
      // ASSERT: -- programDir is guaranteed for canonical form!
/*
      // DEBUG: LIST FILESYSTEM-ROOTS
      File[] roots = File.listRoots();
      if ( roots != null )
         for ( int i = 0; i < roots.length; i++ )
            System.out.println( "*** SYSTEM ROOT-DIR: " + roots[i].getAbsolutePath() );
*/
      
      // init reporting devices
      Reporter.init();

      // COMMANDLINE PARAMETERS
      // detect specific start database (commandline)
      if ( commandlineArgs.length > 0 )
      {
         hstr = commandlineArgs[0];
         if ( !hstr.equalsIgnoreCase( "/p" ) && 
              !hstr.equalsIgnoreCase( "/n" ) && 
              (hstr.length() < 3 || hstr.charAt( 2 ) != ':') )
            startupFilepath = hstr;
      }
      
      // identify program option file 
      iniFile = null;
      try {
         // detect portable modus and existing INI-file
         iniFile = getApplInitFile();
         // ASSERT: PORTABLE/NORMAL modus set (isPortable() valid)

         // create opted INI structures by "/o" parameter (MAY FAIL TO EXIT!) 
         if ( iniFile == null && !(path = extractCLArgument( "/o" )).isEmpty() )
         {
            System.out.println( "# attempting INIT PATH (user option): ".concat( path ) );
            oFile = new File( path ).getCanonicalFile();
            if ( path.toLowerCase().endsWith( ".ini" ) )
               iniFile = oFile;
            else
            {
               iniFile = new File( oFile, Global.OPTIONFILENAME );
            }
            // ensure directory of the INI file
            Util.ensureDirectory( iniFile.getParentFile(), null );
         }

      } 
      catch ( IOException e )
      { e.printStackTrace(); System.exit( -1 ); }
      
      // prepare the PORTABLE MODUS
      if ( isPortable() ) {
         preparePortableModus( iniFile );
      }
      // ASSERT: -- portableDir is guaranteed for canonical form! (may be null)

      // directory preparations (environment)
      applHomeDir = new File( isPortable ? portableDir : getUserHome(), DEFAULT_APPLHOMEDIR );
      // ASSERT: -- applHomeDir is guaranteed for canonical form!
      
      mirrorDir = new File( applHomeDir, DEFAULT_MIRRORDIR );
      // ASSERT: -- mirrorDir is guaranteed for canonical form!
      
      defaultFileDir = new File( applHomeDir, DEFAULT_FILEDIR );
      // ASSERT: -- defaultFileDir is guaranteed for canonical form!
      
      System.out.println( "# Application Home = ".concat( applHomeDir.getAbsolutePath() ));
      
      // define the INI file (if missing)
      if ( iniFile == null ) {
         iniFile = new File( isPortable() ? programDir : applHomeDir, Global.OPTIONFILENAME );
      }
      // ASSERT: -- iniFile is guaranteed in canonical form!

      ensureApplStructure();
      
      // read program options
      readOptions( IOManager.makeLocalContextFile( iniFile ) );
      Options.addChangeListener( new OptionListener() );
      activateLoggingFile();
      
      // detect locale settings
      // take country setting from CL (1.) or default locale (2.)
      locale = Locale.getDefault();
      country = (hstr=extractCLArgument( "/c" ).toUpperCase()).equals( "" ) ? locale.getCountry() : hstr; 
      if ( !Util.isArrayElement( country, Locale.getISOCountries() ) ) {
         System.out.println( "# COUNTRY PARAMETER is invalid code: ".concat( country ));
         System.out.println( "# COUNTRY reset to: ".concat( locale.getCountry() ));
         country = locale.getCountry();
      }
      // take language setting from CL (1.) or program options (2.) or default locale (3.)
      if ( (hstr=extractCLArgument( "/l" ).toLowerCase()).equals( "" ) )
         hstr = Options.getOption( "locale" ).toLowerCase();
      language = hstr.equals( "" ) ? locale.getLanguage() : hstr;
      if ( !Util.isArrayElement( language, Locale.getISOLanguages() )) {
         System.out.println( "# LANGUAGE PARAMETER is invalid code: ".concat( language ));
         System.out.println( "# LANGUAGE reset to: ".concat( locale.getLanguage() ));
         language = locale.getLanguage();
      }
      if ( !Util.isArrayElement( language, SUPPORTED_LANGUAGES )) {
         System.out.println( "# LANGUAGE " + language + " is not supported, switching to \"EN\" instead" ); 
         language = "EN";
      }
      locale = new Locale( language, country );
      
      // install an optional logging file output
      if ( Options.isOptionSet( "monitorSystem" ) ) {
         File logF = new File( applHomeDir, "jpws.log" );
         logPrinter.setOutputFile( logF );
         System.out.println( "# Logging enabled to: ".concat( logF.getAbsolutePath() ));
      }
      
/*      
      // DEBUG INFO
      Locale[] locales = Locale.getAvailableLocales();
      for ( int i=0; i<locales.length; i++ )
         System.out.println( "+ Available Locale: " + locales[i].getDisplayLanguage() 
               + ", " + locales[i].getDisplayCountry() );
      System.out.println();
*/
      
      // init JVM
      Locale.setDefault( locale );
      System.out.println( "# Locale: " + getLocaleString() ); 
      System.out.println( "# TimeZone: " + TimeZone.getDefault().getDisplayName() );
      
      // open resource bundles (may cause program exit)
      ResourceLoader.addResourcePath( "org/jpws/front/resource/" );
      ResourceLoader.addResourcePath( "" );
      ResourceLoader.init ( null );
      
/*
      String[][] tokens = {
            { "$SYSFULL$", "JPasswords" },
            { "$SYSSHORT$", "JPWS" },
            { "$SYSVER$", "B 0.0.0" }
            };
      ResourceLoader.setTokens ( tokens );
*/      
      
//      URL url = ResourceLoader.getResourceURL( "#images/greenba2.gif" );
//      System.out.println( "*** Sample Resource URL: " + url.toString() );
      
      
      SystemDesktopHandler.get();
      
      // init backstage libraries
      try {
         org.jpws.pwslib.global.Global.getDefaultCharset();
         Log.setDebugLevel( DEBUG_LEVEL );
         Log.setLogLevel( DEBUG_LEVEL );
         org.jpws.pwslib.global.Global.setDisplayUsernames(
               Options.isOptionSet( "treeUsername" ) );
         org.jpws.pwslib.global.Global.setProgramName( APPLICATION_TITLE );
         org.jpws.pwslib.global.Util.setCryptoRandom( new OptimalRandom() );

      } catch ( Exception e ) {
         e.printStackTrace();
         exit( "# *** ERROR: Cannot initialize PWSLIB library!", true );
      }
      
      // Swing init
      swingInit();
      
      // init GUI system
      clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      ActionHandler.init();
      mainActionListener = ActionHandler.getMainActionListener();
      DisplayManager.init();
      recentFiles.setMenuFinalItem( MenuHandler.makeItem( "menu.file.deleterecents", true ) );

      // init Daemons
      serviceDemon = new ServiceDemon();
      serviceDemon.start();
     
      // place shutdown hook
      Runtime.getRuntime().addShutdownHook( shutdownThread );
      
      isInited = true;
      System.out.println( "# JPWS-F System initialized" );
      Options.setBounds( "dialogFloatingBarBounds", null );

   }  // init
   
   private static void setupLogging () 
   {
	  // define list of excluded module names 
	  List<String> exclude = new ArrayList<String>();
      exclude.add( "(PwsRecord." );
      exclude.add( "(PwsRecord)" );
      exclude.add( "(CryptoRandom" );
      exclude.add( "(PwsRecList" );
      exclude.add( "(PwsRecordList" );
      exclude.add( "(MenuHandler" );
//      exclude.add( "(DisplayManager" );
      exclude.add( "(ButtonBarDialog" );
      exclude.add( "(DialogButtonBar" );
      exclude.add( "(PwsFileFactory" );
      exclude.add( "(PwsFile." );
      exclude.add( "(PwsFile)" );
      exclude.add( "(PwsFileHeaderV3" );

      Log.setExcludeList(exclude.toArray(new String[exclude.size()]));
	  Log.setDebugLevel( DEBUG_LEVEL );
	  Log.setLogLevel( DEBUG_LEVEL );
   }
   
   private static void swingInit ()
   {
   Keymap keymap;
   Action action;
   KeyStroke keystroke;
   String lookAndFeel;
   boolean failed;

   
/*
   UIDefaults defaults = UIManager.getDefaults();
   Enumeration newKeys = defaults.keys();

   System.out.println( "--- UI-DEFAULTS ---" );
   while (newKeys.hasMoreElements()) {
     Object obj = newKeys.nextElement();
     System.out.printf("%50s : %s\n", new Object[] {obj, UIManager.get(obj)} );
   }
   System.out.println( "# END UI-DEFAULTS" );
*/     
   JFrame.setDefaultLookAndFeelDecorated( true );
   JDialog.setDefaultLookAndFeelDecorated( true );

//   import com.jgoodies.looks.plastic.theme.DesertRed;
   // initialise Look-And-Feel (LAF)
   loadLAF_Class( "com.incors.plaf.kunststoff.KunststoffLookAndFeel" );
   loadLAF_Class( "com.jgoodies.looks.plastic.PlasticLookAndFeel" );
   loadLAF_Class( "com.jgoodies.looks.plastic.Plastic3DLookAndFeel" );
   loadLAF_Class( "com.pagosoft.plaf.PgsLookAndFeel" );
   loadLAF_Class( "com.oyoaha.swing.plaf.oyoaha.OyoahaLookAndFeel" );
   
//   loadLAF_Class( "com.digitprop.tonic.TonicLookAndFeel" );
   
   // install Themes
//   PlasticLookAndFeel.getInstalledThemes();   
//   PlasticLookAndFeel.setPlasticTheme(new DesertRed());
//   PlasticLookAndFeel.setPlasticTheme(new Silver());
   
   // interpret LOOK-AND-FEEL user option
   failed = true;
   lookAndFeel = Options.getOption( "lookAndFeel" );
   if ( !lookAndFeel.isEmpty() )
   {
      System.out.println( "-- lookandfeel user option: ".concat( lookAndFeel ) );; 
   
      // resolve LAF variables
      if ( lookAndFeel.equals( LAF_NATIVE_OPTION ) )
         lookAndFeel = UIManager.getSystemLookAndFeelClassName();
      else if ( lookAndFeel.equals( LAF_CROSSPLATFORM_OPTION ) )
         lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
      
      // first attempt to init LAF after actual option value
      try {
         UIManager.setLookAndFeel( lookAndFeel );
         failed = false;
      }
      catch( Exception e )
      {
         System.out.println( "*** unable to initialize LOOK-AND-FEEL (user option): " + lookAndFeel );
         System.out.println( e );
      }
   }
   
   // if still failed, try system LAF defaults
   if ( failed )
   {
      System.out.print( "-- attempting JPWS default LAF: " ); 
      lookAndFeel = DEFAULT_LOOK_AND_FEEL;
      try { UIManager.setLookAndFeel( lookAndFeel ); System.out.println( "OK"); }
      catch ( Exception e )
      {
         System.out.println( "Failed");
         System.out.print( "-- attempting Java VM default LAFs: " ); 
         lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
         try { 
            UIManager.setLookAndFeel( lookAndFeel ); 
            System.out.println( "Cross-Platform"); 
         }
         catch ( Exception e1 )
         {
            lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            try { 
               UIManager.setLookAndFeel( lookAndFeel ); 
               System.out.println( "Native"); 
            }
            catch ( Exception e2 )
            { System.out.println( "Failed"); }
         }
      }
      
      // store modified user option to program options
      Options.setOption( "lookAndFeel", lookAndFeel );
   }

   // report actual running LAF
   lookAndFeel = null;
   if ( UIManager.getLookAndFeel() != null )
      lookAndFeel = UIManager.getLookAndFeel().getClass().getName();
   System.out.println( "# Active Look-and-Feel: " + lookAndFeel );   
   
   /// CHANGES TO GLOBAL JTextComponent
   keymap = JTextComponent.getKeymap( JTextComponent.DEFAULT_KEYMAP );

   // add "CTRL-INS" to "clipboard copy" functionality
   action = new DefaultEditorKit.CopyAction();
   keystroke = KeyStroke.getKeyStroke( KeyEvent.VK_INSERT, Event.CTRL_MASK );
   keymap.addActionForKeyStroke( keystroke, action );
   

   // add "SHIFT-DEL" to "clipboard cut" functionality
   action = new DefaultEditorKit.CutAction();
   keystroke = KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, Event.SHIFT_MASK );
   keymap.addActionForKeyStroke( keystroke, action );

   // add "SHIFT-INS" to "clipboard paste" functionality
   action = new DefaultEditorKit.PasteAction();
   keystroke = KeyStroke.getKeyStroke( KeyEvent.VK_INSERT, Event.SHIFT_MASK );
   keymap.addActionForKeyStroke( keystroke, action );

   }  // swingInit

   /** Puts the persistent logging file for console output into action
    *  if (and only if) global option "monitorSystem" is set. */
   static void activateLoggingFile ()
   {
      if ( Options.isOptionSet( "monitorSystem" ) )
         logPrinter.setOutputFile( new File( applHomeDir, "jpws.log" ) );
   }
   
   /** Loads a Look-and-Feel class by name and installs it 
    * in UIManager. 
    * 
    * @param classname String full Java class name
    */
   private static boolean loadLAF_Class ( String classname )
   {
      ClassLoader classLoader = Global.class.getClassLoader();
      Class<?> LAF_Class;
      LookAndFeel laf;
      LookAndFeelInfo inst[];
      boolean install;

      try {
         LAF_Class = classLoader.loadClass( classname );
         laf = (LookAndFeel) LAF_Class.newInstance();

         // install LAF only if not instance has installed itself
         inst = UIManager.getInstalledLookAndFeels();
         install = true;
         for ( int i = 0; i < inst.length; i++ )
            if ( inst[i].getClassName().equals( classname ) )
            {
               install = false;
               break;
            }
         if ( install )
            UIManager.installLookAndFeel( new LookAndFeelInfo( laf.getName(), 
                  laf.getClass().getName() ));

         Log.debug( 5,"(Global.loadLAF_Class) loaded LAF class == " + LAF_Class.getName());
         return true;
      } catch ( ClassNotFoundException e ) 
      {}
      catch ( Exception e ) 
      {
         e.printStackTrace();
      }
      return false;
   }
   
   public static void simulateExternalShutdown ()
   {
      try {
         shutdownThread.isInternal = false;
         shutdownThread.start();
         try { shutdownThread.join(); }
         catch ( InterruptedException e )
         {}
      }
      catch ( IllegalThreadStateException e )
      {
         System.out.println( "# JPWS-F EXIT IllegalThreadState on Shutdown (simulate)" );
         return;
      }
   }

   /** Initiates termination of the program (without emergency saves) */
   public static void exit () {
      startServiceAction( ServiceAction.EXIT );
   }  // exit

   /** Initiates termination of the program by optionally invoking 
    * emergencySave on currently loaded files. (EmergencySave 
    * attempts to save the files unconditionally.)
    * <p>Note: If save is true, a failing emergency save will prevent
    * shutdown of the program.
    * 
    * @param save if <b>true</b> all modified loaded files (desktop) get 
    *        saved before exit
    */
   public static void exit ( boolean save ) {
      boolean ok = !save || DisplayManager.emergencySave();
      if ( ok ) {
         startServiceAction( ServiceAction.EXIT );
      }
   }  // exit

   /** Initiates termination of the program after display of the parameter message
    *  at <code>System.err</code>.
    *  @param message text to be displayed
    *   */
   public static void exit ( String message, boolean fatal ) {
      if ( fatal ) {
        System.out.println();
        System.out.println( "# JPWS-F fatal error condition" );
      }
      System.err.println( message );
      exit();
   }  // exit

   /** Error-tolerant check for Java VM version to comply with this software. 
    *  If clearly fails, program terminates with error message.
    */
   private static void controlJavaVersion () {
      String ver, n[];
      int i, num1, num2, num3;
      
      // show Java version and default encoding
      ver = System.getProperty("java.runtime.version");
      if ( ver == null  || ver.isEmpty() )
         ver = System.getProperty("java.version");
      
      System.out.println( "# Java VM " + ver );
      System.out.println( "# Default Encoding: " + getDefaultCharset() );
         
      try {
         // remove trailing subversion ('_' or '-' separator)
         i = ver.indexOf('_');
         if ( i == -1 )
            i = ver.indexOf('-');
         if ( i > 0 )
            ver = ver.substring( 0, i );
         
         n = ver.split( "[.]" );
         num1 = Integer.parseInt( n[0] ) * 10000;
         num2 = Integer.parseInt( n[1] ) * 100;
         num3 = Integer.parseInt( n[2] );
         i = num1 + num2 + num3;
         
         if ( i < 10600 ) {
            System.err.println( "*** !! INCOMPATIBLE JAVA VIRTUAL MACHINE !! ***" ); 
            System.err.println( "*** JPWS requires minimum version: 1.6.0" ); 
            System.exit(1);
         }
      }
      catch ( Exception e )
      {e.printStackTrace();}
   }  
   
   private static int indexCLArgument ( String arg ) {
      int i;
      for ( i=0; i<commandlineArgs.length; i++ )
         if ( commandlineArgs[ i ].toLowerCase().startsWith( arg ) )
            return i;
      return -1;
   }  // hasCLArgument

   public static boolean hasCLArgument ( String arg ) {
      return indexCLArgument( arg ) > -1;
   }  // hasCLArgument

   /** Extracts a command line argument of the given token
    *  or empty string if not found.
    * 
    * @param arg String argument token to search for
    * @return String argument value or ""
    */
   public static String extractCLArgument ( String arg ) {
      int i;
      try {
         i = indexCLArgument( arg + ":" );
         return commandlineArgs[ i ].substring( arg.length()+1 );
      }
      catch ( Exception e )
         { return ""; }
   }  // extractCLArgument

   /** The time elapsed since the latest transfer to the system clipboard.
    *  If there is no transfer defined, or it has been cleared previously, 0 is
    *  returned. 
    * 
    * @return time-millis; 
    */
   public static long getClipboardTime ()
   {
      return hasClipboardTransfer ? System.currentTimeMillis() - clipboardTime : 0;
   }

   public static boolean hasClipboardTransfer ()
   {
      return hasClipboardTransfer;
   }
   
   public static boolean isOpenFile ( ContextFile f )
   {
      return DisplayManager.isRegisteredFile( f );
   }
   
   /**
    * Returns the currently active file container in the JPWS desktop
    * or <b>null</b> if no container is active.
    *  
    * @return <code>PwsFileContainer</code> or <b>null</b>
    */
   public static PwsFileContainer getSelectedFile ()
   {
      return DisplayManager.getSelectedContainer();
   }
   
   public static PwsRecord getSelectedRecord()
   {
      PwsFileContainer file;
      
      if ( (file = getSelectedFile()) != null )
         return file.getSelectedRecord(); 
      return null;
   }
      
   public static String getSelectedGroupName()
   {
      PwsFileContainer file;

      if ( (file = getSelectedFile()) != null )
         return file.getSelectedGroupName(); 
      return null;
   }
   
   /** Returns the mainframe's status bar or <b>null</b> if mainframe is not
    *  available.
    */
   public static StatusBar getStatusBar ()
   {
      return mainFrame == null ? null : mainFrame.getStatusBar();
   }
   
   /** Returns the global Timer instance for general use.
    * 
    * @return <code>Timer</code>
    */
   public static Timer getTimer () 
   {
	   return timer;
   }
   
   /**
    * Puts a text line into the main frame status bar.
    * @param text String
    */
   public static synchronized void setStatusText ( String text )
   {
      if ( mainFrame != null )
         mainFrame.getStatusBar().setMessage( text );
   }
   
   /**
    * Puts text into the system clipboard. (This text will be
    * subject to systematic erasure after some time if 
    * corresponding option is set.)
    * 
    * @param text String
    */
   public static synchronized void setClipboardText ( String text )
   {
      if ( text != null && text.equals( "" ) )
         text = null;
      
      Global.clipboard.setContents( new StringSelection(text), null);
      clipboardTime = System.currentTimeMillis();
      hasClipboardTransfer = text != null;
      ActionHandler.clipboardUpdated();
   }

   /**
    * Sets a name of a dialog as globally reserved or not reserved.
    * 
    * @param name dialog name (any text)
    * @param active if <b>true</v> the dialog name is reserved, 
    *        if <b>false</b> it is not reserved
    * @since 0-5-0
    */
   public static void setDialogActive ( String name, boolean active )
   {
      if ( active )
         activeDialogs.put( name, name );
      else
         activeDialogs.remove( name );
   }
   
   /**
    * Whether the requested global dialog name is currently reserved.
    *  
    * @param name dialog name
    * @return <b>true</b> if the dialog name is reserved, <b>false</b> otherwise
    * @since 0-5-0
    */
   public static boolean isDialogActive ( String name )
   {
      return activeDialogs.containsKey( name );
   }
   
   /** Returns the actual content of the system clipboard
    *  of the text type. Returns empty string if no content
    *  of this type is available.
    *  
    *  @return String
    */
   public static synchronized String getClipboardText ()
   {
      try {
         return (String)clipboard.getContents(null).getTransferData( DataFlavor.stringFlavor );
      }
      catch (Exception e )
      { return ""; }
   }
   
   public static void startBrowser ( URL url ) {
      Runnable run = new ServiceAction( ServiceAction.START_BROWSER, url );
      ActionHandler.startTask(run);
   }
   
   public static void startEmail ( String emailAddr ) {
      Runnable run = new ServiceAction( ServiceAction.START_EMAILCLIENT, emailAddr );
      ActionHandler.startTask(run);
   }
   
   public static File getUserDir () {
      return new File( System.getProperty( "user.dir" ) );
   }
   
   /** Returns the User Home directory in canonical form. 
    * @return String canonical file path
    */
   public static File getUserHome () {
      File f = null;
      try 
      { f = new File( System.getProperty( "user.home" ) ).getCanonicalFile(); }
      catch ( IOException e )
      {}
      return f;
   }

   /** Returns the operating system's system directory root path. 
    * @return String system root path
    */
   public static String getOSRootPath () 
   {
      String root;
      try {
         root = Global.isWindows() ? System.getProperty( "user.home" ).substring( 0, 2) 
                : "/";
      }
      catch ( Exception e )
      {  root = "c:"; }
      return root;
   }
   
   /** Reads the program options (user preferences and persistent values)
    * from the specified options file. If the file is not found, a file
    * is created.
    * 
    * @param file <code>ContextFile</code> option file specification; 
    *        may be <b>null</b> for standard file 
    */ 
   private static void readOptions ( ContextFile file )
   {
      ContextFile oldMir, newMir;
      File mirDir;
      RecentList rlist;
      Iterator<?> it;
      String oldRoot, newRoot, oldStr, newStr, newPDir, path, hstr1, hstr2;
      String[] sarr;

      // load options file from specified location
      Options.init( file );
      
      // determine option dependent global values
      setMaxIdleTime( Options.getIntOption( "maxIdleTime" ) );
      setMaxAutoMinimizeTime( Options.getIntOption( "maxAutoMinTime" ) );
      setMaxClipboardTime( Options.getIntOption( "clipboardTime" ) );
      currentDir = new File( Options.getOption("curdir") );
      backDir = new File( Options.getOption("backdir") );
      exchangeDir = new File( Options.getOption("exchangedir") );
      if ( !(path=Options.getOption("mirrordir")).isEmpty() )
         mirrorDir = new File ( path );
      recentFiles = new RecentList( "load.recent", isPortable() ? 20 : 8, 8 );
      recentFiles.setContent( Options.getOption("recentPaths") );
      recentFinds = new RecentList( RECENTFIND_LIST_LENGTH );
      recentFinds.setContent( Options.getOption("recentFindsContent") );
      hstr1 = Options.getOption("randomMirrorNames");
      randomMirrors.setContent( hstr1 );
      Log.debug(7, "(Global.readOptions) loaded randomMirror paths: ".concat(hstr1));
      passphrasePolicy = new PwsPassphrasePolicy( Options.getOption("policy") );
      generatorPolicy = new PwsPassphrasePolicy( Options.getOption("generatorPolicy") );
      setDateTimeFormat_after_Options();
      optionsRead = true;

      // correct standard directories et.al. in PORTABLE modus
      if ( isPortable() )
      {
         // compare old and new portable directories
         // and extract old and new device ROOT names (oldRoot, newRoot)
         oldStr = Options.getOption( "recentPortableDir" );
         newPDir = Util.normalizedPath( portableDir.getPath(), true );
         sarr = Util.findTrunks( oldStr, newPDir );
         oldRoot = sarr[0];
         newRoot = sarr[1];

         // if devices have changed since last session
         if ( !(oldRoot.isEmpty() | oldRoot.equals( newRoot )) )
         {
            // modify all entries in recent file list which start with
            // the recent portable root directory
            Log.debug( 3, "-- old Portable Root: ".concat( oldRoot ) ); 
            Log.debug( 3, "-- new Portable Root: ".concat( newRoot ) ); 
          
            // work on the RECENT FILE list 
            rlist = (RecentList)recentFiles.clone();
            for ( it = rlist.iterator(); it.hasNext(); )
            {
               oldStr = it.next().toString();
               if ( oldStr.startsWith( oldRoot ) )
               {
                  // replace file paths
                  newStr = Util.substituteTextS( oldStr, oldRoot, newRoot );
                  recentFiles.replaceRecent( oldStr, newStr );
                  Log.debug( 3, "-- modified RECENT entry: " + oldStr + " --> " + newStr );
                  
                  // replace mirror file name or private mirror directory
                  // if mirror for old filepath is available
                  oldMir = Global.getMirrorOf( oldStr );
                  newMir = Global.getMirrorOf( newStr );
                  try
                  {
                     // rename primary mirror file
                     if ( oldMir.exists() &&
                          oldMir.renameTo( newMir.getFilepath() ) ) 
                        Log.debug( 3, "-- modified MIRROR file: " + oldMir.getFilepath() + 
                                   " --> " + newMir.getFilepath() );

                     // rename private mirror directory
                     hstr1 = Util.fileNameOfPath( oldMir.getFilepath() ).substring( 5, 21 );
                     hstr2 = Util.fileNameOfPath( newMir.getFilepath() ).substring( 5, 21 );
                     mirDir = new File( Global.mirrorDir, hstr1 );
//Log.debug( 9, "(Global.readOptions) looking for MIR-DIR: [" + hstr1 + "]" );                     
                     if ( mirDir.isDirectory() &&
                          mirDir.renameTo( new File( Global.mirrorDir, hstr2 ) ))
                        Log.debug( 3, "-- modified MIRROR directory: " + hstr1  + 
                                   " --> " + hstr2 );
                  }
                  catch ( IOException e )
                  { e.printStackTrace(); } 
               }
            }

            // memorise PORTABLE ROOT based transformations
            // (standard dir paths are saved with "writeOptions()" below)
            Options.setOption( "recentPaths", recentFiles.getStringContent() );
         }

         // replace invalid standard directories with portable root
         // AND ensures meaningful default values if existence failure
         currentDir = correctedDirectory( currentDir, oldRoot, newRoot, defaultFileDir );
         exchangeDir = correctedDirectory( exchangeDir, oldRoot, newRoot, portableDir );
         backDir = correctedDirectory( backDir, oldRoot, newRoot, new File( applHomeDir, DEFAULT_BACKUPDIR ) );
         Util.ensureDirectory( backDir, null );
         
         // always store the PORTABLE ROOT dir (must cover first appearance)
         Options.setOption( "recentPortableDir", newPDir );
      }

      // branch for NORMAL program modus
      else
      {
         // standard directory setup
         verifyBackupDir();
         if ( !currentDir.isDirectory() ) {
            currentDir =  defaultFileDir; //getUserHome();
         }
         if ( !exchangeDir.isDirectory() ) {
            exchangeDir = currentDir;
         }
      }

      // save any startup modifications to file
      writeOptions();
   }  // readOptions
   
   /** Tests existence of Backup directory and performs adjustments
    *  where needed to set it to a working definition. Also ensures
    *  the backup dir exists.
    */
   public static void verifyBackupDir ()
   {
      if ( backDir == null || !backDir.isDirectory() ) {
         backDir = new File( applHomeDir, DEFAULT_BACKUPDIR );
         Util.ensureDirectory( backDir, null );
         Options.setOption( "backdir", backDir.getPath() );
      }
   }
   
   /** Performs startup correction on a standard directory in PORTABLE modus
    *  replacing a previous PORTABLE ROOT path part with a new one.
    *  After this existence of the new directory is tested and on failure
    *  the <code>defaultDir</code> is returned.
    * 
    * @param dir standard directory (holding full path)
    * @param oldRoot the portable root path of a previous session (normalised)
    * @param newRoot the portable root path of the actual session (normalised)
    * @return File corrected directory path, may be the same as <code>dir</code>
    *         or <b>null</b> 
    * @since 0-5-0
    */
   private static File correctedDirectory ( File dir, String oldRoot, String newRoot, File defaultDir )
   {
      File newDir;
      String path;
      
      if ( !isPortable() )
         return dir;
      
      if ( newRoot == null )
         throw new IllegalArgumentException( "newRoot==null" );
      if ( defaultDir == null )
         throw new IllegalArgumentException( "defaultDir==null" );
      
      // adjust directory path for portable root shift
      newDir = dir;
      path = Util.normalizedPath( dir.getPath(), true );
      if ( !oldRoot.equals( newRoot ) && path.startsWith( oldRoot ) )
      {
         path = Util.substituteTextS( path, oldRoot, newRoot );
         newDir = new File( path );
      }  
      
      // if directory still does not exist, relocate it to default 
      if ( !newDir.isDirectory() )
         newDir = defaultDir;

      if ( !dir.equals( newDir ) )
         Log.debug( 3, "-- modified standard directory: from " + dir.getPath() 
               + " to " + newDir.getPath() ); 
         
      return newDir;
   }
   
   public static void setMaxIdleTime ( long time )
   {
      maxIdleTime = Math.max( 60000, time );
   }
   
   public static void setMaxClipboardTime ( long time )
   {
      maxClipboardTime = Math.max( 10000, time );
   }
   
   public static void setMaxAutoMinimizeTime ( long time )
   {
      maxAutoMinTime = Math.max( 60000, time );
   }
   
   /** Collects some operating values for persistence and writes
    *  the program options to file.
    */
   private static void writeOptions ()
   {
      if ( !optionsRead )
         return;
      
      // save some current session values
      Options.setOption( "curdir", currentDir.getAbsolutePath() );
      Options.setOption( "backdir", backDir.getAbsolutePath() );
      Options.setOption( "exchangedir", exchangeDir.getAbsolutePath() );
      Options.setOption( "recentFindsContent", recentFinds.getStringContent() );

      Options.save();
   }  // writeOptions
   
   static void switchIdleState ( boolean idle )
   {
      SystemDesktopHandler.get().switchIdleState( idle );
   }  
   
   static void setIconified ( boolean iconified )
   {
      SystemDesktopHandler.get().setIconified( iconified );
   }  
   
   static boolean isIdleState ()
   {
      return SystemDesktopHandler.get().isIdleState();
   }
   
   static boolean isIconified ()
   {
      return SystemDesktopHandler.get().isIconified();
   }
   
   public static Frame getActiveFrame ()
   {
      return !isInited ? null : 
    	     isIdleState() ? SystemDesktopHandler.get().getIdleFrame() 
             : mainFrame;
   }
   
   /** Pushes a file path value to the recent file registry. Does nothing
    * if corresponding system option is not set.
    * 
    *  @param socket a file container (filepath must be defined) 
    */
   public static void pushRecentFile ( PwsFileSocket socket )
   {
      String path;
      
      if ( Options.isOptionSet("useRecentList") )
      {
         path = socket.getFilePath().replaceAll( "\\\\", "/" ); 
         recentFiles.pushRecent( path );
         Options.setOption( "recentPaths", recentFiles.getStringContent() );
      }
   }
   
   /** Removes a file path value to the recent file registry. 
    * 
    *  @param socket a file container (filepath must be defined) 
    */
   public static void removeRecentFile ( PwsFileSocket socket )
   {
      String path = socket.getFilePath();
      if ( path != null )
      {
          path.replaceAll( "\\\\", "/" ); 
          recentFiles.removeRecent( path );
          Options.setOption( "recentPaths", recentFiles.getStringContent() );
      }
   }
   
   /** Deletes all entries in the global "recent-open-files" list. */
   public static void clearRecents ()
   {
      recentFiles.clear();
      Options.setOption( "recentPaths", recentFiles.getStringContent() );
   }
   
   public static boolean isWindows ()
   {
      return isWindows;
   }
   
   public static boolean isPortable ()
   {
      return isPortable;
   }
   
   /** Whether special program + menu features for debugging and monitoring
    * internal states are enabled.
    * 
    * @return boolean debug status
    */
   public static boolean isDebug ()
   {
      return IS_DEBUG;
   }
   
   /** Returns the appropriate IO-adapter for the given file url. */
   public static ApplicationAdapter getAdapter ( URL url )
   {
      String prot = url.getProtocol();
      return prot.equalsIgnoreCase( "file" ) ? IOManager.getLocalFileAdapter() :
             prot.equalsIgnoreCase( "ftp" ) ? IOManager.getFTPAdapter() : IOManager.getURLAdapter();
   }

   /** Returns the filepath nominator suitable for the IO-adapter of the 
    *  given url. */
   public static String getFilePath ( URL url )
   {
      String prot, path;
      
      prot = url.getProtocol();
      path = prot.equalsIgnoreCase( "file" ) ? url.getPath() : url.toExternalForm();

      // Windows paths may not start with a '/'
      if ( isWindows && path.startsWith( "/" ) )
         path = path.substring( 1 );
      
      return path;
   }
   
   /** Returns the mirror file definition for a given file path.
    * @param path String file path of the file for which a mirror
    *             file has to be defined
    * @return <code>ContextFile</code>
    * @throws IllegalArgumentException if path is <b>null</b> or empty
    */
   public static ContextFile getMirrorOf ( String path )
   {
      ContextFile cf;
      String hstr, fname;
      byte[] buf;
      
      if ( path == null || path.length() == 0 )
         throw new IllegalArgumentException( "invalid path" );
      try
      { buf = path.getBytes("utf-8"); }
      catch ( UnsupportedEncodingException e )
      { buf = path.getBytes(); }
      hstr = Util.bytesToHex( Util.fingerPrint( buf ), 0, 8 );
      fname = "jpws-" + hstr + Global.DEFAULT_BACKUPEXTENTION;
      cf = IOManager.makeLocalContextFile( Global.mirrorDir, fname );
      return cf;
   }
   
   /** Returns a context file object to represent the parameter
    * file URL.
    * @param url URL the file definition
    * @return  <code>ContextFile</code>
    * @since 0-6-0
    */ 
   public static ContextFile getContextFile ( URL url ) {
      ApplicationAdapter adapter = getAdapter( url );
      String path = getFilePath( url );
      return IOManager.makeContextFile( adapter, path );
   }
   
   
   /** Causes the current thread to sleep the specified number of milliseconds. */
   public static void delay ( long millis ) {
      try { Thread.sleep( millis ); }
      catch ( Exception e ) {}
   }
   
   /** Requests and registers an exclusive access for the parameter semaphore
    *  definition.
    *  Once a value <b>true</b> is returned, the closure of the semaphore 
    *  remains active until method <code>releaseSemaphoreAccess()</code> is 
    *  called to revoke.
    *  The semaphore may be restricted to a specific object. If <b>null</b> is
    *  given for the object, the semaphore is valid for ANY other request,
    *  otherwise only for request with the same object.
    * 
    * @param name String semaphore name
    * @param object Object optional object restriction of semaphore; 
    *        may be null for GENERAL access
    * @return <b>true</b> if and only if access is permitted
    *          and reserved for the given object
    */
    
   public static boolean requestSemaphoreAccess ( String name, Object object ) {
	  if (name == null)
		  throw new NullPointerException("name is null");
	  
      // control if url already has critical phase 
      if ( isSemaphoreClosed(name, object) ) {
         return false;
      }
      
      // put key into index of closed semaphores
      criticalMap.put( name, object );

      // statusbar display setup
      if ( getStatusBar() != null && !isShutdown ) {
    	  getStatusBar().setActivity( StatusBar.ACTIVE );
      }
      return true;
   }  

   /** Tests for the existence of a closed semaphore for the parameter name and
    * object.
    * 
    * @param name String semaphore name
    * @param object Object optional object restriction of semaphore; 
    *        may be null for GENERAL access
    * @return <b>true</b> if and only if the semaphore for the given object 
    *          is closed. If <code>object</code> is null, ANY occurrence of
    *          name in the map will return <b>true</b>.
    */
    
   public static boolean isSemaphoreClosed ( String name, Object object ) {
	  // no entry means not closed
	  if (!criticalMap.containsKey(name)) return false;

	  // test if what we have mapped is what is probed --> closed
	  Object semObj = criticalMap.get(name);
      return  object == null || object == semObj; 
   }  

   /** Terminates a semaphore access reservation.
    *
    * @param name String semaphore name
    * @param object Object optional object restriction of semaphore; 
    *        may be null for GENERAL access
    */
   public static void releaseSemaphoreAccess ( String name, Object object ) {
      criticalMap.remove( name );

      // statusbar display settings
      if ( criticalMap.size() == 0 && getStatusBar() != null && !isShutdown )
    	  getStatusBar().setActivity( StatusBar.PASSIVE );
   }  

 /**
 *  Singleton class. 
 */
private Global () {
}

private static void startServiceAction ( int opcode ) {
   Runnable task = new ServiceAction( opcode );
   ActionHandler.startTask(task);
}

public static void startProjectServicePage () {
   ProjectServiceParcel p = ProjectServiceParcel.get();
   try {
      URL adr = p != null ? p.getServicePageUrl() 
            : new URL ( ResourceLoader.getCommand( "html.supportpage" ) );
      startBrowser( adr );

   } catch ( MalformedURLException e ) {
      e.printStackTrace();
   }   
}

public static void addTimePulseListener ( TimePulseListener l ) {
   blinkPulse.addTimePulseListener( l );
}

public static void removeTimePulseListener ( TimePulseListener l ) {
   blinkPulse.removeTimePulseListener( l );
}

/** The currently active default character set of the Java Virtual Machine. */
public static String getDefaultCharset () {
   return new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
}

public static boolean isUnixDerivate () {
   // note: all values in lower case!
   return OS.indexOf("linux") > -1 || OS.indexOf("unix") > -1 || OS.indexOf( "mac os" ) > -1;
}


//  *************  INNER CLASSES  **********************

public static HyperlinkListener createHyperLinkListener ()
{
   return new HyperlinkListener()
   {
      private Stack<URL> stack = new Stack<URL>();
      
      @Override
	public void hyperlinkUpdate ( HyperlinkEvent e )
      {
         URL url;
         JEditorPane epane;
         String path, file, fname;
         int i;
         
         if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
         {
            epane = (JEditorPane)e.getSource();
            if ( e instanceof HTMLFrameHyperlinkEvent )
            {
               ((HTMLDocument)epane.getDocument())
                     .processHTMLFrameHyperlinkEvent( (HTMLFrameHyperlinkEvent)e );
            }
            else
            {
               ActionHandler.resetIdleTime();
               
               try
               {
                  url = e.getURL();
                  
            //System.out.println( "--- protocol: " + url.getProtocol() );
                  // take special care of local files  
                  if ( url.getProtocol().equals("file") ||
                       url.getProtocol().equals("jar") )
                  {
            //System.out.println( "--- primary url: " + url );
            
                     // unmodified url for anchor references
                     if ( url.getRef() != null )
                        ;
                     
                     // interpreted file references
                     else
                     {
                        // extract noted filename
                        path = url.getPath();
                        i = path.lastIndexOf( '/' ) + 1;
                        fname = path.substring( i );
                        
                        // look for RETURN (step-back) command
                        if ( fname.equals( "$RETURN" ))
                        {
                           if ( !stack.isEmpty() )
                              url = stack.pop();
                        }
                        else
                        {
                           // resolve filename against language specific setting 
                           // in ACTIONS.PROPERTIES
                           file = ResourceLoader.getCommand( "html.file." + fname );
                           path = path.substring( 0, i ) + file;
                           url = new URL( url.getProtocol(), url.getHost(), path );
                        }
                     }
   
                     // start resulting file URL
                     if ( url != null )
                     {
               //System.out.println( "--- secondary url: " + url );
                        stack.push( epane.getPage() );
                        epane.setPage( url );
                     }
               }

                  // this attempts to start external files
                  else
                     Global.startBrowser( url );
               }
               catch ( IOException ioe )
               {
                  System.out.println( "*** About Dialog: IOError: " + ioe );
               }
            }
         }
      }
   };
}

private static class ServiceAction implements Runnable
{
   static final int SWITCH_TO_IDLE = 1;
   static final int SWITCH_TO_ICONIFIED = 2;
   static final int CLEAR_CLIPBOARD = 3;
   static final int EXIT = 4;
   static final int START_BROWSER = 5;
   static final int START_EMAILCLIENT = 6;
   static final int MIRROR_CHECK = 7;
   static final int SAVE_DATABASE = 8;
   static final int CLEANUP_WORK = 9;
   
   private int opcode;
   private Object arg;
//   private String name;
   
   public ServiceAction ( int code ) {
      this( code, null );
   }
   
   public ServiceAction ( int code, Object obj ) {
//      name = "ServiceAction " + code;
      opcode = code;
      arg = obj;
      
//      if ( code == MIRROR_CHECK )
//         setPriority( getPriority() - 1 );
//      if ( code == EXIT )
//         setPriority( getPriority() + 1 );
   }
   
   @Override
   public void run () {
      switch ( opcode ) {
      
      case SWITCH_TO_IDLE :
         switchIdleState( true );
         break;
      case SWITCH_TO_ICONIFIED :
         setIconified( true );
         break;
      case CLEAR_CLIPBOARD :
         setClipboardText( null );
         GUIService.statusConfirm("msg.confirm.clearclip" );
         break;
      case EXIT :
         exitAction();
         break;
      case START_BROWSER :
         startBrowser( (URL)arg );
         break;
      case START_EMAILCLIENT :
         startEmailClient( (String)arg );
         break;
      case MIRROR_CHECK :
         mirror_checking();
         break;
      case SAVE_DATABASE :
         save_database();
         break;
      case CLEANUP_WORK :
         for ( int i = 0; i < 3; i++ )
            System.gc();
         break;
      default:
         System.out.println( "*** IRREGULAR SERVICE ACTION COMMAND: " + opcode );
      }
   }

   /** Starts the program's standard browser to launch an URL
    * address. If no browser is defined in program options,
    * the operating system's default browser is launched. 
    * 
    * @param url URL target address
    */
   public static void startBrowser ( URL url )
   {
      String browser, cmd;
      boolean ok, noBrowserDef;
      
      // get browser application from program options
      browser = Options.getOption( "browserApplication" );
      noBrowserDef = browser.equals(""); 
      
      // display warning if browser is defined but not operative
      ok = noBrowserDef || new File( browser ).isFile();
      if ( !ok  ) {
         GUIService.failureMessage( "prefmsg.browser", null );
      }
      
      // if there is no valid browser definition, start OS default browser
      if ( noBrowserDef | !ok ) {
         startDefaultBrowser( url );
      }
      
      // if browser option is defined and available, start this application
      else {
         try {
            cmd = browser + " " + url.toExternalForm();
            Log.debug( 4, "(Global.ServiceAction.startBrowser) starting EXTERNAL EXEC: ".concat( cmd ) );
            Runtime.getRuntime().exec( cmd );
            ok = true;
         } catch ( IOException e ) {
            e.printStackTrace();
            GUIService.failureMessage( "msg.failure.browseservice", e );
         }
      }
   }  // startBrowser

private static void save_database ()
   {
      PwsFileContainer ct = getSelectedFile();
      if ( ct != null )
         ct.saveFile( false );
   }

   /** Starts the operating system's standard email client
    * with a recipient's address to "mailto".
    * 
    * @param address String target mail address
    */
   public static void startDefaultEmailClient ( String address )
   {
      // approve email address
      if ( !Util.isEmailAddress( address ) ) {
         GUIService.failureMessage( "msg.failure.emailaddr", null );
         return;
      }
      
      // attempt at user opted application
      
   
      // start the operating system's default email client
      if ( Desktop.isDesktopSupported() ) {
         Desktop sysDesk = Desktop.getDesktop();
         try {
            if ( sysDesk.isSupported( Desktop.Action.MAIL )) {
               sysDesk.mail( new URI( "mailto", address, null) );
            } else {
               GUIService.failureMessage( "msg.failure.nomailservice", null );
            }
         } catch ( Exception e ){
            e.printStackTrace();
            GUIService.failureMessage( "msg.failure.mailservice", e );
         }

      } else {
         GUIService.failureMessage( "msg.failure.missingdesktop", null );
      }
   }  // startEmailClient

   /** Starts the program's standard browser to launch an URL
    * address. If no browser is defined in program options,
    * the operating system's default browser is launched. 
    * 
    * @param url URL target address
    */
   public static void startEmailClient ( String address )
   {
      String client, cmd;
      File clientFile;
      boolean ok, noClientDef;
      if (address == null) address = "";
      
      // get browser application from program options
      client = Options.getOption( "emailApplication" );
      noClientDef = client.equals(""); 
      clientFile = new File( client );
      
      // display warning if browser is defined but not operative
      ok = noClientDef || clientFile.isFile();
      if ( !ok  ) {
         GUIService.failureMessage( "prefmsg.mailclient", null );
      }
      
      // if there is no valid browser definition, start OS default browser
      if ( noClientDef | !ok ) {
         startDefaultEmailClient(address);
      }
      
      // if browser option is defined and available, start this application
      else {
         try {
        	// supply call parameter for mail address (if client known)
        	String params = "";
        	String program = clientFile.getName(); 
        	if (program.indexOf("thunderbird") > -1) {
        		params = "-compose to='$addr'" ;
        	}
        	if (program.indexOf("claws-mail") > -1 ||
        		program.indexOf("kmail") > -1) {
        		params = "$addr" ;
        	}
        	params = Util.substituteText(params, "$addr", address);
        	 
        	// call the mail client 
            cmd = client + " " + params;
            Log.debug( 4, "(Global.ServiceAction.startEmailClient) starting EXTERNAL EXEC: ".concat( cmd ) );
            Runtime.getRuntime().exec( cmd );
            ok = true;
         } catch ( IOException e ) {
            e.printStackTrace();
            GUIService.failureMessage( "msg.failure.browseservice", e );
         }
      }
   } 

   private static void mirror_checking ()
   {
      DisplayManager.checkMirrorActivity();
      if ( Options.isModified() )
         writeOptions();
   }

   /** Initiates termination of the program */
   private synchronized static void exitAction ()
   {
      if ( shutdownThread.isAlive() ) return;
      System.out.println( "# JPWS-F exit" ); 

      // make the demon threads suspended
      if ( serviceDemon != null ) {
         serviceDemon.pause();
      }
      
      // close active database(s)
      if ( !DisplayManager.closeAll() ) {
         // this will interrupt the exit process (if close didn't work, e.g.
         // because of user break action)
         System.out.println( "# JPWS-F exit interrupted" );
         if ( serviceDemon != null ) {
            serviceDemon.endPause();
         }
         return;
      }

      // start the shutdown thread (no return)
      try {
         shutdownThread.isInternal = true;
         shutdownThread.start();
         try { shutdownThread.join(); }
         catch ( InterruptedException e )
         {}

      } catch ( IllegalThreadStateException e ) {
         System.out.println( "# JPWS-F EXIT IllegalThreadState on Shutdown" );
         System.out.println( e );
         return;
      }

      Runtime.getRuntime().removeShutdownHook( shutdownThread );
      System.exit(0);
   }  // exitAction
   
   /** Starts the operating system's standard browser
    *  with an URL address to launch.
    *  
    * @param url URL target address
    */
   public static void startDefaultBrowser ( URL url )
   {
      if ( Desktop.isDesktopSupported() )
      {
         Desktop sysDesk = Desktop.getDesktop();
         try
         {
            if ( sysDesk.isSupported( Desktop.Action.BROWSE ))
            {
               Log.debug( 6, "(Global.ServiceAction.startDefaultBrowser) starting system default browser with "
                     .concat( url.toExternalForm() ));
               sysDesk.browse( url.toURI() );
            }
            else
            {
               GUIService.failureMessage( "msg.failure.nobrowseservice", null );
            }
         }
         catch ( Exception e )
         {
            e.printStackTrace();
            GUIService.failureMessage( "msg.failure.browseservice", e );
         }
      }
      else
      {
         GUIService.failureMessage( "msg.failure.missingdesktop", null );
      }
   }  // startDefaultBrowser

}  // ServiceAction

private static class ServiceDemon extends Thread
{
   private static final long SLEEPTIME = 1000;
   private static final long AUTOSAFETIME = 20000;
   private boolean running;
   private boolean waiting;

   ServiceDemon () {
      super( "JPWS Service Demon" );
      setDaemon( true );
   }
   
   @Override
   public synchronized void run () {
      PwsFileContainer dbf;
      long idleTime;
      int loops = 0, cleanupCount = 0;
      boolean fileActive;
      
      running = true;
      while ( running )
      try {
         idleTime = ActionHandler.getIdleTime();
         dbf = getSelectedFile();
         fileActive = dbf != null;

         // LOCK STATE SERVICE
         if ( !isIdleState() && idleTime > maxIdleTime &&
              Options.isOptionSet("lockIdleState") )
            startServiceAction( ServiceAction.SWITCH_TO_IDLE );

         // ICONIFY SERVICE
         if ( !isIconified() && idleTime > maxAutoMinTime &&
               Options.isOptionSet("autoMinimize") )
            startServiceAction( ServiceAction.SWITCH_TO_ICONIFIED );

         // CLIPBOARD CONTROL
         if ( hasClipboardTransfer && getClipboardTime() > maxClipboardTime &&
              Options.isOptionSet("clearClipboard") )
            startServiceAction( ServiceAction.CLEAR_CLIPBOARD );
         
         // DATABASE FLUSH SERVICE
         if ( fileActive && idleTime > AUTOSAFETIME && dbf.isModified() 
              && Options.isOptionSet("autoflush") && dbf.canWrite() )
            startServiceAction( ServiceAction.SAVE_DATABASE );
         
         // DATABASE MIRROR SERVICE
         if ( loops % MIRROR_CHECKING_PERIOD == 0 && Options.isOptionSet( "useDataMirrors" ) )
            startServiceAction( ServiceAction.MIRROR_CHECK );
      
         // GC SERVICE
         if ( cleanupCount++ % 60 == 0 )
            startServiceAction( ServiceAction.CLEANUP_WORK );

         // DEBUG MONITOR SERVICE
         if ( isDebug() ) {
        	 IOManager.getMonitorPanel().update();
         }
         
         // SYSTEM MONITOR SERVICE
         if ( Options.isOptionSet( "monitorSystem" ) ) {
            setStatusText( "Idle Time: " + idleTime/1000 +
               "   Clipboard Time: " + getClipboardTime()/1000 +
               "   Mem: " + Runtime.getRuntime().freeMemory()/1000 + "/" +
               Runtime.getRuntime().totalMemory()/1000 );
         }
         
         // go into sleep phase
         loops++;
         notifyAll();
         try { wait( waiting ? 0 : SLEEPTIME ); }
         catch ( InterruptedException e )
         {}

      } catch ( Exception e ) {
         GUIService.failureMessage( "Service-Demon", e );
         e.printStackTrace();
      }
      System.out.println( "# Service Demon terminated" ); 
   }  // run

   public synchronized void terminate () {
         running = false;
         notifyAll();
         Log.log( 5, "(ServiceDemon) triggered termination" );
   }
   
   public synchronized void pause () {
      waiting = true;
      Log.log( 5, "(ServiceDemon) triggered suspension start" );
   }
   
   public synchronized void endPause () {
      waiting = false;
      notifyAll();
      Log.log( 5, "(ServiceDemon) triggered suspension end" );
   }
   
}  // inner class ServiceDemon

private static class ShutdownThread extends Thread
{
   boolean isInternal;
   
   public ShutdownThread () {
      super( "JPWS Shutdown Thread" );
   }
   
   @Override
   public void run () {
	  // report this shutdown
      PrintStream print = Reporter.getPrintStream( Reporter.APPLOG );
      String hstr = !isInternal ? "# *** SYSTEM SHUTDOWN HOOK operating now ***"
            : "# *** JPWS-F SHUTDOWN INITIATED";
      System.out.println(  );
      System.out.println( hstr ); 
      print.println( hstr );

      isShutdown = true;
      
      // terminate demon threads
      if ( serviceDemon != null ) {
         serviceDemon.terminate();
      }
      ActionHandler.exit();

      // clear System Clipboard
      if ( hasClipboardTransfer && Options.isOptionSet("clearClipboard") ) {
         setClipboardText( null );
         hstr = "# System clipboard cleared";
         System.out.println( hstr );
         print.println( hstr );
      }

      // desktop manager exit (avoid for external VM shutdown because of conflicts)
      if ( isInternal )
      try {
         DisplayManager.exit();
         System.out.println( "# display exit" );
      }
      catch ( Exception e )
      { e.printStackTrace(); }
      
      // tear down GUI
      if ( isInternal ) {
         // exit idleFrame
         SystemDesktopHandler.get().exit();
   
         // exit mainFrame
         if ( mainFrame != null ) {
            System.out.println( "# exiting MAIN FRAME" ); 
            mainFrame.exit();
            mainFrame = null;
         }
      }

      // save preferences
      if ( optionsRead )
      try {
         // "last chance" settings in Options 
         Options.setIntOption( "lastAccessTime", (int)(System.currentTimeMillis()/1000) );
         Options.setOption( "lastAccessUser", System.getProperty( "user.name" ) );
         hstr = System.getProperty( "os.name" ) + ", " + System.getProperty( "os.version" );
         Options.setOption( "lastAccessHost", hstr );
         if ( !Options.isOptionSet( "useRecentList" ) ) {
            // clear out any session values for recent files entries
            // if user didn't opt for persistence
            recentFiles.clear();
            Options.setOption( "connector.URL.recent", null );
         }

         writeOptions();
         hstr = "# options saved";
         System.out.println( hstr );
         print.println( hstr );
      }
      catch ( Exception e )
      { e.printStackTrace(); }
      
      // shutdown IO-Manager
      IOManager.exit();
      System.out.println( "# IO layer exit" );
      
      System.out.println( "# JPWS TERMINATED, " + System.currentTimeMillis()/1000);
      print.println( "# JPWS TERMINATED " );
      Reporter.exit();
   }
}

private static class OptionListener implements OptionChangeListener
{

   @Override
public void optionChanged ( OptionChangeEvent e )
   {
      String name;
      
      name = e.getOptionName();
      if ( name.equals( "maxIdleTime" ) )
      {
         setMaxIdleTime( Options.getIntOption( "maxIdleTime" ) ); 
      }
      else if ( name.equals( "maxAutoMinTime" ) )
      {
         setMaxAutoMinimizeTime( Options.getIntOption( "maxAutoMinTime" ) ); 
      }
      else if ( name.equals( "clipboardTime" ) )
      {
         setMaxClipboardTime( Options.getIntOption( "clipboardTime" ) ); 
      }
      else if ( name.equals( "dateFormatOption" ) || name.equals( "timeFormatOption" ) )
      {
         setDateTimeFormat_after_Options(); 
      }
   }
}  // class OptionListener

/**
 * This is the application specific version (subclass) of the <code>
 * org.jpws.pwslib.crypto.CryptoRandom</code> random value generator.
 * Method <code>getUserSeed()</code> supplies JPWS owned individual random data 
 * states as input for the data pool collector of <code>CryptoRandom</code> class.
 * 
 */
private static class OptimalRandom extends CryptoRandom
{  
   @Override
public byte[] getUserSeed ()
   {
      ByteArrayOutputStream bout;
      DataOutputStream out;
      PwsFileContainer dbf;
      Component c;
      Point loc;
      Dimension dim;
      Frame frame;
      long l;
      
      bout = new ByteArrayOutputStream();
      out = new DataOutputStream( bout );
      
      try {
         // SHA-256 over application options file
         if ( Options.randomSeed != null )
            out.write( Options.randomSeed );
//         System.out.println( "*** Optimal random seed" ); 
      
         // memory address of this
         out.writeInt( this.hashCode() );
//         System.out.println( "*** PGRandom hashCode: " + this.hashCode() ); 

         // current mainframe position
         if ( (frame = Global.getActiveFrame()) != null )
         {
            loc = frame.getLocation();
            l = (long)loc.x << 32 | loc.y;
            out.writeLong( l );
//          System.out.println( "*** Mainframe position: " + l );
            dim = frame.getSize();
            l = (long)dim.width << 32 | dim.height;
            out.writeLong( l );
//          System.out.println( "*** Mainframe dimension: " + l ); 
            
            // memory address of mainframe
            out.writeInt( frame.hashCode() );
//            System.out.println( "*** Mainframe hashCode: " + frame.hashCode() ); 
         }
         
         // if file is loaded into workspace
         if ( (dbf = Global.getSelectedFile()) != null )
         {
            out.write( dbf.getRandomValue() );
//            System.out.println( "*** FileContainer random: ".concat( Util.bytesToHex( dbf.getRandomValue() )) );
            
            // memory address of selected file
            out.writeInt( dbf.hashCode() );
//            System.out.println( "*** File Container hashCode: " + dbf.hashCode() );
            
            // if record editor is available
            if ( (c=dbf.getEditDialog()) != null )
            {
               // memory address of editor
               out.writeInt( c.hashCode() );
//               System.out.println( "*** Record editor hashCode: " + c.hashCode() ); 

               // dimensions of active record editor if any
               loc = c.getLocation();
               l = (long)loc.x << 32 | loc.y;
               out.writeLong( l );
//               System.out.println( "*** Record editor position: " + l ); 
               dim = c.getSize();
               l = (long)dim.width << 32 | dim.height;
               out.writeLong( l );
//             System.out.println( "*** Record editor dimension: " + l ); 
            }
         }
      }
      catch ( Exception e )
      {
         System.err.println( "*** ERROR in random user seed collector: " + e ); 
      }
    
      return bout.toByteArray();
   }
}

/**
 * Class implementing a memory of some recently used <code>PwsFiles</code> together
 * with their security passwords. Passwords are stored encrypted in 
 * <code>PwsPassphrase</code> types. 
 * 
 *  FileMemory in org.jpws.front
 */
public static class FileMemory
{
   private RecentList    rlist;
   
   /** Creates a new FileMemory with the given number of maximum entries.
    *  @param max max number of record entries
    */
   public FileMemory ( int max )
   {
      rlist = new RecentList( max );
   }
   
   /** Adds a file to this file memory with a displayable name corresponding
    *  to its filepath property.
    * 
    *  @param file PwsFile with persistent state and passphrase
    */ 
   public void pushFile ( PwsFile file )
   {
      pushFile( file, null );
   }  // pushFile

   /** Add a file to this file memory.
    * 
    *  @param file PwsFile with persistent state and passphrase
    *  @param name optional displayable name for this entry
    */ 
   public void pushFile ( PwsFile file, String name )
   {
      FRec rec;
      
      rec = new FRec( file );
      if ( name != null )
         rec.setName( name );
      rlist.pushRecent( rec );
   }  // pushFile

   /** Returns the first memory file record with the specified filepath property,
    *  or <b>null</b> if such a record does not exist.
    *  
    * @param filepath search value 
    * @return FileMemory.FRec
    */ 
   @SuppressWarnings("unchecked")
   public FRec getRecord ( String filepath )
   {
      Iterator<FRec> it;
      FRec rec;
      
      for ( it = (Iterator<FRec>)iterator(); it.hasNext(); )
      {
         rec = it.next();
         if ( rec.filepath.equals( filepath ) )
            return rec;
      }
      return null;
   }
   
   /** Returns the passphrase mapped to the parameter filepath
    *  in this memory or <b>null</b> if such a record is unknown.
    *  
    * @param filepath key value
    * @return <code>PwsPassphrase</code>
    * @since 0-5-0
    */ 
   public PwsPassphrase getPassphrase ( String filepath )
   {
      FRec rec;

      return (rec = getRecord( filepath )) == null ? null : rec.passphrase; 
   }
   
   /** Remove all entries in this memory. */
   public void clear ()
   {
      rlist.clear();
   }

   public int size ()
   {
      return rlist.getSize();
   }
   
   /** Returns an iterator over all currently stored records of this memory
    *  in the timely order of their most recent push actions.
    */
   public Iterator<?> iterator ()
   {
      return rlist.iterator();
   }

   /**
    * GUI active method to open a specified file using this file password memory. 
    * If no such file is recorded or the user chooses "cancel" in a dialog,
    * <b>null</b> is returned. A user dialog with password input is issued 
    * if the password on the recorded file has changed.
    * 
    * @param filepath
    * @return PwsFile if success or <b>null</b> otherwise
    * @since 0-5-0
    */
   public PwsFile openFile ( String filepath )
   {
      FRec fileRec;
      PwsFile target;
   
      if ( filepath == null )
         throw new NullPointerException( "filepath" );
      
      // try open the parameter filepath
      if ( (fileRec = getRecord( filepath )) == null )
         return null;

      target = DatabaseHandler.passTryOpen( fileRec.file, fileRec.passphrase, 
    		   FileAccessModus.cachePreferred );
      if ( target == null )
         return null;
         
      fileRec.passphrase = target.getPassphrase();
      return target;
   }
   
   // ************ inner class to FileMemory **************
   
   public static class FRec
   {
      ContextFile file;
      String filepath;
      String name;
      ApplicationAdapter adapter;
      PwsPassphrase passphrase;
      
      /** Creates a file record with the filename part of the filepath property
       *  as the displayable name of this record.
       *  
       *  @param file
       */
      public FRec ( PwsFile file )
      {
         if ( file.getPassphrase() == null )
            throw new IllegalArgumentException( "no passphrase defined" );
         if ( !file.hasResource() )
            throw new IllegalArgumentException( "no persistent state" );

         filepath = file.getFilePath();
         adapter = file.getApplication();
         this.file = file.getContextFile();
         passphrase = file.getPassphrase();
         name = Util.fileNameOfPath( filepath );
      }  // constructor
      
      @Override
	public boolean equals ( Object o )
      {
         FRec rec;
         
         if ( o == null )
            return false;
         
         rec = (FRec)o;
         return rec.filepath.equals( this.filepath ) && 
                rec.adapter.equals( this.adapter );
      }
      
      @Override
	public int hashCode ()
      {
         return filepath.hashCode() ^ adapter.hashCode();
      }
      
      /** Returns the name field of this record. */
      @Override
	public String toString ()
      {
         return name;
      }
      
      /** Sets the displayable name of this record to the parameter value. */
      public void setName ( String name )
      {
         this.name = name;
      }
   }  // inner class FRec
}

public static class ProjectServiceParcel 
{
   private static ProjectServiceParcel instance;
   private static int attempted;

   private String programName;
   private URL servicePageUrl;
   private URL downloadPageUrl;
   private int programVersion;
   private long newsTime;

   public static ProjectServiceParcel downloadParcel () throws IOException
   {
      ProjectServiceParcel p;
      Properties prop;
      InputStream input;
      URL url;
      String adr, download, hstr, standardPage;
      
      p = new ProjectServiceParcel();

      // download server side project info parcel
      // load server URL and properties
      url = new URL( SERVER_PARCEL_URL ); 
      Log.log( 7, "(Global.ProjectServiceParcel) DOWNLOADING SERVER INFO from: " + url );
      input = url.openStream();
      
      // load server properties
      prop = new Properties();
      prop.load( input );
      input.close();
      Log.log( 7, "(Global.ProjectServiceParcel) -- received program server info parcel" );

      // evaluate PROGRAM HEAD properties
      p.programVersion = (int)Util.longFromString( prop.getProperty( "PUBLICBUILD" ) );
      p.programName = prop.getProperty( "PUBLICNAME", "?" );
      Log.debug( 7, "(Global.ProjectServiceParcel) latest program name = ".concat( p.getProgramName() ) );

      if ( (hstr = prop.getProperty( "NEWSTIME" )) == null )
         p.newsTime = -1;
      else
         p.newsTime = Util.timeFromString( hstr, Util.GMT );
      Log.debug( 7, "(Global.ProjectServiceParcel) latest news time = " + (p.getNewsTime() == -1 ? 
            "FAILED" : Util.standardTimeString( p.getNewsTime())) + " (" + p.getNewsTime() + ")" );
      
      // explicit or standard JPWS support page URL
      standardPage = ResourceLoader.getCommand( "html.supportpage" );
      adr = prop.getProperty( "SERVICEPAGE", standardPage );
      try { p.servicePageUrl = new URL( adr ); }
      catch ( MalformedURLException e )
      {
         e.printStackTrace();
         p.servicePageUrl = new URL( standardPage );
      }
      Log.debug( 7, "(Global.ProjectServiceParcel) service page = " + p.getServicePageUrl() );
      
      // explicit or standard JPWS latest download page URL
      standardPage = ResourceLoader.getCommand( "html.progdownload" );
      download = prop.getProperty( "DOWNLOAD", standardPage );
      try { p.downloadPageUrl = new URL( download ); }
      catch ( MalformedURLException e )
      {
         e.printStackTrace();
         p.downloadPageUrl = new URL( standardPage );
      }
      Log.debug( 7, "(Global.ProjectServiceParcel) download page = " + p.getDownloadPageUrl() );
      
      return p;
   }

   /** Returns a valid service parcel or <b>null</b> if none could be 
    * downloaded from the host during this application session.
    * (During one session the attempt can be repeated 3 times.)
    * 
    * @return <code>ProjectServiceParcel</code> or <b>null</b>
    */
   public static ProjectServiceParcel get () {
      if ( instance == null && attempted++ < 3 ) {
         try { 
        	 instance = downloadParcel(); 
         } catch ( IOException e ) {
            e.printStackTrace();
         }
      }
      return instance;
   }
   
   private ProjectServiceParcel ()
   {}
   
   /** Returns the URL address of JPassword project's service
    * pages on the web as stated in the server info parcel.
    * (This defaults to a standard value if the server parcel
    * does not contain a valid URL entry for this property.)  
    * 
    * @return URL 
    */
   public URL getServicePageUrl ()
   {
      return servicePageUrl;
   }

   /** Returns the URL address of the download page for 
    * the latest reported JPassword version.
    * (This defaults to a standard value if the server parcel
    * does not contain a valid URL entry for this property.)  
    * 
    * @return URL 
    */
   public URL getDownloadPageUrl ()
   {
      return downloadPageUrl;
   }

   /** Returns the latest program build version
    *  as stated in the server info parcel.
    *  (This value may return 0 if service data was corrupted.) 
    *   
    * @return int program build ID (up to 9 digits)
    */
   public int getProgramVersion ()
   {
      return programVersion;
   }
   
   /** Returns the time of the latest news in service pages
    *  as stated in the server info parcel.
    *  (This value may return -1 if service data was corrupted.) 
    *   
    * @return long new time in epoch millis
    */
   public long getNewsTime ()
   {
      return newsTime;
   }
   
   /** Returns the title of the latest program build
    *  as stated in the server info parcel.
    *  (This value may return "?" if service data was corrupted.) 
    *   
    * @return String latest program name
    */
   public String getProgramName ()
   {
      return programName;
   }
}

/** Returns the editor dialog for the currently edited record (database entry)
 * or <b>null</b> if currently no entry is being edited.
 * 
 * @return <code>AddDialog</code> 
 */
public static EditorDialog getEditDialog ()
{
   PwsFileContainer ct = getSelectedFile();
   return ct == null ? null : ct.getEditDialog();
}

/** Whether there is currently a database entry being edited 
 * (whether there is an open editor dialog).
 * 
 * @return boolean <b>true</b> == open editor dialog
 */
public static boolean isEditingRecord ()
{
   return getEditDialog() != null;
}

public static boolean isDecoration () {
	return isChristmas() || isEaster();
}

public static boolean isChristmas() {
	if ( Options.isOptionSet("seasonalDeco") ) {
		GregorianCalendar cal = new GregorianCalendar();
		int day = cal.get(Calendar.DAY_OF_YEAR);
		Log.debug(10, "(Global) DAY-OF-YEAR == " + day);
		return day > 355 | day < 7;
	} else return false;
}

public static boolean isEaster() {
	try {
		if ( Options.isOptionSet("seasonalDeco") ) {
			GregorianCalendar cal = new GregorianCalendar();
			int day = cal.get(Calendar.DAY_OF_YEAR);
//			day = 87;
			int year = cal.get(Calendar.YEAR);
			int[] dates = new int[] {87, 106, 91, 111, 103, 94, 107, 99, 91, 110, 95}; 
			int event = dates[ year-2016 ];
			return day >= event & day < event+3;
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	return false;
}

}
