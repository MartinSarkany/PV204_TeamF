/*
 *  PolicyDialog in org.jpws.front
 *  file: PolicyDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 24.09.2004
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.jpws.front.MessageDialog.MessageType;
import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ActivitySource;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.ButtonBarListener;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.EditorChangeEvent;
import org.jpws.front.util.EditorChangeEventListener;
import org.jpws.front.util.EditorTextField;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.UserOptionDialog;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.PassphraseUtils;
import org.jpws.pwslib.global.Util;

/**
 * Creates a password policy dialog box and handles user interaction with it.
 * The constructed instance does not auto-display. <code>show()</code> or
 * <code>setVisible()</code> must be called separately.
 * 
 * @author Wolfgang Keller
 * @since 0-5-0 subclass of ButtonBarDialog
 */
public class PolicyDialog extends ButtonBarDialog implements ActivitySource
{
   private ObjectChangeListener objectListener = new ObjectChangeListener(this);

   private PolicyDialogPanel dlgPanel;
   private PwsPassphrasePolicy  editedPolicy;

   /** Constructor for a modal Policy dialog.
    * 
    * @param owner Window the ancestor <code>Window</code> component
    * @param policy password policy to be edited
    * @param modus policy validity type (for title only); 0 = global, 1 = record,
    *              2 = generator, 3 = file
    * 
    * @throws java.awt.HeadlessException
    */
   public PolicyDialog( Window owner, PwsPassphrasePolicy policy, int modus ) 
          throws HeadlessException
   {
      super( owner, DialogButtonBar.OK_CANCEL_BUTTON, true );
      init( owner, policy, modus );
   }

   private void init ( Component owner, PwsPassphrasePolicy policy, int modus )
   {
      String ttitle;
      objectListener.addAncestor( owner );
      
      switch ( modus ) {
      case 1: ttitle = "dlg.policy.record"; break;
      case 2: ttitle = "dlg.policy.generator"; break;
      case 3: ttitle = "dlg.policy.file"; break;
      default: ttitle = "dlg.policy.global";
      }
      editedPolicy = (PwsPassphrasePolicy) policy.clone();

      setTitle( ResourceLoader.getDisplay( ttitle ) );
      buildCentrePanel();
      buildButtonPanel();
      pack();
   }
   
   /** Disposes and destroys this dialog. */
   @Override
   public void dispose ()
   {
      super.dispose();
      
      objectListener.removeAncestor( getParent() );
   }
   
   private void buildButtonPanel()
   {
      ButtonBarListener barListener;
      
      barListener = new DefaultButtonBarListener (this)
      {
         @Override
		public boolean okButtonPerformed ()
         {
            objectListener.activity();
            try {
               dlgPanel.transferValues();
            }
            catch ( NumberFormatException e )
            {
               GUIService.infoMessage( PolicyDialog.this, "dlg.badvalue",
                     "msg.badpassinteger" );
               return false;
            }
            
            if ( !editedPolicy.isValid() )
            {
               GUIService.infoMessage( PolicyDialog.this, "dlg.operrejected",
                     "msg.badpasspolicy" ); 
               return false;
            }
            
            dispose();
            return true;
         }
      };
      super.addButtonBarListener( barListener );
   }

   private void buildCentrePanel()
   {
      dlgPanel = new PolicyDialogPanel( editedPolicy, objectListener );
      setDialogPanel( dlgPanel );
   }

   /**
    * Returns the password policy edited during the dialogue.
    * This is not guaranteed to be a valid policy! It is a valid
    * policy only if <code>isOkPressed()</code> returns <b>true</b> 
    * (and the initial policy was also a valid policy).
    * 
    * @return policy 
    */
   public PwsPassphrasePolicy getEditedPolicy() {
      return editedPolicy;
   }

   @Override
   public void addActivityListener ( ActivityListener listener ) {
      objectListener.addActivityListener( listener );
   }

   @Override
   public void removeActivityListener ( ActivityListener listener ) {
      objectListener.removeActivityListener( listener );
   }

//  *********************  INNER CLASSES  *********************   
   
