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

package com.fluidops.iwb.api.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.provider.ProviderUtils;

/**
 * Triple editor source reloading URIs on demand. The triple editor source
 * is optimized for non-remote/federated settings. The heuristics used is to load 
 * data in chunks of 5x the requested size. This avoids caching of unnecessary
 * triples while typically yielding a good "overall" performance for the user.
 * 
 * @author msc
 */
public class TripleEditorSourceURIOnDemand implements TripleEditorSource, TripleEditorSourceURI 
{
    private static final Logger logger = Logger.getLogger(TripleEditorSourceURIOnDemand.class.getName());
	
	/**
	 * The value for which to deliver results
	 */
	private URI value;
	
	/**
	 * Size of triples per property shown in preview
	 */
	private int previewSize;
	
	/**
	 * Whether to display inverse properties or not
	 */
	boolean includeInverseProperties;
	
	/**
	 * Mapping between TEPIs and extractors supporting the extraction of
	 * statements for the TEPIs.
	 */
	private Map<TripleEditorPropertyInfo, CacheBasedTripleEditorStatementExtractor> tepi2extractor = null;

	
	private ReadDataManager dm = EndpointImpl.api().getDataManager();
	
	/**
	 * Default constructor, invoked by reflection call.
	 */
	public TripleEditorSourceURIOnDemand() 
	{
		super();
	}

	@Override
	public void initialize(URI value, int previewSize, boolean includeInverseProperties) throws QueryEvaluationException 
	{
		this.value = value;
		this.previewSize = previewSize;
		this.includeInverseProperties = includeInverseProperties;
		
		// initialize the internal map with constructors for the properties
		tepi2extractor = new HashMap<TripleEditorPropertyInfo, CacheBasedTripleEditorStatementExtractor>();
		
		List<TripleEditorPropertyInfo> props = computeProperties();
		for (TripleEditorPropertyInfo prop : props) 
		{
			if (prop.isOutgoingStatement())
			{
				tepi2extractor.put(prop,new OutgoingTripleEditorStatementExtractor(value, prop));				
			}
			else if (includeInverseProperties)
			{
				tepi2extractor.put(prop,new IncomingTripleEditorStatementExtractor(value, prop));								
			} // else: ignore statement (incoming statement display disabled)
		}
	}

	@Override
	public List<TripleEditorStatement> getStatementPreview()  throws QueryEvaluationException 
	{		
		List<TripleEditorStatement> res = new ArrayList<TripleEditorStatement>();
		for (TripleEditorPropertyInfo tepi : tepi2extractor.keySet())
		{
			res.addAll(getExtractorForTepi(tepi).computePreview(previewSize) );
		}
		return res;
	}

	@Override
	public List<TripleEditorStatement> getStatementsForProperty(
			TripleEditorPropertyInfo tepi, int offset, int limit)  throws QueryEvaluationException 
	{
		return getExtractorForTepi(tepi).getValues(offset, limit);
	}

	@Override
	public Set<TripleEditorPropertyInfo> getPropertyInfos()  throws QueryEvaluationException 
	{
		return tepi2extractor.keySet();
	}

	private CacheBasedTripleEditorStatementExtractor getExtractorForTepi(TripleEditorPropertyInfo tepi)
	{
		CacheBasedTripleEditorStatementExtractor extractor = tepi2extractor.get(tepi);
		if (extractor==null)
			throw new IllegalArgumentException("Property " + tepi.getUri() + " not known.");
		
		return extractor;
	}
	
	
	/**
	 * Computes all TripleEditorPropertyInfo objects
	 * @return
	 * @throws QueryEvaluationException
	 */
	private List<TripleEditorPropertyInfo> computeProperties() throws QueryEvaluationException 
	{	
		List<TripleEditorPropertyInfo> props = new ArrayList<TripleEditorPropertyInfo>();

		
		long start = 0;
		long end = 0;
		
		// calculates the types associated to the instance
		start = System.currentTimeMillis();
		Set<Value> typesOfInstance = new HashSet<Value>();
		RepositoryResult<Statement> typeStmts = null;
		try
		{
			typeStmts = dm.getStatements(value, RDF.TYPE, null, false);
			while (typeStmts.hasNext())
				typesOfInstance.add(typeStmts.next().getObject());
		} 
		catch (RepositoryException e) 
		{
			throw new RuntimeException(e);
		}
		finally
		{
			ReadDataManagerImpl.closeQuietly(typeStmts);
		}
		end = System.currentTimeMillis();
		logger.trace("Calculation of types for resource: " + (end-start) + "ms");
		
		start = System.currentTimeMillis();
		collectOutgoingTEPIs(props, typesOfInstance);
		end = System.currentTimeMillis();
		logger.trace("Calculation of outgoing TEPIs including range: " + (end-start) + "ms");
		
		// NOTE: this is quite expensive in the standard setting, as we have to
		// extract incoming properties including their ranges and set up TEPIs for them
		start = System.currentTimeMillis();
		collectIncomingTEPIs(props, typesOfInstance);
		end = System.currentTimeMillis();
		logger.trace("Calculation of incoming TEPIs including range: " + (end-start) + "ms");
		
		return props;
	}

