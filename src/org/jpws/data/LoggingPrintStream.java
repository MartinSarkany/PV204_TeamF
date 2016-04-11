/*
 *  LoggingPrintStream in org.jpws.data
 *  file: LoggingPrintStream.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 15.05.2010
 *  Version
 * 
 *  Copyright (c) 2010 by Wolfgang Keller, Munich, Germany
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

package org.jpws.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * 
 *  LoggingPrintStream provides multiplex function to a logging
 *  print stream. It is intended to substitute the <tt>System.out</tt> 
 *  and <tt>System.err</tt> print streams.
 */

public class LoggingPrintStream extends PrintStream
{
   ByteArrayOutputStream logOut;
   PrintStream logPrint;
   FileOutputStream fOut;

   /** Creates a new logging print stream with
    *  multiplex function. 
    *  
    * @param out <code>PrintStream</code> original print stream
    *        that will be always serviced by this instance
    */
   public LoggingPrintStream ( PrintStream out ) 
   {
      super( out, true );
      logOut = new ByteArrayOutputStream( 2024 );
      logPrint = new PrintStream( logOut, true );
   }

   public void println ()
   {
      super.println();
      if ( logPrint != null )
         logPrint.println();
   }

   public void print ( String x )
   {
      super.print( x );
      if ( logPrint != null )
         logPrint.print( x );
   }

   public void print ( Object obj )
   {
      super.print( obj );
      if ( logPrint != null )
         logPrint.print( obj );
   }
   
   public void println ( Object obj )
   {
      super.println( obj );
      if ( logPrint != null )
         logPrint.println();
   }

   public void println ( String x )
   {
      super.println( x );
      if ( logPrint != null )
         logPrint.println();
   }

   /** Sets the output file for this logging print stream.
    *  Does nothing if such a file has been defined before.
    *  
    * @param f File log file definition
    */
   public void setOutputFile ( File f )
   {
      
      if ( fOut == null )
      synchronized (this)
      {
         try
         {
            fOut = new FileOutputStream( f );
            if ( logOut.size() > 0 )
            {
               logOut.writeTo( fOut );
               logOut.reset();
            }
            logPrint = new PrintStream( fOut, true );
            logOut = null;
         }
         catch ( IOException e )
         {
            e.printStackTrace();
            logPrint = null;
         }
      }
   }
}
