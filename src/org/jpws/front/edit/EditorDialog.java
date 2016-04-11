package org.jpws.front.edit;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
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
import org.jpws.data.UserBreakException;
import org.jpws.front.ActionHandler;
import org.jpws.front.DisplayManager;
import org.jpws.front.GUIService;
import org.jpws.front.Global;
import org.jpws.front.HistoryHandler;
import org.jpws.front.MenuHandler;
import org.jpws.front.ObjectChangeListener;
import org.jpws.front.PolicyDialog;
import org.jpws.front.PwsFileContainer;
import org.jpws.front.Service;
import org.jpws.front.ToolbarHandler;
import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ActivitySource;
import org.jpws.front.util.BlinkingLabel;
import org.jpws.front.util.ButtonBar;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.EditorChangeEvent;
import org.jpws.front.util.EditorChangeEventListener;
import org.jpws.front.util.EditorTextField;
import org.jpws.front.util.NotesTextArea;
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
 *  <b>Super-Reference</b>
 *  <br>The dialog is always owned by the currently active Frame (as obtained
 *  through <code>Global.getActiveFrame()</code>.
 *  The dialog may or may not carry a reference to a file-container as 
 *  the database reference for the editable record. If the reference is given,
 *  the editable record must be new or contained in the container, otherwise
 *  the constructor fails. If the container is set up, some "outside" actions
 *  like auto-save are enabled from within this dialog. 
 * 
 *  <p><b>Data Handling</b>
 *  <br>EditDialog knows 3 PwsRecord variables:
 *  <br>- <i>record</i>	the current editable data volume; a copy from the initial
 *  data record (dialog parameter or new record)			
 *  <br>- <i>initRec</i>	an unmodifiable copy of the initial data record
 *  <br>- <i>autoSaveRec</i>	a copy of the data state which was last saved as 
 *  intermediate save in the container
 */

public class EditorDialog extends ButtonBarDialog implements ActivitySource, ActivityListener {
   public static enum FormatVariant {pws3, pws2, pws1};
   public static final int YES_OPTION = JOptionPane.YES_OPTION;
   public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
   public static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;
   public static final int TEXTFIELDSIZE = 25;
   private static final int DAYSECS = 86400;  // seconds of a day

   private static final Color GENPASSBUTTON_BGD_COLOR = new Color( 0xff, 0xef, 0xd5); // papayawhip
   private static final Color GENPASSBUTTON_BGD_COLOR_NEW = new Color( 0xff, 0xc0, 0xcb); // pink
   /** Time in milliseconds for the period of user inactivity that leads to record autosave (daemon thread). */
   private static final int AUTOSAVE_PERIOD = 30000;  
	
   private EditUndoManager 		undoManager;

   private PwsFileContainer 	container;
   private ActionListener 		externalActions;
   private PwsRecord    		record;
   private PwsRecord    		initRec, autoSaveRec;
   private final Actions    	actions = new Actions();
   private AutoSaveDaemon		autoSaveThread;
   private ObjectChangeListener objectListener = new ObjectChangeListener(this);

   // GUI elements
   private JPanel       		contentPanel;
   private PanelSystem			panelSystem;
//   private DialogButtonBar 		buttonBar;
   private Font         		dataFont, dataFontBold;
   private JLabel				explainLabel;
   private CardLayout 			coverCard;
   private JPanel 				coverPanel;
   private EditorToolBar		toolbar;
   private FloatingToolbar		floatToolbar;
   private Component			recentFocusOwner;
   
   // operation modi
   private final FormatVariant  formatVariant;
   /** Whether this dialog was called to create a new record. */
   private final boolean		isNewRecord;
   /** Whether this dialog references a container for the editable record. */
   private boolean				haveContainer;
   /** Whether the global option to enable password history is set to "true". */
   private boolean				historyAllowed;
   /** Whether user pressed "enable history" button during this editor session. */
   private boolean				historyAllowedByUser;
   /** Whether DEBUG information should be displayed in the dialog. */
   private boolean				isDebug;
   
   // operation states
   private int          		crc, autoSaveCrc;
   private int					terminateCode = CLOSED_OPTION;
   private boolean				haveHistory;
   private boolean				passwordHidden;


	/** Constructor for a modal dialog to create a new database record.
    *  (This will create a new record with new UUID.) After creation of this
    *  dialog, the new record is available at any time through 
    *  <code>getRecord()</code>.
    * 
	* @param owner Frame the parent Frame (not <b>null</b>!)
    * @param group String an initial value for the record field GROUP
    * @param fo FormatVariant determines dialog layout according to file format
	* @throws java.awt.HeadlessException
	*/
	public EditorDialog( PwsFileContainer container, String group, FormatVariant fo ) 
			throws HeadlessException {
	   super( null, DialogButtonBar.BUTTONLESS, true );

 	   this.container = container;
	   record = new PwsRecord();
	   record.setGroup(group);
	   formatVariant = fo;
       isNewRecord = true;
       passwordHidden = false;

       init();
	}  // constructor

   /** Constructor for a dialog to edit a given record. If container is 
    * specified, the given record must be contained. The current edited record 
    * state is available at any time through <code>getRecord()</code>. The
    * parameter record instance is not modified by the dialog.
    * 
    * @param container <code>PwsFileContainer</code> containment of the record, 
    *                  may be <b>null</b>
    * @param rec the <code>PwsRecord</code> to be edited
    * @param fo FormatVariant determines dialog layout according to file format
    * @throws IllegalArgumentException if the record is not contained in 
    *         container
    * @throws NullPointerException if <code>rec</code> is null
    * @throws java.awt.HeadlessException
    */
   public EditorDialog( PwsFileContainer container, PwsRecord rec, FormatVariant fo ) 
		   throws HeadlessException {
      super( null, DialogButtonBar.BUTTONLESS, true );

	  this.container = container;
      record = (PwsRecord)rec.clone();
	  formatVariant = fo;
      isNewRecord = false;
      passwordHidden = !(Options.isOptionSet("openPassEdit") || record.getPassword() == null);

      init();
   }  // constructor

   private void init () {
	   
	  // establish global references
	  haveContainer = container != null; 
	  historyAllowed = Options.isOptionSet("editActiveHistory");
	  setExternalActions(container);
	  if ( haveContainer && !isNewRecord && !container.containsRecord(record) )
		  throw new IllegalArgumentException("record is not element of container");
	  
	  // prepare editable data
	  initRec = (PwsRecord)record.clone();
	  crc = initRec.getCRC();
	  haveHistory = record.getHistoryPws() != null;
      isDebug = Options.isOptionSet( "monitorSystem" );
      Log.log(5, "(EditorDialog.init) have history: " + haveHistory + 
    		  (haveHistory ? ", value=" + record.getHistory() : ""));
	  
	  // create subsystems
      undoManager = new EditUndoManager();

	  // create GUI elements
      contentPanel = create_GUI();
      setDialogPanel( contentPanel );
	  
	  // load data and memorised states
      passwordHidden = !(Options.isOptionSet("openPassEdit") || record.getPassword() == null);
      setCoverButton();
//      setupDisplay(); // ??
	  
	  // establish window features
	  setDialogTitle();
      setAutonomous( true );
      setClipping( false );
      setSynchronous(true);
      
      // optionally restore window bounds from file option memory
      if ( Options.isOptionSet( "rememberScreen" ) ) {
         gainLocation( Options.getOptions(), "entry_editor", true );
      }

      // activity listening (button bar)
      getButtonBar().addButtonBarListener(objectListener);
      
      // start the auto-save daemon thread
      if ( Options.isOptionSet( "useDataMirrors" ) ) {
          autoSaveCrc = crc;
          autoSaveThread = new AutoSaveDaemon();
          autoSaveThread.start();
      }
   }
   
   private JPanel create_GUI () {
      // design preparations
      dataFont = DisplayManager.getFont( "data" );
      dataFontBold = dataFont.deriveFont( Font.BOLD +
            (dataFont.isItalic() ? Font.ITALIC : 0) );

	  JPanel basePanel = new JPanel( new BorderLayout() );

/* Editor display consists of 3 main sections: the NORTH, CENTER and EAST 
 * (BUTTON) panel. The NORTH panel has a toolbar in the NORTH and the explain-
 * text label in the centre. Below the NORTH panel are CENTER and EAST panel.
 * The CENTER holds the record data display section, the EAST panel is a
 * vertical button bar. 
 */
	  // the basic pattern
	  basePanel.add( createTopPanel(), BorderLayout.NORTH );
	  JPanel southPanel = new JPanel( new BorderLayout() );
	  basePanel.add( southPanel, BorderLayout.CENTER );

	  // defining the southern base part
	  southPanel.add( createButtonPanel(), BorderLayout.EAST );
	  southPanel.add( createCenterPanel(), BorderLayout.CENTER );
	  
	  return basePanel;
   }
   
   private JPanel createButtonPanel() {
	   DialogButtonBar bar;
	   JButton button;
	   JPanel panel = new JPanel( new BorderLayout() );

	   // create the action button bar and add to dialog
	   // also create the bar-listener and add to button bar
	   bar = new DialogButtonBar( DialogButtonBar.OK_CANCEL_HELP_BUTTON, 
			                      ButtonBar.VERTICAL, ButtonBar.TOP );
	   bar.setMargin( new Insets( 18, 10, 0, 0 ) );
	   bar.setSynchronous(true);
	   bar.addButtonBarListener(new EditorBarListener());
       panel.add( bar, BorderLayout.NORTH );
       EditorDialog.this.setButtonBar(bar, null);
       
      button = makeButton( "button.revert" );
      bar.add( button );

      // the "cover-button" panel
      coverCard = new CardLayout();
      coverPanel = new JPanel( coverCard );
      button = makeButton( "button.cover" );
      objectListener.registerChangeableObject(button);
      coverPanel.add( button, "button.cover" );
      button = makeButton( "button.uncover" );
      objectListener.registerChangeableObject(button);
      coverPanel.add( button, "button.uncover" );
      bar.add( coverPanel );
      
      button = makeButton( "button.randpassword" );
      button.setBackground( isNewRecord() ? 
            GENPASSBUTTON_BGD_COLOR_NEW : GENPASSBUTTON_BGD_COLOR );
      bar.add( button );
      
      button = makeButton( "button.extrapassword" );
      bar.add( button );
      
	  return panel;
}

private Component createCenterPanel () {
	  // ---------- TEST -------------
	  
//	  JCheckBox modifiedChk= new JCheckBox( "data modified" );
//	  modifiedChk.addActionListener(new ActionListener() {
//		  
//		@Override
//		public void actionPerformed(ActionEvent evt) {
//			JCheckBox chkBox = (JCheckBox)evt.getSource();
//			if ( chkBox.isSelected() ) {
//				String text = record.getNotes() == null ? "" : record.getNotes(); 
//				record.setNotes(text.concat("XUD"));
//				Log.log(10, "(EditorDialog) -- record modified (TEST)");
//			}
//		}
//	  });
	  
	
    // defining the work panel system
    panelSystem = new PanelSystem();
//  JPanel panel = new JPanel( new BorderLayout() );
//    panel.add(panelSystem.getView(), BorderLayout.CENTER);

    String name;
    name = ResourceLoader.getDisplay("pane.edit.front");
    panelSystem.activatePanel(PanelType.LOGIN, name, 0);
    name = ResourceLoader.getDisplay("pane.edit.stats");
    panelSystem.activatePanel(PanelType.TIMES, name, 1);
    handleHistory();

    // optional: NOTES on separate work-panel
    if ( Options.isOptionSet( "editFullNotes" ) ) {
        name = ResourceLoader.getDisplay("pane.edit.notes");
        panelSystem.activatePanel(PanelType.NOTES, name, 1);
    }
    
    // call LOGIN panel to display
    panelSystem.focusPanelType(PanelType.LOGIN);
	return panelSystem.getView();
}

/** Sets the title of the dialog window. 
    */
   private void setDialogTitle () {
	   String token = isNewRecord ? "adddlg.title" : "edidlg.title";
	   String recT = record.getTitle();
	   String hstr = ResourceLoader.getDisplay(token).concat(
			         recT == null ? "" : " - ".concat(recT));
	   setTitle(hstr);
   }
   
   /** The file-container referenced by this edit dialog or <b>null</b> if no
    * such reference is set up.
    * 
    * @return <code>PwsFileContainer</code> or <b>null</b>
    */
   public PwsFileContainer getFileContainer () {
	   return container;
   }
   
   /** Whether the edited record is a new record (not contained in database).
    * 
    *  @return boolean <b>true</b> == new record
    */
   public boolean isNewRecord () {
      return isNewRecord;
   }
   
   /** Whether the current data state of the editable record is different
    * to its initial state. 
    * 
    * @return boolean <b>true</b> == record is modified compared to its
    *         initial state
    */
   public boolean isRecordModified () {
      // measures to ignore the modify time
      PwsRecord rec = (PwsRecord)getRecord().clone();
      rec.setModifyTime( initRec.getModifiedTime() );
      
      return crc != rec.getCRC();
   }
   
   /**
    * Returns the initial record state of the edit dialog.
    * 
    * @return PwsRecord
    */
   public PwsRecord getInitRecord () {
      return initRec;
   }
   
   /** Returns the record reflecting the modifications having occurred during 
    *  the dialog edit session.
    *  
    * @return <code>PwsRecord</code> the current edited record state 
    *         of this dialog
    */ 
   public PwsRecord getRecord () {
	   // update from current data panel + return record
	   panelSystem.write();
	   return record;
   }

   /** Returns the format variant active on this dialog.
    * 
    * @return <code>FormatVariant</code>
    */
   public FormatVariant getFormatVariant () {
	  return formatVariant;
   }
   
   /** Disposes this dialog and makes it unusable (destruction). */
   @Override
   public void dispose ()
   {
      // terminate auto-save thread and delete save-record entry
      if ( autoSaveThread != null ) {
         autoSaveThread.terminate();
         autoSaveThread = null;
      }
      if ( autoSaveRec != null ) {
         container.deleteRecord( autoSaveRec );
         autoSaveRec = null;
      }

      // optionally remember window bounds
      if ( isShowing() && Options.isOptionSet( "rememberScreen" ) ) {
         storeBounds( Options.getOptions(), "entry_editor", true );
         if ( floatToolbar != null ) {
            floatToolbar.storeBounds( getFloatingToolbarOptions(), "dialogFloatingBarBounds", true );
         }
      }

      // tear down GUI
      super.dispose();
      toolbar.exit();
      getContentPane().removeAll();
      Log.log(6, "(EditorDialog.dispose) *** Record Dialog Disposed ***" );
   }
   
   /** Returns the option bag for the floating toolbar's concern. 
    * @return PersistentOptions
    */
   private PersistentOptions getFloatingToolbarOptions () {
	   return Options.getOptions();
//      return container.getMinorOptions();
   }
   
   @Override
   public void processWindowEvent( WindowEvent e )
   {
      switch ( e.getID() ) {
      case WindowEvent.WINDOW_OPENED :
//       System.out.println( "*** Dialog Window Opened ***" );
         autorun();
         break;
      }
      
      super.processWindowEvent( e );
   } 
   
   /** Creates and runs a EditorDialog to edit the given record on the EDT.
    * Returns the dialog instance after editing is finished.
    *   
    * @param container <code>PwsFileContainer</code>
    * @param record <PwsRecord</code>
    * @return <code>EditorDialog</code>
    */
   public static EditorDialog editRecord ( final PwsFileContainer container, 
		                                   final PwsRecord record, 
		                                   final FormatVariant fo  ) {
	   class EditorRunnable implements Runnable {
		   EditorDialog dlg;
		   
		   @Override
		   public void run() {
			  dlg = new EditorDialog(container, record, fo);
			  dlg.show();
		   }
	   }

	   EditorRunnable r = new EditorRunnable();
	   try {
		  ActionHandler.executeOnEDT_Wait(r);
	   } catch (InterruptedException e) {
		  e.printStackTrace();
	   } catch (InvocationTargetException e) {
		  e.printStackTrace();
	   }
	   return r.dlg;
   }
   
   /** Returns the user action that caused this dialog to terminate.
    *  This can be YES_OPTION or CANCEL_OPTION if the user interacted with
    *  the dialog, or CLOSED_OPTION if the window terminated by some 
    *  interruption.
    * 
    * @return int dialog end code
    */
   public int getTerminateCode () {
	  terminateCode = isOkPressed() ? YES_OPTION : isCancelPressed() ? CANCEL_OPTION : CLOSED_OPTION; 
      return terminateCode;
   }

   public void setExternalActions (  ActionListener actions ) {
	   externalActions = actions;
   }

   public void setExplainText ( String text ) {
	   explainLabel.setText(text);
   }
   
   /** Handles availability of the history object and organises display
    * of history work panel and "off"-button during an editor session.
    */
   public void handleHistory () {
	   handleHistory(null);
   }
   
   /** Handles availability of the history object and organises display
    * of history work panel and "off"-button during an editor session.
    * It also adds a new password history entry if this is possible according
    * to situation and the parameter is supplied.
    * 
    * @param evt <code>PasswordHistoryEvent</code> new password history entry, 
    *            may be <b>null</b> for no event
    */
   public void handleHistory ( PasswordHistoryEvent evt ) {
	   Log.log(7, "(EditorDialog.handleHistory) - enter -");
	   String value = record.getHistory(); 
	   String defaultValue = HistoryObject.defaultValue();
       haveHistory = value != null;
       boolean eventOccurred = evt != null;
       
	   // organise removal of demise history
	   if ( haveHistory & !eventOccurred ) {
		   // remove history from record if it assumes the default value
		   if ( value.equals(defaultValue) ) {
			   record.setHistory((String)null);
			   
			   // update
			   haveHistory = false;
			   Log.debug(7, "(EditorDialog.handleHistory) removed history from record: ".concat(value));
		   }
	   }

	   // organise password entry into history (may be dropped)
	   if ( eventOccurred ) { 
		   // organise adding of a blank history to record
		   if ( !haveHistory & (historyAllowed | historyAllowedByUser) ) {
			  // add blank history
			  record.setHistory(defaultValue);
			   
			  // update
			  haveHistory = true;
			  Log.debug(7, "(EditorDialog.handleHistory) added default history to record");
		   }

		   // if we have a history + it is active, we add the password entry
		   HistoryObject hist = new HistoryObject(record.getHistory());
		   if ( haveHistory && hist.isActivated() ) {
			  hist.addValue(evt.oldTime, evt.oldPass);
			  record.setHistory( hist.getStringValue() );
			  Log.debug(7, "(EditorDialog.handleHistory) added password entry: " + evt.oldPass.getString());
		   }

		  // update work panels
		  panelSystem.notifyFieldValue("HISTORY.UPDATE", null, null);
	   }
	   
	   int histSize = 0;

	   // DISPLAY HISTORY
	   if ( haveHistory ) {
		   HistoryObject hist = new HistoryObject(record.getHistory());
		   histSize = hist.size();
		   if ( histSize > 0 ) {
			  EditorWorkPanel panel = panelSystem.getFirstPanel(PanelType.HISTORY);
			  if ( panel == null ) {
				  String name = ResourceLoader.getDisplay("pane.edit.history");
				  panel = panelSystem.activatePanel(PanelType.HISTORY, name, 2);
			  }

			  // DISPLAY ENABLED STATE
			  panel.setEnabled(hist.isActivated());
		   }
	   }
	   
	   // REMOVE DISPLAY
	   if ( !haveHistory || (histSize == 0 & !eventOccurred) )  {
		   panelSystem.removePanelType(PanelType.HISTORY);
	   }
	   
	   // organise display of "history" button in toolbar
	   toolbar.update();
	   Log.log(7, "(EditorDialog.handleHistory) - done -");
   }
   
   
   
/** Top-panel consists of a toolbar in NORTH and a explanatory text label 
 * in CENTER. 
*/
private JPanel createTopPanel()
{
  // create panel
  JPanel panel = new JPanel( new BorderLayout() );
  toolbar = new EditorToolBar();
  panel.add( toolbar, BorderLayout.NORTH );

  // create explain label
  explainLabel = new JLabel();
  panel.add( explainLabel, BorderLayout.CENTER );
  explainLabel.setBorder( new EmptyBorder( 16, 16, 10, 16 ) );
  Font font = explainLabel.getFont().deriveFont( Font.PLAIN );
  explainLabel.setFont( font );

  return panel;
}

/** Creates a button for the button bar.
 * 
 * @param token String button + command name
 * @return
 */
private JButton makeButton ( String token ) {
   JButton button = new JButton( ResourceLoader.getDisplay(token) );
   button.setActionCommand( token );
   button.addActionListener( actions );
   button.setName( token );
   return button;
}

private void setCoverButton () {
   String token = passwordHidden ? "button.uncover" : "button.cover";
   coverCard.show( coverPanel, token );
}

/** Creates a button in the style of the global ToolbarHandler.
 * 
 * @param imageName String
 * @param buttonName String 
 * @param command String action command
 * @return
 */
private JButton makeButton ( String imageName, String buttonName, String command ) {
   JButton b = ToolbarHandler.makeButton( imageName, buttonName, true );
   b.setName( buttonName );
   b.setActionCommand( command );
   b.removeActionListener( Global.mainActionListener );
   b.addActionListener( actions );
   return b;
}

/** Reverts the content of the editable record to the initial state of the
 * editor session. Includes all subsystem and display updates. 
 */
private void revertContent () {
	 record = (PwsRecord)initRec.clone(); // interim
     haveHistory = record.getHistory() != null;
//     handleHistory();
}

/** Loads the record data display with values from the editable record.
 * This will overwrite unsaved values in the work panels!
 */
private void setupDisplay () {
	panelSystem.read();
	toolbar.update();
	handleHistory();
}

/** Sets the record value PASSPOLICY with the parameter
 * value and depicts the toolbar policy icon according to
 * whether the policy is local or not. This transaction 
 * optionally is undoable. 
 * 
 * @param policy <code>PwsPassphrasePolicy</code>
 */
private void setPassPolicy ( PwsPassphrasePolicy policy, boolean undoable )
{
   PwsPassphrasePolicy oldPolicy, superPolicy;
   UndoableEdit edit;
   boolean isLocalPolicy;
   
   oldPolicy = record.getPassPolicy();
   superPolicy = container.getPassphrasePolicy();
   isLocalPolicy = !superPolicy.equals( policy );
   try { 
      record.setPassPolicy( isLocalPolicy ? policy : null );
      // set the dialog's policy to records's policy
      // and if this is null then policy equals to global policy
      toolbar.update();

      // create undoable event
      if ( undoable ) {
         Log.debug( 7, "(EditorDialog.setPassLifeTime) creating UNDOABLE EDIT for Policy Edit" );
         edit = undoManager.new FieldEdit( oldPolicy );
         undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
      }

   } catch ( InvalidPassphrasePolicy e ) {
      GUIService.failureMessage( null, e );
   }
}

private void setPassword ( PwsPassphrase pass, EditorWorkPanel source ) {
   PwsRecord oldRec = (PwsRecord)record.clone();
   if ( pass == null ) {
	   pass = new PwsPassphrase();
   }
   
   // set the new password value + mod time
   record.setPassword(pass);
   record.passwordUpdated();
   panelSystem.notifyFieldValue("LOGIN.PASSWORD", pass, source);
   Log.debug( 7, "(EditorDialog.setPassword) setting record password to: " + pass );

   // update a password expire time
   int interval = record.getExpiryInterval();
   long lifeTime = record.getPassLifeTime();
   if ( lifeTime > 0 & interval > 0 ) {
	   lifeTime = record.getPassModTime() + (long)interval * DAYSECS * 1000;
	   record.setPassLifeTime( lifeTime );
	   panelSystem.notifyFieldValue("TIMES.UPDATE", null, null);
	   String hstr = Global.getLocalDate(lifeTime);
	   Log.debug( 7, "(EditorDialog.setPassword) updating password EXPIRE TIME to: ".concat(hstr) );
	   
	   String text = "<html>The password expire time has been updated to <br><font color=\"blue\">".
			   		concat(hstr);
	   GUIService.infoMessage(this, null, text);
   }
   
   // update password history
   PwsPassphrase oldPass = oldRec.getPassword();
   if ( oldPass != null && !oldPass.equals(pass) ) {
	  Log.debug( 7, "(EditorDialog.setPassword) creating password HISTORY entry: ".concat(oldPass.getString()) );
	  long oldTime = oldRec.getPassModTime();
	  handleHistory(new PasswordHistoryEvent(this, oldPass, oldTime));
   }

   // create undable edit
   Log.debug( 7, "(EditorDialog.setPassword) creating UNDOABLE EDIT for Set Password" );
   UndoableEdit edit = undoManager.new FieldEdit( oldRec );
   undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
}

private void setPasswordHidden ( boolean value ) {
   if ( value != passwordHidden ) {
      passwordHidden = value;
      setCoverButton();
      panelSystem.notifyFieldValue("LOGIN.PASSWORD_COVER", null, null);
      
      if ( !value ) {
    	 panelSystem.focusPanelType(PanelType.LOGIN);
      }
   }
}

/** Returns the currently active passphrase creation policy for this editor.
 * The returned value is a clone.
 * 
 * @return <code>PwsPassphrasePolicy</code>
 */
private PwsPassphrasePolicy getPassPolicy () {
   PwsPassphrasePolicy policy = record.getPassPolicy(); 
   if ( policy == null ) {
	   policy = container.getPassphrasePolicy();
   }
   return policy;
}

/** Sets default values for time fields if they are currently left void. */
private void correctRecord () {
   long defaultTime = Math.max(record.getCreateTime(), record.getPassModTime());
   if ( defaultTime == 0 ) {
      defaultTime = record.getModifiedTime();
   }

   boolean mod = false;
   if ( record.getModifiedTime() == 0 ) {
      record.setModifyTime( defaultTime );
      mod = true;
   }
   if ( record.getPassModTime() == 0 ) {
      record.setPassModTime( defaultTime );
      mod = true;
   }
   if ( record.getAccessTime() == 0 ) {
      record.setAccessTime( defaultTime );
      mod = true;
   }
   if ( mod ) {
  	 panelSystem.notifyFieldValue("TIMES.UPDATE", null, null);
   }
}

/** Auto-run actions. */
private void autorun () {
   if ( getFloatingToolbarOptions().isOptionSet( "editor-floatingbar-active" ) )
      actions.callAction( "button.editfloatbar" );
}

@Override
public void addActivityListener ( ActivityListener listener ) {
   objectListener.addActivityListener( listener );
}

@Override
public void removeActivityListener ( ActivityListener listener ) {
   objectListener.removeActivityListener( listener );
}

@Override
public void actionOccurred ( ChangeEvent evt ) {
   objectListener.actionOccurred( evt );
}

// ----------------- INNER CLASSES ----------------

private class Actions implements Runnable, ActionListener
//, FocusListener, ChangeListener, ItemListener, KeyListener
{
   //  ***********  INNER CLASSES  *************
	
   String command;
   @SuppressWarnings("unused")
   Object source;
   
   public Actions ()
   {}
   
   private Actions ( String cmd ) {
      command = cmd;
   }
   

   /** Synchronously performs the specified action in the same thread. 
    *  
    * @param cmd String action command
    */
   public void callAction ( String cmd ) {
      new Actions( cmd ).run();
   }
   
	@Override
	public void actionPerformed( ActionEvent event ) {
       command = event.getActionCommand();
       source = event.getSource();
       run();
	}

	@Override
	public void run() {
       UndoableEdit edit;
       String hstr;
       boolean ok;
      
       Log.log(8, "(EditorDialog.Actions.run) starting actions interpreter, CMD=".concat(command));
      
       // trigger user activity / update the global action time value
//       objectListener.activity();
       
       if ( command.equals( "button.cover" ) ) {
          setPasswordHidden( true );
       }
       
       else if ( command.equals( "button.uncover" ) ) {
          setPasswordHidden( false );
       }
       
       else if ( command.equals( "button.randpassword" ) ) {
    	  PwsPassphrasePolicy policy = getPassPolicy(); 
          PwsPassphrasePolicy policyOld = (PwsPassphrasePolicy)policy.clone();
          PwsPassphrase pass = GUIService.generateRandomPassphrase( EditorDialog.this, null, policy );
          
          // if passphrase-policy has been edited
          if ( !policyOld.equals( policy ) ) {
              setPassPolicy( policy, true );
          }
          
          // if user created new password
          if ( pass != null ) {
        	  setPassword(pass, null);
          }
       }

       else if ( command.equals( "button.extrapassword" ) ) {
     	  PwsPassphrasePolicy policy = getPassPolicy(); 
          PwsPassphrasePolicy policyOld = (PwsPassphrasePolicy)policy.clone();
          Service.generatePassword( EditorDialog.this, policy, true );

          // if passphrase-policy has been edited
          if ( !policyOld.equals( policy ) ) {
             setPassPolicy( policy, true );
          }
       }
       
       else if ( command.equals( "button.revert" ) ) {
          if ( isRecordModified() ) {
             // check user confirm if record changed
             ok = GUIService.userConfirm( EditorDialog.this, "msg.ask.revertrecord" );
             
             if ( ok ) {
                // perform revert content
            	PwsRecord rec = (PwsRecord)getRecord().clone();
                revertContent();
                setupDisplay();
             
                // create undoable edit event
                edit = undoManager.new RevertEdit( rec );
                undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
             }
          }
       }
       
       else if ( command.equals( "menu.edit.policy" ) ) {
          final PolicyDialog dlg;
          final PwsPassphrasePolicy oldPolicy;
          
          oldPolicy = getPassPolicy();
          dlg = new PolicyDialog( EditorDialog.this, getPassPolicy(), 1 );
          dlg.setAutonomous( true );
          dlg.setSynchronous( true );
          dlg.moveRelatedTo( EditorDialog.this );
          dlg.addButtonBarListener( new DefaultButtonBarListener() {
             @Override
             public boolean okButtonPerformed () {
            	PwsPassphrasePolicy policy = dlg.getEditedPolicy();
                if ( policy.isValid() && !policy.equals( oldPolicy ) ) {
                   setPassPolicy( policy, true );
                }
                dlg.dispose();
                return true;
             }
          });
          dlg.setVisible( true );
       }
       
       else if ( command.equals( "menu.edit.copyuser" ) ) {
          ActionHandler.sendClipboardUsername( EditorDialog.this, getRecord() );
       }
       
       else if ( command.equals( "menu.edit.copypass" ) ) {
     	  boolean update = container != null && 
     			  (container.isModified() || Options.isOptionSet("storeMinorChanges"));
     	  ok = ActionHandler.sendClipboardPassword(EditorDialog.this, getRecord(), update); 
          if (ok & container != null) {
             container.recordUsed( record );
//             refreshTimeFields();
          }
       }
       
       else if ( command.equals( "button.starturl" ) ) {
           URL url ; 
          
           // if record has no URL value AND there is a valid URL in the clipboard
          // copy it into the URL field
          if ( (hstr = getRecord().getUrl()) == null || hstr.trim().isEmpty() ) {
             hstr = Global.getClipboardText();
             try { 
                new URL( hstr );
                record.setUrl( hstr );
                panelSystem.notifyFieldValue("LOGIN.URL", hstr, null);
             }
             catch ( MalformedURLException e )
             {}
          }

          // otherwise, if we have a valid URL string, start it in browser
          else if ( (url = Util.extractURL(hstr)) != null ) {
             Global.startBrowser( url );
             if ( haveContainer && Options.isOptionSet( "useEntryOnBrowse" ) ) {
                container.recordUsed( record );
             }
             if ( Options.isOptionSet( "autoCopyPass" ) ) {
                callAction( "menu.edit.copypass" );
             }

          // info message operation failure   
          } else {
             GUIService.infoMessage( EditorDialog.this, "dlg.operfailure", "msg.url.failurl" );
          }
       }
       
       else if ( command.equals( "menu.edit.favourite" ) ) {
          boolean isFavourite = !container.isRecordFavourite(record);
          Log.debug(7, "(EditorDialog.Actions.run) switching to record FAVOURITE state = " + isFavourite);
          
          // modify "Favourite" status in file-container
          container.setRecordFavourite( initRec, isFavourite );
          
          // adjust icon display
          toolbar.updateFavouriteState();
       }
       
       else if ( command.equals( "button.editfloatbar" ) ) {
          PersistentOptions options = getFloatingToolbarOptions();

          if ( floatToolbar == null ) {
             floatToolbar = new FloatingToolbar();
             floatToolbar.gainLocation( options, "dialogFloatingBarBounds", true );
             floatToolbar.show();
             options.setOption( "editor-floatingbar-active", "true" );
             
             // set initial position of floating toolbar in case there is no position memory
             if ( options.getBounds( "dialogFloatingBarBounds" ) == null ) {
                floatToolbar.setCorrectedLocation( new Point(450, 250) );
             }

          } else {
        	 boolean visible = !floatToolbar.isVisible();
             floatToolbar.setVisible( visible );
             options.setOption( "editor-floatingbar-active", visible );
          }
       }
       
       else if ( command.equals( "menu.edit.undo" ) ) {
          Log.log(8, "-- UNDO: ".concat(undoManager.getPresentationName()) ); 
          undoManager.undo();
       }
       
       
       else if ( command.equals( "menu.edit.redo" ) ) {
          Log.log(8, "-- REDO: ".concat(undoManager.getRedoPresentationName()) ); 
          undoManager.redo();
       }
       
       // toolbar icon for unavailable history
       else if ( command.equals( "button.historyicon" ) ) {
		   record.setHistory(HistoryObject.defaultValue());
    	   historyAllowedByUser = true;
    	   handleHistory();
       }
       
       // after work
       else {
           Log.log( 8, "(EditorDialog.Actions.run) -- unknown action command: ".concat(command) );
       }
       Log.log( 8, "(EditorDialog.Actions.run) -- finished action handling" );
	}
}

private class EditorToolBar extends JToolBar {
	JButton undoIcon, redoIcon;
	JButton copyPassIcon, copyUserIcon;
	JButton webstartIcon;
	JButton policyIcon;
	JButton favouriteIcon;
	JButton historyIcon;
	Icon favIcon1, favIcon2;
	Icon expiredIcon, expireSoonIcon;
	BlinkingLabel expiryIcon;
	
	public EditorToolBar () {
		init();
	}
	
	private void init () {
	   JButton button;	
	   JToolBar toolBar = this;
	   toolBar.setFloatable( false );

 	   // copy password icon
	   copyPassIcon = makeButton( "copypass", "toolbar.copypass", "menu.edit.copypass" );
	   objectListener.registerChangeableObject(copyPassIcon);
	   toolBar.add( copyPassIcon );

	   // copy username icon
	   copyUserIcon = makeButton( "copyuser", "toolbar.copyuser", "menu.edit.copyuser" );
	   objectListener.registerChangeableObject(copyUserIcon);
	   toolBar.add( copyUserIcon );

	   // clear clipboard icon
	   button = ToolbarHandler.makeButton( ActionHandler.getAction( "CLEARCLIP" ) );
	   objectListener.registerChangeableObject(button);
	   toolBar.add( button );

	   undoIcon = makeButton( "undo", "toolbar.entry.undo", "menu.edit.undo" );
	   undoIcon.setVisible( false );
	   objectListener.registerChangeableObject(undoIcon);
	   toolBar.add( undoIcon );

	   redoIcon = makeButton( "redo", "toolbar.entry.redo", "menu.edit.redo" );
	   redoIcon.setVisible( false );
	   objectListener.registerChangeableObject(redoIcon);
	   toolBar.add( redoIcon );

	   toolBar.addSeparator( new Dimension( 10, 0 ) );

	   webstartIcon = makeButton( "webstarter", "toolbar.webstart.record", "button.starturl" );
	   objectListener.registerChangeableObject(webstartIcon);
	   toolBar.add( webstartIcon );
	
	   if ( haveContainer && Options.isOptionSet( "useFavourites" ) ) {
		  favouriteIcon = makeButton( "favourite", "toolbar.isfavourite", "menu.edit.favourite" );
		  favIcon2 = favouriteIcon.getIcon();
		  favIcon1 = ResourceLoader.getImageIcon( "filter-favourites" );
		  updateFavouriteState();
		  objectListener.registerChangeableObject(favouriteIcon);
		  toolBar.add( favouriteIcon );
	   }

	   policyIcon = makeButton( "policy", "toolbar.recpolicy", "menu.edit.policy" );
	   objectListener.registerChangeableObject(policyIcon);
	   toolBar.add( policyIcon );

	   button = makeButton( "editfloatbar-toggle", "toolbar.floatbar", "button.editfloatbar" );
	   objectListener.registerChangeableObject(button);
	   toolBar.add( button );

	   historyIcon = makeButton( "editor-history", "toolbar.history", "button.historyicon" );
	   objectListener.registerChangeableObject(historyIcon);
	   toolBar.add( historyIcon );

	   toolBar.addSeparator( new Dimension( 10, 0 ) );

	   // state bullet IMPORT status
	   int status = record.getImportStatus();
	   if ( status == PwsRecord.IMPORTED | status == PwsRecord.IMPORTED_CONFLICT ) {
	      String hstr = status == PwsRecord.IMPORTED ? "editor-imported" : "editor-importedconflict"; 
	      Icon icon =  ResourceLoader.getImageIcon( hstr );
	      JLabel label = new JLabel( icon );
	      label.setToolTipText( ResourceLoader.getDisplay( "tooltip.imported" ));
	      toolBar.add( label );
	      toolBar.addSeparator( new Dimension( 3, 0 ) );
	   }

	   // state bullet EXPIRY
	   expiredIcon = ResourceLoader.getImageIcon( "editor-expired" );
	   expireSoonIcon = ResourceLoader.getImageIcon( "editor-expiresoon" );
	   expiryIcon = new BlinkingLabel();
	   expiryIcon.setPreferredSize( new Dimension( 20, 20 ) );
	   Global.addTimePulseListener( expiryIcon );
	   toolBar.add( expiryIcon );
	   
	   update();
	} // init
	
	public void exit () {
	   Global.removeTimePulseListener( expiryIcon );
	}
	
	/** Causes this toolbar to update appearance of its icons according to
	 * current record states.  
	 */
	public void update () {
		copyPassIcon.setEnabled( record.getPassword() != null );
		copyUserIcon.setEnabled( record.getUsernamePws() != null );
		policyIcon.setVisible( record.getPassPolicy() != null );
		
		HistoryObject hist = new HistoryObject(record.getHistory());
		boolean showIcon = !(haveHistory | historyAllowed | historyAllowedByUser)
				           || hist.maxEntries() == 0;
		historyIcon.setVisible( showIcon ); 
		updateUrl();
		updateExpireStates();
	}
	
	public void updateUrl () {
		webstartIcon.setEnabled( Util.extractURL( record.getUrl() ) != null );
	}
	
	public void updateFavouriteState () {
	   if ( favouriteIcon != null ) {
		   boolean fav = container.isRecordFavourite(initRec);
	       favouriteIcon.setIcon( fav ? favIcon2 : favIcon1 );
	       favouriteIcon.setRolloverIcon( fav ? favIcon2 : favIcon1 );
	       favouriteIcon.setToolTipText( ResourceLoader.getCommand( fav ? 
	             "toolbar.isfavourite.tooltip" : "toolbar.isnofavourite.tooltip" ));
	   }
	}

	public void updateExpireStates () {
	   // update EXPIRE condition	
	   long date = System.currentTimeMillis() + (haveContainer ? container.getExpireScope() 
			   : Options.getLongOption( "expireScope" ));
	   boolean check = record.willExpire( date );
	   Icon icon = null;
	   String hstr = null;
	   
	   if ( record.hasExpired() ) {
		  icon = expiredIcon;
		  hstr = "tooltip.expired";
	   } else if ( check ) {
		  icon = expireSoonIcon;
		  hstr = "tooltip.expiresoon";
	   }
       expiryIcon.setBlinking( check );
	   expiryIcon.setIcon( icon );
	   expiryIcon.setToolTipText(check ? ResourceLoader.getDisplay(hstr) : null);
	}
	
	public void updateUndo () {
	   boolean hasUndo = undoManager.canUndo();
	   boolean hasRedo = undoManager.canRedo();
	   String hstr;
	   
	   // UNDO item settings
	   if ( hasUndo ) {
	      hstr = //ResourceLoader.getCommand( "menu.edit.undo" ) + " : " +
	             undoManager.getUndoPresentationName();
	      undoIcon.setToolTipText( hstr );
	      undoIcon.setVisible( true );
	   }
	   undoIcon.setEnabled( hasUndo );
	   
	   // REDO item settings
	   if ( hasRedo ) {
	      hstr = //ResourceLoader.getCommand( "menu.edit.redo" ) + " : " +
	             undoManager.getRedoPresentationName();
	      redoIcon.setToolTipText( hstr );
	      redoIcon.setVisible( true );
	   }
	   redoIcon.setEnabled( hasRedo );
	}  // undoUpdated
}

private class EditorBarListener extends DefaultButtonBarListener {

	@Override
	public boolean okButtonPerformed() {
		Log.log(10, "(EditorDialog.EditorBarListener) OK performed");
        PwsRecord rec = getRecord();
        String title = rec.getTitle();

        // test for close-ability (vetoes from the edit panels)
        boolean ok = panelSystem.terminate();
        
        if ( ok ) {
	        // edit record validity check (don't allow invalid record saved)
	        boolean titleNull = title == null;
	        if ( titleNull || rec.getPassword() == null ) {
	           // reaction to invalid : display error message, ref TITLE or PASSWORD
	           String hstr2 = titleNull ? "adddlg.label.title" : "adddlg.label.password";
	           String hstr = ResourceLoader.getDisplay( "msg.badfieldvalue" );
	           hstr = Util.substituteText(hstr, "$field", ResourceLoader.getDisplay(hstr2));
	           GUIService.infoMessage(EditorDialog.this, "dlg.badrecord", hstr);
	           ok = false;
	        }
	        // request user confirm if password has changed (optional action)
	        else if ( !isNewRecord && !rec.getPassword().equals(initRec.getPassword()) && 
	              Options.isOptionSet( "confirmUpdatePass" )) {
	           ok = GUIService.userConfirm( EditorDialog.this, "msg.ask.passmodify" );
	        }
        }

        // if data saving confirmed
        if ( ok ) {
           // terminate subsidiary tasks
           panelSystem.exit();
           // TODO terminate subsystems / notify work panels
           
           // if valid: call external action event handler
           if ( externalActions != null ) {
              // if there was no effective data change, reset modify time
              if ( !isRecordModified() ) {
                 record.setModifyTime( initRec.getModifiedTime() );
              } else {
                 correctRecord();
              }
              
              // issue event for external action
  			  Log.log(10, "() starting EXTERNAL actions - OK-Button");
  			  ActionEvent evt = new ActionEvent(EditorDialog.this, 0, "dialog.action.ok");
  			  externalActions.actionPerformed(evt);
           }

           // terminate the dialog
           terminateCode = YES_OPTION;
           dispose();
        }
        else if ( recentFocusOwner != null ) {
           recentFocusOwner.requestFocus();
        }
		return true;
	}
	
	@Override
	public void cancelButtonPerformed() {
		Log.log(10, "(EditorDialog.EditorBarListener) CANCEL performed");

		// probes panel system with edit termination
		boolean dataModified = false;
		boolean ok = panelSystem.terminate();

		// checks user confirm if CANCEL occurs on modified data
		if ( ok ) {
	       dataModified = isRecordModified();
	        
	       // check user confirm if record changed
	       if ( dataModified ) {
	          // test for close-ability (vetoes from the edit panels)
	          ok = GUIService.userConfirm(EditorDialog.this, "msg.ask.cancelrecord");
	       }
		}
           
        // if cancel confirmed
        if ( ok ) {
           // terminate subsidiary tasks
           panelSystem.exit();
    	   // TODO terminate subsystems
    		
           // call external action event handler
           if ( externalActions != null ) {
              // if there was no user change, reset modify time
              if ( !dataModified ) {
                 record.setModifyTime( initRec.getModifiedTime() );
              }
              
              // issue event for external action
  			  Log.log(10, "(EditorDialog.EditorBarListener) starting EXTERNAL actions - CANCEL-Button");
  			  ActionEvent evt = new ActionEvent(EditorDialog.this, 0, "dialog.action.cancel");
  			  externalActions.actionPerformed(evt);
           }

           terminateCode = CANCEL_OPTION;
           dispose();
        }

        // focus reset if possible
        else if ( recentFocusOwner != null && recentFocusOwner.isShowing() )
           recentFocusOwner.requestFocus();
	}

	@Override
	public void helpButtonPerformed() {
        GUIService.toggleHelpDialog( EditorDialog.this, "dlg.help.entryeditor" );
        if ( recentFocusOwner != null ) {
           recentFocusOwner.requestFocus();
        }
	}
	
}

private static final int DEFAULT_HISTORY_SIZE = 16;

private static class HistoryObject {
	String history;
	boolean activated;
	int maxEntries;
	int size;
	
	/** Creates an empty, active history object with the default value for 
	 *  maximum size.
	 */
	HistoryObject () {
		String hstr = Util.bytesToHex( new byte[] {(byte)DEFAULT_HISTORY_SIZE});
		history = "1" + hstr + "00";
		activated = true;
		maxEntries = DEFAULT_HISTORY_SIZE;
//		Log.debug(10, "(EditorDialog.HistoryObject) created default empty history object: "
//				.concat(getStringValue()));
	}
	
	/** Creates a new history object from its string representation.
	 * 
	 * @param content String, may be null for the default object
	 * @throws IllegalArgumentException
	 * @throws NumberFormatException
	 */
	HistoryObject ( String content ) {
		if ( content == null ) {
			content = defaultValue();
		}
		if ( content.length() < 5 )
			throw new IllegalArgumentException("invalid history value: ".concat(content));
		
		history = content;
		activated = !content.isEmpty() & content.charAt( 0 ) == '1';
		maxEntries = Integer.valueOf( content.substring( 1, 3 ), 16 ).intValue(); 
		size = Integer.valueOf( content.substring( 3, 5 ), 16 ).intValue();
		Log.debug(10, "(EditorDialog.HistoryObject) created history object from string: "
				.concat(content));
	}

	/** Returns the string value of the default history object.
	 * 
	 * @return String default history value
	 */
	public static String defaultValue() {
		return new HistoryObject().getStringValue();
	}

	/** Whether this history is switched ON. 
	 * @return boolean
	 */
	boolean isActivated () {
		return activated;
	}
	
	/** The number of passwords stored in this history.
	 * @return int
	 */
	int size () {
		return size;
	}

	/** The maximum number of passwords allowed in this history.
	 * @return int 
	 */
	int maxEntries () {
		return maxEntries;
	}
	
	String getStringValue () {
		return history;
	}
	
	/** Sets the maximum nuber of entries in this history.
	 * 
	 * @param max int maximum entries
	 */
	void setMaxEntries (int max) {
		// set new max-entries value
		maxEntries = Math.min(Math.max(0, max), 255);
		setInt(maxEntries, 1);

		// remove overhead entries
		int remove = Math.max(0, size-maxEntries);
		if ( remove > 0 ) {
		   reduceValues(remove);
		   size = size - remove;
		   setInt(size, 3);
		}
	}

//	/** Removes all entries from this list.
//	 */
//	void clear () {
//		reduceValues(size);
//		size = 0;
//		setInt(size, 3);
//	}
//	
//	void setActivated (boolean v) {
//		if ( v != activated ) {
//			activated = v;
//			history = (v ? "1" : "0").concat( history.substring(1) ); 
//		}
//	}
//	
	/** Sets a 2-digit hex integer value at the given index of the history 
	 * string. Replaces previous value at this location.
	 * 
	 * @param v int integer value
	 * @param offs int string index
	 */
	private void setInt (int v, int offs) {
    	String hstr = Util.shortToHex(v).substring(2);
    	history = history.substring(0, offs) + hstr + history.substring(offs+2);
	}
	
	void addValue (long time, PwsPassphrase pass) {
		// create history entry value (string)
		String pwd = pass.getString();
        String hstr = Util.intToHex( time/1000 ) +  
                      Util.shortToHex( pwd.length() ) +  pwd;
        
        // insert to end of history
        history = history.concat(hstr);
        size++;

        // update size value if required
         if ( size <= maxEntries ) {
        	 setInt(size, 3);
        } else {
        	reduceValues(1);
        }
		
		Log.debug(10, "(EditorDialog.HistoryObject.addValue) added history value "+
				" time=" + Util.standardTimeString(time) + ", value=" + pass.getString() +
				", content=" + getStringValue());
	}

	/** Reduces the list of password history entries for the given number of
	 * entries up to what is available. The oldest are removed first. Tolerates 
	 * nonsense values. This does not correct the size value of the history 
	 * which has to be done separately! 
	 * 
	 * @param i int number of entries to be cut away 
	 */
	private void reduceValues (int i) {
		if ( i < 1 ) return;
		i = Math.min(i, size); 
		for ( int j = 0; j < i; j++ ) {
			int len = Integer.valueOf( history.substring( 13, 17 ), 16 );
			history = history.substring( 0, 5 ).concat(history.substring( 17+len ));
		}
	}
}

@SuppressWarnings("serial")
private class FloatingToolbar extends ButtonBarDialog {

   private  JButton passButton, userButton, browseButton;

   public FloatingToolbar () {
      super( EditorDialog.this, DialogButtonBar.BUTTONLESS, false );
      init();
   }

   private void init () {
      // Dialog settings
      moveRelatedTo( EditorDialog.this );
      setAutonomous( true );
      setCloseByEscape( false );
      setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
  
      // register ESCAPE key and translate to parent dialog
      // i.e. we close the parent when ESC is hit in this dialog
      ActionListener kli = new ActionListener () {
         @Override
		public void actionPerformed ( ActionEvent e ) {
             ((ButtonBarDialog)getParent()).processWindowEvent( new WindowEvent(
                FloatingToolbar.this, WindowEvent.WINDOW_CLOSING ));
         }
      };
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      getRootPane().registerKeyboardAction(
            kli, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

      // create buttons and add to toolbar 
      JPanel barPanel = new JPanel();
      passButton = makeButton( "copypass-large", "toolbar.copypass", "menu.edit.copypass" );
      barPanel.add( passButton );

      userButton = makeButton( "copyuser-large", "toolbar.copyuser", "menu.edit.copyuser" );
      barPanel.add( userButton );

      browseButton = makeButton( "webstarter-large", "toolbar.webstart.record", "button.starturl" );
      browseButton.setEnabled( false );
      barPanel.add( browseButton );

      setDialogPanel( barPanel );
      
      String url = record.getUrl();
      if ( url != null ) {
         setBrowsingEnabled( Util.extractURL(url) != null );
      }
   }
   
   /** Sets enables status of the browsing button. Default state is <b>false</b>. */
   public void setBrowsingEnabled ( boolean s ) {
      browseButton.setEnabled( s );
   }
   
   private JButton makeButton ( String imageName, String buttonName, String command ) {
      JButton b = new JButton();
      b.setMargin( new Insets( 0, 0, 0, 0 ) );
      b.setToolTipText( ResourceLoader.getCommand(buttonName+".tooltip") );
      ImageIcon icon = ResourceLoader.getImageIcon(imageName);
      icon.setDescription( ResourceLoader.getDisplay(buttonName) );
      b.setIcon( icon ); 
      b.setActionCommand( command );
      b.addActionListener( actions );
      return b;
   }
}

/**
 * 
 */
@SuppressWarnings("serial")
public class EditUndoManager extends javax.swing.undo.UndoManager {

public EditUndoManager () {
   int i = Options.getIntOption( "maxUndoEntries" );
   int lim = i > 0 ? i : Global.DEFAULT_MAXUNDO;
   this.setLimit( lim );
}

/** Removes all entries in this Undo manager and updates menues. */
public void clear () {
   this.discardAllEdits();
   updated();
}

@Override
public void undoableEditHappened ( UndoableEditEvent e ) {
   if ( Options.isOptionSet( "useUndoRedo" ) ) {
      super.undoableEditHappened( e );
      updated();
      
      // protocol
//      System.out.println( "-- new UNDOABLE EDIT: Source=" + 
//            e.getSource() + ", EDIT=" + e.getEdit() ); 
//      System.out.println( "   Manager Status = " + this.edits.size() );
   }
}

private void updated() {
	toolbar.updateUndo();
}

@Override
public void redo () throws CannotRedoException {
   super.redo();
   updated();
}

@Override
public void undo () throws CannotUndoException {
   super.undo();
   updated();
}

public void removeHistoryEdits () {
   Object obj;
   int i, j;
   
   i = j = 0;
   while ( i < edits.size() ) {
      if ( (obj = edits.get( i )) instanceof FieldEdit ) {
    	  FieldEdit edit = (FieldEdit)obj;
         if ( edit.type == FieldEdit.HISTORY_MAX | edit.type == FieldEdit.HISTORY_DEL ) {
            trimEdits( i, i );
            j++;
            continue;
         }
      }
      i++;
   }
   if ( j > 0 ) {
      updated();
   }
   
//   System.out.println( "-- RemoveHistoryEdits : " + j );
}

//  **********  INNER CLASSES TO EditUndoManager  ****************

public class FieldEdit extends AbstractUndoableEdit {
   public static final int GROUP = 0;
   public static final int PASSWORD = 1;
   public static final int EXPIRY = 2;
   public static final int HISTORY_DEL = 3;
   public static final int HISTORY_MAX = 4;
   public static final int POLICY = 5;
   public static final int FIELD_TOPVALUE = 5;

   private int type;
   private PwsRecord oldRecord;
   private PwsPassphrase newPass;
   private PwsPassphrasePolicy oldPolicy, newPolicy;
   private String oldText, newText;
   private String newHistory;
   private long oldExpireTime, newExpireTime;
   private int  oldExpireInterval, newExpireInterval;
   private long newAccessTime;
   private long newPassModTime;
   private long updateTime;

   
   /**
    * Creates an undoable edit for the modification of text fields.
    *  
    * @param type the edit type (referring to edit field)
    * @param oldText <code>String</code> or <code>PwsPassphrase</code> before edit
    * @param newText <code>String</code> or <code>PwsPassphrase</code> after edit
    */
   public FieldEdit ( int type, String oldText, String newText ) {
      // parameter control
      if ( type < 0 | type > FIELD_TOPVALUE | type == PASSWORD )
         throw new IllegalArgumentException( "illegal edit type" );
      
      if (  newText == null )
         newText = "";
      if (  oldText == null )
         oldText = "";
      
      // init
      this.type = type;
      this.oldText = oldText;
      this.newText = newText;
      
      // record expiry time
      if ( type == EXPIRY ) {
         oldExpireTime = Util.longFromString( oldText );
         newExpireTime = record.getPassLifeTime();
      }
      
      updateTime = System.currentTimeMillis();
      Log.debug( 9, "(EditorDialog.EditUndoManager) created new undoable edit, type=TEXTFIELD" );
//      System.out.println( "-- new undoable edit: FIELD type=" + type + ", text=" + newText );
//      System.out.println( "                      old text was: " + oldText );
   }  // constructor

   
   
   /**
    * Creates an undoable edit for the type PASSWORD (modification of entry password).
    * Current values are taken from the central 'record' variable.
    *  
    * @param oldPass <code>PwsPassphrase</code> password value before edit
    * @param oldHistory <code>String</code> password history content value before edit
    * @param oldExpiry long password expire time before edit
    */
   public FieldEdit ( PwsRecord oldRec ) {
      this.type = PASSWORD;
      this.oldRecord = (PwsRecord)oldRec.clone();
      this.newPass = record.getPassword();
      this.newHistory = record.getHistory();
      this.newPassModTime = record.getPassModTime();
      this.newExpireTime = record.getPassLifeTime();
      this.newAccessTime = record.getAccessTime();
      
      updateTime = System.currentTimeMillis();
      Log.debug( 9, "(EditorDialog.EditUndoManager) created new undoable edit, type=PASSWORD" );
   }  // constructor

   /**
    * Creates an undoable edit for the modification of the record owned 
    * PASSPHRASE_POLICY.
    * Current value is taken from the central 'record' variable.
    */
   public FieldEdit ( PwsPassphrasePolicy oldPolicy ) {
      this.type = POLICY;
      this.oldPolicy = oldPolicy;
      this.newPolicy = record.getPassPolicy();
      
      updateTime = System.currentTimeMillis();
      Log.debug( 9, "(EditorDialog.EditUndoManager) created new undoable edit, type=POLICY" );
   }
   
   /**
    * Creates an undoable edit for the modification of the PASSLIFETIME field.
    * Current values are taken from the central 'record' variable.
    */
   public FieldEdit ( long oldExpireTime, int oldInterval ) {
      this.type = EXPIRY;
      this.oldExpireTime = oldExpireTime;
      this.oldExpireInterval = oldInterval;
      this.newExpireTime = record.getPassLifeTime();
      this.newExpireInterval = record.getExpiryInterval();
      
      updateTime = System.currentTimeMillis();
      Log.debug( 9, "(EditorDialog.EditUndoManager) created new undoable edit, type=EXPIRE_TIME" );
   }  // constructor

   public long getUpdateTime () {
      return updateTime;
   }

   @Override
   public void die () {
      super.die();
//      System.out.println( "-- die UndoableEdit " + toString() );
   }

   @Override
   public String toString () {
      return getPresentationName() + " *** " + super.toString();
   }
   
   @Override
   public String getPresentationName () {
      String field;
      
      switch ( type ) {
      case GROUP:
         field = "adddlg.label.group";
         break;
      case EXPIRY:
         field = "adddlg.label.expire";
         break;
      case PASSWORD:
         field = "adddlg.label.password";
         break;
      case POLICY:
         field = "adddlg.label.policy";
         break;
      case HISTORY_DEL:
         return ResourceLoader.getDisplay( "undo.editor.historydel" );
      case HISTORY_MAX:
         field = "adddlg.label.history_max";
         break;
      default:
         field = "";
      }
      
      return ResourceLoader.getDisplay( "undo.editor.field" ) + " " +
             ResourceLoader.getDisplay( field );
   }  // getPresentationName

   /**
    * This sets the password editor history with the given content. 
    * Given history's ACTIVE state is modified to the current editor history 
    * active state.
    * 
    * @param hist String history value, may be <b>null</b> for erase history
    */
   private void setHistory ( String hist ) {
	   if ( hist != null ) {
		  // take over "active" status from current record value
		  boolean active = new HistoryObject(record.getHistory()).isActivated();
	      hist = (active ? "1" : "0").concat( hist.substring( 1 ) );
	   }
       Log.debug(10, "(EditorDialog.EditUndoManager.FieldEdit.setHistory) setting history to " + hist);
	   
       // set new history value + update panels
       record.setHistory( hist );
       panelSystem.notifyFieldValue("HISTORY.UPDATE", hist, null);
	   handleHistory();
   }
   
   @Override
   public void redo () throws CannotRedoException {
      super.redo();
      updated();
      
      // action of this instance
      switch ( type )  {
      case GROUP:
         record.setGroup( newText );
         panelSystem.notifyFieldValue("LOGIN.GROUP", newText, null);
         break;
      case HISTORY_DEL:
      case HISTORY_MAX:
     	  setHistory( newText );
          break;
      case EXPIRY:
    	 record.setPassLifeTime(newExpireTime); 
    	 record.setExpiryInterval(newExpireInterval);
    	 panelSystem.notifyFieldValue("TIMES.UPDATE", null, null);
         break;
      case POLICY:
         setPassPolicy( newPolicy, false );
         break;
         
      case PASSWORD:
     	 record.setPassLifeTime( newExpireTime ); 
         record.setPassword( newPass );
         panelSystem.notifyFieldValue("LOGIN.PASSWORD", newPass, null);
         record.setPassModTime( newPassModTime );
         record.setAccessTime( newAccessTime );
    	 panelSystem.notifyFieldValue("TIMES.UPDATE", null, null);
         setHistory( newHistory );
         break;
      }
   }

   @Override
   public void undo () throws CannotUndoException {
      super.undo();
      updated();

      // action of this instance
      switch ( type ) {
      case GROUP:
          record.setGroup( oldText );
          panelSystem.notifyFieldValue("LOGIN.GROUP", oldText, null);
         break;
      case EXPIRY:
     	 record.setPassLifeTime(oldExpireTime); 
     	 record.setExpiryInterval(oldExpireInterval);
    	 panelSystem.notifyFieldValue("TIMES.UPDATE", null, null);
         break;
      case POLICY:
         setPassPolicy( oldPolicy, false );
         break;
      case HISTORY_DEL:
      case HISTORY_MAX:
    	 setHistory( oldText );
         break;

      case PASSWORD:
    	 PwsPassphrase oldPass = oldRecord.getPassword();
      	 record.setPassLifeTime( oldRecord.getPassLifeTime() ); 
         record.setPassword( oldPass );
         panelSystem.notifyFieldValue("LOGIN.PASSWORD", oldPass, null);
         record.setPassModTime( oldRecord.getPassModTime() );
         record.setAccessTime( oldRecord.getAccessTime() );
    	 panelSystem.notifyFieldValue("TIMES.UPDATE", null, null);
         setHistory( oldRecord.getHistory() );
         break;
      }
   }
}  // inner class FieldEdit


public class RevertEdit extends AbstractUndoableEdit {
   private PwsRecord oldRecord;
   private long updateTime;

   /**
    * Creates an undoable edit for the "Revert" function.
    *  
    * @param oldRec <code>PwsRecord</code> record value before edit
    */
   public RevertEdit ( PwsRecord oldRec ) {
      this.oldRecord = oldRec;
      
      updateTime = System.currentTimeMillis();
//      System.out.println( "-- new undoable edit: REVERT" );
   }  // constructor

   public long getUpdateTime () {
      return updateTime;
   }

   @Override
   public void die () {
      super.die();
//      System.out.println( "-- die UndoableEdit " + toString() );
   }

   @Override
   public String toString () {
      return getPresentationName() + " *** " + super.toString();
   }
   
   @Override
   public String getPresentationName () {
      return ResourceLoader.getDisplay( "undo.editor.revert" ); 
   }  // getPresentationName

   
   @Override
   public void redo () throws CannotRedoException {
      super.redo();
      
      // action
      revertContent();
      setupDisplay();
   }

   @Override
   public void undo () throws CannotUndoException {
      super.undo();

      // action
      record = (PwsRecord)oldRecord.clone();
      haveHistory = record.getHistory() != null;
      setupDisplay();
   }
}  // inner class RevertEdit
}  // inner class EditUndoManager

private class AutoSaveDaemon extends Thread {
   boolean terminate;

   public AutoSaveDaemon () {
      super( "JPWS EditorDialog Autosave-Daemon" );
      setDaemon( true );
   }

   /** Performs the editor's autosave function. */
   @Override
   public void run () {

       Log.log( 5, "(EditorDialog.AutoSaveDaemon.run) AutoSave-Thread started for EDIT RECORD: "
               .concat( record.toString() ));

       while ( !terminate ) {
         if ( objectListener.idleTime() > AUTOSAVE_PERIOD )
         synchronized ( record ) {	 
	        try {
	           // calculate new CRC over editable data
//	           Log.log( 10, "(EditorDialog.AutoSaveDaemon.run) -- check CRC");
	           int crc = record.getCRC(); 
	           if ( autoSaveCrc != crc ) {
	        	  PwsRecord rec = getRecord().copy();
	        	  String grp = rec.getGroup();
	              rec.setGroup( ResourceLoader.getDisplay( "mirrors.editrecord.safe" ) 
	            		  + "." + (grp == null ? "" : grp) );

	              if ( autoSaveRec != null ) {
	           	     rec.setRecordID(autoSaveRec.getRecordID());
	           	     container.updateRecord(rec);
	              } else {
	           	     container.addRecordRelaxed(rec);
	              }
	              autoSaveRec = rec;
	              autoSaveCrc = crc;
	              objectListener.activity();
	              Log.debug( 5, "(EditorDialog.AutoSaveDaemon.run) created security copy of EDIT RECORD: "
	                            .concat( rec.toString() ));
	           }
	        } catch ( Exception e ) {
	           e.printStackTrace();
	           terminate();
	        }
         }

         try
         { Thread.sleep( 5000 ); }
         catch ( InterruptedException e )
         {}
      }
       Log.log( 5, "(EditorDialog.AutoSaveDaemon.run) AutoSave-Thread terminated for EDIT RECORD: "
               .concat( record.toString() ));
   }

   @Override
   public synchronized void start () {
      super.start();
      Log.debug( 5, "(EditorDialog.AutoSaveDaemon.start) started AutoSave-Daemon for EDIT RECORD: "
            .concat( record.toString() ));
   }

   public void terminate () {
      if ( isAlive() ) {
         Log.debug( 5, "(EditorDialog.AutoSaveDaemon.terminate) terminating AutoSave-Daemon for EDIT RECORD: "
               .concat( record.toString() ));
         terminate = true;
         interrupt();
      }
   }
}

public static enum PanelType {LOGIN, TIMES, NOTES, HISTORY, TEXT}

private class PanelSystem {
//   public static final String LOGIN_PANEL = "LOGIN";
//   public static final String TIMES_PANEL = "TIMES";
//   public static final String NOTES_PANEL = "NOTES";
//   public static final String HISTORY_PANEL = "HISTORY";
   
   private ArrayList<EditorWorkPanel> wPanels = new ArrayList<EditorWorkPanel>();
   private JPanel view;
   private JTabbedPane  tabPane;
   private Border tabbedPaneBorder = BorderFactory.createEmptyBorder(18, 18, 15, 16);

   private EditorWorkPanel focusPanel;

   private MouseListener mouseListener = new MouseAdapter () {

	@Override
	public void mouseClicked (MouseEvent e) {
//		Log.log(10, "(PanelSystem.MouseListener.mouseClicked) event source = ".concat( e.getSource().getClass().getName() ));
		int index = tabPane.indexOfTabComponent(e.getComponent());
		if ( index > -1 ) {
// 		   String hstr = tabPane.getTitleAt(index);
//		   Log.log(10, "(PanelSystem.MouseListener.mouseClicked) clicked on tab ".concat(hstr));
		   
		   String name = tabPane.getComponentAt(index).getName();
		   focusPanel(name);
		   e.consume();
		}
	}
   };

//   private ChangeListener changeListener = new ChangeListener () {
//
//	@Override
//	public void stateChanged(ChangeEvent evt) {
//		int index = tabPane.getSelectedIndex();
//		Log.log(10, "(PanelSystem.ChangeListener.stateChanged) index = " + index);
//	}
//	   
//   };
   
   PanelSystem () {
	   init();
   }
   
   private void init () {
	  // create the top view (tabbed pane)
	  view = new JPanel( new BorderLayout() );
      tabPane = new JTabbedPane();
      
//      view.setOpaque(true);
//      view.setBackground(Color.cyan);
      
//      Border border = new LineBorder(Color.orange, 1);
//      tabPane.setBorder(border);
//      border = new BevelBorder(BevelBorder.RAISED);
//      view.setBorder(border);
      objectListener.registerChangeableObject( tabPane );
//      tabPane.addChangeListener( actions );
      view.add( tabPane, BorderLayout.CENTER );

   }
   
   public JPanel getView () {
	   return view;
   }
   
   /** Returns the work panel of the specified name or <b>null</b> if not 
    * present. For parameter null, always null is returned.
    * 
    * @param name String work panel name, may be <b>null</b>
    * @return <code>EditorWorkPanel</code> or <b>null</b>
    */
   private EditorWorkPanel getPanel (String name) {
	   if ( name != null ) {
		  for (EditorWorkPanel p : wPanels) {
		     if ( name.equals(p.getName()) ) {
			    return p;
		     }
		  }
	   }
	   return null;
   }
   
   /** Returns the first work panel which is of the specified panel type.
    * Returns <b>null</b> if not found.
    * 
    * @param panelType String work panel type name
    * @return <code>EditorWorkPanel</code> or <b>null</b>
    */
   private EditorWorkPanel getFirstPanel (PanelType panelType) {
	   for (EditorWorkPanel p : wPanels) {
		   if ( panelType.equals(p.getType()) ) {
			   return p;
		   }
	   }
	   return null;
   }
   
   public void notifyFieldValue (String field, Object value, EditorWorkPanel source) {
	 if ( field == null ) 
		throw new NullPointerException("field is null");
	 if ( field.isEmpty() )
		throw new IllegalArgumentException("field is empty");
	 
	 for (EditorWorkPanel p : wPanels) {
		 p.notifyFieldValue(field, value, source);
     }
   }

   /** Reads data from the editable record where required.
    */
   public void read() {
	  for (EditorWorkPanel p : wPanels) {
		  p.readData();
	  }
   }

   /** Writes data to the editable record where required.
    */
   public void write() {
	  for (EditorWorkPanel p : wPanels) {
		  p.writeData();
	  }
   }

   /** Terminates editing in all work panels. The return value indicates
    * whether there were vetoes to the process. This does not close the 
    * panels but checks all panels if they agree to edit termination.
    * 
    * @return boolean true == all went well, false == veto occurred
    */
   public boolean terminate () {
	  boolean ok = true;
	  for (EditorWorkPanel p : wPanels) {
		  ok &= p.panelHiding();
	  }
	  return ok;
   }
   
   /** Removes all work panels and terminates this panel system's operations.
    */
   public void exit () {
	  for (EditorWorkPanel p : wPanels) {
		  p.panelClosing();
	  }
   }
   
   /** Adds a new work panel to this panel system at the given index.
    * If index is out of range, e.g. -1, the panel is positioned to the end
    * of the list. The panel is filled with data through <code>read()</code>.
    * 
    * @param type <code>PanelType</code> panel type
    * @param name String panel name (unique)
    * @param index int panel position
    * @return EditorWorkPanel
    */
   public EditorWorkPanel activatePanel (PanelType type, String name, int index) {
	   // check parameters entry
	   if ( type == null )
		   throw new IllegalArgumentException("type is null");
	   if ( name == null || name.isEmpty() )
		   throw new IllegalArgumentException("name is null or empty");
	   if ( index < 0 | index > wPanels.size() ) {
		   index = wPanels.size();
	   }
	   
	   Log.debug(10, "(EditorDialog.PanelSystem.activatePanel) activate name=" + name + ", type=" + type);
	   
	   // check name is unique
	   for (EditorWorkPanel p : wPanels ) {
		   if ( name.equals(p.getName()) ) {
			   throw new IllegalArgumentException("panel name already exists, must be unique!");
		   }
	   }

	   EditorWorkPanel panel;
	   
	   // create a panel according to type
	   if ( type == PanelType.LOGIN ) {
		   panel = new Login_WorkPanel(name);
	       panel.getView().setBorder( tabbedPaneBorder );
	   } else if ( type == PanelType.TIMES ) {
		   panel = new Times_WorkPanel(name);
	       panel.getView().setBorder( tabbedPaneBorder );
	   } else if ( type == PanelType.HISTORY ) {
		   panel = new History_WorkPanel(name);
	       panel.getView().setBorder( tabbedPaneBorder );
	   } else if ( type == PanelType.NOTES ) {
		   panel = new Notes_WorkPanel(name);
	       panel.getView().setBorder( tabbedPaneBorder );
	   } else {
		   throw new IllegalArgumentException("panel type is unknown: ".concat(type.name()));
	   }
	   
	   // initialise panel with data
	   panel.readData();
	   
	   // add panel to structure
	   wPanels.add(index, panel);
	   tabPane.add(panel.getView(), index);
	   JLabel label = new JLabel(panel.getName());
	   label.setOpaque(false);
	   JPanel renderComp = new JPanel(new BorderLayout());
	   renderComp.add(label, BorderLayout.CENTER);
	   renderComp.setOpaque(false);
//	   renderComp.setBorder(new EmptyBorder(0,0,0,0));
//	   renderComp.setBackground(Color.yellow);
	   renderComp.addMouseListener(mouseListener);
	   tabPane.setTabComponentAt(index, renderComp);
	   tabPane.setEnabledAt(index, false);
	   return panel;
   }
   
   /** Removes the work panel specified by name after saving its data to record.
    * The operation may be vetoed by the panel in which case <b>false</b> is 
    * returned.
    * 
    * @param name String panel name
    * @return boolean true == panel removed, false == panel not found or 
    *         panel vetoed operation
    */
   public boolean removePanel (String name) {
	  boolean removed = false;
	  EditorWorkPanel panel = getPanel(name);
	  if ( panel != null ) {
		 removed = panel.panelClosing();
		 if ( removed ) {
		    panel.writeData();
			wPanels.remove(panel);
		    tabPane.remove(panel.getView());
		 }
	  }
	  return removed;
   }
   
   /** Removes all work panels of the specified type. If closing of at least one
    * panel has been vetoed, <b>false</b> is returned, <b>true</b> otherwise.
    * Vetoing a work panel does not prevent other panels from closing if they 
    * are not vetoed on their part.
    * 
    * @param type <code>PanelType</code>
    * @return boolean true == all panels of type closed, false == one or more
    *         panels of type have been vetoed
    */
   @SuppressWarnings("unchecked")
   public boolean removePanelType (PanelType type) {
	  boolean confirm = true;
	  if ( type != null ) { 
		 for (EditorWorkPanel p : (ArrayList<EditorWorkPanel>)wPanels.clone()) {
		    if ( p.getType() == type ) {
			   confirm &= removePanel(p.getName());
			}
	     }
	  }
	  return confirm;
   }
   
   /** Brings the specified work panel to foreground and organises required
    * data movements to make it updated against the record. Does nothing if
    * the parameter is <b>null</b>.
    *  
    * @param panel <code>EditorWorkPanel</code>
    * @return boolean true == operation ok,
    *                 false == panel null, already focused or current panel
    *                 not verified or does not want to hide
    */
   public boolean focusPanel (EditorWorkPanel panel) {
	  if ( panel == null ) return false;
	  
	  // deal with current focused panel
	  if ( focusPanel != null ) {
		 // request backstep (hiding)
		 if ( !focusPanel.panelHiding() ) {
			 return false;
		 }
		 // request verification of data correctness + integrity
		 if ( !focusPanel.verify() ) {
			 return false;
		 }
//		 // write modified data to record
//		 focusPanel.writeData();
	  }
	  
	  // install new work panel
	  String name = panel.getView().getName();
	  Log.log(10, "(EditorDialog.PanelSystem.focusPanel) selecting panel ".concat(name));
	  tabPane.setSelectedComponent(panel.getView());
	  focusPanel = panel;
	  String text = (String)panel.getView().getClientProperty( "explain" );
      explainLabel.setText( text );
	  
//	  panel.readData();

	  // notify new panel
	  if ( panel.getView().isShowing() ) {
		 panel.panelShown();
	  }
	  return true;
   }
   
   /** Brings the specified work panel to foreground and organises required
    * data movements to make it updated against the record. Does nothing if
    * the name is unknown.
    *  
    * @param name String panel name
    * @return boolean true == operation ok,
    *                 false == panel unknown, already focused or current panel
    *                 not verified or does not want to hide
    */
   public boolean focusPanel (String name) {
	  return focusPanel( getPanel(name) );
   }
   
   /** Brings the first available work panel of the specified panel type to 
    * foreground and organises required data movements to make it updated 
    * against the record. Does nothing if the type is unknown.
    *  
    * @param type String work panel type name
    * @return boolean true == operation ok,
    *                 false == type unknown, panel already focused or current 
    *                 panel not verified or does not want to hide
    */
   public boolean focusPanelType (PanelType type) {
	  EditorWorkPanel panel = getFirstPanel(type);
	  return focusPanel(panel);
   }

}

private class Login_WorkPanel extends EditorWorkPanel 
	implements ActionListener, FocusListener, ItemListener {

	private JComboBox 			group;
	private EditorTextField		title;
	private EditorTextField		username;
	private EditorTextField		email;
	private EditorTextField		urlFld;
	private OurNotesTextArea	notes;
	private DlgPasswordField	passFld;
	private JButton				mailButton;
	private JButton				urlButton;
	
	private FocusListener		focusListener = this;
	private Object       		actGroup;
	private boolean				haveNotes;
	
	public Login_WorkPanel (String name) {
		super(name, PanelType.LOGIN);
		init();
	}

	private void init () {
		JPanel		pane, pane2, panel;
		JLabel		label;
        int rows;
        haveNotes = !Options.isOptionSet("editFullNotes");

        pane = new JPanel( new SpringLayout() );
        label = new JLabel( ResourceLoader.getDisplay("adddlg.label.group") );
        rows = 3;

        group = GUIService.getGroupListCombo( container, true, 450 );
        if ( formatVariant != FormatVariant.pws1 ) {
           group.addItemListener( this );
           objectListener.registerChangeableObject( group );
           pane.add( label );
           pane.add( group );
           rows++;
        }

        label = new JLabel( ResourceLoader.getDisplay("adddlg.label.title") );
        title = new EditorTextField( TEXTFIELDSIZE );
        title.setFont( dataFont );
        title.addFocusListener( focusListener );
        objectListener.registerChangeableObject( title );
		pane.add( label );
		pane.add( title );

		label		= new JLabel( ResourceLoader.getDisplay("adddlg.label.username") );
		label.setBorder( new EmptyBorder( 0, 0, 0, 10 ) );
		username	= new EditorTextField( TEXTFIELDSIZE );
        username.setFont( dataFont );
        username.addFocusListener( focusListener );
        objectListener.registerChangeableObject( username );
		pane.add( label );
		pane.add( username );

		label	= new JLabel( ResourceLoader.getDisplay("adddlg.label.password") );
		passFld	= new DlgPasswordField();
        passFld.setFont( DisplayManager.getFont( "password" ) );
        passFld.addFocusListener( focusListener );
        objectListener.registerChangeableObject( passFld );
        pane.add( label );
        pane.add( passFld );

		// EMAIL field
        panel = new JPanel( new BorderLayout() );
        label = new JLabel( ResourceLoader.getDisplay("adddlg.label.email") );
        panel.add( label, BorderLayout.WEST );
        
        mailButton = new JButton( ResourceLoader.getImageIcon( "copy16-icon" ) );
        mailButton.setActionCommand( "button.copyemail" );
        mailButton.addActionListener( this );
        mailButton.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 3 ) );
        mailButton.setBackground( panel.getBackground() );
        mailButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.copyemail" ));
        mailButton.setFocusPainted( false );
        panel.add( mailButton, BorderLayout.EAST );

        email  = new EditorTextField( TEXTFIELDSIZE );
        email.setFont( dataFont );
        if ( formatVariant == FormatVariant.pws3 ) {
           email.addFocusListener( focusListener );
           objectListener.registerChangeableObject( email );
           pane.add( panel );
           pane.add( email );
           rows++;
        }
        
        // URL field
        panel = new JPanel( new BorderLayout() );
        label  = new JLabel( ResourceLoader.getDisplay("adddlg.label.urlfield") );
        panel.add( label, BorderLayout.WEST );
        
        urlButton = new JButton( ResourceLoader.getImageIcon( "webicon" ) );
        urlButton.setActionCommand( "button.starturl" );
        urlButton.addActionListener( actions );
        urlButton.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 3 ) );
        urlButton.setBackground( panel.getBackground() );
        urlButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.starturl" ));
        urlButton.setFocusPainted( false );
        panel.add( urlButton, BorderLayout.EAST );

        urlFld  = new EditorTextField( TEXTFIELDSIZE );
        urlFld.setFont( dataFont );
        urlFld.addChangeEventListener( new EditorChangeEventListener() {

           @Override
           public void documentChanged ( EditorChangeEvent event ) {
//              System.out.println( "--- Editor Feld Change Event: " + event.getType() );
              boolean enable = Util.extractURL(urlFld.getText()) != null;
              toolbar.updateUrl();
              if ( floatToolbar != null ) {
                 floatToolbar.setBrowsingEnabled( enable );
              }
           }
        });
        if ( formatVariant == FormatVariant.pws3 ) {
           urlFld.addFocusListener( focusListener );
           objectListener.registerChangeableObject( urlFld );
           pane.add( panel );
           pane.add( urlFld );
           rows++;
        }
        
        // NOTES field
        if ( haveNotes ) {
	        notes = new OurNotesTextArea( ResourceLoader.getDisplay( "pane.edit.notes" ) );
	        notes.setFont( DisplayManager.getFont( "notes" ));
	        notes.addFocusListener( focusListener );
	        notes.setDialogOwner( EditorDialog.this );
	        objectListener.registerChangeableObject( notes );
	        
            panel = new JPanel( new BorderLayout() );
            label = new JLabel( ResourceLoader.getDisplay("adddlg.label.notes") );
            panel.add( label, BorderLayout.WEST );
            
            pane2 = new JPanel( new FlowLayout( FlowLayout.RIGHT, 0, 0 ) );
            pane2.add( notes.getTearoffButton() );
            panel.add( pane2, BorderLayout.EAST );
            pane.add( panel );
            
      		pane.add( notes.getView() );
      		rows++;
	    }

        // format pane depending on rows 
        SpringUtilities.makeCompactGrid( pane, rows, 2, 0, 0, 6, 6 );

        // set pane into Northern Borderlayout location (avoids resizing of components) 
        pane2 = getView();
        pane2.putClientProperty( "explain", ResourceLoader.getDisplay( "adddlg.explain.login" ));
        pane2.add( pane, BorderLayout.NORTH );
        
//        pane2.setBackground(Color.orange);
	}

	@Override
	public void readData() {
	      synchronized ( record ) {
	    	 String grpVal = record.getGroup();
	         if ( grpVal == null ) {
	        	 grpVal = "";
	         }
	         
	         actGroup = grpVal;
	         group.setSelectedItem( grpVal );
	         title.setText( record.getTitle() );
	         title.clearUndoList();
	         email.setText( record.getEmail() );
	         email.clearUndoList();
	         username.setText( record.getUsername() );
	         username.clearUndoList();
	         urlFld.setText( record.getUrl() );
	         urlFld.clearUndoList();
	         passFld.setPassphrase( record.getPassword() );
	         passFld.clearUndoList();
	         passFld.setHidden( passwordHidden );
	         
	         if ( haveNotes ) {
		        notes.setText( record.getNotes() );
		        notes.clearUndoList();
	         }
 	      }		
	}

	@Override
	public void writeData() {
       PwsPassphrase recPass, pass;
       Object obj = group.getSelectedItem();
       String grpVal = obj == null ? "" : obj.toString();
      
       synchronized ( record ) {
          record.setUsername( username.getText() );
          record.setTitle( title.getText() );
          record.setEmail( email.getText() );
          record.setGroup( grpVal );
          record.setUrl( urlFld.getText() );

          if ( haveNotes ) {
             record.setNotes( notes.getText() );
          }
          pass = passFld.getPassphrase(); // new password value
          recPass = record.getPassword(); // old password value
          if ( !equalPassphrases( recPass, pass ) ) {
             record.setPassword( pass );  // change password
             panelSystem.notifyFieldValue("LOGIN.PASSWORD", pass, this);
          }
       }
	}

	@Override
	public void notifyFieldValue (String field, Object value, EditorWorkPanel source) {
		if ( field == null | source == this ) return;
		
		if ( field.equals("LOGIN.PASSWORD") ) {
			passFld.setPassphrase((PwsPassphrase)value);
		}

		else if ( field.equals("LOGIN.URL") ) {
			urlFld.setText((String)value);
		}

		else if ( field.equals("LOGIN.PASSWORD_COVER") ) {
			passFld.setHidden(passwordHidden);
		}
		
		else if ( field.equals("LOGIN.GROUP") ) {
	       actGroup = value;
           group.setSelectedItem( value );
		}
	}

	/** Whether two parameter passphrases are equal, where <b>null</b>
     * is considered a legal value but does not equal the empty passphrase.
     *  
     * @param p1 PwsPassphrase
     * @param p2 PwsPassphrase
     * @return boolean
     */
    private boolean equalPassphrases ( PwsPassphrase p1, PwsPassphrase p2 ) {
       return p1 == null & p2 == null ||
              (p1 != null & p2 != null && p1.equals( p2 ));
    }

