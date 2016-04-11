/*
 *  FileOpenDialog in org.jpws.front
 *  file: FileOpenDialog.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 06.09.2004
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

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.jpws.front.util.ResourceLoader;

/**
 *  This file open dialog is an extention of <code>JFileChooser</code> which 
 *  incorporates several possible file filters. 
 * 
 * @author Kevin Preece
 * @author Wolfgang Keller
 */
public class FileOpenDialog extends JFileChooser
{
   // OR-able features for filter settings
   public static final int PWSFILE_FILTER = 1;
   public static final int BACKUP_FILTER = 2;
   public static final int EXECUTABLE_FILTER = 4;
   public static final int CSV_FILTER = 8;

   private JDialog dialog;
   private int userChoice;
   
   
	static class PwsFileFilter extends FileFilter
	{
		/**
		 * 
		 * @param pathname
		 * @return
		 * 
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		public boolean accept( File pathname )
		{
			int		pos;
			String	ext;
			String	path;

			if ( pathname.isDirectory() )
			{
				return true;
			}

			path	= pathname.getName();
			pos		= path.lastIndexOf( '.' );

			if ( (pos >= 0) )
			{
				ext	= path.substring( pos );
                if ( Global.isWindows() )
                   ext = ext.toLowerCase();

				if ( ext.equals( Global.DEFAULT_FILEEXTENTION ) ||
                     ext.equals( Global.DEFAULT_PWSFILEEXTENTION ) )
				{
					return true;
				}
			}
			return false;
		}

		public String getDescription()
		{
	        return ResourceLoader.getDisplay("filechooser.filter.native");
	    }
	}

   static class BackupFileFilter extends FileFilter
   {
      /**
       * 
       * @param pathname
       * @return
       * 
       * @see java.io.FileFilter#accept(java.io.File)
       */
      public boolean accept( File pathname )
      {
         if ( pathname.isDirectory() )
            return true;
         return accept( pathname.getName() );
      }

      public String getDescription()
      {
           return ResourceLoader.getDisplay("filechooser.filter.backup");
       }

      /**
       * Returns true if the given filepath ends with a
       * backup or security file extention. Returns <b>false</b>
       * on <b>null</b>.
       * 
       * @param pathname String filename or path; may be <b>null</b>
       * @return boolean
       * 
       * @see java.io.FileFilter#accept(java.io.File)
       */
      public static boolean accept( String filepath )
      {
         int      pos;
         String   ext;
      
         if ( filepath == null || filepath.length() == 0 )
            return false;
         
         pos  = filepath.lastIndexOf( '.' );
         if ( (pos >= 0) )
         {
            ext   = filepath.substring( pos );
            if ( Global.isWindows() )
               ext = ext.toLowerCase();
      
            if ( ext.equals( Global.DEFAULT_BACKUPEXTENTION ) || 
                 ext.equals( ".sec" ) ||
                 ext.equals( ".temp" ) ||
                 ext.equals( ".old" ) )
            {
               return true;
            }
         }
         return false;
      }
   }

   static class ExecutableFileFilter extends FileFilter
   {
      /**
       * 
       * @param pathname
       * @return
       * 
       * @see java.io.FileFilter#accept(java.io.File)
       */
      public boolean accept( File pathname )
      {
         int      pos;
         String   ext;
         String   path;

         if ( !Global.isWindows() || pathname.isDirectory() )
         {
            return true;
         }

         path  = pathname.getName();
         pos   = path.lastIndexOf( '.' );

         if ( (pos >= 0) )
         {
            ext   = path.substring( pos );

            if ( ext.equalsIgnoreCase(".exe") || ext.equalsIgnoreCase(".com") )
            {
               return true;
            }
         }
         return false;
      }

      public String getDescription()
      {
           return ResourceLoader.getDisplay("filechooser.filter.executable");
       }
   }
   
    static class CSVFileFilter extends FileFilter
   {
      /**
       * 
       * @param pathname
       * @return
       * 
       * @see java.io.FileFilter#accept(java.io.File)
       */
      public boolean accept( File pathname )
      {
         int      pos;
         String   ext;
         String   path;
   
         if ( pathname.isDirectory() )
         {
            return true;
         }
   
         path  = pathname.getName();
         pos   = path.lastIndexOf( '.' );
   
         if ( (pos >= 0) )
         {
            ext   = path.substring( pos );
   
            if ( ext.equalsIgnoreCase(".csv") )
            {
               return true;
            }
         }
         return false;
      }
   
