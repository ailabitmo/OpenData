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

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.api.dynamic.MetaData;
import com.fluidops.api.dynamic.MetaData.MethodMetaData;
import com.fluidops.api.dynamic.MetaData.ParamMetaData;
import com.fluidops.api.exception.APIException;
import com.fluidops.api.misc.Param;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.TimerRegistry;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.iwb.provider.ExternalProvider;
import com.fluidops.iwb.provider.LookupProvider;
import com.fluidops.iwb.user.IwbPwdSafe;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.iwb.util.ObjectPersistance;
import com.fluidops.iwb.util.User;
import com.fluidops.util.UnitConverter;
import com.fluidops.util.UnitConverter.Unit;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class ProviderServiceImpl implements ProviderService
{


    private static final Logger logger = Logger.getLogger(ProviderServiceImpl.class.getName());
    
    /** Path to property file storing included providers **/
    static final String PROVIDERS_PROP_FILENAME = "providers.prop";
    
    /** Path to where we serialize the providers **/
    static final String PROVIDERS_USER_FILENAME = "providers-user.xml";
    static final String PROVIDERS_SYSTEM_FILENAME = "providers.xml";
    static File SYSTEM_PROVIDER_STORE_PATH = IWBFileUtil.getFileInConfigFolder(PROVIDERS_SYSTEM_FILENAME);
    static File USER_PROVIDER_STORE_PATH = IWBFileUtil.getFileInConfigFolder(PROVIDERS_USER_FILENAME);
    static File PROVIDERS_PROP_PATH = IWBFileUtil.getFileInConfigFolder(PROVIDERS_PROP_FILENAME);
    
    /** Helper to load/store the providers in/from an XML-file **/
    static ObjectPersistance<AbstractFlexProvider> systemProviderPersistance = 
        new ObjectPersistance<AbstractFlexProvider>(SYSTEM_PROVIDER_STORE_PATH.getAbsolutePath());
    static ObjectPersistance<AbstractFlexProvider> userProviderPersistance = 
            new ObjectPersistance<AbstractFlexProvider>(USER_PROVIDER_STORE_PATH.getAbsolutePath());
    
    private static final Predicate<AbstractFlexProvider> IS_USER_PROVIDER = new Predicate<AbstractFlexProvider>()
    {
        @Override
        public boolean apply(AbstractFlexProvider provider)
        {
            return provider.userModified || provider.deleted;
        }
    };

    private static final Predicate<AbstractFlexProvider> IS_SYSTEM_PROVIDER = new Predicate<AbstractFlexProvider>()
    {
        @Override
        public boolean apply(AbstractFlexProvider provider)
        {
            return !IS_USER_PROVIDER.apply(provider);
        }
    };
            
    private static final Predicate<AbstractFlexProvider> IS_NOT_DELETED = new Predicate<AbstractFlexProvider>()
    {
        @Override
        public boolean apply(AbstractFlexProvider provider)
        {
            return !provider.deleted;
        }
    };

    protected AbstractFlexProvider sessionProvider;
    
    @Override
    public Object invoke(String serviceName, String method, Object[] params) throws Exception
    {
        sessionProvider = null;
        Class providerClass = null;

        for (String tempProviderClass: EndpointImpl.api().getProviderService().getProviderClasses())
        {
            Class c = Class.forName(tempProviderClass);
            if (c.getSimpleName().equals(method))
            {
                providerClass = c;
                break;
            }
        }
        
        // TODO Is the "method" arg the constructor? Can it be any other method in the provider??!?
        if (providerClass == null)
            throw new Exception("Provider " + method + " not found or not enabled.");
        
        AbstractFlexProvider p = (AbstractFlexProvider) providerClass.newInstance();
        Class config = p.getConfigClass();
        Serializable object = (Serializable) config.newInstance();

        for (MethodMetaData mmd : getMetaData().methods)
            if (mmd.name.equals(method))
            {
                int index = 0;
                for (ParamMetaData mp : mmd.params)
                {
                    Field f = config.getField(mp.param.getName());
                    f.set(object, params[index++]);
                }
            }

        List<Statement> res = new ArrayList<Statement>();
        p.config = object;
        p.gather(res);

        logger.info("Result size = " + res.size());
        Repository rep = new SailRepository(new MemoryStore());
        rep.initialize();

        RepositoryConnection con = rep.getConnection();
        try
        {
            con.add(res);
            FileOutputStream fos = new FileOutputStream(p.getClass()
                    .getSimpleName()
                    + ".nt");
            NTriplesWriter ntwriter = new NTriplesWriter(fos);
            con.exportStatements(null, null, null, false, ntwriter);

            ntwriter = null;
            fos.flush();
            fos.close();

            sessionProvider = p;

            if (res.isEmpty())
                return "0 results";
            else
                return res.size() + " results: " + res.get(0) + " ...";

        }
        finally
        {
            con.close();
        }
    }

    @Override
    public MetaData getMetaData() throws Exception
    {
        MetaData md = new MetaData();
        for (String s : EndpointImpl.api().getProviderService().getProviderClasses())
        {
            Class c = Class.forName(s);
            AbstractFlexProvider p = (AbstractFlexProvider) c.newInstance();
            Class config = p.getConfigClass();

            MethodMetaData mmd = new MethodMetaData();
            md.methods.add(mmd);
            mmd.name = c.getSimpleName();
            if (c.getAnnotation(TypeConfigDoc.class) != null)
                mmd.doc = ((TypeConfigDoc) c.getAnnotation(TypeConfigDoc.class)).value();
            mmd.returnType = String.class;
            for (Field f : config.getFields())
            {
                ParamMetaData mp = new ParamMetaData();
                mmd.params.add(mp);
                mp.param = new Param(f.getName(), f.getType()
                        .getCanonicalName(), null, false, f
                        .getAnnotation(TypeConfigDoc.class) == null ? null : f
                        .getAnnotation(TypeConfigDoc.class).value());
                mp.param.setAnnotated(true);
                mp.type = f.getType();
                mp.listType = f.getAnnotation(ParameterConfigDoc.class) == null ? null
                        : f.getAnnotation(ParameterConfigDoc.class).listType();
            }
        }
        return md;
    }

    @Override
    public List<String> getProviderClasses() throws RemoteException
    {
        Properties prop = getProvidersProp();
        
        List<String> result = new ArrayList<String>();
        
        Enumeration<Object> keys = prop.keys();
        
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement().toString();
            
            // It will be false if the value is not a valid boolean, no exception will be thrown
            if (Boolean.valueOf(prop.getProperty(key)))
            {
                try
                {
                    // Test that the class name is correct, class is accessible and visible
                    Class c = Class.forName(key);
                    
                    // Test that instantiation won't cause problems
                    c.newInstance();
                    
                    // If everything is ok, include it in the result
                    result.add( key );
                }
                catch (Throwable e)
                {
                    logger.error("Resources for provider " + key + " missing, provider not available!", e);
                }
            }   
        }

        return result;
    }

    @Override
    public void registerProviderClass(String providerClass) throws RemoteException, Exception
    {
        Class provider = Class.forName(providerClass);
        Object o = provider.newInstance();
        if (o instanceof AbstractFlexProvider)
        {
            Properties providersProp = getProvidersProp();
            
            if (providersProp.containsKey(providerClass))
            {
                // If the specified provider already exists, check if it's already enabled
                if (Boolean.valueOf(providersProp.getProperty(providerClass)))
                {
                    logger.info("Provider " + providerClass + " is already registered");
                    return;
                }
                else
                {
                    // If it's not enabled, enable it
                    providersProp.setProperty(providerClass, "true");
                }
            }
            else
            {
                // If specified provider doesn't exist, add it and set it to true
                providersProp.put(providerClass, "true");
            }
            
            saveProvidersProp(providersProp);
        }
        else
            throw new APIException("Provider class must extend AbstractFlexProvider");
    }

    /**
     * returns a read only version of the provider list
     */
    @Override
    public List<AbstractFlexProvider> getProviders()
    {
        return ImmutableList.copyOf( Iterables.filter(providers, IS_NOT_DELETED));
    }

    public void runProvider(URI provider, String parameter) throws Exception
    {
        AbstractFlexProvider ps = lookup(provider);
        if (ps==null)
        	throw new IllegalStateException("Provider could not be found: " + provider);
        load(ps, (parameter != null) ? ValueFactoryImpl.getInstance().createURI(parameter) : null, null, Global.repository, Global.historyRepository);
        save();
        if (ps.error != null)
            throw new RuntimeException(ps.error);
        logger.info("Provider run finished");
    }

    @Override
    public void removeProvider(URI provider, Boolean deleteData)
    throws RemoteException, Exception
    {
        // delete data only if it is explicitly requested
        if (deleteData == null)
            deleteData = false;

        ReadWriteDataManagerImpl dm = null;
        try
        {
            URI providerContext = provider;

            // delete data, if requested
            if (deleteData)
            {
            	 dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
                 Set<URI> uris = new HashSet<URI>();
                 for (Context c :dm.getContextsForSource(providerContext))
         		{
         			URI internalContextURI = c.getURI();
         			List<Statement> list = dm.getStatementsAsList(null, null, null, false, internalContextURI);
         			
         	 		for (Statement stmt : list)
         	 		{
         	 			if (!(stmt.getSubject() instanceof URI))
         	 				continue;	// ignore BNODE here
         	 			uris.add((URI)stmt.getSubject());
         	 		}
         		}
                 dm.deleteExpiredContextsOfSource(providerContext,null,null,null);
            }

            // delete provider
            AbstractFlexProvider flexProvider = lookup(provider);
            
            removeProviderAndSave(flexProvider);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
            if (dm != null)
                dm.close();
        }
    }

    @Override
    public void saveProvider(URI provider, Integer intervalMs)
            throws RemoteException, Exception
    {
        // NOTE: when calling this method in fiwb we ignore the aggregateMask
        // and
        // aggregatePersistMask parameter, since no historic data is maintained
        // there
        // the method is reimplemented in fiwbcom->ProviderServiceWithHistory

        /* AbstractFlexProvider state = lookup(provider);
        if (state == null)
            state = new AbstractFlexProvider(); */

        // use the session's provider
        AbstractFlexProvider prov = sessionProvider;

        if (intervalMs != null)
            prov.pollInterval = intervalMs.longValue();

        putProvider(provider, prov);
    }

    @Override
    public void addProvider(URI provider, String providerClass,
            Integer interval, Serializable config, 
            Boolean providerDataWritable) throws RemoteException, Exception
    {
        // NOTE: when calling this method in fiwb we ignore the aggregateMask
        // and
        // aggregatePersistMask, since no historic data is maintained there
        // the method is reimplemented in fiwbcom->ProviderServiceWithHistory

        AbstractFlexProvider prov = lookup(provider);
        if (prov == null)
            prov = (AbstractFlexProvider) Class.forName(providerClass).newInstance();

        Long intervalMs = (UnitConverter.convertInputTo(new Double(interval), Unit.MINUTES, Unit.MILLISECONDS)).longValue(); 
        if (intervalMs != null)
            prov.pollInterval = intervalMs;
        
        
        if (config != null)
            prov.config = config;
        
        if(providerDataWritable==null){
            providerDataWritable = false;
        }
        prov.providerDataEditable = providerDataWritable;
        
        putProvider(provider, prov);
    }

    protected void putProvider(URI context, AbstractFlexProvider prov) throws IOException
    {
        if (prov == null)
            throw new APIException("no provider specified");
        prov.providerID = context;

        // need to mask any passwords with ****** and save the real passwords to PwdSafe
        List<User> users = new ArrayList<User>();
        collectUserObjects(prov.config, users);
        for (User user : users)
        {
        	IwbPwdSafe.saveProviderWithUserAndPassword(context, user.username, user.password);
            user.password = User.MASKEDPASSWORD;
        }

        // (aeb) I guess that this contains check is to make sure providers 
        // are not entered twice from the UI. AbstractFlexProvider does not
        // implement equals(), thus this is only a check for object identity
        if(!providers.contains(prov))
            providers.add(prov);
        prov.userModified = true;
        save();
    }

    protected void collectUserObjects(Object config, List<User> users)
            throws IOException
    {
        if (config == null)
            return;

        if (config instanceof User)
            users.add((User) config);

        for (Field f : config.getClass().getFields())
        {
            if (Modifier.isTransient(f.getModifiers()))
                continue;
            if (Modifier.isStatic(f.getModifiers()))
                continue;

            try
            {
                Object kid = f.get(config);
                if (kid instanceof Object[])
                    for (Object item : (Object[]) kid)
                        collectUserObjects(item, users);
                else if (kid instanceof Collection)
                    for (Object item : (Collection) kid)
                        collectUserObjects(item, users);
                else
                    collectUserObjects(kid, users);
            }
            catch (Exception e)
            {
                throw new IOException(
                        "Error saving config object. Could not check for User data. Make sure config objects are simple pojos with public fields",
                        e);
            }
        }
    }

    /**
     * read access must use the method getProviders to get a read only version of this list
     */
    private static final List<AbstractFlexProvider> providers = new CopyOnWriteArrayList<AbstractFlexProvider>();

    /**
     * thread safe version of provider.remove( provider )
     * @param provider
     */
    protected void removeProviderAndSave( AbstractFlexProvider provider ) throws IOException
    {
        provider.deleted = true;
        
        // remove passwords saved by provider
        List<User> users = new ArrayList<User>();
        collectUserObjects(provider.config, users);
        for (User user : users)
        {
        	IwbPwdSafe.deleteProviderUser(provider.providerID, user.username);
        }
        
        save();
    }
    
    protected AbstractFlexProvider lookup(URI context)
    {
        if (context==null)
            return null;
        
        for (AbstractFlexProvider afp: getProviders())
        {
            if (afp.providerID.equals(context))
                return afp;
        }
        
        // Nothing found
        return null;
    }

    // VS remove from API?
    public void load() 
    {
        loadWithoutScheduling();

        scheduleProviders();
    }

    Timer scheduleProviders()
    {
        TimerTask timerTask = new TimerTask()
        {
            Queue<AbstractFlexProvider> queue = new ArrayBlockingQueue<AbstractFlexProvider>(10000);

            @Override
            public void run()
            {
                try
                {
                    if (queue.isEmpty())
                    {
                        // add any work
                            for (AbstractFlexProvider s : getProviders())
                            {
                            	if (s.pollInterval<=0)
                            		continue; // disabled 
                            	
                                if (s.running != null && s.running == true)
                                    continue;

                                if (s instanceof ExternalProvider)
                                    continue;

                                if (s instanceof LookupProvider)
                                    continue;

                                if (s.lastUpdate == null)
                                    queue.add(s);
                                else if (s.lastUpdate.getTime()
                                        + s.pollInterval < System
                                        .currentTimeMillis())
                                    queue.add(s);
                            }
                    }
                    if (!queue.isEmpty())
                    {
                        AbstractFlexProvider provider = queue.poll();
                        runProvider(provider.providerID, null);
                    }
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        };
        Timer timer = new Timer("IWB Provider Update");
        timer.schedule(timerTask, 1000, 1000);
        TimerRegistry.getInstance().registerProviderServiceTimer(timer);
        return timer;
    }

    void loadWithoutScheduling()
    {
        try
        {
            providers.clear();
            if(SYSTEM_PROVIDER_STORE_PATH.exists())
                providers.addAll(systemProviderPersistance.load());
            if(USER_PROVIDER_STORE_PATH.exists()) {
                List<AbstractFlexProvider> userProviders = userProviderPersistance.load();
                for (AbstractFlexProvider provider : userProviders)
                {
                    AbstractFlexProvider existingProvider = lookup(provider.providerID);
                    if(existingProvider != null) providers.remove(existingProvider);
                }
                providers.addAll(userProviders);
            }
        }
        catch (Exception e)
        {
            logger.warn("Could not read/process file \"" + SYSTEM_PROVIDER_STORE_PATH.getAbsolutePath() +"\"");
        }
    }

    public static synchronized void save() throws IOException
    {
        userProviderPersistance.save(newArrayList(filter(providers, IS_USER_PROVIDER)));
        systemProviderPersistance.save(newArrayList(filter(providers, IS_SYSTEM_PROVIDER)));
    }

    @Override
    public void uploadProviderData(URI providerURI, byte[] data, String format)
            throws RemoteException, Exception
    {
        AbstractFlexProvider provider = new ExternalProvider();
        provider.providerID = providerURI;

        Repository rep = new SailRepository(new MemoryStore());
        rep.initialize();

        RepositoryConnection con = rep.getConnection();

        try
        {
            con.add(new ByteArrayInputStream(data), EndpointImpl.api()
                    .getNamespaceService().defaultNamespace(), ReadDataManagerImpl
                    .parseRdfFormat(format));
            List<Statement> l = new ArrayList<Statement>();
            RepositoryResult<Statement> res = con.getStatements(null, null,
                    null, false);

            while (res.hasNext())
                l.add(res.next());
            load(provider, null, l, Global.repository, Global.historyRepository);
        }
        finally
        {
            con.close();
        }
        putProvider(providerURI, provider);
    }

    /**
     * Note: the history repository is only used in the fiwbcom version of the
     * method, i.e. when overriding the method in class ProviderServiceImplCom.
     * No historic data management is done at all in the fiwb version.
     * 
     * @param provider
     * @param data
     *            Externally passed data
     * @param repository
     * @param historyRepository
     */
    private void load(AbstractFlexProvider provider, URI parameter, List<Statement> data,
            Repository repository, Repository historyRepository) throws Exception
    {   
    	logger.info("Starting provider with ID " + provider.providerID.stringValue());
        // fetch new data from provider
        List<Statement> newStmts = new LinkedList<Statement>();
        long start = System.currentTimeMillis();
        provider.running = true;
        try
        {
            if (data != null)
                newStmts = data; // data provided externally, there is no
            // need to run the provider
            else
            {
                if(parameter!=null && provider instanceof LookupProvider)
                {
                    ((LookupProvider)provider).gather(newStmts, parameter);
                }
                else
                    provider.gather(newStmts);
                
            }
                
            provider.error = null;

            long now = System.currentTimeMillis();
            processProviderData(provider, newStmts, parameter, repository, historyRepository, now);
            provider.size = newStmts.size();
        }
        catch (Throwable t)
        {
            logger.error("Provider load error: ", t);
            StringBuilder error = new StringBuilder();
            error.append(t.getMessage());
            for (StackTraceElement tl : t.getStackTrace())
                error.append("\n").append(tl.toString());
            provider.error = error.toString();            
        }
        
        // update statistics
        provider.lastUpdate = new Date();
        provider.lastDuration = System.currentTimeMillis() - start;
        provider.running = false;
        
        logger.info("Provider run of provider with ID " + provider.providerID + " finished");
    }

    /**
     * Processes the provider data that has been gathered.
     * 
     * @param newStmts
     * @param parameter
     * @param repository
     * @param historyRepository
     */
    protected synchronized void processProviderData(AbstractFlexProvider provider,
            List<Statement> newStmts, URI parameter, Repository repository,
            Repository historyRepository, long now)
    {
        URI providerId = provider.getProviderID();
        
        ReadWriteDataManager dm = null;
        dm = ReadWriteDataManagerImpl.openDataManager(repository);

        Set<URI> uris = new HashSet<URI>();
        for (Context c :dm.getContextsForSource(providerId))
		{
			
        	/* Collect all URIs occuring as subjects in any statement of the previous provider run */
        	URI internalContextURI = c.getURI();
			RepositoryResult<Statement> res;
			try
			{
				res = dm.getStatements(null, null, null, false, internalContextURI);
				
				while (res.hasNext())
				{
					Resource subject = res.next().getSubject();
					if (subject instanceof URI)
						uris.add((URI)subject);
				}
			} 
			catch (RepositoryException e1)
			{
				logger.warn(e1.getMessage(), e1);
			}
			
		}
        
        Long contextExpirationTimeMS = provider.getContextExpirationTimeMS();

        Context c = dm.updateDataForSrc(providerId, parameter,
                        Context.ContextType.PROVIDER,
                        ContextLabel.REGULAR_PROVIDER_RUN, newStmts,
                        contextExpirationTimeMS);

        /* Calculate the diff of old subjects and new subjects (i.e. all the URIs that occured 
         * in the last run, but do not occur in the current run anymore) */
        for (Statement st : newStmts)
        	uris.remove(st.getSubject());
        
        // make provider editable
        try 
        {
            if (!newStmts.isEmpty())
                dm.setContextEditable(c,provider.providerDataEditable);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        if (dm != null)
            dm.close();

    }
    
    private static Properties getProvidersProp()
    {
        Properties properties = new Properties();
        FileInputStream propertyFileStream = null;
        try
        {
            propertyFileStream = new FileInputStream(PROVIDERS_PROP_PATH);
            properties.load(propertyFileStream);
        }
        catch (FileNotFoundException e) 
        {
            // TODO Create default one?
            
            logger.error("Providers properties file not found", e);
        } 
        catch (IOException e)
        {
            logger.error("Could not load providers properties file", e);
        }
        finally
        {
            IOUtils.closeQuietly(propertyFileStream);
        }
        
        return properties;
    }
    
    private static void saveProvidersProp(Properties providersProp)
    {
        FileOutputStream propertyFileStream = null;
        try
        {
            propertyFileStream = new FileOutputStream(PROVIDERS_PROP_PATH);
            providersProp.store(propertyFileStream, null);
        }
        catch (FileNotFoundException e)
        {
            // It has to be there as part of the default config, we don't create it if it's not there
            logger.error("Providers properties file not found", e);
        }
        catch (IOException e)
        {
            logger.error("Could not save the providers properties file", e);
        }
        finally 
        {
            IOUtils.closeQuietly(propertyFileStream);
        }
    }


    /**
     * Run a provider staticallly, if it is contained in the list of providers.
     * If the provider of the given type is contained multiple times, all instances
     * of the provider will be triggered.
     */
    public static void runStaticIfDefined(Class c)
    {
        try
        {
            ProviderService ps = EndpointImpl.api().getProviderService();
            List<AbstractFlexProvider> providers = ps.getProviders();
            for (int i=0; i<providers.size(); i++)
            {
                AbstractFlexProvider provider = providers.get(i);
                if (provider.getClass().getName().equals(c.getName())) 
                {
                    try
                    {
                        EndpointImpl.api().getProviderService().runProvider(provider.getProviderID(), null);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.warn(e.getMessage(),e);
        }
    }
}