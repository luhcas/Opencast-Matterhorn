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

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.MediaPackageSerializer;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognises the dublin core file format and
 * provides the functionality of reading it on behalf of the media package.
 * <p>
 * The test currently depends on the filename and mimetype only.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: DublinCoreBuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public class DublinCoreBuilderPlugin extends AbstractElementBuilderPlugin implements MediaPackageElementBuilderPlugin {

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(DublinCoreBuilderPlugin.class);

  public DublinCoreBuilderPlugin() {
    setPriority(0);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws IOException {
    return DublinCoreCatalogImpl.newInstance();
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(java.net.URL,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(URL url, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    try {
      // Check type and flavor
      if (type != null && flavor != null)
        return type.equals(Catalog.TYPE) && flavor.equals(MediaPackageElements.DUBLINCORE_CATALOG);
      else if (type != null && !type.equals(Catalog.TYPE))
        return false;
      else if (flavor != null && !flavor.equals(MediaPackageElements.DUBLINCORE_CATALOG))
        return false;

      // Still uncertain. Let's try to read the catalog
      DublinCoreCatalogImpl.fromURL(url);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (ParserConfigurationException e) {
      return false;
    } catch (SAXException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Catalog) && flavor.equals(MediaPackageElements.DUBLINCORE_CATALOG);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    try {
      String name = elementNode.getNodeName();
      String flavor = xpath.evaluate("@type", elementNode);
      return name.equalsIgnoreCase(MediaPackageElement.Type.Catalog.toString())
              && MediaPackageElements.DUBLINCORE_CATALOG.eq(flavor);
    } catch (XPathExpressionException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node, org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer) throws MediaPackageException {

    String id = null;
    URL url = null;
    long size = -1;
    Checksum checksum = null;
    MimeType mimeType = null;
    String reference = null;

    try {
      // id
      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // url
      url = serializer.resolve(xpath.evaluate("url/text()", elementNode).trim());

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // size
      try {
        size = Long.parseLong(xpath.evaluate("size/text()", elementNode).trim());
      } catch (Exception e) {
        // size may not be present
      }

      // checksum
      String checksumValue = (String) xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING);
      if (checksumValue != null && checksumType != null)
        checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // mimetype
      String mimeTypeValue = (String) xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING);
      if (mimeTypeValue != null)
        mimeType = MimeTypes.parseMimeType(mimeTypeValue);

      // create the catalog
      DublinCoreCatalogImpl dc = DublinCoreCatalogImpl.fromURL(url);
      if (id != null && !id.equals(""))
        dc.setIdentifier(id);

      // Add url
      dc.setURL(url);

      // Add reference
      if (reference != null && !reference.equals(""))
        dc.referTo(MediaPackageReferenceImpl.fromString(reference));

      // Set size
      if (size > 0)
        dc.setSize(size);

      // Set checksum
      if (checksum != null)
        dc.setChecksum(checksum);

      if (mimeType != null)
        dc.setMimeType(mimeType);

      return dc;
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while reading catalog information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Unable to create parser for dublin core catalog " + url + ": " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("Error while reading dublin core file " + url + ": " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error while parsing dublin core catalog " + url + ": " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Dublin core catalog " + url + " is of unknown mime type: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromURL(java.net.URL)
   */
  public MediaPackageElement elementFromURL(URL url) throws MediaPackageException {
    try {
      log_.trace("Creating dublin core metadata container from " + url);
      return DublinCoreCatalogImpl.fromURL(url);
    } catch (IOException e) {
      throw new MediaPackageException("Error reading dublin core from " + url + " : " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Parser configuration exception while reading dublin core catalog from " + url
              + " : " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error parsing dublin core catalog " + url + " : " + e.getMessage());
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Dublin Core Catalog Builder Plugin";
  }

}