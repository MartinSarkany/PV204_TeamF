/*
 *  DatabaseDialog in org.jpws.front
 *  file: DatabaseDialog.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 01.02.2007
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

package org.jpws.front;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoableEdit;

import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket;
import org.jpws.front.util.BlinkingLabel;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.EditorTextField;
import org.jpws.front.util.NotesTextArea;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.SpringUtilities;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.HeaderFieldList;

/**
 * Class to edit file properties such as security level, logical database name
 * and description text. 
 *  
 * since 0-5-0
 */
public class DatabaseDialog extends ButtonBarDialog
{

   /** Possible value of <code>SecurityChoice</code> */
   public static final int SECURE_BASIC = 0;
   /** Possible value of <code>SecurityChoice</code> */
   public static final int SECURE_MEDIUM = 1;
   /** Possible value of <code>SecurityChoice</code> */
   public static final int SECURE_ADVANCED = 2;
   /** Possible value of <code>SecurityChoice</code> */
   public static final int SECURE_TOPSECRET = 3;
   
   private static int TEXTFIELDCOLUMNS = 30; 
   private static int MAX_NAME_LENGTH = 128; 
   private static int MAX_DESCR_LENGTH = 256; 
   
//   private HtmlBrowserDialog helpDialog;
   
   private PwsFileSocket socket;
   private EditorTextField nameFld;
   private NotesTextArea descrArea;
   private JComboBox secureCombo;
   private BlinkingLabel countTextLabel;
   private JLabel loopsValueLabel;

