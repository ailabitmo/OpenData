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
import java.util.Map;
import java.util.Properties;

import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.NamespaceService;

public class NamespaceHandler extends AbstractFailureHandlingHandler
{
    public static final String NAMESPACE_PROP_REL_PATH = "config/namespaces.prop";
    private final ValueFactoryImpl valueFactory = ValueFactoryImpl.getInstance();
    private final NamespaceService namespaceService;
    
    public NamespaceHandler(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }
    
    @Override boolean installIgnoreExceptions(File solutionDir)
    {
        Properties namespacesToRegister = loadProperties(new File(solutionDir, NAMESPACE_PROP_REL_PATH));
        if(namespacesToRegister.isEmpty()) return false;
        for (Map.Entry<Object, Object> namespaceMapping : namespacesToRegister.entrySet())
        {
            namespaceService.registerNamespace(valueFactory.createURI((String)namespaceMapping.getValue()), 
                    (String)namespaceMapping.getKey());
        }
        return true;
    }
}
