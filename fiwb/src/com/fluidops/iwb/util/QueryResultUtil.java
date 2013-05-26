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

package com.fluidops.iwb.util;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.ajax.FValue;
import com.fluidops.iwb.ajax.FValue.ValueConfig;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.service.CodeExecution;
import com.fluidops.iwb.service.CodeExecution.CodeExecutionContext;
import com.fluidops.iwb.service.CodeExecution.WidgetCodeConfig;
import com.fluidops.iwb.widget.CodeExecutionWidget;
import com.fluidops.util.Rand;


/**
 * Utility class to retrieve queries as table model 
 * 
 * @author as
 *
 */
public class QueryResultUtil
{
	
	/**
	 * Returns the given {@link GraphQueryResult} as a list of statements. The
	 * iteration is closed as part of this method.
	 * @param res
	 * @return
	 * @throws QueryEvaluationException 
	 */
	public static List<Statement> graphQueryResultAsList(GraphQueryResult graph) throws QueryEvaluationException {
		return graphQueryResultAsList(graph, Integer.MAX_VALUE);
	}
	
	/**
	 * Returns the given {@link GraphQueryResult} as a list of statements up to
	 * the provided limit. The iteration is closed as part of this method.
	 * 
	 * @param graph
	 * @param limit a positive integer representing the limit
	 * @return
	 * @throws QueryEvaluationException
	 */
	public static List<Statement> graphQueryResultAsList(GraphQueryResult graph, int limit) throws QueryEvaluationException {
		List<Statement> res = new ArrayList<Statement>();
		try {
			int count=0;
			while (count++<limit && graph.hasNext()) {
				res.add(graph.next());
			}
		} finally {
			graph.close();
		}
		return res;
	}
	
	
	/**
     * Retrieve the result of a SPARQL select query as a table model.
     * 
     * @param rep
     * 			the repository to use
     * @param query
     * 			a valid SPARQL SELECT query
     * @param resolveNamespaces
     * 			boolean flag, to specifiy if abbreviated namespaces should be resolved
     * @param resolveValue
     * 					the value to use as replacement for ??
     * @param valueConfig
     * 			a {@link ValueConfig} to define how values shall be treated
	 *
     * @return
     * 			a populated table model
     * 
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public static FTableModel sparqlSelectAsTableModel(Repository rep, String query,
            boolean resolveNamespaces, boolean infer, Value resolveValue, 
            ValueConfig valueCfg)
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException
    {
    	ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
    	
        TupleQueryResult result = null;
        try {
	        result = dm.sparqlSelect(query, resolveNamespaces,
	                resolveValue, infer);
	        FTableModel tm = new FTableModel();
	
	        for (String name : result.getBindingNames())
	            tm.addColumn(name);
	        int rowCounter = 0;
	        
	        // add the row content to the model
	        while (result.hasNext())
	        {
	            List<FComponent> row = 
	            	buildRow(result.next(), result.getBindingNames(), rowCounter, dm, valueCfg);
	            
	            tm.addRow(row.toArray());
	            rowCounter++;
	        }
	        return tm;
        } finally {
        	ReadDataManagerImpl.closeQuietly(result);
        }
        
    }
    
    
    
    /**
     * Return a table model for the query with the specified singleRowAction. A single row action is 
     * added as an additional column to each row. See class documentation for details.
     * 
     * If the user does not have Code Execution privileges, the usual table is rendered
     * 
     * @param rep
     * 				the repository to evaluate the query on
     * @param query
     * 				the SPARQL SELECT query
     * @param resolveNamespaces
     * 				flag to determine if namespaces should be resolved
     * @param resolveValue
     * 				the value to resolve for, i.e. the value that is inserted into the query for ??, can be null
     * @param valueCfg
     * 				maintain value information, such as the ImageResolver
     * @param singleRowAction
     * 				optional list of {@link RowAction}s which are applied (can be null)
     * @param ceCtx
     *  
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public static FTableModel sparqlSelectAsTableModelWithSingleRowAction(Repository rep, String query,
            boolean resolveNamespaces, boolean infer, Value resolveValue, 
            ValueConfig valueCfg,
            List<WidgetCodeConfig> rowActions,
            CodeExecutionContext ceCtx)
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException
    {
    	
    	if (rowActions==null || rowActions.isEmpty())
    		return sparqlSelectAsTableModel(rep, query, resolveNamespaces, infer, resolveValue, valueCfg);
    	
    	// if the user is not allowed to see the CodeExecutionWidget, we render the usual table
    	if (!EndpointImpl.api().getUserManager().hasWidgetAccess(CodeExecutionWidget.class, null))
    		return sparqlSelectAsTableModel(rep, query, resolveNamespaces, infer, resolveValue, valueCfg);
    	
    	ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
    	
        TupleQueryResult result = null;
        try {
	        result = dm.sparqlSelect(query, resolveNamespaces, resolveValue, infer);
	        FTableModel tm = new FTableModel();
	
	        for (String name : result.getBindingNames())
	            tm.addColumn(name);
	        tm.addColumn("");
	        int rowCounter = 0;	    
	        
	        // add the row content to the model
	        while (result.hasNext())
	        {
	        	BindingSet b=result.next();
	            List<FComponent> row = buildRow(b, result.getBindingNames(), rowCounter, dm, valueCfg);
	              	
	            // add row actions (>1 => container, component otherwise)
	            if (rowActions.size()>1) {
	            	FContainer btnCnt = new FContainer("btnCnt"+Rand.getIncrementalFluidUUID());
	            	// we have to render in inverse order to be able to use float:right
	            	// and to have in addition the intended order
	            	for (int i=rowActions.size()-1; i>=0; i--) {
	            		FComponent c = buildActionComponent(rowActions.get(i), b, ceCtx);
	            		c.addStyle("float", "right");
	            		btnCnt.add(c);
	            	}
	            	btnCnt.addStyle("padding-right", "10px");
	            	row.add(btnCnt);
	            } else {
	            	FComponent c = buildActionComponent(rowActions.get(0), b, ceCtx);
	        		c.addStyle("float", "right");
	        		c.addStyle("margin-right", "10px");
	            	row.add(c);
	            }
	            
	            tm.addRow(row.toArray());
	            rowCounter++;
	        } 

	        return tm;
        } finally {
        	ReadWriteDataManagerImpl.closeQuietly(result);
        } 
    }
    
    
    
   
    /**
     * Build a particular row for the retrieved results, convenience method
     * 
     * @param bindingSet
     * @param bindingNames
     * @param rowCounter
     * @param dm
     * @param cfg
     * 			a {@link ValueConfig}, must not be null
     * @return
     */
    protected static List<FComponent> buildRow(BindingSet bindingSet, List<String> bindingNames, int rowCounter, ReadDataManager dm, ValueConfig cfg) 
    {
    	List<FComponent> row = new ArrayList<FComponent>();

        int colCounter = 0;
        for (String name : bindingNames)
        {
        	String cmpId = "html" + rowCounter++ + "_"  + colCounter++;
            Value value = bindingSet.getValue(name);
            if (value != null) 
            {
                row.add(new FValue(cmpId, value, name, dm, cfg));
            }
            else
                row.add(new FHtmlString(cmpId, "&nbsp;", ""));
        }
        
        return row;
    }
    
    
   
