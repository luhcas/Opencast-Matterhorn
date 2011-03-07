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
package org.opencastproject.workflow.handler;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DistributeWorkflowOperationHandlerTest {
  private DistributeWorkflowOperationHandler operationHandler;
  private ServiceRegistry serviceRegistry;
  private TestDistributionService service = null;

  private URI uriMP;
  private MediaPackage mp;

  @Before
  public void setup() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    uriMP = InspectWorkflowOperationHandler.class.getResource("/distribute_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    service = new TestDistributionService();
    serviceRegistry = new ServiceRegistryInMemoryImpl(service);
    service.serviceRegistry = serviceRegistry;

    // set up the handler
    operationHandler = new DistributeWorkflowOperationHandler();
    operationHandler.setDistributionService(service);
    operationHandler.setServiceRegistry(serviceRegistry);

  }

  @Test
  public void testDistribute() throws Exception {
    String sourceTags = "engage,atom,rss";
    String targetTags = "engage,publish";
    WorkflowInstance workflowInstance = getWorkflowInstance(sourceTags, targetTags);
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 3, result.getMediaPackage()
            .getTracks().length);
  }

  private WorkflowInstance getWorkflowInstance(String sourceTags, String targetTags) {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);

    operation.setConfiguration("source-tags", sourceTags);
    operation.setConfiguration("target-tags", targetTags);

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    return workflowInstance;
  }

  class TestDistributionService implements DistributionService, JobProducer {
    public static final String JOB_TYPE = "distribute";

    ServiceRegistry serviceRegistry = null;

    @Override
    public Job distribute(String mediaPackageId, MediaPackageElement element) throws DistributionException,
            MediaPackageException {
      try {
        return serviceRegistry.createJob("distribute", "distribute",
                Arrays.asList(new String[] { MediaPackageElementParser.getAsXml(element) }));
      } catch (ServiceRegistryException e) {
        throw new DistributionException(e);
      }
    }

    @Override
    public Job retract(String mediaPackageId) throws DistributionException {
      try {
        return serviceRegistry.createJob(JOB_TYPE, "retract");
      } catch (ServiceRegistryException e) {
        throw new DistributionException(e);
      }
    }

    @Override
    public String getJobType() {
      return JOB_TYPE;
    }

    @Override
    public long countJobs(Status status) throws ServiceRegistryException {
      return serviceRegistry.getJobs(JOB_TYPE, status).size();
    }

    @Override
    public boolean acceptJob(Job job) throws ServiceRegistryException {
      job.setPayload(job.getArguments().get(0));
      job.setStatus(Status.FINISHED);
      try {
        serviceRegistry.updateJob(job);
        return true;
      } catch (NotFoundException e) {
        // not possible
      }
      return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.opencastproject.job.api.JobProducer#isReadyToAccept(org.opencastproject.job.api.Job)
     */
    @Override
    public boolean isReadyToAccept(Job job) throws ServiceRegistryException {
      return true;
    }

  }

}
