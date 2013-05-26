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
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;

import com.fluidops.iwb.ajax.FLiteralStatementInput.LiteralType;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.util.Pair;

/**
 * Class defining help methods for intelligent statement
 * input creation and guessing, e.g. based on predicate
 * and underlying ontology.
 * 
 * @author msc
 */
public class StatementInputHelper
{
    private static final Logger logger = Logger.getLogger(StatementInputHelper.class.getName());
    
    /**
     * Types of fields
     */
    public enum FieldType
    {    
        DROPDOWN("DROPDOWN"),       // dropdown input
        INPUT_LITERAL("LITERAL"),   // literal input
        INPUT_TEXT("TEXT"),         // literal input
        INPUT_LITERAL_DATE("DATE"), // date literal input
        INPUT_URI("URI"),           // URI input
        ONTOLOGY("ONTOLOGY");       // derive type from ontology
    
        private String name;
        
        private FieldType(String name)
        {
            this.name = name;
        }
        
        @Override 
        public String toString()
        {
            return name;
        }
        
        /**
         * Mapping from user string to field type
         * 
         * @param s input string
         * @return the FieldType, or null if invalid
         */
        public static FieldType fromString(String s) 
        {
            if (s!=null) 
                for (FieldType t : FieldType.values())
                    if (s.equalsIgnoreCase(t.name))
                        return t;
    
            return null;
        }
    }

    /**
     * Retrieves a suited statement input for the given statement
     * (or statement fraction) based on the current selection. Should
     * be extended once new statement input widgets are available.
     * 
     * @param id FComponent id
     * @param s the input form subject
     * @param p the input form predicate
     * @param o the input form object
     * @param deletable whether or not the statement is deletable
     * @return the statement input form
     */
    public static FStatementInput guessStatementInput(String id, Resource s,
            URI p, Value o, boolean deletable)
    {
        if (p!=null)
        {
            ReadDataManager dm = EndpointImpl.api().getDataManager();
            if (dm.isObjectProperty(s,p))
                return uriInput(id,s,p,o,deletable);
            else if (dm.isDatatypeProperty(s,p))
                return literalInput(id,s,p,o,deletable,LiteralType.TYPE_NORMAL);
        }
        
        // no suitable statement input found
        return null;
    }
    
    /**
     * As guessStatementInput(), but returns an FStatementInput with no labels
     * or linked icons at all (no predicate label, no delete icon, no page link
     * icon)
     * 
     * @param id FComponent id
     * @param s the input form subject
     * @param p the input form predicate
     * @param o the input form object
     * @return Requested {@link FStatementInput}, or null
     */
    public static FStatementInput guessUndecoratedStatementInput(String id,
            Resource s, URI p, Value o)
    {
        if (p != null)
        {
            ReadDataManager dm = EndpointImpl.api().getDataManager();

            if (dm.isObjectProperty(s, p))
                return undecoratedUriInput(id, s, p, o);
            else if (dm.isDatatypeProperty(s, p))
                return undecoratedLiteralInput(id, s, p, o, LiteralType.TYPE_NORMAL);
        }

        // no suitable statement input found
        return null;
    }
  
    /**
     * Retrieves a statement input for the given statement
     * (or statement fraction) and the specified field type.
     * If type==FieldType.ONTOLOGY, the type (either FieldType.LITERAL
     * or FieldType.URI) is inferred from the ontology (using URI as
     * a default).
     * 
     * @param id
     * @param s the input form subject
     * @param p the input form predicate
     * @param o the input form object
     * @param deletable whether or not the statement is deletable
     * @param query a query used for suggestion (may be null)
     * @param values a list of values for suggestion (may be null)
     * @param rep the repository over which the input works
     * @return the statement input form
     */
    public static FStatementInput getStatementInput(String id, Resource s,
            URI p, Value o, boolean deletable, FieldType type, String query, 
            List<Value> values, Repository rep)
    {
        FieldType internalType = type;
        if (type==FieldType.ONTOLOGY && p!=null)
        {
            ReadDataManager dm = EndpointImpl.api().getDataManager();
            if (dm.isObjectProperty(s,p))
                internalType=FieldType.INPUT_URI;
            else
                internalType=FieldType.INPUT_LITERAL;
        }        
        
        
        switch (internalType)
        {
        case INPUT_LITERAL:
            return literalInput(id,s,p,o,deletable,LiteralType.TYPE_NORMAL);
        case INPUT_TEXT:
            return literalInput(id,s,p,o,deletable,LiteralType.TYPE_TEXT);
        case INPUT_LITERAL_DATE:
            return dateInput(id,s,p,o,deletable);
        case DROPDOWN:
        	return comboboxInput(id,s,p,o,deletable,query,values,rep);
        case INPUT_URI:
        default:        
            return uriInput(id,s,p,o,deletable,query);
        }
    }
    
