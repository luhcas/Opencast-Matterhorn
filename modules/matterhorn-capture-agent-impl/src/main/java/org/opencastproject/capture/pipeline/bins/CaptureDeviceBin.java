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
import org.opencastproject.capture.pipeline.bins.sinks.NoSinkBinFoundException;
import org.opencastproject.capture.pipeline.bins.sinks.SinkBin;
import org.opencastproject.capture.pipeline.bins.sinks.SinkFactory;
import org.opencastproject.capture.pipeline.bins.sources.SourceFactory;
import org.opencastproject.capture.pipeline.bins.sources.SrcBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Creates a bin that contains a complete pipeline from source to sink for a capture device. 
 */
public class CaptureDeviceBin {
  private Bin bin = new Bin();
  private Element tee;
  private SrcBin srcBin;
  private LinkedList<SinkBin> sinkBins = new LinkedList<SinkBin>();
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBin.class);
  private CaptureDevice captureDevice;
  private static final boolean USE_XV_IMAGE_SINK = false;
  
  public CaptureDeviceBin(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgent)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, NoSinkBinFoundException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    this.captureDevice = captureDevice;
    if(captureDevice.getName() == SourceDeviceName.FILE){
      addFileBin(captureDevice, properties);
    }
    else if (captureDevice.getName() == SourceDeviceName.HAUPPAUGE_WINTV
            && captureDevice.properties.getProperty("codec") == null
            && captureDevice.properties.getProperty("container") == null) {
      // If the device is a Hauppauge card and we aren't changing either the codec or the container create a FileBin
      // that will just copy the data from the Hauppauge card to the FileSink so that we take advantage of the onboard
      // mpeg encoding and don't do it in software.
      addFileBin(captureDevice, properties);
    }
    else{
      addSrcBin(captureDevice, properties, captureAgent);
      addSinkBins(captureDevice, properties, captureAgent);
      linkSrcToSinkBins();
    }
  }

  private void addFileBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    FileBin fileBin = new FileBin(captureDevice, properties);
    bin.add(fileBin.getBin());
  }

  private void addSinkBins(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgentMock)
          throws NoSinkBinFoundException, UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    SinkBin fileSinkBin;
    fileSinkBin = createFileSinkBin(captureDevice, properties);
    sinkBins.add(fileSinkBin);
  }

  private SinkBin createFileSinkBin(CaptureDevice captureDevice, Properties properties) throws NoSinkBinFoundException,
          UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    SinkBin sinkBin;
    if(srcBin.isVideoDevice()){
      sinkBin = SinkFactory.getInstance().getSink(SinkDeviceName.VIDEO_FILE_SINK, captureDevice, properties);
      setupXVImageSink(properties);
    }
    else{
      sinkBin = SinkFactory.getInstance().getSink(SinkDeviceName.AUDIO_FILE_SINK, captureDevice, properties);
    }
    return sinkBin;
  }

  private void setupXVImageSink(Properties properties) throws NoSinkBinFoundException,
          UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    if (USE_XV_IMAGE_SINK) {
      if (srcBin.isVideoDevice()) {
        SinkBin xvImageSinkBin = SinkFactory.getInstance().getSink(SinkDeviceName.XVIMAGE_SINK, captureDevice,
                properties);
        sinkBins.add(xvImageSinkBin);
      }
    }
  }
  
  private void addSrcBin(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgentMock)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
      srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
  }
  
  private void linkSrcToSinkBins() throws UnableToLinkGStreamerElementsException{
    createTee();
    linkSinks();
  }

  private void createTee() throws UnableToLinkGStreamerElementsException {
    tee = ElementFactory.make(GStreamerElements.TEE, null);
    bin.addMany(srcBin.getBin(), tee);
    linkSrcToTee();
  }
  
  private void linkSrcToTee() throws UnableToLinkGStreamerElementsException{
    if(!Element.linkPads(srcBin.getBin(), SrcBin.GHOST_PAD_NAME, tee, GStreamerProperties.SINK)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, srcBin.getBin(), tee);
    }
  }
  
  private void linkSinks() throws UnableToLinkGStreamerElementsException{
    for(SinkBin sinkBin : sinkBins){
        addSinkToBin(sinkBin);
        linkTeeToSink(sinkBin);
    }
  }
  
  private void addSinkToBin(SinkBin sinkBin) {
    bin.add(sinkBin.getBin());
  }
  
  private void linkTeeToSink(SinkBin sinkBin) throws UnableToLinkGStreamerElementsException{
    // For each sink we need to request a new pad from the tee so that we can connect the two. The template for creating
    // a new pad is src%d and %d becomes the next int after the number of sinks already connected to the tee
    Pad newPad = tee.getRequestPad(GStreamerProperties.SRCTEMPLATE);
    Element queue = ElementFactory.make(GStreamerElements.QUEUE, null);
    bin.add(queue);
    if(!Element.linkPads(tee, newPad.getName(), queue, GStreamerProperties.SINK)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, tee, queue);
    }
    else if(!Element.linkPads(queue, GStreamerProperties.SRC, sinkBin.getBin(), SinkBin.GHOST_PAD_NAME)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, sinkBin.getBin());
    }
  }

  public Bin getBin(){
    return bin;
  }
}
