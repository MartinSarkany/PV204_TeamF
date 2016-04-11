/*
 *  PortableInstallDialog in org.jpws.front
 *  file: PortableInstallDialog.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 02.04.2010
 *  Version
 * 
 *  Copyright (c) 2010 by Wolfgang Keller, Munich, Germany
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.jpws.data.IOManager;
import org.jpws.data.Options;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.exception.ApplicationFailureException;
import org.jpws.pwslib.global.Log;

public class PortableInstallDialog extends ButtonBarDialog 
                                   implements ActionListener
{
   private Thread       actionThread;

   private JButton      actionButton;
   private JButton      fileButton;
   private JComboBox    iniFileCombo;
   private JCheckBox    copyWindowsJreChk;
   private JCheckBox    copyLinuxJreChk;
   private JCheckBox    createBatchesChk;
   private JCheckBox    copyJavaChk;
   private JCheckBox    userFilesChk;
   private JLabel       filepathLabel;
   private JLabel       storeSizeLabel;
   private JLabel       warningLabel;
   private File         exeProgram1, exeProgram2; 
   private File         jar1Program, jar2Program; 
   private File         windowsJre_directory; 
   private File         linuxJre_directory; 
   private File         userFiles_directory; 
   private File         java_directory; 
   private File         installDir;
   private ContextFile  iniFile; 
   private String       oldIniOption;
   private String 		javaText = "";
   private long			javaSize, ljreSize, wjreSize;
   private boolean      hasExe2, hasExe1, hasJar2, hasJar1, 
                        hasWindowsJRE, hasLinuxJRE, hasUserFiles;
   private boolean 		spaceVerified;

public PortableInstallDialog ()
{
   super( Global.getActiveFrame(), DialogButtonBar.OK_CANCEL_BUTTON, false );
   init();
}

private void init ()
{
   JPanel cpanel, alphaPanel;
   JLabel label;
   String hstr, iniOptions[];
   boolean meetConditions;
   
   // data setup
   hstr = System.getProperty("os.name").toLowerCase();
//   isLinux = hstr.indexOf("linux") > -1;
//   isWindows = hstr.indexOf("windows") > -1;
   exeProgram1 = new File( Global.programDir, Global.EXE_PROGRAMNAME_1 );
   exeProgram2 = new File( Global.programDir, Global.EXE_PROGRAMNAME_2 );
   jar1Program = new File( Global.programDir, Global.JAR_PROGRAMNAME_1 );
   jar2Program = new File( Global.programDir, Global.JAR_PROGRAMNAME_2 );
   windowsJre_directory = new File( Global.programDir, "wjre" );
   linuxJre_directory = new File( Global.programDir, "ljre" );
   java_directory = new File( System.getProperty("java.home"));
   userFiles_directory = Global.defaultFileDir;
   iniFile = Options.getPersistentFile();
   
   // determine what files or directories are available
   hasExe1 = exeProgram1.isFile();
   hasExe2 = exeProgram2.isFile();
   hasJar1 = jar1Program.isFile();
   hasJar2 = jar2Program.isFile();
   hasWindowsJRE = windowsJre_directory.isDirectory();
   hasLinuxJRE = linuxJre_directory.isDirectory();
   hasUserFiles = userFiles_directory.isDirectory() && 
		          userFiles_directory.list().length > 0;
   
   // load combo box values
//   va = ResourceLoader.getCommand( "combo.install.installtypes" ).split( "," );
//   jarOption = va[0];
//   exeOption = va[1];
//   allOption = va[2];
   iniOptions = ResourceLoader.getCommand( "combo.install.inifiletypes" ).split( "," );
   oldIniOption = iniOptions[0];
   
   // whether we can operate
   meetConditions = hasExe1 | hasExe2 | hasJar1 | hasJar2;

   // dialog
   moveRelatedTo( Global.getActiveFrame() );
   setTitle( ResourceLoader.getDisplay( "dlg.installportable" ) );
   setAutonomous( true );
   markSingleton( "PortableInstallation" );
   
   // button bar
   actionButton = getButtonBar().getOkButton();
   actionButton.setText( ResourceLoader.getDisplay( "button.install" ) );
   actionButton.setEnabled( false );
   this.addButtonBarListener( new BarListener() );
   
   // content base panel
   alphaPanel = new JPanel( new BorderLayout( 0, 25 ) );
   alphaPanel.setBorder( BorderFactory.createEmptyBorder( 10, 30, 15, 30 ) );
   hstr = meetConditions ? "msg.install.enter" : "msg.install.unavailable"; 
   label = new JLabel( ResourceLoader.getDisplay( hstr ) );
   label.setFont( label.getFont().deriveFont( Font.PLAIN ) );

   cpanel = new JPanel( new VerticalFlowLayout( 12, true ));
   alphaPanel.add( label, BorderLayout.NORTH );
   alphaPanel.add( cpanel, BorderLayout.CENTER );

   if ( meetConditions ) {
      // select INI-file source
      iniFileCombo = new JComboBox( iniOptions );
      iniFileCombo.addActionListener( this );
      if ( iniFile != null ) {
         cpanel.add( iniFileCombo );
      }
      
      JPanel panel = new JPanel( new VerticalFlowLayout(2) );
      cpanel.add( panel );
      
      // option for creation of start batches
      createBatchesChk  = new JCheckBox( ResourceLoader.getDisplay( "chk.install.batches" ), true );
      createBatchesChk.addActionListener( this );
      panel.add( createBatchesChk );
      
      // option for copy of user files
      userFilesChk  = new JCheckBox( ResourceLoader.getDisplay( "chk.install.userfiles" ), false );
      if ( hasUserFiles ) {
    	  userFilesChk.addActionListener( this );
    	  panel.add( userFilesChk );
      }
      
      // option for copy of current Java Runtime
      javaText = ResourceLoader.getDisplay("msg.wait");
	  copyJavaChk  = new JCheckBox( javaText, false );
	  copyJavaChk.setEnabled(false);
   	  copyJavaChk.addActionListener( this );
      panel.add( copyJavaChk );
      
      // option for Windows JRE copy
      copyWindowsJreChk = new JCheckBox( ResourceLoader.getDisplay( "chk.install.windowsjre" ), false );
      copyWindowsJreChk.setEnabled(false);
      if ( hasWindowsJRE ) {
          copyWindowsJreChk.addActionListener( this );
          panel.add( copyWindowsJreChk );
      }
      
      // option for Linux JRE copy
      copyLinuxJreChk = new JCheckBox( ResourceLoader.getDisplay( "chk.install.linuxjre" ), false );
      copyLinuxJreChk.setEnabled(false);
      if ( hasLinuxJRE ) {
          copyLinuxJreChk.addActionListener( this );
          panel.add( copyLinuxJreChk );
      }
      
      // file chooser for install directory
      fileButton = new JButton( ResourceLoader.getDisplay( "button.install.selectoutput" ) );
      fileButton.addActionListener( this );
      cpanel.add( fileButton );
      
      // text label for install directory path 
      filepathLabel = new JLabel( " " );
      filepathLabel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 0 ) );
      filepathLabel.setForeground( Color.green );
      filepathLabel.setBackground( Color.gray );
      filepathLabel.setOpaque( true );
      cpanel.add( filepathLabel );

      // text label for store size report
      storeSizeLabel = new JLabel(" ");
      cpanel.add( storeSizeLabel );
      
      // text label warnings (red)
      warningLabel = new JLabel(" ");
      warningLabel.setForeground( Color.RED );
      cpanel.add( warningLabel );
      
      prepare_background();
   }
   
   // complete
   setDialogPanel( alphaPanel );
}  // init

/** Starts a separate thread to calculate the size of the current JRE.
 */
