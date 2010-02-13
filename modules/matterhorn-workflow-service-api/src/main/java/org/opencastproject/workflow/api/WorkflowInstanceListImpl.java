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

/**
 * A {@link List} of {@link WorkflowInstance}s
 */
@XmlType(name="workflow-instances", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-instances", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowInstanceListImpl implements List<WorkflowInstanceImpl> {
  @XmlElement(name="workflow-instance")
  protected List<WorkflowInstanceImpl> instance;
  public List<WorkflowInstanceImpl> getWorkflowInstance() {
    if (instance == null) {
      instance = new ArrayList<WorkflowInstanceImpl>();
    }
    return instance;
  }
  public void add(int index, WorkflowInstanceImpl element) {
    getWorkflowInstance().add(index, element);
  }
  public boolean add(WorkflowInstanceImpl e) {
    return getWorkflowInstance().add(e);
  }
  public boolean addAll(Collection<? extends WorkflowInstanceImpl> c) {
    return getWorkflowInstance().addAll(c);
  }
  public boolean addAll(int index, Collection<? extends WorkflowInstanceImpl> c) {
    return getWorkflowInstance().addAll(index, c);
  }
  public void clear() {
    getWorkflowInstance().clear();
  }
  public boolean contains(Object o) {
    return getWorkflowInstance().contains(o);
  }
  public boolean containsAll(Collection<?> c) {
    return getWorkflowInstance().containsAll(c);
  }
  public WorkflowInstanceImpl get(int index) {
    return getWorkflowInstance().get(index);
  }
  public int indexOf(Object o) {
    return getWorkflowInstance().indexOf(o);
  }
  public boolean isEmpty() {
    return getWorkflowInstance().isEmpty();
  }
  public Iterator<WorkflowInstanceImpl> iterator() {
    return getWorkflowInstance().iterator();
  }
  public int lastIndexOf(Object o) {
    return getWorkflowInstance().lastIndexOf(o);
  }
  public ListIterator<WorkflowInstanceImpl> listIterator() {
    return getWorkflowInstance().listIterator();
  }
  public ListIterator<WorkflowInstanceImpl> listIterator(int index) {
    return getWorkflowInstance().listIterator(index);
  }
  public WorkflowInstanceImpl remove(int index) {
    return getWorkflowInstance().remove(index);
  }
  public boolean remove(Object o) {
    return getWorkflowInstance().remove(o);
  }
  public boolean removeAll(Collection<?> c) {
    return getWorkflowInstance().removeAll(c);
  }
  public boolean retainAll(Collection<?> c) {
    return getWorkflowInstance().retainAll(c);
  }
  public WorkflowInstanceImpl set(int index, WorkflowInstanceImpl element) {
    return getWorkflowInstance().set(index, element);
  }
  public int size() {
    return getWorkflowInstance().size();
  }
  public List<WorkflowInstanceImpl> subList(int fromIndex, int toIndex) {
    return getWorkflowInstance().subList(fromIndex, toIndex);
  }
  public Object[] toArray() {
    return getWorkflowInstance().toArray();
  }
  public <T> T[] toArray(T[] a) {
    return getWorkflowInstance().toArray(a);
  }
}
