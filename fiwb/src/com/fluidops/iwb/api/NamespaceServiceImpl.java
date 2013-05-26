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

import static java.lang.Character.toChars;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.StringUtil;
import com.fluidops.util.persist.Properties;
import com.fluidops.util.persist.TransactionalFile;
import com.google.common.base.Joiner;

/**
 * 
 * @author msc, pha
 *
 */
public class NamespaceServiceImpl implements NamespaceService
{
    private static final Logger logger = Logger.getLogger(NamespaceServiceImpl.class.getName());

    /**
     * Namespace store
     */
    Properties store;

    /**
     * The store, ordered by the namespace name length in descending length,
     * because sometimes we want to resolve longer prefixes first, e.g. if we have
     *   ns1 -> http://www.fluidops.com
     *   ns2 -> http://www.fluidops.com/help
     * and resolve http://www.fluidops.com/help/help
     * we actually want to resolve it to ns:help rather than ns1:help/help.
     */
    private SortedSet<Entry<Object,Object>> namespacesByDescSize;
    
    /**
     * Predefined namespaces
     */
    private Map<String, String> predefinedNamespaces;
    /**
     * Value factory used internally
     */
    private static final ValueFactory valueFactory = new ValueFactoryImpl();

    /**
     * Regex pattern for a valid SPARQL local name.
     */
    private Pattern sparqlLocalNamePattern;

	private Pattern sparqlNamespacePrefixPattern;
    
    /**
     *  the template namespace where templates are stored
     *  It cannot be changed.
     */
    private static final String templateNS = "Template:";
    
    /**
     *  the file namespace where uploaded are stored
     *  It cannot be changed.
     */
    private static final String fileNS = "File:";

    
    @Override
    public void registerNamespace(URI namespace, String abbreviatedNamespaceName)
    {	
    	validateNamespace(abbreviatedNamespaceName, namespace.stringValue());
    	store.setProperty(abbreviatedNamespaceName, namespace.stringValue());

    	synchronizeSortedStore();
    }


    /**
     * checks the correctness of namespace according to the list of predefined namespaces
     * @param abbreviatedNamespaceName
     * @param namespace
     * @return boolean
     */
    private void validateNamespace(String abbreviatedNamespaceName,
    		String namespace)
    {
    	String correctNamespace = predefinedNamespaces.get(abbreviatedNamespaceName);
    	if(correctNamespace!=null && !namespace.equals(correctNamespace))
    		throw new IllegalArgumentException("Incorrect namespace for "+abbreviatedNamespaceName+" : " + namespace + ". " +
    				"The namespace "+correctNamespace+" must not be modified.");
    	if(!sparqlNamespacePrefixPattern.matcher(abbreviatedNamespaceName).matches())
    		throw new IllegalArgumentException(format("Incorrect namespace prefix '%s' according to SPARQL 1.1 PREFIX specification: http://www.w3.org/TR/sparql11-query/#rPN_PREFIX", 
    				abbreviatedNamespaceName));
    	
    }


	@Override
    public Map<String,String> getRegisteredNamespacePrefixes() 
    {
        Map<String,String> mapping = new HashMap<String,String>();
        for (Object key : store.keySet())
            mapping.put((String)key,store.getProperty((String)key));
        return mapping;
    }
    
    @Override
    public void unregisterNamespace(String name)
    {
        store.setProperty(name, null);
        synchronizeSortedStore();
    }

    protected NamespaceServiceImpl() {
    	this(Config.getConfig().getWorkingDir() + "config/namespaces.prop");
    }
    
