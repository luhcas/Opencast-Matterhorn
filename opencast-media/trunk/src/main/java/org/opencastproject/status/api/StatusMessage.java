/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.status.api;

/**
 * TODO Does this really belong in the media osgi bundle?
 *
 */
public interface StatusMessage {
  /**
   * The source of the message.  This should be an ID or description of the component generating
   * the message.
   */
  String getSource();
  
  /**
   * A reference to the job or task this status describes.
   * @return
   */
  String getReference();

  /**
   * The details of the status message.
   * @return
   */
  String getMessage();
}
