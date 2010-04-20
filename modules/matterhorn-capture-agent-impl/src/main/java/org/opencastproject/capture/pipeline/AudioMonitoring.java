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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class containing audio monitoring services that can be incorporated into
 * GStreamer pipelines and used for confidence monitoring.
 */
public class AudioMonitoring {
  
  private static final Logger logger = LoggerFactory.getLogger(VideoMonitoring.class);
  
  private static HashMap<String, SortedSet<Pair>> deviceRMSValues;
  
  /**
   * Simple inner class to pair RMS values with their timestamps and sort them
   * by timestamp
   */
  private static class Pair implements Comparable<Pair> {
    
    private double timestamp;
    
    private double rms;
    
    public Pair(double timestamp, double rms) {
      this.timestamp = timestamp;
      this.rms = rms;
    }
    
    public double getTimestamp() {
      return timestamp;
    }
    
    public double getRMS() {
      return rms;
    }

    @Override
    public int compareTo(Pair p) {
      if (p.getTimestamp() < this.getTimestamp())
        return 1;
      else if (p.getTimestamp() > this.getTimestamp())
        return -1;
      else
        return 0;
    }
    
  }
  
  
  /**
   * Add a method for confidence monitoring to a pipeline capturing audio by
   * teeing the raw data a pulling it to the appsink element.
   * 
   * @param p pipeline to add audio monitoring to
   * @param src the source element which will be tee'd
   * @param sink the sink element which the src originally sent data to
   * @param interval how often to grab data from the pipeline
   * @param maxLength The number of seconds to store RMS data for
   * @param name The friendly name of the device to add audio monitoring to
   * @return the pipeline with the audio monitoring added, or null on failure
   */
  public static boolean addAudioMonitor(Pipeline pipeline, Element src, Element sink, final long interval, final long maxLength, final String name) {
          
      Element tee, queue0, queue1, decodebin, fakesink;
      final Element level;
      
      // setup structure to associate timestamps with RMS values
      if (deviceRMSValues == null) {
        deviceRMSValues = new HashMap<String, SortedSet<Pair>>();
      }
      deviceRMSValues.put(name, new TreeSet<Pair>());
      
      
      
      
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
              
              // add the new value (timestamp, rms) value pair to the hashmap for this device
              TreeSet<Pair> deviceRMS = (TreeSet<Pair>) deviceRMSValues.get(name);
              deviceRMS.add(new Pair(System.currentTimeMillis(), value));
              
              // keep the maximum number of pairs stored to be 1000 / interval
              
              if (deviceRMS.size() > (maxLength / interval)) {
                deviceRMS.remove(deviceRMS.first());
              }
            }
          }
        }
      });
      
      return true;
  }
  
  /**
   * Return all RMS values from device 'name' that occur after Unix time
   * 'timestamp'
   * 
   * @param name The friendly name of the device
   * @param timestamp Unix time in milliseconds marking start of RMS data
   * @return A List of RMS values that occur *after* timestamp
   */
  public static List<Double> getRMSValues(String name, double timestamp) {
    TreeSet<Pair> set = (TreeSet<Pair>) deviceRMSValues.get(name).tailSet(new Pair(timestamp, 0));
    List<Double> rmsValues = new LinkedList<Double>();
    for (Pair p : set) {
      rmsValues.add(p.getRMS());
    }
    return rmsValues;
  }

}
