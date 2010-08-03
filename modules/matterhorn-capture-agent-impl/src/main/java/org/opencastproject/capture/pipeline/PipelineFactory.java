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
package org.opencastproject.capture.pipeline;

import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.CaptureAgentImpl;

import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;

import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Event;
import org.gstreamer.Gst;
import org.gstreamer.Message;
import org.gstreamer.MessageType;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.Pipeline;
import org.gstreamer.event.EOSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Given a Properties object describing devices this class will create a suitable pipeline to capture from all those
 * devices simultaneously.
 */
public class PipelineFactory {

  protected static final Logger logger = LoggerFactory.getLogger(PipelineFactory.class);

  private static Properties properties;
  
  protected static boolean broken;
  
  protected static int v4lsrc_index;
  
  protected static CaptureAgentImpl captureAgent = null;

  /**
   * Create a bin that contains multiple pipelines using each source in the properties object as the gstreamer source
   * 
   * @param props
   *          {@code Properties} object defining sources 
   * @return The {@code Pipeline} to control the pipelines
   * @throws UnsupportedDeviceException
   */
  public static Pipeline create(Properties props, boolean confidence, CaptureAgentImpl ca) {
    properties = props;
    captureAgent = ca;
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();

    String[] friendlyNames = getDeviceNames();
    if (friendlyNames == null) {
      return null;
    }

    String outputDirectory = properties.getProperty(CaptureParameters.RECORDING_ROOT_URL);

    devices = initDevices(friendlyNames, outputDirectory, confidence);
    if (devices == null) {
      return null;
    }

    return startPipeline(devices, confidence);
  }

  /**
   * Splits the device names from the pipeline's properties.
   * @return The device names to capture from.
   */
  private static String[] getDeviceNames() {
    // Setup pipeline for all the devices specified
    String deviceNames = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (deviceNames == null) {
      logger.error("No capture devices specified in {}.", CaptureParameters.CAPTURE_DEVICE_NAMES);
      return null;
    }

    //Sanity checks for the device list
    String[] friendlyNames = deviceNames.split(",");
    if (friendlyNames.length < 1) {
      logger.error("Insufficient number of capture devices listed.  Aborting!");
      return null;
    } else if (friendlyNames.length == 1) {
      //Java gives us an array even if the string being split is blank...
      if (friendlyNames[0].trim().equals("")) {
        logger.error("Invalid capture device listed.  Aborting!");
        return null;
      }
    }

    return friendlyNames;
  }

  /**
   * Returns an {@code ArrayList} of {@code CaptureDevice}s which contain everything the rest of this class needs to start the pipeline
   * @param friendlyNames  The list of friendly names we will be capturing from.
   * @param outputDirectory  The destination directory of the captures.
   * @param confidence  True to enable confidence monitoring, false otherwise.
   * @return A list of {@code CaptureDevice}s which can be captured from.
   */
  private static ArrayList<CaptureDevice> initDevices(String[] friendlyNames, String outputDirectory, boolean confidence) {
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();

    for (String name : friendlyNames) {
      name = name.trim();
      DeviceName devName;
    
      // Get properties from the configuration
      String srcProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_SOURCE;
      String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_DEST;
      if (outputDirectory == null && confidence == false) {
        logger.warn("Output directory is null, this may not work because we may not be able to write to the current output dir!");
      }
      if (!properties.containsKey(outputProperty)) {
        logger.error("Invalid device name: {}.  No keys named {} exist in the properties!", name, CaptureParameters.CAPTURE_DEVICE_PREFIX  + name);
        return null;
      }
      String srcLoc = properties.getProperty(srcProperty);
      File outputFile = new File(outputDirectory, properties.getProperty(outputProperty));
      logger.debug("Device {} has source at {}.", name, srcLoc);
      logger.debug("Device {} has output at {}.", name, outputFile);
    
      //Only try and create an output file if this pipeline will *not* be used for confidence monitoring
      if (!confidence) {
        try {
          if(!outputFile.createNewFile()){
            logger.error("Could not create ouput file for {}, file may already exist.", name);
            return null;
          }
        } catch (IOException e) {
          logger.error("An error occured while creating output file for {}. {}", name, e.getMessage());
          return null;
        }
      }
      String outputLoc = outputFile.getAbsolutePath();
          
    
      if (srcLoc == null) {
        logger.error("Unable to create pipeline for {} because its source file/device does not exist!", name);
        return null;
      }
    
      if (new File(srcLoc).isFile()) {
        // Non-V4L file. If it exists, assume it is ingestable
        // TODO: Fix security risk. Any file on CaptureAgent filesytem could be ingested
        devName = DeviceName.FILE;
      } else {
        // ALSA source
        if (srcLoc.contains("hw:")){
          devName = DeviceName.ALSASRC;
        } else if (srcLoc.equals("dv1394")) {
          devName = DeviceName.DV_1394;
        } else { // V4L devices
          // Attempt to determine what the device is using the JV4LInfo library 
          try {
            V4LInfo v4linfo = JV4LInfo.getV4LInfo(srcLoc);
            String deviceString = v4linfo.toString();
            if (deviceString.contains("Epiphan VGA2USB"))
              devName = DeviceName.EPIPHAN_VGA2USB;
            else if (deviceString.contains("Hauppauge") || deviceString.contains("WinTV"))
              devName = DeviceName.HAUPPAUGE_WINTV;
            else if (deviceString.contains("BT878"))
              devName = DeviceName.BLUECHERRY_PROVIDEO;
            else {
              logger.error("Do not recognized device: {}.", srcLoc);
              return null;
            }
          } catch (JV4LInfoException e) {
            // The v4l device caused an exception
            if (e.getMessage().equalsIgnoreCase("No medium found")) {
              logger.warn("No VGA signal detected. Trying to start capture regardless...");
              devName = DeviceName.EPIPHAN_VGA2USB;
            } else {
              logger.error("Unexpected jv4linfo exception: {} for {}.", e.getMessage(), srcLoc);
              return null;
            }
          }
        }
      }
    
      // devices will store the CaptureDevice list arbitrary order
      CaptureDevice capdev = createCaptureDev(srcLoc, devName, name, outputLoc);
      if (!devices.add(capdev)) {
        logger.error("Unable to add device: {}.", capdev);
      }
    }

    return devices;
  }

