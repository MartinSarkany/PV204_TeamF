/*
 *  UndoManager in org.jpws.front
 *  file: UndoManager.java
 * 
 *  Project Jpws-0-4-0
 *  @author Wolfgang Keller
 *  Created 18.12.2006
 *  Version
 * 
 *  Copyright (c) 2006 by Wolfgang Keller, Munich, Germany
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

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.jpws.data.Options;
import org.jpws.data.PwsFileSocket;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.pwslib.data.HeaderFieldList;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.data.PwsRecordList;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.order.DefaultRecordWrapper;

/**
 * UndoManager active for file modifications of the list display level.
 *    
 *  @since 0-5-0
 */
public class UndoManager extends javax.swing.undo.UndoManager
{


public UndoManager ()
{
   int i, lim;
   
   i = Options.getIntOption( "maxUndoEntries" );
   lim = i > 0 ? i : Global.DEFAULT_MAXUNDO;
   this.setLimit( lim );
}

/** Removes all entries in this Undo manager and updates menues. */
public void clear ()
{
   this.discardAllEdits();
   MenuHandler.undoUpdated();
}

@Override
public void undoableEditHappened ( UndoableEditEvent e )
{
   if ( Options.isOptionSet( "useUndoRedo" ) ) {
      super.undoableEditHappened( e );
      MenuHandler.undoUpdated();
   }
}


public static class DeleteRecordEdit extends AbstractUndoableEdit
{

   private PwsFileSocket file;
   private DefaultRecordWrapper[] delRecs;
   private String group;
   private long updateTime;
   
   /**
    * Creates an undoable edit for the deletion of records in a 
    * <code>PwsFileSocket</code>.
    *  
    * @param file PwsFileSocket in which deletion occurs
    * @param records array of DefaultRecordWrapper, not empty
    * @param groupName if not <b>null</b> indicates a group delete edit with a group name 
    */
   public DeleteRecordEdit ( PwsFileSocket file, DefaultRecordWrapper[] records, String groupName )
   {
      // parameter control
      if ( file == null | records == null )
         throw new NullPointerException();
      if ( records.length == 0 )
         throw new IllegalArgumentException( "empty record list" );
      
      // init
      this.file = file;
      this.delRecs = records;
      this.group = groupName;
      updateTime = System.currentTimeMillis();
   }

   public long getUpdateTime ()
   {
      return updateTime;
   }

   @Override
   public void die ()
   {
      super.die();

      Log.debug( 5, "-- die UndoableEdit " + toString() );
      delRecs = null;
      file = null;
   }

   @Override
   public String toString ()
   {
      return getPresentationName() + " *** " + super.toString();
   }
   
   @Override
   public String getPresentationName ()
   {
      String result;

      // case delete group edit
      if ( group != null )
         result = ResourceLoader.getDisplay( "undo.delete.group" ) + " \"" + group + "\" (" + delRecs.length + ")";
      
      // case delete single record edit
      else if ( delRecs.length == 1 )
         result = ResourceLoader.getDisplay( "undo.delete.record" ) + " \"" + delRecs[0].toString() + "\"";
      
      // case delete multiple record edit
      else
         result = ResourceLoader.getDisplay( "undo.delete.records" ) + " (" + delRecs.length + ")" ; 

      return result;
   }

   @Override
   public void redo () throws CannotRedoException
   {
      ContainerView view;

      super.redo();
      
      try {
         // action of this instance
         file.deleteEntries( delRecs );
   
         if ( file instanceof PwsFileContainer )
         {
            Global.delay( 50 );
            view = ((PwsFileContainer)file).getViewHandler();
            view.setSelectedItems( null );
         }
         MenuHandler.undoUpdated();
      }
      catch ( Exception e )
      {
         e.printStackTrace();
         throw new CannotUndoException();
      }
   }

