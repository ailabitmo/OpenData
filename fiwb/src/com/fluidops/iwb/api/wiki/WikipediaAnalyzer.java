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

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.util.Version;

/**
 * Extension of StandardAnalyzer that is aware of Wikipedia syntax.
 * 
 * @author christian.huetter
 */
public final class WikipediaAnalyzer extends StopwordAnalyzerBase
{
	/**
	 * An unmodifiable set containing some common English words that are usually
	 * not useful for searching.
	 */
	public static final Set<?> STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

	/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param matchVersion
	 *            Lucene version to match See
	 *            {@link <a href="#version">above</a>}
	 * @param stopWords
	 *            stop words
	 */
	public WikipediaAnalyzer(Version matchVersion, Set<?> stopWords)
	{
		super(matchVersion, stopWords);
	}

	/**
	 * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 * 
	 * @param matchVersion
	 *            Lucene version to match See
	 *            {@link <a href="#version">above</a>}
	 */
	public WikipediaAnalyzer(Version matchVersion)
	{
		this(matchVersion, STOP_WORDS_SET);
	}

	/**
	 * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET})
	 * and the current Lucene version.
	 */
	public WikipediaAnalyzer()
	{
		this(Version.LUCENE_35, STOP_WORDS_SET);
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName, final Reader reader)
	{
		final WikipediaTokenizer src = new WikipediaTokenizer(reader);
		TokenStream tok = new StandardFilter(matchVersion, src);
		tok = new LowerCaseFilter(matchVersion, tok);
		tok = new StopFilter(matchVersion, tok, stopwords);
		return new TokenStreamComponents(src, tok)
		{
			@Override
			protected boolean reset(final Reader reader) throws IOException
			{
				return super.reset(reader);
			}
		};
	}
}
