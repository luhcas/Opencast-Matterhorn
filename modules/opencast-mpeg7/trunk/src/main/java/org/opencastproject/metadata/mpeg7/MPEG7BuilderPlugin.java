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

package org.opencastproject.metadata.mpeg7;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.MediaPackageSerializer;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.elementbuilder.AbstractElementBuilderPlugin;
import org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes the mpeg-7 file format and provides
 * the functionality of reading it on behalf of the media package.
 * <p>
 * The test currently depends on the filename and mimetype only.
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
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Catalog) && flavor.equals(Mpeg7Catalog.FLAVOR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
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
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(URI,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
     return type != null && type.equals(Catalog.TYPE) && flavor != null && flavor.equals(Mpeg7Catalog.FLAVOR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromURI(URI)
   */
  public MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException {
    log_.trace("Creating mpeg-7 metadata container from " + uri);
    return Mpeg7CatalogImpl.fromURI(uri);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MPEG-7 Catalog Builder Plugin";
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node,
   *      org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer)
          throws UnsupportedElementException {

    String id = null;
    String reference = null;
    URI url = null;
    long size = -1;
    Checksum checksum = null;
    MimeType mimeType = null;

    try {
      // id
      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // url
      url = serializer.resolvePath(xpath.evaluate("url/text()", elementNode).trim());

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
      if (checksumValue != null && !checksumValue.equals("") && checksumType != null)
        checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // mimetype
      String mimeTypeValue = (String) xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING);
      if (mimeTypeValue != null && !mimeTypeValue.equals(""))
        mimeType = MimeTypes.parseMimeType(mimeTypeValue);

      // create the catalog
      Mpeg7Parser mpeg7Parser = new Mpeg7Parser();
      Mpeg7CatalogImpl mpeg7 = mpeg7Parser.parse(url.toURL().openStream());
      if (id != null && !id.equals(""))
        mpeg7.setIdentifier(id);

      // Add url
      mpeg7.setURI(url);

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

      // Set the tags
      NodeList tagNodes = (NodeList) xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET);
      for (int i = 0; i < tagNodes.getLength(); i++) {
        mpeg7.addTag(tagNodes.item(i).getTextContent());
      }

      return mpeg7;
    } catch (XPathExpressionException e) {
      throw new UnsupportedElementException("Error while reading catalog information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedElementException("Unsupported digest algorithm: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new UnsupportedElementException("Unable to create parser for mpeg-7 catalog " + url + ": " + e.getMessage());
    } catch (IOException e) {
      throw new UnsupportedElementException("Error while reading mpeg-7 catalog " + url + ": " + e.getMessage());
    } catch (SAXException e) {
      throw new UnsupportedElementException("Error while parsing mpeg-7 catalog " + url + ": " + e.getMessage());
    } catch (URISyntaxException e) {
      throw new UnsupportedElementException("Error while reading mpeg-7 catalog " + url + ": " + e.getMessage());
    }
  }

}
