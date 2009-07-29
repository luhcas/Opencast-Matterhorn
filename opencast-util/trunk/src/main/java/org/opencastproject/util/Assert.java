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

/**
 * Assertions.
 *
 * @author Christoph Drießen
 */
public class Assert {

  private Assert() {
  }

  /**
   * Assert that a value is not null.
   *
   * @param o
   *          the object to test
   * @param message
   *          an optional message. May be null
   * @throws IllegalArgumentException
   *           if o is null
   */
  public static void notNull(Object o, Object message) {
    if (o == null) {
      _throw(new IllegalArgumentException(message != null ? message.toString()
          : null));
    }
  }

  /**
   * Asserts that the object is not empty. The definition of "empty" depends on
   * the object's type. See
   * {@link ch.ethz.replay.core.common.util.Tool#empty(Object)} for details.
   *
   * @param o
   *          the object to test
   * @param message
   *          an optional message. May be null
   * @throws IllegalArgumentException
   *           if o is empty
   */
  public static void notEmpty(Object o, Object message) {
    if (Tool.empty(o)) {
      _throw(new IllegalArgumentException(message != null ? message.toString()
          : null));
    }
  }

  /**
   * Assert that a condition is true.
   *
   * @param condition
   *          the condition
   * @param message
   *          an optional message. May be null
   * @throws IllegalArgumentException
   *           if the condition is not true
   */
  public static void that(boolean condition, Object message) {
    if (!condition)
      _throw(new IllegalArgumentException(message.toString() != null ? message
          .toString() : null));
  }

  /**
   * Assert that a condition is false.
   *
   * @param condition
   *          the condition
   * @param message
   *          on optional message. May be null
   * @throws IllegalArgumentException
   *           if the condition is not true
   */
  public static void not(boolean condition, Object message) {
    if (condition)
      _throw(new IllegalArgumentException(message.toString() != null ? message
          .toString() : null));
  }

  /**
   * Assert that a value is in a certain range. Min and max values belong to the
   * range.
   *
   * @throws IllegalArgumentException
   *           if i is not between the bounds
   */
  public static void between(int i, int min, int max) {
    if (i < min || i > max)
      _throw(new IllegalArgumentException(i + " out of bounds: " + min + " - "
          + max));
  }

  /**
   * Correct stacktrace, so that the first element is the line where the
   * assertion is located.
   */
  private static <T extends Throwable> void _throw(T e) throws T {
    e.setStackTrace(CollectionSupport.slice(e.getStackTrace(), 1));
    throw e;
  }
}
