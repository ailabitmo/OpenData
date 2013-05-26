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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.util.Config;

/**
 * map http request to RDF value
 * 
 * @author aeb
 */
public interface RequestMapper
{	
	/**
	 * Initial call which looks up page context's value.
	 */
	public void map( PageContext pc, HttpServletRequest request );
	
	/**
	 * Sets the global context path.
	 */
	public void setContextPath(String contextPath);
	
	/**
	 * Returns the global context path
	 */
	public String getContextPath();
	
    /**
     * Converts a RDF value (URI, blank node or literal) to a request string.
     */
    public String getRequestStringFromValue(Value value);
    
    /**
     * Converts a RDF value to a request string, requesting a specific action 
     * (e.g. "edit"). As specifically requested URIs override session
     * parameters, {@link #getRequestStringFromValue(Value)} should normally be
     * preferred.
     * 
     * @param value
     *            RDF value to address
     * @param action
     *            String id of the requested view
     * @return Browser request string.
     */
    public String getRequestStringFromValueForAction(Value value, String action);

    /**
     * Converts a RDF value to a request string, requesting a specific view to
     * open (e.g. "wiki"). As specifically requested URIs override session
     * parameters, {@link #getRequestStringFromValue(Value)} should normally be
     * preferred.
     * 
     * @param value
     *            RDF value to address
     * @param view
     *            String id of the requested view
     * @return Browser request string.
     */
    public String getRequestStringFromValueForView(Value value, String view);
    
    /**
     * Converts a RDF value to a request string, requesting a specific (wiki)
     * version by adding a "version=..." parameter.
     * 
     * @param value
     *            RDF value to address
     * @param version
     *            String id of the version
     * @return
     */
    public String getRequestStringFromValueForVersion(Value value,
            String version);
	
    /**
     * Convert a URI or literal to a hyperlink.
     */
    public String getAHrefFromValue( Value value );
    
    /**
     * Convert a URI or literal to a hyperlink.
     * 
     * @param value
     * @param isInverse
     * @param tooltip
     * 				tooltip information (html encoded)
     * @return
     */
    public String getAHrefFromValue( Value value, boolean isInverse, String tooltip );
    
    /**
     * Convert a URI or literal to a hyperlink
     * 
     * @param value
     * @param isInverse
     * @param showLabels
     * @param tooltip
     * 				tooltip information (html encoded)
     * @return
     */
    public String getAHrefFromValue( Value value, boolean isInverse, boolean showLabels, String tooltip );
    
    /**
     * Create a hyperlink to the resource specified by value using the provided label. 
     * 
     * Note: the plain label is return if value is not a {@link Resource} and literals
     * shall not be rendered as links (see com.fluidops.iwb.util.Config.getConfig().getLinkLiterals()) 
     * 
     * Both label and tooltip will get html encoded
     * 
     * @param value
     * @param label the label (not html encoded)
     * @param tooltip a tooltip or null
     * 
     * @return
     * 			a hyperlink for the resource or the plain label
     */
    public String getAHref( Value value, String label, String tooltip );
    
    
    /**
     * Create a hyperlink to the resource specified by value using the provided label. 
     * 
     * Note: the plain label is return if value is not a {@link Resource} and literals
     * shall not be rendered as links (see com.fluidops.iwb.util.Config.getConfig().getLinkLiterals()) 
     * 
     * IMPORTANT: the label has to be urlEncoded, use {@link StringEscapeUtils#escapeHTML(String)}
     * 
     * @param value
     * @param encodedLabel the (html encoded) label
     * @param encodedTooltip a (html encoded) tooltip or null
     * 
     * @return
     * 			a hyperlink for the resource or the plain label
     */
    public String getAHrefEncoded( Value value, String encodedLabel, String encodedTooltip );
    
    
    
	/**
	 * Convert a request to a URI or literal.
	 */
    public Value getValueFromRequest(HttpServletRequest request);

    /**
     * "Un-wikifies" the given text, i.e. unescapes strings like the following:
     * "Frankfurt_%2cMain" to "Frankfurt, Main"
     */
    public String unWikify( String t );    
    
