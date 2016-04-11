/*
 *  FileOptions in org.jpws.data
 *  file: FileOptions.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 27.11.2005
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

package org.jpws.data;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.jpws.front.util.PropertiesUTF;

class OptionBag implements PersistentOptions
{
   private static final String METADATA_STORETIME = "METADATA.STORETIME";
   private static final String METADATA_TYPE = "METADATA.TYPE";
   private static final String BAGTYPE_MARKER = "OPTIONBAG";
   private static final String LISTMARK = "LIST.";
   
   private PropertiesUTF properties = new PropertiesUTF();
   private Properties defaults = new Properties();
   private String head;
   private long storeTime;
   private boolean modified;

   /** Empty constructor. */
   public OptionBag () {
	   init();
   }
   
   /** Constructs an empty option bag with a set of default options. 
    * 
    * @param defaults <code>Properties</code> containing default value mappings
    */
   public OptionBag ( Properties defaults ) {
	  init();
      setDefaults( defaults );
   }
   
   /** Constructs an OptionBag with a string as input resource.
    * 
    * @param options String option list as a "Properties" text
    * @throws IOException
    */ 
   public OptionBag ( String options ) throws IOException {
	  init();
      load( options );
   }
   
   /** Constructs an OptionBag from an inputstream resource. 
    * 
    * @param options <code>InputStream</code> containing option list as a 
    *        "Properties" text
    * @param charset name of the character set valid for the input stream         
    * @throws IOException
    */ 
   public OptionBag ( InputStream options, String charset ) throws IOException {
	  init();
      load( options, charset );
   }
   
   private void init () {
	  properties.setProperty(METADATA_TYPE, BAGTYPE_MARKER);
   }
   
   /** Sets the default properties for this OptionBag. (A default value for
    * key K becomes the return value of requesting methods of this class when 
    * there is no value mapped for K in the primary list of an instance.)
    * 
    * @param defaults <code>Properties</code>
    */
   public void setDefaults ( Properties defaults ) {
      properties.setDefaults( defaults );
      if ( (this.defaults = defaults) == null ) {
         this.defaults = new Properties(); 
      }
   }
   
   /** Replaces the content of this OptionBag with a Properties list
    *  contained in a string.
    * 
    * @param options String option list as a "Properties" text
    * @throws IOException
    */ 
   public synchronized void load ( String options ) throws IOException {
      ByteArrayInputStream in = new ByteArrayInputStream( options.getBytes( "UTF-8" ) );
      load( in, "UTF-8" );
   }
   
   public void setHeadText ( String text ) {
      head = text;
   }
   
   public void clear () {
      properties.clear();
      init();
      if ( storeTime > 0 ) {
    	  properties.setProperty(METADATA_STORETIME, String.valueOf(storeTime/1000));
      }
      modified = true;
   }
   
   public int countElements () {
	   Enumeration<Object> it = properties.keys();
	   int i = 0;
	   for (; it.hasMoreElements(); ) {
		   String key = (String)it.nextElement();
		   if ( !key.startsWith("METADATA.") ) {
			   i++;
		   }
	   }
	   return i;
   }
   
   @Override
   public boolean isEmpty () {
      return countElements() == 0;
   }
   
   /** Stores all properties from the parameter option bag into this option bag.
    * Existing assignments in this bag are overwritten.
    * (Exempted from this operation is the properties METADATA key.)
    * 
    * @param bag <code>OptionBag</code>, may be null
    */
