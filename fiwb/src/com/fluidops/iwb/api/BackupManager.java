/*
 * Copyright (C) 2008-2012, fluid Operations AG
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.fluidops.iwb.api;


import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.rio.RDFFormat;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.iwb.wiki.WikiFileStorage;
import com.fluidops.iwb.wiki.WikiStorage;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.GenUtil;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;
import com.fluidops.util.ZipUtil;

/**
 * DB backup management functionality.
 * 
 * @author msc
 */
public class BackupManager
{   
    private static final Logger logger = Logger.getLogger(BackupManager.class.getName());
    
    private static final String BACKUP_DATEPATTERN = "yyyy-MM-dd'T'HH-mm-ss";
    
    private static DateFormat df() {
    	return new SimpleDateFormat(BACKUP_DATEPATTERN);
    }

    
    /**
     * The backup type specified the directory from whic
     * or to which the backup manager is backuping/restoring.
     * If you extend this datastructure, take care to choose
     * a /unique/ name not containing "-" for the enum field.
     * 
     * @author msc
     */
    static public enum BackupType
    {
        GLOBAL_REPOSITORY("dbmodel"),
        HISTORY_REPOSITORY("historymodel"),
        DIAGNOSTIC_FEEDBACK("diagnosticfeedback"); // do not change string
        
        private String name;
        
        private BackupType(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
    };
    
    /**
     * Computes and stores the backup for the specified DB. 
     *This may either be serialized DB file or a zipped
     * diagnostic feedback file. Returns the backup file 
     * (which will be located in the backup directory).
     * If backupType does not refer to a DB backup (but e.g.
     * the DiagnosticFeedback type) an exception is thrown.
     * 
     * @param type type of the backup
     */
    public static String backupDB(BackupType type)
    throws Exception
    {        
        if (!isDBBackup(type))
            throw new Exception("Type is not a valid DB backup type.");
        
        Repository destRepository = backupTypeToRepository(type);
        if (destRepository==null)
            throw new Exception("Destination repository is invalid (null).");
        
        // some sanity checks...
        File file = createFileWithPath(new Date(),type);
        if (file.exists())
            throw new Exception("Backup file " + file.getPath() + " already exists.");

        // for DB backups we just serialize the DB to file
        ReadDataManager dm = ReadWriteDataManagerImpl.getDataManager(destRepository);
        dm.serializeToFile(file,RDFFormat.TRIG);
        
        return file.getPath();
    }
    
    /**
     * Stores a serialized version of all backupable DBs
     * (including context information). Returns the list
     * of created backup files as return value.
     * 
     * @return
     * @throws Exception in case something serious goes wrong
     */
    public static List<String> backupDBs()
    throws Exception
    {
        List<String> backupedFiles = new ArrayList<String>();
        for (BackupType backupType : BackupType.values())
        {
            // ignore non-DB backup types (like diagnostic feedback)
            if (!isDBBackup(backupType))
                continue; 
            
            try
            {
                String backuped = backupDB(backupType);
                if (backuped!=null)
                    backupedFiles.add(backuped);
                
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(),e);
            }
        }
        return backupedFiles;   
    }
    
    /**
     * Restores a backup to the associated directory. The
     * repository content will be completely replaced by the
     * backup content.
     * 
     * @param rep
     * @throws Exception describing error in case restore fails
     */
    public static boolean restoreDBBackup(String backupFile)
    throws Exception
    {
        // we parse the file name to verify file name integrity
        Pair<Date,BackupType> parsedFile = parseFilename(backupFile);

        // some more sanity checks
        File restoreFile = new File(IWBFileUtil.getBackupFolder(), backupFile);
        if (!(restoreFile.exists() && restoreFile.isFile() && restoreFile.canRead()))
            return false;

        // get target repository; if the type target repository was
        // not set, something went wrong and we abort
        Repository targetRepository=backupTypeToRepository(parsedFile.snd);
        if (targetRepository==null)
            return false;
        
        // perform restore task (transactional)
        ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(targetRepository);
        dm.replaceRepositoryContent(restoreFile, RDFFormat.TRIG);
        dm.close();
        
        return true;
    }
    
