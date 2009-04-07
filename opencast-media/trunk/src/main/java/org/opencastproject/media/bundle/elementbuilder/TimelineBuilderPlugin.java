/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.bundle.elementbuilder;

import org.opencastproject.media.bundle.BundleElement;
import org.opencastproject.media.bundle.BundleElementBuilderPlugin;
import org.opencastproject.media.bundle.BundleElementFlavor;
import org.opencastproject.media.bundle.BundleException;
import org.opencastproject.media.bundle.Timeline;
import org.opencastproject.media.bundle.TimelineImpl;
import org.opencastproject.media.bundle.BundleElement.Type;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link BundleElementBuilderPlugin} recognises the
 * replay timeline file format and provides the functionality of reading it on
 * behalf of the bundle.
 * <p>
 * The test currently depends on the filename and mimetype only.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class TimelineBuilderPlugin extends AbstractElementBuilderPlugin
    implements BundleElementBuilderPlugin {

  /** The mime type identifier */
  private static String[] MIME_TYPES = { "text/xml" };

  /** Timeline mime type flavor */
  private static final String FLAVOR_DESCRIPTION = "Bundle Timeline";

  /** Timeline mime type */
  private static MimeType[] mimeTypes = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory
      .getLogger(TimelineBuilderPlugin.class);

  public TimelineBuilderPlugin() {
    setPriority(0);
  }

  /**
   * This method does the setup of mime types that is required for the plugin.
   * 
   * @see org.opencastproject.media.bundle.elementbuilder.AbstractElementBuilderPlugin#setup()
   */
  @Override
  public void setup() throws Exception {
    super.setup();
    if (mimeTypes == null) {
      List<MimeType> types = new ArrayList<MimeType>();
      for (String m : MIME_TYPES) {
        try {
          MimeType mimeType = MimeTypes.parseMimeType(m);
          mimeType.setFlavor(Timeline.FLAVOR.getSubtype(), FLAVOR_DESCRIPTION);
          types.add(mimeType);
          log_.debug("Building of timelines enabled");
        } catch (Exception e) {
          log_.warn("Unable to create bundle timelines: mimetype " + m
              + " is not supported");
        }
      }
      mimeTypes = types.toArray(new MimeType[types.size()]);
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#newElement(org.opencastproject.media.bundle.BundleElement.Type,org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public BundleElement newElement(BundleElement.Type type,
      BundleElementFlavor flavor) throws IOException {
    try {
      return TimelineImpl.newInstance();
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException("Unable to calculate checksum", e);
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("XML files are not supported", e);
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#accept(org.opencastproject.media.bundle.BundleElement.Type,
   *      org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public boolean accept(Type type, BundleElementFlavor flavor) {
    return type.equals(Type.Catalog) && flavor.equals(Timeline.FLAVOR);
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    try {
      String name = elementNode.getNodeName();
      String flavor = xpath.evaluate("@type", elementNode);
      return name.equals(BundleElement.Type.Catalog.toString())
          && flavor.equals(Timeline.FLAVOR.toString());
    } catch (XPathExpressionException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#accept(File,
   *      Type, BundleElementFlavor)
   */
  public boolean accept(File file, BundleElement.Type type,
      BundleElementFlavor flavor) throws IOException {
    try {
      // Check type and flavor
      if (type != null && flavor != null)
        return type.equals(Timeline.TYPE) && flavor.equals(Timeline.FLAVOR);
      else if (type != null && !type.equals(Timeline.TYPE))
        return false;
      else if (flavor != null && !flavor.equals(Timeline.FLAVOR))
        return false;
      // Type and flavor are unspecified. Let's try filename and mimetype
      return checkFilename(file, Timeline.FILENAME)
          && checkMimeType(file, mimeTypes);
    } catch (UnknownFileTypeException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node,
   *      java.io.File, boolean)
   */
  public BundleElement elementFromManifest(Node elementNode, File bundleRoot,
      boolean verify) throws BundleException {
    String catalogId = null;
    String catalogPath = null;
    try {
      // id
      catalogId = (String) xpath.evaluate("@id", elementNode,
          XPathConstants.STRING);

      // file
      catalogPath = xpath.evaluate("File/text()", elementNode);
      catalogPath = PathSupport.concat(bundleRoot.getAbsolutePath(),
          catalogPath);

      // create the catalog
      TimelineImpl timeline = TimelineImpl.fromFile(new File(catalogPath));
      if (catalogId != null && !catalogId.equals(""))
        timeline.setIdentifier(catalogId);

      // checksum
      String checksumValue = (String) xpath.evaluate("Checksum/text()",
          elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("Checksum/@type",
          elementNode, XPathConstants.STRING);
      Checksum checksum = Checksum.create(checksumType, checksumValue);

      // verify the catalog
      if (verify) {
        log_.debug("Verifying integrity of timeline " + catalogPath);
        verifyFileIntegrity(new File(catalogPath), checksum);
      }

      // description
      String description = xpath.evaluate("Description/text()", elementNode);
      if (description != null && !description.equals(""))
        timeline.setElementDescription(description);

      return timeline;
    } catch (XPathExpressionException e) {
      throw new BundleException(
          "Error while reading catalog information from manifest: "
              + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException(
          "Unable to calculate checksum for dublin core catalog " + catalogPath
              + ": " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("XML files are not supported: "
          + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new BundleException(
          "Unable to create parser for dublin core catalog " + catalogPath
              + ": " + e.getMessage());
    } catch (IOException e) {
      throw new BundleException("Error while reading dublin core file "
          + catalogPath + ": " + e.getMessage());
    } catch (SAXException e) {
      throw new BundleException("Error while parsing dublin core catalog "
          + catalogPath + ": " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#elementFromFile(File)
   */
  public BundleElement elementFromFile(File file) throws BundleException {
    try {
      log_.trace("Creating timeline from " + file);
      return TimelineImpl.fromFile(file);
    } catch (IOException e) {
      throw new BundleException("Error reading timeline from " + file + " : "
          + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new BundleException("Timeline " + file
          + " has an unknown file type: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new BundleException(
          "Parser configuration exception while reading timeline from " + file
              + " : " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new BundleException("Timeline " + file + " cannot be checksummed: "
          + e.getMessage());
    } catch (SAXException e) {
      throw new BundleException("Error parsing timeline " + file + " : "
          + e.getMessage());
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Timeline Builder Plugin";
  }

}