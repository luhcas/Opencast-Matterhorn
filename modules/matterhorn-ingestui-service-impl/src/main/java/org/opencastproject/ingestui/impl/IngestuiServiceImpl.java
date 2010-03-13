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
package org.opencastproject.ingestui.impl;

import org.apache.commons.io.FileUtils;
import org.opencastproject.ingestui.api.IngestuiService;
import org.opencastproject.ingestui.api.Metadata;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Dictionary;
import java.lang.NullPointerException;

/**
 * Implementation of the IngetuiService interface. The class can save metadata for a file and retreive it.
 * 
 * @author bwulff@uos.de
 */
public class IngestuiServiceImpl implements IngestuiService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(IngestuiServiceImpl.class);

  private static String metadataPath = "/tmp/matterhorn/metadata";

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * Saves the metadata for a file as a file named [filename].metadata
   * 
   * @param filename
   * @param data
   */
  public void acceptMetadata(String filename, Metadata data) {
    checkFilename(filename);

    try {
      File datafile = getFile(filename);
      if (!datafile.exists()) {
        logger.info("Creating file " + datafile.getAbsolutePath());
        datafile.createNewFile();
      }
      ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(datafile)));
      logger.info("Attempting to write metadata to " + datafile.getAbsolutePath());
      out.writeObject(data);
      out.flush();
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the metadata object for a file or throws a NullPointerException if no metadata for the filename was found.
   * 
   * @param filename
   */
  public Metadata getMetadata(String filename) {
    checkFilename(filename);

    try {
      File datafile = getFile(filename);
      if (!datafile.exists())
        throw new NullPointerException("No metadata file found for " + filename);
      ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(datafile)));
      logger.info("Attempting to read metadata from " + datafile.getAbsolutePath());
      Metadata data = (Metadata) in.readObject();
      return data;
    } catch (Exception e) {
      logger.error("Not able to read metadata for " + filename);
      throw new RuntimeException(e);
    }
  }

  /**
   * returns a new File object representing a metadata file.
   * 
   * @param filename
   * @return
   */
  private File getFile(String filename) {
    return new File(metadataPath + File.separator + filename + ".metadata");
  }

  /**
   * Checks if the metadata directory exists and creates it if not. Primary purpose is the check if the filename is
   * valid. TODO check if filename is secure!
   * 
   * @param filename
   */
  private void checkFilename(String filename) {
    try {
      File metadataDir = new File(metadataPath);
      if (!metadataDir.exists()) {
        logger.info("Creating directory " + metadataPath);
        FileUtils.forceMkdir(metadataDir);
      }
    } catch (Exception e) {
      logger.error("Not able to create directory " + metadataPath);
      throw new RuntimeException(e);
    }
    if (filename == null)
      throw new NullPointerException("Filename is NULL.");
    // FIXME test if filename is secure
  }
}
