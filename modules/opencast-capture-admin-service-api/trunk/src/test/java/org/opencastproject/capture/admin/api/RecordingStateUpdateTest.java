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
    Thread.sleep(1);
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
    Thread.sleep(1);
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
