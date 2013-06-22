package com.fluidops.iwb.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.model.Vocabulary.DCTERMS;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.util.GenUtil;
import com.fluidops.util.StringUtil;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Gathers data from a particular table from website and parses it into RDF
 * format
 * 
 * Current Provider URL (to be used as location):
 * http://gov.spb.ru/gov/otrasl/tr_infr_kom/tekobjekt/tek_rem/
 * 
 * @author mgalkin
 */
@TypeConfigDoc("Gathers data from a table on a website")
public class HTMLProvider extends AbstractFlexProvider<HTMLProvider.Config> {

	private static final Logger logger = Logger.getLogger(HTMLProvider.class
			.getName());
	private static final long serialVersionUID = 1000L;

	public static class Config implements Serializable {

		private static final long serialVersionUID = 1001L;

		@ParameterConfigDoc(desc = "URL of the source table", required = true)
		public String url;
	}

	@Override
	public void setLocation(String location) {
		config.url = location;
	}

	@Override
	public String getLocation() {
		return config.url;
	}

	@Override
	public void gather(List<Statement> res) throws Exception {

		String url = config.url;
		Document doc = Jsoup.connect(url).get();
		Elements links = doc.select("a[href]");
		Elements media = doc.select("[src]");
		Elements imports = doc.select("link[href]");
		// Elements article =
		// doc.select("div.wrapper").select("div.box-shadow").select("div#content.cols").select("div.cl").select("div.crm").select("article").select("section.article").select("div.textblock").select("table");
		Elements article = doc.getElementsByTag("tbody").select("tr");
		Elements tableElem;
		URI nameURI = null;
		URI roadsURI = null;
		URI sideURI = null;
		URI totalURI = null;

		File file = new File("HTMLdata.txt");
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
				file)));

		out.println("Media");
		print("\nMedia: (%d)", media.size());
		for (Element el : media) {
			if (el.tagName().equals("img")) {
				print(" * %s: <%s> %sx%s (%s)", el.tagName(),
						el.attr("abs:src"), el.attr("width"),
						el.attr("height"), trim(el.attr("alt"), 20));
				out.printf(" \n * %s: <%s> %sx%s (%s)", el.tagName(),
						el.attr("abs:src"), el.attr("width"),
						el.attr("height"), trim(el.attr("alt"), 20));
				out.println();
			} else {
				print(" * %s: <%s>", el.tagName(), el.attr("abs:src"));
				out.printf(" \n * %s: <%s>", el.tagName(), el.attr("abs:src"));
				out.println();
			}

		}

		out.println("Imports");
		print("\nImports: (%d)", imports.size());
		for (Element link : imports) {
			print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"),
					link.attr("rel"));
			out.printf(" * %s <%s> (%s)", link.tagName(),
					link.attr("abs:href"), link.attr("rel"));
			out.println();
		}

		out.println("Links");
		print("\nLinks: (%d)", links.size());
		for (Element link : links) {
			print(" * a: <%s> (%s)", link.attr("abs:href"),
					trim(link.text(), 35));
			out.printf(" * a: <%s> (%s)", link.attr("abs:href"), link.text());
			out.println();
		}

		/*
		 * out.println("Custom text"); print("\nCustom: (%d)",customArt.size());
		 * for (Element custom:customArt){
		 * out.printf(" * a (%s): (%s)",custom.tagName(),custom.text());
		 * out.println(); }
		 */

		out.println("Article");
		print("\nArticle: (%d)", article.size());

		for (int i = 3; i < article.size() - 2; i++) {
			tableElem = article.get(i).select("td");
			out.println();

			if (i == 3) {
				nameURI = ProviderUtils.objectToUri(tableElem.get(0).text());
				roadsURI = ProviderUtils.objectToUri(tableElem.get(1).text());
				sideURI = ProviderUtils.objectToUri(tableElem.get(2).text());
				totalURI = ProviderUtils.objectToUri(tableElem.get(3).text());

			} else {
				
				res.add(ProviderUtils.createStatement(ProviderUtils.objectToUri(tableElem.get(0).text()), RDF.TYPE, nameURI));
				res.add(ProviderUtils.createLiteralStatement(ProviderUtils.objectToUri(tableElem.get(0).text()), RDFS.LABEL, tableElem.get(0).text()));
				res.add(ProviderUtils.createLiteralStatement(ProviderUtils.objectToUri(tableElem.get(0).text()), roadsURI, tableElem.get(1).text()));
				res.add(ProviderUtils.createLiteralStatement(ProviderUtils.objectToUri(tableElem.get(0).text()), sideURI, tableElem.get(2).text()));
				res.add(ProviderUtils.createLiteralStatement(ProviderUtils.objectToUri(tableElem.get(0).text()), totalURI, tableElem.get(3).text()));

				for (Element el : tableElem) {
					out.printf("\n * (%s): (%s)", el.tagName(), el.text());
					out.println();
					
					}
			}
			out.println();
			out.printf("\n * a (%s) (%d): (%s)", article.get(i).tagName(),
					tableElem.size(), article.get(i).text());
			out.println();
		}
		out.close();
	}

	@Override
	public Class<? extends Config> getConfigClass() {
		return HTMLProvider.Config.class;
	}

	private static void print(String msg, Object... args) {
		System.out.println(String.format(msg, args));
	}

	private static String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}
}

