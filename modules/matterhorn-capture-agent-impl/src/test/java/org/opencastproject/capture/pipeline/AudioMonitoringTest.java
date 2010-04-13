package org.opencastproject.capture.pipeline;

import junit.framework.Assert;

import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.FakeSink;
import org.gstreamer.elements.FakeSrc;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class AudioMonitoringTest {
  
  @BeforeClass
  public static void setup() {
    if (!new File("/usr/lib/libjv4linfo.so").exists())
      return;
    Gst.init();
  }
  
  @AfterClass
  public static void tearDown() {
    if (!new File("/usr/lib/libjv4linfo.so").exists())
      return;
    Gst.deinit();
  }
  
  @Test
  public void testAudioMonitor() {
    if (!new File("/usr/lib/libjv4linfo.so").exists())
      return;
    Pipeline pipeline = new Pipeline();
    FakeSrc src = (FakeSrc) ElementFactory.make("fakesrc", null);
    FakeSink sink = (FakeSink) ElementFactory.make("fakesink", null);
    pipeline.addMany(src, sink);
    if (!src.link(sink)) {
      Assert.fail();
    }
    
    boolean ret = AudioMonitoring.addAudioMonitor(pipeline, src, sink, 0);
    Assert.assertTrue(ret);
    Assert.assertEquals(8, pipeline.getElements().size());
  }
  
}
