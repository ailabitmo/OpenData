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

import static java.lang.String.format;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.GenUtil;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * File-based wiki storage implementation.
 * 
 * @author Uli, msc
 */
public class WikiFileStorage extends WikiStorage
{
    private static final Logger logger = Logger.getLogger(WikiFileStorage.class.getName());
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH-mm-ss-SSS";
    
    /**
     * The root folder to use for Wiki content
     */
    private File wikiRoot;

    /**
     * Creates a new storage with default FS root.
     */
    public WikiFileStorage()
    {
        this(IWBFileUtil.getWikiFolder());
    }
    
    /**
     * Create a new storage with the given FS root.
     * 
     * @param wikiRoot
     */
    public WikiFileStorage(File wikiRoot)
    {
        this.wikiRoot = wikiRoot;
    }

    @Override
    protected void storeWikiContent(URI resource, String content, WikiRevision revision) throws IOException
    {
    	if (content==null) 
    		throw new IllegalArgumentException("Cannot store 'null' content for resource " + resource.stringValue());
    		
        String time = toFileName(revision.date);
    
        File dir = getContainingDir(resource);
        if (!dir.exists())
            GenUtil.mkdirs(dir);
    
        File f = new File(dir, time + ".wiki");
    
        if (f.exists())
            logger.warn("Warning: overwriting revision " + time + " of "
                    + resource + " [" + f + "]");
        else
            logger.info("Storing revision " + time + " of " + resource + " [" + f
                    + "]");
    
        Writer fw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try
        {
            fw.write(content);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
            fw.close();
        }
    
        File fmeta = new File(dir, time + ".meta");
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                fmeta));
        try
        {
            oos.writeObject(revision);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
            oos.close();
        }
    }

    @Override
    public List<Pair<URI,WikiRevision>> getLatestWikiRevisions()
    {
        List<Pair<URI, WikiRevision>> res = new ArrayList<Pair<URI, WikiRevision>>();

        File[] files = wikiRoot.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return new File(dir, name).isDirectory();
            }
        });

        // Nothing found? Return.
        if (files == null)
            return res;
        List<Pair<URI, WikiRevision>> revisions = new ArrayList<Pair<URI, WikiRevision>>();

        for (File file : files)
        {
            String f = "";
            try
            {
                f = URLDecoder.decode(file.getName(), "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                logger.warn("Exception", e);
            }
            URI uri = ValueFactoryImpl.getInstance().createURI(f);
            WikiRevision revision = getLatestRevision(uri);
            if (revision != null)
                revisions.add(new Pair<URI, WikiRevision>(uri, revision));
        }

        Collections.sort(revisions, new Comparator<Pair<URI, WikiRevision>>()
        {
            @Override
            public int compare(Pair<URI, WikiRevision> o1,
                    Pair<URI, WikiRevision> o2)
            {
                return o2.snd.date.compareTo(o1.snd.date);
            }
        });

        // now only return the top 10 revisions
        int counter = 0;
        for (Pair<URI, WikiRevision> p : revisions)
        {
            if (counter++ > 10)
                break;
            res.add(p);
        }
        return res;
    }

    @Override
    public List<URI> getAllWikiURIs()
    {
        
        Set<String> wikipages = new HashSet<String>();
        List<URI> uris = new LinkedList<URI>();

        logger.debug("Reading all files from " + wikiRoot);
        for (File f : wikiRoot.listFiles())
        {
        	if (!f.isDirectory()) 
        	{
        	    logger.trace(format("Skipping %s as it is not a directory", f));
        	    continue;
        	}
        	
        	if (f.listFiles().length==0) 
        		continue;
        	
            try
            {
            	// first we check whether the directory contains some revisions itself
            	if (f.listFiles(wikiRevisionFilter).length>0)
            	{
	                String wikiPage = URLDecoder.decode(f.getName(), "UTF-8");
	                logger.trace(format("Adding '%s' to set of all wiki uris", wikiPage));
	                wikipages.add(wikiPage);
            	}
            	
                // next, we scan the signature-subfolders to find wiki pages with
            	// different capitalization
                File[] subFolders = f.listFiles(new FileFilter() 
                {
                	@Override
                	public boolean accept(File f)
                	{
                		return f.isDirectory();
                	}
                });
                for (File subFolder : subFolders)
                {
                	if (subFolder.listFiles(wikiRevisionFilter).length==0)
                		continue; // skip (there's no revision in here)

                	String alternativeWikiPage = applySignature(f.getName(),subFolder.getName());
                	if (alternativeWikiPage!=null)
                	{
                    	String decodedSubWikiPage = URLDecoder.decode(alternativeWikiPage, "UTF-8");
    	                logger.trace(format("Adding '%s' to set of all wiki uris", decodedSubWikiPage));
                		wikipages.add(decodedSubWikiPage);
                	}
                }
            }
            catch (UnsupportedEncodingException e)
            {
                logger.warn("Exception", e);
            }

            // uris.add(ValueFactoryImpl.getInstance().createURI(f));
        }

        // build URIs for wiki pages
        if (!wikipages.isEmpty()) 
        {
            //Index all wikipages without any RDF links, i.e. not present as entity in the RDF store
            String ns = EndpointImpl.api().getNamespaceService().defaultNamespace();
            for (String s : wikipages) {
                ValueFactoryImpl f = new ValueFactoryImpl();
                URI uri = null;
                try 
                {
                     uri = f.createURI(s);
                }
                catch (IllegalArgumentException ex) 
                {
                    // if no URI, add defaultNS
                    uri = f.createURI(ns + s);
                }
                uris.add(uri);
            }
        }
        return uris;
    }


	@Override
	public int getRevisionCount(URI resource) {
		return getWikiPageRevisionFiles(resource).length;
	}
	
	
    @Override
    public List<WikiRevision> getWikiRevisions(URI resource)
    {
        List<WikiRevision> revs = new ArrayList<WikiRevision>();
        for (File f : getWikiPageRevisionFiles(resource))
        {
			try
			{
				revs.add(getWikiRevision(f));
			}
			catch (Exception ex)
			{
				logger.warn("ERROR: problems while loading meta info " + f, ex);
			}
        }
        return revs;
    }
        
    @Override
	public WikiRevision getLatestRevision(URI resource)	{
		
    	File[] wf = getWikiPageRevisionFiles(resource);
    	if (wf.length==0)
    		return null;
    	try
		{
			return getWikiRevision(wf[wf.length-1]);
		}
		catch (Exception e)
		{
			logger.warn("ERROR: problems while loading latest revision for " + resource.stringValue(), e);
			return null;
		}
	}

	/**
     * Retrieve the wiki revision for the given file.
     * 
     * @param f
     * @return
     * @throws Exception
     */
    private WikiRevision getWikiRevision(File f) throws Exception {
    	
    	ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
    	
        try
        {
            WikiRevision r = (WikiRevision) ois.readObject();
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            String fName = f.getName();
            String prefix = fName.substring(0, fName.lastIndexOf(".meta"));
            r.date = sdf.parse(prefix);
            return r;
        }
        finally
        {
            ois.close();
        }
    }
    
    /**
     * Returns a list of wiki page revisions available in
     * wikiPageDir. The result is sorted. If no wiki pages
     * are available, an empty array is returned.
     * @param wikiPageDir
     * @return
     */
    private File[] getWikiPageRevisionFiles(URI resource) {
    	
    	File wikiPageDir = getContainingDir(resource);
    	
    	File[] res = wikiPageDir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".meta");
            }
        });
    	
    	if (res==null)
    		res = new File[0];
    	
    	Arrays.sort(res);
    	
    	return res;
    }

    @Override
    public String getWikiContent(URI resource, WikiRevision revision)
    {
        File dir = getContainingDir(resource);
        if (dir == null)
            return null;

        File f = new File(dir, toFileName(revision.date) + ".wiki");
        if(logger.isTraceEnabled()) logger.trace("Trying to load wiki-content from file: " + f);
        if (!f.exists())
            return null;
        
        try {
            return GenUtil.readFileUTF8(f);
        } catch (IOException ex) {
            return null;
        }       
    }   
    
    /**
     * Converts a URI to file name
     * 
     * @param resource
     * @return 
     */
    private String toFileName(URI resource)
    {
    	return StringUtil.urlEncode(resource.stringValue()).replace('*', '_');       
    }

    /**
     * Converts a date to a file name
     * 
     * @param time
     * @return
     */
    private String toFileName(Date time)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return sdf.format(time);
    }

    /**
     * Returns the sub-directory associated to a file
     * 
     * @param resource
     * @return
     */
    private File getContainingDir(URI resource)
    {
        File dir = WikiDirectoryCache.getInstance().lookup(resource);
        if (dir!=null)
            return dir;
        
        String name = toFileName(resource);
        final String outer = name;
        dir = new File(wikiRoot, name); // set to base dir
        
        // check if the directory exists:
        // (a) if so, we need to compare in case-sensitive way
        if (dir.exists()) // case-insensitive comparison under Windows
        {
            FilenameFilter fnf = new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.equals(outer);
                }
            };
            
            String[] matches = wikiRoot.list(fnf);
            if (matches.length==0)
            {
                // if there exists a directory with the same
                // name but different case; in that case, we store
                // the directory as a new subdirectory 
                dir = getCaseInsensitiveSubdir(dir,outer);
            }
        }

        // return new one
        WikiDirectoryCache.getInstance().insert(resource, dir);
        return dir;
    }

    @Override
    public boolean delete(URI resource) 
    {	
    	File f = getContainingDir(resource);
    	
    	if (f.isDirectory()) 
		{
    		for (File fi : f.listFiles()) 
    		{
    		    // note: the folder may contain subfolders, so we
    		    // have to take care to only delete files
    		    if (!fi.isDirectory())
    		        GenUtil.delete(fi);
    		}
    		if (f.delete()) // only applies if empty
    	        WikiDirectoryCache.getInstance().lookup(resource);
    	}
    	return true;
    }
    
	@Override
	public boolean deleteRevision(URI resource, WikiRevision rev) 
	{
		File dir = getContainingDir(resource);
		
		if (dir.isDirectory()) 
		{
			File[] files = dir.listFiles();
			
			for (File f : files) 
			{
				if(f.getName().contains(toFileName(rev.date))) 
				{
					GenUtil.delete(f);
				}
			}
		}
		
		return true;
	}
	


	@Override
	public boolean updateRevision(URI resource, WikiRevision rev) 
	{
		File dir = getContainingDir(resource);
		
		if (dir.isDirectory()) 
		{
			File[] files = dir.listFiles();
			
			for (File f : files) 
			{
				String fileName = toFileName(rev.date) + ".meta";
				if(f.getName().endsWith(fileName)) 
				{
					ObjectOutputStream oos = null;
			        try
			        {
						oos = new ObjectOutputStream(new FileOutputStream(f));
			            oos.writeObject(rev);
			        }
			        catch (Exception e)
			        {
			            logger.error(e.getMessage(), e);
			        }
			        finally
			        {
			        	try
			        	{
				        	if (oos!=null)
				        		oos.close();
			        	}
			        	catch (Exception e)
			        	{
			        	}
			        }
			        return true;
//					f.delete();
				}
			}
		}
		
		return false;
	}

	@Override
	public boolean deleteAllOlder(URI resource, Date date) 
	{
		File dir = getContainingDir(resource);
		if (dir.isDirectory()) 
		{
			File[] files = dir.listFiles();
			for (File f : files) 
			{
				if(!f.isDirectory() && 
				        f.getName().compareTo(toFileName(date)) > 0 && 
				        !f.getName().contains(toFileName(date))) 
				{
					GenUtil.delete(f);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Returns the subdirectory for the uri relative to the baseDir,
	 * thereby considering the case of the string. Creates the
	 * directory if it does not already exist.
	 * 
	 * @param baseDir
	 * @param uri
	 * @return
	 */
	public File getCaseInsensitiveSubdir(File baseDir, String uri)
	{
	    try
	    {
    	    String subdir = captitalStringSignature(uri);
    	    File f = new File(baseDir + File.separator + subdir);
    	    return f;
	    }
	    catch (Exception e)
	    {
	        logger.warn(e.getMessage(), e);
	    }
	    
	    return null; // error
	}
	
	/**
	 * Returns a unique signature of a string w.r.t. all other strings
	 * that have the same characters but a different capitalization.
	 * 
	 * Examples:
	 * - mytest -> 000000
	 * - myTest -> 001000
	 * - MyTeSt -> 101010
	 * 
	 * @param s
	 * @return the signature
	 */
	private String captitalStringSignature(String s)
	{
	    StringBuilder res = new StringBuilder();
	    for (int i=0;i<s.length();i++)
	        res.append(Character.isUpperCase(s.charAt(i)) ? "1" : "0");
	    return res.toString();
	}

    
	/**
	 * Applies a signature to a string, e.g. the signature 0010 applied
	 * to the string Abcd yields abCd.
	 * 
	 * @param wikiPage the string to apply the signature
	 * @param signature the signature itself
	 * @return the resulting string, null if signature invalid (e.g. differs in size)
	 */
	private String applySignature(String wikiPage, String signature) 
	{
		if (wikiPage==null || signature==null)
			return null;
		
		if (wikiPage.length()!=signature.length())
		{
			logger.warn("Signature '" + signature + "' cannot be applied to '" + wikiPage + "' (wrong size)");
			return null;
		}
		
		if (!signature.matches("[0-1]*"))
		{
			logger.warn("Signature '" + signature + "' invalid (contains chars different from 0 and 1");
			return null;			
		}
		
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<wikiPage.length(); i++)
		{
			if (signature.charAt(i)=='0')
				buf.append(wikiPage.substring(i,i+1).toLowerCase());
			else
				buf.append(wikiPage.substring(i,i+1).toUpperCase());
		}
		
		return buf.toString();
	}

	/**
	 * A file filter checking for wiki revisions (*.wiki)
	 */
	public static final FileFilter wikiRevisionFilter = new FileFilter() 
	{
    	@Override
    	public boolean accept(File f)
    	{
    		return f.getName().endsWith(".wiki");
    	}
	};
}
