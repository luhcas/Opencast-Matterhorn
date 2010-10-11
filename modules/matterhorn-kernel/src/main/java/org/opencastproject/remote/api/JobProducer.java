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
package org.opencastproject.remote.api;

import org.opencastproject.util.NotFoundException;

/**
 * A service that creates jobs for long-running operations.
 */
public interface JobProducer {

  /**
   * Returns the job. If no job with the given identifier exists, a {@link NotFoundException} is thrown.
   * 
   * @param id
   *          the job identifier
   * @return the job
   * @throws NotFoundException
   *           if the job doesn't exist
   */
  Job getJob(String id) throws NotFoundException;

}
