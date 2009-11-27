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
package org.opencastproject.scheduler.impl;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.impl.dao.SchedulerServiceImplDAO;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchedulerServiceImplTest {
  private SchedulerService service = null;
  private static final String storageRoot = "target" + File.separator + "scheduler-test-db";

  private Connection connectToDatabase(File storageDirectory) {
    if (storageDirectory == null) {
      storageDirectory = new File(File.separator + "tmp" + File.separator +"opencast" + File.separator + "scheduler-db");
    }
    String jdbcUrl = "jdbc:derby:" + storageDirectory.getAbsolutePath();
    String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    try {
      Class.forName(driver).newInstance();      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Connection conn = null;
    try {
      Properties props = new Properties();
      conn = DriverManager.getConnection(jdbcUrl + ";create=true", props);
    } catch (SQLException e) {
    }
    return conn;
  }    
  
  @Before
  public void setup() {
    // Clean up database
    try { 
      FileUtils.deleteDirectory(new File(storageRoot));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }    
    service = new SchedulerServiceImplDAO(connectToDatabase(new File(storageRoot)));
  }

  @After
  public void teardown() {
    service = null;
  }
  
  @Test
  public void testEventManagement() {
    SchedulerEvent event = new SchedulerEventImpl();
    event.setID("rrolf1001"); 
    event.setTitle("new recording"); 
    event.setStartdate(new Date (System.currentTimeMillis())); 
    event.setEnddate(new Date (System.currentTimeMillis()+50000));
    event.setLocation("42/201");
    SchedulerEvent eventUpdated = service.addEvent(event);
    Assert.assertEquals(service.getEvent(eventUpdated.getID()).getLocation(), event.getLocation());
    Assert.assertNotNull(service.getEvents(null));
    service.removeEvent(eventUpdated.getID());
    Assert.assertNull(service.getEvent(eventUpdated.getID()));
    // TODO Need to improve this, when this is no longer a service stub
  }
}