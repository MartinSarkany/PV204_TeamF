/*
 *  DialogButtonBar in org.jpws.front.util
 *  file: DialogButtonBar.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 28.09.2004
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
package org.jpws.front.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;

import org.jpws.front.ActionHandler;
import org.jpws.pwslib.global.Log;


/**
 * Creates a {@link ButtonBar} containing minimal NO button. Typically it contains
 * at least an "Ok" or a "Close" button.
 * Depending on init settings, additional elements are "Cancel" and "Help"
 * buttons. Moreover custom components (which ideally are JButton or JComboBox)
 * may be added to this bar.
 * 
 * <p>The application can setup a <code>ButtonBarListener</code> in order to
 * specifically react to button/component actions performed by the user. If there
 * is no listener supplied, this bar disposes the first <code>JDialog</code> into 
 * which it is placed upon activation of "Ok", "Close" or "Cancel".   
 * 
 * @author Wolfgang Keller
 */
public class DialogButtonBar extends ButtonBar
                             implements ActionListener, Cloneable
{
   public static final int BUTTONLESS = 0;
   public static final int OK_BUTTON = 1;
   public static final int CLOSE_BUTTON = 2;
   public static final int CANCEL_BUTTON = 3;
   public static final int OK_CANCEL_BUTTON = 4;
   public static final int YES_NO_BUTTON = 5;
   public static final int YES_NO_CANCEL_BUTTON = 6;
   public static final int OK_HELP_BUTTON = 7;
   public static final int OK_CANCEL_HELP_BUTTON = 8;
   public static final int YES_NO_HELP_BUTTON = 9;
   
   private static final int LAST_BUTTON = 9;
   private static final int HELP_BUTTON = 10;
   private static final int EXTRA_BUTTON = 11;
   private static final int NO_BUTTON = 12;
   
   private ArrayList<ButtonBarListener> listeners;
   private Vector<Future<?>>            buttonTasks;

   private JButton      okButton;
   private JButton      cancelButton;
   private JButton      noButton;
   private JButton      helpButton;

   private boolean      okPressed;
   private boolean      okValidated;
   private boolean      cancelPressed;
   private boolean      noPressed;
   private boolean      synchronous = true;

   /**
   *  Creates a button bar with the number of buttons as specified by the
   *  <code>type</code> parameter and asynchronous execution of button actions
   *  (not on the EDT).
   *  
    *  @param type BUTTONLESS, OK_BUTTON, CLOSE_BUTTON, CANCEL_BUTTON, 
    *              OK_CANCEL_BUTTON, OK_CANCEL_HELP_BUTTON,
    *              YES_NO_BUTTON, YES_NO_HELP_BUTTON
   *  @throws IllegalArgumentException if <code>type</code> is unknown
    */
   public DialogButtonBar ( int type )
   {
	   this( type, HORIZONTAL, CENTRE );
   }
   
    /**
    *  Creates a button bar with the number of buttons as specified by the
    *  <code>type</code> parameter and synchronous execution of button actions
    *  (not on the EDT).
    *  
    *  @param type BUTTONLESS, OK_BUTTON, CLOSE_BUTTON, CANCEL_BUTTON, 
    *              OK_CANCEL_BUTTON, OK_CANCEL_HELP_BUTTON,
    *              YES_NO_BUTTON, YES_NO_HELP_BUTTON
    *  @throws IllegalArgumentException if <code>type</code> is unknown
    */
    public DialogButtonBar ( int type,  int orientation, int alignment )
    {
      super(orientation, alignment);
      if ( type < 0 | type > LAST_BUTTON )
         throw new IllegalArgumentException();
      
      String name;
      
      // create "OK" button after dialog type parameter
      if ( type >= OK_BUTTON & type != CANCEL_BUTTON )
      {
         name = type == CLOSE_BUTTON ? "button.close" : "button.ok";
         if ( type == YES_NO_BUTTON | type == YES_NO_CANCEL_BUTTON
              | type == YES_NO_HELP_BUTTON )
            name = "button.yes";
         okButton = new JButton(ResourceLoader.getDisplay( name ));
         okButton.addActionListener( this );
         super.add( okButton );
      }

      // create "NO" button after dialog type parameter
      if ( type == YES_NO_BUTTON | type == YES_NO_CANCEL_BUTTON 
           | type == YES_NO_HELP_BUTTON ) 
      {
         noButton = new JButton(ResourceLoader.getDisplay( "button.no" ));
         noButton.addActionListener( this );
         super.add( noButton );
      }

      // create "CANCEL" button after dialog type parameter
      if ( type == CANCEL_BUTTON | type == OK_CANCEL_BUTTON  
           | type == YES_NO_CANCEL_BUTTON | type == OK_CANCEL_HELP_BUTTON ) 
      {
         cancelButton = new JButton(ResourceLoader.getDisplay( "button.cancel" ));
         cancelButton.addActionListener( this );
         super.add( cancelButton );
      }

      // create "HELP" button after dialog type parameter
      if ( type >= OK_HELP_BUTTON ) {
         helpButton = new JButton(ResourceLoader.getDisplay("button.help"));
         helpButton.addActionListener( this );
         super.add( helpButton );
      }
   }  // constructor
    
    /**
     *  Creates a button bar with the number of buttons as specified by the
     *  <code>type</code> parameter.
     *  
     *  @param type int OK_BUTTON, CLOSE_BUTTON, OK_CANCEL_BUTTON, OK_CANCEL_HELP_BUTTON
     *  @param synchronous boolean whether button action performs on the Event Dispatching Thread
     *  @throws IllegalArgumentException if <code>type</code> is unknown
     */
    public DialogButtonBar ( int type, boolean synchronous )
    {
       this( type );
       this.synchronous = synchronous;
    }
    
    
   
   @Override
   /** Creates a shallow clone of this button bar. */
   protected Object clone () throws CloneNotSupportedException
   {
      return super.clone();
   }

   /** Performs actions of this button bar related to one of its
    * registered buttons. The performance is equivalent to the 
    * event of a button pressed or component activated. 
    * Does nothing if the parameter is void.
    *  
    * @param button button or component triggered
    */
   public void performButton ( Object button )
   {
      if ( button != null )
         actionPerformed( new ActionEvent( button, 0, null ) );
   }
   
   /** Performs actions of this button bar or its registered listeners
    * as a reaction to a pressed button or otherwise performed component.
    * Does nothing if parameter is void.
    *  
    * @param button button or component triggered
    * @since 0-5-0
    */
   private void performButtonIntern ( Object button )
   {
      Component extra;
      boolean hasListeners;
      
      okPressed   = false;
      cancelPressed = false;
      hasListeners = listeners != null && !listeners.isEmpty();
      
      if ( button == null ) return;
      
      if ( button == okButton ) {
         okPressed = true;
         okValidated = true;
         if ( hasListeners ) {
            okButton.setEnabled( false );
            okValidated = performBarListeners( OK_BUTTON, null );
            okButton.setEnabled( true );
         }
         else
            disposeDialog();
      }
      
      else if ( button == cancelButton ) {
         cancelPressed = true;
         if ( hasListeners ) {
            cancelButton.setEnabled( false );
            performBarListeners( CANCEL_BUTTON, null );
            cancelButton.setEnabled( true );
         }
         else
            disposeDialog();
      }
      
      else if ( button == noButton ) {
         noPressed = true;
         if ( hasListeners ) {
            noButton.setEnabled( false );
            performBarListeners( NO_BUTTON, null );
            noButton.setEnabled( true );
         }
         else
            disposeDialog();
      }
      
      else if ( button == helpButton ) {
         if ( hasListeners ) {
            helpButton.setEnabled( false );
            performBarListeners( HELP_BUTTON, null );
            helpButton.setEnabled( true );
         }
      }
      // user added extra buttons
      else {
         if ( hasListeners ) {
            extra = (Component)button;
            extra.setEnabled( false );
            extra.setEnabled( performBarListeners( EXTRA_BUTTON, button ) );
         }
      }
   }  // performButton

   /** Dispatches the button event to the bar listeners and receives
    *  their answers. The first listener to return <b>false</b> breaks
    *  the dispatching process. 
    *  <p>Cancel-Button and Help-Button always return <b>true</b>.
    *  
    * @param buttonType int standard button name
    * @param button <code>Object</b>
    * @return boolean <b>true</b> == all listeners returned "ok" (<b>true</b>);
    *                 <b>false</b> == one listener returned "fail" (<b>false</b>) 
    */
   @SuppressWarnings("unchecked")
   private boolean performBarListeners ( int buttonType, Object button )
   {
      ArrayList<ButtonBarListener> copy;
      boolean ok = true;
      
      if ( listeners != null )
      {
         copy = (ArrayList<ButtonBarListener>)listeners.clone();
         for ( ButtonBarListener i : copy )
         {
            if ( !ok )
               break;
            
            switch ( buttonType )
            {
            case OK_BUTTON:
               if ( !i.okButtonPerformed() )
                  ok = false;
               break;
            case CANCEL_BUTTON:
               i.cancelButtonPerformed();
               break;
            case NO_BUTTON:
               i.noButtonPerformed();
               break;
            case HELP_BUTTON:
               i.helpButtonPerformed();
               break;
            case EXTRA_BUTTON:
               if ( !i.extraButtonPerformed( button ) )
                  ok = false;
               break;
            }
         }
      }
      return ok;
   }
   
   /** Disposes the first occurrence of a JDialog in the 
    * ancestor line of this button bar component.
    * @since 0-5-0
    */
   public void disposeDialog ()
   {
      Log.log( 8, "(DialogButtonBar.disposeDialog) enter disposeDialog" );
      if ( !isShowing() ) return;
      
      // search for nearest JDialog ancestor window
      Component c = this;
      while ( (c=c.getParent()) != null && !(c instanceof JDialog) );
      final JDialog dlg = (JDialog)c;
      
      Runnable r = new Runnable() {
		@Override
		public void run() {
	       try { dlg.dispose(); }
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
/*   
   public void finalize ()
   {
      System.out.println( "*** Finalising DialogButtonBar" );
   }
*/   
   /** Used to implement the internal button action listener. Does nothing for 
    *  external callers. 
    */
   public void actionPerformed( final ActionEvent e )
   {
      if ( listeners != null && !listeners.isEmpty() & !synchronous ) {
         // asynchronous execution of button activity (worker thread)
         Runnable run = new Runnable() {

        	public void run () {
               ActionHandler.resetIdleTime();
               performButtonIntern( e.getSource() );
               ActionHandler.resetIdleTime();
            }
         };
         Future<?> ft = ActionHandler.startTask(run);
         
         // late instantiation of button thread array
         if ( buttonTasks == null ) {
            buttonTasks = new Vector<Future<?>>();
         }

         // add task Future to task list 
         buttonTasks.add( ft );
      }
      else {
         // synchronous execution of button activity (on EDT)
         ActionHandler.resetIdleTime();
         performButtonIntern( e.getSource() );
         ActionHandler.resetIdleTime();
      }
   }

   /** 
    * Blocks execution of the current thread until all tasks triggered
    * by buttons of this bar have been executed. 
    * @since 0-5-0
    */
   @SuppressWarnings("unchecked")
   public void joinButtonThreads () {
      boolean alive;
      
      if ( buttonTasks != null && !buttonTasks.isEmpty() ) {
         do {
            alive = false;
            for ( Future<?> task : (Vector<Future<?>>)buttonTasks.clone() ) {
               try { 
            	   task.get(); 
                   buttonTasks.remove( task );
               } catch ( InterruptedException e ) { 
            	  alive = true; 
               } catch (ExecutionException e) {
			   }
            }
         } while ( alive );
      }
   }
   
   /** Sets whether button triggered actions will run on the
    * Event Dispatching Thread (EDT) (v == true) or on a separate
    * threads (v == false).
    * 
    * @param v boolean 
    */
   public void setSynchronous ( boolean v ) {
      synchronous = v;
   }
   
   /**
    * Adds a <code>ButtonBarListener</code> for this bar that will 
    * handle button actions according to the needs of the user application.
    * If a listener is set external, the default button handling of this 
    * class is disabled.
    * 
    * @param listener  <code>ButtonBarListener</code>
    */
    public void addButtonBarListener( ButtonBarListener listener ) {
       // late instantiation of button-bar listener array
       if ( listeners == null ) {
          listeners  = new ArrayList<ButtonBarListener>();
       }
       
       synchronized ( listeners ) {
          if ( !listeners.contains( listener ) )
             listeners.add( listener );
       }
    }

    /**
     * Removes a <code>ButtonBarListener</code> from this bar.
     * 
     * @param listener  <code>ButtonBarListener</code>
     */
     public void removeButtonBarListener( ButtonBarListener listener ) {
        if ( listeners != null ) {
           synchronized ( listeners ) {
              listeners.remove( listener );
           }
        }
     }

   /**
    * @return Returns the help button or <b>null</b> if undefined.
    */
   public JButton getHelpButton()
   {
      return helpButton;
   }

    /**
     * @return Returns the cancelButton or <b>null</b> if undefined.
     */
    public JButton getCancelButton()
    {
        return cancelButton;
    }

    /**
     * @return Returns the noButton or <b>null</b> if undefined.
     */
    public JButton getNoButton()
    {
        return noButton;
    }

    /**
     * @return Returns the okButton or <b>null</b> if undefined.
     */
    public JButton getOkButton()
    {
        return okButton;
    }

    /**
     * Returns <code>true</code> if the "Cancel" button was pressed by the
     * last user action on this button bar.
     */
    public boolean isCancelPressed()
    {
        return cancelPressed;
    }

    public boolean isNoPressed ()
    {
       return noPressed;
    }
    
    /**
     * Returns <code>true</code> if the "Ok" button was pressed by the
     * last user action on this button bar.
     * 
     */
    public boolean isOkPressed()
    {
        return okPressed;
    }

    /**
     * Returns <code>true</code> if the "OK" (or "Close") button was 
     * pressed and the validation routines of any button-bar listeners 
     * have all returned <b>true</b> ("ok").
     * 
     */
    public boolean isOkValidated()
    {
        return okValidated;
    }

   /**
    * Adds a component to the button bar at the utmost TRAILING position. 
    * If the component is a JButton or a JComboBox then this button bar
    * is added to its action event dispatcher.
    * 
    *  @since 0-4-0
    */ 
   @Override
   public Component add ( Component button )
   {
      Component c;
      
      remove( button );
      c = super.add( button );
      if ( button instanceof JButton )
         ((JButton)button).addActionListener( this );
      else if ( button instanceof JComboBox )
         ((JComboBox)button).addActionListener( this );
      return c;
   }
   
   /**
    * Adds a component to the button bar at the specified index position. 
    * If the component is a JButton or a JComboBox then this button bar
    * is added to its action event dispatcher.
    * 
    *  @since 0-4-0
    */ 
   @Override
   public Component add ( Component button, int index )
   {
      Component c;
      
      remove( button );
      c = super.add( button, index );
      if ( button instanceof JButton )
         ((JButton)button).addActionListener( this );
      else if ( button instanceof JComboBox )
         ((JComboBox)button).addActionListener( this );
      return c;
   }

   // since 0-5-0
   @Override
   public void remove ( Component button ) {
      super.remove( button );
      if ( button instanceof JButton )
         ((JButton)button).removeActionListener( this );
      else if ( button instanceof JComboBox )
         ((JComboBox)button).removeActionListener( this );
   }

   // since 0-5-0
   @Override
   public void remove ( int index ) {
      super.remove( index );
   }

   public boolean isSynchronous () {
      return synchronous;
   }

   /** Removes all ButtonBarListener-s from this button bar. 
    */
   public void clearBarListeners() {
       if ( listeners != null ) {
    	   listeners.clear();
       }
   }

}
