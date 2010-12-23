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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.opencastproject.scheduler.api.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class maps the given metadata keys from a SchedulerEvent to other metadata keys for a different domain (i.e.
 * Dublin Core)
 * 
 */
public class MetadataMapper {
  private static final Logger logger = LoggerFactory.getLogger(MetadataMapper.class);
  Properties mapping;

  /**
   * Constructor that needs the Mapping file
   * 
   * @param mappingFile
   *          An URL to a java Properties file
   * @throws FileNotFoundException
   * @throws IOException
   */
  public MetadataMapper(InputStream mappingFile) throws FileNotFoundException, IOException {
    mapping = new Properties();
    mapping.load(mappingFile);
  }

  /**
   * finds the corresponding key from the properties file
   * 
   * @param key
   *          the key in the current metadata set
   * @return the mapping key from the properties file
   */
  public String resolveKey(String key) {
    return mapping.getProperty(key);
  }

  /**
   * checks if for the given key a mapping is available
   * 
   * @param key
   *          the key to test
   * @return true if a key exist, otherwise false
   */
  public boolean mappingAvailable(String key) {
    return (mapping.getProperty(key) != null);
  }

  /**
   * Creates a new hashtable with the new keys, if available, and only the values where the keys could be converted. So
   * the returned hashtable may be smaller than the original one.
   * 
   * @param metadataSet
   *          the current metadata hashtable from the schedulerEvent
   * @return the new hashtable with the new keys and the corresponding values.
   */
  public Hashtable<String, String> convert(Hashtable<String, String> metadataSet) {
    Hashtable<String, String> updatedMetadata = new Hashtable<String, String>();

    Enumeration<String> keys = metadataSet.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      if (key != null && mappingAvailable(key) && metadataSet.get(key) != null) {
        logger.debug("Mapping {} on {} value: {}", new Object[] { key, resolveKey(key), metadataSet.get(key) });
        updatedMetadata.put(resolveKey(key), metadataSet.get(key));
      }
    }

    return updatedMetadata;
  }

  public Hashtable<String, String> convert(List<Metadata> list) {
    Hashtable<String, String> updatedMetadata = new Hashtable<String, String>();
    for (Metadata m : list) {
      if (m.getKey() != null && mappingAvailable(m.getKey()) && m.getValue() != null) {
        logger.debug("Mapping {} on {} value: {}", new Object[] { m.getKey(), resolveKey(m.getKey()), m.getValue() });
        updatedMetadata.put(resolveKey(m.getKey()), m.getValue());
      }
    }
    return updatedMetadata;
  }
}
