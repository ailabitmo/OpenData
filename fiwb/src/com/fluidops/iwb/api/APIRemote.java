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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import com.fluidops.api.dynamic.MetaData;
import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.iwb.api.solution.ExternalSolutionService;
import com.fluidops.iwb.api.wiki.ReaderFlag;
import com.fluidops.iwb.cms.Collector;
import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.network.RMIBase;

public class APIRemote extends RMIBase implements API, RMIRemote
{
	public void reloadConfig() throws Exception
	{
		delegate.reloadConfig();
	}


	public WidgetSelector getWidgetSelector() throws RemoteException
	{
		return delegate.getWidgetSelector();
	}


	public Layouter getLayouter() throws RemoteException
	{
		return delegate.getLayouter();
	}


	public ExternalNamespaceService getExternalNamespaceService() throws RemoteException
	{
		return delegate.getExternalNamespaceService();
	}


	public WidgetService getWidgetService() throws RemoteException
	{
		return delegate.getWidgetService();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1219062131035050783L;

	public APIRemote() throws RemoteException
	{
		super();
	}
	
	
	API delegate;
	
	public Object getConfigOption(String key) throws Exception
	{
		return delegate.getConfigOption(key);
	}


	public List<List<String>> getConfigOptions() throws Exception
	{
		return delegate.getConfigOptions();
	}


	public void setConfigOption(String key, String value) throws Exception
	{
		delegate.setConfigOption(key, value);
	}

    public String setContextsOfSourceEditable(String source, Boolean editable)
    throws RemoteException, Exception
    {
        return delegate.setContextsOfSourceEditable(source,editable);
    }

    public String setContextsOfGroupEditable(String group, Boolean editable)
    throws RemoteException, Exception
    {
        return delegate.setContextsOfGroupEditable(group,editable);
    }
    
	public ProviderService getProviderService() throws RemoteException
	{
		return delegate.getProviderService();
	}

	public UserManager getUserManager() throws RemoteException
	{
		return delegate.getUserManager();
	}
	@Override
	public void init(RMISessionContext sessionContext, EndpointDescription bootstrap)
	{
		delegate = (API)RMIUtils.getDelegate(sessionContext, bootstrap, bootstrap.getServerApi(), API.class);
	}


    public void deleteFromSource(String source) throws RemoteException
    {
        delegate.deleteFromSource(source);
    }
	
    public void deleteFromGroup(String group) throws RemoteException
    {
        delegate.deleteFromGroup(group);
    }
    
    public String cleanupMetaGarbage() throws RemoteException
    {
        return delegate.cleanupMetaGarbage();
    }

    
	public void importMediaWiki(String filename, ReaderFlag option, String namespace, Integer queuesize, Integer flags)
			throws RemoteException, Exception
	{
		delegate.importMediaWiki(filename, option, namespace, queuesize, flags);
	}


	public String load(String filename, String format, String context, String source, String group, Boolean userEditable)
			throws RemoteException, Exception
	{
		return delegate.load(filename, format, context, source, group, userEditable);
	}
	
	public List<Statement> sparqlConstruct(String query)
			throws RemoteException, Exception
	{
		return delegate.sparqlConstruct(query);
	}

	public Table sparqlSelect(String query) throws RemoteException, Exception
	{
		return delegate.sparqlSelect(query);
	}

	public void sparqlUpdate(String query) throws RemoteException, Exception
	{
		delegate.sparqlUpdate(query);
	}
	
	public void updateKeywordIndex() throws RemoteException, Exception
	{
		delegate.updateKeywordIndex();
	}
	
	public void updateWikiIndex() throws RemoteException, Exception
	{
		delegate.updateWikiIndex();
	}

	public void updateOwlimIndices(String repositoryName,
			String repositoryServer) throws RemoteException, Exception
	{
		delegate.updateOwlimIndices(repositoryName, repositoryServer);
	}	

	public String upload(String filename, byte[] attachment)
			throws RemoteException, IOException
	{
	 	return delegate.upload(filename, attachment);
	}

	@Override
	public void uploadUrls(List<String> documentUrls, Collector collector) throws IOException {
		delegate.uploadUrls(documentUrls, collector);
	}
	
	public String version() throws RemoteException
	{
		return delegate.version();
	}


    @Override
    public Object[] rename(String olduri, String newuri, Boolean whatif)
            throws RemoteException
    {
        return delegate.rename(olduri, newuri, whatif);
    }

    
	@Override
	public void createGraphindex() 
	throws RemoteException, Exception 
	{
		delegate.createGraphindex();
	}

	
	@Override
    public URI getUserURI() throws Exception
    {
        return delegate.getUserURI();
    }

	@Override
	public String getUserName() throws Exception
	{
		return delegate.getUserName();
	}


    @Override
    public String export(String filename, String format, String context)
            throws Exception
    {
        return delegate.export(filename, format, context);
    }


	@Override
	public void loadDatasetsFromDir(String dir,  String format, String context, String source, String group, Boolean userEditable) throws Exception 
	{
		delegate.loadDatasetsFromDir(dir, format, context, source, group, userEditable);
	}


	@Override
	public void extractSemanticLinks() throws Exception 
	{
		delegate.extractSemanticLinks();
	}


	@Override
	public void invalidateAllCaches() throws Exception 
	{
		delegate.invalidateAllCaches();
	}


    @Override
    public BackupService getBackupService() throws RemoteException
    {
        return delegate.getBackupService();
    }

    @Override
	public void restartService() throws Exception
	{
		delegate.restartService();
	}

    @Override
	public void stopService(Integer code) throws Exception
	{
		delegate.stopService(code);
	}

	@Override
    public CommunicationService getCommunicationService(String group, String ontologyContext) throws Exception
    {
        return delegate.getCommunicationService(group, ontologyContext);
    }
	
	@Override
	public Collection<CommunicationService> getAllCommunicationServices() throws Exception
	{
		return delegate.getAllCommunicationServices();
	}
	
	@Override
	public MonitoringService getMonitoringService() throws RemoteException 
	{
		return delegate.getMonitoringService();
	}

	@Override
	public String add(Statement statement) throws RemoteException {
		return delegate.add(statement);
	}

	@Override
	public Set<String> delete(Statement statement) throws RemoteException {
		return delegate.delete(statement);
	}
	
	@Override
	public ExternalSolutionService getSolutionService() throws RemoteException
	{
		return delegate.getSolutionService();
	}

	@Override
	public Object invoke(String serviceName, String method, Object[] params) throws Exception {
		return delegate.invoke(serviceName, method, params);
	}

	@Override
	public MetaData getMetaData() throws Exception {
		return delegate.getMetaData();
	}
}
