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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.jobs.CleanCaptureJob;
import org.opencastproject.capture.impl.jobs.JobParameters;
import org.opencastproject.capture.impl.jobs.PollCalendarJob;
import org.opencastproject.capture.impl.jobs.SerializeJob;
import org.opencastproject.capture.impl.jobs.StartCaptureJob;
import org.opencastproject.capture.impl.jobs.StopCaptureJob;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Duration;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

/**
 * Scheduler implementation class.  This class is responsible for retrieving iCal
 * and then scheduling captures from the resulting calendaring data.
 */
public class SchedulerImpl implements org.opencastproject.capture.api.Scheduler, ManagedService {

  /** Log facility */
  private static final Logger log = LoggerFactory.getLogger(SchedulerImpl.class);

  /** The scheduler used for all of the scheduling */
  private Scheduler scheduler = null;

  /** The stored URL for the remote calendar source */
  private URL remoteCalendarURL = null;

  /** The URL of the cached copy of the recording schedule */
  private URL localCalendarCacheURL = null;

  /** The time in milliseconds between attempts to fetch the calendar data */
  private long pollTime = 0;

  /** A stored copy of the Calendar at the URI */
  private Calendar calendar = null;

  /** The configuration for this service */
  private ConfigurationManager configService = null;

  /** The capture agent this scheduler is scheduling for */
  private CaptureAgentImpl captureAgent = null;

