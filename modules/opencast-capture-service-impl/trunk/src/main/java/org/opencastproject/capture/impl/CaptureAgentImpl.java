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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.media.mediapackage.MediaPackage;

import org.apache.commons.io.FileUtils;
import org.gstreamer.Bus;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * FIXME -- Add javadocs
 */
public class CaptureAgentImpl implements CaptureAgent, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);
  
  public static String tmpPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast";

  public CaptureAgentImpl() {
    this.tmpPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "tmp";
    createTmpDirectory();
  }
  
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture()
   */
  public String startCapture() {
    String aux = "start capture";
    //logger.info("public String startCapture() in "  + this.tmpPath + File.separator + "record");
    Properties props = new Properties();
    props.setProperty("/dev/video0", "/home/kta719/gst-testing/professor.mpg");
    props.setProperty("/dev/video1", "/home/kta719/gst-testing/screen.mpg");
    props.setProperty("hw:0", "/home/kta719/gst-testing/microphone.mp2");
    
    long duration = 10;
    final Pipeline pipe = PipelineFactory.create(props);
    Bus bus = pipe.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject arg0) {
        System.out.println(pipe.getName() + " received EOS.");
        pipe.setState(State.NULL);
        Gst.quit();
      }
    });
    bus.connect(new Bus.ERROR() {
      public void errorMessage(GstObject arg0, int arg1, String arg2) {
        System.err.println(arg0.getName() + ": " + arg2);
        Gst.quit();
      }
    });

    pipe.play();
    while (pipe.getState() != State.PLAYING);
    System.out.println(pipe.getName() + " started.");
    new Thread(new CaptureAgentEOSThread(duration*1000, pipe)).start();
    Gst.main();
    Gst.deinit();
    
    return aux;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public String startCapture(MediaPackage mediaPackage) {
    logger.info("public String startCapture(MediaPackage mediaPackage)");
    return "start capture 2";
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(java.util.HashMap)
   */
    public String startCapture(HashMap<String, String> properties) {
    logger.info("public String startCapture(HashMap properties)");
    return "start capture 3";
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(org.opencastproject.media.mediapackage.MediaPackage, HashMap properties)
   */
    public String startCapture(MediaPackage mediaPackage, HashMap<String, String> properties) {
    logger.info("public String startCapture(MediaPackage mediaPackage, HashMap properties)");
    return "start capture 4";
  }

  /**
   * Create the tmp folder to store de record.
   */
  private void createTmpDirectory() {
    File f = new File(this.tmpPath);
    if( ! f.exists()) {
      try {
        logger.error("Make directory " + this.tmpPath);
        FileUtils.forceMkdir(f);
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}

