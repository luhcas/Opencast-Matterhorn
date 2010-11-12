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

public class CustomVideoSrcBin extends SrcBin {

  private static final boolean LINK_UNUSED_GHOST_PADS = true;
  
  public CustomVideoSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  @Override
  protected Pad getSrcPad() {
    if(bin.getSinks().size() >= 1){
      return bin.getSinks().get(0).getStaticPad("src");
    }
    else{
      return null;
    }
  }

  @Override
  protected void createElements(){
    bin = Bin.launch(captureDeviceProperties.customSource, LINK_UNUSED_GHOST_PADS);
  }
  
  @Override
  protected void addElementsToBin() {

  }

  @Override
  protected void createGhostPads() throws Exception {
    
  }
  
  @Override
  protected void linkElements() throws Exception {

  }

  @Override
  protected void setElementProperties() throws Exception {

  }

  @Override
  public boolean isVideoDevice(){
    return true;
  }
}
