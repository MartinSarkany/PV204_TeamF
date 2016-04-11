/*
 *  PasswordDialog in org.jpws.front
 *  file: PasswordDialog.java
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

package org.jpws.front;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ActivitySource;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.global.Log;

/**
 * Displays a modal password dialog box and handles user interaction with it.
 * The purpose of this class is to render an input passphrase. It assumes
 * one of 3 possible display and functional modi: ACCESS - for
 * user password to access a database or object, DEFINE - for user definition of
 * a new passphrase, and CONTROL - for any security password checking.
 *    
 * 
 * @author Wolfgang Keller
 * @since 0-5-0 subclass of ButtonBarDialog
 */
@SuppressWarnings("serial")
public class PasswordDialog extends ButtonBarDialog implements ActivitySource
{
   public static final int ACCESS = 0;
   public static final int DEFINE = 1;
   public static final int CONTROL = 2;
   
   private ActivityManager   activMan = new ActivityManager();
   private BarActionListener barListener = new BarActionListener();;
   private ButtonBarDialog   infoBox;
   private JButton           exitButton;
   private JButton           createButton;
   private JCheckBox         qualityChk;
   private JLabel            evalLabel;    
   private JPasswordField	  enteredPassword, confirmPassword;
   private int               opModus;
   private String            info1;

    /**
     * Creates a new PasswordDialog without an exit button.
     * 
     * @param owner  the parent <code>Frame</code>
     * @param filename text displayed emphasised for the input field; may be <b>null</b>
     * @param modus operation modus resulting in different text and title display
     *              ACCESS = input for file access (open)
     *              DEFINE = definition of new passphrase
     *              CONTROL =  security request to enter existing passphrase
     * 
     * @throws java.awt.HeadlessException
     */
    public PasswordDialog( Frame owner, String filename, int modus ) 
         throws HeadlessException {
      this( owner, filename, modus, false );
    }
    
	/** 
	 * Creates a new PasswordDialog (full settings).
	 * 
	 * @param owner  the parent <code>Frame</code>
     * @param filename text displayed emphasised for the input field; may be <b>null</b>
     * @param modus operation modus resulting in different text and title display
     *              ACCESS = input for file access (open)
     *              DEFINE = definition of new passphrase
     *              CONTROL =  security request to enter existing passphrase
     * @param exitActive if <b>true</b> the dialog shows a program exit button            
	 * @throws java.awt.HeadlessException
	 */
	public PasswordDialog( Frame owner, String filename, int modus, boolean exitActive ) 
          throws HeadlessException {
      super( owner, DialogButtonBar.OK_CANCEL_BUTTON, true );
      setSynchronous(true);

      String ttitle=null, tinfo=null;

      if ( modus != ACCESS & modus != CONTROL & modus != DEFINE )
         throw new IllegalArgumentException( "modus invalid".concat( String.valueOf( modus )));
      
      opModus = modus;
      
      switch ( modus ) {
      case ACCESS:
         ttitle = "dlg.password.access";
         tinfo = "pwdlg.enter";
         break;
      case DEFINE:
         ttitle = "dlg.password.define";
         tinfo = "pwdlg.define";
         break;
      case CONTROL:
         ttitle = "dlg.password.control";
         tinfo = "pwdlg.control";
         break;
      }
      
      info1 = ResourceLoader.getDisplay( tinfo );
      setTitle( ResourceLoader.getDisplay( ttitle ) );

      buildButtonPanel( exitActive, modus == DEFINE );
      buildCentrePanel( filename, opModus );

      // feed the activity listener
      activMan.registerChangeableObject( enteredPassword );
      activMan.registerChangeableObject( confirmPassword );
      activMan.registerChangeableObject( qualityChk );
      activMan.registerChangeableObject( createButton );
	}

   private void buildButtonPanel( boolean exitActive, boolean createActive ) {
      // we have two additional buttons in the button bar

	   if ( exitActive ) {
         exitButton = new JButton( ResourceLoader.getCommand( "menu.file.exit" ) );
         exitButton.setToolTipText( ResourceLoader.getCommand( "toolbar.exit.tooltip" ) );
         getButtonBar().add( exitButton );
      }
        
      if ( createActive ) {
         createButton = new JButton( ResourceLoader.getDisplay( "button.generate" ) );
         createButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.randpassword" ) );
         getButtonBar().add( createButton );
      }
        
      addButtonBarListener( barListener );
	}

