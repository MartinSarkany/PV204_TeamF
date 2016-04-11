/*
 *  DisplayManager in org.jpws.front
 *  file: DisplayManager.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 08.01.2006
 *  Version
 * 
 *  Copyright (c) 2006 by Wolfgang Keller, Munich, Germany
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket;
import org.jpws.front.util.RecentList;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.UUID;

/**
 * Class to organise global display arrangements. 
 * 
 * <p>Events
 * <br>The class fires events of type <code>javax.swing.event.ChangeEvent</code>
 * under following conditions (not fully implemented!):
 * <br>- when a file or element is added to or removed from the Desktop
 * <br>- when selection of elements (e.g. containers) changes 
 * <br>- when quality changes from NO-MODIFIED to MODIFIED files
 * <br>- when quality changes from MODIFIED to NO-MODIFIED file
 * <br>- when an editor of content elements has started or terminated
 * 
 * 
 */
public final class DisplayManager
{
   public static final int DEFAULT_MAX_DESKTOP_FRAMES = 10;
   private static final int DEFAULT_RECENT_WINS = DEFAULT_MAX_DESKTOP_FRAMES * 2;
   private static final Dimension MINIMUM_WINDOWSIZE = new Dimension( 200, 60 );
   
   public static final int DISPLAY_SINGLE = 0;
   public static final int DISPLAY_DESKTOP = 2;

   public static final int STATE_ACTIVE = 10;
   public static final int STATE_INACTIVE = 11;
   public static final int STATE_ICONIFIED = 12;
   
   // PUBLIC AFFAIRS
   private static ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
   
   // FONT HANDLING
   private static HashMap<String, Font> fontStore;
   private static String[] avFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(); 
   private static Font defaultFont;
   private static Font menuDefault;
   private static Font passwordDefault;
   private static Font displayDefault;

   // CONTAINER HANDLING
   /** boundsStore is a map of file-container UUIDs into the bounds of their DesktopContainers.
    *  This map holds information of past or open windows. It is used to memorise closed windows'
    *  bounds on the desktop for future reopen actions. */
   private static HashMap<UUID, Rectangle> boundsStore = new HashMap<UUID, Rectangle>();
   
   /** sortedWins holds a list of actually open DesktopContainers, sorted to latest activation events. 
    *  It is used for picking preferred windows according to usage footprint. */
   private static RecentList sortedWins = new RecentList( DEFAULT_MAX_DESKTOP_FRAMES );
   
   /** recentWins holds a list of UUID values of the recent window history of the desktop. 
   *   This map holds information of past or open windows. It is the base for serialising 
   *   window bounds information for persistence. */
   private static RecentList recentWins = new RecentList( DEFAULT_RECENT_WINS );
   
   private static JDesktopPane desktopPane;
   private static ContainerListener ctListener = new ContainerListener();
   private static PwsFileContainer activeCT;
   private static int maxDesktopFrames = DEFAULT_MAX_DESKTOP_FRAMES;
   private static boolean showInfoDialog;

   
   // DISPLAY STATE
   private static int displayState;
   

   /** Static singleton. */
   private DisplayManager () {
   }
   
   public static void init () {
      UUID uuid;
      Rectangle bounds;
      String hstr, history, winP[];
      int i, index;
      
      fontStore = new HashMap<String, Font>();
      
      // define default fonts
      defaultFont = Font.decode( "Dialog-PLAIN-12" );
      passwordDefault = Font.decode( "DialogInput-BOLD-14" );
      menuDefault = Font.decode( "Comic Sans MS-ITALIC-13" );
      if ( isSystemFontFamily( "Trebuchet MS" ) )
         displayDefault = Font.decode( "Trebuchet MS-PLAIN-13" );
      else
         displayDefault = defaultFont;
      
      // initialise fonts from options or defaults
      initCanonFont( "menu" );
      initCanonFont( "control" );
      initCanonFont( "tooltip" );
      initCanonFont( "display" );
      initCanonFont( "data" );
      initCanonFont( "password" );
      initCanonFont( "notes" );
      
      // feed window bounds history from persistent state
      history = Options.getOption( "desktop-winhistory" );
      Log.debug(8, "(DisplayManager) - Desktop history:");
      winP = history.split( "," );
      for ( i = winP.length; i > 0; i-- ) {
	      try {
	         if ( (hstr = winP[ i-1 ]).length() > 0 ) {
	            index = hstr.indexOf('.');
	            uuid = new UUID( hstr.substring( 0, index) );
	            bounds = Util.longToBounds( Long.parseLong( hstr.substring( index+1 ) ) );
	            boundsStore.put( uuid, bounds );
	            recentWins.pushRecent( uuid );
                Log.debug(8, "(DisplayManager) -- bounds store added: " + uuid + " at " + bounds );
	         }
	      } catch ( Exception e ) { 
	    	 e.printStackTrace(); 
	      }
      }

      // initiate the display on EDT
      Runnable run = new Runnable() {
		@Override
		public void run() {
	       // create display elements
	       MenuHandler.init();
	       ToolbarHandler.init();

	       // create and display the mainframe
	       Global.mainFrame = new PwsafeJ();
	       Global.mainFrame.setVisible( true );

	       // install recent principal display modus
	       int i = Options.getIntOption( "displayState" );
	       if ( i != DISPLAY_SINGLE & i != DISPLAY_DESKTOP ) {
	          i = DISPLAY_DESKTOP;
	       }
	       setDisplayState( i );
		}
      };
      ActionHandler.executeOnEDT(run);
   }  // init
   
   /**
    * Triggers off mirror save activity on every open database file which is 
    * under administration of the display manager.
    * Files get saved to mirrors in case they were modified. If any save operation 
    * fails, <b>false</b> is returned.
    * 
    * @return boolean <b>true</b> if and only if all modified files were saved
    */
   public static boolean checkMirrorActivity () {
      boolean ok = true;
      
      for ( Iterator<?> it = getContainerIterator(); it.hasNext(); ) {
    	 PwsFileContainer ct = (PwsFileContainer)it.next();
         ok &= ct.checkMirrorActivity();
      }
      return ok;
   }
   
   /**
    * Checks through every open database file under administration of the display manager.
    * Files get saved to mirrors in case they were modified. If any save operation 
    * fails, <b>false</b> is returned.
    * 
    * @return boolean <b>true</b> if and only if all modified files were saved
   public static void checkBlindfoldActivity ()
   {
      PwsFileContainer ct;
      Iterator<?> it;
      
      for ( it = getContainerIterator(); it.hasNext(); )
      {
         ct = (PwsFileContainer)it.next();
         
      }
   }
*/

