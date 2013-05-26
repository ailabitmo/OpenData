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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.XMLBuilder.Attribute;
import com.fluidops.ajax.XMLBuilder.Element;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FSimpleList;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.helper.FHelpers;
import com.fluidops.config.Config;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.autocompletion.AutoCompletionUtil;
import com.fluidops.iwb.autocompletion.AutoSuggester;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * This class realises the text input with autosuggestion, which contains an RDF value, rather than plain text. 
 * In case if the values are literals, there are no substantial behavioural changes from FTextInput2. If the values are URIs, 
 * the autocompletion menu allows the user to type in either the URI itself or its label and reacts on both. 
 * By default, instance label is displayed instead of the URI itself.  
 * 
 * NB: currently contains overlapping code with FTextInput2. Eventually, the target is to merge the code, while FTextInput2 is going to be partially modified 
 * (e.g., to use the AutocompletionOption interface for representing the options instead of plain strings).
 * 
 * @author andriy.nikolov
 *
 */
public abstract class FValueHoldingTextInput extends FTextInput2 {

	public AutoSuggester suggester;
	protected boolean activated = false;
	
	private HashMap<String, OptionStruct> optionsMap = new HashMap<String, OptionStruct>();
	 
	private HashMap<String, OptionStruct> optionsMapByDisplayName = new HashMap<String, OptionStruct>();
	
	private HashSet<String> duplicateDisplayNames = new HashSet<String>();
	
	private List<OptionStruct> options;
	
	private OptionStruct chosenOption;
	
	// Now the options are loaded every time when INPUT_CHANGED is fired. 
	// The first time options are loaded, we need to check 
	// if the currently set value appears in the autosuggestion list.
	// If yes, we display its displayName, otherwise, the value itself.
	private boolean optionsLoaded = false;
	
	private boolean previousInputInterpretedAsAURILabel = false;
	
	/**
	 * Generic interface for autocompletion options. Each option should contain the value itself, its id, display name, and component to be shown in the menu.
	 * Eventually should be moved to FTextInput2
	 * @author andriy.nikolov
	 *
	 * @param <T> Actual type of the contained value: e.g., String for plain text or Value for RDF values
	 */
    public static interface AutocompletionOption<T> {
    	
    	public String getDisplayName();
    	
    	public FComponent getDisplay(String input);
    	
    	/**
    	 * Actual object held by the input, e.g., RDF value (if the input is a URI entity), 
    	 * a string (if the input selects among several pre-defined options), etc.
    	 * @return
    	 */
    	public T getValue();
    	
    	/**
    	 * Unique key of the value (can be the string form of the value itself).
    	 * This key is returned as the event argument when the user clicks on an autocompletion option
    	 * in the drop-down list. 
    	 * @return
    	 */
    	public String getValueId();

    	/**
    	 * Tests whether the option matches the user input in according to a given comparisonType
    	 * @param in
    	 * @param comparisonType
    	 * @return
    	 */
		public boolean matchesInput(String in, ComparisonType comparisonType);
    	
    }
	
    
    /**
     * Implementation of the AutocompletionOption for RDF values.
     * @author andriy.nikolov
     *
     */
	private class OptionStruct implements AutocompletionOption<Value> {
		
		private Value value = null;
		private FComponent display = null;
		private String displayLastGeneratedForInput = "";
		private String displayName = null;
		private String displayUri = null;
		
		public OptionStruct(Value value) {
			this.value = value;
			if(value instanceof URI) {
				this.displayUri = AutoCompletionUtil.toDisplayValue(value, false);
				optionsMapByDisplayName.put(displayUri, this);
			}
		}
		
		@Override
		public String getDisplayName() {
			if(displayName!=null) {
				return displayName;
			}
			displayName = EndpointImpl.api().getDataManager().getLabel(value);
			
			if(!duplicateDisplayNames.contains(displayName)) {

				if(optionsMapByDisplayName.put(displayName, this)!=null) {
					optionsMapByDisplayName.remove(displayName);
					duplicateDisplayNames.add(displayName);
				}
			}
			
			return displayName;
		}
		
