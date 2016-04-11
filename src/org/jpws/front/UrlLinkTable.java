/*
 *  UrlLinkTable in org.jpws.front
 *  file: UrlLinkTable.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 31.10.2005
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.jpws.front.util.ButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;

/**
 * <p>Discussion:</p>
 *  <p>An instance should issue focus-gained and focus-lost events which are equivalent 
 *  to the corresponding events of the JTable instance of this UI. (Assuming here 
 *  JTable issues these events, which is not evident!)

 */
public class UrlLinkTable extends JPanel
{
   private Dialog          parentDlg;
   private UrlTableModel   model;

   /** A list of String[] representing the rows of the table. */
   private ArrayList<String[]>       rowList = new ArrayList<String[]>();
   
   private UrlRenderer     renderer;
   private MouseAdapter    mouseAdapter;
   private ActionListener  actionListener;
   
   private JTable          table;
   private JScrollPane     scrollPane;
   private JPanel          buttonBar;
   private JButton         newButton;
   private JButton         editButton;
   private JButton         deleteButton;

   private String          contentString;
   
   
/** Constructs an empty UrlLink table. */   
public UrlLinkTable ( Dialog parent )
{
   super( new BorderLayout( 0, 3 ) );
   parentDlg = parent;
   init();
}

/** Constructs an UrlLink table with the given initial content.
 *  @param content JPWS-CSV formatted content string 
 */   
public UrlLinkTable ( Dialog parent, String content )
{
   super( new BorderLayout( 0, 3 ) );
   parentDlg = parent;
   init();
   setContent( content );
}

private void init ()
{
   actionListener = new Actions();
   
   // construct table section
   model = new UrlTableModel();
   table = new JTable( model );
   scrollPane = new JScrollPane( table );
   scrollPane.setPreferredSize( new Dimension( 10, 10 ) );
   this.add( scrollPane, BorderLayout.CENTER );
   
   mouseAdapter = new MouseAdapter();
   table.addMouseListener( mouseAdapter );
   table.setRowHeight( 40 );
   renderer = new UrlRenderer(); 
   table.setDefaultRenderer( String[].class, renderer );
   
   // construct button section
   buttonBar = new JPanel( new GridLayout( 1, 3 ) );
   newButton = makeButton( "button.new" );
   buttonBar.add( newButton );
   editButton = makeButton( "button.edit" );
   buttonBar.add( editButton );
   deleteButton = makeButton( "button.delete" );
   buttonBar.add( deleteButton );
   this.add( buttonBar, BorderLayout.SOUTH );
   
   
   // fun settings
   this.setOpaque( true );
//   this.setBackground( new Color(0xDDA0DD) );

/*   
   // test settings
   ta = new JTextArea();
   JScrollPane sc = new JScrollPane( ta );
   this.add( sc, BorderLayout.CENTER );
*/   
}  // init

private JButton makeButton ( String token )
{
   JButton but;
   
   but = new JButton( ResourceLoader.getDisplay( token ) );
   but.setActionCommand( token );
   but.addActionListener( actionListener );
   
   return but;
}  // makeButton

public void setCellFont ( Font font )
{
   UrlRenderer.label.setFont( font );
}

/**
* This discards any previous content and sets up new content for this table. Cells 
* currently edited must end edition.
* 
* @param content a persistent String representation of the content, formatted
*        in JPWS-CSV
*/
public void setContent ( String content )
{
   String[] vl, vl2;
   Util.BufferInt pos;
   
   contentString = content;

   synchronized ( rowList )
   {
      rowList.clear();
      if ( content != null )
      {
//System.out.println( "- URL set content with [" + content + "]" );         
         pos = new Util.BufferInt();
         while ( pos.value < content.length() )
         {
            vl = Util.CSV.decodeLine( content, pos.value, ',', pos );
            
            // validation (must be 2 element array)
            if ( vl.length == 0 )
               continue;
               
            if ( vl.length != 2 )
            {
               vl2 = new String[] { vl[0], "" };
               if ( vl.length > 1 )
                  vl2[1] = vl[1];
               vl = vl2;
            }
            
            rowList.add( vl );
/*            
System.out.println( "- set URL ROW with elements:" );
for ( i = 0; i < vl.length; i++ )
{
   System.out.println( "     field " + i + ": [" + vl[i] + "]" );
}
*/
         }
      }
      model.fireTableDataChanged();
   }
}  // setContent
   
/** Returns the entire actual table content as a string formatted in CSV.
 * 
 *  @return String containing the persistent storage format of this table   
*/
public String getContent ()
{
   StringBuffer sbuf;
   String hstr;
   int i;
   
   synchronized ( rowList )
   {
      sbuf = new StringBuffer();
      for ( i = 0; i < rowList.size(); i++ )
      {
         hstr = getEntry( i );
         if ( i != 0 )
            sbuf.append( "\r\n" );
         sbuf.append( hstr );
      }
   }
   contentString = sbuf.toString();
   return contentString;
}  // getContent

/**
* Returns the current content of a valid table row or null if row is not  
* a valid reference. The return value is formatted according to CSV standard. 
* 
* @param row table row, starting from 0
* @return String containing the persistent storage format of the specified table row   
*/  
public String getEntry ( int row )
{
   synchronized ( rowList )
   {
      if ( row < 0 | row > rowList.size() )
         return null;
      
      return Util.CSV.encodeLine( rowList.get( row ), ',' );
   }
}  // getEntry

/** The number of rows defined in this table. */
public int getListSize ()
{
   return rowList.size();
}

public void editRow ( int row )
{
   EditDialog dlg;
   
   if ( row > -2 & row < rowList.size() )
   {
      // create new dialog
      dlg = new EditDialog( row );
      Util.centreWithin( parentDlg, dlg );
      dlg.setVisible( true );
   }
}  // editRow

public void deleteRow ( int row )
{
   String data[], hstr, ask;
   
   if ( row > -1 & row < rowList.size() )
   {
      // get reference data and prepare 
      data = rowList.get( row );
      if ( (hstr = data[0]) == null || hstr.length() == 0 )
         hstr = data[1];
      if ( hstr != null && hstr.length() > 55 )
         hstr = hstr.substring( 0, 55 ) + "....";
      
      // ask user
      ask = ResourceLoader.getDisplay( "msg.url.askdelete" );
      ask = Util.substituteText( ask, "$url", Util.htmlEncoded( hstr ) );
      if ( hstr == null || GUIService.userConfirm( parentDlg, ask ) )
      synchronized ( rowList )   
      {
         // delete operation
         rowList.remove( row );
         model.fireTableRowsDeleted( row, row );
//         System.out.println( "-- deleted row " + row );      
      }
   }
}  // deleteRow

public void addRow ( String[] data )
{
   int row ;
   
   if ( data != null )
   synchronized ( rowList )
   {
      row = rowList.size();
      rowList.add( data );
      model.fireTableRowsInserted( row, row );
   }
}  // addRow

public void updateRow ( int row, String[] data )
{
   String[] refdat;
   
   if ( data != null & row > -1 & row < rowList.size() )
   synchronized ( rowList )
   {
      refdat = rowList.get( row );
      refdat[0] = data[0];
      refdat[1] = data[1];
      model.fireTableRowsUpdated( row, row );
   }
   else
      throw new IllegalArgumentException();
}  // updateRow

/** Adds a focus listener to the UI element of this table. */
/*
public void addFocusListener ( FocusListener listener )
{
}
*/
/** Removes a focus listener from the UI element of this table. */
/*
public void removeFocusListener ( FocusListener listener )
{
}
*/

// ***********  INNER CLASSES  **********************

private class Actions implements ActionListener
{

