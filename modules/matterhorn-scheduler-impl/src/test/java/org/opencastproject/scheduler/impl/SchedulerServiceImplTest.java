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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.impl.SeriesImpl;
import org.opencastproject.series.impl.SeriesMetadataImpl;
import org.opencastproject.series.impl.SeriesServiceImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;

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
import org.easymock.EasyMock;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
  private static final String storageRoot = "target" + File.separator + "scheduler-test-db" + File.separator
          + System.currentTimeMillis();
  private static final String resourcesRoot = "src" + File.separator + "main" + File.separator + "resources";

  private Event event;
  private DataSource datasource;

  private String seriesID;

  private DataSource connectToDatabase(File storageDirectory) {
    if (storageDirectory == null) {
      storageDirectory = new File(File.separator + "tmp" + File.separator + "opencast" + File.separator
              + "scheduler-db");
    }
    JdbcConnectionPool cp = JdbcConnectionPool.create("jdbc:h2:" + storageDirectory + ";LOCK_MODE=1;MVCC=TRUE", "sa",
            "sa");
    return cp;
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
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

    SeriesServiceImpl seriesService = new SeriesServiceImpl();
    seriesService.setPersistenceProvider(new PersistenceProvider());
    seriesService.setPersistenceProperties(props);
    seriesService.activate(null);
    service.setSeriesService(seriesService);

    WorkflowInstance workflowInstance = getSampleWorkflowInstance();

    WorkflowService workflowService = EasyMock.createMock(WorkflowService.class);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map<String, String>) EasyMock.anyObject())).andReturn(workflowInstance);
    EasyMock.expect(workflowService.getWorkflowById(EasyMock.anyLong())).andReturn(workflowInstance).anyTimes();
    EasyMock.expect(workflowService.stop(EasyMock.anyLong())).andReturn(workflowInstance);
    workflowService.update((WorkflowInstance) EasyMock.anyObject());
    EasyMock.replay(workflowService);
    service.setWorkflowService(workflowService);

    // Add a series
    Series series = new SeriesImpl();
    LinkedList<SeriesMetadata> metadata = new LinkedList<SeriesMetadata>();
    metadata.add(new SeriesMetadataImpl(series, "title", "demo title"));
    metadata.add(new SeriesMetadataImpl(series, "license", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "valid", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "publisher", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "creator", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "subject", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "temporal", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "audience", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "spatial", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "rightsHolder", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "extent", "3600000"));
    metadata.add(new SeriesMetadataImpl(series, "created", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "language", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "isReplacedBy", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "type", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "available", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "modified", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "replaces", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "contributor", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "description", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "issued", "" + System.currentTimeMillis()));
    series.setMetadata(metadata);
    logger.info("Adding new series...");
    seriesService.addSeries(series);

    // now that the series has been persisted, grab its ID
    seriesID = series.getSeriesId();

    service.activate(null);

    try {
      ((SchedulerServiceImpl) service).setDublinCoreGenerator(new DublinCoreGenerator(new FileInputStream(resourcesRoot
              + File.separator + "config" + File.separator + "dublincoremapping.properties")));
      ((SchedulerServiceImpl) service).setCaptureAgentMetadataGenerator(new CaptureAgentMetadataGenerator(
              new FileInputStream(resourcesRoot + File.separator + "config" + File.separator
                      + "captureagentmetadatamapping.properties")));
    } catch (FileNotFoundException e) {
      Assert.fail(e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
    event = new EventImpl();
    event.setTitle("new recording");
    event.setStartDate(new Date());
    event.setEndDate(new Date(System.currentTimeMillis() + 5000000));
    event.setDevice("testrecorder");
    event.setCreator("test lecturer");
    event.setContributor("demo");
    event.setResources("vga");
    event.setSeriesId(seriesID);
    event.setDescription("a test description");
    event.addMetadata((Metadata) new MetadataImpl(event, "location", "testlocation"));
    event.addMetadata((Metadata) new MetadataImpl(event, "channelId", "unittest"));
  }

  @After
  public void teardown() {
    service.destroy();
    service = null;
  }

  protected WorkflowInstance getSampleWorkflowInstance() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    instance.setId(System.currentTimeMillis());
    instance.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    instance.setState(WorkflowState.PAUSED);

    WorkflowOperationInstanceImpl op = new WorkflowOperationInstanceImpl();
    op.setId(SchedulerServiceImpl.SCHEDULE_OPERATION_ID);
    op.setState(OperationState.PAUSED);
    List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>();
    operations.add(op);
    instance.setOperations(operations);
    return instance;
  }

  @Test
  public void testPersistence() throws Exception {
    Event eventStored = service.addEvent(event);
    Assert.assertNotNull(eventStored);
    Assert.assertNotNull(eventStored.getEventId());
    Event eventLoaded = service.getEvent(eventStored.getEventId());
    Assert.assertEquals(eventStored, eventLoaded);

    Event eventModified = service.getEvent(eventLoaded.getEventId());
    logger.info("State of the loaded event {}.", eventModified);

    eventModified.getMetadataList().add(
            (Metadata) new MetadataImpl(eventModified, "stupid.unused.key", "no matter what"));
    for (int i = 0; i < eventModified.getMetadataList().size(); i++) {
      if (eventModified.getMetadataList().get(i).getKey().equals("creator")
              || eventModified.getMetadataList().get(i).getKey().equals("seriesId"))
        eventModified.getMetadataList().remove(i);
    }

    service.updateEvent(eventModified);

    Event eventReloaded = service.getEvent(eventModified.getEventId());

    logger.info("State of the updated event {}.", eventReloaded);
  }

  @Test
  public void testEventManagement() throws Exception {

    // add event
    Event eventUpdated = service.addEvent(event);
    Assert.assertNotNull(eventUpdated);
    Assert.assertNotNull(eventUpdated.getEventId());

    // retrieve event
    Event loadedEvent = service.getEvent(eventUpdated.getEventId());
    logger.debug("loaded: {} ", loadedEvent);

    Assert.assertEquals(loadedEvent.getStartDate(), event.getStartDate());
    Assert.assertEquals(loadedEvent.getEndDate(), event.getEndDate());
    Assert.assertEquals(loadedEvent.getContributor(), event.getContributor());
    Assert.assertEquals(loadedEvent.getCreator(), event.getCreator());
    Assert.assertEquals(loadedEvent.getDevice(), event.getDevice());
    Assert.assertEquals(loadedEvent.getTitle(), event.getTitle());

    // test iCalender export
    CalendarBuilder calBuilder = new CalendarBuilder();
    Calendar cal;
    try {
      String icalString = service.getCalendarForCaptureAgent("testrecorder");
      cal = calBuilder.build(IOUtils.toInputStream(icalString, "UTF-8"));
      ComponentList vevents = cal.getComponents(VEvent.VEVENT);
      for (int i = 0; i < vevents.size(); i++) {
        PropertyList attachments = ((VEvent) vevents.get(i)).getProperties(Property.ATTACH);
        for (int j = 0; j < attachments.size(); j++) {
          String attached = ((Property) attachments.get(j)).getValue();
          String filename = ((Property) attachments.get(j)).getParameter("X-APPLE-FILENAME").getValue();
          attached = new String(Base64.decodeBase64(attached));
          if (filename.equals("agent.properties")) {
            Assert.assertTrue(attached.contains("capture.device.id=" + event.getDevice()));
            Assert.assertTrue(attached.contains("event.title=" + event.getTitle()));
          }
          if (filename.equals("metadata.xml")) {
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
    List<Event> upcoming = service.getUpcomingEvents();
    boolean eventFound = false;
    // for (int i = 0; i < upcoming.length; i++ )
    // if (upcoming[i].equals(eventUpdated)) eventFound= true;
    // Assert.assertFalse(eventFound);

    // test if event is in list in general
    List<Event> allEvents = service.getEvents(null);
    eventFound = false;
    Assert.assertFalse(allEvents.isEmpty());
    for (Event event : allEvents) {
      if (event.equals(eventUpdated)) {
        eventFound = true;
      }
    }
    Assert.assertTrue(eventFound);

    // test event filter (positive test)
    SchedulerFilter filter = new SchedulerFilter();
    filter.withDeviceFilter("testrecorder");
    allEvents = service.getEvents(filter);
    Assert.assertFalse(allEvents.isEmpty());
    eventFound = false;
    for (Event event : allEvents) {
      if (event.equals(eventUpdated)) {
        eventFound = true;
      }
    }
    Assert.assertTrue(eventFound);

    // test event filter (negative test)
    filter = new SchedulerFilter();
    filter.withDeviceFilter("something");
    allEvents = service.getEvents(filter);
    eventFound = false;
    for (Event event : allEvents) {
      if (event.equals(eventUpdated)) {
        Assert.fail("FALSE EVENT:  filtered for " + filter + " in" + event);
      }
    }
    Assert.assertFalse(eventFound);

    // update event
    eventUpdated.setEndDate(new Date(System.currentTimeMillis() + 900000));
    eventUpdated.setStartDate(new Date(System.currentTimeMillis() + 20000));
    eventUpdated.setContributor("Matterhorn");
    service.updateEvent(eventUpdated);
    Assert.assertEquals(service.getEvent(eventUpdated.getEventId()).getContributor(), "Matterhorn");
    Assert.assertNotNull(service.getEvents(null));

    // test for upcoming events (now it should be there)
    upcoming = service.getUpcomingEvents();
    Assert.assertFalse(upcoming.isEmpty());
    eventFound = false;
    for (Event event : upcoming) {
      if (event.equals(eventUpdated)) {
        eventFound = true;
      }
    }
    Assert.assertTrue(eventFound);

    // delete event
    service.removeEvent(eventUpdated.getEventId());
    try {
      service.getEvent(eventUpdated.getEventId());
      Assert.fail();
    } catch (NotFoundException e) {
      // this is an expected exception
    }
  }

  @Test
  public void testMetadataExport() throws Exception {
    service.addEvent(event);
    String dc = service.getDublinCoreMetadata(event.getEventId());
    Assert.assertNotNull(dc);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
      Document doc = builder.parse(IOUtils.toInputStream(dc, "UTF-8"));
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

  /*
   * @Ignore
   * 
   * @Test public void test5000Events () { //Adding Events long time = System.currentTimeMillis(); for (int i = 0; i <
   * 5000; i++) { time += 2000000; event.generateId(); event.updateMetadata(new Metadata("timeEnd", Long.toString(new
   * Date(time + 1000000).getTime()))); event.updateMetadata(new Metadata("timeStart", Long.toString(new
   * Date(time).getTime())));
   * 
   * service.addEvent(event); }
   * 
   * //get upcoming Events long start = System.currentTimeMillis(); Event [] events = service.getUpcomingEvents();
   * Assert.assertNotNull(events); long stop = System.currentTimeMillis();
   * logger.info("Getting {} upcoming events took {} ms.", events.length, (stop-start)); Assert.assertTrue((stop-start)
   * < 30000);
   * 
   * //get All Events start = System.currentTimeMillis(); Event[] events2 = serviceJPA.getEventsJPA(null);
   * Assert.assertNotNull(events); stop = System.currentTimeMillis(); logger.info("Getting all {} events took {} ms.",
   * events2.length, (stop-start)); Assert.assertTrue((stop-start) < 30000);
   * 
   * //getIcal start = System.currentTimeMillis();
   * Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder")); stop = System.currentTimeMillis();
   * logger.info("Getting calendar took {} ms.", (stop-start)); Assert.assertTrue((stop-start) < 60000);
   * 
   * //getIcal again testing cache start = System.currentTimeMillis();
   * Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder")); stop = System.currentTimeMillis();
   * logger.info("Getting calendar again took {} ms.", (stop-start)); Assert.assertTrue((stop-start) < 10);
   * 
   * //adding additional event time += 2000000; event.createID(); event.setEnddate(new Date(time + 1000000));
   * event.setStartdate(new Date(time)); serviceJPA.addEvent(event); // make sure that this is no longer cached start =
   * System.currentTimeMillis(); Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder")); stop =
   * System.currentTimeMillis(); logger.info("Getting calendar after update took {} ms.", (stop-start));
   * Assert.assertTrue((stop-start) < 60000); Assert.assertTrue((stop-start) > 2); // make sure it is not cached
   * 
   * }
   * 
   * @Ignore
   * 
   * @Test public void test5RecurringEvents () {
   * 
   * String [] patterns = new String [] {"FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=8;BYMINUTE=0",
   * "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=10;BYMINUTE=5", "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=12;BYMINUTE=20",
   * "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=14;BYMINUTE=35",
   * "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=16;BYMINUTE=45"}; GregorianCalendar now = new GregorianCalendar();
   * GregorianCalendar end = new GregorianCalendar(now.get(GregorianCalendar.YEAR)+1, now.get(GregorianCalendar.MONTH),
   * now.get(GregorianCalendar.DAY_OF_MONTH));
   * 
   * for (String pattern : patterns) { RecurringEvent recurringEvent = new RecurringEvent();
   * recurringEvent.setRecurrence(pattern); recurringEvent.getMetadata().add(new Metadata("recurrenceStart",
   * ""+System.currentTimeMillis())); recurringEvent.getMetadata().add(new Metadata("recurrenceEnd",
   * ""+end.getTimeInMillis())); recurringEvent.getMetadata().add(new Metadata("recurrenceDuration", "60000"));
   * recurringEvent.getMetadata().add(new Metadata("title", "recurrence test title"));
   * recurringEvent.getMetadata().add(new Metadata("device", "testrecorder"));
   * 
   * RecurringEvent storedEvent = serviceJPA.addRecurringEvent(recurringEvent); }
   * 
   * //get upcoming Events long start = System.currentTimeMillis(); SchedulerEvent [] events =
   * serviceJPA.getUpcomingEvents(); Assert.assertNotNull(events); long stop = System.currentTimeMillis();
   * logger.info("Getting {} upcoming events took {} ms.", events.length, (stop-start)); Assert.assertTrue((stop-start)
   * < 30000);
   * 
   * //get All Events start = System.currentTimeMillis(); Event[] events2 = serviceJPA.getEventsJPA(null);
   * Assert.assertNotNull(events); stop = System.currentTimeMillis(); logger.info("Getting all {} events took {} ms.",
   * events2.length, (stop-start)); Assert.assertTrue((stop-start) < 30000);
   * 
   * //getIcal start = System.currentTimeMillis();
   * Assert.assertNotNull(serviceJPA.getCalendarForCaptureAgent("testrecorder")); stop = System.currentTimeMillis();
   * logger.info("Getting calendar took {} ms.", (stop-start)); Assert.assertTrue((stop-start) < 60000); }
   */
}