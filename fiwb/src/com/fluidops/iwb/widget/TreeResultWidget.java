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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTree;
import com.fluidops.ajax.models.ExtendedTreeNode;
import com.fluidops.ajax.models.FTreeModel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.provider.ProviderUtils;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.StringUtil;

/**
 * This widget creates a tree, which nodes are connected by predicates chosen by
 * the user. The following parameters are used for configuration.
 * 
 * <H1>rootNode</H1> First the user has to specify a
 * <code>rootNode</code> which requires a bootstrap query (<code>SPARQL</code>). 
 * <ul>
 * 	<li>
 * 		<b>query</b>is used to build the first level of the tree and are directly connected to
 * 		the root. The root itself is an <code>URI</code>, whose value may be changed
 * 		in this Java class. So far only the first variable returned by the query
 * 		result is considered when building the tree. Multiple results will be ignored
 * 	</li>
 * </ul>
 * 
 * 
 * <H1>childNodes</H1> the <code>childNodes</code> are children of the rootNode
 * <ul>
 * 	<li>
 * 		<b>predicate</b> defines the parent child relationship 
 * 	</li>
 * 	<li>
 * 		<b>direction</b> gives information about, 
 * 		whether or not the predicate is an incoming, or outgoing connection
 * 	</li>
 * 	<li>
 * 		<b>type</b> represents the allowed object type (rdf:type)
 * 	</li>
 * </ul>
 * 
 * 
 * <H1>Nodes in General</H1>
 * All nodes have the following two properties
 * <ul>
 * 	<li>
 * 		<b>icon</b> path to an image web resource 
 * 		will be rendered within an img tag and therefore can be relative or absolute
 * 	</li>
 * 	<li>
 * 		<b>iconQueryPattern</b> query to the icon
 * 		will render the first result of this query as icon
 * 		variable <code>?:node</code> can then be used, to refer to the current node
 * 	</li>
 * </ul>
 * 
 * <H1>treeName</H1> can be used in order to set a title above the tree
 * <H1>noDataMessage</H1> can be used in order to display this message if there is no result
 * 
 * 
 * <p>
 * The tuple will look like this depending on whether the direction of the
 * predicate is set to incoming or outgoing.
 * 
 * <ul>
 * 	<li>parent :predicate child
 * 	<li>child :predicate parent
 * </ul>
 * 
 * 
 * <H1>Example</H1>
 * The following example will create a class hierarchy of all available classes.
 * 
 * <blockquote>
 * 
 * <pre>
 * {{#widget: com.fluidops.iwb.widget.TreeResultWidget | 
 * rootNode = 
 *   {{ 
 *		query = 'SELECT ?resource WHERE { ?resource rdf:type ?o }' |
 *      icon = '/favicon.ico'
 *   }} | 
 * childNodes = 
 *   {{  
 *     {{
 *        predicate='rdfs:subClassOf' | 
 *        direction='IN'| 
 *        iconQueryPattern = 'SELECT ?icon WHERE { ?instance rdf:type ?:node . ?instance dbpedia:thumbnail ?icon }'
 *     }}
 *   }} |
 * treeName = 'Class Tree' |
 * noDataMessage = 'there are no items to display'
 * }}
 * </pre>
 * 
 * </blockquote>
 * 
 * @author marlon.braun
 * @modified tob
 * 
 */
@TypeConfigDoc("Displays query results as a tree, with Parents and children connected by user defined predicates.")
public class TreeResultWidget extends AbstractWidget<TreeResultWidget.Config>
{
	private static final Logger logger = Logger.getLogger(TreeResultWidget.class.getName());


	/**
	 * Config for the TreeResultWidget. 
	 * Required fields: 'rootNode'.
	 * 
	 * @author tob
	 */
	public static class Config extends WidgetBaseConfig
	{
		@ParameterConfigDoc(
				desc = "top level tree element (bootstrap)",
				type=Type.CONFIG,
				required = true)
		public RootNodeConfig rootNode;
        
		@ParameterConfigDoc(
				desc = "child elements (identified by predicate)",
				listType=ChildNodeConfig.class,
				type=Type.LIST)
		public List<ChildNodeConfig> childNodes;
		
		@ParameterConfigDoc(
				desc = "Defines a custom name for the tree")
		public String treeName;
		
