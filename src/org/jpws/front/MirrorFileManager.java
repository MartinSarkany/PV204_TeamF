package org.jpws.front;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jpws.front.util.Util;

/** 
 * Class that allows automated mirroring of data files into copies or other kind of
 * representations on a separate storage area (currently on local disk space) on a 
 * frequent basis.
 * 
 * <p>Data handling classes which implement the <code>MirrorableFile</code> interface
 * can hereby create relation to automated data saving (mirroring mechanism) which runs
 * silently in the background. In the regular case this mechanism would function as a
 * safeguard parallel to user-triggered save operations, but other solutions are possible.
 */

public class MirrorFileManager {

    public static final String DEFAULT_MIRROR_SUFFIX = ".bak";
    public static final String DEFAULT_MIRROR_PREFIX = "mir-";

    private static final String CHECKTHREAD_NAME = "MirrorFileManager.filechecking";  
    private static final String SAVETHREAD_NAME = "MirrorFileManager.saving";
    
    private Vector<MirrorableFile>  mfList = new Vector<MirrorableFile>();
    private HashMap<MirrorableFile, MirrorFileAdminRecord> mfRecords 
                = new HashMap<MirrorableFile, MirrorFileAdminRecord>(); 
    private ArrayList<OperationListener> operListeners;

    private CheckThread     checkThread; 
    
    private File            mirrorRootDir;
    private String          mirrorFilePrefix = DEFAULT_MIRROR_PREFIX;
    private String          mirrorFileSuffix = DEFAULT_MIRROR_SUFFIX;
    private int             checkPeriod;
    private boolean         terminated;
    

    /** Creates a new mirror file manager with the same background thread priority
     * as the calling thread.
     * 
     * @param rootDirectory <code>File</code> location of mirror files
     * @param period int time in seconds between file investigation loops
     */
    public MirrorFileManager ( File rootDirectory, int period ) {
        this(rootDirectory, period, Thread.currentThread().getPriority());
    }
    /** Creates a new mirror file manager.
     * 
     * @param rootDirectory <code>File</code> location of mirror files
     * @param period int time in seconds (&gt;0) between file investigation loops
     * @param threadPriority int thread priority setting for this manager's background tasks
     */
    public MirrorFileManager ( File rootDirectory, int period, int threadPriority ) {
        if ( rootDirectory == null ) {
            throw new NullPointerException( "root dir missing");
        }
        if ( period < 1 ) {
            throw new IllegalArgumentException( "illegal check period: " + period );
        }
            
        mirrorRootDir = rootDirectory.getAbsoluteFile();
        checkPeriod = period;
        init( threadPriority );
    }

    private void init ( int threadPriority ) {
        
        // create the file checking thread
        checkThread = new CheckThread(threadPriority);
        checkThread.start();
    }

    /** Sets the filename prefix for mirror files.
     * The prefix can be left void by setting it to "" or
     * <b>null</b>. It has a default value of <code>DEFAULT_MIRROR_PREFIX</code>.
     * 
     * @param prefix String, may be <b>null</b>
     */
    public void setMirrorFilePrefix ( String prefix ) {
        mirrorFilePrefix = prefix == null ? "" : prefix;
    }
    
    /** Sets the filename extension (suffix) for mirror files.
     * The suffix must not be void. It has a default value 
     * of <code>DEFAULT_MIRROR_SUFFIX</code>.
     * 
     * @param suffix String, may be <b>null</b>
     */
    public void setMirrorFileSuffix ( String suffix ) {
        if ( suffix == null || suffix.length() < 2 || 
             suffix.indexOf('.') != 0  )
            throw new IllegalArgumentException("suffix is empty or improperly set (must start with '.')");
        
        mirrorFileSuffix = suffix;
    }
    
    /** Causes the file-controlling thread of this manager to pause
     * execution until a call to <code>resume()</code> occurs.
     * (After calling this method it is still possible that an ongoing
     * mirror-save thread is executing, however it is guaranteed that 
     * no new manager thread is scheduled.)
     */
    public void pause () {
        checkThread.pause();
    }

    /** Causes the file-controlling thread of this manager to
     * resume execution if it is in PAUSE state.
     */
    public void resume () {
        checkThread.endPause();
    }

    /** Terminally stops execution of the file-controlling thread.
     * No further call-back functions or event dispatches will be 
     * executed.
     */
    public void exit () {
        checkThread.terminate();
        terminated = true;
    }
    