   private boolean isNewFile;

/**
 * Creates a new database dialog with the active main frame
 * as the owner. The dialog is non-modal.
 * 
 * @param socket the file socket of the database
 * @param title dialog title (text or token) 
 * @param newFile whether socket refers to a new database
 * @throws HeadlessException
 */
public DatabaseDialog ( PwsFileSocket socket, String title, boolean newFile ) throws HeadlessException
{
   super( Global.getActiveFrame(), DialogButtonBar.OK_CANCEL_HELP_BUTTON, false );
   init( socket, title, newFile );
}

/**
 * Creates a new database dialog with the given <code>Dialog</code>
 * as the owner. The dialog is non-modal.
 * 
 * @param owner <code>Dialog</code> parent dialog
 * @param socket the file socket of the database
 * @param title dialog title (text or token) 
 * @param newFile whether socket refers to a new database
 * @throws HeadlessException
 */
public DatabaseDialog ( Dialog owner, PwsFileSocket socket, String title, boolean newFile )
      throws HeadlessException
{
   super( owner, DialogButtonBar.OK_CANCEL_HELP_BUTTON, false );
   init( socket, title, newFile );
}

private void init ( PwsFileSocket socket, String title, boolean newFile )
{
   JPanel content, panel, p1, p2;
   JLabel label;
   JScrollPane scroll;
   Font thinLabelFont;
   String choices[];
   
   if ( socket == null | title == null )
      throw new NullPointerException();
   
   isNewFile = newFile;
   moveRelatedTo( Global.mainFrame );
   setTitle( ResourceLoader.codeOrRealDisplay( title ) );
   addButtonBarListener( new BarListener() );
   setBarGap( 8 );
   this.socket = socket;
   
   content = new JPanel( new BorderLayout() );
   panel = new JPanel( new SpringLayout() );
   panel.setBorder( BorderFactory.createEmptyBorder( 20, 25, 0, 30 ) );
   
   // 1. row : database name
   label = new JLabel( ResourceLoader.getDisplay( "label.newdb.database" ) );
   thinLabelFont = label.getFont().deriveFont( Font.PLAIN );
   nameFld = new EditorTextField( TEXTFIELDCOLUMNS );
   ActionHandler.registerChangeableObject( nameFld );
   panel.add( label );
   panel.add( nameFld );

   // 2. row : database description
   label = new JLabel( ResourceLoader.getDisplay( "label.newdb.description" ) );
   descrArea = new NotesTextArea( null, 5, TEXTFIELDCOLUMNS );
   descrArea.setMargin( new Insets( 2, 4, 2, 2 ) );
   descrArea.getDocument().addDocumentListener( new TextListener() );
   ActionHandler.registerChangeableObject( descrArea );
   scroll = new JScrollPane( descrArea );
   panel.add( label );
   panel.add( scroll );

   // 3. row : security level choice
   label = new JLabel( ResourceLoader.getDisplay( "label.newdb.securitylevel" ) );
   panel.add( label );
   choices = new String[] { 
         ResourceLoader.getDisplay( "choice.newdb.basic" ), 
         ResourceLoader.getDisplay( "choice.newdb.medium" ), 
         ResourceLoader.getDisplay( "choice.newdb.advanced" ), 
         ResourceLoader.getDisplay( "choice.newdb.topsecret" ) 
         };

   p1 = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
   p2 = new JPanel( new BorderLayout() );
   secureCombo = new JComboBox( choices );
   ActionHandler.registerChangeableObject( secureCombo );
   secureCombo.setFont( secureCombo.getFont().deriveFont( (float)14 ) );
   secureCombo.addActionListener( new ComboListener() );
//   secureCombo.setBorder( BorderFactory.createEmptyBorder( 2, 5, 2, 2 ) );
   p1.add( secureCombo );
   p1.add( Box.createHorizontalStrut( 10 ) );
   loopsValueLabel = new JLabel();
   loopsValueLabel.setFont( thinLabelFont );
   p1.add( loopsValueLabel );
   p2.add( p1, BorderLayout.CENTER );
   
   countTextLabel = new BlinkingLabel();
   countTextLabel.setFont( thinLabelFont );
   Global.addTimePulseListener( countTextLabel );
   p2.add( countTextLabel, BorderLayout.EAST );
   
   panel.add( p2 );

   // setup data
   setTextLengthValue( 0 );
   secureCombo.setSelectedIndex( choiceOfLoops( socket.getSecurityLoops() ));
   nameFld.setText( socket.getHeaderValue( PwsFileSocket.HEADERFIELD_DBNAME ) );
   descrArea.setText( socket.getHeaderValue( PwsFileSocket.HEADERFIELD_DBDESCRIPT ) );
   
   SpringUtilities.makeCompactGrid( panel, 3, 2, 0, 0, 10, 8 );
   content.add( panel );

   // finish
   setDialogPanel( panel );
}

public void show ()
{
   super.show();
   if ( isNewFile )
      Global.setDialogActive( "DatabaseDialog.NewFile", true );
}

public void dispose ()
{
   super.dispose();
   Global.removeTimePulseListener( countTextLabel );
   if ( isNewFile )
      Global.setDialogActive( "DatabaseDialog.NewFile", false );
}

/** Name of database as edited by the user. Max. 128 char. */
public String getDatabaseName ()
{
   String hstr;
   
   hstr = nameFld.getText();
   if ( hstr.length() > MAX_NAME_LENGTH )
      hstr = hstr.substring( 0, MAX_NAME_LENGTH );
   return hstr;
}

/** Description of database as edited by the user. Max. 256 char. */
public String getDescription ()
{
   String hstr;
   
   hstr = descrArea.getText();
   if ( hstr.length() > MAX_DESCR_LENGTH )
      hstr = hstr.substring( 0, MAX_DESCR_LENGTH );
   return hstr;
}

/** Database security level as chosen by the user. Ranges
 * within the "SECURE_" constants of this class.
 */
public int getSecurityChoice ()
{
   return Math.max( secureCombo.getSelectedIndex(), 0 );
}

private void setLoopsValue ( int v )
{
   String hstr; 
   
   hstr = ResourceLoader.getDisplay( "label.newdb.loopsvalue" );
   hstr = Util.substituteText( hstr, "$value", Util.dottedNumber( v ) );
   loopsValueLabel.setText( hstr );
}

private void setTextLengthValue ( int v )
{
   boolean exceeding;
   
   exceeding = v > MAX_DESCR_LENGTH;
   countTextLabel.setText( String.valueOf( v ) );
   countTextLabel.setBlinking( exceeding );
   countTextLabel.setForeground( exceeding ? Color.RED : Color.BLACK );
}

/** The actual amount of security loops corresponding to a security level choice. */
public static int loopsOfChoice ( int choice )
{
   int loops, basic;
   
   basic = Global.BASIC_SECURITY_LOOPS;
   
   switch ( choice )
   {
   case SECURE_MEDIUM:  loops = basic * 10;
        break;
   case SECURE_ADVANCED:  loops = basic * 100;
        break;
   case SECURE_TOPSECRET:  loops = basic * 1000;
        break;
   default: loops = basic;     
   }
   return loops;
}

/** The actual amount of security loops corresponding to a security level choice. */
public static int choiceOfLoops ( int loops )
{
   int c, basic;
   
   basic = Global.BASIC_SECURITY_LOOPS;
   
   if ( loops <= basic )
      c = SECURE_BASIC;
   else if ( loops <= basic * 10 )
      c = SECURE_MEDIUM;
   else if ( loops <= basic * 100 )
      c = SECURE_ADVANCED;
   else
      c = SECURE_TOPSECRET;
   
   return c;
}

private class TextListener implements DocumentListener
{
   public void changedUpdate ( DocumentEvent e )
   {
   }

