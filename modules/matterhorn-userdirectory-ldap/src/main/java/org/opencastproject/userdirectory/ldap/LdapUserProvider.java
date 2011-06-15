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

import org.opencastproject.security.api.CachingUserProviderMXBean;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Dictionary;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * LDAP implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
public class LdapUserProvider implements UserProvider, ManagedService, CachingUserProviderMXBean {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LdapUserProvider.class);

  /** The key to look up the ldap search filter in the service configuration properties */
  private static final String SEARCH_FILTER_KEY = "org.opencastproject.userdirectory.ldap.searchfilter";

  /** The key to look up the ldap search base in the service configuration properties */
  private static final String SEARCH_BASE_KEY = "org.opencastproject.userdirectory.ldap.searchbase";

  /** The key to look up the ldap server URL in the service configuration properties */
  private static final String LDAP_URL_KEY = "org.opencastproject.userdirectory.ldap.url";

  /** The key to look up the role attributes in the service configuration properties */
  private static final String ROLE_ATTRIBUTES_KEY = "org.opencastproject.userdirectory.ldap.roleattributes";

  /** The key to look up the organization identifer in the service configuration properties */
  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.ldap.org";

  /** The key to look up the user DN to use for performing searches. */
  private static final String SEARCH_USER_DN = "org.opencastproject.userdirectory.ldap.userDn";

  /** The key to look up the password to use for performing searches */
  private static final String SEARCH_PASSWORD = "org.opencastproject.userdirectory.ldap.password";

  /** The key to look up the number of user records to cache */
  private static final String CACHE_SIZE = "org.opencastproject.userdirectory.ldap.cache.size";

  /** The key to look up the number of minutes to cache users */
  private static final String CACHE_EXPIRATION = "org.opencastproject.userdirectory.ldap.cache.expiration";

  /** The spring ldap userdetails service delegate */
  private LdapUserDetailsService delegate = null;

  /** The organization id */
  private String organization = null;

  /** The LDAP search base */
  private String searchBase = null;

  /** The LDAP search filter */
  private String searchFilter = null;

  /** The LDAP server URL */
  private String url = null;

  /** The user DN for authentication */
  private String userDn = null;

  /** The password for authentication */
  private String password = null;

  /** The role attributes from LDAP */
  private String roleAttributesGlob = null;

  /** The size of the user cache */
  private int cacheSize = -1;

  /** The number of minutes until cached users are removed */
  private long cacheExpiration = -1;

  /** Total number of requests made to load users */
  private AtomicLong requests = null;

  /** The number of requests made to ldap */
  private AtomicLong ldapLoads = null;

  /** A cache of users, which lightens the load on the LDAP server */
  private ConcurrentMap<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /**
   * Connect to LDAP
   */
  private void connect() {
    DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
    if (StringUtils.isNotBlank(userDn)) {
      contextSource.setPassword(password);
      contextSource.setUserDn(userDn);
    }
    contextSource.setAnonymousReadOnly(true);
    try {
      contextSource.afterPropertiesSet();
    } catch (Exception e) {
      throw new org.opencastproject.util.ConfigurationException("Unable to create a spring context source", e);
    }
    FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(searchBase, searchFilter, contextSource);
    this.delegate = new LdapUserDetailsService(userSearch);

    if (StringUtils.isNotBlank(roleAttributesGlob)) {
      LdapUserDetailsMapper mapper = new LdapUserDetailsMapper();
      mapper.setRoleAttributes(roleAttributesGlob.split(","));
      this.delegate.setUserDetailsMapper(mapper);
    }

    // Setup the caches
    cache = new MapMaker().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .makeComputingMap(new Function<String, Object>() {
              public Object apply(String id) {
                User user = loadUserFromLdap(id);
                return user == null ? nullToken : user;
              }
            });

    registerMBean();
  }

  /**
   * Registers an MXBean.
   */
  private void registerMBean() {
    // register with jmx
    requests = new AtomicLong();
    ldapLoads = new AtomicLong();
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName name;
      name = new ObjectName(getClass().getName() + "." + organization + ":type=LDAPRequests");
      Object mbean = this;
      try {
        mbs.unregisterMBean(name);
      } catch (InstanceNotFoundException e) {
        logger.debug(name + " was not registered");
      }
      mbs.registerMBean(mbean, name);
    } catch (Exception e) {
      logger.warn("Unable to register {} as an mbean: {}", this, e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    requests.incrementAndGet();
    try {
      Object user = cache.get(userName);
      if (user == nullToken) {
        return null;
      } else {
        return (User) user;
      }
    } catch (NullPointerException e) {
      logger.debug("This map throws NPE rather than returning null.  Swallowing that exception here.");
      return null;
    }
  }

  /**
   * Loads a user from LDAP.
   * 
   * @param userName
   *          the username
   * @return the user
   */
  protected User loadUserFromLdap(String userName) {
    if (delegate == null || cache == null) {
      throw new IllegalStateException("The LDAP user detail service has not yet been configured");
    }
    ldapLoads.incrementAndGet();
    UserDetails userDetails = null;

    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassloader = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(LdapUserProvider.class.getClassLoader());
      try {
        userDetails = delegate.loadUserByUsername(userName);
      } catch (UsernameNotFoundException e) {
        cache.put(userName, nullToken);
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
      return new User(userDetails.getUsername(), getOrganization(), roles);
    } finally {
      currentThread.setContextClassLoader(originalClassloader);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return organization;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(@SuppressWarnings("unchecked") Dictionary properties) throws ConfigurationException {
    organization = (String) properties.get(ORGANIZATION_KEY);
    if (StringUtils.isBlank(organization))
      throw new ConfigurationException(ORGANIZATION_KEY, "is not set");
    searchBase = (String) properties.get(SEARCH_BASE_KEY);
    if (StringUtils.isBlank(searchBase))
      throw new ConfigurationException(SEARCH_BASE_KEY, "is not set");
    searchFilter = (String) properties.get(SEARCH_FILTER_KEY);
    if (StringUtils.isBlank(searchFilter))
      throw new ConfigurationException(SEARCH_FILTER_KEY, "is not set");
    url = (String) properties.get(LDAP_URL_KEY);
    if (StringUtils.isBlank(url))
      throw new ConfigurationException(LDAP_URL_KEY, "is not set");
    userDn = (String) properties.get(SEARCH_USER_DN);
    password = (String) properties.get(SEARCH_PASSWORD);
    roleAttributesGlob = (String) properties.get(ROLE_ATTRIBUTES_KEY);

    cacheSize = 1000;
    String configuredCacheSize = (String) properties.get(CACHE_SIZE);
    if (configuredCacheSize != null) {
      cacheSize = Integer.parseInt(configuredCacheSize); // just throw the format exception
    }

    cacheExpiration = 1;
    String configuredCacheExpiration = (String) properties.get(CACHE_EXPIRATION);
    if (configuredCacheExpiration != null) {
      cacheExpiration = Long.parseLong(configuredCacheExpiration); // just throw the format exception
    }

    // Now that we have everything we need, go ahead and activate
    connect();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.CachingUserProviderMXBean#getCacheHitRatio()
   */
  public float getCacheHitRatio() {
    if (requests.get() == 0) {
      return 0;
    }
    return (float) (requests.get() - ldapLoads.get()) / requests.get();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return LdapUserProvider.class.getName();
  }

}
