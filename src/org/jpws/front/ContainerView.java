/*
 *  ContainerView in org.jpws.front
 *  file: ContainerView.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 30.09.2004
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

import java.awt.Font;

import javax.swing.JComponent;

import org.jpws.front.util.ActivitySource;
import org.jpws.pwslib.order.DefaultRecordWrapper;

/**
 * Interface to handle general aspects of viewports to a database container's
 * record list. This renders a displayable GUI component and deals with record
 * selections. 
 */
public interface ContainerView extends ActivitySource 
{ 

   /**
    * Returns the first item of the current user selection in this view as index
    * in the <code>OrderedRecordList</code> that forms the data reference of 
    * this view handler.
    * 
    * @return sort index value or -1 of no item is selected
    */
   public int getFirstSelected ();
   
   /**
    * Returns all items of the current user selection in this view as an array
    * of index values into the <code>OrderedRecordList</code> that forms the data
    * reference of this view handler.
    * 
    * @return array of int; empty array if the selection is empty 
    */
   public int[] getSelectedItems ();
   
   /** Whether there exists a meaningful user selection in this view's display.
    *  (Note that this is not equivalent with <code>getFirstSelected() != -1</code>
    *  as it refers to *any* meaningful selections within implementations whose 
    *  nature is unknown to this interface.)
    */
   public boolean hasUserSelection ();
   
   /**
    * Returns all items of the current user selection in this view as an array
    * of <code>DefaultRecordWrapper</code>s. This may serve as an absolute reference 
    * to the records of a list selection (instead of the relative of index
    * positions).
    * 
    * @return array of <code>DefaultRecordWrapper</code>; 
    *         empty array if the selection is empty 
    */
   public DefaultRecordWrapper[] getSelectedWrappers ();
   
   /**
    * Sets the record item selected that belongs to the specified sort index
    * in the <code>OrderedRecordList</code> that forms the data reference of this 
    * view handler.
    * 
    * @param index of the item to be selected; -1 leads to empty selection (deselection)
    */
   public void setSelectedIndex ( int index );

   /**
    * Sets an array of items as the current user selection in the view.
    * With <code>selection</code> == <b>null</b> this works as deselection.
    * 
    * @param selection array of item indices in <code>OrderedRecordList</code>
    *        or <b>null</b>
    *        
    */
   public void setSelectedItems ( int[] selection );
   
   /**
    * Sets an array of <code>DefaultRecordWrapper</code> items as the current 
    * user selection in the view. Records that are not elements of the current
    * view list are disregarded.
    * 
    * @param selection array of <code>DefaultRecordWrapper</code>
    */
   public void setSelectedWrappers ( DefaultRecordWrapper[] selection );
   
   /** 
    * Set this view to select all available items.
    *
    */
   public void setSelectAll ();
   
   /**
    * Attempts to arrange the list display to show the item specified by its
    * index.
    * 
    * @param index of the item to be scrolled to
    */
   public void scrollToVisible ( int index );
   
   /**
    * Returns the visible UI-component that displays the results of this view handler.
    * 
    * @return <code>Component</code>
    */
   public JComponent getView ();
   
   public void repaint ();
   
   /** Sets the display font. */
   public void setFont ( Font font );
   
   public void reconstruct ();

//   /** Grabs the focus to this component if possible.
//    */
//   public void grabFocus();
   
}
