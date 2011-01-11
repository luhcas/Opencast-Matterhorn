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
import static org.junit.Assert.assertNotNull;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParser;
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

public class WorkflowOperationSkippingTest {

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition workingDefinition = null;
  private MediaPackage mediapackage1 = null;
  private SucceedingWorkflowOperationHandler succeedingOperationHandler = null;
  private WorkflowServiceDaoSolrImpl dao = null;
  private Set<HandlerRegistration> handlerRegistrations = null;
  private Workspace workspace = null;

  @Before
  public void setup() throws Exception {
    // always start with a fresh solr root directory
    String storageRoot = "." + File.separator + "target" + File.separator + "workflow-test-db" + File.separator
            + System.currentTimeMillis();
    File sRoot = new File(storageRoot);
    try {
      FileUtils.forceMkdir(sRoot);
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    // create operation handlers for our workflows
    succeedingOperationHandler = new SucceedingWorkflowOperationHandler(mediapackage1);
    handlerRegistrations = new HashSet<HandlerRegistration>();
    handlerRegistrations.add(new HandlerRegistration("op1", succeedingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", succeedingOperationHandler));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);

    ServiceRegistry serviceRegistry = new ServiceRegistryInMemoryImpl();

    dao = new WorkflowServiceDaoSolrImpl();
    dao.setServiceRegistry(serviceRegistry);
    dao.solrRoot = storageRoot + File.separator + "solr." + System.currentTimeMillis();
    dao.activate();
    service.setDao(dao);
    service.activate(null);

    InputStream is = null;
    try {
      is = WorkflowOperationSkippingTest.class.getResourceAsStream("/workflow-definition-skipping.xml");
      workingDefinition = WorkflowParser.parseWorkflowDefinition(is);
      service.registerWorkflowDefinition(workingDefinition);
      IOUtils.closeQuietly(is);

      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      is = WorkflowOperationSkippingTest.class.getResourceAsStream("/mediapackage-1.xml");
      mediapackage1 = mediaPackageBuilder.loadFromXml(is);
      IOUtils.closeQuietly(is);
      Assert.assertNotNull(mediapackage1.getIdentifier());
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @After
  public void teardown() throws Exception {
    System.out.println("All tests finished... tearing down...");
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testIf() throws Exception {
    Map<String, String> properties1 = new HashMap<String, String>();
    properties1.put("executecondition", "true");

    Map<String, String> properties2 = new HashMap<String, String>();
    properties2.put("executecondition", "foo");

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, properties1, WorkflowState.SUCCEEDED);
    WorkflowInstance instance2 = startAndWait(workingDefinition, mediapackage1, properties2, WorkflowState.SUCCEEDED);
    WorkflowInstance instance3 = startAndWait(workingDefinition, mediapackage1, null, WorkflowState.SUCCEEDED);

    // See if the skip operation has been executed
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    assertNotNull(instanceFromDb);
    assertEquals(OperationState.SUCCEEDED, instanceFromDb.getOperations().get(0).getState());

    // See if the skip operation has been skipped (skip value != "true")
    WorkflowInstance instance2FromDb = service.getWorkflowById(instance2.getId());
    assertNotNull(instance2FromDb);
    assertEquals(OperationState.SUCCEEDED, instance2FromDb.getOperations().get(0).getState());

    // See if the skip operation has been skipped (skip property is undefined)
    WorkflowInstance instance3FromDb = service.getWorkflowById(instance3.getId());
    assertNotNull(instance3FromDb);
    assertEquals(OperationState.SKIPPED, instance3FromDb.getOperations().get(0).getState());
  }

  @Test
  public void testUnless() throws Exception {
    Map<String, String> properties1 = new HashMap<String, String>();
    properties1.put("skipcondition", "true");

    Map<String, String> properties2 = new HashMap<String, String>();
    properties2.put("skipcondition", "foo");

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, properties1, WorkflowState.SUCCEEDED);
    WorkflowInstance instance2 = startAndWait(workingDefinition, mediapackage1, properties2, WorkflowState.SUCCEEDED);
    WorkflowInstance instance3 = startAndWait(workingDefinition, mediapackage1, null, WorkflowState.SUCCEEDED);

    // See if the skip operation has been executed
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    assertNotNull(instanceFromDb);
    assertEquals(OperationState.SKIPPED, instanceFromDb.getOperations().get(1).getState());

    // See if the skip operation has been skipped (skip value != "true")
    WorkflowInstance instance2FromDb = service.getWorkflowById(instance2.getId());
    assertNotNull(instance2FromDb);
    assertEquals(OperationState.SKIPPED, instance2FromDb.getOperations().get(1).getState());

    // See if the skip operation has been skipped (skip property is undefined)
    WorkflowInstance instance3FromDb = service.getWorkflowById(instance3.getId());
    assertNotNull(instance3FromDb);
    assertEquals(OperationState.SUCCEEDED, instance3FromDb.getOperations().get(1).getState());
  }

  protected WorkflowInstance startAndWait(WorkflowDefinition definition, MediaPackage mp,
          Map<String, String> properties, WorkflowState stateToWaitFor) throws Exception {
    WorkflowInstance instance = service.start(definition, mp, properties);
    while (!service.getWorkflowById(instance.getId()).getState().equals(stateToWaitFor)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    return instance;
  }

  class SucceedingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    MediaPackage mp;

    SucceedingWorkflowOperationHandler(MediaPackage mp) {
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
      return createResult(mp, Action.CONTINUE);
    }
  }

}
