/*
 *  UTFPropertyResourceBundle in org.jpws.front.resource.bundles
 *  file: UTFPropertyResourceBundle.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 28.08.2005
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

import java.io.InputStream;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.jpws.front.Global;
import org.jpws.front.util.PropertiesUTF;
import org.jpws.front.util.ResourceLoader;

/**
 *  Abstract class to load UTF-8 encoded text resource bundles.
 */
public abstract class UTFPropertyResourceBundle extends ResourceBundle
{
   private PropertiesUTF prop = new PropertiesUTF();
   
/**
 * Constructor.
 * @param path filepath to UTF property textfile (this is a resource path!) 
 */
public UTFPropertyResourceBundle ( String path )
{
   InputStream in;

   try {
      in = ResourceLoader.getResourceStream( path );
      prop.load( in, "utf-8" ); 
   }
   catch ( Exception e )
   {
      e.printStackTrace();
      Global.exit( "*** MISSING RESOURCE FILE: " + path, true );
   }
}


   public Enumeration getKeys ()
   {
      return prop.keys();
   }
   protected Object handleGetObject ( String key )
   {
      return prop.getProperty( key );
   }
}