    /** 
     * 
     * @param name
     * @return a normalized name
     * When generating URIs from names, we try to follow best practices such that
     * - we assure compatibility with other tools
     * - avoid URI chaos and thus making integration easier
     * - make it easier for a user to use short names   
     * The best practices are derived from the behavior in MediaWiki, DBpedia and the like
     * 
     * Best practices include:
     * - do not use whitespace in URIs, replace with _ (see MediaWiki)
     * - encode special characters
     * - (to be completed)
     */
    public String normalize(String name);
    
    /** 
     * Mediawiki redirects in some cases automatically to pages with default
     * naming conventions, e.g. whitespace is replaced with underscore
     * we try to imitate this behaviour
     **/
    public String getRedirect(HttpServletRequest req);
    
    /**
     * Returns a human readable string representation that is unique and can be reconverted to the URI by calling ...
     * @param uri
     * @return
     */
    public String getReconvertableUri(URI uri, boolean encodeLtGt);
    

    /**
     * Returns a complete URL-string built from the passed request and the
     * path-string. Since this URL is built from the request, the client that
     * sent the request should be able to access with the returned URL a
     * resource of the same web-application. The URL is built in the following
     * way:
     * {@code  [protocol from request]://[server name from request]:[server port from request]/[context path from request]/path}
     * 
     * @param request
     *            The request the returned URL is built from
     * @param path
     *            The path (after context-path) the returned URL should refer to
     * @return a complete URL referencing a resource of the web-application
     */
    String getExternalUrl(HttpServletRequest request, String path);
    
    /**
     * Same as {@link #getExternalUrl(HttpServletRequest, String)}, except that
     * {@code path} is assumed to already contain a context path (of a
     * potentially different web application). So the context path of the
     * request is simply left out, when building the URL.
     * 
     * @param request
     *            The request the returned URL is built from
     * @param path
     *            The path (including context-path) the returned URL should refer to
     * @return a complete URL referencing a resource of web-application on the same server
     */
    String getExternalUrlWithoutContext(HttpServletRequest request, String path);
    
    /**
     * Returns a complete URL that can be used by the server to access itself.
     * This returns the same as
     * {@link #getExternalUrl(HttpServletRequest, String)}, if
     * {@link Config#getInternalHostUrl()} is empty. Otherwise the URL is built in
     * the following way:
     * {@link Config#getInternalHostUrl()}{@code /[context path from request]/path}
     * 
     * @param request
     *            The request the returned URL is built from
     * @param path
     *            The path (after context-path) the returned URL should refer to
     * @return a complete URL referencing a resource of the web application
     */
    String getInternalUrl(HttpServletRequest request, String path);

    /**
     * Returns a URL that can be used by the server to locate itself. The
     * returned URL does not contain a context-path, but just the plain server
     * locator: {@code [protocol]://[server name]:[server port]} (without
     * trailing '/') If {@link Config#getInternalHostUrl()} is set, that is
     * returned, otherwise the server-locator is read from the request.
     * 
     * @param request
     *            the URL is built from, if {@link Config#getInternalHostUrl()} is
     *            not set.
     * @return A server-locator.
     */
    String getInternalUrlWithoutContext(HttpServletRequest request);
    
    
    /**
     * Returns the URL that can be used to search for a given query, i.e.
     * either a keyword query or a SPARQL query. A valid security token
     * is also attached within this method. By default, searches over structured data 
     * (i.e., not wiki pages)
     * 
     * Example
     * 
     *	/%CP%/search/?q=searchQuery&st=%securityToken%
     * 
     * @param searchQuery
     * 			a keyword or a SPARQL query (NOT url encoded)
     * @return
     */
    public String getSearchUrlForValue(String searchQuery);
    
    /**
     * Returns the URL that can be used to search for a given query, i.e.
     * either a keyword query or a SPARQL query. A valid security token
     * is also attached within this method.
     * searchTargets specify target repositories to search. 
     * Pre-defined values are WIKI (for wiki search), and RDF (for the main semantic data repository). 
     * For specific types of search targets, the class name of the corresponding SearchProvider class has to be given. 
     * If several values are specified, search results over different targets are merged.
     * 
     * Example
     * 
     *	/%CP%/search/?q=searchQuery&st=%securityToken%
     * 
     * @param searchQuery
     * 			a keyword or a SPARQL query (NOT url encoded)
     * @param searchTarget
     * 			List of strings specifying query targets.  
     * @return
     */
    public String getSearchUrlForValue(String searchQuery, List<String> searchTargets);
}