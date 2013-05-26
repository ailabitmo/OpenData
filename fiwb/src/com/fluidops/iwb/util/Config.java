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

import com.fluidops.config.ConfigDoc;
import com.fluidops.config.ConfigDoc.Category;
import com.fluidops.config.ConfigDoc.IWBCategory;
import com.fluidops.config.ConfigDoc.Type;
import com.fluidops.config.DelegateConfig;
import com.fluidops.iwb.keywordsearch.KeywordSearchAPI;
import com.fluidops.iwb.keywordsearch.SearchProviderFactory.TargetType;
import com.fluidops.util.Singleton.UpdatableSingleton;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * IWB config, leverages fbase Config via containment
 * 
 * @author aeb
 */
@SuppressWarnings(value="MS_PKGPROTECT", justification="Fields must be publicly accessible to be set from external code")
public class Config extends DelegateConfig
{
	/**
	 * default search query targets
	 */
	public static final String DEFAULT_QUERY_TARGETS = "defaultQueryTargets";
	// a couple of config parameters have different defaults across different editions;
	// they have setters that can be called from startup code to change the default
	protected static String WIKI_PAGE_LAYOUT_DEFAULT = "EntityPageWidgetContainer";	
	protected static boolean PIVOT_GOOGLE_IMAGES_DEFAULT = false;
	protected static boolean PIVOT_ACTIVE_DEFAULT = false;
	protected static String SERVLET_SECURITY_KEY_DEFAULT = "IWBServletProection";
	protected static boolean DATABASE_BOOTSTRAPPING_DEFAULT = false;
	protected static String LOGINTOUSER_MAPPING_DEFAULT = "";
	
	public Config() {
	}
	
	private static UpdatableSingleton<Config> config = new UpdatableSingleton<Config>() {
		protected Config createInstance() throws Exception {
			return new Config();
		}
	};

	public static Config getConfig()
	{
		return getConfig(false);
	}
	
	public static Config getConfig(boolean force)
	{
		return config.instance(force);
	}
	
    /**
     * user configured shutdown behaviour
     * if false, user is prompted to press a key to shutdown application (strongly recommended for running from Eclipse!)
     * if true, Information Workbench is entering endless loop, until it receives shutdown signal (Ctrl + C, shutting down system or JVM...)
     * If set explicitly to true, no proper shutdown possible while running instance from Eclipse!
     * Default is false
     */
	@ConfigDoc( name="runnningAsService", 
	        desc="Configure shutdown behavior: if true, the Information Workbench runs until it receives the shutdown signal (CTRL+C); if false, any key will trigger shutdown. Default: false",
	        category=Category.NONE, 
	        iwbCategory=IWBCategory.CORE,
	        type = Type.BOOLEAN )
    public boolean getRunningAsService()
    {
    	return delegate().getBoolean( "runningAsService", false);
    }
    
	/**
	 * Specifies, whether the RDFS vocabulary rdfs:label and rdfs:comment should be searchable. 
	 * If set to false these two properties will be omitted from keyword index creation.
	 */
	@ConfigDoc( name="makeRdfsSearchable", 
	        desc="Specifies, whether the RDFS vocabulary rdfs:label and rdfs:comment should be searchable. Default: true",
	        category=Category.NONE, 
	        iwbCategory=IWBCategory.NONE,
	        type = Type.BOOLEAN )
	public boolean makeRdfsSearchable()
	{
		return delegate().getBoolean( "makeRdfsSearchable", true);
	}

	
	/**
	 * The repository type, see {@link RepositoryFactory} for documentation
	 * @return
	 */
	@ConfigDoc( name="repositoryType", 
	        desc="Type of the underlying RDF repository (native, memory, remoterepository, owlim, oracle, sparql). Default: native",
	        category=Category.INT, 
	        iwbCategory=IWBCategory.DATABASE,
	        type = Type.STRING )
    public String getRepositoryType()
    { 
        return delegate().get( "repositoryType", "native");
    }


