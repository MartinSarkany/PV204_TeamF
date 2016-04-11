/*
 *  FileInfoDialog in org.jpws.front
 *  file: FileInfoDialog.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 26.10.2006
 *  Version
 * 
 *  Copyright (c) 2006 by Wolfgang Keller, Munich, Germany
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Point;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.SpringUtilities;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.PwsFileHeaderV3;

/**
 * Dialog panel to inform about various PWS file states and statistics.
 * 
 * @since 0-4-0
 * @since 0-5-0 subclass of ButtonBarDialog
 */
public class FileInfoDialog extends ButtonBarDialog implements ChangeListener
{
   private static final Font FIELDFONT = new JTextField().getFont();
   private PwsFileSocket socket;
   private BarListener barListener;
   private JButton clearButton;
   private JButton editButton;
   
   private JPanel contentPanel;
   private JLabel nameLabel;
   private JLabel descriptLabel;
   private JLabel filenameLabel;
   private JLabel locationLabel;
   private JLabel securityLabel;
   private JLabel uuidLabel;
   private JLabel formatLabel;
   private JLabel recsLabel;
   private JLabel sizeLabel;
   private JLabel saveTimeLabel;
   private JLabel lastUserLabel;
   private JLabel lastApplLabel;
   private JLabel ukfLabel;
   private JLabel ukfSizeLabel;
   
