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
import org.opencastproject.remote.api.Job;
import org.opencastproject.remote.api.ServiceRegistration;
import org.opencastproject.remote.api.Job.Status;
import org.opencastproject.util.UrlSupport;

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

public class JobTest {
  private static final String RECEIPT_TYPE_1 = "testing1";
  private static final String RECEIPT_TYPE_2 = "testing2";
  private static final String LOCALHOST = UrlSupport.DEFAULT_BASE_URL;
  private static final String HOST_2 = "host2";
  private static final String PATH = "/path";

  private ComboPooledDataSource pooledDataSource = null;
  private RemoteServiceManagerImpl remoteServiceManager = null;

  private ServiceRegistrationImpl regType1Host1 = null;
  private ServiceRegistrationImpl regType1Host2 = null;
  private ServiceRegistrationImpl regType2Host1 = null;
  private ServiceRegistrationImpl regType2Host2 = null;

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

    // register some service instances
    regType1Host1 = (ServiceRegistrationImpl) remoteServiceManager.registerService(RECEIPT_TYPE_1, LOCALHOST, PATH);
    regType1Host2 = (ServiceRegistrationImpl) remoteServiceManager.registerService(RECEIPT_TYPE_1, HOST_2, PATH);
    regType2Host1 = (ServiceRegistrationImpl) remoteServiceManager.registerService(RECEIPT_TYPE_2, LOCALHOST, PATH);
    regType2Host2 = (ServiceRegistrationImpl) remoteServiceManager.registerService(RECEIPT_TYPE_2, HOST_2, PATH);
  }

  @After
  public void tearDown() throws Exception {
    remoteServiceManager.unRegisterService(RECEIPT_TYPE_1, LOCALHOST, PATH);
    remoteServiceManager.unRegisterService(RECEIPT_TYPE_1, HOST_2, PATH);
    remoteServiceManager.unRegisterService(RECEIPT_TYPE_2, LOCALHOST, PATH);
    remoteServiceManager.unRegisterService(RECEIPT_TYPE_2, HOST_2, PATH);
    remoteServiceManager.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testMarshalling() throws Exception {
    JobImpl job = (JobImpl) remoteServiceManager.createJob(RECEIPT_TYPE_1);
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");

    // Simulate starting the job
    job.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(job);

    // Finish the job
    job.setElement(t);
    job.setStatus(Status.FINISHED);
    remoteServiceManager.updateJob(job);
    String xml = job.toXml();
    Job parsed = JobBuilder.getInstance().parseJob(xml);
    Assert.assertEquals(job.getId(), parsed.getId());

    // Unmarshall an attachment
    String attachmentXml = "<ns2:attachment xmlns:ns2=\"http://mediapackage.opencastproject.org\"><tags/><url>http://localhost:8080/camera25fpslowdl.jpg</url></ns2:attachment>";
    JobImpl attachmentReceipt = new JobImpl();
    attachmentReceipt.setElementAsXml(attachmentXml);
    Assert.assertTrue(attachmentReceipt.getElement() instanceof Attachment);
  }

  @Test
  public void testGetReceipt() throws Exception {
    JobImpl job = (JobImpl) remoteServiceManager.createJob(RECEIPT_TYPE_1);

    Job receiptFromDb = remoteServiceManager.getJob(job.getId());
    Assert.assertEquals(Status.QUEUED, receiptFromDb.getStatus());

    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    job.setElement(t);

    // Simulate starting the job
    job.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(job);

    // Finish the job
    job.setStatus(Status.FINISHED);
    remoteServiceManager.updateJob(job);

    receiptFromDb = remoteServiceManager.getJob(job.getId());
    Assert.assertEquals(job.getElement().getIdentifier(), receiptFromDb.getElement().getIdentifier());
  }

  @Test
  public void testGetReceipts() throws Exception {
    String id = remoteServiceManager.createJob(RECEIPT_TYPE_1).getId();
    long queuedJobs = remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED);
    Assert.assertEquals(1, queuedJobs);

    Job receipt = remoteServiceManager.getJob(id);
    receipt.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(receipt);
    queuedJobs = remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED);
    Assert.assertEquals(0, queuedJobs);
  }

  @Test
  public void testCount() throws Exception {
    // create a receipt on each service instance
    remoteServiceManager.createJob(regType1Host1);
    remoteServiceManager.createJob(regType1Host2);

    Assert.assertEquals(1, remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED, LOCALHOST));
    Assert.assertEquals(1, remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED, HOST_2));
    Assert.assertEquals(2, remoteServiceManager.count(RECEIPT_TYPE_1, Status.QUEUED));
  }

  @Test
  public void testGetHostsCountWithNoJobs() throws Exception {
    Assert.assertEquals(2, remoteServiceManager.getServiceRegistrations(RECEIPT_TYPE_1).size());
    Assert.assertEquals(2, remoteServiceManager.getServiceRegistrations(RECEIPT_TYPE_2).size());
  }

  @Test
  public void testGetHostsCount() throws Exception {
    JobImpl localRunning1 = (JobImpl) remoteServiceManager.createJob(RECEIPT_TYPE_1);
    localRunning1.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(localRunning1);

    JobImpl localRunning2 = (JobImpl) remoteServiceManager.createJob(RECEIPT_TYPE_1);
    localRunning2.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(localRunning2);

    JobImpl localFinished = (JobImpl) remoteServiceManager.createJob(RECEIPT_TYPE_1);
    // Simulate starting the job
    localFinished.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(localFinished);
    // Finish the job
    localFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateJob(localFinished);

    JobImpl remoteRunning = (JobImpl) remoteServiceManager.createJob(regType1Host2);
    remoteRunning.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(remoteRunning);

    JobImpl remoteFinished = (JobImpl) remoteServiceManager.createJob(regType1Host2);
    // Simulate starting the job
    remoteFinished.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(remoteFinished);
    // Finish the job
    remoteFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateJob(remoteFinished);

    JobImpl otherTypeRunning = (JobImpl) remoteServiceManager.createJob(RECEIPT_TYPE_2);
    otherTypeRunning.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(otherTypeRunning);

    JobImpl otherTypeFinished = (JobImpl) remoteServiceManager.createJob(RECEIPT_TYPE_2);
    // Simulate starting the job
    otherTypeFinished.setStatus(Status.RUNNING);
    remoteServiceManager.updateJob(otherTypeFinished);
    // Finish the job
    otherTypeFinished.setStatus(Status.FINISHED);
    remoteServiceManager.updateJob(otherTypeFinished);

    List<ServiceRegistration> type1Hosts = remoteServiceManager.getServiceRegistrations(RECEIPT_TYPE_1);
    List<ServiceRegistration> type2Hosts = remoteServiceManager.getServiceRegistrations(RECEIPT_TYPE_2);

    // Host 1 has more "type 1" jobs than host 2
    Assert.assertEquals(2, type1Hosts.size());
    Assert.assertEquals(HOST_2, type1Hosts.get(0).getHost());
    Assert.assertEquals(LOCALHOST, type1Hosts.get(1).getHost());

    // There is just one running job of receipt type 2.  It is on the localhost.  Since host 2 has no type 2 jobs, it
    // is the least loaded server, and takes precedence in the list.
    Assert.assertEquals(1, remoteServiceManager.count(RECEIPT_TYPE_2, Status.RUNNING));
    Assert.assertEquals(2, type2Hosts.size());
    Assert.assertEquals(HOST_2, type2Hosts.get(0).getHost());
    Assert.assertEquals(LOCALHOST, type2Hosts.get(1).getHost());
  }

  @Test
  public void testHandlerRegistration() throws Exception {
    String url = "http://type1handler:8080";
    String receiptType = "type1";
    // we should start with no handlers
    List<ServiceRegistration> hosts = remoteServiceManager.getServiceRegistrations(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    remoteServiceManager.registerService(receiptType, url, PATH);
    hosts = remoteServiceManager.getServiceRegistrations("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals("http://type1handler:8080", hosts.get(0).getHost());

    // set the handler to be in maintenance mode
    remoteServiceManager.setMaintenanceStatus(receiptType, url, true);
    hosts = remoteServiceManager.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());

    // set it back to normal mode
    remoteServiceManager.setMaintenanceStatus(receiptType, url, false);
    hosts = remoteServiceManager.getServiceRegistrations("type1");
    Assert.assertEquals(1, hosts.size());

    // unregister
    remoteServiceManager.unRegisterService(receiptType, url, PATH);
    hosts = remoteServiceManager.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());
  }

  @Test
  public void testDuplicateHandlerRegistrations() throws Exception {
    String url = "http://type1handler:8080";
    String receiptType = "type1";
    // we should start with no handlers
    List<ServiceRegistration> hosts = remoteServiceManager.getServiceRegistrations(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    remoteServiceManager.registerService(receiptType, url, PATH);
    hosts = remoteServiceManager.getServiceRegistrations("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals("http://type1handler:8080", hosts.get(0).getHost());

    // set the handler to be in maintenance mode
    remoteServiceManager.setMaintenanceStatus(receiptType, url, true);
    hosts = remoteServiceManager.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());

    // re-register. this should not unset the maintenance mode and should not throw an exception
    ServiceRegistration reg = remoteServiceManager.registerService(receiptType, url, PATH);
    Assert.assertTrue(reg.isInMaintenanceMode());
    
    // zero because it's still in maintenance mode
    Assert.assertEquals(0, remoteServiceManager.getServiceRegistrations(receiptType).size());


    // unregister
    remoteServiceManager.unRegisterService(receiptType, url, PATH);
    hosts = remoteServiceManager.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());
  }

}
