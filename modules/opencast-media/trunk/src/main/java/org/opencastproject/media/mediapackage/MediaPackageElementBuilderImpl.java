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

package org.opencastproject.media.mediapackage;

import org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.PluginLoader;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Default implementation for a media package element builder.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageElementBuilderImpl.java 2905 2009-07-15 16:16:05Z ced $
 */
public class MediaPackageElementBuilderImpl implements MediaPackageElementBuilder {

  /** Package to search for plugins */
  private static final String PLUGIN_PKG = "org.opencastproject.media.mediapackage.elementbuilder";

  /** Name of the plugin interface */
  private static final Class<?> PLUGIN_INTERFACE = MediaPackageElementBuilderPlugin.class;

  /** The list of plugins */
  private static List<Class<? extends MediaPackageElementBuilderPlugin>> plugins = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(MediaPackageElementBuilderImpl.class.getName());

  // Create the list of available element builder pugins
  static {
    plugins = new ArrayList<Class<? extends MediaPackageElementBuilderPlugin>>();
    ClassLoader cl = MediaPackageElementBuilderImpl.class.getClassLoader();
    Class<?>[] pluginClasses = PluginLoader.findPlugins(PLUGIN_PKG, null, new String[] { PLUGIN_INTERFACE.getName() },
            cl);
    for (Class<?> c : pluginClasses) {
      if (PLUGIN_INTERFACE.isAssignableFrom(c)) {
        plugins.add((Class<? extends MediaPackageElementBuilderPlugin>) c);
        log_.debug("Adding " + c.getName() + " to the list of element builders");
      }
    }
  }

  /** @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromFile(java.io.File) */
  public MediaPackageElement elementFromFile(File file) throws MediaPackageException {
    return elementFromFile(file, null, null);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromFile(java.io.File,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement elementFromFile(File file, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws MediaPackageException {
    // Check system support for the file
    try {
      MimeTypes.fromFile(file);
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("File type not supported: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("IO Exception while reading media package element " + file + ": "
              + e.getMessage());
    }

    // Feed the file to the element builder plugins
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    {
      MediaPackageElementBuilderPlugin plugin = null;
      for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
        try {
          plugin = createPlugin(pluginClass);
          if (plugin.accept(file, type, flavor))
            candidates.add(plugin);
        } catch (IOException e) {
          log_.warn("IO Exception while analyzing " + file + " using element plugin " + plugin);
        }
      }
    }

    // Check the plugins
    if (candidates.size() == 0)
      throw new MediaPackageException("No suitable element builder plugin found for " + file);
    candidates = filterPreferred(candidates);
    if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (MediaPackageElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      log_.warn("More than one element builder plugin with the same priority claims responsibilty for " + file + ": "
              + buf.toString());
    }

    // Create media package element depending on mime type flavor
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.elementFromFile(file);
    builderPlugin.cleanup();
    return element;
  }

  /** @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromManifest(Node, File, boolean) */
  public MediaPackageElement elementFromManifest(Node node, File packageRoot, boolean verify)
          throws MediaPackageException {
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
      MediaPackageElementBuilderPlugin plugin = createPlugin(pluginClass);
      if (plugin.accept(node)) {
        candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0) {
      log_.warn("No element builder found for node of type " + node.getNodeName());
      return null;
    } else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (MediaPackageElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      XPath xpath = XPathFactory.newInstance().newXPath();
      String name = node.getNodeName();
      String elementFlavor = null;
      try {
        elementFlavor = xpath.evaluate("@type", node);
      } catch (XPathExpressionException e) {
        elementFlavor = "(unknown)";
      }
      log_.warn("More than one element builder plugin claims responsability for " + name + " of flavor "
              + elementFlavor + ": " + buf.toString());
    }

    // Create a new media package element
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.elementFromManifest(node, packageRoot, verify);
    builderPlugin.cleanup();
    return element;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws IOException {
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
      MediaPackageElementBuilderPlugin plugin = createPlugin(pluginClass);
      if (plugin.accept(type, flavor)) {
        candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0)
      return null;
    else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (MediaPackageElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      log_.warn("More than one element builder plugin claims responsibilty for " + flavor + ": " + buf.toString());
    }

    // Create a new media package element
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.newElement(type, flavor);
    builderPlugin.cleanup();
    return element;
  }

  /** Creates and initializes a new builder plugin. */
  private MediaPackageElementBuilderPlugin createPlugin(Class<? extends MediaPackageElementBuilderPlugin> clazz) {
    MediaPackageElementBuilderPlugin plugin = null;
    try {
      plugin = clazz.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Cannot instantiate media package element builder plugin of type " + clazz.getName()
              + ". Did you provide a parameterless constructor?", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    try {
      plugin.setup();
    } catch (Exception e) {
      throw new RuntimeException("An error occured while setting up media package element builder plugin " + plugin);
    }
    return plugin;
  }

  /** Gets the preferred plugins. */
  private List<MediaPackageElementBuilderPlugin> filterPreferred(List<MediaPackageElementBuilderPlugin> plugins) {
    if (plugins.size() > 0) {
      List<MediaPackageElementBuilderPlugin> preferred = new ArrayList<MediaPackageElementBuilderPlugin>();
      Collections.sort(plugins, PriorityComparator.INSTANCE);
      int priority = plugins.get(0).getPriority();
      for (MediaPackageElementBuilderPlugin plugin : plugins) {
        if (priority > plugin.getPriority())
          break;
        preferred.add(plugin);
        priority = plugin.getPriority();
      }
      return preferred;
    } else
      return plugins;

  }

  // --------------------------------------------------------------------------------------------

  private static final class PriorityComparator implements Comparator<MediaPackageElementBuilderPlugin> {

    static final PriorityComparator INSTANCE = new PriorityComparator();

    public int compare(MediaPackageElementBuilderPlugin o1, MediaPackageElementBuilderPlugin o2) {
      return o2.getPriority() - o1.getPriority();
    }
  }
}