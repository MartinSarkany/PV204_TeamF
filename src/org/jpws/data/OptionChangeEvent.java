/*
 *  OptionChangeEvent in org.jpws.data
 *  file: OptionChangeEvent.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 05.11.2007
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

package org.jpws.data;

import java.util.EventObject;

/**
 * Type for a single option change event. Option change events are issued by
 * static class "Options" to listeners of type <code>OptionChangeListener</code>. 
 * 
 * @since 0-6-0
 */
public class OptionChangeEvent extends EventObject
{
   private String name;
   private String oldValue;
   private String newValue;


public OptionChangeEvent ( Object source, String option, String oldValue, String newValue )
{
   super( source );
   name = option;
   this.oldValue = oldValue;
   this.newValue = newValue;
}


public String getOptionName ()
{
   return name;
}


public String getNewValue ()
{
   return newValue;
}


public String getOldValue ()
{
   return oldValue;
}

}
