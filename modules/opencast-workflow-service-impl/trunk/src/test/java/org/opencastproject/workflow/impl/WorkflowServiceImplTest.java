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

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
  private static WorkflowDefinition definition1 = null;
  private static MediaPackage mediapackage1 = null;
  private static MediaPackage mediapackage2 = null;
  private static WorkflowOperationHandler operationHandler = null;
  private static JdbcConnectionPool cp = null;

  @BeforeClass
  public static void setup() {
    // always start with a fresh solr root directory
    try {
      FileUtils.deleteDirectory(new File(storageRoot));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    // create an operation handler for our workflows
    operationHandler = new TestWorkflowOperationHandler();

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      protected Set<HandlerRegistration> getRegisteredHandlers() {
        Set<HandlerRegistration> set = new HashSet<HandlerRegistration>();
        set.add(new HandlerRegistration("op1", operationHandler));
        set.add(new HandlerRegistration("op2", operationHandler));
        return set;
      }
    };
    String randomId = UUID.randomUUID().toString();
    cp = JdbcConnectionPool.create("jdbc:h2:target/" + randomId + ";LOCK_MODE=1", "sa", "sa");
    WorkflowServiceImplDaoDatasourceImpl dao = new WorkflowServiceImplDaoDatasourceImpl(cp);
    dao.activate(null);
    service.setDao(dao);
    service.activate(null);

    try {
      definition1 = WorkflowBuilder.getInstance().parseWorkflowDefinition(
              WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-1.xml"));
      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      mediapackage1 = mediaPackageBuilder.loadFromManifest(WorkflowServiceImplTest.class
              .getResourceAsStream("/mediapackage-1.xml"));
      mediapackage2 = mediaPackageBuilder.loadFromManifest(WorkflowServiceImplTest.class
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
    operationHandler = null;
  }

  @Test
  public void testGetWorkflowInstanceById() {
    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!instance.getState().equals(State.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    // verify that we can retrieve the workflow instance from the service by its ID
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    Assert.assertNotNull(instanceFromDb);
    MediaPackage mediapackageFromDb = instanceFromDb.getSourceMediaPackage();
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

    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!instance.getState().equals(State.SUCCEEDED)) {
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
    Catalog[] dcCatalogs = mediapackage1.getCatalogs(DublinCoreCatalog.FLAVOR,
            MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if (dcCatalogs.length == 0)
      Assert.fail("Unable to find a dublin core catalog in the test media package");
    String episodeId = ((DublinCoreCatalog) dcCatalogs[0]).getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER,
            DublinCoreCatalog.LANGUAGE_UNDEFINED);

    // Ensure that the database doesn't have a workflow instance with this episode
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(service.newWorkflowQuery().withEpisode(episodeId)).size());

    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!instance.getState().equals(State.SUCCEEDED)) {
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

  @Test
  public void testGetWorkflowByText() {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(
            service.newWorkflowQuery().withText("Climate").withLimit(100).withOffset(0)).size());

    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!instance.getState().equals(State.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    WorkflowSet workflowsInDb = service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withLimit(100).withOffset(0));
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
    Assert.assertEquals(0, service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withLimit(100).withOffset(0)).size());

    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();
    instances.add(service.start(definition1, mediapackage1, null));
    instances.add(service.start(definition1, mediapackage1, null));
    instances.add(service.start(definition1, mediapackage2, null));
    instances.add(service.start(definition1, mediapackage2, null));
    instances.add(service.start(definition1, mediapackage1, null));

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    for (WorkflowInstance instance : instances) {
      while (!instance.getState().equals(State.SUCCEEDED)) {
        System.out.println("Waiting for workflow to complete...");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
      }
    }

    // We should get the first two workflows
    WorkflowSet firstTwoWorkflows = service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withLimit(2).withOffset(0));
    Assert.assertEquals(2, firstTwoWorkflows.getItems().length);

    // We should get the last workflow
    WorkflowSet lastWorkflow = service.getWorkflowInstances(service.newWorkflowQuery().withText("Climate").withLimit(1).withOffset(2));
    Assert.assertEquals(1, lastWorkflow.getItems().length);

    // We should get the first linguistics (mediapackage2) workflow
    WorkflowSet firstLinguisticsWorkflow = service.getWorkflowInstances(service.newWorkflowQuery().withText("Linguistics").withLimit(1).withOffset(0));
    Assert.assertEquals(1, firstLinguisticsWorkflow.getItems().length);

    // We should get the second linguistics (mediapackage2) workflow
    WorkflowSet secondLinguisticsWorkflow = service.getWorkflowInstances(service.newWorkflowQuery().withText("Linguistics").withLimit(1).withOffset(1));
    Assert.assertEquals(1, secondLinguisticsWorkflow.getItems().length);

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

    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!instance.getState().equals(State.SUCCEEDED)) {
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

    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete. Let the workflow finish before verifying state in the DB
    while (!instance.getState().equals(State.SUCCEEDED)) {
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

  static class TestWorkflowOperationHandler implements WorkflowOperationHandler {
    public WorkflowOperationResult run(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediapackage1, null, false);
    }
  }
}
