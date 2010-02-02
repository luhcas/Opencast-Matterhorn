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

import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Describes an operation or action to be performed as part of a workflow.
 */
@XmlJavaTypeAdapter(WorkflowOperationDefinitionImpl.Adapter.class)
public interface WorkflowOperationDefinition {
  
  String getName();
  
  String getDescription();

  /** The workflow to run if an exception is thrown while this operation is running. */
  String getExceptionHandlingWorkflow();

  /**
   * If true, this workflow will be put into a failed (or failing, if getExceptionHandlingWorkflow() is not null) state
   * when exceptions are thrown during an operation.
   */
  boolean isFailWorkflowOnException();

  /** Gets the configuration elements for this workflow operation. */
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
