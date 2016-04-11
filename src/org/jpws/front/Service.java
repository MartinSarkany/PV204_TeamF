/*
 *  Service in org.jpws.front
 *  file: Service.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 25.11.2005
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StreamCorruptedException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jpws.data.Exchange;
import org.jpws.data.Exchange.RecordSet;
import org.jpws.data.IOManager;
import org.jpws.data.Options;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.ButtonBarListener;
import org.jpws.front.util.DefaultButtonBarListener;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.HtmlBrowserDialog;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.Util.BufferInt;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.data.PwsRecordList;
import org.jpws.pwslib.exception.ApplicationFailureException;
import org.jpws.pwslib.exception.DuplicateEntryException;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.UUID;

public class Service
{

/**
 * This performs a regular request to the JPWS project web-site to obtain
 * information about a new program version or updated news pages.
 * 
 * @param investigate if <b>true</b> there will be a report for "nothing new"
 *
 */
public static void controlProjectNews ( boolean investigate )
{
   Global.ProjectServiceParcel serverInfo;
   URL url;
   String hstr;
   int days;
   long lastNewsTime, build, newstime, targetTime, actualTime;
   boolean somethingNew;

   // TODO semaphore
   
   somethingNew = false;
   System.out.println( "- running CONTROL PROJECT NEWS" );
   
   actualTime = System.currentTimeMillis();
   try { 
      // load server properties
      serverInfo = Global.ProjectServiceParcel.get();
      if ( serverInfo == null ) {
         // return undone if no connection to the Internet
         if ( investigate ) {
            GUIService.infoMessage( "dlg.connect.failure", "msg.checknews.connectfail" );
         }
         return;
      }
      
      // investigate properties
      // look for ACTUAL PROGRAM BUILD (period: 5 days)
      targetTime = Options.getLongOption( "buildWarnTime" );
      if (  investigate | targetTime < actualTime ) {
         // report / ask user if new BUILD is available
         build = serverInfo.getProgramVersion();
         if ( build > Global.BUILDVERSION ) {
            hstr = ResourceLoader.getDisplay( "msg.checknews.newrelease" );
            hstr = Util.substituteText( hstr, "$name", serverInfo.getProgramName() );
            if ( GUIService.userConfirm( hstr ) ) {
               url = serverInfo.getDownloadPageUrl();
               Global.startBrowser( url );
            }
            somethingNew = true;
         }

         // mark for next check time 
         days = Math.max(7, Global.PARCEL_CHECK_DAYS);
         targetTime = actualTime + days * Global.DAY;
         Options.setLongOption( "buildWarnTime", targetTime );
      }
      
      // look for ACTUAL NEWS TIME
      newstime = serverInfo.getNewsTime();
      lastNewsTime = Options.getLongOption( "lastNewsTime" );
      Log.debug( 5, "(Service.controlProjectNews) last news time stored: " 
         + Util.standardTimeString( lastNewsTime ) + " (" + lastNewsTime + ")");
      if ( newstime > lastNewsTime ) {
         if ( GUIService.userConfirm( "msg.checknews.newspage" ) ) {
            url = serverInfo.getServicePageUrl();
            Global.startBrowser( url );
         }
         somethingNew = true;

         // mark actual time as last checked time
         Options.setLongOption( "lastNewsTime", newstime );
      }

      // mark actual time as last project check time
      Options.setLongOption( "lastProjectCheckTime", actualTime );

      // report nothing new if applicable
      if ( investigate & !somethingNew ) {
         GUIService.infoMessage( null, "msg.checknews.nothing" );
      }

   } catch ( Exception e ) { 
      e.printStackTrace();
   }
}  // controlProjectNews

/** Issues a warning text to the user concerning privacy issues and security risk
 *  when performing CSV plaintext conversion.
 */
public static void csvUserWarning ()
{
   HtmlBrowserDialog dlg;
   String title, path;
   
   title = ResourceLoader.getDisplay( "dlg.csvwarning" );
   dlg = new HtmlBrowserDialog( Global.getActiveFrame(), title, true );
   try {
      path = "#standards/" + ResourceLoader.getCommand( "html.file.csvwarning" );
      dlg.setPage( ResourceLoader.getResourceURL( path ) );
      dlg.show();
   }
   catch ( IOException e )
   {
      e.printStackTrace();
   }
}

/** Opens all random marked mirror files (unsaved new databases).
 *  Switches display to DESKTOP modus if necessary.  
 */
public static void controlRandomMirrors ()
{
   PwsFileContainer fco; 
   Iterator<String> it;
   ContextFile file;
   
   it = Global.getRandomMirrorNames();
   if ( it.hasNext() )
   {
      Log.debug(7, "(Service.controlRandomMirrors) --- seeing trans-session random mirror entries: " + 
                   Global.randomMirrors.getSize() );
      
      for ( ; it.hasNext(); )
      {
         // get file name of the memorised mirror
         // and combine it with the current mirror directory
         file = IOManager.makeLocalContextFile( Global.mirrorDir, it.next() );
         Log.debug(7, "(Service.controlRandomMirrors) investigating random mirror entry: "
                   .concat( file.getFilepath() ));
         
         // if the mirror file exists, try open it into shelf
         try {
            if ( file.exists() ) {
                DisplayManager.setDisplayState( DisplayManager.DISPLAY_DESKTOP );
                fco = DatabaseHandler.openFileToShelf( file );
                
                if ( fco != null ) {
                    // the shelf file's path is reset to void
                    // and the mirror is removed on exit
                    Global.removeRecentFile(fco);
                    fco.setFilePath(null);
                    Global.removeRandomMirror( file );
                }
             }
            
             // otherwise remove the memory entry
             else
            	 Global.removeRandomMirror(file);
         } 
         catch (IOException e) 
         { e.printStackTrace(); }
      }
   }
}

/**
 * Assists the user to create a random generated password and displays the result
 * in a free-floating and modal-unbound info-box. The resulting
 * dialog is child to the main frame. Password
 * policy used is the global "generatorPolicy", which can get modified during 
 * execution.
 * 
 * @param owner Dialog the owner of the resulting box, or <b>null</b> for global level
 * @return <code>ButtonBarDialog</code> or <b>null</b> if no password was generated
 * @since 0-6-0
 */
