package org.opencastproject.capture.impl;

import org.gstreamer.Pipeline;
import org.gstreamer.event.EOSEvent;

public class CaptureAgentEOSThread implements Runnable {
  
  private long timeout;
  private Pipeline pipeline;

  public void run() {
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    pipeline.sendEvent(new EOSEvent());
  }

  public CaptureAgentEOSThread(long milli, Pipeline p) {
    timeout = milli;
    pipeline = p;
  }
}
