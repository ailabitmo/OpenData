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

import java.util.Collections;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis.utils.StringUtils;

/**
 * Utility class for the inline editing of predicates
 */
public class InlineEditorUtils
{
	public static final String BEGIN_WIKI_LINK = "\\[\\[";
	public static final String END_WIKI_LINK = "\\]\\]";
	
	/**
	 * Valid characters for both the predicate and the object
	 */
	public static final String VALID_CHARS = "[^:\\[\\]]+";
	
	public static final String POTENTIAL_LABEL = "(\\|" + VALID_CHARS + ")?";
	
	private static Pattern pattern;
	
	private static Matcher matcher;
	
	public static Vector<SemanticWikiLinkOccurrence> findMatches(String text, String patternText)
	{
		Vector<SemanticWikiLinkOccurrence> result = new Vector<SemanticWikiLinkOccurrence>();
		
		pattern = Pattern.compile(patternText);
		matcher = pattern.matcher(text);

		while (matcher.find())
			result.add(new SemanticWikiLinkOccurrence(matcher.group(), matcher.start()));
		
		return result;
	}
	
	public static String getUnannotatedLinkToGivenObjectPattern(String text, String object)
	{
		return BEGIN_WIKI_LINK + object + POTENTIAL_LABEL + END_WIKI_LINK;
	}
	
	public static String getAnnotatedLinkToGivenObjectPattern(String text, String object)
	{
		return BEGIN_WIKI_LINK + VALID_CHARS + "::" + object + POTENTIAL_LABEL + END_WIKI_LINK;
	}
	
	/**
	 * @param text Wiki text we're working on
	 * @param object Object / link we are looking for
	 * @return List of all occurrences, in the order in which they appear in the text
	 */
	public static Vector<SemanticWikiLinkOccurrence> getAllOccurrencesSorted(String text, String object)
	{
		String annotatedPattern = getAnnotatedLinkToGivenObjectPattern(text, object);
		String unannotatedPattern = getUnannotatedLinkToGivenObjectPattern(text, object);
		
		Vector<SemanticWikiLinkOccurrence> result = findMatches(text, annotatedPattern);
		result.addAll(findMatches(text, unannotatedPattern));
		
		Collections.sort(result);
		
		return result;
	}
	
	/**
	 * @param text
	 *            Wiki text on which we're working
	 * @param predicate
	 *            Predicate to annotate given object / occurrence with
	 * @param object
	 *            Object / Link to annotate
	 * @param occurrence
	 *            In case there are more than 1 links to that object in the
	 *            given wiki text, this field indicates which occurrence to
	 *            annotate (0 being the first one)
	 * @return
	 */
	public static SemanticWikiLinkOccurrence annotateOccurrence(String text, String predicate, String object, int occurrence)
	{
		// Create occurrences
		Vector<SemanticWikiLinkOccurrence> occurrences = getAllOccurrencesSorted(text, object);
		
		// Prints all occurrences of the given object
		// printThemAll(occurrences);
		
		// Find the occurrence we're interested in
		SemanticWikiLinkOccurrence o = occurrences.elementAt(occurrence);
		
		if (StringUtils.isEmpty(predicate))
			o.removePredicate();
		else
			o.setPredicate(predicate);
		
		// Prints all occurrences of the given object, after the predicate was set / replaced
		// printThemAll(occurrences);
		
		return o;
	}
	
	public static void printThemAll(Vector<SemanticWikiLinkOccurrence> occurrences)
	{
		for (SemanticWikiLinkOccurrence o: occurrences)
		{
			System.out.print("Start pos: " + o.getStartPosition() + " ");
			System.out.print(o.getValue() + " / ");
			System.out.println("found predicate: " + o.getPredicate());
		}
	}
}
