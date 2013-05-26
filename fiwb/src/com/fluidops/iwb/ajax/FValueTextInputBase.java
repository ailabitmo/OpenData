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

package com.fluidops.iwb.ajax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventType;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FGroupedDataView;
import com.fluidops.ajax.components.FTextInput2.ComparisonType;
import com.fluidops.ajax.components.groupeddataview.EditFieldComponent;
import com.fluidops.ajax.helper.FHelpers;
import com.fluidops.config.Config;
import com.fluidops.iwb.api.APIImpl;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.editor.Datatype;
import com.fluidops.iwb.api.editor.DatatypeValidation;
import com.fluidops.iwb.autocompletion.AutoCompletionUtil;
import com.fluidops.iwb.autocompletion.AutoSuggester;
import com.fluidops.iwb.util.validator.Validator;
import com.fluidops.util.Rand;

/**
 * Implements the abstract basis for a flexible input component, possibly
 * supporting inputs of any type of RDF values (URIs, literals, blank nodes) at
 * the same time.<br/>
 * <br/>
 * The actual type is primarily being guessed based on current user input and an
 * optional preferred (initial) type. Additional restrictions that are possibly
 * implemented in reimplementing classes can limit the actual choice of type to
 * a subset (e.g., URIs only or Resources only). Additional external limitations
 * are supported through auto-suggestion components. The preferred range of
 * literals or resources could be further narrowed down to specific types.<br/>
 * Suggestions of all kind could also be made mandatory requirements for
 * admissible input (see {@link #restrictInputValues(boolean)}).<br/>
 * If more than one type is allowed, a typing dropdown will automatically be
 * added. This also happens when system parameter displayDatatypeDropdown (see
 * {@link com.fluidops.iwb.util.Config#displayDatatypeDropdown()}) is enabled.<br/>
 * <br/>
 * FValueInput implements {@link EditFieldComponent} and could thus listen to a
 * number of FAJAX events, grab its own focus or could be used by
 * {@link FGroupedDataView} as an edit field out of the box.
 * 
 * @author cp
 * @note This class is not abstract by lack of implemented functionality but as
 *       a consequence of a design decision: the class as is implements all the
 *       functionality reused by reimplemented classes but would expose an
 *       overly complex interface mixing functionality for several similar, yet
 *       distinct use cases when used directly. To allow a shared code basis for
 *       these scenarios and still provide a clean and comprehensible API in the
 *       absence of multiple inheritance, this class is marked as abstract and
 *       lacks some of the setters and getters it would otherwise implement.
 */
public abstract class FValueTextInputBase extends FValueInputBase
{
    private FValueHoldingTextInput mainInput;

    private FComponent typeComponent;

    private boolean uiInitialized;

    private String origValStr;

    private URI origType;

    private URI currentType;

    private boolean allowEmpty;

    private AutoSuggester suggester;

    private boolean restrictToSuggestions;

    private List<URI> suggestedUriTypes;

    private List<URI> suggestedLiteralTypes;
    

    /**
     * Constructs a value input field with the specified ID and initial field
     * value.
     * 
     * @param id
     *            FComponent ID.
     * @param initValue
     *            Value to edit.
     */
    public FValueTextInputBase(String id, Value initValue)
    {
        super(id);
        init(initValue);
    }

    /**
     * Constructs an empty value input field with the specified ID.
     * 
     * @param id
     *            FComponent ID.
     */
    public FValueTextInputBase(String id)
    {
        super(id);
        init(null);
    }

    private void init(Value val)
    {
        // initial value
        origVal = val;


        if (origVal == null) {
            origValStr = "";
        } else {
        	origValStr = AutoCompletionUtil.toDisplayValue(val, false);
        }

        origType = null;
        currentType = null;

        // input limitations/suggestions
        allowResource = true;
        allowLiteral = true;
        allowEmpty = false;
        restrictToSuggestions = false;
        suggestedUriTypes = new ArrayList<URI>();
        suggestedLiteralTypes = new ArrayList<URI>();

        // required for proper suggestions' alignment
        appendClazz("floatRightIwb");

        uiInitialized = false;
    }
    
    
    /**
     * Initializes and adds the UI sub components.
     */
    private void initUI()
    {
        uiInitialized = true;

        setCellOrientation(VerticalOrientation.CENTER);
        addStyle("margin", "0");

        // main component
        add(createMainComponent());

        // typing component
        add(createTypeComponent());

        // validation on main component
        mainInput.setValidator(new Validator()
        {
            @Override
            public boolean validate(FComponent c)
            {
                return checkValidity(mainInput.value, currentType);
            }
        });

    }
    