    /**
     * Address of repository server for remote repository
     * 
     * @return
     */
	@ConfigDoc( name="repositoryServer", 
            desc="Repository server to be used for RemoteRepository",
            category=Category.INT,
            iwbCategory=IWBCategory.DATABASE,
            type = Type.STRING )
    public String getRepositoryServer()
    { 
        return delegate().get( "repositoryServer", "");
    }

	/**
     * Directory or repository of remote repository
     * 
     * @return
     */
	
	
	@ConfigDoc( name="repositoryName", 
            desc="Directory name or repository name for RemoteRepository. Default: dbmodel",
            category=Category.NONE,
            iwbCategory=IWBCategory.DATABASE,
            type = Type.STRING )
    public String getRepositoryName()
    { 
        return delegate().get( "repositoryName", "dbmodel");
    }

    
	// TODO maybe remove and use repository server?
	@ConfigDoc( name="endpoint", 
            desc="Endpoint location of SPARQL endpoint",
            category=Category.NONE,
            iwbCategory=IWBCategory.DATABASE,
            type = Type.STRING )
    public String getEndpoint()
    { 
        return delegate().get( "endpoint");
    }
    
	@ConfigDoc( name="publicSparqlEndpointEnabled", 
            desc="Define whether the public SPARQL endpoint is enabled. Default: true",
            category=Category.NONE,
            iwbCategory=IWBCategory.DATABASE,
            type = Type.BOOLEAN )
    public boolean getPublicSparqlEndpointEnabled()
    { 
        return delegate().getBoolean("publicSparqlEndpointEnabled", true);
    }
	
    /**
     * Store the wiki pages in a database (instead of file system)
     */
    @ConfigDoc( name="storeWikiInDatabase", 
            desc="Store the wiki in a database (true) or in file system. Default: false",
            category=Category.INT, 
            iwbCategory=IWBCategory.DATABASE,
            type = Type.BOOLEAN )
    public boolean getStoreWikiInDatabase()
    { 
        return delegate().getBoolean( "storeWikiInDatabase", false);
    }
    
    /**
     * Compress wiki in the database
     */
    @ConfigDoc( name="compressWikiInDatabase", 
            desc="Enable/disable compression of wiki in the database. Default: false",
            category=Category.NONE, 
            iwbCategory=IWBCategory.DATABASE,     
            type = Type.BOOLEAN )
    public boolean getCompressWikiInDatabase()
    { 
        return delegate().getBoolean( "compressWikiInDatabase", false);
    }
    
    /**
     * whether to support autocompletion
     */
    @ConfigDoc( name="autocompletion", 
            desc="Enable/disable auto-completion of keywords in the search field. Default: false",
            category=Category.NONE, 
            iwbCategory=IWBCategory.CORE,
            type = Type.BOOLEAN )
    public boolean getAutocompletion()
    { 
        return delegate().getBoolean("autocompletion", false);
    }
    
    /**
     * should autocompletion be ranked 
     */
    @ConfigDoc( name="rankedCompletion", 
            desc="Specifies if auto-completion should be ranked or not. Default: true",
            category=Category.NONE, 
            iwbCategory=IWBCategory.NONE,
            type = Type.BOOLEAN )
    public boolean getRankedCompletion()
    { 
        return delegate().getBoolean( "rankedCompletion", true);
    }
    
    /**
     * how many suggestions on keyword-Level should be presented
     * @return
     */
    @ConfigDoc( name="numberKeywordSuggestions", 
            desc="Specifies how many suggestions for search keywords the autocompletion should present. Default: 15",
            category=Category.NONE,      
            iwbCategory=IWBCategory.NONE,
            type = Type.INTEGER)
	public int getNumberKeywordSuggestions( )
	{
		return delegate().getInt("numberKeywordSuggestions", 15);
	}
   
