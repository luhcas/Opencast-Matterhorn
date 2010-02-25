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

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;

import org.osgi.service.component.ComponentContext;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

/**
 * JPA implementation of the workflow service persistence layer.
 */
public class WorkflowServiceImplDaoJpaImpl implements WorkflowServiceImplDao {

  /** The DataSource to provide database connections */
  protected DataSource dataSource;
  
  /**
   * @param dataSource the dataSource to set
   */
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /**
   * @param persistenceProvider the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /** The entity manager used for persisting entities. */
  protected EntityManager em = null;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;
  
  @SuppressWarnings("unchecked")
  public void activate(ComponentContext cc) {
    Map factoryProps = new HashMap();
    factoryProps.put("javax.persistence.nonJtaDataSource", dataSource);
    emf = persistenceProvider.createEntityManagerFactory("workflow", factoryProps);
    em = emf.createEntityManager();
  }

  public void destroy() {
    em.close();
    emf.close();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() {
    Query query = em.createQuery("SELECT COUNT(wf) FROM Workflow w");
    Number countResult = (Number) query.getSingleResult();
    return countResult.longValue();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowById(java.lang.String)
   */
  @Override
  public WorkflowInstance getWorkflowById(String workflowId) {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) {
    long start = System.currentTimeMillis();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    Metamodel mm = em.getMetamodel();
    
    return null;
    
//    CriteriaQuery<WorkflowInstanceJPAEntity> jpaQuery = cb.createQuery(WorkflowInstanceJPAEntity.class);
//    Root<WorkflowInstanceJPAEntity> root = jpaQuery.from(WorkflowInstanceJPAEntity.class);
//    EntityType<WorkflowInstanceJPAEntity> WorkflowInstanceJPAEntity_ = root.getModel();;
//    if(query.getCurrentOperation() != null) {
//      Predicate condition = cb.equal(root.get("currentOperation"), query.getCurrentOperation());
//      jpaQuery.where(condition);
//    }
//    if(query.getEpisode() != null) {
//      Predicate condition = cb.equal(root.get("episode"), query.getCurrentOperation());
//      jpaQuery.where(condition);
//    }

    // TODO Add the rest of the criteria
    
//    TypedQuery<WorkflowInstanceJPAEntity> typedQuery = em.createQuery(jpaQuery);
//    List<WorkflowInstanceJPAEntity> list = typedQuery.getResultList();
//    long searchTime = System.currentTimeMillis() - start;
//    WorkflowSetImpl set = new WorkflowSetImpl();
    
    // TODO Enable paging

//    set.setCount(Math.min(query.getCount(), totalCount));
//    set.setStartPage(query.getStartPage());
//    set.setSearchTime(searchTime);
//    set.setTotalCount(totalCount);
//    for(WorkflowInstance workflow : list) {
//      set.addItem(workflow);
//    }
//    return set;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#remove(java.lang.String)
   */
  @Override
  public void remove(String id) {
    WorkflowInstance workflow = getWorkflowById(id);
    em.getTransaction().begin();
    em.remove(workflow);
    em.getTransaction().commit();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(WorkflowInstance instance) {
    WorkflowInstance existingWorkflow = getWorkflowById(instance.getId());
    em.getTransaction().begin();
    if(existingWorkflow == null) {
//      em.persist(new WorkflowInstanceJPAEntity((WorkflowInstanceImpl)instance));
//    } else {
//      em.merge(new WorkflowInstanceJPAEntity((WorkflowInstanceImpl)instance));
    }
    em.getTransaction().commit();
  }
}
