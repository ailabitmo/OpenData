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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * Class for mapping IDs to URIs as specified in propertymapping.prop.
 * TODO: test case
 * 
 * @author michaelschmidt
 */
public class ProviderURIResolver implements Serializable
{
	private static final long serialVersionUID = 5824311766415264468L;

	private static final Logger logger = Logger.getLogger(ProviderURIResolver.class.getName());
	
	/**
	 * Mappings that depend on the type. This map is given first priority.
	 */
	Map<URI,Map<String,URI>> typeDependentMapping;
	
	/**
	 * Mappings that do not depend on the type. This map is given second priority.
	 */
	Map<String,URI> typeIndependentMapping;
	
	private Set<String> unresolvedProperties;
	
	private Set<Pair<URI,URI>> resolvedProperties;
	
	public ProviderURIResolver(String propertyFile)
	{
		typeDependentMapping = new HashMap<URI,Map<String,URI>>();
		typeIndependentMapping = new HashMap<String,URI>();
		unresolvedProperties = new HashSet<String>();
		resolvedProperties = new HashSet<Pair<URI,URI>>();
		
		try
		{
			FileInputStream fis = new FileInputStream(IWBFileUtil.getFileInWorkingDir(propertyFile));
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(dis));
	
			String mappingLine;
			while ((mappingLine = br.readLine()) != null)
			{
				addMapping(mappingLine);
			}
			br.close();
		}
		catch (Exception e)
		{
			logger.info("Resolver file '" + propertyFile + "' could not be loaded: " + e.getMessage());
			logger.info("Using dummy resolver instead.");
		}
	}
		
	private void addMapping(String mappingLine) 
	{
		if (StringUtil.isNullOrEmpty(mappingLine))
			return;
		if (mappingLine.startsWith("%"))
			return; // comment
		
		String[] mappingLineSpl = mappingLine.split(";",-1);
		if (mappingLineSpl.length==3)
		{
			String propName = mappingLineSpl[0];
			String type = mappingLineSpl[1];
			String uri = mappingLineSpl[2];

			// error handling:
			if (StringUtil.isNullOrEmpty(propName))
			{
				logger.warn("Property name in line is empty (invalid line): " + mappingLine);
				return;
			}
			if (StringUtil.isNullOrEmpty(uri))
			{
				logger.warn("URI in line is empty (invalid line): " + uri);
				return;				
			}
			
			if (type.isEmpty())
				typeIndependentMapping.put(propName, EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(uri));
			else
			{
				URI typeUri = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(type);
				Map<String,URI> innerMap = typeDependentMapping.get(typeUri);
				if (innerMap==null)
				{
					innerMap = new HashMap<String,URI>();
					typeDependentMapping.put(typeUri, innerMap);
				}
				innerMap.put(propName, EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(uri));
			}
		}
	}
	
	/**
	 * Resolve a property string for a given type to a URI.
	 * 
	 * @param propName
	 * @param type
	 * @return the URI (not null)
	 */
	public URI resolveProperty(String propName, URI type, URI propertyType)
	{
		if (propName==null)
			throw new RuntimeException("Illegal Call");
		
		URI property = null;
		if (type==null)
		{
			property = typeIndependentMapping.get(propName);
			if (property==null)
			{
				unresolvedProperties.add(propName); // record
				property = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(propName);
			}
		}
		else
		{
			Map<String,URI> typeMap = typeDependentMapping.get(type);
			if (typeMap==null || typeMap.get(propName)==null)
				return resolveProperty(propName,null,propertyType); // try to lookup in typeIndependentMapping
			else
			{
				property = typeMap.get(propName); // defined
			}
		}
		
		resolvedProperties.add(new Pair<URI,URI>(property,propertyType));
		return property;
	}
	
	/**
	 * Return resolved properties, including their type (where specified)
	 * @return
	 */
	public Set<Pair<URI,URI>> resolvedProperties()
	{
		return resolvedProperties;
	}
	
	/**
	 * Return stats about properties that were not explicitly mapped
	 * @return
	 */
	public String dbgStats()
	{
		StringBuilder sb = new StringBuilder("Unresolved Properties:\n");
		for (String up : unresolvedProperties)
		{
			sb.append("- ").append(up).append("\n");
		}
		return sb.toString();
	}
}