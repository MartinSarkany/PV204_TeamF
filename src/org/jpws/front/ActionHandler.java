/*
 *  ActionHandler in org.jpws.front
 *  file: ActionHandler.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 06.09.2004
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

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.jpws.data.Options;
import org.jpws.front.util.ButtonBarListener;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.HtmlBrowserDialog;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.exception.NoSuchRecordException;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.UUID;
import org.jpws.pwslib.order.DefaultRecordWrapper;

/**
 *  Static singleton class organising actions, mainly depending on the command 
 *  menus, and action related assisting routines. This class manages the system's
 *  idle time counter. 
 *  
 *  <p>
 * <p>
 */
public class ActionHandler
{
   private static ExecutorService threadPool;
   private static ScheduledExecutorService schedulethreadPool;
   private static ActionListener actionListener = new MainActionListener();
   private static ObjectChangeListener changableObjListener = new ObjectChangeListener(null);
   private static long lastUseTime;
   
   // special Actions
   private static Action clearClipAction;
   private static Action saveAllAction;
   private static Action closeAllAction;
   private static Action deleteAction;
   private static Action editEntryAction;
   
   /**
    *  Singleton class. 
    */
   private ActionHandler () {
   }

   public static void init () {
	  threadPool = new ThreadPoolExecutor(5, 10, 180, TimeUnit.SECONDS,
			  new LinkedBlockingQueue<Runnable>()); 
	  schedulethreadPool = new ScheduledThreadPoolExecutor(3);
      lastUseTime = System.currentTimeMillis();
      actionInit();
   }  // init
   
   public static void exit () {
	   threadPool.shutdown();
	   schedulethreadPool.shutdownNow();
   }
   
   private static void actionInit () {
      // DELETE Action (menu.edit.delete)
      deleteAction = new HandlerAction( 
            ResourceLoader.getCommand( "menu.edit.delete" ),
            "menu.edit.delete",
            ResourceLoader.getImageIcon( "delete" ), 
            ResourceLoader.getCommand( "toolbar.delete.tooltip" ) );
      
      // CLEARCLIP Action (menu.edit.clearclip)
      clearClipAction = new HandlerAction( 
            ResourceLoader.getCommand( "menu.edit.clearclip" ),
            "menu.edit.clearclip",
            ResourceLoader.getImageIcon( "clearclip" ),
            ResourceLoader.getCommand( "toolbar.clearclip.tooltip" ) );
      
      // SAVEALL Action (menu.file.saveall)
      saveAllAction = new HandlerAction( 
            ResourceLoader.getCommand( "menu.file.saveall" ),
            "menu.file.saveall",
            ResourceLoader.getImageIcon( "filesave-all" ), 
            ResourceLoader.getCommand( "toolbar.save-all.tooltip" ) );
      saveAllAction.setEnabled( false );
      
      // CLOSEALL Action (menu.file.closeall)
      closeAllAction = new HandlerAction( 
            ResourceLoader.getCommand( "menu.file.closeall" ),
            "menu.file.closeall",
            ResourceLoader.getImageIcon( "fileclose-all" ), 
            ResourceLoader.getCommand( "toolbar.close-all.tooltip" ) );
      closeAllAction.setEnabled( false );
      
      // EDIT ENTRY action
      editEntryAction = new HandlerAction( 
    		  ResourceLoader.getCommand("menu.edit.psw.edit"),
    		  "menu.edit.psw.edit" );
   }
   
   /** Returns the <code>ActionListener</code> which performs action commands
    *  defined in this class. 
    */
   public static ActionListener getMainActionListener () {
      return  actionListener;
   }

   /** Returns the current application idle time in milliseconds. 
    *  
    * @return long time value in milliseconds
    */
   public static long getIdleTime () {
      return System.currentTimeMillis() - lastUseTime;
   }

   /** Resets the application's idle counter to zero (current time).
    *  
    *  @return long current time in epoch milliseconds 
    */
   public static long resetIdleTime () {
      return lastUseTime = System.currentTimeMillis();
   }
   
   /** Throws an IllegalStateException if the caller is not an Event Dispatching
    * Thread.
    * 
    * @throws IllegalStateException
    */
   public static void checkForEDT () {
	   if ( !SwingUtilities.isEventDispatchThread() ) {
		  Log.log(5, "(ActionHandler.checkForEDT) --- EDT VIOLATION STATE ! ---");
		  throw new IllegalStateException("EDT required for this action!");
	   }
   }
   
   /** Throws an IllegalStateException if the caller is an Event Dispatching
    * Thread.
    * 
    * @throws IllegalStateException
    */
   public static void checkForThread () {
	   if ( SwingUtilities.isEventDispatchThread() )
		   throw new IllegalStateException("THREAD required for this action!");
   }
   
