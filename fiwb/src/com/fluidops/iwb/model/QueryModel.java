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

package com.fluidops.iwb.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * filters statements of a base model according to a query
 * 
 * @author aeb
 */
public class QueryModel 
{
    private static final Logger logger = Logger.getLogger(QueryModel.class.getName());

    String q;
    Repository repository;
    
    public QueryModel( Repository rep, String q )
    {
        this.repository = rep;
        this.q = q.toLowerCase();
    }

    List<Statement> res;
    
    public List<Statement> list()
    {
        if ( res != null )
            return res;
        
        res = new ArrayList<Statement>();
        
        try {
        	RepositoryConnection con = repository.getConnection();
        	try {
				for ( Statement s : con.getStatements(null, null, null, true).asList() )
				{
				    if ( s.getSubject().stringValue().toLowerCase().contains( q ) )
				    {
				        res.add( s );
				        continue;
				    }
				    if ( s.getPredicate().stringValue().toLowerCase().contains( q ) )
				    {
				        res.add( s );
				        continue;
				    }
				    if ( s.getObject().stringValue().toLowerCase().contains( q ) )
				    {
				        res.add( s );
				        continue;
				    }
				    
				    // TODO: paging
				    if ( res.size() > 2000 )
				        break;
				}
        	}
        	finally {
        		con.close();
        	}
		} catch (RepositoryException e) {
			logger.error(e.getMessage(), e);
		}
        
        return res;
    }

    public String getId()
    {
        return null;
    }
}
