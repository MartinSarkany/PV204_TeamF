/*
 *  HistoryHandler in org.jpws.front
 *  file: HistoryHandler.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 03.10.2006
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

package org.jpws.front;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jpws.front.util.RecentList;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;

@SuppressWarnings("serial")
public class HistoryHandler extends JPanel
{
   private static final int MAX_ENTRIES = 255;

   private RecentList pwlist;
   private JScrollPane scrollPane;
   private JTextArea textArea;
   private boolean isOff;

   public static class Entry 
   {
      private String pwd;
      private long   time;
      
      /** Creates a new password entry from a password and its creation
       *  time.
       *  
       *  @param password history password text
       *  @param time creation time of password
       */ 
      Entry ( String password, long time )
      {
         if ( password == null )
            throw new NullPointerException();
         
         pwd = password;
         this.time = time;
      }

      public String toString ()
      {
         String tstr;

         tstr = time == 0 ? "------------------" : Util.standardTimeString( time ).substring( 0, 16 ); 
         return tstr + "  " + pwd;
      }
      
      public String toPW3String ()
      {
         String hstr;
         
         hstr = Util.intToHex( time/1000 ) +  
                Util.shortToHex( pwd.length() ) +  
                pwd;
         return hstr;
      }
      
      public String toExportString ( char separator )
      {
         String tup[];
         
         tup = new String[] { Util.xmlTimeString( time ), pwd };
         return Util.CSV.encodeLine( tup, separator );
      }
      
      public String getPassword ()
      {
         return pwd;
      }
      
      public long getTime ()
      {
         return time;
      }
   }  // inner class HistoryHandler.Entry

   
public HistoryHandler ()
{
   super( new BorderLayout( 0, 2 ) );

   JPanel panel;
   JLabel label;
   Font titleFont;
   int c1Len, height;
   
   // center text area
   textArea = new JTextArea();
   textArea.setEditable( false );
   scrollPane = new JScrollPane( textArea );
   
   add( scrollPane, BorderLayout.CENTER );
   
   // calculate length of "Created" column
   label = new JLabel( "2005-24-13 23:28  " );
   titleFont = label.getFont();
   titleFont = titleFont.deriveFont( Font.PLAIN, titleFont.getSize2D() - 1 ); 
   label.setFont( textArea.getFont() ); 
   c1Len = label.getPreferredSize().width;
   height = label.getPreferredSize().height;
   
   // title bar
   panel = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
   label = new JLabel( ResourceLoader.getDisplay( "table.created" ) );
   label.setPreferredSize( new Dimension( c1Len, height ) );
   label.setFont( titleFont );
   panel.add( label );
   label = new JLabel( ResourceLoader.getDisplay( "table.password" ) );
   label.setFont( titleFont );
   panel.add( label );
   
   add( panel, BorderLayout.NORTH );
   
   pwlist = new RecentList( 16 );
}  // constructor

/**
 * Creates a history handler with an initial content of
 * the parameter PW3 formatted text representation.
 * 
 * @param pw3Content
 */
public HistoryHandler ( String pw3Content )
{
   this();
   setContentPw3( pw3Content );
}

/**
 * Adds a new password to this history (only if this
 * history is in "Active" state).
 *  
 * @param password
 * @param time
 */
public void pushPassword ( String password, long time ) {
   if ( !isOff ) {
      pwlist.pushRecent( new Entry( password, time ) );
      updateDisplay();
   }
}

/** Clears the password content from this history. Does not
 * modify the "Active" state of this history.
 *
 */
public void clear () {
   pwlist.clear();
   updateDisplay();
}

/**
 * Returns an iterator over all entries in this password history
 * in sorted order (last-in-first-out).
 * 
 * @return <code>Iterator</code>
 */
@SuppressWarnings("unchecked")
public Iterator<HistoryHandler.Entry> iterator () {
   return (Iterator<Entry>) pwlist.iterator();
}

