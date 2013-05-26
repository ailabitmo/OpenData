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
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;
import com.fluidops.api.SubApi;
import com.fluidops.api.dynamic.DynamicService;
import com.fluidops.iwb.api.solution.ExternalSolutionService;
import com.fluidops.iwb.api.wiki.ReaderFlag;
import com.fluidops.iwb.cms.Collector;
import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.iwb.user.UserManager;

/**
 * remote API / CLI core interface
 * 
 * @author aeb
 */
@Doc("Semantic Data Management Root API")
public interface API extends DynamicService, Remote
{
    @Doc("Reload the Config from config.prop")
    public void reloadConfig( ) throws Exception;
    
    @Doc("Lists IWB config option names and descriptions")
    public List<List<String>> getConfigOptions() throws Exception;

    @Doc("Set a config option and reload config")
    public void setConfigOption
    (
    		@Par( name="key", type="String", desc="config option name" ) String key,
    		@Par( name="value", type="String", desc="config option value" ) String value
    ) throws Exception;
    
    @Doc("Get a config option")
    public Object getConfigOption
    (
    		@Par( name="key", type="String", desc="config option name" ) String key
    ) throws Exception;
    
    @Doc("Clean up context garbage")
    public String cleanupMetaGarbage() throws RemoteException;
    
    @Doc("Adds a new statement in a new user context to the database")
    public String add(
    		@Par(name="statement", type="Statement", desc="Statement to add", isRequired=true) Statement statement
    ) throws RemoteException;
    
    @Doc("Delete the statement from the database in all editable contexts, return the modified contexts")
    public Set<String> delete(
    		@Par(name="statement", type="Statement", desc="Statement to delete", isRequired=true)Statement statement 
    ) throws RemoteException;
    
    @Doc("Removes all data from a given data provider")
    public void deleteFromSource( 
            @Par(name="source", type="string", desc="URL of the data provider", isRequired=true) String source 
    ) throws RemoteException;
    
    @Doc("Removes all data in a given context group")
    public void deleteFromGroup( 
            @Par(name="group", type="string", desc="URL of the context group", isRequired=true) String group 
    ) throws RemoteException;
    
    @Doc("Query the statement database")
    public Object[] rename( 
            @Par(name="olduri", type="string", desc="the old URI of the resource to be renamed", isRequired=true) String olduri, 
            @Par(name="newuri", type="string", desc="the new URI of the resource to be renamed", isRequired=false) String newuri, 
            @Par(name="whatif", type="boolean", desc="do not actually rename, just check what would be affected", isRequired=false) Boolean whatif 
    ) throws RemoteException;
    
    @Doc("Upload a file into the Wiki")
    public String upload(
            @Par(name="filename", type="string", desc="name of the file to upload. defaults to the last fragment of the path or url", isRequired=true) String filename, 
            @Par(name="attachment", type="byte[]", desc="the data to upload", isRequired=true) byte[] attachment
    ) throws RemoteException, IOException;

    @Doc("Upload a list of 'web-documents'")
    void uploadUrls(
    		@Par(name="documentUrls", 
    			type="list", 
    			desc="URLs of documents to be uploaded. E.g. [\"http://host1/doc1\", \"http://host2/doc2\"]", 
    			isRequired= true) List<String> documentUrls, 
    		@Par(name="collector", 
    			type="object", 
    			desc="The collector that shall be used for extracting semantic data from the documents. E.g. {\"class\":\"com.fluidops.iwb.cms.extract.LuxidCollector\"}", 
    			isRequired= true) Collector collector) 
    				throws IOException;

    @Doc("Update the keyword index for structured data")
    public void updateKeywordIndex() throws RemoteException, Exception;
    
    @Doc("Update the keyword index for Wiki pages")
    public void updateWikiIndex() throws RemoteException, Exception;

    @Doc("Update the OWLIM indices")
    public void updateOwlimIndices(
    		@Par(name="repositoryName", type="String", desc="Name of the repository, e.g. 'repository'. If no repository name is given, Global.repository is used.", isRequired=false) String repositoryName,
    		@Par(name="repositoryServer", type="String", desc="URL of remote repository server. If no repository server is given, Global.%repositoryName% is used.", isRequired=false) String repositoryServer
    ) throws RemoteException, Exception;
    
    @Doc("Load data from an RDF file to the repository")
	String load(
            @Par(name="filename", type="string", desc="file name", isRequired=true) String filename, 
            @Par(name="format", type="string", desc="file format (one of RDFXML, NTRIPLES, TURTLE, N3, TRIX, TRIG)", isRequired=false) String format,
            @Par(name="context", type="string", desc="target context URI (if not specified, it is taken from the data)", isRequired=false) String context,
            @Par(name="source", type="string", desc="source URI", isRequired=false) String source,
            @Par(name="group", type="string", desc="add source to a context group", isRequired=false) String group,
            @Par(name="userEditable", type="boolean", desc="user editable, default is config parameter contextsEditableDefault", isRequired=false) Boolean userEditable
    ) throws RemoteException, Exception;
    
    @Doc("Load multiple RDF files from directory into the repository")
    void loadDatasetsFromDir(
            @Par(name="dir", type="string", desc="directory name", isRequired=true) String dirname,
            @Par(name="format", type="string", desc="file format (one of RDFXML, NTRIPLES, TURTLE, N3, TRIX, TRIG)", isRequired=false) String format,
            @Par(name="context", type="string", desc="target context", isRequired=false) String context,
            @Par(name="source", type="string", desc="source URI", isRequired=false) String source,
            @Par(name="group", type="string", desc="add source to a context group", isRequired=false) String group,
            @Par(name="userEditable", type="boolean", desc="user editable", isRequired=false) Boolean userEditable
    ) 
    throws Exception;

