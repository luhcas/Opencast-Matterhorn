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
import org.opencastproject.workflow.api.WorkflowOperationResultBuilder;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class WorkflowServiceImplTest {

  /** The solr root directory */
  private String solrRoot = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workflows";

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition definition1 = null;
  private MediaPackage mediapackage1 = null;
  private WorkflowOperationHandler operationHandler = null;
  
  @Before
  public void setup() throws Exception {
    service = new WorkflowServiceImpl(solrRoot) {
      @Override
      protected WorkflowOperationHandler selectOperationHandler(WorkflowOperationDefinition operation) {
        return operationHandler;
      }
    };
    service.activate(null);
    definition1 = WorkflowBuilder.getInstance().parseWorkflowDefinition(getClass().getResourceAsStream("/workflow-definition-1.xml"));
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    builder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
    mediapackage1 = builder.loadFromManifest(
            getClass().getResourceAsStream("/mediapackage-1.xml"));
  }

  @After
  public void teardown() throws Exception {
    service = null;
    operationHandler = null;
    FileUtils.deleteDirectory(new File(solrRoot));
  }
  
  @Test
  public void testGetWorkflowOperationById() throws Exception {
    operationHandler = new TestWorkflowOperationHandler(new String[] {"op1", "op2"}, false);
    WorkflowInstance instance = service.start(definition1, mediapackage1, null);
    // verify that we can retrieve the workflow instance from the service by its ID
    WorkflowInstance instanceFromDb = service.getWorkflowInstance(instance.getId());

    System.out.println("\n\nOriginal: " + WorkflowBuilder.getInstance().toXml(instance));
    System.out.println("\n\nFrom DB: " + WorkflowBuilder.getInstance().toXml(instanceFromDb));
    System.out.println("\n\n");
    
    Assert.assertNotNull(instanceFromDb);
    MediaPackage mediapackageFromDb = instanceFromDb.getSourceMediaPackage();
    Assert.assertNotNull(mediapackageFromDb);
    Assert.assertEquals(mediapackage1.getIdentifier().toString(),
            mediapackageFromDb.getIdentifier().toString());
  }

  @Test
  public void testGetWorkflowOperationByMediaPackageId() {
    operationHandler = new TestWorkflowOperationHandler(new String[] {"op1", "op2"}, false);
    service.start(definition1, mediapackage1, null);
    // TODO verify that we can retrieve the workflow instance from the service by its source mediapackage ID
//    Assert.assertEquals(1,
//            service.getWorkflowsByMediaPackage(mediapackage1.getIdentifier().toString()).getItems().length);
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
      return WorkflowOperationResultBuilder.build(mediapackage1, null, wait);
    }
    
  }
}
