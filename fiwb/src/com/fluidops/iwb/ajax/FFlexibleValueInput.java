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

public class FFlexibleValueInput extends FValueTextInputBase
{

    public FFlexibleValueInput(String id, Value initValue)
    {
        super(id, initValue);
    }

    public FFlexibleValueInput(String id)
    {
        super(id);
    }

    /**
     * Changes the admissible input, allows or disallows resources (i.e.,
     * primarily URIs) as user input. Initially, all supported inputs are
     * allowed.
     * 
     * @param allow
     */
    public void allowResource(boolean allow)
    {
        allowResource = allow;
    }

    /**
     * Changes the admissible input, allows or disallows literals as user input.
     * Initially, all supported inputs are allowed.
     * 
     * @param allow
     */
    public void allowLiteral(boolean allow)
    {
        allowLiteral = allow;
    }
}
