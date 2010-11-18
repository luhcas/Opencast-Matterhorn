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
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.BufferThread;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.PartialBin;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

/** SinkBin is the ancestor for all Sinks and is meant to be inherited. To inherit from SinkBin:
 * 1. In the same pattern for the rest of the bins make sure you create any additional Elements you need in 
 *    overridden createElements, set any of the properties for the Elements in overridden setProperties, add all 
 *    Elements into the Bin with overridden AddElementsToBin and finally link your Elements together with linkElements.
 *      
 * 2. Make sure you return the first Element in your Bin in the getSrc method. This will be used to create the ghost
 *    pads for the bin so that it can be linked to the source.
 */
public abstract class SinkBin extends PartialBin{
  public static final String GHOST_PAD_NAME = "SinkBin Ghost Pad";
  protected Element queue;
  protected Element encoder;
  protected Element muxer;
  protected Element filesink;
  
  
  public SinkBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException {
    super(captureDevice, properties);
    addTrace();
  }
  
  /** Creates all the elements necessary for the Bin. In this case just the suffix. Hopefully the descendants
   * of this class will define further elements that they will use to capture. **/
  protected void createElements() {
    createQueue();
    createFileSink();
  }

  /** Creates the queue that prevents us dropping frames **/
  private void createQueue() {
    queue = ElementFactory.make(GStreamerElements.QUEUE, captureDevice.getFriendlyName());
  }
  
  /** Creates the filesink that will be the output of the capture device **/
  private void createFileSink() {
    filesink = ElementFactory.make(GStreamerElements.FILESINK, null);
  }
  
  /** Sets the queue size for the sink so that we don't lose any information. 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException 
   * @throws IllegalArgumentException **/
  protected void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    setQueueProperties();
  }
  
  /** Sets the size of the queue that acts as a buffer so that we don't lose frames or audio bits. **/ 
  private void setQueueProperties() {
      if(queue == null){
        throw new IllegalArgumentException("Queue cannot be null when we try to set its properties.");
      }
    synchronized (queue) {
      if (captureDeviceProperties.bufferCount != null) {
        logger.debug("{} bufferCount is being set to {}.", captureDevice.getName(),
                captureDeviceProperties.bufferCount);
        queue.set(GStreamerProperties.MAX_SIZE_BUFFERS, captureDeviceProperties.bufferCount);
      }
      if (captureDeviceProperties.bufferBytes != null) {
        logger.debug("{} bufferBytes is being set to {}.", captureDevice.getName(),
                captureDeviceProperties.bufferBytes);
        queue.set(GStreamerProperties.MAX_SIZE_BYTES, captureDeviceProperties.bufferBytes);
      }
      if (captureDeviceProperties.bufferTime != null) {
        logger.debug("{} bufferTime is being set to {}.", captureDevice.getName(), captureDeviceProperties.bufferTime);
        queue.set(GStreamerProperties.MAX_SIZE_TIME, captureDeviceProperties.bufferTime);
      }
    }
  }
  
  /** Creates the ghost pad that will connect this SinkBin to the SrcBin. **/
  @Override
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {
    Pad ghostPadElement = getSrc().getStaticPad(GStreamerProperties.SINK);    
    if(ghostPadElement!= null && !bin.addPad(new GhostPad(GHOST_PAD_NAME, ghostPadElement))){
      throw new UnableToCreateGhostPadsForBinException("Could not create new Ghost Pad with " + this.getSrc().getName()
              + " in this SinkBin.");
    }
  }
  
  /** Is used by createGhostPads, set to the first Element in your Bin so that we can link your SinkBin to Sources. **/
  public abstract Element getSrc();
  
  /** If the system is setup to have trace level error tracking we will add a thread to monitor the queue **/
  private void addTrace(){
    if (logger.isTraceEnabled()) {
      Thread traceThread = new Thread(new BufferThread(queue));
      traceThread.start();
    }
  }
}
