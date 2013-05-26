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

package com.fluidops.iwb.provider;

import java.io.Serializable;
import java.util.List;

import org.openrdf.model.Statement;

import com.fluidops.api.annotation.OWLClass;
import com.fluidops.api.annotation.OWLProperty;
import com.fluidops.iwb.annotation.AnnotationProcessor;

/**
 * Abstract superclass for a provider based on the annotation framework. Instead
 * of a gather() method, such a provider only needs to implement a method called
 * {@link #getAnnotatedObjects()}, which returns all the instances (whose classes
 * must have valid {@link OWLClass} and {@link OWLProperty} annotations.
 * 
 * The {@link #gather(List)} method then uses the {@link AnnotationProcessor} to
 * extract the statements from the annotated objects.
 * 
 * @author michaelschmidt
 *
 * @param <T> provider configuration
 */
public abstract class AnnotationFrameworkBasedProvider<T extends Serializable> extends AbstractFlexProvider<T>
{
	private static final long serialVersionUID = 3458623249696707197L;

	@Override
	public final void gather(List<Statement> res) throws Exception 
	{
		for (Object o : getAnnotatedObjects())
			AnnotationProcessor.extractStmtsFromObject(o, res);
	}

	/**
	 * Method that returns the list of annotated objects. To be overridden
	 * by extending class.
	 * 
	 * @return the list of objects, all of which must be instances of some annotated class
	 */
	public abstract List<Object> getAnnotatedObjects();
}