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

import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;
import org.mcavallo.opencloud.formatters.HTMLFormatter;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.StringUtil;

/**
 * This widget creates a Tag Cloud.
 * 
 * <p>
 * The widget automatically resolves <code>Resources</code> by their respective
 * labels.
 * 
 * <H1>Widget uses aggregated data</H1>
 * The user specifies a
 * <code>SPARQL query</code>,
 * <code>input</code> and <code>output</code> parameters. 
 * The <code>input</code> field specifies the <code>Value</code>, which is 
 * a numeric value aggregated in the query if needed. 
 * (the values represent the weight of corresponding keyword in the tag cloud)
 * The <code>output</code> field specifies the variable containing the keywords to display in the tag cloud.
 *  If <code>input</code> and <code>output</code> are not defined in the configuration 
 *  the first result variable is considered to represent
 * the <code>Value</code>, which is displayed in the tag cloud, the second
 * is considered to represent its weight, e.g. the number of occurrences.
 * 
 * <p>
 * Required fields
 * <ul>
 * <li><code>query</code></li>
 * </ul>
 * 
 * <H2>Example configuration 1</H2>
 * This configuration displays all classes by their number of instances
 * <blockquote>
 * 
 * <pre>
 * #widget: TagCloud |
 *      query = 'SELECT ?class WHERE {
 *                ?instance rdf:type ?class }' |
 *      input = 'class' |
 *      output = 'class' |
 * </pre>
 * 
 * </blockquote>
 * 
 * 
 * 
 * <H2>Example configuration 2</H2>
 * This configuration displays all classes by their number of instances. The
 * class name is referenced by the predicate <code>void:class</code> and the
 * number of occurrences is retrieved by the predicate
 * <code>void:entities</code>.
 * 
 * <blockquote>
 * 
 * <pre>
 * #widget: TagCloud |
 *      query = 'SELECT ?Class ?Entities WHERE {
 *                ?ClassPartition void:class ?Class .
 *                ?ClassPartition void:entities ?Entities }'
 * </pre>
 * 
 * </blockquote>
 * 
 * 
 * <p>
 * 
 * @author marlon.braun
 * 
 */
@TypeConfigDoc("Creates a tag cloud displaying keywords delivered by a select query. The importance of each keyword is " +
		"represented by the font size.")
public class TagCloudWidget extends AbstractWidget<TagCloudWidget.Config>
{

	private static final Logger logger = Logger.getLogger(TagCloudWidget.class.getName());

	/**
	 * Object, which generates the cloud.
	 */
	Cloud cloud;

	/**
	 * <code>Config</code> class
	 * 
	 * @author marlon.braun
	 * 
	 */
	public static class Config extends WidgetQueryConfig
	{	
		
		@ParameterConfigDoc(desc = "Resources, which are aggregated")
		public String input;
		
		@ParameterConfigDoc(desc = "Aggregated by these values")
		public String output;
		
		@ParameterConfigDoc(desc = "Displays custom message, if results were empty")
		public String noDataMessage;
		
		@ParameterConfigDoc(
				desc = "Instead of linking to the aggregated resources, " +
				"you may specify an optional query to execute when the user clicks any of the clouds' labels. " +
				"Query results will then be shown in a seach results-style page. " +
				"Use '?:input' to refer to the value in the input variable or '??' to refer to the page context.",
				type = Type.TEXTAREA)
		public String queryPattern;
		
	} // Config

    /**
     * Constructs a request string for the value specified by key. The link may
     * either point directly to the value's page if queryPattern is null, or
     * otherwise to a search page showing the results of the parameterized query
     * specified by queryPattern (where the parameter ?? will be replaced by
     * the page context resource and the parameter ?:input will be replaced 
     * by the key value.).
     */
    protected String buildLinkToResourceOrQueryResults(Value key, String queryPattern)
    {
        if (queryPattern == null)
        { // no query, simply link to key
            return EndpointImpl.api().getRequestMapper()
                    .getRequestStringFromValue(key);
        }
        else
        { // linking to query results (as search page)

            // replacing page context variable       	
        	queryPattern = ReadDataManagerImpl.replaceSpecialVariablesInQuery(queryPattern, pc.value, false);
        	
        	// replacing input variable 
            if (key instanceof URI)
                queryPattern = queryPattern.replaceAll("\\?\\:input",
                        "<" + Matcher.quoteReplacement(((URI) key).toString())
                                + ">");
            else if (key instanceof Literal)
                queryPattern = queryPattern.replaceAll("\\?\\:input",
                        "\"" + Matcher.quoteReplacement(key.stringValue())
                                + "\"");

            return EndpointImpl.api().getRequestMapper().getSearchUrlForValue(queryPattern);
        }
    }

    
	@Override
	public FComponent getComponent( String id )
	{

		// Make sure query field is set.
		Config config = get();
		config.infer = config.infer!=null && config.infer;	
		String q = config.query;
		if(q == null)
		{
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.NO_QUERY);
		} // if

		// Repository to work with. This could be extended for use with history.
		Repository rep = pc.repository;

