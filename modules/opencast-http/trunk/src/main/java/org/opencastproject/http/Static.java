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
package org.opencastproject.http;

import org.apache.commons.io.FileUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Registers static content resources on the filesystem with the http service.
 */
public class Static implements ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(Static.class);
  String defaultPath;
  protected HttpService httpService;
  StaticContext context;

  public Static() {
    defaultPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "static";
    logger.info("Registering resources at " + defaultPath + " at URL /static");
    context = new StaticContext(defaultPath);
  }
  
  public void setHttpService(HttpService httpService) {
    this.httpService = httpService;
    try {
      httpService.registerResources("/static", "/", context);
    } catch (NamespaceException e) {
      throw new RuntimeException(e);
    }
  }

  public void unsetHttpService(HttpService httpService) {
    httpService.unregister("/static");
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    if(properties.get("baseResourcePath") != null) {
      context.setBaseResourcePath((String) properties.get("baseResourcePath"));
    }
  }

  class StaticContext implements HttpContext {
    private String baseResourcePath;

    public StaticContext(String baseResourcePath) {
      setBaseResourcePath(baseResourcePath);
    }

    public void setBaseResourcePath(String baseResourcePath) {
      this.baseResourcePath = baseResourcePath;
      File f = new File(baseResourcePath);
      if(f.exists() && f.isFile()) {
        throw new IllegalArgumentException("baseResourcePath must be a directory");
      }
      if( ! f.exists()) {
        try {
          FileUtils.forceMkdir(f);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    public String getMimeType(String name) {
      return null;
    }

    public URL getResource(String name) {
      File f = new File(baseResourcePath, name);
      if( ! f.isFile()) {
        return null;
      }
      try {
        return f.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
      return true;
    }
  }
}
