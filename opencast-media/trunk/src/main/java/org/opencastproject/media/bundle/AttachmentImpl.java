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

package org.opencastproject.media.bundle;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Basic implementation of an attachment.
 * 
 * @author Tobias wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class AttachmentImpl extends AbstractBundleElement implements Attachment {

  /** Serial version UID */
  private static final long serialVersionUID = 6626531251856698138L;

  /**
   * Creates an attachment.
   * 
   * @param flavor
   *          the attachment type
   * @param file
   *          the file
   * @param checksum
   *          the attachment's checksum
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the attachment's mime type is not supported
   */
  protected AttachmentImpl(BundleElementFlavor flavor, File file,
      Checksum checksum) throws IOException, UnknownFileTypeException {
    super(Type.Attachment, flavor, file, checksum);
  }

  /**
   * Creates an attachment.
   * 
   * @param flavor
   *          the attachment type
   * @param mimeType
   *          the attachment's mime type
   * @param file
   *          the file
   * @param checksum
   *          the attachment's checksum
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   */
  protected AttachmentImpl(BundleElementFlavor flavor, MimeType mimeType,
      File file, Checksum checksum) throws IOException {
    super(Type.Attachment, flavor, mimeType, file, checksum);
  }

  /**
   * Creates an attachment.
   * 
   * @param flavor
   *          the attachment type
   * @param mimeType
   *          the attachment's mime type
   * @param file
   *          the file
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws NoSuchAlgorithmException
   *           if the attachment's checksum cannot be computed
   */
  protected AttachmentImpl(BundleElementFlavor flavor, MimeType mimeType,
      File file) throws IOException, NoSuchAlgorithmException {
    super(Type.Attachment, flavor, mimeType, file);
  }

  /**
   * Creates an attachment.
   * 
   * @param id
   *          the identifier within the bundle
   * @param flavor
   *          the attachment type
   * @param mimeType
   *          the attachment's mime type
   * @param file
   *          the file
   * @param checksum
   *          the attachment's checksum
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   */
  protected AttachmentImpl(String id, BundleElementFlavor flavor,
      MimeType mimeType, File file, Checksum checksum) throws IOException {
    super(id, Type.Attachment, flavor, mimeType, file, checksum);
  }

  /**
   * Creates an attachment object for the given file.
   * 
   * @param flavor
   *          the attachment type
   * @param file
   *          the file
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the attachment's mime type is not supported
   * @throws NoSuchAlgorithmException
   *           if the attachment's checksum cannot be computed
   */
  protected AttachmentImpl(BundleElementFlavor flavor, File file)
      throws IOException, NoSuchAlgorithmException, UnknownFileTypeException {
    super(Type.Attachment, flavor, file);
  }

  /**
   * Reads the attachment from the specified file and returns it.
   * 
   * @param file
   *          the dublin core metadata container file
   * @return the dublin core object
   * @throws IOException
   *           if reading the metadata fails
   * @throws UnknownFileTypeException
   *           if the dublin core file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  public static AttachmentImpl fromFile(File file) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    AttachmentImpl attachment = new AttachmentImpl(Attachment.FLAVOR, file);
    return attachment;
  }

  /**
   * @see org.opencastproject.media.bundle.AbstractBundleElement#toManifest(org.w3c.dom.Document)
   */
  @Override
  public Node toManifest(Document document) {
    Node node = super.toManifest(document);

    // File
    Element fileNode = document.createElement("File");
    String attachmentPath = PathSupport.concat(getFile().getParentFile()
        .getName(), fileName);
    fileNode.appendChild(document.createTextNode(attachmentPath));
    node.appendChild(fileNode);

    return node;
  }

  /**
   * @see org.opencastproject.media.bundle.AbstractBundleElement#toString()
   */
  @Override
  public String toString() {
    return "attachment (" + getMimeType() + ")";
  }

}