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

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow operation used to inspect all tracks of a media package.
 */
public class InspectWorkflowOperationHandler implements
        WorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  /** The inspection service */
  private MediaInspectionService inspectionService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param inspectionService
   *          the inspection service
   */
  protected void setInspectionService(MediaInspectionService inspectionService) {
    this.inspectionService = inspectionService;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult run(WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = MediaPackageUtil.clone(workflowInstance.getCurrentMediaPackage());
    for (Track track : mediaPackage.getTracks()) {
      logger.info("Inspecting track '{}' of {}", track.getIdentifier(), mediaPackage);
      Track inspectedTrack = inspectionService.enrich(track, false);
      try {
        mediaPackage.remove(track);
        mediaPackage.add(inspectedTrack);
      } catch (UnsupportedElementException e) {
        logger.error("Error adding {} to media package", inspectedTrack, e);
      }
    }
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediaPackage, null, false);

  }

}
