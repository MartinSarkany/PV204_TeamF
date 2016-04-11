/*
 *  GUIService in org.jpws.front
 *  file: GUIService.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 22.09.2005
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
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jpws.data.IOManager;
import org.jpws.data.Options;
import org.jpws.data.UserBreakException;
import org.jpws.front.MessageDialog.MessageType;
import org.jpws.front.util.DialogButtonBar;
import org.jpws.front.util.EditorTextField;
import org.jpws.front.util.HtmlBrowserDialog;
import org.jpws.front.util.ResourceLoader;
import org.jpws.front.util.TooltipComboBox;
import org.jpws.front.util.UserOptionDialog;
import org.jpws.front.util.Util;
import org.jpws.front.util.VerticalFlowLayout;
import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsPassphrasePolicy;
import org.jpws.pwslib.exception.InvalidPassphrasePolicy;
import org.jpws.pwslib.global.PassphraseUtils;


/**
 *  Collection of minor GUI-oriented services like message boxes, etc.
 *  All methods of this class are EDT-safe, which means they guarantee
 *  GUI active routines to perform on the EDT.  
 */
public class GUIService
{
   public static final int ANYFILE_CHOOSER = 0;
   public static final int PWSFILE_CHOOSER = 1;
   public static final int BACKUPFILE_CHOOSER = 2;
   public static final int EXECFILE_CHOOSER = 3;

