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

package com.fluidops.iwb.service;

import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.api.CacheManager;


/**
 * Service facility for cache invalidation: this service invalidates the 
 * cache corresponding to {@link Config#cacheName}. Supported cache names
 * are specified in {@link CacheType}.
 * 
 * This service provides public methods that can be used from within a 
 * CodeExecutionWidget.
 * 
 * Example:
 * <code> 
 * {{#widget: com.fluidops.iwb.widget.CodeExecutionWidget 
 * | label = 'Invalidate Label Cache'
 * | clazz = 'com.fluidops.iwb.service.CacheInvalidationService'
 * | method = 'invalidateCache'
 * | args = {{ 'LabelCache' }}
 * }}
 * </code>
 * @author as
 *
 */
public class CacheInvalidationService implements Service<CacheInvalidationService.Config>
{

	public static enum CacheType {
		ContextCache,
		ImageFileCache,
		InstanceCache, 
		InversePropertyCache,
		LabelCache,
		PropertyCache,
		TypeCache,
		AutoSuggestionCache,
		URIAutoSuggestionCache,
		All;		
	}
	
	public static class Config {
		
		/**
		 * The name of the cache to clear, one of CacheType#values()
		 */
		public String cacheName;
	}
	
	@Override
	public Object run(Config in) throws Exception
	{
		if (in==null || in.cacheName==null)
			throw new IllegalArgumentException("cacheName must not be null");
		
		CacheType cacheType;
		try
		{
			cacheType = CacheType.valueOf(in.cacheName);
		} 
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(
					"Unsupported value for cacheName: " + in.cacheName
							+ ", please refer to the documentation");
		}
		
		CacheManager cm = CacheManager.getInstance();
		
		switch (cacheType) {
		case All:						cm.invalidateAllCaches(); break;
		case ContextCache:
		case ImageFileCache:
		case InstanceCache:
		case InversePropertyCache:
		case LabelCache:
		case PropertyCache:
		case TypeCache:
		case AutoSuggestionCache:
		case URIAutoSuggestionCache:
										cm.getRepositoryCache(cacheType.name()).invalidate();
										break;
		}		
		
		return null;
	}

	@Override
	public Class<Config> getConfigClass()
	{
		return Config.class;
	}
	
	@CallableFromWidget
	public static void invalidateCache(String cacheName) throws Exception {
		Config cacheCfg = new Config();
		cacheCfg.cacheName = cacheName;
		new CacheInvalidationService().run(cacheCfg);
	}
}
