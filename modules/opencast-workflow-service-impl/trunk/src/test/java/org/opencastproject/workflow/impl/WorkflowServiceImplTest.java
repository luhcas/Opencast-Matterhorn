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
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WorkflowServiceImplTest {

  /** The solr root directory */
  private static final String storageRoot = "target" + File.separator + "workflow-test-db";

  private static WorkflowServiceImpl service = null;
  private static WorkflowDefinition definition1 = null;
  private static MediaPackage mediapackage1 = null;
  private static WorkflowOperationHandler operationHandler = null;
  
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
    service = new WorkflowServiceImpl(storageRoot) {
      protected Set<HandlerRegistration> getRegisteredHandlers() {
        Set<HandlerRegistration> set = new HashSet<HandlerRegistration>();
        set.add(new HandlerRegistration("op1", operationHandler));
        set.add(new HandlerRegistration("op2", operationHandler));
        return set;
      }
    };
    service.setDao(new WorkflowServiceImplDaoDerbyImpl());
    service.activate(null);

    try {
      definition1 = WorkflowBuilder.getInstance().parseWorkflowDefinition(
              WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-1.xml"));
      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      mediapackage1 = mediaPackageBuilder.loadFromManifest(
              WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml"));
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
  public void testGetWorkflowOperationById() {
    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete.  Let the workflow finish before verifying state in the DB
    while( ! instance.getState().equals(State.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try { Thread.sleep(1000); } catch(InterruptedException e) {}
    }
    
    // verify that we can retrieve the workflow instance from the service by its ID
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    Assert.assertNotNull(instanceFromDb);
    MediaPackage mediapackageFromDb = instanceFromDb.getSourceMediaPackage();
    Assert.assertNotNull(mediapackageFromDb);
    Assert.assertEquals(mediapackage1.getIdentifier().toString(),
            mediapackageFromDb.getIdentifier().toString());
    Assert.assertEquals(1, service.countWorkflowInstances());

    // cleanup the database
    service.removeFromDatabase(instance.getId());
    
    // And ensure that it's really gone
    Assert.assertNull(service.getWorkflowById(instance.getId()));
    Assert.assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowOperationByMediaPackageId() {
    // Ensure that the database doesn't have a workflow instance with this media package
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowsByMediaPackage(mediapackage1.getIdentifier().toString()).size());

    WorkflowInstance instance = service.start(definition1, mediapackage1, null);

    // Even the sample workflows take time to complete.  Let the workflow finish before verifying state in the DB
    while( ! instance.getState().equals(State.SUCCEEDED)) {
      System.out.println("Waiting for workflow to complete...");
      try { Thread.sleep(1000); } catch(InterruptedException e) {}
    }

    WorkflowSet workflowsInDb = service.getWorkflowsByMediaPackage(mediapackage1.getIdentifier().toString());
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
