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

package org.opencastproject.media.mediapackage.elementbuilder;

import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Timeline;
import org.opencastproject.media.mediapackage.TimelineImpl;
import org.opencastproject.media.mediapackage.MediaPackageElements;
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
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognises the matterhorn timeline file format
 * and provides the functionality of reading it on behalf of the media package.
 * <p>
 * The test currently depends on the filename and mimetype only.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: TimelineBuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public class TimelineBuilderPlugin extends AbstractElementBuilderPlugin implements MediaPackageElementBuilderPlugin {

  /** The mime type identifier */
  private static String[] MIME_TYPES = { "text/xml" };

  /** Timeline mime type flavor */
  private static final String FLAVOR_DESCRIPTION = "Media package Timeline";

  /** Timeline mime type */
  private static MimeType[] mimeTypes = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(TimelineBuilderPlugin.class.getName());

  public TimelineBuilderPlugin() {
    setPriority(0);
  }

  /**
   * This method does the setup of mime types that is required for the plugin.
   * 
   * @see org.opencastproject.media.mediapackage.elementbuilder.AbstractElementBuilderPlugin#setup()
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
          log_.warn("Unable to create media package timelines: mimetype " + m + " is not supported");
        }
      }
      mimeTypes = types.toArray(new MimeType[types.size()]);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      ,org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws IOException {
    try {
      return TimelineImpl.newInstance();
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException("Unable to calculate checksum", e);
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("XML files are not supported", e);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Catalog) && flavor.equals(Timeline.FLAVOR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    try {
      String name = elementNode.getNodeName();
      String flavor = xpath.evaluate("@type", elementNode);
      return name.equals(MediaPackageElement.Type.Catalog.toString()) && Timeline.FLAVOR.eq(flavor);
    } catch (XPathExpressionException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(File,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type , MediaPackageElementFlavor)
   */
  public boolean accept(File file, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws IOException {
    try {
      // Check type and flavor
      if (type != null && flavor != null)
        return type.equals(Timeline.TYPE) && flavor.equals(Timeline.FLAVOR);
      else if (type != null && !type.equals(Timeline.TYPE))
        return false;
      else if (flavor != null && !flavor.equals(Timeline.FLAVOR))
        return false;
      // Type and flavor are unspecified. Let's try filename and mimetype
      return checkFilename(file, MediaPackageElements.TIMELINE_FILENAME) && checkMimeType(file, mimeTypes);
    } catch (UnknownFileTypeException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node,
   *      java.io.File, boolean)
   */
  public MediaPackageElement elementFromManifest(Node elementNode, File packageRoot, boolean verify)
          throws MediaPackageException {
    String catalogId = null;
    String catalogPath = null;
    try {
      // id
      catalogId = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // file
      catalogPath = xpath.evaluate("File/text()", elementNode);
      catalogPath = PathSupport.concat(packageRoot.getAbsolutePath(), catalogPath);

      // create the catalog
      TimelineImpl timeline = TimelineImpl.fromFile(new File(catalogPath));
      if (catalogId != null && !catalogId.equals(""))
        timeline.setIdentifier(catalogId);

      // checksum
      String checksumValue = (String) xpath.evaluate("Checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("Checksum/@type", elementNode, XPathConstants.STRING);
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
      throw new MediaPackageException("Error while reading catalog information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException("Unable to calculate checksum for dublin core catalog " + catalogPath + ": "
              + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("XML files are not supported: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Unable to create parser for dublin core catalog " + catalogPath + ": "
              + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("Error while reading dublin core file " + catalogPath + ": " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error while parsing dublin core catalog " + catalogPath + ": " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#elementFromFile(File)
   */
  public MediaPackageElement elementFromFile(File file) throws MediaPackageException {
    try {
      log_.trace("Creating timeline from " + file);
      return TimelineImpl.fromFile(file);
    } catch (IOException e) {
      throw new MediaPackageException("Error reading timeline from " + file + " : " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Timeline " + file + " has an unknown file type: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Parser configuration exception while reading timeline from " + file + " : "
              + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Timeline " + file + " cannot be checksummed: " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error parsing timeline " + file + " : " + e.getMessage());
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