/**
 *  Copyright 2010 The Regents of the University of California
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

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * Tests the JPA implementation of the workflow service's DAO
 */
public class WorkflowServiceJpaTest {
  WorkflowServiceImplDaoJpaImpl dao;
  
  @Before
  public void setup() throws Exception {
    // Create a database and a connection pool
    ComboPooledDataSource pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis() + ";LOCK_MODE=1;MVCC=TRUE");
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    dao = new WorkflowServiceImplDaoJpaImpl();
    dao.setPersistenceProvider(new PersistenceProvider());
    dao.setPersistenceProperties(props);
    dao.activate(null);
  }
  
  @Test
  public void testFindById() throws Exception {
    String id = "first_workflow";
    String title = "first_workflow_title";
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    workflow.setId(id);
    workflow.setTitle(title);
    dao.update(workflow);
    
    WorkflowInstance workflowFromDb = dao.getWorkflowById(id);
    Assert.assertEquals(title, workflowFromDb.getTitle());
  }
  
}
