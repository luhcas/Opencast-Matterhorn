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
import org.opencastproject.media.mediapackage.AttachmentImpl;
import org.opencastproject.media.mediapackage.Cover;
import org.opencastproject.util.UnknownFileTypeException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

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
   * Creates a new cover attachment.
   * 
   * @param attachment
   *          the general attachment representation
   * @throws Exception
   *           throw an exception if the document cannot be read
   */
  public CoverImpl(Attachment attachment) throws Exception {
    super(attachment.getIdentifier(), Cover.FLAVOR, attachment.getMimeType(), attachment.getFile(), attachment
            .getChecksum());
  }

  /**
   * Creates a new cover object for the given file.
   * 
   * @param cover
   *          the file
   * @throws IOException
   *           if the track file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the track's checksum cannot be computed
   */
  protected CoverImpl(File cover) throws IOException, UnknownFileTypeException, NoSuchAlgorithmException {
    super(Cover.FLAVOR, cover);
  }

  /**
   * Reads a cover from the specified file and returns it encapsulated in a {@link Cover} object.
   * 
   * @param file
   *          the track file
   * @return the cover object
   * @throws IOException
   *           if reading the manifest file fails
   * @throws UnknownFileTypeException
   *           if the manifest file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  public static CoverImpl fromFile(File file) throws IOException, UnknownFileTypeException, NoSuchAlgorithmException {
    return new CoverImpl(file);
  }

  /**
   * @see org.opencastproject.media.mediapackage.AttachmentImpl#toString()
   */
  @Override
  public String toString() {
    return "Cover (" + getMimeType() + ")";
  }

}