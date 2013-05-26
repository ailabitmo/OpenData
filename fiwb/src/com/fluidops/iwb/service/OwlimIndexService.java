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

import java.lang.reflect.Field;

import org.apache.log4j.Logger;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPQueryEvaluationException;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.util.RepositoryFactory;
import com.fluidops.util.StringUtil;


/**
 * Service to update indices of an OWLIM repository
 * 
 * This class provides two methods that are accessible via the CodeExecutionWidget.
 * 
 * a) Invoke on Global.repository
 * <code> 
 * {{#widget: com.fluidops.iwb.widget.CodeExecutionWidget 
 * | label = 'Update Index'
 * | clazz = 'com.fluidops.iwb.service.OwlimIndexService'
 * | method = 'updateOwlimIndex'
 * | args = {{ 'repository' }}
 * | onFinish = 'none'
 * }}
 * </code>
 * 
 * b) Invoke on remote repository
 * 
 * <code> 
 * {{#widget: com.fluidops.iwb.widget.CodeExecutionWidget 
 * | label = 'Update Index' 
 * | clazz = 'com.fluidops.iwb.service.OwlimIndexService'
 * | method = 'updateOwlimIndex'
 * | args = {{ 'http://%host%/openrdf-sesame/' | '%repositoryName%' }}
 * | onFinish = 'none'
 * }}
 * 
 * @author as
 */
public class OwlimIndexService implements Service<OwlimIndexService.Config>{

	private static final Logger logger = Logger.getLogger(OwlimIndexService.class);

	public static class Config 
	{
		/**
		 * The (optional) name of the repository. If no name is given, we
		 * use Global.repository.
		 */
		public String repositoryName;
		
		/**
		 * The (optional) remote repository server. If no server is given,
		 * we lookup the repository by its name in Global.%repositoryName%.
		 */
		public String repositoryServer;
	}

	
	@Override
	public Object run(Config c) throws Exception {
		
		Repository repo = null;
		
		// if no repository name is given, use Global.repository
		if (StringUtil.isNullOrEmpty(c.repositoryName)) {
			repo = Global.repository;
		}
		// if no repository server is given, use Global.%repositoryName%
		else if (StringUtil.isNullOrEmpty(c.repositoryServer)) {
			try {
				Field f = Global.class.getField(c.repositoryName);
				repo = (Repository)f.get(null);
			} catch (Exception e) {				
				throw new IllegalArgumentException("RepositoryName does not refer to a valid (initialized) repository in Global.", e);
			}
		}
		else {
			// otherwise use remote repository
			repo = RepositoryFactory.getRemoteRepository(c.repositoryServer, c.repositoryName);
		}
		
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(repo);
		
		runOwlimConfigQuery(dm, "ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> " +
				"{ ?s <http://www.ontotext.com/owlim/RDFRank#compute> ?o }");
		logger.info("Computation of RDF ranks completed");
		
		// was uncommented in CLIWidget (why?)
//		runOwlimConfigQuery(dm, "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
//				"ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> { luc:exclude luc:setParam \"http://.*|GeoTagConcept\" . }");
		
		runOwlimConfigQuery(dm, "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
				"ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> { luc:languages luc:setParam \"en,none\" . }");
		
		runOwlimConfigQuery(dm, "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
				"ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> { luc:include luc:setParam \"uris, literals\" . }");
		
		runOwlimConfigQuery(dm, "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
				"ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> { luc:index luc:setParam \"literals, bnodes\" . } ");
		
		runOwlimConfigQuery(dm, "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
				"ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> { luc:useRDFRank luc:setParam \"yes\" . }");
		
		runOwlimConfigQuery(dm, "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
				"ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> { luc:moleculeSize luc:setParam \"1\" . }");
		
		runOwlimConfigQuery(dm, "PREFIX luc: <http://www.ontotext.com/owlim/lucene#> " +
				"ASK FROM <http://www.ontotext.com/owlim/cluster/control-query> { luc:luceneIndex luc:createIndex \"true\" . }");
		logger.info("Creation of Lucene index completed");
		
		return null;
	}
	
	/**
	 * Execute an owlim configuration query
	 * 
	 * @param dm
	 * @param query
	 * @throws Exception
	 */
	private void runOwlimConfigQuery(ReadDataManager dm, String query) throws QueryEvaluationException, RepositoryException, Exception {
		
		try {
			BooleanQuery q = (BooleanQuery)dm.prepareQuery(query, false, null, false);		
			if (!q.evaluate())
				throw new Exception("Owlim config query could not be evaluated successfully: " + query);
		} catch (HTTPQueryEvaluationException e) {
			if ("Accepted".equals(e.getMessage()))
				return;		// asynchronous handling (endpoint returns HTTP "Accept" as message)
			HTTPQueryEvaluationException error = new HTTPQueryEvaluationException("Error while updating owlim indices: " + e.getMessage());
			error.setStackTrace(e.getStackTrace());
			throw error;
		}
	}

	@Override
	public Class<Config> getConfigClass() {
		return Config.class;
	}
	
	
	/**
	 * Update OWLIM indices of a registered repository (in {@link Global})
	 * 
	 * @param repositoryName
	 * @throws Exception
	 */
	@CallableFromWidget
	public static void updateOwlimIndex(String repositoryName) throws Exception {
		updateOwlimIndex(null, repositoryName);
	}
	
	/**
	 * Update OWLIM indices of the provided repository. 
	 * 
	 * @param repositoryServer
	 * @param repositoryName
	 * @throws Exception
	 */
	@CallableFromWidget
	public static void updateOwlimIndex(String repositoryServer, String repositoryName) throws Exception {
		Config cfg = new Config();
		cfg.repositoryServer = repositoryServer;
		cfg.repositoryName = repositoryName;
		new OwlimIndexService().run(cfg);
	}
}
