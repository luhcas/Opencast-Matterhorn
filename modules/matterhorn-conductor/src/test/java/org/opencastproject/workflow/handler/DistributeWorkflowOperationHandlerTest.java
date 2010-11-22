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
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;

import org.easymock.EasyMock;
import org.junit.Assert;
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

  @Before
  public void setup() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    uriMP = InspectWorkflowOperationHandler.class.getResource("/distribute_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    // set up the handler
    operationHandler = new DistributeWorkflowOperationHandler();

  }

  @Test
  public void testSourceTags() throws Exception {
    MediaPackageElement track2 = (MediaPackageElement) mp.getElementById("track-2");
    MediaPackageElement catalog1 = (MediaPackageElement) mp.getElementById("catalog-1");
    MediaPackageElement catalog2 = (MediaPackageElement) mp.getElementById("catalog-2");
    MediaPackageElement attachment1 = (MediaPackageElement) mp.getElementById("notes");

    Assert.assertNotNull(track2);

    // Mock up a job
    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getPayload()).andReturn(track2.getAsXml());
    EasyMock.expect(job.getPayload()).andReturn(catalog1.getAsXml());
    EasyMock.expect(job.getPayload()).andReturn(catalog2.getAsXml());
    EasyMock.expect(job.getPayload()).andReturn(attachment1.getAsXml());
    EasyMock.replay(job);

    service = EasyMock.createNiceMock(DistributionService.class);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), track2, true)).andReturn(job);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), catalog1, true)).andReturn(job);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), catalog2, true)).andReturn(job);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), attachment1, true)).andReturn(job);
    EasyMock.replay(service);
    operationHandler.setDistributionService(service);

    // Source tags get tested by our mock
    String sourceTags = "engage,atom,rss";
    String targetTags = "engage,publish";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    operationHandler.start(workflowInstance);
    EasyMock.verify(service);
  }

  @Test
  public void testRssTag() throws Exception {
    MediaPackageElement track2 = (MediaPackageElement) mp.getElementById("track-2");
    MediaPackageElement catalog1 = (MediaPackageElement) mp.getElementById("catalog-1");
    MediaPackageElement catalog2 = (MediaPackageElement) mp.getElementById("catalog-2");
    MediaPackageElement attachment1 = (MediaPackageElement) mp.getElementById("notes");

    Assert.assertNotNull(track2);

    // Mock up a job
    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getPayload()).andReturn(catalog1.getAsXml());
    EasyMock.expect(job.getPayload()).andReturn(catalog2.getAsXml());
    EasyMock.expect(job.getPayload()).andReturn(attachment1.getAsXml());
    EasyMock.replay(job);

    service = EasyMock.createNiceMock(DistributionService.class);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), catalog1, true)).andReturn(job);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), catalog2, true)).andReturn(job);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), attachment1, true)).andReturn(job);
    EasyMock.replay(service);
    operationHandler.setDistributionService(service);

    // Source tags get tested by our mock
    String sourceTags = "rss";
    String targetTags = "engage,publish";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    operationHandler.start(workflowInstance);
    EasyMock.verify(service);

  }

  @Test
  public void testNoSuchTag() throws Exception {
    MediaPackageElement catalog1 = (MediaPackageElement) mp.getElementById("catalog-1");
    MediaPackageElement catalog2 = (MediaPackageElement) mp.getElementById("catalog-2");
    MediaPackageElement attachment1 = (MediaPackageElement) mp.getElementById("notes");


    // Mock up a job
    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getPayload()).andReturn(catalog1.getAsXml());
    EasyMock.expect(job.getPayload()).andReturn(catalog2.getAsXml());
    EasyMock.expect(job.getPayload()).andReturn(attachment1.getAsXml());
    EasyMock.replay(job);

    service = EasyMock.createNiceMock(DistributionService.class);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), catalog1, true)).andReturn(job);
    EasyMock.expect(service.distribute(mp.getIdentifier().compact(), catalog2, true)).andReturn(job);
    EasyMock.replay(service);
    operationHandler.setDistributionService(service);

    // Source tags get tested by our mock
    String sourceTags = "nosuchtag";
    String targetTags = "engage,publish";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    operationHandler.start(workflowInstance);
    EasyMock.verify(service);
  }

  private WorkflowInstance getWorkflowInstance(String sourceTags, String targetTags) {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
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
