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
package org.opencastproject.userdirectory.ldap;

import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;

import java.util.Collection;
import java.util.Dictionary;

/**
 * LDAP implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
public class LdapUserProvider implements UserProvider, ManagedService {

  /** The key to look up the ldap search filter in the service configuration properties */
  private static final String SEARCH_FILTER_KEY = "org.opencastproject.userdirectory.ldap.searchfilter";

  /** The key to look up the ldap search base in the service configuration properties */
  private static final String SEARCH_BASE_KEY = "org.opencastproject.userdirectory.ldap.searchbase";

  /** The key to look up the ldap server URL in the service configuration properties */
  private static final String LDAP_URL_KEY = "org.opencastproject.userdirectory.ldap.url";

  /** The key to look up the role attributes in the service configuration properties */
  private static final String ROLE_ATTRIBUTES_KEY = "org.opencastproject.userdirectory.ldap.roleattributes";

  /** The spring ldap userdetails service delegate */
  protected LdapUserDetailsService delegate = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    if (delegate == null) {
      throw new IllegalStateException("The LDAP user detail service has not yet been configured");
    }
    UserDetails userDetails = null;
    try {
      userDetails = delegate.loadUserByUsername(userName);
    } catch (UsernameNotFoundException e) {
      return null;
    }
    Collection<GrantedAuthority> authorities = userDetails.getAuthorities();
    String[] roles = null;
    if (authorities != null) {
      int i = 0;
      roles = new String[authorities.size()];
      for (GrantedAuthority authority : authorities) {
        roles[i++] = authority.getAuthority();
      }
    }
    return new User(userDetails.getUsername(), roles);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    String searchBase = (String) properties.get(SEARCH_BASE_KEY);
    String searchFilter = (String) properties.get(SEARCH_FILTER_KEY);
    String url = (String) properties.get(LDAP_URL_KEY);
    DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
    contextSource.setAnonymousReadOnly(true);
    try {
      contextSource.afterPropertiesSet();
    } catch (Exception e) {
      throw new org.opencastproject.util.ConfigurationException("Unable to create a spring context source", e);
    }
    FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(searchBase, searchFilter, contextSource);
    this.delegate = new LdapUserDetailsService(userSearch);

    String roleAttributesGlob = (String) properties.get(ROLE_ATTRIBUTES_KEY);
    if (StringUtils.isNotBlank(roleAttributesGlob)) {
      LdapUserDetailsMapper mapper = new LdapUserDetailsMapper();
      mapper.setRoleAttributes(roleAttributesGlob.split(","));
      this.delegate.setUserDetailsMapper(mapper);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getName();
  }
}