   /**
    * parameter for ranking of keyword-suggestions, ranking off as default
    * @return
    */
    @ConfigDoc( name="minDocFreq", 
            desc="Speficies how frequent the suggested keywords should be, i.e. in how many documents they should occur at least. Default: 5",
            category=Category.NONE, 
            iwbCategory=IWBCategory.NONE,
            type = Type.INTEGER)
	public int getMinDocFreq()
	{
	    return delegate().getInt( "minDocFreq", 5);
	}
  
    /**
     * Pivot Parameters
     */
    @ConfigDoc( name="pivotGoogleImages", 
            desc="Retrieve pivot images from google if not specified",
            category=Category.INT,
            iwbCategory=IWBCategory.PIVOT,
            type = Type.BOOLEAN)
    public boolean getPivotGoogleImages() 
    {
        return delegate().getBoolean("pivotGoogleImages", PIVOT_GOOGLE_IMAGES_DEFAULT);
    }
    
    @ConfigDoc( name="analyzeMode", 
            desc="Enables monitoring of database connections and repository access. Default: false",
            category=Category.DEBUG,
            iwbCategory=IWBCategory.DEBUG,
            type = Type.BOOLEAN)
    public boolean getAnalyzeMode() 
    {
    	return delegate().getBoolean("analyzeMode", false);
    }
   
	@ConfigDoc( name="webdavDirectory", 
			desc="Location where uploaded files are stored. Must also be changed in web.xml: <param-name>rootpath</param-name>. Default: upload", 
			category=Category.INT,
			iwbCategory=IWBCategory.NONE,
			type = Type.STRING )
    public String webdavDirectory()
    { 
        return delegate().get( "webdavDirectory", "upload");
    }
    
	@ConfigDoc( name="uploadFileClass", 
			desc="File repository driver class to be used when uploading content", 
			category=Category.INT,
			iwbCategory=IWBCategory.NONE,
			type = Type.STRING )
    public String uploadFileClass()
    { 
        return delegate().get( "uploadFileClass", "com.fluidops.iwb.cms.fs.LocalFile");
    }
	
	@ConfigDoc( name="uploadLocation", 
			desc="Location / URL of the file repository to be used when uploading content", 
			category=Category.INT,
			iwbCategory=IWBCategory.NONE,
			type = Type.STRING )
    public String uploadLocation()
    { 
        return delegate().get( "uploadLocation" );
    }
	
	@ConfigDoc( name="uploadUsername", 
			desc="Username for the file repository to be used when uploading content", 
			category=Category.INT,
			iwbCategory=IWBCategory.NONE,
			type = Type.STRING )
    public String uploadUsername()
    { 
        return delegate().get( "uploadUsername" );
    }
	
	@ConfigDoc( name="uploadPassword", 
			desc="Password for the file repository to be used when uploading content", 
			category=Category.INT,
			iwbCategory=IWBCategory.NONE,
			type = Type.PASSWORD )
    public String uploadPassword()
    { 
        String res = delegate().get( "uploadPassword" );
		com.fluidops.util.user.PwdSafe.Credentials c = new com.fluidops.util.user.PwdSafe.Credentials();
		c.password = res;
		if ( res == null )
			return null;
		String plain = c.getPlainPassword();
		return plain;
    }
	
    // TODO: is this still used? - Referenced in LayouterImpl. / also in TC 692
    public String getWikiPageLayout()
    {                                                                                                            
        return delegate().get( "wikiPageLayout", WIKI_PAGE_LAYOUT_DEFAULT);
    }
    
    /**
     * RMI port for configuration of the RMI endpoint, needed for configuration of two instances
     * @return
     */
    @ConfigDoc( name="RMIPort", 
            desc="RMI port of the endpoint binding. Default: 1099",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.INTEGER)
    public int getRMIPort()
    {                                                                                                            
        return delegate().getInt( "RMIPort", 1099);
    }
    
    /**
     * If set to true, literals are clickable in Table View. Following a link to a literal value,
     * will display a table listing all resources and predicates to which this specific literal is connected.
     * @return
     */
    @ConfigDoc( name="linkLiterals", 
            desc="If set to true, literals are clickable in Table View. Default: true",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.BOOLEAN)
    public boolean getLinkLiterals() 
    {
    	return delegate().getBoolean("linkLiterals", true);
    }
    
