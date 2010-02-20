/**
 *  Copyright 2009 The Regents of the University of California
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
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WorkflowServiceImplTest {

  /** The solr root directory */
  private static final String storageRoot = "target" + File.separator + "workflow-test-db";

  private static WorkflowServiceImpl service = null;
  private static WorkflowDefinition workingDefinition = null;
  private static WorkflowDefinition failingDefinitionWithoutErrorHandler = null;
  private static WorkflowDefinition failingDefinitionWithErrorHandler = null;
  private static MediaPackage mediapackage1 = null;
  private static MediaPackage mediapackage2 = null;
  private static WorkflowOperationHandler succeedingOperationHandler = null;
  private static WorkflowOperationHandler failingOperationHandler = null;
  private static JdbcConnectionPool cp = null;

  @BeforeClass
  public static void setup() {
    // always start with a fresh solr root directory
    try {
      FileUtils.deleteDirectory(new File(storageRoot));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    // create operation handlers for our workflows
    succeedingOperationHandler = new SucceedingWorkflowOperationHandler();
    failingOperationHandler = new FailingWorkflowOperationHandler();
    
    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      protected Set<HandlerRegistration> getRegisteredHandlers() {
        Set<HandlerRegistration> set = new HashSet<HandlerRegistration>();
        set.add(new HandlerRegistration("op1", succeedingOperationHandler));
        set.add(new HandlerRegistration("op2", succeedingOperationHandler));
        set.add(new HandlerRegistration("op3", failingOperationHandler));
        return set;
      }
    };
    String randomId = UUID.randomUUID().toString();
    cp = JdbcConnectionPool.create("jdbc:h2:target/" + randomId + ";LOCK_MODE=1;MVCC=TRUE", "sa", "sa");
    WorkflowServiceImplDaoDatasourceImpl dao = new WorkflowServiceImplDaoDatasourceImpl(cp);
    dao.activate(null);
    service.setDao(dao);
    service.activate(null);

    try {
      workingDefinition = WorkflowBuilder.getInstance().parseWorkflowDefinition(
              WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-1.xml"));
      failingDefinitionWithoutErrorHandler = WorkflowBuilder.getInstance().parseWorkflowDefinition(
              WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-2.xml"));
      failingDefinitionWithErrorHandler = WorkflowBuilder.getInstance().parseWorkflowDefinition(
              WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-3.xml"));
      service.registerWorkflowDefinition(workingDefinition);
      service.registerWorkflowDefinition(failingDefinitionWithoutErrorHandler);
      service.registerWorkflowDefinition(failingDefinitionWithErrorHandler);
      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      mediapackage1 = mediaPackageBuilder.loadFromXml(WorkflowServiceImplTest.class
              .getResourceAsStream("/mediapackage-1.xml"));
      mediapackage2 = mediaPackageBuilder.loadFromXml(WorkflowServiceImplTest.class
              .getResourceAsStream("/mediapackage-2.xml"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @AfterClass
  public static void teardown() {
    System.out.println("All tests finished... tearing down...");
    service.deactivate();
    service = null;
    succeedingOperationHandler = null;
  }

  @Test
  public void testGetWorkflowInstanceById() {
    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
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
    Assert.assertEquals(1, service.countWorkflowInstances());

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowByMediaPackageId() {
    // Ensure that the database doesn't have a workflow instance with this media package
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(
            service.newWorkflowQuery().withMediaPackage(mediapackage1.getIdentifier().toString())).size());

    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    WorkflowSet workflowsInDb = service.getWorkflowInstances(service.newWorkflowQuery().withMediaPackage(
            mediapackage1.getIdentifier().toString()));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowByEpisodeId() {
    String episodeId = mediapackage1.getIdentifier().toString();

    // Ensure that the database doesn't have a workflow instance with this episode
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(service.newWorkflowQuery().withEpisode(episodeId)).size());

    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    WorkflowSet workflowsInDb = service.getWorkflowInstances(service.newWorkflowQuery().withEpisode(episodeId));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }
  
  // TODO This test requires a hold state
  @Test
  @Ignore
  public void testGetWorkflowByCurrentOperation() {
    // Ensure that the database doesn't have a workflow instance in the "op2" operation
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(service.newWorkflowQuery().withCurrentOperation("op2")).size());

    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    WorkflowSet workflowsInDb = service.getWorkflowInstances(service.newWorkflowQuery().withCurrentOperation("op2"));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowByText() {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(
            service.newWorkflowQuery().withText("Climate").withCount(100).withStartPage(0)).size());

    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    WorkflowSet workflowsInDb = service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withCount(100).withStartPage(0));
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testPagedGetWorkflowByText() {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withCount(100).withStartPage(0)).size());

    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();
    instances.add(service.start(workingDefinition, mediapackage1, null));
    instances.add(service.start(workingDefinition, mediapackage1, null));
    instances.add(service.start(workingDefinition, mediapackage2, null));
    instances.add(service.start(workingDefinition, mediapackage2, null));
    instances.add(service.start(workingDefinition, mediapackage1, null));

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

    // We should get the first two workflows
    WorkflowSet firstTwoWorkflows = service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withCount(2).withStartPage(0));
    Assert.assertEquals(2, firstTwoWorkflows.getItems().length);
    Assert.assertEquals(3, firstTwoWorkflows.getTotalCount()); // The total, non-paged number of results should be three

    // We should get the last workflow
    WorkflowSet lastWorkflow = service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withCount(1).withStartPage(2));
    Assert.assertEquals(1, lastWorkflow.getItems().length);
    Assert.assertEquals(3, lastWorkflow.getTotalCount()); // The total, non-paged number of results should be three

    // We should get the first linguistics (mediapackage2) workflow
    WorkflowSet firstLinguisticsWorkflow = service.getWorkflowInstances(service.newWorkflowQuery().withText("Linguistics").withCount(1).withStartPage(0));
    Assert.assertEquals(1, firstLinguisticsWorkflow.getItems().length);
    Assert.assertEquals(2, firstLinguisticsWorkflow.getTotalCount()); // The total, non-paged number of results should be two

    // We should get the second linguistics (mediapackage2) workflow
    WorkflowSet secondLinguisticsWorkflow = service.getWorkflowInstances(service.newWorkflowQuery().withText("Linguistics").withCount(1).withStartPage(1));
    Assert.assertEquals(1, secondLinguisticsWorkflow.getItems().length);
    Assert.assertEquals(2, secondLinguisticsWorkflow.getTotalCount()); // The total, non-paged number of results should be two

    // cleanup the database
    for (WorkflowInstance instance : instances) {
      service.removeFromDatabase(instance.getId());
    }
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowInstanceByMetadataCatalog() {
    WorkflowQuery q = service.newWorkflowQuery().withElement("catalog", "metadata/dublincore", true);
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(q).size());

    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    // reuse our original query, which returned zero before we started a new workflow
    WorkflowSet workflowsInDb = service.getWorkflowInstances(q);
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowInstanceByMissingMetadataCatalog() {
    WorkflowQuery q = service.newWorkflowQuery().withElement("catalog", "metadata/dublincore", false);
    
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(q).size());

    WorkflowInstance instance = service.start(workingDefinition, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    WorkflowQuery queryUnmatched = service.newWorkflowQuery().withElement("catalog", "something/nonexistent", false);
    WorkflowSet workflowsInDb = service.getWorkflowInstances(queryUnmatched);
    Assert.assertEquals(1, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }
  
  @Test
  public void testGetAllWorkflowInstances() {
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(service.newWorkflowQuery()).size());

    WorkflowInstance instance1 = service.start(workingDefinition, mediapackage1, null);
    WorkflowInstance instance2 = service.start(workingDefinition, mediapackage2, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (! service.getWorkflowById(instance1.getId()).getState().equals(WorkflowState.SUCCEEDED) ||
            ! service.getWorkflowById(instance2.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
      System.out.println("Waiting for workflows to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    WorkflowSet workflowsInDb = service.getWorkflowInstances(service.newWorkflowQuery());
    Assert.assertEquals(2, workflowsInDb.getItems().length);

    // cleanup the database
    service.removeFromDatabase(instance1.getId());
    service.removeFromDatabase(instance2.getId());

    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance1.getId()));
    Assert.assertNull(service.getWorkflowById(instance2.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testFailingOperationWithErrorHandler() {
    WorkflowInstance instance = service.start(failingDefinitionWithErrorHandler, mediapackage1, null);
    while (! service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.FAILED)) {
      System.out.println("Waiting for workflow to fail... current state is " + service.getWorkflowById(instance.getId()).getState());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    Assert.assertEquals(WorkflowState.FAILED, service.getWorkflowById(instance.getId()).getState());
    // The second operation should have failed
    Assert.assertEquals(OperationState.FAILED, service.getWorkflowById(instance.getId()).getOperations().get(1).getState());
    
    // cleanup the database
    service.removeFromDatabase(instance.getId());
  }
  
  @Test
  public void testFailingOperationWithoutErrorHandler() {
    WorkflowInstance instance = service.start(failingDefinitionWithoutErrorHandler, mediapackage1, null);
    while (! service.getWorkflowById(instance.getId()).getState().equals(WorkflowState.FAILED)) {
      System.out.println("Waiting for workflow to fail... current state is " + service.getWorkflowById(instance.getId()).getState());
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
   * Starts 100 concurrent workflows to test DB deadlocking.  This takes a while, so this test is ignored by default.
   * @throws Exception
   */
  @Test
  @Ignore
  public void testManyConcurrentWorkflows() throws Exception {
    Assert.assertEquals(0, service.countWorkflowInstances());
    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();
    for(int i=0; i<100; i++) {
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

  static class SucceedingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediapackage1, Action.CONTINUE);
    }
  }

  static class FailingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      throw new WorkflowOperationException("this operation handler always fails.  that's the point.");
    }
  }
}