/*
 * URL registryUrl = new URL(config.location); HttpURLConnection
 * registryConnection = (HttpURLConnection) registryUrl .openConnection();
 * registryConnection.setRequestMethod("GET");
 * 
 * // ////////////////////////////////////////////////////////////////////// //
 * /////////////////////////////////////////////////////////////// STEP // 1
 * logger.info("Retrieving packages from CKAN...");
 * 
 * if (registryConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
 * String msg = "Connection with the registry could not be established. (" +
 * registryConnection.getResponseCode() + ", " +
 * registryConnection.getResponseMessage() + ")"; logger.warn(msg); throw new
 * RuntimeException(msg); // propagate to UI }
 * 
 * String siteContent = GenUtil.readUrl(registryConnection .getInputStream());
 * 
 * JSONObject groupAsJson = null; JSONArray packageListJsonArray = null; try {
 * groupAsJson = new JSONObject(new JSONTokener(siteContent));
 * packageListJsonArray = groupAsJson.getJSONArray("packages"); } catch
 * (JSONException e) { String msg = "Returned content " + siteContent +
 * " is not valid JSON. Check if the registry URL is valid."; logger.warn(msg);
 * throw new RuntimeException(msg); // propagate to UI }
 * 
 * logger.info("-> found " + packageListJsonArray.length() + " packages");
 * 
 * // ////////////////////////////////////////////////////////////////////// //
 * /////////////////////////////////////////////////////////////// STEP // 2
 * logger.info("Registering LOD catalog in metadata repository");
 *//**
 * HINT: the method createStatement allows to create statements if subject,
 * predicate and object are all known; use this method instead of opening a
 * value factory
 */
/*
 * res.add(ProviderUtils.createStatement(CKANVocabulary.CKAN_CATALOG, RDF.TYPE,
 * Vocabulary.DCAT.CATALOG));
 * res.add(ProviderUtils.createStatement(CKANVocabulary.CKAN_CATALOG,
 * RDFS.LABEL, CKANVocabulary.CKAN_CATALOG_LABEL));
 * 
 * logger.info("-> done");
 * 
 * // ////////////////////////////////////////////////////////////////////// //
 * /////////////////////////////////////////////////////////////// STEP // 3
 * logger
 * .info("Extracting metdata for the individual data sets listed in CKAN");
 *//**
 * HINT: Set up an Apache HTTP client with a manager for multiple threads; as
 * a general guideline, use parallelization whenever crawling web sources!
 */
