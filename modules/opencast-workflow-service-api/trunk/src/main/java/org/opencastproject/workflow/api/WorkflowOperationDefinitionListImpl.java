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
 * TODO: Comment me!
 *
 */
@XmlType(name="operation-definition-list", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="operation-definition-list", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationDefinitionListImpl implements WorkflowOperationDefinitionList {
  @XmlElement(name="operation")
  protected List<WorkflowOperationDefinition> ops = new ArrayList<WorkflowOperationDefinition>();

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinitionList#getOperation()
   */
  public List<WorkflowOperationDefinition> getOperation() {
    return ops;
  }
  
  static class Adapter extends XmlAdapter<WorkflowOperationDefinitionListImpl, WorkflowOperationDefinitionList> {
    public WorkflowOperationDefinitionListImpl marshal(WorkflowOperationDefinitionList op) throws Exception {return (WorkflowOperationDefinitionListImpl)op;}
    public WorkflowOperationDefinitionList unmarshal(WorkflowOperationDefinitionListImpl op) throws Exception {return op;}
  }

  public void add(int index, WorkflowOperationDefinition element) {
    ops.add(index, element);
  }

  public boolean add(WorkflowOperationDefinition e) {
    return ops.add(e);
  }

  public boolean addAll(Collection<? extends WorkflowOperationDefinition> c) {
    return ops.addAll(c);
  }

  public boolean addAll(int index, Collection<? extends WorkflowOperationDefinition> c) {
    return ops.addAll(index, c);
  }

  public void clear() {
    ops.clear();
  }

  public boolean contains(Object o) {
    return ops.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return ops.containsAll(c);
  }

  public WorkflowOperationDefinition get(int index) {
    return ops.get(index);
  }

  public int indexOf(Object o) {
    return ops.indexOf(o);
  }

  public boolean isEmpty() {
    return ops.isEmpty();
  }

  public Iterator<WorkflowOperationDefinition> iterator() {
    return ops.iterator();
  }

  public int lastIndexOf(Object o) {
    return ops.lastIndexOf(o);
  }

  public ListIterator<WorkflowOperationDefinition> listIterator() {
    return ops.listIterator();
  }

  public ListIterator<WorkflowOperationDefinition> listIterator(int index) {
    return ops.listIterator(index);
  }

  public WorkflowOperationDefinition remove(int index) {
    return ops.remove(index);
  }

  public boolean remove(Object o) {
    return ops.remove(o);
  }

  public boolean removeAll(Collection<?> c) {
    return ops.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return ops.retainAll(c);
  }

  public WorkflowOperationDefinition set(int index, WorkflowOperationDefinition element) {
    return ops.set(index, element);
  }

  public int size() {
    return ops.size();
  }

  public List<WorkflowOperationDefinition> subList(int fromIndex, int toIndex) {
    return ops.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return ops.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return ops.toArray(a);
  }

}
