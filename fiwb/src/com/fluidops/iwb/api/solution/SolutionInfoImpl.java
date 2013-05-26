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

package com.fluidops.iwb.api.solution;

import com.fluidops.base.VersionInfo;
import com.fluidops.util.StringUtil;

public class SolutionInfoImpl implements SolutionInfo
{
    private final VersionInfo versionInfo;

	public static SolutionInfo snapshotVersion() {
        return new SolutionInfoImpl();
    }
    
    private SolutionInfoImpl() {
    	this(new VersionInfo());
    }
    
    public SolutionInfoImpl(VersionInfo versionInfo) {
		this.versionInfo = versionInfo;
    }
    
    @Override
    public boolean requiresUpgrade(SolutionInfo installedAppInfo)
    {
        return true;
    }


	@Override
	public String getName() {
		return versionInfo.getCanonicalProductName();
	}
	
	@Override
	public VersionInfo getVersionInfo() {
		return versionInfo;
	}
	
	@Override
	public String getNameAndVersion() {
		String version = versionInfo.getCanonicalVersion();
		if (StringUtil.isNullOrEmpty(version)) {
			version = "n/a";
		}
		return versionInfo.getCanonicalProductName() + " (version: " + version + ")";
	}
}
