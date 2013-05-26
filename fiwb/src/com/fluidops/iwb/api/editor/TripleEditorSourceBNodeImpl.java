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
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.query.QueryEvaluationException;

import com.google.common.collect.Sets;

/**
 * Triple editor source for blank nodes retrieving the outgoing and optionally
 * the incoming statements (all at once). Does not perform any clustering.
 * 
 * @author as
 */
public class TripleEditorSourceBNodeImpl extends TripleEditorSourceLoadAtOnceBase<BNode> implements TripleEditorSourceBNode {

	private boolean includeInverse;
	
	public TripleEditorSourceBNodeImpl()
	{
		super();
	}

	@Override
	protected List<TripleEditorStatement> retrieveStatements(BNode subject) throws QueryEvaluationException {
		
		List<TripleEditorStatement> res = new ArrayList<TripleEditorStatement>();
		
		for (Statement st : dm.getStatementsAsList(subject, null, null, false)) 
		{
			res.add(new TripleEditorStatement(st, new TripleEditorPropertyInfo(st.getPredicate(),
					Sets.newHashSet(TripleEditorConstants.getDefaultClusteredResourceOutgoing()), true)));
		}
		
		if (includeInverse) {
			for (Statement st : dm.getStatementsAsList(null, null, subject, false)) 
			{
				res.add(new TripleEditorStatement(st, new TripleEditorPropertyInfo(st.getPredicate(), 
						Sets.newHashSet(TripleEditorConstants.getDefaultClusteredResourceIncoming()), false)));
			}
        }
		
		return res;
	}
	
	@Override
	public void initialize(BNode bnode, boolean includeInverseProperties) throws QueryEvaluationException
	{
		this.value = bnode;
		this.includeInverse = includeInverseProperties;
		
		initialize();
	}
}