   public void actionPerformed ( ActionEvent e )
   {
      String cmd;
      int row;
      
      cmd = e.getActionCommand();
      row = table.getSelectedRow();
      
      // Commands of TABLE PANEL
      if ( cmd.equals( "button.new" ) )
      {
         editRow( -1 );
      }
      else if ( cmd.equals( "button.edit" ) )
      {
         if ( row != -1 )
            editRow( row );
      }
      else if ( cmd.equals( "button.delete" ) )
      {
         deleteRow( row );
      }
   }
}  // class Actions

// ************  DIALOG  *************

private class EditDialog extends JDialog implements ButtonBarListener
{
   DialogButtonBar buttonBar1;
   JTextField targetFld;
   JTextArea textArea;
   boolean createNew;
   int dataRef;

public EditDialog ( int row )
{
   super( parentDlg, true );
   createNew = row == -1;
   init();
   setData( row );
}

private void init ()
{
   JPanel center;
   String hstr;
   
   hstr = createNew ? "dlg.urls.create" : "dlg.urls.edit";
   hstr = ResourceLoader.getDisplay( hstr );
   setTitle( hstr );
   setResizable( false );
   
   // button bar
   buttonBar1 = new DialogButtonBar( DialogButtonBar.OK_CANCEL_BUTTON, true );
   buttonBar1.addButtonBarListener( this );
   getContentPane().add( buttonBar1, BorderLayout.SOUTH );
   
   // centre panel
   center = new JPanel( new VerticalFlowLayout( 4, true ) );
   center.setBorder( BorderFactory.createEmptyBorder( 10, 8, 0, 8 ) );
   getContentPane().add( center, BorderLayout.CENTER );

   center.add ( new JLabel( ResourceLoader.getDisplay( "urldlg.description" )) );
   textArea = new JTextArea();
   textArea.setLineWrap( true );
//   textArea.setFont( DisplayManager.getFont( "data" ) );
   ActionHandler.registerChangeableObject( textArea );
   scrollPane = new JScrollPane( textArea );
   scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
   scrollPane.setPreferredSize( new Dimension( 320, 100 ) );
   center.add( scrollPane );

   center.add( Box.createVerticalStrut( 3 ) );
   center.add ( new JLabel( ResourceLoader.getDisplay( "urldlg.targeturl" )) );
   targetFld = new JTextField( );
   center.add ( targetFld );
   
   pack();
}  // init

private void setData ( int row )
{
   String[] data;
   
   if ( row < 0 | row > rowList.size() )
   {
      dataRef = -1;
   }
   else
   {
      data = rowList.get( row );
      targetFld.setText(data[1] );
      textArea.setText( data[0] );
      dataRef = row;
   }
}  // setData

public void dispose ()
{
   super.dispose();
   buttonBar1 = null;
}

/* 
 * Overridden: @see org.jpws.front.util.ButtonBarListener#cancelButtonPerformed()
 */
public void cancelButtonPerformed ()
{
   dispose();
}

/* 
 * Overridden: @see org.jpws.front.util.ButtonBarListener#helpButtonPerformed()
 */
public void helpButtonPerformed ()
{
}

/* 
 * Overridden: @see org.jpws.front.util.ButtonBarListener#okButtonPerformed()
 */
public boolean okButtonPerformed ()
{
   String descr, target, error, data[];
   
   
   // Validate input
   descr = textArea.getText();
   target = targetFld.getText();
   error = null;
   if ( descr.length() == 0 & target.length() == 0 )
      error = "msg.url.failtext";
   else
   {
      if ( target.length() > 0 )
      try { new URL( target ); }
      catch ( Exception e )
      {
         error = "msg.url.failurl";
      }
   }
   
   // branch: leave with error message
   if ( error != null )
   {
      GUIService.infoMessage( this, "dlg.badrecord", error );
      return false;
   }
   
   // create NEW entry
   data = new String[] { descr, target };
   if ( createNew )
      addRow( data );
   else
      updateRow( dataRef, data );

   dispose();
   return true;
}  // okButtonPerformed

public boolean extraButtonPerformed ( Object button )
{
   return true;
}

@Override
public void noButtonPerformed ()
{}
}  // class EditDialog


private class MouseAdapter extends java.awt.event.MouseAdapter
{
/*
   private void tryPopup ( MouseEvent evt )
   {
      JPopupMenu popup;

      if ( evt.isPopupTrigger() )
      {
         popup = MenuHandler.getListviewContextMenu( PwsFileContainer.this );
         popup.show( (Component)evt.getSource(), evt.getX(), evt.getY() );
      }
   }

   public void mousePressed ( MouseEvent evt )
   {
      tryPopup( evt );
   }

   public void mouseReleased ( MouseEvent evt )
   {
      tryPopup( evt );
   }
*/
   public void mouseClicked ( MouseEvent evt )
   {
      URL url;
      String data[], hstr;
      int row;

      ActionHandler.resetIdleTime();
      if ( evt.getButton() == MouseEvent.BUTTON1 &&
           evt.getClickCount() > 1 )
      {
         row = ((JTable)evt.getSource()).rowAtPoint( new Point( evt.getX(), evt.getY() ) );
         data = rowList.get( row );
         hstr = data[1];
         
         if ( hstr != null && hstr.length() > 0 )
            try {
               url = new URL( hstr );
               Global.startBrowser( url );
            }
         catch ( Exception e )
         {
            hstr = ResourceLoader.getDisplay( "msg.url.failbrowser" );
            GUIService.failureMessage( hstr, e );
         }
      }
   }  // mouseClicked
   }  // class MouseAdapter

private class UrlTableModel extends AbstractTableModel
{
   String columnTitle;
   
