/*
 *  TimeSlicer in org.jpws.front.util
 *  file: TimeSlicer.java
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

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  Thread driven service class to issue a time pulse with two phases: ON and OFF.
 *  The length of the two phases may be designed by the user.
 *  If OFF value is set to zero, only the ON phase is issued.  
 */
public class TimeSlicer
{
   public static final int ON = 1;
   public static final int OFF = 0;
   
   private int onDekas, offDekas;
   private Timer timer;
   private ArrayList<TimePulseListener> list;
   private TimerTask task;
   
/**
 *  Creates a time slicer with the parameter phase lengths.
 *  
 *  @param on length of the ON phase in 10ths of a second 
 *  @param off length of the OFF phase in 10ths of a second; 
 *         may be 0 for ignoring the OFF phase 
 */
public TimeSlicer ( int on, int off )
{
   if ( on <= 0 | off < 0 )
      throw new IllegalArgumentException();
   
   onDekas = on;
   offDekas = off;
   
   list = new ArrayList<TimePulseListener>();
   timer = new Timer( true );
   task = new PulseTask();
   timer.scheduleAtFixedRate( task, 0, (on + off) * 100 );
}

public void cancel ()
{
   timer.cancel();
}

public void addTimePulseListener ( TimePulseListener l )
{
   synchronized ( list )
   {
   if ( l != null && !list.contains( l ) )
      list.add( l );
   }
}

public void removeTimePulseListener ( TimePulseListener l )
{
   synchronized ( list )
   {
   if ( l != null )
      list.remove( l );
   }
}

private class PulseTask extends TimerTask
{
   
   public void run ()
   {
      dispatch( ON );
      try { Thread.sleep( onDekas * 100 ); }
      catch ( InterruptedException e )
      {}
      if ( offDekas > 0 )
         dispatch( OFF );
   }
   
   @SuppressWarnings("unchecked")
   private void dispatch ( int type )
   {
      ArrayList<TimePulseListener> copy;
      synchronized ( list )
      {
         copy = (ArrayList<TimePulseListener>)list.clone();
      }
      for ( TimePulseListener li : copy )
         li.phaseStart( type );
   }
}

}
