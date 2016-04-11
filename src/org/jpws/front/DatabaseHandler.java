package org.jpws.front;

import java.awt.Component;
import java.awt.Dimension;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jpws.data.IOManager;
import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.RecentList;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsRecordList;
import org.jpws.pwslib.exception.InvalidPassphraseException;
import org.jpws.pwslib.exception.LoginFailureException;
import org.jpws.pwslib.exception.PasswordSafeException;
import org.jpws.pwslib.exception.UnsupportedFileVersionException;
import org.jpws.pwslib.exception.UserCancelException;
import org.jpws.pwslib.global.Log;

public class DatabaseHandler {

   /** Modi for retrieving method when opening a database via ActionHandler.
    * <br><i>directAccess</i> == file is loaded from persistent state regardless of cache availability
    * <br><i>cachePreferred</i> == if file is in IO cache, then the cached instance is returned
    * <br><i>desktopSelection</i> == if file is available in desktop, user will be prompted to decide
    * from which source shall be loaded: direct or cached. 
    */
   public static enum FileAccessModus {directAccess, cachePreferred, desktopSelection };

	/** Singleton class */
	private DatabaseHandler() {
	}

	private static PwsFile elementalAccessOpenPws ( 
	           ContextFile file, PwsPassphrase pass, FileAccessModus acm ) 
	                 throws IOException, PasswordSafeException
	   {
	      if ( file == null ) return null;
	      
	      PwsFile pws = null;
	      String path = file.getFilepath();
	      Log.log( 8, "(ActionHandler.elementalAccessOpenPws) enter with modus " + acm );
	      
	      // select method of file access
	      switch ( acm )
	      {
	      case directAccess:
	         Log.log( 5, "(ActionHandler.elementalAccessOpenPws) loading database by direct access: "
	               .concat( path ));
	         if ( pass != null )
	            pws = IOManager.getDirectAccessDatabase( file, pass );
	         break;
	
	      case cachePreferred:
	         Log.log( 5, "(ActionHandler.elementalAccessOpenPws) loading database with cache priority: "
	               .concat( path ));
	         pws = pass == null ? IOManager.getDatabase( file ) : IOManager.getOpenDatabase( file, pass );
	         break;
	      
	      default:
	         // open modus: "desktopSelection"
	         Log.log( 5, "(ActionHandler.elementalAccessOpenPws) loading database with desktop select priority: "
	               .concat( path ));
	         // if the requested file is open in the desktop
	         pws = getUserChosenContainerFile( file );
	
	         // if not loaded from IO-manager operate as "directAccess" modus
	         if ( pws == null ) {
	   	        PwsFileContainer ct = DisplayManager.getFileContainer( file );
	            if ( ct != null ) {
	               // if we have a container for the file
	               // ensure passphrase is from the file noticed to the user
	               // in "getUserChosenContainerFile()"
	               pass = ct.getPassphrase();
	            }
	            Log.log( 5, "(ActionHandler.elementalAccessOpenPws) ... now loading database by direct access: "
	                  .concat( path ));
	            if ( pass != null ) {
	               pws = IOManager.getDirectAccessDatabase( file, pass );
	            }

	         } else {
	            Log.log( 5, "(ActionHandler.elementalAccessOpenPws) ... database loaded from IO Manager: "
	                  .concat( path ));
	         }
	      }
	      return pws;
	   }

	/** Looks for and returns the latest security copy of the parameter database file 
	    *  definition. Security copy can be from various places and occasions of creation
	    *  and may be temp-files. mirror-files or regular save-copies.
	    *  
	    * @param file ContextFile database definition
	    * @param pass PwsPasshrase password for opening the copy
	    * @return PwsFile the latest security copy or <b>null</b> if not available
	    */
	   private static PwsFile getLatestSecurityCopy ( ContextFile file, PwsPassphrase pass ) 
	   {
	      PwsFile pws = null;
	      PwsFileSocket.BackupManager backupMan;
	      ArrayList<ContextFile> list = new ArrayList<ContextFile>();
	      ContextFile f, bestF;
	      String path, hstr;
	      
	      path = file.getFilepath();
	      if ( path == null ) return null;
	      
	      try {
	         // look for TEMP file
	         f = new ContextFile( file.getAdapter(), path.concat( ".temp" ));
	         if  ( f.exists() ) {
	            list.add( f );
	         }
	         
	         // look for MIRROR file
	         f = Global.getMirrorOf( path );
	         if  ( f.exists() ) {
	            list.add( f );
	         }
	   
	         // look for security copy
	         backupMan = new PwsFileSocket.BackupManager();
	         backupMan.refresh( file );
	         f = backupMan.getTopBackup();
	         if  ( f != null && f.exists() ) {
	            list.add( f );
	         }
	         
	         // evaluate list entries and determine the youngest (file mod time)
	         bestF = null;
	         for ( ContextFile g : list ) {
	            Log.debug( 5, "(ActionHandler.getLatestSecurityCopy) listing security file: ".
	                  concat( g.getFilepath() ) );
	            if ( bestF == null || f.modifyTime() > bestF.modifyTime() ) {
	               bestF = g;
	            }
	         }
	            
	         // ask user and try open best security backup
	         hstr = ResourceLoader.getDisplay( "msg.ask.replace.bysecurity" ); 
	         hstr = Util.substituteText( hstr, "$file", bestF.getFilepath() );
	         hstr = Util.substituteText( hstr, "$database", path );
	         if ( bestF != null && GUIService.userConfirm( hstr ) ) {
	            pws = passTryOpen( bestF, pass, FileAccessModus.desktopSelection );
	            if ( pws != null ) {
	               // user notify
	               GUIService.statusConfirm("msg.confirm.restore", bestF.getFilepath() );
	            }
	         }

	      } catch ( IOException e ) { 
	    	  e.printStackTrace(); 
	      }
	      return pws;
	   }

