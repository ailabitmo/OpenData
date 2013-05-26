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

package com.fluidops.iwb.util.validator;

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;

/**
 * The validator checks inline if the value can be converted to a valid uri
 */

public class ConvertibleToUriValidator implements Validator
{
    public boolean validate(FComponent c) 
    {   
        URI check;
        try
        {               
           check = EndpointImpl.api().getNamespaceService().guessURI(c.returnValues().toString());
        }
        catch (Exception e) 
        {
            return false;
        }
        if(check==null)
            return false;
        return true;
    }
}
