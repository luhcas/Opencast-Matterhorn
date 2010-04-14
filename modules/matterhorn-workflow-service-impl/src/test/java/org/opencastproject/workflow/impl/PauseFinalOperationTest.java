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

import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workingfilerepository.impl.WorkingFileRepositoryImpl;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PauseFinalOperationTest {
  /** The solr root directory */
  protected static final String storageRoot = "." + File.separator + "target" + File.separator + "workflow-test-db";

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition def = null;
  private WorkflowInstance workflow = null;
  private MediaPackage mp = null;
  private WorkflowServiceImplDaoFileImpl dao = null;
  private WorkingFileRepositoryImpl repo = null;
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
    mp = mediaPackageBuilder.loadFromXml(WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml"));

    // create operation handlers for our workflows
    final Set<HandlerRegistration> handlerRegistrations = new HashSet<HandlerRegistration>();
    handler = new OpHandler(mp);
    handlerRegistrations.add(new HandlerRegistration("op1", handler));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      protected Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };

    repo = new WorkingFileRepositoryImpl(storageRoot, sRoot.toURI().toString());
    dao = new WorkflowServiceImplDaoFileImpl();
    dao.setRepository(repo);
    dao.setStorageRoot(storageRoot + File.separator + "lucene");
    dao.activate();
    service.setDao(dao);
    service.activate(null);

    def = WorkflowBuilder.getInstance().parseWorkflowDefinition(
            WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-pause-last.xml"));
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
      try {Thread.sleep(1000);} catch (InterruptedException e) {}
    }
    // Ensure that "start" was called on the first operation handler, but not resume
    Assert.assertTrue(handler.startCalled);
    Assert.assertTrue( ! handler.resumeCalled);

    // The workflow should be in the paused state
    Assert.assertEquals(WorkflowState.PAUSED, service.getWorkflowById(workflow.getId()).getState());
    
    // Resume the workflow
    service.resume(workflow.getId());
    
    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflows to complete...");
      try {Thread.sleep(1000);} catch (InterruptedException e) {}
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
    public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      startCalled = true;
      return super.start(workflowInstance);
    }
    
    @Override
    public WorkflowOperationResult resume(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      resumeCalled = true;
      return super.resume(workflowInstance);
    }
  }

}
