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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLEncoder;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */
public class FileWatchingThread extends Thread {

  /**
   * Matterhorn Logger
   */
  protected static final Logger logger = LoggerFactory.getLogger(FileWatchingThread.class);

  /**
   * InboxWatcher, will need to get WorkingFileRepository and remove himself from InboxWatcher Collection before terminate 
   */
  protected  InboxWatcher inboxWatcher;
  /**
   * Refresh time
   */
  protected int refreshTime;
  /**
   * File to watch for
   */
  protected File file;
  /**
   * FileHash for comparing state
   */
  protected long fileHash;
  
  /**
   * Constructor.
   * @param FileName filename to watching for
   * @param inboxWatcher inboxWatcher
   */
  public FileWatchingThread(String FileName, InboxWatcher inboxWatcher) {
    this(new File(FileName), inboxWatcher);
  }
  
  /**
   * Constructor.
   * @param file file to watching for
   * @param inboxWatcher inboxWatcher
   */
  public FileWatchingThread(File file, InboxWatcher inboxWatcher) {
    if (!file.exists()) {
      logger.error("Can not watching fileupload because file not exist! " + file.getName());
      return;
    }
    
    this.file = file;
    this.inboxWatcher = inboxWatcher;
    this.refreshTime = inboxWatcher.getFileRefreshTime() > 0 ? inboxWatcher.getFileRefreshTime() * 1000 : 30000;
  }
  
  /**
   * Return file
   * @return file
   */
  public File getFile() {
    return this.file;
  }
  
  /**
   * Return filename
   * @return filename
   */
  public String getFileName() {
    return this.file.getName();
  }
  
  /**
   * Return file path
   * @return file path
   */
  public String getFilePath() {
    return this.file.getAbsolutePath();
  }
  
  /**
   * Return watching file size in byte
   * @return file size in byte
   */
  public long getFileSize() {
    return this.file.length();
  }
  
  /**
   * Return lastModified from file
   * @return lastModified
   */
  public long getFileLastModify() {
    return this.file.lastModified();
  }
  
  /**
   * Return hash code from filename + file size + lastModified 
   * @return hash code
   */
  public long getFileHash() {
    return this.getFileName().hashCode() +
           this.getFileSize() + 
           this.getFileLastModify();
  }
  
  /**
   * Thread run method.
   * Waiting for file be uploaded and push it into the WorkingFileRepository!
   */
  public void run() {
    
    try {
      logger.debug("Start watching file modified. Filename: " + getFileName());
      boolean done = false;
      while (!done && !isInterrupted()) {
        this.fileHash = getFileHash();
        sleep(this.refreshTime);
        if (this.fileHash == getFileHash()) {
          WorkingFileRepository wfr = this.inboxWatcher.getWorkingFileRepository();
          if (wfr == null) {
            logger.error("WorkingFileRepository Service is not running! Can not put the file (" + 
                    getFileName() + ") into Collection!");
            this.interrupt();
            break;
          }
          // set file writeprotected and upload to the WorkingFileRepository
          file.setWritable(false);
          FileInputStream is = new FileInputStream(file);
          //URI fileUri = 
          wfr.putInCollection(InboxWatcher.INBOX_WFR_COLLECTION_ID, URLEncoder.encode(getFileName(), "UTF-8"), is);
          //wfr.putInCollection(InboxWatcher.INBOX_WFR_COLLECTION_ID, getFileName(), is);
          // TODO check file upload
          logger.info("File '"+getFileName()+"' was uploaded to the WorkingFileRepository. Deleting file from inbox.");
          file.delete();
          done = true;
        }
      }
    } catch (Exception ex) {
      if (ex instanceof InterruptedException) {
        logger.debug("Was interrupted!");
      } else 
      if (ex instanceof SecurityException) {
        logger.error("Have not permission to read the file " + getFileName());
      } else
      if (ex instanceof FileNotFoundException) {
        logger.error("File to watching of does not exist!");
      } else 
      {
        logger.error("Error", ex);
      }
    } finally {
      this.inboxWatcher.removeFileThread(getFileName());
    }
  }
  
  /**
   * Thread interrupt method. 
   * Remove itself from InboxWatcher Collection.
   */
  public void interrupt() {
    if (this.inboxWatcher != null) {
      try {
        this.inboxWatcher.removeFileThread(this.getFileName());
      } catch (Exception ex) {
        logger.debug("Can not remove me from InboxWatcher Collection!", ex);
      }
    }
    super.interrupt();
  }
}
