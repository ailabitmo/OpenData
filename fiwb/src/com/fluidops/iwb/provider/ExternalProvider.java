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

package com.fluidops.iwb.provider;

import java.io.Serializable;
import java.util.List;

import org.openrdf.model.Statement;

/**
 * pseudo provider which is used when data is provided from outside the backend
 * 
 * @author aeb
 */
public class ExternalProvider extends AbstractFlexProvider<Serializable>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7391406983567001085L;

	@Override
	public void gather(List<Statement> res) throws Exception
	{
	}

	@Override
	public Class<? extends Serializable> getConfigClass()
	{
		return null;
	}
}
