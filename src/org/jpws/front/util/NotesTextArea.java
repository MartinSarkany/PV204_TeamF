/*
 *  NotesTextArea in org.jpws.front.util
 *  file: NotesTextArea.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 13.01.2007
 *  Version
 * 
 *  Copyright (c) 2007 by Wolfgang Keller, Munich, Germany
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

package org.jpws.front.util;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.net.URL;
import java.util.HashMap;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;
import javax.swing.undo.UndoManager;

import org.jpws.data.Options;
import org.jpws.front.ActionHandler;
import org.jpws.front.GUIService;
import org.jpws.front.Global;
import org.jpws.front.MenuHandler;

/**
 * This extension of <code>JTextArea</code> adds the following features to the editor field.
 * 
 * <p>1. Undo/Redo manager with 100 operations stack
 * <br>2. a context menu popping up at mouse right-click with commands to
 * handle clipboard exchange, undo/redo, line-wrapping and select-all
 * <br>3. keystroke support with these (additional) assignments: CTRL-W (select-word),
 * CTRL-L (select line), CTRL-P (select paragraph), CTRL-Z (undo), CTRL-Y (redo)  
 *  
 * @since 0-5-0
 */

public class NotesTextArea extends JTextArea 
{
   private static HashMap<Object, Action> actionLookup;
   protected ActionListener actions = new Actions();
   private UndoManager undoManager = new UndoManager();
   private PopupListener popupListener = new PopupListener();
   
   private Window owner;
   private boolean isPopupActive = true;

public NotesTextArea ( String name )
{
   super();
   init( name );
}

public NotesTextArea ( String name, int rows, int columns )
{
   super( rows, columns );
   init( name );
}

public NotesTextArea ( String name, String text )
{
   super( text );
   init( name );
}

public NotesTextArea ( String name, String text, int rows, int columns )
{
   super( text, rows, columns );
   init( name );
}

public NotesTextArea ( String name, Document doc )
{
   super( doc );
   init( name );
}

public NotesTextArea ( String name, Document doc, String text, int rows, int columns )
{
   super( doc, text, rows, columns );
   init( name );
}

private void init ( String name )
{
   Action actionList[];

   if ( actionLookup == null ) {
      //  get all the actions JTextArea provides for us
      actionList = getActions();
      
      // put them in a Hashtable so we can retrieve them by Action.NAME
      actionLookup = new HashMap<Object, Action>();
      for (int j=0; j < actionList.length; j+=1) {
        actionLookup.put(actionList[j].getValue(Action.NAME), actionList[j]);
   //     System.out.println( "-- TextArea Action: " + actionList[j].getValue(Action.NAME) );
      }
   }

   setName( name );
   addMouseListener( popupListener );
   getDocument().addUndoableEditListener( undoManager );
   modifyKeystrokes ();

   setLineWrap( Options.isOptionSet( "editLineWrap" ) );
   setWrapStyleWord( true );
}

private void modifyKeystrokes ()
{
   Keymap parent, map;
   Action action;
   KeyStroke key;
   
   // fetch or create the keymap specific to JPWS text areas
   parent = getKeymap();
   map = JTextComponent.addKeymap( "JPWS_TextAreaKeymap", parent );
   
   // add CTRL-W: select current word 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_MASK );
   action = actionLookup.get( DefaultEditorKit.selectWordAction );
   map.addActionForKeyStroke(key, action);
   
   // add CTRL-L: select current line 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_L, InputEvent.CTRL_MASK );
   action = actionLookup.get( DefaultEditorKit.selectLineAction );
   map.addActionForKeyStroke(key, action);
   
   // add CTRL-P: select current paragraph 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_P, InputEvent.CTRL_MASK );
   action = actionLookup.get( DefaultEditorKit.selectParagraphAction );
   map.addActionForKeyStroke(key, action);
   
   // add CTRL-Z: undo action 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_Z, InputEvent.CTRL_MASK );
   map.addActionForKeyStroke(key, new UndoAction() );
   
   // add CTRL-Y: redo action 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_Y, InputEvent.CTRL_MASK );
   map.addActionForKeyStroke(key, new RedoAction() );
      
   // add CTRL-D: insert current date (local) action 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_D, InputEvent.CTRL_MASK );
   map.addActionForKeyStroke(key,  new Actions( "keystroke.CTRL-D" ));

   // add CTRL-T: insert current time (local) action 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_T, InputEvent.CTRL_MASK );
   map.addActionForKeyStroke(key,  new Actions( "keystroke.CTRL-T" ));

   // add CTRL-U: insert current date+time (UT) action 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_U, InputEvent.CTRL_MASK );
   map.addActionForKeyStroke(key, new Actions( "keystroke.CTRL-U" ));

   // add CTRL-U: insert current date+time (UT) action 
   key = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );
   map.addActionForKeyStroke(key, new Actions( "menu.help"));

