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

import static com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus.INSTALLED_SUCCESSFULLY_RESTART_REQUIRED;
import static com.fluidops.iwb.util.IWBFileUtil.getApplicationFolder;
import static com.fluidops.iwb.util.IWBFileUtil.getConfigFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.tanukisoftware.wrapper.WrapperManager;

import com.fluidops.api.Doc;
import com.fluidops.api.dynamic.DynamicServiceImpl;
import com.fluidops.config.ConfigDoc;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.Version;
import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.api.solution.ChainBasedSolutionService;
import com.fluidops.iwb.api.solution.CompositeSolutionHandler;
import com.fluidops.iwb.api.solution.ConfigPropHandler;
import com.fluidops.iwb.api.solution.CopyFolderStructureHandler;
import com.fluidops.iwb.api.solution.DBBootstrapHandler;
import com.fluidops.iwb.api.solution.DirReferenceBasedSolutionService;
import com.fluidops.iwb.api.solution.ExternalSolutionService;
import com.fluidops.iwb.api.solution.GroovyConfigHandler;
import com.fluidops.iwb.api.solution.ImagesPropHandler;
import com.fluidops.iwb.api.solution.JarHandler;
import com.fluidops.iwb.api.solution.JettyHandler;
import com.fluidops.iwb.api.solution.NamespaceHandler;
import com.fluidops.iwb.api.solution.OntologyUpdateHandler;
import com.fluidops.iwb.api.solution.ProviderClassHandler;
import com.fluidops.iwb.api.solution.ProviderHandler;
import com.fluidops.iwb.api.solution.SolutionService;
import com.fluidops.iwb.api.solution.WidgetClassHandler;
import com.fluidops.iwb.api.solution.WidgetHandler;
import com.fluidops.iwb.api.solution.WikiBootstrapHandler;
import com.fluidops.iwb.api.solution.WikiBotPropHandler;
import com.fluidops.iwb.api.solution.ZipFileBasedSolutionService;
import com.fluidops.iwb.api.wiki.CliRunner;
import com.fluidops.iwb.api.wiki.ReaderFlag;
import com.fluidops.iwb.api.wiki.SemanticLinkExtractor;
import com.fluidops.iwb.api.wiki.WikiIndexer;
import com.fluidops.iwb.api.wiki.XMLWikiReader;
import com.fluidops.iwb.cms.Collector;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.install.PropertyMergerImpl;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.iwb.service.OwlimIndexService;
import com.fluidops.iwb.service.SparqlUpdate;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManagerImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.QueryResultUtil;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.Pair;
import com.fluidops.util.Singleton;
import com.fluidops.util.Singleton.UpdatableSingleton;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * API server implementation
 * 
 * @author aeb
 */
public class APIImpl extends DynamicServiceImpl implements API, InternalAPI
{
	private static final Logger logger = Logger.getLogger(APIImpl.class.getName());
	
	private static final ValueFactoryImpl valueFactory = ValueFactoryImpl.getInstance();
	
    public void reloadConfig() throws Exception
    {
    	com.fluidops.config.Config.reloadConfig();
    }
    
    public List<List<String>> getConfigOptions() throws Exception
    {
    	List<List<String>> res = new ArrayList<List<String>>();
    	for ( Method m : Config.class.getMethods() )
    	{
    		ConfigDoc cfd = m.getAnnotation( ConfigDoc.class );
    		if ( cfd != null )
    		{
    			List<String> item = new ArrayList<String>();
    			item.add( cfd.category().toString() );
    			item.add( cfd.name() );
    			item.add( cfd.desc() );
    			item.add( cfd.type().toString() );
    			res.add( item );
    		}
    	}
    	return res;
    }

    public Object getConfigOption( String key ) throws Exception
    {
    	Object val = null;
    	for ( Method m : Config.class.getMethods() )
    	{
    		ConfigDoc cfd = m.getAnnotation( ConfigDoc.class );
    		if ( cfd != null )
    			if ( cfd.name().equals( key ) )
    				val = m.invoke( Config.getConfig() );
    	}
    	return val;
    }
    
