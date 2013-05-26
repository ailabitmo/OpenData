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

package com.fluidops.iwb.keywordsearch;

import static com.fluidops.iwb.model.Vocabulary.SYSTEM.WIKI;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.lucene.LuceneIndex;
import org.openrdf.sail.lucene.LuceneSail;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.cms.util.ExtractText;
import com.fluidops.iwb.wiki.WikiStorage;
import com.fluidops.iwb.wiki.WikiSynchronizer;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.Pair;

public class KeywordIndexAPI {
	
	private static final Logger logger = Logger.getLogger(KeywordIndexAPI.class.getName());
	
	private static LuceneSail getLuceneSail()
	{
		if (Global.repository != null && (Global.repository instanceof NotifyingRepositoryWrapper))
		{
			NotifyingRepositoryWrapper wrapper = (NotifyingRepositoryWrapper)Global.repository;
			if(wrapper.getDelegate() instanceof SailRepository) {
				Sail sail = ((SailRepository)wrapper.getDelegate()).getSail();
				if (sail instanceof LuceneSail)
					return (LuceneSail) sail;
			}
		}
		return null;
	}
	
	private static LuceneSail getWikiLuceneSail()
	{
		if (Global.wikiLuceneRepository != null)
		{
			Sail sail = Global.wikiLuceneRepository.getSail();
			if (sail instanceof LuceneSail)
				return (LuceneSail) sail;
		}
		return null;
	}
	
	public static boolean updateKeywordIndex() throws Exception
	{
		logger.info("Reindexing semantic data. Can take a while...");
		LuceneSail luceneSail = getLuceneSail();
		if (luceneSail != null)
		{
			luceneSail.reindex();
			return true;
		}
		else
		{
			logger.debug("Could not update keyword index because no LuceneSail is configured.");
			return false;
		}
	}
	
	public static boolean updateWikiIndex() throws Exception
	{
		logger.info("Reindexing Wiki content. Can take a while...");
		LuceneSail luceneSail = getWikiLuceneSail();
		if (luceneSail != null)
		{
			luceneSail.reindex();
			WikiStorage storage = Wikimedia.getWikiStorage();
			List<URI> uriList = storage.getAllWikiURIs();
			for (URI uri : uriList)
			{
				indexWikiPage(luceneSail.getLuceneIndex(), uri, storage.getRawWikiContent(uri, null));
			}
			return true;
		}
		else
		{
			logger.debug("Could not update wiki keyword index because no LuceneSail is configured for the wiki.");
			return false;
		}
	}
	
	public static void replaceWikiIndexEntry(URI uri, String content) throws IOException
	{
		LuceneSail luceneSail = getWikiLuceneSail();
		if (luceneSail != null)
		{
			indexWikiPage(luceneSail.getLuceneIndex(), uri, content);
		}
		else
		{
			logger.debug("Could not update keyword index because no LuceneSail is configured.");
		}
	}

	private static void indexWikiPage(LuceneIndex luceneIndex, Resource resource, String content) throws IOException
	{
		if (luceneIndex == null)
			return;
		
		ValueFactory vf = ValueFactoryImpl.getInstance();
		
		// remove old wiki pages from Lucene index, if any
		List<Document> docs = luceneIndex.getDocuments(resource);
		
		for(Document doc : docs) {
			String[] values = doc.getValues(WIKI.stringValue());
			for (String value : values)
			{
				Statement remove = vf.createStatement(resource, WIKI, vf.createLiteral(value));
				luceneIndex.removeStatement(remove);
			}
		}
		
		if(content==null)
			return;

		// filter out HTML tags from wiki content
		content = ExtractText.html2text(content).trim();
		
		// add new wiki page to index
		Statement add = vf.createStatement(resource, WIKI, vf.createLiteral(content));
		luceneIndex.addStatement(add);
	}
	
	@Deprecated
	public static void replaceKeywordIndexEntry(URI uri) throws Exception
	{
		logger.debug("KeywordIndexAPI.replaceKeywordIndexEntry(URI) is deprecated");
/*
		SimpleFSDirectory dir = new SimpleFSDirectory(new File(Constant.keywordIndexLocation + "/keyword"));
		
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		try 
		{
			IndexWriter indexWriter = new IndexWriter(dir, analyzer, false, IndexWriter.MaxFieldLength.UNLIMITED);

			replaceKeywordIndexEntry(uri, indexWriter);
			indexWriter.close();
		}
		catch (LockObtainFailedException e)
		{
			logger.info("Could not lock index, queuing request: " + e.getMessage());
			synchronized (toBeUpdated)
			{
				if (!toBeUpdated.contains(uri))
					toBeUpdated.add(uri);
			}
		}
*/
	}
	
