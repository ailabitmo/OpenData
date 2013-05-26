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

package com.fluidops.iwb.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;

import com.fluidops.iwb.cache.AutoSuggestionCache;
import com.fluidops.iwb.cache.ContextCache;
import com.fluidops.iwb.cache.ImageFileCache;
import com.fluidops.iwb.cache.InstanceCache;
import com.fluidops.iwb.cache.InversePropertyCache;
import com.fluidops.iwb.cache.LabelCache;
import com.fluidops.iwb.cache.PropertyCache;
import com.fluidops.iwb.cache.RepositoryCache;
import com.fluidops.iwb.cache.TypeCache;
import com.fluidops.iwb.cache.URIAutoSuggestionCache;

/**
 * Singletong global cache management class.
 * 
 * @author tma, msc
 */
public class CacheManager 
{
	
	/**
	 * Cache listener can be registered to listen and react to
	 * certain cache update events.
	 */
	public static interface CacheEventListener {
		public void onUpdate(Repository rep, Resource res);
		public void onInvalidate(Repository rep);
	}
	
	/**
	 * Dummy implementation doing nothing on cache events
	 */
	public static class VoidCacheEventListener implements CacheEventListener {
		@Override
		public void onUpdate(Repository rep, Resource res) { }

		@Override
		public void onInvalidate(Repository rep) { }		
	}
	
    private static final Logger logger = Logger.getLogger(CacheManager.class.getName());

    // list of all property caches
    List<RepositoryCache<?,?>> caches;

    
    /**
     * A global variable to remember when the last update to (any) 
     * repository has occurred
     */
    private long lastupdate = System.currentTimeMillis();

    
    // the one and only instance
    private static CacheManager instance = null;
    
    /**
     * Singleton constructur
     */
    private CacheManager()
    {
        caches = new ArrayList<RepositoryCache<?,?>>();
        caches.add(ContextCache.getInstance());
        caches.add(ImageFileCache.getInstance());
        caches.add(InstanceCache.getInstance());
        caches.add(InversePropertyCache.getInstance());
        caches.add(LabelCache.getInstance());
        caches.add(PropertyCache.getInstance());
        caches.add(TypeCache.getInstance());   
        caches.add(AutoSuggestionCache.getInstance());  
        caches.add(URIAutoSuggestionCache.getInstance());  
    }
    
    private CacheEventListener cacheListener = new VoidCacheEventListener();
    
    
    /**
     * Returns the one and only CacheManager instance.
     * 
     * @return
     */
    public static CacheManager getInstance()
    {
        if (instance==null)
            instance = new CacheManager();
        return instance;
    }
    
    /**
     * Register a custom cache listener and overwrite the 
     * currently set one. The {@link CacheEventListener} methods
     * are invoked on invalidation and update
     * 
     * @param cacheListener
     */
    public void setListener(CacheEventListener cacheListener) {
    	this.cacheListener = cacheListener;
    }
    
    /**
     * Invalidate all caches for all repositories.
     * 
     * @return
     */
    public void invalidateAllCaches()
    {
        // first, we invalidate the repository caches
        for (RepositoryCache<?,?> c : getAllRepositoryCaches()) 
        {
            try 
            {
                c.invalidate();
            } 
            catch (Exception e) 
            {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } 
        }
        
        // store date of last cache change
        lastupdate = System.currentTimeMillis();
        
        cacheListener.onInvalidate(null);
    }

    
    /**
     * Invalidate all caches for a given repository
     * 
     * @parsm rep the repository
     * @return
     */
	public void invalidateAllCaches(Repository rep)	{
		invalidateAllCachesInternal(rep);        
        cacheListener.onInvalidate(rep);
	}
	
	/**
	 * Invalidate all caches, no callback to the registered
	 * listener.
	 * 
	 * @param rep
	 */
	public void invalidateAllCachesWithoutInformingListeners(Repository rep) {
        invalidateAllCachesInternal(rep);
	}
	
	private void invalidateAllCachesInternal(Repository rep) {
		logger.trace("Invalidating ALL caches now:");
        for (RepositoryCache<?,?> cache : getAllRepositoryCaches()) 
        {
            Long before = System.currentTimeMillis();
            cache.invalidate(rep);
            Long after = System.currentTimeMillis();
            logger.trace("Cleared " + cache.getClass().getSimpleName() + " in " + (after-before) + "ms");
        }

        // store date of last cache change
        lastupdate = System.currentTimeMillis();
	}
	
    /**
     * Updates all caches for a given repository,
     * signalizing that outgoing statements for a URI
     * have changed.
     * 
     * @parsm rep the repository
     * @param u the URI that has changed
     * @return
     */
    public void updateAllCaches(Repository rep, Resource res)
    {
        logger.trace("Invalidating ALL caches now:");
        for (RepositoryCache<?,?> cache : getAllRepositoryCaches()) 
        {
            Long before = System.currentTimeMillis();
            cache.updateCache(rep,res);
            Long after = System.currentTimeMillis();
            logger.trace("Updated " + cache.getClass().getSimpleName()
                    + " for resource " + res + " in " + (after - before) + "ms");
        }

        // store date of last cache change
        lastupdate = System.currentTimeMillis();
        
        cacheListener.onUpdate(rep, res);
    }

	
    /**
     * Returns all repository cache classes.
     * 
     * @return
     */
	public List<RepositoryCache<?,?>> getAllRepositoryCaches()
	{       
	    return caches;
	}
	
	/**
	 * Return the repository cache instance matching cacheName. For
	 * matching the simple name of the implementing class is used,
	 * e.g. LabelCache for com.fluidops.iwb.cache.LabelCache.
	 * 
	 * @param cacheName
	 * @return
	 * @throws IllegalArgumentException if there is no cache registered for cacheName
	 */
	public RepositoryCache<?,?> getRepositoryCache(String cacheName) 
	{
		for (RepositoryCache<?,?> cache : caches)
			if (cache.getClass().getSimpleName().equals(cacheName))
				return cache;
		throw new IllegalArgumentException("No cache registered for " + cacheName);
	}
	

	/**
	 * Returns the date (as timestamp) of the last cache update
	 * 
	 * @return
	 */
    public static long getLastupdate()
    {
        return getInstance().lastupdate;
    }
}
