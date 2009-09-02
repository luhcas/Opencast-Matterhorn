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
package org.opencastproject.util;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A static resource for registration with the http service.
 */
public class StaticResource {
  private static final Logger logger = LoggerFactory.getLogger(StaticResource.class);
  String classpath;
  String alias;
  HttpService httpService;

  public void setHttpService(HttpService service) {
    this.httpService = service;
  }
  
  public void activate(ComponentContext context) {
    alias = (String)context.getProperties().get("alias");
    classpath = (String)context.getProperties().get("classpath");
    HttpContext httpContext = httpService.createDefaultHttpContext();
    logger.info("registering " + this);
    try {
      httpService.registerResources(alias, classpath, httpContext);
    } catch (NamespaceException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "StaticResource [alias=" + alias + ", classpath=" + classpath + "]";
  }
}
