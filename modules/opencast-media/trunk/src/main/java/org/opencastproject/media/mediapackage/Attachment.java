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

package org.opencastproject.media.mediapackage;

/**
 * This interface describes methods and fields for attachments as part of a media package.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: Attachment.java 2905 2009-07-15 16:16:05Z ced $
 */
public interface Attachment extends MediaPackageElement {

  /** Media package element type */
  Type TYPE = Type.Attachment;

  /** Element flavor definition */
  MediaPackageElementFlavor FLAVOR = new MediaPackageElementFlavor("attachment", "(unkown)", "Unspecified attachment");
}