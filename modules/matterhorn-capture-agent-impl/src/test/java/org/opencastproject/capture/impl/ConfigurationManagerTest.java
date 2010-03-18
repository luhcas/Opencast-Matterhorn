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
package org.opencastproject.capture.impl;


import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
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
    Assert.assertNotNull(configManager);

    //Checks on the basic operations before updated() has been called
    Assert.assertNull(configManager.getItem("nothing"));
    Assert.assertNull(configManager.getItem(null));
    configManager.setItem("anything", "nothing");
    Assert.assertEquals("nothing", configManager.getItem("anything"));
    configManager.setItem(null, "this should work, but not put anything in the props");
    Assert.assertEquals(1, configManager.getAllProperties().size());

    configManager.updated(null);
  }

  @After
  public void tearDown() {
    configManager.deactivate();
    configManager = null;
  }
  
  @Test
  public void testMerge() {
    //Setup the basic properties
    configManager.setItem("test", "foo");
    configManager.setItem("unchanged", "bar");
    Assert.assertEquals(configManager.getItem("test"), "foo");
    Assert.assertEquals(configManager.getItem("unchanged"), "bar");

    //Setup the additions
    Properties p = new Properties();
    p.setProperty("test","value");

    //Do some idiot checks to make sure that trying to merge a null properties object does nothing
    Properties defaults = configManager.getAllProperties();
    configManager.merge(null, true);
    Assert.assertEquals(defaults, configManager.getAllProperties());
    configManager.merge(null, false);
    Assert.assertEquals(defaults, configManager.getAllProperties());

    //Now test a basic merge
    Properties t = configManager.merge(p, false);
    Assert.assertEquals(t.getProperty("test"), "value");
    Assert.assertEquals(t.getProperty("unchanged"), "bar");
    t = null;

    //Now overwrite the system settings
    t = configManager.merge(p, true);
    Assert.assertEquals(t.getProperty("test"), "value");
    Assert.assertEquals(t.getProperty("unchanged"), "bar");
    Assert.assertEquals(configManager.getItem("test"), "value");
    Assert.assertEquals(configManager.getItem("unchanged"), "bar");
  }
  
  @Test
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

  @Test
  public void testUpdate() throws IOException, ConfigurationException {
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    configManager.updated(sourceProps);

    Properties configProps = configManager.getAllProperties();
    for (Object key : sourceProps.keySet()) {
      if (!configProps.containsKey(key)) {
        Assert.fail();
      }
    }
  }

  @Test
  public void testCapabilities() throws IOException, ConfigurationException {
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    configManager.updated(sourceProps);

    Properties caps = configManager.getCapabilities();
    Assert.assertEquals("screen.mpg", caps.get("SCREEN"));
    Assert.assertEquals("camera.mpg", caps.get("PRESENTER"));
    Assert.assertEquals("audio.mp3", caps.get("MICROPHONE"));
  }

  @Test
  public void testBrokenCapabilities() throws IOException, ConfigurationException {
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    sourceProps.remove("capture.device.PRESENTER.src");
    sourceProps.remove("capture.device.PRESENTER.outputfile");
    configManager.updated(sourceProps);

    Assert.assertNull(configManager.getCapabilities());
  }
}
