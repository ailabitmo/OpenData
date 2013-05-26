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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventListener;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FGridLayouter;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.SearchTextInput;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.autocompletion.AutoCompleteFactory;
import com.fluidops.iwb.autocompletion.AutoCompletionUtil;
import com.fluidops.iwb.autocompletion.AutoSuggester;
import com.fluidops.iwb.keywordsearch.KeywordSearchAPI;
import com.fluidops.iwb.keywordsearch.SearchProviderFactory;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.ComponentType;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;



/**
 * The search widget allows to parameterize structured queries. Queries are
 * specified as search patterns, which may contain parameters such as '?:input'.
 * Each parameter has a type that specifies how the user input is processed. If
 * the type is "LUCENE", the individual keywords in the input are concatenated
 * with "AND". For types "LITERAL" and "LUCENE", the user input is quoted using
 * double quotation marks (""). Finally, each parameter is associated with a
 * component that serves as input for the user.
 *
 * Example usage:
 * 
 * 1. Simple search widget without query pattern to search in wiki pages.
 * <code>
 * {{ #widget: com.fluidops.iwb.widget.SearchWidget | queries = {{ {{ queryTargets = WIKI }} }} }}
 * </code>
 * 
 * 2. Query pattern with a single parameter ?:input, which is bound by the value
 * of a text input component.
 * <code>
 * {{
 * #widget: com.fluidops.iwb.widget.SearchWidget
 * | queries = {{
 *     {{ queryPattern = 'SELECT * WHERE { ?subject rdfs:label ?:input }' 
 *      | parameters = {{ {{ name='input' | parameterType=LITERAL | componentType=TEXTFIELD }} }}
 *     }}
 *   }}
 * }}
 * </code>
 * 
 * 3. Keyword search pattern that performs a full text search.
 * <code>
 * {{
 * #widget: com.fluidops.iwb.widget.SearchWidget
 * | queries = {{
 *     {{ queryPattern = 'PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> SELECT ?subject WHERE { ?subject search:matches ?match . ?match search:query ?:input . } LIMIT 1000' 
 *      | parameters = {{ {{ name='input' | parameterType=LUCENE | componentType=TEXTFIELD }} }}
 *     }}
 *   }}
 * }}
 * </code>
 * 
 * 4. Complex query pattern with two parameters: ?:input is bound by a text 
 * input component and ?:type by a dropdown box.
 * <code>
 * {{
 * #widget: com.fluidops.iwb.widget.SearchWidget
 * | queries = {{
 *     {{ queryPattern = 'SELECT * WHERE { ?subject rdfs:label ?:label . ?subject rdf:type ?:type }' 
 *      | parameters = {{
 *         {{ name='label' | parameterType=LITERAL | componentType=TEXTFIELD }} |
 *         {{ name='type' | parameterType=URI | componentType=DROPDOWN | values={{ foaf:Person | foaf:Organization }} }}
 *       }}
 *     }}
 *   }}
 * }}
 * </code>
 * 
 * 5. Complex example with two query patterns. A tab pane allows the user to
 * switch between patterns. For each pattern, a list of parameters is specified.
 * <code>
 * {{
 * #widget: com.fluidops.iwb.widget.SearchWidget
 * | queries = {{
 *     {{ label = 'Label'
 *      | queryPattern = 'SELECT ?subject WHERE { ?subject rdfs:label ?:label }'
 *      | parameters = {{ {{ name='label' | parameterType=LITERAL | componentType=TEXTFIELD }} }}
 *     }} | 
 *     {{ label = 'Type'
 *      | queryPattern = 'SELECT ?subject WHERE { ?subject rdf:type ?:type }'
 *      | parameters = {{ {{ name='type' | parameterType=URI | componentType=TEXTFIELD }} }}
 *     }}
 *   }}
 * }}
 * </code>
 * 
 * 6. Auto suggestion of URIs for a text field. The autoSuggest query is 
 * evaluated only once when rendering the widget. When the user starts typing, 
 * the query results are filtered.
 * <code>
 * {{
 * #widget: com.fluidops.iwb.widget.SearchWidget
 * | queries = {{
 *     {{ queryPattern = 'SELECT * WHERE { ?:subject ?property ?value }' 
 *      | parameters = {{
 *         {{ name='subject' | parameterType=URI | componentType=TEXTFIELD | autoSuggest='SELECT ?subject WHERE { ?subject rdf:type test:MyType }' }}
 *       }}
 *     }}
 *   }}
 * }}
 * </code>
 * 
 * @author as
 * @author christian.huetter
 */
