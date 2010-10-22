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

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public class IngestServiceImplTest {
  private IngestServiceImpl service = null;
  private WorkflowService workflowService = null;
  private WorkflowInstance workflowInstance = null;
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

  @SuppressWarnings("unchecked")
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

    workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowInstanceID);

    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.start((MediaPackage) EasyMock.anyObject(), (Map) EasyMock.anyObject())).andReturn(
            workflowInstance);
    EasyMock.expect(workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(), (Map) EasyMock.anyObject())).andReturn(
            workflowInstance);
    EasyMock.expect(workflowService.start((MediaPackage) EasyMock.anyObject())).andReturn(workflowInstance);

    EasyMock.replay(workspace);
    EasyMock.replay(workflowInstance);
    EasyMock.replay(workflowService);

    HttpEntity entity = EasyMock.createMock(HttpEntity.class);
    InputStream is = getClass().getResourceAsStream("/av.mov");
    byte[] movie = IOUtils.toByteArray(is);
    IOUtils.closeQuietly(is);
    EasyMock.expect(entity.getContent()).andReturn(new ByteArrayInputStream(movie)).anyTimes();
    EasyMock.replay(entity);
    
    StatusLine statusLine = EasyMock.createMock(StatusLine.class);
    EasyMock.expect(statusLine.getStatusCode()).andReturn(200).anyTimes();
    EasyMock.replay(statusLine);
    
    HttpResponse httpResponse = EasyMock.createMock(HttpResponse.class);
    EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine).anyTimes();
    EasyMock.expect(httpResponse.getEntity()).andReturn(entity).anyTimes();
    EasyMock.replay(httpResponse);
    
    TrustedHttpClient httpClient = EasyMock.createMock(TrustedHttpClient.class);
    EasyMock.expect(httpClient.execute((HttpGet)EasyMock.anyObject())).andReturn(httpResponse).anyTimes();
    EasyMock.replay(httpClient);
    
    Job job = new JaxbJob();
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.createJob((String)EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(job).anyTimes();
    EasyMock.replay(serviceRegistry);
    
    service = new IngestServiceImpl();
    service.setHttpClient(httpClient);
    service.setTempFolder("target/temp/");
    service.setWorkspace(workspace);
    service.setWorkflowService(workflowService);
    service.setServiceRegistry(serviceRegistry);
  }

  @After
  public void teardown() {

  }

  @Test
  public void testThinClient() throws Exception {
    mediaPackage = service.createMediaPackage();
    mediaPackage = service.addTrack(urlTrack, null, mediaPackage);
    mediaPackage = service.addCatalog(urlCatalog, MediaPackageElements.EPISODE, mediaPackage);
    mediaPackage = service.addAttachment(urlAttachment, MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR, mediaPackage);
    WorkflowInstance instance = service.ingest(mediaPackage);
    Assert.assertEquals(1, mediaPackage.getTracks().length);
    Assert.assertEquals(1, mediaPackage.getCatalogs().length);
    Assert.assertEquals(1, mediaPackage.getAttachments().length);
    Assert.assertEquals(workflowInstanceID, instance.getId());
  }

  @Test
  public void testThickClient() throws Exception {
    InputStream packageStream = urlPackage.toURL().openStream();
    WorkflowInstance instance = service.addZippedMediaPackage(packageStream);
    try {
      packageStream.close();
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
    // Assert.assertEquals(2, mediaPackage.getTracks().length);
    // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
    Assert.assertEquals(workflowInstanceID, instance.getId());
  }

}
