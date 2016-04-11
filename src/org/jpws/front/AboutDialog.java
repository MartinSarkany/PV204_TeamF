/*
 *  AboutDialog in org.jpws.front
 *  file: AboutDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 29.06.2005
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkListener;

import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;

/**
 *  The "About" dialog.
 * 
 * @author Wolfgang Keller
 * @since 0-5-0 subclass of ButtonBarDialog
 */
public class AboutDialog extends ButtonBarDialog
{
   private JEditorPane    epane = new JEditorPane();
   private JPanel         content;
/*
   {
      protected InputStream getStream ( URL url ) throws IOException
      {
         String path;
         InputStream in;
         
         path = url.toExternalForm();
         
         System.out.println( "-- JEditorPane: loading URL: " + path );
//         path = Util.substituteTextS( path, "file:/", "file://localhost/");
//         url = new URL( path );
//         System.out.println( "-- JEditorPane: converted URL: " + url );
         
         in = super.getStream( url );
         System.out.println( "-- JEditorPane: received InputStream: " + in );
         return in;
      }
   };
*/  
   private HyperlinkListener hyperListener;   
   
   
	/**
     * Constructor.
	 * @throws java.awt.HeadlessException
	 */
	public AboutDialog() throws HeadlessException
	{
		super( Global.getActiveFrame(), DialogButtonBar.OK_BUTTON, false );
		setAutonomous( true );
		markSingleton("ProgramAboutDialog");
		setTitle( ResourceLoader.getDisplay("about.title") );
        content = new JPanel( new BorderLayout() );

		buildTopPanel();
		buildCentrePanel();

        setDialogPanel( content );
		setVisible( true );
	}

   private void destruct ()
   {
      epane.removeHyperlinkListener( hyperListener );
      hyperListener = null;
   }

   public void dispose ()
   {
      super.dispose();
      destruct();
   }
   
   private void buildCentrePanel()
   {
      JScrollPane scroll;
      URL page;
      String hstr;

      hyperListener = createHyperLinkListener();

      epane.setPreferredSize( new Dimension(550,412) );
      epane.setBorder( new EmptyBorder( 0, 40, 0, 40 ) );
      epane.setEditable( false );
      epane.setOpaque( false );
      epane.setContentType( "text/html;charset=ISO-8859-1" );
      epane.addHyperlinkListener( hyperListener );

//      out = new ByteArrayOutputStream();
      try {
         hstr = ResourceLoader.getCommand( "html.file.about" );
         page = ResourceLoader.getResourceURL( "#standards/".concat( hstr ));
//         page = new URL( "http://xyz.gmxhome.de/apps/about.html");
//         in = page.openStream();
//         org.jpws.pwslib.global.Util.transferData( in, out, 1024 );
//         text = out.toString("ISO-8859-1");
         
//         System.out.println( "--- loading into JEditorPane: " + page );
         epane.setPage( page );  
//         epane.setText( text );  
      }
      catch ( IOException e )
      {
         epane.setContentType( "text/plain" );
         epane.setText( e.toString() );
      }

      scroll = new JScrollPane( epane );
      scroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
      content.add( scroll, BorderLayout.CENTER );
   }

   
	private void buildTopPanel()
	{
/*      
      ImageIcon   icon;
      JButton     logo;

      		icon	= ResourceLoader.getImageIcon("pwsafe-logo");
		logo	= new JButton( icon );

		logo.setDisabledIcon( icon );
		logo.setEnabled( false );
		logo.setBorder( new EmptyBorder( 6, 0, 0, 0 ) );

		content.add( logo, BorderLayout.NORTH );
*/      
	}

	public HyperlinkListener createHyperLinkListener ()
{
      return Global.createHyperLinkListener();
}
}
