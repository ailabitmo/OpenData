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

package com.fluidops.iwb.install;

import static org.apache.log4j.Logger.getLogger;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fluidops.iwb.api.solution.SolutionService;

public class PropertyMergerImpl implements PropertyMerger
{
	private static final Logger installLogger = getLogger(SolutionService.INSTALL_LOGGER_NAME);
	
    @Override
    public Properties merge(Properties userProperties, Properties systemProperties)
    {
        assert userProperties != null;
        assert systemProperties != null;
        Properties result = new Properties();
        result.putAll(systemProperties);
        result.putAll(userProperties);
        // find those properties that are overwritten
        Set<Object> overrideProps = new HashSet<Object>(userProperties.keySet());
        overrideProps.retainAll(systemProperties.keySet());
        for (Object prop : new HashSet<Object>(overrideProps)) {
        	// remove those where value is actually the same
    		if (userProperties.get(prop).equals(systemProperties.get(prop)))
    			overrideProps.remove(prop);
    	}
        if (!overrideProps.isEmpty()) {        	
        	installLogger.warn("Properties " + overrideProps.toString() + " have been overridden by solution properties.");
        }
        return result;
    }
}
