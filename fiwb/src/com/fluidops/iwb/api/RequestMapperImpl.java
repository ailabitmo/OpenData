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

import static com.fluidops.iwb.util.Config.getConfig;
import static com.fluidops.util.StringUtil.isNullOrEmpty;
import static com.fluidops.util.StringUtil.urlEncode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.keywordsearch.SearchProviderFactory;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.user.UserManagerImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;


public class RequestMapperImpl implements RequestMapper
{
	private static final Logger logger = Logger.getLogger(RequestMapperImpl.class.getName());
	
    static final String LITERAL_PARAM = "literal";

    static final String URI_PARAM = "uri";
    
    static final String BNODE_PARAM = "bnode";

    static String contextPath = "";

    
    @SuppressWarnings(value = { "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" }, justification = "The context path is set only once during startup.")
    public void setContextPath(String contextPath) 
    {
        RequestMapperImpl.contextPath = contextPath;
    }
    
    public String getContextPath()
    {
        return contextPath;
    }


    private String getRequestStringFromValue(Value value, String prefix,
			String view, String version) {
		return getRequestStringFromValue(value, prefix, view, version, null);
	}
    
    private String getRequestStringFromValue(Value value, String prefix,
            String view, String version,String action)
    {
        String argAdd = null;
        if (view != null)
            argAdd = "view=" + view;
        if (version != null)
        {
            argAdd = argAdd == null ? "" : argAdd + "&";
            argAdd += "version=" + version;
        }
        if (action != null) {
        	argAdd = argAdd == null ? "" : argAdd + "&";
        	argAdd = "action=" + action;
        }
        // literals are passed using parameter 'literal' using URL encoding
        if (value instanceof Literal)
        {
            Literal literal = (Literal)value;
            String literalStr = "\"" + literal.getLabel() + "\"";
            if (literal.getDatatype()!=null)
                literalStr+="^^" + literal.getDatatype();
            if (literal.getLanguage()!=null)
                literalStr+="@" + literal.getLanguage();
            // Some browsers cause problems with extremely long URL parameters
            // For these cases, we simple link to the empty literal, 
            // as they do not allow useful browsing anyway 
            if (literalStr.length()>255)
                literalStr = "";
            
            /*
             * Double decoding is required to avoid alarms in the XSS filter
             * when dealing with the URL:
             */
            String ret = contextPath + prefix + "?" + RequestMapperImpl.LITERAL_PARAM + "="
                    + urlEncode(urlEncode(literalStr))
                    + (argAdd == null ? "" : "&" + argAdd);
            return ret;
        }
        
        // for URIs we distinguish two cases: if we succeed in
        // encoding the URI as a short name (either using the
        // default or any other namespace), then we simply append
        // the URI, otherwise, we use parameter 'uri' and URL encoding
        else if ( value instanceof URI )
        {
            // try to encode
            URI uri = (URI)value;
            String shortName = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(uri);
            
            if (shortName == null) {
                String ret = contextPath + prefix + "?" + RequestMapperImpl.URI_PARAM + "="
                        + urlEncode(uri.stringValue())
                        + (argAdd == null ? "" : "&" + argAdd);
                return ret;
            }
            else
            {
                    return (contextPath + prefix + shortName + (argAdd == null ? ""
                        : "?" + argAdd));
            }
            
        } else if (value != null) {
            // TODO: The following scheme may need to be reconsidered
            String ret = contextPath + prefix + "?" + RequestMapperImpl.BNODE_PARAM + "="
            + urlEncode(value.stringValue())
            + (argAdd == null ? "" : "&" + argAdd);
            return ret;
        }
        else {
        	return null;
        }
    }
    
    
    @Override
    public String getRequestStringFromValue(Value value)
    {
        return getRequestStringFromValue(value, Config.getConfig()
                .getUrlMapping(), null, null);
    }

    @Override
    public String getRequestStringFromValueForView(Value value, String view)
    {
        return getRequestStringFromValue(value, Config.getConfig()
                .getUrlMapping(), view, null);
    }
    
    @Override
    public String getRequestStringFromValueForVersion(Value value, String version)
    {
        return getRequestStringFromValue(value, Config.getConfig()
                .getUrlMapping(), null, version);
    }
    
	@Override
	public String getRequestStringFromValueForAction(Value value, String action) {
		return getRequestStringFromValue(value, Config.getConfig()
				.getUrlMapping(), null, null, action);
	}
	

	void updateTitle(PageContext pc)
	{
	    ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
	    String term = dm.getLabelHTMLEncoded(pc.value);
	    pc.title = term;
	}
	
