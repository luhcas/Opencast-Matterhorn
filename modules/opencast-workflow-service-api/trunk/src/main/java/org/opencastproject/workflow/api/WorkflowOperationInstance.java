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

import org.opencastproject.workflow.api.WorkflowInstance.State;

import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * An instance of a {@link WorkflowOperationInstance}.  Instances maintain the {@link MediaPackage} resulting from
 * the execution of {@link WorkflowOperationRunner#run(WorkflowInstance)}.
 */
@XmlJavaTypeAdapter(WorkflowOperationInstanceImpl.Adapter.class)
public interface WorkflowOperationInstance {

  String getName();
  
  String getDescription();

  /**
   * The state of this operation.
   */
  State getState();
  
  /**
   * Gets the resulting media package from the execution of {@link WorkflowOperationHandler#run(WorkflowInstance)}.
   * @return The media package, as produced from the execution of a workflow operation runner.
   */
  WorkflowOperationResult getResult();
  
  /**
   * Gets the configuration elements for this workflow operation.
   */
  Set<WorkflowConfiguration> getConfigurations();
  
  /**
   * Returns the value of property <code>name</code> or <code>null</code> if no such property has been set.
   * 
   * @param key
   *          the configuration key
   * @return the configuration value
   */
  public String getConfiguration(String key);

  /**
   * Sets the configuration with name <code>key</code> to value <code>value</code>, or adds it if it doesn't already
   * exist.
   * 
   * @param key
   *          the configuration key
   * @param value
   *          the configuration value
   */
  public void setConfiguration(String key, String value);

  /**
   * Removes the <code>key</code> configuration.
   * 
   * @param key
   *          the configuration key
   */
  public void removeConfiguration(String key);

}
