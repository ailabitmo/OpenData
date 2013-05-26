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

import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FDatePicker;
import com.fluidops.ajax.components.FGroupedDataView;
import com.fluidops.config.Config;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * @author cp
 */
public class FXsdDatePicker extends FValueInputBase
{
    FDatePicker mainInput;

    /**
     * Constructs an RDF literal textarea with no input.
     * 
     * @param id
     *            {@link FComponent} id
     */
    public FXsdDatePicker(String id)
    {
        super(id);
        init();
    }

    /**
     * Constructs an RDF literal textarea with specified input.
     * 
     * @param id
     *            {@link FComponent} id
     * @param initValue
     *            Initial value
     */
    public FXsdDatePicker(String id, Value initValue)
    {
        super(id, initValue);
        init();
    }

    private void init()
    {
        Date date = null;
        
        if (origVal != null) 
        	date = StringUtil.toDate(origVal.stringValue(), "yyyy-MM-dd");

        if (date == null)
            date = new Date();

        mainInput = new FDatePicker(Rand.getIncrementalFluidUUID(), date)
        {
            @Override
            public void populateView()
            {
                super.populateView();

                // client side/html id of this component:
                String id = dateInputField.getComponentid();

                // Focus
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id,
                        "Focus", 5, true));
                // Blur
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id, "Blur",
                        5, true));
            }

            @Override
            public void onchange()
            {
                super.onchange();

                if (origVal != null
                        && mainInput.getValue().equals(origVal.stringValue()))
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
        
        mainInput.setDateFormat("yyyy-MM-dd");
        mainInput.setDateFormatjQuery("yy-mm-dd");
        
        if (origVal != null) 
        	mainInput.setValue(origVal.stringValue());
        
        add(mainInput);
    }

    @Override
    public Value getRdfValue()
    {
    	if (isEmpty())
    		return null;
    	
        return
        		ValueFactoryImpl.getInstance().createLiteral(
                        mainInput.getValue(), XMLSchema.DATE);
    }

    @Override
    public boolean isEmpty()
    {
        return StringUtil.isNullOrEmpty(mainInput.getValue());
    }

    @Override
    public void handleClientSideEvent(FEvent event)
    {
        switch (event.getType())
        {
        case BLUR:
            if (blurCB != null)
                blurCB.handleClientSideEvent(event);
            break;

        case FOCUS:
            if (focusCB != null)
                focusCB.handleClientSideEvent(event);
            break;

        case GENERIC: // subsumes ESC only, ENTER and TAB have different
                      // semantics on text area
            if (event.getArgument().equals("27"))
            { // ESC
                if (cancelCB != null)
                    cancelCB.handleClientSideEvent(null);
            }
            break;

        default:
            super.handleClientSideEvent(event);
        }
    }

    /**
     * Runs parent's populateView() method, then adds updates to capture
     * additional events.
     */
    @Override
    public void populateView()
    {
        super.populateView();

        String id = getId();
        String subId = mainInput.getId();

        // code to suppress some errors unless in debug mode
        String checkExistenceInDOM =
                com.fluidops.config.Config.getConfig().isAjaxDebug() ? ""
                        : "if ($('" + subId + "')) ";

        addClientUpdate(new FClientUpdate(checkExistenceInDOM + ""));

        // MouseOut/In reported to specific receiver method
        addClientUpdate(FGroupedDataView.reportMethodUpdate(id, false));
        addClientUpdate(FGroupedDataView.reportMethodUpdate(id, true));

        // insert methods to receive manually propagated events from specific
        // children (mainInput):
        addClientUpdate(new FClientUpdate(Prio.VERYEND,
                "collectorMethodUpdate('" + id + "', 'Focus', 10, 0, true, "
                        + "'has_focus', 1, 'reportedBlur');"));

        addClientUpdate(new FClientUpdate(Prio.VERYEND,
                "collectorMethodUpdate('" + id + "', 'Blur', 11, 500, true, "
                        + "'has_focus', 0, null);"));
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

}
