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

package com.fluidops.iwb.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.wiki.WikiStorage.WikiRevision;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.textdiff.TextDiff2;
import com.fluidops.textdiff.TextDiff2.Diff;
import com.fluidops.textdiff.TextDiff2.Operation;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;
import com.fluidops.util.XML;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * generate RSS feed content
 * 
 * @author aeb
 */
public class RSS
{
	
	/**
	 * RSS feed item
	 */
	public static class Item implements Comparable<Item>
	{
		public Item( ReadDataManager dm, String link, Statement s )
		{
			this.title = dm.getLabelHTMLEncoded(s.getSubject()) + " " + dm.getLabelHTMLEncoded(s.getPredicate()) + " " + dm.getLabelHTMLEncoded(s.getObject());
			this.link = link;
            Context c = Context.loadContextByURI((URI) s.getContext(),EndpointImpl.api().getDataManager());
            this.date = c.getTimestamp();

		}
		
		public Item( String link, String title, long date )
		{
			this.link = link;
			this.title = title;
			this.date = date;
		}
		
		String title;
		String link;
		long date;
		
		@Override
		@SuppressWarnings(value="EQ_COMPARETO_USE_OBJECT_EQUALS", justification="Compare to explicitly defined differently from equals.")
		public int compareTo(Item o)
		{
			if ( date < o.date )
				return 1;
			if ( date > o.date )
				return -1;
			return 0;
		}
	}
	
	static String rssLink( String base, URI subject ) throws UnsupportedEncodingException
	{
		return base.substring( 0, base.length()-"/rss.jsp".length() ) + Config.getConfig().getUrlMapping() + "?uri="+URLEncoder.encode(subject.stringValue(), "UTF-8");
	}
	
	public static List<Item> getRssItems(URI subject, String base, Value object) throws IOException {
		
		String x = null;
        if (subject != null ) {
        	x = rssLink(base, subject);
        }
		
		List<Item> items = new ArrayList<Item>();

        ReadDataManager dm = EndpointImpl.api().getDataManager();
        NamespaceService ns = EndpointImpl.api().getNamespaceService();
        for ( Statement s : dm.getStatementsAsList( subject, null, object, false ) )
        	if (s.getContext()!=null && s.getContext() instanceof URI && Context.loadContextByURI((URI)s.getContext(),dm).isUserContext() )
        		items.add( new Item( dm, rssLink(base, (URI)s.getSubject()), s ) );
        		
        List<WikiRevision> wikis = null;
        Map<WikiRevision, URI> revToSubject = new HashMap<WikiRevision, URI>();
        if (subject != null) {
	        for ( Statement s : dm.getStatementsAsList( null, null, subject, false ) )
	        	if (s.getContext()!=null && s.getContext() instanceof URI && Context.loadContextByURI((URI)s.getContext(),dm).isUserContext() )
	        		if ( s.getSubject() instanceof URI )
	        			items.add( new Item( dm, rssLink(base, (URI)s.getSubject()), s ) );
	        wikis = Wikimedia.getWikiStorage().getWikiRevisions( subject );
	        for (WikiRevision wiki : wikis) {
	        	revToSubject.put(wiki, subject);
	        }
        }
        else if (object == null){
        	wikis = new ArrayList<WikiRevision>();
        	List<Pair<URI, WikiRevision>> latestRevs = Wikimedia.getWikiStorage().getLatestWikiRevisions();
        	for (Pair<URI, WikiRevision> wikiRev : latestRevs) {
        		wikis.add(wikiRev.snd);
        		revToSubject.put(wikiRev.snd, wikiRev.fst);
        	}
        	
        }
        String prevRevision = "";
        if (wikis != null)
	        for ( WikiRevision wiki : wikis )
	        	if ( "(no comment)".equals( wiki.comment ) )
	        	{
	        		try
	        		{
	    				String revision = StringUtil.removeBeginningAndEndingQuotes( Wikimedia.getWikiStorage().getWikiContent(subject, wiki) );
	                    
	                    List<Diff> diffs = new TextDiff2().diff_lineMode(prevRevision, revision);
	                    
	                    prevRevision = revision;
	                    
	                    for ( Diff diff : diffs )
	                    	if ( diff.operation != Operation.EQUAL ) //Edit or Delete
	                    	{
	                    		items.add( new Item( x, "Wiki modification: " + ns.getAbbreviatedURI(subject) + "  " + diff.text, wiki.date.getTime() ) );
	                    		break;
	                    	}
	        		}
	        		catch ( Exception e )
	        		{
	        			String s = ns.getAbbreviatedURI(revToSubject.get(wiki));
	        			if (s == null)
	        				s = revToSubject.get(wiki).stringValue();
	        			items.add( new Item( rssLink(base, revToSubject.get(wiki)), "Wiki modification: " + s + "  "+ wiki.comment, wiki.date.getTime() ) );
	        		}
	        	}
	        	else {
	        		String s = ns.getAbbreviatedURI(revToSubject.get(wiki));
	    			if (s == null)
	    				s = revToSubject.get(wiki).stringValue();
	        		items.add( new Item( rssLink(base, revToSubject.get(wiki)), "Wiki modification: " + s + "  " + wiki.comment, wiki.date.getTime() ) );
	        	}
		        
		        if (subject!= null) {
			        URI talk = new ValueFactoryImpl().createURI( "Talk:"+subject.stringValue() );
			        List<WikiRevision> blogs = Wikimedia.getWikiStorage().getWikiRevisions( talk );
			        for ( WikiRevision blog : blogs )
			        {
			            String content = Wikimedia.getWikiStorage().getWikiContent( talk, blog );
			            int start = content.indexOf( "'''", 3 );
			            if ( start > 0 )
			            {
			                start = start + 3;
			                int end = content.indexOf( "'''", start );
			                if ( end == -1 )
			                    end = content.length();
			                items.add( new Item( x, blog.user + ": " + content.substring( start, end ).trim(), blog.date.getTime() ) );
			            }
			        }
		        }
        
            
        Collections.sort( items );
        
        return items;
        
	}
	
    public static void rss( HttpServletRequest req, Writer out, String _subject, String base ) throws IOException
    {

        URI subject = (_subject == null) ? null : EndpointImpl.api().getNamespaceService().guessURI(_subject);
        String literal = req.getParameter("literal");
        
        List<Item> items = null;
        if (literal != null) {
        	URI data = null;
        	if (literal.contains("^^"))
        		data = ValueFactoryImpl.getInstance().createURI(literal.substring(literal.indexOf("^^") + 2));
        	if (literal.startsWith("\""))
        		literal = literal.substring(1, literal.lastIndexOf("\""));
        	Literal lit = null;
        	if (data != null) 
        		lit = ValueFactoryImpl.getInstance().createLiteral(literal, data);
        	else 
        		lit = ValueFactoryImpl.getInstance().createLiteral(literal);
        	items = getRssItems(null, base, lit);
        }
        else {
        	 items = getRssItems(subject, base, null);
        }
        int counter = 0;
        if (items != null)
	        for ( Item item : items )
	        {   
	        	if ( counter++ > 10 )
	        		break;
	            out.write( "<item><title>"+XML.escape(item.title)+"</title><link>"+XML.escape(item.link)+"</link></item>\r\n" );
	        }
    }
}
