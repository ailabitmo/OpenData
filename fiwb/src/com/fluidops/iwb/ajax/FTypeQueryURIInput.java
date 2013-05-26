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

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.NamespaceServiceImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.autocompletion.AutocompleteMap;

public class FTypeQueryURIInput extends FURIInput {

	private URI type;
	
	public FTypeQueryURIInput(String id, URI type) {
		super(id, "", (URI)null);
		this.type = type;
	}
	
	@Override
	public synchronized String[] getChoices() {

		ReadDataManager dmSuggest = EndpointImpl.api().getDataManager();
		HashSet<String> list = new HashSet<String>();
        List<Statement> suggestions = dmSuggest.getStatementsAsList(null, RDF.TYPE, type, false);
       
        NamespaceService ns = EndpointImpl.api().getNamespaceService();
        
        for (int j=0; j<suggestions.size(); j++)
        {
            Statement stmt = suggestions.get(j);
            
            if (!(stmt.getSubject() instanceof URI))
            	continue;
            
            // TODO: needs to be aligned with logic in FURIInput (reconvertableUri)
            String localName = ns.getAbbreviatedURI((URI)stmt.getSubject()); 
            String label = dmSuggest.getLabelHTMLEncoded((URI)stmt.getSubject());
            String listString = "<i>" + label + "</i> (";    
            listString += (localName != null)? localName + ")" : stmt.getSubject().stringValue() + ")";
            list.add(listString);
            AutocompleteMap.setNameToUriMapping(listString, (URI)stmt.getSubject());
        }
           
        String[] choices = new String[list.size()];
        int i = 0;
        for (String s : list) { //int i = 0; i < list.size(); i++) {
            choices[i++] = s;
        }
        return choices;
        
	}

}
