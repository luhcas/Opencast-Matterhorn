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

import org.opencastproject.media.mediapackage.MediaPackage;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * This class is a container for the properties relating a certain recording -- 
 * a set of Properties and a MediaPackage with all the metadata/attachments/etc. associated 
 */
public class RecordingImpl {

  private static final Logger logger = LoggerFactory.getLogger(RecordingImpl.class);
  
  /** Directory in the filesystem where the files related with this recording are */
  private File baseDir = null;
 
  /** Unique identifier for this ID   */
  private String recordingID = null;
 
  /** Keeps the properties associated with this recording */
  private Properties props = null;
  
  /** The MediaPackage containing all the metadata/attachments/any file related with this recording */
  private MediaPackage mPkg = null;
  
  /** The manifest associated with this recording */
  private File manifest = null;
  
  /** The name that is assigned to the zip file that is ingested. Defaults to "media.zip" */
  // TODO: Is it correct to have a default hard-coded name?
  // At least, a method for overriding the default name should be provided. 
  private String ZIP_NAME = "media.zip";
  
  /** The name assigned to the manifest file. Defaults to "manifest.xml" */
  // TODO: At least, a method for overriding the default name should be provided. 
  private String MANIFEST_NAME = "manifest.xml";
  
  /** The name assigned to the metadata file created and attached by the capture agent */
  // TODO: At least, a method for overriding the default name should be provided. 
  private String AGENT_CATALOG_NAME = "agent_metadata.xml";

  /** 
   * Constructs a RecordingImpl object using the Properties and MediaPackage provided
   * @param props
   * @param mp
   */
  RecordingImpl(MediaPackage mp, Properties properties) {
    // Stores the MediaPackage
    this.mPkg = mp;

    ConfigurationManager config = ConfigurationManager.getInstance();

    // Merges properties without overwriting the system's configuration
    props = config.merge(properties, false);
    
    //Figures out where captureDir lives
    if (this.props.containsKey(CaptureParameters.RECORDING_ROOT_URL)) {
      baseDir = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL));
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        recordingID = props.getProperty(CaptureParameters.RECORDING_ID);
      } else {
        //In this case they've set the root URL, but not the recording ID.  Get the id from that url instead then.
        logger.warn("{} was set, but not {}.", CaptureParameters.RECORDING_ROOT_URL, CaptureParameters.RECORDING_ID);
        recordingID = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL)).getName();
        props.put(CaptureParameters.RECORDING_ID, recordingID);
      }
    } else {
      //If there is a recording ID use it, otherwise it's unscheduled so just grab a timestamp
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        recordingID = props.getProperty(CaptureParameters.RECORDING_ID);
        baseDir = new File(props.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), recordingID);
      } else {
        //Unscheduled capture, use a timestamp value instead
        recordingID = "Unscheduled-" + System.currentTimeMillis();
        props.setProperty(CaptureParameters.RECORDING_ID, recordingID);
        baseDir = new File(props.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), recordingID);
      }
      props.put(CaptureParameters.RECORDING_ROOT_URL, baseDir.getAbsolutePath());
    }
    
    //Setup the root capture dir, also make sure that it exists.
    if (!baseDir.exists()) {
      try {
        FileUtils.forceMkdir(baseDir);
      } catch (IOException e) {
        logger.error("IOException creating required directory {}.", baseDir.toString());
        //setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
      }
      //Should have been created.  Let's make sure of that.
      if (!baseDir.exists()) {
        logger.error("Unable to start capture, could not create required directory {}.", baseDir.toString());
        //setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
      }
    }

  }
  
  /**
   * @return the props
   */
  public Properties getProperties() {
    return props;
  }

  /**
   * @param props the props to set
   */
  public void setProps(Properties props) {
    this.props = props;
  }

  /**
   * @return the MediaPackage
   */
  public MediaPackage getMediaPackage() {
    return mPkg;
  }

  /**
   * @param mPkg the MediaPackage to set
   */
  public void setMediaPackage(MediaPackage mPkg) {
    this.mPkg = mPkg;
  }
  
  /**
   * @return the recordingID
   */
  public String getRecordingID() {
    return recordingID;
  }
  
  /**
   * @return the baseDir
   */
  public File getDir() {
    return baseDir;
  }

  
  /**
   * 
   * @param key The property name
   * @return The property value, or null if it doesn't exist
   */
  public String getProperty(String key) {
    return props.getProperty(key);
  }
  
  public String setProperty(String key, String value) {
    return (String)props.setProperty(key, value);
  }
  
  /**
   * Returns the manifest associated to this recording
   * @return A File object with the path of the manifest, or null if it has not been yet created
   */
  public File getManifest() {
    return manifest;
  }
  
  /**
   * Sets the manifest for this recording
   * @param manifest
   * @return A boolean indicating success or failure
   */
  public boolean setManifest(File manifest) {
    if (manifest != null && manifest.exists()) {
      this.manifest = manifest;
      return true;
    }
    
    return false;
  }
  
  /**
   * Gets the name assigned to the Zip File ingested
   * @return A String with the name
   */
  public String getZipName() {
   return ZIP_NAME; 
  }
  
  /**
   * Gets the name assigned to the manifest file.
   * @return A String with the name
   */
  public String getManifestName() {
    return MANIFEST_NAME;
  }
  
  public String getAgentCatalogName() {
    return AGENT_CATALOG_NAME;
  }
}
