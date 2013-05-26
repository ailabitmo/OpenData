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

package com.fluidops.iwb.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UrlUtils
{
    
    private static final String DEFULT_ENCODING = "UTF-8";

    public static String urlDecode(String string) {
        try
        {
            return URLDecoder.decode(string, DEFULT_ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Unknown encoding " + DEFULT_ENCODING, e);
        }
    }
    
    public static String urlEncode(String string) {
        try
        {
            return URLEncoder.encode(string, DEFULT_ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Unknown encoding " + DEFULT_ENCODING, e);
        }
    }

}
