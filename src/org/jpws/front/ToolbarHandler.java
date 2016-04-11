/*
 *  ToolbarHandler in org.jpws.front
 *  file: ToolbarHandler.java
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
package org.jpws.front;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;

import org.jpws.data.OptionChangeEvent;
import org.jpws.data.OptionChangeListener;
import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket.ChangeEvent;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.global.Log;

// READY FOR 0-6-0

/** Handles the application's main toolbar. Most toolbar buttons are connected to
 *  <code>Global.mainActionListener</code>, some are created from <code>Action</code> 
 *  objects made available through {@link ActionHandler}.
 *   
 *  <p>The toolbar listens to mouse events and shows a popup menu to customize
 *  its content upon right-click. The popup menu allows to select/deselect each single 
 *  toolbar item, except the "switch-off" button. Choices are stored in global options.
 *  <p>The toolbar listens to any selected open database in the desktop and reflects  
 *  state changes of the open database into appropriate enabling or disabling of buttons.
 *  Awareness of databases is handled by direct external calls to 
 *  <code>registerFile()</code> and <code>unregisterFile()</code>. 
 *  <p>The toolbar listens to changes of global <code>Options</code> with current awareness
 *  of option "useFavourites" which affects the set of available buttons.
 *  
 *  <p>ToolbarHandler operates display activity safely on the EDT. 
 *  
 *  @since / strongly modified in 0-6-0
 * @author Wolfgang Keller
 */
public class ToolbarHandler
{
   private static final Insets BUTTON_INSETS = new Insets( 0, 3, 0, 3 );
   private static final Dimension BUTTON_SIZE = new Dimension( 23, 23 );

	private static JToolBar    	   	    toolBar;
	private static ToolbarItem			buttonAdd;
	private static ToolbarItem			buttonEdit;
	private static ToolbarItem			buttonDelete;
	private static ToolbarItem			buttonCopyPass;
	private static ToolbarItem			buttonCopyUser;
    private static ToolbarItem          fileNew;
    private static ToolbarItem          fileOpen;
    private static ToolbarItem          fileSave;
    private static ToolbarItem          fileSaveAll;
    private static ToolbarItem          fileClose;
    private static ToolbarItem          fileCloseAll;
    private static ToolbarItem          fileExit;
    private static ToolbarItem          clearClipboard;
    private static ToolbarItem          webstartURL;
    private static ToolbarItem          toggleDesktop;
    private static ToolbarItem          filterFavourites;
    private static ToolbarItem          filterExpiring;

    private static Listener         listener;
    private static PopupListener    popupListener = new PopupListener();
    /** Whether there is a file selected in the desktop. */
    private static boolean          isFileOpen;

   /**
	 *  Singleton class. 
	 */
	private ToolbarHandler()
	{
   }
   
   public static void init ()
   {
      OptionListener opl;

      makeToolbar();
      
      listener = new Listener();
      opl = new OptionListener();
      Options.addChangeListener( opl );
      toolBar.addMouseListener( popupListener );
      DisplayManager.addChangeListener(listener);

      // set visibility of cases where global options are involved
      opl.optionChanged( new OptionChangeEvent( toolBar, "useFavourites", null, null ) );
      
   }  // init

