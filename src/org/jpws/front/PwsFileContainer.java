/*
 *  PwsFileContainer2 in org.jpws.front
 *  file: PwsFileContainer2.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 19.11.2005
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
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;

import org.jpws.data.IOManager;
import org.jpws.data.OptionChangeEvent;
import org.jpws.data.OptionChangeListener;
import org.jpws.data.Options;
import org.jpws.data.PersistentOptions;
import org.jpws.data.PwsFileSocket;
import org.jpws.front.DatabaseHandler.FileAccessModus;
import org.jpws.front.Service.PwsListCompareResult;
import org.jpws.front.edit.EditorDialog;
import org.jpws.front.edit.EditorDialog.FormatVariant;
import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ActivitySource;
import org.jpws.front.util.AutoTextField;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.DocumentCheckAdapter;
import org.jpws.front.util.RecentList;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsFileEvent;
import org.jpws.pwslib.data.PwsFileListener;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.data.PwsRecordList;
import org.jpws.pwslib.exception.DuplicateEntryException;
import org.jpws.pwslib.exception.NoSuchRecordException;
import org.jpws.pwslib.exception.PasswordSafeException;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.UUID;
import org.jpws.pwslib.order.DefaultRecordWrapper;
import org.jpws.pwslib.order.OrderedListEvent;
import org.jpws.pwslib.order.OrderedListListener;


/**
 * A containment structure to deal with GUI handling of
 * a loaded <code>PwsFile</code> database. It is an extention of 
 * <code>PwsFileSocket</code>, which is the backend adapter for datafiles into
 * the multi-threaded application environment.
 * 
 *  <P>This class dispatches events of type <code>ChangeEvent</code>. The static member
 *  class of this class or the Java class <code>javax.swing.event.ChangeEvent</code> can be used
 *  to receive events. Only the member class, however, allows to identify the type of 
 *  property or event that has changed on the container. The Java interface  
 *  <code>javax.swing.event.ChangeListener</code> is used for listeners. There are seven 
 *  event types dispatched from this class:
 *  <BR>MODIFY_EVENT : dispatched on each modification of the content or definition of the database;
 *                     also dispatched when the file becomes "unmodified" (e.g. save performed)
 *  <BR>FILTER_MODE : dispatched when property "getFilterStatus()" changes to a new state  
 *  <BR>OPERATION_MODE : dispatched when property "getOperationMode()" changes to a new state  
 *  <BR>DISPLAY_MODE : dispatched when property "getViewType()" changes to a new state  
 *  <BR>SELECTION_EVENT : dispatched on each selection event occurring in the GUI record listing interface 
 *  <BR>SELECTION_STATUS : dispatched when property "getSelectionStatus()" changes to a new state  
 *  <BR>As a general rule, all event types are initially dispatched upon registration of a listener.  
 * 
 * {@link org.jpws.data.PwsFileSocket}    
 */
public class PwsFileContainer extends PwsFileSocket 
       implements ActionListener, OrderedListListener, OptionChangeListener, ActivitySource
{
   /** Record filter mode */
   public static final int FILTER_FAVOURITES = 5;

   /** Property identifier for operation mode. */
   public static final int OPERATION_MODE = 1;
   /** Property identifier for display mode. */
   public static final int DISPLAY_MODE = 2;
   /** Property identifier for selection mode. */
   public static final int SELECTION_STATUS = 3;
   /** Identifier for a selection event. */
   public static final int SELECTION_EVENT = 4;
   /** Identifier for a record editing event. */
   public static final int EDITOR_EVENT = 5;

   // View Type values
   public static final int TABLE_VIEW = 2;
   public static final int TREE_VIEW = 1;
   public static final int NO_VIEW = 0;
   
   // Selection Mode values
   public static final int NOTHING_SELECTED = 0;
   public static final int GROUP_SELECTED = 1;
   public static final int RECORD_SELECTED = 2;
   public static final int RECORDSET_SELECTED = 3;

   // Operation Mode values
   public static final int VIRGIN = 0;
   public static final int MOUNTED_PASSIVE = 1;
   public static final int MOUNTED_ACTIVE = 2;
   public static final int UNMOUNTED = 3;
   
   public static final int LEFT = 0;
   public static final int RIGHT = 1;
   public static final int WHEREIS = 2;
   
   private static final Color BACKUP_FILE_COLOR = new Color( 0xDA70D6 );  // orchid
   private static final Color MIRROR_FILE_COLOR = new Color( 0xFF6347 );  // tomato
   private static final Color LOCKED_SCREEN_COLOR = Color.LIGHT_GRAY;  // 
   
   public static final int OP_SAVE = 1;
   public static final int OP_BACKUP = 2;
   public static final int OP_RESTORE = 3;
   public static final int OP_SAVEAS = 4;
   public static final int OP_SAVECOPY = 5;
   public static final int OP_REVERT = 6;
   public static final int OP_CONVERT = 7;
   public static final int OP_REVERT_MIRROR = 8;

   private static final int QUICKFIND_DELAYTIME = 600;
   private static final Object CLOSE_LOCK = new Object();

   /** Global signal state to document user choice of "Save All" in 
    * "close-all" function. Reset after function which takes use of it.
    */
   public static boolean saveAllTrigger;

   
   private UndoManager          undoManager = new UndoManager();
   private RecordListTransferHandler transferHandler = new RecordListTransferHandler();
   private ArrayList<UndoableEditListener>   editListeners = new ArrayList<UndoableEditListener>();
   private ObjectChangeListener objectListener = new FC_ObjectListener(this);   
   private ContextFile          mirrorFile;
//   private AddDialog         	addDialog;
   private EditorDialog         editDialog;
           FileInfoDialog       fileInfoDlg;
   private TreeHandler          treeHandler;
   private TableHandler         tableHandler;
   private ContainerView        viewHandler;

   private RecentEntryList      lastEditedEntries;
   private RecentEntryList      lastUsedEntries;
   private RecentEntryList      favouriteEntries;
   private RecentList           recentFinds;
   private RecentList           searchExpressions;

   private int                  viewType = NO_VIEW;
   private int                  operationMode = VIRGIN;
   private int                  reportedViewType = -1;
   private int                  reportedSelectionMode = -1;
   private int                  reportedOperationMode = -1;
   private int                  normalViewType;
   private int                  modifyNumber;
   private int                  mirrorNumber;
   private int                  searchExprIndex;
   private int                  idleSeconds, maxIdleSeconds;
   private boolean              isIOProgress;
   private char                 escPrio = ' ';
   private boolean              isLockedView, allowLockedView;

   private TimerTask            secondsTimerTask;
   private byte[]               random = new byte[0];
   private Object               quickFindLock = new Object();
   private DocumentCheckAdapter quickfindAdapter;
   private KeyStrokeHandler		keyStrokeHandler;

   private JPanel               screenView;
   private JPanel               standardScreenPanel;
   private JPanel               emptyFilePanel, emptyFilteringPanel;
   private JScrollPane          scrollPane;
   private JLabel               filterBar;
   private JPanel               filterPanel;
   private JPanel               quickfindPanel;
   private AutoTextField        quickfindField;
   private JLabel               quickfindNumberLabel;
   private JLabel               isMirrorFileLabel;
   private JLabel               isBackupFileLabel;
   private JButton              naviLeftButton, naviRightButton;
   
/**
 *   Create a containment for the parameter <code>PwsFile</code>.
 */
public PwsFileContainer ( PwsFile file )
{
   super( file );
   
   init();
// THIS MAY BE SWITCHED ON FOR TESTING OF FILE STATE REPORTING   
//   addChangeListener( new EventReporter() );
}

@Override
public boolean addChangeListener ( ChangeListener li )
{
   if ( super.addChangeListener( li ) ) {
      li.stateChanged( new ChangeEvent( this, OPERATION_MODE ) );
      li.stateChanged( new ChangeEvent( this, DISPLAY_MODE ) );
      li.stateChanged( new ChangeEvent( this, SELECTION_STATUS ) );
      return true;
   }
   return false;
}

/**
 *  Adds an undoable edit listener to this container.
 * 
 * @param li
 * @since 0-5-0
 */
public void addUndoableEditListener ( UndoableEditListener li )
{
   synchronized( editListeners )
   {
      if ( !editListeners.contains( li ) )
         editListeners.add( li );
   }
}

/**
 *  Removes an undoable edit listener from this container.
 * 
 * @param li 
 * @since 0-5-0
 */
public void removeUndoableEditListener ( UndoableEditListener li )
{
   synchronized( editListeners )
   { editListeners.remove( li ); }
}

/**
 * Fires an undoable edit event to the list of registered edit listeners.
 * 
 * @param edit <code>UndoableEdit</code> used in <code>UndoableEditEvent</code> to be issued
 * @since 0-5-0
 */
public void fireEditEvent ( UndoableEdit edit )
{
   UndoableEditEvent evt;
   
   evt = new UndoableEditEvent( this, edit );
   synchronized( editListeners )
   {
      for ( UndoableEditListener i : editListeners )
         i.undoableEditHappened( evt ); 
   }
}

/** Returns the parent window for sub-window orientation. */
private Component window ()
{
   return Global.mainFrame;
}

/**
 * Loads mayor and minor file option values into class specific data structures.
 * 
 *  @since 0-4-0
 */
private void loadDataSubSystems ()
{
   PersistentOptions fopt;
   boolean oldPause = dbf.getEventPause();
   dbf.setEventPause( true );

   fopt = getMajorOptions();
   lastEditedEntries.setContent( fopt.getOption( "lastEditedEntries" ) );
//   lastEditedEntries.resetModified();
   favouriteEntries.setContent( fopt.getOption( "favouriteEntries" ) );
//   favouriteEntries.resetModified();

   fopt = getMinorOptions();
   lastUsedEntries.setContent( fopt.getOption( "lastUsedEntries" ));
//   lastUsedEntries.resetModified();
   recentFinds.setContent( fopt.getOption( "recentFindTexts" ) );
//   recentFinds.resetModified();
   searchExpressions.setContent( fopt.getOption( "recentFindTexts" ) );
//   searchExpressions.resetModified();
   dbf.setEventPause( oldPause );
}

private void init ()
{
   PwsRecord rec;
   JButton button, button2, button3;
   JPanel qfPanel, panel;
   JLabel label;
   FlowLayout flowLayout;
   Font font;
   Runnable findRun;
   String hstr;
   int len, vt;
   boolean isMirrorFile, isBackupFile;

   // starts the timer schedule for the idle seconds pulse
   secondsTimerTask = new SecondsTimerTask();
   Global.getTimer().scheduleAtFixedRate( secondsTimerTask, 10000, 1000 );
   maxIdleSeconds = Options.getIntOption( "viewCurtainTime" ) * 60;
   allowLockedView = Options.isOptionSet( "useContainerLockedView" );
   
   // replaces super classes' filtering module
   setRecordFilter( new RecordFilter() );
   
   isMirrorFile = isMirrorFile();
   isBackupFile = isBackupFile();
   len = Options.getIntOption( "usedEntryListLength" );
   lastEditedEntries = new RecentEntryList( len, "lastEditedEntries", false );
   lastUsedEntries = new RecentEntryList( len, "lastUsedEntries", true );
   favouriteEntries = new RecentEntryList( Integer.MAX_VALUE, "favouriteEntries", false );
   recentFinds = new PwsFileRecentList( Global.RECENTFIND_LIST_LENGTH, "recentFindTexts", true );
   searchExpressions =  new RecentList( 128 );
   searchExpressions.setContent( getMinorOptions().getOption( "recentFindTexts" ) );

//   loadFileOptions ();
   addUndoableEditListener( undoManager );
   getOrderedList().addOrderedListListener( this );
//   resetModified();

   // overall view structure
   filterPanel = new JPanel( new BorderLayout() );
   quickfindPanel = new JPanel( new BorderLayout() );
   panel = new JPanel( new VerticalFlowLayout( 0, true ) );
   panel.add( filterPanel );

   isMirrorFileLabel = new JLabel( ResourceLoader.getDisplay( "info.ismirrorfile" ),
         SwingConstants.CENTER );
   isMirrorFileLabel.setBackground( MIRROR_FILE_COLOR );
   isMirrorFileLabel.setOpaque( true );
   isMirrorFileLabel.setVisible( isMirrorFile );
   panel.add( isMirrorFileLabel );
   
   isBackupFileLabel = new JLabel( ResourceLoader.getDisplay( "info.isbackupfile" ),
         SwingConstants.CENTER );
   isBackupFileLabel.setBackground( BACKUP_FILE_COLOR );
   isBackupFileLabel.setOpaque( true );
   isBackupFileLabel.setVisible( isBackupFile & !isMirrorFile );
   panel.add( isBackupFileLabel );

   // the regular container view
   standardScreenPanel = new JPanel( new BorderLayout() ); 
   standardScreenPanel.setTransferHandler( getTransferHandler() );
   standardScreenPanel.add( panel, BorderLayout.NORTH );
   standardScreenPanel.add( quickfindPanel, BorderLayout.SOUTH );
   scrollPane = new JScrollPane(  );
   standardScreenPanel.add( scrollPane, BorderLayout.CENTER );

   screenView = new JPanel( new BorderLayout() );
   screenView.add( standardScreenPanel, BorderLayout.CENTER );
   
   keyStrokeHandler = new KeyStrokeHandler(standardScreenPanel);
   dbf.addFileListener(keyStrokeHandler);
   keyStrokeHandler.readContainerKeys();
//   standardScreenPanel.setActionMap(keyStrokeHandler.getActionMap());
//   standardScreenPanel.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
//		   keyStrokeHandler.getInputMap());
   
//   // register ESCAPE key for special terminate actions
//   stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
//   standardScreenPanel.registerKeyboardAction(
//         this, "ESCAPE", stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//
//   stroke = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0);
//   standardScreenPanel.registerKeyboardAction(
//         this, "RECORD_KEY", stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

   
   
   // empty dataView panel
   emptyFilePanel = new JPanel( new BorderLayout() );
   emptyFilePanel.setBackground( Color.white );
   emptyFilePanel.setBorder( new EmptyBorder( 20, 20, 20, 20 ) );
   emptyFilePanel.setName( "Empty File" );
   label = new JLabel( ResourceLoader.getDisplay( "info.emptyfile" ) );
   emptyFilePanel.add( label, BorderLayout.NORTH );
   emptyFilePanel.setTransferHandler( getTransferHandler() );
   
   // empty filtering dataView panel
   emptyFilteringPanel = new JPanel( new BorderLayout() );
   emptyFilteringPanel.setBackground( Color.white );
   emptyFilteringPanel.setBorder( new EmptyBorder( 20, 20, 20, 20 ) );
   label = new JLabel( ResourceLoader.getDisplay( "info.emptyfilterview" ) );
   emptyFilteringPanel.add( label, BorderLayout.NORTH );
   emptyFilteringPanel.setTransferHandler( getTransferHandler() );
   emptyFilteringPanel.setName( "Empty Filtering" );
   
   // filter bar and panel (consisting of label and terminate button)
   filterBar = new JLabel();
   filterBar.setBackground( new Color( 0x7FFFD4 ) );
   filterBar.setForeground( new Color( 0x4169E1 ) );
   filterBar.setOpaque( true );

   button = new JButton( ResourceLoader.getImageIcon( "filtercancel" ) );
   button.setToolTipText( ResourceLoader.getCommand( "tooltip.filter.cancel" ));
   button.setBorder( null );
   button.setOpaque( false );
   button.setActionCommand( "viewport.cancelfilter" );
   button.addActionListener( ActionHandler.getMainActionListener() );

   filterPanel.add( button, BorderLayout.EAST );
   filterPanel.add( filterBar, BorderLayout.CENTER );
   filterPanel.setBackground( filterBar.getBackground() );
   filterPanel.setBorder( BorderFactory.createEmptyBorder( 0, 8, 1, 5 ) );
   filterPanel.setVisible( false );
   
   // quick-find bar
   button = new JButton( ResourceLoader.getImageIcon( "filtercancel" ) );
   button.setToolTipText( ResourceLoader.getCommand( "tooltip.quickfind.cancel" ));
   button.setBorder( null );
   button.setOpaque( false );
   button.setActionCommand( "viewport.cancelquickfind" );
   button.addActionListener( this );

   // quick-find add-button
   button2 = new JButton( ResourceLoader.getImageIcon( "quickfind-add" ) );
//   button2.setPressedIcon( ResourceLoader.getImageIcon( "quickfind-add-pressed" ) );
   button2.setToolTipText( ResourceLoader.getCommand( "tooltip.quickfind.addword" ));
   button2.setBorder( null );
//   button2.setOpaque( false );
   button2.setActionCommand( "quickfind.addexpression" );
   button2.addActionListener( this );

   // quick-find remove-button
   button3 = new JButton( ResourceLoader.getImageIcon( "quickfind-remove" ) );
//   button3.setPressedIcon( ResourceLoader.getImageIcon( "quickfind-add-pressed" ) );
   button3.setToolTipText( ResourceLoader.getCommand( "tooltip.quickfind.removeword" ));
   button3.setBorder( null );
//   button3.setOpaque( false );
   button3.setActionCommand( "quickfind.removeexpression" );
   button3.addActionListener( this );

   // quick-find navigation-panel
   JPanel naviPanel = new JPanel();
   naviPanel.setOpaque(false);
   naviRightButton = new JButton( ResourceLoader.getImageIcon( "rightNavigation" ) );
   naviRightButton.setToolTipText( ResourceLoader.getCommand( "tooltip.quickfind.pagenext" ));
   naviRightButton.setBorder( null );
   naviRightButton.setFocusable(false);
   naviRightButton.setOpaque( false );
   naviRightButton.setActionCommand( "quickfind.pageright" );
   naviRightButton.addActionListener( this );
   naviLeftButton = new JButton( ResourceLoader.getImageIcon( "leftNavigation" ) );
   naviLeftButton.setToolTipText( ResourceLoader.getCommand( "tooltip.quickfind.pageprev" ));
   naviLeftButton.setFocusable(false);
   naviLeftButton.setBorder( null );
   naviLeftButton.setOpaque( false );
   naviLeftButton.setActionCommand( "quickfind.pageleft" );
   naviLeftButton.addActionListener( this );
   naviPanel.add( naviLeftButton );
   naviPanel.add( naviRightButton );
   
   flowLayout = new FlowLayout( FlowLayout.LEADING ); 
   qfPanel = new JPanel( flowLayout );
   qfPanel.setOpaque( false );
   quickfindField = new AutoTextField( getRecentFinds().getContent() );
   font = quickfindField.getFont();
   font = font.deriveFont( (float)(font.getSize2D() - 1.0));
   quickfindField.setFont( font );
   quickfindField.setColumns( 15 );
   quickfindField.setCaseSensitive( searchCaseSensitive );
   quickfindNumberLabel = new JLabel();
   quickfindNumberLabel.setFont( font );
   
   findRun = new Runnable() {
      @Override
	  public void run () {
          // get the current value from QF text input field
          String expr = quickfindField.getText();

          // call search executor
          quickFindRun( expr, false );
      }
   };
   quickfindAdapter = new DocumentCheckAdapter( findRun, QUICKFIND_DELAYTIME );
   quickfindField.getDocument().addDocumentListener( quickfindAdapter );

   label = new JLabel( ResourceLoader.getDisplay( "label.quickfind" ) );
   font = label.getFont();
   label.setFont( font.deriveFont( (float)(font.getSize2D() - 1.0)) );
   qfPanel.add( label );
   qfPanel.add( quickfindField );
   qfPanel.add( button2 );
   qfPanel.add( button3 );
   qfPanel.add( Box.createHorizontalStrut( 10 ) );
   qfPanel.add( naviPanel );
// qfPanel.add( Box.createHorizontalStrut( 5 ) );
   qfPanel.add( quickfindNumberLabel );
   
   quickfindPanel.setBackground( new Color( 0xF5DEB3 ) );
   quickfindPanel.add( qfPanel, BorderLayout.CENTER );
   quickfindPanel.add( button, BorderLayout.EAST );
   quickfindPanel.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 5 ) );
   quickfindPanel.setVisible( false );

   // set display VIEW TYPE (from file options or global default setting) 
   if ( (vt = getMinorOptions().getIntOption( "viewType" )) == 0 )
      vt = Options.getIntOption( "defaultViewType" );
   setViewType( vt == TABLE_VIEW ? TABLE_VIEW : TREE_VIEW );
   
   // set mirror modify number
   if ( isModified() )
      modifyNumber++;
   
   // calculate recent record random
   try {
      hstr = (String)lastUsedEntries.getFirst();
      rec = getRecord( new UUID( hstr ) );
      random = rec.getSignature();
   }
   catch ( Exception e )
   {}
}  // init

