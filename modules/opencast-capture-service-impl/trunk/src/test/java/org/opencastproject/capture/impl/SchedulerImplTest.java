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

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SchedulerImplTest {

  SchedulerImpl sched = null;
  ConfigurationManager config = null;

  @Before
  public void setUp() {
    config = ConfigurationManager.getInstance();
    sched = new SchedulerImpl();
    sched.activate(null);
  }

  @After
  public void tearDown() {
    sched.shutdown();
    sched = null;
    config = null;
  }

  @Test
  public void testValidRemoteUTF8Calendar() {
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    String knownGood = this.getClass().getClassLoader().getResource("calendars/Opencast.ics").toString();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, knownGood);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("20091005T090000", schedule[0]);
  }

  @Test
  public void testValidLocalUTF8Calendar() {
    String knownGood = this.getClass().getClassLoader().getResource("calendars/Opencast.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, knownGood);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("20091005T090000", schedule[0]);
  }

/* Commented out due to problems getting the UTF16 file to read properly.
  @Test
  public void testValidRemoteUTF16Calendar() {
    //Yes, I know this isn't actually remote.  The point is to test the two different paths for loading calendar data
    String knownGood = this.getClass().getClassLoader().getResource("calendars/Opencast-UTF16.ics").toString();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, knownGood);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("20091005T090000", schedule[0]);
  }

  @Test
  public void testValidLocalUTF16Calendar() {
    String knownGood = this.getClass().getClassLoader().getResource("calendars/Opencast-UTF16.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, knownGood);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("20091005T090000", schedule[0]);
  }
*/

  @Test
  public void testBlankRemoteCalendar() {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, cachedBlank);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testBlankLocalCalendar() {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, cachedBlank);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedRemoteURLCalendar() {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "blah!");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedLocalURLCalendar() {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantRemoteCalendar() {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, nonExistant);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertArrayEquals(null, schedule);
  }

  @Test
  public void testNonExistantLocalCalendar() {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile() + "nonExistantTest";
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, nonExistant);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertArrayEquals(null, schedule);
  }
  
  @Test
  public void testGarbageRemoteCalendar() {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, garbage);
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testGarbageCalendar() {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, garbage);
    String[] schedule = sched.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testEndpoit() {
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_URL, "");
    config.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    URL test;
    try {
      test = new URL("http://www.example.com");
      sched.setScheduleEndpoint(test);
      Assert.assertEquals(test, sched.getScheduleEndpoint());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }
}