   @Override
   public void undo () throws CannotUndoException
   {
      ContainerView view;
      
      super.undo();

      // action of this instance
      try {
         file.addRecordList( new PwsRecordList( delRecs ) );

         if ( file instanceof PwsFileContainer )
         {
            Global.delay( 50 );
            view = ((PwsFileContainer)file).getViewHandler();
            view.setSelectedWrappers( delRecs );
         }
         MenuHandler.undoUpdated();
      }
      catch ( Exception e )
      {
         e.printStackTrace();
         throw new CannotUndoException();
      }
   }
}  // class DeleteRecordEdit


public static class ModifyRecordEdit extends AbstractUndoableEdit
{
   // subtype marker of this class
   public static final int MODIFY_RECORD_EDIT = 0;
   public static final int NEW_RECORD_EDIT = 1;
   public static final int MOVE_RECORD_EDIT = 2;
   public static final int MOVE_GROUP_EDIT = 5;
   public static final int COPY_RECORD_EDIT = 8;
   public static final int COPY_GROUP_EDIT = 9;
   public static final int DUPLICATE_RECORD_EDIT = 3;
   public static final int DUPLICATE_GROUP_EDIT = 6;
   public static final int RENAME_GROUP_EDIT = 4;
   public static final int IMPORT_RECORDS_EDIT = 7;
   public static final int TRANSFER_RECEIVE_EDIT = 10;

   private int type;
   private PwsFileSocket file;
   private DefaultRecordWrapper[] oldRecs;
   private DefaultRecordWrapper[] newRecs;
   private DefaultRecordWrapper[] copyRecs;
   private String oldgroup;
   private String newgroup;
   private boolean single;

   
   /**
    * Creates an undoable edit for various record modification types
    * in a <code>PwsFileSocket</code>.
    *  
    * @param type modification type (constants of this class) 
    * @param file file socket in which modification occurs
    * @param oldRecs array of records involved in modification (state before edit) 
    * @param newRecs array of records involved in modification (state after edit) 
    * @param oldgroup for type RENAME_GROUP indicates group name before edit 
    * @param newgroup for type RENAME_GROUP indicates group name after edit 
    */
   public ModifyRecordEdit ( 
                             int type,
                             PwsFileSocket file, 
                             DefaultRecordWrapper[] oldRecs,
                             DefaultRecordWrapper[] newRecs,
                             String oldgroup,
                             String newgroup
                             )
   {
      init( type, file, oldRecs, newRecs, null, oldgroup, newgroup );
   }

   /**
    * Creates an undoable edit for IMPORT_RECORDS type
    * in a <code>PwsFileSocket</code>.
    *  
    * @param type modification type (constants of this class) 
    * @param file file socket in which modification occurs
    * @param oldRecs array of records involved in modification (state before edit) 
    * @param newRecs array of records involved in modification (state after edit) 
    * @param copyRecs array of replaced records copied to different group 
    * @param source text identifying the import source 
    */
   public ModifyRecordEdit ( 
                             int type,
                             PwsFileSocket file, 
                             DefaultRecordWrapper[] oldRecs,
                             DefaultRecordWrapper[] newRecs,
                             DefaultRecordWrapper[] copyRecs,
                             String source
                             )
   {
      init( type, file, oldRecs, newRecs, copyRecs, null, source );
   }

   /**
    * Creates an undoable edit for various record modification types
    * in a <code>PwsFileSocket</code>. (Not for type RENAME_GROUP!)
    *  
    * @param type modification type (constants of this class) 
    * @param file file socket in which modification occurs
    * @param oldRecs records involved in modification (state before edit) 
    * @param newRecs records involved in modification (state after edit) 
    */
   public ModifyRecordEdit ( int type,
                             PwsFileSocket file, 
                             DefaultRecordWrapper[] oldRecs,
                             DefaultRecordWrapper[] newRecs
                             )
   {
      init( type, file, oldRecs, newRecs, null, null, null );
   }
   
   /**
    * Creates an undoable edit for various record modification types
    * in a <code>PwsFileSocket</code> for a single record involved.
    * (Not for type RENAME_GROUP!)
    *  
    * @param type modification type (constants of this class) 
    * @param file file socket in which modification occurs
    * @param oldRec records involved in modification (state before edit) 
    * @param newRec records involved in modification (state after edit) 
    */
   public ModifyRecordEdit ( int type,
                             PwsFileSocket file, 
                             PwsRecord oldRec,
                             PwsRecord newRec
                             )
   {
      DefaultRecordWrapper[] oldR, newR;
      
      newR = new DefaultRecordWrapper[1];
      newR[0] = new DefaultRecordWrapper( newRec, null );
      if ( oldRec != null )
      {
         oldR = new DefaultRecordWrapper[1]; 
         oldR[0] = new DefaultRecordWrapper( oldRec, null );
      }
      else
         oldR = null;
      
      init( type, file, oldR, newR, null, null, null );
   }
   