// -------- ACTIONS FOR THIS PANEL -------    
    
	@Override
	public void actionPerformed(ActionEvent evt) {
	   String command = evt.getActionCommand();
	   String hstr;
	   
      if ( command.equals( "button.copyemail" ) ) {
         // if field is empty AND there is a valid email address in the clipboard
         // copy it into EMAIL
         if ( (hstr = email.getText()).isEmpty() ) {
            hstr = Global.getClipboardText().trim();
            if ( Util.isEmailAddress( hstr ) )
               email.setText( hstr );
         
         // if field is NOT empty
         } else {
            // if there is a valid email address, start external client
            if ( (hstr = Util.extractMailaddress( hstr )) != null ) {
               Global.startEmail( hstr );
            
            // else copy entire content to the clipboard
            } else {
               ActionHandler.sendClipboardText(EditorDialog.this, hstr, "confirm.email");
            }
         }
      }
      
	}
	
// ----------- Implementation of FocusListener ------------	
	
   @Override
   public void focusGained ( FocusEvent e ) {
   }
   
   @Override
   public void focusLost ( FocusEvent e ) {
      boolean exemption;
      
      recentFocusOwner = e.getComponent();
//      Component comp = e.getOppositeComponent();
//      exemption = comp == revertButton | comp == cancelButton;
      exemption = false;
      if ( e.getComponent() == passFld ) {
    	 PwsPassphrase pass = passFld.getPassphrase();
         if ( !equalPassphrases(pass, record.getPassword()) & !exemption ) {
        	 
        	setPassword(pass, this); 
         }
      }
   }
	
