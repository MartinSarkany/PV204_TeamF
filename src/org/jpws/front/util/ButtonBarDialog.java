/*
 *  ButtonBarDialog in org.jpws.front
 *  file: ButtonBarDialog.java
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

package org.jpws.front.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jpws.data.PersistentOptions;
import org.jpws.front.ActionHandler;
import org.jpws.front.Global;
import org.jpws.pwslib.global.Log;

/**
 * This is a dialog framework (extending <code>JDialog</code>) including a
 * buttonbar component in the South, which can hold a variety of 
 * typically usable or custom made buttons.   
 * 
 * <p>By default a <code>ButtonBarDialog</code> is not resizable and the 
 * <i>Window-Closing</i> event is equivalent to CANCEL (1. option) or 
 * OK (2. option) button pressed by the user. Default button execution is
 * synchronous, which normally means on the EDT.
 *  
 * @author Wolfgang Keller
 * @since 0-5-0
 */
public class ButtonBarDialog extends JDialog
{
   /** List of child windows (which are of the same type/class) */
   protected List<Dialog> childWindows;

   /** Listener to parent frame Component events. */
   private AutomoveAdapter      moveAdapter;
   
   /** Listener for ESCAPE key (keyboard) registered in root pane. */
   private ActionListener       kListener = new KListener();
   
   private Window               owner;
   private JPanel               dlgPanel;
   private DialogButtonBar      buttonbar;
   private String               singletonName;
   private boolean              closeByEscape = true;
   private boolean              clipping = true;
   private boolean              isAutonomous;
   private boolean              removeOnDisplay;

   /**
    * Creates a non-modal dialog with the active mainframe as owner
    * using the <code>DialogButtonBar.OK_CANCEL_BUTTON</code> dialog type.
    */
   public ButtonBarDialog ()  throws HeadlessException
   {
      super( Global.getActiveFrame() );
      init( null, DialogButtonBar.OK_CANCEL_BUTTON );
   }
   
   /** Constructor for an empty dialog of given type and modality.
    * 
    * @param owner  <code>Window</code> the parent window (Dialog or Frame);
    *              may be <b>null</b>
    * @param type int, dialog type declares standard buttons used 
    *        (values from class <code>DialogButtonBar</code>,  
    *        use <code>DialogButtonBar.BUTTONLESS</code> for void button bar) 
    * @param modal boolean, whether this dialog is modal
    * 
    * @throws java.awt.HeadlessException
    */
   public ButtonBarDialog( Window owner, int  type, boolean modal ) 
          throws HeadlessException
   {
      this( owner, null, type, modal );
   }

   /** Constructor for a dialog with content panel.
    * 
    * @param owner  <code>Window</code> the parent window (Dialog or Frame);
    *               may be <b>null</b>
    * @param panel  <code>JPanel</code> containing dialog components;
    *               may be <b>null</b>
    * @param type int, dialog type declares standard buttons used 
    *        (values from class <code>DialogButtonBar</code>,  
    *        use <code>DialogButtonBar.BUTTONLESS</code> for void button bar) 
    * @param modal boolean, whether this dialog is modal
    * 
    * @throws java.awt.HeadlessException
    */
   public ButtonBarDialog( Window owner, JPanel panel, int  type, boolean modal ) 
          throws HeadlessException
   {
      super( owner == null ? Global.getActiveFrame() : owner, 
            modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS );
      init( panel, type );
   }

