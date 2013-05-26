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

package com.fluidops.iwb.widget;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.ajax.FValue.ValueConfig;
import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.service.CodeExecution.CodeExecutionContext;
import com.fluidops.iwb.service.CodeExecution.WidgetCodeConfig;
import com.fluidops.iwb.util.QueryResultUtil;


/**
 * ActionableResultWidget is a {@link TableResultWidget} with code execution support, 
 * uses {@link CodeExecutionWidget} functionality for executing code (which can 
 * reference bindings from the table row as additional input).
 * 
 * Bindings from the table can be accessed via the "?:varName" notation, where
 * varName must be a valid projection in the query result.
 * 
 * For each row action basically any of the functionalities available in the
 * {@link CodeExecutionWidget} can be used. Please refer to the documentation
 * of that widget for further details.
 * 
 * If the user does not have privileges to use the {@link CodeExecutionWidget},
 * the usual {@link TableResultWidget} is rendered.
 * 
 * Example:
 * 
 * <source>
 	{{#widget: com.fluidops.iwb.widget.ActionableResultWidget
	| query = 'SELECT ?name ?city WHERE { ?? :name ?name . ?? :city ?city }'
	| rowActions = {{ 
	      {{ label='Hello' | clazz='com.fluidops.iwb.widget.ActionableResultWidget' | method='helloWorld' | args= {{ '?:name' | 'someConst' }} | render='btn' | passContext = true}} |
	      {{ label='Do Something' | clazz='com.fluidops.iwb.widget.ActionableResultWidget' | method='helloWorld' | args= {{ '?:city' | 'someConst' }} | render='btn' | passContext = true}}
	  }}
	}}
 * </source> 
 *  
 * @author as
 *
 */
@TypeConfigDoc("ActionableResult presents a tuple query result in a table and allows to invoke user-defined actions on these results.")
public class ActionableResultWidget extends TableResultWidget
{
	public static class Config extends TableResultWidget.Config
	{
		@ParameterConfigDoc(
				desc = "An user-defined action invoked using input from the table result", 
				type=Type.LIST,
				listType=WidgetCodeConfig.class)  
				public List<WidgetCodeConfig> rowActions;
	}	

	@Override
	protected FTable createTable(String id, Repository rep, String query,
			ValueConfig valueCfg, boolean infer) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException
	{
		final Config c = (Config) get();

		if (c.rowActions == null)
			throw new IllegalArgumentException("Row actions must not be null.");
		for (WidgetCodeConfig rowAction : c.rowActions)
			checkRowAction(rowAction);

		FTable table = new FTable(id);
		CodeExecutionContext ceCtx = new CodeExecutionContext(pc, table);
		FTableModel tm = QueryResultUtil
				.sparqlSelectAsTableModelWithSingleRowAction(rep, query, true,
						infer, pc.value, valueCfg, c.rowActions, ceCtx);
		table.setModel(tm);

		return table;
	}


	@Override
	public String getTitle()
	{
		return "Table Result with code execution support";
	}
	
	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}	


	/**
	 * Check if the rowAction are specified correctly
	 * 
	 * @param rowAction
	 */
	private void checkRowAction(WidgetCodeConfig rowAction) throws IllegalArgumentException {

		// initialize default values
		rowAction.render = rowAction.render==null ? "btn" : rowAction.render;
		rowAction.args = rowAction.args==null ? Collections.<Object>emptyList() : rowAction.args;

		if (rowAction.method==null)
			throw new IllegalArgumentException("rowAction.method must not be null");

		if (rowAction.label==null)
			throw new IllegalArgumentException("rowAction.label must not be null");

		if (rowAction.clazz==null )
			throw new IllegalArgumentException("parameters rowAction.clazz and rowAction.method are required.");
	}


	/**
	 * Demo method for testing which alerts the name that was clicked on.
	 * 
	 * @param ceCtx
	 * @param name
	 */
	@CallableFromWidget
	public static void helloWorld(CodeExecutionContext ceCtx, Value name, String constant) {
		ceCtx.parentComponent.doCallback("alert('Clicked on " + StringEscapeUtils.escapeHtml(name.stringValue()) + "');");
	}
}
