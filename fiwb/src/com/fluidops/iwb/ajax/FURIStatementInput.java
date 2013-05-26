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

import java.util.HashSet;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.FTextInput2.ComparisonType;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.autocompletion.AutocompleteMap;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;

/**
 * Input form for URIs with fixed subject and possibly fixed predicate.
 * Supports auto-suggestion (where available).
 * 
 * @author msc
 */
public class FURIStatementInput extends FSubjectBasedInput
{
    // text input for predicate
    protected FTextInput2 predInput=null;

    // text input for object
    protected FTextInput2 objInput=null;

    // whether a pagelink icon should be rendered
    protected boolean pagelink;

    // whether a label should be added to the field
    protected boolean labeled;
    
    // the query used for driving auto-suggestion (may be null)
    protected String query;
    
    /**
     * Constructor
     * 
     * @param id FComponent id
     * @param s subject init value (must not be null)
     * @param p predicate init value (may be null)
     * @param o object init value (may be null)
     * @param deletable whether or not the input is deletable
     * @param pagelink whether to add a pagelink icon
     * @param labeled whether to add a label
     */
    public FURIStatementInput(String id,Resource s,URI p,URI o,
            boolean deletable,boolean pagelink,boolean labeled)
    {
    	this(id,s,p,o,deletable,pagelink,labeled,null);
    }
    
    public FURIStatementInput(String id,Resource s,URI p,URI o,boolean deletable, 
    		boolean pagelink, boolean labeled, String query)
    {
        super(id,s,p,o,deletable);
        predicateEditable=false; // default, may be changed
        this.pagelink = pagelink;
        setDisplayPredicate(labeled);
        this.query = query;
    }
    
    /**
     * Constructor
     * 
     * @param id FComponent id
     * @param stmt stmt used for initialization (may be 
     *          null or contain null components in predicate
     *          and object position)
     * @param deletable whether or not the input is deletable
     */
    public FURIStatementInput(String id,Statement stmt,boolean deletable)
    {
    	this(id, stmt, deletable, null);
    }
    
    public FURIStatementInput(String id,Statement stmt,boolean deletable, String query)
    {
        super(id,stmt,deletable);
        predicateEditable=false; // default, may be changed
        pagelink = true;
        labeled = true;
        this.query = query;
    }
    
    @Override
    public void initializeView() 
    {
        RequestMapper rm = EndpointImpl.api().getRequestMapper();
        
        // init predicate input form
        predInput=new FTextInput2("pi" + inputId);
        if (getInitPredicate()!=null)
            predInput.setValue(rm.getReconvertableUri(getInitPredicate(),false));
        if (predicateEditable)
            setPredicateAutoCompletion(predInput);
        add(predInput);
        
        // init predicate input form
        String objValue = "";
        if (getInitObject()!=null && getInitObject() instanceof URI)
            objValue = rm.getReconvertableUri((URI)getInitObject(),false);
        
        if (predInput != null) 
        	 objInput=new FURIInput("oi" + inputId, objValue, predInput, query, getInitSubject());
        else if (getInitPredicate() != null)
        	objInput=new FURIInput("oi" + inputId, objValue, getInitPredicate());

        
        objInput.appendClazz("floatRightIwb");
        objInput.setSize(50);
        objInput.enablesuggestions = true;
        objInput.enablevalidation = false;
        objInput.setComparisonType(ComparisonType.Contains);
        
        add(objInput);
        
        
        if (pagelink)
        {
            objLink = new FImageButton(Rand.getFluidUUID(),EndpointImpl.api().getRequestMapper().getContextPath() + "/ajax/icons/pagelink.png")
            {
        	
                @Override
                public void onClick()
                {
                    Value objVal;
                    try
                    {
                        objVal = getCurrentObject();
                        
                        RequestMapper rm = EndpointImpl.api().getRequestMapper();
                        String objLink = rm.getRequestStringFromValue(objVal);
                        addClientUpdate(new FClientUpdate("document.location='" + objLink + "'"));
                    }
                    catch (InvalidUserInputException e)
                    {
                        // do nothing...
                    }
                }
            };
            objLink.setTooltip("Open the page of the resource");
            objLink.setHidden(getInitObject()==null); // show button only if object initialized

            add(objLink);
        }
        
        super.initializeView();
    }
    
