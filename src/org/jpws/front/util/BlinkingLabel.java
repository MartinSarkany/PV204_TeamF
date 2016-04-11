/*
 *  BlinkingLabel in org.jpws.front.util
 *  file: BlinkingLabel.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 25.08.2005
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

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 *  BlinkingLabel in org.jpws.front.util
 */
public class BlinkingLabel extends JLabel implements TimePulseListener
{
   private Color textColor;
   private Icon icon;
   private boolean isOff;
   private boolean blinking;
   
/**
 * 
 */
public BlinkingLabel ()
{
   super();
}

/**
 * @param text
 */
public BlinkingLabel ( String text )
{
   super( text );
}

/**
 * @param text
 * @param horizontalAlignment
 */
public BlinkingLabel ( String text, int horizontalAlignment )
{
   super( text, horizontalAlignment );
}

/**
 * @param image
 */
public BlinkingLabel ( Icon image )
{
   super( image );
}

/**
 * @param image
 * @param horizontalAlignment
 */
public BlinkingLabel ( Icon image, int horizontalAlignment )
{
   super( image, horizontalAlignment );
}

/**
 * @param text
 * @param icon
 * @param horizontalAlignment
 */
public BlinkingLabel ( String text, Icon icon, int horizontalAlignment )
{
   super( text, icon, horizontalAlignment );
}

public void setBlinking ( boolean v )
{
   blinking = v;
   if ( !v )
   {
      if ( textColor != null )
         super.setForeground( textColor );
      if ( icon != null )
         super.setIcon( icon );
      isOff = false;
   }
}

public void setIcon ( Icon i )
{
   if ( isOff )
      icon = i;
   else
      super.setIcon( i );
}

public void setForeground( Color c )
{
   if ( isOff )
      textColor = c;
   else
      super.setForeground( c );
}

/* 
 * Overridden: @see org.jpws.front.util.TimePulseListener#phaseStart(int)
 */
public void phaseStart ( int type )
{
   if ( !blinking )
      return;
   
   if ( type == TimeSlicer.OFF & !isOff )
   {
      textColor = getForeground();
      icon = getIcon();
      super.setForeground( Color.GRAY );
      super.setIcon( null );
      isOff = true;
   }
   else if ( type == TimeSlicer.ON & isOff )
   {
      super.setForeground( textColor );
      super.setIcon( icon );
      isOff = false;
   }
}

}
