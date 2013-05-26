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
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.xeustechnologies.jtar.TarInputStream;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;

import fr.inria.rdfa.RDFaParser;

@TypeConfigDoc( "Reads RDF data from a web document in any RDF format to store the data unmodified locally."+ 
		"The source data may also be compressed in .zip, .gz or .tar.gz format." )
public class RDFProvider extends AbstractFlexProvider<RDFProvider.Config>
{
	private static final long serialVersionUID = 7415666290518242634L;

    private static final Logger logger = Logger.getLogger(RDFProvider.class.getName());
	
	InputStream unpackStream( URL url, InputStream instream ) throws IOException
	{
		if (url.toString().endsWith("tar.gz"))
		{
			TarInputStream tar = new TarInputStream( new GZIPInputStream( instream ) );
			return tar;
		}
		else if (url.toString().endsWith(".gz"))
	        {
	            return new GZIPInputStream(instream);
	        }
		else if (url.toString().endsWith(".zip"))
			return new ZipInputStream(instream);
		else
			return instream;

	}
	
	
	@Override
	public void gather(final List<Statement> res) throws Exception
	{
	    // TODO: This should be done via SPARQL Provider, has nothing to do with RDF Provider.
		// if statement can be moved to DataSourcesTable - leave it in for now so
		// adding lod from the UI works
		if ( config.url.endsWith( "sparql" ) )
		{
			SPARQLEndpointProvider.Config c = new SPARQLEndpointProvider.Config();
			c.endpoint = config.url;
			c.query = "construct {?s ?p ?o} where {?s ?p ?o}";
			SPARQLEndpointProvider p = new SPARQLEndpointProvider();
			p.config = c;
			p.gather(res);
			return;
		}
		Repository repository;
		
		// in streaming mode, we write directly into the repository
		if(config.streaming)
		    repository = Global.repository;
		else {
		    repository = new SailRepository(new MemoryStore());
		    repository.initialize();
		}
		
        URL url = new URL(config.url);
        
        boolean success = false;
        logger.info("Loading " + url);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(1000);
        String contentType = conn.getContentType();

        // try to determine RDFFormat
        RDFFormat rdfFormat = null;
        
        // ideally, the format is already specified in the config
        if(config.format!=null)
        {
           rdfFormat = RDFFormat.forMIMEType(config.format);
        }
        
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
            if(url.getFile().contains("n3"))
                rdfFormat = RDFFormat.N3;
            if(config.format!=null && config.format.contains("ntriples"))
                rdfFormat = RDFFormat.NTRIPLES;
        }
        
        if(rdfFormat == null)
            logger.error("Failed to determine RDF Format for "+url);
        if(rdfFormat !=null )
        {
            RepositoryConnection con = repository.getConnection();
            try 
            {
                con.add(unpackStream(url, conn.getInputStream()), url.toString(), rdfFormat, ValueFactoryImpl.getInstance().createURI(url.toString()));
                success = true;
                logger.info("Successfully loaded "+url+" with RDFFormat "+rdfFormat);
                
                // in non streaming mode, we need to return the triples as list
                if(!config.streaming) 
                    res.addAll(con.getStatements(null, null, null, false).asList());
            }
            catch ( Exception e )
            {
                logger.error("Failed to load "+url+" with RDFFormat "+rdfFormat);
                throw new RuntimeException(e);
            }
            finally 
            {
                con.close();
            }
        }
        
        if ( !success )
        {
            //  try RDFa
            try
            {
                RDFaParser aParser = new RDFaParser()
                {
                    @Override
                    public void handleDataProperty(String subjectURI, String subjectNodeID, String propertyURI, String value, String datatype, String lang) 
                    { 
                        ValueFactoryImpl vf = ValueFactoryImpl.getInstance();
                        res.add( vf.createStatement( vf.createURI( subjectURI ), vf.createURI(propertyURI), vf.createLiteral( value ) ) );
                    } 

                    public void handleObjectProperty(String subjectURI, String subjectNodeID, String propertyURI, String objectURI, String objectNodeID)
                    {
                        ValueFactoryImpl vf = ValueFactoryImpl.getInstance();
                        res.add( vf.createStatement( vf.createURI( subjectURI ), vf.createURI(propertyURI), vf.createURI(objectURI) ) );
                    }
                };
                aParser.parse(new InputStreamReader( unpackStream(url, url.openStream()) ), config.url);

            }
            catch ( Exception ignore )
            {
                // wrong format
            	logger.trace("Error occured while collecting RDF data: " + ignore.getMessage(), ignore);
            }
        }
        if ( res.isEmpty() )
            throw new RuntimeException("No valid RDF data found");
	}

	@Override
	public void setLocation( String location )
	{
		config.url = location;
	}
	
	@Override
	public String getLocation()
	{
		return config.url;
	}

	@Override
	public Class<? extends Config> getConfigClass()
	{
		return Config.class;
	}
	
	public static class Config implements Serializable
	{
        private static final long serialVersionUID = -256843331311012640L;

		@ParameterConfigDoc(
				desc = "URL of the RDF data source",
				required = true)
		public String url;

		@ParameterConfigDoc(desc = "MIME type of the RDF document. Defaults to the content type as given by the http-connection to the given url")
		public String format;

		@ParameterConfigDoc(desc = "Defines whether streaming mode is set to true or false")
		public boolean streaming;
	}
	
	public static class ProviderThread extends Thread
	{
	    RDFProvider provider;
	    public ProviderThread(RDFProvider p)
	    {
	        provider=p;   
	    }
	    
	    @Override
	    public void run()
	    {
	        provider.list();

	    }
	}
	
}
