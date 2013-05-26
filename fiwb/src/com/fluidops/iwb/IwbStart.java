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

package com.fluidops.iwb;

import static com.fluidops.iwb.api.EndpointImpl.*;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;

import com.fluidops.ajax.FSession;
import com.fluidops.api.dynamic.DynamicServiceRegistry;
import com.fluidops.api.dynamic.DynamicServiceUtil;
import com.fluidops.api.security.ssl.SSLUtils;
import com.fluidops.iwb.api.APIRemote;
import com.fluidops.iwb.api.DBBulkServiceImpl;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.LoginImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.WidgetSelectorImpl;
import com.fluidops.iwb.api.WidgetServiceImpl;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl;
import com.fluidops.iwb.api.solution.CompositeInstallationResult;
import com.fluidops.iwb.api.solution.InstallationResult;
import com.fluidops.iwb.api.solution.SolutionService;
import com.fluidops.iwb.extensions.PrinterExtensionsImpl;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.server.IwbServletContextListener;
import com.fluidops.iwb.user.UserManagementAdministration;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.iwb.util.RepositoryFactory;
import com.fluidops.iwb.util.analyzer.Analyzer;
import com.fluidops.iwb.util.analyzer.AnalyzingRepository;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.iwb.wiki.parserfunction.ParserFunctionsFactory;
import com.fluidops.jetty.StartJetty;
import com.fluidops.network.RMIBase;
import com.fluidops.network.RMISSLSocketFactory;
import com.fluidops.test.Start.AjaxSessionExpiredPage;
import com.fluidops.util.SSL;
import com.fluidops.util.i18n.I18n;


/**
 * Main startup of the Information workbench. Provides a main method
 * to start an embedded Jetty server, which starts up the Information
 * Workbench web application and performs the initializations. In 
 * addition this class maintains the initialization code that is
 * relevant to the Information Workbench platform and that is
 * executed at startup of the web application via servlet context 
 * listeners. *
 */
public class IwbStart
{
    private static final Logger logger = Logger.getLogger(IwbStart.class.getName());
	
