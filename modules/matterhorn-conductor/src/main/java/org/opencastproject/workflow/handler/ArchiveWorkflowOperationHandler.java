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
package org.opencastproject.workflow.handler;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.ZipUtil;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import de.schlichtherle.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Produces a zipped archive of a mediapackage, places it in the archive collection, and removes the rest of the
 * mediapackage elements from both the mediapackage xml and if possible, from storage altogether.
 */
public class ArchiveWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ArchiveWorkflowOperationHandler.class);

  /** The workflow operation's property to consult to determine the collection to use to store an archive */
  public static final String ARCHIVE_COLLECTION_PROPERTY = "archive-collection";

  /** The element flavors to maintain in the original mediapackage */
  public static final String PRESERVE_FLAVOR_PROPERTY = "preserve-flavors";

  /** The property indicating whether to apply compression to the archive */
  public static final String COMPRESS_PROPERTY = "compression";

  /** The default collection in the working file repository to store archives */
  public static final String DEFAULT_ARCHIVE_COLLECTION = "archive";

  /** The temporary location to use when building an archive */
  public static final String ARCHIVE_TEMP_DIR = "archive-temp";

  /** The flavor to use for a mediapackage archive */
  public static final MediaPackageElementFlavor ARCHIVE_FLAVOR = MediaPackageElementFlavor.parseFlavor("archive/zip");

  /** The temporary storage location */
  protected File tempStorageDir = null;

  /** The configuration properties */
  protected SortedMap<String, String> configurationOptions = null;

  /**
   * The workspace to use in retrieving and storing files.
   */
  protected Workspace workspace;

  /** The thread pool */
  protected ExecutorService executorService;
  
  /** The default no-arg constructor builds the configuration options set */
  public ArchiveWorkflowOperationHandler() {
    configurationOptions = new TreeMap<String, String>();
    configurationOptions.put(ARCHIVE_COLLECTION_PROPERTY,
            "The configuration key that specifies the archive collection.  Defaults to " + DEFAULT_ARCHIVE_COLLECTION);
    configurationOptions.put(COMPRESS_PROPERTY,
            "The configuration key that specifies whether to compress the zip archive.  Defaults to false.");
    configurationOptions.put(PRESERVE_FLAVOR_PROPERTY,
            "The configuration key that specifies the element flavors to preserve in the original "
                    + "mediapackage.  Any flavors not specified here will remain only as part of the zipped "
                    + "mediapackage archive");
  }

  
  /**
   * Sets the workspace to use.
   * 
   * @param workspace
   *          the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Activate the component, generating the temporary storage directory for building zip archives if necessary.
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate(org.osgi.service.component.ComponentContext)
   */
  protected void activate(ComponentContext cc) {
    tempStorageDir = new File(cc.getBundleContext().getProperty("org.opencastproject.storage.dir"), ARCHIVE_TEMP_DIR);
    if (!tempStorageDir.isDirectory()) {
      try {
        FileUtils.forceMkdir(tempStorageDir);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    int maxThreads = 1;
    if(cc.getBundleContext().getProperty("archive.threads") != null) {
      try {
        maxThreads = Integer.parseInt(cc.getBundleContext().getProperty("archive.threads"));
      } catch(NumberFormatException e) {
        logger.warn("Illegal value set for archive.threads.  Using default value of 1 archive operation at a time.");
      }
    }
    this.executorService = Executors.newFixedThreadPool(maxThreads);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    // Callable allows us to throw checked exceptions, where Runnable does not
    Callable<Void> callable = new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        logger.info("Archiving mediapackage {} in workflow {}", mediaPackage, workflowInstance);

        String compressProperty = currentOperation.getConfiguration(COMPRESS_PROPERTY);
        boolean compress = compressProperty == null ? false : Boolean.valueOf(compressProperty);

        // Zip the contents of the mediapackage
        File zip = null;
        try {
          zip = zip(mediaPackage, compress);
        } catch (Exception e) {
          throw new WorkflowOperationException("Unable to create a zip archive from mediapackage " + mediaPackage, e);
        }

        // Get the collection for storing the archived mediapackage
        String configuredCollectionId = currentOperation.getConfiguration(ARCHIVE_COLLECTION_PROPERTY);
        String collectionId = configuredCollectionId == null ? DEFAULT_ARCHIVE_COLLECTION : configuredCollectionId;

        // Add the zip as an attachment to the mediapackage
        logger.info("Adding zipped mediapackage {} to the {} archive", mediaPackage, collectionId);

        InputStream in = null;
        URI uri = null;
        try {
          in = new FileInputStream(zip);
          uri = workspace.putInCollection(collectionId, mediaPackage.getIdentifier().compact() + ".zip", in);
        } catch (FileNotFoundException e) {
          throw new WorkflowOperationException("zip file " + zip + " not found", e);
        } catch (IOException e) {
          throw new WorkflowOperationException(e);
        } finally {
          IOUtils.closeQuietly(in);
        }
        logger.info("Zipped mediapackage {} moved to the {} archive", mediaPackage, collectionId);

        Attachment attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(uri, Type.Attachment, ARCHIVE_FLAVOR);
        try {
          attachment.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, zip));
        } catch (NoSuchAlgorithmException e) {
          throw new WorkflowOperationException(e);
        } catch (IOException e) {
          throw new WorkflowOperationException(e);
        }

        // Parse the flavors to remove
        List<String> flavorsToKeep = asList(currentOperation.getConfiguration(PRESERVE_FLAVOR_PROPERTY));
        
        // The zip file is safely in the archive, so it's now safe to attempt to remove mediapackage elements and delete
        // the files, if possible
        try {
          FileUtils.forceDelete(zip);
          for (MediaPackageElement element : mediaPackage.getElements()) {
            if( element.getFlavor() != null && ! flavorsToKeep.contains(element.getFlavor().toString())) {
              workspace.delete(mediaPackage.getIdentifier().toString(), element.getIdentifier());
              mediaPackage.remove(element);
            }
          }
        } catch (Exception e) {
          throw new WorkflowOperationException(e);
        }
        mediaPackage.add(attachment);
        return null;
      }
    };
    Future<Void> future = executorService.submit(callable);
    try {
      future.get();
    } catch(Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException)e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediaPackage, Action.CONTINUE);
  }

  /**
   * Creates a zip archive of all elements in a mediapackage.
   * 
   * @param mediaPackage
   *          the mediapackage to zip
   * 
   * @return the zip file
   * 
   * @throws IOException
   *           If an IO exception occurs
   * @throws NotFoundException
   *           If a file referenced in the mediapackage can not be found
   * @throws MediaPackageException
   *           If the mediapackage can not be serialized to xml
   */
  protected File zip(MediaPackage mediaPackage, boolean compress) throws IOException, NotFoundException, MediaPackageException {
    // Create the temp directory
    File mediaPackageDir = new File(tempStorageDir, mediaPackage.getIdentifier().compact());
    FileUtils.forceMkdir(mediaPackageDir);

    // Link or copy each file from the workspace to the temp directory
    MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl(mediaPackageDir);
    MediaPackage clone = (MediaPackage) mediaPackage.clone();
    for (MediaPackageElement element : clone.getElements()) {
      File elementDir = new File(mediaPackageDir, element.getIdentifier());
      FileUtils.forceMkdir(elementDir);
      File workspaceFile = workspace.get(element.getURI());
      File linkedFile = FileSupport.link(workspaceFile, new File(elementDir, workspaceFile.getName()), true);
      try {
        element.setURI(new URI(serializer.encodeURI(linkedFile.toURI())));
      } catch (URISyntaxException e) {
        throw new MediaPackageException("unable to serialize a mediapackage element", e);
      }
    }

    // Add the manifest
    FileUtils.writeStringToFile(new File(mediaPackageDir, "manifest.xml"), clone.toXml(), "UTF-8");

    // Zip the directory
    File zip = new File(tempStorageDir, clone.getIdentifier().compact() + ".zip");
    int compressValue = compress ? ZipEntry.DEFLATED : ZipEntry.STORED;
    
    long startTime = System.currentTimeMillis();
    ZipUtil.zip(new File[] { mediaPackageDir }, zip, true, compressValue);
    long stopTime = System.currentTimeMillis();
    
    logger.debug("Zip file creation took {} seconds", (stopTime - startTime)/1000);
    
    // Remove the directory
    FileUtils.forceDelete(mediaPackageDir);

    // Return the zip
    return zip;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return configurationOptions;
  }
}