    /**
     * Retrieves the explanatory tooltip for the main input component.
     */
    private String explainInput()
    {
        if (currentType == null)
            return ""; // no clue what to explain here

        APIImpl api = EndpointImpl.api();
        ReadDataManagerImpl dm = api.getDataManager();

        if (currentType.equals(RDFS.RESOURCE))
        { // URI
        	Value val = mainInput.getOrCreateRDFValue(currentType, getLanguageTag());
        	if(val==null || !(val instanceof URI)) {
        		// Not a proper resource, so nothing to explain
        		return "";
        	} else {
        		return FHelpers.escapeJavaScriptAndHtml(dm.getLabel(val)) 
        				+ " (URI " 
        				+ FHelpers.escapeJavaScriptAndHtml(val.toString()) 
        				+ ")";
        	}
        }
        else if (currentType.equals(RDFS.LITERAL))
        {
            return "Untyped literal value \\n\"" + FHelpers.escapeJavaScriptAndHtml(mainInput.getValue()) + "\"";
        }
        else
        { // Specific literal
            return dm.getLabel(currentType) + " literal value \\n\""
                    + FHelpers.escapeJavaScriptAndHtml(mainInput.getValue()) + "\"";
        }
    }

    /**
     * Checks the validity of current input and returns the results.
     * 
     * @return Whether or not the current input is valid.
     */
    private boolean checkValidity(String valStr, URI currentType)
    {
        assert currentType!=null;

        if (valStr.equals("")) // special case
            return allowEmpty;

        mainInput.onChange();
        // check on suggestions (if restricted to those)
      
        if (restrictToSuggestions
                && mainInput.getChosenRDFValue()==null)
            return false;
        
        if (restrictToSuggestions && suggestedUriTypes.size() > 0 && currentType.equals(RDFS.RESOURCE))
        {
        	
        	Value val = mainInput.getOrCreateRDFValue(currentType, getLanguageTag());
        	
			if (val instanceof URI) {

				// URI inUri =
				// EndpointImpl.api().getNamespaceService()
				// .guessURI(mainInput.getValue());
				// check on restricted URI types
				for (Resource t : EndpointImpl.api().getDataManager()
						.getPropertyInfo((URI) val).getTypes())
					if (suggestedUriTypes.contains(t))
						return true; // valid type found
			}
            // no valid type
            return false;
        }
        
        // check for known types, i.e. various XSD datatypes
        // Validation should not happen for resources selected from the autosuggestion list,  
        // because the validation method tries to guess the URI for the text input, which would fail if 
        // the input value is a display label.
        if(!currentType.equals(RDFS.RESOURCE) || mainInput.getChosenRDFValue()==null) {
        	DatatypeValidation dv = DatatypeValidation.validatorFor(currentType);
        	if (dv!=null)
        		return dv.validate(valStr);
        }

        // "unknown" types should always be valid
        return true;
    }
    
    @Override
    public Value getRdfValue()
    {
        assert currentType != null;

        // -> only return if valid
        if (!checkValidity(mainInput.value, currentType))
            return null;
        
        return mainInput.getOrCreateRDFValue(currentType, getLanguageTag());
    }
    
    
    private String getLanguageTag() {
    	return (origVal!=null && origVal instanceof Literal) ? 
    				((Literal)origVal).getLanguage() : null;
    }
    
    
    @Override
    public boolean isEmpty()
    {
        return mainInput.getValue().equals("");
    }

    /**
     * Changes the admissible input, allows or disallows the empty field as
     * input. Initially, empty input is not allowed.
     * 
     * @param allow
     */
    public void allowEmpty(boolean allow)
    {
        allowEmpty = allow;
    }