private JPanel createLockedScreenPanel ()
{
   JPanel panel;
   
   // locked screen panel
   if ( Global.isDecoration() ) {
	   // this is the seasonal decorated empty panel
	   panel = DisplayManager.createEmptyScreen();
   } else {
	   // this is the blank screen with a label panel
	   panel = new JPanel( new BorderLayout() );
	   panel.setBackground( LOCKED_SCREEN_COLOR );
	   panel.setBorder( new EmptyBorder( 20, 20, 20, 20 ) );
	   JLabel label = new JLabel(ResourceLoader.getDisplay( "info.lockedscreenview" ));
	   panel.add( label, BorderLayout.NORTH );
   }
   panel.addMouseListener( new MouseAdapter() {
      @Override
      public void mousePressed ( MouseEvent e ) {
         Log.debug( 6, "(PwsFileContainer.MouseAdapter -lockedScreenPanel) mouse pressed" );
         idleSeconds = 0;
         setLockedView( false );
      }
   } );

//	   // register SPACE key for locked view terminate action
//	   lockedScreenPanel.setFocusable( true );
//	   KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
//	   lockedScreenPanel.registerKeyboardAction(
//	         this, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
   return panel;
}

/**
 * Ensures the destruction of circular object references when this object is
 * no more needed for display purposes.
 *
 */
private void exitDisplay ()
{
   if ( viewType != NO_VIEW ) {
      if ( editDialog != null ) {
         editDialog.dispose();
         editDialog = null;
         fireChangeEvent( EDITOR_EVENT );
      }
      
      if ( treeHandler != null )
         treeHandler.destruct();
      if ( tableHandler != null )
         tableHandler.destruct();
      
      treeHandler = null;
      tableHandler = null;
      viewHandler = null;
      viewType = NO_VIEW;
      
      setDataView( null );
      reportPropertyChange( DISPLAY_MODE );
   }
}  // exitDisplay

/** Creates new versions of the view handlers assuming that this object's database
 *  might be a new one. This will setup the display.
 *  
 *  @param v type of dataView to be invoked
 */
private void initDisplay ( int v )
{
   UUID uuid;
   String hstr;
   int i;
   
   treeHandler = new TreeHandler( this );
   treeHandler.addActivityListener( objectListener );
   tableHandler = new TableHandler( this );
   tableHandler.addActivityListener( objectListener );
   viewType = v;
   viewHandler = viewType == TABLE_VIEW ? (ContainerView)tableHandler 
         : treeHandler;
   
//   boolean allowDelete = Options.isOptionSet( "useUndoRedo" );
//   if ( treeHandler != null )
//      treeHandler.setDeleteKeyEnabled( allowDelete );
//   if ( tableHandler != null )
//      tableHandler.setDeleteKeyEnabled( allowDelete );
   
//   setFont( DisplayManager.getFont( "display" ) );
   
   // sets up the display with cleared selection (optionally showing a tree expanded)
   setupDisplay( new int[0] );
   if ( Options.isOptionSet( "autoExpandTree" ))
      setExpandedBranch( "", true );

   // look for LAST-SELECTED record info in minor options
   hstr = getMinorOptions().getOption("selectedRecord");
   if ( hstr.isEmpty() ) {
	  hstr = (String)lastUsedEntries.getFirst();
   }

   // if there is a last-used entry, set it selected in display
   if ( hstr != null && !hstr.isEmpty() ) {
      uuid = new UUID( hstr );
      i = getOrderedList().indexOf( new PwsRecord( uuid ) );
      viewHandler.setSelectedIndex( i );
   }

   reportPropertyChange( SELECTION_STATUS );
   reportPropertyChange( DISPLAY_MODE );
}  // initDisplay

/** Returns the displayable data (record list) UI of this container. Handle with care!
 *  @return JPanel
 */
public JPanel getView ()
{
   return screenView;
}

/** Returns the actual random value available within this object.
 *  @return byte[] with random data (may be of length 0) 
 *  @since 0-5-0
 */  
public byte[] getRandomValue ()
{
   return random;
}

/** Returns the currently active record editor dialog or <b>null</b>
 * if unavailable.
 * 
 * @return <code>AddDialog</code>
 * @since 0-5-0
 */
public EditorDialog getEditDialog ()
{
   return editDialog;
}

/** Returns the <code>UndoManager</code> of this file container.
 * 
 * @return <code>org.jpws.front.UndoManager</code>
 * @since 0-6-0
 */
public UndoManager getUndoManager ()
{
   return undoManager;
}

public TransferHandler getTransferHandler ()
{
   return transferHandler;
}

/** Invokes a dialog to create and add a password entry (record) into the
 *  database of this container.
 */ 
public void addEntry() {
   if ( editDialog != null ) {
      editDialog.show();
   } else {
      editDialog  = new EditorDialog(this, getSelectedGroupName(), FormatVariant.pws3);
      fireChangeEvent( EDITOR_EVENT );
      editDialog.show();
   }
   grabFocus();
}  // addEntry

///** Invokes a dialog to edit an existing password entry (record) in the
// *  database of this container. (The resulting record state will not be
// *  reflected into the parameter object; instead this container class takes
// *  care to update a modified record properly into the underlying database.)
// */ 
//public void editEntry2 ( PwsRecord record )
//{
//	// if a dialog is still available, return to editing (may be effect of error states)
//	if ( addDialog != null ) {
//      addDialog.show();
//      
//   // otherwise create + run dialog for parameter record   
//   } else {
//      addDialog = new AddDialog( Global.getActiveFrame(), (PwsRecord)record.clone() );
//      addDialog.setExternalActions( this );
//      addDialog.addActivityListener( objectListener );
//      fireChangeEvent( EDITOR_EVENT );
//      addDialog.show();
//   
//      // updates the "record used" marker
//      if ( Options.isOptionSet( "useEntryOnOpen" ) ) {
//         recordUsed( record );
//      }
//      
//      setSelectedRecord( record );
//   }
//   grabFocus();
//}  // editEntry

/** Invokes a dialog to edit an existing password entry (record) in the
 *  database of this container. (The resulting record state will not be
 *  reflected into the parameter object; instead this container class takes
 *  care to update a modified record properly into the underlying database.)
 */ 
public void editEntry ( PwsRecord record )
{
	// if a dialog is still available, return to editing (may be effect of error states)
	if ( editDialog != null ) {
      editDialog.show();
      
   // otherwise create + run dialog for parameter record   
   } else  {
      setSelectedRecord( record );
      editDialog = new EditorDialog(this, record, FormatVariant.pws3);
//    editDialog2.addActivityListener( objectListener );
      Log.log(6, "(PwsFileContainer.editEntry2) start editing record");
      fireChangeEvent( EDITOR_EVENT );
      editDialog.show();
      Log.log(6, "(PwsFileContainer.editEntry2) editing record finished");

//      // after-care logic
//      if ( editDialog2.isRecordModified() ) {
//         // update record if modified during edit session
//    	 PwsRecord result = editDialog2.getRecord(); 
//         try {
//            Log.log(8, "(PwsFileContainer.editEntry2) updating record in database");
//			updateRecord( result );
//	        random = result.getSignature();
//		 } catch (NoSuchRecordException e) {
//			Log.debug(5, "(PwsFileContainer.editEntry2) **** ERROR **** Cannot update EDIT RECORD!");
//			e.printStackTrace();
//		 }
//
//         // notify subsystems about record update event
//         if ( editDialog2.getInitRecord().isValid() ) {
//            // create undoable edit object
//            UndoableEdit modEdit = new UndoManager.ModifyRecordEdit( 
//            	  UndoManager.ModifyRecordEdit.MODIFY_RECORD_EDIT,
//                  this, editDialog2.getInitRecord(), record );
//            fireEditEvent( modEdit );
//            
//            // push to "last-edited" list
//            lastEditedEntries.pushRecent( record );
//            GUIService.statusConfirm("msg.confirm.modifyrec", result.getTitle() );
//            fileUpdated();
//         }
//      }
      
      // updates the "record used" marker (if opted for this kind of event)
      if ( Options.isOptionSet( "useEntryOnOpen" ) ) {
         recordUsed( record );
      }
      
//      editDialog2 = null;
//      fireChangeEvent( EDITOR_EVENT );
   }
   grabFocus();
}  // editEntry2

/** Updates the specified record in this file if and only if it is already
 *  an element of this file and it is semantically valid.
 * 
 *  @param rec record to be updated 
 *  @throws NoSuchRecordException if the parameter record is unknown
 *  @throws IllegalArgumentException if the record is not valid
 */
@Override
public void updateRecord ( PwsRecord rec ) throws NoSuchRecordException
{
   super.updateRecord( rec );
   setSelectedRecord( rec );
   Log.debug( 5, "(PwsFileContainer.updateRecord) -- updating record: ".concat( rec.getRecordID().toString() ));
}

/** Updates the specified record in this file if and only if it is already
 *  an element of this file.
 * 
 *  @param rec record to be updated 
 *  @throws NoSuchRecordException if the parameter record is unknown
 */
@Override
public void updateRecordRelaxed ( PwsRecord rec ) throws NoSuchRecordException
{
   super.updateRecordRelaxed( rec );
   setSelectedRecord( rec );
   Log.debug( 5, "(PwsFileContainer.updateRecordRelaxed) -- updating record (relaxed): ".concat( rec.getRecordID().toString() ));
}

public void updateUI ()
{
   if ( treeHandler != null )
      treeHandler.updateUI();
   if ( tableHandler != null )
      tableHandler.updateUI();
}

@Override
public String toString () {
	return getDatabaseName();
}

/**
 * Displays the file info dialog window (non-modal outside of mainframe).
 * @since 0-4-0
 */
public void showFileInfo () {
   if ( fileInfoDlg == null ) {
      fileInfoDlg = new FileInfoDialog( this );
   }  else {
      fileInfoDlg.show();
   }
}

/** Adds a record to this file and adjusts GUI devices of this file to display 
 *  and select the parameter record. 
 * 
 * @param rec
 * @throws DuplicateEntryException if this record already exists in this file
 * @throws IllegalArgumentException if this record is not valid
 */
@Override
public void addRecord ( PwsRecord rec ) throws DuplicateEntryException
{
   super.addRecord( rec );
   setSelectedRecord( rec );
}

/** Adds a record to this file (and adjusts GUI devices) without control of its
 * semantical validity. There is, however, control of uniqueness of its UUID. 
 * 
 * @param rec
 * @throws DuplicateEntryException if this record already exists in this file
 */
@Override
public void addRecordRelaxed ( PwsRecord rec ) throws DuplicateEntryException
{
   super.addRecordRelaxed( rec );
   setSelectedRecord( rec );
}

// since 0-5-0
@Override
public boolean emergencySave ()
{
   boolean ok, oldEventPause;
   
   // save open modified editor record (in an essential modus)
   if ( editDialog != null && editDialog.isRecordModified() ) {
	  PwsRecord record = editDialog.getRecord();
      try { 
         if ( editDialog.isNewRecord() )
            addRecordRelaxed( record );
         else
            updateRecordRelaxed( record );
            lastEditedEntries.pushRecent( record );

      } catch ( Exception e ) {
      }
   }

   // prevent exhausting event digestion 
   oldEventPause = isEventPaused();
   setEventPause( true );
   
   if ( ok = super.emergencySave() ) {
      removeMirrorFile();
   }

   setEventPause( oldEventPause );
   return ok;
}  // emergencySave

private static final int SAVE_FILE = 0;
private static final int SAVE_AS = 1;
private static final int SAVE_COPY = 2;
private static final int SAVE_BACKUP = 3;

/** Returns the most applicable definition for a a backup file of this container.
 * 
 * @return File
 */
private File getBackfileDefinition () 
{
	File backfile = null;
	
	// prefer a memorised output file path  
	String path = getMinorOptions().getOption("backupPath");
	if ( !path.isEmpty() ) {
	   backfile = new File( path );
	}
	 
	// if there is no valid output file path
	if ( backfile == null || backfile.getParent() == null ||
	     !backfile.getParentFile().isDirectory() ) {

		// preset standard file name in BACKUP directory
	   path = Util.fileNameOfPath( getFilePath() );
	   int pos = path.lastIndexOf( "." );
	   if ( pos > -1 ) {
	      path = path.substring( 0, pos ); 
	   }
	   Global.verifyBackupDir();
	   backfile = new File( Global.backDir, path + Global.DEFAULT_BACKUPEXTENTION );
	}
	return backfile;
}

/** Saves the tree expansion string to minor options if this is feasible by
 * current states and preferences. Always returns the current expansion info.
 * 
 * @return String tree expansion info
 */
private String saveTreeExpansionInfoToMinors () {
	// obtain expansion string from tree handler
	String expansionInfo = treeHandler.getTreeExpansionInfo();
	  
    // store into MINOR file options if it does not cause modification
    if (isModified() || !Options.isOptionSet("storeMinorChanges")) {
       getMinorOptions().setOption("treeExpansionString", expansionInfo);
       Log.log( 9, "(PwsFileContainer) tree expansion info saved to minor options: "
      		 .concat(expansionInfo) );
    }
    return expansionInfo;
}

/**
 * Dialog intensive function to create a copy of the PwsFile of this container 
 * to a user selectable output file with an optional user definable passphrase. 
 * The PwsFile of this container is unchanged.
 * 
 * @param modus operation modus (SAVE_FILE, SAVE_AS, SAVE_COPY, SAVE_BACKUP)
 * @param confirm whether operation confirm messages are allowed
 * 
 */
private boolean saveCopyIntern ( int modus, boolean confirm )
{
   String filePath, filename, newFilepath;
   PwsPassphrase pass;
   ContextFile file, newFile;
//   URL url, newFileUrl;
   File selectFile;
   String title, hstr, titleToken, confirmMsg, confirmOpt;
   int filetype, opcode;
   boolean fileExists, isNew, backup, saveAs, copy, save, ok;
   
   if ( isIOProgressing() ) return false;
//   if ( isCriticalPhase() ) return false;
   
   isIOProgress = true;
   
   // analyse operation modus 
   backup = modus == SAVE_BACKUP;
   saveAs = modus == SAVE_AS;
   copy = modus == SAVE_COPY;
   save = modus == SAVE_FILE;
   
   // arrange variables after operation modus
   if ( backup )
   {
      titleToken = "dlg.savebackup";
      confirmMsg = "msg.confirm.backup";
      confirmOpt = "confirmBackup";
      opcode = OP_BACKUP;
   }
   else if ( save )
   {
      titleToken = "dlg.savefile";
      confirmMsg = "msg.confirm.savefile";
      confirmOpt = "confirmSave";
      opcode = OP_SAVE;
   }
   else if ( saveAs )
   {
      titleToken = "dlg.saveas";
      confirmMsg = "msg.confirm.saveas";
      confirmOpt = "true";
      opcode = OP_SAVEAS;
   }
   else if ( copy )
   {
      titleToken = "dlg.savecopy";
      confirmMsg = "msg.confirm.savecopy";
      confirmOpt = "true";
      opcode = OP_SAVECOPY;
   }
   else
      throw new IllegalArgumentException();
   
   // control legal access by requesting existing passphrase
   if ( backup | save || !Options.isOptionSet("createFileCheck") ||
      GUIService.passwordControl( this ) )
   {
      isNew = !hasPersistentFile();
      file = getContextFile();
      filePath = getFilePath();
      pass = getPassphrase();

//      // request critical phase for this
//      if ( !Global.requestCriticalPhase( file ) ) {
//         isIOProgress = false;
//         return false;
//      }
      
      // for save existing file take existing filepath
      if ( save & !isNew ) {
         newFile = file;
         
      // otherwise loop user choice of destination until IO-Manager says ok  
      // or user breaks the dialog 
      } else {

          // prepare file chooser  
          hstr = isNew ? save ? "" : "?" : Util.fileNameOfPath( filePath );
          title = ResourceLoader.getDisplay( titleToken ) + hstr;
          filetype = backup ? GUIService.BACKUPFILE_CHOOSER : GUIService.PWSFILE_CHOOSER;
          selectFile = null;

          // create file chooser preset value for file path
          if ( backup ) {
        	 if ( !hasPersistentFile() ) {
        		 selectFile = new File( Global.backDir, getDatabaseName() 
            				  + Global.DEFAULT_BACKUPEXTENTION );
        	 } else {
        		 selectFile = getBackfileDefinition();
        	 }
          } else if ( isNew ) {
        	  selectFile = new File( Global.currentDir, getDatabaseName() 
     				 	   + Global.DEFAULT_FILEEXTENTION );
          }
          
    	  do  {
	         newFile = GUIService.chooseSaveFile( window(), title.trim(), 
	               filetype, -1, selectFile );
	         ok = true;
	         if ( newFile != null ) {
	            ok = IOManager.access_allowed( newFile, false );
	            if ( !ok ) {
	               hstr = ResourceLoader.getDisplay( "msg.io_conflict.general" );
	               GUIService.failureMessage( hstr, null );
	            }
	         }
	      } while ( !ok );
      }

      // if valid file choice available
      if ( newFile != null ) {

    	 // save tree expansion string to minor options
    	 String expansionInfo = saveTreeExpansionInfoToMinors();
    	  
         // store tree expansion string into file header field
    	 setHeaderField( HEADERFIELD_TREEINFO, expansionInfo );
         Log.log( 9, "(PwsFileContainer.saveCopyIntern) tree expansion info saved to header field: "
        		 .concat(expansionInfo) );

    	 newFilepath = newFile.getFilepath();
         filename = Util.fileNameOfPath( newFilepath );
         pass = null;

         if ( !save | isNew )
         try { 
            // check if file already exists
            // let user confirm overwrite if file exists
            fileExists = newFile.exists(); 
            if ( (fileExists && !GUIService.overwriteConfirm( window(), newFilepath )) ||
            
               // check if user wants to set a new passphrase (only for COPYFILE)
               ( copy && GUIService.userConfirm( window(), "msg.ask.newpassword" ) &&
                 (pass = GUIService.enterNewPassphrase( this, filename )) == null ) )
               {
//                  Global.endCriticalPhase( file );
                  isIOProgress = false;
                  return false;
               }

         } catch ( Exception e ) {
            GUIService.failureMessage( "IO Error", e );
//            Global.endCriticalPhase( file );
            isIOProgress = false;
            return false;
         }
               
         // invoke copy
         try {
//            Global.endCriticalPhase( file );
            if ( saveAs | (save & isNew) ) {
               super.saveAs( newFile, isNew );
            } else if ( save ) {
               super.saveFile();
            } else {
               super.saveCopy( newFile, pass, backup );
               if ( backup ) {
            	   getMinorOptions().setOption("backupPath", newFile.getFilepath());
               }
            }
            
            // delete mirror file
            if ( saveAs | save ) {
               removeMirrorFile();
            }
            
            // delete old file if it is a mirror file
            if ( saveAs & isMirrorFile() ) {
               Global.removeRandomMirror( file );
            }

            // ensure adjustment of panel display (e.g. based on file type)
            if ( saveAs | isNew ) {
               refreshView();
            }

         } catch ( Exception e ) {
            GUIService.failureMessage( "msg.saveerror", e );
            e.printStackTrace();
//            Global.endCriticalPhase( file );
            isIOProgress = false;
            return false;
         }
         
         // update GUI
         if ( confirm ) {
            GUIService.statusConfirm( confirmMsg, newFilepath );
            if (confirmOpt.equals("true") || Options.isOptionSet(confirmOpt)) {
               confirmOperation( opcode, newFilepath );
            }
         }
      }

      // revoke critical phase
//      Global.endCriticalPhase( file );
   }
   isIOProgress = false;
   return true;
}  // saveCopyIntern

/**
 * Dialog intensive function to create a copy of the PwsFile of this container 
 * to a user selectable output file with an optional user definable passphrase. 
 * The PwsFile of this container is unchanged.
 * 
 * @param backup whether a backup copy is to be performed
 * 
 */
public void saveCopy ( boolean backup )
{
   saveCopyIntern( backup ? SAVE_BACKUP : SAVE_COPY, true );
}

/**
 * Makes a copy of the PwsFile of this container to a user selectable output
 * file. The PwsFile of this container keeps its original filepath only under
 * the circumstance that the process was broken by the user. Otherwise any
 * chosen output file will remain active. 
 * 
 */
public boolean saveAs ()
{
   return saveCopyIntern( SAVE_AS, true );
}  

