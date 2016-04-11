/*
 *  PWDB_RecordHandle in org.jpws.data
 *  file: PWDB_RecordHandle.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 11.06.2005
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

/**
 *   Not used in this release.
 */
public interface PWDB_RecordHandle
{

   int getID ();
   
   String getString ( String field );
   Object getSecretField ( String field );
   int getInt ( String field );
   long getLong ( String field );

   void setString ( String field, String value );
   void setSecretField ( String field, Object value );
   void setInt ( String field, int value );
   void setLong ( String field, long value );
   
   void unlock ();
}
