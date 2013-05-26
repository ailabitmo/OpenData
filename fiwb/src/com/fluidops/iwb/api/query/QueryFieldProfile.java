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

package com.fluidops.iwb.api.query;

import info.bliki.wiki.template.dates.StringToTime;
import info.bliki.wiki.template.dates.StringToTimeException;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Contains a descriptor of a particular returned binding variable in a SPARQL tuple query. 
 * Specifically, contains its datatype (or a set of possible datatypes), which can be guessed based on
 * actual bound values returned by the query result.
 * 
 * @author andriy.nikolov
 *
 */
public class QueryFieldProfile {

	/**
	 * Possible types of fields.
	 * @author uli
	 */
	public enum FieldType {
		RESOURCE,
		STRING, 
		NUMERIC,
		DATE,
		GEO_LATITUDE,
		GEO_LONGITUDE,
		BOOLEAN,
		NOMINAL // Values which can serve as names - resources or string literals
	}
	
	// Name of the variable being checked
	String variable;
	
	// Minimal and maximal values among the checked field instances, if the field is numeric
	// public double min;
	// public double max;
	
	public Date minDate = null;
	public Date maxDate = null;
	
	private int counterChecked = 0;
	
	private URI explicitDatatype = null;
	
	boolean maybeNominal = true;
	boolean maybeDate = false;
	boolean maybeNumber = false;
	boolean maybeLongitude = false;
	boolean maybeLatitude = false;
	boolean maybeBoolean = false;
	
	boolean isResource = false;
	
	
	/**
	 * 
	 */
	public QueryFieldProfile(String var) {
		this.variable = var;
	}
	
	/**
	 * 
	 * Checks the value bound to a variable and tries to guess its datatype.
	 * Depending on the check, possible datatypes of the query field profile are updated.
	 * 
	 * @param value The actual value (Resource or Literal) bound to the variable in the query result.
	 */
	public void checkValue(Value value) {
		
		if(value != null) {
			
			if(value instanceof Resource) {
				isResource = true;
			} else {
				
				Literal litValue = (Literal)value;
				
				// Check if it's a date
				Date date = null;
				try {
					XMLGregorianCalendar calendar = litValue.calendarValue();
					date = calendar.toGregorianCalendar().getTime();
							
				} catch(IllegalArgumentException e) {
					try {
						date = new StringToTime(value.stringValue());
					} catch(StringToTimeException e1) {
						setMaybeDate(false);
					}
				}
						
				if(date!=null) {
					minDate = (minDate==null) ? date :
						(date.before(minDate) ? date : minDate);
							
					maxDate = (maxDate==null) ? date :
							(date.after(maxDate) ? date : maxDate);
							
					if(counterChecked==0)
						setMaybeDate(true);
				}
				
				// Check if it's a number
				try {
					
					double numVal = litValue.doubleValue();
					if( counterChecked==0 ) {
						setMaybeNumber(true);
					}
					
					if(numVal>=-180 && numVal<=180) {
						if(counterChecked==0) {
							maybeLongitude = true;
						}
						
						if(numVal>=-90 && numVal<=90) {
							if(counterChecked==0) {
								maybeLatitude = true;
							}
						} else {
							maybeLatitude = false;
						}
						
					} else {
						maybeLongitude = false;
						maybeLatitude = false;
					}
					
					
				} catch(NumberFormatException e) {
					maybeLongitude = false;
					maybeLatitude = false;
					setMaybeNumber(false);
				}
				
				// Check if it's a boolean
				try {
					litValue.booleanValue();
					if(counterChecked==0) {
						setMaybeBoolean(true);
					}
				} catch(IllegalArgumentException e) {
					setMaybeBoolean(false);
				}
				
			}
		}
		
		counterChecked ++;
	}
	
	private void setMaybeNumber(boolean val) {
		if(explicitDatatype==null) {
			this.maybeNumber = val;
		}
	}
	
	private void setMaybeDate(boolean val) {
		if(explicitDatatype==null) {
			this.maybeDate = val;
		}
	}
	