   private String loopsText;
   
   
public FileInfoDialog ( PwsFileSocket file ) throws HeadlessException
{
   super( Global.getActiveFrame(), DialogButtonBar.CLOSE_BUTTON, false );

   if ( file == null )
      throw new NullPointerException();
   
   init( file );
}

/** String value of header field DATABASE_NAME or <b>null</b> if does not exist.
 * @since 0-5-0
 *  */  
private String databaseName()
{
   return socket.getHeaderValue( PwsFileSocket.HEADERFIELD_DBNAME );
}

/** String value of header field DATABASE_DESCRIPTION or <b>null</b> if does not exist. 
 * @since 0-5-0
 * */  
private String databaseDescription()
{
   return socket.getHeaderValue( PwsFileSocket.HEADERFIELD_DBDESCRIPT );
}

private void init ( PwsFileSocket file )
{
   Frame frame;
   Point loc;
   
   frame = Global.getActiveFrame();
   setTitle( ResourceLoader.getDisplay( "dlg.fileinfo" ) );
   setResizable( true );
   setClipping( false );
   setAutonomous( true );
   moveRelatedTo( frame );
   setCloseByEscape( true );
   socket = file;
   loopsText = ResourceLoader.getDisplay( "label.loops" );
   
   // button bar SOUTH
   clearButton = new JButton( ResourceLoader.getDisplay( "button.clear.ukf" ) );
   clearButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.delete_ukf" ));
   getButtonBar().add( clearButton, 0 );
   editButton = new JButton( ResourceLoader.getDisplay( "button.modify" ) );
   editButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.infomodify" ));
   if ( socket.getFileFormat() >= Global.FILEVERSION_3 )
      getButtonBar().add( editButton, 0 );
   barListener = new BarListener();
   addButtonBarListener( barListener );

   // content panel CENTER
   buildCenterPanel();
   setupContent();
   setDialogPanel( contentPanel );

   loc = frame.getLocationOnScreen();
   loc.x += frame.getWidth();
   setCorrectedLocation( loc );
//   Util.setCorrectedLocation( this, loc );
//   setBounds( Util.correctedWindowBounds( this.getBounds(), true, false ) );

   setVisible( true );
   
   socket.addChangeListener( this );

/*   
   // TEST: printout of file record order (hashmap sorting)
   Iterator it;
   int i;
   System.out.println( "*****  FILE " + socket.getFileName() + " PRINTOUT OF PHYSICAL RECORD ORDER  ****" );
   for ( it = socket.getPwsFile().iterator(), i = 0; it.hasNext(); i++ )
   {
      System.out.println( "  " + ((PwsRecord)it.next()).getRecordID() );
   }
   System.out.println( "*****  # PRINTOUT, records = " + i + " of " + socket.getRecordCount() );
*/   
}

public void dispose ()
{
   super.dispose();
   
   if ( socket != null )
   {
      socket.removeChangeListener( this );
      if ( socket instanceof PwsFileContainer )
         ((PwsFileContainer)socket).fileInfoDlg = null;
   }
}

private void buildCenterPanel ()
{
   JPanel panel;
   int rows;

   if ( contentPanel == null )
   {
      contentPanel = new JPanel( new SpringLayout() );
      contentPanel.setBorder( BorderFactory.createEmptyBorder( 6, 8, 0, 8 ) );
   }
   else
      contentPanel.removeAll();
   
   panel = contentPanel;
   rows = 10; 
   
   // database name
   nameLabel = null;
   if ( databaseName() != null )
   {
      nameLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.dbname" ) );
      rows++;
   }
   
   // database description
   descriptLabel = null;
   if ( databaseDescription() != null )
   {
      descriptLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.dbdescript" ) );
      rows++;
   }
   
   // file name 
   filenameLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.filename" ) );
   
   // file location
   locationLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.location" ) );
   
   // file format
   saveTimeLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.savetime" ) );

   // file format
   formatLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.format" ) );

   // file uuid
   uuidLabel = null;
   if ( socket.getFileFormat() >= Global.FILEVERSION_3 )
   {
      uuidLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.uuid" ) );
      rows++;
   }

   // most recent user and host
   lastUserLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.lastuser" ) );

   // most recent user and host
   lastApplLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.lastprogram" ) );

   // security loops
   securityLabel = null;
   if ( socket.getFileFormat() >= Global.FILEVERSION_3 )
   {
      securityLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.security" ) );
      rows++;
   }

   // file size
   sizeLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.filesize" ) );
   
   // record count
   recsLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.records" ) );
   
   // unknown field count
   ukfLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.ukf" ) );
   
   // unknown fields size
   ukfSizeLabel = addField( panel, ResourceLoader.getDisplay( "dlg.fileinfo.ukfsize" ) );
   
   SpringUtilities.makeCompactGrid( panel, rows, 2, 0, 0, 10, 4 );
}

private JLabel addField ( JPanel pane, String label )
{
   JLabel lb, fld;
   
   lb = new JLabel( label );
   fld = new JLabel();
   fld.setFont( FIELDFONT );
   pane.add( lb );
   pane.add( fld );
   return fld;
}

/** Field value DATABASE_DESCRIPTION wrapped into adequate HTML encoding.
 * @since 0-5-0
 *  */ 
private String htmlDescription ()
{
   JLabel label;
   String core, result;
   
   core = Util.htmlEncoded( databaseDescription() );
   result = "<html>" + core;
   label = new JLabel( result );
//   System.out.println( "-- descriptLabel Size 1 == " +  label.getPreferredSize() );

   if ( label.getPreferredSize().width > 400 )
   {
      result = "<html><table cellspacing=\"0\" cellpadding=\"0\" width=\"350\"><tr><td>" + core + "</td></tr></table>";
      label.setText( result );
//      System.out.println( "-- descriptLabel Size 2 == " +  label.getPreferredSize() );
   }
   return result;
}

private void setupContent ()
{
   String hstr;
   int ucount;
   long time;
   
   // database name
   if ( nameLabel != null )
      nameLabel.setText( databaseName() );
   
   // database name
   if ( descriptLabel != null )
      descriptLabel.setText( htmlDescription() );
   
   // file name
   hstr = socket.getFileName() + "  (" +
           ResourceLoader.getDisplay( "dlg.fileinfo.filemod" ) + " " + 
           String.valueOf( socket.getFileModNumber() ) + ")"; 
   filenameLabel.setText( hstr );
   
   // file location
   hstr = Util.pathNameOfPath( socket.getFilePath() );
   if ( hstr.length() > 60 )
   {
      locationLabel.setToolTipText( hstr );
      hstr = "..".concat( hstr.substring( hstr.length() - 60 ) );
   }
   else
      locationLabel.setToolTipText( null );
   locationLabel.setText( hstr );
   
   // uuid
   if ( uuidLabel != null )
      uuidLabel.setText( socket.getUUID().toString() );
   
   // last save time
   time = socket.getStoreTime();
   saveTimeLabel.setText( time == 0 ? "?" : Global.getLocalDateTime( time ) );
   
   // file format
   formatLabel.setText( "Password Safe V" + socket.getFullFileFormatText() );
   
   // number of groups + records
   hstr = String.valueOf( socket.getGroupCount() );
   hstr = String.valueOf( socket.getRecordCount() ) + " " + 
          ResourceLoader.getDisplay("label.in") + " " + hstr + " " + 
          ResourceLoader.getDisplay("dlg.fileinfo.groups"); 
   recsLabel.setText( hstr );
   
   // file size (theoretical)
   hstr = Util.dottedNumber( socket.getStoredSize() );
   sizeLabel.setText( String.valueOf( hstr ));
   
   // last user
   hstr = socket.getLastUserText();
   lastUserLabel.setText( hstr );
   
   // last application
   hstr = socket.getLastProgram();
   lastApplLabel.setText( hstr );
   
   // security loops
   if ( securityLabel != null )
      securityLabel.setText( String.valueOf( socket.getSecurityLoops() ) + " " + loopsText );
   
   // unknown fields count
   ucount = socket.getUKFCount();
   ukfLabel.setText( String.valueOf( ucount ));
   
   // unknown fields size
   ukfSizeLabel.setText( String.valueOf( socket.getUKFSize() ));
   
   clearButton.setEnabled( ucount > 0 );
}

/**
 * Whether the display content must be layed out again.
 * @return boolean
 * @since 0-5-0
 */
private boolean mustReformat ()
{
   boolean minorFormat;
   
   minorFormat = socket.getFileFormat() < Global.FILEVERSION_3;
   return ((nameLabel == null) != (databaseName() == null)) ||
          ((descriptLabel == null) != (databaseDescription() == null)) ||
          ((securityLabel == null) != minorFormat) || 
          ((uuidLabel == null) != minorFormat); 
}

public void stateChanged ( ChangeEvent e )
{
   String hstr, timeStr;
   int prop, value;
   
   prop = ((PwsFileSocket.ChangeEvent)e).getState();
   
   if ( prop == PwsFileSocket.MODIFY_EVENT )
   {
//      System.out.println( "- FileInfoDialog: MODIFY event received" );
      if ( mustReformat() )
      {
//         System.out.println( "** reformatting File-Info " + foCount++ ); 
         buildCenterPanel();
         setupContent();
         if ( socket.getFileFormat() >= Global.FILEVERSION_3 )
            getButtonBar().add( editButton, 0 );
         else
            getButtonBar().remove( editButton );
         pack();
      }
      else
         setupContent();
   }

   else if ( prop == PwsFileContainer.DISPLAY_MODE )
   {
      value = ((PwsFileContainer)socket).getViewType();
      if ( value == PwsFileContainer.NO_VIEW )
      {
         // change window appearence
         timeStr = Util.standardTimeString( System.currentTimeMillis() ).substring( 11, 16 );
         hstr = ResourceLoader.getDisplay( "dlg.fileinfo.closed" );
         setTitle( Util.substituteText( hstr, "$time", timeStr ) );
         contentPanel.setBackground( Color.lightGray );
         getButtonBar().setBackground( Color.lightGray );
         clearButton.setEnabled( false );
         editButton.setEnabled( false );
         
         // disband from socket
         socket.removeChangeListener( this );
         socket = null;
      }
   }
}

private class BarListener extends DefaultButtonBarListener
{
   public boolean extraButtonPerformed ( Object button )
   {
      ButtonBarDialog dlg;

      if ( button == clearButton &&
           GUIService.userConfirm( FileInfoDialog.this, 
              ResourceLoader.getDisplay( "dlg.fileinfo.askdelete.ukf" ) ) )
         {
            socket.clearUnknownFields();
            return socket.getUKFCount() > 0;
         }

      else if ( button == editButton )
      {
         dlg = new DatabaseDialog( FileInfoDialog.this, socket, "dlg.database.edit", false )
         {
            public void dispose ()
            {
               // optionally remember window bounds
               if ( Options.isOptionSet( "rememberScreen" ) )
                  storeBounds( Options.getOptions(), "fileinfo_editor", true );
               
               super.dispose();
            }
         };
         
         // optionally restore window bounds from file option memory
         if ( Options.isOptionSet( "rememberScreen" ) )
            dlg.gainBounds( Options.getOptions(), "fileinfo_editor", true );
         
         dlg.setModal( true );
         dlg.show();
      }
      
      return true;
   }

   public boolean okButtonPerformed ()
   {
      dispose();
      return true;
   }
   
   
}

}