   public UrlTableModel ()
   {
      columnTitle = "Site / URL";
   }

   /* 
    * Overridden: @see javax.swing.table.DefaultTableModel#getColumnCount()
    */
   public int getColumnCount ()
   {
      return 1;
   }

   /* 
    * Overridden: @see javax.swing.table.DefaultTableModel#getColumnName(int)
    */
   public String getColumnName ( int column )
   {
      return null;
   }

   /* 
    * Overridden: @see javax.swing.table.DefaultTableModel#getRowCount()
    */
   public int getRowCount ()
   {
      return rowList.size();
   }

   /* 
    * Overridden: @see javax.swing.table.DefaultTableModel#getValueAt(int, int)
    */
   public Object getValueAt ( int row, int column )
   {
//      String data[];
      
      if ( row > -1 & row < rowList.size() )
      {
//         data = (String[])rowList.get( row );
//System.out.println( "- row data: " + data[0] + "," + data[1] );      
         return rowList.get( row );
      }
      return null;
   }

   /* 
    * Overridden: @see javax.swing.table.AbstractTableModel#getColumnClass(int)
    */
   public Class getColumnClass ( int columnIndex )
   {
      return String[].class;
   }
   
   
}  // UrlTableModel

private static class UrlRenderer extends DefaultTableCellRenderer
{
   static JLabel label = new JLabel();
   static Color normalColor = Color.white;
   static Color selectedColor = new Color( 0xF0E68C );
   static { label.setOpaque( true ); }

   /* 
    * Overridden: @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
    */
   public Component getTableCellRendererComponent ( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
   {
      String text, hstr1, hstr2, data[];
      int height;
      
      if ( value != null && value.getClass().equals( String[].class ))
      {
         data = (String[])value;
         if ( (hstr1 = data[0]) == null )
            hstr1 = "";
         if ( (hstr2 = data[1]) == null )
            hstr2 = "";
         text = "<html>" + Util.htmlEncoded( hstr1 ) + 
                "<p><font color=\"blue\"><i><b>" + Util.htmlEncoded( hstr2 ) + 
                "</b></i></font></html>";
         label.setText( text );
      
         height = label.getPreferredSize().height + 4;
         if ( table.getRowHeight( row ) != height )
            table.setRowHeight( row, height );
         
         label.setBackground( isSelected ? selectedColor : normalColor );
            
         return label;
      }
      else
         return null;
   }  // getTableCellRendererComponent

}

}