    /**
     * Triggers off mirror save activity for all registered mirrorable files.
     * The save is only performed on files with an open modified marker.
     * This command is designed to provoke immediate action from the controller
     * thread and ends a possible sleeping state. CAUTION! This command 
     * is not required for regular execution of the manager's controller!
     */
    public void invokeMirrorActivity ()
    {
        if ( checkThread.isAlive() & !terminated ) {
            checkThread.kick();
        }   
    }

    /** Returns the root directory for the manager to
     * store and retrieve mirror files. The rendered
     * file is an absolute path.
     *  
     * @return <code>File</code> mirror manager root directory 
     */
    public File getMirrorRootDirectory () {
        return mirrorRootDir;
    }
    
    /** Returns the mirror-file of the current program session
     * for the given mirrorable file or <b>null</b> if it doesn't
     * exists. (For deleting the mirror file, method <code>
     * removeCurrentMirror()</code> should be used to allow the 
     * manager for proper state updates.)
     * 
     * @param f <code>MirrorableFile</code>
     * @return <code>File</code> current session mirror file or <b>null</b>
     *         if unavailable
     */
    public File getCurrentMirror ( MirrorableFile f ) {
        MirrorFileAdminRecord rec = getFileAdminRec(f);
        if ( rec != null ) {
            return rec.mirrorFile;
        }
        return null;
    }
    
    /** Returns an iterator over all known history mirrors for a
     * given mirrorable file. History mirrors are mirror files of
     * previous program sessions. They can amount to any number.
     * <code>iterator.remove()</code> does not delete any file .
     * It is save to delete rendered files at any time as they
     * do not interfere with the file mirror mechanics.
     * 
     * @param f <code>MirrorableFile</code>
     * @return <code>Iterator&lt;File&gt; history mirror files
     *         or <b>null</b> if no files found
     */
    public Iterator<File> getHistoryMirrorIterator ( MirrorableFile f ) {
        MirrorFileAdminRecord rec = getFileAdminRec(f);
        if ( rec != null ) {
            File dir = rec.getHistoryDir();
            // this retrieves all mirror files in the specific history dir
            // of f. MirrorFileAdminRecord serves as list filter
            File[] fileArr = dir.listFiles( rec );
            if ( fileArr != null ) {
                List<File> list = Arrays.asList( fileArr );
                return list.isEmpty() ? null : list.iterator();
            }
        }
        return null;
    }

    /** Removes all history mirror files of the given mirrorable file.
     * (This also removes the specific history directory for this file
     * if the directory is empty after deletion of the mirrors.)
     *  
     * @param f <code>MirrorableFile</code>
     * @return boolean <b>true</b> if and only if all listed
     *         mirror files were deleted. On <b>false</b> the appl.
     *         should check <code>getHistoryMirrorIterator()</code>
     *         for files which could not get deleted. 
     * 
     */
    public boolean removeHistoryMirrors ( MirrorableFile f ) {
        boolean ok = true;
        MirrorFileAdminRecord rec = getFileAdminRec(f);
        if ( rec != null ) {
            // delete all history mirrors as known by the iterator
            Iterator<File> it = getHistoryMirrorIterator(f); 
            for ( ; it.hasNext();  ) {
                File file = it.next();
                ok &= file.delete();
            }

            // delete the directory
            File dir = rec.getHistoryDir();
            dir.delete();
        }
        return ok;
    }
    
    /** Removes the current session mirror of the given mirrorable file.
     * If a current mirror save thread is ongoing, deletion takes 
     * place immediately after saving has terminated. 
     * 
     * @param f <code>MirrorableFile</code>
     */
    public void removeCurrentMirror ( MirrorableFile f ) {
        MirrorFileAdminRecord rec = getFileAdminRec(f);
        
        // operate if mirrorable is registered and a mirror file known
        if ( rec != null & rec.mirrorFile != null ) {
            // if a save-thread is running, just mark the file for later deletion
            if ( rec.threadActive() ) {
                rec.deleteMarked = true;
            // otherwise attempt delete and update save-number    
            } else if ( rec.mirrorFile.delete() ) {
                rec.fileSaveNumber = f.getModifyNumber();
                rec.mirrorFile = null;
            } else {
                fireErrorEvent(rec, "unable to erase mirror-file: "
                    .concat(rec.mirrorFile.getAbsolutePath()), null);
            }
        }
    }
    
