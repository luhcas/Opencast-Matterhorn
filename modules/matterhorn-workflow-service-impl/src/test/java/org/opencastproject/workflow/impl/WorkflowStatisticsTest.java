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

import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowStatistics;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport.OperationReport;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WorkflowStatisticsTest {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowStatisticsTest.class);
  
  @Test
  public void testStatisticsMarshalling() throws Exception {
    WorkflowStatistics stats = new WorkflowStatistics();
    stats.setFailed(100);
    stats.setInstantiated(20);

    OperationReport op1 = new OperationReport();
    op1.setId("compose");
    op1.setInstantiated(10);
    op1.setFailing(1);

    List<OperationReport> ops1 = new ArrayList<WorkflowStatistics.WorkflowDefinitionReport.OperationReport>();
    ops1.add(op1);
    
    WorkflowDefinitionReport def1 = new WorkflowDefinitionReport();
    def1.setFailed(40);
    def1.setInstantiated(10);
    def1.setOperations(ops1);
    def1.setId("def1");
    def1.setOperations(ops1);
    
    WorkflowDefinitionReport def2 = new WorkflowDefinitionReport();
    def1.setFailed(60);
    def1.setInstantiated(10);
    
    List<WorkflowDefinitionReport> reports = new ArrayList<WorkflowDefinitionReport>();
    reports.add(def1);
    reports.add(def2);
    stats.setDefinitions(reports);
    
    logger.info(WorkflowBuilder.getInstance().toXml(stats));
  }
}
