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
package org.opencastproject.ingest.impl;

import org.opencastproject.conductor.api.ConductorStrategy;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public class IngestServiceImplTest {
  private IngestServiceImpl service = null;
  private ConductorStrategy conductorStrategy = null;
  private WorkflowService workflowService = null;
  private WorkflowInstance workflowInstance = null;
  private WorkflowDefinition workflowDefinition = null;
  private Workspace workspace = null;
  private MediaPackage mediaPackage = null;
  private URI urlTrack;
  private URI urlTrack1;
  private URI urlTrack2;
  private URI urlCatalog;
  private URI urlCatalog1;
  private URI urlCatalog2;
  private URI urlAttachment;
  private URI urlPackage;

  private static String workflowInstanceID = "junit_workflow_instance_id";

  @Before
  public void setup() throws Exception {

    urlTrack = IngestServiceImplTest.class.getResource("/av.mov").toURI();
    urlTrack1 = IngestServiceImplTest.class.getResource("/vonly.mov").toURI();
    urlTrack2 = IngestServiceImplTest.class.getResource("/aonly.mov").toURI();
    urlCatalog = IngestServiceImplTest.class.getResource("/mpeg-7.xml").toURI();
    urlCatalog1 = IngestServiceImplTest.class.getResource("/dublincore.xml").toURI();
    urlCatalog2 = IngestServiceImplTest.class.getResource("/series-dublincore.xml").toURI();
    urlAttachment = IngestServiceImplTest.class.getResource("/cover.png").toURI();
    urlPackage = IngestServiceImplTest.class.getResource("/data.zip").toURI();
    // set up service and mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlAttachment);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    workflowDefinition = EasyMock.createNiceMock(WorkflowDefinition.class);

    conductorStrategy = EasyMock.createNiceMock(ConductorStrategy.class);
    EasyMock.expect(conductorStrategy.getWorkflow((String) EasyMock.anyObject())).andReturn(workflowDefinition);

    workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowInstanceID);

    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map) EasyMock.anyObject())).andReturn(workflowInstance);

    EasyMock.replay(workspace);
    EasyMock.replay(conductorStrategy);
    EasyMock.replay(workflowInstance);
    EasyMock.replay(workflowService);

    service = new IngestServiceImpl();
    service.setTempFolder("target/temp/");
    service.setWorkspace(workspace);
    service.setConductorStrategy(conductorStrategy);
    service.setWorkflowService(workflowService);
  }

  @After
  public void teardown() {

  }

  @Test
  public void testThinClient() throws Exception {
    mediaPackage = service.createMediaPackage();
    mediaPackage = service.addTrack(urlTrack, null, mediaPackage);
    mediaPackage = service.addCatalog(urlCatalog, DublinCoreCatalog.FLAVOR, mediaPackage);
    mediaPackage = service.addAttachment(urlAttachment, MediaPackageElements.COVER_FLAVOR, mediaPackage);
    String def = service.ingest(mediaPackage);
    Assert.assertEquals(1, mediaPackage.getTracks().length);
    Assert.assertEquals(1, mediaPackage.getCatalogs().length);
    Assert.assertEquals(1, mediaPackage.getAttachments().length);
    Assert.assertEquals(workflowInstanceID, def);
  }

  @Test
  public void testThickClient() throws Exception {
    InputStream packageStream = urlPackage.toURL().openStream();
    String def = service.addZippedMediaPackage(packageStream);
    try {
      packageStream.close();
    } catch (IOException e) {
    }
    // Assert.assertEquals(2, mediaPackage.getTracks().length);
    // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
    Assert.assertEquals(workflowInstanceID, def);
  }

}
