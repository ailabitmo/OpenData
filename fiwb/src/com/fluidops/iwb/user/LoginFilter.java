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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.fluidops.api.exception.ApiSecurityException;
import com.fluidops.api.security.impl.SimpleSessionContext;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.servlet.LoginFilterBase;
import com.fluidops.util.user.UserContext;


/**
 * The Information Workbench {@link LoginFilterBase} implementation. 
 * 
 * If login is required, the default login form "/login/login.jsp"
 * is shown. By default the status servlet is unprotected.
 * 
 * @author as
 *
 */
public class LoginFilter extends LoginFilterBase {
	
	private static final Logger logger = Logger.getLogger(LoginFilter.class);

	@Override
	protected Set<String> initializeDefaultUnprotectedResources(
			String contextPath) {
		Set<String> res = super.initializeDefaultUnprotectedResources(contextPath);
		res.add(contextPath + "/status");
		res.add(contextPath + "/images/common/status_error.png");
		res.add(contextPath + "/images/common/status_okay.png");
		return res;
	}

	@Override
	protected UserContext authenticate(String user, String password)
			throws ApiSecurityException {
		try {
			return new EndpointImpl().getUserContext( new SimpleSessionContext( user, password ) );
		} catch (ApiSecurityException e) {
			throw e;
		} catch (Exception e) {
			logger.warn("General error during authentication: " + e.getMessage(), e);
			throw new ApiSecurityException("General error during authentication: " + e.getMessage(), e);
		}
	}

	@Override
	protected boolean loginRequiredCustomCheck(HttpServletRequest request) {
		if (Config.getConfig().iwbAuthenticationScheme().equals("auth"))
			return true;
		return false;
	}	
}
