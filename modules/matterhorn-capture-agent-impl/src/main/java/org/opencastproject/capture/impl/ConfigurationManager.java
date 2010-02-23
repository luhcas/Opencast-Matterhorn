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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for retrieving, storing and accessing both local and centralised 
 * configuration files for the CaptureAgent. Uses java.util.Properties to store
 * configuration file which can read/write INI style config files.
 */
public class ConfigurationManager implements ManagedService {
  
  /** slf4j logging */
  private static final Logger logger = 
    LoggerFactory.getLogger(ConfigurationManager.class);
  
  /** Hashtable that represents config file in memory */
  private Properties properties;
  
  /** should point to a centralised config file */
  private URL url; 
  
  /** Timer that will every at a specified interval to retrieve the centralised
   * configuration file from a server */
  private Timer timer;

  public void activate(ComponentContext cc) throws ConfigurationException {
    properties = new Properties();
    
    //Load the local config from Felix's config directory
    properties = new Properties();
    if (cc != null) {
      updated(cc.getProperties());
    }
  }

  public void deactivate() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  @Override
  public void updated(Dictionary props) throws ConfigurationException {
    if (props == null) {
      logger.debug("Null properties in updated!");
      return;
    }
    Enumeration<String> keys = props.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      properties.put(key, props.get(key));
    }

    // Attempt to parse the location of the configuration server
    try {
      url = new URL(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_REMOTE_ENDPOINT_URL));
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL for {}, disabling polling.", CaptureParameters.CAPTURE_CONFIG_REMOTE_ENDPOINT_URL);
    }

    // If this is the case capture there will be no capture devices specified
    if (url == null) {
      logger.info("No remote configuration endpoint was found, relying on local config.");
    }

    createCoreDirectories();

    Properties server = retrieveConfigFromServer();
    if (server != null) {
      writeConfigFileToDisk(server);
      merge(server, true);
    }
    
    //Shut down the old timer if it exists
    if (timer != null) {
      timer.cancel();
    }

    // if reload property specified, query server for update at that interval
    String reload = getItem(CaptureParameters.CAPTURE_CONFIG_REMOTE_POLLING_INTERVAL);
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
        logger.warn("Invalid polling time for parameter {}.", CaptureParameters.CAPTURE_CONFIG_REMOTE_POLLING_INTERVAL);
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

    if (value != null) {
      // this will overwrite the previous value is there is a conflict
      properties.setProperty(key, value);
    } else {
      properties.remove(key);
    }
  }
  
  /**
   * Read a remote properties file and load it into memory.
   * The URL it attempts to read from is defined by CaptureParameters.CAPTURE_CONFIG_ENDPOINT_URL
   * 
   * @return The properties (if any) fetched from the server
   * @see org.opencastproject.capture.impl.CaptureParameters#CAPTURE_CONFIG_REMOTE_ENDPOINT_URL
   */
  private Properties retrieveConfigFromServer() {
    if (url == null) {
      return null;
    }

    Properties p = new Properties();
    try {
      URLConnection urlc = url.openConnection();
      p.load(urlc.getInputStream());
    } catch (Exception e) {
      logger.warn("Could not get config file from server: {}.", e.getMessage());
    }
    return p;
  }

  /**
   * Stores a local copy of the properties on disk.
   * 
   * @param p The properties object you wish to store.  It will be saved at CaptureParameters.CAPTURE_CONFIG_FILESYSTEM_URL
   * @see org.opencastproject.capture.impl.CaptureParameters#CAPTURE_CONFIG_CACHE_URL
   */
  private void writeConfigFileToDisk(Properties p) {
    File cachedConfig = new File(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_CACHE_URL));

    if (p == null) {
      logger.warn("Unable to write config to disk because parameter was null");
      return;
    }

    FileOutputStream fout = null;
    try {
      if (!cachedConfig.isFile()) {
        cachedConfig.getParentFile().mkdirs();
        cachedConfig.createNewFile();
      }
      fout = new FileOutputStream(cachedConfig);
      p.store(fout, "Autogenerated config file, do not edit.");
    } catch (Exception e) {
      logger.warn("Could not write config file to disk: {}.", e.getMessage());
    } finally {
      IOUtils.closeQuietly(fout);
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
        merged.setProperty(key.toString(), p.get(key).toString());
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
      Properties server = retrieveConfigFromServer();
      if (server != null) {
        writeConfigFileToDisk(server);
        merge(server, true);
      }
    }
  }
}
