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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FDatePicker;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.helper.FHelpers;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;

/**
 * Input form for typed literal values with fixed subject
 * and possibly fixed predicate.
 * 
 * @author msc
 */
public class FDateStatementInput extends FSubjectBasedInput
{
    private static final Logger logger = Logger.getLogger(FDateStatementInput.class.getName());
    
    // input field for predicate
    protected FTextInput2 predInput=null;

    // input field for object
    protected FDatePicker objInput=null;

    protected String internalDateFormat = "dd.MM.yyyy";
    
    /**
     * Constructor
     * 
     * @param id FComponent id
     * @param s subject init value (must not be null)
     * @param p predicate init value (may be null)
     * @param o object init value (may be null)
     * @param the type of the literal (normal, text, ...)
     * @param deletable whether or not the input is deletable
     */
    public FDateStatementInput(String id,Resource s,
            URI p,Literal o,boolean deletable)
    {
        super(id,s,p,o,deletable);
        predicateEditable=false; // default, may be changed
    }
    
    /**
     * Constructor
     * 
     * @param id FComponent id
     * @param stmt stmt used for initialization (may be 
     *          null or contain null components in predicate
     *          and object position)
     * @param the type of the literal (normal, text, ...)
     * @param deletable whether or not the input is deletable
     */
    public FDateStatementInput(String id,Statement stmt,
            boolean deletable)
    {
        super(id,stmt,deletable);
        predicateEditable=false; // default, may be changed
    }
    
    @Override
    public void initializeView() 
    {
        RequestMapper rm = EndpointImpl.api().getRequestMapper();

        // Note: we always initialize the predicate input field, even though 
        // it is not displayed if predicateEditable==false. The reason is to
        // keep the logics simple, i.e. getCurrentPredicate() will retrieve
        // the value from this field.
        predInput=new FTextInput2("pi" + inputId)
        {
            @Override
            public void setValueAndRefresh(String value, boolean callOnChange)
            {
                if (value == null)
                    value = "";
                this.value = value;

                if (callOnChange)
                    onChange();

                // if component is registered already, send client update
                if (getPage() != null)
                    addClientUpdate(new FClientUpdate(Prio.END, "$('"
                            + getComponentid() + "').value='"
                            + FHelpers.escapeSingleQuote(value)
                                    .replace("&lt;", "<").replace("&gt;", ">")
                            + "';"));
            }
        };
        if (getInitPredicate()!=null)
            predInput.setValue(rm.getReconvertableUri(getInitPredicate(),false));
        add(predInput);

        objInput = new FDatePicker("datepicker" + Rand.getIncrementalFluidUUID());
        objInput.setDateFormat(internalDateFormat);
        Date curDate = getDateFromValue(getInitObject());
        if (curDate!=null)
        	objInput.setPreselectedDate(curDate);
        add(objInput);
        
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
    protected Value getCurrentObject()
    {
        if (deleted() || objInput==null)
            return null;

        // if the object is not editable, we do not store the new one:
        // this is necessary because automated datatype selection may
        // "change" the statement and otherwise it may happen that
        // a non-editable statement is re-stored (e.g. with datatype)
        // in the user context
        if (!objectEditable)
            return getInitObject();
            
        // extract value dependent on type of input field
        try
        {
        	// convert internal date format (idf) into system date format
	        String objStr = objInput.getValue();
			SimpleDateFormat idf = new SimpleDateFormat(internalDateFormat);
			Date date = idf.parse(objStr);
			
			if (date!=null)
				return ValueFactoryImpl.getInstance().createLiteral(ReadDataManagerImpl.dateToISOliteral(date));
        }
        catch (Exception e)
        {
        }
        
    	return null; // no or no valid date
    }
        
    @Override
    public String render()
    {
        TemplateBuilder tb = new TemplateBuilder(FLiteralStatementInput.class);
          
        // render
        String render = tb.renderTemplate(
                "displaySubjectField", displaySubject,      // currently not used in template
                "displayPredicateField", displayPredicate,  // currently not used in template
                "displayObjectField", displayObject,        // currently not used in template
                "subject", subjectFieldAnchor(null),
                "predicate", predicateFieldAnchor(predInput), 
                "object", objectFieldAnchor(objInput),
                "deleteButton", deleteButtonAnchor());
        return render;
    }
    
    @Override
    public FStatementInput getInitClone(String id,
            boolean cloneInitSubj,boolean cloneInitPred, boolean cloneInitObj)
    {
        // TODO: it seems this is never called when adding the plus button of a literal
        // input section
        Resource subject = null;
        if (cloneInitSubj)
            subject=getInitSubject();
        
        URI predicate = null;
        if (cloneInitPred)
            predicate=getInitPredicate();
        
        Literal object = null;
        if (cloneInitObj && (getInitObject() instanceof Literal))
            object=(Literal)getInitObject();
        
        // TODO: actually it might be better to use "real" clones rather
        // than links to the existing objects, but actually I don't see
        // potential problems with using pointers here
        FDateStatementInput clone = 
            new FDateStatementInput(
                    id,subject,predicate,object,getDeleted()!=null);
        clone.setDisplaySubject(getDisplaySubject());
        clone.setDisplayPredicate(getDisplayPredicate());
        clone.setDisplayObject(getDisplayObject());
        clone.setSubjectEditable(subjectEditable);
        clone.setPredicateEditable(predicateEditable);
        clone.setObjectEditable(true); // new input makes only sense with editable object 
        
        return clone;
    }

    
    /**
     * Returns a date from a given value. If the value is null or
     * does not represent a valid date, null is returned.
     * 
     * @return
     */
    private Date getDateFromValue(Value v)
    {
    	try
    	{
    		String dateStr = ((Literal)v).stringValue();
    		return ReadDataManagerImpl.ISOliteralToDate(dateStr);
    	}
    	catch (Exception e)
    	{
    	}
    	
    	return null;
    }
}