	/**
	 * Collect TEPIs for incoming properties.
	 * 
	 * @param props list in which to add TEPIs
	 * @param typesOfInstance types of the instance, used for range intersection
	 * 
	 * @throws QueryEvaluationException
	 */
	private void collectIncomingTEPIs(List<TripleEditorPropertyInfo> props,
			Set<Value> typesOfInstance) throws QueryEvaluationException 
	{
		String queryIncoming = "SELECT ?pred ?ran WHERE { " +
				" { SELECT DISTINCT ?pred WHERE { ?x ?pred " + ProviderUtils.uriToQueryString(value) + " } } " +
				" OPTIONAL { ?pred rdfs:range ?ran } " +
				"}";
		TupleQueryResult qResIncoming = null;
		try 
		{
			Map<URI,Set<Value>> predToRange = new HashMap<URI,Set<Value>>();

			qResIncoming = dm.sparqlSelect(queryIncoming, true);
			while (qResIncoming.hasNext()) 
			{
				BindingSet bs = qResIncoming.next();
				URI pred = (URI)bs.getValue("pred");
				Value range = bs.getValue("ran");
				
				if (!predToRange.containsKey(pred))
					predToRange.put(pred, new HashSet<Value>());
				
				if (range!=null)
					predToRange.get(pred).add(range);
			}
			
			
			for (Entry<URI, Set<Value>> predEntry : predToRange.entrySet())
			{
				Set<Value> clusteredResources = predEntry.getValue();
				clusteredResources.retainAll(typesOfInstance);

				if (clusteredResources.isEmpty())
					clusteredResources.add(TripleEditorConstants.getDefaultClusteredResourceIncoming());
				else
					clusteredResources = TripleEditorConstants.getClusteredResourceIncoming(clusteredResources);
				
				props.add( new TripleEditorPropertyInfo(predEntry.getKey(), clusteredResources, false));
			}			
		} 
		catch (MalformedQueryException e) 
		{
			throw new RuntimeException(e);
		} 
		finally 
		{
			ReadDataManagerImpl.closeQuietly(qResIncoming);
		}
	}

	/**
	 * Collect TEPIs for outgoing properties.
	 * 
	 * @param props list in which to add TEPIs
	 * @param typesOfInstance types of the instance, used for range intersection
	 * 
	 * @throws QueryEvaluationException
	 */
	private void collectOutgoingTEPIs(List<TripleEditorPropertyInfo> props,
			Set<Value> typesOfInstance) throws QueryEvaluationException 
	{
		String queryOutgoing = "SELECT ?pred ?dom WHERE { " +
				" { SELECT DISTINCT ?pred WHERE { " + ProviderUtils.uriToQueryString(value) + " ?pred ?x } } " +
				" OPTIONAL { ?pred rdfs:domain ?dom }  " +
				"}";
		TupleQueryResult qResOutgoing = null;
		try 
		{
			Map<URI,Set<Value>> predToDomain = new HashMap<URI,Set<Value>>();
			
			qResOutgoing = dm.sparqlSelect(queryOutgoing, true);
			while (qResOutgoing.hasNext()) 
			{
				BindingSet bs = qResOutgoing.next();
				URI pred = (URI)bs.getValue("pred");
				Value domain = bs.getValue("dom");
				
				if (!predToDomain.containsKey(pred))
					predToDomain.put(pred, new HashSet<Value>());
				
				if (domain!=null)
					predToDomain.get(pred).add(domain);
			}

			for (Entry<URI, Set<Value>> predEntry : predToDomain.entrySet())
			{
				Set<Value> clusteredResources = predEntry.getValue();
				clusteredResources.retainAll(typesOfInstance);
				if (clusteredResources.isEmpty())
					clusteredResources.add(TripleEditorConstants.getDefaultClusteredResourceOutgoing());
				else
					clusteredResources = TripleEditorConstants.getClusteredResourceOutgoing(clusteredResources);
				
				props.add( new TripleEditorPropertyInfo(predEntry.getKey(), clusteredResources, true));
			}
		} 
		catch (MalformedQueryException e) 
		{
			throw new RuntimeException(e);
		} 
		finally 
		{
			ReadDataManagerImpl.closeQuietly(qResOutgoing);
		}
	}

