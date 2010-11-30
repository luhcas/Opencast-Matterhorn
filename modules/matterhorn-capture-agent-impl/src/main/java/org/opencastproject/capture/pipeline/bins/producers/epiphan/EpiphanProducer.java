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
package org.opencastproject.capture.pipeline.bins.producers.epiphan;

import com.sun.jna.Pointer;
import java.util.Properties;
import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;
import org.gstreamer.Buffer;
import org.gstreamer.Bus.STATE_CHANGED;
import org.gstreamer.Caps;
import org.gstreamer.ClockTime;
import org.gstreamer.Element;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.State;
import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.AppSrc;
import org.gstreamer.elements.AppSrc.NEED_DATA;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.opencastproject.capture.pipeline.bins.producers.V4LProducer;

public class EpiphanProducer extends V4LProducer {

  /** The default resolution (width). */
  public static final int DEFAULT_CAPTURE_WIDTH = 1024;
  /** The default resolution (height). */
  public static final int DEFAULT_CAPTURE_HEIGHT = 768;
  /** The default framerate. */
  public static final int DEFAULT_CAPTURE_FRAMERATE = 25;


  EpiphanSubDeviceBin deviceBin;
  EpiphanSubBin subBin;

  Thread epiphanPoll;

  Element identity, videorate, colorspace;
  AppSrc src;
  String caps;

  /**
   * Adds a pipeline specifically designed to captured from the Epiphan VGA2USB cards to the main pipeline.
   * This extended SrcBin creates an epiphan- and a secondary (e.g. VideoTestSrc) sub-pipelines.
   * By loosing a vga signal the buffer will be get from secondary pipeline and epiphan will be restored automaticly,
   * if the vga is reconnected.
   *
   * @param captureDevice
   *          The VGA2USB {@code CaptureDevice} to create a source out of
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   * @throws UnableToCreateGhostPadsForBinException
   * @throws UnableToLinkGStreamerElementsException
   * @throws CaptureDeviceNullPointerException
   * @throws UnableToCreateElementException
   */
  public EpiphanProducer(CaptureDevice captureDevice, Properties properties)throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    
    super(captureDevice, properties);

    // creates Epiphan-Sub-Pipeline
    deviceBin = new EpiphanSubDeviceBin(captureDevice, getCaps());
    // creates secondary Sub-Pipeline
    subBin = new EpiphanSubTestSrcBin(captureDevice, getCaps());
    // link AppSrc from primary pipeline with AppSink's from secondary pipelines
    linkAppSrcToSink();
    // link StateChange from primary to start and stop secondary pipelines
    linkSubPipeline();

