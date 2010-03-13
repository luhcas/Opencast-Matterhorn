/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.metadata.api;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageMetadata;

/**
 * TODO: Comment me!
 *
 */
public interface MediaPackageMetadataService {

  /** The static constant used when configuring the priority */
  String PRIORITY_KEY = "priority";

  /**
   * The priority of this MediaPackageMetadataService compared to others when more than one is registered in the system.
   * 
   * When more than one MediaPackageMetadataService is registered, the {@link #getMetadata(Catalog)} method may be
   * called on each service in order of priority.  MediaPackageMetadata objects returned by higher priority
   * MediaPackageMetadataServices should override those returned by lower priority services.
   * 
   * The lowest number is the highest priority (i.e. 1 is a higher priority than 2).
   * 
   * @return The priority
   */
  int getPriority();
  
  /**
   * Gets the {@link MediaPackageMetadata} for a {@link MediaPackage} if possible.  If no metadata can be extracted
   * from the catalogs in the {@link MediaPackage}, this returns null;
   * 
   * @param mediaPackage The mediapackage to inspect for catalogs
   * @return The {@link MediaPackageMetadata} extracted from the media package
   */
  MediaPackageMetadata getMetadata(MediaPackage mediaPackage);
}
