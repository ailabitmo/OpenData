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

import static com.google.common.collect.Ordering.natural;

import java.math.BigDecimal;
import java.util.Comparator;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.ajax.FValue;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Ordering;

/**
 * @author michael.meier
 * Util class for the TableResultWidget.
 * 
 */
public class TableResultComparator {

	
	public enum ValueType {
		BNODE(BNode.class), URI(URI.class), LITERAL(Literal.class);
		
		private Class<? extends Value> valueClass;

		ValueType(Class<? extends Value> valueClass) {
			this.valueClass = valueClass;
		}
		
		public static ValueType toValueType(Class<? extends Value> valueClass) {
			for (ValueType valueType : ValueType.values()) {
				if(valueType.valueClass.isAssignableFrom(valueClass)) return valueType;
			}
			throw new IllegalArgumentException("Unkown valueClass: " + valueClass);
		}
	}
	
	private final static Function<String, BigDecimal> toBigDecimal = new Function<String, BigDecimal>() {
		@Override
		public BigDecimal apply(String input) {
			return new BigDecimal(input);
		}
	};
	private final static Function<String, Boolean> toBoolean = new Function<String, Boolean>() {
		@Override
		public Boolean apply(String input) {
			return Boolean.valueOf(input);
		}
	};
	private final static Function<String, Float> toFloat = new Function<String, Float>() {
		@Override
		public Float apply(String input) {
			return Float.valueOf(input);
		}
	};
	private final static Function<String, Double> toDouble = new Function<String, Double>() {
		@Override
		public Double apply(String input) {
			return Double.valueOf(input);
		}
	};
	private final static Function<Object, String> normalize = new Function<Object, String>() {
		@Override
		public String apply(Object input) {
			return normalize(input);
		}
	};
	private static Function<Object, Value> toValue = new Function<Object, Value>() {
		@Override
		public Value apply(Object input) {
			return getValue(input);
		}
	};
	private static Function<Value, ValueType> toValueType = new Function<Value, ValueType>() {
		@Override
		public ValueType apply(Value input) {
			return ValueType.toValueType(input.getClass());
		}
	};
	private static Function<Value, String> toStringValue = new Function<Value, String>() {
		@Override
		public String apply(Value input) {
			return input.stringValue();
		}
	};
	private static Function<Value, String> toLiteralDataType = new Function<Value, String>() {
		@Override
		public String apply(Value input) {
			if(!(input instanceof Literal)) return null;
			URI datatype = ((Literal)input).getDatatype();
			return datatype == null ? null : datatype.stringValue();
		}
	};
	private static Function<Value, String> toLiteralLanguage= new Function<Value, String>() {
		@Override
		public String apply(Value input) {
			if(!(input instanceof Literal)) return null;
			return ((Literal)input).getLanguage();
		}
	};
	
	/**
	 *
	 * Returns a comparator depending on the input URI.
	 * Currently supported datatypes: decimal (and all subdatatypes), boolean, float, double, string
	 * If the input URI matches none of them, then we fall back on an extended version of the SPARQL order by comparator.   
	 * 	
	 */
	public static Comparator<Object> getComparator(URI u) {
		// comparator for decimal and its subclasses
		if(XMLDatatypeUtil.isDecimalDatatype(u))
			return nullSafeOrderingOn(toBigDecimal);
		
		else if(u.equals(XMLSchema.BOOLEAN)) 
			return nullSafeOrderingOn(toBoolean);
			
		// comparator for float
		else if(u.equals(XMLSchema.FLOAT))
			return nullSafeOrderingOn(toFloat);
			
		// comparator for double
		else if(u.equals(XMLSchema.DOUBLE))
			return nullSafeOrderingOn(toDouble);
		
		// comparator for string
		else if(u.equals(XMLSchema.STRING))
			return nullSafeOrderingOn(Functions.<String>identity());
			
		else  
			return natural()					// 5. compare according to enum: BNode < URI < Literals
					.onResultOf(toValueType)	// 4. type to enum ValueType
					.compound(natural().nullsFirst().onResultOf(toStringValue))		// 6. same value-type -> compare stringValue
					.compound(natural().nullsFirst().onResultOf(toLiteralDataType)) // 7. only Literal -> compare datatype-stringValue
					.compound(natural().nullsFirst().onResultOf(toLiteralLanguage)) // 8. only Literal -> compare lang-string 
					.nullsFirst()				// 3. all non-values at top
					.onResultOf(toValue)		// 2. convert to value (non-values to null)
					.nullsFirst();				// 1. nulls at top
	}


	private static Ordering<Object> nullSafeOrderingOn(
			Function<String, ? extends Comparable<?>> conversion) {
		// read from bottom to top
		return natural()						// 5. natural order of type
				.nullsLast()					// 4. all "uncovertables" at end
				.onResultOf(nullOnNumberFormatException(conversion)) // 3. convert string to specific type (null if impossible)
				.onResultOf(normalize)          // 2. convert to "normalized string
				.nullsLast();					// 1. put null at very end
	}


	private static Function<String, Comparable<?>> nullOnNumberFormatException(
			final Function<String, ? extends Comparable<?>> conversion) {
		return new Function<String, Comparable<?>>() {
			
			@Override
			public Comparable<?> apply(String input) {
				try {
					return conversion.apply(input);
				} catch(NumberFormatException ex) {
					return null;
				}
			}
		};
	}
	
	/*
	 *  Normalizes objects to strings.
	 */
	public static String normalize(Object o) {
		String s=o.toString().toLowerCase();
		
		if (o instanceof FComponent && ((FComponent)o).returnValues() != null) {
			s=((FComponent) o).returnValues().toString();
		}
		return s;
	}
	
	/*
	 *  Casts the input to a Value, is possible.
	 *  Returns null, otherwise.
	 */
	private static Value getValue(Object o) {
		
		if(o instanceof FValue) 
			return ((FValue) o).getOriginalValue();
		else if(o instanceof Value) 
			return (Value) o;
		else 
			return null;
	}
}
