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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.util.GenUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Exceptions caugt for robustness")
public class RDFLookupProvider extends AbstractFlexProvider<EmptyConfig> implements
        LookupProvider
{

    private static final long serialVersionUID = 8371805968338899075L;
    private static final Logger logger = Logger.getLogger(RDFLookupProvider.class.getName());
    @Override
    public void gather(List<Statement> res) throws Exception
    {
        // not applicable for lookup

    }

    @Override
    public Class getConfigClass()
    {
        return EmptyConfig.class;
    }

    public boolean accept(URI uri)
    {
        //TODO: Quick fix to avoid interference with TwitterProvider
        //Need ordering of providers! This one needs to run last
        if(uri.stringValue().startsWith("http://twitter.com/"))
            return false;
        
        //If we already have outgoing triples for that URI, return
        ReadDataManager dm = EndpointImpl.api().getDataManager();
        List<Statement> res = dm.getStatementsAsList(uri, null, null, false);

        for (int i=0;i<res.size();i++)
        {
            URI context = (URI) res.get(i).getContext();
            if (context != null && !context.stringValue().toLowerCase().contains("lookupprovider"))
                return false;
        }
        
        return true;
    }
    
    @Override
    public void gather(List<Statement> res, URI uri) throws Exception
    {
        
        ValueFactory valueFactory = ValueFactoryImpl.getInstance();
        Repository repository = new SailRepository(new MemoryStore());
        RepositoryConnection con = null;
        try
        {

            repository.initialize();
            // RDFFormat rdfFormat = RDFFormat.RDFXML;
            URL url = new URL(uri.stringValue());

            URLConnection conn = url.openConnection();

            conn.setRequestProperty("Accept", "application/rdf+xml");

            String contentType = conn.getContentType();
            

            // try to determine RDFFormat
            RDFFormat rdfFormat = null;
            
            // next most reliable is MIME type of content
            if(rdfFormat==null)
            {
                rdfFormat = RDFFormat.forMIMEType(contentType);
            }
            
            //As alternative try file name
            if(rdfFormat==null)
            {
                rdfFormat = RDFFormat.forFileName(url.getFile());
            }
            
            //Additional heuristics
            if(rdfFormat==null) 
            {
                if(contentType!=null && contentType.contains("n3"))
                    rdfFormat = RDFFormat.N3;
                if(contentType!=null && contentType.contains("rdf+xml"))
                    rdfFormat = RDFFormat.RDFXML;
                if(url.getFile().contains("n3"))
                    rdfFormat = RDFFormat.N3;
            }
            
            if(rdfFormat == null)
                logger.error("Failed to determine RDF Format for "+url);
            if(rdfFormat !=null )
            {
                con = repository.getConnection();
                try 
                {
                    con.add(conn.getInputStream(), url.toString(), rdfFormat, ValueFactoryImpl.getInstance().createURI(url.toString()));
                    res.addAll(con.getStatements(null, null, null, false).asList());
                    logger.info("Successfully loaded "+url+" with RDFFormat "+rdfFormat);
                }
                catch ( Exception e )
                {
                    logger.error("Failed to load "+url+" with RDFFormat "+rdfFormat, e);
                }
                finally 
                {
                    con.close();
                }
            }
            else if (contentType != null && contentType.startsWith("text/javascript"))
            { // at least try to read as RDF/XML

                String content = GenUtil.readUrl(conn.getInputStream());
                JSONObject o = new org.json.JSONObject(new JSONTokener(content));
                Iterator iter = o.keys();
                while (iter.hasNext())
                {
                    //TODO
                }

            }
            //TODO: Here we should also try to support RDFa
            //TODO: Currently, we look for a link to an RDF document, perhaps it is better to try content negotiation
            else if (contentType != null && contentType.startsWith("text/html"))
            {
                Tidy tidy = new Tidy(); // obtain a new Tidy instance
                tidy.setXHTML(true); // set desired config options using tidy
                // setters

                Document doc = null;
                try {
                   doc = tidy.parseDOM(conn.getInputStream(), null ); // run
                }
                catch(Exception e)
                {  /* ignore */}
                // tidy, providing an input and output stream
                /*
                 * Element root = doc.getDocumentElement(); NodeList links =
                 * root.getElementsByTagName("link"); for(:links.item()) { Node
                 * link = links.item(i); if(link.get) }
                 */
                if(doc!=null) 
                {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory
                    .newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    Source xmlSource = new DOMSource(doc);
                    Result outputTarget = new StreamResult(outputStream);
                    TransformerFactory.newInstance().newTransformer().transform(
                            xmlSource, outputTarget);
                    InputStream is = new ByteArrayInputStream(outputStream
                            .toByteArray());

                    doc = db.parse(is);

                    XPathFactory factory = XPathFactory.newInstance();
                    XPath xpath = factory.newXPath();
                    XPathExpression expr = xpath
                    .compile("//link[@rel=\"alternate\"][@type=\"application/rdf+xml\"]/@href");
                    Object result = expr.evaluate(doc, XPathConstants.STRING);

                    String href = (String) result;
                    if (href.isEmpty()) // no href to RDF found
                        return;
                    url = new URL(href);

                    conn = url.openConnection();

                    contentType = conn.getContentType();
                    rdfFormat = RDFFormat.forMIMEType(contentType);

                    if (rdfFormat == null)
                    {
                        if(contentType.contains("n3"))
                            rdfFormat = RDFFormat.N3;
                        else
                            rdfFormat = RDFFormat.RDFXML; // try this as default
                    }
                    con = repository.getConnection();

                    con.add(url, null, rdfFormat, valueFactory.createURI(url
                            .toString()));
                    res.addAll(con.getStatements(null, null, null, false).asList());
                    con.close();
                }
            }

        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
     }

}
