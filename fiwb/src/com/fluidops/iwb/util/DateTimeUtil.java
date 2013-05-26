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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.Literal;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Utility methods to deal with date time
 *
 */
public class DateTimeUtil {

	/**
     * The timestamp that was used for the last Context. We compare to this
     * value to have unique/synchronized values across different contexts
     */
    private static AtomicLong lastTimestampUsed = new AtomicLong(0L);
	
    /**
	 * Returns a unique timestamp considering the last stored timestamp. If the current
	 * timestamp is equal to the last one, one millisecond is added. The last step is
	 * repeated until a distinct timestamp is found.
	 *  
	 * @return a unique timestamp
	 */
	public static long getTimestampSafe() 
	{    	
		long timestamp = System.currentTimeMillis();
		
		boolean repeat = true;
		while (repeat)
		{
	    	long lastUsed = lastTimestampUsed.get();
	    	if (timestamp>lastUsed) {
	    		repeat = !lastTimestampUsed.compareAndSet(lastUsed, timestamp);
	    	} else {
	    		timestamp = lastUsed+1;
	    	}
		}
		
		return timestamp;
	}
	
	/**
	 * Returns a unique Date considering the last stored timestamp. If the current
	 * timestamp is equal to the last one, one millisecond is added. The last step is
	 * repeated until a distinct timestamp is found.
	 *  
	 * @return a unique Date
	 * @return
	 */
	public static Date getDateSafe() {
		return new Date(getTimestampSafe());
	}
	
	/**
	 * Return the current timestamp formatted using the given pattern.
	 * @param pattern a valid date pattern, e.g. yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	public static String getDate(String pattern) {
		return new SimpleDateFormat(pattern).format(new Date());
	}
	
	/**
	 * Return the given date as an {@link XMLSchema#DATETIME} typed
	 * literal
	 * @param date
	 * @return
	 */
	public static Literal toDateTimeLiteral(Date date) {
		return ValueFactoryImpl.getInstance().createLiteral(
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date), XMLSchema.DATETIME);
	}
}