   private static void makeToolbar ()
   {
      toolBar = new JToolBar();

      toolBar.setFloatable( false );

      fileNew = new ToolbarItem("filenew", "toolbar.new", "menu.file.new", true);
      toolBar.add( fileNew );

      fileOpen = new ToolbarItem("fileopen", "toolbar.open", "menu.file.open", true);
      toolBar.add( fileOpen );

      fileSave = new ToolbarItem("filesave", "toolbar.save", "menu.file.save", false);
      toolBar.add( fileSave );

      fileSaveAll  = new ToolbarItem( "toolbar.saveall", ActionHandler.getAction( "SAVEALL" ) );
      fileSaveAll.setEnabled( false );
      toolBar.add( fileSaveAll );

      toolBar.addSeparator();

      fileClose = new ToolbarItem("fileclose", "toolbar.close", "menu.file.close", false);
      toolBar.add( fileClose );

      fileCloseAll = new ToolbarItem( "toolbar.closeall", ActionHandler.getAction( "CLOSEALL" ) );
      fileCloseAll.setEnabled( false );
      toolBar.add( fileCloseAll );

      toolBar.addSeparator();

      buttonCopyPass  = new ToolbarItem("copypass", "toolbar.copypass", "menu.edit.copypass", false);
      toolBar.add( buttonCopyPass );

      buttonCopyUser  = new ToolbarItem("copyuser", "toolbar.copyuser", "menu.edit.copyuser", false);
      toolBar.add( buttonCopyUser );

      clearClipboard = new ToolbarItem( "toolbar.clearclip", ActionHandler.getAction( "CLEARCLIP" ) );
      toolBar.add( clearClipboard );

      webstartURL  = new ToolbarItem( "webstarter", "toolbar.webstart.record", "menu.edit.starturl", false );
      toolBar.add( webstartURL );

      toolBar.addSeparator();

      filterFavourites = new ToolbarItem("filter-favourites", "toolbar.filter-favourites", "menu.view.toggle-favourites", false);
      toolBar.add( filterFavourites );

      filterExpiring = new ToolbarItem("filter-expiring", "toolbar.filter-expiring", "menu.view.toggle-expiring", false);
      toolBar.add( filterExpiring );

      toggleDesktop = new ToolbarItem("toggle-desktop", "toolbar.toggle-desktop", "menu.view.toggle-desktop", true);
      toolBar.add( toggleDesktop );

      toolBar.addSeparator();

      buttonAdd = new ToolbarItem("add", "toolbar.add", "menu.edit.psw.add", false);
      toolBar.add( buttonAdd );

      buttonEdit = new ToolbarItem("edit", "toolbar.edit", "menu.edit.psw.edit", false);
      toolBar.add( buttonEdit );

      buttonDelete =  new ToolbarItem( "toolbar.delete", ActionHandler.getAction( "DELETE" ) );
      buttonDelete.setEnabled( false );
      toolBar.add( buttonDelete );

      toolBar.addSeparator();
/*
    // for TESTING purposes
    button = makeButton( "fontchooser", "" );
    button.setActionCommand( "start.filechooser" );
    toolBar.add( button );

    toolBar.addSeparator();
*/
    fileExit = new ToolbarItem("exit", "toolbar.exit", "menu.file.exit", true);
    fileExit.setVisible( true );
    toolBar.add( fileExit );
   }
   
	private static void fileMounted( PwsFileContainer ct )
	{
	   boolean open;
       
       if ( ct == null ) return;
       
       isFileOpen = open = ct.getOperationMode() == PwsFileContainer.MOUNTED_ACTIVE;
       
       fileClose.setEnabled( open );
       buttonAdd.setEnabled( open );
       filterFavourites.setEnabled( open );
       filterExpiring.setEnabled( open );
       fileSave.setEnabled( open & ct.isModified() );
       fileSaveAll.setEnabled( fileSave.isEnabled() || fileSaveAll.isEnabled() );
       rowSelected( ct );
	}

	private static void rowSelected( PwsFileContainer ct )
	{
	   PwsRecord selectedRec;
       boolean isLeafNode, open;
       int status;

       open = ct.getOperationMode() == PwsFileContainer.MOUNTED_ACTIVE;
       status = ct.getSelectionStatus();
       selectedRec = ct.getSelectedRecord();
       isLeafNode = open & status == PwsFileContainer.RECORD_SELECTED; 
//       isGroupNode = open & status == PwsFileContainer.GROUP_SELECTED; 

       buttonEdit.setEnabled( isLeafNode );
       buttonCopyUser.setEnabled( isLeafNode );
       buttonCopyPass.setEnabled( isLeafNode );
       buttonDelete.setEnabled( open & status != PwsFileContainer.NOTHING_SELECTED );
       webstartURL.setEnabled( selectedRec != null && isLeafNode && Util.extractURL( selectedRec.getUrl() ) != null );
	}

   private static void fileUpdated( PwsFileContainer ct )
   {
      boolean check = ct.isModified();
      fileSave.setEnabled( check );
      fileSaveAll.setEnabled( ct.isModified() || DisplayManager.hasModifiedFiles() );
      rowSelected( ct );
   }  // fileUpdated
   
   private static ImageIcon whiteIcon = ResourceLoader.getImageIcon("white-icon");
   
   /** Creates an icon-button, as fitted for a standard toolbar. The button
    *  action is made sensible to <code>Global.mainActionListener</code>.   
    * 
    * @param imgName the canonised image name for the icon
    * @param buttonName the canonised button name, used for tooltip text
    * @return toolbar button
    */
   public static JButton makeButton( String imgName, String buttonName, boolean enabled )
   {
      JButton button   = new JButton();
      
      button.setMargin( BUTTON_INSETS );
      button.setPreferredSize( BUTTON_SIZE );
      button.setToolTipText( ResourceLoader.getCommand(buttonName+".tooltip") );
      ImageIcon icon = ResourceLoader.getImageIcon(imgName);
      icon.setDescription( ResourceLoader.getDisplay(buttonName) );
      button.setIcon( icon ); 
      button.setPressedIcon(whiteIcon);
      button.setEnabled(  enabled );
      button.setFocusable( false );
      button.addActionListener( Global.mainActionListener );

      return button;
   }

