/*
 *  TooltipComboBox in org.jpws.front.util
 *  file: TooltipComboBox.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 20.02.2010
 *  Version
 * 
 *  Copyright (c) 2010 by Wolfgang Keller, Munich, Germany
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.jpws.front.PwsFileContainer;

public class TooltipComboBox extends JComboBox
{
//   private 


public TooltipComboBox ()
{
   init();
}

public TooltipComboBox ( Object[] items )
{
   super( items );
   init();
}

public TooltipComboBox ( Vector<?> items )
{
   super( items );
   init();
}

public TooltipComboBox ( ComboBoxModel aModel )
{
   super( aModel );
   init();
}

private void init ()
{
   ActionListener act;
   
   setRenderer( new TooltipComboBoxRenderer() );
   act = new SelectionListener();
   addActionListener( act );
   act.actionPerformed( null );
}

private class SelectionListener implements ActionListener
{

   public void actionPerformed ( ActionEvent e )
   {
      String text;
      Object item;
      
      // this installs the latest selected value's tooltip text
      // on the combo object itself
      item = TooltipComboBox.this.getSelectedItem();
      if ( item != null )
      {
         text = TooltipComboBoxRenderer.getTooltipText( item );
         TooltipComboBox.this.setToolTipText( text );
      }
   }

}

/**
 * 
 * @since 0-5-0
 */
public static class TooltipComboBoxRenderer extends BasicComboBoxRenderer
{

public Component getListCellRendererComponent ( JList list, Object value, int index, 
      boolean isSelected, boolean cellHasFocus )
{
   PwsFileContainer container;
   JLabel label;
   String tipVal; 
   
   label = (JLabel) super.getListCellRendererComponent( list, value, index, isSelected,
         cellHasFocus );
   
   if ( value instanceof PwsFileContainer )
   {
      container = (PwsFileContainer)value;
      label.setText( container.getDatabaseName() );
      tipVal = container.getFilePath();
   }
   else
   {
      tipVal = value.toString(); 
   }

   label.setToolTipText( tipVal );
   return label;
}

public static String getTooltipText ( Object item )
{
   if ( item instanceof PwsFileContainer )
      return ((PwsFileContainer)item).getFilePath();
   return item.toString();
}

}

}
