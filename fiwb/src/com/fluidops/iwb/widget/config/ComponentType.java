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

package com.fluidops.iwb.widget.config;

import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventListener;
import com.fluidops.ajax.FEventType;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;

/**
 * This enum specifies a set of UI components used as input for widgets. The
 * intention is to establish a consistent naming scheme throughout all widget
 * configs.
 * 
 * @author christian.huetter
 */
public enum ComponentType 
{
	/**
	 * A text field enables the user to type a single line of text.
	 */
	TEXTFIELD {
		@Override
		public FComponent create(final String id, final FEventListener eventListener)
		{
			return new FTextInput2(id)
			{
	        	@Override
				public void onEnter() 
	        	{
	        		eventListener.handleClientSideEvent(new FEvent(id, FEventType.KEY_ENTER, this.value));
				}
			};
		}
	},
	/**
	 * A text area allows the user to type multiple lines of text.
	 */
	TEXTAREA {
		@Override
		public FComponent create(final String id, final FEventListener eventListener)
		{
			return new FTextArea(id);
		}
	},
	/**
	 * A drop-down list of values from which the user may choose one.
	 */
	DROPDOWN {
		@Override
		public FComponent create(final String id, final FEventListener eventListener)
		{
			return new FComboBox(id);
		}
	};
	
	/**
	 * Creates a new input component with the given id. The component type
	 * depends on the enum field implementing this method.
	 * 
	 * @param id	unique id in html document
	 * @param eventListener	listener to be called when the component triggers an event
	 * @return new input component
	 */
	public abstract FComponent create(final String id, final FEventListener eventListener);
}