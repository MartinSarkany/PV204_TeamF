/*
 *  Reporter in org.jpws.front
 *  file: Reporter.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 22.06.2005
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 *  Reporter in org.jpws.front
 */
public class Reporter
{
   public static final int APPLOG = 1;
   public static final int NULLOG = 0;
   
   private static PrintStream applog;
   private static PrintStream nullPrint;
   
   
   private Reporter ()
   {}
   
   static void init ()
   {
      
      nullPrint = new PrintStream( new NullOutputStream() );
/*      
      try {
      applog = new PrintStream( new FileOutputStream( "jpws-log.txt", false ), 
            true, "us-ascii" );
      }
      catch ( IOException e )
      {
         e.printStackTrace();
         applog = nullPrint;
      }
*/
   }
   
   static void exit ()
   {
      if ( applog != null )
         applog.close();
   }
   
   public static PrintStream getPrintStream ( int stream )
   {
      return nullPrint;
/*      
      switch ( stream )
      {
      case NULLOG : return nullPrint;
      case APPLOG : return applog;
      default: return null;
      }
*/      
   }
   
   private static class NullOutputStream extends OutputStream
   {
      
      public void close () throws IOException
      {
      }
      public void flush () throws IOException
      {
      }
      public void write ( byte[] b, int off, int len ) throws IOException
      {
      }
      public void write ( byte[] b ) throws IOException
      {
      }
      public void write ( int b ) throws IOException
      {
      }
}
   
}
