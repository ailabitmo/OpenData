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

import java.util.Properties;

public interface PropertyMerger
{
    /**
     * Merge properties that potentially contain user modifications (typically
     * those from an existing installation) with "system" properties (typically
     * provided by a new installation). The "merge" result contains the
     * following properties:
     * <ul>
     * <li>all properties (key, value) from {@code userProperties}</li>
     * <li>all properties (key, value) from {@code systemProperties} where key
     * is not defined by {@code userProperties}.</li>
     * </ul>
     * 
     * @param userProperties
     *            the properties potentially containing user modifications
     * @param systemProperties
     *            "original" properties as provided by a new installation
     * @return a new set of properties containing the merge result
     */
    Properties merge(Properties userProperties, Properties systemProperties);
}
