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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

@TypeConfigDoc("The Value Based Data Mapping Provider allows for data reconciliation and mapping based on shared properties and paths of URIs.")
public class ValueBasedDataMappingProvider extends DataMappingProvider<ValueBasedDataMappingProvider.Config>
{	
	private static final long serialVersionUID = 1888830588586691656L;
	
	protected static final Logger logger = Logger.getLogger(ValueBasedDataMappingProvider.class.getName());
    
	private static MatchType MATCH_TYPE_DEFAULT = MatchType.EXACT;
	
    public static enum MatchType
    {
    	EXACT,			// match if the value set is identical
    	SUBSET,			// match if the value set of the source query is a subset of the destination query's value set
    	SUPERSET,		// match if the value set of the source query is a superset of the destination query's value set
    	JOINT_MEMBER	// match if the value set of the source query contains a joint member with the value set of the destination query
    };
	
    /**
     * Configuration for SILK provider, which allows to specify SILK plus additional
     * behavior of the provider. 
     */
	public static class Config implements Serializable
	{
		private static final long serialVersionUID = -6368312488826393588L;

		@ParameterConfigDoc(
				desc = "Query selecting the source key-value pairs from the database. The query must contain exactly two projection variables, ?key and ?value. ",
				required = true,
				type = Type.TEXTAREA)
		public String sourceQuery;

		@ParameterConfigDoc(
				desc = "Query selecting the target key-value pairs from the database. The query must contain exactly two projection variables, ?key and ?value. ",
				required = true,
				type = Type.TEXTAREA)
		public String targetQuery;
		
		@ParameterConfigDoc(
				desc = "Identify keys if the value set of the source query is identical (EXACT), " +
						"a subset (SUBSET)/superset (SUPERSET), or contains a joint member " +
						"(JOINT_MEMBER) with the destination query's value set. Default is EXACT.",
				required = false,
				type = Type.DROPDOWN)
		public MatchType matchType;
	    
	    @ParameterConfigDoc(
	    		desc="URI representing the link type, default: owl:sameAs",
	    		required = false)
	    public String linkType;
		
	    // additional configuration
	    @ParameterConfigDoc(
	    		desc = "How is the mapping applied to the RDF store",
	    		required = true,
	    		type = Type.DROPDOWN)
	    public MappingType mappingType;
	}

	@Override
	public MappingType gatherMapping(final List<Statement> res) throws Exception
	{	
		Map<URI,Set<Value>> key2vals1 = new HashMap<URI,Set<Value>>();
		Map<URI,Set<Value>> key2vals2 = new HashMap<URI,Set<Value>>();
		Map<URI,URI> mapping = new HashMap<URI,URI>(); 
		
		// read config values
		MatchType matchType = config.matchType;
		if (matchType==null)
			matchType = MATCH_TYPE_DEFAULT;
		
		URI linkType = (StringUtil.isNullOrEmpty(config.linkType)) ? OWL.SAMEAS : 
			EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(config.linkType);
		
		logger.info("Computing buckets for the source and destination query");
		computeBuckets(config.sourceQuery,config.targetQuery,key2vals1,key2vals2);
		
		logger.info("Calculating mapping (this may take a while)...");
		computeMapping(key2vals1,key2vals2,matchType,mapping);
		
		logger.info("Transforming mapping to statements");
		DataMappingProvider.mappingToStmts(mapping,linkType,res);

		return config.mappingType;
	}

	/**
	 * Computes the buckets with key-value pairs
	 * 
	 * @param key2Val
	 * @param queries
	 * @param value
	 * @return
	 * @throws Exception
	 */
	private static String computeBuckets(String query1, String query2, 
			Map<URI,Set<Value>> key2Vals1, Map<URI,Set<Value>> key2Vals2) throws Exception
	{
		String out = "Computing buckets now...<br/>";
		
		ReadDataManager dm = EndpointImpl.api().getDataManager();
		
		logger.debug("Bucket #1:<br/>");
		computeBucket(key2Vals1,query1,dm);
		logger.debug("Bucket #2:<br/>");
		computeBucket(key2Vals2,query2,dm);

		return out;
	}
	