   /** Performs a modal Policy dialog with given parameters on the EDT and 
    * returns the dialog when it is terminated. The given policy is not
    * modified; the edited version must be fetched by <code>getEditedPolicy()
    * </code> method on the returned dialog.
    * 
    * @param owner Component dialog parent component
    * @param listener ActivityListener may be null
    * @param policy PwsPassphrasePolicy to be edited
    * @param modus int policy validity type (for dialog title); 
    *              0 = global, 1 = record, 2 = generator, 3 = file
    * @return PolicyDialog 
    */
   public static PolicyDialog performed ( Component owner,
		   final ActivityListener listener,
		   final PwsPassphrasePolicy policy, 
		   final int modus ) {
   
	   final Window window = GUIService.getWindowForComponent( owner );
	   final PolicyDialog[] result = new PolicyDialog[1];
	   
	   Runnable run = new Runnable() {
		  @Override
		  public void run() {
			 PolicyDialog dlg = new PolicyDialog(window, policy, modus);
			 result[0] = dlg;
			 dlg.addActivityListener(listener);
		     dlg.moveRelatedTo(window);
		     dlg.setVisible(true);
		  }
	   };
	   
	   try {
		  ActionHandler.executeOnEDT_Wait(run);
	   } catch (InterruptedException e) {
		  e.printStackTrace();
	   } catch (InvocationTargetException e) {
		  e.printStackTrace();
	   }
	   
	   return result[0];
   }

/**
    * A <code>JPanel</code> that holds the initialised dialog components
    * for the edition of a <code>PwsPassphrasePolicy</code>. Edit results are
    * re-transferable to the original policy object by calling  method 
    * <code>transferValues()</code>. 
    * 
    * @since 0-5-0
    */
   public static class PolicyDialogPanel extends JPanel 
                 implements ActionListener, EditorChangeEventListener