/*
 * MultiThreadedHttpConnectionManager connectionManager = new
 * MultiThreadedHttpConnectionManager(); HttpClient client = new
 * HttpClient(connectionManager); ExecutorService pool =
 * Executors.newFixedThreadPool(10);
 * 
 * // we store the data in a temporary memory store, which allows us // to
 * perform transformation on the result set Repository repository = null;
 * RepositoryConnection connection = null; try { // initialize repository and
 * connection repository = new SailRepository(new MemoryStore());
 * repository.initialize(); connection = repository.getConnection();
 * 
 * // Fire up a thread for every package
 * logger.info("-> Fire up threads for the individual packages..."); for (int i
 * = 0; i < packageListJsonArray.length(); i++) { // we use the JSON
 * representation to get a base URI to resolve // relative // URIs in the XML
 * later on. (and a fallback solution) String host =
 * "http://www.ckan.net/package/" + packageListJsonArray.get(i).toString();
 * String baseUri = findBaseUri("http://www.ckan.net/api/rest/package/" +
 * packageListJsonArray.get(i).toString()); baseUri = (baseUri == null) ? host :
 * baseUri; pool.execute(new MetadataReader(client, host, baseUri,
 * CKANVocabulary.CKAN_CATALOG, connection)); }
 * 
 * logger.info("-> Waiting for all tasks to complete (" +
 * packageListJsonArray.length() + "tasks/data sources)..."); pool.shutdown();
 * pool.awaitTermination(4, TimeUnit.HOURS);
 *//**
 * Now the extraction has finished, all statements are available in our
 * temporary repository. We apply some conversions and transformations to align
 * the extracted statements with our target ontology.
 * 
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * !!!!!!!!!!!!! !!! NOTE: this code is /NOT/ best practice, we should
 * eventually extend !!! !!! ProviderUtils to deal with at least lightweight
 * transformations !!! !!! (such as changing property names) or realize such
 * tasks using !!! !!! an integrated mapping framework. !!!
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
/*
 * 
 * // Extraction from temporary repository, phase 1: logger.info(
 * "-> Extract dcterms:title AS rdfs:label, dcterms:contributor AS dcterms:creator, and dcterms:rights AS dcterms:license"
 * ); String mappingQuery = mappingQuery(); GraphQuery mappingGraphQuery =
 * connection.prepareGraphQuery( QueryLanguage.SPARQL, mappingQuery);
 * GraphQueryResult result = mappingGraphQuery.evaluate();
 * 
 * logger.info("-> Appending extracted result to statement list");
 * ProviderUtils.appendGraphQueryResultToListAndClose(result, res);
 * 
 * // Label the distribution nodes
 * logger.info("-> Generate labels for distributions"); String
 * labelDistributionQuery = labelDistributionQuery(); GraphQuery
 * labelDistributionGraphQuery = connection
 * .prepareGraphQuery(QueryLanguage.SPARQL, labelDistributionQuery);
 * GraphQueryResult result2 = labelDistributionGraphQuery.evaluate();
 * 
 * logger.info("-> Appending extracted result to statement list");
 * ProviderUtils.appendGraphQueryResultToListAndClose(result2, res);
 * 
 * // Extraction from temporary repository, phase 2: logger.info(
 * "-> Deleting previously extracted triples and additional, not required information..."
 * ); String deleteQuery = deleteQuery(); Update deleteGraphQuery =
 * connection.prepareUpdate( QueryLanguage.SPARQL, deleteQuery);
 * deleteGraphQuery.execute();
 * 
 * // Extraction from temporary repository, phase 3:
 * logger.info("-> Deleting dcat:distribution and dcat:accessUrl information from"
 * + "temp repository for which format information is missing..."); String
 * cleanDistQuery = cleanDistQuery(); Update cleanupGraphQuery =
 * connection.prepareUpdate( QueryLanguage.SPARQL, cleanDistQuery);
 * cleanupGraphQuery.execute();
 * 
 * logger.info("-> Appending remaining statements to result...");
 * connection.getStatements(null, null, null, false).addTo(res);
 * 
 * logger.info("Provider run finished successfully"); } catch (Exception e) {
 * logger.warn(e.getMessage()); throw new RuntimeException(e); } finally { if
 * (connection != null) connection.close(); if (repository != null)
 * repository.shutDown(); }
 * 
 * // in the end, make sure there are no statements containing null in // any of
 * the position (did not take special care when creating // statements)
 * logger.info("-> cleaning up null statements"); res =
 * ProviderUtils.filterNullStatements(res); }
 *//**
 * SPARQL CONSTRUCT query for the alignment of vocabulary with target
 * ontology.
 */
/*
 * private String mappingQuery() { // HINT: avoid hardcoded URIs inside queries,
 * make use of the vocabulary // class instead String q = "CONSTRUCT { " +
 * "  ?s " + ProviderUtils.uriToQueryString(RDF.TYPE) + " " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCAT.DATASET) + " . " + "  ?s " +
 * ProviderUtils.uriToQueryString(RDFS.LABEL) + " ?o1 . " + // Map dc:title to
 * rdfs:label "  ?s " + ProviderUtils.uriToQueryString(DCTERMS.CREATOR) +
 * " ?o2 . " + // Map contributor to creator "  ?s " +
 * ProviderUtils.uriToQueryString(DCTERMS.LICENSE) + " ?o3 . " + // Map rights
 * to license "}" + "WHERE { " + "  ?s " +
 * ProviderUtils.uriToQueryString(RDF.TYPE) + " " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCAT.DATASET) + " . " + "  ?s " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.TITLE) + " ?o1 . " +
 * "  OPTIONAL { ?s " + ProviderUtils
 * .uriToQueryString(Vocabulary.DCTERMS.CONTRIBUTOR) + "  ?o2 } . " +
 * "  OPTIONAL { ?s " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.RIGHTS) + "  ?o3 } . " +
 * "}"; return q; }
 *//**
 * SPARQL CONSTRUCT query constructing an rdfs:label for distributions.
 */
