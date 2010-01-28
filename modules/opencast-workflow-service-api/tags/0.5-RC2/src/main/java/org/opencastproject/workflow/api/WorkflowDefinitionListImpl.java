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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A {@link List} of {@link WorkflowDefinition}s.
 */
@XmlType(name="workflow-definition-list", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-definition-list", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowDefinitionListImpl implements WorkflowDefinitionList {

  public WorkflowDefinitionListImpl() {}
  
  @XmlElement(name="workflow-definition")
  protected List<WorkflowDefinition> workflowDefinition = new ArrayList<WorkflowDefinition>();

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinitionList#getOperation()
   */
  public List<WorkflowDefinition> getWorkflowDefinition() {
    return workflowDefinition;
  }
  
  public void setOps(List<WorkflowDefinition> workflowDefinition) {
    this.workflowDefinition = workflowDefinition;
  }
  
  static class Adapter extends XmlAdapter<WorkflowDefinitionListImpl, WorkflowDefinitionList> {
    public WorkflowDefinitionListImpl marshal(WorkflowDefinitionList op) throws Exception {return (WorkflowDefinitionListImpl)op;}
    public WorkflowDefinitionList unmarshal(WorkflowDefinitionListImpl op) throws Exception {return op;}
  }

  public void add(int index, WorkflowDefinition element) {
    workflowDefinition.add(index, element);
  }

  public boolean add(WorkflowDefinition e) {
    return workflowDefinition.add(e);
  }

  public boolean addAll(Collection<? extends WorkflowDefinition> c) {
    return workflowDefinition.addAll(c);
  }

  public boolean addAll(int index, Collection<? extends WorkflowDefinition> c) {
    return workflowDefinition.addAll(index, c);
  }

  public void clear() {
    workflowDefinition.clear();
  }

  public boolean contains(Object o) {
    return workflowDefinition.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return workflowDefinition.containsAll(c);
  }

  public WorkflowDefinition get(int index) {
    return workflowDefinition.get(index);
  }

  public int indexOf(Object o) {
    return workflowDefinition.indexOf(o);
  }

  public boolean isEmpty() {
    return workflowDefinition.isEmpty();
  }

  public Iterator<WorkflowDefinition> iterator() {
    return workflowDefinition.iterator();
  }

  public int lastIndexOf(Object o) {
    return workflowDefinition.lastIndexOf(o);
  }

  public ListIterator<WorkflowDefinition> listIterator() {
    return workflowDefinition.listIterator();
  }

  public ListIterator<WorkflowDefinition> listIterator(int index) {
    return workflowDefinition.listIterator(index);
  }

  public WorkflowDefinition remove(int index) {
    return workflowDefinition.remove(index);
  }

  public boolean remove(Object o) {
    return workflowDefinition.remove(o);
  }

  public boolean removeAll(Collection<?> c) {
    return workflowDefinition.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return workflowDefinition.retainAll(c);
  }

  public WorkflowDefinition set(int index, WorkflowDefinition element) {
    return workflowDefinition.set(index, element);
  }

  public int size() {
    return workflowDefinition.size();
  }

  public List<WorkflowDefinition> subList(int fromIndex, int toIndex) {
    return workflowDefinition.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return workflowDefinition.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return workflowDefinition.toArray(a);
  }

}
