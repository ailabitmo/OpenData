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

package com.fluidops.iwb.api;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.provider.RDFProvider;
import com.fluidops.iwb.util.Config;


/**
 * Utility to manage distributions of datasets at runtime:
 *       
 * @author as
 *
 */
public class DatasourceManager {
	
	protected static final Logger log = Logger.getLogger(DatasourceManager.class);
	

	/**
	 * Depending on whether federated mode is active or not, different actions are performed.<p>
	 * 
	 * a) Centralized store:<p>
	 * 		- integrate data using RDFProvider
	 * 
	 * b) federated mode:<p>
	 * 		- SPARQL or local NativeStore: integrate into federation <br />
	 *      - remote Sail: copy to local disk, and integrate (TODO) <br />
	 *      
	 * Note:
	 * For the federated mode it is required to have fedx.jar and the iwb-bridge (i.e. FedXBridge)
	 * on the class path.
	 *      
	 *             
	 * @param datasourceName
	 * 				an identifier for the datasource
	 * @param datasource
	 * 				the datasource, e.g. a URI for SPARQL endpoints or a location for a local repository
	 * @param format
	 * 				the format: {api/sparql, application/x-ntriples, application/.., lsail/NativeStore, sail
	 * 
	 * @throws IllegalArgumentException
	 * 				if an unsupported format is specified
	 * @throws Exception 
	 * @throws RemoteException 
	 */
	public static void integrateDataSource(String datasourceName, String datasource, String format) throws IllegalArgumentException,RemoteException, Exception {
		
		if (Config.getConfig().isFederation()) {
			
			log.debug("Integrating data source '" + datasourceName + "' using FedXBridge ...");
			
			/* federated mode: try to add to federation */
			// TODO fix this
			
			// XXX use reflection to avoid having fedx on the class path for the beginning
			Class<?> fedxUtil = Class.forName("com.fluidops.iwb.federation.FedXBridge");
			Method method = fedxUtil.getMethod("addDatasourceToFederation", new Class<?>[] {String.class, String.class, String.class});
			Object[] args = new Object[] {datasourceName, datasource, format};
			method.invoke(null, args);
			
		} else {
			
			log.debug("Integrating data source '" + datasourceName + "' using RDFProvider ...");
			
			/* use RDFProvider to load into centralized store */
			RDFProvider.Config config = new RDFProvider.Config();
			config.url = datasource;
			config.format = format;
			
			EndpointImpl.api().getProviderService().addProvider( 
					ValueFactoryImpl.getInstance().createURI(datasource), 
					RDFProvider.class.getName(), 
					60*24, 
					config,
					null
			);	
		}		
	}
	
	
	
	/**
	 * Test the provided data source for data.
	 * 
	 * @param datasourceName
	 * 				an identifier for the datasource
	 * @param datasource
	 * 				the datasource, e.g. a URI for SPARQL endpoints or a location for the data dump
	 * @param format
	 * 				the format: {api/sparql, application/x-ntriples, application/*}
	 * 
	 * @return
	 * @throws Exception
	 */
	public static List<Statement> testDataSource(String datasourceName, String datasource, String format) throws Exception {
		
		log.debug( "Testing source '" + datasourceName + "' for data using RDFProvider." );
		RDFProvider lod = new RDFProvider();
		
		List<Statement> res = new ArrayList<Statement>();
		lod.config = new RDFProvider.Config();
		lod.config.url = datasource;
		lod.gather( res );
		
		return res;
	}
	
	
		
}
