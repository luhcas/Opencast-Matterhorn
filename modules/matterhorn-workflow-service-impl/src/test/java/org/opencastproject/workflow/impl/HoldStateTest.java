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
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandlerBase;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class HoldStateTest {

  /** The solr root directory */
  protected static final String storageRoot = "." + File.separator + "target" + File.separator + "workflow-test-db";

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition def = null;
  private WorkflowInstance workflow = null;
  private MediaPackage mp = null;
  private WorkflowServiceDaoSolrImpl dao = null;
  private HoldingWorkflowOperationHandler holdingOperationHandler;

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
    InputStream is = CountWorkflowsTest.class.getResourceAsStream("/mediapackage-1.xml");
    mp = mediaPackageBuilder.loadFromXml(is);
    IOUtils.closeQuietly(is);

    // create operation handlers for our workflows
    final Set<HandlerRegistration> handlerRegistrations = new HashSet<HandlerRegistration>();
    holdingOperationHandler = new HoldingWorkflowOperationHandler();
    handlerRegistrations.add(new HandlerRegistration("op1", holdingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", new ContinuingWorkflowOperationHandler()));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };

    ServiceRegistry serviceRegistry = new MockServiceRegistry();
    service.setServiceRegistry(serviceRegistry);

    dao = new WorkflowServiceDaoSolrImpl();
    dao.solrRoot = storageRoot + File.separator + "solr";
    dao.setServiceRegistry(serviceRegistry);
    dao.activate();
    service.setDao(dao);
    service.activate(null);

    is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-holdstate.xml");
    def = WorkflowBuilder.getInstance().parseWorkflowDefinition(is);
    IOUtils.closeQuietly(is);
    service.registerWorkflowDefinition(def);
  }

  @After
  public void teardown() throws Exception {
    System.out.println("All tests finished... tearing down...");
    if (workflow != null) {
      while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
        System.out.println("Waiting for workflow to complete, current state is "
                + service.getWorkflowById(workflow.getId()).getState());
        Thread.sleep(500);
      }
    }
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testHoldAndResume() throws Exception {
    Map<String, String> initialProps = new HashMap<String, String>();
    initialProps.put("testproperty", "foo");
    workflow = service.start(def, mp, initialProps);
    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.PAUSED)) {
      System.out.println("Waiting for workflow to enter paused state...");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }

    // The variable "testproperty" should have been replaced by "foo", but not "anotherproperty"
    String xml = WorkflowBuilder.getInstance().toXml(workflow);
    Assert.assertTrue(xml.contains("foo"));
    Assert.assertTrue(xml.contains("anotherproperty"));

    // Simulate a user resuming and submitting new properties (this time, with a value for "anotherproperty") to the
    // workflow
    Map<String, String> resumeProps = new HashMap<String, String>();
    resumeProps.put("anotherproperty", "bar");
    service.resume(workflow.getId(), resumeProps);

    WorkflowInstance fromDb = service.getWorkflowById(workflow.getId());
    String xmlFromDb = WorkflowBuilder.getInstance().toXml(fromDb);
    Assert.assertTrue(!xmlFromDb.contains("anotherproperty"));
    Assert.assertTrue(xmlFromDb.contains("foo"));
    Assert.assertTrue(xmlFromDb.contains("bar"));
  }

  @Test
  public void testMultipleHolds() throws Exception {
    workflow = service.start(def, mp);
    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.PAUSED)) {
      System.out.println("Waiting for workflow to enter paused state...");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }

    // Simulate a user resuming the workflow, but the handler still keeps the workflow in a hold state
    holdingOperationHandler.pauseOnResume = true;
    service.resume(workflow.getId());

    // The workflow is running again, but should very quickly reenter the paused state
    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.PAUSED)) {
      System.out.println("Waiting for workflow to reenter paused state...");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }

    WorkflowInstance fromDb = service.getWorkflowById(workflow.getId());
    Assert.assertEquals(WorkflowState.PAUSED, fromDb.getState());

    // Resume the workflow again, and this time continue with the workflow
    holdingOperationHandler.pauseOnResume = false;
    service.resume(workflow.getId());

    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to finish...");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }
    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(workflow.getId()).getState());
  }

  class HoldingWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

    /** Whether to return pause or continue when {@link #resume(WorkflowInstance)} is called */
    boolean pauseOnResume;

    public SortedMap<String, String> getConfigurationOptions() {
      return new TreeMap<String, String>();
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
      Action action = pauseOnResume ? Action.PAUSE : Action.CONTINUE;
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(action);
    }
  }

  class ContinuingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
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
  }
}
