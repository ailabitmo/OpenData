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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.util.DateTimeUtil;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.Pair;
import com.fluidops.util.Singleton;
import com.fluidops.util.persist.Properties;
import com.fluidops.util.persist.TransactionalFile;

/**
 * Wiki storage with revisions and meta data management.
 * 
 * @author Uli, msc
 */
public abstract class WikiStorage
{
    private static final Logger logger = Logger.getLogger(WikiStorage.class.getName());
    
    public static final long IWB_BOOTSTRAP_EPOCH;
    static {
        Calendar instance = Calendar.getInstance();
        instance.clear();
        instance.set(2010, 0, 1, 0, 0, 0);
        IWB_BOOTSTRAP_EPOCH = instance.getTimeInMillis();
    }
    
    private static UserManager userManager = EndpointImpl.api().getUserManager();
    
    private static Singleton<Properties> wikibotfile = new Singleton<Properties>() {
		@Override
		protected Properties createInstance() throws Exception {
			File file = IWBFileUtil.getFileInConfigFolder("wikibot.prop");
        	if(!file.exists())
        	{
        		try
        		{
        			file.createNewFile();
        		}
        		catch (IOException e)
        		{
        			logger.warn(e.getMessage(), e);
        		}
        	}
        	
        	return new Properties( new TransactionalFile( file.getAbsolutePath() ) );
		}    	
    };

	/**
	 * Meta data of a document revision.
	 * msc 
	 * @author Uli
	 */
	public static class WikiRevision implements Serializable
	{
		/**
		 * The prefix of any bootstrap version, the actual bootstrap id is added
		 * as SSS.
		 */
		public static final String BOOTSTRAP_DATE_PREFIX = "2010-01-01T00-00-00-";
				
		private static final long serialVersionUID = -1539420414289087587L;

		public Date date;
		public long size;
		public String comment;
		public String user;
		public String security;
		public String tag;

		public String toString()
		{
			return "[Revision Date=" + date + " Size=" + size + " Comment=" + comment + " User=" + user + " Security=" + security + " Tag=" + tag + "]";
		}
		
		public boolean delete(URI resource) 
		{
			return Wikimedia.getWikiStorage().deleteRevision(resource, this);
			
		}
		
		public boolean deleteAllOlder(URI resource) 
		{
			return Wikimedia.getWikiStorage().deleteAllOlder(resource, date);
		}
		
		/**
		 * Return the bootstrap version as "001" or null if it is not from bootstrap.
		 * @param r
		 * @return
		 */
		public String bootstrapVersion() {
			if(date.getTime() > IWB_BOOTSTRAP_EPOCH + 1000l * 60 * 60 * 24 * 10) return null;
			return String.format("%03d", date.getTime() - IWB_BOOTSTRAP_EPOCH);
		}
		
		public boolean isBootstrapRevision() {
		    return bootstrapVersion() != null;
		}
	}
	
	/**
	 * return the number of revisions for the given resource
	 * 
	 * @param resource
	 * @return
	 */
	public abstract int getRevisionCount(URI resource);
	
	/**
	 * deletes all wiki revisions for the entity resource
	 * 
	 * @param resource
	 * @return
	 */
	public abstract boolean delete(URI resource);
	
	/**
	 * deletes all revisions created before date
	 * 
	 * @param resource
	 * @param date
	 * @return
	 */
	public abstract boolean deleteAllOlder(URI resource, Date date);

	/**
	 * deletes a single wiki revision for the entity resource
	 * 
	 * @param resource
	 * @param rev
	 * @return
	 */
	public abstract boolean deleteRevision(URI resource, WikiRevision rev);

	/**
	 * Updates a single wiki revision: the revision is identified by the
	 * resource and date, and the remaining content is replaced. If the
	 * revision does not exist, a RuntimeException is thrown.
	 */
	public abstract boolean updateRevision(URI resource, WikiRevision rev);	
	
	/**
	 * Map the mediawiki API to its WikiBot instance
	 */
	protected Map<String, WikiBot> wikiBotMap = new ConcurrentHashMap<String, WikiBot>();
	