	/**
	 * Computes a key-set(value) bucket for a certain query, adding the result to
	 * the key2Val list in the form of a list element.
	 * @param key2Val
	 * @param query
	 * @param value
	 * @param dm
	 * @return
	 * @throws Exception
	 */
	private static void computeBucket(Map<URI,Set<Value>> key2vals,
			String query, ReadDataManager dm) throws Exception
	{
		TupleQueryResult res = dm.sparqlSelect(query, true);
		
		int ctr = 0;
		while (res.hasNext())
		{
			BindingSet bs = res.next();
			try
			{
				URI key = (URI)bs.getBinding("key").getValue();
				Value val = bs.getBinding("value").getValue();
				
				Set<Value> set = key2vals.get(key);
				if (set==null)
				{
					set= new HashSet<Value>();
					key2vals.put(key,set);
				}
				set.add(val);
				ctr++;
			}
			catch (Exception e)
			{
				logger.info(e.getMessage());
			}
		}
		logger.debug("-> extracted " + key2vals.keySet().size() + 
						" keys, mapping to " + ctr + " values<br/>");

		Set<Pair<Set<Value>,Set<Value>>> duplicateValues = 
				new HashSet<Pair<Set<Value>,Set<Value>>>();
		for (Entry<URI, Set<Value>> entry : key2vals.entrySet())
		{
			Set<Value> val = entry.getValue();
			
			boolean contained = false;
			for (Pair<Set<Value>,Set<Value>> s : duplicateValues)
			{
				if (s.snd.equals(val))
				{
					contained = true;
					s.fst.add(entry.getKey()); // duplicate found
				}
			}
			
			// initial insert
			if (!contained)
			{
				Set<Value> keys = new HashSet<Value>();					
				Set<Value> values = new HashSet<Value>();
				
				keys.add(entry.getKey());
				values.addAll(val);
				Pair<Set<Value>,Set<Value>> elem = new Pair<Set<Value>,Set<Value>>(keys,values);
				duplicateValues.add(elem);
			}
		}
		
		// count duplicates
		int dupCount = 0;
		Set<URI> toRemove = new HashSet<URI>();
		for (Pair<Set<Value>,Set<Value>> elems : duplicateValues)
		{
			if (elems.fst.size()>1)
			{
				dupCount++;
				
				// remove from key2vals array
				for (Entry<URI, Set<Value>> entry : key2vals.entrySet())
				{
					Set<Value> vals = entry.getValue();;
					if (vals.equals(elems.snd))
					{
						toRemove.add(entry.getKey());
					}
				}
				
				if (logger.isDebugEnabled()) {
					StringBuilder logMsg = new StringBuilder("'''");
					for (Value v : elems.fst)
					{
						logMsg.append(" ");
						logMsg.append("<a href=\"" + EndpointImpl.api().getRequestMapper().getRequestStringFromValue(v) + "\">" + v.toString() + "</a>");
					}
					logMsg.append(" all mapping to");
					for (Value v : elems.snd)
					{
						logMsg.append(" ");
						logMsg.append("<a href=\"" + EndpointImpl.api().getRequestMapper().getRequestStringFromValue(v) + "\">" + v.toString() + "</a>");
					}
					logger.debug(logMsg);
				}
			}
		}
		// remove the ambigous keys
		for (URI key : toRemove)
			key2vals.remove(key);
				
		logger.debug("-> duplicate value sets:  " + dupCount + "<br/>");
		logger.debug("-> removed key-value pairs:  " + toRemove.size() + "<br/>");
		logger.debug("-> remaining: " + key2vals.keySet().size() + " keys<br/>");
	}
	
	
	/**
	 * Computes mappings between key-value sets.
	 * 
	 * @param key2vals1
	 * @param key2vals2
	 * @param matchType
	 * @param mapping
	 * @return
	 */
	private static void computeMapping(
			Map<URI,Set<Value>> key2vals1,Map<URI,Set<Value>> key2vals2,
			MatchType matchType,Map<URI,URI> mapping)
	{
		computeMapping(key2vals1, key2vals2, matchType, mapping, false);
	}
	
