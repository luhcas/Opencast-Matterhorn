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
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.series.api.SeriesService;
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
import org.easymock.IAnswer;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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

  private Event aEvent;
  private Event bEvent;
  private Event cEvent;
  private Event dEvent;

  private DataSource datasource;

  private String seriesID;

  private DataSource connectToDatabase(File storageDirectory) {
    if (storageDirectory == null) {
      storageDirectory = new File(File.separator + "tmp" + File.separator + "opencast" + File.separator
              + "scheduler-db");
    }
    JdbcConnectionPool cp = JdbcConnectionPool.create("jdbc:h2:" + storageDirectory, "sa", "sa");
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

    /*
     * SeriesServiceImpl seriesService = new SeriesServiceImpl(); SeriesServiceDatabaseImpl seriesDb = new
     * SeriesServiceDatabaseImpl(); seriesDb.setDublinCoreService(new DublinCoreCatalogService());
     * seriesDb.setPersistenceProperties(props); seriesDb.setPersistenceProvider(new PersistenceProvider());
     * seriesDb.activate(null); seriesService.setPersistence(seriesDb); seriesService.activate(null);
     */

    // Add a series
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
    seriesID = Long.toString(System.currentTimeMillis());
    dc.set(DublinCoreCatalogImpl.PROPERTY_IDENTIFIER, seriesID);
    dc.set(DublinCoreCatalogImpl.PROPERTY_TITLE, "demo title");
    dc.set(DublinCoreCatalogImpl.PROPERTY_LICENSE, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_PUBLISHER, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_CREATOR, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_SUBJECT, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_TEMPORAL, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_SPATIAL, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_RIGHTS_HOLDER, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_EXTENT, "3600000");
    dc.set(DublinCoreCatalogImpl.PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(DublinCoreCatalogImpl.PROPERTY_LANGUAGE, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_IS_REPLACED_BY, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_TYPE, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_AVAILABLE, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(DublinCoreCatalogImpl.PROPERTY_REPLACES, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_CONTRIBUTOR, "demo");
    dc.set(DublinCoreCatalogImpl.PROPERTY_DESCRIPTION, "demo");

    SeriesService seriesService = EasyMock.createMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries((String)EasyMock.anyObject())).andReturn(dc).anyTimes();

    service.setSeriesService(seriesService);
    service.activate(null);

    WorkflowInstance workflowInstance = getSampleWorkflowInstance();

    WorkflowService workflowService = EasyMock.createMock(WorkflowService.class);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map<String, String>) EasyMock.anyObject())).andAnswer(new IAnswer<WorkflowInstance>() {
      public WorkflowInstance answer() throws Throwable {
        WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
        Random gen = new Random(System.currentTimeMillis());
        instance.setId(gen.nextInt());
        instance.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
        instance.setState(WorkflowState.PAUSED);

        WorkflowOperationInstanceImpl op = new WorkflowOperationInstanceImpl(
                SchedulerServiceImpl.SCHEDULE_OPERATION_ID, OperationState.PAUSED);
        List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>();
        operations.add(op);
        instance.setOperations(operations);
        return instance;
      }
    }).anyTimes();
    EasyMock.expect(workflowService.getWorkflowById(EasyMock.anyLong())).andReturn(workflowInstance).anyTimes();
    EasyMock.expect(workflowService.stop(EasyMock.anyLong())).andReturn(workflowInstance).anyTimes();
    workflowService.update((WorkflowInstance) EasyMock.anyObject());
    EasyMock.replay(workflowService, seriesService);
    service.setWorkflowService(workflowService);

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

    aEvent = new EventImpl();
    bEvent = new EventImpl();
    cEvent = new EventImpl();
    dEvent = new EventImpl();

    aEvent.setTitle("Event A - Test");
    bEvent.setTitle("Event B - Bacon");
    cEvent.setTitle("Event C - Cheese");
    dEvent.setTitle("Event D - Donburi");

    aEvent.setStartDate(new Date(System.currentTimeMillis() + 10000)); // start now + 10 seconds
    aEvent.setEndDate(new Date(System.currentTimeMillis() + 3610000)); // end hour and 10 seconds from now
    bEvent.setStartDate(new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000))); // start 24 hours from now
    bEvent.setEndDate(new Date(System.currentTimeMillis() + (25 * 60 * 60 * 1000))); // end 25 hours from now
    cEvent.setStartDate(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // start an hour ago
    cEvent.setEndDate(new Date(System.currentTimeMillis() - (10 * 60 * 1000))); // end 10 minutes ago
    dEvent.setStartDate(new Date(System.currentTimeMillis() + 10000)); // same as aEvent
    dEvent.setEndDate(new Date(System.currentTimeMillis() + 3610000)); // same as aEvent

    aEvent.setDevice("Device A");
    bEvent.setDevice("Device A");
    cEvent.setDevice("Device C");
    dEvent.setDevice("Device D");

    aEvent.setCreator("Person A");
    bEvent.setCreator("person a");
    cEvent.setCreator("Person B");
    dEvent.setCreator("PERSON B");

    aEvent.setSeries("Series A");
    bEvent.setSeries("series a");
    cEvent.setSeries("Series B");
    dEvent.setSeries("SERIES B");
  }

  @After
  public void teardown() throws Exception {
    service.destroy();
    service = null;
  }

  protected WorkflowInstance getSampleWorkflowInstance() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    Random gen = new Random(System.currentTimeMillis());
    instance.setId(gen.nextInt());
    instance.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    instance.setState(WorkflowState.PAUSED);

    WorkflowOperationInstanceImpl op = new WorkflowOperationInstanceImpl(SchedulerServiceImpl.SCHEDULE_OPERATION_ID,
            OperationState.PAUSED);
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

  @Test
  public void testSchedulerFilter() throws Exception {
    service.addEvent(aEvent);
    service.addEvent(bEvent);
    service.addEvent(cEvent);
    service.addEvent(dEvent);

    List<Event> events = service.getEvents(new SchedulerFilter().withTitleFilter("Event"));
    Assert.assertEquals(4, events.size());

    events = service.getEvents(new SchedulerFilter().withDeviceFilter("Device A"));
    Assert.assertEquals(2, events.size());

    events = service.getEvents(new SchedulerFilter().withCreatorFilter("PERSON B"));
    Assert.assertEquals(2, events.size());

    events = service.getEvents(new SchedulerFilter().withSeriesFilter("series a"));
    Assert.assertEquals(2, events.size());
  }

  @Test
  public void testFindConflictingEvents() throws Exception {
    Date start = new Date(System.currentTimeMillis());
    Date end = new Date(System.currentTimeMillis() + (60 * 60 * 1000));

    List<Event> events = service.findConflictingEvents("Some Other Device", start, end);
    Assert.assertEquals(0, events.size());

    events = service.findConflictingEvents("Device A", start, end);
    Assert.assertEquals(1, events.size());

    events = service.findConflictingEvents("Device A", "FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA", start,
            new Date(start.getTime() + (48 * 60 * 60 * 1000)), new Long(36000));
    Assert.assertEquals(2, events.size());
  }

  @Test
  public void testCalendarNotModified() throws Exception {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.replay(request);

    SchedulerRestService restService = new SchedulerRestService();
    restService.setService(service);

    // Store an event
    Event eventStored = service.addEvent(event);

    String device = eventStored.getDevice();
    Assert.assertNotNull(device);
    Assert.assertNotNull(event.getLastModified());

    // Request the calendar without specifying an etag. We should get a 200 with the icalendar in the response body
    Response response = restService.getCalendarForCaptureAgent(device, request);
    Assert.assertNotNull(response.getEntity());
    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    final String etag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);

    EasyMock.reset(request);
    EasyMock.expect(request.getHeader("If-None-Match")).andAnswer(new IAnswer<String>() {
      @Override
      public String answer() throws Throwable {
        return etag;
      }
    }).anyTimes();
    EasyMock.replay(request);

    // Request using the etag from the first response. We should get a 304 (not modified)
    response = restService.getCalendarForCaptureAgent(device, request);
    Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    Assert.assertNull(response.getEntity());

    // Update the event
    service.updateEvent(eventStored);

    // Try using the same old etag. We should get a 200, since the event has changed
    response = restService.getCalendarForCaptureAgent(device, request);
    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    Assert.assertNotNull(response.getEntity());
    String secondEtag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);

    Assert.assertNotNull(secondEtag);
    Assert.assertFalse(etag.equals(secondEtag));
  }
}
