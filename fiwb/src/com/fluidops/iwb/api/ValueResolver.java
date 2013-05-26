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

package com.fluidops.iwb.api;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.security.XssSafeHttpRequest;

public class ValueResolver
{
    private static final String UNDEFINED = "<i>(undefined)</i>";
	private static final PolicyFactory htmlSanitizer = Sanitizers.FORMATTING
			.and(Sanitizers.BLOCKS)
			.and(Sanitizers.LINKS)
			.and(Sanitizers.STYLES)
			.and(Sanitizers.IMAGES);
	
    /**
     * Generic resolver class, where we can register new value resolvers
     * that transform existing input into a different format.
     * 
     * @author msc, as
     *
     */
    public static enum ResolverType 
    {     
    	/* the following resolvers expect exactly one value */
        DEFAULT("DEFAULT"),                                 // default, when URI/Literal
        DEFAULT_NOERROR("DEFAULT_NOERROR"),
        SYSDATE("SYSDATE"),                                 // converts a system date like '2011-03-31T19:54:33' to user-readable date
        MS_TIMESTAMP2DATE("MS_TIMESTAMP2DATE"),             // date from ms long timestamp
        S_TIMESTAMP2DATE("S_TIMESTAMP2DATE"),               // date from sec long timestamp
        DATE("DATE"),                                       // converts xsd:date to a human-readable format
        TIME("TIME"),                                       // converts xsd:time to a human-readable format
        DATETIME("DATETIME"),                               // converts xsd:dateTime to a human-readable format
        IMAGE("IMAGE"),										// image
        THUMBNAIL("THUMBNAIL"),                             // thumbnail
        BIGTHUMBNAIL("BIGTHUMBNAIL"),                       // big thumbnail
        DOUBLE2INT("DOUBLE2INT"),			                // decimal value from double
        URL("URL"),                                         // URL
        HTML("HTML"),                                       // render as HTML
        LOGINLINK("LOGINLINK"),                             // Link for URL, labelled "Login"
        BYTE2KB("BYTE2KB"),                                 // Byte to KB
        BYTE2MB("BYTE2MB"),                                 // Byte to MB
        BYTE2GB("BYTE2GB"),                                 // Byte to GB
        BYTE2TB("BYTE2TB"),                                 // Byte to TB
        KBYTE2MB("KBYTE2MB"),                               // KByte to MB
        KBYTE2GB("KBYTE2GB"),                               // KByte to GB
        KBYTE2TB("KBYTE2TB"),                               // KByte to TB
        PERCENT("PERCENT"),                                 // Converts a number to percent value
        PERCENT_NOCONVERT("PERCENT_NOCONVERT"),             // Number with additional percent sign
        ROUND_DOUBLE("ROUND_DOUBLE"),                       // Round a number to two decimal places after comma
        CURRENCY_USD("CURRENCY_USD"),                       // Double to currency USD
        CURRENCY_EUR("CURRENCY_EUR"),                       // Double to currency EUR
        CURRENCY_CNY("CURRENCY_CNY"),                       // Double to currency CNY
        
        /* the following resolvers can deal with a list of values */
        COMMA_SEPARATED("COMMA_SEPARATED");					// comma separated representation (uses DEFAULT for each value)
                
        private String name;
    
        private ResolverType(String name)
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
        public static ResolverType fromString(String s) 
        {
        	// TODO use ResolverType.valueOf instead of this method => why implement twice?
            if (s!=null) 
                for (ResolverType t : ResolverType.values())
                    if (s.equalsIgnoreCase(t.name))
                        return t;
    
            return null;
        }
    }
    
