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

import java.util.Dictionary;
import java.util.Hashtable;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InboxService implements ManagedService {
  
  /**
   * Default inbox path
   */
  public static final String DEFAULT_INBOX_PATH = "/tmp/inbox";
  /**
   * Default file refresh time in sec
   */
  public static final Integer DEFAULT_FILE_REFRESH_TIME_SEC = 30;
  
  /**
   * Matterhorn Logger
   */
  protected static final Logger logger = LoggerFactory.getLogger(InboxService.class);
  /**
   * Matterhorn Working FileRepository for pushing uploaded files into
   */
  protected WorkingFileRepository wfrService;
  /**
   * Properties dictinary
   */
  protected Dictionary properties;
  /**
   * Thread for looking up of new files
   */
  protected InboxWatcher watcher;
  
  /**
   * Constructor (empty)
   */
  public InboxService() {
    logger.info("CONSTRUCT");
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  public void updated(Dictionary properties) throws ConfigurationException {
    
//    this.properties = properties;
////    if (this.watcher != null) {
////      this.watcher.updateProperties(properties);
////    }
//    logger.info("UPDATED");
  }
  
  /**
   * Method will be called at start of the InboxService. 
   * Gets required properties and start InboxWatcher Thread.
   * @param componentContext Context
   */
  public void activate(ComponentContext componentContext) {
    logger.info("ACTIVATE");
    
    // read properties
    if (componentContext == null) {
      logger.info("Starting Service with default configuration;");
      this.properties = getDefautltProperties();
    } else {
      this.properties = new Hashtable();
      try {
        String inboxPath = componentContext.getBundleContext().getProperty("org.opencastproject.inbox.inboxPath");
        String refreshTimeStr = componentContext.getBundleContext().getProperty("org.opencastproject.inbox.refreshTime");
        Integer refreshTime = InboxService.DEFAULT_FILE_REFRESH_TIME_SEC;
        try {
          refreshTime = Integer.parseInt(refreshTimeStr);
        } catch (NumberFormatException ex) {
          logger.info("Can not parse 'inbox.refreshTime' property. Setting default value: " + InboxService.DEFAULT_FILE_REFRESH_TIME_SEC);
        }
        
        // use matterhorn workdir if no inbox was defined
        if (inboxPath == null || inboxPath.length() == 0) {
          inboxPath = componentContext.getBundleContext().getProperty("org.opencastproject.storage.dir")+"/inbox";
        }
        
        this.properties.put("inboxPath", inboxPath);
        this.properties.put("refreshTime", Integer.parseInt(refreshTimeStr));
      } catch (Exception ex) {
        if (ex instanceof SecurityException) {
          logger.error("failed to read properties ('inbox.inboxPath' and 'inbox.refreshTime'), maybe do not have read permissions!");
          this.properties = getDefautltProperties();
        }
      }
    }
    // kill running
    if (this.watcher != null) {
      stopWatcher(this.watcher);
    }
    
    // start watcher 
    this.watcher = this.createWatcher(this.properties, this.wfrService);
    if (this.watcher != null) {
      this.watcher.start();
      logger.info("ACTIVE");
    } else {
      logger.error("Can not create InboxWatcher!");
    }
  }

  /**
   * Method will be called at end of the InboxService. 
   * @param componentContext Context
   */
  public void deactivate(ComponentContext componentContext) {

    logger.info("DEACTIVATE");
    stopWatcher(this.watcher);
  }

  /**
   * Return Dictionary with default values (static values)
   * @return dictionary with default property values
   */
  private Dictionary getDefautltProperties() {
    Dictionary properties = new Hashtable();
    properties.put("inboxPath", InboxService.DEFAULT_INBOX_PATH);
    properties.put("refreshTime", InboxService.DEFAULT_FILE_REFRESH_TIME_SEC);
    return properties;
  }
  
  /**
   * Set WorkingFileRepository Service
   * @param workingFileRepository WorkingFileRepository Service
   */
  public void setWorkingFileRepositoryService(WorkingFileRepository workingFileRepository) {
    this.wfrService = workingFileRepository;
    if (this.watcher != null) {
      this.watcher.setWorkingFileRepositoryService(workingFileRepository);
    }
  }
  
  /**
   * Unset WorkingFileRepository Service
   * @param workingFileRepository WorkingFileRepository
   */
  public void unsetWorkingFileRepositoryService(WorkingFileRepository workingFileRepository) {
    this.wfrService = null;
    if (this.watcher != null) {
      this.watcher.unsetWorkingFileRepositoryService(workingFileRepository);
    }
  }
  
  /**
   * Create InboxWatcher (Thread) with the values from properties dictionary and the given WorkingFileRepository Service
   * @param properties Properties
   * @param wfrService WorkingFileRepositoryService
   * @return InboxWatcher Thread
   */
  protected InboxWatcher createWatcher(Dictionary properties, WorkingFileRepository wfrService) {
    try {
      InboxWatcher watcher = new InboxWatcher(properties, wfrService);
      return watcher;
    } catch (IllegalStateException ex) {
      logger.error("Can not start directory watching!");
      return null;
    }
  }
  
  /**
   * Interrupt the InboxWatcher Thread
   * @param watcher InboxWatcher Thread
   */
  protected void stopWatcher(InboxWatcher watcher) {
    if (watcher != null) {
      try {
          watcher.interrupt();
      } catch (Exception ex) { }
    }
  }
}