    /**
     * Returns a literal input form for the given configuration. May
     * return null if the configuration is invalid (e.g., the
     * passed object is a URI).
     * 
     * @param id FComponent ID
     * @param s the input form subject (may not be null)
     * @param p the input form predicate (may be null)
     * @param o the input form object (may be null, but if !=null must be a Literal)
     * @param deletable whether or not the form is deletable
     * @return the constructed input form or null, if construction fails
     * @return
     */
    public static FLiteralStatementInput literalInput(String id, Resource s, 
            URI p, Value o, boolean deletable, LiteralType type)
    {
        try
        {
            Literal oLiteral=null;
            if (o!=null)
                oLiteral=(Literal)o;
            return new FLiteralStatementInput(id,s,p,oLiteral,deletable,type);
        }
        catch (ClassCastException e)
        {
            logger.warn("Cannot construct literal input form:" + e.getMessage());
            return null;
        }
    }
    
    /**
     * Same as literalInput(), but returns a {@link FLiteralStatementInput} without
     * any icon buttons or labels.
     * 
     * @param id FComponent ID
     * @param s the input form subject (may not be null)
     * @param p the input form predicate (may be null)
     * @param o the input form object (may be null, but if !=null must be a Literal)
     * @param deletable whether or not the form is deletable
     * @return the constructed input form or null, if construction fails
     * @return
     */
    public static FLiteralStatementInput undecoratedLiteralInput(String id, Resource s, 
            URI p, Value o, LiteralType type)
    {
        try
        {
            Literal oLiteral = null;
            if (o != null)
                oLiteral = (Literal) o;
            FLiteralStatementInput inp = new FLiteralStatementInput(id, s, p,
                    oLiteral, false, type);
            inp.setDisplayPredicate(false);
            return inp;
        }
        catch (ClassCastException e)
        {
            logger.warn("Cannot construct literal input form:" + e.getMessage());
            return null;
        }
    }

    /**
     * Returns a literal date input form for the given configuration. May
     * return null if the configuration is invalid (e.g., the passed object
     * is a URI).
     * 
     * @param id FComponent ID
     * @param s the input form subject (may not be null)
     * @param p the input form predicate (may be null)
     * @param o the input form object (may be null, but if !=null must be a Literal)
     * @param deletable whether or not the form is deletable
     * @return the constructed input form or null, if construction fails
     * @return
     */
    public static FDateStatementInput dateInput(String id, Resource s, URI p,
            Value o, boolean deletable)
    {
        try
        {
            Literal oLiteral=null;
            if (o!=null)
                oLiteral=(Literal)o;
            return new FDateStatementInput(id,s,p,oLiteral,deletable);
        }
        catch (ClassCastException e)
        {
            logger.warn("Cannot construct date literal input form:" + e.getMessage());
            return null;
        }
    }
    
    /**
     * Returns a URI input form for the given configuration. May
     * return null if the configuration is invalid (e.g., the
     * passed object is a literal).
     * 
     * @param id FComponent ID
     * @param s the input form subject (may not be null)
     * @param p the input form predicate (may be null)
     * @param o the input form object (may be null, but if !=null must be a URI)
     * @param deletable whether or not the form is deletable
     * @return the constructed input form or null, if construction fails
     */
    public static FURIStatementInput uriInput(String id, Resource s, URI p,
            Value o, boolean deletable)
    {
    	return uriInput(id,s,p,o,deletable,null);
    }
    
    
    public static FURIStatementInput uriInput(String id, Resource s, URI p,
            Value o, boolean deletable, String query)
    {
        try
        {
            URI oURI=null;
            if (o!=null)
                oURI=(URI)o;
            return new FURIStatementInput(id,s,p,oURI,deletable,true,true,query);
        }
        catch (ClassCastException e)
        {
            logger.warn("Cannot construct uri input form:" + e.getMessage());
            return null;
        }
    }
    
