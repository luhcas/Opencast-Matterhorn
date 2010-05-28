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
package org.opencastproject.receipt.api;

/**
 * A service that can be placed into maintenance mode.
 */
public interface Maintainable {
  /**
   * Sets the service into maintenance mode.
   * @param maintenanceMode The mode to set
   */
  void setMaintenanceMode(boolean maintenanceMode);

  /**
   * Whether this service is in maintenance mode
   * @return
   */
  boolean isInMaintenanceMode();
}
