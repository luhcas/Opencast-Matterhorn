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

import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Message;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Class containing audio monitoring services that can be incorporated into
 * GStreamer pipelines and used for confidence monitoring.
 */
public class AudioMonitoring {
  
  private static final Logger logger = LoggerFactory.getLogger(VideoMonitoring.class);
  
  private static List<Double> rmsValues;
  
  /**
   * Add a method for confidence monitoring to a pipeline capturing audio by
   * teeing the raw data a pulling it to the appsink element.
   * 
   * @param p pipeline to add audio monitoring to
   * @param src the source element which will be tee'd
   * @param sink the sink element which the src originally sent data to
   * @param interval how often to grab data from the pipeline
   * @return the pipeline with the audio monitoring added, or null on failure
   */
  public static boolean addAudioMonitor(Pipeline pipeline, Element src, Element sink, final long interval) {
          
      Element tee, queue0, queue1, decodebin, fakesink;
      final Element level;
      
      rmsValues = new LinkedList<Double>();
      
      // the items to be tee'd and added to the pipeline
      tee = ElementFactory.make("tee", null);
      queue0 = ElementFactory.make("queue", null);
      queue1 = ElementFactory.make("queue", null);
      decodebin = ElementFactory.make("decodebin", null);
      level = ElementFactory.make("level", null);
      fakesink = ElementFactory.make("fakesink", null);
      
      tee.set("silent", "false");
      level.set("message", "true");
      
      pipeline.addMany(tee, queue0, queue1, decodebin, level, fakesink);
      src.unlink(sink);
      
      decodebin.connect(new Element.PAD_ADDED() {
        public void padAdded(Element element, Pad pad) {
          pad.link(level.getStaticPad("sink"));
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
        logger.error("Could not link {} with {}", queue1.toString(), decodebin.toString());
        return false;
      }
      
      Pad p = new Pad(null, PadDirection.SRC);
      decodebin.addPad(p);
      
      if (!level.link(fakesink)) {
        logger.error("Could not link {} with {}", level.toString(), fakesink.toString());
        return false;
      }
      
      // callback to listen for messages from the level element, giving us
      // information about the audio being recorded
      Bus bus = pipeline.getBus();
      bus.connect(new Bus.MESSAGE() {
        long previous = -1;
        public void busMessage(Bus bus, Message msg) {
          if (msg.getSource().equals(level)) {
            Element level = (Element) msg.getSource();
            long seconds = level.getClock().getTime().getSeconds();
            if (seconds % interval == 0 && seconds != previous) {
              previous = seconds;
              String data = msg.getStructure().toString();
              int start = data.indexOf("rms");
              int end = data.indexOf("}", start);
              String rms = data.substring(start, end+1);
              start = rms.indexOf("{");
              end = rms.indexOf("}");
              double value = Double.parseDouble(rms.substring(start+1, end).split(",")[0]);
              rmsValues.add(value);
            }
          }
        }
      });
      
      return true;
  }
  
  public static List<Double> getRMSValues() {
    return rmsValues;
  }

}
