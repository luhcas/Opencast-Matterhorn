package org.opencastproject.capture.pipeline.bins.sinks;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.BufferThread;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.PartialBin;


public abstract class SinkBin extends PartialBin{
  public static final String GHOST_PAD_NAME = "SinkBin Ghost Pad";
  protected Element queue;
  protected Element encoder;
  protected Element muxer;
  protected Element filesink;
  
  
  public SinkBin(CaptureDevice captureDevice, Properties properties) throws Exception{
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
    queue = ElementFactory.make("queue", captureDevice.getFriendlyName());
  }
  
  /** Creates the filesink that will be the product of the capture device **/
  private void createFileSink() {
    filesink = ElementFactory.make("filesink", null);
  }
  
  /** Sets the particular properties for the elements so that they behave as we expect. 
   * @throws Exception **/
  protected void setElementProperties() throws Exception {
    setQueueProperties();
  }
  
  /** Sets the size of the queue that acts as a buffer so that we don't lose frames or audio bits. **/ 
  private void setQueueProperties() {
    if (captureDeviceProperties.bufferCount != null) {
      logger.debug("{} bufferCount set to {}.", captureDevice.getName(), captureDeviceProperties.bufferCount);
      queue.set("max-size-buffers", captureDeviceProperties.bufferCount);
    }
    if (captureDeviceProperties.bufferBytes != null) {
      logger.debug("{} bufferBytes set to {}.", captureDevice.getName(), captureDeviceProperties.bufferBytes);
      queue.set("max-size-bytes", captureDeviceProperties.bufferBytes);
    }
    if (captureDeviceProperties.bufferTime != null) {
      logger.debug("{} bufferTime set to {}.", captureDevice.getName(), captureDeviceProperties.bufferTime);
      queue.set("max-size-time", captureDeviceProperties.bufferTime);
    }
  }
  
  @Override
  protected void createGhostPads() throws Exception {
    Pad ghostPadElement = getSrc().getStaticPad("sink");    
    if(ghostPadElement!= null && !bin.addPad(new GhostPad(GHOST_PAD_NAME, ghostPadElement))){
      throw new Exception("Could not create new Ghost Pad with " + this.getSrc());
    }
  }
  
  /** If the system is setup to have trace level error tracking we will add a thread to monitor the queue **/
  private void addTrace(){
    if (logger.isTraceEnabled()) {
      BufferThread t = new BufferThread(queue);
      t.start();
    }
  }
  
  public abstract Element getSrc();
}
