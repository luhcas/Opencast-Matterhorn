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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Dictionary;
import java.util.ListIterator;
import java.util.Properties;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Duration;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.impl.jobs.PollCalendarJob;
import org.opencastproject.capture.impl.jobs.StartCaptureJob;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
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

public class SchedulerImpl implements org.opencastproject.capture.api.Scheduler, ManagedService {

  // TODO: Move these constants into some common interface such as 'JobParameters'  
  /** A constant which defines the key to retrieve a pointer to this object in the Quartz job classes.  This is required for PollCalendarJob to know who to push updated calendar data to */
  public static final String SCHEDULER = "scheduler";
  /** Constant used to define the key for the CaptureAgentImpl object which is pulled out of the execution context */
  public static final String CAPTURE_AGENT = "capture_agent";
  
  /** The properties of the scheduler for the calendar polling system */
  private static Properties pollingProperties = null;

  /** The properties of the scheduler for the capture system */
  private static Properties captureProperties = null;

  /** Log facility */
  private static final Logger log = LoggerFactory.getLogger(SchedulerImpl.class);

  /** The scheduler to start the captures */
  private Scheduler captureScheduler = null;

  /** The scheduler which does the actual polling */
  private Scheduler pollScheduler = null;

  /** The stored URL for the remote calendar source */
  private URL remoteCalendarURL = null;

  /** The URL of the cached copy of the recording schedule */
  private URL localCalendarCacheURL = null;

  /** The time in milliseconds between attempts to fetch the calendar data */
  private long pollTime = 0;

  /** A stored copy of the Calendar at the URI */
  private Calendar calendar = null;

  /** The configuration for this service */
  private ConfigurationManager config = null;

  private CaptureAgent captureAgent = null;

  public void deactivate() {
    shutdown();
  }

  /**
   * Called when the bundle is activated and does all of the activation for the schedulers.
   * @param cc The component context
   */
  public void activate(ComponentContext cc) {
    config = ConfigurationManager.getInstance();
    SchedulerFactory sched_fact = null;

    try {
      localCalendarCacheURL = new File(config.getItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL)).toURI().toURL();
    } catch (NullPointerException e) {
      log.warn("Invalid location specified for {} unable to cache scheduling data.", CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL);
    } catch (MalformedURLException e) {
      log.warn("Invalid location specified for {} unable to cache scheduling data.", CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL);
    }

    try {
      remoteCalendarURL = new URL(config.getItem(CaptureParameters.CAPTURE_SCHEDULE_URL));
      //Times are in seconds in the config file, so multiply by 1000
      pollTime = Long.parseLong(config.getItem(CaptureParameters.CAPTURE_SCHEDULE_POLLING_INTERVAL)) * 1000L;
      if (pollTime > 1) {
        //Load the properties for this scheduler.  Each scheduler requires its own unique properties file.
        pollingProperties = new Properties();
        pollingProperties.load(getClass().getClassLoader().getResourceAsStream("config/calendar_polling_scheduler.properties"));
        sched_fact = new StdSchedulerFactory(pollingProperties);
  
        //Create and start the scheduler
        pollScheduler = sched_fact.getScheduler();
        pollScheduler.start();
  
        //Setup the polling
        JobDetail job = new JobDetail("calendarUpdate", Scheduler.DEFAULT_GROUP, PollCalendarJob.class);
        //Create a new trigger                    Name       Group name               Start       End   # of times to repeat               Repeat interval
        SimpleTrigger trigger = new SimpleTrigger("polling", Scheduler.DEFAULT_GROUP, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, pollTime);
  
        trigger.getJobDataMap().put(SCHEDULER, this);
  
        //Schedule the update
        pollScheduler.scheduleJob(job, trigger);
      } else {
        log.info("{} has been set to less than 1, calendar updates disabled.", CaptureParameters.CAPTURE_SCHEDULE_POLLING_INTERVAL);
      }
    } catch (MalformedURLException e) {
      log.warn("Invalid location specified for {} unable to retrieve new scheduling data.", CaptureParameters.CAPTURE_SCHEDULE_URL);
    } catch (NumberFormatException e) {
      log.warn("Invalid polling interval for {} unable to retrieve new scheduling data.", CaptureParameters.CAPTURE_SCHEDULE_POLLING_INTERVAL);
    } catch (IOException e) {
      throw new RuntimeException("IOException, unable to load bundled Quartz properties file for calendar polling.", e);
    } catch (SchedulerException e) {
      throw new RuntimeException("Internal error in polling scheduler, unable to start.", e);
    }