/*
 * private String labelDistributionQuery() { // HINT: avoid hardcoded URIs
 * inside queries, make use of the vocabulary // class instead String q =
 * "CONSTRUCT { " + "  ?d " + ProviderUtils.uriToQueryString(RDFS.LABEL) +
 * " ?distLabel . " + "}" + "WHERE {" +
 * "  SELECT ?d (CONCAT(STR(?label),\" as \",STR(?f)) AS ?distLabel) WHERE { " +
 * "  ?s " + ProviderUtils.uriToQueryString(RDFS.LABEL) + " ?label . " + "  ?s "
 * + ProviderUtils .uriToQueryString(Vocabulary.DCAT.HAS_DISTRIBUTION) +
 * " ?d . " + "  ?d " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCAT.ACCESSURL) + " ?access . " +
 * "  ?d " + ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.FORMAT) +
 * " ?f . " + "  } " + "}"; return q; }
 *//**
 * SPARQL DELETE query for removing dcat-encoded distribution and access URL
 * specifications from a data graph for which no format is specified.
 */
/*
 * private String cleanDistQuery() { // HINT: avoid hardcoded URIs inside
 * queries, make use of the vocabulary // class instead String q = "DELETE {" +
 * "  ?s " + ProviderUtils .uriToQueryString(Vocabulary.DCAT.HAS_DISTRIBUTION) +
 * " ?d . " + "  ?d " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCAT.ACCESSURL) + " ?access . " +
 * "}" + "WHERE {" + "  ?s " + ProviderUtils
 * .uriToQueryString(Vocabulary.DCAT.HAS_DISTRIBUTION) + " ?d . " + "  ?d " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCAT.ACCESSURL) + " ?access . " +
 * "  OPTIONAL { ?d " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.FORMAT) + " ?f } . " +
 * "  FILTER ( !bound(?f) ) . " + "}"; return q; }
 *//**
 * @return SPARQL DELETE query for removing redundant/unneeded daataset
 *         information.
 */
/*
 * private String deleteQuery() { // HINT: avoid hardcoded URIs inside queries,
 * make use of the vocabulary // class instead String q = "DELETE { " + "  ?s "
 * + ProviderUtils.uriToQueryString(RDF.TYPE) + " " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCAT.DATASET) + " . " + "  ?s " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.TITLE) + " ?o1 . " +
 * "  ?s " + ProviderUtils .uriToQueryString(Vocabulary.DCTERMS.CONTRIBUTOR) +
 * " ?o2 . " + "  ?s " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.RIGHTS) + " ?o3 . " +
 * "  ?s " + ProviderUtils.uriToQueryString(OWL.SAMEAS) + " ?o4 . " + "  ?s " +
 * ProviderUtils.uriToQueryString(RDFS.LABEL) + " ?o5 . " + "}" + "WHERE { " +
 * "  ?s " + ProviderUtils.uriToQueryString(RDF.TYPE) + " " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCAT.DATASET) + " . " + "  ?s " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.TITLE) + " ?o1 . " +
 * "  ?s " + ProviderUtils.uriToQueryString(OWL.SAMEAS) + " ?o4 . " + "  ?s " +
 * ProviderUtils.uriToQueryString(RDFS.LABEL) + " ?o5 . " + "  OPTIONAL { ?s " +
 * ProviderUtils .uriToQueryString(Vocabulary.DCTERMS.CONTRIBUTOR) + " ?o2 } . "
 * + "  OPTIONAL { ?s " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.RIGHTS) + " ?o3 } . " +
 * "  OPTIONAL { " + "    ?s " + ProviderUtils
 * .uriToQueryString(Vocabulary.DCAT.HAS_DISTRIBUTION) + " ?d . " + "    ?d " +
 * ProviderUtils.uriToQueryString(Vocabulary.DCTERMS.FORMAT) + " ?f . " + "  }"
 * + "  FILTER ( bound(?f) )" + "}"; return q; }
 *//**
 * Due to some complications with the new CKAN RDF integration, this method
 * will add triples generated from the JSON representation that are missing in
 * the RDF. Hopefully just a temporary solution - maybe the RDF will be updated.
 * 
 * @throws RepositoryException
 */
