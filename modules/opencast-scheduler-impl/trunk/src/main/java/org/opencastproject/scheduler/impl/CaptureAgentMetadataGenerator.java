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

package org.opencastproject.scheduler.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Generate a properties list with key in the capture agents namespace from SchedulerEvent Metadata
 *
 */
public class CaptureAgentMetadataGenerator {

  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentMetadataGenerator.class);
  
  MetadataMapper mapper;
  
  /**
   * Constructor needs the reference to the metadata mapping information.
   * @param caMappingFile Mapping File with Capture Agent specific mapping
   * @throws FileNotFoundException
   * @throws IOException
   */
  public CaptureAgentMetadataGenerator (InputStream caMappingFile) throws FileNotFoundException, IOException {
    logger.info("Initialising Capture Agent Metadata Generator");
    mapper = new MetadataMapper(caMappingFile);  
  }
  
  /**
   * Generates a Properties list with the Capture Agent metadata from the provided event 
   * @param event The SchedulerEvent from which the metadata should be generated as Capture Agent specific data 
   * @return A  Properties List with Capture Agent specific Data
   */
  public Properties generate (SchedulerEvent event) {
    logger.info("generating Capture Agent metadata");
    
    Hashtable<String, String> caMetadata =  mapper.convert(event.getMetadata());
    
    Properties caCatalog = new Properties();
    
    String [] res = event.getResources();
    String resList = "";
    for (int i = 0; i < res.length; i++) {
      if (i > 0) resList += ","; //skip "," in front of first value
      resList +=res[i];
    }
    caCatalog.setProperty("capture.devices.names", resList);
    
    Enumeration<String> keys = caMetadata.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      caCatalog.put (key, caMetadata.get(key));
    }       
    return caCatalog;
  }
  
  /**
   * Generates a Properties list with the Capture Agent metadata from the provided event 
   * @param event The SchedulerEvent from which the metadata should be generated as Capture Agent specific data 
   * @return A String with a Properties List with Capture Agent specific Data
   */
  public String generateAsString (SchedulerEvent event) {
    StringWriter writer = new StringWriter();
    try {
      generate(event).store(writer, "Capture Agent specific data");
      return writer.getBuffer().toString();
    } catch (IOException e) {
      logger.error("Could not convert Capture Agent Data to String");
    }
    return null;
  }
  
  /**
   * Generates a Properties list with the Capture Agent metadata from the provided event and encodes it with Base64 
   * @param event The SchedulerEvent from which the metadata should be generated as Capture Agent specific data 
   * @return A String with a Base64 encoded Properties List with Capture Agent specific Data
   */
  public byte [] generateAsBase64 (SchedulerEvent event) {
    return Base64.encodeBase64(generateAsString(event).getBytes());
  }
}