    /**
     * Resolves a value string using a specified resolver (as string).
     * Can be extended on demand by new resolvers. Should not return
     * null.
     * 
     * @param value
     * @param resolver
     * @return
     */
    public static String resolve(Value value, ResolverType resolver)
    {
        if (resolver == null)
            resolver = ResolverType.DEFAULT;
        if (value==null)
            value = ValueFactoryImpl.getInstance().createLiteral("");
        
        try
        {
            switch (resolver)
            {
                case MS_TIMESTAMP2DATE:
                    return resolveDateFromTimestamp(value.stringValue(), 1);
                case S_TIMESTAMP2DATE:
                    return resolveDateFromTimestamp(value.stringValue(), 1000);
                case DATE:
                    return resolveCalendar(DatatypeConverter.parseDate(value.stringValue()), "MMM dd, yyyy");
                case TIME:
                    return resolveCalendar(DatatypeConverter.parseTime(value.stringValue()), "HH:mm:ss");
                case DATETIME:
                    return resolveCalendar(DatatypeConverter.parseDateTime(value.stringValue()), "MMM dd, yyyy HH:mm:ss");
                case SYSDATE:
                    return resolveSysDate(value.stringValue());
                case IMAGE:
                    return resolveImage(value);
                case THUMBNAIL:
                    return resolveThumbnail(value,"20px");
                case BIGTHUMBNAIL:
                    return resolveThumbnail(value,"50px");
                case DOUBLE2INT:
                	return resolveDecimalFromDouble(Double.valueOf(value.stringValue()));
                case URL:
                    return resolveURL(value.stringValue(), null);
                case HTML:
                    return resolveHtml(value);
                case LOGINLINK:
                    return resolveURL(value.stringValue(),"Login");
                case BYTE2KB:
                    return resolveByte2X(value.stringValue(), "KB");
                case BYTE2MB:
                    return resolveByte2X(value.stringValue(), "MB");
                case BYTE2GB:
                    return resolveByte2X(value.stringValue(), "GB");
                case BYTE2TB:
                    return resolveByte2X(value.stringValue(), "TB");
                case KBYTE2MB:
                    return resolveKByte2X(value.stringValue(), "MB");
                case KBYTE2GB:
                    return resolveKByte2X(value.stringValue(), "GB");
                case KBYTE2TB:
                    return resolveKByte2X(value.stringValue(), "TB");
                case PERCENT:
                    return resolvePercent(value.stringValue());
                case PERCENT_NOCONVERT:
                    return resolvePercentNoConvert(value.stringValue());                                        
                case ROUND_DOUBLE:
                    return resolveNumber2Places(value.stringValue());
                case CURRENCY_USD:
                    return resolveCurrency(value.stringValue(), "USD");
                case CURRENCY_EUR:
                    return resolveCurrency(value.stringValue(), "EUR");
                case CURRENCY_CNY:
                    return resolveCurrency(value.stringValue(), "CNY");
                case DEFAULT_NOERROR:
                    return resolveDefaultNoError(value);
                case DEFAULT:
                default:
                    return resolveDefault(value); 
            }
        }
        catch (Exception e)
        {
            return resolveDefault(value);
        }
    }
    
    
    /**
     * Resolve a list of values using the specified resolver type
     * 
     * Cases:
     * - Specified resolver supports list of values => use the resolver
     * - Specified resolver does not support list of values => handle the 
     *   first item with resolve(Value, ResolverType), ignore rest
     * - List of values is empty or null => resolve as UNDEFINED
     * - List of values has size one => use resolve()
     * 
     * @param values
     * @param resolver
     * @return
     */
    public static String resolveValues(List<Value> values, ResolverType resolver)
    {
    	if (resolver == null)
            resolver = ResolverType.DEFAULT;
    	if (values==null || values.isEmpty())
    		return resolve(null, resolver);
    	
    	if (values.size()==1)
    		return resolve(values.get(0), resolver);
    	
    	switch (resolver) {
    		case COMMA_SEPARATED:		return resolveCommaSeparated(values);
    		default: 					return resolve(values.get(0), ResolverType.DEFAULT);
    	}
    }
    
    
    /**
     * Default handling for URIs and Literals. Performs some intelligent
     * lookup according to input URI/literal.
     * 
     * @param value The value itself (URI/Literal)
     * @return Returns a String (= Literal) or a link (= URI)
     */
    private static String resolveDefault(Value value)
    {
        return resolveDefault(value, UNDEFINED);
    }

    /**
     * Default handling for URIs and Literals. Performs some intelligent
     * lookup according to input URI/literal.
     * 
     * @param value The value itself (URI/Literal)
     * @return Returns a String (= Literal) or a link (= URI)
     */
    private static String resolveDefaultNoError(Value value)
    {
        return resolveDefault(value, "");
    }
    