		@Override
		public FComponent getDisplay(String input) {
			if(display!=null && input!=null && input.equals(displayLastGeneratedForInput)) {
				return display;
			}
			
			ReadDataManagerImpl dm = EndpointImpl.api().getDataManager();

			String displayString = dm.getLabelHTMLEncoded(value);
			String sDisplayUri = (displayUri!=null) ? FHelpers.HTMLify(displayUri) : null;
			
			// The following procedure handles the case where the display URI is given as <fullURI>. In this case, the bevel function should only check 
			// the URI itself. Otherwise, e.g., typing "t" would cause it to highlight the "t" character in "&lt;" and "&gt;".
			// So, we need to remove the brackets before calling bevel.			
			if(value instanceof URI && !displayString.equals(sDisplayUri)) {
				String bevelDisplayUri;
				if(sDisplayUri.startsWith("&lt;")&&sDisplayUri.endsWith("&gt;")) {
					bevelDisplayUri = "&lt;"+bevel(sDisplayUri.substring(4, sDisplayUri.length()-4), input)+"&gt;";
				} else {
					bevelDisplayUri = bevel(displayUri, input);
				}
				display = new FHTML(Rand.getIncrementalFluidUUID(), "<i>" + bevel(displayString, input) + "</i> (" + bevelDisplayUri + ")");
			} else {
				display = new FHTML(Rand.getIncrementalFluidUUID(), bevel(displayString, input));
			}
			
			displayLastGeneratedForInput = input;
			
			return display;
			
		}
		
		@Override
		public boolean matchesInput(String in, ComparisonType comparisonType) {
			boolean ok = false;
			
			switch (comparisonType) {
			case StartsWith:
				ok = ( getDisplayName().toLowerCase().startsWith(in.toLowerCase())
						|| (value instanceof URI 
								&& displayUri.toLowerCase().startsWith(in.toLowerCase())) );
				break;
			case Contains:
				ok = ( getDisplayName().toLowerCase().contains(in.toLowerCase())
						|| (value instanceof URI 
								&& displayUri.toLowerCase().contains(in.toLowerCase())) );
				break;
			case EndsWith:
				ok = ( getDisplayName().toLowerCase().endsWith(in.toLowerCase())
						|| (value instanceof URI 
								&& displayUri.toLowerCase().endsWith(in.toLowerCase())) );
				break;
			default:
				ok = false;
			}
			
			return ok;
		}
		
		public boolean matchesExactly(String in) {
			return value.stringValue().equals(in)
					||getDisplayName().equals(in)
					||(value instanceof URI
							&& displayUri.equals(in));
		}

		@Override
		public Value getValue() {
			return value;
		}

		@Override
		public String getValueId() {
			return value.stringValue();
		}

		/**
		 * @return the displayUri (only for URIs, otherwise null)
		 */
		private String getDisplayUri() {
			return displayUri;
		}
		
	}
	
	/**
	 * TODO: Need refactoring - copy-paste of an inner class just to change a few lines 
	 * (split the "held" value and the displayed name)
	 * 
	 * @author andriy.nikolov
	 *
	 */
	protected class FValueSuggestionList extends FSimpleList {
		
		public FValueSuggestionList(String id)
        {
            super(id);
        }

        @Override
        public String render()
        {
        	
            // store max suggestions in the dom
            String maxValuesStr = "maxSuggestionsDisplayed = "+getMaxSuggestionsDisplayed();
            
            StringBuffer sb = new StringBuffer();
            sb.append(isOrdered() ? "<ol "+maxValuesStr+">" : "<ul "+maxValuesStr+">");
            if ( currentSuggestions!=null )
            	for (int idx : currentSuggestions)
                {
                    String s = options.get(idx).value.stringValue();
                    String displayName = options.get(idx).getDisplayName();
                    FComponent display = options.get(idx).getDisplay(FValueHoldingTextInput.this.value);

                    // if a suggestion is chosen, left mouse event gets executed
                    sb.append("<li onmousedown=\"dropdownLiSelected();$('"+FValueHoldingTextInput.this.getComponentid()+"').value='"+FHelpers.escapeJavaScriptAndHtml(displayName)+"'; $('"+FValueHoldingTextInput.this.getId()+".input').focus(); catchEventId('"+FValueHoldingTextInput.this.getId()+"',1,'"+s+"');\">");
                    if (display != null)
                        sb.append(display.render());
                    else{
                        if(!FValueHoldingTextInput.this.value.isEmpty())
                            s = bevel( displayName, FValueHoldingTextInput.this.value );
                        sb.append(s);
                    }
                    sb.append("</li>");
                    idx++;
                    
                }
            
            sb.append(isOrdered() ? "</ol>" : "</ul>");
            return sb.toString();
        }        
		
	}
	
	
	/**
	 * @param id
	 */
	public FValueHoldingTextInput(String id) {
		this(id, "");
	}

