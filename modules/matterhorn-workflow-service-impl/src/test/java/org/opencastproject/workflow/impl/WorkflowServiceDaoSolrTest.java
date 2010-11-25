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

import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the implementation at {@link WorkflowServiceDaoSolrImpl}.
 */
public class WorkflowServiceDaoSolrTest {

  private WorkflowServiceDaoSolrImpl dao = null;

  @Before
  public void setup() throws Exception {
    dao = new WorkflowServiceDaoSolrImpl();
  }

  @After
  public void teardown() throws Exception {
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
  public void testBuildNegativeStateQuery() throws Exception {
    WorkflowQuery q = new WorkflowQuery().withSeriesId("series1").withoutState(WorkflowState.RUNNING)
            .withoutState(WorkflowState.PAUSED);
    String solrQuery = dao.buildSolrQueryString(q);
    String expected = "seriesid:series1 AND (-state:RUNNING AND -state:PAUSED AND *:*)";
    assertEquals(expected, solrQuery);
  }

}