	@Deprecated
	private static void replaceKeywordIndexEntry(URI uri, IndexWriter indexWriter) throws Exception 
	{
		logger.debug("KeywordIndexAPI.replaceKeywordIndexEntry(URI, IndexWriter) is deprecated");
/*
		Document doc = new Document();
		
		doc.add(new Field(Constant.URI_FIELD, uri.stringValue(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		Field field = new Field(Constant.LOCALNAME_FIELD, Constant.LOCALNAME_FIELD + " = " + uri.getLocalName(), Field.Store.YES, Field.Index.ANALYZED);
		field.setBoost(1);
		doc.add(field);
		
		RepositoryConnection con = Global.repository.getConnection();
		
		doc.add(new Field(Constant.TYPE_FIELD, TypeUtil.ENTITY, Field.Store.YES, Field.Index.NO));
		
		RepositoryResult<Statement> res = con.getStatements(uri, RDF.TYPE, null, false);
		
		Set<String> types = new HashSet<String>();
		boolean docEmpty = !res.hasNext();
		
		while (res.hasNext()) {
			types.add(res.next().getObject().stringValue());
		}
		
		for (String type : types) {
			field = new Field(Constant.CONCEPT_FIELD, type, Field.Store.YES, Field.Index.NOT_ANALYZED);
			field.setBoost(1);
			doc.add(field);
		}
		Set<String> attribs = new HashSet<String>();
		res = con.getStatements(uri, null, null, false);
		docEmpty = !res.hasNext();
		
		while (res.hasNext()) {
			Statement s = res.next();
			attribs.add(s.getPredicate().stringValue().trim() + " = " + s.getObject().stringValue());
			field = new Field(Constant.ATTRIBUTE_FIELD, s.getPredicate().stringValue().trim() + " = " + s.getObject().stringValue(), Field.Store.YES, Field.Index.ANALYZED);
			field.setBoost(1);
			doc.add(field);
		}
		if (!docEmpty)
			indexWriter.updateDocument(new Term(Constant.URI_FIELD, uri.stringValue()), doc);
		else
			indexWriter.deleteDocuments(new Term(Constant.URI_FIELD, uri.stringValue()));
		
		con.close();
*/
	}
	
	public static void changeSemanticLinks(Statement oldStmt, Statement newStmt)
    {
    	WikiSynchronizer.changeSemanticLinks(oldStmt, newStmt);
    }
    
    public static void changeSemanticLinks(List<Pair<Statement, Statement>> pairList)
    {
    	WikiSynchronizer.changeSemanticLinks(pairList);
    }
    
    public static void removeSemanticLinks(List<Statement> stmtList)
    {
    	WikiSynchronizer.removeSemanticLinks(stmtList);
    }
    
    public static void removeSemanticLinks(Statement stmt)
    {
    	WikiSynchronizer.removeSemanticLinks(stmt);
    }
    
    @Deprecated
    public static void updateUrisInIndex(Set<URI> urisToUpdate) 
    {
		logger.debug("KeywordIndexAPI.updateUrisInIndex(Set<URI>) is deprecated");
/*
    	try 
    	{
	    	SimpleFSDirectory dir = new SimpleFSDirectory(new File(Constant.keywordIndexLocation + "/keyword"));
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
			IndexWriter writer = new IndexWriter(dir, analyzer, false, IndexWriter.MaxFieldLength.UNLIMITED);
			
			try 
			{
		    	for (URI uri : urisToUpdate)
		    	{
		    		KeywordIndexAPI.replaceKeywordIndexEntry(uri, writer);
		    	}
			}
	    	finally
	    	{
	    		writer.close();
	    	}
    	}
    	catch (LockObtainFailedException le)
    	{
    		logger.info("Could not lock index, queuing request: " + le.getMessage());
    		synchronized (toBeUpdated)
    		{
    			toBeUpdated.addAll(urisToUpdate);
    		}
    	}
    	catch (Exception e)
    	{
    		logger.warn(e.getMessage(), e);
    	}
*/    	
    }

	/**
	 * Shut down search index by closing the Lucene IndexReader and IndexSearcher.
	 */
	public static void shutdown()
	{
		LuceneSail luceneSail = getLuceneSail();
		if (luceneSail != null)
		{
			try
			{
				luceneSail.getLuceneIndex().shutDown();
			}
			catch (IOException e)
			{
				logger.error("Could not shut down Lucene index.", e);
			}
		}
		else
		{
			logger.debug("Could not shut down search index because no LuceneSail is configured.");
		}
		
		luceneSail = getWikiLuceneSail();
		if (luceneSail != null)
		{
			try
			{
				luceneSail.getLuceneIndex().shutDown();
			}
			catch (IOException e)
			{
				logger.error("Could not shut down wiki Lucene index.", e);
			}
		}
		else
		{
			logger.debug("Could not shut down wiki search index because no LuceneSail is configured.");
		}
	}
}
