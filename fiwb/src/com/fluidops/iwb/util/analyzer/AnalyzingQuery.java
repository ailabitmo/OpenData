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

package com.fluidops.iwb.util.analyzer;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * Base class for any analyzing query: wraps a query and sends information to
 * the {@link Analyzer} when being evaluated.
 * 
 * @author as
 *
 */
public class AnalyzingQuery implements Query {

	protected final Query query;
	protected final String queryString;
	protected final AnalyzingConnection conn;

	public AnalyzingQuery(AnalyzingConnection conn, Query query, String queryString) {
		super();
		this.conn = conn;
		this.query = query;
		this.queryString = queryString;
	}

	public void clearBindings() {
		query.clearBindings();
	}

	public BindingSet getBindings() {
		return query.getBindings();
	}

	public Dataset getDataset() {
		return query.getDataset();
	}

	public boolean getIncludeInferred() {
		return query.getIncludeInferred();
	}

	public int getMaxQueryTime() {
		return query.getMaxQueryTime();
	}

	public void removeBinding(String name) {
		query.removeBinding(name);
	}

	public void setBinding(String name, Value value) {
		query.setBinding(name, value);
	}

	public void setDataset(Dataset dataset) {
		query.setDataset(dataset);
	}

	public void setIncludeInferred(boolean includeInferred) {
		query.setIncludeInferred(includeInferred);
	}

	public void setMaxQueryTime(int maxQueryTime) {
		query.setMaxQueryTime(maxQueryTime);
	}

	public String getQueryString() {
		return queryString;
	}
	
	
	/**
	 * Analysis wrapper for a TupleQuery.
	 * 
	 * @author as	
	 */
	public static class AnalyzingTupleQuery extends AnalyzingQuery implements TupleQuery {

		
		public AnalyzingTupleQuery(AnalyzingConnection conn, TupleQuery query, String queryString) {
			super(conn, query, queryString);
		}

		public TupleQueryResult evaluate() throws QueryEvaluationException {
			long start = System.currentTimeMillis();
			TupleQueryResult res = ((TupleQuery)query).evaluate();
			long duration = System.currentTimeMillis() - start;
	        Analyzer.getInstance().analyze(conn, this, duration);
	        return res;
		}

		public void evaluate(TupleQueryResultHandler handler)
				throws QueryEvaluationException, TupleQueryResultHandlerException {
			long start = System.currentTimeMillis();
			((TupleQuery)query).evaluate(handler);
			long duration = System.currentTimeMillis() - start;
	        Analyzer.getInstance().analyze(conn, this, duration);
		}
	}
	
	
	/**
	 * Analysis wrapper for a GraphQuery.
	 * 
	 * @author as	
	 */
	public static class AnalyzingGraphQuery extends AnalyzingQuery implements GraphQuery {

		public AnalyzingGraphQuery(AnalyzingConnection conn, GraphQuery query, String queryString) {
			super(conn, query, queryString);
		}

		@Override
		public GraphQueryResult evaluate() throws QueryEvaluationException {
			long start = System.currentTimeMillis();
			GraphQueryResult res = ((GraphQuery)query).evaluate();
			long duration = System.currentTimeMillis() - start;
	        Analyzer.getInstance().analyze(conn, this, duration);
	        return res;
		}

		@Override
		public void evaluate(RDFHandler handler)
				throws QueryEvaluationException, RDFHandlerException {
			long start = System.currentTimeMillis();
			((GraphQuery)query).evaluate(handler);
			long duration = System.currentTimeMillis() - start;
	        Analyzer.getInstance().analyze(conn, this, duration);			
		}	
	}
	
	/**
	 * Analysis wrapper for a GraphQuery.
	 * 
	 * @author as	
	 */
	public static class AnalyzingBooleanQuery extends AnalyzingQuery implements BooleanQuery {

		public AnalyzingBooleanQuery(AnalyzingConnection conn, BooleanQuery query, String queryString) {
			super(conn, query, queryString);
		}

		@Override
		public boolean evaluate() throws QueryEvaluationException {
			long start = System.currentTimeMillis();
			boolean res = ((BooleanQuery)query).evaluate();
			long duration = System.currentTimeMillis() - start;
	        Analyzer.getInstance().analyze(conn, this, duration);
	        return res;
		}		
	}
}



 