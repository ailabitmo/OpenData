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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.api.ValueResolver;
import com.fluidops.iwb.api.ValueResolver.ResolverType;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.StringUtil;


/**
 * QueryResultWidget to render the results of a query as desired (fully customizable) (see test case 'QueryResults' for more examples)
 * 
 * Usage examples
 * 
 * a) Without pattern: default
 * <code>
 * {{
 * #widget: QueryResult
 * | query = 'SELECT ?Name ?FirstName WHERE { pubmed:id/21293379 pubmed:author ?a . ?a pubmed:lastName ?Name . ?a pubmed:foreName ?FirstName . }'
 * }}
 * </code>
 * 
 * b) With explicit template
 * <code>
 * {{
 * #widget: QueryResult
 * | query = 'SELECT ?Name ?FirstName WHERE { pubmed:id/21293379 pubmed:author ?a . ?a pubmed:lastName ?Name . ?a pubmed:foreName ?FirstName . }'
 * | template = '{{{FirstName}}} {{{Name}}}'
 * }}
 * </code>
 * 
 * c) With template + format
 * <code>
 * {{
 * #widget: QueryResult
 * | query = 'SELECT ?Name ?FirstName WHERE { pubmed:id/21293379 pubmed:author ?a . ?a pubmed:lastName ?Name . ?a pubmed:foreName ?FirstName . }'
 * | template = '{{{FirstName}}} {{{Name}}}'
 * | format = 'ul'
 * }}
 * </code>
 *   
 * e) With explicit template (numbers) => media wiki
 * <code>
 * {{
 * #widget: QueryResult
 * | query = 'SELECT ?Name ?FirstName WHERE { pubmed:id/21293379 pubmed:author ?a . ?a pubmed:lastName ?Name . ?a pubmed:foreName ?FirstName . }'
 * | template = '{{{2}}} {{{1}}}'
 * }}
 * </code>
 * 
 * f) With value resolver
 * <code>
 * {{#widget: QueryResult 
 * | query = 'SELECT ?image ?time WHERE { ?? :image ?image . ?? :time ?time }'
 * | template = '<span style="width:30px">{{{image}}}</span><br/>{{{time}}}'
 * | valueResolver = 'time=MS_TIMESTAMP2DATE,image=IMAGE'
 * }}
 * </code>
 * 
 * @author as
 *
 */
@TypeConfigDoc("The Query Result widget dynamically displays the result of a query and applies a user-defined template and format (e.g. a list)." +
		" The syntax closely follows that of the media wiki.")
public class QueryResultWidget extends AbstractWidget<QueryResultWidget.Config> {

	protected static final Logger logger = Logger.getLogger(QueryResultWidget.class);
	
	/*
	 * Values are initialized per instance in initializeOutput()
	 */
	protected ResultFormat rf = null;
	protected String pattern = null;		// replacement pattern per result item (constructed once based on c.template)
	protected String separator = "";		// separator if needed
	protected Map<String, ResolverType> resolvers = new HashMap<String, ResolverType>();	// map bindingName to ResolverType, if not specified otherwise, DEFAULT is taken
	
	
		
	/**
     * User parameterization
     */
    public static class Config extends WidgetQueryConfig  {
    	
    	/**
    	 * The template which is used for rendering of projection variable results for a single binding set
    	 * 
    	 * example: 
    	 *  - SELECT ?a ?b WHERE { .. }
    	 *  - Bindings: {{?a=A1,?b=B1}, {?a=A2,?b=B2}}
    	 *  - Template: "{{{a}}}  {{{b}}})"
    	 *  
    	 *  Result: 
    	 *  (A1 - B1) separator (A2 - B2) ...
    	 *  
    	 *  Default: Projection variables are comma separated
    	 */
    	@ParameterConfigDoc(desc = "The template which is used for all results")
    	public String template;
    	
    	@ParameterConfigDoc(
    			desc = "The format selected from  ResultFormat",
    			defaultValue = "COMMA_SEPARATED",
    			type = Type.DROPDOWN)
    	public ResultFormat format;
  	    	    	
    	/**
    	 * Optionally specify a mapping for each binding name (without ?), otherwise default is taken (mappings comma separated)
    	 */
    	@ParameterConfigDoc(
    			desc = "Map Value Resolver, e.g. 'myProjectionVar=HTML,b=IMAGE'. If not specified otherwise the DEFAULT value resolver is used.")
    	public String valueResolver; 
    	
    	@ParameterConfigDoc(
    			desc = "Hides the widget if empty",
    			defaultValue = "false")
    	public Boolean hideIfEmpty;
    	
    } 
    
    
    public static enum ResultFormat {
    	
    	/**
    	 * No seperator between elements
    	 */
    	NONE,
    	
    	/**
    	 * Simple, comma separated representation
    	 */
    	COMMA_SEPARATED,
    	
    	/**
    	 * One result per line
    	 */
    	LINE,
    	
    	/**
    	 * unordered list
    	 */
    	UL,
    	
