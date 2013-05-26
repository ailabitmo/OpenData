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

package com.fluidops.iwb.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.model.Vocabulary.DCAT;
import com.fluidops.iwb.model.Vocabulary.DCTERMS;
import com.fluidops.iwb.model.Vocabulary.FOAF;
import com.fluidops.util.GenUtil;

/**
 * @author marlon.braun
 *
 */
public class CkanProvider extends AbstractFlexProvider<CkanProvider.Config>
{
    private static final long serialVersionUID = -1736444816801600475L;
    
    private static final Logger logger = Logger.getLogger(CkanProvider.class
            .getName());
    
    /**
     * Query for extracting properties of a dataset.
     */
    transient private static final String propertyQuery = "CONSTRUCT { " +
        "?dataset ?p ?o }" +
        "WHERE {" +
        "?dataset ?p ?o ." +
        "MINUS { ?dataset ?p ?o FILTER isBlank(?o) } }";

    /**
     * Query for extracting all distributions linked to a dataset.
     */
    transient private static final String distributionQuery = "SELECT * { " +
        "?dataset " + ProviderUtils.uriToQueryString(DCAT.HAS_DISTRIBUTION) + " ?distribution . " +
        "?distribution " + ProviderUtils.uriToQueryString(DCAT.ACCESSURL) + " ?accessURL . " +
        "OPTIONAL { ?distribution " + ProviderUtils.uriToQueryString(DCTERMS.TITLE) + " ?title } . " +
        "?distribution " + ProviderUtils.uriToQueryString(DCTERMS.FORMAT) + " ?formatNode . " +
        "?formatNode " + ProviderUtils.uriToQueryString(RDFS.LABEL) + " ?format }";

    /**
     * Query for extracting all {@link DCTERMS#RELATION}. Objects are all blank
     * nodes whose outgoing edges need to be properly converted.
     */
    transient private static final String relationQuery = "SELECT * { " +
        "?dataset " + ProviderUtils.uriToQueryString(DCTERMS.RELATION) + " ?relationNode . " +
        "?relationNode " + ProviderUtils.uriToQueryString(RDFS.LABEL) + " ?label . " +
        "?relationNode " + ProviderUtils.uriToQueryString(RDF.VALUE) + " ?value }";

    /**
     * Extracts the names of all {@link DCTERMS#CREATOR}s and
     * {@link DCTERMS#CONTRIBUTOR}s.
     */
    transient private static final String creatorContributorQuery = "SELECT * { " +
    	"?dataset ?creatorContributor ?bNode . " +
    	"?bNode " + ProviderUtils.uriToQueryString(FOAF.NAME) + " ?name . " +
    	"OPTIONAL { ?bNode " + ProviderUtils.uriToQueryString(FOAF.MBOX) + " ?mbox }}";
    
    public static class Config implements Serializable
    {
        private static final long serialVersionUID = 4453613533720534245L;

        @ParameterConfigDoc(desc = "URL of the CKAN registry (http://ckan.net/api/rest/group/lodcloud)", required = true)
        public String location;
    }

    @Override
    public Class<? extends Config> getConfigClass()
    {
        return CkanProvider.Config.class;
    }
    
    /**
     * Ckan Ontology
     * 
     * @author marlon.braun
     */
    public static class CKAN
    {
        public static final String NAMESPACE = "http://www.ckan.net/group/";
        
        public static final URI CKAN_CATALOG = ProviderUtils.objectToURIInNamespace(NAMESPACE, "lodcloud");
        public static final Literal CKAN_CATALOG_LABEL = ProviderUtils.objectToTypedLiteral("LOD Cloud");
    }

