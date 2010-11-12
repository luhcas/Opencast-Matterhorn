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
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class AlsaSrcBin extends AudioSrcBin {
  Element alsasrc;
  
  /**
   * Adds a pipeline specifically designed to captured from an ALSA source to the main pipeline
   * 
   * @param captureDevice
   *          The ALSA source {@code CaptureDevice} to create source around
   * @param properties 
   *          The {@code Properties} properties such as confidence monitoring necessary to create the source. 
   * @throws Exception if it failed to create the alsa source. 
   */
  public AlsaSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  /** Create all the elements required for an ALSA source **/
  @Override
  protected void createElements(){
    super.createElements();
    createAlsasrc();
  }

  private void createAlsasrc() {
    alsasrc = ElementFactory.make("alsasrc", null);
  }
  
  /** Set the correct properties for the ALSA source **/
  @Override
  protected void setElementProperties(){
    alsasrc.set("device", captureDevice.getLocation());
  }
  
  /** Add all the necessary elements to the bin. **/
  protected void addElementsToBin(){
    bin.addMany(alsasrc, queue, audioconvert);
  }

  /** Link all the necessary elements together. 
   * @throws If any of the elements will not link together it throws an exception. 
   */
  @Override
  protected void linkElements() throws Exception{
    if (!alsasrc.link(queue)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, alsasrc, queue);
    }
    else if (!queue.link(audioconvert)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, audioconvert);
    }
  }
  
  /** Returns the sink for this source so that we can create ghost pads for the bin. 
   * These ghost pads are then used to link the source bin to a tee, and from that tee
   * to all of the sinks we might need.
   */
  @Override
  protected Pad getSrcPad() {
    return audioconvert.getStaticPad("src");
  }
}
