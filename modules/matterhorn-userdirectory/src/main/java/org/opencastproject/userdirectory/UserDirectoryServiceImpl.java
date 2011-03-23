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

import static org.opencastproject.security.api.SecurityService.ANONYMOUS_USER;

import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.api.UserProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * COMMENT ME
 * 
 */
public class UserDirectoryServiceImpl implements UserDirectoryService, UserDetailsService, RoleDirectoryService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserDirectoryServiceImpl.class);

  /** The list of user providers */
  protected List<UserProvider> userProviders = new ArrayList<UserProvider>();

  /** The list of role providers */
  protected List<RoleProvider> roleProviders = new ArrayList<RoleProvider>();

  /**
   * Adds a user provider.
   * 
   * @param userProvider
   *          the user provider to add
   */
  protected void addUserProvider(UserProvider userProvider) {
    logger.info("Adding {} to the list of user providers", userProvider);
    this.userProviders.add(userProvider);
  }

  /**
   * Remove a user provider.
   * 
   * @param userProvider
   *          the user provider to remove
   */
  protected void removeUserProvider(UserProvider userProvider) {
    logger.info("Removing {} from the list of user providers", userProvider);
    this.userProviders.remove(userProvider);
  }

  /**
   * Adds a role provider.
   * 
   * @param roleProvider
   *          the role provider to add
   */
  protected void addRoleProvider(RoleProvider roleProvider) {
    logger.info("Adding {} to the list of role providers", roleProvider);
    this.roleProviders.add(roleProvider);
  }

  /**
   * Remove a role provider.
   * 
   * @param roleProvider
   *          the role provider to remove
   */
  protected void removeRoleProvider(RoleProvider roleProvider) {
    logger.info("Removing {} from the list of role providers", roleProvider);
    this.roleProviders.remove(roleProvider);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public String[] getRoles() {
    SortedSet<String> roles = new TreeSet<String>();
    for (RoleProvider provider : roleProviders) {
      roles.addAll(Arrays.asList(provider.getRoles()));
    }
    return roles.toArray(new String[roles.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserDirectoryService#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    for (UserProvider provider : userProviders) {
      User user = provider.loadUser(userName);
      if (user != null)
        return user;
    }
    return ANONYMOUS_USER;
  }

  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException,
          org.springframework.dao.DataAccessException {
    User user = loadUser(userName);
    if (user.equals(ANONYMOUS_USER)) {
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
  };

}
