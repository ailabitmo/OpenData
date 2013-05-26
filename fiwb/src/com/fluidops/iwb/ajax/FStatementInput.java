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

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDeleteButton;
import com.fluidops.ajax.components.FLabel2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.util.Rand;


/**
 * Generic superclass for Input fields that represent an
 * RDF statement.
 * 
 * @author msc
 */
public abstract class FStatementInput extends FContainer
{
    public static class InvalidUserInputException extends Exception
    {
        private static final long serialVersionUID = 1L;
        
        public InvalidUserInputException (String msg)
        {
            super(msg);
        }
    }
    
    private static final Logger logger = Logger.getLogger(FStatementInput.class.getName());

    /**
     * The subject with which the statement input field
     * has been initialized. May be null.
     */
    private Resource initSubject=null;

    /**
     * The predicate with which the statement input field
     * has been initialized. May be null.
     */
    private URI initPredicate=null;

    /**
     * The object with which the statement input field 
     * has been initialized. May be null.
     */
    private Value initObject=null;

    /*
     *  indicate whether the input form is deletable and,
     *  if so, has (already) been deleted:
     *  
     *  - null -> the form is not deletable, i.e. there
     *              is no delete button
     *  - false -> the form has a delete button but 
     *              the stmt has not been deleted so far
     *  - true -> the form has a delete button and the
     *              stmt has been deleted
     */         
    private Boolean deleted=null; // per default: not deletable

    // delete button (may be undefined)
    private FButton deleteButton=null;

    // id field (can be used for assigning FComponent ids)
    protected String inputId = Rand.getIncrementalFluidUUID();
    
    /**
     * True if the subject field is to be displayed.
     * Defaults to false (subject usually implicit by page).
     */
    protected boolean displaySubject = false;

    /**
     * True if the predicate field is to be displayed.
     * Defaults to true.
     */
    protected boolean displayPredicate = true;
    
    /**
     * True if the object field is to be displayed.
     * Defaults to true.
     */    
    protected boolean displayObject = true;
    
    
    /**
     * True if the subject field is editable.
     * Defaults to false.
     */
    protected boolean subjectEditable = true;

    /**
     * True if the predicate field is editable.
     * Defaults to true.
     */
    protected boolean predicateEditable = true;
    
    /**
     * True if the object field is editable.
     * Defaults to true.
     */    
    protected boolean objectEditable = true;
    
    /**
     * Default label for the subject field. Will be initialized
     * on demand, i.e. only if displaySubjectField is true.
     */
    protected FLabel2 subjLabel=null;
    
    /**
     * Default label for the predicate field. Will be initialized
     * on demand, i.e. only if displayPredicateField is true.
     */
    protected FLabel2 predLabel=null;
    
    /**
     * Default label for the object field. Will be initialized
     * on demand, i.e. only if displayObjectField is true.
     */
    protected FLabel2 objLabel=null;

    
    /**
     * Constructor
     * 
     * @param id FComponent id
     * @param s subject init value (may be null)
     * @param p predicate init value (may be null)
     * @param o object init value (may be null)
     * @param deletable whether or not the input is deletable
     */
    protected FStatementInput(String id,Resource s,URI p,Value o,boolean deletable)
    {
        super(id);
        initSubject=s;
        initPredicate=p;
        initObject=o;
        deleted=deletable?false:null;
        initDeleteButton();
    }
    
    /**
     * Constructor
     * 
     * @param id FComponent id
     * @param stmt stmt used for initialization (may be 
     *          null or contain null components)
     * @param deletable whether or not the input is deletable
     */
    protected FStatementInput(String id,Statement stmt,boolean deletable)
    {
        super(id); 
        if (stmt!=null)
        {
            initSubject=stmt.getSubject();
            initPredicate=stmt.getPredicate();
            initObject=stmt.getObject();
        }
        deleted=deletable?false:null;
        initDeleteButton();
    }
    
    @Override
    public void initializeView() 
    {
        ReadDataManager dm = EndpointImpl.api().getDataManager();
        
        // init subject label if it might be used necessary
        if (displaySubject)
        {
            subjLabel=new FLabel2("sl" + inputId,"");
            subjLabel.setValue(dm.getLabel(getInitSubject()));
            add(subjLabel);
        }
            
        // init predicate label if it might be used
        if (displayPredicate)
        {
            predLabel=new FLabel2("pl" + inputId,"");
            predLabel.setValue(dm.getLabel(getInitPredicate()));
            add(predLabel);
        }

        // init object label if it might be used
        if (displayObject)
        {
            objLabel=new FLabel2("ol" + inputId,"");
            objLabel.setValue(dm.getLabel(getInitObject()));
            add(objLabel);
        }
        
        super.initializeView();
    }

