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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class for retrieving, storing and accessing both local and centralised 
 * configuration files for the CaptureAgent. Uses java.util.Properties to store
 * configuration file which can read/write INI style config files.
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

  /** The machine-written copy of the configuration data.  Not to be edited by hand */
  private File cachedConfig;
  
  /** Timer that will every at a specified interval to retrieve the centralised
   * configuration file from a server */
  private Timer timer;
  
  /**
   * Private constructor to enforce the singleton object. Should only be called
   * if manager field is null.
   */
  private ConfigurationManager() {
    properties = new Properties();
    
    // TODO: Do not rely on config file in resource bundle
    // Load the configuration packed with the bundle
    URL bundleConfig = getClass().getClassLoader().getResource("config/capture.properties");
    try {
      properties.load(bundleConfig.openStream());
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL, cannot load bundle config file: {}.", e.getMessage());
    } catch (FileNotFoundException e) {
      logger.error("Bundle configuration file not found: {}.", e.getMessage());
    } catch (IOException e) {
      logger.error("Unable to load bundle config file: {}.", e.getMessage());
    }

    createCoreDirectories();

    //Setup the cached copy of the config and load it
    try {
      cachedConfig = new File(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_CACHE_URL));
      if (!cachedConfig.isFile()) {
        cachedConfig.getParentFile().mkdirs();
        cachedConfig.createNewFile();
      }
      if (!cachedConfig.isFile()) {
        logger.warn("Unable to create cache for config file.");
      } else {
        properties.load(new FileInputStream(cachedConfig));
      }
    } catch (NullPointerException e) {
      logger.warn("Malformed URL for {}: {}.", CaptureParameters.CAPTURE_CONFIG_CACHE_URL, properties.getProperty(CaptureParameters.CAPTURE_CONFIG_CACHE_URL));
    } catch (IOException e) {
      logger.warn("IOException creating cached config file.");
    }

    // Checking the filesystem for configuration file.
    //Note that we check the remote source at the bottom of this function using a timer and an UpdateConfig class
    try {
      localConfig = new File(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_FILESYSTEM_URL));
      properties.load(new FileInputStream(localConfig));
    } catch (NullPointerException e) {
      logger.warn("Malformed URL for " + CaptureParameters.CAPTURE_FILESYSTEM_CONFIG_URL);
    } catch (IOException e) {
      logger.warn("Could not load local configuration: " + e.getMessage());
    }

    // Attempt to parse the location of the configuration server
    try {
      url = new URL(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_ENDPOINT_URL));
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL for {}, disabling polling.", CaptureParameters.CAPTURE_CONFIG_ENDPOINT_URL);
    }

    // If this is the case capture there will be no capture devices specified
    if (url == null && localConfig == null && cachedConfig == null) {
      logger.error("No configuration data was found, this is very bad!");
      throw new RuntimeException("No configuration data found for ConfigurationManager!");
    }

    retrieveConfigFromDisk();
    retrieveConfigFromServer();
    writeConfigFileToDisk();
    
    // if reload property specified, query server for update at that interval
    String reload = getItem(CaptureParameters.CAPTURE_CONFIG_POLLING_INTERVAL);
    if (url != null && reload != null) {
      timer = new Timer();
      long delay = 0;
      try {
        // Times in the config file are in seconds, so multiply by 1000
        delay = Long.parseLong(reload) * 1000L;
        if (delay < 1) {
          logger.info("Polling time has been set to less than 1 second, polling disabled.");
          return;
        }
      } catch (NumberFormatException e) {
        logger.warn("Invalid polling time for parameter {}.", CaptureParameters.CAPTURE_CONFIG_POLLING_INTERVAL);
        // If the polling time value is invalid, don't poll
        return;
      }
      timer.schedule(new UpdateConfig(), delay, delay);
    }
  }

  /**
   * Creates the core Opencast directories.
   */
  private void createCoreDirectories() {
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_CONFIG_URL, this);
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_CACHE_URL, this);
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_VOLATILE_URL, this);
  }

  /**
   * Creates a file or directory.
   * @param key    The key to set in the configuration manager.  Key is set equal to name.
   * @param config The configuration manager to store the key-value pair.
   */
  private void createFileObj(String key, ConfigurationManager config) {
    File target = null;
    try {
      target = new File (config.getItem(key));
      FileUtils.forceMkdir(target);
      config.setItem(key, target.toString());
      if (!target.exists()) {
        throw new RuntimeException("Unable to create directory " + target + ".");
      }
    } catch (IOException e) {
      logger.error("Unable to create directory: {}, because {} happened.", target, e.getMessage());
    } catch (NullPointerException e) {
      logger.error("No value found for key {}.", key);
    }
  }
  
  /**
   * Gets the configuration manager instance.
   * @return the singleton ConfigurationManager.
   */
  public static synchronized ConfigurationManager getInstance() {
    if (manager == null)
      manager = new ConfigurationManager();
    return manager;
  }
  
  /**
   * Retrieve property for configuration.
   * @param key the key to retrieve from the property list.
   * @return the value corresponding to the key.
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
   * Add a key, value pair to the property list.
   * @param key the key to be placed in the properties list.
   * @param value the corresponding value.
   */
  public void setItem(String key, String value) {
    if (properties == null) {
      properties = new Properties();
    }
    if (key == null) {
      return;
    }
    // this will overwrite the previous value is there is a conflict
    properties.setProperty(key, value);
  }
  
  /**
   * Read a remote properties file and load it into memory.
   * This call merges whatever updated configuration settings it gets with the currently existing ones.
   * Note that this means to *clear* a setting on the capture agent one must set it to *blank* on the server.
   */
  private void retrieveConfigFromServer() {
    if (url == null) {
      return;
    }

    try {
      URLConnection urlc = url.openConnection();
      Properties temp = new Properties();
      temp.load(urlc.getInputStream());
      merge(temp, true);
    } catch (Exception e) {
      logger.warn("Could not get config file from server: {}.", e.getMessage());
    }
  }

  /**
   * Fetches the user-editable local copy of the configuration settings.
   * This call merges whatever updated configuration settings it gets with the currently existing ones.
   * Note that this means to *clear* a setting on the capture agent one must set it to *blank* in the configuration file.
   */
  private void retrieveConfigFromDisk() {
    if (localConfig == null) {
      return;
    }

    try {
      Properties t = new Properties();
      t.load(new FileInputStream(localConfig));
      merge(t, true);
    } catch (Exception e) {
      logger.warn("Exception reading {}:  {}.", localConfig.getAbsolutePath(), e.getMessage());
    }
  }
  
  /**
   * Stores a local copy of the properties on disk.
   */
  private void writeConfigFileToDisk() {
    if (properties == null || cachedConfig == null) {
      return;
    }

    try {
      if (!cachedConfig.isFile()) {
        cachedConfig.getParentFile().mkdirs();
        cachedConfig.createNewFile();
      }  
      properties.store(new FileOutputStream(cachedConfig), "Autogenerated config file, do not edit.");
    } catch (Exception e) {
      logger.warn("Could not write config file to disk: {}.", e.getMessage());
    }
  }
  
  /**
   * Returns a Dictionary of all the properties associated with this configuration manager.
   * @return the key/value pair mapping.
   */
  public Properties getAllProperties() {
    return (Properties) properties.clone();
  }
  
  /**
   * Merges the given Properties with the ConfigurationManager's properties. Will
   * not overwrite the ConfigurationManager if specified.
   * 
   * @param p Properties object to be merged with ConfigurationManager.
   * @param overwrite true if this should overwrite the ConfigurationManager's properties, false if not.
   * @return the merged properties.
   */
  public Properties merge(Properties p, boolean overwrite) {
    // if no properties specified, just return current configuration
    if (p == null) {
      return getAllProperties();
    }
    // overwrite the current properties in the ConfigurationManager
    if (overwrite) {
      for (Object key : p.keySet()) {
        properties.setProperty((String) key, (String) p.get(key));
      }
      return getAllProperties();
    }
    // do not overwrite the ConfigurationManager, but merge the properties
    else {
      Properties merged = getAllProperties();
      for (Object key : p.keySet()) {
        merged.setProperty((String) key, (String) p.get(key));
      }
      return merged;
    }

  }
  
  /**
   * Used in a timer that will fire every specified time interval to attempt to
   * get a new version of the capture configuration from a centralised server.
   */
  class UpdateConfig extends TimerTask {

    @Override
    public void run() {
      retrieveConfigFromServer();
      writeConfigFileToDisk();
    }
  }
}
