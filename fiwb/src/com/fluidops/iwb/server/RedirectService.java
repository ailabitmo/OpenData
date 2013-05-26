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

package com.fluidops.iwb.server;

import static com.fluidops.iwb.api.EndpointImpl.api;
import static com.fluidops.iwb.util.Config.getConfig;

import javax.servlet.http.HttpServletRequest;

/**
 * Class generating common redirect URLs, e.g. generating redirects
 * to AccessForbidden and PageNotFound URLs. Can be used by servlets
 * to offer a common redirect scheme.
 * 
 * @author msc
 *
 */
public class RedirectService
{
    /**
     * Mapping from redirect strings to Wiki templates
     * @author msc
     *
     */
    public static enum RedirectType
    {
        ACCESS_FORBIDDEN("System:AccessForbidden"),
        PAGE_NOT_FOUND("System:PageNotFound"),
        FILE_NOT_FOUND("System:FileNotFound");
        
        private String name;
        
        private RedirectType(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
    }
    
    public static String getRedirectURL(RedirectType r, HttpServletRequest req)
    {
        return api().getRequestMapper().getExternalUrl(req, getConfig().getUrlMapping() + r.toString() + "?view=wiki");
    }
}