   private void init ( 
                  int type,
                  PwsFileSocket file, 
                  DefaultRecordWrapper[] oldRecs,
                  DefaultRecordWrapper[] newRecs,
                  DefaultRecordWrapper[] copyRecs,
                  String oldgroup,
                  String newgroup
                  )
   {
      // parameter control
      if ( type < 0 | type > 10 )
         throw new IllegalArgumentException( "undefined edit type" );
      if ( file == null )
         throw new NullPointerException( "file socket void" );
      if ( newRecs == null )
         throw new NullPointerException( "new state records void" );
      if ( newRecs.length == 0 )
         throw new IllegalArgumentException( "empty new state record list" );

      if ( (type == MODIFY_RECORD_EDIT | type == RENAME_GROUP_EDIT |
           type == MOVE_RECORD_EDIT |type == MOVE_GROUP_EDIT)  
           & oldRecs == null )
          throw new NullPointerException( "old state records void" );

      if ( type == RENAME_GROUP_EDIT & (oldgroup == null | newgroup == null) )
         throw new NullPointerException( "group name void" );

      // init
      this.type = type;
      this.file = file;
      this.oldgroup = oldgroup;
      this.newgroup = newgroup;
      this.single = newRecs.length == 1; 

      // make clone copies of actual record states
      this.newRecs = DefaultRecordWrapper.cloneArray( newRecs );
      if ( oldRecs != null )
         this.oldRecs = DefaultRecordWrapper.cloneArray( oldRecs );
      if ( copyRecs != null )
         this.copyRecs = DefaultRecordWrapper.cloneArray( copyRecs );
   }
   
   @Override
   public void die ()
   {
      super.die();

      Log.debug( 5, "-- die UndoableEdit " + toString() );
      newRecs = null;
      oldRecs = null;
      file = null;
   }

   @Override
   public String toString ()
   {
      return getPresentationName() + " *** " + super.toString();
   }
   
   private String recordTitle ()
   {
      return newRecs.length == 1 ?  " \"" + newRecs[0].toString() + " \"" : "";
   }

   private String groupTitle ()
   {
      String hstr;
      int recCount;

      hstr = "?"; 
      recCount = 0;

      if ( oldgroup != null )
         hstr = oldgroup;
      else if ( newgroup != null )
         hstr = newgroup;

      if ( oldRecs != null )
         recCount = oldRecs.length;
      else if ( newRecs != null )
         recCount = newRecs.length; 
      
      return " \"" + hstr + "\" " +
            ( recCount != 0 ? "(" + recCount + ")" : "" );
   }
   
   @Override
   public String getPresentationName ()
   {
      String result, hstr;

      switch ( type )
      {
      case MODIFY_RECORD_EDIT:
         result = ResourceLoader.getDisplay( "undo.modify.record" ) + recordTitle();
         break;
         
      case NEW_RECORD_EDIT:
         result = ResourceLoader.getDisplay( "undo.new.record" ) + recordTitle();
         break;
         
      case MOVE_RECORD_EDIT:
         if ( single )
            result = ResourceLoader.getDisplay( "undo.move.record" ) + recordTitle();
         else
         {
            hstr = ResourceLoader.getDisplay( "undo.move.records" );
            hstr = Util.substituteText( hstr, "$amount", String.valueOf( newRecs.length ) );
            result = Util.substituteText( hstr, "$target", newgroup );
         }
         break;
         
      case MOVE_GROUP_EDIT:
         result = ResourceLoader.getDisplay( "undo.move.group" ) + groupTitle();
         break;
         
      case COPY_RECORD_EDIT:
         if ( single )
            result = ResourceLoader.getDisplay( "undo.copy.record" ) + recordTitle();
         else
         {
            hstr = ResourceLoader.getDisplay( "undo.copy.records" );
            hstr = Util.substituteText( hstr, "$amount", String.valueOf( newRecs.length ) );
            result = Util.substituteText( hstr, "$target", newgroup );
         }
         break;
         
      case COPY_GROUP_EDIT:
         result = ResourceLoader.getDisplay( "undo.copy.group" ) + groupTitle();
         break;
         
      case DUPLICATE_RECORD_EDIT:
         result = single ? ResourceLoader.getDisplay( "undo.duplicate.record" ) + recordTitle()
                  : ResourceLoader.getDisplay( "undo.duplicate.records" ) + " (" + newRecs.length + ")";
         break;
         
      case DUPLICATE_GROUP_EDIT:
         result = ResourceLoader.getDisplay( "undo.duplicate.group" ) + groupTitle();
         break;
         
      case RENAME_GROUP_EDIT:
         result = ResourceLoader.getDisplay( "undo.rename.group" ) + groupTitle();
         break;
         
      case IMPORT_RECORDS_EDIT:
      case TRANSFER_RECEIVE_EDIT:
         hstr = type == IMPORT_RECORDS_EDIT ? "undo.import.records" : "undo.transfer.receive"; 
         hstr = ResourceLoader.getDisplay( hstr );
         hstr = Util.substituteText( hstr, "$amount", String.valueOf( newRecs.length ) );
         result = Util.substituteText( hstr, "$source", newgroup );
         break;
         
      default:
         result = "??";
      }

      return result;
   }

