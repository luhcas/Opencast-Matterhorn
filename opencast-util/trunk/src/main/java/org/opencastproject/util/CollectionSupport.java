/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Utility methods for collections.
 * <p/>
 * Code copied from class <code>appetizer.util.Kollections</code> of project
 * "appetizer", originally create May 24, 2006. Donated to REPLAY by the author.
 *
 * todo translate original german documentation
 *
 * @author Christoph Drießen
 */
public class CollectionSupport {

  private CollectionSupport() {
  }

  /**
   * Gibt die Map zeilenweise aus, je ein Key => Value Pärchen pro Zeile.
   */
  public static <K, V> void dump(Map<K, V> map) {
    for (Map.Entry<K, V> entry : map.entrySet()) {
      System.out.printf("%s => %s\n", entry.getKey(), entry.getValue());
    }
  }

  /**
   * Gibt die Liste zeilenweise aus, je ein Element pro Zeile.
   */
  public static <E> void dump(List<E> list) {
    for (E elem : list)
      System.out.println(elem.toString());
  }

  public static <K, V> V get(Map<K, V> map, K k, Class<V> clazz) {
    V v = map.get(k);
    if (v == null) {
      try {
        v = clazz.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      map.put(k, v);
    }
    return v;
  }

  /**
   * Initialisiert <i>collection</i> mit <i>initElement<i>. Das bedeutet, daß
   * <i>collection</i> vorher geleert wird.
   *
   * @param initElement
   *          darf <i>null</i> sein, wenn es der Collectiontyp zuläßt.
   */
  public static <E, T extends Collection<E>> T init(T collection, E initElement) {
    Assert.notNull(collection, "collection");
    collection.clear();
    collection.add(initElement);
    return collection;
  }

  /**
   * Speichert alle Objekte <i>obj</i> in der Collection <i>collection</i>.
   * Nutzt {@link Collections#addAll}, nur daß die Collection zurückgegeben
   * wird und die Methode somit zu Initialisierungszwecken genutzt werden kann.
   *
   * @param obj
   *          die zuzufügenden Objekte oder <i>null</i>
   * @return <i>collection</i>
   */
  public static <E, T extends Collection<E>> T addAll(T collection, E... obj) {
    Assert.notNull(collection, "collection");
    if (obj != null)
      Collections.addAll(collection, obj);
    return collection;
  }

  /**
   * Fügt alle Elemente der <i>cols</i>-Collections der <i>collection</i>
   * hinzu.
   * 
   * @param cols
   *          darf <i>null</i> sein
   * @return <i>collection</i>
   */
  public static <E, T extends Collection<E>> T addAll(T collection, T... cols) {
    Assert.notNull(collection, "collection");
    if (cols != null)
      for (T col : cols)
        collection.addAll(col);
    return collection;
  }

  public static <E, T extends Collection<E>> T addAll(T collection,
      Enumeration<E> obj) {
    Assert.notNull(collection, "collection");
    while (obj.hasMoreElements())
      collection.add(obj.nextElement());
    return collection;
  }

  /**
   * Liefert eine Liste, die alle Elemente beinhaltet.
   * 
   * @param elements
   *          darf <i>null</i> sein
   * @return ArrayList, bzw. EmptyList, falls <i>elements == null</i>
   */
  public static <E> List<E> createList(E... elements) {
    if (elements != null) {
      List<E> list = new ArrayList<E>(elements.length);
      Collections.addAll(list, elements);
      return list;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Liefert einen Ausschnitt aus <i>array</i>, <i>begin</i> inklusive und
   * <i>end</i> exklusive.
   */
  public static <T> T[] slice(T[] array, int begin, int end) {
    Assert.notNull(array, "array");
    if (begin < 0 || begin > end)
      throw new ArrayIndexOutOfBoundsException(begin);
    if (end > array.length)
      throw new ArrayIndexOutOfBoundsException(end);
    //
    T[] slice = (T[]) Array.newInstance(array.getClass().getComponentType(),
        end - begin);
    System.arraycopy(array, begin, slice, 0, end - begin);
    return slice;
  }

  public static <T> T[] slice(T[] array, int begin) {
    return slice(array, begin, array.length);
  }

  public static <T> T[] copy(T[] a) {
    T[] copy = (T[]) Array.newInstance(a.getClass().getComponentType(),
        a.length);
    System.arraycopy(a, 0, copy, 0, a.length);
    return copy;
  }

  public static <T> T[] merge(T[] a, T... b) {
    T[] merged = (T[]) Array.newInstance(a.getClass().getComponentType(),
        a.length + b.length);
    System.arraycopy(a, 0, merged, 0, a.length);
    System.arraycopy(b, 0, merged, a.length, b.length);
    return merged;
  }

  /**
   * Vereint alle Collections in einer neuen Liste.
   * 
   * @return ArrayList
   */
  public static <E> List<E> merge(Collection<Collection<E>> collections) {
    ArrayList<E> merged = new ArrayList<E>();
    for (Collection<E> c : collections)
      merged.addAll(c);
    return merged;
  }

  /**
   * Vereint alle Collections in einer neuen Liste.
   * 
   * @return ArrayList
   */
  public static <E> List<E> merge(Collection<E>... collections) {
    return merge(new ArrayBackedList<Collection<E>>(collections));
  }

  /**
   * Vereint Collection und Array in einer neuen Liste.
   * 
   * @param collection
   *          darf <i>null</i> sein
   * @param array
   *          darf <i>null</i> sein
   * @return ArrayList
   */
  public static <E> List<E> merge(Collection<E> collection, E... array) {
    int size = collection != null ? collection.size() : 0;
    size += array != null ? array.length : 0;
    ArrayList<E> merged = new ArrayList<E>(size);
    CollectionSupport.addAll(merged, collection);
    CollectionSupport.addAll(merged, array);
    return merged;
  }

  /**
   * Vereint alle Maps in einer neuen. Doppelte Keys überschreiben sich in der
   * Reihenfolge, in der sie in der Collection vorkommen.
   * 
   * @return HashMap
   */
  public static <K, V> Map<K, V> merge(Collection<Map<K, V>> maps) {
    Map<K, V> merged = new HashMap<K, V>();
    for (Map<K, V> m : maps)
      merged.putAll(m);
    return merged;
  }

  /**
   * Vereint alle Maps in einer neuen. Doppelte Keys überschreiben sich in der
   * Reihenfolge, in der sie übergeben werden.
   * 
   * @return HashMap
   */
  public static <K, V> Map<K, V> merge(Map<K, V>... maps) {
    return merge(new ArrayBackedList<Map<K, V>>(maps));
  }

  /**
   * Vereint alle Properties in einem. Doppelte Keys überschreiben sich in der
   * Reihenfolge, in der sie in der Collection vorkommen.
   * 
   * @return die vereinten Properties
   */
  public static Properties merge(Collection<Properties> props) {
    Properties merged = new Properties();
    for (Properties p : props)
      merged.putAll(p);
    return merged;
  }

  /**
   * Vereint alle Properties in einem. Doppelte Keys überschreiben sich in der
   * Reihenfolge, in der sie übergeben werden.
   * 
   * @param props
   *          darf <i>null</i> sein
   * @return die vereinten Properties
   */
  public static Properties merge(Properties... props) {
    return merge(new ArrayBackedList<Properties>(props));
  }

  /**
   * Erzeugt aus <var>map</var> eine neue Map, in der Keys und Values vertauscht
   * sind.
   * 
   * @return HashMap
   */
  public static <K, V> Map<V, K> swap(Map<K, V> map) {
    Map<V, K> swapped = new HashMap<V, K>();
    for (Map.Entry<K, V> e : map.entrySet())
      swapped.put(e.getValue(), e.getKey());
    return swapped;
  }

  /**
   * Zählt die Vorkommen von <i>elem</i> in <i>array</i>.
   * 
   * @param elem
   *          darf null sein
   * @param array
   *          darf nicht null sein, wohl aber seine Elemente
   */
  public static int count(Object[] array, Object elem) {
    Assert.notNull(array, "array");
    //
    int count = 0;
    for (Object e : array)
      if ((e != null && e.equals(elem)) || e == elem)
        count++;
    return count;
  }

  /**
   * Liefert das "erste" Element von 'o'. Bei Arrays und Listen ist es das erste
   * Element der jeweiligen Struktur. Ansonsten wird einfach 'o' selbst
   * zurückgegeben.
   * 
   * @param o
   *          Object | Object[] | List
   */
  public static Object first(Object o) {
    if (o == null)
      return null;
    if (o.getClass().isArray())
      return first((Object[]) o);
    if (o instanceof List)
      return first((List<?>) o);
    return o;
  }

  /**
   * Liefert das erste Element des Arrays oder null.
   * 
   * @param array
   *          darf null sein
   */
  public static <T> T first(T[] array) {
    return array != null && array.length > 0 ? array[0] : null;
  }

  /**
   * Liefert das erste Element der Liste oder null.
   * 
   * @param list
   *          darf null sein
   */
  public static <T> T first(List<T> list) {
    return list != null && list.size() > 0 ? list.get(0) : null;
  }

  /**
   * Liefert das letzte Element des Arrays oder null.
   * 
   * @param array
   *          darf null sein
   */
  public static <T> T last(T[] array) {
    return array != null && array.length > 0 ? array[array.length - 1] : null;
  }

  /**
   * Liefert das letzte Element der Liste oder null.
   * 
   * @param list
   *          darf null sein
   */
  public static <T> T last(List<T> list) {
    return list != null && list.size() > 0 ? list.get(list.size() - 1) : null;
  }

  /**
   * Liefert den letzten Index des Arrays, also array.length - 1.
   */
  public static int lastIndex(Object[] array) {
    return array.length - 1;
  }

  /**
   * Liefert das nächste Elemente des Iterators oder null, falls es keines mehr
   * gibt.
   */
  public static <T> T next(Iterator<T> iterator) {
    Assert.notNull(iterator, "iterator");
    return iterator.hasNext() ? iterator.next() : null;
  }

  /**
   * Liefert alle String-Repräsentierungen der Elemente aus <i>c</i> in
   * Kleinschrift.
   */
  public static Collection<String> lowerCase(Collection<?> c) {
    Collection<String> lc = new ArrayList<String>(c.size());
    for (Object o : c)
      lc.add(o.toString().toLowerCase());
    return lc;
  }

  /**
   * Liefert alle String-Repräsentierungen der Elemente aus <i>c</i> in
   * Großschrift.
   */
  public static Collection<String> upperCase(Collection<?> c) {
    Collection<String> lc = new ArrayList<String>(c.size());
    for (Object o : c)
      lc.add(o.toString().toUpperCase());
    return lc;
  }

  /**
   * Returns a new collection containing only elements that match a certain
   * predicate.
   * 
   * @param c
   *          the source collection
   * @param p
   *          the predicate. Return <code>false</code> to discard the element
   * @return the filtered collection
   */
  public static <T> List<T> grep(Collection<T> c, Predicate<T> p) {
    List<T> grepped = new ArrayList<T>(c.size());
    int i = 0;
    for (T e : c) {
      if (p.evaluate(e, i))
        grepped.add(e);
      i++;
    }
    return grepped;
  }

  /**
   * Predicate for elements.
   * 
   * @param <T>
   *          type of the objects this predicate
   */
  public interface Predicate<T> {

    /**
     * Evaluates an object.
     * 
     * @param object
     *          the object
     * @param index
     *          iteration index of the object
     */
    boolean evaluate(T object, int index);
  }
}
