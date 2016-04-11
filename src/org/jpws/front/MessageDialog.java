/*
 *  MessageDialog in org.jpws.front
 *  file: MessageDialog.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 11.04.2010
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
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;

public class MessageDialog extends ButtonBarDialog
{
   public static enum MessageType { noIcon, info, question, warning, error }
   
   private static final Color warningColor = new Color( 0xff, 0x7f, 0x50 ); // netscape.coral
   private static final Color errorColor = new Color( 0xf0, 0x80, 0x80 ); // netscape.lightcoral

   private MessageContentPanel   dialogPanel;
   
/**
 * Creates an empty, non-modal info message dialog with the active mainframe
 * as owner and OK + CANCEL buttons. 
 */ 
public MessageDialog () throws HeadlessException
{
   super();
   init( MessageType.info );
}

/**
 * Creates an empty info message dialog 
 * of the given dialog type and modality.
 *  
 * @param owner <code>Window</code> the parent window (Dialog or Frame);
 *        may be <b>null</b>
 * @param dlgType int, dialog type declares standard buttons used 
 *        (values from class <code>DialogButtonBar</code>,  
 *        use <code>DialogButtonBar.BUTTONLESS</code> for void button bar) 
 * @param modal boolean, whether this dialog is operating modal
 * 
 * @throws HeadlessException
*/ 
public MessageDialog ( Window owner, int dialogType, boolean modal )
      throws HeadlessException
{
   this( owner, null, MessageType.info, dialogType, true );
}

/**
 * Creates a message dialog with given content and settings.
 * 
 * @param owner <code>Window</code> the parent window (Dialog or Frame);
 *        may be <b>null</b>
 * @param text Object the text content; may be <b>null</b> 
 * @param msgType <code>MessageType</code> determines display outfit (e.g. icon)
 * @param dlgType int, dialog type declares standard buttons used 
 *        (values from class <code>DialogButtonBar</code>,  
 *        use <code>DialogButtonBar.BUTTONLESS</code> for void button bar) 
 * @param modal boolean, whether this dialog is operating modal
 * 
 * @throws HeadlessException
 */
public MessageDialog ( Window owner, Object text, 
      MessageType msgType, int dlgType, boolean modal )
      throws HeadlessException
{
   super( owner, dlgType, modal );
   moveRelatedTo(owner);
   init( msgType);
   if ( text != null )
      setText( text );
}

/**
 * Creates a modal info message dialog with OK_BUTTON
 * and the given text content.
 *   
 * @param owner <code>Window</code> the parent window (Dialog or Frame);
 *        may be <b>null</b>
 * @param text <code>Object</code> the text content (<code>String</code> 
 *        or <code>Component</code>); may be <b>null</b>
 *        
 * @throws HeadlessException
 */
public MessageDialog ( Window owner, Object text )
      throws HeadlessException
{
   this( owner, text, MessageType.info, DialogButtonBar.OK_BUTTON, true );
}

private void init ( MessageType mType )
{
   // body panel (base)
   dialogPanel = new MessageContentPanel() ;
   dialogPanel.setMessageType( mType );

   setDialogPanel( dialogPanel );
   setSynchronous( true );
}

/** Sets the message display type for this dialog.
 * 
 * @param type <code>MessageType</code> new message type
 */
public void setMessageType ( MessageType type )
{
   dialogPanel.setMessageType( type );
}

/** Returns the <code>MessageType</code> of this dialog.
 *  
 * @return <code>MessageType</code>
 */
public MessageType getMessageType ()
{
   return dialogPanel.getMessageType();
}

/**
 * Sets a text label to this message dialog and resizes
 * it if required.
 * 
 * @param text <code>Object</code> new text content
 *        (an object of type <code>String</code> or <code>Component</code>)
 */
public void setText ( Object text )
{
   dialogPanel.setText( text );
   if ( isShowing() )
      pack();
}

/**
 * Sets an icon for the message display.
 * 
 * @param icon <code>Icon</code> new message logo, use <b>null</b> to clear
 */
public void setIcon ( Icon icon )
{
   dialogPanel.setIcon( icon );
   if ( isShowing() )
      pack();
}

// ****************** STATIC SERVICE OFFER *****************

/** Returns a <code>JPanel</code> with a message display format and content.
 *   
 * @param text <code>Object</code> String or Component
 * @param type <code>MessageType</code> layout appearance
 * @return <code>MessageContentPanel</code>
 */
