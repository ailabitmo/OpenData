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

package com.fluidops.iwb.luxid;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.GenUtil;
import com.fluidops.util.Pair;

/**
 * LuxidExtractor offers access to Luxid Text Extraction Webservice
 * 
 * @author tobimath
 *
 */
public class LuxidExtractor {
	
	private static final Logger logger = Logger.getLogger(LuxidExtractor.class);
	
	public static final URI company_type = ValueFactoryImpl.getInstance().createURI("http://www.temis.com/luxid#Company");
	public static final URI person_type = ValueFactoryImpl.getInstance().createURI("http://www.temis.com/luxid#Person");
	public static final URI location_type = ValueFactoryImpl.getInstance().createURI("http://www.temis.com/luxid#Location");
	public static final URI category_type = ValueFactoryImpl.getInstance().createURI("http://www.temis.com/luxid#Category");
	public static final URI document_type = ValueFactoryImpl.getInstance().createURI("http://www.temis.com/luxid#Document");
	public static final URI in_doc_rel = ValueFactoryImpl.getInstance().createURI("http://www.temis.com/luxid#inDocument");
	
	
	public enum AnnotationPlan {
		
		Biological,
		CI_Full,
		MedicalEntities
	}
	
	public enum Outputformats {
		RDF,
		HTMLWriterCasConsumer
	}

	public static String useLuxidWS(String input, String token, String outputFormat, String annotationPlan) throws Exception
	{
		String s = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=" +
		"\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
		"<SOAP-ENV:Body> <ns1:annotateString xmlns:ns1=\"http://luxid.temis.com/ws/types\"> " +
		"<ns1:sessionKey>" + token + "</ns1:sessionKey> <ns1:plan>" + annotationPlan + "</ns1:plan> <ns1:data>";

		
		s += input;// "This is a great providing test";
		
		s += "</ns1:data> <ns1:consumer>" + outputFormat + "</ns1:consumer> </ns1:annotateString> </SOAP-ENV:Body> " +
				"</SOAP-ENV:Envelope>";
		
		URL url = new URL("http://193.104.205.28//LuxidWS/services/Annotation");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		
		conn.getOutputStream().write(s.getBytes());
		
		StringBuilder res = new StringBuilder();

		if ( conn.getResponseCode() == HttpURLConnection.HTTP_OK )
		{
			InputStream stream = conn.getInputStream();
			
			InputStreamReader read = new InputStreamReader(stream);
			BufferedReader rd = new BufferedReader(read);
			
			String line = "";
			
			StringEscapeUtils.escapeHtml("");
			while ((line = rd.readLine()) != null)
			{
				res.append(line);
			}
			rd.close();
		}
//		res = URLDecoder.decode(res, "UTF-8");
		return StringEscapeUtils.unescapeHtml(res.toString().replace("&amp;lt", "<"));
	}
	
	public static String extractWithLuxid(String input, String annotationPlan, String outputFormat, String token) throws Exception
	{
		String res = useLuxidWS(input,token,outputFormat, annotationPlan);
		res = res.substring(res.indexOf("<text>") + "<text>".length(), res.indexOf("</text>")  );
		return res;
	}
	
	public static String authenticate() throws Exception
	{
		String token = "";
		
		/* ***** Authentication ****** */
		String auth = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <SOAP-ENV:Body> <ns1:authenticate xmlns:ns1=\"http://luxid.temis.com/ws/types\"> " +
				"<ns1:userName>" + Config.getConfig().getLuxidUserName() + "</ns1:userName> <ns1:userPassword>" + 
				Config.getConfig().getLuxidPassword() + "</ns1:userPassword> </ns1:authenticate> </SOAP-ENV:Body> </SOAP-ENV:Envelope>";
		URL authURL = new URL(Config.getConfig().getLuxidServerURL());
		
		HttpURLConnection authconn = (HttpURLConnection) authURL.openConnection();
		authconn.setRequestMethod("POST");
		authconn.setDoOutput(true);
		
		authconn.getOutputStream().write(auth.getBytes());
		
		if ( authconn.getResponseCode() == HttpURLConnection.HTTP_OK )
		{
			InputStream stream = authconn.getInputStream();
			
			InputStreamReader read = new InputStreamReader(stream);
			BufferedReader rd = new BufferedReader(read);
			
			String line = "";
			
			if ((line = rd.readLine()) != null)
			{
				token = line.substring(line.indexOf("<token>") + "<token>".length(), line.indexOf("</token>"));
			}
			rd.close();
		}
		return token;
	}
	
