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

package com.fluidops.iwb.monitoring;

import com.fluidops.util.StringUtil;


/**
 * Structure to maintain system state information of a particular
 * {@link SystemStateFeature}.
 * 
 * @author as
 */
public class SystemStateInfo {

	private static final long serialVersionUID = 5959577497356165477L;
		
	public static enum State { 
		
		/** Indicates that the state is OK */
		OK, 
		
		/** Indicates that there is an error, details may contain additional info */
		ERROR, 
		
		/** Indicates that this monitoring component is not available in the instance
		 *  e.g. editorial workflow or mysql			 
		 */
		NOT_APPLICABLE 
	}
	
	public SystemStateFeature type;
	public String details = "";
	public State state;	
	
	public String getKey() {
		return type.keyString;
	}
	
	public String getValueHtml() {
		if (state==State.OK)
			return "<div class=\"statusOK\"/>";
		if (state==State.ERROR)
			return "<div class=\"statusError\" title=\"" + details + "\" />";
		return details;
	}
	
	@Override
	public String toString() {
		return type + ": " + state + (!StringUtil.isNullOrEmpty(details) ? "(Details: " + details + ")" : "");
	}
	
	public static SystemStateInfo createNotApplicable(SystemStateFeature type) {
		return create(type, State.NOT_APPLICABLE, "(n/a)");
	}	
	
	public static SystemStateInfo create(SystemStateFeature type, State state, String details) {
		SystemStateInfo res = new SystemStateInfo();
		res.type = type;
		res.state = state;
		res.details = details;
		return res;
	}
}
