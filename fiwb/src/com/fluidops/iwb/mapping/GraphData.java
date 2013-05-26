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

package com.fluidops.iwb.mapping;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.fluidops.iwb.mapping.TableData.PropertyType;
import com.fluidops.iwb.provider.ProviderURIResolver;
import com.fluidops.iwb.util.Graph;

/**
 * simple graph data model. does not know about
 * RDF data model yet. I.e. has no distinction between
 * resource and literal.
 * 
 * features are delegated to com.fluidops.iwb.util.Graph
 * 
 * @author aeb
 */
public class GraphData extends Data
{
	private static final long serialVersionUID = -4607135151899294938L;
	
	/**
	 * Resolver used when mapping the table data
	 */
	public ProviderURIResolver uriResolver = null;
	
	public GraphData(Graph graph)
	{
		this.graph = graph;
	}
	
    /**
     * Types of the columns, if not explicitly set type ColumnType.UNKNOWN is assumed
     */
    private Map<String,PropertyType> edgetypes = new HashMap<String,PropertyType>();

    public void setEdgeTypes(Map<String,PropertyType> edgetypes)
    {
    	this.edgetypes=edgetypes;
    }    	
    
	public Graph graph;
	
	public RdfData toRdf()
	{
		return new RdfData( graph.toRDF(uriResolver,edgetypes) );
	}
	
    public RdfData toRdf(String schemafile)
	{
	    return new RdfData( graph.toRDF(schemafile) );
	}

	public void splitObject(String pred, String delimiterRegexp, String... labels)
	{
		graph.splitObject(pred, delimiterRegexp, labels);
	}
	
	public void remove(String s, String p, String o)
	{
		graph.remove(s, p, o);
	}
	
	@Override
	public String getContentType()
	{
		return "text/plain";
	}

	@Override
	public void toHTML(Writer out) throws IOException
	{
		out.write(""+graph);
	}
	
	public String toString()
	{
		return ""+graph;
	}

	public void extractFactFromURI(String regexp, String predicate)
	{
		graph.extractFactFromURI( regexp, predicate );
	}
	
	
	public ProviderURIResolver getResolver() 
	{
		return uriResolver;
	}
	
	public void setResolver(ProviderURIResolver resolver) 
	{
		this.uriResolver = resolver;
	}
}