	/**
	 * Abstract superclass for extracting triple editor statements related to a TEPI.
	 */
	public abstract class CacheBasedTripleEditorStatementExtractor
	{
		
		/**
		 *  cache for TripleEditorStatements, to be filled incrementally 
		 */
		protected List<TripleEditorStatement> cache;

		/**
		 * true if cache is (known to be) complete
		 */
		protected boolean cacheComplete = false;

		/**
		 * Value for which we collect triple editor statements
		 */
		protected URI value; 

		/**
		 * The tepi for which we collect triple editor statements
		 */
		protected TripleEditorPropertyInfo tepi;
		
		public CacheBasedTripleEditorStatementExtractor(URI value, TripleEditorPropertyInfo tepi)
		{
			this.value = value;
			this.tepi = tepi;
			cache = new LinkedList<TripleEditorStatement>();
		}
		
		public abstract List<TripleEditorStatement> computePreview(int previewSize)
		throws QueryEvaluationException;
		
		public abstract List<TripleEditorStatement> getValues(int offset, int limit)
		throws QueryEvaluationException;
		
		/**
		 * Returns the sublist for the given offset & limit specification, assuming
		 * the internal cache is initialized and complete w.r.t. the requests limits.
		 * 
		 * @param offset
		 * @param limit
		 * @return a copy of the relevant sublist from the cache datastructure
		 */
		protected List<TripleEditorStatement> getValidSublistFromCache(int offset, int limit)
		{
			// we need to adjust limits whenever values are requested that do not exist
			int eff_offset = Math.min(cache.size(), offset);
			int eff_limit = cache.size(); // fallback: return all if no limit is specified
			if (limit>=0)
				eff_limit = Math.min(cache.size(),offset+limit);

			return new ArrayList<TripleEditorStatement>(cache.subList(eff_offset, eff_limit));
		}
		
		/**
		 * Decides whether the request can be answered form the cache or whether new
		 * triples need to be extracted.
		 * 
		 * @param offset
		 * @param limit
		 * @return
		 */
		protected boolean canAnswerFromCache(int offset, int limit)
		{
			if (cacheComplete)
				return true;
			
			// otherwise, we can answer from cache if the query is limited and
			// the request falls into the given range
			return limit>=0 && cache.size()>offset+limit;
		}
	}
	
	/**
	 * Extractor for extracting incoming triple editor statements related to a TEPI.
	 */
	public class IncomingTripleEditorStatementExtractor extends CacheBasedTripleEditorStatementExtractor
	{
		public IncomingTripleEditorStatementExtractor(URI value, TripleEditorPropertyInfo tepi)
		{
			super(value,tepi);
		}

		@Override
		public List<TripleEditorStatement> computePreview(int previewSize) 
		throws QueryEvaluationException
		{
			long start = System.currentTimeMillis();
			List<TripleEditorStatement> res =  getValues(0,previewSize);
			long end = System.currentTimeMillis();
			logger.trace("Calculated preview for incoming property " + value + " in " + (end-start) + "ms");
			
			return res;
		}
		
