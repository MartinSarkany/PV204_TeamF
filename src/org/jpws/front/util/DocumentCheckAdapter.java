/*
 *  DocumentCheckAdapter in org.jpws.front.util
 *  file: DocumentCheckAdapter.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 03.11.2007
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

package org.jpws.front.util;

import java.util.concurrent.Future;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jpws.front.ActionHandler;



public class DocumentCheckAdapter implements DocumentListener
{
   private Runnable runner;
   private Future<?> thread;
   private long delay;
   private boolean listening;

   /**
    * Creates a new adapter to start an action (Runnable)
    * in dependence of a wait time after document modification.
    * 
    * @param r <code>Runnable</code> action performed
    * @param delay long wait time in milliseconds
    */
public DocumentCheckAdapter ( Runnable r, long delay )
{
   if ( r == null )
      throw new NullPointerException();
   
   runner = r;
   this.delay = delay;
   listening = true;
}

public void changedUpdate ( DocumentEvent e )
{
}

public void insertUpdate ( DocumentEvent e )
{
   performed(e);
}

public void removeUpdate ( DocumentEvent e )
{
   performed(e);
}

private void performed ( DocumentEvent e )
{
   if ( !listening ) return; 
   
   // cancel a previous action schedule
   if ( thread != null ) {
      thread.cancel(false);
      thread = null;
   }
   
   // setup new action schedule
   thread = ActionHandler.startTaskDelayed( runner, delay );
}

/** Sets whether this adapter actually listens to document events.
 * 
 * @param v boolean true == adapter listens
 */
public void setListening ( boolean v ) 
{
    listening = v;
}

}
