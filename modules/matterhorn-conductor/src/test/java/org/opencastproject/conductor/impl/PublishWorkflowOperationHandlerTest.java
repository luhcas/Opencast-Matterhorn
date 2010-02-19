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
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PublishWorkflowOperationHandlerTest {
  @Test
  public void testPublishOperation() throws Exception {
    // Set up a mediapackage to publish
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    Track presentationTrack = (Track)elementBuilder.elementFromURI(new URI("http://foo.bar"), Track.TYPE, MediaPackageElements.PRESENTATION_TRACK);
    Track presenterTrack = (Track)elementBuilder.elementFromURI(new URI("http://foo.bar"), Track.TYPE, MediaPackageElements.PRESENTER_TRACK);
    mp.add(presentationTrack);
    mp.add(presenterTrack);
    
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId("workflow-1");
    workflowInstance.setState(State.RUNNING.toString());
    workflowInstance.setSourceMediaPackage(mp);
    WorkflowOperationInstanceImpl operationInstance = new WorkflowOperationInstanceImpl();
    operationInstance.setState(State.RUNNING.toString());
    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operationInstance);
    workflowInstance.setWorkflowOperationInstanceList(operationsList);
    
    // Set up the operation handler using mock collaborators
    PublishWorkflowOperationHandler operationHandler = new PublishWorkflowOperationHandler();
    SearchService searchService = EasyMock.createNiceMock(SearchService.class);
    EasyMock.replay(searchService);
    operationHandler.setSearchService(searchService);
    
    // Run the media package through the operation handler, ensuring that the flavors are retained
    WorkflowOperationResult result = operationHandler.run(workflowInstance);
    for(Track t : result.getResultingMediaPackage().getTracks()) {
      Assert.assertNotNull(t.getFlavor());
    }
  }
}
