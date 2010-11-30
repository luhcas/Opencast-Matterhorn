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
package org.opencastproject.capture.pipeline.bins.producers.epiphan;

import java.util.List;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.elements.AppSink;
import org.junit.Assert;
import org.junit.Test;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;

/**
 * Test class for {@Link org.opencastproject.capture.pipeline.bins.producers.epiphan.EpiphanSubTestSrcBin}.
 */
public class EpiphanSubTestSrcBinTest extends EpiphanTest {

  @Test
  public void subTestSrcBinTest() throws Exception {

    if (!gstreamerInstalled)
      return;

    EpiphanProducer epiphanBin = new EpiphanProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanSubTestSrcBin))
      return;
    
    Assert.assertNotNull(epiphanBin.subBin);
    Assert.assertTrue(epiphanBin.subBin instanceof EpiphanSubTestSrcBin);
    Assert.assertEquals(epiphanBin.getCaps(), ((EpiphanSubTestSrcBin)epiphanBin.subBin).caps);
  }

  @Test
  public void subTestSrcBinCreateElementsTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanProducer epiphanBin = new EpiphanProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanSubTestSrcBin))
      return;
    
    EpiphanSubTestSrcBin subBin = (EpiphanSubTestSrcBin) epiphanBin.subBin;
    Assert.assertNotNull(subBin.src);
    Assert.assertNotNull(subBin.caps_filter);
    Assert.assertNotNull(subBin.sink);
    Assert.assertTrue(subBin.sink instanceof AppSink);

    List<Element> elements = subBin.pipeline.getElements();
    Assert.assertTrue(elements.size() == 3);
  }

  @Test
  public void subTestSrcBinSetElementPropertiesTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanProducer epiphanBin = new EpiphanProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanSubTestSrcBin))
      return;
    
    EpiphanSubTestSrcBin subBin = (EpiphanSubTestSrcBin) epiphanBin.subBin;
    Assert.assertEquals(subBin.src.get(GStreamerProperties.PATTERN), 0);
    Assert.assertEquals(subBin.src.get(GStreamerProperties.IS_LIVE), true);
    Assert.assertEquals(subBin.src.get(GStreamerProperties.DO_TIMESTAP), false);
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
    if (!gstreamerInstalled)
      return;

    EpiphanProducer epiphanBin = new EpiphanProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanSubTestSrcBin))
      return;
    
    EpiphanSubTestSrcBin subBin = (EpiphanSubTestSrcBin) epiphanBin.subBin;

    // src -> caps_filter
    Pad pad = subBin.src.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == subBin.caps_filter);

    // caps_filter -> sink
    pad = subBin.caps_filter.getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == subBin.sink);
  }

  @Test
  public void subTestSrcBinBinRemoveElementsTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    EpiphanProducer epiphanBin = new EpiphanProducer(captureDevice, properties);

    if (!(epiphanBin.subBin instanceof EpiphanSubTestSrcBin))
      return;

    EpiphanSubTestSrcBin subBin = (EpiphanSubTestSrcBin) epiphanBin.subBin;
    subBin.removeElements();

    List<Element> elements = subBin.pipeline.getElements();
    Assert.assertTrue(elements.isEmpty());
  }
}
