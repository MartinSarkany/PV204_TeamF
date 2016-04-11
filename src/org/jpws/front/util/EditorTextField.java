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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;
import javax.swing.undo.UndoManager;

import org.jpws.front.ActionHandler;
import org.jpws.front.MenuHandler;

/**
 * This is an extension of <code>JTextField</code> and adds the following features.
 * 
 * <p>1. Undo/Redo manager with 100 operations stack
 * <br>2. Keystroke support with these (additional) assignments: CTRL-W (select-word),
 * CTRL-L (select line), CTRL-P (select paragraph), CTRL-Z (undo), CTRL-Y (redo)  
 *  
 * @since 0-5-0
 */
public class EditorTextField extends JTextField
{
   private static HashMap<Object, Action> actionLookup;
   
   private ArrayList<EditorChangeEventListener> editorChangeListeners; 
   private EditorDocumentListener documentListener;
   private UndoManager undoManager = new UndoManager();
   private PopupListener popupListener = new PopupListener();
   private boolean isPopupActive = true;

public EditorTextField ()
{
   super();
   init();
}

public EditorTextField ( int columns )
{
   super( columns );
   init();
}

public EditorTextField ( String text )
{
   super( text );
   init();
}

public EditorTextField ( Document doc, String text, int columns )
{
   super( doc, text, columns );
   init();
}

public EditorTextField ( String text, int columns )
{
   super( text, columns );
   init();
}

private void init ()
{
   Action actionList[];
   
   if ( actionLookup == null )
   {
      //  get all the actions JTextArea provides for us
      actionList = getActions();
      // put them in a Hashtable so we can retrieve them by Action.NAME
      actionLookup = new HashMap<Object, Action>();
      for (int j=0; j < actionList.length; j+=1)
        actionLookup.put(actionList[j].getValue(Action.NAME), actionList[j]);
   }

   addMouseListener( popupListener );
   getDocument().addUndoableEditListener( undoManager );
   modifyKeystrokes ();
}

public void addChangeEventListener ( EditorChangeEventListener listener ) {
   if ( editorChangeListeners == null ) {
      editorChangeListeners = new ArrayList<EditorChangeEventListener>();
   }
   if ( documentListener == null ) {
      documentListener = new EditorDocumentListener();
      getDocument().addDocumentListener( documentListener );
   }
   
   synchronized (editorChangeListeners) {
   if ( !editorChangeListeners.contains( listener ) ) {
      editorChangeListeners.add( listener );
   }}
}

public void removeChangeEventListener ( EditorChangeEventListener listener ) {
   if ( editorChangeListeners == null | documentListener == null ) {
      return;
   }
   synchronized (editorChangeListeners) {
      editorChangeListeners.remove( listener );
      if ( editorChangeListeners.size() == 0 ) {
         getDocument().removeDocumentListener( documentListener );
         documentListener = null;
      }
   }
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
      
   // activate keymap for this text area
   setKeymap( map );
}  // modifyKeystrokes

/** Removes all entries from the undo-manager. */
public void clearUndoList ()
{
   undoManager.discardAllEdits();
}

//  *****************  inner classes  ****************

/**
 * Renders a popup menu for the context of this text field.
 * 
 * @return <code>JPopupMenu</code>
 */
protected JPopupMenu getPopupMenu ()
{
   JPopupMenu menu;
   JMenuItem item;
   Action action;
   URL url;
   String hstr, text;
   
   menu = new JPopupMenu();
   requestFocus();

   
   if ( undoManager.canUndo() )
   {
      item = new JMenuItem( new UndoAction() );
      item.setAccelerator( KeyStroke.getKeyStroke(
            KeyEvent.VK_Z, ActionEvent.CTRL_MASK) );
      menu.add( item );
   }
   
   if ( undoManager.canRedo() )
   {
      item = new JMenuItem( new RedoAction() );
      item.setAccelerator( KeyStroke.getKeyStroke(
            KeyEvent.VK_Y, ActionEvent.CTRL_MASK) );
      menu.add( item );
   }

   if ( undoManager.canUndo() || undoManager.canRedo() )
      menu.addSeparator();

   // the standard CUT action (clipboard)
   item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.cut" ) );
   action = new AbstractAction()
   {
      public void actionPerformed ( ActionEvent e )
      {
         try
         {
            Dimension adr = getOperationSelection();
            int pos = adr.width, len = adr.height-adr.width;
            String hstr = getText( pos, len );
            ActionHandler.sendClipboardText( null, hstr, null );
            EditorTextField.this.getDocument().remove( pos, len ); 
         }
         catch ( BadLocationException e1 )
         { e1.printStackTrace(); }
      }
   };
   item.addActionListener( action );
   menu.add( item );
   
   // the standard COPY action (clipboard)
   item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.copy" ) );
   action = new AbstractAction()
   {
      public void actionPerformed ( ActionEvent e )
      {
         try
         {
            Dimension adr = getOperationSelection();
            String hstr = getText( adr.width, adr.height-adr.width );
            ActionHandler.sendClipboardText( null, hstr, null );
         }
         catch ( BadLocationException e1 )
         { e1.printStackTrace(); }
      }
   };
   item.addActionListener( action );
   menu.add( item );
   
   // the standard PASTE action (clipboard)
   item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.paste" ) );
   action = actionLookup.get( DefaultEditorKit.pasteAction );
   item.addActionListener( action );
   menu.add( item );

   // erases a text selection if present, otherwise the entire field 
   item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.erase" ) );
   action = new AbstractAction()
   {
      public void actionPerformed ( ActionEvent e )
      {
         try
         {
            Dimension adr = getOperationSelection();
            EditorTextField.this.getDocument().remove( adr.width, adr.height-adr.width ); 
         }
         catch ( BadLocationException e1 )
         { e1.printStackTrace(); }
      }
   };
   item.addActionListener( action );
   menu.add( item );

   menu.addSeparator();

   // investigate current text selection or entire text line
   if ( ((hstr = getSelectedText()) != null || (hstr = getText()) != null) )
   {
      // add browsing command if an url is contained 
      if ( (url = Util.extractURL( hstr )) != null ) 
      {
         item = menu.add( ActionHandler.getStartBrowserAction( url, false ) );
         item.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
      }
      // add mailto command if an email address is contained 
      if ( (text = Util.extractMailaddress( hstr )) != null )
      {
         item = menu.add( ActionHandler.getStartEmailAction( text, false ) );
         item.setForeground( MenuHandler.MENUITEM_MARKED_COLOR );
      }
   }

   item = new JMenuItem( ResourceLoader.getCommand( "menu.edit.selectall" ) );
   action = actionLookup.get( DefaultEditorKit.selectAllAction );
   item.addActionListener( action );
   menu.add( item );
   
   return menu;
}

/**
 * Whether popup menu feature is active in this text area. 
 * @return boolean 
 */
public boolean getPopupActive ()
{
   return isPopupActive;
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

private class UndoAction extends TextAction
{

   public UndoAction ()
   {
      super( "Undo" );
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
      super( "Redo" );
   }

   public void actionPerformed ( ActionEvent e )
   {
      if ( undoManager.canRedo() )
         undoManager.redo();
   }
}

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


private class EditorDocumentListener implements DocumentListener       
{

   private void fireEditorEvent ( DocumentEvent e ) {
      // fire our own event
      if ( editorChangeListeners == null ) {
         return;
      }
      synchronized (editorChangeListeners) {
         EditorChangeEvent event = new EditorChangeEvent(e);
         for(EditorChangeEventListener li : editorChangeListeners) {
            li.documentChanged( event );
         }
      }
   }

   public void changedUpdate ( DocumentEvent e ) {
      fireEditorEvent( e );
   }
   
   public void insertUpdate ( DocumentEvent e ) {
      fireEditorEvent( e );
   }
   
   public void removeUpdate ( DocumentEvent e ) {
      fireEditorEvent( e );
   }
}

}
