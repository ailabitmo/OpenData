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

import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.openrdf.model.Value;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FDateTimePicker;
import com.fluidops.ajax.components.FGroupedDataView;
import com.fluidops.config.Config;
import com.fluidops.iwb.util.DateTimeUtil;
import com.fluidops.util.Rand;

/**
 * @author anna.gossen
 *
 */
public class FXsdDateTimePicker extends FValueInputBase
{

    FDateTimePicker mainInput;
	/**
	 * @param id
	 * @param initValue
	 */
	public FXsdDateTimePicker(String id, Value initValue)
	{
		super(id, initValue);
		init();
	}

	/**
	 * 
	 */
    private void init()
    {
        Date date = null;
        
        if (origVal != null) 
        	date = DatatypeConverter.parseDateTime(origVal.stringValue()).getTime();

        mainInput = new FDateTimePicker(Rand.getIncrementalFluidUUID(), date, "")
        {
            @Override
            public void populateView()
            {
                super.populateView();

                // client side/html id of this component:
                String id = mainInput.getComponentid();

                // Focus
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id,
                        "Focus", 5, true));
                
                // TODO blur does not work, must be set to actual inner components
                // Blur
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id, "Blur",
                        5, true));
            }
            
            @Override
            public void onChange()
            {
                super.onChange();
                
                Value newValue = getRdfValue();
                if (origVal != null
                        && newValue!=null && newValue.equals(origVal))
                {
                    if (restoreCB != null)
                        restoreCB.handleClientSideEvent(null);
                }
                else
                {
                    if (changeCB != null)
                        changeCB.handleClientSideEvent(null);
                }
            }

        };
        
        mainInput.setSelectableTime(true);
          
        add(mainInput);
    }


	@Override
	public void focus()
	{
        String id = mainInput.getComponentid();

        String checkExistenceInDOM =
                Config.getConfig().isAjaxDebug() ? "" : "if ($('" + id + "')) ";

        mainInput.addClientUpdate(new FClientUpdate(Prio.VERYEND,
                checkExistenceInDOM + "$('" + id + "').focus();"));

	}

	@Override
	public Value getRdfValue()
	{
    	if (isEmpty())
    		return null;
    	return DateTimeUtil.toDateTimeLiteral(mainInput.getDate());
	}

	@Override
	public boolean isEmpty()
	{
        return mainInput.getDate()==null;
	}
}
