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

import static com.fluidops.util.FileUtil.getFileContent;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.isEmpty;
import static java.lang.String.format;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.api.solution.SolutionService;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.DateTimeUtil;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.iwb.util.UrlUtils;
import com.fluidops.iwb.widget.SemWiki;
import com.fluidops.iwb.wiki.WikiBot;
import com.fluidops.iwb.wiki.WikiStorage;
import com.fluidops.iwb.wiki.WikiStorage.WikiRevision;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.FileUtil;
import com.fluidops.util.GenUtil;
import com.fluidops.util.StringUtil;
import com.fluidops.util.ZipUtil;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;

/**
 * API for access to wiki storage providing various convenience functions.
 * 
 * @author as
 */
public class WikiStorageBulkServiceImpl implements WikiStorageBulkService
{
    public static final String WIKIEXPORT_STORAGE = IWBFileUtil.DATA_DIRECTORY + "/solutions/";
    public static final String WIKIBOOTSTRAP_REL_PATH = IWBFileUtil.DATA_DIRECTORY;
    
    private static final Logger logger = Logger.getLogger(WikiStorageBulkServiceImpl.class);
    private static Logger installLogger = Logger.getLogger(SolutionService.INSTALL_LOGGER_NAME + ".wiki");
    
	/**
	 * Return the location of the wiki export storage
	 * 
	 * @return
	 */
	public static File getWikiExportStorageFolder() {
		return new File(Config.getConfig().getWorkingDir() + WIKIEXPORT_STORAGE);
	}
	
    private static final FilenameFilter BOOTSTRAP_DIRS = new FilenameFilter()
    {
        public boolean accept(File dir, String name)
        {
            return name.startsWith(WikiStorageBulkService.BOOTSTRAP_DIRNAME_PREFIX) && new File(dir, name).isDirectory();
        }
    };
    private static final Predicate<WikiRevision> IS_BOOTSTRAP = new Predicate<WikiRevision>()
    {
        @Override
        public boolean apply(WikiRevision revision)
        {
            return revision != null && revision.isBootstrapRevision();
        }
    };
    private final WikiStorage ws;

    public WikiStorageBulkServiceImpl(WikiStorage wikiStorage)
    {
        this.ws = wikiStorage;
    }

    /**
     * Return all wikipages and associated metadata for the most recent
     * revision.
     * 
     * @return
     */
    public List<WikiPageMeta> getAllWikipages()
    {
        return getWikipagesInternal(new VoidFilter());
    }

    /**
     * Return all wikipages and associated metadata that are user edited for the
     * most recent version
     * 
     * @return
     */
    public List<WikiPageMeta> getAllWikipagesNotFromBootstrap()
    {
        return getWikipagesInternal(new NotFromBootstrapFilter());
    }
    
    /**
     * Return all user pages that shadow at least one bootstrap page, i.e.
     * there exists a bootstrap page in the revision history of a user page
     * @return
     */
    public List<WikiPageMeta> getAllUserPagesShadowingBootstrap() { 
    	return getWikipagesInternal(new UserPageShadowsBootstrapFilter());
    }

    /**
     * Return all wikipages and associated metadata that are user loaded for the
     * most recent version
     * 
     * @return
     */
    public List<WikiPageMeta> getAllWikipagesFromBootstrap()
    {
        return getWikipagesInternal(new FromBootstrapFilter());
    }

    /**
     * Return all wikipages applying some filter
     * 
     * @param filter
     * @return
     */
    public List<WikiPageMeta> getAllWikipages(Filter filter)
    {
        return getWikipagesInternal(filter);
    }

