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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.util.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This manager class tries to read encoding profiles from the classpath.
 * 
 * TODO: Do we need another way of defining them? -tw
 */
public class EncodingProfileManager {

  /** Default name of the file containing the properties */
  private static final String PROP_FILENAME = "/encodingprofiles.properties";

  /** Prefix for encoding profile property keys **/
  private static String PROP_PREFIX = "profile.";

  /* Property names */
  private static final String PROP_NAME = ".name";
  private static final String PROP_APPLICABLE = ".input";
  private static final String PROP_OUTPUT = ".output";
  private static final String PROP_SUFFIX = ".suffix";
  private static final String PROP_MIMETYPE = ".mimetype";

  /** The profiles map */
  private Map<String, EncodingProfile> profiles = null;

  /** The logging instance */
  private static final Logger log_ = LoggerFactory.getLogger(EncodingProfileManager.class);

  /**
   * Creates a profile manager that will load the profiles from its default location, which is a file called
   * <code>encodingprofiles.properties</code> that needs to be on the classpath.
   * 
   * @throws IOException
   *           if the properties file cannot be located or accessed
   * @throws ConfigurationException
   *           if the configuration data is malformed
   */
  public EncodingProfileManager() throws IOException, ConfigurationException {
    Properties props = new Properties();
    InputStream is = EncodingProfileManager.class.getResourceAsStream(PROP_FILENAME);
    if (is == null)
      throw new ConfigurationException("Configuration for encoding profiles (" + PROP_FILENAME + ") missing");
    props.load(is);
    loadFromProperties(props);
  }

  /**
   * Initializes a set of encoding profiles from the given properties.
   * 
   * @param props
   *          the properties
   * @throws ConfigurationException
   *           if the configuration data is malformed
   */
  public EncodingProfileManager(Properties props) throws ConfigurationException {
    loadFromProperties(props);
  }

  /**
   * Returns the list of profiles.
   * 
   * @return the profile definitions
   */
  public Map<String, EncodingProfile> getProfiles() {
    return profiles;
  }
  
  /**
   * Returns the encoding profile for the given identifier or <code>null</code> if no
   * such profile has been configured.
   * 
   * @param id
   *          the profile identifier
   * @return the profile
   */
  public EncodingProfile getProfile(String id) {
    return profiles.get(id);
  }

  /**
   * Returns the list of profiles that are applicable for the given track type.
   * 
   * @return the profile definitions
   */
  public Map<String, EncodingProfile> getApplicableProfiles(MediaType type) {
    Map<String, EncodingProfile> result = new HashMap<String, EncodingProfile>();
    for (Map.Entry<String, EncodingProfile> entry : profiles.entrySet()) {
      EncodingProfile profile = entry.getValue();
      if (profile.isApplicableTo(type)) {
        result.put(entry.getKey(), profile);
      }
    }
    return result;
  }

  /**
   * Reads the profiles from the given set of properties.
   * 
   * @param properties
   *          the properties
   */
  private void loadFromProperties(Properties properties) {
    if (properties == null)
      throw new IllegalArgumentException("Encoding properties cannot be null");
    
    // Find list of formats in properties
    List<String> profileNames = new ArrayList<String>();
    for (Object fullKey : properties.keySet()) {
      String key = fullKey.toString();
      if (key.startsWith(PROP_PREFIX) && key.endsWith(PROP_NAME)) {
        int separatorLocation = fullKey.toString().lastIndexOf('.');
        key = key.substring(PROP_PREFIX.length(), separatorLocation);
        if (!profileNames.contains(key)) {
          profileNames.add(key);
        } else {
          throw new ConfigurationException("Found duplicate definition for encoding profile '" + key + "'");
        }
      }
    }

    // Load the formats
    profiles = new HashMap<String, EncodingProfile>();
    for (String profileId : profileNames) {
      log_.debug("Enabling media format " + profileId);
      EncodingProfile profile = loadProfile(profileId, properties);
      profiles.put(profileId, profile);
    }
  }

  /**
   * Reads the profile from the given properties
   * 
   * @param profile
   * @param properties
   * @return
   */
  private EncodingProfile loadProfile(String profile, Properties properties) {
    String identifier = profile;
    List<String> defaultProperties = new ArrayList<String>(10);

    // Format name
    Object name = getDefaultProperty(profile, PROP_NAME, properties, defaultProperties);
    if (name == null || "".equals(name.toString().trim()))
      throw new ConfigurationException("Distribution profile '" + profile
              + "' is missing a name. (Check web.xml profiles.)");

    EncodingProfileImpl df = new EncodingProfileImpl(identifier, name.toString().trim());

    // Output Type
    Object type = getDefaultProperty(profile, PROP_OUTPUT, properties, defaultProperties);
    if (type == null || "".equals(type.toString().trim()))
      throw new ConfigurationException("Type of profile '" + profile + "' is missing");
    try {
      df.outputType = MediaType.parseString(type.toString().trim());
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException("Type '" + type + "' of profile '" + profile + "' is unknwon");
    }

    // Suffix
    Object suffixObj = getDefaultProperty(profile, PROP_SUFFIX, properties, defaultProperties);
    if (suffixObj == null || "".equals(suffixObj.toString().trim()))
      throw new ConfigurationException("Suffix of profile '" + profile + "' is missing");
    df.suffix = suffixObj.toString();

    // Mimetype
    Object mimeTypeObj = getDefaultProperty(profile, PROP_MIMETYPE, properties, defaultProperties);
    if (mimeTypeObj == null || "".equals(mimeTypeObj.toString().trim()))
      throw new ConfigurationException("Mime type of profile '" + profile + "' is missing");
    df.mimeType = mimeTypeObj.toString();

    // Applicable to the following track categories
    Object applicableObj = getDefaultProperty(profile, PROP_APPLICABLE, properties, defaultProperties);
    if (applicableObj == null || "".equals(applicableObj.toString().trim()))
      df.applicableTypes = new MediaType[] {};
    else {
      StringTokenizer tok = new StringTokenizer(applicableObj.toString(), " ,;");
      List<MediaType> applicable = new ArrayList<MediaType>();
      while (tok.hasMoreTokens()) {
        applicable.add(MediaType.parseString(tok.nextToken().trim()));
      }
      df.applicableTypes = applicable.toArray(new MediaType[applicable.size()]);
    }

    // Look for extensions
    String extensionKey = PROP_PREFIX + profile + ".";
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith(extensionKey) && !defaultProperties.contains(key)) {
        String k = key.substring(extensionKey.length());
        String v = entry.getValue().toString();
        df.addExtension(k, v);
      }
    }

    return df;
  }

  /**
   * Returns the default property and registers the property key in the list.
   * 
   * @param profile
   *          the profile identifier
   * @param keySuffix
   *          the key suffix, like ".name"
   * @param properties
   *          the properties
   * @param list
   *          the list of default property keys
   * @return the property value or <code>null</code>
   */
  private static String getDefaultProperty(String profile, String keySuffix, Properties properties, List<String> list) {
    StringBuffer buf = new StringBuffer(PROP_PREFIX);
    buf.append(profile);
    buf.append(keySuffix);
    String key = buf.toString();
    list.add(key);
    return properties.getProperty(key);
  }

}