//   public void addBag ( OptionBag bag ) 
//   {
//	   if ( bag == null ) return;
//	   
//	   Properties ourProp = getProperties();
//	   Iterator <Entry<Object, Object>> it = bag.getProperties().entrySet().iterator();
//	   for (; it.hasNext(); ) {
//		   Entry<Object, Object> entry = it.next();
//		   String key = (String)entry.getKey();
//		   String value = (String)entry.getValue();
//		   
//		   if ( key != METADATA_STORETIME ) {
//			   Object prev = ourProp.put(key, value);
//			   if ( prev != null && !prev.equals(value) ) {
//				   modified = true;
//			   }
//		   }
//	   }
//   }
//   
   public void addBag ( OptionBag bag ) {
	   if ( bag == null ) return;
	   
//	   Properties ourProp = getProperties();
	   Iterator <Entry<Object, Object>> it = bag.getProperties().entrySet().iterator();
	   for (; it.hasNext(); ) {
		   Entry<Object, Object> entry = it.next();
		   String key = (String)entry.getKey();
		   String value = (String)entry.getValue();
		   
		   if ( !key.startsWith("METADATA.") ) {
			   setOption(key, value);
		   }
	   }
   }

   public synchronized void load ( InputStream options, String charset )
      throws IOException {
      properties.load( options, charset );
      String time = properties.getProperty(METADATA_STORETIME);
	  storeTime = time == null ? 0 : new Long(time).longValue() * 1000;
      modified = false;
   }
   
   /** Saves the contents of the primary list of this option bag to the
    *  specified output stream, using the specified character set for encoding.
    *   
    *  @param out OutputStream
    *  @param charset name of the character set valid for the output stream 
    */
   public synchronized void store ( OutputStream out, String charset )
      throws IOException {
      storeInternal( out, charset );
      modified = false;
   }
   
   /** Saves the contents of the primary list of this option bag to the
    *  specified output stream, using the specified character set for encoding.
    *   
    *  @param out OutputStream
    *  @param charset name of the character set valid for the output stream 
    */
   private synchronized void storeInternal ( OutputStream out, String charset )
      throws IOException {
	  long storeTimeSecs = System.currentTimeMillis() / 1000;
	  storeTime = storeTimeSecs * 1000;
	  properties.setProperty(METADATA_STORETIME, String.valueOf(storeTimeSecs)); 
      properties.store( out, head, charset );
   }
   
   /** Returns a byte array representing the mapping contents of this OptionBag.
    * 
    *  @param charset character set definition for the resulting sequence of bytes
    *  @return byte array
    */
   public byte[] toByteArray ( String charset ) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try { 
         storeInternal( out, charset );
         out.close();
         return out.toByteArray();
      }
      catch ( IOException e )
      { throw new IllegalStateException(); }
   }
   
   /** Returns a string representation of all the mappings in this OptionBag.
    *  (The result largely corresponds to the content of a "Properties" file
    *  except that transformations of special characters into escape encodings
    *  are not performed. Instead all characters are rendered in their natural
    *  state (Java UTF).)
    *  
    *  @return <code>String</code> containing key-value mappings
    */
   @Override
   public String toString () {
      try {
    	  return new String( toByteArray( "UTF-8" ), "UTF-8" );
      } catch ( UnsupportedEncodingException e ) { 
    	  return null; 
      }
   }
   
   /** Elementary property function. 
    * @return String mapped value for token or <b>null</b> if not available
    * @throws NullPointerException if parameter is <b>null</b> 
    */
   public String getProperty ( String token ) {
      return properties.getProperty( token );
   }

   /** Returns the <code>Properties</code> object which hold the primary
    * assignments in this option bag.
    * 
    * @return <code>Properties</code>
    */
   protected Properties getProperties () {
	   return properties;
   }
   
   @Override
   public long getStoreTime () {
	   return storeTime;
   }
   
   /** Sets this option bag into MODIFED state.
    */
   public void setModified () {
	   modified = true;
   }
   
   /** Elementary property function. 
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    * */
   public boolean setProperty ( String token, String value ) {
      Object obj;
      boolean mod;
      
      obj = properties.setProperty( token, value );
      mod = obj == null || !value.equals( obj );
      modified |= mod;
      return mod;
   }

   @Override
   public int getIntOption ( String token ) {
      String p;
      int i;

      i = 0;
      p = getProperty( token );
      if ( p != null )
      try {
         i = Integer.parseInt( p );
      }
      catch ( Exception e )
      {
         // meaning: look for default if mapped value is invalid (unparsable)
         p = defaults.getProperty( token );
         try {
            i = Integer.parseInt( p );
         }
         catch ( Exception e2 )
         {}
      }
      return i;
   }  // getIntOption

   @Override
   public long getLongOption ( String token ) {
      String p;
      long i;

      i = 0;
      p = getProperty( token );
      if ( p != null )
      try { i = Long.parseLong( p ); }
      catch ( Exception e )
      {
         // meaning: look for default if mapped value is invalid (unparsable)
         p = defaults.getProperty( token );
         try { i = Long.parseLong( p ); }
         catch ( Exception e2 )
         {}
      }
      return i;
   }  // getLongOption

   @Override
   public String getOption ( String token ) {
      String hstr;
      
      if ( (hstr = getProperty( token )) == null )
         hstr = "";
      return hstr;
   }

   @Override
   public boolean isOptionSet ( String token ) {
      String p = getProperty( token );
      return p != null && p.equals( "true" );
   }

   @Override
   public boolean setIntOption ( String token, int value ) {
      return setOption( token, String.valueOf( value ) );
   }

   @Override
   public boolean setLongOption ( String token, long value ) {
      return setOption( token, String.valueOf( value ) );
   }

   @Override
   public boolean setOption ( String token, boolean value ) {
      String hstr;
      
      hstr = value ? "true" : "false";
      return setOption( token, hstr );
   }

   @Override
   public boolean setOption ( String token, String value ) {
      boolean mod;
      
      if ( value == null || value.length() == 0 || 
    	   value.equals( defaults.getProperty( token )) )  {

    	 mod = properties.remove( token ) != null;
         modified |= mod;
      } else {
         mod = setProperty( token, value );
      }
      return mod;
   }

   // since 0-5-0
   @Override
   public boolean setBounds ( String token, Rectangle bounds ) {
      Dimension dim;
      Point pos;
      String nameA, nameB;
      boolean b1, b2;
      
      nameA = token.concat( "-framedim" );
      nameB = token.concat( "-framepos" );
      
      // delete bounds info in options 
      if ( bounds == null ) {
         b1 = setOption( nameA, null );
         b2 = setOption( nameB, null );
      }
      // store parameter bounds info
      else {
         dim = bounds.getSize();
         b1 = setIntOption( nameA, (dim.width << 16) | (dim.height & 0xFFFF) );
         pos = bounds.getLocation();
         b2 = setIntOption( nameB, (pos.x << 16) | (pos.y  & 0xFFFF) );
      }
      
      return b1 | b2;
   }
   
   // since 0-5-0
   @Override
   public Rectangle getBounds ( String token ) {
      Rectangle bounds;
      Dimension frameDim;
      Point framePos;
      int i;

      if ( token == null )
         throw new NullPointerException();
      
      // read frame dimensions from option file
      if ( (i = getIntOption( token.concat( "-framedim" ) )) > 0 )
         frameDim = new Dimension( i >> 16, (short)i );
      else
         return null;
      
      // read frame position from option file
      i = getIntOption( token.concat( "-framepos") );
      framePos = new Point( i >> 16, (short)i );

      // create result
      bounds = new Rectangle( framePos, frameDim );
      return bounds;
   }

   @Override
   public boolean setStringList ( String name, List<Object> ls ) {
      String prefix, key;
      Iterator<Object> it;
      boolean eol;
      int i;
      
      if ( name == null )
         throw new NullPointerException();
      
      prefix = LISTMARK.concat( name ).concat( "-" );

      // remove all previous values of this list name
      eol = false;
      i = 0;
      while ( !eol )
      {
         key = prefix.concat( String.valueOf( i++ ) );
         eol = getProperty( key ) == null;
         if ( !eol )
            setOption( key, null );
      }
      
      // if list is available, add strings of list to file
      if ( ls != null && ls.size() > 0 )
      for ( it = ls.iterator(), i=0; it.hasNext(); i++ )
      {
         setOption( prefix.concat( String.valueOf( i ) ), it.next().toString() );
      }
      return true;
   }
   
   @Override
   public List<String> getStringList ( String name ) {
      ArrayList<String> list;
      String prefix, key, value;
      boolean eol;
      int i;
      
      if ( name == null )
         throw new NullPointerException();
      
      prefix = LISTMARK.concat( name ).concat( "-" );
      list = new ArrayList<String>();

      // find all stored values of this list name
      eol = false;
      i = 0;
      while ( !eol )
      {
         key = prefix.concat( String.valueOf( i++ ) );
         value = getProperty( key );
         eol = value == null;
         if ( !eol )
         {
            list.add( list.size(), value );
         }
      }
      return list;
   }

   /** Whether this <tt>OptionBag</tt> has been modified since last being
    *  loaded or stored.
    * @return boolean
    */
   public boolean isModified () {
      return modified;
   }
   
   /** Resets the "modified" marker to <b>false</b>. */
   public void resetModified () {
      modified = false;
   }
   
   /*      
   // TEST
   List list = Options.getStringList( "test.1" );
   if ( list != null )
   for ( Iterator it = list.iterator(); it.hasNext(); )
      System.out.println( "- TEST LIST 1 retrieve value: [" + it.next() + "]" );

   if ( GUIService.userConfirm( "REMOVE TEST LIST?" ))
   {
      Options.setStringList( "test.1", null );
   }
   
   if ( GUIService.userConfirm( "WRITE NEW TEST LIST?" ))
   {
      String[] arr = new String[] {"karina" };
      list = new ArrayList();
      for ( int i = 0; i < arr.length; i++ )
         list.add( arr[i] );
      Options.setStringList( "test.1", list );
   }
*/      

}