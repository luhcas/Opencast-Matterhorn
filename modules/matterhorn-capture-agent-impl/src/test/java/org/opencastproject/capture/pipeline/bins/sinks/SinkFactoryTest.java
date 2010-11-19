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

import static org.easymock.EasyMock.createMock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.SinkDeviceName;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinkFactoryTest {

  CaptureAgent captureAgentMock;

  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;

  /** Properties specifically designed for unit testing */
  private static Properties properties = null;

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

    captureAgentMock = createMock(CaptureAgent.class);

    Properties captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, null, null, null, null,
            null, null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.EPIPHAN_VGA2USB,
            "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);

    properties = BinTestHelpers.createConfidenceMonitoringProperties();
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    properties = null;
    captureDevice = null;
  }

  @Test
  public void testXVImageSink() {
    if (!BinTestHelpers.isLinux())
      return;
    if (!gstreamerInstalled)
      return;
    try {
      SinkBin sinkBin = getSink(SinkDeviceName.XVIMAGE_SINK);
      Assert.assertTrue(sinkBin instanceof XVImageSinkBin);
      Assert.assertTrue(sinkBin.getSrc() != null);
    } catch (UnableToCreateElementException e) {
      logger.error("Unable to create an XV Image Sink in SinkFactoryTest", e);
    }
  }

  @Test
  public void testVideoFileSink() {
    if (!gstreamerInstalled)
      return;
    try {
      SinkBin sinkBin = getSink(SinkDeviceName.VIDEO_FILE_SINK);
      Assert.assertTrue(sinkBin instanceof VideoFileSinkBin);
      Assert.assertTrue(sinkBin.getSrc() != null);
    } catch (UnableToCreateElementException e) {
      logger.error("Unable to create an Video File Sink in SinkFactoryTest", e);
    }
   
  }

  @Test
  public void testAudioFileSink() {
    if (!gstreamerInstalled)
      return;
    try {
      SinkBin sinkBin = getSink(SinkDeviceName.AUDIO_FILE_SINK);
      Assert.assertTrue(sinkBin instanceof AudioFileSinkBin);
      Assert.assertTrue(sinkBin.getSrc() != null);
    } catch (UnableToCreateElementException e) {
      logger.error("Unable to create an Audio File Sink in SinkFactoryTest", e);
    }
  }

  private SinkBin getSink(SinkDeviceName sinkDeviceName) throws UnableToCreateElementException {
    SinkBin sinkBin = null;
    try {
      sinkBin = SinkFactory.getInstance().getSink(sinkDeviceName, captureDevice, properties);
    } catch (NoSinkBinFoundException e) {
      logger.error("Error Creating the Sink ", e);
      Assert.fail(e.getMessage());
    } catch (UnableToLinkGStreamerElementsException e) {
      logger.error("Error Creating the Sink ", e);
      Assert.fail(e.getMessage());
    } catch (UnableToCreateGhostPadsForBinException e) {
      logger.error("Error Creating the Sink ", e);
      Assert.fail(e.getMessage());
    } catch (UnableToSetElementPropertyBecauseElementWasNullException e) {
      logger.error("Error Creating the Sink ", e);
      Assert.fail(e.getMessage());
    } catch (CaptureDeviceNullPointerException e) {
      logger.error("Error Creating the Sink ", e);
      Assert.fail(e.getMessage());
    } 
    return sinkBin;
  }
}
