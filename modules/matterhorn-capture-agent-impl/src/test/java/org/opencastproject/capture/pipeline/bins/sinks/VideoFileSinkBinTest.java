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
package org.opencastproject.capture.pipeline.bins.sinks;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoFileSinkBinTest {

  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;
  
  /** True to run the tests */
  private static boolean gstreamerInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }
   
  @Before
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }
  
  /** Salient encoder properties are codec and bitrate **/
  /** Salient muxer properties are codec and container **/
  private Properties createProperties(String codec, String bitrate, String container) {
    Properties captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, codec, bitrate, null,
            container, null, null, null, null);
    return captureDeviceProperties;
  }

  private void checkEncoderProperties(SinkBin sinkBin, String codec, String bitrate) {
    Assert.assertTrue(sinkBin.encoder.getName().contains(codec));
    Assert.assertEquals(bitrate, sinkBin.encoder.get("bitrate").toString());
  }
  
  private void checkMuxerProperties(SinkBin sinkBin, String muxer) {
    Assert.assertTrue("The muxer name " + sinkBin.muxer.getName() + " should match the muxer type " + muxer,
            sinkBin.muxer.getName().contains(muxer));
  }
  
  @Test
  public void nullSettingsForCodecBitrateAndContainerCreatesElementsWithDefaults() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFileSinkBin videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, VideoFileSinkBin.DEFAULT_ENCODER, "2000000");
    checkMuxerProperties(videoFileSinkBin, VideoFileSinkBin.DEFAULT_MUXER);
  }
  
  @Test
  public void settingBitrateChangesCodecBitrate() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, "32000000", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFileSinkBin videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, VideoFileSinkBin.DEFAULT_ENCODER, "32000000");
  }
  
  @Test
  public void settingCodecButNotContainerResultsInCorrectCodecAndDefaultMuxer() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("x264enc", "4096", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFileSinkBin videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, "x264enc", "4096");
    checkMuxerProperties(videoFileSinkBin, VideoFileSinkBin.DEFAULT_MUXER);
  }
  
  @Test
  public void settingContainerResultsInCorrectMuxer() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, "mpegtsmux");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFileSinkBin videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, VideoFileSinkBin.DEFAULT_ENCODER, "2000000");
    checkMuxerProperties(videoFileSinkBin, "mpegtsmux");
  }
  
  
  
  /** Testing permutations of possible file locations for tests **/
  @Test
  public void realFileLocationExampleSetsPropertyAndDoesntThrowException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = "/tmp/testpipe/test.mp2";
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name",
            location, captureDeviceProperties);
    VideoFileSinkBin sinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    Assert.assertEquals(location, sinkBin.filesink.get("location"));
  }
  
  @Test
  public void emptyFileLocationShouldThrowAnException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = "";
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name",
            location, captureDeviceProperties);
    @SuppressWarnings("unused")
    VideoFileSinkBin sinkBin = createVideoSinkBinWantException(captureDeviceProperties);
  }
  
  @Test
  public void nullFileLocationShouldThrowAnException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = null;
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name",
            location, captureDeviceProperties);
    @SuppressWarnings("unused")
    VideoFileSinkBin sinkBin = createVideoSinkBinWantException(captureDeviceProperties);
  }
  
  

  private VideoFileSinkBin createVideoFileSinkBinDontWantException(Properties captureDeviceProperties) {
    VideoFileSinkBin videoFileSinkBin = null;
    try {
      videoFileSinkBin = createVideoFileSinkBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return videoFileSinkBin;
  }
  
  private VideoFileSinkBin createVideoSinkBinWantException(Properties captureDeviceProperties) {
    VideoFileSinkBin videoFileSinkBin = null;
    try {
      videoFileSinkBin = createVideoFileSinkBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {
      
    }
    return videoFileSinkBin;
  }

  private VideoFileSinkBin createVideoFileSinkBin(Properties captureDeviceProperties) throws Exception {
    VideoFileSinkBin videoFileSinkBin;
    videoFileSinkBin = new VideoFileSinkBin(captureDevice, captureDeviceProperties);
    return videoFileSinkBin;
  }
}
