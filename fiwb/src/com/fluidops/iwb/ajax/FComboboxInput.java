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
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.util.Pair;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;

/**
 * Dropdown-based input field for fixed subject and predicate.
 * Supports both literals and URIs as input, where the choice
 * can be configured via query or input string.
 * 
 * @author msc
 */
public class FComboboxInput extends FSubjectPredicateBasedInput
{
    private static final Logger logger = Logger.getLogger(FComboboxInput.class.getName());

    private boolean displayCombobox = true;
    
    // input string for filling the choices field
    private String query;
    
    // values for auto-completion
    private List<Value> values;

    // text input for object
    protected FComboBox combobox=null;
                
    // the repository to retrieve values/labels
    protected Repository rep;
    
    /**
     * Constructor
     * 
     * @param id FComponent id
     * @param s subject init value (must not be null)
     * @param p predicate init value (must not be null)
     * @param o object init value (may be null)
     * @param deletable whether or not the input is deletable
     * @param name name of the input field
     * @param choices parameter to determine choices
     * @param rep the underlying repository
    */
    public FComboboxInput(String id,Resource s,URI p,Value o,
            boolean deletable,String query, List<Value> values,
            Repository rep)
    {
        super(id,s,p,o,deletable);
        this.query=query;
        this.values=values;
        this.rep=rep;
    }
        
    /**
    * Constructor
    * 
    * @param id FComponent id
    * @param stmt stmt used for initialization (may be 
    *          null or contain null components in predicate
    *          and object position)
    * @param name name of the input field
    * @param choices parameter to determine choices
    * @param deletable whether or not the input is deletable
    * @param rep the underlying repository
    */
    public FComboboxInput(String id,Statement stmt,
            boolean deletable,String query,List<Value> values,Repository rep)
    {
        super(id,stmt,deletable);
        this.query=query;
        this.values=values;
        this.rep=rep;
    }
        
    @Override
    public void initializeView() 
    {
        // init predicate input form
        combobox=buildCombobox(query,values,getInitObject());
        add(combobox);

        objLink = new FImageButton(Rand.getFluidUUID(), EndpointImpl.api().getRequestMapper().getContextPath() + "/ajax/icons/pagelink.png")
        {
            @Override
            public void onClick()
            {
                Value objVal = getCurrentObject();
                if (objVal!=null)
                {
                    RequestMapper rm = EndpointImpl.api().getRequestMapper();
                    String objLink = rm.getRequestStringFromValue(objVal);
                    addClientUpdate(new FClientUpdate("document.location='" + objLink + "'"));
                }
            }
        };
        objLink.setTooltip("Open the page of the resource");
        objLink.setHidden(getInitObject()==null); // show button only if object initialized
        add(objLink);
        
        super.initializeView();
    }
         

    @Override
    protected Value getCurrentObject()
    {
        if (deleted()  || combobox==null)
            return null;
                
        // if display of combobox is disabled (e.g. because
        // the preselected value is not contained in the
        // combobox, there cannot be any modifications)
        if (!displayCombobox)
            return getInitObject();
        
        ArrayList<Object> selected = combobox.getSelected();
        if (selected.size()==1)
        {
            try
            {
                Value ret = (Value)selected.get(0);
                return ret;
            }
            catch (ClassCastException e)
            {
                // probably uninitialized, will return null later
            }
        }
        
        return null;
    }

    private FComboBox buildCombobox(String query,List<Value> values,Value preselect)
    {
        FComboBox cb = new FComboBox("dd"+inputId){
        	
        	@Override
        	public void onChange() 
        	{
        		onSelect();
        		super.onChange();
        	}
        };
        cb.addChoice("","");

        ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);