    /**
     * Create a wiki bootstrap called wikibootstrapTIMESTAMP.zip that contains the
     * most recent revision of the selected pages. The internal structure of the
     * zip file is such that it can be used by the solution service, i.e. pages
     * are in the sub directory "data/wikiBootstrap/".
     * 
     * The Zip file is written to {@link #WIKIEXPORT_STORAGE}
     * 
     * @param selectedPages
     * @return the filename, e.g. wikiBootstrapTIMESTAMP.zip
     * @throws Exception
     *             if an I/O error occurs
     * @throws IllegalArgumentException
     *             if the selections are empty or null
     * @throws IllegalStateException
     *             if the wiki directory could not be created
     */
    public String createWikiBootstrap(List<WikiPageMeta> selectedPages) throws IllegalArgumentException,
            IllegalStateException, Exception
    {

        if (selectedPages == null || selectedPages.size() == 0)
            throw new IllegalArgumentException("No wiki pages selected.");

        File storagePath = getWikiExportStorageFolder();
        if (!storagePath.exists())
        	GenUtil.mkdirs(storagePath);        
        
        File tempDir = GenUtil.createTmpDir("wikiExport");
        
        File wikiBootstrapFolder = wikiStorageRelFolder(tempDir);
       	GenUtil.mkdirs(wikiBootstrapFolder);
        
        try 
        {
	        WikiStorage ws = Wikimedia.getWikiStorage();
	        for (WikiPageMeta m : selectedPages)
	        {
	        	File wikiFile = new File(wikiBootstrapFolder, 
	        			StringUtil.urlEncode(m.getPageUri().stringValue()));
	            String content = ws.getRawWikiContent(m.getPageUri(), m.getRevision().date);
	            FileUtil.writeContentToFile(content, wikiFile.getAbsolutePath());
	        }
	        
	        File zipFile = new File(storagePath, wikiBootstrapFolder.getName() + System.currentTimeMillis() + ".zip");
	
	        // zip files and delete temp files
	        ZipUtil.doZipOutput(zipFile, tempDir, wikiBootstrapFolder.listFiles());	        
	        
	        logger.info("Successfully created wiki export to " + zipFile.getAbsolutePath());	                
	        return zipFile.getName();
        } finally {
        	// clean up temporary folder, i.e. keep zip files only
	        GenUtil.deleteRec(tempDir);
        }
    }
    
    /**
     * Restore the latest bootstrap version iff
     * 
     * a) the current page is a user page
     * b) there exists some bootstrap version
     * c) the content of the latest version and the bootstrap are actually different
     * 
     * This method also writes the semantic link differences into
     * the triple store
     * 
     * @param name
     * @throws IOException
     * @throws {@link IllegalStateException} if the content of current and bootstrap are identical
     * @return true if the restore operation was successful, false if the operation was not performed
     * 			as one of the conditions above was not met
     * 
     */
    public boolean restoreLatestBootstrap(URI name) throws IOException {

    	List<WikiRevision> revs = ws.getWikiRevisions(name);
    	if (revs==null || revs.size()<=1)
    		return false;
    	
    	WikiRevision last = revs.get(revs.size()-1);
    	if (last.isBootstrapRevision())
    		return false;
    	
    	WikiRevision lastBootstrap = null;
    	for (int i=revs.size()-2; i>=0; i--) {
    		WikiRevision current = revs.get(i);
    		if (current.isBootstrapRevision()) {
    			lastBootstrap = current;
    			break;
    		}
    	}
    	
    	if (lastBootstrap==null)
    		return false;
    	    	    	
    	// TODO unify with SemWiki#saveWiki and also consider RevisionTable
    	String content = ws.getWikiContent(name, lastBootstrap);
    	String prevContent = ws.getWikiContent(name, last);
    	if (content.equals(prevContent))
    		throw new IllegalStateException("Current content identical for page " + name.stringValue());
    	
    	logger.debug("Restoring latest bootstrap version of resource " + name.stringValue());
    	ws.storeWikiContent(name, content, "Restored revision from " + lastBootstrap.comment, DateTimeUtil.getDateSafe());
    	SemWiki.saveSemanticLinkDiff(prevContent, content, name, Context.getFreshUserContext(ContextLabel.WIKI));
    	return true;
    }

