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
package org.opencastproject.ingest.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

/**
 * Generates {@link MediaPackage}s from media, metadata, and attachments.
 */
public interface IngestService {

  /**
   * Add an existing compressed MediaPackage to the repository.
   * 
   * @param ZippedMediaPackage
   *          A zipped file containing manifest, tracks, catalogs and attachments
   * @return Workflow instance.
   * @throws MediaPackageException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws Exception
   */
  WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage) throws MediaPackageException,
          FileNotFoundException, IOException, Exception;

  /**
   * Add an existing compressed MediaPackage to the repository.
   * 
   * @param ZippedMediaPackage
   *          A zipped file containing manifest, tracks, catalogs and attachments
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @return Workflow instance.
   * @throws MediaPackageException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws Exception
   */
  WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage, String workflowDefinitionID)
          throws MediaPackageException, FileNotFoundException, IOException, Exception;

  /**
   * Add an existing compressed MediaPackage to the repository.
   * 
   * @param ZippedMediaPackage
   *          A zipped file containing manifest, tracks, catalogs and attachments
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @param wfConfig
   *          configuration parameters for the workflow
   * @return Workflow instance.
   * @throws MediaPackageException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws Exception
   */
  WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage, String workflowDefinitionID,
          Map<String, String> wfConfig) throws MediaPackageException, FileNotFoundException, IOException, Exception;

  /**
   * Create a new MediaPackage in the repository.
   * 
   * @return The created MediaPackage
   * @throws MediaPackageException
   * @throws HandleException
   * @throws ConfigurationException
   * @throws IOException
   */
  MediaPackage createMediaPackage() throws MediaPackageException, ConfigurationException, HandleException, IOException;

  /**
   * Add a media track to an existing MediaPackage in the repository
   * 
   * @param uri
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackageManifest The manifest of a specific Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @throws IOException
   */
  MediaPackage addTrack(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException;

  /**
   * Add a media track to an existing MediaPackage in the repository
   * 
   * @param mediaFile
   *          The media file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @throws MalformedURLException
   * @throws IOException
   */
  MediaPackage addTrack(InputStream mediaFile, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException;

  /**
   * Add a [metadata catalog] to an existing MediaPackage in the repository
   * 
   * @param uri
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @throws IOException
   */
  MediaPackage addCatalog(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException, NotFoundException;

  /**
   * Add a [metadata catalog] to an existing MediaPackage in the repository
   * 
   * @param catalog
   *          The catalog file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @throws MalformedURLException
   * @throws IOException
   */
  MediaPackage addCatalog(InputStream catalog, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException, NotFoundException;

  /**
   * Add an attachment to an existing MediaPackage in the repository
   * 
   * @param uri
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @throws IOException
   */
  MediaPackage addAttachment(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException;

  /**
   * Add an attachment to an existing MediaPackage in the repository
   * 
   * @param file
   *          The file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @throws MalformedURLException
   * @throws IOException
   */
  MediaPackage addAttachment(InputStream file, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException;

  /**
   * Broadcasts an event, that media package is ingested. After broadcast ACK message is expected from ConductorService.
   * If message contains exception, it will be thrown.
   * 
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @return Workflow instance id.
   * @throws Exception
   *           Exception that occured during MediaPackage serialization or happened in WorkflowService during
   *           MediaPackage processing
   */
  WorkflowInstance ingest(MediaPackage mediaPackage) throws IllegalStateException, Exception;

  /**
   * Broadcasts an event, that media package is ingested. After broadcast ACK message is expected from ConductorService.
   * If message contains exception, it will be thrown.
   * 
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @return Workflow instance id.
   * @throws Exception
   *           Exception that occured during MediaPackage serialization or happened in ConductorService durring
   *           MediaPackage processing
   */
  WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID) throws IllegalStateException,
          Exception;

  /**
   * Broadcasts an event, that media package is ingested. After broadcast ACK message is expected from ConductorService.
   * If message contains exception, it will be thrown.
   * 
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @param properties
   *          configuration properties for the workflow
   * @return Workflow instance id.
   * @throws Exception
   *           Exception that occured during MediaPackage serialization or happened in ConductorService durring
   *           MediaPackage processing
   */
  WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID, Map<String, String> properties)
          throws IllegalStateException, Exception;

  /**
   * Delete an existing MediaPackage and any linked files from the temporary ingest filestore.
   * 
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage
   */
  void discardMediaPackage(MediaPackage mediaPackage) throws IOException;

}
