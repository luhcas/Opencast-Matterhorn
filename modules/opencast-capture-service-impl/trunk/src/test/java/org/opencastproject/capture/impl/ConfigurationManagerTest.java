/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.capture.impl;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Test the functionality of the ConfigurationManager
 */
public class ConfigurationManagerTest {
  
  /** the singleton object to test with */
  private ConfigurationManager configManager;

  @Before
  public void setUp() {
    configManager = ConfigurationManager.getInstance();
    Assert.assertNotNull(configManager);
  }

  @After
  public void tearDown() {
    configManager = null;
  }
  
  @Test
  public void testMerge() {
    
    Dictionary<String, String> table1 = new Hashtable<String, String>();
    Dictionary<String, String> table2 = new Hashtable<String, String>();
    Dictionary<String, String> combined;
    
    table1.put("a", "1");
    table2.put("b", "2");
    combined = configManager.merge(table1, table2);
    Assert.assertEquals(2, combined.size());
    
    table2.put("c", "3");
    combined = configManager.merge(table1, table2);
    Assert.assertEquals(3, combined.size());
    Assert.assertEquals("3", combined.get("c"));
    
    table1.put("b", "2");
    table2.put("b", "4");
    combined = configManager.merge(table1, table2);
    Assert.assertEquals(3, combined.size());
    Assert.assertEquals("2", combined.get("b"));
    
  }
  
  @Test
  public void testGetAllProperties() {
    Dictionary<String, String> properties;
    
    configManager.setItem("a", "1");
    configManager.setItem("b", "2");
    configManager.setItem("c", "3");
    
    properties = configManager.getAllProperties();
    Assert.assertEquals("1", properties.get("a"));
    Assert.assertEquals("2", properties.get("b"));
    Assert.assertEquals("3", properties.get("c"));
    
  }
  
  /* Tests will need to be implemented once the environment is setup properly
   * otherwise we will not have local or centralised configuration files that
   * are essential for the functionality of the ConfigurationManager class
   */

}