    /**
     * Returns the Wiki content as stored in the database.
     * 
     * @param name the uri of the resource for which to retrieve the revision
     * @param date if null, the latest revision is retrieved; otherwise, the revision at the given date
     * @return
     */
    public String getRawWikiContent(URI name, Date version)
    {
        WikiRevision wr = (version==null) ? getLatestRevision(name) : getRevision(name, version);
        
        if (wr == null)
        {
            //for now we ignore templates in the wiki bot, should be changed
            //they do work in principle, but transitive is expensive
            if (!wikibotfile.instance().isEmpty()
                    && !name.getLocalName().contains("redirect")
                    && !name.getLocalName().contains("Template")
                    && !name.stringValue().startsWith("Talk:"))
            {
            	
            	String wikimediaApi = wikibotfile.instance().getProperty(name.getNamespace());
            	if (wikimediaApi!=null)
            	{
        			WikiBot wikiBot = wikiBotMap.get(wikimediaApi);
        			if (wikiBot==null) {
        				wikiBot = new WikiBot(wikimediaApi);
        				wikiBotMap.put(wikimediaApi, wikiBot);
        			}
        			
                    try
    				{
                    	return wikiBot.loadAndStoreWikiPage(this, name);
    				}
                    catch (Exception e)
    				{
    					logger.error(e.getMessage(), e);
    					return null;
    				}
				}

            } // else (URI not in resolvable namespace, is redirect or template)
            return null;
        }
        else
        {
        	return getWikiContent(name, wr);
        }
        
    }  
 
    
    /**
	 * Stores Wiki content for the given name. Interface to the outside world.
	 * 
	 * @param name
	 * @param content
	 * @param comment
	 * @param date The date (null will be set to current date)
	 * @throws IOException
	 */
    public void storeWikiContent(URI name, String content, String comment, Date date) throws IOException
    {
        WikiRevision wr = new WikiRevision();
        wr.date = date==null?DateTimeUtil.getDateSafe():date;
        wr.user = userManager.getUser(null);
        wr.comment = comment;
        wr.security = "ALL/ALL";
        wr.size = content.length();
        
        // Prevent storing the same content twice
        Date latestVersion = wr.isBootstrapRevision()? previousBootstrapVersion(wr.date) : null;
        String latestContent = getRawWikiContent(name, latestVersion);
        if (latestContent != null && content.equals(latestContent))
        {
            logger.debug("Info: will not store content for " + name.stringValue() + ", latest revision is identical");
            return;
        }

        storeWikiContent(name, content, wr);
        
        // Update wiki page in keyword index
        KeywordIndexAPI.replaceWikiIndexEntry(name, content);
    }

    private Date previousBootstrapVersion(Date date)
    {
        return new Date(date.getTime() - 1);
    }
    
    public Date nextBootstrapVersion(Date date)
    {
        return new Date(date.getTime() + 1);
    }

    /**
     * Return the latest wiki revision revision
     * 
     * @param resource
     * @return
     */
    public abstract WikiRevision getLatestRevision(URI resource);
    
    /**
     * Return the wiki revision that has been active at the given
     * date. Note that the date must not match the date of a revision,
     * i.e. the function returns the version matching the date or, if
     * none is matching, the latest older version.
     * 
     * @param resource
     * @param d date (if null, null is returned)
     * @return null if no revision existed at that date, the revision otherwise
     */
    public WikiRevision getRevision(URI resource, Date d)
    {
    	// date must be given
    	if (d==null)
    		return null;
    	
    	// return the first revision before the current date
        List<WikiRevision> revs = getWikiRevisions(resource);
        if (revs != null)
        {
        	for (int i=revs.size()-1; i>=0; i--)
        		if (!revs.get(i).date.after(d))
        			return revs.get(i);
        }

        // revision not found
        return null;
    }

    /**
	 * @return mapping from wiki pages to their latest revisions
	 */
	public abstract List<Pair<URI,WikiRevision>> getLatestWikiRevisions();
	
	/**
     * Returns a list of all entities that have a wiki page
     * 
     * @return
     */
    public abstract List<URI> getAllWikiURIs();
	
	/**
	 * Returns the revisions for the given document ordered by date,
	 * i.e. the latest revision is the last element of the result list
	 * 
	 * @param resource
	 * @return
	 */
	public abstract List<WikiRevision> getWikiRevisions(URI resource);
	
	/**
     * Loads specified revision's content for the given resource.
     * 
     * @param resource
     * @param revision
     * @return
     */
    public abstract String getWikiContent(URI resource, WikiRevision revision);
	
	/**
     * Can be used to close pending connections on shutdown. May be overriden
     * by implementing subclasses, default implementation is empty.
     */
    public void shutdown()
    {
        // default: no DB connection
    }


    /**
     * Stores content for a given resource under the specified revision
     */
    protected abstract void storeWikiContent(URI resource, String content, WikiRevision revision) throws IOException;

    /**
	 * Unzip a gzipped string
	 * 
	 * @param bytes
	 * @return
	 */
	public static String gunzip(byte[] bytes)
	{
		InputStream in = null;
		String content = null;
		try
		{
			in = new GZIPInputStream(new ByteArrayInputStream(bytes));

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			content = new String(out.toByteArray());

			in.close();
			out.close();
		}
		catch (Exception e)
		{
			logger.warn( "Exception", e);
		}

		return content;
	}

	/** 
	 * Gzip a string into a byte array
	 * 
	 * @param content
	 * @return
	 */
	public static byte[] gzip(String content)
	{
		try
		{
			// logger.info("Bytes before: "+content.length());
			InputStream byteInputStream = null;
			byteInputStream = new ByteArrayInputStream(content.getBytes());

			ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
			OutputStream zippedOutputStream = new GZIPOutputStream(byteOutputStream);
			byteInputStream.close();

			byte[] buf = new byte[1024];
			int len;
			while ((len = byteInputStream.read(buf)) > 0)
			{
				zippedOutputStream.write(buf, 0, len);
			}

			zippedOutputStream.close();
			byte[] bytes = byteOutputStream.toByteArray();
			byteOutputStream.close();
			// logger.info("Bytes after: "+bytes.length);
			return bytes;
		}
		catch (IOException e)
		{
			logger.warn( "Exception", e);
		}

		return null;
	}
	
	public static void main( String[] args )
	{
		
	}
}