    @ConfigDoc( name="iwbAuthenticationScheme", 
            desc="Authentication scheme for accessing the Information workbench: authentication required (auth) or not (noauth). Default: noauth",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRING)
    public String iwbAuthenticationScheme() 
    {
    	// there are two authentication schemes:
    	// - auth -> authentication required
    	// - noauth -> no authentication required
    	return delegate().get("iwbAuthenticationScheme", "noauth");
    }
    
    
    /**
     * Defines the structure of URLs of entity pages entered in the browser 
     * (structure of URLs: [server Address][urlMapping][Entities name] ). 
     * The mapping additionally has to be adapted in the file 
     * fiwb/wepapps/ROOT/WEB-INF/web.xml and set from the default value 
     * to the respective configuration value.
     * 
     * @return
     */
    @ConfigDoc( name="urlMapping", 
            desc="Defines the structure of URLs of entity pages entered in the browser. Default: /resource/",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRING)
    public String getUrlMapping()
    {
    	return delegate().get("urlMapping", "/resource/");
    }
    
    @ConfigDoc( name="customEcmMapping", 
            desc="Custom mapping triggered after eCM provider run",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.STRING)
    public String getCustomEcmMapping()
    {
    	return delegate().get("customEcmMapping", null);
    }
    
    @ConfigDoc( name="pivotActive", 
            desc="Pivot active, only available in Enterprise Edition",
            category=Category.NONE,
            iwbCategory=IWBCategory.PIVOT,
            type = Type.BOOLEAN)
    public boolean getPivotActive()
    {
    	return delegate().getBoolean("pivotActive", PIVOT_ACTIVE_DEFAULT);
    }
    
    @ConfigDoc( name="internalHostUrl", 
            desc="URL locating the server-host that can be used by the server isself. Only set, if this differs from host-URL clients are using. Must not contain trailing '/', e.g. 'http://server.name:188'",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRING)
    public String getInternalHostUrl()
    {
        return delegate().get("internalHostUrl","");
    }
    
    @ConfigDoc( name="displayDatatypeDropdown", 
            desc="Display dropdown for datatype selection. Default: false",
            category=Category.INT,
            iwbCategory=IWBCategory.CORE,
            type = Type.BOOLEAN)
    public boolean displayDatatypeDropdown() 
    {
        return delegate().getBoolean("displayDatatypeDropdown", false);
    }
    
    @ConfigDoc( name="googleAnalytics", 
            desc="Google analytics.",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.STRING)
    public String getGoogleAnalytics()
    {
    	return delegate().get("googleAnalytics", null);
    }
    
    @ConfigDoc( name="widgetSelector", 
            desc="Widget Selector. Default: com.fluidops.iwb.api.WidgetSelectorImpl",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.STRING)
	public String getWidgetSelector( )
	{
		return delegate().get("widgetSelector", "com.fluidops.iwb.api.WidgetSelectorImpl");
	}
    
    // TODO as: avoid this
    /**
     * Returns true if federated mode is enabled, i.e. repositoryType = fedx
     * 
     * @return
     * 		true or false
     */
	public boolean isFederation( )
	{
		return getRepositoryType().equalsIgnoreCase("fedx");
	}
	
	/**
	 * Location of the fedx configuration, is only to be set if repository type = fedx
	 * @return
	 */
	@ConfigDoc( name="fedxConfig", 
            desc="Location of the fedx configuration for federated mode. Default: config/fedx/fedx_config.prop",
            category=Category.NONE,
            iwbCategory=IWBCategory.DATABASE,
            type = Type.STRING)
	public String getFedXConfig() {
		return delegate().get("fedxConfig", "config/fedx/fedx_config.prop");
	}
    
