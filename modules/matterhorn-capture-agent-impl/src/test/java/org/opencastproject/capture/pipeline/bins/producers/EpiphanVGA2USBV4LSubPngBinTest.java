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
package org.opencastproject.capture.pipeline.bins.producers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.elements.AppSink;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.util.ConfigurationException;

/**
 * Test class for {@Link org.opencastproject.capture.pipeline.bins.producers.EpiphanVGA2USBV4LSubPngBin}.
 */
public class EpiphanVGA2USBV4LSubPngBinTest extends EpiphanVGA2USBV4LTest {


  String imageMockPath = null;
  
  @Before
  @Override
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    super.setup();
    
    if (!readyTestEnvironment())
      return;

    // create fallback image mock
    File imageMock = new File(System.getProperty("java.io.tmpdir"), "testpipe/fallback.png");
    imageMock.createNewFile();
    imageMockPath = imageMock.getAbsolutePath();

    properties.setProperty(CaptureParameters.FALLBACK_PNG, imageMockPath);
  }

  @Test
  public void subTestSrcBinTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    Assert.assertNotNull(epiphanBin.subBin);
    Assert.assertTrue(epiphanBin.subBin instanceof EpiphanVGA2USBV4LSubPngBin);
    Assert.assertEquals(epiphanBin.getCaps(), ((EpiphanVGA2USBV4LSubPngBin)epiphanBin.subBin).caps);
  }

  @Test
  public void subTestSrcBinCreateElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.subBin;
    Assert.assertNotNull(subBin.src);
    Assert.assertNotNull(subBin.pngdec);
    Assert.assertNotNull(subBin.colorspace);
    Assert.assertNotNull(subBin.scale);
    Assert.assertNotNull(subBin.caps_filter);
    Assert.assertNotNull(subBin.sink);
    Assert.assertTrue(subBin.sink instanceof AppSink);

    List<Element> elements = subBin.bin.getElements();
    Assert.assertEquals(elements.size(), 6);
  }

  @Test
  public void subTestSrcBinSetElementPropertiesTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.subBin;
    Assert.assertEquals(subBin.src.get(GStreamerProperties.LOCATION), imageMockPath);
    Assert.assertEquals(subBin.sink.get(GStreamerProperties.EMIT_SIGNALS), false);
    Assert.assertEquals(subBin.sink.get(GStreamerProperties.DROP), true);
    Assert.assertEquals(subBin.sink.get(GStreamerProperties.MAX_BUFFERS), 1);
//    if (subBin.caps != null) {
//      //TODO: can not convert to Caps
//      Assert.assertEquals(subBin.caps_filter.get("caps"), Caps.fromString(subBin.caps));
//    }
  }

  @Test
  public void subTestSrcBinBinLinkElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.subBin;

    // src -> jpegdec
    Pad pad = subBin.src.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.pngdec);

    // jpegdec -> colorspace
    pad = subBin.pngdec.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.colorspace);

    // colorspace -> scale
    pad = subBin.colorspace.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.scale);

    // scale -> caps_filter
    pad = subBin.scale.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.caps_filter);

    // caps_filter -> sink
    pad = subBin.caps_filter.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.sink);
  }

  @Test
  public void subTestSrcBinBinRemoveElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.subBin;
    subBin.removeElements();

    List<Element> elements = subBin.bin.getElements();
    Assert.assertTrue(elements.isEmpty());
  }
}
