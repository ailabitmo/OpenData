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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fluidops.iwb.util.Config;
import com.fluidops.util.Singleton;

import de.umass.lastfm.Artist;


/**
 * Cache for LastFM Data. 
 * 
 * Note: config parameter lastFMKey is required in config.prop
 * 
 * - (Singleton) Main memory cache, which is not persisted at the moment
 * - Retrieves and maintains {@link LastFMData} instances in a hashmap (key=musicbrainz id)
 * - Lookup by mbid, if not available in cache => remote API access
 * - Currently access to artist information, can be easily extended
 * 
 * LastFM Access requires the (slightly modified) code from the lastFM Java API, if additional
 * changes are required use the source code contained in the JAR.
 * 
 * @author as
 *
 */
public class LastFMCache  {
	
	protected static final Logger log = Logger.getLogger(LastFMCache.class);
	
	// the last FM API key, to be specified in the config.prop setting lastFMKey
	protected String apiKey = null;
	
	/*
	 * TODO
	 *  - update/invalidation/crawling
	 *  - persistence
	 *  - save all information in cache? or just most accessed ones
	 */
	

	
	/**
	 * Data structure for last FM data, specific implementations contain the data
	 * 
	 * @author as
	 */
	public static abstract class LastFMData {

		/*
		 * TODO more instance, e.g. for tracks
		 *  - update functionality
		 */
			
		protected int accessCount = 0;
		protected final String mbid;
		
		
		public LastFMData(String mbid) {
			this.mbid = mbid;
		}
		
		public int getAccessCount() {
			return accessCount;
		}
		
		/**
		 * Update this piece of lastFM data
		 */
		public abstract void update();
		
		public void increaseAccessCount() {
			accessCount++;
		}
	}
	
	/**
	 * LastFM data for an Artist (artist information + similar artists)
	 * 
	 * @author as
	 *
	 */
	public static class ArtistLastFMData extends LastFMData {
		
		protected Artist artist = null;
		protected Collection<Artist> similar = null;
		
		public ArtistLastFMData(String mbid, Artist artist, Collection<Artist> similar) {
			super(mbid);
			this.artist = artist;
			this.similar = similar;
		}

		@Override
		public void update() {
			// TODO
		}
		
		public Artist getArtist() {
			return artist;
		}
		
		public Collection<Artist> getSimilar() {
			return similar==null ? artist.getSimilar() : similar;
		}
	}
	
	
	
	/**
	 * map the musicbrainz id (mbid) to the corresponding LastFMData (subclass)
	 */
	protected Map<String, LastFMData> cache = new HashMap<String, LastFMData>();
	
	private static Singleton<LastFMCache> instance = new Singleton<LastFMCache>() {

		@Override
		protected LastFMCache createInstance() throws Exception {
			return new LastFMCache();
		}
		
	};
	 	    
    /**
     * Return the one and only instance 
     * @return
     */
    public static LastFMCache getInstance()   {
        return instance.instance();
    }
	
        
    public LastFMCache() {
    	apiKey = Config.getConfig().getLastFMKey();
    	if (apiKey==null)
    		throw new RuntimeException("For LastFM access config.prop setting lastFMKey is required.");
    }
    
    /**
     * Retrieve artist information from the cache. If the data is not present in 
     * the cache, a remote lookup (=2 remote requests) is performed and the cache
     * is updated. The accessCount of the {@link LastFMData} instance is increased.
     * 
     * @param mbid
     * 			the music brainz id of the artist
     * 
     * @return
     * 			the artist data or null (if mbid does not correspond to an artist)
     * 
     * @throws IllegalArgumentException
     * 				if the provided mbid does not correspond to cached {@link ArtistLastFMData}
     */
    public ArtistLastFMData getArtist(String mbid) {
    	synchronized (cache) {
	    	LastFMData res = cache.get(mbid);
	    	if (res==null) {
	    		res = retrieveArtist(mbid);
	    		if (res==null)
	    			return null;
	    		cache.put(mbid, res);
	    	}
	    	res.increaseAccessCount();
	    	if (res instanceof ArtistLastFMData)
	    		return (ArtistLastFMData)res;
	    	throw new IllegalArgumentException("Data of provided mbid (" + mbid + ") is not an artist.");
    	}
    }
    
    
    /**
     * Retrieve artist information for mbid from LastFM database => two remote requests
     * 
     * @param mbid
     * @return
     * 		an {@link ArtistLastFMData} or null
     */
    protected ArtistLastFMData retrieveArtist(String mbid) {
    	try {
	    	Artist artist = Artist.getInfo(mbid, apiKey);
	    	if (artist==null)
	    		return null;
	    	Collection<Artist> similar = Artist.getSimilar(mbid, 5, apiKey);
	    	return new ArtistLastFMData(mbid, artist, similar); 
    	} catch (Exception e) {
    		// e.g. NumberFormat Exception may occur, which we should ignore
    		log.warn("Exception encountered for mbid " + mbid + " (" + e.getClass().getSimpleName() + "): " + e.getMessage());
    		return null;
    	}
    }
    
}


