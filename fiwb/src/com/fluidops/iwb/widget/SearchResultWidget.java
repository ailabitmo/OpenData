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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FLayoutTable;
import com.fluidops.ajax.components.FLayoutTable.VerticalAlignment;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.helper.Highlight;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.ajax.FValue;
import com.fluidops.iwb.api.ImageResolver;
import com.fluidops.iwb.model.AbstractMutableTupleQueryResult;
import com.fluidops.iwb.page.SearchPageContext;
import com.fluidops.iwb.server.HybridSearchServlet.BooleanQueryResult;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.Pair;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * This widget displays the result set of a search query as a table.
 * Result clustering is triggered in the config.
 * 
 * @see Config.getClusterSearchResult()
 * @author christian.huetter
 */
public class SearchResultWidget extends AbstractWidget<Void> {

	private static final int LIMIT = 1000; // Maximum number of tuples being shown.
	
	private static final Logger logger = Logger.getLogger(SearchResultWidget.class);
	private static final PolicyFactory htmlSanitizer = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
	
	private SearchPageContext spc;
	private ImageResolver ir;

	/**
	 * Data structure for clustering search results.
	 */
	private static class Clustering
	{
		List<Value> subjects;
		Map<Value, List<Pair<Value, Value>>> clusterMap;
		Map<Value, List<Value>> typeMap;
		
		public Clustering() {
			subjects = new ArrayList<Value>();
			clusterMap = new HashMap<Value, List<Pair<Value, Value>>>();
			typeMap = new HashMap<Value, List<Value>>();
		}
	}

	/**
	 * Image is resolved lazily when component is rendered.
	 */
	private class LazyImage extends FComponent
	{
		private Value value;
		private String cache;
		
		public LazyImage(String id, Value value)
		{
			super(id);
			this.value = value;
			this.cache = null;
		}

	    @Override
	    public String render()
	    {
	    	if (cache != null)
	    		return cache;
	    	else
	    		cache = "";
	    	
	        String imgSrc = ir.resolveImage(value);
			if (imgSrc != null && ImageResolver.isImage(imgSrc))
			{
				cache = ImageResolver.thumbnailString(imgSrc, "48", "");
			}
			
			return cache;
	    }
	}
	
	/**
	 * Table with one column and a dynamic number of rows.
	 */
	private class DynamicTable extends FContainer
	{
	    private FComponent sub;
		private FComponent img;

		public DynamicTable(String id, FComponent sub, FComponent img)
	    {
	    	super(id);
	    	this.sub = sub;
	    	this.img = img;
		}

		@Override
	    public String render()
		{
	        String result = "<table cellspacing=\"5\" cellpadding=\"0\" border=\"0\">";
            result += "<tr>";
            result += "<td>" + sub.htmlAnchor().toString() + "</td>";
            result += "</tr>";
            String imgHtml = img.htmlAnchor().toString();
            if (!imgHtml.isEmpty())
            {
	            result += "<tr>";
				result += "<td style=\"max-width:96px; overflow:hidden;\">" + imgHtml + "</td>";
	            result += "</tr>";
            }
	        result += "</table>";
	        return result;
	    }
	}
	
	public SearchResultWidget() {
		super();
		ir = new ImageResolver(null, true);
	}

	@Override
	public FComponent getComponent(String id) {

		// the page context should result from a search
		if (!(pc instanceof SearchPageContext)) {
			logger.error("The PageContext is not a search result: " + pc.toString());
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "This widget can only be used on a search result page");
		}
		spc = (SearchPageContext) pc;

		// should the search results be clustered?
		boolean clustering = Config.getConfig().getClusterSearchResult()
				&& spc.queryType != null
				&& spc.queryType.equals("KEYWORD");