   /** Edits the application's global passphrase policy. 
    */ 
   static void editFilePWPolicy ( final PwsFileContainer fco ) {
      final PolicyDialog dlg;
      final PwsPassphrasePolicy oldPolicy, superPolicy;

	  checkForEDT();
	   
      // get policy definition from MAYOR file options
      superPolicy = Global.passphrasePolicy;
      oldPolicy = fco.getPassphrasePolicy();
      dlg = new PolicyDialog( Global.getActiveFrame(), oldPolicy, 3 );
      dlg.setAutonomous( true );
      ButtonBarListener listener = new DefaultButtonBarListener() {
         @Override
		 public boolean okButtonPerformed () {
            PwsPassphrasePolicy policy = dlg.getEditedPolicy();
            
            // save to options if modified
            if ( !policy.equals( oldPolicy ) && policy.isValid() ) {
            	String value = policy.equals( superPolicy ) ? null 
            			       : policy.getInternalForm();
            	fco.getMajorOptions().setOption( "passwordPolicy", value );
            }
            return true;
         }
      };
      dlg.addButtonBarListener( listener );
      dlg.show();
   }
   
   public static final int OP_SENDCLIPBOARD = 1;
   public static final int OP_PASSMODIFIED = 2;
   
   /** Issues a confirming message to the user about the performance of a 
    *  specific operation. (Currently only "OP_SENDCLIPBOARD".)
    * 
    * @param operation kind of operation performed
    * @param txt text or text token substituted for the "$action" variable
    *        of the message text; may be <b>null</b> 
    */
   public static void confirmOperation ( 
		              Component owner, int operation, String txt ) {
      String text, hstr;
      
      switch ( operation ) {
         case OP_SENDCLIPBOARD : 
            text = ResourceLoader.getDisplay( "confirm.clipsend" );
         break;
         default : text = "? confirm operation ? : $action";
      }

      hstr = txt == null ? "??" : ResourceLoader.codeOrRealDisplay( txt );
      text = Util.substituteText( text, "$action", hstr );
      GUIService.infoMessage( owner, "confirm.operation", text );
   }  // confirmOperation

   /** Update the "Clear Clipboard" menuitem according to content. */
   public static void clipboardUpdated() {
      boolean check = false;
      
      try {
         check = Global.clipboard.getContents(null).
         getTransferData(DataFlavor.stringFlavor) != null;
      } catch ( Exception e ) {
      }
      clearClipAction.setEnabled( check );
   }

   /** ActionHandler harbours a <code>ChangeListener</code> which resets
    *  the idle-time counter on text changes. This method adds changeable
    *  objects of the following types to this listener:
    *  <p><code>AbstractButton</code>, <code>JTabbedPane</code>
    *  <code>JComboBox</code>, <code>JTextComponent</code>
    *  <p>Registering does not need a counterpart. 
    */
   public static void registerChangeableObject( Object c ) {
      changableObjListener.registerChangeableObject( c );
   }
   
   /** Send the password string of the specified record to the clipboard.
    *  If the value is empty, the clipboard is cleared. This updates
    *  the ACCESSTIME field of the record to the current time (conditional).
    * 
    * @param owner <code>Component</code> the owner window or <b>null</b> 
    * @param record <code>PwsRecord</code>
    * @param update boolean if true the record's ACCESSTIME value will get
    *               updated
    * @return <b>true</b> if and only if a value was copied
    */
   public static boolean sendClipboardPassword ( Component owner, 
		   PwsRecord record, boolean update ) {
      PwsPassphrase password = record.getPassword();
      if ( password == null ) return false;
      
      Global.setClipboardText( password.getString() );
      boolean check = Global.hasClipboardTransfer(); 
      if ( check ) { 
         if ( update ) {
            record.setAccessTime( System.currentTimeMillis() );
         }
         GUIService.statusConfirm("msg.confirm.copyclippass", record.getTitle());
         if ( Options.isOptionSet("confirmCopyClipboard") ) {
            confirmOperation( owner, OP_SENDCLIPBOARD, "confirm.password" );
         }
      }
      return check;
   }
   
   /**
    * Sends a parameter text to the clipboard. Optionally a confirm message
    * can be defined (which is shown if the global option "confirmCopyClipboard"
    * is set true).
    * 
    * @param owner <code>Component</code> the owner window or <b>null</b> 
    * @param text String the text to be sent to clipboard
    * @param msg String text or token for naming the send object in confirm message;
    *        may be <b>null</b>
    */
   public static void sendClipboardText ( Component owner, String text, String msg )
   {
      if ( text != null && text.equals( "" ) )
         text = null;
      
      Global.clipboard.setContents( new StringSelection(text), null);
      ActionHandler.clipboardUpdated();

      if ( text != null &&  msg != null && 
           Options.isOptionSet("confirmCopyClipboard") )
         confirmOperation( owner, OP_SENDCLIPBOARD, msg );
   }
   
   /** Send the username string of the specified record to the clipboard.
    *  If the value is empty, the clipboard is cleared.
    * 
    * @param owner <code>Component</code> the owner window or <b>null</b> 
    * @param record <code>PwsRecord</code>
    */
   public static void sendClipboardUsername ( Component owner, PwsRecord record )
   {
      Global.setClipboardText( record.getUsername() );
      if ( Global.hasClipboardTransfer() )
      { 
         GUIService.statusConfirm("msg.confirm.copyclipuser", record.getTitle());
         if ( Options.isOptionSet("confirmCopyClipboard") )
            confirmOperation( owner, OP_SENDCLIPBOARD, "confirm.username" );
      }
   }
   
