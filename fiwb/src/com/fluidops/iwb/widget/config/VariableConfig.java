/*
 * Copyright (C) 2008-2013, fluid Operations AG
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

import com.fluidops.iwb.api.editor.Datatype;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.widget.StockChartWidget;
import com.fluidops.iwb.widget.TableResultWidget;

/**
 * Configuration to associate a display label for a given SPARQL
 * project variable, as well as additional information. Used for
 * instance in the {@link TableResultWidget} to adjust the 
 * column headers.
 * 
 * @author as
 * @see TableResultWidget
 * @see StockChartWidget
 */
public class VariableConfig {

	@ParameterConfigDoc(desc = "Variable name of a projection from the query", required=true)
	public String variableName;
	
	@ParameterConfigDoc(desc = "Display label for the given SPARQL projection variable", required=true)
	public String displayName;
	
	@ParameterConfigDoc(desc = "The datatype associated to the given projected value, e.g. used for the sort order in tables",
			defaultValue="http://www.w3.org/2001/XMLSchema#string", type=Type.DROPDOWN)
	public Datatype datatype;
}
