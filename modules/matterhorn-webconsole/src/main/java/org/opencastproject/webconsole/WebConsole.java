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

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.BrandingPlugin;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 */
public class WebConsole extends OsgiManager {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(WebConsole.class);
  
  public WebConsole(BundleContext bundleContext) {
    super(bundleContext);
    AbstractWebConsolePlugin.setBrandingPlugin(new MatterhornBrandingPlugin());
    logger.info("The matterhorn web console will use the {} branding plugin", AbstractWebConsolePlugin.getBrandingPlugin());
  }

  public void service( ServletRequest req, ServletResponse res ) throws ServletException, IOException {
    super.service(req, res);
  }

  /** Override the http service binding methods, since declarative services will handle binding and unbinding for us */
  protected synchronized void bindHttpService(HttpService httpService) {}
  protected synchronized void unbindHttpService(HttpService httpService) {}

  static class MatterhornBrandingPlugin implements BrandingPlugin {
    public String getBrandName() {
      return "Opencast Matterhorn";
    }
    public String getFavIcon() {
      return "/res/imgs/favicon.ico";
    }
    public String getMainStyleSheet() {
      return "/res/ui/webconsole.css";
    }
    public String getProductImage() {
      return "/res/imgs/logo.png";
    }
    public String getProductName() {
      return "Matterhorn";
    }
    public String getProductURL() {
      return "http://www.opencastproject.org/matterhorn";
    }
    public String getVendorImage() {
      return "/res/imgs/logo.png";
    }
    public String getVendorName() {
      return "The Opencast Project";
    }
    public String getVendorURL() {
      return "http://www.opencastproject.org";
    }
  }
}
