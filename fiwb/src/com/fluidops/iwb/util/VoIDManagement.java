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

package com.fluidops.iwb.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeSet;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.provider.ProviderUtils;

/**
 * Abstract class encapsulating the mechanisms for calculating VoID statistics.
 * The calculation method is abstract. It is implemented by subclasses, since
 * there are two different approaches. The first one uses SPARQL Aggregation
 * queries to calculate the results. The second one iterates over a
 * {@link RepositoryResult} and calculates the statistics manually.
 * 
 * @author marlon.braun
 */
public abstract class VoIDManagement
{
    /**
     * Query for retrieving class partitions when deleting VoID statistics
     */
    private static final String classQuery =
            "CONSTRUCT { " +
            "   ?classPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.CLASS) + " ?class . " +
            "   ?classPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.ENTITIES) + " ?noClasses . " + 
            "  } WHERE { " +
            "   %CONTEXT% " + ProviderUtils.uriToQueryString(Vocabulary.VOID.CLASSPARTITION) + " ?classPartition . " +
            "   ?classPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.CLASS) + " ?class . " +
            "   ?classPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.ENTITIES) + " ?noClasses . }";
    
    /**
     * Query for retrieving property partitions when deleting VoID statistics
     */
    private static final String propertyQuery = 
            "CONSTRUCT { " +
            "   ?propertyPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.PROPERTY) + " ?property . " +
            "   ?propertyPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.ENTITIES) + " ?noProps . " +
            "  } WHERE { " + 
            "   %CONTEXT% " + ProviderUtils.uriToQueryString(Vocabulary.VOID.PROPERTYPARTITION) + " ?propertyPartition . " +
            "   ?propertyPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.PROPERTY) + " ?property . " +
            "   ?propertyPartition " + ProviderUtils.uriToQueryString(Vocabulary.VOID.ENTITIES) + " ?noProps . }";
    
    
    /**
	 * Query for retrieving property partitions when deleting VoID statistics
     * @param context
     * @return
     */
    public static String getPropertyPartitionsQuery(URI context) {
    	return propertyQuery.replace("%CONTEXT%", ProviderUtils.uriToQueryString(context));
    }
    
    /**
     * Query for retrieving class partitions when deleting VoID statistics
     * 
     * @param context
     * @return
     */
    public static String getClassPartitionsQuery(URI context) {
    	return classQuery.replace("%CONTEXT%", ProviderUtils.uriToQueryString(context));
    }
    
    
    /**
     * Comparator for comparing instances of the interface {@link Value}. Values
     * are compared on a lexical base. This comparator is used in the
     * {@link TreeSet}s of this method to gain logarithmic running time for
     * access.
     */
    protected static class ValueComparator implements Comparator<Value>, Serializable
    {
		private static final long serialVersionUID = 1L;

		@Override
        public int compare(Value v1, Value v2)
        {
            String s1 = v1.stringValue();
            String s2 = v2.stringValue();

            int length = Math.min(s1.length(), s2.length());

            for (int i = 0; i < length; i++)
            {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);

                if (c1 > c2)
                {
                    return 1;
                }
                else if (c2 > c1)
                {
                    return -1;
                }
            }

            if (s1.length() == s2.length())
                return 0;
            else if (s1.length() < s2.length())
                return -1;
            else
                return 1;
        }
    }

    /**
     * The method for calculating VoID statistics. Needs to be implemented by
     * implementing subclasses.
     * <p>
     * IMPORTANT NOTE: This method does not delete any VoID statistics before
     * writing newly calculated statistics to the store.
     * 
     * @param context
     *            The context of which statistics are calculated.
     */
    public abstract void calculateVoIDStatistics(URI context);

}
