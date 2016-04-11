/*
 *  HtmlDialogPane in org.jpws.front.util
 *  file: HtmlDialogPane.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkListener;

/**
 *  A <code>JPanel</code> containing a scrollable HTML browsing facility based
 *  on <code>JEditorPane</code>.
 */
public class HtmlDialogPane extends JPanel {
   
   private JEditorPane editor;
   private JScrollPane scroll;
   
/**
 * Constructs an empty scrollable HTML display panel. 
 */
public HtmlDialogPane () {
   super();
   init( true );
}

/**
 * Constructs an empty HTML display panel whose scrollability can be determined
 * by parameter.
 * 
 * @param scrollable whether html display will be an element of a <code>JScrollPane</code> 
 */
public HtmlDialogPane ( boolean scrollable ) {
   super();
   init( scrollable );
}

/** Constructs a scrollable HTML display panel with initial display text.
 * @param text initial text to be displayed
 */
public HtmlDialogPane ( String text ) {
   super();
   init( true );
   editor.setText( text );
}

/** Constructs a scrollable HTML display panel with initial display text
 *  fetched from the given URL.
 * 
 * @param url initial page to be displayed
 * @throws IOException if the given url cannot be accessed
 */
public HtmlDialogPane ( URL url ) throws IOException {
   super();
   init( true );
   editor.setPage( url );
}

private void init ( boolean scrollable ) {
   BorderLayout layout;
   Component component;
   
   layout = new BorderLayout();
   setLayout( layout );
   
   
   editor = new JEditorPane();
   editor.setBorder( BorderFactory.createEmptyBorder( 4, 10, 4, 0 ) );
   editor.setEditable( false );
   editor.setBackground(Color.white);
   editor.setContentType( "text/html;charset=ISO-8859-1" );
   component = editor;
   
   if ( scrollable ) {
      scroll = new JScrollPane( editor );
      scroll.setHorizontalScrollBarPolicy( 
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED  );
      scroll.setVerticalScrollBarPolicy( 
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED ); 
      component = scroll;
   }
   
   this.add( component, BorderLayout.CENTER );
}  // init

public JScrollPane getScrollPane () {
   return scroll;
}

public JEditorPane getEditorPane () {
   return editor;
}

public void setText ( String text ) {
   editor.setText( text );
}

public void setPage ( URL page ) throws IOException {
   editor.setPage( page );
}

public void addHyperlinkListener( HyperlinkListener hyperListener )
{
   editor.addHyperlinkListener( hyperListener );
}

public void removeHyperlinkListener( HyperlinkListener hyperListener ) {
   editor.removeHyperlinkListener( hyperListener );
}

//@Override
//public void setBackground(Color bg) {
//	super.setBackground(bg);
//	if (editor != null) {
//	   editor.setBackground(bg);
//	}
//}
//
//@Override
//public void setForeground(Color bg) {
//	super.setForeground(bg);
//	if (editor != null) {
//	   editor.setForeground(bg);
//	}
//}


}
