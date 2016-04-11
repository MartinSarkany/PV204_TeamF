/*
 *  Util in org.jpws.front.util
 *  file: Util.java
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.StreamCorruptedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

import org.jpws.front.Global;
import org.jpws.pwslib.crypto.SHA256;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.PassphraseUtils;


/**
 * Various useful functions of general purpose.
 * 
 * @author Wolfgang Keller
 */
public final class Util extends org.jpws.pwslib.global.Util
{
//   private static final byte[] RANDOMBUFFER = org.jpws.pwslib.global.Util.cryptoRand.nextBytes( 8192 );
   public static final TimeZone GMT = TimeZone.getTimeZone("GMT");

   public static final String CRLF = "\r\n";
   
	/** Attempts at destroying the contents of a file safely and persistently 
    *  according to security specific methods. Deletes the parameter file.
    * 
    * @param file the file to be wiped out
    * @throws IOException
    */
   public static synchronized void wipeAFile ( File file ) throws IOException
   {
      RandomAccessFile f;
      byte[] buffer;
      int i, len;
      long length, left;
      
      if ( !file.isFile() )
         return;
      
      f = new RandomAccessFile( file, "rws" );
      length = f.length();
         
      // pass 1
      Util.blankFileSpace( f, 0, length );
      
      // pass 2
      Util.blankFileSpace( f, 0, length );
      
      buffer = new byte[ 8192 ];
      for ( i = 0; i < buffer.length; i++ )
         buffer[ i ] = (byte)0xFF;
      
      // pass 3
      fillFileSpace( f, 0, length, buffer, 0 );

      // pass 4
      fillFileSpace( f, 0, length, buffer, 0 );

      // pass 5
      blankFileSpace( f, 0, length );
   
      // pass 6
      blankFileSpace( f, 0, length );
   
      // pass 7 (fill with random)
      f.seek( 0 );
      left = length;
      while ( left > 0 )
      {
         buffer = org.jpws.pwslib.global.Util.getCryptoRand().nextBytes( 8192 );
         len = (int)Math.min( left, buffer.length );
         f.write( buffer, 0, len );
         left -= len;
      }
      
      // erase file
      f.close();
      file.delete();
   }  // wipeAFile

  /** Attempts to calculate the storage size of a file directory.
   * 
   * <p> Since the operation is non-atomic, the returned value may be inaccurate. 
   * However, this method is quick and does its best.
   * 
   * @param path <code>Path</code> the directory denomination
   * @param clusterSize int target file system cluster size, if 0 then only
   *        plain file sizes are summed up, otherwise occupied device space
   * @return long size in bytes of directory, traversing
   */
  public static long directorySize (File path, int clusterSize) {
	  File[] files = path.listFiles();
	  if (files == null) return 0;
	  
	  long sum = 0;
	  for (File f : files) {
		  if (f.isFile()) {
			  long length = f.length();
			  if (clusterSize > 0) {
				  long secs = length / clusterSize;
				  if (length % clusterSize > 0) secs++;
				  length = secs * clusterSize;
			  }
			  sum += length;

		  } else if (f.isDirectory()) {
			  sum += directorySize(f, clusterSize);
		  }
	  }
	  
	  return sum;
//        final AtomicLong size = new AtomicLong(0);
//        try
//        {
//            Files.walkFileTree (path, new SimpleFileVisitor<Path>() 
//            {
//                @Override public FileVisitResult 
//                visitFile(Path file, BasicFileAttributes attrs) {
//                        size.addAndGet (attrs.size());
//                        return FileVisitResult.CONTINUE;
//                    }
//
//                @Override public FileVisitResult 
//                visitFileFailed(Path file, IOException exc) {
//                        System.out.println("skipped: " + file + " (" + exc + ")");
//                        // Skip folders that can't be traversed
//                        return FileVisitResult.CONTINUE;
//                    }
//
//                @Override public FileVisitResult
//                postVisitDirectory (Path dir, IOException exc) {
//                        if (exc != null)
//                            System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
//                        // Ignore errors traversing a folder
//                        return FileVisitResult.CONTINUE;
//                    }
//            });
//
//        } catch (IOException e) {
//            throw new AssertionError ("walkFileTree will not throw IOException if the FileVisitor does not");
//        }
//        return size.get();
    }

   /** Fills a specified section of the parameter file with zero (byte 0) values.
    * 
    * @param f <code>RandomAccessFile</code>
    * @param off start offset in file of blank section; if below 0 the function works
    *        from the current file pointer position 
    * @param length length of blank section
    * @throws IOException
    */   
   public static final void blankFileSpace ( RandomAccessFile f, long off, long length )
   throws IOException
   {
      fillFileSpace( f, off, length, new byte[ 2048 ], 0 );
   }

   /** Fills a specified section of the parameter file with a given pattern value.
    * 
    * @param f <code>RandomAccessFile</code>
    * @param off start offset in file of destined pattern section; if below 0 the 
    *        function works from the current file pointer position 
    * @param length length of pattern section
    * @param pattern byte array containing a cyclic pattern
    * @param cyclus length of the pattern element; if greater 0 then <code>length</length> 
    *        must obey <code>length % cyclus == 0</code> 
    * @throws IOException
    * @throws IllegalArgumentException if there is a mismatch in parameters
    */   
   public static final void fillFileSpace ( RandomAccessFile f, 
                                            long off, 
                                            long length,
                                            byte[] pattern,
                                            int cyclus )
   throws IOException
   {
      int len;

      if ( cyclus > 0 )
      {
         if ( length % cyclus > 0 )
            throw new IllegalArgumentException( "length / cyclus mismatch" );
         if ( pattern.length % cyclus > 0 )
            throw new IllegalArgumentException( "pattern / cyclus mismatch" );
      }
      
      if ( off >= 0 )
         f.seek( off );
      
      while ( length > 0 )
      {
         len = (int)Math.min( length, pattern.length );
         f.write( pattern, 0, len );
         length -= len;
      }
   }  // fillFileSpace

