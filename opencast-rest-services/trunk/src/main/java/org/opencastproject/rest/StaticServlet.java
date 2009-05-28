/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.rest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides access to static content in the directory specified by the "matterhorn.static.path"
 * (system or osgi bundle context) property
 */
public class StaticServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(StaticServlet.class);
  private String fsRoot;
  public StaticServlet(String fsRoot) {
    this.fsRoot = fsRoot;
  }
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    String relativePath = request.getPathInfo();
    if(relativePath.contains("..")) {
      throw new SecurityException("you can not access .. directories");
    }
    String fsPath = fsRoot + "/" + relativePath;
    File f = new File(fsPath);
    String mimeType = new MimetypesFileTypeMap().getContentType(f);
    logger.debug("Returning " + fsPath + ", a file of type " + mimeType);
    response.setContentType(mimeType);
    FileInputStream in = null;
    if(f.exists() && f.canRead()) {
      try {
        in = new FileInputStream(f);
        IOUtils.copy(in, response.getOutputStream());
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
  }
}