	private void setMaybeBoolean(boolean val) {
		if(explicitDatatype==null) {
			this.maybeBoolean = val;
		}
	}
	
	/**
	 * Explicitly sets the datatype of the variable.
	 * 
	 * @param datatype
	 */
	public void setExplicitDatatype(URI datatype) {
		
		this.explicitDatatype = datatype;
		
		if(datatype.toString().startsWith(XMLSchema.NAMESPACE)) {
			isResource = false;
			
		
			if(isDateDatatype(datatype)) {
				
				maybeDate = true;
				maybeNumber = false;
				maybeLongitude = false;
				maybeLatitude = false;
				maybeBoolean = false;
				maybeNominal = false;
				
			} else if(isNumericDatatype(datatype)) {
				maybeNumber = true;
				maybeDate = false;
				maybeBoolean = false;
				maybeNominal = false;
			} else if(datatype.equals(XMLSchema.BOOLEAN)) {
				maybeBoolean = true;
				maybeDate = false;
				maybeNumber = false;
				maybeLatitude = false;
				maybeLongitude = false;
				maybeNominal = false;
			} 
		} else {
			isResource = true;
			maybeNominal = true;
		}
		
		
	}
	
	public Set<FieldType> getPossibleFieldTypes() {
		Set<FieldType> fieldTypes = new HashSet<FieldType>();
		
		if(isResource) {
			fieldTypes.add(FieldType.RESOURCE);
			fieldTypes.add(FieldType.NOMINAL);
		}  else {
			// Is a literal
			fieldTypes.add(FieldType.STRING);
			if(maybeNumber) {
				fieldTypes.add(FieldType.NUMERIC);
				if(maybeLongitude) {
					fieldTypes.add(FieldType.GEO_LONGITUDE);
					if(maybeLatitude) {
						fieldTypes.add(FieldType.GEO_LATITUDE);
					}
				}
			} else if(maybeDate) {
				fieldTypes.add(FieldType.DATE);
			} else if(maybeBoolean) {
				fieldTypes.add(FieldType.BOOLEAN);
			} else if(maybeNominal) {
				fieldTypes.add(FieldType.NOMINAL);
			}
			
		}
		
		return fieldTypes;
	}
	
	
	private static boolean isDateDatatype(URI datatype) {
		
		if(datatype.equals(XMLSchema.DATETIME)
				||datatype.equals(XMLSchema.TIME)
				||datatype.equals(XMLSchema.DATE)
				||datatype.equals(XMLSchema.GDAY)
				||datatype.equals(XMLSchema.GYEAR)
				||datatype.equals(XMLSchema.GMONTH)
				||datatype.equals(XMLSchema.GMONTHDAY)
				||datatype.equals(XMLSchema.GYEARMONTH)) {
			
			return true;
			
		}
		return false;
	}
	
	private static boolean isNumericDatatype(URI datatype) {
		
		if(datatype.equals(XMLSchema.FLOAT)
				||datatype.equals(XMLSchema.DOUBLE)
				||datatype.equals(XMLSchema.DECIMAL)
				||datatype.equals(XMLSchema.INTEGER)
				||datatype.equals(XMLSchema.BYTE)
				||datatype.equals(XMLSchema.SHORT)
				||datatype.equals(XMLSchema.INT)
				||datatype.equals(XMLSchema.LONG)
				||datatype.equals(XMLSchema.POSITIVE_INTEGER)
				||datatype.equals(XMLSchema.NON_NEGATIVE_INTEGER)
				||datatype.equals(XMLSchema.NEGATIVE_INTEGER)
				||datatype.equals(XMLSchema.UNSIGNED_BYTE)
				||datatype.equals(XMLSchema.UNSIGNED_LONG)
				||datatype.equals(XMLSchema.UNSIGNED_SHORT)
				||datatype.equals(XMLSchema.UNSIGNED_INT)
				||datatype.equals(XMLSchema.BASE64BINARY)
				||datatype.equals(XMLSchema.HEXBINARY)) {
			
			return true;
			
		}
		return false;
		
	}

	/**
	 * @return the variable
	 */
	public String getVariable() {
		return variable;
	}
	
	

	
}