    @Override
    protected URI getCurrentPredicate() throws InvalidUserInputException
    {
        if (deleted() || predInput==null)
            return null;

        String predStr = predInput.getInput();
        if (!StringUtil.isNullOrEmpty(predStr))
        {
            URI ret = EndpointImpl.api().getNamespaceService().guessURI(predStr);
            if (ret != null)
                return ret;
            else
                throw new InvalidUserInputException(predStr + " is no valid URI");
        }
        
        return null; // not defined
    }
    

    @Override
    protected Value getCurrentObject() throws InvalidUserInputException
    {
        if (deleted()  || objInput==null)
            return null;
        
        String objStr = objInput.getInput();
        if (!StringUtil.isNullOrEmpty(objStr))
        {
            URI ret = EndpointImpl.api().getNamespaceService().guessURI(objStr);
            if (ret != null)
                return ret;
            else
                throw new InvalidUserInputException(objStr + " is no valid URI");
        }
        
        return null; // not defined
    }

    @Override
    public String render()
    {
        String linkString = "";
        if (objLink != null)
            linkString = objLink.htmlAnchor().toString();
        
        TemplateBuilder tb = new TemplateBuilder(FURIStatementInput.class);
        String render = tb.renderTemplate(
                "displaySubjectField", displaySubject,      // currently not used in template
                "displayPredicateField", displayPredicate,  // currently not used in template
                "displayObjectField", displayObject,        // currently not used in template
                "subject", subjectFieldAnchor(null),
                "predicate", predicateFieldAnchor(predInput), 
                "object", objectFieldAnchor(objInput),
                "deleteButton", deleteButtonAnchor(),
                "objectLink", linkString);
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
        
        URI object = null;
        if (cloneInitObj && (getInitObject() instanceof URI))
            object=(URI)getInitObject();
        
        // note: maybe it might be better to use "real" clones rather
        // than links to the existing objects, but I don't see
        // potential problems with using pointers at the moment,
        // the input fields in the form get cloned anyway (msc)
        FURIStatementInput clone = new FURIStatementInput(id,subject,predicate,object,
                getDeleted()!=null,true,true, query);
        clone.setDisplaySubject(getDisplaySubject());
        clone.setDisplayPredicate(getDisplayPredicate());
        clone.setDisplayObject(getDisplayObject());
        clone.setSubjectEditable(subjectEditable);
        clone.setPredicateEditable(predicateEditable);
        clone.setObjectEditable(true); // new input makes only sense with editable object 
        
        return clone;
    }
    
    public void setPredicateAutoCompletion(FTextInput2 inp)
    {
        // enable auto suggestion, if it makes sense
        String[] choices = null;
        
        HashSet<String> list = new HashSet<String>();

        ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
        
        List<Statement> res = dm.getStatementsAsList(null, RDF.TYPE, OWL.OBJECTPROPERTY, false);
        for (int i=0;i<res.size();i++)
        {
            Statement stmt = res.get(i);
            if (!(stmt.getSubject() instanceof URI))
            	continue;
            URI subjectValue = (URI)stmt.getSubject();
            
            NamespaceService ns = EndpointImpl.api().getNamespaceService();
            String localname = ns.getAbbreviatedURI(subjectValue);
            
            if (!list.contains((subjectValue).getLocalName())) 
            {
                list.add(localname);
                AutocompleteMap.setNameToUriMapping(localname, subjectValue);
            }
        }

        if (list != null && list.size() > 0) 
        {
            choices = new String[list.size()];
            int i = 0;
            for (String s: list)
                choices[i++] = s;
        }
        
        if (choices != null) 
        {
            inp.setChoices(choices);
            inp.appendClazz("floatRightIwb");
            inp.enablesuggestions = true;
            inp.enablevalidation = false;
        }
        inp.setComparisonType(ComparisonType.Contains);
    }
}
