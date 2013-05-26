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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.ajax.FGraph;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.api.RequestMapperImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.StringUtil;

@TypeConfigDoc("The GraphWidget displays RDF data as a graph.")
public class GraphWidget extends AbstractWidget<GraphWidget.Config>
{
	public final String DEFAULTICON = "/images/blacksquare.png";
	public final String CLASSICON = "/images/redcircle.png";
	public final String PROPERTYICON = "/images/bluerectangle.png";
	
    private static final Logger logger = Logger.getLogger(GraphWidget.class.getName());

	private Repository rep;
	private boolean useThumbnails;
	private String graphId;
	public enum NodeType { class_node,  property_node, default_node};
	public enum GraphType {hypertree, rgraph, forcedirectedgraph, classtree, ontologygraph, spacetree};
	
	public static class Config extends WidgetQueryConfig
	
	{
		@ParameterConfigDoc(
				desc = "The graph type", 
				required=true, 
				type = Type.DROPDOWN) 
        public GraphType graphType;
	    
		@ParameterConfigDoc(desc = "The central resource (URI) of a neighborhood graph", defaultValue="Current resource")
        public URI center;
        
		@ParameterConfigDoc(desc = "The maximum depth of a neighborhood graph")
        public Integer maxDepth;
		
		@ParameterConfigDoc(desc = "The maximum number of outgoing edges per node in neighborhood-graphs")
        public Integer spreadFactor;
        
		@ParameterConfigDoc(
				desc = "Is true if thumbnails should be used as nodes",
				defaultValue = "false")
        public Boolean thumbnails;
       
    }
	
	@Override
	public FComponent getComponent(final String id)
    {
		Config conf = get();
		
		if (conf==null)
			return WidgetEmbeddingError.getErrorLabel(id, 
					ErrorType.INVALID_WIDGET_CONFIGURATION, "Widget configuration missing");
		if(conf.query==null)
			 return WidgetEmbeddingError.getErrorLabel(id,
		             ErrorType.MISSING_INPUT_VARIABLE, "The query is not defined");
		if (conf.graphType == null)
			 return WidgetEmbeddingError.getErrorLabel(id,
		             ErrorType.MISSING_INPUT_VARIABLE, "Graph type is not defined");
		
		
		// copy the configuration to not modify the widgets configuration with defaults
		final Config c = new Config();		
		c.query = conf.query;
		c.graphType = conf.graphType;
		c.center = conf.center==null ? getDefaultCenter() : conf.center;
		useThumbnails = conf.thumbnails==null ? true : conf.thumbnails;
		c.maxDepth = conf.maxDepth==null ? Integer.valueOf(4) : conf.maxDepth;
		c.spreadFactor = conf.spreadFactor==null ? Integer.valueOf(10) : conf.spreadFactor;
		c.infer = conf.infer!=null && conf.infer;	
		graphId=id;
		
		try
		{
			createRepository(c);
		}
		catch (MalformedQueryException e)
		{
			 return WidgetEmbeddingError.getErrorLabel(id,
		             ErrorType.SYNTAX_ERROR, e.getMessage());
		}
		catch (QueryEvaluationException e)
		{
			 return WidgetEmbeddingError.getErrorLabel(id,
		             ErrorType.QUERY_EVALUATION, e.getMessage());
		}	
		
		JSONObject input = null;
		String method = null;
		
		switch(c.graphType)
		{
			case forcedirectedgraph: 			
				method = "initIconsForceDirectedGraph";
				input = getRDFAsJsonArray(c.center, rep, c.maxDepth, c.spreadFactor);
				break;
			case hypertree:
				method = "initHypertree";
				input = getRDFAsJsonTree(c.center, rep, c.maxDepth, c.spreadFactor);
				break;
			case rgraph:
				method = "initRGraph";
				input = getRDFAsJsonTree(c.center, rep, c.maxDepth, c.spreadFactor);
				break;
			case classtree:
				method = "initClassTree";
				input = getRDFAsJsonTree(c.center, rep, c.maxDepth, c.spreadFactor);
				break;
			case spacetree:
				method = "initSpacetree";
				input = getRDFAsJsonTree(c.center, rep, c.maxDepth, c.spreadFactor);
				break;
			case ontologygraph:
				method = "initForceDirectedGraph";
				input = getRDFAsJsonArray(c.center, rep, c.maxDepth, c.spreadFactor);
				break;
			default:
				throw new IllegalArgumentException("Graph type not supported: " + c.graphType);
		}

		if (input == null) {
			/*
			 * The query resulted in no data. Print the noDataMessage instead.
			 */
			return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, c.noDataMessage);
		}