	/**
	 * Help method with additional parameter for inverting 
	 * (allows for non-redundant SUBSET/SUPERSET handling).
	 */
	private static void computeMapping(
			Map<URI,Set<Value>> key2vals1,Map<URI,Set<Value>> key2vals2,
			MatchType matchType,Map<URI,URI> mapping, boolean invert)
	{
		if (matchType.equals(MatchType.EXACT))
		{
			for (Entry<URI, Set<Value>> entry1 : key2vals1.entrySet())
			{
				Set<Value> vals1 = entry1.getValue();
				
				for (Entry<URI, Set<Value>> entry2 : key2vals2.entrySet())
				{
					Set<Value> vals2 = entry2.getValue();
					
					if (vals1.equals(vals2))
					{
						appendMapping(mapping, entry1.getKey(), entry2.getKey(), invert);
						break; // matched
					}
				}
			}
		}
		else if (matchType.equals(MatchType.SUBSET))
		{
			for (Entry<URI, Set<Value>> entry1 : key2vals1.entrySet())
			{
				Set<Value> vals1 = entry1.getValue();
				
				for (Entry<URI, Set<Value>> entry2 : key2vals2.entrySet())
				{
					Set<Value> vals2 = entry2.getValue();
					
					boolean containsAll = true;
					for (Value subsetValue : vals1)
					{
						containsAll &= vals2.contains(subsetValue);
						if (!containsAll)
							break;
					}
					
					if (containsAll)
					{
						appendMapping(mapping, entry1.getKey(), entry2.getKey(), invert);
						break; // matched
					}
				}
			}
		}
		else if (matchType.equals(MatchType.SUPERSET))
		{
			computeMapping(key2vals2,key2vals1,MatchType.SUBSET,mapping,!invert);
		}
		else if (matchType.equals(MatchType.JOINT_MEMBER))
		{
			for (Entry<URI, Set<Value>> entry1 : key2vals1.entrySet())
			{
				Set<Value> vals1 = entry1.getValue();
				
				for (Entry<URI, Set<Value>> entry2 : key2vals2.entrySet())
				{
					Set<Value> vals2 = entry2.getValue();
					
					Set<Value> intersection = new HashSet<Value>();
					intersection.addAll(vals1);
					intersection.retainAll(vals2);
					
					if (!intersection.isEmpty())
						appendMapping(mapping, entry1.getKey(), entry2.getKey(), invert);
				}
			}
		}

		// write debug output
		for (Entry<URI, URI> entry : mapping.entrySet())
		{
			URI u = entry.getKey();
			logger.debug(
				"<a href=\"" + EndpointImpl.api().getRequestMapper().getRequestStringFromValue(u)
				+ "\">" + u.toString()  + "</a> "
				+ " (" + EndpointImpl.api().getRequestMapper().getAHrefFromValue(u) + ") "
				+ " -> "
				+ "<a href=\"" + EndpointImpl.api().getRequestMapper().getRequestStringFromValue(entry.getValue()) + "\">" 
				+ mapping.get(u).toString() 
				+ "</a>"
				+ " (" + EndpointImpl.api().getRequestMapper().getAHrefFromValue(entry.getValue()) + ") "
				+ "<br/>");
		}
	}

	private static void appendMapping(Map<URI, URI> mapping, URI key1, URI key2, boolean invert) 
	{
		if (invert)
			mapping.put(key2, key1);
		else
			mapping.put(key1, key2);
	}


	@Override
	public Class<? extends Config> getConfigClass()
	{
		return Config.class;
	}
}