		// construct table model from query result
		FTableModel tm = null;
		int numberOfRows =  -1;
		double[] columnWidth = null;
		if (spc.queryResult != null)
		{
			try {
				// KEYWORD and SELECT
				if (spc.queryResult instanceof TupleQueryResult)
				{
					AbstractMutableTupleQueryResult tqr = (AbstractMutableTupleQueryResult) spc.queryResult;
					if (clustering)
					{
						tm = constructClusteredTableModel(tqr);
						if (tm.getColumnCount() == 3) columnWidth = new double[] {30, 60, 10};
						numberOfRows = 10;
					}
					else {
						tm = constructTableModel(tqr);
						if (tm.getColumnCount() == 4) columnWidth = new double[] {30, 10, 50, 10};
						numberOfRows = 30;
					}
				}
				// CONSTRUCT
				else if (spc.queryResult instanceof GraphQueryResult)
				{
					tm = constructTableModel((GraphQueryResult) spc.queryResult);
					if (tm.getColumnCount() == 3) columnWidth = new double[] {40, 20, 40};
					numberOfRows = 30;
				}
				// ASK
				else if (spc.queryResult instanceof BooleanQueryResult)
				{
					tm = constructTableModel((BooleanQueryResult) spc.queryResult);
					numberOfRows = 1;
				}
				else {
					throw new IllegalArgumentException("Unknown query result type: " + spc.queryResult.getClass().getName());
				}
			} catch (QueryEvaluationException e) {
				logger.error("The query evaluation failed", e);
				return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "The query evaluation failed");
			}

		} else {
			logger.warn("The search result is null");
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "No search result to display");
		}

		// container for the result table
		FContainer cont = new FContainer(id);

		// define result table
		FTable tbl = new FTable(Rand.getIncrementalFluidUUID(), tm, numberOfRows, columnWidth);
		tbl.setEnableFilter(true);
		tbl.setFilterPos(FilterPos.TOP);
		tbl.setShowCSVExport(true);
		tbl.setOverFlowContainer(true);
		cont.add(tbl);

		return cont;
	}

	/**
	 * Construct a table model from the given tuple query result.
	 * 
	 * @param original	the tuple query result
	 * @return the table model
	 * @throws QueryEvaluationException
	 */
	private FTableModel constructTableModel(AbstractMutableTupleQueryResult original) throws QueryEvaluationException {

		FTableModel tm = new FTableModel();

		AbstractMutableTupleQueryResult res = original.getReducedResultSet(LIMIT);
		
		// nothing found?
		if (!res.hasNext()) {
			tm.addColumn("Results");
			tm.addRow(new Object[] {new FLabel(Rand.getIncrementalFluidUUID(), "Nothing found")});
			return tm;
		}

		// add a column for each binding name
		for (String name : res.getBindingNames()) {
			tm.addColumn(name);
		}

		// add a row for each search result
		while (res.hasNext()) {

			List<FComponent> row = new ArrayList<FComponent>();

			BindingSet bindingSet = res.next();

			for (String name : res.getBindingNames()) {
				final Value val = bindingSet.getValue(name);
				
				if (val == null || StringUtil.isNullOrEmpty(val.stringValue())) {
					row.add(new FLabel(Rand.getIncrementalFluidUUID()));
					continue;
				}
				
				// show resources as links
				if (val instanceof Resource) {
					row.add(new FValue(Rand.getIncrementalFluidUUID(), val)
					{
					    @Override
					    public String toString()
					    {
					    	return val.stringValue();
					    }
					});
				}
				// and literals as labels
				else {
					// highlight the query string
					String needle = spc.query != null ? spc.query : "";
					needle = needle.replaceAll("[\"\\?\\*]", "");
					String value = htmlSanitizer.sanitize(val.stringValue());
					row.add(new FHtmlString(Rand.getIncrementalFluidUUID(), value, value, new Highlight(needle)));
				}
			}

			tm.addRow(row.toArray());
		}

		return tm;
	}

	/**
	 * Construct a table model from the given graph query result.
	 * 
	 * @param res	the graph query result
	 * @return the table model
	 * @throws QueryEvaluationException
	 */
	private FTableModel constructTableModel(GraphQueryResult res) throws QueryEvaluationException
	{
		FTableModel tm = new FTableModel();

		// nothing found?
		if (!res.hasNext()) {
			tm.addColumn("Results");
			tm.addRow(new Object[] {new FLabel(Rand.getIncrementalFluidUUID(), "Nothing found")});
			return tm;
		}

		// graph queries return triples { subject predicate object }
		tm.addColumn("Subject");
		tm.addColumn("Predicate");
		tm.addColumn("Object");

		// add a row for each search result
		for (int rowCounter = 0; res.hasNext() && (rowCounter < LIMIT); rowCounter++)
		{
			Statement st = res.next();

			FComponent[] row = new FComponent[3];
			row[0] = new FValue(Rand.getIncrementalFluidUUID(), st.getSubject());
			row[1] = new FValue(Rand.getIncrementalFluidUUID(), st.getPredicate());
			row[2] = new FValue(Rand.getIncrementalFluidUUID(), st.getObject());

			tm.addRow(row);
		}

		return tm;
	}

	/**
	 * Construct a table model from the given boolean query result.
	 * 
	 * @param res	the boolean query result
	 * @return the table model
	 * @throws QueryEvaluationException
	 */
	private FTableModel constructTableModel(BooleanQueryResult res) throws QueryEvaluationException
	{
		FTableModel tm = new FTableModel();
		tm.addColumn("Result");
		
		if (res.hasNext())
		{
			Boolean bool = res.next();
			tm.addRow(new Object[] {new FLabel(Rand.getIncrementalFluidUUID(), bool.toString())});
		}
		else
		{
			tm.addRow(new Object[] {new FLabel(Rand.getIncrementalFluidUUID(), "Nothing found")});
		}

		return tm;
	}

	/**
	 * Construct a table model from the given query result.
	 * Search results are clustered based on the matching values.
	 * 
	 * @param res	the query result
	 * @return the table model
	 * @throws QueryEvaluationException
	 */
	private FTableModel constructClusteredTableModel(AbstractMutableTupleQueryResult res) throws QueryEvaluationException {
		
		FTableModel tm = new FTableModel();

		// nothing found?
		if (!res.hasNext()) {
			tm.addColumn("Results");
			tm.addRow(new Object[] {new FLabel(Rand.getIncrementalFluidUUID(), "Nothing found")});
			return tm;
		}

		// result columns
		tm.addColumn("Subject");
		tm.addColumn("Property: Value");
		tm.addColumn("Type");

		// create clustering from search result
		Clustering clustering = createClustering(res);

		// populate the table model based on the clustering
		populateTableModel(tm, clustering);
		
		return tm;
	}

	/**
	 * Create clustering from the given search result.
	 * 
	 * @param original
	 * @return Clustering
	 * @throws QueryEvaluationException
	 */
	private Clustering createClustering(AbstractMutableTupleQueryResult original) throws QueryEvaluationException
	{
		// initialize data structure
		Clustering clustering = new Clustering();

		AbstractMutableTupleQueryResult res = original.getReducedResultSet(LIMIT);
		
		// iterate over search result to create clusters
		while (res.hasNext()) {

			BindingSet bindingSet = res.next();

			Value sub = bindingSet.getValue("Subject");
			Value type = bindingSet.getValue("Type");
			Value pred = bindingSet.getValue("Property");
			Value obj = bindingSet.getValue("Value");

			if (sub != null)
			{
				// map types to the subject
				List<Value> types = clustering.typeMap.get(sub);
				if (type != null && !StringUtil.isNullOrEmpty(type.stringValue())
						&& (types == null || !types.contains(type)))
				{
					if (types == null) {
						types = new ArrayList<Value>();
					}
					types.add(type);
					clustering.typeMap.put(sub, types);
				}

				// map pairs of predicate and value to the subject
				List<Pair<Value, Value>> cluster = clustering.clusterMap.get(sub);
				Pair<Value, Value> pair = new Pair<Value, Value>(pred, obj);
				if (pred != null && obj != null
						&& (cluster == null || !cluster.contains(pair)))
				{
					if (cluster == null) {
						cluster = new ArrayList<Pair<Value, Value>>();
						clustering.subjects.add(sub);
					}
					cluster.add(pair);
					clustering.clusterMap.put(sub, cluster);
				}
			}
		}
		
		return clustering;
	}

	/**
	 * Populate the table model based on the given clustering.
	 * 
	 * @param tm
	 * @param clustering 
	 */
	private void populateTableModel(FTableModel tm, final Clustering clustering)
	{
		// add a row for each (unique) subject
		for (final Value subject : clustering.subjects)
		{
			// subject as link
			FComponent sub = new FValue(Rand.getIncrementalFluidUUID(), subject);

			// lazy-loaded image
			FComponent img = new LazyImage(Rand.getIncrementalFluidUUID(), subject);

			// table to group sub and img
			FContainer match = new DynamicTable(Rand.getIncrementalFluidUUID(), sub, img)
			{
			    @Override
			    public String toString()
			    {
			    	return subject.stringValue();
			    }
			};
	    	match.add(sub);
	    	match.add(img);

			// table for clusters
			List<List<FComponent>> clusterTable = new ArrayList<List<FComponent>>();
			for (Pair<Value, Value> cluster : clustering.clusterMap.get(subject))
			{
				if (cluster.fst == null || cluster.snd == null) continue;
				
				FComponent pred = null;
				// show resources as links
				if (cluster.fst instanceof Resource) {
					pred = new FValue(Rand.getIncrementalFluidUUID(), cluster.fst);
				}
				// and literals as labels
				else {
					String value = "<em>" + htmlSanitizer.sanitize(cluster.fst.stringValue()) + ":</em>";
					pred = new FHtmlString(Rand.getIncrementalFluidUUID(), value, value);
				}
//				pred.addStyle("float", "right");
				
				String value = htmlSanitizer.sanitize(cluster.snd.stringValue());
				FComponent obj = new FHtmlString(Rand.getIncrementalFluidUUID(), value, value);

				clusterTable.add(Arrays.asList(pred, obj));
			}
			
			FLayoutTable clusters = new FLayoutTable(Rand.getIncrementalFluidUUID(), clusterTable)
			{
			    @Override
			    public String toString()
			    {
			    	List<Pair<Value, Value>> clusters = clustering.clusterMap.get(subject);
			    	return clusters != null ? clusters.toString() : "";
			    }
			};
			clusters.setColWidth(new String[] {"20%", "80%"});
			clusters.cellspacing = 5;
			clusters.setVerticalAlignment(VerticalAlignment.TOP);

			// table for types
			List<List<FComponent>> typeTable = new ArrayList<List<FComponent>>();
			if (clustering.typeMap.get(subject) != null)
			{
		        for (Value type : clustering.typeMap.get(subject))
		        {
					FComponent c = new FValue(Rand.getIncrementalFluidUUID(), type);
					typeTable.add(Arrays.asList(c));
				}
			}
			else {
				FComponent c = new FLabel(Rand.getIncrementalFluidUUID());
				typeTable.add(Arrays.asList(c));
			}
			
			FLayoutTable types = new FLayoutTable(Rand.getIncrementalFluidUUID(), typeTable)
			{
			    @Override
			    public String toString()
			    {
			    	List<Value> types = clustering.typeMap.get(subject);
			    	return types != null ? types.toString() : "";
			    }
			};
			
			Object[] row = {match, clusters, types};
			tm.addRow(row);
		}
	}

	@Override
	public String getTitle() {
		return "Search Result Widget";
	}

	@Override
	public Class<Void> getConfigClass() {
		return Void.class;
	}
}