private void prepare_background() {
	if ( javaSize > 0 ) return;
	
	SwingWorker<Long, Long> run = new SwingWorker<Long, Long>() {

	@Override
	public Long doInBackground() {
        supplyProgramFiles();

        // calculate space of running JRE installation 
		if ( java_directory.isDirectory() ) {
		   // calculate the java home directory size (JRE)	
		   javaSize = Util.directorySize(java_directory, 8192);

		   // prepare checkbox text, with JRE version if feasible
		   javaText = ResourceLoader.getDisplay( "chk.install.java" );
		   String hstr = System.getProperty("java.specification.version");
		   if ( hstr == null ) {
			   hstr = System.getProperty("java.vm.specification.version");
		   }
		   if ( hstr == null ) {
			   hstr = System.getProperty("java.version");
		   }
		   hstr = hstr == null ? "???" : hstr;
		   javaText = Util.substituteText(javaText, "$version", hstr) + ", size=" + 
		          NumberFormat.getInstance().format(javaSize/1000000) + " MB";
		}

        // calculate space of LJRE directory 
		if ( linuxJre_directory.isDirectory() ) {
		   ljreSize = Util.directorySize(linuxJre_directory, 8192);
		}
		
        // calculate space of WJRE directory 
		if ( windowsJre_directory.isDirectory() ) {
		   wjreSize = Util.directorySize(windowsJre_directory, 8192);
		}
		return 0L;
	}
	
	@Override
	public void done () {
		boolean ok = javaSize > 0;
		copyJavaChk.setText(ok ? javaText : null);
		copyJavaChk.setEnabled(ok);
		copyJavaChk.setVisible(ok);

  	  	String hstr = ", size=" + NumberFormat.getInstance().format(wjreSize/1000000) + " MB";
  	  	copyWindowsJreChk.setText( copyWindowsJreChk.getText().concat(hstr) );
        copyWindowsJreChk.setEnabled( true );

  	    hstr = ", size=" + NumberFormat.getInstance().format(ljreSize/1000000) + " MB";
  	    copyLinuxJreChk.setText( copyLinuxJreChk.getText().concat(hstr) );
        copyLinuxJreChk.setEnabled( true );

  	    calculateSpace();
	}
	
	};
	ActionHandler.startTask(run);
}

