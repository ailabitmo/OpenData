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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.URI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fluidops.iwb.wiki.WikiStorage.WikiRevision;
import com.fluidops.util.StringUtil;

public class WikiBot
{

	private HttpClient httpClient;
	private boolean expandTemplates = false;
	/**
	 * The wikimedia source URL, e.g. http://en.wikipedia.org/w/
	 */
	private final String wikimediaSource;
	private static final String WIKIBOT_USER_AGENT = "Information Workbench Wikibot";
	
	public static final String WIKIBOT_WIKIREVISION_COMMENT = "Imported by Wikibot";

	/**
	 * 
	 * @param wikimediaSource the wikimedia url, e.g. http://en.wikipedia.org/w/
	 */
	public WikiBot(String wikimediaSource) {
		this.wikimediaSource = wikimediaSource;
		httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
	}
	
	/**
	 * Retrieve the requested wiki page using the media wiki API and store
	 * in local wiki storage. 
	 * 
	 * @param ws
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public String loadAndStoreWikiPage(WikiStorage ws, URI name)
			throws Exception {

		String localName = StringUtil.urlDecode(name.getLocalName());

		String content = retrieveWikiContent(localName);
		if (StringUtil.isNullOrEmpty(content))
			return null;

		WikiRevision wr = new WikiRevision();
		wr.date = new Date();
		wr.user = "wikibot";
		wr.comment = WIKIBOT_WIKIREVISION_COMMENT;
		wr.security = "ALL/ALL";
		wr.size = content.length();
		
		ws.storeWikiContent(name, content, wr);
		return content;
	}
		
		
	/**
	 * Execute a wikimedia query asking for the latest content of the
	 * given wikipedia page. If {@link #expandTemplates} is set, the
	 * templates within the content are automatically expanded
	 * 		
	 * @param wikiPage
	 * @return
	 */
	private String retrieveWikiContent(String wikiPage) throws IOException, Exception {
		
		PostMethod method = new PostMethod(wikimediaApiURL());
		method.addRequestHeader("User-Agent", WIKIBOT_USER_AGENT);
		method.addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		method.addParameter("action", "query");
		method.addParameter("prop", "revisions");
		method.addParameter("rvprop", "ids|content");
		if (expandTemplates)
			method.addParameter("rvexpandtemplates", "");			
		method.addParameter("tlnamespace", "0");
		method.addParameter("format", "xml");
		method.addParameter("redirects", "");
		method.addParameter("titles", wikiPage.replace(" ", "_"));

		try {
			int status = httpClient.executeMethod(method);
	
			if (status== HttpStatus.SC_OK) {
				return parsePage(method.getResponseBodyAsStream());
			}	
			
			throw new IOException("Error while retrieving wiki page " + wikiPage + ": " + status + " (" + method.getStatusText() + ")");
		} finally {
			method.releaseConnection();
		}
	}
		
				
	/**
	 * @return the fully constructed url to the wikimedia API,
	 *            e.g http\://en.wikipedia.org/w/api.php
	 */
	protected String wikimediaApiURL() {
		return wikimediaSource + "api.php";
	}
		
	
	/**
   	 * InputStream contains a document with at least one page
   	 * 
   	 * <code>
   	 * ...
   	 * <page pageid="15580374" ns="0" title="Main Page">
   	 *  	<revisions>
   	 *  		<rev revid="464887589" parentid="447996010" timestamp="2011-12-09T03:08:20Z"
   	 *  				xml:space="preserve">
   	 *  		Content
   	 *  		</rev>
   	 *  	</revisions>
   	 * </page>
   	 * ...
   	 * </code>
   	 * 
   	 * Content is html decoded
   	 * 
   	 * @param page
   	 * @return
   	 * @throws IOException 
   	 * @throws SAXException 
   	 */
   	private String parsePage(InputStream in) throws Exception {
   		
   		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
   		Document d = documentBuilder.parse(in);
		
		NodeList pages = d.getElementsByTagName("page");
		
		if (pages==null || pages.getLength()==0)
			throw new IllegalArgumentException("InputStream does not have the expected 'page' section");

   		NodeList revs = getChild(pages.item(0), "revisions").getElementsByTagName("rev");
   		
   		if (revs.getLength()!=1)
   			throw new IllegalArgumentException("Page does not have the expected structure, exactly one revision expected.");
   		
   		Element rev = (Element)revs.item(0);

   		return StringEscapeUtils.unescapeHtml(rev.getTextContent());
   		
   	}
	 
   	/**
   	 * retrieves the child with name nodeName, throws an 
   	 * Exception if there is no such element
   	 * 
   	 * @param node
   	 * @param nodeName
   	 * @return
   	 */
   	private Element getChild(Node node, String nodeName) {
   		NodeList childNodes = node.getChildNodes();
   		for (int i=0; i<childNodes.getLength(); i++)
   			if (childNodes.item(i).getNodeName().equals(nodeName))
   				return (Element)childNodes.item(i);
   		throw new IllegalArgumentException("No such element in node: " + nodeName);
   	}
}
