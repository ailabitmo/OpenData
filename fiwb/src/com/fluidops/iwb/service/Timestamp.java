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

package com.fluidops.iwb.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.Function;


/**
 * Returns the current timestamp in ms.
 * 
 * Example: 
 * 
 * SELECT * WHERE { 
 *   BIND (<http://www.fluidops.com/service/timestamp>() AS ?x) 
 * }
 * 
 * @author msc
 */
public class Timestamp implements Function
{
	public static final String NAMESPACE = "http://www.fluidops.com/service/";
	
	protected static final Logger logger = Logger.getLogger(Timestamp.class.getName());


	@Override
	public String getURI()
	{
		return NAMESPACE + "timestamp";
	}

	@Override
	public Value evaluate(ValueFactory arg0, Value... arg1)
			throws ValueExprEvaluationException {
		if(arg1.length > 0)
		{
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");			
			
				try
				{
					return arg0.createLiteral(df.parse(arg1[0].stringValue()).getTime());
				}
				catch (ParseException e)
				{
					throw new ValueExprEvaluationException();
				}

		}
		return arg0.createLiteral(System.currentTimeMillis());
	}
}