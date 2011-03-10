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

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * LDAP implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
public class LdapUserDetailService implements UserDetailsService, ManagedService {

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
   * A collection of accounts internal to Matterhorn.
   */
  protected Map<String, UserDetails> internalAccounts = null;
  
  /**
   * Callback to activate the component.
   * 
   * @param cc
   *          the declarative services component context
   */
  protected void activate(ComponentContext cc) {
    internalAccounts = new HashMap<String, UserDetails>();
    String digestUsername = cc.getBundleContext().getProperty("org.opencastproject.security.digest.user");
    String digestUserPass = cc.getBundleContext().getProperty("org.opencastproject.security.digest.pass");
    Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
    authorities.add(new GrantedAuthorityImpl("ROLE_ADMIN"));
    User user = new User(digestUsername, digestUserPass, true, true, true, true, authorities);
    internalAccounts.put(digestUsername, user);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
    if(internalAccounts != null) {
      if(internalAccounts.containsKey(username)) {
        return internalAccounts.get(username);
      }
    }
    if (delegate == null) {
      throw new IllegalStateException("The LDAP user detail service has not yet been configured");
    }
    return delegate.loadUserByUsername(username);
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

}
