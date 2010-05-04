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
package org.opencastproject.webconsole;

import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

/**
 *
 */
public class WebConsole extends OsgiManager {
  private static final long serialVersionUID = 1L;
  
  public WebConsole(BundleContext bundleContext) {
    super(bundleContext);
  }
  
  /** Override the http service binding methods, since declarative services will handle binding and unbinding for us */
  protected synchronized void bindHttpService(HttpService httpService) {}
  protected synchronized void unbindHttpService(HttpService httpService) {}

}
