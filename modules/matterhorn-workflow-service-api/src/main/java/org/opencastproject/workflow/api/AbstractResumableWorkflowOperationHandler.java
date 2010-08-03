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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import java.net.URL;

/**
 * Abstract base implementation for a resumable operation handler, which implements a simple operations for starting an
 * operation, returning a {@link WorkflowOperationResult} with the current mediapackage and {@link Action#PAUSE} and
 * resuming an operation, returning a {@link WorkflowOperationResult} with the current mediapackage and
 * {@link Action#CONTINUE}.
 */
public abstract class AbstractResumableWorkflowOperationHandler extends AbstractWorkflowOperationHandler implements
        ResumableWorkflowOperationHandler {
  protected ComponentContext componentContext;
  protected ServiceRegistration staticResourceRegistration;
  protected StaticResource staticResource;
  protected String holdActionTitle;
  
  private static final String DEFAULT_TITLE = "Action"; // TODO maybe there's a better default action title?

  /** Name of the configuration option that determines whether this operation is run at all */
  protected static final String REQUIRED_PROPERTY = "required-property";

  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    super.activate(componentContext);
  }
  
  public void deactivate() {
    if(staticResourceRegistration != null) staticResourceRegistration.unregister();
  }

  public URL getHoldStateUserInterfaceURL(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    if (staticResource == null)
      return null;
    return staticResource.getDefaultUrl();
  }

  protected void setHoldActionTitle(String title) {
    this.holdActionTitle = title;
  }

  public String getHoldActionTitle() {
    if (holdActionTitle == null) {
      return DEFAULT_TITLE;
    } else {
      return holdActionTitle;
    }
  }

  protected URL registerHoldStateUserInterface(final String resourcePath) {
    String alias = "/workflow/hold/" + getClass().getName().toLowerCase();
    if (resourcePath == null)
      throw new IllegalArgumentException("Classpath must not be null");
    String path = FilenameUtils.getPathNoEndSeparator(resourcePath);
    String welcomeFile = FilenameUtils.getName(resourcePath);
    staticResource = new StaticResource(componentContext.getBundleContext(), path, alias, welcomeFile);
    staticResourceRegistration = componentContext.getBundleContext().registerService(StaticResource.class.getName(),
            staticResource, null);
    return staticResource.getDefaultUrl();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.PAUSE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
  }

}
