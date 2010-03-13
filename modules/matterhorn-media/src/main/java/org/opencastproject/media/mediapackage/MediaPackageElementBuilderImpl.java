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

package org.opencastproject.media.mediapackage;

import org.opencastproject.media.mediapackage.elementbuilder.AttachmentBuilderPlugin;
import org.opencastproject.media.mediapackage.elementbuilder.CatalogBuilderPlugin;
import org.opencastproject.media.mediapackage.elementbuilder.CoverBuilderPlugin;
import org.opencastproject.media.mediapackage.elementbuilder.TrackBuilderPlugin;
import org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Default implementation for a media package element builder.
 */
public class MediaPackageElementBuilderImpl implements MediaPackageElementBuilder {

  /** The list of plugins */
  private List<Class<? extends MediaPackageElementBuilderPlugin>> plugins = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(MediaPackageElementBuilderImpl.class.getName());

  // Create the list of available element builder pugins
  public MediaPackageElementBuilderImpl() {
    plugins = new ArrayList<Class<? extends MediaPackageElementBuilderPlugin>>();
    plugins.add(AttachmentBuilderPlugin.class);
    plugins.add(CatalogBuilderPlugin.class);
    plugins.add(CoverBuilderPlugin.class);
    plugins.add(TrackBuilderPlugin.class);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromURI(URI)
   */
  public MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException {
    return elementFromURI(uri, null, null);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromURI(URI,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement elementFromURI(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws UnsupportedElementException {

    // Feed the file to the element builder plugins
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    {
      MediaPackageElementBuilderPlugin plugin = null;
      for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
        plugin = createPlugin(pluginClass);
        if (plugin.accept(uri, type, flavor))
          candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0) {
      throw new UnsupportedElementException("No suitable element builder plugin found for " + uri);
    } else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (MediaPackageElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      log_.debug("More than one element builder plugin with the same priority claims responsibilty for " + uri + ": "
              + buf.toString());
    }

    // Create media package element depending on mime type flavor
    Collections.sort(candidates, PriorityComparator.INSTANCE);
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.elementFromURI(uri);
    element.setFlavor(flavor);
    builderPlugin.cleanup();
    return element;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromManifest(org.w3c.dom.Node,
   *      org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public MediaPackageElement elementFromManifest(Node node, MediaPackageSerializer serializer)
          throws UnsupportedElementException {
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
      MediaPackageElementBuilderPlugin plugin = createPlugin(pluginClass);
      if (plugin.accept(node)) {
        candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0) {
      throw new UnsupportedElementException("No suitable element builder plugin found for node " + node.getNodeName());
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
      log_.debug("More than one element builder plugin claims responsability for " + name + " of flavor "
              + elementFlavor + ": " + buf.toString());
    }

    // Create a new media package element
    Collections.sort(candidates, PriorityComparator.INSTANCE);
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.elementFromManifest(node, serializer);
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
      log_.debug("More than one element builder plugin claims responsibilty for " + flavor + ": " + buf.toString());
    }

    // Create a new media package element
    Collections.sort(candidates, PriorityComparator.INSTANCE);
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.newElement(type, flavor);
    builderPlugin.cleanup();
    return element;
  }

  /**
   * Creates and initializes a new builder plugin.
   */
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

  /**
   * Comperator used to sort plugins by priority.
   */
  private static final class PriorityComparator implements Comparator<MediaPackageElementBuilderPlugin> {

    static final PriorityComparator INSTANCE = new PriorityComparator();

    public int compare(MediaPackageElementBuilderPlugin o1, MediaPackageElementBuilderPlugin o2) {
      return o2.getPriority() - o1.getPriority();
    }

  }

}