    /** Iterator over contained mirrorable files.
     * 
     * @return <code>Iterator&lt;MirrorableFile&gt;</code>
     */
    public Iterator<MirrorableFile> iterator () {
         return mfList.iterator();
    }
    
    /** This method checks whether a mirror of the mirrorable file is present in the 
     * current mirror directory. If so, it moves the mirror file (renamed) into a special 
     * directory for the mirrorable and informs the user about its existence.
     */
    protected void controlMirrors ( MirrorFileAdminRecord admin ) {
       File mdir, mir=null, copy;
       String hstr;
       
       try
       {
          mir = admin.getMirrorFileDef();
          mdir = admin.getHistoryDir();
    
          // if a MIRROR exists for this database (primary occurrence)
          if ( mir.exists() ) {
             // copy mirror file into private mirror directory
             // create private directory if needed
             Util.ensureDirectory( mdir, null );
             copy = File.createTempFile( mirrorFilePrefix, mirrorFileSuffix, mdir );
             Util.copyFile(mir, copy);
//             Log.debug( 7, "(MirrorFileManager.controlMirrors) created private MIRROR copy: "
//                   .concat( copy.getAbsolutePath() ));
    
             // delete original mirror file
             mir.delete();
             
             // fire event of "mirror-file found"
             fireMirrorFileFound( admin, copy );
          }
          
          
          // else check for erasing an empty history mirror directory 
          else if ( mdir.isDirectory() && mdir.listFiles().length == 0 ) {
             mdir.delete();
//             Log.debug( 7, "(PwsFileContainer.controlMirrors) removed private MIRROR directory: "
//                   .concat( mdir.getAbsolutePath() ));
          }
    
       }
       catch ( IOException e )
       {
          e.printStackTrace();
          hstr = "WARNING! Cannot copy Mirror file<br><font color=\"green\">" + 
          mir.getAbsolutePath() + "</font>";

          fireErrorEvent( admin, hstr, e );
       }
    }  // controlMirrors

    /** Adds a mirrorable file to administration in this manager.
     * 
     * @param f <code>MirrorableFile</code>
     */
    public void addMirrorable ( MirrorableFile f ) {
        if ( !(mfList.contains(f) | terminated) ) {
            mfList.add(f);
            MirrorFileAdminRecord adminRec = new MirrorFileAdminRecord(f);
            mfRecords.put(f, adminRec);
            fireListModified(adminRec);
            controlMirrors(adminRec);
        }
    }
    
    /** Removes the given mirrorable file from administration in
     * this mirror file manager. This does not perform any removal
     * of mirror files! After the mirrorable file has been removed,
     * this manager cannot remove any of its mirror files. 
     * 
     * @param f <code>MirrorableFile</code>
     */
    public void removeMirrorable ( MirrorableFile f ) {
        if ( mfList.remove(f) ) {
            MirrorFileAdminRecord rec = mfRecords.remove(f);
            fireListModified( rec );
        }
    }
    
    /** Returns the file administration record for a specific 
     * mirrorable file or <b>null</b> if it doesn't exists.
     * 
     * @param f <code>MirrorableFile</code>
     * @return <code>MirrorFileAdminRecord</code> or <b>null</b>
     */
    protected MirrorFileAdminRecord getFileAdminRec ( MirrorableFile f ) {
        return mfRecords.get(f);
    }

    
//  **************  EVENT HANDLING  ***************    

    @SuppressWarnings("unchecked")
    private void dispatchOperationEvent( OperationEvent evt ) {
        if ( evt != null & operListeners != null ) {
            ArrayList<OperationListener> list;
            synchronized (operListeners) {
                list = (ArrayList<OperationListener>) operListeners.clone();
            }
            for (OperationListener li : list) {
                switch ( evt.getType() ) {
                    case OperationEvent.SAVESTART_EVENT: li.saveStarted(evt);
                    break;
                    case OperationEvent.SAVEREADY_EVENT: li.saveTerminated(evt);
                    break;
                    case OperationEvent.ERROR_EVENT: li.errorOccurred(evt);
                    break;
                    case OperationEvent.LISTCHANGE_EVENT: li.fileListChanged(evt);
                    break;
                }
            }
        }
    }
    
