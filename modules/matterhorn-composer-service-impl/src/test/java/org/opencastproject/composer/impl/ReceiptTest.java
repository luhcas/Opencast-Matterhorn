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
package org.opencastproject.composer.impl;


import org.opencastproject.composer.api.Receipt;
import org.opencastproject.composer.api.Receipt.Status;
import org.opencastproject.composer.impl.dao.ComposerServiceDaoJpaImpl;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ReceiptTest {
  private ComboPooledDataSource pooledDataSource = null;
  private ComposerServiceDaoJpaImpl dao = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUpBeforeClass() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis() + ";LOCK_MODE=1;MVCC=TRUE");
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    dao = new ComposerServiceDaoJpaImpl();
    dao.setPersistenceProvider(new PersistenceProvider());
    dao.setPersistenceProperties(props);
    dao.activate(null);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDownAfterClass() throws Exception {
    pooledDataSource.close();
  }
  
  @Test
  public void testGetReceipt() throws Exception {
    ReceiptImpl receipt = (ReceiptImpl) dao.createReceipt();
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    receipt.setElement(t);
    receipt.setStatus(Status.FINISHED);
    dao.updateReceipt(receipt);
    
    Receipt receiptFromDb = dao.getReceipt(receipt.getId());
    Assert.assertEquals(receipt.getElement().getIdentifier(), receiptFromDb.getElement().getIdentifier());
  }

  @Test
  public void testGetReceipts() throws Exception {
    dao.createReceipt();
    long queuedJobs = dao.count(Status.QUEUED);
    Assert.assertEquals(1, queuedJobs);
  }

}