/** Verifies the conditions to commence operation of installation. This sets
 * the operation button enabled or disabled, hence should run on EDT!
 */
private void verifyConditions () {
	Log.log(6, "(PortableInstrallDialog.verifyConditions) conditions are: " +
			   (spaceVerified ? "met" : "not met"));
	actionButton.setEnabled( spaceVerified );
}

/** GUI-active space calculation (refers to creation of an install-list).
 */
private void calculateSpace ()
{
   List<String> iList;
   String iOpt, text, hstr;
   long space;
   double dSpace;
   
   Log.log( 10, "(PortableInstallDialog.calculateSpace) calculating installation space" );
   space = 0;
   spaceVerified = false;
   verifyConditions();
   
   // program copy size
   iList = createInstallList();
   
   for ( String path : iList ) {
	  int index = path.indexOf(';');
	  if (index > -1) {
	     path = path.substring(0, index);
	  }
	  File f = new File(path);
      if ( f.isFile() ) {
         space += f.length();
      }
      else if ( f.isDirectory() ) {
    	 if ( f.equals(java_directory) ) {
    		 space += javaSize;
    	 } else {
    		 space += Util.directorySize( f, 8192 );
    	 }
      }
   }
   
   // INI file size
   iOpt = (String)iniFileCombo.getSelectedItem();
   if ( iOpt.equals( oldIniOption ) & iniFile != null ) {
      space += iniFile.length();
   }
   
   // batch files size
   if ( createBatchesChk.isSelected() ) {
      space += 8000L;
   }
   
   // generously add 10 % for imponderabilities
   space = (long)(space * 1.10);
   
   // format size display
   NumberFormat form = NumberFormat.getNumberInstance(Global.locale);
   form.setMaximumFractionDigits(2);
   dSpace = (double)space / 1000000;
   hstr = form.format(dSpace) + " MB";
   text = ResourceLoader.getDisplay( "label.spacerequired" ) + " " + hstr;
   
   // control + format space on target dir if available
   long free = 0;
   if ( installDir != null ) {
	   free = installDir.getFreeSpace() / 1000000;
	   hstr = ResourceLoader.getDisplay( "label.space_free" );
	   text += "    " + hstr + ": " + form.format(free) + " MB";

	   hstr = free < dSpace ? ResourceLoader.getDisplay( "label.space_warning" ) : null;
	   warningLabel.setText( hstr );
   }
   
   // GUI settings
   storeSizeLabel.setText( text );
   spaceVerified = free > dSpace;
   verifyConditions();
}

/** Supplies as many of the 4 standard installation program files as can be
 * derived by copying.
 */