	private void buildCentrePanel( String filename, int modus )
	{
	  Font     passwordFont;
	  VerticalFlowLayout layout;
      JPanel   panel;
      JLabel   nameLabel;
      String   text, info2;

      layout = new VerticalFlowLayout( 7 );
      layout.setHorizontalFill( true );
      panel	= new JPanel( layout );
      panel.setBorder( new EmptyBorder( 20, 20, 3, 20 ) );
      passwordFont = DisplayManager.getFont( "password" );

      // first password entry field
      enteredPassword = new JPasswordField(20);
      enteredPassword.enableInputMethods(true);
      enteredPassword.setFont( passwordFont );

      // label: file name in green colour
      if ( filename == null ) {
         filename = "?";
      }
      text = "<html><font color=\"green\" size=\"+1\">" + filename + "</font></html>";
      nameLabel = new JLabel( text );

      // dialog appearance for ACCESS and CONTROL modi
      panel.add( nameLabel );
      if ( modus == DEFINE ) {
         panel.add( Box.createVerticalStrut( 3 ));
      }
      panel.add( new JLabel( info1 ) );
      panel.add( enteredPassword );

      if ( modus == DEFINE ) {
         confirmPassword = new JPasswordField(20);
         confirmPassword.enableInputMethods(true);
         confirmPassword.setFont( passwordFont );
         
         evalLabel = new JLabel( " " );
         evalLabel.setBackground( Color.red );
         evalLabel.setOpaque( true );
         evalLabel.setBorder( BorderFactory.createEmptyBorder( 3, 15, 3, 0 ) );  

         qualityChk = new JCheckBox( ResourceLoader.getDisplay( "prefbox.ignorepassquality" ) );
         qualityChk.setIconTextGap( 10 );
         Font font = qualityChk.getFont();
         qualityChk.setFont( font.deriveFont( font.getSize()-2 ) );
         
         info2 = ResourceLoader.getDisplay( "pwdlg.confirm" );;
         panel.add( new JLabel( info2 ) );
         panel.add( confirmPassword );
         panel.add( Box.createVerticalStrut( 2 ));
         panel.add( evalLabel );
         panel.add( qualityChk );

         // block the OK button until the passphrase has been confirmed
         getButtonBar().getOkButton().setEnabled( false );
      }
      
      setDialogPanel( panel );
	}  // buildCentrePanel

	/** Checks the conditions for moving on to releasing the
	 * new password by terminating this dialog. It tests password 
	 * quality and displays the result in the color bar. Then it 
	 * tests whether both password fields contain the same value.
	 */
	private void determineValidity ()
	{
	   Color color;
	   String text;
	   char[] p1, p2;
	   int qualPct, domainVar;
	   boolean qualOk, correct;
	   
	   // determine password quality in 4 levels
       p1 = enteredPassword.getPassword();
       qualPct = 0;
       domainVar = Util.textDomainVariance( p1 );
       if ( p1.length >= 5 & Util.textVariance( p1 ) >= 5 )
          qualPct = 50;
       if ( qualPct == 50 && p1.length >= 10 & ((domainVar >= 2 
            & Util.containsDigit( p1 )) | domainVar >= 3) )
          qualPct = 75;
       if ( qualPct == 75 && p1.length >= 12 & domainVar == 4 )
          qualPct = 100;
	   qualOk = qualPct >= 50 | qualityChk.isSelected();

       // display quality picture (label)
	   color = Color.red;
	   text = ResourceLoader.getDisplay( "pwdlg.eval.bad" );
	   if ( qualPct >= 50 ) {
	      color = Color.orange;
          text = ResourceLoader.getDisplay( "pwdlg.eval.medium" );
	   }
	   if ( qualPct >= 75 ) {
	      color = Color.yellow;
          text = ResourceLoader.getDisplay( "pwdlg.eval.good" );
	   }
       if ( qualPct == 100 ) {
          color = Color.green;
          text = ResourceLoader.getDisplay( "pwdlg.eval.excel" );
       }
       evalLabel.setBackground( color );
       evalLabel.setText( text );
	   
	   // check password correctness (double entry)
	   p2 = confirmPassword.getPassword();
	   correct = Util.equalArrays( p1, p2 );
	   Util.destroyChars( p1 );
	   Util.destroyChars( p2 );
	   
	   // release OK button of dialog if pass conditions
       getButtonBar().getOkButton().setEnabled( correct & qualOk );
       Log.debug( 9, "(PasswordDialog.determineValidity) validity checked" + valCheckTimes++ );
	}
    private static int valCheckTimes;
    
	
	/**
	 * Returns the password entered in the dialog.
	 * 
	 * @return the password entered in the dialog
	 */
	public char[] getEnteredPassword() {
		return enteredPassword.getPassword();
	}

