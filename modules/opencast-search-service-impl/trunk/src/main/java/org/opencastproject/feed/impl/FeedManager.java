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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Feed;
import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.util.PluginLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This manager tries to satisfy requests to various feed types.
 */
@SuppressWarnings("unchecked")
public class FeedManager {

  /** Package to search for plugins */
  private static final String PLUGIN_PKG = "org.opencastproject.feed.impl.generator";

  /** Name of the plugin interface */
  private static final Class PLUGIN_INTERFACE = FeedGenerator.class;

  /** The list of plugins */
  private static List<Class<?>> pluginClasses = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(FeedManager.class);

  // Create the list of available element builder pugins
  static {
    pluginClasses = new ArrayList<Class<?>>();
    ClassLoader cl = FeedManager.class.getClassLoader();
    Class[] classes = PluginLoader.findPlugins(PLUGIN_PKG, null, new String[] { PLUGIN_INTERFACE.getName() }, cl);
    for (Class c : classes) {
      if (PLUGIN_INTERFACE.isAssignableFrom(c)) {
        try {
          c.newInstance();
          pluginClasses.add(c);
          log_.debug("Registering feed generator " + c.getName());
        } catch (InstantiationException e) {
          log_.error("Error creating new instance of class " + c.getName());
        } catch (IllegalAccessException e) {
          log_.error("Access exception for class " + c.getName());
        }
      }
    }
  }

  /**
   * Tries to answer the query with an xml feed by asking all registered plugins for help.
   * 
   * @param type
   *          the feed type
   * @param query
   *          the query string
   * @param locale
   *          the request locale
   * @return the feed or <code>null</code>
   * @throws IllegalArgumentException
   *           if an unkown feed type is requested or the query is <code>null</code>
   */
  public static Feed createFeed(Feed.Type type, String query[], Locale locale) throws IllegalArgumentException {
    String linkTemplate = "http://localhost/feeds"; // TODO: Get from configuration
    // TODO: Load as service
    for (Class<?> pluginClass : pluginClasses) {
      FeedGenerator generator;
      try {
        generator = (FeedGenerator) pluginClass.newInstance();
        if (linkTemplate != null)
          generator.setLinkTemplate(linkTemplate);
        if (generator.accept(query)) {
          return generator.createFeed(type, query, locale);
        }
      } catch (InstantiationException e) {
        log_.error("Error creating new instance of class " + pluginClass.getName());
      } catch (IllegalAccessException e) {
        log_.error("Access exception for class " + pluginClass.getName());
      }
    }

    // No plugin has accepted
    StringBuffer buf = new StringBuffer();
    for (String s : query) {
      if (buf.length() > 0)
        buf.append("/");
      buf.append(s);
    }
    log_.warn("Unable to answer feed request for '" + buf.toString() + "'");
    return null;
  }

}
