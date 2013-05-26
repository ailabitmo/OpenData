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

package com.fluidops.iwb.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;

import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.cache.PropertyCache.PropertyInfo;
import com.fluidops.iwb.model.Vocabulary;


/**
 * Cache for property ontology information, enabling efficient
 * access to type, domain, and range of properties. Could be
 * easily extended to sub/superproperties once we need it.
 * 
 * @author msc
 */
public class PropertyCache extends RepositoryCache<URI,PropertyInfo>
{
    private static PropertyCache instance = null;
    
    /**
     * Return the one and only instance
     * 
     * @return
     */
    static public PropertyCache getInstance()
    {
        if (instance==null)
            instance = new PropertyCache();
        return instance;
    }
    
    /**
     * Private Constructor (Singleton)
     */
    private PropertyCache()
    {
        super();
    }
    
       
    public static class PropertyInfo
    {
        public static final short OBJECT_PROPERTY = 0;
        public static final short DATATYPE_PROPERTY = 1;
        public static final short UNKNOWN = 2;
        
        /**
         * 
         * @param property as predicate
         * @param dm Opened data manager
         */
        public PropertyInfo(URI property, ReadDataManager dm)
        {
            this.property=property;
            this.label=dm.getLabel(property);
         
            // initialize type
            propertyType = UNKNOWN;
            Statement type = dm.searchOne(property,RDF.TYPE,null);
            if (type != null )
            {
                if (type.getObject().equals(OWL.OBJECTPROPERTY))
                    this.propertyType=OBJECT_PROPERTY;
                else if (type.getObject().equals(OWL.DATATYPEPROPERTY))
                    this.propertyType=DATATYPE_PROPERTY;
            }
            
            // initialize comment
            comment = "";
            Statement st = dm.searchOne(property,RDFS.COMMENT,null);
            if (st != null )
            {
                this.comment = st.getObject().stringValue();
            }
            
            // initialize domain list
            this.dom = dm.getProps(property,RDFS.DOMAIN);
            this.ran = dm.getProps(property, RDFS.RANGE);
            
            // initialize inverse property
            Value inverseProp = dm.getProp(property, OWL.INVERSEOF); 
            this.inverse = (inverseProp instanceof URI)?(URI)inverseProp:null;            
            
            this.types = dm.getType(property);
            
            // initialize editable
            if (types!=null && types.contains(RDFS.CONTAINERMEMBERSHIPPROPERTY))
                this.editable = false; // never edit CM properties
            else
            {
				this.editable = !dm.hasStatement(property, Vocabulary.SYSTEM_CONTEXT.ISEDITABLE, Vocabulary.FALSE, false);
            }
        }
        
        // the property that is described
        private URI property;
        
        // the property that is described
        private URI inverse;
        
        // the label of the property
        private String label;
        
        // property comment
        private String comment;
        
        // is the property editable
        private boolean editable;
        
        // types of this property
        private Set<Resource> types;
        
        /*
         * The following values are possible:
         * 0 -> unknown
         * 1 -> owl:ObjectProperty
         * 2 -> owl:DatatypeProperty 
         */
        private short propertyType;
        
        // domain(s) of predicate according to ontology (may be empty)
        private List<Value> dom;
        
        // range(s) of predicate according to ontology  (may be empty)
        private List<Value> ran;
        
        public URI getProperty()
        {
            return property;
        }
        
        /**
         * Make sure to apply HTML encoding
         * when necessary.
         */
        public String getPropertyLabel()
        {
            return label;
        }
        
        /**
         * Returns the property comment. Make sure to apply HTML encoding
         * when necessary.
         */
        public String getComment()
        {
            return comment;
        }
        
        public boolean getEditable()
        {
            return editable;
        }
        
        public URI getInverse()
        {
            return inverse;
        }
        
        public List<URI> getDom()
        {
            List<URI> cast = new ArrayList<URI>();

            for (Value d : dom)
            {
                if (d instanceof URI)
                   cast.add((URI)d);
                
            }
            return cast;
        }
        
        public List<URI> getRan()
        {
            List<URI> cast = new ArrayList<URI>();

            for (Value r : ran)
            {
               if (r instanceof URI)
                   cast.add((URI)r);
                
            }
            return cast;
        }        
        
        /**
         * Returns information on this property either from direct knowledge
         * or derived from {@link #isObjectPropertyByRange()}
         * 
         * @return true if this property is a object property
         */
        public boolean isKnownObjectProperty()
        {
            return propertyType==OBJECT_PROPERTY || isObjectPropertyByRange();
        }
        
        /**
         * Returns information on this property either from direct knowledge
         * or derived from {@link #isDatatypePropertyByRange()}
         * 
         * @return true if this property is a datatype property
         */
        public boolean isKnownDatatypeProperty()
        {
            return propertyType==DATATYPE_PROPERTY || isDatatypePropertyByRange();
        }
        
        /**
         * Return true iff this property is a data property by its
         * defined range, i.e. if some defined range
         * 
         * a) starts with http://www.w3.org/2001/XMLSchema#
         * b) is XML.Literal
         * c) is RDF.XMLLiteral
         * 
         * @return
         */
        private boolean isDatatypePropertyByRange() {
        	for (URI range : getRan())
            {      	
                if (range.toString().startsWith("http://www.w3.org/2001/XMLSchema#") || range.equals(RDFS.LITERAL) || range.equals(RDF.XMLLITERAL))
                	return true;
            }
        	return false;
        }
        
        /**
         * Returns true if there are ranges defined which are not those
         * of datatype properties, i.e. ones defined by 
         * {@link #isObjectPropertyByRange()}
         * 
         * @return
         */
        private boolean isObjectPropertyByRange() {
        	return ran.size()>0 && !isDatatypePropertyByRange();
        }
        
        /**
         * Debugging output
         */
        @Override
        public String toString()
        {
            StringBuilder s = new StringBuilder();
            s.append("### " + property.stringValue() + "; ");
            s.append("type=");
            if (isKnownObjectProperty())
                s.append("ObjectProperty");
            else if (isKnownDatatypeProperty())
                s.append("DatatypeProperty");
            else
                s.append("unknown");
            s.append("; dom=");
            for (URI d : getDom())
            {
                s.append(d.stringValue());
                s.append("#");
            }
            s.append(" ran=");
            for (URI r : getRan())
            {
                s.append(r.stringValue());
                s.append("#");
            }
            
            return s.toString();
        }
        
        public Set<Resource> getTypes() 
        {
        	return types;
        }
        
        public void setPropertyType(short type)
        {
        	this.propertyType = type;
        }
    }
    
    @Override
    public void updateCache(Repository rep, Resource u)
    {
        Map<URI,PropertyInfo> repCache = cache.get(rep);
        if (repCache!=null)
            repCache.remove(u);
    }
}
