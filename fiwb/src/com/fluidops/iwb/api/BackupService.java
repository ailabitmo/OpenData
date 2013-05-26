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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;

/**
 * Backup services for IWB databases 
 * 
 * @author msc
 */
@Doc("Backup service")
public interface BackupService extends Remote
{
    @Doc("Creates a backup for the given backup type")
    public String backupDB(
            @Par(name="backupType", type="type identifiying the backup", desc="use listDBBackupTypes() for available types", isRequired=true) String backupType
    ) throws RemoteException, Exception;
    
    @Doc("Creates a backup of all DBs that are supported by the backup service")
    public String backupDBs()
    throws RemoteException, Exception;
    
    @Doc("Restores an existing backup of the specified DB. Deletes all previous data.")    
    public String restoreDBBackup(
            @Par(name="backupFile", type="file", desc="name of backup file", isRequired=true) String backupFile
    ) throws RemoteException, Exception;
    
    @Doc("Restores the latest backups of the specified DB.")    
    public String restoreLatestDBBackup(
            @Par(name="backupType", type="database type identifier", desc="use listDbTypes() for available types", isRequired=true) String backupType
    ) throws RemoteException, Exception;

    @Doc("Restores the latest backups of all DBs (for all DBs where it is available).")    
    public String restoreLatestDBBackups()
    throws RemoteException, Exception;
        
    @Doc("Display available backups (including diagnostic feedback)")
    public Collection<String> listAvailableBackups()
    throws RemoteException;

    @Doc("List available backups")
    public Collection<String> listDBBackupTypes()
    throws RemoteException;
    
    @Doc("Deletes the specified backup file")
    public String deleteBackup(
            @Par(name="backupFile", type="file", desc="name of backup file", isRequired=true) String backupFile
    ) throws RemoteException;
    
    @Doc("Deletes all backup files")
    public String deleteAllBackups()    throws RemoteException;

    @Doc("Deletes all backup files older than the specified number of days")
    public String deleteBackupsOlderThan(
        @Par(name="nrOfDays", type="integer greater than zero", desc="number of days", isRequired=true) Integer nrOfDays
    ) throws RemoteException;
 
    @Doc("Creates diagnostic feedback .zip file")
    public String createDiagnosticFeedback(
        @Par(name="includeGlobalDB", type="boolean", desc="include global DB (default: true)", isRequired=false) Boolean includeGlobalDB,
        @Par(name="includeHistoryDB", type="boolean", desc="include history DB (default: true)", isRequired=false) Boolean includeHistoryDB,
        @Par(name="includeWiki", type="boolean", desc="include history DB (default: true)", isRequired=false) Boolean includeWiki,
        @Par(name="includeOntologies", type="boolean", desc="this parameter is currently ignored", isRequired=false) Boolean includeOntologies,
        @Par(name="includeConfigProp", type="boolean", desc="include top-level config.prop (default: true)", isRequired=false) Boolean includeConfigProp,
        @Par(name="includeConfigFolder", type="boolean", desc="include config subfolder (default: true)", isRequired=false) Boolean includeConfigFolder,
        @Par(name="includeUploads", type="boolean", desc="include WEBDAV upload folder (default: false)", isRequired=false) Boolean includeUploads
    ) throws RemoteException;
}