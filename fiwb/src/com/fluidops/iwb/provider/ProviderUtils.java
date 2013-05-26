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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.GraphQueryResult;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.provider.ProviderUtils.StatementFilter.FilterMode;
import com.fluidops.util.StringUtil;

/**
 * Convenience methods for statement creation and cleansing, to be used by providers.
 * 
 * @author msc
 */
public class ProviderUtils 
{
	private static ValueFactory vf = ValueFactoryImpl.getInstance();
	
	/**
	 * Creates a statement, basically wrapping Sesame's value factory.
	 * 
	 * @param subject the statement's subject
	 * @param predicate the statement's predicate
	 * @param object the statement's object
	 * @return the created statement or null if one of the parameters was null
	 */
	public static Statement createStatement(Resource subject, URI predicate, Value object)
	{
		if (subject==null || predicate==null || object==null)
			return null;

		return vf.createStatement(objectToUri(subject), predicate, object);
	}
	
	/**
	 * Creates a statement, basically wrapping Sesame's value factory. The object in
	 * subject position is converted to a URI based on its toString() method.
	 * 
	 * @param subjectObj the object to be converted to the subject URI
	 * @param predicate the statement's predicate
	 * @param object the statement's object
	 * @return the created statement or null if one of the parameters was null
	 */
	public static Statement createStatement(Object subjectObj, URI predicate, Value object)
	{
		if (subjectObj==null)
			return null;

		return vf.createStatement(objectToUri(subjectObj), predicate, object);
	}

	/**
	 * Creates a statement, basically wrapping Sesame's value factory. The objects in
	 * subject and object position are converted to a URI based on their toString() methods.
	 * 
	 * @param subjectObj the object to be converted to the subject URI
	 * @param predicate the statement's predicate
	 * @param objectObj the object to be converted to the object URI
	 * @return the created statement or null if one of the parameters was null
	 */
	public static Statement createUriStatement(Object subjectObj, URI predicate, Object objectObj)
	{
		if (subjectObj==null || objectObj==null)
			return null;
		
		return vf.createStatement(objectToUri(subjectObj), predicate, objectToUri(objectObj));
	}

	/**
	 * Creates a statement, basically wrapping Sesame's value factory. The object in
	 * object position is converted to a URI based on its toString() method.
	 * 
	 * @param subject the statements subject
	 * @param predicate the statement's predicate
	 * @param objectObj the object to be converted to the object URI
	 * @return the created statement or null if one of the parameters was null
	 */
	public static Statement createUriStatement(Resource subject, URI predicate, Object objectObj)
	{
		if (objectObj==null)
			return null;

		return vf.createStatement(subject, predicate, objectToUri(objectObj));
	}

	
	/**
	 * Creates a statement where (1) for the subject string a URI is guessed according
	 * to internal guessing mechanisms and (2) the literal will become a typed literal 
	 * statement based on a mapping between java object types and xsd datatypes,
	 * where string literals remain untyped. This means that for objects like Integer,
	 * Double, etc., corresponding xsd datatypes are used. In case the object is a
	 * complex object, toString() is used to get a string representation. Returns null
	 * if casting of the object fails.
	 * 
	 * @param subjectObj the object whose string representation will be converted to the subject URI
	 * @param predicate the predicate of the triple to be created
	 * @param objectObj the object whose string representation will be converted to the object literal
	 * @return the statement or null if statement creation failed
	 */
	public static Statement createLiteralStatement(Object subjectObj, URI predicate, Object objectObj)
	{
		return createLiteralStatement(objectToUri(subjectObj), predicate, objectObj);
	}
	
	/**
	 * Creates a typed literal statement based on a mapping between 
	 * java object types and xsd datatypes, where string literals remain untyped.
	 * This means that for objects like Integer, Double, etc., corresponding xsd
	 * datatypes are used. In case the object is a complex object, toString() is
	 * used to get a string representation. Returns null if casting of the object fails.
	 * 
	 * @param subject the subject of the triple to be created
	 * @param predicate the predicate of the triple to be created
	 * @param objectObj the object whose string representation will be converted to the object literal
	 * @return the statement or null if statement creation failed

	 */
	public static Statement createLiteralStatement(Resource subject, URI predicate, Object objectObj)
	{
		if (subject==null || predicate==null || objectObj==null)
			return null;
		
		Literal object = objectToTypedLiteral(objectObj);	
		if (object==null)
			return null; // casting failed for some reason
		
		return vf.createStatement(subject,predicate,object);
	}
	
