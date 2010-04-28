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
package org.opencastproject.scheduler.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.scheduler.impl.jpa.Event;
import org.opencastproject.scheduler.impl.jpa.IncompleteDataException;
import org.opencastproject.scheduler.impl.jpa.Metadata;
import org.opencastproject.scheduler.impl.jpa.RecurringEvent;
import org.opencastproject.scheduler.impl.jpa.SchedulerServiceImplJPA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import junit.framework.Assert;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchedulerServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImplTest.class);
  
  private SchedulerService service = null;
  private SchedulerServiceImplJPA serviceJPA = null;
  private static final String storageRoot = "target" + File.separator + "scheduler-test-db";
  private static final String resourcesRoot = "src" + File.separator + "main" + File.separator + "resources";
  
  private SchedulerEvent event;
  private DataSource datasource;

  private DataSource connectToDatabase(File storageDirectory) {
    if (storageDirectory == null) {
      storageDirectory = new File(File.separator + "tmp" + File.separator +"opencast" + File.separator + "scheduler-db");
    }
      JdbcConnectionPool cp = JdbcConnectionPool.create("jdbc:h2:" + storageDirectory + ";LOCK_MODE=1;MVCC=TRUE", "sa", "sa");
    return cp;
  }    
  
  @Before
  public void setup() {
    // Clean up database
    try { 
      FileUtils.deleteDirectory(new File(storageRoot));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }    
    datasource = connectToDatabase(new File(storageRoot));
    // set Metadata Mapping Files. This depends on if its called from OSGI or in a regular case.
    
    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", datasource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");
//    
    serviceJPA = new SchedulerServiceImplJPA();
    serviceJPA.setPersistenceProvider(new PersistenceProvider());
    serviceJPA.setPersistenceProperties(props);
