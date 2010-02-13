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

public class RecordingStateUpdateTest {
  private Recording recording = null;
  private RecordingStateUpdate rsu = null;

  @Before
  public void setup() throws InterruptedException {
    recording = new Recording("test", RecordingState.CAPTURING);
    Assert.assertNotNull(recording);
    Thread.sleep(5);
    rsu = new RecordingStateUpdate(recording);
    Assert.assertNotNull(rsu);
  }

  @After
  public void teardown() {
    recording = null;
    rsu = null;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", rsu.id);
    Assert.assertEquals(RecordingState.CAPTURING, rsu.state);
    if (rsu.time_delta <= 1) {
      Assert.fail("Invalid update time in recording state update");
    }
  }
}
