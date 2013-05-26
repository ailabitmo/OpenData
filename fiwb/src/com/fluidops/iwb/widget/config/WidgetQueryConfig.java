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

package com.fluidops.iwb.widget.config;

import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;

public class WidgetQueryConfig extends WidgetBaseConfig
{
	public static final String DEFAULT_NO_DATA_MESSAGE = "No data available";

	@ParameterConfigDoc(
			desc = "The query string to apply",  
			required=true,        		
			type=Type.TEXTAREA) 
	public String query;
	
	@ParameterConfigDoc(
			desc = "Apply inferencing to during query evaluation (requires repository support)",  
			required=false,        		
			defaultValue = "false") 
	public Boolean infer;

	/**
	 * Use {@link WidgetQueryConfig#getNoDataMessage(WidgetQueryConfig)} to
	 * retrieve the value of this field, as it checks the value and applies the
	 * correct fallbacks in case of {@code null} or empty strings.
	 */
	@ParameterConfigDoc(desc = "Custom message to be displayed if the resulting query is empty",
			defaultValue=DEFAULT_NO_DATA_MESSAGE)
	public String noDataMessage;

}
