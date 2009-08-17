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

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl;
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

  /** The mime type identifier */
  private static String[] MIME_TYPES = { "text/xml" };

  /** Dublin core metadata mime type flavor */
  private static final String FLAVOR_DESCRIPTION = "Dublin Core Metadata";

  /** Dublin core metadata mime type */
  private static MimeType[] mimeTypes = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(DublinCoreBuilderPlugin.class);

  public DublinCoreBuilderPlugin() {
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
          mimeType.setFlavor(MediaPackageElements.DUBLINCORE_CATALOG.getSubtype(), FLAVOR_DESCRIPTION);
          types.add(mimeType);
          log_.debug("Building of dublin core catalogs enabled");
        } catch (Exception e) {
          log_.warn("Unable to create dublin core documents: mimetype " + m + " is not supported");
        }
      }
      mimeTypes = types.toArray(new MimeType[types.size()]);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws IOException {
    try {
      return DublinCoreCatalogImpl.newInstance();
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException("Unable to calculate checksum", e);
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("XML files are not supported", e);
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
        return type.equals(Catalog.TYPE) && flavor.equals(MediaPackageElements.DUBLINCORE_CATALOG);
      else if (type != null && !type.equals(Catalog.TYPE))
        return false;
      else if (flavor != null && !flavor.equals(MediaPackageElements.DUBLINCORE_CATALOG))
        return false;

      // Check mime type
      if (!checkMimeType(file, mimeTypes))
        return false;

      // Still uncertain. Let's try to read the catalog
      DublinCoreCatalogImpl.fromFile(file);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (UnknownFileTypeException e) {
      return false;
    } catch (NoSuchAlgorithmException e) {
      return false;
    } catch (ParserConfigurationException e) {
      return false;
    } catch (SAXException e) {
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
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromManifest(org.w3c.dom.Node, File,
   *      boolean)
   */
  public MediaPackageElement elementFromManifest(Node elementNode, File packageRoot, boolean verify)
          throws MediaPackageException {
    String catalogId = null;
    String catalogPath = null;
    String reference = null;
    try {
      // id
      catalogId = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // file
      catalogPath = xpath.evaluate("url/text()", elementNode).trim();
      catalogPath = PathSupport.concat(packageRoot.getAbsolutePath(), catalogPath);

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // create the catalog
      DublinCoreCatalog dc = DublinCoreCatalogImpl.fromFile(new File(catalogPath));
      if (catalogId != null && !catalogId.equals(""))
        dc.setIdentifier(catalogId);

      // Add reference
      if (reference != null && !reference.equals(""))
        dc.referTo(MediaPackageReferenceImpl.fromString(reference));

      // checksum
      String checksumValue = (String) xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING);
      Checksum checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // verify the catalog
      if (verify) {
        log_.debug("Verifying integrity of dublin core catalog " + catalogPath);
        verifyFileIntegrity(new File(catalogPath), checksum);
      }
      return dc;
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while reading catalog information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
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
      log_.trace("Creating dublin core metadata container from " + file);
      return DublinCoreCatalogImpl.fromFile(file);
    } catch (IOException e) {
      throw new MediaPackageException("Error reading dublin core from " + file + " : " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Dublin core metadata " + file + " has an unknown file type: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Parser configuration exception while reading dublin core catalog from " + file
              + " : " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Dublin core document " + file + " cannot be checksummed: " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error parsing dublin core catalog " + file + " : " + e.getMessage());
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