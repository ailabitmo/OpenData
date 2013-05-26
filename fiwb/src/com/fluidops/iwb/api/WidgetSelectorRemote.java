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

import java.rmi.RemoteException;
import java.util.List;

import org.openrdf.model.Value;

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.network.RMIBase;

public class WidgetSelectorRemote extends RMIBase implements WidgetSelector, RMIRemote
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2727033525321125877L;

	protected WidgetSelectorRemote() throws RemoteException
	{
		super();
	}

	WidgetSelector delegate;
	
	
	public void addWidget(Class<? extends Widget<?>> widget, Operator input, Value value, Boolean applyToInstances, String preCondition) throws RemoteException, Exception
	{
		delegate.addWidget(widget, input, value, applyToInstances, preCondition);
	}

	public void removeWidget(Class<? extends Widget<?>> widget, Operator input, Value value, Boolean applyToInstances) throws RemoteException, Exception
	{
		delegate.removeWidget(widget, input, value, applyToInstances);
	}

	public void selectWidgets(PageContext pc) throws RemoteException
	{
		delegate.selectWidgets(pc);
	}

	@Override
	public void init(RMISessionContext sessionContext, EndpointDescription bootstrap) throws Exception
	{
		delegate = (WidgetSelector)RMIUtils.getDelegate(sessionContext, bootstrap, ((API)bootstrap.getServerApi()).getWidgetSelector(), WidgetSelector.class);
	}

    @Override
    public List<WidgetConfig> getWidgets() throws RemoteException
    {
        return delegate.getWidgets();
    }
}
