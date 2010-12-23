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
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

import com.sun.jna.Pointer;

import org.gstreamer.Bin;
import org.gstreamer.Buffer;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Properties;

import javax.imageio.ImageIO;

/**
 * Class containing video monitoring services that can be incorporated into GStreamer pipelines and used for confidence
 * monitoring.
 */
public final class VideoMonitoring {

  private static final Logger logger = LoggerFactory.getLogger(VideoMonitoring.class);

  /**
   * Prevents the instantiation of this static utility class.
   */
  private VideoMonitoring() {
    // Nothing to do here
  }

  /**
   * Add a method for confidence monitoring to a {@code Pipeline} capturing video by teeing the raw data a pulling it to
   * the appsink element.
   * 
   * @param bin
   *          {@code Pipeline} to add video monitoring to
   * @param src
   *          the source {@code Element} which will be tee'd
   * @param sink
   *          the sink {@code Element} which the src originally sent data to
   * @param interval
   *          how often to grab data from the pipeline
   * @param location
   *          the directory to save the image to
   * @param device
   *          name of device; used to name the jpeg file
   * @return True if the pipeline worked, or null on failure
   */
  public static boolean addVideoMonitor(Bin bin, Element src, Element sink, final long interval, final String location,
          final String device, final boolean trace) {

    Element tee;
    Element queue0;
    Element queue1;
    Element decodebin;
    Element capsfilter;
    Element jpegenc;
    Element queue2;
    final Element ffmpegcolorspace;
    AppSink appsink;

    // the items to be tee'd and added to the pipeline
    tee = ElementFactory.make("tee", null);
    queue0 = ElementFactory.make("queue", null);
    queue1 = ElementFactory.make("queue", null);
    decodebin = ElementFactory.make("decodebin", null);
    ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
    capsfilter = ElementFactory.make("capsfilter", null);
    jpegenc = ElementFactory.make("jpegenc", null);
    queue2 = ElementFactory.make("queue", null);
    appsink = (AppSink) ElementFactory.make("appsink", null);

    tee.set("silent", "false");
    appsink.set("emit-signals", "true");

    bin.addMany(tee, queue0, queue1, decodebin, ffmpegcolorspace, capsfilter, jpegenc, queue2, appsink);
    src.unlink(sink);
    appsink.set("drop", "true");

    decodebin.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        pad.link(ffmpegcolorspace.getStaticPad("sink"));
      }
    });

    if (!src.link(tee)) {
      logger.error("Could not link {} with {}", src.toString(), tee.toString());
      return false;
    }
    if (!tee.link(queue0)) {
      logger.error("Could not link {} with {}", tee.toString(), queue0.toString());
      return false;
    }
    if (!queue0.link(sink)) {
      logger.error("Could not link {} with {}", queue0.toString(), sink.toString());
      return false;
    }
    if (!tee.link(queue1)) {
      logger.error("Could not link {} with {}", tee.toString(), queue1.toString());
      return false;
    }
    if (!queue1.link(decodebin)) {
      logger.error("Could not link {} with {}", queue1.toString(), ffmpegcolorspace.toString());
      return false;
    }

    Pad p = new Pad(null, PadDirection.SRC);
    decodebin.addPad(p);

    if (!ffmpegcolorspace.link(capsfilter)) {
      logger.error("Could not link {} with {}", ffmpegcolorspace.toString(), capsfilter.toString());
      return false;
    }
    if (!capsfilter.link(jpegenc)) {
      logger.error("Could not link {} with {}", capsfilter.toString(), jpegenc.toString());
      return false;
    }
    if (!jpegenc.link(queue2)) {
      logger.error("Could not link {} with {}", jpegenc.toString(), queue2.toString());
      return false;
    }
    if (!queue2.link(appsink)) {
      logger.error("Could not link {} with {}", queue2.toString(), appsink.toString());
      return false;
    }

    // Callback that will be executed every time a new buffer is received
    // from the pipeline capturing the video. For confidence monitoring
    // it is not necessary to use every buffer
    appsink.connect(new AppSink.NEW_BUFFER() {
      private long previous = -1;

      public void newBuffer(Element elem, Pointer userData) {
        AppSink appsink = (AppSink) elem;
        long seconds = appsink.getClock().getTime().getSeconds();
        Buffer buffer = appsink.pullBuffer();
        // If we're on the division between intervals and we're not in the same second as the last entry
        if (seconds % interval == 0 && seconds != previous) {
          previous = seconds;
          /* saving the frame to disk */
          Timestamp timestamp = new Timestamp(System.currentTimeMillis());
          String text = timestamp.toString();
          if (text.split("\\.").length > 1)
            text = text.split("\\.")[0];
          try {
            byte[] bytes = new byte[buffer.getSize()];
            ByteBuffer byteBuffer = buffer.getByteBuffer();
            byteBuffer.get(bytes, 0, buffer.getSize());
            BufferedImage br = ImageIO.read(new ByteArrayInputStream(bytes));
            if (trace) {
              Graphics2D graphic = br.createGraphics();
              graphic.setFont(new Font("Helvetica", Font.BOLD, 16));
              graphic.drawString(text, 0, 20);
              graphic.finalize();
              graphic.dispose();

            }
            if (!ImageIO.write(br, "jpeg", new File(location, device + ".jpg")))
              logger.error("Unable to save confidence image for device: {}", device);
          } catch (Exception e) {
            logger.error(e.getMessage());
          }
        }
        buffer.dispose();
        buffer = null;
      }
    });

    return true;
  }

  /**
   * Return a {@code Pipeline} that doesn't capture, but only does confidence monitoring
   * 
   * @param pipeline
   *          The {@code Pipeline} to add the new confidence monitoring pipeline to
   * @param capdev
   *          The {@code CaptureDevice} the confidence monitoring is for
   * @param properties
   *          The {@code Properties} that define the interval and location for the confidence monitoring
   */
  public static void getConfidencePipeline(Pipeline pipeline, CaptureDevice capdev, Properties properties) {
    Element src;
    Element queue;
    Element decodebin;
    Element jpegenc;
    Element queue2;
    final Element ffmpegcolorspace;
    AppSink appsink;
    boolean success = true;
    final int interval = Integer.parseInt(properties.getProperty(
            CaptureParameters.CAPTURE_DEVICE_PREFIX + capdev.getFriendlyName()
                    + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "30"));
    final String device = new File(capdev.getOutputPath()).getName();
    final String location = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    final boolean trace = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_DEBUG));

    switch (capdev.getName()) {
    case EPIPHAN_VGA2USB:
      src = ElementFactory.make("v4lsrc", null);
      src.set("device", capdev.getLocation());
      break;
    case HAUPPAUGE_WINTV:
      src = ElementFactory.make("filesrc", null);
      src.set("location", capdev.getLocation());
      break;
    case BLUECHERRY_PROVIDEO:
      src = ElementFactory.make("v4l2src", null);
      src.set("device", capdev.getLocation());
      break;
    case DV_1394:
      src = ElementFactory.make("dv1394src", null);
      break;
    default:
      return;
    }

    queue = ElementFactory.make("queue", null);
    decodebin = ElementFactory.make("decodebin", null);
    ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
    jpegenc = ElementFactory.make("jpegenc", null);
    queue2 = ElementFactory.make("queue", null);
    appsink = (AppSink) ElementFactory.make("appsink", null);

    appsink.set("emit-signals", "true");

    pipeline.addMany(src, queue, decodebin, ffmpegcolorspace, jpegenc, queue2, appsink);

    decodebin.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        pad.link(ffmpegcolorspace.getStaticPad("sink"));
      }
    });

    if (!src.link(queue)) {
      logger.error("Could not link {} with {}", src.toString(), queue.toString());
      success = false;
    }
    if (!queue.link(decodebin)) {
      logger.error("Could not link {} with {}", queue.toString(), decodebin.toString());
      success = false;
    }

    Pad p = new Pad(null, PadDirection.SRC);
    decodebin.addPad(p);

    if (!ffmpegcolorspace.link(jpegenc)) {
      logger.error("Could not link {} with {}", ffmpegcolorspace.toString(), jpegenc.toString());
      success = false;
    }
    if (!jpegenc.link(queue2)) {
      logger.error("Could not link {} with {}", jpegenc.toString(), queue2.toString());
      success = false;
    }
    if (!queue2.link(appsink)) {
      logger.error("Could not link {} with {}", queue2.toString(), appsink.toString());
      success = false;
    }

    if (!success) {
      pipeline.removeMany(src, queue, decodebin, ffmpegcolorspace, jpegenc, queue2, appsink);
      return;
    }

    // FIXME: This looks like duplicated code. Can we move it out into a private function or is there a reason for the
    // duplication?
    // Callback that will be executed every time a new buffer is received
    // from the pipeline capturing the video. For confidence monitoring
    // it is not necessary to use every buffer
    appsink.connect(new AppSink.NEW_BUFFER() {
      private long previous = -1;

      public void newBuffer(Element elem, Pointer userData) {
        AppSink appsink = (AppSink) elem;
        long seconds = appsink.getClock().getTime().getSeconds();
        Buffer buffer = appsink.pullBuffer();
        if (seconds % interval == 0 && seconds != previous) {
          previous = seconds;
          /* saving the frame to disk */
          Timestamp timestamp = new Timestamp(System.currentTimeMillis());
          String text = timestamp.toString();
          if (text.split("\\.").length > 1)
            text = text.split("\\.")[0];
          try {
            byte[] bytes = new byte[buffer.getSize()];
            ByteBuffer byteBuffer = buffer.getByteBuffer();
            byteBuffer.get(bytes, 0, buffer.getSize());
            BufferedImage br = ImageIO.read(new ByteArrayInputStream(bytes));
            if (trace) {
              Graphics2D graphic = br.createGraphics();
              graphic.setFont(new Font("Helvetica", Font.BOLD, 16));
              graphic.drawString(text, 0, 20);
              graphic.finalize();
            }
            if (!ImageIO.write(br, "jpeg", new File(location, device + ".jpg")))
              logger.error("Unable to save confidence image for device: {}", device);
          } catch (Exception e) {
            logger.error(e.getMessage());
          }
        }
        buffer = null;
      }
    });
  }
}
