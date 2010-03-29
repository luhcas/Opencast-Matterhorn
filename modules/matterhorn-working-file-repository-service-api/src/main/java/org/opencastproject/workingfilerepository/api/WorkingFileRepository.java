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
package org.opencastproject.workingfilerepository.api;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The Working File Repository is a file storage service that supports the lecture capture system.
 * It may be used by other clients, but is neither intended nor required to be used by other systems.
 */
public interface WorkingFileRepository {
  /**
   * Store the data stream under the given media package and element IDs. Name of the
   * file is equal to element ID.
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @param in
   */
  URI put(String mediaPackageID, String mediaPackageElementID, InputStream in);
  
  /**
   * Store the data stream under the given media package and element IDs with filename as
   * name of the file.
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @param filename
   * @param in
   * @return The URL to access this file
   */
  URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in);
  
  /**
   * Stream the file stored under the given media package and element IDs.
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @return
   */
  InputStream get(String mediaPackageID, String mediaPackageElementID);
  
  /**
   * Get the URL of the file stored under the given media package and element IDs, or null if it does not exist.
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @return
   */
  URI getURI(String mediaPackageID, String mediaPackageElementID);
  
  /**
   * Delete the file stored at the given media package and element IDs.
   * @param mediaPackageID
   * @param mediaPackageElementID
   */
  void delete(String mediaPackageID, String mediaPackageElementID);

  /**
   * Gets the number of files in a collection.
   * @param  the collection identifier
   * @return the number of files in a collection
   * @throws IllegalArgumentException if the collection does not exist
   */
  long getCollectionSize(String id);
  
  /**
   * Puts a file into a collection, overwriting the existing file if present.
   * @param collection The collection identifier
   * @param fileName The filename to use in storing the input stream
   * @param in the data to store
   * @return The URI identifying the file
   * @throws URISyntaxException if either the filename or collection ID are can not be included in a valid URI
   */
  URI putInCollection(String collectionId, String fileName, InputStream in) throws URISyntaxException;

  /**
   * Gets the URIs of the members of this collection
   * @param collectionId the collection identifier
   * @return the URIs for each member of the collection
   */
  URI[] getCollectionContents(String collectionId);
  
  /**
   * Gets data from a collection
   * @param collectionId the collection identifier
   * @param fileName The filename to retrieve
   * @return the data as a stream, or null if not found
   */
  InputStream getFromCollection(String collectionId, String fileName);
  
  /**
   * Removes a file from a collection
   * @param collectionId the collection identifier
   * @param fileName the filename to remove
   */
  void removeFromCollection(String collectionId, String fileName);

  /**
   * Moves a file from a collection into a mediapackage
   * @param fromCollection The collection holding the file
   * @param fromFileName The filename
   * @param toMediaPackage The media package ID to move the file into
   * @param toMediaPackageElement the media package element ID of the file
   * @return the URI pointing to the file's new location
   */
  URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement);

  /**
   * Copies a file from a collection into a mediapackage
   * @param fromCollection The collection holding the file
   * @param fromFileName The filename
   * @param toMediaPackage The media package ID to copy the file into
   * @param toMediaPackageElement the media package element ID of the file
   * @return the URI pointing to the file's new location
   */
  URI copyTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement);
}
