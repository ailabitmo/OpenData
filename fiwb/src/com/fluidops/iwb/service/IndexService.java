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

package com.fluidops.iwb.service;

import javax.naming.OperationNotSupportedException;

import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.keywordsearch.SearchProviderFactory.TargetType;


/**
 * Service to update keyword indices for structured data or for Wiki pages.
 * This class provides two methods that are accessible via the CodeExecutionWidget.
 * 
 * Example usage:
 * 
 * <code> 
 * {{#widget: com.fluidops.iwb.widget.CodeExecutionWidget 
 * | label = 'Update keyword index'
 * | clazz = 'com.fluidops.iwb.service.IndexService'
 * | method = 'updateKeywordIndex'
 * }}
 * </code>
 * 
 * @author christian.huetter
 */
public class IndexService implements Service<TargetType> {

	@Override
	public Object run(TargetType t) throws Exception {

		switch (t) {
		case RDF:
			KeywordIndexAPI.updateKeywordIndex();
			break;
		case WIKI:
			KeywordIndexAPI.updateWikiIndex();
			break;
		default:
			throw new OperationNotSupportedException("Unknown target type " + t.name());
		}
		
		return Boolean.TRUE;
	}

	@Override
	public Class<TargetType> getConfigClass() {
		return TargetType.class;
	}
	
	/**
	 * Update the keyword index for structured data.
	 * 
	 * @throws Exception
	 */
	@CallableFromWidget
	public static void updateKeywordIndex() throws Exception {
		new IndexService().run(TargetType.RDF);
	}
	
	/**
	 * Update the keyword index for Wiki pages.
	 * 
	 * @throws Exception
	 */
	@CallableFromWidget
	public static void updateWikiIndex() throws Exception {
		new IndexService().run(TargetType.WIKI);
	}
}