    /**
     * 
     * @param namespacesFile
     * 				the location of the namespaces configuration file
     */
    protected NamespaceServiceImpl(String namespacesFile)
    {
    	initPatternForSparqlGrammerChecks();
        store = new Properties( new TransactionalFile(namespacesFile) );

        predefinedNamespaces = getPredefinedNamespaces();

        // protected namespaces: Admin, Help and config are not to be set by the user
        // since if one of those is missing, many functionalities won't work anymore
        // make sure standard namespaces like rdf, rdfs, and default are
        // always found, even if namespaces.prop is not found
        // namespaces rdf, rdfs, owl and xsd have to be set and are not allowed to be overwritten
        
       for( Entry<String, String> namespaceMapping : predefinedNamespaces.entrySet())
       {
    	   setNamespace(namespaceMapping.getKey(), namespaceMapping.getValue());
       }
        
        //default is allowed to be overwritten, but has to be set
        if ( ! store.containsKey( "default" ))
            store.put("default", "http://www.fluidops.com/resource/");
            
        HashMap<String, String> map = new HashMap<String, String>();
        for (Object key : store.keySet()) {
        	
        	String ns = (String)store.getProperty((String)key);
        	char last = ns.charAt(ns.length()-1);
        	if (last >= 65 && last <= 122){
        		ns += "/";
        		map.put((String)key, ns);
        	}
        }
        
        for (Entry<String, String> entry : map.entrySet())  {
        	store.remove(entry.getKey());
        	store.put(entry.getKey(), entry.getValue());
        }

        synchronizeSortedStore();
        
        // don't forget to synchronize the sorted store
    }

	private void initPatternForSparqlGrammerChecks() {
		// initialize regex pattern for SPARQL local names
        String PN_CHARS_BASE =
                String.format("[A-Za-z\u00C0-\u00D6\u00D8-\u00F6"
                        + "\u00F8-\u02FF\u0370-\u037D"
                        + "\u037F-\u1FFF\u200C-\u200D"
                        + "\u2070-\u218F\u2C00-\u2FEF"
                        + "\u3001-\uD7FF\uF900-\uFDCF"
                        + "\uFDF0-\uFFFD%s-%s]",
                        // note that unicode code points above ffff cannot be expressed as single character
                        // (i.e. u-notation) and thus have to expressed using Character.toChars(int codePoint)
                        new String(toChars(0x10000)), new String(toChars(0xEFFFF)));

        String PN_CHARS_U = union(PN_CHARS_BASE, "[_]");
        String PN_CHARS = union(PN_CHARS_U, "[\\-0-9\u00B7\u0300-\u036F\u203F-\u2040]");

        sparqlLocalNamePattern =
                Pattern.compile(format("%s(%s*%s)?", PN_CHARS_U, union(PN_CHARS, "[.]"), PN_CHARS));
        sparqlNamespacePrefixPattern = 
        		Pattern.compile(format("%s(%s*%s)?", PN_CHARS_U, union(PN_CHARS, "[.]"), PN_CHARS));
	}

	private String union(String... characterClasses) {
		return "[" + Joiner.on("").join(characterClasses) + "]";
	}


	Map<String, String> getPredefinedNamespaces()
    {
    	predefinedNamespaces = new HashMap<String, String>();

    	predefinedNamespaces.put("Help", "http://www.fluidops.com/help/");
    	predefinedNamespaces.put("Admin", "http://www.fluidops.com/admin/");
    	predefinedNamespaces.put("Settings", "http://www.fluidops.com/settings/");
    	predefinedNamespaces.put("System", "http://www.fluidops.com/");
    	predefinedNamespaces.put("rdf", RDF.NAMESPACE);
    	predefinedNamespaces.put("owl", OWL.NAMESPACE);
    	predefinedNamespaces.put("rdfs", RDFS.NAMESPACE);
    	predefinedNamespaces.put("foaf", Vocabulary.FOAF.NAMESPACE);
    	predefinedNamespaces.put("xsd", "http://www.w3.org/2001/XMLSchema#");

    	return predefinedNamespaces;
    }


	/**
     * sets the namespace if it is not set in namespaces.prop 
     * and throws an exception if it is set incorrectly
     * @param key
     * @param value
     */
    private void setNamespace(String key, String value)
    {
    	if ( ! store.containsKey( key ) )
    		store.put(key, value);
    	else validateNamespace(key, store.getProperty(key));

    }


	@Override
    public String defaultNamespace()
    {
        return store.getProperty("default");
    }
    
    @Override
    public String systemNamespace()
    {
        return store.getProperty("System");
    }
    
    @Override
    public String settingsNamespace()
    {
        return store.getProperty("Settings");
    }
    
    @Override
    public String templateNamespace()
    {
        return templateNS;
    }
    
    @Override
    public String fileNamespace()
    {
        return fileNS;
    }
    
