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

import com.sun.jna.Pointer;

import org.gstreamer.Buffer;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.Pipeline;
import org.gstreamer.Structure;
import org.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * Class containing video monitoring services that can be incorporated into
 * GStreamer pipelines and used for confidence monitoring.
 */
public class VideoMonitoring {
  
  private static final Logger logger = LoggerFactory.getLogger(VideoMonitoring.class);
  
  /**
   * Add a method for confidence monitoring to a pipeline capturing video by
   * teeing the raw data a pulling it to the appsink element.
   * 
   * @param p pipeline to add video monitoring to
   * @param src the source element which will be tee'd
   * @param sink the sink element which the src originally sent data to
   * @param interval how often to grab data from the pipeline
   * @param location the directory to save the image to
   * @param device name of device; used to name the jpeg file
   * @return the pipeline with the video monitoring added, or null on failure
   */
  public static boolean addVideoMonitor(Pipeline pipeline, Element src, Element sink, final long interval, final String location,
          final String device) {
          
      Element tee, queue0, queue1, decodebin, jpegenc;
      final Element ffmpegcolorspace;
      AppSink appsink;
      
      // the items to be tee'd and added to the pipeline
      tee = ElementFactory.make("tee", null);
      queue0 = ElementFactory.make("queue", null);
      queue1 = ElementFactory.make("queue", null);
      decodebin = ElementFactory.make("decodebin", null);
      ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
      jpegenc = ElementFactory.make("jpegenc", null);
      appsink = (AppSink) ElementFactory.make("appsink", null);
      
      tee.set("silent", "false");
      appsink.set("emit-signals", "true");
      
      pipeline.addMany(tee, queue0, queue1, decodebin, ffmpegcolorspace, jpegenc, appsink);
      src.unlink(sink);
      
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
      
      if (!ffmpegcolorspace.link(jpegenc)) {
        logger.error("Could not link {} with {}", ffmpegcolorspace.toString(), jpegenc.toString());
        return false;
      }
      if (!jpegenc.link(appsink)) {
        logger.error("Could not link {} with {}", jpegenc.toString(), appsink.toString());
        return false;
      }

      // Callback that will be executed every time a new buffer is received
      // from the pipeline capturing the video. For confidence monitoring
      // it is not necessary to use every buffer
      appsink.connect(new AppSink.NEW_BUFFER() {
        long previous = -1;
        public void newBuffer(Element elem, Pointer userData) {
          AppSink appsink = (AppSink) elem;
          long seconds = appsink.getClock().getTime().getSeconds();
          Buffer buffer = appsink.pullBuffer();
          if (seconds % interval == 0 && seconds != previous) {
            previous = seconds;
            Caps caps = buffer.getCaps();
            Structure s = caps.getStructure(0);
            int width = s.getInteger("width");
            int height = s.getInteger("height");
            logger.debug("Grabbed frame: ({}, {})", width, height);
            
            /* saving the frame to disk */
            byte bytes[] = new byte[buffer.getSize()];
            ByteBuffer byteBuffer = buffer.getByteBuffer();
            byteBuffer.get(bytes, 0, buffer.getSize());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
              baos.write(bytes);
              OutputStream fos = new FileOutputStream(new File(location, device + ".jpg"));
              baos.writeTo(fos);
              fos.close();
              baos.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
            
          }
          buffer = null;
        }
      });
      
      return true;
  }
  
  /**
   * Return a pipeline that doesn't capture, but only does confidence monitoring
   * 
   * @param pipeline The pipeline to add the new confidence monitoring pipeline to
   * @param capdev The CaptureDevice the confidence monitoring is for
   * @param properties Properties that define the interval and location for the confidence monitoring
   */
  public static void getConfidencePipeline(Pipeline pipeline, CaptureDevice capdev, Properties properties) {
      Element src, queue, decodebin, jpegenc;
      final Element ffmpegcolorspace;
      AppSink appsink;
      boolean success = true;
      final int interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + capdev.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "30"));
      final String device = new File(capdev.getOutputPath()).getName();
      final String location = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);

      
      switch (capdev.getName()) {
        case EPIPHAN_VGA2USB: src = ElementFactory.make("v4lsrc", null); src.set("device", capdev.getLocation()); break;
        case HAUPPAUGE_WINTV: src = ElementFactory.make("filesrc", null); src.set("location", capdev.getLocation()); break;
        case BLUECHERRY_PROVIDEO: src = ElementFactory.make("v4l2src", null); src.set("device", capdev.getLocation()); break;
        default: return;
      }
      
      queue = ElementFactory.make("queue", null);
      decodebin = ElementFactory.make("decodebin", null);
      ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
      jpegenc = ElementFactory.make("jpegenc", null);
      appsink = (AppSink) ElementFactory.make("appsink", null);
      
      appsink.set("emit-signals", "true");
      
      pipeline.addMany(src, queue, decodebin, ffmpegcolorspace, jpegenc, appsink);
      
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
      if (!jpegenc.link(appsink)) {
        logger.error("Could not link {} with {}", jpegenc.toString(), appsink.toString());
        success = false;
      }
      
      if (!success) {
        pipeline.removeMany(src, queue, decodebin, ffmpegcolorspace, jpegenc, appsink);
        return;
      }
      
      
      // Callback that will be executed every time a new buffer is received
      // from the pipeline capturing the video. For confidence monitoring
      // it is not necessary to use every buffer
      appsink.connect(new AppSink.NEW_BUFFER() {
        long previous = -1;
        public void newBuffer(Element elem, Pointer userData) {
          AppSink appsink = (AppSink) elem;
          long seconds = appsink.getClock().getTime().getSeconds();
          Buffer buffer = appsink.pullBuffer();
          if (seconds % interval == 0 && seconds != previous) {
            previous = seconds;
            Caps caps = buffer.getCaps();
            Structure s = caps.getStructure(0);
            int width = s.getInteger("width");
            int height = s.getInteger("height");
            logger.debug("Grabbed frame: ({}, {})", width, height);
            
            /* saving the frame to disk */
            byte bytes[] = new byte[buffer.getSize()];
            ByteBuffer byteBuffer = buffer.getByteBuffer();
            byteBuffer.get(bytes, 0, buffer.getSize());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
              baos.write(bytes);
              OutputStream fos = new FileOutputStream(new File(location, device + ".jpg"));
              baos.writeTo(fos);
              fos.close();
              baos.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
            
          }
          buffer = null;
        }
      });
  }
}
