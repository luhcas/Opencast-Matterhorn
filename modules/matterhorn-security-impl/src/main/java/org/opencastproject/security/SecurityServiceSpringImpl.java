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
package org.opencastproject.security;

import org.opencastproject.security.api.SecurityService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A Spring Security implementation of {@link SecurityService}.
 */
public class SecurityServiceSpringImpl implements SecurityService {

  @Override
  public String getUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    } else {
      Object principal = auth.getPrincipal();
      if (principal == null) {
        return null;
      }
      if (principal instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) principal;
        return userDetails.getUsername();
      } else {
        return principal.toString();
      }
    }
  }

  @Override
  public String[] getRoles() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ANONYMOUS;
    } else {
      Collection<GrantedAuthority> authorities = auth.getAuthorities();
      if (auth == null || authorities.size() == 0)
        return ANONYMOUS;
      List<String> roles = new ArrayList<String>(authorities.size());
      for (GrantedAuthority ga : authorities) {
        roles.add(ga.getAuthority());
      }
      Collections.sort(roles);
      return roles.toArray(new String[roles.size()]);
    }
  }

}
