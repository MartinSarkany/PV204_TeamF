package org.jpws.front;

import java.awt.Font;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.undo.UndoableEdit;

import org.jpws.data.IOManager;
import org.jpws.front.DatabaseHandler.FileAccessModus;
import org.jpws.front.util.ButtonBarDialog;
import org.jpws.front.util.ButtonBarListener;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsRecordList;

@SuppressWarnings("serial") 
class ResolveMirrorDialog extends ButtonBarDialog 
                          implements ButtonBarListener 
{
   /**
     * 
     */
//    private final MirrorFileManager mirrorFileManager;
   private static final int MERGE_ACTION = 1;
   private static final int REVERT_ACTION = 2;

   private PwsFileContainer ct;
   private PwsFile       mirDb;
   private JRadioButton  deleteButton, actionButton, openButton;
   private int           action;
   private boolean       mirrorDeleted;

   /**
    * Constructs a mirror resolving dialog.
    * 
    * @param mirror <tt>File</tt> the mirror file
    * @param mirrorFileManager TODO
    * @throws IllegalArgumentException if the mirror could not be opened (wrong file type, password, etc.)
    * @throws IOException if an IO error occurred 
    */
   public ResolveMirrorDialog ( PwsFileContainer cont, File mirror ) 
		   throws IllegalArgumentException, IOException {
	  super();
      this.ct = cont;
//      this.mirrorFileManager = mirrorFileManager;
      setModal( true );
      init( mirror );
   }

   private void init ( File mirror ) throws IOException {
      PwsFile origDb;
      ContextFile cf;
      ButtonGroup grp;
      JPanel pane, panel;
      JLabel msgLabel;
      String text;
      
      // open parameter database (mirror file)
      // load mirror file (via direct access)
      origDb = ct.getPwsFile();
      cf = IOManager.makeLocalContextFile( mirror );
      mirDb = DatabaseHandler.passTryOpen( cf, ct.getPassphrase(), FileAccessModus.desktopSelection ); 
      if ( mirDb == null )
         throw new IllegalArgumentException( "no database verified" );

      action = mirDb.lastModified() > origDb.lastModified() ?
               REVERT_ACTION : MERGE_ACTION;
      
      setSynchronous(true);
      addButtonBarListener( this );
      
      // framework
      setTitle( ResourceLoader.getDisplay( "dlg.mirror.resolve" ) );
      pane = new JPanel( new VerticalFlowLayout( 20 ) );
      pane.setBorder( BorderFactory.createEmptyBorder( 20, 30, 0, 30 ) );
      
      // message text
      text = ResourceLoader.getDisplay( "msg.mirror.resolve" );
         
      text = Util.substituteText( text, "$dbname", ct.getDatabaseName() );
      text = Util.substituteText( text, "$time-m", Global.getLocalDateTime( mirDb.lastModified() ) );
      text = Util.substituteText( text, "$time-o", Global.getLocalDateTime( origDb.lastModified() ) );
      text = Util.substituteText( text, "$t-color", action == REVERT_ACTION ? "blue" : "red" );
      msgLabel = new JLabel( text );
      msgLabel.setFont( msgLabel.getFont().deriveFont( Font.PLAIN ) );
      pane.add( msgLabel );
      
      // create radio buttons
      panel = new JPanel( new VerticalFlowLayout( 10 ) );
      pane.add( panel );
      text = action == REVERT_ACTION ? ResourceLoader.getDisplay( "radio.resolvemirror.revert" ) :
             ResourceLoader.getDisplay( "radio.resolvemirror.merge" );
      actionButton = new JRadioButton( text, true );
      deleteButton = new JRadioButton( ResourceLoader.getDisplay( "radio.resolvemirror.delete" ) );
      openButton = new JRadioButton( ResourceLoader.getDisplay( "radio.resolvemirror.open" ) );
      grp = new ButtonGroup();
      grp.add( actionButton );
      grp.add( deleteButton );
      grp.add( openButton );
      panel.add( actionButton );
      panel.add( deleteButton );
      panel.add( openButton );

      setDialogPanel( pane );
   }

   // ******** IMPLEMENTS ButtonBarListener ***************

   public boolean extraButtonPerformed ( Object button ) { 
	   return false; 
   }
   
   public void noButtonPerformed () {
   }

   public void helpButtonPerformed () {
   }

   public void cancelButtonPerformed () {
      dispose();
   }

   public boolean okButtonPerformed () {
      PwsFileContainer container;
      ContextFile cf;
      UndoableEdit edit;
      String hstr;
      
      cf = mirDb.getContextFile();
      boolean ok = true;
      
      // discriminate action
      if ( actionButton.isSelected() ) {
         if ( action == MERGE_ACTION ) {
             ct.mergeDatabase( this, mirDb, mirDb.getFileName(), null, 
                               PwsRecordList.MERGE_MODIFIED, false ); 

            // delete mirror file
            deleteMirror( cf );
         }
         
         else if ( action == REVERT_ACTION )
         try {
            // create undoable edit event (must do before change happens on "this")
            edit = new UndoManager.FileEdit( UndoManager.FileEdit.REVERT_MIRROR_EDIT,
                  ct, mirDb, mirDb.lastModified() );

            // change editor file
            ct.substituteContent( mirDb ); 
            ct.setLastSaveTime( System.currentTimeMillis() );

            // delete mirror file
            deleteMirror( cf );

            // fire edit event
            ct.fireEditEvent( edit );

            // user notify
            GUIService.statusConfirm( "msg.confirm.revert.mirror" );
            ct.confirmOperation( this, PwsFileContainer.OP_REVERT_MIRROR, mirDb.getFileName() );

         } catch ( Exception e ) {
            e.printStackTrace();
            ok = false;
         }
      }
      
      else if ( deleteButton.isSelected() ) {
         // if DELETE FILE - option
         if ( ok = deleteMirror( cf ) ) {
            hstr = ResourceLoader.getDisplay( "msg.confirm.deletefile" );
            hstr = "<html>" + hstr + "<br><font color=\"green\">" +
                   cf.getFilepath() + "</font>";
            GUIService.statusConfirm("msg.confirm.deletefile", cf.getFileName() );
            GUIService.infoMessage( this, null, hstr );
         }
      }
      
      else {
         // if OPEN - option, open mirror file into desktop
         if ( !Global.isOpenFile( cf ) ) {
        	 DatabaseHandler.putFileToShelf( mirDb );
         } else {
            container = DisplayManager.getFileContainer( cf ); 
            DisplayManager.requestActivationState( container,
                  DisplayManager.STATE_ACTIVE );
         }
      }
      
      if ( ok ) {
         if ( mirrorDeleted ) {
            ct.checkMirrorActivity();
         }
         dispose();
      }
      return ok;
   }

   /** Attempts to delete the specified file and reports a warning
    * or IO-failure message if deletion was not successful.
    * 
    * @param cf <code>ContextFile</code>
    * @return boolean <b>true</b> if and only if deletion took place
    * @throws IOException
    */
   private boolean deleteMirror ( ContextFile cf ) {
      boolean ok = false;
      
      String delFailText = ResourceLoader.getDisplay( "msg.error.deletefile" );
      delFailText = Util.substituteText( delFailText, "$path", cf.getFilepath() );
      try {
         if ( !(ok = cf.delete()) ) {
            GUIService.warningMessage( this, null, delFailText );
         }
      } catch ( IOException e ) { 
         e.printStackTrace(); 
         GUIService.failureMessage( this, delFailText, e );
      }
      mirrorDeleted |= ok;
      return ok;
   }

}  // class ResolveMirrorDialog