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

package com.fluidops.iwb.rdf;

import java.util.Collection;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.api.ListType;
import com.fluidops.api.annotation.OWLClass;
import com.fluidops.api.annotation.OWLLabel;
import com.fluidops.api.annotation.OWLObjectId;
import com.fluidops.api.annotation.OWLProperty;

/**
 * java incarnation of RDF URIs
 * 
 * @author aeb
 */
@OWLClass(className="RDF_URI")
public interface FURI
{
	/**
	 * there's a 1:1 relationship between FURI and URI
	 */
	public URI getURI();
	
	/**
	 * allows access to the underlying RDF statement of a pojo field
	 * @param prop	the RDF property
	 * @return		a statement (this, prop, ?) if one exists, null otherwise
	 */
	public Statement getStatement( String prop );
	
	/**
	 * allows access to the underlying RDF statements of a pojo field
	 * @param prop	the RDF property
	 * @return		list of all statements (this, prop, ?), might be empty
	 */
	public RepositoryResult<Statement> getStatements( String prop );
	
	/**
	 * pre-defined methods for rdf:type
	 */
	@OWLProperty(propName="rdf:type", propLabel="Type") @ListType(URI.class) public Collection<URI> getTypes();
	
	/**
	 * pre-defined methods for rdf:type
	 */
	@OWLProperty(propName="rdf:type", propLabel="Type") @ListType(URI.class) public void setTypes( Collection<URI> types );

	@OWLLabel
	@OWLObjectId
	@Override
	public String toString();
}
