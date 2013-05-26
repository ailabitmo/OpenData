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

import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;

/**
 * marker interface for all providers that query services rather than
 * a datasource that can be queried completely.
 * 
 * TODO: The above distinction between datasource and service is not adequate.
 * A datasource that is queried completely can just as well be based on a service.
 * Vice versa, a data source that delivers additional data for a value is not necessarily based on a Service
 * (just think of dynamic URI lookups for an RDF resource)
 * These concepts are completely orthogonal. 
 * 
 * A more adequate characteristic is that dynamic lookups for the current URI are performed, therefore renaming to LookupProvider
 * 
 * AbstractServiceProvider can be queried little by little
 * given a context URI (e.g. a city for getting weather info)
 * or a stock symbol for getting stock quotes
 * 
 * @author aeb, pha
 */
public interface LookupProvider
{
    /**
     * lookup of data driven by a given subject 
     * @param config
     * @param res
     * @param subject
     */
    public void gather( List<Statement> res, URI uri ) throws Exception;
    
    /**
     * Checks whether the provider is able to lookup data for this URI
     * @param uri
     * @return
     */

    public boolean accept( URI uri );
}