public static MessageContentPanel createMessageContentPanel ( Object text, MessageType type )
{
   MessageContentPanel p = new MessageContentPanel();
   p.setMessageType( type );
   p.setText( text );
   return p;
}

public static JLabel createMessageTextLabel ( String text )
{
   String hstr = ResourceLoader.codeOrRealDisplay( text );
   JLabel label = new JLabel( hstr );
   label.setOpaque( false );
   return label;
}

/** Displays a simple modal info message that the user can click away
 * through "Ok" button. Display is guaranteed to run on the EDT. The calling 
 * thread blocks until user input is available or the thread is interrupted.
 *  
 * @param parent <code>Component</code> the component this message's 
 *               display is related to; may be null
 * @param title <code>String</code> dialog title; may be <b>null</b>              
 * @param text <code>Object</code> text to display (<code>Component</code> 
 *             or <code>String</code>)
 * @param type <code>MessageType</code> appearance quality            
 */
public static void showInfoMessage ( Component parent, String title, Object text, 
      MessageType type )
{
   showMessage(parent, title, text, type, DialogButtonBar.OK_BUTTON);
}

/** Displays a modal question message which the user can answer with "Yes" 
 * or "No". Display is guaranteed to run on the EDT. The calling thread blocks 
 * until user input is available or the thread is interrupted.
 * 
 * @param parent <code>Component</code> the component this message's 
 *               display is related to; may be null
 * @param title <code>String</code> dialog title; may be <b>null</b>              
 * @param text <code>Object</code> text to display (<code>Component</code> 
 *             or <code>String</code>)
 * @param dlgType int dialog type, one of DialogButtonBar.YES_NO_BUTTON, DialogButtonBar.OK_CANCEL_BUTTON           
 * @return boolean <b>true</b> == "Yes" (confirmed), <b>false</b> == "No" (rejected/unanswered) 
 */
public static boolean showConfirmMessage ( final Component parent, 
		                                   final String title, 
                                           final Object text, 
                                           final int dlgType )
{
	return showMessage(parent, title, text, MessageType.question, dlgType)[0];
}

/** Displays a modal message box with user interaction as designed by given
 * parameters. Display is guaranteed to run on the EDT. The calling thread 
 * blocks until user input is available or the thread is interrupted.
 * 
 * @param parent <code>Component</code> the component this message's 
 *               display is related to; may be null
 * @param title <code>String</code> dialog title; may be <b>null</b> or a code           
 * @param text <code>Object</code> text to display (<code>Component</code> 
 *             or <code>String</code>)
 * @param type <code>MessageType</code> appearance quality            
 * @param dlgType int dialog type for button setup (see DialogButtonBar)            
 * @return boolean[] with values indicating what action was taken to terminate
 *         the dialog: 0 = confirmed, 1 = Cancel, 2 = No, 3 = Escape 
 */
public static boolean[] showMessage ( final Component parent, 
		                                   final String title, 
                                           final Object text, 
                                           final MessageType msgType,
                                           final int dlgType )
{
   final boolean[] result = new boolean[4];
   final Window ancestor = parent == null ? null : GUIService.getAncestorWindow( parent );
   
   Runnable run = new Runnable() {
		@Override
		public void run() {
		   MessageDialog dlg = new MessageDialog( ancestor, text, msgType, dlgType, true );
		   boolean singleButton = dlgType == DialogButtonBar.OK_BUTTON | 
				   dlgType == DialogButtonBar.CANCEL_BUTTON | 
				   dlgType == DialogButtonBar.CLOSE_BUTTON;
		   String defTitle = singleButton ? "dlg.information" : "dlg.confirm";
		   String hstr = title == null ? defTitle : title;
		   dlg.setTitle( ResourceLoader.codeOrRealDisplay( hstr ) );
		   dlg.setAutonomous( msgType != MessageType.question );
		   dlg.pack();
		   dlg.setVisible(true);
		   result[0] = dlg.isOkPressed();
		   result[1] = dlg.isCancelPressed();
		   result[2] = dlg.isNoPressed();
		   result[3] = dlg.isCloseByEscape();
		}
   };

   try {
   	  ActionHandler.executeOnEDT_Wait(run);
	} catch (InvocationTargetException e) {
		e.printStackTrace();
	} catch (InterruptedException e) {
		e.printStackTrace();
	}

   return result;
}

