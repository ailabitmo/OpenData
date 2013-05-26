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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.StringUtil;

/**
 * Class representing a context in the data repository.
 * Except for the METACONTEXT (cf. type METACONTEXT),
 * contexts are represented by URIs of the form
 * 
 * -> URI_OF_CONTEXT_SRC/CONTEXT_CREATION_TIMESTAMP/CONTEXT_TYPE
 * 
 * Each context is uniquely identified by its URI.
 * 
 * @author msc
 */
public class Context implements Comparable<Object>
{     
    private static final Logger logger = Logger.getLogger(Context.class.getName());

    /**
     * Context types:
     * 
     * USER -> data written by user (e.g. wiki, edit form)
     * CLI -> data loaded via CLI
     * SYSTEM -> data created automatically by the system
     * PROVIDER -> data provider
     * COMMUNICATIONSERVICE -> communication service
     * METACONTEXT -> the one and only meta context (fixed URI)
     */
    static public enum ContextType
    {    
        USER("User"), 
        PROVIDER("Provider"), 
        CLI("CLI"),
        SYSTEM("System"), 
        METACONTEXT("MetaContext"),
        LOOKUPPROVIDER("LookupProvider"),
        COMMUNICATIONSERVICE("CommunicationService"),
        EMPTY("Empty"),
        UNKNOWN("Unknown"),
        VOID("VoID"); // Vocabulary of interlinked data sets
        
        private String name;
        
        private ContextType(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
    } 
    
    /**
     * The context label describes through which component the context
     * was created (i.e., the prior "where" field). We use the context
     * label in some cases to identify triples with a certain property,
     * e.g. triples created in the Wiki when updating semantic links.
     * Therefore, do NOT change existing labels. Extension of this data
     * structure should be unproblematic, though.
     *
     */
    static public enum ContextLabel
    {
    	ANNOTATION_FRAMEWORK_INSTANCE_DATA_IMPORT("Annotation framework instance data import"),
    	ANNOTATION_FRAMEWORK_ONTOLOGY_IMPORT("Annotation framework ontology import"), 
    	ATMOS_IMPORT("Atmos Import"),
    	AUTOMATIC_SYNCHRONISATION("Automatic Synchronization"),
    	CHANGELOG("changelog"),
    	DATA_INPUT_FORM("Data input form"),
    	FILE_UPLOAD("File upload"),
    	LUXID_IMPORT("Luxid Import"),
    	NEW_INSTANCE_WIZARD("New instance wizard"),
    	ONTOLOGY_IMPORT("Ontology import"),
    	RDF_IMPORT("RDF Import"),
    	REGULAR_PROVIDER_RUN("Provider run"),
    	SPARQL_UPDATE("SPARQL update command"),
    	WORKFLOW("Workflow"),
    	WIKI("Wiki"),
    	SOLUTION("Solution"),
        UNKNOWN("Unknown");
    	
        private String name;
        
        private ContextLabel(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
    }
    
    /**
     * Context state, used for editorial workflows
     * 
     */
    static public enum ContextState
    {    
        DRAFT("Draft"), 
        REJECTED("Rejected"), 
        APPROVED("Approved"),
        PUBLISHED("Published");

        private String name;
        
        private ContextState(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }      
    }
    
    /**
     * The timestamp that was used for the last Context. We compare to this
     * value to have unique/synchronized values across different contexts
     */
    private static AtomicLong lastTimestampUsed = new AtomicLong(0L);
    
    // the context URI, *unique* identifier of the context
    private URI contextURI;
    
    // context type
    private Context.ContextType type;
    
    // context state
    private Context.ContextState state;
    
    // context time
    private Long timestamp;
    
    // context source
    private URI source;
    
    // context group (can be used to group contexts together)
    private URI group;
    
    // Providers can be called for a particular value (typically the value of the page context)
    // That parameter is stored here
    private URI inputParameter;
    
    // the label
    private Context.ContextLabel label;

    // is the context editable or not
    private Boolean isEditable;
	
