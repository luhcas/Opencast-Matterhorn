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

import com.sun.jna.Pointer;

import org.gstreamer.Buffer;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pipeline;
import org.gstreamer.Structure;
import org.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   * @param interval how often to grab data from the pipeline
   * @return the pipeline with the video monitoring added, or null on failure
   */
  public static Pipeline addVideoMonitor(Pipeline p, final long interval) {
    
    Element tee, queue, jpegenc; 
    AppSink appsink;
    
    // the items to be tee'd and added to the pipeline
    tee = ElementFactory.make("tee", null);
    queue = ElementFactory.make("queue", null);
    jpegenc = ElementFactory.make("jpegenc", null);
    appsink = (AppSink) ElementFactory.make("appsink", null);
    
    p.addMany(tee, queue, jpegenc, appsink);
    
    if (!queue.link(jpegenc) || !jpegenc.link(appsink)) {
      return null;
    }
    
    // using appsink, we can grab each new buffer from the pipeline and gather
    // information from it, or even write data to disc
    appsink.connect(new AppSink.NEW_BUFFER() {
      long previous = -1;
      
      public void newBuffer(Element elem, Pointer data) {
        AppSink appsink = (AppSink) elem;
        long seconds = appsink.getClock().getTime().getSeconds();
        Buffer buffer = appsink.pullBuffer();
        if (seconds % interval == 0 && seconds != previous) {
          previous = seconds;
          Caps caps = buffer.getCaps();
          Structure s = caps.getStructure(0);
          int width = s.getInteger("width");
          int height = s.getInteger("height");
          logger.debug("Grabbed frame of size: {}, {}", width, height);
        }
        
      }
      
    });
    
    return null;
    
  }

}
