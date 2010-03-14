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

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowSetImpl;

import org.osgi.service.component.ComponentContext;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.spi.PersistenceProvider;

/**
 * JPA implementation of the workflow service persistence layer.
 */
public class WorkflowServiceImplDaoJpaImpl implements WorkflowServiceImplDao {

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /**
   * @param persistenceProvider the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }
  
  @SuppressWarnings("unchecked")
  protected Map persistenceProperties;

  /**
   * @param persistenceProperties the persistenceProperties to set
   */
  @SuppressWarnings("unchecked")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#activate()
   */
  @Override
  public void activate() {
    emf = persistenceProvider.createEntityManagerFactory("workflow", persistenceProperties);
  }
  
  public void activate(ComponentContext cc) {
    activate();
  }

  public void deactivate() {
    emf.close();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createQuery("SELECT COUNT(w) FROM workflow w");
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowById(java.lang.String)
   */
  @Override
  public WorkflowInstance getWorkflowById(String workflowId) {
    EntityManager em = emf.createEntityManager();
    try {
      return em.find(WorkflowInstanceImpl.class, workflowId);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) {
    EntityManager em = emf.createEntityManager();
    try {
      long start = System.currentTimeMillis();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<WorkflowInstanceImpl> c = cb.createQuery(WorkflowInstanceImpl.class);
      Root<WorkflowInstanceImpl> root = c.from(WorkflowInstanceImpl.class);

      if(query.getCurrentOperation() != null) {
        Predicate condition = cb.equal(root.get("currentOperation"), query.getCurrentOperation());
        c.where(condition);
      }
      if(query.getState() != null) {
        Predicate condition = cb.equal(root.get("state"), query.getState());
        c.where(condition);
      }
      if(query.getMediaPackage() != null) {
        Predicate condition = cb.equal(root.get("mediaPackageId"), query.getMediaPackage());
        c.where(condition);
      }
      if(query.getSeries() != null) {
        Predicate condition = cb.equal(root.get("series"), query.getSeries());
        c.where(condition);
      }
      if(query.getText() != null) {
        Predicate condition = cb.equal(root.get("title"), query.getText()); // TODO: or the rest of the possible columns
        c.where(condition);
      }
      if(query.getStartPage() > 0) {
      }
      
          
      // TODO Add the rest of the criteria
      // TODO Enable paging
      
      c.select(root);
      TypedQuery<WorkflowInstanceImpl> typedQuery = em.createQuery(c);

      long startPage = query.getStartPage();
      long count = query.getCount();
      
      if(startPage > 1 && count > 0) {
        typedQuery.setFirstResult((int)((startPage - 1) * count));
      }
      
      if (count > 0) {
        typedQuery.setMaxResults((int)count);
      }
      List<WorkflowInstanceImpl> list = typedQuery.getResultList();
      long searchTime = System.currentTimeMillis() - start;
      WorkflowSetImpl set = new WorkflowSetImpl();
      
      set.setCount(Math.min(query.getCount(), 0)); // FIXME: the last arg should be the total count
      set.setStartPage(query.getStartPage());
      set.setSearchTime(searchTime);
      set.setTotalCount(0); // FIXME: Set this to the total count
      for(WorkflowInstance workflow : list) {
        set.addItem(workflow);
      }
      return set;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#remove(java.lang.String)
   */
  @Override
  public void remove(String id) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      WorkflowInstance workflow = getWorkflowById(id);
      tx.begin();
      em.remove(workflow);
      tx.commit();
    } catch(RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(WorkflowInstance instance) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      WorkflowInstance existingWorkflow = getWorkflowById(instance.getId());
      tx.begin();
      if(existingWorkflow == null) {
        em.persist(instance);
      } else {
        em.merge(instance);
      }
      tx.commit();
    } catch(RollbackException e) {
      tx.rollback();
    } finally {
      em.close();
    }
  }
}