   /** Makes a scrambles of the user record (buffer) over the specified length only.
    *  This works resembling to a mirror and can both en-scatter and de-scatter a set 
    *  of data, depending on the parameter switch <code>enscatter</code>.
    *  <p>(Note that this algorithm is not bound onto a cyclic block length, while 
    *  the cipher is.)
    *  
    *  @param buffer
    *  @param length
    *  @param enscatter boolean switch of the transformation direction. 
    *         <b>true</b> = enscatter, <b>false</b> = descatter
    */
   public static void scatter ( byte[] buffer, int length, boolean enscatter )
   {
      int i,j,k,len, plo, phi, shift, loops, mod, offset;
      byte x;
   
      // this scatters a series of blocks of 16 bytes length (analogic to encryption)
      // the last block may be of any lower size
      
      shift = enscatter ? 13 : -13;
      loops = length / 16;
      mod = length % 16;
      if ( mod > 0 )
         loops++;
      k = 15;
      len = 4;
      offset = 0;
   
      for ( j = 0; j < loops; j++ )
      {
         if ( j == loops-1 && mod > 0 )
         {
            k = mod-1;
            len = mod/4;
         }
         for ( i=0; i<len; i++ )
         {
            plo = i * 2;
            phi = k - plo;
            x = buffer[ offset+plo ];
            buffer[ offset+plo ] = (byte)(buffer[ offset+phi ] + shift);
            buffer[ offset+phi ] = (byte)(x + shift);
         }
         offset += 16;
      }
   }  // enscatter
   
   /**
    * Calculates the X and Y coordinates that a child component should have to be centred
    * within its parent.  This method does not try to constrain the calculation so that
    * the point remains on visible screen.
    * 
    * @param rec  the bounding rectangle of the parent.
    * @param dim  the dimensions of the child.
    * 
    * @return the X and Y coordinates the child should have.
    */
   public static Point centredWindowLocation( Rectangle rec, Dimension dim )
   {
       Point   pos;

       pos     = new Point();
       pos.x   = rec.x + ((rec.width - dim.width) >> 1);
       pos.y   = rec.y + ((rec.height - dim.height) >> 1);
       
       return pos;
   }

   /**
    * Centres the child component within its parent. Does nothing
    * if <code>child</code> is <b>null</b>.
    * 
    * @param parent  the parent component; <b>null</b> defaults to system screen
    * @param child   the child component; may be <b>null</b>
    */
   public static void centreWithin( Component parent, Component child )
   {
      Rectangle win;
     
      if ( child == null ) return;
      
     // get system screen if no parent
     if ( parent == null ) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        win = new Rectangle( screen ); 
     }
     // otherwise parent window
     else
        win = parent.getBounds();
     
     Point p = centredWindowLocation( win, child.getSize() );
     child.setLocation( p );
   }

   /** Sets the location of a window c to screen point p by correcting point
    *  p if necessary to ensure that the window is as fully visible as possible.
    *  Does nothing if any of the parameters is <b>null</b>.
    * 
    *  @param c <tt>Component</tt> window to check
    *  @param p <tt>Point</tt> intended position of window (upper left corner)
    */
   public static void setCorrectedLocation ( Component c, Point p )
   {
      Dimension screen, win;
      int diff;
      
      if ( c == null | p == null )
         return;
      
      screen = Toolkit.getDefaultToolkit().getScreenSize();
      win = c.getSize();
      if ( (diff = screen.width - p.x - win.width) < 0 )
         p.x += diff;
      if ( (diff = screen.height - p.y - win.height) < 0 )
         p.y += diff;
      p.x = Math.max( 0, p.x );
      p.y = Math.max( 0, p.y );
      c.setLocation( p );
   }

   /**
    * Defines new window bounds (location + dimension) by interpreting the relation
    * of a window <tt>win</tt> to the size of the current system screen (primary display).
    * Window locations are expressed in coordinates of the screen.
    *    
    * @param win Rectangle window size and position in screen coordinates
    * @param resizable boolean if <b>true</b> the window will get resized if it
    *        exceeds screen limits
    * @param clipping boolean if <b>true</b> it is ok if only a part of the window
    *        is visible on the screen while its top-bar remains functional
    * @return Rectangle new bounds definition (potentially corrected) of window <tt>win</tt> 
    */
   public static Rectangle correctedWindowBounds ( Rectangle win, boolean resizable, boolean clipping )
   {
      Dimension screen;
      
      screen = Toolkit.getDefaultToolkit().getScreenSize();
      screen.height -= 30;
      return correctedWindowBounds( screen, win, resizable, clipping );
   }
