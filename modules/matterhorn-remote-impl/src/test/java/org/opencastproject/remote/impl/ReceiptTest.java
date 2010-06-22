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
package org.opencastproject.remote.impl;


import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.Receipt.Status;

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
  private RemoteServiceManagerImpl remoteServiceManager = null;

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

    remoteServiceManager = new RemoteServiceManagerImpl();
    remoteServiceManager.setPersistenceProvider(new PersistenceProvider());
    remoteServiceManager.setPersistenceProperties(props);
    remoteServiceManager.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    remoteServiceManager.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testMarshalling() throws Exception {
    ReceiptImpl receipt = (ReceiptImpl) remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    receipt.setElement(t);
    receipt.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(receipt);
    String xml = receipt.toXml();
    Receipt parsed = ReceiptBuilder.getInstance().parseReceipt(xml);
    Assert.assertEquals(receipt.getId(), parsed.getId());
    
    // Unmarshall an attachment
    String attachmentXml = "<ns2:attachment xmlns:ns2=\"http://mediapackage.opencastproject.org\"><tags/><url>http://localhost:8080/camera25fpslowdl.jpg</url></ns2:attachment>";
    ReceiptImpl attachmentReceipt = new ReceiptImpl();
    attachmentReceipt.setElementAsXml(attachmentXml);
    Assert.assertTrue(attachmentReceipt.getElement() instanceof Attachment);
  }
  
  @Test
  public void testGetReceipt() throws Exception {
    ReceiptImpl receipt = (ReceiptImpl) remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    
    Receipt receiptFromDb = remoteServiceManager.getReceipt(receipt.getId());
    Assert.assertEquals(Status.QUEUED, receiptFromDb.getStatus());

    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    receipt.setElement(t);
    receipt.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(receipt);
    
    receiptFromDb = remoteServiceManager.getReceipt(receipt.getId());
    Assert.assertEquals(receipt.getElement().getIdentifier(), receiptFromDb.getElement().getIdentifier());
  }
  
  @Test
  public void testGetReceipts() throws Exception {
    String id = remoteServiceManager.createReceipt(RECEIPT_TYPE_1).getId();
    long queuedJobs = remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED);
    Assert.assertEquals(1, queuedJobs);
    
    Receipt receipt = remoteServiceManager.getReceipt(id);
    receipt.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(receipt);
    queuedJobs = remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED);
    Assert.assertEquals(0, queuedJobs);
  }
  
  @Test
  public void testCount() throws Exception {
    remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    ReceiptImpl remote1 = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    ReceiptImpl remote2 = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    remote1.setHost("remote");
    remote2.setHost("remote");
    remoteServiceManager.updateReceipt(remote1);
    remoteServiceManager.updateReceipt(remote2);
    Assert.assertEquals(2, remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED, "remote"));
  }

  @Test
  public void testGetHostsCount() throws Exception {
    remoteServiceManager.registerService(RECEIPT_TYPE_1, HOST_1);
    remoteServiceManager.registerService(RECEIPT_TYPE_1, HOST_2);
    remoteServiceManager.registerService(RECEIPT_TYPE_2, HOST_1);
    remoteServiceManager.registerService(RECEIPT_TYPE_2, HOST_2);
    
    ReceiptImpl localRunning1 = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    localRunning1.setHost(HOST_1);
    localRunning1.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(localRunning1);
    
    ReceiptImpl localRunning2 = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    localRunning2.setHost(HOST_1);
    localRunning2.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(localRunning2);

    ReceiptImpl localFinished = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    localFinished.setHost(HOST_1);
    localFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(localFinished);

    ReceiptImpl remoteRunning = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    remoteRunning.setHost(HOST_2);
    remoteRunning.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(remoteRunning);

    ReceiptImpl remoteFinished = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_1);
    remoteFinished.setHost(HOST_2);
    remoteFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(remoteFinished);

    ReceiptImpl otherTypeRunning = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_2);
    otherTypeRunning.setHost(HOST_1);
    otherTypeRunning.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(otherTypeRunning);

    ReceiptImpl otherTypeFinished = (ReceiptImpl)remoteServiceManager.createReceipt(RECEIPT_TYPE_2);
    otherTypeFinished.setHost(HOST_1);
    otherTypeFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(otherTypeFinished);

    List<String> type1Hosts = remoteServiceManager.getRemoteHosts(RECEIPT_TYPE_1);
    List<String> type2Hosts = remoteServiceManager.getRemoteHosts(RECEIPT_TYPE_2);

    // Host 1 has more jobs than host 2
    Assert.assertEquals(2, type1Hosts.size());
    Assert.assertEquals(HOST_2, type1Hosts.get(0));
    Assert.assertEquals(HOST_1, type1Hosts.get(1));

    // The same order applies regardless of job type
    Assert.assertEquals(2, type2Hosts.size());
    Assert.assertEquals(HOST_2, type1Hosts.get(0));
    Assert.assertEquals(HOST_1, type1Hosts.get(1));
  }

  @Test
  public void testHandlerRegistration() throws Exception {
    String url = "http://type1handler:8080";
    String receiptType = "type1";
    // we should start with no handlers
    List<String> hosts = remoteServiceManager.getHosts(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    remoteServiceManager.registerService(receiptType, url);
    hosts = remoteServiceManager.getHosts("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals("http://type1handler:8080", hosts.get(0));

    // set the handler to be in maintenance mode
    remoteServiceManager.setMaintenanceMode(receiptType, url, true);
    hosts = remoteServiceManager.getHosts("type1");
    Assert.assertEquals(0, hosts.size());

    // set it back to normal mode
    remoteServiceManager.setMaintenanceMode(receiptType, url, false);
    hosts = remoteServiceManager.getHosts("type1");
    Assert.assertEquals(1, hosts.size());

    // unregister
    remoteServiceManager.unRegisterService(receiptType, url);
    hosts = remoteServiceManager.getHosts("type1");
    Assert.assertEquals(0, hosts.size());
  }

  @Test
  public void testDuplicateHandlerRegistrations() throws Exception {
    String url = "http://type1handler:8080";
    String receiptType = "type1";
    // we should start with no handlers
    List<String> hosts = remoteServiceManager.getHosts(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    remoteServiceManager.registerService(receiptType, url);
    hosts = remoteServiceManager.getHosts("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals("http://type1handler:8080", hosts.get(0));

    // set the handler to be in maintenance mode
    remoteServiceManager.setMaintenanceMode(receiptType, url, true);
    hosts = remoteServiceManager.getHosts("type1");
    Assert.assertEquals(0, hosts.size());

    // try to re-register.  this should unset the maintenance mode and log a warning, but should not throw an exception
    remoteServiceManager.registerService(receiptType, url);
    Assert.assertEquals(1, remoteServiceManager.getServiceRegistrations().size());
    Assert.assertFalse(remoteServiceManager.getServiceRegistrations().get(0).isInMaintenanceMode());

    // unregister
    remoteServiceManager.unRegisterService(receiptType, url);
    hosts = remoteServiceManager.getHosts("type1");
    Assert.assertEquals(0, hosts.size());
  }

}