	/**
	 * Creates default RDF vocabulary information for the subject (where the subject's URI
	 * is derived from the object's toString() method), i.e. associating an
	 * rdf:type, rdfs:label, and rdfs:comment. All parameters except the subject may be
	 * null (in which case the statement is not written).
	 * 
	 * @param subject the subject for which to write the information
	 * @param type the type of the subject (may be null)
	 * @param label the label of the subject (may be null)
	 * @param comment the comment associated with the subject (may be null)
	 * @return up to three statements containing the statement's type, label, and comment; never null
	 */
	public static List<Statement> createDefaultRdfStatements(Object subjectObj,URI type,String label,String comment)
	{
		if (subjectObj==null)
			return new ArrayList<Statement>();
		
		return createDefaultRdfStatements(objectToUri(subjectObj), type, label, comment);
	}

	/**
	 * Creates default RDF vocabulary information for the subject, i.e. associating an
	 * rdf:type, rdfs:label, and rdfs:comment. All parameters except the subject may be
	 * null (in which case the statement is not written).
	 * 
	 * @param subject the subject for which to write the information
	 * @param type the type of the subject (may be null)
	 * @param label the label of the subject (may be null)
	 * @param comment the comment associated with the subject (may be null)
	 * @return up to three statements containing the statement's type, label, and comment; never null
	 */
	public static List<Statement> createDefaultRdfStatements(Resource subject, URI type, String label,String comment)
	{
		if (subject==null || StringUtil.containsNonIriRefCharacter(subject.toString(),false))
			return new ArrayList<Statement>();
		
		List<Statement> res = new ArrayList<Statement>();
		if (type!=null)
			res.add(vf.createStatement(subject, RDF.TYPE, type));
		if (label!=null)
			res.add(vf.createStatement(subject, RDFS.LABEL, objectToTypedLiteral(label)));
		if (comment!=null)
			res.add(vf.createStatement(subject, RDFS.COMMENT, objectToTypedLiteral(comment)));
		
		return res;
	}

	/**
	 * Filters a list of statements with a single of filters. The statements must not
	 * contain null values in their positions.
	 * 
	 * @param stmts the statements to be filtered
	 * @param filterList the list of filters (may be null or empty)
	 * @return the filtered list of statement
	 */
	public static List<Statement> filterStatements(List<Statement> stmts, StatementFilter filter)
	{
		// call the more generic method with filter list:
		StatementFilterList filterList = new StatementFilterList();
		filterList.addStatementFilter(filter);		
		return filterStatements(stmts,filterList);
	}
	
	/**
	 * Filters a list of statements with a list of filters. The statements must not
	 * contain null values in their positions.
	 * 
	 * @param stmts the statements to be filtered
	 * @param filterList the list of filters (may be null or empty)
	 * @return the filtered list of statement
	 */
	public static List<Statement> filterStatements(List<Statement> stmts, StatementFilterList filterList)
	{
		return filterStatements(stmts,false,false,filterList);
	}
	
