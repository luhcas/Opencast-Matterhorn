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
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

/** PulseAudioSrcBin captures from a pulse audio source such as a desktop linux distro. **/
public class PulseAudioSrcBin extends AudioSrcBin {

  Element pulseAudioSrc;
  
  public PulseAudioSrcBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException {
    super(captureDevice, properties);
  }

  @Override
  protected void createElements(){
    pulseAudioSrc = ElementFactory.make(GStreamerElements.PULSESRC, null);
  }
  
  @Override
  public Pad getSrcPad() {
    return pulseAudioSrc.getStaticPad(GStreamerProperties.SRC);
  }

  @Override
  protected void addElementsToBin() {
    bin.add(pulseAudioSrc);
  }
  
  @Override
  public boolean isAudioDevice(){
    return true;
  }
  
}
