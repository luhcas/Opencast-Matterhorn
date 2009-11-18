/**
 *  Copyright 2009 The Regents of the University of California
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

import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.capture.impl.ConfigurationManager;

import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Given a Properties object describing devices this class will create a suitable pipeline to capture from all those
 * devices simultaneously.
 */
public class PipelineFactory {

  private static final Logger logger = LoggerFactory.getLogger(PipelineFactory.class);
  
  public enum Codec { MPEG2, H264, AAC, MP3 };

  /**
   * Create a bin that contains multiple pipelines using each source in the properties object as the gstreamer source
   * 
   * @param props
   *          Properties object defining sources 
   * @return The bin to control the pipelines
   * @throws UnsupportedDeviceException
   */
  public static Pipeline create(Properties props) {
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();
    
    ConfigurationManager config = ConfigurationManager.getInstance();
    
    /*
     * Identify which candidate video devices are described in the properties and create CaptureDevice objects out of
     * them.
     */
    String deviceNames = config.getItem(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (deviceNames == null) {
      logger.error("No capture devices specified in " + CaptureParameters.CAPTURE_DEVICE_NAMES);
      return null;
    }
    
    String[] friendlyNames = deviceNames.split(",");
    
    for (String name : friendlyNames) {
      name = name.trim();
      DeviceName devName;
     
      /* disregard the empty string */
      if (name == "")
        continue;
      
      String srcProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + "." + name + ".src";
      String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + "." + name + ".outputfile";
      String srcLoc = config.getItem(srcProperty);
      String outputLoc = config.getItem(outputProperty);
      
      /* Attempt to determine what the device is using the JV4LInfo library */
      try {
        if (srcLoc.contains("hw:"))
          devName = DeviceName.ALSASRC;
        else {
          V4LInfo v4linfo = JV4LInfo.getV4LInfo(srcLoc);
          String deviceString = v4linfo.toString();
          if (deviceString.contains("Epiphan VGA2USB"))
            devName = DeviceName.EPIPHAN_VGA2USB;
          else if (deviceString.contains("Hauppauge") || deviceString.contains("WinTV"))
            devName = DeviceName.HAUPPAUGE_WINTV;
          else if (deviceString.contains("BT878"))
            devName = DeviceName.BLUECHERRY_PROVIDEO;
          else {
            logger.error("Device not recognized: " + srcLoc + ", ignoring...");
            continue;
          }
        }
      } catch (JV4LInfoException e) {
        logger.error("Could not access device: " + srcLoc);
        continue;
      }
      
      /* devices will store the CaptureDevice list arbitrary order */
      CaptureDevice capdev = new CaptureDevice(srcLoc, devName, outputLoc);
      String pluginProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + "." + name + ".plugin";
      String bitrateProperty = pluginProperty + ".properties.bitrate";
      String plugin = config.getItem(pluginProperty);
      String bitrate = config.getItem(bitrateProperty);
      
      if (plugin != null)
        capdev.properties.setProperty("plugin", plugin);
      if (bitrate != null)
        capdev.properties.setProperty("bitrate", bitrate);
  
      if (!devices.add(capdev))
        logger.error("Unable to add device: " + capdev);
    }

    logger.info("Successfully initialised " + devices.size() + " devices.");
    for (int i = 0; i < devices.size(); i++)
      logger.info("Device #" + i + ": " + devices.get(i));

    /* setup gstreamer pipeline using capture devices */
    Gst.init(); /* cannot using gst library without first initialising it */

    Pipeline pipeline = new Pipeline();
    for (int i = 0; i < devices.size(); i++) {
      if (!addPipeline(devices.get(i), pipeline))
        logger.error("Failed to create pipeline for " + devices.get(i));
    }

    return pipeline;
  }

  /**
   * addPipeline will add a pipeline for the specified capture device to the bin.
   * 
   * @param captureDevice
   *          CaptureDevice to create pipeline around
   * @param pipeline
   *          The Pipeline bin to add it to
   * @return True, if successful
   */
  private static boolean addPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    if (captureDevice.getName() == DeviceName.EPIPHAN_VGA2USB)
      return getVGA2USBPipeline(captureDevice, pipeline);
    else if (captureDevice.getName() == DeviceName.HAUPPAUGE_WINTV)
      return getHauppaugePipeline(captureDevice, pipeline);
    else if(captureDevice.getName() == DeviceName.BLUECHERRY_PROVIDEO)
      return getBluecherryPipeline(captureDevice, pipeline);
    else if (captureDevice.getName() == DeviceName.ALSASRC)
      return getAlsasrcPipeline(captureDevice, pipeline);
    return false;
  }

