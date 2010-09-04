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
package org.opencastproject.distribution.api;

import org.opencastproject.mediapackage.MediaPackage;

/**
 * Distributes elements from {@link MediaPackage}s to distribution channels.
 */
public interface DistributionService {
  /**
   * A prefix used by distribution service implementations to indicate the types of distribution channels they manage.
   */
  public static final String JOB_TYPE_PREFIX = "org.opencastproject.distribution.";

  /**
   * Distribute the elementIds from a media package.
   * 
   * @param mediaPackage The media package to distribute
   * @param elementIds The elements in the media package to include in this distribution
   * @return The augmented media package, typically including all of the source media package's elements along with new
   * elements pointing to the distributed media. 
   * @throws DistributionException if there was a problem distributing the media
   */
  MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) throws DistributionException;

  /**
   * Retract all media and metadata associated with this media package from the distribution channel.
   * 
   * @param mediaPackageId The identifier of the media package to retract
   * @throws DistributionException if there was a problem retracting the mediapackage
   */
  void retract(String mediaPackageId) throws DistributionException;

}

