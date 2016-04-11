/*
 *  AutomoveDialog in org.jpws.front.util
 *  file: AutomoveDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 25.11.2005
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

package org.jpws.front.util;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JDialog;

/** Substitute class for <code>JDialog</code> which creates a dialog window
 *  that automatically moves together with its parent component.
 *  
 *  <p><u>Note to code snifflers:</u> According to GPL regulations you should give
 *  a fair acknowledgement of the source authors and/or project when using
 *  code for your own, GPL entitled purposes that constitutes copyrights.
 *  
 *  @author Wolfgang Keller, n.n. (Sarath)
 *  @since 0-3-0
 */

public class AutomoveDialog extends JDialog
{
   /** Listener to parent frame Component events. */
   private AutomoveAdapter    moveAdapter;
   

public AutomoveDialog () throws HeadlessException
{
   super();
   init();
}

public AutomoveDialog ( Frame owner ) throws HeadlessException
{
   super( owner );
   init();
}

public AutomoveDialog ( Frame owner, boolean modal ) throws HeadlessException
{
   super( owner, modal );
   init();
}

public AutomoveDialog ( Frame owner, String title ) throws HeadlessException
{
   super( owner, title );
   init();
}

public AutomoveDialog ( Frame owner, String title, boolean modal )
      throws HeadlessException
{
   super( owner, title, modal );
   init();
}

public AutomoveDialog ( Frame owner, String title, boolean modal,
      GraphicsConfiguration gc )
{
   super( owner, title, modal, gc );
   init();
}

public AutomoveDialog ( Dialog owner ) throws HeadlessException
{
   super( owner );
   init();
}

public AutomoveDialog ( Dialog owner, boolean modal ) throws HeadlessException
{
   super( owner, modal );
   init();
}

public AutomoveDialog ( Dialog owner, String title ) throws HeadlessException
{
   super( owner, title );
   init();
}

public AutomoveDialog ( Dialog owner, String title, boolean modal )
      throws HeadlessException
{
   super( owner, title, modal );
   init();
}

public AutomoveDialog ( Dialog owner, String title, boolean modal,
      GraphicsConfiguration gc ) throws HeadlessException
{
   super( owner, title, modal, gc );
   init();
}


private void init ()
{
   Component frame;
   
   frame = this.getParent();
   if ( frame != null )
   {
      moveAdapter = new AutomoveAdapter( frame, this );
   }
}  // init

public void dispose ()
{
   if ( moveAdapter != null )
      moveAdapter.release();

   super.dispose();
}

}
