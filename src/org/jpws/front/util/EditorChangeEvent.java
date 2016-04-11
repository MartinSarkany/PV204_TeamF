/*
 *  EditorChangeEvent in org.jpws.front.util
 *  file: EditorChangeEvent.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 04.01.2012
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

package org.jpws.front.util;

import javax.swing.event.DocumentEvent;

public class EditorChangeEvent 
{
   /** Type of change in the editor's document (CHANGE, INSERT, REMOVE). */
   public enum EditorChangeType {CHANGE, INSERT, REMOVE};
   
   private DocumentEvent docEvt;
   private EditorChangeEvent.EditorChangeType type;
   
   public EditorChangeEvent ( DocumentEvent evt) {
      if ( evt == null ) {
         throw new NullPointerException();
      }
      docEvt = evt;
      DocumentEvent.EventType docET = docEvt.getType(); 
      type = docET == DocumentEvent.EventType.INSERT ? EditorChangeType.INSERT :
             docET == DocumentEvent.EventType.REMOVE ? EditorChangeType.REMOVE :
             docET == DocumentEvent.EventType.CHANGE ? EditorChangeType.CHANGE : null;
   }
   
   public DocumentEvent getDocumentEvent () {
      return docEvt;
   }

   /** Returns the type of change that occurred in the editor's document
    * or <b>null</b>.
    * @return
    */
   public EditorChangeEvent.EditorChangeType getType () {
      return type;
   }
}