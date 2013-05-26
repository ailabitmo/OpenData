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

package com.fluidops.iwb.widget;

import com.fluidops.security.acl.ACL;

/**
 * Interface that allows specification of additional ACL permissions that are required in order
 * to have permission to use the widget.
 * This typically returns ACL permissions that are depending on the specific instance of a widget.
 * I.e. the generic script widget can request access to the
 * specific script that should be executed, which means mapping of scripts<-->users can easily be controlled
 * through ACLs.
 * 
 * @author uli
 */
public interface WidgetAccessControl
{
	/**
	 * Returns the additionally required ACL.
	 * 
	 * @return ACL, or null if no ACL required.
	 */
	public ACL getAdditionalACL();
}