@TypeConfigDoc("The search widget can be used to perform keyword or structured search on the database.")
public class SearchWidget extends AbstractWidget<SearchWidget.Config> 
{
	protected static final Logger logger = Logger.getLogger(SearchWidget.class);
	
	/**
     * User parameterization
     */
	public static class Config
	{
		@ParameterConfigDoc(desc = "List of query configurations. If no query is given, a basic search field is constructed.", type = Type.LIST, listType = QueryConfig.class)
		public List<QueryConfig> queries;
		
	}
    
	public static class QueryConfig
	{
		@ParameterConfigDoc(desc = "The label is used to name the tabs for multi-query configurations. It is required only if there are multiple queries.", type = Type.SIMPLE)
		public String label;

		@ParameterConfigDoc(desc = "The SPARQL query pattern, which may contain parameters of the form '?:parameter'.", type = Type.TEXTAREA)
		public String queryPattern;

		@ParameterConfigDoc(desc = "The configurations of the parameters used in the query pattern.", type = Type.LIST, listType = ParameterConfig.class)
		public List<ParameterConfig> parameters;
		
		@ParameterConfigDoc(desc = "The comma-separated list of targets against which the query is evaluated (RDF, WIKI, or user-defined class).", type = Type.SIMPLE)
		public String queryTargets;
	}
    
    public static class ParameterConfig
    {
    	@ParameterConfigDoc(desc = "The name of the parameter, e.g. 'parameter' for the pattern '?:parameter'.", type = Type.SIMPLE)
    	public String name;
    	
    	@ParameterConfigDoc(
    			desc = "The type of the parameter, i.e. LITERAL, LUCENE, or URI.", 
    			type = Type.DROPDOWN)
    	public ParameterType parameterType;
    	
    	@ParameterConfigDoc(
    			desc = "The UI component used as input for the parameter, i.e. TEXTFIELD, TEXTAREA, or DROPDOWN.",
    			type = Type.DROPDOWN)
    	public ComponentType componentType;
    	
    	@ParameterConfigDoc(desc = "List of values to be used as dropbox items or for auto suggestion.", type = Type.LIST, listType = String.class)
    	public List<String> values;
    	
    	@ParameterConfigDoc(desc = "A SELECT query with one projection used for auto suggestion of values for text fields.", type = Type.TEXTAREA)
    	public String autoSuggest;
    }

	/**
	 * The type of the parameter. The default is 'LITERAL' for simple data values.
	 */
	public static enum ParameterType
	{
		/**
		 * Literals are automatically quoted using double quotation marks ("").
		 */
		LITERAL,
		/**
		 * Special type of Literals where the individual keywords are concatenated with AND.
		 */
		LUCENE,
		/**
		 * URIs can be either IRIs or prefixed names (rdf:type). IRIs have to be surrounded with angle brackets (<>).
		 */
		URI
	}

	private static class QueryContainer
	{
		String label;
		FContainer container;
		
		QueryContainer(String label, FContainer container)
		{
			this.label = label;
			this.container = container;
		}
	}
	
	private static class InputTuple
	{
		String name;
		ParameterType parameterType;
		FComponent component;
		
		InputTuple(String name, ParameterType parameterType, FComponent component)
		{
			this.name = name;
			this.parameterType = parameterType;
			this.component = component;
		}
	}
	
	protected FContainer container;