    /**
     * Initialize the Information Workbench application:
     * 
     * 1) Log4j configuration
     * 2) Jetty startup
     * 
     * The actual webapp is initialized by means of servlet
     * context listeners, e.g. {@link IwbServletContextListener}.
     * for IWB CE. The listener in turn invokes an initialization
     * method for the webapp, which in case of the community 
     * edition is {@link #initializeIWBWebapp(String)}. 
     *  
     * @throws Exception
     */
	public static void initializeApp() throws Exception {
		
		// initialize logging for iwbcom
        PropertyConfigurator.configure("etc/log4j.properties");        
        logger.info("Starting Information Workbench");
        logger.debug("Version information: " + Version.getProductLongName());
        
		Config.getConfigWithOverride();	
		
        I18n.initialize();
        
        // for Information Workbench, by default we support only local authorization        
        com.fluidops.config.Config.getConfig().setAuthConfigNameDefault("Local");

		final String debugfiwb = System.getenv("debugfiwb");
		if (debugfiwb != null && debugfiwb.equals("1"))
		{
			logger.info("Started in debug mode");
			System.in.read();
		}        

		StartJetty.main(new String[0]);
		
		System.out.println("Information Workbench Server URL: "
				+ StartJetty.getDefaultHttpUrl());

		// distinguish between running as a Service or use Key input to shutdown
		if (!Config.getConfig().getRunningAsService())
		{
			System.out.println();
			System.out.println("press key to shutdown");
			System.in.read();

			logger.info("Exiting Information Workbench");
			System.exit(0);
		}
		else
		{
			System.out.println("running as a service");
		}
	}
	
	
	/**
	 * Initialize the Information Workbench webapp:
	 * 
	 * 1) invoke the generic Information Workbench initializaton code
	 *    via {@link #initializeWebapp(String)}
	 * 2) register RMI endpoint
	 * 3) register shutdown hook
	 * 
	 * This method is to be called once at startup of the web application,
	 * e.g. in case of the community edition from the 
	 * {@link IwbServletContextListener} that is registered to the webapp
	 * 
	 * @param j2eeWebPrefix
	 * @throws Exception
	 */
	public static void initializeIWBWebapp(String j2eeWebPrefix) throws Exception {
		        
        // force shutdownhook to persist datasources
        // note:
        // (1) this method is also called from IWBCom startup code, so the
        //     shutdown hook is also registered form IWBCom code
        // (2) this method is NOT called when starting eCM; for eCM, we call
        //      method shutdownIwb() explicitly inside the eCM shutdown hook,
        //      to make sure that shutdown is performed in order (IWB must be
        //      shut down after all eCM shutdown code has been performed!)
		addIwbShutdownHook();
		
		initializeWebapp(j2eeWebPrefix);
					
		if (EndpointImpl.USE_SSL) {
			// FIXME this is essentially the code from fcoremgt SSLSupport#enableSSLSupportServer()
			// however fcoremgtm is not referenced in here, so we cannot use it
			RMISSLSocketFactory.initializeServer(
					IWBFileUtil.getFileInWorkingDir("fluidops.ca").getAbsolutePath(), "changeit", 
					IWBFileUtil.getFileInWorkingDir("fluidops.key").getAbsolutePath(), "01FluidOps02");
			RMIBase.initialize();
			SSLUtils.enableSSL();
		}		
		SSL.trustAll();
		
		LoginImpl rmiimpl = new LoginImpl(APIRemote.class, EndpointImpl.class);
        RMIBase.createRegistry( Config.getConfig().getRMIPort() ).bind( "OM", rmiimpl );
        
        DynamicServiceUtil.initializeDynamicServices();
	}
	
	
	/**
	 * Generic initialization code for the Information Workbench that is
	 * to be executed in all different editions.
	 * 
	 * @param j2eeWebPrefix 
	 */
	public static void initializeWebapp(String j2eeWebPrefix) throws Exception
	{
		// set context path globally
		EndpointImpl.api().getRequestMapper().setContextPath(j2eeWebPrefix);

		// use a factory to retrieve the repository
		Global.repository = RepositoryFactory.getRepository(Config.getConfig().getRepositoryType());
		
		// create a LuceneSail for searching wiki pages
		Global.wikiLuceneRepository = RepositoryFactory.getWikiLuceneSailRepository();
		
		/* Analyze module: monitor use of connections and calls to underlying repositories */
		if (Config.getConfig().getAnalyzeMode()) 
		{
			logger.info("Analyzing mode is enabled.");
			Analyzer.init();
			Global.repository = new AnalyzingRepository(Global.repository);
		}
			
		// make sure directory structure and files exist
		IWBFileUtil.createFolderIfNotExists(IWBFileUtil.getConfigFolder());
		IWBFileUtil.createFolderIfNotExists(IWBFileUtil.getDataFolder());
		IWBFileUtil.createFolderIfNotExists(IWBFileUtil.getSilkFolder());		
		IWBFileUtil.createFolderIfNotExists(IWBFileUtil.getBackupFolder());
		IWBFileUtil.createFileIfNotExists(IWBFileUtil.getFileInConfigFolder("namespaces.prop"));
		IWBFileUtil.createFileIfNotExists(IWBFileUtil.getFileInConfigFolder("widgets.prop"));
		IWBFileUtil.createFolderIfNotExists(IWBFileUtil.getWikiFolder());

		
		// extensions
		if (Global.printerExtension == null)
			Global.printerExtension = new PrinterExtensionsImpl();

		Global.repository.initialize();
		Global.repository = new NotifyingRepositoryWrapper(Global.repository);		
		
        // bootstrap IWB Wiki pages and database
		// Only load dbBootstrap if config parameter is set.
        if(Config.getConfig().loadDBBootstrap())
        	IwbStart.bootstrapDB();

        IwbStart.bootstrapWiki();

		// by default, historic data is stored in global repository, 
		// may be overridden in separate startup code, so we have to check
		// whether it has already been set from outside
		if (Global.historyRepository==null)
			Global.historyRepository = Global.repository;
				
		// initialize user from userManagementInit.prop (if the file exists)		
		UserManagementAdministration.initializeUserFromFile(
				IWBFileUtil.getFileInConfigFolder("userManagementInit.prop"));
		
		// register jsps
		FSession.registerPage(FSession.PageCode.SESSION_EXPIRED, AjaxSessionExpiredPage.class);
	    
        // startup provider service
		EndpointImpl.api().getProviderService().load();
		// startup widget service
		WidgetServiceImpl.load();
		WidgetSelectorImpl.load();
		
		installSolutions();
		
		// register custom parser functions
		ParserFunctionsFactory.registerCustomParserFunctions();
		
	}
	
	