    /**
     * Returns the statement with which the object was initialized
     * if the init statement was complete (i.e., s+p+o!=null),
     * null otherwise.
     */
    public Statement initStatement()
    {
        if (initSubject!=null && initObject!=null && initPredicate!=null)
            return ValueFactoryImpl.getInstance().createStatement(initSubject,initPredicate,initObject);
        
        return null; // initialization was not complete
    }

    /**
     * @return the subject with which the statement input
     *          field has been initialized (may be null)
     */
    public Resource getInitSubject()
    {
        return initSubject;
    }
    
    /**
     * @return the subject with which the statement input
     *          field has been initialized (may be null)
     */
    public URI getInitPredicate()
    {
        return initPredicate;
    }

    /**
     * @return the subject with which the statement input
     *          field has been initialized (may be null)
     */
    public Value getInitObject()
    {
        return initObject;
    }

    /**
     * @return sets the subject with which the statement input
     *          field has been initialized (to be used in wizards
     *          where the subject may be generated belatedly)
     *          Make sure to call this method before initializeView().
     */
    public void setInitSubject(Resource initSubject)
    {
        this.initSubject=initSubject;
    }
    
    /**
     * @return sets the predicate with which the statement input
     *          field has been initialized (to be used in wizards
     *          where the predicate may be generated belatedly).
     *          Make sure to call this method before initializeView().
     */
    public void setInitPredicate(URI initPredicate)
    {
        this.initPredicate=initPredicate;
    }

    /**
     * @return sets the object with which the statement input
     *          field has been initialized (to be used in wizards
     *          where the object may be generated belatedly).
     *          Make sure to call this method before initializeView().
     */
    public void setInitObject(Value initObject)
    {
        this.initObject=initObject;
    }

    /**
     * Turn display of the subject on/off
     */
    public void setDisplaySubject(boolean displaySubject)
    {
        this.displaySubject=displaySubject;
    }

    /**
     * Turn display of the predicate on/off
     */
    public void setDisplayPredicate(boolean displayPredicate)
    {
        this.displayPredicate=displayPredicate;
    }
    
    /**
     * Turn display of the object on/off
     */
    public void setDisplayObject(boolean displayObject)
    {
        this.displayObject=displayObject;
    }
    
    /**
     * Declare the statement as readonly, 
     * by setting all fields to non-editable.
     */
    public void setReadonly()
    {
        this.subjectEditable=false;
        this.predicateEditable=false;
        this.objectEditable=false;
        this.deleted=null; // means: no delete button avlb.
    }
    
    /**
     * Check if the statement is readonly
     */
    public boolean isReadonly()
    {
        return !subjectEditable && !predicateEditable && !objectEditable && deleted==null; 
    }

    /**
     * Declare the subject field as editable/non-editable
     */
    protected void setSubjectEditable(boolean subjectEditable)
    {
        this.subjectEditable=subjectEditable;
    }

    /**
     * Declare the predicate field as editable/non-editable
     */
    protected void setPredicateEditable(boolean predicateEditable)
    {
        this.predicateEditable=predicateEditable;
    }
    /**
     * Declare the subject field as editable/non-editable
     */
    protected void setObjectEditable(boolean objectEditable)
    {
        this.objectEditable=objectEditable;
    }
    
    /**
     * Retrieves the display subject field
     */
    public boolean getDisplaySubject()
    {
        return displaySubject;
    }
    
    /**
     * Retrieves the display predicate field
     */
    public boolean getDisplayPredicate()
    {
        return displayPredicate;
    }
    
    /**
     * Retrieves the display object field
     */
    public boolean getDisplayObject()
    {
        return displayObject;
    }
    
    /**
     * To be overriden in subclasses. Should return
     * null if the current subject is undefined or the
     * statement has been deleted, otherwise the value
     * according to the input form.
     * 
     * @return the current subject 
     */
    abstract protected Resource getCurrentSubject() throws InvalidUserInputException;
    
    /**
     * To be overriden in subclasses. Should return
     * null if the current predicate is undefined or the
     * statement has been deleted, otherwise the value
     * according to the input form.
     * 
     * @return the current predicate
     * @throws InvalidUserInputException 
     */    
    abstract protected URI getCurrentPredicate() throws InvalidUserInputException;