/**
 * Saves the PwsFile of this container. GUI-active.
 * 
 * @param confirm boolean if <b>true</b> a positive operation success will
 *        be reported to the user in case a file-write operation was performed
 * @return <b>true</b> if and only if the file is regularly saved or void (closed) 
 *         after this routine terminates 
 */
public boolean saveFile ( boolean confirm )
{
   boolean ok;
   
   if ( !isModified() ) return true;
   if ( isIOProgressing() ) return false;
//   if ( isCriticalPhase() ) return false;
   
   try {
      // warn user about failing write access of persistency IO-context
      String hstr = getFilePath();
      if ( hstr != null && !getApplication().canWrite( hstr ) ) {
         hstr = ResourceLoader.getDisplay( "msg.url.writedenied" );
         hstr = Util.substituteText( hstr, "$adapter", getApplication().getName() );
         GUIService.infoMessage( window(), "dlg.operrejected", hstr );
         return false;
      }

      // Save function including path-less state (new file)
      ok = saveCopyIntern( SAVE_FILE, confirm );

   } catch ( IOException e ) {
      e.printStackTrace();
      ok = false;
   }
   return ok;
}  // saveFile

/** Confirms the success of an operation performed on the database of this class. */
private void confirmOperation ( int operation, String filename )
{
   confirmOperation( window(), operation, filename );
}

/** Confirms the success of an operation performed on the database of this class. */
public void confirmOperation ( Component owner, int operation, String filename )
{
   String text, token;
   
   text = ResourceLoader.getDisplay( "confirm.fileaction" );
   text = Util.substituteText( text, "$target", filename );
   switch ( operation )
   {
      case OP_BACKUP : token = "confirm.backup";
      break;
      case OP_RESTORE : token = "confirm.restore";
      break;
      case OP_SAVE   : token = "confirm.save";
      break;
      case OP_SAVEAS : token = "confirm.saveas";
      break;
      case OP_SAVECOPY : token = "confirm.savecopy";
      break;
      case OP_REVERT : token = "confirm.revert";
      break;
      case OP_REVERT_MIRROR : token = "confirm.revert.mirror";
      break;
      case OP_CONVERT : token = "confirm.convert";
      break;
      default : token = null;
   }
   text = Util.substituteText( text, "$action", ResourceLoader.getDisplay( token ) );
   GUIService.infoMessage( owner, "confirm.operation", text );
}  // confirmOperation

/**
 * Attempts to open the specified file as a <code>PwsFile</code> database. 
 * In the first step the current passphrase of this container is tried. If fails, 
 * a user input loop is initiated to query for the correct passphrase.
 * 
 * @param file <code>ContextFile</code> persistent state to be accessed 
 * @param directAccess boolean if <b>true</b> the IO-Manager's database cache
 *        is ignored and the file accessed directly via file-IO (still in the 
 *        framework of the IO-Manager)
 * @return a <code>PwsFile</code> object or <b>null</b> if the attempt failed
 */
private PwsFile passTryOpen ( ContextFile file, FileAccessModus acm )
{
   return DatabaseHandler.passTryOpen( file, getPassphrase(), acm );
}

private PwsPassphrase enterNewPassphrase ()
{
   return GUIService.enterNewPassphrase( this, getDatabaseName() );
}  // enterNewPassphrase

/**
 * Dialog active function to let the user modify the access passphrase for
 * the Pws database of this container.
 *
 */
public void changePassphrase ()
{
   PwsPassphrase pass, oldPass;
   UndoableEdit edit;

   if ( isIOProgressing() ) return;
// if ( isCriticalPhase() ) return;
   
   isIOProgress = true;
   oldPass = getPassphrase();
   
   // if have a passphrase
   // control legal access by requesting existing passphrase
   if ( !(oldPass != null && !GUIService.passwordControl( this )) 
   
   // enter new passphrase
        && (pass = enterNewPassphrase()) != null )
   {
      
      // assign new passphrase
      setPassphrase( pass );
      
      // create undoable edit event
      edit = new UndoManager.ModifyPasswordEdit( this, oldPass, pass );
      fireEditEvent( edit );

      // user confirmation
      GUIService.statusConfirm("msg.confirm.passmodify");
   }
   
   isIOProgress = false;
}  // changePassphrase

/** Performs a delete command; discriminates between entry selection and
 *  group selection within the list display.
 */
public void deleteCommand ()
{
	Log.log(10, "(PwsFileContainer.deleteCommand) --- ");
   int s = getSelectionStatus();
   if ( s == GROUP_SELECTED ) {
      deleteGroupDlg( getSelectedGroupName() );
   } else if ( s == RECORD_SELECTED || s == RECORDSET_SELECTED ) {
      deleteEntriesDlg( getSelectedRecords() );
   }
}

/** Performs a dialog to delete a group of file entries.
 *  This function works independently from display selection (but may get called
 *  in consequence of a selection). It currently does not operate when a display filter
 *  is set. 
 *  
 *  @param group String, a PWS normalized GROUP value
 */
public void deleteGroupDlg ( String group )
{
   DefaultRecordWrapper[] recs;
   String hstr;
   int count;
   
   if ( isIOProgressing() || (count = getGroupRecords( group ).length) == 0 )
      return;
   
   hstr = ResourceLoader.getDisplay("msg.delete.group");
   hstr = Util.substituteText( hstr, "$group", group );
   hstr = Util.substituteText( hstr, "$count", String.valueOf( count ));
   
   // ask user to confirm (if opted)
   if ( GUIService.userConfirm( window(), hstr ) ) {
      recs = deleteGroup( group );
      clearSelection();
      
      // fire undoable edit event
      fireEditEvent( new UndoManager.DeleteRecordEdit( this, recs, group ) );
   }
   GUIService.statusConfirm("msg.confirm.deleterec");
}  // deleteGroupDlg

/**
 * Performs a dialog to delete a given set of selected file entries.
 * Does nothing if the selection is empty.
 *  
 * @param selection <code>DefaultRecordWrapper[]</code>, may be <b>null</b>
 */
public void deleteEntriesDlg ( DefaultRecordWrapper[] selection )
{
   if ( isIOProgressing() ) return;
   
   if ( selection != null & selection.length > 0 ) {
      int size = selection.length;
      String hstr;
      
      // determine correct user message for confirm request
      if ( size == 1 ) {
         // prepare user confirm text
         hstr = ResourceLoader.getDisplay("msg.ask.deleterec");
         hstr = Util.substituteText( hstr, "$name", selection[0].toString() );
      } else {
         // prepare user confirm text
         hstr = ResourceLoader.getDisplay("msg.ask.deletemultirec");
         hstr = Util.substituteText( hstr, "$amount", String.valueOf( size ));
      }  

      // ask user to confirm (if opted)
      if ( !Options.isOptionSet("confirmDeleteRecord") ||
           GUIService.userConfirm( window(), hstr ) ) {

    	 // delete entries
         deleteEntries( selection );
         clearSelection();
         
         // fire undoable edit event
         fireEditEvent( new UndoManager.DeleteRecordEdit( this, selection, null ) );
         GUIService.statusConfirm("msg.confirm.deleterec");
      }
   }
}  // deleteEntriesDlg

/**
 * Removes the parameter records from the database of this container under the edit type
 * "TRANSFER_REMOVED".
 *   
 * @param recs DefaultRecordWrapper[], may be <b>null</b>
 * @param group String name of group indicating group-delete occurred, may be <b>null</b>
 */
public void deleteTransferred ( DefaultRecordWrapper[] records )
{
   if ( records != null && records.length != 0 ) {
      // get out true copies of the parameter record list from the file socket
      // (parameter seen as UUID-list here as the content of records may be modified
      //  and this had a bad impact on undo/redo operations)
	  DefaultRecordWrapper[] recs = new DefaultRecordWrapper[ records.length ];
      for ( int i = 0; i < records.length; i++ )
         recs[i] = new DefaultRecordWrapper( getRecord( records[i].getRecordID() ), null );
      
      // remove the entries
      deleteEntries( recs );
      
      // fire undoable edit event
      fireEditEvent( new UndoManager.DeleteRecordEdit( this, recs, null ) );
   }
}

/** Attempts to import (modus: MOVE) the currently selected group or records
 *  from the source database (container) to this container.
 *  
 * @param source
 */
public void importSelectedFrom ( PwsFileContainer source, boolean copy )
{
   DefaultRecordWrapper[] recs;
   String group = null;
   
   if ( source.getSelectionStatus() == GROUP_SELECTED ) {
      group = source.getSelectedGroupName();
      recs = source.getGroupRecords( group );
   } else { 
      recs = source.getSelectedRecords();
   }

   if ( recs != null ) {
	  Transferable trans = new RecordListTransferable( source, recs, group );
      transferHandler.dropAction = copy ? DnDConstants.ACTION_COPY : DnDConstants.ACTION_MOVE;
      transferHandler.importData( null, trans );
   }
}

/** Performs dialog to incorporate a set of <code>PwsRecord</code>s, contained 
 *  in the parameter record list, into this database. A conflict solving 
 *  strategy may be specified in the <code>modus</code> parameter, which comes 
 *  into effect when a record of the parameter list shares an identity within 
 *  this file but has a different data signature. 
 *  <p>This method includes GUI action as e.g. undoable edit update! The third 
 *  parameter gives a description (name) of the source which may be displayed to 
 *  represent the merge event.
 *  <p>Records incorporated through this method will be marked as IMPORTED for
 *  the lifetime of this container instance.
 *  
 *  @param parent <code>Dialog</code> parent dialog or <b>null</b> for mainframe
 *  @param list <code>PwsRecordList</code> list of import records
 *  @param source String a human readable name for the input source         
 *  @param target String optional target GROUP name for merged records 
 *         (may be <b>null</b>)   
 *  @param initialConflictModus int initial conflict solving strategy
 *                  (one from PwsRecordList.MERGE_* constants)  
 *  @param allowInvalids boolean determines whether invalid records of the 
 *         source are excluded (=false) or considered candidates for inclusion 
 *         (=true)     
 *  @return  <code>PwsRecordList</code> containing records which were NOT MERGED
 *         due to conflict or <b>null</b> if this function was broken      
 *  since 0-5-0
 */
public PwsRecordList mergeDatabase ( 
      Dialog parent, 
      final PwsRecordList list, 
      final String source, 
      final String target, 
      final int initialConflictModus,
      final boolean allowInvalids
      )
{
   final Service.PwsListCompareResult compareRes;
   final PwsRecordList thisDb, thisOld;
   final int srcRecs, nrIdentical, nrConflict;
   int modus;
   boolean hasConflict;

   String hstr, t1;
   final String t2Basis;
   JLabel labelT0, labelT1, label;
   final JLabel labelT2;
   JPanel pane, comboPanel;
   VerticalFlowLayout vflow;
   Font thinLabelFont;
   final ButtonBarDialog dialog;
   final JComboBox combo;
   final JCheckBox copyCheck;
   
   // parameter check
   if ( source == null )
      throw new NullPointerException( "source parameter missing" );

   // GUI elements
   combo = new JComboBox( 
		   ResourceLoader.getCommand( "combo.mergeconflict" ).split(",") );
   copyCheck = new JCheckBox( ResourceLoader.getDisplay( "chk.merge.copyold" ) );
   copyCheck.setIconTextGap( 10 );
   copyCheck.setSelected( Options.isOptionSet( "merge.copycheck" ) );
   
   // PREPARE - ANALYSE PARAMETER RECORD LIST
   // number of records in source
   srcRecs = list.size();

   // identical records (match UUID & Signature)
   // and conflict records (match UUID & !Signature) 
   thisDb = getPwsFile();
   compareRes = Service.comparePwsLists( thisDb, list );
   thisOld = compareRes.source_A;
   nrIdentical = compareRes.identical.size();
   nrConflict = compareRes.conflict.size();
   hasConflict = nrConflict > 0;

   // init display T2 label and font
   labelT2 = new JLabel();
   thinLabelFont = labelT2.getFont().deriveFont( Font.PLAIN );
   labelT2.setFont( thinLabelFont );
   t2Basis = ResourceLoader.getDisplay( hasConflict ? "msg.merge.conflict" : "msg.merge.no_conflict" );

   // create dialog object
   modus = srcRecs - nrIdentical == 0 ? DialogButtonBar.CLOSE_BUTTON : DialogButtonBar.OK_CANCEL_BUTTON;
   if ( parent == null )
      dialog = new ButtonBarDialog( Global.getActiveFrame(), modus, true );
   else
      dialog = new ButtonBarDialog( parent, modus, true );
   dialog.setTitle( ResourceLoader.getDisplay( "dlg.merge" ) );
   
   class OurActionListener implements ActionListener
   {
      int nrMerge, nrSolved;

      public OurActionListener () {
         nrMerge = srcRecs - nrIdentical - nrConflict;
      }
      
      @Override
      public void actionPerformed ( ActionEvent e ) {
         // determine conflict solving modus
         int modus1 = getChosenStrategyModus();
         
         // pro-testis execution of merge of the conflicting items
         PwsRecordList target1 = (PwsRecordList)thisDb.clone();
         PwsRecordList remain = target1.merge( compareRes.conflict, modus1, allowInvalids );
         nrSolved = nrConflict - remain.size();
         nrMerge = srcRecs - nrIdentical - nrConflict + nrSolved;

         // refresh display
         refresh();
         
//         System.out.println( "-- performed MERGE calculation: merge=" + nrMerge + ", solved=" + nrSolved ); 
      }
      
      /** Refreshes display of the T2 label and copy-replaced checkbox. */
      public void refresh () {
         String t2 = t2Basis;
         t2 = Util.substituteText( t2, "$records", String.valueOf( nrMerge ) );
         t2 = Util.substituteText( t2, "$overwrites", String.valueOf( nrSolved ) );
         t2 = Util.substituteText( t2, "$remains", String.valueOf( nrConflict - nrSolved ) );
         labelT2.setText( t2 );
         copyCheck.setEnabled( nrSolved != 0 );
      }
      
      public int getIndexOfStrategy ( int strategy )
      {
         int i; 

         switch ( strategy ) {
         case PwsRecordList.MERGE_MODIFIED: i = 1;
                 break;
         case PwsRecordList.MERGE_PASSMODIFIED: i = 2;
                 break;
         case PwsRecordList.MERGE_EXPIRY: i = 3;
                 break;
         case PwsRecordList.MERGE_INCLUDE: i = 4 ;
                 break;
         default: i = 0;
         }
         return i;
      }
      
      public int getChosenStrategyModus ()
      {
         int modus1; 
         
         switch ( combo.getSelectedIndex() )
         {
         case 1: modus1 = PwsRecordList.MERGE_MODIFIED;
                 break;
         case 2: modus1 = PwsRecordList.MERGE_PASSMODIFIED;
                 break;
         case 3: modus1 = PwsRecordList.MERGE_EXPIRY;
                 break;
         case 4: modus1 = PwsRecordList.MERGE_INCLUDE;
                 break;
         default: modus1 = PwsRecordList.MERGE_PLAIN;
         }
         return modus1;
      }
   }  // inner class OurActionListener
   
   final OurActionListener selectListener = new OurActionListener();

   class  OurButtonBarListener extends DefaultButtonBarListener
   {
      PwsRecordList excludes;

      OurButtonBarListener ( ButtonBarDialog dlg ) {
         super( dlg );
      }
      
      @Override
      public boolean okButtonPerformed () {
         PwsRecordList includes, input, newEntries, stockInclude, modified, written;
         final DefaultRecordWrapper[] newRecs;
         DefaultRecordWrapper[] duplRecs, oldRecs, records;
         String msg;
         int count, modus1;
         
         // if there is something to do
         if ( srcRecs - nrIdentical > 0 ) {
        	 
            // get selected conflict solving strategy
            modus1 = selectListener.getChosenStrategyModus();
            
            // determine merge input
            // (if INCLUDE option we have to remove identical records from source
            // otherwise they count as included, too)
            input = modus1 != PwsRecordList.MERGE_INCLUDE ? list 
                    : list.excludeRecordList( compareRes.identical ); 

            // merge parameter record list (record include and exclude list)
            excludes = mergeDatabase( input, modus1, allowInvalids );
            includes = input.excludeRecordList( excludes );
            stockInclude = includes.intersectionRecordList( thisOld );
            newEntries = includes.excludeRecordList( thisOld );
            modified = stockInclude.excludeRecordList( compareRes.identical );
//            identical = stockInclude.excludeRecordList( modified );
            written = modified.unionRecordList( newEntries );
            
            // move new included records to target directory if opted
            if ( target != null ) {
               records = newEntries.toRecordWrappers( null );
               records = moveEntries( records, target, true );
               if ( records != null ) {
            	  written.updateRecordList(records);
               }
            }
            
            oldRecs = thisOld.intersectionRecordList(modified).toRecordWrappers(null);
            newRecs = written.toRecordWrappers(null);
            count = written.size();

            // save replaced entries into special folder if opted
            duplRecs = null;
            if ( copyCheck.isSelected() )
            try {
               duplRecs = duplicateRecords( oldRecs );
               addRecordList( duplRecs );
               moveEntries( duplRecs, ResourceLoader.getDisplay( "merge.replacement_group" ), true );

            } catch ( PasswordSafeException e ) {
               e.printStackTrace();
               GUIService.failureMessage( "Could not create replacement duplicates!", e );
               duplRecs = null;
            }
            
            // update global menu items
            MenuHandler.setOperationMode( PwsFileContainer.this );
            
            // after-work, if records have been written to the database
            if ( count != 0 ) {
               // create the undoable edit object
               UndoableEdit edit = new UndoManager.ModifyRecordEdit( 
                     UndoManager.ModifyRecordEdit.IMPORT_RECORDS_EDIT,
                     PwsFileContainer.this, oldRecs, newRecs, duplRecs, source );
               fireEditEvent( edit );
               
               // arrange display
               viewHandler.setSelectedWrappers( newRecs );
               viewHandler.scrollToVisible( viewHandler.getFirstSelected() );
               
               // display confirmation messages
               msg = ResourceLoader.getDisplay( "msg.confirm.merge" );
               msg = Util.substituteText( msg, "$count", String.valueOf( count ) );

            } else {
               msg = ResourceLoader.getDisplay( "msg.confirm.merge_nothing" );
            }
               
            // report operation success
            Global.setStatusText( msg );
            GUIService.infoMessage( dialog, "confirm.operation", msg );
            
            // report any invalid entries imported
            if ( written.hasInvalidRecs() ) {
               showInvalidsMessage( false );
            }
         }

         Options.setOption( "merge.copycheck", copyCheck.isSelected() );
         return super.okButtonPerformed();  // disposes dialog, returns true
      }
   }  // inner class OurButtonBarListener
   OurButtonBarListener barListener;
   
   // INIT DIALOG
   // content labels
   hstr = ResourceLoader.getDisplay( "label.merge.source" );
   hstr = Util.substituteText( hstr, "$source", source );
   labelT0 = new JLabel( hstr );
   labelT1 = new JLabel();

   // case: nothing worth considering
   if ( srcRecs - nrIdentical == 0 ) {
      t1 = ResourceLoader.getDisplay( "msg.merge.nothing" );

   // case: everything for inclusion
   } else if ( nrConflict == 0 & nrIdentical == 0 ) {
      t1 = ResourceLoader.getDisplay( "msg.merge.everything" );
      t1 = Util.substituteText( t1, "$records", String.valueOf( srcRecs ) );
   }
   
   // case: conflicts remaining
   else {
      t1 = ResourceLoader.getDisplay( "msg.merge.subset" );
      t1 = Util.substituteText( t1, "$srcrecs", String.valueOf( srcRecs ) );
      t1 = Util.substituteText( t1, "$identrecs", String.valueOf( nrIdentical ) );
      t1 = Util.substituteText( t1, "$confrecs", String.valueOf( nrConflict ) );
      
      selectListener.refresh();
   }
   labelT1.setText( t1 );

   combo.addActionListener( selectListener );
   combo.setSelectedIndex( selectListener.getIndexOfStrategy( initialConflictModus ));
   comboPanel = new JPanel( new BorderLayout( 8, 8 ) );
   comboPanel.setBorder( BorderFactory.createEmptyBorder( 5, 0, 5, 0 ) );
   label = new JLabel( ResourceLoader.getDisplay( "label.merge.strategy" ) );
   comboPanel.add( label, BorderLayout.WEST );
   comboPanel.add( combo, BorderLayout.CENTER );
   
   // create dialog content panel
   vflow = new VerticalFlowLayout( 5 );
   pane = new JPanel( vflow );
   pane.setBorder( BorderFactory.createEmptyBorder( 15, 15, 0, 15 ) );
   pane.add( labelT0 );
   pane.add( labelT1 );
   pane.add( labelT2 );
   if ( hasConflict ) {
      pane.add( comboPanel );
      pane.add( copyCheck );
   }

   // make dialog operative and show
   barListener = new OurButtonBarListener( dialog );
   dialog.addButtonBarListener( barListener );
   dialog.setDialogPanel( pane );
   if ( dialog.getWidth() < 250 ) {
      dialog.setSize( new Dimension( 300, dialog.getHeight() ) );
   }
   dialog.show();
   dialog.joinButtonThreads();
   
   return barListener.excludes;
}  // mergeDatabase

