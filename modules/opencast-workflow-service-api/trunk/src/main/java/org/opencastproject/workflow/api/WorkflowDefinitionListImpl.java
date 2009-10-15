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


import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A list of {@link WorkflowDefinition}s.
 */
@XmlType(name="workflow-definitions", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-definitions", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowDefinitionListImpl implements WorkflowDefinitionList {

  public WorkflowDefinitionListImpl() {}
  
  @XmlElement(name="workflow-definition")
  protected List<WorkflowDefinition> ops;
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinitionList#getOperation()
   */
  public List<WorkflowDefinition> getWorkflowDefinition() {
    if (ops == null) {
      ops = new ArrayList<WorkflowDefinition>();
    }
    return ops;
  }
  
  static class Adapter extends XmlAdapter<WorkflowDefinitionListImpl, WorkflowDefinitionList> {
    public WorkflowDefinitionListImpl marshal(WorkflowDefinitionList op) throws Exception {return (WorkflowDefinitionListImpl)op;}
    public WorkflowDefinitionList unmarshal(WorkflowDefinitionListImpl op) throws Exception {return op;}
  }

}
