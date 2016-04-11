/*
 *  ReporterWindow in org.jpws.front.util
 *  file: ReporterWindow.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 16.02.2006
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Writer;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/*
 * A dialog window - derived from <code>AutomoveDialog</code> - that functions
 * as a text reporter device. There is no other input into this window than
 * 'close' button, hence instances of this class are always non-modal.
 */

public class ReporterWindow extends AutomoveDialog
{
   public static final Dimension DEFAULT_DIMENSION = new Dimension( 400, 250 );

   private JScrollPane scrollPane;
   private JTextArea textArea;
   private WindowWriter writer = new WindowWriter();
   private DialogButtonBar buttonBar;
   

/** Creates a reporter window with default dimension settings and still missing a
 * parent window. 
 * 
 * @throws HeadlessException
 */ 
public ReporterWindow () throws HeadlessException
{
   super();
   init( null );
}

/** Creates a reporter window for a given parent frame. Position is set
 *  close outside the frame and dimension is set to a default value. 
 * 
 *  @param owner parent <code>Frame</code>
 *  @throws HeadlessException
 */ 
public ReporterWindow ( Frame owner ) throws HeadlessException
{
   super( owner );
   init( owner );
}

/** Creates a reporter window for a given parent frame. Position is set
 *  close outside the frame and dimension is set to a default value. 
 * 
 *  @param owner parent <code>Frame</code>
 *  @param title window decoration title
 *  @throws HeadlessException
 */ 
public ReporterWindow ( Frame owner, String title ) throws HeadlessException
{
   super( owner, title );
   init( owner );
}

/** Creates a reporter window for a given parent frame. Position is set
 *  close outside the owner and dimension is partially derived from the owner. 
 * 
 *  @param owner parent <code>Frame</code>
 *  @throws HeadlessException
 */ 
public ReporterWindow ( Dialog owner ) throws HeadlessException
{
   super( owner );
   init( owner );
}

/** Creates a reporter window for a given parent frame. Position is set
 *  close outside the owner and dimension is partially derived from the owner. 
 * 
 *  @param owner parent <code>Frame</code>
 *  @throws HeadlessException
 */ 
public ReporterWindow ( Dialog owner, String title ) throws HeadlessException
{
   super( owner, title );
   init( owner );
}

private void init ( Object owner )
{
   Dimension dim;
   Rectangle parentBounds;
   Point point;
   JPanel panel;
   
   // WINDOW SIZING AND POSITION
   dim = (Dimension)DEFAULT_DIMENSION.clone();
   if ( owner != null )
   {
      // set dimension
      parentBounds = ((Component)owner).getBounds();
      dim.height = parentBounds.height;
      
      // set origin
      point = new Point( parentBounds.x - dim.width, parentBounds.y );
      Util.setCorrectedLocation( this, point );
   }
   
   // WINDOW COMPONENTS
   textArea = new JTextArea();
   scrollPane = new JScrollPane( textArea );
   panel = new JPanel( new BorderLayout() );
   panel.add( scrollPane, BorderLayout.CENTER );
   
   getContentPane().add( panel, BorderLayout.CENTER );
   pack();
   setSize( dim );
}  // init

/*
public void finalize ()
{
   System.out.println( "-- finalize ReporterWindow" );
}
*/

/** Adds a button bar with a CLOSE button to this window. */
public void addButtonBar ()
{
   if ( buttonBar == null )
   {
      buttonBar = new DialogButtonBar( DialogButtonBar.CLOSE_BUTTON );
      getContentPane().add( buttonBar, BorderLayout.SOUTH );
   }
}

/**
 * Returns a <close>Writer</close> object to receive output text for this
 * reporter window.
 * 
 * @return <close>Writer</close>
 */
public Writer getWriter ()
{
   return writer;
}

/** Erases window content. */
public void clearText ()
{
   textArea.setText( null );
}

//  ************  INNER CLASSES  *****************

private class WindowWriter extends Writer
{

   /* 
    * Causes the window content to clear.
    */
   public void close () throws IOException
   {
   }

   /* 
    * Overridden: @see java.io.Writer#flush()
    */
   public void flush () throws IOException
   {
   }

   /* 
    * Overridden: @see java.io.Writer#write(char[], int, int)
    */
   public void write ( char[] cbuf, int off, int len ) throws IOException
   {
      if ( !ReporterWindow.this.isShowing() )
         ReporterWindow.this.setVisible( true );
      
      textArea.append( new String( cbuf, off, len ) );
   }
}

}
