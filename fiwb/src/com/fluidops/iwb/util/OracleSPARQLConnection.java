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

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sparql.SPARQLConnection;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * This {@link SPARQLConnection} ignores context information because the Joseki
 * SPARQL endpoint used for Oracle doesn't support named graphs in the FROM
 * clause.
 * 
 * @author christian.huetter
 */
public class OracleSPARQLConnection extends SPARQLConnection
{
	public OracleSPARQLConnection(SPARQLRepository repository,
			String queryEndpointUrl, String updateEndpointUrl)
	{
		super(repository, queryEndpointUrl, updateEndpointUrl);
	}

	@Override
	public void exportStatements(Resource subj, URI pred, Value obj, boolean includeInferred,
			RDFHandler handler, Resource... ignored)
		throws RepositoryException, RDFHandlerException
	{
		// ignore contexts
		super.exportStatements(subj, pred, obj, includeInferred, handler);
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, URI pred,
			Value obj, boolean includeInferred, Resource... ignored)
			throws RepositoryException
	{
		// ignore contexts
		return super.getStatements(subj, pred, obj, includeInferred);
	}

	@Override
	public boolean hasStatement(Resource subj, URI pred, Value obj, boolean includeInferred,
			Resource... ignored)
		throws RepositoryException
	{
		// ignore contexts
		return super.hasStatement(subj, pred, obj, includeInferred);
	}
}
