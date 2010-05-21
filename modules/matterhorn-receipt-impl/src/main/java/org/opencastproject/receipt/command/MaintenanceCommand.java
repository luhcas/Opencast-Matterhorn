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
package org.opencastproject.receipt.command;

import org.opencastproject.receipt.api.Maintainable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An interactive shell command for putting Maintainable services in and out of maintenance mode
 *
 */
public class MaintenanceCommand {
  private static final Logger logger = LoggerFactory.getLogger(MaintenanceCommand.class);
  protected List<Maintainable> maintainableServices = new ArrayList<Maintainable>();
  
  public void addMaintainable(Maintainable maintainable) {
    maintainableServices.add(maintainable);
  }
  
  public void removeMaintainable(Maintainable maintainable) {
    maintainableServices.remove(maintainable);
  }
  
  public String set(String serviceRegEx, boolean maintenance) {
    StringBuilder sb = new StringBuilder();
    if(maintainableServices.size() > 0) {
      sb.append("Setting services matching '");
      sb.append(serviceRegEx);
      sb.append("' to ");
      sb.append(maintenance);
      sb.append("\n");
    }
    int matches = 0;    
    for(Maintainable m : maintainableServices) {
      logger.debug("{} matches {}: {}", new Object[] {
              m.getClass().getName(), serviceRegEx, Pattern.matches(serviceRegEx, m.getClass().getName())});
      if(Pattern.matches(serviceRegEx, m.getClass().getName())) {
        if(m.isInMaintenanceMode() != maintenance) {
          // we are changing the service's maintenance mode state
          m.setMaintenanceMode(maintenance);
          sb.append(m.getClass().getName());
          matches++;
          if(maintenance) {
            sb.append(" is now in maintenance mode\n");
          } else {
            sb.append(" has returned to service \n");
          }
        }
      }
    }
    sb.append("Set maintenance mode on ");
    sb.append(matches);
    sb.append(" services");
    return sb.toString();
  }

  public String list() {
    StringBuilder sb = new StringBuilder();
    if(maintainableServices.size() > 0) {
      sb.append("Listing maintainable services...\n");
    } else {
      sb.append("No maintainable services\n");
    }
    for(Maintainable m : maintainableServices) {
      if(m.isInMaintenanceMode()) {
        sb.append("Maintenance mode: ");
      } else {
        sb.append("Normal mode: ");
      }
      sb.append(m.getClass().getName());
      sb.append("\n");
    }
    return sb.toString();
  }

}