	/**
	 * @param id
	 * @param value
	 */
	public FValueHoldingTextInput(String id, String value) {
		this(id, value, "");
	}

	/**
	 * @param id
	 * @param value
	 * @param label
	 */
	public FValueHoldingTextInput(String id, String value, String label) {
		this(id, value, label, true);
	}

	/**
	 * @param id
	 * @param value
	 * @param label
	 * @param enablesuggestion
	 */
	public FValueHoldingTextInput(String id, String value, String label,
			boolean enablesuggestion) {
		super(id, value, label, enablesuggestion);
	}

	/**
	 * @param id
	 * @param enablesuggestion
	 */
	public FValueHoldingTextInput(String id, boolean enablesuggestion) {
		this(id, "", "", enablesuggestion);
	}

	/**
	 * @param id
	 * @param value
	 * @param enablesuggestion
	 */
	public FValueHoldingTextInput(String id, String value,
			boolean enablesuggestion) {
		super(id, value, enablesuggestion);
	}
	
	@Override
	public void handleClientSideEvent(FEvent event) {
		switch (event.getType()) {
		case MOUSE_LEFT: {
			String sValue = event.getArgument();
			// if value changes, currentSuggestions have to be recalculated.
			// necessary for use cases
			// where suggestions change dynamically (lucene-based suggestions)
			chosenOption = optionsMap.get(sValue);
			value = chosenOption.getDisplayName();

			if (enablesuggestions)
				currentSuggestions = getSuggestions(this.value);

			// populateView() triggers no input changed event (
			// $('dropdown.id').value = "..." )
			populateView();
			populateValidity();
			populateInterpretation();
			break;
		}
		case INPUT_CHANGED: {
			activated = true;
            super.handleClientSideEvent(event);
            
            if(!this.value.equals(event.getArgument())) {
            	populateView();
            }
            populateInterpretation();            
            break;
		}
		case BLUR: // set the value for the dropdown, UP/DOWN executes this
		{
			Integer idx = Integer.parseInt(event.getArgument());

			if (enablesuggestions) {
				// If user types smth that results in no suggestions,
				// currentSuggestions will be empty
				if (currentSuggestions.isEmpty())
					break;

				chosenOption = options.get(currentSuggestions.get(idx));
				setValueAndRefresh(chosenOption.getDisplayName());
			}
			populateValidity();
			onChange();
			populateInterpretation();
			break;
		}
		default: {
			super.handleClientSideEvent(event);
		}

		}

	}
	
	/**
	 * Whenever the input changes, checks whether it matches some option from the autosuggestion list and, 
	 * if yes, sets the option as the chosen one. 
	 */
	@Override
	public void onChange() {
		if(StringUtil.isNullOrEmpty(this.value)) {
			this.chosenOption = null;
		} else {
			if(chosenOption==null 
					|| !chosenOption.matchesExactly(this.value) ) { 
					
				this.chosenOption = optionsMapByDisplayName.get(this.value);
			}
		}
	}

	protected List<OptionStruct> getAndSetOptions() {
			if (!activated) {
				return this.options;
			}
			
			List<Value> suggestions = suggester.suggest(value);
			
			options = Lists.newArrayList();
			optionsMap.clear();
			optionsMapByDisplayName.clear();
			
			OptionStruct option;
			
			for (Value suggestion : suggestions) {

				option = new OptionStruct(
						suggestion);
				options.add(option);
				optionsMap.put(suggestion.stringValue(), option);
				optionsMapByDisplayName.put(suggestion.stringValue(), option);				
			}
			
			// Now the options are loaded every time when INPUT_CHANGED is fired. 
			// The first time options are loaded, we need to check 
			// if the current value appears in the autosuggestion list.
			// If yes, we display its displayName, otherwise, the value itself.
			// TODO: This has to be done only once, so probably the onFocus event handler would suit better.
			// Need to check why the FOCUS event never gets fired for the component.
			if(!optionsLoaded && this.chosenOption!=null ) {
				if(optionsMap.containsKey(this.chosenOption.getValueId())) {
					this.chosenOption = optionsMap.get(this.chosenOption.getValueId());
					this.value = this.chosenOption.getDisplayName();
				}
			}
			
			optionsLoaded = true;
				
			return this.options;
	}
	 	 
