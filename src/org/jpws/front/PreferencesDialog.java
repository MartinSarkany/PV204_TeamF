/*
 *  PolicyDialog in org.jpws.front
 *  file: PolicyDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 01.07.2005
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jpws.data.Options;
import org.jpws.front.PolicyDialog.PolicyDialogPanel;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.ButtonBarListener;
import org.jpws.front.util.DateTimeFormat;
import org.jpws.front.util.DateTimeFormat.DateFormat;
import org.jpws.front.util.DateTimeFormat.TimeFormat;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.FontChooser;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.SpringUtilities;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.global.Log;


/**
 * Creates the preference dialog box and handles user interaction with it.
 * The constructed instance does auto-display. 
 * 
 * @author Wolfgang Keller
 * @since 0-5-0 subclass of ButtonBarDialog
 */
public class PreferencesDialog extends ButtonBarDialog //implements Runnable
{
   protected static final long DATEFORMAT_DEMOTIME = new Date( 112, 9, 5).getTime() + 
                             14*Global.HOUR + 35*Global.MINUTE + 12000;
   protected static final String NATIVE = "NATIVE";
   protected static final Border PANEL_BORDER = 
                          BorderFactory.createEmptyBorder( 8, 8, 18, 8 );
   protected static final int MIN_IDLE_MIN = 1; 
   protected static final int MAX_IDLE_MIN = 999; 
   protected static final int MIN_CLIPBOARD_SEC = 10; 
   protected static final int MAX_CLIPBOARD_SEC = 300; 
   protected static final LookAndFeelInfo[] LAFs = UIManager.getInstalledLookAndFeels();
   
   private  ArrayList<ChangeListener>   changeListeners = new ArrayList<ChangeListener>();
   protected GeneralPanel           generalPanel;
   protected OptionPanel[]          panelSet;
   protected OptionPanel            activePanel;
   protected Font                   textLabelFont;

   protected CardLayout             cardLayout;
   protected JPanel                 dlgPanel;
   protected JList                  lister;
   protected X                      xListener;
   protected boolean                okPressed;

   private static class DateFormatOption {
       private DateTimeFormat.DateFormat format;
       
       public DateFormatOption ( DateTimeFormat.DateFormat f ) {
           if ( f == null )
               throw new NullPointerException();
           this.format = f;
       }
       public DateFormatOption ( String name ) {
          DateFormat df =  DateFormat.forName(name);
          if ( df == null )
             df = DateFormat.VM_default;
          this.format = df;
       }
       public DateTimeFormat.DateFormat getFormat () {
           return format;
       }
       @Override
	public String toString () {
           if ( format.equals(DateFormat.VM_default) ) {
               return NATIVE;
           }
           return new DateTimeFormat( format ).renderDate(DATEFORMAT_DEMOTIME, null);
       }
       /** Options equal if their date format equal in name. */ 
       @Override
	public boolean equals ( Object obj ) {
           return obj != null && ((DateFormatOption)obj).getFormat().name()
                  .equals(this.format.name()); 
       }
   }
   
   private DateFormatOption[] dateFormatOptions =
       { new DateFormatOption( DateFormat.VM_default ), 
         new DateFormatOption( DateFormat.standard_short ),  
         new DateFormatOption( DateFormat.standard_long ),  
         new DateFormatOption( DateFormat.standard_text ),  
         new DateFormatOption( DateFormat.technical ),  
         new DateFormatOption( DateFormat.USA_text ),  
         new DateFormatOption( DateFormat.USA_short ),  
         new DateFormatOption( DateFormat.USA_long )  
       };

   private static class TimeFormatOption {
       private DateTimeFormat.TimeFormat format;
       
       public TimeFormatOption ( DateTimeFormat.TimeFormat f ) {
           if ( f == null )
               throw new NullPointerException();
           this.format = f;
       }
       public TimeFormatOption ( String name ) {
          TimeFormat tf =  TimeFormat.forName(name);
          if ( tf == null )
             tf = TimeFormat.VM_default;
          this.format = tf;
       }
       public DateTimeFormat.TimeFormat getFormat () {
           return format;
       }
       @Override
	public String toString () {
           if ( format.equals(TimeFormat.VM_default) ) {
               return NATIVE;
           }
           return new DateTimeFormat( format ).renderTime(DATEFORMAT_DEMOTIME, null);
       }
       /** Options equal if their date format equal in name. */ 
       @Override
	public boolean equals ( Object obj ) {
           return obj != null && ((TimeFormatOption)obj).getFormat().name()
                  .equals(this.format.name()); 
       }
   }
   
   private TimeFormatOption[] timeFormatOptions =
       { new TimeFormatOption( TimeFormat.VM_default ), 
         new TimeFormatOption( TimeFormat.standard_short ),  
         new TimeFormatOption( TimeFormat.standard_long ),  
         new TimeFormatOption( TimeFormat.English_short ),  
         new TimeFormatOption( TimeFormat.English_long )  
       };

   /** Constructor for a Dialog parent.
    * 
    * @param owner  the parent <code>Dialog</code>.
    * @param modal whether the dialog is in modal modus
    * 
    * @throws java.awt.HeadlessException
    */
   public PreferencesDialog( Dialog owner, boolean modal ) 
          throws HeadlessException
   {
      super( owner, DialogButtonBar.OK_CANCEL_BUTTON, modal );
      init( owner );
   }

   /** Constructor for a Frame parent.
    * 
    * @param owner  the parent <code>Frame</code>.
    * @param modal whether the dialog is in modal modus
    * 
    * @throws java.awt.HeadlessException
    */
   public PreferencesDialog( Frame owner ) 
          throws HeadlessException
   {
      super( owner, DialogButtonBar.OK_BUTTON, false );
      init( owner );
   }
/*
   public void finalize ()
   {
      System.out.println( "-- finalize PreferencesDialog" );
   }
*/   
   private void init ( Component owner ) {
	  if (!markSingleton("PreferencesDialog"))
	   	  throw new IllegalStateException("multiple dialog instantiation");

      xListener = new X();
      textLabelFont = DisplayManager.getFont( "control" );

      panelSet = new OptionPanel[8];
      generalPanel = new GeneralPanel();
      panelSet[ 0 ] = generalPanel;
      panelSet[ 1 ] = new SecurityPanel();
      panelSet[ 2 ] = new DisplayPanel();
      panelSet[ 3 ] = new EditorPanel();
      panelSet[ 4 ] = new ReportsPanel();
      panelSet[ 5 ] = new ConfirmPanel();
      panelSet[ 6 ] = new FontsPanel();
      panelSet[ 7 ] = new PassPolicyPanel();
      
      String ttitle = "dlg.preferences";
      setTitle( ResourceLoader.getDisplay( ttitle ) );
      setAutonomous( true );

      buildButtonPanel();
      buildTopPanel();
      buildCentrePanel();

      lister.setSelectedIndex( 0 );
   }
   
   @Override
   public void dispose () {
      // continue with window destruction
      super.dispose();
      
      if ( panelSet != null ) {
         for ( int i = 0; i < panelSet.length; i++ ) {
            panelSet[i].destruct();
            panelSet[i] = null;
         }
      }
      panelSet = null;

      if ( lister != null )
         lister.removeListSelectionListener( xListener );
      
      lister = null;
      xListener = null;
      activePanel = null;
      
      getContentPane().removeAll();
   }
   