// *********  ITEMLISTENER  ***********

  @Override
  public void itemStateChanged ( ItemEvent e ) {
  String value = (String)e.getItem();

  // this deals with 
  if ( e.getSource() == group  && e.getStateChange() == ItemEvent.SELECTED && 
       !value.equals( actGroup ) ) {
     Log.debug(10, "(Login_WorkPanel.itemStateChanged) New Group Value (Item): ".concat(value));
     
     // create undoable edit event
	 UndoableEdit edit = undoManager.new FieldEdit( EditUndoManager.FieldEdit.GROUP,
 			             (String)actGroup, value );
     undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
     
     actGroup = value;
  }
}

}

private class Notes_WorkPanel extends EditorWorkPanel implements FocusListener {
	private OurNotesTextArea	notes;

	public Notes_WorkPanel (String name) {
	   super(name, PanelType.NOTES);
	   init();
	}
	
	private void init () {
	    JPanel panel  = getView();
	    panel.putClientProperty( "explain", ResourceLoader.getDisplay( "adddlg.explain.login" ) );
	
	    notes = new OurNotesTextArea( ResourceLoader.getDisplay( "pane.edit.notes" ) );
	    notes.setFont( DisplayManager.getFont( "notes" ));
	    notes.addFocusListener( this );
	    notes.setDialogOwner( EditorDialog.this );
	    objectListener.registerChangeableObject( notes );
	    
	    panel.add( notes.getView() );
	    panel.setPreferredSize( new Dimension( 10, 270 ) );
	}
	