    @Override
    public void map(PageContext pc, HttpServletRequest request)
    {
        pc.value = getValueFromRequest(request);

        // Redirect if requested page is invalid URI
        if (pc.value == null)
            pc.value = ValueFactoryImpl.getInstance().createURI(
                    "http://www.fluidops.com/Error/Invalid_URI_Request");
        updateTitle(pc);
    }

	
	@Override
    public Value getValueFromRequest(HttpServletRequest request) {

        // try to read uri from parameters
        String uri = request.getParameter(RequestMapperImpl.URI_PARAM);

        if (uri != null)
        {
            if (StringUtil.containsNonIriRefCharacter(uri,false))
                return null;
            ValueFactoryImpl f = new ValueFactoryImpl();

            return f.createURI(uri);
        }
            
        // try to read literal from parameters
        String literal = request.getParameter(RequestMapperImpl.LITERAL_PARAM);
        if (literal != null && !literal.equals("")) {
            
            try
            {
                literal = URLDecoder.decode(literal, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            
            // we deal with possibly typed literals, so we need
            // to split the string into components first; note:
            // either we have a datatype OR a language tag OR none
            // of them, but never both at a time
            String label = null;
            String datatype = null;
            String languageTag = null;
            int indexOfDatatypeSep = literal.lastIndexOf("^^"); 
            int indexOfLanguageSep = literal.lastIndexOf("@");

            if (indexOfDatatypeSep!=-1)
            {
                label = literal.substring(1,indexOfDatatypeSep-1); // cut leading/final "
                datatype = literal.substring(indexOfDatatypeSep+2);
            }
            else if (indexOfLanguageSep!=-1 && indexOfLanguageSep != 1 ) // -1 = @ not found, 1 = @ is first char after "
            {
                label = literal.substring(1,indexOfLanguageSep-1); // cut leading/final "
                languageTag= literal.substring(indexOfLanguageSep+1);                
            }
            else 
            {
                label = literal.substring(1,literal.length()-1);
            }
            
            Literal l = null;
            if (datatype!=null)
            {
                // construct literal with datatype:
                l = new ValueFactoryImpl().createLiteral(label,new ValueFactoryImpl().createURI(datatype));
            }
            else if (languageTag!=null)
            {
                // construct literal with language tag:
                l = new ValueFactoryImpl().createLiteral(label,languageTag);                
            }
            else
            {
                // construct plain literal:
                l = new ValueFactoryImpl().createLiteral(label);
            }
            return l;
        }

        String bnode = request.getParameter(RequestMapperImpl.BNODE_PARAM);
        if (bnode != null && !bnode.equals("")) 
        {
        	ValueFactoryImpl f = ValueFactoryImpl.getInstance();
        	return f.createBNode( bnode );
        }
        
        // if both methods did not succeed we have a URI from the
        // default namespace or in prefix notation
        String requestURI = request.getRequestURI();
        try
        {
            requestURI = URLDecoder.decode(requestURI, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            logger.error(e.getMessage(), e);
        }
        
        if (StringUtil.containsNonIriRefCharacter(requestURI,false))
            return null;

        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String shortUri = "";
        if (requestURI.length() > contextPath.length()+servletPath.length())
        	shortUri = requestURI.substring(contextPath.length()+servletPath.length() + 1);
        else
        	shortUri = "";
  
        // this is actually not necessary: the browser does its own encoding
        // and requests that pass our internal short URI encoding scheme
        // should not contain any problemantic characters anyway
//        try
//        {
//            shortUri = URLDecoder.decode(shortUri,"UTF-8");
//        }
//        catch (Exception e)
//        {
//        }

        URI longUri = EndpointImpl.api().getNamespaceService().getFullURI(shortUri);
        return longUri;

	}


    // TODO: remove this one?
    @Override
    public String unWikify( String t )
    {
        try
        {
            String decode = URLDecoder.decode(t, "UTF-8");
            decode = decode.replace('_', ' ');
            return decode;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    // TODO: remove this one?
    @Override
    public String normalize(String name)
    {
        name = name.replaceAll(" ", "_");
        
        try
        {
            name = URLEncoder.encode(name,  "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            logger.error(e.getMessage(), e);
        }
        
        return name; 
    }
    
    @Override
    public String getRedirect(HttpServletRequest req)
    {
        String prefix = req.getContextPath() + Config.getConfig().getUrlMapping();

        String uri = req.getParameter("uri");

        // we only redirect for short names, URI is always taken as is
        if (uri != null)
            return null;

        String requestURI = req.getRequestURI();

        String name;

        if (requestURI.length() < prefix.length())
            name = prefix;
        else
            name = requestURI.substring(prefix.length());

        String redirect = name.replaceAll("%20", "_");
        if(!name.equals(redirect)) 
            return redirect;

        return null;
    }
    

    public String getReconvertableUri(URI uri, boolean encodeLtGt) 
    {
    	String str = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(uri);
		if (str == null)
			str = encodeLtGt ? "&lt;" + uri + "&gt;" : "<" + uri + ">";
		
		return str;
    }

    @Override
    public String getAHrefFromValue(Value value, boolean isInverse, boolean showLabels, String tooltip)
    {
        ReadDataManagerImpl dm = ReadDataManagerImpl.getDataManager(Global.repository);
        String label = ""; 
        String link = "";
        if (isInverse)
        {
            // we try to fetch the inverse property
            URI inverse = null;
            if(value instanceof URI)
                inverse = dm.getInverseProp((URI)value);
                
            if (inverse==null)
            {
                label = showLabels?dm.getLabel(value):value.stringValue();
                link = getRequestStringFromValue(value);
                
                // if inverse label is not defined, we distinguish two cases:
                // (1) the property ends with "of", then we cut it
                if (label.endsWith("of") || label.endsWith("Of"))
                    label = label.substring(0,label.length()-2);
                // (2) otherwise, we append an "of" as suffix
                else
                    label += " of";
            }
            else
            {
                link = getRequestStringFromValue(inverse);
                label = showLabels?dm.getLabel(inverse):inverse.stringValue();
            }
        }
        else
        {
            link = getRequestStringFromValue(value);
            label = showLabels?dm.getLabel(value):value.stringValue();
        }                    

        return getAHref(link, StringEscapeUtils.escapeHtml(label), tooltip);
    }
    
    
    @Override
    public String getAHrefFromValue(Value value, boolean isInverse, String tooltip )
    {
        return getAHrefFromValue(value, isInverse, true, tooltip);
    }
    
    @Override
    public String getAHrefFromValue(Value value)
    {
        return getAHrefFromValue(value, false, true, null);
    }
    
    @Override
	public String getAHref(Value value, String label, String tooltip)
	{
    	return getAHrefEncoded(value, StringEscapeUtils.escapeHtml(label), 
    			tooltip!=null ? StringEscapeUtils.escapeHtml(tooltip) : null );
	}
    
    @Override
	public String getAHrefEncoded(Value value, String encodedLabel, String encodedTooltip)
	{
    	if (value instanceof Resource || com.fluidops.iwb.util.Config.getConfig().getLinkLiterals())
    		return getAHref(getRequestStringFromValue(value), encodedLabel, encodedTooltip);
    	else
    		return encodedLabel;
	}
    
    /**
     * Build the link from encoded information
     * @param link
     * @param encodedLabel
     * @param encodedTooltip
     * @return
     */
	protected String getAHref(String link, String encodedLabel,
			String encodedTooltip)
    {
        return "<a "
           + ((encodedTooltip == null) ? "" : "title=\"" + encodedTooltip + "\"")
           + " href=\"" + link + "\">" + encodedLabel
           + "</a>";
	}
    
    @Override
    public String getInternalUrlWithoutContext(HttpServletRequest request) {
        String hostUrl = getConfig().getInternalHostUrl();
        return isNullOrEmpty(hostUrl) ? hostUrl(request) : hostUrl;
    }
    
    @Override
    public String getInternalUrl(HttpServletRequest request, String path) {
        assert path.startsWith("/") : path;
        return getInternalUrlWithoutContext(request) + request.getContextPath() + path;
    }
    
    @Override
    public String getExternalUrl(HttpServletRequest request, String path) {
        return externalUrlForContext(request, request.getContextPath(), path); 
    }

    @Override
    public String getExternalUrlWithoutContext(HttpServletRequest request, String path) {
        return externalUrlForContext(request, "", path); 
    }
    
    private String externalUrlForContext(HttpServletRequest request, String contextPath, String path)
    {
        assert path.startsWith("/") : path;
        return hostUrl(request) + contextPath + path;
	}
    
    private String hostUrl(HttpServletRequest request)
    {
    	return String.format("%s://%s:%s", request.getScheme(), request.getServerName(), request.getServerPort());
    }

	@Override
	public String getSearchUrlForValue(String searchQuery)
	{
		return getSearchUrlForValue(searchQuery, SearchProviderFactory.getDefaultQueryTargets());
	}

	@Override
	public String getSearchUrlForValue(String searchQuery, List<String> queryTargets)
	{
        StringBuilder res = new StringBuilder();
        res.append(getContextPath());
        res.append("/search/?q=").append(StringUtil.urlEncode(searchQuery));
        for(String queryTarget : queryTargets)
        	res.append("&queryTarget=").append(StringUtil.urlEncode(queryTarget.toString()));
        res.append("&st=").append(
                StringUtil.urlEncode(UserManagerImpl
                        .generateSecurityToken(searchQuery)));
        return res.toString();
	}
}
