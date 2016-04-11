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
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jpws.data.Exchange;
import org.jpws.data.IOManager;
import org.jpws.data.Options;
import org.jpws.data.PersistentOptions;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.OutputVector;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.exception.DuplicateEntryException;
import org.jpws.pwslib.order.DefaultRecordWrapper;
import org.jpws.pwslib.persist.ByteArrayOutputStreamPws;

/**
 * Dialog to perform CSV export, either for entire file or
 * a record selection.
 * 
 * since 0-5-0 subclass of ButtonBarDialog
 */
public class ExportCSVDialog extends ButtonBarDialog 
                          implements ActionListener
{
   private PwsFileContainer  container;
   private PwsFile           recordList;
   private PersistentOptions options;

//   private JComboBox         sourceFileCombo;
   private JComboBox   	     formatCombo;
   private JComboBox         charsetCombo;
   private JButton           fileButton;
   private JLabel            filepathLabel;  
   
   private File              outFile;
   private boolean           isFileExport;
   private boolean           isDesktop;
   
/**
 * Constructor used for exporting the entire file content.
 * 
 * @param file PwsFileContainer
 * since 0-5-0
 */
public ExportCSVDialog (  PwsFileContainer file )
{
   super( Global.mainFrame, DialogButtonBar.OK_CANCEL_HELP_BUTTON, true );

   if ( file == null )
      throw new NullPointerException();

   isFileExport = true;
   container = file;
   recordList = container.getPwsFile();

   init();
}  // constructor

/**
 * Constructor used for exporting a selection of records.
 * 
 * @param file PwsFileContainer
 * @param recs DefaultRecordWrapper[] marking the set of records to be exported
 * since 0-5-0
 */
public ExportCSVDialog ( PwsFileContainer file, DefaultRecordWrapper[] recs )
{
   super( Global.mainFrame, DialogButtonBar.OK_CANCEL_HELP_BUTTON, true );

   if ( recs == null | file == null )
      throw new NullPointerException();
   
   isFileExport = false;
   container = file;
   
   // create a new record list containing the set of selected records 
   try {
	  recordList = new PwsFile( recs );
      recordList.setFilePath( file.getFilePath() );
      recordList.setPassphrase( file.getPassphrase() );
   } catch (DuplicateEntryException e) {
   }

   init();
}  // constructor

/*
public void finalize ()
{
   System.out.println( "-- finalize ExportCSVDialog" );
}
*/

private void setContainer ( PwsFileContainer ct )
{
   if ( isFileExport ) {
      container = ct;
      recordList = container.getPwsFile();
   }
   
   // define default output file
   String hstr = container.getFileName();
   if ( (hstr).equals( "?" ) ) {
	  // render logical DB name if file has not been saved yet 
      hstr = container.getDatabaseName();
   }
   outFile = new File( Global.exchangeDir, hstr + 
         (isFileExport ? ".export.csv" : ".exportsel.csv") );

   // display update
   filepathLabel.setText( outFile.getPath() );
   filepathLabel.setToolTipText( outFile.getAbsolutePath() );
}

private void init ()
{
   JPanel centerPanel, leftPanel, leftP0, leftP1;
   JLabel label;
   GridLayout grid;
   String hstr;
   int i, rows;
   
   isDesktop = DisplayManager.getDisplayState() == DisplayManager.DISPLAY_DESKTOP;
   options = container.getMinorOptions();

   setTitle( ResourceLoader.getDisplay( "dlg.export.csv" ) );
   setAutonomous( true );
   addButtonBarListener( new BarListener() );
   setBarGap( 2 );

   // dialog center
   centerPanel = new JPanel( new BorderLayout() );
   
   // filepath label
   filepathLabel = new JLabel();
   filepathLabel.setBorder( BorderFactory.createEmptyBorder( 2, 10, 2, 0 ) );
   filepathLabel.setForeground( Color.green );
   filepathLabel.setBackground( Color.gray );
   filepathLabel.setOpaque( true );
   centerPanel.add( filepathLabel, BorderLayout.SOUTH );
   
   // left center
   leftPanel = new JPanel( new BorderLayout( 10, 0 ) );
   leftPanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
   centerPanel.add( leftPanel, BorderLayout.WEST );

   // left-panel label part
   rows = isDesktop ? 5 : 4;
   leftP0 = new JPanel();
   grid =  new GridLayout( rows, 1, 0, 10 );
   leftP0.setLayout( grid );
   leftP1 = new JPanel( new GridLayout( rows, 1, 0, 10 ) );
   leftPanel.add( leftP0, BorderLayout.WEST );
   leftPanel.add( leftP1, BorderLayout.CENTER );
   
   // in case of multifile DESKTOP, add SOURCE FILE info or chooser combo
   if ( isDesktop )
   {
      // naming of file
      label = new JLabel( ResourceLoader.getDisplay( "label.sourcefile" ) );
      leftP0.add( label );
//      if ( !isFileExport ) {
     // information on source file
     hstr = container.getDatabaseName();
     label = new JLabel( hstr );
     label.setToolTipText( container.getFilePath() );
     leftP1.add( label );

//      } else {
//         // chooser combo if ENTIRE FILE export
//         sourceFileCombo = GUIService.getDesktopContainerCombo();
//         sourceFileCombo.addActionListener( this );
//         leftP1.add( sourceFileCombo );
//      }
   }
   
   // indicator to source scope
   label = new JLabel( ResourceLoader.getDisplay( "label.scope" ) );
   leftP0.add( label );
   hstr = isFileExport ? ResourceLoader.getDisplay( "label.entirefile" ) : 
      ResourceLoader.getDisplay( "label.recordselection" ) + " (" + 
      recordList.getRecordCount() + ")";
   label = new JLabel( hstr );
   leftP1.add( label );

   // output format chooser combo
   label = new JLabel( ResourceLoader.getDisplay( "label.fileformat" ) );
   leftP0.add( label );
   formatCombo = new JComboBox( new String[] { 
         ResourceLoader.getDisplay( "label.database" ), 
         ResourceLoader.getDisplay( "label.spreadsheet" ) } );
   i = options.getIntOption( "export.format" );
   if ( i < 0 | i > 1 ) {
      i = 0;
   }
   formatCombo.setSelectedIndex( i );
   formatCombo.addActionListener( this );
   leftP1.add( formatCombo );

   // charset chooser combo
   label = new JLabel( ResourceLoader.getDisplay( "label.charset" ) );
   leftP0.add( label );

   charsetCombo = GUIService.getListedCharsetsCombo();
   if ( (hstr = options.getOption( "export.charset" )).isEmpty() ) {
      hstr = Global.getDefaultCharset();
   }
   charsetCombo.setSelectedItem( Charset.forName( hstr ) );
   charsetCombo.addActionListener( this );
   leftP1.add( charsetCombo );
   
   // file chooser button
   label = new JLabel( ResourceLoader.getDisplay( "label.outputfile" ) );
   leftP0.add( label );
   
   fileButton = new JButton( ResourceLoader.getDisplay( "button.browse" ) );
   fileButton.addActionListener( this );
   leftP1.add( fileButton );

   setContainer( container );
   setDialogPanel( centerPanel );
}  // init

@Override
public void dispose ()
{
   super.dispose();
   recordList = null;
   fileButton.removeActionListener( this );
   container = null;
}

private void disableControls ()
{
   charsetCombo.setEnabled( false );
   formatCombo.setEnabled( false );
   fileButton.setEnabled( false );
//   if ( isDesktop & isFileExport )
//      sourceFileCombo.setEnabled( false );
}

private void enableControls ()
{
   charsetCombo.setEnabled( true );
   formatCombo.setEnabled( true );
   fileButton.setEnabled( true );
//   if ( isDesktop & isFileExport )
//      sourceFileCombo.setEnabled( true );
}

/* 
 * Overridden: @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
 */
@Override
public void actionPerformed ( ActionEvent e )
{
   if ( e.getSource() == fileButton ) {
      JFileChooser   fc;
      File file;

      // start file open dialog on FILE BUTTON pressed
      fc = new FileOpenDialog(  FileOpenDialog.CSV_FILTER  );
      fc.setDialogTitle( ResourceLoader.getDisplay( "dlg.export.csv.outputfile" ) );
      fc.setSelectedFile( outFile );

      if ( fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION &&
           (file = fc.getSelectedFile()) != null )
      {
         outFile = file;
         filepathLabel.setText( file.getPath() );
         filepathLabel.setToolTipText( file.getAbsolutePath() );
         pack();
      }
      Global.exchangeDir = fc.getCurrentDirectory();
   }

   // memorise combo settings
   boolean storeVolatiles = !Options.isOptionSet("storeMinorChanges") || container.isModified();
   if ( e.getSource() == formatCombo & storeVolatiles ) {
      options.setIntOption( "export.format", formatCombo.getSelectedIndex() );
   }

   else if ( e.getSource() == charsetCombo & storeVolatiles ) {
	   Charset charset = (Charset)charsetCombo.getSelectedItem(); 
       String name = charset == null ? Global.getDefaultCharset() : charset.name(); 
       options.setOption( "export.charset", name );
   }

//   // reset values to new SOURCE FILE selection
//   if ( e.getSource() == sourceFileCombo )
//   {
//      PwsFileContainer ct;
//      
//      ct = (PwsFileContainer)sourceFileCombo.getSelectedItem();
//      if ( ct != null )
//      {
//         setContainer( ct );
//         pack();
//      }
//   }
}  // actionPerformed

//  ****************  IMPLEMENTATION OF ButtonBarListener  **********************

private class BarListener extends DefaultButtonBarListener
{
   public BarListener ()
   {
      super( ExportCSVDialog.this, "dlg.help.csvexport" );
   }

   @Override
public void cancelButtonPerformed ()
   {
// just used for testing purpose      
//      String hstr = ResourceLoader.getDisplay( "msg.export.error" );
//      GUIService.failureMessage( hstr, null );

      synchronized ( recordList )
      {
         dispose();
      }
   }
   
   @Override
public boolean okButtonPerformed ()
   {
      OutputStream out;
      Exchange.FieldSet fields;
      ByteArrayOutputStreamPws byteOut;
      OutputVector outVector;
      Charset charset;
      String hstr;
      int modus;
      boolean ok;
      
      out = null;
      synchronized ( recordList )
      {
      try {
         disableControls();
         
         // let user confirm overwrite if file exists
         if ( outFile.exists() ) {
            if ( !GUIService.overwriteConfirm( ExportCSVDialog.this, outFile ) )
               return false;
   
            // securely erase content of existing file
            Util.wipeAFile( outFile );
         }
         
         // create file and related output objects
         out = IOManager.makeLocalContextFile( outFile ).getOutputStream();
         byteOut = new ByteArrayOutputStreamPws();
         outVector = new OutputVector();
         outVector.addStream( out );
         outVector.addStream( byteOut );
         
         // determine file format parameters
         if ( formatCombo.getSelectedIndex() == 0 ) {
            // user option: DATABASE format
            fields = Exchange.allFields();
            modus = 0;

         } else {
            // user option: SPREADSHEET format
            fields = Exchange.essentialFields();
            modus = 1;
         }
         
         if ( (charset = (Charset)charsetCombo.getSelectedItem()) == null ) {
            charset = Charset.forName( Global.getDefaultCharset() );
         }
   //System.out.println( "- output charset: " + charset );
   
         // operate
         ok = Service.csvExportRecords( outVector, recordList, fields, 
               null, charset, modus );
         outVector.close();
//         options.setOption( "export.charset", charset.name() );
//         options.setIntOption( "export.format", formatCombo.getSelectedIndex() );
   
         if ( ok ) {
            // report success
            hstr = ResourceLoader.getDisplay( "msg.confirm.export" );
            hstr = Util.substituteText( hstr, "$file", outFile.getAbsolutePath() );
            GUIService.infoMessage( ExportCSVDialog.this, null, hstr );
            dispose();
            return true;
         }

      } catch ( Exception e ) {
         try { 
            if ( out != null ) { 
               out.close(); 
            }
         } catch ( IOException e1 ) {
         }
         
         e.printStackTrace();
         hstr = ResourceLoader.getDisplay( "msg.export.error" );
         GUIService.failureMessage( hstr, e );

      } finally {
         enableControls();
      }
      return false;
      }
   }  // okButtonPerformed
}
}
