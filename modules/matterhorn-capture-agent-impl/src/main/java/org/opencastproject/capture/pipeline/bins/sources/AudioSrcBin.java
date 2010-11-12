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
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public abstract class AudioSrcBin extends SrcBin{

  /** Super class for all audio sources whether they are test, pulse, alsa or other. **/
  public AudioSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  /** Audio convert is used to convert any input audio into a format usable by gstreamer. Might not be strictly necessary. **/
  Element audioconvert;
  
  /** Create all the common elements necessary for audio sources including a queue and an audio converter. **/
  protected void createElements(){
    super.createElements();
    createQueue();
    createAudioConverter();
  }
  
  private void createQueue() {
    queue = ElementFactory.make("queue", null);
  }

  private void createAudioConverter() {
    audioconvert = ElementFactory.make("audioconvert", null);
  }
  
  @Override
  public boolean isVideoDevice(){
    return false;
  }
  
  @Override
  public boolean isAudioDevice(){
    return true;
  }
}