	/**
	 * Generic filter method having the capabilities to
	 * (1) make a list of statements null-free,
	 * (2) assert that the statements contain valid URIs,
	 * (3) applies a list of user-defined filters.
	 * The three mechanisms can be arbitrarily combined, with the exception that
	 * filtering with user-defined filters implicitly assumes that the statements
	 * do not contain null values in their positions. However, when setting the
	 * flag to make the statements null-free, null statements are filtered away
	 * prior to applying the user-defined filters.
	 * 
	 * @param stmts the statements to be filtered
	 * @param filterNullStatements flag to filter away statements with null in any position
	 * @param filterInvalidStatements flag filter away statements with invalid RDF primitives in any position
	 * @param additionalFilter additional filter specification
	 * @return the filtered list of statement, may be null
	 */
	public static List<Statement> filterStatements(List<Statement> stmts, boolean filterNullStatements,
			boolean filterInvalidStatements, StatementFilterList filterList)
	{
		List<Statement> filteredStmts = new LinkedList<Statement>();
		for (Statement stmt : stmts)
		{
			if (stmt!=null)
			{
				Resource subject = stmt.getSubject();
				Resource predicate = stmt.getPredicate();
				Value object = stmt.getObject();
				
				// (1) filterNullStatements enabled: all positions must differ from null
				if (filterNullStatements)
				{
					if (subject==null || predicate==null || object==null)
						continue; // invalid statement
				}
				
				// (1) filterInvalidStatements enabled: all positions must differ from null
				if (filterInvalidStatements)
				{
					try
					{
						assertValidRDFPrimitive(subject);
						assertValidRDFPrimitive(predicate);
						assertValidRDFPrimitive(object);
					}
					catch (IllegalArgumentException e)
					{
						continue; // invalid statement
					}
				}
				
				// (3) filter defined: apply additional filters
				if (filterList!=null)
				{
					boolean filtered = false;
					for (StatementFilter filter : filterList.getStatementFilters())
					{
						if (!passesThroughFilter(stmt,filter))
						{
							filtered=true;
							continue; // seen enough...
						}
					}
					
					if (filtered)
						continue; 
				}
				
				filteredStmts.add(stmt);
			}
		}
		
		return filteredStmts;
	}

	/**
	 * Removes null values and statements containing null values in their positions
	 * from a list of statements.
	 * 
	 * @param stmts the statements to be filtered
	 * @return the filtered list of statement
	 */
	public static List<Statement> filterNullStatements(List<Statement> stmts)
	{
		return filterStatements(stmts,true,false,null);
	}
	
	/**
	 * Removes statements containing invalid RDF primitives from a list of statements.
	 * 
	 * @param stmts the statements to be filtered
	 * @return the filtered list of statement
	 */
	public static List<Statement> filterInvalidStatements(List<Statement> stmts)
	{
		return filterStatements(stmts,false,true,null);
	}
	
	/**
	 * Removes null values and statements containing null values in their positions
	 * from a list of statements and removes statements containing invalid RDF primitives.
	 * 
	 * @param stmts the statements to be filtered
	 * @return the filtered list of statement
	 */
	public static List<Statement> filterNullAndInvalidStatements(List<Statement> stmts)
	{
		return filterStatements(stmts,true,true,null);
	}
	
	/**
	 * Checks whether a statement passes through a filter. The statement must be a
	 * valid RDF statement, containing no null values; in case it contains any
	 * null values, false is returned.
	 * 
	 * @param stmt the statement to be filtered
	 * @param filter the {@link StatementFilter} to be applied
	 * @return true iff the statement passes through the filter
	 */
	public static boolean passesThroughFilter(Statement stmt, StatementFilter filter)
	{
		if (filter==null)
			return true; // no filter defined means the statement always passes
		
		Resource subject = stmt.getSubject();
		Resource predicate = stmt.getPredicate();
		Value object = stmt.getObject();
		
		// make sure the statement is valid
		if (subject==null || predicate==null || object==null)
			return false;
		
		// the statement must pass the filter defined over all three positions
		boolean passesThroughFilter = true;
		passesThroughFilter&=passesThroughFilter(subject,filter.subjectFilter,filter.filterMode);
		passesThroughFilter&=passesThroughFilter(predicate,filter.predicateFilter,filter.filterMode);
		passesThroughFilter&=passesThroughFilter(object,filter.objectFilter,filter.filterMode);
		
		return passesThroughFilter;
	}

	/**
	 * Checks whether a single value passes through a filter in the respective filter mode.
	 * 
	 * @param value must be not null
	 * @param filter the string or regexp to be filtered away
	 * @param filterMode the mode of the filter
	 * @return true iff the statement passes through the filter
	 */
	private static boolean passesThroughFilter(Value value, String filter, FilterMode filterMode)
	{
		switch (filterMode)
		{
		case STRING:
			return filter==null || !value.stringValue().equals(filter);
		case REGEXP:
			return filter==null || !value.stringValue().matches(filter);
		default:
			return false; // should never happen
		}
	}