    @Override
    public void gather(List<Statement> res) throws Exception
    {
        // Read CKAN location and establish connection
        URL registryUrl = new URL(config.location);
        HttpURLConnection registryConnection = (HttpURLConnection) registryUrl
                .openConnection();
        registryConnection.setRequestMethod("GET");

        // Check if connection to CKAN could be established
        if (registryConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
        {
            String msg = String
                    .format("Connection to the CKAN registry could not be established. (%s, %s)",
                            registryConnection.getResponseCode(),
                            registryConnection.getResponseMessage());
            logger.info(msg);
            throw new IllegalStateException(msg);
        }
        logger.trace("Connection to CKAN established successfully.");

        String siteContent = GenUtil.readUrl(registryConnection
                .getInputStream());

        JSONObject groupAsJson = null;
        JSONArray packageListJsonArray = null;
        try
        {
            groupAsJson = new JSONObject(new JSONTokener(siteContent));
            packageListJsonArray = groupAsJson.getJSONArray("packages");
        }
        catch (JSONException e)
        {
            String msg = String
                    .format("Returned content %s is not valid JSON. Check if the registry URL is valid.",
                            siteContent);
            logger.debug(msg);
            throw new IllegalStateException(msg);
        }
        logger.trace("Extracted JSON from CKAN successfully");

        // Create metadata about LOD catalog
        res.add(ProviderUtils.createStatement(CKAN.CKAN_CATALOG,
                RDF.TYPE, Vocabulary.DCAT.CATALOG));
        res.add(ProviderUtils.createStatement(CKAN.CKAN_CATALOG,
                RDFS.LABEL, CKAN.CKAN_CATALOG_LABEL));

        // Extract metadata for individual data sets listed in CKAN
        MultiThreadedHttpConnectionManager connectionManager=null;
        ExecutorService pool=null;
        try {
        	pool = Executors.newFixedThreadPool(10);
	        connectionManager = new MultiThreadedHttpConnectionManager();
	        HttpClient client = new HttpClient(connectionManager);
	        	
	        List<Statement> synchedList = Collections.synchronizedList(res);
	        for (int i = 0; i < packageListJsonArray.length(); i++)
	        {
	            String host = "http://www.ckan.net/package/"
	                    + packageListJsonArray.get(i).toString();
	            String baseUri = findBaseUri("http://www.ckan.net/api/rest/package/"
	                    + packageListJsonArray.get(i).toString());
	            baseUri = (baseUri == null) ? host : baseUri;
	            pool.execute(new MetadataReader(client, host, baseUri,
	                    CKAN.CKAN_CATALOG, synchedList));
	        }
        } finally {
        	if (pool!=null) {
	        	pool.shutdown();
	            pool.awaitTermination(4, TimeUnit.HOURS);
        	}
        	if (connectionManager!=null)
        		connectionManager.shutdown();
        }        
    }
    
    /**
     * Task class for the worker threads reading the metadata. Writes the
     * extracted data into the statements list of the provider).
     */
    private class MetadataReader implements Runnable {
        private String url;
        private HttpClient client;
        private String baseUri;
        private URI catalog;
        private List<Statement> stmts;

        public MetadataReader(HttpClient httpClient, String packageURL,
                String baseUri, URI cata, List<Statement> stmts) {
            this.url = packageURL;
            this.client = httpClient;
            this.baseUri = baseUri;
            this.catalog = cata;
            this.stmts = stmts;
        }

        @Override
        public void run() {
            // Query the new RDF-metadata CKAN integration via <URL> + '.rdf'
            // per dataset.
            logger.debug("Processing dataset " + url);

            HttpMethod method = new GetMethod(this.url + ".rdf");
            method.setFollowRedirects(true);

            InputStream response = null;
            Repository repo = null;
            RepositoryConnection conn = null;
            
            try
            {
                repo = new SailRepository(new MemoryStore());
                repo.initialize();
                conn = repo.getConnection();

                int status = client.executeMethod(method);
                if (status == HttpStatus.OK_200)
                {

                    // Read rdf data and add to memory store
                    response = method.getResponseBodyAsStream();
                    conn.add(response, baseUri.toString(), RDFFormat.RDFXML,
                            ProviderUtils.objectAsUri(url.toString()));

                    // Connect dataset to LOD catalog
                    URI subject = ProviderUtils.objectAsUri(this.url.replace(
                            "www.ckan.net/package", "datahub.io/dataset"));
                    stmts.add(ProviderUtils.createStatement(catalog,
                            Vocabulary.DCAT.HAS_DATASET, subject));

                    // Extract metadata and write to list
                    stmts.addAll(getProperties(subject, conn));
                    stmts.addAll(getDistributions(subject, conn));
                    stmts.addAll(getRelations(subject, conn));
                    stmts.addAll(getAuthor(subject, DCTERMS.CREATOR, conn));
                    stmts.addAll(getAuthor(subject, DCTERMS.CONTRIBUTOR, conn));
                }
                else
                {
                    // do not abort here, as this affects only a single data source
                    logger.info(String.format("Bad response from server, cannot obtain metadataset (status %s, Url: %s)",status,url));
                }
            }
            catch (Exception e)
            {
                // do not abort here, as this affects only a single data source
                logger.info("Exception in extractor thread: " + e.getMessage());
                logger.debug("Details:", e);
            }
            finally
            {
                IOUtils.closeQuietly(response);
                method.releaseConnection();
                ReadWriteDataManagerImpl.closeQuietly(conn);
                ReadWriteDataManagerImpl.shutdownQuietly(repo);
                
                logger.trace("Finished processing dataset " + url);
            }
        }
    }
    
    /**
     * Retrieves the base URI from a given host. Returns null if retrieval
     * fails.
     */
    private String findBaseUri(String host)
    {
        HttpURLConnection conn = null;
        
        // Read the base URI from the JSON, located in the "url" key-value pair
        try
        {
            URL url = new URL(host);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                String content = GenUtil.readUrl(conn.getInputStream());
                Object ob = getJson(content);

                String baseUrl = ((JSONObject) ob).getString("url");

                return baseUrl == null ? null : baseUrl;
            }

            conn.disconnect();
        }
        catch (MalformedURLException e)
        {
            logger.warn("Supplied host is not a valid URL: " + host);
            // ignore warning (affects only a single dataset)
        }
        catch (IOException e)
        {
            logger.warn("IOException while retrieving base URI: " + host);
            // ignore warning (affects only a single dataset)
        }
        catch (JSONException e)
        {
            logger.warn(String.format("No base URL found for: %s!%n%s", host,
                    e.getMessage()));
            // ignore warning (affects only a single dataset)
        }

        return null;
    }
    
