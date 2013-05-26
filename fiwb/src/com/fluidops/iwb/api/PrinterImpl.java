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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.SearchTextInput;
import com.fluidops.iwb.extensions.PrinterExtensions;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;

public class PrinterImpl implements Printer
{
    private static final Logger logger = Logger.getLogger(PrinterImpl.class.getName());

    private static UserManager userManager = EndpointImpl.api().getUserManager();
    
	@Override
	public void print(PageContext pc, HttpServletResponse response) throws IOException
	{
		
		final String context = pc.httpRequest.getContextPath();
		final SearchTextInput searchInput = new SearchTextInput("searchInput"+Rand.getIncrementalFluidUUID(),context);
		pc.page.register(searchInput);

		String uriString = null;
		String toolBarButtons = null;
		//String leftToolbarButtons = null;
		String abbreviatedUri = null;
		PrinterExtensions ext = Global.printerExtension;
		
		if ( pc.value instanceof URI )
		{
    		uriString = pc.value.stringValue();
    		if (uriString != null)
                uriString = java.net.URLEncoder.encode(uriString, "UTF-8");
    		
    		abbreviatedUri = EndpointImpl.api().getNamespaceService().getAbbreviatedURI((URI)pc.value);
		}
		
		String htmlHead = pc.page.htmlHead();
		toolBarButtons = ext.addToolbarButtons(pc, htmlHead);		
		//leftToolbarButtons = ext.addLeftToolbarButtons(pc);
		
		String googleAnalytics = ext.addGoogleAnalytics(Config.getConfig().getGoogleAnalytics());
		
		String user = userManager.getUserName(null);
		String loggedIn = userManager.isLoggedIn(null) ? "true" : null;
		String roleString = ext.getRoleString();
		
		String toolBarLeft = ext.addToolbarLeft(pc, htmlHead, user, userManager.isLoggedIn(null));
		
		TemplateBuilder tb;
		if(pc.mobile)
		     tb = new TemplateBuilder( Package.getPackage("com.fluidops.iwb.server"), "MobileServlet" );
		else 
		     tb = new TemplateBuilder( Package.getPackage("com.fluidops.iwb.server"), "Servlet" );

		
		boolean userHasReadAccess = userManager.hasValueAccess(pc.value,ValueAccessLevel.READ);
		String content = tb.renderTemplate( 
				"term", pc.title, 
				"head", htmlHead, 
				"body", pc.container.getContainer().htmlAnchor(),
				"path", pc.contextPath,
				"uriString", uriString,
				"toolBarButtons", toolBarButtons, 
				"toolBarLeft", toolBarLeft, 
				"loggedIn", loggedIn,
				"user", user,
				"roleString", roleString,
				"abbreviatedURI", abbreviatedUri,
				"tracking", googleAnalytics,
				"searchfield", userHasReadAccess ? searchInput.htmlAnchor() : "",
//				"searchlabel", userHasReadAccess ? searchLabel.htmlAnchor() : "",
				"licenseNagging", ext.getLicenseWarning(pc),
				"userScript", ext.getUserScript()
//				"leftToolbarButtons", leftToolbarButtons
		);
		response.setContentType( pc.contentType );
		ServletOutputStream out = response.getOutputStream();
		out.write( content.getBytes() );
		
		try
		{
		    out.close();
		}
		catch (Exception e)
		{
		    // TODO: proper handling, for now we disable it to avoid
		    // flooding the log files
		    // ignore: sometimes closing failes for graphs 
		}
	}
	
    
	/**
	 * A POST form that extracts the HTML anchor of the Wiki view,
	 * and sends it to the PDF servlet.
	 * @param htmlHead 
	 * 
	 * @return
	 */
	public static String getPdfGenerationPrefix(PageContext pc, String htmlHead)
	{
		Value v = pc.value;
		if (!(v instanceof URI))
			return "";
		String path = pc.contextPath;
        
        String templatePref = "<!DOCTYPE html PUBLIC\\\"-//W3C//DTD HTML 4.01 Transitional//EN\\\" \\\"http://www.w3.org/TR/html4/loose.dtd\\\">";
        templatePref = "<html><head>";
        templatePref += "<title>Print View for " + EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)pc.value, true) + "</title><script type=\\'text/javascript\\' src=\\'file:webapps/ROOT/ajax/ajax.js\\'></script>";
        templatePref += htmlHead.replace("'", "\\'").replace("\n","");        
        templatePref += "<script type=\\'text/javascript\\' src=\\'" + path + "/ajax/ajax.js\\'></script>";
        templatePref += "<link rel=\\'stylesheet\\' href=\\'" + path + "/ajax/stylesheet_fajax.css\\' type=\\'text/css\\' />";
        templatePref += "<link rel=\\'stylesheet\\' href=\\'" + path + "/stylesheet_fiwb.css\\' type=\\'text/css\\' />";        
        templatePref += "<!--[if lte IE 7]><link rel=\\'stylesheet\\' href=\\'" + path + "/css/ie7hacks.css\\' type=\\'text/css\\' /><![endif]-->";
        templatePref += "<!--[if IE 8]><link rel=\\'stylesheet\\' href=\\'" + path + "/css/ie8hacks.css\\' type=\\'text/css\\' /><![endif]-->";
        templatePref += "<link rel=\\'stylesheet\\' href=\\'" + path + "/css/semwiki.css\\' type=\\'text/css\\' />";
//        templatePref += "</head><body onload=\\'javascript:fluInit();\\'>";
        templatePref += "</head><body>";

        return templatePref;
	}
	
	/**
	 * A POST form that extracts the HTML anchor of the Wiki view,
	 * and sends it to the PDF servlet.
	 * 
	 * @return
	 */
	public static String getPdfGenerationSuffix(PageContext pc)
	{
		Date nowDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		String now = sdf.format(nowDate);
		

		String versionDateStr = null;
		String version = pc.httpRequest.getParameter("version");
		if (!StringUtil.isNullOrEmpty(version))
		{
			try
			{
				Long timestamp = Long.valueOf(version);
				Date versionDate = new Date();
				versionDate.setTime(timestamp);
				versionDateStr = " (version from " + sdf.format(versionDate) + ")";
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage());
			}
		}
		if (versionDateStr==null)
			versionDateStr = " (latest version)";
		
		
		String templateSuf = "<hr/>";
		templateSuf += "<i>Printable version of document <b>" + EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)pc.value, true) + "</b>" + versionDateStr + ". The document was generated on " + now + " by user " + userManager.getUserName(null) + ".";
		templateSuf += "</body></html>";
		
		return templateSuf;
	}
}