    /**
     * Create a wiki bootstrap called wikibootstrapTIMESTAMP that contains all
     * user edited pages (i.e. excluding the ones that are imported by another
     * bootstrap)
     * 
     * @throws Exception
     *             if an I/O error occurs
     * @throws IllegalArgumentException
     *             if the selections are empty or null
     * @throws IllegalStateException
     *             if the wiki directory could not be created
     */
    public String createWikiBootstrap() throws IllegalArgumentException, IllegalStateException, Exception
    {
        return createWikiBootstrap(getAllWikipagesNotFromBootstrap());
    }

    @Override
    public void bootstrapWiki(File workingDir, String artifactVersion)
    {
        bootstrapWiki(workingDir, false, artifactVersion);
    }
    
	/**
	 * the wiki storage folder relative to baseDir,
	 * e.g. %baseDir%/data/wikiBootstrap/
	 * @param baseDir
	 * @return
	 */
	static File wikiStorageRelFolder(File baseDir) {
		return new File(new File(baseDir, WIKIBOOTSTRAP_REL_PATH), BOOTSTRAP_DIRNAME_PREFIX);
	}
	
    public void bootstrapWikiAndRemove(File workingDir) {
        bootstrapWiki(workingDir, true, applicationVersion());
    }
    
    private String applicationVersion() {
    	try {
			return EndpointImpl.api().version();
		} catch (RemoteException e) {
			throw Throwables.propagate(e);
		}
    }

    private void bootstrapWiki(File workingDir, boolean removeDirAfterImport, String artifactVersion)
    {
        for (File dir : workingDir.listFiles(BOOTSTRAP_DIRS))
        {
            installLogger.info("bootstrap all wiki pages from folder: " + dir);
            
            for (File wikiFile : dir.listFiles()) {
                if(wikiFile.isFile() && wikiFile.canRead()) importFile(wikiFile, dir.getName(), artifactVersion);
            }
            if(removeDirAfterImport) FileUtil.deleteDirectory(dir);
        }
    }