      public String getDescription()
      {
           return ResourceLoader.getDisplay("filechooser.filter.csv");
       }
   }

   /** Creates a FileOpenDialog with an optional set of file filtering options
    *  pointing to the user's default directory.  
     * 
	 * @param filtermod determines available file type filters in OR conjunction.
     *        0 = no filter; PWSFILE_FILTER, BACKUP_FILTER, EXECUTABLE_FILTER   
	 */
	public FileOpenDialog( int filtermod )
	{
		super();
		setOptions( filtermod );
	}

    /** Creates a FileOpenDialog with an optional set of file filtering options
     *  and a specified start directory.  
     * 
     * @param filtermod determines available file type filters in OR conjunction.
     *        0 = no filter; PWSFILE_FILTER, BACKUP_FILTER, EXECUTABLE_FILTER   
	 * @param currentDirectory File 
	 */
	public FileOpenDialog(  int filtermod, File currentDirectory )
	{
		super( currentDirectory );
		setOptions( filtermod );
	}

    /** Creates a FileOpenDialog with an optional set of file filtering options
     *  and a start directory.  
     * 
     * @param filtermod determines available file type filters in OR conjunction.
     *        0 = no filter; PWSFILE_FILTER, BACKUP_FILTER, EXECUTABLE_FILTER   
     * @param currentDirectoryPath String filepath
     */
	public FileOpenDialog(  int filtermod, String currentDirectoryPath )
	{
		super( currentDirectoryPath );
		setOptions( filtermod );
	}
/*
	public FileOpenDialog( FileSystemView fsv )
	{
		super( fsv );
		setOptions();
	}

	public FileOpenDialog( File currentDirectory, FileSystemView fsv )
	{
		super( currentDirectory, fsv );
		setOptions();
	}

	public FileOpenDialog( String currentDirectoryPath, FileSystemView fsv )
	{
		super( currentDirectoryPath, fsv );
		setOptions();
	}
*/
   protected JDialog createDialog(Component parent) throws HeadlessException {
      dialog = super.createDialog(parent);
      return dialog;	
   }
   
   public void dispose () {
      dialog.dispose();
   }

   @Override
   public int showOpenDialog(Component parent) throws HeadlessException {
	  userChoice = super.showOpenDialog(parent);
	  return userChoice;
   }

   @Override
   public int showSaveDialog(Component parent) throws HeadlessException {
	  userChoice =  super.showSaveDialog(parent);
	  return userChoice;
   }

   @Override
   public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
	  userChoice =  super.showDialog(parent, approveButtonText);
	  return userChoice;
   }

   /** Returns the user choice for terminating this dialog.
    * 
    * @return int CANCEL_OPTION, APPROVE_OPTION, ERROR_OPTION
    */
   public int getUserChoice () {
	  return userChoice;
   }
   
private void setOptions( int filtermod )
   {
      FileFilter filter, activeFilter;
      
	  setMultiSelectionEnabled( false );
      activeFilter = null;
      
      if ( (filtermod & PWSFILE_FILTER) == PWSFILE_FILTER )
      {
         addChoosableFileFilter( filter = new PwsFileFilter() );
         if ( activeFilter == null )
            activeFilter = filter;
      }

      if ( (filtermod & BACKUP_FILTER) == BACKUP_FILTER )
      {
         addChoosableFileFilter( filter = new BackupFileFilter() );
         if ( activeFilter == null )
            activeFilter = filter;
      }

      if ( (filtermod & CSV_FILTER) == CSV_FILTER )
      {
         addChoosableFileFilter( filter = new CSVFileFilter() );
         if ( activeFilter == null )
            activeFilter = filter;
      }

      if ( (filtermod & EXECUTABLE_FILTER) == EXECUTABLE_FILTER )
      {
         addChoosableFileFilter( filter = new ExecutableFileFilter() );
         if ( activeFilter == null )
            activeFilter = filter;
      }
      
      if ( activeFilter != null )
         setFileFilter( activeFilter );
   }  // setOptions
}
