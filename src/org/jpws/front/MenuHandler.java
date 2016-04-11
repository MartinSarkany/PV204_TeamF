/*
 *  MenuHandler in org.jpws.front
 *  file: MenuHandler.java
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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jpws.data.OptionChangeEvent;
import org.jpws.data.OptionChangeListener;
import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket.ChangeEvent;
import org.jpws.front.util.RecentList;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.global.Log;


/**
 * Static singelton class to handle command menu setup and runtime administration.
 * 
 * @author Kevin Preece
 * @author Wolfgang Keller
 */
public class MenuHandler 
{
   public static Color MENUITEM_MARKED_COLOR = Color.BLUE;
   
   public static final int FILTER_SHOWALL = 0;
   public static final int FILTER_SHOW_EXIRING = 1;
   public static final int FILTER_SHOW_IMPORTED = 2;
   public static final int FILTER_SHOW_MODIFIED = 3;
   public static final int FILTER_SHOW_INVALID = 4;
   public static final int FILTER_SHOW_FAVOURITES = 5;
   
   private static JMenuBar	     menuBar;
   private static JMenu          recordMenu;    
   private static JMenu          recentMenu;
   private static JMenu          revertMenu;
   private static JMenu          lastEditedMenu;
   private static JMenu          lastUsedMenu;
   private static JMenu          filterMenu;
   private static JMenu          moveToFileMenu;
   private static JMenu          copyToFileMenu;

   // multi-purpose event listener for this class
   private static MListener     mListener = new MListener();

   private static JMenuItem		editAdd;
   private static JMenuItem		editEdit;
   private static JMenuItem		editDelete;
   private static JMenuItem		editFind;
   private static JMenuItem     editQuickFind;
   private static JMenuItem		editCopyUser;
   private static JMenuItem		editCopyPass;
   private static JMenuItem     editUndo, editRedo;
   private static JSeparator    editUndoSeparator;
   private static JMenuItem      exportSelected;
   private static JMenuItem      editSelectAll;
   private static JMenuItem      fileClose;
   private static JMenuItem      fileCloseAll;
   private static JMenuItem      fileConvert;
   private static JMenuItem      fileSave;
   private static JMenuItem      fileSaveAll;
   private static JMenuItem      fileSaveAs;
   private static JMenuItem      fileSaveCopy;
   private static JMenuItem      clearClip;
   private static JMenuItem	     manageChangePw;
   private static JMenuItem      managePolicy;
   private static JMenuItem	     manageBackup;
   private static JMenuItem	     manageRestore;
   private static JMenuItem      manageMergefile;
   private static JMenuItem      manageExportFileCSV;
   private static JMenuItem      manageImportFileCSV;
   private static JMenuItem      fileInfo;
   private static JCheckBoxMenuItem      monitorCheck;
   private static JCheckBoxMenuItem      trayIconCheck;
   
   private static JRadioButtonMenuItem      viewTable;
   private static JRadioButtonMenuItem      viewTree;
   private static JRadioButtonMenuItem      viewSingle;
   private static JRadioButtonMenuItem      viewDesktop;

   private static JRadioButtonMenuItem filterAll;
   private static JRadioButtonMenuItem filterFavourites;
   private static JRadioButtonMenuItem filterExpiry;
   private static JRadioButtonMenuItem filterInvalid;
   private static JRadioButtonMenuItem filterModify;
   private static JRadioButtonMenuItem filterImport;
   
   private static Set<KeyStroke> accelerators;
   
   private static boolean isInited; 

   private MenuHandler ()
   {
   }

   /**
	 * MenuHandler initial routine. Builds up menu structure.
	 */
	public static void init () 
	{
       if ( isInited )
          return;
       
		menuBar	= new JMenuBar();
		menuBar.add( buildFileMenu() );
		menuBar.add( buildEditMenu() );
		menuBar.add( buildViewMenu() );
		menuBar.add( buildManageMenu() );
		menuBar.add( buildHelpMenu() );
      
        ActionHandler.clipboardUpdated();
        DisplayManager.addChangeListener( mListener );
        Options.addChangeListener( new OptionListener() );
        
        isInited = true;
	}

	private static void traverseMenuElement (MenuElement el, Set<KeyStroke> keyset) {
		MenuElement[] subElem = el.getSubElements();
		for (MenuElement sub : subElem) {
			traverseMenuElement(sub, keyset);
		}
		if ( el instanceof JMenuItem ) {
			KeyStroke stroke = ((JMenuItem) el).getAccelerator();
			if ( stroke != null ) {
				keyset.add(stroke);
			}
		}
	}
	
	public static Set<KeyStroke> getAccelerators () {
		if ( accelerators == null ) {
			Set<KeyStroke> kset = new HashSet<KeyStroke>();
			
			// traverse all main menu items
			traverseMenuElement( getMenuBar(), kset );
			accelerators = kset;
		}
		return accelerators;
	}
	
