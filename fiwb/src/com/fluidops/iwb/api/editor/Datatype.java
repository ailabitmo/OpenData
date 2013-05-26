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

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.widget.TripleEditorWidget;

/**
 * Supported datatypes in the Information Workbench. 
 * 
 * These datatypes (mostly XSD + RDFS) are used especially
 * in the {@link TripleEditorWidget} for type selection of
 * the input values.
 * 
 * @author as
 *
 */
public enum Datatype {

	RDFS_LITERAL(RDFS.LITERAL),
	
	RDFS_RESOURCE(RDFS.RESOURCE),
	
	XSD_STRING(XMLSchema.STRING),
	
	XSD_INTEGER(XMLSchema.INTEGER),
	
	XSD_BOOLEAN(XMLSchema.BOOLEAN),
	
	XSD_DATE(XMLSchema.DATE),
	
	XSD_DATETIME(XMLSchema.DATETIME),
	
	XSD_DURATION(XMLSchema.DURATION),
	
	XSD_DECIMAL(XMLSchema.DECIMAL),
	
	XSD_DOUBLE(XMLSchema.DOUBLE);

	
	private URI typeUri;
	
	Datatype(URI typeUri) {
		this.typeUri = typeUri;
	}
	
	public URI getTypeURI() {
		return this.typeUri;
	}	

	/**
	 * Returns a string representation of this datatype, which
	 * corresponds to the abbreviated URI. The default implementation uses
	 * {@link NamespaceService.getAbbreviatedURI(URI)}
	 * which typically returns a prefixed URI string, e.g. "xsd:string"
	 */
	public String toString() {
		return EndpointImpl.api().getNamespaceService().getAbbreviatedURI(typeUri);
	}
	
	static Map<URI, Datatype> supportedTypes = new HashMap<URI, Datatype>();
	static {
		for (Datatype d : Datatype.values())
			supportedTypes.put(d.typeUri, d);
	}
	
	/**
	 * Return a string representation for the given type URI, which
	 * corresponds to the label of the given resource as retrieved via
	 * {@link ReadWriteDataManager#getLabel()}. This method implements
	 * a special case for type URIs rdfs:Resource (=> URI/Resource) and
	 * rdfs:Literal (=> Literal (any/untyped)).
	 * 
	 * @param typeURI
	 * @return
	 */
	public static String getLabel(URI typeURI) {
		if (RDFS.LITERAL.equals(typeURI))
			return "Literal (any/untyped)";
		if (RDFS.RESOURCE.equals(typeURI))
			return "URI/Resource";
		return EndpointImpl.api().getDataManager().getLabel(typeURI);
	}
	
	/**
	 * Return a {@link Validation} for the given type, if available.
	 * Uses {@link DatatypeValidation} to retrieve the known val
	 * @param typeURI
	 * @return
	 */
	public static Validation getValidation(URI typeURI) {
		return DatatypeValidation.validatorFor(typeURI);
	}
}


