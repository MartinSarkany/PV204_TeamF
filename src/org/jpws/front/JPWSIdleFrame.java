/*
 *  JPWSIdleFrame in org.jpws.front
 *  file: JPWSIdleFrame.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 16.06.2005
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

package org.jpws.front;

import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.global.Log;

/**
 *  The<code>JFrame</code> that is active when the program is in IDLE state.
 */
public class JPWSIdleFrame extends JFrame
{
   private String winTitle;
   
public JPWSIdleFrame ()
{
   super();
   winTitle = Global.APPLICATION_TITLE;
   setTitle( winTitle );
   setIconImage( ResourceLoader.getImageIcon( "pwsafe-logo-idle" ).getImage() );
   setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
//   setExtendedState( JFrame.ICONIFIED );
//   setResizable( false );
   addWindowEvents();
   
   pack();
   setLocation( Util.centredWindowLocation( new Rectangle(getToolkit().getScreenSize()), 
         this.getSize()) );
}  // constructor  
   
private void addWindowEvents()
{
   addWindowListener( new WindowAdapter()
   {
      public void windowClosing( WindowEvent arg0 )
      {
         System.out.println( "++ IDLE FRAME closing ++" );
         Global.exit( true );
      }
      
      public void windowDeiconified( WindowEvent arg0 )
      {
         Log.log( 8, "(JPWSIdleFrame.WindowAdapter) -- received DEICONIFIED on IdleFrame" );
         if ( !SystemDesktopHandler.get().isSwitching() )
            Global.setIconified( false );
      }
/*      
      public void windowActivated(WindowEvent arg0 )
      {
//         System.out.println( "++ iconified window activated ++" );
//         JPWSIdleFrame.this.requestFocus();
//         JPWSIdleFrame.this.toFront();
      }
*/
   } );

}  // addWindowEvents

}