    /**
     * Sets a suggester instance for auto suggestions.
     * 
     * @param suggester
     *            URI suggester object.
     */
    public void setSuggester(AutoSuggester suggester)
    {
        this.suggester = suggester;
    }

    /**
     * Sets or releases restrictions on admissible input. When restricted, only
     * values produced by the URI and/or literal suggester (see
     * {@link #setUriSuggestions(UriSuggester)},
     * {@link #setLiteralSuggestions(AutoSuggester)}) are admissible as input,
     * all other potential input will be highlighted red and classified as
     * inadmissible. Also, input will be restricted to suggested URI and/or
     * literal types (see {@link #setSuggestedUriTypes(List)},
     * {@link #setSuggestedLiteralTypes(List)}).
     * 
     * @note If you wish to only restrict types but not particularly suggested
     *       values, use a graceful auto-suggester for value suggestions (i.e.,
     *       one that always also suggests the value it sees as current input)
     * @param restrictToSuggestions
     */
    public void restrictInputValues(boolean restrictToSuggestions)
    {
        this.restrictToSuggestions = restrictToSuggestions;
    }

    /**
     * Sets or releases a restriction of preferred literal types. Preferred
     * literal types can always be chosen from the type dropdown. When also
     * restricted (@link {@link #restrictInputValues(boolean)}), only literals
     * matching the specified type are admissible as literal input. Also, the
     * first preferred type would be preselected on empty input fields.
     * 
     * @note Note, that no literals at all are admissible if disallowed by
     *       {@link #allowLiteral(boolean)}.
     * @param types
     *            URI of required XSD type, null to lift restrictions. Use URIs
     *            from {@link XMLSchema} for standard types.
     */
    public void setSuggestedLiteralTypes(List<URI> types)
    {
        suggestedLiteralTypes = types;
    }

    public void setSuggestedUriTypes(List<URI> preferredUriClasses)
    {
        suggestedUriTypes = preferredUriClasses;
    }

    /**
     * After a UI side change, checks whether this is a real change or rather
     * restoring the original state, then calls corresponding callbacks.
     */
    private void callOnChangeCallbacks(FEvent event)
    {
        // callback handling
        if (mainInput.value.equals(origValStr) && currentType.equals(origType))
        {
            if (restoreCB != null)
                restoreCB.handleClientSideEvent(event);
        }
        else
        { // real change
            if (changeCB != null)
                changeCB.handleClientSideEvent(event);
        }

        // update input explanation tooltip
        addClientUpdate(new FClientUpdate("$('" + mainInput.getId()
                + "')['title'] = '" + explainInput() + "';"));
    }

    @Override
    public void handleClientSideEvent(FEvent event)
    {
        switch (event.getType())
        {
        case BLUR:
            if (blurCB != null)
                blurCB.handleClientSideEvent(event);

            break;

        case FOCUS:
            if (focusCB != null)
                focusCB.handleClientSideEvent(event);
            break;

        case GENERIC: // subsumes ENTER, ESC & TAB for this purpose
            if (event.getArgument().equals("10"))
            { // ENTER
                if (acceptCB != null)
                    acceptCB.handleClientSideEvent(null);
            }
            else if (event.getArgument().equals("27"))
            { // ESC
                if (cancelCB != null)
                    cancelCB.handleClientSideEvent(null);
            }
            else if (event.getArgument().equals("9"))
            { // TAB
                if (tabOutCB != null)
                    tabOutCB.handleClientSideEvent(null);
            }
            break;

        default:
            super.handleClientSideEvent(event);
        }
    }