    /**
     * Wraps a string into a JSON object. Returns null if content is not a valid
     * JSON object.
     */
    private static Object getJson(String content)
    {
        JSONTokener tokener = new JSONTokener(content);

        try
        {
            JSONObject json = new JSONObject(tokener);
            return json;
        }
        catch (Exception e)
        {
            logger.debug(e);
        }

        try
        {
            JSONArray jsonArray = new JSONArray(tokener);
            return jsonArray;
        }
        catch (Exception e)
        {
            logger.debug(e);
        }

        return null;
    }

    /**
     * Extracts all metadata that exists in the form
     * <p>
     * ?dataset ?property ?value
     * <p>
     * where value is not a blank node.
     * 
     * @param dataset
     *            {@link URI} of the dataset whose metadata is being extracted.
     * @param conn
     *            {@link RepositoryConnection} to the {@link MemoryStore} in
     *            which the metadata resides.
     * @return The metadata extracted as RDF {@link Statement}s.
     * @throws Exception
     *             Any exception occuring during the extraction process
     */
    private List<Statement> getProperties(URI dataset, RepositoryConnection conn) throws Exception
    {
        List<Statement> stmts = new ArrayList<Statement>();

        GraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL, propertyQuery);
        query.setBinding("dataset", dataset);

        GraphQueryResult res = query.evaluate();
        while (res.hasNext())
            stmts.add(res.next());
        res.close();
        
