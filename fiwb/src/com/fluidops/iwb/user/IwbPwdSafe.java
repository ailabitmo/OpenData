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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.util.Authentication;
import com.fluidops.util.persist.Properties;
import com.fluidops.util.persist.TransactionalFile;
import com.fluidops.util.user.PwdSafe;

/**
 * Wrapper class for IWB password functionality, allowing password storage and
 * retrieval for Information Workbench users as well as Information Workbench + INT
 * providers.
 * 
 * @author msc
 */
public class IwbPwdSafe
{
    private static final Logger logger = Logger.getLogger(IwbPwdSafe.class.getName());

    // ID in PwdSafe secrets.xml identifying an IWB user
    public static final String USER = "eCloudManagerUser";

    // ID in PwdSafe secrets.xml identifying an IWB provider
    public static final String IWB_PROVIDER = "IWBProvider";

    
	static Properties store = new Properties( new TransactionalFile( "passwords.properties" ) );

	/**
	 * Sets password for an IWB user, including group information
	 * 
	 * @param user the user name
	 * @param group the group of the user (may be null)
	 * @param pwd the user password
	 */
	public static void saveUserWithGroupAndPassword(String user, String group, String pwd) throws Exception
	{
		if (PwdSafe.getAllUsers().contains(user))
			throw new Exception("User '" + user + "' already exists");
		
		PwdSafe.setPwd(USER,group,user,pwd);
		saveToDisk();
	}
	
	/**
	 * Retrieves password for an IWB user
	 * 
	 * @param user the user name
	 * @return the user password
	 */
	public static String retrieveUserPassword(String user)
	{
		 return PwdSafe.getPwd(USER, null, user);
	}
	
	/**
	 * Retrieves the group an IWB user is associated to.
	 * 
	 * @param user
	 * @return
	 */
	public static String retrieveUserGroup(String user)
	{
		return PwdSafe.getGroupForUser(user);
	}

	/**
	 * Removes an IWB user from PwdSafe
	 * 
	 * @param user
	 */
	public static void deleteUser(String user)
	{
		Authentication.removeUserFromCache(user, retrieveUserPassword(user));
		PwdSafe.remove(USER,null,user);
	}
	
	/**
	 * Checks if an IWB user is contained in PwdSafe
	 */
	public static boolean containsUser(String user)
	{
		return PwdSafe.getAllUsers().contains(user);
	}

	/**
	 * Sets password for an IWB provider
	 * 
	 * @param providerId ID of the provider
	 * @param user the user name used for authorization
	 * @param newPass the password used for authorization
	 */
	public static void saveProviderWithUserAndPassword(URI providerId, String user, String newPass)
	{
		PwdSafe.setPwd(IWB_PROVIDER, providerId.toString(), user, newPass);
		saveToDisk();
	}
	
	/**
	 * Retrieves password for a user specification within an IWB provider
	 * 
	 * @param providerId ID of the provider
	 * @param user the user name used for authorization
	 * @return the password used for authorization
	 */
	public static String retrieveProviderUserPassword(URI agentLocation, String user)
	{
		return PwdSafe.getPwd(IWB_PROVIDER, agentLocation.toString(), user);
	}

	/**
	 * Removes password for a user specification within an IWB provider
	 * 
	 * @param providerId ID of the provider
	 * @param user the user name used for authorization
	 * @return the password used for authorization
	 */
	public static void deleteProviderUser(URI agentLocation, String user)
	{
		PwdSafe.remove(IWB_PROVIDER, agentLocation.toString(), user);
	}
	
	/**
	 * Saves passwords in PwdSafe secrets.xml to disk.
	 */
	private static void saveToDisk()
	{
    	try
		{
			PwdSafe.savePwdSafe();
		} 
    	catch (IOException e)
		{
    		logger.warn(e.getMessage());
		}
	}
}
