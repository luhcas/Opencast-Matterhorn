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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.gstreamer.Bus;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.event.EOSEvent;
import org.opencastproject.capture.api.AgentState;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.RecordingState;
import org.opencastproject.capture.api.StatusService;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.util.Compressor;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


// TODO: Eliminate dependency with MediaInspector in pom.xml, if it is not finally used

/**
 * Implementation of the Capture Agent: using gstreamer, generates several Pipelines
 * to store several tracks from a certain recording.
 */
public class CaptureAgentImpl implements CaptureAgent, StatusService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);

  private Pipeline pipe = null;
  //TODO:  Document this, what does it contain?
  private Properties props = null;
  private ConfigurationManager config = ConfigurationManager.getInstance();

  /** Used by the AgentStatusJob class to pull a pointer to the CaptureAgentImpl so it can poll for status updates */
  public static final String SERVICE = "status_service";
  /** The agent's current state.  Used for logging */
  private String agent_state = AgentState.UNKNOWN;
  /** The recording's current state.  Used for logging */
  private String recording_state = RecordingState.UNKNOWN;

  //TODO:  Document this, and read it from the config manager
  public static final String tmpPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "captures";
  //TODO:  Document this
  private final File tmpDir = new File(tmpPath);
  //TODO:  Document this
  private final File manifest = new File(tmpDir.getAbsolutePath()+File.separator+"manifest.xml");

  /**
   * Builds an instance of the Capture agent.
   */
  public CaptureAgentImpl() {
    createTmpDirectory();
  }

  /**
   * Called when the bundle is activated.
   * @param cc The component context
   */
  public void activate(ComponentContext cc) {
    logger.info("Starting CaptureAgentImpl.");
    createPollingTask();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture()
   */
  public String startCapture() {

    logger.info("Starting capture using default values for MediaPackage and properties.");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e.getMessage());
      return "Wrong MediaPackage configuration";
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
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

    logger.info("Starting capture using default values for the capture properties and a passed in media package.");
    
    return startCapture(mediaPackage, null);

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(java.util.HashMap)
   */
  public String startCapture(Properties properties) {
    logger.info("Starting capture using a passed in properties and default media package.");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e.getMessage());
      return "Wrong MediaPackage configuration";
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
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

    if (stopped.exists()) {
      if (!stopped.delete()) {
        logger.error("\"capture.stopped\" could not be deleted.");
        return "\"capture.stopped\" could not be deleted.";
      }
    }

    logger.info("Initializing devices for capture.");

    // merges properties without overwriting the system's configuration
    Properties merged = config.merge(properties, false);
    
    pipe = PipelineFactory.create(merged);

    
    if (pipe == null) {
      logger.error("Capture could not start, pipeline was null!");
      return "Capture could not start, pipline was null!";
    }

    Bus bus = pipe.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject arg0) {
        stopCapture();
      }
    });
    bus.connect(new Bus.ERROR() {
      public void errorMessage(GstObject arg0, int arg1, String arg2) {
        //TODO:  What does this mean?
        logger.error(arg0.getName() + ": " + arg2);
        stopCapture();
      }
    });

    pipe.play();

    // It **SEEMS** the state changes are immediate (for the test i've done so far)
    //while (pipe.getState() != State.PLAYING);
    logger.info("{} started.", pipe.getName());
    //Gst.main();
    //Gst.deinit();

    setRecordingState(RecordingState.CAPTURING);
    setAgentState(AgentState.CAPTURING);
    return "Capture started";
  }

  /**
   * Create the tmp folder to store the record.
   */
  private void createTmpDirectory() {
    if (!tmpDir.exists()) {
      try {
        logger.info("Making directory {}.", CaptureAgentImpl.tmpPath);
        FileUtils.forceMkdir(tmpDir);
        if (!tmpDir.exists()) {
          throw new RuntimeException("Unable to create directory " + CaptureAgentImpl.tmpPath + ".");
        }
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
    if (pipe == null) {
      logger.warn("Pipeline is null, unable to stop capture.");
      return "Pipeline is null.";
    }
    String result = "Capture OK";
    File stopFlag = new File(tmpDir.getAbsolutePath(), "capture.stopped");
    
    try {
      // "READY" is the idle state for pipelines
      pipe.sendEvent(new EOSEvent());
      //while (pipe.getState() != State.NULL);
      //pipe.setState(State.NULL);

      Gst.deinit();

      stopFlag.createNewFile();
    } catch (IOException e) {
      logger.error("IOException: Could not create \"capture.stopped\" file: {}.", e.getMessage());
      result = "\"capture.stopped\" could not be created.";
      //TODO:  Is this capture.stopped file required for ingest to work?  Then it should die here rather than trying to build the manifest.
    }

    // Does the manifest
    try {
      doManifest();
      setRecordingState(RecordingState.CAPTURE_FINISHED);
    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: {}.", e.getMessage());
      result = "MediaPackage Exception: " + e.getMessage();
      setRecordingState(RecordingState.CAPTURE_ERROR);
    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: {}.", e.getMessage());
      result = "Unsupported Element Exception: " + e.getMessage();
      setRecordingState(RecordingState.CAPTURE_ERROR);
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e.getMessage());
      result = "I/O Exception: " + e.getMessage();
      setRecordingState(RecordingState.CAPTURE_ERROR);
    } catch (TransformerException e) {
      logger.error("Transformer Exception: {}.", e.getMessage());
      result = "Transformer Exception: " + e.getMessage();
      setRecordingState(RecordingState.CAPTURE_ERROR);
    } finally {
      setAgentState(AgentState.IDLE);
    }

    //TODO:  Either remove this code or bring it back.  We need to comment/log it (per MH-1597) if it's going to stay

    return result;
  }

  /**
   * Creates the Quartz task which pushes the agent's status to the status server
   */
  private void createPollingTask() {
    try {
      long pollTime = Long.parseLong(config.getItem(CaptureParameters.AGENT_STATUS_POLLING_INTERVAL)) * 1000L;
      Properties pollingProperties = new Properties();
      pollingProperties.load(getClass().getClassLoader().getResourceAsStream("config/misc.properties"));
      StdSchedulerFactory sched_fact = new StdSchedulerFactory(pollingProperties);
  
      //Create and start the scheduler
      Scheduler pollScheduler = sched_fact.getScheduler();
      if (pollScheduler.getJobGroupNames().length > 0) {
        logger.info("createPollingTask has already been called.  Stop freakin' calling it already!");
        return;
      }
      pollScheduler.start();
  
      //Setup the polling
      JobDetail job = new JobDetail("agentStatusUpdate", Scheduler.DEFAULT_GROUP, AgentStatusJob.class);
      //TODO:  Support changing the polling interval
      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger trigger = new SimpleTrigger("status_polling", Scheduler.DEFAULT_GROUP, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, pollTime);

      trigger.getJobDataMap().put(SERVICE, this);

      //Schedule the update
      pollScheduler.scheduleJob(job, trigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push status to remote server!", CaptureParameters.AGENT_STATUS_POLLING_INTERVAL);
    } catch (IOException e) {
      logger.error("IOException caught in CaptureAgentImpl: {}.", e.getMessage());
    } catch (SchedulerException e) {
      logger.error("SchedulerException in CaptureAgentImpl: {}.", e.getMessage());
    }
  }

  /**
   * Sets the machine's current encoding status
   * 
   * @param state The state for the agent.  Defined in AgentState.
   * @see org.opencastproject.capture.api.AgentState
   */
  private void setAgentState(String state) {
    agent_state = state;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.StatusService#getAgentState()
   */
  public String getAgentState() {
    return agent_state;
  }

  /**
   * Sets the recording's current state
   * 
   * @param state The state for the recording.  Defined in RecordingState.
   * @see org.opencastproject.capture.api.RecordingState
   */
  private void setRecordingState(String state) {
    recording_state = state;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.StatusService#getRecordingState()
   */
  public String getRecordingState() {
    return recording_state;
  }


  /**
   * Generates the manifest.xml file from the files specified in the properties
   * @return A String indicating the success or fail of the operation
   * @throws MediaPackageException 
   * @throws UnsupportedElementException 
   * @throws IOException 
   * @throws TransformerException 
   */
  private boolean doManifest() throws MediaPackageException, UnsupportedElementException, IOException, TransformerException {
    logger.debug("Generating manifest.");

    // Generates the manifest
    try {
      MediaPackage pkg = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

      // Inserts the tracks in the MediaPackage
      // TODO Specify the flavour
      String deviceNames = config.getItem(CaptureParameters.CAPTURE_DEVICE_NAMES);
      if (deviceNames == null) {
        logger.error("No capture devices specified in " + CaptureParameters.CAPTURE_DEVICE_NAMES);
        return false;
      }
      
      String[] friendlyNames = deviceNames.split(",");
      
      for (String name : friendlyNames) {
        name = name.trim();
       
        // Disregard the empty string
        if (name.equals(""))
          continue;
        
        String fileName = config.getItem(CaptureParameters.CAPTURE_DEVICE_PREFIX + "." + name + ".outputfile");
        
        if (fileName == null)
          continue;
     
        File outputFile = new File(fileName);

        if (outputFile.exists())
            // Adds a track
            pkg.add(new URL(outputFile.getName()));
      }

      // TODO insert a catalog with some capture metadata
      
      // Gets the manifest.xml as a Document object
      Document doc = pkg.toXml();

      // Defines a transformer to convert the object in a xml file
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      // Initializes StreamResult with File object to save to file
      StreamResult stResult = new StreamResult(new FileOutputStream(manifest));
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, stResult);

      // Closes the stream to make sure all the content is written to the file
      stResult.getOutputStream().close();
    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: {}.", e.getMessage());
      throw e;
    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: {}.", e.getMessage());
      throw e;
    } catch (TransformerException e) {
      logger.error("Transformer Exception: {}.", e.getMessage());
      throw e;
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e.getMessage());
      throw e;
    }

    return true;
    
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
        if (!(item.equals("capture.stopped") ||
                item.substring(item.lastIndexOf('.')).trim().equals("zip")))
          filesToZip[i++] = tmpDir.getAbsolutePath()+File.separatorChar+item;

      return new Compressor().zip(filesToZip, zipName);
    }
    
    /**
     * Sends a file to the REST ingestion service
     * @param url : The service URL
     * @param fileDesc : The descriptor for the media package
     */
    public String doIngest(String url, File fileDesc) {

      logger.info("Beginning ingest of recording.");

      HttpClient client = new DefaultHttpClient();
      HttpPost postMethod = new HttpPost(url);
      String retValue = null;

      setAgentState(AgentState.UPLOADING);
      setRecordingState(RecordingState.UPLOADING);
      
      try {
        // Set the file as the body of the request
        postMethod.setEntity(new FileEntity(fileDesc, URLConnection.getFileNameMap().getContentTypeFor(fileDesc.getName())));

        // Send the file
        HttpResponse response = client.execute(postMethod);
        
        retValue = response.getStatusLine().getReasonPhrase();

        setRecordingState(RecordingState.UPLOAD_FINISHED);
      } catch (ClientProtocolException e) {
        logger.error("Failed to submit the data: {}.", e.getMessage());
        setRecordingState(RecordingState.UPLOAD_ERROR);
      } catch (IOException e) {
        logger.error("I/O Exception: {}.", e.getMessage());
        setRecordingState(RecordingState.UPLOAD_ERROR);
      } finally {
        client.getConnectionManager().shutdown();
        setAgentState(AgentState.IDLE);
      }

      return retValue;
    }

    public void updated(Dictionary props) throws ConfigurationException {
      // Update any configuration properties here
    }
  }
