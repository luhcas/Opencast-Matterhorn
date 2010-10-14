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

import java.text.Collator;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A {@link List} of {@link AdminRecording}s
 */
@XmlRootElement(name="recordingList")
@XmlAccessorType(XmlAccessType.FIELD)
public class AdminRecordingListImpl implements AdminRecordingList {

  protected LinkedList<AdminRecording> recordings = new LinkedList<AdminRecording>();   // makes automatic marshaling work with jaxb

  @XmlAttribute(name="sort-by")             // FIXME doesn't get marshaled
  public Field sortBy = Field.StartDate;

  @XmlAttribute(name="sort-order")
  public Order sortOrder = Order.Descending;

  public AdminRecordingListImpl() {
  }

  public AdminRecordingListImpl(Field sortBy, Order sortOrder) {
    this.sortBy = sortBy;
    this.sortOrder = sortOrder;
  }

  public List<AdminRecording> getRecordings() {
    if (recordings == null) {
      recordings = new LinkedList<AdminRecording>();
    }
    return recordings;
  }

  static class Adapter extends XmlAdapter<AdminRecordingListImpl, AdminRecordingList> {
    public AdminRecordingListImpl marshal(AdminRecordingList op) throws Exception {return (AdminRecordingListImpl)op;}
    public AdminRecordingList unmarshal(AdminRecordingListImpl op) throws Exception {return op;}
  }

  @Override
  public boolean add(AdminRecording recording) {
    insertSorted(recording);
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends AdminRecording> list) {
    Iterator<? extends AdminRecording> iter = list.iterator();
    while (iter.hasNext()) {
      insertSorted(iter.next());
    }
    return true;
  }

  /** Inserts an AdminRecording keeping the list in order by performing binary
   *  search on the underlying list to find the right place to insert the new
   *  element. If recording 'equals' another AdminRecording already in the
   *  list, then recording is inserted after the element that is 'equal'
   *  (in sorting direction).
   *
   * @param recording
   */
  private boolean insertSorted(AdminRecording recording) {
    if (sortOrder == Order.Descending) {
      insertDescSorted(recording);
    } else {
      insertAscSorted(recording);
    }
    return true;
  }

  private void insertAscSorted(AdminRecording recording) {
    Collator collator = null;
    Comparable compNew = getComparedMember(recording);
    boolean stringComp = compNew instanceof String;
    if (stringComp) {
      collator = Collator.getInstance();
    }
    Iterator<AdminRecording> iter = this.iterator();
    for (int i=0; iter.hasNext(); i++) {
      Comparable compCurrent = getComparedMember(iter.next());
      int relation;
      if (stringComp) {
        relation = collator.compare(compCurrent, compNew);
      } else {
        relation = compCurrent.compareTo(compNew);
      }
      if (relation > 0) {
        insertAt(i, recording);
        return;
      }
    }
    recordings.addLast(recording);
  }

  private void insertDescSorted(AdminRecording recording) {
    Collator collator = null;
    Comparable compNew = getComparedMember(recording);
    boolean stringComp = compNew instanceof String;
    if (stringComp) {
      collator = Collator.getInstance();
    }
    Iterator<AdminRecording> iter = this.iterator();
    for (int i=0; iter.hasNext(); i++) {
      Comparable compCurrent = getComparedMember(iter.next());
      int relation;
      if (stringComp) {
        relation = collator.compare(compCurrent, compNew);
      } else {
        relation = compCurrent.compareTo(compNew);
      }
      if (relation < 0) {
        insertAt(i, recording);
        return;
      }
    }
    recordings.addLast(recording);
  }

  /** Inserts an recording at specified index caring about cases:
   *  index < 0   and  index > this.size()
   *
   * @param index
   * @param recording
   */
  private void insertAt(int index, AdminRecording recording) {
    if (index < 0) {
      recordings.addFirst(recording);
    } else if (index > this.size()) {
      recordings.addLast(recording);
    } else {
      add(index, recording);
    }
  }

    /** Returns the field of an AdminRecording that is to be compare for sorting
   *  in this instance (specified
   * @param r
   * @return
   */
  private Comparable getComparedMember(AdminRecording r) {
    String out;
    switch (sortBy) {
      case Title:
        out = r.getTitle();
        break;
      case Presenter:
        out =  r.getPresenter();
        break;
      case Series:
        out =  r.getSeriesTitle();
        break;
      case StartDate:
        try {
          return Long.valueOf(r.getStartTime());    // FIXME make startDate a long in AdminRecording
        } catch (NumberFormatException e) {
          return 0L;
        }
      case RecordingStatus:
        out =  r.getRecordingStatus();
        break;
      case ProcessingStatus:
        out =  r.getProcessingStatus();
        break;
      case CaptureAgent:
        out =  r.getCaptureAgent();
        break;
      case HoldTitle:
        out =  r.getHoldOperationTitle();
        break;
      default:                              // should not happen
        out =  "";
        break;
    }
    if (out == null) {    // return empty String so comparing doesn't yield Exception
      return "";
    } else {
      return out;
    }
  }

  public void add(int index, AdminRecording element) {
    recordings.add(index, element);
  }

  public boolean addAll(int index, Collection<? extends AdminRecording> c) {
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

  public AdminRecording get(int index) {
    return recordings.get(index);
  }

  public int indexOf(Object o) {
    return recordings.indexOf(o);
  }

  public boolean isEmpty() {
    return recordings.isEmpty();
  }

  public Iterator<AdminRecording> iterator() {
    return recordings.iterator();
  }

  public int lastIndexOf(Object o) {
    return recordings.lastIndexOf(o);
  }

  public ListIterator<AdminRecording> listIterator() {
    return recordings.listIterator();
  }

  public ListIterator<AdminRecording> listIterator(int index) {
    return recordings.listIterator(index);
  }

  public AdminRecording remove(int index) {
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

  public AdminRecording set(int index, AdminRecording element) {
    return recordings.set(index, element);
  }

  public int size() {
    return recordings.size();
  }

  public List<AdminRecording> subList(int fromIndex, int toIndex) {
    return recordings.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return recordings.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return recordings.toArray(a);
  }
}
