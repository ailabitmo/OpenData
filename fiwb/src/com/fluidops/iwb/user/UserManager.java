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

import java.rmi.Remote;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.api.misc.ApiMethod;
import com.fluidops.api.security.SessionContext;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.server.HybridSearchServlet;
import com.fluidops.iwb.server.SparqlServlet;

/**
 * The (new) IWB User Management Interface. Wraps the fbase user
 * management functionality, by interpreting roles etc.
 * 
 * @author msc
 */
public interface UserManager extends Remote
{	
	// below we define the default roles for IWB;
	// they do not have a semantics in IWB CE, but are interpreted in IWB EE
    /**  user role  */
    public static String USER_ROLE = "user";
	
    /** the guest role */
    public static String GUEST_ROLE = "guest";
    
    /** the admin role */
    public static String ADMIN_ROLE = "admin";
	
	/** display name of guest users */
    public static String GUEST_USER_NAME = "guest";


    
    public static enum UIComponent
    {
    	// top menu header buttons
    	HEADER_WWW_BUTTON("btn_www"),
    	HEADER_ECMHOME_BUTTON("btn_ecmhome"),
       	HEADER_USERHOME_BUTTON("btn_userhome"),
      	HEADER_ECMENT_BUTTON("btn_ecment"),
      	HEADER_ECMIFS_BUTTON("btn_ecmifs"),
      	HEADER_TYPE_BUTTON("btn_type"),   
      	HEADER_FILEUPLOAD_BUTTON("btn_fileupload"),
    	HEADER_NOTIFY_BUTTON("btn_notify"),       	
    	HEADER_ADMIN_BUTTON("btn_admin"),
      	HEADER_PRINT_BUTTON("btn_print"),
      	HEADER_HELP_BUTTON("btn_help"),
    	HEADER_EVENTS_BUTTON("btn_events"), 
    	HEADER_WIZARDS_BUTTON("btn_wizards"), 
    	HEADER_JOBS_BUTTON("btn_jobs"),
    	
      	// left-hand navigation bar
      	NAVIGATION_WIKI("nav_wiki"),
      	NAVIGATION_TABLE("nav_table"),
      	NAVIGATION_GRAPH("nav_graph"),
      	NAVIGATION_PIVOT("nav_pivot"),
      	
    	// wiki functionality buttons
    	WIKIVIEW_REVISIONDELETE_BUTTON("btn_revisiondelete"),
    	WIKIVIEW_REVISIONRESTORE_BUTTON("btn_revisionrestore");
    	
        private String name;
        
        private UIComponent(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
    }
    
    public static enum EditorialWorkflow
    {

    	// Editorial workflow
    	APPROVE("Approve"),
    	REJECT("Reject"),
    	BACK_TO_DRAFT("BackToDraft"),
    	PUBLISH("Publish");
    	
        private String name;
        
        private EditorialWorkflow(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
    }
    
    
    /**
     * Resource access levels:
     * - READ: user is allowed to read the resource
     * - WRITE_LIMITED: user has READ access to the resource + limited write (no widgets)
     * - WRITE_FULL: user has full access to the resource page (modulo widgets etc. that may be blocked)
     */
    public enum ValueAccessLevel
    {
    	// attention: do not change order!!!
        READ("read"),
        WRITE_LIMITED("write_limited"),
        WRITE("write");
        
        private String name;
        
        private ValueAccessLevel(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
    };
    
    /**
     * Checks whether the user has (at least) the passed value
     * access level (order: READ < WRITE_LIMITED < WRITE).
     * 
     * @param the value to check
     * @param the access type to verify
     * @return true if and only if the user has access
     *          to the given value according to the access rules
     */
    public boolean hasValueAccess(Value v, ValueAccessLevel al);
    
    /**
     * Returns the maximum access level for the user and the
     * specified value.
     * 
     * @param the value to check
     * @param the access type to verify
     * @return true if and only if the user has access
     *          to the given value according to the access rules
     */
    public ValueAccessLevel getValueAccessLevel(Value v);
    
    /**
     * To be called prior to executing an API method. If the user
     * does not have access to the API, then an exception will be
     * thrown.
     * 
     * @param ApiMethod the api method to check
 	 * @param sc optional context to register against, if null user context is used
     * @return true if and only if the user has access
     *          to the given API according to the access rules
     */
    public boolean hasAPIAccess(ApiMethod apiMethod, SessionContext sc);
    
