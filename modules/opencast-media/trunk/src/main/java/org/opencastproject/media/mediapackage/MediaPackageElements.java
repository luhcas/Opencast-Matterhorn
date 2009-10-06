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
 * Contains all well-known definitions, names and symbols REPLAY relies on as constants for an easy usage and as a
 * documentation.
 * <p/>
 * todo This class (not the definitions!) may be seen as an implementation detail and therefore should reside in the
 * common module. This issue is to be discussed.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public interface MediaPackageElements {

  /** The MPEG-7 filename */
  String MPEG7_FILENAME = "mpeg-7.xml";

  /** The manifest file name */
  String MANIFEST_FILENAME = "index.xml";

  /** Element type definition */
  MediaPackageElementFlavor DUBLINCORE_CATALOG = new MediaPackageElementFlavor("metadata", "dublincore");

  /** Track containing the presenter/s */
  MediaPackageElementFlavor PRESENTER_TRACK = new MediaPackageElementFlavor("track", "presenter");

  /** Track containing presentational material */
  MediaPackageElementFlavor PRESENTATION_TRACK = new MediaPackageElementFlavor("track", "presentation");

  /** Track capturing the audience */
  MediaPackageElementFlavor AUDIENCE_TRACK = new MediaPackageElementFlavor("track", "audience");

  /** Track capturing the contents of a document camera */
  MediaPackageElementFlavor DOCUMENTS_TRACK = new MediaPackageElementFlavor("track", "documents");

  /** Track without any known semantics */
  MediaPackageElementFlavor INDEFINITE_TRACK = new MediaPackageElementFlavor("track", "indefinite");

}