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

import org.opencastproject.scheduler.impl.EventImpl;
import org.opencastproject.scheduler.impl.IncompleteDataException;
import org.opencastproject.scheduler.impl.MetadataImpl;
import org.opencastproject.scheduler.impl.RecurringEventImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.impl.SeriesImpl;
import org.opencastproject.series.impl.SeriesMetadataImpl;
import org.opencastproject.series.impl.SeriesServiceImpl;

import junit.framework.Assert;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

public class SchedulerServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImplTest.class);
  
  private SchedulerServiceImpl service = null;
  private static final String storageRoot = "target" + File.separator + "scheduler-test-db";
  private static final String resourcesRoot = "src" + File.separator + "main" + File.separator + "resources";
  
  private Event event;
  private DataSource datasource;
  
  private String seriesID;

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
    logger.info("----- Setting up tests -----");
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
    
    service = new SchedulerServiceImpl();
    service.setPersistenceProvider(new PersistenceProvider());
    service.setPersistenceProperties(props);
    
    SeriesServiceImpl seriesService = new SeriesServiceImpl ();
    seriesService.setPersistenceProvider(new PersistenceProvider());
    seriesService.setPersistenceProperties(props);
    seriesService.activate(null);
    service.setSeriesService(seriesService);
    logger.info("Adding new series...");
    Series series = new SeriesImpl();
    
    LinkedList<SeriesMetadata> metadata = new LinkedList<SeriesMetadata>();
    
    metadata.add(new SeriesMetadataImpl(series, "title", "demo title"));
    metadata.add(new SeriesMetadataImpl(series, "license", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "valid", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "publisher", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "creator", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "subject", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "temporal", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "audience", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "spatial", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "rightsHolder", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "extent", "3600000"));
    metadata.add(new SeriesMetadataImpl(series, "created", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "language", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "isReplacedBy", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "type", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "available", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "modified", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "replaces", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "contributor", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "description", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "issued", ""+System.currentTimeMillis()));
    
    series.setMetadata(metadata);
    seriesService.addSeries(series);
    
    // now that the series has been persisted, grab its ID
    seriesID = series.getSeriesId();

    service.activate(null);

    try {
      ((SchedulerServiceImpl)service).setDublinCoreGenerator(new DublinCoreGenerator(new FileInputStream(resourcesRoot+ File.separator+"config"+File.separator+"dublincoremapping.properties")));
      ((SchedulerServiceImpl)service).setCaptureAgentMetadataGenerator(new CaptureAgentMetadataGenerator(new FileInputStream(resourcesRoot+ File.separator+"config"+File.separator+"captureagentmetadatamapping.properties")));
    } catch (FileNotFoundException e) {
      Assert.fail(e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
    event = new EventImpl();
    event.generateId();
    event.addMetadata((Metadata) new MetadataImpl("title","new recording"));
    event.addMetadata((Metadata) new MetadataImpl("timeStart",Long.toString(System.currentTimeMillis())));
    event.addMetadata((Metadata) new MetadataImpl("timeEnd",Long.toString(System.currentTimeMillis()+5000000)));
    event.addMetadata((Metadata) new MetadataImpl("location","testlocation"));
    event.addMetadata((Metadata) new MetadataImpl("device","testrecorder"));
    event.addMetadata((Metadata) new MetadataImpl("creator","test lecturer"));
    event.addMetadata((Metadata) new MetadataImpl("resouces","vga"));
    event.addMetadata((Metadata) new MetadataImpl("seriesId",seriesID));
    event.addMetadata((Metadata) new MetadataImpl("description","a test description"));
    event.addMetadata((Metadata) new MetadataImpl("channelId","unittest"));
    List<Metadata> test = event.getCompleteMetadata();
    logger.info("Metadata: {}", test);
    
  }

  @After
  public void teardown() {
    service.destroy();
    service = null;
  }
  
  @Test
  public void testPersistence () {
    Event eventStored = service.addEvent(event);
    Assert.assertNotNull(eventStored);
    Assert.assertNotNull(eventStored.getEventId());
    Event eventLoaded = service.getEvent(eventStored.getEventId());
    Assert.assertEquals(eventStored, eventLoaded);
    
    Event eventModified = service.getEvent(eventLoaded.getEventId());
    logger.info("State of the loaded event {}.", eventModified);
    
    eventModified.getCompleteMetadata().add((Metadata) new MetadataImpl("stupid.unused.key","no matter what"));
    for (int i = 0; i < eventModified.getCompleteMetadata().size(); i++) {
      if (eventModified.getCompleteMetadata().get(i).getKey().equals("creator") || eventModified.getCompleteMetadata().get(i).getKey().equals("seriesId")) 
        eventModified.getCompleteMetadata().remove(i);
    }
    
    service.updateEvent(eventModified);
    
    Event eventReloaded = service.getEvent(eventModified.getEventId());
    
    logger.info("State of the updated event {}.", eventReloaded);
  }
  
  @Test
  public void testEventManagement() {
    
    // add event
    Event eventUpdated = service.addEvent(event);
    Assert.assertNotNull(eventUpdated);
    Assert.assertNotNull(eventUpdated.getEventId());
    
    // retrieve event
    Event loadedEvent = service.getEvent(eventUpdated.getEventId());
    logger.debug("loaded: {} ",loadedEvent);
    
    Assert.assertEquals(loadedEvent.getValue("location"), event.getValue("location"));
    Assert.assertEquals(loadedEvent.getValue("timeStart"), event.getValue("timeStart"));
    Assert.assertEquals(loadedEvent.getValue("timeEnd"), event.getValue("timeEnd"));
    Assert.assertEquals(loadedEvent.getValue("contributor"), event.getValue("contributor"));
    Assert.assertEquals(loadedEvent.getValue("creator"), event.getValue("creator"));
    Assert.assertEquals(loadedEvent.getValue("device"), event.getValue("device"));
    Assert.assertEquals(loadedEvent.getValue("title"), event.getValue("title"));
    
    //test iCalender export
    CalendarBuilder calBuilder = new CalendarBuilder();
    Calendar cal;
    try {
      String icalString = service.getCalendarForCaptureAgent("testrecorder");
      cal = calBuilder.build(IOUtils.toInputStream(icalString, "UTF-8"));
      ComponentList vevents = cal.getComponents(VEvent.VEVENT);
      for (int i = 0; i < vevents.size(); i++) {
        PropertyList attachments = ((VEvent)vevents.get(i)).getProperties(Property.ATTACH);
        for (int j = 0; j < attachments.size(); j++) {
          String attached = ((Property)attachments.get(j)).getValue();
          String filename = ((Property)attachments.get(j)).getParameter("X-APPLE-FILENAME").getValue();
          attached = new String (Base64.decodeBase64(attached));
          if (filename.equals("agent.properties")) {
            Assert.assertTrue(attached.contains("capture.device.id="+event.getValue("device")));
            Assert.assertTrue(attached.contains("event.title="+event.getValue("title")));
          }
          if (filename.equals("metadata.xml")) {
            Assert.assertTrue(attached.contains(event.getValue("location")));
            Assert.assertTrue(attached.contains(event.getValue("title")));
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
    List<Event> upcoming = service.getUpcomingEvents();
    boolean eventFound = false;
//    for (int i = 0; i < upcoming.length; i++ ) 
//      if (upcoming[i].equals(eventUpdated)) eventFound= true; 
//    Assert.assertFalse(eventFound);    
    
    // test if event is in list in general 
    List<Event> allEvents = service.getEvents(null);
    eventFound = false;
    Assert.assertFalse(allEvents.isEmpty());
    for (Event event : allEvents) {
      if (event.equals(eventUpdated)) {
        eventFound= true; 
      }
    }
    Assert.assertTrue(eventFound); 
    
    //test event filter (positive test)
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setDeviceFilter("testrecorder");
    allEvents = service.getEvents(filter);
    Assert.assertFalse(allEvents.isEmpty());
    eventFound = false;
    for (Event event : allEvents) {
      if (event.equals(eventUpdated)) {
        eventFound= true; 
      }
    }
    Assert.assertTrue(eventFound);    
    
    //test event filter (negative test)
    filter = new SchedulerFilterImpl();
    filter.setDeviceFilter("something");
    allEvents = service.getEvents(filter);
    eventFound = false;
    for (Event event : allEvents) {
      if (event.equals(eventUpdated)) {
        Assert.fail("FALSE EVENT:  filtered for "+filter+" in" + event);
      }
    }
    Assert.assertFalse(eventFound);    
    
    // update event
    eventUpdated.updateMetadata((Metadata) new MetadataImpl("timeEnd", Long.toString(System.currentTimeMillis()+900000)));
    eventUpdated.updateMetadata((Metadata) new MetadataImpl("timeStart", Long.toString(System.currentTimeMillis()+20000)));
    eventUpdated.updateMetadata((Metadata) new MetadataImpl("contributor", "Matterhorn"));
    Assert.assertTrue(service.updateEvent(eventUpdated));
    Assert.assertEquals(service.getEvent(eventUpdated.getEventId()).getValue("contributor"), "Matterhorn");
    Assert.assertNotNull(service.getEvents(null));
    
    // test for upcoming events (now it should be there
    upcoming = service.getUpcomingEvents();
    Assert.assertFalse(upcoming.isEmpty());
    eventFound = false;
    for (Event event : upcoming) {
      if (event.equals(eventUpdated)) {
        eventFound= true; 
      }
    }
    Assert.assertTrue(eventFound);
    
    //delete event
    service.removeEvent(eventUpdated.getEventId());
    Assert.assertNull(service.getEvent(eventUpdated.getEventId()));   
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
    String dc = service.getDublinCoreMetadata(event.getEventId());
    System.out.println("DC-String: "+dc);
    Assert.assertNotNull(dc);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
      Document doc =  builder.parse(IOUtils.toInputStream(dc, "UTF-8"));
      XPath xPath = XPathFactory.newInstance().newXPath();
      Assert.assertEquals(xPath.evaluate("/dublincore/creator", doc), "test lecturer");
    } catch (Exception e1) {
      e1.printStackTrace();
      Assert.fail();
    }
    
    
    
    String ca = service.getCaptureAgentMetadata(event.getEventId());
    Assert.assertNotNull(ca);
    Properties p = new Properties();
    try {
      p.load(new StringReader(ca));
      Assert.assertTrue(p.get("event.title").equals("new recording"));
      Assert.assertTrue(p.get("event.series").equals(seriesID));
      Assert.assertTrue(p.get("event.source").equals("unittest"));
      Assert.assertTrue(p.get("capture.device.location").equals("testlocation"));
    } catch (IOException e) {
      Assert.fail();
    }
    service.removeEvent(event.getEventId());
  }
  
  @Test
  public void testRecurrence () {
    RecurringEventImpl recurringEvent = new RecurringEventImpl();
    recurringEvent.setRecurrence("FREQ=WEEKLY;BYDAY=TU;BYHOUR=12");
    GregorianCalendar now = new GregorianCalendar();
    GregorianCalendar start = new GregorianCalendar(now.get(GregorianCalendar.YEAR)+1, 1, 1);
    GregorianCalendar end = new GregorianCalendar(now.get(GregorianCalendar.YEAR)+1, 6, 1);
    recurringEvent.getMetadata().add((Metadata) new MetadataImpl("recurrenceStart", ""+start.getTimeInMillis()));
    recurringEvent.getMetadata().add((Metadata) new MetadataImpl("recurrenceEnd", ""+end.getTimeInMillis()));
    recurringEvent.getMetadata().add((Metadata) new MetadataImpl("recurrenceDuration", "900000"));
    recurringEvent.getMetadata().add((Metadata) new MetadataImpl("title", "recurrence test title"));
    
    RecurringEventImpl storedEvent = (RecurringEventImpl) service.addRecurringEvent(recurringEvent);
    
    Assert.assertNotNull(storedEvent);
    Assert.assertNotNull(storedEvent.getRecurringEventId());
    
    RecurringEventImpl loadedEvent = (RecurringEventImpl) service.getRecurringEvent(storedEvent.getRecurringEventId());
    
    Assert.assertNotNull(loadedEvent);
    Assert.assertEquals(storedEvent.getValueAsDate("recurrenceStart"), loadedEvent.getValueAsDate("recurrenceStart"));
    
    Assert.assertNotNull(loadedEvent.getEvents());
    
    Event oldEvent = null;
    for (Event e : loadedEvent.getEvents()) {
      Assert.assertTrue(e.getStartdate().before(e.getEnddate()));
      try {
        logger.info("recurring Event titles {} and {}", e.getValue("title"), loadedEvent.getValue("title"));
        //Assert.assertTrue(e.getValue("title").equals(loadedEvent.getValue("title"))); // Can not work because events the numbers now
        Assert.assertTrue(e.getRecurringEventId().equals(loadedEvent.getRecurringEventId()));
      } catch (IncompleteDataException e1) {
        Assert.fail("Recurring event Metadata could not be processed");
      }
      if (oldEvent != null) {
         
        Assert.assertTrue(oldEvent.getStartdate().before(e.getStartdate()));
      }
      oldEvent = e;
    }
     
  }
  
  /*
  @Ignore
  @Test 
  public void test5000Events () {
    //Adding Events
    long time = System.currentTimeMillis();
    for (int i = 0; i < 5000; i++) {
      time += 2000000;
      event.generateId();
      event.updateMetadata(new Metadata("timeEnd", Long.toString(new Date(time + 1000000).getTime())));
      event.updateMetadata(new Metadata("timeStart", Long.toString(new Date(time).getTime())));
      
      service.addEvent(event);
    }
    
    //get upcoming Events
    long start = System.currentTimeMillis();
    Event [] events = service.getUpcomingEvents();
    Assert.assertNotNull(events);
    long stop = System.currentTimeMillis();
    logger.info("Getting {} upcoming events took {} ms.", events.length, (stop-start));
    Assert.assertTrue((stop-start) < 30000);
    
    //get All Events
    start = System.currentTimeMillis();
    Event[] events2 = serviceJPA.getEventsJPA(null);
    Assert.assertNotNull(events);
    stop = System.currentTimeMillis();
    logger.info("Getting all {} events took {} ms.", events2.length, (stop-start));
    Assert.assertTrue((stop-start) < 30000);
    
    //getIcal
    start = System.currentTimeMillis();
    Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder"));
    stop = System.currentTimeMillis();
    logger.info("Getting calendar took {} ms.", (stop-start));
    Assert.assertTrue((stop-start) < 60000);
    
    //getIcal again testing cache
    start = System.currentTimeMillis();
    Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder"));
    stop = System.currentTimeMillis();
    logger.info("Getting calendar again took {} ms.", (stop-start));
    Assert.assertTrue((stop-start) < 10);
    
    //adding additional event 
    time += 2000000;
    event.createID();
    event.setEnddate(new Date(time + 1000000));
    event.setStartdate(new Date(time));
    serviceJPA.addEvent(event);
    // make sure that this is no longer cached
    start = System.currentTimeMillis();
    Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder"));
    stop = System.currentTimeMillis();
    logger.info("Getting calendar after update took {} ms.", (stop-start));
    Assert.assertTrue((stop-start) < 60000);
    Assert.assertTrue((stop-start) > 2); // make sure it is not cached
    
  }
  
  @Ignore
  @Test 
  public void test5RecurringEvents () {
    
    String [] patterns = new String [] {"FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=8;BYMINUTE=0",
            "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=10;BYMINUTE=5",
            "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=12;BYMINUTE=20",
            "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=14;BYMINUTE=35",
            "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=16;BYMINUTE=45"};
    GregorianCalendar now = new GregorianCalendar();
    GregorianCalendar end = new GregorianCalendar(now.get(GregorianCalendar.YEAR)+1, now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH));
    
    for (String pattern : patterns) {
      RecurringEvent recurringEvent = new RecurringEvent();
      recurringEvent.setRecurrence(pattern);
      recurringEvent.getMetadata().add(new Metadata("recurrenceStart", ""+System.currentTimeMillis()));
      recurringEvent.getMetadata().add(new Metadata("recurrenceEnd", ""+end.getTimeInMillis()));
      recurringEvent.getMetadata().add(new Metadata("recurrenceDuration", "60000"));
      recurringEvent.getMetadata().add(new Metadata("title", "recurrence test title"));
      recurringEvent.getMetadata().add(new Metadata("device", "testrecorder"));
      
      RecurringEvent storedEvent = serviceJPA.addRecurringEvent(recurringEvent);
    }
    
    //get upcoming Events
    long start = System.currentTimeMillis();
    SchedulerEvent [] events = serviceJPA.getUpcomingEvents();
    Assert.assertNotNull(events);
    long stop = System.currentTimeMillis();
    logger.info("Getting {} upcoming events took {} ms.", events.length, (stop-start));
    Assert.assertTrue((stop-start) < 30000);
    
    //get All Events
    start = System.currentTimeMillis();
    Event[] events2 = serviceJPA.getEventsJPA(null);
    Assert.assertNotNull(events);
    stop = System.currentTimeMillis();
    logger.info("Getting all {} events took {} ms.", events2.length, (stop-start));
    Assert.assertTrue((stop-start) < 30000);
    
    //getIcal
    start = System.currentTimeMillis();
    Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder"));
    stop = System.currentTimeMillis();
    logger.info("Getting calendar took {} ms.", (stop-start));
    Assert.assertTrue((stop-start) < 60000);
  }*/
}