	@Override
	public void readData() {
		notes.setText( record.getNotes() );
	    notes.clearUndoList();
	}
	
	@Override
	public void writeData() {
		record.setNotes( notes.getText() );
	}

	@Override
	public void focusGained(FocusEvent e) {
	}

	@Override
	public void focusLost(FocusEvent e) {
		recentFocusOwner = e.getComponent();
	}
}

private class History_WorkPanel extends EditorWorkPanel 	
		implements ActionListener {

	private HistoryHandler history;
	private JButton historyOffButton;
	
	public History_WorkPanel(String name) {
	   super(name, PanelType.HISTORY);
	   init();
	}

	private void init () {
       JPanel pane, panel;
       JButton clearButton, settingButton;
       ActionListener actions = this;
      
       pane = new JPanel( new BorderLayout() );
       pane.setName( ResourceLoader.getDisplay( "pane.edit.history" ) );

       // top part is history text area
       history = new HistoryHandler();
       pane.add( history, BorderLayout.CENTER );
      
       // bottom part is service area
       panel = new JPanel( );
       pane.add( panel, BorderLayout.SOUTH );
      
       // clear history button
       clearButton = new JButton( ResourceLoader.getDisplay( "button.clear" ) );
       clearButton.setActionCommand( "button.clearhistory" );
       clearButton.addActionListener( actions );
       clearButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
       clearButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.clearhistory" ));
       panel.add( clearButton );
      
       // settings button (max entries)
       settingButton = new JButton( ResourceLoader.getDisplay( "adddlg.button.setmax" ) );
       settingButton.setActionCommand( "button.historysettings" );
       settingButton.addActionListener( actions );
       settingButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
       settingButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.historysettings" ));
       panel.add( settingButton );

       // history off button
       String text = history.isActive() ? ResourceLoader.getDisplay( "button.off" )
    		         : ResourceLoader.getDisplay( "button.on" );
       historyOffButton = new JButton( text );
       historyOffButton.setActionCommand( "button.historyoff" );
       historyOffButton.addActionListener( actions );
       historyOffButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
       historyOffButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.historyoff" ));
       panel.add( historyOffButton );

       setView( pane );
       refreshHistoryComment();
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
	   String command = evt.getActionCommand();
		
       if ( command.equals( "button.clearhistory" ) ) {
          if ( GUIService.userConfirm( history, "msg.passhist.delete" ) ) {
             String hstr = history.getContentPw3();
             history.clear();
            
             // create undoable edit event
             UndoableEdit edit = undoManager.new FieldEdit( EditUndoManager.FieldEdit.HISTORY_DEL, 
            		             hstr, history.getContentPw3() );
             undoManager.undoableEditHappened( new UndoableEditEvent(this, edit) );
          }
       }
      
       else if ( command.equals( "button.historysettings" ) ) {
          // ask user for text input
          int i = history.getMaxEntries();
          int j;
          boolean iconPressed = evt.getSource() == toolbar.historyIcon;
         
          try {
             // perform user input if maxhist == 0 or the "maximum" button is pressed
             if ( i == 0 | !iconPressed ) {
                j = GUIService.integerInput( getView(), 
                    ResourceLoader.getDisplay( "dlg.input.historymax" ),
                    null, 0, 255, i );
             } else {
                j = i;
             }
            
             String oldContent = history.getContentPw3();
             history.setMaxItems( j );
//             history.setActive( j > 0 );
             record.setHistory(history.getContentPw3());
             updateButtons();
            
             if ( j != i ) {
                // create undoable edit event
            	UndoableEdit edit = undoManager.new FieldEdit( EditUndoManager.FieldEdit.HISTORY_MAX, 
            			            oldContent, history.getContentPw3() );
                undoManager.undoableEditHappened( new UndoableEditEvent(this, edit) );
             }
          } catch ( UserBreakException e ) {
          }
       }
      
       else if ( command.equals( "button.historyoff" ) ) {
          history.setActive( !history.isActive() );
          updateButtons();
       }
       
       record.setHistory(history.getContentPw3());
       handleHistory();
       refreshHistoryComment();
       toolbar.update();
       objectListener.activity();
	}

	@Override
	public void readData() {
	   String value = record.getHistory();
	   history.setContentPw3(value);
	   Log.debug(8, "(EditorDialog.History_WorkPanel.readData) read HISTORY value from record: " + value);

	   // normalise history value if its list size is zero 
	   if ( history.getListSize() == 0 & history.getMaxEntries() > 0 ) {
		  value = HistoryObject.defaultValue();
		  history.setContentPw3(value);
		  Log.debug(8, "(EditorDialog.History_WorkPanel.readData) corrected HISTORY value to: " + value);
	   }
	   updateButtons();
	}

	@Override
	public void writeData() {
	   String value = history.getContentPw3();
	   
	   // remove histories of list size zero
	   if ( history.getListSize() == 0 & history.getMaxEntries() > 0 ) {
		   value = null;
	   }
	   record.setHistory( value );
	   Log.debug(8, "(EditorDialog.History_WorkPanel.writeData) written HISTORY value to record: " + value);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		history.setEnabled(enabled);
		updateButtons();
	}

	@Override
	public void notifyFieldValue(String field, Object value, EditorWorkPanel source) {
		if ( field == null )
		   throw new NullPointerException();
		
		if ( field.equals("HISTORY.UPDATE") ) {
			readData();
		}
	}

   /** Refreshes and if possible displays the EXPLAIN TEXT label of the editor
    * with current HISTORY settings.
    */
   private void refreshHistoryComment () {
      // compose actual version of explain text (history)
      String text = ResourceLoader.getDisplay( "adddlg.explain.history" );
      text = Util.substituteTextS( text, "$items", String.valueOf( history.getListSize() ) );
      text = Util.substituteTextS( text, "$maxitems", String.valueOf( history.getMaxEntries() ) );
      getView().putClientProperty( "explain", text );
      
      // display if history panel is displayed
      if ( isShowing() ) {
         explainLabel.setText( text );
      }
   }
	
   private void updateButtons () {
      String onText = ResourceLoader.getDisplay( "button.on" );
      String offText = ResourceLoader.getDisplay( "button.off" );
 	  historyOffButton.setText( history.isActive() ? offText : onText );
   }
}

