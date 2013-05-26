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
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.util.StringUtil;

/**
 * allows treating google refine as a provider datasource.
 * Makes sense if
 * 
 * 1) we incrementally improve refine mappings after exploring the data in the IWB
 * 2) a file dump is being generated periodically by a 3rd party software and 
 *    continually imported by IWB
 * 
 * @author aeb
 */
public class GoogleRefineProvider extends AbstractFlexProvider<GoogleRefineProvider.Config>
{
	private static final long serialVersionUID = -8029934070691785231L;

	@Override
	public Class<Config> getConfigClass() 
	{
		return Config.class;
	}

	public static class Config implements Serializable
	{
		private static final long serialVersionUID = 6075298921055165870L;

		@ParameterConfigDoc(desc = "Google Refine RDF Export URL", required = true)
		public String url;
		
		@ParameterConfigDoc(desc = "Google Refine Project ID", required = true)
		public String project;
	}
	
	@Override
	public void gather(List<Statement> res) throws Exception 
	{
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put("engine", "{\"facets\":[],\"mode\":\"row-based\"}");
		parameter.put("project", config.project);
		parameter.put("format", "rdf");
		
		URL url = new URL( config.url );
		URLConnection con = doPost(url, parameter);
		
    	InputStream is = con.getInputStream();
		res.addAll( readStatements(is, EndpointImpl.api().getNamespaceService().defaultNamespace(), RDFFormat.RDFXML) );
	}
	
	/**
	 * utility to simplify doing POST requests
	 * 
	 * @param url			URL to post to
	 * @param parameter		map of parameters to post
	 * @return				URLConnection is a state where post parameters have been sent
	 * @throws IOException
	 * 
	 * TODO: move to util class
	 */
	public static URLConnection doPost( URL url, Map<String, String> parameter ) throws IOException
	{
		URLConnection con = url.openConnection();
		con.setDoOutput( true );
		
		StringBuilder params = new StringBuilder();
		for(Entry<String, String> entry : parameter.entrySet())
			params.append(StringUtil.urlEncode(entry.getKey())).append("=").append(StringUtil.urlEncode(entry.getValue())).append("&");

		// Send data
		OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
		wr.write(params.toString());
		wr.flush();
		wr.close();
		
		return con;
	}
	
	/**
	 * read RDF from stream
	 * 
	 * @param is		input stream to read the statements from - stream will be closed
	 * @param baseUri	baseUri to use (prepend) when resolving relative URIs
	 * @param format	source RDF format
	 * @return			parsed list of statements
	 * @throws RepositoryException
	 * 
	 * TODO: move to util class
	 */
	public static List<Statement> readStatements( InputStream is, String baseUri, RDFFormat format ) throws RepositoryException
	{
	    Repository repository = new SailRepository(new MemoryStore());
	    repository.initialize();
    	ReadWriteDataManagerImpl dm = ReadWriteDataManagerImpl.openDataManager(repository);
    	dm.importRDFfromInputStream( is, baseUri, RDFFormat.RDFXML );
    	List<Statement> res = dm.getStatementsAsList(null, null, null, false);
    	IOUtils.closeQuietly( is );
    	dm.close();
    	return res;
	}
}
