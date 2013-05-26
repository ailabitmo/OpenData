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

package com.fluidops.iwb.api;

import java.util.List;
import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * Namespace functionality that is not exposed outside,
 * i.e. accessable only internally.
 * 
 * @author aeb, msc
 */
public interface NamespaceService
{

    /**
     * Register a namespace 
     */
    public void registerNamespace(URI uri,String name);
    
    /**
     * Unregister a namespace abbreviation
     */
    public void unregisterNamespace(String name);

    /**
     * Returns all registered namespaces prefixes and their mapping
     * to full namespaces
     */
    public Map<String,String> getRegisteredNamespacePrefixes();
    
    /**
     * Return the default namespace
     */
    public String defaultNamespace();
    
    /**
     * Return the system namespace
     */
    public String systemNamespace();

    /**
     * Return the system namespace
     */
    public String settingsNamespace();

    /**
     * Return the template namespace
     */
    public String templateNamespace();
    
    /**
     * Return the file namespace
     */
    public String fileNamespace();


    /**
     * We implement a special logic to see whether the URI *startsWith* 
     * the namespace (instead of checking for equality). This allows to 
     * also map e.g. http://www.fluidops.com/foo/bar to /ns/foo/bar
     * (assuming http://www.fluidops.com/ being the namespace in question).
     * 
     * @param uri The URI we want to represent as short name
     * @return The short name
     */
    public String getAbbreviatedURI(URI uri);
    
    /**
     * Gets all abbreviated URIs known for the uri. For instance,
     * for <http://.../comment> we return [rdfs:comment,comment].
     * 
     * @param uri The URI we want to represent as short name
     * @return list of short names (may be empty)
     */
    public List<String> getAbbreviatedURIs(URI uri);
    
    /**
     * Returns the (largest mapping) namespace from the list
     * of loaded namespaces. If the namespace is not matched,
     * null is returned.
     * 
     * @param uri
     * @return
     */
    public String getNamespace(URI uri);
    
    /**
     * Given a URI in prefix form (as string), return the full URI
     */
    public URI getFullURI(String uri);
 
    /**
     * Tries to resolve a prefixed URI using namespace information. May return null
     * if string cannot be treated as URI. If the string is enclosed in "<" and
     * ">" signs, the inner string will be treated as URI.
     */
    public URI guessURI(String name);

    /**
     * Tries to resolve a prefixed URI using namespace information. If the string
     * is enclosed in "<" and ">" signs, the inner string will be treated as URI.
     * 
     * Replaces all illegal URI characters by an underscore. Does never return 
     * null, but note that different strings may be mapped to the same URI, e.g.
     * "Person{A" and "Person}A" will both be mapped to URI "Person_A". In the
     * (very unlikely) case that something goes wrong, a RuntimeException is thrown.
     */
    public URI guessURIOrCreateInDefaultNS(String name);


    /**
     * Creates a URI for the string in the default namespaces. Replaces all
     * illegal URI characters by an underscore. Does never return null, but note
     * that different strings may be mapped to the same URI, e.g. "Person{A" and
     * "Person}A" will both be mapped to URI "Person_A". In the (very unlikely)
     * case that something goes wrong, a RuntimeException is thrown.
     */
    public URI createURIInDefaultNS(String name);    

    /**
     * Tries to resolve a prefixed URI or a value given as a string using namespace information
     */
    public Value guessValue(String valueStr);

    /**
     * Parses a value according to (a slightly simplified) version of the SPARQL
     * GraphTerm specification (cf. W3C SPARQL Grammar "[45] Graph Term").
     * Namespaces are resolved (and if resolving fails, an error is thrown). 
     * 
     * In contrast to the W3C, we do not support blank nodes and no NIL; further
     * the "double" datatype (with exponent notation) is not and we assume there
     * is no escaping in literals. If we need better standard compliance at some
     * time, it would probably be best to integrate the W3C EBNF grammar and use
     * an existing EBNF parser, but for now this is good enough.
     * 
     * @return null if parsing failed, the value otherwise
     */
    public Value parseValue(String s);

    /**
     * Parses a full URI or a prefixed URI, dependent on the first
     * non-whitespace character.
     * 
     * @param s
     */
    public URI parseURI(String s);
    
    /**
     * Parses a prefixed URI like xsd:int
     * 
     * @param s the string to parse
     * @return null if parsing fails, the URI otherwise
     */
    public URI parsePrefixedURI(String s);

    /**
     * Parses a full URI like "<http://www.fluidops.com/test#115>"
     * 
     * @param s the string to parse
     * @return null if parsing fails, the URI otherwise
     */
    public URI parseFullURI(String s);    

    /**
     * Parses a string literal like "test", "test"@de", or "test"^^xsd:string.
     * 
     * @param s the string to parse
     * @return null if parsing fails, the URI otherwise
     */
    public Literal parseStringLiteral(String s);
    
    /**
     * Tries to match the string against existing standard vocabulary. 
     * Returns the URI of the matched vocabulary in case of success,
     * false otherwise.
     * 
     * @param string
     * @return
     */
    public URI matchStandardURI(String string);

}