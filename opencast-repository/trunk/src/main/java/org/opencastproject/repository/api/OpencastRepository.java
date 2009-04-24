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
package org.opencastproject.repository.api;

import java.util.Map;

/**
 * Provides access to the Opencast Repository
 */
public interface OpencastRepository {
  /**
   * Gets an object from the repository.
   * 
   * @param type The {@link Class} of object expected
   * @param path The path to the object in the repository
   * @return
   */
  <T> T getObject(Class<T> type, String path);
  
  /**
   * Gets the types of object this repository can store
   * 
   * @param <T>
   * @return An array 
   */
  Class<?>[] getSupportedTypes();

  /**
   * @param path
   * @return Whether the repository contains an object at this path
   */
  boolean hasObject(String path);

  /**
   * Stores an object in the repository at a specific path.  This will clobber any existing object
   * stored at this path.
   * 
   * @param object The object to store
   * @param path The path in the repository to store the object.
   * @return The unique ID of this object in the repository.
   */
  String putObject(Object object, String path);
  
  /**
   * Puts a key/value pair onto an object in the repository
   * 
   * @param value The value to be stored
   * @param key The key under which the value is stored
   * @param path The path to the object in the repository
   */
  void putMetadata(String value, String key, String path);
  
  /**
   * Gets the metadata key/value pairs from the object at path.
   * 
   * @param path
   * @return
   */
  Map<String, String> getMetadata(String path);
}