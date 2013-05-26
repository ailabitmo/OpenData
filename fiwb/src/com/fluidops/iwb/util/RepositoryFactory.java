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

package com.fluidops.iwb.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.lucene.LuceneSail;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.nativerdf.NativeStore;

import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.util.Base64;
import com.fluidops.util.StringUtil;
import com.fluidops.util.scripting.DynamicScriptingSupport;

/**
 * Repository Factory to load supported and custom Repositories via {@link #getRepository(String)}.
 * 
 * Supported Types:
 * 
 * <ul>
 *   <li>native: {@link NativeRepositoryFactory}</li>
 *   <li>memory: {@link MemoryRepositoryFactory}</li>
 *   <li>owlim | bigowlim: {@link OwlimRepositoryFactory}</li>
 *   <li>remoterepository: {@link RemoteRepositoryFactory}</li>
 *   <li>sparql: {@link SparqlRepositoryFactory}</li>
 *   <li>fedx: {@link FedXRepositoryFactory}</li>
 *   <li>class=com.MyRepositoryFactory: {@link CustomRepositoryFactory}</li>
 * </ul>
 * 
 * To load a custom repository set
 *  	repositoryType = class=com.mypackage.MyRepositoryFactory
 *  
 *  where MyRepositoryFactory extends {@link RepositoryFactoryBase}
 *  and provides a default constructor.
 *  
 * @author as
 *
 */
public class RepositoryFactory
{
	private static final Logger logger = Logger.getLogger(RepositoryFactory.class.getName());
	
	/**
	 * Abstract base class for any RepositoryFactory. Can be extended to load
	 * custom repositories via 'repositoryType = class=com.mypackage.MyRepositoryFactory'
	 * 
	 * @author as
	 */
	public static abstract class RepositoryFactoryBase 
	{
		public abstract Repository loadRepository() throws Exception;
	}
	
	
	/**
	 * Return the repository matching the provided repositoryType.<p>
	 * 
	 * Supported Types:
	 * 
	 * <ul>
	 *   <li>native: {@link NativeRepositoryFactory}</li>
	 *   <li>memory: {@link MemoryRepositoryFactory}</li>
	 *   <li>owlim | bigowlim: {@link OwlimRepositoryFactory}</li>
	 *   <li>swiftowlim: {@link SwiftOwlimRepositoryFactory}</li>
	 *   <li>remoterepository: {@link RemoteRepositoryFactory}</li>
	 *   <li>httprepository: {@link HttpRepositoryFactory}</li>
	 *   <li>sparql: {@link SparqlRepositoryFactory}</li>
	 *   <li>fedx: {@link FedXRepositoryFactory}</li>
	 *   <li>class=com.MyRepositoryFactory: {@link CustomRepositoryFactory}</li>
	 * </ul>
	 * 
	 * To load a custom repository set
	 *  	repositoryType = class=com.mypackage.MyRepositoryFactory
	 *  
	 *  where MyRepositoryFactory extends {@link RepositoryFactoryBase}
	 *  and provides a default constructor.
	 * 
	 * @param repositoryType
	 * @return
	 * @throws Exception
	 */
	public static Repository getRepository(String repositoryType) throws Exception
	{
		logger.info("Repository type set to " + repositoryType);
		
		if (repositoryType.equals("native"))
		{
			if (Config.getConfig().getEnableFulltextIndex())
				return new NativeRepositoryFactoryWithLucene().loadRepository();
			else
				return new NativeRepositoryFactory().loadRepository();
		}
		if (repositoryType.equals("sparql"))
			return new SparqlRepositoryFactory().loadRepository();
		if (repositoryType.equals("oracle"))
			return new OracleRepositoryFactory().loadRepository();
		if (repositoryType.equals("memory"))
		{
			if (Config.getConfig().getEnableFulltextIndex())
				return new MemoryRepositoryFactoryWithLucene().loadRepository();
			else
				return new MemoryRepositoryFactory().loadRepository();
		}
		if (repositoryType.equals("owlim") || repositoryType.equals("bigowlim"))
			return new OwlimRepositoryFactory().loadRepository();
		if (repositoryType.equals("remoterepository"))
			return new RemoteRepositoryFactory().loadRepository();
		if (repositoryType.equals("fedx"))
			return new FedXRepositoryFactory().loadRepository();
		if (repositoryType.startsWith("class="))
			return new CustomRepositoryFactory(repositoryType).loadRepository();
		
		logger.error("Unknown repository type: " + repositoryType);
		throw new RuntimeException("Unknown repository type: " + repositoryType);
	}
	
	/**
	 * Create a new {@link NativeStore} with a given repository name.
	 * 
	 * @param repositoryName
	 * @return
	 * @throws Exception
	 */
	public static Repository getNativeRepository(String repositoryName) throws Exception {
		logger.info("Loading native repository with name " + repositoryName);
		return new NativeRepositoryFactory(repositoryName).loadRepository();
	}
	