private class Times_WorkPanel extends EditorWorkPanel 	
		implements ActionListener, ChangeListener {
   private final Color TIME_COLOR = new Color( 0x005FBF );
   private final Color INVALID_COLOR = Color.GRAY;
   private final Color HOTFIELD_COLOR = new Color( 0x8B0000 );  // darkred
   private final int SPINNERACTION_DELAYTIME = 2000; // milliseconds

   // index values for expire period combi
   private static final int LIFE_UNLIMITED = 0; 
   private static final int LIFE_PERIOD = 12 ;
   private static final int LIFE_SPECIAL = 13;

   private JLabel           recModLabel;
   private JLabel           passModLabel;
   private JLabel           accessLabel;
//   private JLabel           explainLabel;
   private JLabel           shortKeyValueLabel;
   private JLabel           Signature_Label;
   private JLabel           UUID_Label;
   private BlinkingLabel    expiryLabel;
//   private BlinkingLabel    expiryIcon;
   private JSpinner         expIntervalSpinner;
   private JPanel           expIntervalPanel;
   private JComboBox    	expireCombo;
   private JButton      	timeButton;
   
   private boolean			actionPause;
   
   public Times_WorkPanel (String name) {
	  super(name, PanelType.TIMES);
	  init();
   }

   private void init () {
      JPanel      pane, panel;
      JLabel      label;
      JButton 	button;
      SpinnerNumberModel spinnerModel; 
      String      hstr;
      Color       color;
      Font        font;
      
      ActionListener actions = this;
      int timelabel_width = 100;
      
      pane  = new JPanel( new VerticalFlowLayout( 4 ) );
      pane.setName( ResourceLoader.getDisplay( "pane.edit.stats" ) );

      label = new JLabel( ResourceLoader.getDisplay( "adddlg.label.record" ) + " " );
      label.setFont( label.getFont().deriveFont( Font.ITALIC ) );
      pane.add( label );
      
      label = new JLabel();
      pane.add( makeTimeValuePanel( label, "adddlg.time.create", timelabel_width,
                record.getCreateTime(), null ));
      
      recModLabel = new JLabel();
      pane.add( makeTimeValuePanel( recModLabel, "adddlg.time.modify", 
            timelabel_width, record.getModifiedTime(), null ));

      pane.add( Box.createVerticalStrut( 10 ) );
      
      label = new JLabel( ResourceLoader.getDisplay( "adddlg.label.password" ) + " " );
      label.setFont( label.getFont().deriveFont( Font.ITALIC ) );
      pane.add( label );
      
      accessLabel = new JLabel();
      color = Options.isOptionSet( "storeMinorChanges" ) || container.isModified() 
    		  ? null : INVALID_COLOR;
      pane.add( makeTimeValuePanel( accessLabel, "adddlg.time.access", 
            timelabel_width, record.getAccessTime(), color ));

      passModLabel = new JLabel();
      pane.add( makeTimeValuePanel( passModLabel, "adddlg.time.modify", 
            timelabel_width, record.getPassModTime(), null ));

      expiryLabel = new BlinkingLabel();
      expiryLabel.setForeground( HOTFIELD_COLOR );
      Global.addTimePulseListener( expiryLabel );
      pane.add( makeTimeValuePanel( expiryLabel, "adddlg.time.expire", 
            timelabel_width, 0, HOTFIELD_COLOR ));
      
      panel = new JPanel( );
      pane.add( panel );
      panel.add( Box.createHorizontalStrut( timelabel_width - 10 ) );
      
      hstr = ResourceLoader.getCommand( "combo.passlifetime" );
      expireCombo = new JComboBox( hstr.split( "," ) );
      expireCombo.addActionListener( actions );
      expireCombo.setToolTipText( ResourceLoader.getCommand( "tooltip.combo.expiry" ));
      expireCombo.setFont( dataFontBold );
      panel.add( expireCombo );
      
      timeButton = new JButton( ResourceLoader.getImageIcon( "button.enter.16" ) );
      timeButton.setActionCommand( "button.expirydate" );
      timeButton.addActionListener( actions );
      timeButton.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 3 ) );
      timeButton.setBackground( panel.getBackground() );
      timeButton.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.expiry" ));
      timeButton.setFocusPainted( false );
      panel.add( timeButton );
      
      panel = new JPanel( );
      pane.add( panel );
      label = new JLabel( ResourceLoader.getDisplay( "units.days" ) );
      label.setPreferredSize( new Dimension( timelabel_width - 10, 16 ) );
      panel.add( label );
