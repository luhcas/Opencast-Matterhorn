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
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.MediaPackageSerializer;
import org.opencastproject.media.mediapackage.Mpeg7Catalog;
import org.opencastproject.media.mediapackage.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.media.mediapackage.mpeg7.Mpeg7Parser;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes the mpeg-7 file format and provides
 * the functionality of reading it on behalf of the media package.
 * <p>
 * The test currently depends on the filename and mimetype only.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MPEG7BuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public class MPEG7BuilderPlugin extends AbstractElementBuilderPlugin implements MediaPackageElementBuilderPlugin {

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(MPEG7BuilderPlugin.class);

  public MPEG7BuilderPlugin() {
    setPriority(0);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws IOException {
    return Mpeg7CatalogImpl.newInstance();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Catalog) && flavor.equals(Mpeg7Catalog.FLAVOR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    try {
      String name = elementNode.getNodeName();
      String flavor = xpath.evaluate("@type", elementNode);
      return name.equalsIgnoreCase(MediaPackageElement.Type.Catalog.toString()) && Mpeg7Catalog.FLAVOR.eq(flavor);
    } catch (XPathExpressionException e) {
      return false;
    }
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
        return type.equals(Catalog.TYPE) && flavor.equals(Mpeg7Catalog.FLAVOR);
      else if (type != null && !type.equals(Catalog.TYPE))
        return false;
      else if (flavor != null && !flavor.equals(Mpeg7Catalog.FLAVOR))
        return false;

      // Still uncertain. Let's try to read the catalog
      Mpeg7Parser parser = new Mpeg7Parser();
      parser.parse(url.openStream());
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
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromURL(java.net.URL)
   */
  public MediaPackageElement elementFromURL(URL url) throws MediaPackageException {
    try {
      log_.trace("Creating mpeg-7 metadata container from " + url);
      return Mpeg7CatalogImpl.fromURL(url);
    } catch (IOException e) {
      throw new MediaPackageException("Error reading mpeg-7 from " + url + " : " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Parser configuration exception while reading mpeg-7 metadata from " + url
              + " : " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error parsing mpeg-7 catalog " + url + " : " + e.getMessage());
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MPEG-7 Catalog Builder Plugin";
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node, org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer) throws MediaPackageException {

    String id = null;
    String reference = null;
    URL url = null;
    long size = -1;
    Checksum checksum = null;
    MimeType mimeType = null;

    try {
      // id
      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // url
      url = serializer.resolve(xpath.evaluate("url/text()", elementNode).trim());

      // size
      try {
        size = Long.parseLong(xpath.evaluate("size/text()", elementNode).trim());
      } catch (Exception e) {
        // size may not be present
      }

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

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
      Mpeg7Parser mpeg7Parser = new Mpeg7Parser();
      Mpeg7CatalogImpl mpeg7 = mpeg7Parser.parse(url.openStream());
      if (id != null && !id.equals(""))
        mpeg7.setIdentifier(id);
      
      // Add url
      mpeg7.setURL(url);

      // Add reference
      if (reference != null && !reference.equals(""))
        mpeg7.referTo(MediaPackageReferenceImpl.fromString(reference));

      // Set size
      if (size > 0)
        mpeg7.setSize(size);

      // Set checksum
      if (checksum != null)
        mpeg7.setChecksum(checksum);

      // Set the mime type
      if (mimeType != null)
        mpeg7.setMimeType(mimeType);

      return mpeg7;
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while reading catalog information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Unable to create parser for mpeg-7 catalog " + url + ": " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("Error while reading mpeg-7 file " + url + ": " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error while parsing mpeg-7 catalog " + url + ": " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Mpeg-7 catalog " + url + " is of unknown mime type: " + e.getMessage());
    }
  }

}