   /** Creates an icon-button fitting for a standard toolbar, derived from
    *  a standard Action definition.   
    * 
    *  @return toolbar button
    */
   public static JButton makeButton( Action action ) {
      JButton button   = new JButton( action );
      button.setMargin( BUTTON_INSETS );
      button.setPreferredSize( BUTTON_SIZE );
      button.setText( null );

      return button;
   }
    

	/**
	 * @return Returns the toolBar.
	 */
	public static JToolBar getToolBar() {
		return toolBar;
	}
    
    public static void registerFile ( PwsFileContainer container ) {
       if ( container != null ) {
          Log.log( 5, "(ToolbarHandler.registerFile) registering file : ".concat(container.getDatabaseName()));
          container.addChangeListener( listener );
       }
    }
    
    public static void unregisterFile ( PwsFileContainer container ) {
       if ( container != null ) {
          Log.log( 5, "(ToolbarHandler.unregisterFile) un-registering file : ".concat(container.getDatabaseName()));
          container.removeChangeListener( listener );
       }
    }

    /**
     * Renders a popup menu for the context of this text area
     * including actual options of the UNDO manager.
     * 
     * @return <code>JPopupMenu</code>
     */
    private static JPopupMenu getPopupMenu () {
       JPopupMenu menu;
       
       menu = new JPopupMenu( ResourceLoader.getDisplay( "menu.toolbar.customize" ) );
       toolBar.requestFocus();
       
       menu.add( fileNew.getMenuItem() );
       menu.add( fileOpen.getMenuItem() );
       menu.add( fileSave.getMenuItem() );
       menu.add( fileSaveAll.getMenuItem() );
       
       menu.addSeparator();
    
       menu.add( fileClose.getMenuItem() );
       menu.add( fileCloseAll.getMenuItem() );
       
       menu.addSeparator();
    
       menu.add( buttonCopyPass.getMenuItem() );
       menu.add( buttonCopyUser.getMenuItem() );
       menu.add( clearClipboard.getMenuItem() );
       menu.add( webstartURL.getMenuItem() );
       
       menu.addSeparator();
       
       menu.add( filterFavourites.getMenuItem() );
       menu.add( filterExpiring.getMenuItem() );
       menu.add( toggleDesktop.getMenuItem() );
       
       menu.addSeparator();
       
       menu.add( buttonAdd.getMenuItem() );
       menu.add( buttonEdit.getMenuItem() );
       menu.add( buttonDelete.getMenuItem() );

       return menu;
    }
    
   // ***********************  INNER CLASSES  **************************
       
    /** This is the listener class to the {@link PwsFileContainer} that
     * is selected in the desktop.
     */
    private static class Listener implements ChangeListener
    {
       //  ********* IMPLEMENTATION OF CHANGELISTENER  ************* 
       public void stateChanged ( final javax.swing.event.ChangeEvent evt )
       {
          Runnable r;
          
          r = new Runnable() {
          public void run()
          {
             PwsFileContainer ct;
             if ( evt.getSource() == null ) return;
             
             if ( evt instanceof ChangeEvent && 
                  evt.getSource() instanceof PwsFileContainer )
             {
                ct = (PwsFileContainer)evt.getSource();
                
                switch ( ((ChangeEvent)evt).getState() )
                {
                // OPERATION MODE
                case PwsFileContainer.OPERATION_MODE:
                   fileMounted( ct );
                   break;
                   
                case PwsFileContainer.SELECTION_STATUS: 
                case PwsFileContainer.SELECTION_EVENT: 
                   rowSelected( ct );
                   break;
                
                case PwsFileContainer.MODIFY_EVENT: 
                   fileUpdated( ct );
                   break;
                }
             }
             
             if ( evt.getSource() == DisplayManager.class ) {
                 fileSaveAll.setEnabled( DisplayManager.hasModifiedFiles() );
                 Log.log(10, "(ToolbarHandler.Listener.RUN) signal from DisplayManager -> update");
             }
             
          } };
          ActionHandler.executeOnEDT( r );
       }  // stateChanged
    }

/** The class to create a toolbar button; all toolbar buttons are an instance of this. 
 *  The feature that represent a button in global <code>Options</code> is its
 *  ButtonName. The item owns a <code>JMenuItem</code> which is an element of 
 *  the popup menu to customize the toolbar.
 *  @since 0-6-0
 * */   
private static class ToolbarItem extends JButton implements ActionListener
{
   private JMenuItem menuItem = new JCheckBoxMenuItem();
   private String buttonName;

