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

/**
 * Metadata on a App/Solution. Basically allows access to the METAINF.MF file of a solution artifact. 
 */
public interface SolutionInfo
{
	/**
	 * @param installedSolutionInfo the {@link SolutionInfo} of a already installed solution.
	 * @return {@code true}, if this {@link SolutionInfo} belongs to a "newer" version of the "same" Solution. 
	 */
	boolean requiresUpgrade(SolutionInfo installedSolutionInfo);
	
	String getName();
	VersionInfo getVersionInfo();
	String getNameAndVersion();
}