    /**
     * Validates a local name according to the SPARQL specs,
     * http://www.w3.org/TR/rdf-sparql-query/#rPN_LOCAL
     * 
     * @param localName
     * @return
     */
    protected boolean validateLocalName(String localName)
    {
        Matcher sparqlLocalNameMatcher =
                sparqlLocalNamePattern.matcher(localName);

        return sparqlLocalNameMatcher.matches();
    }

    @Override
    public String getAbbreviatedURI(URI uri)
    {
        if (uri==null)
            return null;
        
        // Special handling for Template-pages
        if (uri.stringValue().startsWith(templateNS))
        {
            String abbrv = getAbbreviatedURI(ValueFactoryImpl.getInstance()
                    .createURI(
                            uri.stringValue().substring(templateNS.length())));
        	return abbrv != null ? templateNS + abbrv : null;
        }
        // Special handling for Files
        if (uri.stringValue().startsWith(fileNS))
        {
            return uri.stringValue();
        }
        
        for ( Entry<Object, Object> s : namespacesByDescSize )
        {
            String namespace = (String)s.getValue();
            if ( uri.stringValue().startsWith(namespace) )
            {
                String localname =
                        uri.stringValue().substring(namespace.length());

                // return null, if the local name does not conform to standard
                // specifications:
                // [XML] http://www.w3.org/TR/2006/REC-xml-names11-20060816/#NT-LocalPart
                // [SPARQL] http://www.w3.org/TR/rdf-sparql-query/#rPN_LOCAL
                // Note that technically both apply to us, but we follow SPARQL:
                if (! validateLocalName(localname))
                    return null; // no abbreviated version exists

                // for the default namespace, we omit the namespace prefix
                String prefix =
                        (((String) s.getValue()).equals(defaultNamespace())) ? ""
                                : (String) s.getKey() + ":";
                return prefix + localname;
            }
        }
        return null;
    }
    
    @Override
    public String getNamespace(URI uri)
    {
        if (uri==null)
            return null;
        
        for ( Entry<Object, Object> s : namespacesByDescSize )
        {
            String namespace = (String)s.getValue();
            if ( uri.stringValue().startsWith(namespace) )
                return namespace;
        }
        return null;
    }
    
    @Override
    // TODO: this is only a quick fix 
    public List<String> getAbbreviatedURIs(URI uri)
    {
        String abbreviatedURI = getAbbreviatedURI(uri);
        List<String> abbreviatedURIs = new ArrayList<String>();
        if (abbreviatedURI == null)
            return abbreviatedURIs;
        else
            abbreviatedURIs.add(abbreviatedURI);
        
        // get alternative solutions
        // rdf vocabulary
        if (abbreviatedURI.equals("rdf:type"))
            abbreviatedURIs.add("type");

        // rdfs vocabulary
        if (abbreviatedURI.equals("rdfs:label"))
            abbreviatedURIs.add("label");
        if (abbreviatedURI.equals("rdfs:subClassOf"))
            abbreviatedURIs.add("subClassOf");
        if (abbreviatedURI.equals("rdfs:subPropertyOf"))
            abbreviatedURIs.add("subPropertyOf");
        if (abbreviatedURI.equals("rdfs:comment"))
            abbreviatedURIs.add("comment");
        if (abbreviatedURI.equals("rdfs:domain"))
            abbreviatedURIs.add("domain");
        if (abbreviatedURI.equals("rdfs:range"))
            abbreviatedURIs.add("range");

        // xsd vocabulary
        if (abbreviatedURI.equals("xsd:int"))
            abbreviatedURIs.add("int");
        if (abbreviatedURI.equals("xsd:integer"))
            abbreviatedURIs.add("integer");
        if (abbreviatedURI.equals("xsd:short"))
            abbreviatedURIs.add("short");
        if (abbreviatedURI.equals("xsd:long"))
            abbreviatedURIs.add("long");
        if (abbreviatedURI.equals("xsd:string"))
            abbreviatedURIs.add("string");
        if (abbreviatedURI.equals("xsd:string"))
            abbreviatedURIs.add("string");

        // dbpedia
        if (abbreviatedURI.equals("dbpedia:thumbnail"))
            abbreviatedURIs.add("thumbnail");
        
        return abbreviatedURIs;
    }