	@Override
	public FComponent getComponent(String id)
	{
		Config c = get();
		
		// if queries is null or empty, return a basic search field
		if (c==null || c.queries == null || c.queries.isEmpty()) 
			return createDefaultTextInput(id, SearchProviderFactory.getDefaultQueryTargets());
		
		// data structure for queries
		List<QueryContainer> containers = new ArrayList<QueryContainer>();
		
		// extract config for each query
		for (final QueryConfig query : c.queries)
		{
			// query label
			String label = StringEscapeUtils.escapeHtml(query.label);

			final List<String> queryTargets = query.queryTargets == null ? SearchProviderFactory.getDefaultQueryTargets() : Lists.newArrayList(query.queryTargets.split("\\s*,\\s*"));
			
			// label is required only if there are multiple queries
			if (c.queries.size() > 1 && StringUtil.isNullOrEmpty(label))
				return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Label must not be null or empty.");
			
			// query pattern
			final String queryPattern = query.queryPattern;
			
			// construct a container for the input components
			final List<InputTuple> tuples = new ArrayList<InputTuple>();
			FContainer cont = null;
			
			// default case: both query pattern and input parameters are given
			if (!isNullOrEmpty(query.parameters) && !StringUtil.isNullOrEmpty(queryPattern))
			{
				try
				{
					cont = constructInputContainer(query.parameters, tuples);
				}
				catch (Exception e)
				{
					return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Error while evaluating query: " + e.getMessage());
				}
				
				// search button
				FButton searchButton = new FButton("searchButton", "Search")
				{
					@Override
					public void onClick()
					{
						submitInput(this, queryPattern, tuples, queryTargets);
					}
				};
				cont.add(searchButton);
				searchButton.appendClazz("searchButton");
			}
			// if neither query pattern nor parameters are given, create a basic search field
			else if (isNullOrEmpty(query.parameters) && StringUtil.isNullOrEmpty(queryPattern))
			{
				cont = new FContainer(Rand.getIncrementalFluidUUID());
				cont.add(createDefaultTextInput(Rand.getIncrementalFluidUUID(), queryTargets));
			}
			// otherwise, either query pattern or parameters missing
			else
			{
				return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Either query pattern or input parameters missing.");
			}
			
			containers.add(new QueryContainer(label, cont));
		}
		
		// construct a tab container if more than one query is specified
		if (containers.size() > 1) {
			container = constructTabContainer(id, containers);
		}
		// otherwise, return input container directly
		else {
			container = new FContainer(id);
			container.add(containers.get(0).container);
			
			// if there is only one search field it will get a different look
			if(c.queries.size() == 1 && c.queries.get(0).parameters != null && c.queries.get(0).parameters.size() == 1)
				container.appendClazz("singleFieldSearch");
		}
		return container;
	}
	
	private SearchTextInput createDefaultTextInput(String id, List<String> queryTargets)
	{
		return new SearchTextInput(
					id,
					EndpointImpl.api().getRequestMapper().getContextPath(),
					(String)null,
					com.fluidops.iwb.util.Config.getConfig().getAutocompletion(),
					queryTargets);
	}

	/**
	 * Construct a container for the given input parameters.
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 */
	private FContainer constructInputContainer(List<ParameterConfig> parameters,
			List<InputTuple> tuples)
			throws MalformedQueryException, QueryEvaluationException
	{
		final FGridLayouter grid = new FGridLayouter(Rand.getIncrementalFluidUUID());
		
		for (ParameterConfig parameter : parameters)
		{
			FLabel label = new FLabel(Rand.getIncrementalFluidUUID(), StringEscapeUtils.escapeHtml(parameter.name));
			
			label.appendClazz("searchName");
			
			grid.add(label);
			
			if (parameter.componentType == null)
				throw new RuntimeException("Component type must not be null.");
			
			// handle onEnter event for text fields
			FEventListener listener = null;
			if (parameter.componentType == ComponentType.TEXTFIELD)
			{
				listener = new FEventListener()
				{
					@Override
					public void handleClientSideEvent(FEvent evt)
					{
						for (FComponent c : grid.getComponents())
						{
							if (c.getId().contains("searchButton"))
							{
								((FButton) c).onClick();
							}
						}

					}
				};
			}
			
			final FComponent comp = parameter.componentType.create(Rand.getIncrementalFluidUUID(), listener);
			
			if (comp instanceof FComboBox)
			{
				if (StringUtil.isNotNullNorEmpty(parameter.autoSuggest))
				{					
					((FComboBox) comp).setChoices(computeChoicesOrSuggestions(parameter.autoSuggest));
				}
				else if (parameter.values != null)
				{
					// not necessary to escape dropdown items
					((FComboBox) comp).setChoices(parameter.values);
				}
				else
				{
					throw new RuntimeException("For a DROPDOWN component, either an item query or a list of items must be specified.");
				}
			}
			else if (comp instanceof FTextInput2)
			{
				if (StringUtil.isNotNullNorEmpty(parameter.autoSuggest))
				{					
					FTextInput2 t = (FTextInput2)comp;
					AutoSuggester autoSuggestor = AutoCompleteFactory.createQuerySuggester(parameter.autoSuggest, pc.value);
					t.setChoices(AutoCompletionUtil.toDisplayChoices(autoSuggestor.suggest("")));
				}				
			}
			grid.add(comp);
			
			tuples.add(new InputTuple(parameter.name, parameter.parameterType, comp));
			grid.next();
		}
		
		grid.appendClazz("searchWidget");
		
		return grid;
	}

