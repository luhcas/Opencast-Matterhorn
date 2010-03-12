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
  private String id = null;
 
  /** Keeps the properties associated with this recording */
  private Properties props = null;

  /** The MediaPackage containing all the metadata/attachments/any file related with this recording */
  private MediaPackage mPkg = null;
  
  // FIXME: Why do we need fields for both the manifest and the mediapackage?  Right now, we have two (possibly out-of-sync)
  // versions of the same information.
  /** The manifest associated with this recording */
  private File manifest = null;
  
  /** The name assigned to the metadata file created and attached by the capture agent */
  // TODO: Necessary?
  // TODO: At least, a method for overriding the default name should be provided. 
  //private String AGENT_CATALOG_NAME = "agent_metadata.xml";

  /** 
   * Constructs a RecordingImpl object using the Properties and MediaPackage provided
   * @param props The {@code Properties} object associated to this recording
   * @param mp    The {@code MediaPackage} with this recording files
   * @throws IOException If the base directory could not be fetched
   */
  public RecordingImpl(MediaPackage mp, Properties properties) throws IOException, IllegalArgumentException {
    // Stores the MediaPackage
    this.mPkg = mp;
    this.props = (Properties)properties.clone();
    
    // FIXME: It would make sense to make this if ()...else () its own method. It would certainly ease testing. (jt)
    //Figures out where captureDir lives
    if (this.props.containsKey(CaptureParameters.RECORDING_ROOT_URL)) {
      baseDir = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL));
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        id = props.getProperty(CaptureParameters.RECORDING_ID);
      } else {
        //In this case they've set the root URL, but not the recording ID.  Get the id from that url instead then.
        logger.debug("{} was set, but not {}.", CaptureParameters.RECORDING_ROOT_URL, CaptureParameters.RECORDING_ID);
        id = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL)).getName();
        props.put(CaptureParameters.RECORDING_ID, id);
      }
    } else {
      //If there is a recording ID use it, otherwise it's unscheduled so just grab a timestamp
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        id = props.getProperty(CaptureParameters.RECORDING_ID);
        baseDir = new File(props.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), id);
      } else {
        //Unscheduled capture, use a timestamp value instead
        id = "Unscheduled-" + System.currentTimeMillis();
        props.setProperty(CaptureParameters.RECORDING_ID, id);
        baseDir = new File(props.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), id);
      }
      props.put(CaptureParameters.RECORDING_ROOT_URL, baseDir.getAbsolutePath());
    }
    
    // Checks that recordingID is not null (can't be, otherwise we won't be able to identify the recording
    if (id == null) {
      logger.error("Couldn't get a proper recordingID from Properties");
      throw new IllegalArgumentException("Couldn't get a proper recordingID from Properties");
    }
    //Setup the root capture dir, also make sure that it exists.
    if (!baseDir.exists()) {
      try {
        FileUtils.forceMkdir(baseDir);
      } catch (IOException e) {
        logger.error("IOException creating required directory {}.", baseDir.toString());
        //setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        throw e;
      }
      //Should have been created.  Let's make sure of that.
      if (!baseDir.exists()) {
        logger.error("Unable to start capture, could not create required directory {}.", baseDir.toString());
        //setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        throw new IOException ("Unable to create base directory");
      }
    }
    
    
  }
  
  /**
   * @return The {@code Properties} object associated with the recording
   */
  public Properties getProperties() {
    return props;
  }

  /**
   * @param props A {@code Properties} object to associate to the recording
   */
  public void setProps(Properties props) {
    this.props = props;
  }

  /**
   * @return The current MediaPackage
   */
  public MediaPackage getMediaPackage() {
    return mPkg;
  }

  /**
   * @param mPkg the MediaPackage to set
   */
  // TODO: As one can get a copy of the local MediaPackage and modify it outside, this method may not be necessary
  
  public void setMediaPackage(MediaPackage mPkg) {
    this.mPkg = mPkg;
  }
  
  /**
   * @return The ID for this recording
   */
  public String getID() {
    return id;
  }
  
  /**
   * @return A {@code File} object pointing to the directory where this recording files are
   */
  public File getDir() {
    return baseDir;
  }

  
  /**
   * Gets a property from the local {@code Properties} object
   * @param key The property name
   * @return The property value, or {@code null} if it doesn't exist
   * @see java.util.Properties#getProperty(String)
   */
  public String getProperty(String key) {
    return props.getProperty(key);
  }
  
  /**
   * Sets a property in the local {@code Properties} object (by simply calling its own setProperty method)
   * @param  key The property name
   * @param  value The value to be set
   * @return The previous value of the specified key in this property list, or null if it did not have one.
   * @see java.util.Properties#setProperty(String, String)
   */
  public String setProperty(String key, String value) {
    return (String)props.setProperty(key, value);
  }
  
  // FIXME: Remove this method (see comment at the very top). The recording should hold a reference to the media
  // package only.
  /**
   * Gets the manifest for this recording
   * @return A {@code File} object with the path of the manifest, or {@code null} if it has not been yet created
   */
  public File getManifest() {
    return manifest;
  }
  
  // FIXME: Remove this method (see comment at the very top). The recording should hold a reference to the media
  // package only.
  /**
   * Sets the manifest for this recording
   * @param manifest
   * @return A {@code boolean} indicating success or failure
   */
  public boolean setManifest(File manifest) {
    if (manifest != null && manifest.isFile()) {
      this.manifest = manifest;
      return true;
    }
    
    return false;
  }
  
  /*
  /**
   * Gets the name assigned to the capture metadata catalog
   * @return A {@code String} with the name
   */
  /*public String getAgentCatalogName() {
    return AGENT_CATALOG_NAME;
  }*/
}
