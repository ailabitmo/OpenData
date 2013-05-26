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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.mapping.TableData.PropertyType;
import com.fluidops.iwb.provider.ProviderURIResolver;

/**
 * simple graph which serves as a pre cursor to RDF
 * 
 * @author aeb
 */
public class Graph implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(Graph.class.getName());
	
	/**
	 * string to graph node lookup map
	 */
	public Map<String, Node<String>> nodes = new HashMap<String, Node<String>>();
	
	/**
	 * graph edges set
	 */
	public Set<Edge<String>> edges = new HashSet<Edge<String>>();

	public String toString()
	{
		StringBuffer b = new StringBuffer();
		for ( Edge edge : edges )
			b.append( edge + "\n" );
		return b.toString();
	}
	
	/**
	 * add node, check for duplicates
	 * @param name	node name
	 * @return		corresponding graph node object
	 */
	public Node addNode( String name )
	{
		if ( nodes.containsKey( name ) )
			return nodes.get(name);
		Node node = new Node();
		node.name = name;
		nodes.put(name, node);
		return node;
	}

	/**
	 * graph node
	 * 
	 * @author aeb
	 */
	public static class Node<T> implements Serializable
	{
		/**
		 * node name
		 */
		public T name;
		
		/**
		 * equality delegated to name equality
		 */
		@Override
		public boolean equals( Object o )
		{
			if ( o instanceof Node )
			{
				Node e = (Node)o;
				return e.name.equals( name );
			}
			return false;
		}
		
		/**
		 * hash delegated to name hash
		 */
		@Override
		public int hashCode()
		{
			return name.hashCode();
		}
		
		public String toString()
		{
			if ( name == null )
				return "null";
			else
				return name.toString();
		}
	}

	/**
	 * graph edge as s,p,o triple
	 * 
	 * @author aeb
	 */
	public static class Edge<T> implements Serializable
	{
		public Node<T> subject;
		public Node<T> predicate;
		public Node<T> object;
		
		public Edge(Node<T> subject, Node<T> predicate, Node<T> object)
		{
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
		}

		/**
		 * equality delegated to s,p,o node equality
		 */
		@Override
		public boolean equals( Object o )
		{
			if ( o instanceof Edge )
			{
				Edge<T> e = (Edge<T>)o;
				return e.subject.equals( subject ) && e.predicate.equals( predicate ) && e.object.equals( object );
			}
			return false;
		}
		
		/**
		 * hash delegated to s,p,o node hash
		 */
		@Override
		public int hashCode()
		{
			return subject.hashCode()+predicate.hashCode()+object.hashCode();
		}
		
		public String toString()
		{
			return subject.name + "\t" + predicate.name + "\t" + object.name;
		}

		/**
		 * convert to RDF
		 */
		public Statement toStatement(ProviderURIResolver uriResolver,Map<String,PropertyType> edgetypes)
		{
			ValueFactory vf = ValueFactoryImpl.getInstance();
	        NamespaceService ns = EndpointImpl.api().getNamespaceService();
				        
		    PropertyType ct = edgetypes.get((String)predicate.name);
		    if (ct==null)
		    	ct = PropertyType.UNKNOWN;

			URI s = ns.guessURIOrCreateInDefaultNS((String)subject.name);
			URI p = (uriResolver==null) ?
				ns.guessURIOrCreateInDefaultNS((String)predicate.name) :
            	uriResolver.resolveProperty((String)predicate.name,null,null);
				
			// the object is build depending on the edge type...
			Value o = null;
			switch (ct)
			{
				case OBJECT_PROPERTY:
				case OBJECT_PROPERTY_INVERSE:					
					o = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS((String)object.name);
					break;
				case DATATYPE_PROPERTY:
					o = vf.createLiteral((String)object.name);
					break;
				default:
				{
					String name = (String)object.name;
			        if (p.equals(RDF.TYPE) 
			        		|| p.equals(RDFS.SUBCLASSOF)
			                || p.equals(RDFS.SUBPROPERTYOF)
			                || p.equals(RDFS.DATATYPE)
			                || p.equals(RDFS.DOMAIN)
			                || p.equals(RDFS.RANGE))
			            o = EndpointImpl.api().getNamespaceService().guessURI(name);
			        else
			            o = vf.createLiteral(name);
				}
			}
			
			// make sure there's no null inside some positions of the statement
			if (s==null || p==null || o==null)
				return null;
			
			if (ct.equals(PropertyType.OBJECT_PROPERTY_INVERSE))
				return vf.createStatement((URI)o, p, s);
			else
				return vf.createStatement(s, p, o);
				
		}
	}

	/**
	 * convert to RDF by converting all edges
	 */
	public List<Statement> toRDF(ProviderURIResolver uriResolver, Map<String,PropertyType> edgetypes)
	{
		logger.debug("Starting Transformation to RDF");
		Long start = System.currentTimeMillis();
		
		List<Statement> res = new ArrayList<Statement>();
		
		int i=1;
		Long startIt = System.currentTimeMillis();
		for ( Edge edge : edges )
		{
			Statement stmt = edge.toStatement(uriResolver,edgetypes);
			if (stmt!=null) 
				res.add(stmt);
			
			if (++i%1000==0)
			{
				Long endIt = System.currentTimeMillis();		
				logger.trace("Transformed next 10000 edges in = " + (endIt-startIt)+ "ms");

				i = 1;
				startIt = System.currentTimeMillis();
			}
				
		}
		
		Long end = System.currentTimeMillis();
		logger.debug("Finished Transformation to RDF, total time = " + (end-start)/100 + "s");
		
		return res;
	}
	
	/**
	 * convert to RDF taking given schema into account
	 */
	public List<Statement> toRDF(String schemafile)
	{
        List<Statement> stmts = new ArrayList<Statement>();
	    if(schemafile.endsWith("rdf")||schemafile.endsWith("rdfs")) {


	        Map<String, URI> properties = new HashMap<String,URI>();
	        Map<URI, URI> ranges = new HashMap<URI,URI>();
	        Map<URI, URI> domains = new HashMap<URI,URI>();

	        Repository schema = new SailRepository(new ForwardChainingRDFSInferencer(new MemoryStore()));

	        try
	        {       
	            schema.initialize();
	            RepositoryConnection con = schema.getConnection(); 
	            try {
	            	con.add(new File(schemafile), null, RDFFormat.RDFXML);
	            }
	            finally {
	            	con.close();
	            }
	        }
	        catch (Exception e)
	        {
	            logger.error(e.getMessage(), e);
	        }


	        ValueFactory f = new ValueFactoryImpl();

	        URI property = null;
	        URI range = null;
	        URI domain = null;
	        String label= null;
	        properties.put("label", RDFS.LABEL);

	        try
	        {

	        	RepositoryConnection con = schema.getConnection();
	        	try {
	            RepositoryResult<Statement> result = con.getStatements(null, RDFS.LABEL, null, false);

	            while(result.hasNext()) {
	                Statement stmt = result.next();
	                label = stmt.getObject().stringValue();
	                property = (URI)stmt.getSubject();
	                properties.put(label, property);
	            }   
	        	
	            result = con.getStatements(null, RDFS.RANGE, null, false);

	            while(result.hasNext()) {
	                Statement stmt = result.next();
	                range = (URI)stmt.getObject();
	                property = (URI)stmt.getSubject();
	                ranges.put(property, range);
	            }   

	            result = con.getStatements(null, RDFS.DOMAIN, null, false);
	            while(result.hasNext()) {
	                Statement stmt = result.next();
	                domain = (URI)stmt.getObject();
	                property = (URI)stmt.getSubject();
	                domains.put(property, domain);
	            }   
	        	}
	        	finally {
	        		con.close();
	        	}
	        }
	        catch (Exception e)
	        {
	            logger.error(e.getMessage(), e);
	        }

	        ReadDataManager dm = EndpointImpl.api().getDataManager();
	        NamespaceService ns = EndpointImpl.api().getNamespaceService();
	        
	        for ( Edge<String> edge : edges )
	        {

	            URI subject = null;

	            property = properties.get(edge.predicate.name);
	            if(property==null)
	                property = ns.guessURI(edge.predicate.name);

	            range = ranges.get(property);
	            if(range==null)
	                range = XMLSchema.STRING;

	            domain = domains.get(property);

	            subject = ns.guessURI(edge.subject.name);


	            if(domain!=null) 
	                stmts.add(ReadDataManagerImpl.s(subject, RDF.TYPE, domain));
	            if(range.stringValue().startsWith("http://www.w3.org/2001/XMLSchema#"))
	                stmts.add(ReadDataManagerImpl.s(subject, property, f.createLiteral(edge.object.name)));
	            else 
	            {
	                URI object = dm.lookupURIfromName(edge.object.name);
	                if(object==null)
	                    object = ns.guessURI(edge.object.name);
	                stmts.add(ReadDataManagerImpl.s(subject, property,object ));
	                stmts.add(ReadDataManagerImpl.s(object, RDF.TYPE, range));

	            }
	        }
	    }
/*	    else if(schemafile.endsWith("owl")) {

            Map<String, OWLObjectProperty> objectProperties = new HashMap<String,OWLObjectProperty>();
            Map<String, OWLDataProperty> dataProperties = new HashMap<String,OWLDataProperty>();
            Map<URI, URI> ranges = new HashMap<URI,URI>();
            Map<URI, URI> domains = new HashMap<URI,URI>();
            
            OWLOntology schema = OWLManager.createOWLOntologyManager().loadOntology(new FileInputSource(new File(schemafile)));

            
            OWLObjectProperty objectProperty = null;
            OWLDataProperty dataProperty = null;
            URI range = null;
            URI domain = null;
            String label= null;
            properties.put("label", RDFS.LABEL);

            try
            {

                RepositoryResult<Statement> result = schema.getConnection().getStatements(null, RDFS.LABEL, null, false);

                while(result.hasNext()) {
                    Statement stmt = result.next();
                    label = stmt.getObject().stringValue();
                    property = (URI)stmt.getSubject();
                    properties.put(label, property);
                }   

                result = schema.getConnection().getStatements(null, RDFS.RANGE, null, false);

                while(result.hasNext()) {
                    Statement stmt = result.next();
                    range = (URI)stmt.getObject();
                    property = (URI)stmt.getSubject();
                    ranges.put(property, range);
                }   

                result = schema.getConnection().getStatements(null, RDFS.DOMAIN, null, false);
                while(result.hasNext()) {
                    Statement stmt = result.next();
                    domain = (URI)stmt.getObject();
                    property = (URI)stmt.getSubject();
                    domains.put(property, domain);
                }   
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }

            for ( Edge edge : edges )
            {

                URI subject = null;

                property = properties.get(edge.predicate.name);
                if(property==null)
                    property = Util.getURIFromName(edge.predicate.name);

                range = ranges.get(property);
                if(range==null)
                    range = XMLSchema.STRING;

                domain = domains.get(property);

                subject = Util.getURIFromName(edge.subject.name);


                if(domain!=null) 
                    stmts.add(Util.s(subject, RDF.TYPE, domain));

                if(range.stringValue().startsWith("http://www.w3.org/2001/XMLSchema#"))
                    stmts.add(Util.s(subject, property, f.createLiteral(edge.object.name)));
                else 
                {
                    Util util = WikiServlet.getUtil();
                    URI object = util.lookupURIfromName(edge.object.name);
                    if(object==null)
                        object = Util.getURIFromName(edge.object.name);
                    stmts.add(Util.s(subject, property,object ));
                    stmts.add(Util.s(object, RDF.TYPE, range));
                }

            }
	    }*/
	        
	    else 
	        logger.warn("Format of schema unknown.");
	    return stmts;
	}



    /**
	 * shrinks a graph by merging subject and object of a given predicate.
	 * If the graph contains (x,y,s) (s,x,y) (s,p,o) (a,b,o) (o,a,b)
	 * and collapsePredicate(p) is called, the graph looks like this afterwards:
	 * (x,y,o) (o,x,y) (a,b,o) (o,a,b)
	 */
	public void collapsePredicate(String pred)
	{
		Node predicate = addNode(pred);
		for ( Edge edge : edges )
			if ( edge.predicate == predicate )
				renameNode(edge.subject, edge.object);
		remove( null, pred, null );
	}

	/**
	 * rename node - changes all triples that are affected
	 * (potentially all in case of a star shaped graph)
	 */
	public void renameNode( Node oldNode, Node newNode )
	{
		for ( Edge edge : edges )
		{
			if ( edge.subject == oldNode )
				edge.subject = newNode;
			if ( edge.predicate == oldNode )
				edge.predicate = newNode;
			if ( edge.object == oldNode )
				edge.object = newNode;
		}
	}
	
	/**
	 * remove edge duplicates
	 * (currently unused)
	 */
	protected void removeDuplicates()
	{
		edges = new HashSet<Edge<String>>( edges );
	}

	/**
	 * sometimes object values are comma seperated lists:
	 * s p o1,o2,o3. This operation splits this statement
	 * into 3 statements s p o1, s p o2, s p o3.
	 * 
	 * if labels are given, s p lat,lng can be split into
	 * s hasLat lat, s hasLng lng
	 */
	public void splitObject(String pred, String delimiterRegexp, String... labels)
	{
		Node<String> predicate = addNode( pred );
		List<Edge<String>> toAdd = new ArrayList<Edge<String>>();
		for ( Edge<String> edge : edges )
			if ( edge.predicate == predicate )
			{
				int i=0;
				for ( String part : edge.object.name.split( delimiterRegexp ) )
				{
					part = part.trim();
					Node<String> p;
					if ( labels.length == 0 )
						p = edge.predicate;
					else
						p = addNode( labels[ i % labels.length ] );
					toAdd.add( new Edge<String>( edge.subject, p, addNode( part ) ) );
					i++;
				}
			}
		edges.addAll( toAdd );
	}

	/**
	 * remove edges. s,p,o values of null represent wildcards.
	 * thus, remove(null,null,null) deletes all edges
	 */
	public void remove(String s, String p, String o)
	{
		Node subject = s == null ? null : addNode( s );
		Node predicate = p == null ? null : addNode( p );
		Node object = o == null ? null : addNode( o );
		
		List<Edge> toRemove = new ArrayList<Edge>();
		
		for ( Edge edge : edges )
		{
			if ( subject == null || subject == edge.subject )
				if ( predicate == null || predicate == edge.predicate )
					if ( object == null || object == edge.object )
						toRemove.add( edge );
		}
		
		edges.removeAll( toRemove );
	}

	/**
	 * scans all URIs in the graph. If the URI matches the regexp,
	 * a new triple URI, prediate, matchValue is added
	 * @param regexp
	 * @param predicate
	 */
	public void extractFactFromURI(String regexp, String predicate)
	{
		List<Edge<String>> toAdd = new ArrayList<Edge<String>>();
		Pattern p = Pattern.compile(regexp);
		
		for ( Node<String> node : new ArrayList<Node<String>>( nodes.values() ) )
		{
			 Matcher m = p.matcher(node.name);
			 if ( m.find() )
				 toAdd.add( new Edge( node, addNode( predicate ), addNode( m.group() ) ) );
		}
		
		edges.addAll( toAdd );
	}
}
