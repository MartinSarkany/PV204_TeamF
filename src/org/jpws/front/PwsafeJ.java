/*
 *  PwsafeJ in org.jpws.front
 *  file: PwsafeJ.java
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jpws.data.IOManager;
import org.jpws.data.Options;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.ShowDialogRunnable;
import org.jpws.front.util.Util;
import org.jpws.pwslib.global.Log;

/**
 * The "mainframe" of this application. Holds the static "main()" function
 * (which only branches into Global class for startup routines).
 * 
 * @author Wolfgang Keller
 */
@SuppressWarnings("serial")
public class PwsafeJ extends JFrame 
{
   private ArrayList<Dialog> childWindows = new ArrayList<Dialog>();
   private WindowEar childListener = new WindowEar();
   
   private StatusBar 	   statusBar;
   private JPanel          centerPanel;
   private String          winTitleTrunk;
   
   /**
	 * 
	 * @param args
	 */
	public static void main( String [] args ) {
      // initialize backend systems 
      Global.init( args );

      // run startup batch
      Global.autorun();
	}  // main

	/**
	 * @throws java.awt.HeadlessException
	 */
	public PwsafeJ() throws HeadlessException {
		super();

      Rectangle bounds;
      Dimension dim;
      Point framePos;

      // window decoration
      winTitleTrunk = Global.APPLICATION_TITLE;
      setTitle( winTitleTrunk );
      setIconImage( ResourceLoader.getImageIcon( "pwsafe-logo" ).getImage() );
      setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );

      // create STATUSBAR and MENUBAR
      statusBar = new StatusBar();
      setJMenuBar( MenuHandler.getMenuBar() );

      centerPanel = new JPanel( new BorderLayout() );
      getContentPane().add( ToolbarHandler.getToolBar(), BorderLayout.NORTH );
      getContentPane().add( centerPanel, BorderLayout.CENTER );
      getContentPane().add( statusBar, BorderLayout.SOUTH );
      setScreenView(null);

      pack();

      // get mainframe bounds from memory or create default bounds
      if ( Options.isOptionSet( "rememberScreen" ) &&
           (bounds = Options.getBounds( "mainframe" )) != null ) {
         ;
	  } else {
         // default bounds in the centre of the screen
         dim = new Dimension( 480, 300 ) ;
         framePos = Util.centredWindowLocation( new Rectangle(getToolkit().getScreenSize()), dim);
         bounds = new Rectangle( framePos, dim );
      }
      
