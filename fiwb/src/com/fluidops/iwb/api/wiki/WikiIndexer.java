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

package com.fluidops.iwb.api.wiki;

import static org.openrdf.model.impl.ValueFactoryImpl.getInstance;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;


public class WikiIndexer extends WikiConsumer
{
    private static final Logger logger = Logger.getLogger(WikiIndexer.class.getName());

    final IndexWriter indexWriter;
    
    public WikiIndexer(XMLWikiReader reader) throws CorruptIndexException, LockObtainFailedException, IOException
    {
        super(reader);
        
        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
//        indexWriter = new IndexWriter("keywordindex/resources/wiki1", analyzer, true);
        FSDirectory dir = new SimpleFSDirectory(new File("keywordindex/resources/wiki1"));
		indexWriter = new IndexWriter(dir, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
    }

    @Override
    protected void perform(Page page)
    {

        // Add an entry to the Wiki-Keyword-Index
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
        String uri = null;
        try {
			uri = getInstance().createURI(reader.getNamespace() + URLEncoder.encode(page.title.toString(), "UTF-8")).stringValue();
		} catch (UnsupportedEncodingException e1) {
			logger.error(e1.getMessage(), e1);
		}
        doc.add(new Field("URI", uri, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("localname", page.title.toString(), Field.Store.YES,
                Field.Index.ANALYZED));
        doc.add(new Field("page", page.text.toString(), Field.Store.YES,
                Field.Index.ANALYZED));

        try {
            indexWriter.addDocument(doc);
        }
        catch (CorruptIndexException e) {
            logger.error(e.getMessage(), e);
        }
        catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected void finish() throws Exception
    {
        indexWriter.close();
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName();
    }
}
