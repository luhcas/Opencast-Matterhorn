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

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * A Spring Security implementation of {@link SecurityService}.
 */
public class SecurityServiceSpringImpl implements SecurityService {

  /** Holds delegates users for new threads that have been spawned from authenticated threads */
  private static final ThreadLocal<User> delegatedUserHolder = new ThreadLocal<User>();

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#getUser()
   */
  @Override
  public User getUser() {
    User delegatedUser = delegatedUserHolder.get();
    if(delegatedUser != null) {
      return delegatedUser;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ANONYMOUS_USER;
    } else {
      Object principal = auth.getPrincipal();
      if (principal == null) {
        return ANONYMOUS_USER;
      }
      UserDetails userDetails = (UserDetails) principal;

      String[] roles = null;
      Collection<GrantedAuthority> authorities = auth.getAuthorities();
      if (authorities != null && authorities.size() > 0) {
        roles = new String[authorities.size()];
        int i = 0;
        for (GrantedAuthority ga : authorities) {
          roles[i++] = ga.getAuthority();
        }
      }
      return new User(userDetails.getUsername(), roles);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.SecurityService#setUser(org.opencastproject.security.api.User)
   */
  @Override
  public void setUser(User user) {
    delegatedUserHolder.set(user);
  }

}