   // activate keymap for this text area
   setKeymap( map );
}  // modifyKeystrokes

/** Removes all entries from the undo-manager. */
public void clearUndoList ()
{
   undoManager.discardAllEdits();
}

/**
 * Sets the feature for popup menu active or inactive.
 * (Default value is <b>true</b>.)
 * 
 * @param v boolean <b>true</b> == popup active
 */
public void setPopupActive ( boolean v )
{
   isPopupActive = v;
}

/**
 * Whether popup menu feature is active in this text area. 
 * @return boolean 
 */
public boolean getPopupActive ()
{
   return isPopupActive;
}

/** Returns the text of the line where the caret is currently positioned.
 *  @return String line text or <b>null</b> if unavailable 
 */
public String getCurrentLine ()
{
   String text;
   int cp, line, offs;
   
   cp = getCaretPosition();
   try { 
      line = getLineOfOffset(cp); 
      offs = getLineStartOffset(line);
      text = getText( offs, getLineEndOffset(line) - offs );
      return text;
   } 
   catch (BadLocationException e) {
      e.printStackTrace();
      return null;
   }
}

/** Renders the valid text range for operations that assume the 
 * entire field if nothing is selected, but the user selection otherwise.
 * 
 * @return Dimension with width = start position, height = end position
 */
public Dimension getOperationSelection ()
{
   int start, end;
   Document doc = getDocument();
   String selText = getSelectedText();
   if ( selText != null )
   {
      start = getSelectionStart();
      end = getSelectionEnd();
   }
   else
   {
      start = 0;
      end = doc.getLength();
   }
   return new Dimension( start, end );
}

/**
 * Renders a popup menu for the context of this text area
 * including actual options of the UNDO manager.
 * 
 * @return <code>JPopupMenu</code>
 */
protected JPopupMenu getPopupMenu ()
{
   JPopupMenu menu;
   JMenuItem item;
   JMenu subMenu;
   Action action;
   URL urlArr[];
   String hstr, addArr[];
   
   menu = new JPopupMenu();
   requestFocus();

   if ( undoManager.canUndo() ) {
      item = new JMenuItem( new UndoAction() );
      item.setAccelerator( KeyStroke.getKeyStroke(
            KeyEvent.VK_Z, ActionEvent.CTRL_MASK) );
      menu.add( item );
   }
   
   if ( undoManager.canRedo() ) {
      item = new JMenuItem( new RedoAction() );
      item.setAccelerator( KeyStroke.getKeyStroke(
            KeyEvent.VK_Y, ActionEvent.CTRL_MASK) );
      menu.add( item );
   }

   if ( undoManager.canUndo() || undoManager.canRedo() )
      menu.addSeparator();
   
   // the standard CUT action (clipboard)
   item = makeMenuItem( "menu.edit.cut" );
   menu.add( item );
   
   // the standard COPY action (clipboard)
   item = makeMenuItem( "menu.edit.copy" );
   menu.add( item );
   
   // the standard PASTE action (clipboard)
   item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.paste" ) );
   action = actionLookup.get( DefaultEditorKit.pasteAction );
   item.addActionListener( action );
   menu.add( item );

   // erases a text selection if present, otherwise the entire field 
   item = makeMenuItem( "menu.edit.erase" );
   menu.add( item );

   menu.addSeparator();

   // investigate current text selection or entire text line
   if ( ((hstr = getSelectedText()) != null || (hstr = getText()) != null) )
   {
      // investigate for browsing URLs (multiple occurrences enabled)
      if ( (urlArr = Util.extractURLs( hstr )) != null ) 
      {
         if ( urlArr.length == 1 )
         {
            // add browsing command if single url was found 
            item = menu.add( ActionHandler.getStartBrowserAction( urlArr[0] , false ));
            item.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
         }
         else
         {
            // create and add a submenu containing the URLs as item names
            subMenu = new JMenu( ResourceLoader.getCommand( "menu.edit.starturl" ) );
            subMenu.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
            menu.add( subMenu );
            for ( URL u : urlArr )
            {
               item = subMenu.add( ActionHandler.getStartBrowserAction( u , true ));
               item.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
            }
         }
      }
      
      // investigate for EMAIL ADDRESSES (multiple occurrences enabled)
      if ( (addArr = Util.extractMailAddresses( hstr )) != null ) 
      {
         if ( addArr.length == 1 )
         {
            // add browsing command if single url was found 
            item = menu.add( ActionHandler.getStartEmailAction( addArr[0], false ) );
            item.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
         }
         else
         {
            // create and add a submenu containing the URLs as item names
            subMenu = new JMenu( ResourceLoader.getCommand( "menu.edit.startmail" ) );
            subMenu.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
            menu.add( subMenu );
            for ( String h : addArr )
            {
               item = subMenu.add( ActionHandler.getStartEmailAction( h , true ));
               item.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
            }
         }
      }
   }


   // line wrapping option
   item = new JCheckBoxMenuItem( ResourceLoader.getCommand( "menu.edit.linewrap" ) );
   item.setSelected( getLineWrap() );
   item.setActionCommand( "menu.edit.linewrap" );
   item.addActionListener( actions );
   menu.add( item );

   // printing the text
   item = makeMenuItem( "menu.manage.print" );
   menu.add( item );
   
   item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.selectall" ) );
   action = actionLookup.get( DefaultEditorKit.selectAllAction );
   item.addActionListener( action );
   menu.add( item );
   
   menu.addSeparator();

   item = makeMenuItem( "menu.help" );
   menu.add( item );
   
   return menu;
}

