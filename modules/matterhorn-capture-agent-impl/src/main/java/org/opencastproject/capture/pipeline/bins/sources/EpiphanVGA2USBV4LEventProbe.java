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

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.Event;
import org.gstreamer.Pad;
import org.gstreamer.Pad.EVENT_PROBE;
import org.gstreamer.event.EOSEvent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpiphanVGA2USBV4LEventProbe implements EVENT_PROBE{
  protected static final Logger logger = LoggerFactory.getLogger(PipelineFactory.class);
  private CaptureAgent captureAgent;
  private CaptureDevice captureDevice;
  private Bin bin;
  private boolean broken = false;
  
  public EpiphanVGA2USBV4LEventProbe(CaptureAgent captureAgent, CaptureDevice captureDevice, Bin bin){
    this.captureAgent = captureAgent;
    this.captureDevice = captureDevice;
    this.bin = bin;
  }
  
  public boolean isBroken(){
    return broken;
  }
  
  /**
   * @return true if we should propagate the EOS down the chain, false otherwise
   */
  @Override
  public synchronized boolean eventReceived(Pad pad, Event event) {
    logger.debug("Event received: {}", event.toString());
    if (event instanceof EOSEvent) {
      if (captureAgent.getAgentState().equals(AgentState.SHUTTING_DOWN)) {
        synchronized (PollEpiphan.enabled) {
          PollEpiphan.enabled.notify();
        }
        //return true;  
        return false; //TODO: this is insane
        
      }
      logger.debug("EOS event received, state is not shutting down: Lost VGA signal. " );
      
      // Sanity check, if we have already identified this as broken no need to unlink the elements
      if (broken){
        //return false;  
        return true; //TODO: this is insane
      }
      
      // An EOS means the Epiphan source has broken (unplugged)
      broken = true;
      
      // Remove the broken v4lsrc
      Element src = bin.getElementByName("v4lsrc_" + captureDevice.getLocation() + "_"
              + EpiphanVGA2USBV4LSrcBin.v4lsrcIndex); 
      src.unlink(bin.getElementByName(captureDevice.getLocation() + "_v4l_identity"));
      bin.remove(src);
      
      // Tell the input-selector to change its active-pad
      Element selector = bin.getElementByName(captureDevice.getLocation() + "_selector");
      Pad newPad = selector.getStaticPad("sink1");
      selector.set("active-pad", newPad);
      
      // Do not propagate the EOS down the pipeline
      //return false;  
      return true; //TODO: this is insane
    }
    //return true;  
    return false; //TODO: this is insane
  }

}