	/**
	 * Asserts the RDF primitive is valid.
	 * 
	 * @param value the RDF primitive
	 * @throws IllegalArgumentException if the argument is not valid
	 */
	public static void assertValidRDFPrimitive(Value value) throws IllegalArgumentException
	{
		// currently this is the only known case where we might have invalid RDF: a URI with invalid chars
		if (value instanceof URI && StringUtil.containsNonIriRefCharacter(((URI)value).stringValue(),false))
			throw new IllegalArgumentException("Non-URI character in " + ((URI)value).stringValue());
	}
	
    /**
     * Convert a {@link String} to an untyped literal value.
	 * In case the passed parameter is null, the function returns null.
     */
	public static Literal toLiteral(String s)
    {
        return s==null?
        	null:vf.createLiteral(String.valueOf(s));
    }
	
	/**
	 * Converts a {@link Byte} to an xsd:integer-typed literal value. 
	 * In case the passed parameter is null, the function returns null.
	 */
	public static Literal toLiteral(Byte b)
	{
	    return b==null?
	    	null:vf.createLiteral(String.valueOf(b),XMLSchema.INTEGER);
	}
	
    /**
     * Converts a {@link Double} to an xsd:double-typed literal value.
	 * In case the passed parameter is null, the function returns null.
     */
    public static Literal toLiteral(Double d)
    {
        return d==null?
        	null:vf.createLiteral(String.valueOf(d),XMLSchema.DOUBLE);
    }
    
    /**
     * Converts an {@link Integer} to an xsd:integer-typed literal value.
	 * In case the passed parameter is null, the function returns null.
     */
    public static Literal toLiteral(Integer i)
    {
        return i==null?
        	null:vf.createLiteral(String.valueOf(i),XMLSchema.INTEGER);
    }
    
    /**
     * Converts a {@link Float} to an xsd:double-typed literal value.
	 * In case the passed parameter is null, the function returns null.
     */
    public static Literal toLiteral(Float f)
    {
        return f==null?
        	null:vf.createLiteral(String.valueOf(f),XMLSchema.DOUBLE);
    }

    /**
     * Converts a short {@link Short} to an xsd:integer literal value.
	 * In case the passed parameter is null, the function returns null.
     */
    public static Literal toLiteral(Short s)
    {
        return s==null?
        	null:vf.createLiteral(String.valueOf(s),XMLSchema.INTEGER);
    }
    
    /**
     * Convert a {@link Long} to an xsd:integer-typed literal value.
	 * In case the passed parameter is null, the function returns null.
     */
    public static Literal toLiteral(Long l)
    {
        return l==null?
        	null:vf.createLiteral(String.valueOf(l),XMLSchema.INTEGER);
    }
    
    /**
     * Converts a {@link Boolean} to an xsd:boolean-typed literal value.
	 * In case the passed parameter is null, the function returns null.
     */
    public static Literal toLiteral(Boolean b)
    {
        return b==null?
        	null:vf.createLiteral(String.valueOf(b),XMLSchema.BOOLEAN);
    }

    /**
     * Converts a java object to a typed literal, based on a mapping between
     * java datatypes and XSD datatypes. String literals are not typed. The
     * same applies to objects of unknown types, where we simply use toString()
     * to serialize the object to RDF.
     * 
     * @param object the object to be converted to a literal
     * @return the literal or null, if conversion fails
     */
	public static Literal objectToTypedLiteral(Object object)
	{
		if (object==null)
			return null;
		
		Class<?> objectClass = object.getClass();
		Literal lit = null;
		if (objectClass.equals(Double.class))
			lit = toLiteral((Double)object);
        else if (objectClass.equals(Integer.class))
        	lit = toLiteral((Integer)object);
        else if (objectClass.equals(Float.class))
        	lit = toLiteral((Float)object);
        else if (objectClass.equals(Short.class))
        	lit = toLiteral((Short)object);
        else if (objectClass.equals(Long.class))
        	lit = toLiteral((Long)object);
        else if (objectClass.equals(Byte.class))
        	lit = toLiteral((Byte)object);
        else if (objectClass.equals(Boolean.class))
        	lit = toLiteral((Boolean)object);
        else
        	lit = toLiteral(object.toString());
		
		return lit;
	}
	
