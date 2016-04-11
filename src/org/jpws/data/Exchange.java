/*
 *  Exchange in org.jpws.data
 *  file: Exchange.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 09.12.2005
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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.KeyStroke;

import org.jpws.front.HistoryHandler;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.exception.InvalidPassphrasePolicy;
import org.jpws.pwslib.global.UUID;

/**
 * Class describing various elements for communicating records and data fields
 * of a database with a serializable text form for export and import features
 * of the program.
 * 
 * JDK1.6 OK
 *  
 */
public class Exchange
{
	
/** Returns an object that represents the record content of the given field.
 * These objects render their serializable content with the <code>toString()</code>
 * method.
 * 
 * @param rec PwsRecord
 * @param field DataField, describing a data element of the record
 * @return Object or <b>null</b> if this field has no assignment
 */
public static Object getFieldContent ( PwsRecord rec, Exchange.DataField field )
{
   PwsPassphrasePolicy pp;
   KeyStroke stroke;
   Object o;
   
   if ( rec == null | field == null ) return null;
   
   o = null;
   switch ( field.getName() )
   {
   case Exchange.DataField.GROUP:  o = rec.getGroup(); break;
   case Exchange.DataField.TITLE:  o = rec.getTitle(); break;
   case Exchange.DataField.USER:  o = rec.getUsernamePws(); break;
   case Exchange.DataField.PASSWORD:  o = rec.getPassword(); break;
   case Exchange.DataField.NOTES:  o = rec.getNotesPws(); break;
   case Exchange.DataField.EMAIL:  o = rec.getEmailPws(); break;
   case Exchange.DataField.UUID:  o = rec.getRecordID(); break;
   case Exchange.DataField.URL:  o = rec.getUrlPws(); break;
   case Exchange.DataField.HISTORY:  o = historyListValue( rec.getHistory() ); break;
   case Exchange.DataField.INTERVAL:  
      o = rec.getExpiryInterval(); break;
   case Exchange.DataField.POLICY:
      o = (pp = rec.getPassPolicy()) == null ? null : pp.getInternalForm(); 
      break;
   case Exchange.DataField.SHORTKEY:
	      o = (stroke = rec.getKeyboardShortcut()) == null ? null : stroke.toString(); 
	      break;
   case Exchange.DataField.T_CREATED:  o = new Long( rec.getCreateTime() ); break;
   case Exchange.DataField.T_MODIFIED:  o = new Long( rec.getModifiedTime() );  break;
   case Exchange.DataField.T_PASSACCESS:  o = new Long( rec.getAccessTime() );  break;
   case Exchange.DataField.T_PASSEXPIRY:  o = new Long( rec.getPassLifeTime() );  break;
   case Exchange.DataField.T_PASSMODIFIED:  o = new Long( rec.getPassModTime() );  break;
   }
   return o;
}  // getFieldContent

/** Sets the content of a single field in the given <code>PwsRecord</code>.
 * 
 * @param record <code>PwsRecord</code>
 * @param field <code>Exchange.DataField</code> field identification object
 * @param value String, the textual representation of the field value as used
 *              in data exports 
 * @throws ParseException
 * @throws InvalidPassphrasePolicy
 */
public static void setFieldContent ( PwsRecord record, Exchange.DataField field, String value ) 
throws ParseException, InvalidPassphrasePolicy
{
   if ( record == null | field == null | value == null ) return;

   switch ( field.getName() ) {
   case DataField.UUID:      record.setRecordID( new UUID( 
                             Util.condensedNumber( value, 16 ))); break;
   case DataField.TITLE:     record.setTitle( value ); break;
   case DataField.GROUP:     record.setGroup( value ); break;
   case DataField.USER:      record.setUsername( value ); break;
   case DataField.PASSWORD:  record.setPassword( new PwsPassphrase( value )); break;
   case DataField.URL:       record.setUrl( value ); break;
   case DataField.EMAIL:     record.setEmail( value ); break;
   case DataField.NOTES:     record.setNotes( value ); break;
   case DataField.POLICY:    record.setPassPolicy( getPassPolicy( value )); break;
   case DataField.SHORTKEY:  record.setKeyboardShortcut( getShortcutKey( value )); break;
   case DataField.HISTORY:   record.setHistory( extractedHistory( value )); break;
   case DataField.INTERVAL:  record.setExpiryInterval( getInteger( value )); break;
   case DataField.T_CREATED: record.setCreateTime( getTimeValue( value )); break;
   case DataField.T_MODIFIED: record.setModifyTime( getTimeValue( value )); break;
   case DataField.T_PASSACCESS: record.setAccessTime( getTimeValue( value )); break;
   case DataField.T_PASSEXPIRY: record.setPassLifeTime( getTimeValue( value )); break;
   case DataField.T_PASSMODIFIED: record.setPassModTime( getTimeValue( value )); break;
   }
}

/** Transforms a record based HISTORY value to the corresponding
 *  export file representation.
 *    
 * @param h String record based value of password history
 * @return String export value
 */
private static String historyListValue ( String h )
{
   if ( h == null ) return null;
   
   char separator = ',';
   HistoryHandler hist = new HistoryHandler( h );
   String[] arr = new String[ hist.getListSize() ];

   // collect encoded password entries (history elements)
   Iterator<HistoryHandler.Entry> it;
   int i;
   for ( it = hist.iterator(), i = 0; it.hasNext(); i++ ) {
      arr[ i ] = it.next().toExportString( separator );
   }
   
   // encode history
   String hstr = Util.CSV.encodeLine( arr, separator );
   return hstr;
}

/** Transforms an input field set into a list of fields, ordered to
 *  their ID values.
 *  
 * @param fields <code>Exchange.FieldSet</code>
 * @return List&lt;DataField&gt;
 */
public static List<DataField> getNormalizedFieldList ( FieldSet fields )
{
   if ( fields == null ) return null;
   
   ArrayList<DataField> list = new ArrayList<DataField>();
   for ( Iterator<DataField> it = fields.iterator(); it.hasNext(); ) {
	  DataField f1 = it.next();
      int size = list.size();
      for ( int i = 0; i < list.size(); i++ ) {
    	 DataField f2 = list.get( i );
         if ( f1.getName() < f2.getName() ) {
            list.add( i, f1 );
            break;
         }
      }
      
      if ( list.size() == size ) {
         list.add( f1 );
      }
   }
   return list;
}  // getNormalizedFieldList

/**
 * Converts an export encoded password history text into a PW3 encoded 
 * password history.
 * 
 * @param hs JPWS export history value
 * @return PW3 history value
 */
private static String extractedHistory ( String hs )
{
   if ( hs == null | hs.length() == 0 ) return null;
   
   char separator = ',';
   HistoryHandler hist = new HistoryHandler();
   String pwd, entr[], tuple[];
   
   // extract an array of entries
   entr = Util.CSV.decodeLine( hs, 0, separator );
   hist.setMaxItems( Math.max( entr.length, 16 ) );
   
   // parse single items
   for ( int i = entr.length; i > 0 ; i-- ) {
      tuple = Util.CSV.decodeLine( entr[ i-1 ], 0, separator );
      if ( tuple.length >= 2 ) {
         long time = Util.timeFromString( tuple[ 0 ], Util.GMT );
         pwd = tuple[ 1 ];
         hist.pushPassword( pwd, time );
      }
   }
   
   // get return value
   return hist.getContentPw3();
}

/** Attempts to extract a time value from the data field content string <code>c</code>.
 *  The time value is assumed to be in GMT (UT) time zone.
 * 
 * @param c field content
 * @return epoch milliseconds
 * @throws java.text.ParseException 
 */
private static long getTimeValue ( String c )
   throws java.text.ParseException
{
   long v = 0;
   
   if ( c.length() == 0 ) return 0;
   
   // attempt interpretation as EPOCH SECONDS (integer)
   try {
      v = Long.parseLong( c ) * 1000;

   } catch ( Exception e ) {
      // attempt parse a formatted text time-value
      try { 
         // Java known formats
         Date date = DateFormat.getDateInstance().parse( c );
         v = date.getTime();

      } catch ( ParseException e1 ) {
         // XML and other industry formats
         if ( (v = Util.timeFromString( c, Util.GMT )) == -1 )
            throw e1;
      }
   }
   return v;
}  // getTimeValue

/** Parses a string integer value by allowing the empty string
 * as equivalent to zero value.
 * 
 * @param c String
 * @return int
 * @throws NumberFormatException
 */
private static int getInteger ( String c ) throws NumberFormatException
{
   int v = 0;
   if ( !c.isEmpty()  )
      v = Integer.parseInt( c );
   return v;
}

/** Converts a string serialised value into a PwsPassphrasePolicy.
 * Returns <b>null</b> if the parameter is the empty string.
 * 
 * @param c String
 * @return PwsPassphrasePolicy or null
 */
private static PwsPassphrasePolicy getPassPolicy ( String c ) 
{
	PwsPassphrasePolicy pol = null;
	if ( !c.isEmpty() ) {
		pol = new PwsPassphrasePolicy(c);
	}
	return pol;
}

/** Converts a string serialised value into a KeyStroke.
 * Returns <b>null</b> if the parameter is the empty string.
 * 
 * @param c String
 * @return KeyStroke or null
 */
private static KeyStroke getShortcutKey ( String c ) 
{
	KeyStroke stroke = null;
	if ( !c.isEmpty() ) {
		stroke = KeyStroke.getKeyStroke(c);
	}
	return stroke;
}

/** Returns all exchangeable fields in a field set.
 *  @return <code>FieldSet</code>
 */
public static FieldSet allFields ()
{
   FieldSet set = new FieldSet();
   set.add( new DataField( DataField.GROUP ) );
   set.add( new DataField( DataField.TITLE ) );
   set.add( new DataField( DataField.USER ) );
   set.add( new DataField( DataField.PASSWORD ) );
   set.add( new DataField( DataField.NOTES ) );
   set.add( new DataField( DataField.UUID ) );
   set.add( new DataField( DataField.EMAIL ) );
   set.add( new DataField( DataField.URL ) );
   set.add( new DataField( DataField.HISTORY ) );
   set.add( new DataField( DataField.INTERVAL ) );
   set.add( new DataField( DataField.POLICY ) );
   set.add( new DataField( DataField.SHORTKEY ) );
   set.add( new DataField( DataField.T_CREATED ) );
   set.add( new DataField( DataField.T_MODIFIED ) );
   set.add( new DataField( DataField.T_PASSMODIFIED ) );
   set.add( new DataField( DataField.T_PASSEXPIRY ) );
   set.add( new DataField( DataField.T_PASSACCESS ) );
   return set;
}

/** Returns a field set which is regarded to hold the essential fields.
 *  @return <code>FieldSet</code>
 */
public static FieldSet essentialFields ()
{
   FieldSet set = new FieldSet();
   set.add( new DataField( DataField.GROUP ) );
   set.add( new DataField( DataField.TITLE ) );
   set.add( new DataField( DataField.USER ) );
   set.add( new DataField( DataField.PASSWORD ) );
   set.add( new DataField( DataField.URL ) );
   set.add( new DataField( DataField.NOTES ) );
   set.add( new DataField( DataField.EMAIL ) );
   set.add( new DataField( DataField.INTERVAL ) );
   set.add( new DataField( DataField.T_PASSEXPIRY ) );
   return set;
}

//  *********************  INNER CLASSES  ************************

/**
 *  Class to represent a set of records, represented by their UUID.
 *  The elements of a <code>RecordSet</code> are of type org.jpws.pwslib.global.UUID;
 *  the class inherits the methods of <code>HashSet</code>.  
 */
@SuppressWarnings("serial")
public static class RecordSet extends HashSet<UUID>
{
}

/**
 *  Class to represent a set of <code>DataField</code> instances.
 *  The purpose of this is to define an abstract field set as input element
 *  of various algorithms. (This does not contain record field values.)
 *  The elements of a <code>FieldSet</code> are of type org.jpws.data.Exchange.DataField;
 *  the class inherits the methods of <code>HashSet</code>.  
 *  
 */
@SuppressWarnings("serial")
public static class FieldSet extends HashSet<Exchange.DataField>
{
}

/**
 * This class represents a datafield of a database record in its generic plane.
 * Currently it only serves to identify a datafield by name. In future it might
 * also describe generic static or variable features of datafields.  
 */
public static class DataField
{
   // definition of integer field names
   public static final int GROUP = 1;
   public static final int TITLE = 2;
   public static final int USER = 3;
   public static final int PASSWORD = 4;
   public static final int EMAIL = 5;
   public static final int URL = 6;
   public static final int UUID = 7;
   public static final int NOTES = 8;
   public static final int HISTORY = 9;
   public static final int INTERVAL = 10;
   public static final int POLICY = 11;
   public static final int SHORTKEY = 12;
   public static final int T_CREATED = 33;
   public static final int T_MODIFIED = 34;
   public static final int T_PASSACCESS = 35;
   public static final int T_PASSMODIFIED = 36;
   public static final int T_PASSEXPIRY = 37;
   
