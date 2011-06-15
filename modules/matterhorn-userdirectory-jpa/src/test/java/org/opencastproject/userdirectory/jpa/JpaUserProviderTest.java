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
package org.opencastproject.userdirectory.jpa;

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JpaUserProviderTest {

  private ComboPooledDataSource pooledDataSource = null;
  private JpaUserAndRoleProvider provider = null;

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

    provider = new JpaUserAndRoleProvider();
    provider.setPersistenceProperties(props);
    provider.setPersistenceProvider(new PersistenceProvider());
    provider.activate(null);

  }

  @After
  public void tearDown() throws Exception {
    pooledDataSource.close();
  }

  @Test
  public void testLoadAndGetUser() throws Exception {
    Set<String> authorities = new HashSet<String>();
    authorities.add("ROLE_ASTRO_101_SPRING_2011_STUDENT");
    JpaUser user = new JpaUser("user1", "pass1", DEFAULT_ORGANIZATION_ID, authorities);
    provider.addUser(user);
    Assert.assertNotNull(provider.loadUser("user1"));
    Assert.assertNull("Loading 'does not exist' should return null", provider.loadUser("does not exist"));
  }

  @Test
  public void testRoles() throws Exception {
    Set<String> authoritiesOne = new HashSet<String>();
    authoritiesOne.add("ROLE_ONE");
    JpaUser userOne = new JpaUser("user1", "pass1", DEFAULT_ORGANIZATION_ID, authoritiesOne);
    provider.addUser(userOne);

    Set<String> authoritiesTwo = new HashSet<String>();
    authoritiesTwo.add("ROLE_ONE");
    authoritiesTwo.add("ROLE_TWO");
    JpaUser userTwo = new JpaUser("user2", "pass2", DEFAULT_ORGANIZATION_ID, authoritiesTwo);
    provider.addUser(userTwo);

    Assert.assertEquals("There should be two roles", 2, provider.getRoles().length);
  }

}
