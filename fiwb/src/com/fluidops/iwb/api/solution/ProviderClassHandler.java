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



import com.fluidops.iwb.api.ProviderService;

/**
 * add providers classes via the API
 * input is a file config/providers.prop in the extensions dir
 * 
 * providers are added via the API and are thus merged with the
 * original providers.prop
 * 
 * @author aeb
 */
public class ProviderClassHandler extends AbstractPropertyRegisteringHandler
{
    private final ProviderService providerService;

    public ProviderClassHandler(ProviderService providerService)
    {
        this.providerService = providerService;
    }
    
    @Override
    protected String getPath()
	{
		return "config/providers.prop";
	}
    
    @Override
    protected void registerProperty(String key, String value) throws Exception
    {
        if(Boolean.valueOf(value)) providerService.registerProviderClass(key);
    }
}