public void setDialogOwner (Dialog owner)
{
   this.owner = owner; 
}

public void setFrameOwner (Frame owner)
{
   this.owner = owner; 
}

/** Creates a new menu item which executes in this class'es 
 * <code>Actions</code>. The name of the item is drawn from <code>
 * ResourceLoader.getCommand(token)</code> and the action
 * command == token. Optional a keyboard mnemonic can be set.
 * 
 * @param token String key expression for the rendered menu item
 * @param mnemonic int optional key value
 * @return <code>JMenuItem</code> or <b>null</b> if <code>token</code> 
 *         was <b>null</b> or empty 
 */
protected JMenuItem makeMenuItem ( String token, int mnemonic )
{
   if ( token == null || token.isEmpty() ) return null;
   
   JMenuItem item = new JMenuItem( ResourceLoader.getCommand( token ), mnemonic );
   item.setActionCommand( token );
   item.addActionListener( actions );
   return item;
}

/** Creates a new menu item which executes in this class'es 
 * <code>Actions</code>. The name of the item is drawn from <code>
 * ResourceLoader.getCommand(token)</code> and the action
 * command == token. 
 * 
 * @param token String key expression for the rendered menu item
 * @return <code>JMenuItem</code> or <b>null</b> if <code>token</code> 
 *         was <b>null</b> or empty 
 */
protected JMenuItem makeMenuItem( String token )
{
   return makeMenuItem( token, 0 );
}

//  *****************  inner classes  ****************

private class Actions extends AbstractAction implements ActionListener
{
   String command;
   
   public Actions ()
   {}
   
   public Actions ( String command ) {
      if ( command == null || command.isEmpty() )
         throw new IllegalArgumentException("null or empty ACTION COMMAND");
      
      this.putValue( ACTION_COMMAND_KEY, command );
   }

   public Actions ( String command, String name ) {
      this( command );
      if ( name != null )
         this.putValue( NAME, name );
   }

   public Actions ( String command, String name, int mnemonic ) {
      this( command, name );
      if ( mnemonic > 0 )
         this.putValue( MNEMONIC_KEY, mnemonic );
   }

