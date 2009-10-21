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

package org.opencastproject.media.mediapackage;

import org.opencastproject.media.mediapackage.handle.Handle;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.util.ConfigurationException;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

/**
 * This class provides factory methods for the creation of media packages from manifest files, directories or from
 * scratch.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageBuilderImpl.java 2908 2009-07-17 16:51:07Z ced $
 */
public class MediaPackageBuilderImpl implements MediaPackageBuilder {

  /** The media package serializer */
  protected MediaPackageSerializer serializer = null;

  /**
   * Creates a new media package builder.
   * 
   * @throws IllegalStateException
   *           if the temporary directory cannot be created or is not accessible
   */
  public MediaPackageBuilderImpl() {
    this(new DefaultMediaPackageSerializerImpl());
  }

  /**
   * Creates a new media package builder that uses the given serializer to resolve urls while reading manifests and
   * adding new elements.
   * 
   * @param serializer
   *          the media package serializer
   * @throws IllegalStateException
   *           if the temporary directory cannot be created or is not accessible
   */
  public MediaPackageBuilderImpl(MediaPackageSerializer serializer) {
    if (serializer == null)
      throw new IllegalArgumentException("Serializer may not be null");
    this.serializer = serializer;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#createNew()
   */
  public MediaPackage createNew() throws MediaPackageException {
    return new MediaPackageImpl();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#createNew(org.opencastproject.media.mediapackage.handle.Handle)
   */
  public MediaPackage createNew(Handle identifier) throws MediaPackageException {
    return new MediaPackageImpl(identifier);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#loadFromManifest(java.io.InputStream)
   */
  public MediaPackage loadFromManifest(InputStream is) throws MediaPackageException {
    ManifestImpl manifest = null;

    // Read the manifest
    try {
      manifest = ManifestImpl.fromStream(is, serializer, false);
    } catch (IOException e) {
      throw new MediaPackageException("I/O error while accessing manifest: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Media package manifest cannot be parsed: " + e.getMessage());
    } catch (TransformerException e) {
      throw new MediaPackageException("Error while updating media package manifest: " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Media package manifest cannot be parsed: " + e.getMessage());
    } catch (ConfigurationException e) {
      throw new MediaPackageException("Configuration error while accessing manifest: " + e.getMessage());
    } catch (HandleException e) {
      throw new MediaPackageException("Handle system error while accessing manifest: " + e.getMessage());
    } catch (ParseException e) {
      throw new MediaPackageException("Error while parsing invalid media package start date: " + e.getMessage());
    }

    return new MediaPackageImpl(manifest);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#loadFromPackage(org.opencastproject.media.mediapackage.MediaPackagePackager,
   *      java.io.InputStream)
   */
  public MediaPackage loadFromPackage(MediaPackagePackager packager, InputStream in) throws IOException,
          MediaPackageException {
    if (packager == null)
      throw new IllegalArgumentException("The packager must not be null");
    if (in == null)
      throw new IllegalArgumentException("The input stream must not be null");
    return packager.unpack(in);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#getSerializer()
   */
  public MediaPackageSerializer getSerializer() {
    return serializer;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageBuilder#setSerializer(org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public void setSerializer(MediaPackageSerializer serializer) {
    this.serializer = serializer;
  }

}
