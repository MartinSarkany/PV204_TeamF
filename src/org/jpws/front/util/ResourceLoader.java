/*
 *  ResourceLoader in org.jpws.front.util
 *  file: ResourceLoader.java
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

package org.jpws.front.util;

/**
 *  Multilanguage text resources, image resources.
 * 
 *  @author Wolfgang Keller
 */
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.jpws.front.Global;

public class ResourceLoader
{
   
public static String[][] substTokens;

private static List<String> resPaths = new LinkedList<String>(); 
private static Hashtable<String, ResourceBundle> bundleTable = new Hashtable<String, ResourceBundle>();
private static Properties imageTable;
private static URL defaultFailImage;

private ResourceLoader()
{}

public static void setTokens ( String[][] tok )
{
   substTokens = tok; 
}

public static void addResourcePath ( String path )
{
   if ( path != null )
   {
      if ( !path.equals("") && !path.endsWith( "/" ) )
         path += "/";
   
      resPaths.add( path );
   }
}

private static ResourceBundle openBundle ( String bundle )
{
   String bundlePath;
   ResourceBundle rbundle = null;
   Iterator<String> it;
   
   for ( it = resPaths.iterator(); it.hasNext() && rbundle == null; )
   {
      bundlePath = it.next() + "bundles/" + bundle;
      try { rbundle = ResourceBundle.getBundle( bundlePath, Global.locale ); }
      catch ( Exception e )
         {
         System.out.println("*** failed bundle search: " + bundlePath ); 
//            e.printStackTrace();
         }
   } 

   if ( rbundle == null )
      Global.exit( "*** MISSING RESOURCE BUNDLE *** : " + bundle, true );
   else 
	  bundleTable.put( bundle, rbundle );
   return rbundle;
}  // openBundle

public static String getString( String bundle, String key )
{
   String hstr;
   ResourceBundle rbundle;

   if ( key == null )
      return "";
   
   rbundle = bundleTable.get( bundle );
   if ( rbundle == null )
      rbundle = openBundle( bundle );

   try {
      hstr = rbundle.getString( key );
      }
   catch (Exception ex)
      {
         return "FIXME";
      }
/*
// interpret global variables
if ( hstr.indexOf( '$' ) > -1 )
   {
   for ( i = 0; i < substTokens.length; i++ )
   hstr = Functions.substituteText( hstr, substTokens[i][0], substTokens[i][1] );
   }
*/
return hstr;
}  // getString

public static String getCommand ( String key )
{
   return getString( "action", key );
}  

public static String getDisplay ( String key )
{
   return getString( "display", key );
}  

public static String getMessage ( String key )
{
   return getString( "message", key );
}  

/** Tries to interpret <code>key</code> as string token of resource bundle "message"
*  but renders the input if no such code is defined.
* @return the bundle contained text, if <code>key</code> was a token;
*         <code>key</code> as is, otherwise.
*/
public static String codeOrRealMsg ( String key )
{
String text = getString( "message", key );
if ( text.equals( "FIXME" ) )
   text = key;
return text;
}  // codeOrRealMsg


/** Tries to interpret <code>key</code> as string token of resource bundle "display"
*  but renders the input if no such code is defined.
* @return the bundle contained text, if <code>key</code> was a token;
*         <code>key</code> as is, otherwise.
*/
public static String codeOrRealDisplay ( String key )
{
String text = getString( "display", key );
if ( text.equals( "FIXME" ) )
   text = key;
return text;
}  // codeOrRealMsg

// *****************  IMAGES SECTION  *************************

public static void init ( String resourcePath )
{
   if ( resourcePath != null )
      ResourceLoader.addResourcePath( resourcePath );
   
   getString("action","testvalue");
// getString("message","testvalue");
   getString("display","testvalue");
 
   imageTable = new Properties();
   try {
       imageTable.load( getResourceStream( "#standards/imagemap.properties" ));
       }
   catch ( Exception e )
      {
      Global.exit( "*** MISSING RESOURCE *** : imagemap.properties\r\n" + e, true );
      }

   defaultFailImage = getImageURL("default_fail");
}  // int

//******** FOLLOWS STANDARD RESOURCE RETRIEVAL (file or jar protocol) ***************

public static URL getImageURL ( String token )
{
   String path = imageTable.getProperty( token );
   if ( path == null ) {
      System.err.println("*** missing image association: " + token);
      return null;
   }

   return getResourceURL( path );
}  // getImageURL


public static ImageIcon getImageIcon ( String token )
{
   URL url = getImageURL( token );
   if (url == null) {
      url = defaultFailImage;
   }

   ImageIcon icon = null;
   try {
	   BufferedImage img = ImageIO.read(url);
	   icon = new ImageIcon( img );
	   return icon;
   } catch (IOException e) {
	   e.printStackTrace();
   }
   return new ImageIcon( url );
}

public static Image getImage( String id )
{
   ImageIcon icon = getImageIcon( id );
   return icon == null ? null : icon.getImage();
}

public static ImageIcon getDefaultImageIcon( String id, String failcase )
{
   ImageIcon icon = (ImageIcon) UIManager.getIcon(id);

   if (icon == null) {
      icon = getImageIcon( failcase );
   }
   return icon;
}

public static Image getImage( String id, String failcase )
{
   ImageIcon icon = getDefaultImageIcon( id, failcase );
   return icon == null ? null : icon.getImage();
}


// *****************  RESOURCE HANDLING  *******************

/** 
 * Returns the text resource from a given resource file denotation.
 * 
 * @param path the full path and filename of the resource requested. If
 * <code>path</code> begins with "#" it is resolved against the program's
 * standard resource folder after removing "#"
 * @param enc the character encoding to be applied for reading the file
 *        (if <b>null</b> the platform default is used)
 * @return String text decoded with the given character encoding or <b>null</b>
 *         if the resource couldn't be obtained
 * @throws IOException
 */
public static String getResourceText ( String path, String enc ) throws IOException
{
   InputStream in;
   ByteArrayOutputStream bOut;
   String res = null;
   
   if ( (in = ResourceLoader.getResourceStream( path )) != null )
   {
      bOut = new ByteArrayOutputStream();
      Util.copyStream( in, bOut );
      in.close();
      try { res = bOut.toString( enc ); }
      catch ( Exception e )
      { res = bOut.toString(); }
   }
   return res;
}

/** General use resource InputStream getter.
 * @param path the full path and filename of the resource requested. If
 * <code>path</code> begins with "#" it is resolved against the program's
 * standard resource folder after removing "#"
 * @return an InputStream to read the resource data, or <b>null</b> if the resource
 * could not be obtained
 * @throws java.io.IOException if there was an error opening the input stream
 */
public static InputStream getResourceStream ( String path )
               throws java.io.IOException 
{
   URL url;

   if ((url = getResourceURL(path)) == null)
      return null;
   else
      return url.openStream();
} // getResourceStream

/** General use resource URL getter.
 * @param path the full path and filename of the resource requested. If
 * <code>path</code> begins with "#" it is resolved against the program's
 * standard resource folder after removing "#"
 * @return an URL instance, or <b>null</b> if the resource could not be obtained
 */
public static URL getResourceURL ( String path ) //throws java.io.IOException
{
   URL url = null;
   String rp;

   if ( path == null )
      throw new IllegalArgumentException("path = null");

   if ( path.startsWith("#") )
   {
      for ( int i = 0; i < resPaths.size() && url == null; i++ )
      {
         rp = resPaths.get( i ) + path.substring(1);
         url = ResourceLoader.class.getClassLoader().getResource( rp );
//         url = ClassLoader.getSystemResource( rp );
      } 
   }
   else
      url = ResourceLoader.class.getClassLoader().getResource( path );
//      url = ClassLoader.getSystemResource( path );

   if ( url == null ) {
      System.err.println("*** failed locating resource: " + path);
      return null;
   }

//   System.out.println( "resource: " +url.getProtocol() + ", url:"+url.getPath() );
   return url;
} // getResourceURL

  public static String[] getResourcePaths()
  {
     Object[] a;
     String[] s;
     
     a = resPaths.toArray();
     s = new String[ a.length ];
     for ( int i = 0; i < a.length; i++ )
        s[i] = (String) a[i];
     return s; 
  }

}
