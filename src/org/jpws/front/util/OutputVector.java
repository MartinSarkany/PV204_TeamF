/*
 *  OutputVector in org.jpws.front.util
 *  file: OutputVector.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 31.12.2006
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

package org.jpws.front.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class offers a multiplexor to writing to output streams.
 * Elements of the stream vector must be subclasses of
 * <code>OutputStream</code>.
 * 
 * <p>The vector can be addressed as a single output stream. 
 * Vector methods of the <code>OutputStream</code> interface  
 * simply hand over execution to all registered element
 * streams. Execution is thread-safe.
 * 
 * @since 0-5-0
 */

public class OutputVector extends OutputStream
{
   private ArrayList<OutputStream> streams = new ArrayList<OutputStream>();

   
public OutputVector () 
{
}

/**
 * Adds an output stream to this stream vector. If the
 * parameter object is already part of this vector, it
 * is not added.
 * 
 * @param out <code>OutputStream</code> to be added
 * @return <b>true</b> if and only if the parameter stream was added
 *         to this vector (returns <b>false</b> if it is already present)
 */
public boolean addStream ( OutputStream out )
{
   synchronized ( streams )
   {
      if ( !streams.contains( out ) )
         return streams.add( out );
      else
         return false;
   }
}

/**
 * Adds an output stream to this stream vector. If the
 * parameter object is already part of this vector, it
 * is not added.
 * 
 * @param out <code>OutputStream</code> to be added
 */
public void removeStream ( OutputStream out )
{
   synchronized ( streams )
   {
      streams.remove( out );
   }
}

/**
 * Returns an element of this stream vector identified by its
 * index position.
 *    
 * @param index int 0..size()-1
 * @return <code>OutputStream</code> 
 * @throws IndexOutOfBoundsException
 */
public OutputStream getStream ( int index )
{
   synchronized ( streams )
   {
      return streams.get( index );
   }
}

/**
 * Removes all elements from this stream vector.
 */
public void clear ()
{
   synchronized ( streams )
   {
      streams.clear();
   }
}

/**
 * Returns an iterator over all elements of this stream vector.
 * 
 * @return <code>Iterator</code> with element type <code>OutputStream</code>
 */
public Iterator<OutputStream> iterator ()
{
   synchronized ( streams )
   {
      return streams.iterator();
   }
}

/**
 * The number of elements in this vector.
 * @return int
 */
public int size ()
{
   return streams.size();
}

public void close () throws IOException
{
   Iterator<OutputStream> it;
   
   synchronized ( streams )
   {
      for ( it = streams.iterator(); it.hasNext(); )
         it.next().close();
   }   
}

public void flush () throws IOException
{
   Iterator<OutputStream> it;
   
   synchronized ( streams )
   {
      for ( it = streams.iterator(); it.hasNext(); )
         it.next().flush();
   }   
}

public void write ( byte[] b, int off, int len ) throws IOException
{
   Iterator<OutputStream> it;
   
   synchronized ( streams )
   {
      for ( it = streams.iterator(); it.hasNext(); )
         it.next().write( b, off, len );
   }   
}

public void write ( byte[] b ) throws IOException
{
   Iterator<OutputStream> it;
   
   synchronized ( streams )
   {
      for ( it = streams.iterator(); it.hasNext(); )
         it.next().write( b );
   }   
}

public void write ( int b ) throws IOException
{
   Iterator<OutputStream> it;
   
   synchronized ( streams )
   {
      for ( it = streams.iterator(); it.hasNext(); )
         it.next().write( b );
   }   
}

}
