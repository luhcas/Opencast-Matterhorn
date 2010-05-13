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

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageMetadata;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.Receipt.Status;
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
import java.util.Date;
import java.util.List;

public class InspectWorkflowOperationHandlerTest {
  private InspectWorkflowOperationHandler operationHandler;
  // private DublinCoreCatalogService dcService = null;
  private Workspace workspace = null;
  private MediaInspectionService inspectionService = null;

  private URI uriMP;
  private URI uriMPUpdated;
  private MediaPackage mp;
  private MediaPackage mpUpdatedDC;
  private Track newTrack;
  private Receipt receipt;

  private MediaPackageMetadataService metadataService;
  private MediaPackageMetadata metadata;
  private static final Date DATE = new Date(1);
  private static final String LANGUAGE = "language";
  private static final String LICENSE = "license";
  private static final String SERIES = "series";
  private static final String SERIES_TITLE = "series title";
  private static final String TITLE = "title";

  @Before
  public void setup() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    
    // test resources
    uriMP = InspectWorkflowOperationHandler.class.getResource("/inspect_mediapackage.xml").toURI();
    uriMPUpdated = InspectWorkflowOperationHandler.class.getResource("/inspect_mediapackage_updated.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    mpUpdatedDC = builder.loadFromXml(uriMPUpdated.toURL().openStream());
    newTrack = (Track)mpUpdatedDC.getTracks()[0];

    // set up service
    operationHandler = new InspectWorkflowOperationHandler();

    // set up mock metadata and metadata service providing it
    metadata = EasyMock.createNiceMock(MediaPackageMetadata.class);
    EasyMock.expect(metadata.getDate()).andReturn(DATE);
    EasyMock.expect(metadata.getLanguage()).andReturn(LANGUAGE);
    EasyMock.expect(metadata.getLicense()).andReturn(LICENSE);
    EasyMock.expect(metadata.getSeriesIdentifier()).andReturn(SERIES);
    EasyMock.expect(metadata.getSeriesTitle()).andReturn(SERIES_TITLE);
    EasyMock.expect(metadata.getTitle()).andReturn(TITLE);
    EasyMock.replay(metadata);

    metadataService = EasyMock.createNiceMock(MediaPackageMetadataService.class);
    EasyMock.expect(metadataService.getMetadata((MediaPackage) EasyMock.anyObject())).andReturn(metadata);
    EasyMock.replay(metadataService);

    operationHandler.addMetadataService(metadataService);

    // set up mock receipt and inspect service providing it
    receipt = EasyMock.createNiceMock(Receipt.class);
    EasyMock.expect(receipt.getElement()).andReturn(newTrack);
    EasyMock.expect(receipt.getId()).andReturn("123");
    EasyMock.expect(receipt.getStatus()).andReturn(Status.FINISHED);
    EasyMock.replay(receipt);

    inspectionService = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(
            inspectionService.enrich((Track) EasyMock.anyObject(), EasyMock.anyBoolean(), EasyMock.anyBoolean()))
            .andReturn(receipt);
    EasyMock.replay(inspectionService);

    operationHandler.setInspectionService(inspectionService);

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);

    //TODO: problem requested is dublicorecatalogservice impl
    // set up mock dublin core and dcService providing it
//    dc = EasyMock.createNiceMock(DublinCoreCatalog.class);
//    EasyMock.expect(dc.hasValue(DublinCore.PROPERTY_EXTENT)).andReturn(false);
//    EasyMock.expect(dc.hasValue(DublinCore.PROPERTY_CREATED)).andReturn(false);
//    
//    CatalogService<DublinCoreCatalog> dcService = EasyMock.createNiceMock(CatalogService.class);
//    EasyMock.expect(dcService.load((Catalog)EasyMock.anyObject())).andReturn(dc);
//    EasyMock.replay(dcService);
//    operationHandler.setDublincoreService((DublinCoreCatalogService)dcService);
  }

  @Test
  public void testInspectOperation() throws Exception {
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
    MediaPackage mpNew = result.getMediaPackage();
    Track trackNew = mpNew.getTracks()[0];
    
    // check mediapackage metadata
    Assert.assertEquals(DATE, mpNew.getDate());
    Assert.assertEquals(LANGUAGE, mpNew.getLanguage());
    Assert.assertEquals(LICENSE, mpNew.getLicense());
    Assert.assertEquals(SERIES, mpNew.getSeries());
    Assert.assertEquals(SERIES_TITLE, mpNew.getSeriesTitle());
    Assert.assertEquals(TITLE, mpNew.getTitle());
    
    // check track metadata
    Assert.assertNotNull(trackNew.getChecksum());
    Assert.assertNotNull(trackNew.getMimeType());
    Assert.assertNotNull(trackNew.getDuration());
    Assert.assertNotNull(trackNew.getStreams());
    
  }
}
