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
package org.opencastproject.media.mediapackage.selector;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElementSelector;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This selector will return any <code>MediaPackageElement</code>s from a
 * <code>MediaPackage</code> that matches the tag and flavors.
 */
public class SimpleMediaPackageElementSelector<T extends MediaPackageElement>
        implements MediaPackageElementSelector<T> {

  /** The tags */
  protected Set<String> tags = new HashSet<String>();

  /** The flavors */
  protected List<MediaPackageElementFlavor> flavors = new ArrayList<MediaPackageElementFlavor>();

  /**
   * This base implementation will return those media package elements that
   * match the type specified as the type parameter to the class and that flavor
   * (if specified) and at least one of the tags (if specified) match.
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackageElementSelector#select(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @SuppressWarnings("unchecked")
  public Collection<T> select(MediaPackage mediaPackage) {
    Set<T> result = new HashSet<T>();
    Class type = getParametrizedType(result);
    for (MediaPackageElement e : mediaPackage.getElements()) {

      // Does the type match?
      if (type.isAssignableFrom(e.getClass())) {

        // Any of the flavors?
        if (flavors.size() > 0 && !flavors.contains(e.getFlavor()))
          continue;

        // What about tags?
        if (tags.size() > 0) {
          boolean anyTag = false;
          for (String tag : tags)
            if (e.containsTag(tag)) {
              anyTag = true;
              break;
            }
          if (!anyTag)
            continue;
        }

        // Match!
        result.add((T) e);
      }
    }

    return result;
  }

  /**
   * This constructor tries to determine the entity type from the type argument
   * used by a concrete implementation of <code>GenericHibernateDao</code>.
   * <p>
   * Note: This code will only work for immediate specialization, and especially not
   * for subclasses.
   */
  @SuppressWarnings("unchecked")
  private Class getParametrizedType(Object object) {
    Class<T> c = (Class<T>)this.getClass();
    ParameterizedType type = ((ParameterizedType) c.getGenericSuperclass());
    Class<T> actualType = (Class<T>)type.getActualTypeArguments()[0];
    return actualType;

    // Class current = getClass();
    // Type superclass;
    // Class<? extends T> entityClass = null;
    // while ((superclass = current.getGenericSuperclass()) != null) {
    // if (superclass instanceof ParameterizedType) {
    // entityClass = (Class<T>) ((ParameterizedType) superclass)
    // .getActualTypeArguments()[0];
    // break;
    // } else if (superclass instanceof Class) {
    // current = (Class) superclass;
    // } else {
    // break;
    // }
    // }
    // if (entityClass == null) {
    // throw new IllegalStateException("Cannot determine entity type because "
    // + getClass().getName() + " does not specify any type parameter.");
    // }
    // return entityClass;
  }

  /**
   * This constructor tries to determine the entity type from the type argument
   * used by a concrete implementation of <code>GenericHibernateDao</code>.
   * <p>
   * Note: This code will only work for immediate specialization, and especially not
   * for subclasses.
   */
  @SuppressWarnings("unchecked")
  private Class getParametrizedType() {
    Class c = getClass();
    ParameterizedType type = ((ParameterizedType) c.getGenericSuperclass());
    Class<T> actualType = (Class<T>)type.getActualTypeArguments()[0];
    return actualType;

    // Class current = getClass();
    // Type superclass;
    // Class<? extends T> entityClass = null;
    // while ((superclass = current.getGenericSuperclass()) != null) {
    // if (superclass instanceof ParameterizedType) {
    // entityClass = (Class<T>) ((ParameterizedType) superclass)
    // .getActualTypeArguments()[0];
    // break;
    // } else if (superclass instanceof Class) {
    // current = (Class) superclass;
    // } else {
    // break;
    // }
    // }
    // if (entityClass == null) {
    // throw new IllegalStateException("Cannot determine entity type because "
    // + getClass().getName() + " does not specify any type parameter.");
    // }
    // return entityClass;
  }

  /**
   * Sets the flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by
   * this selector.
   * 
   * @param flavors
   *          the list of flavors
   * @throws IllegalArgumentException
   *           if the flavors list is <code>null</code>
   */
  public void setFlavors(List<MediaPackageElementFlavor> flavors) {
    if (flavors == null)
      throw new IllegalArgumentException("List of flavors must not be null");
    this.flavors = flavors;
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by
   * this selector.
   * 
   * @param flavor
   */
  public void addFlavor(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.add(flavor);
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by
   * this selector.
   * 
   * @param flavor
   */
  public void addFlavor(String flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.add(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by
   * this selector.
   * 
   * @param index
   *          the position in the list
   * @param flavor
   *          the flavor to add
   */
  public void addFlavorAt(int index, MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.add(index, flavor);
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by
   * this selector.
   * 
   * @param index
   *          the position in the list
   * @param flavor
   *          the flavor to add
   */
  public void addFlavorAt(int index, String flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.add(index, MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Removes all occurences of the given flavor from the list of flavors.
   * 
   * @param flavor
   *          the flavor to remove
   */
  public void removeFlavor(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.remove(flavor);
  }

  /**
   * Removes all occurences of the given flavor from the list of flavors.
   * 
   * @param flavor
   *          the flavor to remove
   */
  public void removeFlavor(String flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.remove(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Removes all occurences of the given flavor from the list of flavors.
   * 
   * @param index
   *          the position in the list
   */
  public void removeFlavorAt(int index) {
    flavors.remove(index);
  }

  /**
   * Adds <code>tag</code> to the list of tags that are used to select the
   * media.
   * 
   * @param tag
   *          the tag to include
   */
  public void includeTag(String tag) {
    tags.add(tag);
  }

  /**
   * Adds <code>tag</code> to the list of tags that are used to select the
   * media.
   * 
   * @param tag
   *          the tag to include
   */
  public void excludeTag(String tag) {
    tags.remove(tag);
  }

  /**
   * Removes all of the tags from this selector.
   */
  public void clearTags() {
    tags.clear();
  }

}