private void supplyProgramFiles () {
	try {
    if ( !hasJar1 ) {
	   if ( hasJar2 ) { 
	  	  // create a temp copy of JAR2 and activate the copy
	  	  jar1Program = copyProgramFile(Global.JAR_PROGRAMNAME_1, jar2Program);
	  	  hasJar1 = true;
       
       } else if ( hasExe1 | hasExe2 ) {
    	  // create a temp copy of EXE1 or EXE2 and activate the copy
  	  	  File exe = hasExe1 ? exeProgram1 : exeProgram2;
  		  File copy = new File( IOManager.getTempDir(), Global.JAR_PROGRAMNAME_1); 
    	  Service.transferZipfile(exe, copy);
 	  	  copy.setLastModified(exe.lastModified());
 	  	  jar1Program = copy;
 	  	  hasJar1 = true;
 	 	  IOManager.deleteOnExit(copy);
       }
    }
    
    if ( !hasJar2 ) {
       if ( hasJar1 ) {
 	  	  jar2Program = copyProgramFile(Global.JAR_PROGRAMNAME_2, jar1Program);
 	  	  hasJar2 = true;
       }
    }
    
    if ( !hasExe1 ) {
        if ( hasExe2 ) {
  	  	  exeProgram1 = copyProgramFile(Global.EXE_PROGRAMNAME_1, exeProgram2);
  	  	  hasExe1 = true;
        }
     }
     
    if ( !hasExe2 ) {
        if ( hasExe1 ) {
  	  	  exeProgram2 = copyProgramFile(Global.EXE_PROGRAMNAME_2, exeProgram1);
  	  	  hasExe2 = true;
        }
     }
     
    } catch (IOException e) {
    }
}

private File copyProgramFile (String target, File source) 
		throws ApplicationFailureException, IOException {
   File copy = new File( IOManager.getTempDir(), target); 
   ContextFile src = IOManager.makeLocalContextFile(source);
   src.copyTo(IOManager.makeLocalContextFile(copy));
   copy.setLastModified(src.modifyTime());
   IOManager.deleteOnExit(copy);
   return copy;
}

/** Returns a list of <code>File</code> objects which
 * represents to-be-installed files of the target installation. 
 * Entries may refer to files or directories.
 * (Does not contain INI-file and batch-files, which are handled separately.)
 * 
 * @return <code>List</code> of <code>File</code>
 */
private List<String> createInstallList () {
   ArrayList<String> list = new ArrayList<String>();
   
   // the default EXE program file without version number
   if ( hasExe1 ) {
      list.add( exeProgram1.getAbsolutePath() );
   }

   // the EXE program file with version number
   if ( hasExe2 ) {
      list.add( exeProgram2.getAbsolutePath() );
      if ( !hasExe1 ) {
    	  try {
    		 // create a temp copy of EXE2 and add the copy to list
    		 File copy = new File( IOManager.getTempDir(), Global.EXE_PROGRAMNAME_1); 
    		 ContextFile exe2 = IOManager.makeLocalContextFile(exeProgram2);
    		 exe2.copyTo(IOManager.makeLocalContextFile(copy));
    		 copy.setLastModified(exe2.modifyTime());
    		 list.add( copy.getAbsolutePath() );
   		  	 IOManager.deleteOnExit(copy);
    	  } catch (IOException e) {
    	  }
      }
   }

   // the default JAR program file without version number
   if ( hasJar1 ) {
      list.add( jar1Program.getAbsolutePath() );
   }

   // the JAR program file with version number
   if ( hasJar2 ) {
      list.add( jar2Program.getAbsolutePath() );
      if ( !hasJar1 ) {
    	  try {
    		 // create a temp copy of JAR2 and add the copy to list
    		 File copy = new File( IOManager.getTempDir(), Global.JAR_PROGRAMNAME_1); 
    		 ContextFile jar2 = IOManager.makeLocalContextFile(jar2Program);
    		 jar2.copyTo(IOManager.makeLocalContextFile(copy));
    		 copy.setLastModified(jar2.modifyTime());
    		 list.add( copy.getAbsolutePath() );
   		  	 IOManager.deleteOnExit(copy);
    	  } catch (IOException e) {
    	  }
      }
   }
   
   // the user files directory in the user's APPLICATION HOME directory
   // available only if there are files in it
   if ( userFilesChk.isSelected() ) {
      list.add( userFiles_directory.getAbsolutePath() + ";.jpws/files" );
   }
	   
   if ( copyWindowsJreChk.isSelected() ) {
      list.add( windowsJre_directory.getAbsolutePath() + ";wjre");
   }
   
   if ( copyLinuxJreChk.isSelected() ) {
      list.add( linuxJre_directory.getAbsolutePath() + ";ljre");
   }
   
   // the current JRE (running this program)
   if ( copyJavaChk.isSelected() ) {
	  list.add( java_directory.getAbsolutePath() + ";jre");
	  File infoFile = writeJREInfo();
	  if ( infoFile != null ) {
		  list.add(infoFile.getAbsolutePath());
		  IOManager.deleteOnExit(infoFile);
	  }
   }
   return list;
}