//	      panel.add( Box.createHorizontalStrut( timelabel_width - 10 ) );
      spinnerModel = new SpinnerNumberModel( 0, 0, 3650, 1 );
      expIntervalSpinner = new JSpinner( spinnerModel );
      expIntervalSpinner.addChangeListener( this );
      objectListener.registerChangeableObject( expIntervalSpinner );
      panel.add( expIntervalSpinner );
      expIntervalPanel = panel;
      expIntervalPanel.setVisible( false );
      
      panel = new JPanel( );
      pane.add( panel );
      button = new JButton(ResourceLoader.getImageIcon("button.shortcutkey"));
      button.setActionCommand("button.shortcutkey");
      button.addActionListener(actions);
      button.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 5 ) );
      button.setBorderPainted(false);
      button.setBackground( panel.getBackground() );
      button.setToolTipText( ResourceLoader.getCommand( "tooltip.edit.shortcutkey" ));
      button.setFocusPainted( false );
      panel.add(button);
      shortKeyValueLabel = new JLabel();
      font = shortKeyValueLabel.getFont().deriveFont( Font.PLAIN, (float)14.0 );
      shortKeyValueLabel.setFont(font);
      shortKeyValueLabel.setForeground(Color.BLACK);
      panel.add(shortKeyValueLabel);
      
      // Debug modus values
      if ( isDebug ) {
         pane.add( Box.createVerticalStrut( 10 ) );
         UUID_Label = new JLabel( "UUID = ".concat( record.getRecordID().toString() ) );
         font = UUID_Label.getFont().deriveFont( Font.PLAIN );
         UUID_Label.setFont( font );
         pane.add( UUID_Label );
         Signature_Label = new JLabel( "Signature = ".concat( Util.bytesToHex( record.getSignature() ).substring(0,16) ));
         Signature_Label.setFont( font );
         pane.add( Signature_Label );
      }
      
      pane.putClientProperty( "explain", ResourceLoader.getDisplay( "adddlg.explain.stats" ));
	  setView(pane);
   }

   private JPanel makeTimeValuePanel ( JLabel label, String token, 
	         int width, long time, Color color )
   {
      JPanel panel  = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
      
      JLabel label1 = new JLabel( ResourceLoader.getDisplay( token ) );
      if ( color != null )
         label1.setForeground( color );
      Dimension dim = label1.getSize();
      dim.width = width; 
      label1.setPreferredSize( new Dimension( width, 16 ) );
      panel.add( label1 );

      setTimeLabel( label, time );
      label.setForeground( color == null ? TIME_COLOR : color );
      label.setFont( dataFont.deriveFont( Font.BOLD ) );
      panel.add( label );
      
      return panel;
   }

   private void setTimeLabel ( JLabel label, long time ) {
      label.setText( time == 0 ? "?" : Global.getLocalDateTime(time) ); 
   }
   
