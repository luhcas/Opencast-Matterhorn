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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;

import java.util.List;

/**
 * Provides generation and interpretation of policy documents in media packages
 */
public interface AuthorizationService {

  /**
   * Determines whether the current user can take the specified action on the mediapackage.
   * 
   * @param mediapackage
   *          the mediapackage
   * @param action
   *          the action (e.g. read, modify, delete)
   * @return whether the current user has the correct privileges to take this action
   * @throws MediaPackageException
   *           if the policy can not be read from the mediapackage
   */
  boolean hasPermission(MediaPackage mediapackage, String action) throws MediaPackageException;

  /**
   * Gets the permissions associated with this mediapackage, as specified by its XACML attachment.
   * 
   * @param mediapackage
   *          the mediapackage
   * @return the set of permissions and explicit denials
   * @throws MediaPackageException
   *           if the policy can not be read from the mediapackage
   */
  List<AccessControlEntry> getAccessControlList(MediaPackage mediapackage) throws MediaPackageException;

  /**
   * Attaches the provided policies to a mediapackage as a XACML attachment.
   * 
   * @param mediapackage
   *          the mediapackage
   * @param accessControlList
   *          the tuples of roles to actions
   * @return the mediapackage with attached XACML policy
   * @throws MediaPackageException
   *           if the policy can not be attached to the mediapackage
   */
  MediaPackage setAccessControl(MediaPackage mediapackage, List<AccessControlEntry> accessControlList)
          throws MediaPackageException;
}
