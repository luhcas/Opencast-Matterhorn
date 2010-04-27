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
package org.opencastproject.metadata.dublincore;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageMetadata;
import org.opencastproject.media.mediapackage.MediapackageMetadataImpl;
import org.opencastproject.metadata.api.CatalogService;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.security.api.TrustedHttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Parses {@link DublinCoreCatalog}s from serialized DC representations
 *
 */
public class DublinCoreCatalogService implements CatalogService<DublinCoreCatalog>, MediaPackageMetadataService {
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogService.class);

  protected int priority = 0;

  protected TrustedHttpClient trustedHttpClient;
  
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }
  
  @SuppressWarnings("unchecked")
  public void activate(Map properties) {
    logger.debug("activate()");
    if(properties != null) {
      String priorityString = (String)properties.get(PRIORITY_KEY);
      if(priorityString != null) {
        try {
          priority = Integer.parseInt(priorityString);
        } catch (NumberFormatException e) {
          logger.warn("Unable to set priority to {}", priorityString );
          throw e;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.metadata.api.CatalogService#getMetadata(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackageMetadata getMetadata(MediaPackage mp) {
    MediapackageMetadataImpl metadata = new MediapackageMetadataImpl();
    
    Catalog[] dcs = mp.getCatalogs(DublinCoreCatalog.FLAVOR);
    for (Catalog catalog : dcs) {
      DublinCoreCatalog dc = load(catalog);
      if (catalog.getReference() == null) {
        // Title
        metadata.setTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));

        // Created date
        if (dc.hasValue(DublinCore.PROPERTY_CREATED))
          metadata.setDate(EncodingSchemeUtils.decodeDate(dc.get(
                  DublinCore.PROPERTY_CREATED).get(0)));

        // Series id
        if (dc.hasValue(DublinCore.PROPERTY_IS_PART_OF))
          metadata.setSeriesIdentifier(dc.get(DublinCore.PROPERTY_IS_PART_OF).get(0).getValue());

        // Creator
        if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
          for (DublinCoreValue creator : dc.get(DublinCore.PROPERTY_CREATOR)) {
            mp.addCreator(creator.getValue());
          }
        }

        // Contributor
        if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
          for (DublinCoreValue contributor : dc
                  .get(DublinCore.PROPERTY_CONTRIBUTOR)) {
            mp.addContributor(contributor.getValue());
          }
        }

        // Subject
        if (dc.hasValue(DublinCore.PROPERTY_SUBJECT)) {
          for (DublinCoreValue subject : dc.get(DublinCore.PROPERTY_SUBJECT)) {
            mp.addSubject(subject.getValue());
          }
        }

        // License
        metadata.setLicense(dc.getFirst(DublinCore.PROPERTY_LICENSE));

        // Language
        metadata.setLanguage(dc.getFirst(DublinCore.PROPERTY_LANGUAGE));

        break;
      } else {
        // Series Title
        metadata.setSeriesTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));
      }
    }
    return metadata;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.metadata.api.CatalogService#load(org.opencastproject.media.mediapackage.Catalog)
   */
  @Override
  public DublinCoreCatalog load(Catalog catalog) {
    DublinCoreCatalogImpl cat = new DublinCoreCatalogImpl(catalog);
    cat.trustedHttpClient = trustedHttpClient;
    return cat;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.metadata.api.CatalogService#newInstance()
   */
  @Override
  public DublinCoreCatalog newInstance() {
    return new DublinCoreCatalogImpl();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.metadata.api.CatalogService#getPriority()
   */
  @Override
  public int getPriority() {
    return priority;
  }

}
