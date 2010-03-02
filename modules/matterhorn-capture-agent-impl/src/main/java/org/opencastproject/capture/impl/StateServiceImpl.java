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

import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.jobs.AgentCapabilitiesJob;
import org.opencastproject.capture.impl.jobs.AgentStateJob;
import org.opencastproject.capture.impl.jobs.JobParameters;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State service implementation.  This service keeps track of the states for the agent, as well as its recording(s).
 */
public class StateServiceImpl implements StateService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(StateServiceImpl.class);

  private Agent agent = null;
  private Hashtable<String, Recording> recordings = null;
  private ConfigurationManager configService = null;
  private Scheduler scheduler = null;

  public void setConfigService(ConfigurationManager svc) {
    configService = svc;
  }

  public void unsetConfigService() {
    configService = null;
  }

  public void activate(ComponentContext ctx) {
    recordings = new Hashtable<String, Recording>();
    agent = new Agent(configService.getItem(CaptureParameters.AGENT_NAME), AgentState.UNKNOWN,  configService.getCapabilities());
  }
  
  public void updated(Dictionary properties) throws ConfigurationException {
    Properties props = new Properties();
    Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      props.put(key, properties.get(key));
    }
    createPollingTask(props);
  }
  
  public void deactivate() {
    try {
      if (scheduler != null) {
          scheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      logger.warn("Finalize for pollScheduler did not execute cleanly: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#setRecordingState(java.lang.String, java.lang.String)
   */
  public void setRecordingState(String recordingID, String state) {
    recordings.put(recordingID, new Recording(recordingID, state));
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String recordingID) {
    return recordings.get(recordingID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getKnownRecordings()
   */
  public Map<String, Recording> getKnownRecordings() {
    return recordings;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getAgent()
   */
  public Agent getAgent() {
    return agent;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getAgentState()
   */
  public String getAgentState() {
    return agent.getState();
  }
  
  /**
   * {@inheritDoc}
   * Note that this specific implementation does nothing.  getAgentState queries directly from the CaptureAgent itself, rather than storing a string here.
   * @see org.opencastproject.capture.api.StateService#setAgentState(java.lang.String)
   */
  public void setAgentState(String state) {
    if (agent != null) {
      agent.setState(state);
    } else {
      logger.warn("Unable to set agent state because agent was null.  This is only a problem if you see this message repeating!");
    }
  }

  /**
   * Creates the Quartz task which pushes the agent's state to the state server.
   * @param schedulerProps The properties for the Quartz scheduler
   */
  private void createPollingTask(Properties schedulerProps) {
    //Either create the scheduler or empty out the existing one
    try {
      if (scheduler != null) {
        //Clear the existing jobs and reschedule everything
        for (String name : scheduler.getJobNames(JobParameters.POLLING_TYPE)) {
          scheduler.deleteJob(name, JobParameters.POLLING_TYPE);
        }
      } else {
        StdSchedulerFactory sched_fact = new StdSchedulerFactory(schedulerProps);

        //Create and start the scheduler
        scheduler = sched_fact.getScheduler();
        scheduler.start();
      }
    } catch (SchedulerException e) {
      logger.error("Scheduler exception in State Service: {}.", e.getMessage());
      return;
    }

    //Setup the agent state polling
    try {
      long statePollTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL)) * 1000L;
      
      //Setup the polling
      JobDetail stateJob = new JobDetail("agentStateUpdate", JobParameters.POLLING_TYPE, AgentStateJob.class);

      stateJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      stateJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);

      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger stateTrigger = new SimpleTrigger("state_polling", JobParameters.POLLING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, statePollTime);
      
      //Schedule the update
      scheduler.scheduleJob(stateJob, stateTrigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push state to remote server!", CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule state polling: {}.", e.getMessage());
    }

    //Setup the agent capabilities polling
    try {
      long capbsPollTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL)) * 1000L;
      
      //Setup the polling
      JobDetail capbsJob = new JobDetail("agentCapabilitiesUpdate", JobParameters.POLLING_TYPE, AgentCapabilitiesJob.class);

      capbsJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      capbsJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);     
      
      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger capbsTrigger = new SimpleTrigger("capabilities_polling", JobParameters.POLLING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, capbsPollTime);
      
      //Schedule the update
      scheduler.scheduleJob(capbsJob, capbsTrigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push state to remote server!", CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule capability polling: {}.", e.getMessage());
    }
  }
}