  /**
   * Initializes the pipeline itself, but does not start capturing
   * 
   * @param devices  The list of devices to capture from.
   * @param confidence  True to enable confidence monitoring.
   * @return The created {@code Pipeline}, or null in the case of an error.
   */
  private static Pipeline startPipeline(ArrayList<CaptureDevice> devices, boolean confidence) {
    logger.info("Successfully initialised {} devices.", devices.size());
    for (int i = 0; i < devices.size(); i++)
      logger.debug("Device #{}: {}.", i, devices.get(i));

    // setup gstreamer pipeline using capture devices 
    Gst.init(); // cannot using gst library without first initialising it

    Pipeline pipeline = new Pipeline();
    
    if (confidence) {
      for (CaptureDevice c : devices) {
        if (c.getName() == DeviceName.ALSASRC)
          AudioMonitoring.getConfidencePipeline(pipeline, c, properties);
        else
          VideoMonitoring.getConfidencePipeline(pipeline, c, properties);
      }
    } else {
      for (CaptureDevice c : devices) {
        if (!addPipeline(c, pipeline))
          logger.error("Failed to create pipeline for {}.", c);
      }
    }

    return pipeline;
  }

  //TODO:  Document me!
  private static CaptureDevice createCaptureDev(String srcLoc, DeviceName devName, String name, String outputLoc) {
    CaptureDevice capdev = new CaptureDevice(srcLoc, devName, name, outputLoc);
    String codecProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_CODEC;
    String containerProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_CONTAINER;
    String bitrateProperty = codecProperty + CaptureParameters.CAPTURE_DEVICE_BITRATE;
    String quantizerProperty = codecProperty + CaptureParameters.CAPTURE_DEVICE_QUANTIZER;
    String bufferProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_BUFFER;
    String bufferCountProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_BUFFERS;
    String bufferByteProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_BYTES;
    String bufferTimeProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_TIME;
    String framerateProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_FRAMERATE;
    String codec = properties.getProperty(codecProperty);
    String container = properties.getProperty(containerProperty);
    String bitrate = properties.getProperty(bitrateProperty);
    String quantizer = properties.getProperty(quantizerProperty);
    String bufferCount = properties.getProperty(bufferCountProperty);
    String bufferBytes = properties.getProperty(bufferByteProperty);
    String bufferTime = properties.getProperty(bufferTimeProperty);
    String framerate = properties.getProperty(framerateProperty);

    if (codec != null)
      capdev.properties.setProperty("codec", codec);
    if (bitrate != null)
      capdev.properties.setProperty("bitrate", bitrate);
    if (quantizer != null)
      capdev.properties.setProperty("quantizer", quantizer);
    if (container != null)
      capdev.properties.setProperty("container", container);
    if (bufferCount != null)
      capdev.properties.setProperty("bufferCount", bufferCount);
    if (bufferBytes != null)
      capdev.properties.setProperty("bufferBytes", bufferBytes);
    if (bufferTime != null)
      capdev.properties.setProperty("bufferTime", bufferTime);
    if (framerate != null)
      capdev.properties.setProperty("framerate", framerate);

    return capdev;
  }

