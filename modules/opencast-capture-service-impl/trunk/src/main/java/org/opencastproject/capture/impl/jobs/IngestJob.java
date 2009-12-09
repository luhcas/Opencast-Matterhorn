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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.util.ZipUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.StatefulJob;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Date;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Creates the manifest, then attempts to ingest the media to the remote server
 */
public class IngestJob implements StatefulJob {

  private static final Logger logger = LoggerFactory.getLogger(IngestJob.class);

  /** The constant used to define where the ConfigurationManager lives within the JobExecutionContext */
  private static final String CONFIGURATION = "configuration";
  private static final String SERVICE = "state_service";

  /** The manifest and zip file standard names */
  // TODO Can these names be a configurable property?
  private static final String MANIFEST_NAME = "manifest.xml";
  private static final String ZIP_NAME = "media.zip";

  /** The configuration for this capture */
  private Properties config = null;
  /** The service where the states of the captures are kept */
  private StateService service = null;
  /** The ID of the recording we're dealing with */
  private String recordingID = null;
  /** The directory we're dealing with when trying to ingest */
  private File currentCaptureDir = null;
  /** A File abstract path to the zip file generated */
  private File theZip = null;

  public static void scheduleJob(Properties props, StateService state_service) throws IOException, SchedulerException {

    long retry = Long.parseLong(props.getProperty(CaptureParameters.INGEST_RETRY_INTERVAL)) * 1000L;
    String id = props.getProperty(CaptureParameters.RECORDING_ID);

    Properties retryProperties = new Properties();
    retryProperties.load(IngestJob.class.getClassLoader().getResourceAsStream("config/ingest_scheduler.properties"));
    StdSchedulerFactory sched_fact = new StdSchedulerFactory(retryProperties);

    //Create and start the scheduler
    Scheduler retryScheduler = sched_fact.getScheduler();
    if (!retryScheduler.isStarted()) {
      retryScheduler.start();
    }

    //Setup the polling
    JobDetail job = new JobDetail(id, Scheduler.DEFAULT_GROUP, IngestJob.class);
    job.getJobDataMap().put(CONFIGURATION, props);
    job.getJobDataMap().put(SERVICE, state_service);

    //Create a new trigger                    Name         Group name               Start       End   # of times to repeat, Repeat interval
    SimpleTrigger trigger = new SimpleTrigger(id, Scheduler.DEFAULT_GROUP, new Date(), null, 1, retry);
    //If the trigger misfires (ie, the ingest didn't work) then retry
    trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);

