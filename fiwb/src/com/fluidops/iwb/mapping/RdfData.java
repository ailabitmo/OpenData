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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;

import com.fluidops.iwb.util.Graph.Edge;
import com.fluidops.iwb.util.Graph.Node;
import com.fluidops.util.Pair;

/**
 * RDF data model. Final step of the mapping pipeline, can be added to the repository
 * 
 * @author aeb
 */
public class RdfData extends Data
{
	private static final long serialVersionUID = 2098661265963243177L;

	public RdfData(List<Statement> statements)
	{
		this.statements = statements;
	}

	public Collection<Statement> statements;

	/**
	 * replace (s,source,o) with (s,target,o)
	 * @param source
	 * @param target
	 */
	public void renamePred( URI source, URI target )
	{
		List<Statement> tochange = new ArrayList<Statement>();
		for ( Statement s : statements )
		{
			if ( s.getPredicate().equals( source ) )
				tochange.add( s );
		}
		
		for ( Statement s : tochange )
			statements.add( ValueFactoryImpl.getInstance().createStatement(s.getSubject(), target, s.getObject() ) );
		
		statements.removeAll( tochange );
	}
	
	private Node<Value> addNode( Map<Value, Node<Value>> nodes, Value v )
	{
		Node<Value> node = nodes.get( v );
		if ( node == null )
		{
			node = new Node<Value>();
			node.name = v;
			nodes.put(v, node);
		}
		return node;
	}
	
	/**
	 * Modifies input dataset according to the specified matching condition
	 */
	public void align( AlignmentOperator op )
	{
		// modified graph representation
		Map<Value, Node<Value>> nodes = new HashMap<Value, Node<Value>>();
		Set<Edge<Value>> edges = new HashSet<Edge<Value>>();
		
		for ( Statement s : statements )
		{
			Node<Value> subject = addNode( nodes, s.getSubject() );
			Node<Value> predicate = addNode( nodes, s.getPredicate() );
			Node<Value> object = addNode( nodes, s.getObject() );
			edges.add( new Edge<Value>( subject, predicate, object ) );
		}
		
		for ( Pair<Resource, Resource> item : op.align( statements ) )
		{
			Node<Value> match = nodes.get( item.fst );
			if ( match != null )
				match.name = item.snd;
		}
		
		statements.clear();
		for ( Edge<Value> edge : edges )
			statements.add( ValueFactoryImpl.getInstance().createStatement( (Resource)edge.subject.name, (URI)edge.predicate.name, edge.object.name ) );
	}

	/**
	 * Generates owl:sameAs statements according to the specified matching condition
	 */
	public void createSameAs( AlignmentOperator op )
	{
		for ( Pair<Resource, Resource> item : op.align( statements ) )
			statements.add( ValueFactoryImpl.getInstance().createStatement(item.fst, OWL.SAMEAS, item.snd) );
	}
	
	@Override
	public String getContentType()
	{
		return "text/plain";
	}

	@Override
	public void toHTML(Writer out) throws IOException
	{
		for ( Statement statement : statements )
			out.write(""+statement+"\n");
	}
	
	public String toString()
	{
		StringBuilder res = new StringBuilder();
		for ( Statement statement : statements )
			res.append(statement.toString()).append("\n");
		return res.toString();
	}
	
	/**
	 * removes all triples having null as subject, predicate or object
	 */
	public void makeNullFree()
	{
		List<Statement> nullFreeStatements = new ArrayList<Statement>();
		for (Statement stmt : this.statements)
    	{
    		if (stmt.getSubject()!=null && stmt.getObject()!=null && stmt.getPredicate()!=null 
    				&& !stmt.getSubject().toString().equals("http://www.fluidops.com/null") 
    				&& !stmt.getObject().toString().equals("http://www.fluidops.com/null"))
    			nullFreeStatements.add(stmt);
    	}
		
		this.statements = nullFreeStatements;
	}
}
