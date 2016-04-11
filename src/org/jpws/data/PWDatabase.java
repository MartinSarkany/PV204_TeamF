/*
 *  PWDatabase in org.jpws.data
 *  file: PWDatabase.java
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

import java.net.URL;
import java.util.Iterator;

import org.jpws.pwslib.exception.NoSuchRecordException;

/**
 *   Not used in this release.
 */
public interface PWDatabase
{

   /** Retrieves a record from this database and returns a handle for read 
    *  and write operations. The requested record must exist prior to the call.
    *  This function performs record locking on the database according to the
    *  IO modus requested and will notify the caller about impossible access 
    *  through exception thrown.  
    * 
    * @param recID a valid record number
    * @param modus specifies the IO access modus that will be enabled by the
    *        handle. Values are: "r" for read-only access, "rw" for read/write
    *        access. 
    * 
    * @throws NoSuchRecordException
    * @throws RecordInUseException
    * @throws IllegalArgumentException  
    */
   PWDB_RecordHandle getRecord ( int recID, String modus );

   PWDB_RecordHandle newRecord ();
   
   void deleteRecord ( int recID );
   

   /** Returns an <code>Iterator</code> over all records in this database.
    *  The returned values are record identifiers in the form of <code>Integer</code>
    *  objects.
    * 
    * @return iterator on element type <code>Integer</code>
    */ 
   Iterator<Integer> iterator ();

   /** The number of records in this database. This value is identical with the
    *  cardinality of the set of records that is returned by the <code>iterator()
    *  </code>. 
    * */ 
   int size ();

   // **********  DATABASE HANDLING  ***************
   
   /** Closes this database and makes any further IO access impossible.
    *  The persistent file should be released from any file lockings.  
    */  
   void close ();

   /** Attempts to remove the persistent file of this database. By convention 
    *  this will only work if this database is closed.
    * 
    *  @return <b>true</b> if and only if the file does not exist after finish
    *          of this routine
   */  
   boolean remove ();
   
   /** Writes any pending changes to the database and attempts to create a 
    *  consistent persistent file when the routine finishes. 
    */
   void flush ();

   /** Removes all records from this database */
   void clear ();
   
   /** Attempts to rename the filename of this database. Does not work for path
    *  relocations.
    * 
    * @return <b>true</b> if and only if the rename was successful  
    */ 
   boolean rename ( String filename );
   
   /** Creates a copy of this database at the given location.
    * 
    * @param url
    */
   void copyTo ( URL url );
   
   /** Returns property settings for this database. This may be used to
    *  determine supported features from a canon of database features.
    *  For boolean type properties the values "true" or "false" are returned.
    * 
    * @param name
    * @return the property expression active for the given name or <b>null</b>
    *         if the property is unknown
    */
   String getProperty ( String name );

   /** Sets the value of a modifiable property of this databse.
    * 
    * @param name name of the property to be set
    * @param value value to be assigned to the property
    */
   void setProperty ( String name, String value );
   
   /** Returns <b>true</b> on a given value x if <code>getProperty(x)</code>
    *  returns "true" and <code>false</code> in any other case.
    * 
    * @param name property to be tested
    * @return whether the given property is active in this database 
    */ 
   boolean isActiveFeature ( String name );
   
}
