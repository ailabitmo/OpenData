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

package com.fluidops.iwb.wiki;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.URI;


public class WikiDirectoryCache
{
    /**
     * Mapping from URIs to directory names
     */
    private Map<URI,File> cache;
    
    private static WikiDirectoryCache instance = null;
    
    /**
     * Gets the one and only instance of the wiki
     * directory cache.
     * 
     * @return
     */
    public static WikiDirectoryCache getInstance()
    {
        if (instance==null)
            instance = new WikiDirectoryCache();
        return instance;
    }
    
    /**
     * Returns the associated directory, if it exists, null otherwise.
     * 
     * @param uri
     * @return
     */
    public File lookup(URI resource)
    {
        return cache.get(resource);
    }
    
    /**
     * Inserts the given key-value pair into the cache
     */
    public void insert(URI resource, File file)
    {
        cache.put(resource,file);
    }
    
    /**
     * Clears the cache.
     */
    public void clearCache()
    {
        cache = new HashMap<URI,File>();
    }
    
    /**
     * Remove from cache
     */
    public void removeFromCache(URI resource)
    {
        cache.remove(resource);
    }
    
    /**
     * Singleton constructor
     */
    private WikiDirectoryCache()
    {
        cache = new HashMap<URI,File>();
    }
    
    
}