   public static final int YES_TO_ALL_OPTION = 16;
   
/**
 * Singleton class. 
 */
private GUIService () {
}

/** Displays an information message centred within the given parent component.
 * 
 * @param owner parent component; if <b>null</b> the current
 *        mainframe window is used
 * @param title title of dialog; text or token; may be <b>null</b> in which 
 *        case a standard title is used 
 * @param text message text; text or token
 */
public static void infoMessage ( Component owner, String title, String text )
{
   MessageDialog.showInfoMessage( owner, title, text, MessageType.info );
   ActionHandler.resetIdleTime();
}

/** Displays a warning message centered within the given parent component.
 * 
 * @param owner parent component; may be <b>null</b> 
 * @param title title of dialog; text or token; may be <b>null</b> in which 
 *        case a standard title is used 
 * @param text message text; text or token
 */
public static void warningMessage ( Component owner, String title, String text )
{
   warningMessage( owner, title, text, null );
}

/** Displays a warning message centered within the given parent component.
 * 
 * @param owner parent component; may be <b>null</b> 
 * @param title title of dialog; text or token; may be <b>null</b> in which 
 *        case a standard title is used 
 * @param text message text; text or token
 * @param e if not <b>null</b> this <tt>Exception</tt> is reported in the
 *        message text 
 */
public static void warningMessage ( Component owner, String title, String text,
                                    Exception e )
{
   String hstr1, hstr2;
   
   if ( title == null )
      title = "dlg.warning";
   
   hstr1 = ResourceLoader.codeOrRealDisplay( title );
   hstr2 = "<html><b>".concat( ResourceLoader.codeOrRealDisplay( text ) ); 
   hstr2 = getExceptionMessage( hstr2, e );

   MessageDialog.showInfoMessage( owner, hstr1, hstr2, MessageType.warning );
   ActionHandler.resetIdleTime();
}

/** Displays a message in the application's statusline.
 * 
 * @param token text token; may be <b>null</b>
 * @param text additional text that is appended to the text defined
 *        by <code>token</code>; may be <b>null</b>
 */
public static void statusConfirm ( String token, String text )
{
   StringBuffer buf = new StringBuffer();
   String hstr = null;
   
   if ( token != null ) {
      hstr = ResourceLoader.getDisplay( token );
      buf.append( hstr );
   }
   
   if ( hstr != null & text != null )
      buf.append( " " );
   if ( text != null )
      buf.append( text );

   Global.setStatusText( buf.toString() );
}

/** Displays a message in the system statusline.
 * 
 * @param token text token; may be <b>null</b>
 */
public static void statusConfirm ( String token )
{
   statusConfirm( token, null );
}

/** Starts a filechooser dialog to determine the external browser application
    *  path value in program options.
    *  
    *  @return the dialog termination option (<code>FileOpenDialog</code>)
    */   
   public static int editApplicationOption ( Component parent, String title, String option )
   {
      String browser, path;
      FileOpenDialog fc;
      File file, startDir;
      int choice;
      
      if (option == null || option.isEmpty())
    	  throw new IllegalArgumentException("option parameter void");

      path = Options.getOption( option );
//      path = Options.getOption( "browserApplication" );
      startDir = path.equals( "" ) ? null : new File( path ).getParentFile();
      if ( startDir == null ) {
         startDir = new File( Global.getOSRootPath() );
         if ( Global.isUnixDerivate() && (file = new File( "/usr/bin" )).exists() )
            startDir = file;
         if ( Global.isWindows() && (file = new File( startDir, "program files" )).exists() )
            startDir = file;
      }
      
      fc = new FileOpenDialog( FileOpenDialog.EXECUTABLE_FILTER, startDir );
      fc.setDialogTitle( ResourceLoader.codeOrRealDisplay(title) );
//      fc.setDialogTitle( ResourceLoader.getDisplay( "dlg.choosebrowser" ));
      choice = fc.showOpenDialog( parent == null ? Global.getActiveFrame() : parent );
      fc.dispose();
      ActionHandler.resetIdleTime();
      if ( choice == JFileChooser.APPROVE_OPTION &&
           (file = fc.getSelectedFile()) != null )
      {
         browser = file.isFile() ? file.getAbsolutePath() : null;
         Options.setOption( option, browser );
      }
      return choice;
   }

/** Lets the user input a new passphrase for a file. This includes
 *  verification through double input and testing for password quality. 
 *
 * @param ct <code>PwsFileContainer</code> if called from a file container,
 *        may be <b>null</b>
 * @param filename filename to be displayed in input request
 * @return a valid passphrase or <b>null</b> if the action was cancelled
 */   
public static PwsPassphrase enterNewPassphrase ( PwsFileContainer ct, String filename )
{
   PasswordDialog pwDlg;
   PwsPassphrase passwd;
   char[] cbuf;

   // enter password dialog
   try {
	   pwDlg = PasswordDialog.performed(ct, filename, PasswordDialog.DEFINE, false);
	   if ( pwDlg.isOkPressed() ) {
	      // create and return the defined password (PwsPassphrase)
	      cbuf = pwDlg.getEnteredPassword();
	      passwd = new PwsPassphrase( cbuf );
	      Util.destroyChars( cbuf );
	      return passwd;
	   }
   } catch (InterruptedException e) {
   }
   
//   pwDlg = new PasswordDialog( Global.getActiveFrame(), filename, PasswordDialog.DEFINE );
//   if ( ct != null ) {
//      pwDlg.addActivityListener( ct.getActivityListener() );
//   }
//   pwDlg.setVisible( true );
   
   return null;
}  // enterNewPassphrase

/**
 * Lets the user enter a filepath from the local file system 
 * for a file of various types by offering a file chooser dialog.
 *  
 * @param parent parent display component 
 * @param title dialog title text
 * @param filetype functional orientation (*_CHOOSER type constants; default = 0) 
 * @param filters optional file filter types as of class <code>FileOpenDialog</code>;
 *        (defaults = -1)
 * @param startFile an optional selected start file in local file system
 * 
 * @return <code>ContextFile</code> with chosen filepath or <b>null</b> if broken
 */
public static ContextFile chooseSaveFile ( 
      Component parent, 
      String title,
      int filetype,
      int filters, 
      File startFile 
      )
{
   FileOpenDialog fc;
   File dir, file;
   String extent, path;
   
   // correct functional orientation
   if ( filetype < 0 | filetype > 3 )
      filetype = ANYFILE_CHOOSER;
   
   // defaults for filter installation
   if ( filters == -1 )
      if ( filetype == PWSFILE_CHOOSER )
         filters = FileOpenDialog.PWSFILE_FILTER | FileOpenDialog.BACKUP_FILTER;
      else if ( filetype == BACKUPFILE_CHOOSER )
         filters = FileOpenDialog.BACKUP_FILTER;
      else if ( filetype == EXECFILE_CHOOSER )
         filters = FileOpenDialog.EXECUTABLE_FILTER;
      else
         filters = 0;
   
   // initial directory
   dir = Global.currentDir;
   if ( filetype == BACKUPFILE_CHOOSER )
      dir = Global.backDir;
   
   // create an open dialog
   fc = new FileOpenDialog( filters, dir );
   if ( title != null )
      fc.setDialogTitle( title );
   
   if ( startFile != null )
      fc.setSelectedFile( startFile );

   // show the dialog
   if ( parent == null )
      parent = Global.getActiveFrame();
   if ( fc.showSaveDialog( parent ) == JFileChooser.APPROVE_OPTION &&
        (file = fc.getSelectedFile()) != null )
   {
      // remember current directory
      dir =  fc.getCurrentDirectory();
      if ( filetype == BACKUPFILE_CHOOSER )
         Global.backDir = dir;
      else if ( filetype != EXECFILE_CHOOSER )
         Global.currentDir = dir;

      // create default file extention
      extent = null;
      if ( filetype == PWSFILE_CHOOSER )
         extent = Global.DEFAULT_FILEEXTENTION;
      if ( filetype == BACKUPFILE_CHOOSER )
         extent = Global.DEFAULT_BACKUPEXTENTION;
    
      // return user file choice as ContextFile
      try { 
         // context file with a normalized filepath
         path = Service.normalizedFilepath( file.getPath(), extent );
         return IOManager.makeLocalContextFile( path );
      }
      catch ( Exception e )
      {
         failureMessage( "msg.url.formerror", e );
         return null;
      }
   }
   return null;
}  // chooseSaveFile

/** Displays an information message without parent component. It will get 
 *  centred within the application's mainframe.
 */
public static void infoMessage ( String title, String text )
{
   infoMessage( null, title, text );
}

/** Asks the user to confirm to overwrite the specified file. 
 * 
 * @param owner the parent component for the dialog; if <b>null</b> the current
 *        mainframe window is used
 * @param file the file in question as URL object
 * @return boolean user decission
 */
public static boolean overwriteConfirm ( Component owner, URL file )
{
   return overwriteConfirm( owner, new ContextFile(
         Global.getAdapter( file ), Global.getFilePath( file ) ));
}

/** Asks the user to confirm to overwrite the specified file. 
 * 
 * @param owner the parent component for the dialog; if <b>null</b> the current
 *        mainframe window is used
 * @param path the filepath info displayed in overwrite question 
 * @return boolean user decission
 */ 
public static boolean overwriteConfirm ( Component owner, String path )
{
   return overwriteConfirm( owner, new ContextFile( IOManager.getLocalFileAdapter(), path ));
}
 

/** Asks the user to confirm to overwrite the specified file. 
 * 
 * @param owner the parent component for the dialog; if <b>null</b> the current
 *        mainframe window is used
 * @param file the file in question as local <code>File</code> object 
 * @return boolean user decission
 */
public static boolean overwriteConfirm ( Component owner, File file )
{
   return overwriteConfirm( owner, file.getAbsolutePath() );
}

/** Asks the user to confirm to overwrite the specified file. If feasible the time of
 * last modification of this file is displayed. 
 * 
 * @param owner the parent component for the dialog; if <b>null</b> the current
 *        mainframe window is used
 * @param file <code>ContextFile</code> the context-file to be overwritten 
 * @return boolean user decision
 */
public static boolean overwriteConfirm ( Component owner, ContextFile file )
{
   String hstr, title, path;
   long time;
   boolean check;
   
   time = file.modifyTime();
   path = file.getFilepath();
   hstr = ResourceLoader.getDisplay( "msg.ask.overwritefile" );
   hstr = Util.substituteText( hstr, "$file", path );
   hstr = Util.substituteText( hstr, "$time", Global.getLocalDateTime( time ) );
   title = ResourceLoader.getDisplay( "dlg.confirm" );
   
   check = MessageDialog.showConfirmMessage( owner, title, hstr, 
           DialogButtonBar.YES_NO_BUTTON );
   ActionHandler.resetIdleTime();
   return check;
}

/** Asks the user via a password input dialog to enter a password. 
 *  Controls whether the input equals the password defined by the
 *  parameter. If both values mismatch, a corresponding message is displayed
 *  to indicate a failure.
 *  
 * @param pass PwsPassphrase the password control value
 * @param filename String name of the file in question (only filename; may be <b>null</b>) 
 * @param exitActive boolean if <b>true</b> the dialog shows a program exit button            
 * @return <b>true</b> if and only if the entered password equals <code>pass</code>
 */ 
public static boolean passwordControl ( PwsPassphrase pass, String filename, boolean exitActive )
{
   if ( pass == null ) return true;
   
   while ( true ) {
      // enter password dialog
	  PasswordDialog pwDlg;
	try {
		pwDlg = PasswordDialog.performed( null, filename, 
		                              PasswordDialog.CONTROL, exitActive );
	    if ( !pwDlg.isOkPressed() ) return false;
	    
	} catch (InterruptedException e) {
		return false;
	}

      // compare entered passphrase with defined passphrase
      PwsPassphrase enterPass = new PwsPassphrase( pwDlg.getEnteredPassword() );
      if ( !enterPass.equals( pass ) ) {
         infoMessage( "dlg.badvalue", "msg.failpassword" );
      } else { 
         return true;
      }
   }
}

/** Asks the user via a password input dialog to enter a password. 
 *  Controls whether the input equals the password defined by the
 *  parameter database container.  
 *  If values don't match, a corresponding message is displayed
 *  to indicate a failure.
 *  
 * @param c <code>PwsFileContainer</code> as a database reference
 * @param exitActive if <b>true</b> the dialog shows a program exit button            
 * @return <b>true</b> if and only if the parameter container holds a database
 *         and the entered passphrase equals the passphrase that is defined
 *         for that database
 */ 
public static boolean passwordControl ( PwsFileContainer c, boolean exitActive )
{
   return passwordControl( c.getPassphrase(), c.getDatabaseName(), exitActive );
}

/** Asks the user via a password input dialog to enter a password. 
 *  Controls whether the input equals the password defined by the
 *  parameter database container.  
 *  If values don't match, a corresponding message is displayed
 *  to indicate a failure.
 *  
 * @param c <code>PwsFileContainer</code> as a database reference
 * @return <b>true</b> if and only if the parameter container holds a database
 *         and the entered passphrase equals the passphrase that is defined
 *         for that database
 */ 
public static boolean passwordControl ( PwsFileContainer c )
{
   return passwordControl( c.getPassphrase(), c.getDatabaseName(), false );
}

/** Asks the user (dialog) to confirm a given question with "Yes" or "No".
 * 
 * @param owner the parent component for the dialog; if <b>null</b> the current
 *        mainframe window is used
 * @param text the question as text or token
 * @return boolean user decision (false in case of thread interruption)
 */ 
public static boolean userConfirm ( Component owner, String text )
{
   return userConfirm(owner, null, text);
}

/** Asks the user (dialog) to confirm a given question with "Yes" or "No".
 * 
 * @param owner the parent component for the dialog; if <b>null</b> the current
 *        mainframe window is used
 * @param title String the dialog title as text or token, may be null
 * @param text String the question as text or token
 * @return boolean user decision (false in case of thread interruption)
 */ 
public static boolean userConfirm ( Component owner, String title, String text )
{
   boolean result = MessageDialog.showConfirmMessage( owner, title, text, 
         DialogButtonBar.YES_NO_BUTTON );
   ActionHandler.resetIdleTime();
   return result;
}

/** Asks the user (dialog) to confirm a given question with "Yes" or "No". 
 * 
 * @param text the question as text or token
 * @return boolean user decision (false in case of thread interruption)
 */
public static boolean userConfirm ( String text )
{
   return userConfirm( null, text );
}

/** Returns the first top level <code>Window</code> component
 * that is the ancestor of the given component. 
 *  
 * @param c <code>Component</code>; may be <b>null</b>
 * @return <code>Window</code> or <b>null</b> if parameter was <b>null</b>
 */
public static Window getAncestorWindow ( Component c )
{
   while ( c != null && !(c instanceof Window) ) 
      c = c.getParent();
   return (Window)c;
}

/** Asks the user (dialog) to confirm a given question with "Yes", "No" or
 *  "Cancel". The possible return values are JOptionPane.YES_OPTION,
 *  JOptionPane.NO_OPTION, JOptionPane.CANCEL_OPTION. In case of broken
 *  dialog thread, the cancel value is returned.  
 * 
 * @param owner the parent component for the dialog; if <b>null</b> the current
 *        mainframe window is used
 * @param text the question as text or token
 * @return the user decision as an integer
 */
public static int userConfirmOption ( Component owner, String text )
{
//   MessageDialog dlg;
//   Window parent = getAncestorWindow( owner );
  
//   dlg = new MessageDialog( parent, text, MessageType.question, 
//         DialogButtonBar.YES_NO_CANCEL_BUTTON, true );
//   dlg.setTitle( ResourceLoader.codeOrRealDisplay( "dlg.confirm" ) );
//   dlg.pack();
//   dlg.setVisible( true );
   
   boolean[] answer = MessageDialog.showMessage(owner, null, text, 
		   MessageType.question, DialogButtonBar.YES_NO_CANCEL_BUTTON);
	   
   // translate result
   int result;
   if ( answer[0] )
      result = JOptionPane.YES_OPTION;
   else if ( answer[2] )
      result = JOptionPane.NO_OPTION;
   else
      result = JOptionPane.CANCEL_OPTION;
      
   ActionHandler.resetIdleTime();
   return result;
}

/** Asks the user to confirm a given question with "Yes", "No" or
 *  "Cancel". The dialog is centered within the application's mainframe.
 *  The possible return values are JOptionPane.YES_OPTION,
 *  JOptionPane.NO_OPTION, JOptionPane.CANCEL_OPTION. In case of broken
 *  dialog thread, the cancel value is returned.  
 * 
 * @param text the question as text or token
 * @return the user decision as an integer
 */
public static int userConfirmOption ( String text )
{
   return userConfirmOption( null, text );
}

/** Asks the user to confirm a given question with "Yes", "No" or
 *  "Cancel" while there is an additional option "Yes to All". This
 *  applies best for parsing lists with choices on elements. 
 *     
 *  @param owner the parent component for the dialog; if <b>null</b> the current
 *         mainframe window is used
 *  @param title the dialog title or <b>null</b> for a default       
 *  @param text the question as text or token
 *  @param type int, determines additional option with GUIService.YES_TO_ALL_OPTION 
 *         or 0 for no additional option
 *  @return int user decision, JOptionPane.YES_OPTION,
 *  JOptionPane.NO_OPTION, JOptionPane.CANCEL_OPTION, GUIService.YES_TO_ALL_OPTION 
 */     
public static int userConfirmListParsing ( Component owner, String title, String text, int type )
{
   String yes_value, no_value, yestoall_value, hstr;
   String options[];
   int opt, result;
   
   yes_value = ResourceLoader.getDisplay( "button.yes" );
   no_value = ResourceLoader.getDisplay( "button.no" );
   yestoall_value = ResourceLoader.getDisplay( "button.yestoall" );
   options = new String[3];
   options[0] = yes_value;
   options[1] = yestoall_value;
   options[2] = no_value;

   // create and show option panel
   if ( title == null )
      title = ResourceLoader.getDisplay( "dlg.confirm" );
   hstr = ResourceLoader.codeOrRealDisplay( text );
   opt = userOptionInput( owner, title, hstr, options, true );

   // interpret user answer and form function result
   switch ( opt )
   {
      case 0: result = JOptionPane.YES_OPTION;
      break;
      case 1: result = GUIService.YES_TO_ALL_OPTION;
      break;
      case 2: result = JOptionPane.NO_OPTION;
      break;
      default: result = JOptionPane.CANCEL_OPTION;
   }
   
   return result;
}

/** Asks the user for text input in a message dialog and returns the answer.
 *  
 * @param owner parent component; if <b>null</b> the current
 *        mainframe window is used
 * @param title , may be <b>null</b>
 * @param text message text displayed in dialog
 * @param initial initial value of the text input field, may be <b>null</b>
 * @return String user input or <b>null</b> if cancelled
 */
public static String userInput ( Component owner, String title, String text, 
      String initial )
{
   if ( title == null ) {
      title = "dlg.input";
   }
   title = ResourceLoader.codeOrRealDisplay( title );
   
   // create message content panel
   JPanel cp = new JPanel( new VerticalFlowLayout( 10, true ) );
   cp.add( MessageDialog.createMessageTextLabel( text ) );
   JTextField fld = new EditorTextField( initial );
   cp.add( fld );
   
   // call the message display (question)
   boolean ok = MessageDialog.showConfirmMessage( owner, title, cp, 
                DialogButtonBar.OK_CANCEL_BUTTON );

   // return text field value or null if user cancelled
   return ok ? fld.getText() : null;
}

/**
 * Presents the user a message text plus a combination of other display/input 
 * components in a confirm dialog with OK and CANCEL button. Returns the user
 * choice of termination button through a boolean value.
 *    
 * @param owner parent component, may be null
 * @param title dialog title, may be null for default
 * @param msg presented top element (message); may be <b>null</b>
 * @param content array of components to be shown (top to down)
 * 
 * @return boolean <b>true</b> == "OK" pressed, <b>false</b> == "CANCEL" pressed  
 */
public static boolean userCombiInput ( Component owner, 
                                   String title, 
                                   String msg, 
                                   Component[] content )
{
   if ( content == null )
      throw new NullPointerException();
   if ( owner == null )
      owner = Global.getActiveFrame();
   if ( title == null )
      title = "dlg.input";
   
   title = ResourceLoader.codeOrRealDisplay( title );
   JPanel panel = new JPanel( new VerticalFlowLayout( 10 ) );
   if ( msg != null ) {
      JLabel label = new JLabel( msg );
      panel.add( label );
   }
   for ( int i = 0; i < content.length; i++ ) {
      panel.add( content[ i ] );
   }
   
   boolean result = MessageDialog.showConfirmMessage( owner, title, panel, 
            DialogButtonBar.OK_CANCEL_BUTTON );
   return result;
}

/**
 * Presents the user with an input request for a login consisting of user-name
 * and password. The input is returned in a single string with the format 
 * "user:password". The dialog is a confirm dialog with OK and CANCEL button. 
 * This returns the input result in case of "OK" or <b>null</b> in case of 
 * user abortion.
 *    
 * @param owner parent component, may be null
 * @param title dialog title, may be null for default
 * @param msg presented top element (message); may be null
 * 
 * @return String formatted input result or <b>null</b> if user cancel  
 */
public static String loginInput ( Component owner, String title, String msg ) {
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.LINE_START;
    c.ipadx = 10;
    c.ipady = 2;
    c.insets = new Insets(2, 0, 2, 0);
	
	JPanel panel = new JPanel(gridbag);
	String userFldName = ResourceLoader.getDisplay("dlg.input.user");
	String passFldName = ResourceLoader.getDisplay("dlg.input.password");
	EditorTextField userFld = new EditorTextField(20);
	JPasswordField passFld = new JPasswordField(20);

	panel.add(new JLabel(userFldName), c);
	c.gridwidth = GridBagConstraints.REMAINDER; //end row
	panel.add(userFld, c);
	c.gridwidth = GridBagConstraints.RELATIVE; // start of line
	panel.add(new JLabel(passFldName), c);
	c.gridwidth = GridBagConstraints.REMAINDER; //end row
	panel.add(passFld, c);
	
	boolean ok = userCombiInput(owner, title, msg, new Component[] {panel});
	if ( ok ) {
		String result = userFld.getText().trim() + ":" 
						+ new String(passFld.getPassword()).trim();
		return result;
	}
	return null;
}

/**
 * Shows a dialog with a message text and a list of button-borne
 * choices the user can select from. The choice performed, or CANCEL, are
 * returned as an integer value. CANCEL is always a possible return value
 * independent of the setting of "cancelOption". 
 * 
 * @param owner parent component; may be <b>null</b>
 * @param title dialog title; may be <b>null</b>
 * @param msg presented top element (message); may be <b>null</b>
 * @param choices String[] where each element is the text of a choice
 *        available to the user; may be <b>null</b>
 * @param msgType <code>MessageDialog.MessageType</code> determines icon + appearance       
 * @param cancelOption boolean whether a dialog cancel option should 
 *        be available
 * 
 * @return int user choice (0..nrOfChoices-1; -1 for cancel; -2 for interrupted/exception)
 * @since 0.6.0
 */
public static int userOptionInput ( Component owner,
                                    String title,
                                    String msg,
                                    String[] choices, 
                                    MessageType msgType,
                                    boolean cancelOption )
{
   return UserOptionDialog.performed(owner, title, msg, choices, msgType, cancelOption);
}

/**
 * Shows a dialog with a message text and a list of button-borne
 * choices the user can select from. The choice performed, or CANCEL, are
 * returned as an integer value. CANCEL is always a possible return value
 * independent of the setting of "cancelOption". 
 * 
 * @param owner parent component; may be <b>null</b>
 * @param title dialog title; may be <b>null</b>
 * @param msg presented top element (message); may be <b>null</b>
 * @param choices String[] where each element is the text of a choice
 *        available to the user; may be <b>null</b>
 * @param cancelOption boolean whether a dialog cancel option should 
 *        be available
 * 
 * @return int user choice (0..nrOfChoices-1; -1 for cancel)
 * @since 0.6.0
 */
public static int userOptionInput ( Component owner,
                                    String title,
                                    String msg,
                                    String[] choices, 
                                    boolean cancelOption )
{
   return userOptionInput( owner, title, msg, choices, MessageType.question, cancelOption );
}

/** Returns a non-editable JComboBox loaded with the currently available 
 *  character sets (text encoding) of the Java VM, the default charset being
 *  the current box selection.
 *   
 *  @return JComboBox, option loaded JComboBox 
 */
public static JComboBox getListedCharsetsCombo ()
{
   SortedMap<String, Charset> map;
   JComboBox co;
   Charset[] objs;
   
   map = Charset.availableCharsets();
   objs = map.values().toArray(new Charset[0]);
   co = new JComboBox( objs );
   co.setSelectedItem( Charset.forName( Global.getDefaultCharset() ));
   return co;
}

/**
 * Organises user entry of an integer within a given range. The user may
 * cancel the operation in which case an exception is thrown.
 * 
 * @param parent parent component for window
 * @param title dialog title (defaults) 
 * @param msg dialog message (defaults)
 * @param low lower bound of validity range
 * @param high higher bound of validity range
 * @param init initial edit value
 * @return a valid user input
 * 
 * @throws UserBreakException if cancel was pressed or irregular end of dialog
 */
public static int integerInput ( Component parent, String title, String msg, int low, int high, int init )
   throws UserBreakException
{
   int j;
   
   // install default title and message if required 
   title = ResourceLoader.codeOrRealDisplay( title == null ? "dlg.input" : title );
   if ( msg == null ) {
      msg = ResourceLoader.getDisplay( "dlg.input.integer.range" );
      msg = Util.substituteText( msg, "$lowbound", String.valueOf( low ) );
      msg = Util.substituteText( msg, "$highbound", String.valueOf( high ) );
   } else {
      msg = ResourceLoader.codeOrRealDisplay( msg );
   }
   
   while ( true ) {
      // ask user for text input
      String hstr = String.valueOf( init );
      hstr = userInput( parent, title, msg, hstr );

      // check for user break
      ActionHandler.resetIdleTime();
      if ( hstr == null )
         throw new UserBreakException();
      
      try {
         // extract integer value and check range
         j = Integer.parseInt( hstr.trim() );
         if ( j < low | j > high ) {
            // error message: value out of range
            hstr = ResourceLoader.getDisplay( "msg.illegalinteger.range" );
            hstr = Util.substituteText( hstr, "$lowbound", String.valueOf( low ) );
            hstr = Util.substituteText( hstr, "$highbound", String.valueOf( high ) );
            GUIService.infoMessage( parent, "dlg.badvalue", hstr );
         }
         else
            break;

      } catch ( NumberFormatException e ) {
         // error message: invalid input (not a number)
         GUIService.infoMessage( parent, "dlg.badvalue", "msg.badintegervalue");
      }
   }
   return j;
}  // integerInput

/**
 * Creates a new secret passphrase from a cryptographically satisfying random generator.
 * Shows a dialog which displays the generated passphrase and offers option to modify
 * the parameter passphrase policy.
 *    
 * @param parent  <code>Component</code> parent component for dialog; may be <b>null</b>
 * @param title <code>String</code> title of dialog; <b>null</b> for default
 * @param policy <code>PwsPassphrasePolicy</code> passphrase policy for generation (may get edited 
 *         and modified during the dialog)
 * @return <code>PwsPassphrase</code> a newly generated passphrase or <b>null</b> if dialog was broken
 *         or cancelled
 * @since  0-6-0       
 */
public static PwsPassphrase generateRandomPassphrase ( Component parent, 
		                    String title, 
                            PwsPassphrasePolicy policy )
{
   String hstr, key, dkey, text, yes_value, no_value, policy_value;
   String opt2[];
   int option;
   boolean policyEditOk, externalClose;
   
   // control parameters
   if ( policy == null )
      throw new IllegalArgumentException( "policy == null" );
   if ( parent == null )
      parent = Global.getActiveFrame();
   
   try {
      text = ResourceLoader.getDisplay( "msg.genpassword" );
      yes_value = ResourceLoader.getDisplay( "button.yes" );
      no_value = ResourceLoader.getDisplay( "button.no" );
      policy_value = ResourceLoader.getDisplay( "button.policy" );
      opt2 = new String[3];
      opt2[0] = yes_value;
      opt2[1] = no_value;
      opt2[2] = policy_value;

      policyEditOk = true;
      externalClose = false;
      key = dkey = "";

      do {
         // generate a new password according to current build settings
         if ( policyEditOk ) {
            // (this can throw the InvalidPassphrasePolicy)
            key = new String( PassphraseUtils.makePassword( policy ) );
            dkey = Util.htmlEncoded( key );
         }
         hstr = Util.substituteText( text, "$password", dkey );
         policyEditOk = true;

         // create and show option panel
         // (this displays the created passphrase and gives user choices) 
         if ( title == null ) {
            title = ResourceLoader.getDisplay( "dlg.confirm" );
         }
         option = userOptionInput( parent, title, hstr, opt2, true );

         // interpret user answer and form function result
         switch ( option ) {
            case 0: 
               // user confirm is "Yes": return the new passphrase
               return new PwsPassphrase( key.toCharArray() );

            case 2:
               // branch to policy edit 
               // initiate and perform edit-policy dialog
               int modus = policy == Global.passphrasePolicy ? 0 : policy == Global.generatorPolicy ? 2 : 1;
               PolicyDialog dlg = PolicyDialog.performed(parent, null, policy, modus);
               dlg.dispose();
   
               externalClose = dlg.isUnselected();
               if ( policyEditOk = dlg.isOkPressed() ) {
                  policy.setFromInternal( dlg.getEditedPolicy().getInternalForm() );
               }
            break;
         }
            
      // do while user confirm is "NO"
      } while ( option != -1 & !externalClose );

   } catch ( InvalidPassphrasePolicy e ) {
      e.printStackTrace();
      GUIService.failureMessage( "msg.badpasspolicy", null );
   }
   return null;
}

private static HashMap<String, HelpDialog> objectMap = new HashMap<String, HelpDialog>(); 

/**
 * Convenience class to initiate a <code>HtmlBrowserDialog</code>
 * with a help text identified by a display token.
 * 
 * @since 0-5-0
 */
private static class HelpDialog extends HtmlBrowserDialog 
{
   private String name;

