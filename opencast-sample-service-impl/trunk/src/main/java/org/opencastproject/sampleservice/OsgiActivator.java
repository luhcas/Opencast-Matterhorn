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
package org.opencastproject.sampleservice;

import org.ops4j.pax.web.extender.whiteboard.Resources;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;

public class OsgiActivator implements BundleActivator {

  /**
   * Registers static web resources at /samplehtml and /samplejs.
   */
  public void start(BundleContext context) throws Exception {
    // Register the static web resources.  This handles the http service tracking automatically
    Dictionary<String, String> staticJsProps = new Hashtable<String, String>();
    staticJsProps.put("alias", "/samplejs");
    context.registerService(
        Resources.class.getName(), new Resources("/js"), staticJsProps);
    Dictionary<String, String> staticHtmlProps = new Hashtable<String, String>();
    staticHtmlProps.put("alias", "/samplehtml");
    context.registerService(
        Resources.class.getName(), new Resources("/html"), staticHtmlProps);
    
  }

  public void stop(BundleContext context) throws Exception {
  }
}