	 /**
	  * TODO: Mainly the copypaste from FTextInput2, just to change a couple of lines 
	  * - should be merged when the FTextInput2 gets updated. 
	  */
	 @Override
	    public String render()
	    {
			String compid = getComponentid();
			Attribute onkeydown = null;
			Attribute onkeyup = null;
			Attribute autocomplete = null;
			Attribute onfocus = null;
			//set the dropdown for suggestions automatically
			
			// if enablesuggestions is not true, but there are choices, set enablesuggestions to true
			if(!enablesuggestions){
				this.enablesuggestions = options != null && !getAndSetOptions().isEmpty();
			}
			//this.enablesuggestions = true;
			String checkDropDown = "";
			if(enableTypingEvents)
			{
				if(getType()==ElementType.PASSWORD)
					onkeyup = new Attribute( "onkeyup", "catchPostEventIdEncode('"+getId()+"',9,this.value,'"+POST_PARAM_NAME+"');");
				else
					onkeyup = new Attribute( "onkeyup", "catchEventId('"+getId()+"',5,this.value);" );
			}
			
			if(enablesuggestions)
			{
				//this lazy registration is possible as dropdown id never changes
				if(suggList==null)
				{
					suggList = new FValueSuggestionList( "dropdown" );
					add(suggList);
				}
				setOnblur("textInputOnBlur('"+getId()+"');");
				onkeydown = new Attribute("onkeydown","keypressAutocompleteHandler1(event,'"+getId()+"');");
				onkeyup = new Attribute( "onkeyup", "keyreleaseAutocompleteHandler1(event,'"+getId()+"');" );
				autocomplete = new Attribute( "autocomplete", "off" );
				onfocus = new Attribute("onfocus","textInputOnFocus('"+getId()+"');");
				checkDropDown = "if(!isDropDownVisible('"+getId()+"'))";
			}
			
			//build request string, for pw post, other get
	        String getreqstr = checkDropDown+"catchSuggestionList('"+getId()+"',5,this.value);";
	        if(getType()==ElementType.PASSWORD)
	            getreqstr = checkDropDown+"catchPostEventIdEncode('"+getId()+"',9,this.value,'"+POST_PARAM_NAME+"');";

	        Attribute onchange = new Attribute( "onChange", getreqstr );
	        String commonClass = "";
	        Boolean valid = validate();
	        if(valid !=null)
	            commonClass+=valid?"valid":"invalid";
	        if(!getInnerClazz().isEmpty())
	            commonClass+=" "+getInnerClazz();
	        
	        String valueField = "";
	        if(!StringUtil.isNullOrEmpty(this.value))
	        {
	        	if(renderValueField)
	        		valueField = FHelpers.HTMLify(this.value);
	        	else
	        		valueField = "**********";
	        }
	        
			Element input = new Element( "input", 
					//If renderValueField is true, display the value
					//Else: Check if the value is empty. If it is, display an empty string. Otherwise, display eight stars
	                new Attribute("value", valueField),
	                new Attribute( "size", ""+getSize() ), 
	                new Attribute( "type", getType().name().toLowerCase() ), 
	                new Attribute( "id", compid),
	                commonClass.isEmpty()?null:new Attribute( "class", commonClass.trim()),
	                getMaxlength()==0 ? null : new Attribute( "maxlength", String.valueOf(getMaxlength())),
	                onkeyup,
	                onkeydown,
	                onfocus,
	                autocomplete,
	                (enabled ? null : new Attribute( "disabled", "disabled" ) ),
	                (getName().isEmpty() ? null : new Attribute( "name", getName() ) ),
	                (getOnblur().isEmpty() ? null : new Attribute( "onblur", getOnblur() ) ),
	                onchange,
	                (getComponentStyle().size() != 0? getComponentStyleAsAttribute() : null));
	     	String res = input.toString();
	     	
	     	//suggestion div
	     	if(enablesuggestions){
	         	Element div = new Element( "div",
	         			new Attribute( "id", suggList.getId() ),
	         			new Attribute( "class", "flSuggestionDropDown" ),
	         			new Attribute( "onmousedown", "suggListOnClick()"), // to keep focus on input while scrolling in dropdown list
	         			new Attribute( "style", "visibility:hidden; background-color: transparent;" )).addChild(new Element( "ul" ));
	     		res+="<br/>"+div.toString();
	     	}

	        res = label + res;
	     	return res;
	    }
	 
	
	/**
	 * 
	 * @param in
	 * @return
	 */
	@Override
	public List<Integer> getSuggestions(String in) {

		List<OptionStruct> options = getAndSetOptions();
		List<Integer> res = new ArrayList<Integer>();
		
		if (in == null /* || in.length()==0 */|| options == null
				|| options.isEmpty())
			return res;

		for (int i = 0; i < options.size(); i++)
			if (options.get(i) != null) {
				
				if (options.get(i).matchesInput(in, comparisonType)) {
					res.add(i);
					if (res.size() >= getMaxSuggestions())
						break;
				}
			}

		return res;
	}
	
