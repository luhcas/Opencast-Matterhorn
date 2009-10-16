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
package org.opencastproject.conductor.api;

import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionList;

/**
 * An abstraction to access customized workflow definitions.
 */
public interface ConductorService {

  /**
   * @return All registered {@link WorkflowDefinition}s
   */
  WorkflowDefinitionList getWorkflowDefinitions();

  /**
   * Returns the {@link WorkflowDefinition} whith the specified name or <code>null</code> if no such definition is
   * available.
   * 
   * @param title the title of the workflow definition
   * @return the workflow definition
   */
  WorkflowDefinition getWorkflowDefinitionByTitle(String title);

  /**
   * Adds a new workflow definition.
   * @param def The new workflow definition
   */
  void addWorkflowDefinition(WorkflowDefinition def);

  /**
   * Removes the workflow definition with the given title
   * @param title The title of the workflow definition to remove
   * @return The removed workflow definition, or null if nothing was removed
   */
  WorkflowDefinition removeWorkflowDefinition(String title);
}
