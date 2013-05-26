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

package com.fluidops.iwb.wiki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.util.ObjectHolder;

/**
 * parser for nested template parameters / widget config
 * 
 * @author aeb
 */
public class TP
{
	public static final String OPEN = "{{";
	public static final String SINGLE = "'";
	public static final String DOUBLE = "\"";
	
	/**
	 * parse nested wiki config
	 * 
	 * @param s		a config such as {{ a=b | c={{1|2|3}} }} or simple strings with or without quotes
	 * @return		a mix of list, map, string, bool, int, double representing the value tree
	 */
	public static Object parse(String s)
	{
		if ( s == null )
			return null;
		
		s = s.trim();
		if ( ! s.startsWith( OPEN ) )
		{
			s = s.replaceAll("\\{\\{Pipe\\}\\}", "|");
			
			if ( s.startsWith( SINGLE ) && s.endsWith( SINGLE ))
				return s.substring( 1, s.length()-1 );
			if ( s.startsWith( DOUBLE ) && s.endsWith( DOUBLE ))
				return s.substring( 1, s.length()-1 );
			
			try
			{
				return new Integer(s);
			}
			catch ( NumberFormatException noInteger )
			{
				try
				{
					return new Double(s);
				}
				catch ( NumberFormatException noDouble )
				{
					if ( "true".equals( s ) )
						return true;
					if ( "false".equals( s ) )
						return false;
					return s;
				}
			}
		}
		else
		{
			s = "{{ dummy |" + s.substring( 2 );
		}
		
		final ObjectHolder<Object> holder = new ObjectHolder<Object>();
		
        FluidWikiModel wikiModel = new FluidWikiModel(null, null);
        wikiModel.addTemplateResolver( new TemplateResolver() 
        {
			@Override
			public String resolveTemplate(String namespace,
					String templateName,
					Map<String, String> pars, URI page,
					FComponent parent)
			{
				// array if key 1 exists
				if ( pars.containsKey( "1" ) )
				{
					// array
					List list = new ArrayList();
					for ( String s : pars.values() )
						list.add( parse(s) );
					holder.value = list;
				}
				else
				{
					// struct
					Map map = new HashMap();
					for ( Entry<String, String> entry : pars.entrySet() )
						map.put( entry.getKey(), parse( entry.getValue()) );
					holder.value = map;
				}
				return null;
			}
		} );
        wikiModel.render( s );
        return holder.value;
	}
}
