/*
 *  RecentList in org.jpws.front
 *  file: RecentList.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 10.09.2004
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
package org.jpws.front.util;

import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jpws.front.ActionHandler;

/**
 *  Utility class to serve a recent file list for menus.
 *  <p>The RecentList's maxvalue has a minimum of 0 and a default of 8.
 * 
 *  @author Wolfgang Keller
 */
public class RecentList extends AbstractListModel 
                        implements ComboBoxModel, MenuListener, Cloneable, Iterable
{
   private static final long serialVersionUID = 5519260209419347928L;
   
   protected ArrayList<Object>     vlist = new ArrayList<Object>();
   protected ActionListener actionListener = ActionHandler.getMainActionListener();
   protected JMenuItem     menuFinalItem;
   protected MenuItem      menuFinalItem2;
   private   int           maxEntries;
   protected int           maxDisplay;
   private   String        command;
   private   Object        selected;
   private boolean         modified;       
   
/**
 * Creates an empty RecentList with max entries of 8.
 */
public RecentList ()
{
   maxEntries = 8;
   maxDisplay = 8;
}

/**
 * Creates an empty RecentList with the specified max entries value.
 * @param max maximum number of entries in this list
 */
public RecentList ( int max )
{
   maxEntries = max;
   maxDisplay = max;
}

/**
 * Creates an empty RecentList with the specified menu-item command socket.
 * Max entries of 8.
 * 
 * @param command the command socket which is used to address global ActionListener
 */
public RecentList ( String command )
{
	this( command, 8, 8 );
}

/**
 * Creates an empty RecentList with the specified menu-item command socket and
 * max entries.
 * 
 * @param command String the command socket which is used to address global 
 *                ActionListener, may be null
 * @param max int maximum number of entries in this list
 */
public RecentList ( String command, int max )
{
	this( command, max, max );
}

/**
 * Creates an empty RecentList with the specified menu-item command socket, 
 * max list entries and max entries in the menu.
 * 
 * @param command the command socket which is used to address global ActionListener
 * @param max maximum number of entries in this list
 * @param displayed maximum number of entries which are taken into menu display
 * @since 0-5-0
 */
public RecentList ( String command, int max, int displayed )
{
   setCommand( command );
   maxEntries = max;
   maxDisplay = displayed;
}

/** Returns a shallow clone of this RecentList. */
@Override
@SuppressWarnings("unchecked")
public Object clone ()
{
   RecentList obj = null;
   
   try { obj = (RecentList)super.clone(); }
   catch ( Exception e )
   { e.printStackTrace(); }
   
   obj.vlist = (ArrayList<Object>)vlist.clone();
   return obj;
}

/** Sets the content of this RecentList through a semicolon separated list
 *  of text values. The values may be encoded according to CSV rules (rfc-4180).
 *  
 *  @param list sequence of semicolon separated text values (CSV encoded)
 */
public void setContent ( String list )
{
   synchronized ( vlist ) {
	  // clear and populate list 
      vlist.clear();
      String [] arr = Util.CSV.decodeLine( list, 0, ';' );
      for ( int i = 0; i < arr.length & i < maxEntries; i++ ) {
    	 String entry = arr[i];
         if ( !entry.equals("") ) {
            vlist.add( entry );
         }
      }
   }
   // notify
   fireContentsChanged( this, 0, getSize()-1 );
}  // setContent

/** Sets the content of this RecentList through an array of <code>Object</code>
 *  values. 
 *  
 *  @param objs object array
 */
public void setContent ( Object[] objs )
{
   if ( objs == null )
	   throw new NullPointerException("parameter is null");
	
   synchronized ( vlist ) {
	  // clear and populate list 
      vlist.clear();
      for ( int i = 0; i < objs.length & i < maxEntries; i++ ) {
    	 Object entry = objs[ i ];  
         if ( entry != null ) {
            vlist.add( entry );
         }
      }
   }
   // notify
   fireContentsChanged( this, 0, getSize()-1 );
}  // setContent

public void setCommand ( String cmd )
{
   this.command = cmd;
}

public String getCommand () 
{
	return command;
}

public void setActionListener ( ActionListener a )
{
   this.actionListener = a;
}

/** 
 * If not <b>null</b> the menu item specified here is shown
 * as last element, separated by a separator line, when this
 * recent list is used as a menu generator.
 * 
 * @param item <code>JMenuItem</code>
 * @since 0-5-0
 */
public void setMenuFinalItem ( JMenuItem item )
{
   menuFinalItem = item;
}

/** 
 * If not <b>null</b> the menu item specified here is shown
 * as last element, separated by a separator line, when this
 * recent list is used as a menu generator.
 * 
 * @param item <code>JMenuItem</code>
 * @since 0-5-0
 */
public void setMenuFinalItem ( MenuItem item )
{
   menuFinalItem2 = item;
}

/** Returns a String value representing the content of this RecentList
 *  as a semicolon separated concatenation of "toString()" values of the 
 *  listed objects. This value may be used for persistent storage and is
 *  encoded according to rfc-4180.
 *  
 *  @return String content value
 */ 
public String getStringContent ()
{
   return Util.CSV.encodeLine( toStringArray(), ';' );
}

/** Returns an array of objects containing all listed values of this RecentList.
 *  
 *  @return <code>Object[]</code>
 */ 
public Object[] getContent ()
{
   return vlist.toArray();
}

/** Returns a <code>String</code> array representing all currently stored values
 *  of this RecentList in the order most recent to least recent. (The result utilizes
 *  the <code>toString()</code> property of the listed objects.)
 *   
 *  @return <code>String[]</code>
 */ 
public String[] toStringArray ()
{
   String[] arr;
   int i, size;

   synchronized ( vlist )
   {
      size = getSize();
      arr = new String[ size ];
      for ( i = 0; i < size; i++ )
         arr[ i ] = vlist.get( i ).toString();
   }
   return arr;
}

/** Updates the given <code>JMenu</code> with a list of all stored objects
 *  of this RecentList. The <code>toString()</code> property of objects is used
 *  to represent them.
 *  
 * @param menu
 */ 
@SuppressWarnings("unchecked")
public void updateMenu ( JMenu menu )
{
   JMenuItem item;
   Iterator<Object> it;
   Object obj;
   String hstr;
   int count;

   synchronized ( vlist )
   {
      it = (Iterator<Object>) iterator();
      menu.removeAll();
      count = 0;
      while ( it.hasNext() && count < maxDisplay )
      {
         if ( (obj = it.next()) != null &&
              !(hstr = obj.toString()).equals( "" ) )
         {
            item = new JMenuItem( hstr );
            item.setActionCommand( command.concat( hstr ) );
            item.addActionListener( actionListener );
            menu.add( item );
            count++;
         }
      }
      
      // optional terminating element
      if ( menuFinalItem != null && vlist.size() != 0 )
      {
         menu.addSeparator();
         menu.add( menuFinalItem );
      }
   }
}  // updateMenu

/** Updates the given <code>JMenu</code> with a list of all stored objects
 *  of this RecentList. The <code>toString()</code> property of objects is used
 *  to represent them.
 *  
 * @param menu
 */ 
@SuppressWarnings("unchecked")
public void updateMenu ( Menu menu )
{
   MenuItem item;
   Iterator<Object> it;
   Object obj;
   String hstr;
   int count;

   synchronized ( vlist )
   {
      it = (Iterator<Object>) iterator();
      menu.removeAll();
      count = 0;
      while ( it.hasNext() && count < maxDisplay )
      {
         if ( (obj = it.next()) != null &&
              !(hstr = obj.toString()).equals( "" ) )
         {
            item = new MenuItem( hstr );
            item.setActionCommand( command.concat( hstr ) );
            item.addActionListener( actionListener );
            menu.add( item );
            count++;
         }
      }
      
      // optional terminating element
      if ( menuFinalItem2 != null && vlist.size() != 0 )
      {
         menu.addSeparator();
         menu.add( menuFinalItem2 );
      }
   }
}  // updateMenu

/** Erases all content from this list. */
public void clear ()
{
   int size;
   synchronized ( vlist ) {
      size = vlist.size();
      vlist.clear(); 
      modified = modified | size != 0;
   }

   // notify
   if ( size != 0 ) {
	   fireContentsChanged( this, 0, size-1 );
   }
}

/** Sets the maximum enries this list can contain. */
public void setMaxEntries ( int value )
{
   maxEntries = Math.max( 0, value );
   int size, newSize, pos;
   
   synchronized ( vlist ) {
	  size = vlist.size(); 
      while ( (pos=vlist.size()) > maxEntries ) {
         int i = pos - 1;
         vlist.remove( i );
         modified = true;
      }
      newSize = vlist.size();
   }
   
   // notify
   if ( size != newSize ) {
       fireIntervalRemoved( this, newSize, size-1 );
   }
}  // setMaxEntries

/** Removes the specified object from this RecentList. 
 * 
 *  @param value <code>Object</code> to be removed from list, may be null
 *  @return <b>true</b> if the value was contained, <b>false</b> otherwise
 */
public boolean removeRecent ( Object value )
{
   if ( value == null ) return false;
   int index;
   
   // eliminate entry from vlist
   synchronized ( vlist ) {
	  index = vlist.indexOf( value ); 
      if ( index != -1 ) {
         vlist.remove( index );
         modified = true;
      }
   }
   
   // notify and return
   if ( index != -1 ) {
	   fireIntervalRemoved( this, index, index );
	   return true;
   }
   return false;
}

/** Returns the first entry in this RecentList or <b>null</b> if empty. */
public Object getFirst ()
{
   return vlist.size() == 0 ? null : vlist.get(0);
}

/** Returns the last entry in this RecentList or <b>null</b> if empty. */
public Object getLast ()
{
   return vlist.size() == 0 ? null : vlist.get( vlist.size()-1 );
}

/** Returns the text value of the object at position index or <b>null</b> 
 *  if index is out of range. The text value is derived from the stored 
 *  object's <code>toString()</code> method.
 *  
 *  @param index entry position in list
 *  @return entry text value 
 */
public String getStringValue ( int index )
{
   if ( index < 0 | index > getSize()-1 )
      return null;
   return vlist.get( index ).toString();
}

/** Inserts or re-arranges the specified object in this RecentList.
 * 
 *  @param value text value to be inserted/updated
 *  @return boolean <b>true</b> if and only if the content of recent list 
 *          has been modified by this operation
 */
public boolean pushRecent ( Object value )
{
   boolean acted = false;
   
   synchronized ( vlist ) {
      if ( !value.equals( getFirst() ) ) {
    	 acted = true; 
         removeRecent( value );
         vlist.add( 0, value );
         modified = true;
      }
   }
   // notify
   if ( acted ) {
       fireIntervalAdded( this, 0, 0 );
       return true;
   }
   return false;
}  // pushRecent

/**
 * Replaces an entry in this recent list so that the new object is
 * in the same list position as the old. Does nothing if <code>
 * oldObj</code> is not present in the list.
 * 
 * @param oldObj existing object (as per equals()) to be removed
 * @param newObj new object to be inserted
 * @since 0-5-0
 */
public void replaceRecent ( Object oldObj, Object newObj )
{
   int i;	
   synchronized ( vlist ) {	
	   i = vlist.lastIndexOf( oldObj );
	   if ( i != -1 ) {
	      vlist.set( i, newObj );
	   }
   }
   // notify
   if ( i != -1 ) {
      fireContentsChanged( this, i, i );
   }
}

/** Returns an iterator over all entry objects in this RecentList
 *  in proper sequence. */ 
@Override
public Iterator<?> iterator ()
{
   return vlist.iterator();
}

public boolean isModified ()
{
   return modified;
}

public void resetModified ()
{
   modified = false;
}

/**
 * Whether a given object is an element of this recent list.
 * The <code>equals()</code> method is used to test identify.
 *  
 * @param obj Object to be tested
 *  @return boolean <b>true</b> if and only if the parameter object equals an
 *          element of this recent list
 *  @since 0-6-0        
 */
public boolean hasRecent ( Object obj )
{
   for ( Iterator<?> it = iterator(); it.hasNext(); ) {
      if ( it.next().equals( obj ) )
         return true;
   }
   return false;
}

//**********  IMPLEMENTATION OF THE ListModel INTERFACE *********

@Override
public Object getElementAt ( int index )
{
   if ( index < 0 | index > getSize()-1 )
      return null;
   return vlist.get( index );
}

/** The number of entries in this list. */
@Override
public int getSize ()
{
   return vlist.size();
}

/** The number of maximum entries in this list. */
public int getMaxEntries ()
{
   return maxEntries;
}

//**********  IMPLEMENTATION OF THE ComboBoxModel INTERFACE *********

/* 
 * Overridden: @see javax.swing.ComboBoxModel#getSelectedItem()
 */
@Override
public Object getSelectedItem ()
{
   return selected;
}

/* 
 * Overridden: @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
 */
@Override
public void setSelectedItem ( Object item )
{
   selected = item;
   pushRecent( item );
}

//**********  IMPLEMENTATION OF THE MenuListener INTERFACE *********

@Override
public void menuCanceled ( MenuEvent arg0 )
{
}

@Override
public void menuDeselected ( MenuEvent arg0 )
{
}

@Override
public void menuSelected ( MenuEvent evt )
{
   if ( evt.getSource() instanceof JMenu ) {
      JMenu menu = (JMenu)evt.getSource();
      updateMenu( menu );
   }
}

}