    public void setConfigOption( String key, String value ) throws Exception
    {
    	Method found = null;
    	// fbase Config
    	for ( Method m : Config.class.getMethods() )
    	{
    		ConfigDoc cfd = m.getAnnotation( ConfigDoc.class );
    		if ( cfd != null )
    			if ( cfd.name().equals( key ) )
    				found = m;
    	}
    	
    	// check if prop exists
    	if ( found == null )
    		throw new Exception( "Config property does not exist: " + key );

    	// check type
    	if ( found.getReturnType().equals( boolean.class ) )
    		Boolean.valueOf( value );
    	else if ( found.getReturnType().equals( int.class ) )
    		new Integer( value );
    	else if ( found.getReturnType().equals( long.class ) )
    		new Long( value );
    	else if ( found.getReturnType().equals( byte.class ) )
    		new Byte( value );
    	else if ( found.getReturnType().equals( short.class ) )
    		new Short( value );
    	else if ( found.getReturnType().equals( float.class ) )
    		new Float( value );
    	else if ( found.getReturnType().equals( double.class ) )
    		new Double( value );
    	else if ( found.getReturnType().equals( char.class ) )
    		Character.valueOf( value.charAt(0) );
    	else if ( found.getReturnType().equals( Character.class ) )
    		Character.valueOf(value.charAt(0) );
    	else 
    		found.getReturnType().getConstructor( String.class ).newInstance( value );
    	
    	com.fluidops.config.Config.getConfig().set(key, value);
    }
    
    @Override
    public Table sparqlSelect( String query ) throws RemoteException, Exception    
    {
    	return getDataManager().sparqlSelectAsTable( query );
    }
    
    @Override
    public List<Statement> sparqlConstruct( String query ) throws RemoteException, Exception    
    {
    	return QueryResultUtil.graphQueryResultAsList(getDataManager().sparqlConstruct(query, true));
    }
    
    @Override
	public void sparqlUpdate(String query) throws RemoteException, Exception
	{
		SparqlUpdate.executeUpdate(query);		
	}
           

    @Override
    public void deleteFromSource(String source) throws RemoteException
    {
        ReadWriteDataManager dm = null;

        URI sourceURI = null;
        if (source!=null)
        {
            sourceURI = getNamespaceService().parseURI(source);
            if (sourceURI==null)
                throw new RemoteException("Parsing of source URI failed.");
        }
        dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
        dm.deleteExpiredContextsOfSource(sourceURI,null,null, null);
        dm.close();
    }
    
    @Override
    public void deleteFromGroup(String group) throws RemoteException
    {
        ReadWriteDataManager dm = null;

        URI groupURI = null;
        if (group!=null)
        {
            groupURI = getNamespaceService().parseURI(group);
            if (groupURI==null)
                throw new RemoteException("Parsing of group URI failed.");
        }
        dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
        dm.deleteExpiredContextsOfGroup(groupURI,null, null);
        dm.close();
    }
    
    
    @Override
    public String cleanupMetaGarbage()
    {
        ReadWriteDataManager dm = 
            ReadWriteDataManagerImpl.openDataManager(Global.repository);
        return dm.cleanupMetaGarbage();
    }

    
    @Override
    public String upload(String filename, byte[] attachment) throws RemoteException, IOException
    {
    	com.fluidops.iwb.cms.File file = IWBCmsUtil.upload(filename, null, new ByteArrayInputStream( attachment ));
    	return file.getName();
    }

	@Override
    public void uploadUrls(List<String> documentUrls, Collector collector) throws IOException {
		for (String urlString : documentUrls) {
			URL url = new URL(urlString);
			IWBCmsUtil.upload(url.getPath(), valueFactory.createURI(url.toString()), url.openStream(), collector);
		}
	}
	
    @Override
    public String load(String filename, String format, String context, String source, String group, Boolean userEditable) throws Exception 
    {
        return load(filename,format,context,source,group,ContextType.CLI,userEditable);
    }
    
