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
package org.opencastproject.capture.pipeline.bins.sinks;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

/**
 * AudioFileSinkBin dumps the audio source into a file. It is used when a SrcBin has the isAudioFlag set to true. The
 * main difference between this class and the VideoFileSinkBin is the defaults for the encoder (default=twolame) and
 * muxer(default=capsfilter). This class is not intended to be inherited.
 */
public class AudioFileSinkBin extends SinkBin {

  public static final String DEFAULT_ENCODER = GStreamerElements.TWOLAME;
  public static final String DEFAULT_MUXER = GStreamerElements.CAPSFILTER;
  
  public AudioFileSinkBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  @Override
  protected void createElements() throws UnableToCreateElementException{
    super.createElements();
    createEncoder();
    createMuxer();
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(queue, encoder, muxer, filesink); 
  }
  
  protected void createEncoder() throws UnableToCreateElementException{
    if (captureDeviceProperties.codec != null) {
      logger.debug("{} setting encoder to: {}", captureDevice.getName(), captureDeviceProperties.codec);
      encoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
              captureDeviceProperties.codec, null);
    }
    else {
      logger.debug("{} setting encoder to: {}", captureDevice.getName(), DEFAULT_ENCODER);
      encoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), DEFAULT_ENCODER,
              null);
    }   
  }

  private void createMuxer() {
    if (captureDeviceProperties.codec != null) {
      if (captureDeviceProperties.codec.equalsIgnoreCase(GStreamerElements.FAAC))
        muxer = ElementFactory.make(GStreamerElements.MP4MUX, null);
      else
        muxer = ElementFactory.make(DEFAULT_MUXER, null);
    }
    else {
      muxer = ElementFactory.make(DEFAULT_MUXER, null);
    }
    
    if (captureDeviceProperties.container != null) {
      logger.debug("{} setting muxing to: {}", captureDevice.getName(), captureDeviceProperties.container);
      muxer = ElementFactory.make(captureDeviceProperties.container, null);
    }
  }
  
  @Override
  protected void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    setEncoderProperties();
    setFileSinkProperties();
  }

  private void setEncoderProperties() {
    if (captureDeviceProperties.bitrate != null) {
      logger.debug("{} setting bitrate to: {}", captureDevice.getName(), captureDeviceProperties.bitrate);
      encoder.set(GStreamerProperties.BITRATE, captureDeviceProperties.bitrate);
    }
  }

  private void setFileSinkProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if(filesink == null){
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesink, GStreamerProperties.LOCATION);
    }
    else if(captureDevice.getOutputPath().equals("")){
      throw new IllegalArgumentException("File location must be set, it cannot be an empty String.");
    }
    else{
      filesink.set(GStreamerProperties.LOCATION, captureDevice.getOutputPath());
    }
  }
  
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!queue.link(encoder))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, encoder);
    else if (!encoder.link(muxer))
      throw new UnableToLinkGStreamerElementsException(captureDevice, encoder, muxer);
    else if (!muxer.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, muxer, filesink);
  }
  
  @Override
  public Element getSrc(){
    return queue;
  }
}
