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
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageReference;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    try {
      MediaPackage mp = MediaPackageUtil.clone(workflowInstance.getCurrentMediaPackage());
      logger.info("Publishing media package {} to search index", mp);

      // Check which tags have been configured
      String tags = workflowInstance.getCurrentOperation().getConfiguration("tags");
      if (StringUtils.trimToNull(tags) == null) {
        logger.warn("No tags have been specified");
        return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mp, null, false);
      }

      // Look for elements matching any tag
      Set<MediaPackageElement> keep = new HashSet<MediaPackageElement>();
      for (String tag : tags.split("\\W")) {
        if(StringUtils.trimToNull(tag) == null) continue;
        keep.addAll(Arrays.asList(mp.getTracksByTag(tag)));
        keep.addAll(Arrays.asList(mp.getAttachmentsByTag(tag)));
        keep.addAll(Arrays.asList(mp.getCatalogs()));
      }

      // Remove what we don't want to publish (i. e. what is not tagged accordingly)
      for(MediaPackageElement element : mp.getElements()) {
        if( ! keep.contains(element)) {
          mp.remove(element);
        }
      }

      // Fix references
      for(MediaPackageElement element : mp.getElements()) {
        MediaPackageReference reference = element.getReference();
        if (reference != null && mp.getElementByReference(reference) == null) {
          element.clearReference();
        }
      }
      // adding media package to the search index
      searchService.add(mp);
      logger.debug("Publish operation complete");
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mp, null, false);
    } catch (Throwable t) {
      throw new WorkflowOperationException(t);
    }
  }
}