	public String load(String filename, String format, String context, String source, String group, ContextType contextType, Boolean userEditable) throws Exception 
	{   
	    NamespaceService ns = EndpointImpl.api().getNamespaceService();
	    
	    File file = new File(filename);
	    
	    URI contextURI=null;
	    URI sourceURI=null;
	    URI groupURI=null;
	    
	    if(context!=null)
	    {
	        contextURI = ns.parseURI(context);
	        if(contextURI==null)
	            return "Not a valid context URI";
	    }     
	    
	    if(source!=null)
	    {
	        sourceURI = ns.parseURI(source);
	        if(sourceURI==null)
	            return "Not a valid source URI";
	    }     
	    
	    if(group!=null)
	    {
	    	groupURI = ns.parseURI(group);
	    	if(groupURI==null)
	    		return "Not a valid group URI";
	    }
	    else
	    {
	    	groupURI = null;
	    }
	    
		RDFFormat rdfFormat = ReadDataManagerImpl.parseRdfFormat( format );
		if (format==null)
			rdfFormat = RDFFormat.forFileName(filename, rdfFormat);
		long start = System.currentTimeMillis();

		if(userEditable==null)
		    userEditable=Config.getConfig().getContextsEditableDefault();
		
		ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
		FileInputStream in = new FileInputStream(file);
		try
		{
    		long before=dm.size();
    		long after=0;

			dm.importRDFfromInputStream(in, file.toURI().toString(), rdfFormat, 
					Context.getFreshPublishedContext(contextType, contextURI, 
							sourceURI, groupURI, null, userEditable, ContextLabel.RDF_IMPORT));

            after = dm.size();
                		
            long finish = System.currentTimeMillis();
    		return "Loaded "+ (after-before) + " triples (including context information) in " + (finish-start) /1000 + " seconds";
		}
		finally
		{
			IOUtils.closeQuietly( in );
		    ReadWriteDataManagerImpl.closeQuietly(dm);
		}
	}
	
	@Override
	public void loadDatasetsFromDir(String dir,  String format, String context, String source, String group, Boolean userEditable) throws Exception {
		
		File file = new File(dir);
		if (!file.exists() || !file.isDirectory())
			return;
		
		logger.info("Loading all files from directory: " + file.getAbsolutePath() );
		for (File f : file.listFiles()) {			
			// for RDFFormat use heuristics from enum class, 
			// e.g. ".rdf" and ".owl" result in RDFXML
			load(f.getAbsolutePath(), format, context, source, group, userEditable);
			logger.info("Loaded file " + f.getAbsolutePath() + " into repository, context=" + context);
		}
		
	}
	
	@Override
	public String setContextsOfSourceEditable(String source, Boolean editable)
	    throws RemoteException, Exception
	{	
	    ReadWriteDataManager cm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
	    
	    URI u = EndpointImpl.api().getNamespaceService().parseURI(source);
	    if (u==null)
	        return "Source is not a valid URI";
	    
	    List<Context> contexts = cm.getContextsForSource(u);
	    boolean e = editable!=null && editable;
	    for (int i=0;i<contexts.size();i++)
	    {
	        Context c = contexts.get(i);
	        cm.setContextEditable(c, e);
	    }
	    
	    String es = e?"editable":"non-editable";
	    return "Set '" + contexts.size() + "' context(s) " + es + ".";
	}
	
	@Override
	public String setContextsOfGroupEditable(String group, Boolean editable)
	    throws RemoteException, Exception
    {
	    ReadWriteDataManager cm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
	        
	    URI u = EndpointImpl.api().getNamespaceService().parseURI(group);
	    if (u==null)
	        return "Group is not a valid URI";
	        
	    List<Context> contexts = cm.getContextsForGroup(u);
	    boolean e = editable!=null && editable;
	    for (int i=0;i<contexts.size();i++)
	    {
	        Context c = contexts.get(i);
	        cm.setContextEditable(c, e);
	    }
	        
	    String es = e?"editable":"non-editable";
	    return "Set '" + contexts.size() + "' context(s) " + es + ".";
    }
	
