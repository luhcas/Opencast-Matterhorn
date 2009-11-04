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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for retrieving, storing and accessing both local and centralised 
 * configuration files for the CaptureAgent. Uses java.util.Properties to store
 * configuration file which can read/write INI style config files
 */
public class ConfigurationManager {
  
  /** slf4j logging */
  private static final Logger logger = 
    LoggerFactory.getLogger(ConfigurationManager.class);
  
  /** The singleton instance for this class */
  private static ConfigurationManager manager;

  /** Hashtable that represents config file in memory */
  private Properties properties;
  
  /** should point to a centralised config file */
  private URL url; 
  
  /** the local copy of the configuration */
  private File localConfig;
  
  /** Timer that will every at a specified interval to retrieve the centralised
   * configuration file from a server */
  private Timer timer;
  
  /**
   * Private constructor to enforce the singleton object. Should only be called
   * if manager field is null.
   */
  private ConfigurationManager() {
    //TODO: Do not rely on config file in resource bundle
    URL bundleConfig = getClass().getClassLoader().getResource("config/capture.cfg");
    properties = new Properties();
    
    /* attempt to load properties into memory and retrieve centralised config */
    try {
      properties.load(bundleConfig.openStream());
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL, cannot load bundle config file", e);
    } catch (FileNotFoundException e) {
      logger.error("Bundle configuration file not found.", e);
    } catch (IOException e) {
      logger.error("Unable to load bundle config file", e);
    }

    try {
      url = new URL(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_URL));
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL for " + CaptureParameters.CAPTURE_CONFIG_URL + ", disabling polling");
    }

    try {
      localConfig = new File(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_CACHE_URL));
    } catch (NullPointerException e) {
      logger.warn("Malformed URL for " + CaptureParameters.CAPTURE_CONFIG_CACHE_URL + ", disabling caching");
    }

    if (url == null && localConfig == null) {
      logger.error("No configuration data was found, this is very bad!");
    }
    
    retrieveConfigFromServer();
    writeConfigFileToDisk();
    
    /* if reload property specified, query server for update at that interval */
    String reload = getItem(CaptureParameters.CAPTURE_CONFIG_POLLING_INTERVAL);
    if (url != null && reload != null) {
      timer = new Timer();
      long delay = 0;
      try {
        delay = Long.parseLong(reload);
        if (delay < 1) {
          logger.info("Polling time has been set to less than 1 second, polling disabled");
          return;
        }
      } catch (NumberFormatException e) {
        logger.warn("Invalid polling time for parameter " + CaptureParameters.CAPTURE_CONFIG_POLLING_INTERVAL);
        //If the polling time value is invalid, don't poll
        return;
      }
      timer.schedule(new UpdateConfig(), delay, delay);
    }
  }
  
  /**
   * @return the singleton ConfigurationManager
   */
  public static synchronized ConfigurationManager getInstance() {
    if (manager == null)
      manager = new ConfigurationManager();
    return manager;
  }
  
  /**
   * Retrieve property for configuration
   * @param key the key to retrieve from the property list
   * @return the value corresponding to the key
   */
  public String getItem(String key) {
    if (properties == null) {
      logger.warn("No properties are loaded into memory.");
      return null;
    } else if (key == null) {
      return null;
    }
    else
      return properties.getProperty(key);
  }
  
  /**
   * Add a key, value pair to the property list
   * @param key the key to be placed in the properties list
   * @param value the corresponding value
   */
  public void setItem(String key, String value) {
    if (properties == null) {
      properties = new Properties();
    }
    if (key == null) {
      return;
    }
    /* this will overright the previous value is there is a conflict */
    properties.setProperty(key, value);
  }
  
  /**
   * Read a remote properties file and load it into memory.
   */
  private void retrieveConfigFromServer() {
    if (url == null) {
      return;
    }

    try {
      URLConnection urlc = url.openConnection();
      properties.load(urlc.getInputStream());
    } catch (Exception e) {
      logger.warn("Could not get config file from server", e);
    }
  }
  
  /**
   * Stores a local copy of the properties on disk.
   */
  private void writeConfigFileToDisk() {
    if (properties == null || localConfig == null) {
      return;
    }

    try {
      if (!localConfig.isFile()) {
        localConfig.createNewFile();
      }
      properties.store(new FileWriter(localConfig), "capture config");
    } catch (Exception e) {
      logger.warn("Could not write config file to disk!");
    }
  }
  
  /**
   * Returns a Dictionary of all the properties associated with this
   * configuration manager
   * @return the key/value pair mapping
   */
  @SuppressWarnings("unchecked")
  public Dictionary<String, String> getAllProperties() {
    return (Dictionary<String, String>) properties.clone();
  }
  
  /**
   * Produces a Dictionary that combines the key/value pairs from both
   * parameters. If there are conflicts the primary value will remain.
   * @param primary the dictionary that should not change
   * @param secondary the dictionary that will change if there are conflicts
   * @return a combination of the mappings using stated collision resolution
   */
  @SuppressWarnings("unchecked")
  public Dictionary<String, String> merge(Dictionary<String, String> primary, 
          Dictionary<String, String> secondary) {
    Hashtable<String, String> merged = 
      new Hashtable<String, String>((Map<String, String>)primary);
    
    for (Enumeration<String> i = secondary.keys(); i.hasMoreElements();) {
      String next = i.nextElement();
      if (merged.get(next) == null)
        merged.put(next, secondary.get(next));
    }
    return merged;
  }
  
  /**
   * Used in a timer that will fire every specified time interval to attempt to
   * get a new version of the capture configuration from a centralised server.
   */
  class UpdateConfig extends TimerTask {
    public void run() {
      retrieveConfigFromServer();
      writeConfigFileToDisk();
    }
  }
}
