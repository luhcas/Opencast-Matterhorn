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
package org.opencastproject.scheduler.impl.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.osgi.service.component.ComponentContext;

/**
 * TODO: Comment me!
 *
 */
public class SchedulerServiceImplJPA extends SchedulerServiceImpl {

  protected PersistenceProvider persistenceProvider;
  protected Map<String, Object> persistenceProperties;
  protected EntityManagerFactory emf = null;
  
  public Map<String, Object> getPersistenceProperties() {
    return persistenceProperties;
  }

  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }
  
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }
  
  public PersistenceProvider getPersistenceProvider() {
    return persistenceProvider;
  }

  protected Event makeIdUnique (Event e) {
    EntityManager em = emf.createEntityManager();
    if (e.getEventId() == null) e.generateId();
    Event found = em.find(Event.class, e.getEventId());
    while (found != null) {
      e.generateId();
      found = em.find(Event.class, e.getEventId());
    }
    return e;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @Override
  public SchedulerEvent addEvent(SchedulerEvent e) {
    EntityManager em = emf.createEntityManager();
    Event event = ((SchedulerEventImpl) e).toEvent();
    event = makeIdUnique(event);
    EntityTransaction tx = em.getTransaction();
    tx.begin();
    em.persist(event);
    tx.commit();
    
    return event.toSchedulerEvent();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#getEvent(java.lang.String)
   */
  @Override
  public SchedulerEvent getEvent(String eventID) {
    EntityManager em = emf.createEntityManager();
    Event e = em.find(Event.class, eventID);
    return e.toSchedulerEvent();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   */
  @Override
  public SchedulerEvent[] getEvents(SchedulerFilter filter) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#removeEvent(java.lang.String)
   */
  @Override
  public boolean removeEvent(String eventID) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#updateEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @Override
  public boolean updateEvent(SchedulerEvent e) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#findConflictingEvents(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @Override
  public SchedulerEvent[] findConflictingEvents(SchedulerEvent e) {
    // TODO Auto-generated method stub
    return null;
  }
  
  public void activate (ComponentContext cc) {
    //super.activate(cc); 
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.scheduler", persistenceProperties);
    //EntityManager em = emf.createEntityManager();
  }
  
  public void destroy() {
    emf.close();
  }  

}