   /**
     * Create a new OWLIM store with given repository location
     * @param repositoryName Physical location of store
     * @return An OWLIM store
     * @throws Exception
     */
    public static Repository getOwlimRepository(String repositoryName) throws Exception {
        logger.info("Loading owlim repository with name " + repositoryName);
        return new OwlimRepositoryFactory(repositoryName).loadRepository();
    }
    
    /**
     * Create a SPARQL repository for the given endpoint
     * 
     * @param endpoint
     * @return
     * @throws Exception
     */
    public static Repository getSPARQLRepository(String endpoint) throws Exception {
    	logger.info("Loading SPARQL repository for endpoint " + endpoint);
        return new SparqlRepositoryFactory(endpoint).loadRepository();
    }
    
    /**
     * Create a SPARQL repository for the given endpoint using the provided user
     * credentials for basic authentication
     * 
     * @param endpoint
     * @param user
     * @param pass
     * @return
     * @throws Exception
     */
    public static Repository getSPARQLRepository(String endpoint, String user, String pass) throws Exception {
		logger.info("Loading SPARQL repository for endpoint " + endpoint
				+ (user != null ? (", basic authentication for user " + user): ""));
        return new SparqlRepositoryFactory(endpoint, user, pass).loadRepository();
    }
	
	/**
	 * Return a new remote repository instance for the given settings
	 * 
	 * @param repositoryServer
	 * @param repositoryName
	 * @return
	 * @throws Exception
	 */
	public static Repository getRemoteRepository(String repositoryServer,
			String repositoryName) throws Exception {
		logger.info("Loading remote repository with " + repositoryServer + "/" + repositoryName);
		return new RemoteRepositoryFactory(repositoryServer, repositoryName)
				.loadRepository();
	}
	
	/**
	 * 
	 * Return a new LuceneSail repository for wiki pages indexing.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static SailRepository getWikiLuceneSailRepository() throws Exception {
		MemoryStore store = new MemoryStore();
		LuceneSail luceneSail = new LuceneSail();
		
		luceneSail.setParameter(LuceneSail.LUCENE_DIR_KEY, IWBFileUtil.getWikiLuceneIndexFolder().getAbsolutePath());
		// index wiki pages only
		luceneSail.setParameter(LuceneSail.INDEXEDFIELDS, "index.1="+Vocabulary.SYSTEM.WIKI.stringValue());
		// use WikipediaAnalyzer
		luceneSail.setParameter(LuceneSail.ANALYZER_CLASS_KEY, "com.fluidops.iwb.api.wiki.WikipediaAnalyzer");
		
		luceneSail.setBaseSail(store);
		SailRepository resRepository = new SailRepository(luceneSail); 
		resRepository.initialize();
		
		return resRepository;
	}
	
	/**
	 * Factory for a {@link NativeStore}
	 * 
	 * {@link Config#getRepositoryName()}
	 * 
	 * @author as
	 */
	private static class NativeRepositoryFactory extends RepositoryFactoryBase {

		private final String repositoryName;
		
		public NativeRepositoryFactory() {
			this(Config.getConfig().getRepositoryName());
		}
		
		public NativeRepositoryFactory(String repositoryName) {
			this.repositoryName = repositoryName;
		}

		@Override
		public Repository loadRepository()
		{
			return new SailRepository(
					ReadDataManagerImpl.getNativeStore(IWBFileUtil.getFileInDataFolder(repositoryName)));
		}		
	}
	
	/**
	 * Factory for a {@link NativeStore} with Lucene, repositoryType = native
	 * 
	 * {@link Config#getRepositoryName()}
	 * 
	 * @author christian.huetter
	 */
	private static class NativeRepositoryFactoryWithLucene extends RepositoryFactoryBase {

		private final String repositoryName;
		
		public NativeRepositoryFactoryWithLucene() {
			this(Config.getConfig().getRepositoryName());
		}
		
		public NativeRepositoryFactoryWithLucene(String repositoryName) {
			this.repositoryName = repositoryName;
		}

		@Override
		public Repository loadRepository()
		{
			// create a sesame native store
			NativeStore store = ReadDataManagerImpl.getNativeStore(IWBFileUtil.getFileInDataFolder(repositoryName));

			// create a lucenesail to wrap the store
			LuceneSail luceneSail = new LuceneSail();
			// store the lucene index on disk
			luceneSail.setParameter(LuceneSail.LUCENE_DIR_KEY, IWBFileUtil.getLuceneIndexFolder().getAbsolutePath());
			// wrap store in a lucenesail
			luceneSail.setBaseSail(store);

			// create a Repository to access the sail
			return new SailRepository(luceneSail);
		}
	}
	
