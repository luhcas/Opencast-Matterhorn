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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class SchedulerImplTest {

  SchedulerImpl sched = null;
  ConfigurationManager config = null;
  Properties schedulerProps = null;

  @AfterClass
  public static void afterclass() {
    FileUtils.deleteQuietly(new File(System.getProperty("java.io.tmpdir"), "capture-sched-test"));
  }

  @Before
  public void setUp() throws ConfigurationException {
    config = new ConfigurationManager();
    InputStream s = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file for capture!");
    }

    Properties p = new Properties();
    try {
      p.load(s);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for capture!");
    }
    config.merge(p, true);
    //We need this line so we can load the schedules correctly
    //If we don't have it then the locations get turned into /tmp/valid-whatever/demo_capture_agent
    config.setItem(CaptureParameters.AGENT_NAME, "");
    config.setItem("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "capture-sched-test").getAbsolutePath());
    config.setItem("org.opencastproject.server.url", "http://localhost:8080");

    sched = new SchedulerImpl();
    sched.setConfigService(config);
    schedulerProps = new Properties();
    s = getClass().getClassLoader().getResourceAsStream("config/scheduler.properties");
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file for scheduler!");
    }

    try {
      schedulerProps.load(s);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for scheduler!");
    }
  }

  @After
  public void tearDown() {
    sched.unsetCaptureAgent();
    sched.unsetConfigService();
    sched.deactivate();
    sched = null;
    config = null;
    schedulerProps = null;
  }

  private String[] formatDate(Date d) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    String[] times = new String[2];
    times[0] = sdf.format(d);
    d.setTime(d.getTime() + 60000L);
    times[1] = sdf.format(d);
    return times;
  }

  private String readFile(URL target) throws IOException {
    StringBuilder sb = new StringBuilder();
    DataInputStream in = new DataInputStream(target.openStream());
    int c = 0;
    while ((c = in.read()) != -1) {
      sb.append((char) c);
    }
    return sb.toString();
  }

  private File setupTestCalendar(String calLocation, String[] times) throws IOException {
    String source = readFile(this.getClass().getClassLoader().getResource(calLocation));

    source = source.replace("@START@", times[0]);
    source = source.replace("@END@", times[1]);
    
    File output = File.createTempFile("valid", ".ics");
    FileWriter out = null;
    out = new FileWriter(output);
    out.write(source);
    out.close();

    return output;
  }

  @Test
  public void testValidRemoteUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    testfile.delete();
  }

  @Test
  public void testValidLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    testfile.delete();
  }

  @Test
  public void test0LengthRemoteUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test
  public void test0LengthLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test
  public void testNegativeLengthRemoteUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    String temp = times[0];
    times[0] = times[1];
    times[1] = temp;
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test
  public void testNegativeLengthLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    String temp = times[0];
    times[0] = times[1];
    times[1] = temp;
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test @Ignore
  public void testValidRemoteUTF16Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast-UTF16.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test @Ignore
  public void testValidLocalUTF16Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast-UTF16.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals(times[0], schedule[0]);
    testfile.delete();
  }

  @Test
  public void testBlankRemoteCalendar() throws ConfigurationException {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, cachedBlank);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testBlankLocalCalendar() throws ConfigurationException {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, cachedBlank);
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedRemoteURLCalendar() throws ConfigurationException {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedLocalURLCalendar() throws ConfigurationException {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedCalendars() throws ConfigurationException {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantRemoteCalendar() throws ConfigurationException {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, nonExistant);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantLocalCalendar() throws ConfigurationException {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, nonExistant);
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }
  
  @Test
  public void testGarbageRemoteCalendar() throws ConfigurationException {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, garbage);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testGarbageLocalCalendar() throws ConfigurationException {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, garbage);
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testValidRemoteDuplicateCalendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast-with-dups.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.updated(schedulerProps);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    testfile.delete();
  }

  @Test
  public void testValidLocalDuplicateCalendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast-with-dups.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.updated(schedulerProps);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    testfile.delete();
  }

  @Test
  public void testEndpoint() throws MalformedURLException {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "http://www.example.com");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    URL test = new URL("http://www.example.com");
    sched.setScheduleEndpoint(test);
    Assert.assertEquals(test, sched.getScheduleEndpoint());
  }

  @Test
  public void testBrokenScheduler() {
    boolean expectedException = false;

    //Try it
    try {
      sched.updated(null);
    } catch (ConfigurationException e) {
      expectedException = true;
    }

    //Check it
    if (!expectedException) {
      Assert.fail();
    } else {
      expectedException = false;
    }

    //Try it with a different config
    try {
      sched.updated(new Properties());
    } catch (ConfigurationException e) {
      expectedException = true;
    }

    //Check it again
    if (!expectedException) {
      Assert.fail();
    } else {
      expectedException = false;
    }
  }

  @Test
  public void testBadPolling() throws Exception {
    sched.updated(schedulerProps);
    Thread.sleep(10);
    Assert.assertTrue(sched.isPollingEnabled());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "0");
    sched.updated(schedulerProps);
    Assert.assertFalse(sched.isPollingEnabled());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "?asewrtk5fw5");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "60");
    sched.updated(schedulerProps);
    Assert.assertFalse(sched.isPollingEnabled());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "0");
    sched.updated(schedulerProps);
    Assert.assertFalse(sched.isPollingEnabled());
  }
}