	/*   
	   // since 0-5-0  
	   public static void test_1 ()
	   {
	      ApplicationAdapter ada;
	      String target, fpath;
	      File file;
	      URL url;
	      InputStream in;
	      ByteArrayInputStream in2;
	      ByteArrayOutputStream out;
	      FileOutputStream fout;
	      
	      try {
	         // test URL file loading via Internet
	         target = "http://glasarts.gmxhome.de/Pferde.dat";
	         url = new URL( target );
	   
	         Log.debug( 0, "~~ URL TEST, connecting with: ".concat( target ) );
	         
	         ada = Global.getAdapter( url );
	         in = new V3_InputStream( StreamFactory.getInputStream( ada, target ) );
	
	         Log.debug( 0, "~~ URL TEST, input stream acquired" );
	         out = new ByteArrayOutputStream();
	         Util.transferData( in, out, 2048 );
	         in.close();
	
	         Log.debug( 0, "~~ URL TEST, data transfer completed" );
	         Log.debug( 0, "~~ URL TEST, received size: ".concat( String.valueOf( out.size() )) );
	         
	         in2 = new ByteArrayInputStream( out.toByteArray() );
	         file = File.createTempFile( "jpws-", null );
	         file.deleteOnExit();
	         fout = new FileOutputStream( file );
	         Util.transferData( in2, fout, 2048 );
	         fout.close();
	         
	         Log.debug( 0, "~~ URL TEST, data transfered to external file" );
	         Log.debug( 0, "~~ URL TEST, external size: ".concat( String.valueOf( file.length() )) );
	         
	         openFileToShelf( file.getAbsolutePath() );
	         Log.debug( 0, "~~ URL TEST, open file to shelf" );
	      }
	      catch ( Exception e )
	      {
	         Log.debug( 0, "*** EXCEPTION : " + e );
	         e.printStackTrace();
	      }
	   }
	*/   
	   /** Causes a new password file to be created and displayed in the GUI
	    *  (includes user passphrase input). Optionally a record import can be
	    *  performed at the same time. Requires non-EDT thread to perform!
	    *  
	    *  @param imports <code>PwsRecordList</code> building the initial content
	    *         of the new database, marked as "IMPORTED"; may be <b>null</b>
	    *  
	    *   @return <code>PwsFileContainer</code> the newly installed file
	    *           or <b>null</b> if installation was not completed
	    *  @since 0-5-0
	    */
	   public static PwsFileContainer newFileToShelf( final PwsRecordList imports )
	   {
	      final PwsFile pws;
	      final PwsFileContainer ct;
	      final Object lock = new Object();
	
	      ActionHandler.checkForThread();
	      
	      // create a new database file 
	      pws = IOManager.getNewDatabase();   
	      ct = new PwsFileContainer( pws );
	      ct.setSecurityLoops( Options.getIntOption( "newFileSecurity" ) );
	//   	  ct.getMayorOptions().setOption( "passwordPolicy", 
	//   			  			Global.passphrasePolicy.getInternalForm() );
	
	      Runnable run = new Runnable () {
	    	  
	    	  public void run () {
	      // create the database specification dialog
	      DatabaseDialog dlg = new DatabaseDialog( ct, "dlg.database.new", imports == null );
	      dlg.setAutonomous( true );
	      dlg.clearBarListeners();
	      dlg.addButtonBarListener( dlg.new BarListener() 
	      {
	         @Override
			 public boolean okButtonPerformed () {
	            boolean ok  = false;
	
	            if ( super.okButtonPerformed() ) {
	               // obtain a new passphrase from user (may cancel operation)
	               PwsPassphrase pass = GUIService.enterNewPassphrase( null, ResourceLoader.getDisplay( "ui.newtitle" ) );
	               if ( pass != null ) {
	                  pws.setPassphrase( pass );
	                  
	                  // optionally perform merge
	                  if ( imports != null ) {
	                     pws.merge( imports, 0, true );
	                  }
	      
	                  // install database into GUI framework 
	                  if ( ! (ok = DisplayManager.registerContainer( ct )) ) {
	                     // attempt to clear desktop before register
	                     ok = DisplayManager.closeForOne() && 
	                          DisplayManager.registerContainer( ct );
	                  }
	               }
	
	               Log.debug(9, "(ActionHandler.newFileToShelf) OK button before NOTIFY");
	               synchronized ( lock )
	               { lock.notify(); }
	            }
	            return ok;
	         }
	
	         @Override
			public void cancelButtonPerformed () {
	            super.cancelButtonPerformed();
	            Log.debug(9, "(ActionHandler.newFileToShelf) CANCEL button before NOTIFY");
	            synchronized ( lock )
	            { lock.notify(); }
	         }
	      } );
	      
	      dlg.show();
	      Log.debug(9, "(ActionHandler.newFileToShelf) dialog shown (before WAIT)");
	      }
	      };
	      ActionHandler.executeOnEDT(run);
	      
	      // wait until dialog has terminated
	      synchronized ( lock ) {
	         try { 
	        	 lock.wait(); 
	         } catch ( Exception e ) {
	            e.printStackTrace();
	         }
	         Log.debug(9, "(ActionHandler.newFileToShelf) dialog terminated (after WAIT)");
	      }
	//      dlg.joinButtonThreads();
	      return ct == Global.getSelectedFile() ? ct : null;
	   }  // newFile

