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

package com.fluidops.iwb.widget;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.w3c.dom.Document;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;

/**
 * Shows the YouTube player for a given YouTube video ID or finds items according
 * to a specified search string.
 * 
 * Is also invoked by the suggest() method, if pc.value is of type 
 * {@link Vocabulary#TYPE_MUSICALWORK} and has an outgoing edge with 
 * predicate {@link Vocabulary#ARTIST}.
 * 
 * Example usage:
 * 
 * a) by YouTubeID
 * 
 * <code>
 * {{ #widget: Youtube | youTubeID = 'DSzJs4SAS18' }}
 * </code>
 * 
 * b) by Search String
 * 
 * The search string can be specified either as a keyword sequence
 * 
 * <code>
 * {{
 * #widget: Youtube
 * | searchString = 'Red Hot Chili Peppers'
 * }}
 * </code>
 * 
 * or as a structured SPARQL query
 * 
 * <code>
 * {{
 * #widget: Youtube
 * | searchString = $SELECT ?x WHERE { ?? foaf:name ?x . }$
 * }}
 * </code>
 * 
 * @author aeb, pha, marlon.braun, as, andriy.nikolov
 */
@TypeConfigDoc( "Shows the YouTube player for a given YouTube video ID (such as UDYYxCdOkGk)" )
public class YouTubeWidget extends AbstractWidget<YouTubeWidget.Config>
{
    /**
     * logger
     */
	private static final Logger logger = Logger.getLogger(YouTubeWidget.class.getName());
	
    /**
     * YouTube Widget config class
     * 
     * Combinations of uses:
     * 
     * 1) youTubeID => lookup by youtube id
     * 2) searchString => search for videos, take first video
     * 
     * Note: Preference is indicated by the order of the list, e.g. if both youTubeID 
     * and searchString are specified, only the youTubeID is considered.
     * 
     * @author marlon.braun, as, andriy.nikolov
     */
    public static class Config extends WidgetBaseConfig
    {
		@ParameterConfigDoc(desc = "ID of the video, which is displayed")
        public String youTubeID;
        
		@ParameterConfigDoc(desc = "The search string for videos (optional)")
        public String searchString;
        
    }
    
	
    @Override
    public FComponent getComponent(final String id)
    {
        final Config c = get();
              
        // Set width/height to default values if not configured
        c.width = c.width == null ? "640" : c.width;
        c.height = c.height == null ? "385" : c.height;
       
    	String youTubeID = null;
        
        if (c.youTubeID!=null) {
        	// case 1: use specified YouTube id if mentioned
        	youTubeID=c.youTubeID;
        	
        } else if (c.searchString!=null) {
        	// case 2: use search string to lookup results
        	youTubeID=queryYoutube(c.searchString);	// may return null, check later
        	
        } else {
        	
        	// configuration specified incorrectly
        	 if (youTubeID == null)
                 return WidgetEmbeddingError.getErrorLabel(id, WidgetEmbeddingError.ErrorType.INVALID_WIDGET_CONFIGURATION,
                            "Please specify either youTubeID, a searchString, or predicates in the configuration.");
                  
        }
        
        // Valid configuration requires youTubeID
        if (youTubeID == null)
            return WidgetEmbeddingError.getErrorLabel(id, WidgetEmbeddingError.ErrorType.INVALID_WIDGET_CONFIGURATION,
                            "No valid YouTube ID could be found with your configuration.");
 
        return new FHTML(id, "<iframe class='youtube-player' type='text/html' "
                		+ "width='" + c.width + "' height='" + c.height
                		+ "' src='http://www.youtube.com/embed/" + youTubeID
                		+ "' frameborder='0'></iframe>");
    }

    @Override
    public String getTitle()
    {
        return "YouTube";
    }

    @Override
    public Class<?> getConfigClass()
    {
        return YouTubeWidget.Config.class;
    }
 
    
    /**
     * Query YouTube for the provided keyword query string, return the best match (or null)
     * 
     * @param query
     * 			the keyword query (may contain spaces)
     * @return
     * 			the best match or null
     */
    protected String queryYoutube(String query) {
    	try {
    		// replace spaces and underscore (for URL)
    		// TODO use URL encoding here (e.g. if other UTF characters occur?
    		query = query.replaceAll(" ", "+").replaceAll("_", "+");
    		
	    	URL url;
	        url = new URL("http://gdata.youtube.com/feeds/api/videos?q="
	                + query + "&start-index=1&max-results=1&v=2");
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	
	        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
	        {
	            InputStream stream = conn.getInputStream();
	
	            Document doc = null;
	
	            doc = DocumentBuilderFactory.newInstance()
	                    .newDocumentBuilder().parse(stream);
	            XPathFactory factory = XPathFactory.newInstance();
	            XPath xpath = factory.newXPath();
	            XPathExpression expr = xpath.compile("//videoid/text()");
	            Object result = expr.evaluate(doc, XPathConstants.STRING);
	
	            String text = (String) result;
                if (text.length() > 0)
                	return text;
	        }
    	} catch (Exception e) {
    		 logger.warn(e.getMessage(), e);
    	}
    	
        return null;
    }
    
    
    /**
     * Create a URI from the string using the namespace service
     * 
     * @param s
     * @return
     */
    protected URI createURI(String s) {
    	// TODO add this logic to the namespace service directly to simplify code
    	if (s.startsWith("http://"))
    		return ValueFactoryImpl.getInstance().createURI(s);
    	return EndpointImpl.api().getNamespaceService().guessURI(s);
    }
}
