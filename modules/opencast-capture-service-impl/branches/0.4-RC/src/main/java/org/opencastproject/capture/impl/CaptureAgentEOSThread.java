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
