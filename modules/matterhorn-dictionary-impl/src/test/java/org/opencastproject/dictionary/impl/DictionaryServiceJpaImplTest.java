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
package org.opencastproject.dictionary.impl;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryServiceJpaImplTest {
  private ComboPooledDataSource pooledDataSource = null;

  DictionaryServiceJpaImpl service = null;

  @Before
  public void setUp() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis() + ";LOCK_MODE=1;MVCC=TRUE");
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    service = new DictionaryServiceJpaImpl();
    service.setPersistenceProperties(props);
    service.setPersistenceProvider(new PersistenceProvider());
    service.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    service.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testAddAndRetrieveWords() throws Exception {
    service.addWord("foo", "en");
    service.addWord("foo", "de");
    service.addWord("bar", "de");

    List<String> allLanguages = Arrays.asList(service.getLanguages());
    Assert.assertEquals(2, allLanguages.size());
    
    List<String> fooLanguages = Arrays.asList(service.getLanguages("foo"));
    Assert.assertEquals(2, fooLanguages.size());
    Assert.assertTrue(fooLanguages.contains("en"));
    Assert.assertTrue(fooLanguages.contains("de"));

    List<String> barLanguages = Arrays.asList(service.getLanguages("bar"));
    Assert.assertEquals(1, barLanguages.size());
    Assert.assertFalse(barLanguages.contains("en"));
    Assert.assertTrue(barLanguages.contains("de"));

  }
}
