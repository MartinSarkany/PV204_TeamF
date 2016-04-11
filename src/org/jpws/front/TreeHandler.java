/*
 *  TreeHandler in org.jpws.front
 *  file: TreeHandler.java
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

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jpws.data.Options;
import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ResourceLoader;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.order.Collatable;
import org.jpws.pwslib.order.DefaultRecordWrapper;
import org.jpws.pwslib.order.OrderedListEvent;
import org.jpws.pwslib.order.OrderedListListener;
import org.jpws.pwslib.order.OrderedRecordList;

/**
 *  Defines a <code>TreeModel</code> and organises the display of a 
 * <code>JTree</code> relating to the content of a <code>PwsFileContainer</code>. 
 * 
 * @author Wolfgang Keller
 */
public class TreeHandler implements TreeSelectionListener,
                           ContainerView, OrderedListListener
{
    private PwsFileContainer owner;
    
    private ExpansionMemory treeExpansionMemory;
    private ObjectChangeListener objectListener = new ObjectChangeListener(this);   

	private JTree tree;
    private DefaultTreeModel treeModel;
	private PwsTreeNode rootNode;
    private OrderedRecordList sortList;
    private DefaultTreeCellRenderer renderer;

    /**
	 * @param owner PwsFileContainer
	 */
	public TreeHandler( PwsFileContainer owner )
	{
      this.owner  = owner;
      
      init();
	}

   private void init()
   {
      String info;
      

      sortList = owner.getOrderedList();
      sortList.addOrderedListListener( this );

      treeModel = new DefaultTreeModel(null);
      tree  = new JTree( treeModel );
      tree.setName( "Main Data-Tree" );
      renderer = new TreeCellRenderer();
      renderer.setLeafIcon( ResourceLoader.getImageIcon("treeleaf-pass") );
      treeExpansionMemory = new ExpansionMemory();
      objectListener.registerChangeableObject( tree );

      tree.setDragEnabled( true );
      tree.setTransferHandler( owner.getTransferHandler() );
      
      tree.setCellRenderer( renderer );
      tree.setBorder( new EmptyBorder( 3, 6, 3, 0 ) );
      tree.setRootVisible( true );
      tree.setEditable( false );
      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
      tree.addTreeSelectionListener( this );
      tree.addMouseListener( owner.getMouseAdapter() );
      tree.setExpandsSelectedPaths( true );
      tree.addTreeExpansionListener( treeExpansionMemory );
      
      tree.getInputMap().put( KeyStroke.getKeyStroke(
            KeyEvent.VK_A, ActionEvent.CTRL_MASK), "none" );
      tree.getInputMap().put( KeyStroke.getKeyStroke(
            KeyEvent.VK_X, ActionEvent.CTRL_MASK), "none" );
      
      // initial 
      rootNode = createTree( sortList );
      treeModel.setRoot( rootNode );
      
      // install persistent expanded branches info if available
      info = owner.getTreeExpansionInfo();
      if ( info != null ) {
         installTreeExpansionInfo( info );
      }
   }  // init

   /**
    * Ensures the destruction of object references when this tree handler is
    * no more needed.
    *
    */
   public void destruct ()
   {
      owner = null;
      rootNode = null;
      tree = null;
      treeModel = null;
      sortList = null;
   }

	/**
	 * @return Returns the rootNode.
	 */
	public DefaultMutableTreeNode getRootNode()
	{
		return rootNode;
	}

    @Override
	public boolean hasUserSelection ()
    {
       return getSelectedNode() != null;
    }
    
    /** Returns the tree expansion info string that
     * serves for persistent state.
     * 
     * @return String
     */
    public String getTreeExpansionInfo ()
    {
       String info = treeExpansionMemory.getTreeExpansionString();	
       Log.log( 8, "(TreeHandler.getTreeExpansionInfo) return info: ".concat( info ) );
       return info;
    }
    
    /** Imposes the given node set expansion info onto the entire structure
     *  of the tree.
     *  
     *  @param info String, coded expansion information for tree nodes
     */
    @SuppressWarnings("unchecked")
   public void installTreeExpansionInfo ( final String info )
    {
       if ( info == null | tree == null ) return;
       
       Runnable r = new Runnable() 
       {
         @Override
         public void run ()
         {
            PwsTreeNode node;
            Enumeration<PwsTreeNode> en;
            int i;
   
            synchronized ( tree ) {
               Log.log( 8, "(TreeHandler.installTreeExpansionInfo.run) installing new tree expansion: ".concat( info ) );
               treeExpansionMemory.clear();
               node = (PwsTreeNode)treeModel.getRoot();
               tree.collapsePath(node.getTreePath());
               for ( en = node.breadthFirstEnumeration(), i = 0; 
                     en.hasMoreElements() & i < info.length(); )
               {
                  node = en.nextElement();
                  if ( node.isDir ) {
                     if ( info.charAt( i ) == '1' ) {
                        tree.expandPath( node.getTreePath() );
                     }
                     i++;
                  }
               }
            }
//            expansionString = info;
            Log.log( 8, "(TreeHandler.installTreeExpansionInfo.run) ready /w tree expansion: " );
         }
      };
      ActionHandler.executeOnEDT( r );
    }

    /** Marks the current state of the tree expansion memory. If there exists a
     * recent mark without reset, this does nothing!
     */
    public void markTreeExpansionState ()
    {
       treeExpansionMemory.mark();
    }
    
    /** Resets tree expansion memory to the latest marked version.
     * (Does nothing if there has been no call to <tt>markExpansionState()</tt>.)
     */
    public void resetTreeExpansionState ()
    {
       treeExpansionMemory.reset();
    }
    
   /** Recreates the actual tree expansion state from internal expansion memory. 
    */ 
    @SuppressWarnings("unchecked")
   private void refreshTreeExpansion ()
    {
       Runnable r = new Runnable() 
       {
          @Override
          public void run ()
          {
             synchronized ( tree ) {
            	 PwsTreeNode node = (PwsTreeNode)treeModel.getRoot();
                for ( Enumeration<PwsTreeNode> en = node.breadthFirstEnumeration(); 
                	  en.hasMoreElements(); ) {
                	
                   node = en.nextElement();
                   if ( node.isDir ) {
                	   TreePath path = node.getTreePath();
//                      Log.debug( 8, "(TreeHandler.refreshTreeExpansion) investigating: ".concat( path.toString() ) );
                      if ( treeExpansionMemory.knowsExpandedNode( path ) ) {
                         tree.expandPath( path );
//                         Log.debug( 8, "(TreeHandler.refreshTreeExpansion) ** expanding: ".concat( path.toString() ) );
                      }
                   }
                }
                Log.log( 8, "(TreeHandler.refreshTreeExpansion) done" );
             }
          }
       };
       try { ActionHandler.executeOnEDT_Wait( r ); }
       catch ( Exception e )
       { e.printStackTrace(); }
    }
    
    /**
    * Returns the currently selected node or <b>null</b>.
    * 
    * @return DefaultMutableTreeNode
    */
   public DefaultMutableTreeNode getSelectedNode()
   {
      return (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
   }

   /** Returns a tree node for an ordered list record index. 
    */
   private PwsTreeNode findItemNode( int index )
   {
      if ( index > -1 && index < owner.getOrderedList().size() ) {
    	  DefaultRecordWrapper wrap = owner.getOrderedList().getItemAt( index );
         return findItemNode( wrap );
      }
      return null;
   }
   
   private PwsTreeNode findItemNode( DefaultRecordWrapper rec )
   {
      PwsTreeNode node;
      Enumeration<?> en;
      Object obj;
      
      if ( rec != null )
      synchronized ( tree ) {
         // search tree content for record item
         node = (PwsTreeNode)treeModel.getRoot();
         for ( en = node.breadthFirstEnumeration(); en.hasMoreElements(); ) {
            node = (PwsTreeNode)en.nextElement();
            obj = node.getUserObject();
            if ( obj instanceof DefaultRecordWrapper &&
                 ((DefaultRecordWrapper)obj).equals( rec ) ) {
              return node;   
            }
         }
      }
      return null;
   }
   
   /** Returns the node of the tree branch specified by a GROUP value. 
    *  The result is always a directory node. An empty search string ("")
    *  results in the tree root node; a <b>null</b> value returns <b>null</b>.
    *  
    * @param group node search value (may be <b>null</b>)
    * @return <code>PwsTreeNode</code> or <b>null</b> if not found
    */ 
   private PwsTreeNode findItemNode( String group )
   {
      return group == null ? null :
             ensureNode( rootNode, group, false );
   }
   
   private PwsTreeNode createTree ( OrderedRecordList list )
   {
      DefaultRecordWrapper wrap;
      PwsTreeNode     root, node;
      String actGrp, prevGrp, hstr;
      int idx;
      
      root = new PwsTreeNode( "" );
      actGrp = "";
      node = root;
      
      for ( idx = 0; idx < list.size(); idx++ ) {

    	  // acquire values for next item 
         prevGrp = actGrp;
         wrap = list.getItemAt( idx );
         actGrp = wrap.getGroup();
         
         // prepare parent node
         // if group changed against previous
         if ( !actGrp.equals( prevGrp ) ) {
            // if new group is extension to previous
            if ( actGrp.startsWith( prevGrp.concat( "." )) ) {
               hstr = actGrp.substring( prevGrp.length() +1 );
               node = ensureNode( node, hstr, true );
            }
            // if new group is other
            else {
               node = ensureNode( root, actGrp, true );
            }
         }
               
         // insert node into tree
         node.add( new PwsTreeNode( wrap ) );
      }
      
      return root;
   }
   
   /**
    * Removes a node from the tree and recursively removes all predecessors
    * which have no child as a result of this action.
    * 
    * @param node <code>DefaultMutableTreeNode</code>, the node to be removed
    */
   private void removeNode ( PwsTreeNode node )
   {
	  Log.log(10, "(TreeHandler.removeNode) remove: " + node); 
      if ( node == rootNode ) {
         rootNode = null;
         treeModel.setRoot( null );
      }
         
      else if ( node != null ) {
         if ( node.isDir ) {
            treeExpansionMemory.removeNode( node.getTreePath() );
         }
         
         PwsTreeNode parent = (PwsTreeNode)node.getParent();
         parent.removeChild( node );
         if ( parent.getChildCount() == 0 ) {
            removeNode( parent );
         }
      }
   }

   /** 
    * Realises or identifies a directory element in the tree.
    * Returns the directory node named <code>group</code> under the root node
    * <code>root</code>. Nodes are created as necessary to match the node
    * pattern explicit in the group name if they don't exist. 
    * 
    * @param root   the root node where the search starts.
    * @param group  the node to look for/create; an empty string or a list of node
    * names, separated by 'dots', e.g. "bank.online".  
    * @param create whether a new node shall be created for missing path elements;
    *               if <b>false</b> this function works only for identification
    *               and renders <b>null</b> if <code>group</code> is not found
    * 
    * @return The node named <code>root.toString() + group</code> (as a placed 
    *         directory element in the tree). If <code>group</code> is empty, 
    *         <code>root</code> is returned by itself.
    */
   @SuppressWarnings("unchecked")
   private PwsTreeNode ensureNode( PwsTreeNode root, String group, boolean create )
   {
      PwsTreeNode              node, temp;
      String[]                 tokens;
      String                   token;

      if ( group == null ) {
         group = "";
      }
      
      tokens = group.split( "[.]" );
      node  = root;
      temp = null;

      for ( int i = 0; i < tokens.length; i++ ) {
         // get next group node name
         token = tokens[ i ];
         temp = null;
         if ( token.isEmpty() ) continue;
         
         // search for matching node
         for ( Enumeration<PwsTreeNode> en = node.children(); en.hasMoreElements(); ) {
            temp = en.nextElement(); 
            if ( temp.isDir && temp.toString().equals( token ) )
               break;
            temp = null;
         }

         // create new node if not found
         if ( temp == null ) {
            if ( create ) {
               temp  = new PwsTreeNode( new DirectoryObject( token ) );
               node.insertDirectory( temp );
            }
            else return null;
         }
         
         // set child node to next investigated
         node  = temp;
      }

      return node;
   }  // ensureNode

// ********  IMPLEMENTATIONS OF INTERFACES  ********************   
   
   @Override
   public JComponent getView ()
   {
      return tree;
   }

   @Override
   public int getFirstSelected ()
   {
	  int[] items = getSelectedItems();
      return items.length == 0 ? -1 : items[0];
   }

   private int indexForNode ( DefaultMutableTreeNode node )
   {
      Object userObj;

      if ( node == null || 
            !((userObj=node.getUserObject()) instanceof DefaultRecordWrapper) )
          return -1;

       return owner.getOrderedList().indexOf( (DefaultRecordWrapper)userObj );
   }
   
   @Override
   public int[] getSelectedItems ()
   {
      TreePath select[];
      DefaultMutableTreeNode  node;
      int i, val, count, results[], res2[];
      
      synchronized ( tree ) {
         select = tree.getSelectionPaths();
         results = select != null ? new int[ select.length ] : 
                   new int[0];
   
         for ( i = 0, count = 0; i < results.length; i++ ) {
            node = (DefaultMutableTreeNode) select[i].getLastPathComponent();
            if ( (val = indexForNode( node )) > -1 ) {
               results[ count++ ] = val;  
            }
         }
         if ( count == results.length ) {
            return results;
         }
         
         res2 = new int[ count ];
         System.arraycopy( results, 0, res2, 0, count );
         return res2;
      }
   }  // getSelectedItems
   
   @Override
   public void setSelectedIndex ( int index )
   {
      int[] sel = null;
      if ( index > -1 ) {
         sel = new int[1];
         sel[0] = index;
      }
      setSelectedItems( sel );   
   }  // setSelectedIndex

   
   @Override
public DefaultRecordWrapper[] getSelectedWrappers ()
   {
      DefaultRecordWrapper[] result;
      int i, items[];
      
      synchronized ( tree ) {
         items = getSelectedItems();
         result = new DefaultRecordWrapper[ items.length ];
         for ( i = 0; i < items.length; i++ )
         {
            result[ i ] = sortList.getItemAt( items[ i ] );
         }
         return result;
      }
   }
   
   @Override
   public void setSelectedWrappers ( DefaultRecordWrapper[] selection )
   {
      int len = selection == null ? 0 : selection.length;
      int[] items = new int[ len ];

      for ( int i = 0; i < len; i++ ) {
         items[ i ] = sortList.indexOf( selection[ i ] );
      }
      setSelectedItems( items );
   }
   
   /** Sets a group tree node to all expanded or all collapsed. This will
    *  cause all subdirectories of a branch to adopt the specified state.
    * 
    *  @param group GROUP field name for an existing record group;may be <b>null</b>
    *  @param v value <b>true</b> == expanded, <b>false</b> == collapsed
    */ 
   public void setExpanded ( final String group, final boolean v )
   {
      Runnable r = new Runnable () 
      {
         @Override
		public void run ()
         {
            PwsTreeNode node;
            TreePath path;
            String childGroup;
            DirectoryObject obj;
            int i;
   
            if ( group != null && (node = findItemNode( group )) != null )
            {
               // get the tree path
               path = node.getTreePath();
      
               // expand the specified group node (if opted)
               if ( v )
                  tree.expandPath( path );
               
               // recurse into children directories
               for ( i = 0; i < node.dirCount; i++ )
               {
                  obj = (DirectoryObject) ((PwsTreeNode)node.getChildAt( i )).getUserObject();
                  childGroup = group + "." + obj.name;
                  setExpanded( childGroup, v );
               }
      
               // collapse the specified group node (if opted)
               if ( !v )
                  tree.collapsePath( path );
               
               // root node must be expanded
               if ( node == rootNode )
               {
                  tree.expandPath( path );
   //                  setSelectedIndex( -1 );
               }
            }
            }
      };
//      synchronized ( tree )
      {
         // wait for action to happen
         try
         { ActionHandler.executeOnEDT_Wait( r ); }
         catch ( Exception e )
         { e.printStackTrace(); }
      }
   }  // setExpanded
   
   private PwsTreeNode nodeForPath ( TreePath path )
   {
      return (PwsTreeNode)path.getLastPathComponent();
   }
   
   private TreePath pathForIndex ( int index )
   {
      PwsTreeNode node;
      
      if ( (node = findItemNode( index )) != null )
         return node.getTreePath();
      return null;
   }
   
   @Override
public void setSelectedItems ( final int[] selection )
   {
      Runnable r = new Runnable ()
      {
         @Override
		public void run ()
         {
            List<TreePath> list = new ArrayList<TreePath>();
            TreePath  path, paths[];
            int i;
      
            synchronized ( tree )
            {
               tree.clearSelection();
               if ( selection != null && selection.length > 0 ) {
                  for ( i = 0; i < selection.length; i++ ) {
                     if ( (path = pathForIndex( selection[i] )) != null )
                        list.add( path );
                  }
                  
                  paths = list.toArray( new TreePath[0] );
                  tree.setSelectionPaths( paths );
         
                  // scroll to first tree row of selection
                  if ( paths.length > 0 )
                     tree.scrollRowToVisible( tree.getSelectionRows()[0] );
               }
            }
         }
      };
      // wait for selection to happen
      try
      { ActionHandler.executeOnEDT_Wait( r ); }
      catch ( Exception e )
      { e.printStackTrace(); }
   }  // setSelectedItems
   
   @Override
   public void setSelectAll ()
   {
      Runnable r = new Runnable () {
         @Override
		 public void run () {
            synchronized ( tree ) {
               int size = owner.getOrderedList().size();
               if ( size > 0 ) {
                  tree.clearSelection();
                  for ( int i = 0; i < size; i++ )
                     tree.addSelectionPath( pathForIndex( i ) );
               }
            }
         }
      };
      
      // wait for selection to happen
      try
      { ActionHandler.executeOnEDT_Wait( r ); }
      catch ( Exception e )
      { e.printStackTrace(); }
   }

   @Override
   public void scrollToVisible ( final int index )
   {
      Runnable r = new Runnable () {
         @Override
		 public void run () {
            synchronized ( tree ) {
               tree.scrollRowToVisible( tree.getRowForPath( pathForIndex( index )));
            }
         }
      };
      
      // wait for scroll to happen
      try
      { ActionHandler.executeOnEDT_Wait( r ); }
      catch ( Exception e )
      { e.printStackTrace(); }
   }
      
   /**
    * Handles selection of a tree node.
    * 
    * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
    */
   @Override
   public void valueChanged( TreeSelectionEvent e )
   {
      if ( e.isAddedPath() ) {
         controlSelection( e );
      }
      owner.reportPropertyChange( PwsFileContainer.SELECTION_STATUS );
      ActionHandler.resetIdleTime();
   }
   
   /**
    * Purposefully deselects selected rows in order to ensure the
    * special selection topology valid for our tree.
    *   
    * @param e <code>TreeSelectionEvent</code>
    * since 0-5-0
    */
   private void controlSelection ( TreeSelectionEvent e )
   {
      ArrayList<TreePath> list = new ArrayList<TreePath>();
      TreePath selected[], removals[], path, oldLead;
      DefaultMutableTreeNode  node;
      int i;
      boolean isGroup;

      synchronized ( tree )
      {
         // get the selection parameters
         oldLead = e.getOldLeadSelectionPath();
         selected = e.getPaths();
         
         // if there is only one path selected, give up control
         if ( tree.getSelectionCount() == 1 ) 
            return;
         
         // determine type of operation (GROUP or LEAF)
         // (the type is primarily determined as the type of the previous selection,
         // if this is absent, as the type of the first newly selected paths)
         if ( oldLead == null )
            oldLead = selected[0];
         isGroup = !((DefaultMutableTreeNode)oldLead.getLastPathComponent()).isLeaf();
   //      System.out.println( "-- control selection: detected selection type is " + (isGroup ? "GROUP" : "LEAF") 
   //            + "\r\n   anckor  == " + oldLead == null ? "NEW SELECTION" : oldLead.toString() );
         
         if ( isGroup )
         {
            // for GROUP type remove all added selections
   //         System.out.println( "-- control selection: removing " + selected.length + " ANY nodes" );
            removals = selected;
         }
         else
         {
            // for LEAF type remove all non-leaf selection paths
            for ( i = 0; i < selected.length; i++ )
            {
               path = selected[i];
               node = (DefaultMutableTreeNode) path.getLastPathComponent();
               if ( !node.isLeaf() )
                  list.add( path );
            }
            removals = list.toArray( new TreePath[ list.size() ]);
   //         System.out.println( "-- control selection: removing " + removals.length + " GROUP nodes" );
         }
   
         // perform removing 
         tree.removeSelectionPaths( removals );
      }
   }  // controlSelection

   @Override
   public void orderedListPerformed ( final OrderedListEvent evt )
   {
      Runnable r = new Runnable() 
      {
         PwsTreeNode node;
         DefaultRecordWrapper record = evt.getRecord();
         int eventType;

         @Override
		 public void run()
         {
            if ( tree != null & treeModel != null )
            synchronized ( tree )
            {
               eventType = evt.getType();
               Log.log( 7, "(TreeHandler.orderedListPerformed) Event = ".concat( String.valueOf(eventType)) );
               
               if ( eventType == OrderedListEvent.LIST_RELOADED ) {
                  rootNode = createTree( (OrderedRecordList)evt.getSource() );
                  treeModel.setRoot( rootNode );
                  tree.revalidate();
                  tree.repaint();
   //System.out.println( "- Treehandler: LIST RELOADED" );               
                  if ( Options.isOptionSet( "autoExpandTree" ) )
                     setExpanded( "", true );
                  else
                     refreshTreeExpansion();
               }
               
               else if ( eventType == OrderedListEvent.LIST_CLEARED ) {
                  rootNode = null;
                  treeModel.setRoot( null );
                  tree.revalidate();
                  tree.repaint();
               }
               
               else if ( eventType == OrderedListEvent.ITEM_UPDATED ) {
                   node = findItemNode( record );
                   DefaultRecordWrapper oldRec = (DefaultRecordWrapper)node.getUserObject();
                   
                   // if group sorting position changed
                   if ( !record.getGroup().equals(node.getGroup()) ) {
                	   removeNode( node );
                	   insertNode( record );
                   }
                   node.setUserObject( record );
                   treeModel.nodeChanged(node);
                }
                
               else if ( eventType == OrderedListEvent.ITEM_REMOVED ) {
                  node = findItemNode( record );
                  removeNode( node );
               }
               
               else if ( eventType == OrderedListEvent.ITEM_ADDED ) {
                  if ( rootNode == null ) {
                     rootNode = createTree( (OrderedRecordList)evt.getSource() );
                     treeModel.setRoot( rootNode );
                     refreshTreeExpansion();
                     tree.revalidate();
                     tree.repaint();

                  } else {
                	 insertNode( record );
                  }
               }
            }
         }
      };
      
      try { ActionHandler.executeOnEDT_Wait( r ); }
      catch ( Exception e )
      { e.printStackTrace(); }
   }
   
   private void insertNode ( DefaultRecordWrapper wrap ) {
	   Log.log(10, "(TreeHandler.insertNode) insert: " + wrap.getSortValue() ); 
	   PwsTreeNode node = new PwsTreeNode( wrap );
	   PwsTreeNode parent = ensureNode( rootNode, wrap.getRecord().getGroup(), true ); 
       parent.insertLeaf( node );
       TreePath path = parent.getTreePath();
       if ( treeExpansionMemory.knowsExpandedNode( path ) ) {
          tree.expandPath( path );
          Log.debug( 8, "(TreeHandler.orderedListPerformed) ** expanding: "
                .concat( path.toString() ) );
       }
   }
   
   @Override
   public void reconstruct ()
   {
      sortList = owner.getOrderedList();
      sortList.addOrderedListListener( this );
      sortList.loadDatabase( owner.getPwsFile(), Options.getLongOption( "expireScope" ) );
   }

   @Override
   @SuppressWarnings("unchecked")
   public void repaint ()
   {
       // this is required because node display modus might have changed (e.g. names)
       PwsTreeNode node = (PwsTreeNode)treeModel.getRoot();
       for ( Enumeration<PwsTreeNode> en = node.breadthFirstEnumeration(); 
    		 en.hasMoreElements(); ) {
          treeModel.nodeChanged( en.nextElement() );
       }
       tree.revalidate();
       tree.repaint();
   }

      
   public void updateUI ()
   {
//      renderer.updateUI();
//      tree.updateUI();
//      tree.revalidate();
//      repaint();
   }
      

   @Override
   public void setFont ( Font font )
   {
      renderer.setFont( font );
      tree.setFont( font );
   }

   @Override
   public void addActivityListener ( ActivityListener listener )
   {
      objectListener.addActivityListener( listener );
   }

   @Override
   public void removeActivityListener ( ActivityListener listener )
   {
      objectListener.removeActivityListener( listener );
   }

//  **************  INNER CLASSES  *******************   
   
public class PwsTreeNode extends DefaultMutableTreeNode
{
   boolean isDir;
   int dirCount;
   
   /**
    * Constructs a new tree node with the given user object.
    *  
    * @param userObject user object which this node represents
    */
   public PwsTreeNode ( Object userObject ) {
      super( userObject );
   }

   /** Returns the GROUP name belonging to this tree node.
    * 
    * @return String
    */
   public String getGroup() {
	  PwsTreeNode dir = isDir ? this : (PwsTreeNode)getParent();
	  return dir.getPathName();
   }

/** Inserts the parameter node as a child of this node and as a DIRECTORY node . */
   public void insertDirectory ( PwsTreeNode node ) {
	  int i;
      for ( i = 0; i < dirCount; i++ ) {
    	 PwsTreeNode inode = (PwsTreeNode)getChildAt( i );
         if ( node.compareTo( inode ) < 0 )
            break;
      }
      treeModel.insertNodeInto( node, this, i );
      node.isDir = true;
      dirCount++;
   }

   /** Inserts the parameter node as a child of this node and as a LEAF node . */
   public void insertLeaf ( PwsTreeNode node ) {
      if ( !node.isLeaf() )
         throw new IllegalArgumentException();
      
      int i;
      for ( i = dirCount; i < getChildCount(); i++ ) {
    	 PwsTreeNode inode = (PwsTreeNode)getChildAt( i );
         if ( node.compareTo( inode ) < 0 )
            break;
      }
      treeModel.insertNodeInto( node, this, i );
   }

   /** Removes the parameter node from the child list of this node. */
   public void removeChild ( PwsTreeNode node ) {
      int index = getIndex( node );;
      if ( index == -1 ) return;
      
      if ( index < dirCount ) {
         dirCount--;
      }
      treeModel.removeNodeFromParent( node );
   }
   
   /** Returns the PWS normalised node name for this tree node.
    *  The primary sense of this is for nodes which represent a GROUP identifier
    *  (hence are DIRECTORY nodes).
    *  The returned value is intended to correspond to a GROUP field value.
    * 
    *  @return String, normalised node name
    */
   public String getPathName () {
      TreeNode[] nodes = getPath();
      
      // return empty string for root node only
      if ( nodes.length == 1 ) return "";
      
      StringBuffer name = new StringBuffer();
      int i;
      for ( i = 0; i < nodes.length; i++ ) {
         name.append( nodes[ i ] );
         name.append( "." );
      }
      
      String hstr = name.toString();
      return hstr.substring( 1, hstr.length() - 1 );
   }

   public TreePath getTreePath () {
      return new TreePath( getPath() );
   }
   
   /**
    * Compares this node's user object to the parameter node's user object.
    * Both objects are compared to their String (<code>toString()</code>) 
    * representation.
    * 
    * @param obj
    * @return 0 = equal; lesser 0 = this is lesser than param; greater 0 =
    *         this is greater than param
    */
   public int compareTo ( PwsTreeNode obj ) {
      return ((Collatable)getUserObject()).getCollationKey().compareTo( 
            ((Collatable)obj.getUserObject()).getCollationKey() );
   }
   
   /** Returns an <code>Iterator</code> over directory nodes 
    *  under this directory node, including any depth subdirectories. 
    *  The set of directories is determined by parameter.
    *  Returns <b>null</b> if this is not a directory node.
    *
    *  @param expanded boolean, if <b>true</b> only expanded directories are listed,
    *         if <b>false</b> all directories are listed
    *  @return <code>Iterator</code> of type <code>PwsTreeNode</code>
    *         or <b>null</b>
    */
   public Iterator<PwsTreeNode> getDescendantDirIterator ( boolean expanded ) {
      Iterator<PwsTreeNode> iter =
      isDir ? new DescendantDirectoryIterator( this, expanded ) : null;
      return iter;   
   }
}  // class PwsTreeNode


/** Class which implements an Iterator of <code>PwsTreeNode</code> objects over 
 *  descendants of the tree node given at construction.
 */
private class DescendantDirectoryIterator implements Iterator<PwsTreeNode>
{
   private ArrayList<PwsTreeNode> list = new ArrayList<PwsTreeNode>();
   private Iterator<PwsTreeNode> iterator;
   private boolean expanded;

   /**
    * Creates a new tree iterator for directory nodes below <tt>n</tt>.
    * 
    * @param n <code>PwsTreeNode</code> root node for this iteration
    * @param expandedOnly boolean, if <b>true</b> only expanded directories are listed,
    *         if <b>false</b> all directories are listed
    */
   public DescendantDirectoryIterator ( PwsTreeNode n, boolean expandedOnly ) {
      this.expanded = expandedOnly;
      addOpenChildren( n );
      iterator = list.iterator();
   }

   @SuppressWarnings("unchecked")
   private void addOpenChildren ( PwsTreeNode n )
   {
      Enumeration<PwsTreeNode> en;
      PwsTreeNode node;
      
      for ( en = n.breadthFirstEnumeration(); en.hasMoreElements(); )
      {
         node = en.nextElement();
         if ( node != n && node.isDir && 
              (!expanded || tree.isExpanded( node.getTreePath())) )
         {
           list.add( node );
           addOpenChildren( node );
         }
      }
   }
   
   @Override
   public boolean hasNext ()
   {
      return iterator.hasNext();
   }

   @Override
   public PwsTreeNode next ()
   {
      return iterator.next();
   }

   @Override
   public void remove ()
   {}
}

/** Our tree cell renderer organises our special node icons 
 *  (currently active for leaf nodes). 
 */
private static class TreeCellRenderer extends DefaultTreeCellRenderer
{
   public TreeCellRenderer () {
      Font font = UIManager.getFont( "Tree.font" );
      setFont( font );
   }
   
   @Override
   public Component getTreeCellRendererComponent ( JTree tree1, Object value,
         boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus1 )
   {
      DefaultMutableTreeNode node;
      DefaultRecordWrapper wrap;
      String icon;
      int expiry, importStatus;
      
      super.getTreeCellRendererComponent( tree1, value, sel, expanded,
            leaf, row, hasFocus1 );

      if ( value == null )
         return this;
      
      node = (DefaultMutableTreeNode)value;
      if ( node.getUserObject() instanceof DefaultRecordWrapper )
      {
         wrap = (DefaultRecordWrapper)node.getUserObject();
         expiry = wrap.getExpiry();
         importStatus = wrap.getImportStatus();
         if ( expiry > 0 | importStatus > 0 )
         {
            icon = "treeleaf-pass";
            if ( importStatus == DefaultRecordWrapper.IMPORTED )
               icon = "treeleaf-pass-imported";
            else if ( importStatus == DefaultRecordWrapper.IMPORTED_CONFLICT )
               icon = "treeleaf-pass-importedconflict";
            else if ( expiry == DefaultRecordWrapper.EXPIRED )
               icon = "treeleaf-pass-expired";
            else if ( expiry == DefaultRecordWrapper.EXPIRE_SOON )
               icon = "treeleaf-pass-expiresoon";
            
            setIcon( ResourceLoader.getImageIcon( icon ) );
         }
      }
      return this;
   }
}

/** 
 * This class constitutes a tree node user object for directory nodes.
 * Directory nodes are all nodes which do not represent database entries.
 * Currently all directory nodes represent GROUP identities of the database.
 * <p>Motivation: This class is required to ensure proper list row collation.  
 */
private static class DirectoryObject implements Collatable
{
   protected String name;
   protected CollationKey key;
   
   /**
    * Constructs a new <code>DirectoryObject</code> representing the 
    * specified node name.
    *      
    * @param name
    */
   public DirectoryObject ( String name ) {
      this.name = name;
      key = Collator.getInstance().getCollationKey( name );
   }
   
   /** Returns the collation key for the name value for this object. */
   @Override
   public CollationKey getCollationKey () {
      return key;
   }
   
   /** Returns the user defined name value for this object. */
   @Override
   public String toString () {
      return name;
   }
}

private class ExpansionMemory implements TreeExpansionListener
{
   private HashMap<String, String> expandMap = new HashMap<String, String>();
   private HashMap<String, String> markedMap;
//   private Thread expansionCalculator; 
   private String expansionString = "";
   private boolean isValidated;

   
   /** Whether the given tree path is known to be an expanded node.
    * 
    * @param path TreePath
    * @return boolean true == node is expanded
    */
   public boolean knowsExpandedNode ( TreePath path )
   {
	  synchronized (expandMap) {
		  return expandMap.containsKey( path.toString() );
	  }
   }

   /** Empties the state memory.
    */
   public void clear () {
	   synchronized (expandMap) {
		   expandMap.clear();
	   }
   }
   
   public void removeNode ( TreePath path ) {
	  boolean removed;
	  synchronized (expandMap) {
		  removed = expandMap.remove( path.toString()) != null;
	  }
      if (  removed ) {
        Log.debug(9, "(TreeExpansionMemory) remove path: ".concat(path.toString()));
      }
   }
   
   /** Marks the current state of the tree expansion memory. If there exists a
    * recent mark without reset, this does nothing!
    */
   @SuppressWarnings("unchecked")
   public void mark ()
   {
      if ( !isMarked() ) {
    	 synchronized (expandMap) {
    	    markedMap = (HashMap<String, String>)expandMap.clone();
    	    getTreeExpansionString();
            Log.debug( 9, "(TreeExpansionListener.mark) marked expansion memory " );
    	 }
	  }
   }
   
   /** Resets this tree expansion memory to the latest marked version.
    * Does nothing if there has been no call to <tt>mark()</tt>.
    */
   public void reset ()
   {
      if ( isMarked() ) {
         expandMap = markedMap;
         markedMap = null;
         Log.debug( 9, "(TreeExpansionListener.reset) reset expansion memory (from mark state)" );
      }
   }
   
   /** Whether the tree expansion state has been marked (pushed to safe).
    * 
    * @return boolean true == state marked
    */
   private boolean isMarked () {
	   return markedMap != null;
   }
   
   @Override
   public void treeExpanded ( TreeExpansionEvent evt )
   {
	  synchronized (expandMap) {
		  // put the event's path to memory
	      String key = evt.getPath().toString();
	      expandMap.put( key, key ); 
//	      Log.debug( 9, "(TreeExpansionListener) put path: ".concat( key ));
	
	      // put all nested expanded paths to memory
	      PwsTreeNode node = nodeForPath( evt.getPath() );
	      for ( Iterator<PwsTreeNode> it = node.getDescendantDirIterator( true ); 
	    		it != null && it.hasNext(); ) {
	         key = it.next().getTreePath().toString();
	         expandMap.put( key, key ); 
//	         Log.debug( 9, "(TreeExpansionListener) put path: ".concat( key ));
	      }
	      
	      // invalidate expansion info string (if not in "marked" state)
	      if ( !isMarked() ) {
	    	 isValidated = false;
	      }
	  }
   }
   
   @Override
   public void treeCollapsed ( TreeExpansionEvent evt )
   {
	  synchronized (expandMap) {
		  // remove the event's path from memory
	      String key = evt.getPath().toString();
	      expandMap.remove( key ); 
//	      Log.debug( 9, "(TreeExpansionListener) remove path: ".concat( key ) );
	      
          // remove all nested paths from memory
	      PwsTreeNode node = nodeForPath( evt.getPath() );
	      for ( Iterator<PwsTreeNode> it = node.getDescendantDirIterator( false ); 
         		it != null && it.hasNext(); ) {
	         key = it.next().getTreePath().toString();
	         expandMap.remove( key );
//	         boolean rem = expandMap.remove( key ) != null;
//	         if ( rem )
//	         Log.debug( 9, "(TreeExpansionListener) remove path: ".concat( key ));
	      }

	      // invalidate expansion info string (if not in "marked" state)
	      if ( !isMarked() ) {
	    	 isValidated = false;
	      }
	  }
   }
   
   public String getTreeExpansionString () {
	   synchronized ( tree ) {
		   if ( !isValidated ) {
			   expansionString = createTreeExpansionString();
			   isValidated = true;
		   }
		   return expansionString;
	   }
   }
   
   /** Creates and returns a new tree expansion string from the current
     * expansion state of the tree.
     * 
     * @return String tree expansion info string
     */
    @SuppressWarnings("unchecked")
   private String createTreeExpansionString ()
    {
       Enumeration<PwsTreeNode> en;
       String hstr = "";

      // calculate new expansion string
	  PwsTreeNode node = (PwsTreeNode)treeModel.getRoot();
	  if ( node != null ) {
	     for ( en = node.breadthFirstEnumeration(); en.hasMoreElements(); ) {
	        node = en.nextElement();
	        if ( node.isDir ) {
	           char h = tree.isExpanded( node.getTreePath() ) ? '1' : '0';
	           hstr += h;
	        }
	     }
	  }
      Log.log( 8, "(TreeHandler.createTreeExpansionInfo) created tree expansion string: ".concat( hstr ) );
      return hstr;
    }
}  // class

}
