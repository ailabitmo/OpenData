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

class Page
{
    final StringBuilder title;
    final StringBuilder text;
    int processed;
    boolean isRedirect;

    Page()
    {
        title = new StringBuilder(40);
        text = new StringBuilder(1000);
        processed = 0;
    }
    
    void processed(int who) {
        processed |= 1 << who;
    }

    boolean isProcessed(int consumerNumber)
    {
        return (processed & (1 << consumerNumber)) != 0;
    }
    
    void reset() {
        title.setLength(0);
        text.setLength(0);
        processed = 0;
        isRedirect = false;
    }
}