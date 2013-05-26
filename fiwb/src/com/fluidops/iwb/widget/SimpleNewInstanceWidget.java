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

package com.fluidops.iwb.widget;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FForm2;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.Pair;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * @author msc
 */
@TypeConfigDoc("The Simple New Instance Wizard allows the creation of new instances of a specific type. It must be integrated in the wiki page of the type for which a new instance is to be created.")
public class SimpleNewInstanceWidget extends AbstractWidget<SimpleNewInstanceWidget.Config>
{

    private static final Logger logger = Logger
            .getLogger(SimpleNewInstanceWidget.class.getName());

    /**
     * User parameterization
     */
    public static class Config
    {
    	@ParameterConfigDoc(
    			desc = "Save Creation Date (true or false)",
    			defaultValue = "false")
        public Boolean saveCreationDate;
        
    	@ParameterConfigDoc(
    			desc = "Omit dynamic input verification",
    			defaultValue = "false")
        public String omitDynamicVerification = "false";
        
    	@ParameterConfigDoc(
    			desc = "The namespace in which instances will be stored, " +
    					"if different from default namespace")
        public String namespace;
        
    	@ParameterConfigDoc(
    			desc = "Defines which randomization strategy is to be used for the creation of the URI",
    			type = Type.DROPDOWN)
        public RandomizingType randomizingType;
        
    	@ParameterConfigDoc(
    			desc = "Defines the randomiyed pattern of the URIs to be created. " +
    					"Use the variable $random$ in the user defined pattern, this will be replaced by the actual random value calculated according to randomizingType")
        public String randomizingURIPattern;
        
    	@ParameterConfigDoc(
    			desc = "If URI is randomized, this parameter decides about the property " +
    					"to be used as label property for the newly created instance",
    			defaultValue = "rdfs:label")
        public String labelProperty;
        
    }
    
    public static enum RandomizingType
    {
    	UUID, fluidRandom
    }
    
    @Override
    public FComponent getComponent(String id)
    {
        Config c = get();
        final Boolean omitDynamicVerification = 
            c!=null && c.omitDynamicVerification!=null && c.omitDynamicVerification.equals("true");   
        final Boolean saveCreationDate = 
            c!=null && c.saveCreationDate != null && c.saveCreationDate;
        final String namespace = c==null? null : c.namespace;
        
        if (c != null && c.randomizingType != null && !(c.randomizingType == RandomizingType.UUID || c.randomizingType == RandomizingType.fluidRandom ))
        	return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION);
       
        FTextInput2 tfNF = null;
        if (!omitDynamicVerification)
        {
            tfNF =new FTextInput2("FTinp", false)
            {
                @Override
                public Boolean validate()
                {
                	if (value == null || value.isEmpty())
                		return false;
                	if (get() != null && get().randomizingType != null)
                		return true;
                	if(StringUtil.containsNonIriRefCharacter(value,true))
                        return false;

                    return true;
                }
            };
        }
        else
            tfNF = new FTextInput2("FTinp", false);
        
        final FTextInput2 tf = tfNF;
        final FPopupWindow err = new FPopupWindow(Rand.getFluidUUID());
        final FLabel msg = new FLabel(Rand.getFluidUUID(), "");
        final FButton ok = new FButton(Rand.getFluidUUID(), "OK")
        {
            @Override
            public void onClick()
            {
                err.hide();
            }
        };
        err.add(msg);
        err.add(ok);

        final String type = (pc.value != null && pc.value instanceof URI) ? EndpointImpl
                .api().getNamespaceService().getAbbreviatedURI((URI) pc.value)
                : "";
        ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
        
        // label
        String label = dm.getLabelHTMLEncoded(pc.value);
        if (StringUtil.isNullOrEmpty(label))
            label = type;
        if (StringUtil.isNullOrEmpty(label))
            label = "Instance"; 
        