  /**
   * String representation of linking errors that occur when creating pipeline
   * 
   * @param device
   *          The device the error occurred on
   * @param src
   *          The source element being linked
   * @param sink
   *          The sink element being linked
   * @return String representation of the error
   */
  private static String formatPipelineError(CaptureDevice device, Element src, Element sink) {

    return device.getLocation() + ": " + "(" + src.toString() + ", " + sink.toString() + ")";
  }
  
  /**
   * Adds a pipeline specifically designed to captured from the Hauppauge WinTv cards to the main pipeline
   * 
   * @param captureDevice
   *          The Hauppauge CaptureDevice to create pipeline around
   * @param pipeline
   *          The Pipeline bin to add it to
   * @return True, if successful
   */
  private static boolean getHauppaugePipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    String error = null;
    String plugin =  captureDevice.properties.getProperty("plugin");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    Element dec, enc;
    Element filesrc = ElementFactory.make("filesrc", null);
    Element queue = ElementFactory.make("queue", null);
    Element mpegpsdemux = ElementFactory.make("mpegpsdemux", null);
    final Element mpegvideoparse = ElementFactory.make("mpegvideoparse", null);
    if (plugin != null) {
      dec = ElementFactory.make("mpeg2dec", null);
      enc = ElementFactory.make(plugin, null);
    }
    else {
      dec = ElementFactory.make("capsfilter", null);
      enc = ElementFactory.make("capsfilter", null);
    }
    Element mpegtsmux = ElementFactory.make("mpegtsmux", null);
    Element filesink = ElementFactory.make("filesink", null);

    filesrc.set("location", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null)
      enc.set("bitrate", bitrate);

    pipeline.addMany(filesrc, queue, mpegpsdemux, mpegvideoparse, dec, enc, mpegtsmux, filesink);

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
    else if (!dec.link(enc))
      error = formatPipelineError(captureDevice, dec, enc);
    else if (!enc.link(mpegtsmux))
      error = formatPipelineError(captureDevice, enc, mpegtsmux);
    else if (!mpegtsmux.link(filesink))
      error = formatPipelineError(captureDevice, mpegtsmux, filesink);

    if (error != null) {
      pipeline.removeMany(filesrc, queue, mpegpsdemux, mpegvideoparse, mpegtsmux, filesink);
      logger.error(error);
      return false;
    }
    
