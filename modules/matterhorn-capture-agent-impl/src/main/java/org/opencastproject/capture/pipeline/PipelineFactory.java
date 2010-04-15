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

import org.opencastproject.capture.api.CaptureParameters;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Given a Properties object describing devices this class will create a suitable pipeline to capture from all those
 * devices simultaneously.
 */
public class PipelineFactory {

  private static final Logger logger = LoggerFactory.getLogger(PipelineFactory.class);

  private static Properties properties;

  /**
   * Create a bin that contains multiple pipelines using each source in the properties object as the gstreamer source
   * 
   * @param properties
   *          Properties object defining sources 
   * @return The bin to control the pipelines
   * @throws UnsupportedDeviceException
   */
  public static Pipeline create(Properties props) {
    properties = props;
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();
    

    // Setup pipeline for all the devices specified
    String deviceNames = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (deviceNames == null) {
      logger.error("No capture devices specified in {}.", CaptureParameters.CAPTURE_DEVICE_NAMES);
      return null;
    }

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
    String outputDirectory = properties.getProperty(CaptureParameters.RECORDING_ROOT_URL);

    for (String name : friendlyNames) {

      name = name.trim();
      DeviceName devName;

      // Get properties from 
      String srcProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_SOURCE;
      String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_DEST;
      if (outputDirectory == null) {
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
      try {
        if(!outputFile.createNewFile()){
          logger.error("Could not create ouput file for {}, file may already exist.", name);
          return null;
        }
      } catch (IOException e) {
        logger.error("An error occured while creating output file for {}. {}", name, e.getMessage());
        return null;
      }
      String outputLoc = outputFile.getAbsolutePath();

      if (new File(srcLoc).isFile()) {
        // Non-V4L file. If it exists, assume it is ingestable
        // TODO: Fix security risk. Any file on CaptureAgent filesytem could be ingested
        devName = DeviceName.FILE;
      } else {
        // Attempt to determine what the device is using the JV4LInfo library 
        try {
          // ALSA source
          if (srcLoc.contains("hw:"))
            devName = DeviceName.ALSASRC;
          // V4L devices
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
              logger.error("Do not recognized device: {}.", srcLoc);
              return null;
            }
          }
        } catch (JV4LInfoException e) {
          // The v4l device caused an exception
          logger.error("Unexpected jv4linfo exception: {}.", e.getMessage());
          return null;
        }
      }

      // devices will store the CaptureDevice list arbitrary order
      CaptureDevice capdev = new CaptureDevice(srcLoc, devName, name, outputLoc);
      String codecProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_CODEC;
      String bitrateProperty = codecProperty + CaptureParameters.CAPTURE_DEVICE_BITRATE;
      String codec = properties.getProperty(codecProperty);
      String bitrate = properties.getProperty(bitrateProperty);

      if (codec != null)
        capdev.properties.setProperty("codec", codec);
      if (bitrate != null)
        capdev.properties.setProperty("bitrate", bitrate);

      if (!devices.add(capdev))
        logger.error("Unable to add device: {}.", capdev);
    }

    logger.info("Successfully initialised {} devices.", devices.size());
    for (int i = 0; i < devices.size(); i++)
      logger.debug("Device #{}: {}.", i, devices.get(i));

    // setup gstreamer pipeline using capture devices 
    Gst.init(); // cannot using gst library without first initialising it

