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
package org.opencastproject.workflow.api;

import java.util.List;

/**
 * A workflow definition.
 */
public interface WorkflowDefinition {
  /**
   * The unique ID of this workflow definition
   * @return
   */
  String getId();

  /**
   * The short title of this workflow definition
   */
  String getTitle();

  /**
   * A longer description of this workflow definition
   */
  String getDescription();

  /**
   * The operations, listed in order, that this workflow definition includes.
   */
  List<String> getOperations();
}

