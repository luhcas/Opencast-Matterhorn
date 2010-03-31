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
package org.opencastproject.workflow.api;


import org.opencastproject.http.SecureContext;
import org.opencastproject.http.StaticResource;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Abstract base implementation for an operation handler, which implements a simple resume operation that returns a
 * {@link WorkflowOperationResult} with the current mediapackage and {@link Action#CONTINUE}.
 */
public abstract class AbstractWorkflowOperationHandler implements ResumableWorkflowOperationHandler {

  protected StaticResource resource;
  protected BundleContext bundleContext;
  protected URL holdStateUserInterfaceUrl;
  protected ServiceTracker httpTracker;
  
  public void activate(ComponentContext cc) {
    bundleContext = cc.getBundleContext();
  }
  
  public void deactivate() {
    if(httpTracker != null) httpTracker.close();
  }
  
  public URL getHoldStateUserInterfaceURL() {
    return holdStateUserInterfaceUrl;
  }

  public URL registerHoldStateUserInterface(final String resourcePath) {
    final String alias = "/" + getClass().getName().toLowerCase();
    if (resourcePath == null)
      throw new IllegalArgumentException("Classpath must not be null");
    final String path = FilenameUtils.getPathNoEndSeparator(resourcePath);
    final String fileName = FilenameUtils.getName(resourcePath);

    httpTracker = new ServiceTracker(bundleContext, HttpService.class.getName(), null) {
      public Object addingService(ServiceReference reference) {
        HttpService httpService = (HttpService)bundleContext.getService(reference);
        HttpContext defaultHttpContext = httpService.createDefaultHttpContext();
        try {
          httpService.registerResources(alias, path, new SecureContext(defaultHttpContext, bundleContext));
        } catch (NamespaceException e) {
          throw new IllegalStateException(alias + " has already been registered.  Choose another alias.", e);
        }
        return super.addingService(reference);
      }
      public void removedService(ServiceReference reference, Object service) {
        super.removedService(reference, service);
        HttpService httpService = (HttpService)service;
        httpService.unregister(alias);
        holdStateUserInterfaceUrl = null;
      }
    };
    httpTracker.open();

    String url = UrlSupport.concat(new String[] { bundleContext.getProperty("org.opencastproject.server.url") , alias, fileName }) + "?{id}";
    try {
      holdStateUserInterfaceUrl = new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalStateException(url + " is not a valid URL");
    }
    return holdStateUserInterfaceUrl;
  }
  
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
  }

/**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#destroy(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void destroy(WorkflowInstance workflowInstance) throws WorkflowOperationException {}

}