public static ButtonBarDialog generatePassword ( final Dialog owner, boolean cascade )
{
    PwsPassphrasePolicy oldPolicy, usePolicy;
    ButtonBarDialog dlg;
    
    usePolicy = Global.generatorPolicy;
    oldPolicy = (PwsPassphrasePolicy)usePolicy.clone();
    dlg = generatePassword( owner, usePolicy, cascade );
    if ( !usePolicy.equals( oldPolicy ) )
        Options.setOption( "generatorPolicy", usePolicy.getInternalForm() );
    return dlg;
}

/**
 * Assists user to create a random generated password and displays the result
 * in a free-floating and modal-unbound info-box. The resulting
 * dialog is child to main frame.
 * 
 * @param owner Dialog the owner of the resulting box, or <b>null</b> for global level
 * @param policy PwsPassphrasePolicy the password generation policy to be applied or <b>null</b>;
 *        may get edited during execution of the dialog
 * @param cascade boolean if true the user can cascade with generation of more 
 *        passwords trough an extra button
 * @return <code>ButtonBarDialog</code> or <b>null</b> if no password was generated
 * @since 0-6-0
 */
public static ButtonBarDialog generatePassword ( final Dialog owner, 
		                      final PwsPassphrasePolicy policy,
		                      final boolean cascade )
{
   final PwsPassphrase pass;
   ActionHandler.checkForEDT();
   
   // create a random password
   // (this will block until user confirms a password or cancels)
   // store a modified general passphrase policy to Options
   pass = GUIService.generateRandomPassphrase( owner, null, policy );
   
   // display the new password in a modal-free ButtonBarDialog
   // returns the dialog
   if ( pass != null )
   {
      JLabel label;
      VerticalFlowLayout layout;
      JPanel panel;
      final JButton clipButton, nextButton;
      final ButtonBarDialog dlg;
      ButtonBarListener barListener;
      String hstr;
      
      layout = new VerticalFlowLayout( 10 );
      panel = new JPanel( layout );
      panel.setBorder( BorderFactory.createEmptyBorder( 20, 20, 0, 20 ));
      
      hstr = ResourceLoader.getDisplay( "msg.randpassgen.warning" );
      label = new JLabel( hstr );
      label.setFont( DisplayManager.getFont("control").deriveFont( Font.PLAIN ) );
      panel.add( label );
      
      label = new JLabel( pass.getString() );
      label.setFont( DisplayManager.getFont("password").deriveFont( Font.BOLD, (float)18.0 ));
      panel.add( label );
      panel.putClientProperty( "passphrase", pass );

      // create the dialog
      dlg = new ButtonBarDialog( Global.getActiveFrame(), panel, DialogButtonBar.OK_BUTTON, false );
      dlg.setTitle( ResourceLoader.getDisplay( "dlg.randpassgen" ) );
      dlg.setCloseByEscape( false );
      dlg.moveRelatedTo( dlg.getOwner() );
      dlg.setModalExclusionType( Dialog.ModalExclusionType.APPLICATION_EXCLUDE );
      dlg.setAutonomous( true );
      dlg.setSynchronous(true);

      // add a copy to clipboard button
      clipButton = new JButton( ResourceLoader.getDisplay( "button.clipboard" ) );
      clipButton.setToolTipText( ResourceLoader.getCommand( "tooltip.clipboard.copypass" ) );
      dlg.getButtonBar().add( clipButton );
      
      // add a "Generate" next password button, if opted 
	  nextButton = new JButton( ResourceLoader.getDisplay( "button.next.pw" ) );
	  nextButton.setToolTipText( ResourceLoader.getCommand( "tooltip.generator.nextpassword" ) );
      if ( cascade ) {
	     dlg.getButtonBar().add( nextButton );
      }
      
      // implement the button bar listener
      barListener = new DefaultButtonBarListener ( dlg ) {
    	  
         public boolean extraButtonPerformed ( Object button ) {  
            if ( button == clipButton & button != null ) {
               String text = pass.getString();
               Global.clipboard.setContents( new StringSelection( text ), null );
               ActionHandler.clipboardUpdated();
               ActionHandler.confirmOperation( dlg, ActionHandler.OP_SENDCLIPBOARD, "confirm.password" );

            } else if ( button == nextButton & button != null ) {
               generatePassword( dlg, policy, cascade );
            }
            return true;
        }
      };
      dlg.addButtonBarListener( barListener );
      
      dlg.setVisible(true);
      dlg.toFront();
      return dlg;
   }
   return null;
}

/**
 * GUI active function to perform CSV cleartext export of a set of PWS database 
 * records. This method does not export invalid records and reports to the GUI.
 * 
 * @param output the output stream where cleartext is written to
 * @param recs PwsFile representing the set of records to be exported
 * @param fields set of record fields drawn for output
 * @param exclude optional set of records to be excluded from this operation
 * @param charset target text character set
 * @param modus target data format; 0 = DATABASE (RFC4180), 1 = SPREADSHEET
 * 
 * @return <b>true</b> if and only if operation went fine ; (<b>false</b>
 *         e.g. if user broke the operation OR recs file had no password defined) 
 * @throws IOException
 * @throws IllegalStateException
 */
