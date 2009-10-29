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
package org.opencastproject.capture.impl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Properties;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Duration;

import org.apache.commons.codec.binary.Base64;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerImpl implements org.opencastproject.capture.api.Scheduler {

  public static final String SCHEDULER = "scheduler";

  /** The properties of the scheduler for the calendar polling system */
  private static Properties POLLING_PROPS = null;

  /** The properties of the scheduler for the capture system */
  private static Properties CAPTURE_PROPS = null;

  /** Log facility */
  private static final Logger log = LoggerFactory.getLogger(SchedulerImpl.class);

  /** The scheduler to start the captures */
  private Scheduler captureScheduler = null;

  /** The stored URI for the calendar source */
  private URL calendarURL = null;
  
  /** The time in milliseconds between attempts to fetch the calendar data */
  private int pollTime = 0;

  /** A stored copy of the Calendar at the URI */
  private Calendar calendar = null;

  /** The scheduler which does the actual polling */
  private Scheduler pollScheduler = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.CalendarService#init()
   */
  public void init(URL url, int pollingTime) {
    log.debug("init()");
    calendarURL = url;
    pollTime = pollingTime;

    try {
      //Load the properties for this scheduler.  Each scheduler requires its own unique properties file.
      POLLING_PROPS = new Properties();
      POLLING_PROPS.load(getClass().getClassLoader().getResourceAsStream("config/polling.properties"));
      SchedulerFactory sched_fact = new StdSchedulerFactory(POLLING_PROPS);

      //Create and start the scheduler
      pollScheduler = sched_fact.getScheduler();
      pollScheduler.start();

      //Setup the polling
      JobDetail job = new JobDetail("calendarUpdate", Scheduler.DEFAULT_GROUP, PullCalendarJob.class);
      //Create a new trigger                    Name       Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger trigger = new SimpleTrigger("polling", Scheduler.DEFAULT_GROUP, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, pollTime * 1000L);

      trigger.getJobDataMap().put(SCHEDULER, this);

      //Schedule the update
      pollScheduler.scheduleJob(job, trigger);

      //Load the properties for this scheduler.  Each scheduler requires its own unique properties file.
      CAPTURE_PROPS = new Properties();
      CAPTURE_PROPS.load(getClass().getClassLoader().getResourceAsStream("config/scheduler.properties"));
      sched_fact = new StdSchedulerFactory(CAPTURE_PROPS);
      //Create and start the capture scheduler
      captureScheduler = sched_fact.getScheduler();
      captureScheduler.start();
    } catch (SchedulerException e) {
      log.error("init:  Unable to start scheduler(s)", e);
    } catch (IOException e) {
      log.error("init:  Unable to load Quartz properties file", e);
    }
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#finalize()
   */
  public void finalize() {
    try {
      if (pollScheduler != null) {
          pollScheduler.shutdown();
      }
      if (captureScheduler != null) {
        captureScheduler.shutdown();
      }
    } catch (SchedulerException e) {
      log.warn("Finalize for " + this.getClass().getName() + " did not execute cleanly...", e);
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.CalendarService#updateCalendar()
   */
  public void updateCalendar() {
    log.debug("updateCalendar()");

    try {
      //Fetch the calendar data and build a iCal4j representation of it
      if (calendarURL != null) {
        calendar = parseCalendar(calendarURL);
        setCaptureSchedule(calendar);
      } else {
        log.error("Calendar URL is invalid!");
      }
    } catch (MalformedURLException e) {
      log.error("updateCalendar:  Invalid URL specified", e);
    }
  }

  /**
   * Reads in a calendar from either an HTTP or local source and turns it into a cron4j Calendar object
   * @param url The URL to read the calendar data from
   * @return A calendar object, or null in the case of an error
   * @throws MalformedURLException 
   */
  public Calendar parseCalendar(URL url) throws MalformedURLException {
    log.debug("parseCalendar()");
    try {
      //Urgh, read in the data.  These ugly lines handle both HTTP and local file
      DataInputStream in = new DataInputStream(new BufferedInputStream(url.openConnection().getInputStream()));
      int c = 0;
      StringBuilder sb = new StringBuilder();
      while ((c = in.read()) != -1) {
        sb.append((char) c);
      }
      //Great, now build the calendar instance
      CalendarBuilder cb = new CalendarBuilder();
      return cb.build(new StringReader(sb.toString()));
    } catch (ParserException e) {
      log.error("readCalendarData: Parsing error", e);
      return null;
    } catch (IOException e) {
      log.error("readCalendarData: I/O error", e);
      return null;
    }
  }
  
  /**
   * Returns a string of the name for every scheduled job
   * @return An array of Strings containing the name of every scheduled job
   * @throws SchedulerException 
   */
  public String[] getCaptureSchedule() throws SchedulerException {
    return captureScheduler.getJobNames(Scheduler.DEFAULT_GROUP);
  }

  private void checkSchedules() throws SchedulerException {
    log.info("Currently scheduled jobs for capture schedule:");
    for (String name : captureScheduler.getJobNames(Scheduler.DEFAULT_GROUP)) {
      log.info(name);  
    }
    log.info("Currently scheduled jobs for poll schedule:");
    for (String name : pollScheduler.getJobNames(Scheduler.DEFAULT_GROUP)) {
      log.info(name);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.ScheduleService#setCaptureSchedule()
   */
  public void setCaptureSchedule(Calendar newCal) {
    log.debug("setCaptureSchedule()");
    try {
      checkSchedules();
      //Clear the existing jobs and reschedule everything
      captureScheduler.shutdown();
      SchedulerFactory sched_fact = new StdSchedulerFactory(CAPTURE_PROPS);
      captureScheduler = sched_fact.getScheduler();

      ComponentList list = newCal.getComponents(Component.VEVENT);
      for (Object item : list) {
        VEvent event = (VEvent) item;
        Date start = event.getStartDate().getDate();
        Date end = event.getEndDate().getDate();
        Duration duration = event.getDuration();
        if (duration == null) {
          //Note that in the example metadata I was given the duration field did not exist...
          duration = new Duration(start, end);
        }

        //TODO:  Figure out what to do with this attachment
        String attachment = getAttachment(event);
        
        CronExpression cronString = getCronString(event, start);

        CronTrigger trig = new CronTrigger();
        trig.setCronExpression(cronString);
        trig.setName(cronString.toString());

        JobDetail job = new JobDetail(start.toString(), Scheduler.DEFAULT_GROUP, CaptureJob.class);

        captureScheduler.scheduleJob(job, trig);
      }
      log.debug("Capture schedule updating...");
      checkSchedules();
    } catch (NullPointerException e) {
      log.error("Invalid calendar data, one of the start or end times is incorrect", e);
    } catch (SchedulerException e) {
      log.error("Invalid scheduling data", e);
    } catch (ParseException e) {
      log.error("Parsing error", e);
    }
  }

  /**
   * Decodes and returns a Base64 encoded attachment
   * @param event The event to get the attachment from
   * @return A String representation of the attachment
   * @throws ParseException
   */
  private String getAttachment(VEvent event) throws ParseException {
    byte[] bytes = Base64.decodeBase64(event.getProperty(Property.ATTACH).getValue());
    String attach = new String(bytes);
    return attach;
  }

  /**
   * Parses an event to build a cron-like time string
   * @param event The Event which we need the time string for
   * @param start The start time
   * @return A cron-like scheduling string
   * @throws ParseException
   */
  private CronExpression getCronString(VEvent event, Date start) throws ParseException {
    /*
     * Initial implementation called for recurring events.  Keeping this code for later (ie, once the specs are set in stone)
      if (event.getProperty(Property.RRULE) != null) {
      Recur rrule = new Recur(event.getProperty(Property.RRULE).getValue());

      WeekDayList weekdays = rrule.getDayList();
      Iterator<WeekDay> iter = (Iterator<WeekDay>) weekdays.iterator();
      StringBuilder captureDays = new StringBuilder();
      while (iter.hasNext()) {
        WeekDay cur = iter.next();
        //Sigh, why doesn't RFC 2445 use *normal* day abbreviations?  There's no other way to do this unfortunately...
        if (cur.equals(WeekDay.SU)) {class
          captureDays.append("SUN,");
        } else if (cur.equals(WeekDay.MO)) {
          captureDays.append("MON,");
        } else if (cur.equals(WeekDay.TU)) {
          captureDays.append("TUE,");
        } else if (cur.equals(WeekDay.WE)) {
          captureDays.append("WED,");
        } else if (cur.equals(WeekDay.TH)) {
          captureDays.append("THU,");
        } else if (cur.equals(WeekDay.FR)) {
          captureDays.append("FRI,");
        } else if (cur.equals(WeekDay.SA)) {
          captureDays.append("SAT,");
        }
      }
      //Remove the trailing comma to conform to Cron style
      captureDays.deleteCharAt(captureDays.length()-1);

      String frequency = rrule.getFrequency();
      int interval = rrule.getInterval();


      //TODO:  Remove the deprecated calls here.
      return new CronExpression(start.getSeconds() + " " + start.getMinutes() + " " + start.getHours() + " " + start.getDate() + " " + start.getMonth() + " * ");
    }
    return null;*/

    //Note:  Skipped above code because we no longer need to deal with recurring events.  Keeping it for now since they might come back.
    //Note:  "?" means no particular setting.  Equivalent to "ignore me", rather than "I don't know what to put here"
    return new CronExpression(start.getSeconds() + " " + start.getMinutes() + " " + start.getHours() + " " + start.getDate() + " " + start.getMonth() + " ? ");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.ScheduleService#disableScheduler()
   */
  public void disableScheduler() {
    try {
      captureScheduler.pauseAll();
    } catch (SchedulerException e) {
      log.error("Unable to disable capture scheduler", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.ScheduleService#enableScheduler()
   */
  public void enableScheduler() {
    try {
      captureScheduler.start();
    } catch (SchedulerException e) {
      log.error("Unable to enable capture scheduler", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.CalendarService#disablePolling()
   */
  public void disablePolling() {
    try {
      pollScheduler.pauseAll();
    } catch (SchedulerException e) {
      log.error("Unable to disable polling scheduler", e);
    }    
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.CalendarService#enablePolling()
   */
  public void enablePolling() {
    try {
      pollScheduler.pauseAll();
    } catch (SchedulerException e) {
      log.error("Unable to disable polling scheduler", e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#getPollingTime()
   */
  public int getPollingTime() {
    return pollTime;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#setPollingTime(int)
   */
  public void setPollingTime(int pollingTime) {
    if (pollingTime <= 0) {
      log.warn("Unable to set polling time to less than 1 second...");
      return;
    }
    pollTime = pollingTime;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#getScheduleEndpoint()
   */
  public URL getScheduleEndpoint() {
    return calendarURL;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#setScheduleEndpoint(java.net.URL)
   */
  public void setScheduleEndpoint(URL url) {
    if (url == null) {
      log.error("setScheduleEndpoint():  Invalid URL specified");
      return;
    }
    calendarURL = url;
  }
}
