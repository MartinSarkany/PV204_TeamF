/*
 *  ShowDialogRunnable in org.jpws.front.util
 *  file: ShowDialogRunnable.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 29.03.2007
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

import java.awt.Dialog;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jpws.front.ActionHandler;
import org.jpws.front.GUIService;
import org.jpws.pwslib.global.Log;

/**
 * 
 * @since 0-5-0
 */
public class ShowDialogRunnable implements Runnable
{
   Dialog child;
   
   public ShowDialogRunnable ( Dialog child )
   {
      this.child = child;
   }

   @SuppressWarnings("deprecation")
   public void run ()
   {
      if ( !child.isShowing() )
      {
         Log.log( 8, "(ShowDialogRunnable.run) *** Dialog Children SHOW OPERATION : " + child.getTitle() );
         child.show();
      }
   }
   
   /**
    * Evaluates a list of dialogs and makes them show via <code>SwingUtilities.invokeLater()</code>
    * for all elements that are not showing.
    * 
    * @param dialogs list of element type <code>Dialog</code>
    */
   public static void startDialogsLater ( List<Dialog> dialogs )
   {
      // first show all non-modal dialogs
      for ( Dialog dlg : dialogs )
      {
         if ( !(dlg.isModal() | dlg.isShowing()) )
         {
        	 ActionHandler.executeOnEDT( new ShowDialogRunnable( dlg ) );
         }
      }

      // second show all modal dialogs
      for ( Dialog dlg : dialogs )
      {
         if ( dlg.isModal() & !dlg.isShowing() )
         {
            SwingUtilities.invokeLater( new ShowDialogRunnable( dlg ) );
         }
      }
   }
}