	/** Returns a dialog which hold the auto-generated password on display,
	 * in case the user has used this function.
	 * 
	 * @return <code>ButtonBarDialog</code> or <b>null</b>
	 */
	public ButtonBarDialog getPasswordInfoBox () {
	   return infoBox;
	}
	
	/**
	 * Whether this dialog was terminated by a user "OK" action.
	 * @return <b>true</b> if and only if the dialog has terminated and
    *         termination was caused by user pressing "OK" button
	public boolean isOkPressed()
	{
		return okPressed;
	}
     */
   
   /** This should destroy the entered user value. Currently the underlying Java
    * structure does not support secure erasing of all such content.
    */ 
   public void destroy () {
      try {
         Document doc = enteredPassword.getDocument();
         doc.remove(0,doc.getLength());
         if ( confirmPassword != null ) {
            doc = confirmPassword.getDocument();
            doc.remove(0,doc.getLength());
         }
      } catch ( BadLocationException e ) {
      }
   }
     
/*   
	   public void destroy ()
	   {
	       enteredPassword.destroyValue();
           if ( confirmPassword != null )
           {
	           confirmPassword.destroyValue();
           }
	   }
*/
   public void addActivityListener ( ActivityListener listener ) {
      activMan.addActivityListener( listener );
   }
   
   public void removeActivityListener ( ActivityListener listener ) {
      activMan.removeActivityListener( listener );
   }

   /** Performs a modal password dialog on the EDT and waits for its termination. 
    * The resulting dialog instance may be used to retrieve values.
    * 
    * @param ct PwsFileContainer file container referenced, may be null
    * @param filename String naming part of the display message, may be null
    * @param modus int operation modus (DEFINE, ACCESS, CONTROL)
    * @param exitActive boolean true == a program exit button is shown 
    * @return <code>PasswordDialog</code> dialog instance after termination
    * @throws InterruptedException if the calling thread was interrupted
    * @throws IllegalStateException if dialog throws execution exception
    */
   public static PasswordDialog performed (
		   		 final PwsFileContainer ct,
		         final String filename, 
                 final int modus, 
                 final boolean exitActive ) throws InterruptedException {
	   
	   final PasswordDialog[] result = new PasswordDialog[1];
	   Runnable run = new Runnable() {
		  @Override
		  public void run() {
			 PasswordDialog dlg = new PasswordDialog( Global.getActiveFrame(), 
					 filename, modus, exitActive );
		     if ( ct != null ) {
			    dlg.addActivityListener( ct.getActivityListener() );
			 }
		     dlg.setVisible( true );
		     result[0] = dlg;
		  }
	   };
	   
	   try {
		  ActionHandler.executeOnEDT_Wait(run);
		  return result[0];
	   } catch (InvocationTargetException e) {
		  e.printStackTrace();
		  throw new IllegalStateException("dialog execution error", e);
	   }
   }
   
   private final class BarActionListener extends DefaultButtonBarListener
   {
      private BarActionListener () {
         super( PasswordDialog.this );
      }

/*      
      @Override
      public boolean okButtonPerformed ()
      {
         return true;
      }
*/

      @Override
      public void cancelButtonPerformed () {
         if ( infoBox != null ) {
            infoBox.dispose();
            infoBox = null;
         }
         super.cancelButtonPerformed();
      }

      @Override
      public boolean extraButtonPerformed ( Object button ) {  
         if ( button != null ) {
            if ( button == exitButton ) {
               Global.exit( true );
            }
            else if ( button == createButton & 
            		  (infoBox == null || !infoBox.isShowing()) ) {
               infoBox = Service.generatePassword( PasswordDialog.this, false );
               if ( infoBox != null ) {
            	   infoBox.moveRelatedTo(PasswordDialog.this);
            	   
            	  // add a listener for close event of the info box
            	  infoBox.addWindowListener( new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
					   createButton.setEnabled(true);
					}
            	  }); 

            	  // extract the generated passphrase + assign to panel values
                  PwsPassphrase pp = (PwsPassphrase)infoBox.getDialogPanel().getClientProperty( "passphrase" ); 
                  if ( pp != null ) {
                     enteredPassword.setText( pp.getString() );
                     confirmPassword.setText( pp.getString() );
                     return false;
                  }
               }
            }
         }
         return true;
      }
   }

   private final class ActivityManager extends ObjectChangeListener
   {
      public ActivityManager () {
         super( PasswordDialog.this );
      }

      @Override
      protected void fireEvent ( EventObject evt ) {
         if ( opModus == DEFINE ) {
            determineValidity();
         }
         super.fireEvent( evt );
      }
   }
}