      // activate mainframe bounds including corrections related to maximum screen size
      setBounds( Util.correctedWindowBounds( bounds, true, false ) );
	}

   /**
    * Puts a visible component into the centre panel of the main fame
    * display. If the parameter is <b>null</b> a default screen view 
    * (empty desktop) is shown.
    * 
    * @param panel <code>Component</code> to be shown; may be <b>null</b>
    */ 
   public void setScreenView( Component panel ) {
	  executeEDT_Action(EDT_Actions.SET_SCREENVIEW, panel);
   }
   
   @Override
   public void setTitle ( final String title ) {
//	  Log.log(10, "(PwsafeJ.setTitle) setting to TITLE ".concat(title));
	  executeEDT_Action(EDT_Actions.SET_TITLE, title);
   }
   
   private void setTitleIntern ( String title ) {
//	  Log.log(10, "(PwsafeJ.setTitleIntern) --> setting to TITLE ".concat(title));
	  super.setTitle(title);
   }
   
   private void executeEDT_Action(int cmd, Object par) {
	  try {
		  if (cmd == EDT_Actions.SET_TITLE | cmd == EDT_Actions.SHOW_IO_MONITOR) {
			 ActionHandler.executeOnEDT(new EDT_Actions(cmd, par));
		  } else {
		     ActionHandler.executeOnEDT_Wait(new EDT_Actions(cmd, par));
		  }
	   } catch (InvocationTargetException e ) {
		  e.printStackTrace();
	   } catch (InterruptedException e) {
		  e.printStackTrace();
	   }
   }
   
   /**
    * Updates main frame title line and status bar with useful information
    * about the given (currently active) file container (database).
    * Use <b>null</b> to purge.
    * 
    * @param file <code>PwsFileContainer</code>, may be <b>null</b>
    */
   public void setTitleFile ( PwsFileContainer file ) {
      String hstr, encoding;
      
      if ( file != null ) {
         // display database infos 
         setTitle( winTitleTrunk + " - " + file.getTitle() );
         
         // set status-bar format info
         encoding = file.getCharset();
         if ( !encoding.equalsIgnoreCase( "utf-8" ) ) {
            hstr = file.getFileFormatText() + " - " + encoding;
         } else {
            hstr = file.getFileFormatText();
         }
         statusBar.setFormatCell( hstr );
         
         // set status-bar record count info
         statusBar.setRecordCounterCell( String.valueOf( file.getRecordCount() ));

      } else {
         // "void of database" appearance 
         setTitle( winTitleTrunk );
         statusBar.setFormatCell( null );
         statusBar.setRecordCounterCell( null );
      }
   }
   
   /**
    * Neutralises the main frame display (eliminates all
    * specific content and resets to void centre display).
    */
   public void resetDisplay() {
      setTitle( winTitleTrunk );
      statusBar.setFormatCell( null );
      statusBar.setRecordCounterCell( null );
      setScreenView( null );
   }

   public StatusBar getStatusBar () {
      return statusBar;
   }
   
   public void exit () {
	  executeEDT_Action(EDT_Actions.EXIT, null);
   }
   
   @Override
   public void show () {
	  executeEDT_Action(EDT_Actions.SHOW, null);
   }  // show
   
   /** Adds the parameter dialog to the child list of this top level
    * application window (Frame). Does nothing if the parameter is 
    * <b>null</b> or already contained in the child list.
    * 
    * @param dlg <code>Dialog</code> active dialog to be remembered
    * @since 0-5-0
    */
   public void addChildWindow ( Dialog dlg ) {
      synchronized ( childWindows ) {
         if ( dlg != null & !childWindows.contains( dlg ) ) {
            childWindows.add( dlg );
            dlg.addWindowListener( childListener );
            Log.log( 8, "(PwsafeJ.addChildDialog) added child dialog: " + dlg.getTitle() );
            Log.debug( 8, "*** Mainframe Children Status (C) = " + childWindows.size() );
         }
      }
   }
   
   /** Removes the parameter dialog from the list of registered child dialogs
    * of this top level application window (Frame).
    * 
    * @param dlg <code>Dialog</code>
    * @return boolean <b>true</b> if and only if the dialog was contained in the list
    */
   public boolean removeChildWindow ( Dialog dlg ) {
      synchronized ( childWindows ) {
         boolean ok = childWindows.remove( dlg );
         if ( ok ) {
            dlg.removeWindowListener( childListener );
            Log.log( 8, "(PwsafeJ.removeChildWindow) removed child dialog: " + dlg.getTitle() );
            Log.debug( 8, "*** Mainframe Children Status (C) = " + childWindows.size() );
         }
         return ok;
      }
   }
   
   /** Class internal. 
    * @since 0-5-0
    * */
   @Override
   public void processWindowEvent( WindowEvent e ) {
//      System.out.println( "-- Mainframe Window Event: " + e.getID() );
      switch ( e.getID() ) {
      case WindowEvent.WINDOW_CLOSED :
//         System.out.println( "*** Mainframe Window Closed ***" );
         break;
         
      case WindowEvent.WINDOW_CLOSING :
         Log.log( 5, "(PwsafeJ.processWindowEvent) -- received MAIN FRAME CLOSING command" ); 
         Global.exit();
         return;

      case WindowEvent.WINDOW_ICONIFIED :
         Log.log( 8, "(PwsafeJ.processWindowEvent) -- received WINDOW_ICONIFIED on MainFrame" );
         Log.log( 8, "(PwsafeJ.processWindowEvent) -- Window State == ".concat( windowStateInfo( getState() )));
         if ( !SystemDesktopHandler.get().isSwitching() )
            Global.setIconified( true );
         break;

      case WindowEvent.WINDOW_DEICONIFIED :
//       System.out.println( "*** Mainframe Window Iconified ***" );
       Log.log( 8, "(PwsafeJ.processWindowEvent) -- received WINDOW_DEICONIFIED on MainFrame" );
       Log.log( 8, "(PwsafeJ.processWindowEvent) -- Window State == ".concat( windowStateInfo( getState() )));
       if ( !SystemDesktopHandler.get().isSwitching() )
          Global.setIconified( false );
       break;
      }
      
      super.processWindowEvent( e );
   }  // processWindowEvent

   
   private String windowStateInfo ( int state ) {
      String res;
      if ( state == Frame.ICONIFIED )
         res = "ICONIFIED";
      else if ( state == Frame.NORMAL )
         res = "NORMAL";
      else
         res = "-- unrecognised -- " + state;
      return res;
   }
   
   /** Creates a dialog to display a periscope into hidden system values
    * for the purpose of monitoring and debugging.
    * 
    * @return ButtonBarDialog
    */
   private ButtonBarDialog createMonitorWindow () {
	   ButtonBarDialog dlg = new ButtonBarDialog(this, DialogButtonBar.OK_BUTTON, false);
//	   Log.log(7, "(PwsafeJ.createMonitorWindow) created buttonbardialog");
	   dlg.setTitle("IOManager Periscope");
	   JPanel content = IOManager.getMonitorPanel();
	   dlg.setDialogPanel(content);
	   dlg.setResizable(true);
	   dlg.pack();
	   return dlg;
   }
   
   static ButtonBarDialog monDlg; 
   
   public void showMonitorDialog (boolean show) {
	  executeEDT_Action(EDT_Actions.SHOW_IO_MONITOR, new Boolean(show));
   }


   //  ************  INNER CLASSES  *************

   private class EDT_Actions implements Runnable {
	   public static final int SHOW = 1;
	   public static final int SHOW_IO_MONITOR = 2;
	   public static final int SET_TITLE = 3;
	   public static final int SET_SCREENVIEW = 4;
	   public static final int EXIT = 5;
	   
	   int command;
	   Object param1;
	   
	   public EDT_Actions (int cmd, Object param) {
		   command = cmd;
		   param1 = param;
	   }

	   @Override
	   public void run() {
		  switch ( command ) {
		  case SHOW:
			  show();
			  break;
		  case EXIT:
			  exit();
			  break;
		  case SHOW_IO_MONITOR:
			  showMonitorDialog( (Boolean)param1 );
			  break;
		  case SET_TITLE:
	          setTitleIntern( (String)param1 );
			  break;
		  case SET_SCREENVIEW:
			  setScreenView((JComponent)param1);
			  break;
		  }
	   }

	   public void showMonitorDialog (boolean show) {
		   Log.log(7, "(PwsafeJ.showMonitorDialog) showing == " + show);
		   if ( show ) {
			   if ( monDlg == null || !monDlg.isDisplayable() ) {
				  monDlg = createMonitorWindow();
				  monDlg.gainBounds(Options.getOptions(), "monitorDialog", true);
				  monDlg.setVisible(true);
			   }
		   }
		   else if ( monDlg != null ) {
			   monDlg.storeBounds(Options.getOptions(), "monitorDialog", true);
			   monDlg.setVisible(false);
			   monDlg.dispose();
			   monDlg = null;
		   }
	   }

	// since 0-5-0
	   @SuppressWarnings({ "deprecation", "unchecked" })
	   public void show () {
	      ArrayList<Dialog> wins;
	   
	      // the following ensures display of registered child windows
	      // which sometimes is "forgotten" by the Swing component
	      synchronized( childWindows ) {
	         wins = (ArrayList<Dialog>)childWindows.clone();
	      }
	      ShowDialogRunnable.startDialogsLater( wins );
	
	      // show main window
	      PwsafeJ.super.show();
	   }  // show

	   /**
	    * Puts a visible component into the centre panel of the main fame
	    * display. If the parameter is <b>null</b> a default screen view 
	    * (empty desktop) is shown.
	    * 
	    * @param panel <code>Component</code> to be shown; may be <b>null</b>
	    */ 
	   public void setScreenView( JComponent panel ) {
          Log.log( 10, "(PwsafeJ.setScreenView) setting screen view to " + panel );
          if ( panel == null ) {
             panel = DisplayManager.createEmptyScreen();
          }
          centerPanel.removeAll();
          centerPanel.add( panel, BorderLayout.CENTER );
          centerPanel.validate();
          centerPanel.repaint();
	   }

	   public void exit () {
	      Options.setBounds( "mainframe", getBounds() );
		  showMonitorDialog(false);
	      statusBar.shutdown();
	      setVisible(false);
	      dispose();
	      Log.log( 5, "(PwsafeJ.exit) # - MAIN FRAME disposed" ); 
	   }
   } // class EDT_Actions
   
   /**
    * This WindowEar listens to elements in this mainframe's child window list
    * and removes them when they have been closed.
    * 
    * @since 0-5-0
    */
   private class WindowEar extends WindowAdapter {
      @Override
	  public void windowClosed( WindowEvent evt ) {
         Dialog dlg = (Dialog)evt.getWindow();
         Log.log( 8, "(PwsafeJ.WindowEar.windowClosed) -- received WINDOW-CLOSED for " + dlg.getTitle() );

         removeChildWindow( dlg );
      }
   }
   