	/**
	 * Factory for a {@link MemoryStore}
	 * 
	 * @author as
	 */
	private static class MemoryRepositoryFactory extends RepositoryFactoryBase {

		@Override
		public Repository loadRepository()
		{
			return new SailRepository(new MemoryStore(IWBFileUtil.getFileInDataFolder(Config
							.getConfig().getRepositoryName())));
		}		
	}
	
	/**
	 * Factory for a {@link MemoryStore} with Lucene, repositoryType = memory
	 * 
	 * @author christian.huetter
	 */
	private static class MemoryRepositoryFactoryWithLucene extends RepositoryFactoryBase {

		@Override
		public Repository loadRepository()
		{
			// create a sesame memory store
			MemoryStore store = new MemoryStore(IWBFileUtil.getFileInDataFolder(Config.getConfig().getRepositoryName()));
			
			// create a lucenesail to wrap the memorystore
			LuceneSail luceneSail = new LuceneSail();
			// let the lucene index store its data in ram
			luceneSail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
			// wrap memorystore in a lucenesail
			luceneSail.setBaseSail(store);
			
			// create a Repository to access the sails
			return new SailRepository(luceneSail);
		}
	}
	
	
	/**
	 * Factory for a Owlim store, repositoryType = owlim | bigowlim (for legacy)
	 * 
	 * @author as
	 */
	private static class OwlimRepositoryFactory extends RepositoryFactoryBase {

		private final String repositoryLocation;
				
		/**
		 * storage folder is used from config
		 */
		public OwlimRepositoryFactory() {
			this(IWBFileUtil.getFileInDataFolder(Config.getConfig()
							.getRepositoryName()).getAbsolutePath()); 
		}

		/**
		 * @param repositoryLocation the storage folder for this OWLIM repository
		 */
		public OwlimRepositoryFactory(String repositoryLocation) {
			super();
			this.repositoryLocation = repositoryLocation;
		}


		@Override
		public Repository loadRepository() throws Exception
		{
		    System.setProperty("ruleset", "empty"); // for performance reasons
	        System.setProperty("repository-type", "weighted-file-repository");
			System.setProperty("console-thread", "false");
			System.setProperty("storage-folder", repositoryLocation);

			// Removed context indexing for the time being. Reason being this
            // can corrupt the store when using a pre-existing database, which
            // did not use context indexing.
//			System.setProperty("build-pcsot", "true");
//			System.setProperty("build-ptsoc", "true");
			
			// Node search for autocompletion
//			System.setProperty("ftsIndexPolicy", "onStartup");
//			System.setProperty("fts-memory", "20m");
//			System.setProperty("ftsLiteralsOnly", "false");
			
			// we initialize with empty ruleset
			return new SailRepository((Sail) Class
					.forName("com.ontotext.trree.OwlimSchemaRepository")
					.getConstructor().newInstance());
		}		
	}
		
	
	
	
	/**
	 * Factory for a remote repository, repositoryType = remoterepository
	 * 
	 * Supported Configuration parameters
	 *  - {@link Config#getRepositoryServer()}
	 *  - {@link Config#getRepositoryName()}
	 *  - {@link Config#getRepositoryUser()}
	 *  - {@link Config#getRepositoryPassword()}
	 *  
	 * @author as
	 */
	private static class RemoteRepositoryFactory extends RepositoryFactoryBase {

		private final String repositoryServer;
		private final String repositoryName;
		
		public RemoteRepositoryFactory() {
			// read server and name from config
			this(Config.getConfig().getRepositoryServer(), Config.getConfig().getRepositoryName());
		}
		 
		public RemoteRepositoryFactory(String repositoryServer,
				String repositoryName) {
			this.repositoryServer = repositoryServer;
			this.repositoryName = repositoryName;
		}

		@Override
		public Repository loadRepository() throws Exception
		{
					    
            if (repositoryServer.isEmpty() || repositoryName.isEmpty()) 
            {
            	logger.error("At least one of the properties 'repositoryServer'/'repositoryName' is not set.");
                throw new RuntimeException("Illegal configuration of remotereposiotory, repository server and name are required");
            }
            
            String user = Config.getConfig().get("repositoryUser");
			String pass = Config.getConfig().get("repositoryPassword");
            
            RemoteRepositoryManager rm = new RemoteRepositoryManager(repositoryServer);
                 
			if (user!=null && pass!=null) {
				logger.info("Initializing repository with password and user name");
				rm.setUsernameAndPassword(user, pass);
			} else {
				logger.info("No user information specified, no login information used.");
			}
            
            rm.initialize();
            
            Repository repo = rm.getRepository(repositoryName); 
            
            if (repo==null)
            	throw new RuntimeException("Server '" + repositoryServer + "' does not have a repository for '" + repositoryName + "'");
            return repo; 
		}		
	}
	
	
	