/** Runs "setVisible(true)" on the given dialog guaranteed on the EDT.
 * @param dialog JDialog
 */
private static void setDialogVisible ( final JDialog dialog ) {
	
    Runnable r = new Runnable() {
		@Override
		public void run() {
	       try { dialog.setVisible(true); }
	       catch ( Exception e )
	       {}
		}
    };

    try {
    	ActionHandler.executeOnEDT_Wait(r);
	  } catch (InvocationTargetException e) {
		e.printStackTrace();
	  } catch (InterruptedException e) {
		e.printStackTrace();
	  }
}

// ***********  INNER CLASS  ******************

/**
 * A JPanel for holding message display data and layout
 * consisting of an Icon on the left side and a centered 
 * text block on the right. The text may be represented
 * by either a string or a <code>Component</code>.
 * 
 */
public static class MessageContentPanel extends JPanel
{

   private JPanel   textPanel;
   private JLabel   iconLabel;
   private MessageType messageType = MessageType.noIcon;

   /** Creates a new message panel of type <code>noIcon</code>
    * and which is empty of content.
    */
   public MessageContentPanel ()
   {
      super( new BorderLayout( 6, 6 ) );
      init();
   }

   private void init ()
   {
      // body panel (base)
      setBorder( BorderFactory.createEmptyBorder( 12, 10, 5, 25 ) );
   
      // prepare TEXT label (CENTER of display)
      textPanel = new JPanel( new BorderLayout() ) ;
      textPanel.setOpaque( false );
      add( textPanel, BorderLayout.CENTER );
   
      // prepare ICON label (WEST side of display)
      iconLabel = new JLabel();
      iconLabel.setIconTextGap( 0 );
      iconLabel.setHorizontalAlignment( SwingConstants.CENTER );
      iconLabel.setBorder(  BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
      add( iconLabel, BorderLayout.WEST );
   }

   /** Sets the message display type for this dialog.
    * 
    * @param type <code>MessageType</code> new message type
    */
   public void setMessageType ( MessageType type )
   {
      String hstr;
      Icon icon;
      
      // avoid unnecessary action
      if ( type == messageType ) return;
      
      // set text background color
      if ( type == MessageType.warning ) {
         setBackground( warningColor );
         setOpaque( true );
      }
      else if ( type == MessageType.error ) {
         setBackground( errorColor );
         setOpaque( true );
      }
      else
         setOpaque( false );
      
      // select icon from message type
      if ( type == MessageType.info )
         hstr =  "OptionPane.informationIcon";
      else if ( type == MessageType.question )
         hstr = "OptionPane.questionIcon";
      else if ( type == MessageType.error )
         hstr = "OptionPane.errorIcon";
      else
         hstr = "OptionPane.warningIcon";
      
      // obtain UI standard icon for messages
      // and set icon
      if ( type != MessageType.noIcon ) {
         icon = UIManager.getIcon( hstr );
         setIcon( icon );
      }
      else
         setIcon( null );
      
      messageType = type;
   }

   /**
    * Sets an icon for the message display.
    * 
    * @param icon <code>Icon</code> new message logo, use <b>null</b> to clear
    */
   public void setIcon ( Icon icon )
   {
      JLabel icL = getIconLabel();
      icL.setIcon( icon );
   }

   /** Returns the <code>MessageType</code> of this dialog.
    *  
    * @return <code>MessageType</code>
    */
   public MessageType getMessageType ()
   {
      return messageType;
   }

   private JPanel getTextPanel ()
   {
      return textPanel;
   }

   private JLabel getIconLabel ()
   {
      return iconLabel;
   }

   /**
    * Sets a text label to this message dialog and resizes
    * it if required.
    * 
    * @param text <code>Object</code> new text content
    *        (an object of type <code>String</code> or <code>Component</code>)
    */
   public void setText ( Object text )
   {
      JPanel panel;
      JLabel label;
   
      panel = getTextPanel();
      panel.removeAll();
      
      if ( text != null ) {
         if ( text instanceof String ) {
            label = createMessageTextLabel( (String)text );
            panel.add( label );

         } else if ( text instanceof Component ) {
            panel.add( (Component)text );
         }
      }
   }

}

}
