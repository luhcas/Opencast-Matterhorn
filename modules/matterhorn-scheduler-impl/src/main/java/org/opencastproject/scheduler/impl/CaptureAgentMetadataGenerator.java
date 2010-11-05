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

package org.opencastproject.scheduler.impl;

import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Map.Entry;

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
    logger.debug("Initialising Capture Agent Metadata Generator");
    mapper = new MetadataMapper(caMappingFile);  
  }
  
  /**
   * Generates a Properties list with the Capture Agent metadata from the provided event 
   * @param event The SchedulerEvent from which the metadata should be generated as Capture Agent specific data 
   * @return A  Properties List with Capture Agent specific Data
   */
  public Properties generate (Event event) {
    logger.debug("generating Capture Agent metadata");
    
    Hashtable<String, String> caMetadata =  mapper.convert(event.getMetadataList());
    
    // pass through all workflow metadata to capture agent 
    for (Metadata m : event.getMetadataList()) {
      String key = m.getKey();
      if (key.startsWith("org.opencastproject.workflow.")) {
        caMetadata.put(key, m.getValue());
      }
    }
    
    Properties caCatalog = new Properties();
    if (event.containsKey("resources"))
      caCatalog.setProperty("capture.device.names", event.getResources());
    for (Entry<String, String> e : caMetadata.entrySet()) {
      caCatalog.put (e.getKey(), e.getValue());
    }       
    return caCatalog;
  }
  
  /**
   * Generates a Properties list with the Capture Agent metadata from the provided event 
   * @param event The SchedulerEvent from which the metadata should be generated as Capture Agent specific data 
   * @return A String with a Properties List with Capture Agent specific Data
   */
  public String generateAsString (Event event) {
    StringWriter writer = new StringWriter();
    try {
      generate(event).store(writer, "Capture Agent specific data");
      return writer.toString();
    } catch (IOException e) {
      logger.error("Could not convert Capture Agent Data to String");
    }
    return null;
  }
}
