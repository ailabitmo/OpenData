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

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.components.FForm.Validation;
import com.fluidops.iwb.model.Capability;
import com.fluidops.iwb.model.PojoConfigurable;
import com.fluidops.iwb.model.PojoConfigurableHelper;
import com.fluidops.iwb.ui.ProviderDomain;
import com.fluidops.iwb.util.Configurable;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * base class for POJO-configurable providers
 * 
 * @author aeb, pha
 *
 * @param <T>   config pojo to be used
 */
public abstract class AbstractFlexProvider<T extends Serializable> implements PojoConfigurable<T>, Configurable, Serializable
{
    private static final Logger logger = Logger.getLogger(AbstractFlexProvider.class.getName());

	private static final long serialVersionUID = -1293426854287867763L;

	// the provider configuration
    public T config = null;

    /**
     * RDF resource identifying the provider as context
     */
    public URI getProviderID()
    {
        return providerID;
    }
    
    /**
     * determines how long a context is kept in the store. Default is 0
     * which means the data is queried fully from the provider
     * and the previous data is replaced completely
     * 
     * With data sources that are looked up dynamically, we can set this value in the provider
     * in oder to determine how long data is cached
     * 
     * TODO: In principle, this is exactly the same as the poll interval - 
     * if the interval has not yet passed, we do not need to rerun
     * Consider getting rid of this variable
     * (or the other way around - we could check the context expiration time instead of the poll interval for the regular providers)
     */
    public long getContextExpirationTimeMS()
    {
    	return 0;
    }
    
    /**
     * fields that are null in the special class are carried over from the base class
     * done recursively for all objects in the subtree
     */
    public static <T> void merge( T special, T base )
    {
        if ( base == null || special == null )
            return;
        
        for ( Field f : base.getClass().getFields() )
        {
            try
            {
                if ( f.getType().isPrimitive() || 
                        f.getType().equals( Calendar.class ) || 
                        f.getType().equals( Date.class ) || 
                        f.getType().equals( URL.class ) || 
                        f.getType().equals( java.net.URI.class ) || 
                        f.getType().getName().startsWith( "java.lang" )
                   )
                {
                    if ( f.get( special ) != null )
                        // primitive value AND special != null, nothing to do
                        continue;
                    
                    f.set( special, f.get( base ) );
                }
                else if ( List.class.isAssignableFrom( f.getType() ) )
                {
                    List lst = (List)f.get( special );
                    if ( lst != null && !lst.isEmpty() )
                        // primitive value AND special != null, nothing to do
                        continue;
                    
                    f.set( special, f.get( base ) );
                }
                else
                {
                    Object newS = f.get( special );
                    Object newB = f.get( base );
                    
                    if ( newS == null && newB != null )
                        // copy over entire subtree
                        f.set( special, newB );
                    else
                        // examine both kids
                        merge( newS, newB );
                }
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    /**
     * get all provider configs, query those and return union
     */
    public List<Statement> list()
    {
    	List<Statement> res = new LinkedList();
 
        boolean success = false;
        try
        {
                gather( res );
                success = true;
        }
        catch ( Exception e )
        {
           logger.error(e.getMessage(), e);
        }
        
        if ( !success  )
            throw new RuntimeException( "Provider instance failed" );
        return res;
    }

    /**
     * get the config class type
     */
    public abstract Class<? extends T> getConfigClass();
    
    
    /**
     * classify providers into domains like music, datacentern, etc.
     */
    public ProviderDomain getProviderDomain()
    {
        return ProviderDomain.General;
    }
    
    /**
     * given a config pojo, add new statements to res - exceptions are handled in list
     */
    public abstract void gather( List<Statement> res ) throws Exception;
    
    /**
     * given a config pojo, add new statements to res - exceptions are handled in list
     */
    public void gatherOntology( List<Statement> res ) throws Exception
    {
    	// empty by default, may be re-implemented by provider
    }
    
    public List<Capability> getCapabilities() {
    	return PojoConfigurableHelper.getCapabilities( this );
    }
    
    public List<String> getRequiredProps()
    {
        return getProps();
    }
    
    public List<String> getProps()
    {
        return PojoConfigurableHelper.getProps( this );
    }
    
    public boolean check( T config )
    {
        return PojoConfigurableHelper.check( this, config );
    }
    
    public String getPropDoc( String prop )
    {
        return PojoConfigurableHelper.getPropDoc( this, prop );
    }
    
    /**
     * called from the provider edit UI. must translate the generic location info into the propper config field(s)
     * @param location
     */
    @SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Exception caught for robustness")
    public void setLocation( String location )
    {
		try
		{
			config.getClass().getField("__resource").set(config, new ValueFactoryImpl().createURI( location ));
		}
		catch ( Exception e )
		{
			try
			{
				String namespace = com.fluidops.iwb.api.EndpointImpl.api().getNamespaceService().defaultNamespace();
				config.getClass().getField("__resource").set(config, new ValueFactoryImpl().createURI( namespace+location ));
			} 
			catch ( Exception ex )
			{
				// ignore
				logger.debug("Exception while initializing provider configuration: " + e.getMessage(), e);
			}
		}
    }

    @SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Exception caught for robustness")
	public String getLocation()
	{
		try
		{
			return (String)config.getClass().getField("file").get(config);
		}
		catch ( Exception e )
		{
			try
			{
				return ""+(URI)config.getClass().getField("__resource").get(config);
			}
			catch ( Exception ex )
			{
				return null;
			}
		}
	}

	/**
	 * allows the UI to set one additional parameter in addition to
	 * location, user and pwd. For instance, the SPARQL Provider
	 * can override this method and map the parameter to its query
	 */
	public void setParameter( String parameter )
	{
	}

	/**
	 * allows the UI to set one additional parameter in addition to
	 * location, user and pwd. For instance, the SPARQL Provider
	 * can override this method and map the parameter to its query
	 */
	public String getParameter()
	{
		return null;
	}

	// from ProviderState

    /**
     * interval in which provider is run, in ms
     */
    public Long pollInterval;
    
    /**
     * currently running
     */
    public transient Boolean running;
    
    /**
     * last time the provider finished
     */
    public Date lastUpdate;
    
    /**
     * last update duration
     */
    public Long lastDuration;
    
    /**
     * last error msg
     */
    public String error;
    
    /**
     * Unique identifier of a provider.
     * May be used in the context to identify the source.
     */
    public URI providerID;
    
    /**
     * Context in which the extracted ontology is written
     */
    public URI ontologyContext;
    
    /**
     * number of facts in the respective context
     */
    public Integer size;
    
    /**
     * Is provider data editable
     */
    public boolean providerDataEditable;

    @Deprecated
    /**
     * deprecated: keep here for legacy xml serialization (see bug 7993)
     */
    public transient boolean indexingEnabled;

    public boolean userModified;

    public boolean deleted;
    
    /**
     * Is field required
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Required{}
    
    /**
     * Set validator for the field
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface TextInputValidation{
    	public Validation getValidation();
    }
    
    /**
     * Needs the field a textarea for user input
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface TextAreaReq{}
    
    
	/* (non-Javadoc)
	 * @see com.fluidops.iwb.util.Configurable#getConfigurablesClass()
	 */
	public Class<?> getConfigurablesClass(){
		
		return getClass(); 
		
	}
	
	/* (non-Javadoc)
	 * @see com.fluidops.iwb.util.Configurable#getConfigurationClass()
	 */
	public Class<?> getConfigurationClass(){

		return getConfigClass();
	}
}