	@Override
	public String export(String filename, String format, String context) throws Exception 
	{
	    Repository repository = Global.repository;
	    
	    RDFFormat rdfFormat = RDFFormat.NTRIPLES; //default is NTRIPLES
	    if(format!=null)
	    {
	       rdfFormat = ReadDataManagerImpl.parseRdfFormat( format );
	       if(rdfFormat==null)
	           return "Not a valid RDF format";
	    }
	    long start = System.currentTimeMillis();
	    RepositoryConnection con = repository.getConnection();
	    long size;
	    
	    
	    URI contextURI = null;
	    if(context!=null)
	    {
	        NamespaceService ns = EndpointImpl.api().getNamespaceService();
	            contextURI = ns.parseURI(context);
	            if(contextURI==null)
	                return "Not a valid context";
	    }   
	
	    FileOutputStream out = new FileOutputStream(filename);
	    RDFWriter writer = Rio.createWriter(rdfFormat, out);
	    if(contextURI!=null)
	    {
	        size = con.size(contextURI);
	        con.export(writer, contextURI);
	    }
	        
	    else 
	    {
	        size = con.size();
	        con.export(writer);
	    }
	    out.close();
	    long finish = System.currentTimeMillis();
	    return "Exported "+ size + " triples in " + (finish-start) /1000 + " seconds";
	}	
	
	@Override
	public void createGraphindex() throws RemoteException, Exception
	{
		try {
			Class<?> cls = Class.forName("com.fluidops.iwb.ext.util.InitializerUtil");
			Method meth = cls.getMethod("redirectGraphindexDirectory");
			
			meth.invoke(cls);
		}
		catch (Exception e) {
			logger.warn("not supported", e);
		}
		
//		InitializerUtil.redirectGraphindexDirectory();
	}

	@Override
	public void updateKeywordIndex() throws Exception 
	{
		KeywordIndexAPI.updateKeywordIndex();
	}
	
	@Override
	public void updateWikiIndex() throws Exception 
	{
		KeywordIndexAPI.updateWikiIndex();
	}

	public void updateOwlimIndices(String repositoryName,
			String repositoryServer) throws RemoteException, Exception
	{
		OwlimIndexService.Config cfg = new OwlimIndexService.Config();
		cfg.repositoryName = repositoryName;
		cfg.repositoryServer = repositoryServer;
		new OwlimIndexService().run(cfg);
	}    
	
	@Override
	public void extractSemanticLinks() throws Exception 
	{
		SemanticLinkExtractor.extractSemanticLinks();
	}
	
	@Override
	public void importMediaWiki(String filename, ReaderFlag option,  final String namespace, Integer queuesize, Integer flags) throws Exception
	{
		queuesize = (queuesize == null) ? Integer.valueOf(0) : queuesize;
		flags = (flags == null) ? Integer.valueOf(0) : flags;
	    XMLWikiReader wikiReader = new XMLWikiReader(filename, namespace, queuesize, flags);
	    new WikiIndexer(wikiReader);
	    
	    wikiReader.parse();
	    
	    String read;
	    do {
	        logger.info("Hit enter to abort algorithm");
	        CliRunner.input.next();
	        logger.warn("Please type yes [y] if you want to abort:");
	        read = CliRunner.input.next();
	    } while (read.isEmpty() || read.charAt(0) != 'y');
	    wikiReader.abort();
	}

	@Override
	public String version() throws RemoteException
	{
		return Version.getProductLongName() + " " + Version.getVersion();
	}

