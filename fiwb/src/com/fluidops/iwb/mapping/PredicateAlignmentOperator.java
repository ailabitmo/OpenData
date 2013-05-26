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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.Pair;

public class PredicateAlignmentOperator implements AlignmentOperator
{
	public URI source;
	public URI target;
	
	public PredicateAlignmentOperator( URI source, URI target )
	{
		this.source = source;
		this.target = target;
	}

	@Override
	public List<Pair<Resource, Resource>> align(Collection<Statement> stmts)
	{
		List<Pair<Resource, Resource>> res = new ArrayList<Pair<Resource,Resource>>();
		for ( Statement s : stmts )
		{
			if ( s.getPredicate().equals( source ) )
			{
				Resource match = EndpointImpl.api().getDataManager().getInverseProp(s.getObject(), target);
				if ( match != null )
				{
					res.add(new Pair<Resource, Resource>( s.getSubject(), match ));
				}
			}
		}
		return res ;
	}
}
