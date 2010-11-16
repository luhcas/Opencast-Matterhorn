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
package org.opencastproject.workflow.api;

import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A workflow definition.
 */
@XmlJavaTypeAdapter(WorkflowDefinitionImpl.Adapter.class)
public interface WorkflowDefinition {
  /**
   * The variable in a workflow definition that is to be replaced by the reason for an operation's failure.
   */
  final String FAILURE_KEY = "failure.message";

  /**
   * The short title of this workflow definition
   */
  String getId();

  /**
   * Sets the identifier
   * 
   * @param id
   *          the workflow definition identifier
   */
  void setId(String id);

  /**
   * The title for this workflow definition
   */
  String getTitle();

  /**
   * Sets the title
   * 
   * @param title
   *          the workflow definition title
   */
  void setTitle(String title);

  /**
   * A longer description of this workflow definition
   */
  String getDescription();

  /**
   * Sets the description
   * 
   * @param description
   *          the workflow definition description
   */
  void setDescription(String description);

  /**
   * An XML String describing the configuration parameter/panel for this WorkflowDefinition.
   */
  String getConfigurationPanel();

  /**
   * The operations, listed in order, that this workflow definition includes.
   */
  List<WorkflowOperationDefinition> getOperations();

  /**
   * Whether this definition is published. This information is useful for user interfaces.
   * 
   * @return Whether this is a published workflow definition
   */
  boolean isPublished();
}
