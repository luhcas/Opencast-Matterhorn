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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.api.ConfidenceMonitor;
import org.opencastproject.capture.api.ScheduledEvent;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.jobs.AgentCapabilitiesJob;
import org.opencastproject.capture.impl.jobs.AgentStateJob;
import org.opencastproject.capture.impl.jobs.JobParameters;
import org.opencastproject.capture.impl.jobs.LoadRecordingsJob;
import org.opencastproject.capture.pipeline.AudioMonitoring;
import org.opencastproject.capture.pipeline.InvalidDeviceNameException;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.XProperties;
import org.opencastproject.util.ZipUtil;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.gstreamer.Bus;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.event.EOSEvent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.component.ComponentContext;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Implementation of the Capture Agent: using gstreamer, generates several Pipelines
 * to store several tracks from a certain recording.
 */
public class CaptureAgentImpl implements CaptureAgent, StateService, ConfidenceMonitor, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);

  /** The default maximum length to capture, measured in seconds. */
  public static final long DEFAULT_MAX_CAPTURE_LENGTH = 8 * CaptureParameters.HOURS;

  /** The number of nanoseconds in a second.  This is a borrowed constant from gStreamer and is used in the pipeline initialisation routines */
  public static final long GST_SECOND = 1000000000L;
  
  /** The capture type for audio devices */
  public static final String CAPTURE_TYPE_AUDIO = "audio";
  /** The capture type for video devices */
  public static final String CAPTURE_TYPE_VIDEO = "video";

  /** The agent's pipeline. **/
  Pipeline pipe = null;

  /** Pipeline for confidence monitoring while agent is idle */
  Pipeline confidencePipe = null;

  /** Keeps the recordings which have not been succesfully ingested yet. **/
  Map<String, AgentRecording> pendingRecordings = new ConcurrentHashMap<String, AgentRecording>();

  /** Keeps the recordings which have been successfully ingested. */
  Map<String, AgentRecording> completedRecordings = new ConcurrentHashMap<String, AgentRecording>();

  /** The agent's name. */
  String agentName = null;

  /** The agent's current state.  Used for logging. */
  String agentState = null;

  /** A pointer to the scheduler. */
  org.opencastproject.capture.api.Scheduler scheduler = null;

  /** The scheduler the agent will use to schedule any recurring events */
  org.quartz.Scheduler agentScheduler = null;

  /** The configuration manager for the agent. */
  ConfigurationManager configService = null;

  /** The http client used to communicate with the core */
  TrustedHttpClient client = null;

  /** Indicates the ID of the recording currently being recorded. **/
  String currentRecID = null;

  /** Is confidence monitoring enabled? */
  boolean confidence = false;

  /** Stores the start time of the current recording */
  long startTime = -1l;

  /** Stores the current bundle context */
  ComponentContext context = null;

    /**
   * Sets the configuration service form which this capture agent should draw its configuration data.
   * @param cfg The configuration service.
   */
  public void setConfigService(ConfigurationManager cfg) {
    configService = cfg;
    agentName = configService.getItem(CaptureParameters.AGENT_NAME);
  }

  /**
   * Sets the scheduler service which this service uses to schedule stops for unscheduled captures.
   * @param s The scheduler service.
   */
  public void setScheduler(org.opencastproject.capture.api.Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Sets the http client which this service uses to communicate with the core.
   * @param c The client object.
   */
  public void setTrustedClient(TrustedHttpClient c) {
    client = c;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#startCapture()
   */
  @Override
  public String startCapture() {

    logger.debug("startCapture()");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e.getMessage());
      return null;
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
      return null;
    }

    return startCapture(pack, configService.getAllProperties());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#startCapture(MediaPackage)
   */
  @Override
  public String startCapture(MediaPackage mediaPackage) {

    logger.debug("startCapture(mediaPackage): {}", mediaPackage);

    return startCapture(mediaPackage, configService.getAllProperties());

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.api.CaptureAgent#startCapture(Properties)
   */
  @Override
  public String startCapture(Properties properties) {
    // Creates default MediaPackage
    //TODO:  This is also done in the RecordingImpl class, should this code go away?
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Configuration Exception creating media package: {}.", e.getMessage());
      return null;
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
      return null;
    }

    return startCapture(pack, properties);
  }

  /**
   * {@inheritDoc}
   *
   * @see
   *      org.opencastproject.capture.api.CaptureAgent#startCapture(MediaPackage, Properties)
   */
  @Override
  public String startCapture(MediaPackage mediaPackage, Properties properties) {
    logger.debug("startCapture(mediaPackage, properties): {} {}", mediaPackage, properties);
    //Check to make sure we're not already capturing something
    if (currentRecID != null || !agentState.equals(AgentState.IDLE)) {
      logger.warn("Unable to start capture, a different capture is still in progress in {}.",
              pendingRecordings.get(currentRecID).getBaseDir().getAbsolutePath());
      //Set the recording's state to error
      if (properties != null && properties.getProperty(CaptureParameters.RECORDING_ID) != null) {
        setRecordingState(properties.getProperty(CaptureParameters.RECORDING_ID), RecordingState.CAPTURE_ERROR);
      } else {
        setRecordingState("Unscheduled-" + agentName + "-" + System.currentTimeMillis(), RecordingState.CAPTURE_ERROR);
      }
      return null;
    } else {
      setAgentState(AgentState.CAPTURING);
    }
    //Generate a combined properties object for this capture
    properties = configService.merge(properties, false);

    //Create the recording
    RecordingImpl newRec = createRecording(mediaPackage, properties);
    if (newRec == null) {
      if (properties != null && properties.contains(CaptureParameters.RECORDING_ID)) {
        resetOnFailure((String) properties.get(CaptureParameters.RECORDING_ID));
      } else {
        resetOnFailure("Unscheduled-" + agentName + "-" + System.currentTimeMillis());
      }
      return null;
    }

    //Init the pipeline and break out if it fails
    String recordingID = initPipeline(newRec);
    if (recordingID == null) {
      resetOnFailure(newRec.getID());
      return null;
    }

    //Great, capture is running.  Set the agent state appropriately.
    setRecordingState(recordingID, RecordingState.CAPTURING);
    //If the recording does *not* have a scheduled endpoint then schedule one.
    if (newRec.getProperty(CaptureParameters.RECORDING_END) == null) {
      if (!scheduleStop(newRec.getID())) {
        //If the attempt to schedule an end to the recording fails then shut everything down.
        stopCapture(newRec.getID(), false);
        resetOnFailure(newRec.getID());
      }
    }

    serializeRecording(recordingID);
    return recordingID;
  }

  /**
   * Creates a RecordingImpl instance used in a capture.  Also adds the recording to the agent's internal list of upcoming recordings.
   * @param mediaPackage The media package to create the recording around
   * @param properties The properties of the recording
   * @return The RecordingImpl instance, or null in the case of an error
   */
  protected RecordingImpl createRecording(MediaPackage mediaPackage, Properties properties) {
    // Creates a new recording object, checking if it was correctly initialized
    RecordingImpl newRec = null;
    try {
      newRec = new RecordingImpl(mediaPackage, configService.merge(properties, false));
    } catch (IllegalArgumentException e) {
      logger.error("Recording not created: {}", e.getMessage());
      return null;
    } catch (IOException e) {
      logger.error("Recording not created due to an I/O Exception: {}", e.getMessage());
      return null;
    }
    // Checks there is no duplicate ID
    String recordingID = newRec.getID();
    if (pendingRecordings.containsKey(recordingID)) {
      logger.error("Can't create a recording with ID {}: there is already another recording with such ID", recordingID);
      return null;
    } else {
      pendingRecordings.put(recordingID, newRec);
      currentRecID = recordingID;
      return newRec;
    }
  }

  /**
   * Creates the gStreamer pipeline and blocks until it starts successfully
   * @param newRec The RecordingImpl of the capture we wish to perform.
   * @return The recording ID (equal to newRec.getID()) or null in the case of an error
   */
  protected String initPipeline(RecordingImpl newRec) {
    //Create the pipeline
    try {
      pipe = PipelineFactory.create(newRec.getProperties(), false, this);
    } catch (UnsatisfiedLinkError e) {
      logger.error(e.getMessage() + " : please add libjv4linfo.so to /usr/lib to correct this issue.");
      return null;
    } 
    
    //Check if the pipeline came up ok
    if (pipe == null) {
      logger.error("Capture {} could not start, pipeline was null!", newRec.getID());
      resetOnFailure(newRec.getID());
      return null;
    }

    logger.info("Initializing devices for capture.");

    //Hook up the shutdown handlers
    Bus bus = pipe.getBus();
    bus.connect(new Bus.EOS() {
      /**
       * {@inheritDoc}
       * @see org.gstreamer.Bus.EOS#endOfStream(org.gstreamer.GstObject)
       */
      public void endOfStream(GstObject arg0) {
        logger.debug("Pipeline received EOS.");
        pipe.setState(State.NULL);
        pipe = null;
      }
    });
    bus.connect(new Bus.ERROR() {
      /**
       * {@inheritDoc}
       * @see org.gstreamer.Bus.ERROR#errorMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      public void errorMessage(GstObject obj, int retCode, String msg) {
        logger.warn("{}: {}", obj.getName(), msg);
      }
    });
    bus.connect(new Bus.WARNING() {
      /**
       * {@inheritDoc}
       * @see org.gstreamer.Bus.WARNING#warningMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      public void warningMessage(GstObject obj, int retCode, String msg) {
        logger.warn("{}: {}", obj.getName(), msg);
      }
    });

    // Grab time to wait for pipeline to start
    int wait;
    String waitProp = newRec.getProperty(CaptureParameters.CAPTURE_START_WAIT);
    if (waitProp != null) {
      wait = Integer.parseInt(waitProp);
    } else {
      wait = 5; // Default taken from gstreamer docs
    }

    //Try and start the pipline
    pipe.play();
    if (pipe.getState(wait * GST_SECOND) != State.PLAYING) {
      logger.error("Unable to start pipeline after {} seconds.  Aborting!", wait);
      return null;
    }
    logger.info("{} started.", pipe.getName());
    startTime = System.currentTimeMillis();

    return newRec.getID();
  }

  /**
   * Convenience method to reset an agent when a capture fails to start.
   * @param recordingID The recordingID of the capture which failed to start.
   */
  protected void resetOnFailure(String recordingID) {
    setAgentState(AgentState.IDLE);
    setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
    currentRecID = null;
  }

  /**
   * Schedules a stopCapture call for unscheduled captures.
   * @param recordingID The recordingID to stop.
   * @return true if the stop was scheduled, false otherwise.
   */
  protected boolean scheduleStop(String recordingID) {
    String maxLength = pendingRecordings.get(recordingID).getProperty(CaptureParameters.CAPTURE_MAX_LENGTH);
    long length = 0L;
    if (maxLength != null) {
      //Try and parse the value found, falling back to the agent's hardcoded max on error
      try {
        length = Long.parseLong(maxLength);
      } catch (NumberFormatException e) {
        configService.setItem(CaptureParameters.CAPTURE_MAX_LENGTH, String.valueOf(CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH));
        length = CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH;
      }
    } else {
      configService.setItem(CaptureParameters.CAPTURE_MAX_LENGTH, String.valueOf(CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH));
      length = CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH;
    }

    //Convert from seconds to milliseconds
    length = length * 1000L;
    Date stop = new Date(length + System.currentTimeMillis());
    if (scheduler != null) {
      logger.debug("Scheduling stop for recording {} at {}.", recordingID, stop);
      return scheduler.scheduleUnscheduledStopCapture(recordingID, stop);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture(boolean)
   */
  @Override
  public boolean stopCapture(boolean immediateIngest) {
    logger.debug("stopCapture() called.");
    setAgentState(AgentState.SHUTTING_DOWN);
    // If pipe is null and no mock capture is on
    if (pipe == null) {
        logger.warn("Pipeline is null, this is normal if running a mock capture.");
        setAgentState(AgentState.IDLE);
    } else {
      // We must stop the capture as soon as possible, then check whatever needed
      if (!pipe.sendEvent(new EOSEvent())) {
        logger.warn("Unable to send EOS event to pipeline.");
        setAgentState(AgentState.IDLE);
        return false;
      }
      long startWait = System.currentTimeMillis();
      long timeout = 60000l;
      if (configService.getItem(CaptureParameters.RECORDING_SHUTDOWN_TIMEOUT) == null) {
        logger.warn("Unable to find shutdown timeout value.  Assuming 1 minute.  Missing key is {}.", CaptureParameters.RECORDING_SHUTDOWN_TIMEOUT);
      } else {
        timeout = Long.parseLong(configService.getItem(CaptureParameters.RECORDING_SHUTDOWN_TIMEOUT)) * 1000l;
      }
      while (pipe != null) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {}

        //If we've timed out then force kill the pipeline
        if (System.currentTimeMillis() - startWait >= timeout) {
          if (pipe != null) {
            pipe.setState(State.NULL);
          }
          pipe = null;
        }
      }

      // Checks there is a currentRecID defined --should always be
      if (currentRecID == null) { 
        logger.warn("There is no currentRecID assigned, but the Pipeline was not null!");
        setAgentState(AgentState.IDLE);
        return false;
      }
    }

    AgentRecording theRec = pendingRecordings.get(currentRecID);
    if (theRec == null) {
      logger.info("Stop capture called, but no capture running. This is normal if running a mock capture.");
      return true;
    }

    // Clears currentRecID to indicate no recording is on
    currentRecID = null;

    //Update the states of everything.
    setRecordingState(theRec.getID(), RecordingState.CAPTURE_FINISHED);
    setAgentState(AgentState.IDLE);
    serializeRecording(theRec.getID());

    theRec.setProperty(CaptureParameters.RECORDING_DURATION, String.valueOf(System.currentTimeMillis() - startTime));
    startTime = -1l;

    logger.info("Recording \"{}\" succesfully stopped", theRec.getID());

    if (immediateIngest) {
      if (scheduler.scheduleSerializationAndIngest(theRec.getID())) {
        logger.info("Ingest scheduled for recording {}.", theRec.getID());
      } else {
        logger.warn("Ingest scheduling failed for recording {}!", theRec.getID());
        setRecordingState(theRec.getID(), RecordingState.UPLOAD_ERROR);
      }
    }

    return true;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture(java.lang.String, boolean)
   */
  @Override
  public boolean stopCapture(String recordingID, boolean immediateIngest) {
    if (currentRecID != null) {
      if (recordingID.equals(currentRecID)) {
        return stopCapture(immediateIngest);
      } else {
        logger.debug("Current capture ID does not match parameter capture ID, ignoring stopCapture call.");
      }
    } else {
      logger.debug("No capture in progress, ignoring stopCapture call.");
    }
    return false;
  }

  /**
   * Generates the manifest.xml file from the files specified in the properties
   * @param recID The ID for the recording whose manifest will be created
   * @return A state boolean
   * @throws IOException
   * @throws NoSuchAlgorithmException
   */
  public boolean createManifest(String recID) throws NoSuchAlgorithmException, IOException {

    AgentRecording recording = pendingRecordings.get(recID);
    if (recording == null) {
      logger.error("[createManifest] Recording {} not found!", recID);
      setRecordingState(recID, RecordingState.MANIFEST_ERROR);
      return false;
    } else {
      logger.debug("Generating manifest for recording {}", recID);
      setRecordingState(recording.getID(), RecordingState.MANIFEST);
    }

    //Get the list of device names so we can attach all the files appropriately (re: flavours, etc)
    String[] friendlyNames = recording.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES).split(",");
    if (friendlyNames.length == 1 && friendlyNames[0].equals("")) {
      //Idiot check against blank name lists.
      logger.error("Unable to build mediapackage for recording {} because the device names list is blank!", recID);
      return false;
    }

    MediaPackageElementBuilder elemBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElementFlavor flavor = null;
    // Includes the tracks in the MediaPackage
    try {

      // Adds the files present in the Properties
      for (String name : friendlyNames) {
        name = name.trim();

        String flavorPointer = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_FLAVOR;
        String flavorString = recording.getProperty(flavorPointer);
        flavor = MediaPackageElementFlavor.parseFlavor(flavorString);

        String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_DEST;
        File outputFile = new File(recording.getBaseDir(), recording.getProperty(outputProperty));

        // Adds the file to the MediaPackage
        if (outputFile.exists()) {
          //TODO:  This should really be Track rather than TrackImpl, but otherwise a bunch of functions we need disappear...
          TrackImpl t = (TrackImpl) elemBuilder.elementFromURI(
                  outputFile.toURI(),
                  MediaPackageElement.Type.Track,
                  flavor);
          t.setSize(outputFile.length());
          String[] detectedMimeType = new MimetypesFileTypeMap().getContentType(outputFile).split("/");
          t.setMimeType(new MimeType(detectedMimeType[0], detectedMimeType[1]));
          t.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, outputFile));
          if (recording.getProperty(CaptureParameters.RECORDING_DURATION) != null) {
            t.setDuration(Long.parseLong(recording.getProperty(CaptureParameters.RECORDING_DURATION)));
          }

          //Commented out because this does not work properly with mock captures.  Also doesn't do much when it does work...
          /*if (name.contains("hw:")) {
            AudioStreamImpl stream = new AudioStreamImpl(outputFile.getName());
            t.addStream(stream);
          } else {
            VideoStreamImpl stream = new VideoStreamImpl(outputFile.getName());
            t.addStream(stream);
          }*/
          if (recording.getMediaPackage() == null) {
            logger.error("Recording media package is null!");
            return false;
          }
          recording.getMediaPackage().add(t);
        } else {
          // FIXME: Is the admin reading the agent logs? (jt)
          // FIXME: Who will find out why one of the tracks is missing from the media package? (jt)
          // FIXME: Think about a notification scheme, this looks like an emergency to me (jt)
          logger.error("Required file {} not found, aborting manifest creation!", outputFile.getName());
          setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
          return false;
        }
      }

    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: {}.", e.getMessage());
      setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
      return false;
    }

    // Serialize the metadata file and the MediaPackage
    FileOutputStream fos = null;
    try {
      logger.debug("Serializing metadata and MediaPackage...");

      // Gets the manifest.xml as a Document object and writes it to a file
      MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl(recording.getBaseDir());
      File manifestFile = new File(recording.getBaseDir(), CaptureParameters.MANIFEST_NAME);
      Result outputFile = new StreamResult(manifestFile);
      Document manifest = recording.getMediaPackage().toXml(serializer);

      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer t = tf.newTransformer();
      t.setOutputProperty(OutputKeys.INDENT, "yes");
      t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      t.transform(new DOMSource(manifest), outputFile);

    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: {}.", e.getMessage());
      setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
      return false;
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e.getMessage());
      setRecordingState(recording.getID(), RecordingState.MANIFEST_ERROR);
      return false;
    } catch (TransformerConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(fos);
    }

    setRecordingState(recording.getID(), RecordingState.MANIFEST_FINISHED);
    return true;
  }

  /**
   * Compresses the files contained in the output directory
   *
   * @param recID
   *      The ID for the recording whose files are going to be zipped
   * @return A File reference to the file zip created
   */
  public File zipFiles(String recID) {

    logger.debug("Compressing files...");
    AgentRecording recording = pendingRecordings.get(recID);
    if (recording == null) {
      logger.error("[zipFiles] Recording {} not found!", recID);
      setRecordingState(recID, RecordingState.COMPRESSING_ERROR);
      return null;
    } else {
      setRecordingState(recording.getID(), RecordingState.COMPRESSING);
    }

    Iterable<MediaPackageElement> mpElements = recording.getMediaPackage().elements();
    Vector<File> filesToZip = new Vector<File>();

    // Now adds the files from the MediaPackage
    for (MediaPackageElement item : mpElements) {
      File tmpFile = null;
      String elementPath = item.getURI().getPath();

      // Relative and absolute paths are mixed
      if (elementPath.startsWith("file:") || elementPath.startsWith(File.separator)) {
        tmpFile = new File(elementPath);
      } else {
        tmpFile = new File(recording.getBaseDir(), elementPath);
      }

      if (!tmpFile.isFile()) {
        // TODO: Is this really a warning or should we fail completely and return an error?
        logger.warn("Required file {} doesn't exist!", tmpFile.getAbsolutePath());
      }
      filesToZip.add(tmpFile);
    }
    filesToZip.add(new File(recording.getBaseDir(), CaptureParameters.MANIFEST_NAME));

    logger.info("Zipping {} files:", filesToZip.size());
    for (File f : filesToZip)
      logger.debug("--> {}", f.getName());

    //Nuke any existing zipfile, we want to recreate it if it already exists.
    File outputZip = new File(recording.getBaseDir(), CaptureParameters.ZIP_NAME);
    if (outputZip.isFile()) {
      outputZip.delete();
    }

    //Return a pointer to the zipped file
    File returnZip;
    try {
      returnZip = ZipUtil.zip(filesToZip.toArray(new File[filesToZip.size()]), outputZip, ZipUtil.NO_COMPRESSION);
    } catch (IOException e) {
      logger.error("An IOException has occurred while zipping the files for recording {}: {}", recID, e.getMessage());
      return null;
    } 
    
    return returnZip;
        
  }

  // FIXME: Replace HTTP-based ingest with remote implementation of the Ingest Service. (jt)
  // See the ComposerServiceRemoteImpl to get an idea of the approach
  // The idea is to get the details of the HTTP interaction out of the client code
  /**
   * Sends a file to the REST ingestion service.
   *
   * @param recID
   *      The ID for the recording to be ingested.
   * @return The status code for the http post, or one of a number of error values.
   *      The error values are as follows:
   *        -1:  Unable to ingest because the recording id does not exist
   *        -2:  Invalid ingest url
   *        -3:  Invalid ingest url
   *        -4:  Invalid ingest url
   *        -5:  Unable to open media package
   */
  public int ingest(String recID) {
    logger.info("Ingesting recording: {}", recID);
    AgentRecording recording = pendingRecordings.get(recID);

    if (recording == null) {
      logger.error("[ingest] Recording {} not found!", recID);
      return -1;
    }

    URL url = null;
    try {
      url = new URL(recording.getProperty(CaptureParameters.INGEST_ENDPOINT_URL));
    } catch (NullPointerException e) {
      logger.warn("Nullpointer while parsing ingest target URL.");
      return -2;
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL for ingest target.");
      return -3;
    }

    if (url == null) {
      logger.warn("Unable to ingest media because the ingest target URL is null.");
      return -4;
    }

    File fileDesc = new File(recording.getBaseDir(), CaptureParameters.ZIP_NAME);

    // Set the file as the body of the request
    MultipartEntity entities = new MultipartEntity();
    //Check to see if the properties have an alternate workflow definition attached
    if (recording.getProperty(CaptureParameters.INGEST_WORKFLOW_DEFINITION) != null) {
      try {
        //Add it to the normal endpoint URL
        url = new URL(url.toString() + "/" + recording.getProperty(CaptureParameters.INGEST_WORKFLOW_DEFINITION));
      } catch (MalformedURLException e1) {
        logger.warn("Malformed URL for ingest target with workflow definition.");
        return -3;
      }

      //Copy appropriate keys from the properties so they can be passed to the REST endpoint separately
      Set<Object> keys = recording.getProperties().keySet();
      for (Object o : keys) {
        String key = (String) o;
        if (key.contains("org.opencastproject.workflow.config.")) {
          try {
            String configKey = key.replaceFirst("org\\.opencastproject\\.workflow\\.config\\.", "");
            entities.addPart(configKey, new StringBody(recording.getProperty(key)));
          } catch (UnsupportedEncodingException e) {
            logger.warn("Unable to attach property {} to POST.  Exception message: {}.", key, e.getMessage());
          }
        }
      }
    }

    try {
      entities.addPart(fileDesc.getName(), new InputStreamBody(new FileInputStream(fileDesc), fileDesc.getName()));
    } catch (FileNotFoundException ex) {
      logger.error("Could not find zipped mediapackage " + fileDesc.getAbsolutePath());
      return -5;
    }

    logger.debug("Ingest URL is " + url.toString());
    HttpPost postMethod = new HttpPost(url.toString());
    int retValue = -1;

    logger.debug("Sending the file " + fileDesc.getAbsolutePath() + " with a size of "+ fileDesc.length());

    setRecordingState(recID, RecordingState.UPLOADING);

    postMethod.setEntity(entities);

    // Send the file
    HttpResponse response = null;
    try {
      response = client.execute(postMethod);
    } catch (TrustedHttpClientException e) {
      logger.error("Unable to ingest recording {}, message reads: {}.", recID, e.getMessage());
    } catch (NullPointerException e) {
      logger.error("Unable to ingest recording {}, null pointer exception!", recID);
    } finally {
      if (response != null) {
        retValue = response.getStatusLine().getStatusCode();
        client.close(response);
      } else {
        retValue = -1;
      }
    }

    if (retValue == 200) {
      setRecordingState(recID, RecordingState.UPLOAD_FINISHED);
    } else {
      setRecordingState(recID, RecordingState.UPLOAD_ERROR);
    }

    serializeRecording(recID);
    if (retValue == 200) {
      completedRecordings.put(recID, recording);
      pendingRecordings.remove(recID);
    }
    return retValue;
  }
  
  /**
   * Returns the number of captures that the capture agent is aware of that are upcoming.
   * @return The number of captures that are waiting to fire.
   */
  public int getPendingRecordingSize(){
    return pendingRecordings.size();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getAgentName()
   */
  public String getAgentName() {
    //Occasionally we're seeing a null agent name, so this fixes the problem
    if (agentName == null) {
      agentName = configService.getItem(CaptureParameters.AGENT_NAME);
    }
    return agentName;
  }

  /**
   * Sets the state of the agent.  Note that this should not change the *actual* state of the agent, only update the StateService's record of its state.
   * This is taking a string so that inter-version compatibility it maintained (eg, a version 2 agent talking to a version 1 core)
   * 
   * @param state The state of the agent.  Should be defined in AgentState.
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  protected void setAgentState(String state) {
    if (confidence) {
      if (state.equalsIgnoreCase(AgentState.CAPTURING) && confidencePipe != null) {
        confidencePipe.stop();
        //TODO:  What if this loop never finishes?
        while (confidencePipe.isPlaying());
        confidencePipe = null;
        logger.info("Confidence monitoring has been shut down.");
      } else if (state.equalsIgnoreCase(AgentState.IDLE)){
        try {
          while (configService.getAllProperties().size() == 0);
          confidencePipe = PipelineFactory.create(configService.getAllProperties(), true, this);
          Bus bus = confidencePipe.getBus();
          bus.connect(new Bus.EOS() {

            @Override
            public void endOfStream(GstObject source) {
              // TODO Auto-generated method stub

            }
          });
          confidencePipe.play();
          //TODO:  What if the pipeline crashes out?  We just run without it?  Should we add endpoints to manually restart it?
          logger.info("Confidence monitoring beginning.");
        } catch (Exception e) {
          logger.warn("Confidence monitoring not started: {}", e.getMessage());
        }
      }
    }

    /*
    // When returning to idle state reload Epiphan driver to prevent problems caused by
    // too many captures running without reloaded the driver.
    if (state.equals(AgentState.IDLE)){
      try {
        Process p = Runtime.getRuntime().exec("reload_epiphan");
        p.waitFor();
        if (p.exitValue() == 0) {
          logger.debug("VGA2USB driver successfully reloaded");
        } else {
          logger.warn("Unable to reload Epiphan VGA2USB driver.");
        }
        p.destroy();
      } catch (Exception e) {
        logger.warn("Unable to reload Epiphan VGA2USB driver.");
      }
    }
    */

    agentState = state;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#getAgentState()
   */
  public String getAgentState() {
    return agentState;
  }

  /**
   * Serializes a recording to disk
   * @param newRec The recording object.  The output object will be written to the recording's directory and will be named $recordingID.recording.
   */
  protected void serializeRecording(String id) {
    if (id == null) {
      logger.error("Unable to serialize recording, bad id parameter: null!");
      return;
    }
    try {
      RecordingImpl newRec = (RecordingImpl) pendingRecordings.get(id);
      if (newRec == null) {
        newRec = (RecordingImpl) completedRecordings.get(id);
      }
      if (newRec == null) {
        logger.error("Unable to serialize {} because it does not exist in any recording state table!", id);
      }
      File output = new File(newRec.getBaseDir(), newRec.getID() + ".recording");
      logger.debug("Serializing recording {} to {}.", newRec.getID(), output.getAbsolutePath());
      ObjectOutputStream serializer = new ObjectOutputStream(new FileOutputStream(output));
      serializer.writeObject(newRec);
      serializer.close();
    } catch (IOException e) {
      logger.error("Unable to serialize recording {}, IO exception.  Message: {}.", id, e);
    }
  }

  /**
   * Deserializes a recording from disk.
   * @param recording The directory containing the serialized recording and all of its files.
   * @return The deserialized {@code RecordingImpl}, or null in the case of an error.
   */
  protected RecordingImpl loadRecording(File recording) {
    RecordingImpl rec = null;
    try {
      logger.debug("Loading {}.", recording.getAbsolutePath());
      ObjectInputStream stream = new ObjectInputStream(new FileInputStream(recording));
      rec = (RecordingImpl) stream.readObject();
      if (context != null) {
        rec.getProperties().setBundleContext(context.getBundleContext());
      }
    } catch (FileNotFoundException e) {
      logger.error("Unable to load recording {}, file not found!", recording.getAbsolutePath());
    } catch (IOException e) {
      logger.error("IOException loading recording {}: {}.", recording.getAbsolutePath(), e);
    } catch (ClassNotFoundException e) {
      logger.error("Unable to load recording {}, file not found!", recording.getAbsolutePath());
    }
    return rec;
  }

  /**
   * Sets a pending recording's current state.
   *
   * @param recordingID The ID of the recording.
   * @param state The state for the recording.  Defined in RecordingState.
   * @see org.opencastproject.capture.admin.api.RecordingState
   */
  protected void setRecordingState(String recordingID, String state) {
    if (recordingID != null && state != null) {
      AgentRecording rec = pendingRecordings.get(recordingID);
      if (rec != null) {
        rec.setState(state);
      } else {
        XProperties p = configService.getAllProperties();
        p.put(CaptureParameters.RECORDING_ID, recordingID);
        try {
          rec = new RecordingImpl(null, p);
          rec.setState(state);
        } catch (IOException e) { /* Squash this, it's trying to create a directory for a (probably) failed capture */ }
        pendingRecordings.put(recordingID, rec);
      }
    } else if (recordingID == null) {
      logger.info("Unable to create recording because recordingID parameter was null!");
    } else if (state == null) {
      logger.info("Unable to create recording because state parameter was null!");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String recID) {
    return pendingRecordings.get(recID);
  }

  /**
   * Removes a recording from the completed recording table.
   * @param recID The ID of the recording to remove.
   */
  public void removeCompletedRecording(String recID) {
    completedRecordings.remove(recID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getKnownRecordings()
   */
  public Map<String, AgentRecording> getKnownRecordings() {
    return pendingRecordings;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#getAgentCapabilities()
   */
  public Properties getAgentCapabilities() {
    if (configService != null) {
      Properties p = configService.getCapabilities();
      if (p != null) {
        //TODO:  Do we need this?  What is it used for?  If nothing else the key should be in the capture parameters file...
        Calendar cal = Calendar.getInstance();
        p.setProperty("capture.device.timezone.offset", Integer.toString((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000)));
        p.setProperty("capture.device.timezone", TimeZone.getDefault().getID());
      } else {
        logger.warn("Returning null capabilities from capture agent...");
      }
      return p;
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#getDefaultAgentProperties()
   */
  public Properties getDefaultAgentProperties() {
    return configService.getAllProperties();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#getDefaultAgentPropertiesAsString()
   */
  @Deprecated
  public String getDefaultAgentPropertiesAsString() {
    Properties p = configService.getAllProperties();
    StringBuffer result = new StringBuffer();
    String[] lines = new String[p.size()];
    int i = 0;
    for (Object k : p.keySet()) {
      String key = (String) k;
      lines[i++] = key + "=" + p.getProperty(key) + "\n";
    }
    Arrays.sort(lines);
    for (String s : lines) {
      result.append(s);
    }

    return result.toString();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#getAgentSchedule()
   */
  public List<ScheduledEvent> getAgentSchedule() {
    if (scheduler != null) {
      return scheduler.getSchedule();
    } else {
      logger.info("Scheduler is null, so the agent cannot have a current schedule.");
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      throw new ConfigurationException("null", "Null configuration in updated!");
    }

    //Update the agent's properties from the parameter
    Properties props = new Properties();
    Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      props.put(key, properties.get(key));
    }
    //Recreate the agent state push tasks
    createPushTask(props);
    //Setup the task to load the recordings from disk once everything has started (let's be safe and use 60 seconds)
    createRecordingLoadTask(props, 60);
  }

  /**
   * Callback from the OSGi container once this service is started. This is where we register our shell commands.
   *
   * @param ctx
   *          the component context
   */
  public void activate(ComponentContext ctx) {
    logger.info("Starting CaptureAgentImpl.");
    confidence = Boolean.valueOf(configService.getItem(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));
    if (confidence)
      logger.info("Confidence monitoring enabled.");
    else
      logger.info("Confidence monitoring disabled.");



    if (ctx != null) {
      //Setup the shell commands
      Dictionary<String, Object> commands = new Hashtable<String, Object>();
      commands.put(CommandProcessor.COMMAND_SCOPE, "capture");
      commands.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "status", "start", "stop", "ingest", "reset", "capture" });
      logger.info("Registering capture agent osgi shell commands");
      ctx.getBundleContext().registerService(CaptureAgentShellCommands.class.getName(), new CaptureAgentShellCommands(this), commands);
      this.context = ctx;
    } else {
      logger.warn("Bundle context is null, so this is probably a test.  If you see this message from Felix please post a bug!");
    }

    setAgentState(AgentState.IDLE);
    if (configService != null && ctx != null) {
      configService.setItem("org.opencastproject.server.url", ctx.getBundleContext().getProperty("org.opencastproject.server.url"));
    } else if (configService == null) {
      logger.warn("Config service was null, unable to set local server url!");
    } else if (ctx == null) {
      logger.warn("Context was null, unable to set local server url!");
    }
  }

  /**
   * Shuts down the capture agent.
   */
  public void deactivate() {
    try {
      if (agentScheduler != null) {
        for (String groupname : agentScheduler.getJobGroupNames()) {
          for (String jobname : agentScheduler.getJobNames(groupname)) {
            agentScheduler.deleteJob(jobname, groupname);
          }
        }
        agentScheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      logger.warn("Finalize for scheduler did not execute cleanly: {}.", e.getMessage());
    }
  }

  /**
   * Loads all of the recordings from the capture cache dir.
   */
  public boolean loadRecordingsFromDisk() {
    if (configService == null) {
      logger.error("Unable to load recordings from disk, configuration service is null!");
      return false;
    } else if (configService.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL) == null) {
      logger.error("Unable to load recordings from disk, configuration service has null cache URL.");
      return false;
    }

    if (scheduler == null) {
      logger.error("Unable to load recordings from disk, scheduler service is null!");
      return false;
    }

    File captureRootDir = new File(configService.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    if (captureRootDir.listFiles() == null) {
      logger.debug("Unable to load recordings from disk, capture root dir is a file...");
      return false;
    }

    for (File f : captureRootDir.listFiles()) {
      if (f != null && f.isDirectory()) {
        for (File r : f.listFiles()) {
          if (r.getName().endsWith(".recording")) {
            RecordingImpl rec = loadRecording(r);
            String state = rec.getState();
            String id = rec.getID();
            logger.info("Loading recording {}...", id);
            if (pendingRecordings.containsKey(id) || completedRecordings.containsKey(id)) {
              logger.debug("Unable to load recording {} with state {}.  The recording already exists.", id, state);
              continue;
            }

            //FIXME:  This should be redone when the job refactoring ticket is finished (MH-5235).
            if (state.equals(RecordingState.CAPTURE_ERROR)) {
              logger.debug("Loaded recording {} with state {}.  This is an error state so placing recording in completed index.", id, state);
              completedRecordings.put(id, rec);
            } else if (state.equals(RecordingState.CAPTURE_FINISHED) || state.equals(RecordingState.CAPTURING)) {
              logger.debug("Loaded recording {} with state {}.  This is a completed recording so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleSerializationAndIngest(id);
            } else if (state.equals(RecordingState.MANIFEST) || state.equals(RecordingState.MANIFEST_ERROR)) {
              logger.debug("Loaded recording {} with state {}.  This is ready to ingest so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleSerializationAndIngest(id);
            } else if (state.equals(RecordingState.MANIFEST_FINISHED) || state.equals(RecordingState.COMPRESSING) || state.equals(RecordingState.COMPRESSING_ERROR)) {
              logger.debug("Loaded recording {} with state {}.  This is partially ingested so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleSerializationAndIngest(id);
            } else if (state.equals(RecordingState.UPLOAD_ERROR) || state.equals(RecordingState.UPLOADING)) {
              logger.debug("Loaded recording {} with state {}.  This is to upload so placing recording in pending index.", id, state);
              pendingRecordings.put(id, rec);
              scheduler.scheduleIngest(id);
            } else if (state.equals(RecordingState.UPLOAD_FINISHED)) {
              logger.debug("Loaded recording {} with state {}.  This is a completed state so placing recording in completed index.", id, state);
              completedRecordings.put(id, rec);
            } else if (state.equals(RecordingState.UNKNOWN)) {
              logger.debug("Loaded recording {} with state {}.  This is an unknown state, discarding.", id, state);
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * Creates the agent's scheduler
   * @param schedulerProps
   * @param jobname
   * @param jobtype
   */
  private void createScheduler(Properties schedulerProps, String jobname, String jobtype) {
    //Either create the scheduler or empty out the existing one
    try {
      if (agentScheduler != null) {
        //Clear the existing jobs and reschedule everything
        for (String name : agentScheduler.getJobNames(jobtype)) {
          if (jobname.equals(name)) {
            agentScheduler.deleteJob(name, JobParameters.RECURRING_TYPE);
          }
        }
      } else {
        StdSchedulerFactory sched_fact = new StdSchedulerFactory(schedulerProps);

        //Create and start the scheduler
        agentScheduler = sched_fact.getScheduler();
        agentScheduler.start();
      }
    } catch (SchedulerException e) {
      logger.error("Scheduler exception in State Service: {}.", e.getMessage());
      return;
    }
  }

  /**
   * Creates the Quartz task which loads the recordings from disk.
   * @param schedulerProps The properties of the scheduler, used to create the scheduler if it doesn't already exist
   * @param delay The delay before firing the job, in seconds
   */
  void createRecordingLoadTask(Properties schedulerProps, long delay) {
    createScheduler(schedulerProps, "recordingLoad", JobParameters.OTHER_TYPE);
    if (agentScheduler == null) {
      return;
    }

    JobDetail loadJob = new JobDetail("recordingLoad", JobParameters.OTHER_TYPE, LoadRecordingsJob.class);

    loadJob.getJobDataMap().put(JobParameters.CAPTURE_AGENT, this);

    //Create a new trigger                          Name              Group name               Start
    SimpleTrigger loadTrigger = new SimpleTrigger("recording_load", JobParameters.OTHER_TYPE, new Date(System.currentTimeMillis() + delay * 1000l));
    loadTrigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

    //Schedule the update
    try {
      agentScheduler.scheduleJob(loadJob, loadTrigger);
    } catch (SchedulerException e) {
      logger.warn("Unable to load recordings from disk due to scheduler error: {}.", e.getMessage());
    }
  }

  /**
   * Creates the Quartz task which pushes the agent's state to the state server.
   * @param schedulerProps The properties for the Quartz scheduler
   */
  private void createPushTask(Properties schedulerProps) {
    createScheduler(schedulerProps, "agentStateUpdate", JobParameters.RECURRING_TYPE);
    if (agentScheduler == null) {
      return;
    }

    //Setup the agent state push jobs
    try {
      long statePushTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL)) * 1000L;

      //Setup the push job
      JobDetail stateJob = new JobDetail("agentStateUpdate", JobParameters.RECURRING_TYPE, AgentStateJob.class);

      stateJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      stateJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);
      stateJob.getJobDataMap().put(JobParameters.TRUSTED_CLIENT, client);
      stateJob.getJobDataMap().put("org.opencastproject.server.url", configService.getItem("org.opencastproject.server.url"));

      //Create a new trigger                          Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger stateTrigger = new SimpleTrigger("state_push", JobParameters.RECURRING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, statePushTime);

      //Schedule the update
      agentScheduler.scheduleJob(stateJob, stateTrigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push state to remote server!", CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule state push jobs: {}.", e.getMessage());
    }

    //Setup the agent capabilities push jobs
    try {
      long capbsPushTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL)) * 1000L;

      //Setup the push job
      JobDetail capbsJob = new JobDetail("agentCapabilitiesUpdate", JobParameters.RECURRING_TYPE, AgentCapabilitiesJob.class);

      capbsJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      capbsJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);
      capbsJob.getJobDataMap().put(JobParameters.TRUSTED_CLIENT, client);

      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger capbsTrigger = new SimpleTrigger("capabilities_polling", JobParameters.RECURRING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, capbsPushTime);

      //Schedule the update
      agentScheduler.scheduleJob(capbsJob, capbsTrigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push capabilities to remote server!", CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule capability push jobs: {}.", e.getMessage());
    }
  }

  /**
   *
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.ConfidenceMonitor#grabFrame(java.lang.String)
   */
  public byte[] grabFrame(String friendlyName) {
    // get the image for the device specified
    String location = configService.getItem(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
    String device = configService.getItem(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + CaptureParameters.CAPTURE_DEVICE_DEST);
    File fimage = new File(location, device + ".jpg");
    int length = (int) fimage.length();
    byte[] ibytes = new byte[length];
    try {
      //Read the bytes and dump them to the byte array
      InputStream fis = new FileInputStream(fimage);
      fis.read(ibytes, 0, length);
      fis.close();
      return ibytes;
    } catch (FileNotFoundException e) {
      logger.error("Could not read confidence image from: {}", device);
    } catch (IOException e) {
      logger.error("Confidence read error: {}", e.getMessage());
    }
    return null;
  }

  /**
   *
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.ConfidenceMonitor#getFriendlyNames()
   */
  public LinkedList<String> getFriendlyNames() {
    String devices = configService.getItem(CaptureParameters.CAPTURE_DEVICE_NAMES);
    String[] friendlyNames = devices.split(",");
    LinkedList<String> deviceList = new LinkedList<String>();
    //For each device name in the split list above
    for (String name : friendlyNames) {
      String srcName = configService.getItem(CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_SOURCE);
      //Determine the type and add it to the appropriate list
      if (srcName.contains("hw:")) {
        deviceList.add(name + "," + CAPTURE_TYPE_AUDIO);
      } else {
        deviceList.add(name + "," + CAPTURE_TYPE_VIDEO);
      }
    }
    return deviceList;
  }

  /**
   *
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.ConfidenceMonitor#getRMSValues(java.lang.String, double)
   */
  public List<Double> getRMSValues(String friendlyName, double timestamp) {
    return AudioMonitoring.getRMSValues(friendlyName, timestamp);
  }

  /**
   *
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.ConfidenceMonitor#getCoreUrl()
   */
  public String getCoreUrl() {
    return configService.getItem(CaptureParameters.CAPTURE_CORE_URL);
  }
  
  /**
    * Determines if the current agent state is idle.
    * @return returns true if the capture agent is idle.
    */
  public boolean isIdle(){
    if(getAgentState() == null){
        return false;
    }
    else{
      return getAgentState().equals(AgentState.IDLE);  
    }
  }
}
