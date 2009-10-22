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
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class WorkflowServiceImplTest {

  /** The solr root directory */
  private static final String solrRoot = "target" + File.separator + "workflow-test-db";

  private static WorkflowServiceImpl service = null;
  private static WorkflowDefinition definition1 = null;
  private static MediaPackage mediapackage1 = null;
  private static MediaPackage mediapackage2 = null;
  private static WorkflowOperationHandler operationHandler = null;
  
  @BeforeClass
  public static void setup() {
    // always start with a fresh solr root directory
    try {
      FileUtils.deleteDirectory(new File(solrRoot));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    // instantiate a service implementation, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl(solrRoot) {
      @Override
      protected WorkflowOperationHandler selectOperationHandler(WorkflowOperationDefinition operation) {
        return operationHandler;
      }
      @Override
      public WorkflowDefinition getWorkflowDefinitionByName(String name) {
        return definition1.getTitle().equals(name) ? definition1 : null;
      }
    };
    
    // activate the service and build some objects for it to work with
    service.activate(null);
    try {
      definition1 = WorkflowBuilder.getInstance().parseWorkflowDefinition(
              WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-1.xml"));
      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      mediapackage1 = mediaPackageBuilder.loadFromManifest(
              WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml"));
      mediapackage2 = mediaPackageBuilder.loadFromManifest(
              WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-2.xml"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @AfterClass
  public static void teardown() {
    // TODO For some reason, deactivating solr throws exceptions
    // service.deactivate();
    service = null;
    operationHandler = null;
  }
    
  @Test
  public void testGetWorkflowOperationById() {
    operationHandler = new TestWorkflowOperationHandler(new String[] {"op1", "op2"}, false);
    WorkflowInstance instance = service.start(definition1, mediapackage1, null);
    // verify that we can retrieve the workflow instance from the service by its ID
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());

    Assert.assertNotNull(instanceFromDb);
    MediaPackage mediapackageFromDb = instanceFromDb.getSourceMediaPackage();
    Assert.assertNotNull(mediapackageFromDb);
    Assert.assertEquals(mediapackage1.getIdentifier().toString(),
            mediapackageFromDb.getIdentifier().toString());
    // cleanup for the next test
    service.removeFromDatabase(instance.getId());
  }

  // FIXME The removeFromDatabase() method is not working.  Combined with our inability to cleanly shut down solr,
  // we now have no way of clearing the search index between test runs.  So we'll need to use a mediapackage with a
  // different ID for each test.

  @Test
  public void testGetWorkflowOperationByMediaPackageId() {
    operationHandler = new TestWorkflowOperationHandler(new String[] {"op1", "op2"}, false);
    WorkflowInstance instance = service.start(definition1, mediapackage2, null);
    Assert.assertEquals(1,
            service.getWorkflowsByMediaPackage(mediapackage2.getIdentifier().toString()).getItems().length);
    // cleanup for the next test instance
    service.removeFromDatabase(instance.getId());
  }

  class TestWorkflowOperationHandler implements WorkflowOperationHandler {
    String[] operationsToHandle;
    boolean wait;
    TestWorkflowOperationHandler(String[] operationsToHandle, boolean wait) {
      this.operationsToHandle = operationsToHandle;
      this.wait = wait;
    }
    public String[] getOperationsToHandle() {return operationsToHandle;}
    public WorkflowOperationResult run(WorkflowInstance workflowInstance) throws WorkflowOperationException {
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediapackage1, null, wait);
    }
    
  }
}