   private static final int SLEEPTIME = 100;
   
   @Override
   public void redo () throws CannotRedoException
   {
      ContainerView view=null;
      boolean isContainer;
      
      super.redo();
      
      // action of this instance
      if ( isContainer = file instanceof PwsFileContainer )
         view = ((PwsFileContainer)file).getViewHandler();

      synchronized ( file.getPwsFile() ) 
      {
         try {
            switch ( type )
            {
            case MODIFY_RECORD_EDIT:
               file.updateRecordRelaxed( newRecs[0].getRecord() );
               break;
               
            case NEW_RECORD_EDIT:
               file.addRecord( newRecs[0].getRecord() );
               break;
               
            case DUPLICATE_RECORD_EDIT:
            case DUPLICATE_GROUP_EDIT:
            case COPY_GROUP_EDIT:
            case COPY_RECORD_EDIT:
               file.addRecordList( newRecs );
               if ( isContainer )
               {
                  Global.delay( SLEEPTIME );
                  view.setSelectedWrappers( newRecs );
               }
               break;
               
            case IMPORT_RECORDS_EDIT:
            case TRANSFER_RECEIVE_EDIT:
               file.deleteEntries( oldRecs );
               file.addRecordList( newRecs );
               file.addRecordList( copyRecs );
               if ( isContainer )
               {
                  Global.delay( SLEEPTIME );
                  view.setSelectedWrappers( newRecs );
               }
               break;
               
            case MOVE_RECORD_EDIT:
            case MOVE_GROUP_EDIT:
               file.updateRecordList( newRecs );
               if ( isContainer )
               {
                  Global.delay( SLEEPTIME );
                  view.setSelectedWrappers( newRecs );
               }
               break;
   
            case RENAME_GROUP_EDIT:
               file.updateRecordList( newRecs );
               view.setSelectedWrappers( newRecs );
               break;
               
            default:
            }
   
            MenuHandler.undoUpdated();
         }
         catch ( Exception e )
         {
            e.printStackTrace();
            throw new CannotUndoException();
         }
      }
   }

   @Override
   public void undo () throws CannotUndoException
   {
      ContainerView view=null;
      boolean isContainer;
      
      super.undo();

      if ( isContainer = file instanceof PwsFileContainer )
         view = ((PwsFileContainer)file).getViewHandler();

      synchronized ( file.getPwsFile() ) 
      {
         // action of this instance
         try {
            switch ( type )
            {
            case MODIFY_RECORD_EDIT:
               file.updateRecordRelaxed( oldRecs[0].getRecord() );
               break;
               
            case NEW_RECORD_EDIT:
            case DUPLICATE_GROUP_EDIT:
               file.deleteEntries( newRecs );
               if ( isContainer )
                  view.setSelectedIndex( -1 );
               break;
               
            case DUPLICATE_RECORD_EDIT:
            case COPY_GROUP_EDIT:
            case COPY_RECORD_EDIT:
               file.deleteEntries( newRecs );
               if ( isContainer & oldRecs != null )
               {
                  Global.delay( SLEEPTIME );
                  view.setSelectedWrappers( oldRecs );
               }
               break;
               
            case IMPORT_RECORDS_EDIT:
            case TRANSFER_RECEIVE_EDIT:
               file.deleteEntries( newRecs );
               file.deleteEntries( copyRecs );
               if ( isContainer )
                  view.setSelectedIndex( -1 );
               if ( oldRecs != null )
               {
                  file.addRecordList( oldRecs );
                  if ( isContainer )
                  {
                      Global.delay( SLEEPTIME );
                     view.setSelectedWrappers( oldRecs );
                  }
               }
               break;
               
            case MOVE_RECORD_EDIT:
            case MOVE_GROUP_EDIT:
               file.updateRecordList( oldRecs );
               if ( isContainer )
               {
                   Global.delay( SLEEPTIME );
                  view.setSelectedWrappers( oldRecs );
               }
               break;
   
            case RENAME_GROUP_EDIT:
               file.updateRecordList( oldRecs );
               view.setSelectedWrappers( oldRecs );
               break;
               
            default:
            }
   
            MenuHandler.undoUpdated();
         }
         catch ( Exception e )
         {
            e.printStackTrace();
            throw new CannotUndoException();
         }
      }
   }
   
}  // ModifyRecordEdit

public static class FileEdit extends AbstractUndoableEdit
{
   // subtype marker of this class
   public static final int REVERT_EDIT = 0;
   public static final int REVERT_MIRROR_EDIT = 2;
   public static final int RESTORE_EDIT = 1;

