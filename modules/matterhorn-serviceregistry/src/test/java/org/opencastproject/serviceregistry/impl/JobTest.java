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
package org.opencastproject.serviceregistry.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.util.UrlSupport;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class JobTest {
  private static final String JOB_TYPE_1 = "testing1";
  private static final String JOB_TYPE_2 = "testing2";
  private static final String LOCALHOST = UrlSupport.DEFAULT_BASE_URL;
  private static final String HOST_2 = "host2";
  private static final String PATH = "/path";

  private ComboPooledDataSource pooledDataSource = null;
  private ServiceRegistryJpaImpl serviceRegistry = null;

  private ServiceRegistrationJpaImpl regType1Host1 = null;
  private ServiceRegistrationJpaImpl regType1Host2 = null;
  private ServiceRegistrationJpaImpl regType2Host1 = null;
  private ServiceRegistrationJpaImpl regType2Host2 = null;

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

    serviceRegistry = new ServiceRegistryJpaImpl();
    serviceRegistry.setPersistenceProvider(new PersistenceProvider());
    serviceRegistry.setPersistenceProperties(props);
    serviceRegistry.activate(null);

    // register some service instances
    regType1Host1 = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_1, LOCALHOST, PATH);
    regType1Host2 = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_1, HOST_2, PATH);
    regType2Host1 = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_2, LOCALHOST, PATH);
    regType2Host2 = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_2, HOST_2, PATH);
  }

  @After
  public void tearDown() throws Exception {
    serviceRegistry.unRegisterService(JOB_TYPE_1, LOCALHOST, PATH);
    serviceRegistry.unRegisterService(JOB_TYPE_1, HOST_2, PATH);
    serviceRegistry.unRegisterService(JOB_TYPE_2, LOCALHOST, PATH);
    serviceRegistry.unRegisterService(JOB_TYPE_2, HOST_2, PATH);
    serviceRegistry.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testMarshalling() throws Exception {
    JobJpaImpl job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1);
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");

    // Simulate starting the job
    job.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(job);

    // Finish the job
    job.setElement(t);
    job.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(job);
    String xml = JobParser.serializeToString(job);
    Job parsed = JobParser.parseJob(xml);
    Assert.assertEquals(job.getId(), parsed.getId());

    // Unmarshall an attachment
    String attachmentXml = "<ns2:attachment xmlns:ns2=\"http://mediapackage.opencastproject.org\"><tags/><url>http://localhost:8080/camera25fpslowdl.jpg</url></ns2:attachment>";
    JobJpaImpl attachmentReceipt = new JobJpaImpl();
    attachmentReceipt.setElementAsXml(attachmentXml);
    Assert.assertTrue(attachmentReceipt.getElement() instanceof Attachment);
  }

  @Test
  public void testGetReceipt() throws Exception {
    JobJpaImpl job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1);

    Job receiptFromDb = serviceRegistry.getJob(job.getId());
    Assert.assertEquals(Status.QUEUED, receiptFromDb.getStatus());

    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    job.setElement(t);

    // Simulate starting the job
    job.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(job);

    // Finish the job
    job.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(job);

    receiptFromDb = serviceRegistry.getJob(job.getId());
    Assert.assertEquals(job.getElement().getIdentifier(), receiptFromDb.getElement().getIdentifier());
  }

  @Test
  public void testGetReceipts() throws Exception {
    String id = serviceRegistry.createJob(JOB_TYPE_1).getId();
    long queuedJobs = serviceRegistry.count(JOB_TYPE_1, Status.QUEUED);
    Assert.assertEquals(1, queuedJobs);

    Job receipt = serviceRegistry.getJob(id);
    receipt.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(receipt);
    queuedJobs = serviceRegistry.count(JOB_TYPE_1, Status.QUEUED);
    Assert.assertEquals(0, queuedJobs);
  }

  @Test
  public void testCount() throws Exception {
    // create a receipt on each service instance
    serviceRegistry.createJob(regType1Host1);
    serviceRegistry.createJob(regType1Host2);

    Assert.assertEquals(1, serviceRegistry.count(JOB_TYPE_1, Status.QUEUED, LOCALHOST));
    Assert.assertEquals(1, serviceRegistry.count(JOB_TYPE_1, Status.QUEUED, HOST_2));
    Assert.assertEquals(2, serviceRegistry.count(JOB_TYPE_1, Status.QUEUED));
  }

  @Test
  public void testGetHostsCountWithNoJobs() throws Exception {
    Assert.assertEquals(2, serviceRegistry.getServiceRegistrations(JOB_TYPE_1).size());
    Assert.assertEquals(2, serviceRegistry.getServiceRegistrations(JOB_TYPE_2).size());
  }

  @Test
  public void testGetHostsCount() throws Exception {
    JobJpaImpl localRunning1 = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1);
    localRunning1.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(localRunning1);

    JobJpaImpl localRunning2 = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1);
    localRunning2.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(localRunning2);

    JobJpaImpl localFinished = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1);
    // Simulate starting the job
    localFinished.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(localFinished);
    // Finish the job
    localFinished.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(localFinished);

    JobJpaImpl remoteRunning = (JobJpaImpl) serviceRegistry.createJob(regType1Host2);
    remoteRunning.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(remoteRunning);

    JobJpaImpl remoteFinished = (JobJpaImpl) serviceRegistry.createJob(regType1Host2);
    // Simulate starting the job
    remoteFinished.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(remoteFinished);
    // Finish the job
    remoteFinished.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(remoteFinished);

    JobJpaImpl otherTypeRunning = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_2);
    otherTypeRunning.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(otherTypeRunning);

    JobJpaImpl otherTypeFinished = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_2);
    // Simulate starting the job
    otherTypeFinished.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(otherTypeFinished);
    // Finish the job
    otherTypeFinished.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(otherTypeFinished);

    List<ServiceRegistration> type1Hosts = serviceRegistry.getServiceRegistrations(JOB_TYPE_1);
    List<ServiceRegistration> type2Hosts = serviceRegistry.getServiceRegistrations(JOB_TYPE_2);

    // Host 1 has more "type 1" jobs than host 2
    Assert.assertEquals(2, type1Hosts.size());
    Assert.assertEquals(HOST_2, type1Hosts.get(0).getHost());
    Assert.assertEquals(LOCALHOST, type1Hosts.get(1).getHost());

    // There is just one running job of receipt type 2.  It is on the localhost.  Since host 2 has no type 2 jobs, it
    // is the least loaded server, and takes precedence in the list.
    Assert.assertEquals(1, serviceRegistry.count(JOB_TYPE_2, Status.RUNNING));
    Assert.assertEquals(2, type2Hosts.size());
    Assert.assertEquals(HOST_2, type2Hosts.get(0).getHost());
    Assert.assertEquals(LOCALHOST, type2Hosts.get(1).getHost());
  }

  @Test
  public void testHandlerRegistration() throws Exception {
    String url = "http://type1handler:8080";
    String receiptType = "type1";
    // we should start with no handlers
    List<ServiceRegistration> hosts = serviceRegistry.getServiceRegistrations(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    serviceRegistry.registerService(receiptType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrations("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals("http://type1handler:8080", hosts.get(0).getHost());

    // set the handler to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(receiptType, url, true);
    hosts = serviceRegistry.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());

    // set it back to normal mode
    serviceRegistry.setMaintenanceStatus(receiptType, url, false);
    hosts = serviceRegistry.getServiceRegistrations("type1");
    Assert.assertEquals(1, hosts.size());

    // unregister
    serviceRegistry.unRegisterService(receiptType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());
  }

  @Test
  public void testDuplicateHandlerRegistrations() throws Exception {
    String url = "http://type1handler:8080";
    String receiptType = "type1";
    // we should start with no handlers
    List<ServiceRegistration> hosts = serviceRegistry.getServiceRegistrations(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    serviceRegistry.registerService(receiptType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrations("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals("http://type1handler:8080", hosts.get(0).getHost());

    // set the handler to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(receiptType, url, true);
    hosts = serviceRegistry.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());

    // re-register. this should not unset the maintenance mode and should not throw an exception
    ServiceRegistration reg = serviceRegistry.registerService(receiptType, url, PATH);
    Assert.assertTrue(reg.isInMaintenanceMode());
    
    // zero because it's still in maintenance mode
    Assert.assertEquals(0, serviceRegistry.getServiceRegistrations(receiptType).size());


    // unregister
    serviceRegistry.unRegisterService(receiptType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrations("type1");
    Assert.assertEquals(0, hosts.size());
  }

}