  /** The maximum duration to schedule something for */
  private Dur maxDuration = new Dur(new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + (CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH * 1000)));


  public void setConfigService(ConfigurationManager svc) {
    configService = svc;
  }

  public void unsetConfigService() {
    configService = null;
  }

  /**
   * Called when the bundle is deactivated.  This function shuts down all of the schedulers.
   */
  public void deactivate() {
    shutdown();
  }

  /**
   * Updates the scheduler with new configuration data.
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    try {
      localCalendarCacheURL = new File(configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL)).toURI().toURL();
    } catch (NullPointerException e) {
      log.warn("Invalid location specified for {} unable to cache scheduling data.", CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL);
    } catch (MalformedURLException e) {
      log.warn("Invalid location specified for {} unable to cache scheduling data.", CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL);
    }

    if (properties == null) {
      log.debug("Null properties in updated!");
      throw new ConfigurationException("Null properties in updated!", "null");
    }

    try {
      //Load the required properties
      Properties props = new Properties();
      Enumeration<String> keys = properties.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        props.put(key, properties.get(key));
      }

      //Create the scheduler!
      SchedulerFactory sched_fact = null;
      sched_fact = new StdSchedulerFactory(props);
      //Create and start the scheduler
      scheduler = sched_fact.getScheduler();
      setupPolling();
      updateCalendar();
      scheduleCleanJob();
      scheduler.start();
    } catch (SchedulerException e) {
      throw new ConfigurationException("Internal error in scheduler, unable to start.", e.getMessage());
    }
  }

  /**
   * Creates the polling task with the 
   */
  private void setupPolling() {
    if (scheduler == null) {
      log.warn("Unable to setup polling because internal scheduler is null.");
      return;
    }

    try {
      //Nuke any existing polling tasks
      for (String name : scheduler.getJobNames(JobParameters.RECURRING_TYPE)) {
        scheduler.deleteJob(name, JobParameters.RECURRING_TYPE);
      }

      //Find the remote endpoint for the scheduler
      String remoteBase = StringUtils.trimToNull(configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL));
      if (remoteBase != null && remoteBase.charAt(remoteBase.length()-1) != '/') {
        remoteBase = remoteBase + "/";
      } else if (remoteBase == null) {
        log.error("Key {} is missing from the config file or invalid, unable to start polling.", CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL);
        return;
      }
      remoteCalendarURL = new URL(new URL(remoteBase), configService.getItem(CaptureParameters.AGENT_NAME));

      //Times are in seconds in the config file, so don't forget to multiply by 1000 later!
      pollTime = Long.parseLong(configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL));
      if (pollTime > 1) {
        //Setup the polling
        JobDetail job = new JobDetail("calendarUpdate", JobParameters.RECURRING_TYPE, PollCalendarJob.class);
        //Create a new trigger                    Name       Group name               Start       End   # of times to repeat               Repeat interval
        SimpleTrigger trigger = new SimpleTrigger("polling", JobParameters.RECURRING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, pollTime * 1000L);
        trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

        trigger.getJobDataMap().put(JobParameters.SCHEDULER, this);

        //Schedule the update
        scheduler.scheduleJob(job, trigger);
      } else {
        log.info("{} has been set to less than 1, calendar updates disabled.", CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL);
      }
    } catch (StringIndexOutOfBoundsException e) {
      log.warn("Unable to build valid scheduling data endpoint from key {}: {}.  Value must end in a / character.", CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL));
    } catch (MalformedURLException e) {
      log.warn("Invalid location specified for {} unable to retrieve new scheduling data: {}.", CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL));
    } catch (NumberFormatException e) {
      log.warn("Invalid polling interval for {} unable to retrieve new scheduling data: {}.", CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL));
    } catch (SchedulerException e) {
      log.warn("Scheduler exception when attempting to start polling tasks: {}", e.toString());
    }
  }

  /**
   * Sets the capture agent which this scheduler should be scheduling for.
   * @param agent The agent.
   */
  public void setCaptureAgent(CaptureAgentImpl agent) {
    captureAgent = agent;
    if (agent != null) {
      agent.setScheduler(this);
    }
  }

  /**
   * Sets the capture agent for this scheduler to null.
   */
  public void unsetCaptureAgent() {
    captureAgent = null;
  }

  /**
   * Overridden finalize function to shutdown all Quartz schedulers.
   * @see java.lang.Object#finalize()
   */
  @Override
  public void finalize() {
    shutdown();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#updateCalendar()
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
      log.debug("Calendar already exists, and {} is invalid, skipping update.", CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL);
      return;
    }

    //Now set the calendar data
    if (calendar != null) {
      setCaptureSchedule(calendar);
    } else {
      log.debug("Calendar parsing routine returned null, skipping update of capture schedule.");
    }
  }

  /**
   * Reads in a calendar from either an HTTP or local source and turns it into a iCal4j Calendar object.
   * @param url The URL to read the calendar data from.
   * @return A calendar object, or null in the case of an error or to indicate that no update should be performed.
   */
  private Calendar parseCalendar(URL url) {

    String calendarString = null;
    try {
      calendarString = readCalendar(url);
    } catch (Exception e) {
      log.debug("Parsing exception: {}.", e.toString());
      //If the calendar is null, which only happens when the machine has *just* been powered on.
      //This case handles not having a network connection by just reading from the cached copy of the calendar 
      if (calendar == null) {
        try {
          if (localCalendarCacheURL != null) {
            calendarString = readCalendar(localCalendarCacheURL);
            url = localCalendarCacheURL;
          } else {
            log.warn("Unable to read calendar from local calendar cache because location was null!");
            return null;
          }
        } catch (IOException e1) {
          log.warn("Unable to read from cached schedule: {}.", e1.getMessage());
          return null;
        }
      } else {
        log.debug("Calendar already exists, and {} is invalid, skipping update.", CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL);
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

    if (cal != null && localCalendarCacheURL != null) {
      writeFile(localCalendarCacheURL, calendarString);
    }

    return cal;
  }

  /**
   * Writes the contents variable to the URL.  Note that the URL must be a local URL.
   * @param file The URL of the local file you wish to write to.
   * @param contents The contents of the file you wish to create.
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
      } catch (IOException e) {}
    }
  }

  /**
   * Convenience method to read in a file from either a remote or local source.
   * @param url The URL to read the source data from.
   * @return A String containing the source data.
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
   * Returns the name for every scheduled job.
   * Job titles are their UUIDs assigned from the scheduler, or Unscheduled-$timestamp.
   * @return An array of Strings containing the name of every scheduled job, or null if there is an error.
   */
  public String[] getCaptureSchedule() {
    if (scheduler != null) {
      try {
        return scheduler.getJobNames(JobParameters.CAPTURE_TYPE);
      } catch (SchedulerException e) {
        log.error("Scheduler exception: {}.", e.toString());
      }
    }
    log.warn("Internal scheduler was null, returning null!");
    return null;
  }

  /**
   * Prints out the current schedules for both schedules.
   * @throws SchedulerException
   */
  private void checkSchedules() throws SchedulerException {
    if (scheduler != null) {
      log.debug("Currently scheduled jobs for capture schedule:");
      for (String name : scheduler.getJobNames(JobParameters.CAPTURE_TYPE)) {
        log.debug("{}.", name);  
      }
      log.debug("Currently scheduled jobs for poll schedule:");
      for (String name : scheduler.getJobNames(JobParameters.RECURRING_TYPE)) {
        log.debug("{}.", name);
      }
      log.debug("Currently scheduled jobs for other schedule:");
      for (String name : scheduler.getJobNames(JobParameters.OTHER_TYPE)) {
        log.debug("{}.", name);
      }
    }
  }

  /**
   * Sets this machine's schedule based on the iCal data passed in as a parameter.
   * Note that this call wipes all currently scheduled captures and then schedules based on the new data.
   * Also note that any files which are in the way when this call tries to save the iCal attachments are overwritten without prompting.
   * @param newCal The new calendar data
   */
  private synchronized void setCaptureSchedule(Calendar newCal) {
    log.debug("setCaptureSchedule(newCal)");

    try {
      //Clear the existing jobs and reschedule everything
      for (String name : scheduler.getJobNames(JobParameters.CAPTURE_TYPE)) {
        scheduler.deleteJob(name, JobParameters.CAPTURE_TYPE);
      }

      Map<String, String> scheduledEvents = new Hashtable<String, String>();
      ComponentList list = newCal.getComponents(Component.VEVENT);
      for (Object item : list) {
        VEvent event = (VEvent) item;

        //Get the ID, or generate one
        String uid = null;
        if (event.getUid() != null) {
          uid = event.getUid().getValue();
        } else {
          log.warn("Event has no UID field, autogenerating name...");
          uid = "UnknownUID-" + item.hashCode(); 
        }

        //Get the start time
        Date start = null;
        if (event.getStartDate() != null) {
          start = event.getStartDate().getDate();          
        } else {
          log.error("Event {} has no start time, unable to schedule!", uid);
          continue;
        }

        //Get the end time
        Date end = null;
        if (event.getEndDate() != null) {
          end = event.getEndDate().getDate();
        } else {
          log.debug("Event {} has no end time...", uid);
        }

        //Determine the duration
        //Note that we're assuming this is accurate...
        Duration duration = event.getDuration();
        if (duration == null) {
          if (end != null) {
            duration = new Duration(start, end);
          } else {
            log.warn("Event {} has no duration and no end time, skipping.", uid);
            continue;
          }
        }

        //Sanity checks on the duration
        if (duration.getDuration().isNegative()) {
          log.warn("Event {} has a negative duration, skipping.", uid);
          continue;
        } else if (start.before(new Date())) {
          log.debug("Event {} is scheduled for a time that has already passed, skipping.", uid);
          continue;
        } else if (duration.getDuration().compareTo(new Dur(0,0,0,0)) == 0) {
          log.warn("Event {} has a duration of 0, skipping.", uid);
          continue;
        }

        //Check to make sure the recording isn't set for more than the maximum length
        if (duration.getDuration().compareTo(maxDuration) > 0) {
          log.warn("Event {} set to longer than maximum allowed capture duration, cutting off capture at {} seconds.", uid, CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH);
          duration.setDuration(new Dur(0,8,0,0));
        }

        CronExpression startCronExpression = getCronString(start);
        String conflict = scheduledEvents.get(startCronExpression.toString()); 
        if (conflict != null) {
          log.warn("Unable to schedule event {} because its starting time coinsides with event {}!", uid, conflict);
          continue;
        }
        CronTrigger trig = new CronTrigger();
        trig.setCronExpression(startCronExpression);
        trig.setName(startCronExpression.toString());
        trig.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);

        JobDetail job = new JobDetail(uid, JobParameters.CAPTURE_TYPE, StartCaptureJob.class);

        Properties props = new Properties();
        props.put(CaptureParameters.RECORDING_ID, uid);
        props.put(JobParameters.JOB_POSTFIX, uid);
        String endCronString = getCronString(duration.getDuration().getTime(start)).toString();
        props.put(CaptureParameters.RECORDING_END, endCronString);

        
        if (!scheduleEvent(event, props, job)) {
          log.warn("No capture properties file attached to scheduled capture {}, using default capture settings.", uid);
        }
        scheduler.scheduleJob(job, trig);
        scheduledEvents.put(startCronExpression.toString(), uid);
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
    } catch (MalformedURLException e) {
      log.error("MalformedURLException: {}.", e.getMessage());
    }
  }

  private boolean scheduleEvent(VEvent event, Properties props, JobDetail job) throws org.opencastproject.util.ConfigurationException, MediaPackageException, MalformedURLException, ParseException {
    MediaPackage pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    boolean hasProperties = false;

    //Create the directory we'll be capturing into
    File captureDir = new File(configService.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL),
            props.getProperty(CaptureParameters.RECORDING_ID));
    if (!captureDir.exists()) {
      try {
        FileUtils.forceMkdir(captureDir);
      } catch (IOException e) {
        log.error("IOException creating required directory {}, skipping capture.", captureDir.toString());
        return false;
      }
    }

    PropertyList attachments = event.getProperties(Property.ATTACH);
    ListIterator<Property> iter = (ListIterator<Property>) attachments.listIterator();
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
      try { 
        if (filename.equals("org.opencastproject.capture.agent.properties")) {
          Properties jobProps = new Properties();
          jobProps.load(new StringReader(contents));
          jobProps.putAll(props);
          job.getJobDataMap().put(JobParameters.CAPTURE_PROPS, jobProps);
          hasProperties = true;
          pack.add(new URI(filename));
        } else if (filename.equals("metadata.xml"))
          pack.add(new URI(filename), MediaPackageElement.Type.Catalog, MediaPackageElements.DUBLINCORE_CATALOG);
        else
          pack.add(new URI(filename));
      } catch(Exception e) {};
      job.getJobDataMap().put(JobParameters.MEDIA_PACKAGE, pack);
      //Note that we overwrite any pre-existing files with this.  In other words, if there is a file called foo.txt in the
      //captureDir directory and there is an attachment called foo.txt then we will overwrite the one on disk with the one from the ical
      URL u = new File(captureDir, filename).toURI().toURL();
      writeFile(u, contents);
    }

    job.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgent);
    job.getJobDataMap().put(JobParameters.JOB_SCHEDULER, scheduler);

    return hasProperties;
  }

  /**
   * Decodes and returns a Base64 encoded attachment as a String.  Note that this function does *not*
   * attempt to guess what the file might actually be.
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
   * Parses an date to build a cron-like time string.
   * @param date The Date you want returned in a cronstring.
   * @return A cron-like scheduling string.
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
   * Schedules a {@Code StopCaptureJob} to stop a capture at a given time.
   * @param recordingID The recordingID of the recording you wish to stop.
   * @param stop The time (in seconds since 1970) at which to stop the capture.
   * @return True if the job was scheduled, false otherwise.
   */
  public boolean scheduleUnscheduledStopCapture(String recordingID, Date stop) {
    SimpleTrigger trig = new SimpleTrigger("StopCaptureTrigger-" + recordingID, JobParameters.OTHER_TYPE, stop);
    trig.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
    JobDetail job = new JobDetail("StopCapture-" + recordingID, JobParameters.OTHER_TYPE, StopCaptureJob.class);

    job.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgent);
    job.getJobDataMap().put(JobParameters.JOB_POSTFIX, recordingID);
    job.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);

    try {
      scheduler.scheduleJob(job, trig);
    } catch (SchedulerException e) {
      log.error("Unable to schedule stop capture, failing capture attempt.");
      return false;
    }
    return true; 
  }

  /**
   * Schedules a {@Code StopCaptureJob} to stop a capture at a given time.
   * @param recordingID The recordingID of the recording you wish to stop.
   * @param atTime The time (in seconds since 1970) at which to stop the capture.
   * @return True if the job was scheduled, false otherwise.
   */
  public boolean scheduleUnscheduledStopCapture(String recordingID, long atTime) {
    return scheduleUnscheduledStopCapture(recordingID, new Date(atTime));
  }

  /**
   * Schedules an immediate {@code SerializeJob} for the recording.
   * @param recordingID The ID of the recording to it ingest.
   * @return True if the job was scheduled correctly, false otherwise.
   */
  public boolean scheduleIngest(String recordingID) {
    try {
      String[] jobs = scheduler.getJobNames(JobParameters.OTHER_TYPE);
      for (String jobname : jobs) {
        if (jobname.equals("StopCapture-" + recordingID)) {
          scheduler.deleteJob(jobname, JobParameters.OTHER_TYPE);
        }
      }
    } catch (SchedulerException e) {
      log.warn("Unable to remove scheduled stopCapture for recording {}.", recordingID);
    }

    // Create job and trigger
    JobDetail job = new JobDetail("SerializeJob-" + recordingID, JobParameters.OTHER_TYPE, SerializeJob.class);

    //Setup the trigger.  The serialization job will automatically refire if it fails, so we don't need to worry about it
    SimpleTrigger trigger = new SimpleTrigger("SerializeJobTrigger-" + recordingID, JobParameters.OTHER_TYPE, new Date());
    trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

    trigger.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);
    trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, this.captureAgent);
    trigger.getJobDataMap().put(JobParameters.JOB_POSTFIX, recordingID);

    try {
      scheduler.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      log.error("Unable to schedule ingest of recording {}!", recordingID);
      return false;
    }
    return true;
  }

  /**
   * Schedules the job in charge of cleaning the old captures
   */
  private void scheduleCleanJob() {

    try {
      long cleanInterval = Long.parseLong(configService.getItem(CaptureParameters.CAPTURE_CLEANER_INTERVAL)) * 1000L;

      //Setup the polling
      JobDetail cleanJob = new JobDetail("cleanCaptures", JobParameters.OTHER_TYPE, CleanCaptureJob.class);

      cleanJob.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgent);
      cleanJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);

      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger cleanTrigger = new SimpleTrigger("cleanCapture", JobParameters.RECURRING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, cleanInterval);

      //Schedule the update
      scheduler.scheduleJob(cleanJob, cleanTrigger);
    } catch (NumberFormatException e) {
      log.warn("Invalid time specified in the {} value. Job for cleaning captures not scheduled!", CaptureParameters.CAPTURE_CLEANER_INTERVAL);
    } catch (SchedulerException e) {
      log.error("SchedulerException while trying to schedule a cleaning job: {}.", e.getMessage());
    }

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#stopScheduler()
   */
  public void stopScheduler() {
    log.debug("stopScheduler()");
    try {
      if (scheduler != null) {
        scheduler.pauseAll();
      }
    } catch (SchedulerException e) {
      log.error("Unable to disable capture scheduler: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#startScheduler()
   */
  public void startScheduler() {
    log.debug("startScheduler()");
    try {
      if (scheduler != null) {
        scheduler.start();
      } else {
        log.error("Unable to start capture scheduler, the scheduler is null!");
      }
    } catch (SchedulerException e) {
      log.error("Unable to enable capture scheduler: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#enablePolling(boolean)
   */
  public void enablePolling(boolean enable) {
    log.debug("enablePolling(enable): {}", enable);
    try {
      if (enable) {
        if (scheduler != null) {
          scheduler.resumeJobGroup(JobParameters.RECURRING_TYPE);
        } else {
          log.error("Unable to start polling, the scheduler is null!");
        }
      } else {
        if (scheduler != null) {
          scheduler.pauseJobGroup(JobParameters.RECURRING_TYPE);
        }        
      }
    } catch (SchedulerException e) {
      log.error("Unable to disable polling scheduler: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#getPollingTime()
   */
  public int getPollingTime() {
    return (int) (pollTime / 1000L);
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
   * @see org.opencastproject.capture.api.Scheduler#isSchedulerEnabled()
   */
  public boolean isSchedulerEnabled() {
    if (scheduler != null) {
      try {
        return scheduler.isStarted();
      } catch (SchedulerException e) {
        log.warn("Scheduler exception: {}.", e.getMessage());
        return false;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.Scheduler#isPollingEnabled()
   */
  public boolean isPollingEnabled() {
    if (scheduler != null) {
      try {
        return scheduler.getTrigger("polling", JobParameters.RECURRING_TYPE) != null;
      } catch (SchedulerException e) {
        log.warn("Scheduler exception: {}.", e.getMessage());
        return false;
      }
    } else {
      log.warn("Scheduler is null, so polling cannot be enabled!");
    }
    return false;
  }

  /**
   * Shuts this whole class down, and waits until the schedulers have all terminated.
   */
  public void shutdown() {
    try {
      if (scheduler != null) {
        scheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      log.warn("Finalize for scheduler did not execute cleanly: {}.", e.getMessage());
    }
  }
}