//    try {
//      serviceJPA.setDataSource(datasource);
//    } catch (SQLException e1) {
//      Assert.fail("Could not connect to Database");
//    }
    serviceJPA.activate(null);    
    
    service = serviceJPA;

    try {
      ((SchedulerServiceImpl)service).setDublinCoreGenerator(new DublinCoreGenerator(new FileInputStream(resourcesRoot+ File.separator+"config"+File.separator+"dublincoremapping.properties")));
      ((SchedulerServiceImpl)service).setCaptureAgentMetadataGenerator(new CaptureAgentMetadataGenerator(new FileInputStream(resourcesRoot+ File.separator+"config"+File.separator+"captureagentmetadatamapping.properties")));
    } catch (FileNotFoundException e) {
      Assert.fail(e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
    event = new SchedulerEventImpl();
    event.setID(event.createID()); 
    event.setTitle("new recording");
    event.setStartdate(new Date (System.currentTimeMillis())); 
    event.setEnddate(new Date (System.currentTimeMillis()+5000000));
    event.setLocation("testlocation");
    event.setDevice("testrecorder");
    event.setCreator("test lecturer");
    event.addResource("vga");
    event.setAbstract("a test description");
    event.setChannelID("unittest");
    event.setSeriesID("testevents");    
  }

  @After
  public void teardown() {
    service = null;
    serviceJPA.destroy();
    serviceJPA = null;
  }
  
  @Test
  public void testPersistence () {
    SchedulerEvent eventStored = serviceJPA.addEvent(event);
    Assert.assertNotNull(eventStored);
    Assert.assertNotNull(eventStored.getID());
    SchedulerEvent eventLoaded = serviceJPA.getEvent(eventStored.getID());
    Assert.assertEquals(eventStored, eventLoaded);
    
    Event eventModified = serviceJPA.getEventJPA(eventLoaded.getID());
    logger.info("State of the loaded event {}.", eventModified);
    
    eventModified.getMetadata().add(new Metadata("stupid.unused.key","no matter what"));
    for (int i = 0; i < eventModified.getMetadata().size(); i++) {
      if (eventModified.getMetadata().get(i).getKey().equals("creator") || eventModified.getMetadata().get(i).getKey().equals("series-id")) 
        eventModified.getMetadata().remove(i);
    }
    
    serviceJPA.updateEvent(eventModified);
    
    Event eventReloaded = serviceJPA.getEventJPA(eventModified.getEventId());
    
    logger.info("State of the updated event {}.", eventReloaded);
  }
  
  @Test
  public void testEventManagement() {
    
    // add event
    SchedulerEvent eventUpdated = service.addEvent(event);
    Assert.assertNotNull(eventUpdated);
    Assert.assertNotNull(eventUpdated.getID());
    
    // retrieve event
    SchedulerEvent loadedEvent = service.getEvent(eventUpdated.getID());
    logger.debug("loaded: {} ",loadedEvent);
    Assert.assertEquals(loadedEvent.getLocation(), event.getLocation());
    Assert.assertEquals(loadedEvent.getStartdate(), event.getStartdate());
    Assert.assertEquals(loadedEvent.getEnddate(), event.getEnddate());
    Assert.assertEquals(loadedEvent.getContributor(), event.getContributor());
    Assert.assertEquals(loadedEvent.getCreator(), event.getCreator());
    Assert.assertEquals(loadedEvent.getDevice(), event.getDevice());
    Assert.assertEquals(loadedEvent.getTitle(), event.getTitle());
    
    //test iCalender export
    CalendarBuilder calBuilder = new CalendarBuilder();
    Calendar cal;
    try {
      String icalString = service.getCalendarForCaptureAgent("testrecorder");
      cal = calBuilder.build(IOUtils.toInputStream(icalString));
      ComponentList vevents = cal.getComponents(VEvent.VEVENT);
      for (int i = 0; i < vevents.size(); i++) {
        PropertyList attachments = ((VEvent)vevents.get(i)).getProperties(Property.ATTACH);
        for (int j = 0; j < attachments.size(); j++) {
          String attached = ((Property)attachments.get(j)).getValue();
          String filename = ((Property)attachments.get(j)).getParameter("X-APPLE-FILENAME").getValue();
          attached = new String (Base64.decodeBase64(attached));
          if (filename.equals("agent.properties")) {
            Assert.assertTrue(attached.contains("capture.device.id="+event.getDevice()));
            Assert.assertTrue(attached.contains("event.title="+event.getTitle()));
          }
          if (filename.equals("metadata.xml")) {
            Assert.assertTrue(attached.contains(event.getLocation()));
            Assert.assertTrue(attached.contains(event.getTitle()));
          }
          logger.info("iCal attachment checked: {}", filename);
        }
      }
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } catch (ParserException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
    
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }
    
    // test for upcoming events (it should not be in there). Not in use currently because of changes in design. 
    SchedulerEvent[] upcoming = service.getUpcomingEvents();
    boolean eventFound = false;
//    for (int i = 0; i < upcoming.length; i++ ) 
//      if (upcoming[i].equals(eventUpdated)) eventFound= true; 
//    Assert.assertFalse(eventFound);    
    
    // test if event is in list in general 
    SchedulerEvent[] allEvents = service.getEvents(null);
    eventFound = false;
    Assert.assertTrue(allEvents.length > 0);
    for (int i = 0; i < allEvents.length; i++ ) 
      if (allEvents[i].equals(eventUpdated)) eventFound= true; 
    Assert.assertTrue(eventFound); 
    
    //test event filter (positive test)
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setDeviceFilter("testrecorder");
    allEvents = service.getEvents(filter);
    Assert.assertTrue(allEvents.length > 0);
    eventFound = false;
    for (int i = 0; i < allEvents.length; i++ ) 
      if (allEvents[i].equals(eventUpdated)) eventFound= true; 
    Assert.assertTrue(eventFound);    
    
    //test event filter (negative test)
    filter = new SchedulerFilterImpl();
    filter.setDeviceFilter("something");
    allEvents = service.getEvents(filter);
    eventFound = false;
    for (int i = 0; i < allEvents.length; i++ ) {    
      if (allEvents[i].equals(eventUpdated)) {
        Assert.fail("FALSE EVENT:  filtered for "+filter+" in" + allEvents[i]);
      }
    }
    Assert.assertFalse(eventFound);    
    
    // update event
    eventUpdated.setEnddate(new Date(System.currentTimeMillis()+900000));
    eventUpdated.setStartdate(new Date(System.currentTimeMillis()+20000));
    eventUpdated.setContributor("Matterhorn");
    Assert.assertTrue(service.updateEvent(eventUpdated));
    Assert.assertEquals(service.getEvent(eventUpdated.getID()).getContributor(), "Matterhorn");
    Assert.assertNotNull(service.getEvents(null));
    
    // test for upcoming events (now it should be there
    upcoming = service.getUpcomingEvents();
    Assert.assertTrue(upcoming.length > 0);
    eventFound = false;
    for (int i = 0; i < upcoming.length; i++ ) 
      if (upcoming[i].equals(eventUpdated)) eventFound= true; 
    Assert.assertTrue(eventFound);
    
    //delete event
    service.removeEvent(eventUpdated.getID());
    Assert.assertNull(service.getEvent(eventUpdated.getID()));   
  }
  
  @Test
  public void testRESTDocs () {
    SchedulerRestService restService = new SchedulerRestService();
    restService.setService(service);
    restService.activate(null);
    
    Assert.assertNotNull(restService.getDocumentation());
  }
  
  @Test
  public void testMetadataExport () {
    service.addEvent(event);
    String dc = service.getDublinCoreMetadata(event.getID());
    System.out.println("DC-String: "+dc);
    Assert.assertNotNull(dc);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
      Document doc =  builder.parse(IOUtils.toInputStream(dc));
      XPath xPath = XPathFactory.newInstance().newXPath();
      Assert.assertEquals(xPath.evaluate("/dublincore/creator", doc), "test lecturer");
    } catch (Exception e1) {
      e1.printStackTrace();
      Assert.fail();
    }
    
    
    
    String ca = service.getCaptureAgentMetadata(event.getID());
    Assert.assertNotNull(ca);
    Properties p = new Properties();
    try {
      p.load(new StringReader(ca));
      Assert.assertTrue(p.get("event.title").equals("new recording"));
      Assert.assertTrue(p.get("event.series").equals("testevents"));
      Assert.assertTrue(p.get("event.source").equals("unittest"));
      Assert.assertTrue(p.get("capture.device.location").equals("testlocation"));
    } catch (IOException e) {
      Assert.fail();
    }
    service.removeEvent(event.getID());
  }
  
  @Test
  public void testRecurrence () {
    RecurringEvent recurringEvent = new RecurringEvent("FREQ=WEEKLY;BYDAY=TU");
    GregorianCalendar now = new GregorianCalendar();
    GregorianCalendar start = new GregorianCalendar(now.get(GregorianCalendar.YEAR)+1, 1, 1);
    GregorianCalendar end = new GregorianCalendar(now.get(GregorianCalendar.YEAR)+1, 6, 1);
    recurringEvent.getMetadata().add(new Metadata("", ""+start.getTimeInMillis()));
    recurringEvent.getMetadata().add(new Metadata("recurrence.end", ""+end.getTimeInMillis()));
    recurringEvent.getMetadata().add(new Metadata("recurrence.duration", "900000"));
    recurringEvent.getMetadata().add(new Metadata("title", "recurrence test title"));
    
    RecurringEvent storedEvent = serviceJPA.addRecurringEvent(recurringEvent);
    
    Assert.assertNotNull(storedEvent);
    Assert.assertNotNull(storedEvent.getRecurringEventId());
    
    RecurringEvent loadedEvent = serviceJPA.getRecurringEvent(storedEvent.getRecurringEventId());
    
    Assert.assertNotNull(loadedEvent);
    Assert.assertEquals(storedEvent.getValueAsDate("recurrence.start"), loadedEvent.getValueAsDate("recurrence.start"));
    
    Assert.assertNotNull(loadedEvent.getEvents());
    
    Event oldEvent = null;
    for (Event e : loadedEvent.getEvents()) {
      Assert.assertTrue(e.getStartdate().before(e.getEnddate()));
      try {
        Assert.assertTrue(e.getValue("title").equals(loadedEvent.getValue("title")));
      } catch (IncompleteDataException e1) {
        Assert.fail("Recurring event Metadata could not be processed");
      }
      if (oldEvent != null) {
        Assert.assertTrue(oldEvent.getStartdate().before(e.getStartdate()));
      }
      oldEvent = e;
    }
    
    
  }
  
}