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
import java.util.List;
import java.util.Map;

public class ReceiptTest {
  private static final String RECEIPT_TYPE_1 = "testing1";
  private static final String RECEIPT_TYPE_2 = "testing2";
  private static final String HOST_1 = "host1";
  private static final String HOST_2 = "host2";
  
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
    ReceiptImpl receipt = (ReceiptImpl) receiptService.createReceipt(RECEIPT_TYPE_1);
    
    Receipt receiptFromDb = receiptService.getReceipt(receipt.getId());
    Assert.assertEquals(Status.QUEUED, receiptFromDb.getStatus());

    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    receipt.setElement(t);
    receipt.setStatus(Status.FINISHED);
    receiptService.updateReceipt(receipt);
    
    receiptFromDb = receiptService.getReceipt(receipt.getId());
    Assert.assertEquals(receipt.getElement().getIdentifier(), receiptFromDb.getElement().getIdentifier());
  }

  @Test
  public void testGetReceipts() throws Exception {
    String id = receiptService.createReceipt(RECEIPT_TYPE_1).getId();
    long queuedJobs = receiptService.count(RECEIPT_TYPE_1, Status.QUEUED);
    Assert.assertEquals(1, queuedJobs);
    
    Receipt receipt = receiptService.getReceipt(id);
    receipt.setStatus(Status.RUNNING);
    receiptService.updateReceipt(receipt);
    queuedJobs = receiptService.count(RECEIPT_TYPE_1, Status.QUEUED);
    Assert.assertEquals(0, queuedJobs);
  }
  
  @Test
  public void testCount() throws Exception {
    receiptService.createReceipt(RECEIPT_TYPE_1);
    ReceiptImpl remote1 = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_1);
    ReceiptImpl remote2 = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_1);
    remote1.setHost("remote");
    remote2.setHost("remote");
    receiptService.updateReceipt(remote1);
    receiptService.updateReceipt(remote2);
    Assert.assertEquals(2, receiptService.count(RECEIPT_TYPE_1, Status.QUEUED, "remote"));
  }

  @Test
  public void testGetHostsCount() throws Exception {
    ReceiptImpl localRunning1 = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_1);
    localRunning1.setHost(HOST_1);
    localRunning1.setStatus(Status.RUNNING);
    receiptService.updateReceipt(localRunning1);
    
    ReceiptImpl localRunning2 = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_1);
    localRunning2.setHost(HOST_1);
    localRunning2.setStatus(Status.RUNNING);
    receiptService.updateReceipt(localRunning2);

    ReceiptImpl localFinished = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_1);
    localFinished.setHost(HOST_1);
    localFinished.setStatus(Status.FINISHED);
    receiptService.updateReceipt(localFinished);

    ReceiptImpl remoteRunning = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_1);
    remoteRunning.setHost(HOST_2);
    remoteRunning.setStatus(Status.RUNNING);
    receiptService.updateReceipt(remoteRunning);

    ReceiptImpl remoteFinished = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_1);
    remoteFinished.setHost(HOST_2);
    remoteFinished.setStatus(Status.FINISHED);
    receiptService.updateReceipt(remoteFinished);

    ReceiptImpl otherTypeRunning = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_2);
    otherTypeRunning.setHost(HOST_1);
    otherTypeRunning.setStatus(Status.RUNNING);
    receiptService.updateReceipt(otherTypeRunning);

    ReceiptImpl otherTypeFinished = (ReceiptImpl)receiptService.createReceipt(RECEIPT_TYPE_2);
    otherTypeFinished.setHost(HOST_1);
    otherTypeFinished.setStatus(Status.FINISHED);
    receiptService.updateReceipt(otherTypeFinished);

    Map<String, Long> type1Counts = receiptService.getHostsCount(RECEIPT_TYPE_1, new Status[] {Status.RUNNING});

    Assert.assertEquals(2L, type1Counts.get(HOST_1).longValue());
    Assert.assertEquals(1L, type1Counts.get(HOST_2).longValue());
  }

  @Test
  public void testHandlerRegistration() throws Exception {
    String url = "http://type1handler:8080";
    String receiptType = "type1";
    // we should start with no handlers
    List<String> hosts = receiptService.getHosts(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    receiptService.registerService(receiptType, url);
    hosts = receiptService.getHosts("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals("http://type1handler:8080", hosts.get(0));
    
    // unregister
    receiptService.unRegisterService(receiptType, url);
    hosts = receiptService.getHosts("type1");
    Assert.assertEquals(0, hosts.size());
  }
  
}