	/**
	    * Opens a specific Pws database file and makes it available for general 
	    * purpose. Function is dialog active and asks the user for the 
	    * passphrase in a loop of possibly multiple access validation attempts.
	    * The user is informed in case of passphrase failure, file-not-found or 
	    * other exceptions. If there exists a cache entry of the requested file
	    * in IOManager, the cached instance is returned without asking for 
	    * password. Can be run on any thread.
	    * 
	    * @param file <code>ContextFile</code> of PWS persistent state to be opened
	    * @param acm <code>FileAccessModus</code> method of file retrieving 
	    * @return a <code>PwsFile</code> object or <b>null</b> if 
	    *         the parameter is void or the user pressed CANCEL during
	    *         GUI passphrase verification or an IO error occurred
	    */
	   public static PwsFile openFilePws( ContextFile file, FileAccessModus acm )
	   {
	      PwsFile        pws;
	      PwsPassphrase  pass;
	      PasswordDialog pwDlg;
	      String         hstr;
	      boolean        exitChoice;
	
	      if ( file == null ) return null;
	      Log.log( 8, "(ActionHandler.openFilePws (input)) enter with modus " + acm );
	      
	      // I. pre-selection phase (use system open files if opted)
	      pws = null;
	      switch ( acm )
	      {
	      case directAccess:
	         break;
	         
	      case cachePreferred:
	         pws = IOManager.getDatabase( file );
	         break;
	
	      case desktopSelection:
	         // if the requested file is open in the desktop
	         // let user decide on load source
	         pws = getUserChosenContainerFile( file );
	      }
	      if ( pws != null ) {
	         return pws;
	      }
	      
	      // II. test for file existence (and report)
	      try {
	         if ( !file.exists() )
	            // this invokes the FileNotFound error message
	            return openFilePws( file, new PwsPassphrase(), acm );
	      }
	      
	      // during test a FTP login may be requested from the user
	      catch ( LoginFailureException e ) {
	         Log.debug( 5, "() FTP Login Failure: " + e ); 
	         hstr = ResourceLoader.getDisplay( "msg.url.loginfailed" );
	         try { hstr = Util.substituteText( hstr, "$domain", file.getUrl().getHost() ); }
	         catch ( Exception e1 ) {}
	         GUIService.infoMessage( "dlg.connect.failure", hstr );
	         return null;
	      }
	      catch ( UserCancelException e ) {
	         return null;
	      }
	      catch ( IOException e ) {
	         GUIService.failureMessage( "msg.url.failconnect", e );
	         return null;
	      }
	
	      // III. passphrase (user input) attempt phase
	      // reduce access possible modi to exclude desktop selection
	      if ( acm == FileAccessModus.desktopSelection ) {
	         acm = FileAccessModus.directAccess;
	      }
	      
	      while ( pws == null ) {
	         // enter password dialog, optional break
	         hstr = Util.fileNameOfPath( file.getFilepath() );
	         exitChoice = Global.getSelectedFile() == null;
	         try {
				pwDlg = PasswordDialog.performed( null, hstr, PasswordDialog.ACCESS, exitChoice );
			 } catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			 }
	         if ( !pwDlg.isOkPressed() ) return null;
	
	         // attempt to open file with entered password
	         pass = new PwsPassphrase( pwDlg.getEnteredPassword() );
	         pws = openFilePws( file, pass, acm );
	      }
	      return pws;
	   }

	/**
	    * Opens a specific Pws database file with a specific passphrase
	    * and makes it available for general purpose. Function is dialog active
	    * and the user is informed about passphrase failure, file-not-found or 
	    * other exceptions. (This method does not request for user input; it
	    * can be run on any thread.)
	    * 
	    * @param url url of Pws file to be opened
	    * @param pass the <code>PwsPassphrase</code> to access the file
	    * @param acm boolean if <b>true</b> the IO-Manager's database cache
	    *        is ignored and the file accessed directly via file-IO (still in the 
	    *        framework of the IO-Manager)
	    * @return a PwsFile object or <b>null</b>. Returns <b>null</b> if a 
	    *         parameter is void or the operation failed (e.g. due to
	    *         mismatched passphrase or file-not-found error, etc.)
	    */
	   public static PwsFile openFilePws( ContextFile file, PwsPassphrase pass, 
	                                      FileAccessModus acm )
	   {
	      PwsFile pws;
	      String hstr, path;
	
	      if ( file == null | pass == null ) return null;
	      
	      path = file.getFilepath();
	      pws = null;
	      Log.log( 8, "(ActionHandler.openFilePws (passphrase)) enter with modus " + acm);
	      
	      try {
	         pws = elementalAccessOpenPws( file, pass, acm );
	         
	      } catch ( InvalidPassphraseException e ) {
	         // error: failed password
	         GUIService.infoMessage( "dlg.accessdenied", "msg.failpassword" );
	
	      } catch ( Exception e ) {
	         // test whether file exists on external medium
	         // and inform user nicely if not
	         try {
	            if ( !file.exists() ) {
	               // nice message
	               Log.log( 5, "(ActionHandler.openFilePws-Passphrase) file not found: ".concat( path ) );
	               GUIService.fileNotExistsInfo(file);
	
	            } else {
	               // error: failed file read: WARNING message to user
	               e.printStackTrace();
	               boolean wrongFileType = e instanceof EOFException ||
	            		   e instanceof UnsupportedFileVersionException;
	               hstr = wrongFileType ? "msg.wrongfile" : "msg.failure";
	
	               GUIService.failureMessage( hstr, e );
	               if ( (pws = getLatestSecurityCopy( file, pass )) != null )
	                  pws.setPersistentFile( file );
	            }
	
	         } catch ( IOException e1 ) {
	            // error during application adapter's IO accessing (rare event)
	            Log.log( 5, "(ActionHandler.openFilePws-Passphrase) failed connect: ".concat( path ) );
	            GUIService.failureMessage( "msg.url.failconnect", e );
	         }
	      }
	
	      // return the open database or null if wasn't obtainable
	      if ( pws != null )
	         Log.log( 5, "(ActionHandler.openFilePws-Passphrase) exit with returning DB: ".concat( pws.getFilePath() ) );
	      return pws;
	   }

	/**
	    * Lets the user select a PWS database from a file chooser panel
	    * and starts a worker task to open it into the GUI shelf.
	    * Includes a user passphrase input dialog.  Requires EDT to perform!
	    * 
	    * @return <code>PwsFileContainer</code> GUI container for given database
	              or <b>null</b> if loading to shelf failed
	    */
	   public static void openFileToShelf()
	   {
	      // file open dialog
		  ActionHandler.checkForEDT();
	      JFileChooser fc = new FileOpenDialog( FileOpenDialog.PWSFILE_FILTER | 
	                               FileOpenDialog.BACKUP_FILTER, 
	                               Global.currentDir );
	      int answer = fc.showOpenDialog(Global.getActiveFrame());
	      Global.currentDir = fc.getCurrentDirectory();

	      // start worker task to open file (if user opted) 
	      if ( answer == JFileChooser.APPROVE_OPTION ) {
	    	 final File selectedFile = fc.getSelectedFile();
	    	 
	    	 Runnable run = new Runnable() {
				@Override
				public void run() {
				   try {	
			          openFileToShelf( selectedFile.getPath() );
				   } catch (Throwable e) {
					   e.printStackTrace();
				   }
				}
	    	 };
	    	 ActionHandler.startTask(run);
	      }
	   }  // openFile

		/**
	    * Opens a specific PWS database from a context file definition of its
	    * persistent state and puts it into the GUI framework for user 
	    * processing. Can be run on any thread.
	    *  
	    * @param file <code>ContextFile</code> specification of database file to be loaded
	    * @return <code>PwsFileContainer</code> GUI container for given database
	              or <b>null</b> if loading to shelf failed
	    */
	   public static PwsFileContainer openFileToShelf( ContextFile file )
	   {
	      // avoid multiple exclusive tasks at a time
//	      if ( !Global.requestCriticalPhase( file ) ) return null;
	      Log.log( 5, "(ActionHandler.openFileToShelf) attempt open file to Shelf: ".concat( file.getFilepath() ) );
	      
	      // control of double file open
	      if ( Global.isOpenFile( file ) ) {
	         GUIService.infoMessage( null, "msg.doubleopen" );
//	         Global.endCriticalPhase( file );
	         return null;
	      }
	      
	      // open the specified PWS database (general use)
	      PwsFile pws = openFilePws( file, FileAccessModus.directAccess );
	      if ( pws == null ) {
//	         Global.endCriticalPhase( file );
	         return null;
	      }
		  final PwsFile pws2 = IOManager.cacheDatabase( pws );
		  final PwsFileContainer[] result = new PwsFileContainer[1];
		  
		  Runnable run = new Runnable() {
			@Override
			public void run() {
			   PwsFileContainer fco = putFileToShelf( pws2 );
			   result[0] = fco;
			}
		  };
		  try {
			ActionHandler.executeOnEDT_Wait(run);
		  } catch (InterruptedException e) {
			e.printStackTrace();
		  } catch (InvocationTargetException e) {
			e.printStackTrace();
		  }
	      
	//      Log.log( 5, "** END open file task: ".concat( file.getFilepath() ));
//	      Global.endCriticalPhase( file );
	      return result[0];
	   }  // openFile

	/**
	    * Opens a specific Pws database into the GUI desktop for user processing.
	    * The parameter may denote a valid URL expression (any supported protocol) 
	    * or a local file path.  Can be run on any thread.
	    *  
	    * @param filepath String database filepath (may be an URL)
	    * @return <code>PwsFileContainer</code> GUI container for given database
	    *         or <b>null</b> if loading to shelf failed
	    */
	   public static PwsFileContainer openFileToShelf( String filepath )
	   {
	      // generate URL from filepath 
	      try {
	         Log.log( 5, "(ActionHandler.openFileToShelf) attempt open file from path: ".concat( filepath ) );
	         ContextFile file = Global.getContextFile( Util.makeFileURL( filepath ) );
	         return openFileToShelf( file );
	
	      } catch ( Exception e ) {
	         e.printStackTrace();
	         GUIService.failureMessage( "msg.url.formerror", e );
	         return null;
	      }
	   }

	 /** A non-modal dialog to load a database from an URL location.
	  */
	 private static class OpenFileToShelf_Dialog extends ButtonBarDialog {
	      private JButton clearButton;
	      private RecentList recentURLs;
	      private JTextField textFld;
	      private JComboBox combo;
	      private JPanel panel;
	      private boolean isUseRecents;
	      
		public OpenFileToShelf_Dialog () {
		    super( Global.getActiveFrame(), DialogButtonBar.OK_CANCEL_HELP_BUTTON, false );
		    isUseRecents = Options.isOptionSet( "useRecentList" );
			init();
		}
		
		private void init () {
		   markSingleton( "OpenFileToShelf_URL" );
		   setTitle( ResourceLoader.getDisplay( "dlg.urlcon" ) );
		   moveRelatedTo( Global.mainFrame );
		   setAutonomous( true );
			
	      // extra button: CLEAR
	      clearButton = new JButton( ResourceLoader.getDisplay( "button.clear" ) );
	      getButtonBar().add( clearButton );
	      
	      panel = new JPanel();
	      panel.add( new JLabel( "URL: ") );
	      
	      // init input component
	      if ( isUseRecents ) {
	         // combo-box input
	         recentURLs = new RecentList( 8 );
	         recentURLs.setContent( Options.getOption( "connector.URL.recent" ) );
	         combo = new JComboBox( recentURLs );
	         Dimension dim = combo.getPreferredSize();
	         combo.setPreferredSize( new Dimension( 350, dim.height ) );
	         if ( recentURLs.getSize() > 0 ) {
	            combo.setSelectedItem( recentURLs.getFirst() );
	         }
	         combo.setEditable( true );
	         panel.add( combo );
	         textFld = null;
	
	      } else {
	         // text field input
	         textFld = new JTextField(35);
	         panel.add( textFld );
	         combo = null;
	         recentURLs = null;
	      }
	
	      addButtonBarListener( new DefaultButtonBarListener( this )
	      {
	         @Override
			 public boolean okButtonPerformed () {
	            String choice, choicePrev, hstr;
	            URL url;
	
	            dispose();
	            
	            // get user input for URL
	            if ( isUseRecents ) {
	               choice = (String)combo.getSelectedItem();
	            } else {
	               choice = textFld.getText();
	            }
	            
	            try {
	               // if user input is available
	               if ( choice != null && !choice.isEmpty() ) {
	            	  // determine protocol useable
	            	  choice = choice.trim();
	            	  hstr = choice.toLowerCase();
	            	  boolean isFTP = hstr.startsWith("ftp://");
	//            	  boolean isHTML = hstr.startsWith("http://");
	//            	  boolean isFTPS = hstr.startsWith("ftps://");
	
	            	  // pre-evaluation and input extrapolation
	                  try { 
	                	  url = new URL( choice ); 
	                  } catch ( MalformedURLException e ) {
	                	 // if URL is malformed: prepend it to a HTTP protocol 
	             		 choicePrev = choice;
	                	 if ( choice.indexOf("://") == -1 ) { 
	                		 choice = "http://".concat( choice );
	//                		 isHTML = true;
	                	 }
	                     url = new URL( choice );
	                     
	                     if ( isUseRecents ) {
	                        recentURLs.replaceRecent( choicePrev, choice );
	                     }
	                  }
	                  
	                  // operational
	                  if ( isUseRecents ) {
	                     Options.setOption( "connector.URL.recent", 
	                    		 recentURLs.getStringContent() );
	                  }
	   
	                  // create normalised context file for URL
	                  String path = url.toExternalForm();
	                  ContextFile file = Global.getContextFile( Util.makeFileURL(path) );
	                  
	                  // if file does not exist
	                  if ( !file.exists() ) {
	                	 if ( isFTP && Options.isOptionSet("allowFTPcreate") ) {
	                		 // if FTP : attempt create new database
	                		 // create user message
		                	 String text = ResourceLoader.getDisplay("msg.url.notexistsfile");
		                	 text = Util.substituteText(text, "$domain", url.getHost());
		                	 text = Util.substituteText(text, "$path", url.getPath());
		                	 
		                	 if ( GUIService.userConfirm(getDialog(), "dlg.ftp.filecreation", text) ) {
		                		// create a new file in the desktop 
		                		PwsFileContainer fco = newFileToShelf(null);
		                		if ( fco != null ) {
		                			// save new file to our target file
		                			fco.saveAs(file, true);
		                		}
		                	 }
	
	                	 } else { 
	                		 GUIService.fileNotExistsInfo( file );
	                	 }
	                	 
	                  } else {
	                	  // file exists
	                	  openFileToShelf( file );
	                  }
	               }
	
	            } catch ( MalformedURLException e ) {
	               GUIService.infoMessage( null, "msg.url.formerror" );
	            }  catch ( IOException e ) {
	     	         GUIService.failureMessage( "msg.url.failconnect", e );
	            }  catch ( Exception e ) {
	     	         GUIService.failureMessage( "msg.failure.general", e );
	     	    }
	            return false;
	         }
	
	        @Override
			public void helpButtonPerformed () {
	           GUIService.toggleHelpDialog( getDialog(), "dlg.help.open_url" );
	        }
	
	        @Override
			public boolean extraButtonPerformed ( Object button ) {
	           if ( button == clearButton & recentURLs != null &&
	        	    GUIService.userConfirm(getDialog(), "msg.ask.delete.recentlist") ) {
	        	 // delete recent file list (URL) after user confirm
	             recentURLs.clear();
	             Options.setOption( "connector.URL.recent", recentURLs.getStringContent() );
	           }
	           return true;
	        }
	      } );
	      
	      setDialogPanel( panel );
		}
	 }
	   
	/**
	    * Starts dialog which lets the user setup a URL net connection to a Pws 
	    * database using a simple text input field. This will open the database 
	    * into the GUI framework if successful. (Includes user passphrase input.)
	    * Requires to run on EDT!
	    */
	   public static void openFileToShelf_Url()
	   {
	      // avoid double open of this dialog
//	      if ( Global.isDialogActive( "OpenFileToShelf_URL" ) ) return;
	      Log.log(5, "(DatabaseHandler.openFileToShelf_Url) running");
	      
	      ActionHandler.checkForEDT();
	      ButtonBarDialog dialog = new OpenFileToShelf_Dialog();
		  dialog.show();
	   }  // openFileToShelf_Url

	/** Brings up a file chooser dialog and lets the user select an existing 
	    *  or define a new PWS file in the local file system. Runs on any 
	    *  thread.
	    *  
	    * @param parent owner window to place the dialog 
	    * @param title the dialog title (code or real)
	    * @param dir File optional browsing start directory (may be <b>null</b>)
	    * @return the opened PWS file or <b>null</b> if failed
	    * @since 0-5-0 modified parameter list
	    */
	   public static PwsFile openOrNewFile ( final Component parent, 
			                                 final String title, 
			                                 final File dir )
	   {
	      ContextFile cf;
	      PwsFile pws;
	      PwsPassphrase  pass;
	      File file;
	      String path;

	      final FileOpenDialog[] darr = new FileOpenDialog[1];
	      
	      // file open dialog
	      Runnable run = new Runnable() {

			@Override
			public void run() {
			   FileOpenDialog dlg = new FileOpenDialog( FileOpenDialog.PWSFILE_FILTER, 
		               dir != null ? dir : Global.currentDir );
		       dlg.setDialogTitle( ResourceLoader.codeOrRealDisplay( title ) );
		       dlg.showOpenDialog(parent);
		       darr[0] = dlg;
			}
	      };
	
	      try {
			 ActionHandler.executeOnEDT_Wait(run);
		  } catch (InterruptedException e1) {
			 e1.printStackTrace();
		  } catch (InvocationTargetException e1) {
			 e1.printStackTrace();
		  }
	      
	      if ( darr[0].getUserChoice() != JFileChooser.APPROVE_OPTION ) {
	         return null;
	      }
	
	      try {
	         file = darr[0].getSelectedFile();
	         path = Service.normalizedFilepath( file.getPath(), 
	               Global.DEFAULT_FILEEXTENTION );
	
	         if ( file.isFile() ) {
	            cf = IOManager.makeLocalContextFile( path );
	            pws = openFilePws( cf, FileAccessModus.cachePreferred );
	            if ( pws != null )            
	               Log.log( 3, "(ActionHandler) opened existing PWS file: " + pws.getFilePath() );

	         } else {
	            // obtain a new passphrase from user (may cancel operation)
	            pass = GUIService.enterNewPassphrase( null, Util.fileNameOfPath( path ) );
	            if ( pass == null ) return null;
	   
	            // define a new file 
	            pws = new PwsFile(); 
	            pws.setPassphrase( pass );
	            pws.setApplication( IOManager.getLocalFileAdapter() );
	            pws.setFilePath( path );
	            pws = IOManager.getRegisteredDatabase( pws );
	            Log.log( 3, "(ActionHandler) created new PWS file: " + pws.getFilePath() );
	         }

	      } catch ( IOException e ) {
	         GUIService.failureMessage( "msg.url.formerror", e );
	         return null;
	      }
	      
	      return pws;
	   }  // openOrNewFile

	/**
	    * Attempts to open the specified file as a <code>PwsFile</code> database. 
	    * In the first step the specified parameter passphrase is tried. If fails, 
	    * a user input loop is initiated to query for the correct passphrase.
	    * Runs on any thread.
	    * 
	    * @param file <code>ContextFile</code> file to be opened
	    * @param pass <code>PwsPassphrase</code>
	    * @param directAccess boolean if <b>true</b> the IO-Manager's database cache
	    *        is ignored and the file accessed directly via file-IO (still in the 
	    *        framework of the IO-Manager)
	    * @return a <code>PwsFile</code> object or <b>null</b> if the attempt failed
	    * @since 0-5-0
	    * @since 0-6-0 modified parameters
	    */
	   public static PwsFile passTryOpen ( ContextFile file, PwsPassphrase pass,
	                                       FileAccessModus acm )
	   {
	      // request critical phase
		  if ( file == null ) return null;
//	      if ( !Global.requestCriticalPhase( file ) ) return null;
	      
	      Log.log( 8, "(ActionHandler.passTryOpen) enter with modus " + acm);
	      
	      // attempt open with given passphrase
	      PwsFile pws;
	      try {
	         pws = elementalAccessOpenPws( file, pass, acm );
	         
	      } catch ( Exception e ) {
	         Log.log( 8, "(ActionHandler.passTryOpen) first attempt failed with Ex = " + e);
	         
	         // if failed, open with user passphrase input
	         pws = openFilePws( file, acm );
	      }
	
	      // revoke critical phase
//	      Global.endCriticalPhase( file );
	      Log.log( 8, "(ActionHandler.passTryOpen) exit");
	      return pws;
	   }

	/**
	    * Attempts to add the given database file to the GUI workshelf and load
	    * the parameter PWS file as actual work file. Several checks are
	    * performed before and after loading, possibly ensuing user information.
	    * These checks are: checksum control, invalid records check, expiring records 
	    * check, file format conversion, check mirror existence. Requires EDT!
	    *  
	    *  
	    * @param pws <code>PwsFile</code> Pws database file to be loaded
	    * @return <code>PwsFileContainer</code> GUI container for given database
	              or <b>null</b> if loading to shelf failed
	    * @since 0-5-0
	    */
	   public static PwsFileContainer putFileToShelf( PwsFile pws )
	   {
		   ActionHandler.checkForEDT();
		   
	      // test integrity of database (checksum)
	      if ( pws.getFormatVersion() == 3 && !pws.isChecksumVerified() ) {
	    	 String hstr = ResourceLoader.getDisplay( "msg.warning.badfilecrc" );
	         hstr = Util.substituteText( hstr, "$file", pws.getFilePath() );
	         if ( !GUIService.userConfirm( hstr ) ) return null;
	      }
	
	      // ensure database is registered in IOManager
	      try { 
	    	  pws = IOManager.getRegisteredDatabase( pws ); 
	      } catch ( IOException e ) {
	         e.printStackTrace();
	         GUIService.failureMessage( "msg.desktop.loadfailure", e );
	      }
	      
	      // install database into GUI framework for processing and display
	      PwsFileContainer fco = new PwsFileContainer( pws );
	      synchronized ( DisplayManager.class ) {
	         if ( !DisplayManager.registerContainer( fco ) ) {
	
	        	// attempt to clear desktop before register
	            if ( !DisplayManager.closeForOne() ) return null;
	            if ( !DisplayManager.registerContainer( fco ) ) {
	               GUIService.failureMessage( "msg.desktop.loadfailure", null );
	               return null;
	            }
	         }
	      }
	
	      // AFTER INSTALLATION TO SHELF
	      
	      // update recent file list 
	      Global.pushRecentFile( fco );
	      
	      // control existence of file mirrors
	      if ( Options.isOptionSet( "useDataMirrors" ) ) {
	         fco.controlMirrors();
	      }
	
	      // if container was not shut down during controlMirrors()
	      if ( fco.getPwsFile() != null ) {
	    	  
	         // offer file format conversion where applicable
	         fco.convert();
	         
	         // inform user about invalid records
	         fco.showInvalidsMessage( false );
	         
	         // inform user about expired records
	         if ( Options.isOptionSet( "expiryCheck" ) ) {
	            fco.showExpiredMessage( false );
	         }
	      }
	      return fco;
	   }  // putFileToShelf

	  /** Reopens the most recently used file or a file that comes next to this.
	    * The behaviour is dependent on program operation mode (NORMAL or 
	    * PORTABLE). Runs on any thread.
	    * 
	    * @since 0-5-0
	    */
	   public static void reopenFileFromRecents () {
		  if (  Global.recentFiles.getFirst() == null ) return;  
	
        String path = Global.recentFiles.getFirst().toString();
         
        // PORTABLE mode start
        if ( Global.isPortable() ) {
        	 
           // if first list item is not a file protocol, open it directly
           if ( path != null && !Util.isFileProtocol( path ) ) {
              System.out.println("-- Start Recent (PORTABLE III): ".concat( path ) );
              openFileToShelf( path );
              return;
           }

           // first walk-through: try open any path in recent list
           // which matches the filename of the first path 
           // (only for local files)
           String target = Util.fileNameOfPath( path );
           int count = 0;
           for ( Iterator<?> it = Global.recentFiles.iterator(); it.hasNext(); count++ ) {
              path = it.next().toString();
              if ( Util.isFileProtocol( path ) && path.endsWith( target ) &&
                   new File( path ).exists() ) {
            	   
                 System.out.println("-- Start Recent (PORTABLE I): ".concat( path ) );
                 openFileToShelf( path );
                 if ( count != 0 ) {
                    GUIService.warningMessage( Global.getActiveFrame(), null, "msg.file.notmostrecent" );
                 }
                 return;
              }
           }
            
           // second walk-through: try open first path which
           // has a living file (only for local files)
           for ( Iterator<?> it = Global.recentFiles.iterator(); it.hasNext(); ) {
              path = it.next().toString();
              if ( Util.isFileProtocol( path ) && new File( path ).exists() ) {
                 System.out.println("-- Start Recent (PORTABLE II): ".concat( path ) );
                 openFileToShelf( path );
                 GUIService.warningMessage( Global.getActiveFrame(), null, "msg.file.notmostrecent" );
                 return;
              }
           }
            
//           // last resort: open-or-new-file dialog
//           PwsFile pws = openOrNewFile( Global.getActiveFrame(), "dlg.open.existornewfile", null );
//           if ( pws != null ) {
//              putFileToShelf( pws );
//           }
        }
         
        // NORMAL mode start
        else {
           if ( path != null ) {
              System.out.println("-- Start Recent (NORMAL): ".concat( path ) );
              openFileToShelf( path );
           }
        }
     }

	/** Returns the PWS file as specified by parameter if it is available
	    * in the desktop and if user has chosen for the desktop version in case
	    * it differs from the stored (external) version. Can be run on any 
	    * thread.
	    *   
	    * @param file ContextFile defines the external source of the PWS file
	    * @return <code>PwsFile</code> container version of the database or
	    *        <b>null</b> if unavailable or user opted for external version
	    */
	   private static PwsFile getUserChosenContainerFile ( ContextFile file )
	   {
	      PwsFile pws = null;
	
	      // if the requested file is open in the desktop
    	  PwsFileContainer ct = DisplayManager.getFileContainer( file ); 
	      if ( ct != null ) {
	         if ( !ct.isModified() ) {
	            // take desktop copy if unmodified
	            pws = IOManager.getDatabase( file );

	         } else {
	            // if modified, let user decide on load source (desktop or external)
	            String hstr = ResourceLoader.getDisplay( "msg.ask.fileopensource" );
	            hstr = Util.substituteText( hstr, "$file", file.getFilepath() );
	            String[] choices = new String[]
	              { "confirm.loadsource.desktop", "confirm.loadsource.external" };
	            if ( GUIService.userOptionInput( null, null, hstr, choices, false ) == 0 ) {
	               // take desktop copy if user opted
	               pws = IOManager.getDatabase( file );
	            }
	         }
	      }
	      return pws;
	   }
	
}