/*
 * private List<Statement> jsonFallBack(String host, HttpClient client, URI
 * subject) throws RepositoryException {
 * logger.debug("Executing JSON fallback for: " + host); HttpMethod method = new
 * GetMethod(host); method.setFollowRedirects(true);
 * 
 * List<Statement> res = new LinkedList<Statement>(); try { int status =
 * client.executeMethod(method);
 * 
 * if (status == HttpStatus.OK_200) { InputStream response =
 * method.getResponseBodyAsStream(); String content = GenUtil.readUrl(response);
 * JSONObject ob = (JSONObject) getJson(content);
 * 
 * // Resources (Distributions) JSONArray resources =
 * ob.getJSONArray("resources");
 * 
 * for (int i = 0; i < resources.length(); i++) { JSONObject resource =
 * (JSONObject) resources.get(i);
 * 
 * // generate a unique timestamp long timestamp = System.currentTimeMillis() +
 * i;
 * 
 * // HINT: // the method ProviderUtils.objectAsUri() is a safe // replacement
 * for // the ValueFactory.createUri(). It may, however, return // null. At //
 * this position we're null safe, as subject is a valid URI // and // the
 * timestamp does not break the URI URI distributionUri =
 * ProviderUtils.objectAsUri(subject .toString() + "/" + timestamp);
 * 
 * // HINT: // again, ProviderUtils.createStatement() is used to // generate
 * statements // when all the three components are known
 * res.add(ProviderUtils.createStatement(subject,
 * Vocabulary.DCAT.HAS_DISTRIBUTION, distributionUri));
 * res.add(ProviderUtils.createStatement(distributionUri, RDF.TYPE,
 * Vocabulary.DCAT.DISTRIBUTION));
 * 
 * String accessURL = resource.getString("url"); String format =
 * resource.getString("format");
 * 
 * // HINT: // the method ProviderUtils.createUriStatement() can be used // to
 * create // statements with a URI in object position whenever //
 * subject+predicate // (or only the predicate) are known if
 * (!StringUtil.isNullOrEmpty(accessURL))
 * res.add(ProviderUtils.createUriStatement( distributionUri,
 * Vocabulary.DCAT.ACCESSURL, accessURL));
 * 
 * // HINT: // the method ProviderUtils.createLiteralStatement() can be // used
 * to create // statements with literal in object position whenever //
 * subject+predicate // (or only the predicate) are known if
 * (!StringUtil.isNullOrEmpty(format))
 * res.add(ProviderUtils.createLiteralStatement( distributionUri,
 * Vocabulary.DCTERMS.FORMAT, format)); }
 * 
 * // tags JSONArray tags = ob.getJSONArray("tags"); for (int i = 0; i <
 * tags.length(); i++) { String tag = tags.getString(i);
 * 
 * if (!StringUtil.isNullOrEmpty(tag))
 * res.add(ProviderUtils.createLiteralStatement(subject,
 * Vocabulary.DCAT.KEYWORD, tag));
 * 
 * // HINT: // below, we use ProviderUtils.objectToURIInNamespace to // create a
 * // URI in some target namespace; this method works for any // object based on
 * // its toString() representation; it is null safe, assuming // the object's
 * // string representation is neither empty nor null if
 * (!(tag.startsWith("lod") || tag.contains("-") || tag .startsWith("rdf")))
 * res.add(ProviderUtils.createStatement(subject, Vocabulary.DCAT.THEME,
 * ProviderUtils .objectToURIInNamespace( Vocabulary.DCAT.NAMESPACE, tag))); }
 * 
 * response.close(); } else {
 * logger.warn("Bad response from server, JSON fallback failed (status " +
 * status + ", Url: " + host + ")"); } } catch (Exception e) {
 * logger.warn(e.getMessage()); res.clear(); // do not return partial result //
 * ignore warning (affects only a single dataset) } finally {
 * method.releaseConnection(); }
 * 
 * return res; }
 *//**
 * Retrieves the base URI from a given host. Returns null if retrieval fails.
 */
