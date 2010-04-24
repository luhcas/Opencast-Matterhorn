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
package org.opencastproject.receipt.impl;


import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.Receipt.Status;

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
  private static final String RECEIPT_TYPE = "testing";
  
  private ComboPooledDataSource pooledDataSource = null;
  private ReceiptServiceImpl receiptService = null;

  @Before
  public void setUp() throws Exception {
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

    receiptService = new ReceiptServiceImpl();
    receiptService.setPersistenceProvider(new PersistenceProvider());
    receiptService.setPersistenceProperties(props);
    receiptService.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    receiptService.deactivate();
    pooledDataSource.close();
  }
  
  @Test
  public void testGetReceipt() throws Exception {
    ReceiptImpl receipt = (ReceiptImpl) receiptService.createReceipt(RECEIPT_TYPE);
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    receipt.setElement(t);
    receipt.setStatus(Status.FINISHED);
    receiptService.updateReceipt(receipt);
    
    Receipt receiptFromDb = receiptService.getReceipt(receipt.getId());
    Assert.assertEquals(receipt.getElement().getIdentifier(), receiptFromDb.getElement().getIdentifier());
  }

  @Test
  public void testGetReceipts() throws Exception {
    receiptService.createReceipt(RECEIPT_TYPE);
    long queuedJobs = receiptService.count(RECEIPT_TYPE, Status.QUEUED);
    Assert.assertEquals(1, queuedJobs);
  }
  
  @Test
  public void testCount() throws Exception {
    receiptService.createReceipt(RECEIPT_TYPE);
    ReceiptImpl remote1 = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE);
    ReceiptImpl remote2 = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE);
    remote1.setHost("remote");
    remote2.setHost("remote");
    receiptService.updateReceipt(remote1);
    receiptService.updateReceipt(remote2);
    Assert.assertEquals(2, receiptService.count(RECEIPT_TYPE, Status.QUEUED, "remote"));
  }

}