/** Top level function and dialog to perform incorporation of another <code>
 *  PwsFile</code> into this database. The user may select a file from file 
 *  chooser and receives modified feedback whether all, none or a part of a 
 *  selected file can be imported. 
 */
public void mergeDatabase ()
{
   PwsFile sourceDbf = null;
   JFileChooser   fc;
   ContextFile source;
//   ContextFile thisFile;
   File file;
   int p;
   
   if ( isIOProgressing() ) return;
//   if ( isCriticalPhase() ) return;
   
   isIOProgress = true;
//   thisFile = getContextFile();

   // let user select a merge file
   p = FileOpenDialog.PWSFILE_FILTER | FileOpenDialog.BACKUP_FILTER;
   fc = new FileOpenDialog( p, Global.currentDir );
   fc.setDialogTitle( ResourceLoader.getDisplay("dlg.mergefile") );

   // file chooser dialog
   if ( fc.showOpenDialog(window()) != JFileChooser.APPROVE_OPTION ) {
      isIOProgress = false;
      return;
   }
   Global.currentDir = fc.getCurrentDirectory();

   // create URL notation for input file 
   file = fc.getSelectedFile();
   try { 
	   source = Global.getContextFile( Util.makeFileURL( file.getPath() )); 
   } catch ( Exception e ) { 
      GUIService.failureMessage( "msg.url.formerror", e );
      isIOProgress = false;
      return;
   }

   // open the source file; terminate if impossible
   if ( (sourceDbf = passTryOpen( source, FileAccessModus.desktopSelection )) == null )
        // request critical phases for merge file and this file
//        || !Global.requestCriticalPhase( thisFile )
//        || !Global.requestCriticalPhase( source ) )
   {
	   
//      Global.endCriticalPhase( thisFile );
      isIOProgress = false;
      return;
   }
   
   // finally merge the two databases
   mergeDatabase( null, sourceDbf, sourceDbf.getFileName(), null, 0, true );

   // release critical phases for files
//   Global.endCriticalPhase( thisFile );
//   Global.endCriticalPhase( source );
   isIOProgress = false;
}  // mergeDatabase

public void importFileCSV ()
{
   if ( isIOProgressing() ) return;
//   if ( isCriticalPhase() ) return;
   
   ImportCSVDialog dlg = new ImportCSVDialog( this );
   dlg.setVisible( true );
}  // importFile

public void exportFileCSV ()
{
   if ( isIOProgressing() ) return;
//   if ( isCriticalPhase() ) return;
   
   Service.csvUserWarning();
   ExportCSVDialog dlg = new ExportCSVDialog( this );
   dlg.setVisible( true );
}  // exportFile

public void exportSelectionCSV ()
{
   ExportCSVDialog dlg;
   DefaultRecordWrapper[] recs;

   if ( isIOProgressing() ) return;
// if ( isCriticalPhase() ) return;
   
   if ( (recs = getSelectedGroupRecords()) == null ) {
      recs = getSelectedRecords();
   }
   
   if ( recs.length != 0 ) {
      Service.csvUserWarning();
      dlg = new ExportCSVDialog( this, recs );
      dlg.setVisible( true );
   }
}  // exportSelectionCSV

/**
 * Dialog function to restore the currently open (selected) file by a choosable
 * backup file.
 *   
 * @return <b>true</b> if and only if the restore from a backup PWS database
 *         has taken place successfully
 */
public boolean restoreBackup ()
{
   PwsFile backFile = null;
   UndoableEdit edit;
   JFileChooser   fc;
   ContextFile source;
//   ContextFile thisFile;
   Iterator<PwsRecord> it;
   File file, backfile;
   String text, hstr;
   int total, count, ratio;
   boolean ok;
   
//   thisFile = getContextFile();

   // if not new file, request a critical phase for this
   if ( isIOProgressing() ) return false;
// if ( isCriticalPhase() ) return;
   
   Global.verifyBackupDir();
   isIOProgress = true;
   
   // let user select a backup file
   // prepare filechooser dialog
   fc = new FileOpenDialog( FileOpenDialog.BACKUP_FILTER, Global.backDir );
   backfile = getBackfileDefinition(); 
   fc.setSelectedFile( backfile );
   fc.setDialogTitle( ResourceLoader.getDisplay("dlg.restorebackup") + getDatabaseName() );
   
   // show and evaluate file chooser dialog
   ok = false;
   file = null;
   if ( fc.showOpenDialog(window()) == JFileChooser.APPROVE_OPTION ) {
      Global.backDir = fc.getCurrentDirectory();
   
      // determine backup file url
      file = fc.getSelectedFile();
      try { 
         source = Global.getContextFile( Util.makeFileURL( file.getPath() ));
         ok = true;
      } catch ( Exception e ) { 
         GUIService.failureMessage( "msg.url.formerror", e );
         source = null;
      }

      // attempt open file (incl. user password input) 
      ok = ok && (backFile = passTryOpen( source, FileAccessModus.desktopSelection )) != null;
   }

   if ( !ok ) {
//      Global.endCriticalPhase( thisFile );
      isIOProgress = false;
      return false;
   }
   
   // plausibility check and user confirm
   // count matches of backup file records in current database
   count = 0;
   for ( it = backFile.iterator(); it.hasNext(); )
      if ( getPwsFile().contains( it.next() ) )
         count++;

   // info/ask user to confirm action
   total = backFile.size();
   ratio = 0;
   if ( total > 0 )
      ratio = count * 100 / total;

   hstr = "<font color=\"red\">" + String.valueOf( ratio ) + " %</font>";
   text = ResourceLoader.getDisplay( "msg.ask.loadbackup" );
   text = Util.substituteText( text, "$value", hstr );

   if ( ok = GUIService.userConfirm( window(), text ) ) {
      // install opened file object
      try { 
         // create undoable edit event (must do before change happens on "this")
         edit = new UndoManager.FileEdit( UndoManager.FileEdit.RESTORE_EDIT,
                this, backFile, backFile.lastModified() );

         // change file
         substituteContent( backFile );

         fireEditEvent( edit );
       
       // user notify
       GUIService.statusConfirm("msg.confirm.restore", file.getAbsolutePath() );
       if ( Options.isOptionSet( "confirmBackup" ) )
          confirmOperation( OP_RESTORE, file.getAbsolutePath() );
       } catch ( Exception e ) {
          GUIService.failureMessage( "Substitute Content", e );
          ok = false;
       }
   }
   
//   Global.endCriticalPhase( thisFile );
   isIOProgress = false;
   return ok;
}  // restoreBackup

//  since 0-5-0
@Override
public void substituteContent (  PwsRecordList list ) throws PasswordSafeException
{
   PwsPassphrase oldPass = getPassphrase();
   DefaultRecordWrapper[] selrecs = getSelectedRecords();
   
   super.substituteContent( list );
   loadDataSubSystems();
   setSelectedRecords( selrecs );
   fileUpdated();
   
   if ( !oldPass.equals( getPassphrase() ) ) {
      GUIService.warningMessage( window(), null, "msg.warning.accesspasschanged" );
   }
}

/**
 * Dialog function to revert the currently open (selected) database to the state
 * of the latest persistent file.
 * 
 * @param path the source file path (with IO-context of this database)
 *   
 * @return <b>true</b> if and only if the revert has performed successfully
 * since 0-5-0 modified parameter list (void before)
 */
public boolean revert ( String path )
{
   PwsFile backFile;
   UndoableEdit edit;
   ContextFile source;
   String text, hstr;
   long time;
   boolean sameFile;

   
   if ( !hasPersistentFile() | isIOProgressing() ) return false;

   sameFile = path.equals( getFilePath() );
   isIOProgress = true;
   
   // open backup file in direct access mode (ignores database cache)
   try { 
	   source = Global.getContextFile( Util.makeFileURL( path ) ); 
   } catch ( IOException e ) {
      GUIService.failureMessage( "Bad File Path", e );
      return false;
   }
   FileAccessModus acm = path.lastIndexOf( "SEC-" ) == -1 ? 
         FileAccessModus.directAccess : FileAccessModus.desktopSelection;
   if ( (backFile = passTryOpen( source, acm )) == null ) {
      isIOProgress = false;
      return false;
   }

   // msg <- insert name
   text = ResourceLoader.getDisplay( "msg.ask.loadrevert" );
   text = Util.substituteText( text, "$name", getDatabaseName() );

   // msg <- insert time info
   try {
      if ( (time = Util.timeFromSECPath( path )) == -1 ) {
         time = backFile.lastModified();
      }
      hstr = time == 0 ? "- ? -" : Global.getLocalDateTime( time ); 
   } catch ( IOException e ) {
      time = 0;
      hstr = "- ? -"; 
   }
   text = Util.substituteText( text, "$time", hstr );

   // info/ask user (msg)
   if ( !GUIService.userConfirm( window(), text ) ) {
      isIOProgress = false;
      return false;
   }

   // install opened file object
   try { 
      // create undoable edit event (must do before change happens on "this")
      edit = new UndoManager.FileEdit( UndoManager.FileEdit.REVERT_EDIT,
            this, backFile, time );

      // change file content and associated data systems
      substituteContent( backFile ); 
      if ( sameFile ) {
         resetModified();
      }
      setLastSaveTime( System.currentTimeMillis() );

      // fire edit event
      fireEditEvent( edit );

      // user notify
      GUIService.statusConfirm("msg.confirm.revert");
      if ( Options.isOptionSet( "confirmRevert" ) ) {
         confirmOperation( OP_REVERT, "" );
      }
      return true;

   } catch ( Exception e ) {
      GUIService.failureMessage( "Substitute Content", e );
      return false;
   } finally {
//      Global.endCriticalPhase( source );
      isIOProgress = false;
   }
}  // revert


// since 0-5-0
@Override
public void eraseBackups ()
{
   String hstr;
   
   hstr = ResourceLoader.getDisplay( "msg.ask.delete.backups" );
   hstr = Util.substituteText( hstr, "$amount", String.valueOf( getAutoBackups().size() ) );
   hstr = Util.substituteText( hstr, "$name", getDatabaseName() );

   if ( GUIService.userConfirm( window(), hstr ) )
   {
      super.eraseBackups();
      MenuHandler.fileUpdated( this );
   }
}

/**
 * Sets a newer file format for the persistent state of this file
 * if and only if its current format is an old format (V1, V2).
 * Runs on any thread.
 *
 * @since 0-4-0
 */
public void convert ()
{
   String text;
   
   if ( getFileFormat() == Global.FILEVERSION_LATEST )
      return;
   
   text = ResourceLoader.getDisplay( "msg.convert.ask" );
   text = Util.substituteText( text, "$file", getDatabaseName() );
   text = Util.substituteText( text, "$oldformat", getFileFormatText( getFileFormat() ) );
   text = Util.substituteText( text, "$newformat", getFileFormatText( Global.FILEVERSION_LATEST ) );
   
   if ( GUIService.userConfirm( text ) )
   {
      Global.delay( 500 );
      setFileFormat( Global.FILEVERSION_LATEST );

      // user notify
      GUIService.statusConfirm("msg.confirm.convert");
      confirmOperation( OP_CONVERT, "" );
   }
}  // convert

/** Whether there is an IO function active for this container. This will 
 *  be true when a user dialog is running in the context of a IO function
 *  (even when no critical phase is yet activated for the file).
 *  
 *  @return <b>true</b> if and only if some thread is performing an IO function
 *          of this container which currently does or is expected to affect the 
 *          state of the persistent file
 */
public boolean isIOProgressing ()
{
   return isIOProgress;
}

/** Whether there is currently a record editor active on this database. 
 * 
 * @return boolean <b>true</b> == record is being edited
 */
public boolean isRecordEditing ()
{
   return getEditDialog() != null;
}

/**
 * Attempts to close this container for operations. Saves the <code>PwsFile</code> 
 * of this container if modified. 
 * The attempt is broken if there exists a critical phase for this file or
 * the user chooses to cancel a confirm dialog or save operation fails.
 * After successful save, the display facilities for this container are 
 * removed and the container is CLOSED. 
 * 
 * @return boolean <b>true</b> if and only if the file has been closed 
 */
public boolean close () {
	return close(0);
}
/**
 * Attempts to close this container for operations. Saves the <code>PwsFile</code> 
 * of this container if modified. 
 * The attempt is broken if there exists a critical phase for this file or
 * the user chooses to cancel a confirm dialog or save operation fails.
 * After successful save, the display facilities for this container are 
 * removed and the container is CLOSED. 
 * 
 * @param modus int 0 = single operation, 1 = multi-close operation
 * @return boolean <b>true</b> if and only if the file has been closed 
 */
public boolean close ( int modus ) {

   // control open file
   if ( isOpen() ) {
	   
      // test for critical phase
	   if ( isIOProgressing() ) return false;
	//   if ( isCriticalPhase() ) return;
      
      // store into MINOR file options if it does not cause modification
 	  saveTreeExpansionInfoToMinors();

      // if modified, ask user for save operation
      if ( isModified() ) {
	      boolean saveFile = saveAllTrigger;
	      
	      // obtain user confirmation for save (may be "Save All" choice) 
	      if ( !saveFile ) synchronized (CLOSE_LOCK) {
		     saveFile = saveAllTrigger;
	    	 if ( !saveFile ) {
		    	String title = "dlg.confirm";
			    String msg = ResourceLoader.getDisplay( "msg.cansave" );
			    msg = Util.substituteText( msg, "$name", getDatabaseName() );
			    String opt0 = ResourceLoader.getDisplay("button.yes");
			    String opt1 = ResourceLoader.getDisplay("button.no");
			    String opt2 = modus == 0 ? null : ResourceLoader.getDisplay("button.saveall");
			    String[] opts = new String[] {opt0, opt1, opt2};
	
			    // dialog + evaluation of choice
			    int choice = GUIService.userOptionInput(window(), title, msg, opts, true);
			    switch ( choice  ) {
			    case -1 :  // CANCEL option
			   	   return false;
			    case 2 :   // Save-All option
			   	   saveAllTrigger = true;
			    case 0 :   // YES option
			   	   saveFile = true;
			   	   break;
			    }
	    	 } 
		  }
	
	      // user confirmed SAVE operation
	      if ( saveFile && !saveFile(true) ) { 
	    	 return false;
	      }
      }
   }

   // MOB UP SECTION
   // update references
   secondsTimerTask.cancel();
   transferHandler.exit();
   if ( getOrderedList() != null ) {
      getOrderedList().removeOrderedListListener( this );
   }
   exitDisplay();
   
   dbf.removeFileListener(keyStrokeHandler);
   editListeners.clear();
   undoManager.clear();
   setOperationMode( UNMOUNTED );
   
   try { 
      removeMirrorFile();
      super.close( false ); 
   }
   catch ( Exception e )
   {}
   
   return true;
}  // close

/**
 * Returns the <code>PwsRecord</code> associated to the currently selected display
 * row, if and only if this row qualifies to point to a record. Otherwise, if
 * display is disabled or if there is nothing selected, <b>null</b> is 
 * returned instead. If more than one records are selected, the first sorted
 * is returned. 
 */
public PwsRecord getSelectedRecord()
{
   if ( viewHandler == null ) return null;
   
   int index = viewHandler.getFirstSelected();
   DefaultRecordWrapper obj = getOrderedList().getItemAt( index );

   if ( obj != null ) {
      return obj.getRecord();
   }
   return null;
}

/** Returns an array with the currently selected records of this container.
 *  The array is empty if nothing is selected. */ 
public DefaultRecordWrapper[] getSelectedRecords ()
{
   int[] indices;
   DefaultRecordWrapper results[];
   int i;
   
   if ( viewHandler == null )
      results = new DefaultRecordWrapper[ 0 ];
   else
   {
      indices = viewHandler.getSelectedItems();
      results = new DefaultRecordWrapper[ indices.length ];
      for ( i = 0; i < indices.length; i++ )
         results[i] = getOrderedList().getItemAt( indices[i] );
   }
   return results;
}

/**
 * Returns the GROUP value that may be identified through the currently selected
 * display row (regardless if it is a folder or a leaf node). If there is no
 * GROUP value defined or nothing is selected, <b>null</b> is returned. 
 */
public String getSelectedGroupName()
{
   TreeHandler.PwsTreeNode node;
   PwsRecord record;
   Object obj;
   
   if ( viewHandler == null )
      return null; 
   
   if ( viewType == TREE_VIEW )
   {
      // branch: TREE
      if ( (node = (TreeHandler.PwsTreeNode)treeHandler.getSelectedNode()) == null )
         return null;
         
      obj = node.getUserObject();
      if ( obj instanceof DefaultRecordWrapper )
      {
         return ((DefaultRecordWrapper)obj).getRecord().getGroup();
      }
      return node.getPathName();
   }
   
   // branch: TABLE
   if ( (record = getSelectedRecord()) != null )
      return record.getGroup();
   return null;
}

/**
 * If a GROUP line is selected in TREE VIEW, the set of records contained in
 * this group is returned. Otherwise <b>null</b> is returned.
 * 
 * @return <code>DefaultRecordWrapper[]</code>
 */
public DefaultRecordWrapper[] getSelectedGroupRecords ()
{
   if ( getSelectionStatus() == GROUP_SELECTED )
      return getGroupRecords( getSelectedGroupName() );
   return null;
}

/** Returns the current selection status of this container. 
 *   
 *  @return one of NOTHING_SELECTED, GROUP_SELECTED, RECORD_SELECTED or RECORDSET_SELECTED.
 */
public int getSelectionStatus ()
{
   int recs;
   
   if ( viewHandler == null || !viewHandler.hasUserSelection() )
      return NOTHING_SELECTED;
   
   recs = viewHandler.getSelectedItems().length;
   return recs == 0 ? GROUP_SELECTED : recs == 1 ? RECORD_SELECTED : RECORDSET_SELECTED;
}

/** Returns the currently active viewtype of this container. */
public int getViewType ()
{
   return viewType;
}

/** The displayable title of this container. This renders the 
 * logical database name if user opted and available otherwise 
 * the full file path
 * if it is defined, otherwise a constant "new file" indicator.
 */
public String getTitle ()
{
   String hstr = null;
   
   // look for logical file name
   if ( Options.isOptionSet( "logicalFilenames" ) )
      hstr = getHeaderValue( HEADERFIELD_DBNAME );
   // if unavailable use full file path 
   if ( hstr == null )
      hstr = getFilePath();
   // if unavailable use "new file" display
   if ( hstr == null)
      hstr = ResourceLoader.getDisplay("ui.newtitle");
   
   return hstr;
}

/**
 * Causes this container to adopt the given dataView type.
 * Does nothing if the parameter dataView is already in place.
 * 
 * @param view data viewing mode to be activated
 */
public void setViewType ( int view )
{
   int selected[];
   
   // operate if parameter valid and constitutes view change
   if (  view > -1 & view < 3 & view != viewType )
   {
      viewType = view;
      selected = null;

      // view already installed
      if ( viewHandler != null )
      {
         if ( view == NO_VIEW )
         {
            exitDisplay();
            return;
         }
      }
      // no view installed
      else
         if ( view != NO_VIEW )
            initDisplay( view );
         else return; // this should not happen
      
      // adjust dataView-handler
      selected = viewHandler.getSelectedItems();
      viewHandler = viewType == NO_VIEW ? null : 
                    viewType == TREE_VIEW ? (ContainerView)treeHandler : 
                    tableHandler;
      
      // store selection
      if ( getFilterStatus() == FILTER_FAVOURITES )
         getMinorOptions().setIntOption( "favouriteViewtype", viewType );
      else
      {
         getMinorOptions().setIntOption( "viewType", viewType == TABLE_VIEW ? 
               TABLE_VIEW : TREE_VIEW );  
         normalViewType = viewType;
      }
                    
      // modify display and report change             
      setupDisplay( selected );
      getViewHandler().getView().requestFocusInWindow();
      reportPropertyChange( SELECTION_STATUS );
      reportPropertyChange( DISPLAY_MODE );
   }
}  // setViewType