	private static JMenu buildEditMenu()
	{
		JMenu		menu;
		JMenuItem	item;
	
      menu = new JMenu( ResourceLoader.getCommand("menu.edit") );
      menu.addMenuListener( mListener );

      editUndo = makeItem( "menu.edit.undo", true );
      menu.add( editUndo );
      
      editRedo = makeItem( "menu.edit.redo", true );
      menu.add( editRedo );
      
      editUndoSeparator = new JSeparator();
      menu.add( editUndoSeparator );

      undoUpdated();
    
      recordMenu = buildRecordMenu();
      menu.add( recordMenu );

      lastUsedMenu = new JMenu( ResourceLoader.getCommand("menu.edit.lastused"));
      lastUsedMenu.addMenuListener( mListener );
//      lastUsedMenu.setVisible( Options.isOptionSet( "storeMinorChanges" ) );
      lastUsedMenu.setEnabled( false );
      menu.add( lastUsedMenu );
    
      lastEditedMenu = new JMenu( ResourceLoader.getCommand("menu.edit.lastedited"));
      lastEditedMenu.addMenuListener( mListener );
//      lastEditedMenu.setVisible( Options.isOptionSet( "storeMinorChanges" ) );
      lastEditedMenu.setEnabled( false );
      menu.add( lastEditedMenu );
    
      menu.addSeparator();
	
      editCopyPass = makeItem( "menu.edit.copypass", KeyEvent.VK_P, false );
      menu.add( editCopyPass );
	
      editCopyUser = makeItem( "menu.edit.copyuser", KeyEvent.VK_U, false );
      menu.add( editCopyUser );
	
      clearClip = makeItem( ActionHandler.getAction( "CLEARCLIP" ), 0 );
	  menu.add( clearClip );

      menu.addSeparator();
      
      editFind = makeItem( "menu.edit.find", KeyEvent.VK_F, false );
      menu.add( editFind );
   
      // THIS IS ALWAYS INVISIBLE !! (Dummy to allow CTRL-S command trigger)
      editQuickFind = makeItem( "menu.edit.quickfind", KeyEvent.VK_Q, false );
//      editQuickFind.setVisible( false );
      menu.add( editQuickFind );

      editSelectAll = makeItem( "menu.edit.selectall", KeyEvent.VK_A, false );
      menu.add( editSelectAll );
      
      moveToFileMenu = new MoveFileMenu( false );
      moveToFileMenu.setEnabled( false );
      menu.add( moveToFileMenu );

      copyToFileMenu = new MoveFileMenu( true );
      copyToFileMenu.setEnabled( false );
      menu.add( copyToFileMenu );

      exportSelected = makeItem( "menu.edit.export.csv", false );  
      exportSelected.setEnabled( false );
      menu.add( exportSelected );
/*   
      item = makeItem( "menu.edit.selectrecords", true );
      menu.add( item );
   
      item = makeItem( "menu.edit.duplicate", true );
      menu.add( item );
   
      item = makeItem( "menu.edit.expandbranch", true );
      menu.add( item );
   
      item = makeItem( "menu.edit.foldall", true );
      menu.add( item );
   
      item = makeItem( "menu.edit.moveentries", true );
      menu.add( item );
   
      editRenameGroup = makeItem( "menu.edit.rename.group", false );
      menu.add( editRenameGroup );
*/   
      menu.addSeparator();
      
      item = makeItem( "menu.edit.options", KeyEvent.VK_R, true );
      menu.add( item );

      return menu;
	}  // buildEditMenu
   
   /** Creates a standard menu item referring to the global action listener
    * with <b>command</b> used as command word.
    * 
    * @param command String action command and token into Command resource bundle 
    *        for text shown on item
    * @param enabled boolean whether this item is initially enabled
    * @return <code>JMenuItem</code> menu item
    * @since 0-5-0 public (private before)
    */  
   public static JMenuItem makeItem ( String command, boolean enabled )
   {
      JMenuItem item = new JMenuItem( ResourceLoader.getCommand( command ) );
      item.setActionCommand( command );
      item.setEnabled( enabled );
      item.addActionListener( Global.mainActionListener );
      return item;
   }

   /** Creates a standard menu item referring to the global action listener
    * with <b>command</b> used as command word.
    * 
    * @param command String action command and token into Command resource bundle 
    *        for text shown on item
    * @param key int, keyboard key identifier (as of KeyEvent.xy)       
    * @param enabled boolean whether this item is initially enabled
    * @return <code>JMenuItem</code> menu item
    * @since 0-7-0
    */  
   public static JMenuItem makeItem ( String command, int key, boolean enabled )
   {
      JMenuItem item = new JMenuItem( ResourceLoader.getCommand( command ) );
      if ( key > 0 ) {
    	 item.setAccelerator(KeyStroke.getKeyStroke(key, ActionEvent.CTRL_MASK));
      }
      item.setActionCommand( command );
      item.setEnabled( enabled );
      item.addActionListener( Global.mainActionListener );
      return item;
   }

   /** Creates a menu item created from a standard Action definition.   
    *  @return <code>JMenuItem</code>  menu item
    */
   public static JMenuItem makeItem( Action action, int key )
   {
      JMenuItem item   = new JMenuItem( action );
      if ( key > 0 ) {
    	 item.setAccelerator(KeyStroke.getKeyStroke(key, ActionEvent.CTRL_MASK));
      }
      item.setIcon( null );
      item.setToolTipText( null );
      return item;
   }

   private static JRadioButtonMenuItem makeRadioItem ( String token, 
		          ButtonGroup group, int key, boolean enabled )
   {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(ResourceLoader.getCommand(token));
      if ( key > 0 ) {
    	 item.setAccelerator(KeyStroke.getKeyStroke(key, ActionEvent.CTRL_MASK));
      }
      group.add( item );
      item.setActionCommand( token );
      item.setEnabled( enabled );
      item.addActionListener( Global.mainActionListener );
      return item;
   }

