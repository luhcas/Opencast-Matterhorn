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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.util.FileSupport;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * Cleans up captures if the capture has been successfully ingested and the 
 * remaining diskspace is below a minimum threshold or above a maximum archive 
 * days threshold.
 */
public class CleanCaptureJob implements Job {
  
  private static final Logger logger = LoggerFactory.getLogger(CleanCaptureJob.class);
  
  /** Constant used to define the key for the properties object which is pulled out of the execution context */
  public static final String CAPTURE_PROPS = "capture_props";
  
  /** File signifying ingestion of media has been completed */
  public static final String CAPTURE_INGESTED = "captured.ingested";
  
  /** The length of one day represented in milliseconds */
  public static final long DAY_LENGTH_MILLIS = 86400000;
  
  /**
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    try {
      Properties p = (Properties) ctx.getMergedJobDataMap().get(CAPTURE_PROPS);
      File rootDir = new File(p.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
      
      if (rootDir.isDirectory()) {
        String[] children = rootDir.list();
        for (String subDir : children) {
          try {
            attemptClean(new File(rootDir, subDir), p);
          } catch (Exception e) {
            logger.error("Error cleaning directory: {}. {}", subDir, e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      logger.error("Could not execute job. {}", e.getMessage());
    }
  }
  
  /**
   * If the directory meets the appropriate criteria, recursively delete it.
   * 
   * @param dir
   *          the candidate capture directory
   * @param p
   *          cleaning properties
   *          
   */
  public boolean attemptClean(File dir, Properties p) {
    // if the capture.ingested file does not exist we cannot delete the data
    File ingested = new File(dir, CAPTURE_INGESTED);
    if (!ingested.exists()) {
      logger.info("Skipped cleaning for {}. Ingestion has not been completed.", dir.getAbsolutePath());
      return false;
    }
    
    // clean up capture if its age of ingestion is higher than max archival days property
    String maxDays = p.getProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS);
    if (maxDays != null) {
      long age = ingested.lastModified();
      long currentTime = System.currentTimeMillis();
      long maxArchivalDays = Long.parseLong(maxDays);
      if (currentTime - age > (maxArchivalDays * DAY_LENGTH_MILLIS)) {
        logger.info("Removing capture archive: {}. Exceeded the maximum archival days.", dir.getAbsolutePath());
        FileSupport.delete(dir, true);
        return true;
      }
      else {
        logger.debug("Archive: {} has not yet exceeded the maximum archival days, not deleting...", dir.getAbsolutePath());
      }
    }
    
    // clean up if we are running out of disk space 
    // TODO: Support Java 1.5 (dir.getFreeSpace() is 1.6 only)
    String minSpace = p.getProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE);
    if (minSpace != null) {
      long freeSpace = dir.getFreeSpace();
      long minDiskSpace = Long.parseLong(minSpace);
      if (freeSpace < minDiskSpace) {
        logger.info("Removing capture archive: {}. Under minimum free disk space.", dir.getAbsolutePath());
        FileSupport.delete(dir, true);
        return true;
      }
      else {
        logger.debug("Archive: {} not removed, enough disk space remains for archive.", dir.getAbsolutePath());
      }
    }
    
    logger.info("Archive: {} not deleted.", dir.getAbsolutePath());
    return false;
  }

}