   private static final HashMap<String,DataField> fmap = new HashMap<String,DataField>();
   static {
      // standard fields
      fmap.put( stringForName(GROUP), new DataField(GROUP) );
      fmap.put( stringForName(TITLE), new DataField(TITLE) );
      fmap.put( stringForName(USER), new DataField(USER) );
      fmap.put( stringForName(PASSWORD), new DataField(PASSWORD) );
      fmap.put( stringForName(EMAIL), new DataField(EMAIL) );
      fmap.put( stringForName(UUID), new DataField(UUID) );
      fmap.put( stringForName(URL), new DataField(URL) );
      fmap.put( stringForName(NOTES), new DataField(NOTES) );
      fmap.put( stringForName(HISTORY), new DataField(HISTORY) );
      fmap.put( stringForName(INTERVAL), new DataField(INTERVAL) );
      fmap.put( stringForName(POLICY), new DataField(POLICY) );
      fmap.put( stringForName(SHORTKEY), new DataField(SHORTKEY) );
      fmap.put( stringForName(T_CREATED), new DataField(T_CREATED) );
      fmap.put( stringForName(T_MODIFIED), new DataField(T_MODIFIED) );
      fmap.put( stringForName(T_PASSACCESS), new DataField(T_PASSACCESS) );
      fmap.put( stringForName(T_PASSEXPIRY), new DataField(T_PASSEXPIRY) );
      fmap.put( stringForName(T_PASSMODIFIED), new DataField(T_PASSMODIFIED) );

      // KEEPASS fields
      fmap.put( "PASSWORD GROUPS", new DataField(GROUP) );
      fmap.put( "LOGIN NAME", new DataField(USER) );
      fmap.put( "COMMENTS", new DataField(NOTES) );
      fmap.put( "CREATION TIME", new DataField(T_CREATED) );
      fmap.put( "LAST MODIFICATION", new DataField(T_MODIFIED) );
      fmap.put( "LAST ACCESS", new DataField(T_PASSACCESS) );
      fmap.put( "EXPIRES", new DataField(T_PASSEXPIRY) );
      fmap.put( "WEB SITE", new DataField(URL) );
   }
   
