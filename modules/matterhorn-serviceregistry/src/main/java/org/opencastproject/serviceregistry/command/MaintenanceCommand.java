/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.serviceregistry.command;

import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;

/**
 * An interactive shell command for putting Maintainable services in and out of maintenance mode
 * 
 */
public class MaintenanceCommand {
  protected ServiceRegistry jobManager;

  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.jobManager = remoteServiceManager;
  }

  public String set(String jobType, String baseUrl, boolean maintenanceMode) {
    try {
      jobManager.setMaintenanceStatus(jobType, baseUrl, maintenanceMode);
      if (maintenanceMode) {
        return jobType + "@" + baseUrl + " is now in maintenance mode\n";
      } else {
        return jobType + "@" + baseUrl + " has returned to service\n";
      }
    } catch (ServiceUnavailableException e) {
      return jobType + "@" + baseUrl + " is not registered, so its maintenance mode can not be set\n";
    } catch (ServiceRegistryException e) {
      return "Error setting maintenance mode: " + e.getMessage() + "\n";
    }
  }

  public String list() {
    try {
      StringBuilder sb = new StringBuilder();
      for (ServiceRegistration reg : jobManager.getServiceRegistrations()) {
        sb.append(reg.getServiceType());
        sb.append("@");
        sb.append(reg.getHost());
        if (reg.isInMaintenanceMode()) {
          sb.append(" (maintenance mode)");
        }
        sb.append("\n");
      }
      return sb.toString();
    } catch (ServiceRegistryException e) {
      return "Error: " + e.getMessage() + "\n";
    }
  }

}