   private static void newGroupHelp () {
      HtmlBrowserDialog dlg;
      URL page;
      String hstr;
      
	  checkForEDT();
	   
      hstr = ResourceLoader.getDisplay( "dlg.information" );
      dlg = new HtmlBrowserDialog( Global.getActiveFrame(), hstr, false );
      if ( dlg.markSingleton( "help.newgroup" ) )
      {
         dlg.setSize( 500, 360 );
         dlg.moveRelatedTo( Global.getActiveFrame() );
         hstr = ResourceLoader.getCommand( "html.file.dlg.help.newgroup" );
         page = ResourceLoader.getResourceURL( "#standards/".concat( hstr ));
         try
         { dlg.setPage( page ); }
         catch ( IOException e )
         { e.printStackTrace(); }
         dlg.show();
      }
   }
   
   /**
    * Starts any <code>Runnable</code> task in a separate, new thread.
    * 
    * @param run <code>Runnable</code> task 
    * @return <code>Thread</code> started thread which is active (or has run)
    */
   public static Thread startTaskSeparate ( Runnable run ) {
      Thread thread = new Thread( run, "ActionHandler OrderedTask" );
      thread.start();
      return thread;
   }
   
   /**
    * Starts any <code>Runnable</code> task in the pool of worker threads.
    * 
    * @param run <code>Runnable</code> task 
    * @return <code>Future</code> handle to control the task
    * @throws NullPointerException 
    */
   public static Future<?> startTask ( Runnable run ) {
	  return threadPool.submit(run);
//      Thread thread = new Thread( run, "ActionHandler OrderedTask" );
//      thread.start();
   }

   /**
    * Starts any <code>Callable</code> task in the pool of worker threads.
    * 
    * @param run <code>Runnable</code> task 
    * @return <code>Future</code> handle to control the task
    * @throws NullPointerException 
    */
   public static Future<?> startTask ( Callable<?> callable ) {
	  return threadPool.submit(callable);
   }

   /**
    * Start any <code>Runnable</code> task after a wait time in a worker pool 
    * thread. The returned <code>ScheduledFuture</code> allows to inspect, 
    * cancel or join the task before its scheduled start time occurred.
    * 
    * @param run <code>Runnable</code> task 
    * @param delay long wait time in milliseconds from now
    * @return <code>ScheduledFuture</code> task handle
    */
   public static ScheduledFuture<?> startTaskDelayed ( Runnable run, long delay ) {
	   return schedulethreadPool.schedule(run, delay, TimeUnit.MILLISECONDS);
   }
   
   /**
    * Start any <code>Callable</code> task after a wait time in a worker pool 
    * thread. The returned <code>ScheduledFuture</code> allows to inspect, 
    * cancel or join the task before its scheduled start time occurred.
    * 
    * @param run <code>Callable</code> task 
    * @param delay long wait time in milliseconds from now
    * @return <code>ScheduledFuture</code> task handle
    */
   public static ScheduledFuture<?> startTaskDelayed ( Callable<?> callable, long delay ) {
	   return schedulethreadPool.schedule(callable, delay, TimeUnit.MILLISECONDS);
   }
   
   /**
    * Start any <code>Runnable</code> task in a worker pool thread after a wait 
    * time and continued periodically thereafter. The returned 
    * <code>ScheduledFuture</code> allows to inspect, cancel or join the task.
    * <p><small>The "join" will only return after the task is cancelled.</small>
    * 
    * @param run <code>Runnable</code> task 
    * @param delay long delay time between task executions in milliseconds 
    * @return <code>ScheduledFuture</code> task handle
    */
   public static ScheduledFuture<?> startTaskPeriodic ( Runnable run, long delay ) {
	   return schedulethreadPool.scheduleWithFixedDelay(
			   run, delay, delay, TimeUnit.MILLISECONDS);
   }
   
   /** Asynchronously performs the specified command action on the EDT.
    * This method returns immediately. 
    *  
    * @param command String command token of ActionRunner class
    */
   public static void invokeActionLater ( String cmd ) {
      executeOnEDT( new ActionRunner( cmd ) );
   }

   /** Asynchronously performs the specified command action on the EDT 
    * and awaits its completion. This method blocks until command execution
    * has completed. 
    *  
    * @param command String command token of ActionRunner class
    * @throws InvocationTargetException 
    * @throws InterruptedException
    */
   public static void invokeActionNow ( String cmd ) 
		   throws InterruptedException, InvocationTargetException {
      executeOnEDT_Wait( new ActionRunner( cmd ) );
   }
   
   /** Synchronously performs the specified command action in the calling thread. 
    * <p>Note: this can cause problems if the called command performs GUI
    * activity since it may not be guaranteed they are performed on EDT!
    *  
    * @param command String command token of ActionRunner class
    */
   public static void performAction ( String command ) {
      new ActionRunner( command ).run();
   }
   
   /** Ensures the parameter executable block is run on the AWT
    * Event Dispatching Thread. If the current thread is an AWT
    * Event Dispatching Thread, the block is called directly
    * without transferring it via "SwingUtilities.invokeLater".
    * 
    * @param r Runnable code block to execute
    */
   public static void executeOnEDT ( Runnable r )
   {
      if ( !SwingUtilities.isEventDispatchThread() )
         SwingUtilities.invokeLater( r );
      else
         r.run();
   }

