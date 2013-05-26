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

package com.fluidops.iwb.facets;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.page.SearchPageContext;
import com.fluidops.util.Rand;

public class FacetCalculator {
    private static final Logger logger = Logger.getLogger(FacetCalculator.class.getName());

	private SearchPageContext pc;
	private String facetQuery = "";
	
	int facetCount = Rand.nextBoundedInt(1, 10000);

	public FacetCalculator(SearchPageContext pc) {
		this.pc = pc;
	}

	public FContainer getFacetContainer() {
		
		Map<URI, Vector<Value>> facets = null;
		
		FContainer allFacetContainer = new FContainer("all" + Rand.getIncrementalFluidUUID());
		allFacetContainer.setClazz("allFacets");
		
		FLabel titleLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Refine Search Result");
		titleLabel.setClazz("facettedSearchTitle");
		allFacetContainer.add(titleLabel);
		
		if ( pc.queryType.equals("CONSTRUCT")) {
			facets = facetsForConstruct();
		} else if (pc.queryType.equals("SELECT")) {
			facets = facetsForSelect();
		}
		else if (pc.queryType.equals("KEYWORD")) {
			facets = facetsForKeyword();
		}
		else {
			facets = new HashMap<URI, Vector<Value>>();
		}
		
		boolean active = true;
		int displayedFacets = 0;
		for (Entry<URI, Vector<Value>> entry : facets.entrySet()) {
			
			URI facetName = entry.getKey();
			FacetContainer facetContainer = new FacetContainer("facetContainer" + Rand.getIncrementalFluidUUID(), facetName.getLocalName(), active);
//			facetContainer.setActive(active);

			FContainer facetValueContainer = new FContainer("facetValue01");
			facetValueContainer.setClazz("facetValues");

			Set<String> facetValues = new HashSet<String>();
			
			for (Value v : entry.getValue()) {

				if (!facetValues.contains(v.stringValue())) {
					facetValues.add(v.stringValue());
					
					String facetDisplayString = (v instanceof URI) ? ((URI)v).getLocalName() : v.stringValue();

					FLabel facetVal = new FLabel("fVal" + Rand.getIncrementalFluidUUID(), facetDisplayString, "", true) {

						@Override
						public void onClick() {
							String value = "";
							if (facetQuery.contains("?facetPred"))
								facetQuery = facetQuery.replace("?facetPred" + facetCount, "<" + this.value + ">");
							else
								facetQuery = facetQuery.substring(0, facetQuery.lastIndexOf("<")+1) + this.value + facetQuery.substring(facetQuery.lastIndexOf(">"));
							try {
								value = ValueFactoryImpl.getInstance().createURI((String)this.name).stringValue();
								String updateString = "document.location='?q=" + URLEncoder.encode(facetQuery.substring(0, facetQuery.lastIndexOf("?")) + "<" + value + "> }", "UTF-8") + "'"; 
								addClientUpdate(new FClientUpdate(updateString));
							}
							catch (Exception e) {
								addClientUpdate(new FClientUpdate("document.location='?q=" + facetQuery.substring(0, facetQuery.lastIndexOf("}")-1) + ". FILTER (?facetObj" + facetCount + " = \"" + this.name + "\") }" +  "'"));
							}
							
							super.onClick();
						}
					};
					facetVal.value = facetName.stringValue();
					facetVal.name = v.stringValue();

					facetVal.setClazz("facetValue");
					facetValueContainer.add(facetVal);
				}
			}

			facetContainer.addInternal(facetValueContainer);
			if (displayedFacets ++ > 5)
				active = false;

			facetContainer.drawAdvHeader(true);
			allFacetContainer.add(facetContainer);
			
		}

		return allFacetContainer;
	}
	