    private FComponent createMainComponent()
    {

    	final FComponent container = this;
    	
        mainInput = new FValueHoldingTextInput(Rand.getIncrementalFluidUUID(), origValStr)
        {
        	
        	@Override
            public void populateView()
            {
                super.populateView();

                // client side/html id of this component:
                String id = getId();

                // code to suppress some errors unless in debug mode
                String checkExistenceInDOM =
                        Config.getConfig().isAjaxDebug() ? "" : "if ($('" + id
                                + "')) ";

                // Focus
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id,
                        "Focus", 4, true));

                // Blur
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id, "Blur",
                        4, true));

                // ENTER & ESC (subsumed under event type GENERIC, number 0, see
                // FEventType) - will be sent to including container, not to
                // this input field.
                addClientUpdate(new FClientUpdate(
                        Prio.VERYEND,
                        checkExistenceInDOM
                                + "cbAddEventListener($('"
                                + id
                                + "'), 'keyup', "
                                + "function(e) { var arg = 0; "
                                + "if (e.keyCode == 10 || e.keyCode == 13) arg = 10; " // ENTER
                                + "if (e.keyCode == 27) arg = 27; " // ESC
                                + "if (arg > 0) { catchEventId('"
                                + container.getId() + "', 0, arg); } }, "
                                + "true);"));

                // TAB (subsumed under event type GENERIC, number 0, see
                // FEventType) - special handling for tab out only if there
                // is no extra sub component (choosing type) that takes the
                // focus.
                if (typeComponent == null)
                { // tab out right from main component, as type component is
                  // inactive
                    addClientUpdate(new FClientUpdate(Prio.VERYEND,
                            checkExistenceInDOM + "cbAddEventListener($('" + id
                                    + "'), 'keydown', " + "function(e) { "
                                    + "if (e.keyCode == 9) { catchEventId('"
                                    + container.getId()
                                    + "', 0, 9); return false; } }, "
                                    + "true);"));
                }

            }

			@Override
            public void handleClientSideEvent(FEvent event)
            {
                if (event.getType() == FEventType.INPUT_CHANGED)
                {
                    super.handleClientSideEvent(event);
                    callOnChangeCallbacks(event);
                }
                else
                    // need that in all cases for other processing steps
                    super.handleClientSideEvent(event);
            }
        };
        
        mainInput.setRDFValue(origVal);
        
        mainInput.suggester = suggester;
        
        mainInput.setComparisonType(ComparisonType.Contains);

        return mainInput;
    }

    private FComponent createTypeComponent()
    {
        final FComponent container = this;

        // construct the combobox with suggestions
        FComboBox typeInput = new FComboBox(Rand.getIncrementalFluidUUID())
        {

            @Override
            public void populateView()
            {
                super.populateView();

                String id = getId();

                String checkExistenceInDOM =
                        com.fluidops.config.Config.getConfig().isAjaxDebug() ? ""
                                : "if ($('" + id + "')) ";

                // Focus
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id,
                        "Focus", 4, true));

                // Blur
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id, "Blur",
                        4, true));

                // special handling of tab outs; reports to container
                addClientUpdate(new FClientUpdate(Prio.VERYEND,
                        checkExistenceInDOM + "cbAddEventListener($('" + id
                                + "'), 'keydown', " + "function(e) { "
                                + "if (e.keyCode == 9) { catchEventId('"
                                + container.getId()
                                + "', 0, 9); return false; } }, " + "true);"));

            }

            @Override
            public void onChange()
            {
                // remember which type to try when saving
                currentType = (URI) this.getSelected().get(0);

                // re-validate
                mainInput.populateValidity();

                // possibly notify change
                callOnChangeCallbacks(null);
            }
        };

        // initializing...
        HashSet<URI> literalMenuEntries = new HashSet<URI>();

        // (1) add explicitly suggested literal types (if any)
        for (URI dt : suggestedLiteralTypes)
        {
            typeInput.addChoice(Datatype.getLabel(dt), dt);
            literalMenuEntries.add(dt);
        }

        // (2) add general resource type (if allowed)
        if (allowResource)
            typeInput.addChoice(Datatype.getLabel(RDFS.RESOURCE), RDFS.RESOURCE);

        // (3) add basic literal type (if allowed, depending on restrictions)
        if (restrictToSuggestions)
        {
            // restricted: basic literal only if no literal types suggested
            if (allowLiteral && suggestedLiteralTypes.size() == 0)
            {
                typeInput.addChoice(Datatype.getLabel(RDFS.LITERAL), RDFS.LITERAL);
                literalMenuEntries.add(RDFS.LITERAL);
            }
        }
        else
        {
            // unrestricted: basic literal whenever literals allowed
            if (allowLiteral)
            {
                typeInput.addChoiceIfAbsent("Literal (any/untyped)",
                        RDFS.LITERAL);
                literalMenuEntries.add(RDFS.LITERAL);
            }
        }

        // (4) add all remaining literal types if unrestricted and explicit
        // typing for data types switched on by global configuration
        if (allowLiteral
                && !restrictToSuggestions
                && com.fluidops.iwb.util.Config.getConfig()
                        .displayDatatypeDropdown())
        {
        	for (Datatype d : Datatype.values()) {
        		typeInput.addChoiceIfAbsent(d.toString(), d.getTypeURI());
        		literalMenuEntries.add(d.getTypeURI());
        	}
        }

        // we have arranged types s.t. preferred types are listed first; still,
        // preselection also needs to consider the current (loaded) value
        typeInput.clearSelected();

        if ((origVal instanceof Resource || origVal == null) && allowResource)
            typeInput.addSelected(RDFS.RESOURCE);
        else if (origVal instanceof Literal && allowLiteral)
        {
            Literal lit = (Literal) origVal;

            if (literalMenuEntries.contains(lit.getDatatype()))
                typeInput.addSelected(lit.getDatatype());
            else if (literalMenuEntries.contains(RDFS.LITERAL))
                typeInput.addSelected(RDFS.LITERAL);
        }

        // fallback (e.g. for empty fields), preferred type should be first:
        if (typeInput.getSelected().size() == 0
                && typeInput.getChoices().size() > 0)
            typeInput.addSelected(typeInput.getChoices().get(0).snd);

        // selection represents the current type at initialization (=
        // original/initial type)
        if (typeInput.getSelected().size() > 0)
            origType = (URI) typeInput.getSelected().get(0);

        currentType = origType;

		// fine, there is a choice, keep dropdown
		typeComponent = typeInput;
		return typeInput;
    }

    /**
     * Runs parent's populateView() method, then adds updates to capture
     * additional events.
     */
    @Override
    public void populateView()
    {
        if (!uiInitialized)
            initUI();

        super.populateView();

        String id = getId();

        // code to suppress some errors unless in debug mode
        String checkExistenceInDOM =
                com.fluidops.config.Config.getConfig().isAjaxDebug() ? ""
                        : "if ($('" + id + "')) ";

        // MouseOut/In reported to specific receiver method
        addClientUpdate(FGroupedDataView.reportMethodUpdate(id, false));
        addClientUpdate(FGroupedDataView.reportMethodUpdate(id, true));

        // insert methods to receive manually propagated focus/blur events from
        // children (very similar to event bubbling but allows to control
        // delayed processing respecting "non-events"
        // occurring from focus switches between sub components)

        // TODO: these two calls should be replaced with simpler/shorter
        // invocations of ajax.js's collectorMethodUpdate method OR a comment
        // explaining why this is not possible in this case, see bug 8654
        addClientUpdate(new FClientUpdate(Prio.VERYEND, checkExistenceInDOM
                + "$('" + id + "')['reportedFocus'] = function() { "
                + "var el = $('" + id + "'); "
                + "if (el['has_focus']) return; " + "el['has_focus'] = true; "
                + "if (el['event_num'] == undefined) el['event_num'] = 0; "
                + "var eventNum = ++el['event_num']; " + "if ($('" + id
                + "')['has_focus']) " + "catchEventId('" + id
                + "', 10, ''); };"));

        addClientUpdate(new FClientUpdate(Prio.VERYEND, checkExistenceInDOM
                + "$('" + id + "')['reportedBlur'] = function() { "
                + "var el = $('" + id + "'); "
                + "if (! el['has_focus']) return; "
                + "el['has_focus'] = false; "
                + "if (el['event_num'] == undefined) el['event_num'] = 0; "
                + "var eventNum = ++el['event_num']; "
                + "setTimeout(\"if (! $('" + id + "')['has_focus'] " + "&& $('"
                + id + "')['event_num'] == '\" + eventNum + \"') "
                + "catchEventId('" + id + "', 11, '');\", 600); };"));

    }

    @Override
    public void focus()
    {
        String id = mainInput.getComponentid();

        String checkExistenceInDOM =
                Config.getConfig().isAjaxDebug() ? "" : "if ($('" + id + "')) ";

        mainInput.addClientUpdate(new FClientUpdate(Prio.VERYEND,
                checkExistenceInDOM + "$('" + id + "').focus();"));
    }

}