   /** Create a new toolbar item from a button name and the features 
    * available in an <code>Action</code> object.
    * 
    * @param buttonName String
    * @param action <code>Action</code>
    */
   public ToolbarItem (  String buttonName, Action action )
   {
      super( action );
      setText( null );
      this.buttonName = buttonName;
      
      init( (String)action.getValue( Action.NAME ) );
   }
   
   /** Create a toolbar item from a set of features conveyed as parameters.
    * 
    * @param imgName String
    * @param buttonName String
    * @param command String
    * @param enabled boolean
    */
   public ToolbarItem (  String imgName, String buttonName, String command, boolean enabled )
   {
      super();
      ImageIcon icon;
      String title;
      
      this.buttonName = buttonName;
      icon = ResourceLoader.getImageIcon(imgName);
      title = ResourceLoader.getCommand( command );
//      icon.setDescription( ResourceLoader.getDisplay(buttonName) );

      setToolTipText( ResourceLoader.getCommand(buttonName+".tooltip") );
      setIcon( icon ); 
      setEnabled(  enabled );
      setActionCommand( command );
      addActionListener( Global.mainActionListener );

      init( title );
   }
   
   private void init ( String title )
   {
      boolean v;
      
      menuItem.setText( title );
      menuItem.setSelected( true );
      menuItem.addActionListener( this );

      setMargin( BUTTON_INSETS );
      setPreferredSize( BUTTON_SIZE );
      v = Options.isOptionSet( buttonName );
      setVisible( v );
      addMouseListener( popupListener );
   }
   
   public void setVisible( final boolean v )
   {
      Runnable r = new Runnable ()
      {
         @Override
         public void run ()
         {
            if ( isVisible() != v )
            {
               ToolbarItem.super.setVisible( v );
               menuItem.setSelected( v );
               Options.setOption( buttonName, v );
            }
         }
      };
      ActionHandler.executeOnEDT( r );
   }
   
   /** Renders the ButtonName feature. */
   public String getButtonName ()
   {
      return buttonName;
   }

   /** Returns the button's menu item for the popup menu to customize
    *  the toolbar.
    * @return <code>JMenuItem</code>
    */
   public JMenuItem getMenuItem ()
   {
      return menuItem;
   }
   
   /** This is the action invoked by triggering a menu item of
    * the popup menu to customize the toolbar. It sets visibility
    * of the button according to menu item state and stores this
    * feature in global options.
    */
   public void actionPerformed ( ActionEvent e )
   {
      boolean check;
      
      if ( e.getSource() != null )
      {
//         Log.log( 0, "++ popup menu command: " + e.getActionCommand() );
         check = menuItem.isSelected();
         setVisible( check );
      }
   }
   }  // ToolbarItem

   /** Listener class to <code>Options</code> global class. 
    *  @since 0-6-0
    */
   private static class OptionListener implements OptionChangeListener
   {
      public void optionChanged ( final OptionChangeEvent e )
      {
         Runnable r = new Runnable() 
         {
            @Override
            public void run ()
            {
               String name;
               boolean check, opted;
               
               name = e.getOptionName();
               if ( name.equals( "useFavourites" ) ) {
                  // disable custom menu item if global option for this function is OFF
                  opted = Options.isOptionSet( name );
                  filterFavourites.getMenuItem().setEnabled( opted );
      
                  // switch ON/OFF button if global option AND custom menu option
                  check = opted && Options.isOptionSet( filterFavourites.getButtonName() );
                  filterFavourites.setEnabled( isFileOpen );
                  filterFavourites.setVisible( check );
               }
            }
         };
         ActionHandler.executeOnEDT( r );
      }
   }

   /**
    * Mouse listener class for the toolbar object and its contained buttons.
    * Reaction follows native JVM specific variances of mouse-action conventions.
    *  @since 0-6-0
    */
   private static class PopupListener extends MouseAdapter
   {
   public void mousePressed(MouseEvent e) 
   {
       maybeShowPopup(e);
   }
   
   public void mouseReleased(MouseEvent e) 
   {
       maybeShowPopup(e);
   }
   
   private void maybeShowPopup(MouseEvent e) 
   {
      ActionHandler.resetIdleTime();
       if ( e.isPopupTrigger() ) 
          getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
   }

   }  // PopupListener

}
