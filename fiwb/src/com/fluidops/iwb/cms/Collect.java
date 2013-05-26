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

package com.fluidops.iwb.cms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.Pair;

/**
 * abstract metadata collection class
 * 
 * @author aeb
 */
public abstract class Collect implements Collector
{
	private static final long serialVersionUID = -4065802176417122563L;
	
	protected ValueFactoryImpl vf = ValueFactoryImpl.getInstance();

	/**
	 * core collection method which is to be implemented
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public abstract List<Pair<String, String>> collect( File file ) throws IOException;

	/**
	 * null safe add
	 * @param res
	 * @param key
	 * @param value
	 */
	public void add( List<Pair<String, String>> res, String key, Object value )
	{
		if ( key != null && value != null )
			res.add(new Pair<String, String>(key, value.toString()));
	}
	
	public void add( List<Statement> res, Resource subject, URI predicate, Value object )
	{
		if ( subject != null && predicate != null && object != null )
			res.add( vf.createStatement(subject, predicate, object) );
	}
	
	public List<Statement> collectRDF( File file, URI subject ) 
	{
		// TODO: port purl / foaf mappings from MP3 and ImageProviders
		ValueFactoryImpl vf = ValueFactoryImpl.getInstance();
		List<Statement> res = new ArrayList<Statement>();
		try {
		    for ( Pair<String, String> i : collect(file) )
		    {
		        URI predicate;
		        Value object;

		        // TODO: align with provider utility framework
		        if ( i.fst.startsWith( "http" ) )
		            predicate = ValueFactoryImpl.getInstance().createURI( i.fst );
		        else
		            predicate = EndpointImpl.api().getNamespaceService().createURIInDefaultNS(i.fst);

		        if ( i.snd.startsWith( "http" ) )
		            object = ValueFactoryImpl.getInstance().createURI( i.snd );
		        else
		        {
		            try
		            {
		                object = ValueFactoryImpl.getInstance().createLiteral( new Integer( i.snd ) );
		            }
		            catch ( NumberFormatException e )
		            {
		                try
		                {
		                    object = ValueFactoryImpl.getInstance().createLiteral( new Double( i.snd ) );
		                }
		                catch ( NumberFormatException ex )
		                {
		                    object = EndpointImpl.api().getNamespaceService().createURIInDefaultNS(i.snd);
		                }
		            }
		        }

		        res.add( vf.createStatement(subject, predicate, object) );
		    }
		} catch(IOException ex) {
		    throw new IllegalStateException(ex);
		}
		
		return res;
	}
}