/** Writes information about the running JVM into a temporary text file which
 * is returned.
 * 
 * @return <code>ContextFile</code> or null if an IOError occurred
 */
private File writeJREInfo () {
	File file = null;
    try {
	   file = new File( IOManager.getTempDir(), "JRE_info.txt"); 
 	   String text = "Enclosed JRE directory was copied on " + 
 	      Global.getLocalDate(System.currentTimeMillis()) + "\n\n" +
 	      System.getProperty("java.vm.name") + " " + System.getProperty("java.runtime.version") + 
 		  "\n\nArchitecture: " + System.getProperty("os.name") + ", " + System.getProperty("os.arch") +
 		  "\nVendor: " + System.getProperty("java.vm.vendor");  
 	   new ContextFile(file).writeString(text, "ASCII");
 	   
    } catch (IOException e) {
	   e.printStackTrace();
    }
	return file;
}

/** Whether there is an installation action (thread) currently performing. */
public boolean isActionRunning () {
   return actionThread != null && actionThread.isAlive();
}

//  *** the button listener ***

@Override
public void actionPerformed ( ActionEvent e )
{
   // start file open dialog on FILE BUTTON pressed
   if ( e.getSource() == fileButton ) {
      File file, dir;

      // prepare file-chooser
      JFileChooser fc = new FileOpenDialog( 0, Global.exchangeDir );
      fc.setDialogTitle( ResourceLoader.getDisplay( "dlg.install.outputfile" ) );
      fc.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
      String hstr = Options.getOption("lastPortableInstTarget");
      dir = hstr.isEmpty() ? null : new File(hstr);
      fc.setCurrentDirectory(dir);

      if ( installDir != null ) {
         // go back in INSTALL directory hierarchy if needed to get an existing one
         dir = installDir;
         while ( dir != null && !dir.exists() ) 
             dir = dir.getParentFile();
         if ( dir != null )
            fc.setCurrentDirectory( dir );
      }

      // run file-chooser + exploit
      if ( fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION &&
           (file = fc.getSelectedFile()) != null ) {
    	 
    	 if ( !file.isDirectory() ) {
    		GUIService.DirectoryNotExistsInfo(this, file); 
    	 } else {
	        installDir = file;
	        filepathLabel.setText( file.getPath() );
	        filepathLabel.setToolTipText( file.getAbsolutePath() );
    	 }
      }
      Options.setOption("lastPortableInstTarget", fc.getCurrentDirectory().getAbsolutePath());
   }
   
   // reaction to selection changes
   calculateSpace();

}  // actionPerformed


//****************  INNER CLASS ButtonBarListener  **********************

private class BarListener extends DefaultButtonBarListener
{
   public BarListener () {
      super( PortableInstallDialog.this, "dlg.help.portableinstall" );
   }

   @Override
   public boolean okButtonPerformed () {
      List<String> list;
      File batchDir;
      ContextFile ini;
      String iOpt, hstr;
      
      actionThread = Thread.currentThread();
      fileButton.setEnabled( false );
      getButtonBar().getCancelButton().setEnabled(false);
      
      // collect parameters
      try {
         // determine batch directory and option
         batchDir = Util.findRoot( installDir );
         batchDir = batchDir == null ? installDir : batchDir;
         batchDir = createBatchesChk.isSelected() ? batchDir : null;

         // select program files
         list = createInstallList();
         
         // determine INI-file parameter
         iOpt = (String)iniFileCombo.getSelectedItem();
         ini = iOpt.equals( oldIniOption ) & iniFile != null ? iniFile : null;
         
         // execute
         String[] applFs = list.toArray(new String[ list.size() ]);
         hstr = ResourceLoader.getDisplay("msg.warning.wait_on_copy");
         warningLabel.setText(hstr);
         Service.createPortableInstallation( PortableInstallDialog.this, 
        		 installDir, batchDir, applFs, ini );
         warningLabel.setText(null);

         dispose();
         
      } catch ( Exception e ) {
         e.printStackTrace();
         GUIService.failureMessage( PortableInstallDialog.this, "PORTABLE Installation failed!", e );
      } finally {
         fileButton.setEnabled( true );
         getButtonBar().getCancelButton().setEnabled(true);
      }
      return false;
   }

   @Override
   public void cancelButtonPerformed () {
      if ( !isActionRunning() ) {
         dispose();
      } 
   }
}

}
