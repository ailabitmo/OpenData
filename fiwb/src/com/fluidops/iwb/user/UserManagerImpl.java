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

package com.fluidops.iwb.user;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.api.misc.ApiMethod;
import com.fluidops.api.security.SHA512;
import com.fluidops.api.security.SessionContext;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.server.HybridSearchServlet;
import com.fluidops.iwb.server.SparqlServlet;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.StringUtil;
import com.fluidops.util.user.UserContext;

/**
 * Default implementation of the IWB user management. Grants almost
 * full rights to every user. The ACL user approach is implemented
 * in fiwbcom, class AclUserManagerImpl (which in parts makes use
 * of convenience methods defined here, such as user name and URI access).
 * 
 * @author msc
 */
public class UserManagerImpl implements UserManager
{	
    protected static final List<String> emptyRoles = Collections.<String>emptyList();
	
    private static final Logger logger = Logger
            .getLogger(UserManagerImpl.class.getName());
    
    /**
	 * Convenience method to determine if user has query privileges, which can
	 * be used from servlets, i.e. {@link SparqlServlet} or {@link HybridSearchServlet}.
	 * 
	 * Informally the user is allowed to submit a query if either a valid
	 * security token is provided (only SELECT/CONSTRUCT), or the user
	 * has access on ACL level. Note that regardless of ACL access rights
	 * the endpoint can be disabled using {@link Config#getPublicSparqlEndpointEnabled()}.
	 * 
	 * Formally access is granted, if one of the following holds
	 * a) query is null or empty, or query type is null (e.g. keyword query)
	 * b) {@link Config#getServletSecurityKey()} is null or empty (= default for IWB CE)
	 *    and query is not UPDATE or ASK 
	 * c) securityToken matches the hash generated from query and {@link Config#getServletSecurityKey()}
	 *    and query is not UPDATE or ASK
	 * d) access has been granted on ACL level (default is true in IWB CE) and 
	 *    {@link Config#getPublicSparqlEndpointEnabled()} is true
	 * 
	 * @param query
	 * @param qt
	 * @param securityToken the security token (as-is) from the request, is URL decoded internally
	 * @return
	 */
    @Override
	public boolean hasQueryPrivileges(String query, SparqlQueryType qt, String securityToken) {
		
		if (StringUtil.isNullOrEmpty(query) || qt==null)
			return true;
		
        if (securityToken!=null) 
			securityToken = StringUtil.urlDecode(securityToken);
        
        // is token-based access enabled and the token valid?
		boolean hasTokenBasedAccess = false;
		if (qt==SparqlQueryType.SELECT || qt==SparqlQueryType.CONSTRUCT)
			hasTokenBasedAccess = tokenBasedAccessEnabled(query, securityToken);
				
		if (hasTokenBasedAccess)
			return true;
		
		if (!Config.getConfig().getPublicSparqlEndpointEnabled())
			return false;
		
		// return true if the user has ACL privileges
		return hasQueryRight(qt, null);
	}
	
	/**
	 * Convenience method to generate a security token using a
	 * SHA512 hash, the password is retrieved from
	 * {@link Config#getServletSecurityKey()}
	 * 
	 * @param query
	 * @return
	 */
	public static String generateSecurityToken(String query) {
		
		String tokenBase = Config.getConfig().getServletSecurityKey() + query;
		try	{
			return SHA512.encrypt(tokenBase);
		} catch (Exception e) {
			throw new RuntimeException("Error while generating security token.", e);
		}         
	}
	
	@Override
    public boolean hasValueAccess(Value v, ValueAccessLevel al)
    {
		return true;
    }
    
	@Override
    public ValueAccessLevel getValueAccessLevel(Value v)
    {
		return ValueAccessLevel.WRITE; // full access
    }

    
	@Override
    public boolean hasAPIAccess(ApiMethod apiMethod, SessionContext sc)
    {
		return true;
    }
    
	@Override
    public boolean hasServletAccess(Class<?> servletClass, SessionContext sc)
    {
    	return true;
    }
    