    protected static FComponent buildActionComponent(WidgetCodeConfig rowAction, BindingSet b, final CodeExecutionContext ceCtx) {
    	    	    		
		CodeExecutionWidget cw = new CodeExecutionWidget() ;
		CodeExecution.WidgetCodeConfig cfg = WidgetCodeConfig.copy(rowAction);
		cfg.onFinish = rowAction.onFinish==null ? "none" : rowAction.onFinish;
		cfg.args = new ArrayList<Object>();		         
        
        // pass the code execution context with the table as parent component
        if (rowAction.passContext!=null && rowAction.passContext) {
        	cfg.args.add(ceCtx); 
        	cfg.passContext = false;
        }
        
        for (Object arg : rowAction.args) {
        	
        	// special handling for code execution in table:
        	// it is possible to reference bindings via the ?:varName notation
        	if (arg instanceof String) {
        		String param = (String)arg;
        		if (param.startsWith("?:")) {
        			String bindingName = param.substring(2);
        			if (!b.hasBinding(bindingName))
            			throw new IllegalArgumentException("Variable name " + bindingName + " cannot be used, as it is not part of the query result.");
        			cfg.args.add( b.getBinding(bindingName).getValue() );
        			continue;
        		}
        	}        	
        	cfg.args.add(arg);
        }        
				
		cw.setConfig(cfg);
		return cw.getComponentUAE("ce"+Rand.getIncrementalFluidUUID());    	
    	
    }   
    
   
   
}