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
package org.opencastproject.example.api;

/**
 * FIXME -- Add javadocs
 */
public interface ExampleService {
  /**
   * Gets an entity by ID.  Replace this method with your service operations.
   */
  ExampleEntity getExampleEntity(String id);

  /**
   * Adds an entity for a given ID.
   * 
   * @param id The ID of the entity
   * @param entity The entity to save
   */
  void saveExampleEntity(ExampleEntity entity);
}

