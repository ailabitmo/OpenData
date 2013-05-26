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

package com.fluidops.iwb.api.editor;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;

import com.fluidops.util.StringUtil;

/**
 * Factory for triple editor source instances.
 * 
 * @author uli
 */
public class TripleEditorSourceFactory 
{	
	// defaults
	private final static Class<?> DEFAULT_TRIPLE_EDITOR_SOURCE_FOR_URI = TripleEditorSourceURIOnDemand.class;
	private final static Class<?> DEFAULT_TRIPLE_EDITOR_SOURCE_FOR_LITERAL = TripleEditorSourceLiteralImpl.class;
	private final static Class<?> DEFAULT_TRIPLE_EDITOR_SOURCE_FOR_BNODE = TripleEditorSourceBNodeImpl.class;
	
	
	/**
	 * Returns a triple editor source for a URI
	 * 
	 * @param uri the subject
	 * @param clazz class of the triple source, if null or empty the system default is used
	 * @param initialValuesDisplayed number of values displayed in preview
	 * @param includeInverseProperties whether to include inverse properties or not
	 *
	 * @return the triple editor source
	 * 
	 * @throws QueryEvaluationException
	 */
	public static TripleEditorSource tripleEditorSourceForURI(URI uri, String clazz, 
			int initialValuesDisplayed, boolean includeInverseProperties)
	throws QueryEvaluationException
	{
		// set clazz default, if class not provided
		if (StringUtil.isNullOrEmpty(clazz))
			clazz = DEFAULT_TRIPLE_EDITOR_SOURCE_FOR_URI.getName();
		
		TripleEditorSource tes = loadTripleEditorSourceByClazz(clazz);
		if (!(tes instanceof TripleEditorSourceURI))
			throw new IllegalArgumentException("Class " + clazz + " must implement interface " + TripleEditorSourceURI.class.getName() + ", but does not.");

		((TripleEditorSourceURI)tes).initialize(uri, initialValuesDisplayed, includeInverseProperties);
		
		return tes;
	}
	
	/**
	 * Returns a triple editor source for a literal
	 * 
	 * @param literal the subject
	 * @param clazz class of the triple source, if null or empty the system default is used
	 *
	 * @return the triple editor source
	 * 
	 * @throws QueryEvaluationException
	 */
	public static TripleEditorSource tripleEditorSourceForLiteral(Literal literal, String clazz)
	throws QueryEvaluationException
	{
		// set clazz default, if class not provided
		if (StringUtil.isNullOrEmpty(clazz))
			clazz = DEFAULT_TRIPLE_EDITOR_SOURCE_FOR_LITERAL.getName();
		
		
		TripleEditorSource tes = loadTripleEditorSourceByClazz(clazz);
		if (!(tes instanceof TripleEditorSourceLiteral))
			throw new IllegalArgumentException("Class " + clazz + " must implement interface " + TripleEditorSourceLiteral.class.getName() + ", but does not.");

		((TripleEditorSourceLiteral)tes).initialize(literal);
		
		return tes;
	}
	
	/**
	 * Returns a triple editor source for a BNode
	 * 
	 * @param bnode the subject
	 * @param clazz class of the triple source, if null or empty the system default is used
	 * @param includeInverseProperties whether to include inverse properties or not
	 *
	 * @return the triple editor source
	 * 
	 * @throws QueryEvaluationException
	 */
	public static TripleEditorSource tripleEditorSourceForBNode(BNode bNode, 
			String clazz, boolean includeInverseProperties) 
	throws QueryEvaluationException
	{
		// set clazz default, if class not provided
		if (StringUtil.isNullOrEmpty(clazz))
			clazz = DEFAULT_TRIPLE_EDITOR_SOURCE_FOR_BNODE.getName();
		
		TripleEditorSource tes = loadTripleEditorSourceByClazz(clazz);
		if (!(tes instanceof TripleEditorSourceBNode))
			throw new IllegalArgumentException("Class " + clazz + " must implement interface " + TripleEditorSourceBNode.class.getName() + ", but does not.");

		((TripleEditorSourceBNode)tes).initialize(bNode, includeInverseProperties);
		
		return tes;
	}
	
	/**
	 * Loads and casts an instance of TripleEditorSource by class name using 
	 * the default constructor.
	 * 
	 * @param clazz
	 * 
	 * @return the instance
	 */
	private static TripleEditorSource loadTripleEditorSourceByClazz(String clazz)
	{
		try
		{
			Class<?> tripleEditorSourceClass = Class.forName(clazz);
			return (TripleEditorSource)tripleEditorSourceClass.newInstance();
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalArgumentException("Class " + clazz + " is not a valid class");
		}
		catch (InstantiationException e)
		{
			throw new IllegalArgumentException("Class " + clazz + " cannot be instantiated (no default constructor available?)");
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Problem instantiating class " + clazz);
		}
	}
}