    @Override
    public Object[] rename(String olduri, String newuri, Boolean whatif)
            throws RemoteException
    {
        
        Set<Resource> affectedContexts = new HashSet<Resource>();
        ValueFactory f = ValueFactoryImpl.getInstance();
        NamespaceService ns = EndpointImpl.api().getNamespaceService();
        URI oldURI = ns.parseURI(olduri);
        URI newURI = ns.parseURI(newuri);
        
        
        try {
        RepositoryConnection con =  Global.repository.getConnection();
        
        
        RepositoryResult<Statement> stmts = con.getStatements(oldURI, null, null, false);
        while(stmts.hasNext()) {
            Statement s = stmts.next();
            affectedContexts.add(s.getContext());
            if(!whatif) {
                con.remove(s);
                con.add(f.createStatement(newURI, s.getPredicate(), s.getObject()), s.getContext());
            }    
        }
        
        stmts = con.getStatements(null, oldURI, null, false);
        while(stmts.hasNext()) {
            Statement s = stmts.next();
            affectedContexts.add(s.getContext());
            if(!whatif) {
                con.remove(s);
                con.add(f.createStatement(s.getSubject(), newURI, s.getObject()), s.getContext());
            }    
        }

        stmts = con.getStatements(null, null, oldURI, false);
        while(stmts.hasNext()) {
            Statement s = stmts.next();
            affectedContexts.add(s.getContext());
            if(!whatif) {
                con.remove(s);
                con.add(f.createStatement(s.getSubject(), s.getPredicate(), newURI), s.getContext());
            }    
        }
        con.close();
        }
        catch(RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        
        
        return affectedContexts.toArray();
     
    }

    private Singleton<ProviderService> providerService = new Singleton<ProviderService>() {
    	protected ProviderService createInstance() throws Exception {
    		return new ProviderServiceImpl();
    	}
    };
    
    public ProviderService getProviderService() throws RemoteException
    {
    	return providerService.instance();
    }

    /**
     * The communication service is parametrized along group and ontology
     * context. There is one context for each group+ontology-context pair.
     */
    protected Map<Pair<URI,URI>,CommunicationService> communicationServices =
        new HashMap<Pair<URI,URI>,CommunicationService>();
    
    
    private Singleton<WidgetService> widgetService = new Singleton<WidgetService>() {
    	protected WidgetService createInstance() throws Exception {
    		return new WidgetServiceImpl();
    	}
    };
    
    public WidgetService getWidgetService()
    {
    	return widgetService.instance();
    }

    public BackupService getBackupService()
    {
        throw new RuntimeException("Backup Service not available in IWB Community Edition");
    }

    @Doc( "Returns the current user name" )
    public String getUserName() throws Exception
    {
    	return getUserManager().getUser(null);
    }

    
    @Doc( "Returns the current user's URI (null-safe)" )
    public URI getUserURI()
    {
    	try
    	{
    		URI userUri = getUserManager().getUserURI(null);
    		return userUri == null ? Vocabulary.SYSTEM.UNKNOWNUSER : userUri;
    	}
    	catch (Exception e)
    	{
    		return Vocabulary.SYSTEM.UNKNOWNUSER;
    	}
    }
    
    
    private Singleton<ExternalNamespaceService> externalNamespaceService = new Singleton<ExternalNamespaceService>() {
    	protected ExternalNamespaceService createInstance() throws Exception {
    		return new ExternalNamespaceServiceImpl();
    	}
    };
	
    public ExternalNamespaceService getExternalNamespaceService()
    {
        return externalNamespaceService.instance();
    }
    
    
    private UpdatableSingleton<NamespaceService> namespaceService = new UpdatableSingleton<NamespaceService>() {
    	protected NamespaceService createInstance() throws Exception {
    		return new NamespaceServiceImpl();
    	}
    };

    @Override
    public NamespaceService getNamespaceService()
    {
        return namespaceService.instance();
    }
    
    public void resetNamespaceService()
    {
        namespaceService.reset();
    }
    
	
	private Singleton<Layouter> layouter = new Singleton<Layouter>() {
    	protected Layouter createInstance() throws Exception {
    		return new LayouterImpl();
    	}
    };
    
	@Override
	public Layouter getLayouter()
	{
		return layouter.instance();
	}
	
	private Singleton<WidgetSelector> widgetSelector = new Singleton<WidgetSelector>() {
    	protected WidgetSelector createInstance() throws Exception {
    		try {
				return (WidgetSelector) Class.forName(Config.getConfig().getWidgetSelector()).newInstance();
			} catch (Exception e) {
				logger.error("Error initializing widget selector: " + e.getMessage(), e);
				throw e;
			} 
    	}
    };

	private Singleton<RequestMapper> requestMapper = new Singleton<RequestMapper>() {
    	protected RequestMapper createInstance() throws Exception {
    		return new RequestMapperImpl();
    	}
    };
    
    private Singleton<UserManager> userManager = new Singleton<UserManager>() {
    	protected UserManager createInstance() throws Exception {
    		return new UserManagerImpl();
    	}
    };
    
	private Singleton<Printer> printer = new Singleton<Printer>() {
		protected Printer createInstance() throws Exception	{
			return new PrinterImpl();
		}
	};	

	@Override
	public RequestMapper getRequestMapper()
	{
		return requestMapper.instance();
	}
	
	@Override
	public UserManager getUserManager()
	{
		return userManager.instance();
	} 

	@Override
	public WidgetSelector getWidgetSelector()
	{
		return widgetSelector.instance();
	}

	@Override
	public ReadDataManagerImpl getDataManager()
	{
	    return ReadDataManagerImpl.getDataManager(Global.repository);
	}

	@Override
	public Printer getPrinter()
	{
		return printer.instance();
	}

	@Override
	public void invalidateAllCaches() 
	{
		CacheManager cm = CacheManager.getInstance();
		cm.invalidateAllCaches();
	}

    @CallableFromWidget
    public static String refreshNamespaces(PageContext p,String param)
    {
        EndpointImpl.api().resetNamespaceService();
        return "Changes deployed successfully!";
    }


    @Override
    public Collection<CommunicationService> getAllCommunicationServices()
    {
    	return communicationServices.values();
    }
    
    @Override
    public CommunicationService getCommunicationService(String group, String ontologyContext) throws RemoteException
    {
    	URI groupUri = EndpointImpl.api().getNamespaceService().guessURI(group);
    	URI ontologyContextUri = EndpointImpl.api().getNamespaceService().guessURI(ontologyContext);
    	
    	return getCommunicationService(groupUri, ontologyContextUri);
    }
    
    /**
     * Help method for CommunicationService setup
     * 
     * @param groupUri
     * @param ontologyContextUri
     * @return
     * @throws RemoteException
     */
    public CommunicationService getCommunicationService(URI groupUri, URI ontologyContextUri) throws RemoteException
    {
    	if (groupUri==null)
    		throw new RuntimeException("groupUri in CommunicationService initialization is null");
    	if (ontologyContextUri==null)
    		throw new RuntimeException("ontologyContext in CommunicationService initialization is null");
    	
        Pair<URI,URI> csId = new Pair<URI,URI>(groupUri,ontologyContextUri);
        CommunicationService cs = communicationServices.get(csId);
        if ( cs == null )
        {
            try
            {
                cs = new CommunicationServiceImpl(groupUri,ontologyContextUri);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage(),e);
            }
            communicationServices.put(csId, cs);
        }
        
        return cs;
    }
    
