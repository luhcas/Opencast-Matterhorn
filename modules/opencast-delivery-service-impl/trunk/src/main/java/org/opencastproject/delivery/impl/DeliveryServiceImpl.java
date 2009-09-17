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
package org.opencastproject.delivery.impl;

import org.opencastproject.delivery.api.DeliveryService;

import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * FIXME -- Add javadocs
 */
public class DeliveryServiceImpl implements DeliveryService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(DeliveryServiceImpl.class);

  private String rootDirectory = System.getProperty("java.io.tmpdir");

  
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  public InputStream download(String path) {
    File f = new File(rootDirectory + File.separator + path);
    logger.info("Attempting to read file " + f.getAbsolutePath());
    try {
      return new FileInputStream(f);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public void upload(InputStream stream, String path)
  {
    File f = new File (rootDirectory + File.separator + path);
    
    try {
      FileOutputStream fos = new FileOutputStream(f);
      IOUtils.copy(stream, fos);
      IOUtils.closeQuietly(stream);
      IOUtils.closeQuietly(fos);
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
