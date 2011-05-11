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

package org.opencastproject.metadata.api.util;

import java.util.Date;

public abstract class Interval {

  private Interval() {
  }

  /**
   * Constructor function.
   */
  public static Interval closedInterval(final Date leftBound, final Date rightBound) {
    return new Interval() {
      @Override
      public boolean isLeftOpen() {
        return false;
      }

      @Override
      public boolean isRightOpen() {
        return false;
      }

      @Override
      public <A> A fold(Match<A> visitor) {
        return visitor.closed(leftBound, rightBound);
      }
    };
  }

  /**
   * Constructor function.
   */
  public static Interval leftOpenInterval(final Date rightBound) {
    return new Interval() {
      @Override
      public boolean isLeftOpen() {
        return true;
      }

      @Override
      public boolean isRightOpen() {
        return false;
      }

      @Override
      public <A> A fold(Match<A> visitor) {
        return visitor.leftOpen(rightBound);
      }
    };
  }

  public static Interval rightOpenInterval(final Date leftBound) {
    return new Interval() {
      @Override
      public boolean isLeftOpen() {
        return false;
      }

      @Override
      public boolean isRightOpen() {
        return true;
      }

      @Override
      public <A> A fold(Match<A> visitor) {
        return visitor.rightOpen(leftBound);
      }
    };
  }

  public static Interval fromValues(final Date leftBound, final Date rightBound) {
    if (leftBound != null && rightBound != null)
      return closedInterval(leftBound, rightBound);
    if (leftBound != null)
      return rightOpenInterval(leftBound);
    if (rightBound != null)
      return leftOpenInterval(rightBound);
    throw new IllegalArgumentException("Please give at least one bound");
  }

  public boolean isClosed() {
    return !(isLeftOpen() || isRightOpen());
  }

  public abstract boolean isLeftOpen();

  public abstract boolean isRightOpen();

  /**
   * Safe decomposition.
   */
  public abstract <A> A fold(Match<A> visitor);

  public interface Match<A> {
    A closed(Date leftBound, Date rightBound);

    A leftOpen(Date rightBound);

    A rightOpen(Date leftBound);
  }

}
