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
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * default implementations for PojoConfigurable - called by widgets, services, and providers
 * 
 * @author aeb
 */
public class PojoConfigurableHelper 
{
    private static final Logger logger = Logger.getLogger(PojoConfigurableHelper.class.getName());
    /**
     * default: 
     */
    public static List<String> getProps( PojoConfigurable pc )
    {
        List<String> res = new ArrayList<String>();
        if ( pc.getConfigClass() != null )
            for ( Field f : pc.getConfigClass().getFields() )
                res.add( f.getName() );
        return res;
    }
    
    /**
     * default: null
     *
     */
    //TODO: Check for errors
    public static List<Capability> getCapabilities( PojoConfigurable pc )
    {
    	return Collections.emptyList();
    }
    
    /**
     * default: can do it
     */
    public static boolean check( PojoConfigurable pc, Object config )
    {
        for ( Object p : pc.getRequiredProps() )
            try
            {
                if ( null == config.getClass().getField( (String)p ).get( config ) )
                    return false;
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
                return false;
            }    
        return true;
    }
    
    public static String getPropDoc( PojoConfigurable pc, String prop )
    {
        try
        {
            Field f = pc.getConfigClass().getField( prop );
            ParameterConfigDoc i = f.getAnnotation( ParameterConfigDoc.class );
            return i.desc();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
