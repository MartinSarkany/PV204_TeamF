/*
 *  AddDialog in org.jpws.front
 *  file: AddDialog.java
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.zip.CRC32;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import org.jpws.data.Options;
import org.jpws.data.PersistentOptions;
import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ActivitySource;
import org.jpws.front.util.BlinkingLabel;
import org.jpws.front.util.ButtonBar;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.EditorChangeEvent;
import org.jpws.front.util.EditorChangeEventListener;
import org.jpws.front.util.EditorTextField;
import org.jpws.front.util.NotesTextArea;
import org.jpws.front.util.PushSemaphor;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.SpringUtilities;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.exception.InvalidPassphrasePolicy;
import org.jpws.pwslib.global.Log;

/**
 * Dialog class to handle display and edition of a singular PWS record 
 * (<code>PwsRecord</code>). The record that is edited during this dialog
 * may be either a new record or one handed over with the constructor.
 * In the latter case, the same record object is returned via <code>getRecord()</code>
 * as was handed over to the dialog.
 * <p>This class issues events related to dialog buttons pressed
 * by the user. Currently these buttons are "OK" and "Cancel" (dialog termination).
 * The issued events are <code>ActionEvent</code>s with specific type values and
 * command strings.
 * <p><u>Issued ActionEvents:</u>
 * <p>Types: DIALOG_EDIT or DIALOG_NEW (reflecting the constructor used)
 * <p>Command: "dialog.action.ok" issued when OK button is pressed; implies that
 *  a valid record object is available through the <code>getRecord()</code> method.
 * <p>Command: "dialog.action.cancel"  issued when CANCEL button is pressed;
 * during event handling the record available by <code>getRecord()</code> is
 * in the state as "last edited". After event handling the record
 * is returned to its initial state.
 *  
 * @author Wolfgang Keller
 * @author previous work of Kevin Preece
 * 
 * @since 0-5-0 subclass of ButtonBarDialog
 */
