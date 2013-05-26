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

import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;

public class InversePropertyCache extends RepositoryCache<URI,URI>
{
    /**
     * Cache for inverse properties, to avoid repeated database access when
     * computing the inverse properties.
     * 
     * @author msc
     */

    private static InversePropertyCache instance = null;

    /**
     * Return the one and only instance
     * 
     * @return
     */
    static public InversePropertyCache getInstance()
    {
        if (instance == null)
            instance = new InversePropertyCache();
        return instance;
    }

    /**
     * Private Constructor (Singleton)
     */
    private InversePropertyCache()
    {
        super();
    }
    
    @Override
    public void updateCache(Repository rep, Resource u)
    {
        Map<URI,URI> repCache = cache.get(rep);
        if (repCache!=null)
            repCache.remove(u);
    }
}
