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

public class VideoMonitoringTest {

  @BeforeClass
  public static void setup() {
    Gst.init();
  }
  
  @AfterClass
  public static void tearDown() {
    Gst.deinit();
  }
  
  @Test
  public void testVideoMonitor() {
    Pipeline pipeline = new Pipeline();
    FakeSrc src = (FakeSrc) ElementFactory.make("fakesrc", null);
    FakeSink sink = (FakeSink) ElementFactory.make("fakesink", null);
    pipeline.addMany(src, sink);
    if (!src.link(sink)) {
      Assert.fail();
    }
    
    boolean ret = VideoMonitoring.addVideoMonitor(pipeline, src, sink, 0);
    Assert.assertTrue(ret);
    Assert.assertEquals(9, pipeline.getElements().size());
  }
}
