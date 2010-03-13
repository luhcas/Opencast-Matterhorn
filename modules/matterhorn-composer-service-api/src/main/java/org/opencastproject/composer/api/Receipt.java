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
package org.opencastproject.composer.api;

import org.opencastproject.media.mediapackage.MediaPackageElement;

/**
 * A receipt for an encoding job.  A Receipt may be used to track an encoding job once it has been queued.
 */
public interface Receipt {
  public static enum Status {QUEUED, RUNNING, FINISHED, FAILED}

  public String getId();

  public void setId(String id);

  public Status getStatus();

  public void setStatus(Status status);

  public String getHost();

  public void setHost(String host);

  public MediaPackageElement getElement();

  public void setElement(MediaPackageElement element);
}