    /**
     * Default handling for URIs and Literals. 
     * 
     * @param value The value itself (URI/Literal)
     * @param def The default text in case the value cannot be resolved
     * @return Returns a String (= Literal) or a link (= URI)
     */
    private static String resolveDefault(Value value, String def)
    {
        if (value == null || value.stringValue().isEmpty())
            return def; // default value
        
        if (value instanceof Literal)  
            return StringEscapeUtils.escapeHtml(value.stringValue());
        else
            return EndpointImpl.api().getRequestMapper().getAHrefFromValue(value);
    }

    
    /**
     * Convert a timestamp like 182843848 to a human-readable date.
     * 
     * @param value
     * @return
     */
    private static String resolveDateFromTimestamp(String value,long multiplyToMs)
    {
        Long t = Long.valueOf(value)*multiplyToMs;
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(t);
            
        DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
        return df.format(c.getTime());
    }

    /**
     * Converts a system date like '2011-03-31T19:54:33' to user-readable date.
     * If the input is not a valid system date, the value is returned as is.
     * 
     * @param sysdate
     * @return
     */
    public static String resolveSysDate(String sysdate)
    {
        Date d = ReadDataManagerImpl.ISOliteralToDate(sysdate);
        if (d==null)
            return StringEscapeUtils.escapeHtml(sysdate);

        DateFormat df = null;
        if (d.getHours()==0 && d.getMinutes()==0 && d.getSeconds()==0)
            df = new SimpleDateFormat("MMMMM dd, yyyy");
        else
            df = new SimpleDateFormat("MMMMM dd, yyyy, HH:mm:ss");
        return df.format(d);
    }

    /**
     * Converts a given Calendar object into a human-readable format.
     * 
     * @param cal The Calendar object
     * @param pattern The format pattern used for {@link SimpleDateFormat}
     * @return A human-readable String
     */
    public static String resolveCalendar(Calendar cal, String pattern)
    {
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(cal.getTime());
    }

    /**
     * Converts a string to a an image link, if it represents an image (which is determined by file ending).
     * 
     * @param The image link as a String
     * @return Returns the correct image link
     */
    private static String resolveImage(Value value)
    {
    	String link = getDefaultImageResolver().resolveImage(value);
    	return ImageResolver.imageString(link);
    }
    
	/**
     * Converts a string to a a thumbnail link, if it represents an image (which is determined by file ending).
     * 
     * @param The image link as a String
     * @return Returns the correct image link
     */
    private static String resolveThumbnail(Value value, String height)
    {
    	String link = getDefaultImageResolver().resolveImage(value);
        return ImageResolver.thumbnailString(link,height, "");
    }
    
    /**
     * Get default image resolver.
     * 
	 * @return image resolver
	 */
	private static ImageResolver getDefaultImageResolver() {
		return new ImageResolver(IWBFileUtil.getFileInConfigFolder("images.prop").getPath(), false);
	}
    
    /**
     * 
     * @param value
     * @return
     */
    private static String resolveDecimalFromDouble(Double value) {
        
        DecimalFormat numberFormatter = new DecimalFormat("0");
        String out = numberFormatter.format(value);
        
        return out;
    }
    
    /**
     * Create a URL link
     * @param value The URL link
     * @return Returns a link
     */
	private static String resolveURL(String value,String linkName)
	{
	    String valueStr = value.toString();
	    
        valueStr = XssSafeHttpRequest.cleanXSS(valueStr);
        
        if (linkName==null)
        	linkName = value;
        linkName = StringEscapeUtils.escapeHtml(linkName);
		return "<a href='" + valueStr + "' target='_blank'>" + linkName + "</a>";
	}

    /**
     * Handle HTML in a Literal and undefined occurrence
     * @param value The value itself, URI/Literal
     * @return Returns the String value
     */
    private static String resolveHtml(Value value)
    {
    	return htmlSanitizer.sanitize(value.stringValue());
    }
    	
