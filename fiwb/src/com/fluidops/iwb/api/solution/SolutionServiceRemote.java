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

package com.fluidops.iwb.api.solution;

import java.net.URL;
import java.rmi.RemoteException;

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.iwb.api.API;
import com.fluidops.network.RMIBase;

public class SolutionServiceRemote extends RMIBase implements ExternalSolutionService, RMIRemote
{
    private static final long serialVersionUID = -6163110582921080263L;
    
    private ExternalSolutionService delegate;

    public SolutionServiceRemote() throws RemoteException {
    }
    
    @Override
    public void init(RMISessionContext sessionContext, EndpointDescription bootstrap) throws Exception
    {
        delegate = (ExternalSolutionService) RMIUtils.getDelegate(sessionContext,
                bootstrap, ((API) bootstrap.getServerApi()).getSolutionService(), ExternalSolutionService.class);
    }
    
    @Override
    public InstallationResult install(URL solutionArtifact) throws RemoteException
    {
        return delegate.install(solutionArtifact);
    }
}
