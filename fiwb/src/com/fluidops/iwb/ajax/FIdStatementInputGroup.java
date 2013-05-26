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

import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.FStatementInput.InvalidUserInputException;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * An statement input group used for deriving a user id. Basically
 * a standard statement input group plus support for id computation
 * etc.
 * 
 * @author msc
 */
public class FIdStatementInputGroup extends FStatementInputGroup
{
    private static final Logger logger = Logger.getLogger(FIdStatementInputGroup.class.getName());
    
    /**
     * The regular expression driving id generation.
     */
    String userRegexp;

    /**
     * If this character/string is set, it is used to replace
     * whitespaces in generated URIs. If not, no whitespaces
     * are allowed.
     */
    String whitespaceReplacementChar;

    /**
     * Whether the ID section generates triples in addition
     * to the ID generation service it provides.
     */
    boolean generateTriples;

    /**
     * Rule for generating the label of the section.
     * Only applies if generateTriples is set to true.
     */
    String idLabelRule;
    
    /**
     * Whether or not the id input section saves the type or not.
     */
    boolean saveType;

    
    /**
     * Constructs an empty FIdStatementInput group with the given ID
     */
    public FIdStatementInputGroup(String id, List<FStatementInput> inputs,
            Repository rep, String userRegexp,
            boolean generateTriples, String idLabelRule,
            String whitespaceReplacementChar,
            boolean saveType, GroupLayout layout)
    {
        super(id,inputs,rep,layout,null);
        setLoadExistingValues(false);
        this.userRegexp=userRegexp;
        this.generateTriples=generateTriples;
        this.idLabelRule=idLabelRule;
        this.whitespaceReplacementChar=whitespaceReplacementChar;
        this.saveType=saveType;
    }
    
    /**
     * Return the id defined by this statement input group.
     * If URI calculation fails, an exception will occur.
     * 
     * @param rep the repository, if specified it is checkked whether the
     *          generated URI already exists and, if so, an Exception is thrown;
     *          just pass null if you want to omit this check
     * @return the id defined by this regexp (null in case it is not defined)
     * @throws human-understandable exception describing the error
     */
    public URI getGeneratedURI(Repository rep) throws Exception
    {
        String res = resolveString(userRegexp);
        
        if (!StringUtil.isNullOrEmpty(whitespaceReplacementChar))
            res=res.replaceAll("\\s",whitespaceReplacementChar);
        
        if (res.matches(".*\\s.*"))
            throw new Exception(
                    "The generated ID must not contain whitespace characters.");
        
        // fix for bug #6248 (exception text is rendered as html)
        if (StringUtil.containsNonIriRefCharacter(res, false))
        	throw new Exception("The name rule does not produce a valid URI: non-URI characters are not allowed in the input.");
        
        URI uri = ValueFactoryImpl.getInstance().createURI(res);
        
        // if the repository is given, we also check if this is
        // a fresh URI and throw an exception if not
        if (uri!=null)
        {
            ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
            if (dm.resourceExists(uri))
            {
                String err = "A resource with the specified properties already exists ";
                err += "(URI=" + uri.stringValue() + "). ";
                err += "Please make sure that your input generates a fresh resource.";
                throw new Exception(err);
            }
        }
        else
        {
            String err = "The name rule does not produce a valid URI. ";
            err += "Please make sure that your input generates a URI.";
            throw new Exception(err);            
        }
        return uri;
    }
    
    @Override
    public void collectStatements(List<Statement> addStmts,
            List<Statement> removeStmts, List<Pair<Statement,Statement>> changeList) throws InvalidUserInputException
    {
        boolean generateLabel = !StringUtil.isNullOrEmpty(idLabelRule);
        
        if (!generateLabel && !generateTriples)
            return;
        
        URI subject = null;
        try
        {
            subject = getGeneratedURI(Global.repository);
        }
        catch (Exception e)
        {
            logger.warn(e.getLocalizedMessage());
        }
        if (subject == null)
            return;
        
        if (generateTriples)
        {
            for (FStatementInput input : inputs)
            {
                // case 1: both statements exist
                URI predicate = input.getInitPredicate();
                Value object = input.getCurrentObject();

                if (predicate != null && object != null)
                    addStmts.add(ValueFactoryImpl.getInstance().createStatement(subject, predicate, object));
            }
        }
        if (generateLabel)
        {
            try
            {
                Literal literal = ValueFactoryImpl.getInstance().createLiteral(resolveString(idLabelRule));
                addStmts.add(ValueFactoryImpl.getInstance().createStatement(subject, RDFS.LABEL, literal));
            }
            catch (Exception e)
            {
                // no critical exception, log warning only
                logger.warn("Label statement could not be added");
            }
        }
    }
    
    public boolean saveType()
    {
    	return saveType;
    }
}