    	/**
    	 * ordered list
    	 */
    	OL;
    }


	@Override
	public FComponent getComponent(String id) {
        
		Config c = get();
		
		if (c.query==null)
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.NO_QUERY);
		
		c.infer = c.infer!=null && c.infer;	
		
		String query = c.query.trim();
        
        // only select queries are supported, however PREFIX may be in the beginning
		try {
			SparqlQueryType type = ReadDataManagerImpl.getSparqlQueryType(query, true);
			if (!type.equals(SparqlQueryType.SELECT))
				return WidgetEmbeddingError.getErrorLabel(id,ErrorType.NO_SELECT_QUERY);
		} catch (MalformedQueryException e) {
			return WidgetEmbeddingError.getErrorLabel(id,ErrorType.SYNTAX_ERROR, e.getMessage());
		}
        	
		
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
		
		TupleQueryResult resIter=null;
		try {
			resIter = dm.sparqlSelect(query, true, pc.value, c.infer);
			
			try {
				initializeOutput(resIter.getBindingNames(), c);
			} catch (Exception e) {
				resIter.close();
				return WidgetEmbeddingError.getErrorLabel(id,ErrorType.INVALID_WIDGET_CONFIGURATION, e.getMessage());
			}
			
			StringBuilder sb = new StringBuilder();
			
			while (resIter.hasNext()) {
				
				BindingSet bs = resIter.next();
												
				String results = pattern;
				int index=1;
				for (String name : resIter.getBindingNames()) {	
					ResolverType rt = resolvers.containsKey(name) ? resolvers.get(name) : ResolverType.DEFAULT;
					results = results.replace("{{{"+index +"}}}", ValueResolver.resolve(bs.getValue(name), rt));
					index++;
				}
				
				sb.append(results);
				if (resIter.hasNext())
					sb.append(separator);		// add a separator (if needed), e.g. ","
			}
			
			if (sb.length()==0 && (c.hideIfEmpty==null || !c.hideIfEmpty))
				/*
				 * No results found and the widget should not be hidden if empty;
				 * print out the noDataMessage
				 */
				return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, c.noDataMessage);
			
			String res = sb.toString();
			
			// in the case of  e.g ul/ol, we need to add surrounding html tags
			switch (rf) {
			case OL:		res = "<ol>" + res + "</ol>";	break;
			case UL:		res = "<ul>" + res + "</ul>";	break;
			default: 		break;	// do nothing
			}
			
			FHTML fComponent = new FHTML(id, res);
			
	        if(StringUtil.isNotNullNorEmpty(c.width))
	        	fComponent.addStyle("width", c.width +"px");
	        if(StringUtil.isNotNullNorEmpty(c.height))
	        	fComponent.addStyle("height", c.height+"px");
	        
			return fComponent;
			
		} catch (Exception e) {
			logger.warn("Error while evaluating query: " + e.getMessage());
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "Error while evaluation query: " + e.getMessage() );
		} finally {
			ReadDataManagerImpl.closeQuietly(resIter);
		}
	}
	
	

	@Override
	public String getTitle() {
		return "Query Result Widget";
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
	
	
	protected void initializeOutput(List<String> bindingNames, Config c) {
		
		// initialize default values
		rf = c.format==null ? ResultFormat.COMMA_SEPARATED : c.format;
		
		if (c.valueResolver!=null) {
			for (String mapping : c.valueResolver.split(",")) {
				if (!mapping.contains("="))
					throw new IllegalArgumentException("ValueResolver parameter specified incorrectly, comma separated mappings are expected.");
				String[] m = mapping.trim().split("=");		// e.g. myProjectionVar=HTML
				resolvers.put(m[0], ResolverType.valueOf(m[1]));
			}
		}
		
		String template = c.template;
		//if no template specified: set default
		if (template==null) {
			StringBuilder sb = new StringBuilder();
			// projection variables are displayed ; separated
			for (int index=1; index<=bindingNames.size(); index++)
				sb.append("{{{").append(index).append("}}}; ");
			template= sb.substring(0, sb.length()-2);	// remove last ;
		} else {
			
			// replace variable names with the index of the projection
			// e.g. q=SELECT ?a ?b; tpl={{{b}}} {{{a}}} => tpl_1={{{2}}} {{{1}}}
			int index = 1;
			for (String name : bindingNames) {
				template = template.replace("{{{"+name+"}}}", "{{{"+index+"}}}");
				index++;
			}
		}
					
		// adjust the pattern according to the selected format
		switch (rf) {
		case NONE:					pattern=template;
									separator="";
									break;
		case COMMA_SEPARATED: 		pattern=template; 
									separator=", ";
									break;
		case LINE:					pattern=template;
									separator="<br/>";
									break;
		case OL:					;
		case UL:					pattern="<li>" + template + "</li>";
									break;
		default:					throw new UnsupportedOperationException("ResultFormat " + rf + " not supported.");
		}
	}

}