public static boolean csvExportRecords ( 
      OutputStream output,
      PwsFile recs, 
      Exchange.FieldSet fields,
      RecordSet exclude, 
      Charset charset,
      int modus )
   throws IOException
{
   List<Exchange.DataField> fieldList = new ArrayList<Exchange.DataField>();
   Exchange.DataField field;
   PwsRecord record;
   PwsPassphrase fpass;
   Writer out;
   Iterator<PwsRecord> itRec;
   Iterator<Exchange.DataField> itFld;
   Object obj;
   String hstr, data[];
   int i, h, nrOfFields, failCount, invalidCount;;
   long time;
   char separator;

   if ( fields == null )
      fields = Exchange.allFields();
   if ( output == null | recs == null | fields.size() == 0 )
      return false;
   if ( charset == null )
      charset = Charset.forName( Global.getDefaultCharset() );
   
   // access control
   if ( (fpass = recs.getPassphrase()) == null )
   {
      GUIService.failureMessage( "Illegal CSV output access", null );
      return false;
   }
   
   // security check for password
   hstr = Util.fileNameOfPath( recs.getFilePath() );
   if ( !GUIService.passwordControl( fpass, hstr, false ) )
   {
      return false;
   }
   
   // determine number of invalid and failing (exempted) records 
   // iterate over input record list
   failCount = invalidCount = 0;
   for ( itRec = recs.iterator(); itRec.hasNext(); )
   {
      record = itRec.next();
      if ( !record.isValid() )
         invalidCount++;
      else if ( exclude != null && exclude.contains( record.getRecordID() ) )
         failCount++;
   }

   // report invalids
   if ( invalidCount > 0 )
   {
      hstr = ResourceLoader.getDisplay( "msg.exportcsv.invalids" );
      hstr = Util.substituteText( hstr, "$count", String.valueOf( invalidCount ) );
      if ( !GUIService.userConfirm( hstr ) )
         return false;
   }
   
   
   separator = modus == 1 ?  ';' : ',';
   out = new BufferedWriter( new OutputStreamWriter( output, charset ) );
   nrOfFields = fields.size();
//System.out.println( "- activated charset: " + charset );
   
   // Normalise field list
   fieldList = Exchange.getNormalizedFieldList( fields );
   
   // write COMMENT LINE
   hstr = "# JPWS-EXPORT MIME type=\"text/csv\" charset=\"" + charset.name() + 
          "\" header=\"present\" delim=\"" + 
          Integer.toHexString( separator ).toUpperCase() + "\"\r\n";
   out.write( hstr );
   hstr = "# RECORDS=" + (recs.getRecordCount()-invalidCount-failCount) + " OUTPUT=" + 
          (modus == 1 ? "SPREADSHEET" : "RFC4180") + "\r\n";
   out.write( hstr );
   
   // write HEADER LINE
   for ( itFld = fieldList.iterator(), i = 0; itFld.hasNext(); i++ )
   {
      hstr = itFld.next().toString();
      if ( i != 0 )
         out.write( separator );
      out.write( hstr );
   }   
   out.write( "\r\n" );
   
   // iterate over input record list
   for ( itRec = recs.iterator(); itRec.hasNext(); )
   {
      record = itRec.next();
      if ( record.isValid() && 
           (exclude == null || !exclude.contains( record.getRecordID() )) )
      {
         // write a single record (= line)
         // compile export strings
         data = new String[ nrOfFields ];
         for ( itFld = fieldList.iterator(), i = 0; itFld.hasNext(); i++ )
         {
            field = itFld.next();
            if ( (obj = Exchange.getFieldContent( record, field )) == null )
               continue;

            if ( obj instanceof PwsPassphrase )
               hstr = ((PwsPassphrase)obj).getString();

            else if ( obj instanceof Integer )
            {
               h = ((Integer)obj).intValue();
               hstr = h == 0 ? "" : String.valueOf( h );
            }
            else if ( obj instanceof Long )
            {
               time = ((Long)obj).longValue();
               if ( time == 0 )
                  hstr = "";
               else
                  hstr = modus == 1 ? Util.xmlTimeString( time ) :
                         Long.toString( time / 1000 );
            }
            
            else
               hstr = obj.toString();
//               throw new IllegalStateException("bad record value");
            
            if ( modus == 1 )
            {
               hstr = Util.substituteText( hstr, "\r\n", " " );
               hstr = Util.substituteText( hstr, "\n", " " );
            }
            data[ i ] = hstr;
         }   
         
         // write strings to output (CSV encoded)
         hstr = Util.CSV.encodeLine( data, separator );
         out.write( hstr );
         out.write( "\r\n" );
      }
   }
   out.flush();
   
   return true;
}  // exportRecords

/**
 * Reads a line in the CSV context from a <code>Reader</code> device. 
 * Returns <b>null</b> if the line was not properly terminated (open quote). 
 *    
 * @param reader <code>Reader</code> object to be read from
 * @param c first character of line (push back)
 * @return String containing CSV line or <b>null</b> if unexpected eof  
 * @throws IOException
 */
private static String readQuoteContainingLine ( Reader reader, char c ) throws IOException
{
   StringBuffer sbuf;
   boolean quoted;
   
   sbuf = new StringBuffer( 512 ); 
   quoted = false;
   
   while ( true) 
   {
      sbuf.append( c );

      // on unquoted NEWLINE return line 
      if ( !quoted && c == '\n' )
      {
         return sbuf.toString();
      }
      
      // on each quote change "text quoted" state
      if ( c == '"' )
      {
         quoted = !quoted;
      }

      // attempt read next character
      if ( reader.ready() )
         c = (char)reader.read();
      else
         // on file-end react to "text quoted" state
         return quoted ? null : sbuf.toString();
   }
} // readQuoteContainingLine

/** Determines the field position pattern from the header text line.
 *  Controls if the fields "TITLE" and "PASSWORD" are present in the header.
 *   
 *  @throws StreamCorruptedException if essential header elements are missing
 */ 
private static Exchange.DataField[] detectFieldPattern ( String[] header )
   throws StreamCorruptedException
{
   Exchange.DataField field, titleField, passwordField, arr[];
   Exchange.FieldSet set;
   int i;
   
   arr = new Exchange.DataField [ header.length ];
   set = new Exchange.FieldSet();
   titleField = Exchange.DataField.forName( Exchange.DataField.TITLE );
   passwordField = Exchange.DataField.forName( Exchange.DataField.PASSWORD );
   
   // cycle: allocate fields for column names
   for ( i = 0; i < header.length; i++ )
   {
      field = Exchange.DataField.forName( header[ i ].trim() );
      arr[ i ] = field;
      set.add( field );
      Log.debug(7, "(Service.PwsListCompareResult.detectFieldPattern) added import column: [" + header[i].trim() + "]");
   }      

   // check validity
   if ( !(set.contains(titleField) & set.contains(passwordField)) )
      throw new StreamCorruptedException( "missing essential HEADER LINE fields" );
   
   return arr;
}  // detectFieldPattern

