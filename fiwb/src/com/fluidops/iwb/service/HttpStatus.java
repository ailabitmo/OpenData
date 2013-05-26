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

package com.fluidops.iwb.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.Function;



/**
 * Service to retrieve http status of a given url.
 * 
 * Can be called as {@link Function} from SPARQL queries.
 * 
 * - namespace definition: service=http://www.fluidops.com/service/
 * - arguments: the url that is evaluated as http GET (e.g. http://iwb.fluidops.com:7883)
 * 
 * Example Query:
 * <code>
 * SELECT ?hostUrl (service:httpStatus(str(?hostUrl)) AS ?status) WHERE { ?? :hostUrl ?hostUrl }
 * </code>
 * 
 * To use custom functions in sesame, the class must be registered in META-INF/services
 * 
 * @author as
 * @see Function
 */
public class HttpStatus implements Service<HttpStatus.Config>, Function
{

	public static final String NAMESPACE = "http://www.fluidops.com/service/";
	public static final String OK = "OK";
	public static final String DOWN = "DOWN";
	
	public static class Config 
	{
		public String hostUrl;
		
		/**
		 * timeout an int that specifies the connect timeout value in milliseconds
		 */
		public int timeout = 3000;
	}

	@Override
	public Object run(Config in) throws IOException
	{
		URL url = new URL(in.hostUrl);
		
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setConnectTimeout(in.timeout);
		
		try {
			conn.connect();
			int response = conn.getResponseCode();
			
			if (response==HttpURLConnection.HTTP_OK) 
				return OK;
			
			return "HTTP"+response;
		} catch (IOException e) {
			return DOWN;
		} finally {
			conn.disconnect();
		}
	}

	@Override
	public Class<Config> getConfigClass()
	{
		return Config.class;
	}

	@Override
	public String getURI()
	{
		return NAMESPACE + "httpStatus";
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args)
			throws ValueExprEvaluationException
	{
		if (args.length==0) 
			throw new ValueExprEvaluationException("Expected the hostUrl or host+port as arguments");

		Config config = new Config();
		
		// argument is the entire hostUrl which is used for connection
		if (args.length==1)
			config.hostUrl = args[0].stringValue();
		
		// argument is the ip/host + the port
		else if (args.length==2)
			config.hostUrl = "http://" + args[0].stringValue() + ":" + args[1].stringValue() + "/";
		
		Object result = null;
		
		try
		{
			result = run(config);			
		}
		catch (IOException e) 
		{
			throw new ValueExprEvaluationException("Error evaluating HttpStatus for " + Arrays.toString(args), e);
		}
		
		return valueFactory.createLiteral(result.toString());		
	}
	
	
}
