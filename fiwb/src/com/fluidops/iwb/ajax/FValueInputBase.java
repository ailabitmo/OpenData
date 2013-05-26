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

import org.openrdf.model.Value;
import com.fluidops.ajax.FEventListener;
import com.fluidops.ajax.components.FHorizontalLayouter;
import com.fluidops.ajax.components.groupeddataview.EditFieldComponent;

/**
 * Provides a set of common basic routines for RDF value input components
 * implementing EditFieldComponent, such as initialization and validation
 * methods. The component renders as a simple {@link FHorizontalLayouter}, which
 * arranges any actual component(s).<br/>
 * <br/>
 * In particular: (a) the class initializes the component with an RDF value,
 * stored in protected variable {@ref #origVal}, (b) implements required
 * callback setter methods from {@link EditFieldComponent} and stores the
 * callbacks in protected variables and (c) provides protected validation
 * methods.
 * 
 * @author cp
 */
public abstract class FValueInputBase extends FHorizontalLayouter implements
        EditFieldComponent, ValueInput
{
    protected Value origVal;

    protected boolean allowResource;

    protected boolean allowLiteral;

    protected FEventListener focusCB;

    protected FEventListener blurCB;

    protected FEventListener changeCB;

    protected FEventListener restoreCB;

    protected FEventListener acceptCB;

    protected FEventListener cancelCB;

    protected FEventListener tabOutCB;

    /**
     * Constructs a value input field with the specified ID and initial field
     * value.
     * 
     * @param id
     *            FComponent ID.
     * @param initValue
     *            Value to edit.
     */
    public FValueInputBase(String id, Value initValue)
    {
        super(id);
        init(initValue);
    }

    /**
     * Constructs an empty value input field with the specified ID.
     * 
     * @param id
     *            FComponent ID.
     */
    public FValueInputBase(String id)
    {
        super(id);
        init(null);
    }

    /**
     * @param val
     */
    private void init(Value val)
    {
        origVal = val;

        // EditFieldComponent callbacks
        focusCB = null;
        blurCB = null;
        changeCB = null;
        restoreCB = null;
        acceptCB = null;
        cancelCB = null;
        tabOutCB = null;
    }

    /*
     * (non-Javadoc)
     * @see com.fluidops.ajax.components.groupeddataview.EditFieldComponent#
     * setFocusCallback(com.fluidops.ajax.FEventListener)
     */
    @Override
    public void setFocusCallback(FEventListener cb)
    {
        focusCB = cb;
    }

    /*
     * (non-Javadoc)
     * @see com.fluidops.ajax.components.groupeddataview.EditFieldComponent#
     * setBlurCallback(com.fluidops.ajax.FEventListener)
     */
    @Override
    public void setBlurCallback(FEventListener cb)
    {
        blurCB = cb;
    }

    /*
     * (non-Javadoc)
     * @see com.fluidops.ajax.components.groupeddataview.EditFieldComponent#
     * setChangeCallback(com.fluidops.ajax.FEventListener)
     */
    @Override
    public void setChangeCallback(FEventListener cb)
    {
        changeCB = cb;
    }

    /*
     * (non-Javadoc)
     * @see com.fluidops.ajax.components.groupeddataview.EditFieldComponent#
     * setRestoreCallback(com.fluidops.ajax.FEventListener)
     */
    @Override
    public void setRestoreCallback(FEventListener cb)
    {
        restoreCB = cb;
    }

    /*
     * (non-Javadoc)
     * @see com.fluidops.ajax.components.groupeddataview.EditFieldComponent#
     * setAcceptCallback(com.fluidops.ajax.FEventListener)
     */
    @Override
    public void setAcceptCallback(FEventListener cb)
    {
        acceptCB = cb;
    }

    /*
     * (non-Javadoc)
     * @see com.fluidops.ajax.components.groupeddataview.EditFieldComponent#
     * setCancelCallback(com.fluidops.ajax.FEventListener)
     */
    @Override
    public void setCancelCallback(FEventListener cb)
    {
        cancelCB = cb;
    }

    /*
     * (non-Javadoc)
     * @see com.fluidops.ajax.components.groupeddataview.EditFieldComponent#
     * setTabOutCallback(com.fluidops.ajax.FEventListener)
     */
    @Override
    public void setTabOutCallback(FEventListener cb)
    {
        tabOutCB = cb;
    }

}
