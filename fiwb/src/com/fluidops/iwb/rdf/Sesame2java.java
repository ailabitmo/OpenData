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

package com.fluidops.iwb.rdf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.api.ListType;
import com.fluidops.iwb.api.CommunicationServiceImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;

/**
 * dynamic proxy which handles transparent translation between java and RDF worlds
 * 
 * @author aeb
 */
public class Sesame2java implements InvocationHandler, FURI
{
	/**
	 * sesame connection
	 */
	RepositoryConnection con;
	
	/**
	 * sesame repository
	 */
	Repository rep;
	
	/**
	 * URI representing this java instance
	 */
	URI uri;

	/**
	 * java interface used to access the RDF data
	 */
	Class<? extends FURI> ifc;

	/**
	 * (optional) RDF context to use when accessing the RDF store
	 */
	URI context;
	
	/**
	 * make an RDF URI look like a java instance
	 * @param <T>	interface to use
	 * @param uri	URI to use
	 * @param ifc	interface class
	 * @param context RDF context to use
	 * @return		java instance
	 */
	public static <T extends FURI> T create( RepositoryConnection con, URI uri, Class<T> ifc, URI context )
	{
		Sesame2java factory = new Sesame2java();
		factory.con = con;
		factory.uri = uri;
		factory.ifc = ifc;
		factory.context = context;
		return (T)Proxy.newProxyInstance( uri.getClass().getClassLoader(), new Class[] {ifc}, factory ); 
	}

	/**
	 * make an RDF URI look like a java instance
	 * @param <T>	interface to use
	 * @param uri	URI to use
	 * @param ifc	interface class
	 * @param context RDF context to use
	 * @return		java instance
	 */
	public static <T extends FURI> T create( Repository rep, URI uri, Class<T> ifc, URI context )
	{
		Sesame2java factory = new Sesame2java();
		factory.rep = rep;
		factory.uri = uri;
		factory.ifc = ifc;
		factory.context = context;
		return (T)Proxy.newProxyInstance( uri.getClass().getClassLoader(), new Class[] {ifc}, factory ); 
	}

