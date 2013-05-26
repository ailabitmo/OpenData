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

package com.fluidops.iwb.api.wiki;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.widget.SemWiki;
import com.fluidops.iwb.wiki.WikiStorage;
import com.fluidops.iwb.wiki.Wikimedia;

public class SemanticLinkExtractor
{
    private static final Logger logger = Logger.getLogger(SemanticLinkExtractor.class.getName());

    @CallableFromWidget
    public static void extractSemanticLinks()
    {
    	extractSemanticLinks(Wikimedia.getWikiStorage().getAllWikiURIs());
    }


    @CallableFromWidget
    public static void extractSemanticLinks(List<URI> wikiPages)
    {
        ReadWriteDataManager dm = null;
        try
        {
            WikiStorage storage = Wikimedia.getWikiStorage();
			Context context = Context.getFreshUserContext(ContextLabel.WIKI);
			
            dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
            for (URI uri : wikiPages)
            {
                String content = storage.getRawWikiContent(uri,null);
                
                // collect previous statements
                List<Statement> oldStatements = dm.getStatementsAsList(uri, null, null, false);
                List<Statement> toDelete = new ArrayList<Statement>();
                for (Statement stmt : oldStatements)
                {
                	if (stmt.getContext()!=null && stmt.getContext() instanceof URI)
                	{
                    	Context c = dm.getContext((URI)stmt.getContext());
                    	if (c!=null && c.isUserContext() && c.getLabel()!=null && 
                    			(c.getLabel().equals(ContextLabel.WIKI)))
                    		toDelete.add(stmt);
                	}
                }
                
                // delete previous and write new statements
        		dm.removeInSpecifiedContexts(toDelete,null);
                SemWiki.saveSemanticLinkDiff("", content, uri, context);
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
            if (dm != null)
                dm.close();
        }
    }
    
}