    // start epiphan polling thread
    epiphanPoll = new EpiphanPoll(captureDevice.getLocation());
  }

  /**
   * Create elements.
   */
  @Override
  protected void createElements() throws UnableToCreateElementException {
    GStreamerElements elements;
    src = (AppSrc) GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.APPSRC, null);
    identity = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.IDENTITY, null);
    colorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.FFMPEGCOLORSPACE, null);
    videorate = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.VIDEORATE, null);
  }

  /**
   * Set element properties.
   */
  @Override
  protected void setElementProperties() {
    src.set(GStreamerProperties.IS_LIVE, "true");
    src.set(GStreamerProperties.DO_TIMESTAP, "true");
    src.set(GStreamerProperties.BLOCK, "true");
    src.setStreamType(AppSrc.Type.STREAM);
    src.setCaps(Caps.fromString(getCaps()));
    identity.set(GStreamerProperties.SINGLE_SEGMENT, "true");
  }

  /**
   * Add all elements to bin.
   */
  @Override
  protected void addElementsToBin() {
    bin.addMany(src, identity, videorate, colorspace);
  }

  /**
   * Link elements.
   * @throws UnableToLinkGStreamerElementsException if Elements can not connected to each other.
   */
  @Override
  public void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!src.link(identity)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, identity);
    }
    if (!identity.link(videorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, identity, videorate);
    }
    if (!videorate.link(colorspace)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, colorspace);
    }
  }

  /**
   * Returns SrcPad from last element in the pipeline.
   * @return
   */
  @Override
  public Pad getSrcPad() {
    return colorspace.getStaticPad(GStreamerProperties.SRC);
  }

  /**
   * Returns caps.
   * @return Caps
   */
  protected String getCaps() {

    if (caps == null || caps.isEmpty()) {
      try {
        V4LInfo v4linfo = JV4LInfo.getV4LInfo(captureDevice.getLocation());
        int width = v4linfo.getVideoCapability().getMaxwidth();
        int height = v4linfo.getVideoCapability().getMaxheight();
        caps = GStreamerProperties.VIDEO_X_RAW_YUV+", "+
               GStreamerProperties.WIDTH+"="+width+", "+
               GStreamerProperties.HEIGHT+"="+height+
               ", format=(fourcc)I420, "+      // the last part is needed by AppSrc
               GStreamerProperties.FRAMERATE+"="+DEFAULT_CAPTURE_FRAMERATE+"/1";
        
      } catch (JV4LInfoException e) {
        caps = GStreamerProperties.VIDEO_X_RAW_YUV+", "+
               GStreamerProperties.WIDTH+"="+DEFAULT_CAPTURE_WIDTH+", "+
               GStreamerProperties.HEIGHT+"="+DEFAULT_CAPTURE_HEIGHT+
               ", format=(fourcc)I420, "+      // the last part is needed by AppSrc
               GStreamerProperties.FRAMERATE+"="+DEFAULT_CAPTURE_FRAMERATE+"/1";
      }
    }
    return caps;
  }

  /**
   * Link AppSink from sub pipelines to AppSrc from primary pipeline.
   */
  protected void linkAppSrcToSink() {
    src.connect(new NEED_DATA() {

      @Override
      public void needData(Element elem, int size, Pointer userData) {
        AppSrc src = (AppSrc) elem;
        AppSink sink = null;
        Buffer buffer = null;
        try{
          // try to get buffer from epiphan pipeline
          sink = deviceBin.getSink();
          if (sink == null) throw new NullPointerException("AppSink is null");
          if (sink.isEOS()) throw new IllegalStateException("AppSink is EOS");
          buffer = sink.pullBuffer();
          if (buffer == null) throw new NullPointerException("Buffer is null");
        } catch (Exception ex) {
          //logger.debug(ex.getMessage());
          // epiphan pipeline is down, try to get buffer from testsrc pipeline
          sink = subBin.getSink();
          if (sink == null) src.endOfStream();
          buffer = sink.pullBuffer();
          if (buffer == null) {
            src.endOfStream();
            return;
          }
        }
        buffer.setTimestamp(ClockTime.NONE);
        src.pushBuffer(buffer);
        buffer.dispose();
      }
    });
  }

  /**
   * Set state control. Starts sub pipelines, if primary started and stop these,
   * if primary will stopping.
   */
  protected void linkSubPipeline() {
    src.getBus().connect(new STATE_CHANGED() {

      @Override
      public void stateChanged(GstObject source, State old, State current, State pending) {

        if (source == src) {

          if (old == State.NULL) {
            // start sub pipelines
            subBin.start(-1);
            deviceBin.start(5);
            epiphanPoll.start();
          }

          if (current == State.NULL) {
            // stop sub pipelines
            epiphanPoll.interrupt();
            subBin.stop();
            deviceBin.stop();
          }
        }
      }
    });
  }

  /** TODO - Make this part platform independent **/
  /**
   * When we have lost a VGA signal, this method can be continually executed
   * to test for a new signal.
   *
   * @param device the absolute path to the device
   * @return true if there is a VGA signal
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

  /**
   * Epiphan polling thread.
   * Restore Epiphan sub pipeline, if the signal has been reconnected.
   */
  class EpiphanPoll extends Thread {

    private static final int DELAY_BETWEEN_POLLS = 1000;

    /** Location of the Epiphan device */
    private String location;

    public EpiphanPoll(String location) {
      this.location = location;
    }

    public void run() {

      logger.debug("Start Epiphan VGA2USB polling thread!");
      
      while(!interrupted()) {
        if (deviceBin.isBroken() && checkEpiphan(location)) {
          try {
            EpiphanSubDeviceBin newBin = new EpiphanSubDeviceBin(captureDevice, caps);
            if (!newBin.start(5)) {
              newBin.stop();
              logger.debug("Can not start Epiphan VGA2USB pipeline!");
            } else {
              deviceBin = newBin;
              logger.debug("Epiphan VGA2USB pipeline restored!");
            }
          } catch (UnableToLinkGStreamerElementsException ex) {
            logger.error("Can not create epiphan bin!", ex);
          } catch (UnableToCreateElementException ex) {
            logger.error("Can not create epiphan bin!", ex);
          }
        }
        
        try {
          sleep(DELAY_BETWEEN_POLLS);
        } catch (InterruptedException ex) {
          interrupt();
        }
      }
      logger.debug("Shutting down Epiphan VGA2USB polling thread!");
    }
  }
}
