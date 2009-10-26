/**
 *  Copyright 2009 The Regents of the University of California
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

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Generates {@link MediaPackage}s from media, metadata, and attachments.
 */
public interface IngestService {

  /**
   * Add an existing MediaPackage to the repository
   * 
   * @param MediaPackageManifest
   *          A manifest of a MediaPackage
   * @return MediaPackageManifest The manifest of a specific Matterhorn MediaPackage element
   * @throws MediaPackageException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws Exception
   */
  MediaPackage addMediaPackage(InputStream MediaPackageManifest) 
          throws MediaPackageException, FileNotFoundException, IOException, Exception;

  /**
   * Create a new MediaPackage in the repository.
   * 
   * @return The created MediaPackage
   * @throws MediaPackageException
   * @throws HandleException
   * @throws ConfigurationException
   */
  MediaPackage createMediaPackage() throws MediaPackageException, ConfigurationException, HandleException;

  /**
   * Add a media track to an existing MediaPackage in the repository
   * 
   * @param url
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackageManifest The manifest of a specific Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   */
  MediaPackage addTrack(URL url, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException;

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
   */
  MediaPackage addTrack(InputStream mediaFile, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, MalformedURLException;

  /**
   * Add a [metadata catalog] to an existing MediaPackage in the repository
   * 
   * @param url
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   */
  MediaPackage addCatalog(URL url, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException;

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
   */
  MediaPackage addCatalog(InputStream catalog, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, MalformedURLException;

  /**
   * Add an attachment to an existing MediaPackage in the repository
   * 
   * @param url
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   */
  MediaPackage addAttachment(URL url, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException;

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
   */
  MediaPackage addAttachment(InputStream file, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, MalformedURLException;

  /**
   * Copy all files linked to an existing MediaPackage to the repository.
   * 
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @return MediaPackage A Matterhorn MediaPackage (via EventAdmin)
   */
  void ingest(MediaPackage mediaPackage);

  /**
   * Delete an existing MediaPackage and any linked files from the temporary ingest filestore.
   * 
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage
   */
  void discardMediaPackage(MediaPackage mediaPackage);

  /**
   * Injects the working file repository to be used by the ingest service
   * 
   * @param repo
   *          The repository to be used
   */
  void setWorkingFileRepository(WorkingFileRepository repo);
  
  /**
   * Injects the media inspection service to be used by the ingest service
   * 
   * @param inspection
   *          The inspection service to be used
   */
  void setMediaInspection(MediaInspectionService inspection);
}
