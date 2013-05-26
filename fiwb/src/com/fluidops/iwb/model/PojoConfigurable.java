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

package com.fluidops.iwb.model;

import java.util.List;

/**
 * widgets, services and providers can all be configured via a pojo.
 * this interface is implemented by all of them and allows determining
 * which inputs there are, which ones are required, etc. This
 * info can be specified manually as well as via the default
 * reflection / metadata implementation
 * 
 * @author aeb
 */
public interface PojoConfigurable<C>
{
    /**
     * returns the class of the config pojo
     */
    public Class<? extends C> getConfigClass();

    /**
     * which capabilities are there.
     */
    public List<Capability> getCapabilities();
    
    /**
     * of the props returned by getProps, which ones are
     * required to be present
     */
    public List<String> getRequiredProps();
    
    /**
     * which props are there. note that this might differ from the
     * fields in the config class since some fields might be unused
     * and some might be obtained manually via model.search( __resource )
     */
    public List<String> getProps();
    
    /**
     * given a prop name, returns its documentation
     */
    public String getPropDoc( String prop );
    
    /**
     * given a config, determines whether the operation can be performed.
     * this is even more fine grained than getRequiredProps(), since
     * there might be requirement that one of 2 optional params must be
     * given. This cannot be expressed via getRequiredProps 
     */
    public boolean check( C config );
}