    private URI importFile(File wikiFile, String bootstrapName, String artifactVersion)
    {
        URI subject = EndpointImpl.api().getNamespaceService().guessURI(UrlUtils.urlDecode(wikiFile.getName()));
        Date date = bootstrapTimestampFor(subject);
        logger.trace("bootstrapping wiki file: " + wikiFile);
        try
        {
        	// store the old content of the current version
        	WikiRevision prevRevision = ws.getLatestRevision(subject);
        	String oldContent = prevRevision==null?"":ws.getWikiContent(subject, prevRevision);

        	if (prevRevision!=null && !prevRevision.isBootstrapRevision())
        		installLogger.warn("Bootstrap page '" + subject.stringValue() + "' is shadowed by a user page.");
        		
        	// update the content
            String content = getFileContent(wikiFile);
            ws.storeWikiContent(subject, content, format("Bootstrap %s (%s, v%03d)", artifactVersion, bootstrapName, date.getTime() - WikiStorage.IWB_BOOTSTRAP_EPOCH), date);
            
        	// store the new content of the current version
        	WikiRevision curRevision = ws.getLatestRevision(subject);
        	String newContent = curRevision==null?"":ws.getWikiContent(subject, curRevision);
        	        	
            // if the current version has changed, we need to perform an update
            if (!oldContent.equals(newContent))
            {
            	// store the semantic link difference between the previous 
            	// version (if any) and the current version
    	        Context context = Context.getFreshPublishedContext(
    	        		ContextType.USER, null, Vocabulary.SYSTEM.USER, 
    	        		null, null, true, ContextLabel.WIKI);
    	        SemWiki.saveSemanticLinkDiff(oldContent,content,subject,context);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while processing file " + wikiFile, e);
        }
        return subject;
    }

    private Date bootstrapTimestampFor(URI subject)
    {
        Iterable<WikiRevision> bootstrapRevisions = filter(ws.getWikiRevisions(subject), IS_BOOTSTRAP);
        if(isEmpty(bootstrapRevisions)) return new Date(WikiStorage.IWB_BOOTSTRAP_EPOCH);
        WikiRevision latesBootstrapRevision = max(bootstrapRevisions);
        return ws.nextBootstrapVersion(latesBootstrapRevision.date);
    }

    private WikiRevision max(Iterable<WikiRevision> bootstrapRevisions)
    {
        WikiRevision latesBootstrapRevision = bootstrapRevisions.iterator().next();
        for (WikiRevision wikiRevision : bootstrapRevisions)
        {
            if(latesBootstrapRevision.date.getTime() < wikiRevision.date.getTime())
                latesBootstrapRevision = wikiRevision;
        }
        return latesBootstrapRevision;
    }
    
    /**
     * Return all available wikipages after applying the provided filter. Uses
     * {@link Wikimedia#getWikiStorage()} as underlying API.
     * 
     * @param filter
     * @return
     */
    protected List<WikiPageMeta> getWikipagesInternal(Filter filter)
    {

        List<WikiPageMeta> res = new ArrayList<WikiPageMeta>();

        for (URI wikiPage : ws.getAllWikiURIs())
        {
            WikiRevision revision = ws.getLatestRevision(wikiPage);
            int numberOfRevisions = ws.getRevisionCount(wikiPage);
            // try to find shadowing user pages:
            // there exists a bootstrap page that is shadowed by a user page
            boolean shadows=false;
            if (!revision.isBootstrapRevision() && numberOfRevisions>1) {
            	List<WikiRevision> revs = ws.getWikiRevisions(wikiPage);
            	for (WikiRevision r : revs) {
            		if (r.isBootstrapRevision()) {
            			shadows = true;
            		}
            	}
            }
            WikiPageMeta wikiPageMeta = new WikiPageMeta(wikiPage, revision, numberOfRevisions, shadows);
            if (filter.keep(wikiPageMeta))
                res.add(wikiPageMeta);
        }

        return res;
    }

    public static class WikiPageMeta
    {
        private final WikiRevision revision;

        private final URI pageUri;

        private final int numberOfRevisions;
        
        private final boolean shadows;

        public WikiPageMeta(URI pageUri, WikiRevision revision, int numberOfRevisions, boolean shadows)
        {
            this.revision = revision;
            this.pageUri = pageUri;
            this.numberOfRevisions = numberOfRevisions;
            this.shadows = shadows;
        }
        
        /**
         * Returns true iff this revision is a user page and there
         * exists a shadowed bootstrap version
         * @return
         */
        public boolean shadowsBootstrapPage() {
        	return shadows;
        }
        
        /**
         * Returns the latest version of the wiki page content
         */
        public String getContent()
        {
            return Wikimedia.getWikiContent(pageUri, null);
        }
        
        public WikiRevision getRevision()
        {
            return revision;
        }

        public URI getPageUri()
        {
            return pageUri;
        }

        public boolean isBootstrap()
        {
            return revision.bootstrapVersion() != null;
        }

        public int getNumberOfRevisions()
        {
            return numberOfRevisions;
        }

        @Override
        public String toString()
        {
            return "[pageUri=" + pageUri + "]";
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((pageUri == null) ? 0 : pageUri.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            WikiPageMeta other = (WikiPageMeta) obj;
            if (pageUri == null)
            {
                if (other.pageUri != null)
                    return false;
            }
            else if (!pageUri.equals(other.pageUri))
                return false;
            return true;
        }

    }

    /**
     * Implementations can filter wiki pages.
     * 
     * @author as
     */
    public static interface Filter
    {
        /**
         * Implementations can decide if the provided wikpage shall be kept
         * (true) or not (false)
         * 
         * @param wikiPage
         *            true if this wikipage shall be kept
         * @return
         */
        public boolean keep(WikiPageMeta wikiPage);
    }

    public static class VoidFilter implements Filter
    {
        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
            return true;
        }
    }

    /**
     * Filter if loaded from bootstrap
     */
    public static class NotFromBootstrapFilter implements Filter
    {
        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
            return !wikiPage.isBootstrap();
        }
    }

