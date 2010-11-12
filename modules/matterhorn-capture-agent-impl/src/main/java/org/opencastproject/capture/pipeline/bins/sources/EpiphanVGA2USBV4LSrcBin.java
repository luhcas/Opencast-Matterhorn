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

import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;

import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.State;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBin;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class EpiphanVGA2USBV4LSrcBin extends V4LSrcBin {
  // This was never set to anything else, should be moved to a constant?
  public static int v4lsrc_index = 0;
  
  private CaptureAgent captureAgent = null;
  private Element v4lsrc;
  private Element v4l_identity;
  private Pad v4l_identity_sink_pad;
  private Element videotestsrc;
  private Element resolutionCapsfilter;
  private Element static_identity;
  private Element selector;
  private Element segment;
  private Element ffmpegcolorspace;
  
  /**
   * Adds a pipeline specifically designed to captured from the Epiphan VGA2USB cards to the main pipeline
   * 
   * @param captureDevice
   *          The VGA2USB {@code CaptureDevice} to create a source out of
   * @param properties
   *          The {@code Properties} of the confidence monitoring. 
   * @param captureAgent
   *          The {@code CaptureAgent} that we will use to create the event 
   *          probe for our source bin that will let us swap out for our test
   *          signal in case the Epiphan card disapears. 
   * @throws Exception - If our elements fail to link together we will throw an exception.
   */
  public EpiphanVGA2USBV4LSrcBin(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgent) throws Exception {
    super(captureDevice, properties);
    this.captureAgent = captureAgent;
    setEventProbe();
    checkVGASignalAtStartUp(selector);
    createThreadToCheckNewVGASignal();
  }
  
  /** Used to add detection of an EOS event for testing purposes. Not used in production **/
  private void addBusLevelTracing(){
    Bus bus = v4lsrc.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject v4lsrc) {
        logger.debug("Received EOS event from {}", v4lsrc.getName());
        Element src = (Element) v4lsrc;
        src.setState(State.NULL);
      }
    });
  }
  
  /** Add an event probe to the v4l_identity elements so that we can catch the
   *  EOS that propagates when the signal is lost and switch it out for the
   *  backup video source
   */
  private void setEventProbe() {
    v4l_identity_sink_pad.addEventProbe(new EpiphanVGA2USBV4LEventProbe(captureAgent, captureDevice, bin));
  }

  /** Create all of the Elements that we will need to use for our Epiphan source **/
  protected void createElements(){
    super.createElements();
    // The source for the epiphan card
    v4lsrc = ElementFactory.make("v4lsrc", "v4lsrc_" + captureDevice.getLocation() + "_" + v4lsrc_index);
    // An identity to stick between the epiphan card and the rest of the pipeline so that we can detect a disconnect.
    v4l_identity = ElementFactory.make("identity", captureDevice.getLocation() + "_v4l_identity");
    // The pad through which to catch the disconnection
    v4l_identity_sink_pad = v4l_identity.getStaticPad("sink");
    // Elements that enable VGA signal hotswapping
    videotestsrc = ElementFactory.make("videotestsrc", null);
    resolutionCapsfilter = setupResolutionFilter();
    static_identity = ElementFactory.make("identity", captureDevice.getLocation() + "_static_identity");
    // The input-selector which allows us to choose which source we want to capture from
    selector = ElementFactory.make("input-selector", captureDevice.getLocation() + "_selector");
    segment = ElementFactory.make("identity", captureDevice.getLocation() + "_identity-segment");
    ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
  }
  
  /** Set the element properties for our Epiphan source **/
  protected void setElementProperties(){
    v4lsrc.set("device", captureDevice.getLocation());
    v4l_identity.set("sync", true);
    videotestsrc.set("pattern", 0);
    // Make the test source live, or images just queue up and reconnecting won't work
    videotestsrc.set("is-live", true);
    // Tell identity elements to be false, this shouldn't be required as it is the default
    v4l_identity.set("sync", false);
    static_identity.set("sync", false);
    segment.set("sync", false);
  }
  
  /** Add all of our elements to the source bin. **/
  @Override
  protected void addElementsToBin(){
    bin.addMany(v4lsrc, v4l_identity, queue, videotestsrc, resolutionCapsfilter, 
            static_identity, selector, segment, videorate, fpsfilter, 
            ffmpegcolorspace);
  }
  
  /** Return the ffmpegcolorspace as the sink for our Epiphan source bin to be 
   *  used to create ghost pads to connect this source bin to the tee in 
   *  {@code CaptureDeviceBin}. 
   */
  @Override
  public Pad getSrcPad() {
    return ffmpegcolorspace.getStaticPad("src");
  } 
  
  /** Create a capabilities filter that will allow us to set the videotestsrc
   *  resolution to the same as the Epiphan capture size. When we swap 
   *  out the Epiphan card for the videotestsrc they will be the same size. 
   * @return {@code Element} that will be a capsfilter at the right resolution if possible, otherwise 1280x720.  
   */
  private Element setupResolutionFilter() {
    Element capsfilter = ElementFactory.make("capsfilter", null);
    try {
      V4LInfo v4linfo = JV4LInfo.getV4LInfo(captureDevice.getLocation());
      int width = v4linfo.getVideoCapability().getMaxwidth();
      int height = v4linfo.getVideoCapability().getMaxheight();
      capsfilter.set("caps", Caps.fromString("video/x-raw-yuv, width=" + width + ", height=" + height));
    } catch (JV4LInfoException e) {
      capsfilter.set("caps", Caps.fromString("video/x-raw-yuv, width=1280, height=720"));
      logger.error("Could not get resolution Epiphan device is outputting: {}", e.getLocalizedMessage());
    }
    return capsfilter;
  }

  private String addBackupTestSource(String error) {
    // Add backup video source in case we lose the VGA signal
    if (!videotestsrc.link(resolutionCapsfilter))
      error = CaptureDeviceBin.formatBinError(captureDevice, videotestsrc, resolutionCapsfilter);
    else if (!resolutionCapsfilter.link(static_identity))
      error = CaptureDeviceBin.formatBinError(captureDevice, resolutionCapsfilter, static_identity);
    else if (!static_identity.link(selector))
      error = CaptureDeviceBin.formatBinError(captureDevice, static_identity, selector);
    return error;
  }
  
  /*@Override
  protected String linkElements() {
    String error = null;
    if (!v4lsrc.link(v4l_identity))
      error = CaptureDeviceBin.formatBinError(captureDevice, v4lsrc, v4l_identity);
    else if (!v4l_identity.link(queue))
      error = CaptureDeviceBin.formatBinError(captureDevice, v4l_identity, queue);
    else if (!queue.link(selector))
      error = CaptureDeviceBin.formatBinError(captureDevice, queue, selector);
    //else
      //error = addBackupTestSource(error, selector, segment, videorate, fpsfilter);
    if (captureDeviceProperties.confidence) {
      boolean trace = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_DEBUG));
      if (!VideoMonitoring.addVideoMonitor(bin, fpsfilter, ffmpegcolorspace, confidenceMonitoringProperties.interval, confidenceMonitoringProperties.imageloc, confidenceMonitoringProperties.device, trace))
        error = CaptureDeviceBin.formatBinError(captureDevice, fpsfilter, ffmpegcolorspace);
    } else {
      if (!fpsfilter.link(ffmpegcolorspace))
        error = CaptureDeviceBin.formatBinError(captureDevice, fpsfilter, ffmpegcolorspace);
    }
    //error = addBackupTestSource(error, ffmpegcolorspace, encoder, muxer, filesink);
    return error;
  }*/
  
  /** Link all of the elements we need for an Epiphan source together 
   * @throws Exception - if any of the Elements cannot be linked together.
   * **/
  @Override
  protected void linkElements() throws Exception{
    if (!v4lsrc.link(v4l_identity))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4lsrc, v4l_identity);
    else if (!v4l_identity.link(queue))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4l_identity, queue);
    else if (!queue.link(selector))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, selector);
    else if (!selector.link(segment))
      throw new UnableToLinkGStreamerElementsException(captureDevice, selector, segment);
    else if (!segment.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, segment, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    else if (!fpsfilter.link(ffmpegcolorspace))
      throw new UnableToLinkGStreamerElementsException(captureDevice, fpsfilter, ffmpegcolorspace);
    // Add backup video source in case we lose the VGA signal
    else if (!videotestsrc.link(resolutionCapsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videotestsrc, resolutionCapsfilter);
    else if (!resolutionCapsfilter.link(static_identity))
      throw new UnableToLinkGStreamerElementsException(captureDevice, resolutionCapsfilter, static_identity);
    else if (!static_identity.link(selector))
      throw new UnableToLinkGStreamerElementsException(captureDevice, static_identity, selector);
  }

  /** Checks to see if the Epiphan card is present, if not we will use the videotestsrc instead. **/
  private void checkVGASignalAtStartUp(Element selector) {
    // Check it see if there is a VGA signal on startup
    if (check_epiphan(captureDevice.getLocation())) {
      useEpiphanAsSource(selector);
    } else {
      doNotUseEpiphanAsSource(selector);
    }
  }

  private void useEpiphanAsSource(Element selector) {
    logger.debug("Have signal on startup");
    Pad new_pad = selector.getStaticPad("sink0");
    selector.set("active-pad", new_pad);
  }

  private void doNotUseEpiphanAsSource(Element selector) {
    // No VGA signal on start up, remove Epiphan source
    /** Adam - He didn't seem to be using broken broken = true; **/
    Element src = bin.getElementByName("v4lsrc_" + captureDevice.getLocation() + "_" + v4lsrc_index);
    bin.remove(src);
    Pad new_pad = selector.getStaticPad("sink1");
    selector.set("active-pad", new_pad);
  }

  private void createThreadToCheckNewVGASignal() {
    Thread poll = new Thread(new PollEpiphan(bin, captureDevice.getLocation()));
    poll.start();
  }
  
  /**
   * When we have lost a VGA signal, this method can be continually executed
   * to test for a new signal.
   * 
   * @param device the absolute path to the device
   * @return true iff there is a VGA signal
   */
  protected static synchronized boolean check_epiphan(String device) {
    try {
      V4LInfo v4linfo = JV4LInfo.getV4LInfo(device);
      String deviceName = v4linfo.getVideoCapability().getName();
      if (deviceName.equals("Epiphan VGA2USB")) {
        return true;
      }
    } catch (JV4LInfoException e) {
      return false;
    }
    return false;
  }

}
