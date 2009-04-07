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
import java.util.List;

/** @author Christoph E. Driessen <ced@neopoly.de> */
public class ArrayBuilder<E> {

  private List<E> array = new ArrayList<E>() {
  };

  private Class<E> arrayType;

  public ArrayBuilder() {
  }

  public ArrayBuilder(Class<E> arrayType) {
    this.arrayType = arrayType;
  }

  public ArrayBuilder<E> add(E e) {
    array.add(e);
    return this;
  }

  public ArrayBuilder<E> add(Collection<E> e) {
    array.addAll(e);
    return this;
  }

  public ArrayBuilder<E> add(E... e) {
    for (E f : e) {
      array.add(f);
    }
    return this;
  }

  public E[] toArray() {
    return array.toArray((E[]) Array.newInstance(arrayType != null ? arrayType
        : Object.class, array.size()));
  }
}
