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

package com.fluidops.iwb.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;

/**
 * Abstract superclass for implementing caches over the repository.
 * 
 * @author msc
 *
 */
public abstract class RepositoryCache<KEY,VALUE>
{    
    /**
     * The cache, mapping keys to values
     */
    protected Map<Repository,Map<KEY,VALUE>> cache;
 
    /**
     * Constructor
     */
    public RepositoryCache()
    {
        cache = new HashMap<Repository,Map<KEY,VALUE>>();
    }
    
    /**
     * Looks up the value associated to a key in the repository
     * 
     * @param rep
     * @param key
     * @return
     */
    public VALUE lookup(Repository rep, KEY key)
    {
        // get repository-specific cache
        Map<KEY,VALUE> repCache = cache.get(rep);
        return repCache==null?null:repCache.get(key);
    }
    
    public boolean containsKey(Repository rep, KEY key)
    {
        // get repository-specific cache
        Map<KEY,VALUE> repCache = cache.get(rep);
        return repCache!=null&&repCache.containsKey(key);
    }
    /**
     * Inserts a key-value pair into the repository
     * @param rep
     * @param key
     * @param val
     */
    public void insert(Repository rep, KEY key, VALUE val)
    {
        Map<KEY,VALUE> repCache = cache.get(rep);
        
        // initially create repository-dependent cache, if it does not exist
        if (repCache==null)
        {
            repCache = Collections.synchronizedMap(new HashMap<KEY,VALUE>());
            cache.put(rep,repCache);
        }
        
        repCache.put(key,val);
    }
    
    /**
     * Invalidates the cache globally, for all repositories
     */
    public void invalidate()
    {
        cache.clear();
    }
    
    
    /**
     * Invalidates the cache for the respective repository only
     */
    public void invalidate(Repository rep)
    {
        Map<KEY,VALUE> repCache = cache.get(rep);
        if (repCache!=null)
            repCache.clear();
    }
    
    
    /**
     * Updates the cache for the given Repository and URI.
     * The default implementation performs a complete cache
     * invalidation, for the repository, so it is highly
     * recommended to re-implement this method for efficiency
     * reason wherever possible.
     */
    public void updateCache(Repository rep, Resource u)
    {
        invalidate(rep);
    }
    
    
    /**
     * Return the size of the cache for the provided repository, i.e
     * the number of KEY elements present in the cache. If rep is null,
     * the size is returned for all repositories present in the cache.
     * 
     * @param rep
     * 			the repository, null is allowed
     * @return
     * 			the size of the cache (or 0 if there is no cache for the given rep)
     */
    public int size(Repository rep) {	
		if (rep!=null) {
			Map<KEY, VALUE> repCache = cache.get(rep);
			if (repCache==null)
				return 0;
			return repCache.size();
		}
		
		int size=0;
		for (Repository r : cache.keySet())
			size+=cache.get(r).size();
		return size;
	}
				
	
}
