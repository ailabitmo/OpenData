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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.iwb.api.CommunicationServiceImpl;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;

/**
 * like Sesame2java but basing on ReadWriteDataManager API
 * 
 * @author aeb
 */
public class DataManager2java extends Sesame2java
{
	/**
	 * sesame connection
	 */
	ReadWriteDataManager con;
	
	/**
	 * (optional) RDF context to use when accessing the RDF store
	 */
	Context context;
	
	public static <T extends FURI> T create( ReadWriteDataManager con, URI uri, Class<T> ifc, Context context )
	{
		DataManager2java factory = new DataManager2java();
		factory.con = con;
		factory.uri = uri;
		factory.ifc = ifc;
		factory.context = context;
		return (T)Proxy.newProxyInstance( uri.getClass().getClassLoader(), new Class[] {ifc}, factory ); 
	}
	
	public static <T extends FURI> T create( Repository rep, URI uri, Class<T> ifc, Context context )
	{
		DataManager2java factory = new DataManager2java();
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
	public static <T extends FURI> List<T> list( ReadWriteDataManager con, Resource s, URI p, Value o, Class<T> ifc, Context context, boolean useSubject )
	{
		try
		{
			List<T> list = new ArrayList<T>();
			RepositoryResult<Statement> res;
			if ( context == null )
				res = con.getStatements(null, RDF.TYPE, CommunicationServiceImpl.getClassURI(ifc), false);
			else
				res = con.getStatements(null, RDF.TYPE, CommunicationServiceImpl.getClassURI(ifc), false, context.getURI());
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
	
	/*
	 * con methods
	 */
	
	@Override
	protected void open() throws RepositoryException
	{
		con = ReadWriteDataManagerImpl.openDataManager(rep);
	}
	
	@Override
	protected void close() throws RepositoryException
	{
		con.close();
	}
	
	protected RepositoryResult<Statement> getStatements(Resource s, URI p, Value o) throws RepositoryException
	{
		if ( context == null )
			return con.getStatements( s, p, o, false );
		else
			return con.getStatements( s, p, o, false, context.getURI());
	}
	
	protected void remove( Statement stmt ) throws RepositoryException
	{
		if ( context == null )
			throw new RuntimeException( "writing RDF without context is not supported" );
		else
		{
			if ( context.getURI().equals( stmt.getContext() ) )
				con.removeInSpecifiedContext(stmt, null);
			else
				throw new RuntimeException( "assertion failure: context mismatch" );
		}
	}
	
	protected void add( Statement stmt ) throws RepositoryException
	{
		if ( context == null )
			throw new RuntimeException( "writing RDF without context is not supported" );
		else
			con.addToContext( stmt, context );
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);		// override for Findbugs warning
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}		
}
