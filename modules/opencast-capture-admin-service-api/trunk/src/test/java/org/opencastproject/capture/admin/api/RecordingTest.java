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
package org.opencastproject.capture.admin.api;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RecordingTest {
  private Recording recording = null;
  private Long time = 0L;

  @Before
  public void setup() {
    recording = new Recording("test", RecordingState.CAPTURING);
    Assert.assertNotNull(recording);
    time = recording.getLastCheckinTime();
  }

  @After
  public void teardown() {
    recording = null;
    time = 0L;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", recording.getID());
    Assert.assertEquals(RecordingState.CAPTURING, recording.getState());
  }
  
  @Test
  public void changedInformation() throws InterruptedException {
    Assert.assertEquals("test", recording.getID());
    Assert.assertEquals(AgentState.CAPTURING, recording.getState());
    Assert.assertEquals(time, recording.getLastCheckinTime());

    Thread.sleep(1);
    recording.setState(RecordingState.UPLOADING);

    Assert.assertEquals("test", recording.getID());
    Assert.assertEquals(AgentState.UPLOADING, recording.getState());
    Thread.sleep(1);
    if (recording.getLastCheckinTime() <= time || recording.getLastCheckinTime() >= System.currentTimeMillis()) {
      Assert.fail("Invalid checkin time");
    }
  }  
}
