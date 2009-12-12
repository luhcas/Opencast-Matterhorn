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

import java.net.URISyntaxException;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.ResourceList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.property.Attach;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RelatedTo;
import net.fortuna.ical4j.model.property.Resources;
import net.fortuna.ical4j.model.property.Uid;

/**
 * TODO: Comment me!
 *
 */
public class CalendarGenerator {
  private static final Logger logger = LoggerFactory.getLogger(CalendarGenerator.class);
  
  Calendar cal;
  DublinCoreGenerator dcGenerator;
  
  
  public CalendarGenerator (DublinCoreGenerator dcGenerator) {
    cal = new Calendar();
    this.dcGenerator = dcGenerator;
  }
  
  /**
   * gets the iCalendar creates by this object.
   * @return the iCalendar
   */
  public Calendar getCalendar() {
    return cal;
  }

  /**
   * Sets an iCalender to work with
   * @param cal the iCalendar to set
   */
  public void setCalendar(Calendar cal) {
    this.cal = cal;
  }

  /**
   * adds an SchedulerEvent as a new entry to this iCalendar 
   * @param e the event to add 
   * @return true if the event could be added. 
   */
  public boolean addEvent (SchedulerEvent e) {
    logger.debug("creating iCal VEvent from SchedulerEvent: "+e.toString());
    VEvent event = new VEvent(new DateTime(e.getStartdate()), new DateTime(e.getEnddate()), e.getTitle());
    try {
      ParameterList pl = new ParameterList();
      pl.add(new Cn(e.getCreator()));
      event.getProperties().add(new Uid(e.getID()));
      
      // TODO Organizer should be URI (email-address?) created fake adress
      if (e.getCreator() != null && ! e.getCreator().equalsIgnoreCase("null")) event.getProperties().add(new Organizer(pl ,e.getCreator().replace(" ", "_")+"@matterhorn.opencast"));
      if (e.getAbstract() != null && ! e.getAbstract().equalsIgnoreCase("null")) event.getProperties().add(new Description(e.getAbstract()));
      if (e.getLocation() != null && ! e.getLocation().equalsIgnoreCase("null")) event.getProperties().add(new Location(e.getLocation()));
      if (e.getSeriesID() != null && ! e.getSeriesID().equalsIgnoreCase("null")) event.getProperties().add(new RelatedTo(e.getSeriesID()));
      if (e.getAttendees() != null) {
        String [] attendees = e.getAttendees();
        for (int i = 0; i < attendees.length; i++) {
          ParameterList plAtt = new ParameterList();
          plAtt.add(new Cn(attendees[i]));
          // TODO Organizer should be URI (email-address?) created fake adress
          if ( ! attendees[i].equals(e.getDevice())) event.getProperties().add(new Attendee(plAtt, attendees[i].replace(" ","_")+"@matterhorn.opencast"));
        }
      }
      if (e.getResources() != null) {
        String [] resources = e.getResources();
        ResourceList resList = new ResourceList();
        for (int i = 0; i < resources.length; i++) resList.add(resources[i]);     
        event.getProperties().add(new Resources(resList));
      }
        event.getProperties().add(new Attach((dcGenerator.generateAsBase64(e))));
      //event.getProperties().add(new XProperty(name));
    } catch (URISyntaxException e1) {
      logger.error("could not create Calendar entry for Event "+ e.toString()+". Reason : "+e1.getMessage());
    } 
    cal.getComponents().add(event);
    logger.debug("new VEvent = "+event.toString() );
    return true;
  }
  
}