	/**
	 * Creates a valid URI for the given object, assuming that the object's string
	 * representation represents a valid URI. This essentially wraps the
	 * ValueFactory.createUri() method and, in addition, replaces non-valid
	 * URI characters by underscores. May return null if the object is empty,
	 * null, or if the object's string representation does not represent a
	 * structurally valid URI (e.g., if the protocol identifier is missing).
	 */
	public static URI objectAsUri(Object object)
	{
		if (object==null || object.toString()==null)
			return null;
		
		return vf.createURI(StringUtil.replaceNonIriRefCharacter(object.toString(), '_'));
	}
	
	/**
	 * Creates a valid URI for the given object using the objects toString()
	 * method, based on our internal URI guessing mechanisms. This means, the
	 * input's string representation can be either a bare string Person1,
	 * a prefixed string link "foaf:Person", or a full URI like "<http://www.fluidops.com>".
	 * Does not return null except in case that object==null or object.toString()==null.
	 */
	public static URI objectToUri(Object object)
	{
		if (object==null || object.toString()==null)
			return null;
		
		return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(object.toString());
	}
	
	/**
	 * Converts an object to a literal in the specified namespace based on the
	 * object's toString() method. The namespace must not be null. The URI is
	 * composed by concatenating the namespace and the objects string representation,
	 * thereby making sure that no non-URI-compatible characters are replaced
	 * by an underscore character.
	 * 
	 * The method returns null in the following cases:
	 * (i) object==null || object.toString()==null
	 * (ii) namespace==null
	 * (iii) namespace+object.toString() is structurally not a valid URI.
	 * 
	 * In particular, if a URI is generated this URI is a valid URI.
	 */
	public static URI objectToURIInNamespace(String namespace, Object object)
	{
		if (object==null || object.toString()==null || namespace==null)
			return null;
		
		return vf.createURI(namespace, StringUtil.replaceNonIriRefCharacter(object.toString(),'_'));
	}
	
	/**
	 * Creates a fresh blank node.
	 */
	public static BNode freshBlankNode()
	{
		return vf.createBNode();
	}
	
	/**
	 * Wraps a URI to a string, e.g. to be used inside a SPARQL query.
	 */
	public static String uriToQueryString(URI u)
	{
		return "<" + u.stringValue() + ">";
	}
    
	/**
	 * Filter structure for statements. Offers interfaces to inject
	 * filter strings for subject, predicate, and object position.
	 * Supports both String and RegExp matching (where the default
	 * is regexp matching).
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UWF_NULL_FIELD", justification="Fields are used externally.")
	public static class StatementFilter
	{
		public static enum FilterMode
		{
			STRING,
			REGEXP
		}
		
		/**
		 * Filter for subject position
		 */
		public String subjectFilter;
		
		/**
		 * Filter for predicate position
		 */
		public String predicateFilter;
	
		/**
		 * Filter for object position
		 */
		public String objectFilter;
		
		/**
		 * Filter mode (i.e., whether to filter by regexp or string)
		 */
		public FilterMode filterMode;
		
		/**
		 * Creates and initializes a statement filter
		 */
		public StatementFilter()
		{
			subjectFilter = null;
			predicateFilter = null;
			objectFilter = null;
			filterMode = FilterMode.REGEXP; // default
		}
	}

	/**
	 * A collection of {@link StatementFilter} objects.
	 */
	public static class StatementFilterList
	{
		private Collection<StatementFilter> statementFilters;
		
		public StatementFilterList()
		{
			statementFilters = new HashSet<StatementFilter>();
		}
		
		public void addStatementFilter(StatementFilter statementFilter)
		{
			statementFilters.add(statementFilter);
		}
		
		public Collection<StatementFilter> getStatementFilters()
		{
			return statementFilters;
		}
	}

	/**
	 * Appends a graph query result to a list of statements and closes the iterator.
	 * @param iterator the result iterator
	 * @param res the list where to append the statements
	 * @throws RuntimeException in case someting goes wrong
	 */
	public static void appendGraphQueryResultToListAndClose(GraphQueryResult result, List<Statement> res)
	{
		try
		{
			while (result.hasNext())
				res.add(result.next());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			try
			{
				result.close();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
