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
package org.opencastproject.kernel.security;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.NotFoundException;

import org.osgi.service.component.ComponentContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the organizational directory. As long as no organizations are published in the service registry, the
 * directory will contain the default organization as the only instance.
 */
public class OrganizationDirectoryServiceImpl implements OrganizationDirectoryService {

  /** The registered organizations */
  protected Map<String, Organization> organizations = new HashMap<String, Organization>();

  /** The organization that is used when no organization directory is available */
  protected Organization defaultOrganization = null;

  /**
   * Sets the default organization to return when no organization directory is registered.
   * 
   * @param cc
   *          the OSGI componentContext
   */
  protected void activate(ComponentContext cc) {
    String configuredServerName = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    URL url = null;
    if (configuredServerName == null) {
      defaultOrganization = new DefaultOrganization();
    } else {
      try {
        url = new URL(configuredServerName);
        defaultOrganization = new DefaultOrganization(url.getHost(), url.getPort());
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }
    addOrganization(defaultOrganization);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganization(java.lang.String)
   */
  @Override
  public Organization getOrganization(String id) throws NotFoundException {
    Organization o = organizations.get(id);
    if (o != null)
      return o;
    throw new NotFoundException(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganization(java.net.URL)
   */
  @Override
  public Organization getOrganization(URL url) throws NotFoundException {
    for (Organization o : organizations.values()) {
      if (o.getServerName().equals(url.getHost()) && o.getServerPort() == url.getPort())
        return o;
    }
    throw new NotFoundException(url.toExternalForm());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganizations()
   */
  @Override
  public List<Organization> getOrganizations() {
    List<Organization> result = new ArrayList<Organization>(organizations.size());
    result.addAll(organizations.values());
    return result;
  }

  /**
   * Adds the organization to the list of organizations.
   * 
   * @param organization
   *          the organization
   */
  public void addOrganization(Organization organization) {
    if (organizations.containsKey(organization))
      throw new IllegalStateException("Can not register an organization with id '" + organization.getId()
              + "' since an organization with that identifier has already been registered");
    organizations.put(organization.getId(), organization);
  }

  /**
   * Removes the organization from the list of organizations.
   * 
   * @param organization
   *          the organization
   */
  public void removeOrganization(Organization organization) {
    organizations.remove(organization);
  }

}