public void setSelectedRecord ( PwsRecord rec )
{
   if ( viewHandler != null ) {
	  Global.delay(100);
      viewHandler.setSelectedIndex( getOrderedList().indexOf( rec ) );
   }
}

public void setSelectedRecords ( DefaultRecordWrapper[] recs )
{
   if ( viewHandler != null ) {
	  Global.delay(50);
      viewHandler.setSelectedWrappers( recs );
   }
}

public void selectAll ()
{
   if ( viewHandler != null ) {
      viewHandler.setSelectAll();
   }
}

public void clearSelection ()
{
   if ( viewHandler != null )
      viewHandler.setSelectedIndex( -1 );
}

/** Expands or collapses a display tree branch identified by a group name. 
 * 
 *  @param group GROUP field name for an existing record group
 *  @param v value <b>true</b> == expanded, <b>false</b> == collapsed
 */ 
public void setExpandedBranch ( final String group, final boolean v ) {
	
	ActionHandler.executeOnEDT( new Runnable () {
       @Override
	   public void run () {
         treeHandler.setExpanded( group, v );
       }
   });
}

/** Sets the "data view" component of the container display and waits
 * until it is actually shown on the EDT. <p><small>The "data view" component is the closest to the
 * entry database and is showing a list of records, sorted and filtered. Alternatively,
 * in its place other display like e.g. the "empty file" or "empty filter list" message
 * can be put.</small>
 * 
 * @param component <code>Component</code>
 */
private void setDataView( Component component )
{
   final Component c = component == null ? new JPanel() : component;
   final String hstr = c.getClass().getName() + "Name = " + 
         (c.getName() != null ? c.getName() : "");

   if ( scrollPane.getViewport().getView() != c ) {

	  Runnable r = new Runnable () {
         @Override
         public void run () {
            Log.debug( 6, "(PwsFileContainer.setDataView.RUN) + + + + +  Setting new VIEW, type: "
            		   .concat(hstr) );
            scrollPane.setViewportView( c );
            standardScreenPanel.revalidate();
            standardScreenPanel.repaint();
            Log.debug( 6, "(PwsFileContainer.setDataView.RUN) + + + + +  Setting new VIEW, executed" );
         }
      };

      Log.debug( 6, "(PwsFileContainer.setDataView) - - - initiating new VIEW, type: ".concat( 
            hstr ) );
      try { ActionHandler.executeOnEDT_Wait( r ); }
      catch ( Exception e ) {}
   }
}

/** Lets this container grab the input focus if and only if it is the selected
 * container.
 */
public void grabFocus () {
    if ( isSelectedContainer() ) {
        Log.log(10, "(PwsFileContainer.grabFocus) grabing focus for container: ".concat(getTitle()));
    	if ( viewHandler != null ) {
    		viewHandler.getView().requestFocusInWindow();
    	}
//        standardScreenPanel.requestFocusInWindow();
    }
}

private void setEmptyView () {
   setDataView( !isFiltering() || getRecordCount() == 0 ? emptyFilePanel : emptyFilteringPanel );
}

/** Sets the "locked view" status for the entire container display. The locked view makes
 *  all UI content of the container invisible until a mouse-click removes this state again.
 *   
 * @param lock boolean <b>true</b> = locked, <b>false</b> = unlocked
 */
public void setLockedView ( final boolean lock )
{
   Runnable r = new Runnable ()
   {
      @Override
      public void run ()
      {
         if ( lock & !isLockedView ) {
            Log.debug( 6, "(PwsFileContainer.setLockedView) attempting to LOCK view" );
            screenView.removeAll();
            screenView.add( createLockedScreenPanel(), BorderLayout.CENTER );
            screenView.revalidate();
            screenView.repaint();
            isLockedView = true;
         }
         else if ( !lock & isLockedView ) {
            Log.debug( 6, "(PwsFileContainer.setLockedView) attempting to UNLOCK view" );
            screenView.removeAll();
            screenView.add( standardScreenPanel, BorderLayout.CENTER );
            grabFocus();
            screenView.revalidate();
            screenView.repaint();
            isLockedView = false;
         }
      }
   };
   ActionHandler.executeOnEDT( r );
}

/** Sets up this container's display screen. Sets entry selections in the 
 *  active list as specified by the parameter. Parameter <b>null</b> leads
 *  to unmodified selection settings (ignore). To clear a selection, an empty
 *  index has to be passed.
 *  
 *  @param index array of indices into order record list
 */ 
public void setupDisplay ( int index[] )
{
   if ( getFilteredSize() == 0 ) {
      setEmptyView();

   } else {
      setDataView( viewHandler.getView() );
      if ( index != null ) {
         viewHandler.setSelectedItems( index );
      }
   }

   setFilterStatusIntern( getFilterStatus() );
   grabFocus();
}  // setupDisplay

private void refreshView () 
{
    boolean isMirror = isMirrorFile();
    ActionHandler.executeOnEDT( new PanelSwitcher( VisiblePanel.MirrorIndicator, 
            isMirror, null ) );
    ActionHandler.executeOnEDT( new PanelSwitcher( VisiblePanel.BackupIndicator, 
            isBackupFile() & !isMirror, null ) );
}

/** Whether the enclosed database is a mirror file (i.e. it resides under the
 *  application's mirror directory).
 *  
 * @return boolean
 */
public boolean isMirrorFile () 
{
    String path = getFilePath();
    return path == null ? false : path.startsWith( 
           Util.normalizedPath( Global.mirrorDir.getAbsolutePath(), true ));
}

/** Whether the enclosed database is a backup file (i.e. it has the 
 * corresponding file name extension).
 *  
 * @return boolean
 */
public boolean isBackupFile () 
{
    String path = getFilePath();
    return path == null ? false : FileOpenDialog.BackupFileFilter.accept( path );
}

/** Whether serializable content of this container was modified. 
 *  @since 0-4-0
 */
@Override
public boolean isModified ()
{
   return super.isModified() || favouriteEntries.isModified() ||
          (Options.isOptionSet("storeMinorChanges") &&
           lastUsedEntries.isModified());
}

/** Whether this container is the currently selected container in display. */
public boolean isSelectedContainer ()
{
   return DisplayManager.getSelectedContainer() == this;
}

/** Resets the modify marker for this container and database to UNMODIFIED.
 *  @since 0-4-0
 */
@Override
public void resetModified ()
{
   lastEditedEntries.resetModified();
   lastUsedEntries.resetModified();
   favouriteEntries.resetModified();
   recentFinds.resetModified();
   super.resetModified();
}

@Override
protected void fileSaved ()
{
   super.fileSaved();
   mirrorNumber = modifyNumber;
   Log.debug( 10, "(PwsFileContainer.fileSaved) MODIFY == MIRROR" );
   Global.pushRecentFile( this );
}

@Override
protected void fileUpdated ()
{
   super.fileUpdated();
//   modifyNumber++;
}

public ContainerView getViewHandler()
{
   return viewHandler;
}

public MouseAdapter getMouseAdapter ()
{
   return new ViewportMouseAdapter();
}

public RecentList getLastUsedList ()
{
   return lastUsedEntries;
}

public RecentList getFavouriteList ()
{
   return favouriteEntries;
}

/** Returns the <code>RecentList</code> structure that reflects recent "Find" 
 * values that are performed through <code>findMatching()</code> method.
 *  
 *  @return RecentList of "Find" string values
 */ 
public RecentList getRecentFinds ()
{
//   return Options.isOptionSet("storeMinorChanges") ? recentFinds : Global.recentFinds;
   return recentFinds;
}

/**
 * Assigns or withdraws the FAVOURITE marker for a specific record of this
 * container. If the record is not contained, nothing is performed.
 *   
 * @param rec PwsRecord to be assigned or withdrawn the FAVOURITE mark
 * @param favourite boolean true == FAVOURITE on, false == FAVOURITE off
 * @since 0-6-0
 */
public void setRecordFavourite ( PwsRecord rec, boolean favourite )
{
   boolean mod;
   
   if ( containsRecord( rec ) )
   {
      if ( favourite )
         mod = favouriteEntries.pushRecent( rec );
      else
         mod = favouriteEntries.removeRecent( rec );
      
      if ( mod )
      {
//         reportPropertyChange( MODIFY_EVENT );
         if ( getFilterStatus() == FILTER_FAVOURITES )
            refreshFilter();
      }
   }
}

/**
 * Assigns or withdraws the FAVOURITE marker to a set of records in this
 * container. Records not contained are not handled.
 *   
 * @param set DefaultRecordWrapper[] records to be assigned or withdrawn the FAVOURITE mark
 * @param favourite boolean true == FAVOURITE on, false == FAVOURITE off
 * @since 0-6-0
 */
public void setRecordsFavourite ( DefaultRecordWrapper[] set, boolean favourite )
{
   for ( int i = 0; i < set.length; i++ )
      setRecordFavourite( set[i].getRecord(), favourite );
}

/**
 * Whether the specified record is marked as FAVOURITE in this container.
 * 
 * @param rec PwsRecord to be tested
 * @return boolean <b>true</b> if and only if the parameter record is contained and holds
 *         a FAVOURITE marker in this container
 */
public boolean isRecordFavourite ( PwsRecord rec )
{
   return favouriteEntries.hasRecent( rec );
}

/**
 *  @since 0-4-0
 */
public RecentList getLastEditedList ()
{
   return lastEditedEntries;
}

/** Unconditionally sets the display elements according to the specified Filter 
 *  status.
 *  
 * @param status
 */
private void setFilterStatusIntern ( int status )
{
   DefaultRecordWrapper selected[];
   String hstr;
   int vt;
   boolean modified;

   // whether the filter status is changing
   modified = status != getFilterStatus();

   // filter bar with appropriate text 
   switch ( status )
   {
   case FILTER_EXPIRING:
      hstr = "filter.modus1.text";
      break;
   case FILTER_IMPORTED:
      hstr = "filter.modus2.text";
      break;
   case FILTER_MODIFIED:
      hstr = "filter.modus3.text";
      break;
   case FILTER_INVALID:
      hstr = "filter.modus4.text";
      break;
   case FILTER_FAVOURITES:
      hstr = "filter.modus5.text";
      break;
   default: status = FILTER_OFF;
            hstr = null;   
   }
   
   // this sets the filter indication bar North of the container display
   ActionHandler.executeOnEDT( new PanelSwitcher( VisiblePanel.Filter, 
         hstr != null, hstr == null ? null : ResourceLoader.getCommand( hstr ) ) );

   // remember actual list selection
   selected = null;
   if ( modified )
      selected = viewHandler == null ? null : viewHandler.getSelectedWrappers();

   // deal with tree-handler's expansion paths memory
   if ( status == FILTER_OFF & !isTextFindFilterActive() )
      treeHandler.resetTreeExpansionState();
   else
      treeHandler.markTreeExpansionState();
   
   // modify ordered list (issues list reload event)
   super.setFilterStatus( status );
   if ( status != FILTER_OFF )
      escPrio = 'F';
   
   // arrange empty dataView panel if required
   if ( getFilteredSize() == 0 )
      setEmptyView();
   else
   {
      if ( status == FILTER_FAVOURITES )
      {
         vt = getMinorOptions().getIntOption( "favouriteViewtype" );
         setViewType( vt == TABLE_VIEW ? TABLE_VIEW : TREE_VIEW );
      }
      else if ( normalViewType != 0 )
         setViewType( normalViewType );
      
      setDataView( viewHandler.getView() );
   }
   
   // set tree display to expand-all in filtered modi or if other filtering is active
   if ( status != FILTER_OFF | isTextFindFilterActive() )
      setExpandedBranch( "", true );
   
   // try reinstall list selection in modified filter list
   if ( viewHandler != null & modified )
      viewHandler.setSelectedWrappers( selected );
}  // setFilterStatusIntern

/** Sets the display filter for the entry list browser to a specified filter mode.
 *  Does nothing if the current status already corresponds to the parameter.
 * 
 * @param status new filter status
 */  
@Override
public void setFilterStatus ( int status )
{
   if ( status == getFilterStatus() )
      return;

   setFilterStatusIntern( status );
}

/** Toggles filter status of this file container between FILTER_OFF and the
 * given filtering type. 
 * <p>In other words: if the current filter modus is the 
 * given modus, then the modus is set to FILTER_OFF, otherwise it is set to 
 * the given modus.
 *  
 * @param filter int filter mode
 */
public void toggleFilter ( int filter ) {
   setFilterStatus( getFilterStatus() == filter ? PwsFileContainer.FILTER_OFF 
         : filter );
}

private synchronized void setOperationMode ( int mode )
{
   if ( mode < 1 | mode > 3 | mode == operationMode ) return;
   
   switch ( mode ) {
      case MOUNTED_ACTIVE:
         setupDisplay( null );
         break;
   
      case UNMOUNTED:         
         break;
   }
   
   operationMode = mode;
   reportPropertyChange( OPERATION_MODE );
}  // setOperationMode

public int getOperationMode ()
{
   return operationMode;
}

public ActivityListener getActivityListener ()
{
   return objectListener;
}

/**
 * Attempts to make this container the selected (active) one in case
 * it is an element of a multi-file desktop view.
 * 
 * @param v boolean <b>true</b> == selected
 */
public void setSelected ( boolean v )
{
   if ( getOperationMode() == UNMOUNTED ) return;
   setOperationMode( v ? MOUNTED_ACTIVE : MOUNTED_PASSIVE );
}

/** Displays a user info box about the number of records in this
 *  database which are expired or will expire soon. Requires EDT!
 * 
 * @param always if and only if <b>true</b> the message is also shown for 0 results 
 */
public void showExpiredMessage ( boolean always )
{
   ButtonBarDialog dlg;
   JLabel label;
   String msg, title, s1, s2;
   long exScope;
   int exp1, exp2;
   
   ActionHandler.checkForEDT();
   exScope = Options.getLongOption( "expireScope" );
   exp1 = getPwsFile().countExpired( System.currentTimeMillis() );
   exp2 = getPwsFile().countExpired( System.currentTimeMillis() + exScope );
   title = ResourceLoader.getDisplay( "title.expirecheck" );

   // if we have any hits of calculations
   if ( exp2 > 0 )
   {
      // compile info message
      s1 = s2 = "";
      if ( exp1 > 0 ) {         
         s1 = ResourceLoader.getDisplay( "msg.expire.now" );
         s1 = Util.substituteText( s1, "$expire", String.valueOf( exp1 ) );
      }
      if ( exp2 - exp1 > 0 ) {         
         s2 = ResourceLoader.getDisplay( "msg.expire.soon" );
         s2 = Util.substituteText( s2, "$expire", String.valueOf( exp2 - exp1 ) );
         s2 = Util.substituteText( s2, "$scope", String.valueOf( exScope / Global.DAY ) );
      }

      msg = "<html><font color=\"green\" size=\"+1\">" + getDatabaseName() + 
             "</font><p> " + s1 + s2 + "<p> <p></html>"; 
   
      // create info dialog
      dlg = new ButtonBarDialog( Global.getActiveFrame(), DialogButtonBar.OK_CANCEL_BUTTON, true );
      dlg.setTitle( title );
      label = new JLabel( msg );
      label.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
      dlg.getDialogPanel().add( label );
      dlg.getButtonBar().getOkButton().setText( ResourceLoader.getDisplay( "button.show" ) );
      dlg.getButtonBar().getCancelButton().setText( ResourceLoader.getDisplay( "button.close" ) );
      dlg.pack();
      dlg.show();
      
      // switch display to expiring-filter dataView (if user opted)
      if ( dlg.isOkPressed() )
         setFilterStatus( PwsFileSocket.FILTER_EXPIRING );
      
//      GUIService.infoMessage( title, msg );
   }
   
   // else display info if opted
   else if ( always )
   {
      GUIService.infoMessage( window(), title, ResourceLoader.getDisplay( "msg.noexpiredinfo" ) );
   }
}  // showExpiredMessage

/** Displays a user info box about the number of invalid records in this
 *  database. The message does not show if there are no invalid records AND
 *  parameter <code>always</code> is <b>false</b>. Requires EDT!
 * 
 * @param always if and only if <b>true</b> the message is also shown for 0 results 
 */
public void showInvalidsMessage ( boolean always )
{
   ButtonBarDialog dlg;
   JLabel label;
   String msg, title, s1;
   int inval;
   
   ActionHandler.checkForEDT();
   inval = getPwsFile().countInvalid();
   title = ResourceLoader.getDisplay( "title.invalidscheck" );

   // if we have any hits of calculations
   if ( inval > 0 )
   {
      // compile info message
      s1 = ResourceLoader.getDisplay( "msg.warning.invalids" );
      s1 = Util.substituteText( s1, "$count", String.valueOf( inval ) );

      msg = "<html><font color=\"green\" size=\"+1\">" + getDatabaseName() + 
             "</font><p> " + s1 + "<p> <p></html>"; 
   
      // create info dialog
      dlg = new ButtonBarDialog( Global.getActiveFrame(), DialogButtonBar.OK_CANCEL_BUTTON, true );
      dlg.setTitle( title );
      label = new JLabel( msg );
      label.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
      dlg.getDialogPanel().add( label );
      dlg.getButtonBar().getOkButton().setText( ResourceLoader.getDisplay( "button.show" ) );
      dlg.getButtonBar().getCancelButton().setText( ResourceLoader.getDisplay( "button.close" ) );
      dlg.pack();
      dlg.show();
      
      // switch display to invalids-filter dataView (if user opted)
      if ( dlg.isOkPressed() ) {
         setFilterStatus( PwsFileSocket.FILTER_INVALID );
      }
   }
   
   // else display info if opted
   else if ( always ) {
      GUIService.infoMessage( window(), title, ResourceLoader.getDisplay( "msg.noinvalidsinfo" ) );
   }
}  // showInvalidsMessage

/** Brings up a dialog to enter a new name for a given record GROUP name.
 *  This does nothing if the record set defined by the parameter value is empty.
 *  (The record group is identified by a starting-with match compared to <code>group</code>.)
 *  
 *  @param group string to identify an existing record group
 */
public void renameGroupDlg ( String group )
{
   DefaultRecordWrapper[] oldRecs, newRecs;
   UndoableEdit edit;
   String newName, name, hstr;
   int i;
   
   // do not operate if such a group does not exist
   if ( getPwsFile().getGrpRecordCount( group, true ) > 0 )
   {
      // analyse group text
      name = group;
      if ( (i = group.lastIndexOf( '.' )) > -1 )
         name = group.substring( i+1 );
      
      // prepare dialog to obtain user modification (text)
      hstr = ResourceLoader.getDisplay( "msg.renamegroup" );
      hstr = Util.substituteText( hstr, "$group", group );

      // ask user for text input
      newName = GUIService.userInput( window(),  
            ResourceLoader.getDisplay( "dlg.input.groupname" ),
            hstr, name );

      // perform modification
      if ( newName != null )
      {
         oldRecs = getAbsoluteGroupRecords( group );
         newRecs = renameGroup( group, newName );
         viewHandler.setSelectedWrappers( newRecs );

         // create undoable edit event
         edit = new UndoManager.ModifyRecordEdit( 
               UndoManager.ModifyRecordEdit.RENAME_GROUP_EDIT,
               this, oldRecs, newRecs, group, newName );
         fireEditEvent( edit );
      }
   }
}  // renameGroupDlg

/**
 * Creates duplicates of the parameter record set and inserts them into 
 * the database. (The duplicates have updated create and modify time fields.)
 * 
 * @param records
 * since 0-5-0
 */
public void addDuplicates ( DefaultRecordWrapper[] records )
{
   DefaultRecordWrapper[] duplRecs;
   UndoableEdit edit;

   // insert duplicates into database
   try { 
      duplRecs = duplicateRecords( records );
      addRecordList( duplRecs );
      viewHandler.setSelectedWrappers( duplRecs );

      // create undoable edit event
      edit = new UndoManager.ModifyRecordEdit( 
            UndoManager.ModifyRecordEdit.DUPLICATE_RECORD_EDIT,
            this, records, duplRecs );
      fireEditEvent( edit );
   }
   catch ( Exception e )
   { GUIService.failureMessage( "Duplicate or invalid Record", e ); }
}

/**
 * Makes duplicates of all records belonging to the parameter
 * entry group and inserts them into this database.
 *  
 * @param group Sting group name
 * since 0-5-0
 */
