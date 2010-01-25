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
