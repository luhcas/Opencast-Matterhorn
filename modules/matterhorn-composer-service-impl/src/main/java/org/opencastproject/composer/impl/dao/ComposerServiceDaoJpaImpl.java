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
import javax.persistence.Query;
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

  /** The entity manager used for persisting entities. */
  protected EntityManager em = null;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;
  
  public void activate(ComponentContext cc) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.composer", persistenceProperties);
    em = emf.createEntityManager();
  }

  public void deactivate() {
    em.close();
    emf.close();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#count(org.opencastproject.composer.api.Receipt.Status)
   */
  @Override
  public long count(Status status) {
    Query query = em.createQuery("SELECT COUNT(r) FROM Receipt r where r.status = :status");
    query.setParameter("status", status);
    Number countResult = (Number) query.getSingleResult();
    return countResult.longValue();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#count(org.opencastproject.composer.api.Receipt.Status, java.lang.String)
   */
  @Override
  public long count(Status status, String host) {
    Query query = em.createQuery("SELECT COUNT(r) FROM Receipt r where r.status = :status and r.host = :host");
    query.setParameter("status", status);
    query.setParameter("host", host);
    Number countResult = (Number) query.getSingleResult();
    return countResult.longValue();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#createReceipt()
   */
  @Override
  public Receipt createReceipt() {
    String id = UUID.randomUUID().toString();
    Receipt receipt = new ReceiptImpl(id, Status.QUEUED, "localhost"); // FIXME Find host name from configuration?
    em.getTransaction().begin();
    em.persist(receipt);
    em.getTransaction().commit();
    return receipt;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    return em.find(ReceiptImpl.class, id);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.dao.ComposerServiceDao#updateReceipt(org.opencastproject.composer.api.Receipt)
   */
  @Override
  public void updateReceipt(Receipt receipt) {
    em.getTransaction().begin();
    Receipt fromDb = getReceipt(receipt.getId());
    if(fromDb == null) throw new IllegalArgumentException("receipt " + receipt + " is not a persistent object.");
    fromDb.setElement(receipt.getElement());
    fromDb.setStatus(receipt.getStatus());
    fromDb.setHost(receipt.getHost());
    em.getTransaction().commit();
  }

}