		@ParameterConfigDoc(desc = "Displays a custom message if the resulting tree is empty")
		public String noDataMessage;
		
		@ParameterConfigDoc(
				desc = "Defines the page size of non-leaf nodes, i.e. the number of elements that are rendered on a single page. Defaults to unlimited.")
		public Integer treePageSize;

		@ParameterConfigDoc(
				desc = "Defines the sort order for elements of all nodes except top level nodes. For top level nodes, the sorting can be done in the query.", 
				defaultValue="NONE")
		public SortMethod childrenSortOrder = SortMethod.NONE;

	} // Config

	public static class TreeNodeBaseConfig 
	{
		@ParameterConfigDoc(
				desc = "icon string, e.g. /images/name.png")
		public String icon;
		
		@ParameterConfigDoc(
				desc = "query to get the icon ('?:node' can be used for the actual node resource)")
		public String iconQueryPattern;
		
		public static final String CURRENT_NODE_QUERY_REPLACEMENT = "?:node";
	}
	
	public static class RootNodeConfig extends TreeNodeBaseConfig
	{
		@ParameterConfigDoc(
				desc = "Bootstrap sparql query for top level nodes",
				type=Type.TEXTAREA)
		public String query;		
	}
	
	public static class ChildNodeConfig extends TreeNodeBaseConfig
	{
		@ParameterConfigDoc(
				desc = "Predicate URI which specifies the children relationship")
		public URI predicate;
		
		@ParameterConfigDoc(
				desc = "The predicate direction (IN or OUT)",
				type=Type.DROPDOWN)
		public Direction direction;
		
		@ParameterConfigDoc(
				desc = "Allowed type of the resource, e.g. :MyType")
		public URI type;
	}
	
	/**
	 * predicate connection direction
	 */
	public enum Direction
	{
		/**
		 * ingoing connection
		 */
		IN, 
		/**
		 * outgoing connection
		 */
		OUT
	}
	
	/**
	 * sorting method
	 */
	public enum SortMethod
	{
		/**
		 * no sorting
		 */
		NONE,
		/**
		 * ascending a to z
		 */
		ASC, 
		/**
		 * ascending a to z (ignore case)
		 */
		ASC_IGNORE_CASE, 
		/**
		 * descending z to a
		 */
		DESC, 
		/**
		 * descending z to a (ignore case)
		 */
		DESC_IGNORE_CASE
	}
	
	/**
	 * URI of the root node. May be changed to any valid URI
	 */
	Value root = ValueFactoryImpl.getInstance().createURI("http://www.fluidops.com/Root");
	
	@Override
	public FComponent getComponent( String id )
	{
		FContainer fcont = new FContainer(id);
		
		// Create tree model
		ValueTreeNode rootTreeNode = new ValueTreeNode(new ValueTreeNodeElement(root));
		FTreeModel<ValueTreeNodeElement> tm = new FTreeModel<ValueTreeNodeElement>(rootTreeNode);
		
		if(tm.isLeaf(tm.getRoot()))
		{
			//render no data message
			fcont.add(new FLabel("nodata", getNoDataMessage()));
		}
		else
		{
			// Put the FTreeModel into an FTree and return the tree
			FTree treeResult = new FTree(id, tm);
			Integer pageSize = get().treePageSize;
			if(pageSize != null)
				treeResult.setPageSize(pageSize.intValue());

			fcont.add(treeResult);
		}

		return fcont;

	} // getComponent


	@Override
	public Class<?> getConfigClass( )
	{
		return TreeResultWidget.Config.class;

	} // getConfigClass
	
	protected String getNoDataMessage()
	{
		return get().noDataMessage == null ? "" : get().noDataMessage;
	}

	@Override
	public String getTitle( )
	{
		return get().treeName == null ? "" : get().treeName;
	} // getTitle

	/**
	 * represents the data object for one node in the tree
	 */
	public static class ValueTreeNodeElement
	{
		private Value value;
		private TreeNodeBaseConfig treeNode;
		public ValueTreeNodeElement(Value value)
		{
			super();
			this.value = value;
		}
		
		/**
		 * @param treeNode the treeNode to set
		 */
		public void setTreeNode(TreeNodeBaseConfig treeNode)
		{
			this.treeNode = treeNode;
		}
		
		/**
		 * @return the val
		 */
		public Value getValue()
		{
			return value;
		}
		
