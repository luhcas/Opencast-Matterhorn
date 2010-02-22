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
package org.opencastproject.persistence;

import java.util.Dictionary;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;

public class Service implements ManagedService {

  @PersistenceUnit(unitName = "test")
  private EntityManagerFactory emf = Persistence.createEntityManagerFactory("test");

  public void activate(ComponentContext ctx) {
    EntityManager em = emf.createEntityManager();

    System.out.println("You may see exceptions above, they are (probably) safe to ignore and only occur when the persistence unit first connects.");
    System.out.println("They occur because the PU is trying to create the tables, which may already exist.");
    System.out.println("This behaviour can be disabled, but then the tables must already exist and be the same as what the code is expecting.");
    System.out.println("It will also always try and create the sequence table, this is a TODO.\n\n\n");

    Query q = em.createNamedQuery("JPAList.getByID");
    q.setParameter("id", "myList");
    JPAList l;
    try {
      l = (JPAList) q.getSingleResult();
      System.out.println("Found list of objects with id " + l.getId());
    } catch (NoResultException e) {
      em.getTransaction().begin();
      l = new JPAList();
      l.setId("myList");
      em.persist(l);
      em.getTransaction().commit();
      System.out.println("Created list of objects " + l.getId());
    }

    JPAObject t = new JPAObject();
    t.setString("Example");
    t.setInteger(4064);

    em.getTransaction().begin();
    l.addObject(t);
    em.persist(t);
    em.persist(l);
    em.getTransaction().commit();
    Long id = t.getId();

    System.out.println("Created entity with id " + id);

    t = null;

    System.out.println("Refinding entity based on PK values");
    t = em.find(JPAObject.class, id);
    System.out.println(id + ": " + t.getString() + ", " + t.getInteger());

    System.out.println("Modifying entity " + id);
    em.getTransaction().begin();
    t.setString("Other");
    t.setInteger(4);
    em.getTransaction().commit();

    t = null;

    System.out.println("All entities with string value set to \"Other\":");
    q = em.createNamedQuery("JPAObject.getByString");
    q.setParameter("string", "Other");
    List<JPAObject> results = q.getResultList();
    System.out.println("List size: " + results.size());
    for (JPAObject x : results) {
      System.out.println(x.getId() + ": " + x.getString() + " " + x.getInteger());
    }

    System.out.println("Getting by ID using a named query:");
    //Note:  This is unfortunate, but required
    q = em.createNamedQuery("JPAObject.getByID");
    q.setParameter("id", id);
    t = (JPAObject) q.getSingleResult();
    System.out.println(t.getId() + ": " + t.getString() + " " + t.getInteger());

    System.out.println("Objects in the list created/found at the beginning:");
    l = em.find(JPAList.class, "myList");
    for (JPAObject x : l.getList()) {
      System.out.println(x.getId() + ": " + x.getString() + " " + x.getInteger());
    }
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    // TODO Auto-generated method stub
    
  }
  
}
