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
import java.util.List;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;

import com.fluidops.util.Pair;

/**
 * Cache for types of instances. Contains two components,
 * namely direct types and inferred types.
 * 
 * @author msc
 */
public class TypeCache extends RepositoryCache<Resource,Pair<List<Resource>,List<Resource>>>
{
    private static TypeCache instance = null;
    
    /**
     * Return the one and only instance
     * 
     * @return
     */
    static public TypeCache getInstance()
    {
        if (instance==null)
            instance = new TypeCache();
        return instance;
    }
    
    /**
     * Private Constructor (Singleton)
     */
    private TypeCache()
    {
        super();
    }
    
    
    public void insertDirectTypesForResource(Repository rep, Resource res, List<Resource> types)
    {
        Map<Resource,Pair<List<Resource>,List<Resource>>> repCache = cache.get(rep);
        if (repCache==null)
        {
            repCache = Collections.synchronizedMap(
                    new HashMap<Resource,Pair<List<Resource>,List<Resource>>>());
            cache.put(rep,repCache);
        }
        
        Pair<List<Resource>,List<Resource>> val = repCache.get(res);
        if (val==null)
            val = new Pair<List<Resource>,List<Resource>>(types,null);
        else
            val = new Pair<List<Resource>,List<Resource>>(types,val.snd);
        repCache.put(res,val);
    }
    
    public void insertIndirectTypesForResource(Repository rep, Resource res, List<Resource> types)
    {
        Map<Resource,Pair<List<Resource>,List<Resource>>> repCache = cache.get(rep);
        if (repCache==null)
        {
            repCache = Collections.synchronizedMap(
                    new HashMap<Resource,Pair<List<Resource>,List<Resource>>>());
            cache.put(rep,repCache);
        }
        
        Pair<List<Resource>,List<Resource>> val = repCache.get(res);
        if (val==null)
            val = new Pair<List<Resource>,List<Resource>>(null,types);
        else
            val = new Pair<List<Resource>,List<Resource>>(val.fst,types);
        repCache.put(res,val);
    }
    
    @Override
    public void updateCache(Repository rep, Resource res)
    {
        Map<Resource,Pair<List<Resource>,List<Resource>>> repCache = cache.get(rep);
        if (repCache!=null)
            repCache.remove(res);
    }
}
