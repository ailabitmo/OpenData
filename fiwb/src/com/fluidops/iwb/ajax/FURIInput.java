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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.XMLBuilder.Attribute;
import com.fluidops.ajax.XMLBuilder.Element;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.helper.FHelpers;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.autocompletion.AutocompleteMap;
import com.fluidops.iwb.cache.AutoSuggestionCache;
import com.fluidops.util.StringUtil;

/**
 * reusable extension of FTextInput2 being able to suggest ontology-derived
 * autocompletions
 * 
 * @deprecated Use {@link FValueTextInputBase} instead (set allowed content and
 *             suggestions accordingly to simulate behavior of this class)
 * @author tobimath
 */
@Deprecated
public class FURIInput extends FTextInput2 
{
    private static final Logger logger = Logger.getLogger(FURIInput.class.getName());
    
	URI initPredicate;
    boolean initialized = false;
    FComponent predInput;
    String query;
    Value queryContext;		// the value that is used for ?? in the query for generating suggestions
    
    @Override
    public Boolean validate()
    {
        if (StringUtil.containsNonIriRefCharacter(value,true))
            return false;

        // we allow full URIs of the form <URI>
        if (value.contains("<") || value.contains(">"))
            return value.startsWith("<") && value.endsWith(">") && value.length()>2;

        // TODO: this is probably not the best idea for some use cases; maybe we could
        // make it configurable whether to turn on or off
        /*
        URI u = EndpointImpl.api().getNamespaceService().guessURI(
                this.value);
        ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);
        boolean exists = dm.resourceExists(u);
        return !exists;
        */
        
        return true;
    }
    
    public FURIInput(String id, String objStr, URI initPredicate)
    {
        super(id,objStr, true);
        this.initPredicate=initPredicate;
    }
    
    public FURIInput(String id, String objStr, FComponent predInput)
    {
    	this(id, objStr, predInput, null, null);
    }
    
    /**
     * 
     * @param id
     * @param objStr
     * @param predInput
     * @param query
     * @param queryContext the value that is used for ?? in the query for generating suggestions
     */
    public FURIInput(String id, String objStr, FComponent predInput, String query, Value queryContext)
    {
        super(id,objStr, true);
        this.predInput = predInput;
        this.query = query;
        this.queryContext = queryContext;
    }
    
