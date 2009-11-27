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

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import org.opencastproject.capture.api.StatusService;
import org.opencastproject.capture.impl.jobs.AgentStatusJob;
import org.osgi.service.component.ComponentContext;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateSingleton implements StatusService {
  private static final Logger logger = LoggerFactory.getLogger(StateSingleton.class);

  private static StateSingleton instance = null;
  private CaptureAgentImpl service = null;
  private Hashtable<String, String> recordings = null;

  /**
   * Private constructor
   */
  private StateSingleton() {
    recordings = new Hashtable<String, String>();
    createPollingTask();
  }

  /**
   * Called when the bundle is activated.
   * @param cc The component context
   */
  public void activate(ComponentContext cc) {
    getInstance();
  }


  public static synchronized StateSingleton getInstance() {
    if (instance == null) {
      instance = new StateSingleton();
    }
    return instance;
  }

  public CaptureAgentImpl getCaptureAgent() {
    return service;
  }

  public void setCaptureAgent(CaptureAgentImpl sourceService) {
    service = sourceService;
  }

  /**
   * Sets the recording's current state
   * 
   * @param recordingID The ID of the recording.
   * @param state The state for the recording.  Defined in RecordingState.
   * @see org.opencastproject.capture.api.RecordingState
   */
  public void setRecordingState(String recordingID, String state) {
    recordings.put(recordingID, state);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StatusService#getRecordingState(java.lang.String)
   */
  public String getRecordingState(String recordingID) {
    return recordings.get(recordingID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StatusService#getRecordings()
   */
  public Set<String> getRecordings() {
    return recordings.keySet();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StatusService#getAgentState()
   */
  public String getAgentState() {
    if (service != null) {
      return service.getAgentState();
    } else {
      return null;
    }
  }

  /**
   * Creates the Quartz task which pushes the agent's status to the status server
   */
  private void createPollingTask() {
    try {
      long pollTime = Long.parseLong(ConfigurationManager.getInstance().getItem(CaptureParameters.AGENT_STATUS_POLLING_INTERVAL)) * 1000L;
      Properties pollingProperties = new Properties();
      pollingProperties.load(getClass().getClassLoader().getResourceAsStream("config/state_update.properties"));
      StdSchedulerFactory sched_fact = new StdSchedulerFactory(pollingProperties);
  
      //Create and start the scheduler
      Scheduler pollScheduler = sched_fact.getScheduler();
      if (pollScheduler.getJobGroupNames().length > 0) {
        logger.info("createPollingTask has already been called.  Stop freakin' calling it already!");
        return;
      }
      pollScheduler.start();
  
      //Setup the polling
      JobDetail job = new JobDetail("agentStatusUpdate", Scheduler.DEFAULT_GROUP, AgentStatusJob.class);
      //TODO:  Support changing the polling interval
      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger trigger = new SimpleTrigger("status_polling", Scheduler.DEFAULT_GROUP, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, pollTime);

      //Schedule the update
      pollScheduler.scheduleJob(job, trigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push status to remote server!", CaptureParameters.AGENT_STATUS_POLLING_INTERVAL);
    } catch (IOException e) {
      logger.error("IOException caught in StateSingleton: {}.", e.getMessage());
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateSingleton: {}.", e.getMessage());
    }
  }
}
