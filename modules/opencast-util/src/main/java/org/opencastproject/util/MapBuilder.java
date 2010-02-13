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

import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@link java.util.Map}s on the fly.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class MapBuilder<K, V> {

  private Map<K, V> map;

  /**
   * Use {@link java.util.HashMap} as Map implementation.
   */
  public MapBuilder() {
    map = new HashMap<K, V>();
  }

  /**
   * Provide your own Map implementation.
   */
  public MapBuilder(Map<K, V> map) {
    this.map = map;
  }

  /**
   * Puts a value under a key.
   */
  public MapBuilder<K, V> put(K key, V value) {
    map.put(key, value);
    return this;
  }

  /**
   * Includes the map.
   */
  public MapBuilder<K, V> putAll(Map<K, V> map) {
    this.map.putAll(map);
    return this;
  }

  /**
   * Puts a value under multiple keys.
   */
  public MapBuilder<K, V> putMultiple(V value, K... keys) {
    for (K key : keys)
      map.put(key, value);
    return this;
  }

  /**
   * Returns the built map.
   */
  public Map<K, V> toMap() {
    return map;
  }
}