		/**
		 * @return the icon
		 */
		public String getIcon()
		{
			if(treeNode == null)
				return null;
			return treeNode.icon;
		}
		
		/**
		 * @return the icon query pattern
		 */
		public String getIconQueryPattern()
		{
			if(treeNode == null)
				return null;
			return treeNode.iconQueryPattern;
		}
		
		public String getQueryReplacement()
		{
			if(treeNode == null)
				return null;
			return TreeNodeBaseConfig.CURRENT_NODE_QUERY_REPLACEMENT;
			
		}
	}
	
	/**
	 * represents a single node in the tree
	 */
	public class ValueTreeNode extends ExtendedTreeNode<ValueTreeNodeElement>
	{
		private static final long serialVersionUID = -159812714979530704L;

		Map<TreeNodeBaseConfig, List<Value>> childMap = null;
		
		private Map<TreeNodeBaseConfig, List<Value>> getChildMap()
		{
			if(childMap == null)
				childMap = getNodeConfig2ChildValues();
			return childMap;
		}
		
		public ValueTreeNode(ValueTreeNodeElement localTreeNode)
		{
			super(localTreeNode);
		}
		
		@Override
		public List<ValueTreeNode> getChildren()
		{
			List<ValueTreeNode> res = new ArrayList<ValueTreeNode>();
			// Initialize repository and get subject
       		for (Entry<TreeNodeBaseConfig, List<Value>> intTreeNode : getChildMap().entrySet())
			{
   				for (Value value : intTreeNode.getValue())
				{
   					ValueTreeNodeElement itne = new ValueTreeNodeElement(value);
   					itne.setTreeNode(intTreeNode.getKey());
   					ValueTreeNode it = new ValueTreeNode(itne);
   					res.add(it);
				}
			}
       		//do the sorting of the children (independent from types)
			sort(res, get().childrenSortOrder);
       		
       		return res;
		}

		/**
		 * sorting
		 * @param res - sorts the list based on sm
		 */
		private void sort(List<ValueTreeNode> res, final SortMethod sm)
		{
			//no sorting, if not required
			if(res == null || res.isEmpty() || sm == SortMethod.NONE)
				return;
			
			//sorts
			Collections.sort(res, new Comparator<ValueTreeNode>()
			{
				@Override
				public int compare(ValueTreeNode arg0, ValueTreeNode arg1)
				{
					Value val1 = arg0.getObj().getValue();
					Value val2 = arg1.getObj().getValue();
					ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
					switch (sm)
					{
					case ASC:
						return dm.getLabel(val1).compareTo(dm.getLabel(val2));
					case ASC_IGNORE_CASE:
						return dm.getLabel(val1).compareToIgnoreCase(dm.getLabel(val2));
					case DESC:
						return dm.getLabel(val2).compareTo(dm.getLabel(val1));
					case DESC_IGNORE_CASE:
						return dm.getLabel(val2).compareToIgnoreCase(dm.getLabel(val1));
					default:
						return 0;
					}
				}
			});
			
		}

		@Override
		public int getChildCount()
		{
			int count = 0;
       		for (List<Value> value : getChildMap().values())
				count += value.size();
			return count;
		}
		
		@Override
		public boolean isRoot()
		{
			return getObj().getValue().equals(root);
		}

		/**
		 * 
		 * @return a map of the nodeconfig and the according values (children)
		 */
		protected Map<TreeNodeBaseConfig, List<Value>> getNodeConfig2ChildValues()
		{
			Map<TreeNodeBaseConfig, List<Value>> res = new HashMap<TreeResultWidget.TreeNodeBaseConfig, List<Value>>();
			ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
       		
			
       		//ROOT node
       		if(isRoot())
       		{
       			RootNodeConfig qtn = get().rootNode;
       			
       			res.put(qtn, getResults(dm, qtn.query));
       		}
       		//in the tree
       		else if(this.getObj().getValue() instanceof Resource && get().childNodes != null)
       		{
       			Resource theVal = (Resource) this.getObj().getValue();
   				for (ChildNodeConfig predicateTreeNode : get().childNodes)
				{
   					List<Value> vals = new ArrayList<Value>();
   					switch (predicateTreeNode.direction)
					{
					case OUT:
						
						List<Statement> resultsOut = dm.getStatementsAsList( theVal, predicateTreeNode.predicate, null, false);
						for (Statement state : resultsOut)
							vals.add(state.getObject());
						break;
					case IN:
            			List<Statement> resultsIn = dm.getStatementsAsList( null , predicateTreeNode.predicate, theVal, false );
            			for (Statement state : resultsIn)
            				vals.add(state.getSubject());
						break;

					default:
						break;
					}
        			// Evaluate query
   					List<Value> resultsMatchingType = new ArrayList<Value>();
        			for(Value value : vals)
        			{
        				//if type is not specified or the specified type is matched -> valid result
        				if(predicateTreeNode.type == null || matchesType(dm, predicateTreeNode.type, value))
        					resultsMatchingType.add(value);
            		}
        			res.put(predicateTreeNode, resultsMatchingType);
				}
       		}
			return res;
		}

