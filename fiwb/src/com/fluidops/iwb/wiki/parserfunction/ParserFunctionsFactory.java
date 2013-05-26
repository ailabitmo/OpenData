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

import info.bliki.wiki.model.WikiModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.google.common.base.Throwables;

/**
 * Helper class to register custom parser function extensions. Parser
 * functions can be static - and configured statically via
 * 
 * Configuration.DEFAULT_CONFIGURATION.addTemplateFunction("#name", MyParserFunction.singleton); 
 *
 * or they can be page context aware (i.e. instances of {@link PageContextAwareParserFunction}.
 * The latter type are registered to the bliki engine instance (i.e. {@link FluidWikiModel})
 * at rendering time and they are aware of the current page context.
 * 
 * This factory provides a convenience method to register custom extensions via the
 * Java SPI mechanism, i.e. create a file in META-INF/services called
 * "com.fluidops.iwb.wiki.parserfunction.PageContextAwareParserFunction"
 * and define the parser functions one in a line. Note that each parser
 * function must have a default constructor.
 * 
 * @author as
 */
public class ParserFunctionsFactory {

	private static Logger logger = Logger.getLogger(ParserFunctionsFactory.class);
	
	
	// keep track of class available from the service loader
	private static Set<Class<? extends PageContextAwareParserFunction>> registeredServices = 
			new HashSet<Class<? extends PageContextAwareParserFunction>>();
	
	/**
	 * Register static parser functions and initialize the custom extensions
	 * to be registered at rendering time.
	 */
	public static void registerCustomParserFunctions() {
		logger.info("Registering and initializing custom bliki parser functions.");
		
		/* NOTE: static functions could be added here */
		
		// initialize custom functions that are defined using Java SPI
		ServiceLoader<PageContextAwareParserFunction> serviceLoader = ServiceLoader.load(PageContextAwareParserFunction.class);
		Iterator<PageContextAwareParserFunction> iter = serviceLoader.iterator();
		while (iter.hasNext()) {
			PageContextAwareParserFunction pf = iter.next();
			registeredServices.add( pf.getClass() );
		}

		logger.debug("Registered additional custom parser functions: " + registeredServices);
	}
	
	/**
	 * Registers the page context aware parser functions as defined in 
	 * {@link #createPageContextAwareParserFunctions()} to the wiki model
	 * and sets the given page context. Note that instances are created
	 * at rendering time
	 * 
	 * @param wikiModel
	 * @param pc
	 */
	public static void registerPageContextAwareParserFunctions(FluidWikiModel wikiModel, PageContext pc) {
		
		for (PageContextAwareParserFunction pf : createPageContextAwareParserFunctions()) {
			pf.setPageContext(pc);
			wikiModel.addContextAwareTemplateFunction(pf);
		}
	}
	
	/**
	 * Creates new instances of the defined page context aware parser functions. These
	 * parser functions are created at rendering time, and the page context is
	 * set in {@link #registerPageContextAwareParserFunctions(WikiModel, PageContext)}
	 * 
	 * @return
	 */
	private static List<PageContextAwareParserFunction> createPageContextAwareParserFunctions() {
		List<PageContextAwareParserFunction> res = new ArrayList<PageContextAwareParserFunction>();
		for (Class<? extends PageContextAwareParserFunction> c : registeredServices) {
			try {
				res.add(c.newInstance());
			} catch (Exception e) {
				Throwables.propagate(e);
			}
		}
		
		/* NOTE: additional classes known at compile time can be added here */ 
		
		return res;				
	}
}