	/**
	 * performs LUXID text extraction to the string passed as input. Puts the parsed and extended RDF to the global repository.
	 * additionally returns a set of Key-Value-pairs which are inteded to be used as Metadata tags for Atmos
	 * 
	 * @param input
	 * @param token
	 * @param annotationPlan
	 * @param id
	 * @param c
	 * @param f
	 * @return
	 * @throws Exception
	 */
	public static List<Pair<String, String>> extractRDFWithLuxid(String input, String token, String annotationPlan, String id, Context c, File f) throws Exception
	{
		ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
		Set<Resource> entities = new HashSet<Resource>();
		List<Pair<String,String>> atmosTags = new ArrayList<Pair<String,String>>();
		
		List<Statement> stmts = retrieveLuxidStatements(input, token,
				annotationPlan, id, dm, entities);
		dm.addToContext(stmts, c);
		stmts.clear();
		int personCounter = 0;
		int companyCounter = 0;
		int locationCounter = 0;
		
		// STEP 3: generate Atmos metadata tags
		for (Resource entity : entities)
		{
			String stringValue = entity.stringValue();
			if (entity instanceof URI && stringValue.contains("Person"))
				atmosTags.add(new Pair<String, String>("Person_" + personCounter++, stringValue.substring(stringValue.lastIndexOf("/") + 1 )));
			else if (entity instanceof URI && stringValue.contains("Company"))
				atmosTags.add(new Pair<String, String>("Company_" + companyCounter++, stringValue.substring(stringValue.lastIndexOf("/") + 1 )));
			else if (entity instanceof URI && stringValue.contains("Location"))
				atmosTags.add(new Pair<String, String>("Location_" + locationCounter++, stringValue.substring(stringValue.lastIndexOf("/") + 1 )));
		}
		dm.addToContext(stmts, c);
		dm.close();
		dm.calculateVoIDStatistics(c.getURI());
		
		return atmosTags;
	}

	public static String luxidUriEncode(String stringValue) {
		return stringValue.replace(" ", "_");
	}