	/**
	 * like create, but returns a list of all instances of T
	 * @param <T>			return list of this type
	 * @param con			connection to use
	 * @param s				query s
	 * @param p				query p
	 * @param o				query o
	 * @param ifc			class of T
	 * @param context		context to use (optional)
	 * @param useSubject	if true, of the triples matching (s,p,o) use the subject as a URI of the resulting pojos, use object otherwise
	 * @return				list of pojos whose URIs match the s,p,o query
	 */
	public static <T extends FURI> List<T> list( RepositoryConnection con, Resource s, URI p, Value o, Class<T> ifc, URI context, boolean useSubject )
	{
		try
		{
			List<T> list = new ArrayList<T>();
			RepositoryResult<Statement> res;
			if ( context == null )
				res = con.getStatements(null, RDF.TYPE, CommunicationServiceImpl.getClassURI(ifc), false);
			else
				res = con.getStatements(null, RDF.TYPE, CommunicationServiceImpl.getClassURI(ifc), false, context);
			while ( res.hasNext() )
			{
				T t = create(con, useSubject ? (URI)res.next().getSubject() : (URI)res.next().getObject(), ifc, context);
				list.add( t );
			}
			return list;
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * invokation handler
	 * @param proxy		proxy instance
	 * @param method	method called
	 * @param args		methods args
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		// implicit connection management
		if ( rep != null )
			open();
		try
		{
		if ( method.getDeclaringClass().equals( Object.class ) )
			return method.invoke(this, args);
		else if ( method.getDeclaringClass().equals( FURI.class ) && !method.getName().equals( "getTypes" ) && !method.getName().equals( "setTypes" ) )
			return method.invoke(this, args);
		else
		{
			if ( method.getParameterTypes().length == 0 && !method.getReturnType().equals( void.class ) )
			{
				// getter
				if ( Collection.class.isAssignableFrom( method.getReturnType() ) )
				{
					Class<?> retType = String.class;
					ListType lt = method.getAnnotation( ListType.class );
					if ( lt != null )
						retType = lt.value();
					List<Object> res = new ArrayList<Object>();
					for ( Value value : walkN( uri, getPredicate( method ) ) )
						res.add( value2java( value, retType ) );
					return res;
				}
				else
				{
					Value value = walk( uri, getPredicate( method ) );
					return value2java( value, method.getReturnType() );
				}
			}
			else if ( method.getParameterTypes().length == 1 && method.getReturnType().equals( void.class ) )
			{
				// setter
				if ( args[0] instanceof Collection )
				{
					RepositoryResult<Statement> stmts = getStatements( uri, getPredicate( method ), null );
					while ( stmts.hasNext() )
						remove( stmts.next() );
					ListType lt = method.getAnnotation( ListType.class );
					for ( Object arg : (Collection)args[0] )
						if ( lt != null && FURI.class.isAssignableFrom( lt.value() ) )
							add( ValueFactoryImpl.getInstance().createStatement( uri, getPredicate( method ), ((FURI)arg).getURI() ) );
						else
							add( ValueFactoryImpl.getInstance().createStatement( uri, getPredicate( method ), java2literal( arg ) ) );
				}
				else if ( args[0] instanceof FURI )
					set( uri, getPredicate( method ), ((FURI)args[0]).getURI() );
				else
					set( uri, getPredicate( method ), java2literal( args[0] ) );
				return null;
			}				
		}
		throw new RuntimeException( "unimlemented: "+method );
		}
		finally
		{
			// implicit connection management
			if ( rep != null )
				close();
		}
	}
	
	protected URI getPredicate( Method method )
	{
		return CommunicationServiceImpl.getPredicate(method,false);
	}
	
	/**
	 * translates the setter command into write / delete commands on the RDF store
	 * @param uri			
	 * @param predicate
	 * @param object
	 * @throws RepositoryException
	 */
	protected void set(URI uri, URI predicate, Value object) throws RepositoryException
	{
		Statement stmt = getStatement(uri, predicate);
		if ( stmt != null )
			remove(stmt);
		if ( object != null )
			add( ValueFactoryImpl.getInstance().createStatement( uri, predicate, object ) );
	}

	/**
	 * convert a literal into an instance of type
	 * @param value
	 * @param type
	 * @return
	 */
	protected Object value2java(Value value, Class type)
	{
		if ( value == null )
			return null;
		if ( FURI.class.isAssignableFrom( type ) )
		{
			Object res = Sesame2java.create(con, (URI) value, type, context);
			return res;
		}
		else
		{
			if ( type.equals(String.class) )
				return value.stringValue();
			if ( type.equals(Byte.class) )
				return new Byte(value.stringValue());
			if ( type.equals(Double.class) )
				return new Double(value.stringValue());
			if ( type.equals(Float.class) )
				return new Float(value.stringValue());
			if ( type.equals(Boolean.class) )
				return Boolean.valueOf(value.stringValue());
			if ( type.equals(Integer.class) )
				return new Integer(value.stringValue());
			if ( type.equals(Short.class) )
				return new Short(value.stringValue());
			if ( Value.class.isAssignableFrom( type ) )
				return value;
			if ( Date.class.isAssignableFrom( type ) )
				return ReadDataManagerImpl.ISOliteralToDate( value.stringValue() );
			throw new RuntimeException( "unimlemented: "+type );
		}
	}

	/**
	 * convert a java object to an RDF literal
	 * @param java
	 * @return
	 */
	protected Value java2literal( Object java )
	{
		if ( java == null )
			return null;
		if ( java instanceof String )
			return ValueFactoryImpl.getInstance().createLiteral( (String)java );
		if ( java instanceof Integer )
			return ValueFactoryImpl.getInstance().createLiteral( (Integer)java );
		if ( java instanceof Boolean )
			return ValueFactoryImpl.getInstance().createLiteral( (Boolean)java );
		if ( java instanceof Byte )
			return ValueFactoryImpl.getInstance().createLiteral( (Byte)java );
		if ( java instanceof Float )
			return ValueFactoryImpl.getInstance().createLiteral( (Float)java );
		if ( java instanceof Double )
			return ValueFactoryImpl.getInstance().createLiteral( (Double)java );
		if ( java instanceof Short )
			return ValueFactoryImpl.getInstance().createLiteral( (Short)java );
		if ( java instanceof Date )
			return ValueFactoryImpl.getInstance().createLiteral( ReadDataManagerImpl.dateToISOliteral((Date)java) );
		if ( java instanceof Value )
			return (Value)java;
		if ( java.getClass().isEnum() )
			return ValueFactoryImpl.getInstance().createLiteral( java.toString() );
		throw new RuntimeException( "unimlemented: "+java.getClass() );
	}
	
	/**
	 * get a statement that matches (uri, predicate, ?)
	 * @param uri
	 * @param predicate
	 * @return
	 * @throws RepositoryException
	 */
	protected Statement getStatement( URI uri, URI predicate ) throws RepositoryException
	{
		RepositoryResult<Statement> res = getStatements(uri, predicate, null);
			
		if ( ! res.hasNext() )
			return null;
		else
			return res.next();
	}

	/**
	 * get a statement's object that matches (uri, predicate, ?)
	 * @param uri
	 * @param predicate
	 * @return
	 * @throws RepositoryException
	 */
	protected Value walk( URI uri, URI predicate ) throws RepositoryException
	{
		Statement stmt = getStatement(uri, predicate);
		if ( stmt == null )
			return null;
		else
			return stmt.getObject();
	}

	/**
	 * get all statement's object that match (uri, predicate, ?)
	 * @param uri
	 * @param predicate
	 * @return
	 * @throws RepositoryException
	 */
	protected Collection<Value> walkN( URI uri, URI predicate ) throws RepositoryException
	{
		RepositoryResult<Statement> stmt = getStatements(uri, predicate, null);
		List<Value> res = new ArrayList<Value>();
		while ( stmt.hasNext() )
			res.add( stmt.next().getObject() );
		return res;
	}

	/*
	 * FURI methods
	 */
	
	@Override
	public Statement getStatement(String prop)
	{
		try
		{
			return getStatement( uri, CommunicationServiceImpl.getPredicateURI( ifc, prop ) );
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public RepositoryResult<Statement> getStatements(String prop)
	{
		try
		{
			return getStatements( uri, CommunicationServiceImpl.getPredicateURI( ifc, prop ), null );
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public URI getURI()
	{
		return uri;
	}

	/*
	 * object methods are delegated to uri
	 */
	
	@Override
	public String toString()
	{
		return uri.toString();
	}
	
	@Override
	public boolean equals( Object o )
	{
		if ( o instanceof FURI )
			return uri.equals(((FURI)o).getURI());
		else
			return false;
	}
	
	@Override
	public int hashCode()
	{
		return uri.hashCode();
	}
	
	@Override
	public Collection<URI> getTypes()
	{
		throw new RuntimeException("assertion error: should be handled by dynamic proxy");
	}

	@Override
	public void setTypes(Collection<URI> types)
	{
		throw new RuntimeException("assertion error: should be handled by dynamic proxy");
	}
	
	/*
	 * con methods
	 */
	
	protected void open() throws RepositoryException
	{
		con = rep.getConnection();
	}
	
	protected void close() throws RepositoryException
	{
		con.close();
	}
	
	protected RepositoryResult<Statement> getStatements(Resource s, URI p, Value o) throws RepositoryException
	{
		if ( context == null )
			return con.getStatements( s, p, o, false );
		else
			return con.getStatements( s, p, o, false, context );
	}
	
	protected void remove( Statement stmt ) throws RepositoryException
	{
		if ( context == null )
			con.remove( stmt );
		else
			con.remove( stmt, context );
	}
	
	protected void add( Statement stmt ) throws RepositoryException
	{
		if ( context == null )
			con.add( stmt );
		else
			con.add( stmt, context );
	}
}