		// Initialize the cloud, set additional different parameters here
		// FUTURE IMPROVEMENTS: Let user decide about these settings
		cloud = new Cloud(); // create cloud
		cloud.setMaxWeight(38.0); // max font size
		cloud.setMinWeight(7.0); // min font size
		cloud.setMaxTagsToDisplay(50); //max number of tags to be displayed
		cloud.setTagCase(Cloud.Case.CASE_SENSITIVE); // Make tags case sensitive

		// Evaluate query
		q = q.trim();
		// only select queries are supported
		try 
		{
			SparqlQueryType type = ReadDataManagerImpl.getSparqlQueryType(q, true);
			if (!type.equals(SparqlQueryType.SELECT))
				return WidgetEmbeddingError.getErrorLabel(id,ErrorType.NO_SELECT_QUERY);
		} 
		catch (MalformedQueryException e) 
		{
			// is handled below
		}
		
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
		TupleQueryResult result = null;
		
		try
		{
			// Evaluate query
			result = dm.sparqlSelect(q, true, pc.value,config.infer);

			// Query result is empty.
			if(result == null || !result.hasNext())
			{
				return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, config.noDataMessage);
			}				
			
			String rq = null;
			if(config.queryPattern != null)
			    rq = config.queryPattern;

		
			// In this case the query does not return exactly two
			// bindings
			if(result.getBindingNames().size() != 2)
			{
				return WidgetEmbeddingError.getErrorLabel(id, ErrorType.SYNTAX_ERROR,
						"You must specify exactly two result variables.");
			} // if

			// namesList.get(0) = URI
			// namesList.get(1) = Weight
			List<String> namesList = result.getBindingNames();

			// Iterate through the TupleQueryResult
			// The TupleQueryResult contains BindingSet. If set
			// correctly each BindingSet consists of two bindings.
			// The first one representing the URI and the second one the
			// weight / number of occurrences.
			while ( result.hasNext())
			{

				BindingSet bindingSet = result.next();

				String tagBinding = StringUtil.isNotNullNorEmpty(config.input)? config.input : namesList.get(0);
				String weightBinding = StringUtil.isNotNullNorEmpty(config.output)? config.output : namesList.get(1);
				
				Binding uri = bindingSet.getBinding(tagBinding);
				Binding weight = bindingSet.getBinding(weightBinding);
				
				if(uri == null || weight == null)
					return config.noDataMessage == null ? WidgetEmbeddingError.getErrorLabel(id,
							ErrorType.EXCEPTION, "Query did not return any data.") : new FLabel(id,
									StringEscapeUtils.escapeHtml(config.noDataMessage));

				// Catch here a NumberFormatException in case the weight
				// variable does not represent a valid number
				try
				{
					double weight_double = Double.valueOf(weight.getValue().stringValue());
                    // Tag: Label, URI, Value, Weight
                    cloud.addTag(new Tag(dm.getLabelHTMLEncoded(uri.getValue()),
                            buildLinkToResourceOrQueryResults(
                                    uri.getValue(), rq), weight_double));

				} // try
				catch ( NumberFormatException e )
				{
					logger.error(e.getMessage(), e);
					return WidgetEmbeddingError.getErrorLabel(id, ErrorType.EXCEPTION,
							"The weight variable does not represent a valid number.");
				} // catch

			} // while

		} // try
		catch ( MalformedQueryException e )
		{
			logger.warn(e.getMessage());
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.SYNTAX_ERROR,
					e.getMessage());
		} // catch
		catch ( QueryInterruptedException e )
		{
			logger.warn(e.getMessage());
			return WidgetEmbeddingError
					.getErrorLabel(id, ErrorType.QUERY_TIMEOUT, config.query);
		}
		catch ( QueryEvaluationException e )
		{
			logger.warn(e.getMessage());
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.QUERY_EVALUATION,
					e.getMessage());
		} // catch
		finally
		{
			ReadDataManagerImpl.closeQuietly(result);
		}

		// Render here the TagCloud
		FComponent fComponent = new FComponent(id) {

			@Override
			public String render( )
			{
				// HTMLFormatter taken from the Cloud jar automatically renders
				// the TagCloud into HTML
				HTMLFormatter htmlf = new HTMLFormatter();
				String htmltext = htmlf.html(cloud);
				return htmltext;
			}

		}; // FComponent
		fComponent.appendClazz("TagCloud");
		
        if(StringUtil.isNotNullNorEmpty(config.width))
        	fComponent.addStyle("width", config.width +"px");
        if(StringUtil.isNotNullNorEmpty(config.height))
        	fComponent.addStyle("height", config.height+"px");
        
		return fComponent;

	} // getComponent

	/**
	 * Returns the title of the Widget.
	 */
	@Override
	public String getTitle( )
	{
		return "Tag Cloud";

	} // getTitle

	/**
	 * Returns the Config class of the widget.
	 */
	@Override
	public Class<?> getConfigClass( )
	{
		return TagCloudWidget.Config.class;

	} // getConfigClass

} // TagCloudWidget
