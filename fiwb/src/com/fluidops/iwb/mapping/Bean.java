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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * jsp bean used for the mapping UI prototype.
 * The prototype was dropped in favor of the eclipse debugging view
 * in conjunction with the API framework
 * 
 * @author aeb
 */
@SuppressWarnings(value="UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD", justification="Fields are accessed externally")
public class Bean
{
	public Data data;
	public List<String> steps = new ArrayList<String>();
	
	public String type;
	public String objectsOfPredicate;
	public String subjectsOfPredicate;
	public List<String> compareTo;
	
	public String run( String command, String... parameters )
	{
		try
		{
			Class<?> clazz = Data.class;
			if ( data != null )
				clazz = data.getClass();
    		for ( Method method : clazz.getMethods() )
    			if ( method.getName().equals( command ) )
    			{
    				Object[] _parameters = processParameters( method.getParameterTypes(), parameters );
    				Object result = method.invoke(data, _parameters);
    				if ( result instanceof Data )
    					data = (Data)result;
    				else
    					return ""+result;
    			}
    		StringBuilder x = new StringBuilder();
    		for ( String p : parameters )
    			x.append(", ").append(p);
    		steps.add( command + "(" + x + ");" );
    		return null;
		}
		catch ( InvocationTargetException e )
		{
			return e.getTargetException().toString();
		}
		catch ( Exception e )
		{
			return e.toString();
		}
	}

	protected Object[] processParameters(Class<?>[] parameterTypes, String[] parameters)
	{
		Object[] _parameters = new Object[ parameterTypes.length ];
		for (int i=0; i<parameterTypes.length;i++)
		{
			_parameters[i] = parameters[i];
		}
		return _parameters;
	}
	
	public List<String> possibleCommands()
	{
		List<String> res = new ArrayList<String>();
		Class<?> clazz = Data.class;
		if ( data != null )
			clazz = data.getClass();
		for ( Method method : clazz.getMethods() )
			if ( ! method.getDeclaringClass().equals(Object.class) )
				if ( data == null )
				{
					if ( Modifier.isStatic(method.getModifiers()) )
						res.add( method.getName() );
				}
				else
				{
					if ( ! Modifier.isStatic(method.getModifiers()) )
						if ( ! method.getDeclaringClass().equals(Data.class) )
							res.add( method.getName() );
				}
		return res;
	}
}
