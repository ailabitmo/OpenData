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

import java.rmi.RemoteException;
import java.util.List;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.provider.TableProvider.Table;

/**
 * helper methods to align mapped resources to resources existing in the current DB
 * 
 * @author aeb
 */
public class Align
{
	/**
	 * performs one of the alignments implemented in the 4 methods below
	 */
	public static List<String> oneOf( String sparql, String subjectsOfPredicate, String objectsOfPredicate, String type ) throws RemoteException, Exception
	{
		if ( objectsOfPredicate != null )
			return Align.objectsOfPredicate(objectsOfPredicate);
		if ( subjectsOfPredicate != null )
			return Align.subjectsOfPredicate(subjectsOfPredicate);
		if ( type != null )
			return Align.type(type);
		if ( sparql != null )
    		return Align.sparql(sparql);
		return null;
	}

	/**
	 * executes a sparql query and returns the first column of the result table
	 * @param query			sparql query to run
	 * @return				first column of the result table
	 * @throws Exception
	 */
	public static List<String> sparql( String query ) throws Exception
	{
		Table table = EndpointImpl.api().getDataManager().sparqlSelectAsTable( query );
		return table.getColumn(0);
	}
	
	/**
	 * returns all instances of the given type in the DB
	 */
	public static List<String> type( String type ) throws Exception
	{
	    String defaultNS = EndpointImpl.api().getNamespaceService().defaultNamespace();
		return sparql( "SELECT ?s WHERE {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + defaultNS +type+">}" );
	}
	
	/**
	 * returns all objects that appear in triples with the given predicate
	 */
	public static List<String> objectsOfPredicate( String predicate ) throws Exception
	{
        String defaultNS = EndpointImpl.api().getNamespaceService().defaultNamespace();
	    return sparql( "SELECT ?o WHERE {?s <" + defaultNS+predicate+"> ?o}" );
	}
	
	/**
	 * returns all subjects that appear in triples with the given predicate
	 */
	public static List<String> subjectsOfPredicate( String predicate ) throws Exception
	{
	    String defaultNS = EndpointImpl.api().getNamespaceService().defaultNamespace();
		return sparql( "SELECT ?s WHERE {?s <" + defaultNS+predicate+"> ?o}" );
	}
}