private static InputStream clearUTF ( InputStream input, Charset charset )
      throws IOException
{
   PushbackInputStream pbIn;
   byte[] buf;
   
   pbIn = new PushbackInputStream( input, 4 );

   if ( charset.equals( Charset.forName( "UTF-8" )) )
   {
      buf = new byte[ 3 ];
      pbIn.read( buf );
      if (  !(buf[0] == (byte)0xEF & buf[1] == (byte)0xBB & buf[2] == (byte)0xBF) )
         pbIn.unread( buf );
   }
   else if ( charset.equals( Charset.forName( "UTF-16BE" )) )
   {
      buf = new byte[ 2 ];
      pbIn.read( buf );
      if (  !(buf[0] == (byte)0xFE & buf[1] == (byte)0xFF) )
         pbIn.unread( buf );
   }
   else if ( charset.equals( Charset.forName( "UTF-16LE" )) )
   {
      buf = new byte[ 2 ];
      pbIn.read( buf );
      if (  !(buf[0] == (byte)0xFF & buf[1] == (byte)0xFE) )
         pbIn.unread( buf );
   }

   return pbIn; 
}

/**
 * Imports a set of records from a CSV formatted text source and returns 
 * the result as a <code>PwsRecordList</code>. 
 *  
 * @param input source input stream
 * @param charset source text character set
 * @param modus source data format; 0 = DATABASE (RFC4180), 1 = SPREADSHEET
 * @param omitted BufferInt, integer return variable informing about omitted
 *        records due to format or validity errors (may be <b>null</b>)
 * @param errLog printing device for error logging (non-fatal errors only)
 *        (may be <b>null</b>)
 * @return <code>PwsRecordList</code>
 * @throws StreamCorruptedException if input text has fatal format errors
 * @throws IOException
 */
public static PwsRecordList csvImportRecords (
      InputStream input,
      Charset charset,
      int modus,
      BufferInt omitted,
      PrintWriter errLog
      ) throws IOException
{
   PwsRecordList list;
   PwsRecord record;
   BufferedReader reader;
   Exchange.DataField pattern[], field, modField;
   String hstr, csvLine, arr[];
   char c1, separator;
   boolean commentPhase;
   int i, line, recs, omits, fields;
   long modTime;
   
   // clean UTF data stream of encode leading stuff
   input = clearUTF( input, charset );
   
   list = new PwsRecordList();
   reader = new BufferedReader( new InputStreamReader( input, charset ) );
   separator = modus == 1 ? ';' : ',';
   line = recs = omits = fields = 0;

   // "pattern" is an array realising a relation from field value positions
   // in a line of import text into the corresponding record data fields.
   // Data field of <b>null</b> indicates a position to be skipped (e.g. unknown field)
   pattern = null;

   commentPhase = true;
   while ( reader.ready() )
   {
      // read first char in line; ignore empty lines
      c1 = (char)reader.read();
      line++;
      if ( c1 == '\n' | c1 == '\r' )
         continue;

      // skip comment line
      if ( commentPhase && c1 == '#' ) {
         reader.readLine();
         continue;
      }
      commentPhase = false;

      // read rest of line
      csvLine = readQuoteContainingLine( reader, c1 );
      if ( csvLine == null )
         throw new StreamCorruptedException("CSV fatal corrupted line: " + line );
      Log.debug(7, "(Service.csvImportRecords) text line input: [" + csvLine + "]");
      
      // interpret line (extract fields)
      arr = Util.CSV.decodeLine( csvLine, 0, separator );
      
      // deal with file header line
      if ( recs == 0 ) {
         fields = arr.length;
         pattern = detectFieldPattern( arr );
      }

      // deal with normal line
      else {
         // control field size (-> error log)
         if ( arr.length != fields ) {
            if ( errLog != null ) {
               hstr = ResourceLoader.getDisplay( "msg.import.error.invalidline" );
               hstr = Util.substituteText( hstr, "$record", String.valueOf( recs ));
               hstr = Util.substituteText( hstr, "$fields", String.valueOf( arr.length ));
               hstr = Util.substituteText( hstr, "$expected", String.valueOf( fields ));
               errLog.println( hstr );
               errLog.println( csvLine );
            }
            recs++;
            omits++;
            continue;
         }
         
         // create record
         record = new PwsRecord();
         modTime = record.getModifiedTime();
         try {
            record.setImportStatus( PwsRecord.IMPORTED );

            // assign all field values of the current import line
            modField = Exchange.DataField.forName( Exchange.DataField.T_MODIFIED );
            for ( i = 0; i < arr.length; i++ ) {
               field = pattern[i];
               Exchange.setFieldContent(record, field, arr[i] );
               if ( field.equals( modField ) ) {
                  modTime = record.getModifiedTime();
               }
            }

            // ensure the record's modify time value for import line value
            record.setModifyTime( modTime );
            
            // append record
            list.addRecord( record ); 

         } catch ( Exception e ) {
            e.printStackTrace();
            if ( errLog != null ) {
               hstr = ResourceLoader.getDisplay( "msg.import.error.invalidrec" );
               hstr = Util.substituteText( hstr, "$record", String.valueOf( recs ) );
               hstr = Util.substituteText( hstr, "$exc", e.toString() );
               errLog.println( hstr ); 
               errLog.println( csvLine );
               omits++;
            }
         }
      }
      recs++;
   } // while
   
   if ( omitted != null )
      omitted.value = omits;
   return list;
} // csvImportRecords

/**
 * Structure containing the results of a comparison of two <code>PwsRecordList</code>
 * objects. This structure e.g. is returned by method <code>Service.comparePwsLists()</code>. 
 * Where not otherwise stated, records in the resulting lists are deep clones of the
 * source list records.
 */
