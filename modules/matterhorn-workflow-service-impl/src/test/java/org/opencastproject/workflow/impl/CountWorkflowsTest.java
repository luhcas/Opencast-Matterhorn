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

import static org.junit.Assert.assertEquals;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class CountWorkflowsTest {

  /** The solr root directory */
  protected static final String storageRoot = "." + File.separator + "target" + File.separator + "workflow-test-db";

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition def = null;
  private MediaPackage mp = null;
  private WorkflowServiceImplDaoFileImpl dao = null;
  private Workspace workspace = null;
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
    InputStream is = HoldStateTest.class.getResourceAsStream("/mediapackage-1.xml");
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

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);
    dao = new WorkflowServiceImplDaoFileImpl();
    dao.setWorkspace(workspace);
    dao.solrRoot = storageRoot + File.separator + "solr";
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
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testHoldAndResume() throws Exception {
    Map<String, String> initialProps = new HashMap<String, String>();
    initialProps.put("testproperty", "foo");
    WorkflowInstance workflow1 = service.start(def, mp, initialProps);
    WorkflowInstance workflow2 = service.start(def, mp, initialProps);

    // Wait for both workflows to be in paused state
    boolean waitForEverybody = true;
    while (waitForEverybody) {
      System.out.println("Waiting for both workflows to enter paused state...");
      try {
        waitForEverybody = !service.getWorkflowById(workflow1.getId()).getState().equals(WorkflowState.PAUSED);
        waitForEverybody |= !service.getWorkflowById(workflow2.getId()).getState().equals(WorkflowState.PAUSED);
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }

    // Test for two paused workflows in "op1"
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(2, service.countWorkflowInstances(WorkflowState.PAUSED, null));
    assertEquals(2, service.countWorkflowInstances(null, "op1"));
    assertEquals(2, service.countWorkflowInstances(WorkflowState.PAUSED, "op1"));
    assertEquals(0, service.countWorkflowInstances(WorkflowState.SUCCEEDED, null));
    assertEquals(0, service.countWorkflowInstances(null, "op2"));
    assertEquals(0, service.countWorkflowInstances(WorkflowState.SUCCEEDED, "op1"));

    // Continue one of the two worfkows
    service.resume(workflow1.getId());
    while (!service.getWorkflowById(workflow1.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow 1 to finish...");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // Nothing to be done here
      }
    }

    // Make sure one workflow is still on hold, the other is finished.
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(1, service.countWorkflowInstances(WorkflowState.PAUSED, null));
    assertEquals(1, service.countWorkflowInstances(WorkflowState.PAUSED, "op1"));
    assertEquals(1, service.countWorkflowInstances(WorkflowState.SUCCEEDED, null));
  }

  /**
   * Test implementation for a workflow operation handler that will do nothing and not hold.
   */
  class HoldingWorkflowOperationHandler extends AbstractResumableWorkflowOperationHandler {

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
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
    }
  }

  /**
   * Test implementatio for a workflow operation handler that will go on hold, and continue when resume() is called.
   */
  class ContinuingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
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
