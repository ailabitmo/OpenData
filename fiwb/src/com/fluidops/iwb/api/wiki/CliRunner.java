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

package com.fluidops.iwb.api.wiki;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.fluidops.api.Cli2;
import com.fluidops.iwb.api.API;
import com.fluidops.iwb.api.APIImpl;
import com.fluidops.util.Parse;

public class CliRunner
{
    public static final Scanner input;
    
    static {
        input = new Scanner(System.in);
    }
    
    public static void main(String[] args) throws IOException
    {
        APIImpl impl = new APIImpl();
        Method[] methods = API.class.getMethods();
        HashMap<String, Class<?>[]> method_table = new HashMap<String, Class<?>[]>(methods.length);
        
        for (Method m : methods) {
            method_table.put(m.getName(), m.getParameterTypes());
        }
        
        final String[] signature;
        if (args.length > 0) {
            signature = args;
        } else {
            System.out.println("please enter the command [CLI2 syntax]:\n>");
            List<String> lineargs = Cli2.args( input.nextLine() );
            signature = lineargs.toArray(new String[lineargs.size()]);
        }
            
        String mname = signature[0];
        Class<?>[] parameterTypes = method_table.get(mname);
        try {
            Method m = impl.getClass().getMethod(mname, parameterTypes);
            Object[] parsed = new Object[parameterTypes.length];
            
            for (int i = 0; i < parameterTypes.length; i++) {
                parsed[i] = Parse.parse(signature[i+1], parameterTypes[i]);
            }
            m.invoke(impl, parsed);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