public static class PwsListCompareResult
{
   /** Input PWS record list A (shallow clone) */ 
   PwsRecordList source_A;
   /** Input PWS record list B (shallow clone) */ 
   PwsRecordList source_B;
   /** List of records represented only in source A (shallow clones) */ 
   PwsRecordList only_A;
   /** List of records represented only in source B (shallow clones) */ 
   PwsRecordList only_B;
   /** List of records represented both in in A and B (via UUID). 
    * (This list exclusively contains records of source_A) */ 
   PwsRecordList cutSet;
   /** List of records with identical content in A and B */ 
   PwsRecordList identical;
   /** List of records with conflicting content in A and B
    * (Two records are conflicting if they have same UUID but
    * divergent content;  
    * this list exclusively contains records of source_B) */ 
   PwsRecordList conflict;
}

public static PwsListCompareResult comparePwsLists ( PwsRecordList a, PwsRecordList b )
{
   PwsListCompareResult res;
   PwsRecord rec, rec2;
   Iterator<PwsRecord> it;
   
   if ( a == null | b == null )
      throw new NullPointerException();
   
   res = new PwsListCompareResult();
   res.source_A = (PwsRecordList)a.clone();
   res.source_B = (PwsRecordList)b.clone();
   res.cutSet = a.intersectionRecordList( b );
   res.only_A = a.excludeRecordList( b );
   res.only_B = b.excludeRecordList( a );
   
   // walk-through cutset for identical-conflict discrimination
   res.identical = new PwsRecordList();
   res.conflict = new PwsRecordList();

   for ( it = res.cutSet.iterator(); it.hasNext(); ) {
      UUID id = it.next().getRecordID();
      rec = a.getRecord( id );
      rec2 = b.getRecord( id );
      try {
         if ( Util.equalArrays( rec2.getSignature(), rec.getSignature() ) ) {
            res.identical.addRecord( rec );
         } else {
            res.conflict.addRecord( rec2 );
         }
      } catch ( DuplicateEntryException e ) {
      }
   }
   return res;
}

public static void transferZipfile (File source, File target) 
		throws ZipException, IOException {
	
	ZipFile srcZip = new ZipFile(source);
	OutputStream output = new FileOutputStream(target);
	ZipOutputStream tarOut = new ZipOutputStream(output);
	
	for ( Enumeration<?> en = srcZip.entries(); en.hasMoreElements(); ) {
		ZipEntry entry = (ZipEntry)en.nextElement();
		tarOut.putNextEntry(entry);
		InputStream input = srcZip.getInputStream(entry);
		Util.transferData(input, tarOut, 4000);
		input.close();
	}
	
	tarOut.flush();
	tarOut.close();
}

/**
 * Lets the user choose a file from a chooser dialog and perform a secure wipe out 
 * operation on it. The secure wipe function performs iterative writes
 * of various data patterns to the content of the specified file. The file is 
 * deleted from its directory if possible, otherwise it contains random data.  
 */
public static void secureWipeFile ()
{
   JFileChooser   fc;
   ContextFile lock;
   File file;
   String hstr, path;

   // file open dialog
   fc = new FileOpenDialog( 0, Global.currentDir );
   fc.setDialogTitle( ResourceLoader.getDisplay( "dlg.wipefile" ) );
   if ( fc.showOpenDialog( Global.getActiveFrame() ) != JFileChooser.APPROVE_OPTION )
      return;

   Global.currentDir = fc.getCurrentDirectory();
   file = fc.getSelectedFile();
   path = file.getAbsolutePath();
   if ( !file.isFile() ) {
      // file not found message
      hstr = ResourceLoader.getDisplay( "msg.filenotfound" );
      hstr = Util.substituteText( hstr, "$path", path );
      GUIService.infoMessage( null, hstr );
      return;
   }
   
   try {
      lock = Global.getContextFile( Util.makeFileURL( path ) );
      
      // check for IO-permittance
      if ( !IOManager.access_allowed( lock, false ) ) {
         hstr = ResourceLoader.getDisplay( "msg.io_conflict.general" );
         GUIService.failureMessage( hstr, null );
         return;
      }
      
      // ask for user confirm to wipe file
      hstr = ResourceLoader.getDisplay( "msg.perform.wipefile" );
      hstr = Util.substituteText( hstr, "$file", file.getAbsolutePath() );
      if ( !GUIService.userConfirm( hstr ) ) {
         return;
      }
      
      // wipe the file
      // TODO IO-Manager: reserve write access to file
      Util.wipeAFile( file );

      // operation confirm message
      hstr = ResourceLoader.getDisplay( "msg.confirm.filewipe" );
      hstr = Util.substituteText( hstr, "$file", file.getAbsolutePath() );
      GUIService.infoMessage( null, hstr );

   } catch ( IOException e ) {
      GUIService.failureMessage( "Cannot wipe the file!", e );
   }
}  // secureWipeFile


private static void copyFileList ( File[] flist, File tarDir ) 
             throws ApplicationFailureException, IOException
{
   ContextFile fileTarget, fileSource;
   File f, dirTarget, files[];
   int i;
   
   Util.ensureDirectory( tarDir, null );

   for ( i = 0; i < flist.length; i++ ) {
      f = flist[ i ];
      if ( f.isDirectory() ) {
         // copy directory and sub-content
         dirTarget = new File( tarDir, f.getName() );
         files = f.listFiles();
         Log.log( 5, "(Service.copyFileList) copying directory: " + 
               f.getAbsolutePath() + " --> " + dirTarget.getAbsolutePath() );
         copyFileList( files, dirTarget );
      }
      else {
         // copy single file
         fileTarget = IOManager.makeLocalContextFile( tarDir.getAbsolutePath(), 
                      f.getName() );
         fileSource = IOManager.makeLocalContextFile( f );
         Log.log( 5, "(Service.copyFileList) creating APPLICATION file: "
                      .concat( fileTarget.getFilepath() ) );
         fileSource.copyTo( fileTarget );
         fileTarget.setModifyTime( fileSource.modifyTime() );
      }
   }
}  // copyFileList