   @Override
    public void handleClientSideEvent(FEvent event)
    {
        switch ( event.getType() )
        {
	      //the list was clicked
	    	case MOUSE_LEFT:
	    	{
	    		this.value = event.getArgument();
	    		// populateView() triggers no input changed event ( $('dropdown.id').value = "..." )
	    		populateView();
	    		populateValidity();
	    		break;
	    	}
	        case INPUT_CHANGED: //set the value and populate
	        {   
	        	initialized = true;
	            this.value = event.getArgument();
	            if(enablesuggestions){
	                currentSuggestions = getSuggestions( value );
	                //populate suggestion list
	                if(suggList!=null)
	                {
	                    suggList.setHidden(false);
	                    suggList.populateView();
	                    
	                    // set suggestion list to visible (no waiting in javascript)
	                   addClientUpdate(new FClientUpdate(Prio.VERYEND,"setDropdownVisibilityId('"+suggList.getId()+"',true)")); 
	                }
	            }
	            populateValidity();
	            onChange();
	            break;
	        }
            case BLUR: //set the value for the dropdown, UP/DOWN executes this
            {
                Integer idx = Integer.parseInt(event.getArgument());
                if(enablesuggestions)
                {
                    // If user types smth that results in no suggestions, currentSuggestions will be empty
                    if (currentSuggestions.isEmpty())
                        break;
                    
                    URI uri = AutocompleteMap.getUriForName(_choices[currentSuggestions.get(idx)]);
                    String valString = EndpointImpl.api().getRequestMapper().getReconvertableUri(uri,false);
                    setValueAndRefresh(valString);
                }
                populateValidity();
                onChange();
                break;
            }
            case KEY_ENTER:
            {
            	this.value = event.getArgument();
            	populateValidity();
            	onEnter();
            	break;
            }
            //post event was sent, currently used for passwords
            case POST_EVENT:
            {
                this.value = event.getPostParameter(POST_PARAM_NAME);
                populateValidity();
                onChange();
                break;
            }
            default:
                super.handleClientSideEvent(event);
        }
    }
    
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
			this.enablesuggestions = _choices != null && getAndSetChoices().length >0;
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
                suggList = new FSuggestionList( "dropdown" ) 
                {
                    @Override
                    public String render() 
                    {
                    	if (initialized) 
                    	{
                            String[] choices = getAndSetChoices();
                            FComponent[] display = getDisplay();
                            
                            // store max suggestions in the dom
                            String maxValuesStr = "maxSuggestionsDisplayed = "+getMaxSuggestionsDisplayed();
                            
                            StringBuffer sb = new StringBuffer();
                            sb.append(isOrdered() ? "<ol "+maxValuesStr+">" : "<ul "+maxValuesStr+">");
                            if ( currentSuggestions!=null )
                                for (int idx : currentSuggestions)
                                {
                                    String displayString = choices[idx];
                                    URI uri = AutocompleteMap.getUriForName(choices[idx]);
                                    String s = null;
                                    if (uri != null)
                                         s = EndpointImpl.api().getRequestMapper().getReconvertableUri(uri,false);
                                    
                                    if (s == null) 
                                    {
                                    	 URI displayuri = AutocompleteMap.getUriForName(choices[idx]);
                                         s = (displayuri != null) ? displayuri.stringValue() : null;
                                    }
                                    
                                    if (s != null)
                                        sb.append("<li onclick=\"catchEventId('"+FURIInput.this.getId()+"',1,'"+s+"');$('"+FURIInput.this.getComponentid()+"').value='"+s+"';\" onmousedown=\"dropdownLiSelected();\">");
                                    if (display != null) 
                                    {
                                        sb.append(display[idx].render());
                                        sb.append("</li>");
                                    }
                                    else 
                                    {
                                        if(!displayString.equals("")) 
                                        {
                                            if (this.value != null && !this.value.isEmpty())
                                                displayString = bevel( displayString, this.value );
                                            sb.append(displayString);
                                            sb.append("</li>");
                                        }
                                    }
                                    
                                    idx++;
                                }
                            sb.append(isOrdered() ? "</ol>" : "</ul>");
                            return sb.toString();
                        }
                    	else 
                    	{
                    		return "";
                    	}
                    }  
                };
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
     	if(enablesuggestions)
     	{
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
     * Note: to avoid duplicate computation in the case of parallel
     * events, we make the method synchronize; in that case, the method
     * will be called twice in sequence, where the second call is efficient
     * in that it falls back in the AutoSuggestionCache.
     */
    @Override
    public synchronized String[] getChoices() 
    {
    	ReadDataManager dm = EndpointImpl.api().getDataManager();
    	if (!StringUtil.isNullOrEmpty(this.query))
    	{
    		try
            {
                TupleQueryResult res = dm.sparqlSelect(query.trim(),true, queryContext, true);
                List<String> names = res.getBindingNames();
                    
                List<String> suggestions = new ArrayList<String>();
                    
                if (names.size()==1)
                {
                    String bindingName = names.get(0);
                    while (res.hasNext())
                    {
                        BindingSet bs = res.next();
                        Binding b = bs.getBinding(bindingName);
                        Value v = b.getValue();
                        if (v!=null)
                            if (v instanceof URI)
                            {
                            	String label = dm.getLabel((Resource)v);
                            	if (label.length() > 50)
                                	label=label.substring(0,49) + "...";
                                	
                                String listString = "<i title='"+v.stringValue()+"'>" + StringEscapeUtils.escapeHtml(label) + "</i> ";    
                                listString +=  "("+ v.stringValue() + ")";
                                suggestions.add(listString);
                                AutocompleteMap.setNameToUriMapping(listString, (URI)v);
                            }
                                
                    }
                }
                else
                {
                    logger.warn("Error: expecting exactly one binding in query.");
                    // leave list blank
                }
                    
                String[] suggs = new String[suggestions.size()];
                int i = 0;
                for (String s : suggestions)
                	suggs[i++] = s;

                return suggs;
            }
            catch (Exception e)
            {
                logger.warn("Invalid Query for FURIInput: " + e.getLocalizedMessage());
                return new String[0];
            }
    	}
    	
    	String predStr = null;
    	if (predInput != null)
    		predStr = (String)predInput.returnValues();
    	if (predStr == null && initPredicate != null) 
    	{
    		predStr = EndpointImpl.api().getNamespaceService()
            .getAbbreviatedURI(initPredicate);  
    	}
    	
        // lookup or create URI
        URI predicate = AutocompleteMap.getUriForName(predStr);
        if (predicate == null) 
        	predicate = EndpointImpl.api().getNamespaceService().guessURI(predStr);
        
        if (predicate != null) 
        {
            // try cache lookup
            AutoSuggestionCache asc = AutoSuggestionCache.getInstance();
            String[] autoCompleteMap = null;
            
            List<String> cacheEntry = asc.lookup(Global.repository,predicate);
            if (cacheEntry != null)
            	autoCompleteMap = cacheEntry.toArray(new String[0]);
            if (autoCompleteMap!=null)
                return autoCompleteMap;

            // if not cached, compute and add to cache
            ReadDataManager dmSuggest = null;
            HashSet<String> list = new HashSet<String>();

            dmSuggest = ReadDataManagerImpl.getDataManager(Global.repository);
            List<URI> range = dmSuggest.getPropertyInfo(predicate).getRan();

            for (int i=0; i<range.size(); i++)
            {
                List<Statement> suggestions = dmSuggest.getStatementsAsList(null, RDF.TYPE, range.get(i), false);
                NamespaceService ns = EndpointImpl.api().getNamespaceService();
                for (int j=0; j<suggestions.size(); j++)
                {
                    Statement stmt = suggestions.get(j);
                    
                    if (!(stmt.getSubject() instanceof URI))
                    	continue;
                    
                    String localName = ns.getAbbreviatedURI((URI)stmt.getSubject()); 
                    String label = dmSuggest.getLabelHTMLEncoded((URI)stmt.getSubject());
                    String uriString = (localName != null)? localName : EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)stmt.getSubject(),true);
                    String listString = "<i title='"+uriString+"'>" + label + "</i> " + "("+ uriString + ")";
                    list.add(listString);
                    AutocompleteMap.setNameToUriMapping(listString, (URI)stmt.getSubject());
                }
            }

            ArrayList<String> array = new ArrayList<String>(list);
            asc.insert(Global.repository, predicate, array);
            return array.toArray(new String[0]);
        } 
        return null; 
    }
}
