/*
 *  PersistentOptions in org.jpws.data
 *  file: PersistentOptions.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 26.11.2005
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

package org.jpws.data;

import java.awt.Rectangle;
import java.util.List;

public interface PersistentOptions
{

   /** Returns the mapped option integer value or 0 if the option is
    *  undefined.
    * @param token the option name
    * @return int
    */
   public int getIntOption ( String token );

   /** Returns the mapped option long integer value or 0 if the option is
    *  undefined.
    * @param token the option name
    * @return long
    */
   public long getLongOption ( String token );
   
   /** Returns the mapped option string value or empty string if the option is
    *  undefined.
    * 
    * @param token the option name
    * @return option value string or "" if undefined
    */
   public String getOption ( String token );
   
   /** Returns the mapped bounds object (e.g. for a window) or 
    * <b>null</b> if this option is undefined.
    *  
    * @param token the option name
    * @return <code>Rectangle</code> specifying position and size of a window
    * @since 0-5-0
    */
   public Rectangle getBounds ( String token );

   /** Whether the specified (boolean) option is set to "true". */
   public boolean isOptionSet ( String token );
   
   /** Sets an integer value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned int value
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   public boolean setIntOption ( String token, int value );

   /** Sets a long integer value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned long value
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   public boolean setLongOption ( String token, long value );

   /** Sets a boolean value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned boolean value
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   public boolean setOption ( String token, boolean value );

   /** Sets a string value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned string value, may be <b>null</b> to clear
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   public boolean setOption ( String token, String value );
   
   /** Sets a bounds object (e.g. from a window or dialog) to be
    * associated with the token string.
    * 
    * @param token the option name
    * @param bounds <code>Rectangle</code>; may be <b>null</b> to clear
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    * @since 0-5-0
    */
   public boolean setBounds ( String token, Rectangle bounds );
   
   /**
    * Stores a list of string representations of objects
    * in options. The <code>toString()</code> method is used on each 
    * object to derive its representation value. (NOTE:
    * this is not necessarily a serialization of objects themselves!)    
    * 
    * @param name String name of the list to store
    * @param ls List list of objects, use <b>null</b> to clear
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    * @since 0-6-0
    */
   public boolean setStringList ( String name, List<Object> ls );
   
   /**
    * Retrieves a list of strings that has been previously stored
    * into options by <code>setStringList()</code>. If no value was
    * found for <b>name</b>, an empty list is returned.
    * 
    * @param name String name of the list
    * @return List a list of retrieved String values (may be empty)
    * @since 0-6-0
    */
   public List<String> getStringList ( String name );

   /** Returns the STORETIME value from this option set's METADATA.
    * 
    * @return long time in milliseconds or 0 if undefined
    */
   public long getStoreTime();

   /** Whether there is no assignment in this option bag.
    * 
    * @return boolean true == empty bag
    */
   boolean isEmpty();

   
}