/** 
 * Installs a new PORTABLE installation at the given location (should be a
 * removable data device).
 * <p>Convention on input file list is: archive files are copied as is
 * into given installation dir; directories are copied with all their sub-content
 * into given installation dir where the last name segment of the source directory
 * is used as new sub-directory in the installation dir. All archive files
 * copied are attempted to preserve their original time stamp.
 * 
 * @param owner Component parent dialog for messages 
 * @param installDir File installation directory (target system)
 * @param batchDir File if not <b>null</b> target directory for batch files
 *                 (otherwise no batch files created)
 * @param sources  String[] source files to be copied into installation dir
 *                 can be split into source and target name by separator ';'
 *                 (target is extension to installDir)
 * @param sourceIni ContextFile source file for JPWS INI-file (<b>null</b>
 *                  for a default file to be written)
 * @throws IOException 
 */
public static void createPortableInstallation ( Component owner,
                                                File installDir,
                                                File batchDir,
                                                String[] sources,
                                                ContextFile sourceIni
                                               ) throws IOException
{
   ContextFile iniF, iniOut, batchF;
   File file, target;
   List<File> simpleF = new ArrayList<File>();
   List<String> targetF = new ArrayList<String>();
   InputStream in;
   OutputStream out;
   String text, path, s1, s2; 
   int i;
   
   if ( installDir == null  )
      throw new NullPointerException( "missing 'installation' parameter" );
   if ( sources == null )
      throw new IllegalArgumentException( "missing 'sources' parameter" );
   if ( sources.length == 0 )
      throw new IllegalArgumentException( "no source files defined" );

   // test source files for availability (must all exist)
   for ( i = 0; i < sources.length; i++ ) {
	  String src = sources[i];
	  if (src != null) {
		 // filter set of parameter files into simple and complex targets
		 int index = src.indexOf(';');
		 if (index > -1) {
			 // complex
			 targetF.add(src);
			 file = new File( src.substring(0, index) );
		 } else {
			 // simple
		     file = new File( src );
		     simpleF.add(file);
		 }

		 // test if source file exists
		 if ( !file.exists() ) {
	        path = file.getAbsolutePath();
	        throw new IllegalArgumentException( "unavailable source file: "
	               .concat( path ));
	     }
	  }
   }
   
   // prepare install directories
   Util.ensureDirectory( installDir, null );
   if ( batchDir != null ) {
      Util.ensureDirectory( batchDir, null );
   }
   
   // copy simple target application files
   File[] files = simpleF.toArray(new File[simpleF.size()]);
   copyFileList( files, installDir );
   Log.log( 5, "(Service.createPortableInstallation) --- simple files transferred to medium: "
		   + files.length);
   
   // copy complex target application files
   for (String src : targetF) {
	   int index = src.indexOf(';');
	   String trail = src.substring(index+1);
	   file = new File( src.substring(0, index));
	   target = new File( installDir, trail);

	   copyFileList( new File[] {file}, target.getParentFile() );
	   File copy = new File(target.getParentFile(), file.getName());
	   copy.renameTo(target);
	   Log.log( 5, "(Service.createPortableInstallation) --- special target transferred: " 
			   + file + " --> " + target);
   }

   // create target INI file
   iniOut = IOManager.makeLocalContextFile( installDir.getAbsolutePath(),
         Global.OPTIONFILENAME );
   text = ResourceLoader.getDisplay( "msg.install.conflict.inifile" );
   if ( !iniOut.exists() || !GUIService.userConfirm( owner, text ) ) {
      if ( (iniF = sourceIni) == null ) {
         // create blank INI file if parameter void
         Log.log( 5, "(Service.createPortableInstallation) creating BLANK INI-file: "
               .concat( iniOut.getFilepath() ) );
         Options.saveNew( iniOut );
      } else {
         // create copy of INI file if parameter exists
         Log.log( 5, "(Service.createPortableInstallation) copy existing INI-file to: "
               .concat( iniOut.getFilepath() ) );
         iniF.copyTo( iniOut );
      }
   }
   
   // create batch files if opted
   if ( batchDir != null ) {
      String accessPath;
      String batchFileText;

      // determine program access path
      s1 = Util.normalizedPath( installDir.getAbsolutePath(), true );
      s2 = Util.normalizedPath( batchDir.getAbsolutePath(), true );
      accessPath = s1.startsWith( s2 ) ? s1.substring( s2.length() ) : s1;
      
      // Windows batch file
      batchFileText = ResourceLoader.getResourceText( "#standards/jpws-start.bat", "ISO-8859-1" );
      batchFileText = Util.substituteText( batchFileText, "$root", accessPath.replace( '/', '\\' ) );
      if ( batchFileText != null ) {
         batchF = IOManager.makeLocalContextFile( batchDir.getAbsolutePath(), "jpws.bat" );
         Log.log( 5, "(Service.createPortableInstallation) creating batch file: "
                     .concat( batchF.getFilepath() ) );
         out = batchF.getOutputStream();
         in = new ByteArrayInputStream( batchFileText.getBytes( "ISO-8859-1" ) );
         Util.copyStream( in, out );
         out.close();
      }
      
      // Linux/Unix batch file
      batchFileText = ResourceLoader.getResourceText( "#standards/jpws-start.bash", "UTF-8" );
      batchFileText = Util.substituteText( batchFileText, "$root", accessPath );
      if ( batchFileText != null ) {
        batchF = IOManager.makeLocalContextFile( batchDir.getAbsolutePath(), "jpws.bs" );
        Log.log( 5, "(Service.createPortableInstallation) creating batch file: "
                    .concat( batchF.getFilepath() ) );
        out = batchF.getOutputStream();
        in = new ByteArrayInputStream( batchFileText.getBytes( "UTF-8" ) );
        Util.copyStream( in, out );
        out.close();
      }
   }
   
   // report installation success
   text = ResourceLoader.getDisplay( "msg.install.success" );
   text = Util.substituteText( text, "$PATH", installDir.getAbsolutePath() );
   GUIService.infoMessage( owner, "dlg.success", text );
} // createPortableInstallation

