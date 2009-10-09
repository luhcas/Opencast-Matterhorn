package org.opencastproject.capture.pipeline;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import org.gstreamer.*;
import net.luniks.linux.jv4linfo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a Properties object describing devices this class will create a 
 * suitable pipeline to capture from all those devices simultaneously.
 */
public class PipelineFactory {

  private static final Logger logger = 
    LoggerFactory.getLogger(PipelineFactory.class);


  /**
   * Create a bin that contains multiple pipelines using each source in 
   * the properties object as the gstreamer source
   * @param props		Properties object defining sources 
   * e.g., {"hw:0"=MICROPHONE, "/dev/video1"=SLIDES, "/dev/video0"=PRESENTER}
   * @return			The bin to control the pipelines
   * @throws UnsupportedDeviceException
   */
  public static Pipeline create(Properties props) {
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();

    /* Identify which candidate video devices are described in the 
     * properties and create CaptureDevice objects out of them. */
    Enumeration<Object> keys = props.keys();
    while (keys.hasMoreElements()) {
      DeviceName devName;
      String devicePath = (String) keys.nextElement();

      try {
        if (devicePath.contains("hw:"))
          devName = DeviceName.ALSASRC;
        else {

          /* uses jv4linfo to determine video sources */
          V4LInfo v4linfo = JV4LInfo.getV4LInfo(devicePath);
          String deviceString = v4linfo.toString();
          if (deviceString.contains("Epiphan VGA2USB"))
            devName = DeviceName.VGA2USB;
          else if (deviceString.contains("Hauppauge WinTV PVR-350"))
            devName = DeviceName.PVR350;
          else {
            logger.error("Device not recognized: " + devicePath);
            continue;
          }
        }	
      } catch (JV4LInfoException e) {
        logger.error("Could not access device: " + devicePath);
        continue;
      }

      /* devices will store the CaptureDevice list arbitrary order */
      CaptureDevice capdev = new CaptureDevice(devicePath, devName, 
              (String) props.getProperty(devicePath));
      if (!devices.add(capdev))
        logger.error("Unable to add device.");

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
   * addPipeline will add a pipeline for the specified capture device to the
   * bin.
   * @param captureDevice		CaptureDevice to create pipeline around
   * @param pipeline				The Pipeline bin to add it to
   * @return							  True, if successful
   */
  private static boolean addPipeline(CaptureDevice captureDevice, 
          Pipeline pipeline) {

    String error = null;

    /* Setup pipeline to capture data from VGA2USB device */
    if (captureDevice.getName() == DeviceName.VGA2USB) {

      /* Create elements, add them to pipeline, then link them */
      Element v4lsrc = ElementFactory.make("v4lsrc", null);
      Element queue = ElementFactory.make("queue", null);
      Element videoscale = ElementFactory.make("videoscale", null);
      Element videorate = ElementFactory.make("videorate", null);
      Element filter = ElementFactory.make("capsfilter", null);
      Element mpeg2enc  = ElementFactory.make("ffenc_mpeg2video", null);
      Element mpegtsmux = ElementFactory.make("mpegtsmux", null);
      Element filesink = ElementFactory.make("filesink", null);

      v4lsrc.set("device", captureDevice.getLocation());
      filter.setCaps(Caps.fromString("video/x-raw-yuv, width=1024," +
      "height=768,framerate=30/1"));
      filesink.set("location", captureDevice.getOutputPath());

      pipeline.addMany(v4lsrc, queue, videoscale, videorate, filter, 
              mpeg2enc, mpegtsmux, filesink);

      if (!v4lsrc.link(queue))
        error = formatPipelineError(captureDevice, v4lsrc, queue);
      else if (!queue.link(videoscale))
        error = formatPipelineError(captureDevice, queue, videoscale);
      else if (!videoscale.link(videorate))
        error = formatPipelineError(captureDevice, videoscale, videorate);
      else if (!videorate.link(filter))
        error = formatPipelineError(captureDevice, videorate, filter);
      else if (!filter.link(mpeg2enc))
        error = formatPipelineError(captureDevice, filter, mpeg2enc);
      else if (!mpeg2enc.link(mpegtsmux))
        error = formatPipelineError(captureDevice, mpeg2enc, mpegtsmux);
      else if (!mpegtsmux.link(filesink))
        error = formatPipelineError(captureDevice, mpegtsmux, filesink);

      if (error != null) {
        pipeline.removeMany(v4lsrc, queue, videoscale, videorate, 
                filter, mpeg2enc, mpegtsmux, filesink);
        logger.error(error);
        return false;
      }
    }

    /* Setup pipeline to capture from Hauppauge WinTV PVR-350 device */
    else if (captureDevice.getName() == DeviceName.PVR350) {

      Element filesrc = ElementFactory.make("filesrc", null);
      Element queue = ElementFactory.make("queue", null);
      Element mpegpsdemux = ElementFactory.make("mpegpsdemux", null);
      final Element mpegvideoparse = ElementFactory.make("mpegvideoparse", null);
      Element mpegtsmux = ElementFactory.make("mpegtsmux", null);
      Element filesink = ElementFactory.make("filesink", null);

      filesrc.set("location", captureDevice.getLocation());
      filesink.set("location", captureDevice.getOutputPath());

      pipeline.addMany(filesrc, queue, mpegpsdemux, mpegvideoparse, 
              mpegtsmux, filesink);

      /* mpegpsdemux source pad is only available sometimes, therefore we
       * need to add a listener to accept dynamic pads */
      mpegpsdemux.connect(new Element.PAD_ADDED() {
        @Override
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
      else if (!mpegvideoparse.link(mpegtsmux))
        error = formatPipelineError(captureDevice, mpegvideoparse, mpegtsmux);
      else if (!mpegtsmux.link(filesink))
        error = formatPipelineError(captureDevice, mpegtsmux, filesink);

      if (error != null) {
        pipeline.removeMany(filesrc, queue, mpegpsdemux, mpegvideoparse,
                mpegtsmux, filesink);
        logger.error(error);
        return false;
      }
    }

    /* Setup pipeline to capture from an alsasrc */
    else if (captureDevice.getName() == DeviceName.ALSASRC) {

      Element alsasrc = ElementFactory.make("alsasrc", null);
      Element queue = ElementFactory.make("queue", null);
      Element twolame = ElementFactory.make("twolame", null);
      Element filesink = ElementFactory.make("filesink", null);

      alsasrc.set("device", captureDevice.getLocation());
      filesink.set("location", captureDevice.getOutputPath());
      pipeline.addMany(alsasrc, queue, twolame, filesink);

      if (!alsasrc.link(queue))
        error = formatPipelineError(captureDevice, alsasrc, queue);
      else if (!queue.link(twolame))
        error = formatPipelineError(captureDevice, queue, twolame);
      else if (!twolame.link(filesink))
        error = formatPipelineError(captureDevice, twolame, filesink);

      if (error != null) {
        pipeline.removeMany(alsasrc, queue, twolame, filesink);
        logger.error(error);
        return false;
      }
    }
    return true;
  }

  /**
   * String representation of linking errors that occur when creating pipeline
   * @param device   The device the error occurred on
   * @param src      The source element being linked
   * @param sink     The sink element being linked
   * @return         String representation of the error
   */
  private static String formatPipelineError(CaptureDevice device, Element src,
          Element sink) {

    return device.getLocation() + ": " + "(" + src.toString() + ", " + 
    sink.toString() + ")";
  }
}

