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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

public class SchedulerImplTest {

  SchedulerImpl sched = null;
  ConfigurationManager config = null;

  @Before
  public void setUp() throws ConfigurationException {
    config = new ConfigurationManager();
    config.activate(null);
    config.setItem(CaptureParameters.AGENT_NAME, "");
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, System.getProperty("java.io.tmpdir"));
    sched = new SchedulerImpl();
    sched.setConfigService(config);
  }

  @After
  public void tearDown() {
    sched.shutdown();
    sched = null;
    config = null;
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
  public void testValidRemoteUTF8Calendar() throws IOException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.activate(null);
    //TODO:  Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals(times[0], schedule[0]);
    testfile.delete();
  }

  @Test
  public void testValidLocalUTF8Calendar() throws IOException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals(times[0], schedule[0]);
    testfile.delete();
  }

  @Test
  public void test0LengthRemoteUTF8Calendar() throws IOException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.activate(null);
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
  public void test0LengthLocalUTF8Calendar() throws IOException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

  @Test
  public void testNegativeLengthRemoteUTF8Calendar() throws IOException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    String temp = times[0];
    times[0] = times[1];
    times[1] = temp;
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL().toString());
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.activate(null);
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
  public void testNegativeLengthLocalUTF8Calendar() throws IOException {
    String[] times = formatDate(new Date(System.currentTimeMillis() + 120000L));
    String temp = times[0];
    times[0] = times[1];
    times[1] = temp;
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    testfile.delete();
  }

/* Commented out due to problems getting the UTF16 file to read properly.
  @Test @Ignore
  public void testValidRemoteUTF16Calendar() {
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    String knownGood = this.getClass().getClassLoader().getResource("calendars/Opencast-UTF16.ics").toString();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, knownGood);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("20091005T090000", schedule[0]);
  }

  @Test @Ignore
  public void testValidLocalUTF16Calendar() {
    String knownGood = this.getClass().getClassLoader().getResource("calendars/Opencast-UTF16.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, knownGood);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("20091005T090000", schedule[0]);
  }
*/

  @Test
  public void testBlankRemoteCalendar() {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, cachedBlank);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testBlankLocalCalendar() {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, cachedBlank);
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedRemoteURLCalendar() {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedLocalURLCalendar() {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedCalendars() {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantRemoteCalendar() {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, nonExistant);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantLocalCalendar() {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, nonExistant);
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }
  
  @Test
  public void testGarbageRemoteCalendar() {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, garbage);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testGarbageCalendar() {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, garbage);
    sched.activate(null);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testEndpoint() throws MalformedURLException {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "http://www.example.com");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    URL test = new URL("http://www.example.com");
    sched.setScheduleEndpoint(test);
    Assert.assertEquals(test, sched.getScheduleEndpoint());
  }
}
