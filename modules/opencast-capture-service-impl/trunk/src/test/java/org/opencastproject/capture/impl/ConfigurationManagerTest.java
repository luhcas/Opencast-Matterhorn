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
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Properties;

/**
 * Test the functionality of the ConfigurationManager
 */
public class ConfigurationManagerTest {
  
  /** the singleton object to test with */
  private ConfigurationManager configManager;

  @Before
  public void setUp() throws ConfigurationException {
    configManager = new ConfigurationManager();
    configManager.activate(null);
    Assert.assertNotNull(configManager);
  }

  @After
  public void tearDown() {
    configManager = null;
  }
  
  @Test @Ignore
  public void testMerge() {
    
  }
  
  @Test @Ignore
  public void testGetAllProperties() {
    Properties properties;
    
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