		FGraph graphComponent = new FGraph(id);		
		
		graphComponent.setInitMethodToUse(method);
		graphComponent.setInput(input);
		
		if(StringUtil.isNotNullNorEmpty(conf.width))
			graphComponent.setWidth(conf.width);
		if(StringUtil.isNotNullNorEmpty(conf.height))
			graphComponent.setHeight(conf.height);
		
		
		return graphComponent;
    }
    
    private URI getDefaultCenter() {
    	if (pc.value instanceof URI)
    		return (URI)pc.value;
    	// create some URI from the BNode or literal that can serve as center
    	return EndpointImpl.api().getNamespaceService().guessURI(pc.value.stringValue());
    }

	private JSONObject getRDFAsJsonArray(Value value, Repository rep, int depth, int spread)
	{
        JSONArray jarray = new JSONArray();
        
        appendArrayObject(rep, jarray, value, depth, spread);	

        JSONObject json = new JSONObject();
        try {
			json.put("array", jarray);
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
   
        return json;
	}

	private void appendArrayObject(Repository rep, JSONArray jarray, Value value, int depth, int spread) 
	{       
       
        int edgeCount = 0;
        
        List<Statement> res =null;
        ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(rep);
        
        try
        {
	    		res = dm.getStatementsAsList( (Resource)value, null, null, false);
	    		
	        	JSONObject obj = createNode(value, rep);
	          
	            JSONArray adjs = new JSONArray();
	            
	   		 if(depth-->=0)
	   		 
	   		 {
	   		     for (int i=0; i<res.size() && edgeCount<spread; i++)
	   		     {
	        		Statement st = res.get(i);
	        		
					if(st.getPredicate().stringValue().contains(Vocabulary.DBPEDIA_ONT.THUMBNAIL.stringValue())||st.getPredicate().stringValue().contains(RDFS.LABEL.stringValue()))continue;
					if(st.getObject().stringValue().contains(Vocabulary.DBPEDIA_ONT.THUMBNAIL.stringValue())||st.getObject().stringValue().contains(RDFS.LABEL.stringValue()))continue;
		           
					// note: the graph library takes care of HTML encoding, so we pass an unencoded version of the predicate label
					adjs.put(makeEdge(graphId+value.stringValue(), graphId+st.getObject().stringValue(), dm.getLabel(st.getPredicate())));
		            Value next = st.getObject();
		            rep.getConnection().remove(st);
		            
		            if(next instanceof Resource )
		            {
		            	appendArrayObject(rep, jarray, next, depth, spread);
		            }
		            else
		            {
			        	JSONObject obj2 = createNode(st.getObject(), rep);
			            JSONArray adjs2 = new JSONArray();
			            obj2.put("adjacencies", adjs2);
			            jarray.put(obj2);
		            }
	
		            edgeCount++;
	        	}
	   		 }
	            obj.put("adjacencies", adjs);
	            jarray.put(obj);
       
        } 
        catch (JSONException e) 
        {
			logger.error(e.getMessage(), e);
		}
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
				 dm.close();
        }
		
	}

	private JSONObject createNode(Value value, Repository rep) {
		
        ReadWriteDataManager dm = null;
        RequestMapper rm = new RequestMapperImpl();
        
    	JSONObject obj = new JSONObject();
    	try 
    	{
    		dm = ReadWriteDataManagerImpl.openDataManager(rep);
	        obj.put("id", graphId+value.stringValue());  
	        
			String objLabel = dm.getLabel(value);
			if (objLabel.length() > 100)
				objLabel = objLabel.substring(0, 55)+"..";
			String link = "<a class='nodeLabels' href='"+ rm.getRequestStringFromValue(value) +"'>" 
				+ StringEscapeUtils.escapeHtml(objLabel) + "</a>";
	        obj.put("name", link);
	        JSONObject data = addData(selectNodeType(value), getThumbnail(value, rep, dm));
	        obj.put("data", data);   

		} catch (JSONException e) {
			logger.debug("Error while creating graph node: " + e.getMessage(), e);
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
        return obj;
	}

	private JSONObject makeEdge(String from, String to, String label) {
		
	      JSONObject adj = new JSONObject();
	      JSONObject data = new JSONObject();         
	      try
	      {
	    	  adj.put("nodeFrom",  from);
			  adj.put("nodeTo", to);
		      data.put("labeltext", label);
		      adj.put("data", data);
	      } 
	      catch (JSONException e)
	      {
			logger.error(e.getMessage(), e);
	      }
	      return adj;
		}
	
        private NodeType selectNodeType(Value value)
        {
        	ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
        	NodeType design;
 	        Set<Resource> valueTypes = null;
 	        
            if(value instanceof Resource)
            {
            	valueTypes = dm.getType((Resource) value);
            }
            else
            {
            	return NodeType.default_node;
            }

 	        if(valueTypes.size()>0&&valueTypes.contains(OWL.CLASS)||valueTypes.contains(OWL.ONTOLOGY))
        	{
 	        	design = NodeType.class_node;
        	} 
 	        else if(valueTypes.size()>0&&valueTypes.contains(OWL.OBJECTPROPERTY)||valueTypes.contains(OWL.DATATYPEPROPERTY)
 	        		||valueTypes.contains(OWL.ONTOLOGYPROPERTY)||valueTypes.contains(RDF.PROPERTY))
 	        {
 	        	design = NodeType.property_node;
 	        }
 	        else
 	        {
 	        	design = NodeType.default_node;
 	        }

 	        return design;
        }
        
		private JSONObject addData(NodeType design, String image) throws JSONException
		
		{
	        JSONObject data = new JSONObject();

	        if(image.length()>0)
	        {
	        	 customizeNode(data,"#53A850","circle",10, image);	                                
	             return data;
	        }	
	        
	        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
	        
	        switch(design)
	        {
	        
	        case class_node:   customizeNode(data,"#53A850","circle",10, cp+CLASSICON);	                                
	             return data;
	             
	        case property_node:
				customizeNode(data,"#81BEF7", "square", 10,  cp+PROPERTYICON);
	            return data;
             	
	        default:			 
                customizeNode(data, "black", "triangle", 10,  cp+DEFAULTICON);
				return data;
	        }
		}

	private void customizeNode(JSONObject data, String color, String form, int size, String image) 
	    {
		        try {
					data.put("$color", color);
			        data.put("$type", form);
			        data.put("$dim", size);
			        data.put("img", image);
		        } 
		        catch (JSONException e)
		        {
					logger.error(e.getMessage(), e);
				}
		}

	@Override
	public String getTitle()
	{
		return "Graph";
	}
	
	@Override
	public Class<?> getConfigClass()
	{
		return GraphWidget.Config.class;
	}
	/**
	 * creates and populates the repository to be visualized as graph. If the configuration provides a
	 * sparql-query, this query is used to compute the repositpory. If no query but a maxDepth is provided, 
	 * the complete maxDepth-neighborhood is calculated. If none of the two, the complete repository is returned.
	 * 
	 * If anything goes wrong during repository creation, we return an empty, initialized repository
	 * 
	 * @param c
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 */
	private void createRepository(Config c) throws MalformedQueryException, QueryEvaluationException{
		
		try 
		{
			ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
	
			if (c.query != null) 
			{
				rep = new SailRepository(new MemoryStore());
				
				rep.initialize();
			
				GraphQueryResult result = dm.sparqlConstruct(c.query, true, pc.value, c.infer);
				RepositoryConnection con = rep.getConnection();
				con.add(result);
				con.close();
			}
			// if query is not defined, take the global repository and work on this

			else 
			{
				rep = new SailRepository(new MemoryStore());
				
				rep.initialize();
			}

		}
		// if anything goes wrong, return an initialized, empty repository
		catch (RepositoryException  t)
		{
			logger.warn(t.getMessage(), t);
			rep = new SailRepository(new MemoryStore());
			try 
			{
				rep.initialize();
			}
			catch (RepositoryException e) 
			{
				logger.warn(t.getMessage(), t);
			}
		}
	}
		
	
	/**
	 * recursively computes the maxDepth-hop-neighbourhood of the current entity on the passed repository. For evaluation
	 * on the main repository, pass null as rep.
	 * 
	 * @param value  the central value of the graph to be computed
	 * @param rep  repository on which the graph shall be computed
	 * @param depth  number of hops to be computed
	 * @return A JSon representation of the content of the repository, or {@code null} if the repository contains no data
	 * @throws JSONException
	 * @throws RepositoryException
	 */
	public JSONObject getRDFAsJsonTree(URI value, Repository rep, int depth, int spread)
	{
		return getRDFAsJsonTree(value, 0, rep, depth, spread);
	}
	
	private JSONObject getRDFAsJsonTree(URI value, int recCount, Repository rep, int maxDepth, int spread)  
	{
		if (recCount++ > maxDepth) 
		{
			return new JSONObject();
		}

		ReadWriteDataManager dm = null; 

		RequestMapper rm = new RequestMapperImpl();

		JSONObject json = new JSONObject();

		try {
			dm = ReadWriteDataManagerImpl.openDataManager(rep);
			json.put("id", graphId+value.stringValue());
			String label = dm.getLabelHTMLEncoded(value);
			String link = "<a class='nodeLabels' href='"+ rm.getRequestStringFromValue(value) +"'>" + label + "</a>";
			json.put("name", link);
			json.put("data", new JSONObject());

			int edgeCount = 0;

			List<Statement> res = dm.getStatementsAsList((URI)value, null, null, false);

			JSONArray children = new JSONArray();
            Iterator<Statement> it = res.iterator();
			while (it.hasNext() && edgeCount <= spread) 
			{

				Statement st = it.next();
				
				if(st.getPredicate().stringValue().contains(Vocabulary.DBPEDIA_ONT.THUMBNAIL.stringValue()))
				{				
					if (useThumbnails)
					{
						JSONObject data = new JSONObject();
						String thumbnail = st.getObject().stringValue();
						data.put("img", thumbnail);
						json.put("data", data);
					}
					continue;
				}
				
                if(st.getPredicate().stringValue().contains(RDFS.LABEL.stringValue())||
                   st.getObject().stringValue().contains(RDFS.LABEL.stringValue())||
                   st.getObject().stringValue().contains(Vocabulary.DBPEDIA_ONT.THUMBNAIL.stringValue()))continue;
                
				JSONObject child = new JSONObject();
				child.put("id", graphId+st.getObject().stringValue());
				String objLabel = dm.getLabel(st.getObject());
				if (objLabel.length() > 100)
					objLabel = objLabel.substring(0, 55)+"..";
				String link2 = "<a class='nodeLabels' href='"+rm.getRequestStringFromValue(st.getObject())+"'>" 
					+ StringEscapeUtils.escapeHtml(objLabel)+ "</a>";
				child.put("name", link2);

				JSONArray children2 = new JSONArray();
				
				if (st.getObject() instanceof URI)
				{	

					if (useThumbnails)
					{
						JSONObject data = new JSONObject();
						String thumbnail = getThumbnail(st.getObject(), rep, dm);
						data.put("img", thumbnail);
						child.put("data", data);
					}

					if (recCount <maxDepth) 
					{   
						children2.put(getRDFAsJsonTree((URI)st.getObject(), recCount, rep, maxDepth, spread));
					}
				}

				child.put("children", children2);
				children.put(child);
				edgeCount++;
			}

			res = dm.getStatementsAsList(null, null, (URI)value, false);
            Iterator<Statement> it2 = res.iterator();
			while (it2.hasNext() && edgeCount <= spread) 
			{
				Statement st = it2.next();

				JSONObject child = new JSONObject();
				child.put("id", graphId+st.getSubject().stringValue());
				String objLabel = dm.getLabel(st.getSubject());
				if (objLabel.length() > 100)
					objLabel = objLabel.substring(0, 55)+"..";
				String link2 = "<a class='nodeLabels' href='"+rm.getRequestStringFromValue(st.getSubject())+"'>" + StringEscapeUtils.escapeHtml(objLabel)+ "</a>";				   
				child.put("name", link2);
				JSONArray children2 = new JSONArray();
				
				if (st.getSubject() instanceof URI)
				{				
					if (useThumbnails)
					{
						JSONObject data = new JSONObject();
						String thumbnail = getThumbnail(st.getSubject(), rep, dm);
						data.put("img", thumbnail);
						child.put("data", data);
					}

					if (recCount < maxDepth) 
					{  
						children2.put(getRDFAsJsonTree((URI)st.getSubject(), recCount, rep, maxDepth, spread));
					}
				}
				child.put("children", children2);
				children.put(child);
				edgeCount++;
			}
			json.put("children", children);
			return json;

		} 
		catch (JSONException e) {
			logger.error(e.getMessage(), e);
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
		return new JSONObject();
	}

	private String getThumbnail(Value value, Repository  rep, ReadDataManager dm) {

		String thumbnail = "";
		
		List<Statement> res = null;
        if((value instanceof URI)&&(useThumbnails==true))
	        {
			res = dm.getStatementsAsList((URI)value, Vocabulary.DBPEDIA_ONT.THUMBNAIL, null, false);
	        }
        else
	        {
	        	return thumbnail;
	        }
		Iterator<Statement> it = res.iterator();
		while(it.hasNext())
		{
			Statement st = it.next();
			thumbnail = st.getObject().stringValue(); break;
		}
		return thumbnail;
	}
	
	@Override
	public String[] jsURLs() 
	{
	      String cp = EndpointImpl.api().getRequestMapper().getContextPath();
		  return new String[] {cp+"/ajax/JIT/jit-yc.js", cp+"/ajax/JIT/excanvas.js", cp+"/ajax/JIT/jit_resources.js"};
	}
}
