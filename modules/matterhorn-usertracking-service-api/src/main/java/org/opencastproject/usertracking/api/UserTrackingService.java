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
package org.opencastproject.usertracking.api;

/**
 * Provides annotation capabilities, possibly to the engage tools, possibly to other services.
 */
public interface UserTrackingService {

  /**
   * Adds a new annotation to the database and returns the event with an updated annotation-id, to make sure the
   * annotation-id stays unique
   * 
   * @param a
   *          The Annotation that will be added to the database
   * @return the updated annotation, with a new ID. NULL if there are errors while adding the annotation.
   */
  Annotation addAnnotation(Annotation a);

  /**
   * Returns annotations
   * 
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotations(int offset, int limit);
  
  /**
   * Returns annotations of a given key
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

  /**
   * Returns annotations of a given day (YYYYMMDD)
   * 
   * @param day
   *          The day in the format of YYYYMMDD
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByDay(String day, int offset, int limit);

  /**
   * Returns annotations of a given key and day
   * 
   * @param key
   *          the annotation key
   * @param day
   *          the day
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByKeyAndDay(String key, String day, int offset, int limit);

  /**
   * Returns annotations of a given key and mediapackage id
   * 
   * @param key
   *          the annotation key
   * @param mediapackageId
   *          the mediapackage id
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByKeyAndMediapackageId(String key, String mediapackageId, int offset, int limit);

  /**
   * Returns the views of a mediapackage
   * 
   * @param mediapackageId
   *          the mediapackeId
   * @return the views
   */
  int getViews(String mediapackageId);

  /**
   * Returns a report
   * 
   * @param from
   *          The from day key
   * @param to
   *          The to day key
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the report
   */
  Report getReport(String from, String to, int offset, int limit);

  /**
   * Returns a report
   * 
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the report
   */
  Report getReport(int offset, int limit);

  /**
   * Returns a list of footprints, if a userId is passed only the footprints of that user are returned.
   * 
   * @param mediapackageId
   *          The mediapackageId
   * @param userId
   *          The userId is optional
   * @return the footprintList
   */
  FootprintList getFootprints(String mediapackageId, String userId);


}
