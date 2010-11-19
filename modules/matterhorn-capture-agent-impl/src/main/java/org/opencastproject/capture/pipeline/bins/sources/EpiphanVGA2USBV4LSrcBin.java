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

import org.gstreamer.Bin;
import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Event;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.State;
import org.gstreamer.Pad.EVENT_PROBE;
import org.gstreamer.event.EOSEvent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpiphanVGA2USBV4LSrcBin extends V4LSrcBin {
  // This was never set to anything else, should be moved to a constant?
  public static int v4lsrcIndex = 0;
  
  private CaptureAgent captureAgent = null;
  private Element v4lsrc;
  private Element v4lIdentity;
  private Pad v4lIdentitySinkPad;
  private Element videotestsrc;
  private Element resolutionCapsfilter;
  private Element staticIdentity;
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
   * @throws UnableToSetElementPropertyBecauseElementWasNullException 
   * @throws UnableToCreateGhostPadsForBinException 
   * @throws UnableToLinkGStreamerElementsException 
   * @throws CaptureDeviceNullPointerException 
   * @throws UnableToCreateElementException 
   * @throws Exception - If our elements fail to link together we will throw an exception.
   */
  public EpiphanVGA2USBV4LSrcBin(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgent)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
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
    v4lIdentitySinkPad.addEventProbe(new EpiphanVGA2USBV4LEventProbe(captureAgent, captureDevice, bin));
  }

  /** Create all of the Elements that we will need to use for our Epiphan source 
   * @throws UnableToCreateElementException **/
  protected void createElements() throws UnableToCreateElementException{
    super.createElements();
    // The source for the epiphan card
    /**
     * TODO - Get rid of all static names that will cause us all sorts of grief if we ever have more than one kicking
     * around
     **/
    v4lsrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.V4LSRC, "v4lsrc_" + captureDevice.getLocation() + "_" + v4lsrcIndex);
    // An identity to stick between the epiphan card and the rest of the pipeline so that we can detect a disconnect.
    v4lIdentity = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.IDENTITY, captureDevice.getLocation() + "_v4l_identity");
    // The pad through which to catch the disconnection
    v4lIdentitySinkPad = v4lIdentity.getStaticPad(GStreamerProperties.SINK);
    // Elements that enable VGA signal hotswapping
    videotestsrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.VIDEOTESTSRC, null);
    resolutionCapsfilter = setupResolutionFilter();
    staticIdentity = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.IDENTITY, captureDevice.getLocation() + "_static_identity");
    // The input-selector which allows us to choose which source we want to capture from
    selector = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.INPUT_SELECTOR, captureDevice.getLocation() + "_selector");
    segment = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.IDENTITY, captureDevice.getLocation() + "_identity-segment");
    ffmpegcolorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, null);
  }
  
  /** Set the element properties for our Epiphan source **/
  protected void setElementProperties(){
    v4lsrc.set(GStreamerProperties.DEVICE, captureDevice.getLocation());
    v4lIdentity.set(GStreamerProperties.SYNC, true);
    videotestsrc.set(GStreamerProperties.PATTERN, 0);
    // Make the test source live, or images just queue up and reconnecting won't work
    videotestsrc.set(GStreamerProperties.IS_LIVE, true);
    // Tell identity elements to be false, this shouldn't be required as it is the default
    v4lIdentity.set(GStreamerProperties.SYNC, false);
    staticIdentity.set(GStreamerProperties.SYNC, false);
    segment.set(GStreamerProperties.SYNC, false);
  }
  
  /** Add all of our elements to the source bin. **/
  @Override
  protected void addElementsToBin(){
    bin.addMany(v4lsrc, v4lIdentity, queue, videotestsrc, resolutionCapsfilter, 
            staticIdentity, selector, segment, videorate, fpsfilter, 
            ffmpegcolorspace);
  }
  
  /** Return the ffmpegcolorspace as the sink for our Epiphan source bin to be 
   *  used to create ghost pads to connect this source bin to the tee in 
   *  {@code CaptureDeviceBin}. 
   */
  @Override
  public Pad getSrcPad() {
    return ffmpegcolorspace.getStaticPad(GStreamerProperties.SRC);
  } 
  
  /** Create a capabilities filter that will allow us to set the videotestsrc
   *  resolution to the same as the Epiphan capture size. When we swap 
   *  out the Epiphan card for the videotestsrc they will be the same size. 
   * @return {@code Element} that will be a capsfilter at the right resolution if possible, otherwise 1280x720.  
   */
  private Element setupResolutionFilter() {
    Element capsfilter = ElementFactory.make(GStreamerElements.CAPSFILTER, null);
    try {
      V4LInfo v4linfo = JV4LInfo.getV4LInfo(captureDevice.getLocation());
      int width = v4linfo.getVideoCapability().getMaxwidth();
      int height = v4linfo.getVideoCapability().getMaxheight();
      capsfilter.set(GStreamerProperties.CAPS, Caps.fromString(GStreamerProperties.VIDEO_X_RAW_YUV + ", "
              + GStreamerProperties.WIDTH + "=" + width + ", " + GStreamerProperties.HEIGHT + "=" + height));
    } catch (JV4LInfoException e) {
      capsfilter.set(GStreamerProperties.CAPS, Caps.fromString(GStreamerProperties.VIDEO_X_RAW_YUV + ", "
              + GStreamerProperties.WIDTH + "=1280, " + GStreamerProperties.HEIGHT + "=720"));
      logger.error("Could not get resolution Epiphan device is outputting: {}", e.getLocalizedMessage());
    }
    return capsfilter;
  }

  private String addBackupTestSource(String error) {
    // Add backup video source in case we lose the VGA signal
    if (!videotestsrc.link(resolutionCapsfilter))
      error = formatBinError(captureDevice, videotestsrc, resolutionCapsfilter);
    else if (!resolutionCapsfilter.link(staticIdentity))
      error = formatBinError(captureDevice, resolutionCapsfilter, staticIdentity);
    else if (!staticIdentity.link(selector))
      error = formatBinError(captureDevice, staticIdentity, selector);
    return error;
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
  
  /** Link all of the elements we need for an Epiphan source together 
   * @throws Exception - if any of the Elements cannot be linked together.
   * **/
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException{
    if (!v4lsrc.link(v4lIdentity))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4lsrc, v4lIdentity);
    else if (!v4lIdentity.link(queue))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4lIdentity, queue);
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
    else if (!resolutionCapsfilter.link(staticIdentity))
      throw new UnableToLinkGStreamerElementsException(captureDevice, resolutionCapsfilter, staticIdentity);
    else if (!staticIdentity.link(selector))
      throw new UnableToLinkGStreamerElementsException(captureDevice, staticIdentity, selector);
  }

  /** Checks to see if the Epiphan card is present, if not we will use the videotestsrc instead. **/
  private void checkVGASignalAtStartUp(Element selector) {
    // Check it see if there is a VGA signal on startup
    if (checkEpiphan(captureDevice.getLocation())) {
      useEpiphanAsSource(selector);
    } else {
      doNotUseEpiphanAsSource(selector);
    }
  }

  private void useEpiphanAsSource(Element selector) {
    logger.debug("Have signal on startup");
    Pad newPad = selector.getStaticPad("sink0");
    selector.set("active-pad", newPad);
  }

  private void doNotUseEpiphanAsSource(Element selector) {
    // No VGA signal on start up, remove Epiphan source
    /** Adam - He didn't seem to be using broken broken = true; **/
    Element src = bin.getElementByName("v4lsrc_" + captureDevice.getLocation() + "_" + v4lsrcIndex);
    bin.remove(src);
    Pad newPad = selector.getStaticPad("sink1");
    selector.set("active-pad", newPad);
  }

  private void createThreadToCheckNewVGASignal() {
    Thread poll = new Thread(new PollEpiphan(bin, captureDevice.getLocation()));
    poll.start();
  }
  /** TODO - Make this part platform independent **/
  /**
   * When we have lost a VGA signal, this method can be continually executed
   * to test for a new signal.
   * 
   * @param device the absolute path to the device
   * @return true iff there is a VGA signal
   */
  protected static synchronized boolean checkEpiphan(String device) {
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



  public class EpiphanVGA2USBV4LEventProbe implements EVENT_PROBE {
    protected final Logger logger = LoggerFactory.getLogger(EpiphanVGA2USBV4LEventProbe.class);
    private CaptureAgent captureAgent;
    private CaptureDevice captureDevice;
    private Bin bin;
    private boolean broken = false;

    public EpiphanVGA2USBV4LEventProbe(CaptureAgent captureAgent, CaptureDevice captureDevice, Bin bin) {
      this.captureAgent = captureAgent;
      this.captureDevice = captureDevice;
      this.bin = bin;
    }

    public boolean isBroken() {
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
          // return true;
          return false; // TODO: this is insane

        }
        logger.debug("EOS event received, state is not shutting down: Lost VGA signal. ");

        // Sanity check, if we have already identified this as broken no need to unlink the elements
        if (broken) {
          // return false;
          return true; // TODO: this is insane
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
        // return false;
        return true; // TODO: this is insane
      }
      // return true;
      return false; // TODO: this is insane
    }

  }
}