//@Override
//public void itemStateChanged(ItemEvent arg0) {
//}
//
//@Override
//public void focusGained(FocusEvent evt) {
//	Log.log(10, "(focusGained) comp= " + evt.getSource());
//}
//
//@Override
//public void focusLost(FocusEvent evt) {
//	Log.log(10, "(focusLost) comp= " + evt.getSource());
//}
//
@Override
public void actionPerformed (ActionEvent evt) {
   if ( actionPause ) return;
	
   String command = evt.getActionCommand();
   String hstr;
	   
   // handle PASSLIFETIME combo events
   if ( evt.getSource() == expireCombo ) {
      int i = expireCombo.getSelectedIndex();
      expIntervalPanel.setVisible( i == LIFE_PERIOD );
      setPassLifeTimeAfterIndex( i, true );
      
      if ( i == LIFE_SPECIAL ) {
    	  Runnable run = new TimesRunnable( 2 );
    	  SwingUtilities.invokeLater(run);
      }
   }
   
   if ( command.equals( "button.expirydate" ) ) {
       // ask user for text input
       long time = record.getPassLifeTime();
       hstr = time == 0 ? null : Util.standardTimeString( time );
       hstr = GUIService.userInput( getView(), 
             ResourceLoader.getDisplay( "dlg.input.expirydate" ),
             ResourceLoader.getDisplay( "format.standarddate" ),
             hstr );
       objectListener.activity();
       
       // extract time value and set expiry date
       if ( hstr != null ) {
          if ( (time = Util.timeFromString( hstr, null )) == -1 ) {
             GUIService.infoMessage( getView(), "dlg.badvalue", "msg.badtimevalue");
          } else {
             setPassLifeTime( time, 0, true );
          }
       }
	   
   }
   
   else if ( command.equals( "button.shortcutkey" ) ) {
 	  setupShortcutKey();
   }
   
}

private class TimesRunnable extends TimerTask {
	private int type;
	private boolean hasRun;
	
	TimesRunnable ( int type ) {
		this.type = type;
	}
	
	@Override
	public void run() {
	   if ( hasRun ) return;
	   
	   // JSpinner "Period" value interpretation 	
	   if ( type == 1 ) {
		  if ( SwingUtilities.isEventDispatchThread() ) { 
			 // execute the command on EDT
	         Log.log( 7, "(EditorDialog.TimesRunnable.run) executing spinner-value record update" );
	         int days = ((Integer)expIntervalSpinner.getValue()).intValue();
	         setPassLifeTimeAfterInterval( days, true );
	         hasRun = true;
		  } else {
			 // put this task on the EDT for later execution
		     Log.log( 7, "(EditorDialog.TimesRunnable.run) placing order to EDT: spinner value" );
			 SwingUtilities.invokeLater(this);
		  }
	   } 
	   
	   else if ( type == 2 ) {
	      timeButton.doClick();
	   }
	}
	
}

private TimesRunnable spinnerAction;

@Override
public void stateChanged(ChangeEvent evt) {
    if ( actionPause ) return;
    
//    if ( evt.getSource() == tabPane ) {
//       JPanel panel = (JPanel)tabPane.getSelectedComponent();
//       if ( panel != null )
//          explainLabel.setText( (String)panel.getClientProperty( "explain" ) );
//    }
    
    if ( evt.getSource() == expIntervalSpinner ) {
       Log.log( 9, "(EditorDialog.Actions.stateChanged) for expIntervalSpinner" );
       if ( spinnerAction != null ) {
          spinnerAction.cancel();
       }
       
       spinnerAction = new TimesRunnable( 1 ); 
       Global.getTimer().schedule(spinnerAction, SPINNERACTION_DELAYTIME);
    }
}
	
@Override
public void readData() {
//   setTimeLabel( recModLabel, record.getModifiedTime() );
   setTimeLabel( recModLabel, record.getModifiedTime() );
   setTimeLabel( passModLabel, record.getPassModTime() );
   setTimeLabel( accessLabel, record.getAccessTime() );
   setTimeLabel( expiryLabel, record.getPassLifeTime() );
   refreshExpireFields();
   
   // short-key value
   KeyStroke shortKey = record.getKeyboardShortcut();
   String text = shortKey == null ? null : keyStrokeText(shortKey);
   shortKeyValueLabel.setText(text);
   
}

