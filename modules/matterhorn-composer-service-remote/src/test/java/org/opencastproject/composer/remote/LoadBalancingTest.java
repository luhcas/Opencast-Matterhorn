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
package org.opencastproject.composer.remote;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.remote.impl.ReceiptImpl;
import org.opencastproject.remote.impl.RemoteServiceManagerImpl;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class LoadBalancingTest {
  private static final String JOB_TYPE_OTHER = "other_type";
  private static final String HOST_1 = "http://remotehost1";
  private static final String HOST_2 = "http://remotehost2";
  private static final String HOST_3 = "http://remotehost3";

  ComposerServiceRemoteImpl service;
  ComboPooledDataSource pooledDataSource = null;
  RemoteServiceManagerImpl remoteServiceManager = null;

  @Before
  public void setUp() throws Exception {
    service = new ComposerServiceRemoteImpl();
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

    service.setRemoteServiceManager(remoteServiceManager);
  }

  @After
  public void tearDown() throws Exception {
    remoteServiceManager.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testRemoteHostSelection() throws Exception {
    remoteServiceManager.registerService(ComposerService.JOB_TYPE, HOST_1);
    remoteServiceManager.registerService(ComposerService.JOB_TYPE, HOST_2);
    remoteServiceManager.registerService(ComposerService.JOB_TYPE, HOST_3);
    
    // Add some receipts for each of these hosts
    ReceiptImpl localRunning = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    localRunning.setHost(HOST_1);
    localRunning.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(localRunning);
    
    ReceiptImpl localQueued = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    localQueued.setHost(HOST_1);
    localQueued.setStatus(Status.QUEUED);
    remoteServiceManager.updateReceipt(localQueued);

    ReceiptImpl localFinished = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    localFinished.setHost(HOST_1);
    localFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(localFinished);

    ReceiptImpl remoteRunning = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    remoteRunning.setHost(HOST_2);
    remoteRunning.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(remoteRunning);

    ReceiptImpl remoteFinished = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    remoteFinished.setHost(HOST_2);
    remoteFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(remoteFinished);

    ReceiptImpl otherTypeRunning = (ReceiptImpl)remoteServiceManager.createReceipt(JOB_TYPE_OTHER);
    otherTypeRunning.setHost(HOST_1);
    otherTypeRunning.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(otherTypeRunning);

    ReceiptImpl otherTypeFinished = (ReceiptImpl)remoteServiceManager.createReceipt(JOB_TYPE_OTHER);
    otherTypeFinished.setHost(HOST_1);
    otherTypeFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateReceipt(otherTypeFinished);

    // Host 1 has 1 running and one queued receipt.  Host 2 has 1 running and 1 finished receipt.
    // Host 3 has no running or queued receipts. so host 3 is the 'lightest'
    Assert.assertEquals(HOST_3, remoteServiceManager.getRemoteHosts(ComposerService.JOB_TYPE).get(0));
    
    // Now let's load host 3 with some running and queued receipts
    ReceiptImpl host3Running1 = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    host3Running1.setHost(HOST_3);
    host3Running1.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(host3Running1);

    ReceiptImpl host3Running2 = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    host3Running2.setHost(HOST_3);
    host3Running2.setStatus(Status.RUNNING);
    remoteServiceManager.updateReceipt(host3Running2);

    ReceiptImpl host3Queued = (ReceiptImpl)remoteServiceManager.createReceipt(ComposerService.JOB_TYPE);
    host3Queued.setHost(HOST_3);
    host3Queued.setStatus(Status.QUEUED);
    remoteServiceManager.updateReceipt(host3Queued);
    
    // Now that host3 is loaded, host 2 is the lightest, since it has only 1 running and no queued receipts
    Assert.assertEquals(HOST_2, remoteServiceManager.getRemoteHosts(ComposerService.JOB_TYPE).get(0));
  }
}
