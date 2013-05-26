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

package com.fluidops.iwb.api;

import java.net.URL;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.util.User;
import com.fluidops.util.Parse.StringToTypeParser;

/**
 * IWB CLI special handling
 * 
 * @author aeb
 */



public class CliParser implements StringToTypeParser
{
    
    static ThreadLocal<String> className = new ThreadLocal<String>();
    
	@Override
	public boolean acceptType(Class<?> type)
	{
		if ( Class.class.isAssignableFrom( type ) )
			return true;
		if ( Value.class.isAssignableFrom( type ) )
			return true;
		if ( User.class.isAssignableFrom( type ) )
			return true;
		if ( Operator.class.isAssignableFrom( type ) )
			return true;
		if ( URL.class.isAssignableFrom(type))
		    return true;
		if ( URI.class.isAssignableFrom(type))
			return true;
		return false;
	}

	@Override
	public Object parse(String s, Class<?> type) throws Exception
	{
		if ( Operator.class.isAssignableFrom( type ) )	{
		    return Operator.parse(s);
		}
		if ( Class.class.isAssignableFrom( type ) )
		{
			// s has to be the fully qualified class name when used from CLI
			try {
				className.set(s);
				return Class.forName(s);
			} catch (Exception e) {
				throw new IllegalAccessException("'" + s + "' cannot be converted to Class: " + e.getMessage());
			}			
		}
		if ( Value.class.isAssignableFrom( type ) )
		{
		    return EndpointImpl.api().getNamespaceService().parseValue(s);
		}
		if ( User.class.isAssignableFrom( type ) )
		{
			User user = new User();
			user.username = s.substring( 0, s.indexOf( ':' ) );
			user.password = s.substring( s.indexOf( ':' )+1, s.length() );
			return user;
		}
		if(URL.class.isAssignableFrom(type))
		{
		    return new URL(s);
		}
		if (URI.class.isAssignableFrom(type)) {
			return EndpointImpl.api().getNamespaceService().guessURI(s);
		}
		return s;
	}
}