   public void insertUpdate ( DocumentEvent e )
   {
      setTextLengthValue( e.getDocument().getLength() );
   }

   public void removeUpdate ( DocumentEvent e )
   {
      setTextLengthValue( e.getDocument().getLength() );
   }
}

private class ComboListener implements ActionListener
{
   public void actionPerformed ( ActionEvent e )
   {
      if ( e.getSource() == secureCombo )
      {
         setLoopsValue( loopsOfChoice( secureCombo.getSelectedIndex() ) );
      }
   }
}

public class BarListener extends DefaultButtonBarListener
{
   public boolean okButtonPerformed ()
   {
      HeaderFieldList oldHeader;
      UndoableEdit edit;
      String hstr, field;
      int len, choice, loops, oldLoops;
      
      oldHeader = socket.getHeaderFields();
      oldLoops = socket.getSecurityLoops();
      
      // check for exceeding message
      hstr = null;
      if ( (len = nameFld.getText().length()) > MAX_NAME_LENGTH )
      {
         field = ResourceLoader.getDisplay( "label.newdb.database" );
         hstr = ResourceLoader.getDisplay( "msg.exceedingvalue.cut" );
         hstr = Util.substituteText( hstr, "$field", field );
         hstr = Util.substituteText( hstr, "$max", String.valueOf( MAX_NAME_LENGTH ) );
         hstr = Util.substituteText( hstr, "$delta", String.valueOf( len - MAX_NAME_LENGTH ) );
      }
      else if ( (len = descrArea.getDocument().getLength()) > MAX_DESCR_LENGTH )
      {
         field = ResourceLoader.getDisplay( "label.newdb.description" );
         hstr = ResourceLoader.getDisplay( "msg.exceedingvalue.cut" );
         hstr = Util.substituteText( hstr, "$field", field );
         hstr = Util.substituteText( hstr, "$max", String.valueOf( MAX_DESCR_LENGTH ) );
         hstr = Util.substituteText( hstr, "$delta", String.valueOf( len - MAX_DESCR_LENGTH ) );
      }
      
      if ( hstr == null || GUIService.userConfirm( DatabaseDialog.this, hstr ) )
      {
         // write specification data
         socket.setHeaderField( PwsFileSocket.HEADERFIELD_DBNAME, getDatabaseName() );   
         socket.setHeaderField( PwsFileSocket.HEADERFIELD_DBDESCRIPT, getDescription() );
         choice = getSecurityChoice();
         loops = loopsOfChoice( choice );
         socket.setSecurityLoops( loops );
         Options.setIntOption( "newFileSecurity", loops );

         if ( socket instanceof PwsFileContainer )
         {
            // create undoable edit event (must do after change has happened)
            edit = new UndoManager.HeaderEdit( socket, oldHeader, oldLoops );
            ((PwsFileContainer)socket).fireEditEvent( edit );
         }
         
         dispose();
         return true;
      }
      return false;
   }
   
   public void cancelButtonPerformed ()
   {
      dispose();
   }

   public void helpButtonPerformed ()
   {
      GUIService.toggleHelpDialog( DatabaseDialog.this, "dlg.help.database" );
   }
}
}