    //Schedule the update
    retryScheduler.scheduleJob(job, trigger);
  }

  /**
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    config = (Properties) ctx.getMergedJobDataMap().get(CONFIGURATION);
    service = (StateService) ctx.getMergedJobDataMap().get(SERVICE);

    if (config.getProperty(CaptureParameters.RECORDING_ID) == null || config.getProperty(CaptureParameters.RECORDING_ROOT_URL) == null) {
      logger.error("Invalid capture configuration, unable to process ingest!");
      return;
    }
    recordingID = config.getProperty(CaptureParameters.RECORDING_ID);
    currentCaptureDir = new File(config.getProperty(CaptureParameters.RECORDING_ROOT_URL));
    String ingestURL = config.getProperty(CaptureParameters.INGEST_ENDPOINT_URL);

    logger.info("Beginning ingest procedure for {}.", recordingID);

    //TODO:  We currently lack multi-state support (both uploading and capturing, for example).  We need to add this at the appropriate places when it comes online.

    // Does the manifest
    if (doManifest()) {
      service.setRecordingState(recordingID, RecordingState.CAPTURE_FINISHED);
      logger.info("Ingest {}: manifest created succesfully", recordingID);
    } else {
      logger.error("Ingest {}: manifest not created", recordingID);
      service.setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
      throw new JobExecutionException();
    }

    // Zips files                                                                                                                                        
    theZip = zipFiles(ZIP_NAME);
    
    if (theZip == null || !theZip.exists()) {
      logger.error("Ingest {}: zip file not generated correctly", recordingID);
      throw new JobExecutionException();
    }

    // Ingests the zip file                                                                                                                              
    int ingestValue = doIngest(ingestURL, theZip);

    if (ingestValue != 200) {
      logger.error("Ingest {} failed with a value of: {}", recordingID, ingestValue);
    } else
      logger.info("Ingestion {} finished!", recordingID);
    
    // TODO: Remove the capture from the local status service
  }

  /**
   * Generates the manifest.xml file from the files specified in the properties
   * @return {@code true} if the manifest is succesfully created. False otherwise.
   */
  public boolean doManifest() {
    logger.debug("Generating manifest.");
    MediaPackageElementFlavor flavor = null;

    // Generates the manifest
    try {
      MediaPackage pkg = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
      MediaPackageElementBuilder elemBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();

      // Adds the files present in the Properties                                                                                                      
      String[] friendlyNames = config.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES).split(",");
      for (String name : friendlyNames) {
        name = name.trim();

        if (name == "")
          continue;
        
        // TODO: This should be modified to allow a more flexible way of detecting the track flavour.
        // Suggestions: a dedicated class or a/several field(s) in the properties indicating what type of track is each
        if (name.equals("PRESENTER") || name.equals("AUDIO"))
          flavor = MediaPackageElements.PRESENTER_TRACK;
        else if (name.equals("SCREEN"))
          flavor = MediaPackageElements.PRESENTATION_TRACK;

        String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_DEST;
        File outputFile = new File(currentCaptureDir, config.getProperty(outputProperty));

        // Adds the file to the MediaPackage                                                                                                         
        if (outputFile.exists())
          pkg.add(elemBuilder.elementFromURI(new URI(outputFile.getName()),
                  MediaPackageElement.Type.Track,
                  flavor));
        else
          logger.warn ("Required file {} not found", outputFile.getName());
      }

      // Adds the rest of the files (in case some attachment was left there by the scheduler)                                                          
      File[] files = currentCaptureDir.listFiles();
      for (File item : files)
        // Discards the "capture.stopped" file and the files in the properties --they have already been processed                                    
        // Also checks the file exists                                                                                                               
        if (item.exists() && (!config.contains(item.getName().trim())) && (!item.getName().equals("capture.stopped")))
          pkg.add(new URI(item.getName()));

      // TODO insert a catalog with some capture metadata

      // Gets the manifest.xml as a Document object
      Document doc = pkg.toXml();

      // Defines a transformer to convert the object in a xml file
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      // Initializes StreamResult with File object to save to file
      StreamResult stResult = new StreamResult(new FileOutputStream(new File(currentCaptureDir, MANIFEST_NAME)));
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, stResult);

      // Closes the stream to make sure all the content is written to the file
      stResult.getOutputStream().close();
      
    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: {}.", e.getMessage());
      return false;
    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: {}.", e.getMessage());
      return false;
    } catch (TransformerException e) {
      logger.error("Transformer Exception: {}.", e.getMessage());
      return false;
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e.getMessage());
      return false;
    } catch (URISyntaxException e) {
      logger.error("URI Exception: {}", e.getMessage());
      return false;
    }

    return true;
  }

  /**
   * Compresses the files contained in the output directory
   * @param zipName - The name of the zip file created
   * @return A File reference to the file zip created
   */
  public File zipFiles(String zipName) {
    File[] totalFiles = currentCaptureDir.listFiles();
    File[] filesToZip = new File[totalFiles.length - 1];
    int i = 0;

    // Lists all the files in the capture directory and zips all of them but 'capture.stopped' and the .zip itself
    for (File item : totalFiles) {
      String fileName = item.getName().trim();
      if (!(fileName.equals("capture.stopped") &&
              (!item.getName().equals(zipName))))
        filesToZip[i++] = item;
    }

    return ZipUtil.zip(filesToZip, new File(currentCaptureDir, zipName).getAbsolutePath());
  }

  /**
   * Sends a file to the REST ingestion service
   * @param url : The service URL
   * @param fileDesc : The descriptor for the zipped media
   */
  public int doIngest(String url, File fileDesc) {

    logger.info("Beginning ingest of recording.");

    HttpClient client = new DefaultHttpClient();
    HttpPost postMethod = new HttpPost(url);
    int retValue = 0;

    //TODO:  We currently lack multi-state support (both uploading and capturing, for example).  We need to add this at the appropriate places when it comes online.
    //setAgentState(AgentState.UPLOADING);
    service.setRecordingState(recordingID, RecordingState.UPLOADING);

    try {
      // Set the file as the body of the request
      postMethod.setEntity(new FileEntity(fileDesc, URLConnection.getFileNameMap().getContentTypeFor(fileDesc.getName())));

      // Send the file
      HttpResponse response = client.execute(postMethod);

      retValue = response.getStatusLine().getStatusCode();

      service.setRecordingState(recordingID, RecordingState.UPLOAD_FINISHED);
    } catch (ClientProtocolException e) {
      logger.error("Failed to submit the data: {}.", e.getMessage());
      service.setRecordingState(recordingID, RecordingState.UPLOAD_ERROR);
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e.getMessage());
      service.setRecordingState(recordingID, RecordingState.UPLOAD_ERROR);
    } finally {
      client.getConnectionManager().shutdown();
      //setAgentState(AgentState.IDLE);
    }

    return retValue;
  }
}
