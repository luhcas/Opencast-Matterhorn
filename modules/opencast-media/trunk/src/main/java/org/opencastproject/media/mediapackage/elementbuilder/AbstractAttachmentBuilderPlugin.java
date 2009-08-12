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

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.AttachmentImpl;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes attachments and provides utility
 * methods for creating media package element representations for them.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: AbstractAttachmentBuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public abstract class AbstractAttachmentBuilderPlugin extends AbstractElementBuilderPlugin {

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(AbstractAttachmentBuilderPlugin.class);

  /** The candidate type */
  protected MediaPackageElement.Type type = MediaPackageElement.Type.Attachment;

  /** The flavor to look for */
  protected MediaPackageElementFlavor flavor = null;

  /**
   * Creates a new attachment plugin builder that will accept attachments with any flavor.
   */
  public AbstractAttachmentBuilderPlugin() {
    this(null);
  }

  /**
   * Creates a new attachment plugin builder that will accept attachments with the given flavor.
   * 
   * @param flavor
   *          the attachment flavor
   */
  public AbstractAttachmentBuilderPlugin(MediaPackageElementFlavor flavor) {
    this.flavor = flavor;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(java.io.File,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(File file, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws IOException {
    if (mimeTypes != null && mimeTypes.size() > 0)
      try {
        if (!checkMimeType(file))
          return false;
      } catch (UnknownFileTypeException e) {
        return false;
      }
    return accept(type, flavor);
  }

  /**
   * This implementation of <code>accept</code> tests for the element type (attachment).
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    if (this.flavor != null && !this.flavor.equals(flavor))
      return false;
    return type == null || MediaPackageElement.Type.Attachment.equals(type);
  }

  /**
   * This implementation of <code>accept</code> tests for the correct node type (attachment).
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    try {
      // Test for attachment
      String nodeName = elementNode.getNodeName();
      if (!MediaPackageElement.Type.Attachment.toString().equals(nodeName))
        return false;
      // Check flavor
      if (this.flavor != null) {
        String nodeFlavor = (String) xpath.evaluate("@type", elementNode, XPathConstants.STRING);
        if (!flavor.eq(nodeFlavor))
          return false;
      }
      // Check mime type
      if (mimeTypes != null && mimeTypes.size() > 0) {
        String nodeMimeType = (String) xpath.evaluate("MimeType", elementNode, XPathConstants.STRING);
        try {
          MimeType mimeType = MimeTypes.parseMimeType(nodeMimeType);
          if (!mimeTypes.contains(mimeType))
            return false;
        } catch (UnknownFileTypeException e) {
          return false;
        }
      }

      return true;
    } catch (XPathExpressionException e) {
      log_.warn("Error while reading attachment flavor from manifest: " + e.getMessage());
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    throw new UnsupportedOperationException("Creation of new attachments from scratch is unsupported");
  }

  /**
   * Utility method that returns an attachment object from the given manifest node.
   * 
   * @param elementNode
   *          the attachment node in the manifest
   * @param packageRoot
   *          media package root in the filesystem
   * @param verify
   *          <code>true</code> to verify the file
   * @return the attachment object
   * @throws MediaPackageException
   *           if the attachment cannot be read
   */
  public MediaPackageElement elementFromManifest(Node elementNode, File packageRoot, boolean verify)
          throws MediaPackageException {
    String attachmentId = null;
    String attachmentFlavor = null;
    String reference = null;
    String attachmentPath = null;
    try {
      // id
      attachmentId = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // flavor
      attachmentFlavor = (String) xpath.evaluate("@type", elementNode, XPathConstants.STRING);

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // file
      attachmentPath = xpath.evaluate("File/text()", elementNode).trim();
      attachmentPath = PathSupport.concat(packageRoot.getAbsolutePath(), attachmentPath);

      // create the attachment
      AttachmentImpl attachment = AttachmentImpl.fromFile(new File(attachmentPath));
      if (attachmentId != null && !attachmentId.equals(""))
        attachment.setIdentifier(attachmentId);

      // Add reference
      if (reference != null && !reference.equals(""))
        attachment.referTo(MediaPackageReferenceImpl.fromString(reference));

      // Add type/flavor information
      if (attachmentFlavor != null && !attachmentFlavor.equals("")) {
        try {
          MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(attachmentFlavor);
          attachment.setFlavor(flavor);
        } catch (IllegalArgumentException e) {
          log_.warn("Unable to read attachment flavor: " + e.getMessage());
        }
      }

      // checksum
      String checksumValue = (String) xpath.evaluate("Checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("Checksum/@type", elementNode, XPathConstants.STRING);
      Checksum checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // verify the catalog
      if (verify) {
        log_.debug("Verifying integrity of attachment " + attachmentPath);
        verifyFileIntegrity(new File(attachmentPath), checksum);
      }

      // description
      String description = xpath.evaluate("Description/text()", elementNode);
      if (description != null && !description.equals(""))
        attachment.setElementDescription(description.trim());

      return specializeAttachment(attachment);
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while reading attachment from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("Filetype not supported: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("Error while reading attachment file " + attachmentPath + ": " + e.getMessage());
    }
  }

  /**
   * Utility method that returns an attachment object from the given file.
   * 
   * @param file
   *          the file
   * @return an attachment object
   * @throws MediaPackageException
   *           if the attachment cannto be read
   */
  public MediaPackageElement elementFromFile(File file) throws MediaPackageException {
    try {
      log_.trace("Creating attachment from " + file);
      return specializeAttachment(AttachmentImpl.fromFile(file));
    } catch (IOException e) {
      throw new MediaPackageException("Error reading dublin core from " + file + " : " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Dublin core metadata " + file + " has an unknown file type: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Dublin core document " + file + " cannot be checksummed: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilder#elementFromFile(java.io.File,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement elementFromFile(File file, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws MediaPackageException {
    return elementFromFile(file);
  }

  /**
   * Overwrite this method in order to return a specialization of the attachment. This implementation just returns the
   * attachment that is was given.
   * 
   * @param attachment
   *          the general attachment representation
   * @return a specialized attachment
   * @throws MediaPackageException
   *           if the media package fails to be specialized
   */
  protected Attachment specializeAttachment(Attachment attachment) throws MediaPackageException {
    return attachment;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Attachment Builder Plugin";
  }

}