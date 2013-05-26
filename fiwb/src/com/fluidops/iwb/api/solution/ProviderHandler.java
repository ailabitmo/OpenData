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

import java.io.File;

import com.fluidops.iwb.api.ProviderService;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.iwb.util.ObjectPersistance;

/**
 * add providers via the API
 * input is a file config/providers.xml in the extensions dir
 * 
 * providers are added via the API and are thus merged with the
 * original providers.xml
 * 
 * @author aeb
 */
public class ProviderHandler extends AbstractFailureHandlingHandler
{
	static final String PROVIDER_XML_REL_PATH = "config/providers.xml";
    private final ProviderService providerService;

    public ProviderHandler(ProviderService providerService)
    {
        this.providerService = providerService;
    }

    @Override boolean installIgnoreExceptions(File solutionDir) throws Exception
	{
	    File file = new File(solutionDir, PROVIDER_XML_REL_PATH);
	    if(!file.exists()) return false;
	    ObjectPersistance<AbstractFlexProvider<?>> persistance = 
	            new ObjectPersistance<AbstractFlexProvider<?>>(file.getPath());
	    for (AbstractFlexProvider<?> provider : persistance.load()) {
	        if(provider.deleted) continue;
	        providerService.addProvider(
	                provider.providerID, 
	                provider.getClass().getName(), 
	                provider.pollInterval == null ? null : provider.pollInterval.intValue()/60000, 
                    provider.config, 
                    provider.providerDataEditable);
	    }
	    return true;
	}
}
