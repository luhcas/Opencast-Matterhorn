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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Federates user and role providers, and exposes a spring UserDetailsService so user lookups can be used by spring
 * security.
 */
public class UserAndRoleDirectoryServiceImpl implements UserDirectoryService, UserDetailsService, RoleDirectoryService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserAndRoleDirectoryServiceImpl.class);

  /** The list of user providers */
  protected Map<String, List<UserProvider>> userProviders = new HashMap<String, List<UserProvider>>();

  /** The list of role providers */
  protected Map<String, List<RoleProvider>> roleProviders = new HashMap<String, List<RoleProvider>>();

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * Adds a user provider.
   * 
   * @param userProvider
   *          the user provider to add
   */
  protected void addUserProvider(UserProvider userProvider) {
    logger.debug("Adding {} to the list of user providers", userProvider);
    List<UserProvider> providers = userProviders.get(userProvider.getOrganization());
    if (providers == null) {
      providers = new ArrayList<UserProvider>();
      userProviders.put(userProvider.getOrganization(), providers);
    }
    providers.add(userProvider);
  }

  /**
   * Remove a user provider.
   * 
   * @param userProvider
   *          the user provider to remove
   */
  protected void removeUserProvider(UserProvider userProvider) {
    logger.debug("Removing {} from the list of user providers", userProvider);
    List<UserProvider> providers = userProviders.get(userProvider.getOrganization());
    if (providers != null) {
      providers.remove(userProvider);
    }
  }

  /**
   * Adds a role provider.
   * 
   * @param roleProvider
   *          the role provider to add
   */
  protected void addRoleProvider(RoleProvider roleProvider) {
    logger.debug("Adding {} to the list of role providers", roleProvider);
    List<RoleProvider> providers = roleProviders.get(roleProvider.getOrganization());
    if (providers == null) {
      providers = new ArrayList<RoleProvider>();
      roleProviders.put(roleProvider.getOrganization(), providers);
    }
    providers.add(roleProvider);
  }

  /**
   * Remove a role provider.
   * 
   * @param roleProvider
   *          the role provider to remove
   */
  protected void removeRoleProvider(RoleProvider roleProvider) {
    logger.debug("Removing {} from the list of role providers", roleProvider);
    List<RoleProvider> providers = roleProviders.get(roleProvider.getOrganization());
    if (providers != null) {
      providers.remove(roleProvider);
    }
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
    List<RoleProvider> orgRoleProviders = roleProviders.get(org.getId());
    if (orgRoleProviders == null || orgRoleProviders.isEmpty()) {
      throw new IllegalStateException("No role providers for " + org);
    }
    SortedSet<String> roles = new TreeSet<String>();
    for (RoleProvider roleProvider : orgRoleProviders) {
      for (String role : roleProvider.getRoles()) {
        roles.add(role);
      }
    }
    return roles.toArray(new String[roles.size()]);
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
    List<UserProvider> orgUserProviders = userProviders.get(org.getId());
    if (orgUserProviders == null || orgUserProviders.isEmpty()) {
      throw new IllegalStateException("No user providers for " + org);
    }
    // Collect all of the roles known from each of the user providers for this user
    User user = null;
    for (UserProvider userProvider : orgUserProviders) {
      User providerUser = userProvider.loadUser(userName);
      if (providerUser == null) {
        continue;
      }
      if (user == null) {
        user = providerUser;
      } else {
        user = mergeUsers(user, providerUser);
      }
    }
    return user;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
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
   * Merges two representations of a user, as returned by two different user providers. The set or roles from the
   * provided users will be merged into one set.
   * 
   * @param user1
   *          the first user to merge
   * @param user2
   *          the second user to merge
   * @return a user with a merged set of roles
   */
  protected User mergeUsers(User user1, User user2) {
    String[] roles1 = user1.getRoles();
    String[] roles2 = user2.getRoles();

    String[] roles = new String[(roles1.length + roles2.length)];
    for (int i = 0; i < roles.length; i++) {
      roles[i] = i < roles1.length ? roles1[i] : roles2[i - roles1.length];
    }
    String userName = user1.getUserName();
    String organization = user1.getOrganization();
    String password = user1.getPassword() == null ? user2.getPassword() : null;
    return new User(userName, password, organization, roles);
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
    List<RoleProvider> orgRoleProviders = roleProviders.get(org.getId());
    if (orgRoleProviders == null || orgRoleProviders.isEmpty()) {
      throw new IllegalStateException("No role providers for " + org);
    }
    for (RoleProvider roleProvider : orgRoleProviders) {
      String localRole = roleProvider.getLocalRole(role);
      if (localRole != null) {
        return localRole;
      }
    }
    throw new NotFoundException("No role '" + role + "' defined for organization '" + org.getId() + "'");
  }

}
