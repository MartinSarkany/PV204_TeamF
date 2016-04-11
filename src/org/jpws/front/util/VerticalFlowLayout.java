/*
 *  VerticalFlowLayout in org.jpws.front.util
 *  file: VerticalFlowLayout.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 30.07.2007
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * A simple yet convincing VerticalFlowLayout.
 * <p>Features of the Layout:
 * <br>VFILL (Vertical Fill): if <b>true</b> all components will get same height.
 * <br>HFILL (Horizontal Fill): if <b>true</b> all components will get same width.
 * <br>ALIGN (Alignment): orientation of components (LEFT, CENTER, RIGHT).
 * <br>VGAP: int number of pixels between components 
 * 
 * @author Anonymus
 * @author Wolfgang Keller
 */
public class VerticalFlowLayout implements LayoutManager 
{
   public static final int LEFT = 0;
   public static final int CENTER = 1;
   public static final int RIGHT = 2;
   
   private int vgap = 0;
   private int alignment = LEFT;
   private boolean hfill, vfill;

  /**
   * Creates a VerticalFlowLayout with ALIGN=LEFT, VGAP=0 and
   * no "Fill" functions.
   */
  public VerticalFlowLayout() {
    this(0);
  }

  /**
   * Creates a VerticalFlowLayout with ALIGN=LEFT, the specified VGAP and
   * no "Fill" functions.
   */
  public VerticalFlowLayout(int vgap) {
     this.vgap = Math.max( 0, vgap );
  }

  /**
   * Creates a VerticalFlowLayout with ALIGN=LEFT, VFILL=false and
   * the specified VGAP and HFILL.
   */
  public VerticalFlowLayout( int vgap, boolean horizontalFill ) {
    this( vgap );
    hfill = horizontalFill;
  }

  /**
   * Sets the Vgap of this layout manager. 
   * @param v Vgap in pixels
   */
  public void setVgap ( int v )
  {
     this.vgap = v;
  }

  /**
   * addLayoutComponent method comment.
   */
  public void addLayoutComponent(String name, Component comp) {
  } 

  /**
   * layoutContainer method comment.
   */
  public void layoutContainer ( Container parent ) 
  {
     Component cps[];
     Insets insets;
     int x, y, w, h, i, stH, newH, numVComp;
     
     cps = parent.getComponents();
     insets = parent.getInsets();
     w = parent.getSize().width - insets.left - insets.right;
     h = parent.getSize().height - insets.top - insets.bottom;
     y = insets.top;
     
     numVComp = 0;
     stH = 0;
     if ( vfill )
     {
        for ( Component c : cps ) 
           if ( c.isVisible() )
              numVComp++;
        stH = numVComp > 0 ? (h - (numVComp-1)*vgap) / numVComp : 0;   
     }
     
     for ( Component c : cps ) 
     {
        if (c.isVisible()) 
        {
           Dimension d = c.getPreferredSize();
           x = insets.left;
           if ( !hfill )
           {
              if ( alignment == RIGHT )
                 x += w - d.width;
              else if ( alignment == CENTER )
                 x += (w - d.width)/2;
           }
           newH = vfill ? stH : d.height; 
           c.setBounds( x, y, 
              hfill ? w : d.width, 
              newH );
           y += newH + vgap;
        } 
     } 
  } 

  /**
   * minimumLayoutSize method comment.
   */
  public Dimension minimumLayoutSize(Container parent) 
  {
    Insets insets = parent.getInsets();
    Component[] cps = parent.getComponents();
    int maxWidth = 0;
    int totalHeight = 0;
    int numComponents = 0;
    for ( Component c : cps ) 
    {
      if (c.isVisible()) {
        Dimension cd = c.getMinimumSize();
        maxWidth = Math.max(maxWidth, cd.width);
        totalHeight += cd.height;
        numComponents++;
      } 
    } 
    Dimension td = new Dimension(maxWidth + insets.left + insets.right, 
                                 totalHeight + insets.top + insets.bottom 
                                 + vgap * numComponents);
    return td;
  } 

  /**
   * preferredLayoutSize method comment.
   */
  public Dimension preferredLayoutSize(Container parent) 
  {
    Insets insets = parent.getInsets();
    Component[] cps = parent.getComponents();
    int maxWidth = 0;
    int totalHeight = 0;
    int numComponents = 0;
    
    for ( Component c : cps ) 
    {
      if (c.isVisible()) {
        Dimension cd = c.getPreferredSize();
        maxWidth = Math.max(maxWidth, cd.width);
        totalHeight += cd.height;
        numComponents++;
      } 
    } 
    Dimension td = new Dimension(maxWidth + insets.left + insets.right, 
                                 totalHeight + insets.top + insets.bottom 
                                 + vgap * Math.max( 0, numComponents-1));
    return td;
  } 

  /**
   * removeLayoutComponent method comment.
   */
  public void removeLayoutComponent(Component comp) {
  } 

  public void setAlignment ( int align )
  {
     if ( align == RIGHT )
        alignment = RIGHT;
     else if ( align == CENTER )
        alignment = CENTER;
     else
        alignment = LEFT;
  }
  
  public void setHorizontalFill ( boolean v )
  {
    hfill = v;
  }
  
  public void setVerticalFill ( boolean v )
  {
    vfill = v;
  }
}