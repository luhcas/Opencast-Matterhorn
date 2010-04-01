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
package org.opencastproject.http;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Registers static content resources on the filesystem with the http service.  Follows the security scheme defined in
 * {@link SecureHttpContext}.
 */
public class StaticHttpContext {
  private static final Logger logger = LoggerFactory.getLogger(StaticHttpContext.class);
  String filesystemPath;
  protected HttpService httpService;
  StaticContext context;

  public StaticHttpContext() {
    filesystemPath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "static";
    logger.info("Registering resources at {} at URL /static", filesystemPath);
  }
  
  public void setHttpService(HttpService httpService) {
    this.httpService = httpService;
  }
  
  public void activate(ComponentContext cc) {
    try {
      httpService.registerResources("/static", "/", new StaticContext(filesystemPath, httpService.createDefaultHttpContext(), cc.getBundleContext()));
    } catch (NamespaceException e) {
      throw new RuntimeException(e);
    }
  }
  
  public void deactivate() {
    httpService.unregister("/static");
  }

  class StaticContext extends SecureHttpContext {
    private String baseResourcePath;
    public StaticContext(String baseResourcePath, HttpContext delegate, BundleContext bundleContext) {
      super(delegate, bundleContext);
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
  }
}
