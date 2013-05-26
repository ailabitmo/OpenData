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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fluidops.api.JavaCompletor;
import com.fluidops.api.aop.Aspect;
import com.fluidops.api.completor.CliCompletor2;
import com.fluidops.api.endpoint.Endpoint;
import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.Protocol;
import com.fluidops.api.endpoint.impl.DefaultEndpoint;
import com.fluidops.api.exception.ApiSecurityException;
import com.fluidops.api.security.Authenticator;
import com.fluidops.api.security.LoginMode;
import com.fluidops.api.security.RoleMapper;
import com.fluidops.api.security.SessionContext;
import com.fluidops.api.security.impl.WindowsAuth;
import com.fluidops.api.security.ssl.SSLUtils;
import com.fluidops.iwb.api.solution.ExternalSolutionService;
import com.fluidops.iwb.api.solution.SolutionServiceRemote;
import com.fluidops.iwb.util.Config;
import com.fluidops.network.RMIBase;
import com.fluidops.network.RMISSLSocketFactory;
import com.fluidops.util.user.UserContext;

/**
 * The Class EndpointImpl.
 */
public class EndpointImpl extends EndpointDescription
{
	private static final Logger logger = Logger.getLogger(EndpointImpl.class.getName());
	
	/**
	 * flag indicating if ssl shall be used, see initClient and IwbStart
	 */
	public static final boolean USE_SSL = true;
	
	@Override
	public Class<?> getApiClass()
	{
        return API.class;
	}

	@Override
	public List<Aspect> getAspects()
	{
		return null ;
	}

	protected Authenticator auth;
	
	public void init() 
	{
		// TODO 
		try
		{
			/**
			 * aeb
			 * same code as SSLSupport.enableSSLSupport() (minus config flexibility, multihome support)
			 * since class SSLSupport is located in fcoremgmgt
			 */
//			if ( RMISocketFactory.getSocketFactory() == null )
//			{
//				RMISSLSocketFactory.initializeServer("fluidops.ca", "changeit", "fluidops.key", "01FluidOps02");
//				RMIBase.initialize();
//				RMIBase.installGlobally();
//			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Authenticator getAuthenticator() throws Exception
	{
		if (auth==null)
			auth = new WindowsAuth();
		return auth;
	}

	@Override
	public Object getCompletor(String language)
	{
		if ( "cli".equals( language ) )
			return new CliCompletor2( getApiClass() );
		if ( "bsh".equals( language ) )
			return new JavaCompletor( getApiClass() );
		return null;
	}

	@Override
	public Collection<Endpoint> getEndpoints()
	{
		List<Endpoint> res = new ArrayList<Endpoint>();
		
		if (host==null) {
			host = System.getProperty("com.fluidops.api.HOST", null);
			if (host==null) {
				logger.warn("WARNING: host is not specified. Using localhost instead");
				host = "localhost";
			}
		}
		
		res.add( new DefaultEndpoint( Protocol.RMI, "//"+host+":" + Config.getConfig().getRMIPort() + "/OM", LoginMode.API_LOGIN ) );
		res.add( new DefaultEndpoint( Protocol.JSONRpc, "http://"+host + ":8888/REST/JSON/", LoginMode.API_LOGIN) );
		res.add( new DefaultEndpoint( Protocol.XMLRpc, "http://"+host + ":8888/REST/XML/", LoginMode.API_LOGIN) );
		
		return res;
	}

	RoleMapper rm;
	
	/**
	 * API root object
	 */
	private static APIImpl api;
	
	/**
	 * API root object
	 */
	public static APIImpl api()
	{
		if ( api == null )
			api = new APIImpl();
		return api;
	}
	
	public static void setApi(APIImpl impl)
	{
		api = impl;
	}
	
	/**
	 * performs authentication of the session context and creates a new UserContext instance
	 * with the user name and roles set
	 * @param sc		session context (user / pwd)
	 * @return			UserContext with name and roles set
	 * @throws Exception
	 * @throws ApiSecurityException
	 */
	public UserContext getUserContext( SessionContext sc ) throws Exception, ApiSecurityException
	{
		RoleMapper rm = getRoleMapper();
		Authenticator auth = getAuthenticator();
		auth.authenticate(sc);
		UserContext c = new UserContext();
		c.name = sc.getUser();
		List<String> roles = rm.getUserRoles(sc);
		if ( roles != null )
			c.roles = roles;
		return c;
	}
	
	@Override
	public RoleMapper getRoleMapper() throws Exception
	{
		if (rm==null)
			rm = new WindowsAuth();

		return rm;
	}

	@Override
	public Object getServerApi()
	{
		return EndpointImpl.api();
	}


	@Override
	protected Map<Class<?>, Object> initRemoteMapping() throws Exception
	{
		Map<Class<?>, Object> res = new HashMap<Class<?>, Object>();
		res.put( ProviderService.class, new ProviderServiceRemote());
		res.put( WidgetService.class, new WidgetServiceRemote());
		res.put( WidgetSelector.class, new WidgetSelectorRemote());
		res.put( ExternalNamespaceService.class, new NamespaceServiceRemote());
		res.put( Layouter.class, new LayouterRemote());
	    res.put( BackupService.class, new BackupServiceRemote());
	    res.put(MonitoringService.class, new MonitoringServiceRemote());
	    res.put(ExternalSolutionService.class, new SolutionServiceRemote());
		return res;
	}

	@Override
	public void initClient() throws Exception {
		if (USE_SSL) {
			// FIXME this is the same code as in fcoremgtm SSLSUpport.enableSSLSupportClient()
			// however, it cannot be used here
			// TODO add SSL init code here
			String ts = "fluidops.ca";
			String tsPsw = "changeit";
	    	RMISSLSocketFactory.initializeClient(ts, tsPsw);
			RMIBase.initialize();
			// enable SSL for the API (system property)
			SSLUtils.enableSSL();
		}
	}

	@Override
	public void setContext(SessionContext sc, RoleMapper rm) {
		if ( sc == null ) {
			UserContext.set( null );
			return;
		}
		
		UserContext c = new UserContext();
		c.name = sc.getUser();
		if ( rm != null ) {
			try {
				List<String> roles = rm.getUserRoles(sc);
				if ( roles != null )
					c.roles = roles;
			}
			catch ( Exception e ){
				logger.error("error while getting user roles", e );
			}
		}
		UserContext.set( c );
	}
	
}