		/**
		 * @param dm
		 * @param query
		 * @return
		 */
		protected List<Value> getResults(ReadDataManager dm, String query)
		{
			List<Value> vals = new ArrayList<Value>();
			
			// Evaluate query
			TupleQueryResult result = null;
			try
			{
				result = dm.sparqlSelect(query, true, pc.value, false);

				// Add values of query result to root. Do this tuple by
				// tuple
				while ( result.hasNext())
				{
					BindingSet bindingSet = result.next();
					Iterator<Binding> it = bindingSet.iterator();

					// Extract value by value
					// Only first binding of each BindingSet is extracted.
					// Rest is ignored.
					if(it.hasNext())
					{
						Binding binding = it.next();
						Value value = binding.getValue();

						// Turn value to node
						if(value != null)
						{
							//if there are predicates, create predicate nodes, else, create value nodes
							vals.add(value);
						} // if
					} // if
				} // while
			}
			catch (MalformedQueryException e)
			{
				logger.error("Failed", e );
			}
			catch (QueryEvaluationException e)
			{
				logger.error("Failed", e );
			}
			finally
			{
				ReadWriteDataManagerImpl.closeQuietly(result);
			}
			return vals;
		}
		
        // stores the file's values --> to be able to retrieve them later
        // Resources are represented by hyperlinks leading to the associated resource.
        // Literals are returned as String values.
        @Override
        public List<String> setValues(ValueTreeNodeElement obj)
        {
        	// Distinguish between root and nodes
        	String link;
        	if (isRoot())
        	{
        		link = "<b>" + getTitle() + "</b>";
        	}
        	else
        	{
        		link = EndpointImpl.api().getRequestMapper().getAHrefFromValue(obj.getValue(), false, true, null);
        	}
            
        	List<String> res = new ArrayList<String>();
        	
        	//icon
        	String iconImg = getIcon(obj);
        	
        	//add icon in here in front of link
            res.add(iconImg + link);
            
            return res;
        } // setValues

        /**
         * 
         * @param obj
         * @return icon img html tag
         */
		protected String getIcon(ValueTreeNodeElement obj)
		{
			String iconImgStr = "";
			String queryPattern = obj.getIconQueryPattern();
        	if(StringUtil.isNotNullNorEmpty(queryPattern))
        	{
        		if(obj.getValue() instanceof URI)
        		{
        			String sparqlobj = ProviderUtils.uriToQueryString((URI) obj.getValue());
        			queryPattern = queryPattern.replace(obj.getQueryReplacement(), sparqlobj);
        			
        			ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
        			
        			// Evaluate query
        			List<Value> vals = getResults(dm, queryPattern);
        			
        			for (Value value : vals)
        				iconImgStr = value.stringValue();
        		}
        	}
        	else if(StringUtil.isNotNullNorEmpty(obj.getIcon()))
			{
        		iconImgStr = obj.getIcon();
			}
        	
        	if(StringUtil.isNullOrEmpty(iconImgStr))
        		return "";
    		return "<img src=\""+iconImgStr+"\" style=\"max-height: 24px;\" align=\"absmiddle\" title=\""+obj.getValue().stringValue().replace("\"", "")+"\"/>&nbsp;";
		}
        
		/**
         * 
         * @param dm
         * @param type
         * @param value
         * @return whether or not value is of type type, by default return true
         */
		protected boolean matchesType(ReadDataManager dm, URI type, Value value)
		{
			if(type != null && value instanceof Resource)
				return dm.getType((Resource)value).contains(type);
			return true;
		}
		
	}
	
} // TreeResultWidget
