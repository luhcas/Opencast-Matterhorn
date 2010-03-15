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
package org.opencastproject.conductor.impl;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class InspectWorkflowOperationHandlerTest {
  private InspectWorkflowOperationHandler operationHandler;
  private DublinCoreCatalogService dcService = null;
  private Workspace workspace = null;
  private MediaInspectionService inspectionService = null;

  private URI uriTrack;
  private URI uriMP;
  private Track track, newTrack;
  private MediaPackage mp;

  @Before
  public void setup() throws Exception {
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    
    uriTrack = InspectWorkflowOperationHandler.class.getResource("/av.mov").toURI();
    uriMP = InspectWorkflowOperationHandler.class.getResource("/manifest.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    track =  (Track)elementBuilder.elementFromURI(new URI("http://foo.bar"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    newTrack = mp.getTracks()[0];
    
    // set up service
    operationHandler = new InspectWorkflowOperationHandler();
    
    // set up mock inspect
    inspectionService = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(inspectionService.enrich((Track)EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(newTrack);
    EasyMock.replay(inspectionService);
    operationHandler.setInspectionService(inspectionService);
    
    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);
    
    // set up mock dcService
    // DublinCoreCatalogService dcService = EasyMock.createNiceMock(DublinCoreCatalogService.class);
    // EasyMock.replay(dcService);
    // operationHandler.setDublincoreService(dcService);
  }

  @Test
  public void testPublishOperation() throws Exception {
    // Set up a mediapackage to publish
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    Track presentationTrack = (Track) elementBuilder.elementFromURI(uriTrack, Track.TYPE,
            MediaPackageElements.PRESENTATION_SOURCE);
    mp.add(presentationTrack);

    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId("workflow-inspect-test");
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operationInstance = new WorkflowOperationInstanceImpl();
    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operationInstance);
    workflowInstance.setOperations(operationsList);
    workflowInstance.next(); // Simulate starting the workflow

    // Run the media package through the operation handler, ensuring that metadata gets added
    WorkflowOperationResult result = operationHandler.start(workflowInstance);
    for (Track t : result.getMediaPackage().getTracks()) {
      Assert.assertNotNull(t.getChecksum());
      Assert.assertNotNull(t.getMimeType());
      Assert.assertNotNull(t.getDuration());
    }
  }
}