    /**
     * Same as uriInput(), but returns a {@link FURIStatementInput} without
     * any icon buttons or labels.
     * 
     * @param id FComponent ID
     * @param s the input form subject (may not be null)
     * @param p the input form predicate (may be null)
     * @param o the input form object (may be null, but if !=null must be a URI)
     * @return the constructed input form or null, if construction fails
     */
    public static FURIStatementInput undecoratedUriInput(String id, Resource s, URI p,
            Value o)
    {
        try
        {
            URI oURI=null;
            if (o!=null)
                oURI=(URI)o;
            return new FURIStatementInput(id,s,p,oURI,false,false,false);
        }
        catch (ClassCastException e)
        {
            logger.warn("Cannot construct uri input form:" + e.getMessage());
            return null;
        }
    }
    
    /**
     * Returns a dropdown-based input form for the given configuration. May
     * return null if the configuration is invalid.
     * 
     * @param id FComponent ID
     * @param s the input form subject (may not be null)
     * @param p the input form predicate (may be null)
     * @param o the input form object (may be null, but if !=null must be a URI)
     * @param deletable whether or not the form is deletable
     * @return the constructed input form or null, if construction fails
     * @param rep the repository over which the input works
     */
    public static FStatementInput comboboxInput(String id, Resource s, URI p,
            Value o, boolean deletable, String query, List<Value> values, Repository rep)
    {
        return new FComboboxInput(id,s,p,o,deletable,query,values,rep);
    }
    

    /**
     * Convenience method to save a list of input statement into a new
     * context, while deleting old statement that have become invalid
     * (e.g. because they have been deleted or changed in some
     * FStatementInput field). 
     * 
     * @param rep the repository in which the changes will be saved
     * @param addStmts the statements to add
     * @param remStmts the statements to remove
     * @param changeList the statements that have changed value
     * @param addStmts the FInputStatements to save
     * @param userContext type of the context to create the statement in
     * @return The newly created context holding all changes.
     * 
     * @throws Exception containing a HTML string describing the problems,
     *          the caller should make sure to propagate this message to the user
     */
    public static Context saveStatementInputs(Repository rep,
            List<Statement> addStmts, List<Statement> remStmts, 
            List<Pair<Statement,Statement>> changeList,
            ContextLabel contextLabel)
    throws Exception
    {        
        Context context = Context.getFreshUserContext(contextLabel);        
        
        ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(rep);
        RequestMapper rm = EndpointImpl.api().getRequestMapper();
        
        // collect add and remove list arising from change list
        List<Statement> remByChange = new ArrayList<Statement>();
        List<Statement> addByChange = new ArrayList<Statement>();
        for (Pair<Statement,Statement> pair : changeList)
        {
            remByChange.add(pair.fst);
            addByChange.add(pair.snd);
        }


        // next we record the deleted/changed statements that 
        // have been changed by someone else in meantime
        List<Statement> allRemStmts = new ArrayList<Statement>();
        allRemStmts.addAll(remStmts);
        allRemStmts.addAll(remByChange);
        List<Statement> concurrentlyRemovedStmts = new ArrayList<Statement>();
        for (Statement stmt : allRemStmts)
        {
            try
            {
                List<Statement> existingStmts = 
                    dm.getStatements(stmt.getSubject(), stmt.getPredicate(), 
                                        stmt.getObject(), false).asList();

                boolean existsInEditableContext = false;
                for (Statement existingStmt : existingStmts)
                {
                    existsInEditableContext |= dm.isEditableStatement(existingStmt);
                    if (existsInEditableContext)
                        break;
                }
                
                if (!existsInEditableContext)
                    concurrentlyRemovedStmts.add(stmt);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(),e);
            }
        }
        