    @Doc("Sets context from given source editable")
    String setContextsOfSourceEditable(
            @Par(name="source", type="string", desc="source URI", isRequired=true) String source,
            @Par(name="editable", type="boolean", desc="editable", isRequired=false) Boolean editable
    ) throws RemoteException, Exception;
    
    @Doc("Sets context from given group editable")
    String setContextsOfGroupEditable(
            @Par(name="group", type="string", desc="group URI", isRequired=true) String group,
            @Par(name="editable", type="boolean", desc="editable", isRequired=false) Boolean editable
    ) throws RemoteException, Exception;
    
    @Doc( "Export repository to RDF file" )
    public String export(
            @Par(name="filename", type="string", desc="file name", isRequired=true) String filename,
            @Par(name="format", type="string", desc="RDF format", isRequired=false) String format,
            @Par(name="context", type="string", desc="context", isRequired=false) String context
    ) throws Exception;

    @Doc("Import from (Semantic) MediaWiki")
	void importMediaWiki(
			@Par(name="filename", type="string", desc="file name", isRequired=true) String filename,
			@Par(name="option", type="ReaderFlag", desc="options (one of UseWikiIndexer, UseWikiDatabase, UseWikiIndexerAndDatabase", isRequired=true) ReaderFlag option,
            @Par(name="namespace", type="string", desc="target namespace", isRequired=false) String namespace,
            @Par(name="queuesize", type="integer", desc="queue size of reader", isRequired=false) Integer queuesize,
            @Par(name="flags", type="integer", desc="flags for the algorithm (OR'ed)", isRequired=false) Integer flags
    ) throws RemoteException, Exception;	
	       
    @Doc("SPARQL select query")
	public Table sparqlSelect(
			@Par(name="query", type="SPARQL select query", desc="e.g. SELECT * WHERE {?s ?p ?o}", isRequired=true) String query
	) throws RemoteException, Exception;

    @Doc("SPARQL construct query")
	public List<Statement> sparqlConstruct(
			@Par(name="query", type="SPARQL construct query", desc="e.g. CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}", isRequired=true) String query
	) throws RemoteException, Exception;
    
    /**
     * Perform a SPARQL 1.1 Update query (e.g. INSERT, DELETE, UPDATE). If the 
     * query contains the special variable $usercontext$, it is replaced
     * with an new user context. In such case also the meta context information
     * are written.
     * 
     * @param query
     */
    @Doc("SPARQL update query")    
	public void sparqlUpdate(
			@Par(name="query", type="SPARQL update query", desc="e.g. INSERT INTO { GRAPH $usercontext$ { s p o } }", isRequired=true) String query
	) throws RemoteException, Exception;
    
    @Doc( "Get the program version" )
    public String version() throws RemoteException;
    
    @SubApi("Configure and run providers")
    public ProviderService getProviderService() throws RemoteException;

    @SubApi("Configure and widgets")
	public WidgetService getWidgetService() throws RemoteException;

    @SubApi("Database backup service")
    public BackupService getBackupService() throws RemoteException;
    
    @Doc("Update the graph-index")
	public void createGraphindex() throws RemoteException, Exception;
    
    @Doc( "Returns the current user name" )
    public String getUserName() throws Exception;

    @Doc( "Returns the current user URI" )
    public URI getUserURI() throws Exception;
    
    @SubApi( "Manages RDF namespace abbreviations" )
    public ExternalNamespaceService getExternalNamespaceService() throws RemoteException;

    @SubApi( "Manages solutions" )
    public ExternalSolutionService getSolutionService() throws RemoteException;
    
    @SubApi( "Manages widget layouts" )
    public Layouter getLayouter() throws RemoteException;

    @SubApi( "User Manager")
    public UserManager getUserManager() throws RemoteException;
    
    @SubApi( "Manages widget selections" )
    public WidgetSelector getWidgetSelector() throws RemoteException;

    @Doc("Extracts all semantic links from wiki and puts them in RDF store, while removing all previous semantic links that were entered through the Wiki. " +
    		"Can be used to solve problems arising due to compatibility versions when the semantic link extraction logics has changed.")
	void extractSemanticLinks() throws Exception;
    
    @Doc("invalidates all IWB caches")
    void invalidateAllCaches() throws Exception;
    
    @Doc("returns the communication service for given parameters")
    public CommunicationService getCommunicationService(
    		@Par(name="group", type="String", desc="the context group the communication service writes to") String group, 
    		@Par(name="ontologyContext", type="String", desc="the ontology context the provider writes to") String ontologyContext) throws Exception;

    @Doc("returns a collection of all communication services that have been requested via API so far")
    public Collection<CommunicationService> getAllCommunicationServices() throws Exception;
    
    @SubApi("returns the monitoring API")
    public MonitoringService getMonitoringService() throws RemoteException;
    
	@Doc( "Restart the Information Workbench service" )
	public void restartService() throws Exception;

	@Doc( "Stop the Information Workbench service" )
	public void stopService(
			@Par( name="code", type="int", desc="exit code", isRequired=false ) Integer code
	) throws Exception;
    
}