public void duplicateGroup ( String group )
{
   DefaultRecordWrapper duplRecs[];
   UndoableEdit edit;
   PwsRecord recs[];
   String newName;

   // get duplicate records on group name and rename group value
   recs = groupDuplicates( group );
   newName = createGroupVariant( recs, group );
   
   // create wrapper array of duplicated records
   duplRecs = DefaultRecordWrapper.makeWrappers( recs, null );

   // insert duplicates into database
   try { 
      addRecordList( duplRecs );
      viewHandler.setSelectedWrappers( duplRecs );
   }
   catch ( Exception e )
   { GUIService.failureMessage( "Duplicate or invalid Record", e ); }

   // create undoable edit event
   edit = new UndoManager.ModifyRecordEdit( 
         UndoManager.ModifyRecordEdit.DUPLICATE_GROUP_EDIT,
         this, null, duplRecs, group, newName );
   fireEditEvent( edit );
}

/**
 * Brings up a dialog to move selected entries or a group to a choosable location
 * in the GROUP tree of the file. 
 *
 */
public void moveSelectedDlg ()
{
   List<String> groups;
   final DefaultRecordWrapper[] recs, oldState;
   UndoableEdit edit;
   Runnable run;
   JComboBox box;
   JCheckBox chkBox = null;
   Component[] comps;
   String msg, selectMsg, group;
   final String target;
   int editType;
   final boolean isGroup;
   boolean reply;
   
   /** This function works in two modi related to the selected source:
    *  A) record selected B) group selected
    */
   
   // discriminate operation modi requirements
   isGroup = getSelectionStatus() == GROUP_SELECTED;
   if ( isGroup )
   {
      // GROUP selected
      group = getSelectedGroupName();
      recs = getGroupRecords( group );
      msg = ResourceLoader.getDisplay( "msg.moveentrygroup" );
   }
   else
   {
      // RECORD selected
      group = "";
      recs = getSelectedRecords();
      msg = ResourceLoader.getDisplay( "msg.moveentryset" );
   }
   
   // remember actual state of records for undoable edit
   oldState = DefaultRecordWrapper.cloneArray( recs );
      
   if ( recs.length != 0 )
   {
      groups = getGroupList();
      msg = Util.substituteText( msg, "$count", String.valueOf( recs.length ) );
      
      // combobox to chose the target GROUP
      box = GUIService.getGroupListCombo( groups, true, 450 );
      selectMsg = ResourceLoader.getCommand( "combo.select.group" );
      box.insertItemAt( selectMsg, 0 );
      box.setSelectedIndex( 0 );
      
      // preserve group-path option
      if ( !isGroup )
      {
         chkBox = new JCheckBox( ResourceLoader.getDisplay( "dlg.input.keepgrouppaths" ) );
         comps = new Component[] { box, chkBox };
      }
      else
         comps = new Component[] { box };
      
      // call combo dialog
      reply = GUIService.userCombiInput( null, "dlg.input.moveentries", msg, comps );
      
      if ( reply )  // OK_BUTTON pressed
      try {
         target = (String) box.getSelectedItem();
         if ( !target.equals( selectMsg ) )
         {
            clearSelection();

            if ( isGroup )
            {
               moveGroup( group, target, false );
               editType = UndoManager.ModifyRecordEdit.MOVE_GROUP_EDIT;
            }
            else
            {
               setExpandedBranch( target, true );
               moveEntries( recs, target, chkBox.isSelected() );
               editType = UndoManager.ModifyRecordEdit.MOVE_RECORD_EDIT;
            }
            
            // create undoable edit event
            edit = new UndoManager.ModifyRecordEdit( editType, this, oldState, recs, group, target );
            fireEditEvent( edit );

            // adjust result dataView
            run = new Runnable() 
            {
               @Override
			public void run ()
               {
                  if ( isGroup )
                     setExpandedBranch( target, true );
                  viewHandler.setSelectedWrappers( recs );
                  viewHandler.scrollToVisible( getOrderedList().indexOf( recs[ recs.length-1 ] ) );
               }
            };
            ActionHandler.executeOnEDT( run );
         }
      }
      catch ( Exception e )
      {
         GUIService.warningMessage( null, null, e.toString() );
      }
   }
}  // moveSelectedEntriesDlg

/**
 * Selects all records contained in the currently selected GROUP.
 */
public void selectRecords ()
{
   String group;
   
   if ( (group = getSelectedGroupName()) == null )
      group = "";
   
   viewHandler.setSelectedWrappers( getGroupRecords( group ) );
}

/** This local method reports a container property change. This will issue a
 *  change event of the container provided the value of the specified property 
 *  has changed since last report. It is primarily used by constitutive submodules 
 *  like the view managers.
 *  
 *  @param property one of property or event identifiers of this class 
 */  
@Override
protected void reportPropertyChange ( int property )
{
   int value;
   
   if ( isEventPaused() ) return;
   
   switch ( property )
   {
   case OPERATION_MODE:
      // conditional dispatch operation mode
      value = getOperationMode(); 
      if ( value != reportedOperationMode )
         fireChangeEvent( OPERATION_MODE );
      reportedOperationMode = value;
      break;
         
   case SELECTION_STATUS:
   case SELECTION_EVENT:
	  // update the current selected record info in minor options
	  PwsRecord record = getSelectedRecord();
	  if ( record != null && 
		(isModified() || !Options.isOptionSet("storeMinorChanges")) ) {
		  String hstr = record.getRecordID().toHexString();
		  getMinorOptions().setOption("selectedRecord", hstr );
		  Log.debug(10, "(PwsFileContainer.reportPropertyChange) updated SELECTED RECORD in minor options: "
				  .concat(hstr));
	  }
	   
      // always dispatch selection event
      fireChangeEvent( SELECTION_EVENT );

      // conditional dispatch selection mode
      value =  getSelectionStatus(); 
      if ( value != reportedSelectionMode )
         fireChangeEvent( SELECTION_STATUS );
      reportedSelectionMode = value;
      break;
      
   case DISPLAY_MODE:
      // conditional dispatch display mode
      value = getViewType();
      if ( value != reportedViewType )
         fireChangeEvent( DISPLAY_MODE );
      reportedViewType = viewType;
      break;

   case MODIFY_EVENT:
	  if ( modifyNumber == mirrorNumber && isModified() ) { 
		  modifyNumber++;
	      Log.debug( 10, "(PwsFileContainer.reportPropertyChange) MODIFY-NUMBER ++" );
	  }
      
   default:
      super.reportPropertyChange( property );
   }
}  // reportPropertyChange

/** Pushes the parameter record into the "last used" convenience list. 
 *  @since 0-4-0
 */
public void recordUsed ( PwsRecord record )
{
   if ( !containsRecord( record ) ) return;
   lastUsedEntries.pushRecent( record );
}

/**
 * Whether the quick find function panel is visible (available to the user)
 * in this container's display.
 * 
 * @return boolean
 * @since 0-6-0
 */
public boolean isQuickFindActive ()
{
   return quickfindPanel.isVisible();
}

/**
 * Whether one of the classical filtering options is active
 * in this container's display. (Not valid for "QuickFind"!)
 * 
 * @return boolean
 * @since 0-6-0
 */
public boolean isFilterActive ()
{
   return getFilterStatus() != FILTER_OFF;
}

/**
 * Sets the visibility (availability) of the quick find function panel
 * in this container's display. Setting this <b>true</b> will grab the 
 * focus to the panel's text input field; setting <b>false</b> will
 * remove any previous text setting for the filter.
 *  
 * @param v boolean new activation state for the quick find panel
 * @since 0-6-0
 */
public void setQuickFindActive ( boolean v )
{
   ActionHandler.executeOnEDT( new PanelSwitcher( VisiblePanel.Quickfind, v, null ) );
}

public void pushRecentFindValue( String text )
{
   Log.debug( 5, "(PwsFileContainer.pushRecentFindValue) add find expression: ".concat( text ) );
   getRecentFinds().pushRecent( text );
   quickfindField.setDataList( getRecentFinds().getContent() );
}

public void removeRecentFindValue( String text ) {
   Log.debug( 5, "(PwsFileContainer.removeRecentFindValue) remove find expression: ".concat( text ) );
   getRecentFinds().removeRecent( text );
   searchExpressions.removeRecent(text);
   quickfindField.setDataList( getRecentFinds().getContent() );
   pageSearchExpressions(WHEREIS);
//   quickfindField.setText(null);
}

public void setFont ( Font font ) {
   treeHandler.setFont( font );
   tableHandler.setFont( font );
}

/** Triggers a user activity signal for this container. */
public void userActivity () {
   objectListener.activity();
}

//*********** MIRRORS DATA SECURITY  **************

/** Returns a unique file definition for the mirror file 
 * of this container. This definition is based on the filepath
 * feature of the database and should return same results as long 
 * as the filepath hasn't changed. If a database has no filepath,
 * a random temporary file is returned. 
 * (NOTE: This renders a definition for a file. It does not
 * imply this file already exists!)
 * 
 * @param ContextFile mirror file definition of container
 */
protected ContextFile getMirrorFileDef () throws IOException
{
   ContextFile cf;
   String path;
   
   // create the mirror file
   if ( (path = getFilePath()) == null || path.length() == 0 )
      // .. for a new database
      cf = IOManager.getTemporaryFile( 
            Global.DEFAULT_BACKUPEXTENTION, Global.mirrorDir );
   else
      // .. for a database with persistent state definition
      cf = Global.getMirrorOf( path );
   return cf;
}

/** This method checks whether a mirror of this database is present in the 
 * JPWS mirror directory. If so, it moves the mirror file into a special 
 * directory for this database and informs the user accordingly.
 * Requires to run on EDT!
 */
public void controlMirrors ()
{
   ContextFile mir=null, copy;
   MessageDialog infoDlg;
   FileOpenDialog openDlg;
   PwsListCompareResult compRes;
   PwsFile db;
   File mdir, f, mfiles[]=null;
   String hstr, text;
   long thisModTime;
   int i;
   boolean ok;
   
   try
   {
	  ActionHandler.checkForEDT();
      mir = getMirrorFileDef();
      hstr = Util.fileNameOfPath( mir.getFilepath() ).substring( 5, 21 );
      mdir = new File( Global.mirrorDir, hstr );

      // if a MIRROR exists for this database (primary occurrence)
      if ( mir.exists() ) {
         // copy mirror file into private mirror directory
         // create private directory if needed
         Util.ensureDirectory( mdir, null );
         copy = IOManager.getTemporaryFile( Global.DEFAULT_BACKUPEXTENTION, mdir );
         mir.copyTo( copy );
         copy.setModifyTime( mir.modifyTime() );
         Log.debug( 7, "(PwsFileContainer.controlMirrors) created private MIRROR copy: "
               .concat( copy.getFilepath() ));

         // delete original mirror file
         mir.delete();
      }
      
      // check for irrelevant mirror files and delete them
      thisModTime = getPwsFile().lastModified();
      if ( (mfiles=mdir.listFiles()) != null && mfiles.length > 0 )
      for ( i = 0; i < mfiles.length; i++ ) {
         // only investigate mirrors which would be MERGED (opposed of REPLACED)
         f = mfiles[ i ];
         if ( f.lastModified() <= thisModTime ) {
            // open a mirror DB in direct access mode
            db = DatabaseHandler.openFilePws( IOManager.makeLocalContextFile( f ), 
                  getPassphrase(), FileAccessModus.directAccess );

            // compare mirror content to this database
            compRes = Service.comparePwsLists( getPwsFile(), db );
            if ( compRes.conflict.size() == 0 &&
                 compRes.only_B.size() == 0 )
            {
               // delete mirror file if there is no conflicting content in it
               if ( f.delete() )
               Log.debug( 7, "(PwsFileContainer.controlMirrors) removed irrelevant MIRROR : ".
                     concat( f.getAbsolutePath() ) );
            }
         }
      }
      
      // check for erasing the database-private directory 
      if ( mdir.isDirectory() && (mfiles=mdir.listFiles()).length == 0 ) {
         mdir.delete();
         Log.debug( 7, "(PwsFileContainer.controlMirrors) removed private MIRROR directory: "
               .concat( mdir.getAbsolutePath() ));
      }

      // inform user about existence of security copies
      ok = true;
      while ( ok && mdir.exists() ) {
         Frame frame = Global.getActiveFrame();
         
         // inform user
         text = ResourceLoader.getDisplay( "msg.mirror.warning" ); 
         text = Util.substituteText( text, "$file", getFileName() );
         text = Util.substituteText( text, "$mirrordir", mdir.getAbsolutePath() );
         
         infoDlg = new MessageDialog( frame, DialogButtonBar.OK_CANCEL_BUTTON, true );
         infoDlg.moveRelatedTo( frame );
         infoDlg.setTitle( ResourceLoader.getDisplay( "dlg.mirrorwarning" ) );
         infoDlg.setText( text );
         infoDlg.getButtonBar().getOkButton().setText( ResourceLoader.getDisplay( "button.show" ) );
         infoDlg.getButtonBar().getCancelButton().setText( ResourceLoader.getDisplay( "button.later" ) );
         infoDlg.pack();
         infoDlg.show();
         infoDlg.dispose();

         // if user opted, private mirror directory is investigated 
         if ( (ok=infoDlg.isOkPressed()) ) {
            openDlg = new FileOpenDialog( FileOpenDialog.BACKUP_FILTER, mdir );
            ok = openDlg.showOpenDialog( frame ) == JFileChooser.APPROVE_OPTION;
            if ( ok ) {
               ResolveMirrorDialog dlg = new ResolveMirrorDialog( this, openDlg.getSelectedFile() );
               dlg.show();
               ok = dlg.isOkPressed();
            }
         }
         
         // check for erasing the database-private directory 
         if ( ok && mdir.isDirectory() && (mfiles=mdir.listFiles()).length == 0 ) {
            mdir.delete();
            Log.debug( 7, "(PwsFileContainer.controlMirrors) removed private MIRROR directory: "
                  .concat( mdir.getAbsolutePath() ));
         }
      }
   } catch ( IOException e ) {
      e.printStackTrace();
      hstr = "WARNING! Cannot copy Mirror file<br><font color=\"green\">" + 
      mir != null ? mir.getFilepath() : "??" + "</font>";
      GUIService.failureMessage( hstr, e );
   }
}  // controlMirrors

/** Erases the mirror file of this container on external medium. */
protected void removeMirrorFile ()
{
   if ( mirrorFile != null )
   {
      try { Global.removeRandomMirror( mirrorFile ); } 
      catch (IOException e) 
      { e.printStackTrace(); }
      
      mirrorFile = null;
      mirrorNumber = modifyNumber;
   }
}

/** Sets a specific mirror file definition for this database.
 * @param file <code>ContextFile</code> mirror file definition
 */
void setMirrorFile ( ContextFile file )
{
   if ( file != null )
   {
      removeMirrorFile();
      mirrorFile = file;
   }
}

@Override
public void setFilePath( String path ) 
{
   removeMirrorFile();
   super.setFilePath(path);
   refreshView();
}

/**
 * Triggers off mirror save activity for this container's database,
 * in case it was modified since the last mirror save.
 * 
 * @return boolean <b>false</b> if an error occurred preventing the save operation
 *                 and <b>true</b> otherwise
 */
public boolean checkMirrorActivity ()
{
   boolean ok = true;
   
   if ( modifyNumber != mirrorNumber && Options.isOptionSet( "useDataMirrors" ) 
//        && !Global.isCriticalPhase( getContextFile() ) 
      )
   try {
      // create mirror file definition if not present
      mirrorNumber = modifyNumber;
      if ( mirrorFile == null ) {
         mirrorFile = getMirrorFileDef();
      }

      // create a mirror copy of the database of this container 
      Log.debug( 5, "(PwsFileContainer.checkMirrorActivity) saving MIRROR file for: "
            .concat( getDatabaseName() ));
      Log.debug( 5, "(PwsFileContainer.checkMirrorActivity) saving MIRROR to: "
            .concat( mirrorFile.getFilepath() ));
      Util.ensureDirectory( Global.mirrorDir, null );
      super.saveCopy( mirrorFile, getPassphrase(), true );
      mirrorNumber = modifyNumber;
      Log.debug( 10, "(PwsFileContainer.checkMirrorActivity) MODIFY-NUMBER ==" );
      Log.debug( 10, "(PwsFileContainer.checkMirrorActivity) mirrorNr==" + mirrorNumber +
            ", modifyNr=" + modifyNumber );
      
      if ( !hasPersistentFile() ) {
         Global.addRandomMirror( mirrorFile );
      }

   } catch ( Exception e ) {
      e.printStackTrace();
      String path = getFilePath();
      String hstr = "WARNING! Cannot save Mirror file for<br><font color=\"green\">" + 
             ( path == null ? getDatabaseName() : path ) + "</font>";
      GUIService.failureMessage( hstr, e );
      ok = false;
   }
   
   return ok;
}

private Future<?> pagingFindRunner;

private void pageSearchExpressions ( final int direction ) 
{
    // operation conditions
    final int leftTop = searchExpressions.getSize(); 
    if ( leftTop > 0 &&
         (direction == LEFT & leftTop-1 > searchExprIndex ||
         direction == RIGHT & searchExprIndex > 0) ||
         direction == WHEREIS )
    {
        // break operation if another of this type is still running
        if ( pagingFindRunner != null && !pagingFindRunner.isDone() )
           return;

        // create paging thread
        Runnable task = new Runnable() {
            @Override
			public void run () {
               // get "paged" search expression
               int newIndex;	
               if ( direction == WHEREIS  ) {
            	  newIndex = Math.min(searchExprIndex, leftTop-1);
               } else {
                  newIndex = searchExprIndex + (direction == RIGHT ? -1 : 1);
               }
               searchExprIndex = newIndex;
               String expr = searchExpressions.getStringValue( newIndex );

               // call search executor
               quickFindRun( expr, true );
            }
        };
        pagingFindRunner = ActionHandler.startTask(task);
    }
}

private void quickFindRun ( String expr, boolean isPaging )
{
    DefaultRecordWrapper[] selRecs =null;
    String text;
    int count;
    boolean hasText;

    synchronized ( quickFindLock ) 
    {
        selRecs = viewHandler.getSelectedWrappers();
    
        // get search value
        text = expr;
        hasText = text != null && !text.isEmpty();
        Log.debug( 5, "(PwsFileContainer.quickFindRun) -- executing QUICK FIND for: [" + text + "]" );
        
        // memorise tree expansion state on first find filter text
        // reset tree expansion state if text is void (except other filtering)
        if ( hasText )
           treeHandler.markTreeExpansionState();
        else if ( !isFilterActive() )
           treeHandler.resetTreeExpansionState();
    
        // initiate find filter function (triggers list reload in tree)
        setTextFindFilter( text );
        
        // adjust tree screenView with "all-expanded" state if there is filtering active 
        if ( hasText | isFilterActive() )
           setExpandedBranch( "", true );
        
        // reinstall record selection
        viewHandler.setSelectedWrappers( selRecs );
    
        // determine actual filtered list size and display in QF panel
        count = getOrderedList().size();
        quickfindNumberLabel.setText( isTextFindFilterActive() ? String.valueOf(  count ) : null );
        quickfindField.requestFocusInWindow();

        // paging update activity
        if ( hasText ) {
           if ( isPaging ) {
              // avoid event overrun when setting field value for display only 
              quickfindAdapter.setListening(false);
               
              // update value display (textfield; if PAGING)
              quickfindField.setText(text);
              quickfindAdapter.setListening(true);

           } else {
              // update recent search expression list (if not PAGING)
              searchExpressions.pushRecent(text);
              searchExprIndex = 0;
           }
        } else {
        	quickfindField.setText(null);
        }
        updatePagingButtons();
    }
}

private void updatePagingButtons ()
{
    int listSz = searchExpressions.getSize();
    naviLeftButton.setEnabled( listSz > 0 && listSz - 1 > searchExprIndex );
    naviRightButton.setEnabled( listSz > 0 && searchExprIndex > 0 );
}

/** Returns the tree-node expansion info used by the TreeHandler class.
 * (The info is retrieved either from a file header field ot from minor options,
 * depending which is the younger.)
 *  
 * @return String expansion info or <b>null</b> if unavailable
 */
public String getTreeExpansionInfo() {
	boolean optionAccess = getStoreTime() < getMinorOptions().getStoreTime(); 
	String info = optionAccess ? getMinorOptions().getOption("treeExpansionString") 
			      : getHeaderValue( PwsFileSocket.HEADERFIELD_TREEINFO );
	return info == null || info.isEmpty() ? null : info;
}

