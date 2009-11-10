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

package org.opencastproject.media.mediapackage.attachment;

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Cover;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;

import java.net.URL;

/**
 * This is a specialized implementation for a media package cover.
 * 
 * @author Tobias Wunden
 * @version $Id
 */

public class CoverImpl extends AttachmentImpl implements Cover {

  /** Serial Version UID */
  private static final long serialVersionUID = -7420968157749682999L;

  /**
   * Creates a cover attachment.
   * 
   * @parm identifier the cover identifier
   * @param url
   *          the cover location
   * @param size
   *          the cover size in bytes
   * @param checksum
   *          the cover checksum
   * @param mimeType
   *          the cover mime type
   */
  protected CoverImpl(String identifier, URL url, long size, Checksum checksum, MimeType mimeType) {
    super(identifier, Cover.FLAVOR, url, size, checksum, mimeType);
  }

  /**
   * Creates a cover attachment.
   * 
   * @param url
   *          the covers location
   * @param size
   *          the covers size in bytes
   * @param checksum
   *          the covers checksum
   * @param mimeType
   *          the covers mime type
   */
  protected CoverImpl(URL url, long size, Checksum checksum, MimeType mimeType) {
    super(Cover.FLAVOR, url, size, checksum, mimeType);
  }

  /**
   * Creates a cover attachment.
   * 
   * @param url
   *          the covers location
   */
  protected CoverImpl(URL url) {
    this(null, url, -1, null, null);
  }

  /**
   * Dresses the attachment as a {@link Cover}.
   * 
   * @param attachment
   *          the general attachment representation
   */
  public static CoverImpl fromAttachment(Attachment attachment) {
    return new CoverImpl(attachment.getIdentifier(), attachment.getURL(), attachment.getSize(), attachment
            .getChecksum(), attachment.getMimeType());
  }

  /**
   * Creates a new {@link Cover} object.
   * 
   * @param url
   *          the cover location
   * @param size
   *          the covers size in bytes
   * @param checksum
   *          the file checksum
   * @param mimeType
   *          the mime type
   * @return the cover object
   */
  public static CoverImpl fromURL(URL url) {
    return new CoverImpl(url);
  }

  /**
   * @see org.opencastproject.media.mediapackage.attachment.AttachmentImpl#toString()
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer("cover");
    if (getMimeType() != null) {
      buf.append(" (");
      buf.append(getMimeType());
      buf.append(")");
    }
    return buf.toString();
  }

}
