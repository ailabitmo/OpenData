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

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.model.MutableTupleQueryResultImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.StringUtil;

public class KeywordSearchAPI
{
	public static final String defaultQuerySkeleton =
			"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> \n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
			"SELECT ?Subject ?Property ?Value ?Type \n" +
			"WHERE { \n" +
			"  { SELECT ?Subject ?Property ?Value \n" +
			"    WHERE { \n" +
			"      ?Subject search:matches ?match . \n" +
			"      ?match search:query \"??\" ; \n" +
			"             search:property ?Property ; \n" +
			"             search:snippet ?Value ; \n" +
			"             search:score ?score . \n" +
			"    } ORDER BY DESC(?score) LIMIT 1000 \n" +
			"  } \n" +
			"  OPTIONAL { ?Subject rdf:type ?Type . } \n" +
			"}";

	public static final String defaultWikiQuerySkeleton =
			"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> \n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
			"SELECT ?Subject ?Property ?Value ?Type \n" +
			"WHERE { \n" +
			"  { SELECT ?Subject ?Property ?Value \n" +
			"    WHERE { \n" +
			"      ?Subject search:matches ?match . \n" +
			"      ?match search:query \"??\" ; \n" +
			"             search:property ?Property ; \n" +
			"             search:snippet ?Value ; \n" +
			"             search:score ?score . \n" +
			"    } ORDER BY DESC(?score) LIMIT 1000 \n" +
			"  } \n" +
			"  BIND (\"Wiki page\" AS ?Type) \n" +
			"}";
	
	/**
	 * Parses the keyword string and returns a list of the keywords contained.
	 * Stop words are filtered out.
	 * 
	 * @param keywordString	the keyword string
	 * @return list	of the keywords contained
	 */
	public static List<String> parseKeywords(String keywordString)
	{
		keywordString = keywordString.trim();
		List<String> keywords = new ArrayList<String>();
		String[] ar = keywordString.split("\\s+");
		for (String s : ar) {
			if (!StandardAnalyzer.STOP_WORDS_SET.contains(s.toLowerCase())) {
				keywords.add(s.toLowerCase());
			}
		}
		return keywords;
	}

	/**
	 * Parse the given Lucene query using AND as default operator.
	 * 
	 * @param query	the query string to be parsed
	 * @return the parsed query
	 */
	public static Query parseQuery(String query) throws ParseException
	{
		return parseQuery("", query);
	}

	/**
	 * Parse the given Lucene query using AND as default operator.
	 * 
	 * @param field	the default field for query terms
	 * @param query	the query string to be parsed
	 * @return the parsed query
	 */
	private static Query parseQuery(String field, String query) throws ParseException
	{
		query = StringUtil.escapeUnsupportedLuceneCharacters(query);
		QueryParser qp = new QueryParser(Version.LUCENE_35, field,
				new StandardAnalyzer(Version.LUCENE_35));
		qp.setDefaultOperator(QueryParser.Operator.AND);
		return qp.parse(query);
	}

	/**
	 * Parse the given Lucene query using AND as default operator, and convert
	 * the query into a normalized String representation.
	 * 
	 * @param query	the query string to be parsed
	 * @return normalized String representation of the query
	 */
	public static String normalizeLuceneQuery(String query) throws ParseException
	{
		Query q = parseQuery("", query);
		return q.toString();
	}

	/**
	 * Perform keyword search over structured data using the given query.
	 * 
	 * @param query	the keyword query
	 * @return search result
	 */
	public static TupleQueryResult search(String query) throws ParseException, MalformedQueryException, QueryEvaluationException
	{
		String luceneQuery = normalizeLuceneQuery(query);
		luceneQuery = StringUtil.escapeSparqlStrings(luceneQuery);
		String sparqlQuery = Config.getConfig().getKeywordQuerySkeleton().replace("??", luceneQuery);

		ReadDataManager dm = EndpointImpl.api().getDataManager();
        TupleQueryResult res = dm.sparqlSelect(sparqlQuery, true, null, false);
        return new MutableTupleQueryResultImpl(res);
	}

}
