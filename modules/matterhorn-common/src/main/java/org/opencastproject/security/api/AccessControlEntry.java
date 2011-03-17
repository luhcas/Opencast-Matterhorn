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

/**
 * A tuple of role, action, and whether the combination is to be allowed.
 */
public class AccessControlEntry {
  private String role = null;
  private String action = null;
  private boolean allow = false;

  public AccessControlEntry(String role, String action, boolean allow) {
    this.role = role;
    this.action = action;
    this.allow = allow;
  }

  /**
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * @return the action
   */
  public String getAction() {
    return action;
  }

  /**
   * @return the allow
   */
  public boolean isAllow() {
    return allow;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AccessControlEntry) {
      AccessControlEntry other = (AccessControlEntry) obj;
      return this.allow == other.allow && this.role.equals(other.role) && this.action.equals(other.action);
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (role + action + Boolean.toString(allow)).hashCode();
  }
}