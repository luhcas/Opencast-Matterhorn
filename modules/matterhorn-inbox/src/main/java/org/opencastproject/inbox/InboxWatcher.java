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
package org.opencastproject.inbox;

import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and starts FileWatchingThreads for each new founded file in the inbox.
 */
public class InboxWatcher extends Thread {
  
  /**
   * Default value for WorkingFileRepository Collection ID
   */
  public static final String INBOX_WFR_COLLECTION_ID = "inbox";
  /**
   * Matterhorn Logger
   */
  protected static final Logger logger = LoggerFactory.getLogger(InboxWatcher.class);
  
  /**
   * Matterhorn WorkingFileRepository for pushing uploaded files into 
   */
  protected WorkingFileRepository wfrService;
  /**
   * Properties Dictionary
   */
  protected Dictionary properties;
  /**
   * Table with FileWatchingThreads
   */
  protected Map<String,FileWatchingThread> watchingThreads;
  
  /**
   * Constructor.
   * @param properties Properties
   * @param workingFileRepositoryService Matterhorn WorkingFileRepository
   */
  public InboxWatcher(Dictionary properties, WorkingFileRepository workingFileRepositoryService) {
    // set properties
    if (properties == null) {
      logger.error("Properties dictionary is null!");
      return;
    } else {
      this.properties = properties;
      this.wfrService = workingFileRepositoryService;
      this.watchingThreads = Collections.synchronizedMap(new Hashtable<String, FileWatchingThread>());
    }
    
    // test inbox-dir exist, create otherwise
    File inboxDir = new File(getInboxPath());
    if (!inboxDir.exists()) {
      try {
        inboxDir.mkdir();
      } catch (SecurityException ex) {
        logger.error("Inboxdir is not exist and failed create (maybe you have not read/write permissions on the dir: '" +
               inboxDir.getAbsolutePath() + "').");
        return;
      }
    }
  }
  
  /**
   * Set WorkingFileRepository Service
   * @param workingFileRepository Matterhorn WorkingFileRepository Service
   */
  public void setWorkingFileRepositoryService(WorkingFileRepository workingFileRepository) {
      this.wfrService = workingFileRepository;
  }
  
  /**
   * Unset WorkingFileRepository Service
   * @param workingFileRepository Matterhorn WorkingFileRepository Service
   */
  public void unsetWorkingFileRepositoryService(WorkingFileRepository workingFileRepository) {
      this.wfrService = null;
  }
  
  /**
   * Get WorkingFileRepository Service
   * @return Matterhorn WorkingFileRepository Service
   */
  public WorkingFileRepository getWorkingFileRepository() {
      return this.wfrService;
  }
  
  /**
   * Update properties
   * @param properties properties
   */
  public void updateProperties(Dictionary properties) {
    this.properties = properties;
    logger.info("UPDATED");
  }
  
  /**
   * Get inbox path from properties. If properties does not contain 'inboxPath' key, then return DEFAULT_INBOX_PATH
   * @return inbox path
   */
  protected String getInboxPath() {
    try {
      return (String) this.properties.get("inboxPath");
    } catch (NullPointerException ex) {
      logger.info("Can not get 'inboxPath' property. Use default value: " + InboxService.DEFAULT_INBOX_PATH);
      return InboxService.DEFAULT_INBOX_PATH;
    }
  }
  
  /**
   * Get refresh time (in sec) from properties. If properties does not contain 'refreshTime' key, then return DEFAULT_FILE_REFRESH_TIME
   * @return file refresh time 
   */
  protected Integer getFileRefreshTime() {
    try {
      return (Integer) this.properties.get("refreshTime");
    } catch (NullPointerException ex) {
      logger.info("Can not get 'refreshTime' property. Use default value: " + InboxService.DEFAULT_FILE_REFRESH_TIME_SEC);
      return InboxService.DEFAULT_FILE_REFRESH_TIME_SEC;
    }
  }
  
  /**
   * Get directry refresh time (in sec) from properties. If properties does not contain 'dirRefreshTime' key, then return DEFAULT_DIR_REFRESH_TIME
   * @return file refresh time 
   */
  protected Integer getDirRefreshTime() {
    try {
      return (Integer) this.properties.get("dirRefreshTime");
    } catch (NullPointerException ex) {
      logger.info("Can not get 'refreshTime' property. Use default value: " + InboxService.DEFAULT_DIR_REFRESH_TIME_SEC);
      return InboxService.DEFAULT_DIR_REFRESH_TIME_SEC;
    }
  }
  
  /**
   * Thread run method. 
   * Creates and starts FileWatchingThreads for each new founded file in the inbox.
   * {@inheritDoc}
   * @see java.lang.Thread#run()
   */
  public void run() {
    
    try {
      File inboxDir = new File(getInboxPath());
      while (!isInterrupted()) {
        File[] files = inboxDir.listFiles();
        for (File file : files) {
          String fileName = file.getName();
          synchronized (this.watchingThreads) {
            if (!this.watchingThreads.containsKey(fileName)) {
              FileWatchingThread watchingThread = new FileWatchingThread(file, this);
              watchingThread.start();
              this.watchingThreads.put(fileName, watchingThread);
              logger.debug("WatchingTread for the file '"+fileName+"' started.");
            }
          }
        }
        sleep(getDirRefreshTime() * 1000);
      }
    } catch (Exception ex) {
      if (ex instanceof NullPointerException) {
        // inbox directory does not exist or path is null!
        logger.debug("Inbox directory does not exist or path is null!", ex);
      } else 
      if (ex instanceof SecurityException) {
        // no read permission for the inbox directory!
        logger.debug("Have not permission reading files.", ex);
      } else 
      if (ex instanceof InterruptedException) {
        logger.debug("Was interrupted!");
      } else 
      logger.error("Error", ex);
    }
  }
  
  /**
   * Thread interrupt method. 
   * Interrupt all FileWatchingThreads and then interrupt himself. 
   * {@inheritDoc}
   * @see java.lang.Thread#interrupt()
   */
  public void interrupt() {
    // kill all watching FileThreads
    super.interrupt();
    this.terminateFileThreads();
  }
  
  /**
   * Terminate all FileWatchingThreads. Do not call this method, it will be called by interrupt!
   */
  private void terminateFileThreads() {
    synchronized (watchingThreads) {
      if (this.watchingThreads != null) {
        for (FileWatchingThread t : this.watchingThreads.values()) {
          try {
            t.interrupt();
          } catch (SecurityException ex) {
            logger.error("Can not interrupt Thread!", ex);
          }
        }
      } 
    }
    logger.debug("Was interrupted");
  }
  
  /**
   * Remove FileWatchingThread from Collection. Do not call this method, it will be called by FileWatchingThread!
   * @param key filename
   */
  public void removeFileThread(String key) {  
    // clean up collection 
    synchronized (this.watchingThreads) {
      try {
        this.watchingThreads.remove(key);
      } catch (NullPointerException ex) {
        logger.debug("key is null");
      }
    }
  }
}