        return stmts;
    }
    
    /**
     * Extracts all distributions linked to a dataset and writes them to RDF.
     * 
     * @param dataset
     *            {@link URI} of the dataset whose metadata is being extracted.
     * @param conn
     *            {@link RepositoryConnection} to the {@link MemoryStore} in
     *            which the metadata resides.
     * @return The metadata about the distributions extracted as RDF
     *         {@link Statement}s.
     * @throws Exception
     *             Any exception occuring during the extraction process
     */
    private List<Statement> getDistributions(URI dataset, RepositoryConnection conn) throws Exception
    {
        List<Statement> stmts = new ArrayList<Statement>();

        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, distributionQuery);
        query.setBinding("dataset", dataset);

        TupleQueryResult res = query.evaluate();
        long counter = System.currentTimeMillis();

        while (res.hasNext())
        {
            BindingSet bs = res.next();
            
            // Create unique identifier for distribution
            URI distribution = ProviderUtils.objectToUri(dataset.stringValue() + "/distribution/" + counter);
            
            stmts.add(ProviderUtils.createStatement(dataset, DCAT.HAS_DISTRIBUTION, distribution));
            stmts.add(ProviderUtils.createStatement(distribution, RDF.TYPE, DCAT.DISTRIBUTION));
            stmts.add(ProviderUtils.createLiteralStatement(distribution, DCAT.ACCESSURL, bs.getValue("accessURL").stringValue()));
            stmts.add(ProviderUtils.createStatement(distribution, DCTERMS.FORMAT, bs.getValue("format")));
            
            // Write label of distribution
            String distributionLabel = dataset.getLocalName() + " - ";
            distributionLabel += bs.getValue("title")!=null ? bs.getValue("title").stringValue() : bs.getValue("format").stringValue();
                stmts.add(ProviderUtils.createLiteralStatement(distribution, RDFS.LABEL, distributionLabel));
            
            counter++;
        }
        res.close();
        
        return stmts;
    }
    
    /**
     * Extracts all {@link DCTERMS#RELATION}s of a dataset. These relations can
     * either contain links to other datasets, the namespace of the dataset or
     * the number of triples contained in the dataset.
     * 
     * @param dataset
     *            {@link URI} of the dataset whose metadata is being extracted.
     * @param conn
     *            {@link RepositoryConnection} to the {@link MemoryStore} in
     *            which the metadata resides.
     * @return The metadata of the {@link DCTERMS#RELATION}s extracted as RDF
     *         {@link Statement}s.
     * @throws Exception
     *             Any exception occuring during the extraction process
     */
    private List<Statement> getRelations(URI dataset, RepositoryConnection conn) throws Exception
    {
        List<Statement> stmts = new ArrayList<Statement>();

        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, relationQuery);
        query.setBinding("dataset", dataset);

        TupleQueryResult res = query.evaluate();

        while (res.hasNext())
        {
            BindingSet bs = res.next();

            // Distinguish between three different kind of relations
            if(bs.getValue("label").stringValue().trim().startsWith("links"))
            {
                String linkLabel = bs.getValue("label").stringValue().split(":")[1];
                
                // Data about the number of links is not stored
                stmts.add(ProviderUtils.createStatement(dataset, DCAT.LINKSTO, ProviderUtils.objectAsUri("http://datahub.io/dataset/" + linkLabel)));
            }
            else if(bs.getValue("label").stringValue().trim().startsWith("triples"))
                stmts.add(ProviderUtils.createStatement(dataset, Vocabulary.VOID.TRIPLES, bs.getValue("value")));
            else if(bs.getValue("label").stringValue().trim().startsWith("namespace"))
                stmts.add(ProviderUtils.createStatement(dataset, Vocabulary.VOID.URISPACE, bs.getValue("value")));
        }
        res.close();
        
        return stmts;
    }

    /**
     * Extracts all metadata regarding {@link DCTERMS#CREATOR} and
     * {@link DCTERMS#CONTRIBUTOR}. The method
     * 
     * @param dataset
     *            {@link URI} of the dataset whose creators/contributors are
     *            being extracted.
     * @param property
     *            Either {@link DCTERMS#CREATOR} or {@link DCTERMS#CONTRIBUTOR}
     * @param conn
     *            {@link RepositoryConnection} to the {@link MemoryStore} in
     *            which the metadata resides.
     * @return The metadata of the creators/contributors extracted as RDF
     *         {@link Statement}s.
     * @throws Exception
     *             Any exception occuring during the extraction process
     */
    List<Statement> getAuthor(URI dataset, URI property, RepositoryConnection conn) throws Exception
    {
        List<Statement> stmts = new ArrayList<Statement>();
        
        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, creatorContributorQuery);
        query.setBinding("dataset", dataset);
        query.setBinding("creatorContributor", property);
        
        TupleQueryResult res = query.evaluate();
        
        while(res.hasNext())
        {
            BindingSet bs = res.next();
            
            stmts.add(ProviderUtils.createStatement(dataset, property, bs.getValue("name")));
        }
        res.close();
        
        return stmts;
    }

}
