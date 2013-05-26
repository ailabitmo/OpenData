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

package com.fluidops.iwb.provider;

import java.io.Serializable;

/**
 * XML out to RDF statement mapping
 * Mapping is in the form of a statement already, however, the values can be one of
 * 1) absolute XPath: can only appear in one of the 4 slots
 * 2) relative XPath
 * 3) literal / resource
 * Note that one of object or value must be null
 * 
 * @author aeb
 */
public class Mapping implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String context;
    
    public String subject;
    public String predicate;
    public String object;
    
    /**
     * need default constructor
     */
    public Mapping() {}
    
    public Mapping(String subject, String predicate, String object)
    {
        this( null, subject, predicate, object );
    }
    
    public void normalize()
    {
        if ( context == null )
        {
            if ( subject.startsWith( "/" ) )
            {
                context = subject;
                subject = "./";
            }
            else if ( predicate.startsWith( "/" ) )
            {
                context = predicate;
                predicate = "./";
            }
            else if ( object.startsWith( "/" ) )
            {
                context = object;
                object = "./";
            }
        }
        
        // context must be absolute
        if ( context == null || context.charAt(0) != '/' )
            throw new RuntimeException( "Mapping context must be absolute" );

        // all others must be strings or must be relative queries
        check( subject );
        check( predicate );
        check( object );
    }
    
    public Mapping(String context, String subject, String predicate, String object)
    {
        this.context = context;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }
    
    protected void check( String s )
    {
        if ( s.contains( "/" ) )
            if ( s.charAt(0) == '/' )
                throw new RuntimeException( "Mapping subject, predicate, object, value must be simple text values or absolute queries" );
    }
}
