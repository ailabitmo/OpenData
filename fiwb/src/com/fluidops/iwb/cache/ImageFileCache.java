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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Singleton cache class that resolves URIs and literals to
 * user-defined pictures from different config files. 
 * 
 * @author msc
 */
public class ImageFileCache extends RepositoryCache<String, String>
{
    private static final Logger logger = Logger.getLogger(ImageFileCache.class.getName());

    /**
     * Mapping filename -> (ID -> Image)
     */
    private Map<String,Map<String,String>> cache;
    
    /**
     * The one and only instance
     */
    private static ImageFileCache instance = null;
    
    /**
     * Constructor
     */
    private ImageFileCache()
    {
        cache = new HashMap<String,Map<String,String>>();
    }
    
    /**
     * Access to singleton instance
     */
    public static ImageFileCache getInstance()
    {
        if (instance==null)
            instance = new ImageFileCache();
        return instance;
    }
    
    /**
     * (Re)-initialization of data structure, clears the cache.
     */
    public void invalidate()
    {
        cache.clear();
        cache = new HashMap<String,Map<String,String>>();
    }
    
    /**
     * Return image mapping stored in one file using cache
     * if file has already been loaded.
     * 
     * @param file
     * @return The defined mappings
     */
    public Map<String,String> getImageMappings(String file)
    {
        Map<String,String> mapping = cache.get(file);
        
        // if not defined load
        if (mapping==null)
        {
            mapping = new HashMap<String,String>();
            BufferedReader br = null;
            try
            {
                FileInputStream fstream = new FileInputStream(file);
                DataInputStream in = new DataInputStream(fstream);
                br = new BufferedReader(new InputStreamReader(in));
                
                String line;
                while ((line = br.readLine()) != null)
                {
                    String[] spl = line.split("=");
                    if (spl.length==2 && spl[0]!=null && spl[1]!=null)
                        mapping.put(spl[0].trim(),spl[1].trim());
                }
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage());
            } finally {
            	IOUtils.closeQuietly(br);
            }
            
            // store in cache
            cache.put(file,mapping);
        }
        
        return mapping;
    }
}
