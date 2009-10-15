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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Describes an operation or action to be performed as part of a workflow.
 */
@XmlJavaTypeAdapter(WorkflowOperationImpl.Adapter.class)
public interface WorkflowOperation {

  /**
   * A unique name identifying this operation
   * 
   * @return the operation name
   */
  String getName();

  /**
   * Returns a human readable description of the workflow operation.
   * 
   * @return the description
   */
  String getDescription();

  boolean isFailOnError();

  /**
   * Asks the workflow operation to execute its logic based on the media packages and properties found in the workflow
   * instance.
   * 
   * @param workflow
   *          the workflow instance
   */
  void execute(WorkflowInstance workflow);

}