    protected void fireListModified( MirrorFileAdminRecord adminRec ) {
        OperationEvent evt = new OperationEvent( this, OperationEvent.LISTCHANGE_EVENT, adminRec );
        dispatchOperationEvent(evt);
    }

    protected void fireSaveStarted( MirrorFileAdminRecord adminRec ) {
        OperationEvent evt = new OperationEvent( this, OperationEvent.SAVESTART_EVENT, adminRec );
        dispatchOperationEvent(evt);
    }

    protected void fireSavePerformed(MirrorFileAdminRecord adminRec) {
        OperationEvent evt = new OperationEvent( this, OperationEvent.SAVEREADY_EVENT, adminRec );
        dispatchOperationEvent(evt);
    }
    
    protected void fireMirrorFileFound( MirrorFileAdminRecord admin, File mirror ) {
        admin.file.mirrorDetected(mirror);
    }
    
    protected void fireErrorEvent(MirrorFileAdminRecord adminRec, String hstr, IOException e) {
        if ( e != null ) {
            e.printStackTrace();
            OperationEvent evt = new OperationEvent( this, e, hstr, adminRec );
            dispatchOperationEvent(evt);
        }
    }
    
    public void addOperationListener ( OperationListener oli ) {
        if ( operListeners == null ) {
            operListeners = new ArrayList<OperationListener>();
        }
        synchronized ( operListeners ) {
            if ( !operListeners.contains(oli) ) {
                operListeners.add(oli);
            }
        }
    }

    public void removeOperationListener ( OperationListener oli ) {
        if ( operListeners != null ) 
        synchronized ( operListeners ) {
            operListeners.add(oli);
        }
    }
    
//  **************  INTERNAL CLASSES  ***************    
    
    public interface OperationListener {

        /** Called to indicate that a mirror save operation has started. 
         * Details about files can be obtained from the event object.
         *    
         * @param evt <code>OperationEvent</code>
         */
        public void saveStarted( OperationEvent evt );
        
        /** Called when a <code>MirrorableFile</code> is added or removed to/from
         * the manager. The file can be obtained from the event object.
         *  
         * @param evt <code>OperationEvent</code>
         */
        public void fileListChanged(OperationEvent evt);

        /** Called to indicate that a mirror save operation has just terminated successfully. 
         * Details about files can be obtained from the event object.
         *    
         * @param evt <code>OperationEvent</code>
         */
        public void saveTerminated( OperationEvent evt );
        
        
        /** Called to indicate that an IO-error has occurred in background operations. 
         * Details about the error and involved file can be obtained 
         * from the event object.
         *    
         * @param evt <code>OperationEvent</code>
         */
        public void errorOccurred ( OperationEvent evt );
    }
    
    public interface MirrorableFile {

        /** Returns a string that identifies the mirrorable source file
         * in the application system in a way that is stable over 
         * different sessions of the user program. In the regular case 
         * the absolute file path of the source file is sufficient.
         * If different media channels are used, a likewise stable identifier
         * of the channel can be added to the identifier.  
         * 
         * @return <code>String</code> unfailable file identifier 
         */
        public String getFilePath ();
        
        /** Returns the number associated with this file which marks
         * its latest modified state. All modification steps performed
         * on the mirrorable file should result in increment of this number.
         *  
         * @return int modify number
         */
        public int getModifyNumber ();
        
        /** Returns a <code>MirrorableFile</code> object which represents
         * a data clone of the source file. If this method returns not <b>null</b>
         * the mirror manager will perform IO activity in relation to this mirror file
         * through calling the clone instead of the original. (This is handy if the
         * clone is created much quicker than the mirror file can be written, and if
         * enough space can be assumed available in VM. IO to the mirror file is
         * always done in a background thread, but working on the clone will not block
         * other work on the original by synchronisation blocks.)
         * <p>Note: Only method <code>mirrorWrite()</code> will ever
         * be called from the manager on the clone.
         * @see mirrorWrite()
         *  
         * @return <code>MirrorableFile</code> "snapshot" clone or <b>null</b>
         */
        public MirrorableFile getMirrorableClone ();
        
        /** Performs output of a persistent state of the mirrorable file to
         * the mirror. The implementing class must ensure that during performance 
         * of this routine no other thread can modify the relevant data 
         * content of this file.  
         *      
         * @param out <code>OutputStream</code>
         */
        public void mirrorWrite ( OutputStream out ) throws IOException;