        // add choices as specified in query, if defined
    	if (!StringUtil.isNullOrEmpty(this.query))
    	{
    		TupleQueryResult res = null;
	        try
	        {
	            res = dm.sparqlSelect(query.trim(),true, getInitSubject(), true);
	            List<String> names = res.getBindingNames();
	            if (names.size()==1)
	            {
	                String bindingName = names.get(0);
	                while (res.hasNext())
	                {
	                    BindingSet bs = res.next();
	                    Binding b = bs.getBinding(bindingName);
	                    Value v = b.getValue();
	                    if (v!=null)
	                        if (v instanceof Resource)
	                        {
	                        	String label = dm.getLabel((Resource)v);
	                        	if (label.length() > 50)
	                        		label=label.substring(0,49) + "...";
	                        	
	                            cb.addChoice(label,v);
	                        }
	                        else
	                            cb.addChoice(v.stringValue(),v);
	                }
	            }
	            else
	            {
	                logger.warn("Error: expecting exactly one binding in query.");
	                // leave list blank
	            }
	        }
	        catch (Exception e)
	        {
	            logger.warn("Invalid Query for FCombobox: " + e.getLocalizedMessage());
	        }
	        finally {
	        	ReadWriteDataManagerImpl.closeQuietly(res);
	        }
    	}

    	// add manually specified choices, if defined
    	if (this.values!=null && !this.values.isEmpty())
        {
            for (Value val : this.values)
            {
                if (val!=null)
                    if (val instanceof Resource)
                        cb.addChoice(dm.getLabelHTMLEncoded((Resource)val),val);
                    else
                        cb.addChoice(val.stringValue(),val);
            }
        }
        
        // compute preselection if required
        if (preselect!=null)
        {
            boolean match = false;
            ArrayList<Pair<String,Object>> allChoices = cb.getChoices();
            for (Pair<String,Object> choice : allChoices)
            {
                if (choice!=null && preselect.equals(choice.snd))
                {
                    cb.setPreSelected(preselect);
                    match = true;
                    break;
                }
            }
            
            // if the selected element is not contained in the list, 
            // we set the input readonly, so the label will be shown 
            if (!match)
                displayCombobox=false;
        }
        return cb;
    }

    @Override
    public String render()
    {
        Object objSelect = displayCombobox?objectFieldAnchor(combobox):objLabel.htmlAnchor();
        
        TemplateBuilder tb = new TemplateBuilder(FComboboxInput.class);
        String render = tb.renderTemplate(
                "displaySubjectField", displaySubject,      // currently not used in template
                "displayPredicateField", displayPredicate,  // currently not used in template
                "displayObjectField", displayObject,        // currently not used in template
                "subject", subjectFieldAnchor(null),
                "predicate", displayPredicate?predicateFieldAnchor(null):"", // always non-editable 
                "combobox", objSelect,
                "deleteButton", deleteButtonAnchor(),
                "objectLink", objLink.htmlAnchor());
        return render;
    }
    
    @Override
    public FStatementInput getInitClone(String id,
            boolean cloneInitSubj,boolean cloneInitPred, boolean cloneInitObj)
    { 
        Resource subject = null;
        if (cloneInitSubj)
            subject=getInitSubject();
        
        URI predicate = null;
        if (cloneInitPred)
            predicate=getInitPredicate();
        
        Value object = null;
        if (cloneInitObj)
            object=getInitObject();
        
        // TODO: actually it might be better to use "real" clones rather
        // than links to the existing objects, but actually I don't see
        // potential problems with using pointers here
        FComboboxInput clone = new FComboboxInput(id, subject, predicate,
                object, getDeleted()!=null, this.query, this.values, this.rep);
        clone.setDisplaySubject(getDisplaySubject());
        clone.setDisplayPredicate(getDisplayPredicate());
        clone.setDisplayObject(getDisplayObject());
        clone.setSubjectEditable(subjectEditable);
        clone.setPredicateEditable(predicateEditable);
        clone.setObjectEditable(true); // new input makes only sense with editable object 
        
        return clone;
    }
    
    /**
     * To be overridden in subclasses, executed onClick on one of the suggestions
     */
    protected void onSelect() {
		
	}
	
}