/** Returns a local filepath nominator normalised to JPWS requirements
 *  or <b>null</b> if the parameter path was <b>null</b>. An optional default
 *  filename extention come into effect if there is no extention present in
 *  <code>path</code>.
 *  
 * @param path raw filepath (may be <b>null</b>)
 * @param ext an optional default filename extention (may be <b>null</b>)
 * @return normalised filepath
 * @throws IOException if the parameter value does not constitute a valid filepath
 */ 
public static String normalizedFilepath ( String path, String ext )
      throws IOException
{
   if ( path == null )
      return null;
   
   // append default file extention, if void 
   if ( ext != null && path.lastIndexOf( "." ) == -1 )
      path = path + ext;

   return Global.getFilePath( Util.makeFileURL( path ) );
}  // normalizedFilepath

/**
 * Searches for some fixed application paths (defined in "command" resource bundle)
 * and offers user to select one of the available programs.
 * 
 * @return boolean <b>true</b> if and only if program option "browserApplication" has been
 *         modified by the user during execution
 * @since 0-6-0        
 */
@SuppressWarnings("serial")
public static boolean defaultBrowserInstall ()
{
   final ButtonBarDialog dlg;
   JPanel panel;
   JButton browseButton;
   final JTable table;
   final TableModel tableModel;
   ArrayList<String[]> pathEntries;
   String key, keybase, driveLetter, hstr, name, path;
   String user, initBrowser, row[], columns[], grid[][];
   int i, count;
   boolean ok;
   
   // check if option was set before
   initBrowser = Options.getOption( "browserApplication" );
   
   // read available browser-path options into a structure
   // depending on what OS is current
   
   pathEntries = new ArrayList<String[]>();
   keybase = "browserpath.".concat( Global.isWindows() ? "windows." : "unix." ) ;
   driveLetter = Global.isWindows() ? Global.getOSRootPath() : "";
   count = 0;
   user = System.getProperty( "user.name" );
   do {
      key = keybase.concat( String.valueOf( count++ ) );
      hstr = ResourceLoader.getCommand( key );
      ok = !hstr.equals( "FIXME" );
      if ( ok )
      {
         Log.debug( 7, "~~ investigating possible browser path: " + hstr );
         
         // analyse program name and start-path from option text
         if ( (i = hstr.indexOf( ';' )) == -1 )
            continue;
         name = hstr.substring( 0, i );
         path = hstr.substring( i+1 );
         path = Util.substituteText( path, "$user", user );
         path = driveLetter.concat( path );
         Log.debug( 7, "   name = " + name + ", path = " + path );

         // test for program existence and define user option list
         if ( path != null && new File( path ).isFile() )
         {
//            System.out.println( "   ** file exists" );
            
            row = new String[2];
            row[0] = name;
            row[1] = path;
            
            pathEntries.add( row );
         }
      }
   } while ( ok );
   
   // setup and display option table for user
   // this contains a button action allowing to select any file from OS directory
   
   grid = pathEntries.toArray( new String[0][0] );
//      System.out.println( "~~ grid defined with " + grid.length + " rows" );

   // construct table and content
   columns = new String[] { "Browser Name", "Program Path" };
   tableModel = new DefaultTableModel( grid, columns )
   {
      public boolean isCellEditable ( int r, int c ) {
         return false;
      }
   };
   panel = new JPanel( new BorderLayout() );
   table = new JTable( tableModel );
   table.getColumnModel().getColumn(0).setPreferredWidth( 50 );
   table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
   if ( tableModel.getRowCount() == 1 )
      table.setRowSelectionInterval( 0, 0 );
   panel.add( new JScrollPane( table ) );
   table.setPreferredScrollableViewportSize( new Dimension( 480, 200 ) );

   // construct and display user dialog
   dlg = new ButtonBarDialog();
   dlg.setModal( true );
   dlg.setResizable( true );
   dlg.setTitle( ResourceLoader.getDisplay( "dlg.autoinstall.browser" ));
   dlg.setDialogPanel( panel );
   dlg.moveRelatedTo( Global.getActiveFrame() );
   dlg.addButtonBarListener( new DefaultButtonBarListener ( dlg )
   {
      public boolean okButtonPerformed () {
         String browserPath;
         int option;
         
         if ( (option = table.getSelectedRow()) > -1 ) {
            browserPath = (String)tableModel.getValueAt( option, 1 );
            Options.setOption( "browserApplication", browserPath );
//               System.out.println( "~~ SET BROWSER OPTION with: " + browserPath );
         }
         dlg.dispose();
         return true;
      }
   });
   
   // add a browsing button for browser application
   browseButton = new JButton( ResourceLoader.getDisplay( "button.browse" ) );
   browseButton.addActionListener( new ActionListener() {
	   
      @Override
      public void actionPerformed ( ActionEvent e ) {
         int opt = GUIService.editApplicationOption( dlg, "dlg.choosebrowser", "browserApplication"  );
         if ( opt == FileOpenDialog.APPROVE_OPTION ) {
            dlg.dispose();
         }
      }
   });
   dlg.getButtonBar().add( browseButton );
   
   dlg.show();
   return !Options.getOption( "browserApplication" ).equals( initBrowser );
} // defaultBrowserInstall