        /**
         * This method is called by the manager in response to <code>addMirrorable()</code>
         * indicating that a mirror file for the added source file has been found.
         * The mirror file is indicated with the parameter.
         * 
         * @param mirror <code>File</code> mirror file of this source file
         */
        public void mirrorDetected ( File mirror );
    }
    
    public static class OperationEvent extends EventObject {
        private static final long serialVersionUID = 5386975459896213823L;
        public static final int SAVESTART_EVENT = 1;
        public static final int SAVEREADY_EVENT = 2;
        public static final int ERROR_EVENT = 3;
        public static final int LISTCHANGE_EVENT = 4;
        
        MirrorFileAdminRecord adminRec;
        Throwable exception;
        String message;
        int eventType;
        
        /** Creates a new operation event for the types SAVESTART and SAVEREADY.
         * 
         * @param source
         * @param type
         * @param rec
         * @throws IllegalArgumentException if source is null or type undefined
         */
        private OperationEvent(Object source, int type, MirrorFileAdminRecord rec) {
            super(source);
            this.adminRec = rec;
            this.eventType = type;
            if ( type < 1 | type > 4 ) {
                throw new IllegalArgumentException("event type unknown: " + type);
            }
        }

        /** Creates a new error event. 
         * 
         * @param source
         * @param e 
         * @param hstr
         * @param adminRec
         * @throws IllegalArgumentException if source is null
         */
        public OperationEvent(Object source, Throwable e, String hstr, MirrorFileAdminRecord adminRec) {
            this(source, OperationEvent.ERROR_EVENT, adminRec );
            this.exception = e;
            this.message = hstr;
        }

        /** The event type as defined by the constants of this class.
         * @return int event type
         */
        public int getType() {
            return eventType;
        }

        public MirrorableFile getMirrorableFile () {
            return adminRec == null ? null : adminRec.file;
        }
        
        public File getMirrorFile () {
            return adminRec == null ? null : adminRec.mirrorFile;
        }

        public Throwable getException() {
            return exception;
        }

        public String getMessage() {
            return message;
        }

        public int getEventType() {
            return eventType;
        }
    }
    
    private class CheckThread extends Thread {

        private boolean terminate;
        private boolean pausing;
        
        public CheckThread( int priority ) {
            super( CHECKTHREAD_NAME );
            setDaemon(true);
            setPriority( priority );
        }

        @Override
        public void run() {
            System.out.println( "# MirrorFileManager started" );
            
            while ( !terminate ) {
                // sleep for the designed time period ("checkPeriod" seconds)
                try {
                    Thread.sleep(checkPeriod*1000);
                } catch (InterruptedException e) {
                }
                
                // into wait-state when PAUSE is set
                if ( pausing ) synchronized (this) {
                    try { wait();
                    } catch (InterruptedException e) {
                    }
                }
                
                // investigate all registered mirrorable files
                // and start mirror save-threads as required by files' current modify-number
                if ( !(pausing | terminate) )
                {
                    @SuppressWarnings("unchecked")
                    Vector<MirrorableFile> fileList = (Vector<MirrorableFile>)mfList.clone();
                    for ( MirrorableFile f : fileList ) {
                        MirrorFileAdminRecord admin = getFileAdminRec(f);
                        
                        // check if not a another save thread is still running
                        // in this case break this operation
                        if ( admin.saveThread != null && admin.saveThread.isAlive() ) {
                            continue;
                        } else {
                            admin.saveThread = null;
                        }

                        // create and start a file-save thread if modified marker is set
                        // for this file
                        if ( f.getModifyNumber() != admin.fileSaveNumber ) {
                            admin.saveThread = new FileSaveThread( f, admin );
                            admin.saveThread.start();
                        }
                    }
                }
            }
            System.out.println( "# MirrorFileManager terminated" );
        }

        /** Causes this thread to pause execution until a call to <code>
         * endPause</code> occurs.
         */
        public void pause () {
            pausing = true;
        }
        
        /** Causes this thread to continue execution if it is in
         * PAUSING state.
         */
        public synchronized void endPause () {
            pausing = false;
            notify();
        }
        
        /** Causes this thread to terminally stop execution. */
        public void terminate () {
            terminate = true;
            pausing = false;
            interrupt();
        }
        
