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

package com.fluidops.iwb.mapping;

import java.util.Collection;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import com.fluidops.util.Pair;

/**
 * interface that helps providers to either generate owl:sameAs
 * or align URIs before entering data into the triplestore
 * 
 * the API is inspired by the SILK Link specification language:
 * http://www4.wiwiss.fu-berlin.de/bizer/silk/spec/
 * 
 * It usually defines 2 datasources:
 * source: the IWB repository
 * target: the provider graph passed on as a List of Statements
 * 
 * LinkCondition: currently simplified to a Pair of predicate URIs. 
 * TODO: generalize to support more complex constructs
 * 
 * output: the API modified the target set directly. 
 * If this is not to be done, the client must clone the input graph first
 * 
 * @author aeb
 */
public interface AlignmentOperator
{
	public List<Pair<Resource, Resource>> align( Collection<Statement> stmts );
}