   @Override
   public void actionPerformed ( ActionEvent e )
   {
      String hstr;
      
      String cmd = command == null ? e.getActionCommand() : command;
      if ( cmd == null ) return;
      
      try {
      if ( cmd.equals( "menu.edit.linewrap" ) ) {
         setLineWrap( !getLineWrap() );

      } else if ( cmd.equals( "menu.edit.erase" ) ) {
         try {
            Dimension adr = getOperationSelection();
            NotesTextArea.this.getDocument().remove( adr.width, adr.height-adr.width ); 
         } catch ( BadLocationException e1 ) { 
        	 e1.printStackTrace(); 
         }

      } else if ( cmd.equals( "menu.edit.copy" ) ) {
         try {
            Dimension adr = getOperationSelection();
            hstr = getText( adr.width, adr.height-adr.width );
            ActionHandler.sendClipboardText( null, hstr, null );
         } catch ( BadLocationException e1 ) { 
        	 e1.printStackTrace(); 
         }

      } else if ( cmd.equals( "menu.edit.cut" ) ) {
         try {
            Dimension adr = getOperationSelection();
            int pos = adr.width, len = adr.height-adr.width;
            hstr = getText( pos, len );
            ActionHandler.sendClipboardText( null, hstr, null );
            NotesTextArea.this.getDocument().remove( pos, len ); 
         } catch ( BadLocationException e1 ) { 
        	 e1.printStackTrace(); 
         }

      } else if ( cmd.equals( "menu.manage.print" ) ) {
         startPrinting();

      } else if ( cmd.equals( "menu.help" ) ) {
         GUIService.toggleHelpDialog( owner, "dlg.help.notespopup" );
      }

      else if ( cmd.equals( "keystroke.CTRL-U" ) )
      {
         String dtext = Util.standardTimeString( System.currentTimeMillis(),
               TimeZone.getTimeZone( "UTC" )).concat( " UT " );
         NotesTextArea.this.insert( dtext, NotesTextArea.this.getCaretPosition() );
      }
      else if ( cmd.equals( "keystroke.CTRL-D" ) )
      {
         String dtext = Util.standardTimeString( System.currentTimeMillis() );
         dtext = dtext.substring( 0, 11 );
         NotesTextArea.this.insert( dtext, NotesTextArea.this.getCaretPosition() );
      }
      else if ( cmd.equals( "keystroke.CTRL-T" ) )
      {
         String dtext = Util.standardTimeString( System.currentTimeMillis() );
         dtext = dtext.substring( 11 ).concat( " " );
         NotesTextArea.this.insert( dtext, NotesTextArea.this.getCaretPosition() );
      }
   }
      // uncaught exception during any command (protects the caller) 
      catch ( Exception e1 )
   {
      e1.printStackTrace();
      GUIService.failureMessage( owner, "Unable to excute command: ".concat( cmd ), e1 );
   }
   }
}  // Actions


private class UndoAction extends TextAction
{
   public UndoAction ()
   {
      super( undoManager.getUndoPresentationName() );
   }

   public void actionPerformed ( ActionEvent e )
   {
      if ( undoManager.canUndo() )
         undoManager.undo();
   }
}

private class RedoAction extends TextAction
{
   public RedoAction ()
   {
      super( undoManager.getRedoPresentationName() );
   }

   public void actionPerformed ( ActionEvent e )
   {
      if ( undoManager.canRedo() )
         undoManager.redo();
   }
}
/*
private class LineWrapAction extends TextAction
{
   public LineWrapAction ()
   {
      super( ResourceLoader.getCommand( "menu.edit.linewrap" ) );
   }

   public void actionPerformed ( ActionEvent e )
   {
      setLineWrap( !getLineWrap() );
   }
}
*/
private class PopupListener extends MouseAdapter 
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
    if ( e.isPopupTrigger() && getPopupActive() ) 
        getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
}
}  // PopupListener

private class PrintJob implements Runnable
{

   @Override
   public void run ()
   {
      try
      {
         print();
      }
      catch ( PrinterException e1 )
      {
         e1.printStackTrace();
         GUIService.failureMessage( owner, "Unable to print the text!", e1 );
      }
   }
}

public void startPrinting ()
{
   String hstr, name;
   int i, lines, length;
   boolean hasLongLine;
   
   // set Attribute "LineWrap" to user option
   hasLongLine = false;
   if ( !getLineWrap() )
   {
      // check for cut lines
      lines = getLineCount();
      for ( i = 0; i < lines; i++ )
      {
         try { length = getLineEndOffset( i ) - getLineStartOffset( i ); }
         catch ( BadLocationException e1 ) 
         { length = 0; }
         hasLongLine |= length > 80;
         if ( hasLongLine )
            break;
      }
      
      // ask user for line wrapping if we have a long line
      name = getName() == null ? "?" : getName();
      hstr = ResourceLoader.getDisplay( "msg.ask.longlineswrap" );
      hstr = Util.substituteText( hstr, "$name", name );
      if ( hasLongLine && GUIService.userConfirm( owner, hstr ) )
         setLineWrap( true );
   }

   // start a thread with the print job
   if ( Global.mainFrame.isVisible() )
      ActionHandler.startTask( new PrintJob() );
}

}
