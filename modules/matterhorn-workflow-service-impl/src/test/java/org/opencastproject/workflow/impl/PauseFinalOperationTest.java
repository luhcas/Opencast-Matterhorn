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
package org.opencastproject.workflow.impl;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class PauseFinalOperationTest {
  /** The solr root directory */
  protected static final String storageRoot = "." + File.separator + "target" + File.separator + "workflow-test-db";

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition def = null;
  private WorkflowInstance workflow = null;
  private MediaPackage mp = null;
  private WorkflowServiceImplDaoFileImpl dao = null;
  private Workspace workspace = null;
  private OpHandler handler = null;

  @Before
  public void setup() throws Exception {
    // always start with a fresh solr root directory
    File sRoot = new File(storageRoot);
    try {
      FileUtils.deleteDirectory(sRoot);
      FileUtils.forceMkdir(sRoot);
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
    InputStream is = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml");
    mp = mediaPackageBuilder.loadFromXml(is);
    IOUtils.closeQuietly(is);

    // create operation handlers for our workflows
    final Set<HandlerRegistration> handlerRegistrations = new HashSet<HandlerRegistration>();
    handler = new OpHandler(mp);
    handlerRegistrations.add(new HandlerRegistration("op1", handler));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);
    dao = new WorkflowServiceImplDaoFileImpl();
    dao.setWorkspace(workspace);
    dao.solrRoot = storageRoot + File.separator + "solr";
    dao.activate();
    service.setDao(dao);
    service.activate(null);

    is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-pause-last.xml");
    def = WorkflowBuilder.getInstance().parseWorkflowDefinition(is);
    IOUtils.closeQuietly(is);
    service.registerWorkflowDefinition(def);
  }

  @After
  public void teardown() throws Exception {
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testHoldAndResume() throws Exception {
    // Start a new workflow
    workflow = service.start(def, mp, null);

    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.PAUSED)) {
      System.out.println("Waiting for workflows to enter the hold state...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    // Ensure that "start" was called on the first operation handler, but not resume
    Assert.assertTrue(handler.startCalled);
    Assert.assertTrue(!handler.resumeCalled);

    // The workflow should be in the paused state
    Assert.assertEquals(WorkflowState.PAUSED, service.getWorkflowById(workflow.getId()).getState());

    // Resume the workflow
    service.resume(workflow.getId());

    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflows to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(workflow.getId()).getState());
  }

  class OpHandler extends AbstractResumableWorkflowOperationHandler {
    MediaPackage mp;
    boolean startCalled = false;
    boolean resumeCalled = false;

    OpHandler(MediaPackage mp) {
      this.mp = mp;
    }

    @Override
    public SortedMap<String, String> getConfigurationOptions() {
      return new TreeMap<String, String>();
    }

    @Override
    public String getId() {
      return this.getClass().getName();
    }

    @Override
    public String getDescription() {
      return "ContinuingWorkflowOperationHandler";
    }

    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      startCalled = true;
      return super.start(workflowInstance);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
     *      java.util.Map)
     */
    @Override
    public WorkflowOperationResult resume(WorkflowInstance workflowInstance, Map<String, String> properties)
            throws WorkflowOperationException {
      resumeCalled = true;
      return super.resume(workflowInstance, properties);
    }
  }

}
