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
package org.opencastproject.fsresources;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves static content from a configured path on the filesystem.  In production systems, this should be replaced with
 * apache httpd or another web server optimized for serving static content.
 */
public class StaticResourceServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(StaticResourceServlet.class);

  protected HttpContext httpContext;
  protected HttpService httpService;
  protected String distributionDirectory;

  public StaticResourceServlet() {
  }

  public void setHttpContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }

  public void setHttpService(HttpService httpService) {
    this.httpService = httpService;
  }

  public void activate(ComponentContext cc) {
    if (cc != null) {
      String ccDistributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
      logger.info("serving static files from '{}'", ccDistributionDirectory);
      if (ccDistributionDirectory != null) {
        this.distributionDirectory = ccDistributionDirectory;
      }
    }

    if (distributionDirectory == null)
      distributionDirectory = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator
              + "static";

    try {
      httpService.registerServlet("/static", this, null, httpContext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void deactivate() {
    httpService.unregister("/static");
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    logger.debug("Looking for static resource '{}'", req.getRequestURI());
    String path = req.getPathInfo();
    String normalized = path == null ? null : path.trim().replaceAll("/+", "/").replaceAll("\\.\\.", "");
    if (normalized != null && normalized.startsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(1);
    }

    File f = new File(distributionDirectory, normalized);
    if (f.isFile() && f.canRead()) {
      logger.debug("Serving static resource '{}'", f.getAbsolutePath());
      FileInputStream in = new FileInputStream(f);
      try {
        IOUtils.copyLarge(in, resp.getOutputStream());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(in);
      }
    } else {
      logger.debug("unable to find file '{}', returning HTTP 404");
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

}