   protected void runEndActivity ( final OptionPanel panel ) {
	  if (panel != null) {
         panel.endActivity();
	  }
   }  // runEndActivity

   
   private void buildButtonPanel() {
      ButtonBarListener barListener = new DefaultButtonBarListener () {
         @Override
		 public boolean okButtonPerformed () {
            if ( activePanel != null ) {
               if ( !activePanel.validated() ) return false;
               runEndActivity( activePanel );
            }
            dispose();
            return true;
         }
         
         @Override
		 public void cancelButtonPerformed () {
            runEndActivity( activePanel );
            dispose();
         }

		@Override
		public boolean extraButtonPerformed (Object button) {
			Log.log(10, "(PreferencesdDialog.ButtonBarListener) extra button performed, panel=" 
					+ activePanel);
			if ( activePanel != null && activePanel.validateData() ) {
               runEndActivity( activePanel );
			}
            return true;
		}
      };
      
      addButtonBarListener( barListener );
      
      JButton applyButton = new JButton("Apply");
      getButtonBar().add(applyButton);
   }

   private void buildCentrePanel()
   {
      JPanel container, panel;
      int i;
      
      // create the flippable display panel
      cardLayout = new CardLayout();
      dlgPanel = new JPanel( cardLayout );
      dlgPanel.setBorder( BorderFactory.createCompoundBorder( 
            BorderFactory.createEmptyBorder( 4, 4, 0, 4 ), 
            BorderFactory.createEtchedBorder() ));
      
      // add the panels to the card-layout
      for ( i = 0; i < panelSet.length; i++ ) {
         dlgPanel.add( panelSet[i], String.valueOf( i ) );
      }

      // create the selection lister
      lister = new JList( panelSet );
      lister.addListSelectionListener( xListener );
      lister.setCellRenderer( new ListCellView() );
      if (lister.getSelectionBackground() == null) {
    	  lister.setSelectionBackground(TableHandler.DEFAULT_SELECTION_BGD_COLOR);
      }
      
      panel = new JPanel( new BorderLayout() );
      panel.add( lister, BorderLayout.CENTER );
      panel.setBorder( BorderFactory.createCompoundBorder( 
            BorderFactory.createEmptyBorder( 4, 4, 0, 0 ),
            BorderFactory.createEtchedBorder()) );

      // compose container
      container = new JPanel( new BorderLayout() );
      container.add( panel, BorderLayout.WEST );
      container.add( dlgPanel, BorderLayout.CENTER );
      
      setDialogPanel( container );
   }  // buildCentrePanel

   private void buildTopPanel()
   {
   }
   
   protected void errorMessage ( String text )
   {
      GUIService.infoMessage( this, "dlg.badvalue", text );      
   }
   
   protected JCheckBox makeCheckBox ( String text )
   {
      JCheckBox chk = new JCheckBox( ResourceLoader.codeOrRealDisplay( text ) );
      chk.setIconTextGap( 10 );
      return chk;
   }
   
   protected JTextField makeNumField ()
   {
      JTextField fld;
      
      fld = new JTextField( 3 );
      fld.setHorizontalAlignment( JTextField.RIGHT );
      fld.setBorder( BorderFactory.createLineBorder( Color.darkGray ) );
      return fld;
   }
   
   protected JLabel makeTextLabel ( String text )
   {
       text = ResourceLoader.codeOrRealDisplay( text );
       JLabel label = new JLabel( text );
       label.setFont(textLabelFont);
       
       return label;
   }
   
   protected int getFieldInt ( JTextField fld )
   {
      int i = -1;
   
      try { i = Integer.parseInt( fld.getText() ); }
      catch ( Exception e )
      {
         errorMessage( "prefmsg.numinputfault" );
      }
      return i;
   }

   /**
    *  Adds a change listener to this class.
    * 
    * @param li javax.swing.event.ChangeListener
    * @return <b>true</b> if and only if the parameter object was added to 
    *         the list of listeners 
    */
   public boolean addChangeListener ( ChangeListener li )
   {
      synchronized( changeListeners )
      {
         if ( !changeListeners.contains( li ) )
         {
            changeListeners.add( li );
            return true;
         }
         return false;
      }
   }

   /**
    *  Removes a change listener from this class.
    * 
    * @param li javax.swing.event.ChangeListener
    */
   public void removeChangeListener ( ChangeListener li )
   {
      synchronized( changeListeners )
      { changeListeners.remove( li ); }
   }

   //  ****************  INNER CLASSES  *******************
   
   /**
    * Fires a change event to the list of registered change listeners.
    */
   @SuppressWarnings("unchecked")
   protected void fireChangeEvent ()
   {
      ArrayList<ChangeListener> clients;
      ChangeEvent evt;
      int i;
      
      synchronized ( changeListeners )
      {
         clients = (ArrayList<ChangeListener>)changeListeners.clone();
         evt = new ChangeEvent( this );
         for ( i = 0; i < clients.size(); i++ )
            clients.get( i ).stateChanged( evt );
      }
   }

   private class X implements ListSelectionListener, ActionListener 
   {
   
      @Override
	  public void valueChanged ( ListSelectionEvent e ) {
         OptionPanel panel;
         int index;
         
         index = lister.getSelectedIndex();
         if ( index > -1 & index < panelSet.length )
         {
            panel = panelSet[ index ];
            if ( activePanel != panel )
            {
               // terminate activity of currently installed panel
               if ( activePanel != null )
               {  
                  if ( !activePanel.validated() )
                     return;

                  runEndActivity( activePanel );
               }
               
               // install selected panel
               cardLayout.show( dlgPanel, String.valueOf( index ) );
               activePanel = panel;
               activePanel.startActivity();
               ActionHandler.resetIdleTime();
            }
         }
      }
      
      @Override
	  public void actionPerformed ( ActionEvent e )  {
         String command = e.getActionCommand();

         if ( command.equals( "dialog.browseroption" )) {
            Service.defaultBrowserInstall();
//          GUIService.editBrowserOption( PreferencesDialog.this );
            generalPanel.browserFld.setText( Options.getOption("browserApplication") );
         }

         if ( command.equals( "dialog.emailoption" )) {
//             Service.defaultBrowserInstall();
             GUIService.editApplicationOption( PreferencesDialog.this, "dlg.choosemailclient", "emailApplication" );
             generalPanel.emailFld.setText( Options.getOption("emailApplication") );
          }
      }
   }
   
   private static class ListCellView implements ListCellRenderer 
   {
      private static JLabel label;
      
      public ListCellView ()
      {
         Font font;
         
         label = new JLabel();
         font = label.getFont();
         label.setFont( font.deriveFont( font.getSize2D() + 1 ) );
         label.setBorder( BorderFactory.createEmptyBorder( 3, 5, 3, 8 ) );
         label.setOpaque(true);
         
      }
      
      // This is the only method defined by ListCellRenderer.
      // We just reconfigure the JLabel each time we're called.

      @Override
	public Component getListCellRendererComponent(
        JList list,
        Object value,            // value to display
        int index,               // cell index
        boolean isSelected,      // is the cell selected
        boolean cellHasFocus)    // the list and the cell have the focus
      {
          label.setText( value.toString() );
          if (isSelected) {
              label.setBackground( list.getSelectionBackground() );
              label.setForeground( list.getSelectionForeground() );
          } else {
             label.setBackground( list.getBackground() );
             label.setForeground( list.getForeground() );
          }
          label.setEnabled( list.isEnabled() );
          return label;
      }
   }
   
   private class GeneralPanel extends OptionPanel
   {
      private String title; 
      private String browserStartVal;
      private String emailStartVal;
      private boolean validated;
      private int initLAF;
      private int initLocale;
      
      private JButton browserButton;
      private JButton emailButton;
      private JTextField browserFld;
      private JTextField emailFld;
      private JComboBox localeCbo;
      private JComboBox lookAndFeelCbo;
      private JCheckBox reopenChk;
      private JCheckBox useRecentChk;
      private JCheckBox autoFlushChk;
      private JCheckBox remScreenChk;
      private JCheckBox storeMinorsChk;
      private JCheckBox autoCopyPassChk;
      
