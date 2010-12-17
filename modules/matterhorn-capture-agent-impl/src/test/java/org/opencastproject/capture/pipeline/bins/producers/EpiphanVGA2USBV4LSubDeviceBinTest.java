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
 * Test class for {@Link org.opencastproject.capture.pipeline.bins.producers.epiphan.EpiphanVGA2USBV4LSubDeviceBin}.
 */
public class EpiphanVGA2USBV4LSubDeviceBinTest extends EpiphanVGA2USBV4LTest {

  @Test
  @Ignore
  public void subDeviceBinTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.deviceBin);
    Assert.assertTrue(epiphanBin.deviceBin instanceof EpiphanVGA2USBV4LSubDeviceBin);
    Assert.assertEquals(epiphanBin.getCaps(), epiphanBin.deviceBin.getCaps());
  }

  @Test
  @Ignore
  public void subDeviceBinCreateElementsTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.deviceBin.src);
    Assert.assertNotNull(epiphanBin.deviceBin.colorspace);
    Assert.assertNotNull(epiphanBin.deviceBin.videoscale);
    Assert.assertNotNull(epiphanBin.deviceBin.capsfilter);
    Assert.assertNotNull(epiphanBin.deviceBin.sink);
    Assert.assertTrue(epiphanBin.deviceBin.sink instanceof AppSink);

    List<Element> elements = epiphanBin.deviceBin.bin.getElements();
    Assert.assertTrue(elements.size() == 5);
  }

  @Test
  @Ignore
  public void subDeviceBinSetElementPropertiesTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertEquals(epiphanBin.deviceBin.src.get(GStreamerProperties.DEVICE), epiphanLocation);
    Assert.assertEquals(epiphanBin.deviceBin.src.get(GStreamerProperties.DO_TIMESTAP), false);
    Assert.assertEquals(epiphanBin.deviceBin.sink.get(GStreamerProperties.EMIT_SIGNALS), false);
    Assert.assertEquals(epiphanBin.deviceBin.sink.get(GStreamerProperties.DROP), true);
    Assert.assertEquals(epiphanBin.deviceBin.sink.get(GStreamerProperties.MAX_BUFFERS), 1);
//    if (epiphanBin.deviceBin.getCaps() != null) {
      //TODO: can not convert to Caps
//      Assert.assertEquals(epiphanBin.deviceBin.capsfilter.get("caps"), Caps.fromString(epiphanBin.deviceBin.getCaps()));
//      Assert.assertEquals(epiphanBin.deviceBin.sink.get("caps"), Caps.fromString(epiphanBin.deviceBin.getCaps()));
//    }
  }

  @Test
  @Ignore
  public void subDeviceBinBinLinkElementsTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    // src -> colorspace
    Pad pad = epiphanBin.deviceBin.src.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.deviceBin.colorspace);

    // colorspace -> videoscale
    pad = epiphanBin.deviceBin.colorspace.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.deviceBin.videoscale);

    // videoscale -> capsfilter
    pad = epiphanBin.deviceBin.videoscale.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.deviceBin.capsfilter);

    // capsfilter -> sink
    pad = epiphanBin.deviceBin.capsfilter.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.deviceBin.sink);
  }

  @Test
  @Ignore
  public void subDeviceBinBinRemoveElementsTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
    epiphanBin.deviceBin.removeElements();
    List<Element> elements = epiphanBin.deviceBin.bin.getElements();
    Assert.assertTrue(elements.isEmpty());
  }
}
