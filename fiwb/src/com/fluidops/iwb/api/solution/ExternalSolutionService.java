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
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;

@Doc("Solution interface that must be implemented by all installable solutions")
public interface ExternalSolutionService extends Remote
{
    @Doc("Install a solution artifact that is retrieved from a remote source")
    InstallationResult install(
             @Par(name="solutionArtifact", type="File", desc="a URL to a solution zip artifact") URL solutionArtifact) 
        throws RemoteException;
}