    /**
     * A number is converted to only have 2 places after the comma.
     * 
     * @param value The number to convert to
     * @return Returns the number with only 2 places after the comma as String
     */
    private static String resolveNumber2Places(String value)
    {
        Double number = Double.valueOf(value);
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(number);
    }

    /**
     * A String is converted into a long and than calculated to the set unit (KB, MB, GB, TB).
     * 
     * @param value The String to convert to long
     * @param unit The Unit in which the value is converted
     * @return Returns String representation of the changed long value with unit
     */
    private static String resolveByte2X(String value, String unit)
    {
        Double byteSize = Double.valueOf(value);
        DecimalFormat df = new DecimalFormat("0.00");

        if (unit.equals("KB"))
            return String.valueOf(df.format(byteSize / 1024.0)) + " " + unit;
        else if (unit.equals("MB"))
            return String.valueOf(df.format(byteSize / 1048576.0)) + " " + unit;
        else if (unit.equals("GB"))
            return String.valueOf(df.format(byteSize / 1073741824.0)) + " " + unit;
        else if (unit.equals("TB"))
            return String.valueOf(df.format((byteSize / 1073741824.0) / 1024.0)) + " " + unit;
        else
            return "<em>" + StringEscapeUtils.escapeHtml(value)
                    + " could not be converted to "
                    + StringEscapeUtils.escapeHtml(unit) + "!</em>";
     }

    /**
     * A String is converted into a long and than calculated to the set unit (MB, GB, TB).
     * 
     * @param value The String to convert to long
     * @param unit The Unit in which the value is converted
     * @return Returns String representation of the changed long value with unit
     */
    private static String resolveKByte2X(String value, String unit)
    {
        Double kByteSize = Double.valueOf(value);
        DecimalFormat df = new DecimalFormat("0.00");

        if (unit.equals("MB"))
            return String.valueOf(df.format(kByteSize / 1024.0)) + " " + unit;
        else if (unit.equals("GB"))
            return String.valueOf(df.format(kByteSize / 1048576.0)) + " " + unit;
        else if (unit.equals("TB"))
            return String.valueOf(df.format(kByteSize / 1073741824.0)) + " " + unit;
        else
            return "<em>" + StringEscapeUtils.escapeHtml(value)
                    + " could not converted to "
                    + StringEscapeUtils.escapeHtml(unit) + "!</em>";
     }
    
    /**
     * Displays a numeric value as percent (including conversion, i.e. multiplication with 100);
     * in case the value is non-numeric, no modifications are performed.
     */
    private static String resolvePercent(String value)
    {
        Double d = Double.valueOf(value); // throws exception if conversion fails
        double dPerc = d*100;
        
        DecimalFormat df = new DecimalFormat("0.00");
        return String.valueOf(df.format(dPerc)) + "%";
    }

    /**
     * Adds a %-sign to a numeric value; in case the value is non-numeric, 
     * no modifications are performed.
     */
    private static String resolvePercentNoConvert(String value)
    {
        Double d = Double.valueOf(value); // throws exception if conversion fails

        DecimalFormat df = new DecimalFormat("0.00");
        return String.valueOf(df.format(d)) + "%";
    }

    
    /**
     * A number, e.g. a double, gets returned with the defined currency symbol
     * @param value The number which is a financial value
     * @param currency The currency to adapt to
     * @return Returns the number with the appropriate currency symbol
     */
    private static String resolveCurrency(String value, String currency)
    {
        if (currency.equals("USD"))
            return "&#36;" + resolveNumber2Places(value);
        if (currency.equals("EUR"))
            return resolveNumber2Places(value) + "&#8364;";
        if (currency.equals("CNY"))
            return "&yen;" + resolveNumber2Places(value);
        
        return StringEscapeUtils.escapeHtml(value) + " "
                + StringEscapeUtils.escapeHtml(currency);
    }
      
    
    
    /**
     * Return a comma separated list of each value rendered as default
     * 
     * @param values
     * @return
     */
    private static String resolveCommaSeparated(List<Value> values) {
    	StringBuilder sb = new StringBuilder();
    	for (Value v : values) {
    		sb.append(resolveDefault(v)).append(", "); 
    	}
    	return sb.substring(0, sb.length()-2 );		// remove the last comma and space
    }
}