    /**
     * valid values are "type", "types", and "types-recursive";
     * @return
     */
	@ConfigDoc( name="wikiIncludeScheme", 
            desc="Specifies how a template is selected for a resource which does not have wiki page associated. Valid values are type, types, and types-recursive, mostSpecificTypes. Default: types",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRING)
    public String getWikiIncludeScheme() 
    {
        String dflt = "types";
        String ret = delegate().get("wikiIncludeScheme", dflt);
        if (ret==null)
            return dflt;
        
        if (!(ret.equals("type") || ret.equals("types-recursive") || ret.equals("mostSpecificTypes")))
            return dflt;
        else
            return ret;
    }
    
	/**
	 * returns the working directory where all the resources like rdf-stores, wiki, config etc are
	 * located
	 */
	@ConfigDoc( name="workingDir", 
            desc="returns the working directory where all the resources located. Default: <empty>",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRING)
    public String getWorkingDir() 
    {
    	return delegate().get("workingDir", "");
    }
    
	/**
	 * decides whether deletion and manipulation of existing triples is allowed, if the editability
	 * flag is not explicitly set for the given context (i.e. if the editability flag is not
	 * persisted in the database).
	 */
	@ConfigDoc( name="contextsEditableDefault", 
            desc="decides whether deletion and manipulation of existing triples is allowed, if the editability " +
            		"flag is not explicitly set for the given context (i.e. if the editability flag is not " +
            		"persisted in the database). Default: false",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.BOOLEAN)
	public boolean getContextsEditableDefault() 
	{
		return  delegate().getBoolean("contextsEditableDefault", false);
	}
	
    @ConfigDoc( name="queryTimeout", 
            desc="Timeout for SPARQL queries in seconds. Default: 30",
            category=Category.INT,
            iwbCategory=IWBCategory.CORE,
            type = Type.INTEGER)
    public int queryTimeout() 
    {
        return delegate().getInt("queryTimeout", 30);
    }

    /**
     *  Whether editorial workflow show be enabled 
     * 
     * @return
     */
	@ConfigDoc( name="editorialWorkflow", 
            desc="Whether editorial workflow show be enabled. Default: false",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.BOOLEAN)
    public boolean getEditorialWorkflow()
    {
        return delegate().getBoolean("editorialWorkflow", false);
    }
	
    /**
     *  Define deafult color scheme by setting the lightest and the darkest color shade in RGB format
     * 
     * @return
     */
	
    @ConfigDoc( name="chartColorLow", 
            desc="The lightest color to be used in charts (comma separated RGB values). Default: 200,230,255",
            category=Category.INT,
            iwbCategory=IWBCategory.APPEARANCE,
            type = Type.INTEGERARRAY)
    public Integer[] getChartColorLow()
    {

        return delegate().getIntArray("chartColorLow", new Integer[] {200,230,255}); 

    }
    
    @ConfigDoc( name="chartColorHigh", 
            desc="The darkest color to be used in charts (comma separated RGB values). Default: 10,60,255",
            category=Category.INT,
            iwbCategory=IWBCategory.APPEARANCE,
            type = Type.INTEGERARRAY)
    public Integer[] getChartColorHigh()
    {

        return delegate().getIntArray("chartColorHigh", new Integer[] {10,60,255}); 

    }
	
	/**
	 * Decide whether data from dbBootstrap directory is loaded or not. If set to true, data is
	 * loaded after every clean on the project.
	 * 
	 * @return true or false
	 */
	@ConfigDoc( name="loadDBBootstrap", 
            desc="Load data from dbBootstrap directory",
            category=Category.NONE,
            iwbCategory=IWBCategory.DATABASE,
            type = Type.BOOLEAN)
    public boolean loadDBBootstrap()
    {
    	return delegate().getBoolean("loadDBBootstrap",DATABASE_BOOTSTRAPPING_DEFAULT);
    }
    
    /**
     * Comma separated list of all properties to be treated as label
     */
	@ConfigDoc( name="labelProperties", 
            desc="Comma separated list of all properties to be treated as label",
            category=Category.NONE,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRINGARRAY)
    public String[] getLabelProperties()
    {
    	String[] labelProperties = delegate().get("labelProperties", "rdfs:label").split(","); 
    	
    	return labelProperties;
    }


