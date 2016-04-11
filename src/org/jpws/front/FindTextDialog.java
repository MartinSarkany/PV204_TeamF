/*
 *  FindTextDialog in org.jpws.front
 *  file: FindTextDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 18.08.2005
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jpws.data.Options;
import org.jpws.front.util.AutoComboBox;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.global.Log;

/**
 *  FindTextDialog in org.jpws.front
 */
public class FindTextDialog extends ButtonBarDialog implements ActionListener, ChangeListener
{
   private static int        index = -1;
   private static int        winCounter;

   private JPanel            dlgPanel;
   private JCheckBox         checkCase;
   private JCheckBox         checkWord;
   private JButton           findButton;
   private AutoComboBox      findCombo;
   
/**
 * @throws java.awt.HeadlessException
 */
public FindTextDialog () throws HeadlessException {
   super();
   init();
}

/**
 * @param owner
 * @throws java.awt.HeadlessException
*/
public FindTextDialog ( Dialog owner ) throws HeadlessException {
   super( owner, DialogButtonBar.CLOSE_BUTTON, false );
   init();
}

/**
 * @param owner
 * @throws java.awt.HeadlessException
 */
public FindTextDialog ( Frame owner ) throws HeadlessException {
   super(  owner, DialogButtonBar.CLOSE_BUTTON, false );
   init();
}

/*
public void finalize ()
{
   System.out.println( "-- finalize FindTextDialog" );
}
*/

private void init () {
   JPanel panel;
   JLabel label;
   VerticalFlowLayout vflow;
   Point loc;
   Object values[];
   int h;
   
   // hint for Quicksearch utility
   if ( !Options.isOptionSet( "Hint_Quicksearch_Seen" ) ) {
      GUIService.infoMessage( ResourceLoader.getDisplay( "dlg.hint" ), 
            ResourceLoader.getDisplay( "hint.quicksearch" ) );
      Options.setOption( "Hint_Quicksearch_Seen", true );
   }
   
   setTitle( ResourceLoader.getDisplay( "dlg.findtext" ) );
   setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
   addWindowFocusListener( new WindowHandler() );
   setAutonomous( true );
   
   // dialog panel CENTER
   vflow = new VerticalFlowLayout( 6 );
   vflow.setHorizontalFill( true );
   dlgPanel = new JPanel( vflow );
   dlgPanel.setBorder( BorderFactory.createEmptyBorder( 15, 10, 0, 7 ) );
   
   // combo / text input field
   panel = new JPanel( new BorderLayout( 5, 0 ) );
   label = new JLabel( ResourceLoader.getDisplay( "find.text" ) );
   panel.add( label, BorderLayout.WEST );
   values = Global.getSelectedFile().getRecentFinds().getContent();
   findCombo = new AutoComboBox( values );
   findCombo.setPreferredSize( new Dimension( 150, 20 ) );
   panel.add( findCombo, BorderLayout.CENTER );

   dlgPanel.add( panel );
   
   // check boxes
   h = Options.getIntOption( "findTextOpt" );
   checkCase = new JCheckBox( ResourceLoader.getDisplay( "find.checkCS" ),
         (h & 1) > 0 );
   checkCase.addActionListener( this );
   findCombo.setCaseSensitive( checkCase.isSelected() );
   dlgPanel.add( checkCase );
   checkWord = new JCheckBox( ResourceLoader.getDisplay( "find.checkWD" ),
         (h & 2) > 0 );
   checkWord.addActionListener( this );
   dlgPanel.add( checkWord );
   
   // find button
   findButton = new JButton( ResourceLoader.getDisplay( "button.find" ) );
   findButton.addActionListener( this );
   dlgPanel.add( findButton );
   
   setDialogPanel( dlgPanel );

   getRootPane().setDefaultButton( findButton );
   loc = Global.mainFrame.getLocationOnScreen();
   loc.x -= getWidth();
   if ( winCounter == 1 )
      loc.y += this.getHeight();
   if ( winCounter == 2 )
      loc.y += this.getHeight() * 2;
   if ( winCounter > 2 )
      loc.translate( winCounter * 5, winCounter * 5 );
   Util.setCorrectedLocation( this, loc );
   
   DisplayManager.addChangeListener( this );
   winCounter++;
   setVisible( true );
}

public void dispose () {
   super.dispose();
   
   DisplayManager.removeChangeListener( this );
   findButton.removeActionListener( this );
   winCounter--;
}

public static void resetIndex () {
   index = -1;
}

//  ********  IMPLEMENTATION OF ActionListener INTERFACE  **********

   public void actionPerformed ( ActionEvent e ) {
      PwsFileContainer  container;
      String text, hstr;
      Object source;
      int h;
      boolean hasFound;
      
      source = e.getSource();
      
      // action of FIND BUTTON
      if ( source == findButton ) {
         // container may have disappeared
         if ( (container = Global.getSelectedFile()) == null ) {
            dispose();
            return;
         }
   
         text = (String)findCombo.getSelectedItem();
         if ( text.equals("") ) return;
         
         hasFound = index > -1;
         index = container.findMatching( index, text, checkCase.isSelected(),
               checkWord.isSelected() );
         if ( index > -1 ) {
            container.pushRecentFindValue( text );
            container.getViewHandler().setSelectedIndex( index );
//            container.getViewHandler().scrollToVisible( index );
            hstr = ResourceLoader.getDisplay( "msg.confirm.textfind" ) + " : " +
                   container.getRecordAt( index ).getTitle();
         } else {
            hstr = ResourceLoader.getDisplay( hasFound ? "msg.endofdocument" : "msg.failtextfind" );
            GUIService.infoMessage( this, null, hstr );
         }
   
         // reload recent values
         reloadValues();
//         Object[] values = Global.getSelectedFile().getRecentFinds().getContent();
//         findCombo.setDataList( values );
         
         // update display
         Global.setStatusText( hstr );
         hstr = index == -1 ? "button.find" : "button.findnext";
         findButton.setText( ResourceLoader.getDisplay( hstr ) );
      }
      
      // action of CHECKBOXES
      if ( source == checkCase | source == checkWord ) {
         findCombo.setCaseSensitive( checkCase.isSelected() );
         // remember current find option settings 
         h = checkCase.isSelected() ? 1 : 0;
         h += checkWord.isSelected() ? 2 : 0;
         Options.setIntOption( "findTextOpt", h );
      }      
   }
   
   /**
    * Reloads the combobox list values from the 
    * active database without modifying
    * the current value of the combo textfield.
    */
   private void reloadValues () {
	   
      PwsFileContainer ct = DisplayManager.getSelectedContainer();
      if ( ct != null ) {
         Log.log( 5, "(FindTextDialog) reloading data list from: ".concat( ct.getDatabaseName() ) );
         Object v = findCombo.getEditor().getItem();
         
         // reload recent values
         Object[] values = ct.getRecentFinds().getContent();
         findCombo.setDataList( values );
         findCombo.setSelectedItem( v );
      }
   }

// ********  IMPLEMENTATION OF ChangeListener INTERFACE  **********
   
   public void stateChanged ( ChangeEvent e ) {
      // this event indicates that the active file has changed in the DisplayManager
      
      reloadValues();
   }
   
//  *********  INNER CLASSES  **********
   private class WindowHandler extends WindowAdapter  {

      public void windowGainedFocus ( WindowEvent e ) {
//         Log.log( 5, "(FindTextDialog) focus gained" );
         reloadValues();
      }
   
   }
}