    Pipeline pipeline = new Pipeline();
    for (int i = 0; i < devices.size(); i++) {
      if (!addPipeline(devices.get(i), pipeline))
        logger.error("Failed to create pipeline for {}.", devices.get(i));
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
    // confidence monitoring vars
    String imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    String device = new File(captureDevice.getOutputPath()).getName();
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "30"));
    // pipeline vars
    String error = null;
    String codec =  captureDevice.properties.getProperty("codec");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    Element dec, enc;
    Element filesrc = ElementFactory.make("filesrc", null);
    Element queue = ElementFactory.make("queue", null);
    Element mpegpsdemux = ElementFactory.make("mpegpsdemux", null);
    final Element mpegvideoparse = ElementFactory.make("mpegvideoparse", null);
    if (codec != null) {
      dec = ElementFactory.make("mpeg2dec", null);
      enc = ElementFactory.make(codec, null);
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
    else if (!VideoMonitoring.addVideoMonitor(pipeline, dec, enc, interval, imageloc, device))
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
    // confidence vars
    String imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    String device = new File(captureDevice.getOutputPath()).getName();
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "30"));

    String error = null;
    String codec = captureDevice.properties.getProperty("codec");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    // Create elements, add them to pipeline, then link them 
    Element enc;
    Element v4lsrc = ElementFactory.make("v4lsrc", null);
    Element queue = ElementFactory.make("queue", null);
    Element videoscale = ElementFactory.make("videoscale", null);
    Element videorate = ElementFactory.make("videorate", null);
    Element filter = ElementFactory.make("capsfilter", null);
    Element ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
    if (codec != null)
      enc = ElementFactory.make(codec, null);
    else
      enc = ElementFactory.make("ffenc_mpeg2video", null);
    Element mpegtsmux = ElementFactory.make("mpegtsmux", null);
    Element filesink = ElementFactory.make("filesink", null);

    v4lsrc.set("device", captureDevice.getLocation());
    filter.setCaps(Caps.fromString("video/x-raw-yuv,width=1024,height=768,framerate=30/1"));
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null)
      enc.set("bitrate", bitrate);
    else
      enc.set("bitrate", "2000000");

    pipeline.addMany(v4lsrc, queue, videoscale, videorate, filter, ffmpegcolorspace, enc, mpegtsmux, filesink);

    if (!v4lsrc.link(queue))
      error = formatPipelineError(captureDevice, v4lsrc, queue);
    else if (!queue.link(videoscale))
      error = formatPipelineError(captureDevice, queue, videoscale);
    else if (!videoscale.link(videorate))
      error = formatPipelineError(captureDevice, videoscale, videorate);
    else if (!videorate.link(filter))
      error = formatPipelineError(captureDevice, videorate, filter);
    else if (!VideoMonitoring.addVideoMonitor(pipeline, filter, ffmpegcolorspace, interval, imageloc, device))
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
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "30"));
    String error = null;
    String codec = captureDevice.properties.getProperty("codec");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    Element enc, mux;

    Element alsasrc = ElementFactory.make("alsasrc", null);
    Element queue = ElementFactory.make("queue", null);
    if (codec != null) {
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
    Element filesink = ElementFactory.make("filesink", null);


    alsasrc.set("device", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null)
      enc.set("bitrate", bitrate);

    pipeline.addMany(alsasrc, queue, enc, mux, filesink);

    if (!alsasrc.link(queue))
      error = formatPipelineError(captureDevice, alsasrc, queue);
    else if (!AudioMonitoring.addAudioMonitor(pipeline, queue, enc, interval))
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
    // confidence vars
    String imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    String device = new File(captureDevice.getOutputPath()).getName();
    int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "30"));

    String error = null;
    String codec = captureDevice.properties.getProperty("codec");
    String bitrate = captureDevice.properties.getProperty("bitrate");
    Element enc;
    Element v4l2src = ElementFactory.make("v4l2src", null);
    Element queue = ElementFactory.make("queue", null);
    if (codec != null)
      enc = ElementFactory.make(codec, null);
    else
      enc = ElementFactory.make("ffenc_mpeg2video", null);
    Element mpegtsmux = ElementFactory.make("mpegtsmux", null);
    Element filesink = ElementFactory.make("filesink", null);

    v4l2src.set("device", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
    if (bitrate != null)
      enc.set("bitrate", bitrate);
    else
      enc.set("bitrate", "2000000");

    pipeline.addMany(v4l2src, queue, enc, mpegtsmux, filesink);

    if (!v4l2src.link(queue))
      error = formatPipelineError(captureDevice, v4l2src, queue);
    else if (!VideoMonitoring.addVideoMonitor(pipeline, queue, enc, interval, imageloc, device))
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

  /**
   * Adds a pipeline for a media file that just copies it to a new location
   * 
   * @param captureDevice
   *          capture device with source and output information
   * @param pipeline
   *          the Pipeline bin to add it to
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
}
