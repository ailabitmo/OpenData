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

import java.rmi.RemoteException;
import java.util.Collection;

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.network.RMIBase;

/**
 * Remote access to backup services.
 * 
 * @author msc
 */
public class BackupServiceRemote extends RMIBase implements BackupService, RMIRemote
{
    /**
     * 
     */
    private static final long serialVersionUID = -3311138402022229654L;
    
    /**
     * The provider service this class delegates to
     */
    BackupService delegate;

    /**
     * Constructor 
     * 
     * @throws RemoteException
     */
    protected BackupServiceRemote() throws RemoteException
    {
        super();
    }
    
    @Override
    public void init(RMISessionContext sessionContext,
            EndpointDescription bootstrap) throws Exception
    {
        delegate = (BackupService) RMIUtils.getDelegate(sessionContext,
                bootstrap, ((API) bootstrap.getServerApi())
                        .getBackupService(), BackupService.class);
    }

    
    @Override
    public String backupDB(String dbType)
    throws RemoteException, Exception
    {
        return delegate.backupDB(dbType);
    }

    @Override
    public String backupDBs()
    throws RemoteException, Exception
    {
        return delegate.backupDBs();
    }
    
    @Override
    public String restoreDBBackup(String backupFile)
    throws RemoteException, Exception
    {
        return delegate.restoreDBBackup(backupFile);
    }
    
    @Override
    public String restoreLatestDBBackups()
    throws RemoteException, Exception
    {
        return delegate.restoreLatestDBBackups();
    }
    
    @Override
    public String restoreLatestDBBackup(String dbType)
    throws RemoteException, Exception
    {
        return delegate.restoreLatestDBBackup(dbType);
    }
   
    @Override
    public Collection<String> listAvailableBackups()
    throws RemoteException
    {
        return delegate.listAvailableBackups();
    }

    @Override
    public Collection<String> listDBBackupTypes()
    throws RemoteException
    {
        return delegate.listDBBackupTypes();
    }
    
    @Override
    public String deleteBackup(String backupFile)
    throws RemoteException
    {
        return delegate.deleteBackup(backupFile);
    }
    
    @Override
    public String deleteAllBackups()
    throws RemoteException
    {
        return delegate.deleteAllBackups();
    }

    @Override
    public String deleteBackupsOlderThan(Integer nrOfDays) 
    throws RemoteException
    {
        return delegate.deleteBackupsOlderThan(nrOfDays);
    }
    
    @Override
    public String createDiagnosticFeedback(Boolean includeGlobalDB,
            Boolean includeHistoryDB, Boolean includeWiki,
            Boolean includeOntology, Boolean includeConfigProp,
            Boolean includeConfigFolder, Boolean includeWebdavFolder)
            throws RemoteException
    {
        return delegate.createDiagnosticFeedback(includeGlobalDB, 
                includeHistoryDB, includeWiki, includeOntology, 
                includeConfigProp, includeConfigFolder, includeWebdavFolder);
    }
}
