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
package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

public class AudioTestSrcBin extends AudioSrcBin {

  Element audiotestsrc;
  
  /** Used to create an audio test src great for testing the capture agent without needing any devices
   * installed but still gives that authentic capturing experience. 
   * @param captureDevice - The details for this capture device.
   * @param properties - The confidence monitoring details for this device. 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException 
   * @throws UnableToCreateGhostPadsForBinException 
   * @throws UnableToLinkGStreamerElementsException 
   * @throws CaptureDeviceNullPointerException 
   * @throws Exception - If anything fails to link, in this case nothing, it throws an exception with the details.
   */
  public AudioTestSrcBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException {
    super(captureDevice, properties);
  }

  /** Create an audiotestsrc Element (see gst-inspect for details) **/
  @Override
  protected void createElements(){
    audiotestsrc = ElementFactory.make("audiotestsrc", null);
  }
  
  /** Add the audiotestsrc to the bin we will return as a source. **/
  @Override
  protected void addElementsToBin() {
    bin.add(audiotestsrc);
  }

  /** The sink for this source is the audiotestsrc itself that will be used to
   * create the ghost pads for this bin. **/
  @Override
  public Pad getSrcPad() {
    return audiotestsrc.getStaticPad("src");
  }
}
