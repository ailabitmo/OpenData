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

package com.fluidops.iwb.ajax;

import org.openrdf.model.Value;

/**
 * Interface for FComponents to handle and return their input as an RDF
 * {@link Value}.
 * 
 * @author cp
 */
public interface ValueInput
{
    /**
     * Returns the current input RDF value, or null.
     * 
     * @return
     */
    Value getRdfValue();

    /**
     * Returns whether the current input is empty (which may result in
     * {@link #getRdfValue()} returning null, e.g., when the input is supposed
     * to be a URI or otherwise invalid.
     * 
     * @return
     */
    boolean isEmpty();
}
