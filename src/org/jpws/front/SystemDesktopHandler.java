/*
 *  SystemDesktopHandler in org.jpws.front
 *  file: SystemDesktopHandler.java
 * 
 *  Project jpws-prg
 *  @author Wolfgang Keller
 *  Created 02.07.2012
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

package org.jpws.front;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Future;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jpws.data.Options;
import org.jpws.front.util.PushSemaphor;
import org.jpws.front.util.ResourceLoader;
import org.jpws.pwslib.global.Log;

public class SystemDesktopHandler
{
   private static SystemDesktopHandler instance;
   
   /** Time during which no reaction occurs to ICONIFY/DEICONIFY events from a Frame. */ 
   private static final int RESET_DELAY_TIME = 750; 

   /** Returns the singleton instance of class <code>SystemDesktopHandler</code>. */
   public static SystemDesktopHandler get ()
   {
      if ( instance == null )
         new SystemDesktopHandler();
      return instance;
   }

   private PushSemaphor         semaphor = new PushSemaphor();
   private JPWSIdleFrame        idleFrame;
   private TrayActionListener   actionListener = new TrayActionListener();
   private SystemTray           systemTray;
   private TrayIcon             trayIcon;
   private Future<?>        	resetSwitching;

   private boolean      isIconSet;
   private boolean      isIconified;
   private Menu         recentUsedMenu;
   private MenuItem     addRecordMenuItem;
   private MenuItem     saveAllMenuItem;
   private MenuItem     goIdleMenuItem;
   private MenuItem     minimizeMenuItem;
   
   
   private SystemDesktopHandler ()
   {
      instance = this;
      init();
   }
   
   private void init ()
   {
      if ( SystemTray.isSupported() )
      try {
         systemTray = SystemTray.getSystemTray();
         System.out.println( "# System Tray is supported" );
      }
      catch ( Exception e )
      {
         System.out.println( "# System Tray is UNSUPPORTED!" );
         System.out.println( e.toString() );
      }

      // some changeable menu items of the popup menu
      recentUsedMenu = new Menu( ResourceLoader.getCommand( "menu.edit.lastused" ) );
      recentUsedMenu.setEnabled( DisplayManager.hasSelectedFile() );
      
      goIdleMenuItem = new MenuItem( ResourceLoader.getCommand( "menu.view.idle" ) );
      goIdleMenuItem.setActionCommand( "menu.goidle" );
      goIdleMenuItem.addActionListener( actionListener );
      goIdleMenuItem.setEnabled( DisplayManager.hasOpenFiles() );
      
      minimizeMenuItem = new MenuItem( ResourceLoader.getCommand( "menu.tray.minimize" ) );
      minimizeMenuItem.setActionCommand( "menu.minimize" );
      minimizeMenuItem.addActionListener( actionListener );

      addRecordMenuItem = new MenuItem( ResourceLoader.getCommand( "menu.edit.psw.add" ) );
      addRecordMenuItem.setActionCommand( "menu.edit.psw.add" );
      addRecordMenuItem.addActionListener( ActionHandler.getMainActionListener() );
      addRecordMenuItem.setEnabled( DisplayManager.getSelectedContainer() != null );
      
      saveAllMenuItem = new MenuItem( ResourceLoader.getCommand( "menu.file.saveall" ) );
      saveAllMenuItem.setActionCommand( "menu.file.saveall" );
      saveAllMenuItem.addActionListener( ActionHandler.getMainActionListener() );
      saveAllMenuItem.setEnabled( DisplayManager.hasModifiedFiles() );
      
      // we listen here to update enabled status of the "go idle" command
      DisplayManager.addChangeListener( actionListener );
      
      // install tray icon if says so in Options
      setTrayActive( Options.isOptionSet( "minToTrayIcon" ) );
   }
   
   public void exit ()
   {
      // exit idleFrame 
      if ( idleFrame != null )
      {
         System.out.println( "# disposing IDLE FRAME" );
         idleFrame.dispose();
         idleFrame = null;
      }
   }
   
   /** Whether there is an icon representation of this
    * application in the system tray.
    * 
    * @return boolean <b>true</b> == tray active
    */
   public boolean isTrayActive ()
   {
      return isIconSet;
   }
   
   /** Whether this application's main frame is in an ICONIFIED state.
    * 
    * @return boolean <b>true</b> == iconified
    */
   public boolean isIconified ()
   {
      return isIconified;
   }
   
   private PopupMenu getPopupMenu ()
   {
      PopupMenu popup = new PopupMenu();
      MenuItem item;

//      if ( Options.isOptionSet( "storeMinorChanges" ) )
         popup.add( recentUsedMenu );
      
      // add record (editor) command
      popup.add( addRecordMenuItem );
      
      // Save All command
      popup.add( saveAllMenuItem );
      popup.addSeparator();
      
      // Idle/Activate command
      popup.add( goIdleMenuItem );

      // Minimise/Restore command
      popup.add(  minimizeMenuItem );
      
      // Program exit command
      item = new MenuItem( ResourceLoader.getCommand( "menu.file.exit" ) );
      item.setActionCommand( "menu.exit" );
      item.addActionListener( actionListener );
      popup.add( item );

      return popup;
   }
   
   /** Sets up or closes down the system tray icon of
    * this application and associated behaviour features.
    * 
    * @param v boolean <b>true</b> == tray icon active
    */
   public void setTrayActive ( final boolean v )
   {
      Image iconImage;
      
      if ( systemTray == null )
         return;

      if ( v & !isIconSet )
      {
         // create a new trayIcon (including special action listener)
         iconImage = ResourceLoader.getImage( "pwsafe-logo" );
         trayIcon = new TrayIcon( iconImage, Global.APPLICATION_TITLE,
                    getPopupMenu() );
         trayIcon.setImageAutoSize( true );
         trayIcon.setActionCommand( "trayicon.clicked" );
         trayIcon.addActionListener( actionListener );
         
         // add the tray icon to the system icon tray
         try
         {
//            throw new AWTException("TEST");
            systemTray.add( trayIcon );
            isIconSet = true;
         }
         catch ( AWTException e )
         {
            // recall tray availability and listener
            Options.setOption( "minToTrayIcon", false );
            systemTray = null;
            DisplayManager.removeChangeListener( actionListener );
            
            e.printStackTrace();
            GUIService.failureMessage( "msg.failure.notrayservice", e );
         }
      }
      
      if ( !v & isIconSet )
      {
         Runnable run = new Runnable ()
         {
            public void run ()
            {
               systemTray.remove( trayIcon );
               isIconSet = false;
               Global.mainFrame.setVisible( true );
            }
         };
         ActionHandler.executeOnEDT( run );
      }
   }
   
   private class TrayActionListener implements ActionListener, ChangeListener
   {

      @Override
      public void actionPerformed ( ActionEvent e )
      {
         final String cmd = e.getActionCommand();
         Log.log( 8, "(SystemDesktopHandler.TrayActionListener.actionPerformed) -- action from TRAY ICON: " 
               + cmd );
         
         if ( cmd == null )
            return;
         
         Runnable run = new Runnable () 
         {
            public void run ()
            {
               if ( cmd.equals( "menu.exit" ) )
               {
                  Global.exit( isIconified() );
               }
               else if ( cmd.equals( "menu.minimize" ))
               {
                  setIconified( true );
               }
               else if ( cmd.equals( "menu.restore" ))
               {
                  setIconified( false );
               }
               else if ( cmd.equals( "menu.activate" ))
               {
                  switchIdleState( false );
               }
               else if ( cmd.equals( "menu.goidle" ))
               {
                  switchIdleState( true );
               }
               else if ( cmd.equals( "menu.removeicon" ))
               {
                  setTrayActive( false );
               }
               else if ( cmd.equals( "trayicon.clicked" ))
               {
                  if ( !isIconified() && Global.isEditingRecord() )
                     Global.getEditDialog().toFront();
                  else
                     setIconified( !isIconified() );
               }
            }
         };
         ActionHandler.startTask(run);
      }

      @Override
      public void stateChanged ( ChangeEvent e )
      {
         controlMenuItems();
      }
   }

   /** Controls the Enabled status of some menu items according
    * to system states.
    */
   private void controlMenuItems ()
   {
      PwsFileContainer ct = DisplayManager.getSelectedContainer();
      boolean hasOpenFiles = DisplayManager.hasOpenFiles();
      boolean containerAvailable = ct != null & !isIdleState();
      boolean withoutEditor = containerAvailable && !ct.isRecordEditing();
      
      recentUsedMenu.setEnabled( withoutEditor );
      if ( containerAvailable )
      {
         ct.getLastUsedList().updateMenu( recentUsedMenu );
      }
      else
      { 
         recentUsedMenu.removeAll();
      }
      addRecordMenuItem.setEnabled( containerAvailable );
      addRecordMenuItem.setLabel( ResourceLoader.getCommand( 
            withoutEditor ? "menu.edit.psw.add" : "menu.tray.tofront" ) );
      saveAllMenuItem.setEnabled( DisplayManager.hasModifiedFiles() & !isIdleState() );
      goIdleMenuItem.setEnabled( hasOpenFiles );
   }

   private void startSwitching () {
      // we identify the SWITCHING state by thread variable
      // "resetSwitching" being not null
      // the SWITCHING state is automatically reset to false
      // after a short time
      // following construction is safe against overrun problems
      
      Runnable reset = new Runnable() {
         public void run () {
            resetSwitching = null;
         }
      };
      if ( resetSwitching != null ) {
         resetSwitching.cancel(false);
      }
      resetSwitching = ActionHandler.startTaskDelayed( reset, RESET_DELAY_TIME );
   }
   
   /** Whether there is a Iconifying/De-iconifying switching process 
    * on the mainframe window set up.
    * 
    * @return boolean <b>true</b> == programmatical switching  
    */
   public boolean isSwitching ()
   {
      return resetSwitching != null;
   }
   
   /** Controls visibility of the two application Frames used in JPWS.
    * Switch progress runs in a separate thread in order to not block
    * other switching of the calling thread. Hence returns quickly!
    * This does not create or destroy any of the Frames.
    *  
    * @param main boolean visibility of Global.mainframe
    * @param idle boolean visibility of SystemDesktop.idleFrame
    */
   private synchronized void setFramesVisible ( final boolean main, final boolean idle )
   {
      Runnable r = new Runnable ()
      {
         @Override
         public void run ()
         {
            startSwitching();
            
            Log.log( 10, "(SystemDesktopHandler.setFramesVisible.run) performing VISIBILITY switches" );
            
            // first the negative switches
            if ( !idle & idleFrame != null )
            {
               Log.log( 10, "(SystemDesktopHandler.setFramesVisible.run) setting IDLE-FRAME invisible" );
               idleFrame.setVisible( false );
            }
            if ( !main & Global.mainFrame != null )
            {
               Log.log( 10, "(SystemDesktopHandler.setFramesVisible.run) setting MAINFRAME invisible" );
               Global.mainFrame.setVisible( false );
            }
            
            // then perhaps the positive switches
            if ( idle & idleFrame != null )
            {
               Log.log( 10, "(SystemDesktopHandler.setFramesVisible.run) setting IDLE-FRAME visible" );
               idleFrame.setVisible( true );
            }
            if ( main & Global.mainFrame != null )
            {
               Log.log( 10, "(SystemDesktopHandler.setFramesVisible.run) setting MAINFRAME visible" );
               Global.mainFrame.setVisible( true );
               Global.mainFrame.toFront();
            }

            Log.log( 10, "(SystemDesktopHandler.setFramesVisible.run) leaving VISIBILITY switches" );
         }
      };
      // start smartly
      SwingUtilities.invokeLater( r );
//      ActionHandler.startTask( r );
//      ActionHandler.executeOnEDT( r );
   }
   
   public synchronized void setIconified ( final boolean v )
   {
      Log.log( 10, "(SystemDesktopHandler.setIconified) enter setIconified" );
      
      Runnable run = new Runnable()
      {
         public void run ()
         {
            Log.log( 10, "(SystemDesktopHandler.setIconified.run) start" );

            // RESTORE branch
            if ( !v )
            {
               ActionHandler.resetIdleTime();
               if ( !isIdleState() )
               {
                  startSwitching();
                  
                  // de-iconify when not in IDLE state
                  Log.log( 10, "(SystemDesktopHandler.setIconified.run) setting MAINFRAME state to NORMAL" );
                  Global.mainFrame.setState( JFrame.NORMAL );
                  isIconified = false;
                  setFramesVisible( true, false );
                  minimizeMenuItem.setLabel( ResourceLoader.getCommand( "menu.tray.minimize" ) );
                  minimizeMenuItem.setActionCommand( "menu.minimize" );
               }
               else
               {
                  // de-iconify when in IDLE state
                  switchIdleState( false );
               }
            }
      
            // ICONIFY branch
            else 
            {
               if ( !isIdleState() && DisplayManager.hasOpenFiles() 
                    && Options.isOptionSet( "lockMinimize" ))
                  switchIdleState( true );
               else
               {
                  startSwitching();
                  
                  // iconify (both states)
                  Log.log( 10, "(SystemDesktopHandler.setIconified.run) setting MAINFRAME state to ICONIFIED" );
                  Global.mainFrame.setState( JFrame.ICONIFIED );
                  isIconified = true;
                  boolean mainVisible = !isTrayActive() & !isIdleState();
                  boolean idleVisible = !isTrayActive();
                  setFramesVisible( mainVisible, idleVisible );
                  minimizeMenuItem.setLabel( ResourceLoader.getCommand( "menu.manage.restore" ) );
                  minimizeMenuItem.setActionCommand( "menu.restore" );
               }
            }
            
            minimizeMenuItem.setEnabled( !isIdleState() );
            Log.log( 10, "(SystemDesktopHandler.setIconified.run) leave" );
         }
      };
      SwingUtilities.invokeLater( run );
      Log.log( 10, "(SystemDesktopHandler.setIconified) leave setIconified" );
   }

