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
package org.opencastproject.security.api;

import org.opencastproject.util.NotFoundException;

/**
 * A marker interface for the federation of all {@link RoleProvider}s.
 */
public interface RoleDirectoryService {

  /**
   * Gets all known roles.
   * 
   * @return the roles
   */
  String[] getRoles();

  /**
   * Returns the local role name as defined by the organization or <code>null</code> if undefined. For example, the
   * matterhorn role <code>{@link AuthorizationService#ADMIN}</code> would translate to the local role name
   * <code>ucb_admin</code>.
   * 
   * @return the local role name
   * @throws NotFoundException
   *           if there is no mapping for role <code>role</code>
   */
  String getLocalRole(String role) throws NotFoundException;

}