    /*
     * The opposite of getPrefixURI(): returns the long version of a prefixed URI.
     * If no prefix is given or the prefix is undefined, the default prefix is assumed.
     */
    @Override
    public URI getFullURI(String shortUri)
    {
        int col = shortUri.indexOf(':');
        String prefix;
        String name;
        if (col == -1)
        {
            prefix = "default"; // use default if no NS is given
            name = shortUri;
        }
        else
        {
            prefix = shortUri.substring(0, col);
            name = shortUri.substring(col + 1);
        }

        // special handling for template prefix
        // Note that after the Template prefix we follow the same logic as for any URI
        if(templateNS.startsWith(prefix))
        {
	        	URI uri = guessURI(name);
	        	
	            return (uri==null ? null : valueFactory.createURI(templateNS+guessURI(name).stringValue()));
        }
        // special handling for file prefix
        if(fileNS.startsWith(prefix))
        {
        	return valueFactory.createURI(shortUri);
        }
        if (store.containsKey(prefix))
            return valueFactory.createURI((String)store.get(prefix),name);
        else
            return valueFactory.createURI((String)store.get("default"),shortUri);
    }


    @Override
    public URI guessURI(String name)
    {         
        if (name == null || name.isEmpty())
            return null;
        
        // cannot make a valid URI out of this
        if (StringUtil.containsNonIriRefCharacter(name,true))
            return null;
        
        // if the URI is a full URI, use standard parsing procedure
        name = name.trim();
        if (name.startsWith("<") && name.endsWith(">")) 
    		return parseFullURI(name);

        // if not, there must be no "<" and ">" signs within
        if (name.contains("<") || name.contains(">"))
            return null;
        
        // we treat the standard vocabulary in a special way:
        URI standardURI = matchStandardURI(name);
        if (standardURI!=null)
            return standardURI;

     // special handing for templates: form corresponding non-template URI
		// for now, add Template: prefix again in the end
		boolean isTemplate = false;
		if (name.startsWith(templateNS)) {
			name = name.substring(templateNS.length());
			isTemplate = true;
		}
		
        // if no standard vocabulary was matched, we try to split the NS
        int splitPosition = identifyNamespacePrefix(name);
        URI ret = null;

        // if no namespace is specified, use default namespace
        if (splitPosition == -1)
            ret = valueFactory.createURI((String)store.get("default"),name); // default NS
        
        // if the string contains ":", but the prefix is unresolvable ...
        // ... split position -2 indicates 
        else if (splitPosition == -2)
        {
            try
            {
                ret = valueFactory.createURI(name); // create as is
            }
            catch (Exception e)
            {
                // not a valid URI, so we return null
                return null;
            }
        }
        // URI that can be resolved
        else
        {
            String prefix = name.substring(0,splitPosition);
            if (prefix.isEmpty())
                prefix = "default";
            String namespace = (String)store.get(prefix);
            ret = valueFactory.createURI(namespace,name.substring(splitPosition + 1));
        }
        
        if (isTemplate)
			ret = valueFactory.createURI(templateNS, ret.stringValue());
        
        return ret;
    }
    
    @Override
    public URI guessURIOrCreateInDefaultNS(String name)
    {
    	URI u = guessURI(name);
    	return u!=null ? u : createURIInDefaultNS(name);
        
    }
    
    @Override
    public URI createURIInDefaultNS(String s)
    {
        if (s==null)
            throw new RuntimeException("Passed URI is null");
        
        String namespace = EndpointImpl.api().getNamespaceService().defaultNamespace();
        URI u = valueFactory.createURI(namespace,StringUtil.replaceNonIriRefCharacter(s,'_'));

        if (u==null)
            throw new RuntimeException("Creation of URI failed for string " + s);
        return u;
    }

