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

package com.fluidops.iwb.extensions;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.PrinterImpl;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManager.UIComponent;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.user.UserContext;

public class PrinterExtensionsImpl implements PrinterExtensions {

	private static final Logger logger = Logger.getLogger(PrinterExtensionsImpl.class.getName());
    
    private static Pattern containsParams = Pattern.compile(".*\\?.*"); // used to match if http request contains parameters

    
	@Override
	public String addGoogleAnalytics(String trackingNumber) 
	{
		if (trackingNumber != null)
			return "<script type=\"text/javascript\">\n" +
				"var gaJsHost = ((\"https:\" == document.location.protocol) ? \"https://ssl.\" : \"http://www.\");\n" +
				"document.write(unescape(\"%3Cscript src='\" + gaJsHost + \"google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E\"));\n" +
				"</script>\n" +
				
				"<script type=\"text/javascript\">\n" +
				"var pageTracker = _gat._getTracker(\"" + trackingNumber + "\");\n" +
				"pageTracker._trackPageview();\n" +
				"</script>";
		
		return null;
	}
	
	@Override
	public String addToolbarButtons(PageContext pc, String htmlHead) 
	{
		String buttons = "";

	    UserManager userManager = EndpointImpl.api().getUserManager();
	   
		boolean loggedIn = userManager.isLoggedIn(null);
		
		String uri = null;
		if (pc.value != null)
			uri = pc.value.stringValue();
		
		// PDF button
        if (pc.value!=null && pc.value instanceof URI && userManager.hasUIComponentAccess(UIComponent.HEADER_PRINT_BUTTON, null))
        {
        	String templatePref = PrinterImpl.getPdfGenerationPrefix(pc, htmlHead);
        	String templateSuf = PrinterImpl.getPdfGenerationSuffix(pc);
        	String onClickAction = "var cont = ($(\'.viewContentClazz\'))[0].innerHTML; var w = window.open();  w.document.write(\'" + templatePref + "\' + cont + \'" + templateSuf + "\'); w.document.close();";
        	buttons += "<div class=\"topmenubarRightButtonPDF navButton\" onclick=\"" + onClickAction + "\" /><img src=\"" + pc.contextPath + "/images/navigation/pdf.png\" /><div><span class=\"buttontext\">Print</span></div></div>";
	        buttons += "<div class=\"topmenubarRightSpacer\"></div>";
        }

        
        // Admin Tools button
        if (userManager.hasUIComponentAccess(UIComponent.HEADER_ADMIN_BUTTON, null))
        {
			buttons += "<div class=\"topmenubarRightButtonAdminTools navButton\" onclick=\"$r('" + EndpointImpl.api().getRequestMapper().getContextPath() + Config.getConfig().getUrlMapping() + "Admin:Admin?view=wiki');\"><img src=\"" + pc.contextPath + "/images/navigation/admin_tools.png\" /><div><span class=\"buttontext\">Admin</span></div></div>";
			buttons += "<div class=\"topmenubarRightSpacer\"></div>";
        }
        
		// Help button
        if (userManager.hasUIComponentAccess(UIComponent.HEADER_HELP_BUTTON, null))
        {
			buttons += "<div class=\"topmenubarRightButtonHelp navButton\" onclick=\"$r('" + pc.contextPath + Config.getConfig().getUrlMapping() + "Help:Help?view=wiki');\"><img src=\"" + pc.contextPath + "/images/navigation/help.png\" /><div><span class=\"buttontext\">Help</span></div></div>";
	        buttons += "<div class=\"topmenubarRightSpacer\"></div>";
        }
        
        // if logout should be hidden, do not render
        if (UserContext.get() == null || !UserContext.get().hideLogout)
        {
        	// Login & Logout button
		    if (loggedIn)
		    {
				buttons += "<div class=\"topmenubarRightButtonAdminTools navButton\" onclick=\"$r('" + pc.contextPath + "/logout.jsp');\"><img src=\"" + pc.contextPath + "/images/navigation/admin_tools.png\" /><div><span class=\"buttontext\">Logout</span></div></div>";
				buttons += "<div class=\"topmenubarRightSpacer\"></div>";
		    }
			else
			{
				
				
				if (uri != null)
				{
					String requestString =  EndpointImpl.api().getRequestMapper().getRequestStringFromValue(pc.value);
					String connector = (containsParams.matcher(requestString).matches()) ? "&" : "?";
					buttons += "<div class=\"topmenubarRightButtonLogin navButton\" onclick=\"$r('" + requestString + connector + "login=true" + "');\"><img src=\"" + pc.contextPath + "/images/navigation/login.png\" /><div><span class=\"buttontext\">Login</span></div></div>";
				}
				else
				{
					String fullRequestURI = pc.httpRequest.getRequestURI();
					if (pc.httpRequest.getQueryString()!=null && !pc.httpRequest.getQueryString().isEmpty())
						fullRequestURI += "?" + pc.httpRequest.getQueryString();
					String connector = (containsParams.matcher(fullRequestURI).matches()) ? "&" : "?";
					buttons += "<div class=\"topmenubarRightButtonLogin navButton\" onclick=\"$r('" + fullRequestURI + connector + "login=true');\"><img src=\"" + pc.contextPath + "/images/navigation/login.png\" /><div><span class=\"buttontext\">Login</span></div></div>";
				}
			}
        }
        
	    return buttons;
	}

    @Override
    public String getLicenseWarning(PageContext pc)
    {
        return null;
    }
    
    @Override
    public String getUserScript()
    {
        return null;
    }
    
    public String getCurrentEdition()
    {
        return "Information Workbench";
    }

	@Override
	public String addToolbarLeft(PageContext pc, String htmlHead, String user, boolean loggedIn)
	{
		if ( ! loggedIn )
			return "<div class=\"topmenubarLeft\"></div>";
		else
			return "<div class=\"topmenubarLeft\"><div class=\"loggedInLabel\">&nbsp;Logged in as <b>"+user+"</b></div></div>";
	}

	@Override
	public String getRoleString() 
	{
		return null;
	}
}