/** Whether this History is keeping new password changes.
 *  ("Active" state of this history.)
 * 
 *  @return <b>true</b> if and only if History is switched ON
 */
public boolean isActive () {
   return !isOff;
}

/** Sets whether this history is keeping new password changes.
 *  ("Active" state of this history.)
 *  
 *  @param v active state (<b>true</b> == active)
 */
public void setActive ( boolean v ) {
   isOff = !v;
}

@Override
public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	textArea.setEnabled(enabled);
}

/** Sets the maximum number of passwords that can be stored in this history. */
public void setMaxItems ( int max )
{
   max = Math.max( Math.min( max, MAX_ENTRIES ), 0 );
   boolean update = max < pwlist.getSize();
   pwlist.setMaxEntries( max );
   
   if ( update ) {
      updateDisplay();
   }
}

public void setFont ( Font font )
{
   super.setFont( font );
   if ( textArea != null )
      textArea.setFont( font );
}

private void updateDisplay () {
   String text = "";
   
   for ( Iterator<?> it = pwlist.iterator(); it.hasNext(); ) {
      text += Util.CRLF.concat( it.next().toString() );
   }
         
   textArea.setText( text );
   setEnabled( isActive() );
}

/** The number of passwords actually stored in this history. */
public int getListSize ()
{
   return pwlist.getSize();
}

/** The maximum number of passwords that can be stored in this history. */
public int getMaxEntries ()
{
   return pwlist.getMaxEntries();
}

/** Creates the content of this history from its PW3-format string representation.
 *  (The parameter text - if specified - also determines the new "Active" state 
 *  of this history.) 
 * 
 *  @param pw3 textual representation of password history as used in PW3;
 *         <b>null</b> or empty string clear the content
 *  @throws NumberFormatException
 */
public void setContentPw3 ( String pw3 )
{
   String hstr, password;
   int length, size, max, i, pnt;
   long time;
   
//   System.out.println( "PWHISTORY set content: [" + pw3 + "]" );
   
   pwlist.clear();
   if ( pw3 == null || pw3.length() == 0 ) return;
   
   // analyse header
   isOff = pw3.charAt( 0 ) == '0';
   max = Integer.valueOf( pw3.substring( 1, 3 ), 16 ).intValue(); 
   size = Integer.valueOf( pw3.substring( 3, 5 ), 16 ).intValue();
   setMaxItems( max );
   
   // read password entries
   pnt = 5;
   for ( i = 0; i < size; i++ ) {
      // extract time
      hstr = pw3.substring( pnt, pnt+8 );
      time = Long.valueOf( hstr, 16 ).longValue() * 1000;
      
      // extract password
      hstr = pw3.substring( pnt+8, pnt+12 );
      length = Integer.valueOf( hstr, 16 ).intValue();
      password = pw3.substring( pnt+12, pnt+12 + length );
      
      // create entry and progress pointer
      pwlist.pushRecent( new Entry( password, time ) );
      pnt += 12 + length;
   }
   
   updateDisplay();
}

/**
 * Returns a string representation of this history consisting at
 * least (empty content) in a 5-byte header sequence. (This is the
 * text conforming to PW3 format definition. The text contains information
 * of the actual "Active" state of this history.) 
 *  
 * @return String (min 5 chars)
 */
public String getContentPw3 ()
{
   StringBuffer sbuf;
   String hstr;
   Object[] items;
   int i;
   
   sbuf = new StringBuffer();
 
   // create header
   sbuf.append( isOff ? '0' : '1' );
   sbuf.append( Util.byteToHex( getMaxEntries() ) );
   sbuf.append( Util.byteToHex( getListSize() ) );
   
   // create list of passwords (reverse order)
   items = pwlist.getContent();
   for ( i = items.length; i > 0; i-- )
   {
      hstr = ((Entry)items[ i-1 ]).toPW3String();
      sbuf.append( hstr );
   }

//   System.out.println( "PWHISTORY get content: [" + sbuf.toString() + "]" );
   return sbuf.toString();
}
} 
