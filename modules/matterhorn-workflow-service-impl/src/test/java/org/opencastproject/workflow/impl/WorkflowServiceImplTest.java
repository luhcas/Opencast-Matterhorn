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
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class WorkflowServiceImplTest {

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition workingDefinition = null;
  private WorkflowDefinition failingDefinitionWithoutErrorHandler = null;
  private WorkflowDefinition failingDefinitionWithErrorHandler = null;
  private MediaPackage mediapackage1 = null;
  private MediaPackage mediapackage2 = null;
  private SucceedingWorkflowOperationHandler succeedingOperationHandler = null;
  private WorkflowOperationHandler failingOperationHandler = null;
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
    succeedingOperationHandler = new SucceedingWorkflowOperationHandler();
    failingOperationHandler = new FailingWorkflowOperationHandler();
    handlerRegistrations = new HashSet<HandlerRegistration>();
    handlerRegistrations.add(new HandlerRegistration("op1", succeedingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", succeedingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op3", failingOperationHandler));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);

    ServiceRegistry serviceRegistry = new MockServiceRegistry();
    service.setServiceRegistry(serviceRegistry);

    dao = new WorkflowServiceDaoSolrImpl();
    dao.setServiceRegistry(serviceRegistry);
    dao.solrRoot = storageRoot + File.separator + "solr." + System.currentTimeMillis();
    dao.activate();
    service.setDao(dao);
    service.activate(null);

    InputStream is = null;
    try {
      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-1.xml");
      workingDefinition = WorkflowBuilder.getInstance().parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-2.xml");
      failingDefinitionWithoutErrorHandler = WorkflowBuilder.getInstance().parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-3.xml");
      failingDefinitionWithErrorHandler = WorkflowBuilder.getInstance().parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      service.registerWorkflowDefinition(workingDefinition);
      service.registerWorkflowDefinition(failingDefinitionWithoutErrorHandler);
      service.registerWorkflowDefinition(failingDefinitionWithErrorHandler);

      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));

      is = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml");
      mediapackage1 = mediaPackageBuilder.loadFromXml(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-2.xml");
      mediapackage2 = mediaPackageBuilder.loadFromXml(is);

      Assert.assertNotNull(mediapackage1.getIdentifier());
      Assert.assertNotNull(mediapackage2.getIdentifier());
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
  public void testGetWorkflowInstanceById() throws Exception {
    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);
    WorkflowInstance instance2 = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)
            && !service.getWorkflowById(instance2.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflows to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    // verify that we can retrieve the workflow instance from the service by its ID
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    Assert.assertNotNull(instanceFromDb);
    MediaPackage mediapackageFromDb = instanceFromDb.getMediaPackage();
    Assert.assertNotNull(mediapackageFromDb);
    Assert.assertEquals(mediapackage1.getIdentifier().toString(), mediapackageFromDb.getIdentifier().toString());
    Assert.assertEquals(2, service.countWorkflowInstances());

    // cleanup the database
    service.removeFromDatabase(instance.getId());
    service.removeFromDatabase(instance2.getId());

    // And ensure that they are really gone
    try {
      service.getWorkflowById(instance.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    try {
      service.getWorkflowById(instance2.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowByMediaPackageId() throws Exception {
    // Ensure that the database doesn't have a workflow instance with this media package
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(
            0,
            service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediapackage1.getIdentifier().toString()))
                    .size());
    Assert.assertEquals(
            0,
            service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediapackage2.getIdentifier().toString()))
                    .size());

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance instance2 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);
    WorkflowInstance instance3 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance.getId()).getState());
    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance2.getId()).getState());
    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance3.getId()).getState());

    Assert.assertEquals(mediapackage1.getIdentifier().toString(), service.getWorkflowById(instance.getId())
            .getMediaPackage().getIdentifier().toString());
    Assert.assertEquals(mediapackage2.getIdentifier().toString(), service.getWorkflowById(instance2.getId())
            .getMediaPackage().getIdentifier().toString());
    Assert.assertEquals(mediapackage2.getIdentifier().toString(), service.getWorkflowById(instance3.getId())
            .getMediaPackage().getIdentifier().toString());

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediapackage1
            .getIdentifier().toString()));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());
    service.removeFromDatabase(instance2.getId());
    service.removeFromDatabase(instance3.getId());

    // And ensure that it's really gone
    try {
      service.getWorkflowById(instance.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowByCreator() throws Exception {
    // Set different creators in the mediapackages
    String manfred = "Dr. Manfred Frisch";
    mediapackage1.addCreator(manfred);
    mediapackage2.addCreator("Somebody else");
    
    // Ensure that the database doesn't have any workflow instances with media packages with this creator
    Assert.assertEquals(0, service.countWorkflowInstances());

    WorkflowInstance instance1 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance instance2 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance1.getId()).getState());
    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance2.getId()).getState());

    // Build the workflow query
    WorkflowQuery queryForManfred = new WorkflowQuery().withCreator(manfred);

    Assert.assertEquals(1, service.getWorkflowInstances(queryForManfred).getTotalCount());
    Assert.assertEquals(instance1.getMediaPackage().getIdentifier().toString(),
            service.getWorkflowInstances(queryForManfred).getItems()[0].getMediaPackage().getIdentifier().toString());

    // cleanup the database
    service.removeFromDatabase(instance1.getId());
    service.removeFromDatabase(instance2.getId());

    // And ensure that it's really gone
    try {
      service.getWorkflowById(instance1.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testParentWorkflow() throws Exception {
    // Don't use startAndWait here. We want to ensure that these can run in parallel.
    WorkflowInstance originalInstance = service.start(workingDefinition, mediapackage1, null);
    WorkflowInstance childInstance = service.start(workingDefinition, mediapackage1, originalInstance.getId(), null);
    Assert.assertNotNull(service.getWorkflowById(childInstance.getId()).getParentId());
    Assert.assertEquals(originalInstance.getId(), (long) service.getWorkflowById(childInstance.getId()).getParentId());

    try {
      service.start(workingDefinition, mediapackage1, new Long(1876234678), null);
      Assert.fail("Workflows should not be started with bad parent IDs");
    } catch (NotFoundException e) {
    } // the exception is expected

    // Wait for the workflows to finish running
    while (!service.getWorkflowById(originalInstance.getId()).getState().equals(WorkflowState.SUCCEEDED)
            && !service.getWorkflowById(childInstance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflows to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    // cleanup the database
    service.removeFromDatabase(childInstance.getId());
    service.removeFromDatabase(originalInstance.getId());
  }

  @Test
  public void testGetWorkflowByEpisodeId() throws Exception {
    String mediaPackageId = mediapackage1.getIdentifier().toString();

    // Ensure that the database doesn't have a workflow instance with this episode
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediaPackageId)).size());

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediaPackageId));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    try {
      service.getWorkflowById(instance.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  // TODO This test requires a hold state
  @Test
  @Ignore
  public void testGetWorkflowByCurrentOperation() throws Exception {
    // Ensure that the database doesn't have a workflow instance in the "op2" operation
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(new WorkflowQuery().withCurrentOperation("op2")).size());

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withCurrentOperation("op2"));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowByText() throws Exception {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0,
            service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100).withStartPage(0))
                    .size());

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100)
            .withStartPage(0));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    try {
      service.getWorkflowById(instance.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testNegativeWorkflowQuery() throws Exception {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0,
            service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100).withStartPage(0))
                    .size());

    WorkflowInstance succeededInstance1 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance succeededInstance2 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);
    WorkflowInstance failedInstance = startAndWait(failingDefinitionWithoutErrorHandler, mediapackage1,
            WorkflowState.FAILED);

    WorkflowSet succeededWorkflows = service.getWorkflowInstances(new WorkflowQuery()
            .withState(WorkflowState.SUCCEEDED));
    Assert.assertEquals(2, succeededWorkflows.getItems().length);

    WorkflowSet failedWorkflows = service.getWorkflowInstances(new WorkflowQuery().withState(WorkflowState.FAILED));
    Assert.assertEquals(1, failedWorkflows.getItems().length);

    // Ensure that the "without" queries works
    WorkflowSet notFailedWorkflows = service.getWorkflowInstances(new WorkflowQuery()
            .withoutState(WorkflowState.FAILED));
    Assert.assertEquals(2, notFailedWorkflows.getItems().length);

    // cleanup the database
    service.removeFromDatabase(succeededInstance1.getId());
    service.removeFromDatabase(succeededInstance2.getId());
    service.removeFromDatabase(failedInstance.getId());

    // And ensure that they are really gone
    try {
      service.getWorkflowById(succeededInstance1.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    try {
      service.getWorkflowById(succeededInstance2.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    try {
      service.getWorkflowById(failedInstance.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  protected WorkflowInstance startAndWait(WorkflowDefinition definition, MediaPackage mp, WorkflowState stateToWaitFor)
          throws Exception {
    WorkflowInstance instance = service.start(definition, mp, null);
    while (!service.getWorkflowById(instance.getId()).getState().equals(stateToWaitFor)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    return instance;
  }

  @Test
  public void testPagedGetWorkflowByText() throws Exception {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0,
            service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100).withStartPage(0))
                    .size());

    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();
    instances.add(startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED));

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    for (WorkflowInstance instance : instances) {
      while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
        System.out.println("Waiting for workflow to complete...");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      }
    }

    Assert.assertEquals(5, service.countWorkflowInstances());
    Assert.assertEquals(5, service.getWorkflowInstances(new WorkflowQuery()).getItems().length);

    // We should get the first two workflows
    WorkflowSet firstTwoWorkflows = service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(2)
            .withStartPage(0));
    Assert.assertEquals(2, firstTwoWorkflows.getItems().length);
    Assert.assertEquals(3, firstTwoWorkflows.getTotalCount()); // The total, non-paged number of results should be three

    // We should get the last workflow
    WorkflowSet lastWorkflow = service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(1)
            .withStartPage(2));
    Assert.assertEquals(1, lastWorkflow.getItems().length);
    Assert.assertEquals(3, lastWorkflow.getTotalCount()); // The total, non-paged number of results should be three

    // We should get the first linguistics (mediapackage2) workflow
    WorkflowSet firstLinguisticsWorkflow = service.getWorkflowInstances(new WorkflowQuery().withText("Linguistics")
            .withCount(1).withStartPage(0));
    Assert.assertEquals(1, firstLinguisticsWorkflow.getItems().length);
    Assert.assertEquals(2, firstLinguisticsWorkflow.getTotalCount()); // The total, non-paged number of results should
                                                                      // be two

    // We should get the second linguistics (mediapackage2) workflow
    WorkflowSet secondLinguisticsWorkflow = service.getWorkflowInstances(new WorkflowQuery().withText("Linguistics")
            .withCount(1).withStartPage(1));
    Assert.assertEquals(1, secondLinguisticsWorkflow.getItems().length);
    Assert.assertEquals(2, secondLinguisticsWorkflow.getTotalCount()); // The total, non-paged number of results should
                                                                       // be two

    // cleanup the database
    for (WorkflowInstance instance : instances) {
      service.removeFromDatabase(instance.getId());
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetAllWorkflowInstances() throws Exception {
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(new WorkflowQuery()).size());

    WorkflowInstance instance1 = service.start(workingDefinition, mediapackage1, null);
    WorkflowInstance instance2 = service.start(workingDefinition, mediapackage2, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance1.getId()).getState().equals(WorkflowState.SUCCEEDED)
            || !service.getWorkflowById(instance2.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflows to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery());
    Assert.assertEquals(2, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance1.getId());
    service.removeFromDatabase(instance2.getId());

    // And ensure that it's really gone
    try {
      service.getWorkflowById(instance1.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    try {
      service.getWorkflowById(instance2.getId());
      Assert.fail();
    } catch (NotFoundException e) {
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testFailingOperationWithErrorHandler() throws Exception {
    WorkflowInstance instance = service.start(failingDefinitionWithErrorHandler, mediapackage1, null);
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.FAILED)) {
      System.out.println("Waiting for workflow to fail... current state is "
              + service.getWorkflowById(instance.getId()).getState());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    Assert.assertEquals(WorkflowState.FAILED, service.getWorkflowById(instance.getId()).getState());
    // The second operation should have failed
    Assert.assertEquals(OperationState.FAILED, service.getWorkflowById(instance.getId()).getOperations().get(1)
            .getState());

    // Make sure the error handler has been added
    Assert.assertEquals(4, instance.getOperations().size());
    Assert.assertEquals("op1", instance.getOperations().get(0).getId());
    Assert.assertEquals("op3", instance.getOperations().get(1).getId());
    Assert.assertEquals("op1", instance.getOperations().get(2).getId());
    Assert.assertEquals("op2", instance.getOperations().get(3).getId());

    // cleanup the database
    service.removeFromDatabase(instance.getId());
  }

  @Test
  public void testFailingOperationWithoutErrorHandler() throws Exception {
    WorkflowInstance instance = service.start(failingDefinitionWithoutErrorHandler, mediapackage1, null);
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.FAILED)) {
      System.out.println("Waiting for workflow to fail... current state is "
              + service.getWorkflowById(instance.getId()).getState());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    Assert.assertEquals(WorkflowState.FAILED, service.getWorkflowById(instance.getId()).getState());

    // cleanup the database
    service.removeFromDatabase(instance.getId());
  }

  /**
   * Starts 100 concurrent workflows to test DB deadlocking. This takes a while, so this test is ignored by default.
   * 
   * @throws Exception
   */
  @Test
  @Ignore
  public void testManyConcurrentWorkflows() throws Exception {
    Assert.assertEquals(0, service.countWorkflowInstances());
    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();
    for (int i = 0; i < 100; i++) {
      MediaPackage mp = i % 2 == 0 ? mediapackage1 : mediapackage2;
      instances.add(service.start(workingDefinition, mp, null));
    }

    // Give the workflows a chance to finish before looping
    Thread.sleep(5000);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    for (WorkflowInstance instance : instances) {
      while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
        System.out.println("Waiting for one of many workflows to complete...");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      }
    }

    Assert.assertEquals(100, service.countWorkflowInstances());
  }

  class SucceedingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
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
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(workflowInstance.getMediaPackage(),
              Action.CONTINUE);
    }
  }

  class FailingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
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
      throw new WorkflowOperationException("this operation handler always fails.  that's the point.");
    }
  }
}
