/*
 *  Test_PropertyFiles in org.jpws.front.util
 *  file: Test_PropertyFiles.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 14.09.2005
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 *  Test_PropertyFiles in org.jpws.front.util
 */
public class Test_PropertyFiles
{


public static void main ( String[] args )
{
   Properties master, slave;
   File mf, sf;
   InputStream in;
   Enumeration<?> en;
   String key, path1, path2;
   int missing, additional;
   boolean masterIsUtf, slaveIsUtf;
   
   if ( args.length < 2 )
   {
      System.out.println( "*** Not enough parameters! ***" );
      return;
   }
   
   try {
      path1 = args[0];
      masterIsUtf = path1.startsWith( "[utf]:" );
      if ( masterIsUtf )
         path1 = path1.substring( 6 );
      
      path2 = args[1];
      slaveIsUtf = path2.startsWith( "[utf]:" );
      if ( slaveIsUtf )
         path2 = path2.substring( 6 );
      
      
      // prepare files
      mf = new File( path1 ).getCanonicalFile();
      sf = new File( path2 ).getCanonicalFile();
      
      System.out.println( "*** COMPARING PROPERTY FILES ***" );
      System.out.println( "File Master: " + mf.getPath() );
      System.out.println( "File Slave: " + sf.getPath() );

      // load master
      in = new FileInputStream( mf );
      if ( masterIsUtf )
      {
         master =  new PropertiesUTF();
         ((PropertiesUTF)master).load( in, "utf-8" );
      }
      else
      {
         master =  new Properties();
         master.load( in );
      }
      in.close();
      
      // load slave
      in = new FileInputStream( sf );
      if ( slaveIsUtf )
      {
         slave =  new PropertiesUTF();
         ((PropertiesUTF)slave).load( in, "utf-8" );
      }
      else
      {
         slave =  new Properties();
         slave.load( in );
      }
      in.close();
      
      // check for missing in slave
      missing = 0;
      for ( en = master.keys(); en.hasMoreElements(); )
      {
         key = (String)en.nextElement();
         if ( !slave.containsKey( key ) )
         {
            missing++;
            if ( missing == 1 )
               System.out.println( "*** Found missing keys in Slave:" );
            System.out.println( "   " + key + " = " + (String)master.get( key ) );
         }
      }
      
      // check for additional in slave
      additional = 0;
      for ( en = slave.keys(); en.hasMoreElements(); )
      {
         key = (String)en.nextElement();
         if ( !master.containsKey( key ) )
         {
            additional++;
            if ( additional == 1 )
               System.out.println( "*** Found additional keys in Slave:" );
            System.out.println( "   " + key );
         }
      }
      
      System.out.println( "*** Terminating investigation" );
      if ( missing == 0 & additional == 0 )
         System.out.println( "*** Congrats, no problems found!" );
         
   }
   catch ( IOException e )
   {
      System.out.println( "*** IO-Error: "  );
      e.printStackTrace();
      return;
      
   }
   
   
}

}