    /**
     * To be overriden in subclasses. Should return
     * null if the current object is undefined or the
     * statement has been deleted, otherwise the value
     * according to the input form.
     * 
     * @return the current object
     */
    abstract protected Value getCurrentObject() throws InvalidUserInputException;
    
    /**
     * Returns the current statement if it is complete (i.e., user input
     * has been made), or null if the form does currently not represent
     * a valid statement. This implementation relies on getCurrentSubject(),
     * getCurrentPredicate(), and getCurrentObject() and should work if all
     * of them are re-implemented properly in the subclass. Still, in some
     * cases the developer may opt to completely re-implement this method.
     * @throws InvalidUserInputException 
     */
    public Statement currentStatement() throws InvalidUserInputException
    {
        // if the form has been deleted, it represents no statement
        if (deleted())
            return null;
        
        Resource subject = getCurrentSubject();
        URI predicate = getCurrentPredicate();
        Value object = getCurrentObject();
        
        if (subject!=null && predicate!=null && object!=null)
            return ValueFactoryImpl.getInstance().createStatement(subject,predicate,object);
        
        return null; // not a valid statement
    }
    
    /**
     * Initialize the delete button if activated 
     */
    private void initDeleteButton()
    {
        if (deleted!=null)
        {
            deleteButton=new FDeleteButton("bd"+inputId, EndpointImpl.api().getRequestMapper().getContextPath())
            {
                @Override
                public void onClick() 
                {
                    deleted=true;
                    FStatementInput.this.hide(true);
                }
            };
            deleteButton.setConfirmationQuestion("Statement will be deleted when saving the form. Are you sure you want to proceed?");
            add(deleteButton);
        }
    }
    
    /**
     * Returns true if the statement has been deleted
     */
    protected boolean deleted()
    {
        return deleted!=null && deleted;
    }

    /**
     * Returns the value of the deleted member, may be
     * null, true or false,
     */
    protected Boolean getDeleted()
    {
        return deleted;
    }
    
    /**
     * Returns the HTML anchor of the delete button, if any,
     * or the empty string if it is not defined.
     */
    protected Object deleteButtonAnchor()
    {
        // otherwise, its presence depends on 
        return deleteButton==null || deleted==null?"":deleteButton.htmlAnchor();
    }
    
    /**
     * Returns the HTML anchor of the subject component.
     * The passed component is used if the subject is editable,
     * otherwise the anchor of the subject label is returned.
     */
    protected Object subjectFieldAnchor(FComponent compIfEditable)
    {
        if (!displaySubject)
            return "";
        
        if (subjectEditable && compIfEditable==null)
            throw new RuntimeException("Subjectfield declared editable, "
                    + "but no editable component available");
        
        return subjectEditable?
                compIfEditable.htmlAnchor():subjLabel.htmlAnchor();
    }
    
    /**
     * Returns the HTML anchor of the predicate component.
     * The passed component is used if the predicate is editable,
     * otherwise the anchor of the predicate label is returned.
     */
    protected Object predicateFieldAnchor(FComponent compIfEditable)
    {
        if (!displayPredicate)
            return "";
        
        if (predicateEditable && compIfEditable==null)
            throw new RuntimeException("Predicate field declared editable, "
                    + "but no editable component available");
        
        return predicateEditable?
                compIfEditable.htmlAnchor():predLabel.htmlAnchor();
    }
    
    
    /**
     * Returns the HTML anchor of the object component.
     * The passed component is used if the object is editable,
     * otherwise the anchor of the object label is returned.
     */
    protected Object objectFieldAnchor(FComponent compIfEditable)
    {
        if (!displayObject)
            return "";
        
        if (objectEditable && compIfEditable==null)
            throw new RuntimeException("Object field declared editable, "
                    + "but no editable component available");
        
        return objectEditable?
                compIfEditable.htmlAnchor():objLabel.htmlAnchor();
    }
    
    
    /**
     * Clones the current object, while possibly copying the init
     * subject, predicate, and object according to the configuration.
     * The current values are not considered, i.e. will be blank
     * for the cloned object.
     * 
     * @param id the FComponent id for the new object
     * @param cloneSubj keep (true) or discard (false) the init subject
     * @param clonePred keep (true) or discard (false) the init predicate
     * @param cloneObj keep (true) or discard (false) the init object
     * @return
     */
    public abstract FStatementInput getInitClone(String id,
            boolean cloneInitSubj,boolean cloneInitPred, boolean cloneInitObj);
}