    /** 
     * The current implementation works as follows: if we have an explicitly prefixed
     * URI and succeed in resolving the prefix, then a URI is created. Otherwise, if
     * there is no prefix or the prefix cannot be resolved, create a value (and not a
     * prefix in the default namespace). If one wants to create a URI in the default
     * namespace instead, one should use the createURI method instead of this one,
     * 
     * Side note:  This is probably the best we can do to give the user the choice
     * in input forms: if the user specified the prefix (using 'default' for the
     * default prefix) or some default vocabulary, s/he can assert that a URI is created,
     * otherwise a literal is created.  
     **/
    @Override
    public  Value guessValue(String name)
    {
        if (name == null || name.isEmpty())
            return null;
        
        // we treat the standard vocabulary in a special way:
        URI standardURI = matchStandardURI(name);
        if (standardURI!=null)
            return standardURI;

        // let's check if we have some prefixed URI
        int splitPosition = identifyNamespacePrefix(name);
        Value ret = null;
        if (splitPosition < 0)
            ret = valueFactory.createLiteral(name);
        else
        {
            // is splitPosition differs from -1, we can be sure that the prefix
            // is resolvable, hence we can perform a lookup
            String prefix = name.substring(0,splitPosition);
            if (prefix.isEmpty())
                prefix = "default";
            String namespace = (String)store.get(prefix);
            ret = valueFactory.createURI(namespace,name.substring(splitPosition + 1));
        }
        return ret;
    }
    
    @Override
    public URI matchStandardURI(String string) {

        if (string.equals("label"))
            return RDFS.LABEL;
        if (string.equals("subClassOf"))
            return RDFS.SUBCLASSOF;
        if (string.equals("subPropertyOf"))
            return RDFS.SUBPROPERTYOF;
        if (string.equals("comment"))
            return RDFS.COMMENT;
        if (string.equals("type"))
            return RDF.TYPE;
        if (string.equals("domain"))
            return RDFS.DOMAIN;
        if (string.equals("range"))
            return RDFS.RANGE;
        if (string.equals("int"))
            return XMLSchema.INT;
        if (string.equals("integer"))
            return XMLSchema.INTEGER;
        if (string.equals("short"))
            return XMLSchema.SHORT;
        if (string.equals("long"))
            return XMLSchema.LONG;
        if (string.equals("string"))
            return XMLSchema.STRING;
        if (string.equals("thumbnail"))
            return Vocabulary.DBPEDIA_ONT.THUMBNAIL;
        // if you extend this list, also extend
        // method getAbbreviatedURIs accordingly
        
        return null; // no match
    }
    
    @Override
    public Value parseValue(String s)
    {
        s = s.trim(); // ignore leading and trailing whitespaces
       
        // full URI parsing
        if (s.startsWith("<"))
        {
            return parseFullURI(s);
        }
        // string literal parsing
        else if (s.startsWith("\"") || s.startsWith("'"))
        {
            return parseStringLiteral(s);
        }
        // numeric literal parsing
            
        else if (s.matches("-?[0-9]*(\\.[0-9]*)?"))
        {
            
            // here we simplify a bit w.r.t. the standard:
            // the Double.valueOf method is not exactly identical to
            // the standard, but it should do its job...
            if (s.contains("."))
            {
                try
                {
                    return valueFactory.createLiteral(Double.valueOf(s));
                }
                catch (Exception e)
                {
                    // invalid format
                    return null;
                }
            }
            else
            {
                try
                {
                    return valueFactory.createLiteral(Integer.valueOf(s));
                }
                catch (Exception e)
                {
                    // invalid format
                    return null;
                }
            }
        }
        // boolean true literal
        else if (s.equals("true"))
        {
            return valueFactory.createLiteral(true);
        }
        // boolean false literal
        else if (s.equals("false"))
        {
            return valueFactory.createLiteral(false);
        }
        // prefix notation URI parsing (only alternative left)
        else
        {
            return parsePrefixedURI(s);

        }
    }

    @Override
    public URI parseURI(String s)
    {
        s = s.trim();
        return s.startsWith("<") ? parseFullURI(s) : parsePrefixedURI(s);
    }

    @Override
    public URI parseFullURI(String s)
    {
        if (s.startsWith("<") && s.endsWith(">"))
        {
            s = s.substring(1, s.length() - 1);
            try
            {
                return valueFactory.createURI(s);
            }
            catch (Exception e)
            {
                // string contains invalid parameters
            }
        }
        return null; // parsing failed
    }