      public GeneralPanel () {
         title = ResourceLoader.getDisplay("prefpanel.general");
         init();
      }
      
      private void init () {
         JLabel label;
         JPanel panel1, northPanel, applPanel;
         VerticalFlowLayout vflow;
         FlowLayout fLayout;
         JPanel box;
         int i;
         
         northPanel = new JPanel( new VerticalFlowLayout( 3 ) );
         this.add( northPanel, BorderLayout.NORTH );
         
         // panel for external application setup
         applPanel = new JPanel( new SpringLayout() );
         northPanel.add( applPanel );
         
         // the browser-app definition panel
         label = makeTextLabel("preflabel.browser");
         applPanel.add( label );
         fLayout = new FlowLayout( FlowLayout.LEFT );
         panel1 = new JPanel( fLayout );
         browserFld = new JTextField( 25 );
         panel1.add( browserFld );
         browserButton = new JButton( ResourceLoader.getDisplay("button.select") );
         browserButton.setActionCommand( "dialog.browseroption" );
         browserButton.addActionListener( xListener );
         panel1.add( browserButton );
         applPanel.add( panel1 );

         // the email-app definition panel
         label = makeTextLabel("preflabel.mailclient");
         applPanel.add( label );
         fLayout = new FlowLayout( FlowLayout.LEFT );
         panel1 = new JPanel( fLayout );
         emailFld = new JTextField( 25 );
         panel1.add( emailFld );
         emailButton = new JButton( ResourceLoader.getDisplay("button.select") );
         emailButton.setActionCommand( "dialog.emailoption" );
         emailButton.addActionListener( xListener );
         panel1.add( emailButton );
         applPanel.add( panel1 );
         SpringUtilities.makeCompactGrid(applPanel, 2, 2, 5, 0, 5, 0);

         // the Locale combo
         fLayout = new FlowLayout( FlowLayout.LEFT );
         panel1 = new JPanel( fLayout );
         label = makeTextLabel("preflabel.locale");
         panel1.add( label );
         localeCbo = new JComboBox();
         localeCbo.addItem( ResourceLoader.getDisplay("locale.en") );
         localeCbo.addItem( ResourceLoader.getDisplay("locale.de") );
         localeCbo.addItem( ResourceLoader.getDisplay("locale.es") );
         panel1.add( localeCbo );
         northPanel.add( panel1 );

         // the look-and-feel combo
         panel1.add( Box.createHorizontalStrut( 20 ) );
         label = makeTextLabel("preflabel.lookandfeel");
         panel1.add( label );
         lookAndFeelCbo = new JComboBox();
         lookAndFeelCbo.addItem( Global.LAF_CROSSPLATFORM_OPTION );
         lookAndFeelCbo.addItem( Global.LAF_NATIVE_OPTION );
         for ( i = 0; i < LAFs.length; i++ )
            lookAndFeelCbo.addItem( LAFs[ i ].getName() );
         panel1.add( lookAndFeelCbo );
         
         // the boolean options
         vflow = new VerticalFlowLayout( 3 );
         box = new JPanel( vflow );
         box.add( Box.createVerticalStrut( 25 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.gui") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 4, 8, 0 ));
         box.add( label );
         
         autoFlushChk = makeCheckBox("prefbox.autoflush");
         box.add( autoFlushChk );
         storeMinorsChk = makeCheckBox("prefbox.storeminordata");
         box.add( storeMinorsChk );

         useRecentChk = makeCheckBox("prefbox.userecent");
         box.add( useRecentChk );
         reopenChk = makeCheckBox("prefbox.reopen");
         box.add( reopenChk );

         remScreenChk = makeCheckBox("prefbox.remscreen");
         box.add( remScreenChk );

         autoCopyPassChk = makeCheckBox("prefbox.autocopypass");
         box.add( autoCopyPassChk );

         this.add( box, BorderLayout.CENTER );
      }
      
      @Override
	public void destruct ()
      {
         browserButton.removeActionListener( xListener );
      }
      
      @Override
	public String toString ()
      {
         return title;
      }

      @Override
	public boolean validateData ()
      {
/*
         String path;

         path = browserFld.getText();
         validated = path.equals("") || new File( path ).isFile();
         if ( !validated  )
         {
            errorMessage( "prefmsg.browser" );
            return false;
         }
*/         
         if ( localeCbo.getSelectedIndex() != initLocale ||
              lookAndFeelCbo.getSelectedIndex() != initLAF )
         {
            GUIService.infoMessage( PreferencesDialog.this, null, 
                  ResourceLoader.getDisplay( "prefmsg.reloadprogram" ));
         }
         validated = true;
         return validated;
      }  // validated
      
      @Override
	public void startActivity ()
      {
         String hstr, lafOption;
         int i, index;

         validated = false;
         
         browserStartVal = Options.getOption( "browserApplication" );
         browserFld.setText( browserStartVal );
         emailStartVal = Options.getOption( "emailApplication" );
         emailFld.setText( emailStartVal );

         // language combo
         hstr = Options.getOption( "locale" );
         if ( hstr.equals( "de" ) )
            i = 1;
         else if ( hstr.equals( "es" ) )
            i = 2;
         else if ( hstr.equals( "fr" ) )
            i = 3;
         else 
            i = 0;
         localeCbo.setSelectedIndex( i );
         initLocale = localeCbo.getSelectedIndex();
         
         // look-and-feel combo
         // look for list index of user opted LAF (and set selected)
         lafOption = Options.getOption( "lookAndFeel" );
         index = -1;
         if ( lafOption.equals( Global.LAF_CROSSPLATFORM_OPTION ) )
            index = 0;
         else if ( lafOption.equals( Global.LAF_NATIVE_OPTION ) )
            index = 1;
         else
         {
            for ( i = 0; i < LAFs.length; i++ )
               if ( LAFs[ i ].getClassName().equals( lafOption ) )
               {
                  index = i + 2;
                  break;
               }
         }
         // init the combo
         lookAndFeelCbo.setSelectedIndex( index );
         initLAF = lookAndFeelCbo.getSelectedIndex();

         // bool options
         autoFlushChk.setSelected( Options.isOptionSet("autoflush") );
         reopenChk.setSelected( Options.isOptionSet("reopenFile") );
         useRecentChk.setSelected( Options.isOptionSet("useRecentList") );
         remScreenChk.setSelected( Options.isOptionSet("rememberScreen") );
         storeMinorsChk.setSelected( Options.isOptionSet("storeMinorChanges") );
         autoCopyPassChk.setSelected( Options.isOptionSet("autoCopyPass") );

      }  // startActivity
      
      @Override
	public void endActivity ()
      {
         String hstr;
         int i;
         
         if ( !validated )
            return;
         
         // browser setting
         hstr = browserFld.getText();
         if ( !browserStartVal.equals( hstr ) ) {
            Options.setOption( "browserApplication", browserStartVal );
            Options.setOption( "browserApplication", hstr );
         }

         hstr = emailFld.getText();
         if ( !emailStartVal.equals( hstr ) ) {
            Options.setOption( "emailApplication", browserStartVal );
            Options.setOption( "emailApplication", hstr );
         }

         if ( (i = localeCbo.getSelectedIndex()) != initLocale )
         {
            switch ( i )
            {
               case 1 :  hstr = "de";
               break;
               case 2 :  hstr = "es";
               break;
               case 3 :  hstr = "fr";
               break;
               default : hstr = "en";
            }
            Options.setOption( "locale", hstr );
         }

         // Look-And-Feel setting
         if ( (i = lookAndFeelCbo.getSelectedIndex()) > -1 &&
               initLAF != i )
         {
            if ( i == 0 )
               hstr = Global.LAF_CROSSPLATFORM_OPTION;
            else if ( i == 1 )
               hstr = Global.LAF_NATIVE_OPTION;
            else 
               hstr = LAFs[ i - 2 ].getClassName();

            Options.setOption( "lookAndFeel", hstr );
//            System.out.println( "- setting user LAF option to: ".concat( hstr )); 
         }
         
         // bool option settings
         Options.setOption( "autoflush", autoFlushChk.isSelected() );
         Options.setOption( "reopenFile", reopenChk.isSelected() );
         Options.setOption( "useRecentList", useRecentChk.isSelected() );
         Options.setOption( "rememberScreen", remScreenChk.isSelected() );
         Options.setOption( "storeMinorChanges", storeMinorsChk.isSelected() );
         Options.setOption( "autoCopyPass", autoCopyPassChk.isSelected() );

         // update PWSLIB library setting
         org.jpws.pwslib.global.Global.setDisplayUsernames(
               Options.isOptionSet( "treeUsername" ) );
         
         // update menu handler
//         MenuHandler.setRecentMenuVisible( useRecentChk.isSelected() );
//         MenuHandler.setEntryListsVisible( storeMinorsChk.isSelected() );
         
      }  // endActivity
   }  // class GeneralPanel

   private class ConfirmPanel extends OptionPanel
   {
      private String title; 
      private boolean validated;
      
      private JCheckBox deleteChk;
      private JCheckBox updateChk;

      private JCheckBox copyClipChk;
      private JCheckBox saveFileChk;
      private JCheckBox backupChk;
      private JCheckBox revertChk;
      
      public ConfirmPanel ()
      {
         title = ResourceLoader.getDisplay("prefpanel.confirm");
         init();
      }
      
      private void init ()
      {
         JLabel label;
         Box box;
         Border boxBorder;
         
         boxBorder = BorderFactory.createEmptyBorder( 0, 3, 0, 0 );
         
         // the Ask Confirms box
         box = Box.createVerticalBox();
         box.setBorder( boxBorder );
         box.add( Box.createVerticalStrut( 5 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.askconfirm") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 4, 8, 0 ));
         box.add( label );
         deleteChk = makeCheckBox("prefbox.delentries");
         box.add( deleteChk );
         updateChk = makeCheckBox("prefbox.updatepass");
         box.add( updateChk );
         
         this.add( box, BorderLayout.NORTH );
   
         // the Report Confirms box
         box = Box.createVerticalBox();
         box.setBorder( boxBorder );
         box.add( Box.createVerticalStrut( 25 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.reportconfirm") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 4, 8, 0 ));
         box.add( label );
         
         copyClipChk = makeCheckBox("prefbox.copyclip");
         box.add( copyClipChk );
         saveFileChk = makeCheckBox("prefbox.savefile");
         box.add( saveFileChk );
         backupChk = makeCheckBox("prefbox.backup");
         box.add( backupChk );
         revertChk = makeCheckBox("prefbox.revert");
         box.add( revertChk );

         this.add( box, BorderLayout.CENTER );
      }
      
      @Override
	public void destruct ()
      {}
      
      @Override
	public String toString ()
      {
         return title;
      }
   
      @Override
	public boolean validateData ()
      {
         validated = true;
         return validated;
      }
      
      @Override
	public void startActivity ()
      {
         validated = false;
         
         deleteChk.setSelected( Options.isOptionSet("confirmDeleteRecord") );
         updateChk.setSelected( Options.isOptionSet("confirmUpdatePass") );

         copyClipChk.setSelected( Options.isOptionSet("confirmCopyClipboard") );
         saveFileChk.setSelected( Options.isOptionSet("confirmSave") );
         backupChk.setSelected( Options.isOptionSet("confirmBackup") );
         revertChk.setSelected( Options.isOptionSet("confirmRevert") );
         
      }  // startActivity
      
      @Override
	public void endActivity ()
      {
         if ( !validated )
            return;
            
         Options.setOption( "confirmDeleteRecord", deleteChk.isSelected() );
         Options.setOption( "confirmUpdatePass", updateChk.isSelected() );

         Options.setOption( "confirmCopyClipboard", copyClipChk.isSelected() );
         Options.setOption( "confirmSave", saveFileChk.isSelected() );
         Options.setOption( "confirmBackup", backupChk.isSelected() );
         Options.setOption( "confirmRevert", revertChk.isSelected() );
      }  // endActivity
      
   }

   private class NumberCheckBoxPanel extends JPanel
   {
      JCheckBox cbox;
      SpinnerNumberModel model;
      JSpinner spinner;
      int defaultVal, minVal, maxVal;
      
      private class CheckListener implements ItemListener
      {
         @Override
		public void itemStateChanged ( ItemEvent e )
         {
            boolean selected;
            
            selected = e == null ? false : e.getStateChange() == ItemEvent.SELECTED;
            spinner.setEnabled( selected );
            spinner.setBackground( selected ? Color.white : Color.lightGray );
         }
/*       
         protected void finalize () throws Throwable
         {
            super.finalize();
            System.out.println( "----- FINALIZE NumberCheckBoxPanel.CheckListener" ); 
         }
*/         
      }

      public NumberCheckBoxPanel ( int min, int max, int stepsize, int defvalue,
                                   String leader, String trailer )
      {
         super( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
         JLabel label;
         CheckListener checkListener;
         
         minVal = min;
         maxVal = max;
         defaultVal = defvalue;
         
         // Checkbox 
         checkListener = new CheckListener();
         cbox = makeCheckBox( leader );
         cbox.addItemListener( checkListener );

         // spinner box
         model = new SpinnerNumberModel( min, min, max, stepsize );
         spinner = new JSpinner( model );
         setValue( 0 );
         checkListener.itemStateChanged( null );

         // trailer label
         label = new JLabel( ResourceLoader.getDisplay( trailer ) );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 6, 0, 0 ) );
         label.setFont( textLabelFont );
         
         // Panel
         this.add( cbox );
         this.add( Box.createHorizontalStrut( 4 ) );
         this.add( spinner );
         this.add( label );
      }
      
      public void setValue ( int value )
      {
         if ( value < minVal | value > maxVal )
            value = defaultVal;
         model.setValue( new Integer( value ) );
      }
      
      public int getValue ()
      {
         return ((Integer)model.getValue()).intValue();
      }
      
      public void setSelected ( boolean v )
      {
         cbox.setSelected( v );
      }
      
      public boolean isSelected ()
      {
         return cbox.isSelected();
      }
   }  // class NumberCheckBoxPanel
   
   private class SecurityPanel extends OptionPanel
   {
      String title; 
      boolean validated;
      
      JCheckBox lockMinimizeChk;
      JCheckBox passCheckChk;
      JCheckBox openPassChk;
      JCheckBox useMirrorsChk;
      JCheckBox useUndoRedoChk;
      JCheckBox ftpCreateChk;
      JComboBox undoEntriesCbo;
      NumberCheckBoxPanel autoBackupNCP;
      NumberCheckBoxPanel autoClearNCP;
      NumberCheckBoxPanel lockIdleNCP;
      
      public SecurityPanel ()
      {
         title = ResourceLoader.getDisplay("prefpanel.security");
         init();
      }
      
      private void init ()
      {
         JLabel label;
         VerticalFlowLayout vflow;
         JPanel box, panel1;
//         Border labelBorder;
//         Font labelFont;
         
//         labelBorder = BorderFactory.createEmptyBorder( 0, 6, 0, 0 );
//         labelFont = DisplayManager.getFont( "control" );
         
         // options box
         vflow = new VerticalFlowLayout( 3 );
         box = new JPanel( vflow );
        
         box.add( Box.createVerticalStrut( 5 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.general") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 4, 8, 0 ));
         box.add( label );

         lockIdleNCP = new NumberCheckBoxPanel( MIN_IDLE_MIN, MAX_IDLE_MIN, 1, (int)(Global.DEFAULT_MAXIDLE/60000), 
               "prefbox.lockonidle", "units.min" );
         box.add( lockIdleNCP );

         lockMinimizeChk = makeCheckBox("prefbox.lockonminimize");
         box.add( lockMinimizeChk );

         autoClearNCP = new NumberCheckBoxPanel( MIN_CLIPBOARD_SEC, MAX_CLIPBOARD_SEC, 1, 
               (int)(Global.DEFAULT_MAXCLIPBOARD/1000), "prefbox.autoclearclip", "units.sec" );
         box.add( autoClearNCP );
         
         passCheckChk = makeCheckBox("prefbox.passcheckcopy");
         box.add( passCheckChk );
         openPassChk = makeCheckBox("prefbox.openpassword");
         box.add( openPassChk );
         
         box.add( Box.createVerticalStrut( 5 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.security2") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 4, 8, 0 ));
         box.add( label );

         // the UNDO/REDO option and entries combo
         panel1 = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
         
         useUndoRedoChk = makeCheckBox("prefbox.useundoredo");
         panel1.add( useUndoRedoChk );
         panel1.add( Box.createHorizontalStrut( 3 ) );

         undoEntriesCbo = new JComboBox();
         undoEntriesCbo.addItem( ResourceLoader.getDisplay("value.dec.10") );
         undoEntriesCbo.addItem( ResourceLoader.getDisplay("value.dec.25") );
         undoEntriesCbo.addItem( ResourceLoader.getDisplay("value.dec.50") );
         panel1.add( undoEntriesCbo );
         
         panel1.add( Box.createHorizontalStrut( 5 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.operations") );
         label.setFont( DisplayManager.getFont( "control" ) );
         panel1.add( label );
         box.add( panel1 );
         
         // auto-save files
         autoBackupNCP = new NumberCheckBoxPanel( 1, 99, 1, Global.DEFAULT_AUTOBACKUPS, 
               "prefbox.autobackup", "units.copies" );
         box.add( autoBackupNCP );
         
         useMirrorsChk = makeCheckBox("prefbox.usedatamirrors");
         box.add( useMirrorsChk );

         ftpCreateChk = makeCheckBox("prefbox.allowFTPcreate");
         box.add( ftpCreateChk );

         this.add( box, BorderLayout.CENTER );
      }
      
      @Override
	public void destruct ()
      {
      }
      
      @Override
	public String toString ()
      {
         return title;
      }
   
      @Override
	public boolean validateData ()
      {
         validated = true;
         return validated;
      }
      
      @Override
	public void startActivity ()
      {
         int i, index;
         
         validated = false;
         
         lockMinimizeChk.setSelected( Options.isOptionSet("lockMinimize") );
         passCheckChk.setSelected( Options.isOptionSet("createFileCheck") );
         openPassChk.setSelected( Options.isOptionSet("openPassEdit") );
         useMirrorsChk.setSelected( Options.isOptionSet("useDataMirrors") );
         useUndoRedoChk.setSelected( Options.isOptionSet("useUndoRedo") );
         ftpCreateChk.setSelected( Options.isOptionSet("allowFTPcreate") );
         
         autoClearNCP.setSelected( Options.isOptionSet("clearClipboard") );
         autoClearNCP.setValue( Options.getIntOption("clipboardTime")/1000 ) ;
         
         lockIdleNCP.setSelected( Options.isOptionSet("lockIdleState") ) ;
         lockIdleNCP.setValue( Options.getIntOption("maxIdleTime")/60000 ) ;
         
         autoBackupNCP.setSelected( Options.isOptionSet("autoBackup") );
         autoBackupNCP.setValue( Options.getIntOption( "autoBackupFiles" ) );
         
         // set list index for max-undo-operations 
         index = 0;
         i = Options.getIntOption( "maxUndoEntries" );
         if ( i == 25 )
            index = 1;
         else if ( i == 50 )
            index = 2;
         undoEntriesCbo.setSelectedIndex( index );
      }  // startActivity
      
      @Override
	public void endActivity ()
      {
         int i, v;
         
         if ( !validated )
            return;
            
         Options.setOption( "lockIdleState", lockIdleNCP.isSelected() );
         Options.setOption( "lockMinimize", lockMinimizeChk.isSelected() );
         Options.setOption( "clearClipboard", autoClearNCP.isSelected() );
         Options.setOption( "autoBackup", autoBackupNCP.isSelected() );
         Options.setOption( "createFileCheck", passCheckChk.isSelected() );
         Options.setOption( "openPassEdit", openPassChk.isSelected() );
         Options.setOption( "useDataMirrors", useMirrorsChk.isSelected() );
         Options.setOption( "useUndoRedo", useUndoRedoChk.isSelected() );
         Options.setOption( "allowFTPcreate", ftpCreateChk.isSelected() );
         
         Options.setIntOption( "maxIdleTime", lockIdleNCP.getValue()*60000 );
         Options.setIntOption( "clipboardTime", autoClearNCP.getValue()*1000 );
         Options.setIntOption( "autoBackupFiles", autoBackupNCP.getValue() );

         // UNDO manager setting
         if ( useUndoRedoChk.isSelected() )
         {
            if ( (i = undoEntriesCbo.getSelectedIndex()) == 0 )
               v = 10;
            else if ( i == 1 )
               v = 25;
            else
               v = 50;
            Options.setOption( "maxUndoEntries", String.valueOf( v ) );
         }
      }  // endActivity
   }  // SecurityPanel

   private class DisplayPanel extends OptionPanel
      {
         private String title; 
         private boolean validated;
         
         private JCheckBox usernameChk;
         private JCheckBox colorUseChk;
         private JCheckBox expandTreeChk;
         private JCheckBox useDecorationChk;
         private JRadioButton treeViewRadio;
         private JRadioButton tableViewRadio;
         private JCheckBox logicalNamesChk;
         private JCheckBox useFavouritesChk;
         private JComboBox dateFormatCbo;
         private JComboBox timeFormatCbo;
         NumberCheckBoxPanel autoMiniNCP;
         NumberCheckBoxPanel curtainNCP;
         

         public DisplayPanel ()
         {
            title = ResourceLoader.getDisplay( "prefpanel.display" );
            init();
         }
         
         private void init ()
         {
            JLabel label;
            JPanel panel, box;
            FlowLayout fLayout;
            VerticalFlowLayout vflow;
            ButtonGroup group;
            
            // options box
            vflow = new VerticalFlowLayout( 3 );
            box = new JPanel( vflow );
            box.add( Box.createVerticalStrut( 5 ) );
            label = new JLabel( ResourceLoader.getDisplay("preflabel.listscreen") );
            label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ));
            box.add( label );

            // List View options
            fLayout = new FlowLayout( FlowLayout.LEFT, 0, 0 );
            panel = new JPanel( fLayout );
            label = makeTextLabel("preffield.standardview");
//            label.setFont( textLabelFont );
            panel.add( label );
            group = new ButtonGroup();
            treeViewRadio = new JRadioButton( ResourceLoader.getCommand( "menu.view.tree" ) );
            group.add( treeViewRadio );
            panel.add( treeViewRadio );
            tableViewRadio = new JRadioButton( ResourceLoader.getCommand( "menu.view.list" ) );
            group.add( tableViewRadio );
            panel.add( tableViewRadio );
            box.add( panel );
            
            usernameChk = makeCheckBox("prefbox.username");
            box.add( usernameChk );
            expandTreeChk = makeCheckBox("prefbox.expandtree");
            box.add( expandTreeChk );
            colorUseChk = makeCheckBox("prefbox.uselistcolors");
            box.add( colorUseChk );

            // Other options
            box.add( Box.createVerticalStrut( 3 ) );
            label = new JLabel( ResourceLoader.getDisplay("preflabel.others") );
            label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ));
            box.add( label );

            autoMiniNCP = new NumberCheckBoxPanel( MIN_IDLE_MIN, MAX_IDLE_MIN, 1, 
                     (int)(Global.DEFAULT_MAXIDLE/60000), "prefbox.autominimize", "units.min" );
            box.add( autoMiniNCP );

            curtainNCP = new NumberCheckBoxPanel( MIN_IDLE_MIN, MAX_IDLE_MIN, 1, (int)(Global.DEFAULT_MAXIDLE/60000), 
                    "prefbox.usecurtain", "units.min" );
              box.add( curtainNCP );

            useFavouritesChk = makeCheckBox("prefbox.usefavourites");
            box.add( useFavouritesChk );

            logicalNamesChk = makeCheckBox("prefbox.logicalnames");
            box.add( logicalNamesChk );
            
            useDecorationChk = makeCheckBox("prefbox.seasonaldeco");
            box.add( useDecorationChk );
            
            // Formatting options
            box.add( Box.createVerticalStrut( 3 ) );
            label = new JLabel( ResourceLoader.getDisplay("preflabel.formatting") );
            label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ));
            box.add( label );

            // the Date-Format combo
            fLayout = new FlowLayout( FlowLayout.LEFT );
            panel = new JPanel( fLayout );
            label = makeTextLabel("preflabel.dateformat");
            panel.add( label );
            
            dateFormatCbo = new JComboBox( dateFormatOptions );
            dateFormatCbo.setFont(textLabelFont);
            panel.add( dateFormatCbo );
//            box.add( panel );

            // the Time-Format combo
//            fLayout = new FlowLayout( FlowLayout.LEFT );
//            panel = new JPanel( fLayout );
            panel.add( Box.createHorizontalStrut( 10 ) );
            label = makeTextLabel("preflabel.timeformat");
            panel.add( label );
            
            timeFormatCbo = new JComboBox( timeFormatOptions );
            timeFormatCbo.setFont(textLabelFont);
            panel.add( timeFormatCbo );
            box.add( panel );

            this.add( box, BorderLayout.CENTER );
         }
         
        @Override
		public void destruct () {
        }
         
        @Override
		public String toString () {
            return title;
         }
      
        @Override
		public boolean validateData () {
            validated = true;
            return validated;
        }
         
        @Override
		public void startActivity () {
             DateFormatOption dateOpt;
             TimeFormatOption timeOpt;
             validated = false;
            
//System.out.println( "--- start activity DISPLAY panel - Enter" );            
            usernameChk.setSelected( Options.isOptionSet("treeUsername") );
            colorUseChk.setSelected( Options.isOptionSet("useTableColors") );
            expandTreeChk.setSelected( Options.isOptionSet("autoExpandTree") );
            logicalNamesChk.setSelected( Options.isOptionSet("logicalFilenames") );
            useFavouritesChk.setSelected( Options.isOptionSet("useFavourites") );
            useDecorationChk.setSelected( Options.isOptionSet("seasonalDeco") );

            try {
                dateOpt = new DateFormatOption( Options.getOption("dateFormatOption") );
                dateFormatCbo.setSelectedItem ( dateOpt );
                timeOpt = new TimeFormatOption( Options.getOption("timeFormatOption") );
                timeFormatCbo.setSelectedItem ( timeOpt );
            } catch (Exception e) {
            }
            
            autoMiniNCP.setSelected( Options.isOptionSet("autoMinimize") ) ;
            autoMiniNCP.setValue( Options.getIntOption("maxAutoMinTime")/60000 ) ;
            
            curtainNCP.setSelected( Options.isOptionSet("useContainerLockedView") );
            curtainNCP.setValue( Options.getIntOption("viewCurtainTime") ) ;

            if ( Options.getIntOption( "defaultViewType" ) == PwsFileContainer.TABLE_VIEW )
               tableViewRadio.setSelected( true );
            else
               treeViewRadio.setSelected( true );
//System.out.println( "--- start activity DISPLAY panel - Leave" );            
         }  // startActivity
         
     @Override
	public void endActivity ()
     {
        if ( !validated ) return;
        
// System.out.println( "--- end activity DISPLAY panel - Enter" );            
		PwsFileContainer fc = Global.getSelectedFile();
        
        Options.setOption( "treeUsername", usernameChk.isSelected() );
        Options.setOption( "useTableColors", colorUseChk.isSelected() );
        Options.setOption( "useFavourites", useFavouritesChk.isSelected() );
        Options.setOption( "autoExpandTree", expandTreeChk.isSelected() );
        Options.setOption( "logicalFilenames", logicalNamesChk.isSelected() );
        Options.setOption( "useContainerLockedView", curtainNCP.isSelected() );
        Options.setOption( "seasonalDeco", useDecorationChk.isSelected() );
        Options.setIntOption( "defaultViewType", tableViewRadio.isSelected() ?
              PwsFileContainer.TABLE_VIEW : PwsFileContainer.TREE_VIEW );

        Options.setOption( "autoMinimize", autoMiniNCP.isSelected() );
        Options.setIntOption( "maxAutoMinTime", autoMiniNCP.getValue()*60000 );
        Options.setIntOption( "viewCurtainTime", curtainNCP.getValue() );
        Options.setOption( "dateFormatOption", 
                ((DateFormatOption)dateFormatCbo.getSelectedItem()).getFormat().name() );
        Options.setOption( "timeFormatOption", 
                ((TimeFormatOption)timeFormatCbo.getSelectedItem()).getFormat().name() );
        
        // update interface view
        org.jpws.pwslib.global.Global.setDisplayUsernames( usernameChk.isSelected() );
        if ( fc != null ) {
           Global.mainFrame.setTitleFile( fc );
           fc.getViewHandler().repaint();
        }
// System.out.println( "--- end activity DISPLAY panel - Leave" );            
     }  // endActivity
  }

   private class FontsPanel extends OptionPanel implements ActionListener
   {
         private String title; 
         private boolean validated;
         
         private JPanel bigPanel;
         
         private HashMap<String, JLabel> labelMap;
         private HashMap<String, Font> fontMap, fontInitMap;
         

         public FontsPanel () {
            title = ResourceLoader.getDisplay("prefpanel.fonts");
            init();
         }
         
         private void init () {
            JPanel panel;
            JButton button;
            String text;
            
            labelMap = new HashMap<String, JLabel>();
            fontMap = new HashMap<String, Font>();
            //labelBorder = BorderFactory.createEmptyBorder( 0, 6, 0, 0 ); 
            
            // create font sample text labels
            text = "Lorem ipsum Dolor sit Amet, Consectetur adipisicing Elit";
            labelMap.put( "menu", new JLabel( text ) );
            labelMap.put( "notes", new JLabel( text ) );
            labelMap.put( "password", new JLabel( text ) );
            labelMap.put( "data", new JLabel( text ) );
            labelMap.put( "display", new JLabel( text ) );
            labelMap.put( "control", new JLabel( text ) );
            labelMap.put( "tooltip", new JLabel( text ) );
            
            bigPanel = new JPanel( new VerticalFlowLayout( 5 ) );
            bigPanel.add( getFontPanel( "menu" ) );
            bigPanel.add( getFontPanel( "tooltip" ) );
            bigPanel.add( getFontPanel( "control" ) );
            bigPanel.add( getFontPanel( "display" ) );
            bigPanel.add( getFontPanel( "data" ) );
            bigPanel.add( getFontPanel( "notes" ) );
            bigPanel.add( getFontPanel( "password" ) );
            
            // reset button
            panel = new JPanel();
            button = new JButton( ResourceLoader.getDisplay( "fontpanel.reset" ) );
            button.setBackground( new Color( 0xFFEFD5 ) ); // papayawhip
            button.setActionCommand( "resetfonts" );
            button.addActionListener( this );
            button.setToolTipText( ResourceLoader.getCommand( 
                  "tooltip.prefs.font.reset" ) );
            panel.add( button );
            bigPanel.add( panel );
            
            this.add( bigPanel, BorderLayout.CENTER );
         }
         
         private JPanel getFontPanel ( String name ) {
            JButton button;
            JPanel panel;
            Insets insets;
            
            panel = new JPanel();
            button = new JButton();
            insets = button.getMargin();
            insets.left = 5;
            insets.right = 5;
            
            // buttons
            button.setText( ResourceLoader.getDisplay( "fontname." + name ) );
            button.setToolTipText( ResourceLoader.getCommand( 
                  "tooltip.prefs.font." + name ) );
            button.setMargin( insets );
            button.setActionCommand( "performfont." + name );
            button.addActionListener( this );
            button.setPreferredSize( new Dimension( 100, 26 ) );
            button.setFocusPainted( false );
            
            panel.add( button );
            
            // labels
            panel.add( labelMap.get( name ) );
            
            return panel;
         }  // getFontPanel
         
         @Override
		public void destruct () {
            bigPanel.removeAll();
         }
         
         @Override
		public String toString () {
            return title;
         }
      
         @Override
		public boolean validateData () {
            validated = true;
            return validated;
         }

         private void updatePanel () {
            // set sample labels' font
            updateFontPanel( "menu" );
            updateFontPanel( "data" );
            updateFontPanel( "password" );
            updateFontPanel( "display" );
            updateFontPanel( "control" );
            updateFontPanel( "tooltip" );
            updateFontPanel( "notes" );
         }
         
         private void updateFontPanel ( String name ) {
            Font font = fontMap.get( name );
            JLabel label = labelMap.get( name );
            label.setFont( font ); 
            label.setToolTipText( DisplayManager.fontCode( font ) );
         }
         
         @Override
		@SuppressWarnings("unchecked")
         public void startActivity ()
         {
            validated = false;
            
//    System.out.println( "--- start activity FONTS panel" );
            
            // obtain fonts from DisplayManager
            fontMap.put( "menu", DisplayManager.getFont( "menu" ) );
            fontMap.put( "data", DisplayManager.getFont( "data" ) );
            fontMap.put( "notes", DisplayManager.getFont( "notes" ) );
            fontMap.put( "password", DisplayManager.getFont( "password" ) );
            fontMap.put( "display", DisplayManager.getFont( "display" ) );
            fontMap.put( "control", DisplayManager.getFont( "control" ) );
            fontMap.put( "tooltip", DisplayManager.getFont( "tooltip" ) );

            fontInitMap = (HashMap<String, Font>)fontMap.clone();
            updatePanel();
         }  // startActivity
         
         @Override
		public void endActivity () {
            if ( !validated ) return;
            
//   System.out.println( "--- end activity FONTS panel" );            
            storeFont( "menu" ); 
            storeFont( "data" ); 
            storeFont( "password" ); 
            storeFont( "notes" ); 
            storeFont( "display" ); 
            storeFont( "control" ); 
            storeFont( "tooltip" ); 

         }  // endActivity

         /** Stores a font by its name and activates the corresponding
          *  application display facilities. Does nothing if name is <b>null</b>.
          */ 
         private void storeFont ( String name )
         {
            Font font, fontOld;
            
            if ( name == null )
               return;
            
            font = fontMap.get( name );
            fontOld = fontInitMap.get( name );
            if ( !font.equals( fontOld ) )
            {
//               DisplayManager.storeFont( name, fontOld );
               DisplayManager.storeFont( name, font );
            }
         }

         /* 
          * Overridden: @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
          */
         @Override
		public void actionPerformed ( ActionEvent e )
         {
            String cmd, name;
            Font font;
            
            cmd = e.getActionCommand();
            
            // this handles button actions
            if ( cmd.startsWith( "performfont." ) )
            {
               name = cmd.substring( 12 );
               
               title = ResourceLoader.getDisplay( "dlg.pref.choosefont" ); 
               title = Util.substituteText( title, "$name", 
                       ResourceLoader.getDisplay( "fontname." + name ) );
               font = FontChooser.showDialog( PreferencesDialog.this, 
                      title, fontMap.get( name ) );
               
               if ( font != null )
               {
                  fontMap.put( name, font );
                  DisplayManager.setFont( name, font );
                  updateFontPanel( name );
               }
            }
            // this reset action
            else if ( cmd.startsWith( "resetfonts" ) )
            {
               resetFont( "menu" );
               resetFont( "data" );
               resetFont( "password" );
               resetFont( "notes" );
               resetFont( "display" );
               resetFont( "control" );
               resetFont( "tooltip" );
            }
            
         } // actionPerformed
         
         private void resetFont ( String name )
         {
            Font font;
            
            font = DisplayManager.getDefaultFont( name );
            fontMap.put( name, font );
            DisplayManager.setFont( name, font );
            updateFontPanel( name );
         }
         
      }

   /**
    * @since 0-5-0
    */
   private class ReportsPanel extends OptionPanel
   {
      String title; 
      boolean validated;
      long oldExpireScope;
      
      JCheckBox useMarkerBrowseChk;
      JCheckBox useMarkerOpenChk;
      JCheckBox reportExpiryChk;
      JCheckBox autoNewsChk;
      NumberPanel  entryListLengthNP;
      NumberPanel expireDaysNP;

      public ReportsPanel ()
      {
         title = ResourceLoader.getDisplay("prefpanel.reports");
         init();
      }
      
      private void init ()
      {
         JLabel label;
         JPanel panel;
         FlowLayout fLayout;
         VerticalFlowLayout vflow;
         JPanel box;
         
         // options box
         vflow = new VerticalFlowLayout( 3 );
         box = new JPanel( vflow );

         // Expiry options
         box.add( Box.createVerticalStrut( 5 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.expiry") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ));
         box.add( label );

         // look-ahead days of expiry warning
         expireDaysNP = new NumberPanel( 0, 999, 1, (int)(Global.DEFAULT_EXPIRESCOPE/Global.DAY), 
               "preffield.lookahead", "units.days" );
         box.add( expireDaysNP );

         // checkbox report-on-file-open
         reportExpiryChk =  makeCheckBox("prefbox.reportexpiry");
         box.add( reportExpiryChk );
         
         // Index options
         box.add( Box.createVerticalStrut( 10 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.indices") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ));
         box.add( label );
         
         // the USE ENTRY MARKERS options (checkboxes)
         fLayout = new FlowLayout( FlowLayout.LEFT, 0, 0 );
         panel = new JPanel( fLayout );
         label = makeTextLabel("preffield.usedentrymarkers");
         label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 5 ));
         panel.add( label );
         useMarkerBrowseChk = makeCheckBox("prefbox.usedonbrowseentry");
         panel.add( useMarkerBrowseChk );
         useMarkerOpenChk = makeCheckBox("prefbox.usedonopenentry");
         useMarkerOpenChk.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 0 ));
         panel.add( useMarkerOpenChk );
         box.add( panel );
         
         // used entry list length
         entryListLengthNP = new NumberPanel( 3, 64, 1, Global.DEFAULT_USEDENTRYLISTLENGTH, 
               "preffield.usedlistlength", null );
         box.add( entryListLengthNP );

         // Other options
         box.add( Box.createVerticalStrut( 5 ) );
         label = new JLabel( ResourceLoader.getDisplay("preflabel.others") );
         label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ));
         box.add( label );

         autoNewsChk = makeCheckBox("prefbox.autonewscheck");
         box.add( autoNewsChk );
        
         this.add( box, BorderLayout.CENTER );
      }
      
      @Override
	public void destruct ()
      {
      }
      
      @Override
	public String toString ()
      {
         return title;
      }
   
      @Override
	public boolean validateData ()
      {
         validated = true;
         return true;
      }
      
      @Override
	public void startActivity ()
      {
         validated = false;
         
         oldExpireScope = Options.getLongOption("expireScope");
         reportExpiryChk.setSelected( Options.isOptionSet("expiryCheck") );
         expireDaysNP.setValue( (int)(oldExpireScope/Global.DAY) );

         useMarkerBrowseChk.setSelected( Options.isOptionSet("useEntryOnBrowse") );
         useMarkerOpenChk.setSelected( Options.isOptionSet("useEntryOnOpen") );
         autoNewsChk.setSelected( Options.isOptionSet("checkProjectNews") );
         
         entryListLengthNP.setValue( Options.getIntOption( "usedEntryListLength" ) );
      }  // startActivity
      
      @Override
	public void endActivity ()
      {
         long i;
         
         if ( !validated )
            return;
            
         Options.setOption( "expiryCheck", reportExpiryChk.isSelected() );
         Options.setOption( "useEntryOnBrowse", useMarkerBrowseChk.isSelected() );
         Options.setOption( "useEntryOnOpen", useMarkerOpenChk.isSelected() );
         Options.setOption( "checkProjectNews", autoNewsChk.isSelected() );
         Options.setIntOption( "usedEntryListLength", entryListLengthNP.getValue() );
         
         // update expiry display
         i = expireDaysNP.getValue() * Global.DAY;
         Options.setLongOption( "expireScope", i );
      }  // endActivity
   }

   private class NumberPanel extends JPanel
      {
         SpinnerNumberModel model;
         JSpinner spinner;
         int defaultVal, minVal, maxVal;
         
         public NumberPanel ( int min, int max, int stepsize, int defvalue, String leader, String trailer )
         {
            super( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
            JLabel label;
            
            minVal = min;
            maxVal = max;
            defaultVal = defvalue;
            
            // optional leading label
            if ( leader != null )
            {
               label = makeTextLabel( leader );
               this.add( label );
               this.add( Box.createHorizontalStrut( 6 ) );
            }
            
            // spinner box
            model = new SpinnerNumberModel( min, min, max, stepsize );
            spinner = new JSpinner( model );
            setValue( 0 );
            this.add( spinner );

            // optional trailing label
            if ( trailer != null )
            {
               this.add( Box.createHorizontalStrut( 5 ) );
               label = makeTextLabel( trailer );
               this.add( label );
            }
         }
         
         public void setValue ( int value )
         {
            if ( value < minVal | value > maxVal )
               value = defaultVal;
            model.setValue( new Integer( value ) );
         }
         
         public int getValue ()
         {
            return ((Integer)model.getValue()).intValue();
         }
      }  // class NumberCheckBoxPanel

   public abstract class OptionPanel extends JPanel
   {
   
      public OptionPanel () {
         super( new BorderLayout() );
         setBorder( PANEL_BORDER );
      }
   
      public boolean validated () {
         KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
         return validateData();
      }
      
      @Override
	  public abstract String toString ();
      public abstract void startActivity ();
      public abstract void endActivity ();
      public abstract boolean validateData ();
      public abstract void destruct ();
   }

private class EditorPanel extends OptionPanel
      {
         private String title; 
         private boolean validated;
         
         private JCheckBox fullNotesChk;
         private JCheckBox lineWrapChk;
         private JCheckBox activeHistoryChk;
         private JCheckBox restrictKeysChk;

         public EditorPanel ()
         {
            title = ResourceLoader.getDisplay( "prefpanel.editor" );
            init();
         }
         
         private void init ()
         {
            // options box
            VerticalFlowLayout vflow = new VerticalFlowLayout( 3 );
            JPanel box = new JPanel( vflow );

            // Editor options
            box.add( Box.createVerticalStrut( 5 ) );
            JLabel label = new JLabel( ResourceLoader.getDisplay("preflabel.editor") );
            label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ));
            box.add( label );

            fullNotesChk = makeCheckBox("prefbox.fullnotes");
            box.add( fullNotesChk );
            
            lineWrapChk = makeCheckBox("prefbox.linewrapping");
            box.add( lineWrapChk );
            
            activeHistoryChk = makeCheckBox("prefbox.activehistory");
            box.add( activeHistoryChk );
            
            restrictKeysChk = makeCheckBox("prefbox.restrictKeys");
            box.add( restrictKeysChk );
            
            this.add( box, BorderLayout.CENTER );
         }
         
         @Override
		public void destruct ()
         {
         }
         
         @Override
		public String toString ()
         {
            return title;
         }
      
         @Override
		public boolean validateData ()
         {
            validated = true;
            return validated;
         }
         
         @Override
		public void startActivity ()
         {
            validated = false;
            
 //System.out.println( "--- start activity EDITOR panel" );            
            fullNotesChk.setSelected( Options.isOptionSet("editFullNotes") );
            lineWrapChk.setSelected( Options.isOptionSet("editLineWrap") );
            activeHistoryChk.setSelected( Options.isOptionSet("editActiveHistory") );
            restrictKeysChk.setSelected( Options.isOptionSet("restrictAccelerators") );
         }  // startActivity
         
         @Override
		public void endActivity ()
         {
            if ( !validated )
               return;
//System.out.println( "--- end activity EDITOR panel" );            
            
            Options.setOption( "editFullNotes", fullNotesChk.isSelected() );
            Options.setOption( "editLineWrap", lineWrapChk.isSelected() );
            Options.setOption( "editActiveHistory", activeHistoryChk.isSelected() );
            Options.setOption( "restrictAccelerators", restrictKeysChk.isSelected() );
         }  // endActivity
      }

private class PassPolicyPanel extends OptionPanel
{
    private String title; 
    
    private PolicyDialog.PolicyDialogPanel dlgPanel;
    private PwsPassphrasePolicy policy;

    public PassPolicyPanel ()
    {
       title = ResourceLoader.getDisplay( "prefpanel.password" );
       policy = (PwsPassphrasePolicy)Global.passphrasePolicy.clone();
       
       dlgPanel = new PolicyDialogPanel(policy, null);
       add( dlgPanel, BorderLayout.CENTER );
       
       // title message
       String title = ResourceLoader.getDisplay( "dlg.policy.global" );
       JLabel label = new JLabel( title );
       add( label, BorderLayout.NORTH );
    }
    
	@Override
	public String toString() {
		return title;
	}

	@Override
	public void startActivity() {
	}

	@Override
	public void endActivity() {
		if ( policy.isValid() ) {
			Global.passphrasePolicy = policy;
            Options.setOption( "policy", policy.getInternalForm() );
		}
	}

	@Override
	public boolean validateData() {
        try {
            dlgPanel.transferValues();

        } catch ( NumberFormatException e ) {
            GUIService.infoMessage(this, "dlg.badvalue", "msg.badpassinteger");
            return false;
        }
         
         if ( !policy.isValid() ) {
            GUIService.infoMessage(this, "dlg.operrejected", "msg.badpasspolicy"); 
            return false;
         }
		return true;
	}

	@Override
	public void destruct() {
	}
}
}