    /**
     * Filter if loaded from bootstrap
     */
    public static class FromBootstrapFilter implements Filter
    {
        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
            return wikiPage.isBootstrap();
        }
    }
    
    /**
     * Filter to show those user pages that shadow a bootstrap page
     */
    public static class UserPageShadowsBootstrapFilter implements Filter
    {
        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
            return wikiPage.shadowsBootstrapPage();
        }
    }

    
    /**
     * Filter if imported from {@link WikiBot}, keeps item if
     * comment equals {@value WikiBot#WIKIBOT_WIKIREVISION_COMMENT}
     */
    public static class ImportedFromWikibotFilter implements Filter
    {
        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
            return wikiPage.revision.comment.equals(WikiBot.WIKIBOT_WIKIREVISION_COMMENT);
        }
    }

    /**
     * Filter anything that does not have the provided namespace as prefix, i.e.
     * return all those wikipages that have the prefix
     */
    public static class NamespacePrefixFilter implements Filter
    {
        private final String nsPrefix;

        public NamespacePrefixFilter(String nsPrefix)
        {
            this.nsPrefix = nsPrefix;
        }

        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
            return wikiPage.getPageUri().stringValue().startsWith(nsPrefix);
        }
    }

    /**
     * Filter anything that is not in the provided set.
     */
    public static class StringSetFilter implements Filter
    {
        private final Set<String> keepSet;

        public StringSetFilter(Set<String> keepSet)
        {
            this.keepSet = keepSet;
        }

        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
            return keepSet.contains(wikiPage.getPageUri().stringValue());
        }
    }
    
    /**
     * Filter based on a regular expression. If the passed regexp is null
     * or empty, then the regexp filter has no effect.
     */
    public static class RegexpFilter implements Filter
    {
        private final String regexp;

        public RegexpFilter(String regexp)
        {
            this.regexp = regexp;
        }

        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
        	// if no regexp is provided, the filter has no effect
        	if (StringUtil.isNullOrEmpty(regexp))
        		return true;
        	
            return wikiPage.getPageUri().toString().matches(regexp);
        }
    }
    
    /**
     * Filter consisting of several filters
     */
    public static class MultiFilter implements Filter
    {
        private final List<Filter> filters;

        public MultiFilter(List<Filter> filters)
        {
            this.filters = filters;
        }

        @Override
        public boolean keep(WikiPageMeta wikiPage)
        {
        	for (Filter filter : filters)
        	{
        		if (!filter.keep(wikiPage))
        			return false;
        	}
        	
        	return true;
        }
    }

	/**
	 * Keep a number of latest revisions and delete the rest (check if the bootstrap revisions should be kept)
	 * @param wikiPageURI
	 * @param latestRevisionsToKeep
	 * @param keepBootstrap
	 */
	public void deleteRevisions(URI wikiPageURI, int latestRevisionsToKeep, boolean keepBootstrap)
	{
		if (latestRevisionsToKeep<0)
			throw new IllegalArgumentException("Latest revisions to keep must be a positive integer");
		
		List<WikiRevision> revs = ws.getWikiRevisions(wikiPageURI);
		for (int i=0; i<revs.size()-latestRevisionsToKeep; i++) {
			WikiRevision wr = revs.get(i);
			if (keepBootstrap && wr.isBootstrapRevision())
				continue;
			wr.delete(wikiPageURI);
		}
	}

	/**
	 * Delete a number of latest revisions (check if the bootstrap revisions should be kept)
	 * @param wikiPageURI
	 * @param revisionsToDelete
	 * @param keepBootstrap
	 */
	public void deleteLatestRevisions(URI wikiPageURI, int revisionsToDelete, boolean keepBootstrap)
	{
		if (revisionsToDelete<0)
			throw new IllegalArgumentException("Revisions to delete must be a positive integer");
		
		List<WikiRevision> revs = ws.getWikiRevisions(wikiPageURI);
		
		int allRevsNumber = revs.size();
		for(int i = 1 ; i <= revisionsToDelete && i <= allRevsNumber; i++ )
		{
			WikiRevision wr = revs.get(allRevsNumber-i);
			if (keepBootstrap && wr.isBootstrapRevision())
				continue;
			wr.delete(wikiPageURI);
		}
	}
}
