package org.opencastproject.capture.pipeline.bins;

import java.util.LinkedList;
import java.util.Properties;

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.SinkDeviceName;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.capture.pipeline.bins.sinks.SinkBin;
import org.opencastproject.capture.pipeline.bins.sinks.SinkFactory;
import org.opencastproject.capture.pipeline.bins.sources.SourceFactory;
import org.opencastproject.capture.pipeline.bins.sources.SrcBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Creates a bin that contains a complete pipeline from source to sink for a capture device. 
 * @author akm220
 *
 */
public class CaptureDeviceBin {
  private Bin bin = new Bin();
  private Element tee;
  private SrcBin srcBin;
  private LinkedList<SinkBin> sinkBins = new LinkedList<SinkBin>();
  private static final Logger logger = LoggerFactory.getLogger(PipelineFactory.class);
  private CaptureDevice captureDevice;
  private static final boolean USE_XV_IMAGE_SINK = false;
  
  public CaptureDeviceBin(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgent) throws Exception{
    this.captureDevice = captureDevice;
    if(captureDevice.getName() == SourceDeviceName.FILE){
      addFileBin(captureDevice, properties);
    }
    else if(captureDevice.getName() == SourceDeviceName.HAUPPAUGE_WINTV &&  captureDevice.properties.getProperty("codec") == null && captureDevice.properties.getProperty("container") == null){
      addFileBin(captureDevice, properties);
    }
    else{
      getSrcBin(captureDevice, properties, captureAgent);
      getSinkBins(captureDevice, properties, captureAgent);
      linkSrcToSinkBins();
    }
  }

  private void addFileBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    FileBin fileBin = null;
    fileBin = new FileBin(captureDevice, properties);
    if(bin != null){
      bin.add(fileBin.getBin());
    }
  }

  private void getSinkBins(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgentMock) throws Exception {
    SinkBin fileSinkBin;
    try {
      fileSinkBin = createFileSinkBin(captureDevice, properties);
      sinkBins.add(fileSinkBin);
    } catch (Exception e) {
      logger.error("Failed to create File Sink Bin " + e.getMessage());
      e.printStackTrace();
    }
  }

  private SinkBin createFileSinkBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    SinkBin sinkBin;
    if(srcBin.isVideoDevice()){
      sinkBin = SinkFactory.getInstance().getSink(SinkDeviceName.VIDEO_FILE_SINK, captureDevice, properties);
      useXVImageSink(properties);
    }
    else{
      sinkBin = SinkFactory.getInstance().getSink(SinkDeviceName.AUDIO_FILE_SINK, captureDevice, properties);
    }
    return sinkBin;
  }

  private void useXVImageSink(Properties properties){
    if(USE_XV_IMAGE_SINK){
      try {
        SinkBin xvImageSinkBin = createXVImageSinkBin(captureDevice, properties);
        sinkBins.add(xvImageSinkBin);
      } catch (Exception e) {
        logger.error("Failed to create XV Image Sink Bin " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
  
  private SinkBin createXVImageSinkBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    SinkBin sinkBin;
    if(srcBin.isVideoDevice()){
      sinkBin = SinkFactory.getInstance().getSink(SinkDeviceName.XVIMAGESINK, captureDevice, properties);
    }
    else{
      sinkBin = SinkFactory.getInstance().getSink(SinkDeviceName.AUDIO_FILE_SINK, captureDevice, properties);
    }
    return sinkBin;
  }
  
  private void getSrcBin(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgentMock) throws Exception  {
      srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
  }
  
  private void linkSrcToSinkBins() throws Exception {
    createTee();
    linkSinks();
  }

  private void createTee() throws Exception {
    makeTee();
    addTeeAndSrc();
    linkSrcToTee();
  }
  
  private void makeTee() {
    tee = ElementFactory.make("tee", null);
  }
  
  private void addTeeAndSrc() {
    bin.addMany(srcBin.getBin(), tee);
  }
  
  private void linkSrcToTee() throws Exception {
    if(!Element.linkPads(srcBin.getBin(), SrcBin.GHOST_PAD_NAME, tee, "sink")){
      throw new UnableToLinkGStreamerElementsException(captureDevice, srcBin.getBin(), tee);
    }
  }
  
  private void linkSinks() throws Exception {
    for(SinkBin sinkBin : sinkBins){
        addSinkToBin(sinkBin);
        linkTeeToSink(sinkBin);
    }
  }
  
  private void addSinkToBin(SinkBin sinkBin) {
    bin.add(sinkBin.getBin());
  }
  
  private void linkTeeToSink(SinkBin sinkBin) throws Exception {
    Pad newPad = tee.getRequestPad("src%d");
    Element queue = ElementFactory.make("queue", null);
    bin.add(queue);
    if(!Element.linkPads(tee, newPad.getName(), queue, "sink")){
      throw new Exception(CaptureDeviceBin.formatBinError(captureDevice, tee, sinkBin.getBin()));
    }
    else if(!Element.linkPads(queue, "src", sinkBin.getBin(), SinkBin.GHOST_PAD_NAME)){
      throw new Exception(CaptureDeviceBin.formatBinError(captureDevice, queue, sinkBin.getBin()));
    }
  }

  public Bin getBin(){
    return bin;
  }
  
  /**
   * String representation of linking errors that occur when creating pipeline
   * 
   * @param device
   *          The {@code CaptureDevice} the error occurred on
   * @param src
   *          The source {@code Element} being linked
   * @param sink
   *          The sink {@code Element} being linked
   * @return String representation of the error
   */
  public static String formatBinError(CaptureDevice device, Element src, Element sink) {
    return device.getLocation() + ": " + "(" + src.toString() + ", " + sink.toString() + ")";
  }
}
