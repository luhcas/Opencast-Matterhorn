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
 * 
 * FIXME: This functionality should be moved to CaptureAgentImpl in order to prevent circular dependencies. Also, the
 * state is actually determined by the client itself, so this is probably where it should be kept. If you really want
 * to keep a separate interface, you can keep the StateService, but implement it in CaptureAgentImpl. (jt)
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
    if (properties == null) {
      throw new ConfigurationException("null", "Null configuration in StateServiceImpl!");
    }

    Properties props = new Properties();
    Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      props.put(key, properties.get(key));
    }
    createPushTask(props);
  }
  
  public void deactivate() {
    try {
      if (scheduler != null) {
          scheduler.shutdown(true);
      }
    } catch (SchedulerException e) {
      logger.warn("Finalize for scheduler did not execute cleanly: {}.", e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#setRecordingState(java.lang.String, java.lang.String)
   */
  public void setRecordingState(String recordingID, String state) {
    if (recordings != null && recordingID != null && state != null) {
      recordings.put(recordingID, new Recording(recordingID, state));
    } else if (recordingID == null) {
      logger.info("Unable to create recording because recordingID parameter was null!");
    } else if (state == null) {
      logger.info("Unable to create recording because state parameter was null!");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.StateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String recordingID) {
    if (recordings != null) {
      return recordings.get(recordingID);
    } else {
      logger.debug("Agent is not ready yet, returning null for getRecordingState call.");
      return null;
    }
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
    if (agent != null) {
      return agent.getState();
    } else {
      logger.debug("Agent is not ready yet, returning null for getAgentState call.");
      return null;
    }
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
  private void createPushTask(Properties schedulerProps) {
    //Either create the scheduler or empty out the existing one
    try {
      if (scheduler != null) {
        //Clear the existing jobs and reschedule everything
        for (String name : scheduler.getJobNames(JobParameters.RECURRING_TYPE)) {
          scheduler.deleteJob(name, JobParameters.RECURRING_TYPE);
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

    //Setup the agent state push jobs
    try {
      long statePushTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL)) * 1000L;
      
      //Setup the push job
      JobDetail stateJob = new JobDetail("agentStateUpdate", JobParameters.RECURRING_TYPE, AgentStateJob.class);

      stateJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      stateJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);

      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger stateTrigger = new SimpleTrigger("state_push", JobParameters.RECURRING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, statePushTime);
      
      //Schedule the update
      scheduler.scheduleJob(stateJob, stateTrigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push state to remote server!", CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule state push jobs: {}.", e.getMessage());
    }

    //Setup the agent capabilities push jobs
    try {
      long capbsPushTime = Long.parseLong(configService.getItem(CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL)) * 1000L;
      
      //Setup the push job
      JobDetail capbsJob = new JobDetail("agentCapabilitiesUpdate", JobParameters.RECURRING_TYPE, AgentCapabilitiesJob.class);

      capbsJob.getJobDataMap().put(JobParameters.STATE_SERVICE, this);
      capbsJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);     
      
      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger capbsTrigger = new SimpleTrigger("capabilities_polling", JobParameters.RECURRING_TYPE, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, capbsPushTime);
      
      //Schedule the update
      scheduler.scheduleJob(capbsJob, capbsTrigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the {} value, unable to push capabilities to remote server!", CaptureParameters.AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StateServiceImpl while trying to schedule capability push jobs: {}.", e.getMessage());
    }
    
  }
}