    try {
      //Load the properties for this scheduler.  Each scheduler requires its own unique properties file.
      captureProperties = new Properties();
      captureProperties.load(getClass().getClassLoader().getResourceAsStream("config/capture_scheduler.properties"));
      sched_fact = new StdSchedulerFactory(captureProperties);
      //Create and start the capture scheduler
      captureScheduler = sched_fact.getScheduler();
      updateCalendar();
      captureScheduler.start();
    } catch (SchedulerException e) {
      throw new RuntimeException("Internal error in capture scheduler, unable to start.", e);
    } catch (IOException e) {
      throw new RuntimeException("IOException, unable to load Quartz properties file for capture scheduling.");
    }
  }

  public void setCaptureAgent(CaptureAgent agent) {
    captureAgent = agent;
  }

  public void unsetCaptureAgent() {
    captureAgent = null;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#finalize()
   */
  @Override
  public void finalize() {
    shutdown();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.StateService#updateCalendar()
   */
  public void updateCalendar() {

    //Fetch the calendar data and build a iCal4j representation of it
    if (remoteCalendarURL != null) {
      calendar = parseCalendar(remoteCalendarURL);
    } else if (calendar == null && localCalendarCacheURL != null) {
      calendar = parseCalendar(localCalendarCacheURL);
    } else if (calendar == null && localCalendarCacheURL == null) {
      log.warn("Unable to update calendar from either local or remote sources.");
    } else {
      log.info("Calendar already exists, and {} is invalid, skipping update.", CaptureParameters.CAPTURE_SCHEDULE_URL);
      return;
    }

    //Now set the calendar data
    if (calendar != null) {
      setCaptureSchedule(calendar);
    } else {
      log.warn("Calendar parsing routine returned null, skipping update of capture schedule.");
    }
  }

  /**
   * Reads in a calendar from either an HTTP or local source and turns it into a iCal4j Calendar object
   * @param url The URL to read the calendar data from
   * @return A calendar object, or null in the case of an error or to indicate that no update should be performed
   */
  private Calendar parseCalendar(URL url) {

    String calendarString = null;
    try {
      calendarString = readCalendar(url);
    } catch (Exception e) {
      log.warn("Parsing exception, keeping current scheduling data: {}.", e.toString());
      //If the calendar is null, which only happens when the machine has *just* been powered on.
      //This case handles not having a network connection by just reading from the cached copy of the calendar 
      if (calendar == null) {
        try {
          calendarString = readCalendar(localCalendarCacheURL);
        } catch (IOException e1) {
          log.warn("Unable to read from cached schedule: {}.", e.getMessage());
          return null;
        }
      } else {
        log.info("Calendar already exists, and {} is invalid, skipping update.", CaptureParameters.CAPTURE_SCHEDULE_URL);
        return null;
      }
    }

    if (calendarString == null) {
      log.warn("Invalid calendar data, skipping parse attempt.");
      return null;
    }

    Calendar cal = null;
    try {
      //Great, now build the calendar instance
      CalendarBuilder cb = new CalendarBuilder();
      cal = cb.build(new StringReader(calendarString));
    } catch (ParserException e) {
      log.error("Parsing error for {}: {}.", url, e.getMessage());
      return null;
    } catch (IOException e) {
      log.error("I/O error for {}: {}.", url, e.getMessage());
      return null;
    } catch (NullPointerException e) {
      log.error("NullPointerException for {}: {}.", url, e.getMessage());
      return null;
    }

    if (cal != null) {
      writeFile(localCalendarCacheURL, calendarString);
    }

    return cal;
  }

  /**
   * Writes the contents variable to the URL passed in
   * @param file The URL of the local file you wish to write to
   * @param contents The contents of the file you wish to create
   */
  private void writeFile(URL file, String contents) {
    //TODO:  Handle file corruption issues for this block
    //TODO:  Handle UTF16, etc
    FileWriter out = null;
    try {
        out = new FileWriter(file.getFile());
        out.write(contents);
    } catch (IOException e) {
      log.error("Unable to write to {}: {}.", file, e.getMessage());
    } finally {
      try {
        if (out != null) {
          out.close();
        }
      } catch (IOException e) {
        //TODO:  What goes here?  The JVM should clean this up shortly...
      }
    }
  }

  /**
   * Convenience method to read in a file from either a remote or local source
   * @param url The URL to read the source data from
   * @return A String containing the source data
   * @throws IOException
   */
  private String readCalendar(URL url) throws IOException, NullPointerException {
    StringBuilder sb = new StringBuilder();
    //TODO:  Handle reading UTF16, and every other format under the sun
    //Urgh, read in the data.  These ugly lines handle both HTTP and local file
    DataInputStream in = new DataInputStream(url.openStream());
    int c = 0;
    while ((c = in.read()) != -1) {
      sb.append((char) c);
    }
    return sb.toString();
  }

  /**
   * Returns a string of the name for every scheduled job
   * Job titles are their DTSTART fields, so they look like 20091105T142500
   * @return An array of Strings containing the name of every scheduled job
   */
  public String[] getCaptureSchedule() {
    if (captureScheduler != null) {
      try {
        return captureScheduler.getJobNames(Scheduler.DEFAULT_GROUP);
      } catch (SchedulerException e) {
        log.error("Scheduler exception: {}.", e.toString());
      }
    }
    return null;
  }

  /**
   * Prints out the current schedules for both schedules
   * @throws SchedulerException
   */
  private void checkSchedules() throws SchedulerException {
    if (captureScheduler != null) {
      log.info("Currently scheduled jobs for capture schedule:");
      for (String name : captureScheduler.getJobNames(Scheduler.DEFAULT_GROUP)) {
        log.info("{}.", name);  
      }
    }
    if (pollScheduler != null) {
      log.info("Currently scheduled jobs for poll schedule:");
      for (String name : pollScheduler.getJobNames(Scheduler.DEFAULT_GROUP)) {
        log.info("{}.", name);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.ScheduleService#setCaptureSchedule()
   */
  private synchronized void setCaptureSchedule(Calendar newCal) {

    try {
      //Clear the existing jobs and reschedule everything
      captureScheduler.shutdown();
      SchedulerFactory sched_fact = new StdSchedulerFactory(captureProperties);
      captureScheduler = sched_fact.getScheduler();

      ComponentList list = newCal.getComponents(Component.VEVENT);
      for (Object item : list) {
        VEvent event = (VEvent) item;
        Date start = event.getStartDate().getDate();
        Date end = event.getEndDate().getDate();
        //Note that we're assuming this is accurate...
        Duration duration = event.getDuration();
        if (duration == null) {
          if (start != null && end != null) {
            //Note that in the example metadata I was given the duration field did not exist...
            duration = new Duration(start, end);
          } else {
            if (start != null) {
              log.warn("Event {} has no duration and no end time, skipping.", start.toString());
            } else if (end != null) {
              log.warn("Event {} has no duration and no begin time, skipping.", end.toString());
            } else {
              log.warn("Event has no duration, no begin and no end time, skipping.");
            }
            continue;
          }
        }

        if (duration != null && duration.getDuration().isNegative()) {
          log.warn("Event {} has a negative duration, skipping.", start.toString());
          continue;
        }

        if (start.before(new Date())) {
          log.warn("Event {} is scheduled for a time that has already passed, skipping.", start.toString());
          continue;
        }

        CronExpression startCronExpression = getCronString(start);
        CronTrigger trig = new CronTrigger();
        trig.setCronExpression(startCronExpression);
        trig.setName(startCronExpression.toString());

        JobDetail job = new JobDetail(start.toString(), Scheduler.DEFAULT_GROUP, StartCaptureJob.class);

        PropertyList attachments = event.getProperties(Property.ATTACH);
        ListIterator<Property> iter = (ListIterator<Property>) attachments.listIterator();

        boolean hasProperties = false;
        Properties props = new Properties();
        props.put(CaptureParameters.RECORDING_ID, start.toString());
        props.put(CaptureParameters.RECORDING_END, getCronString(duration.getDuration().getTime(start)).toString());

        //Create the directory we'll be capturing into
        File captureDir = new File(config.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL),
                                    props.getProperty(CaptureParameters.RECORDING_ID));
        if (!captureDir.exists()) {
          try {
            FileUtils.forceMkdir(captureDir);
          } catch (IOException e) {
            log.error("IOException creating required directory {}.", captureDir.toString());
          }
          //Should have been created.  Let's make sure of that.
          if (!captureDir.exists()) {
            log.error("Could not create required directory {}.", captureDir.toString());
          }
        }

        MediaPackage pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
        //For each attachment
        while (iter.hasNext()) {
          Property p = iter.next();
          //TODO:  Make this not hardcoded?  Make this not depend on Apple's idea of rfc 2445?
          String filename = p.getParameter("X-APPLE-FILENAME").getValue();
          if (filename == null) {
            log.warn("No filename given for attachment, skipping.");
            continue;
          }
          String contents = getAttachmentAsString(p);

          //Handle any attachments
          //TODO:  Should this string be hardcoded?
          if (filename.equals("agent.properties")) {
            Properties jobProps = new Properties();
            jobProps.load(new StringReader(contents));
            jobProps.putAll(props);
            job.getJobDataMap().put(StartCaptureJob.CAPTURE_PROPS, jobProps);
            hasProperties = true;
          } else if (filename.equals("metadata.xml")) {
            pack.add(new File(captureDir, filename).toURI(), MediaPackageElement.Type.Attachment, MediaPackageElements.DUBLINCORE_CATALOG);

            job.getJobDataMap().put(StartCaptureJob.MEDIA_PACKAGE, pack);
          }
          URL u = new File(captureDir, filename).toURI().toURL();
          writeFile(u, contents);
        }

        job.getJobDataMap().put(SchedulerImpl.CAPTURE_AGENT, captureAgent);

        if (!hasProperties) {
          log.warn("No capture properties file attached to scheduled capture {}, using default capture settings.", start.toString());
        }
        
        captureScheduler.scheduleJob(job, trig);
      }

      checkSchedules();
    } catch (NullPointerException e) {
      log.error("Invalid calendar data, one of the start or end times is incorrect: {}.", e.getMessage());
    } catch (SchedulerException e) {
      log.error("Invalid scheduling data: {}.", e.getMessage());
    } catch (ParseException e) {
      log.error("Parsing error: {}.", e.getMessage());
    } catch (org.opencastproject.util.ConfigurationException e) {
      log.error("Configuration exception: {}.", e.getMessage());
    } catch (MediaPackageException e) {
      log.error("MediaPackageException exception: {}.", e.getMessage());
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedElementException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Decodes and returns a Base64 encoded attachment
   * @param property The attachment to decode
   * @return A String representation of the attachment
   * @throws ParseException
   */
  private String getAttachmentAsString(Property property) throws ParseException {
    byte[] bytes = Base64.decodeBase64(property.getValue());
    String attach = new String(bytes);
    return attach;
  }

  /**
   * Parses an date to build a cron-like time string
   * @param date The Date you want returned in a cronstring
   * @return A cron-like scheduling string
   * @throws ParseException
   */
  private CronExpression getCronString(Date date) throws ParseException {
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
    */

      //Note:  Skipped above code because we no longer need to deal with recurring events.  Keeping it for now since they might come back.
      //Note:  "?" means no particular setting.  Equivalent to "ignore me", rather than "I don't know what to put here"
      //TODO:  Remove the deprecated calls here.
      StringBuilder sb = new StringBuilder();
      sb.append(date.getSeconds() + " ");
      sb.append(date.getMinutes() + " ");
      sb.append(date.getHours() + " ");
      sb.append(date.getDate() + " ");
      sb.append(date.getMonth() + 1 + " "); //Note:  Java numbers months from 0-11, Quartz uses 1-12.  Sigh.
      sb.append("? ");
      return new CronExpression(sb.toString());
    /*}
    return null;*/
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.ScheduleService#disableScheduler()
   */
  public void disableScheduler() {
    try {
      if (captureScheduler != null) {
        captureScheduler.pauseAll();
      }
    } catch (SchedulerException e) {
      log.error("Unable to disable capture scheduler: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.ScheduleService#enableScheduler()
   */
  public void enableScheduler() {
    try {
      if (captureScheduler != null) {
        captureScheduler.start();
      } else {
        log.error("Unable to start capture scheduler, the scheduler is null!");
      }
    } catch (SchedulerException e) {
      log.error("Unable to enable capture scheduler: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.StateService#disablePolling()
   */
  public void disablePolling() {
    try {
      if (pollScheduler != null) {
        pollScheduler.pauseAll();
      }
    } catch (SchedulerException e) {
      log.error("Unable to disable polling scheduler: {}.", e.getMessage());
    }    
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.StateService#enablePolling()
   */
  public void enablePolling() {
    try {
      if (pollScheduler != null) {
        pollScheduler.pauseAll();
      } else {
        log.error("Unable to start polling, the scheduler is null!");
      }
    } catch (SchedulerException e) {
      log.error("Unable to disable polling scheduler: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#getPollingTime()
   */
  public long getPollingTime() {
    return pollTime;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#setPollingTime(int)
   */
  public void setPollingTime(int pollingTime) {
    if (pollingTime <= 0) {
      log.warn("Unable to set polling time to less than 1 second.");
      return;
    }
    pollTime = pollingTime;
    //TODO:  Actually do something with this changed time.  This would involve rescheduling the polling.
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#getScheduleEndpoint()
   */
  public URL getScheduleEndpoint() {
    return remoteCalendarURL;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#setScheduleEndpoint(java.net.URL)
   */
  public void setScheduleEndpoint(URL url) {
    if (url == null) {
      log.warn("Invalid URL specified.");
      return;
    }
    remoteCalendarURL = url;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#isEnabled()
   */
  public boolean isEnabled() {
    if (captureScheduler != null) {
      try {
        return captureScheduler.isStarted();
      } catch (SchedulerException e) {
        log.warn("Scheduler exception: {}.", e.getMessage());
        return false;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#isPolling()
   */
  public boolean isPolling() {
    if (pollScheduler != null) {
      try {
        return pollScheduler.isStarted();
      } catch (SchedulerException e) {
        log.warn("Scheduler exception: {}.", e.getMessage());
        return false;
      }
    }
    return false;
  }

  /**
   * Shuts this whole class down, and waits until the schedulers have all terminated.
   */
  public void shutdown() {
    try {
      if (pollScheduler != null) {
          pollScheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      log.warn("Finalize for pollScheduler did not execute cleanly: {}.", e.getMessage());
    }
    try {
      if (captureScheduler != null) {
        captureScheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      log.warn("Finalize for captureScheduler did not execute cleanly: {}.", e.getMessage());
    }
  }

  public void updated(Dictionary properties) throws ConfigurationException {
    // TODO Auto-generated method stub
  }
}
