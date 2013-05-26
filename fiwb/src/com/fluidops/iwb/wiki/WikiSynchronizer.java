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

package com.fluidops.iwb.wiki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * Static methods for synchronizing semantic links in Wiki when
 * statements have been changed/deleted outside the Wiki.
 * 
 * @author tma, msc, dau
 */
public class WikiSynchronizer {
    
    private static final Logger logger = Logger.getLogger(WikiSynchronizer.class.getName());
	
    /**
     * Wrapper-method for IWB-usage. Wraps recalculation of wikitexts and
     * storage to the IWB-Wikistorage
     * 
     * @param oldStmt The old Statement to be replaced
     * @param newStmt The new Statement to replace the old Statement
     */
    public static void changeSemanticLinks(Statement oldStmt, Statement newStmt)
    {
        Pair<Statement, Statement> p = new Pair<Statement, Statement>(oldStmt, newStmt);
        List<Pair<Statement, Statement>> pairList = new ArrayList<Pair<Statement, Statement>>();
        pairList.add(p);
        changeSemanticLinks(pairList);
    }
        
	/**
     * Wrapper-method for IWB-usage. Wraps recalculation of wikitexts and
     * storage to the IWB-Wikistorage
     * 
     * @param pairList A List of Pairs consisting of the old a new Statements
     */
    public static void changeSemanticLinks(List<Pair<Statement, Statement>> pairList)
    {
        // Return if no Pair in List
        if (pairList.isEmpty())
            return;
        
        // Check for equality of all old subjects
        List<Statement> stmts = new ArrayList<Statement>();
        for (int j = 0; j < pairList.size(); j++)
        {
            stmts.add(pairList.get(j).fst);
            stmts.add(pairList.get(j).snd);            
        }
        if (checkSubjectForEquality(stmts) == false)
        {
            logger.warn("Subjects in the statement list differ. Selected semantic links were not changed.");
            return;
        }
        
        URI subjectURI = (URI)pairList.get(0).fst.getSubject();
        String s = Wikimedia.getRawWikiContent(subjectURI,null);
    	
        // return if no Wiki text defined
        if (StringUtil.isNullOrEmpty(s))
            return;
        
        for (int i = 0; i < pairList.size(); i++)
            s = updateWikipages(pairList.get(i).fst, pairList.get(i).snd, s);
    	
        try
        {
            Wikimedia.getWikiStorage().storeWikiContent(subjectURI, s, "", new Date());
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);
        }
    }	
    
    /**
     * Wrapper for IWB-usage. Wraps removing of semantic annotations from 
     * the Wikistorage, if the resulting statements have been removed.
     * 
     * @param stmt A Statement containing a URI/Literal
     */
    public static void removeSemanticLinks(Statement stmt) {
        List<Statement> stmtList = new ArrayList<Statement>();
        stmtList.add(stmt);
        removeSemanticLinks(stmtList);
    }
    
    /**
     * Wrapper for IWB-usage. Wraps removing of semantic annotations from 
     * the Wikistorage, if the resulting statements have been removed.
     * 
     * @param stmtList A List of Statements containing URIs/Literals
     */
    public static void removeSemanticLinks(List<Statement> stmtList)
    {
    	// Return if no Statment in List
        if (stmtList.isEmpty())
            return;
        
        // Check if all subjects are equal
        if (checkSubjectForEquality(stmtList) == false)
        {
            logger.warn("Subjects in the statement list differ. Selected semantic links were not removed.");
            return;
        }
        
        NamespaceService ns = EndpointImpl.api().getNamespaceService();
        
        // if subject is a blank node, we do not need to sync wiki, since blanknodes
        // do not have a wiki
        Resource subject = stmtList.get(0).getSubject();
        if (!(subject instanceof URI))
        	return;
        
        URI subjectURI = (URI)stmtList.get(0).getSubject();
        String s = Wikimedia.getRawWikiContent(subjectURI,null);
        
        for (int i = 0; i < stmtList.size(); i++)
        {
            Statement stmt = stmtList.get(i);
            
            // return if no Wiki text defined
            if (StringUtil.isNullOrEmpty(s))
                return;
            
            if (stmt.getObject() instanceof URI)
            {
            	URI object = (URI) stmt.getObject();
                s = s.replace("[[" + ns.getAbbreviatedURI(stmt.getPredicate()) + "::"
                        + ns.getAbbreviatedURI(object)+ "]]", "[["
                        + ns.getAbbreviatedURI(object) + "]]")
                     .replace("[[" + ns.getAbbreviatedURI(stmt.getPredicate()) + "::"
                        + object.stringValue() + "]]", "[["
                        + object.stringValue() + "]]")
            		.replace("[[" + ns.getAbbreviatedURI(stmt.getPredicate()) + ":::"
    	                + object.stringValue() + "]]", "[["
    	                + object.stringValue() + "]]")
    	            .replace("[[" + ns.getAbbreviatedURI(stmt.getPredicate()) + ":::"
                        + ns.getAbbreviatedURI(object)+ "]]", "[["
                        + ns.getAbbreviatedURI(object) + "]]");
    		}
            else
            {
                List<String> abbreviatedURIs = ns.getAbbreviatedURIs(stmt.getPredicate());
                for (String abbreviatedURI : abbreviatedURIs)
                {
        			Value object = stmt.getObject();
                    s = s.replace("[[" + abbreviatedURI + "::" + object.stringValue() + "]]", 
                            "[[" + object.stringValue() + "]]");
                    s = s.replace("[[" + abbreviatedURI + ":=" + object.stringValue() + "]]", 
                            object.stringValue());
                }
            }
        }
            
        try
        {
            Wikimedia.getWikiStorage().storeWikiContent((URI)stmtList.get(0).getSubject(), s, "", new Date());
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);
        }
    }
    
    /**
     * Check for equality of all subjects in the Statement List
     * 
     * @param stmtList The List of Statements to check
     * @return Returns true if all subjects are equal
     */
    private static boolean checkSubjectForEquality(List<Statement> stmtList)
    {
        boolean isEqual = true;
        String subject1 = stmtList.get(0).getSubject().stringValue();
        
        for (int i = 1; i < stmtList.size(); i++)
        {
            String subject2 = stmtList.get(i).getSubject().stringValue();
            
            if (!subject1.equals(subject2))
                isEqual = false;
        }
        
        return isEqual;        
    }

    /**
	 * Most generic way of updating wikipages, when structured data has changed that
	 * initially was created in wiki-syntax. If the object of an outgoing statement
	 * has changed, the object in the semantic link has to be adapted and changed to
	 * the new object. Additionally, this method takes care if predicates have changed.
	 * 
	 * @param oldStmt the old statement
	 * @param newStmt the new statement, replacing the old one
	 * @param wikiContent the old content of the wikipage associated to the subject of the old statement
	 * @return the new wiki-content
	 */
    private static String updateWikipages(Statement oldStmt, Statement newStmt,
            String wikiContent)
    {
        // new value (new statement's object)
        String newValue = "";
        String newFullUri = null;

        NamespaceService ns = EndpointImpl.api().getNamespaceService();

        if (newStmt.getObject() instanceof URI)
        {
            newValue = ns.getAbbreviatedURI((URI) newStmt.getObject());
            newFullUri = newStmt.getObject().stringValue();
        }
        else
        {
            newValue = newStmt.getObject().stringValue();
        }

        // new Wiki entries (to replace the old ones)
        String newPred = ns.getAbbreviatedURI(newStmt.getPredicate());
        String newSeparator =
                newStmt.getObject() instanceof Literal ? ":=" : "::";

        String newEntry = "[[" + newPred + newSeparator + newValue + "]]";
        String newFullUriEntry = null;
        if (newFullUri != null)
            newFullUriEntry = "[[" + newPred + newSeparator + newFullUri + "]]";
        else
            newFullUriEntry = newEntry;

        if (oldStmt.getObject() instanceof URI)
        {
            String oldAbbrevUri =
                    ns.getAbbreviatedURI((URI) oldStmt.getObject());
            String oldFullUri = oldStmt.getObject().stringValue();

            // patterns to replace
            wikiContent = wikiContent.replace(
                    "[[" + ns.getAbbreviatedURI(oldStmt.getPredicate()) + "::"
                            + oldAbbrevUri + "]]", newEntry);
            wikiContent = wikiContent.replace(
                    "[[" + ns.getAbbreviatedURI(oldStmt.getPredicate()) + "::"
                            + oldFullUri + "]]", newFullUriEntry);
            wikiContent = wikiContent.replace(
                    "[[" + ns.getAbbreviatedURI(oldStmt.getPredicate()) + ":::"
                            + oldAbbrevUri + "]]", newEntry);
        }
        else
        // old obj was literal or blank node
        {
            String oldValue = oldStmt.getObject().stringValue();

            wikiContent = wikiContent.replace(
                    "[[" + ns.getAbbreviatedURI(oldStmt.getPredicate()) + "::"
                            + oldValue + "]]", newEntry);
            wikiContent = wikiContent.replace(
                    "[[" + ns.getAbbreviatedURI(oldStmt.getPredicate()) + ":="
                            + oldValue + "]]", newEntry);
        }

        return wikiContent;
    }
}
