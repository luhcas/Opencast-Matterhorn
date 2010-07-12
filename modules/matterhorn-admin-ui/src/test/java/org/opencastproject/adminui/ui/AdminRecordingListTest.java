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
package org.opencastproject.adminui.ui;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.adminui.api.AdminRecording;
import org.opencastproject.adminui.api.AdminRecordingImpl;
import org.opencastproject.adminui.api.AdminRecordingList;
import org.opencastproject.adminui.api.AdminRecordingList.Field;
import org.opencastproject.adminui.api.AdminRecordingList.Order;
import org.opencastproject.adminui.api.AdminRecordingListImpl;

public class AdminRecordingListTest {

  private static AdminRecording a;
  private static AdminRecording b;
  private static AdminRecording c;

  /** Create test entities a, b and c so that for field title and startDate
   *  the a < b < c.
   */
  @BeforeClass
  public static void createTestEntities() {
    a = new AdminRecordingImpl();
    a.setTitle("AAA");
    a.setStartTime("0");
    b = new AdminRecordingImpl();
    b.setTitle("BBB");
    b.setStartTime("10");
    c = new AdminRecordingImpl();
    c.setTitle("CCC");
    c.setStartTime("200");
  }

  /** Test list sorting descending by date
   *
   */
  @Test
  public void sortDescByDate(){
    AdminRecordingList list = new AdminRecordingListImpl(Field.StartDate, Order.Descending);
    list.add(b);
    list.add(c);
    list.add(a);
    Assert.assertEquals(c, list.get(0));
    Assert.assertEquals(b, list.get(1));
    Assert.assertEquals(a, list.get(2));
  }

  /** Tests list sorting ascending by title
   *
   */
  @Test
  public void sortAscByTitle() {
    AdminRecordingList list = new AdminRecordingListImpl(Field.Title, Order.Ascending);
    list.add(b);
    list.add(a);
    list.add(c);
    Assert.assertEquals(a, list.get(0));
    Assert.assertEquals(b, list.get(1));
    Assert.assertEquals(c, list.get(2));
  }

}
