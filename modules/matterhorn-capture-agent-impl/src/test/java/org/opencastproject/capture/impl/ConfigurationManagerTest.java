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


import org.opencastproject.capture.api.CaptureParameters;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Test the functionality of the ConfigurationManager
 */
public class ConfigurationManagerTest {
  
  /** the singleton object to test with */
  private ConfigurationManager configManager;

  @AfterClass
  public static void after() {
    FileUtils.deleteQuietly(new File(System.getProperty("java.io.tmpdir"), "configman-test"));
  }

  @Before
  public void setUp() throws ConfigurationException, IOException {
    configManager = new ConfigurationManager();
    Assert.assertNotNull(configManager);
    configManager.activate(null);

    //Checks on the basic operations before updated() has been called
    Assert.assertNull(configManager.getItem("nothing"));
    Assert.assertNull(configManager.getItem(null));
    configManager.setItem("anything", "nothing");
    Assert.assertEquals("nothing", configManager.getItem("anything"));
    configManager.setItem(null, "this should work, but not put anything in the props");
    Assert.assertEquals(1, configManager.getAllProperties().size());

    Properties p = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    p.load(is);
    IOUtils.closeQuietly(is);
    p.put("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "configman-test").getAbsolutePath());

    configManager.updated(p);
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

    configManager.setItem("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "configman-test").getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
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

    configManager.setItem("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "configman-test").getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configManager.updated(sourceProps);
    
    Properties caps = configManager.getCapabilities();
    Assert.assertNotNull(caps);
    assertCaps(caps, "MOCK_SCREEN", "M2_REPO", "/org/opencastproject/samples/screen/1.0/screen-1.0.mpg", "screen_out.mpg", "presentation/source");
    assertCaps(caps, "MOCK_PRESENTER", "M2_REPO", "/org/opencastproject/samples/camera/1.0/camera-1.0.mpg", "camera_out.mpg", "presentation/source");
    assertCaps(caps, "MOCK_MICROPHONE", "M2_REPO", "/org/opencastproject/samples/audio/1.0/audio-1.0.mp3", "audio_out.mp3", "presentation/source");
  }

  private void assertCaps(Properties caps, String name, String baseVar, String relPath, String dest, String flavour) {
    Assert.assertEquals("${"+baseVar+"}"+relPath, configManager.getUninterpretedItem(CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_SOURCE));
    Assert.assertEquals(configManager.getVariable(baseVar) + relPath, caps.get(CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_SOURCE));
    Assert.assertEquals(dest, caps.get(CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_DEST));
    Assert.assertEquals(flavour, caps.get(CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_FLAVOR));
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

    sourceProps.remove("capture.device.MOCK_PRESENTER.src");
    sourceProps.remove("capture.device.MOCK_PRESENTER.outputfile");
    configManager.setItem("capture.device.MOCK_PRESENTER.src", null);
    configManager.setItem("capture.device.MOCK_PRESENTER.outputfile", null);
    configManager.setItem("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "configman-test").getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configManager.updated(sourceProps);

    Assert.assertNull(configManager.getCapabilities());
  }
}