public class AddDialog //extends ButtonBarDialog implements ActivitySource, ActivityListener
{
//   public static final int YES_OPTION = JOptionPane.YES_OPTION;
//   public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
//   public static final int DIALOG_EDIT = 0;
//   public static final int DIALOG_NEW = 1;
//   
//   /** Time in milliseconds for the period of inactivity that leads to record autosave (daemon thread). */
//   private static final int AUTOSAVE_PERIOD = 30000;  
//   private static final int DAYSECS = 86400;  // seconds of a day
//   private static final int TEXTFIELDSIZE = 25;
//   private static final String HIDEDUMMY = "**********";
////   private static final Color HIDECOLOR = Color.LIGHT_GRAY;
//   private static final Color INVALID_COLOR = Color.GRAY;
//   private static final Color TIME_COLOR = new Color( 0x005FBF );
////   private static final Color ALERT_COLOR = new Color( 0xDC143C );  // crimson
////   private static final Color WARN_COLOR = new Color( 0xD2691E );  // chocolate
//   private static final Color HOTFIELD_COLOR = new Color( 0x8B0000 );  // darkred
//   private static final int SPINNERACTION_DELAYTIME = 2000; // milliseconds
//
//   private static final int LIFE_UNLIMITED = 0;
//   private static final int LIFE_PERIOD = 12 ;
//   private static final int LIFE_SPECIAL = 13;
//   private static final Color GENPASSBUTTON_BGD_COLOR = new Color( 0xff, 0xef, 0xd5); // papayawhip
//   private static final Color GENPASSBUTTON_BGD_COLOR_NEW = new Color( 0xff, 0xc0, 0xcb); // pink
//   
//   private static String badFieldText;
//   
//   private JTabbedPane      tabPane;
//   private JComboBox        group;
//   private EditorTextField	title;
//   private EditorTextField	username;
//   private EditorTextField  email;
//   private DlgPasswordField	passFld;
//   private EditorTextField  urlFld;
//   private JScrollPane      notesScrollPane;
//   private NotesTextArea    notes;
//   private ButtonBarDialog  tearoffNotesDialog;
//   private FloatingToolbar  floatToolbar;
//   private JLabel           recModLabel;
//   private JLabel           passModLabel;
//   private JLabel           accessLabel;
//   private JLabel           explainLabel;
//   private JLabel           shortKeyValueLabel;
//   private JLabel           Signature_Label;
//   private JLabel           UUID_Label;
//   private BlinkingLabel    expiryLabel;
//   private BlinkingLabel    expiryIcon;
//   private JSpinner         expIntervalSpinner;
//   private JPanel           expIntervalPanel;
////   private HtmlBrowserDialog helpDialog;
//
//   private JPanel       contentPanel;
//   private JPanel       statsPanel;
//   private JPanel       historyPanel;
//   private JPanel       notesPanel;
//   private JToolBar     toolBar;
//   private JButton      copyPassIcon;
//   private JButton      copyUserIcon;
//   private JButton      policyIcon;
//   private JButton      webstartIcon;
//   private JButton      historyIcon;
//   private JButton      clearClipIcon;
//   private JButton      favouriteIcon;
//   private JButton      undoIcon, redoIcon;
//   private JButton      floatingBarIcon;
//   private Border       tabbedPaneBorder;
//   private History      pwHistory;
//   
//   private JButton      okButton;
//   private JButton      cancelButton;
//   private JButton      helpButton;
//   private JButton      genpassButton;
//   private JButton      extraPassButton;
//   private JButton      revertButton;
//
//   private JButton      coverButton;
//   private JButton      uncoverButton;
//   private JPanel       coverPanel;
//   private CardLayout   coverCard;
//   
//   private JButton      timeButton;
//   private JButton      urlButton;
//   private JButton      mailButton;
//   private JButton      tearOffButton;
//   private JComboBox    expireCombo;
//   private Icon         icon1;
//   private Icon         icon2;
//   private Icon         favIcon1;
//   private Icon         favIcon2;
//   private Component    recentFocusOwner;
//   private Font         dataFont;
//   private Font         dataFontBold;
//   
//   private PwsFileContainer container;
//   private ObjectChangeListener objectListener = new ObjectChangeListener(this);
//   private final Actions   actions = new Actions();
//   private ActionListener externalActions;
//   private EditUndoManager undoManager;
//   private PushSemaphor actionSemaphor = new PushSemaphor();
//   private AutoSaveRunner autoSaveRunner;
//   private PwsRecord    record;
//   private PwsRecord    initRec, autoSaveRec;
//   private PwsPassphrasePolicy policy;
//   private String       initGroup, actGroup;
//   private boolean      editing;
//   private boolean      passwordHidden;
//   private boolean      isFavourite;
//   private boolean      isDebug;
//   private int          userOption;
//   private int          crc, dataCrc, autoSaveCrc;
//   private int          timelabel_width;
//
//	/** Constructor for a dialog to create a new record.
//    *  (This will create a new record with already an UUID installed.)
//    * 
//	 * @param owner the parent Frame (not <b>null</b>!)
//    * @param group an initial value for the record field GROUP
//	 * @throws java.awt.HeadlessException
//	 */
//	public AddDialog( Frame owner, String group ) throws HeadlessException
//	{
//	   super( owner, DialogButtonBar.BUTTONLESS, true );
//	   setTitle( ResourceLoader.getDisplay("adddlg.title") );
//
//	   record = new PwsRecord();
//       passwordHidden = false;
//       initGroup = group;
//
//       init( owner );
//	}  // constructor
//
//   /** Constructor for a dialog to edit a given record.
//    * 
//    * @param owner the parent Frame (not <b>null</b>!)
//    * @param rec the <code>PwsRecord</code> to be edited
//    * @throws java.awt.HeadlessException
//    */
//   public AddDialog( Frame owner, PwsRecord rec ) throws HeadlessException
//   {
//      super( owner, DialogButtonBar.BUTTONLESS, true );
//      setTitle( ResourceLoader.getDisplay("edidlg.title") );
//
//      record = rec;
//      initGroup = rec.getGroup();
//      editing = true;
//      passwordHidden = !(Options.isOptionSet("openPassEdit") || record.getPassword() == null);
//
//      init( owner );
//   }  // constructor
//
//   private void init ( Component owner )
//   {
//      isDebug = Options.isOptionSet( "monitorSystem" );
//      
//      container = Global.getSelectedFile();
//      undoManager = new EditUndoManager();
//
//      // record and validation preparations
//      crc = record.getCRC();
//      initRec = (PwsRecord)record.clone();
//      isFavourite = container.isRecordFavourite( initRec );
//      if ( (policy = record.getPassPolicy()) == null ) {
//         policy = container.getPassphrasePolicy();
//      }
//      
//
//      // design preparations
//      dataFont = DisplayManager.getFont( "data" );
//      dataFontBold = dataFont.deriveFont( Font.BOLD +
//            (dataFont.isItalic() ? Font.ITALIC : 0) );
//      tabbedPaneBorder = BorderFactory.createEmptyBorder( 18, 18, 15, 16 );
//      contentPanel = new JPanel( new BorderLayout() );
//
//      // expand dialog title with database name for MULTIFILE desktop
//      if ( DisplayManager.getDisplayState() == DisplayManager.DISPLAY_DESKTOP )
//         setTitle( getTitle().concat( " (" + container.getDatabaseName() + ")" ));
//      
//      buildTopPanel();
//      buildCentrePanel();
//      buildRightPanel();
//      
//      updateMenu();
//      setupDisplay( initGroup );
//      dataCrc = dataCRC();
//      if ( Options.isOptionSet( "useDataMirrors" ) ) {
//         autoSaveCrc = dataCrc;
//         autoSaveRunner = new AutoSaveRunner();
//         autoSaveRunner.start();
//      }
//
//      undoManager.clear();
//      title.clearUndoList();
//      username.clearUndoList();
//      email.clearUndoList();
//      notes.clearUndoList();
//      urlFld.clearUndoList();
//      passFld.clearUndoList();
//
//      setAutonomous( true );
//      setClipping( false );
//      setDialogPanel( contentPanel );
//      
//      // optionally restore window bounds from file option memory
//      if ( Options.isOptionSet( "rememberScreen" ) )
//         gainLocation( Options.getOptions(), "entry_editor", true );
//      Log.log(8, "(AddDialog.init) -- init AddDialog performed");
//   }  // init
//   
//   /** Registers an external <code>ActionListener</code> for the button bar of 
//    *  this dialog. (See class description!)
//    * 
//    *  @param actions <code>ActionListener</code> object
//    */ 
//   public void setExternalActions ( ActionListener actions )
//   {
//      externalActions = actions;
//   }
//
//   @Override
//public void addActivityListener ( ActivityListener listener )
//   {
//      objectListener.addActivityListener( listener );
//   }
//
//   @Override
//public void removeActivityListener ( ActivityListener listener )
//   {
//      objectListener.removeActivityListener( listener );
//   }
//
//   private void autorun ()
//   {
//      // autorun actions
//      if ( getFloatingToolbarOptions().isOptionSet( "editor-floatingbar-active" ) )
//         actions.callAction( "button.editfloatbar" );
//   }
//   
//   /** Disposes this dialog and makes it unusable (destruction). */
//   @Override
//   public void dispose ()
//   {
//      // terminate auto-save thread and delete save-record entry
//      if ( autoSaveRunner != null )
//         autoSaveRunner.terminate();
//      if ( autoSaveRec != null ) {
//         container.deleteRecord( autoSaveRec );
//         autoSaveRec = null;
//      }
//
//      // optionally remember window bounds
//      if ( isShowing() && Options.isOptionSet( "rememberScreen" ) )
//      {
//         storeBounds( Options.getOptions(), "entry_editor", true );
//         if ( floatToolbar != null )
//            floatToolbar.storeBounds( getFloatingToolbarOptions(), "dialogFloatingBarBounds", true );
//      }
//
//      // tear down GUI
//      super.dispose();
//      Global.removeTimePulseListener( expiryLabel );
//      Global.removeTimePulseListener( expiryIcon );
//      getContentPane().removeAll();
////      System.out.println( "*** Dialog Window Disposed ***" );
//   }
//   
//   /** Returns the option bag for the floating toolbar's concern. */
//   private PersistentOptions getFloatingToolbarOptions () {
//      return container.getMinorOptions();
//   }
//   
//   @Override
//   public void actionOccurred ( ChangeEvent evt )
//   {
//      objectListener.actionOccurred( evt );
//   }
//
//   /**
//    * 
//    * @since 0-5-0
//    */
//   private void undoUpdated()
//   {
//      String hstr;
//      boolean hasUndo, hasRedo;
//      
//      hasUndo = undoManager.canUndo();
//      hasRedo = undoManager.canRedo();
//      
//      // UNDO item settings
//      if ( hasUndo ) {
//         hstr = //ResourceLoader.getCommand( "menu.edit.undo" ) + " : " +
//                undoManager.getUndoPresentationName();
//         undoIcon.setToolTipText( hstr );
//         undoIcon.setVisible( true );
//      }
//      undoIcon.setEnabled( hasUndo );
//      
//      // REDO item settings
//      if ( hasRedo ) {
//         hstr = //ResourceLoader.getCommand( "menu.edit.redo" ) + " : " +
//                undoManager.getRedoPresentationName();
//         redoIcon.setToolTipText( hstr );
//         redoIcon.setVisible( true );
//      }
//      redoIcon.setEnabled( hasRedo );
//   }  // undoUpdated
//
//   private void updateMenu ()
//   {
//      undoUpdated();
//   }
//   
//   /** Class internal. */
//   @Override
//   public void processWindowEvent( WindowEvent e )
//   {
////      System.out.println( "-- Dialog Window Event: " + e.getID() );
//      switch ( e.getID() )
//      {
//      case WindowEvent.WINDOW_CLOSED :
////         System.out.println( "*** Dialog Window Closed ***" );
//         break;
//      case WindowEvent.WINDOW_CLOSING :
////         System.out.println( "*** Dialog Window Closing ***" );
//         actions.actionPerformed( new ActionEvent( this, 0, "button.cancel" ));
//         return;
//      case WindowEvent.WINDOW_OPENED :
////       System.out.println( "*** Dialog Window Closed ***" );
//         autorun();
//       break;
//      }
//      
//      super.processWindowEvent( e );
//   }  // processWindowEvent
//   
//   /** Assigns content to all variable fields of the display. 
//    * 
//    * @param grpval String the initial setting for the field GROUP ind display
//    */
//   private void setupDisplay ( String grpval )
//   {
//      Log.log( 8, "(AddDialog.setupDisplay) enter" );
//      actionSemaphor.push();
//      
//      synchronized ( record )
//      {
//         if ( grpval == null )
//            grpval = record.getGroup();
//         
//         if ( (actGroup = grpval) == null )
//            actGroup = "";
//         
//         group.setSelectedItem( grpval );
//         title.setText( record.getTitle() );
//         email.setText( record.getEmail() );
//         username.setText( record.getUsername() );
//         notes.setText( record.getNotes() );
//         urlFld.setText( record.getUrl() );
//         pwHistory.setContentPw3( record.getHistory() );
//         expIntervalSpinner.setValue( new Integer( record.getExpiryInterval()) );
//         passFld.setPassphrase( record.getPassword() );
//         passFld.setHidden( passwordHidden );
//   
//         // Favourite-icon
//         favouriteIcon.setIcon( isFavourite ? favIcon2 : favIcon1 );
//         favouriteIcon.setRolloverIcon( isFavourite ? favIcon2 : favIcon1 );
//         favouriteIcon.setToolTipText( ResourceLoader.getCommand( isFavourite ? 
//               "toolbar.isfavourite.tooltip" : "toolbar.isnofavourite.tooltip" ));
//         
//         if ( (policy = record.getPassPolicy()) == null ) {
//            policy = container.getPassphrasePolicy();
//         }
//         policyIcon.setVisible( record.getPassPolicy() != null );
//         KeyStroke shortKey = record.getKeyboardShortcut();
//         String text = shortKey == null ? null : keyStrokeText(shortKey);
//         shortKeyValueLabel.setText(text);
//         
//   //      setPassLifeTime( record.getPassLifeTime(), false );
//         refreshTimeFields();
//         refreshExpireFields();
//      }
//      
//      actionSemaphor.pop();
//      Log.log( 8, "(AddDialog.setupDisplay) exit" );
//}  // setupDisplay
//   
//   
//   /**  @since 0-4-0 */
//   private void controlHistoryPanel ()
//   {
//      boolean yes;
//      
//      // irrelevant for older file formats
//      if ( container.getFileFormat() <= 2 | historyPanel == null )
//         return;
//
//      // condition for history panel to be up
//      yes =  pwHistory.getListSize() > 0 & pwHistory.getMaxEntries() > 0  & pwHistory.isActive();
//      
//      // install if opted and not yet installed
//      if ( yes & historyPanel.getClientProperty( "installed" ) == null )
//      {
//         tabPane.add( historyPanel );
//         historyPanel.putClientProperty( "installed", "true" );
//         historyIcon.setVisible( false );
//      }
//      
//      // de-install if opted
//      else if ( !yes )
//      {
//         tabPane.remove( historyPanel );
//         historyPanel.putClientProperty( "installed", null );
//         historyIcon.setVisible( pwHistory.getMaxEntries() == 0 | !pwHistory.isActive() );
//      }
//   }  // controlHistoryPanel
//   
//   private void buildCentrePanel()
//   {
//      tabPane = new JTabbedPane();
//      objectListener.registerChangeableObject( tabPane );
//      tabPane.addChangeListener( actions );
//      contentPanel.add( tabPane, BorderLayout.CENTER );
//      
//      tabPane.add( buildLoginPanel() );
//
//      if ( Options.isOptionSet( "editFullNotes" ) )
//      {
//         tabPane.add( buildNotesPanel() );
//      }
//
//      statsPanel = buildStatsPanel();
//      if ( container.getFileFormat() > 1 )
//      {
//         tabPane.add( statsPanel );
//      }
//      
//      historyPanel = buildHistoryPanel();
//   }
//   
//   private String keyStrokeText ( KeyStroke stroke ) {
//	   if ( stroke == null ) return "null";
//	   String hstr = stroke.toString();
//	   hstr = Util.substituteText(hstr, "pressed ", "");
//	   return hstr.toUpperCase();
//   }
//   
//	private JPanel buildLoginPanel()
//	{
//		JPanel		pane, pane2, panel;
//		JLabel		label;
//        int rows;
//
//        pane = new JPanel( new SpringLayout() );
//        label = new JLabel( ResourceLoader.getDisplay("adddlg.label.group") );
//        rows = 3;
//
//        group = GUIService.getGroupListCombo( container, true, 450 );
//        objectListener.registerChangeableObject( group );
//        group.addItemListener( actions );
//        if ( container.getFileFormat() > 1 )
//        {
//           pane.add( label );
//           pane.add( group );
//           rows++;
//        }
//
//        label = new JLabel( ResourceLoader.getDisplay("adddlg.label.title") );
//        title = new EditorTextField( TEXTFIELDSIZE );
//        title.setFont( dataFont );
//        title.addFocusListener( actions );
//        objectListener.registerChangeableObject( title );
//		pane.add( label );
//		pane.add( title );
//
//		label		= new JLabel( ResourceLoader.getDisplay("adddlg.label.username") );
//		label.setBorder( new EmptyBorder( 0, 0, 0, 10 ) );
//		username	= new EditorTextField( TEXTFIELDSIZE );
//        username.setFont( dataFont );
//        username.addFocusListener( actions );
//        objectListener.registerChangeableObject( username );
//		pane.add( label );
//		pane.add( username );
//
//		label	= new JLabel( ResourceLoader.getDisplay("adddlg.label.password") );
//		  passFld	= new DlgPasswordField();
//        passFld.setFont( DisplayManager.getFont( "password" ) );
//        passFld.addFocusListener( actions );
//        objectListener.registerChangeableObject( passFld );
//        pane.add( label );
//        pane.add( passFld );
//
//		  // EMAIL field
//        panel = new JPanel( new BorderLayout() );
//        label = new JLabel( ResourceLoader.getDisplay("adddlg.label.email") );
//        panel.add( label, BorderLayout.WEST );
//        
//        mailButton = new JButton( ResourceLoader.getImageIcon( "copy16-icon" ) );
//        mailButton.setActionCommand( "button.copyemail" );
//        mailButton.addActionListener( actions );
//        mailButton.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 3 ) );
//        mailButton.setBackground( panel.getBackground() );
//        mailButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.copyemail" ));
//        mailButton.setFocusPainted( false );
//        panel.add( mailButton, BorderLayout.EAST );
//
//        email  = new EditorTextField( TEXTFIELDSIZE );
//        email.setFont( dataFont );
//        email.addFocusListener( actions );
//        objectListener.registerChangeableObject( email );
//        if ( container.getFileFormat() > 2 )
//        {
//           pane.add( panel );
//           pane.add( email );
//           rows++;
//        }
//        
//        // URL field
//        panel = new JPanel( new BorderLayout() );
//        label  = new JLabel( ResourceLoader.getDisplay("adddlg.label.urlfield") );
//        panel.add( label, BorderLayout.WEST );
//        
//        urlButton = new JButton( ResourceLoader.getImageIcon( "webicon" ) );
//        urlButton.setActionCommand( "button.starturl" );
//        urlButton.addActionListener( actions );
//        urlButton.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 3 ) );
//        urlButton.setBackground( panel.getBackground() );
//        urlButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.starturl" ));
//        urlButton.setFocusPainted( false );
//        panel.add( urlButton, BorderLayout.EAST );
//
//        urlFld  = new EditorTextField( TEXTFIELDSIZE );
//        urlFld.setFont( dataFont );
//        urlFld.addFocusListener( actions );
//        urlFld.addChangeEventListener( new EditorChangeEventListener() {
//
//        @Override
//		public void documentChanged ( EditorChangeEvent event ) {
////              System.out.println( "--- Editor Feld Change Event: " + event.getType() );
//              boolean enable = Util.extractURL(getUrl()) != null;
//              webstartIcon.setEnabled( enable );
//              if ( floatToolbar != null ) {
//                 floatToolbar.setBrowsingEnabled( enable );
//              }
//           }
//        });
//        objectListener.registerChangeableObject( urlFld );
//        if ( container.getFileFormat() > 2 )
//        {
//           pane.add( panel );
//           pane.add( urlFld );
//           rows++;
//        }
//        
//        // NOTES field
//        notes = new NotesTextArea( ResourceLoader.getDisplay( "pane.edit.notes" ) )
//        {
//           @Override
//		protected JPopupMenu getPopupMenu ()
//           {
//              JPopupMenu menu;
//              JMenuItem item;
//               
//              menu = super.getPopupMenu();
//              if ( Options.isOptionSet( "editFullNotes" ) & tearoffNotesDialog == null )
//              {
//                 item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.tearoff" ) );
//                 item.setActionCommand( "button.tearoff.notes" );
//                 item.addActionListener( AddDialog.this.actions );
//                 menu.insert( item, menu.getComponentCount() - 2 );
//              }
//              return menu;
//           }
//        };
//        notes.setFont( DisplayManager.getFont( "notes" ));
//        notes.addFocusListener( actions );
//        notes.setDialogOwner( this );
//        objectListener.registerChangeableObject( notes );
//        
//        notesScrollPane  = new JScrollPane( notes );
//        tearOffButton = new JButton( ResourceLoader.getImageIcon( "edittearoff" ) );
//        notesPanel = new JPanel( new BorderLayout() );
//        notesPanel.setPreferredSize( new Dimension( 280, 128 ) );
//        notesPanel.add( notesScrollPane );
//
//        if ( !Options.isOptionSet("editFullNotes") )
//        {
//            panel = new JPanel( new BorderLayout() );
//            label       = new JLabel( ResourceLoader.getDisplay("adddlg.label.notes") );
//            panel.add( label, BorderLayout.WEST );
//            
//            tearOffButton.setActionCommand( "button.tearoff.notes" );
//            tearOffButton.addActionListener( actions );
//            tearOffButton.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 3 ) );
//            tearOffButton.setBackground( panel.getBackground() );
//            tearOffButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.tearoffnotes" ));
//            tearOffButton.setFocusPainted( false );
////            tearOffButton.setPreferredSize( new Dimension( 20, 20 ) );
////            tearOffButton.setPreferredSize( tearOffButton.getPreferredSize() );
//
//            pane2 = new JPanel( new FlowLayout( FlowLayout.RIGHT, 0, 0 ) );
//            pane2.add( tearOffButton );
//            panel.add( pane2, BorderLayout.EAST );
//            pane.add( panel );
//            
//      		pane.add( notesPanel );
//      		rows++;
//	    }
//
//        // format pane depending on rows 
//        SpringUtilities.makeCompactGrid( pane, rows, 2, 0, 0, 6, 6 );
//
//        // set pane into Northern Borderlayout location (avoids resizing of components) 
//        pane2 = new JPanel( new BorderLayout() );
//        pane2.setBorder( tabbedPaneBorder );
//        pane2.setName( ResourceLoader.getDisplay( "pane.edit.front" ) );
//        pane2.putClientProperty( "explain", ResourceLoader.getDisplay( "adddlg.explain.login" ));
//        pane2.add( pane, BorderLayout.NORTH );
//		return pane2;
//   }
//
//   private void setTimeLabel ( JLabel label, long time )
//   {
//      label.setText( time == 0 ? "?" : Global.getLocalDateTime(time) ); 
//   }
//   
//   private JPanel makeTimeValuePanel ( JLabel label, String token, 
//         int width, long time, Color color )
//   {
//      JPanel panel;
//      JLabel label1;
//      Dimension dim;
//      
//      panel  = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
//      
//      label1 = new JLabel( ResourceLoader.getDisplay( token ) );
//      if ( color != null )
//         label1.setForeground( color );
//      dim = label1.getSize();
//      dim.width = width; 
//      label1.setPreferredSize( new Dimension( width, 16 ) );
//      panel.add( label1 );
//
//      setTimeLabel( label, time );
//      label.setForeground( color == null ? TIME_COLOR : color );
//      label.setFont( dataFont.deriveFont( Font.BOLD ) );
//      panel.add( label );
//      
//      return panel;
//   }
//
//   /** Sets the password lifetime combo and the interval spinner after 
//    * current values of the record. (Does not cause a selection changed action.)
//    */
//   private void setPassLifeControls ()
//   {
//      int  life, index, exp;
//
//      Log.log( 8, "(AddDialog.setPassLifeCombo) enter" );
//      actionSemaphor.push();
//      
//      // "life" is seconds distance between pass-modified and pass-death time
//      life = (int)((record.getPassLifeTime() - record.getPassModTime()) / 1000);
//      // "expiry interval" is days of the period of password validity
//      exp = record.getExpiryInterval();
//      index = -1;
//      
//      // 1. rank: no pass-lifetime set == UNLIMITED
//      if ( record.getPassLifeTime() == 0 )
//         index = LIFE_UNLIMITED;
//
//      // 2. rank: an expiry interval is set (EDITABLE-PERIOD or SELECTABLE-PERIOD)
//      else if ( exp==30 | exp==60 | exp==91 | exp==182 | exp==273 | exp==365 | exp==730 
//                | exp==1095 | exp==1460 | exp==1825 | exp==3650 )
//         life = exp * DAYSECS;
//      else if ( exp > 0 )
//         index = LIFE_PERIOD;
//      
//      // 3. rank: something very special is set (likely irregular data)
//      else if ( life < 0 | life > Integer.MAX_VALUE )
//         index = LIFE_SPECIAL;
//      
//      // if no priority, try setup any selectable period
//      // if not possible, render "SPECIAL" selected
//      if ( index == -1 ) 
//         switch ( life )
//         {
//            case 30 * DAYSECS :  index = 1;
//            break;
//            case 60 * DAYSECS :  index = 2;
//            break;
//            case 91 * DAYSECS :  index = 3;
//            break;
//            case 182 * DAYSECS :  index = 4;
//            break;
//            case 273 * DAYSECS :  index = 5;
//            break;
//            case 365 * DAYSECS :  index = 6;
//            break;
//            case 2 * 365 * DAYSECS :  index = 7;
//            break;
//            case 3 * 365 * DAYSECS :  index = 8;
//            break;
//            case 4 * 365 * DAYSECS :  index = 9;
//            break;
//            case 5 * 365 * DAYSECS :  index = 10;
//            break;
//            case 10 * 365 * DAYSECS :  index = 11;
//            break;
//            default: index = LIFE_SPECIAL;
//         }
//      
//      // set the combo
//      expireCombo.setSelectedIndex( index );
//      
//      // we also set up the expiry interval spinner
//      expIntervalPanel.setVisible( index == LIFE_PERIOD );
//      expIntervalSpinner.setValue( new Integer( exp ) );
//
//      actionSemaphor.pop();
//      Log.log( 8, "(AddDialog.setPassLifeCombo) exit" );
//   }  // setPassLifeCombo
//   
//   /** Calculates the PASSLIFETIME value after the parameter combobox index
//    *  for all index cases except PERIOD and SPECIAL.
//    *  Sets the value of the record.
//    * 
//    *  @param index passlife combo index
//    *  @param undoable boolean whether undoable edit is created 
//    *  @since 0-5-0 modified parameter list
//    */ 
//   private void setPassLifeTimeAfterIndex ( int index, boolean undoable )
//   {
//      long h, v = 0;
//      int days = 0;
//      
//      // only regard the "selectable period" indices
//      if ( index < 0 | index > 11 )
//         return;
//      
//      Log.log( 8, "(AddDialog.setPassLifeTimeAfterIndex) enter with index=".concat( 
//            String.valueOf( index )) );
//
//      if ( index > 0 )
//      {
//         switch ( index )
//         {
//            case 1 : days = 30;
//            break;
//            case 2 : days = 60;
//            break;
//            case 3 : days = 91;
//            break;
//            case 4 : days = 182;
//            break;
//            case 5 : days = 273;
//            break;
//            case 6 : days = 365;
//            break;
//            case 7 : days = 2 * 365;
//            break;
//            case 8 : days = 3 * 365;
//            break;
//            case 9 : days = 4 * 365;
//            break;
//            case 10 : days = 5 * 365;
//            break;
//            case 11 : days = 10 * 365;
//            break;
//         }
//
//         // the new value for record PASSLIFETIME
//         correctRecord();
//         h = record.getPassModTime();
//         v = h + (long)days * DAYSECS * 1000;
//      }
//
//      // set record value, label text and combo display
//      setPassLifeTime( v, days, undoable );
//      Log.log( 8, "(AddDialog.setPassLifeTimeAfterIndex) exit with value set = " + v ); 
//   }  // setPassLifeTimeAfterIndex
//
//   
//   /** Calculates the PASSLIFETIME value after the parameter number of
//    *  days of lifetime interval. Sets the value of the record.
//    *  
//    *  @param days int lifetime interval in days
//    *  @param undoable boolean whether undoable edit is created 
//    */  
//   private void setPassLifeTimeAfterInterval ( int days, boolean undoable )
//   {
//      correctRecord();
//      long value = record.getPassModTime() + (long)days * DAYSECS * 1000;
//      setPassLifeTime( value, days, undoable );
//   }
//   
//   /** Sets default values for time fields if they are currently left void. */
//   private void correctRecord ()
//   {
//      long defaultTime = Math.max(record.getCreateTime(), record.getPassModTime());
//      if ( defaultTime == 0 ) {
//         defaultTime = record.getModifiedTime();
//      }
//      
//      if ( record.getModifiedTime() == 0 ) {
//         record.setModifyTime( defaultTime );
//         setTimeLabel( recModLabel, record.getModifiedTime() );
//      }
//      if ( record.getPassModTime() == 0 ) {
//         record.setPassModTime( defaultTime );
//         setTimeLabel( passModLabel, record.getPassModTime() );
//      }
//      if ( record.getAccessTime() == 0 ) {
//         record.setAccessTime( defaultTime );
//         setTimeLabel( accessLabel, record.getAccessTime() );
//      }
//   }
//   
//   /** Sets PASSLIFETIME record value, label text and combo display.
//    *  Arranges dialog display elements according to new expiry status.
//    *  
//    *  @param value long the new time value in epoch milliseconds
//    *  @param interval int new expire interval to be set alongside with 
//    *                  expire time; if value<0, no interval will be set 
//    *  @param undoable boolean if <b>true</b> a change in value will be 
//    *         undoable in UI 
//    *  @since 0-5-0 modified parameter list
//    */
//   private void setPassLifeTime ( long value, int interval, boolean undoable )
//   {
//      UndoableEdit edit;
//      long oldPassLifeTime;
//      int oldInterval;
//      
//      Log.log( 8, "(AddDialog.setPassLifeTime) enter" );
//      
//      oldInterval = record.getExpiryInterval();
//      oldPassLifeTime = record.getPassLifeTime();
//      
//      // set values in objects
//      if ( interval >= 0 )
//         record.setExpiryInterval( interval );
//      
//      if ( value == record.getPassModTime() )
//         value = 0;
//      record.setPassLifeTime( value );
//      value = record.getPassLifeTime();
//      refreshExpireFields();
//
//      // create undoable edit event
//      if ( undoable && oldPassLifeTime != value ) {
//         Log.debug( 7, "(AddDialog.setPassLifeTime) creating UNDOABLE EDIT for Expire Time" );
//         edit = undoManager.new FieldEdit( oldPassLifeTime, oldInterval );
//         undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
//      }
//      Log.log( 8, "(AddDialog.setPassLifeTime) exit" );
//   }
//   
//   private JPanel buildStatsPanel()
//   {
//      JPanel      pane, panel;
//      JLabel      label;
//      JButton 	button;
//      SpinnerNumberModel spinnerModel; 
//      String      hstr;
//      Color       color;
//      Font        font;
//      
//      timelabel_width = 100;
//      
//      pane  = new JPanel( new VerticalFlowLayout( 4 ) );
//      pane.setName( ResourceLoader.getDisplay( "pane.edit.stats" ) );
//      pane.setBorder( tabbedPaneBorder );
//
//
//      label = new JLabel( ResourceLoader.getDisplay( "adddlg.label.record" ) + " " );
//      label.setFont( label.getFont().deriveFont( Font.ITALIC ) );
//      pane.add( label );
//      
//      label = new JLabel();
//      pane.add( makeTimeValuePanel( label, "adddlg.time.create", timelabel_width,
//                record.getCreateTime(), null ));
//      
//      recModLabel = new JLabel();
//      pane.add( makeTimeValuePanel( recModLabel, "adddlg.time.modify", 
//            timelabel_width, record.getModifiedTime(), null ));
//
//      pane.add( Box.createVerticalStrut( 10 ) );
//      
//      label = new JLabel( ResourceLoader.getDisplay( "adddlg.label.password" ) + " " );
//      label.setFont( label.getFont().deriveFont( Font.ITALIC ) );
//      pane.add( label );
//      
//      accessLabel = new JLabel();
//      color = Options.isOptionSet( "storeMinorChanges" ) || container.isModified() 
//    		  ? null : INVALID_COLOR;
//      pane.add( makeTimeValuePanel( accessLabel, "adddlg.time.access", 
//            timelabel_width, record.getAccessTime(), color ));
//
//      passModLabel = new JLabel();
//      pane.add( makeTimeValuePanel( passModLabel, "adddlg.time.modify", 
//            timelabel_width, record.getPassModTime(), null ));
//
//      expiryLabel = new BlinkingLabel();
//      expiryLabel.setForeground( HOTFIELD_COLOR );
//      Global.addTimePulseListener( expiryLabel );
//      pane.add( makeTimeValuePanel( expiryLabel, "adddlg.time.expire", 
//            timelabel_width, 0, HOTFIELD_COLOR ));
//      
//      panel = new JPanel( );
//      pane.add( panel );
//      panel.add( Box.createHorizontalStrut( timelabel_width - 10 ) );
//      
//      hstr = ResourceLoader.getCommand( "combo.passlifetime" );
//      expireCombo = new JComboBox( hstr.split( "," ) );
//      expireCombo.addActionListener( actions );
//      expireCombo.setToolTipText( ResourceLoader.getCommand( "tooltip.combo.expiry" ));
//      expireCombo.setFont( dataFontBold );
//      panel.add( expireCombo );
//      
//      timeButton = new JButton( ResourceLoader.getImageIcon( "button.enter.16" ) );
//      timeButton.setActionCommand( "button.expirydate" );
//      timeButton.addActionListener( actions );
//      timeButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
//      timeButton.setBackground( panel.getBackground() );
//      timeButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.expiry" ));
//      timeButton.setFocusPainted( false );
//      panel.add( timeButton );
//      
//      panel = new JPanel( );
//      pane.add( panel );
//      label = new JLabel( ResourceLoader.getDisplay( "units.days" ) );
//      label.setPreferredSize( new Dimension( timelabel_width - 10, 16 ) );
//      panel.add( label );
////      panel.add( Box.createHorizontalStrut( timelabel_width - 10 ) );
//      spinnerModel = new SpinnerNumberModel( 0, 0, 3650, 1 );
//      expIntervalSpinner = new JSpinner( spinnerModel );
//      expIntervalSpinner.addChangeListener( actions );
//      objectListener.registerChangeableObject( expIntervalSpinner );
//      panel.add( expIntervalSpinner );
//      expIntervalPanel = panel;
//      expIntervalPanel.setVisible( false );
//      
//      panel = new JPanel( );
//      pane.add( panel );
//      button = new JButton(ResourceLoader.getImageIcon("button.shortcutkey"));
//      button.setActionCommand("button.shortcutkey");
//      button.addActionListener(actions);
//      button.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 5 ) );
//      button.setBorderPainted(false);
//      button.setBackground( panel.getBackground() );
//      button.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.shortcutkey" ));
//      button.setFocusPainted( false );
//      panel.add(button);
//      shortKeyValueLabel = new JLabel();
//      font = shortKeyValueLabel.getFont().deriveFont( Font.PLAIN, (float)14.0 );
//      shortKeyValueLabel.setFont(font);
//      shortKeyValueLabel.setForeground(Color.BLACK);
//      panel.add(shortKeyValueLabel);
//      
//      // Debug modus values
//      if ( isDebug ) {
//         pane.add( Box.createVerticalStrut( 10 ) );
//         UUID_Label = new JLabel( "UUID = ".concat( record.getRecordID().toString() ) );
//         font = UUID_Label.getFont().deriveFont( Font.PLAIN );
//         UUID_Label.setFont( font );
//         pane.add( UUID_Label );
//         Signature_Label = new JLabel( "Signature = ".concat( Util.bytesToHex( record.getSignature() ).substring(0,16) ));
//         Signature_Label.setFont( font );
//         pane.add( Signature_Label );
//      }
//      
//      pane.putClientProperty( "explain", ResourceLoader.getDisplay( "adddlg.explain.stats" ));
//      return pane;
//   }  // buildStatsPanel 
//
//   /**  @since 0-5-0 */
//   private JPanel buildNotesPanel()
//   {
//      JPanel panel;
//
//      panel  = new JPanel( new BorderLayout() );
//      panel.setName( ResourceLoader.getDisplay( "pane.edit.notes" ) );
//      panel.setBorder( tabbedPaneBorder );
//      panel.putClientProperty( "explain", ResourceLoader.getDisplay( "adddlg.explain.login" ) );
//
//      panel.add( notesPanel );
//      panel.setPreferredSize( new Dimension( 10, 270 ) );
//      
//      return panel;
//   }
//   
//   /**  @since 0-4-0 */
//   private JPanel buildHistoryPanel ()
//   {
//      JPanel pane, panel;
//      JButton clearButton, settingButton, historyOffButton;
//      
//      pane = new JPanel( new BorderLayout() );
//      pane.setBorder( tabbedPaneBorder );
//      pane.setName( ResourceLoader.getDisplay( "pane.edit.history" ) );
//
//      // top part is history text area
//      pwHistory = new History();
//      pane.add( pwHistory, BorderLayout.CENTER );
//      
//      // bottom part is service area
//      panel = new JPanel( );
//      pane.add( panel, BorderLayout.SOUTH );
//      
//      // clear history button
//      clearButton = new JButton( ResourceLoader.getDisplay( "button.clear" ) );
//      clearButton.setActionCommand( "button.clearhistory" );
//      clearButton.addActionListener( actions );
//      clearButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
//      clearButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.clearhistory" ));
//      panel.add( clearButton );
//      
//      // settings button (max entries)
//      settingButton = new JButton( ResourceLoader.getDisplay( "adddlg.button.setmax" ) );
//      settingButton.setActionCommand( "button.historysettings" );
//      settingButton.addActionListener( actions );
//      settingButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
//      settingButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.historysettings" ));
//      panel.add( settingButton );
//
//      // history off button
//      historyOffButton = new JButton( ResourceLoader.getDisplay( "button.off" ) );
//      historyOffButton.setActionCommand( "button.historyoff" );
//      historyOffButton.addActionListener( actions );
//      historyOffButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
//      historyOffButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.historyoff" ));
//      panel.add( historyOffButton );
//
//      historyPanel = pane;
//      refreshHistoryComment();
//      return pane;
//   }
///*   
//   private JPanel buildUrlPanel()
//   {
//      JPanel panel;
//      
//      panel  = linkTable;
//      panel.setName( ResourceLoader.getDisplay( "pane.edit.urls" ) );
//      panel.setBorder( tabbedPaneBorder );
//      panel.putClientProperty( "explain", ResourceLoader.getDisplay( "adddlg.explain.urlpanel" ));
//
//      return panel;
//   }
//*/   
//	private void buildRightPanel()
//	{
//	   ButtonBar bar;
//	   JPanel    rightPanel; 
//
//	   rightPanel = new JPanel( new BorderLayout() );
///*
//       JPanel toolPanel;
//       FlowLayout flowLay1;
//
//	   // create the floating toolbar panel
//	   flowLay1 = new FlowLayout();
//	   toolPanel = new JPanel( flowLay1 );
//	   toolPanel.setBorder( BorderFactory.createEmptyBorder( 12, 0, 0, 0 ) );
//	   rightPanel.add( toolPanel, BorderLayout.CENTER );
//	   floatingBarIcon = new JButton( ResourceLoader.getImageIcon( "editor-floatbarswitch" ) );
//	   floatingBarIcon.setActionCommand( "button.editfloatbar" );
//	   floatingBarIcon.addActionListener( actions );
//	   floatingBarIcon.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 3 ) );
//	   floatingBarIcon.setBackground( toolPanel.getBackground() );
//	   floatingBarIcon.setToolTipText( ResourceLoader.getCommand( "tooltip.button.editfloatbar" ));
//	   floatingBarIcon.setFocusPainted( false );
//	   toolPanel.add( floatingBarIcon );
//*/
//
//	   // create the action button bar
//	   bar = new ButtonBar( ButtonBar.VERTICAL, ButtonBar.TOP,
//             new Insets( 18, 10, 0, 0 ) );
//       rightPanel.add( bar, BorderLayout.NORTH );
//	   
//	   okButton = makeButton( "button.ok" );
//	   bar.add( okButton );
//
//      cancelButton = makeButton( "button.cancel" );
//      bar.add( cancelButton );
//
//      helpButton = makeButton( "button.help" );
//      bar.add( helpButton );
//
//      revertButton = makeButton( "button.revert" );
//      bar.add( revertButton );
//
//      // the "cover-button" panel
//      coverCard = new CardLayout();
//      coverPanel = new JPanel( coverCard );
//      coverButton = makeButton( "button.cover" );
//      coverPanel.add( coverButton, "button.cover" );
//      uncoverButton = makeButton( "button.uncover" );
//      coverPanel.add( uncoverButton, "button.uncover" );
//      bar.add( coverPanel );
//      
//      setCoverButton();
//
//      genpassButton = makeButton( "button.randpassword" );
//      genpassButton.setBackground( isNewRecord() ? 
//            GENPASSBUTTON_BGD_COLOR_NEW : GENPASSBUTTON_BGD_COLOR );
//      bar.add( genpassButton );
//      
//      extraPassButton = makeButton( "button.extrapassword" );
//      bar.add( extraPassButton );
//      
//      // add our works to the display's content panel
//      contentPanel.add( rightPanel, BorderLayout.EAST );
//      getRootPane().setDefaultButton( okButton );
//	}
//
//   private JButton makeButton ( String token )
//   {
//      JButton     button;
//      
//      button = new JButton( ResourceLoader.getDisplay(token) );
//      button.setActionCommand( token );
//      button.addActionListener( actions );
//      button.setName( token );
//      return button;
//   }
//   
//   private void setCoverButton ()
//   {
//      String token = passwordHidden ? "button.uncover" : "button.cover";
//      coverCard.show( coverPanel, token );
//   }
//
//   private JButton makeButton ( String imageName, String buttonName,
//                                String command )
//   {
//      JButton b;
//      
//      b = ToolbarHandler.makeButton( imageName, buttonName, true );
//      b.setActionCommand( command );
//      b.removeActionListener( Global.mainActionListener );
//      b.addActionListener( actions );
//      return b;
//   }
//   
//   
//   
//   /** Top-panel consists of toolbar NORTH and dialog explanatory text SOUTH. 
//    */
//	private void buildTopPanel()
//	{
//      JPanel   panel;
//      JLabel   label;
//      Font     font;
//      Icon     icon;
//      String   hstr;
//      int      status;
//
//      // create the little window toolbar
//      toolBar = new JToolBar();
//      toolBar.setFloatable( false );
//
//      copyPassIcon = makeButton( "copypass", "toolbar.copypass", "menu.edit.copypass" );
//      toolBar.add( copyPassIcon );
//      
//      copyUserIcon = makeButton( "copyuser", "toolbar.copyuser", "menu.edit.copyuser" );
//      toolBar.add( copyUserIcon );
//      
//      clearClipIcon = ToolbarHandler.makeButton( ActionHandler.getAction( "CLEARCLIP" ) );
//      toolBar.add( clearClipIcon );
//      
//      undoIcon = makeButton( "undo", "toolbar.entry.undo", "menu.edit.undo" );
//      undoIcon.setVisible( false );
//      toolBar.add( undoIcon );
//
//      redoIcon = makeButton( "redo", "toolbar.entry.redo", "menu.edit.redo" );
//      redoIcon.setVisible( false );
//      toolBar.add( redoIcon );
//      
//      toolBar.addSeparator( new Dimension( 10, 0 ) );
//
//      webstartIcon = makeButton( "webstarter", "toolbar.webstart.record", "button.starturl" );
//      webstartIcon.setEnabled( false );
//      toolBar.add( webstartIcon );
//      
//      policyIcon = makeButton( "policy", "toolbar.recpolicy", "menu.edit.policy" );
//      policyIcon.setVisible( record.getPassPolicy() != null );
//      toolBar.add( policyIcon );
//      
//      favouriteIcon = makeButton( "favourite", "toolbar.isfavourite", "menu.edit.favourite" );
//      favIcon2 = favouriteIcon.getIcon();
//      favIcon1 = ResourceLoader.getImageIcon( "filter-favourites" );
//      favouriteIcon.setVisible( Options.isOptionSet( "useFavourites" ) );
//      toolBar.add( favouriteIcon );
//      
//      floatingBarIcon = makeButton( "editfloatbar-toggle", "toolbar.floatbar", "button.editfloatbar" );
//      toolBar.add( floatingBarIcon );
//
//      historyIcon = makeButton( "editor-history", "toolbar.history", "button.historysettings" );
//      historyIcon.setVisible( false );
//      toolBar.add( historyIcon );
//      
//      toolBar.addSeparator( new Dimension( 10, 0 ) );
//
//      status = record.getImportStatus();
//      if ( status == PwsRecord.IMPORTED | status == PwsRecord.IMPORTED_CONFLICT )
//      {
//         hstr = status == PwsRecord.IMPORTED ? "editor-imported" : "editor-importedconflict"; 
//         icon =  ResourceLoader.getImageIcon( hstr );
//         label = new JLabel( icon );
//         label.setToolTipText( ResourceLoader.getDisplay( "tooltip.imported" ));
//         toolBar.add( label );
//         toolBar.addSeparator( new Dimension( 3, 0 ) );
//      }
//      
//      icon1 = ResourceLoader.getImageIcon( "editor-expired" );
//      icon2 = ResourceLoader.getImageIcon( "editor-expiresoon" );
//      expiryIcon = new BlinkingLabel();
//      expiryIcon.setPreferredSize( new Dimension( 20, 20 ) );
//      Global.addTimePulseListener( expiryIcon );
//      toolBar.add( expiryIcon );
//
//      // create panel
//      panel = new JPanel( new BorderLayout() );
//      panel.add( toolBar, BorderLayout.NORTH );
//
//      // create explain label
//      explainLabel = new JLabel();
//      panel.add( explainLabel, BorderLayout.CENTER );
//      explainLabel.setBorder( new EmptyBorder( 16, 16, 10, 16 ) );
//      font = explainLabel.getFont().deriveFont( Font.PLAIN );
//      explainLabel.setFont( font );
//
//      contentPanel.add( panel, BorderLayout.NORTH );
//	}
///*   
//   // The URLLIST content string as byte[] in UTF-8 or <b>null</b> if empty. 
//   private byte[] getUrlList ()
//   {
//      String content;
//
//      try { 
//         content = linkTable.getContent();
//         return content == null || content.equals("") ? null : 
//            content.getBytes( "UTF-8"); 
//      }
//      catch ( UnsupportedEncodingException e )
//      { 
//         GUIService.failureMessage( null, e );
//         return null; 
//      }
//   }
//*/ 
//   /** Returns the content of the URL text field.
//    * @since 0-4-0
//    */ 
//   private String getUrl ()
//   {
//      return urlFld.getText();
//   }
//   
//   /** Returns the text representation of the password history
//    * (according to PW3 format) or empty string if history is empty.
//    * 
//    *  @return String
//    *  @since 0-4-0
//    */
//   private String getPasswordHistory ()
//   {
//      if ( pwHistory.getListSize() == 0 & pwHistory.getMaxEntries() > 0 & 
//           (pwHistory.isActive() == Options.isOptionSet( "editActiveHistory" )) )
//         return "";
//      
//      return pwHistory.getContentPw3();
//   }
//   
//   private String getNotes ()
//   {
//      String hstr;
//
//      hstr = notes.getText();
//      hstr = hstr.replaceAll( "\n", "\r\n" );
//      hstr = Util.substituteText( hstr, "\r\r", "\r" );
//      return hstr;
//   }
//
//   private String getTitleFld ()
//   {
//      return title.getText();
//   }
//
//   /** Returns the content of the EMAIL field. */
//   private String getEmailFld ()
//   {
//      return email.getText();
//   }
//
//   private String getUsername ()
//   {
//      return username.getText();
//   }
//   
//   private String getGroup ()
//   {
//      Object obj = group.getSelectedItem();
//      return obj == null ? "" : obj.toString();
//   }
//
//   /** Returns the user action that caused this dialog to terminate.
//    *  This can be YES_OPTION or CANCEL_OPTION.
//    * 
//    * @return int user option
//    */
//   public int getUserOption ()
//   {
//      return userOption;
//   }
//
//   private void setPasswordHidden ( boolean value )
//   {
//      if ( value != passwordHidden )
//      {
//         passFld.setHidden( value );
//         passwordHidden = value;
//         setCoverButton();
//         if ( !value )
//            tabPane.setSelectedIndex( 0 );
//      }
//   }
//   
//   /**
//    * Returns the initial record state of the edit dialog.
//    * 
//    * @return PwsRecord
//    *  @since 0-5-0
//    */
//   public PwsRecord getInitRecord ()
//   {
//      return initRec;
//   }
//   
///*   
//   public PersistentOptions getRecodOptions ()
//   {
//      return 
//   }
//*/ 
//   
//   /** Returns the record reflecting the modifications having occurred during 
//    *  the dialog edit session.
//    *  
//    * @return <code>PwsRecord</code> the edited record result of this dialog
//    */ 
//   public PwsRecord getRecord ()
//   {
//      PwsPassphrase recPass, pass;
//      String notes1 = getNotes();
//      
//      synchronized ( record ) {
//         record.setUsername( getUsername() );
//         record.setTitle( getTitleFld() );
//         record.setEmail( getEmailFld() );
//         record.setNotes( notes1 );
//         record.setGroup( getGroup() );
//         record.setUrl( getUrl() );
//         record.setHistory( getPasswordHistory() );
//         
//         pass = passFld.getPassphrase(); // new password value
//         recPass = record.getPassword(); // old password value
////         if ( recPass == null || !equalPassphrases( recPass, pass ) )
//         if ( !equalPassphrases( recPass, pass ) ) {
//            record.setPassword( pass );  // change password
//            passwordModified( recPass );
//         }
//      }
//      
//      return record;
//   }
//   
//   /** Assembles a CRC value over all editable data components of the edit dialog. 
//    *  @since 0-4-0
//    */
//   private int dataCRC ()
//   {
//      ByteArrayOutputStream output;
//      DataOutputStream out;
//      CRC32 crc1 = new CRC32();
//      PwsPassphrase pass;
//
//      output = new ByteArrayOutputStream();
//      out = new DataOutputStream( output );
//      
//      try {
//       if ( (pass=passFld.getPassphrase()) != null )
//         // secure integration of a password value crc
//         out.writeInt( pass.hashCode() );
//         
//         if ( policy != null ) {
//            out.writeInt( policy.getIntForm() );
//            out.writeChars( new String( policy.getActiveSymbols() ));
//         }
//         if ( record.getKeyboardShortcut() != null ) {
//        	 out.writeInt( record.getKeyboardShortcut().hashCode() );
//         }
//
//         out.writeLong( record.getPassLifeTime() );
//         out.writeInt( record.getExpiryInterval() );
//
//         out.writeChars( getTitleFld() );
//         out.writeChars( getEmailFld() );
//         out.writeChars( getGroup() );
//         out.writeChars( getUsername() );
//         out.writeChars( getNotes() );
//         out.writeChars( getUrl() );
//         out.writeChars( getPasswordHistory() );
//         out.close();
//      }
//      catch ( IOException e )
//      {
//         System.out.println( "*** ERROR in PwsRecord CRC : " + e );
//         return -1;
//      }
//      
//      crc1.update( output.toByteArray() );
//      return (int)crc1.getValue();
//   }  // dataCRC
//   
//   /**
//    * Reverts the content of this edit record to its state when 
//    * edition started. 
//    */
//   private void revertContent ()
//   {
//      Log.debug( 5, "(AddDialog.revertContent) reverting edit record to initial state" ); 
//      
//      synchronized ( record )
//      {
//         record.setUsername( initRec.getUsernamePws() );
//         record.setTitle( initRec.getTitle() );
//         record.setNotes( initRec.getNotesPws() );
//         record.setGroup( initRec.getGroup() );
//         record.setUrl( initRec.getUrlPws() );
//         record.setEmail( initRec.getEmailPws() );
//         record.setHistory( initRec.getHistoryPws() );
//         record.setPassword( initRec.getPassword() );
//         try { record.setPassPolicy( initRec.getPassPolicy() ); }
//         catch ( Exception e ) {}
//         
//         record.setAccessTime( initRec.getAccessTime() );
//         record.setPassLifeTime( initRec.getPassLifeTime() );
//         record.setPassModTime( initRec.getPassModTime() );
//         record.setModifyTime( initRec.getModifiedTime() );
//         record.setExpiryInterval( initRec.getExpiryInterval() );
//         record.setKeyboardShortcut( initRec.getKeyboardShortcut() );
//         
//         crc = record.getCRC();
//   
//         // update policy and editor toolbar
//         policyIcon.setVisible( record.getPassPolicy() != null );
//         if ( (policy = record.getPassPolicy()) == null )
//            policy = container.getPassphrasePolicy();
//      }
//   }
//   
//   /** Sets the record value PASSPOLICY with the parameter
//    * value and depicts the toolbar policy icon according to
//    * whether the policy is local or not. This transaction 
//    * optionally is undoable. 
//    * @param policy <code>PwsPassphrasePolicy</code>
//    */
//   private void setPassPolicy ( PwsPassphrasePolicy policy, boolean undoable )
//   {
//      PwsPassphrasePolicy oldPolicy, superPolicy;
//      UndoableEdit edit;
//      boolean isLocalPolicy;
//      
//      oldPolicy = record.getPassPolicy();
//      superPolicy = container.getPassphrasePolicy();
//      isLocalPolicy = policy != null && !policy.equals( superPolicy );
//      try { 
//         record.setPassPolicy( isLocalPolicy ? policy : null );
//         // set the dialog's policy to records's policy
//         // and if this is null then policy equals to global policy
//         if ( (this.policy = record.getPassPolicy()) == null )
//            this.policy = container.getPassphrasePolicy();
//         policyIcon.setVisible( isLocalPolicy );
//
//         // create undoable event
//         if ( undoable ) {
//            Log.debug( 7, "(AddDialog.setPassLifeTime) creating UNDOABLE EDIT for Policy Edit" );
//            edit = undoManager.new FieldEdit( oldPolicy );
//            undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
//         }
//      }
//      catch ( InvalidPassphrasePolicy e )
//      {
//         GUIService.failureMessage( null, e );
//      }
//   }
//   
//   private void refreshExpireFields ()
//   {
//      setTimeLabel( expiryLabel, record.getPassLifeTime() );
//      setPassLifeControls();
//
//      // evaluate expiry status and display alerts if necessary
//      long scope = System.currentTimeMillis() + Options.getLongOption("expireScope");
//      boolean check = record.willExpire( scope );
//      boolean expired = record.hasExpired();
//      Icon icon = check ? expired ? icon1 : icon2 : null;
//      String hstr = expired ? "tooltip.expired" : "tooltip.expiresoon";
//
//      expiryLabel.setBlinking( expired );
//      expiryIcon.setBlinking( check );
//      expiryIcon.setIcon( icon );
//      expiryIcon.setToolTipText( check ? ResourceLoader.getDisplay( hstr ) : null );
//   }
//   
//   private void refreshTimeFields ()
//   {
//      setTimeLabel( passModLabel, record.getPassModTime() );
//      setTimeLabel( accessLabel, record.getAccessTime() );
//      setTimeLabel( expiryLabel, record.getPassLifeTime() );
//   }
//   
//   /**  @since 0-4-0 */
//   private void refreshHistoryComment ()
//   {
//      String hstr;
//      
//      if ( historyPanel == null )
//         return;
//      
//      // compose actual version of explain text (history)
//      hstr = ResourceLoader.getDisplay( "adddlg.explain.history" );
//      hstr = Util.substituteTextS( hstr, "$items", String.valueOf( pwHistory.getListSize() ) );
//      hstr = Util.substituteTextS( hstr, "$maxitems", String.valueOf( pwHistory.getMaxEntries() ) );
//      historyPanel.putClientProperty( "explain", hstr );
//      
//      // display if history panel is displayed
//      if ( tabPane.getSelectedComponent() == historyPanel )
//         explainLabel.setText( hstr );
//   }
//
//   /** Whether two parameter passphrases are equal, where <b>null</b>
//    * is considered a legal value (but not equals the empty passphrase).
//    *  
//    * @param p1 PwsPassphrase
//    * @param p2 PwsPassphrase
//    * @return boolean
//    */
//   private boolean equalPassphrases ( PwsPassphrase p1, PwsPassphrase p2 ) 
//   {
//      
//      return p1 == null & p2 == null ||
//             (p1 != null & p2 != null && p1.equals( p2 ));
//   }
//   
//   /** Handles issues depending on modification of the password value.
//    * 
//    * @param oldValue old password value
//    * @param time creation time of the old value (history entry)
//    * @since 0-5-0 modified parameter list
//    */ 
//   private void passwordModified ( PwsPassphrase oldValue  )
//   {
//      UndoableEdit edit;
//      String hstr, time, oldHistory;
//      long oldExpiry, oldModTime, oldAccTime;
//      int index, interval;
//      boolean isPeriod;
//      
//      synchronized ( record ) {
//         // collect data
//         oldHistory = pwHistory.getContentPw3();
//         oldExpiry = record.getPassLifeTime();
//         oldModTime = record.getPassModTime(); // password creation time before change
//         oldAccTime = record.getAccessTime(); // last access time before change
//   
//         // update time values in record 
//         record.passwordUpdated();
//   
//         // enter old value into history
//         if ( oldValue != null ) {
//            pwHistory.pushPassword( oldValue.getString(), oldModTime );
//         }
//         
//         // handle time value issues
//         refreshTimeFields();
//         if ( oldExpiry > 0 ) {
//            isPeriod = expireCombo.getSelectedIndex() == LIFE_PERIOD; 
//            index = expireCombo.getSelectedIndex();
//            if ( index > LIFE_UNLIMITED & index < LIFE_SPECIAL )
//            {
//               // prepare text
//               interval = record.getExpiryInterval();
//               time = isPeriod ? 
//                     interval + " " + ResourceLoader.getDisplay( "units.days" )
//                      : expireCombo.getSelectedItem().toString();
//               hstr = ResourceLoader.getDisplay( "msg.update_lifetime" );
//               hstr = Util.substituteText( hstr, "$life", time );
//   
//               // ask user for recalculation of expiry date
//               tabPane.setSelectedComponent( statsPanel );
//               if ( GUIService.userConfirm( this, hstr ) )
//               {
//                  if ( isPeriod )
//                     setPassLifeTimeAfterInterval( interval, false );
//                  else
//                     setPassLifeTimeAfterIndex( index, false );
//               }
//               else
//                  setPassLifeControls(); 
//            }
//         }
//         
//         // create undoable edit 
//         if ( !equalPassphrases( passFld.getPassphrase(), oldValue ) ) {
//            edit = undoManager.new FieldEdit ( 
//                  oldValue, oldHistory, oldModTime, oldExpiry, oldAccTime );
//            undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
//         }
//      }
//   }  // passwordModified
//   
//   /** Whether there was some modification to the record during dialog. */
//   public boolean recordModified ()
//   {
//      // measures to ignore the modify time
//      PwsRecord rec = (PwsRecord)getRecord().clone();
//      rec.setModifyTime( initRec.getModifiedTime() );
//      
//      return crc != rec.getCRC();
//   }
//   
//   /** Whether there was user modification of the editable record data during dialog.
//    *  @since 0-4-0
//    */
//   public boolean dataModified ()
//   {
//      return dataCrc != dataCRC();
//   }
//   
//   /** Whether the edited record is a new entry (not contained in database).
//    * @return <b>true</b> if and only if this instance was constructed by
//    *         initializer ( Frame, String )
//    * @since 0-5-0        
//    */
//   public boolean isNewRecord ()
//   {
//      return !editing;
//   }
//   
////  ***********  INNER CLASSES  *************
//
//@SuppressWarnings("serial")
//private class DlgPasswordField extends EditorTextField
//{
//   private PwsPassphrase passdata;
//   private boolean isHidden;
//
//   public DlgPasswordField ()
//   {
//      this( null );
//   }
//   
//   public DlgPasswordField ( PwsPassphrase pass )
//   {
//      super( TEXTFIELDSIZE );
//      setHidden( true );
//      setPassphrase( pass );
//   }
///*   
//   public boolean isHidden ()
//   {
//      return isHidden;
//   }
//*/   
//   public void setHidden ( boolean v )
//   {
//      if ( v != isHidden )
//      {
//         isHidden = v;
//         if ( isHidden )
//         {
//            super.setText( HIDEDUMMY );
//            setEditable( false );
//            setPopupActive( false );
//         }
//         else
//         {
//            super.setText( passdata.getString() );
//            setEditable( true );
//            setPopupActive( true );
//         }
//         clearUndoList();
//      }
//   }
//   
//   /**
//    * Returns the passphrase of this field in its current edit value
//    * (as a clone). Returns <b>null</b> for an empty passphrase.
//    * 
//    * @return <code>PwsPassphrase</code>
//    */
//   public PwsPassphrase getPassphrase ()
//   {
//      if ( !isHidden )
//         passdata.setValue( super.getText().toCharArray() );
//      
//      
//      return passdata.isEmpty() ? null : (PwsPassphrase)passdata.clone();
//   }
//   
//   /**
//    * Sets the current edit value of this password field
//    * with an encrypted passphrase value.
//    * 
//    * @param pass <code>PwsPassphrase</code>; <b>null</b> for clearing
//    */
//   public void setPassphrase ( PwsPassphrase pass )
//   {
//      if ( pass == null )
//         passdata = new PwsPassphrase();
//      else 
//         passdata = pass;
//      
//      if ( !isHidden )
//         super.setText( passdata.getString() );
//   }
//   
//   /**
//    * Sets the current edit value of this password field
//    * with a plain text value.
//    * 
//    * @param text 
//    */
//   @Override
//public void setText ( String text )
//   {
//      if ( text == null )
//         throw new NullPointerException();
//      
//      passdata.setValue( text );
//      
//      if ( !isHidden )
//         super.setText( passdata.getString() );
//   }
///*   
//   public String getText ()
//   {
//      if ( !isHidden )
//         return super.getText();
//      return passdata.getString();
//   }
//*/   
//}  // class DlgPasswordField
//   
//@SuppressWarnings("serial")
//private class FloatingToolbar extends ButtonBarDialog {
//
//   private  JButton passButton, userButton, browseButton;
//
//   public FloatingToolbar () {
//      super( AddDialog.this, DialogButtonBar.BUTTONLESS, false );
//      init();
//   }
//
//   private void init () {
//      
//      // Dialog settings
//      moveRelatedTo( AddDialog.this );
//      setAutonomous( true );
//      setCloseByEscape( false );
//      setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
//  
//      // register ESCAPE key and translate to parent dialog
//      ActionListener kli = new ActionListener ()
//      {
//         @Override
//		public void actionPerformed ( ActionEvent e )
//         {
//             ((ButtonBarDialog)getParent()).processWindowEvent( new WindowEvent(
//                FloatingToolbar.this, WindowEvent.WINDOW_CLOSING ));
//         }
//      };
//      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
//      getRootPane().registerKeyboardAction(
//            kli, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//
//      // create buttons and add to toolbar 
//      JPanel barPanel = new JPanel();
//      passButton = makeButton( "copypass-large", "toolbar.copypass", "menu.edit.copypass" );
//      barPanel.add( passButton );
////      addSeparator();
//
//      userButton = makeButton( "copyuser-large", "toolbar.copyuser", "menu.edit.copyuser" );
//      barPanel.add( userButton );
////      addSeparator();
//
//      browseButton = makeButton( "webstarter-large", "toolbar.webstart.record", "button.starturl" );
//      browseButton.setEnabled( false );
//      barPanel.add( browseButton );
//
//      setDialogPanel( barPanel );
//      
//      if ( urlFld != null )
//         setBrowsingEnabled( Util.extractURL(getUrl()) != null );
//   }
//   
//   /** Sets enables status of the browsing button. Default state is <b>false</b>. */
//   public void setBrowsingEnabled ( boolean s ) {
//      browseButton.setEnabled( s );
//   }
//   
//   private JButton makeButton ( String imageName, String buttonName,
//         String command )
//   {
//      JButton b;
//      ImageIcon icon;
//      
//      b = new JButton();
//      b.setMargin( new Insets( 0, 0, 0, 0 ) );
//      b.setToolTipText( ResourceLoader.getCommand(buttonName+".tooltip") );
//      icon = ResourceLoader.getImageIcon(imageName);
//      icon.setDescription( ResourceLoader.getDisplay(buttonName) );
//      b.setIcon( icon ); 
//      b.setActionCommand( command );
//      b.addActionListener( actions );
//      return b;
//   }
//}
//
///**
// * Extends HistoryHandler with some specific display refresh operations.
// * @since 0-4-0
// */   
//@SuppressWarnings("serial")
//private class History extends HistoryHandler
//{
//   /**
//    *  @since 0-5-0
//    */
//   public History ()
//   {
//      super();
//      setFont( DisplayManager.getFont( "data" ) );
//   }
//
//   @Override
//   public void pushPassword ( String password, long time )
//   {
//      super.pushPassword( password, time );
//      refreshHistoryComment();
//      controlHistoryPanel();
//   }
//
//   @Override
//   public void setMaxItems ( int max )
//   {
//      super.setMaxItems( max );
//      refreshHistoryComment();
//      controlHistoryPanel();
//   }
//   
//   @Override
//   public void setContentPw3 ( String pw3 )
//   {
//      int selTab = tabPane.getSelectedIndex();
//      super.setContentPw3( pw3 );
//      if ( pw3 == null && !Options.isOptionSet( "editActiveHistory" ) )
//         super.setActive( false );
//
//      refreshHistoryComment();
//      controlHistoryPanel();
//      if ( selTab < tabPane.getTabCount() )
//         tabPane.setSelectedIndex( selTab );
//   }
//
//   // since 0-5-0
//   @Override
//public void clear ()
//   {
//      super.clear();
//      controlHistoryPanel();
//   }
//
//   // since 0-5-0
//   @Override
//public void setActive ( boolean v )
//   {
//      super.setActive( v );
//      controlHistoryPanel();
//      
//      // remove all undoables concerning history
//      if ( !v )
//         undoManager.removeHistoryEdits();
//   }
//}  // inner class History
//   
//   
//private class Actions implements Runnable, ActionListener, FocusListener, 
//                      ChangeListener, ItemListener, KeyListener
//{
//   Future<?> actionThread;
//   Future<?> spinnerActionFuture;
//   String command;
//   Object source;
//   
//   public Actions ()
//   {}
//   
//   private Actions ( String cmd ) {
//      command = cmd;
//   }
//   
//   /** Performs a new action thread only if not a previous action triggered 
//    *  through this <code>Actions</code> instance is still running. Otherwise 
//    *  returns undone. 
//   */ 
//   @Override
//   public void actionPerformed ( ActionEvent arg ) {
//      if ( !actionSemaphor.isOpen() || 
//    	   (actionThread != null && !actionThread.isDone()) )
//         return;
//      
//      command = arg.getActionCommand();
//      source = arg.getSource();
//      actionThread = ActionHandler.startTask( this );
//   }
//
//   /** Synchronously performs the specified action in the same thread. */ 
//   public void callAction ( String cmd ) {
//      new Actions( cmd ).run();
//   }
//   
//   @Override
//   public void run ()
//   {
//      UndoableEdit edit;
//      ActionEvent evt;
//      String hstr, hstr2;
//      boolean ok, dataModified;
//      int i, j, type;
//      
//      Log.log( 8, "(AddDialog.Actions.run) starting actions interpreter, CMD=".concat( command ) );
//      
//      // trigger user activity / update the global action time value
//      objectListener.activity();
//
//      // handle PASSLIFETIME combo events
//      if ( source == expireCombo )
//      {
//         i = expireCombo.getSelectedIndex();
//         expIntervalPanel.setVisible( i == LIFE_PERIOD );
//         setPassLifeTimeAfterIndex( i, true );
//      }
//      
//      // AddDialog actions
//      else if ( command.equals( "button.ok" ) )
//      {
//         // data set validity check
//         ok = true;
//         if ( getTitleFld().equals("") || passFld.getPassphrase() == null )
//         {
//            // reaction to invalid (display error message)
//            if ( badFieldText == null )
//               badFieldText = ResourceLoader.getDisplay( "msg.badfieldvalue" );
//            
//            if ( getTitleFld().equals("") )
//               hstr2 = "adddlg.label.title";
//            else
//               hstr2 = "adddlg.label.password";
//            hstr = Util.substituteText( badFieldText, "$field", 
//                   ResourceLoader.getDisplay( hstr2 ) );
//            
//            GUIService.infoMessage( AddDialog.this, "dlg.badrecord", hstr );
//            ok = false;
//         }
//
//         // check user confirm on password change
//         else if ( editing && !passFld.getPassphrase().equals( initRec.getPassword() ) && 
//               Options.isOptionSet( "confirmUpdatePass" ) )
//         {
//            ok = GUIService.userConfirm( AddDialog.this, "msg.ask.passmodify" );
//         }
//            
//         if ( ok )
//         {
//            // terminate subsidiary tasks
//            if ( spinnerActionFuture != null ) {
//            	try {
//					spinnerActionFuture.get();
//				} catch (InterruptedException e) {
//				} catch (ExecutionException e) {
//				}
//            }
//            
//            // if valid: call external action event handler
//            if ( externalActions != null )
//            {
//               // if there was no user change, reset modify time
//               // ( not to bother other applications unnecessarily)
//               if ( !dataModified() ) {
//                  record.setModifyTime( initRec.getModifiedTime() );
//               } else {
//                  correctRecord();
//               }
//               
//               // issue event for external action
//               type = editing ? DIALOG_EDIT : DIALOG_NEW;
//               evt = new ActionEvent( AddDialog.this, type, "dialog.action.ok" );
//               externalActions.actionPerformed( evt );
//            }
//
//            // terminate the dialog
//            userOption = YES_OPTION;
//            dispose();
//         }
//      }
//      
//      else if ( command.equals( "button.cancel" ) )
//      {
//         // terminate subsidiary tasks
//         if ( spinnerActionFuture != null ) {
//        	 try {
//				spinnerActionFuture.get();
//			} catch (InterruptedException e) {
//			} catch (ExecutionException e) {
//			}
//         }
//         
//         ok = true;
//         dataModified = dataModified();
//         
//         // check user confirm if record changed
//         if ( dataModified ) {
//            correctRecord();
//            ok = GUIService.userConfirm( AddDialog.this, "msg.ask.cancelrecord" );
//         }
//            
//         if ( ok )
//         {
//            // if cancel confirmed: call external action event handler
//            if ( externalActions != null )
//            {
//               // if there was no user change, reset modify time
//               if ( !dataModified )
//                  record.setModifyTime( initRec.getModifiedTime() );
//               
//               // issue event for external action
//               type = editing ? DIALOG_EDIT : DIALOG_NEW;
//               evt = new ActionEvent( AddDialog.this, type, "dialog.action.cancel" );
//               externalActions.actionPerformed( evt );
//            }
//
////            revertContent();
//            userOption = CANCEL_OPTION;
//            dispose();
//         }
//         
//         else if ( recentFocusOwner != null )
//            recentFocusOwner.requestFocus();
//      }
//
//      else if ( command.equals( "button.help" ) )
//      {
//         GUIService.toggleHelpDialog( AddDialog.this, "dlg.help.entryeditor" );
//         if ( recentFocusOwner != null )
//             recentFocusOwner.requestFocus();
//      }
//      
//      else if ( command.equals( "button.revert" ) )
//      {
//         PwsRecord rec;
//         
//         if ( dataModified() )
//         {
//            // check user confirm if record changed
//            ok = true;
//            if ( !Options.isOptionSet( "useUndoRedo" ) )
//            {
//               ok = GUIService.userConfirm( AddDialog.this, "msg.ask.revertrecord" );
//            }
//            
//            if ( ok )
//            {
//               // perform revert content
//               rec = (PwsRecord)getRecord().clone();
//               revertContent();
//               setupDisplay( initGroup );
//            
//               // create undoable edit event
//               edit = undoManager.new RevertEdit( rec );
//               undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
//            }
//         }
//      }
//      
//      else if ( command.equals( "button.cover" ) )
//      {
//         setPasswordHidden( true );
//      }
//      
//      else if ( command.equals( "button.uncover" ) )
//      {
//         setPasswordHidden( false );
//      }
//      
//      else if ( command.equals( "button.randpassword" ) )
//      {
//         PwsPassphrasePolicy policyOld = (PwsPassphrasePolicy)policy.clone();
//         PwsPassphrase pass = GUIService.generateRandomPassphrase( AddDialog.this, null, policy );
//         
//         // if passphrase-policy has been edited
//         if ( !policyOld.equals( policy ) ) {
//            setPassPolicy( policy, true );
//         }
//         
//         // if user created new password
//         if ( pass != null )
//         {
//            passFld.setPassphrase( pass );
//            getRecord();
//            refreshTimeFields();
//         }
//      }
//
//      else if ( command.equals( "button.extrapassword" ) )
//      {
//    	 Runnable run = new Runnable() {
//			@Override
//			public void run() {
//		       PwsPassphrasePolicy policyOld = (PwsPassphrasePolicy)policy.clone();
//		       Service.generatePassword( AddDialog.this, policy, true );
//
//		       // if passphrase-policy has been edited
//		       if ( !policyOld.equals( policy ) ) {
//		          setPassPolicy( policy, true );
//		       }
//			}
//    	 };
//    	 ActionHandler.executeOnEDT(run);
//      }
//      
//      else if ( command.equals( "button.editfloatbar" ) )
//      {
//         PersistentOptions options = getFloatingToolbarOptions();
//
//         if ( floatToolbar == null )
//         {
//            floatToolbar = new FloatingToolbar();
//            floatToolbar.gainLocation( options, "dialogFloatingBarBounds", true );
//            floatToolbar.show();
//            options.setOption( "editor-floatingbar-active", "true" );
//            
//            // set initial position of floating toolbar in case there is no position memory
//            if ( options.getBounds( "dialogFloatingBarBounds" ) == null ) 
//            {
//               floatToolbar.setCorrectedLocation( new Point(450, 250) );
//            }
//         }
//         else
//         {
//            floatToolbar.setVisible( !floatToolbar.isVisible() );
//            options.setOption( "editor-floatingbar-active", "false" );
//         }
//      }
//      
//      else if ( command.equals( "button.expirydate" ) )
//      {
//         // ask user for text input
//         long time = record.getPassLifeTime();
//         hstr = time == 0 ? null : Util.standardTimeString( time );
//         hstr = GUIService.userInput( tabPane, 
//               ResourceLoader.getDisplay( "dlg.input.expirydate" ),
//               ResourceLoader.getDisplay( "format.standarddate" ),
//               hstr );
//         objectListener.activity();
//         
//         // extract time value and set expiry date
//         if ( hstr != null )
//         {
//            if ( (time = Util.timeFromString( hstr, null )) == -1 )
//               GUIService.infoMessage( tabPane, "dlg.badvalue", "msg.badtimevalue");
//            else
//            {
//               setPassLifeTime( time, 0, true );
//            }
//         }
//      }
//      
//      else if ( command.equals( "button.clearhistory" ) )
//      {
//         if ( GUIService.userConfirm( pwHistory, "msg.passhist.delete" ) )
//         {
//            hstr = pwHistory.getContentPw3();
//            pwHistory.clear();
//            
//            // create undoable edit event
//            edit = undoManager.new FieldEdit( EditUndoManager.FieldEdit.HISTORY_DEL, hstr, pwHistory.getContentPw3() );
//            undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
//         }
//         objectListener.activity();
//      }
//      
//      else if ( command.equals( "button.historysettings" ) )
//      {
//         boolean iconPressed;
//         
//         // ask user for text input
//         i = pwHistory.getMaxEntries();
//         iconPressed = source == historyIcon;
//         
//         try {
//            // perform user input if maxhist == 0 or the "maximum" button is pressed
//            if ( i == 0 | !iconPressed )
//               j = GUIService.integerInput( tabPane, 
//                     ResourceLoader.getDisplay( "dlg.input.historymax" ),
//                     null, 0, 255, i );
//            else
//               j = i;
//            
//            hstr = pwHistory.getContentPw3();
//            pwHistory.setMaxItems( j );
//            pwHistory.setActive( j > 0 );
//            
//            if ( iconPressed & pwHistory.isActive() & pwHistory.getListSize() > 0 )
//               tabPane.setSelectedComponent( historyPanel );
//            
//            if ( j != i )
//            {
//               // create undoable edit event
//               edit = undoManager.new FieldEdit( EditUndoManager.FieldEdit.HISTORY_MAX, hstr, pwHistory.getContentPw3() );
//               undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
//            }
//         }
//         catch ( UserBreakException e )
//         {}
//         objectListener.activity();
//      }
//      
//      else if ( command.equals( "button.historyoff" ) )
//      {
//         pwHistory.setActive( false );
//      }
//      
//      else if ( command.equals( "menu.edit.copypass" ) )
//      {
//    	 boolean update = container.isModified() || 
//                		Options.isOptionSet( "storeMinorChanges" );
//         if (ActionHandler.sendClipboardPassword( AddDialog.this, getRecord(), update )) 
//         {
//            container.recordUsed( record );
//            refreshTimeFields();
//         }
//      }
//      
//      else if ( command.equals( "button.shortcutkey" ) )
//      {
//    	  setupShortcutKey();
//      }
//      
//      else if ( command.equals( "button.copyemail" ) )
//      {
//         // if field is empty AND there is a valid email address in the clipboard
//         // copy it into EMAIL
//         if ( (hstr = getEmailFld()).isEmpty() )
//         {
//            hstr = Global.getClipboardText().trim();
//            if ( Util.isEmailAddress( hstr ) )
//               email.setText( hstr );
//         }
//         // if field is NOT empty
//         else 
//         {
//            // if there is a valid email address, start external client
//            if ( (hstr = Util.extractMailaddress( hstr )) != null )
//            {
//               Global.startEmail( hstr );
//            }
//            // else copy entire content to the clipboard
//            else
//               ActionHandler.sendClipboardText( AddDialog.this, getEmailFld(),
//                  "confirm.email" );
//         }
//      }
//      
//      else if ( command.equals( "button.starturl" ) )
//      {
//         URL url; 
//         
//         // if there is a valid URL in the clipboard
//         // copy it into the URL Field
//         if ( (hstr = getUrl()).length() == 0 )
//         {
//            hstr = Global.getClipboardText();
//            try { 
//               new URL( hstr );
//               urlFld.setText( hstr );
//            }
//            catch ( MalformedURLException e )
//            {}
//         }
//         
//         else if ( (url = Util.extractURL( hstr )) != null ) 
//         {
//            Global.startBrowser( url );
//            if ( Options.isOptionSet( "useEntryOnBrowse" ) )
//               container.recordUsed( record );
//            if ( Options.isOptionSet( "autoCopyPass" ) )
//               callAction( "menu.edit.copypass" );
//         }
//            else
//               GUIService.infoMessage( AddDialog.this, "dlg.operfailure", "msg.url.failurl" );
//      }
//      
//      else if ( command.equals( "button.tearoff.notes" ) )
//      {
//         JPanel panel;
//         
//         // create free editor window
//         panel = new JPanel( new BorderLayout() );
//         panel.setPreferredSize( new Dimension( 300, 180 ) );
//         panel.add( notesScrollPane, BorderLayout.CENTER );
//         tearoffNotesDialog = new ButtonBarDialog( AddDialog.this, panel, DialogButtonBar.OK_BUTTON, false )
//         {
//            @Override
//			public void dispose ()
//            {
//               // re-install "regular" editor place
//               if ( tearoffNotesDialog != null )
//               {
//                  // optionally remember window bounds
//                  if ( Options.isOptionSet( "rememberScreen" ) )
//                     storeBounds( Options.getOptions(), "tearoff_notes", true );
//                
//                  notesPanel.add( notesScrollPane );
//                  notesPanel.getParent().validate();
////                  notesPanel.repaint();
//                  tearOffButton.setEnabled( true );
//                  tearoffNotesDialog = null;
//                  objectListener.activity();
//               }
//
//               super.dispose();
//            }
//         };
//         tearoffNotesDialog.setTitle( ResourceLoader.getDisplay("adddlg.label.notes") );
//         tearoffNotesDialog.moveRelatedTo( AddDialog.this );
//         tearoffNotesDialog.setResizable( true );
//         tearoffNotesDialog.setAutonomous( true );
//         
//         // optionally restore last remembered window bounds (if available)
//         if ( Options.isOptionSet( "rememberScreen" ) )
//            tearoffNotesDialog.gainBounds( Options.getOptions(), "tearoff_notes", true );
//
//         // disable "regular" editor place
//         tearOffButton.setEnabled( false );
//         notesPanel.repaint();
//         
//         tearoffNotesDialog.show();
//      }
//      
//      else if ( command.equals( "menu.edit.copyuser" ) )
//      {
//         ActionHandler.sendClipboardUsername( AddDialog.this, getRecord() );
//      }
//      
//      else if ( command.equals( "menu.edit.policy" ) )
//      {
//         final PolicyDialog dlg;
//         final PwsPassphrasePolicy oldPolicy;
//         
//         oldPolicy = (PwsPassphrasePolicy)policy.clone();
//         dlg = new PolicyDialog( AddDialog.this, policy, 1 );
//         dlg.setAutonomous( true );
//         dlg.setSynchronous( true );
//         dlg.moveRelatedTo( AddDialog.this );
//         dlg.addButtonBarListener( new DefaultButtonBarListener() 
//         {
//            @Override
//            public boolean okButtonPerformed ()
//            {
//               policy = dlg.getEditedPolicy();
//               if ( policy.isValid() && !policy.equals( oldPolicy ) )
//               {
//                  setPassPolicy( policy, true );
//               }
//               dlg.dispose();
//               return true;
//            }
//         });
//         dlg.setVisible( true );
//      }
//      
//      else if ( command.equals( "menu.edit.undo" ) )
//      {
////         System.out.println( "-- ENTRY UNDO: " ); 
//         undoManager.undo();
//      }
//      
//      
//      else if ( command.equals( "menu.edit.redo" ) )
//      {
////         System.out.println( "-- ENTRY REDO: " ); 
//         undoManager.redo();
//      }
//      
//      else if ( command.equals( "menu.edit.favourite" ) )
//      {
//         isFavourite = !isFavourite;
//         System.out.println( "-- ENTRY Favourite == " + isFavourite );
//         
//         // modify "Favourite" status in file-container
//         container.setRecordFavourite( initRec, isFavourite );
//         
//         // adjust icon display
//         favouriteIcon.setIcon( isFavourite ? favIcon2 : favIcon1 );
//         favouriteIcon.setRolloverIcon( isFavourite ? favIcon2 : favIcon1 );
//         favouriteIcon.setToolTipText( ResourceLoader.getCommand( isFavourite ? 
//               "toolbar.isfavourite.tooltip" : "toolbar.isnofavourite.tooltip" ));
//      }
//      
//      Log.log( 8, "(AddDialog.Actions.run) record action interpreter DONE" );
//   }  // run
//
//private void setupShortcutKey () {
//  KeyStroke stroke = record.getKeyboardShortcut();
//  final JLabel keyLabel = new JLabel( stroke == null ? null : stroke.toString() );
//  final JLabel keyDisplayLabel = new JLabel( stroke == null ? " " : keyStrokeText(stroke) );
//  final Set<KeyStroke> accKeys = MenuHandler.getAccelerators();
//  
//  String text = ResourceLoader.getDisplay("msg.edit.shortcutkey");
//  JLabel textLabel = new JLabel(text);
//  textLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
//  Border border = BorderFactory.createEmptyBorder(8, 8, 8, 8);
//  border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), border);
//  keyDisplayLabel.setBorder(border);
//  Font font = DisplayManager.getFont("data").deriveFont(Font.PLAIN, (float)14);
//  keyDisplayLabel.setFont(font);
//  final JLabel msgLabel = new JLabel(" ");
//  msgLabel.setForeground(Color.RED);
//  VerticalFlowLayout fLayout = new VerticalFlowLayout(10);
//  fLayout.setAlignment( VerticalFlowLayout.CENTER );
//  JPanel panel = new JPanel(fLayout);
//  panel.add(textLabel);
//  panel.add(keyDisplayLabel);
//  panel.add(msgLabel);
//  
//  final ButtonBarDialog dlg = new ButtonBarDialog(AddDialog.this, panel, 
//		  DialogButtonBar.OK_CANCEL_BUTTON, true);
//  dlg.setTitle( ResourceLoader.getDisplay("dlg.edit.shortcutkey") );
//  dlg.moveRelatedTo(AddDialog.this);
//  
//  // install a listener to key input
//  KeyListener keyListener =  new KeyAdapter() {
//
//	@Override
//	public void keyPressed(KeyEvent e) {
//		// receive entered key
//		int keyCode = e.getKeyCode();
//		int modif = e.getModifiers();
//		KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, modif);
//		Log.log(10, "(AddDialog.Actions) -- received KEY event: " + keyCode + " - " + modif + ", stroke: " + stroke);
//	
//		if ( isAllowedKeyStroke(stroke) ) {
//			// show and accept entered stroke
//			keyDisplayLabel.setText(keyStrokeText(stroke));
//			keyLabel.setText(stroke.toString());
//			e.consume();
//			String text = null;
//
//			// warning about conflict with other record
//			PwsRecord rec = conflictingRecord(stroke); 
//			if ( rec != null ) {
//				text = ResourceLoader.getDisplay("msg.edit.conflictkey");
//				text = Util.substituteText(text, "$record", rec.getTitle());
//			}
//			
//			// warning about accelerator conflict
//		    else if ( accKeys.contains(stroke) ) {
//				text = ResourceLoader.getDisplay("msg.edit.reservedkey");
//				text = Util.substituteText(text, "$key", keyStrokeText(stroke));
//		    }
//
//			msgLabel.setText(text);
//		}
//	}
//
//	/** Returns the first record in the natural order of the database
//	 * which contains the given shortcut key.
//	 *  
//	 * @param stroke <code>KeyStroke</code>
//	 * @return <code>PwsRecord</code> or null if not found
//	 */
//	private PwsRecord conflictingRecord (KeyStroke stroke) {
//		for (Iterator<PwsRecord> it = container.iterator(); it.hasNext();) {
//			PwsRecord record = it.next();
//			KeyStroke recStroke = record.getKeyboardShortcut(); 
//			if ( recStroke != null && !record.equals(initRec) && 
//				 recStroke.equals(stroke) ) 
//				return record;
//		}
//		return null;
//	}
//
//	/** Whether the given key-stroke is not a case of the basic
//	 * functional restriction set.
//	 * 
//	 * @param stroke <code>KeyStroke</code>
//	 * @return boolean true == key is allowed
//	 */
//	private boolean isAllowedKeyStroke (KeyStroke stroke) {
//		if ( stroke == null || Global.forbiddenKeys.contains(stroke) ) return false;
//		int code = stroke.getKeyCode();
//		if ( code == 0 | code == KeyEvent.VK_SHIFT | code == KeyEvent.VK_CONTROL
//			 | code == KeyEvent.VK_ALT | code == KeyEvent.VK_ALT_GRAPH 
//			 | code == KeyEvent.VK_META ) return false;
//		return true;
//	}
//  };
//  
//  // add "Remove" button to button bar, including action
//  JButton removeButton = new JButton(ResourceLoader.getDisplay("button.remove")); 
//  removeButton.addActionListener( new ActionListener() {
//
//	@Override
//	public void actionPerformed(ActionEvent e) {
//	  dlg.dispose();
//	  record.setKeyboardShortcut(null);
//	  shortKeyValueLabel.setText(null);
//	  Log.debug(10, "(AddDialog.Actions) removed shortcut-key assignment");
//	}
//  });
//  
//  removeButton.addKeyListener(keyListener);
//  dlg.getButtonBar().getOkButton().addKeyListener(keyListener); 
//  dlg.getButtonBar().getCancelButton().addKeyListener(keyListener);
//  dlg.getButtonBar().add(removeButton);
//  dlg.show();
//  
//  if ( dlg.isOkPressed() ) {
//	  stroke = KeyStroke.getKeyStroke(keyLabel.getText());
//	  record.setKeyboardShortcut(stroke);
//	  shortKeyValueLabel.setText(keyDisplayLabel.getText());
//	  Log.debug(10, "(AddDialog.Actions) set new shortcut-key: " + stroke);
//  }
//}
//   
//   // ************ ChangeListener ************
//   @Override
//<<<<<<< HEAD
//public void stateChanged ( ChangeEvent e )
//   {
//=======
//   public void stateChanged ( ChangeEvent e ) {
//>>>>>>> new-editor
//      if ( !actionSemaphor.isOpen() ) return;
//      
//      if ( e.getSource() == tabPane ) {
//         JPanel panel = (JPanel)tabPane.getSelectedComponent();
//         if ( panel != null )
//            explainLabel.setText( (String)panel.getClientProperty( "explain" ) );
//      }
//      
//      else if ( e.getSource() == expIntervalSpinner ) {
//         Log.log( 9, "(AddDialog.Actions.stateChanged) for expIntervalSpinner" );
//<<<<<<< HEAD
//         if ( spinnerActionFuture != null )
//        	 spinnerActionFuture.cancel(false);
//         
//         Runnable run =   new Runnable() {
//               @Override
//			   public void run () {
//                  Log.log( 7, "(AddDialog.Actions.spinnerActionThread) executing record update" );
//                  int days = ((Integer)expIntervalSpinner.getValue()).intValue();
//                  setPassLifeTimeAfterInterval( days, true );
//               }
//            };
//         spinnerActionFuture = ActionHandler.startTaskDelayed(run, SPINNERACTION_DELAYTIME); 
//=======
//         if ( spinnerActionThread != null ) {
//            spinnerActionThread.cancel();
//         }
//         
//         spinnerActionThread = new ActionHandler.TimedThread( 
//               new Runnable() {
//                  @Override
//				  public void run () {
//                     Log.log( 7, "(AddDialog.Actions.spinnerActionThread) executing record update" );
//                     int days = ((Integer)expIntervalSpinner.getValue()).intValue();
//                     setPassLifeTimeAfterInterval( days, true );
//                  }
//               } , "EditorSpinnerAction", SPINNERACTION_DELAYTIME );
//>>>>>>> new-editor
//      }
//   }
//   
//   //  *********  ITEMLISTENER  ***********
//   
//   // since 0-5-0
//   @Override
//   public void itemStateChanged ( ItemEvent e ) {
//      UndoableEdit edit;
//      String value;
//      
////      System.out.println( "*** Item State Changed" );
//      value = (String)e.getItem();
//
//      // this deals with 
//      if ( e.getSource() == group  && e.getStateChange() == ItemEvent.SELECTED && 
//           !value.equals( actGroup ) )
//      {
////         System.out.println( "-   New Group Value (Item): ".concat( value ) );
//         
//         // create undoable edit event
//         edit = undoManager.new FieldEdit( EditUndoManager.FieldEdit.GROUP, actGroup, value );
//         undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
//         
//         actGroup = value;
//      }
//   }
//   
//   //  *******  FOCUSLISTENER  *******
//
//   
//   @Override
//   public void focusGained ( FocusEvent e )
//   {}
//   
//   @Override
//   public void focusLost ( FocusEvent e )
//   {
//      Component comp;
//      boolean exemption;
//      
//      recentFocusOwner = e.getComponent();
//      comp = e.getOppositeComponent();
//      exemption = comp == revertButton | comp == cancelButton;
//      if ( e.getComponent() == passFld )
//      {
//         if ( !equalPassphrases(passFld.getPassphrase(), record.getPassword())
//              & !exemption )
//         {
//            getRecord();
//         }
//      }
//   }
//
//   @Override
//   public void keyPressed ( KeyEvent e )
//   {
//      Log.log( 9, "(AddDialog.Actions.KeyListener) KEY PRESSED: " + e.getKeyChar() );
//      
//   }
//
//   @Override
//public void keyReleased ( KeyEvent e )
//   {
//      Log.log( 9, "(AddDialog.Actions.KeyListener) KEY RELEASED: " + e.getKeyChar() );
//      
//   }
//
//   @Override
//public void keyTyped ( KeyEvent e )
//   {
//      Log.log( 9, "(AddDialog.Actions.KeyListener) KEY TYPED: " + e.getKeyChar() );
//      
//   }
//
//}  // inner class Actions
//
///**
// * 
// * since 0-5-0
// */
//@SuppressWarnings("serial")
//public class EditUndoManager extends javax.swing.undo.UndoManager
//{
//
//
//public EditUndoManager ()
//{
//   int i, lim;
//   
//   i = Options.getIntOption( "maxUndoEntries" );
//   lim = i > 0 ? i : Global.DEFAULT_MAXUNDO;
//   this.setLimit( lim );
//}
//
///** Removes all entries in this Undo manager and updates menues. */
//public void clear ()
//{
//   this.discardAllEdits();
//   undoUpdated();
//}
//
//@Override
//public void undoableEditHappened ( UndoableEditEvent e )
//{
//   if ( Options.isOptionSet( "useUndoRedo" ) )
//   {
//      super.undoableEditHappened( e );
//      undoUpdated();
//      
//      // protocol
////      System.out.println( "-- new UNDOABLE EDIT: Source=" + 
////            e.getSource() + ", EDIT=" + e.getEdit() ); 
////      System.out.println( "   Manager Status = " + this.edits.size() );
//   }
//}
//
//@Override
//public void redo () throws CannotRedoException
//{
//   super.redo();
//   undoUpdated();
//}
//
//@Override
//public void undo () throws CannotUndoException
//{
//   super.undo();
//   undoUpdated();
//}
//
//public void removeHistoryEdits ()
//{
//   Object obj;
//   FieldEdit edit;
//   int i, j;
//   
//   i = j = 0;
//   while ( i < edits.size() )
//   {
//      if ( (obj = edits.get( i )) instanceof FieldEdit ) 
//      {
//         edit = (FieldEdit)obj;
//         if ( edit.type == FieldEdit.HISTORY_MAX | edit.type == FieldEdit.HISTORY_DEL )
//         {
//            trimEdits( i, i );
//            j++;
//            continue;
//         }
//      }
//      i++;
//   }
//   if ( j > 0 )
//      undoUpdated();
//   
////   System.out.println( "-- RemoveHistoryEdits : " + j );
//}
//
////  **********  INNER CLASSES TO EditUndoManager  ****************
//
//public class FieldEdit extends AbstractUndoableEdit
//{
//   public static final int GROUP = 0;
//   public static final int PASSWORD = 1;
//   public static final int EXPIRY = 2;
//   public static final int HISTORY_DEL = 3;
//   public static final int HISTORY_MAX = 4;
//   public static final int POLICY = 5;
//   public static final int FIELD_TOPVALUE = 5;
//
//   private int type;
//   private String oldText, newText;
//   private PwsPassphrase oldPass, newPass;
//   private PwsPassphrasePolicy oldPolicy, newPolicy;
//   private String oldHistory, newHistory;
//   private long oldExpireTime, newExpireTime;
//   private int  oldExpireInterval, newExpireInterval;
//   private long oldAccessTime, newAccessTime;
//   private long oldPassModTime, newPassModTime;
//   private long updateTime;
//
//   
//   /**
//    * Creates an undoable edit for the modification of text fields.
//    *  
//    * @param type the edit type (referring to edit field)
//    * @param oldText <code>String</code> or <code>PwsPassphrase</code> before edit
//    * @param newText <code>String</code> or <code>PwsPassphrase</code> after edit
//    */
//   public FieldEdit ( int type, String oldText, String newText )
//   {
//      // parameter control
//      if ( type < 0 | type > FIELD_TOPVALUE | type == PASSWORD )
//         throw new IllegalArgumentException( "illegal edit type" );
//      
//      if (  newText == null )
//         newText = "";
//      if (  oldText == null )
//         oldText = "";
//      
//      // init
//      this.type = type;
//      this.oldText = oldText;
//      this.newText = newText;
//      
//      // record expiry time
//      if ( type == EXPIRY )
//      {
//         oldExpireTime = Util.longFromString( oldText );
//         newExpireTime = record.getPassLifeTime();
//      }
//      
//      updateTime = System.currentTimeMillis();
//      Log.debug( 9, "(AddDialog.EditUndoManager) created new undoable edit, type=TEXTFIELD" );
////      System.out.println( "-- new undoable edit: FIELD type=" + type + ", text=" + newText );
////      System.out.println( "                      old text was: " + oldText );
//   }  // constructor
//
//   
//   
//   /**
//    * Creates an undoable edit for the type PASSWORD (modification of entry password).
//    *  
//    * @param oldPass <code>PwsPassphrase</code> password value before edit
//    * @param oldHistory <code>String</code> password history content value before edit
//    * @param oldExpiry long password expire time before edit
//    */
//   public FieldEdit ( PwsPassphrase oldPass, String oldHistory, 
//         long oldPassModTime, long oldExpiry, long oldAccessTime )
//   {
//      this.type = PASSWORD;
//      this.oldPass = oldPass == null ? null : (PwsPassphrase) oldPass.clone();
//      this.newPass = passFld.getPassphrase();
//      this.oldHistory = oldHistory;
//      this.newHistory = pwHistory.getContentPw3();
//      this.oldPassModTime = oldPassModTime;
//      this.newPassModTime = record.getPassModTime();
//      this.oldExpireTime = oldExpiry;
//      this.newExpireTime = record.getPassLifeTime();
//      this.oldAccessTime = oldAccessTime;
//      this.newAccessTime = record.getAccessTime();
//      
//      updateTime = System.currentTimeMillis();
//      Log.debug( 9, "(AddDialog.EditUndoManager) created new undoable edit, type=PASSWORD" );
//   }  // constructor
//
//   /**
//    * Creates an undoable edit for the modification of the special 
//    * PASSPHRASE_POLICY data set.
//    */
//   public FieldEdit ( PwsPassphrasePolicy oldPolicy )
//   {
//      this.type = POLICY;
//      this.oldPolicy = oldPolicy;
//      this.newPolicy = record.getPassPolicy();
//      
//      updateTime = System.currentTimeMillis();
//      Log.debug( 9, "(AddDialog.EditUndoManager) created new undoable edit, type=POLICY" );
//   }
//   
//   /**
//    * Creates an undoable edit for the modification of the 
//    * PASSLIFETIME field.
//    */
//   public FieldEdit ( long oldExpireTime, int oldInterval )
//   {
//      this.type = EXPIRY;
//      this.oldExpireTime = oldExpireTime;
//      this.oldExpireInterval = oldInterval;
//      this.newExpireTime = record.getPassLifeTime();
//      this.newExpireInterval = record.getExpiryInterval();
//      
//      updateTime = System.currentTimeMillis();
//      Log.debug( 9, "(AddDialog.EditUndoManager) created new undoable edit, type=EXPIRE_TIME" );
//   }  // constructor
//
//
//
//   public long getUpdateTime ()
//   {
//      return updateTime;
//   }
//
//   @Override
//public void die ()
//   {
//      super.die();
////      System.out.println( "-- die UndoableEdit " + toString() );
//   }
//
//   @Override
//public String toString ()
//   {
//      return getPresentationName() + " *** " + super.toString();
//   }
//   
//   @Override
//public String getPresentationName ()
//   {
//      String field;
//      
//      switch ( type )
//      {
//      case GROUP:
//         field = "adddlg.label.group";
//         break;
//      case EXPIRY:
//         field = "adddlg.label.expire";
//         break;
//      case PASSWORD:
//         field = "adddlg.label.password";
//         break;
//      case POLICY:
//         field = "adddlg.label.policy";
//         break;
//      case HISTORY_DEL:
//         return ResourceLoader.getDisplay( "undo.editor.historydel" );
//      case HISTORY_MAX:
//         field = "adddlg.label.history_max";
//         break;
//      default:
//         field = "";
//      }
//      
//      return ResourceLoader.getDisplay( "undo.editor.field" ) + " " +
//             ResourceLoader.getDisplay( field );
//   }  // getPresentationName
//
//   /**
//    * This sets the password editor history with given content where
//    * ACTIVE state is modified to the current editor history active state.
//    * 
//    * @param hist
//    */
//   private void setHistory ( String hist )
//   {
//      hist = (pwHistory.isActive() ? "1" : "0").concat( hist.substring( 1 ) );
//      pwHistory.setContentPw3( hist );
//   }
//   
//   @Override
//public void redo () throws CannotRedoException
//   {
//      super.redo();
//      
//      // action of this instance
//      switch ( type )
//      {
//      case GROUP:
//         actGroup = newText;
//         group.setSelectedItem( newText );
//         break;
//      case HISTORY_DEL:
//      case HISTORY_MAX:
//         pwHistory.setContentPw3( newText );
//         break;
//      case EXPIRY:
//         setPassLifeTime( newExpireTime, newExpireInterval, false );
//         break;
//      case POLICY:
//         setPassPolicy( newPolicy, false );
//         break;
//         
//      case PASSWORD:
//         synchronized ( record )
//         {
//            setPassLifeTime( newExpireTime, -1, false );
//            record.setPassword( newPass );
//            record.setPassModTime( newPassModTime );
//            record.setAccessTime( newAccessTime );
//   
//            passFld.setPassphrase( newPass );
//            setHistory( newHistory );
//            refreshTimeFields();
//            setPassLifeControls();
//         }
//         break;
//      }
//   }
//
//   @Override
//public void undo () throws CannotUndoException
//   {
//      super.undo();
//
//      // action of this instance
//      switch ( type )
//      {
//      case GROUP:
//         actGroup = oldText;
//         group.setSelectedItem( oldText );
//         break;
//      case EXPIRY:
//         setPassLifeTime( oldExpireTime, oldExpireInterval, false );
//         break;
//      case POLICY:
//         setPassPolicy( oldPolicy, false );
//         break;
//      case HISTORY_DEL:
//      case HISTORY_MAX:
//         pwHistory.setContentPw3( oldText );
//         break;
//
//      case PASSWORD:
//         synchronized ( record )
//         {
//            setPassLifeTime( oldExpireTime, -1, false );
//            record.setPassword( oldPass );
//            record.setPassModTime( oldPassModTime );
//            record.setAccessTime( oldAccessTime );
//   
//            passFld.setPassphrase( oldPass );
//            setHistory( oldHistory );
//            refreshTimeFields();
//            setPassLifeControls();
//         }
//         break;
//      }
//   }
//}  // inner class FieldEdit
//
//
//public class RevertEdit extends AbstractUndoableEdit
//{
//   private PwsRecord oldRecord;
//   private long updateTime;
//
//   
//   /**
//    * Creates an undoable edit for the "Revert" function.
//    *  
//    * @param oldRec <code>PwsRecord</code> record value before edit
//    */
//   public RevertEdit ( PwsRecord oldRec )
//   {
//      this.oldRecord = oldRec;
//      
//      updateTime = System.currentTimeMillis();
////      System.out.println( "-- new undoable edit: REVERT" );
//   }  // constructor
//
//   public long getUpdateTime ()
//   {
//      return updateTime;
//   }
//
//   @Override
//public void die ()
//   {
//      super.die();
////      System.out.println( "-- die UndoableEdit " + toString() );
//   }
//
//   @Override
//public String toString ()
//   {
//      return getPresentationName() + " *** " + super.toString();
//   }
//   
//   @Override
//public String getPresentationName ()
//   {
//      return ResourceLoader.getDisplay( "undo.editor.revert" ); 
//   }  // getPresentationName
//
//   
//   @Override
//public void redo () throws CannotRedoException
//   {
//      super.redo();
//      
//      // action
//      revertContent();
//      setupDisplay( initGroup );
//   }
//
//   @Override
//public void undo () throws CannotUndoException
//   {
//      super.undo();
//
//      // action
//      record = (PwsRecord)oldRecord.clone();
//      setupDisplay( null );
//   }
//}  // inner class RevertEdit
//}  // inner class EditUndoManager
//
//
//private class AutoSaveRunner implements Runnable
//{
//   ScheduledFuture<?> handle;
//
//   /** Performs the AddDialog Autosave function.
//    * This runs in the global scheduled tasks until it is terminated by call 
//    * or an exception occurs, in which case it self-terminates. 
//    */
//   @Override
//   public void run () {
//	   
//         if ( objectListener.idleTime() > AUTOSAVE_PERIOD )
//         try {
//            // calculate new CRC over editable data
//            int c = dataCRC(); 
////            Log.debug(10, "(AddDialog.AutoSaveRunner.run) checking record CRC");
//            if ( autoSaveCrc != c ) {
//               PwsRecord rec = getRecord().copy();
//               rec.setGroup( ResourceLoader.getDisplay( "mirrors.editrecord.safe" ) );
//               if ( autoSaveRec != null ) {
//            	   rec.setRecordID(autoSaveRec.getRecordID());
//            	   container.updateRecord(rec);
//               } else {
//            	   container.addRecordRelaxed( rec );
//               }
//               autoSaveRec = rec;
//               autoSaveCrc = c;
//               Log.debug( 5, "(AddDialog.AutoSaveRunner.run) created security copy of EDIT RECORD: "
//                             .concat( rec.toString() ));
//            }
//
//         } catch ( Exception e ) {
//            e.printStackTrace();
//            terminate();
//         }
//   }
//
//   /** Starts this Runnable in the global worker pool for periodic execution.
//    */
//   public synchronized void start () {
//	  if (handle != null) 
//		  throw new IllegalStateException("double start attempt");
//	  
//	  handle = ActionHandler.startTaskPeriodic(this, 10000);
//      Log.debug( 5, "(AddDialog.AutoSaveRunner.start) --- started AutoSave-Runner for EDIT RECORD: "
//            .concat( record.toString() ));
//   }
//
//   public void terminate () {
//      if ( handle!= null && !handle.isCancelled() ) {
//         Log.debug( 5, "(AddDialog.AutoSaveRunner.terminate) --- terminating AutoSave-Runner for EDIT RECORD: "
//               .concat( record.toString() ));
//         handle.cancel(false);
//      }
//   }
//}
//
//
}
