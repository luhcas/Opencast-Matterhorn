/*
 * Copyright 2009, 2010 The Regents of the University of California
 * Licensed under the Educational Community License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opencast.metadata.api.util;

import org.junit.Test;
import org.opencastproject.metadata.api.util.Interval;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.metadata.api.util.Interval.closedInterval;
import static org.opencastproject.metadata.api.util.Interval.leftOpenInterval;
import static org.opencastproject.metadata.api.util.Interval.rightOpenInterval;

public class IntervalTest {

  @Test
  public void testInterval() {
    Interval closed = closedInterval(new Date(), new Date());
    assertTrue(closed.isClosed());
    assertFalse(closed.isLeftOpen());
    assertFalse(closed.isRightOpen());
    Interval.Match<Integer> visitor = new Interval.Match<Integer>() {
      @Override
      public Integer closed(Date leftBound, Date rightBound) {
        return 1;
      }

      @Override
      public Integer leftOpen(Date rightBound) {
        return 2;
      }

      @Override
      public Integer rightOpen(Date leftBound) {
        return 3;
      }
    };
    assertSame(1, closed.fold(visitor));
    Interval leftOpen = leftOpenInterval(new Date());
    assertFalse(leftOpen.isClosed());
    assertTrue(leftOpen.isLeftOpen());
    assertFalse(leftOpen.isRightOpen());
    assertSame(2, leftOpen.fold(visitor));
    Interval rightOpen = rightOpenInterval(new Date());
    assertFalse(rightOpen.isClosed());
    assertFalse(rightOpen.isLeftOpen());
    assertTrue(rightOpen.isRightOpen());
    assertSame(3, rightOpen.fold(visitor));
  }
}
