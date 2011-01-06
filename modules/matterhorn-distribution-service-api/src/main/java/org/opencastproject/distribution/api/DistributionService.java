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

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;

/**
 * Distributes elements from MediaPackages to distribution channels.
 */
public interface DistributionService extends JobProducer {

  /**
   * A prefix used by distribution service implementations to indicate the types of distribution channels they manage.
   */
  String JOB_TYPE_PREFIX = "org.opencastproject.distribution.";

  /** The operation name for distributing content */
  String DISTRIBUTE = "distribute";

  /** The operation name for retracting content */
  String RETRACT = "retract";

  /**
   * Distribute the elementIds from a media package.
   * 
   * @param mediaPackageId
   *          The media package to distribute
   * @param element
   *          The element in the media package to distribute
   * @param block
   *          <code>true</code> to wait for this method to finish
   * @return The job
   * @throws DistributionException
   *           if there was a problem distributing the media
   * @throws MediaPackageException
   *           if there was a problem with the mediapackage element
   */
  Job distribute(String mediaPackageId, MediaPackageElement element, boolean block) throws DistributionException, MediaPackageException;

  /**
   * Retract all media and metadata associated with this media package from the distribution channel.
   * 
   * @param mediaPackageId
   *          The identifier of the media package to retract
   * @param block
   *          <code>true</code> to wait for this method to finish
   * @throws DistributionException
   *           if there was a problem retracting the mediapackage
   */
  Job retract(String mediaPackageId, boolean block) throws DistributionException;

}
