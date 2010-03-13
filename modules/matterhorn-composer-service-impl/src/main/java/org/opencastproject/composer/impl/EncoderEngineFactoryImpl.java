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

package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.util.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Factory used to obtain an encoder engine based on the node configuration and an optional encoding profile passed.
 */
public class EncoderEngineFactoryImpl extends EncoderEngineFactory {

  /** Name of the configuration files */
  private static final String SETTINGS_FILE = "/encoderengines.properties";
  
  /** Property prefix for encoder configuration */
  private static final String PROP_PREFIX = "encoder.";

  /** Property suffix for encoder class configuration */
  private static final String CLASS_SUFFIX = ".class";

  /** The engines */
  private Map<String, Class<?>> engineClasses = null;

  /** The default encoder engine */
  private Class<?> defaultEngineClass = null;

  /** the logging facility provided by log4j */
  private static Logger log_ = LoggerFactory.getLogger(EncoderEngineFactoryImpl.class.getName());

  /**
   * Private method to create a new engine factory.
   */
  public EncoderEngineFactoryImpl() {
    engineClasses = new HashMap<String, Class<?>>();
    initProfiles();
  }

  /**
   * Reads the engines from the properties found on the local node, starting with prefix <code>encoder</code>.
   */
  private void initProfiles() {
    Properties config = new Properties();
    List<String> engineList = new ArrayList<String>();
    try {
      config.load(EncoderEngineFactoryImpl.class.getResourceAsStream(SETTINGS_FILE));
      for (Object keyObject : config.keySet()) {
        String key = keyObject.toString();
        if (key.startsWith(PROP_PREFIX) && key.endsWith(CLASS_SUFFIX)) {
          int separatorLocation = key.lastIndexOf('.');
          String engineId = key.substring(PROP_PREFIX.length(), separatorLocation);
          if (!engineList.contains(engineId))
            log_.info("Adding {} to the list of encoding engines", engineId);
            engineList.add(engineId);
        }
      }
    } catch (IOException e) {
     throw new ConfigurationException("Unable to load encoder engine configuration: " + e.getMessage());
    }

    // Are there any configurations available?
    if (engineList.size() == 0)
      throw new ConfigurationException("No encoder engine configurations found");

    // Instantiate the engines
    for (String engineId : engineList) {
      log_.debug("Instantiating encoding engine " + engineId);

      String className = config.getProperty(PROP_PREFIX + engineId + ".class");
      String profileList = config.getProperty(PROP_PREFIX + engineId + ".profiles");
      String defaultString = config.getProperty(PROP_PREFIX + engineId + ".default");

      if (className == null) {
        throw new ConfigurationException("Class not specified for engine '" + engineId + "'");
      }

      // Create the engine
      EncoderEngine engine = null;
      Class<?> engineClass = null;
      try {
        engineClass = Class.forName(className);
        engine = (EncoderEngine) engineClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException("Class not found while creating encoder engine: " + e.getMessage(), e);
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating encoder engine: " + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating encoder engine: " + e.getMessage(), e);
      }
      engine.setConfig(engineConfig);

      // Get the profile list
      List<String> profiles = new ArrayList<String>();
      if (profileList != null) {
        StringTokenizer tok = new StringTokenizer(profileList, " ,;");
        while (tok.hasMoreTokens()) {
          profiles.add(tok.nextToken());
        }
      }

      // Register engine for specified profiles
      for (String profile : profiles) {
        if (engineClasses.containsKey(profile)) {
          Class<?> first = engineClasses.get(profile);
          log_.warn("Encoder engine '" + engine + "' claims responsibility for profile '" + profile + "', but " + first
                  + " is already registered");
        } else {
          engineClasses.put(profile, engineClass);
        }
      }

      // Is this the default engine?
      if ("true".equals(defaultString) || profiles.size() == 0) {
        if (defaultEngineClass != null) {
          log_.warn("Additional encoder engine '" + engine + "' claims to be te default engine");
        } else {
          log_.debug("Setting encoder engine '" + engine + "' as default");
          defaultEngineClass = engineClass;
        }
      }

    }
  }

  /**
   * Factory method that returns an instance of an engine factory for the given profile.
   * 
   * @param profile
   *          the encoding profile
   * @return the encoder engine
   * @throws IllegalStateException
   *           If no suitable engine can be found
   */
  public EncoderEngine newEngineByProfile(String profile) throws IllegalStateException {
    if (profile == null)
      throw new IllegalArgumentException("Argument 'profile' must not be null");

    // Find out which engine to instantiate
    Class<?> engineClass = engineClasses.get(profile);
    if (engineClass == null && defaultEngineClass != null) {
      engineClass = defaultEngineClass;
      log_.debug("Servicing request for encoding with profile '" + profile + "' with default engine " + defaultEngineClass);
    }

    // Create a new engine instance
    if (engineClass != null) {
      EncoderEngine engine = null;
      try {
        EncodingProfileManager mgr = new EncodingProfileManager();
        engine = (EncoderEngine) engineClass.newInstance();
        if (engine instanceof AbstractEncoderEngine) {
          Map<String, EncodingProfile> profiles = new HashMap<String, EncodingProfile>();
          for (Map.Entry<String, Class<?>> entry : engineClasses.entrySet()) {
            String profileId = entry.getKey();
            if (profileId.equals(engineClass)) { // FIXME comparing a string to a class will always be false
              EncodingProfile p = mgr.getProfile(profileId);
              if (p != null)
                profiles.put(profileId, p);
            }
          }          
          ((AbstractEncoderEngine)engine).setSupportedProfiles(profiles);
        }
        engine.setConfig(engineConfig);
        return engine;
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating encoder engine: " + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating encoder engine: " + e.getMessage(), e);
      } catch (IOException e) {
        throw new ConfigurationException("Error configuring encoder engine: " + e.getMessage(), e);
      }
    }

    throw new ConfigurationException("No encoding engine found to serve profile '" + profile + "'");
  }

}
