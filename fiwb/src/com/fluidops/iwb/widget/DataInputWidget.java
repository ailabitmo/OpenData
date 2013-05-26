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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.iwb.ajax.FMultiStageInputWizard;
import com.fluidops.iwb.ajax.StatementInputHelper.FieldType;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.service.CodeExecution.WidgetCodeConfig;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * Each customizable new instance widget is composed as follows: 
 * 
 * Wizard Page 1: ID generation
 * (1a) There is a idInput struct to configure id a list of. Each has
 * 
 *      type = URI, LITERAL, DROPDOWN
 *      predicate = a valid predicate
 *      query = a SPARQL select query for dropdown values
 *      
 *             
 * (1b) There is an addition field idNameRule, determining how
 *      (given the input fields) to construct the final name of the resource (in
 *      regexp style), for the names you can use the predicates defined in idInput
 *      
 *      idNameRule = '$BaseURI$/$Param1$/$Person$_$Param3$'
 *      simply concatenates the values from the four fields.
 * 
 * Wizard Page 2: Ontology Section
 * (2a) set field: 
 *      useOntology = 'true|false' (default: false)
 *      to allow user modifications according to ontology
 * 
 * Wizard Page 3:
 * (3a) User-defined values: analogously to Wizard Page 1, there are is a
 *      customInput struct, fixing the layout of the third wizard page.
 *      Example: 
 *      customInput = {{
 *       {{ predicate='label' | type='Literal' }} 
 *      }}
 *      
 *      Supported values:
 *      
 *      predicate: some valid predicate, e.g rdfs:label, myPredicate
 *      type: URI,LITERAL,ONTOLOGY,DROPDOWN
 *      query: a SELECT query to be used for dropdown values (one projection)
 *      values: a list of values used for dropdown
 *
 *      The type ONTOLOGY makes the system infer the type from
 *      the ontology (and uses type URI as a default if type inference fails. Note
 *      that ONTOLOGY is not a valid type in Wizard Page 1 (as we are not dealing
 *      with predefined predicates there). In addition, there is a field the property
 *      multiValue that can be set to 'false' in order to forbid multi-value
 *      input for the particular property. 
 *      
 *      Additional properties that can be set are
 *      "title", "types" (a possibly comma-separated list of types that are used for
 *      typing the object and creating the ontology section), "subject" (the subject
 *      of the object to edit, applies only if no id section is specified), and
 *      finally parameter "omitDescriptionPage" (which can be set to true to turn the
 *      wizard description page off). Finally, there are two fields idTitle and
 *      customTitle that can be used to set the title of the ontology section.
 * 
 * Examples:
 * 
 * <code>
 * {{
 * #widget: DataInput
 * | subject = 'Testpage'
 * | omitDescriptionPage = 'true'
 * | customInput = {{
 *       {{ predicate='label' | type='Literal' }} |
 *       {{ predicate='foaf:Person' | type='URI' | query='SELECT ?person WHERE { ?person rdf:type foaf:Person } LIMIT 10' }} |
 *       {{ predicate='myLiteral' | type='DROPDOWN' | values={{ '"str"' | uri }} | multiValue='false' }} 
 *     }}
 *  }}
 * </code>
 * 
 * <code>
 * {{
 * #widget: DataInput
 * | idTitle = 'Project wizard'
 * | idInput = {{ 
 *       {{ predicate='ProjectName' | type='LITERAL' }} |
 *       {{ predicate='Organization' | type='DROPDOWN' | query='SELECT DISTINCT ?org WHERE { ?org rdf:type <http://xmlns.com/foaf/0.1/Organization> }' }}
 *   }}
 * | idNameRule = '$Organization$/$ProjectName$'
 * | idLabelRule = '$ProjectName$'
 * | idWhitespaceReplacementChar = '_'
 * | useOntology = 'false'
 * | omitDescriptionPage = 'true'
 * }}
 * </code>
 * 
 * To enable code execution onSave:
 * 
 * <code>
 * | onSave = {{ clazz='com.fluidops.iwb.widget.CodeExecutionWidget' | method='testMe3' | args={{'string'}} }}
 * </code>
 * 
 * For type=DROPDOWN, both values and query can be used.
 * 
 * @author msc, as
 */
@TypeConfigDoc("Data Input Widget customizes the creation of new instances while simplifying the modification of existing entities")
public class DataInputWidget extends AbstractWidget<DataInputWidget.Config>
{
    private static final Logger logger = Logger.getLogger(DataInputWidget.class
            .getName());

    private static final String WIZARD_DESCRIPTION_INPUT = "This wizard provides support in creating a new instance of the given type. "
            + "It helps you generating a unique ID for the new instance and allows you "
            + "to add information to the new resource, as well as to associate it with "
            + "other objects in the database.";

    private static final String WIZARD_DESCRIPTION_EDIT = "This wizard provides support in creating, modifying, and deleting data "
            + "associated to the current resource. You can go through the wizard "
            + "step by step and fill in the required information.";

    /**
     * What action to take upon finishing the DataInput widget.
     */
    public static enum AfterFinishAction
    {
    	NONE,
    	RELOAD,
    	REDIRECT_TO_SUBJECT
    }
    
    /**
     * User parameterization
     */
    public static class Config extends WidgetBaseConfig
    {
    	@ParameterConfigDoc(desc = "title")
        public String title;

        // the types which are generated by the wizard;
        // if omitted, we use the type represented by the current page
        // (if we are on a class page) or the types of the current instance
    	@ParameterConfigDoc(
    			desc = "Specifies the types for which the wizard generates data", 
    			type=Type.LIST,
    			listType=URI.class)
        public List<URI> types;

        // the subject represented by the wizard; this field is ignored if
        // the wizard has an ID section which generate the subject;
        // if provided, make sure that subject represents a valid URI
    	@ParameterConfigDoc(
    			desc = "The subject (resource) whose data is modified by the wizard")
        public String subject;

        // omit wizard description page
    	@ParameterConfigDoc(
    			desc = "Omit the description page from the wizard", 
    			defaultValue="false")
        public Boolean omitDescriptionPage;

        // first page of wizard
    	@ParameterConfigDoc(
    			desc = "Sets an individual title for the section id generation.")
        public String idTitle;

    	@ParameterConfigDoc(
    			desc = "Specifies the input fields, which collect user input to generate a new and meaningful ID (URI). " +
    					"This is a parameter group containing one entry per input field. Each entry again is a parameter group " +
    					"with required sub parameters <i>predicate</i> and <i>type</i>, and optional sub parameter <i>query</i>.", 
    			type=Type.LIST, 
    			listType=StatementInput.class)
        public List<StatementInput> idInput;
        
    	@ParameterConfigDoc(
    			desc = "If set to true, information provided in id generation will additionally be stored as triples in the database with <i>subject</i> being the newly created entity.",
    			defaultValue="false")
        public Boolean idGenerateTriples;
        
    	@ParameterConfigDoc(
    			desc = "Saves the type for the object", 
    			defaultValue="true")
        public Boolean idSaveType;        

    	@ParameterConfigDoc(
    			desc = "Whitespace replacement character for ID Section")
        public String idWhitespaceReplacementChar;

    	@ParameterConfigDoc(
    			desc = "Rule for building ID (a URI) of id section")
        public String idNameRule;

    	@ParameterConfigDoc(
    			desc = "Rule for constructing label of id section")
        public String idLabelRule;

        // second page of wizard
    	@ParameterConfigDoc(
    			desc = "Turns the usage of ontoligies on and off", 
    			defaultValue="false")
        public Boolean useOntology;

        // third page of wizard
    	@ParameterConfigDoc(
    			desc = "Set a custong title of a custom data input section")
        public String customTitle;

    	@ParameterConfigDoc(
    			desc = "Specifies the input fields, which collect user input to add to the new instance as subject-predicate-object" +
    					" triples. This is a parameter group containing one entry per input field. Each entry again is a parameter group with required sub parameters <i>predicate</i> and <i>type</i>, and optional sub parameter <i>query</i>", 
    			type=Type.LIST, 
    			listType=StatementInput.class)
        public List<StatementInput> customInput;
        
    	@ParameterConfigDoc(
    			desc = "Save the creation date as outgoing edge of the entity", 
    			defaultValue="false")
        public Boolean saveCreationDate;
        
    	@ParameterConfigDoc(
    			desc = "Save the creator", 
    			defaultValue="false")
        public Boolean saveCreator;

    	@ParameterConfigDoc(
    			desc = "Save the last modification date", 
    			defaultValue="false")
        public Boolean saveLastModificationDate;

        /** See {@link CodeExecutionWidget} for examples */
    	@ParameterConfigDoc(
    			desc = "Optional (static) method to invoke on save", 
    			type=Type.CONFIG)
        public WidgetCodeConfig onSave;
        
    	@ParameterConfigDoc(
    			desc = "Action to take after finishing. If no finishAction is provided, " +
    					"the wizard per default redirects to the page, if it has an ID section, " +
    					"or redirects to the subject, if it has no ID section.",
    			type = Type.DROPDOWN)
        public AfterFinishAction doAfterFinish;
    }
    
    public static class StatementInput 
    {
    	@ParameterConfigDoc(desc = "Predicate of the input field")
    	public String predicate;
    	
    	@ParameterConfigDoc(
    			desc = "Type of the input field", 
    			type=Type.DROPDOWN)
    	public FieldType type;

    	@ParameterConfigDoc(
    			desc = "Query driven auto-suggested values",
    			type = Type.TEXTAREA)
    	public String query;
    	
    	@ParameterConfigDoc(
				desc = "List of suggested values", 
				type=Type.LIST, 
				listType=Value.class)
    	public List<Value> values;
    	
    	@ParameterConfigDoc(
				desc = "Enable/disable multiple value input for the input field", 
				defaultValue="true")
    	public Boolean multiValue;		// used for custom section only
    }
    
    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }

    @Override
    public String getTitle()
    {
        Config c = get();
        return StringUtil.isNullOrEmpty(c.title) ? "Data Manipulation Wizard"
                : c.title;
    }

    @Override
    public FComponent getComponent(String id)
    {
		// If user management is active only users with write access are able to use widget
        Config config = get();

        // convert Boolean null values to true/false
        config.useOntology = config.useOntology != null && config.useOntology;
        config.omitDescriptionPage = config.omitDescriptionPage != null
                && config.omitDescriptionPage;
        config.idGenerateTriples = config.idGenerateTriples != null
                && config.idGenerateTriples;
        config.saveCreationDate = config.saveCreationDate != null
                && config.saveCreationDate;
        config.saveCreator = config.saveCreator != null
                && config.saveCreator;
        config.saveLastModificationDate = config.saveLastModificationDate != null
                && config.saveLastModificationDate;
        config.idSaveType = config.idSaveType==null || config.idSaveType; // default: true!
        
        // Just to handle the case where the rule consists of only the reference to an output value.
        // To avoid interpreting it as an operator, we require that the user encloses the rule in double quotes, e.g. "$firstName$".
        // If the value starts with '"$' and ends with '$"', we remove the double quotes. 
        if(config.idNameRule!=null && config.idNameRule.startsWith("\"$") && config.idNameRule.endsWith("$\"")) {
        	config.idNameRule = config.idNameRule.substring(1, config.idNameRule.length()-1);
        }
        
        if(config.idLabelRule!=null && config.idLabelRule.startsWith("\"$") && config.idLabelRule.endsWith("$\"")) {
        	config.idLabelRule = config.idLabelRule.substring(1, config.idLabelRule.length()-1);
        }
        
        
        // sanity check if there is some obvious problem with the config;
        // also assert that the lengths of comma-separated fields are equal
        // (if defined), so we can safely assume this in the following...
        FComponent error = checkConsistency(id, config);
        if (error != null)
            return error;

        // the following three components could possibly be defined;
        // at least one these wizard sections
        boolean hasIdSection = config.idInput!=null && 
					config.idInput.size()>0;
        boolean hasOntologySection = config.useOntology;
        boolean hasCustomSection = config.customInput!=null && 
        			config.customInput.size()>0;
        if (!hasIdSection && !hasOntologySection && !hasCustomSection)
            return WidgetEmbeddingError
                    .getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION,
                            "The wizard configuration does not define a regular input page.");

        // in the following, initialize the subject ...
        Resource subject = null;
        List<URI> types = null;
        try
        {
            subject = getWizardSubject(hasIdSection, config);
        }
        catch (Exception e)
        {
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.INVALID_WIDGET_CONFIGURATION, e
                            .getLocalizedMessage());
        }
        
        // ... and the types
        try
        {
            types = getWizardTypes(hasIdSection, config, subject);            
        }
        catch (Exception e)
        {
            // type only required if id is to be generated
            if (hasIdSection)
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.INVALID_WIDGET_CONFIGURATION, e
                                .getLocalizedMessage());
        }
        
        // set finish action considering the default behavior
        AfterFinishAction doAfterFinish = null;
        if (hasIdSection)
        {
        	// if there is an ID section, the REDIRECT_TO_SUBJECT is default
        	doAfterFinish = config.doAfterFinish==null ?
        			AfterFinishAction.REDIRECT_TO_SUBJECT : config.doAfterFinish;
        } 
        else
        {
        	// if there is no ID section, then the default is RELOAD
        	doAfterFinish = config.doAfterFinish==null ?
        			AfterFinishAction.RELOAD : config.doAfterFinish;
        }

        // now all what's left to be done is initializing the wizard ...
        FMultiStageInputWizard wizard = new FMultiStageInputWizard(Rand.getIncrementalFluidUUID(), subject,
                types, pc.repository, hasIdSection || (subject != pc.value),
                !config.omitDescriptionPage, config.saveCreationDate, config.saveCreator,
                config.saveLastModificationDate, config.onSave, doAfterFinish, this);
        if (hasIdSection)
            wizard.setDescription(WIZARD_DESCRIPTION_INPUT);
        else
            wizard.setDescription(WIZARD_DESCRIPTION_EDIT);

        // ... and add the id section (if defined) ...
        if (hasIdSection)
        {
            error = wizard.appendIdStatementInputStep(config.idTitle,
                    config.idInput,
                    config.idNameRule, config.idGenerateTriples,
                    config.idLabelRule, config.idWhitespaceReplacementChar, 
                    config.idSaveType, id);
            if (error != null)
                return error;
        }

        // ... the ontology section (if defined) ...
        if (hasOntologySection)
        {
            error = wizard.appendOntologyStatementInputStep();
            if (error != null)
                return error;
        }

        // ... and the custom section (if defined) ...
        if (hasCustomSection)
        {
            error = wizard.appendCustomStatementInputStep(config.customTitle,
                    config.customInput, id);
            if (error != null)
                return error;
        }

        // finally initialize the first step and we're done
        //wizard.returnFirstStep().init();
        
        FContainer cont = new FContainer(id);
        cont.add(wizard);
        
        if(StringUtil.isNotNullNorEmpty(config.width))
        	cont.addStyle("width", config.width +"px");
        if(StringUtil.isNotNullNorEmpty(config.height))
        	cont.addStyle("height", config.height+"px");
        
        wizard.appendClazz("DataInputWidget");
        
        return cont;
    }

    /**
     * Consistency check for wizard configuration. Checks for required fields in 
     * each {@link IDStatementInput} and {@link StatementInput} of the
     * provided configuration.
     * 
     * @param id
     *            the id to be used for constructing the error label
     * @param config
     *            the configuration to check for consistency
     * @return an error label in case of failure, null in case of success.
     */
    private FComponent checkConsistency(String id, Config config)
    {
    	FComponent error = null;
    	if (config.idInput!=null)
    	{
    		if (config.idInput.size()>0)
    			if (StringUtil.isNullOrEmpty(config.idNameRule))
                    return WidgetEmbeddingError.getErrorLabel(id,
                            ErrorType.INVALID_WIDGET_CONFIGURATION,
                            "field 'idNameRule' is empty");
    		
    		for (StatementInput inp : config.idInput)
    		{
    			error = checkConsistency(id, inp);
    			if (error!=null)
    				return error;
    		}
    	}
    	
    	if (config.customInput!=null)
    	{
    		for (StatementInput inp : config.customInput)
    		{
    			error = checkConsistency(id, inp);
    			if (error!=null)
    				return error;
    		}
    	}

    	if (config.onSave!=null)
    	{
			if (StringUtil.isNullOrEmpty(config.onSave.clazz))
				return WidgetEmbeddingError.getErrorLabel(id,
						ErrorType.INVALID_WIDGET_CONFIGURATION,
						"onSave.clazz must not be null");
			if (StringUtil.isNullOrEmpty(config.onSave.method))
				return WidgetEmbeddingError.getErrorLabel(id,
						ErrorType.INVALID_WIDGET_CONFIGURATION,
						"onSave.method must not be null");
		}
    	
        // if no error was detected, indicate success:
        return null;
    }
    
    private FComponent checkConsistency(String id, StatementInput inp) 
    {
    	if (StringUtil.isNullOrEmpty(inp.predicate))
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, 
					"idInput.predicate must not be null");
    	
		if (inp.type == null)
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, 
					"idInput.type must not be null");
		
		return null;
    }

    /**
     * Returns the subject that is associated with the wizard. Implements the
     * following rules: (1) if the wizard has an id section, there is no subject
     * (in that case, the subject will be generated by the id section)
     * otherwise, if (2a) the subject is provided by the config, use the config
     * subject, or (2b) if the subject is not provided by the config, use the
     * URI as subject
     */
    protected Resource getWizardSubject(boolean hasIdSection, Config config)
    {
        if (hasIdSection)
            return null;

        try
        {
            if (!StringUtil.isNullOrEmpty(config.subject))
                return EndpointImpl.api().getNamespaceService().guessURI(
                        config.subject);
            else
                return (Resource) pc.value;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    /**
     * Returns the types that are associated with the wizard, either from the
     * config or from the current resource. Implements the following rules: 
     * If (1) the types are explicitly provided in the config, use these types 
     * (1b) the types are not provided in the config and the wizard has an
     * ID section, use the current subject as type, and if the wizard has no 
     * ID section (2) use the types of the current resource as types.
     * Throws a runtime exception if the extracted type list is empty.
     */
    protected List<URI> getWizardTypes(boolean hasIdSection, Config config,
            Resource subject)
    {
    	// if types are explicitly set by user, return them
        if (config.types!=null)
        	return config.types;
        
        Set<Resource> types = new HashSet<Resource>();
        if (hasIdSection)
        {
            if (pc.value instanceof Resource)
                types.add((Resource) pc.value);
            else
                logger.warn("Cannot cast '" + subject + "' to URI.");
        }
        else if (subject != null)
        {
            ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
            types = dm.getType(subject);
        }

        // cast type list to URI
        List<URI> typesAsUris = new ArrayList<URI>();
        for (Resource type : types)
            if (type instanceof URI)
                typesAsUris.add((URI)type);

        // final check: we should have discovered at least one type
        if (typesAsUris.isEmpty())
            throw new RuntimeException("No valid types specified for input wizard.");
        return typesAsUris;
    }
    
    /**
     * Called when a cancel is executed on the inner wizard. May be overridden.
     */
    public void onWizardCancel()
    {
    	// override if needed
    }

    /**
     * Called when a cancel is executed on the inner wizard. May be overridden.
     */
    public void onWizardFinish()
    {
    	// override if needed
    }

}