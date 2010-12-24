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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opencastproject.scheduler.api.Event;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Encoding;
import net.fortuna.ical4j.model.parameter.FmtType;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Attach;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RelatedTo;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Create an iCalendar from the provided SchedulerEvents
 * 
 */
public class CalendarGenerator {
  private static final Logger logger = LoggerFactory.getLogger(CalendarGenerator.class);

  protected Calendar cal;
  protected DublinCoreGenerator dcGenerator;
  protected CaptureAgentMetadataGenerator caGenerator;
  protected SeriesService seriesService;

  /**
   * default constructor that creates a CalendarGenerator object
   * 
   * @param dcGenerator
   *          A DublinCoreGenerator is needed but cannot be constructed in this object
   * @param caGenerator
   *          A CaptureAgentMetadataGenerator is needed but cannot be constructed in this object
   */
  public CalendarGenerator(DublinCoreGenerator dcGenerator, CaptureAgentMetadataGenerator caGenerator,
          SeriesService seriesService) {
    cal = new Calendar();
    cal.getProperties().add(new ProdId("Opencast Matterhorn Calendar File 0.5"));
    cal.getProperties().add(Version.VERSION_2_0);
    cal.getProperties().add(CalScale.GREGORIAN);
    this.dcGenerator = dcGenerator;
    this.caGenerator = caGenerator;
    this.seriesService = seriesService;
  }

  /**
   * gets the iCalendar creates by this object.
   * 
   * @return the iCalendar
   */
  public Calendar getCalendar() {
    return cal;
  }

  /**
   * Sets an iCalender to work with
   * 
   * @param cal
   *          the iCalendar to set
   */
  public void setCalendar(Calendar cal) {
    this.cal = cal;
  }

  /**
   * adds an SchedulerEvent as a new entry to this iCalendar
   * 
   * @param e
   *          the event to add
   * @return true if the event could be added.
   */
  public boolean addEvent(Event e) {
    logger.debug("creating iCal VEvent from SchedulerEvent: {}", e.toString());
    Date start = e.getStartDate();
    Date end = e.getEndDate();
    if (start == null) {
      logger.debug("Couldn't get startdate from event!");
      return false;
    }
    if (end == null) {
      logger.debug("Couldn't get enddate from event!");
      return false;
    }
    DateTime startDate = new DateTime(start);
    DateTime endDate = new DateTime(end);
    startDate.setUtc(true);
    endDate.setUtc(true);
    String seriesID = null;

    VEvent event = new VEvent(startDate, endDate, e.getTitle());
    try {
      ParameterList pl = new ParameterList();
      pl.add(new Cn(e.getCreator()));
      event.getProperties().add(new Uid(Long.toString(e.getEventId())));

      // TODO Organizer should be URI (email-address?) created fake address
      if (StringUtils.isNotEmpty(e.getCreator())) {
        event.getProperties().add(new Organizer(pl, e.getCreator().replace(" ", "_") + "@matterhorn.opencast"));
      }
      if (StringUtils.isNotEmpty(e.getDescription())) {
        event.getProperties().add(new Description(e.getDescription()));
      }
      if (e.containsKey("location") && StringUtils.isNotEmpty(e.getMetadataValueByKey("location"))) {
        event.getProperties().add(new Location(e.getMetadataValueByKey("location")));
      }
      if (StringUtils.isNotEmpty(e.getSeries())) {
        seriesID = e.getSeries();
        event.getProperties().add(new RelatedTo(seriesID));
      }

      ParameterList dcParameters = new ParameterList();
      dcParameters.add(new FmtType("application/xml"));
      dcParameters.add(Value.BINARY);
      dcParameters.add(Encoding.BASE64);
      dcParameters.add(new XParameter("X-APPLE-FILENAME", "episode.xml"));
      Attach metadataAttachment = new Attach(dcParameters, dcGenerator.generateAsString(e).getBytes("UTF-8"));
      event.getProperties().add(metadataAttachment);

      String seriesDC = getSeriesDublinCoreString(seriesID);
      if (seriesDC != null) {
        logger.debug("Attaching series {} information to event {}", seriesID, e.getEventId());
        ParameterList sDcParameters = new ParameterList();
        sDcParameters.add(new FmtType("application/xml"));
        sDcParameters.add(Value.BINARY);
        sDcParameters.add(Encoding.BASE64);
        sDcParameters.add(new XParameter("X-APPLE-FILENAME", "series.xml"));
        Attach seriesAttachment = new Attach(sDcParameters, seriesDC.getBytes("UTF-8"));
        event.getProperties().add(seriesAttachment);
      } else {
        logger.debug("No series provided for event {}.", e.getEventId());
      }

      ParameterList caParameters = new ParameterList();
      caParameters.add(new FmtType("application/text"));
      caParameters.add(Value.BINARY);
      caParameters.add(Encoding.BASE64);
      caParameters.add(new XParameter("X-APPLE-FILENAME", "org.opencastproject.capture.agent.properties"));
      Attach agentsAttachment = new Attach(caParameters, caGenerator.generateAsString(e).getBytes("UTF-8"));
      event.getProperties().add(agentsAttachment);

    } catch (Exception e1) {
      logger.error("could not create Calendar entry for Event {}. Reason : {} ", e.toString(), e1);
      return false;
    }
    cal.getComponents().add(event);

    logger.debug("new VEvent = {} ", event.toString());
    return true;
  }

  private String getSeriesDublinCoreString(String seriesID) {
    if (seriesID == null || seriesID.length() == 0)
      return null;
    if (seriesService == null) {
      logger.warn("No SeriesService available");
      return null;
    }

    try {
      Series series = seriesService.getSeries(seriesID);
      Document doc = (series.getDublinCore()).toXml();

      Source source = new DOMSource(doc);
      StringWriter stringWriter = new StringWriter();
      Result result = new StreamResult(stringWriter);
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.transform(source, result);

      return stringWriter.getBuffer().toString().trim();
    } catch (NotFoundException e) {
      logger.warn("Could not find series '" + seriesID + "': {}", e);
    } catch (ParserConfigurationException e) {
      logger.error("Could not parse DublinCoreCatalog for Series: {}", e.getMessage());
    } catch (IOException e) {
      logger.error("Could not open DublinCoreCatalog for Series to parse it: {}", e.getMessage());
    } catch (TransformerException e) {
      logger.error("Could not transform DublinCoreCatalog for Series: {}", e.getMessage());
    }
    return null;
  }
}