        FForm2 f = new FForm2(id, "Create new " + label)
        {
            @Override
            public void setStringTemplateClazz(String stringTemplateLocation)
            {
                this.stringTemplateLocation = "com/fluidops/ajax/components/FForm2";
            }

            
            @Override
            public void onSubmit(List<Pair<String, FComponent>> list)
            {
            	Config conf = get();
            	
                for (int i = 0; i < list.size(); i++)
                {
                    String tfVal = tf.value;
                    if (StringUtil.isNullOrEmpty(tfVal))
                    {
                        displayError("No resource name specified",
                                "Please specify a non-empty id for the resource you want to create!");
                        return;
                    }
                    else if (conf != null && conf.randomizingType != null) // this is the case if URIs are supposed to be randomly generated
                    {
                    	String uriPattern = conf.randomizingURIPattern;
                    	String labelProp = conf.labelProperty;
                    	String namespace = conf.namespace;
                    	
                    	String random = "";
                    	
                    	// supported random strategies: UUID, fluidUUID
                    	if (conf.randomizingType == RandomizingType.UUID)
                    		random = UUID.randomUUID().toString();
                    	else if (conf.randomizingType == RandomizingType.fluidRandom)
                    		random = Rand.getFluidUUID();
                    	
                    	// double-check: this should never happen since the check for randomizing strategy was done in getComponent,
                    	// only possibility to land here is problem in randomizer
                    	if (StringUtil.isNullOrEmpty(random))
                    		throw new IllegalArgumentException("Unknown randomization scheme");
                    	
                    	ValueFactory vf = ValueFactoryImpl.getInstance();
                    	ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(pc.repository);

                    	// create the URI, if pattern defined based on this pattern, if not: defaultNS:random
                    	String uriString;
                    	if (uriPattern != null)
                    		uriString = uriPattern.replace("$random$", random);
                    	else if (namespace != null)
                    		uriString = namespace + random;
                    	else
                    		uriString = EndpointImpl.api().getNamespaceService().defaultNamespace() + random;
                    	
                    	URI u = vf.createURI(uriString);
                    	Context c = Context.getFreshUserContext(ContextLabel.NEW_INSTANCE_WIZARD);
                    	 
                    	// create the type-statement
                    	Statement s1 =vf.createStatement(u, RDF.TYPE, pc.value);
                    	dm.addToContext(s1, c);
                    	Statement s2;
                    	
                    	// create the label-statement of the newly created instance. Value of the textfield in the wizard
                    	// will be set as the label of the new instance, if configured, the de-facto label can be added
                    	// as another, user-defined property (important for BBC)
                    	if (labelProp != null)
                    		s2 = vf.createStatement(u, EndpointImpl.api().getNamespaceService().getFullURI(labelProp), vf.createLiteral(tfVal));
                    	else
                    		s2 = vf.createStatement(u, RDFS.LABEL, vf.createLiteral(tfVal));
                    	
                    	dm.addToContext(s2, c);
                       
                        dm.close();
                        
                        String redirect = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(u);
                        addClientUpdate(new FClientUpdate("document.location='" + redirect + "'"));
                        return;
                    }
                    else if (StringUtil.containsNonIriRefCharacter(tfVal,true))
                    {
                        displayError(
                                "Resource name contains an invalid character",
                                "The specified resource name contains an invalid character: " +
                                "whitespaces and the characters \", {, }, ^, `, and \\, are not allowed.");
                        return;
                    }

                    URI u = EndpointImpl.api().getNamespaceService().guessURI(tfVal);
                    
                    if (u == null)
                    {
                        displayError("Invalid resource name specified",
                                "The ID you entered contains invalid characters " +
                                "(such as <, >, or other unsupported special characters). " +
                                "Please fix the ID and try again!");
                        return;
                    }
                    else
                    {
                        // move to specified namespace, if URI was created in default NS
                        // (if the namespace is different from the default NS, no changes will be performed)
                        String defaultNS = EndpointImpl.api().getNamespaceService().defaultNamespace();
                        if (!StringUtil.isNullOrEmpty(namespace))
                        {
                            if (defaultNS.equals(u.getNamespace()))
                            {
                                String defaultNSUri = u.toString();
                                String specifiedNSUri = namespace + defaultNSUri.substring(defaultNS.length());
                                u = ValueFactoryImpl.getInstance().createURI(specifiedNSUri);
                            }
                        }
                        
                        ReadWriteDataManager dm = ReadWriteDataManagerImpl
                                .openDataManager(pc.repository);
                        if (dm.resourceExists(u))
                        {
                            displayError(
                                    "Resource '" + tf.value
                                            + "' already exists",
                                   "Please choose another name and try again.");
                            return;
                        }
                        else
                        {
                            Statement s = ValueFactoryImpl.getInstance().createStatement(u, RDF.TYPE,
                                    pc.value);
                            Context c = Context
                                    .getFreshUserContext(ContextLabel.NEW_INSTANCE_WIZARD);
                            dm.addToContext(s, c);

                            // Creation date is stored if checkbox activated
                            if (saveCreationDate)
                            {
                                ValueFactory f = ValueFactoryImpl.getInstance();
                                s = f.createStatement(u, Vocabulary.DC.DATE,
                                        f.createLiteral(ReadDataManagerImpl
                                                .dateToISOliteral(new Date())));
                                dm.addToContext(s, c);
                            }

                            try
                            {
                                KeywordIndexAPI
                                        .replaceKeywordIndexEntry(u);
                            }
                            catch (Exception e)
                            {
                                logger.info(e.getMessage());
                            }

                            String redirect = EndpointImpl.api()
                                    .getRequestMapper()
                                    .getRequestStringFromValue(u);
                            addClientUpdate(new FClientUpdate(
                                    "document.location='" + redirect + "'"));
                        }
                        dm.close();
                    }
                }

            }

            public void displayError(String title, String msgStr)
            {
                err.setTitle(title);
                err.populateView();
                msg.setText(msgStr);
                msg.populateView();
                err.show();
                addClientUpdate(new FClientUpdate("scroll(0,0)"));
            }
        };

        f.addFormElement("Name of Resource", tf);
        f.add(err);
        f.addStyle("width", "400px");
        f.appendClazz("SimpleNewInstanceWidget");
        return f;
    }

    @Override
    public String getTitle()
    {
        String type = "";
        if (pc.value != null && pc.value instanceof URI)
            type = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(
                    (URI) pc.value);

        return "Create new " + type;
    }

    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }
}
