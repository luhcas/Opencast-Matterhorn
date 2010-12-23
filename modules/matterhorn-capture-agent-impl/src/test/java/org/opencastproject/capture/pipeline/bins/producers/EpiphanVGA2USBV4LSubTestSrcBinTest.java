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

import java.util.List;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.elements.AppSink;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;

/**
 * Test class for {@Link
 * org.opencastproject.capture.pipeline.bins.producers.epiphan.EpiphanVGA2USBV4LSubTestSrcBin}.
 */
public class EpiphanVGA2USBV4LSubTestSrcBinTest extends EpiphanVGA2USBV4LTest {

  @Test
  public void subTestSrcBinTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.subBin);
    Assert.assertTrue(epiphanBin.subBin instanceof EpiphanVGA2USBV4LSubTestSrcBin);
    Assert.assertEquals(epiphanBin.getCaps(), ((EpiphanVGA2USBV4LSubTestSrcBin) epiphanBin.subBin).caps);
  }

  @Test
  public void subTestSrcBinCreateElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubTestSrcBin subBin = (EpiphanVGA2USBV4LSubTestSrcBin) epiphanBin.subBin;
    Assert.assertNotNull(subBin.src);
    Assert.assertNotNull(subBin.capsFilter);
    Assert.assertNotNull(subBin.sink);
    Assert.assertTrue(subBin.sink instanceof AppSink);

    List<Element> elements = subBin.bin.getElements();
    Assert.assertTrue(elements.size() == 3);
  }

  @Test
  public void subTestSrcBinSetElementPropertiesTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubTestSrcBin subBin = (EpiphanVGA2USBV4LSubTestSrcBin) epiphanBin.subBin;
    Assert.assertEquals(subBin.src.get(GStreamerProperties.PATTERN), 0);
    Assert.assertEquals(subBin.src.get(GStreamerProperties.IS_LIVE), true);
    Assert.assertEquals(subBin.src.get(GStreamerProperties.DO_TIMESTAP), false);
    Assert.assertEquals(subBin.sink.get(GStreamerProperties.EMIT_SIGNALS), false);
    Assert.assertEquals(subBin.sink.get(GStreamerProperties.DROP), true);
    Assert.assertEquals(subBin.sink.get(GStreamerProperties.MAX_BUFFERS), 1);
    // if (subBin.caps != null) {
    // //TODO: can not convert to Caps
    // Assert.assertEquals(subBin.caps_filter.get("caps"), Caps.fromString(subBin.caps));
    // }
  }

  @Test
  public void subTestSrcBinBinLinkElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubTestSrcBin subBin = (EpiphanVGA2USBV4LSubTestSrcBin) epiphanBin.subBin;

    // src -> caps_filter
    Pad pad = subBin.src.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == subBin.capsFilter);

    // caps_filter -> sink
    pad = subBin.capsFilter.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == subBin.sink);
  }

  @Test
  public void subTestSrcBinBinRemoveElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubTestSrcBin subBin = (EpiphanVGA2USBV4LSubTestSrcBin) epiphanBin.subBin;
    subBin.removeElements();

    List<Element> elements = subBin.bin.getElements();
    Assert.assertTrue(elements.isEmpty());
  }
}
