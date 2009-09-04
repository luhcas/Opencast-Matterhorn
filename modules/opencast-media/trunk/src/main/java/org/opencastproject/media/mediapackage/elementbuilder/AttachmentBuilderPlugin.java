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
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;

import org.w3c.dom.Node;

import java.net.URL;

import javax.xml.xpath.XPathExpressionException;

/**
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: AttachmentBuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public class AttachmentBuilderPlugin extends AbstractAttachmentBuilderPlugin implements MediaPackageElementBuilder {

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.AbstractAttachmentBuilderPlugin#accept(java.io.File,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  @Override
  public boolean accept(URL url, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    if (type != null && flavor != null) {
      if (!type.equals(MediaPackageElement.Type.Attachment))
        return false;
    } else if (type != null && !type.equals(MediaPackageElement.Type.Attachment)) {
      return false;
    } else if (flavor != null && !flavor.equals(Attachment.FLAVOR)) {
      return false;
    }
    return super.accept(url, type, flavor);
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.AbstractAttachmentBuilderPlugin#accept(org.w3c.dom.Node)
   */
  @Override
  public boolean accept(Node elementNode) {
    try {
      String flavor = xpath.evaluate("@type", elementNode);
      if (flavor != null && !"".equals(flavor) && !Attachment.FLAVOR.eq(flavor))
        return false;
      return super.accept(elementNode);
    } catch (XPathExpressionException e) {
      return false;
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Attachment Builder Plugin";
  }

}