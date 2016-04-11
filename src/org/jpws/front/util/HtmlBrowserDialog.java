/*
 *  HtmlBrowserDialog in org.jpws.front.util
 *  file: HtmlBrowserDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 14.07.2005
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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.io.IOException;
import java.net.URL;

import org.jpws.data.Options;
import org.jpws.front.Global;

/**
 *  Dialog to display and browse a HTML text document.
 *    
 *  @since 0-5-0 subclass of ButtonBarDialog
 */
public class HtmlBrowserDialog extends ButtonBarDialog
{
   private HtmlDialogPane htmlPane;
   private String boundsToken; 
   private boolean boundsRelative;

/**
 * Creates a non-modal dialog.
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog () throws HeadlessException
{
   super();
   init();
}

/**
 * @param owner
 * @param modal
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog ( Dialog owner, boolean modal )
      throws HeadlessException
{
   super( owner, DialogButtonBar.OK_BUTTON, modal );
   init();
}

/**
 * @param owner
 * @param modal
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog ( Frame owner, boolean modal )
      throws HeadlessException
{
   super( owner, DialogButtonBar.OK_BUTTON, modal );
   init();
}

/**
 * @param owner
 * @param title
 * @param modal
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog ( Dialog owner, String title, boolean modal )
      throws HeadlessException
{
   this( owner, modal );
   setTitle( title );
}

/**
 * @param owner
 * @param title
 * @param modal
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog ( Frame owner, String title, boolean modal )
      throws HeadlessException
{
   this( owner, modal );
   setTitle( title );
}

private void init ()
{
   htmlPane = new HtmlDialogPane();
   htmlPane.addHyperlinkListener( Global.createHyperLinkListener() );
   htmlPane.setPreferredSize( new Dimension(500,400) );

   setResizable( true );
   setDialogPanel( htmlPane );
}

/*
public void setVisible ( boolean v )
{
   if ( v )
      Util.centreWithin( this.getOwner(), this );
   super.setVisible( v );
}
*/
/*
public void setPreferredSize( Dimension x )
{
   
}
*/
public void setText ( String text )
{
   htmlPane.getEditorPane().setText( text );
}

public void setPage ( URL page ) throws IOException
{
   htmlPane.getEditorPane().setPage( page );
}

/**
 * If <code>token</code> is not <b>null</b> this activates
 * persistent storage and recovery of this dialog's bounds
 * in global options.
 * 
 * @param token String name of bounds record
 * @param relative if <b>true</b> bounds are seen in relation to parent window
  *        otherwise absolute 
 * @since 0-5-0
 */
public void setBoundsToken( String token, boolean relative )
{
   boundsRelative = relative;
   boundsToken = token;
}

public void show ()
{
   if ( boundsToken != null && Options.isOptionSet( "rememberScreen" ) )
      gainBounds( Options.getOptions(), boundsToken, boundsRelative );
   
   super.show();
}

// since 0-5-0
public void dispose ()
{
   if ( boundsToken != null && Options.isOptionSet( "rememberScreen" ) )
      storeBounds( Options.getOptions(), boundsToken, boundsRelative );
   
   super.dispose();
}

}
