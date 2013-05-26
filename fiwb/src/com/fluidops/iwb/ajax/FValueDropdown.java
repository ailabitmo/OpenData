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

import java.util.ArrayList;

import org.openrdf.model.Value;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FGroupedDataView;
import com.fluidops.config.Config;
import com.fluidops.iwb.autocompletion.AutoSuggester;
import com.fluidops.iwb.util.UIUtil;
import com.fluidops.util.Rand;

/**
 * @author cp
 */
public class FValueDropdown extends FValueInputBase
{
	
	public static final int DEFAULT_MAX_DISPLAY_LENGTH = 50;
	
    FComboBox mainInput;
    private AutoSuggester suggester;
    
    /**
     * Constructs an RDF literal textarea with specified input.
     * 
     * @param id
     *            {@link FComponent} id
     * @param initValue
     *            Initial value
     */
    public FValueDropdown(String id, Value initValue)
    {
        super(id, initValue);
        init();
    }

    private void init()
    {
        mainInput = new FComboBox(Rand.getIncrementalFluidUUID())
        {
            @Override
            public void populateView()
            {
                super.populateView();

                // client side/html id of this component:
                String id = getComponentid();

                // Focus
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id,
                        "Focus", 5, true));
                // Blur
                addClientUpdate(FGroupedDataView.reportMethodUpdate(id, "Blur",
                        5, true));
            }

            @Override
            public void onChange()
            {
                super.onChange();
                
                Value currentValue = getCurrentValue();
                boolean isEmpty = isEmpty();
                
                boolean hasRestoredOldValue = 
                		(origVal==null && isEmpty ) 
                		|| (origVal!=null && !isEmpty && currentValue.equals(origVal));
                
                if (hasRestoredOldValue) {
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

        mainInput.setMaxDisplayLength(DEFAULT_MAX_DISPLAY_LENGTH);
        
        add(mainInput);
        
        suggester = null;
    }

    /**
     * Safely access current selection; returns null if current selection is not
     * a {@link Value} or if there is no single selected value.
     * 
     * @return
     */
    private Value getCurrentValue()
    {
        ArrayList<Object> sel = mainInput.getSelected();

        if (sel.size() != 1 || !(sel.get(0) instanceof Value))
            return null;

        return (Value) sel.get(0);
    }

    @Override
    public Value getRdfValue()
    {
        Value ret = getCurrentValue();

        if (ret==null || ret.stringValue().equals(""))
            return null;

        return ret;
    }

    @Override
    public boolean isEmpty()
    {
        Value val = getCurrentValue();
        return val==null || val.stringValue().equals("");
    }
    
    private void updateChoices()
    {
    	if(this.origVal==null) {
    		mainInput.addChoice("", "");
    	}
        for (Value v : suggester.suggest(""))
        {
            mainInput.addChoice(UIUtil.getDisplayStringForDropdown(v), v);
        }
        
        setSelectedValue(origVal);
    }
    
    /**
     * Sets the suggester that is supposed to produce available values.
     * 
     * @param suggester
     */
    public void setSuggester(AutoSuggester suggester)
    {
        this.suggester = suggester;
        updateChoices();
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
                "collectorMethodUpdate('" + id + "', 'Blur', 11, 100, true, "
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
    
    private void setSelectedValue(Value val) {
    	if(val!=null) {
    		mainInput.setSelected(val);
    	} else {
    		mainInput.setSelected("");
    	}
    }
}
