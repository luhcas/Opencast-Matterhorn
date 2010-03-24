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
package org.opencastproject.feedback.api;

/**
 * Provides annotation capabilities, possibly to the engage tools, possibly to other services.
 */
public interface FeedbackService {

  /**
   * Adds a new annotation to the database and returns the event with an updated annotation-id, to make sure the
   * annotation-id stays unique
   * 
   * @param a
   *          The Annotation that will be added to the database
   * @return the updated annotation, with a new ID. null if there are errors in adding the annotation.
   */
  Annotation addAnnotation(Annotation a);

  /**
   * Returns annotations of a give key
   * 
   * @param key
   *          The annotation key
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByKey(String key, int offset, int limit);
}