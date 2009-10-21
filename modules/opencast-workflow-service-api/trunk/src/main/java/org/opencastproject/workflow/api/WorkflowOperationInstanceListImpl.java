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
 * A list of {@link WorkflowOperationInstance}s.
 */
@XmlType(name="operation-instance-list", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="operation-instance-list", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationInstanceListImpl implements WorkflowOperationInstanceList {

  @XmlElement(name="operation-instance")
  protected List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinitionList#getOperation()
   */
  public List<WorkflowOperationInstance> getOperationInstance() {
    return ops;
  }
  
  static class Adapter extends XmlAdapter<WorkflowOperationInstanceListImpl, WorkflowOperationInstanceList> {
    public WorkflowOperationInstanceListImpl marshal(WorkflowOperationInstanceList op) throws Exception {return (WorkflowOperationInstanceListImpl)op;}
    public WorkflowOperationInstanceList unmarshal(WorkflowOperationInstanceListImpl op) throws Exception {return op;}
  }

  public void add(int index, WorkflowOperationInstance element) {
    ops.add(index, element);
  }

  public boolean add(WorkflowOperationInstance e) {
    return ops.add(e);
  }

  public boolean addAll(Collection<? extends WorkflowOperationInstance> c) {
    return ops.addAll(c);
  }

  public boolean addAll(int index, Collection<? extends WorkflowOperationInstance> c) {
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

  public WorkflowOperationInstance get(int index) {
    return ops.get(index);
  }

  public int indexOf(Object o) {
    return ops.indexOf(o);
  }

  public boolean isEmpty() {
    return ops.isEmpty();
  }

  public Iterator<WorkflowOperationInstance> iterator() {
    return ops.iterator();
  }

  public int lastIndexOf(Object o) {
    return ops.lastIndexOf(o);
  }

  public ListIterator<WorkflowOperationInstance> listIterator() {
    return ops.listIterator();
  }

  public ListIterator<WorkflowOperationInstance> listIterator(int index) {
    return ops.listIterator(index);
  }

  public WorkflowOperationInstance remove(int index) {
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

  public WorkflowOperationInstance set(int index, WorkflowOperationInstance element) {
    return ops.set(index, element);
  }

  public int size() {
    return ops.size();
  }

  public List<WorkflowOperationInstance> subList(int fromIndex, int toIndex) {
    return ops.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return ops.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return ops.toArray(a);
  }

}