	public static List<Statement> retrieveLuxidStatements(String input,
			String token, String annotationPlan, String id,
			ReadDataManager dm, Set<Resource> entities)
			throws Exception, RepositoryException, IOException,
			MalformedQueryException, QueryEvaluationException {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		boolean ci = annotationPlan.equals("CI_Full");
		
		// STEP 1: get the RDFXML from Luxid Webservice, and put it into a temporary RDF-Store
		String res = LuxidExtractor.useLuxidWS(input, token, "RDF", annotationPlan);
		Set<Statement> stmts = new HashSet<Statement>();
		
		res = res.substring(res.indexOf("<rdf:RDF") , res.indexOf("</rdf:RDF>") + "</RDF:rdf>".length() );
		Repository ms = new SailRepository(ReadDataManagerImpl.getMemoryStore());
		ms.initialize();
		RepositoryConnection con = ms.getConnection();
		
		
		StringReader read1 = new StringReader(res);
		
		try 
		{
			con.add(read1, "http://luxid.de/", RDFFormat.RDFXML);
			
			// STEP 2: collect all "real" entities, i.e. Persons, Locations, Companies, ...
			RepositoryResult<Statement> findEntities = con.getStatements(null, vf.createURI("http://www.temis.com/luxid#type"), 
					vf.createLiteral("com.temis.uima.Entity"), false);
			
			Statement stmt1;
			while (findEntities.hasNext())
			{
				stmt1 = findEntities.next();
				entities.add(stmt1.getSubject());
				
			}
			
			RepositoryResult<Statement> resTemp = con.getStatements(null, null, null, false);
			
			try
			{
				while (resTemp.hasNext())
				{
					Statement stmt = resTemp.next();
					Resource subject = stmt.getSubject();
					String subjectString = subject.stringValue();
					String newSub = subjectString;
					
					URI predicate = stmt.getPredicate();
					
					Value object = stmt.getObject();
					
					if(subjectString.equals("urn:doc")) {
						subject = filenameToUri(id);
					} else {
						if (subjectString.startsWith("urn:doc"))
							newSub = EndpointImpl.api().getNamespaceService().defaultNamespace() + newSub.substring(7);
						// if the subject of the triple is not an entity, append an ID (e.g. for occurrences in the document)
						if (entities.contains(subject))
							subject = vf.createURI(luxidUriEncode(newSub));
						else	
							subject = vf.createURI(luxidUriEncode(newSub) + id);
					}
					
					predicate = vf.createURI(predicate.stringValue().replace(" ", "_"));
					
					if ( (dm.searchOne(subject, predicate, null) != null) )
						continue;
					
					if (object instanceof Resource)
						if (entities.contains(object))
							object = vf.createURI(object.stringValue().replace(" ", "_"));
						else
							object = vf.createURI(object.stringValue().replace(" ", "_") + id);
					
					if (predicate.stringValue().equals("http://www.temis.com/luxid#type"))
					{
						if (!dm.getType(subject).isEmpty())
							continue;
						else
						{
							predicate = RDF.TYPE;
							if (ci)
							{
								object = vf.createURI("http://" + object.stringValue());
								if (dm.searchOne(subject, predicate, null) == null)
									
									// set the rdf:type of the current subject
									if (subject.stringValue().contains("Company"))
										stmts.add(vf.createStatement(subject, predicate, company_type));
									else if (subject.stringValue().contains("Person"))
										stmts.add(vf.createStatement(subject, predicate, person_type));
									else if (subject.stringValue().contains("Location"))
										stmts.add(vf.createStatement(subject, predicate, location_type));
							}
							else
								object = vf.createURI("http://" + object.stringValue() + "_lifescience");
							
							if (dm.searchOne((URI)object, RDF.TYPE,  OWL.CLASS) != null)
								stmts.add(vf.createStatement((URI)object, RDF.TYPE, OWL.CLASS));
							
							if (dm.searchOne((URI)object, RDFS.LABEL, null) != null)
								stmts.add(vf.createStatement((URI)object, RDFS.LABEL, vf.createLiteral(object.stringValue().substring(object.stringValue().lastIndexOf(".")+ 1) )));
							
							if (object.stringValue().equals("http://org.apache.uima.jcas.impl.JCasImpl_lifescience"))
							{
								stmts.add(vf.createStatement(subject, vf.createURI("http://www.temis.com/luxid#value"), vf.createLiteral(input)));
							}
							else if (object.stringValue().equals("http://org.apache.uima.jcas.impl.JCasImpl"))
							{
								stmts.add(vf.createStatement(subject, vf.createURI("http://www.temis.com/luxid#value"), vf.createLiteral(input)));
								
								// for XCAS-documents: get the title of the doc, set it as rdfs:label
//								if (f.getName().endsWith(".xml"))
//									getDocumentTitle(input, stmts, vf, id, f);
							}
						}
					}
					else if (predicate.stringValue().equals("http://www.temis.com/luxid#Latitude"))
					{
						predicate = Vocabulary.GEO.LAT;
					}
					else if (predicate.stringValue().equals("http://www.temis.com/luxid#Longitude"))
					{
						predicate = Vocabulary.GEO.LONG;
					}
					stmts.add(vf.createStatement(subject, predicate, object));
					if (!subjectString.startsWith("urn:doc"))
						if (dm.searchOne((URI)subject, RDFS.LABEL,null) != null)
							stmts.add(vf.createStatement(subject, RDFS.LABEL, vf.createLiteral(subject.stringValue().substring(subject.stringValue().lastIndexOf("/") + 1))));
					
				}
			}
			catch (Exception ex1)
			{
				logger.warn(ex1.getMessage(), ex1);
			}
			
			
			SailRepository msTemp = new SailRepository(ReadWriteDataManagerImpl.getMemoryStore());
			msTemp.initialize();
			
			ReadWriteDataManager dmTemp = ReadWriteDataManagerImpl.openDataManager(msTemp);
			dmTemp.add(stmts);
			
			TupleQueryResult res1 = dmTemp.sparqlSelect("SELECT ?ent ?begin ?end WHERE { ?ent luxid:begin ?begin. ?ent luxid:end ?end . }", true);
			
			// generate the text snippets for all entities, occurrences, ... of the current document
			while (res1.hasNext())
			{
				BindingSet bind = res1.next();
				Resource x = (Resource) bind.getBinding("ent").getValue();
				int startIndex = Integer.parseInt(bind.getBinding("begin").getValue().stringValue());
				int endIndex = Integer.parseInt(bind.getBinding("end").getValue().stringValue());
				
				String snippet = "";
				try {
					snippet = input.substring(startIndex, endIndex);
				} catch (Exception ex) {
					try {
						snippet = input.substring(startIndex, input.length());
					}
					catch (Exception ex1)
					{
						snippet = "";
					}
				}
				if (dmTemp.searchOne(x, vf.createURI("http://www.temis.com/luxid#value"), null) == null)
					stmts.add(vf.createStatement(x, vf.createURI("http://www.temis.com/luxid#value"), vf.createLiteral(snippet)));

			}
			
			dmTemp.close();
		}
		catch (RDFParseException e)
		{
			logger.warn(e.getMessage(), e);
		}
		for (Resource entity : entities)
		{
			if (dm.searchOne(entity, RDFS.LABEL,null) == null)		
				stmts.add(vf.createStatement(vf.createURI(luxidUriEncode(entity.stringValue())), RDFS.LABEL, vf.createLiteral(entity.stringValue().substring(entity.stringValue().lastIndexOf("/") + 1))));
		}
		return new ArrayList<Statement>(stmts);
	}