   /**
    * @param owner Dialog
    * @param name systematic dialog name (unique)
    * @param modal boolean
    * @throws HeadlessException
    */
   public HelpDialog ( Dialog owner, String name ) throws HeadlessException {
      super( owner, ResourceLoader.getDisplay( name ), false );
      init( name );
   }

   /**
    * @param owner Frame
    * @param name systematic dialog name (unique)
    * @param modal boolean
    * @throws HeadlessException
    */
   public HelpDialog ( Frame owner, String name ) throws HeadlessException {
      super( owner, ResourceLoader.getDisplay( name ), false );
      init( name );
   }

   private void init ( String name ) {
      this.name = name;
      setClipping( false );
      setAutonomous( true );
   }
   
   public void dispose () {
      super.dispose();
      objectMap.remove( name );
   }
   
}

/**
 * Closes an open help dialog (and removes if from object registry).
 *  
 * @param token systematic name of the help dialog; should start with "dlg.help." followed
 *        by an individual name
 * @return HtmlBrowserDialog the help dialog closed of <b>null</b> if this dialog wasn't found          
 */
public static synchronized void closeHelpDialog ( final String token )
{
	Runnable run = new Runnable() {
	@Override
	public void run() {
	   try { 
		   HelpDialog helpDialog = objectMap.get( token ); 
	      if ( helpDialog != null ) {
	         helpDialog.dispose();
	      }
	   } catch ( Exception e ) {
	   }
	}
   };
   ActionHandler.executeOnEDT(run);
}

/**
 * Whether an object with the specified name is registered in the service's 
 * object map. (This e.g. holds open help dialogs.)
 *  
 * @param name systematic name of the object
 * @return boolean <b>true</b> if and only if an object with the given name is registered
 */
public static boolean isRegisteredObject ( String name )
{
   return name != null && objectMap.containsKey( name );
}

/**
 * Starts or closes a non-modal help dialog. Can be called repeatedly but only one
 * object is created until it is disposed (e.g. by user "Cancel"). The dialog
 * bounds are memorised in global options and it moves connected with its parent
 * window. 
 * 
 * <p>Convention on resources:  
 *    a) dialog title from Display resource bundle with code == token
 *    b) dialog content from a file in "#standards" resource directory with 
 *       name defined in Commands resource bundle entry "html.file." + token
 *  
 * @param owner <code>Dialog</code> or <code>Frame</code> as the help dialog owner;
 *        if <b>null</b> the global main frame is referenced
 * @param token systematic name of the help dialog; should start with "dlg.help." followed
 *        by an individual name   
 * @since 0-6-0
 */
public static void toggleHelpDialog ( Window owner, String token )
{
   if ( isRegisteredObject( token ) ) {
      closeHelpDialog( token );
   } else {
      startHelpDialog( owner, token );
   }
}

/**
 * Starts a non-modal help dialog. Can be called repeatedly but only one
 * object is created until it is disposed (e.g. by user "Cancel"). The dialog
 * bounds are memorised in global options and it moves connected with its parent
 * window. 
 * 
 * <p>Convention on resources:  
 *    a) dialog title from Display resource bundle with code == token
 *    b) dialog content from a file in "#standards" resource directory with 
 *       name defined in Commands resource bundle entry "html.file." + token
 *  
 * @param owner <code>Dialog</code> or <code>Frame</code> as the help dialog owner;
 *        if <b>null</b> the global main frame is referenced
 * @param token systematic name of the help dialog; should start with "dlg.help." followed
 *        by an individual name   
 * @since 0-5-0
 */
public static synchronized void startHelpDialog ( final Window owner, final String token )
{
	Runnable run = new Runnable() {

	@Override
	public void run() {
	   HelpDialog helpDialog = null;
	   try { helpDialog = objectMap.get( token ); }
	   catch ( Exception e )
	   {}
	   
	   if ( helpDialog == null ) {
		  Window ancestor = owner;
	      if ( ancestor == null ) {
	    	  ancestor = Global.getActiveFrame();
	      }

	      if ( ancestor instanceof Dialog )
	         helpDialog = new HelpDialog( (Dialog)ancestor, token );
	      else if ( ancestor instanceof Frame )
	         helpDialog = new HelpDialog( (Frame)ancestor, token );
	      else
	         throw new IllegalArgumentException("owner must be Frame or Dialog");
	      
	      String bounds_tok = "bounds_".concat( token );
	      String content_tok = "html.file.".concat( token );
	      helpDialog.setBoundsToken( bounds_tok, true );
	      try {
	         String hstr = "#standards/" + ResourceLoader.getCommand( content_tok );
	         helpDialog.setPage( ResourceLoader.getResourceURL( hstr ) );
	         objectMap.put( token, helpDialog );

	      } catch ( IOException e ) {
	         e.printStackTrace();
	      }
	   }
	   helpDialog.show();
	}
   };
   ActionHandler.executeOnEDT(run);
}  // startHelpDialog

/** Displays an error message dialog referring to the parameter exception.
 * 
 * @param text the message token or plain text; if <b>null</b> a standard 
 *        message is used
 * @param e if not <b>null</b> this exception is reported in the message text 
 *        
 */ 
public static void failureMessage ( String text, Exception e )
{
   failureMessage( Global.getActiveFrame(), text, e );
}

/** Returns a non-editable JComboBox loaded with the currently available 
 *  database containers represented by their database name. 
 *   
 *  @return JComboBox, option loaded 
 */
public static JComboBox getDesktopContainerCombo ()
{
   ArrayList<PwsFileContainer> list = new ArrayList<PwsFileContainer>();
   PwsFileContainer ct;
   Iterator<PwsFileContainer> it;
   JComboBox co;
   
   for ( it = (Iterator<PwsFileContainer>)DisplayManager.getContainerIterator(); 
		 it.hasNext(); ) {
      ct = it.next();
      list.add( ct );
   }
   co = new TooltipComboBox( list.toArray() );
//   co.setRenderer( new TooltipComboBoxRenderer() );
//   co.setToolTipText( "Hey, this is my COMBO chooser!" );
   return co;
}

/** Returns a specialised JComboBox for the GROUP field of a PWS record
 *  with added logic. The combo reflects all available distinct GROUP values
 *  in the parameter value list.
 *  <p>If the WIDTH limit is exceeded by the concrete data set, a combo with 
 *  a prototype-display is rendered and Tooltips are shown
 *  for each item value displaying the complete value. The combo is displayed
 *  with DATAFONT.
 *  
 *  @param values List containing group values, may be <b>null</b>  
 *  @param editable boolean whether the combo is rendered editable
 *  @param limit int intended maximum width in pixels of the combo 
 */  
 public static JComboBox getGroupListCombo ( List<String> values, 
		                                   boolean editable, int limit )
{
   JComboBox co;
   Font font;
   Object[]    grpItems;
   
   if ( values == null )
      values = new ArrayList<String>();
   grpItems = values.toArray(); 

   font = DisplayManager.getFont( "data" );
   co = new JComboBox( grpItems ); 
   co.setFont( font );

   // create combobox with fix width if necessary
   if ( values.size() > 100 || co.getPreferredSize().width > limit ) {
      co = new TooltipComboBox( grpItems ); 
      co.setFont( font );
      co.setPrototypeDisplayValue( "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD" );
//      System.out.println( "-- TOOLTIP-COMBOBOX INSTALLED" ); 
   }

   co.setEditable( editable );
   ActionHandler.registerChangeableObject( co );
   
   return co;
}

