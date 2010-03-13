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
package org.opencastproject.adminui.api;

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
 * A {@link List} of {@link RecordingDataView}s
 */
@XmlType(name="recordingList", namespace="http://adminui.opencastproject.org/")
@XmlRootElement(name="recordingList", namespace="http://adminui.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordingDataViewListImpl implements RecordingDataViewList {

  @XmlElement(name="adminui-recordings")
  protected List<RecordingDataView> recordings = new ArrayList<RecordingDataView>();

  public List<RecordingDataView> getRecordings() {
    if (recordings == null) {
      recordings = new ArrayList<RecordingDataView>();
    }
    return recordings;
  }

  static class Adapter extends XmlAdapter<RecordingDataViewListImpl, RecordingDataViewList> {
    public RecordingDataViewListImpl marshal(RecordingDataViewList op) throws Exception {return (RecordingDataViewListImpl)op;}
    public RecordingDataViewList unmarshal(RecordingDataViewListImpl op) throws Exception {return op;}
  }

  public void add(int index, RecordingDataView element) {
    recordings.add(index, element);
  }

  public boolean add(RecordingDataView e) {
    return recordings.add(e);
  }

  public boolean addAll(Collection<? extends RecordingDataView> c) {
    return recordings.addAll(c);
  }

  public boolean addAll(int index, Collection<? extends RecordingDataView> c) {
    return recordings.addAll(index, c);
  }

  public void clear() {
    recordings.clear();
  }

  public boolean contains(Object o) {
    return recordings.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return recordings.containsAll(c);
  }

  public RecordingDataView get(int index) {
    return recordings.get(index);
  }

  public int indexOf(Object o) {
    return recordings.indexOf(o);
  }

  public boolean isEmpty() {
    return recordings.isEmpty();
  }

  public Iterator<RecordingDataView> iterator() {
    return recordings.iterator();
  }

  public int lastIndexOf(Object o) {
    return recordings.lastIndexOf(o);
  }

  public ListIterator<RecordingDataView> listIterator() {
    return recordings.listIterator();
  }

  public ListIterator<RecordingDataView> listIterator(int index) {
    return recordings.listIterator(index);
  }

  public RecordingDataView remove(int index) {
    return recordings.remove(index);
  }

  public boolean remove(Object o) {
    return recordings.remove(o);
  }

  public boolean removeAll(Collection<?> c) {
    return recordings.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return recordings.retainAll(c);
  }

  public RecordingDataView set(int index, RecordingDataView element) {
    return recordings.set(index, element);
  }

  public int size() {
    return recordings.size();
  }

  public List<RecordingDataView> subList(int fromIndex, int toIndex) {
    return recordings.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return recordings.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return recordings.toArray(a);
  }
}