	/**
	 * The target repository name to which changes in a editorial workflow are published to
	 * 
	 * @return name of the repository
	 */
	@ConfigDoc( name="targetRepositoryName", 
            desc="Target repository name to which changes in a editorial workflow are published to",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.STRING)
	public String getTargetRepositoryName( )
	{
		return delegate().get("targetRepositoryName");
	}

	/**
	 * The target repository name to which changes in a editorial workflow are published to
	 * 
	 * @return name of the repository
	 */
	@ConfigDoc( name="targetRepositoryServer", 
            desc="Target repository server to which changes in a editorial workflow are published to",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.STRING)
	public String getTargetRepositoryServer( )
	{
		return delegate().get("targetRepositoryServer");
	}

    /**
     * The target repository name to which changes in a editorial workflow are published to 
     * @return name of the repository
     */
	@ConfigDoc( name="targetContext", 
            desc="Target repository context to which changes in a editorial workflow are published to",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.STRING)
    public String getTargetContext()
    {
        return  delegate().get("targetContext", "");
    }
	
    /**
     * Returns the set of indices to be built as a comma separated specifier
     * string as accepted by Sesame.
     * 
     * @return Index configuration string
     */
    @ConfigDoc(name = "nativeStoreIndices",
            desc = "Configures the indices to be built in the Sesame native triple store. Indices need to be specified as a comma-separated list of index specifiers. Must be set before data is loaded into the store. Default: 'spoc,psoc'",
            category = Category.NONE,
            iwbCategory = IWBCategory.DATABASE,
            type = Type.STRING)
    public String getNativeStoreIndices()
    {
        return delegate().get("nativeStoreIndices", "spoc,psoc");
    }
    
    /**
     * Configures whether the full text index is enabled.
     */
	@ConfigDoc( name="enableFulltextIndex",
            desc="Configures whether the full text index is enabled. Default: true",
            category=Category.INT,
            iwbCategory=IWBCategory.CORE,
            type = Type.BOOLEAN)
	public boolean getEnableFulltextIndex() 
	{
		return delegate().getBoolean("enableFulltextIndex", true);
	}
	
	/**
	 * Returns the query skeleton used for keyword search over structured data. If result clustering
	 * is enabled, this skeleton must contain the variable bindings "Subject",
	 * "Property", "Value", "Type". The query skeleton may contain the place 
	 * holder <code>??</code>, which is replaced with the user input.
	 * 
	 * Use the following query skeleton for OWLIM:
	 * 
	 * <code>
	 * PREFIX luc: <http://www.ontotext.com/owlim/lucene#> 
	 * PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
	 * SELECT DISTINCT ?Subject ?Property ?Value ?Type 
	 * WHERE { 
	 * ?Value luc:luceneIndex "??" . 
	 * ?Subject ?Property ?Value . 
	 * OPTIONAL { ?Subject rdf:type ?Type . } 
	 * } LIMIT 1000
	 * </code>
	 * 
	 * @see Config#getClusterSearchResult()
	 * @return Query skeleton
	 */
	@ConfigDoc( name="keywordQuerySkeleton",
            desc="Configures the query skeleton used for keyword search over structured data",
            category=Category.INT,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRING)
	public String getKeywordQuerySkeleton() 
	{
		return delegate().get("keywordQuerySkeleton", KeywordSearchAPI.defaultQuerySkeleton);
	}
	
	/**
	 * Similarly to getKeywordQuerySkeleton, returns the query skeleton used for keyword search over wiki pages. If result clustering
	 * is enabled, this skeleton must contain the variable bindings "Subject",
	 * "Property", "Value", "Type". The query skeleton may contain the place 
	 * holder <code>??</code>, which is replaced with the user input.
	 * 
	 * 
	 * @see Config#getClusterSearchResult()
	 * @return Query skeleton
	 */
	@ConfigDoc( name="wikiQuerySkeleton",
            desc="Configures the query skeleton used for keyword search over wiki pages",
            category=Category.INT,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRING)
	public String getWikiQuerySkeleton() 
	{
		return delegate().get("wikiQuerySkeleton", KeywordSearchAPI.defaultWikiQuerySkeleton);
	}

