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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.gstreamer.Bus;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.event.EOSEvent;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.RecordingState;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.util.Compressor;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// TODO: Eliminate dependency with MediaInspector in pom.xml, if it is not finally used

/**
 * FIXME -- Add javadocs
 */
public class CaptureAgentImpl implements CaptureAgent, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);

  private Pipeline pipe = null;
  //private MediaInspectionService inspector;
  //private Properties props = null;
 
  public static final String tmpPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "captures";
  private final File tmpDir = new File(tmpPath);

  public CaptureAgentImpl() {
    createTmpDirectory();
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture()
   */
  public String startCapture() {
    
    logger.info("[startCapture]Setting up default values for MediaPackage and properties...");
    
    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("[startCapture]Wrong configuration for the default media package " + e.getMessage());
      return "Wrong MediaPackage configuration";
    } catch (MediaPackageException e) {
      logger.error("[startCapture]Media Package exception" + e.getMessage());
      return "Media Package exception";
    }
    
    return startCapture(pack, null);
    
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public String startCapture(MediaPackage mediaPackage) {

    logger.info("[startCapture]Setting up default values for the capture properties...");
    
    return startCapture(mediaPackage, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(java.util.HashMap)
   */
  public String startCapture(Properties properties) {
    logger.info("[startCapture]Setting up default values for the capture properties...");
    
    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("[startCapture]Wrong configuration for the default media package " + e.getMessage());
      return "Wrong MediaPackage configuration";
    } catch (MediaPackageException e) {
      logger.error("[startCapture]Media Package exception" + e.getMessage());
      return "Media Package exception";
    }

    return startCapture(pack, properties);
  }

  /**
   * {@inheritDoc}
   * 
   * @see 
   *      org.opencastproject.recorder.api.CaptureAgent#startCapture(org.opencastproject.media.mediapackage.MediaPackage,
   *      HashMap properties)
   */
  public String startCapture(MediaPackage mediaPackage, Properties properties) {
    
    // Deletes possibly existing file "capture.stopped"
    File stopped = new File(tmpDir.getAbsolutePath() + File.separator + "capture.stopped");
    
    if (stopped.exists())
      if (!stopped.delete()) {
        logger.error("\"capture.stopped\" could not be deleted");
        return "\"capture.stopped\" could not be deleted";
      }
    
    logger.info("Initializing devices for capture...");
    
    ConfigurationManager.getInstance().merge(properties);
    
    pipe = PipelineFactory.create(properties);
    
    if (pipe == null)
      return "Capture could not start.";
    
    Bus bus = pipe.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject arg0) {
         stopCapture();
      }
    });
    bus.connect(new Bus.ERROR() {
      public void errorMessage(GstObject arg0, int arg1, String arg2) {
        logger.error(arg0.getName() + ": " + arg2);
        stopCapture();
      }
    });
    
    pipe.play();
    
    // It **SEEMS** the state changes are immediate (for the test i've done so far)
    //while (pipe.getState() != State.PLAYING);
    logger.info(pipe.getName() + " started.");
    //Gst.main();
    //Gst.deinit();

    //TODO:  Get valid id for this recording
    logRecordingState("?", RecordingState.CAPTURING);
    return "Capture started";
  }

  /**
   * Create the tmp folder to store the record.
   */
  private void createTmpDirectory() {
    if (!tmpDir.exists()) {
      try {
        logger.info("Make directory " + CaptureAgentImpl.tmpPath);
        FileUtils.forceMkdir(tmpDir);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture()
   */
  public String stopCapture() {
    if (pipe == null)
      return "Pipeline is null.";
    String result = "Capture OK";
    File stopFlag = new File(tmpDir.getAbsolutePath() + File.separator + "capture.stopped");

    //TODO:  Get valid id for this recording
    logRecordingState("?", RecordingState.CAPTURE_FINISHED);
    
    try {
      // "READY" is the idle state for pipelines
      pipe.sendEvent(new EOSEvent());
      //while (pipe.getState() != State.NULL);
      //pipe.setState(State.NULL);
      
      Gst.deinit();
      
      stopFlag.createNewFile();
    } catch (IOException e) {
      logger.error("IOException: Could not create \"capture.stopped\" file" + e.getMessage());
      result = "\"capture.stopped\" could not be created";
    }
    // Check all the output files are correctly generated
    //String[] fileNames = props.values().toArray(new String[props.size()]);
    //long duration = -1;
    
    // For the moment checks all files have the same duration and it's longer than 1 sec.
    /*
    try {
      for (String fileName : fileNames) {
        File aFile = new File(fileName);
        if (aFile.exists()) {
          long tempDuration = inspector.inspect(aFile.toURL()).getDuration();
            
          // If duration == -1 we need to initialize it
          if (duration == -1)
            if (tempDuration > 1000)
              duration = tempDuration;
            else {
              result = "Invalid media file: " + fileName;
              break;
            }
          
          if (tempDuration != duration) {
            result = "Unexpected duration (" + tempDuration +") in file " + fileName;
            result += "\n(Should be " + duration + ")";
            break;
          }
        } else
          result = "File " + fileName + "not found";
      }
    } catch (MalformedURLException e) {
      result = "Malformed URL Exception";
    }
    */
    return result;
  }
  
  /**
   * Compresses the files contained in the output directory
   * @param zipName - The name of the zip file created
   * @return A File reference to the file zip created
   */
  public File zipFiles(String zipName) {
    String[] totalFiles = tmpDir.list();
    String[] filesToZip = new String[totalFiles.length - 1];
    int i = 0;
    
    for (String item : totalFiles)
      if (!item.equals("capture.stopped"))
          filesToZip[i++] = item;
    
    return new Compressor().zip(filesToZip, tmpDir.getAbsolutePath()+File.separator+zipName);
  }

  public void logRecordingState(String id, String state) {
    ConfigurationManager config = ConfigurationManager.getInstance();
    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.RECORDING_STATUS_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for " + CaptureParameters.RECORDING_STATUS_ENDPOINT_URL + " is invalid, unable to push recording status to remote server");
      return;
    }
    HttpPost remoteServer = new HttpPost(url);
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("id", id));
    formParams.add(new BasicNameValuePair("state", state));

    //Send the data
    try {
      remoteServer.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
      HttpClient client = new DefaultHttpClient();
      client.execute(remoteServer);
    } catch (Exception e) {
      logger.error("Unable to push agent status to remote server", e);
    }
  }
  
  /**
   * Obtains a MediaInspectionService object from the OSGI service
   * @param inspector
   */
/*  public void setInspector(MediaInspectionService inspector) {
    this.inspector = inspector;
  }
*/  
  
  /**
   * We need this to invoke Gst.quit() and Gst.deinit() in case something unexpected occurs
   * Shouldn't this be on PipelineFactory? That's the class where the Pipeline is created
   * {@inheritDoc}
   * @see java.lang.Object#finalize()
   */
/*  protected void finalize() {
    if (pipe != null) {
      pipe.setState(State.NULL);
      while (pipe.getState() != State.NULL)
        ;
    }
    //Gst.quit();      // Is it necessary??
    Gst.deinit();
  }
*/  
  
  
  
  
  /*private static void main(String args[]) {
    
    File outputFile = new File("ogg.ogg");
    
    if (outputFile.exists())
      outputFile.delete();
    
    staticCapture();
    System.out.println("Salida de staticCapture");
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("Fin de temporizaci�n");
    
    System.out.println("Enviamos evento fin de stream");
    pipe.sendEvent(new EOSEvent());
    
    Gst.deinit();
    
    System.out.println("fin");
  }
*/
}
