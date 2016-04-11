/*
 *  ObjectChangeListener in org.jpws.front
 *  file: ObjectChangeListener.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 17.03.2012
 *  Version
 * 
 *  Copyright (c) 2012 by Wolfgang Keller, Munich, Germany
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;

import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ActivitySource;
import org.jpws.front.util.ButtonBarListener;

/**
 *  This class is at the core of object related user activity tracing in JPWS.
 *  It is Janus-headed as it implements both the <code>ActivityListener</code> and
 *  <code>ActivitySource</code> interface. It thus serves to handle and multiplex
 *  incoming activity events and reach them down to registered listeners. Its second
 *  function is to detect "user" activity from certain objects which are registered
 *  at the listener (in fact the listener is registered at the objects, but alas..).
 *  
 *  <p>Objects of the following types can be handled: 
 *  <p><code>AbstractButton</code>, <code>JTabbedPane</code>, <code>JSpinner</code>,
 *  <code>JComboBox</code>, <code>JTextComponent</code>, <code>JTree</code>,
 *  <code>JTable</code>
 *  
 *  @see ActivityListener 
 *  @see ActivitySource
 */
public class ObjectChangeListener 
   implements ChangeListener, DocumentListener, ActionListener, ButtonBarListener,
              TreeSelectionListener, TreeModelListener, ListSelectionListener,
              TableModelListener, ActivityListener, ActivitySource 
{
   private ArrayList<ActivityListener> listeners = new ArrayList<ActivityListener>();
   private long timeStamp = System.currentTimeMillis();
   private Object source;

/** Creates a new object change listener.
 *    
 * @param source Object, 
 */
public ObjectChangeListener ( Object source )
{
   this.source = source;
}
   
   

//@Override
//protected void finalize() throws Throwable {
//	System.out.println("+++ FINALIZED ObjectChangeListener of Source: "
//			+ source);
//	super.finalize();
//}



public void changedUpdate ( DocumentEvent e )
{}

public void insertUpdate ( DocumentEvent e )
{
   fireEvent(null);
}

public void removeUpdate ( DocumentEvent e )
{
   fireEvent(null);
}


public void stateChanged ( ChangeEvent e )
{
   fireEvent(e);
}

public void actionPerformed ( ActionEvent e )
{
   fireEvent(e);
}

public void actionOccurred ( ChangeEvent evt )
{
   fireEvent(evt);
}

/** Find an ancestor that implements the ActivityListener interface.
 * 
 * @param parent Component ancestor line descendant, may be null
 * @return <code>ActivityListener</code> or <b>null</b> if not found
 */
public static ActivityListener getActivityAncestor( Component parent )
{
   while ( parent != null && !(parent instanceof ActivityListener) )
      parent = parent.getParent();

   return (ActivityListener)parent;
}

/** Lets the first ancestor which is an <code>AcitivityListener</code>
 * listen to this activity source. Traverses the hierarchy of ancestors.
 * 
 * @param parent Component parent component to investigate, may be null
 * @return boolean true == ancestor was found, false == no listener added
 */
public boolean addAncestor ( Component parent )
{
   ActivityListener ancest = getActivityAncestor( parent );
   if ( ancest != null )
   {
      // let the ancestor listen to this activity source 
      addActivityListener( ancest );
      return true;
   }
   return false;
}

/** Stops the first ancestor which is an <code>AcitivityListener</code>
 * from listening to this activity source. Traverses the hierarchy of 
 * ancestors.
 * 
 * @param parent Component parent component to investigate
 * @return
 */
public boolean removeAncestor ( Component parent )
{
   ActivityListener ancest = getActivityAncestor( parent );
   if ( ancest != null )
   {
      // remove the ancestor from listening to this activity source 
      removeActivityListener( ancest );
      return true;
   }
   return false;
}

public void addActivityListener ( ActivityListener listener )
{
   if ( listener != null && !listeners.contains( listener ) )
      synchronized (listeners) 
      { listeners.add( listener ); }
}

public void removeActivityListener ( ActivityListener listener )
{
   if ( listener != null )
      synchronized (listeners) 
      { listeners.remove( listener ); }
}

@SuppressWarnings("unchecked")
protected void fireEvent ( EventObject evt )
{
   ArrayList<ActivityListener> list;
   Object src;
   ChangeEvent chEv;
   
   timeStamp = ActionHandler.resetIdleTime();
   if ( listeners.isEmpty() )
      return;
   
   src = source != null ? source : evt == null ? this : evt.getSource();
   chEv = new ChangeEvent( src );
   synchronized (listeners) 
   { list = (ArrayList<ActivityListener>)listeners.clone(); }
   for ( ActivityListener i : list )
      i.actionOccurred( chEv );
}

/** Triggers off an activity event. */
public void activity ()
{
   actionOccurred( new ChangeEvent(this) );
}

/** Time since the last activity event occurred on this listener.
 *  
 * @return long idle time in milliseconds 
 */
public long idleTime ()
{
   return System.currentTimeMillis() - timeStamp;
}

/** Add this <code>ObjectChangeListener</code> to the parameter object
 *  in order to listen to some activity occurring. See the class description
 *  for the types (classes) which work with this listener.
 *  <p>Registering does not need a counterpart. 
 */
public void registerChangeableObject( Object c )
{
   if ( c instanceof JTabbedPane )
      ((JTabbedPane)c).addChangeListener( this );
   else if ( c instanceof AbstractButton )
      ((AbstractButton)c).addChangeListener( this );
   else if ( c instanceof JSpinner )
      ((JSpinner)c).addChangeListener( this );
   else if ( c instanceof JComboBox )
      ((JComboBox)c).addActionListener( this );
   else if ( c instanceof JTextComponent )
      ((JTextComponent)c).getDocument().addDocumentListener( this );
   else if ( c instanceof JTree )
   {
      ((JTree)c).getSelectionModel().addTreeSelectionListener( this );
      ((JTree)c).getModel().addTreeModelListener( this );
   }
   else if ( c instanceof JTable )
   {
      ((JTable)c).getSelectionModel().addListSelectionListener( this );
      ((JTable)c).getModel().addTableModelListener( this );
   }
}


public void valueChanged ( TreeSelectionEvent e )
{
   fireEvent(e);
}

public void treeNodesChanged ( TreeModelEvent evt )
{
   fireEvent(evt);
}


public void treeNodesInserted ( TreeModelEvent evt )
{
   fireEvent(evt);
}


public void treeNodesRemoved ( TreeModelEvent evt )
{
   fireEvent(evt);
}


public void treeStructureChanged ( TreeModelEvent evt )
{
   fireEvent(evt);
}


public void valueChanged ( ListSelectionEvent evt )
{
   fireEvent(evt);
}


public void tableChanged ( TableModelEvent evt )
{
   fireEvent(evt);
}

// implements ButtonBarListener

@Override
public boolean okButtonPerformed() {
	fireEvent(null);
	return true;
}

@Override
public void cancelButtonPerformed() {
	fireEvent(null);
}

@Override
public void noButtonPerformed() {
	fireEvent(null);
}

@Override
public void helpButtonPerformed() {
	fireEvent(null);
}

@Override
public boolean extraButtonPerformed(Object button) {
	fireEvent(null);
	return true;
}

} 