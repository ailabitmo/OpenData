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

package com.fluidops.iwb.install;

import static com.fluidops.install.InstallerUtil.hasSuffix;

import java.io.File;
import java.util.List;
import java.util.Properties;

import com.fluidops.install.ConfigUpgrader;
import com.fluidops.util.PropertyUtils;
import com.google.common.collect.Lists;


public class PropertyUpgrader implements ConfigUpgrader
{
    static final String CONFIG_DIR_NAME = "config";
    static final String PROPERTY_FILE_SUFFIX = ".prop";
    private final PropertyMerger merger;
    private final List<String> fileNameBlackList;
    
    public PropertyUpgrader(PropertyMerger merger)
    {
        this.merger = merger;
        this.fileNameBlackList = Lists.newArrayList();
    }

    @Override
    public void upgrade(File oldRootDir, File newRootDir)
    {
        File oldConfigDir = new File(oldRootDir, CONFIG_DIR_NAME);
        File newConfigDir = new File(newRootDir, CONFIG_DIR_NAME);
        assert oldConfigDir != null && oldConfigDir.isDirectory();
        assert newConfigDir != null && newConfigDir.isDirectory();
        for (String oldPropertyFilename : oldConfigDir.list(hasSuffix(PROPERTY_FILE_SUFFIX)))
        {
        	if (!shouldUpgradeFile(oldPropertyFilename)) continue;
            try
            {
                Properties oldPoperties = PropertyUtils.loadProperties(new File(oldConfigDir, oldPropertyFilename));
                File newFile = new File(newConfigDir, oldPropertyFilename);
                Properties newProperties = PropertyUtils.loadProperties(newFile);
                Properties result = merger.merge(oldPoperties, newProperties);
                PropertyUtils.saveProperties(newFile, result);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    
    public PropertyUpgrader skipFile(String fileName) {
    	fileNameBlackList.add(fileName);
    	return this;
    }

	/**
	 * Determine whether to upgrade (i.e. merge) a property file.
	 * 
	 * @param propertyFilename name of file to check
	 * @return <code>true</code> to upgrade/merge the file, <code>false</code> to skip it
	 */
	protected boolean shouldUpgradeFile(String propertyFilename) {
		return !fileNameBlackList.contains(propertyFilename);
	}
}
