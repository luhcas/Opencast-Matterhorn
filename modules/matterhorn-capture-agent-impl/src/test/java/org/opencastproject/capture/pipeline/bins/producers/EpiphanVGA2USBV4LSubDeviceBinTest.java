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
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;

/**
 * Test class for {@Link org.opencastproject.capture.pipeline.bins.producers.epiphan.EpiphanVGA2USBV4LSubDeviceBin}.
 */
public class EpiphanVGA2USBV4LSubDeviceBinTest extends EpiphanVGA2USBV4LTest {
  
  @Test
  public void subDeviceBinTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.deviceBin);
    Assert.assertTrue(epiphanBin.deviceBin instanceof EpiphanVGA2USBV4LSubDeviceBin);
    Assert.assertEquals(epiphanBin.getCaps(), epiphanBin.deviceBin.getCaps());
  }

  @Test
  public void subDeviceBinCreateElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubDeviceBin deviceBin = epiphanBin.deviceBin;
    Assert.assertNotNull(deviceBin.src);
    Assert.assertNotNull(deviceBin.colorspace);
    Assert.assertNotNull(deviceBin.videoscale);
    Assert.assertNotNull(deviceBin.capsfilter);
    Assert.assertNotNull(deviceBin.sink);
    Assert.assertTrue(deviceBin.sink instanceof AppSink);

    List<Element> elements = deviceBin.bin.getElements();
    Assert.assertEquals(elements.size(), 5);
  }

  @Test
  public void subDeviceBinSetElementPropertiesTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubDeviceBin deviceBin = epiphanBin.deviceBin;
    Assert.assertEquals(deviceBin.src.get(GStreamerProperties.DEVICE), epiphanLocation);
    Assert.assertEquals(deviceBin.src.get(GStreamerProperties.DO_TIMESTAP), false);
    Assert.assertEquals(deviceBin.sink.get(GStreamerProperties.EMIT_SIGNALS), false);
    Assert.assertEquals(deviceBin.sink.get(GStreamerProperties.DROP), true);
    Assert.assertEquals(deviceBin.sink.get(GStreamerProperties.MAX_BUFFERS), 1);
//    if (deviceBin.getCaps() != null) {
      //TODO: can not convert to Caps
//      Assert.assertEquals(deviceBin.capsfilter.get("caps"), Caps.fromString(deviceBin.getCaps()));
//      Assert.assertEquals(deviceBin.sink.get("caps"), Caps.fromString(deviceBin.getCaps()));
//    }
  }

  @Test
  public void subDeviceBinLinkElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubDeviceBin deviceBin = epiphanBin.deviceBin;
    
    // src -> colorspace
    Pad pad = deviceBin.src.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.colorspace);

    // colorspace -> videoscale
    pad = deviceBin.colorspace.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.videoscale);

    // videoscale -> capsfilter
    pad = deviceBin.videoscale.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.capsfilter);

    // capsfilter -> sink
    pad = deviceBin.capsfilter.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.sink);
  }

  @Test
  public void subDeviceBinRemoveElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);

    epiphanBin.deviceBin.removeElements();
    List<Element> elements = epiphanBin.deviceBin.bin.getElements();
    Assert.assertTrue(elements.isEmpty());
  }
}