   /** Ensures the parameter executable block is run on the AWT
    * Event Dispatching Thread. If the current thread is an AWT
    * Event Dispatching Thread, the block is called directly,
    * otherwise it is loaded via "SwingUtilities.invokeAndWait".
    * This command blocks until r has been run and then continues.
    * 
    * @param r Runnable code block to execute
    * @throws InvocationTargetException 
    * @throws InterruptedException 
    */
   public static void executeOnEDT_Wait ( Runnable r ) 
         throws InterruptedException, InvocationTargetException
   {
      if ( !SwingUtilities.isEventDispatchThread() )
         SwingUtilities.invokeAndWait( r );
      else
         r.run();
   }

   /** If the calling thread is an EDT, this starts the given action command
    * as an asynchronous worker task and returns <b>false</b>. Otherwise it
    * returns <b>true</b>.
    *  
    * @param command String action command
    * @return boolean true == we are running NON-EDT
    */
   private static boolean ensuredNonEDT (String command) {
	   return ensuredNonEDT(command, false);
   }
   
   /** If the calling thread is an EDT, this starts the given action command
    * as an asynchronous task and returns <b>false</b>. Otherwise it
    * returns <b>true</b>. The task can be run either in a worker thread or a
    * new separate thread. 
    *  
    * @param command String action command
    * @param separate boolean true == separate thread, false == worker thread
    * @return boolean true == we are running NON-EDT
    */
   private static boolean ensuredNonEDT (String command, boolean separate) {
	   if ( SwingUtilities.isEventDispatchThread() ) {
		  Runnable run = new ActionRunner(command);
		  if (separate) {
			 startTaskSeparate( run );
		  } else {
			 startTask( run );
		  }
		  return false;
	   }
	   return true;
   }

   /**
    * Renders a basic Action performable by this handler in return
    * to a name. 
    * Currently available names: "DELETE" (menu.edit.delete), 
    * "SAVEALL" (menu.file.saveall), "CLEARCLIP" (menu.edit.clearclip), 
    * "CLOSEALL" (menu.file.closeall) and "EDIT_ENTRY" (menu.edit.psw.edit).
    * 
    * @param name Action name
    * @return Action
    * @since 0-5-0
    */
   public static Action getAction( String name )
   {
      Action a = null;
      
      if ( name.equalsIgnoreCase( "DELETE" ) )
         a = deleteAction;
      else if ( name.equalsIgnoreCase( "EDIT_ENTRY" ) )
         a = editEntryAction;
      else if ( name.equalsIgnoreCase( "SAVEALL" ) )
          a = saveAllAction;
      else if ( name.equalsIgnoreCase( "CLOSEALL" ) )
         a = closeAllAction;
      else if ( name.equalsIgnoreCase( "CLEARCLIP" ) )
         a = clearClipAction;
      
      return a;
   }

   /**
    * Returns an <code>Action</code> to start the application's Internet
    * browser with the given URL parameter.
    * 
    * @param url URL the net address to start browsing
    * @originalName boolean if <b>true</b> the first 50 char or the URL are 
    *               taken as name of the action
    * @return <code>Action</code>
    * @since 0-6-0
    */
   @SuppressWarnings("serial")
   public static Action getStartBrowserAction( final URL url, boolean originalName )
   {
      String name = originalName ? url.toString() : ResourceLoader.getCommand( "menu.edit.starturl" );
      if ( name.length() > 50 ) {
         name = name.substring( 0, 50 );
      }
      
      Action action = new AbstractAction( name ) {
         @Override
		 public void actionPerformed ( ActionEvent e ) {
            Global.startBrowser( url ); 
         }
      };
      return action;
   }
   
   /**
    * Returns an <code>Action</code> to start the operating system's
    * standard email client with the given address parameter.
    * 
    * @param address String the email address to start writing to 
    * @originalName boolean if <b>true</b> the first 50 char of the address are 
    *               adopted as name of the action
    * @return <code>Action</code>
    * @since 0-6-0
    */
   public static Action getStartEmailAction ( final String address, boolean originalName )
   {
      String name = originalName ? address : ResourceLoader.getCommand( "menu.edit.startmail" );
      if ( name.length() > 50 ) {
         name = name.substring( 0, 50 );
      }

      Action action = new AbstractAction( name ) {
         @Override
		 public void actionPerformed ( ActionEvent e ) {
            Global.startEmail( address );
         }
      };
      return action;
   }

   public static String getThreadPoolStatistics () {
	   ThreadPoolExecutor tp = (ThreadPoolExecutor)threadPool;
	   String hstr = "Thread Pool Core = " + tp.getPoolSize() +
			         ", working = " + tp.getActiveCount() +
			         ", tasks = " + tp.getCompletedTaskCount();
	   return hstr;
   }
   
//**********  INNER CLASSES  ***********   
/**
 * @since 0-5-0
 */
@SuppressWarnings("serial")
private static class HandlerAction extends AbstractAction
{
	private boolean performSynchron;
	
   /**
    * Creates a new HandlerAction which performs asynchronous on the EDT.
    * 
    * @param title String name of action shown on control; obligatory
    * @param command String command issued to ActionHandler.actionListener; obligatory
    * @param icon Icon optional
    * @param tooltip String optional
    */
   public HandlerAction ( String title, String command, Icon icon, String tooltip )
   {
      super( title, icon );
      if ( title == null | command == null )
         throw new NullPointerException();
      
      putValue( ACTION_COMMAND_KEY, command );
      if ( tooltip != null ) {
         putValue( SHORT_DESCRIPTION, tooltip );
      }
   }

