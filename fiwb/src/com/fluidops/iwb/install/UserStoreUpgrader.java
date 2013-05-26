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

import com.fluidops.install.ConfigUpgrader;
import com.fluidops.util.GenUtil;

public class UserStoreUpgrader implements ConfigUpgrader
{

    static final String CONFIG_DIR_NAME = "config";
    static final String USER_STORE_SUFFIX = "-user.xml";

    @Override
    public void upgrade(File oldRootDir, File newRootDir)
    {
        File oldConfigDir = new File(oldRootDir, CONFIG_DIR_NAME);
        File newConfigDir = new File(newRootDir, CONFIG_DIR_NAME);
        for (String filename : oldConfigDir.list(hasSuffix(USER_STORE_SUFFIX)))
            GenUtil.copyFile(new File(oldConfigDir, filename), new File(newConfigDir, filename));
    }
}
