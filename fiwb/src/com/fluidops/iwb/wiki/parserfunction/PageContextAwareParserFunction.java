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

package com.fluidops.iwb.wiki.parserfunction;

import info.bliki.wiki.template.ITemplateFunction;

import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.wiki.FluidWikiModel;


/**
 * Interface for any custom page context aware parser function. Parser
 * functions are a possibility to extend the functionality of the bliki
 * engine. Instances of this interface are aware of the page context as
 * they are created at rendering time and associated to the {@link FluidWikiModel}.
 * 
 * See {@link ParserFunctionsFactory} for usage instructions.
 * 
 * @author as
 */
public interface PageContextAwareParserFunction extends ITemplateFunction {

	/**
	 * Set the page context to this instance of the parser function
	 * @param pc
	 */
	public void setPageContext(PageContext pc);
	
	/**
	 * Return the function name of the parser function, typically
	 * starting with "#", e.g. #myParserFunction
	 * 
	 * @return
	 */
	public String getFunctionName();
}
