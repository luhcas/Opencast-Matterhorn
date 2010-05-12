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
package org.opencastproject.workflow.handler;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class DistributeWorkflowOperationHandlerTest {
  private DistributeWorkflowOperationHandler operationHandler;
  private DistributionService service = null;

  private URI uriMP;
  private MediaPackage mp;
  private MediaPackage mpAfter;
  private Track t;

  @Before
  public void setup() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    uriMP = InspectWorkflowOperationHandler.class.getResource("/distribute_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    mpAfter = (MediaPackage) mp.clone();
    t = (Track) mp.getTracks()[0].clone();
    t.setIdentifier("Test-newtrack");
    mpAfter.add(t);

    // set up service
    operationHandler = new DistributeWorkflowOperationHandler();

  }

  @Test
  public void testDistributeOperation() throws Exception {
    // set up mock distribution
    service = EasyMock.createStrictMock(DistributionService.class);
    EasyMock.expect(
            service.distribute((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock
                    .anyObject())).andReturn(mpAfter);
    EasyMock.replay(service);
    operationHandler.setDistributionService(service);

    String sourceTags = "nosuchtag";
    String targetTags = "engage,publish";

    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, targetTags);

    // Run the media package through the operation handler, ensuring that metadata gets added
    WorkflowOperationResult result = operationHandler.start(workflowInstance);
    MediaPackage mpNew = result.getMediaPackage();

    // test that the new track was annotated with tags
    String actualTags = StringUtils.join(mpNew.getTrack(t.getIdentifier()).getTags(), ",");
    Assert.assertEquals(targetTags, actualTags);

  }

  @Test
  public void testSourceTags() throws Exception {
    service = EasyMock.createStrictMock(DistributionService.class);
    EasyMock.expect(
            service.distribute((MediaPackage) EasyMock.anyObject(), EasyMock.matches("catalog-2"), EasyMock
                    .matches("notes"), EasyMock.matches("track-2"), EasyMock.matches("catalog-1"))).andReturn(mpAfter);
    EasyMock.expect(
            service.distribute((MediaPackage) EasyMock.anyObject(), EasyMock.matches("catalog-2"), EasyMock
                    .matches("notes"), EasyMock.matches("catalog-1"))).andReturn(mpAfter);
    EasyMock.expect(
            service.distribute((MediaPackage) EasyMock.anyObject(), EasyMock.matches("catalog-2"), EasyMock
                    .matches("catalog-1"))).andReturn(mpAfter);
    EasyMock.replay(service);
    operationHandler.setDistributionService(service);

    
    // Source tags get tested by StrictMock
    String sourceTags = "engage,atom,rss";
    String targetTags = "engage,publish";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    operationHandler.start(workflowInstance);

    sourceTags = "rss";
    workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    operationHandler.start(workflowInstance);

    sourceTags = "nosuchtag";
    workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    operationHandler.start(workflowInstance);

  }

  private WorkflowInstance getWorkflowInstance(String sourceTags, String targetTags) {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId("workflow-distribute-test");
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operationInstance = new WorkflowOperationInstanceImpl();

    operationInstance.setConfiguration("source-tags", sourceTags);
    operationInstance.setConfiguration("target-tags", targetTags);

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operationInstance);
    workflowInstance.setOperations(operationsList);
    workflowInstance.next();

    return workflowInstance;
  }

}