	/**
	 * @return the chosenOption
	 */
	public Value getChosenRDFValue() {
		return (chosenOption!=null) ? chosenOption.value : null;
	}
	
	public void setRDFValue(Value value) {
		if(value instanceof URI) {
			if(optionsMap.containsKey(value.stringValue())) {
				this.chosenOption = optionsMap.get(value.stringValue());
			} else {
				this.chosenOption = new OptionStruct(value);
			}
		}
	}

	
	/**
	 * Returns an RDF value for the current input. First, checks whether the value has been chosen from the autosuggestion list and, if not, tries to guess it according to the current type.
	 * 
	 * @param currentType
	 * @param languageTag
	 * @return
	 */
	public Value getOrCreateRDFValue(URI currentType, String languageTag) {

		Value val = this.getChosenRDFValue();

		if(valueSatisfiesDatatype(val, currentType))			
			return val;

		ValueFactoryImpl vf = ValueFactoryImpl.getInstance();

		// produce RDF value from original value, input and current type
		if (currentType.equals(RDFS.RESOURCE))
			return EndpointImpl.api().getNamespaceService()
					.guessURI(this.value);
		else if (currentType.equals(RDFS.LITERAL)) {
			// the generic literal
			if (languageTag!=null) {// need to
									// copy over
									// language
									// tag
				return vf.createLiteral(this.value,
						languageTag);
			} else {
				// no language tag to copy
				return vf.createLiteral(this.value);
			}

		} else {
			// explicit XML type; can have no language tag (cf. Sesame
			// Implementation of Literal)
			return vf.createLiteral(this.value, currentType);
		}
	}
	
	/**
	 * 
	 * @return true if the current input represents a display name of a value rather than the value itself.
	 */
	private boolean isCurrentInputAURILabel() {
		if(this.chosenOption!=null 
				&& chosenOption.getValue() instanceof URI
				&& this.chosenOption.getDisplayName().equals(this.value) 
				&& !this.chosenOption.getDisplayUri().equals(this.value)
				&& !this.chosenOption.getValueId().equals(this.value)) {
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Sends a client update indicating whether the input is interpreted as a label or as the value itself. 
	 * For labels, sets the text style to italic. Probably, should be merged with populateValidity()
	 */
	private void populateInterpretation() {
		boolean isURILabel = isCurrentInputAURILabel();

		if(isURILabel!=previousInputInterpretedAsAURILabel) {
			// check the element's existance before trying to change the className
			String checkExistanceInDOM = "if ($('" + getComponentid() + "')) ";
			if (Config.getConfig().isAjaxDebug())
				// if ajax debug is enabled --> create output if action fails
				checkExistanceInDOM = "";
			String style = isURILabel ? "italic" : "normal";
			addComponentStyle("font-style", style);
			
			addClientUpdate(new FClientUpdate(Prio.VERYEND, checkExistanceInDOM
					+ "$('" + getComponentid() + "').style.fontStyle='" + style + "';"));
		}
		
		previousInputInterpretedAsAURILabel = isURILabel;
	}
	
	private boolean valueSatisfiesDatatype(Value val, URI datatype) {
		if(val==null) return false;
		
		if(val instanceof URI) {
			// If the value is a URI,then it only satisfies rdfs:Resource.
			if(datatype.equals(RDFS.RESOURCE))
				return true;
		} else if(val instanceof Literal) {
			// If the value is an untyped literal, then it only satisfies rdfs:Literal,
			// otherwise the value datatype must be equal to the input datatype.
			Literal lit = (Literal)val;
			if(lit.getDatatype()==null) {
				if(datatype.equals(RDFS.LITERAL))
					return true;
			} else {
				if(lit.getDatatype().equals(datatype))
					return true;
			}
		}
		
		return false;
	}

}