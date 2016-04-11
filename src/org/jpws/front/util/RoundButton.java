/*
 *  RoundButton in org.jpws.front.util
 *  file: RoundButton.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 21.04.2012
 *  Version
 * 
 *  Copyright (c) 2012 by Wolfgang Keller, Munich, Germany
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

/***********************************************************************************
 * RoundButton                                                                     *
 ***********************************************************************************
 * A class which creates a round JButton. The button reacts only if the mouse      *
 * cursor is positioned over the round shape of the button instead of the bounding *
 * rectangle of a normal JButton.                                                  *
 * The icon of the button switches from the inactive icon to the active icon once  *
 * the mouse cursor is positioned over the round shape of the button.              * 
 ***********************************************************************************
 * (c) Impressive Artworx, 2k8                                                     * 
 * @author Manuel Kaess                                                            *
 * @version 1.0                                                                    *
 ***********************************************************************************
 *  This program is free software: you can redistribute it and/or modify           *
 *  it under the terms of the GNU General Public License as published by           *
 *  the Free Software Foundation, either version 3 of the License, or              *
 *  (at your option) any later version.                                            *
 *                                                                                 *
 *  This program is distributed in the hope that it will be useful,                *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of                 *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                  *
 *  GNU General Public License for more details.                                   *
 *                                                                                 *
 *  You should have received a copy of the GNU General Public License              *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.          *
 ***********************************************************************************/ 

public class RoundButton extends JButton
{
    private Shape shape;
    
    /**
     * Constructor with only one image. This image will be used as active and inactive icon.
     * 
     * @param activeIcon the image for active and inactive state
     */
    public RoundButton(Icon activeIcon) 
    {
        this(activeIcon, activeIcon);
    }
    
    /**
     * Constructor with all parameters.
     * 
     * @param text the button's text
     * @param activeIcon the image which will be shown on mouse over event
     * @param inactiveIcon the image that will otherwise be shown
     */
    public RoundButton(Icon activeIcon, Icon inactiveIcon) 
    {
        setIcon(inactiveIcon);
        setRolloverIcon(activeIcon);
        setPressedIcon(activeIcon);
        
        int iw = Math.max(inactiveIcon.getIconWidth(), inactiveIcon.getIconHeight());
        int sw = 1;
        // create a one pixel wide border around the button
        setBorder(BorderFactory.createEmptyBorder(sw, sw, sw, sw));
        // the dimension is the image size plus a one pixel border
        Dimension dim = new Dimension(iw+sw*2, iw+sw*2);
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setBackground(Color.BLACK);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setAlignmentY(Component.TOP_ALIGNMENT);
        // the round shape of the button must be set in order to handle mouse moves correctly
        initShape();
    }
    
    protected void initShape() 
    {
        Dimension s = this.getPreferredSize();
        shape = new Ellipse2D.Float(0, 0, s.width-1, s.height-1);
    }
    
    @Override
    protected void paintBorder(Graphics g) 
    {
        initShape();
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.draw(shape);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    
    @Override
    public boolean contains(int x, int y) 
    {
        /*
         *  This method returns true, if the mouse cursor is positioned inside the circle.
         *  Otherwise it returns false.
         */
        if(shape == null) initShape();
        return shape.contains(x, y);
    }
}