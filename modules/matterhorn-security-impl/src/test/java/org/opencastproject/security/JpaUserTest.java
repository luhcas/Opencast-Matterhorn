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

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JpaUserTest {

  private ComboPooledDataSource pooledDataSource = null;
  private JpaUserDetailService userDetailService = null;

  @Before
  public void setUp() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    userDetailService = new JpaUserDetailService();
    userDetailService.setPersistenceProperties(props);
    userDetailService.setPersistenceProvider(new PersistenceProvider());
    userDetailService.activate(null);

  }

  @After
  public void tearDown() throws Exception {
    pooledDataSource.close();
  }

  @Test
  public void testLoadAndGetUser() throws Exception {
    Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
    authorities.add(new GrantedAuthorityImpl("ROLE_ASTRO_101_SPRING_2011_STUDENT"));
    JpaUser user = new JpaUser("user1", "pass1", true, true, true, true, authorities);
    userDetailService.addUser(user);
    Assert.assertNotNull(userDetailService.loadUserByUsername("user1"));
    try {
      userDetailService.loadUserByUsername("does not exist");
      Assert.fail("Attempting to load 'does not exist' should throw an exception");
    } catch (UsernameNotFoundException e) {
      // expected
    }
  }
}
