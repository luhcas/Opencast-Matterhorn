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
import java.util.Dictionary;
import java.util.Properties;

import org.opencastproject.capture.api.AgentState;
import org.opencastproject.capture.api.StatusService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see org.opencastproject.capture.api.StatusService
 */
public class StatusServiceImpl implements StatusService, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(StatusServiceImpl.class);
  public static final String SERVICE = "StatusServiceImpl";
  private String cur_status = null;
  private ConfigurationManager config = null;

  /**
   * Creates an instance of the status service, and sets its initial state to AgentState.IDLE
   */
  public StatusServiceImpl() {
    logger.info("Starting StatusServiceImpl");
    if (cur_status == null) {
      cur_status = AgentState.IDLE;
    }
    config = ConfigurationManager.getInstance();

    try {
      long pollTime = Long.parseLong(config.getItem(CaptureParameters.AGENT_STATUS_POLLING_INTERVAL)) * 1000L;
      Properties pollingProperties = new Properties();
      pollingProperties.load(getClass().getClassLoader().getResourceAsStream("config/misc.properties"));
      StdSchedulerFactory sched_fact = new StdSchedulerFactory(pollingProperties);
  
      //Create and start the scheduler
      Scheduler pollScheduler = sched_fact.getScheduler();
      if (pollScheduler.getJobGroupNames().length > 0) {
        logger.info("StatusServiceImpl's constructor has already been called.  Stop freakin' calling it already!");
        return;
      }
      pollScheduler.start();
  
      //Setup the polling
      JobDetail job = new JobDetail("agentStatusUpdate", Scheduler.DEFAULT_GROUP, AgentStatusJob.class);
      //TODO:  Support changing the polling interval
      //Create a new trigger                    Name              Group name               Start       End   # of times to repeat               Repeat interval
      SimpleTrigger trigger = new SimpleTrigger("status_polling", Scheduler.DEFAULT_GROUP, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, pollTime);

      trigger.getJobDataMap().put(SERVICE, this);

      //Schedule the update
      pollScheduler.scheduleJob(job, trigger);
    } catch (NumberFormatException e) {
      logger.error("Invalid time specified in the " + CaptureParameters.AGENT_STATUS_POLLING_INTERVAL + " value, unable to push status to remote server!");
    } catch (IOException e) {
      logger.error("IOException caught in StatusServiceImpl", e);
    } catch (SchedulerException e) {
      logger.error("SchedulerException in StatusServiceImpl", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.StatusService#getState()
   */
  public String getState() {
    return cur_status;
  }

  public void start() {
    cur_status = AgentState.CAPTURING;
  }

  public void stop() {
    if (cur_status == AgentState.CAPTURING) {
      cur_status = AgentState.UPLOADING;
    } else {
      cur_status = AgentState.IDLE;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.StatusService#setState()
   */
  public void setState(String state) {
    if (state.equals(AgentState.CAPTURING) || state.equals(AgentState.IDLE) || state.equals(AgentState.UPLOADING)) {
      cur_status = state;
    } else {
      //TODO:  What should I be doing here?
    }
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }
}