/**
    * Defines new window bounds (location + dimension) by interpreting the relation
    * of a window <tt>win</tt> to a screen dimension <tt>screen</tt>. Window locations 
    * are expressed in coordinates of the screen. Returns <b>null</b> if
    * window was <b>null</b>.
    *    
    * @param screen Dimension the screen dimensions in x and y coordinates;
    *               <b>null</b> defaults to system screen
    * @param win Rectangle window size and position in screen coordinates
    * @param resizable boolean if <b>true</b> the window will get resized if it
    *        exceeds screen limits
    * @param clipping boolean if <b>true</b> it is ok if only a part of the window
    *        is visible on the screen while its top-bar remains functional
    * @return Rectangle new bounds definition (potentially corrected) of window <tt>win</tt>
    *         or <b>null</b> if <tt>win</tt> was <b>null</b>
    */
   public static Rectangle correctedWindowBounds ( Dimension screen, Rectangle win, 
                                             boolean resizable, boolean clipping )
   {
      Rectangle result;
      Dimension size;
      Point loc;
      int minVisHig, minVisWid, diff, minHeight, minWidth;
   
      if ( win == null )
         return null;
      
      // get system screen if no screen supplied
      if ( screen == null )
         screen = Toolkit.getDefaultToolkit().getScreenSize();
      
      size = win.getSize();
      loc = win.getLocation();
      result = win.getBounds();
      minVisHig = 50; minVisWid = 75;
      
      // correct window size if opted and necessary
      if ( resizable )
      {
         if ( size.height > screen.height )
            size.height = screen.height;
         if ( size.width > screen.width )
            size.width = screen.width;
         result.setSize( size );
      }
      
      // correct window location if necessary
      // correct top window bar
      minHeight = clipping ? minVisHig : result.height;
      if ( (diff = screen.height - loc.y - minHeight) < 0 )
         loc.y += diff;
      loc.y =  Math.max( loc.y, 0 );

      minWidth = clipping ? minVisWid : result.width;
      if ( (diff = screen.width - loc.x - minWidth) < 0 )
         loc.x += diff;
      loc.x =  Math.max( loc.x, 0 );
      
      result.setLocation( loc );
      return result;
   }
   
   /**
    * Returns the file or directory name part of the file/directory path
    * given with the parameter.
    *  
    * @param path input file or directory path, may be <b>null</b>
    * @return file/directory name or empty string
    */
   public static String fileNameOfPath ( String path )
   {
      File f;
      
      if ( path == null )
         return "";
      
      f = new File( path );
      return f.getName();
   }
   
   /**
    * Returns the parent directory path of the file/directory path
    * given with the parameter.
    *  
    * @param path input file or directory path, may be <b>null</b>
    * @return parent path or empty string if no parent is noted
    */
   public static String pathNameOfPath ( String path )
   {
      File f;
      String hstr;
      
      if ( path == null )
         return "";
      
      f = new File( path );
      hstr = f.getParent();
      return hstr == null ? "" : hstr;
   }
   
   /** Substitutes conflicting characters in <code>text</code> with html
    *  masked expressions. The returned string is displayable in html interpreting
    *  components (but comes without "html"-flag).
    * 
    * @param text
    * @return String
    */
   public static String htmlEncoded ( String text )
   {
      StringBuffer b;
      int i, len;
      char c;
      
      if ( text == null )
         return null;
      
      len = text.length();
      b = new StringBuffer( len * 3 );
      for ( i = 0; i < len; i++ )
      {
         c = text.charAt( i );
         switch ( c )
         {
         case '<':  b.append( "&lt;" );
         break;
         case '>':  b.append( "&gt;" );
         break;
         case '&':  b.append( "&amp;" );
         break;
         case '"':  b.append( "&quot;" );
         break;
         case '\n':  b.append( "<br>" );
         break;
         case '\r':  
         break;
         default: b.append( c );
         }
      }
      return b.toString();
   }
   
   /** Copies the contents of any disk file to a specified output file.  If the
    *  output file is a relative path, it is made absolute against the directory
    *  of the input file.  The output file will be overwritten if it exist.
    *  A CRC32 check is performed to compare source and copy after the copy process
    *  and if results negative a <code>StreamCorruptedException</code> is thrown.
    *  Function reports errors to <code>System.err</code>.
    *  The filetime of output will be the same as of input.
    *  
    *  @param inputFile a File object
    *  @param outputFile a File object
    *  @throws java.io.IOException if the function could not be completed
    *  because of an IO or CRC check error
    */
   public static void copyFile( File inputFile, File outputFile )
                                   throws java.io.IOException
   {
      File parent;
      FileInputStream in = null;
      FileOutputStream out = null;
      CRC32 crcSum;
      int writeCrc;
      long time;

      // control parameter
      if ( inputFile == null | outputFile == null )
         throw new IllegalArgumentException( "null pointer" );
      if ( inputFile.equals( outputFile ) )
         throw new IllegalArgumentException( "illegal self reference" );

      byte[] buffer = new byte[2048];
      int len;

      try {
         // make output file absolute (if not already)
         parent=inputFile.getAbsoluteFile().getParentFile();
         if ( !outputFile.isAbsolute() )
            outputFile = new File( parent, outputFile.getPath() );

         // make sure the directory for the output file exists
         ensureFilePath( outputFile, parent );

         // create file streams
         in = new FileInputStream(inputFile);
         out = new FileOutputStream(outputFile);
         time = inputFile.lastModified();
         crcSum = new CRC32();

         while ((len = in.read(buffer)) != -1)
            {
            out.write(buffer, 0, len);
            crcSum.update( buffer, 0, len );
            }

         in.close();
         out.close();
         writeCrc = (int)crcSum.getValue();
         outputFile.setLastModified( time );

         // control output CRC
         in = new FileInputStream( outputFile );
         crcSum.reset();
         while ((len = in.read(buffer)) != -1)
            crcSum.update( buffer, 0, len );
         if ( writeCrc != (int)crcSum.getValue() )
            throw new StreamCorruptedException( "bad copy CRC" );
         }

      catch (IOException e)
         {
         System.err.println(
            "*** error during file copy: " + outputFile.getAbsolutePath());
         System.err.println(e);
         throw e;
         }
      finally
         {
         if ( in != null )
            in.close();
         if ( out != null )
            out.close();
         }
   } // copyFile

   /** Ensures the existence of the directory which may be part of the path
    *  specification of parameter <code>file</code>. If the specified  file is a
    *  relative path, it is made absolute against <code>defaultDir</code>. If
    *  <code>defaultDir</code> is <b>null</b> the System property "user.home" is
    *  assumed as default directory. Does nothing if <code>file</code> is <b>null</b>
    *  (return == <b>false</b>).
    *  
    *  @return <b>true</b> if and only if the parent directory of the specified file
    *  exists after this function terminates
    */
   public static boolean ensureFilePath ( File file, File defaultDir )
   {
      File parent;

      if ( file == null )
         return false;
      
      if ( (parent=file.getParentFile()) == null )
      {
         parent = defaultDir;
         defaultDir = null;
      }
      return ensureDirectory( parent, defaultDir );
   }  // ensureFilePath

   /** Ensures the existence of the directory specified by the parameter. If the
    *  directory does not exist, an attempt is performed to create it including all
    *  necessary parent directories that may be implied by the specification.
    *  Does nothing if <code>dir</code> is <b>null</b> (return == <b>false</b>).
    *  
    *  @param dir File specifying the intended directory; if the specified
    *  path is a relative path, it is made absolute against <code>defaultDir</code>.
    *  
    *  @param defaultDir File base directory in case <code>dir</code> specified a
    *         relative path. If <code>defaultDir</code> is <b>null</b> the System 
    *         directory "user.home" is assumed.
    *         If <code>defaultDir</code> is a relative path it is made absolute
    *         against the "user.home" directory.  
   *   
    *  @return <b>true</b> if and only if the specified file exists after execution 
    *          of this method and is a directory 
    */
   public static boolean ensureDirectory ( File dir, File defaultDir )
   {
      File homeDir; 
      boolean success = true;

      if ( dir == null )
         return false;

      if ( !dir.isAbsolute() )
      {
         // adjust default directory if necessary
         homeDir = new File (System.getProperty( "user.home" ));
         if ( defaultDir == null )
            defaultDir = homeDir;
         else if ( !defaultDir.isAbsolute() )
            defaultDir = new File( homeDir, defaultDir.getPath() );

         // resolve against default directory
         dir = new File( defaultDir, dir.getPath() );
      }

      if ( !dir.isDirectory() )
      {
         try {
            success = !dir.isFile() && dir.mkdirs();
         } catch ( SecurityException e )
         {
            e.printStackTrace();
            success = false;
         }
         if ( !success )
            System.err.println("** (Util.ensureDirectory) failed while trying to create directory: "+ dir.toString() );
      }

      return success;
   }  // ensureDirectory

   public static int textVariance ( char[] ca )
   {
      BitSet set = new BitSet();
      int i;
      
      for ( i = 0; i < ca.length; i++ )
         set.set( ca[i] );
      return set.cardinality();         
   }

   /**
    * Whether the parameter URL is denoting a file protocol.
    * @param url
    * @return <b>true</b> if and only if url is not <b>null</b> and
    *         its protocol part equals to "file"
    */
   public static boolean isFileProtocol ( URL url )
   {
      return url != null && url.getProtocol().equals( "file"); 
   }

   /**
    * Whether the parameter filepath is denoting a file protocol.
    * @param path String file path
    * @return <b>true</b> if and only if path is not <b>null</b> and
    *         not malformed and its URL equivalent renders a "file" protocol
    * @since 0-5-0
    */
   public static boolean isFileProtocol ( String path )
   {
      try { return isFileProtocol( makeFileURL( path ) ); }
      catch ( Exception e )
      { 
         e.printStackTrace();
         return false; 
      }
   }

   /** Replaces '\' with '/' and ensures a trailing '/' on directory paths.
    *  <br>(Appends nothing if <tt>filepath</tt> is empty.) 
    * @since 0-5-0
    */
   public static String normalizedPath ( String filepath, boolean isDirectory )
   {
      String hstr;
      
      hstr = filepath.replace( '\\', '/' );
      if ( isDirectory && hstr.length() > 0 && !hstr.endsWith("/") )
         hstr = hstr.concat( "/" );
      return hstr;
   }
   
   /** Returns a string containing all digits of the parameter string in their
    * original series but with eliminated non-digit material.
    * 
    * @param v String input text
    * @param radix identifies the number system (0..16)
    * @return String digit text
    */
   public static String condensedNumber ( String v, int radix )
   {
      StringBuffer sbuf;
      String matrix, test;
      int i;
      
      if ( radix < 0 | radix > 16 )
         throw new IllegalArgumentException( "radix invalid" );
      
      matrix = "0123456789abcdef".substring( 0, radix );
      test = v.toLowerCase();
      sbuf = new StringBuffer( v.length() );
      for ( i = 0; i < test.length(); i++ )
      {
         if ( matrix.indexOf( test.charAt( i ) ) > -1 )
            sbuf.append( v.charAt( i ) );
      }
      return sbuf.toString();
   }
   
   /** Returns a number representation of the parameter integer value.
    *  The minimum length of the number is guaranteed by leading '0'
    *  characters.   
    * 
    * @param v long integer value
    * @param length minimum length of number string 
    * @return String number
    */
   public static String number ( long v, int length )
   {
      StringBuffer sbuf;
      String hstr;
      int i, n;
      
      sbuf = new StringBuffer( length );
      hstr = String.valueOf( v );
      n = length - hstr.length();
      for ( i = 0; i < n; i++ )
         sbuf.append( '0' );
      sbuf.append(  hstr );
      return sbuf.toString();
   }
   
   /**
    * Returns the standard time string for the user's (VM) time zone.
    * 
    * @param time long universal time value in epoch milliseconds
    * @return String formated time string (length == 19)
    * @since 0-4-0
    */
   public static String standardTimeString ( long time )
   {
      return standardTimeString( time, TimeZone.getDefault() );
   }
   
   /**
    * Returns the standard time string for the given epoch time and time zone.
    * 
    * @param time long universal time value in epoch milliseconds
    * @param tz <code>TimeZone</code> for which to interpret the time
    *        or <b>null</b> for the current default time zone 
    * @return String formated time string (length == 19)
    */
   public static String standardTimeString ( long time, TimeZone tz )
   {
      GregorianCalendar cal;
      StringBuffer sbuf;
      
      if ( tz == null )
         tz = TimeZone.getDefault();
      
      cal = new GregorianCalendar( tz );
      cal.setTimeInMillis( time );
      sbuf = new StringBuffer(20);
      sbuf.append( number( cal.get( GregorianCalendar.YEAR ), 4 ) );
      sbuf.append( '-' );
      sbuf.append( number( cal.get( GregorianCalendar.MONTH ) + 1, 2 ) );
      sbuf.append( '-' );
      sbuf.append( number( cal.get( GregorianCalendar.DAY_OF_MONTH ), 2 ) );
      sbuf.append( ' ' );
      sbuf.append( number( cal.get( GregorianCalendar.HOUR_OF_DAY ), 2 ) );
      sbuf.append( ':' );
      sbuf.append( number( cal.get( GregorianCalendar.MINUTE ), 2 ) );
      sbuf.append( ':' );
      sbuf.append( number( cal.get( GregorianCalendar.SECOND ), 2 ) );
      return sbuf.toString();
   }  // standardTimeString
   
   /**
    * Returns a technical time string apt for collation purposes
    * for the user's (VM) time zone.
    * 
    * @param time long universal time value in epoch milliseconds
    * @return String time string (length == 14)
    */
   public static String technicalTimeString ( long time )
   {
      return technicalTimeString( time, null );
   }
   
   /**
    * Returns a technical time string apt for collation purposes.
    * 
    * @param time long universal time value in epoch milliseconds
    * @param tz <code>TimeZone</code> for which to interpret the time
    *        or <b>null</b> for the current default time zone 
    * @return String time string (length == 14)
    */
   public static String technicalTimeString ( long time, TimeZone tz )
   {
      GregorianCalendar cal;
      StringBuffer sbuf;
      
      if ( tz == null )
         tz = TimeZone.getDefault();
      
      cal = new GregorianCalendar( tz );
      cal.setTimeInMillis( time );
      sbuf = new StringBuffer(20);
      sbuf.append( number( cal.get( GregorianCalendar.YEAR ), 4 ) );
      sbuf.append( number( cal.get( GregorianCalendar.MONTH ) + 1, 2 ) );
      sbuf.append( number( cal.get( GregorianCalendar.DAY_OF_MONTH ), 2 ) );
      sbuf.append( number( cal.get( GregorianCalendar.HOUR_OF_DAY ), 2 ) );
      sbuf.append( number( cal.get( GregorianCalendar.MINUTE ), 2 ) );
      sbuf.append( number( cal.get( GregorianCalendar.SECOND ), 2 ) );
      return sbuf.toString();
   }  // standardTimeString
   
   /**
    * Returns the human readable time string for the given epoch time, 
    * expressed in the current VM timezone and locale.
    *  
    * @param time long universal time value in epoch milliseconds
    * @return String formatted time string (variable length)
    */ 
   public static String localeTimeString ( long time )
   {
      return new Date( time ).toLocaleString();
   }
   
   /** Returns the topmost characters of the parameter string
    *  or the parameter itself. If the string is shortened,
    *  it will be constantly lead by three dots. 
    * 
    * @param v String input
    * @param max maximum length the returned string shall assume
    * @return String either v or a bottom shortened string of v
    */ 
   public static String topMostStr ( String v, int max )
   {
      if ( max < 3 )
         throw new IllegalArgumentException( "max must be >= 3" );
      
      int len = v.length();
      if ( len > max )
         v = "...".concat( v.substring( len-max+3 ) );
      return v;
   }
   
   /** Returns the leading characters of the parameter string
    *  or the parameter itself. If the string is shortened,
    *  it will be trailed by three dots. 
    * 
    * @param v String input
    * @param max maximum length the returned string shall assume
    * @return String either v or a bottom shortened string of v
    */ 
   public static String leadingStr ( String v, int max )
   {
      if ( max < 3 )
         throw new IllegalArgumentException( "max must be >= 3" );
      
      int len = v.length();
      if ( len > max )
         v = v.substring( 0, Math.max(len-max-3, 0) ).concat("...");
      return v;
   }
   
   /**
    * Returns a XML standardized time string for the given epoch time 
    * in the GMT (UT) time zone.
    * 
    * @param time epoch milliseconds
    * @return String XML formated time string (length == 19)
    */
   public static String xmlTimeString ( long time )
   {
      String hstr;
      
      hstr = standardTimeString( time, GMT );
      return substituteText( hstr, " ", "T" );
   }   
   
   /**
    * Extracts a <code>long</code> time value from a given text string
    * formatted according to standard time format.
    *   
    * @param input standard time formatted text
    * @param tz the <code>TimeZone</code> of the input value or <b>null</b>
    *        for the current default time zone 
    * @return long time value or -1 if parse failed
    */
   public static long timeFromString ( String input, TimeZone tz )
   {
      Calendar cal;
      StringBuffer sbuf;
      String hstr;
      int i, year, month, day, hour, min, sec, len;
      char c;
      
      if ( tz == null )
         tz = TimeZone.getDefault();
      
      // extract date value (only digits, ignore other types)
      hstr=input;
      sbuf = new StringBuffer( hstr.length() );
      for ( i = 0; i < hstr.length(); i++ )
         if ( Character.isDigit( (c=hstr.charAt( i )) ) )
            sbuf.append( c );
      hstr = sbuf.toString();   
      len = hstr.length();
         
      // control plausibility
      if ( len != 8 & len != 12 & len != 14 )
      {
         return -1;
      }

      // construct a date from option value
      year = Integer.parseInt( hstr.substring( 0, 4 ) );
      month = Integer.parseInt( hstr.substring( 4, 6 ) );
      day = Integer.parseInt( hstr.substring( 6, 8 ) );
      hour = min = sec = 0;
      if ( len >= 12 )
      {
         hour = Integer.parseInt( hstr.substring( 8, 10 ) );
         min = Integer.parseInt( hstr.substring( 10, 12 ) );
      }
      if ( len == 14 )
         sec = Integer.parseInt( hstr.substring( 12, 14 ) );

      cal = new GregorianCalendar( tz );
      cal.set( year, month-1, day, hour, min, sec );
      return cal.getTimeInMillis() / 1000 * 1000;
   }  // timeFromString
   
   /**
    * Returns the millisecond time value of the time marker within
    * a SEC (security) backup file path. (The marker text is 
    * interpreted as of the GMT timezone.)
    * 
    * @param path filepath of a SEC- marked backup file
    * @return long time value (milliseconds) or -1 if no valid time value found
    * @since 0-5-0
    */
   public static long timeFromSECPath ( String path )
   {
      try {
         int index = path.lastIndexOf( "SEC-" );
         String hstr = path.substring( index+4, index+19 );
         return Util.timeFromString( hstr, Util.GMT );
      } catch (Exception e)
      {
         return -1;
      }
   }
   
   /**
    * Returns the parsed long value from parameter text or 0 if not successful.
    * 
    * @param val String text containing number (may be <b>null</b>)
    * @return parsed long value or 0 if text was <b>null</b> or unreadable
    */
   public static long longFromString ( String val )
   {
      long i;

      i = 0;
      if ( val != null )
         try { i = Long.parseLong( val ); }
         catch ( Exception e )
         {}
      return i;
   }  // longFromString
   
   /**
    * Extracts the first occurrence of a valid URL within the given
    * text string as a java.net.URL object.
    * 
    * @param text url specification or <b>null</b> 
    * @return java.net.URL or <b>null</b> if no valid URL was found
    * @since 0-4-0
    */
   public static URL extractURL ( String text )
   {
      URL url;
      String arr[];
      
      if ( text != null )
      {
         text = reducedText( text, "\r\n()", true );
         arr = text.split( " " );
         
         for ( String h : arr ) 
            try {
               url = new URL( h );
               return url;
            }
         catch ( MalformedURLException e )
         {}
      }
      return null;
   }
   
   /**
    * Extracts all occurrences of valid URLs within the given
    * text string as an array of java.net.URL objects. Multiple identical
    * URLs are returned as one URL.
    * 
    * @param text String text to be investigated; may be <b>null</b> 
    * @return Array of java.net.URL or <b>null</b> if no valid URL was found
    * @since 0-6-0
    */
   public static URL[] extractURLs ( String text )
   {
      ArrayList<URL> list = new ArrayList<URL>();
      URL url;
      String hstr;
      int i;
      
      while ( (url=extractURL( text )) != null )
      {
         // add the first occurrence of URL to array
         list.add( url );

         // proceed to next 
         hstr = url.toString();
         i = text.length();
         text = Util.substituteText( text, hstr, "" );
         if ( i == text.length() )
            break;
      }
      return list.isEmpty() ? null : list.toArray(new URL[list.size()]);
   }
   
   public static long boundsToLong ( Rectangle bounds )
   {
      Dimension dim;
      Point pos;
      int a, b;
      
      dim = bounds.getSize();
      a = (dim.width << 16) | (dim.height & 0xFFFF);
      pos = bounds.getLocation();
      b = (pos.x << 16) | (pos.y  & 0xFFFF);
      
      return (long)a << 32 | b;
   }
   
   public static Rectangle longToBounds ( long v )
   {
      Rectangle bounds;
      int a, b;
      
      a = (int)(v >> 32);
      b = (int)v;
      bounds = new Rectangle( new Point( b >> 16, (short)b ), new Dimension( a >> 16, (short)a ) );
      return bounds;
   }
   
   //  ************  INNER CLASSES  *********************+
   
   public static class CSV
   {

      /**
       * Quotes a text string according to CSV rules (RFC 4180).
       * Any input is allowed. The returned string is extended in length by
       * at least 2 and starts and ends with '"' characters. The quoting
       * occurs unconditional and no evaluation is performed whether quoting
       * is required for the input. 
       * 
       * @param s String input text
       * @return quoted text version or <b>null</b> if input was <b>null</b>
       */
      public static String quoteText( String s  )
      {
         StringBuffer sb;
         int i, len;
         char c;
         
         if ( s == null )
            return null;
      
         len = s.length();
         sb = new StringBuffer( len );
         sb.append( '"' );
         
         for ( i=0; i < len; i++ )
         {
            c = s.charAt( i );
            
            if ( c == '"' )
               sb.append( '"' );
            sb.append( c );
         }      
      
         sb.append( '"' );
         return sb.toString();
      }  // CSV_quoteText

      /**
       * Searches the quote end from a specified start position in a text string.
       * Function returns the next index position in the input string after the first
       * occurence of an unescaped quote character ('"'). If the end of string is reached,
       * -1 is returned. 
       *    
       * @param s string to be investigated
       * @param start index in s to start search (must be greater zero - opening quote) 
       * @return index position after first valid end quote 
       * @throws IllegalArgumentException if start is zero or exceeds index range
       */
      public static int searchQuoteEnd ( String s, int start )
      {
         int i, len, lastPos;
         
         len = s.length();
         lastPos = len - 1;
         if ( start <= 0 | start >= len )
            throw new IllegalArgumentException( "illegal start value" );
         
         for ( i = start; i < len; i++ )
         {
            if ( s.charAt( i ) == '"' )
            {
               // found end pos condition
               if ( i == lastPos || s.charAt( i + 1 ) != '"' )
                  return i+1;

               // skip double " 
               i++;
            }
         }
         return -1;
      } // searchQuoteEnd

      /**
       * Unquotes a quoted text string according to CSV rules (RFC 4180).
       * The input must be quoted in '"' characters, otherwise an exception
       * is thrown.
       * 
       * @param s input text
       * @return unquoted text version or <b>null</b> if input was <b>null</b>
       * @throws IllegalArgumentException if input is not quoted 
       */
      public static String unquoteText( String s )
      {
         StringBuffer sb;
         String hstr;
         int i, len;
         char c;
         
         if ( s == null )
            return null;
         
         s = s.trim();
         len = s.length();
         
         // control if input is quoted
         if ( len < 2 || s.charAt(0) != '"' || s.charAt(len-1) != '"' )
            throw new IllegalArgumentException( "unquoted text" );
         
         // return shortened string if no escapes
         hstr = s.substring( 1, len-1 );
         if ( hstr.indexOf('"') == -1 )
            return hstr;
         
         // walk through value otherwise
         sb = new StringBuffer( len );
         for ( i=1; i < len-1; i++ )
         {
            c = s.charAt( i );
            if ( c == '"' && i < len-2 )
            {
               if ( s.charAt( i+1 ) == '"' )
                  i++;
            }
            
            sb.append( c );
         }
         
         return sb.toString();
      }  // unquoteText

      /**
       * Creates a text string containing a series of CSV encoded String values
       * which are given by the parameter String array.
       * All string values are allowed. A <b>null</b> value in one of the array
       * fields is equivalent to an empty string. The result may be seen as a valid
       * CSV line but is not yet terminated by CRLF.
       * <p><u>Note:</u> The rules of RFC-4180 are followed for construction. 
       * The following cases result in quoted fields:
       * value contains one of {separator, double-quote ('"'), LF} or value starts
       * or ends with a blank ("blank rule").  
       *  
       * @param values String array of the values to be encoded (not <b>null</b>)
       * @param separator the separator character between resulting field values 
       *        (may not be one of ' ', '"', CR, LF) 
       *        
       * @return String of CSV encoded text fields 
       * @throws IllegalArgumentException if <code>separator</code> is illegal
       */
      public static String encodeLine ( String[] values, char separator )
      {
         StringBuffer outbuf;
         String field;
         int i;
         boolean contains, blankRule, quoting;
         
         if ( separator == ' ' | separator == '"' | separator == '\r' 
              | separator == '\n')
            throw new IllegalArgumentException("illegal separator");
         
         outbuf = new StringBuffer( 256 );
         for ( i = 0; i < values.length; i++ )
         {
            // convert null value to empty string
            if ( (field = values[ i ]) == null )
               field = "";
            
            // decide on quoting
            quoting = false;
            if ( field.length() > 0 )
            {
               contains = field.indexOf( separator ) > -1 || 
                       field.indexOf( '"' ) > -1 || 
                       field.indexOf( "\n" ) > -1; 
               blankRule = field.startsWith( " " ) || field.endsWith( " " );         
               quoting = contains | blankRule;
            }
            
            // write field
            if ( quoting )
               field = quoteText( field );
            outbuf.append( field );
            
            // write field separator except on last element
            if ( i < values.length-1 )
               outbuf.append( separator );
         }
         
         return outbuf.toString();
      }  // encodeLine
   
      /**
       * Decodes a line of CSV encoded field values into an array of <code>String</code>s.
       * If <code>input<code> is an empty string, <code>start</code> parameter is ignored.
       * 
       * @param input CSV encoded text (not <b>null</b>)
       * @param start offset in <code>input</code> to start interpretation
       * @param separator character used as field separator in <code>text</code> 
       * @param endpos a <code>BufferInt</code> instance to receive output value;
       *        value indicates last-read position + 1 in <code>input</code> 
       *  
       * @return array of <code>String</code> values, each consisting of a
       *         decoded field 
       * @throws IllegalArgumentException if <code>start</code> is out of range or
       *         if <code>separator</code> is illegal
       */
      public static String[] decodeLine ( String input, int start, char separator,
            BufferInt endpos )
      {
         ArrayList<String> list;
         String field;
         int end, pos, mark, slen;
         char c1;
         boolean eol;
         
         slen = input.length();

         if ( separator == ' ' | separator == '"' | separator == '\r' 
            | separator == '\n')
          throw new IllegalArgumentException("illegal separator");
       
         if ( input.length() == 0 )
            return new String[0];

         if ( start < 0 | start > slen-1 )
            throw new IllegalArgumentException( "illegal start position" );
         
         list = new ArrayList<String>();
         pos = start;
         
         eol = false;
         c1 = 0;
         while ( !eol )
         {
            // READ ONE FIELD
            // interpret first char
            c1 = input.charAt( pos );

            // if field is (expected to be) quoted
            if ( c1 == '"' )
            {
               // we expect to be at start of field here
               if ( pos == slen-1 )
                  throw new IllegalStateException();

               // search end of field (end-quote +1)
               end = searchQuoteEnd( input, pos+1 );
               
               // we expect to have encountered a field end-quote 
               if ( end == -1 )
                  throw new IllegalStateException();
               
               // extract the field value
               field = unquoteText( input.substring( pos, end ) );
               
               // set line pointer after next field separator or eol
               pos = end;
               while ( pos < slen && (c1 = input.charAt( pos++ )) != separator 
                       && c1 != '\n' );
            }
            
            // field is unquoted
            else
            {
               mark = pos;

               // search for separator or line end
               while ( pos < slen && (c1 = input.charAt( pos++ )) != separator 
                     && c1 != '\n' );

               // CASES OF VALUE/LINE TERMINATION
               // value + line termination by LF
               if ( c1 == '\n' ) {
                  eol = true;
                  end = pos-1;
                  
                  // decrement end position when encountered CRLF
                  while ( end > 0 && input.charAt( end -1 ) == '\r' )
                     end--;
               }
               // value termination by separator char
               else if ( c1 == separator ) {
                  end = pos - 1;
               }
               // line termination by string end
               else {
                  end = pos;
               }
               
               field = input.substring( mark, end );
            }
            
            // STORE ONE FIELD
            list.add( field );
            eol = eol | pos >= slen;
            
            // SPECIAL CASE empty last element
            if ( eol & c1 == separator )
               list.add( "" );
         }  // while
          
         // create result array
         endpos.value = pos;
         return list.toArray( new String[0] );
      }  // decodeLine       

      /**
       * Decodes a line of CSV encoded field values into an array of <code>String</code>s.
       * If <code>input<code> is an empty string, <code>start</code> parameter is ignored.
       * 
       * @param input CSV encoded text (not <b>null</b>)
       * @param start offset in <code>input</code>
       * @param separator character seen as field separator
       *  
       * @return array of <code>String</code> values, each consisting of a
       *         decoded field 
       * @throws IllegalArgumentException if <code>start</code> is out of range or
       *         if <code>separator</code> is illegal
       */
      public static String[] decodeLine ( String input, int start, char separator )
      {
         return decodeLine( input, start, separator, new BufferInt() );
      }
   }  // CSV
   
   public static class BufferInt
   {
      public int value;
      
   public BufferInt ()
   {}
   
   public BufferInt ( int value )
   {
      this.value = value;
   }
   
   }

   /**
    * Returns an array with device descriptors of the type <code>String[]</code>.
    * Each descriptor holds 2 entries with index 0=unix device name (e.g. "/dev/sdc1")
    * and 1=drive name in directory tree (e.g. "/media/USB DISK").
    * 
    * @return String[][] array of device descriptors, or <b>null</b> if 
    *         current operating system is not a Unix derivate
    * @throws IOException if required system information could not be obtained
    */
   public static String [][] getUnixDevices() throws IOException
   {
      Process prc;
      InputStream in;
      BufferedReader read;
      List<String[]> list = new ArrayList<String[]>();
      String line, s1, s2, rec[], arr[];
      int i1, i2;

      if ( Global.isUnixDerivate() )
      {
         Log.debug( 9, "(Util.getUnixDevices) -- calling Linux/Unix MOUNT" );
         prc = Runtime.getRuntime().exec( "mount" );
         in = new BufferedInputStream( prc.getInputStream() );
         read = new BufferedReader( new InputStreamReader( in ) );

         while ( (line = read.readLine()) != null )
         {
            if ( line.startsWith( "/dev/" ) )
            try {
               i1 = line.indexOf( "on" );
               i2 = line.indexOf( "type" );
               s1 = line.substring( 0, i1-1 );
               s2 = line.substring( i1+3, i2-1 );
               rec = new String[] { s1, s2 };
               list.add( rec );
            }
            catch ( Exception e )
            {}
         }
         read.close();
         Log.debug( 9, "(Util.getUnixDevices) -- devices: " + list.size() );
         if ( Log.getDebugLevel() >= 9 )
         for ( int i = 0; i < list.size(); i++ )
         {
            arr =  list.get( i );
            System.out.println( "++ device: " + arr[0] + ", " + arr[1] );
         }
         return list.toArray( new String[0][0] );
/*       
         Log.debug( 8, "(Util.getUnixDevices) -- Linux/Unix MOUNT started; output follows:" );
         in = prc.getInputStream();     
         out = System.out;
         Util.copyStream( in, out );
         in.close();
         out.flush();
         Log.debug( 8, "(Util.getUnixDevices) ## Linux/Unix MOUNT output END" );
*/         
      }
      return null;
   }
   
   /** Returns the file-system root directory (device name) for the given file.
    * 
    * @param file File file to investigate (may be <b>null</b>)
    * @return root directory of <tt>file</tt> or <b>null</b> if not found
    */
   public static File findRoot ( File file )
   {
      File f, roots[];
      String path, hstr, devices[][];
      int i;
      
      Log.debug( 8, "(Util.findRoot) investigating file: ".concat( 
            file == null ? "null" : file.getPath() )); 
      f = null;
      if ( file != null )
      {
         try
         {
            file = file.getCanonicalFile();
            path = file.getPath();
            if ( !Global.isWindows() )
            {
               if ( (devices = getUnixDevices()) != null )
                  for ( i = 0; i < devices.length; i++ )
                  {
                     hstr = devices[i][1];
                     if ( path.startsWith( hstr ) && !hstr.equals( "/" ) )
                     {
                        f = new File( hstr );
                        break;
                     }
                  }
            }
            else
            {
               roots = File.listRoots();
               if ( roots != null )
               {
                  for ( i = 0; i < roots.length; i++ )
                     if ( path.startsWith( roots[i].getAbsolutePath() ))
                     {
                        f = roots[i];
                        break;
                     }
               }
            }
         }
         catch ( IOException e )
         {
            e.printStackTrace();
         }
      }
      Log.debug( 8, "(Util.findRoot) -- detected file root: ".concat( 
            f == null ? "- none -" : f.getPath() ));
      return f;
   }

   /** Returns the truncated versions of the parameter strings
    * where the equal TRAILING part of both strings is cut off.
    * <p>Example: s1="a:/hanuman/zwei", s2="c:/programs/jpws/zwei"
    * returns {"a:/hanuman", "c:/programs/jpws"}; 
    * s1="a:/hanuman/zwei", s2="c:/programs/jpws/fuenf"
    * returns {"a:/hanuman/zwei", "c:/programs/jpws/fuenf"};
    * s1.equals(s2) returns two empty strings.
    * 
    * @param s1 String 1
    * @param s2 String 2
    * @return String[] of size 2 
    */
   public static String[] findTrunks ( String s1, String s2 )
   {
      String res[];
      int p1, p2;
      
      if ( s1 == null | s2 == null )
         throw new NullPointerException();

      res = new String[2];
      for ( p1 = s1.length(), p2 = s2.length(); p1 > 0 & p2 > 0; p1--, p2-- )
      {
         if ( s1.charAt( p1-1 ) != s2.charAt( p2-1 ) )
            break;
      }
      res[0] = s1.substring( 0, p1 );
      res[1] = s2.substring( 0, p2 );
      return res;
   }

   /** Returns a SHA-256 fingerprint value of the parameter string buffer.
    * 
    * @param buffer <code>String</code> data to digest
    * @return SHA256 digest
    */
   public static byte[] fingerPrint ( String buffer )
   {
      SHA256 sha;

      sha = new SHA256();
      sha.update( buffer );
      sha.finalize();
      return sha.digest();
   }

   /** Returns <b>true</b> if and only if the parameter string
    * complies with some formal requirements for an email address.
    * Note that leading and trailing blanks in the string are ignored.
    *  
    * @param hstr String text
    * @return boolean
    */
   public static boolean isEmailAddress ( String hstr )
   {
      int p1, p2, p3, len;
      
      if ( hstr != null && hstr.length() > 4 ) 
      {
         hstr = hstr.trim();
         len = hstr.length();
         p1 = hstr.indexOf( '@' );
         p2 = hstr.lastIndexOf( '.' );
         p3 = hstr.indexOf( ' ' );
         return len < 255 & p3 == -1 & p1 > 0 & p2 > p1 & p2 < len-1;
      }
      return false;
   }

   /** Whether a specified character is contained in a char array.
    * 
    * @param arr char[] character array
    * @param c char search character
    * @return boolean <b>true</b> if and only if c is an element of
    *         the character set defined by arr
    */
   public static boolean containsChar ( char[] arr, char c )
   {
      for ( int i = 0; i < arr.length; i++ )
         if ( c == arr[i] )
            return true;
      return false;
   }
   
   /** Whether the given char array contains a digit character.
    * 
    * @param arr char[] to be investigated
    * @return boolean <b>true</b> if and only if there is at least one
    *         character of the range '0'..'9' contained in the array 
    */
   public static boolean containsDigit ( char[] arr )
   {
      for ( int i = 0; i < arr.length; i++ )
         if ( Util.containsChar( PassphraseUtils.DIGIT_CHARS, arr[i] ) )
            return true;
      return false;
   }
   
   /** Returns a value of 0..4 indicating how many separate character
    * domains are referenced by the character set defined through the 
    * given character array.
    * 
    * @param ca char[] character set to investigate
    * @return int, result value 0..4
    */
   public static int textDomainVariance ( char[] ca )
   {
      int i, res;
      char c;
      boolean r1, r2, r3, r4;
      
      // analyse the char array
      r1 = r2 = r3 = r4 = false;
      for ( i = 0; i < ca.length; i++ )
      {
         c = ca[i];
         if ( c >= 'a' & c <= 'z' )
            r1 = true;
         if ( c >= 'A' & c <= 'Z' )
            r2 = true;
         if ( c >= '0' & c <= '9' )
            r3 = true;
         if ( Util.containsChar( PassphraseUtils.SYMBOL_CHARS, c ) )
            r4 = true;
      }
      
      // code the result
      res = 0;
      if ( r1 ) res++;
      if ( r2 ) res++;
      if ( r3 ) res++;
      if ( r4 ) res++;
      return res;
   }

   /** Investigates all separatable parts of the parameter text
    * and returns the first occurrence of a valid email address
    * or <b>null</b> if none found.
    * 
    * @param text Sting text to investigate
    * @return String email address or <b>null</b>
    */
   public static String extractMailaddress ( String text )
   {
      if ( text != null )
      {
         String exclude = "\r\n(),;:[]<>";
         text = reducedText( text, exclude, true );
/*         
         text = substituteText( text, "\r", " " );
         text = substituteText( text, "\n", " " );
         text = substituteText( text, "(", " " );
         text = substituteText( text, ")", " " );
*/         
         String[] arr = text.split( " " );
         for ( String address : arr )
         {
            if ( isEmailAddress( address ) )
               return address;
         }
      }
      return null;
   }

   /** 
    * Returns the parameter <code>text</code> string reduced by all occurrences
    * of characters which are elements in the <code>exclude</code> string (char set).
    * Optionally such occurrences can get replaced by blank characters and results in
    * a returned string of the length of the input string.
    * 
    * @param text String input text
    * @param exclude String set of characters to be excluded
    * @param blanks boolean if <b>true</b> occurrences will be replaced by blanks
    * @return String operated text
    */
   public static String reducedText ( String text, String exclude, boolean blanks )
   {
      StringBuffer sb;
      int i, c, blk, len;

      if ( text != null && exclude != null && !exclude.isEmpty() && !text.isEmpty() )
      {
         blk = " ".codePointAt( 0 );
         len = text.length();
         sb = new StringBuffer( len );
         for ( i = 0; i < len; i++ )
         {
            c = text.codePointAt( i );
            if ( exclude.indexOf( c ) > -1 )
            {
               if ( blanks )
                  sb.appendCodePoint( blk );
            }
            else
               sb.appendCodePoint( c );
         }
         text = sb.toString();
      }
      return text;
   }
   
   /**
    * Extracts all occurrences of valid Email Address within the given
    * text string as an array of java.lang.String objects. Multiple identical
    * addresses are returned as one address.
    * 
    * @param text String text to be investigated; may be <b>null</b> 
    * @return Array of java.lang.String or <b>null</b> if no valid address was found
    * @since 0-6-0
    */
   public static String[] extractMailAddresses ( String text )
   {
      ArrayList<String> list = new ArrayList<String>();
      String addr;
      int i;
      
      while ( (addr=extractMailaddress( text )) != null )
      {
         // add the first occurrence of ADDRESS to array
         list.add( addr );

         // proceed to next 
         i = text.length();
         text = Util.substituteText( text, addr, "" );
         if ( i == text.length() )
            break;
      }
      return list.isEmpty() ? null : list.toArray(new String[list.size()]);
   }

   /** Makes the current thread sleep for the given time. 
    * (Convenience method to avoid the try-catch statement, which is
    * executed with "e.printStackTrace()" when <code>InterruptedException
    * </code> is thrown.)
    * 
    * @param millis long milliseconds sleep time
    */
   public static void sleep ( long millis )
   {
      try
      { Thread.sleep( millis  ); }
      catch ( InterruptedException e )
      { e.printStackTrace(); }
   }

/**
 * Returns a string of a given length which consists only
 * of characters of type c.
 *    
 * @param c char stamp character
 * @param length length of result string
 * @return String
 */
public static String iterString( char c, int length ) {
    char[] a = new char[length];
    for (int i = 0; i < length; i++)
        a[i] = c;
    return new String(a);
}
   
}
