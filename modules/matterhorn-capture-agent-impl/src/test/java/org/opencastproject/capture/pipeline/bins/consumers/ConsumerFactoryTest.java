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
package org.opencastproject.capture.pipeline.bins.consumers;

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
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerType;

import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerFactoryTest {

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
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", ProducerType.EPIPHAN_VGA2USB,
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
      ConsumerBin sinkBin = getSink(ConsumerType.XVIMAGE_SINK);
      Assert.assertTrue(sinkBin instanceof XVImagesinkConsumer);
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
      ConsumerBin sinkBin = getSink(ConsumerType.VIDEO_FILE_SINK);
      Assert.assertTrue(sinkBin instanceof VideoFilesinkConsumer);
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
      ConsumerBin sinkBin = getSink(ConsumerType.AUDIO_FILE_SINK);
      Assert.assertTrue(sinkBin instanceof AudioFilesinkConsumer);
      Assert.assertTrue(sinkBin.getSrc() != null);
    } catch (UnableToCreateElementException e) {
      logger.error("Unable to create an Audio File Sink in SinkFactoryTest", e);
    }
  }

  private ConsumerBin getSink(ConsumerType sinkDeviceName) throws UnableToCreateElementException {
    ConsumerBin sinkBin = null;
    try {
      sinkBin = ConsumerFactory.getInstance().getSink(sinkDeviceName, captureDevice, properties);
    } catch (NoConsumerFoundException e) {
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
