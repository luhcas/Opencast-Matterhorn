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
package org.opencastproject.userdirectory;

import org.opencastproject.security.api.RoleProvider;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides a sorted set of known roles
 */
@Path("/")
public class RoleEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(RoleEndpoint.class);

  /** The set of role providers */
  protected Set<RoleProvider> providers = null;

  // TODO: Add rest docs once the new annotations are available

  /**
   * Constructs a new RoleEndpoint
   */
  public RoleEndpoint() {
    providers = new HashSet<RoleProvider>();
  }

  @GET
  @Path("/list.json")
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  public String getRoles() {
    SortedSet<String> knownRoles = new TreeSet<String>();
    for (RoleProvider provider : providers) {
      for (String role : provider.getRoles()) {
        knownRoles.add(role);
      }
    }
    JSONArray json = new JSONArray();
    json.addAll(knownRoles);

    return json.toJSONString();
  }

  public void addRoleProvider(RoleProvider roleProvider) {
    providers.add(roleProvider);
  }

  public void removeRoleProvider(RoleProvider roleProvider) {
    if (!providers.remove(roleProvider)) {
      logger.warn("{} was not a registered role provider");
    }
  }
}