   public static void exit () {
      synchronized ( sortedWins ) {
         // close any still open desktop windows
         if ( sortedWins.getSize() > 0 ) {
            for ( Iterator<?> it = ((RecentList)sortedWins.clone()).iterator(); 
            	  it.hasNext(); ) {
               DesktopContainer dtc = (DesktopContainer)it.next();
               dtc.dispose();
            }
         }
         
         // save window bounds history to program options
         String text = "";
         int i = 0;
         for ( Iterator<?> it = recentWins.iterator(); 
        	   it.hasNext() && i < maxDesktopFrames; i++ ) {
        	 
            UUID uuid = (UUID)it.next();
            Rectangle bounds = boundsStore.get( uuid );
            if ( bounds != null ) {
               long b = Util.boundsToLong( bounds );
               String hstr = uuid.toHexString() + "." + String.valueOf( b );
 //               System.out.println( "[" + hstr + "]" );
               text = i == 0 ? hstr : text + "," + hstr;  
            }
         }
         Options.setOption( "desktop-winhistory", text );
      }
   }
   
   /** Returns the first (=latest used) <tt>PwsFileContainer</tt> from the desktop
    * set of windows or <b>null</b> if no file is loaded.
    *    
    * @return <tt>PwsFileContainer</tt> or <b>null</b>
    */
   public static PwsFileContainer getFirstContainer () {
      Iterator<?> it = getContainerIterator();
      return it.hasNext() ? (PwsFileContainer)it.next() : null;
   }
   
   /** Returns the open desktop window which has the shortest time span
    * since it was last activated. 
    * 
    * @return DesktopContainer or <b>null</b> if desktop is empty
    */ 
   private static DesktopContainer getLatestWin () {
      return (DesktopContainer)sortedWins.getFirst();
   }
   
   /** Returns the desktop window which has the longest time span
    * since it was last activated. (Does not return any closed windows.)
    * 
    * @return DesktopContainer or <b>null</b> if desktop is empty
    */ 
   private static DesktopContainer getOldestWin () {
      return (DesktopContainer)sortedWins.getLast();
   }
   
   private static void createDesktop () {
      desktopPane = new JDesktopPane();
      Log.log( 7, "(DisplayManager) created desktop, drag mode = ".concat( 
            desktopPane.getDragMode() == JDesktopPane.LIVE_DRAG_MODE ? "LIFE" : "OUTLINE" ) );
   }
   
   /** Creates and returns a standard empty screen panel with BorderLayout.
    * The panel may contain decorations to the season if this option is set
    * in preferences and the time is right.
    * 
    * @return <code>JPanel</code>
    */
   public static JPanel createEmptyScreen () {
	   JPanel panel = new JPanel(new BorderLayout());
	   Icon icon = null;
	   
	   // check for an event decoration
	   if ( Global.isChristmas() ) {
		   icon = ResourceLoader.getImageIcon("deco-christmas-1");
	   } else if ( Global.isEaster() ) {
		   icon = ResourceLoader.getImageIcon("deco-easter-1");
	   }

	   // install event decoration if available
	   if ( icon != null ) {
		  JLabel label = new JLabel(icon);
		  panel.add(label);
	   }
	   return panel;
   }
   
   /** Returns the current principal display modus.
    * 
    * @return DISPLAY_DESKTOP or DISPLAY_SINGLE
    */
   public static int getDisplayState () {
      return displayState;
   }
   
   /** Whether the current display modus is DISPLAY_DESKTOP. */
   public static boolean isDesktopView () {
      return displayState == DisplayManager.DISPLAY_DESKTOP;
   }
   
   /**
    * Returns the file container identified by the parameter file UUID
    * value or <b>null</b> if this ID is unknown in the JPWS desktop.
    * 
    * @param id UUID file UUID
    * @return <code>PwsFileContainer</code> or <b>null</b>
    */
   public static PwsFileContainer getFileContainer ( UUID id ) {
      if ( id == null ) {
         Log.log( 7, "(DisplayManager.getFileContainer) ++ looking for UUID == null !!! --> not found" );
         return null;
      }
      
      Log.log( 7, "(DisplayManager.getFileContainer) ++ looking for UUID: ".concat( id.toString() ) );
      if ( desktopPane != null ) {
    	 JInternalFrame[] frames = desktopPane.getAllFrames();
         for ( int i = 0; i < frames.length; i++ ) {
        	DesktopContainer winCt = ((DesktopContainer)frames[ i ]);
            if ( winCt.container.getUUID().equals( id ) ) {
               Log.log( 7, "(DisplayManager.getFileContainer) ++ found CT: ".concat( winCt.container.getDatabaseName() ));
               return winCt.container;
            }
         }
      }
      if ( activeCT != null && activeCT.getUUID().equals( id ) ) {
         Log.log( 7, "(DisplayManager.getFileContainer) ++ found CT: ".concat( activeCT.getDatabaseName() ));
         return activeCT;
      }
      Log.log( 7, "(DisplayManager.getFileContainer) ++ CT not found" );
      return null;
   }
   
   // TODO render a List of PwsFileContainer instead or additional to iterator
   
   /** Returns an Iterator over all <code>PwsFileContainer</code> objects that are 
    * currently represented in the Desktop. 
    * @return <code>Iterator</code> over type {@link PwsFileContainer}
    */
   public static Iterator<PwsFileContainer> getContainerIterator () {
      return new ContainerIterator();
   }
   
   /**
    * Whether the given UUID denotes a database which is represented in the the display
    * manager (i.e. has a GUI window).
    *  
    * @param id UUID of database
    * @return boolean
    */
   public static boolean hasDatabase ( UUID id ) {
      return getFileContainer( id ) != null;
   }
   
   /**
    * Whether the parameter file is the persistent state of a file
    * container currently registered in this display manager.
    * <b>null</b> parameter returns <b>false</b>. 
    *  
    * @param f <code>ContextFile</code>; may be <b>null</b>
    */
   public static boolean isRegisteredFile( ContextFile f ) {
      return getFileContainer( f ) != null;
   }
   
   /**
    * Returns the file container identified by the parameter context file 
    * or <b>null</b> if unknown in the desktop.
    * 
    * @param f <code>ContextFile</code> database file 
    * @return <code>PwsFileContainer</code> or <b>null</b>
    */
   public static PwsFileContainer getFileContainer( ContextFile f ) {
      if ( f == null ) {
         Log.log( 7, "(DisplayManager.isRegisteredFile) !!! looking for file == null !!! --> not found" );
         return null;
      }
      
      if ( displayState == DISPLAY_SINGLE && activeCT != null && 
    	   activeCT.getContextFile() != null && activeCT.getContextFile().equals( f ) ) {
    	  
         Log.log( 7, "(DisplayManager.isRegisteredFile) ++ found file registered: ".concat( f.getFilepath() ));
         return activeCT;
      }

      Log.log( 7, "(DisplayManager.isRegisteredFile) ++ looking for file: ".concat( f.getFilepath() ) );
      if ( desktopPane != null ) {
    	  JInternalFrame[] frames = desktopPane.getAllFrames();
         for ( int i = 0; i < frames.length; i++ ) {
        	DesktopContainer winCt = ((DesktopContainer)frames[ i ]);
            if ( f.equals( winCt.container.getContextFile() ) ) {
               Log.log( 7, "(DisplayManager.isRegisteredFile) ++ found file registered: ".concat( f.getFilepath() ));
               return winCt.container;
            }
         }
      }
      
      Log.log( 7, "(DisplayManager.f.getFilepath()) ++ file not found" );
      return null;
   }
   
