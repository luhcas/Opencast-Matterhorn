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
package org.opencastproject.conductor.impl;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * The workflow definition for handling "distribute" operations
 */
public class DistributeWorkflowOperationHandler implements WorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DistributeWorkflowOperationHandler.class);

  /** The distribution service */
  private DistributionService distributionService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param distributionService
   *          the distribution service
   */
  protected void setDistributionService(DistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult run(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running distribution workflow operation");

    MediaPackage resultingMediaPackage = null;
    try {

      // Check which tags have been configured
      String sourceTags = workflowInstance.getCurrentOperation().getConfiguration("source-tags");
      String targetTags = workflowInstance.getCurrentOperation().getConfiguration("target-tags");
      MediaPackage currentMediaPackage = workflowInstance.getCurrentMediaPackage();
      if (StringUtils.trimToNull(sourceTags) == null) {
        logger.warn("No tags have been specified");
        return WorkflowBuilder.getInstance().buildWorkflowOperationResult(currentMediaPackage, null, false);
      }

      // Send a mediapackage clone to the distribution service so we don't manipulate mediapackages associated with previous workflow operations
      MediaPackage clone = (MediaPackage)currentMediaPackage.clone();
      
      // Look for elements matching any tag
      Set<String> elementIds = new HashSet<String>();
      for (String tag : sourceTags.split("\\W")) {
        if(StringUtils.trimToNull(tag) == null) continue;
        MediaPackageElement[] elts = currentMediaPackage.getElementsByTag(tag);
        for (MediaPackageElement e : elts) {
          if (elementIds.add(e.getIdentifier())) {
            logger.info("Distributing '{}'to the local repository", e.getIdentifier(), currentMediaPackage);
          }
        }
      }

      // Also distribute all of the metadata catalogs
      for(Catalog c : clone.getCatalogs()) elementIds.add(c.getIdentifier());
      
      resultingMediaPackage = distributionService.distribute(clone, elementIds.toArray(new String[elementIds.size()]));
      
      // Tag the distributed elements
      String[] tags = targetTags.split("\\W");
      for (MediaPackageElement element : resultingMediaPackage.getElements()) {
        if (currentMediaPackage.getElementById(element.getIdentifier()) == null) {
          for (String tag : tags) {
            if(StringUtils.trimToNull(tag) == null) continue;
            element.addTag(tag);
          }
        }
      }
 
      logger.debug("Distribute operation completed");
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw new WorkflowOperationException(e);
    }
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, null, false);
  }

}