    /**
	 * Loads an existing context by URI using the specified data manager. The
	 * returned context will have meta information available (whenever attached).
	 * The method is null-safe: in case the contextURI is null, a dummy context
	 * with URI SYSTEM_NAMESPACE:EmptyContext is returned. 
	 */
	public static Context loadContextByURI(URI contextURI, ReadDataManager dm)
	{
    	if (contextURI == null)
    		return new Context(ContextType.EMPTY, null, null, null, null, null, null, null, null);    	
        if (contextURI.equals(Vocabulary.SYSTEM_CONTEXT.METACONTEXT))
        	return new Context(ContextType.METACONTEXT, null, null, null, null, null, null, null, null);
        else if (contextURI.equals(Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT))
        	return new Context(ContextType.VOID, null, null, null, null, null, null, null, null);
        else
        {
            // remaining information is encoded in the DB, load it
            try
            {                 
                RepositoryResult<Statement> stmts = 
                		dm.getStatements(contextURI, null, null, false, Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
                
            	ContextLabel label = null;
            	ContextState state = null;
            	ContextType type = null;
            	URI group = null;
            	URI source = null;
            	Long timestamp = null;
            	Boolean isEditable = null;
            	URI inputParameter = null;
            	
            	// collect values: best-effort, load whatever is available in the DB
                for(Statement s : stmts.asList())
                {
                	
                    if (s.getPredicate().equals(RDFS.LABEL))
                    	label = stringToContextLabel(s.getObject().stringValue());
                    else if (s.getPredicate().equals(Vocabulary.SYSTEM_CONTEXT.CONTEXTSTATE))                        
                    	state = stringToContextState(s.getObject().stringValue());
                    else if (s.getPredicate().equals(Vocabulary.SYSTEM_CONTEXT.CONTEXTTYPE))                        
                    	type = stringToContextType(s.getObject().stringValue());
                    else if (s.getPredicate().equals(Vocabulary.SYSTEM_CONTEXT.CONTEXTGROUP))
                    {
                        Value groupVal = s.getObject();
                        if (groupVal instanceof URI)
                            group = (URI)groupVal;
                    }
                    else if (s.getPredicate().equals(Vocabulary.SYSTEM_CONTEXT.CONTEXTSRC))
                    {
                        Value sourceVal = s.getObject();
                        if (sourceVal instanceof URI)
                        	source = (URI)sourceVal;
                    }
                    else if (s.getPredicate().equals(Vocabulary.DC.DATE))                    
                    {
                        String dateString = s.getObject().stringValue();
                        Date date = ReadWriteDataManagerImpl.ISOliteralToDate(dateString);
                        
                        // for backward compatibility: We used to record the timestamp in different (inconsistent formats)
                        timestamp = date!=null ? date.getTime() : 0L;
                    }
                    else if (s.getPredicate().equals(Vocabulary.SYSTEM_CONTEXT.ISEDITABLE) )
                    	isEditable = s.getObject().equals(Vocabulary.TRUE);                        
                    else if (s.getPredicate().equals(Vocabulary.SYSTEM_CONTEXT.INPUTPARAMETER))
                        inputParameter = ValueFactoryImpl.getInstance().createURI(s.getObject().stringValue());                        
                }
                stmts.close();

                return new Context(type, contextURI, state, source, group, inputParameter, isEditable, timestamp, label);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }            
        }
	}

	/**
	 * Returns a fresh context (with generated URI) of the specified type in
	 * PUBLISHED state (which is the desired default whenever dealing with non-user
	 * data). The method allows to specify the context source and the context label. 
	 * Whether or not the context is editable at runtime depends on the system-wide setting,
	 * as defined by Config.getContextsEditableDefault(). The context group and
	 * its inputParam are undefined. The context's timestamp will be generated
	 * internally, using the current time as a basis.  Note that, when creating a
	 * context of type {@link ContextType#MetaContext}, {@link ContextType#VOID},
	 * or {@link ContextType#EMPTY}, all parameters except type are ignored and
	 * the one and only meta/void/empty context is returned.
	 * 
	 * If your use case requires different settings, consider using the more
	 * flexible method {@link #getFreshPublishedContext(ContextType, URI, URI, URI, URI, Boolean, ContextLabel)}.
	 * 
	 * @param type the type of the context
	 * @param source the context source
	 * @param contextLabel the label of the context, providing information about its creator and origin
	 * @return the new context
	 */
	public static Context getFreshPublishedContext(ContextType type, URI source, ContextLabel contextLabel)
	{
		return getFreshPublishedContext(type, null, source, null, null, null, contextLabel);
	}

	/**
	 * Returns a fresh context of the specified type in PUBLISHED state (which is the
	 * desired default whenever dealing with non-user data). The method allows to specify 
	 * a contextURI (if set to null, a URI will be generated), a context source,
	 * a context group (which can be used to group together multiple contexts), as
	 * well as the context label. Further, the parameter {@link #isEditable()} can
	 * be used to specify whether the context is editable or not; when set to null,
	 * Config.getContextsEditableDefault() will determine whether the context is
	 * editable or not (at runtime). The context's timestamp will be generated internally, 
	 * using the current time as a basis. Note that, when creating a context 
	 * of type {@link ContextType#MetaContext}, {@link ContextType#VOID},
	 * or {@link ContextType#EMPTY}, all parameters except type are ignored and
	 * the one and only meta/void/empty context is returned.
	 * 
     * @param type the type of the context
     * @param contextURI an optional context URI; if no context URI is specified, 
     *         the URI is computed from the {@link #source}, {@link #timestamp}, and {@link #type}
     * @param source the context source
     * @param group the context group
     * @param inputParam additional parameter (complementing the source) used to generate the context
     * @param isEditable use null to leave default (runtime decision), otherwise initialize to override an existing default
     * @param contextLabel the label of the context, providing information about its creator and origin

	 */
	public static Context getFreshPublishedContext(ContextType type, URI contextURI, URI source,
	        URI group, URI inputParam, Boolean isEditable, ContextLabel contextLabel)
	{
		long timestamp = getContextTimestampSafe();
		
		if (contextURI==null)
			contextURI = getGeneratedContextURI(source, timestamp, type);
		
		return new Context(type, contextURI, getDefaultContextState(type), source, group, 
				inputParam, isEditable, 
				timestamp, contextLabel);
	}

	
	/**
	 * Returns a fresh context of the specified type in a given state (if the 
	 * passed state is null, the state is initialized with PUBLISHED by default 
	 * and with DRAFT in editorial workflow mode. The method allows to specify 
	 * a contextURI (if set to null, a URI will be generated), a context source,
	 * a context group (which can be used to group together multiple contexts), as
	 * well as the context label. Further, the parameter {@link #isEditable()} can
	 * be used to specify whether the context is editable or not; when set to null,
	 * Config.getContextsEditableDefault() will determine whether the context is
	 * editable or not at runtime. The context's timestamp will be generated internally, 
	 * using the current time as a basis. Note that, when creating a
	 * context of type {@link ContextType#MetaContext}, {@link ContextType#VOID},
	 * or {@link ContextType#EMPTY}, all parameters except type are ignored and
	 * the one and only meta/void/empty context is returned.
	 * 
     * @param type the type of the context
     * @param contextURI an optional context URI; if no context URI is specified, the URI is computed from the {@link #source}, {@link #timestamp}, and {@link #type}
     * @param contextState the state of the context to be created
     * @param source the context source
     * @param group the context group
     * @param inputParam additional parameter (complementing the source) used to generate the context
     * @param isEditable use null to set default, otherwise initialize to override an existing default
     * @param contextLabel the label of the context, providing information about its creator and origin

	 */
	public static Context getFreshContext(ContextType type, ContextState contextState, URI contextURI, 
			URI source, URI group, URI inputParam, Boolean isEditable, ContextLabel contextLabel)
	{
		long timestamp = getContextTimestampSafe();
		
		if (contextURI==null)
			contextURI = getGeneratedContextURI(source, timestamp, type);
		
		if (contextState==null)
			contextState = getDefaultContextState(type);
		
		return new Context(type, contextURI, contextState, source, group, 
				inputParam, isEditable,
				timestamp, contextLabel);
	}
	
	/**
	 * Generates a fresh context with the given label for the current user and time.
	 */
	public static Context getFreshUserContext( ContextLabel contextLabel )
	{
		return getFreshUserContextWithURI(null, contextLabel);
	}

	/**
	 * Get a new context for contextUri in the user context space. The state is initialized
	 * with PUBLISHED by default (and with DRAFT in editorial workflow mode). If the contextUri
	 * is null, this call is identical to method getFreshUserContext(ContextLabel),
	 * i.e. a context URI will be generated.
	 * 
	 * @param contextUri the designated context URI
	 * @param contextLabel the context label
	 * @return
	 */
	public static Context getFreshUserContextWithURI(URI contextUri,  ContextLabel contextLabel ) 
	{			
		long timestamp = getContextTimestampSafe();
		URI source = EndpointImpl.api().getUserURI();
			
		if (contextUri==null)
			contextUri = getGeneratedContextURI(source, timestamp, ContextType.USER);
			
		return new Context(ContextType.USER, contextUri, 
				getDefaultContextState(ContextType.USER), 
				source, null, null, true, timestamp, contextLabel );
	}

	/**
	 * Generates a context for the communication service. If the passed timestamp is
	 * null, a new context will be created; if the timestamp is passed and a communication
	 * service context with the specified parameters (i.e., source and group) already
	 * exists, the existing context is retrieved (yet the associated metadata is
	 * not loaded).
	 */
	public static Context getCommunicationServiceContext(URI source, URI group, Long _timestamp)
	{
	    long timestamp = _timestamp==null ? getContextTimestampSafe() : _timestamp;
	    try
	    {
	        return new Context(ContextType.COMMUNICATIONSERVICE,
	        		getGeneratedContextURI(source, timestamp, ContextType.COMMUNICATIONSERVICE), 
	        		getDefaultContextState(ContextType.COMMUNICATIONSERVICE), source, group, null, false,
	        		timestamp, ContextLabel.AUTOMATIC_SYNCHRONISATION);
	    }
	    catch ( Exception e )
	    {
	        throw new RuntimeException( "CommunicationService context could not retrieved", e );
	    }
	}

    /**
	 * @return the URI representing the context
	 */
	public URI getURI()
	{
	    return contextURI;
	}

	/**
	 * @return the type of the context
	 */
	public ContextType getType()
	{
	    return type;
	}

	/**
	 * @return current context state
	 */
	public Context.ContextState getState()
	{
	    return state;
	}

	/**
	 * Sets the current context state
	 * 
	 * @param state the new state
	 */
	public void setState(Context.ContextState state)
	{
	    this.state = state;
	}

	/**
	 * @return the context's timestamp (may be null)
	 */
	public Long getTimestamp()
	{
	    return timestamp;
	}

	/**
	 * @return the context's source
	 */
	public URI getSource()
	{
	    return source;
	}

	/**
	 * @return the group the context is associated to (may be null)
	 */
	public URI getGroup()
	{
	    return group;
	}

	/**
	 * @return inputParameter field of the context
	 */
	public URI getInputParameter()
	{
		return inputParameter;
	}

	/**
	 * @return the context's label
	 */
	public ContextLabel getLabel()
	{
	    return label;
	}

	/**
	 * @return whether the context is editable or not. if the editable property 
	 *  		is undefined, {@link Config#getContextsEditableDefault()} is used
	 */
	public boolean isEditable()
	{
	    return isEditable==null?Config.getConfig().getContextsEditableDefault():isEditable;
	}

	/**
	 * Retrieve the actual editable property of this instance. Method is package private
	 * and can be used for instance in RDM
	 * @return the actual editable property (i.e. true|false|null(=undefined)). 
	 */
	Boolean getIsEditable() {
		return isEditable;
	}
	
	/**
	 * Set context editable. The method does not persist this information
	 * in the DB.
	 * 
	 * @param editable to set editable or not
	 */
	public void setEditable(boolean editable)
	{
	    isEditable = editable;
	}

	/**
	 * Converts the context meta information into a human-readable tooltip
	 * (e.g. for display in a table or list).
	 * 
	 * @return
	 */
	public String tooltip()
	{
	    if (contextURI==null)
	        return "(no context specified)";
	    
	    if (contextURI.equals(Vocabulary.SYSTEM_CONTEXT.METACONTEXT))
	        return "MetaContext (stores data about contexts)";
	    
	    if(contextURI.equals(Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT))
	    	return "VoID Context (stores statistics about contexts)";
	    
	    if (type!=null && timestamp!=null)
	    {
	        GregorianCalendar c = new GregorianCalendar();
	        c.setTimeInMillis(timestamp);
	        
	        String date = ReadWriteDataManagerImpl.dateToISOliteral(c.getTime());           
	        
	        if (!StringUtil.isNullOrEmpty(date))
	                date = ValueResolver.resolveSysDate(date);
	        
	        String ret = "Created by " + type;
	        if (type.equals(ContextType.USER))
	            ret += " '"
	                    + source.getLocalName()
	                    + "'";
	        else
	        {
	            String displaySource = null;
	            if(source!=null)
	            {
	                displaySource = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(source);
	                if (StringUtil.isNullOrEmpty(displaySource))
	                    displaySource = source.stringValue(); // fallback solution: full URI
	            }
	            String displayGroup = null;
	            if (group!=null)
	            {
	                displayGroup = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(group);
	                if (StringUtil.isNullOrEmpty(displayGroup))
	                    displayGroup = group.stringValue(); // fallback solution: full URI
	            }
	            
	            ret += " (";
	            ret += source==null ? "" : "source='" + displaySource + "'";
	            ret += source!=null && group!=null ? "/" : "";
	            ret += group==null  ? "" : "group='" +  displayGroup + "'";
	            ret += ")";
	        }
	        ret += " on " + date;
	        if (label!=null)
	            ret += " (" + label.toString() + ")";
	        if (state!=null && !state.equals(ContextState.PUBLISHED))
	            ret += " (" + state + ")"; 
	        return ret;
	    }
	    else
	       return contextURI.stringValue();
	}



	/**
	 * 
	 * @return true if the context is associated to a user
	 */
	public boolean isUserContext()
	{
	    return type==Context.ContextType.USER;
	}

	// compares according to timestamp, required for ordering of changes
	@Override
	public int compareTo(Object o)
	{
	    if(o instanceof Context)
	    {
	    	if (timestamp==null || ((Context) o).timestamp==null)
	    		return 0;
	        return timestamp.compareTo(((Context) o).timestamp);
	    }
	    return 0;
	}

	// Context URIs are unique identifiers
	@Override
	public boolean equals(Object o)
	{
	    if(o instanceof Context)
	    {
	        return contextURI.equals(((Context) o).contextURI);
	    }
	    return false;
	}

	@Override 
	public int hashCode()
	{
	    return contextURI.hashCode();
	}
    
    
    /**
	 * Persists the context's meta information. For normal operations (such as adding/removing
	 * triples, this is internally called by the data manager. However, if you manipulate the
	 * context's state directly, you need to call this function in order to assert that
	 * the context meta information is persisted in the database.
	 */
	public void persistContextMetaInformation(ReadWriteDataManager dm)
	{
		// for transaction reasons, this is implemented inside the data manager directly:
		dm.persistContextMetaInformation(this);
	}

	/** 
     * Constructs a fresh context of the specified type with the given parameters.
     * The constructor allows to specify a contextURI, a context source, a context group
     * (which can be used to group together multiple contexts), the inputParam (allowing
     * to distinguish between several contexts of the same source), a timestamp, as well
     * as the context label. Further, the parameter {@link #isEditable()} can be used to
     * specify whether the context is editable or not.  Note that, when creating a
	 * context of type {@link ContextType#MetaContext}, {@link ContextType#VOID},
	 * or {@link ContextType#EMPTY}, all parameters except type are ignored and
	 * the one and only meta/void/empty context is returned.
     * 
     * The constructor is private, use methods
     * {@link #getFreshPublishedContext(ContextType, URI, ContextLabel)},
     * {@link #getFreshPublishedContext(ContextType, URI, URI, URI, URI, Boolean, ContextLabel)},
     * {@link #getFreshUserContext(ContextLabel)}, and 
     * {@link #getFreshUserContextWithURI(URI, ContextLabel)} if you want to
     * instantiate a new context.
     * 
     * @param type the type of the context
     * @param contextURI an optional context URI; if no context URI is specified, the URI is computed from the {@link #source}, {@link #timestamp}, and {@link #type}
     * @param state the current state of the context, if the state is null, it is automatically set to {@link ContextState#PUBLISHED}
     * @param source the context source
     * @param group the context group
     * @param inputParam additional parameter (complementing the source) used to generate the context
     * @param isEditable whether or not the context is editable
     * @param timestamp the timestamp when the context was created
     * @param contextLabel the label of the context, providing information about its creator and origin
     */
    private Context(ContextType type, URI contextURI, ContextState state, URI source,
            URI group, URI inputParam, Boolean isEditable, Long timestamp, ContextLabel contextLabel)
    {
    	if (type==ContextType.EMPTY)
    		initializeSystemContext(type, Vocabulary.SYSTEM_CONTEXT.EMPTYCONTEXT);
    	else if (type==ContextType.METACONTEXT)
            initializeSystemContext(type, Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
        else if (type==ContextType.VOID)
        	initializeSystemContext(type, Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT);
        else
        {
            // init members
            this.type = type;
            this.source = source;
            this.group = group;
            this.timestamp = timestamp;
            this.inputParameter=inputParam;
            this.label = contextLabel;
            this.state = state==null ? ContextState.PUBLISHED : state;
            this.contextURI = contextURI;
            this.isEditable = isEditable;
        }
    }

    
    private static URI getGeneratedContextURI(URI source, Long timestamp, ContextType type)
    {
        ValueFactory vf = ValueFactoryImpl.getInstance();
        
        String sourceStr = source==null?"http://www.fluidops.com/":source.stringValue();
        String typeStr = type==null?"null":type.toString();
        return vf.createURI(sourceStr + "/" + timestamp + "/" + typeStr);   
    }
    
    
    /**
     * Initializes a (non-editable) system context.
     * 
     * @param ctxType
     * @param contextUri
     */
    private void initializeSystemContext(ContextType ctxType, URI contextUri) 
    {
    	this.type=ctxType;
        
        // not in use for system contexts
        this.source=null;
        this.group=null;
        this.timestamp=null;
        this.inputParameter=null;
        this.label=null;
        this.isEditable=false;
        
        this.contextURI = contextUri;
    }
    
    /**
     * Converts a context source string back to the context src enum element.
     * 
     * @param s the string
     * @return the ContextType enum element (Context.Type.UNKNOWN if no match)
     */
    private static ContextType stringToContextType(String s) 
    {
        for (ContextType enumVal : ContextType.values())
        {
            if (enumVal.toString().equals(s))
                return enumVal;
        }
        
        // in case of failure:
        return ContextType.UNKNOWN;
    }    
    
    /**
     * Converts a context state string back to the context state enum element.
     * 
     * @param s the string
     * @return the ContextState enum element (ContextState.PUBLISHED as default)
     */
    private static ContextState stringToContextState(String s) 
    {
        for (ContextState enumVal : ContextState.values())
        {
            if (enumVal.toString().equals(s))
                return enumVal;
        }
        
        // in case of failure:
        return ContextState.PUBLISHED;
    }    

    /**
     * Converts a string to a context label.
     * @param s
     * @return
     */
    private static ContextLabel stringToContextLabel(String s)
    {
    	for (ContextLabel enumVal : ContextLabel.values())
    	{
    		if (enumVal.toString().equals(s))
    			return enumVal;
    	}
    	
    	// in case of failure
    	return ContextLabel.UNKNOWN;
    }
    
    /**
	 * Returns a unique timestamp considering the last stored timestamp. If the current
	 * timestamp is equal to the last one, one millisecond is added. The last step is
	 * repeated until a distinct timestamp is found.
	 *  
	 * @return a unique timestamp
	 */
	protected static long getContextTimestampSafe() 
	{    	
		long timestamp = System.currentTimeMillis();
		
		boolean repeat = true;
		while (repeat)
		{
	    	long lastUsed = lastTimestampUsed.get();
	    	if (timestamp>lastUsed) {
	    		repeat = !lastTimestampUsed.compareAndSet(lastUsed, timestamp);
	    	} else {
	    		timestamp = lastUsed+1;
	    	}
		}
		
		return timestamp;
	}
	
	protected static ContextState getDefaultContextState(ContextType contextType)
	{
		if (contextType!=ContextType.USER)
			return ContextState.PUBLISHED;
		else
			return Config.getConfig().getEditorialWorkflow()?
					Context.ContextState.DRAFT : ContextState.PUBLISHED;
	}
}