	/**
	 * Factory for a {@link SPARQLRepository}, repositoryType = sparql
	 * 
	 * When using default constructor, requires {@link Config#getEndpoint()}
	 * Optionally a user can be specified which is used for basic authentication.
	 * 
	 * @author as
	 */
	private static class SparqlRepositoryFactory extends RepositoryFactoryBase {

		private final String endpoint;
		private final String user;
		private final String pass;
		
		public SparqlRepositoryFactory() {
			this(Config.getConfig().getEndpoint());
		}
		
		public SparqlRepositoryFactory(String endpoint) {
			this(endpoint, null, null);
		}
		
		public SparqlRepositoryFactory(String endpoint, String user, String pass) {
			super();
			this.endpoint = endpoint;
			this.user = user;
			this.pass = pass;
		}


		@Override
		public Repository loadRepository()
		{
			if (StringUtil.isNullOrEmpty(endpoint))
				throw new IllegalArgumentException("'endpoint' property must not be null for sparql");
			SPARQLRepository repo = new SPARQLRepository(endpoint);
			if (user!=null && pass!=null) {
				logger.debug("Connecting to SPARQL Repository using basic authentication, user: " + user);			
				Map<String, String> additionalHeaders = new HashMap<String, String>();
				additionalHeaders.put("Authorization", "Basic " + Base64.encode(user + ":" + pass));
				repo.setAdditionalHttpHeaders(additionalHeaders);
			}
            return repo;
		}		
	}
	
	
	/**
	 * Factory for an Oracle {@link SPARQLRepository}, repositoryType = oracle
	 * 
	 * requires {@link Config#getEndpoint()}
	 * 
	 * @author as
	 */
	private static class OracleRepositoryFactory extends RepositoryFactoryBase {

		@Override
		public Repository loadRepository()
		{
			final String endpoint = Config.getConfig().getEndpoint();
			if (StringUtil.isNullOrEmpty(endpoint))
				throw new IllegalArgumentException("'endpoint' property must not be null or empty for oracle repository");
			
            return new SPARQLRepository(endpoint)
            {
            	public RepositoryConnection getConnection() throws RepositoryException {
            		return new OracleSPARQLConnection(this, endpoint, null);
            	}
            };
		}		
	}
	
	
	/**
	 * Factory for a FedX, repositoryType = fedx
	 * 
	 * Only available in enterprise edition.
	 * 
	 * requires {@link Config#getFedXConfig()}
	 * 
	 * @author as
	 */
	private static class FedXRepositoryFactory extends RepositoryFactoryBase {

		@Override
		public Repository loadRepository() throws Exception
		{
			// requires fiwbcom
			String fedxConfig = Config.getConfig().getFedXConfig();
						
			try {
				Method m = Class.forName("com.fluidops.iwb.federation.FedXBridge").getMethod("initializeFedX", String.class);
				return (SailRepository)m.invoke(null, fedxConfig);
			} catch(ClassNotFoundException e) {
				logger.info("FedX support only available in Information Workbench Enterprise edition");
				throw e;
			} catch (Exception e) {
				logger.error("Error while initializing fedx: " + e.getMessage());
				throw new RuntimeException("Error while initializing fedx:", e);
			}	
		}		
	}
	
	/**
	 * Factory for a generic repository, repositoryType = class=com.my.package.MyRepositoryFactory
	 * 
	 * 
	 * @author as
	 */
	private static class CustomRepositoryFactory extends RepositoryFactoryBase {

		private final String clazzString;	// class=com.my.package.MyRepositoryFactory
		
		public CustomRepositoryFactory(String clazzString)
		{
			this.clazzString = clazzString;
		}
		
		
		@Override
		@SuppressWarnings("unchecked")
		public Repository loadRepository() throws Exception
		{
			if (!clazzString.startsWith("class="))
				throw new IllegalArgumentException(
						"RepositoryType for custom factory must be 'class=com.my.package.MyRepositoryFactory'");
			
			String className = clazzString.split("=")[1];
			
			// support groovy
			DynamicScriptingSupport.installDynamicClassLoader();	
			
			
			Class<? extends RepositoryFactoryBase> factoryClass;			


			try
			{				
				factoryClass = (Class<? extends RepositoryFactoryBase>) Class
						.forName(className, true, Thread.currentThread()
								.getContextClassLoader());
			}
			catch (ClassNotFoundException e)
			{
				logger.warn("Class " + className + " cannot be found on classpath.");
				throw e;
			}

			RepositoryFactoryBase factory;
			try {
				 factory = factoryClass.newInstance();
			} catch (Exception e) 
			{
				logger.warn("Class " + factoryClass.getName() + " must implement a default constructor.");
				throw e;
			}

			return factory.loadRepository();
		}		
	}
}
