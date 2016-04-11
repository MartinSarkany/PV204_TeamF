/*
 *  TableHandler in org.jpws.front
 *  file: TableHandler.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 28.09.2004
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jpws.data.Options;
import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.order.DefaultRecordWrapper;
import org.jpws.pwslib.order.OrderedListEvent;
import org.jpws.pwslib.order.OrderedListListener;
import org.jpws.pwslib.order.OrderedRecordList;

/**
 * Defines a <code>TableModel</code> and organises the display of a 
 * <code>JTable</code> related to the content of a <code>PwsFileContainer</code>.
 */
public class TableHandler implements ContainerView, OrderedListListener,
                           ListSelectionListener
{
   public static final Color EXPIRED_COLOR = new Color( 0xB22222 ); // firebrick
   public static final Color EXPIRESOON_COLOR = new Color( 0xCD853F ); // peru
   public static final Color IMPORTED_COLOR = new Color( 0xF5CD00 ); // gold (reduced)
   public static final Color IMPORTSELECTED_COLOR = IMPORTED_COLOR; //Color.GRAY;  
   public static final Color CONFLICT_COLOR = new Color( 0x4169E1 ); // royalblue
   public static final Color CONFLICTSELECTED_COLOR = new Color( 0x191970 ); // midnightblue
   public static final Color LIGHTRED_COLOR = new Color( 0xFF6347 ); // tomatoe
   public static final Color DEFAULT_SELECTION_BGD_COLOR = new Color( 0xADD8E6 ); // lightblue
//   public static final Color BACKGROUND_COLOR = new Color( 0xF5F5F5 ); // white smoke 
   
   private static Font boldFont;
   
   private PwsFileContainer       owner;
   private PwsTableModel          tableModel;
   private JTable                 table;
   private ObjectChangeListener   objectListener = new ObjectChangeListener(this);   
   private boolean                useColors;
   
/**
 */
public TableHandler ( PwsFileContainer owner )
{
   init( owner );
}

private void init ( PwsFileContainer owner )
{
   this.owner = owner;
   owner.getOrderedList().addOrderedListListener( this );

   tableModel = new PwsTableModel( owner.getOrderedList() );
   TableColumnModel colModel = createColumnModel();
   
   table = new JTable( tableModel, colModel );
   table.setName( "Main Data-Table" );
   table.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
   table.addMouseListener( owner.getMouseAdapter() );
   table.getSelectionModel().addListSelectionListener( this );
   table.setDefaultRenderer( "".getClass(), new PwsCellRenderer() );
   objectListener.registerChangeableObject( table );
   
   table.setDragEnabled( true );
   table.setTransferHandler( owner.getTransferHandler() );
   
   KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
   Action action = ActionHandler.getAction("EDIT_ENTRY");
   table.getInputMap().put(stroke, "EDIT_ENTRY");
   table.getActionMap().put("EDIT_ENTRY", action);
   
   useColors = Options.isOptionSet("useTableColors");
}

private static final String COLUMN_TITLE = "TITLE";
private static final String COLUMN_USER = "USER";
private static final String COLUMN_GROUP = "GROUP";

/**
 * Creates a column model for this table. If user options
 * exist in persistent file options, the column model is
 * adjusted accordingly.
 * 
 * @return <code>TableColumnModel</code>
 * @since 0-6-0
 */
private TableColumnModel createColumnModel ()
{
   TableColumnModel m;
   TableColumn col;
   String setup, name, sarr[], carr[];
   int size, h, i;
   
   // create default model of all columns 
   m = new DefaultTableColumnModel();
   
   col = new TableColumn( 0 );
   col.setIdentifier( COLUMN_GROUP );
   col.setHeaderValue( ResourceLoader.getDisplay( "table.group" ) );
   m.addColumn( col );
   col = new TableColumn( 1 );
   col.setIdentifier( COLUMN_TITLE );
   col.setHeaderValue( ResourceLoader.getDisplay( "table.title" ) );
   m.addColumn( col );
   col = new TableColumn( 2 );
   col.setIdentifier( COLUMN_USER );
   col.setHeaderValue( ResourceLoader.getDisplay( "table.username" ) );
   m.addColumn( col );

   // setup user choices on columns
   if ( (setup = owner.getMinorOptions().getOption( "userColumnModel" )).length() > 0 )
   {
      sarr = Util.CSV.decodeLine( setup, 0, ',' );
      for ( i = 0; i < sarr.length; i++ )
      try {
         // identify column and adjust its position and preferred size
         carr = Util.CSV.decodeLine( sarr[i], 0, ',' );
         name = carr[ 0 ];
         size = Integer.parseInt( carr[ 1 ] );
         h = m.getColumnIndex( name );
         m.getColumn( h ).setPreferredWidth( size );
         m.moveColumn( h, i );
         Log.debug( 9, "(TableHandler.createColumnModel) column setup performed: "
               + name + ", w=" + size );
      }
      catch ( Exception e )
      { 
         e.printStackTrace();
         continue; 
      }
   }

   m.addColumnModelListener( new OurTableColumnListener() );
   return m;
}

/** Saves current column setup to persistent file options. 
 * @since 0-6-0
 */
private void saveColumnModel ()
{
   TableColumnModel model;
   TableColumn col;
   String setup, name, size, sarr[];
   int i, cols;
   
//   if ( !(Options.isOptionSet( "storeMinorChanges" ) | owner.isModified()) || 
   if ( table == null ) return;
   
   model = table.getColumnModel();
   cols = model.getColumnCount();
   
   // create column setup value for persistence
   sarr = new String[ cols ];
   for ( i = 0; i < cols; i++ ) {
      col = model.getColumn( i );
      name = (String)col.getIdentifier();
      size = String.valueOf( col.getPreferredWidth() );
      sarr[ i ] = Util.CSV.encodeLine( new String[] {name,size}, ',' );
   }
   setup = Util.CSV.encodeLine( sarr, ',' );
//   Log.log( 9, "(TableHandler.saveColumnModel) model setup = ".concat( setup ) );
   
   // store setup value
   owner.getMinorOptions().setOption( "userColumnModel", setup );
//   Log.log( 9, "(TableHandler.saveColumnModel) model saved: ".concat( owner.getDatabaseName() ) );
}

/**
 * Ensures the destruction of circular object references when this object is
 * no more needed.
 *
 */
public void destruct ()
{
   owner = null;
   table = null;
   tableModel = null;
}

@Override
public int getFirstSelected ()
{
   synchronized ( table ) {
      return table.getSelectedRow();
   }
}

@Override
public int[] getSelectedItems ()
{
   synchronized ( table ) {
      return table.getSelectedRows();
   }
}

@Override
public boolean hasUserSelection ()
{
   return getFirstSelected() != -1;
}

@Override
public void setSelectAll ()
{
   Runnable r = new Runnable() {
      @Override
	  public void run () {
         synchronized ( table ) {
            int size = owner.getOrderedList().size();
            if ( size > 0 ) {
               table.addRowSelectionInterval( 0, size-1 );
            }
         }
      }
   };
   try { ActionHandler.executeOnEDT_Wait( r ); }
   catch ( Exception e )
   { e.printStackTrace(); }
}

@Override
public void setSelectedIndex ( int index )
{
   int[] sel = null;
   if ( index > -1 ) {
      sel = new int[1];
      sel[0] = index;
   }
   setSelectedItems( sel );   
}

@Override
public void setSelectedItems ( final int[] selection )
{
   Runnable r = new Runnable() {
      @Override
	  public void run () {
         int i, v, first;

         synchronized ( table ) {
            table.clearSelection();
            
            if ( selection != null && selection.length > 0 ) {
               // add selection, find first selection index
               first = Integer.MAX_VALUE;
               for ( i = 0; i < selection.length; i++ ) {
                  v = selection[i];
                  // check if in bounds of model
                  if ( v > -1 && v < tableModel.getRowCount() ) {
                     table.addRowSelectionInterval( v, v );
                     if ( v < first ) {
                        first = v;
                     }
                  }
               }
               
               // scroll to first row of selection
               scrollToVisible( first );
            }
         }
      }
   };
   try { ActionHandler.executeOnEDT_Wait( r ); }
   catch ( Exception e )
   { e.printStackTrace(); }
}  // setSelectedItems

@Override
public DefaultRecordWrapper[] getSelectedWrappers ()
{
   synchronized ( table ) {
      int[] items = getSelectedItems();
      DefaultRecordWrapper[] result = new DefaultRecordWrapper[ items.length ];
      
      for ( int i = 0; i < items.length; i++ ) {
         result[ i ] = tableModel.getRecord( items[ i ] );
      }
      return result;
   }
}

@Override
public void setSelectedWrappers ( DefaultRecordWrapper[] selection )
{
   int len = selection == null ? 0 : selection.length;
   int[] items = new int[ len ];

   synchronized ( table ) {
	   for ( int i = 0; i < len; i++ ) {
	      items[ i ] = tableModel.getIndexOf( selection[ i ] );
	   }
   }
   setSelectedItems( items );
}

@Override
public JComponent getView ()
{
   return table;
}

public void updateUI ()
{
   table.updateUI();
}

@Override
public void scrollToVisible ( final int index )
{
   if ( index > -1 ) {
      Runnable r = new Runnable () {
         @Override
		 public void run () {
            synchronized ( table ) {
               if ( index < table.getRowCount() )
                  table.scrollRectToVisible( table.getCellRect( index, 0, true ) );
            }
         }
      };
      
      // wait for scroll to happen
      try
      { ActionHandler.executeOnEDT_Wait( r ); }
      catch ( Exception e )
      { e.printStackTrace(); }
   }
}
   
//  *********  IMPLEMENTATIONS  **********

@Override
public void orderedListPerformed ( final OrderedListEvent evt )
{
   Runnable r = new Runnable() {
   @Override
   public void run() {
         int row = evt.getIndex();

         if ( table != null )
         synchronized ( table )
         {
            switch ( evt.getType() ) {
            case OrderedListEvent.ITEM_ADDED:
               tableModel.fireTableRowsInserted( row, row );
               break;
            case OrderedListEvent.ITEM_REMOVED:
               tableModel.fireTableRowsDeleted( row, row );
               break;
            case OrderedListEvent.ITEM_UPDATED:
               tableModel.fireTableRowsUpdated( row, row );
               break;
            case OrderedListEvent.LIST_RELOADED:
               tableModel.fireTableDataChanged();
               scrollToVisible( getFirstSelected() );
               break;
            default: 
               tableModel.fireTableDataChanged();
            }
         }
      }
   };
   try {
	  ActionHandler.executeOnEDT_Wait( r );
   } catch (InvocationTargetException e) {
	  e.printStackTrace();
   } catch (InterruptedException e) {
	e.printStackTrace();
}
}

/**
 * Handles selection of a table node.
 * 
 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
 */
@Override
public void valueChanged( ListSelectionEvent e )
{
   owner.reportPropertyChange( PwsFileContainer.SELECTION_STATUS ); 
   ActionHandler.resetIdleTime();
}


@Override
public void reconstruct ()
{
   init( owner );
}

@Override
public void repaint ()
{
   useColors = Options.isOptionSet("useTableColors");
   table.revalidate();
   table.repaint();
}

@Override
public void setFont ( Font font )
{
   table.setFont( font );
   boldFont = null;
}

//  *************  INNER CLASSES  ***************

/** This listener stores modified column setup into persistent file
 *  options, unless "minorFileChanges" is switched off.
 *  @since 0-6-0
 */
private class OurTableColumnListener implements TableColumnModelListener
{
   @Override
   public void columnSelectionChanged ( ListSelectionEvent e ) {
   }
   
   @Override
   public void columnRemoved ( TableColumnModelEvent e ) {
      saveColumnModel();
   }
   
   @Override
   public void columnMoved ( TableColumnModelEvent e ) {
      saveColumnModel();
   }
   
   @Override
   public void columnMarginChanged ( ChangeEvent e ) {
      saveColumnModel();
   }
   
   @Override
   public void columnAdded ( TableColumnModelEvent e ) {
      saveColumnModel();
   }
}

private class PwsTableModel extends AbstractTableModel
{
   private OrderedRecordList recList;

   public PwsTableModel( OrderedRecordList list ) {
     recList = list;
   }

   @Override
   public int getColumnCount() {
     return 3;
   }

   @Override
   public int getRowCount() {
      return recList.size();
   }

   public DefaultRecordWrapper getRecord ( int row ) {
      return recList.getItemAt( row );
   }
   
   public int getIndexOf ( DefaultRecordWrapper rec ) {
      return recList.indexOf( rec );
   }
   
   @Override
   public Object getValueAt( int row, int col ) {
     DefaultRecordWrapper wrap = getRecord( row ); 
     if ( wrap == null ) return "";
     
     PwsRecord record = wrap.getRecord();
     Object value = null;

     switch (col) {
     
//       case 0:
//          int expiry = wrap.getExpiry();
//          int importStatus = wrap.getImportStatus();
//          String icon = "treeleaf-pass";
//          if ( expiry > 0 | importStatus > 0 ) {
//             if ( importStatus == DefaultRecordWrapper.IMPORTED )
//                icon = "treeleaf-pass-imported";
//             else if ( importStatus == DefaultRecordWrapper.IMPORTED_CONFLICT )
//                icon = "treeleaf-pass-importedconflict";
//             else if ( expiry == DefaultRecordWrapper.EXPIRED )
//                icon = "treeleaf-pass-expired";
//             else if ( expiry == DefaultRecordWrapper.EXPIRE_SOON )
//                icon = "treeleaf-pass-expiresoon";
//          }
//          value = ResourceLoader.getImageIcon( icon );
//          break;
//
       case 0:
         value = record.getGroup();
         break;

       case 1:
         value = record.getTitle();
         break;

       case 2:
         value = record.getUsername();
         break;
     }

     return value == null ? "" : value;
   }

   @Override
   public Class<?> getColumnClass(int c) {
      return String.class;
   }
}

private class PwsCellRenderer extends DefaultTableCellRenderer 
{
//   private final Color BACKGROUND_COLOR = new Color( 0x00CED1 );
   private Color defaultForeground = UIManager.getColor ("Table.foreground"); 
   private Color defaultBackground = UIManager.getColor ("Table.background");
   private Color defaultSelectionForeground = UIManager.getColor ("Table.selectionForeground");
   private Color defaultSelectionBackground = UIManager.getColor ("Table.selectionBackground");
   
   public PwsCellRenderer() {
	  if (defaultForeground == null) defaultForeground = Color.black;
	  if (defaultBackground == null) defaultBackground = Color.white;
	  if (defaultSelectionForeground == null) defaultSelectionForeground = Color.black;
	  if (defaultSelectionBackground == null) defaultSelectionBackground = DEFAULT_SELECTION_BGD_COLOR;
   }

   @Override
   public Component getTableCellRendererComponent(
         JTable table1, 
         Object value, 
         boolean isSelected, 
         boolean hasFocus, 
         int row, 
         int column )
   {
      DefaultRecordWrapper wrap;
      Font font;
      Color color, bgdColor;
      int expiry, importStatus;
      
      super.getTableCellRendererComponent( table1, value, isSelected, hasFocus, row, column );
      
      if ( tableModel == null || (wrap = tableModel.getRecord( row )) == null )
         return null;
      
	  expiry = wrap.getExpiry();
      importStatus = wrap.getImportStatus();
      color = isSelected ? defaultSelectionForeground : defaultForeground;
      bgdColor = isSelected ? defaultSelectionBackground : defaultBackground;
      
      if ( useColors && (expiry > 0 | importStatus > 0) ) {
    	 // set text colour
         if ( importStatus == DefaultRecordWrapper.IMPORTED )
            color = isSelected  ? IMPORTSELECTED_COLOR : IMPORTED_COLOR;
         else if ( importStatus == DefaultRecordWrapper.IMPORTED_CONFLICT )
            color = isSelected  ? CONFLICTSELECTED_COLOR : CONFLICT_COLOR;
         else if ( expiry == DefaultRecordWrapper.EXPIRED )
//            color = isSelected ? LIGHTRED_COLOR : EXPIRED_COLOR;
         	color = EXPIRED_COLOR;
         else if ( expiry == DefaultRecordWrapper.EXPIRE_SOON )
            color = EXPIRESOON_COLOR;

         // set font
         if ( boldFont == null ) {
            font = getFont();
            boldFont = font.deriveFont( Font.BOLD + 
                       (font.isItalic() ? Font.ITALIC : 0) );
         }
         setFont( boldFont );
      }

	  // correct background + foreground colours for contrast
      if ( color != null & bgdColor != null ) {
	     boolean brighter = brightnessValue(color) > brightnessValue(bgdColor);
	     while (contrast(color, bgdColor) < 300) {
	   	    if (brighter) {
	   		   color = brighter(color);
	   		   bgdColor = darker(bgdColor);
//	   		   Log.debug(10, "(PwsCellRenderer) applying BRIGHTER - DARKER to color/bgd");
	   	    } else {
	   		   color = darker(color);
	   		   bgdColor = brighter(bgdColor);
//	   		   Log.debug(10, "(PwsCellRenderer) applying DARKER - BRIGHTER to color/bgd");
	   	    }
	     }
      } else {
  		   Log.debug(10, "(PwsCellRenderer) *** " + (color == null ? 
  				          "color" : "bgdColor") + " is null");
      }
      
      // set the component's print colours
      setForeground( color );
      setBackground( bgdColor );
      
      return this;
   }
   
   private static final int COLOR_DELTA = 20;  
   
   private Color brighter (Color c) {
	   int d = COLOR_DELTA;
	   Color v = new Color(
			   Math.min(c.getRed()+d, 255), 
			   Math.min(c.getGreen()+d, 255), 
			   Math.min(c.getBlue()+d, 255));
	   return v;
   }
   
   private Color darker (Color c) {
	   int d = COLOR_DELTA;
	   Color v = new Color(
			   Math.max(c.getRed()-d, 0), 
			   Math.max(c.getGreen()-d, 0), 
			   Math.max(c.getBlue()-d, 0));
	   return v;
   }
   
   private int brightnessValue (Color c) {
	   int v = c.getRed() + c.getBlue() + c.getGreen();
	   return v;
   }
   
   private int contrast (Color c1, Color c2) {
	   int v = Math.abs(brightnessValue(c1) - brightnessValue(c2));
	   return v;
   }
   
   @Override
   public void updateUI() {
      super.updateUI();
      
      defaultForeground = UIManager.getColor ("Table.foreground"); 
      defaultBackground = UIManager.getColor ("Table.background"); 
      defaultSelectionForeground = UIManager.getColor ("Table.selectionForeground");
      defaultSelectionBackground = UIManager.getColor ("Table.selectionBackground");
   }
}

@Override
public void addActivityListener ( ActivityListener listener ) {
   objectListener.addActivityListener( listener );
}

@Override
public void removeActivityListener ( ActivityListener listener ) {
   objectListener.removeActivityListener( listener );
}

}