/** Displays a HTML browser dialog with the content of a list of Java runtime 
    *  environment properties.
    */  
   public static void showSystemInfo ()
   {
      HtmlBrowserDialog systemInfoDlg;
      StringBuffer sbuf;
      Map.Entry<Object,Object> entry;
      TreeSet<Entry<Object,Object>> sort;
      Iterator<Entry<Object,Object>> it;
      ContextFile f;
      String hstr, title, singletonName;
      long time;
      int i;
      boolean special;

      // allow only one system info panel
      if ( Global.isDialogActive( (singletonName = "SystemInfoDialog")) )
         return;
      
      // sort system properties
      sort = new TreeSet<Entry<Object,Object>>( new Comparator<Entry<Object,Object>>()
      {
         @Override
		public int compare ( Map.Entry<Object,Object> k1, Map.Entry<Object,Object> k2 )
         {
            if ( k1 == null | k2 == null )
               throw new IllegalArgumentException();
            return k1.getKey().toString().compareTo( (String)k2.getKey() ); 
         }
      });
      for ( it = System.getProperties().entrySet().iterator(); it.hasNext(); )
         sort.add( it.next() );
      
      // compile text about application globals
      sbuf = new StringBuffer(2048);
      sbuf.append( "<html><body><h3><u>Application Properties</u></h3>" );
      sbuf.append( "<font color=\"blue\"><b>Init Modus</b></font> = " );
      sbuf.append( Global.isPortable() ? "PORTABLE" : "NORMAL" );
      sbuf.append( "<br><font color=\"blue\"><b>OS Modus</b></font> = " );
      sbuf.append( Global.isWindows() ? "WINDOWS" : "UNIX/LINUX/MAC" );
      sbuf.append( "<br><font color=\"blue\"><b>Locale</b></font> = " );
      sbuf.append( Global.getLocaleString() );
      sbuf.append( "<br><font color=\"blue\"><b>LAF</b></font> = " );
      sbuf.append( UIManager.getLookAndFeel().getClass().getName() );
      sbuf.append( "<br><font color=\"blue\"><b>Option File</b></font> = " );
      f = Options.getPersistentFile();
      sbuf.append( f == null ? "- undefined -" : f.getFilepath() );
      for ( i = 0, hstr = ""; i < Global.commandlineArgs.length; i++ )
         hstr = hstr.concat( Global.commandlineArgs[i] ).concat( " " );
      sbuf.append( "<br><font color=\"blue\"><b>Command Line</b></font> = " );
      sbuf.append( hstr );
      if ( Global.isPortable() )
      {
         sbuf.append( "<br><font color=\"blue\"><b>Portable Root</b></font> = " );
         sbuf.append( Global.portableDir.getAbsolutePath() );
      }
      sbuf.append( "<br><font color=\"blue\"><b>Application Home</b></font> = " );
      sbuf.append( Global.applHomeDir.getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>Program Dir</b></font> = " );
      sbuf.append( Global.programDir.getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>Current Dir</b></font> = " );
      sbuf.append( Global.currentDir.getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>Backup Dir</b></font> = " );
      sbuf.append( Global.backDir.getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>Exchange Dir</b></font> = " );
      sbuf.append( Global.exchangeDir.getAbsolutePath() );
      if ( (time = (long)Options.getIntOption( "lastAccessTime" ) * 1000) > 0 )
      {
         sbuf.append( "<br><font color=\"blue\"><b>Previous Session Time: </b></font> = " );
         sbuf.append( Global.getLocalDateTime( time ) );
      }
      if ( !(hstr = Options.getOption( "lastAccessHost" )).isEmpty() )
      {
         sbuf.append( "<br><font color=\"blue\"><b>Previous Session Host: </b></font> = " );
         sbuf.append( hstr );
      }
      if ( !(hstr = Options.getOption( "lastAccessUser" )).isEmpty() )
      {
         sbuf.append( "<br><font color=\"blue\"><b>Previous Session User: </b></font> = " );
         sbuf.append( hstr );
      }
      
      // compile text from system properties
      sbuf.append( "<h3><u>Runtime System Properties</u></h3>" );
      for ( it = sort.iterator(); it.hasNext(); )
      {
         special = false;
         entry = it.next();
         sbuf.append( "<font color=\"maroon\"><b>" );
         hstr = (String)entry.getKey();
         sbuf.append( Util.htmlEncoded( hstr ) ); 
         sbuf.append( "</b></font> = " );
         if ( hstr.startsWith( "user." ) )
         {
            sbuf.append( "<font color=\"green\"><b>" );
            special = true;
         }
         if ( hstr.startsWith( "java.vm." ) || hstr.startsWith( "os." ) )
         {
            sbuf.append( "<font color=\"#005FBF\"><b>" );
            special = true;
         }
         else if ( hstr.startsWith( "java." ) )
         {
            sbuf.append( "<font color=\"black\"><b>" );
            special = true;
         }
         sbuf.append( Util.htmlEncoded( (String)entry.getValue() ) );
         if ( special )
            sbuf.append( "</b></font>" );
         sbuf.append( "<br>" );
      }
      sbuf.append( "#</p></body></html>" );

      // show the results
      title = ResourceLoader.getDisplay( "dlg.systeminfo" );
      systemInfoDlg = new HtmlBrowserDialog( Global.getActiveFrame(), title, false );
      systemInfoDlg.moveRelatedTo( Global.mainFrame );
      systemInfoDlg.markSingleton( singletonName );
      systemInfoDlg.setAutonomous( true );
      if ( Toolkit.getDefaultToolkit().getScreenSize().width > 800 )
         systemInfoDlg.setSize( new Dimension( 600, 400 ) );

      systemInfoDlg.setText( sbuf.toString() );
      systemInfoDlg.show();
   }  // showSystemInfo

   public static void showReleaseNotes () {
      HtmlBrowserDialog dlg;

      ActionHandler.checkForEDT();
      String hstr = ResourceLoader.getDisplay( "dlg.releasenotes" );
      dlg = new HtmlBrowserDialog( Global.getActiveFrame(), hstr, false );
      if ( dlg.markSingleton( "help.releasenotes" ) ) {
         dlg.setSize( 500, 480 );
         dlg.moveRelatedTo( Global.mainFrame );
         hstr = ResourceLoader.getCommand( "html.file.dlg.help.releasenotes" );
         URL page = ResourceLoader.getResourceURL( "#standards/".concat( hstr ));
         try { dlg.setPage( page ); }
         catch ( IOException e )
         { e.printStackTrace(); }
         dlg.show();
      }
   }

   public static void showAboutDialog () {
      // allow only one About panel
      if ( Global.isDialogActive( ("ProgramAboutDialog")) ) return;
      new AboutDialog();
   }
}
