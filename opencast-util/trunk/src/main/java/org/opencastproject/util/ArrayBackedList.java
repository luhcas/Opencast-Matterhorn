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

package org.opencastproject.util;

import java.util.AbstractList;

/**
 * Wraps an array into a list for reading. Changes to the array are reflected by
 * the list. Write operations are not supported. This class allows arrays to be
 * used in situations where a list is requested.
 *
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class ArrayBackedList<E> extends AbstractList<E> {

  private E[] array;

  /**
   * Creates a new wrapper.
   *
   * @param array
   *          may not be null
   */
  public ArrayBackedList(E[] array) {
    if (array != null)
      this.array = array;
    else
      this.array = (E[]) new Object[0];
  }

  @Override
  public E get(int index) {
    return array[index];
  }

  @Override
  public int size() {
    return array.length;
  }
}
