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

package com.fluidops.iwb;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;

import com.fluidops.iwb.extensions.PrinterExtensions;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * global RDF repositories
 * 
 * @author aeb
 */
@SuppressWarnings(value="MS_CANNOT_BE_FINAL", justification="Static fields need to be write accessed externally")
public class Global
{
	
	/**
	 * The history is stored here (snapshots and aggregates)
	 */
	public static Repository historyRepository;	
	
	/**
     * The change history is stored here (positive deltas, statements added)
     */
    public static Repository positiveChangeRepository; 
    
    /**
     * The change history is stored here (negative deltas, statements deleted)
     */
    public static Repository negativeChangeRepository; 
    
    /**
     * The repository to which published changes are propagated
     */
    public static Repository targetRepository;
    
	/**
	 * global top level model
	 */
	public static Repository repository;
    
	/**
	 * Repository for LuceneSail, now should be always different from Global.repository
	 */
	public static SailRepository wikiLuceneRepository;
	
	/**
	 * Extension class for additional and optional stuff, e.g. toolbar buttons and google tracking
	 */
	public static PrinterExtensions printerExtension;
	
	public static String repositoryName(Repository rep) {
		if (repository.equals(rep))
			return "Global.repository";
		if (negativeChangeRepository.equals(rep))
			return "Global.negativeChangeRepository";
		if (positiveChangeRepository.equals(rep))
			return "Global.positiveChangeRepository";
		if (targetRepository.equals(rep))
			return "Global.targetRepository";
		if (historyRepository.equals(rep))
			return "Global.historyRepository";
		if (wikiLuceneRepository.equals(rep))
			return "Global.wikiLuceneRepository";
		return "(unknown)";
	}
}
