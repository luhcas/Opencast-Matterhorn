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
package org.opencastproject.job.api;

import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a wrapper around an existing job that will use a service registry to poll for job status changes.
 * <p>
 * Clients of this class can simply call <code>wait()</code> on this object and will be notified once the job status
 * changes to either one of
 * <ul>
 * <li>{@link Job.Status#FINISHED}</li>
 * <li>{@link Job.Status#FAILED}</li>
 * <li>{@link Job.Status#DELETED}</li>
 * </ul>
 */
public class BlockingJob extends JaxbJob {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(BlockingJob.class);

  /** Default polling interval is one minute */
  protected static final long DEFAULT_POLLING_INTERVAL = 60000L;

  /** The service registry used to do the polling */
  protected ServiceRegistry serviceRegistry = null;

  /** Time in milliseconds between two pools for the job status */
  protected long pollingInterval = DEFAULT_POLLING_INTERVAL;

  /** An exception that might have been thrown while polling */
  protected Exception pollingException = null;

  /**
   * Creates a wrapper for <code>job</code>, using <code>registry</code> to poll for the job outcome.
   * 
   * @param registry
   *          the registry
   */
  public BlockingJob(ServiceRegistry registry) {
    this(registry, DEFAULT_POLLING_INTERVAL);
  }

  /**
   * Creates a wrapper for <code>job</code>, using <code>registry</code> to poll for the job outcome.
   * 
   * @param job
   *          the job to poll
   * @param registry
   *          the registry
   * @param pollingInterval
   *          the time in miliseconds between two polling operations
   */
  public BlockingJob(ServiceRegistry registry, long pollingInterval) {
    this.serviceRegistry = registry;
    this.pollingInterval = pollingInterval;
  }

  /**
   * Waits for a status change and returns the new status.
   * 
   * @return the new job status
   */
  public Status waitForStatus() {
    return waitForStatus(0);
  }

  /**
   * Waits for a status change.
   */
  public Status waitForStatus(long timeout) {
    synchronized (this) {
      JobStatusUpdater updater = new JobStatusUpdater();
      try {
        updater.start();
        wait(timeout);
      } catch (InterruptedException e) {
        logger.debug("Interrupted while waiting for job");
      } finally {
        updater.interrupt();
      }
    }
    return getStatus();
  }

  /**
   * Thread that keeps polling for status changes.
   */
  class JobStatusUpdater extends Thread {

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      while (true) {

        try {
          Job.Status jobStatus = serviceRegistry.getJob(BlockingJob.this.getId()).getStatus();
          switch (jobStatus) {
          case DELETED:
          case FAILED:
          case FINISHED:
            updateAndNotify(jobStatus);
            break;
          case PAUSED:
          case QUEUED:
          case RUNNING:
            logger.trace("Job {} is still in the works", BlockingJob.this);
            // Nothing to do, let's keep waiting
            break;
          default:
            logger.error("Unhandled job status '{}' found", jobStatus);
            break;
          }
        } catch (NotFoundException e) {
          pollingException = e;
          break;
        } catch (ServiceRegistryException e) {
          logger.warn("Error polling service registry {} for job {}: {}",
                  new Object[] { serviceRegistry, BlockingJob.this, e.getMessage() });
        }

        // Wait a little..
        try {
          Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
          logger.debug("Job polling thread was interrupted");
          return;
        }

      }
    }

    /**
     * Notifies listeners about the status change.
     * 
     * @param status
     *          the status
     */
    private void updateAndNotify(Job.Status status) {
      BlockingJob.this.setStatus(status);
      synchronized (BlockingJob.this) {
        BlockingJob.this.notifyAll();
      }
    }

  }

}
