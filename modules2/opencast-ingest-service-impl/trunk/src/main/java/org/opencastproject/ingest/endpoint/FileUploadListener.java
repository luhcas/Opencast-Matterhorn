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
package org.opencastproject.ingest.endpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.commons.fileupload.ProgressListener;

/**
 * This is a File Upload Listener that is used by Apache
 * Commons File Upload to monitor the progress of the
 * uploaded file.
 */
public class FileUploadListener implements ProgressListener {
  private Connection con;
  private String mediaPackageID = null;
  private String filename = null;
  boolean entryExists = false;

  public FileUploadListener() {
    super();
  }

  @Override
  public void update(long received, long total, int item) {
    if (entryExists) {
      try {
        PreparedStatement s = con.prepareStatement("UPDATE UPLOADPROGRESS SET total = ?, received = ? WHERE mediapackageId = ? AND filename = ?");
        s.setLong(1, received);
        s.setLong(2, total);
        s.setString(3, mediaPackageID);
        s.setString(4, filename);
        s.execute();
        if (received == total) {   // close sql conn if we don't need to write data anymore
          con.close();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public void setConnection(Connection con) {
    this.con = con;
  }

  public void setMediaPackageID(String id) {
    mediaPackageID = id;
    entryExists = createEntry();
  }

  public void setFilename(String filename) {
    this.filename = filename;
    entryExists = createEntry();
  }

  private boolean createEntry() {
    if ((mediaPackageID != null) && (filename != null)) {
      PreparedStatement s;
      try {
        s = con.prepareStatement("INSERT INTO UPLOADPROGRESS (mediapackageId, filename, total, received) VALUES (?, ?, ?, ?)");
        s.setString(1, mediaPackageID);
        s.setString(2, filename);
        s.setLong(3, 0L);
        s.setLong(4, 0L);
        s.execute();
        return true;
      } catch (SQLException ex) {
        ex.printStackTrace();
        return false;
      }
    } else {
      return false;
    }
  }
}