        // next we record the added statements that are already present
        // (e.g., have been added by another user before)
        // have been changed by someone else in meantime
        List<Statement> allAddStmts = new ArrayList<Statement>(); 
        allAddStmts.addAll(addStmts);
        allAddStmts.addAll(addByChange);
        List<Statement> previouslyAddedStmts = new ArrayList<Statement>();
        for (Statement stmt : allAddStmts)
        {
            try
            {
                List<Statement> existingStmts = 
                    dm.getStatements(stmt.getSubject(), stmt.getPredicate(), 
                                        stmt.getObject(), false).asList();
                
                boolean existsInEditableContext = false;
                for (Statement existingStmt : existingStmts)
                {
                    existsInEditableContext |= dm.isEditableStatement(existingStmt);
                    if (existsInEditableContext)
                        break;
                }
                
                if (existsInEditableContext)
                    previouslyAddedStmts.add(stmt);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(),e);
            }
        }

        StringBuilder concurrentErrorMessage = new StringBuilder(); // "" means no error
        
        // now we remove the remove statements (not including the change list)...
        for (Statement s : remStmts)
        {
            if (concurrentlyRemovedStmts.contains(s))
            {
                String pVal = rm.getReconvertableUri(s.getPredicate(),false);
                String oVal = (s.getObject() instanceof URI) ? 
                    rm.getReconvertableUri((URI)s.getObject(),false) : s.getObject().stringValue();
                concurrentErrorMessage.append( 
                    "<li>Value-property pair " + pVal + " -&gt; " + oVal
                    + " has been deleted/modified by another user in meantime.</li>");
            }
            else
                dm.removeInEditableContexts(s, context);
        }
        
        // ... and add the add statements (not including the change list) ...
        Set<URI> uris = new HashSet<URI>();
        for (Statement s : addStmts)
        {
            if (previouslyAddedStmts.contains(s))
            {
                String pVal = rm.getReconvertableUri(s.getPredicate(),false);
                String oVal = (s.getObject() instanceof URI) ? 
                    rm.getReconvertableUri((URI)s.getObject(),false) : 
                    ((Literal)(s.getObject())).getLabel();
                concurrentErrorMessage.append( 
                    "<li>Value-property pair " + pVal + " -&gt; " + oVal
                    + " is already present (will not be added again).</li>");
            }
            else
            {
                dm.addToContext(s,context);
                uris.add((URI)s.getSubject());
            }
        }
        
        // ... and finally process the change list
        for (Pair<Statement,Statement> change : changeList)
        {
            if (concurrentlyRemovedStmts.contains(change.fst))
            {
                String pValOld = rm.getReconvertableUri(change.fst.getPredicate(),false);
                String oValOld = (change.fst.getObject() instanceof URI) ? 
                    rm.getReconvertableUri((URI)change.fst.getObject(),false) : 
                    ((Literal)(change.fst.getObject())).getLabel();
                String pValNew = rm.getReconvertableUri(change.snd.getPredicate(),false);
                String oValNew = (change.snd.getObject() instanceof URI) ? 
                    rm.getReconvertableUri((URI)change.snd.getObject(),false) : 
                    ((Literal)(change.snd.getObject())).getLabel();

                concurrentErrorMessage.append(
                    "<li>Value-property pair <i>" + pValOld + "</i> -&gt; <i>" + oValOld
                    + "</i> has been deleted/modified by another user in meantime. " 
                    + "Your change to the new value-property pair <i>" 
                    + pValNew + "</i> -&gt; <i>" + oValNew + "</i> will not be propagated.</li>");
            }
            else if (previouslyAddedStmts.contains(change.snd))
            {
                String pValOld = rm.getReconvertableUri(change.fst.getPredicate(),false);
                String oValOld = (change.fst.getObject() instanceof URI) ? 
                    rm.getReconvertableUri((URI)change.fst.getObject(),false) : 
                    ((Literal)(change.fst.getObject())).getLabel();
                String pValNew = rm.getReconvertableUri(change.snd.getPredicate(),false);
                String oValNew = (change.snd.getObject() instanceof URI) ? 
                    rm.getReconvertableUri((URI)change.snd.getObject(),false) : 
                    ((Literal)(change.snd.getObject())).getLabel();

                concurrentErrorMessage.append(
                    "<li>Your requested change from <i>" + pValOld + "</i> -&gt; <i>" + oValOld
                    + "</i> to <i>" + pValNew + "</i> -&gt; <i>" + oValNew + "</i> will not be" 
                    + "propagated: the new value-property pair is already present in the database.</li>");
            }
            else
            {
                dm.removeInEditableContexts(change.fst, context);
                dm.addToContext(change.snd,context);
                uris.add((URI)change.snd.getSubject());                
            }
        }
        dm.close();

        // update wiki page
        // attention: order in which we update the semantic links matters
        KeywordIndexAPI.removeSemanticLinks(remStmts);
        KeywordIndexAPI.changeSemanticLinks(changeList);
        KeywordIndexAPI.removeSemanticLinks(remByChange);
        
        // update keyword index
        try
        {
            for (URI u : uris)
                KeywordIndexAPI.replaceKeywordIndexEntry(u);
        }
        catch (Exception e) 
        {
            logger.error(e.getMessage(), e);
        }
        
        // pass warning that not everything went fine to caller
        if (concurrentErrorMessage.length()>0)
        {
            String msg = "Parts of the requested data changes have not been stored because they are conflicting:<br>";
            msg += "<ul>";
            msg += concurrentErrorMessage.toString();
            msg += "</ul><br>";
            msg += "These changes will not take effect.<br/><br/>";
            
            throw new Exception(msg);
        }
        
        return context;
    }
}
