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

package com.fluidops.iwb.ui;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FCheckBox;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.FTextInput2.ElementType;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ProviderServiceImpl;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.iwb.user.IwbPwdSafe;
import com.fluidops.iwb.util.Configurable;
import com.fluidops.iwb.util.User;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;
import com.fluidops.util.UnitConverter;
import com.fluidops.util.UnitConverter.Unit;

public class ProviderEditTableForm extends ConfigurationForm {
        
        private static final Logger logger = Logger.getLogger(ProviderEditTableForm.class.getName());
        
        ProviderEditTable providerTable;
        
        /**
         * Input fields for the standard provider fields
         */
        public FTextInput2 providerID;
        public FTextInput2 pollInterval;
        public FTextInput2 userName;
        public FTextInput2 password;
        
        /**
         * Data writable
         */
        public FCheckBox providerDataEditable;
        
        /**
         *  Variable to temporarily store the pw of a user while he edits a provider.
         */
        private String tempPwStored = null;
        
        /**
         * Create provider from for provider config edit
         * @param id ID for the component
         */
        public ProviderEditTableForm(String id) 
        {
            super(id);
            initializeComponents();
        }
        
        /**
         * Initialize a form for an existing provider
         * @param provider Provider to edit
         */

        @Override
        List<Configurable> getConfigurablesList() 
        {
        	List<Configurable> list = new ArrayList<Configurable>();
  	
            try
			{
            	List<String> providerClasses = EndpointImpl.api().getProviderService().getProviderClasses();
            	
                for(String providerClass : providerClasses)
                {
                	list.add((Configurable) Class.forName(providerClass).newInstance());
                }
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			return list;

        }
        
        @Override
        void addStartFields(Class<?> configClass, String doc)
        {
            // Standard fields for every provider
            addFormElement("Provider", configurablesSelectBox, true, new VoidValidator(), "Provider type");
            
            providerID = new FTextInput2("Identifier");
            addFormElement("Identifier", providerID, true, new ConvertibleToUriValidator(), "Identifier of the provider");
            
            pollInterval = new FTextInput2("PollInterval");
            addFormElement("Poll interval", pollInterval, true, new NumberValidator(), "Poll intervall in minutes");
            
            // user input fields
            try{
                // some providers don't have user field in their config
                if(containsUserField(configClass.getDeclaredFields())){
                    userName = new FTextInput2("UserName");
                    addFormElement("Username",userName, false, new VoidValidator(), "Username for the provider.");
                    
                    password = new FTextInput2("password");
                    password.setType(ElementType.PASSWORD);
                    addFormElement("Password",password, false, new VoidValidator(), "Password for the provider.");
                }
            }catch(Exception e){
                logger.error(e);
            }
        }
        
        @Override
        void addEndFields(){
            // Checkbox to make provider data writable
            providerDataEditable = new FCheckBox("providerDataWritable");
            addFormElement("Provider data editable:", providerDataEditable, false, new VoidValidator());
            
            if(providerTable!=null){
                providerTable.populateView();
            }
        }

        
        /**
         * Get provider infos and set input fields
         * @param provider
         */
        public void populateForm(AbstractFlexProvider<Serializable> provider) {

            configurablesSelectBox.setPreSelected(provider.getClass());
            providerID.setValue(EndpointImpl.api().getRequestMapper().getReconvertableUri(provider.providerID,false));
            // convert milliseconds back to minutes for display
            pollInterval.setValue(Integer.valueOf(UnitConverter.convertInputTo(new Double(provider.pollInterval), Unit.MILLISECONDS, Unit.MINUTES).intValue()).toString()); 
            
            // set user if provider has user field
            try{
                if(containsUserField(provider.config.getClass().getDeclaredFields())){
                    User user = null;
                    user = (User)provider.config.getClass().getField("user").get(provider.config);
                    userName.setValue(user.username);
                    
                    // Load the real password from the save and store it temporarily to allow
                    // keeping the PW when it has not been changed (do not take the masked PW!!)
                    tempPwStored = IwbPwdSafe.retrieveProviderUserPassword(provider.providerID, user.username);
                    password.value = user.password; // Masked ! equals("*********") == true !
                    
                    password.populateView();
                }
            }catch(Exception e){
                logger.error(e);
            }
            
            providerDataEditable.checked = provider.providerDataEditable;
            
            // no change of provider while editing provider
            configurablesSelectBox.setEnabled(false);
            // also no change of provider id allowed, because api method generates new provider for different id
            providerID.setEnabled(false);
            
            Class<?> configClass = provider.config.getClass();
            //populate other fields
            populateFieldsFromObject(provider.config, inputs, configClass);
            
        }
        
    /**
     * Adding the dropdownbox for provider selection
     */
    @Override
    public void addDropDownBox(){

        // Provider dropdown list
        configurablesSelectBox = new FComboBox( "providertypes" ){
            
            // on change load form for chosen provider
            @Override
            public void onChange(){
                List<?> list = this.getSelected();
                AbstractFlexProvider<Serializable> provider = null;
                try {
                    
                    provider = (AbstractFlexProvider<Serializable>)((Class)list.get(0)).newInstance();
                    provider.config = (Serializable) provider.getConfigClass().newInstance();
                    
                } catch (Exception e) {
                    logger.error(e);
                }
                initializeComponents(provider);
                
                configurablesSelectBox.setPreSelected((Class<?>)list.get(0));
                // populate the popup after changing the containing formular
                getPage().getPopupWindowInstance().populateView();
                
            }
        };
        
        // add choices to dropdown list
        try{
            for ( Configurable provider : configurablesList )
            {
                Class<?> c = provider.getClass();
                configurablesSelectBox.addChoice( c.getSimpleName(), c );
            }
        }catch(Exception e){
            logger.error(e);
        }
    }
        

    private boolean providerExists(URI id)
    {
        try
        {
            List<AbstractFlexProvider> providers = EndpointImpl.api().getProviderService().getProviders();
            
            for(AbstractFlexProvider<Serializable> prov : providers)
            {
                if(prov.providerID.equals(id)) return true;
            }
        }
        catch (RemoteException e)
        {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    
    public void setProviderEditTable(ProviderEditTable providerTable){
        this.providerTable = providerTable;
    }


    @Override
    void submitData() {

        
        // get Provider from input form
        AbstractFlexProvider<Serializable> provider = null;
        try{
            provider = (AbstractFlexProvider<Serializable>)((Class)configurablesSelectBox.getSelected().get(0)).newInstance();
        }catch (Exception e) {
            
            logger.error(e);
        }
        
        provider.providerID = EndpointImpl.api().getNamespaceService().guessURI(providerID.getValue());
        
        if(providerExists(provider.providerID)&&!editMode)
            
        {            
            getPage().getPopupWindowInstance().showError("Provider with this id already exists.");
            return;
        
        }
        // Set fields of config to values from the form
        try{
            if (providerID==null)
                throw new Exception("providerID is not a valid URI");
                
            // convert input value from minutes to milliseconds 
            provider.pollInterval = (long)Integer.parseInt(pollInterval.getValue());
            provider.providerDataEditable = providerDataEditable.checked;
            
            provider.config = (Serializable) provider.getConfigClass().newInstance();
            // set user
            if(containsUserField(provider.config.getClass().getDeclaredFields())){
                User user = new User();
                user.username = userName.getValue();
                
                // Save the password
                // In case it is not changed (== do not take the **** as PW), restore the tempPW
                user.password = password.getValue().equals(User.MASKEDPASSWORD) ? tempPwStored : password.getValue();
                // Clean the temporary data
                tempPwStored = null;
                
                provider.config.getClass().getField("user").set(provider.config, user);
            }
            
             Object configInstance = provider.config;
             
             configInstance = collectValues(this, configInstance.getClass(), configInstance);
            
            // Add provider with api method
            EndpointImpl.api().getProviderService()
                    .addProvider(provider.providerID,
                            provider.getClass().getName(),
                            provider.pollInterval.intValue(), provider.config,
                            provider.providerDataEditable);
                
            if(editMode) getPage().getPopupWindowInstance().showInfo("Provider edited");
            else getPage().getPopupWindowInstance().showInfo("Provider added");
        }catch(Exception e){
            logger.error(e);
            if(editMode) getPage().getPopupWindowInstance().showWarning("Provider editing failed"+e);
            else getPage().getPopupWindowInstance().showWarning("Provider adding failed"+e);
        }
        
        // persist to disk
        try
        {
            ProviderServiceImpl.save();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        providerTable.updateTable();
        providerTable.table.populateView();
        
        //refresh the page to update the provider status
        addClientUpdate( new FClientUpdate(Prio.VERYEND, "document.location=document.location;"));
    }
    
    void populateFieldsFromObject(Object obj,Map<String, FComponent> formInputs, Class<?> configClass)
	{
		for(Entry<String, FComponent> entry: formInputs.entrySet()){
			String fieldname = entry.getKey();
			try
			{    
				Object val;

				if(obj instanceof String || obj instanceof URI|| obj instanceof Value ||configClass == Object.class || obj == null)                   
					val = obj;           
				else
					val = configClass.getField(fieldname).get(obj);

				FComponent container = entry.getValue();

				if((container instanceof AddFormContainer)&&val!=null)
				{
					AddFormContainer addCnt = ((AddFormContainer)container);
					if (List.class.isAssignableFrom(val.getClass()))
					{
						for(Object innerConfig : (List<?>)val)
						{
							//create a subform and fill it with values
							SubForm f = addCnt.addInput();
							populateFieldsFromObject(innerConfig,f.inputs,f.inputClass);
						}
					}else
					{
						//create a subform and fill it with values
						addCnt.addInput();

						for(SubForm f : addCnt.forms)
							populateFieldsFromObject(val, f.inputs,f.inputClass);
					}
				}
				formInputs.get(fieldname).setValue(val==null?"":unescape(val.toString())); 

								if(formInputs.get(fieldname) instanceof FComboBox)
								{
									((FComboBox)formInputs.get(fieldname)).setSelected(val==null?"":unescape(val.toString()));
								}
			}
			catch(Exception e)
			{
				logger.error(e);
			}
		}

	}

}