   private int type;
   private PwsFileSocket file;
   private PwsFile       oldFile;
   private PwsFile       externalFile;
   private long time;

   /**
    * Creates an undoable edit for various file oriented modification types.
    *  
    * @param type modification type (constant of this class) 
    * @param file file socket in which modification occurs
    * @param externalFile PwsFile involved in modification (source or target depending on type)
    */
   public FileEdit ( int type,
                     PwsFileSocket file, 
                     PwsFile externalFile,
                     long externalTime
                    )
   {
      init( type, file, externalFile, externalTime );
   }
   
   private void init ( int type,
                       PwsFileSocket file, 
                       PwsFile externalFile,
                       long time
                      )
   {
      // parameter control
      if ( type < 0 | type > 2 )
         throw new IllegalArgumentException( "undefined edit type" );
      if ( file == null )
         throw new NullPointerException( "file socket void" );
      if ( externalFile == null )
         throw new NullPointerException( "external file parameter void" );

      // init
      this.type = type;
      this.file = file;
      this.time = time;
      this.externalFile = (PwsFile)externalFile.clone();
      oldFile = (PwsFile) file.getPwsFile().clone();
   }  // init

   @Override
   public String getPresentationName ()
   {
      String result, timestr, hstr;
   
      switch ( type )
      {
      case REVERT_EDIT:
      case REVERT_MIRROR_EDIT:
//         try { time = adapter.getModifiedTime( externalPath ); }
//         catch ( Exception e )
//         { time = 0; }
         timestr = time == 0 ? "" : Global.getLocalDateTime( time );
         hstr = type == REVERT_EDIT ? "undo.revert.file" : "undo.revert.mirror";
         result = ResourceLoader.getDisplay( hstr ) + " \"" + timestr + "\"";
         break;
         
      case RESTORE_EDIT:
         result = ResourceLoader.getDisplay( "undo.restore.file" ) + " \"" 
                  + externalFile.getFileName() + "\"";
         break;
         
      default:
         result = "??";
      
      }
   
      return result;
   }

   @Override
   public void die ()
   {
      super.die();
   
      Log.debug( 5, "-- die UndoableEdit: " + toString() );
//      newRecs = null;
   }

   @Override
   public String toString ()
   {
      return getPresentationName() + " *** " + super.toString();
   }
   
   @Override
   public void redo () throws CannotRedoException
   {
      super.redo();
      
      // action of this instance
      try {
         // open backup file
         file.substituteContent( externalFile );
         String hstr = type == REVERT_EDIT ? "msg.confirm.revert" : 
                type == REVERT_MIRROR_EDIT ? "msg.confirm.revert.mirror" :
                "<html>" + ResourceLoader.getDisplay( "msg.confirm.restore" ) + 
                "<br><font color=\"green\">" + externalFile.getFilePath();
         GUIService.infoMessage( null, hstr );
   
         MenuHandler.undoUpdated();
      }
      catch ( Exception e )
      {
         e.printStackTrace();
         throw new CannotUndoException();
      }
   }

   @Override
   public void undo () throws CannotUndoException
   {
      super.undo();
   
      // action of this instance
      try {
         file.substituteContent( oldFile );
         String hstr = type == REVERT_EDIT ? "msg.undo.confirm.revertundo" : 
                "msg.undo.confirm.restoreundo";
         GUIService.infoMessage( null, hstr );
   
         MenuHandler.undoUpdated();
      }
      catch ( Exception e )
      {
         e.printStackTrace();
         GUIService.failureMessage( "UNDO failure!", e );
         throw new CannotUndoException();
      }
   }
} // FileEdit


public static class ModifyPasswordEdit extends AbstractUndoableEdit
{

   private PwsFileSocket file;
   private PwsPassphrase oldPass, newPass;
   private long updateTime;
   
