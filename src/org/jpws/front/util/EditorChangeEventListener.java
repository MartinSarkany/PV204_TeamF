/*
 *  EditorChangeEventListener in org.jpws.front.util
 *  file: EditorChangeEventListener.java
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

public interface EditorChangeEventListener {
   
   public void documentChanged ( EditorChangeEvent event );
}