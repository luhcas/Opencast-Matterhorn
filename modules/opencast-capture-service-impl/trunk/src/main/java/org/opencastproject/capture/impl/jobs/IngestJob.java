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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.opencastproject.capture.api.RecordingState;
import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.capture.impl.StateSingleton;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.util.Compressor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Creates the manifest, then attempts to ingest the media to the remote server
 */
public class IngestJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(IngestJob.class);

  /** The constant used to define where the ConfigurationManager lives within the JobExecutionContext */
  public static final String CONFIGURATION = "configuration";
  /** The configuration for this capture */
  private Properties config = null;
  /** The singleton where the states of the captures are kept */
  private StateSingleton singleton = StateSingleton.getInstance();
  /** The ID of the recording we're dealing with */
  private String recordingID = null;
  /** The directory we're dealing with when trying to ingest */
  private File current_capture_dir = null;

  /**
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    //get the config out of the context
    config = (Properties) ctx.getMergedJobDataMap().get(CONFIGURATION);
    if (config == null) {
      logger.error("Capture configuration was missing, cannot continue!");
      return;
    }

    if (config.getProperty(CaptureParameters.RECORDING_ID) == null || config.getProperty(CaptureParameters.RECORDING_ROOT_URL) == null) {
      logger.error("Invalid capture configuration, unable to process ingest!");
      return;
    }
    recordingID = config.getProperty(CaptureParameters.RECORDING_ID);
    current_capture_dir = new File(config.getProperty(CaptureParameters.RECORDING_ROOT_URL));
    logger.info("Beginning ingest procedure for {}.", recordingID);

    //TODO:  We currently lack multi-state support (both uploading and capturing, for example).  We need to add this at the appropriate places when it comes online.

    // Does the manifest
    try {
      doManifest();
      singleton.setRecordingState(recordingID, RecordingState.CAPTURE_FINISHED);
    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: {}.", e.getMessage());
      singleton.setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: {}.", e.getMessage());
      singleton.setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e.getMessage());
      e.printStackTrace();
      singleton.setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
    } catch (TransformerException e) {
      logger.error("Transformer Exception: {}.", e.getMessage());
      singleton.setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
    } finally {
      //setAgentState(AgentState.IDLE);
    }
  }

  /**
   * Generates the manifest.xml file from the files specified in the properties
   * @return A String indicating the success or fail of the operation
   * @throws MediaPackageException
   * @throws UnsupportedElementException
   * @throws IOException
   * @throws TransformerException
   */
  public boolean doManifest() throws MediaPackageException, UnsupportedElementException, IOException, TransformerException {
    logger.debug("Generating manifest.");

    // Generates the manifest
    try {
      MediaPackage pkg = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

      // Inserts the tracks in the MediaPackage
      // TODO Specify the flavour
      String deviceNames = config.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
      if (deviceNames == null) {
        logger.error("No capture devices specified in " + CaptureParameters.CAPTURE_DEVICE_NAMES);
        return false;
      }

      String[] friendlyNames = deviceNames.split(",");
      String outputDirectory = config.getProperty(CaptureParameters.RECORDING_ROOT_URL);
      for (String name : friendlyNames) {
        name = name.trim();

        // Disregard the empty string
        if (name.equals(""))
          continue;

        String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_DEST;
        File outputFile = new File(outputDirectory, config.getProperty(outputProperty));

        // add the file to the MediaPackage
        if (outputFile.exists()) {
          pkg.add(outputFile.toURI().toURL());
        }

      }

      // TODO insert a catalog with some capture metadata

      // Gets the manifest.xml as a Document object
      Document doc = pkg.toXml();

      // Defines a transformer to convert the object in a xml file
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      // Initializes StreamResult with File object to save to file
      StreamResult stResult = new StreamResult(new FileOutputStream(new File(current_capture_dir, "manifest.xml")));
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
    String[] totalFiles = current_capture_dir.list();
    String[] filesToZip = new String[totalFiles.length - 1];
    int i = 0;

    for (String item : totalFiles)
      if (!(item.equals("capture.stopped") ||
              item.substring(item.lastIndexOf('.')).trim().equals("zip")))
        filesToZip[i++] = new File(current_capture_dir, item).getAbsolutePath();

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

    //TODO:  We currently lack multi-state support (both uploading and capturing, for example).  We need to add this at the appropriate places when it comes online.
    //setAgentState(AgentState.UPLOADING);
    singleton.setRecordingState(recordingID, RecordingState.UPLOADING);

    try {
      // Set the file as the body of the request
      postMethod.setEntity(new FileEntity(fileDesc, URLConnection.getFileNameMap().getContentTypeFor(fileDesc.getName())));

      // Send the file
      HttpResponse response = client.execute(postMethod);

      retValue = response.getStatusLine().getReasonPhrase();

      singleton.setRecordingState(recordingID, RecordingState.UPLOAD_FINISHED);
    } catch (ClientProtocolException e) {
      logger.error("Failed to submit the data: {}.", e.getMessage());
      singleton.setRecordingState(recordingID, RecordingState.UPLOAD_ERROR);
    } catch (IOException e) {
      logger.error("I/O Exception: {}.", e.getMessage());
      singleton.setRecordingState(recordingID, RecordingState.UPLOAD_ERROR);
    } finally {
      client.getConnectionManager().shutdown();
      //setAgentState(AgentState.IDLE);
    }

    return retValue;
  }
}
