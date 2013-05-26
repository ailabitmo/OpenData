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

package com.fluidops.iwb.xml;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * utility class for mapping the DOM child iteration to
 * Java5 iteration
 * 
 * @author aeb
 */
public class ChildElements extends ArrayList<Element>
{
    /**
     * present node children as java5 list
     */
    public ChildElements( Node node )
    {
        this( node.getChildNodes() );
    }
    
    public ChildElements( NodeList kids )
    {
        for ( int k=0; k<kids.getLength(); k++ )
        {
            Node kid = kids.item(k);
            if ( kid instanceof Element )
                add( (Element)kid );
        }
    }
}