	/**
     * Configures whether the search widgets should search over wiki pages by default.
     * 
     * @return
     */
	@ConfigDoc( name=DEFAULT_QUERY_TARGETS,
            desc="Specifies the default search targets (as a comma-separated list): RDF for structured data, WIKI for wiki pages, or the fully qualified name of a custom SearchProvider implementation. Default: RDF, WIKI",
            category=Category.INT,
            iwbCategory=IWBCategory.CORE,
            type = Type.STRINGARRAY)
	public String[] getDefaultQueryTargets() {
		return delegate().get(DEFAULT_QUERY_TARGETS, TargetType.RDF + "," +TargetType.WIKI).split("\\s*,\\s*");
	}
	
	
	public boolean getUseMySQL()
	{
		return delegate().getBoolean("useMySQL", false);
	}
	
	public String getMySQLServeradress( )
	{
		return delegate().get("mySQLServername", "localhost:3306");
	}

	public String getMySQLUser( )
	{
		return delegate().get("mySQLUser", "root");
	}

	public String getMySQLPassword( )
	{
		return delegate().get("mySQLPassword", "");
	}

	/**
	 * Returns whether clustering of search results is enabled. If it is
	 * enabled, the keyword query skeleton must contain the variable bindings
	 * "Subject", "Property", "Value", "Type".
	 * 
	 * @see Config#getKeywordQuerySkeleton()
	 */
	public boolean getClusterSearchResult( )
	{
		return delegate().getBoolean("clusterSearchResult", true);
	}
	
	@ConfigDoc( name="luxidEnabled", 
            desc="Enable/disable extraction of semantic data from documents using the Luxid webservice",
            category=Category.NONE,
            iwbCategory=IWBCategory.CMS,
            type = Type.BOOLEAN)
    public boolean getLuxidEnabled()
    {
    	return delegate().getBoolean("luxidEnabled", false);
    }
    
	@ConfigDoc( name="luxidServerURL", 
            desc="URL of the Luxid webservice, e.g. http://host//LuxidWS/services/Annotation",
            category=Category.NONE,
            iwbCategory=IWBCategory.CMS,
            type = Type.STRING)
    public String getLuxidServerURL()
    {
    	return delegate().get("luxidServerURL", "");
    }
    
	@ConfigDoc( name="luxidUserName", 
			desc="Username for the Luxid webservice",
			category=Category.NONE,
			iwbCategory=IWBCategory.CMS,
			type = Type.STRING)
    public String getLuxidUserName()
    {
    	return delegate().get("luxidUserName", "");
    }
	
	@ConfigDoc( name="luxidPassword", 
			desc="Password for the Luxid webservice",
			category=Category.NONE,
			iwbCategory=IWBCategory.CMS,
			type = Type.STRING)
    public String getLuxidPassword()
    {
    	return delegate().get("luxidPassword", "");
    }
	
	@ConfigDoc( name="openUpMode", 
            desc="Enable/disable extraction of semantic data from documents using OpenUp webservice (disabled, demo (limited demo service), enabled). Default: disabled",
            category=Category.NONE,
            iwbCategory=IWBCategory.CMS,
            type = Type.STRING)
    public String getOpenUpMode()
    {
    	return delegate().get("openUpMode", "disabled");
    }
	
	@ConfigDoc( name="openUpUrl", 
            desc="URL of OpenUp webservice, e.g. \"http://openup.tso.co.uk/des/enriched-text\"",
            category=Category.NONE,
            iwbCategory=IWBCategory.CMS,
            type = Type.STRING)
	public String getOpenUpUrl()
	{
		return delegate().get("openUpUrl", "http://openup.tso.co.uk/des/enriched-text");
	}
    
