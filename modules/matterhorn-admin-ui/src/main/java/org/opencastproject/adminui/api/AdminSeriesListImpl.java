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
 * A {@link List} of {@link AdminSeries}s
 */
@XmlRootElement(name="seriesList")
@XmlAccessorType(XmlAccessType.FIELD)
public class AdminSeriesListImpl implements AdminSeriesList {

  protected LinkedList<AdminSeries> seriesList = new LinkedList<AdminSeries>();   // makes automatic marshaling work with jaxb

  @XmlAttribute(name="sort-by")             // FIXME doesn't get marshaled
  public Field sortBy = Field.Title;

  @XmlAttribute(name="sort-order")
  public Order sortOrder = Order.Descending;

  public AdminSeriesListImpl() {
  }

  public AdminSeriesListImpl(Field sortBy, Order sortOrder) {
    this.sortBy = sortBy;
    this.sortOrder = sortOrder;
  }

  public List<AdminSeries> getSeriesList() {
    if (seriesList == null) {
      seriesList = new LinkedList<AdminSeries>();
    }
    return seriesList;
  }

  static class Adapter extends XmlAdapter<AdminSeriesListImpl, AdminSeriesList> {
    public AdminSeriesListImpl marshal(AdminSeriesList op) throws Exception {return (AdminSeriesListImpl)op;}
    public AdminSeriesList unmarshal(AdminSeriesListImpl op) throws Exception {return op;}
  }

  @Override
  public boolean add(AdminSeries series) {
    insertSorted(series);
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends AdminSeries> list) {
    Iterator<? extends AdminSeries> iter = list.iterator();
    while (iter.hasNext()) {
      insertSorted(iter.next());
    }
    return true;
  }
  
  private boolean insertSorted(AdminSeries series) {
    if (sortOrder == Order.Descending) {
      insertDescSorted(series);
    } else {
      insertAscSorted(series);
    }
    return true;
  }

  private void insertAscSorted(AdminSeries series) {
    Collator collator = null;
    Comparable compNew = getComparedMember(series);
    boolean stringComp = compNew instanceof String;
    if (stringComp) {
      collator = Collator.getInstance();
    }
    Iterator<AdminSeries> iter = this.iterator();
    for (int i=0; iter.hasNext(); i++) {
      Comparable compCurrent = getComparedMember(iter.next());
      int relation;
      if (stringComp) {
        relation = collator.compare(compCurrent, compNew);
      } else {
        relation = compCurrent.compareTo(compNew);
      }
      if (relation > 0) {
        insertAt(i, series);
        return;
      }
    }
    seriesList.addLast(series);
  }

  private void insertDescSorted(AdminSeries series) {
    Collator collator = null;
    Comparable compNew = getComparedMember(series);
    boolean stringComp = compNew instanceof String;
    if (stringComp) {
      collator = Collator.getInstance();
    }
    Iterator<AdminSeries> iter = this.iterator();
    for (int i=0; iter.hasNext(); i++) {
      Comparable compCurrent = getComparedMember(iter.next());
      int relation;
      if (stringComp) {
        relation = collator.compare(compCurrent, compNew);
      } else {
        relation = compCurrent.compareTo(compNew);
      }
      if (relation < 0) {
        insertAt(i, series);
        return;
      }
    }
    seriesList.addLast(series);
  }

  /** Inserts an series at specified index caring about cases:
   *  index < 0   and  index > this.size()
   *
   * @param index
   * @param series
   */
  private void insertAt(int index, AdminSeries series) {
    if (index < 0) {
      seriesList.addFirst(series);
    } else if (index > this.size()) {
      seriesList.addLast(series);
    } else {
      add(index, series);
    }
  }

    /** Returns the field of an AdminSeries that is to be compare for sorting
   *  in this instance (specified
   * @param r
   * @return
   */
  private Comparable getComparedMember(AdminSeries series) {
    String out;
    switch (sortBy) {
      case Title:
        out = series.getTitle();
        break;
      case Creator:
        out =  series.getCreator();
        break;
      case Contributor:
        out =  series.getContributor();
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

  public void add(int index, AdminSeries element) {
    seriesList.add(index, element);
  }

  public boolean addAll(int index, Collection<? extends AdminSeries> c) {
    return seriesList.addAll(index, c);
  }

  public void clear() {
    seriesList.clear();
  }

  public boolean contains(Object o) {
    return seriesList.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return seriesList.containsAll(c);
  }

  public AdminSeries get(int index) {
    return seriesList.get(index);
  }

  public int indexOf(Object o) {
    return seriesList.indexOf(o);
  }

  public boolean isEmpty() {
    return seriesList.isEmpty();
  }

  public Iterator<AdminSeries> iterator() {
    return seriesList.iterator();
  }

  public int lastIndexOf(Object o) {
    return seriesList.lastIndexOf(o);
  }

  public ListIterator<AdminSeries> listIterator() {
    return seriesList.listIterator();
  }

  public ListIterator<AdminSeries> listIterator(int index) {
    return seriesList.listIterator(index);
  }

  public AdminSeries remove(int index) {
    return seriesList.remove(index);
  }

  public boolean remove(Object o) {
    return seriesList.remove(o);
  }

  public boolean removeAll(Collection<?> c) {
    return seriesList.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return seriesList.retainAll(c);
  }

  public AdminSeries set(int index, AdminSeries element) {
    return seriesList.set(index, element);
  }

  public int size() {
    return seriesList.size();
  }

  public List<AdminSeries> subList(int fromIndex, int toIndex) {
    return seriesList.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return seriesList.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return seriesList.toArray(a);
  }
}
