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
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * The workflow definition for handling "distribute" operations
 */
public class DistributeWorkflowOperationHandler implements WorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(DistributeWorkflowOperationHandler.class);

  private DistributionService distributionService;

  protected void setDistributionService(DistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult run(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.info("run() distribution workflow operation");
    // if(workflowInstance.getProperties() != null && workflowInstance.getProperties().keySet().contains("itunes")) {
    // logger.info("Distributing media to itunes for media package " +
    // workflowInstance.getSourceMediaPackage().getIdentifier());
    // }
    // if(workflowInstance.getProperties() != null && workflowInstance.getProperties().keySet().contains("youtube")) {
    // logger.info("Distributing media to youTube for media package " +
    // workflowInstance.getSourceMediaPackage().getIdentifier());
    // }
    // if(workflowInstance.getProperties() == null || workflowInstance.getProperties().isEmpty()) {
    // logger.info("This workflow contains no properties, so we can't distribute any media");
    // }
    
    // TODO: Determine which distribution channels should be called

//    if (workflowInstance.getConfiguration("local") != null) {
      try {
        logger.info("Distributing to the local repository");
        Set<String> elementIds = new HashSet<String>();
        MediaPackage mp = workflowInstance.getCurrentMediaPackage();
        for(Track track : mp.getTracks()) {
          elementIds.add(track.getIdentifier());
        }
        for(Catalog cat : mp.getCatalogs()) {
          elementIds.add(cat.getIdentifier());
        }
        for(Attachment a : mp.getAttachments()) {
          elementIds.add(a.getIdentifier());
        }
        distributionService.distribute(mp, elementIds.toArray(new String[elementIds.size()]));
      } catch (RuntimeException e) {
        throw new WorkflowOperationException(e);
      }
//    } else {
//      logger.info("Distribution to local repository skipped");
//    }

    // TODO Add any distributed media to the media package
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(workflowInstance.getCurrentMediaPackage(), null, false);
  }
}
