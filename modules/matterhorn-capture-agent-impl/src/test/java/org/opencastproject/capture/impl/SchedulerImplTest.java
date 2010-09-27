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
import org.opencastproject.capture.api.ScheduledEvent;

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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class SchedulerImplTest {

  SchedulerImpl schedulerImpl = null;
  ConfigurationManager configurationManager = null;
  Properties schedulerProperties = null;
  CaptureAgentImpl captureAgentImpl = null;

  @Before
  public void setup() {
    Properties properties = setupCaptureProperties();
    setupConfigurationManager(properties);
    setupCaptureAgentImpl();
    setupSchedulerProperties();
    setupSchedulerImpl();
    Assert.assertNull(schedulerImpl.getCaptureSchedule());
  }

  private Properties setupCaptureProperties() {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (inputStream == null) {
      throw new RuntimeException("Unable to load configuration file for capture!");
    }

    Properties properties = new Properties();
    try {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for capture!");
    }
    return properties;
  }

  private void setupConfigurationManager(Properties properties) {
    configurationManager = new ConfigurationManager();
    configurationManager.merge(properties, true);
    // We need this line so we can load the schedules correctly
    // If we don't have it then the locations get turned into /tmp/valid-whatever/demo_capture_agent
    configurationManager.setItem(CaptureParameters.AGENT_NAME, "");
    configurationManager.setItem("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"),
            "capture-sched-test").getAbsolutePath());
    configurationManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
  }

  private void setupCaptureAgentImpl() {
    captureAgentImpl = new CaptureAgentImpl();
  }

  private void setupSchedulerProperties() {
    InputStream inputStream;
    schedulerProperties = new Properties();

    inputStream = getClass().getClassLoader().getResourceAsStream("config/scheduler.properties");
    if (inputStream == null) {
      throw new RuntimeException("Unable to load configuration file for scheduler!");
    }

    try {
      schedulerProperties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for scheduler!");
    }
  }

  private void setupSchedulerImpl() {
    schedulerImpl = new SchedulerImpl();
    schedulerImpl.setConfigService(configurationManager);
    schedulerImpl.setCaptureAgent(captureAgentImpl);
  }

  @AfterClass
  public static void afterClass() {
    FileUtils.deleteQuietly(new File(System.getProperty("java.io.tmpdir"), "capture-sched-test"));
  }

  @After
  public void tearDown() {
    schedulerImpl.deactivate();
    schedulerImpl.finalize();
    schedulerImpl = null;
    configurationManager = null;
    schedulerProperties = null;
  }

  private String[] formatDate(Date d) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    String[] times = new String[5];
    times[0] = sdf.format(d);
    d.setTime(d.getTime() + 60000L);
    times[1] = sdf.format(d);
    d.setTime(d.getTime() + 60000L);
    times[2] = sdf.format(d);
    d.setTime(d.getTime() + 60000L);
    times[3] = sdf.format(d);
    d.setTime(new Date().getTime() - 60000L);
    times[4] = sdf.format(d);
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

    //TODO:  Fix this
    source = source.replace("@START@", times[0]);
    source = source.replace("@END@", times[1]);
    source = source.replace("@START2@", times[1]);
    source = source.replace("@END2@", times[2]);
    source = source.replace("@START3@", times[2]);
    source = source.replace("@END3@", times[3]);
    source = source.replace("@PAST@", times[4]);
    
    File output = File.createTempFile("scheduler-test-", ".ics");
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
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    testfile.delete();
  }

  @Test
  public void testValidLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    testfile.delete();
  }

  @Test
  public void testBrokenCalendarURLs() throws IOException, ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "foobar:?8785346");
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void test0LengthRemoteUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test
  public void test0LengthLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
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
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
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
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test @Ignore
  public void testValidRemoteUTF16Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast-UTF16.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test @Ignore
  public void testValidLocalUTF16Calendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast-UTF16.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals(times[0], schedule[0]);
    testfile.delete();
  }

  @Test
  public void testBlankRemoteCalendar() throws ConfigurationException {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, cachedBlank);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testBlankLocalCalendar() throws ConfigurationException {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, cachedBlank);
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedRemoteURLCalendar() throws ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedLocalURLCalendar() throws ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedCalendars() throws ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantRemoteCalendar() throws ConfigurationException {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, nonExistant);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantLocalCalendar() throws ConfigurationException {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, nonExistant);
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }
  
  @Test
  public void testGarbageRemoteCalendar() throws ConfigurationException {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, garbage);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testGarbageLocalCalendar() throws ConfigurationException {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, garbage);
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testValidRemoteDuplicateCalendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast-with-dups.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl.updated(schedulerProperties);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("one", schedule[0]);
    testfile.delete();
  }

  @Test
  public void testValidLocalDuplicateCalendar() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast-with-dups.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("one", schedule[0]);
    testfile.delete();
  }

  @Test
  public void testEdgecases() throws IOException, ConfigurationException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Edge-Cases.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl.updated(schedulerProperties);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(4, schedule.length);
    Arrays.sort(schedule);
    Assert.assertEquals("Longer-than-max-capture-time-using-DTEND", schedule[0]);
    Assert.assertEquals("Longer-than-max-capture-time-using-duration", schedule[1]);
    Assert.assertEquals("No-attachments", schedule[2]);
    Assert.assertEquals("No-end-but-duration", schedule[3]);
    testfile.delete();
  }

  @Test
  public void testEndpoint() throws MalformedURLException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "http://www.example.com");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    URL test = new URL("http://www.example.com");
    schedulerImpl.setScheduleEndpoint(test);
    Assert.assertEquals(test, schedulerImpl.getScheduleEndpoint());
  }

  @Test
  public void testBrokenScheduler() {
    //TODO:  Fix this
    boolean expectedException = false;

    //Try it
    try {
      schedulerImpl.updated(null);
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
      schedulerImpl.updated(new Properties());
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
    schedulerImpl.updated(schedulerProperties);
    Thread.sleep(10);
    Assert.assertTrue(schedulerImpl.isPollingEnabled());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "0");
    schedulerImpl.updated(schedulerProperties);
    Assert.assertFalse(schedulerImpl.isPollingEnabled());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "?asewrtk5fw5");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "60");
    schedulerImpl.updated(schedulerProperties);
    Assert.assertFalse(schedulerImpl.isPollingEnabled());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "0");
    schedulerImpl.updated(schedulerProperties);
    Assert.assertFalse(schedulerImpl.isPollingEnabled());
  }
  
  @Test
  public void scheduleRendersCaptureTimesCorrectly() throws IOException, ConfigurationException, InterruptedException {
    Calendar start = Calendar.getInstance();
    int firstMinuteOffset = -20;
    int secondMinuteOffset = 10;
    int thirdMinuteOffset = 20;
    setupThreeCaptureCalendar(firstMinuteOffset, secondMinuteOffset, thirdMinuteOffset);
    Thread.sleep(10);
    List<ScheduledEvent> events = schedulerImpl.getSchedule();
    Assert.assertTrue("There should be some events in the schedule.", events.size() > 0);
    for (ScheduledEvent scheduleEvent : events) {
      if (scheduleEvent.getTitle().equalsIgnoreCase("1st-Capture")) {
        checkTime(start, firstMinuteOffset, scheduleEvent);
      } else if (scheduleEvent.getTitle().equalsIgnoreCase("2nd-Capture")) {
        checkTime(start, secondMinuteOffset, scheduleEvent);
      } else if (scheduleEvent.getTitle().equalsIgnoreCase("3rd-Capture")) {
        checkTime(start, thirdMinuteOffset, scheduleEvent);
      }
    }
  }

  private void checkTime(Calendar start, int offset, ScheduledEvent scheduleEvent) {
    Calendar before = Calendar.getInstance();
    before.setTime(start.getTime());
    Calendar after = Calendar.getInstance();
    after.setTime(start.getTime());
    // Create a calendar event a second before the time we expect.
    before.set(Calendar.MINUTE, before.get(Calendar.MINUTE) + offset);
    before.set(Calendar.SECOND, before.get(Calendar.SECOND) - 1);
    // Create a calendar event a second after the time we expect.
    after.set(Calendar.MINUTE, after.get(Calendar.MINUTE) + offset);
    after.set(Calendar.SECOND, after.get(Calendar.SECOND) + 1);

    Assert.assertTrue("getSchedule() returned " + new Date(scheduleEvent.getStartTime()).toString()
            + " as a start time for the event when it should have been " + offset
            + " minutes after right now and 1 second after " + before.getTime().toString(), before.getTime().before(
            new Date(scheduleEvent.getStartTime())));
    Assert.assertTrue("getSchedule() returned " + new Date(scheduleEvent.getStartTime()).toString()
            + " as a start time for the event when it should have been " + offset
            + " minutes before right now and 1 second before " + before.getTime().toString(), after.getTime().after(
            new Date(scheduleEvent.getStartTime())));
  }

  private void setupThreeCaptureCalendar(int firstMinuteOffset, int secondMinuteOffset, int thirdMinuteOffset)
          throws IOException, ConfigurationException {
    String[] times = createThreeCaptures(firstMinuteOffset, secondMinuteOffset, thirdMinuteOffset);
    File testfile = setupCaptureAgentTestCalendar("calendars/ThreeCaptures.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl.updated(schedulerProperties);
    captureAgentImpl.setConfigService(configurationManager);
    schedulerImpl.setConfigService(configurationManager);
  }

  private String[] createThreeCaptures(int firstMinuteOffset, int secondMinuteOffset, int thirdMinuteOffset) {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    String[] times = new String[3];
    /* Setup first calendar value */
    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + firstMinuteOffset);
    times[0] = sdf.format(calendar.getTime());
    /* Setup second calendar value. */
    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - firstMinuteOffset);
    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + secondMinuteOffset);
    times[1] = sdf.format(calendar.getTime());
    /* Setup third calendar value. */
    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - secondMinuteOffset);
    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + thirdMinuteOffset);
    times[2] = sdf.format(calendar.getTime());
    return times;
  }

  private File setupCaptureAgentTestCalendar(String calLocation, String[] times) throws IOException {
    String source = readFile(this.getClass().getClassLoader().getResource(calLocation));

    source = source.replace("@START1@", times[0]);
    source = source.replace("@START2@", times[1]);
    source = source.replace("@START3@", times[2]);

    File output = File.createTempFile("capture-scheduler-test-", ".ics");
    FileWriter out = null;
    out = new FileWriter(output);
    out.write(source);
    out.close();

    return output;
  }
}
