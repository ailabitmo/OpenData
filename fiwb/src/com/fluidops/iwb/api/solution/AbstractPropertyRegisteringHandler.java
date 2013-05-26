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

package com.fluidops.iwb.api.solution;

import static com.fluidops.util.PropertyUtils.loadProperties;

import java.io.File;
import java.util.Map.Entry;


public abstract class AbstractPropertyRegisteringHandler extends AbstractFailureHandlingHandler
{
    protected abstract void registerProperty(String key, String value) throws Exception;

    protected abstract String getPath();

    @Override
    protected boolean installIgnoreExceptions(File solutionDir) throws Exception
    {
        File propertyFile = new File(solutionDir, getPath());
        if (!propertyFile.exists()) return false;
        for (Entry<Object, Object> entry : loadProperties(propertyFile).entrySet())
        {
            String value = (String)entry.getValue();
            String key = (String) entry.getKey();
            registerProperty(key, value);
        }
        return true;
    }
}
