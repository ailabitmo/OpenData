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
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.fluidops.util.GenUtil;
import com.fluidops.util.StringUtil;

public class UserManagementAdministration
{
	private static final Logger logger = Logger.getLogger(UserManagementAdministration.class.getName());
	

	/**
	 * Initialize a user from a file "userManagementInit.prop" that resides in
	 * the config folder of the Information Workbench. After successful initialization
	 * this file is deleted.
	 * 
	 * Example for userManagementInit.prop
	 * 
	 * <source>
	 * user=admin
	 * pass=iwb
	 * group=admin
	 * </source>
	 * 
	 * @param userInitFile the location of the properties file, typically "userManagementInit.prop"
	 * @throws Exception
	 */
	public static void initializeUserFromFile(File userInitFile) throws Exception {

		if (!userInitFile.exists()) {
			logger.debug("File " + userInitFile.getName() + " could not be found (no new admin user created)");
			return;
		}				

        FileInputStream stream = null;
		try {
			stream = new FileInputStream(userInitFile);
			Properties user = new Properties();
			user.load(stream);
			
			String userName = user.getProperty("user");
			String userPass = user.getProperty("pass");
			String userGroup = user.getProperty("group");
			
			// check if specification is correctly
			if (StringUtil.isNullOrEmpty(userName) || StringUtil.isNullOrEmpty(userPass)
					|| StringUtil.isNullOrEmpty(userGroup)) {
				logger.warn("Illegal user specification in " + userInitFile.getName());
				return;
			}
			
			// check if a user with given name already exists
			if (IwbPwdSafe.containsUser(userName)) {
				logger.warn("Cannot create user from " + userInitFile.getName() + ": User '" + userName +  "' already exists. ");
				return;
			}
			
			IwbPwdSafe.saveUserWithGroupAndPassword(userName, userGroup, userPass);
			logger.info("Created new user " + userName + " in group " + userGroup);			
			
		} catch (Exception e) {
			logger.warn("An error occured while processing " + userInitFile.getName() + ": " + e.getMessage());
			throw e;
		} finally {
			GenUtil.closeQuietly(stream);			
		}	
		
		// delete file in case of success
		GenUtil.delete(userInitFile);
	}
}