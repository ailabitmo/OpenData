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

package com.fluidops.iwb.api.operator;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.api.ReadDataManagerImpl;

/**
 * Structure to maintain a tuple result as a materialized table. The passed
 * query result iterator is closed, once the data is materialized into
 * this class
 * 
 * @author as
 *
 */
public class SPARQLResultTable
{

	private final List<BindingSet> rows;
	private final List<String> bindingNames;
	
	
	public SPARQLResultTable(TupleQueryResult queryResult) throws QueryEvaluationException {
		rows = initialize(queryResult);
		bindingNames = queryResult.getBindingNames();
	}

	private List<BindingSet> initialize(TupleQueryResult queryResult) throws QueryEvaluationException {
		
		List<BindingSet> res = new ArrayList<BindingSet>();
		try {
			while (queryResult.hasNext()) {
				res.add(queryResult.next());
			}
		} finally {
			ReadDataManagerImpl.closeQuietly(queryResult);
		}
		
		return res;
	}
	
	/**
	 * @return the binding names, i.e. the titles of the columns
	 */
	public List<String> getBindingNames() {
		return bindingNames;
	}
	
	/**
	 * @return the number of rows in this table
	 */
	public int size() {
		return rows.size();
	}
	
	/**
	 * @param row the number of the given row, starting with 0
	 * @return the content of the given row
	 */
	public List<Value> row(int row) {
		if (row>=rows.size())
			throw new IllegalArgumentException("Table has only " + size() + " rows.");
		return asList(rows.get(row));		
	}
	
	/**
	 * @param column
	 * @return the contents of the given row
	 */
	public List<Value> column(String column) {
		if (!bindingNames.contains(column))
			throw new IllegalArgumentException("Table does not have column " + column);
		return columnAsList(column);
	}
	
	private List<Value> columnAsList(String column) {
		List<Value> columnList = new ArrayList<Value>();
		for (BindingSet row : rows)
			columnList.add( row.getValue(column) );
		return columnList;
	}

	/**
	 * @return the value of the left most cell, i.e. the first projected element
	 */
	public Value firstBinding() {
		if (size()==0)
			return null;
		return rows.get(0).getValue(bindingNames.get(0));
	}
	
	/**
	 * @return the data of this table in a two dimensional array
	 */
	public List<List<Value>> data() {
		List<List<Value>> data = new ArrayList<List<Value>>();
		for (BindingSet bindingSet : rows)
			data.add(asList(bindingSet));
		return data;
	}
	
	private List<Value> asList(BindingSet bindingSet) {
		List<Value> rowList = new ArrayList<Value>();
		for (String bindingName : bindingNames)
			rowList.add( bindingSet.getValue(bindingName) );
		return rowList;
	}
}
