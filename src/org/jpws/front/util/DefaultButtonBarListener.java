/*
 *  DefaultButtonBarListener in org.jpws.front.util
 *  file: DefaultButtonBarListener.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 26.10.2006
 *  Version
 * 
 *  Copyright (c) 2006 by Wolfgang Keller, Munich, Germany
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

import org.jpws.front.ExportCSVDialog;
import org.jpws.front.GUIService;

public class DefaultButtonBarListener implements ButtonBarListener
{
   private ButtonBarDialog dialog;
   private DialogButtonBar buttonBar;
   private String          helpCode;

public DefaultButtonBarListener ()
{
}

/**
 * 
 * @param buttonBar DialogButtonBar
 * since 0-5-0
 */
public DefaultButtonBarListener ( DialogButtonBar buttonBar )
{
   this.buttonBar = buttonBar;
}

/**
 * Creates a bar-listener with reference to a dialog's button bar.
 * 
 * @param dialog <code>ButtonBarDialog</code>
 * since 0-5-0
 */
public DefaultButtonBarListener ( ButtonBarDialog dialog )
{
   if ( dialog != null )
      this.buttonBar = dialog.getButtonBar();
}

/**
 * Creates a bar-listener with reference to a dialog's button bar
 * and by defining a help dialog associated. The help-ID refers to 
 * a text element in DISPLAY bundle. 
 * 
 * @param dialog <code>ButtonBarDialog</code> dialog containing the button bar
 * @param helpCode <code>String</code> the id-string of the help dialog 
 *                 available through the button bar; may be <b>null</b>   
 * since 0-5-0
 * since 0-6-0 modified parameter list
 */
public DefaultButtonBarListener ( ButtonBarDialog dialog, String helpCode )
{
   if ( dialog != null )
   {
      this.dialog = dialog;
      this.buttonBar = dialog.getButtonBar();
      this.helpCode = helpCode;
   }
}

public ButtonBarDialog getDialog () {
	return dialog;
}

public boolean okButtonPerformed ()
{
   if ( buttonBar != null )
      buttonBar.disposeDialog();
   
   return true;
}

@Override
public void noButtonPerformed ()
{
   cancelButtonPerformed();
}

public void cancelButtonPerformed ()
{
   if ( buttonBar != null )
      buttonBar.disposeDialog();
}

public void helpButtonPerformed ()
{
   if ( dialog != null & helpCode != null )
   {
      GUIService.toggleHelpDialog( dialog, helpCode );
   }
}

public boolean extraButtonPerformed ( Object button )
{
   return true;
}

}
