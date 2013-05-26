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

import java.io.File;

import com.fluidops.install.ConfigUpgrader;
import com.fluidops.install.InstallerUtil;

public class IwbDataUpgrader implements ConfigUpgrader
{
    @Override
    public void upgrade(File oldRootDir, File newRootDir)
    {
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/wiki");
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/dbmodel");
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/historymodel");
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/luceneindex");
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/wikiindex");
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/backup");
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/upload");       
        InstallerUtil.copyFolder(oldRootDir, newRootDir, "data/ontologies");       
    }
}
