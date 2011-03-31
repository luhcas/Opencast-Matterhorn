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
package org.opencastproject.kernel.userdirectory;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Federates user and role providers.
 */
public class UserDirectoryServiceImpl implements UserDirectoryService, UserDetailsService, RoleDirectoryService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserDirectoryServiceImpl.class);

  /** The list of user providers */
  protected Map<String, UserProvider> userProviders = new HashMap<String, UserProvider>();

  /** The list of role providers */
  protected Map<String, RoleProvider> roleProviders = new HashMap<String, RoleProvider>();

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * Adds a user provider.
   * 
   * @param userProvider
   *          the user provider to add
   */
  protected void addUserProvider(UserProvider userProvider) {
    logger.info("Adding {} to the list of user providers", userProvider);
    this.userProviders.put(userProvider.getOrganization(), userProvider);
  }

  /**
   * Remove a user provider.
   * 
   * @param userProvider
   *          the user provider to remove
   */
  protected void removeUserProvider(UserProvider userProvider) {
    logger.info("Removing {} from the list of user providers", userProvider);
    this.userProviders.remove(userProvider.getOrganization());
  }

  /**
   * Adds a role provider.
   * 
   * @param roleProvider
   *          the role provider to add
   */
  protected void addRoleProvider(RoleProvider roleProvider) {
    logger.info("Adding {} to the list of role providers", roleProvider);
    this.roleProviders.put(roleProvider.getOrganization(), roleProvider);
  }

  /**
   * Remove a role provider.
   * 
   * @param roleProvider
   *          the role provider to remove
   */
  protected void removeRoleProvider(RoleProvider roleProvider) {
    logger.info("Removing {} from the list of role providers", roleProvider);
    this.roleProviders.remove(roleProvider.getOrganization());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  public String[] getRoles() {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }
    RoleProvider roleProvider = roleProviders.get(org.getId());
    if (roleProvider == null) {
      throw new IllegalStateException("No role provider for " + org);
    }
    return roleProvider.getRoles();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserDirectoryService#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) throws IllegalStateException {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }
    UserProvider userProvider = userProviders.get(org.getId());
    if (userProvider == null) {
      throw new IllegalStateException("No user provider for " + org);
    }
    return userProvider.loadUser(userName);
  }

  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException,
          org.springframework.dao.DataAccessException {
    User user = loadUser(userName);
    if (user == null) {
      throw new UsernameNotFoundException(userName);
    } else {
      Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
      for (String role : user.getRoles()) {
        authorities.add(new GrantedAuthorityImpl(role));
      }
      String password = user.getPassword() == null ? "" : user.getPassword();
      return new org.springframework.security.core.userdetails.User(user.getUserName(), password, true, true, true,
              true, authorities);
    }
  }

  /**
   * Sets the security service
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleDirectoryService#getLocalRole(java.lang.String)
   */
  @Override
  public String getLocalRole(String role) throws NotFoundException {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }
    RoleProvider roleProvider = roleProviders.get(org.getId());
    if (roleProvider == null) {
      throw new IllegalStateException("No role provider for " + org);
    }
    return roleProvider.getLocalRole(role);
  }

}
