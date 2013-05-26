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

package com.fluidops.iwb.widget.config;

import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.widget.WidgetEmbeddingError;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.StringUtil;

public class WidgetChartConfig extends WidgetQueryConfig
{
	/* is the query issued against the history repository or not? */
    @ParameterConfigDoc(
            desc = "The parameter defines if the query is issued against the history repository or not.", 
            defaultValue="false") 
    public Boolean historic;

    /*
     * The input variable, to be specified without leading ? symbol.
     * If aggregation is used, this variable is the variable on
     * which we cluster the values.
     */
    @ParameterConfigDoc(
            desc = "The input variable to display (without leading '?' symbol). " +
                    "E.g. if countries' population is visualized with the chart " +
                    "the input variable would most probably be 'country' " +
                    "(corresponding to the variable '?country' in the query). ", 
                    required=true) 
    public String input; 

    /*
     * The output variable, to be specified without leading ? symbol.
     * If aggregation is used, this variable gives the value on which
     * we perform aggregation. It is also possible to specify multiple
     * output variables in a comma-separated list (in combination with
     * aggregation, the same aggregation will be performed on all
     * output variables)
     */
    @ParameterConfigDoc(
            desc = "The output variable to display (without leading '?' symbol). " +
                    "E.g. if countries' population is visualized with the pie chart " +
                    "the output variable would be 'population' " +
                    "(corresponding to the variable '?population' in the query)", 
            type = Type.LIST,
            listType = String.class,
            required=true) 
    public List<String> output;

    /*
     * The settings file used for rendering. Currently, settings files
     * are only supported for the AMCHART rendering engine. The file
     * is specified as abbreviated URL, pointing to the location of
     * the settings file in the default namespace. If no settings file
     * is specified, the default settings file (which always exists)
     * will be used.
     */
    @ParameterConfigDoc(
            desc = "Settings file: abbreviated URL, pointing to the location of " +
            "the settings file in the default namespace") 
    public String settings;

    /*
     * The title of the chart(s). If multiple output variables are
     * specified, the title may be specified in form of a comma-separated
     * list specifying the names of the input variables.
     */
    @ParameterConfigDoc(
            desc = "Title for named queries. If multiple output variables are " +
                    "specified, the title may be specified in form of a comma-separated " +
            "list specifying the names of the input variables.")
    public String title;

    /*
     * Custom mappings for user-defined variables, to be passed as 
     * a string of the form "var1=val1,var2=val2,..."
     */
    @ParameterConfigDoc(
            desc = "Custom mappings for user-defined variables, to be passed as " +
            "a string of the form 'var1=val1,var2=val2,...'")
    public String customMappings;
    
    public FComponent checkObligatoryFields(String id, WidgetChartConfig config, Value value, String defaultHeight)
	{
        // Check if input and output are set
        if (StringUtil.isNullOrEmpty(config.input)
                || config.output == null)
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.MISSING_INPUT_VARIABLE);
        
        // make sure the query is set to a valid SELECT query (for now,
        // we do not support other query types in charts)
        if (config.query != null)
        	config.query = config.query.trim();
        
		SparqlQueryType queryType = null;
		
		try
		{
			queryType = ReadDataManagerImpl.getSparqlQueryType(config.query, true);
		}
		catch (MalformedQueryException e)
		{
			return WidgetEmbeddingError.getErrorLabel(id,
					ErrorType.GENERIC, e.getMessage());
		}
		if(queryType != SparqlQueryType.SELECT)
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.NO_SELECT_QUERY);
		
        
        // unify values/set generic defaults
		config.infer = config.infer!=null && config.infer;	
		config.historic = config.historic != null && config.historic;
		config.title = config.title != null ? config.title : "";
		config.height = config.height != null ? config.height : defaultHeight;

		return null;
		
	}
    
}