    /**
     * To be called in each servlet prior to delivering data.
     * Not sure if we want to have it, but it may be a usefool
     * tool to disable some servlets for users (e.g., RDF Export).
     * However, components that rely on the servlets may break.
     * Therefore, you should use this feature with care and
     * basically allow all the components to every user.
     * 
     * @param servletClass the servlet class
 	 * @param sc optional context to register against, if null user context is used
 	 * @return true if and only if user has access to the
 	 * 			the given servlet class according to the access rules
     */
    public boolean hasServletAccess(Class<?> servletClass, SessionContext sc);
    
    /**
     * Checks if the user has access to some widget.
     * 
     * @param widgetClass the widget class
 	 * @param sc optional context to register against, if null user context is used
     * @return true if and only if the user has access
     *          to the given API according to the access rules
     */
    public boolean hasWidgetAccess(Class<?> widgetClass, SessionContext sc);

    /**
     * Checks if the user has access to some UI component (such as a button).
     * 
     * @param component the UI component
 	 * @param sc optional context to register against, if null user context is used
     * @return true if and only if the user has access
     *          to the given API according to the access rules
     */
    public boolean hasUIComponentAccess(UIComponent component, SessionContext sc);
    
    /**
     * Checks if the user has the right to perform some action in the editorial workflow (such as approve, publish).
     * 
     * @param action in the editorial workflow
 	 * @param sc optional context to register against, if null user context is used
     * @return true if the user the right
     */
    public boolean hasEditorialWorkflowAccess(EditorialWorkflow action, SessionContext sc);
    
    
    /**
     * Checks if the user has access to some file residing on the server.
     * 
     * @param file the file name (including path) on the server
 	 * @param sc optional context to register against, if null user context is used
 	 * @param enforceExists true if access should only be granted if file exists
     * @return true if and only if the user has access to the 
     *          given file (in that case, he is allowed to read and write)
     */
    public boolean hasFileAccess(String file, SessionContext sc, boolean enforceExists);

    /**
     * Is the user allowed to submit queries of a certain type over the search input.
     * @return
     */
    public boolean hasQueryRight(SparqlQueryType qt, SessionContext sc);

    
    /**
	 * Convenience method to determine if user has query privileges, which can
	 * be used from servlets, i.e. {@link SparqlServlet} or {@link HybridSearchServlet}.
	 * 
     * @param query
     * @param qt
     * @param securityToken
     * @return
     */
    public boolean hasQueryPrivileges(String query, SparqlQueryType qt, String securityToken);
    	
    	
    /**
     * Generic access check for the given type and component name/ID.
     * Most more specific access right methods use this generic method internally.
     * 
     * @param componentType
     * @param componentName
     * @param sc
     * @return
     */
    public boolean hasComponentAccess(String componentType, String componentName, SessionContext sc);
    
    /**
     * Checks if the user is logged in or not, i.e. if the UserContext exists.
     * 
 	 * @param sc optional context to register against, if null user context is used
     * @return true if and only if the user is logged in
     */
    public boolean isLoggedIn(SessionContext sc);

    /**
     * Get the user name.
     * 
 	 * @param sc optional context to register against, if null user context is used
     * @return the user name
     */
    public String getUser(SessionContext sc);
    
    /**
     * Get the user roles. Contains special handling for the IWB user
     * modes, such as assigning admin role to users if user management
     * is turned off.
     * 
 	 * @param sc optional context to register against, if null user context is used
     * @return the list of roles
     */
    public List<String> getRoles(SessionContext sc);    
    
    /**
     * Get the user's URI. The users URI is the global identifier for the user
     * and identifies its profile page. Implementation should *always* return
     * a valid URI (not null, user {@link Vocabulary.SYSTEM}.UNKNOWN_USER as
     * a fallback when the user is not known (or not logged in).
     * 
 	 * @param sc optional context to register against, if null user context is used
     * @return the URI of the user
     */
    public URI getUserURI(SessionContext sc);
    
    /**
     * Get the user's name.
     * 
 	 * @param sc optional context to register against, if null user context is used
     * @return the URI of the user
     */
    public String getUserName(SessionContext sc);
	
	/**
	 * Token-protected servlet security check convenience method:
	 * checks whether a string and security token are a valid combination.
	 * 
	 * @param str
	 * @param securityToken
	 * @return
	 */
    public boolean tokenBasedAccessEnabled(String str, String securityToken);
}
