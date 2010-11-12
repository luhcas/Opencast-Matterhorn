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
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class VideoFileSinkBin extends SinkBin {

  public static final String DEFAULT_ENCODER = "ffenc_mpeg2video";
  public static final String DEFAULT_MUXER = "mpegpsmux";
  public static final String DEFAULT_X264_PASS = "4";
  
  public VideoFileSinkBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    super.createElements();
    createEncoder();
    createMuxer();
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(queue, encoder, muxer, filesink); 
  }
  
  protected void createEncoder(){
    if (captureDeviceProperties.codec != null) {
      logger.debug("{} encoder set to: {}", captureDevice.getName(), captureDeviceProperties.codec);
      encoder = ElementFactory.make(captureDeviceProperties.codec, null);
    }
    else {
      encoder = ElementFactory.make(DEFAULT_ENCODER, null);
    }
  }
  
  private void createMuxer() {
    // Must set H.264 encoding to use constant quantizer or else it will not start
    // Pass 0 is CBR (default), Pass 4 is constant quantizer, Pass 5 is constant quality
    if (captureDeviceProperties.codec != null && captureDeviceProperties.codec.equalsIgnoreCase("x264enc")) {
      encoder.set("pass", DEFAULT_X264_PASS);
      if (captureDevice.properties.contains("quantizer"))
        encoder.set("quantizer", captureDevice.properties.getProperty("quantizer"));
    }
    if (captureDeviceProperties.container != null) {
      logger.debug("{} muxing to: {}", captureDevice.getName(), captureDeviceProperties.container);
      muxer = ElementFactory.make(captureDeviceProperties.container, null);
    }
    else {
      muxer = ElementFactory.make(DEFAULT_MUXER, null);
    }
  }
  
  @Override
  protected void setElementProperties() throws Exception{
    super.setElementProperties();
    setFileSinkProperties();
  }
  
  /** Defines the location and bit rate of the filesink that will be the file that is created by the capture 
   * @throws Exception **/
  private void setFileSinkProperties() throws Exception {
    if(!captureDevice.getOutputPath().equals("")){
      filesink.set("location", captureDevice.getOutputPath());
    }
    else{
      throw new Exception("File location must be set, it cannot be an empty String.");
    }
    
    if (captureDeviceProperties.bitrate != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), captureDeviceProperties.bitrate);      
      encoder.set("bitrate", captureDeviceProperties.bitrate);
    }
    else{
      encoder.set("bitrate", "2000000"); 
    }
  }
  
  @Override
  protected void linkElements() throws Exception {
    if (!queue.link(encoder))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, encoder);
    else if (!encoder.link(muxer))
      throw new UnableToLinkGStreamerElementsException(captureDevice, encoder, muxer);
    else if (!muxer.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, muxer, filesink);
  }
  
  @Override
  public Element getSrc() {
    return queue;
  }
}
