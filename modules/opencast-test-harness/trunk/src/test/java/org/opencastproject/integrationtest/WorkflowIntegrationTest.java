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
package org.opencastproject.integrationtest;

import org.opencastproject.conductor.api.ConductorService;
import org.opencastproject.integrationtest.AbstractIntegrationTest;
import org.opencastproject.workflow.api.WorkflowService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the workflow service in a running osgi container
 *
 */
public class WorkflowIntegrationTest extends AbstractIntegrationTest {
  private WorkflowService workflowService;

  @Before
  public void setup() throws Exception {
    workflowService = retrieveService(WorkflowService.class);
    retrieveService(ConductorService.class); // Ensure the conductor launches so we have some workflow definitions
  }

  @Test
  public void testWorkflowService() throws Exception {
    Assert.assertTrue(workflowService.listAvailableWorkflowDefinitions().size() > 0);
  }

}