    protected Singleton<MonitoringService> monitoringService = new Singleton<MonitoringService>() {
		protected MonitoringService createInstance() throws Exception	{
			return createMonitoringService();
		}
	};
	
	/**
	 * Subclasses can overwrite this, i.e. EE can return advanced
	 * monitoring service
	 * @return
	 */
	protected MonitoringService createMonitoringService() {
		return new MonitoringServiceImpl();
	}
    
    public MonitoringService getMonitoringService() throws RemoteException
    {
    	return monitoringService.instance(); 
    }

	@Override
	public String add(Statement statement) throws RemoteException {
				  
		ReadWriteDataManager dm = null;
		try {
			Context context = Context.getFreshUserContext(ContextLabel.RDF_IMPORT);	
			dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);			
			dm.addToContext(statement, context);
			return context.getURI().stringValue();
		} 
		finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
	}

	@Override
	public Set<String> delete(Statement statement)
			throws RemoteException {
		ReadWriteDataManager dm = null;
		try {
			Context changelog = Context.getFreshUserContext(ContextLabel.CHANGELOG);
			dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);								
			Set<URI> modifiedContexts = dm.removeInEditableContexts(statement, changelog);
			dm.cleanupMetaGarbage(modifiedContexts);
			return Sets.newHashSet(Iterables.transform(modifiedContexts, new Function<URI, String>() {
				@Override
				public String apply(URI u) {
					return u.stringValue();
				}				
			}));			 
		} 
		finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
	}

	@Override
	public void restartService() throws Exception
	{
		logger.info("Service RESTART requested by user "+EndpointImpl.api().getUserManager().getUserName(null));
		if(WrapperManager.isControlledByNativeWrapper())
		WrapperManager.restartAndReturn();		
		else
		    logger.info("Service RESTART not possible, as jvm is not controlled by native wrapper");
	}

	@Override
	public void stopService(Integer code) throws Exception
	{
		if ( code == null )
			code = 0;
		
		logger.info("Service STOP with code "+code+" requested by user "+ EndpointImpl.api().getUserManager().getUserName(null));
		WrapperManager.stopAndReturn(code);		
	}

    private Singleton<SolutionService> solutionService = new Singleton<SolutionService>() {
        protected SolutionService createInstance() throws Exception {
            File configPropFile = new File(Config.getConfig().getSourcePath());
            DBBulkServiceImpl dbBulkService = new DBBulkServiceImpl(new Supplier<Repository>() {
				@Override public Repository get() { return Global.repository; }
			});
            CompositeSolutionHandler handler = 
                    new CompositeSolutionHandler(new ConfigPropHandler(
                            new PropertyMergerImpl(), configPropFile.getParentFile(), configPropFile.getName()))
            .add(new GroovyConfigHandler("config.groovy", getApplicationFolder(), configPropFile.getName()))
            .add(new ImagesPropHandler(new PropertyMergerImpl(), getConfigFolder().getParentFile()))
            .add(new WikiBotPropHandler(new PropertyMergerImpl(), getConfigFolder().getParentFile()))
            .add(new JarHandler(getApplicationFolder()))
            .add(new CopyFolderStructureHandler(getApplicationFolder(), "scripts"))
            .add(new CopyFolderStructureHandler(
                    getApplicationFolder(), "resources", INSTALLED_SUCCESSFULLY_RESTART_REQUIRED))
            .add(new CopyFolderStructureHandler(getApplicationFolder(), "webapps"))
            .add(new CopyFolderStructureHandler(
                    getConfigFolder().getParentFile(), "config/acl", INSTALLED_SUCCESSFULLY_RESTART_REQUIRED))
            .add(new CopyFolderStructureHandler(
                    getConfigFolder().getParentFile(), "config/annotations", INSTALLED_SUCCESSFULLY_RESTART_REQUIRED))
            .add(new JettyHandler(getApplicationFolder())) 
            .add(new WidgetHandler(getWidgetSelector()))
            .add(new WidgetClassHandler(getWidgetService()))
            .add(new NamespaceHandler(getNamespaceService()))
            .add(new ProviderHandler(getProviderService()))
            .add(new ProviderClassHandler(getProviderService()))
            .add(new DBBootstrapHandler(dbBulkService))
            .add(new OntologyUpdateHandler(dbBulkService))
            .add(new WikiBootstrapHandler(new WikiStorageBulkServiceImpl(Wikimedia.getWikiStorage())));

            return new ChainBasedSolutionService(
                    new ZipFileBasedSolutionService(getApplicationFolder(), handler),
                    new DirReferenceBasedSolutionService(getApplicationFolder(), handler));
        }
    };
	   
	@Override
	public ExternalSolutionService getSolutionService() throws RemoteException
	{
	    return solutionService.instance();
	}
}
