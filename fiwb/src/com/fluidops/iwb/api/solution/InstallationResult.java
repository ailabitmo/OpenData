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

import java.io.Serializable;
import java.util.List;

/**
 * Contains metadata on the installation of a single solution respectively what
 * a single {@link SolutionHandler} returned on its aspect of the installation.
 * This can be used to check the status of the installation.
 */
public interface InstallationResult extends Serializable
{
    /**
     * Defines possible statuses for the installation of a solution. The order
     * of these statuses is significant. In case several statuses shall be
     * combined the maximal value should be a valid representation of the
     * overall status.
     */
    enum InstallationStatus {
        /** Installation was successful, however nothing was actually installed. */
        INSTALLED_NOTHING(true, false),
        /** Installation was successful and solution becomes immediately active. */
        INSTALLED_SUCCESSFULLY(true, false),
        /** Installation was successful, however the application needs to be restarted to activate all changes.*/
        INSTALLED_SUCCESSFULLY_RESTART_REQUIRED(true, true),
        /**
         * Installation ended with errors, so the solution is currently installed
         * partially. In this case {@link SolutionInstallationInfo#getErrors()}
         * return the {@link Exception}s that occurred during the installation.
         */
        INSTALLED_PARTIALLY_WITH_ERRORS(false, false);
        
        private final boolean success;
        private final boolean restartRequired;

        private InstallationStatus(boolean success, boolean needsRestart) {
            this.success = success;
            this.restartRequired = needsRestart;
        }
        
        /**
         * @return true, if the installation completed successfully. This
         *         includes the case that nothing needed to be installed at all.
         */
        public boolean isSuccess()
        {
            return success;
        }
        
        /**
         * @return true, if the application needs 
         */
        public boolean isRestartRequired()
        {
            return restartRequired;
        }
    }
    
    InstallationStatus getInstallationStatus();
    
    /**
     * @return If {@link #getInstallationStatus()} returns
     *         {@link InstallationStatus#INSTALLED_PARTIALLY_WITH_ERRORS} this
     *         return all {@link Exception}s that occurred during the
     *         installation, otherwise this returns an empty list.
     */
    List<Exception> getErrors();
}