  /**
   * addPipeline will add a pipeline for the specified capture device to the bin.
   * 
   * @param captureDevice
   *          {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  public static boolean addPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    if (captureDevice.getName() == DeviceName.EPIPHAN_VGA2USB)
      return getVGA2USBPipeline(captureDevice, pipeline);
    else if (captureDevice.getName() == DeviceName.HAUPPAUGE_WINTV)
      return getHauppaugePipeline(captureDevice, pipeline);
    else if(captureDevice.getName() == DeviceName.BLUECHERRY_PROVIDEO)
      return getBluecherryPipeline(captureDevice, pipeline);
    else if (captureDevice.getName() == DeviceName.ALSASRC)
      return getAlsasrcPipeline(captureDevice, pipeline);
    else if(captureDevice.getName() == DeviceName.FILE)
      return getFilePipeline(captureDevice, pipeline);
    else if(captureDevice.getName() == DeviceName.DV_1394)
      return getDvPipeline(captureDevice, pipeline);
    return false;
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
  private static String formatPipelineError(CaptureDevice device, Element src, Element sink) {
    return device.getLocation() + ": " + "(" + src.toString() + ", " + sink.toString() + ")";
  }

  /**
   * Adds a pipeline specifically designed to captured from the Hauppauge WinTv cards to the main pipeline
   * 
   * @param captureDevice
   *          The Hauppauge {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  private static boolean getHauppaugePipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    // confidence monitoring vars
    String imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    String device = new File(captureDevice.getOutputPath()).getName();
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "5"));
    // pipeline vars
    String error = null;
    String codec =  captureDevice.properties.getProperty("codec");
    String container = captureDevice.properties.getProperty("container");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    String framerate = captureDevice.properties.getProperty("framerate");
    String bufferCount = captureDevice.properties.getProperty("bufferCount");
    String bufferBytes = captureDevice.properties.getProperty("bufferBytes");
    String bufferTime = captureDevice.properties.getProperty("bufferTime");
    boolean confidence = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));

    Element dec, enc, muxer;
    Element filesrc = ElementFactory.make("filesrc", null);
    Element queue = ElementFactory.make("queue", captureDevice.getFriendlyName());
    if (bufferCount != null) {
      logger.debug("{} bufferCount set to {}.", captureDevice.getName(), bufferCount);
      queue.set("max-size-buffers", bufferCount);
    }
    if (bufferBytes != null) {
      logger.debug("{} bufferBytes set to {}.", captureDevice.getName(), bufferBytes);
      queue.set("max-size-bytes", bufferBytes);
    }
    if (bufferTime != null) {
      logger.debug("{} bufferTime set to {}.", captureDevice.getName(), bufferTime);
      queue.set("max-size-time", bufferTime);
    }
    Element mpegpsdemux = ElementFactory.make("mpegpsdemux", null);
    final Element mpegvideoparse = ElementFactory.make("mpegvideoparse", null);
    
    // Elements that allow for change in fps
    Element videorate = ElementFactory.make("videorate", null);
    Element fpsfilter = ElementFactory.make("capsfilter", null);
    Caps fpsCaps;
    if (framerate != null) {
      fpsCaps = new Caps("video/x-raw-yuv, framerate=" + framerate + "/1");
      logger.debug("{} fps: {}", captureDevice.getName(), framerate);
    }
    else
      fpsCaps = Caps.anyCaps();
    fpsfilter.setCaps(fpsCaps);
    
    
    if (codec != null && codec.equalsIgnoreCase("ffenc_mpeg2video")) {
      logger.debug("{} using encoder: {}", captureDevice.getName(), codec);
      dec = ElementFactory.make("mpeg2dec", null);
      enc = ElementFactory.make(codec, null);
    }
    else {
      dec = ElementFactory.make("capsfilter", null);
      enc = ElementFactory.make("capsfilter", null);
    }
    
    if (container != null) {
      logger.debug("{} muxing to: {}", captureDevice.getName(), container);
      muxer = ElementFactory.make(container, null);
    }
    else {
      muxer = ElementFactory.make("mpegtsmux", null);
    }
    
    Element filesink = ElementFactory.make("filesink", null);
    filesrc.set("location", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    
    if (bitrate != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), bitrate);
      enc.set("bitrate", bitrate);
    }
    pipeline.addMany(filesrc, queue, mpegpsdemux, mpegvideoparse, dec, videorate, fpsfilter, enc, muxer, filesink);

    /*
     * mpegpsdemux source pad is only available sometimes, therefore we need to add a listener to accept dynamic pads
     */
    mpegpsdemux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element arg0, Pad arg1) {
        arg1.link(mpegvideoparse.getStaticPad("sink"));
      }
    });
    Pad newpad = new Pad(null, PadDirection.SRC);
    if (!filesrc.link(queue))
      error = formatPipelineError(captureDevice, filesrc, queue);
    else if (!queue.link(mpegpsdemux))
      error = formatPipelineError(captureDevice, queue, mpegpsdemux);
    else if (!mpegpsdemux.addPad(newpad))
      error = formatPipelineError(captureDevice, mpegpsdemux, mpegvideoparse);
    else if (!mpegvideoparse.link(dec))
      error = formatPipelineError(captureDevice, mpegvideoparse, dec);
    else if (!dec.link(videorate))
      error = formatPipelineError(captureDevice, dec, videorate);
    else if (!videorate.link(fpsfilter))
      error = formatPipelineError(captureDevice, videorate, fpsfilter);
    if (confidence) {
      boolean trace = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_DEBUG));
      if (!VideoMonitoring.addVideoMonitor(pipeline, fpsfilter, enc, interval, imageloc, device, trace))
        error = formatPipelineError(captureDevice, fpsfilter, enc);
    } else {
      if (!fpsfilter.link(enc))
        error = formatPipelineError(captureDevice, fpsfilter, enc);
    }
    if (!enc.link(muxer))
      error = formatPipelineError(captureDevice, enc, muxer);
    else if (!muxer.link(filesink))
      error = formatPipelineError(captureDevice, muxer, filesink);

    if (error != null) {
      pipeline.removeMany(filesrc, queue, mpegpsdemux, mpegvideoparse, dec, videorate, fpsfilter, enc, muxer, filesink);
      logger.error(error);
      return false;
    }

    if (logger.isTraceEnabled()) {
      BufferThread t = new BufferThread(queue);
      t.start();
    }

    return true;
  }

  // TODO: Rid of checkstyle off
  // CHECKSTYLE:OFF
  /**
   * Adds a pipeline specifically designed to captured from the Epiphan VGA2USB cards to the main pipeline
   * 
   * @param captureDevice
   *          The VGA2USB {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  private static boolean getVGA2USBPipeline(final CaptureDevice captureDevice, final Pipeline pipeline) {
    // confidence vars
    String imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    String device = new File(captureDevice.getOutputPath()).getName();
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "5"));

    String error = null;
    String codec = captureDevice.properties.getProperty("codec");
    String container = captureDevice.properties.getProperty("container");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    String framerate = captureDevice.properties.getProperty("framerate");
    String bufferCount = captureDevice.properties.getProperty("bufferCount");
    String bufferBytes = captureDevice.properties.getProperty("bufferBytes");
    String bufferTime = captureDevice.properties.getProperty("bufferTime");
    boolean confidence = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));
    
    // The codec and container format. Configurable, therefore not initialized. 
    Element enc, muxer;
    
    // The Epiphan card
    v4lsrc_index = 0;
    Element v4lsrc = ElementFactory.make("v4lsrc", "v4lsrc_" + v4lsrc_index);
    /* Bus bus = v4lsrc.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject v4lsrc) {
        logger.debug("Received EOS event from {}", v4lsrc.getName());
        Element src = (Element) v4lsrc;
        src.setState(State.NULL);
      }
    }); */
    Element v4l_identity = ElementFactory.make("identity", "v4l_identity");
    v4l_identity.set("sync", true);
    
    // Add an event probe to the v4l_identity elements so that we can catch the
    // EOS that propagates when the signal is lost and switch it out for the 
    // backup video source
    Pad v4l_identity_sink_pad = v4l_identity.getStaticPad("sink");
    v4l_identity_sink_pad.addEventProbe(new Pad.EVENT_PROBE() {
      /**
       * @return true if we should propagate the EOS down the chain, false otherwise
       */
      public synchronized boolean eventReceived(Pad pad, Event event) {
        logger.debug("Event received: {}", event.toString());
        if (event instanceof EOSEvent) {
          if (captureAgent.getAgentState().equals(AgentState.SHUTTING_DOWN)) {
            synchronized (PollEpiphan.enabled) {
              PollEpiphan.enabled.notify();
            }
            //return true;  
            return false; //TODO: this is insane
            
          }
          logger.debug("EOS event received, state is not shutting down: Lost VGA signal. " );
          
          // Sanity check, if we have already identified this as broken no need to unlink the elements
          if (broken){
            //return false;  
            return true; //TODO: this is insane
          }
          
          // An EOS means the Epiphan source has broken (unplugged)
          broken = true;
          
          // Remove the broken v4lsrc
          Element src = pipeline.getElementByName("v4lsrc_" + v4lsrc_index); 
          src.unlink(pipeline.getElementByName("v4l_identity"));
          pipeline.remove(src);
          
          // Tell the input-selector to change its active-pad
          Element selector = pipeline.getElementByName("selector");
          Pad new_pad = selector.getStaticPad("sink1");
          selector.set("active-pad", new_pad);
          
          // Do not propagate the EOS down the pipeline
          //return false;  
          return true; //TODO: this is insane
        }
        //return true;  
        return false; //TODO: this is insane
      }
    });
    
    Element queue = ElementFactory.make("queue", captureDevice.getFriendlyName());
    
    // Elements that enable VGA signal hotswapping
    Element videotestsrc = ElementFactory.make("videotestsrc", null);
    videotestsrc.set("pattern", 0);
    
    // Create a filter to ensure videotestsrc is the same dimensions as the Epiphan card
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
    Element static_identity = ElementFactory.make("identity", "static_identity");
    
    // The input-selector which allows us to choose which source we want to capture from
    Element selector = ElementFactory.make("input-selector", "selector");
    Element segment = ElementFactory.make("identity", "identity-segment");
    
    
    if (bufferCount != null) {
      logger.debug("{} bufferCount set to {}.", captureDevice.getName(), bufferCount);
      queue.set("max-size-buffers", bufferCount);
    }
    if (bufferBytes != null) {
      logger.debug("{} bufferBytes set to {}.", captureDevice.getName(), bufferBytes);
      queue.set("max-size-bytes", bufferBytes);
    }
    if (bufferTime != null) {
      logger.debug("{} bufferTime set to {}.", captureDevice.getName(), bufferTime);
      queue.set("max-size-time", bufferTime);
    }

    // Elements that allow for change in fps
    Element videorate = ElementFactory.make("videorate", null);
    Element fpsfilter = ElementFactory.make("capsfilter", null);
    Caps fpsCaps;
    if (framerate != null) {
      fpsCaps = new Caps("video/x-raw-yuv, framerate=" + framerate + "/1");
      logger.debug("{} fps: {}", captureDevice.getName(), framerate);
    }
    else
      fpsCaps = Caps.anyCaps();
    fpsfilter.setCaps(fpsCaps);

    Element ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
    if (codec != null) {
      logger.debug("{} encoder set to: {}", captureDevice.getName(), codec);
      enc = ElementFactory.make(codec, null);
    }
    else {
      enc = ElementFactory.make("ffenc_mpeg2video", null);
    }
    
    // Must set H.264 encoding to use constant quantizer or else it will not start
    // Pass 0 is CBR (default), Pass 4 is constant quantizer, Pass 5 is constant quality
    if (codec != null && codec.equalsIgnoreCase("x264enc")) {
      enc.set("pass", "4");
      if (captureDevice.properties.contains("quantizer"))
        enc.set("quantizer", captureDevice.properties.getProperty("quantizer"));
    }
    if (container != null) {
      logger.debug("{} muxing to: {}", captureDevice.getName(), container);
      muxer = ElementFactory.make(container, null);
    }
    else {
      muxer = ElementFactory.make("mpegtsmux", null);
    }
    
    Element filesink = ElementFactory.make("filesink", null);

    v4lsrc.set("device", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), bitrate);      
      enc.set("bitrate", bitrate);
    }
    else
      enc.set("bitrate", "2000000");

    pipeline.addMany(v4lsrc, v4l_identity, queue, videotestsrc, capsfilter, 
            static_identity, selector, segment, videorate, fpsfilter, 
            ffmpegcolorspace, enc, muxer, filesink);

    if (!v4lsrc.link(v4l_identity))
      error = formatPipelineError(captureDevice, v4lsrc, v4l_identity);
    else if (!v4l_identity.link(queue))
      error = formatPipelineError(captureDevice, v4l_identity, queue);
    else if (!queue.link(selector))
      error = formatPipelineError(captureDevice, queue, selector);
    else if (!selector.link(segment))
      error = formatPipelineError(captureDevice, selector, segment);
    else if (!segment.link(videorate))
      error = formatPipelineError(captureDevice, segment, videorate);
    else if (!videorate.link(fpsfilter))
      error = formatPipelineError(captureDevice, videorate, fpsfilter);
    if (confidence) {
      boolean trace = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_DEBUG));
      if (!VideoMonitoring.addVideoMonitor(pipeline, fpsfilter, ffmpegcolorspace, interval, imageloc, device, trace))
        error = formatPipelineError(captureDevice, fpsfilter, ffmpegcolorspace);
    } else {
      if (!fpsfilter.link(ffmpegcolorspace))
        error = formatPipelineError(captureDevice, fpsfilter, ffmpegcolorspace);
    }
    if (!ffmpegcolorspace.link(enc))
      error = formatPipelineError(captureDevice, ffmpegcolorspace, enc);
    else if (!enc.link(muxer))
      error = formatPipelineError(captureDevice, enc, muxer);
    else if (!muxer.link(filesink))
      error = formatPipelineError(captureDevice, muxer, filesink);
    
    // Make the test source live, or images just queue up and reconnecting won't work
    videotestsrc.set("is-live", true);
    // Tell identity elements to be false, this shouldn't be required as it is the default
    v4l_identity.set("sync", false);
    static_identity.set("sync", false);
    segment.set("sync", false);
    
    // Add backup video source in case we lose the VGA signal
    if (!videotestsrc.link(capsfilter))
      error = formatPipelineError(captureDevice, videotestsrc, capsfilter);
    else if (!capsfilter.link(static_identity))
      error = formatPipelineError(captureDevice, capsfilter, static_identity);
    else if (!static_identity.link(selector))
      error = formatPipelineError(captureDevice, static_identity, selector);

    if (error != null) {
      pipeline.removeMany(v4lsrc, v4l_identity, queue, videotestsrc, capsfilter, 
              static_identity, selector, segment, videorate, fpsfilter, 
              ffmpegcolorspace, enc, muxer, filesink);
      logger.error(error);
      return false;
    }

    // Check it see if there is a VGA signal on startup
    if (check_epiphan(captureDevice.getLocation())) {
      logger.debug("Have signal on startup");
      Pad new_pad = selector.getStaticPad("sink0");
      selector.set("active-pad", new_pad);
    } else {
      // No VGA signal on start up, remove Epiphan source
      broken = true;
      Element src = pipeline.getElementByName("v4lsrc_" + v4lsrc_index);
      pipeline.remove(src);
      Pad new_pad = selector.getStaticPad("sink1");
      selector.set("active-pad", new_pad);
    }

    Thread poll = new Thread(new PollEpiphan(pipeline, captureDevice.getLocation()));
    poll.start();

    if (logger.isTraceEnabled()) {
      BufferThread t = new BufferThread(queue);
      t.start();
    }

    return true;
  }
  // CHECKSTYLE:ON
  
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

  /**
   * Adds a pipeline specifically designed to captured from an ALSA source to the main pipeline
   * 
   * @param captureDevice
   *          The ALSA source {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  private static boolean getAlsasrcPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "5"));
    int monitoringLength = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_AUDIO_LENGTH, "60"));
    String error = null;
    String codec = captureDevice.properties.getProperty("codec");
    String container = captureDevice.properties.getProperty("container");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    String bufferCount = captureDevice.properties.getProperty("bufferCount");
    String bufferBytes = captureDevice.properties.getProperty("bufferBytes");
    String bufferTime = captureDevice.properties.getProperty("bufferTime");
    boolean confidence = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));

    Element enc, mux;

    Element alsasrc = ElementFactory.make("alsasrc", null);
    Element queue = ElementFactory.make("queue", captureDevice.getFriendlyName());
    Element audioconvert = ElementFactory.make("audioconvert", null);
    if (bufferCount != null) {
      logger.debug("{} bufferCount set to {}.", captureDevice.getName(), bufferCount);
      queue.set("max-size-buffers", bufferCount);
    }
    if (bufferBytes != null) {
      logger.debug("{} bufferBytes set to {}.", captureDevice.getName(), bufferBytes);
      queue.set("max-size-bytes", bufferBytes);
    }
    if (bufferTime != null) {
      logger.debug("{} bufferTime set to {}.", captureDevice.getName(), bufferTime);
      queue.set("max-size-time", bufferTime);
    }

    if (codec != null) {
      logger.debug("{} encoder set to: {}", captureDevice.getName(), codec);
      enc = ElementFactory.make(codec, null);
      if (codec.equalsIgnoreCase("faac"))
        mux = ElementFactory.make("mp4mux", null);
      else
        mux = ElementFactory.make("capsfilter", null);
    }
    else {
      enc = ElementFactory.make("twolame", null);
      mux = ElementFactory.make("capsfilter", null);
    }
    
    if (container != null) {
      logger.debug("{} muxing to: {}", captureDevice.getName(), container);
      mux = ElementFactory.make(container, null);
    }
    Element filesink = ElementFactory.make("filesink", null);


    alsasrc.set("device", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), bitrate);
      enc.set("bitrate", bitrate);
    }

    pipeline.addMany(alsasrc, queue, audioconvert, enc, mux, filesink);

    if (!alsasrc.link(queue))
      error = formatPipelineError(captureDevice, alsasrc, queue);
    else if (!queue.link(audioconvert))
      error = formatPipelineError(captureDevice, queue, audioconvert);
    if (confidence) {
      if (!AudioMonitoring.addAudioMonitor(pipeline, audioconvert, enc, interval, monitoringLength, captureDevice.getFriendlyName()))
        error = formatPipelineError(captureDevice, audioconvert, enc);
    } else {
      if (!audioconvert.link(enc))
        error = formatPipelineError(captureDevice, audioconvert, enc);
    }
    if (!enc.link(mux))
      error = formatPipelineError(captureDevice, enc, mux);
    else if (!mux.link(filesink))
      error = formatPipelineError(captureDevice, mux, filesink);

    if (error != null) {
      pipeline.removeMany(alsasrc, queue, enc, mux, filesink);
      logger.error(error);
      return false;
    }

    if (logger.isTraceEnabled()) {
      BufferThread t = new BufferThread(queue);
      t.start();
    }

    return true;
  }

  /**
   * Adds a pipeline specifically designed to captured from the Bluecherry Provideo cards to the main pipeline
   * 
   * @param captureDevice
   *          The Bluecherry {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  private static boolean getBluecherryPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    // confidence vars
    String imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    String device = new File(captureDevice.getOutputPath()).getName();
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "5"));

    String error = null;
    String codec = captureDevice.properties.getProperty("codec");
    String container = captureDevice.properties.getProperty("container");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    String framerate = captureDevice.properties.getProperty("framerate");
    String bufferCount = captureDevice.properties.getProperty("bufferCount");
    String bufferBytes = captureDevice.properties.getProperty("bufferBytes");
    String bufferTime = captureDevice.properties.getProperty("bufferTime");
    boolean confidence = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));


    Element enc, muxer;
    Element v4l2src = ElementFactory.make("v4l2src", null);
    Element queue = ElementFactory.make("queue", captureDevice.getFriendlyName());
    if (bufferCount != null) {
      logger.debug("{} bufferCount set to {}.", captureDevice.getName(), bufferCount);
      queue.set("max-size-buffers", bufferCount);
    }
    if (bufferBytes != null) {
      logger.debug("{} bufferBytes set to {}.", captureDevice.getName(), bufferBytes);
      queue.set("max-size-bytes", bufferBytes);
    }
    if (bufferTime != null) {
      logger.debug("{} bufferTime set to {}.", captureDevice.getName(), bufferTime);
      queue.set("max-size-time", bufferTime);
    }

    if (codec != null) {
      logger.debug("{} encoder set to: {}", captureDevice.getName(), codec);
      enc = ElementFactory.make(codec, null);
    }
    else
      enc = ElementFactory.make("ffenc_mpeg2video", null);
    
    if (container != null) {
      logger.debug("{} muxing to: {}", captureDevice.getName(), container);
      muxer = ElementFactory.make(container, null);
    }
    else {
      muxer = ElementFactory.make("mpegtsmux", null);
    }
    
    Element filesink = ElementFactory.make("filesink", null);

    v4l2src.set("device", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), bitrate);
      enc.set("bitrate", bitrate);
    }
    else
      enc.set("bitrate", "2000000");
    
    // Elements that allow for change in fps
    Element videorate = ElementFactory.make("videorate", null);
    Element fpsfilter = ElementFactory.make("capsfilter", null);
    Caps fpsCaps;
    if (framerate != null) {
      fpsCaps = Caps.fromString("video/x-raw-yuv, framerate=" + framerate + "/1");
      logger.debug("{} fps: {}", captureDevice.getName(), framerate);
    }
    else
      fpsCaps = Caps.anyCaps();
    fpsfilter.setCaps(fpsCaps);

    pipeline.addMany(v4l2src, queue, videorate, fpsfilter, enc, muxer, filesink);

    if (!v4l2src.link(queue))
      error = formatPipelineError(captureDevice, v4l2src, queue);
    else if (!queue.link(videorate))
      error = formatPipelineError(captureDevice, queue, videorate);
    else if (!videorate.link(fpsfilter))
      error = formatPipelineError(captureDevice, videorate, fpsfilter);
    if (confidence) {
      boolean trace = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_DEBUG));
      if (!VideoMonitoring.addVideoMonitor(pipeline, fpsfilter, enc, interval, imageloc, device, trace))
        error = formatPipelineError(captureDevice, fpsfilter, enc);
    } else {
      if (!fpsfilter.link(enc))
        error = formatPipelineError(captureDevice, fpsfilter, enc);
    }
    if (!enc.link(muxer))
      error = formatPipelineError(captureDevice, enc, muxer);
    else if (!muxer.link(filesink))
      error = formatPipelineError(captureDevice, muxer, filesink);

    if (error != null) {
      pipeline.removeMany(v4l2src, queue, videorate, fpsfilter, enc, muxer, filesink);
      logger.error(error);
      return false;
    }

    if (logger.isTraceEnabled()) {
      BufferThread t = new BufferThread(queue);
      t.start();
    }

    return true;
  }

  /**
   * Adds a pipeline for a media file that just copies it to a new location
   * 
   * @param captureDevice
   *          The {@code CaptureDevice} with source and output information
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  private static boolean getFilePipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    Element filesrc = ElementFactory.make("filesrc", null);
    Element filesink = ElementFactory.make("filesink", null);

    filesrc.set("location", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());

    pipeline.addMany(filesrc, filesink);

    if (!filesrc.link(filesink)) {
      pipeline.removeMany(filesrc, filesink);
      logger.error(formatPipelineError(captureDevice, filesrc, filesink));
      return false;
    }

    return true;
  }
  
  /**
   * Adds a pipeline specifically designed to captured from a DV Camera attached by firewire to the main pipeline
   * 
   * @deprecated  This function has not been maintained in a long time and has many problems.  If you need DV support let the list know.
   * @param captureDevice
   *          DV Camera attached to firewire {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  private static boolean getDvPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    String error = null;
    String codec =  captureDevice.properties.getProperty("codec");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    String bufferCount = captureDevice.properties.getProperty("bufferCount");
    String bufferBytes = captureDevice.properties.getProperty("bufferBytes");
    String bufferTime = captureDevice.properties.getProperty("bufferTime");

    Element queue = ElementFactory.make("queue", captureDevice.getFriendlyName());
    if (bufferCount != null)
      queue.set("max-size-buffers", bufferCount);
    if (bufferBytes != null)
      queue.set("max-size-bytes", bufferBytes);
    if (bufferTime != null)
      queue.set("max-size-time", bufferTime);

    Element src = ElementFactory.make("dv1394src", null);
    Element dec = ElementFactory.make("capsfilter", null);
    Element enc = ElementFactory.make("capsfilter", null);
    Element filesink = ElementFactory.make("filesink", null);
    
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), bitrate);
      enc.set("bitrate", bitrate);
    }
    
    pipeline.addMany(src, queue, dec, enc, filesink);
    
    if (!src.link(queue))
      error = formatPipelineError(captureDevice, src, queue);
    else if (!queue.link(dec))
      error = formatPipelineError(captureDevice, queue, dec);
    else if (!dec.link(enc))
      error = formatPipelineError(captureDevice, dec, enc);
    else if (!enc.link(filesink))
      error = formatPipelineError(captureDevice, enc, filesink);
    
    if (error != null) {
      pipeline.removeMany(src, queue, enc, dec, filesink);
      logger.error(error);
      return false;
    }

    if (logger.isTraceEnabled()) {
      BufferThread t = new BufferThread(queue);
      t.start();
    }
    
    return true;
  }
  
}

/**
 * A Quick and dirty logging class.  This will only be created when the logging level is set to TRACE.
 * It's sole purpose is to output the three limits on the buffer for each device
 */
class BufferThread extends Thread {

  private static final Logger log = LoggerFactory.getLogger(BufferThread.class);
 
  Element queue = null;
  boolean run = true;

  public BufferThread(Element e) {
    log.info("Buffer monitoring thread started for device " + e.getName());
    queue = e;
    
    queue.getBus().connect(new Bus.MESSAGE() {
      @Override
      public void busMessage(Bus arg0, Message arg1) {
        if (arg1.getType().equals(MessageType.EOS)) {
          log.info("Shutting down buffer monitor thread for {}.", queue.getName());
          shutdown();
        }
      }
    });

  }

  public void run() {
    while (run) {
      log.trace(queue.getName() + "," + queue.get("current-level-buffers") + "," + queue.get("current-level-bytes") + "," + queue.get("current-level-time"));
      try {
        Thread.sleep(60000);
      } catch (InterruptedException e) {
        log.trace(queue.getName() + "'s buffer monitor thread caught an InterruptedException but is continuing.");
      }
    }
    log.trace(queue.getName() + "'s buffer monitor thread hit the end of the run() function.");
  }

  public void shutdown() {
    run = false;
  }
}