	@Override
    public boolean hasWidgetAccess(Class<?> widgetClass, SessionContext sc)
    {
    	return true;
    }
    
	@Override
    public boolean hasUIComponentAccess(UIComponent component, SessionContext sc)
    {
    	return true;
    }
    
	@Override
    public boolean hasFileAccess(String file, SessionContext sc, boolean enforceExists)
    {
		File f = new File(file);
		try {
			// allow file access to the wiki export storage for download
			if (WikiStorageBulkServiceImpl.getWikiExportStorageFolder().getCanonicalFile().equals(f.getCanonicalFile().getParentFile()))
				return true;
			// allow file access to the upload folder for download
			if (IWBFileUtil.getUploadFolder().getCanonicalFile().equals(f.getCanonicalFile().getParentFile()))
				return true;
		} catch (IOException e) {
			logger.debug("File access cannot be checked: " + e.getMessage());
			return false;
		}
		// TODO: think about this, e.g. for config directory
		// for security reasons, no file access via UI is enabled at all
		return false;
    }

	@Override
    public boolean hasQueryRight(SparqlQueryType qt, SessionContext sc)
    {
		// in IWB CE all query types are allowed
		// TODO think about this (shall we allow UPDATE in general?)
    	return true;
    }
    
	@Override
    public boolean isLoggedIn(SessionContext sc)
    {	
    	UserContext c = UserContext.get();
    	if (c==null || StringUtil.isNullOrEmpty(c.name))
    		return false;
 
    	return true;
    }
    
	@Override
    public String getUser(SessionContext sc)
    {
    	if (sc==null)
    	{
	    	UserContext c = UserContext.get();
	    	return c==null || StringUtil.isNullOrEmpty(c.name) ?
	    			GUEST_USER_NAME:c.name;
    	}
    	else
    	{
    		return sc.getUser();
    	}
    }
    
	@Override
    public List<String> getRoles(SessionContext sc)
    {
		return emptyRoles;
   	}
     
	@Override
    public URI getUserURI(SessionContext sc)
    {
    	String user = getUser(sc);
    	if (StringUtil.isNullOrEmpty(user))
    		return Vocabulary.SYSTEM.UNKNOWNUSER;
    	else
    	{
    		user = removeDomain(user);
    		return ValueFactoryImpl.getInstance().createURI(
    				EndpointImpl.api().getNamespaceService().systemNamespace() 
    				+ StringUtil.replaceNonIriRefCharacter(user, '_'));
    	}
    }
	
	@Override
    public String getUserName(SessionContext sc)
    {
    	String user = getUser(sc);
    	user = removeDomain(user);
    	return user;
    }

	@Override
    public boolean tokenBasedAccessEnabled(String query, String securityToken)
    {
        if (StringUtil.isNullOrEmpty(query) 
         || StringUtil.isNullOrEmpty(com.fluidops.iwb.util.Config.getConfig().getServletSecurityKey()))
            return true; // endpoint not protected
        else
        {
            try
            {
                String tokenBase = com.fluidops.iwb.util.Config.getConfig().getServletSecurityKey() + query;
                String hash = SHA512.encrypt(tokenBase);
                
                if (hash.equals(securityToken))
                    return true;
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(),e);
            }
        }
        
        return false; // no access
    }

	/**
	 * removes domain prefix or suffix from a user name
	 * 
	 * @param user
	 * @return
	 */
    protected String removeDomain(String user)
    {
    	// cut away domain prefix or suffix
    	if (user.contains("\\"))
    		user = user.substring(user.indexOf('\\')+1);
    	else if (user.contains("@"))
    		user = user.substring(0,user.indexOf('@'));
    	return user;
    }

	@Override
	public boolean hasEditorialWorkflowAccess(EditorialWorkflow action,
			SessionContext sc) {
		// In the Community Edition we do not support editorial workflows
		return false;
	}

	@Override
	public boolean hasComponentAccess(String componentType,
			String componentName, SessionContext sc)
	{
		// By default we allow access to everything
		return true;
	}
}
