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

package com.fluidops.iwb.util;

import java.io.Serializable;

import org.openrdf.model.Resource;

import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.iwb.user.IwbPwdSafe;

/**
 * user object for managing login data for providers.
 * This object is used in several provider config pojos. 
 * The tasks at hand are
 * 
 * 1) client needs to be able to specify credentials
 * 2) credentials need to be stored securely
 * 3) providers need to be able to retrieve credentials
 * 
 * @author aeb, msc
 */
public class User implements Serializable
{
	private static final long serialVersionUID = 4723042848140364109L;

	public static final String MASKEDPASSWORD = "********";
    
	public String username;
    public String password = MASKEDPASSWORD;
    public Resource __resource;
    
    public String password( AbstractFlexProvider p )
    {
    	if ( MASKEDPASSWORD.equals( password ) )
    		return IwbPwdSafe.retrieveProviderUserPassword(p.providerID, username);
    	else
    		return password;
    }
}