//   /**
//    * Panel for a blank screen. Might get more decorated in the future.
//    */
//   private class ScreenPanel extends JPanel {
      
//=======
//   /** Creates a dialog to display a periscope into hidden system values
//    * for the purpose of monitoring and debugging.
//    * 
//    * @return ButtonBarDialog
//    */
//   private static ButtonBarDialog createMonitorWindow () {
//	   ButtonBarDialog dlg = new ButtonBarDialog();
//	   dlg.setTitle("IOManager Periscope");
//	   JPanel content = IOManager.getMonitorPanel();
//	   dlg.setDialogPanel(content);
//	   dlg.setResizable(true);
//	   dlg.pack();
//	   return dlg;
//   }
//   
//   static JDialog monDlg; 
//   
//   public void showMonitorDialog (boolean show) {
//	   if ( show && monDlg == null ) {
//		   monDlg = createMonitorWindow();
//		   Rectangle bounds = Options.getBounds("monitorDialog");
//		   if ( bounds != null ) {
//			   monDlg.setBounds(bounds);
//		   }
//		   monDlg.setVisible(true);
//	   }
//	   if ( !show && monDlg != null ) {
//		   Options.setBounds("monitorDialog", monDlg.getBounds());
//		   monDlg.setVisible(false);
//		   monDlg.dispose();
//		   monDlg = null;
//	   }
//   }

}