    /**
     * Restores the latest backup of the given type.
     * 
     * @return name of the restored backup file or null if no backup available
     * @throws exception in case something goes wrong
     */
    public static String restoreLatestDBBackup(BackupType bt)
    throws Exception 
    {
        SortedSet<String> backups = getAvailableBackups();
        
        // collect backups to be deleted
        
        Pair<Date,BackupType> latest = null;
        for (String backup : backups)
        {
            try
            {
                Pair<Date,BackupType> backupInfo = parseFilename(backup);
                if (backupInfo.snd.toString().equals(bt.toString()))
                {
                    if (latest==null)
                        latest = backupInfo;
                    else
                    {
                        long latestTS = latest.fst.getTime();
                        long curTS = backupInfo.fst.getTime();
                        if (curTS>latestTS)
                            latest = backupInfo;
                    }
                }
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(),e);
            }
        }
        
        if (latest==null)
            return null;

        String restoreFile = createFilename(latest.fst,latest.snd);
        restoreDBBackup(restoreFile);
        return restoreFile;
    }

    /**
     * Restores the latest backup of the given type.
     * 
     * @return name of the restored backup file or null if no backup available
     * @throws exception in case something goes wrong
     */
    public static List<String> restoreLatestDBBackups()
    throws Exception
    {
        List<String> restoredFiles = new ArrayList<String>();
        for (BackupType backupType : BackupType.values())
        {
            if (!isDBBackup(backupType))
                continue;
            
            try
            {
                String restored = restoreLatestDBBackup(backupType);
                if (restored!=null)
                    restoredFiles.add(restored);
                
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(),e);
            }
        }
        return restoredFiles;
    }
    
    /**
     * Retrieves a lexigrophically sorted set of available
     * backups. As we use the naming scheme <TIMESTAMP>-<DBTYPE>
     * and a lexicographically sortable timestamp, this list
     * reflects the order of backups.
     */
    public static SortedSet<String> getAvailableBackups()
    {
        File backupDir = IWBFileUtil.getBackupFolder();        
        SortedSet<String> backups = new TreeSet<String>();
        
        // first make sure the backup directory exists (we
        // create it in IwbStart, so it should always be there)
        if (!backupDir.isDirectory())
        {
            logger.error("Backup directory does not exist or is not available");
            return backups;
        }
        
        String[] files = backupDir.list();
        for (String file : files)
        {
        	if (file.endsWith(".trig") || file.endsWith(".zip"))
        		backups.add(file);
        }
        return backups;
    }
    
    /**
     * Remove the specified backup from the list of backups.
     * 
     * @param backupFile
     * @return success
     */
    public static void deleteBackup(String backupFile)
    throws Exception
    {
        parseFilename(backupFile); // verify filename
        
        File f = new File(IWBFileUtil.getBackupFolder(), backupFile);
        if (!f.exists())
            throw new Exception("Backup file " + backupFile + " does not exist");
        GenUtil.delete(f);
    }
    
    /**
     * Deletes all backups older than nrOfDays days. If nrOfDays
     * is <=0, all backups will be removed. Returns the total number
     * of backups that have been deleted.

     * @param nrOfDays
     * @return number of deleted backups
     */
    public static int deleteBackupsOlderThan(int nrOfDays)
    {
        SortedSet<String> backups = getAvailableBackups();
        
        // collect backups to be deleted
        List<String> toDelete = new ArrayList<String>();
        for (String backup : backups)
        {
            try
            {
                Pair<Date,BackupType> backupInfo = parseFilename(backup);
                
                long backupMs = backupInfo.fst.getTime();
                long nowMs = System.currentTimeMillis();
                long nrOfDaysMs = (long)nrOfDays*24*60*60*1000;
                
                if (nowMs-nrOfDaysMs>backupMs)
                    toDelete.add(backup);
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(),e);
            }
        }
        
        // delete those...
        int deletedCtr = 0;
        for (String backup : toDelete)
        {
            try
            {
                deleteBackup(backup);
                deletedCtr++;
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(),e);
            }
        }
        
        return deletedCtr;
            
    }
    
    /**
     * Resolves a backup type string to an enum element.
     */
    public static BackupType resolveBackupType(String backupTypeStr)
    {
        for (BackupType backupType : BackupType.values())
            if (backupType.toString().equals(backupTypeStr))
                return backupType;
        
        return null; // not resolvable
    }

    /**
     * Checks if the backup type is a datatbase backup (currently, 
     * if not it means it is a diagnostic feedback file, but in
     * future there might be other backup types that are not DB backups).
     * 
     * @param t
     * @return
     */
    public static boolean isDBBackup(BackupType t)
    {
        return t==BackupType.GLOBAL_REPOSITORY 
            || t==BackupType.HISTORY_REPOSITORY;
    }

    
    /**
     * Retrieves the repository associated with a given backup type.
     * Returns null in case of an uncovered repository type (should
     * never happen, though, if everything is properly implemented).
     */
    private static Repository backupTypeToRepository(BackupType type)
    {
        Repository repository = null;
        switch (type)
        {
        case GLOBAL_REPOSITORY:
            repository = Global.repository;
            break;
        case HISTORY_REPOSITORY:
            repository = Global.historyRepository;
            break;
        default: break;
        }
        
        return repository;
    }
    
    
    /**
     * Parse the file, throws an exception if file naming scheme is invalid.
     * If no exception occurs, the metod returns the date and the backup
     * type represented by the filename (and both are definitely set).
     * 
     * @param file
     * @return
     * @throws Exception
     */
    public static Pair<Date,BackupType> parseFilename(String file)
    throws Exception
    {   
        // assert file naming scheme is valid
        int suffixDelim = file.lastIndexOf(".");
        if (suffixDelim==-1)
            throw new Exception("Invalid file type (no suffix specified): " + file);
        
        String prefix = file.substring(0,suffixDelim);
        String suffix = file.substring(suffixDelim,file.length());
        if (!(suffix.equals(".trig") || suffix.equals(".zip")))
            throw new Exception("Backup manager supports only internally generated " +
            		"*.trig files (for DB backup) and *.zip files (for general backup)");
        
        // the backup file name must follow the naming scheme "<TIMESTAMP>-<DBTYPE>"
        String[] spl = prefix.split("_");
        if (spl==null || spl.length!=2 || spl[0].isEmpty() || spl[1].isEmpty())
            throw new Exception("Illegal backup file format: " + file);
        
        // the first component must be a valid timestamp according to our
        // internal date representation format
        Date d = df().parse(spl[0]);
        if (d==null)
            throw new Exception("Illegal date in backup file: " + spl[0]);

        // the second component must be a valid backup type
        BackupType bt = resolveBackupType(spl[1]);
        if (bt==null)
            throw new Exception("Illegal backup type in backup file: " + spl[1]);
            
        return new Pair<Date,BackupType>(d,bt);
    }
    
    /**
     * Creates filename (without path) for the given
     * date and backup type.
     * 
     * @param d
     * @param bt
     * @return
     * @throws Exception
     */
    private static String createFilename(Date d, BackupType bt)
    throws Exception
    {
        String curDateStr = df().format(d);
        String typeStr = bt.toString();
        
        if (StringUtil.isNullOrEmpty(curDateStr))
            throw new Exception("Could not compute current timestamp.");
        if (StringUtil.isNullOrEmpty(typeStr))
            throw new Exception("Invalid type for backup.");
        
        String suffix = 
            bt==BackupType.DIAGNOSTIC_FEEDBACK ? ".zip" : ".trig";
        
        String fileName = curDateStr + "_" + typeStr + suffix;
        return fileName;
    }

  
    /**
     * Return the path of the selected backup as File relative to
     * backup directory.
     * 
     * @param d
     * @param bt
     * @return
     * @throws Exception
     */
    private static File createFileWithPath(Date d, BackupType bt)  throws Exception
    {
        return new File(IWBFileUtil.getBackupFolder(), createFilename(d,bt));
    }
    
    /**
     * Creates diagnostic feedback zip file using best effort approach.
     * The boolean parameters can be used to include/exclude individual
     * components from the diagnostic feedback. The method returns the
     * a pair of the zip-file and a flag being set to false if one or
     * more specified subtasks did not succeed, true otherwise.
     */
    public static Pair<String,Boolean> createDiagnosticFeedback(boolean includeGlobalDB, 
            boolean includeHistoryDB, boolean includeWiki, boolean ignore,
            boolean includeConfigPropDB, boolean includeConfigFolder, boolean includeUploadFolder)
    {
    
        List<File> filesAndFolders = new ArrayList<File>();
        List<File> createdFiles = new ArrayList<File>();
        
        boolean success = true;
        
        // Wiki
        // Global DB
        if (includeGlobalDB)
            success &= includeDB(BackupType.GLOBAL_REPOSITORY,filesAndFolders,createdFiles);

        // history DB
        if (includeHistoryDB)
            success &= includeDB(BackupType.HISTORY_REPOSITORY,filesAndFolders,createdFiles);

        // Wiki
        if (includeWiki)
        {
            WikiStorage ws = Wikimedia.getWikiStorage();
            if (ws instanceof WikiFileStorage)
            {
                File f = IWBFileUtil.getWikiFolder();
                if (f.exists() && f.isDirectory() && f.canRead())
                    filesAndFolders.add(f);
                else 
                    success = false;
            }
            else
            {
                // TODO: we may want to implement support for Wiki DB storage,
                // but for now WikiFileStorage is enough...
                logger.error("Wiki backup only supported for Wiki file storage.");
                success = false;
            }
        }
        
        if (includeConfigPropDB)
        {
            File f = IWBFileUtil.getFileInWorkingDir("config.prop");
            if (f.exists() && f.canRead())
                filesAndFolders.add(f);
            else 
                success = false;
        }
        
        if (includeConfigFolder)
        {
            File f = IWBFileUtil.getConfigFolder();
            if (f.exists() && f.isDirectory() && f.canRead())
                filesAndFolders.add(f);
            else 
                success = false;
        }
        

        if (includeUploadFolder)
        {
            File f = IWBFileUtil.getUploadFolder();
            if (f.exists() && f.isDirectory() && f.canRead())
                filesAndFolders.add(f);
            else 
                success = false;
        }
        
        
        try
        {
            if (!filesAndFolders.isEmpty()) // Only create a diagnostic zip when there is something to realy back up
            {
	            
	            File zipFile = createFileWithPath(new Date(),BackupType.DIAGNOSTIC_FEEDBACK);
	            
	            // zip files and delete temp files
	            ZipUtil.doZipOutput(zipFile, IWBFileUtil.getIwbWorkingDir(), filesAndFolders.toArray(new File[0]));
	            for (File f : createdFiles)
	                GenUtil.delete(f);
	            
	            return new Pair<String,Boolean>(zipFile.getPath(),success);
            }
            else
            {
				logger.warn("No files backuped: " + includeGlobalDB + "/"
						+ includeHistoryDB + "/" + includeWiki + "/"
						+ includeConfigPropDB + "/"
						+ includeConfigFolder + "/" + includeUploadFolder);
				logger.warn("success=" + success);
            	
            	return new Pair<String,Boolean>(null,false);
            }
        }
        catch (Exception e)
        {
            logger.warn(e.getMessage(),e);
            return new Pair<String,Boolean>(null,false);
        }
    }        

    /**
     * Help function for backuping and including a DB to the
     * file lists.
     * 
     * @param type
     * @param filesAndFolders
     * @param createdFiles
     * @return
     */
    private static boolean includeDB(BackupType type,
            List<File> filesAndFolders, List<File> createdFiles)
    {
        try
        {
            String file = BackupManager.backupDB(type);
            if (StringUtil.isNullOrEmpty(file)) 
                return false;
            else 
            {
                File f = new File(file);
                if (f.exists())
                {
                    filesAndFolders.add(f);
                    createdFiles.add(f);
                    return true;
                }
                else
                    return false;
            }
        }
        catch (Exception e)
        {
            logger.warn(type.toString() + " not included in diagnostic zip: " + e.getMessage());
            return false;
        }
    }
}
