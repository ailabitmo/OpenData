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

import static com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus.INSTALLED_NOTHING;
import static com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus.INSTALLED_PARTIALLY_WITH_ERRORS;
import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

public class SimpleInstallationResult implements InstallationResult
{
    private static final long serialVersionUID = 3187868017553328156L;
    private final InstallationStatus installationStatus;
    private final List<Exception> exceptions;

    public static SimpleInstallationResult success(InstallationStatus status) {
        assert status.isSuccess(): status;
        return new SimpleInstallationResult(status, Collections.<Exception>emptyList());
    }
    
    public static SimpleInstallationResult nothing() {
        return new SimpleInstallationResult(INSTALLED_NOTHING, Collections.<Exception>emptyList());
    }
    
    public static SimpleInstallationResult failure(Exception ex) {
        return new SimpleInstallationResult(INSTALLED_PARTIALLY_WITH_ERRORS, singletonList(ex));
    }
    
    public SimpleInstallationResult(InstallationStatus installationStatus)
    {
        this(installationStatus, Collections.<Exception>emptyList());
    }

    public SimpleInstallationResult(InstallationStatus installedSuccessfully, List<Exception> exceptions)
    {
        this.installationStatus = installedSuccessfully;
        this.exceptions = exceptions;
    }
    
    @Override
    public InstallationStatus getInstallationStatus()
    {
        return installationStatus;
    }

    @Override
    public List<Exception> getErrors()
    {
        return exceptions;
    }
    
    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("InstallationStatus", installationStatus)
            .append("exceptions", exceptions).toString();
    }
}