	/**
	 * Convenience method to submit input.
	 */
	protected void submitInput(FComponent comp, String queryPattern,
			List<InputTuple> tuples, List<String> queryTargets)
	{
		try
		{
			String query = completeQueryPattern(queryPattern, tuples);
			String url = EndpointImpl.api().getRequestMapper().getSearchUrlForValue(query, queryTargets);		
			comp.addClientUpdate(new FClientUpdate("document.location='" + url + "\';"));        			
		}
		catch (ParseException e)
		{
			comp.addClientUpdate(new FClientUpdate("alert('There are syntax errors in the search input.')"));
		}
	}
	
	/**
	 * Complete the query pattern by replacing parameters with the given input.
	 */
	protected String completeQueryPattern(String queryPattern,
			List<InputTuple> tuples)
			throws ParseException
	{
		for (InputTuple tuple : tuples)
		{
			String input = tuple.component.returnValues().toString();
			input = input.trim();
			
			ParameterType searchType = tuple.parameterType;
			
			if (searchType == ParameterType.LUCENE && !input.isEmpty())
				input = KeywordSearchAPI.normalizeLuceneQuery(input);
			
			input = StringUtil.escapeSparqlStrings(input);
			
			if (searchType == ParameterType.LITERAL || searchType == ParameterType.LUCENE)
			{
				input = "\"" + input + "\"";
			}
			else if (searchType == ParameterType.URI)
			{
				input = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(input).stringValue();
				input = "<" + input + ">";
			}			
            // replace user input variable   
			queryPattern = queryPattern.replace("?:" + tuple.name, input);
		}
        // replace page context variable       	
    	queryPattern = ReadDataManagerImpl.replaceSpecialVariablesInQuery(queryPattern, pc.value, false);
    	
		return queryPattern;
	}
		
	/**
	 * Construct query string for the search servlet.
	 */
	protected String buildQueryString(String queryPattern)
			throws ParseException
	{
		return EndpointImpl.api().getRequestMapper().getSearchUrlForValue(queryPattern);
	}

	/**
	 * Construct a tab pane as container for the given queries.
	 */
	private FContainer constructTabContainer(String id, List<QueryContainer> containers)
	{
		FTabPane2Lazy tabContainer = new FTabPane2Lazy(id);
		tabContainer.appendClazz("searchWidget");
		tabContainer.drawAdvHeader(true);
		
		// add a tab for each query
		for (QueryContainer container : containers)
		{
			tabContainer.addTab(container.label, container.container);
		}

		return tabContainer;
	}

	@Override
	public String getTitle() {
		return "Search Widget";
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
	
	/**
	 * Compute a list of choices that can be added both to text input components
	 * or to drop down items.
	 * 
	 * @param query
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 */
	private List<Value> computeChoicesOrSuggestions(String query) throws MalformedQueryException, QueryEvaluationException
	{
		List<Value> choices = new ArrayList<Value>();
		
		// evaluate itemQuery
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
		TupleQueryResult res = dm.sparqlSelect(query, true, pc.value, false);
		
		try
		{
			if (res.getBindingNames().size() != 1)
				throw new MalformedQueryException("The query must contain exactly one binding variable.");
			
			String bindingName = res.getBindingNames().get(0);
			
			while (res.hasNext())
			{
				BindingSet bindingSet = res.next();
				Value value = bindingSet.getValue(bindingName);
				choices.add(value);
			}
		}
		finally
		{
			res.close();
		}
		
		return choices;
	}

	private static boolean isNullOrEmpty(Collection<?> c)
	{
		return c == null || c.isEmpty();
	}
}