      private static JMenu buildRecordMenu()
   {
      JMenu menu  = new JMenu( ResourceLoader.getCommand("menu.edit.psw") );
      menu.addMenuListener( mListener );
      menu.setEnabled( false );
   
      editAdd  = makeItem( "menu.edit.psw.add", false );
      editAdd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));     
      menu.add( editAdd );
   
      editEdit  = makeItem( "menu.edit.psw.edit", false );
      editEdit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));     
      menu.add( editEdit );
   
      editDelete  = makeItem( "menu.edit.psw.delete", false );
      editDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));     
      menu.add( editDelete );
   
      return menu;
   }

   private static JMenu buildDebugMenu()
   {
	  JMenu menu  = new JMenu( ResourceLoader.getCommand("menu.help.debug") );
      menu.addMenuListener( mListener );
//      menu.setEnabled( false );

      // menu point: open inspector window for IOManager
      JMenuItem item  = makeItem( "menu.debug.iomanager", true );
      menu.add( item );
   
//      item  = makeItem("menu.debug.transferzip", true );
//      menu.add( item );

//      item  = makeItem("menu.debug.test1", true );
//      menu.add( item );

      return menu;
   }

   private static JMenu buildFileMenu()
   {
      boolean isDesktop = Options.getIntOption( "displayState" ) == DisplayManager.DISPLAY_DESKTOP;
      
      JMenu menu = new JMenu( ResourceLoader.getCommand( "menu.file" ) );
      menu.addMenuListener( mListener );
	
      JMenuItem item	= makeItem( "menu.file.new", true );
      menu.add( item );
	
      item	= makeItem( "menu.file.open", KeyEvent.VK_O, true );
      menu.add( item );
	
      fileClose  = makeItem("menu.file.close", KeyEvent.VK_X, false );
      menu.add( fileClose );
   
      fileCloseAll  = makeItem( ActionHandler.getAction( "CLOSEALL" ), 0 );
      fileCloseAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
      fileCloseAll.setVisible( isDesktop );
      menu.add( fileCloseAll );
   
      menu.addSeparator();
      
      recentMenu = new JMenu( ResourceLoader.getCommand("menu.file.openrecent"));
      recentMenu.addMenuListener( Global.recentFiles );
      recentMenu.addMenuListener( mListener );
      recentMenu.setVisible( Options.isOptionSet( "useRecentList" ) );
      menu.add( recentMenu );
	
      item  = makeItem( "menu.file.openurl", true );
      menu.add( item );
   
      item  = makeItem( "menu.file.import", true );
      menu.add( item );
   
      fileConvert  = makeItem( "menu.file.convert", true );
      fileConvert.setVisible( false );
      menu.add( fileConvert );
   
      revertMenu = new OldStateMenu();
      menu.add( revertMenu );
   
      menu.addSeparator();
      
      fileSave	= makeItem("menu.file.save", KeyEvent.VK_S, false );
      menu.add( fileSave );
	
      fileSaveAll  =  makeItem( ActionHandler.getAction( "SAVEALL" ), 0 );
      fileSaveAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
      fileSaveAll.setVisible( isDesktop );
      menu.add( fileSaveAll );
    
      fileSaveAs	= makeItem("menu.file.saveas", false );
      menu.add( fileSaveAs );
	
      fileSaveCopy  = makeItem("menu.file.savecopy", false );
      menu.add( fileSaveCopy );
   
      menu.addSeparator();
	
      item	= makeItem( "menu.file.exit", KeyEvent.VK_F4, true );
      menu.add( item );

      return menu;
	}

	private static JMenu buildHelpMenu()
	{
		JMenu		menu;
		JMenuItem	item;
	
	  menu	= new JMenu( ResourceLoader.getCommand("menu.help") );
      menu.addMenuListener( mListener );
/*	
		item	= makeItem("menu.help.docs", true );
      item.setEnabled( false );
		menu.add( item );
*/
      // Generate Random Password
      item  = makeItem("menu.help.genrandpass", KeyEvent.VK_J, true );
      menu.add( item );

      // Wipe a File
      item  = makeItem("menu.help.wipefile", true );
      menu.add( item );

      // Portable Install
      item  = makeItem("menu.help.portableinstall", true );
      menu.add( item );

      menu.addSeparator();

      // File Info
      fileInfo  = makeItem("menu.help.fileinfo", KeyEvent.VK_I, false );
      menu.add( fileInfo );

      // System Info
      item  = makeItem("menu.help.system", KeyEvent.VK_Y, true );
      menu.add( item );

      // Program Info (release notes)
      item  = makeItem("menu.help.releasenotes", true );
      menu.add( item );

      menu.addSeparator();
      
      // Support Page (internet)
      item  = makeItem("menu.help.support", true );
      item.setIcon( ResourceLoader.getImageIcon( "webicon" ));
      item.setHorizontalTextPosition( SwingConstants.LEADING );
      menu.add( item );

      // Check for News (internet)
      item  = makeItem("menu.help.checknews", true );
      menu.add( item );
      
      // TEST FUNCTION TRIGGER
      if ( Global.isDebug() ) {
          menu.addSeparator();
          
    	  // debug sub menu
          menu.add( buildDebugMenu() );
      }
      
      monitorCheck = new JCheckBoxMenuItem( ResourceLoader.getCommand("menu.help.monitor") );
      monitorCheck.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));     
      monitorCheck.setSelected( Options.isOptionSet( "monitorSystem" ));
      monitorCheck.setActionCommand( "menu.help.monitor" );
      monitorCheck.addActionListener( Global.mainActionListener );
      menu.add( monitorCheck );
      
      menu.addSeparator();
      
      item	= makeItem("menu.help.about", true );
      menu.add( item );

      return menu;
	}

	private static JMenu buildManageMenu()
	{
	   JMenu menu = new JMenu( ResourceLoader.getCommand("menu.manage") );
       menu.addMenuListener( mListener );
	
	   manageChangePw	= makeItem("menu.manage.changepass", false );
	   menu.add( manageChangePw );
	
       managePolicy = makeItem("menu.manage.policy", false );
       menu.add( managePolicy );
   
       menu.addSeparator();
        
       manageMergefile = makeItem("menu.manage.mergefile", false );
       menu.add( manageMergefile );
   
       manageImportFileCSV = makeItem("menu.manage.importfile.csv", false);
       menu.add( manageImportFileCSV );
   
       manageExportFileCSV = makeItem("menu.manage.exportfile.csv", false);
       menu.add( manageExportFileCSV );
   
	   menu.addSeparator();
	
	   manageBackup	= makeItem("menu.manage.backup", KeyEvent.VK_W, false);
	   menu.add( manageBackup );
	
	   manageRestore	= makeItem("menu.manage.restore", false );
	   menu.add( manageRestore );
	
 	   return menu;
	}

	private static JMenu buildViewMenu()
	{
		JMenu		menu;
		JRadioButtonMenuItem	item;
        JMenuItem item2;
		ButtonGroup	group, group2;
	
      group	= new ButtonGroup();
      group2 = new ButtonGroup();
      menu	= new JMenu( ResourceLoader.getCommand("menu.view") );
      menu.addMenuListener( mListener );

      item = new JRadioButtonMenuItem( ResourceLoader.getCommand("menu.view.list") );
      item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));     
      item.setActionCommand( "menu.view.list" );
      item.addActionListener( Global.mainActionListener );
      item.setEnabled( false );
      group.add( item );
      menu.add( item );
      viewTable = item;
	
      item	= new JRadioButtonMenuItem( ResourceLoader.getCommand("menu.view.tree") );
      item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));     
      item.setActionCommand( "menu.view.tree" );
      item.addActionListener( Global.mainActionListener );
      item.setEnabled( false );
      group.add( item );
      menu.add( item );
      viewTree = item;

      menu.addSeparator();

      item  = new JRadioButtonMenuItem( ResourceLoader.getCommand("menu.view.single") );
      item.setActionCommand( "menu.view.single" );
      item.addActionListener( Global.mainActionListener );
      item.setEnabled( true );
      group2.add( item );
      menu.add( item );
      viewSingle = item;
      
      item  = new JRadioButtonMenuItem( ResourceLoader.getCommand("menu.view.desktop") );
      item.setActionCommand( "menu.view.desktop" );
      item.addActionListener( Global.mainActionListener );
      item.setEnabled( true );
      group2.add( item );
      menu.add( item );
      viewDesktop = item;
      
      menu.addSeparator();
      
      filterMenu = buildFilterMenu();
      menu.add( filterMenu );
      
      menu.addSeparator();
      
      trayIconCheck = new JCheckBoxMenuItem( ResourceLoader.getCommand("menu.view.mintotrayicon") );
      trayIconCheck.setSelected( Options.isOptionSet( "minToTrayIcon" ) );
      trayIconCheck.setActionCommand( "menu.view.mintrayicon" );
      trayIconCheck.addActionListener( Global.mainActionListener );
      menu.add( trayIconCheck );
      
      item2 = makeItem( "menu.view.idle", true );
      item2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));     
      menu.add( item2 );

      setViewType( Options.getIntOption("defaultViewType") );
      setMainframeDisplayState( Options.getIntOption("displayState") );
      return menu;
	}
   
   
   private static JMenu buildFilterMenu ()
   {
      JMenu menu;
      ButtonGroup group;
      boolean check;
      
      menu = new JMenu( ResourceLoader.getCommand( "menu.view.filter" ) );
      menu.setEnabled( false );
      
      group = new ButtonGroup();
      filterAll = makeRadioItem( "menu.view.filter.all", group, KeyEvent.VK_N, true );
      filterAll.setSelected( true );
      menu.add( filterAll );
      
      menu.addSeparator();
      
      filterInvalid = makeRadioItem( "menu.view.filter.invalid", group, KeyEvent.VK_J, false );
      filterInvalid.setForeground( Color.RED );
      filterInvalid.setVisible( false );
      menu.add( filterInvalid );

      check = Options.isOptionSet( "useFavourites" );
      filterFavourites = makeRadioItem( "menu.view.filter.favourites", group, KeyEvent.VK_B, check );
      filterFavourites.setVisible( check );
      menu.add( filterFavourites );
      
      filterExpiry = makeRadioItem( "menu.view.filter.expiry", group, KeyEvent.VK_E, true );
      menu.add( filterExpiry );

      filterImport = makeRadioItem( "menu.view.filter.import", group, 0, true );
      menu.add( filterImport );
      
      filterModify = makeRadioItem( "menu.view.filter.modify", group, KeyEvent.VK_G, true );
      menu.add( filterModify );
      
      return menu;
   }

   public static JMenu buildFavouriteMarkMenu ()
   {
      JMenu menu;

      menu = new JMenu( ResourceLoader.getCommand( "menu.edit.markfavourites" ) );
      menu.add( makeItem( "menu.edit.favmark.set", true ) );
      menu.add( makeItem( "menu.edit.favmark.cancel", true ) );
      
      return menu;
   }
   
   /** Sets the display filter status for the menu items. 
    * 
    * @param v 
    */ 
   public static void setFilterModus ( int v )
   {
      JRadioButtonMenuItem item;
      String name;
      
      switch ( v )
      {
      case FILTER_SHOWALL :  item = filterAll;
           name = "ALL";
      break;
      case FILTER_SHOW_EXIRING :  item = filterExpiry;
           name = "EXPIRING";
      break;
      case FILTER_SHOW_IMPORTED :  item = filterImport;
           name = "IMPORTED";
      break;
      case FILTER_SHOW_MODIFIED :  item = filterModify;
           name = "MODIFIED";
      break;
      case FILTER_SHOW_INVALID :  item = filterInvalid;
           name = "INVALID";
      break;
      case FILTER_SHOW_FAVOURITES :  item = filterFavourites;
           name = "FAVOURITES";
      break;
      default: item = null; name = " - null -";
      }

      if (item != null )
      {
         item.setSelected( true );
      }
      Log.log( 7, "(MenuHandler.setFilterSelection) new FILTER MODUS == ".concat( name ) );
   }
   
   /** Sets the menu items for file-container viewtype according to a given viewtype
    *  value. */
   public static void setViewType( int v )
   {
      if ( v == PwsFileContainer.NO_VIEW )
         return;

      (v == PwsFileContainer.TABLE_VIEW ? viewTable : viewTree).setSelected( true );
      Log.log( 7, "(MenuHandler.setViewType) VIEW == ".concat( v == PwsFileContainer.TABLE_VIEW ?
            "TABLE" : "TREE") );
   }
   
   public static void setMainframeDisplayState ( int state )
   {
      (state == DisplayManager.DISPLAY_DESKTOP ? viewDesktop : viewSingle).setSelected( true );
   }
   
   public static void setRecentMenuVisible ( boolean v )
   {
      recentMenu.setVisible( v );
   }
   
   /**
    * @since 0-5-0
    */
   public static void setEntryListsVisible ( boolean v )
   {
      lastUsedMenu.setVisible( v );
      lastEditedMenu.setVisible( v );
   }
   
   /**
    * Sets text and manages visibility for UNDO and REDO commands
    * in the menu, referring to the currently selected file container.
    * 
    * @since 0-5-0
    */
   public static void undoUpdated()
   {  
      PwsFileContainer ct;
      UndoManager undoMg = null;
      String hstr;
      boolean haveFile, hasUndo, hasRedo;
      
      ct = Global.getSelectedFile();
      if ( (haveFile = ct != null) )
         undoMg = ct.getUndoManager();

      hasUndo = haveFile && undoMg.canUndo();
      hasRedo = haveFile && undoMg.canRedo();
      
      // UNDO item settings
      if ( hasUndo )
      {
         hstr = //ResourceLoader.getCommand( "menu.edit.undo" ) + " : " +
            undoMg.getUndoPresentationName();
         editUndo.setText( hstr );
      }
      editUndo.setVisible( hasUndo );
      
      // REDO item settings
      if ( hasRedo )
      {
         hstr = //ResourceLoader.getCommand( "menu.edit.redo" ) + " : " +
            undoMg.getRedoPresentationName();
         editRedo.setText( hstr );
      }
      editRedo.setVisible( hasRedo );
      
      // visibility of separator
      editUndoSeparator.setVisible( hasUndo | hasRedo );
   }  // undoUpdated

   public static void fileUpdated( PwsFileContainer file )
   {
      boolean modified;
      
      modified = file.isModified();
      fileSave.setEnabled( modified );
      revertMenu.setEnabled( (modified && file.hasPersistentFile()) || file.getAutoBackups().size() != 0 );
      fileConvert.setVisible( file.getFileFormat() != Global.FILEVERSION_LATEST );
      lastEditedMenu.setEnabled( file.getLastEditedList().getSize() != 0 );
      lastUsedMenu.setEnabled( file.getLastUsedList().getSize() != 0 );
      
      if ( modified )
         ActionHandler.getAction( "SAVEALL" ).setEnabled( true );
      else
         checkAllFiles();

      Log.log( 7, "(MenuHandler.fileUpdated) MODIFIED == ".concat( modified ? "TRUE" : "FALSE") );
   } 
   
   /**
    * Adjusts menus to reflect the current operation mode of the
    * parameter file container.
    *  
    * @param ct <code>PwsFileContainer</code>
    */
	public static void setOperationMode( PwsFileContainer ct )
	{
       RecentList rList;
      boolean open, modified, check;
   
      if ( ct == null )
         return;
      
      open = ct.getOperationMode() == PwsFileContainer.MOUNTED_ACTIVE;
      modified = ct.isModified();
      Log.log( 7, "(MenuHandler.setOperationMode) FILE IS ( " + (open ? "OPEN" : "CLOSED" ) + " ) : " + 
            ct.getDatabaseName() );

      editAdd.setEnabled( open );
      editFind.setEnabled( open );
      editQuickFind.setEnabled( open );
      manageChangePw.setEnabled( open );
      managePolicy.setEnabled( open );
      manageBackup.setEnabled( open );
      manageRestore.setEnabled( open );
      manageMergefile.setEnabled( open );
      manageExportFileCSV.setEnabled( open );
      manageImportFileCSV.setEnabled( open );
      recordMenu.setEnabled( open );
      fileClose.setEnabled( open );
      fileSave.setEnabled( open & modified );
      revertMenu.setEnabled( open && ((modified && ct.hasPersistentFile()) || ct.getAutoBackups().size() != 0) );
      fileConvert.setVisible( open && ct.getFileFormat() != Global.FILEVERSION_LATEST );
      fileSaveAs.setEnabled( open );
      fileSaveCopy.setEnabled( open );
      viewTable.setEnabled( open );
      viewTree.setEnabled( open );
      filterMenu.setEnabled( open );
      editSelectAll.setEnabled( open );
      lastEditedMenu.setEnabled( open && ct.getLastEditedList().getSize() != 0 );
      lastUsedMenu.setEnabled( open && ct.getLastUsedList().getSize() != 0 );
      fileInfo.setEnabled( open );

      checkAllFiles();

      // set "filter invalid records" command
       check = open && ct.hasInvalidRecs();
       filterInvalid.setVisible( check );
       filterInvalid.setEnabled( check );
      
      // update UNDO/REDO commands
      undoUpdated();
      
      // update LAST USED menu
      rList = ct.getLastUsedList();
      if ( open )
         lastUsedMenu.addMenuListener( rList );
      else
      { 
         lastUsedMenu.removeAll();
         lastUsedMenu.removeMenuListener( rList );
      }
      
      // update LAST EDITED menu
      rList = ct.getLastEditedList();
      if ( open )
         lastEditedMenu.addMenuListener( rList );
      else
      { 
         lastEditedMenu.removeAll();
         lastEditedMenu.removeMenuListener( rList );
      }
      
      setSelectionStatus( ct );
	} // setOperationMode

    /**
     * Adjusts menus to reflect the current list selection status of the
     * parameter file container.
     *  
     * @param ct <code>PwsFileContainer</code>
     */
	public static void setSelectionStatus( PwsFileContainer ct )
	{
       boolean isLeafNode, isSelection, open;
       int status;

       open = ct.getOperationMode() == PwsFileContainer.MOUNTED_ACTIVE;
       status = ct.getSelectionStatus();
       isLeafNode = open & status == PwsFileContainer.RECORD_SELECTED;
       isSelection = open & status != PwsFileContainer.NOTHING_SELECTED;
       
	   editEdit.setEnabled( isLeafNode );
	   editDelete.setEnabled( isSelection );
	   editCopyUser.setEnabled( isLeafNode );
	   editCopyPass.setEnabled( isLeafNode );
//     editRenameGroup.setEnabled( status == PwsFileContainer.GROUP_SELECTED );
       
       exportSelected.setEnabled( isSelection );
       moveToFileMenu.setEnabled( isSelection & DisplayManager.getFileCount() > 1 );
       copyToFileMenu.setEnabled( isSelection & DisplayManager.getFileCount() > 1 );
       Log.log( 7, "(MenuHandler.setSelectionStatus) new SELECTION STATUS == " +
             (isSelection ? "SELECTED ".concat(isLeafNode ? "RECORD(S)" : "GROUP") : "NOTHING ")  
              + " : " + ct.getDatabaseName() );
	}  // setSelectionStatus

   public static boolean isMonitorSelected ()
   {
      return monitorCheck.isSelected();
   }
   
   /**
	 * @return Returns the mainframe menu bar.
	 */
	public static JMenuBar getMenuBar()
	{
		return menuBar;
	}

    private static void setMenuElementFont ( MenuElement element, Font font )
    {
       MenuElement arr[];
       int i;
       
       if ( element == null )
          return;
       
       element.getComponent().setFont(font);
//System.out.println("- menu FONT update: " + element.getClass().getName() );           
       
       // recursively traverse all menu elements
       for ( i=0, arr=element.getSubElements(); i < arr.length; i++ )
       {
          setMenuElementFont( arr[i], font );
       }
    }
    
    public static void setMenuFont( Font font )
    {
       if ( font == null )
          return;
       
       UIDefaults def = UIManager.getLookAndFeelDefaults();
       def.put( "Menu.font", font );
       def.put( "MenuItem.font", font );
       def.put( "CheckBoxMenuItem.font", font );
       def.put( "RadioButtonMenuItem.font", font );
       def.put( "MenuBar.font", font );

       setMenuElementFont( menuBar, font );   
    }
    
    /** Returns the actual user choice concerning file-container viewtype
     *  expressed in <code>PwsFileContainer</code> view status values. */
   public static int getViewSelection ()
   {
      return viewTable.isSelected() ? PwsFileContainer.TABLE_VIEW 
            : PwsFileContainer.TREE_VIEW;
   }
   
   public static ChangeListener getChangeListener ()
   {
      return mListener;
   }
   
   public static void registerFile ( PwsFileContainer container )
   {
      if ( container != null )
      {
         Log.log( 5, "(MenuHandler.registerFile) registering file : ".concat(container.getDatabaseName()));
         container.addChangeListener( mListener );
      }
   }
   
   public static void unregisterFile ( PwsFileContainer container )
   {
      if ( container != null )
      {
         Log.log( 5, "(MenuHandler.unregisterFile) un-registering file : ".concat(container.getDatabaseName()));
         container.removeChangeListener( mListener );
      }
   }
   
   public static JPopupMenu getListviewContextMenu ( PwsFileContainer ct )
   {
      
      JPopupMenu menu;
      JMenu moveMenu, copyMenu, markFavMenu;
      JMenuItem copyPass, copyUser, deleteRec, unfold, fold, selectAll, selectRec,
                foldAll, duplicate, rename, export, move, deleteGrp, duplicateGrp,
                startURL, createGrp;
      int selectionStatus; 
      boolean singleRecordSelected;
      
      menu = new JPopupMenu();
      
      /* ** construct menu items ** */
      copyPass = makeItem( "menu.edit.copypass", true );
      copyUser = makeItem( "menu.edit.copyuser", true );
      deleteRec = makeItem( "menu.edit.delete.record", true );
      deleteGrp = makeItem( "menu.edit.delete.group", true );
      unfold = makeItem( "menu.edit.expandbranch", true );
      fold = makeItem( "menu.edit.foldbranch", true );
      selectAll = makeItem( "menu.edit.selectall", true );
      selectRec = makeItem( "menu.edit.selectrecords", true );
      foldAll = makeItem( "menu.edit.foldall", true );
      duplicate = makeItem( "menu.edit.duplicate", true );
      duplicateGrp = makeItem( "menu.edit.duplicategroup", true );
      rename = makeItem( "menu.edit.rename.group", true );
      startURL = makeItem( "menu.edit.starturl", true );
      export = makeItem( "menu.edit.export.csv", true );  
      move = makeItem( "menu.edit.moveentries", true );
      createGrp = makeItem( "menu.edit.newgroup", true );
      
      moveMenu = new MoveFileMenu( false );
      moveMenu.setVisible( DisplayManager.getFileCount() > 1 );

      copyMenu = new MoveFileMenu( true );
      copyMenu.setVisible( DisplayManager.getFileCount() > 1 );

      markFavMenu = buildFavouriteMarkMenu();
      markFavMenu.setVisible( Options.isOptionSet( "useFavourites" ) );
      
      selectionStatus = ct.getSelectionStatus();
      singleRecordSelected = selectionStatus == PwsFileContainer.RECORD_SELECTED;
      
      /* ** decide on components ** */
      // TREE VIEW
      if ( ct.getViewType() == PwsFileContainer.TREE_VIEW )
         switch ( selectionStatus )   
         {
         case PwsFileContainer.NOTHING_SELECTED:
            menu.add( selectAll );
            menu.add( foldAll );
            break;
            
         case PwsFileContainer.RECORD_SELECTED:
         case PwsFileContainer.RECORDSET_SELECTED:
            if ( singleRecordSelected )
            {
               menu.add( copyPass );
               menu.add( copyUser );
               if ( Util.extractURL( ct.getSelectedRecord().getUrl() ) != null )
                  menu.add( startURL );
            }
            menu.add( deleteRec );
            menu.add( duplicate );
            menu.add( markFavMenu );
   
            menu.addSeparator();
            if ( singleRecordSelected )
               menu.add( selectRec );
            menu.add( selectAll );
            menu.add( foldAll );
            
            menu.addSeparator();
            menu.add( move );
            menu.add( moveMenu );
            menu.add( copyMenu );
            menu.add( export );
            break;
            
         case PwsFileContainer.GROUP_SELECTED:
            menu.add( unfold );
            menu.add( fold );
            
            menu.addSeparator();
            menu.add( rename );
            menu.add( deleteGrp );
            menu.add( duplicateGrp );
            menu.add( createGrp );
   
            menu.addSeparator();
            menu.add( selectRec );
            menu.add( selectAll );
            menu.add( foldAll );
            
            menu.addSeparator();
            menu.add( move );
            menu.add( moveMenu );
            menu.add( copyMenu );
            menu.add( export );
            break;
         }
      
      // TABLE VIEW
      else
         switch ( selectionStatus )   
         {
         case PwsFileContainer.NOTHING_SELECTED:
            menu.add( selectAll );
            break;
            
         case PwsFileContainer.RECORD_SELECTED:
         case PwsFileContainer.RECORDSET_SELECTED:
            if ( singleRecordSelected )
            {
               menu.add( copyPass );
               menu.add( copyUser );
               if ( Util.extractURL( ct.getSelectedRecord().getUrl() ) != null )
                  menu.add( startURL );
            }
            menu.add( deleteRec );
            menu.add( duplicate );
            menu.add( markFavMenu );

            menu.addSeparator();
            if ( singleRecordSelected )
               menu.add( selectRec );
            menu.add( selectAll );
            
            menu.addSeparator();
            menu.add( move );
            menu.add( moveMenu );
            menu.add( copyMenu );
            menu.add( export );
            break;
         }
      
      return menu;
   }  // getListviewContextMenu
   
   //  ********  INNER CLASSES  **********
   
   private static class OptionListener implements OptionChangeListener
   {
      @Override
	  public void optionChanged ( OptionChangeEvent e )
      {
         String name;
         boolean check;
         
         name = e.getOptionName();
         if ( name.equals( "useRecentList" ) )
         {
            setRecentMenuVisible( Options.isOptionSet( name ) );
         }
//         else if ( name.equals( "storeMinorChanges" ) )
//         {
//            setEntryListsVisible( Options.isOptionSet( name ) );
//         }
         else if ( name.equals( "useFavourites" ) )
         {
            check = Options.isOptionSet( name );
            filterFavourites.setEnabled( check );
            filterFavourites.setVisible( check );
         }
         else if ( name.equals( "useUndoRedo" ) )
         {
            undoUpdated();
         }
         else if ( name.equals( "displayState" ) )
         {
            check = Options.getIntOption( "displayState" ) == DisplayManager.DISPLAY_DESKTOP;
            fileCloseAll.setVisible( check | DisplayManager.getFileCount() > 1 );
            fileSaveAll.setVisible( check | fileSaveAll.isEnabled() );
         }
         else if ( name.equals( "minToTrayIcon" ) )
         {
            trayIconCheck.setSelected( Options.isOptionSet( "minToTrayIcon" ) );
         }
      }
   }
   
   private static class MListener implements MenuListener, ChangeListener
   {
      //  ********* IMPLEMENTATION OF MENULISTENER  ************* 
      @Override
	public void menuCanceled ( MenuEvent e )
      {
      }
      @Override
	public void menuDeselected ( MenuEvent e )
      {
      }
      @Override
	public void menuSelected ( MenuEvent e )
      {
         ActionHandler.resetIdleTime();
      }
      
      //  ********* IMPLEMENTATION OF CHANGELISTENER  ************* 
      @Override
	public void stateChanged ( javax.swing.event.ChangeEvent evt )
      {
         PwsFileContainer ct;
         
         if ( evt.getSource() == null )
            return;
         
         if ( evt instanceof ChangeEvent &&
              evt.getSource() instanceof PwsFileContainer )
         {
            ct = (PwsFileContainer)evt.getSource();
            
            switch ( ((ChangeEvent)evt).getState() )
            {
            // OPERATION MODE
            case PwsFileContainer.OPERATION_MODE:
//               System.out.println( "MenuHandler : received event OPERATION_MODE" );
               setOperationMode( ct );
               break;
               
            case PwsFileContainer.SELECTION_STATUS: 
               setSelectionStatus( ct );
               break;
            
            case PwsFileContainer.DISPLAY_MODE: 
               setViewType( ct.getViewType() );
               break;

            case PwsFileContainer.FILTER_MODE: 
               setFilterModus( ct.getFilterStatus() );
               break;

            case PwsFileContainer.MODIFY_EVENT: 
               Global.mainFrame.setTitleFile( ct );
               fileUpdated( ct );
               break;
            }
         }
        
         else if ( evt.getSource() == DisplayManager.class )
         {
            ActionHandler.getAction( "CLOSEALL" ).setEnabled( DisplayManager.hasOpenFiles() );
         }
         
      }  // stateChanged
      
   }

   //  ********  INNER CLASSES  **********
      /**
       * Sub-menu for the list of automatic backup copies of the currently active
       * database. 
       * 
       * @since 0-5-0
       */
      public static class OldStateMenu extends JMenu
      {
         public static final Color PRIMARYFILE_COLOR = new Color( 0x8B008B ); 
         public static final String COMMAND_BASE = "menu.file.revert."; 
      
      
         public OldStateMenu ()
         {
            setText( ResourceLoader.getCommand( "menu.file.revert" ) );
            setEnabled( false );
         }
      
         private String itemText ( long time )
         {
            return Global.getLocalDateTime( time );
         }
         
         /* 
          * Overridden: @see javax.swing.JMenu#fireMenuSelected()
          */
         @Override
		protected void fireMenuSelected ()
         {
            PwsFileContainer ct;
            List<ContextFile> flist;
            Iterator<ContextFile> it;
            JMenuItem item;
            String hstr, path;
            long time;
            
            removeAll();
            if ( (ct = Global.getSelectedFile()) != null )
            {
               flist = ct.getAutoBackups();
   
               // construct primary state item
               if ( ct.isModified() && ct.hasPersistentFile() )
               {
                  hstr = itemText( ct.getStoreTime() );
                  item = new JMenuItem( hstr );
                  item.setForeground( PRIMARYFILE_COLOR );
                  item.setActionCommand( COMMAND_BASE.concat( ct.getFilePath() ) );
                  item.addActionListener( Global.mainActionListener );
                  add( item );

                  if ( flist.size() != 0 )
                     addSeparator();
               }
                  
               // construct auto-backup entries
               for ( it = flist.iterator(); it.hasNext(); )
               {
                  path = it.next().getFilepath();
                  try {
                     time = Util.timeFromSECPath( path );
                     if ( time != -1 )
                     {
                        hstr = itemText( time );
                        item = new JMenuItem( hstr );
                        item.setActionCommand( COMMAND_BASE.concat( path ) );
                        item.addActionListener( Global.mainActionListener );
                        add( item );
                     }
                  }
                  catch ( Exception e )
                  {}
               }
               
               if ( flist.size() != 0 )
               {
                  addSeparator();
                  item = makeItem( "menu.file.erasebackups", true );
                  add( item );
               }
            }
            
            super.fireMenuSelected();
         }  // fireMenuSelected
      }  // class OldStateMenu

      
      public static class MoveFileMenu extends JMenu
         {
            public static final Color TARGETFILE_COLOR = new Color( 0x8B008B ); 
            private boolean copy;
         
            public MoveFileMenu ( boolean copy )
            {
               setText( ResourceLoader.getCommand( 
                     copy ? "menu.edit.copytofile" : "menu.edit.movetofile" ) );
               this.copy = copy;
            }
         
            /* 
             * Overridden: @see javax.swing.JMenu#fireMenuSelected()
             */
            @Override
			protected void fireMenuSelected ()
            {
               PwsFileContainer fc;
               Iterator<PwsFileContainer> it;
               JMenuItem item;
               String baseCmd;
               
               removeAll();
               
               // construct dynamic items
               if ( DisplayManager.getFileCount() > 1 )
               {
                  for ( it = DisplayManager.getContainerIterator(); it.hasNext(); )
                  {
                     // get next container; avoid listing the actual selected CT
                     fc = it.next();
                     if ( DisplayManager.getSelectedContainer() == fc )
                        continue;
                     
                     item = new JMenuItem( fc.getDatabaseName() );
                     item.setForeground( TARGETFILE_COLOR );
                     baseCmd = copy ? "selection.copytofile." : "selection.movetofile.";
                     item.setActionCommand( baseCmd.concat( fc.getUUID().toHexString() ));
                     item.addActionListener( Global.mainActionListener );
                     add( item );
                  }
               }
               
               super.fireMenuSelected();
            }
         }  // class

      public static void checkAllFiles ()
       {
          boolean check;
          
          check = false;
          if ( DisplayManager.getFileCount() > 0 )
          for ( Iterator<PwsFileContainer> it = DisplayManager.getContainerIterator(); it.hasNext(); )
             if ( it.next().isModified() )
      	      { 
                check = true;
      	         break;
      	      }
          
          ActionHandler.getAction( "SAVEALL" ).setEnabled( check );
       }

}