@Override
public void writeData() {
    if ( spinnerAction != null ) {
        spinnerAction.cancel();
        spinnerAction.run();
     }

}

   /** Sets the password lifetime combo and the period spinner after record values
    * PASSLIFETIME, PASSMODTIME and INTERVAL.
    * (Does not cause a selection changed action in the controls.)
    */
   private void setPassLifeControls () {
      Log.log( 8, "(EditorDialog.setPassLifeCombo) enter" );
      actionPause = true;
      
      long lifeTime = record.getPassLifeTime();
      long modTime = record.getPassModTime();
      int exp = record.getExpiryInterval();
      
      // "life" is seconds distance between pass-modified and pass-death time
      int life = (int)((lifeTime - modTime) / 1000);
      // "expiry interval" is days of the period of password validity
      int index = -1;
      
      // 1. rank: no pass-lifetime set == UNLIMITED
      if ( record.getPassLifeTime() == 0 )
         index = LIFE_UNLIMITED;

      // 2. rank: an expiry interval is set (EDITABLE-PERIOD or SELECTABLE-PERIOD)
      else if ( exp==30 | exp==60 | exp==91 | exp==182 | exp==273 | exp==365 | exp==730 
                | exp==1095 | exp==1460 | exp==1825 | exp==3650 )
         life = exp * DAYSECS;
      else if ( exp > 0 )
         index = LIFE_PERIOD;
      
      // 3. rank: something very special is set (likely irregular data)
      else if ( life < 0 | life > Integer.MAX_VALUE )
         index = LIFE_SPECIAL;
      
      // if no priority, try setup any selectable period
      // if not possible, render "SPECIAL" selected
      if ( index == -1 ) 
         switch ( life )
         {
            case 30 * DAYSECS :  index = 1;
            break;
            case 60 * DAYSECS :  index = 2;
            break;
            case 91 * DAYSECS :  index = 3;
            break;
            case 182 * DAYSECS :  index = 4;
            break;
            case 273 * DAYSECS :  index = 5;
            break;
            case 365 * DAYSECS :  index = 6;
            break;
            case 2 * 365 * DAYSECS :  index = 7;
            break;
            case 3 * 365 * DAYSECS :  index = 8;
            break;
            case 4 * 365 * DAYSECS :  index = 9;
            break;
            case 5 * 365 * DAYSECS :  index = 10;
            break;
            case 10 * 365 * DAYSECS :  index = 11;
            break;
            default: index = LIFE_SPECIAL;
         }
      
      // set the combo
      expireCombo.setSelectedIndex( index );
      
      // we also set up the expiry interval spinner
      expIntervalPanel.setVisible( index == LIFE_PERIOD );
      expIntervalSpinner.setValue( new Integer( exp ) );

      actionPause = false;
      Log.log( 8, "(EditorDialog.setPassLifeCombo) exit" );
   }  // setPassLifeControls

   private String keyStrokeText ( KeyStroke stroke ) {
	   if ( stroke == null ) return "null";
	   String hstr = stroke.toString();
	   hstr = Util.substituteText(hstr, "pressed ", "");
	   return hstr.toUpperCase();
   }

   /** Calculates the PASSLIFETIME value after the parameter number of
    *  days of lifetime interval. Sets the value of the record.
    *  
    *  @param days int lifetime interval in days
    *  @param undoable boolean whether undoable edit is created 
    */  
   private void setPassLifeTimeAfterInterval ( int days, boolean undoable ) {
      correctRecord();
      long value = record.getPassModTime() + (long)days * DAYSECS * 1000;
      setPassLifeTime( value, days, undoable );
   }

   /** Sets PASSLIFETIME record value, label text and combo display.
    *  Arranges dialog display elements according to new expire status.
    *  
    *  @param value long the new time value in epoch milliseconds
    *  @param interval int new expire interval to be set alongside with 
    *                  expire time; if value<0, no interval will be set 
    *  @param undoable boolean if <b>true</b> a change in value will be 
    *         undoable in UI 
    */
   private void setPassLifeTime ( long value, int interval, boolean undoable ) {
      Log.log( 8, "(EditorDialog.setPassLifeTime) enter" );
      
      int oldInterval = record.getExpiryInterval();
      long oldPassLifeTime = record.getPassLifeTime();
      
      // set values in objects
      if ( interval >= 0 ) {
         record.setExpiryInterval( interval );
      }
      
      if ( value == record.getPassModTime() ) {
         value = 0;
      }
      record.setPassLifeTime( value );
      value = record.getPassLifeTime();
      refreshExpireFields();

      // create undoable edit event
      if ( undoable && oldPassLifeTime != value ) {
         Log.debug( 7, "(EditorDialog.setPassLifeTime) creating UNDOABLE EDIT for Expire Time" );
         UndoableEdit edit = undoManager.new FieldEdit( oldPassLifeTime, oldInterval );
         undoManager.undoableEditHappened( new UndoableEditEvent( this, edit ) );
      }
      Log.log( 8, "(EditorDialog.setPassLifeTime) exit" );
   }

   /** Calculates the PASSLIFETIME value after the parameter combo-box index
    *  for all index cases except PERIOD and SPECIAL.
    *  Sets PASSLIFETIME of the record.
    * 
    *  @param index int passlife combo index
    *  @param undoable boolean whether undoable edit is created 
    */ 
   private void setPassLifeTimeAfterIndex ( int index, boolean undoable ) {
      long h, value = 0;
      int days = 0;
      
      // only regard the "selectable period" indices
      if ( index < 0 | index > 11 ) return;
      
      Log.log( 8, "(EditorDialog.Times_WorkPanel.setPassLifeTimeAfterIndex) enter with index=".concat( 
            String.valueOf( index )) );

      if ( index > 0 ) {
         switch ( index ) {
            case 1 : days = 30;
            break;
            case 2 : days = 60;
            break;
            case 3 : days = 91;
            break;
            case 4 : days = 182;
            break;
            case 5 : days = 273;
            break;
            case 6 : days = 365;
            break;
            case 7 : days = 2 * 365;
            break;
            case 8 : days = 3 * 365;
            break;
            case 9 : days = 4 * 365;
            break;
            case 10 : days = 5 * 365;
            break;
            case 11 : days = 10 * 365;
            break;
         }

         // the new value for record PASSLIFETIME
         correctRecord(); 
         h = record.getPassModTime();
         value = h + (long)days * DAYSECS * 1000;
      }

      // set record value, label text and combo display
      setPassLifeTime( value, days, undoable );
      Log.log( 8, "(EditorDialog.Times_WorkPanel.setPassLifeTimeAfterIndex) exit with value set = " + value ); 
   }  // setPassLifeTimeAfterIndex

   private void refreshExpireFields () {
      setTimeLabel( expiryLabel, record.getPassLifeTime() );
      setPassLifeControls();
      expiryLabel.setBlinking( record.hasExpired() );
      toolbar.updateExpireStates();
   }

@Override
public void notifyFieldValue (String field, Object value, EditorWorkPanel source) {
	if ( field == null | source == this ) return;
	
	if ( field.equals("TIMES.UPDATE") | field.equals("LOGIN.PASSWORD") ) {
		readData();
	}
}

private void setupShortcutKey () {
  KeyStroke stroke = record.getKeyboardShortcut();
  final JLabel keyLabel = new JLabel( stroke == null ? null : stroke.toString() );
  final JLabel keyDisplayLabel = new JLabel( stroke == null ? " " : keyStrokeText(stroke) );
  final Set<KeyStroke> accKeys = MenuHandler.getAccelerators();
  
  String text = ResourceLoader.getDisplay("msg.edit.shortcutkey");
  JLabel textLabel = new JLabel(text);
  textLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
  Border border = BorderFactory.createEmptyBorder(8, 8, 8, 8);
  border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), border);
  keyDisplayLabel.setBorder(border);
  Font font = DisplayManager.getFont("data").deriveFont(Font.PLAIN, (float)14);
  keyDisplayLabel.setFont(font);
  final JLabel msgLabel = new JLabel(" ");
  msgLabel.setForeground(Color.RED);
  VerticalFlowLayout fLayout = new VerticalFlowLayout(10);
  fLayout.setAlignment( VerticalFlowLayout.CENTER );
  JPanel panel = new JPanel(fLayout);
  panel.add(textLabel);
  panel.add(keyDisplayLabel);
  panel.add(msgLabel);
  
  final ButtonBarDialog dlg = new ButtonBarDialog(EditorDialog.this, panel, 
		  DialogButtonBar.OK_CANCEL_BUTTON, true);
  dlg.setTitle( ResourceLoader.getDisplay("dlg.edit.shortcutkey") );
  dlg.moveRelatedTo(EditorDialog.this);
  
  // install a listener to key input
  KeyListener keyListener =  new KeyAdapter() {

	@Override
	public void keyPressed(KeyEvent e) {
		// receive entered key
		int keyCode = e.getKeyCode();
		int modif = e.getModifiers();
		KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, modif);
		Log.log(10, "(EditorDialog.Time_WorkPanel.KeyAdapter) -- received KEY event: " + keyCode + " - " + modif + ", stroke: " + stroke);
	
		if ( isAllowedKeyStroke(stroke) ) {
			// show and accept entered stroke
			keyDisplayLabel.setText(keyStrokeText(stroke));
			keyLabel.setText(stroke.toString());
			e.consume();
			String text = null;

			// warning about conflict with other record
			PwsRecord rec = conflictingRecord(stroke); 
			if ( rec != null ) {
				text = ResourceLoader.getDisplay("msg.edit.conflictkey");
				text = Util.substituteText(text, "$record", rec.getTitle());
			}
			
			// warning about accelerator conflict
		    else if ( accKeys.contains(stroke) ) {
				text = ResourceLoader.getDisplay("msg.edit.reservedkey");
				text = Util.substituteText(text, "$key", keyStrokeText(stroke));
		    }

			msgLabel.setText(text);
		}
	}

	/** Returns the first record in the natural order of the database
	 * which contains the given shortcut key.
	 *  
	 * @param stroke <code>KeyStroke</code>
	 * @return <code>PwsRecord</code> or null if not found
	 */
	private PwsRecord conflictingRecord (KeyStroke stroke) {
		for (Iterator<PwsRecord> it = container.iterator(); it.hasNext();) {
			PwsRecord record = it.next();
			KeyStroke recStroke = record.getKeyboardShortcut(); 
			if ( recStroke != null && !record.equals(initRec) && 
				 recStroke.equals(stroke) ) 
				return record;
		}
		return null;
	}

	/** Whether the given key-stroke is not a case of the basic
	 * functional restriction set.
	 * 
	 * @param stroke <code>KeyStroke</code>
	 * @return boolean true == key is allowed
	 */
	private boolean isAllowedKeyStroke (KeyStroke stroke) {
		if ( stroke == null || Global.forbiddenKeys.contains(stroke) ) return false;
		int code = stroke.getKeyCode();
		if ( code == 0 | code == KeyEvent.VK_SHIFT | code == KeyEvent.VK_CONTROL
			 | code == KeyEvent.VK_ALT | code == KeyEvent.VK_ALT_GRAPH 
			 | code == KeyEvent.VK_META ) return false;
		return true;
	}
  };
  
  // add "Remove" button to button bar, including action
  JButton removeButton = new JButton(ResourceLoader.getDisplay("button.remove")); 
  removeButton.addActionListener( new ActionListener() {

	@Override
	public void actionPerformed(ActionEvent e) {
	  dlg.dispose();
	  record.setKeyboardShortcut(null);
	  shortKeyValueLabel.setText(null);
	  Log.debug(10, "(EditorDialog.Times_WorkPanel.actionPerformed) removed shortcut-key assignment");
	}
  });
  
  removeButton.addKeyListener(keyListener);
  dlg.getButtonBar().getOkButton().addKeyListener(keyListener); 
  dlg.getButtonBar().getCancelButton().addKeyListener(keyListener);
  dlg.getButtonBar().add(removeButton);
  dlg.show();
  
  if ( dlg.isOkPressed() ) {
	  stroke = KeyStroke.getKeyStroke(keyLabel.getText());
	  record.setKeyboardShortcut(stroke);
	  shortKeyValueLabel.setText(keyDisplayLabel.getText());
	  Log.debug(10, "(EditorDialog.Times_WorkPanel.setupShortcutKey) set new shortcut-key: " + stroke);
  }
}

@Override
public boolean panelClosing() {
   Global.removeTimePulseListener( expiryLabel );
   return true;
}
   
}

//  ***********  INNER CLASSES  *************

private static class DlgPasswordField extends EditorTextField
{
   private static final String HIDEDUMMY = "**********";
   private PwsPassphrase passdata;
   private boolean isHidden;

   public DlgPasswordField () {
      this( null );
   }

   public DlgPasswordField ( PwsPassphrase pass ) {
      super( TEXTFIELDSIZE );
      setHidden( true );
      setPassphrase( pass );
   }
/*   
   public boolean isHidden ()
   {
      return isHidden;
   }
*/   
   public void setHidden ( boolean v ) {
      if ( v != isHidden ) {
         isHidden = v;
         if ( isHidden ) {
            super.setText( HIDEDUMMY );
            setEditable( false );
            setPopupActive( false );
         } else {
            super.setText( passdata.getString() );
            setEditable( true );
            setPopupActive( true );
         }
         clearUndoList();
      }
   }

   /**
    * Returns the passphrase of this field in its current edit value
    * (as a clone). Returns <b>null</b> for an empty passphrase.
    * 
    * @return <code>PwsPassphrase</code>
    */
   public PwsPassphrase getPassphrase () {
      if ( !isHidden ) {
         passdata.setValue( super.getText().toCharArray() );
      }
      return passdata.isEmpty() ? null : (PwsPassphrase)passdata.clone();
   }

   /**
    * Sets the current edit value of this password field
    * with an encrypted passphrase value.
    * 
    * @param pass <code>PwsPassphrase</code>; <b>null</b> for clearing
    */
   public void setPassphrase ( PwsPassphrase pass ) {
      if ( pass == null ) {
         passdata = new PwsPassphrase();
      } else { 
         passdata = pass;
      }
      
      if ( !isHidden ) {
         super.setText( passdata.getString() );
      }
   }

   /**
    * Sets the current edit value of this password field
    * with a plain text value.
    * 
    * @param text 
    */
   @Override
public void setText ( String text ) {
      if ( text == null )
         throw new NullPointerException();
      
      passdata.setValue( text );
      
      if ( !isHidden ) {
         super.setText( passdata.getString() );
      }
   }
/*   
   public String getText ()
   {
      if ( !isHidden )
         return super.getText();
      return passdata.getString();
   }
*/   
}  // class DlgPasswordField

private class OurNotesTextArea extends NotesTextArea implements ActionListener {
	
	private JPanel				notesPanel;
	private JScrollPane			notesScrollPane;
	private JButton 			tearOffButton;
	private ButtonBarDialog 	tearoffNotesDialog;
	
    public OurNotesTextArea (String name) {
    	super(name);
    	init();
	}

    private void init () {
        notesScrollPane  = new JScrollPane( this );
        tearOffButton = new JButton( ResourceLoader.getImageIcon( "edittearoff" ) );
        notesPanel = new JPanel( new BorderLayout() );
        notesPanel.setPreferredSize( new Dimension( 280, 128 ) );
        notesPanel.add( notesScrollPane );

        tearOffButton.setActionCommand( "button.tearoff.notes" );
        tearOffButton.addActionListener( this );
        tearOffButton.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 3 ) );
        tearOffButton.setBackground( new JPanel().getBackground() );
        tearOffButton.setToolTipText( ResourceLoader.getCommand( "tooltip.button.tearoffnotes" ));
        tearOffButton.setFocusPainted( false );

    }
    
	@Override
    protected JPopupMenu getPopupMenu () {
       JPopupMenu menu;
       JMenuItem item;
        
       menu = super.getPopupMenu();
       if ( tearoffNotesDialog == null ) {
          item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.tearoff" ) );
          item.setActionCommand( "button.tearoff.notes" );
          item.addActionListener( this );
          menu.insert( item, menu.getComponentCount() - 2 );
       }
       return menu;
    }

	/** Returns the view panel of the text area, including a JScrollPane.
	 * 
	 * @return JPanel
	 */
	public JPanel getView () {
		return notesPanel;
	}
	
	/** Returns the action button for tearing off a separate text editor
	 * for this text area.
	 * 
	 * @return JButton
	 */
	public JButton getTearoffButton () {
		return tearOffButton;
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
	   String command = evt.getActionCommand();
	   
       if ( command.equals( "button.tearoff.notes" ) ) {
          // create free editor window
          JPanel panel = new JPanel( new BorderLayout() );
          panel.setPreferredSize( new Dimension( 300, 180 ) );
          panel.add( notesScrollPane, BorderLayout.CENTER );
          tearoffNotesDialog = new ButtonBarDialog( EditorDialog.this, panel, 
         		 DialogButtonBar.OK_BUTTON, false )
          {
             @Override
 			public void dispose () {
                // re-install "regular" editor place
                if ( tearoffNotesDialog != null ) {
                   // optionally remember window bounds
                   if ( Options.isOptionSet( "rememberScreen" ) ) {
                      storeBounds( Options.getOptions(), "tearoff_notes", true );
                   }
                 
                   notesPanel.add( notesScrollPane );
                   notesPanel.getParent().validate();
                   tearOffButton.setEnabled( true );
                   tearoffNotesDialog = null;
                   objectListener.activity();
                }

                super.dispose();
             }
          };
          
          String title = ResourceLoader.getDisplay("adddlg.label.notes") + " - " 
        		         + record.getTitle();
          title = Util.leadingStr(title, 120);
          tearoffNotesDialog.setTitle( title );
          tearoffNotesDialog.moveRelatedTo( EditorDialog.this );
          tearoffNotesDialog.setResizable( true );
          tearoffNotesDialog.setAutonomous( true );
          
          // optionally restore last remembered window bounds (if available)
          if ( Options.isOptionSet( "rememberScreen" ) ) {
             tearoffNotesDialog.gainBounds( Options.getOptions(), "tearoff_notes", true );
          }

          // disable "regular" editor place
          tearOffButton.setEnabled( false );
          notesPanel.repaint();
          
          tearoffNotesDialog.show();
       }
	}
}

private static class PasswordHistoryEvent extends EventObject {
	PwsPassphrase oldPass;
	long oldTime;
	
	public PasswordHistoryEvent ( Object source, 
			                      PwsPassphrase oldPass, long oldTime ) {
		super(source);
		if ( oldPass == null )
			throw new NullPointerException("password is null");
		
		this.oldPass = oldPass;
		this.oldTime = oldTime;
	}
	
}

}
