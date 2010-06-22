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
package org.opencastproject.remote.api;

/**
 * Manages clustered services and the {@link Receipt}s they may create to enable asynchronous job handling.
 */
public interface ServiceRegistration {
  /**
   * @return the host
   */
  public String getHost();
  
  /**
   * @return the receiptType
   */
  public String getReceiptType();
  
  /**
   * @return the inMaintenanceMode
   */
  public boolean isInMaintenanceMode();
}
