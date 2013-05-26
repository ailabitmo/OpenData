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

import java.util.List;
import java.util.Set;

import org.openrdf.query.QueryEvaluationException;

/**
 * Interface that models data source for the Triple editor.
 * 
 * @author uli
 */
public interface TripleEditorSource {

	/**
	 * Integer representing the LIMIT to retrieve all statements for a given
	 * property, see getStatementsForProperty
	 */
	public int ALL_STATEMENTS = -1;
	
	/**
	 * Return a list of statements which are associated to the given resource
	 * or label. Depending on the implementation this can include outgoing 
	 * and/or incoming statements. This method is designed to return a 
	 * preview of statements (i.e., it is not required to be complete 
	 * for the given resource). However, it is guaranteed that each 
	 * property is present in at least one statement, i.e. the list 
	 * of properties is complete.
	 * 
	 * @return
	 */
	public List<TripleEditorStatement> getStatementPreview() throws QueryEvaluationException;
	
	/**
	 * Returns a list of statements for the given property matching
	 * the offset and limit settings. The method returns a list with 
	 * at most <code>limit</code>, however, the list can also be empty
	 * if there are no statements matching the properties.
	 * 
	 * If limit equals {@link #ALL_STATEMENTS}, the entire set of 
	 * statements for the given property is to be returned.
	 * 
	 * @param tepi
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<TripleEditorStatement> getStatementsForProperty(TripleEditorPropertyInfo tepi, int offset, int limit) throws QueryEvaluationException;
	
	/**
	 * Returns the complete set of all available property information
	 * for the given resource.
	 * 
	 * @return
	 */
	public Set<TripleEditorPropertyInfo> getPropertyInfos() throws QueryEvaluationException;
}
