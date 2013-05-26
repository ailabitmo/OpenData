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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.QueryEvaluationException;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;


/**
 * Base class implementation for all {@link TripleEditorSource}s that retrieve all
 * data at once for the given value. The result of {@link #getStatementPreview()}
 * is always guaranteed to be complete.
 * 
 * @author as
 *
 * @param <T>
 */
public abstract class TripleEditorSourceLoadAtOnceBase<T> implements TripleEditorSource {

	protected static final Logger logger = Logger.getLogger(TripleEditorSourceLoadAtOnceBase.class);
	
	/**
	 * the current resource (e.g. in most cases the pc.value)
	 */
	protected T value;
	
	protected ReadDataManager dm = EndpointImpl.api().getDataManager();
	protected ValueFactory vf = ValueFactoryImpl.getInstance();
	
	private List<TripleEditorStatement> statements;
	private Map<TripleEditorPropertyInfo, List<TripleEditorStatement>> clusteredByProperty = null;
	
	public TripleEditorSourceLoadAtOnceBase() {
		super();
	}
	
	public void initialize() throws QueryEvaluationException {
		this.statements = retrieveStatements(value);
	}

	@Override
	public List<TripleEditorStatement> getStatementPreview() {
		if (statements==null)
			throw new IllegalStateException("Triple editor source not initialized.");
		return statements;
	}

	
	@Override
	public Set<TripleEditorPropertyInfo> getPropertyInfos() {
		if (statements==null)
			throw new IllegalStateException("Triple editor source not initialized.");
		if (clusteredByProperty==null)
			doClustering();	// initialize clustering
		return clusteredByProperty.keySet();
	}	
	

	@Override
	public List<TripleEditorStatement> getStatementsForProperty(
			TripleEditorPropertyInfo tepi, int offset, int limit) {
		if (statements==null)
			throw new IllegalStateException("Triple editor source not initialized.");
		if (clusteredByProperty==null)
			doClustering();	// initialize clustering
		List<TripleEditorStatement> res = clusteredByProperty.get(tepi);
		if (res==null)
			throw new IllegalArgumentException("No statement available for property " + tepi.getUri());
		if (offset>=res.size())
			return Collections.emptyList();
		if (limit==TripleEditorSource.ALL_STATEMENTS)
			limit = res.size();	// retrieve all
		return res.subList(offset, Math.min(offset+limit, res.size()));
	}
	
	private void doClustering() {
		
		clusteredByProperty = new HashMap<TripleEditorPropertyInfo, List<TripleEditorStatement>>();
		
		for (TripleEditorStatement st : statements) {
			List<TripleEditorStatement> l = clusteredByProperty.get(st.getPropertyInfo());
			if (l==null) {
				l = new ArrayList<TripleEditorStatement>();
				clusteredByProperty.put(st.getPropertyInfo(), l);
			}
			l.add(st);
		}
	}
	
	protected abstract List<TripleEditorStatement> retrieveStatements(T value) throws QueryEvaluationException;	
}
