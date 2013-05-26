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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fluidops.util.XML;

/**
 * Handles serialization and deserialization of IWB providers
 */
public class ProviderPersistance
{
	public static void saveProviders(Collection<AbstractFlexProvider> providers) throws Exception
	{
		// TODO Use / adapt ProviderServiceImpl.PROVIDERS_PROP_PATH instead of using hardcoded filename / path
    	OutputStream fo = new FileOutputStream("providers.xml");
    	OutputStreamWriter wo = new OutputStreamWriter( fo, "UTF-8" );
    	
		//For each provider, the fields that need to be serialized are:
		//	- all fields in AbstractFlexProvider
		//	- all RdfInput-annotated fields of the provider's config (basically AbstractFlexProvider.config)
    	
    	// TODO Currently serializing everything => should only care about the fields mentioned above
    	// TODO Easy, fast, clean solution: make all fields of all subclasses of AbstractFlexProvider transient => this way, only the relevant stuff is serialized
		XML.writeObject( providers, wo );
    	wo.close();
	}
	
	public static Collection<AbstractFlexProvider> loadProviders() throws Exception
	{
		List<AbstractFlexProvider> result = new ArrayList<AbstractFlexProvider>();
		
		// TODO Use / adapt ProviderServiceImpl.PROVIDERS_PROP_PATH instead of using hardcoded filename / path
		InputStream in = new FileInputStream("providers.xml");
		InputStreamReader ir = new InputStreamReader( in, "UTF-8" );
		
		Object o = XML.readObject(ir);
		if (o != null)
			result = (List<AbstractFlexProvider>) o;
		ir.close();
		
		return result;
	}
}