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

import org.opencastproject.media.mediapackage.MediaPackage;

import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * An single instance of a running, paused, or stopped workflow.
 */
@XmlJavaTypeAdapter(WorkflowInstanceImpl.Adapter.class)
public interface WorkflowInstance {
  public enum State { INSTANTIATED, RUNNING, STOPPED, PAUSED, SUCCEEDED, FAILED, FAILING }

  /**
   * The unique ID of this {@link WorkflowInstance}
   */
  String getId();

  /**
   * The short title of this {@link WorkflowInstance}
   */
  String getTitle();

  /**
   * The longer description of this {@link WorkflowInstance}
   */
  String getDescription();

  /**
   * The {@link WorkflowOperationDefinitionList} that serves as a template for this {@link WorkflowOperationInstanceList}
   * belonging to this WorkflowInstance
   * was created.
   */
  WorkflowOperationDefinitionList getWorkflowOperationDefinitionList();

  /**
   * Returns the {@link WorkflowOperation}s that make up this workflow.
   * 
   * @return the workflow operations
   */
  public WorkflowOperationInstanceList getWorkflowOperationInstanceList();

  /**
   * Returns the {@link WorkflowOperation} that is currently being executed.
   * 
   * @return the current operation
   */
  public WorkflowOperationInstance getCurrentOperation();

  /**
   * Appends a {@link WorkflowOperationDefinition} to the end of the list of operations to perform.
   * 
   * @param operation
   *          the operation to append
   */
  public void addWorkflowOperationDefinition(WorkflowOperationDefinition operation);

  /**
   * Inserts a {@link WorkflowOperationDefinition} at index <code>location</code> into the list of workflow operations
   * to perform.
   * 
   * @param location
   *          index into the list of operations
   * @param operation
   *          the operation to append
   */
  public void addWorkflowOperationDefinition(int location, WorkflowOperationDefinition operation);

  /**
   * Appends a {@link WorkflowOperationDefinitionList} to the end of the list of operations to perform.
   * 
   * @param operations
   *          the operations to append
   */
  public void addWorkflowOperationDefinitions(WorkflowOperationDefinitionList operations);

  /**
   * Inserts the contents of a {@link WorkflowOperationDefinitionList} at index <code>location</code> into the list of
   * workflow operations to be performed.
   * 
   * @param location
   *          index into the list of operations
   * @param operations
   *          the operations to append
   */
  public void addWorkflowOperationDefinitions(int location, WorkflowOperationDefinitionList operations);

  /**
   * The current {@link State} of this {@link WorkflowInstance}
   */
  State getState();

  /**
   * The {@link MediaPackage} associated with this {@link WorkflowInstance}
   */
  MediaPackage getSourceMediaPackage();

  /**
   * The {@link MediaPackage} from the latest {@link WorkflowOperationResult}, or the source mediapackage if no
   * operations have successfully executed.
   */
  MediaPackage getCurrentMediaPackage();

  /**
   * The properties associated with this workflow instance. Properties can be used to affect how
   * {@link WorkflowDefinition#getOperations()} are eventually run.
   */
  Map<String, String> getProperties();

  /**
   * Returns the value of property <code>name</code> or <code>null</code> if no such property has been set.
   * 
   * @param name
   *          the property name
   * @return the property value
   */
  public String getProperty(String name);

  /**
   * Sets the property with name <code>name</code> to value <code>value</code>.
   * 
   * @param name
   *          the property name
   * @param value
   *          the property value
   */
  public void setProperty(String name, String value);

  /**
   * Removes the property with name <code>name</code> from the list of defined properties.
   * 
   * @param name
   *          the property name
   */
  public void removeProperty(String name);

}
