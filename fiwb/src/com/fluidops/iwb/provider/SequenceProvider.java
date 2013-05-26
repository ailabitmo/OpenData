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

package com.fluidops.iwb.provider;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ProviderService;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.util.StringUtil;

/**
 * Meta provider capable of triggering existing providers in sequence.
 * The providers are referenced by their ID. Referred provider IDs that
 * cannot be resolved (i.e., do not exist) are skipped while running
 * the {@link SequenceProvider}. Note that the {@link SequenceProvider}
 * itself does not extract any data, but the extracted data resides in the
 * context of the respective providers.
 */
@TypeConfigDoc("The Sequence Provider is a meta provider that allows to trigger existing providers in sequence.")
public class SequenceProvider extends AbstractFlexProvider<SequenceProvider.Config>
{
	private static final Logger logger = Logger.getLogger(SequenceProvider.class.getName());
	
	private static final long serialVersionUID = 2777688652542206175L;

	public static class Config implements Serializable
	{
		private static final long serialVersionUID = -5764311060861106965L;
		
		@ParameterConfigDoc(
				desc = "Ordered list of provider identifiers referring to the providers to be triggered in sequence", 
				required = true,
				type=Type.LIST,
    			listType=URI.class)
        public List<URI> providers;
	}

	@Override
	public void gather(List<Statement> res) throws Exception
	{
		for (URI providerUri : config.providers)
		{
			if (providerUri==null)
				break; // ignore
			
			boolean run = false;
		    try
	        {
	            ProviderService ps = EndpointImpl.api().getProviderService();
	            List<AbstractFlexProvider> providers = ps.getProviders();
	            for (int i=0; i<providers.size() && !run; i++)
	            {
	                AbstractFlexProvider provider = providers.get(i);
	                if (!provider.deleted && provider.providerID.equals(providerUri))
	                {
	                    try
	                    {
	                        EndpointImpl.api().getProviderService().runProvider(provider.getProviderID(), null);
	                        run = true; 
	                    }
	                    catch (Exception e)
	                    {
	                        throw new RuntimeException(e);
	                    }
	                }
	            }
	            
	            if (!run)
	            	logger.warn("Provider " + providerUri.toString() + " does not exist. Skipped execution.");
	        }
	        catch (Exception e)
	        {
	            logger.warn(e.getMessage(),e);
	        }
		}
	}

	@Override
	public Class<? extends Config> getConfigClass() 
	{
		return Config.class;
	}
}