   /**
    * Creates an undoable edit for the modification of access passphrase
    * in a <code>PwsFileSocket</code>.
    *  
    * @param file PwsFileSocket in which deletion occurs
    * @param oldPass the passphrase before edit
    * @param newPass the passphrase after edit
    */
   public ModifyPasswordEdit ( PwsFileSocket file, PwsPassphrase oldPass, PwsPassphrase newPass )
   {
      // parameter control
      if ( file == null | newPass == null )
         throw new NullPointerException();
      
      // init
      this.file = file;
      this.oldPass = oldPass;
      this.newPass = newPass;
      updateTime = System.currentTimeMillis();
   }

   public long getUpdateTime ()
   {
      return updateTime;
   }

   @Override
   public void die ()
   {
      super.die();

      Log.debug( 5, "-- die UndoableEdit " + toString() );
      oldPass = null;
      newPass = null;
      file = null;
   }

   @Override
   public String toString ()
   {
      return getPresentationName() + " *** " + super.toString();
   }
   
   @Override
   public String getPresentationName ()
   {
      return ResourceLoader.getDisplay( "undo.modify.passphrase" );
   }

   
   @Override
   public void redo () throws CannotRedoException
   {
      super.redo();
      
      // action of this instance
      file.setPassphrase( newPass );
      MenuHandler.undoUpdated();
   }

   @Override
   public void undo () throws CannotUndoException
   {
      super.undo();

      // action of this instance
      file.setPassphrase( oldPass );
      MenuHandler.undoUpdated();
   }
}


public static class HeaderEdit extends AbstractUndoableEdit
{
   private PwsFileSocket    file;
   private HeaderFieldList  oldList, newList;
   private int              oldLoops, newLoops;

   /**
    * Creates an undoable edit for modifications of the header field list.
    * (This must be generated *after* modifications occured.)
    *  
    * @param file file socket in which modification occurs
    * @param oldList <code>HeaderFieldList</code> previous to modifications
    */
   public HeaderEdit ( PwsFileSocket file, 
                       HeaderFieldList oldList 
                      )
   {
      if ( file == null | oldList == null )
         throw new NullPointerException();
      
      this.file = file;
      this.oldList = (HeaderFieldList)oldList.clone();
      this.newList = file.getHeaderFields();
   }

   /**
    * Creates an undoable edit for modifications of the header field list
    * or of the security loops value.
    * (This must be generated *after* modifications occured.)
    *  
    * @param file file socket in which modification occurs
    * @param oldList <code>HeaderFieldList</code> previous to modifications;
    *        may be <b>null</b> if no modification occured
    * @param oldLoops old security loops value or 0 if no modification occured        
    */
   public HeaderEdit ( PwsFileSocket file, 
                       HeaderFieldList oldList,
                       int oldLoops
                      )
   {
      if ( file == null )
         throw new NullPointerException();
      
      this.file = file;
      if ( oldList != null )
      {
         this.oldList = (HeaderFieldList)oldList.clone();
         this.newList = file.getHeaderFields();
      }
      if ( oldLoops > 0 )
      {
         this.oldLoops = oldLoops;
         this.newLoops = file.getSecurityLoops();
      }
   }

   @Override
   public String getPresentationName ()
   {
      return ResourceLoader.getDisplay( "undo.headeredit" );
   }

   @Override
   public void die ()
   {
      super.die();
   
      Log.debug( 5, "-- die UndoableEdit: " + toString() );
      newList = null;
      oldList = null;
   }

   @Override
   public String toString ()
   {
      return getPresentationName() + " *** " + super.toString();
   }
   
   @Override
   public void redo () throws CannotRedoException
   {
      super.redo();
      
      // action of this instance
      try {
         if ( newList != null )
            file.setHeaderFields( newList );
         if ( newLoops > 0 )
            file.setSecurityLoops( newLoops );
         
         MenuHandler.undoUpdated();
      }
      catch ( Exception e )
      {
         e.printStackTrace();
         throw new CannotUndoException();
      }
   }

   @Override
   public void undo () throws CannotUndoException
   {
      super.undo();
      
      // action of this instance
      try {
         if ( oldList != null )
            file.setHeaderFields( oldList );
         if ( oldLoops > 0 )
            file.setSecurityLoops( oldLoops );

         MenuHandler.undoUpdated();
      }
      catch ( Exception e )
      {
         e.printStackTrace();
         GUIService.failureMessage( "UNDO failure!", e );
         throw new CannotUndoException();
      }
   }
} // FileEdit

}  
