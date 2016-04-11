/*
 *  ExportDialog in org.jpws.front
 *  file: ExportDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 12.12.2005
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.nio.charset.Charset;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.undo.UndoableEdit;

import org.jpws.data.IOManager;
import org.jpws.data.Options;
import org.jpws.data.PersistentOptions;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ReporterWindow;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.Util.BufferInt;
import org.jpws.pwslib.data.PwsRecordList;
import org.jpws.pwslib.order.DefaultRecordWrapper;

/**
 * Dialog to setup and perform a CSV import into a parameter open database 
 * (<code>PwsFileContainer</code>) or a file that is created new.
 */
public class ImportCSVDialog extends ButtonBarDialog 
                          implements ActionListener
{
   private PwsFileContainer  container;
   /** Ordered list of already used record group names */
   private List<String>      groups;
   private PersistentOptions fileOptions;
   private Object            lock = new Object();
   private DefaultRecordWrapper[] importedRecords;

   private JComboBox           formatCombo;
   private JComboBox  		   charsetCombo;
   private JComboBox           groupCombo;
   private JButton             fileButton;
   private JLabel              filepathLabel;  
   private JPanel              controlPanel;
   
   private String            rootChoice;
   private ReporterWindow    reporter;
   private PrintWriter       errorLog;
   private File              inFile;
   /** Whether this dialog is not related to a given file container */
   private boolean           isNewFile;
   /** Whether the multi-file desktop is active in display */
   private boolean           isDesktop;
   

public ImportCSVDialog ( PwsFileContainer ct )
{
   super( Global.mainFrame, DialogButtonBar.OK_CANCEL_HELP_BUTTON, ct != null );

   container = ct;
   isNewFile = ct == null;

   init();
}  // constructor


//public void finalize ()
//{
//   System.out.println( "-- finalize ImportCSVDialog" );
//}


private void init ()
{
   JPanel centerPanel, leftPanel, leftP0;
   JLabel label;
   GridLayout grid;
   File f;
   String hstr;
   int i, rows;
   
   isDesktop = DisplayManager.getDisplayState() == DisplayManager.DISPLAY_DESKTOP;
   fileOptions = isNewFile ? Options.getOptions() : container.getMinorOptions();
   groups = isNewFile ? null : container.getGroupList(); 
   
   // look for a previous source file definition
   String path = fileOptions.getOption( "importfile.recent");
   if ( !path.isEmpty()  && (f = new File(path)).isFile() ) {
      inFile = f;
   }

   // dialog frame
   setTitle( ResourceLoader.getDisplay( "dlg.import.csv" ) );
   addButtonBarListener( new BarListener() );
   setBarGap( 2 );
   setAutonomous( true );
   setSynchronous(false);
   if ( isNewFile ) {
      moveRelatedTo( Global.mainFrame );
   }

   // dialog center
   centerPanel = new JPanel( new BorderLayout() );
   
   // filepath label
   filepathLabel = new JLabel( inFile != null ? inFile.getAbsolutePath() : "" );
   filepathLabel.setToolTipText( filepathLabel.getText() );
   filepathLabel.setBorder( BorderFactory.createEmptyBorder( 2, 10, 2, 0 ) );
   filepathLabel.setForeground( Color.green );
   filepathLabel.setBackground( Color.gray );
   filepathLabel.setOpaque( true );
   centerPanel.add( filepathLabel, BorderLayout.SOUTH );
   
   // left center
   leftPanel = new JPanel( new BorderLayout( 10, 0 ) );
   leftPanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
   centerPanel.add( leftPanel, BorderLayout.WEST );

   // left-panel
   rows = isDesktop & !isNewFile ? 6 : 5;
   leftP0 = new JPanel();
   grid =  new GridLayout( rows, 1, 0, 10 );
   leftP0.setLayout( grid );
   controlPanel = new JPanel( new GridLayout( rows, 1, 0, 10 ) );
   leftPanel.add( leftP0, BorderLayout.WEST );  // labels
   leftPanel.add( controlPanel, BorderLayout.CENTER ); // controls
   
   // in case of multifile DESKTOP, add target file name
   if ( isDesktop & !isNewFile ) {
      // naming of file
      label = new JLabel( ResourceLoader.getDisplay( "label.targetfile" ) );
      leftP0.add( label );
      
      // label for target file
      label = new JLabel( container.getDatabaseName() );
      label.setToolTipText( container.getFilePath() );
      controlPanel.add( label );
   }
   
   // indicator to DESTINATION
   label = new JLabel( ResourceLoader.getDisplay( "label.destination" ) );
   leftP0.add( label );
   label = new JLabel( ResourceLoader.getDisplay( 
           isNewFile ? "label.newfile" : "label.merge" ) );
   controlPanel.add( label );

   // TARGET GROUP selector
   label = new JLabel( ResourceLoader.getDisplay( "label.targetgroup" ) );
   leftP0.add( label );
   groupCombo = GUIService.getGroupListCombo( groups, true, 400 );
   controlPanel.add( groupCombo );
   
   // make "root" choice available
   rootChoice = ResourceLoader.getCommand( "combo.select.root" );
   groupCombo.insertItemAt( rootChoice, 0 );
   
   // read and set latest user choice for GROUP (only in NEW FILE modus)
   hstr = fileOptions.getOption( "import.targetgroup" );
   if ( hstr.isEmpty() || hstr.equals(rootChoice) ) {
	  groupCombo.setSelectedIndex( 0 );
   } else if ( isNewFile ) {
      // for new file dialog make latest user input available as choice
      groupCombo.insertItemAt( hstr, 1 );
      groupCombo.setSelectedIndex( 1 );
   }
   groupCombo.addActionListener( this );

   // output format chooser combo
   label = new JLabel( ResourceLoader.getDisplay( "label.fileformat" ) );
   leftP0.add( label );
   formatCombo = new JComboBox( new String[] { 
         ResourceLoader.getDisplay( "label.database" ), 
         ResourceLoader.getDisplay( "label.spreadsheet" ) } );
   i = fileOptions.getIntOption( "import.format" );
   if ( i < 0 | i > 1 ) {
      i = 0;
   }
   formatCombo.setSelectedIndex( i );
   formatCombo.addActionListener( this );
   controlPanel.add( formatCombo );

   // CHARSET chooser combo
   label = new JLabel( ResourceLoader.getDisplay( "label.charset" ) );
   leftP0.add( label );

   // retrieve recent user choices
   hstr = fileOptions.getOption( "import.charset" );
   if ( hstr.isEmpty() ) {
	  hstr = Options.getOption( "import.charset" );
   }
   if ( hstr.isEmpty() ) {
      hstr = Global.getDefaultCharset();
   }
   charsetCombo = GUIService.getListedCharsetsCombo();
   charsetCombo.setSelectedItem( Charset.forName( hstr ) );
   charsetCombo.addActionListener( this );
   
   controlPanel.add( charsetCombo );
   
   // FILE CHOOSER button
   label = new JLabel( ResourceLoader.getDisplay( "label.inputfile" ) );
   leftP0.add( label );
   
   fileButton = new JButton( ResourceLoader.getDisplay( "button.browse" ) );
   fileButton.addActionListener( this );
   controlPanel.add( fileButton );

   setDialogPanel( centerPanel );
   
   // TEST start reporter window
   hstr = ResourceLoader.getDisplay( "dlg.import.errorlog" );
   reporter = new ReporterWindow( this, hstr );
   errorLog = new PrintWriter( reporter.getWriter() );
}  // init

@Override
public void dispose () {
   super.dispose();
   fileButton.removeActionListener( this );
   if ( isNewFile ) {
      Global.setDialogActive( "ImportCSVDialog.NewFile", false );
   }
}

/** Returns the set of imported records or <b>null</b> if
 * import failed.
 * 
 * @return DefaultRecordWrapper[] or <b>null</b>
 */
public DefaultRecordWrapper[] getImportedRecords () {
   return importedRecords;
}

/**
 * 
 * @param v
 * @since 0-5-0
 */
public void setControlsEnabled ( boolean v ) {
   fileButton.setEnabled( v );
   charsetCombo.setEnabled( v );
   formatCombo.setEnabled( v );
   groupCombo.setEnabled( v );
}

/** 
 * Actions of the dialog GUI, except button-bar actions.
 */
@Override
public void actionPerformed ( ActionEvent e )
{
   // start file chooser for source file 
   if ( e.getSource() == fileButton ) {
      File file;

      // file open dialog
      JFileChooser fc = new FileOpenDialog( FileOpenDialog.CSV_FILTER );
      fc.setDialogTitle( ResourceLoader.getDisplay( "dlg.import.csv.inputfile" ) );
      if ( inFile != null ) {
         fc.setSelectedFile( inFile );
      } else {
         fc.setCurrentDirectory( Global.exchangeDir );
      }

      if ( fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION &&
           (file = fc.getSelectedFile()) != null )
      {
         inFile = file;
         filepathLabel.setText( file.getPath() );
         filepathLabel.setToolTipText( file.getAbsolutePath() );
         fileOptions.setOption("importfile.recent", file.getAbsolutePath());
         Global.exchangeDir = fc.getCurrentDirectory();
         pack();
      }
   }

   // memorise combo settings
   if ( e.getSource() == groupCombo ) {
	  String hstr = groupCombo.getSelectedItem().toString();
	  if ( hstr.equals(rootChoice) ) {
		  hstr = null;
	  }
	  fileOptions.setOption( "import.targetgroup", hstr );
   }
   
   if ( e.getSource() == formatCombo ) {
      fileOptions.setIntOption( "import.format", formatCombo.getSelectedIndex() );
   }

   if ( e.getSource() == charsetCombo ) {
	   Charset charset = (Charset)charsetCombo.getSelectedItem(); 
       String name = charset == null ? Global.getDefaultCharset() : charset.name(); 
       fileOptions.setOption( "import.charset", name );
       Options.setOption( "import.charset", name );
   }

}  // actionPerformed

//  ****************  INNER CLASS ButtonBarListener  **********************

private class BarListener extends DefaultButtonBarListener
{
   public BarListener () {
      super( ImportCSVDialog.this, "dlg.help.csvimport" );
   }

   @Override
   public void cancelButtonPerformed () {
      synchronized ( lock ) {
         dispose();
      }
   }
   
   @Override
   public boolean okButtonPerformed () {
      PwsFileContainer ct;
      PwsRecordList recList, excluded;
      DefaultRecordWrapper[] records=null, movedRecs;
      UndoableEdit edit;
      InputStream in;
      Charset charset;
      String hstr, hstr2, target;
      BufferInt omits = new BufferInt();
      int modus, nrImported;
      boolean ok;
      
      in = null;
      nrImported = 0;
      
      synchronized ( lock ) {
      try {
         // break if input file is not specified
         if ( inFile == null ) {
            GUIService.infoMessage( ImportCSVDialog.this, null, "msg.nofilespecified" );
            return false;
         }
         
         // inform user if file does not exists
         if ( !inFile.exists() ) {
            hstr = ResourceLoader.getDisplay( "msg.filenotfound" );
            hstr = Util.substituteText( hstr, "$path", inFile.getAbsolutePath() );
            GUIService.infoMessage( ImportCSVDialog.this, null, hstr );
            return false;
         }
   
         // get target directory selection (if any)
         target = (String) groupCombo.getSelectedItem();
         if ( target != null && target.equals( rootChoice ) ) {
            target = null;
         }
         
         // open input file
         in = IOManager.makeLocalContextFile( inFile ).getInputStream();
         
         // determine file format parameters
         if ( formatCombo.getSelectedIndex() == 0 ) {
            // user option: DATABASE format
            modus = 0;
         } else {
            // user option: SPREADSHEET format
            modus = 1;
         }
         
         // determine applied character set
         if ( (charset = (Charset)charsetCombo.getSelectedItem()) == null ) {
            charset = Charset.forName( Global.getDefaultCharset() );
         }
//         System.out.println( "- output charset: " + charset );
   
         setControlsEnabled( false );
   
         // operate primary import phase (makes all records of the source available in recList)
         reporter.clearText();
         recList = Service.csvImportRecords( in, charset, modus, omits, errorLog );
         records = recList.toRecordWrappers( null );
         in.close();
         
         // break if import is empty
         if ( recList.size() == 0 ) {
            GUIService.infoMessage( ImportCSVDialog.this, null, ResourceLoader.getDisplay( "msg.import.empty" ) );
            setControlsEnabled( true );
            return false;
         }
         
         // function branch: create new file from imported records
         if ( isNewFile ) {
        	 
            // attempt install new file as container
            ct = DatabaseHandler.newFileToShelf( recList );
            ok = ct != null;
            
            // operate target directory move if opted
            if ( ok & target != null ) {
               ct.moveEntries( records, target, true );
            }
            
            // only if success take over new file values for this dialog
            if ( ok ) {
               container = ct;
               nrImported = ct.getRecordCount();
               
               // create the undoable edit object
               movedRecs = null;
               edit = new UndoManager.ModifyRecordEdit( 
                     UndoManager.ModifyRecordEdit.IMPORT_RECORDS_EDIT,
                     container, null, records, movedRecs, inFile.getName() );
               container.fireEditEvent( edit );
            }
         }
   
         // function branch: merge imported records
         else {
            int old = container.getRecordCount();
            excluded = container.mergeDatabase(ImportCSVDialog.this, recList, 
            		   inFile.getName(), target, 0, false);
            nrImported = container.getRecordCount() - old;
            ok = excluded != null;
            
            // if function was not broken, highlight imported records
            if ( ok ) {
               records = container.getImportedRecords(recList.excludeRecordList(excluded));
            }
         }
         
         setControlsEnabled( true );
         if ( ok & (isNewFile | omits.value > 0) ) {
            // report success
            hstr2 = "";
            if ( omits.value > 0 ) {
               hstr2 =  ResourceLoader.getDisplay( "msg.confirm.import.omitted" );
               hstr2 = Util.substituteText( hstr2, "$omits", String.valueOf( omits.value ));
            }
            
            hstr = ResourceLoader.getDisplay( "msg.confirm.import" );
            hstr = Util.substituteText( hstr, "$file", inFile.getAbsolutePath() );
            hstr = Util.substituteText( hstr, "$recs", String.valueOf( nrImported ));
            hstr = Util.substituteText( hstr, "$omitinfo", hstr2 );
            GUIService.infoMessage( ImportCSVDialog.this, null, hstr );
         }
         
         // select imported records in container
         if ( ok ) {
            importedRecords = records;
            if ( container != null ) {
               container.setSelectedRecords( records );
            }
         }
   
         // terminate dialog display (only if no error occurred)
         if ( omits.value == 0 ) {
            dispose();
         }
         return true;

      } catch ( StreamCorruptedException e ) {
         hstr = ResourceLoader.getDisplay( "msg.import.error.format" );
         hstr = Util.substituteText( hstr, "$exc", e.getMessage() );
         GUIService.infoMessage( ImportCSVDialog.this, "dlg.operfailure", hstr );

      } catch ( Exception e ) {
         e.printStackTrace();
         hstr = ResourceLoader.getDisplay( "msg.import.error" );
         GUIService.failureMessage( hstr, e );

      } finally {
         setControlsEnabled( true );
         try { 
            if ( in != null ) {
               in.close(); 
            }
         } catch ( IOException e1 ) {
         }
      }
      return false;
      }
   }  // okButtonPerformed
}

}
