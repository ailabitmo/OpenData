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

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link SolutionService} that basically tries a couple of injected
 * {@link SolutionService}s until one is successful installing/detecting ((
 * {@link #install(File)}/{@link #detectSolution()} does not return
 * <code>null</code>) a solution.
 */
public class ChainBasedSolutionService implements SolutionService
{
    private final SolutionService[] servicesChain;

    public ChainBasedSolutionService(SolutionService... servicesChain)
    {
        this.servicesChain = servicesChain;
    }

    @Override
    public InstallationResult install(File solution)
    {
        for (SolutionService service : servicesChain)
        {
            InstallationResult installationResult = service.install(solution);
            if(installationResult != null) return installationResult;
        }
        return null;
    }

    @Override
    public File detectSolution()
    {
        for (SolutionService service : servicesChain)
        {
            File detected = service.detectSolution();
            if(detected!= null) return detected;
        }
        return null;
    }
    
   	@Override
	public URL[] detectSolutions() {
		List<URL> urls = new ArrayList<URL>();
		for (SolutionService service : servicesChain)
        {
            URL[] u = service.detectSolutions();
            if (u != null) {
            	urls.addAll(Arrays.asList(u));
            }
        }
		return (URL[]) urls.toArray(new URL[urls.size()]);
	}


    @Override
    public void addHandler(SolutionHandler<?> handler) {
        for (SolutionService service : servicesChain) {
            service.addHandler(handler);
        }
    }

    @Override
	public InstallationResult install(URL solutionArtifact)
			throws RemoteException
	{
        for (SolutionService service : servicesChain)
        {
            InstallationResult installationResult = service.install(solutionArtifact);
            if(installationResult != null) return installationResult;
        }
        return null;
	}
}