        /** If this thread is sleeping, this will attempt to immediately awake it.
         * Does nothing if this thread is PAUSED.
         */
        public void kick () {
            if ( !pausing) {
                interrupt();
                interrupted();
            }
        }
    }
    
    private class FileSaveThread extends Thread {

        private MirrorableFile file;
        private int modifyNumber;
        private MirrorFileAdminRecord adminRec;

        public FileSaveThread( MirrorableFile f, MirrorFileAdminRecord admin ) {
            super( SAVETHREAD_NAME );
            file = f;
            modifyNumber = f.getModifyNumber();
            adminRec = admin;
            setDaemon(false);
        }

        @Override
        public void run() {
            fireSaveStarted( adminRec );
            
            // attempt working on file clone
            MirrorableFile opFile = file.getMirrorableClone();
            opFile = opFile == null ? file : opFile;

            // determine name of the mirror file to be created
            // and ensure existence of the target directory
            File targetFile = adminRec.getMirrorFileDef();
            Util.ensureFilePath(targetFile, null);

            // let application write persistent file data
            OutputStream fileOut;
            try {
                fileOut = new BufferedOutputStream( new FileOutputStream( targetFile ) );
                opFile.mirrorWrite(fileOut);
                fileOut.close();
            } catch (FileNotFoundException e) {
                fireErrorEvent(adminRec, "cannot create mirror file", e);
            } catch (IOException e1) {
                fireErrorEvent(adminRec, "cannot write mirror file", e1);
            }

            // update save-state number
            adminRec.fileSaveNumber = modifyNumber;
            fireSavePerformed( adminRec );
            
            // erase the mirror if delete-marked in admin record
            if ( adminRec.deleteMarked ) {
                boolean ok = targetFile.delete();
                adminRec.deleteMarked = false;
                if ( !ok ) {
                    fireErrorEvent(adminRec, "cannot delete mirror file!", null);
                }
            }
        }
    }

    
    protected class MirrorFileAdminRecord implements FileFilter {
        // always value
        MirrorableFile file;
        String fileID;
        int fileSaveNumber;
        boolean deleteMarked;
        
        // may be null
        File mirrorFile;  // not null if current mirror file is assumed to exist
        FileSaveThread saveThread;  // not null if a mirror save has taken or is taking place
        
        public MirrorFileAdminRecord (MirrorableFile f) {
            if ( f == null ) {
                throw new NullPointerException();
            }
            file = f;
            fileSaveNumber = f.getModifyNumber();

            // create mirrorable identifier string
            String path = f.getFilePath();
            byte[] idcode;
            if ( path == null || path.isEmpty() ) {
                // create a random name if no path is supplied 
                idcode = Util.randomBytes(8);
            } else {
                // otherwise create a fingerprint name of the file path 
                idcode = Util.fingerPrint(path);
                idcode = Util.arraycopy(idcode, 8);
            }
            fileID = Util.bytesToHex( idcode );
            
            // note mirror file if exists in defined location
            File mir = getMirrorFileDef();
            mirrorFile = mir.isFile() ? mir : null;
        }
        
        /** Returns <b>true</b> if and only if there currently is a 
         * file-save thread active for the mirror file of this record.
         * 
         * @return boolean 
         */
        public boolean threadActive () {
            return saveThread != null && saveThread.isAlive();
        }

        /** Returns the unique file definition for the current mirror file
         * (absolute path). 
         * 
         * @param File mirror file definition
         */
        public File getMirrorFileDef ()
        {
           String path = mirrorFilePrefix + fileID + mirrorFileSuffix;
           return new File( getMirrorRootDirectory(), path );
        }
        
        /** Returns the file definition for the directory 
         * within the mirror root directory that may hold history mirror 
         * files of the mirrorable file.
         * (Rendered file does not imply this directory exists!)
         * 
         * @return <code>File</code> directory (absolute path) 
         */
        public File getHistoryDir () {
            return new File( getMirrorRootDirectory(), fileID );
        }

        @Override
        /**
         * We filter mirror files of this manager as identified by
         * prefix and suffix. (This has nothing to do with specific
         * record values but resides in this structure for convenience.)
         *  
         * @param pathname File
         * @return boolean
         */
        public boolean accept( File pathname ) {
            String filename = pathname.getName();
            return pathname.isFile() && filename.endsWith(mirrorFileSuffix) 
                   && filename.startsWith(mirrorFilePrefix);
        }

    }


}
