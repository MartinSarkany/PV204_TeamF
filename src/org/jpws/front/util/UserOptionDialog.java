/*
 *  UserOptionDialog in org.jpws.front.util
 *  file: UserOptionDialog.java
 * 
 *  Project jpws-prg
 *  @author Wolfgang Keller
 *  Created 21.06.2012
 *  Version
 * 
 *  Copyright (c) 2012 by Wolfgang Keller, Munich, Germany
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

package org.jpws.front.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;

import org.jpws.front.ActionHandler;
import org.jpws.front.GUIService;
import org.jpws.front.MessageDialog;
import org.jpws.front.MessageDialog.MessageType;
import org.jpws.front.ObjectChangeListener;
import org.jpws.pwslib.global.Log;

/**
 * A modal user interaction dialog to present a message element and a series of
 * button-borne choices, each represented by a String. Choices are displayed
 * in the button bar. Optionally a CANCEL button can be added. 
 * The user's selection, if he didn't opt for cancel, can be retrieved 
 * after the dialog is terminated through "getUserOption()" method.
 *
 */
public class UserOptionDialog extends ButtonBarDialog
                              implements ActionListener
{
   /** Number of display pixels for all options being shown 
    * in a single line instead of a vertical list layout.
    */
   private static final int MAX_BUTTONLINE_PIXELS = 500;

   private ActivityListener listenerAncestor;
   private String[] userOptions;
   private int userChoice = -1;


   public UserOptionDialog ( Window owner, String title, Object msg,
         String[] choices, MessageType type, boolean cancelOption ) {

	  super( owner, DialogButtonBar.BUTTONLESS, true );
      init( owner, title, msg, choices, type, cancelOption );
   }

   private void init ( Window owner, String title, Object msg, String[] choices,
         MessageType type, boolean cancelOption )
   {
      moveRelatedTo(owner);

      userOptions = choices;
      listenerAncestor = ObjectChangeListener.getActivityAncestor( owner );

      title = title == null || title.isEmpty() ? "dlg.optionchoice" : title; 
      setTitle( ResourceLoader.codeOrRealDisplay( title ) );
      setCloseByEscape( cancelOption );

      JPanel content = new JPanel( new BorderLayout() );
      content.setBorder( BorderFactory.createEmptyBorder( 5, 10, 0, 10 ) );

      // main message if supplied 
      if ( msg != null ) {
         JPanel cp = MessageDialog.createMessageContentPanel( msg, type );
//         cp.setBorder( new EmptyBorder( 5, 0, 5, 10 ) );
         content.add( cp );
      }

      int buttonOption = cancelOption ? DialogButtonBar.CANCEL_BUTTON 
    		             : DialogButtonBar.BUTTONLESS;
      DialogButtonBar bar = new DialogButtonBar( buttonOption, true );

      // pro testis create all options as buttons in the bottom bar
      
      if ( choices != null ) {
         // insert choices in reverse order
         for ( int i = choices.length; i > 0; i-- ) {
            String m = choices[i-1];
        	if ( m == null ) continue;
            JButton button = new JButton( ResourceLoader.codeOrRealDisplay( m ) );
            button.setName( m );
            button.addActionListener( this );
            bar.add( button, 0 );
         }
      }
      bar.doLayout();
      
      // drop the bar solution if we find it too large
      // and use a vertical list panel instead
      
//      Log.debug( 8, "(UserOptionDialog.init) detected BAR WIDTH == " + bar.getPreferredSize().getWidth() );
      if ( bar.getPreferredSize().getWidth() > MAX_BUTTONLINE_PIXELS ) {

    	 JPanel choicesPanel = new JPanel( new VerticalFlowLayout( 6, true ) );
         choicesPanel.setBorder( new EmptyBorder( 12, 0, cancelOption ? 0 : 12, 0 ) );
         for ( String m : choices ) {
        	if ( m == null ) continue;
            JButton button = new JButton( ResourceLoader.codeOrRealDisplay( m ) );
            button.setName( m );
            button.addActionListener( this );
            choicesPanel.add( button );
         }
         content.add( choicesPanel, BorderLayout.SOUTH );
         bar = new DialogButtonBar( buttonOption, true );
      }
      
      // set a button bar if meaningful
      if ( !bar.isEmpty() ) {
         setButtonBar( bar, BorderLayout.SOUTH );
      }

      // set the content panel
      setDialogPanel( content );
   }

   public int getUserOption () {
      return userChoice;
   }

   @Override
   public void actionPerformed ( ActionEvent e ) {
      String name = ((JButton)e.getSource()).getName();
      int i = 0;
      for ( String m : userOptions ) {
         if ( name.equals( m ) ) {
            userChoice = i;
            Log.debug( 8, "(UserOptionDialog.actionPerformed) user opted for button: " + m );
            Log.debug( 8, "(UserOptionDialog.actionPerformed) leaving dialog with choice nr == " + i );
            dispose();
            break;
         }
         i++;
      }
   }

   @Override
   public void dispose () {
      if ( listenerAncestor != null ) {
         listenerAncestor.actionOccurred( new ChangeEvent( this ) );
      }
      super.dispose();
   }

   /** Performs a dialog with given parameters on the EDT and returns the user
    * choice as an integer 0..x-1 for x number of choices, -1 if user cancelled
    * the dialog, -2 if the dialog was interrupted or caused an exception.
    * 
    * @param owner Component dialog parent component
    * @param title String dialog title
    * @param msg Object dialog message
    * @param choices String[] user options
    * @param msgType MessageType graphical dialog appearance
    * @param cancelOption boolean whether CANCEL button shall be available
    * @return
    */
   public static int performed ( final Component owner, 
		                         final String title, 
		                         final Object msg,
                                 final String[] choices, 
                                 final MessageType msgType, 
                                 final boolean cancelOption ) {

	   final int[] result = new int[1];
	   Runnable run = new Runnable() {

		  @Override
		  public void run() {
		     Window window = GUIService.getWindowForComponent( owner );
		     UserOptionDialog dlg = new UserOptionDialog( window, title, msg, 
			   	              choices, msgType, cancelOption );
		     dlg.setVisible(true);
		     result[0] = dlg.getUserOption();
		  }
	   };
	   
	   try {
		ActionHandler.executeOnEDT_Wait(run);
		return result[0];
		
	   } catch (InterruptedException e) {
		  e.printStackTrace();
	   } catch (InvocationTargetException e) {
		  e.printStackTrace();
	   }
	   return -2;
   }
   
}