   /**
    * Sets the parameter file container as ACTIVE container, linking it to
    * required listeners and references. A previous active container is 
    * de-activated. <b>null</b> parameter de-activates the currently active
    * container and sets ACTIVE to void.
    * 
    * @param file PwsFileContainer, may be <b>null</b>
    * @return
    */
   private static boolean makeActiveContainer ( PwsFileContainer file ) {
      String dbname = file == null ? "null" : file.getDatabaseName();
      PwsFileContainer oldActive = activeCT;
      boolean ok = true;
      
      // entering conditions
      if ( file != null ) {
    	  
         if ( file == activeCT ) {
        	file.grabFocus(); 
            Log.log( 7, "(DisplayManager.makeActiveContainer) found file already active: ".concat( dbname ) );
            return true;
         }
         if ( displayState == DISPLAY_DESKTOP && findContainerWindow( file ) == null ) {
            Log.log( 7, "(DisplayManager.makeActiveContainer) *** ERROR *** : requested file not part of the desktop pane: ".concat( dbname ) );
            return false;
         }
      }
      
      // DE-ACTIVATE old active container
      if ( activeCT != null ) {
         Log.log( 7, "(DisplayManager.makeActiveContainer) de-activating container: ".concat( activeCT.getDatabaseName() ) );
         PwsFileContainer ct = activeCT;
         activeCT = null;
         
         // unregister listeners to container if this is the active CT
         ct.setSelected( false );
         MenuHandler.unregisterFile( ct );
         ToolbarHandler.unregisterFile( ct );
         showInfoDialog = ct.fileInfoDlg != null;

         // in the SINGLE view modus clear the desktop panel
         if ( displayState == DISPLAY_SINGLE ) {
            Log.log( 7, "(DisplayManager.makeActiveContainer) resetting MAINFRAME screen to empty" );
            Global.mainFrame.resetDisplay();
            FindTextDialog.resetIndex();
         } else {
            Global.mainFrame.setTitleFile( null );
         }
      }

      // ACTIVATE new container
      if ( file != null ) {
         Log.log( 7, "(DisplayManager.makeActiveContainer) activating container: ".concat( dbname ) );
         activeCT = file;
   
         try { 
            MenuHandler.registerFile( activeCT );
            ToolbarHandler.registerFile( activeCT );
            
            if ( displayState == DISPLAY_SINGLE ) {
               Global.mainFrame.setScreenView( activeCT.getView() );
            }
            
            activeCT.setSelected( true );
            ActionHandler.resetIdleTime();
            Log.log( 7, "(DisplayManager.makeActiveContainer) ++ file activated ++: ".concat( dbname ) );

         } catch ( Exception e ) {
            e.printStackTrace();
            Log.log( 7, "(DisplayManager.makeActiveContainer) -- FAILURE -- could not activate file: ".concat( dbname ) );
            ok = false;
         }
      }
      
      // dispatch selection change event (if occurring)
      if ( activeCT != oldActive ) {
         fireSelectionChanged();
      }
      
      return ok;
   }  // makeActiveContainer
   
   /**
    * Attempts to sets the principal display modus. The operation
    * may fail if display manager finds it impossible (e.g. failing 
    * file save).
    * 
    * @param state DISPLAY_DESKTOP or DISPLAY_SINGLE
    * @return boolean <b>true</b> if and only if the requested state 
    *         has been established
    */
   public static boolean setDisplayState ( int state ) {
      DesktopContainer winCt;
      PwsFileContainer file;
      
      String hstr = "TARGET DISPLAY STATE: " + 
                    (state == DISPLAY_DESKTOP ? "DESKTOP" : "SINGLE");
      Log.log( 7, "(DisplayManager.setDisplayState) ".concat( hstr ) );
      
      if ( state == DISPLAY_SINGLE & displayState != DISPLAY_SINGLE ) {
         // from multi to single
         // we leave all settings in the desktop pane as they are
         
         // try to determine a new active file if nothing set up
         file = activeCT;
         if ( file == null ) {
            if ( (winCt = getLatestWin()) != null ) {
               file = winCt.container; 
            }

            Log.log( 7, "(DisplayManager.setDisplayState) no active file, identified successor: ".concat( 
                  file == null ? "- none -" : file.getDatabaseName() ) );
         }
            
         // remove internal frame (would be empty anyway)
         if ( (winCt = findContainerWindow( file )) != null ) {
            winCt.dispose();
         }
         
         // switch to single view
         Global.mainFrame.setScreenView( file == null ? null : file.getView() );
         displayState = DISPLAY_SINGLE;
         makeActiveContainer( file );
         
         // update new state infos
         Options.setIntOption( "displayState", DISPLAY_SINGLE );
         MenuHandler.setMainframeDisplayState( DISPLAY_SINGLE );
         
         if ( file != null ) file.grabFocus();
         
         Log.log( 7, "(DisplayManager.setDisplayState) ++ switched to desktop state SINGLE" );
         return true;
      }
      
      if ( state == DISPLAY_DESKTOP & displayState != DISPLAY_DESKTOP ) {
         // from single to multi
         // create the desktop pane if not present
         if ( desktopPane == null ) {
            createDesktop();
         }

         // switch to desktop view
         Global.mainFrame.setScreenView( desktopPane );
         displayState = DISPLAY_DESKTOP;
         
         // if single view has a CT
         if ( activeCT != null ) {
        	 
            // if CT is unknown to desktop pane
            if ( findContainerWindow( activeCT ) == null ) {
               winCt = new DesktopContainer( activeCT );
               synchronized (desktopPane) {
                  desktopPane.add( winCt );
               }
               fireWindowlistModified();
            }

            // set activation on active container (triggers internal frame selection)
            file = activeCT;
            file.setSelected( false );
            file.setSelected( true );
         }
        
         // if single view was empty
         else if ( (winCt = getLatestWin()) != null ) {
            winCt.container.setSelected( true );
         }

         // update new state infos
         Options.setIntOption( "displayState", DISPLAY_DESKTOP );
         MenuHandler.setMainframeDisplayState( DISPLAY_DESKTOP );
         Log.log( 7, "(DisplayManager.setDisplayState) ++ switched to desktop state DESKTOP" );
         return true;
      }
      
      Log.log( 7, "(DisplayManager.setDisplayState) leaving with nothing changed" );
      return false;
   }  // setDisplayState
   
