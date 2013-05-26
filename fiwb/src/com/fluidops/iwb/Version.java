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

package com.fluidops.iwb;

import java.util.Date;

import com.fluidops.base.VersionInfo;
import com.fluidops.util.Singleton;

/**
 * Handles Versioning of the product.
 * 
 * @author vasu
 */
public class Version
{
	private static Singleton<VersionInfo> versionInfo = new Singleton<VersionInfo>() {
		protected VersionInfo createInstance() throws Exception {
			VersionInfo versionInfo = null;
			try {
				versionInfo = VersionInfo.getVersionInfoForClass(Version.class);
			}
			catch (Throwable t) {
				// if the classes are not packaged as jar (e.g. when running from Eclipse)
				// we might get an exception, which we ignore
			}
			if (versionInfo == null) {
				// fallback: return hard-coded values
				versionInfo = new VersionInfo("Information Workbench")
								.setVersion("8.8.8.8888")
								.setBuildDate(new Date().toString());
			}
			
			return versionInfo;
		}
	};
	
	public static VersionInfo getVersionInfo() {
		return versionInfo.instance();
	}
    	
    /**
     * Returns the version.
     * @return
     */
	public static String getVersion()
	{	    
		return versionInfo.instance().getVersion();
	}

    /**
     * Returns the major version.
     * @return
     */
	public static String getMajorVersion()
	{	    
		return versionInfo.instance().getMajorVersion();
	}

    /**
     * Returns the minor version.
     * @return
     */
	public static String getMinorVersion()
	{
	    return versionInfo.instance().getMinorVersion();
	}

    /**
     * Returns the patch version.
     * @return
     */
    public static String getPatchVersion()
    {
        return versionInfo.instance().getPatchVersion();
    }

    /**
     * Returns the build date.
     * @return
     */
	public static String getBuildDate()
	{
	    return versionInfo.instance().getBuildDate();
	}

    /**
     * Returns the build number.
     * @return
     */
	public static String getBuildNumber()
	{
	    return versionInfo.instance().getBuildNumber();
	}

    /**
     * @return Returns the companyName.
     */
    public static String getCompanyName()
    {
        return versionInfo.instance().getCompanyName();
    }

    /**
     * @return Returns the productLongName.
     */
    public static String getProductLongName()
    {
        return versionInfo.instance().getProductLongName();
    }

    /**
     * @return Returns the productName.
     */
    public static String getProductName()
    {
        return versionInfo.instance().getProductName();
    }

    /**
     * @return Returns the productVersion.
     */
    public static String getProductVersion()
    {
        return versionInfo.instance().getProductVersion();
    }

    /**
     * @return Returns an email
     */
    public static String getProductContact()
    {
        return versionInfo.instance().getProductContact();
    }

    /**
     * Prints the version info.
     * @param args
     */
	public static void main(String[] args)
	{
	    System.out.println("Version Information");
		System.out.println("Version: " + getVersion());
		System.out.println("Major: " + getMajorVersion());
        System.out.println("Minor: " + getMinorVersion());
        System.out.println("Patch: " + getPatchVersion());
		System.out.println("Build Date: " + getBuildDate());
        System.out.println("Build: " + getBuildNumber());
        System.out.println("ProductName: " + getProductName());
        System.out.println("ProductLongName: " + getProductLongName());
        System.out.println("CompanyName: " + getCompanyName());
        System.out.println("ProductVersion: " + getProductVersion());
        System.out.println("ProductContact: " + getProductContact());
	}
}
