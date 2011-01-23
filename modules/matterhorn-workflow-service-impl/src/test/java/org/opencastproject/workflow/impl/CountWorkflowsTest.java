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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowStateListener;
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

public class CountWorkflowsTest {

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition def = null;
  private WorkflowServiceSolrIndex dao = null;
  private MediaPackage mp = null;
  private ResumableWorkflowOperationHandler holdingOperationHandler;
  private ServiceRegistryInMemoryImpl serviceRegistry = null;

  private File sRoot = null;

  protected static final String getStorageRoot() {
    return "." + File.separator + "target" + File.separator + System.currentTimeMillis();
  }

  @Before
  public void setup() throws Exception {
    // always start with a fresh solr root directory
    sRoot = new File(getStorageRoot());
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
    holdingOperationHandler = new ResumableTestWorkflowOperationHandler();
    handlerRegistrations.add(new HandlerRegistration("op1", holdingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", new ContinuingWorkflowOperationHandler()));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };

    serviceRegistry = new ServiceRegistryInMemoryImpl();

    dao = new WorkflowServiceSolrIndex();
    dao.setServiceRegistry(serviceRegistry);
    dao.solrRoot = sRoot + File.separator + "solr";
    dao.activate();
    service.setDao(dao);
    service.activate(null);
    service.setServiceRegistry(serviceRegistry);

    is = CountWorkflowsTest.class.getResourceAsStream("/workflow-definition-holdstate.xml");
    def = WorkflowParser.parseWorkflowDefinition(is);
    IOUtils.closeQuietly(is);
    service.registerWorkflowDefinition(def);

    serviceRegistry.registerService(service);

  }

  @After
  public void teardown() throws Exception {
    dao.deactivate();
  }

  @Test
  public void testHoldAndResume() throws Exception {
    // Wait for all workflows to be in paused state
    WorkflowStateListener listener = new WorkflowStateListener(WorkflowState.PAUSED);
    service.addWorkflowListener(listener);

    Map<String, String> initialProps = new HashMap<String, String>();
    initialProps.put("testproperty", "foo");
    WorkflowInstance workflow1 = null;
    synchronized (listener) {
      workflow1 = service.start(def, mp, initialProps);
      listener.wait(10000);
      service.start(def, mp, initialProps);
      listener.wait(10000);
    }
    service.removeWorkflowListener(listener);

    // Test for two paused workflows in "op1"
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(2, service.countWorkflowInstances(WorkflowState.PAUSED, null));
    assertEquals(2, service.countWorkflowInstances(null, "op1"));
    assertEquals(2, service.countWorkflowInstances(WorkflowState.PAUSED, "op1"));
    assertEquals(0, service.countWorkflowInstances(WorkflowState.SUCCEEDED, null));
    assertEquals(0, service.countWorkflowInstances(null, "op2"));
    assertEquals(0, service.countWorkflowInstances(WorkflowState.SUCCEEDED, "op1"));

    // Continue one of the two workflows, waiting for success
    listener = new WorkflowStateListener(WorkflowState.SUCCEEDED);
    service.addWorkflowListener(listener);
    synchronized (listener) {
      service.resume(workflow1.getId());
      listener.wait(10000);
    }
    service.removeWorkflowListener(listener);

    // Make sure one workflow is still on hold, the other is finished.
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(1, service.countWorkflowInstances(WorkflowState.PAUSED, null));
    assertEquals(1, service.countWorkflowInstances(WorkflowState.PAUSED, "op1"));
    assertEquals(1, service.countWorkflowInstances(WorkflowState.SUCCEEDED, null));
  }

  /**
   * Test implementatio for a workflow operation handler that will go on hold, and continue when resume() is called.
   */
  class ContinuingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
      return createResult(Action.CONTINUE);
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