    public String getServletSecurityKey()
    {
        return delegate().get("servletSecurityKey",SERVLET_SECURITY_KEY_DEFAULT);
    }    
    
    public Boolean getOpenSparqlServletForLocalAccess()
    {
    	return delegate().getBoolean("openSparqlServletForLocalAccess", false);
    }
    
	/**
	 * Size of the request queue hold by the CommunicationService. As a guideling,
	 * a queue size of 100000 requires about 1GB of main memory.
	 */
	@ConfigDoc( name="communicationServiceQueueSize", 
	        desc="Size of the CommunicationService queue",
	        category=Category.INT, 
	        iwbCategory=IWBCategory.NONE,
	        type = Type.INTEGER )
    public int getCommunicationServiceQueueSize()
    {
    	return delegate().getInt("communicationServiceQueueSize", 100000);
    }
	
	@ConfigDoc( name="annotationCustomizerClassName", 
			desc="Name of class for OWL Annotation customization. Must be a subclass of OWLAnnotationCustomizer", 
			category=Category.INT, 
			type = Type.STRING )
	public String annotationCustomizerClassName()
	{
       return delegate().get("annotationCustomizerClassName", "");
	}
        
	 /**
	  * API key for LastFM API access, required if LastFM Cache is accessed.
	  * 
	  * @return
	  */
	@ConfigDoc( name="lastFMKey", 
            desc="API key for LastFM API access, required if LastFM Cache is accessed, e.g. from LastFMWidget",
            category=Category.NONE,
            iwbCategory=IWBCategory.NONE,
            type = Type.STRING)
	 public String getLastFMKey() 
	 {
		 return delegate().get("lastFMKey");
	 }
	 
	 /**
	  * Tagging of Wiki Revisions
	  */
	 public Boolean getWikiTaggingEnabled()
	 {
		 if (getStoreWikiInDatabase())
			 return false; // not supported for now
		 
		 return delegate().getBoolean("wikiTaggingEnabled", false);
	 }
	 
	 /**
	  * SPARQL query for matching the login string to a user URI.
	  */
	 public String getLoginToUserMapping()
	 {
		 return delegate().get("loginToUserMapping",LOGINTOUSER_MAPPING_DEFAULT);
	 } 
	 
	 // setters for default values
	 @SuppressWarnings(
			 value = { "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" }, 
			 justification = "The default is only set once during startup")
	 public void setWikiPageLayoutDefault(String wikiPageLayout)
	 {
		 WIKI_PAGE_LAYOUT_DEFAULT = wikiPageLayout;
	 }
	 
	 @SuppressWarnings(
			 value = { "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" }, 
			 justification = "The default is only set once during startup")
	 public void setPivotGoogleImagesDefault(boolean pivotGoogleImages)
	 {
		 PIVOT_GOOGLE_IMAGES_DEFAULT = pivotGoogleImages;
	 }

	 @SuppressWarnings(
			 value = { "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" }, 
			 justification = "The default is only set once during startup")
	 public void setPivotActiveDefault(boolean pivotActive)
	 {
		 PIVOT_ACTIVE_DEFAULT = pivotActive;
	 }
	 
	 @SuppressWarnings(
			 value = { "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" }, 
			 justification = "The default is only set once during startup")
	 public void setServletSecurityKeyDefault(String servletSecurityKey)
	 {
		 SERVLET_SECURITY_KEY_DEFAULT = servletSecurityKey;
	 }
	 
	 @SuppressWarnings(
			 value = { "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" }, 
			 justification = "The default is only set once during startup")
	 public void setDatabaseBootstrappingDefault(boolean databaseBootstrapping)
     {
         DATABASE_BOOTSTRAPPING_DEFAULT = databaseBootstrapping;
     }
	 
	 @SuppressWarnings(
			 value = { "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" }, 
			 justification = "The default is only set once during startup")
	 public void setLoginToUserMappingDefault(String login2UserMapping)
	 {
		 LOGINTOUSER_MAPPING_DEFAULT = login2UserMapping;
	 }
}