/*
 * private String findBaseUri(String host) { // Read the base URI from the JSON,
 * located in the "url" key-value pair try { URL url = new URL(host);
 * HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 * conn.setRequestMethod("GET");
 * 
 * if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) { String content =
 * GenUtil.readUrl(conn.getInputStream()); Object ob = getJson(content);
 * 
 * String baseUrl = ((JSONObject) ob).getString("url");
 * 
 * return baseUrl == null ? null : baseUrl; }
 * 
 * conn.disconnect(); } catch (MalformedURLException e1) {
 * logger.warn("Supplied host is not a valid URL."); // ignore warning (affects
 * only a single dataset) } catch (IOException e2) {
 * logger.warn("IOException while retrieving base URI."); // ignore warning
 * (affects only a single dataset) } catch (JSONException e3) {
 * logger.warn("No base URL found for: " + host + "!\n" + e3.getMessage()); //
 * ignore warning (affects only a single dataset) }
 * 
 * return null; }
 *//**
 * Task class for the worker threads reading the metadata. Writes the
 * extracted data into a (temorary repository connection).
 */
/*
 * private class MetadataReader implements Runnable { private String url;
 * private HttpClient client; private String baseUri; private URI catalog;
 * private RepositoryConnection connection;
 * 
 * public MetadataReader(HttpClient httpClient, String packageURL, String
 * baseUri, URI cata, RepositoryConnection connection) { this.url = packageURL;
 * this.client = httpClient; this.baseUri = baseUri; this.catalog = cata;
 * this.connection = connection; }
 * 
 * @Override public void run() { // Query the new RDF-metadata CKAN integration
 * via <URL> + '.rdf' // per dataset. logger.debug("Processing " + url + "...");
 * 
 * HttpMethod method = new GetMethod(this.url + ".rdf");
 * method.setFollowRedirects(true);
 * 
 * try { int status = client.executeMethod(method); if (status ==
 * HttpStatus.OK_200) { InputStream response = method.getResponseBodyAsStream();
 * 
 * connection.add(response, baseUri.toString(), RDFFormat.RDFXML,
 * ProviderUtils.objectAsUri(url.toString())); response.close();
 * 
 * URI subject = ProviderUtils.objectAsUri(this.url.replace(
 * "www.ckan.net/package", "thedatahub.org/dataset"));
 * connection.add(ProviderUtils.createStatement(catalog,
 * Vocabulary.DCAT.HAS_DATASET, subject));
 * 
 * // FallBack List<Statement> stmts = jsonFallBack(url.replace(
 * "http://www.ckan.net/package/", "http://www.ckan.net/api/rest/package/"),
 * client, subject); connection.add(stmts); } else {
 * logger.warn("Bad response from server, cannot obtain metadataset (status " +
 * status + ", Url: " + url + ")"); // do not abort here, as this affects only a
 * single data // source } } catch (Exception e) { // do not abort here, as this
 * affects only a single data source
 * logger.warn("Exception in extractor thread: " + e.getMessage()); } finally {
 * method.releaseConnection(); } logger.info("Processed " + url + "..."); } }
 *//**
 * Wraps a string into a JSON object. Returns null if content is not a valid
 * JSON object.
 */
/*
 * private static Object getJson(String content) { JSONTokener tokener = new
 * JSONTokener(content);
 * 
 * try { JSONObject json = new JSONObject(tokener); return json; } catch
 * (Exception e) { logger.warn(e.getMessage(), e); }
 * 
 * try { JSONArray jsonArray = new JSONArray(tokener); return jsonArray; } catch
 * (Exception e) { logger.warn(e.getMessage(), e); }
 * 
 * return null; }
 *//**
 * HINT: Provider vocabulary class, containing provider-specific vocabulary
 * (i.e., only such vocabulary for which we are sure that (i) we cannot use any
 * external ontology and (ii) that will not be used by other providers. This
 * includes, for instance, provider-specific URIs and the like.
 */
/*
 * private static class HTMLVocabulary { // namespace for CKAN catalog private
 * static final String HTML_CATALOG_NAMESPACE =
 * "http://gov.spb.ru/gov/otrasl/tr_infr_kom/tekobjekt/tek_rem/";
 * 
 * // URI identifying the CKAN catalog itself public static final URI
 * HTML_CATALOG = ProviderUtils .objectToURIInNamespace(HTML_CATALOG_NAMESPACE,
 * "datatable");
 * 
 * // Label for the CKAN catalog public static final Literal HTML_CATALOG_LABEL
 * = ProviderUtils .toLiteral(HTML_CATALOG_NAMESPACE + "datatable");
 * 
 * } }
 */