   {
      private final int ICONTEXTGAP = 12; 
      private final Color OWN_SYMBOLS_BGDCOLOR = new Color(0x9CC6E7); 
      
      private PwsPassphrasePolicy policy;
      private ObjectChangeListener objectListener;
      
      private JTextField   lengthFld;
      private JCheckBox    lowerCaseChk;
      private JCheckBox    upperCaseChk;
      private JCheckBox    digitsChk;
      private JCheckBox    symbolsChk;
      private JCheckBox    easyReadChk;
      private JCheckBox    hexadecimalChk;
      private EditorTextField symbolsFld;
      private JButton      symResetButton; 
      private JPanel       symbolsPanel;
      private boolean      ownSymbols;
      
   /**
    * Creates the dialog panel with the parameter policy object.
    * The given policy is modified directly when method <code>transferValues()
    * </code> is called, otherwise it remains unchanged.
    * 
    *  @param policy <code>PwsPassphrasePolicy</code> object to be edited 
    *  @param listener <code>ObjectChangeListener</code> may be null
    */
   public PolicyDialogPanel ( PwsPassphrasePolicy policy, ObjectChangeListener listener )
   {
      super();
      if ( policy == null )
    	  throw new NullPointerException("policy");
      
      setLayout( new VerticalFlowLayout( 10 ) );
      setBorder( new EmptyBorder( 16, 25, 5, 25 ) );
   
      this.policy = policy;
      if ( listener != null ) {
    	  this.objectListener = listener;
      }
      init();
   }
   
   private void init ()
   {
      JPanel lengthPanel, p1;
      Dimension p1Size;
      JLabel label;
      
      // create password length input panel
      lengthPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
      label = new JLabel( ResourceLoader.getDisplay("chk.policy.lengthfield") );
      label.setBorder( new EmptyBorder( 0, 0, 0, 12 ) );
      label.setFont( DisplayManager.getFont( "control" ) );
      lengthPanel.add( label );
      
      lengthFld = new JTextField( 2 );
      lengthFld.setText( String.valueOf( policy.length ) );
      lengthPanel.add( lengthFld );
      
      add( lengthPanel );
      
      // create check boxes for character types to be used
      lowerCaseChk = new JCheckBox( ResourceLoader.getDisplay( "chk.policy.lowercase" ),
            policy.lowercaseChars );
      lowerCaseChk.setIconTextGap( ICONTEXTGAP );
      lowerCaseChk.addActionListener( this );
      upperCaseChk = new JCheckBox( ResourceLoader.getDisplay( "chk.policy.uppercase" ),
            policy.uppercaseChars );
      upperCaseChk.setIconTextGap( ICONTEXTGAP );
      upperCaseChk.addActionListener( this );
      digitsChk = new JCheckBox( ResourceLoader.getDisplay( "chk.policy.digits" ),
            policy.digitChars );
      digitsChk.setIconTextGap( ICONTEXTGAP );
      hexadecimalChk = new JCheckBox( ResourceLoader.getDisplay( "chk.policy.hexadecimal" ),
            policy.hexadecimalChars );
      hexadecimalChk.setIconTextGap( ICONTEXTGAP );
      hexadecimalChk.addActionListener( this );
      symbolsChk = new JCheckBox( ResourceLoader.getDisplay( "chk.policy.symbols" ),
            policy.symbolChars );
      symbolsChk.setIconTextGap( ICONTEXTGAP );
      symbolsChk.addActionListener( this );
      easyReadChk = new JCheckBox( ResourceLoader.getDisplay( "chk.policy.easyread" ), 
            policy.easyview );
      easyReadChk.setIconTextGap( ICONTEXTGAP );
      easyReadChk.addActionListener( this );
      
      symbolsFld = new EditorTextField( 20 );
      symbolsFld.setText( new String( policy.getActiveSymbols() ) );
      symbolsFld.addChangeEventListener( this );
      symbolsPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
      symbolsPanel.add(  Box.createHorizontalStrut( ICONTEXTGAP + 16 ) );
//      label = new JLabel( ResourceLoader.getDisplay("chk.policy.lengthfield") );
//      label.setBorder( new EmptyBorder( 0, 0, 0, 12 ) );
//      label.setFont( DisplayManager.getFont( "control" ) );
//      symbolsPanel.add( label );
      symbolsPanel.add( symbolsFld );
      symbolsPanel.doLayout();
      p1Size = symbolsPanel.getPreferredSize();
      symbolsPanel.setVisible( symbolsChk.isSelected() );

      symResetButton = new JButton( ResourceLoader.getDisplay( "button.reset" ) );
      symResetButton.setActionCommand( "symbols.reset" );
      symResetButton.addActionListener( this );
      symbolsPanel.add( symResetButton );
      
      add( lowerCaseChk );
      add( upperCaseChk );
      add( digitsChk );
      add( symbolsChk );
      add( symbolsPanel );
      add( hexadecimalChk );
      add( easyReadChk );
      
      // add a buffer space for the invisible symbols panel
      if ( !symbolsPanel.isVisible() )
      {
         p1 = new JPanel();
         p1.setPreferredSize( p1Size );
         add( p1 );
      }
      
      setupHexadecimal();
      setSymbolEditColor();
      
      // register editable components to object change listener
      if (objectListener != null) {
          objectListener.registerChangeableObject( lengthFld );
          objectListener.registerChangeableObject( lowerCaseChk );
          objectListener.registerChangeableObject( upperCaseChk );
          objectListener.registerChangeableObject( digitsChk );
          objectListener.registerChangeableObject( hexadecimalChk );
          objectListener.registerChangeableObject( symbolsChk );
          objectListener.registerChangeableObject( easyReadChk );
          objectListener.registerChangeableObject( symbolsFld );
          objectListener.registerChangeableObject( symResetButton );
      }
   }
   
   public void transferValues () throws NumberFormatException
   {
      int i = Integer.parseInt( lengthFld.getText().trim() );
      
      policy.length = i;
      policy.lowercaseChars = lowerCaseChk.isSelected();
      policy.uppercaseChars = upperCaseChk.isSelected();
      policy.digitChars = digitsChk.isSelected();
      policy.symbolChars = symbolsChk.isSelected();
      policy.hexadecimalChars = hexadecimalChk.isSelected();
      policy.easyview = easyReadChk.isSelected();
      policy.setOwnSymbols( symbolsFld.getText().toCharArray() );
   }


   private void setupHexadecimal ()
   {
      if ( hexadecimalChk.isSelected() )
      {
         symbolsFld.setEnabled( false );
         symResetButton.setEnabled( false );
         digitsChk.setEnabled( false );
         symbolsChk.setEnabled( false );
         easyReadChk.setEnabled( false );
         if ( lowerCaseChk.isSelected() == upperCaseChk.isSelected() )
         {
            lowerCaseChk.setSelected( true );
            upperCaseChk.setSelected( false );
         }
      }
      else
      {
         symbolsFld.setEnabled( true );
         symResetButton.setEnabled( ownSymbols );
         digitsChk.setEnabled( true );
         symbolsChk.setEnabled( true );
         easyReadChk.setEnabled( true );
      }
   }

   /** Resets the value of the "Own Symbols" edit field to 
    * a default value. */
   private void setStandardSymbols ()
   {
      char[] symbols = easyReadChk.isSelected() ? 
            PassphraseUtils.EASYVISION_SYMBOL_CHARS : PassphraseUtils.SYMBOL_CHARS;
      symbolsFld.setText( new String( symbols ) );
      setSymbolEditColor();
   }
   
   /** Sets boolean variable "ownSymbols" (detects whether there are Own Symbols defined
    * for this policy) and the editor field's background color (according to ownSymbols).
    */
   private void setSymbolEditColor ()
   {
      char[] symbols = Util.clearedSymbolSet( symbolsFld.getText().toCharArray() );
      boolean easyChecked = easyReadChk.isSelected();
      ownSymbols = !(Util.equalArrays( symbols, PassphraseUtils.SYMBOL_CHARS ) & !easyChecked ||
            Util.equalArrays( symbols, PassphraseUtils.EASYVISION_SYMBOL_CHARS ) & easyChecked);

      Color c = ownSymbols ? OWN_SYMBOLS_BGDCOLOR : Color.WHITE;
      symbolsFld.setBackground( c );
      symResetButton.setEnabled( ownSymbols );
      Log.debug( 10, "(PolicyDialogPanel.setSymbolEditorColor) own symbols := " + ownSymbols );
   }
   
   @Override
   public void actionPerformed ( ActionEvent e )
   {
      Object source = e.getSource();
      
      if ( source == null )
         return;
      
      Log.log( 9, "(PolicyDialog.PolicyDialogPanel.actionPerformed) action from ".concat( source.toString() ));
      
      // visibility of the "Own Symbols" panel
      if ( source == symbolsChk )
      {
         symbolsPanel.setVisible( symbolsChk.isSelected() );
//         PolicyDialog.this.pack();
//         PolicyDialog.this.repaint();
      }
      
      if ( source == symResetButton )
      { 
         setStandardSymbols();
      }
      
      if ( source == easyReadChk )
      {
         if ( !ownSymbols )
            setStandardSymbols();
      }
      
      // ensure some option unavailable for Hexadecimal passwords
      if ( source == hexadecimalChk )
      {
         setupHexadecimal();
      }
      
      // ensure only one digit style set up in Hexadecimal mode
      if ( hexadecimalChk.isSelected() )
      {
         // upper or lower semantics for hexadecimal option
         if ( source == lowerCaseChk && lowerCaseChk.isSelected() )
            upperCaseChk.setSelected( false );
         else if ( source == upperCaseChk && upperCaseChk.isSelected() )
            lowerCaseChk.setSelected( false );
      }
   }

   @Override
   public void documentChanged ( EditorChangeEvent event )
   {
      setSymbolEditColor();
   }
   
   }

}
