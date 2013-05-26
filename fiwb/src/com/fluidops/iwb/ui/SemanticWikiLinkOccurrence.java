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

package com.fluidops.iwb.ui;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Represents an occurrence of a wiki link in a wiki text. The occurrence can be
 * annotated with a predicate, or the existing predicate can be modified. It
 * provides useful information for finding and replacing that instance
 * (startPosition, value, etc).
 */
public class SemanticWikiLinkOccurrence implements Comparable<SemanticWikiLinkOccurrence>
{
	/**
	 * Used when when replacing the old link with the new edited link
	 */
	private int oldEndPosition;
	
	private int startPosition, endPosition;
	
	private String value;
	
	public SemanticWikiLinkOccurrence(String value, int startPosition)
	{
		this.value = value;
		this.startPosition = startPosition;
		endPosition = oldEndPosition = startPosition + value.length();
	}
	
	public String getPredicate()
	{
		int separateAt = value.indexOf("::");
		if (separateAt < 0)
			return "";
		
		// Start after the first two characters [[, stop before the ::
		return value.substring(2, separateAt);
	}
	
	public void setPredicate(String predicate)
	{
		int predicateDelimiter = value.indexOf("::");
		StringBuilder sb = new StringBuilder(value);
		
		// No existing predicate
		if (predicateDelimiter < 0)
			// After the [[ add the predicate and the ::
			sb.insert(2, predicate + "::");
		else
			// Replace existing predicate
			sb.replace(2, predicateDelimiter, predicate);
		
		value = sb.toString();
		endPosition = startPosition + value.length();
	}
	
	public void removePredicate()
	{
		int predicateDelimiter = value.indexOf("::");
		
		// No existing predicate
		if (predicateDelimiter < 0)
			return;
		else
		{
			StringBuilder sb = new StringBuilder(value);
			
			// Delete existing predicate
			sb.delete(2, predicateDelimiter + 2);
			
			value = sb.toString();
			endPosition = startPosition + value.length();
		}
	}
	
	/**
	 * Allows sorting by start position
	 */
	@SuppressWarnings(value="EQ_COMPARETO_USE_OBJECT_EQUALS", justification="Compare to explicitly defined differently from equals.")
	public int compareTo(SemanticWikiLinkOccurrence swlo)
	{
		if (startPosition < swlo.getStartPosition())
			return -1;
		if (startPosition == swlo.getStartPosition())
			return 0;
		return 1;
	}
	
	public int getOldEndPosition()
	{
		return oldEndPosition;
	}

	public int getStartPosition()
	{
		return startPosition;
	}

	public int getEndPosition()
	{
		return endPosition;
	}

	public String getValue()
	{
		return value;
	}
}