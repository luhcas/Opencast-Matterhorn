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
package org.opencastproject.engage.api;

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
 * A {@link List} of {@link EpisodeViewList}s
 */
@XmlType(name="episodeList", namespace="http://searchui.opencastproject.org/")
@XmlRootElement(name="episodeList", namespace="http://searchui.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class EpisodeViewListImpl implements EpisodeViewList {

  @XmlElement(name="searchui-episode")
  protected List<EpisodeView> episodes = new ArrayList<EpisodeView>();
  
  public List<EpisodeView> getEpisodes() {
    if (episodes == null) {
      episodes = new ArrayList<EpisodeView>();
    }
    return episodes;
  }

  static class Adapter extends XmlAdapter<EpisodeViewListImpl, EpisodeViewList> {
    public EpisodeViewListImpl marshal(EpisodeViewList op) throws Exception {return (EpisodeViewListImpl)op;}
    public EpisodeViewList unmarshal(EpisodeViewListImpl op) throws Exception {return op;}
  }

  public void add(int index, EpisodeView element) {
    episodes.add(index, element);
  }

  public boolean add(EpisodeView e) {
    return episodes.add(e);
  }

  public boolean addAll(Collection<? extends EpisodeView> c) {
    return episodes.addAll(c);
  }

  public boolean addAll(int index, Collection<? extends EpisodeView> c) {
    return episodes.addAll(index, c);
  }

  public void clear() {
    episodes.clear();
  }

  public boolean contains(Object o) {
    return episodes.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return episodes.containsAll(c);
  }

  public EpisodeView get(int index) {
    return episodes.get(index);
  }

  public int indexOf(Object o) {
    return episodes.indexOf(o);
  }

  public boolean isEmpty() {
    return episodes.isEmpty();
  }

  public Iterator<EpisodeView> iterator() {
    return episodes.iterator();
  }

  public int lastIndexOf(Object o) {
    return episodes.lastIndexOf(o);
  }

  public ListIterator<EpisodeView> listIterator() {
    return episodes.listIterator();
  }

  public ListIterator<EpisodeView> listIterator(int index) {
    return episodes.listIterator(index);
  }

  public EpisodeView remove(int index) {
    return episodes.remove(index);
  }

  public boolean remove(Object o) {
    return episodes.remove(o);
  }

  public boolean removeAll(Collection<?> c) {
    return episodes.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return episodes.retainAll(c);
  }

  public EpisodeView set(int index, EpisodeView element) {
    return episodes.set(index, element);
  }

  public int size() {
    return episodes.size();
  }

  public List<EpisodeView> subList(int fromIndex, int toIndex) {
    return episodes.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return episodes.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return episodes.toArray(a);
  }
}
