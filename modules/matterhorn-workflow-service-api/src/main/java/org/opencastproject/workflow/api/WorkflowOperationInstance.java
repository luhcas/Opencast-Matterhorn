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
 * An instance of a {@link WorkflowOperationInstance}.  Instances maintain the {@link MediaPackage} resulting from
 * the execution of {@link WorkflowOperationRunner#start(WorkflowInstance)}.
 */
@XmlJavaTypeAdapter(WorkflowOperationInstanceImpl.Adapter.class)
public interface WorkflowOperationInstance extends Configurable {
  public enum OperationState { INSTANTIATED, RUNNING, PAUSED, SUCCEEDED, FAILED }

  String getId();
  
  String getDescription();

  /**
   * The state of this operation.
   */
  OperationState getState();
  
  /**
   * Sets the state of this operation
   * @param state
   */
  void setState(OperationState state);
}