	/**
	 * Registers the IWB shutdown hook
	 */
	public static void addIwbShutdownHook()
	{
		// save all cache models before shutting down
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				shutdownIwb();
			}
		});
	}
	
	/**
	 * Static method performing IWB shutdown, currently both IWB CE and EE.
	 * Blocks until all shutdown code has been executed.
	 * 
	 * NOTE: method is invoked via reflection from other projects. do not
	 * change signature
	 */
	public static void shutdownIwb()
	{
		// shutdown timers
		logger.info("Shutting down IWB timers...");
		try 
		{
			TimerRegistry.getInstance().shutdownTimers();
		} 
		catch (Exception e) 
		{
			logger.warn(e.getMessage(),e);
		}
		
		logger.info("Shutting down keyword search engine...");
        KeywordIndexAPI.shutdown();

		// close pending connections of global
		// ReadDataManagerImpl objects:
		logger.info("Shutting down global data manager...");
        ReadDataManagerImpl.shutdown();

        // repository
		logger.info("Shutting down global repository...");
		try
		{
			Global.repository.shutDown();
			Wikimedia.getWikiStorage().shutdown();
		}
		catch (RepositoryException e)
		{
			logger.warn(e.getMessage(),e);
		}
		logger.info("-> repository shut down.");
		
        // history repository				
		if (Global.historyRepository==null)
			logger.info("Global history repository not initialized (no shutdown required)");
		else
		{
			logger.info("Shutting down global history repository...");
			if (!Global.historyRepository.equals(Global.repository))
			{
				try 
				{
					Global.historyRepository.shutDown();
				} 
				catch (RepositoryException e) 	
				{
					logger.warn(e.getMessage(),e);
				}
				logger.info("-> repository shut down.");
			}
			else
				logger.info("-> repository was shut down already.");
		}
		
		logger.info("Shutting down dynamic services");
		DynamicServiceRegistry.shutdown();
	}
	
	public static void main(String[] args) throws Exception
	{
		initializeApp();
	}
	
	public static boolean installSolutions() throws Exception {
        SolutionService solutionService = (SolutionService) api().getSolutionService();
        URL[] solutions = solutionService.detectSolutions();
        if ((solutions == null) || (solutions.length == 0)) return false;
        
        CompositeInstallationResult compositeResult = new CompositeInstallationResult();
        for (int s = 0; s < solutions.length; s++) {
			URL solution = solutions[s];
			InstallationResult result = solutionService.install(solution);
			compositeResult.addResultForHandler(solution.toString(), result);
		}
        if(compositeResult.getInstallationStatus().isRestartRequired()) api().restartService();
        return true;
	}
	
	/**
	 * Bootstraps the wiki from a given a bootstrap directory. May be called
	 * multiple times for merging multiple wiki bootstrap directories (in the
	 * latter case, for duplicate wiki pages the last bootstrap directory
	 * page will be used).
	 */
    public static void bootstrapWiki()
    {
		new WikiStorageBulkServiceImpl(Wikimedia.getWikiStorage())
				.bootstrapWikiAndRemove(IWBFileUtil.getDataFolder());
    }
    
    /**
     * Load files from dbBootstrap folder into DB.
     */
    public static void bootstrapDB()
    {    	
        File bootstrapDir = IWBFileUtil.getFileInDataFolder("dbBootstrap");
        new DBBulkServiceImpl(Global.repository).bootstrapDBAllFromAndRemove(bootstrapDir);
    }

    
}