   /**
    * Creates a new HandlerAction which performs synchronous on the calling
    * thread of the action event. There is no restraint on what nature this
    * thread is.
    * 
    * @param title String name of action shown on control; obligatory
    * @param command String command issued to ActionHandler.actionListener; obligatory
    */
   public HandlerAction ( String title, String command )
   {
	   super(title);
       if ( title == null | command == null )
          throw new NullPointerException();
       
       putValue( ACTION_COMMAND_KEY, command );
	   performSynchron = true;
   }
   
   @Override
   public void actionPerformed ( ActionEvent e ) {
	  String cmd = e.getActionCommand(); 
	  if ( performSynchron ) {
		  performAction( cmd );
	  } else {
		  invokeActionLater( cmd );
	  }
   }
}
   
private static class ActionRunner implements Runnable
{
   private String cmd;

   ActionRunner ( String command ) {
      cmd = command; 
   }

   @Override
   public void run ()
   {
      try {
      PwsFileContainer file;
      DefaultRecordWrapper records[];
      PwsRecord record;
      UUID uid;
      String hstr;
      boolean isSelected, bo1;
      int i;
      
      lastUseTime = System.currentTimeMillis();
      
      if ( cmd.equals( "" ) )
         return;
      
      file = Global.getSelectedFile();
      isSelected = file != null;
   
      // FIRST GROUP: INDEPENDENTS
      if ( cmd.equals( "menu.file.new" ) )
      {
	     if ( !(Global.isDialogActive( "DatabaseDialog.NewFile" )) &&
    	      ensuredNonEDT(cmd)) {
	           DatabaseHandler.newFileToShelf( null );
    	 }
      }
      
      else if ( cmd.equals( "menu.file.open" ) )
      {
    	 DatabaseHandler.openFileToShelf();
      }
      
      else if ( cmd.equals( "menu.file.openurl" ) )
      {
    	 DatabaseHandler.openFileToShelf_Url();
      }
      
      else if ( cmd.equals( "menu.file.import" ) )
      {
	     if ( !Global.isDialogActive( "ImportCSVDialog.NewFile" ) &&
     	       ensuredNonEDT(cmd)) {
	        	
	         ImportCSVDialog dlg = new ImportCSVDialog( null );
	         dlg.setVisible( true );
	         Global.setDialogActive( "ImportCSVDialog.NewFile", true );
	      }
      }
      
      else if ( cmd.equals( "menu.file.saveall" ) )
      {
       	 if (ensuredNonEDT(cmd, true)) {
            Global.setStatusText(null);
            DisplayManager.saveAll();
       	 }
      }
      
      else if ( cmd.equals( "menu.file.closeall" ) )
      {
      	 if (ensuredNonEDT(cmd, true)) {
            Global.setStatusText(null);
            DisplayManager.closeAll();
      	 }
      }
      
      else if ( cmd.equals( "menu.file.exit" ) )
      {
         Global.exit();
      }
      
      else if ( cmd.startsWith( "load.recent" ) )
      {
      	 if (ensuredNonEDT(cmd)) {
    	    DatabaseHandler.openFileToShelf( cmd.substring( 11 ) );
      	 }
      }
   
      else if ( cmd.equals( "menu.view.idle" ) )
      {
         if ( DisplayManager.hasOpenFiles() )
            Global.switchIdleState( true );
         else
            SystemDesktopHandler.get().setIconified( true );
      }
      
      else if ( cmd.equals( "menu.view.mintrayicon" ) )
      {
         bo1 = !Options.isOptionSet( "minToTrayIcon" );
         SystemDesktopHandler.get().setTrayActive( bo1 );
         Options.setOption( "minToTrayIcon", bo1 );
      }
      
      else if ( cmd.equals( "menu.edit.clearclip" ) )
      {
         Global.setClipboardText( null );
         GUIService.statusConfirm("msg.confirm.clearclip");
      }
      
      else if ( cmd.equals( "menu.edit.options" ) )
      {
 	     if ( !Global.isDialogActive( "PreferencesDialog" ) ) {
            new PreferencesDialog( Global.getActiveFrame() ).setVisible(true);
 	     }
      }
      
      else if ( cmd.equals( "menu.view.single" ) )
      {
         DisplayManager.setDisplayState( DisplayManager.DISPLAY_SINGLE );
      }
      
      else if ( cmd.equals( "menu.view.desktop" ) )
      {
         DisplayManager.setDisplayState( DisplayManager.DISPLAY_DESKTOP );
      }
      
      else if ( cmd.equals( "menu.view.toggle-desktop" ) )
      {
         i = DisplayManager.isDesktopView() ?
             DisplayManager.DISPLAY_SINGLE : DisplayManager.DISPLAY_DESKTOP;
         DisplayManager.setDisplayState( i );
      }
      
      else if ( cmd.equals( "menu.help.docs" ) )
      {
      }
      
      else if ( cmd.equals( "menu.help.wipefile" ) )
      {
         Service.secureWipeFile();
      }
      
      else if ( cmd.equals( "menu.help.genrandpass" ) )
      {
         Service.generatePassword(null, true);
      }
      
      else if ( cmd.equals( "menu.help.portableinstall" ) )
      {
         if ( !Global.isDialogActive( "PortableInstallation" ) ) 
            new PortableInstallDialog().show();
      }
      
      else if ( cmd.equals( "menu.help.about" ) )
      {
         Service.showAboutDialog();
      }
      
      else if ( cmd.equals( "menu.help.system" ) )
      {
         Service.showSystemInfo();
      }

      else if ( cmd.equals( "menu.help.releasenotes" ) )
      {
         Service.showReleaseNotes();
      }
/*      
      else if ( cmd.equals( "menu.help.test1" ) )
      {
         test_1();
      }
*/      
      else if ( cmd.equals( "menu.help.support" ) )
      {
         Global.ProjectServiceParcel p = Global.ProjectServiceParcel.get();
         try {
            URL url = p != null ? p.getServicePageUrl() 
                      : new URL( ResourceLoader.getCommand( "html.supportpage" ) ); 
            Global.startBrowser( url );
         }
         catch ( Exception e ) 
         {}
      }
      
      else if ( cmd.equals( "menu.help.checknews" ) )
      {
      	 if (ensuredNonEDT(cmd)) {
            Service.controlProjectNews( true );
      	 }
      }
      
      else if ( cmd.equals( "menu.help.monitor" ) )
      {
         Options.setOption( "monitorSystem", MenuHandler.isMonitorSelected() );
         Global.setStatusText(null);
         Global.activateLoggingFile();
      }

      else if ( cmd.equals( "menu.debug.iomanager" )  )
      {
         Global.mainFrame.showMonitorDialog(true);
      }

      else if ( cmd.equals( "menu.debug.transferzip" )  )
      {
    	  JFileChooser chooser = new JFileChooser();
    	  if ( chooser.showOpenDialog(Global.mainFrame) == JFileChooser.APPROVE_OPTION ) {
    		  File src = chooser.getSelectedFile();
        	  if ( chooser.showSaveDialog(Global.mainFrame) == JFileChooser.APPROVE_OPTION ) {
        		 File tar = chooser.getSelectedFile();
        		 String msg = "Zip-File created: ".concat(tar.getAbsolutePath());
        		 try {
        		 	Service.transferZipfile(src, tar);
        		 	
        		 } catch (Exception e) {
        			 e.printStackTrace();
        			 msg = "Did not work, sorry!";
        		 }
        		 GUIService.infoMessage(null, msg);
        	  }
    	  }
      }

      /* ********************************************************************** */
      /* **** SECOND GROUP: Only checked if a file is operative in display  *** */
      /* ********************************************************************** */
      
      else if ( isSelected )
      {
         file.userActivity();
         
         if ( cmd.equals( "menu.file.convert" ) )
         {
            if (ensuredNonEDT(cmd)) {
               file.convert();
            }
         }
         
         else if ( cmd.startsWith( "menu.file.revert." ) )
         {
            if (ensuredNonEDT(cmd)) {
               file.revert( cmd.substring( 17 ) );
            }
         }
         
         else if ( cmd.equals( "menu.file.erasebackups" ) )
         {
            file.eraseBackups();
         }

         else if ( cmd.equals( "menu.file.deleterecents" ) )
         {
            if ( GUIService.userConfirm( null, "msg.ask.delete.recentlist" ) ) {
               Global.clearRecents();
            }
         }

         else if ( cmd.equals( "menu.file.save" ) )
         {
            if (ensuredNonEDT(cmd)) {
               file.saveFile( true );
            }
         }
         
         else if ( cmd.equals( "menu.file.close" ) )
         {
            if (ensuredNonEDT(cmd)) {
               Global.setStatusText(null);
               file.close();
            }
         }
         
         else if ( cmd.equals( "menu.file.savecopy" ) )
         {
            if (ensuredNonEDT(cmd)) {
               file.saveCopy(false);
            }
         }
         
         else if ( cmd.equals( "menu.file.saveas" ) )
         {
            if (ensuredNonEDT(cmd)) {
               file.saveAs();
            }
         }
         
         else if ( cmd.equals( "menu.edit.undo" ) )
         {
            file.getUndoManager().undo();
         }
         
         else if ( cmd.equals( "menu.edit.redo" ) )
         {
            file.getUndoManager().redo();
         }
         
         else if ( cmd.equals( "menu.edit.psw.add" ) )
         {
            file.addEntry();
         }
         
         else if ( cmd.equals( "menu.edit.psw.edit" ) )
         {
        	record = file.getSelectedRecord();
            if ( record != null && !file.hasMultiSelection() ) {
               file.editEntry( record );
            }
         }
         
         else if ( cmd.equals( "menu.edit.psw.delete" ) )
         {
        	 Log.log(10, "(ActionHandler.ActionRunner) -- DELETE COMMAND"); 
            if ( (records = file.getSelectedRecords()) != null ) {
               file.deleteCommand();
            }
         }
         
         else if ( cmd.startsWith( "edit.recent" ) )
         {
            hstr = cmd.substring( 11 );
            uid = new UUID( org.jpws.pwslib.global.Util.hexToBytes( hstr ) );
            if ( (record = file.getRecord( uid )) != null ) {
               file.editEntry( record );
            }
         }
      
         else if ( cmd.equals( "menu.edit.copypass" ) )
         {
            if ( (record = Global.getSelectedRecord()) != null ) {
               // determine whether to update record data + file	
               boolean updateRecord = file.isModified() || 
            		   Options.isOptionSet( "storeMinorChanges" );
               
               // send to clipboard, push recent list and update file (if possible)
               if ( sendClipboardPassword( null, record, updateRecord ) ) { 
                  try { 
                     file.recordUsed( record );
                     if ( updateRecord ) {
                    	 file.updateRecord( record );
                     }
                  } catch ( NoSuchRecordException e ) {
                  }
               }
            }
         }
         
         else if ( cmd.equals( "menu.edit.copyuser" ) )
         {
            if ( (record = Global.getSelectedRecord()) != null ) {
               sendClipboardUsername( null, record );
            }
         }
         
         else if ( cmd.equals( "menu.edit.delete" ) | 
                   cmd.equals( "menu.edit.delete.group" ) |
                   cmd.equals( "menu.edit.delete.record" ) )
         {
            file.deleteCommand();
         }
         
         else if ( cmd.equals( "menu.edit.duplicate" ) )
         {
            if ( (records = file.getSelectedRecords()) != null ) {
               file.addDuplicates( records );
            }
         }
         
         else if ( cmd.equals( "menu.edit.duplicategroup" ) )
         {
            if ( (hstr = file.getSelectedGroupName()) != null ) {
               file.duplicateGroup( hstr );
            }
         }
         
         else if ( cmd.equals( "menu.edit.selectall" ) )
         {
            file.selectAll();
         }
         
         else if ( cmd.equals( "menu.edit.selectrecords" ) )
         {
            file.selectRecords();
         }
         
         else if ( cmd.equals( "menu.edit.rename.group" ) )
         {
            if ( (hstr = file.getSelectedGroupName()) != null  )
               file.renameGroupDlg( hstr );
         }
         
         else if ( cmd.equals( "menu.edit.moveentries" ) )
         {
            file.moveSelectedDlg();
         }
         
         else if ( cmd.equals( "menu.edit.expandbranch" ) )
         {
            if ( file.getSelectionStatus() == PwsFileContainer.GROUP_SELECTED )
               file.setExpandedBranch( file.getSelectedGroupName(), true );
         }
         
         else if ( cmd.equals( "menu.edit.foldbranch" ) )
         {
            if ( file.getSelectionStatus() == PwsFileContainer.GROUP_SELECTED )
               file.setExpandedBranch( file.getSelectedGroupName(), false );
         }
         
         else if ( cmd.equals( "menu.edit.foldall" ) )
         {
            file.setExpandedBranch( "", false );
         }
         
         else if ( cmd.equals( "menu.edit.find" ) )
         {
            new FindTextDialog( Global.getActiveFrame() );
         }
         
         else if ( cmd.equals( "menu.edit.quickfind" ) )
         {
            
            file.setQuickFindActive( !file.isQuickFindActive() );
         }
         
         else if ( cmd.equals( "menu.edit.export.csv" ) )
         {
            
            file.exportSelectionCSV();
         }
         
         else if ( cmd.equals( "menu.edit.starturl" )  )
         {
            URL url;
            
            if ( (record = file.getSelectedRecord()) != null &&
                 (url = Util.extractURL( record.getUrl() )) != null ) 
            {
               Global.startBrowser( url );
               if ( Options.isOptionSet( "useEntryOnBrowse" ) )
                  file.recordUsed( record );

               if ( Options.isOptionSet( "autoCopyPass" ) )
                  performAction( "menu.edit.copypass" );
            }
         }

         else if ( cmd.startsWith( "menu.edit.favmark." )  )
         {
            hstr = cmd.substring( 18 );
            file.setRecordsFavourite( file.getSelectedRecords(), hstr.equals( "set" ) );
         }
         
         else if ( cmd.startsWith( "menu.edit.newgroup" )  )
         {
            newGroupHelp();
         }
         
         else if ( cmd.equals( "menu.manage.policy" ) )
         {
            editFilePWPolicy(file);
         }
         
         else if ( cmd.equals( "menu.manage.changepass" ) )
         {
            file.changePassphrase();
         }
         
         else if ( cmd.equals( "menu.manage.importfile.csv" ) )
         {
            file.importFileCSV();
         }
         
         else if ( cmd.equals( "menu.manage.exportfile.csv" ) )
         {
            file.exportFileCSV();
         }
         
         else if ( cmd.equals( "menu.manage.backup" ) )
         {
            file.saveCopy(true);
         }
         
         else if ( cmd.equals( "menu.manage.restore" ) )
         {
            file.restoreBackup();
         }
         
         else if ( cmd.equals( "menu.manage.mergefile" ) )
         {
            file.mergeDatabase();
         }
         
         else if ( cmd.equals( "menu.view.list" ) )
         {
            file.setViewType( PwsFileContainer.TABLE_VIEW );
         }
         
         else if ( cmd.equals( "menu.view.tree" )  )
         {
            file.setViewType( PwsFileContainer.TREE_VIEW );
         }
         
         else if ( cmd.equals( "menu.view.filter.all" )  )
         {
            file.setFilterStatus( PwsFileContainer.FILTER_OFF );
         }
         
         else if ( cmd.equals( "menu.view.filter.modify" )  )
         {
            file.toggleFilter( PwsFileContainer.FILTER_MODIFIED );
         }
         
         else if ( cmd.equals( "menu.view.filter.import" )  )
         {
            file.toggleFilter( PwsFileContainer.FILTER_IMPORTED );
         }
   
         else if ( cmd.equals( "menu.view.filter.expiry" ) |
                   cmd.equals( "menu.view.toggle-expiring" ) )
         {
            file.toggleFilter( PwsFileContainer.FILTER_EXPIRING );
         }
         
         else if ( cmd.equals( "menu.view.filter.favourites" ) |
                   cmd.equals( "menu.view.toggle-favourites" ) )
         {
            file.toggleFilter( PwsFileContainer.FILTER_FAVOURITES );
         }
   
         else if ( cmd.equals( "menu.view.filter.invalid" )  )
         {
            file.setFilterStatus( PwsFileContainer.FILTER_INVALID );
         }
   
         else if ( cmd.equals( "menu.help.fileinfo" )  )
         {
            file.showFileInfo();
         }
   
         else if ( cmd.equals( "viewport.cancelfilter" )  )
         {
            file.setFilterStatus( PwsFileContainer.FILTER_OFF );
         }

         else if ( cmd.startsWith( "selection.movetofile." )  )
         {
            hstr = cmd.substring( 21 );
            PwsFileContainer target = DisplayManager.getFileContainer(new UUID(hstr));
            if ( target != null ) 
               target.importSelectedFrom( file, false );
         }
         
         else if ( cmd.startsWith( "selection.copytofile." )  )
         {
            PwsFileContainer target;
            
            hstr = cmd.substring( 21 );
            target = DisplayManager.getFileContainer( new UUID( hstr ) );
            if ( target != null ) 
               target.importSelectedFrom( file, true );
         }
         
         else
            System.out.println("** unrecognized command: " + cmd );
         
      }  // end SELECTED
      
      else
         System.out.println("** unrecognized command: " + cmd );

      // returns the input focus to the currently selected container
//      if ( file != null ) {
//         file.grabFocus();
//      }
      } catch (Throwable e) {
    	  e.printStackTrace();
      }
   }  // run
} // class ActionRunner
   
/**
 *  This class is the main listener to action commands. Those are majorly
 *  coming from the GUI's menubars and toolbars.
 *  
 */
private static class MainActionListener implements ActionListener
{
   @Override
   public void actionPerformed ( ActionEvent arg0 ) {
	   performAction( arg0.getActionCommand() );
   }
}

///**
// *  A subclass of <code>java.lang.Thread</code> that can be scheduled
// *  for future execution.
// *  After the scheduled time is reached this thread will be activated.
// *  The schedule may get terminated before the thread has actually started 
// *  by use of <code>TimedThread.cancel()</code>.
// */
//public static class TimedThread extends Thread
//{
//   private boolean isStarted;
//   private TimerTask timerTask;
//   
//   /**
//    * Creates a thread run schedule with the specified delay time. After
//    * time has elapsed, this thread will start execution.
//    *  
//    * @param r <code>Runnable</code> to be run by this thread
//    * @param name String (optional) name for this thread; may be <b>null</b>
//    * @param delay start delay time in milliseconds
//    */
//   public TimedThread ( final Runnable r, String name, long delay ) {
//      super( r, "Ordered Timed Task: ".concat( name == null ? "?" : name ) );
//      if ( r == null )
//         throw new NullPointerException();
//      
//      // our timer task to start this thread
//      timerTask =  new TimerTask() {
//          @Override
//			public void run() { 
//             if ( !isStarted ) {
//                isStarted = true;
//                TimedThread.this.start();
//             }
//          }
//       };
// 
//      Global.getTimer().schedule( timerTask, delay );
//   }  // constructor
//   
//   /**
//    * Creates a thread run schedule with the specified delay time. After
//    * time has elapsed, this thread will start execution.
//    *  
//    * @param r <code>Runnable</code> to be run by this thread
//    * @param delay start delay time in milliseconds
//    */
//   public TimedThread ( final Runnable r, long delay ) {
//      this( r, null, delay );
//   }
//   
//   /** Terminates the thread run schedule. If the thread has already started, it 
//    *  will run to its end.
//    */
//   public void cancel () {
//      timerTask.cancel();
//   }
//   
//   /** If this thread has not been started yet, it will be scheduled for
//    * execution immediately (equal priority to current thread). 
//    */
//   public void executeNow () {
//      timerTask.cancel();
//      if ( !isStarted ) {
//         isStarted = true;
//         start();
//      }
//   }
//   
//   /** If this thread has not been started yet, it will be scheduled for
//    * execution immediately (equal priority to current thread). The current
//    * thread waits until this thread has terminated. 
//    */
//   public void executeAndWait () {
//      timerTask.cancel();
//      if ( !isStarted ) {
//         isStarted = true;
//         start();
//      }
//      try { this.join(); 
//      } catch ( InterruptedException e ) { 
//    	  e.printStackTrace(); 
//      }
//   }
//   
//}  // class TimedTask


}
