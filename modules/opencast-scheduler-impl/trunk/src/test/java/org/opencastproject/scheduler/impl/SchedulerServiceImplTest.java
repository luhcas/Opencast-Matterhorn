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
package org.opencastproject.scheduler.impl;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerService;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchedulerServiceImplTest {
  private SchedulerService service = null;
  @Before
  public void setup() {
    service = new SchedulerServiceImpl();
  }

  @After
  public void teardown() {
    service = null;
  }
  
  @Test
  public void testEventManagement() {
    Assert.assertEquals("nothing", "nothing");
    SchedulerEvent event = new SchedulerEventImpl();
    event.setID("rrolf1001"); event.setTitle("new recording"); event.setLocation("42/201");
    SchedulerEvent event2 = service.addEvent(event);
    Assert.assertEquals(service.getEvent(event2.getID()).getLocation(), event.getLocation());
    Assert.assertNotNull(service.getEvents(null));
    service.removeEvent(event2.getID());
    Assert.assertNull(service.getEvent(event2.getID()));
    // TODO Need to improve this, when this is no longer a service stub
  }
}