//   @SuppressWarnings("deprecation")
   public synchronized void switchIdleState ( final boolean idle )
   {
      Log.log( 10, "(SystemDesktopHandler.switchIdleState) enter switchIdleState with " + idle);
      if ( Global.mainFrame != null & DisplayManager.hasOpenFiles() )
      {
         ActionHandler.resetIdleTime();
         
         Runnable run = new Runnable()
         {
            public void run ()
            {
               // To IDLE
               if ( idle & !isIdleState() )
               {
//                  startSwitching();
                  
                  idleFrame = new JPWSIdleFrame();
                  Log.log( 10, "(SystemDesktopHandler.switchIdleState) TO IDLE: setting IDLE-FRAME visible" );
                  idleFrame.setVisible(true);
                  Log.log( 10, "(SystemDesktopHandler.switchIdleState) TO IDLE: setting IDLE-FRAME state == ICONIFIED" );
                  idleFrame.setState( JFrame.ICONIFIED );
                  setIconified( true );
                  goIdleMenuItem.setLabel( ResourceLoader.getCommand( "menu.tray.activate" ) );
                  goIdleMenuItem.setActionCommand( "menu.activate" );
                  controlMenuItems();
                  Log.log( 10, "(SystemDesktopHandler.switchIdleState) finished TO IDLE" );
               }
      
               // to NOT-IDLE
               else if ( !idle & isIdleState() )
               {
                  PwsFileContainer fc;
                  boolean checkin;
      
                  Log.log( 8, "(SystemDesktopHandler.switchIdleState.run) setting to IDLE==FALSE" );
                  Log.log( 8, "(SystemDesktopHandler.switchIdleState.run) unlock Thread == " + Thread.currentThread() );
                  // we need this semaphor because we don't want multiple threads, e.g. triggered by 
                  // the system tray, wait and execute late on the same task
                  if ( semaphor.isOpen() )
                  {
                     semaphor.push();
//                     startSwitching();
                     
                     // request password from user (most recent used database)
                     Log.log( 10, "(SystemDesktopHandler.switchIdleState.run) setting IDLE-FRAME invisible" );
 //                    idleFrame.setVisible( false );
                     fc = DisplayManager.getFirstContainer();
                     checkin = fc == null || GUIService.passwordControl( fc, true );
                     
                     if ( checkin )
                     {
                        Log.log( 10, "(SystemDesktopHandler.switchIdleState.run) disposing IDLE-FRAME" );
                        idleFrame.dispose();
                        idleFrame = null;
                        setIconified( false );
                        goIdleMenuItem.setLabel( ResourceLoader.getCommand( "menu.view.idle" ) );
                        goIdleMenuItem.setActionCommand( "menu.goidle" );
                        controlMenuItems();
                     }
                     else
                     {
                        Log.log( 10, "(SystemDesktopHandler.switchIdleState.run) setting IDLE-FRAME visible" );
                        if ( idleFrame.getState() != JFrame.ICONIFIED )
                           idleFrame.setState( JFrame.ICONIFIED );
                        setFramesVisible( Global.mainFrame.isVisible(), !isTrayActive() );
                     }
                     
                     semaphor.pop();
                  }
                  
                  else if ( idleFrame.isVisible() )
                     idleFrame.toFront();

                  Log.log( 8, "(SystemDesktopHandler.switchIdleState.run) terminate setting to IDLE==FALSE" );
               }
            }
         };
         ActionHandler.executeOnEDT( run );
      }
      Log.log( 10, "(SystemDesktopHandler.switchIdleState) leave switchIdleState  (" + idle + ")");
   }  // switchIdleState

   /** returns the Idle Frame or <b>null</b> if IDLE
    * is not active.
    * 
    * @return <code>JPWSIdleFrame</code> or <b>null</b>
    */
   public JPWSIdleFrame getIdleFrame ()
   {
      return idleFrame;
   }

   public boolean isIdleState ()
   {
      return idleFrame != null;
   }
}