   /**
    * Returns the current activation state of the parameter file container
    * in the JPWS desktop.
    * 
    * @param ct <code>PwsFileContainer</code> desktop container to be evaluated
    * @return int one of STATE_ACTIVE, STATE_INACTIVE, STATE_ICONIFIED
    * @throws IllegalArgumentException if the parameter is not a registered
    *         element of the desktop
    */
   public static int getActivationState ( PwsFileContainer ct ) {
      if ( displayState == DISPLAY_SINGLE ) {
         if ( activeCT != ct ) {
            throw new IllegalArgumentException( "not an element in DisplayManager" );
         } else {
            return  STATE_ACTIVE;
         }
      }
      
      DesktopContainer winCt = findContainerWindow( ct );
      if ( winCt == null ) { 
         throw new IllegalArgumentException( "not an element in DisplayManager" );
      }
      
      // TODO improvement for ICONIFIED state
      return  winCt.container == activeCT ? STATE_ACTIVE : STATE_INACTIVE;
   }
   
   /**
    * Requests the display manager to establish a specific activation state
    * for the parameter file container in the GUI. The parameter has to be 
    * already a registered element of the JPWS desktop. Activation may deactivate
    * other desktop elements. The request can be denied if the requested state
    * conflicts with ongoing operations in the GUI. 
    * 
    * @param ct <code>PwsFileContainer</code> desktop container to be activated
    * @param state int one of STATE_ACTIVE, STATE_INACTIVE, STATE_ICONIFIED
    * 
    * @return boolean <b>true</b> if and only if the requested activation 
    *         has been performed
    * @throws IllegalArgumentException if the parameter is not a registered
    *         element of the desktop
    * 
    */
   public static boolean requestActivationState ( PwsFileContainer ct, int state ) {
      Log.log( 7, "(DisplayManager.requestActivationState) enter CT: " + 
            (ct == null ? "null" : ct.getDatabaseName()) + ", next state=" + state );

      if ( displayState == DISPLAY_SINGLE ) {
         return ct == activeCT;
      }
      
      DesktopContainer winCt = findContainerWindow( ct );
      if ( winCt != null )
      try {
         if ( state == STATE_ACTIVE ) {
        	if ( !winCt.isSelected() )
        	   winCt.setSelected( true );
        	if ( winCt.isIcon() )
               winCt.setIcon( false );
         }
         if ( state == STATE_INACTIVE ) {
         	if ( winCt.isSelected() )
               winCt.setSelected( false );
         }
         if ( state == STATE_ICONIFIED ) {
         	if ( !winCt.isIcon() )
         		winCt.setIcon( true );
         }

      } catch ( PropertyVetoException e ) { 
    	  e.printStackTrace(); 
      }
      return false;
   }
   
   /**
    * Whether the parameter file container is registered in the
    * display manager. (<b>null</b> returns <b>false</b>.)
    * 
    * @param ct PwsFileContainer; may be <b>null</b>
    * @return boolean
    */
   public static boolean isRegisteredContainer ( PwsFileContainer ct ) {
      if ( displayState == DISPLAY_DESKTOP )
         return findContainerWindow( ct ) != null;
      return ct != null & ct == activeCT;
   }
   
