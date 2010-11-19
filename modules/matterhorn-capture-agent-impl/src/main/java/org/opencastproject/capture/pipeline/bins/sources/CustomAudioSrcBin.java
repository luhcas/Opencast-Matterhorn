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

import org.gstreamer.Bin;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

public class CustomAudioSrcBin extends SrcBin {

  private static final boolean LINK_UNUSED_GHOST_PADS = true;
  
  public CustomAudioSrcBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  @Override
  protected Pad getSrcPad() throws UnableToCreateGhostPadsForBinException {
    synchronized (bin) {
      if(bin.getSinks().size() >= 1){
        return bin.getSinks().get(0).getStaticPad(GStreamerProperties.SRC);
      }
      else{
        throw new UnableToCreateGhostPadsForBinException("Unable to ghost pads for Custom Audio Source");
      }
    }
  }

  @Override
  protected void createElements(){
    bin = Bin.launch(captureDeviceProperties.customSource, LINK_UNUSED_GHOST_PADS);
  }

  /** Need an empty method for createGhostPads because the Bin.launch will create the ghost pads all on its own.**/
  @Override
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {
    
  }
  
  @Override
  public boolean isAudioDevice(){
    return true;
  }
}
