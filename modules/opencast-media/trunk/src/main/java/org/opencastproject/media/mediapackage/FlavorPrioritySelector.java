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
package org.opencastproject.media.mediapackage;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This selector will return one or zero <code>MediaPackageElements</code> from a <code>MediaPackage</code>, following
 * these rules:
 * <ul>
 * <li>Elements will be returned depending on tags that have been set</li>
 * <li>If no tags have been specified, all the elements will be taken into account</li>
 * <li>The result is one or zero elements</li>
 * <li>The element is selected based on the order of flavors</li>
 * </ul>
 */
public class FlavorPrioritySelector<T extends MediaPackageElement> implements MediaPackageElementSelector<T> {

  /** The tags */
  protected Set<String> tags = new HashSet<String>();

  /** The flavors */
  protected List<MediaPackageElementFlavor> flavors = new ArrayList<MediaPackageElementFlavor>();

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackageElementSelector#select(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @SuppressWarnings("unchecked")
  public T[] select(MediaPackage mediaPackage) {
    Set<T> candidates = new HashSet<T>();
    Set<T> result = new HashSet<T>();

    // Select the element candidates by looking at the tags
    if (tags.size() > 0) {
      for (String tag : tags) {
        MediaPackageElement[] elements = mediaPackage.getElementsByTag(tag);
        for (MediaPackageElement e : elements) {
          if (e.getClass().isAssignableFrom(getParametrizedType()))
            candidates.add((T) e);
        }
      }
    } else {
      MediaPackageElement[] elements = mediaPackage.getElements();
      for (MediaPackageElement e : elements) {
        if (e.getClass().isAssignableFrom(getParametrizedType()))
          candidates.add((T) e);
      }
    }

    result: for (MediaPackageElementFlavor flavor : flavors) {
      for (T element : candidates) {
        if (flavor.equals(element.getFlavor())) {
          result.add(element);
          break result;
        }
      }
    }

    return (T[]) result.toArray();
  }

  /**
   * This constructor tries to determine the entity type from the type argument used by a concrete implementation of
   * <code>GenericHibernateDao</code>.
   */
  @SuppressWarnings("unchecked")
  protected Class getParametrizedType() {
    Class current = getClass();
    Type superclass;
    Class<? extends T> entityClass = null;
    while ((superclass = current.getGenericSuperclass()) != null) {
      if (superclass instanceof ParameterizedType) {
        entityClass = (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
        break;
      } else if (superclass instanceof Class) {
        current = (Class) superclass;
      } else {
        break;
      }
    }
    if (entityClass == null) {
      throw new IllegalStateException("DAO creation exception: Cannot determine entity type because "
              + getClass().getName() + " does not specify any type parameter.");
    }
    return entityClass.getClass();
  }

  /**
   * Sets the flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by this selector.
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
   * Note that the order is relevant to the selection of the track returned by this selector.
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
   * Note that the order is relevant to the selection of the track returned by this selector.
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
   * @param index
   *          the position in the list
   */
  public void removeFlavorAt(int index) {
    flavors.remove(index);
  }

  /**
   * Adds <code>tag</code> to the list of tags that are used to select the media.
   * 
   * @param tag
   *          the tag to include
   */
  public void includeTag(String tag) {
    tags.add(tag);
  }

  /**
   * Adds <code>tag</code> to the list of tags that are used to select the media.
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
