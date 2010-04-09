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

import org.opencastproject.http.StaticResource;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import java.net.URL;

/**
 * Abstract base implementation for a resumable operation handler, which implements a simple operations for starting
 * an operation, returning a {@link WorkflowOperationResult} with the current mediapackage and {@link Action#PAUSE} and
 * resuming an operation, returning a {@link WorkflowOperationResult} with the current mediapackage and
 * {@link Action#CONTINUE}.
 */
public abstract class AbstractResumableWorkflowOperationHandler extends AbstractWorkflowOperationHandler
  implements ResumableWorkflowOperationHandler {
  protected ComponentContext componentContext;
  protected StaticResource resource;
  protected HttpService httpService;
  protected HttpContext httpContext;
  protected StaticResource staticResource;

  
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
  }

  public URL getHoldStateUserInterfaceURL(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    if(staticResource == null) return null;
    return staticResource.getDefaultUrl();
  }

  protected URL registerHoldStateUserInterface(final String resourcePath) {
    if(httpService == null || httpContext == null) {
      throw new IllegalStateException("HttpService and HttpContext must be set before registering a hold state UI");
    }
    String alias = "/workflow/hold/" + getClass().getName().toLowerCase();
    if (resourcePath == null)
      throw new IllegalArgumentException("Classpath must not be null");
    String path = FilenameUtils.getPathNoEndSeparator(resourcePath);
    String welcomeFile = FilenameUtils.getName(resourcePath);
    staticResource = new StaticResource(path, alias, welcomeFile, httpService, httpContext);
    staticResource.activate(componentContext);
    return staticResource.getDefaultUrl();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.PAUSE);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
  }

}