/** Returns a copy of the active passphrase policy for this container.
 * (This is always valid; if no policy is found in the file settings, the
 * global policy is returned.)
 *  
 *  @return <code>PwsPassphrasePolicy</code>
 */
public PwsPassphrasePolicy getPassphrasePolicy() {
	String hstr = getMajorOptions().getOption("passwordPolicy");
	return hstr.isEmpty() ? (PwsPassphrasePolicy)Global.passphrasePolicy.clone() 
			: new PwsPassphrasePolicy(hstr);
}

//  *********** IMPLEMENTS ACTIONLISTENER  **************

/**
 * This action handler is added to components which are element of or
 * triggered off by this file container. It reacts to buttons pressed
 * for specific purposes that must be dealt with within the container
 * or the ESC key pressed.
 * <p>It currently listens to AddDialog (record editor), the Quickfind 
 * bar and the ContainerView (for ESC button). 
 */
@Override
public void actionPerformed ( ActionEvent arg )
{
   UndoManager.ModifyRecordEdit modEdit;
   PwsRecord record;
   String command, hstr;
   
   command = arg.getActionCommand();
   if ( command == null ) {
	   command = "";
   }
   
   // reacts to Terminate button on Quickfind bar
   if ( command.equals( "viewport.cancelquickfind" ) ) {
      setQuickFindActive( false );
   }
   
   // this acts to the "memorise expression" button in the Quickfind bar
   else if ( command.equals( "quickfind.addexpression" ) ) {
      if ( (hstr = quickfindField.getText()).length() != 0 ) {
         Log.log( 5, "(button action: add quickfind expression: " + hstr + ")" );
         pushRecentFindValue( hstr );
      }
   }
   
   // this acts to the "remove expression" button in the Quickfind bar
   else if ( command.equals( "quickfind.removeexpression" ) ) {
      if ( (hstr = quickfindField.getText()).length() != 0 ) {
         Log.log( 5, "(button action: remove quickfind expression: " + hstr + ")" );
         removeRecentFindValue( hstr );
      }
   }
   
   // Button "Left-Arrow" in Quickfind panel 
   else if ( command.equals( "quickfind.pageleft" ) ) {
       pageSearchExpressions( LEFT );
   }
   // Button "Right-Arrow" in Quickfind panel 
   else if ( command.equals( "quickfind.pageright" ) ) {
       pageSearchExpressions( RIGHT );
   }

   // this handles actions when the record editor returns
   else if ( command.equals( "dialog.action.ok" ) || 
		     command.equals( "dialog.action.cancel" ) ) {
      if ( editDialog == null ) return;
   
      record = editDialog.getRecord();
      random = record.getSignature();
      try {
         // EDITOR OK branch
         if ( command.equals( "dialog.action.ok" ) ) {
             Log.log(7, "(PwsFileContainer.actionPerformed) Command EDIT-RECORD OK-Button ");
            // CASE RECORD EDIT MODUS
            if ( !editDialog.isNewRecord() ) {
               // if record was modified	
               if ( editDialog.isRecordModified() ) {
                  // update record if modified during edit session
                  updateRecord( record );
                  Log.log(7, "(PwsFileContainer.actionPerformed) AFTER-EDIT: record updated: " + record);
      
                  // notify subsystems about record modified 
                  if ( editDialog.getInitRecord().isValid() ) {
                     // create undoable edit object
                     modEdit = new UndoManager.ModifyRecordEdit( UndoManager.ModifyRecordEdit.MODIFY_RECORD_EDIT,
                           this, editDialog.getInitRecord(), record );
                     fireEditEvent( modEdit );
                     Log.log(7, "(PwsFileContainer.actionPerformed) AFTER-EDIT: pushed UNDOABLE");
                     
                     // push to last-edited list
                     lastEditedEntries.pushRecent( record );
                     GUIService.statusConfirm("msg.confirm.modifyrec", record.getTitle() );
                     fileUpdated();
                  }
               }
            }
            
            // CASE NEW RECORD MODUS
            else {
               addRecord( record );
               Log.log(7, "(PwsFileContainer.actionPerformed) AFTER-EDIT: record added: " + record);

               // create undoable edit object
               modEdit = new UndoManager.ModifyRecordEdit( UndoManager.ModifyRecordEdit.NEW_RECORD_EDIT,
                     this, null, record );
               fireEditEvent( modEdit );
               Log.log(7, "(PwsFileContainer.actionPerformed) AFTER-EDIT: pushed UNDOABLE");
                     
               lastEditedEntries.pushRecent( record );
               GUIService.statusConfirm("msg.confirm.newrec", record.getTitle() );
               fileUpdated();
            }
         }  // "dialog.action.ok"
         
      } catch ( Exception e ) {
         e.printStackTrace();
         GUIService.failureMessage( "dlg.operfailure", e );

      } finally {
    	 Log.log(10, "(PwsFileContainer.actionPerformed) -- resetting editDialog to null"); 
         editDialog = null;
         record = null;
         fireChangeEvent( EDITOR_EVENT );
      }
   }

   else {
       Log.debug(5, "(PwsFileContainer.actionPerformed) *** unrecognised command: ".concat(command) );
   }
}  // actionPerformed

//*********** IMPLEMENTS OptionChangeListener  **************

// since 0-6-0
@Override
public void optionChanged ( OptionChangeEvent e )
{
   String name;
   boolean chk;
   
   super.optionChanged( e );
   
   name = e.getOptionName();
   if ( name.equals( "findTextOpt" ) && isQuickFindActive() )
   {
      quickfindField.setCaseSensitive( searchCaseSensitive );
      if (  isTextFindFilterActive() )
         setTextFindFilter( getFindFilterText() );
   }
   
//   else if ( name.equals( "storeMinorChanges" )  )
//   {
//      quickfindField.setDataList( getRecentFinds().getContent() );
//   }
//   
   else if ( name.equals( "usedEntryListLength" )  )
   {
      lastUsedEntries.setMaxEntries( Options.getIntOption( name ) );
      lastEditedEntries.setMaxEntries( Options.getIntOption( name ) );
   }

   else if ( name.equals( "viewCurtainTime" )  )
   {
      maxIdleSeconds = Options.getIntOption( "viewCurtainTime" ) * 60;
   }
   else if ( name.equals( "useContainerLockedView" )  )
   {
      chk = allowLockedView;
      allowLockedView = Options.isOptionSet( "useContainerLockedView" );
      if ( !chk & allowLockedView ) 
         idleSeconds = 0;
   }
//   else if ( name.equals( "useUndoRedo" )  )
//   {
//      chk = Options.isOptionSet( "useUndoRedo" );
//      if ( treeHandler != null )
//         treeHandler.setDeleteKeyEnabled( chk );
//      if ( tableHandler != null )
//         tableHandler.setDeleteKeyEnabled( chk );
//   }
   else if ( name.equals( "restrictAccelerators" )  )
   {
	   keyStrokeHandler.readContainerKeys();
   }
}   

// ************ IMPLEMENTS ActivitySource Interface **************

@Override
public void addActivityListener ( ActivityListener listener )
{
   objectListener.addActivityListener( listener );
}

@Override
public void removeActivityListener ( ActivityListener listener )
{
   objectListener.removeActivityListener( listener );
   
}



// ************  INNER CLASSES  *************

public static final String RECORDTRANSFERFLAVOR_NAME = "PWS Record Transfer Data";
public static final DataFlavor RECORDTRANSFERFLAVOR = new DataFlavor( UUID[].class, RECORDTRANSFERFLAVOR_NAME );

/** Record structure to represent the source file data commitment for transfer. 
 *  @since 0-6-0
 * */
public static class TransferableData implements Serializable
{
   private static final long serialVersionUID = -9218780935966385600L;
   
   /** UUID identifier of the source file (as obtainable in DisplayManager). 
    * Should be not <b>null</b>. */
   public UUID socket;
   /** Set of database entries of the source file to be transferred. 
    * Should be not <b>null</b>. */
   public UUID[] records;
   /** <tt>group</tt> not <b>null</b> means that an entire group has been selected
    * in the source file for transfer. The value holds the full name of the group.
    * <tt>group</tt> == <b>null</b> means that no group has been selected.
    */
   public String group;
}

/** This class extends java.awt.datatransfer.Transferable of the Java
 *  Drag and Drop mechanism. It creates and harbours the {@link TransferableData}
 *  data record from the current source file state as given through constructor 
 *  parameters. It represents the special Data Flavour for this type of operation.
 *  @since 0-6-0
 */
public static class RecordListTransferable implements Transferable
{
   private TransferableData data;

   public RecordListTransferable ( PwsFileSocket file, 
                                   DefaultRecordWrapper[] records, 
                                   String selectedGroup )
   {
      if ( file == null | records == null )
         throw new NullPointerException();

      data = new TransferableData();
      data.socket = file.getUUID();
      data.records = new UUID[ records.length ];
      for ( int i = 0; i < records.length; i++ )
         data.records[ i ] = records[ i ].getRecordID();
      data.group = selectedGroup;
      
      Log.debug( 8, "(RecordListTransferable.<init>) creating transferable with " + records.length +
            " records, group selected = " + selectedGroup );
   }

   @Override
public Object getTransferData ( DataFlavor flavor ) throws UnsupportedFlavorException, IOException
   {
      if ( !flavor.equals( RECORDTRANSFERFLAVOR ) )
         throw new UnsupportedFlavorException( flavor );
      return  data;
   }

   @Override
public DataFlavor[] getTransferDataFlavors ()
   {
      return new DataFlavor[] { RECORDTRANSFERFLAVOR };
//      return new DataFlavor[] { DataFlavor.getTextPlainUnicodeFlavor() };
   }

   @Override
public boolean isDataFlavorSupported ( DataFlavor flavor )
   {
      return flavor.equals( RECORDTRANSFERFLAVOR );
//      return flavor.equals( DataFlavor.getTextPlainUnicodeFlavor() );
   }
}  // class RecordListTransferable


@SuppressWarnings("serial")
private class RecordListTransferHandler extends TransferHandler
{
   int dropAction = DnDConstants.ACTION_NONE;
   DragSourceListener dsListener = new ActionChangeListener(); 
   
   private class ActionChangeListener extends DragSourceAdapter
   {
      @Override
	  public void dropActionChanged ( DragSourceDragEvent dsde ) {
         dropAction = dsde.getUserAction();
         Log.log( 7, "(PwsFileContainer - ActionChangeListener Ch) user action = ".concat( String.valueOf( dropAction )) );
      }

      @Override
	  public void dragEnter ( DragSourceDragEvent dsde ) {
         dropAction = dsde.getUserAction();
         Log.log( 7, "(PwsFileContainer - ActionChangeListener En) user action = ".concat( String.valueOf( dropAction )) );
      }
   }

   /**
    * 
    */
   public RecordListTransferHandler ()
   {
      super();
      DragSource.getDefaultDragSource().addDragSourceListener( dsListener );
   }

   /** Cleans up references to this TransferHandler when it is no longer needed. */
   public void exit ()
   {
      DragSource.getDefaultDragSource().removeDragSourceListener( dsListener );
   }
   
   @Override
   public boolean canImport ( JComponent comp, DataFlavor[] transferFlavors )
   {
      if ( comp != null & transferFlavors != null ) {
         for ( DataFlavor flv : transferFlavors ) {
            if ( flv.equals( RECORDTRANSFERFLAVOR ) )
               return true;
         }
      }
      return false;
   }

   @Override
   protected Transferable createTransferable ( JComponent c )
   {
      DefaultRecordWrapper recs[];
      String group = null;
      
      int s = getSelectionStatus();
      if ( s == GROUP_SELECTED ) {
         group = getSelectedGroupName();
         recs = getGroupRecords( group );
      }
      else {
         recs = getSelectedRecords();
      }

      return recs == null ? null : new RecordListTransferable( PwsFileContainer.this, recs, group );
   }
/*
   protected void exportDone ( JComponent s, Transferable data, int action )
   {
      TransferableData tdata;
      UUID list[];
      PwsRecordList exportRecs;
      PwsRecord record;
      int i;
      
      try {
         tdata = (TransferableData)data.getTransferData( RECORDTRANSFERFLAVOR );
         Log.debug( 8, "(RecordListTransferHandler.exportDone) transferred set: " + 
               tdata.records.length + " records" );
   
         if ( action == MOVE )
         {
            // create source record list from UUID list
            list = tdata.records;
            exportRecs = new PwsRecordList();
            for ( i = 0; i < list.length; i++ )
            {
               record = getRecord( list[i] );
               if ( record != null )
                  exportRecs.addRecord( record );
            }
   
            // remove exported record list from this file container
            deleteRecordList( exportRecs );
            Log.debug( 8, "(RecordListTransferHandler.exportDone) deleted records: " + exportRecs.getRecordCount() ); 
         }
      }
      catch ( Exception e )
      {
         e.printStackTrace();
      }
   }
*/   

   @Override
   public int getSourceActions ( JComponent c )
   {
      return COPY_OR_MOVE;
   }

   private void selectRecords ( PwsRecordList records )
   {
      Log.debug( 7, "(RecordListTransferHandler.selectRecords) selecting " + records.size() + " records" );
//      try { Thread.sleep( 100 ); }
//      catch ( InterruptedException e )
//      {}
      getViewHandler().setSelectedWrappers( records.toRecordWrappers(null) );
   }
   
   @Override
   public boolean importData ( final JComponent comp, final Transferable t )
   {
      TransferableData tdata;
      PwsFileContainer source;
      PwsRecordList importRecs, actionRecs, oldStateRecs;
      PwsRecord record, oldRec;
      String group, srcTrunk, srcTrail;
      String targetGroupName, recgrp, oldGroup, hstr;
      Iterator<PwsRecord> it;
      DefaultRecordWrapper[] newState, oldState;
      DefaultRecordWrapper wrap;
      UndoableEdit edit;
      long modTime;
      int i, opt, editType;
      boolean sameFile, transferGroup, copyAction, performed, yesToAll;
      
      try {
         // identify and get the source file (from transferred file UUID)
         // return undone if there is nothing to do or data error
         tdata = (TransferableData)t.getTransferData( RECORDTRANSFERFLAVOR );
         if ( tdata.records.length == 0 || tdata.socket == null ||
              (source = DisplayManager.getFileContainer( tdata.socket )) == null )
            return false;
         
         // the group of drop target position
         // (is the group implied by current cursor/selection position in target list) 
         if ( (group = getSelectedGroupName()) == null )
            group = "";

         // decide on some switches and group value analysis
         copyAction = dropAction != DnDConstants.ACTION_MOVE;
         sameFile = source == PwsFileContainer.this;
         srcTrunk = srcTrail = targetGroupName = null;
         if ( transferGroup = tdata.group != null ) {
            srcTrunk = groupAncestors( tdata.group );
            srcTrail = tdata.group.substring( srcTrunk.length() );
            if ( srcTrunk.length() > 0 )
               srcTrunk = srcTrunk.substring( 0, srcTrunk.length()-1 );
            targetGroupName = comp instanceof JPanel ?  tdata.group
                  : tdata.group.length() == 0 ? group : getNewGroupName( group + "." + srcTrail );
         }
         
         Log.debug( 8, "(RecordListTransferHandler.importData) importing record set of " + tdata.records.length +
               ", same file = " + sameFile );
         
         // create source record list from transferred UUID list
         importRecs = new PwsRecordList();
         for ( i = 0; i < tdata.records.length; i++ ) {
            record = source.getRecord( tdata.records[i] );
            if ( record != null ) {
               importRecs.addRecord( record );
            }
         }
         oldState = importRecs.toRecordWrappers(null);
         oldGroup = transferGroup ? tdata.group : "";

         // COPY ACTION identical for both SAME-FILE and EXTERNAL
         if ( copyAction ) {
            // break operation if invalid record contained in source list
            if ( importRecs.hasInvalidRecs() ) {
               GUIService.failureMessage( "msg.failure.transfer", null );
               if ( sameFile ) {
                  selectRecords( importRecs );
               }
               return false;
            }
            
            // COPY (duplicate) records of this action
            Log.debug( 8, "(RecordListTransferHandler.importData) COPY to group: " + group );
            if ( transferGroup )
               importRecs = new PwsRecordList( source.groupDuplicates( tdata.group ) );
            else
               importRecs = new PwsRecordList( duplicateRecords( oldState ) );
            
            // correct GROUP value of cloned records according to type
            // of user selection in source (RECORD or GROUP)
            for ( it = importRecs.iterator(); it.hasNext(); ) {
               // get import record
               record = it.next();
               
               // set new group value for import record
               hstr = group;
               if ( transferGroup ) {
                  // if complete group is transferred, compile
                  // new group names from constant trunk (target)
                  // and record individual trail (source) elements
                  recgrp = record.getGroup();
                  hstr = targetGroupName.concat( "." ).concat( 
                        recgrp != null ? recgrp.substring( tdata.group.length() ) : "" );
               }
               record.setGroup( hstr );
               importRecs.updateRecord( record );
            }
            
            // add cloned records to target
            addRecordList( importRecs );

            // prepare undoable edit event
            if ( sameFile ) {
               editType = transferGroup ? UndoManager.ModifyRecordEdit.COPY_GROUP_EDIT  
                               : UndoManager.ModifyRecordEdit.COPY_RECORD_EDIT;
            } else {
               editType = UndoManager.ModifyRecordEdit.TRANSFER_RECEIVE_EDIT;
               oldState = null;
               group = Util.topMostStr(source.getDatabaseName(),40);
            }
            newState = importRecs.toRecordWrappers(null);
            actionRecs = importRecs;

         } else
            
         // MOVE ACTION
         // SAME FILE - move to group if source and target file are identical

            if ( sameFile )
         {
            // MOVE records action
            Log.debug( 8, "(RecordListTransferHandler.importData) INTERNAL MOVE to group: " + group );
            
            // move selected group
            if ( transferGroup ) {
               // break operation if target and source group are identical
               if ( srcTrunk.equals( group ) )   
                  return false;
               
               // FIXME ?? newState not updated?
               newState = getGroupRecords( tdata.group );
               editType = UndoManager.ModifyRecordEdit.MOVE_GROUP_EDIT;
               Log.debug( 7, "(RecordListTransferHandler.importData) MOVING group of " + newState.length  
                     + ", source=" + tdata.group + ", target group=" + targetGroupName );
               moveGroup( tdata.group, targetGroupName, true );

            // move selected records
            } else {
               newState = importRecs.toRecordWrappers(null);
               editType = UndoManager.ModifyRecordEdit.MOVE_RECORD_EDIT;
               Log.debug( 7, "(RecordListTransferHandler.importData) MOVING records: " + newState.length +
                     ", target group=" + group );
               newState = moveEntries( newState, group, false );
            }
            
            // prepare undoable edit event
            actionRecs = importRecs;
         }

         // EXTERNAL SOURCE - add import records to this file container
         // by controlling double entries through user confirmed overwrite
         else
         
         {
            Log.debug( 8, "(RecordListTransferHandler.importData) copy/move to file: " +
                  getDatabaseName() + " -> " + group );
            actionRecs =  new PwsRecordList();
            oldStateRecs = new PwsRecordList();
            opt = -1;
            yesToAll = false;
            
            for ( it = importRecs.iterator(); it.hasNext(); )
            {
               // get import record
               record = it.next();
               modTime = record.getModifiedTime();
               performed = false;

               if ( comp != null )
               {
                  // set new group value for import record
                  hstr = group;
                  if ( transferGroup )
                  {
                     recgrp = record.getGroup();
                     hstr = targetGroupName.concat( "." ).concat( 
                            recgrp != null ? recgrp.substring( tdata.group.length() ) : "" );
                  }
                  record.setGroup( hstr );
               }
               
               // add or update import record
               performed = true;
               if ( containsRecord( record ) )
               {
                  // if record ID is already there
                  oldRec = getRecord( record.getRecordID() );
                  oldStateRecs.addRecord( oldRec );
                  if ( !record.isIdentical( oldRec ) )
                  {
                     if ( !yesToAll )
                     {
                        // ask user if he wants to overwrite existing record
                        wrap = new DefaultRecordWrapper( oldRec, null );
                        hstr = ResourceLoader.getDisplay( "msg.transfer.overwrite" );
                        hstr = Util.substituteText( hstr, "$rec1", wrap.toString() );
                        hstr = Util.substituteText( hstr, "$date1", Global.getLocalDateTime( oldRec.getModifiedTime() ));
                        hstr = Util.substituteText( hstr, "$group1", Util.topMostStr( wrap.getGroup(), 128 ));
                        wrap = new DefaultRecordWrapper( record, null );
                        hstr = Util.substituteText( hstr, "$rec2", wrap.toString() );
                        hstr = Util.substituteText( hstr, "$date2", Global.getLocalDateTime( modTime ));
                        
                        opt = GUIService.userConfirmListParsing(null, null, hstr, GUIService.YES_TO_ALL_OPTION );
                        yesToAll = opt == GUIService.YES_TO_ALL_OPTION;
                     }
                     
                     if ( opt == JOptionPane.YES_OPTION | yesToAll )
                     {
                        updateRecordRelaxed( record );
                     }
                     else
                     {
                        oldStateRecs.removeRecord( oldRec );
                        performed = false;
                        if ( opt == JOptionPane.CANCEL_OPTION )
                           break;
                     }
                  }
               }
               else 
               {
                  // if record ID is unknown
                  addRecordRelaxed( record );
               }
               if ( performed )
                  actionRecs.addRecord( record );
            }
            
            // remove transferred records from source file
            newState = actionRecs.toRecordWrappers(null);
            source.deleteTransferred( newState );
            source.getViewHandler().setSelectedWrappers( 
                  importRecs.excludeRecordList( actionRecs ).toRecordWrappers(null) );
            
            // prepare undoable edit
            editType = UndoManager.ModifyRecordEdit.TRANSFER_RECEIVE_EDIT;
            oldState = oldStateRecs.toRecordWrappers(null);
            oldGroup = null;
            group = Util.topMostStr(source.getDatabaseName(),40);
         }
         
         // create undoable edit event
         if ( newState != null && newState.length != 0 ) {
            edit = new UndoManager.ModifyRecordEdit( editType, PwsFileContainer.this, 
                   oldState, newState, oldGroup, group );
            fireEditEvent( edit );
         }
         
         // give EDT a time to work ??
         if ( actionRecs.size() != 0 ) {
            selectRecords( actionRecs );
         }

      } catch ( Exception e ) {
         GUIService.failureMessage( null, e );
         e.printStackTrace();
      }
      return false;
   }
}  // class RecordListTransferHandler