	public static URI filenameToUri(String filename) {
		return ValueFactoryImpl.getInstance().createURI("File:" + filename);
	}
	
	/**
	 * returns the name of a XCAS- document
	 * 
	 * @param input
	 * @param stmts
	 * @param vf
	 * @param id
	 * @param f
	 * @throws Exception
	 */
	public static void getDocumentTitle(String input, Set<Statement> stmts, ValueFactory vf, String id, File f) throws Exception 
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Document doc = db.parse(f);
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile("//com.temis.uima.Zone[@name='title']");
		
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		for ( int i = 0 , n = nodes.getLength() ; i < n ; i++ )
		{
			String begin = nodes.item(i).getAttributes().getNamedItem("begin").getTextContent();
			String end = nodes.item(i).getAttributes().getNamedItem("end").getTextContent();
			
			stmts.add(vf.createStatement(vf.createURI(EndpointImpl.api().getNamespaceService().defaultNamespace() + id), RDFS.LABEL, vf.createLiteral(input.substring(Integer.parseInt(begin), Integer.parseInt(end)))));
		}
	}

	/**
	 * extracts a zip-file and returns references to the unziped files. If the file passed to this method
	 * is not a zip-file, a reference to the file is returned.
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static Set<File> extractZip(String fileName) throws Exception
	{
        File zipf = new File((new StringBuilder("luxid/")).append(fileName).toString());
        
        Set<File> toBeUploaded = new HashSet<File>();
        if(zipf.getName().endsWith(".zip"))
        {
            ZipFile zip = new ZipFile(zipf);
            for(Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();)
            {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                if(entry.isDirectory())
                {
                    logger.info((new StringBuilder("Extracting directory: ")).append(entry.getName()).toString());
                    GenUtil.mkdir(new File(entry.getName()));
                }
                else
                {
                	logger.info((new StringBuilder("Extracting file: ")).append(entry.getName()).toString());
                	String entryPath = "luxid/" + entry.getName();
                	FileOutputStream fileOutputStream = null;
                	InputStream zipEntryStream = zip.getInputStream(entry);
                	try {
                		fileOutputStream = new FileOutputStream(entryPath);
                		IOUtils.copy(zipEntryStream, fileOutputStream);
                	} finally {
                		closeQuietly(zipEntryStream);
                		closeQuietly(fileOutputStream);
                	}
                    toBeUploaded.add(new File(entryPath));
                }
            }

            zip.close();
        }
        else
        {
            toBeUploaded.add(zipf);
        }
        
        return toBeUploaded;
	}
}