 /** Returns a specialised JComboBox for the GROUP field of a PWS record
  *  with added logic. The combo reflects all available distinct GROUP values
  *  in the parameter database.
  *  <p>If the WIDTH limit is exceeded by the concrete data set, a combo with 
  *  prototype-display of the given width is rendered and Tooltips are shown
  *  for each item value displaying the complete value. The combo is displayed
  *  with DATAFONT.
  *  
  *  @param ct PwsFileContainer container that harbours the database 
  *  @param editable boolean whether the combo is rendered editable
  *  @param limit int intended maximum width in pixels of the combo 
  */  
  public static JComboBox getGroupListCombo ( PwsFileContainer ct, boolean editable, int limit )
  {
     return getGroupListCombo( ct == null ? null : ct.getGroupList(), editable, limit );
  }

/** Returns a concatenation of parameter <tt>text</tt> and an excerpt
 *  from the given <tt>Exception</tt> making it informative to the user.
 *  If <tt>e</tt> is <b>null</b>, <tt>text</tt> is returned. 
 *  (<tt>text</tt> should be Html encoded.)
 *      
 * @param text String message ante-text; may be <b>null</b> 
 * @param e Exception; may be <b>null</b>
 * @return String Html encoded message 
 */
private static String getExceptionMessage ( String text, Exception e )
{
   String msg = text;
   
   if ( e != null ) {
      msg = e.getMessage(); 
      msg = (text != null ? text : "") + "<p>" + e.getClass().getName() + (msg == null ? "" : 
             "<p><font color=red>".concat( msg ).concat( "</font>" ));
   }
   return msg;
}
  
/** Displays an error message dialog referring to the parameter exception.
 * 
 * @param owner Component owner of the message dialog
 * @param text the message token or plain text; if <b>null</b> a standard 
 *        message is used
 * @param e if not <b>null</b> this exception is reported in the message text 
 *        
 */ 
public static void failureMessage ( Component owner, String text, Exception e )
{
   if ( text == null ) {
      text = "msg.failure.general";
   }
   
   String title = ResourceLoader.getDisplay( "dlg.operfailure" );
   String hstr = "<html><b>".concat( ResourceLoader.codeOrRealDisplay( text ) ); 
   hstr = getExceptionMessage( hstr, e );
   MessageDialog.showInfoMessage( owner, title, hstr, MessageType.error );
}

/**
 * Returns the specified component's toplevel <code>Frame</code> or
 * <code>Dialog</code>.
 * (Note: Taken from JOptionPane JDK 1.6)
 * 
 * @param parentComponent the <code>Component</code> to check for a 
 *      <code>Frame</code> or <code>Dialog</code>
 * @return the <code>Frame</code> or <code>Dialog</code> that
 *      contains the component, or the default
 *          frame if the component is <code>null</code>,
 *      or does not have a valid 
 *          <code>Frame</code> or <code>Dialog</code> parent
 * @exception HeadlessException if
 *   <code>GraphicsEnvironment.isHeadless</code> returns
 *   <code>true</code>
 * @see java.awt.GraphicsEnvironment#isHeadless
 */
public static Window getWindowForComponent(Component parentComponent) 
    throws HeadlessException {
    if (parentComponent == null)
        return Global.getActiveFrame();
    if (parentComponent instanceof Frame || parentComponent instanceof Dialog)
        return (Window)parentComponent;
    return getWindowForComponent(parentComponent.getParent());
}

public static void fileNotExistsInfo (ContextFile file) {
	fileNotExistsInfo(null, file);
}

public static void fileNotExistsInfo (Component owner, ContextFile file) {
    String hstr = ResourceLoader.getDisplay( "msg.filenotfound" );
    hstr = Util.substituteText( hstr, "$path", file.getFilepath() );
    GUIService.infoMessage( owner, "dlg.operrejected", hstr );
}

public static void DirectoryNotExistsInfo (Component owner, File file) {
    String hstr = ResourceLoader.getDisplay( "msg.nosuch.directory" );
    hstr = Util.substituteText( hstr, "$path", file.getAbsolutePath() );
    GUIService.infoMessage( owner, "dlg.operrejected", hstr );
}

/** Starts a filechooser dialog to determine the external email application
 *  path value in program options.
 *  
 *  @return the dialog termination option (<code>FileOpenDialog</code>)
public static int editEmailApplOption ( Component parent )
{
   String executable, path;
   FileOpenDialog fc;
   File file, startDir;
   int option;

   path = Options.getOption( "emailApplication" );
   startDir = path.equals( "" ) ? null : new File( path ).getParentFile();
   if ( startDir == null )
      startDir = Global.getUserHome();
   
   fc = new FileOpenDialog( FileOpenDialog.EXECUTABLE_FILTER, startDir );
   fc.setDialogTitle( ResourceLoader.getDisplay( "dlg.choosemailapp" ));
   option = fc.showOpenDialog( parent == null ? Global.getActiveFrame() : parent );
   fc.dispose();
   ActionHandler.resetIdleTime();
   if ( option == JFileChooser.APPROVE_OPTION &&
        (file = fc.getSelectedFile()) != null )
   {
      executable = file.isFile() ? file.getAbsolutePath() : null;
      Options.setOption( "emailApplication", executable );
   }
   return option;
}
 */   



}
