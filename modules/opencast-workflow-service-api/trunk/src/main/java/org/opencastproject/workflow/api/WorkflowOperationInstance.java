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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * An instance of a {@link WorkflowOperationDefinition}.  Instances maintain the {@link MediaPackage} resulting from
 * the execution of a {@link WorkflowOperationRunner#run(WorkflowInstance)}.
 */
@XmlJavaTypeAdapter(WorkflowOperationInstanceImpl.Adapter.class)
public interface WorkflowOperationInstance extends WorkflowOperationDefinition {
  /**
   * Gets the resulting media package from the execution of {@link WorkflowOperationRunner#run(WorkflowInstance)}.
   * @return The media package, as produced from the execution of a workflow operation runner.
   */
  MediaPackage getResult();
}
