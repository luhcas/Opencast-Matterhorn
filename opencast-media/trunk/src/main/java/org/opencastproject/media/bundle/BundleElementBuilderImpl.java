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

package org.opencastproject.media.bundle;

import org.opencastproject.media.bundle.BundleElement.Type;
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
 * Default implementation for a bundle element builder.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleElementBuilderImpl implements BundleElementBuilder {

  /** Package to search for plugins */
  private static final String PLUGIN_PKG = "org.opencastproject.media.bundle.elementbuilder";

  /** Name of the plugin interface */
  private static final Class<?> PLUGIN_INTERFACE = BundleElementBuilderPlugin.class;

  /** The list of plugins */
  private static List<Class<? extends BundleElementBuilderPlugin>> plugins = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory
      .getLogger(BundleElementBuilderImpl.class);

  // Create the list of available element builder pugins
  static {
    plugins = new ArrayList<Class<? extends BundleElementBuilderPlugin>>();
    ClassLoader cl = BundleElementBuilderImpl.class.getClassLoader();
    Class<?>[] pluginClasses = PluginLoader.findPlugins(PLUGIN_PKG, null,
        new String[] { PLUGIN_INTERFACE.getName() }, cl);
    for (Class<?> c : pluginClasses) {
      if (PLUGIN_INTERFACE.isAssignableFrom(c)) {
        plugins.add((Class<? extends BundleElementBuilderPlugin>) c);
        log_
            .debug("Adding " + c.getName() + " to the list of element builders");
      }
    }
  }

  /** @see org.opencastproject.media.bundle.BundleElementBuilder#elementFromFile(java.io.File) */
  public BundleElement elementFromFile(File file) throws BundleException {
    return elementFromFile(file, null, null);
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilder#elementFromFile(java.io.File,
   *      org.opencastproject.media.bundle.BundleElement.Type,
   *      org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public BundleElement elementFromFile(File file, Type type,
      BundleElementFlavor flavor) throws BundleException {
    // Check system support for the file
    try {
      MimeTypes.fromFile(file);
    } catch (UnknownFileTypeException e) {
      throw new BundleException("File type not supported: " + e.getMessage());
    } catch (IOException e) {
      throw new BundleException("IO Exception while reading bundle element "
          + file + ": " + e.getMessage());
    }

    // Feed the file to the element builder plugins
    List<BundleElementBuilderPlugin> candidates = new ArrayList<BundleElementBuilderPlugin>();
    {
      BundleElementBuilderPlugin plugin = null;
      for (Class<? extends BundleElementBuilderPlugin> pluginClass : plugins) {
        try {
          plugin = createPlugin(pluginClass);
          if (plugin.accept(file, type, flavor))
            candidates.add(plugin);
        } catch (IOException e) {
          log_.warn("IO Exception while analyzing " + file
              + " using element plugin " + plugin);
        }
      }
    }

    // Check the plugins
    if (candidates.size() == 0)
      throw new BundleException("No suitable element builder plugin found for "
          + file);
    candidates = filterPreferred(candidates);
    if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (BundleElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      log_
          .warn("More than one element builder plugin with the same priority claims responsibilty for "
              + file + ": " + buf.toString());
    }

    // Create bundle element depending on mime type flavor
    BundleElementBuilderPlugin builderPlugin = candidates.get(0);
    BundleElement element = builderPlugin.elementFromFile(file);
    builderPlugin.cleanup();
    return element;
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilder#elementFromManifest(Node,
   *      File, boolean)
   */
  public BundleElement elementFromManifest(Node node, File bundleRoot,
      boolean verify) throws BundleException {
    List<BundleElementBuilderPlugin> candidates = new ArrayList<BundleElementBuilderPlugin>();
    for (Class<? extends BundleElementBuilderPlugin> pluginClass : plugins) {
      BundleElementBuilderPlugin plugin = createPlugin(pluginClass);
      if (plugin.accept(node)) {
        candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0) {
      log_.warn("No element builder found for node of type "
          + node.getNodeName());
      return null;
    } else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (BundleElementBuilderPlugin plugin : candidates) {
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
      log_
          .warn("More than one element builder plugin claims responsability for "
              + name + " of flavor " + elementFlavor + ": " + buf.toString());
    }

    // Create a new bundle element
    BundleElementBuilderPlugin builderPlugin = candidates.get(0);
    BundleElement element = builderPlugin.elementFromManifest(node, bundleRoot,
        verify);
    builderPlugin.cleanup();
    return element;
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilder#newElement(BundleElement.Type,
   *      org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public BundleElement newElement(BundleElement.Type type,
      BundleElementFlavor flavor) throws IOException {
    List<BundleElementBuilderPlugin> candidates = new ArrayList<BundleElementBuilderPlugin>();
    for (Class<? extends BundleElementBuilderPlugin> pluginClass : plugins) {
      BundleElementBuilderPlugin plugin = createPlugin(pluginClass);
      if (plugin.accept(type, flavor)) {
        candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0)
      return null;
    else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (BundleElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      log_
          .warn("More than one element builder plugin claims responsibilty for "
              + flavor + ": " + buf.toString());
    }

    // Create a new bundle element
    BundleElementBuilderPlugin builderPlugin = candidates.get(0);
    BundleElement element = builderPlugin.newElement(type, flavor);
    builderPlugin.cleanup();
    return element;
  }

  /** Creates and initializes a new builder plugin. */
  private BundleElementBuilderPlugin createPlugin(
      Class<? extends BundleElementBuilderPlugin> clazz) {
    BundleElementBuilderPlugin plugin = null;
    try {
      plugin = clazz.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(
          "Cannot instantiate bundle element builder plugin of type "
              + clazz.getName()
              + ". Did you provide a parameterless constructor?", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    try {
      plugin.setup();
    } catch (Exception e) {
      throw new RuntimeException(
          "An error occured while setting up bundle element builder plugin "
              + plugin);
    }
    return plugin;
  }

  /** Gets the preferred plugins. */
  private List<BundleElementBuilderPlugin> filterPreferred(
      List<BundleElementBuilderPlugin> plugins) {
    if (plugins.size() > 0) {
      List<BundleElementBuilderPlugin> preferred = new ArrayList<BundleElementBuilderPlugin>();
      Collections.sort(plugins, PriorityComparator.INSTANCE);
      int priority = plugins.get(0).getPriority();
      for (BundleElementBuilderPlugin plugin : plugins) {
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

  private static final class PriorityComparator implements
      Comparator<BundleElementBuilderPlugin> {

    static final PriorityComparator INSTANCE = new PriorityComparator();

    public int compare(BundleElementBuilderPlugin o1,
        BundleElementBuilderPlugin o2) {
      return o2.getPriority() - o1.getPriority();
    }
  }
}