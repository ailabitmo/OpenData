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

package com.fluidops.iwb.api.operator;

import java.util.List;

import org.openrdf.model.Value;

import com.google.common.collect.Lists;

/**
 * Operator utility functions.
 * 
 * @author uli
 *
 */
public class OperatorUtil {	

	
	public static String removeEnclosingTicks(String serialized) {
		String token = serialized.startsWith("'") ? serialized.substring(1) : serialized;
		token = token.endsWith("'") ? token.substring(0, token.length()-1) : token;
		return token;
	}
	
	
	/**
	 * Replace special tokens in the input string,
	 * 
	 * {{Pipe}} => |
	 * 
	 * @param input
	 * @return
	 */
	public static String replaceSpecialTokens(String input) {
		input = input.replaceAll("\\{\\{Pipe\\}\\}", "|");

		return input;
	}
	
	
	/**
	 * Tries a conversion of the given value to the expected target type.
	 * 
	 * Currently supports:
	 * String => {@link Value#stringValue()}
	 * 
	 * @param v
	 * @param targetType
	 * @return
	 */
	public static Object toTargetType(Value v, Class<?> targetType) {
		if (v==null)
			return null;
		if (targetType.equals(String.class))
			return v.stringValue();	
		if (targetType.equals(List.class))
			return Lists.newArrayList(v);
		return v;	
	}
}
