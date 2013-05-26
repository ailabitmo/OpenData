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

package com.fluidops.iwb.api.editor;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.iwb.api.EndpointImpl;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.xs.BooleanDV;
import com.sun.org.apache.xerces.internal.impl.dv.xs.DateDV;
import com.sun.org.apache.xerces.internal.impl.dv.xs.DateTimeDV;
import com.sun.org.apache.xerces.internal.impl.dv.xs.DecimalDV;
import com.sun.org.apache.xerces.internal.impl.dv.xs.DoubleDV;
import com.sun.org.apache.xerces.internal.impl.dv.xs.DurationDV;
import com.sun.org.apache.xerces.internal.impl.dv.xs.IntegerDV;



/**
 * This class provides standard validators for known types, e.g. the most
 * important XML schema types as defined in http://www.w3.org/TR/xmlschema11-2/
 * 
 * This enumeration can be used for type based evaluation, i.e. if the target
 * type of some output is known. The enumeration provides a convenience 
 * method to retrieve a fitting validator, c.f. {@link #validatorFor(URI)}.
 * If a validator is available it is returned and the {@link #validate(String)}
 * method can be used. Otherwise, null is returned.
 * 
 * In the Information Workbench we support Datatypes as defined in {@link Datatype}.
 * For each of these we have a matching validator.
 * 
 * @author as
 * @see Datatype
 *
 */
public enum DatatypeValidation implements Validation {

	LITERAL(RDFS.LITERAL) {
		@Override
		public boolean validate(String input) {
			return true;
		}		
	}, 
	
	RESOURCE(RDFS.RESOURCE) {
		@Override
		public boolean validate(String input) {
			return EndpointImpl.api().getNamespaceService().guessURI(input)!=null;
		}
	}, 
	
	STRING(XMLSchema.STRING) {
		@Override
		public boolean validate(String input) {
			return true;
		}		
	},
	
	INTEGER(XMLSchema.INTEGER) {
		@Override
		public boolean validate(String input) {
			try {
				new IntegerDV().getActualValue(input, null);
				return true;
			} catch (InvalidDatatypeValueException e) {
				return false;
			}
		}		
	},
	
	DECIMAL(XMLSchema.DECIMAL) {
		@Override
		public boolean validate(String input) {
			try {
				new DecimalDV().getActualValue(input, null);
				return true;
			} catch (InvalidDatatypeValueException e) {
				return false;
			}
		}		
	}, 
	
	BOOLEAN(XMLSchema.BOOLEAN) {
		@Override
		public boolean validate(String input) {
			try {
				new BooleanDV().getActualValue(input, null);
				return true;
			} catch (InvalidDatatypeValueException e) {
				return false;
			}
		}		
	}, 
	
	DURATION(XMLSchema.DURATION) {
		@Override
		public boolean validate(String input) {
			try {
				new DurationDV().getActualValue(input, null);
				return true;
			} catch (InvalidDatatypeValueException e) {
				return false;
			}
		}
	},
	
	DATE(XMLSchema.DATE) {
		@Override
		public boolean validate(String input) {
			try {
				new DateDV().getActualValue(input, null);
				return true;
			} catch (InvalidDatatypeValueException e) {
				return false;
			}
		}		
	},
	
	DATETIME(XMLSchema.DATETIME) {
		@Override
		public boolean validate(String input) {
			try {
				new DateTimeDV().getActualValue(input, null);
				return true;
			} catch (InvalidDatatypeValueException e) {
				return false;
			}
		}		
	},
	
	NONNEGATIVEINTEGER(XMLSchema.NON_NEGATIVE_INTEGER) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, 0, Long.MAX_VALUE);
		}		
	},
	
	NONPOSITIVEINTEGER(XMLSchema.NON_POSITIVE_INTEGER) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, Long.MIN_VALUE, 0);
		}		
	}, 

	POSITIVEINTEGER(XMLSchema.POSITIVE_INTEGER) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, 1, Long.MAX_VALUE);
		}		
	},

	NEGATIVEINTEGER(XMLSchema.NEGATIVE_INTEGER) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, Long.MIN_VALUE, -1);
		}		
	}, 
	
	LONG(XMLSchema.LONG) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, -9223372036854775808L, 9223372036854775807L);
		}		
	},
	
	INT(XMLSchema.INT) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, -2147483648L, 2147483647);
		}		
	},
	
	SHORT(XMLSchema.SHORT) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, -32768, 32767);
		}		
	},
	
	BYTE(XMLSchema.BYTE) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, -128, 127);
		}		
	},
	
	UNSIGNEDINT(XMLSchema.UNSIGNED_INT) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, 0, 4294967295L);
		}		
	},
	
	UNSIGNEDSHORT(XMLSchema.UNSIGNED_SHORT) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, 0, 65535);
		}		
	},
	
	UNSIGNEDBYTE(XMLSchema.UNSIGNED_BYTE) {
		@Override
		public boolean validate(String input) {
			return isIntegerInRange(input, 0, 255);
		}		
	},
	
	DOUBLE(XMLSchema.DOUBLE) {
		@Override
		public boolean validate(String input) {
			try {
				new DoubleDV().getActualValue(input, null);				
				return true;
			} catch (Exception e) {
				return false;
			}
		}		
	},
	
	FLOAT(XMLSchema.FLOAT) {
		@Override
		public boolean validate(String input) {
			return DOUBLE.validate(input);
		}		
	};

	private DatatypeValidation(URI type) {
		this.type = type;
	}
	
	
	URI type;	
	
	/**
	 * Validate the given input against the given type base validator.
	 * @param input
	 * @return true if validated successfully, false otherwise
	 */
	@Override
	public abstract boolean validate(String input);
	
	static Map<URI, DatatypeValidation> supportedTypes = new HashMap<URI, DatatypeValidation>();
	static {
		for (DatatypeValidation dv : DatatypeValidation.values())
			supportedTypes.put(dv.type, dv);
	}
	
	/**
	 * A convenience method to retrieve a fitting validator:
	 * 
	 *  If a validator is available it is returned and the {@link #validate(String)}
	 *  method can be used. Otherwise, null is returned.
	 *  
	 * @param type
	 * @return a matching validator (if available) or null
	 */
	public static DatatypeValidation validatorFor(URI type) {		
		return supportedTypes.get(type);		
	}
	
	private static boolean isIntegerInRange(String input, long minInclusive, long maxInclusive) {
		try {
			long l = parseLong(input);
			return l>=minInclusive && l<=maxInclusive;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Due to Java 1.6 compatibility we have to remove
	 * the + sign manually (as it is a valid character).
	 * Since Java 1.7 parseLong can deal with the +
	 * character. Once we switch to Java 7 this should
	 * be changed.
	 * 
	 * @param input
	 * @return
	 */
	private static long parseLong(String input) {
		if (input.startsWith("+"))
			input = input.substring(1);
		return Long.parseLong(input);
	}
}
