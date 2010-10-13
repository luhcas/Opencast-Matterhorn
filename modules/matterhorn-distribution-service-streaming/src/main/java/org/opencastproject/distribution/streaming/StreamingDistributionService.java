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
package org.opencastproject.distribution.streaming;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Distributes media to the local media delivery directory.
 */
public class StreamingDistributionService implements DistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(StreamingDistributionService.class);

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.streaming";

  /** Default distribution directory */
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator;

  /** The workspace reference */
  protected Workspace workspace = null;

  /** The service registry */
  protected ServiceRegistry remoteServiceManager = null;

  /** The distribution directory */
  protected File distributionDirectory = null;

  /** The base URL for streaming */
  protected String streamingUrl = null;

  /** The executor service used to queue and run jobs */
  protected ExecutorService executor = null;

  protected void activate(ComponentContext cc) {
    // Get the configured streaming and server URLs
    if (cc != null) {
      streamingUrl = cc.getBundleContext().getProperty("org.opencastproject.streaming.url");
      if (streamingUrl == null)
        throw new IllegalStateException("Stream url must be set (org.opencastproject.streaming.url)");
      logger.info("streaming url is {}", streamingUrl);

      distributionDirectory = new File(cc.getBundleContext().getProperty("org.opencastproject.streaming.directory"));
      if (distributionDirectory == null)
        throw new IllegalStateException("Distribution directory must be set (org.opencastproject.streaming.directory)");
      logger.info("Streaming distribution directory is {}", distributionDirectory);

      int threads = 1;
      String threadsConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(
              "org.opencastproject.distribution.streaming.threads"));
      if (threadsConfig != null) {
        try {
          threads = Integer.parseInt(threadsConfig);
        } catch (NumberFormatException e) {
          logger.warn("streaming distribution threads configuration is malformed: '{}'", threadsConfig);
        }
      }
      executor = Executors.newFixedThreadPool(threads);
    }
  }

  /**
   * Called when service deactivates. Defined in OSGi resource file.
   */
  public void deactivate() {
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
    final ServiceRegistry rs = remoteServiceManager;
    final Job receipt = rs.createJob(JOB_TYPE);

    Runnable command = new Runnable() {
      public void run() {
        receipt.setStatus(Status.RUNNING);
        rs.updateJob(receipt);

        try {

          // The streaming server only supports tracks
          if (!(element instanceof Track)) {
            receipt.setStatus(Status.FINISHED);
            rs.updateJob(receipt);
            return;
          }

          File sourceFile = workspace.get(element.getURI());
          File destination = getDistributionFile(element);

          // Put the file in place
          FileUtils.forceMkdir(destination.getParentFile());
          logger.info("Distributing {} to {}", element, destination);

          FileSupport.copy(sourceFile, destination);

          // Create a representation of the distributed file in the mediapackage
          MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
          distributedElement.setURI(getDistributionUri(element));
          distributedElement.setIdentifier(null);

          receipt.setElement(distributedElement);
          receipt.setStatus(Status.FINISHED);
          rs.updateJob(receipt);

          logger.info("Finished distribution of {}", element);

        } catch (Exception e) {
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw new DistributionException(e);
        }
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        receipt.setStatus(Status.FAILED);
        remoteServiceManager.updateJob(receipt);
        throw new DistributionException(e);
      }
    }

    return receipt;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(java.lang.String)
   */
  @Override
  public Job retract(final String mediaPackageId, boolean block) throws DistributionException {
    final ServiceRegistry rs = remoteServiceManager;
    final Job receipt = rs.createJob(JOB_TYPE);

    Runnable command = new Runnable() {
      public void run() {
        receipt.setStatus(Status.RUNNING);
        rs.updateJob(receipt);

        try {
          if (!FileSupport.delete(getMediaPackageDirectory(mediaPackageId), true)) {
            throw new DistributionException("Unable to retract mediapackage " + mediaPackageId);
          }

          receipt.setStatus(Status.FINISHED);
          rs.updateJob(receipt);

          logger.info("Finished rectracting media package {}", mediaPackageId);

        } catch (DistributionException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw e;
        } catch (Exception e) {
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw new DistributionException(e);
        }
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        receipt.setStatus(Status.FAILED);
        remoteServiceManager.updateJob(receipt);
        throw new DistributionException(e);
      }
    }

    return receipt;
  }

  /**
   * Gets the destination file to copy the contents of a mediapackage element.
   * 
   * @param element
   *          The mediapackage element being distributed
   * @return The file to copy the content to
   */
  protected File getDistributionFile(MediaPackageElement element) {
    String mediaPackageId = element.getMediaPackage().getIdentifier().compact();
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
   * @param element
   *          The mediapackage element being distributed
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected URI getDistributionUri(MediaPackageElement element) throws URISyntaxException {
    String mediaPackageId = element.getMediaPackage().getIdentifier().compact();
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String destinationURI = UrlSupport.concat(new String[] { streamingUrl, mediaPackageId, elementId, fileName });
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
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJob(java.lang.String)
   */
  public Job getJob(String id) {
    return remoteServiceManager.getJob(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  public long countJobs(Status status) {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    return remoteServiceManager.count(JOB_TYPE, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status,
   *      java.lang.String)
   */
  public long countJobs(Status status, String host) {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    if (host == null)
      throw new IllegalArgumentException("host must not be null");
    return remoteServiceManager.count(JOB_TYPE, status, host);
  }

}
