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
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.FTextInput2.ComparisonType;
import com.fluidops.ajax.helper.FHelpers;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.api.editor.DatatypeValidation;
import com.fluidops.iwb.autocompletion.AutocompleteMap;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;

/**
 * Input form for typed literal values with fixed subject
 * and possibly fixed predicate.
 * 
 * @author msc
 */
public class FLiteralStatementInput extends FSubjectBasedInput
{
    public static enum LiteralType
    {
        TYPE_NORMAL,    // normal text input
        TYPE_TEXT;      // text area
    }
    
    /**
     * Set to true if you want to use a TextArea
     * instead of a statement input.
     */
    private LiteralType type = null;
    
    // input field for predicate
    protected FTextInput2 predInput=null;

    // input field for object
    protected FComponent objInput=null;

    private boolean predicateChanged = false;
    
    // the datatype selector dropdown
    protected FComboBox datatypeDropdown=null;

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
    public FLiteralStatementInput(String id,Resource s,
            URI p,Literal o,boolean deletable,LiteralType type)
    {
        super(id,s,p,o,deletable);
        this.type=type==null?LiteralType.TYPE_NORMAL:type;
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
    public FLiteralStatementInput(String id,Statement stmt,
            boolean deletable,LiteralType type)
    {
        super(id,stmt,deletable);
        this.type=type==null?LiteralType.TYPE_NORMAL:type;
        predicateEditable=false; // default, may be changed
    }
    
    private void setEdited()
    {
 	   this.predicateChanged = true;
    }
    
    @Override
    public void initializeView() 
    {
        RequestMapper rm = EndpointImpl.api().getRequestMapper();

        final List<URI> range = new ArrayList<URI>();
        
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

			@Override
			public void onChange()
			{
				setEdited();
				((FTextInput2)objInput).populateValidity();
				super.onChange();
			}
        };
        if (getInitPredicate()!=null)
            predInput.setValue(rm.getReconvertableUri(getInitPredicate(),false));

        // if the predicate field is displayed, we turn on autocompletion
        if (predicateEditable)
            setPredicateAutoCompletion(predInput);

        add(predInput);

        // init object input field...
        if (type==LiteralType.TYPE_TEXT)
        {
            objInput=new FTextArea("oi" + inputId);
            objInput.appendClazz("wizardTextArea");
            if (getInitObject()!=null)
            {
                String objStr = getInitObject().stringValue();
                objInput.setValue(objStr);
            }                        
        }
        else
        {
            objInput=new FTextInput2("oi" + inputId)
            {
            	private String oldValue = "";
            	
            	boolean valid = false;
            	
            	@Override
            	public Boolean validate()
            	{
            		range.clear();
            		valid = false;
            		
            		ReadDataManager dm = EndpointImpl.api().getDataManager();
            		
            		boolean hasChanged = !oldValue.equals(this.value) || FLiteralStatementInput.this.predicateChanged;

            		FLiteralStatementInput.this.predicateChanged = false;
            		
        			oldValue = this.value;
        			
        			//if the predicate or the object is changed, try to detect the datatype 
        			//and select the corresponding value in the dropdown box
            		if (hasChanged)
            		{
            		    URI initPred = getInitPredicate();
            		    if (initPred != null)
            		        range.addAll(dm.getPropertyInfo(initPred).getRan());

            		    // no range defined --> everything is allowed
            		    if (range.size() == 0)
            		    {
            		        URI initPredicate = EndpointImpl.api().getNamespaceService().guessURI(predInput.returnValues().toString());
            		        if ( initPredicate == null)
            		            valid = true;
            		        else
            		            range.addAll(dm.getPropertyInfo(initPredicate).getRan());

            		        valid |= range.isEmpty();
            		    } 

	            		if (getInitObject() != null)
	            		{
	            			URI range = ((Literal)getInitObject()).getDatatype();
	            			if(range!=null)
	            			{
		            			datatypeDropdown.addChoice( dm.getLabel(range) , range);
		            			datatypeDropdown.setPreSelected(range);
	            			}
	            		}
	            		else
	            			if(!range.isEmpty())
	            				datatypeDropdown.setPreSelected(range);
	            			else
	            				datatypeDropdown.setPreSelected("");
	            		
	            		datatypeDropdown.populateView();
            		}
            		
            		if(datatypeDropdown == null || 
            				StringUtil.isNullOrEmpty(datatypeDropdown.getSelected().get(0).toString()) ||
            				StringUtil.isNullOrEmpty(this.value))  
            		{
            		    return true;
            		}
            		//the datatype is selected in the datatype combobox
            		else
            		{
            			//if no validator is found 'valid' will remain true 
            		    valid = true;
            		    
            		    URI selectedDatatype = (URI) datatypeDropdown.getSelected().get(0);

                        DatatypeValidation d = DatatypeValidation.validatorFor(selectedDatatype);
                        if (d!=null)
                        	valid = d.validate(returnValues().toString());

            		}

            		return valid;
            	}
            };
            if (getInitObject()!=null)
            {
                String objStr = getInitObject().stringValue();
                objInput.setValue(objStr);
            }            
        }
        add(objInput);
        

        // ... and associated datatype dropdown 
        datatypeDropdown=freshDatatypeDropDown();

        if (!com.fluidops.iwb.util.Config.getConfig().displayDatatypeDropdown())
            datatypeDropdown.hide(true);
        add(datatypeDropdown);
        
        super.initializeView();        
    }

