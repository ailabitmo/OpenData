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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.Rand;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * presents a pojo graph as RDF model. Changes to the pojos themselves
 * are not intercepted. The pojo graph can however be modified via
 * the RDF API. The pojo fields are modified accordingly.
 * 
 * @author aeb
 */
public class PojoModel
{
	static class Statement
	{
		Statement(String subject, String predicate, String object)
		{
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
		}
		
		String subject;
		String predicate;
		String object;
	}
	
	/**
	 * root object of the model
	 */
    protected Object root;
    
    /**
     * resource id / primary key of the root object
     */
    protected String pk;
    
    /**
     * resource describing the model "homepage"
     */
    protected String id;
    
    /**
     * common statement timestamp
     */
    @SuppressWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="checked")
    protected Date now = new Date();
    
    /**
     * RDF representation of the object graph
     */
    protected List<Statement> data = new ArrayList<Statement>();
    
    /**
     * remember which objects have been traversed already
     */
    protected List<Object> visited = new ArrayList<Object>();
    
    /**
     * each object must have one pk it represents
     */
    protected Map<Object, String> pojo2id = new HashMap<Object, String>();
    
    /**
     * constructs a new model
     * @param root	root object that provides the model data
     * @param pk	pk of the root
     * @param id	model id
     */
    public PojoModel( Object root, String pk, String id )
    {
        this.root = root;
        this.pk = pk;
        this.id = id;
        
        load( pk, root );
    }

    protected void load( String subject, Object object )
    {
        pojo2id.put( object, subject );
        visited.add( object );
        for ( Field f : object.getClass().getFields() )
        {
            Object fget = null;
            try
            {
                fget = f.get( object );
                add( subject, f, fget );
            }
            catch (Exception e)
            {
            }
        }
    }

    protected void add( String subject, Field f, Object fget )
    {
        if ( fget == null )
            return;
            
        if ( isPrimitive( fget.getClass() ) )
        {
        	String fgetString = (""+fget).trim();
        	if ( fgetString.length() == 0 )
        		return;
            data.add( new Statement( subject, f.getName(), fgetString ) );
        }
        else if ( fget instanceof List )
        {
            List<?> list = (List<?>)fget;
            for ( Object item : list )
                add( subject, f, item );
        }
        else
        {
            String obj = pojo2id.get( fget );
            if ( obj == null )
                obj = "_" + Rand.getFluidUUID();
            if ( ! visited.contains( fget ) )
                load( obj, fget );
            data.add( new Statement( subject, f.getName(), obj ) );
        }
    }
    
    @SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Exception caught for robustness")
    public boolean delete(Statement s)
    {
        try
        {
            Object pojo = getPojo( s.subject );
            Field p = pojo.getClass().getField( s.predicate );
            
            // TODO: object is ignored right now
            // TODO: remove from list
            p.set( pojo, null );
            
            visited.clear();
            data.clear();
            load( pk, root );
            
            return true;
        }
        catch ( Exception e )
        {
            return false;
        }
    }

    protected Object getPojo( String key )
    {
        for ( Map.Entry<Object, String> x : pojo2id.entrySet() )
            if ( key.equals( x.getValue() ) )
                return x.getKey();
        return null;
    }
    
    @SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Exception caught for robustness")
    public boolean add(Statement s)
    {
        try
        {
            Object pojo = getPojo( s.subject );
            Field p = pojo.getClass().getField( s.predicate );
            if ( isPrimitive( p.getType() ) )
                p.set( pojo, s.object );
            else if ( List.class.isAssignableFrom( p.getType() ) )
            {
                // TODO: currently only lists of strings are supported
                if ( p.get( pojo ) == null )
                    p.set( pojo, new ArrayList<Object>() );
                List list = (List<?>)p.get( pojo );
                if ( ! list.contains( s.object ) )
                    list.add( s.object );
            }
            else
            {
                Object x = getPojo( s.object );
                if ( x == null )
                {
                    x = p.getType().newInstance();
                    pojo2id.put( x, s.object );
                }
                p.set( pojo, x );
            }            
            visited.clear();
            data.clear();
            load( pk, root );
            
            return true;
        }
        catch ( Exception e )
        {
            return false;
        }
    }

    public List<org.openrdf.model.Statement> list()
    {
    	List<org.openrdf.model.Statement> res = new ArrayList<org.openrdf.model.Statement>();
    	for ( Statement s : data )
    		res.add( EndpointImpl.api().getDataManager().s(s.subject, s.predicate, s.object) );
    	return res;
    }

    public String getId()
    {
        return id;
    }

    public boolean isWriteable()
    {
        return true;
    }

    /**
     * @deprecated  
     */
    private static boolean isPrimitive(Class type)
    {
        // TODO - consider enums, we have code for this already in XML2Pojo, API, etc.
        return type.isEnum() || type.isPrimitive() || type.equals( Date.class ) || type.getPackage().equals( String.class.getPackage() );
    }
}