   private int name;

   public DataField ( int name ) {
      this.name = name;
   }
   
   /**
    * @return returns the export field name (textual)
    */
   public String toString () {
      return stringForName( name );
   }

   /**
    * @return returns the field name (integer)
    */
   public int getName () {
      return name;
   }

   /** Returns an export text representation of the given data field. 
    * 
    * @param name int, any value
    * @return String, textual field name or <b>null</b> if name is undefined
    */
   public static String stringForName ( int name ) {
      String hstr = null;

      switch ( name ) {
      case GROUP:  hstr = "GROUP"; break;
      case TITLE:  hstr = "TITLE"; break;
      case USER:  hstr = "USER"; break;
      case PASSWORD:  hstr = "PASSWORD"; break;
      case NOTES:  hstr = "NOTES"; break;
      case EMAIL:  hstr = "EMAIL"; break;
      case UUID:  hstr = "UUID"; break;
      case URL:  hstr = "URL"; break;
      case INTERVAL:  hstr = "INTERVAL"; break;
      case POLICY:  hstr = "POLICY"; break;
      case SHORTKEY:  hstr = "SHORTKEY"; break;
      case HISTORY:  hstr = "PWHISTORY"; break;
      case T_CREATED:  hstr = "T_CREATED"; break;
      case T_MODIFIED:  hstr = "T_MODIFIED"; break;
      case T_PASSACCESS:  hstr = "T_PASSACCESS"; break;
      case T_PASSEXPIRY:  hstr = "T_PASSEXPIRY"; break;
      case T_PASSMODIFIED:  hstr = "T_PASSMODIFIED"; break;
      }
      return hstr;
   }  // stringForName
   
   /** Returns the <code>DataField</code> corresponding to the given textual name.
    * 
    * @param name String field name, may be <b>null</b> 
    * @return <code>Exchange.DataField</code> or <b>null</b> if parameter does not
    *         match any defined field name 
    */
   public static DataField forName ( String name ) {
      return fmap.get(name);
   }

   /** Returns the <code>DataField</code> corresponding to the given integer name.
    * 
    * @param name int field name 
    * @return <code>Exchange.DataField</code> or <b>null</b> if parameter does not
    *         match any defined field name 
    */
   public static DataField forName ( int name ) {
      return fmap.get( stringForName( name ) );
   }

   @Override
   public boolean equals (Object obj) {
      return obj != null && ((DataField)obj).name == name;
   }

   @Override
   public int hashCode () {
      return name *5026;
   }
   
}  // class DataField

}