    return true;
  }
  
  /**
   * Adds a pipeline specifically designed to captured from the Epiphan VGA2USB cards to the main pipeline
   * 
   * @param captureDevice
   *          The VGA2USB CaptureDevice to create pipeline around
   * @param pipeline
   *          The Pipeline bin to add it to
   * @return True, if successful
   */
  private static boolean getVGA2USBPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    String error = null;
    String plugin = captureDevice.properties.getProperty("plugin");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    /* Create elements, add them to pipeline, then link them */
    Element enc;
    Element v4lsrc = ElementFactory.make("v4lsrc", null);
    Element queue = ElementFactory.make("queue", null);
    Element videoscale = ElementFactory.make("videoscale", null);
    Element videorate = ElementFactory.make("videorate", null);
    Element filter = ElementFactory.make("capsfilter", null);
    Element ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
    if (plugin != null)
      enc = ElementFactory.make(plugin, null);
    else
      enc = ElementFactory.make("ffenc_mpeg2video", null);
    Element mpegtsmux = ElementFactory.make("mpegtsmux", null);
    Element filesink = ElementFactory.make("filesink", null);

    v4lsrc.set("device", captureDevice.getLocation());
    filter.setCaps(Caps.fromString("video/x-raw-yuv, width=1024," + "height=768,framerate=30/1"));
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null)
      enc.set("bitrate", bitrate);

    pipeline.addMany(v4lsrc, queue, videoscale, videorate, filter, ffmpegcolorspace, enc, mpegtsmux, filesink);

    if (!v4lsrc.link(queue))
      error = formatPipelineError(captureDevice, v4lsrc, queue);
    else if (!queue.link(videoscale))
      error = formatPipelineError(captureDevice, queue, videoscale);
    else if (!videoscale.link(videorate))
      error = formatPipelineError(captureDevice, videoscale, videorate);
    else if (!videorate.link(filter))
      error = formatPipelineError(captureDevice, videorate, filter);
    else if (!filter.link(ffmpegcolorspace))
      error = formatPipelineError(captureDevice, filter, ffmpegcolorspace);
    else if (!ffmpegcolorspace.link(enc))
      error = formatPipelineError(captureDevice, ffmpegcolorspace, enc);
    else if (!enc.link(mpegtsmux))
      error = formatPipelineError(captureDevice, enc, mpegtsmux);
    else if (!mpegtsmux.link(filesink))
      error = formatPipelineError(captureDevice, mpegtsmux, filesink);

    if (error != null) {
      pipeline.removeMany(v4lsrc, queue, videoscale, videorate, filter, ffmpegcolorspace, enc, mpegtsmux, filesink);
      logger.error(error);
      return false;
    }
    
    return true;
  }
  
  /**
   * Adds a pipeline specifically designed to captured from an ALSA source to the main pipeline
   * 
   * @param captureDevice
   *          The ALSA source CaptureDevice to create pipeline around
   * @param pipeline
   *          The Pipeline bin to add it to
   * @return True, if successful
   */
  private static boolean getAlsasrcPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    String error = null;
    String plugin = captureDevice.properties.getProperty("plugin");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    Element enc, mux;
    
    Element alsasrc = ElementFactory.make("alsasrc", null);
    Element queue = ElementFactory.make("queue", null);
    if (plugin != null) {
      enc = ElementFactory.make(plugin, null);
      if (plugin.equalsIgnoreCase("faac"))
        mux = ElementFactory.make("mp4mux", null);
      else
        mux = ElementFactory.make("capsfilter", null);
    }
    else {
      enc = ElementFactory.make("twolame", null);
      mux = ElementFactory.make("capsfilter", null);
    }
    Element filesink = ElementFactory.make("filesink", null);
    

    alsasrc.set("device", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null)
      enc.set("bitrate", bitrate);
    
    pipeline.addMany(alsasrc, queue, enc, mux, filesink);

    if (!alsasrc.link(queue))
      error = formatPipelineError(captureDevice, alsasrc, queue);
    else if (!queue.link(enc))
      error = formatPipelineError(captureDevice, queue, enc);
    else if (!enc.link(mux))
      error = formatPipelineError(captureDevice, enc, mux);
    else if (!mux.link(filesink))
      error = formatPipelineError(captureDevice, mux, filesink);

    if (error != null) {
      pipeline.removeMany(alsasrc, queue, enc, mux, filesink);
      logger.error(error);
      return false;
    }
    
    return true;
  }
  
  /**
   * Adds a pipeline specifically designed to captured from the Bluecherry Provideo cards to the main pipeline
   * 
   * @param captureDevice
   *          The Bluecherry CaptureDevice to create pipeline around
   * @param pipeline
   *          The Pipeline bin to add it to
   * @return True, if successful
   */
  private static boolean getBluecherryPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    String error = null;
    String plugin = captureDevice.properties.getProperty("plugin");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    Element enc;
    Element v4l2src = ElementFactory.make("v4l2src", null);
    Element queue = ElementFactory.make("queue", null);
    if (plugin != null)
      enc = ElementFactory.make(plugin, null);
    else
      enc = ElementFactory.make("ffenc_mpeg2video", null);
    Element mpegtsmux = ElementFactory.make("mpegtsmux", null);
    Element filesink = ElementFactory.make("filesink", null);
    
    v4l2src.set("device", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null)
      enc.set("bitrate", bitrate);
    
    pipeline.addMany(v4l2src, queue, enc, mpegtsmux, filesink);
    
    if (!v4l2src.link(queue))
      error = formatPipelineError(captureDevice, v4l2src, queue);
    else if (!queue.link(enc))
      error = formatPipelineError(captureDevice, queue, enc);
    else if (!enc.link(mpegtsmux))
      error = formatPipelineError(captureDevice, enc, mpegtsmux);
    else if (!mpegtsmux.link(filesink))
      error = formatPipelineError(captureDevice, mpegtsmux, filesink);
    
    if (error != null) {
      pipeline.removeMany(v4l2src, queue, enc, mpegtsmux, filesink);
      logger.error(error);
      return false;
    }
    return true;
  }
}