    /**
     * Calculates the datatype dropdown for the literal input
     * @return
     */
    private FComboBox freshDatatypeDropDown() 
    {
        FComboBox menu = new FComboBox("ddd" + inputId){
            
            @Override
            public void onChange()
            {
                if(objInput instanceof FTextInput2 && ((FTextInput2)objInput).getPage()!=null)
                    ((FTextInput2)objInput).populateValidity();
            }
        };

        ReadDataManager dm = EndpointImpl.api().getDataManager();
        menu.addChoice("");
    	menu.addChoice(dm.getLabel(XMLSchema.INTEGER),XMLSchema.INTEGER);
    	menu.addChoice(dm.getLabel(XMLSchema.FLOAT),XMLSchema.FLOAT);
    	menu.addChoice(dm.getLabel(XMLSchema.BOOLEAN),XMLSchema.BOOLEAN);
    	menu.addChoice(dm.getLabel(XMLSchema.DOUBLE),XMLSchema.DOUBLE);
    	menu.addChoice(dm.getLabel(XMLSchema.STRING),XMLSchema.STRING);
    	menu.addChoice(dm.getLabel(XMLSchema.LONG),XMLSchema.LONG);
    	menu.addChoice(dm.getLabel(XMLSchema.DATE),XMLSchema.DATE);
        
        URI curPredicate;
        try
        {
            curPredicate = getCurrentPredicate();
        }
        catch (InvalidUserInputException e)
        {
            curPredicate = null;
        }
        
        // in the remainder, we implement heuristics for datatype preselection
        // if object field has been pre-initialized, use the datatype
        // of the associated literal (if available)
        if (getInitObject()!=null && ((Literal)getInitObject()).getDatatype()!=null)
        {
            menu.clearSelected();
            menu.addSelected(((Literal)getInitObject()).getDatatype());            
        }
        // otherwise, if there is no init object, we try to derive the datatype
        // from predicate range information
        else if (getInitObject()==null && curPredicate!=null)
        {
            Value dataType = null;
            List<Statement> dataTypes = dm.getStatementsAsList(curPredicate, RDFS.RANGE, null, false);
            if (!dataTypes.isEmpty()) 
                dataType = dataTypes.get(0).getObject();
            
            menu.setSelected(dataType);
        }
        // else: no datatype preselection possible
                
        return menu;
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
        String objStr = getInputFieldValue();

        if (!StringUtil.isNullOrEmpty(objStr))
        {
            ArrayList<Object> selected = datatypeDropdown.getSelected();
            if (selected != null && selected.size() == 1
                    && selected.get(0) instanceof URI)
                return ValueFactoryImpl.getInstance().createLiteral(
                        objStr,(URI)selected.get(0));
            else // automatic datatype selection
            {
                // if the datatype is automatic and the stmt did not
                // change with respect to its label, we just return the
                // init statement
                Value initObject = getInitObject();
                if (initObject!=null && initObject instanceof Literal 
                        && initObject.stringValue().equals(objStr))
                    return initObject;
                
                
                // otherwise we infer the datatype;
                URI predicate;
                try
                {
                    predicate = getCurrentPredicate();
                }
                catch (InvalidUserInputException e)
                {
                    predicate = null;
                }
                if (predicate==null)
                    return ValueFactoryImpl.getInstance().createLiteral(objStr);
                else
                    return EndpointImpl.api().getDataManager()
                        .createLiteralForPredicate(getInputFieldValue(),predicate);
            }
        }
        
        return null; // not defined
    }
        
    @Override
    public String render()
    {
        TemplateBuilder tb = new TemplateBuilder(FLiteralStatementInput.class);

        // enable datatype dropdow selection where it makes sense
        Object datatypeDropdownAnchor = "";
        if (type==LiteralType.TYPE_NORMAL)
        {
            datatypeDropdownAnchor = 
                displayObject && objectEditable ? datatypeDropdown.htmlAnchor() : "";
        }       
          
        // render
        String render = tb.renderTemplate(
                "displaySubjectField", displaySubject,      // currently not used in template
                "displayPredicateField", displayPredicate,  // currently not used in template
                "displayObjectField", displayObject,        // currently not used in template
                "subject", subjectFieldAnchor(null),
                "predicate", predicateFieldAnchor(predInput), 
                "object", objectFieldAnchor(objInput),
                "datatypeDropdown", datatypeDropdownAnchor,
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
        FLiteralStatementInput clone = 
            new FLiteralStatementInput(
                    id,subject,predicate,object,getDeleted()!=null,type);
        clone.setDisplaySubject(getDisplaySubject());
        clone.setDisplayPredicate(getDisplayPredicate());
        clone.setDisplayObject(getDisplayObject());
        clone.setSubjectEditable(subjectEditable);
        clone.setPredicateEditable(predicateEditable);
        clone.setObjectEditable(true); // new input makes only sense with editable object 
        
        return clone;
    }
    
    // TODO: move to a global class
    public void setPredicateAutoCompletion(FTextInput2 inp)
    {
        // enable auto suggestion, if it makes sense
        String[] choices = null;
        
        HashSet<String> list = new HashSet<String>();

        ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
        
        List<Statement> res = dm.getStatementsAsList(null, RDF.TYPE, OWL.DATATYPEPROPERTY, false);
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
    
    private String getInputFieldValue()
    {
        String objStr = null;
        if (objInput instanceof FTextInput2)
            objStr = ((FTextInput2)objInput).getInput();
        else if (objInput instanceof FTextArea)
            objStr = ((FTextArea)objInput).getText();
        return objStr;

    }
}