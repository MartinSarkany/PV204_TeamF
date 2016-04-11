/*
 *  StatusBar in org.jpws.front
 *  file: StatusBar.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 02.10.2005
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
import java.awt.Dimension;
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jpws.front.util.ResourceLoader;

/**
 *  A status bar component (JPanel) with multiple separately addressable display 
 *  elements. These elements are "Text", "File Format" and "Program Activity".
 *  <p>The status bar is a constitutional part of the program mainframe (
 *  <code>PwsafeJ</code>).
 *  
 *  <p>StatusBar operates display activity safely on the EDT. 

 */
public class StatusBar extends JPanel
{
   /** Message display duration time; 30 seconds. */
   public static final int STATUSTEXTDELAY = 30000;
   
   public static final int ACTIVE = 1;
   public static final int PASSIVE = 0;
   
   /** Internal marker for operating the SwingUtilities block. */
   private enum OperationType { message, counter, dataformat, activity, font }; 

   private JPanel          rightPanel;
   private JLabel          textField = new JLabel();
   private JLabel          activeLabel = new JLabel();
   private JLabel          formatLabel = new JLabel();
   private JLabel          reccountLabel = new JLabel();
   private Timer           statusTimer;
   private TimerTask       statusTextRemover;

public StatusBar ()
{
   super( new BorderLayout() );
   init();
}

private void init ()
{
   JPanel panel;
   
   // determine fonts
   setFont( textField.getFont() );
   statusTimer = Global.getTimer();
   
   // init message text output
   add( textField, BorderLayout.CENTER );
   textField.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.gray ),
         BorderFactory.createEmptyBorder( 3, 8, 3, 2 ) ));

   // init right side parts
   rightPanel = new JPanel( new BorderLayout() );
   add( rightPanel, BorderLayout.EAST );
   panel = new JPanel( new BorderLayout() );
   rightPanel.add( panel, BorderLayout.EAST );

   // program activity icon
   panel.add( activeLabel, BorderLayout.EAST );
   activeLabel.setPreferredSize( new Dimension( 22, 16 ) );
   activeLabel.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 0, 1, 0, Color.gray ),
         BorderFactory.createEmptyBorder( 2, 3, 2, 0 ) ));
   
   // file encoding cell
   panel.add( reccountLabel, BorderLayout.CENTER );
   reccountLabel.setVisible( false );
   reccountLabel.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 0, 1, 1, Color.gray ),
         BorderFactory.createEmptyBorder( 2, 4, 2, 4 ) ));

   // file format cell
   rightPanel.add( formatLabel, BorderLayout.CENTER );
   formatLabel.setVisible( false );
   formatLabel.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 0, 1, 1, Color.gray ),
         BorderFactory.createEmptyBorder( 2, 4, 2, 4 ) ));
   
}  // init

/** Drops a text message into the status line. */ 
public void setMessage ( String text )
{
   // setup a timer task for message removal
   if ( statusTextRemover != null )
      statusTextRemover.cancel();
   
   statusTextRemover = new TimerTask()
   {
      public void run ()
      {
         startOperation( OperationType.message, null );
      }
   };
   statusTimer.schedule( statusTextRemover, STATUSTEXTDELAY );
   
   if ( text == null )
      text = "";
   else
      text = ResourceLoader.codeOrRealDisplay( text );
//   if ( text.length() > 80 )
//      text = text.substring( 0, 77 ) + " ..";
   
   startOperation( OperationType.message, text );
}

/** Sets the content of the "Format" cell of the status line. 
 *  If <b>null</b> the cell will be invisible. 
 * 
 *  @param text new content of Format cell
 */
public void setFormatCell ( String text )
{
//   Log.log( 10, "(StatusBar.setFormatCell) set FORMAT with " + text); 
   startOperation( OperationType.dataformat, text );
}

/** Sets the content of the "Text Encoding" cell of the status line. 
 *  If <b>null</b> the cell will be invisible. 
 * 
 *  @param text new content of Encoding cell
 */
public void setRecordCounterCell ( String text )
{
//   Log.log( 10, "(StatusBar.setRecordCounterCell) set COUNTER with " + text ); 
   startOperation( OperationType.counter, text );
}

/** Informs StatusBar about the current program activity modus. This will set 
 *  the content of the "Program Activity" cell.
 * 
 *  @param activity program activity constant (ACTIVE or PASSIVE)   
 * */
public void setActivity ( int activity )
{
//   Log.log( 10, "(StatusBar.setActivity) set ACTIVITY with " + activity ); 
   startOperation( OperationType.activity, new Integer( activity ) );
}

public void setFont ( Font font )
{
   startOperation( OperationType.font, font );
}

public void startOperation ( OperationType dataformat, Object param )
{
   ActionHandler.executeOnEDT( new SwingOperation( dataformat, param ) );
}

public void shutdown ()
{
   statusTimer.cancel();
}

/** This class runs Swing relevant operations in a shell
 * in order to guarantee their performance on the EDT.  
 */
private class SwingOperation implements Runnable
{
   private OperationType operation;
   private Object par;

   SwingOperation ( OperationType type, Object param )
   {
      operation = type;
      par = param;
   }

   @Override
   public void run ()
   {
      try {
         switch ( operation )
         {
         case message:
            textField.setText( par == null ? null : par.toString() );
            break;
            
         case dataformat:
//            Log.log( 10, "(StatusBar.SwingOperation.run) DATAFORMAT with ".concat( 
//                  par == null ? "null" : par.toString() ));
            if ( par == null )
               formatLabel.setVisible( false );
            else
            {
               formatLabel.setText( par.toString() );
               formatLabel.setVisible( true );
            }
            break;
            
         case counter:
//            Log.log( 10, "(StatusBar.SwingOperation.run) COUNTER with ".concat( 
//                  par == null ? "null" : par.toString() ));
            if ( par == null )
               reccountLabel.setVisible( false );
            else
            {
               reccountLabel.setText( par.toString() );
               reccountLabel.setVisible( true );
            }
            break;
            
         case activity:
            boolean activ = par != null && ((Integer)par).intValue() == ACTIVE;
//            Log.log( 10, "(StatusBar.SwingOperation.run) ACTIVITY with ".concat( String.valueOf( activ )));
            Icon icon = activ ? ResourceLoader.getImageIcon( "activity" ) : null;
            activeLabel.setIcon( icon );
            break;
            
         case font:
            if ( par != null )
            {
               Font font = (Font)par;
//               Log.log( 10, "(StatusBar.SwingOperation.run) FONT with ".concat( font.getName() ));
               if ( font != null & reccountLabel != null )
               {
                  font = font.deriveFont( Font.PLAIN );
                  reccountLabel.setFont( font );
                  formatLabel.setFont( font );
                  textField.setFont( font );
               }
               StatusBar.super.setFont( font );
            }
            break;
         }
      } catch ( Exception e )
      { e.printStackTrace(); }
   }
}

}
