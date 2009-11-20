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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
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
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.util.Compressor;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


// TODO: Eliminate dependency with MediaInspector in pom.xml, if it is not finally used

/**
 * Implementation of the Capture Agent: using gstreamer, generates several Pipelines
 * to store several tracks from a certain recording.
 */
public class CaptureAgentImpl implements CaptureAgent, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);

  private Pipeline pipe = null;
  private Properties props = null;
  private ConfigurationManager config = ConfigurationManager.getInstance();
  
  public static final String tmpPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "captures";
  private final File tmpDir = new File(tmpPath);
  private final File manifest = new File(tmpDir.getAbsolutePath()+File.separator+"manifest.xml");

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

    props = properties;

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
    
    // Does the manifest
    try {
      doManifest();
    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: "+e.getMessage());
      result = "MediaPackage Exception: "+e.getMessage();
    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: "+e.getMessage());
      result = "Unsupported Element Exception: "+e.getMessage();
    } catch (IOException e) {
      logger.error("I/O Exception: "+e.getMessage());
      result = "I/O Exception: "+e.getMessage();
    } catch (TransformerException e) {
      logger.error("Transformer Exception: "+e.getMessage());
      result = "Transformer Exception: "+e.getMessage();
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
   * Pushes the recording's state to the admin server
   * @param id The id of the lecture
   * @param state The state of the lecture
   */
  private void logRecordingState(String id, String state) {
    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.RECORDING_STATUS_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for " + CaptureParameters.RECORDING_STATUS_ENDPOINT_URL + " is invalid, unable to push recording state to remote server");
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
   * Generates the manifest.xml file from the files specified in the properties
   * @return A String indicating the success or fail of the operation
   * @throws MediaPackageException 
   * @throws UnsupportedElementException 
   * @throws IOException 
   * @throws TransformerException 
   */
  private void doManifest() throws MediaPackageException, UnsupportedElementException, IOException, TransformerException {
    
    // Generates the manifest
    try {
      MediaPackage pkg = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

      Enumeration<Object> keys = props.keys();
      // Inserts the tracks in the MediaPackage
      while (keys.hasMoreElements()) {
        File item = new File(props.getProperty((String)keys.nextElement()));
        if (item.exists())
            // Adds a track
            pkg.add(TrackImpl.fromURL(item.toURI().toURL()));
      }

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
      logger.error("MediaPackage Exception: "+e.getMessage());
      throw e;
    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: "+e.getMessage());
      throw e;
    } catch (TransformerException e) {
      logger.error("Transformer Exception: "+e.getMessage());
      throw e;
    } catch (IOException e) {
      logger.error("I/O Exception: "+e.getMessage());
      throw e;
    }

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

      HttpClient client = new DefaultHttpClient();
      HttpPost postMethod = new HttpPost(url);
      String retValue = null;

      logRecordingState("?", RecordingState.UPLOADING);
      try {
        // Set the file as the body of the request
        postMethod.setEntity(new FileEntity(fileDesc, URLConnection.getFileNameMap().getContentTypeFor(fileDesc.getName())));

        // Send the file
        HttpResponse response = client.execute(postMethod);

        retValue = response.getStatusLine().getReasonPhrase();

        //TODO:  Get valid id for this recording
        logRecordingState("?", RecordingState.UPLOAD_FINISHED);
      } catch (ClientProtocolException e) {
        logger.error("doIngest: Failed to submit the data. "+ e.getMessage());
        //TODO:  Get valid id for this recording
        logRecordingState("?", RecordingState.UPLOAD_ERROR);
      } catch (IOException e) {
        logger.error("doIngest: I/O Exception. " + e.getMessage());
        //TODO:  Get valid id for this recording
        logRecordingState("?", RecordingState.UPLOAD_ERROR);
      } finally {
        client.getConnectionManager().shutdown();
      }

      return retValue;
    }

  }
