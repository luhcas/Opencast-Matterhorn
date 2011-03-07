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
package org.opencastproject.capture.pipeline;

import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.opencastproject.capture.pipeline.bins.consumers.AudioMonitoring;

import junit.framework.Assert;

import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.FakeSink;
import org.gstreamer.elements.FakeSrc;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Ignore
public class AudioMonitoringTest {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  @BeforeClass
  public void setUp() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
    if (!new File("/usr/lib/libjv4linfo.so").exists()) {
      return;
    }
    Gst.init();
  }

  @Test
  public void testAudioMonitor() {
    if (!gstreamerInstalled)
      return;
    if (!new File("/usr/lib/libjv4linfo.so").exists())
      return;
    Pipeline pipeline = new Pipeline();
    FakeSrc src = (FakeSrc) ElementFactory.make("fakesrc", null);
    FakeSink sink = (FakeSink) ElementFactory.make("fakesink", null);
    pipeline.addMany(src, sink);
    if (!src.link(sink)) {
      Assert.fail();
    }

    boolean ret = AudioMonitoring.addAudioMonitor(pipeline, src, sink, 0, 0, "test");
    Assert.assertTrue(ret);
    Assert.assertEquals(8, pipeline.getElements().size());
  }
}
