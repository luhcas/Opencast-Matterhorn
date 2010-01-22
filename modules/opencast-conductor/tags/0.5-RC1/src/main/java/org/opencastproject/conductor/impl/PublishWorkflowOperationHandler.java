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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow operation for handling "publish" operations
 */
public class PublishWorkflowOperationHandler implements WorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(PublishWorkflowOperationHandler.class);

  private SearchService searchService;

  protected void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult run(WorkflowInstance workflowInstance) throws WorkflowOperationException {

    logger.debug("Running publish workflow operation");

    MediaPackage mp = workflowInstance.getCurrentMediaPackage();
    logger.info("Publishing media package {} to search index", mp);

    try {
      // adding media package to the search index
      searchService.add(mp);
    } catch (SearchException e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Running publish operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mp, null, false);
  }
}