	private Map<URI, Vector<Value>> facetsForKeyword() {
		
		Map<URI, Vector<Value>> facets = new HashMap<URI, Vector<Value>>();
		
		try {
			RepositoryConnection con = Global.repository.getConnection();
			
	        while (pc.queryResult.hasNext()) {
	            BindingSet bindingSet = (BindingSet) pc.queryResult.next();
	            String uri = bindingSet.getValue("Subject").stringValue();
	            
				String queryString = "SELECT ?y ?z WHERE { <" + uri + "> ?y ?z }";
				TupleQuery query = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				
				TupleQueryResult queryRes = query.evaluate();
				
				while (queryRes.hasNext()) {

					BindingSet s = queryRes.next();

					Binding predicateBinding = s.getBinding("y");
					URI predicate = (URI )predicateBinding.getValue();
					
					Vector<Value> facet = null;
					if (facets.containsKey(predicate))
						facet = facets.get(predicate);
					else {
						facet = new Vector<Value>();
						facets.put(predicate, facet);
					}
					facet.add(s.getBinding("z").getValue());

				}
			}
			
		} catch (RepositoryException e1) {
			logger.error(e1.getMessage(), e1);
		} catch (MalformedQueryException e) {
			logger.error(e.getMessage(), e);
		} catch (QueryEvaluationException e) {
			logger.error(e.getMessage(), e);
		} 
		return facets;
	}

	private Map<URI, Vector<Value>> facetsForSelect() {

		TupleQuery query;
		Map<URI, Vector<Value>> facets = new HashMap<URI, Vector<Value>>();

		try {
			RepositoryConnection con = Global.repository.getConnection();
			try {
				
				String uriBinding = pc.query.substring(pc.query.indexOf("?")+1, pc.query.indexOf(" ", pc.query.indexOf("?")));
				String parsedFacetQuery = pc.query.substring(0, pc.query.lastIndexOf("}")) + ". ?" + uriBinding + " ?facetPred" + facetCount + " ?facetObj" + facetCount + " }";
				facetQuery = parsedFacetQuery;				
				parsedFacetQuery = parsedFacetQuery.replace("SELECT", "SELECT ?facetPred" + facetCount + " ?facetObj" + facetCount +" ");
				
				query = con.prepareTupleQuery(QueryLanguage.SPARQL, parsedFacetQuery);

				TupleQueryResult queryRes = (TupleQueryResult) query.evaluate();

				// fix msc: graph query result may contain duplicate statements
				// (actually a bug/feature of Sesame), so we filter them out
				
				while (queryRes.hasNext()) {

					BindingSet s = queryRes.next();

					Binding predicateBinding = s.getBinding("facetPred" + facetCount);
					URI predicate = (URI )predicateBinding.getValue();
					
					Vector<Value> facet = null;
					if (facets.containsKey(predicate))
						facet = facets.get(predicate);
					else {
						facet = new Vector<Value>();
						facets.put(predicate, facet);
					}
					facet.add(s.getBinding("facetObj" + facetCount).getValue());

				}

				
			} finally {
				con.close();
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	
		return facets;
	}

	private Map<URI, Vector<Value>> facetsForConstruct() {

		GraphQuery query;
		Map<URI, Vector<Value>> facets = new HashMap<URI, Vector<Value>>();
		
		String uriBinding = pc.query.substring(pc.query.indexOf("?")+1, pc.query.indexOf(" ", pc.query.indexOf("?")));
		
		String parsedFacetQuery = pc.query.substring(0, pc.query.lastIndexOf("}")) + ". ?" + uriBinding + " ?facetPred" + facetCount + " ?facetObj" + facetCount + " }";
		facetQuery = parsedFacetQuery;	
		// TODO this is a dead store, fix appropriately
//		parsedFacetQuery = parsedFacetQuery.replace("SELECT", "SELECT ?facetPred" + facetCount + " ?facetObj" + facetCount +" ");
		

		try {
			RepositoryConnection con = Global.repository.getConnection();
			try {
				query = con.prepareGraphQuery(QueryLanguage.SPARQL, facetQuery);

				GraphQueryResult queryRes = (GraphQueryResult) query.evaluate();

				// fix msc: graph query result may contain duplicate statements
				// (actually a bug/feature of Sesame), so we filter them out
				Set<Statement> queryResNoDups = new HashSet<Statement>();
				while (queryRes.hasNext())
					queryResNoDups.add(queryRes.next());
				Iterator<Statement> res = queryResNoDups.iterator();

				while (res.hasNext()) {

					Statement s = res.next();

					URI predicate = s.getPredicate();
					
					Vector<Value> facet = null;
					if (facets.containsKey(predicate))
						facet = facets.get(predicate);
					else {
						facet = new Vector<Value>();
						facets.put(predicate, facet);
					}
					facet.add(s.getObject());

				}

				
			} finally {
				con.close();
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	
		return facets;
	}
}