    @Override
    public URI parsePrefixedURI(String s)
    {
        try
        {
            int delim = s.indexOf(":");
            if (delim == -1) // no colon found
            {
                // create URI in the default namespace
                return valueFactory.createURI(defaultNamespace(), s);
            }
            else if (delim == 0) // colon in first place: :name
            {
                // create URI in the default namespace
                return valueFactory.createURI(defaultNamespace(), s.substring(1));
            }
            else
            {
                String prefix = (String) store.get(s.subSequence(0, delim));
                if (prefix != null)
                    return valueFactory.createURI(prefix, s
                            .substring(delim + 1));
                else
                    logger.error("Prefix '" + s.subSequence(0, delim)
                            + "' in prefixed URI cannot be resolved.");
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }

        return null; // parsing failed
    }

    @Override
    public Literal parseStringLiteral(String s)
    {
        // we forbid backtick escaping for now (not 100% standard conformant)
        int firstTicks = s.startsWith("\"") ? s.indexOf("\"") : s.indexOf("'");
        int sndTicks = s.startsWith("\"") ? s.lastIndexOf("\"") : s.lastIndexOf("'");

        String literalValue = s.substring(firstTicks+1, sndTicks);
        
        String language = null;
        URI datatype = null;

        String tail = s.substring(sndTicks + 1);

        // create language tag, if present
        if (tail.startsWith("@"))
        {
            language = tail.substring(1);
            // ... and let Sesame try to parse this
        }

        // create datatype information, if present
        else if (tail.startsWith("^^"))
        {
            // recursively parse trailing URI
            datatype = parseURI(tail.substring(2));
        }

        // otherwise there must be no tail
        else if (!tail.isEmpty())
        {
            return null;
        }

        // try to create the literal
        try
        {
            if (language == null && datatype == null)
                return valueFactory.createLiteral(literalValue);
            else if (language != null)
                return valueFactory.createLiteral(literalValue, language);
            else if (datatype != null)
                return valueFactory.createLiteral(literalValue, datatype);
        }
        catch (Exception e)
        {
            // something's wrong here
        }

        return null; // parsing failed
    }
    
    /*
     * Checks if the string starts with an namespace prefix that is
     * available in the namespace cache, thereby taking available
     * namespaces into account. Returns -1 if no splitting is possible.
     * Here are some results, assuming rdfs, rdf, and myNS are the
     * known namespaces:
     * 
     * rdfs:label -> 4
     * rdf:type -> 3
     * myNS:test -> 4
     * myNS2:xyz -> -1
     * default:test -> 7 ('default' is predefined and stands for the default NS)
     * 
     * @returns A positive integer denotes the split position, if resolvable;
     *          -1 means the String contains no ":" at all;
     *          -2 means the String contains a ":", but the prefix is not defined; 
     */
    private int identifyNamespacePrefix(String uri)
    {
        int index = uri.indexOf(":");
        if (index==-1)
            return -1;
        
        // return the index if and only if the prefix is known
        String prefix = uri.substring(0,index);
        if (prefix.equals("default") || prefix.equals("") || 
                store.containsKey(uri.substring(0,index)))
            return index;
        else
            return -2; // there's ":", but no matching prefix
    }


    /**
     * This is of course not the most elegant way and error prone (we need to 
     * take care the sorted store is synchronized at all times): we keep
     * a set of namespace key-value mappings sorted by length of the value.
     * The problem is that SortedMap allows sorting only by keys, not by
     * values. And sorted set with an inner pair is not what we want. Using
     * a bidirectional map is also invalid, because we may have duplicates
     * (different namespace prefixes mapping to the same namespace).
     */
    private void synchronizeSortedStore()
    {
        
        namespacesByDescSize = new TreeSet<Entry<Object,Object>>(
                new Comparator<Entry<Object,Object>>()
                {
                    @Override
                    public int compare(Entry<Object,Object> o1, Entry<Object,Object> o2)
                    {
                        Object o1val = o1.getValue();
                        Object o2val = o2.getValue();
                        
                        if (o1val instanceof String && o2val instanceof String)
                        {
                            int o1l = ((String)o1val).length();
                            int o2l = ((String)o2val).length();

                            if (o1l!=o2l)
                                return o2l-o1l;
                        }
                        
                        // fallback or equal-size
                        return o1.toString().compareTo(o2.toString());
                    }
                });
        namespacesByDescSize.addAll(store.entrySet());
        
    }

}