   /**
    * Attempts to add a file container to the JPWS desktop. After registration, 
    * if it is the
    * first element of the desktop, or if there is no other element currently
    * active, the parameter will be activated, otherwise it will be deactivated. 
    * If the desktop cannot hold another element, this registration fails by 
    * returning <b>false</b>. 
    * 
    * @param ct <code>PwsFileContainer</code> to be added
    * @return boolean <b>true</b> if and only if the requested registration 
    *         has been performed
    */
   public static boolean registerContainer ( final PwsFileContainer file ) {
      if ( file == null )
         throw new NullPointerException();
      
      String dbname = file.getDatabaseName();
      Log.log( 7, "(DisplayManager.registerContainer) enter to REGISTER CT: ".concat( dbname ) );
      
      // control if file is not already loaded
      if ( isRegisteredContainer( file ) ) {
         Log.log( 7, "(DisplayManager.registerContainer) -- file already registered, leaving" );
         return true;
      }

      // SINGLE state: complain if we have some file loaded 
      if ( displayState == DISPLAY_SINGLE & activeCT != null ) {
         Log.log( 7, "(DisplayManager.registerContainer) -- SINGLE MODE: no room for new CT, leaving" );
         return false;
      }
      
      // DESKTOP VIEW
      if ( displayState == DISPLAY_DESKTOP ) {
    	  
         // complain if desktop pane element limit is reached
         if ( desktopPane.getAllFrames().length == maxDesktopFrames ) {
            Log.log( 7, "(DisplayManager.registerContainer) -- DESKTOP MODE: no room for new CT, leaving" );
            return false;
         }
         
         Runnable run = new Runnable () {
			@Override
			public void run() {
		       // create new desktop pane element
		      DesktopContainer winCt = new DesktopContainer( file );
		      synchronized (desktopPane) {
		         desktopPane.add( winCt );
		         desktopPane.validate();
		         try {
					winCt.setSelected(true);
				 } catch (PropertyVetoException e) {
					e.printStackTrace();
				 }
		      }
		      Log.log( 7, "(DisplayManager.registerContainer) -- DESKTOP MODE: added new CT frame" );
		      fireWindowlistModified();
			}
         };
         try {
			ActionHandler.executeOnEDT_Wait(run);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

      // SINGLE VIEW
      } else {
	      // assign new file as ACTIVE
	      if ( !makeActiveContainer( file ) ) {
	         Log.log( 7, "(DisplayManager.registerContainer) -- FAILURE -- could not register file: ".concat( dbname ) );
	         return false;
	      }
      }

      // trigger info-dialog if marked
      if ( showInfoDialog ) {
         activeCT.showFileInfo();
      }

      containerEntering( file );
      Log.log( 7, "(DisplayManager.registerContainer) ++ DONE ++ registered file: ".concat( dbname ) );
      return true;
   } // registerContainer

   /** Operations when a container enters the display manager. */ 
   private static void containerEntering ( PwsFileContainer ct ) {
      ct.addChangeListener( ctListener );
   }
   
   /** Operations when a container leaves the display manager. */ 
   private static void containerLeaving ( PwsFileContainer ct ) {
      ct.removeChangeListener( ctListener );
   }
   
   /** Saves all modified databases which are open in the desktop. */
   public static void saveAll () {
	  ActionHandler.checkForThread();
	  if ( !Global.requestSemaphoreAccess("DisplayManager.saveAll", null) )
		  return;
	   
      Log.log( 7, "(DisplayManager.saveAll) enter" );
      final boolean[] ok = new boolean[1];
      final List<Future<?>> tasks = new ArrayList<Future<?>>();
      ok[0] = true;

      for ( Iterator<PwsFileContainer> it = getContainerIterator(); it.hasNext(); ) {
     	final PwsFileContainer file = it.next();
      	Runnable run = new Runnable() {
			@Override
			public void run() {
				Log.log(8, "(DisplayManager.saveAll.RUN) saving file " + file);
	            ok[0] &= file.saveFile(false);
			}
      	};
      	Future<?> ft = ActionHandler.startTask(run);
      	tasks.add(ft);
      }
      
      // waiting for tasks to complete
      while ( !tasks.isEmpty() ) {
    	  try { tasks.get(0).get();
    	  } catch (Exception e) {
    		  e.printStackTrace();
    	  }
    	  tasks.remove(0);
      }

      // inform user if not all files had save success
      if ( !ok[0] ) {
          Log.log( 7, "(DisplayManager.saveAll) -- not all files were saved! --" );
//    	  String msg = ResourceLoader.getDisplay("msg.warning.saveall");
    	  GUIService.infoMessage("dlg.saveall", "msg.warning.saveall");
      }
      
      Global.releaseSemaphoreAccess("DisplayManager.saveAll", null);
      fireElementModified();
      Log.log( 7, "(DisplayManager.saveAll) done" );
   }
   
   /**
    * Triggers off emergency save action on every open database file which is 
    * under administration of the display manager.
    * Files get saved in case they were modified, without dialog to
    * the user. If any save operation fails, <b>false</b> is returned.
    * 
    * @return boolean <b>true</b> if and only if all modified files were saved
    */
   public static boolean emergencySave () {
      boolean ok = true;
      
      // all Desktop files
      for ( Iterator<?> it = getContainerIterator(); it.hasNext(); ) {
         ok &= ((PwsFileContainer)it.next()).emergencySave();
      }
      return ok;
   }
   
    /**
    * Closes all elements in the JPWS desktop and removes their
    * views. The operation may be interrupted by user action or
    * IO failure.
    * 
    * @return boolean <b>true</b> if and only if all elements were removed
    */
   public static boolean closeAll () {
	  ActionHandler.checkForThread();
	  if ( !Global.requestSemaphoreAccess("DisplayManager.saveAll", null) )
		  return false;
	   
      Log.log( 7, "(DisplayManager.closeAll) enter Phase-1" );

      // close all non-modified frames
      for ( Iterator<PwsFileContainer> it = getContainerIterator(); it.hasNext(); ) {
    	 PwsFileContainer file = it.next();
         if ( !file.isModified() ) {
            if ( !file.close() )
               return false;
         }
      }
      Log.log( 7, "(DisplayManager.closeAll) End Phase-1: closed non-modified frames" );

      boolean ok = closeAllBut( null ); 
      Global.releaseSemaphoreAccess("DisplayManager.saveAll", null);
      return ok;
   }
   
   /**
    * Closes all containers in JPWS desktop except the one specified
    * as parameter.
    * 
    * @param ct PwsFileContainer or <b>null</b> for closing all
    * @return boolean true == all requested files closed, false == some closings broken
    */
   public static boolean closeAllBut ( PwsFileContainer ct ) {
	  ActionHandler.checkForThread();

	  Log.log( 7, "(DisplayManager.closeAllBut) start looking through desktop frames (DESKTOP MODUS)" );
	  final boolean[] ok = new boolean[1];
      final List<Future<?>> tasks = new ArrayList<Future<?>>();
      ok[0] = true;
      
      // run close for all eligible files, each in separate pool task
      // if any one fails (e.g. because of user break) ok[0] becomes false
      for ( Iterator<PwsFileContainer> it = getContainerIterator(); it.hasNext(); ) {
    	 final PwsFileContainer file = it.next();
         if ( file != ct ) {
        	Runnable run = new Runnable() {
				@Override
				public void run() {
					Log.log(8, "(DisplayManager.closeAllBut.RUN) closing file " + file);
					int modus = getFileCount() > 1 ? 1 : 0;
		            ok[0] &= file.close( modus );
				}
        	};
        	Future<?> ft = ActionHandler.startTask(run);
        	tasks.add(ft);
         }
      }

      // waiting for tasks to complete
      while ( !tasks.isEmpty() ) {
    	  try { tasks.get(0).get();
    	  } catch (Exception e) {
    		  e.printStackTrace();
    	  }
    	  tasks.remove(0);
      }
      
      // reset a "Save All" choice of the user triggered during this performance
      PwsFileContainer.saveAllTrigger = false;
      
      Log.log( 7, "(DisplayManager.closeAllBut) end closing desktop frames with status: " + ok[0]);
      return ok[0];
   }
   
   /**
    * Requests the display manager to make room for one additional container
    * to get registered. This may trigger closing of an open desktop element
    * in case the desktop has reached its capacity.
    *  
    * @return boolean <b>true</b> if and only if there is room for another
    *         container in desktop
    */
   public static boolean closeForOne () {
      JInternalFrame[] frames;
      DesktopContainer winCt;
      
      Log.log( 7, "(DisplayManager.closeForOne) enter" );

      // MULTI view state
      if ( displayState == DISPLAY_DESKTOP ) {
         frames = desktopPane.getAllFrames();
         if ( frames.length == maxDesktopFrames ) {
            // take oldest container to remove
            winCt = getOldestWin();
            // TODO container close (threaded)
            Log.log( 6, "(DisplayManager.closeForOne) attempting to close LEAST CT: ".concat( winCt.getName() ));
            return winCt.container.close();
         }
         return true;
      }
      
      // SINGLE view state
      if ( activeCT != null )
         return activeCT.close();

      return true;
   }
   
   /**
    * Closes all desktop pane frames belonging to the parameter 
    * file container. This does nothing to close the container
    * itself! Does nothing if container
    * is <b>null</b> or not an element of the desktop.
    * 
    * @param ct PwsFileContainer to be closed; may be <b>null</b>
    * @return boolean <b>true</b> if and only if ct is <b>null</b> or
    *         (the container was element of desktop and it has been 
    *         properly closed)
    */
   private static void closeFrames ( PwsFileContainer ct ) {
      boolean ok = false;
      
      Log.log( 6, "(DisplayManager.closeFrames) enter closing desktop frame for CT: ".concat( ct.getDatabaseName()) );
      if ( ct != null ) {
         DesktopContainer winCt = findContainerWindow ( ct );
         if ( winCt != null ) {
            winCt.dispose();
            fireWindowlistModified();
            ok = true;
         }
      }
      if ( !ok ) {
         Log.log( 6, "(DisplayManager.closeFrames) frame not available" );
      }
   }

   /**
    * Searches the <code>DesktopContainer</code> in the desktop pane list which 
    * wraps the parameter file container. This works independent of actual 
    * view state. Returns <b>null</b> for parameter <b>null</b>.
    *  
    * @param ct PwsFileContainer, may be <b>null</b>
    * @return <code>DesktopContainer</code> or <b>null</b> if not found
    */
   private static DesktopContainer findContainerWindow ( PwsFileContainer ct ) {
      
//      Log.log( 7, "(DisplayManager.findContainerWindow) ++ looking for CT: ".concat( ct.getDatabaseName() ) );
      if ( desktopPane != null && ct != null ) {
    	 JInternalFrame[] frames;
    	 synchronized (desktopPane) {
    	    frames = desktopPane.getAllFrames();
    	 }
         for ( int i = 0; i < frames.length; i++ ) {
        	DesktopContainer winCt = ((DesktopContainer)frames[ i ]);
            if ( winCt.container == ct ) {
//               Log.log( 7, "(DisplayManager.findContainerWindow) ++ found CT: ".concat( ct.getDatabaseName() ));
               return winCt;
            }
         }
      }
      
      Log.log( 7, "(DisplayManager.findContainerWindow) ++ CT not found: " + ct);
      return null;
   }
   
   /**
    * Returns the currently activated file container in the JPWS desktop
    * or <b>null</b> if no container is active.
    *  
    * @return <code>PwsFileContainer</code> or <b>null</b>
    */
   public static PwsFileContainer getSelectedContainer () {
      return activeCT;
   }
   
   /**
    * Whether there is a file container currently active (selected)
    * in the JPWS desktop.
    * @return boolean
    */
   public static boolean hasSelectedFile () {
      return getSelectedContainer() != null;
   }
   
   /**
    * Whether there are file containers registered in the JWPS
    * desktop.
    * 
    * @return boolean <b>true</b> if and only if there is at least one
    *         file container registered in the display manager
    */
   public static boolean hasOpenFiles () {
      return getFileCount() > 0;
   }

   public static boolean hasModifiedFiles () {
      for ( Iterator<?>it = getContainerIterator(); it.hasNext(); ) {
         if ( ((PwsFileContainer)it.next()).isModified() ) {
            return true;
         }
      }
      return false;
   }
   
   /** 
    * Returns the actual number of database files open in the desktop, regardless of
    * the actual display modus (SINGLE or DESKTOP).
    * 
    * @return int number of open files
    */
   public static int getFileCount () {
      int count = sortedWins.getSize();
      if ( displayState == DISPLAY_SINGLE & activeCT != null ) {
         count++;
      }
      return count;
   }
   
   // ******* LISTENERS ********
   
   public static void addChangeListener ( ChangeListener li ) {
	  synchronized (changeListeners ) { 
         if ( li != null && !changeListeners.contains( li ) ) {
            changeListeners.add( li );
         }
	  }
   }
   
   public static void removeChangeListener ( ChangeListener li ) {
	  synchronized (changeListeners ) { 
		  changeListeners.remove( li );
	  }
   }
   
   @SuppressWarnings("unchecked")
   private static void fireChangeEvent ( ChangeEvent evt ) {
      if ( changeListeners.size() > 0 ) {
         ArrayList<ChangeListener> copy = (ArrayList<ChangeListener>)changeListeners.clone();
         for ( ChangeListener i : copy )
            i.stateChanged( evt );
      }  
   }
   
   private static void fireSelectionChanged () {
      fireChangeEvent( new ChangeEvent( DisplayManager.class ) );
   }
   
   private static void fireWindowlistModified () {
      fireChangeEvent( new ChangeEvent( DisplayManager.class ) );
   }
   
   private static void fireElementModified () {
      fireChangeEvent( new ChangeEvent( DisplayManager.class ) );
   }
   
   private static void fireElementEdited () {
      fireChangeEvent( new ChangeEvent( DisplayManager.class ) );
   }
   
   // ********* F O N T  A D M I N I S T R A T I O N  ********************
   
   /**
    * Initialises a JPWS font identified by its canon name.
    * The actual font is preferred from a valid option setting. 
    * If no valid option setting exists, the system default 
    * for this font is installed. 
    *   
    * @param name canonical name of a JPWS font
    */
   private static void initCanonFont ( String name ) {
      Font font = fontForToken( "Font." + name );
      if ( font == null ) {
         font = getDefaultFont( name );
      }
      setFont( name, font );
   }

   /**
    * Whether the parameter font name is a system (JVM) font family name. 
    * @param fontFamily
    * @return boolean
    * since 0-5-0
    */
   private static boolean isSystemFontFamily ( String fontFamily ) {
      for ( int i = 0; i < avFonts.length; i++ ) {
         if ( avFonts[i].equalsIgnoreCase( fontFamily ) ) {
            return true;
         }
      }
      return false;
   }
   
   private static void setDisplayFont ( Font font ) {
      if ( font == null ) return;
      
      UIDefaults def = UIManager.getLookAndFeelDefaults();
      def.put( "TextField.font", font );
      def.put( "FormattedTextField.font", font );
      def.put( "TextArea.font", font );
      def.put( "TextPane.font", font );
      def.put( "EditorPane.font", font );
      def.put( "Table.font", font );
      def.put( "Tree.font", font );
      def.put( "List.font", font );
      
      PwsFileContainer ct = Global.getSelectedFile();
      if ( ct != null ) {
         ct.setFont( font );
      }
   }  // setDisplayFont

   private static void setControlFont ( Font font ) {
      if ( font == null ) return;
      
      Font boldFont = font.deriveFont( Font.BOLD );
      Font plainFont = font.deriveFont( Font.PLAIN );
      Font font1 = font.deriveFont( Font.BOLD + 
            (font.isItalic() ? Font.ITALIC : 0) );
    
      UIDefaults def = UIManager.getLookAndFeelDefaults();
      def.put( "Label.font", font1 );
      def.put( "CheckBox.font", font );
      def.put( "RadioButton.font", font );
      def.put( "Button.font", boldFont );
      def.put( "ComboBox.font", boldFont );
      def.put( "ProgressBar.font", boldFont );
      def.put( "Spinner.font", boldFont );
      def.put( "TabbedPane.font", boldFont );

      StatusBar statusBar = Global.getStatusBar();
      if (statusBar != null ) {
         statusBar.setFont( plainFont );
      }
   }  // setControlFont

   /** Returns a font identified by a canonical name or that has been
    *  previously stored by method <code>setFont</code>.
    *  If the name is a canonical JPWS application font name, a valid font 
    *  is guaranteed to be returned.
    *  Canonical names are: menu, data, password, notes, display, control,
    *  tooltip. 
    * 
    * @param name name of application font 
    * @return Font or <b>null</b> if and only if the name is unknown
    */ 
   public static Font getFont ( String name ) {
      Font font = fontStore.get( name );
      return font != null ? font : null;
   }

   /** Returns the default JPWS font for the parameter 
    *  canonical font name.
    * 
    * @param name canonical name of JPWS font
    * @return Font 
    */
   public static Font getDefaultFont ( String name ) {
      Font font = defaultFont;
      
      if ( name.equals( "menu" ) )
         font = menuDefault;
      else if ( name.equals( "tooltip" ) )
         font = menuDefault.deriveFont( Font.PLAIN, (float)14.0 );
      else if ( name.equals( "password" ) )
         font = passwordDefault;
      else if ( name.equals( "display" ) )
         font = displayDefault;
      
      return font;
   }
   
   /**
    * Stores a font for a given name. If the name is one of the canonical
    * application font names, the corresponding displays are set to adopt
    * the given font.
    * Canonical names are: menu, data, password, notes, display, control,
    * tooltip. 
    * 
    * @param name token for the given font
    * @param font <code>Font</code>
    */
   public static void setFont ( String name, Font font )
   {
      if ( name == null || name.length() == 0 || font == null )
         throw new IllegalArgumentException();
      
      // store font in map
      fontStore.put( name, font );
      
      // for some canonical names, modify display elements 
      if ( name.equals( "display" ) ) {
         setDisplayFont( font );
      
      } else if ( name.equals( "control" ) ) {
         setControlFont( font );
      
      } else if ( name.equals( "tooltip" ) ) {
         UIManager.getLookAndFeelDefaults().put( "ToolTip.font", font );
      }

      else if ( name.equals( "menu" ) ) {
         MenuHandler.setMenuFont( font );
      }
   }  // setFont

   /** Returns a Font from global program options or <b>null</b> if
    *  there is nothing defined or the definition cannot be realized
    *  in current VM (missing system Font).
    *  
    * @param token name of font in <code>Options</code>
    * @return Font or <b>null</b> if unavailable
    */
   private static Font fontForToken ( String token ) {
      Font font = null;
      String hstr;
      
      // try get latest user option
      if ( !(hstr = Options.getOption( token )).isEmpty()  )
      try {
         if ( isSystemFontFamily( hstr.substring( 0, hstr.indexOf( '-' ))) ) {
//System.out.println( "- retrieving font " + token + " from Options: " + hstr );
            font = Font.decode( hstr );
         }
      } catch ( Exception e ) {
         System.out.println( "*** FAULTY FONT ENTRY in options: " + token );
      }
      return font;
   }
   
   /** Stores a font with the specified name into global persistent options.
    *  
    * @param name name of font
    * @param font <code>Font</code>
    */
   public static void storeFont ( String name, Font font )
   {
      if ( name == null | font == null )
         throw new NullPointerException();
      
//System.out.println( "- store font to Options: " + fontCode( font ) );
      Options.setOption( "Font." + name, fontCode( font ) );
   }
   
   /** Returns a definition text for the given font. 
    * 
    * @param font font to be represented
    * @return font text representation or <b>null</b> if font == null 
    */
   public static String fontCode ( Font font ) {
      if ( font == null ) return null;
      
      int style = font.getStyle();
      String hstr = "PLAIN";
      if ( style == Font.BOLD )
         hstr = "BOLD";
      else if ( style == Font.ITALIC )
         hstr = "ITALIC";
      else if ( style == Font.ITALIC + Font.BOLD )
         hstr = "BOLDITALIC";
         
      return font.getFamily() + "-" + hstr + "-" + font.getSize();
   }
   
//  ***********  INNER CLASSES  *******************

private static class DesktopContainer extends JInternalFrame
{
   PwsFileContainer container;
   boolean initPhase;

   /**
    * Creates a new desktop frame for the parameter file container.
    * @param ct <code>PwsFileContainer</code>
    */
   public DesktopContainer ( PwsFileContainer ct ) {
      super( ct.getDatabaseName(), true, true, true, true );
	  ActionHandler.checkForEDT();

      Log.log( 7, "(DisplayManager.DesktopContainer) enter creation of NEW DESKTOP-CT: ".concat( ct.getDatabaseName() ) );
      initPhase = true;
      setToolTipText( ct.getFilePath() );
      setFrameIcon( ResourceLoader.getImageIcon( "pwsafe-logo" ) );
      getDesktopIcon().setToolTipText( ct.getFilePath() );
      setDefaultCloseOperation( JInternalFrame.DO_NOTHING_ON_CLOSE );
      addInternalFrameListener( ctListener );
      container = ct;
      setContentPane( ct.getView() );
      
      // layout
      Rectangle bounds = boundsStore.get( ct.getUUID() );
      if ( bounds != null ) {
         setBounds( bounds );
      } else {
         setSize( new Dimension( 350, 350 ) );
         Util.centreWithin( desktopPane, this );
      }
      adjustBounds();

      // make visible
      setVisible( true );
      initPhase = false;
      Log.log( 7, "(DisplayManager.DesktopContainer) NEW DESKTOP-CT created for: ".concat( ct.getDatabaseName() ) );
   }

   public PwsFileContainer getFileContainer () {
      return container;
   }
	   
   /**
    * Corrects the bounds of this container's window inside of the desktop pane
    * so that it meats minimum conditions of visibility and accessibility. 
    */
   private void adjustBounds () {
      Rectangle bounds;
      
      bounds = getBounds();
      bounds.width = Math.max( bounds.width, MINIMUM_WINDOWSIZE.width );
      bounds.height = Math.max( bounds.height, MINIMUM_WINDOWSIZE.height );
      setBounds( Util.correctedWindowBounds( desktopPane.getSize(), bounds, isResizable(), true ) );
      // TODO improved adjustment relative to all other windows in desktop (avoid identical bounds)
   }
   
   @Override
   public void dispose () {
	  // ensure this runs on EDT
	  if ( !SwingUtilities.isEventDispatchThread() ) {
		  disposeFrame();
		  return;
	  }
	   
      String dbName = container.getDatabaseName();  
      Log.log( 3, "(DisplayManager.DesktopContainer.dispose) disposing frame for: "
    		   .concat( dbName ) );

      // update the window bounds store with current values
      UUID uuid = container.getUUID();
      if ( uuid != null ) {
         boundsStore.put( uuid, getBounds() );
         recentWins.pushRecent( uuid );
      }

      removeInternalFrameListener( ctListener );
      super.dispose();
      
      synchronized ( sortedWins ) {
         Log.debug( 9, "(DisplayManager.DesktopContainer.dispose) removing from SortedWins: ".concat( dbName ) );
         sortedWins.removeRecent( this );
      }
      Log.log( 5, "(DisplayManager.DesktopContainer.dispose) frame disposed: ".concat( dbName ) );
   }

   /** Runs this frame's "dispose()" method on the EDT.
    */
   public void disposeFrame () {
      Runnable run = new Runnable() {
  		 @Override
  		 public void run() {
  			dispose();
  		 }
      };
      
      try {
		 ActionHandler.executeOnEDT_Wait(run);
	  } catch (InterruptedException e) {
	     e.printStackTrace();
	  } catch (InvocationTargetException e) {
		 e.printStackTrace();
	  }
   }
   
   @Override
   public String getName () {
      return container == null ? null : container.getDatabaseName();
   }

   @Override
	public String toString() {
	   String hstr = getName();
	   return "WinCt = ".concat(hstr == null ? "null" : hstr);
	}

   @Override
   public void setIcon (boolean iconified) throws PropertyVetoException {
	  // ensure this runs on EDT
	  if ( !SwingUtilities.isEventDispatchThread() ) {
		  setIconFrame(iconified);
		  return;
	  }
	   
      super.setIcon( iconified );
   }
   
   public void setIconFrame (final boolean iconified) {
      Runnable run = new Runnable() {
    	 @Override
    	 public void run() {
    		try {
 			   setIcon(iconified);
 		    } catch (PropertyVetoException e) {
 		    }
    	 }
      };
        
      try {
  		 ActionHandler.executeOnEDT_Wait(run);
  	  } catch (InterruptedException e) {
  	     e.printStackTrace();
  	  } catch (InvocationTargetException e) {
  		 e.printStackTrace();
  	  }
   }

   @Override
   public void setSelected ( boolean selected ) throws PropertyVetoException {
	  Log.log(10, "(DisplayManager.DesktopContainer.setSelected) pre-enter with " + selected
			  + ", Win=" + getName());
	  // ensure this runs on EDT
	  if ( !SwingUtilities.isEventDispatchThread() ) {
		  setSelectedFrame(selected);
		  return;
	  }
	  
	  boolean operate = selected != isSelected();
	  Log.log(10, "(DisplayManager.DesktopContainer.setSelected) enter with " 
	          + selected + ", operate=" + operate + ", Win=" + getName());
	  super.setSelected( selected );
      if ( !operate ) return;
      
      if ( selected ) { 
    	 synchronized ( sortedWins ) {
    	    Log.debug( 9, "(DisplayManager.DesktopContainer.setSelected) pushing in SortedWins: "
    	    		+ ", Win=" + getName());
    	    sortedWins.pushRecent( this );
         }
         if ( !initPhase ) {
            adjustBounds();
            makeActiveContainer( container );
         }

      } else {
         makeActiveContainer( null );
      }
   }
   
   public void setSelectedFrame ( final boolean selected ) {
      Runnable run = new Runnable() {
   		 @Override
   		 public void run() {
   			try {
				setSelected(selected);
			} catch (PropertyVetoException e) {
			}
   		 }
       };
       
       try {
 		  ActionHandler.executeOnEDT_Wait(run);
 	   } catch (InterruptedException e) {
 	      e.printStackTrace();
 	   } catch (InvocationTargetException e) {
 		  e.printStackTrace();
 	   }
   }
}  // class DesktopContainer
   
private static class ContainerListener extends InternalFrameAdapter 
             implements ChangeListener
{

   @Override
   public void stateChanged ( ChangeEvent e ) {
      int prop = ((PwsFileSocket.ChangeEvent)e).getState();
      PwsFileContainer ct = (PwsFileContainer)e.getSource();
      
      switch ( prop ) {
      case PwsFileContainer.OPERATION_MODE:
         switch ( ct.getOperationMode() ) {
         
         case PwsFileContainer.UNMOUNTED:
            Log.log( 7, "(DisplayManager.ContainerListener) received CT event: UNMOUNTED" ); 
            // de-activate container if it is the ACTIVE
            if ( ct == activeCT ) {
               makeActiveContainer( null );
            }
            // remove any desktop frames for container
            closeFrames( ct );
            containerLeaving( ct );
            break;

         case PwsFileContainer.MOUNTED_ACTIVE:
            requestActivationState( ct, STATE_ACTIVE );
            break;
            
         case PwsFileContainer.MOUNTED_PASSIVE:
            requestActivationState( ct, STATE_INACTIVE );
            break;
         }
         break;

      case PwsFileContainer.MODIFY_EVENT:
         DesktopContainer winCt;
         if ( (winCt = findContainerWindow( ct )) != null ) {
            winCt.setTitle( ct.getDatabaseName() );
         }
         fireElementModified();
         break;
         
      case PwsFileContainer.EDITOR_EVENT:
         fireElementEdited();
         break;
      }
   }

   @Override
   public void internalFrameActivated ( InternalFrameEvent e ) {
      Log.log( 7, "(DisplayManager.internalFrameActivated) " + e.getInternalFrame()); 
      requestActivationState( ((DesktopContainer)e.getInternalFrame()).container, STATE_ACTIVE );
   }

   @Override
   public void internalFrameDeactivated ( InternalFrameEvent e ) {
      Log.log( 7, "(DisplayManager.internalFrameDeactivated) + e.getInternalFrame()" ); 
      requestActivationState( ((DesktopContainer)e.getInternalFrame()).container, STATE_INACTIVE );
      
   }

   @Override
   public void internalFrameClosing ( InternalFrameEvent e ) {
      Log.log( 7, "(DisplayManager.internalFrameClosing)"  + e.getInternalFrame()); 
      ((DesktopContainer)e.getInternalFrame()).container.close();
   }
}

/** Iterator over all <code>PwsFileContainers</code> that are represented 
 *  in the Desktop at the time when an instance of this Iterator is created.
 *  The sorting follows user selection order with latest selected first.
 *  <p>Method <tt>Iterator.remove()</tt> is not supported and
 *  throws an exception.
 */
private static class ContainerIterator implements Iterator<PwsFileContainer>
{
   Iterator<?> windowsIterator;
   PwsFileContainer singleViewCt;

   /**
    * Creates a new ContainerIterator. 
    */
   private ContainerIterator () {
      synchronized ( sortedWins ) { 
         windowsIterator = ((RecentList)sortedWins.clone()).iterator();
         if ( !isDesktopView() ) {
            singleViewCt = getSelectedContainer();
         }
      }
   }

   @Override
   public boolean hasNext () {
      return singleViewCt != null ? true : windowsIterator.hasNext();
   }

   /** Returns the next element of type <code>PwsFileContainer</code>
    * of this Iterator.
    */
   @Override
   public PwsFileContainer next () {
      if ( singleViewCt != null ) {
    	 PwsFileContainer ct = singleViewCt;
         singleViewCt = null;
         return ct;
      }
      return ((DesktopContainer)windowsIterator.next()).getFileContainer();
   }

   @Override
   public void remove () {
      throw new UnsupportedOperationException();
   }
}

}