private class ViewportMouseAdapter extends java.awt.event.MouseAdapter
{
   private void tryPopup ( MouseEvent evt ) {
      if ( evt.isPopupTrigger() ) {
    	 JPopupMenu popup = MenuHandler.getListviewContextMenu(PwsFileContainer.this);
         popup.show( (Component)evt.getSource(), evt.getX(), evt.getY() );
      }
   }

   @Override
   public void mousePressed ( MouseEvent evt ) {
      tryPopup( evt );
   }

   @Override
   public void mouseReleased ( MouseEvent evt ) {
      tryPopup( evt );
   }

   @Override
   public void mouseClicked ( MouseEvent evt ) {
      objectListener.activity();
      if ( evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() > 1 ) {
         PwsRecord rec = getSelectedRecord();
         if ( rec != null ) {
            editEntry( rec );
         }
      }
   }
} // class ViewportMouseAdapter

private class SecondsTimerTask extends TimerTask 
{

   @Override
   public void run ()
   {
      idleSeconds++;
//      Log.debug( 6, "(PwsFileContainer.SecondsTimerTask) IDLE SECS = " + idleSeconds); 
      
      // check for screen locked state
      setLockedView( allowLockedView && idleSeconds > maxIdleSeconds );
   }

}

/** Class to extend the listener to user activity in objects. 
 */
private class FC_ObjectListener extends ObjectChangeListener
{
   /**
    * @param source Object
    */
   public FC_ObjectListener ( Object source )
   {
      super( source );
   }

   @Override
   protected void fireEvent ( EventObject evt )
   {
      idleSeconds = 0;
      super.fireEvent( evt );
   }
}

///**
// * This is a listener to the loaded PwsFile. We use it to get notice about
// * and react to some of the file modifications.
// * <p><u>TARGET_ALTERED</u>: will cause an event issued from this class
// * concerning alteration of file title.
// */  
//   protected void processFileEvent ( PwsFileEvent evt )
//   {
//      switch ( evt.getType() )
//      {
//      case PwsFileEvent.LIST_SAVED:
//      case PwsFileEvent.RECORD_UPDATED:
//      case PwsFileEvent.RECORD_REMOVED:
//      case PwsFileEvent.RECORD_ADDED:
//      case PwsFileEvent.LIST_UPDATED:
//      case PwsFileEvent.LIST_CLEARED:
//      }
//   }

@Override
public void orderedListPerformed ( OrderedListEvent evt )
{
   Log.log( 7, "(PwsFileContainer.orderedListPerformed) Event = ".concat( String.valueOf(evt.getType())) );

   // determine actual filtered list size and display in QF panel
   int count = getOrderedList().size();
   quickfindNumberLabel.setText( isTextFindFilterActive() ? String.valueOf(  count ) : null );

   // this switches between content dataView and EMPTY screen
   // depending on whether the list became empty or filled
   if ( getFilteredSize() == 0 ) {
      setEmptyView();
   } else { 
      setDataView( viewHandler == null ? null : viewHandler.getView() );
   }
}

@SuppressWarnings("serial")
private class RecentEntryList extends PwsFileRecentList 
{

/** Creates a new recent entry list.
 * 	
 * @param maxEntries int maximum list entries
 * @param token String option name for list content value
 * @param minor boolean whether minor file options are addressed (mayor otherwise)
 */
public RecentEntryList ( int maxEntries, String token, boolean minor )
{
   super( "edit.recent", maxEntries, token, minor );
}

@Override
public void setContent ( String content )
{
   clear();
   
   // this filters the content to include only real existing records
   String[] arr = Util.CSV.decodeLine( content, 0, ';' );
   StringBuffer buffer = new StringBuffer();
   int top = Math.min( arr.length, getMaxEntries() );
   for ( int i = 0; i < top; i++ ) {
      try {
         String hstr = arr[ i ];
         if ( containsRecord( new UUID(hstr) ) ) {
        	 buffer.append( hstr );
        	 if ( i < top-1 )  {
        		 buffer.append(';');
        	 }
         }
      } catch ( Exception e ) {
      }
   }

   // set content of recent list with filtered value
   content = buffer.toString();
   super.setContent( content );
}  // setContent

/** Inserts or re-orders the specified record in this RecentList.
 * 
 *  @param record PwsRecord whose ID will be entered into this list
 *  @return boolean <b>true</b> if and only if the content of recent list 
 *          has been modified by this operation
 */          
public boolean pushRecent ( PwsRecord record )
{
   return super.pushRecent( record.getRecordID().toHexString() );
}

/** Removes the specified record from this RecentList.
 * 
 *  @param record PwsRecord whose ID will be removed from this list
 *  @return boolean <b>true</b> if and only if the content of recent list 
 *          has been modified by this operation
 *  @since 0-6-0        
 */          
public boolean removeRecent( PwsRecord record )
{
   return super.removeRecent( record.getRecordID().toHexString() );
}

/** Tests whether the specified record is an element of this RecentList.
 * 
 *  @param record PwsRecord whose ID is tested for membership
 *  @return boolean <b>true</b> if and only if the parameter record is an
 *          element of this recent list
 *  @since 0-6-0        
 */          
public boolean hasRecent ( PwsRecord record )
{
   return super.hasRecent( record.getRecordID().toHexString() );
}

@Override
public void updateMenu ( JMenu menu )
{
   JMenuItem item;
   Iterator<?> it;
   UUID uid;
   String ident, name;
   
   synchronized ( vlist )
   {
      
      it = iterator();
      menu.removeAll();
      while ( it.hasNext() )
      {
         if ( (ident = it.next().toString()).length() != 0 )
         {
            // attempt to obtain listed record and title field 
            // if fail then just continue with next
            try { 
               uid = new UUID( ident );
               name = getRecord( uid ).getTitle();
            }
            catch ( Exception e )
            { continue; }
            
            // update menu item and command reference
            item = new JMenuItem( name );
            item.setActionCommand( getCommand() + ident );
            item.addActionListener( ActionHandler.getMainActionListener() );
            menu.add( item );
         }
      }
   }
}  // updateMenu

@Override
public void updateMenu ( Menu menu )
{
   MenuItem item;
   Iterator<?> it;
   UUID uid;
   String ident, name;
   
   synchronized ( vlist )
   {
      
      it = iterator();
      menu.removeAll();
      while ( it.hasNext() )
      {
         if ( (ident = it.next().toString()).length() != 0 )
         {
            // attempt to obtain listed record and title field 
            // if fail then just continue with next
            try { 
               uid = new UUID( ident );
               name = getRecord( uid ).getTitle();
            }
            catch ( Exception e )
            { continue; }
            
            // update menu item and command reference
            item = new MenuItem( name );
            item.setActionCommand( getCommand() + ident );
            item.addActionListener( ActionHandler.getMainActionListener() );
            menu.add( item );
         }
      }
   }
}  // updateMenu
}  // class RecentEntryList

/**
 * Extends class <code>RecordFilter</code> of PwsFileSocket in order to perform the
 * specific filtering option "Favourites" which takes reference into container data.
 * @since 0-6-0 
 */
private class RecordFilter extends PwsFileSocket.RecordFilter
{

   @Override
public boolean acceptEntry ( DefaultRecordWrapper record )
   {
      boolean ok;
      
      if ( !(ok = super.acceptEntry( record )) && getFilterStatus() == FILTER_FAVOURITES )
      {
         ok = isRecordFavourite( record.getRecord() ) && findFilterOk( record );
      }
      return ok;
   }
}  // class RecordFilter

private enum VisiblePanel { Quickfind, Filter, MirrorIndicator, BackupIndicator };

/** This structure serves to execute display of the various faces of the container
 * on the Event Dispatching Thread (EDT).
 */
private class PanelSwitcher implements Runnable 
{
   private VisiblePanel panel;
   private String text;
   private boolean toVisible;

   public PanelSwitcher ( VisiblePanel p, boolean visible, String text )
   {
      panel = p;
      toVisible = visible;
      this.text = text;
   }

   @Override
   public void run ()
   {
      switch ( panel ) 
      {
      case Quickfind:
         DefaultRecordWrapper[] selRecs =null;
         if ( toVisible )
         {
            Log.debug( 5, "-- ACTIVATE QUICK FIND" );
            quickfindPanel.setVisible( true );
            quickfindField.requestFocusInWindow();
            searchExprIndex = -1;
            escPrio = 'Q';
            updatePagingButtons();
         }
         else if ( isQuickFindActive() )
         {
            Log.debug( 5, "-- CANCEL QUICK FIND" );
            selRecs = viewHandler.getSelectedWrappers();
            quickfindPanel.setVisible( false );
            quickfindField.setText( null );
            
            if ( !isFilterActive() )
               treeHandler.resetTreeExpansionState();
            
            setTextFindFilter( null );
            if ( isFilterActive() )
               setExpandedBranch( "", true );
            
            viewHandler.setSelectedWrappers( selRecs );
            viewHandler.getView().grabFocus();
         }
         break;

      case Filter:
         filterBar.setText( text );
         filterPanel.setVisible( toVisible );
         break;

      case MirrorIndicator:
         isMirrorFileLabel.setVisible( toVisible );
         break;
   
      case BackupIndicator:
         isBackupFileLabel.setVisible( toVisible );
         break;
      }
   }
}

/**
 * This is only for testing purpose. 
 */
@SuppressWarnings("unused")
private static class EventReporter implements ChangeListener
{

   @Override
public void stateChanged ( javax.swing.event.ChangeEvent e )
   {
      PwsFileContainer ct;
      String propStr, valstr;
      int prop, val;
      
      prop = ((ChangeEvent)e).getState();
      ct = (PwsFileContainer)e.getSource();
      propStr = "?"; valstr = "?"; 

      if ( prop == FILTER_MODE | prop == MODIFY_EVENT )
         return;
      
      else if ( prop == OPERATION_MODE )
      {
         val = ct.getOperationMode();
         propStr = "OPERATION MODE";
         switch ( val )
         {
         case VIRGIN: valstr = "VIRGIN";
              break;
         case UNMOUNTED: valstr = "UNMOUNTED";
              break;
         case MOUNTED_PASSIVE: valstr = "MOUNTED_PASSIVE";
              break;
         case MOUNTED_ACTIVE: valstr = "MOUNTED_ACTIVE";
              break;
         }
      }
      else if ( prop == SELECTION_STATUS | prop == SELECTION_EVENT )
      {
         val = ct.getSelectionStatus();
         propStr = prop == SELECTION_EVENT ? "SELECTION EVENT" : "SELECTION STATUS";
         valstr = val == NOTHING_SELECTED ? "NOTHING" : 
                  val == RECORD_SELECTED ? "RECORD" : 
                  val == RECORDSET_SELECTED ? "RECORD SET" : "GROUP";   
      }
      else if ( prop == DISPLAY_MODE )
      {
         val = ct.getViewType();
         propStr = "DISPLAY MODE";
         valstr = val == NO_VIEW ? "NO VIEW" : val == TABLE_VIEW ? "TABLE" : "TREE";   
      }
      
      Log.debug( 5, "+++ New Container State: " + propStr + " == " + valstr ); 
   }

}


private class KeyStrokeHandler implements PwsFileListener
{
//   private HashMap<KeyStroke, Integer>   strokeRegistry = new HashMap<KeyStroke, Integer>();
   private Set<KeyStroke> restrictedKeys = new HashSet<KeyStroke>();
   private ActionMap actionMap; // = new ActionMap();
   private InputMap inputMap; // = new InputMap();
//   private JComponent component;

   private class KeyStrokeAction extends AbstractAction {
	   KeyStroke keyStroke;
	   UUID recordId;
	   
	   KeyStrokeAction ( KeyStroke stroke, PwsRecord record ) {
		   keyStroke = stroke;
		   if ( record != null ) {
			   recordId = record.getRecordID();
		   }
	   }
	   
		@Override
		public void actionPerformed (ActionEvent arg) {
			String command = keyStroke.toString();
	        Log.debug(10, "(PwsFileContainer.KeyStrokeHandler.actionPerformed) event received: " + command);
	        
		   if ( arg.getSource() == standardScreenPanel ) {
			   // handle ESC key pressed in Container window
			  if ( KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0).equals(keyStroke) ) {
			      if ( escPrio == 'F' )
			         if ( isFilterActive() )
			            setFilterStatus( PwsFileContainer.FILTER_OFF );
			         else 
			            setQuickFindActive( false );
			      else if ( escPrio == 'Q' )
			         if ( isQuickFindActive() )
			            setQuickFindActive( false );
			         else 
			            setFilterStatus( PwsFileContainer.FILTER_OFF );
			      return;
			  } 

			  // handle record editing keys
			  else if ( recordId != null & !isQuickFindActive() ) {
				  String hex = recordId.toString(); 
				  Log.debug(10, "(PwsFileContainer.KeyStrokeAction) received KEYSTROKE command for record ".concat(hex)); 
				  PwsRecord record = getRecord( recordId );
				  if ( record != null ) {
					  Log.debug(10, "(PwsFileContainer.KeyStrokeAction) starting to EDIT record "
							  .concat(record.getTitle())); 
					  editEntry(record);
				  } else {
					  Log.debug(10, "(PwsFileContainer.KeyStrokeAction) ***** UNKNOWN RECORD to EDIT: "
							  .concat(hex)); 
				  }
			  }
		   }
		}
   } // KeyStrokeAction
   
   /** Creates a new keystroke handler with reference to the given Swing 
    * component.
    * 
    * @param component <code>JComponent</code>
    */
   KeyStrokeHandler ( JComponent component ) {
	   super();
//	   this.component = component;
	   actionMap = new ActionMap();
	   actionMap.setParent(component.getActionMap());
	   component.setActionMap(actionMap);
	   inputMap = new InputMap();
	   int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
	   inputMap.setParent(component.getInputMap(condition));
	   component.setInputMap(condition, inputMap);
   }
   
	/** Clears the key mappings in this handler and loads new assignments from 
	 * the underlying database (all records). 
	 */
	public void readContainerKeys () {
		initRestrictedKeys();
		actionMap.clear();
		inputMap.clear();
		Log.log(10, "(PwsFileContainer.KeyStrokeHandler.readContainerKeys) cleared key map");

		// read in key-stroke references from database records
		for ( Iterator<PwsRecord> it = iterator(); it.hasNext(); ) {
			PwsRecord rec = it.next();
			updateKey(rec, null);
		}
		
		// fix-define ESCAPE key for quick-search function
		String cmd = "ESCAPE";
	    KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		actionMap.put(cmd, new KeyStrokeAction(stroke, null));
		inputMap.put(stroke, cmd);

		Log.log(8, "(PwsFileContainer.KeyStrokeHandler) read container key assignments: size=" 
				+  inputMap.size() + "  " + getDatabaseName());
	}

	/** Update a key reference in this handler with the current short-key value
	 *  of the given record, if not this value is excluded by global restrictions.
	 * 
	 * @param rec PwsRecord new record value, may be null
	 * @param oldRec PwsRecord old record value, may be null
	 */
	public void updateKey ( PwsRecord rec, PwsRecord oldRec ) {
		KeyStroke stroke = null;
		if ( rec != null ) {
			stroke = rec.getKeyboardShortcut();
			if ( stroke != null && !restrictedKeys.contains(stroke) ) {
				// feed the action map with a key-specific reference to the record
				String cmd = "RECORD_" + rec.getRecordID().toHexString();
				actionMap.put(cmd, new KeyStrokeAction(stroke, rec));
	
				// feed the input map with with key-stroke
				inputMap.put(stroke, cmd);
				Log.debug(10, "(PwsFileContainer.KeyStrokeHandler) assigned KEY to RECORD relation: " + stroke + " --> " + cmd);
			}
		}
		
		if ( oldRec != null ) {
		   // remove previous record key if divergent
		   KeyStroke oldKey = oldRec.getKeyboardShortcut();
		   if ( oldKey != null && !oldKey.equals(stroke) ) {
			   inputMap.remove(oldKey);
			   Log.debug(10, "(PwsFileContainer.KeyStrokeHandler) removed KEY to RECORD relation: " + oldKey);
			   activateKey(oldKey);
		   }
		}
	}

//	/** Informs this handler about the loss of a record. Consequently, the 
//	 * shortkey of the lost record is attempted for re-assignment to another
//	 * record which may also carry it.
//	 *  
//	 * @param rec PwsRecord record lost to the enclosing container; may be null
//	 */
//	public void recordRemoved ( PwsRecord rec ) {
//	   KeyStroke key;
//	   if ( rec != null && (key = rec.getKeyboardShortcut()) != null ) {
//		   activateKey(key);
//	   }
//	}
	
	/** Activates the key->record assignment in this handler if a record is
	 * found in the enclosing container that matches this key. This is used
	 * when a previous assignment has been discarded.
	 * 
	 * @param key KeyStroke 
	 */
	private void activateKey (KeyStroke key) {
		if ( key != null ) {
			for ( Iterator<PwsRecord> it = iterator(); it.hasNext(); ) {
				PwsRecord record = it.next();
				KeyStroke stroke = record.getKeyboardShortcut(); 
				if ( key.equals(stroke) ) {
					updateKey(record, null);
					break;
				}
			}
		}
	}

	/** Collects the active set of restricted (not permitted for usage as 
	 * shortcuts) keyboard keys from global sources. This may vary due to
	 * option settings. This is used when the map of keys is (re)generated.
	 */
	private void initRestrictedKeys () {
		restrictedKeys.clear();
		restrictedKeys.addAll(Global.forbiddenKeys);
		if ( Options.isOptionSet("restrictAccelerators") ) {
			restrictedKeys.addAll(MenuHandler.getAccelerators());
		}
	}
	
	@Override
	public void fileStateChanged (PwsFileEvent evt) {
		switch (evt.getType()) {
		case PwsFileEvent.LIST_CLEARED:
		case PwsFileEvent.LIST_UPDATED:
			readContainerKeys();
			break;

		case PwsFileEvent.RECORD_ADDED:
		case PwsFileEvent.RECORD_UPDATED:
			updateKey(evt.getRecord(), evt.getOldRecord());
			break;

		case PwsFileEvent.RECORD_REMOVED:
			updateKey(null, evt.getRecord());
			break;
		}
	}
	
//	public ActionMap getActionMap () {
//		return actionMap;
//	}
//	
//	public InputMap getInputMap () {
//		return inputMap;
//	}
}

/** Whether the current user selection encompasses more than one record.
 * 
 * @return boolean true == has multiple record selections
 */
public boolean hasMultiSelection() {
	return getSelectedRecords().length > 1;
}

}