   @SuppressWarnings("hiding")
   private void init ( JPanel panel, int type ) {
	  owner = getOwner();
      setResizable( false );
      if ( owner != null && owner == Global.mainFrame ) {
         moveRelatedTo( owner );
      }
      
      addComponentListener( new CListener() );
      
      // register ESCAPE key as (optional) window close trigger
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      getRootPane().registerKeyboardAction(
            kListener, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

      buildCentrePanel( panel );
      buildButtonPanel( type );
      pack();
   }
   
   /** Disposes and destroys this dialog. */
   public void dispose () {
      Log.log( 10, "(ButtonBarDialog.dispose) DISPOSING dialog \"" + getTitle() + 
            "\"; modal == " + isModal() );
      super.dispose();
      if ( singletonName != null )
         Global.setDialogActive( singletonName, false );
      
      if ( owner instanceof ButtonBarDialog )
         ((ButtonBarDialog)owner).removeChildWindow( this );
      else if ( owner != null && owner == Global.mainFrame )
         Global.mainFrame.removeChildWindow( this );

      if ( moveAdapter != null )
         moveAdapter.release();
   }

   protected synchronized void addChildWindow ( ButtonBarDialog win )
   {
      if ( win != null )
      {
         // late instantiation of child window array list 
         if ( childWindows == null )
            childWindows  = new ArrayList<Dialog>();
         
         if ( !childWindows.contains( win ) )
         {
            Log.log( 7, "(ButtonBarDialog.addChildWindow) adding child window : " + win.getTitle() );
            childWindows.add( win );
         }
      }
   }
   
   protected synchronized void removeChildWindow ( ButtonBarDialog win )
   {
      if ( childWindows != null && childWindows.remove( win ) )
         Log.log( 7, "(ButtonBarDialog.addChildWindow) removed child window : " + win.getTitle() );
   }
/*   
   public void finalize ()
   {
     Log.log( 7, "(ButtonBarDialog.finalize) FINALIZE : " + getTitle() );
   }
*/   
   /** 
    * Halts execution of the current thread until all threads triggered
    * by buttons of this dialog's button bar have been terminated. 
    */
   public void joinButtonThreads ()
   {
      if ( buttonbar != null )
         buttonbar.joinButtonThreads();
   }
   
   /** Adds a <code>ButtonBarListener</code> to this dialog. 
    *  
    *  @param barListener <code>ButtonBarListener</code>
    * */
   public void addButtonBarListener( ButtonBarListener barListener )
   {
      if ( buttonbar != null ) 
         buttonbar.addButtonBarListener( barListener );
      else
         throw new UnsupportedOperationException( "dialog has no button bar!" );
   }
   
   /** Clears all <code>ButtonBarListener</code>s from this dialog. 
    * */
   public void clearBarListeners()
   {
      if ( buttonbar != null ) 
         buttonbar.clearBarListeners();
      else
         throw new UnsupportedOperationException( "dialog has no button bar!" );
   }
   
   /** Removes a <code>ButtonBarListener</code> from this dialog. 
    *  
    *  @param barListener <code>ButtonBarListener</code>
    * */
   public void removeButtonBarListener( ButtonBarListener barListener )
   {
      if ( buttonbar != null )
         buttonbar.removeButtonBarListener( barListener );
   }
   
   /** Returns the button bar used in South display of this dialog.
    * @return  <code>DialogButtonBar</code> or <b>null</b> if this dialog
    *          has no button-bar
    */
   public DialogButtonBar getButtonBar ()
   {
      return buttonbar;
   }
   
   /** Returns the content panel used in North display of this dialog. 
    *  (The default panel owns a <code>VerticalFlowLayout</code>.)
    */
   public JPanel getDialogPanel ()
   {
      return dlgPanel;
   }
   
   /** Sets the content panel used in North display of this dialog.
    *  If <b>null</b> is specified, a new (empty) default panel is created.
    *  
    * @param panel <code>JPanel</code> functioning as the new dialog 
    *        content (Center)
    */
   public void setDialogPanel( JPanel panel )
   {
      buildCentrePanel( panel );
      pack();
   }
   
   public void pack ()
   {
      super.pack();
      Util.centreWithin( getOwner(), this );
      setCorrectedBounds( this.getBounds() );
   }
   
   /** Sets whether this dialog's window is allowed to clip with the
    * system screen, so that only part of it may be visible when 
    * initially displayed. By default <b>true</b>.
    * @param v boolean <b>true</b> == clipping allowed  
    * @since 0-6-0
    */
   public void setClipping ( boolean v )
   {
      clipping = v;
   }

   /** Sets whether this dialog is given the AUTONOMOUS feature (title).
    * Dialogs which are NOT autonomous get disposed when programmatically set to
    * INVISIBLE, while autonomous dialogs may return to VISIBLE state later
    * e.g. through a return to VISIBLE of its parent window. 
    * By default this feature is <b>false</b>.
    * 
    * @param v boolean <b>true</b> == dialog is autonomous  
    * @since 0-6-0
    */
   public void setAutonomous ( boolean v )
   {
      isAutonomous = v;
      
      if ( owner instanceof ButtonBarDialog )
      {
         if ( v )
            ((ButtonBarDialog)owner).addChildWindow( this );
         else
            ((ButtonBarDialog)owner).removeChildWindow( this );
      }
      
      else if ( owner != null & owner == Global.mainFrame )
      {
         if ( v )
            Global.mainFrame.addChildWindow( this );
         else
            Global.mainFrame.removeChildWindow( this );
      }
   }

   /** Returns whether this dialog's window is allowed to clip with the
    * system screen.
    * @since 0-6-0
    */
   public boolean isClippingAllowed ()
   {
      return clipping;
   }
   
   /** Sets whether this dialog can be closed by the ESCAPE button.
    * If <b>true</b>, the triggered action of the ESCAPE button is 
    * equivalent to the <tt>WindowClosing</tt> event (as called by a
    * frame decoration close, e.g.). The default value is <b>true</b>. 
    * @param v boolean <b>true</b> == closeable by ESCAPE  
    * @since 0-6-0
    */
   public void setCloseByEscape ( boolean v )
   {
      closeByEscape = v;
   }

   /** Returns <b>true</b> if and only if this dialog can be closed
    * by pressing the ESCAPE button.
    * @since 0-6-0
    */
   public boolean isCloseByEscape ()
   {
      return closeByEscape;
   }
   
   /** Sets an (additional) display gap between button bar and dialog panel.
    * (Defaults to zero.)
    * 
    * @param v pixel of gap
    */  
   public void setBarGap ( int v )
   {
      ((BorderLayout)getContentPane().getLayout()).setVgap( v );
   }
   
   private void buildButtonPanel( int type )
   {
      if ( type > DialogButtonBar.BUTTONLESS )
         setButtonBar( new DialogButtonBar( type ), BorderLayout.SOUTH );
   }

   /**
    * Sets a new button bar for this dialog. If a button bar existed
    * previously, it is removed. If no orientation is specified by <code>
    * constraints</code>, the user must make the button bar an element of the
    * content pane before calling this method. 
    * 
    * @param bar <code>DialogButtonBar</code> the new button bar 
    *        or <b>null</b> to remove existing bar
    * @param constraints String location of bar in display (use BorderLayout constraints)
    *        or <b>null</b> for no specified placement
    */
   public void setButtonBar( DialogButtonBar bar, String constraints )
   {
      if ( buttonbar != null ) {
         getContentPane().remove( buttonbar );
         buttonbar = null;
      }

      if ( bar != null ) {
         buttonbar = bar;
         if ( constraints != null ) {
        	 getContentPane().add( bar, constraints );
         }
         getRootPane().setDefaultButton( bar.getOkButton() );
      }

      if ( isShowing() ) {
         pack();
      }
   }
   
   private void buildCentrePanel( JPanel panel )
   {
      VerticalFlowLayout flowLayout;
      Component prevPanel;
      
      flowLayout = new VerticalFlowLayout();
      prevPanel = dlgPanel;
      
      if ( panel != null )
         dlgPanel = panel;
      else
      {
         dlgPanel = new JPanel( flowLayout );
         dlgPanel.setBorder( BorderFactory.createEmptyBorder( 15, 20, 0, 20 ) );
      }

      if ( prevPanel != null )
         getContentPane().remove( prevPanel );
      
      getContentPane().add( dlgPanel, BorderLayout.CENTER );
   }

   /**
    * Makes this dialog move whenever the parent component is moved
    * or resized. Use <b>null</b> to disengage.
    * 
    * @param parent move-able <code>Component</code>, e.g. windows, frames, dialogs
    */
   public void moveRelatedTo ( Component parent )
   {
      if ( moveAdapter != null )
         moveAdapter.release();
      
      if ( parent != null )
         moveAdapter = new AutomoveAdapter( parent, this );
   }
   
   /** Moves this dialog to a new location on screen by showing a
    * movement animation.
    * 
    * @param p <code>Point</code> new location of this dialog
    *          in the coordinates of its owner
    * @param speed int movement speed in pixel per second
    * @return <code>Point</code> location reached
    */
   public Point moveTo ( Point p, int speed )
   {
      // TODO
      
      setLocation( p );
      return p;
   }
   
   /**
    * Stores the current window bounds to a <code>PersistentOptions</code>
    * object. Position of the window may be interpreted as absolute or
    * relative to its parent window.
    * 
    * @param options <code>PersistentOptions</code> persistent storage place 
    * @param token name of bounds record in options
    * @param relative if <b>true</b> bounds are seen in relation to parent window
    *        otherwise absolute 
    */
   public void storeBounds ( PersistentOptions options, String token, boolean relative )
   {
      Rectangle bounds, parentBounds;
      
      if ( token == null || token.isEmpty() ) {
         throw new IllegalArgumentException("token is undefined");
      }
      bounds = getBounds();
      if ( relative )
      {
         parentBounds = getParent().getBounds();
         bounds.translate( -parentBounds.x, -parentBounds.y );
      }
      options.setBounds( token, bounds );
   }
/*   
      public Rectangle getBounds ()
      {
         Rectangle b = super.getBounds();
   //      System.out.println( "-- dialog bounds = (" + b.x + ", " + b.y + ", " + b.width + ", " + b.height );
         return b;
      }
   */   
      /**
       * Sets the bounds of this window from a stored bounds record in a 
       * <code>PersistentOptions</code> object. Does nothing if the
       * specified bounds could not be found.
       *  
       * @param options <code>PersistentOptions</code> persistent storage place 
       * @param token name of bounds record in options
       * @param relative if <b>true</b> bounds are seen in relation to parent window
       *        otherwise absolute
       * @return <b>true</b> if and only if the bounds were set  
       */
      public boolean gainBounds ( PersistentOptions options, String token, boolean relative )
      {
         Rectangle bounds, parentBounds;
         
         bounds = options.getBounds( token );
         if ( bounds != null )
         {
            if ( relative )
            {
               parentBounds = getParent().getBounds();
               bounds.translate( parentBounds.x, parentBounds.y );
            }
            setCorrectedBounds( bounds );
            return true;
         }
         return false;
      }

   /*   
   public Rectangle getBounds ()
   {
      Rectangle b = super.getBounds();
//      System.out.println( "-- dialog bounds = (" + b.x + ", " + b.y + ", " + b.width + ", " + b.height );
      return b;
   }
*/   
   /**
    * Sets the location of this window from a stored bounds record in a 
    * <code>PersistentOptions</code> object. Does nothing if bound info
    * is not found.
    *  
    * @param options <code>PersistentOptions</code> persistent storage place 
    * @param token name of bounds record in options
    * @param relative if <b>true</b> bounds are seen in relation to parent window
    *        otherwise absolute 
    */
   public void gainLocation ( PersistentOptions options, String token, boolean relative )
   {
      Rectangle bounds, parentBounds;
      
      bounds = options.getBounds( token );
      if ( bounds != null )
      {
         if ( relative )
         {
            parentBounds = getParent().getBounds();
            bounds.translate( parentBounds.x, parentBounds.y );
         }
         setCorrectedLocation( bounds.getLocation() );
      }
   }
   
   /**
    * Whether this dialog was terminated by a (validated) user pressed OK 
    * or CLOSE button.
    * 
    * @return <b>true</b> if and only if "OK" or "CLOSE" was pressed
    *         and termination of the dialog was validated 
    */
   public boolean isOkPressed()
   {
      return buttonbar == null ? false : buttonbar.isOkValidated();
   }
   
   /**
    * Whether this dialog was terminated by a user pressed NO button.
    * (This value can only return <b>true</b> if the dialog was initiated
    * to contain the "NO" button by a corresponding dialog type parameter.)
    * 
    * @return <b>true</b> if and only if "No" was pressed
    */
   public boolean isNoPressed()
   {
      return buttonbar == null ? false : buttonbar.isNoPressed();
   }
   
   /**
    * Whether this dialog was terminated by a user pressed CANCEL button.
    * 
    * @return <b>true</b> if and only if "CANCEL" was pressed 
    */
   public boolean isCancelPressed()
   {
      return buttonbar == null ? false : buttonbar.isCancelPressed();
   }
   
   /**
    *  Whether there was no user option pressed, leading to termination
    *  of the dialog. This may be interesting
    *  when a dialog has been closed other than by user handling.
    * 
    *  @return <b>true</b> if and only if neither Ok/Close nor Cancel has 
    *          been pressed at the time when this method is invoked
    */
   public boolean isUnselected()
   {
      return !isOkPressed() & !isCancelPressed();
   }

   public boolean isSynchronous ()
   {
      return buttonbar == null ? true : buttonbar.isSynchronous();
   }
   
   @SuppressWarnings("deprecation")
   @Override
   public void hide ()
   {
      Log.log( 10, "(ButtonBarDialog.hide) enter HIDE in dialog \"" + getTitle() + 
            "\"; modal == " + isModal() );
      super.hide();
   }

   @SuppressWarnings("deprecation")
   public void show ()
   {
      Log.log( 10, "(ButtonBarDialog.show) enter SHOW in dialog \"" + getTitle() + 
            "\"; modal == " + isModal() + ", remove == " + removeOnDisplay );

      if ( removeOnDisplay )
      {
         return;
      }
      
      // attempt make all child windows showing
      if ( childWindows != null )
         ShowDialogRunnable.startDialogsLater( childWindows );
               
      super.show();
      Log.log( 10, "(ButtonBarDialog.show) leaving SHOW in dialog \"" + getTitle() + 
            "\"; modal == " + isModal() );
   }

   public void setVisible ( boolean v )
   {
      Log.log( 10, "(ButtonBarDialog.setVisible) enter SET VISIBLE in dialog \"" + getTitle() + 
            "\", switch = " + v );
      super.setVisible( v );
   }
   
   /** Sets whether actions triggered on the systemic button bar 
    * will run on the Event Dispatching Thread (EDT) (v == true) 
    * or on a separate threads (v == false). By default this value
    * is <b>true</b>.
    * 
    * @param v boolean 
    */
   public void setSynchronous ( boolean v ) {
      if ( buttonbar != null )
         buttonbar.setSynchronous( v );
   }
   
   public void setCorrectedBounds ( Rectangle r )
   {
      super.setBounds( Util.correctedWindowBounds( r, isResizable(), clipping ) );
   //   setBounds(r.x, r.y, r.width, r.height);
   }
   
   public void setCorrectedLocation( Point p )
   {

      Rectangle r = getBounds();
      r.setLocation( p );
      setCorrectedBounds( r );
   }
/*
   public void setBounds (int x, int y, int width, int height) 
   {
      Rectangle r = Util.correctedWindowBounds( new Rectangle(x,y,width,height), 
                         isResizable(), clipping );
      super.setBounds( r.x, r.y, r.width, r.height );
   }

/*   
   public void setLocation ( Point p )
   {
      super.setLocation( p );
   }
*/
   /**
    * Tests if there is another dialog active (global) with the 
    * parameter name. If negative, the given name is registered 
    * as active singleton dialog (global). 
    * 
    * @param name (singleton) dialog name
    * @return <b>true</b> if and only if the given name has been registered
    *         as singleton dialog with this operation;
    *         <b>false</b> means there is another dialog already active with 
    *         this name
    */
   public boolean markSingleton ( String name )
   {
      if ( Global.isDialogActive( name ) )
         return false;

      // register new singleton dialog as active
      Global.setDialogActive( name, true );
      singletonName = name;
      return true;
   }

   /** Class internal implementation. */
   public void processWindowEvent( WindowEvent e )
   {
      Component button;
      
      switch ( e.getID() )
      {
      case WindowEvent.WINDOW_CLOSING :
         if ( buttonbar != null )
         {
            if ( (button = buttonbar.getCancelButton()) != null ||
                 (button = buttonbar.getNoButton()) != null ||
                 (button = buttonbar.getOkButton()) != null )
            {
               buttonbar.performButton( button );
            }
            return;
         }
      }
      super.processWindowEvent( e );
   }  // processWindowEvent
   
   /** Class listening to key events which were registered at the root pane. 
    *  Currently only ESCAPE-key. */
   private class KListener implements ActionListener
   {
      public void actionPerformed ( ActionEvent e )
      {
         if ( closeByEscape )
         {
//            Log.debug( 0, "-- ACTION-key pressed: " );
            processWindowEvent( new WindowEvent(
               ButtonBarDialog.this, WindowEvent.WINDOW_CLOSING ));
         }
      }
   }
   
   private class CListener implements ComponentListener
   {

      @Override
      public void componentResized ( ComponentEvent e )
      {
      }

      @Override
      public void componentMoved ( ComponentEvent e )
      {
      }

      @Override
      public void componentShown ( ComponentEvent e )
      {
         Log.log( 10, "(ButtonBarDialog.CListener.componentShown) component event " );
      }

      @Override
      public void componentHidden ( ComponentEvent e )
      {
         Log.log( 10, "(ButtonBarDialog.CListener.componentHidden) component event " );
         Runnable run = new Runnable () 
         {
            public void run ()
            {
               // dispose invisible dialogs which don't carry the AUTONOMOUS feature
               if ( !isAutonomous )
               {
                  dispose();
                  removeOnDisplay = true;
               }
            }
         };
         ActionHandler.startTaskDelayed( run, 300 );
      }
   
   }
}