		@Override
		public List<TripleEditorStatement> getValues(int offset, int limit) throws QueryEvaluationException
		{
			long start = System.currentTimeMillis();
			if (!canAnswerFromCache(offset,limit))
			{
				List<TripleEditorStatement> stmts = new ArrayList<TripleEditorStatement>();
				
				// extract 5x more triples then requested to accelerate iterative user requests
				// (next time, the triples will be taken from the cache)
				int nrRequestedStatements = offset+5*limit;
				String query =  "SELECT DISTINCT ?s ?c WHERE { " +
												    "  ?s %PRED% %NODE% . " +
													"	OPTIONAL { GRAPH ?c { ?s %PRED% %NODE% } } " +
													"} ORDER BY ?s ?c OFFSET " + cache.size();
				
				if (limit>=0)
					query += " LIMIT " + nrRequestedStatements;
				
				query = query.replace("%NODE%", ProviderUtils.uriToQueryString(value));
				query = query.replace("%PRED%", ProviderUtils.uriToQueryString(tepi.getUri()));
				
				TupleQueryResult qRes = null;
				try 
				{
					qRes = dm.sparqlSelect(query, true);
					while (qRes.hasNext()) 
					{
						BindingSet tuple = qRes.next();
	
						Resource context = tuple.getBinding("c") != null ? (Resource) tuple.getBinding("c").getValue() : null;
						stmts.add(new TripleEditorStatement((Resource)tuple.getBinding("s").getValue(), tepi.getUri(), value, context, tepi));
					}
				}
				catch (MalformedQueryException e) 
				{
					throw new RuntimeException(e);
				} 
				finally 
				{
					ReadDataManagerImpl.closeQuietly(qRes);
				}
				
				cache.addAll(stmts);
				if (stmts.size()<nrRequestedStatements)
					cacheComplete = true;
			}
			
			long end = System.currentTimeMillis();
			logger.trace("Calculated requested values for outgoing property " + value + " in " + (end-start) + "ms");

			return getValidSublistFromCache(offset,limit);
		}
	}
	
	/**
	 * Extractor for extracting outgoing triple editor statements related to a TEPI.
	 */
	public class OutgoingTripleEditorStatementExtractor extends CacheBasedTripleEditorStatementExtractor
	{	
		public OutgoingTripleEditorStatementExtractor(URI value, TripleEditorPropertyInfo tepi)
		{
			super(value,tepi);
		}

		@Override
		public List<TripleEditorStatement> computePreview(int previewSize) 
		throws QueryEvaluationException
		{
			long start = System.currentTimeMillis();
			List<TripleEditorStatement> res = getValues(0,previewSize);
			long end = System.currentTimeMillis();
			logger.trace("Calculated preview for outgoing property " + value + " in " + (end-start) + "ms");


			return res;
		}

		@Override
		public List<TripleEditorStatement> getValues(int offset, int limit) 
		throws QueryEvaluationException
		{
			long start = System.currentTimeMillis();
			
			if (!canAnswerFromCache(offset,limit))
			{
				List<TripleEditorStatement> stmts = new ArrayList<TripleEditorStatement>();
				
				// extract 5x more triples then requested to accelerate iterative user requests
				// (next time, the triples will be taken from the cache)
				int nrRequestedStatements = offset+5*limit;
				String query =  "SELECT DISTINCT ?o ?c WHERE { " +
												    "   %NODE% %PRED% ?o . " +
													"	OPTIONAL { GRAPH ?c { %NODE% %PRED% ?o } } " +
													"} ORDER BY ?o ?c OFFSET " + cache.size();
				if (limit>=0)
					query += " LIMIT "+ nrRequestedStatements;
				
				query = query.replace("%NODE%", ProviderUtils.uriToQueryString(value));
				query = query.replace("%PRED%", ProviderUtils.uriToQueryString(tepi.getUri()));
				
				TupleQueryResult qRes = null;
				try 
				{
					qRes = dm.sparqlSelect(query, true);
					while (qRes.hasNext()) 
					{
						BindingSet tuple = qRes.next();
	
						Resource context = tuple.getBinding("c") != null ? (Resource) tuple.getBinding("c").getValue() : null;
						stmts.add(new TripleEditorStatement(value, tepi.getUri(),
								tuple.getBinding("o").getValue(), context, tepi));
					}
				}
				catch (MalformedQueryException e) 
				{
					throw new RuntimeException(e);
				} 
				finally 
				{
					ReadDataManagerImpl.closeQuietly(qRes);
				}
				
				cache.addAll(stmts);
				if (stmts.size()<nrRequestedStatements)
					cacheComplete = true;
			}
			
			long end = System.currentTimeMillis();
			logger.trace("Calculated requested values for outgoing property " + value + " in " + (end-start) + "ms");

			return getValidSublistFromCache(offset,limit);
		}
	}
}
