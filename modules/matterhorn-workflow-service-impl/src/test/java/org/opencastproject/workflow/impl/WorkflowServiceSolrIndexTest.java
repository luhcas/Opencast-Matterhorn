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

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.io.FileUtils;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for the implementation at {@link WorkflowServiceDaoSolrImpl}.
 */
public class WorkflowServiceSolrIndexTest {

  private WorkflowServiceSolrIndex dao = null;

  @Before
  public void setup() throws Exception {
    // Create a job with a workflow as its payload
    List<Job> jobs = new ArrayList<Job>();
    JaxbJob job = new JaxbJob();
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    workflow.setId(123);
    workflow.setState(WorkflowState.INSTANTIATED);
    workflow.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    job.setPayload(WorkflowParser.toXml(workflow));
    jobs.add(job);

    // Mock up the service registry to return the job
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.count(WorkflowService.JOB_TYPE, null)).andReturn(new Long(1));
    EasyMock.expect(serviceRegistry.getJobs(WorkflowService.JOB_TYPE, null)).andReturn(jobs);
    EasyMock.expect(serviceRegistry.getJob(123)).andReturn(job);
    EasyMock.replay(serviceRegistry);

    // Now create the dao
    dao = new WorkflowServiceSolrIndex();
    dao.solrRoot = PathSupport.concat("target", Long.toString(System.currentTimeMillis()));
    dao.setServiceRegistry(serviceRegistry);
    dao.activate();
  }

  @After
  public void teardown() throws Exception {
    dao.deactivate();
    FileUtils.deleteDirectory(new File(dao.solrRoot));
    dao = null;
  }

  /**
   * Tests whether a simple query is built correctly
   */
  @Test
  public void testBuildSimpleQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withMediaPackage("123").withSeriesId("series1");
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "mp:123 AND seriesid:series1";
    assertEquals(expected, solrQuery);
  }

  /**
   * Tests whether the query is built properly, using OR rather than AND, when supplying multiple inclusive states
   */
  @Test
  public void testBuildMultiStateQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withSeriesId("series1").withState(WorkflowState.RUNNING)
            .withState(WorkflowState.PAUSED);
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "seriesid:series1 AND (state:RUNNING OR state:PAUSED)";
    assertEquals(expected, solrQuery);
  }

  /**
   * Tests whether the query is built using AND rather than OR when supplying multiple excluded states
   */
  @Test
  public void testBuildNegativeStatesQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withSeriesId("series1").withoutState(WorkflowState.RUNNING)
            .withoutState(WorkflowState.PAUSED);
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "seriesid:series1 AND (-state:RUNNING AND -state:PAUSED AND *:*)";
    assertEquals(expected, solrQuery);
  }

  /**
   * Tests whether the query is built using *:* when supplying a single excluded state
   */
  @Test
  public void testBuildNegativeStateQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withSeriesId("series1").withoutState(WorkflowState.RUNNING);
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "seriesid:series1 AND (-state:RUNNING AND *:*)";
    assertEquals(expected, solrQuery);
  }

}
