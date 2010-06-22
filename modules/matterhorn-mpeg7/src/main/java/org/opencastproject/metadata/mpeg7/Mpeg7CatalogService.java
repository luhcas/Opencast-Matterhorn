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
package org.opencastproject.metadata.mpeg7;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.api.CatalogService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Loads {@link Mpeg7Catalog}s
 */
public class Mpeg7CatalogService implements CatalogService<Mpeg7Catalog> {

  protected Workspace workspace = null;
  
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.metadata.api.CatalogService#accepts(org.opencastproject.mediapackage.Catalog)
   */
  @Override
  public boolean accepts(Catalog catalog) {
    if(catalog == null) throw new IllegalArgumentException("Catalog must not be null");
    MediaPackageElementFlavor flavor = catalog.getFlavor();
    return flavor != null && (flavor.equals(Mpeg7Catalog.ANY_MPEG7));
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.metadata.api.CatalogService#serialize(org.opencastproject.metadata.api.MetadataCatalog)
   */
  @Override
  public InputStream serialize(Mpeg7Catalog catalog) throws IOException {
    try {
      Transformer tf = TransformerFactory.newInstance().newTransformer();
      DOMSource xmlSource = new DOMSource(catalog.toXml());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      tf.transform(xmlSource, new StreamResult(out));
      return new ByteArrayInputStream(out.toByteArray());
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.api.CatalogService#load(org.opencastproject.mediapackage.Catalog)
   */
  @Override
  public Mpeg7Catalog load(Catalog catalog) throws IOException, IllegalArgumentException, IllegalStateException {
    if(catalog == null) throw new IllegalArgumentException("Catalog must not be null");
    URI uri = catalog.getURI();
    if(uri == null) throw new IllegalStateException("Found catalog without a URI");
    FileInputStream is = null;
    try {
      File file = workspace.get(uri);
      is = new FileInputStream(file);
      return new Mpeg7CatalogImpl(is);
    } catch (NotFoundException e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.api.CatalogService#newInstance()
   */
  @Override
  public Mpeg7Catalog newInstance() {
    return new Mpeg7CatalogImpl();
  }

}
