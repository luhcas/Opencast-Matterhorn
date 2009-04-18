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

import java.io.Serializable;
import java.util.Calendar;

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
   * Gets a version of an object from the repository.
   * 
   * @param type The {@link Class} of object expected
   * @param path The path to the object in the repository
   * @param version The desired version of the object
   * @return
   */
//  <T> T getObject(Class<T> type, String path, int version);

  /**
   * Gets the times that objects have been saved at this path
   * 
   * @param path
   * @return
   */
//  Calendar[] getVersionTimes(String path);
  
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
   * Stores an object in the repository
   * 
   * @param object The object to store
   * @param path The path in the repository to store the object.
   * 
   * @return The version of the object stored at the specified path
   */
//  int putObject(Object object, String path);

  /**
   * Stores an object in the repository
   * 
   * @param object The object to store
   * @param path The path in the repository to store the object.
   */
  void putObject(Object object, String path);
}