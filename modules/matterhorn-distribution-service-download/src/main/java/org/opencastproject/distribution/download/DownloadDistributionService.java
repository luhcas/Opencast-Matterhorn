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
package org.opencastproject.distribution.download;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Distributes media to the local media delivery directory.
 */
public class DownloadDistributionService implements DistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionService.class);

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.download";

  /** Default distribution directory */
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator + "static";

  /** Path to the distribution directory */
  protected File distributionDirectory = null;

  /** this media download service's base URL */
  protected String serviceUrl = null;

  /** The remote service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The workspace reference */
  protected Workspace workspace = null;

  /** The executor service used to queue and run jobs */
  protected ExecutorService executor = null;

  /**
   * Activate method for this OSGi service implementation.
   * 
   * @param cc
   *          the OSGi component context
   */
  protected void activate(ComponentContext cc) {

    int threads = 1;
    String threadsConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.distribution.download.threads"));
    if (threadsConfig != null) {
      try {
        threads = Integer.parseInt(threadsConfig);
      } catch (NumberFormatException e) {
        logger.warn("Download distribution threads configuration is malformed: '{}'", threadsConfig);
      }
    }
    executor = Executors.newFixedThreadPool(threads);

    serviceUrl = cc.getBundleContext().getProperty("org.opencastproject.download.url");
    if (serviceUrl == null)
      throw new IllegalStateException("Download url must be set (org.opencastproject.download.url)");

    String ccDistributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
    if (ccDistributionDirectory == null)
      throw new IllegalStateException("Distribution directory must be set (org.opencastproject.download.directory)");
    this.distributionDirectory = new File(ccDistributionDirectory);
    logger.info("Download distribution directory is {}", distributionDirectory);
  }

  /**
   * OSGi deactivation callback.
   * 
   * @param cc
   *          the component context
   */
  protected void deactivate(ComponentContext cc) {
    executor.shutdown();
  }

  /**
   * Distributes the mediapackage's element to the location that is returned by the concrete implementation. In
   * addition, a representation of the distributed element is added to the mediapackage.
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(String, MediaPackageElement, boolean)
   */
  @Override
  public Job distribute(final String mediaPackageId, final MediaPackageElement element, boolean block)
          throws DistributionException {
    final Job job;
    try {
      job = serviceRegistry.createJob(JOB_TYPE);
    } catch (ServiceUnavailableException e) {
      throw new DistributionException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }

    Callable<Void> command = new Callable<Void>() {
      /**
       * {@inheritDoc}
       * 
       * @see java.util.concurrent.Callable#call()
       */
      @Override
      public Void call() throws DistributionException {
        job.setStatus(Status.RUNNING);
        updateJob(job);

        File sourceFile;
        try {
          sourceFile = workspace.get(element.getURI());
        } catch (NotFoundException e) {
          throw new DistributionException("Unable to find " + element.getURI() + " in the workspace", e);
        } catch (IOException e) {
          throw new DistributionException("Error loading " + element.getURI() + " from the workspace", e);
        }
        File destination = getDistributionFile(mediaPackageId, element);

        // Put the file in place
        try {
          FileUtils.forceMkdir(destination.getParentFile());
        } catch (IOException e) {
          throw new DistributionException("Unable to create " + destination.getParentFile(), e);
        }
        logger.info("Distributing {} to {}", element, destination);

        try {
          FileSupport.copy(sourceFile, destination);
        } catch (IOException e) {
          throw new DistributionException("Unable to copy " + sourceFile + " to " + destination, e);
        }

        // Create a representation of the distributed file in the mediapackage
        MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
        try {
          distributedElement.setURI(getDistributionUri(mediaPackageId, element));
        } catch (URISyntaxException e) {
          throw new DistributionException("Distributed element produces an invalid URI", e);
        }
        distributedElement.setIdentifier(null);

        job.setElement(distributedElement);
        job.setStatus(Status.FINISHED);
        updateJob(job);

        logger.info("Finished distribution of {}", element);
        return null;
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        try {
          job.setStatus(Status.FAILED);
          updateJob(job);
        } catch (Exception failureToFail) {
          logger.warn("Unable to update job to failed state", failureToFail);
        }
        if (e instanceof DistributionException) {
          throw (DistributionException) e;
        } else {
          throw new DistributionException(e);
        }
      }
    }
    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(java.lang.String)
   */
  @Override
  public Job retract(final String mediaPackageId, boolean block) throws DistributionException {
    final Job job;
    try {
      job = serviceRegistry.createJob(JOB_TYPE);
    } catch (ServiceUnavailableException e) {
      throw new DistributionException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }

    Callable<Void> command = new Callable<Void>() {
      /**
       * {@inheritDoc}
       * 
       * @see java.util.concurrent.Callable#call()
       */
      @Override
      public Void call() throws DistributionException {
        job.setStatus(Status.RUNNING);
        updateJob(job);
        if (!FileSupport.delete(getMediaPackageDirectory(mediaPackageId), true)) {
          throw new DistributionException("Unable to retract mediapackage " + mediaPackageId);
        }
        job.setStatus(Status.FINISHED);
        updateJob(job);
        logger.info("Finished rectracting media package {}", mediaPackageId);
        return null;
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        try {
          job.setStatus(Status.FAILED);
          updateJob(job);
        } catch (Exception failureToFail) {
          logger.warn("Unable to update job to failed state", failureToFail);
        }
        if (e instanceof DistributionException) {
          throw (DistributionException) e;
        } else {
          throw new DistributionException(e);
        }
      }
    }
    return job;
  }

  /**
   * Callback for the OSGi environment to set the workspace reference.
   * 
   * @param workspace
   *          the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi environment to set the service registry reference.
   * 
   * @param remoteServiceManager
   *          the service registry
   */
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.serviceRegistry = remoteServiceManager;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJob(java.lang.String)
   */
  public Job getJob(String id) throws NotFoundException, ServiceRegistryException {
    return serviceRegistry.getJob(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  public long countJobs(Status status) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    return serviceRegistry.count(JOB_TYPE, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  public long countJobs(Status status, String host) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    if (host == null)
      throw new IllegalArgumentException("host must not be null");
    return serviceRegistry.count(JOB_TYPE, status, host);
  }

  /**
   * Gets the destination file to copy the contents of a mediapackage element.
   * 
   * @param element
   *          The mediapackage element being distributed
   * @return The file to copy the content to
   */
  protected File getDistributionFile(String mediaPackageId, MediaPackageElement element) {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String directoryName = distributionDirectory.getAbsolutePath();
    String destinationFileName = PathSupport
            .concat(new String[] { directoryName, mediaPackageId, elementId, fileName });
    return new File(destinationFileName);
  }

  /**
   * Gets the URI for the element to be distributed.
   * 
   * @param mediaPackageId
   *          the mediapackage identifier
   * @param element
   *          The mediapackage element being distributed
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected URI getDistributionUri(String mediaPackageId, MediaPackageElement element) throws URISyntaxException {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String destinationURI = UrlSupport.concat(new String[] { serviceUrl, mediaPackageId, elementId, fileName });
    return new URI(destinationURI);
  }

  /**
   * Gets the directory containing the distributed files for this mediapackage.
   * 
   * @param mediaPackageId
   *          the mediapackage ID
   * @return the filesystem directory
   */
  protected File getMediaPackageDirectory(String mediaPackageId) {
    return new File(distributionDirectory, mediaPackageId);
  }

  /**
   * Updates the job in the service registry. The exceptions that are possibly been thrown are wrapped in a
   * {@link DistributionException}.
   * 
   * @param job
   *          the job to update
   * @throws DistributionException
   *           the exception that is being thrown
   */
  private void updateJob(Job job) throws DistributionException {
    try {
      serviceRegistry.updateJob(job);
    } catch (NotFoundException notFound) {
      throw new DistributionException("Unable to find job " + job, notFound);
    } catch (ServiceUnavailableException e) {
      throw new DistributionException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException serviceRegException) {
      throw new DistributionException("Unable to update job '" + job + "' in service registry", serviceRegException);
    }
  }

}
