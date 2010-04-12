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
package org.opencastproject.composer.impl.dao;

import org.opencastproject.composer.api.Receipt;
import org.opencastproject.composer.api.Receipt.Status;
import org.opencastproject.composer.impl.ReceiptImpl;

import org.osgi.service.component.ComponentContext;

import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;

/**
 * Uses JPA for persistence of {@link Receipt}s
 */
public class ComposerServiceDaoJpaImpl implements ComposerServiceDao {
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
  
  public void activate(ComponentContext cc) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.composer", persistenceProperties);
  }

  public void deactivate() {
    emf.close();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#count(org.opencastproject.composer.api.Receipt.Status)
   */
  @Override
  public long count(Status status) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createQuery("SELECT COUNT(r) FROM Receipt r where r.status = :status");
      query.setParameter("status", status);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } finally {
      em.close();
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#count(org.opencastproject.composer.api.Receipt.Status, java.lang.String)
   */
  @Override
  public long count(Status status, String host) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createQuery("SELECT COUNT(r) FROM Receipt r where r.status = :status and r.host = :host");
      query.setParameter("status", status);
      query.setParameter("host", host);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#createReceipt()
   */
  @Override
  public Receipt createReceipt() {
    String id = UUID.randomUUID().toString();
    Receipt receipt = new ReceiptImpl(id, Status.QUEUED, "localhost"); // FIXME Find host name from configuration?
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      em.persist(receipt);
      tx.commit();
      return receipt;
    } catch(RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    EntityManager em = emf.createEntityManager();
    try {
      return em.find(ReceiptImpl.class, id);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#updateReceipt(org.opencastproject.composer.api.Receipt)
   */
  @Override
  public void updateReceipt(Receipt receipt) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Receipt fromDb;
      try {
        fromDb = em.find(ReceiptImpl.class, receipt.getId());
      } catch (NoResultException e) {
        throw new IllegalArgumentException("receipt " + receipt + " is not a persistent object.", e);
      }
      fromDb.setElement(receipt.getElement());
      fromDb.setStatus(receipt.getStatus());
      fromDb.setHost(receipt.getHost());
      tx.commit();
    } catch(RollbackException e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }
}
