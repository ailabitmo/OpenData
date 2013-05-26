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

package com.fluidops.iwb.server;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.ajax.FSession;
import com.fluidops.ajax.api.PageMapper;
import com.fluidops.ajax.api.PageMapperImpl;
import com.fluidops.ajax.components.FPage;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.APIImpl;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.iwb.provider.LookupProvider;
import com.fluidops.iwb.server.RedirectService.RedirectType;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.SQL;
import com.fluidops.util.GenUtil;

/**
 * simplified wiki servlet workflow
 * 
 * @author aeb, pha
 */
public class Servlet extends IWBHttpServlet
{
 	private static final long serialVersionUID = 6284806805007152097L;

    private static final Logger logger = Logger.getLogger(Servlet.class.getName());
    
    private static UserManager userManager = EndpointImpl.api().getUserManager();
    
   
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
		    Value value = EndpointImpl.api().getRequestMapper().getValueFromRequest(request);
		    
		    ////////////////// SECURITY CHECKS
		    // illegal page
		    if (value==null)
		    {
		        response.sendRedirect(RedirectService.getRedirectURL(RedirectType.PAGE_NOT_FOUND, request));
                return;
            }   
		    // no read access to page -> forward to access forbidden page
		    if (!userManager.hasValueAccess(value,ValueAccessLevel.READ))
		    {
		    	if (userManager.isLoggedIn(null))
		    		response.sendRedirect(RedirectService.getRedirectURL(RedirectType.ACCESS_FORBIDDEN, request));
		    	else
		    	{
		    		// decide how the login-parameter has to be attached to the new request
		    		String delimiter = request.getParameterMap().size() == 0 ? "?" : "&";
		    		response.sendRedirect(EndpointImpl.api().getRequestMapper().getRequestStringFromValue(value) + delimiter + "login=true");
		    	}
                return;
		    }   
		    
			// every page is a LeanPage
			// we do not share pages for different URIs in order to
			// be able to support multiple browser tabs
			// TODO: pages need to be cleaned up
		    String accept = request.getHeader("accept");
		    if(accept != null && accept.contains("application/rdf+xml"))
		    {
		        if(value instanceof URI) 
		            RDFServlet.getRDF((URI)value, response);
		        return;
		    }

//		    String uriHash = (request.getParameter("uri") == null) ? "" : request.getParameter("uri");
		    PageMapper pm = new PageMapperImpl();
			FSession.registerPage(pm.getMapping(request), FPage.class);

			// get session and page
			FSession session = FSession.getSession(request);
			FPage page = (FPage)session.getComponentById(pm.getMapping(request), request);
			
			if ( page!=null ) page.setRequest(request);
			
			// the main object passed between processing steps
			PageContext pc = new PageContext();
			
			// page webapp context
			pc.contextPath = request.getContextPath();
			
			// register FPage
			pc.page = page;
			pc.session = session;
			pc.httpRequest = request;
			pc.httpResponse = response;
			pc.repository = Global.repository;

			//hide the popup window
			if (pc.page!=null)
				pc.page.getPopupWindowInstance().hide();
			
			APIImpl api = EndpointImpl.api();
	        
			// check if the connection to the global repository works
			ReadWriteDataManagerImpl.verifyConnection(Global.repository, false);

			// if SQLStorage is used: check if connection to the server is possible
			if (Config.getConfig().getUseMySQL())
				SQL.verifyConnection();
			
	        
			// lookup subject
	        api.getRequestMapper().map(pc, request);
	        	
	        //  Lookup Providers for this URI
	        // TODO: Should think about more flexible mechanism to assign providers to certain types of objects (e.g. based on rdf:type), could be similar as for WidgetConfigs
	        if ( pc.value instanceof URI )
				for ( AbstractFlexProvider provider : EndpointImpl.api().getProviderService().getProviders() )
					if ( provider instanceof LookupProvider )
					{
					    URI uri = (URI)pc.value;
						if(!((LookupProvider)provider).accept(uri)) continue;
						ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
						
						// if data for this URI is already cached, find our in which context and whether it needs updating
						URI contextURI = dm.isCached(provider.getProviderID(), uri);

						if(contextURI==null || ( System.currentTimeMillis() - dm.getContext(contextURI).getTimestamp())>provider.pollInterval)
						{
						    // the correct way to store the provider data is as follows, but it seems not to scale with OWLIM
						    EndpointImpl.api().getProviderService().runProvider(provider.providerID, uri.stringValue());
						    
						    // The following is a workaround: The context management with OWLIM does not scale, therefore we write directly into the repository
						    /*List<Statement> stmts = new LinkedList<Statement>();
						    ((LookupProvider)provider).gather(stmts, (URI)pc.value);
						    Context c = new Context(Context.ContextType.LOOKUPPROVIDER, 
						            provider.providerID, null, uri,
						            System.currentTimeMillis(), "Live Lookup");
						    dm.addToContextNoDuplicates(stmts, c);*/
						    
						}

						dm.close();
					}
 
	        String activeLabel = request.getParameter("view");
	        if (activeLabel != null) {
	        	if (activeLabel.equalsIgnoreCase("wiki"))
	        		pc.session.setSessionState("activeLabel","<div style=\"width:100%; height:45px; font-size:0;\">wiki</div><div class=\"wikiViewHover\">Wiki View</div>");
	        	else if (activeLabel.equalsIgnoreCase("table"))
	        		pc.session.setSessionState("activeLabel","<div style=\"width:100%; height:45px; font-size:0;\">table</div><div class=\"wikiViewHover\">Table View</div>");
	        	else if (activeLabel.equalsIgnoreCase("graph"))
	        		pc.session.setSessionState("activeLabel","<div style=\"width:100%; height:45px; font-size:0;\">graph</div><div class=\"wikiViewHover\">Graph View</div>");
	        	else if (activeLabel.equalsIgnoreCase("pivot"))
	        		pc.session.setSessionState("activeLabel","<div style=\"width:100%; height:45px; font-size:0;\">pivot</div><div class=\"wikiViewHover\">Pivot View</div>");
	        }
	        
	        // widgets
	        File file = new File(Config.getConfig().getWorkingDir() + "config/widgets.prop");
	        if (!file.exists()) {
	        	GenUtil.createNewFile(file);
	        }
	        api.getWidgetSelector().selectWidgets(pc);
	        
	        File file2 = new File(Config.getConfig().getWorkingDir() + "config/layout.prop");
	        if (!file2.exists()) {
	        	GenUtil.createNewFile(file2);
	        }
	        
	        // layout
	        api.getLayouter().populateContainer( pc );
	        
	        // print
	        api.getPrinter().print(pc, response);
	        
	        // server side state is reinitialized, thus reset onload count
			pc.page.onLoadCount = 0;
		}
		catch ( Exception e )
		{
			/* exception here mean severe errors, we let jetty deal with them */
			logger.error(e.getMessage(), e);
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;			
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * The login is done via a POST request, therefore also need to support it here.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        doGet(req, resp);
    }
    
}
