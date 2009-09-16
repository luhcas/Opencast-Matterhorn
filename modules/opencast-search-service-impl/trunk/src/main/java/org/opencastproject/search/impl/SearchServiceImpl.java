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
package org.opencastproject.search.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * FIXME -- Add javadocs
 */
public class SearchServiceImpl implements SearchService {
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

  protected String indexDirectory = System.getProperty("java.io.tmpdir") + File.separator + "opencast" +
    File.separator + "searchindex";

  public void activate(ComponentContext componentContext) {
    try {
      logger.info("Initializing " + this.getClass().getName() + " with search index directory " + indexDirectory);
      File dir = new File(indexDirectory);
      if(! dir.exists()) {
        FileUtils.forceMkdir(dir);
        dir = new File(indexDirectory);
      }
      IndexWriter writer = new IndexWriter(indexDirectory, new StandardAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
      System.out.println("Indexing to directory '" + indexDirectory + "'...");
      System.out.println("Optimizing...");
      writer.optimize();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }   
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#getSearchResults(java.lang.String)
   */
  public List<SearchResult> getSearchResults(String query) {
    // FIXME Actually search for search results
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#index(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public void index(MediaPackage mediaPackage) {
    logger.info("Indexing media package " + mediaPackage.getIdentifier());
    // FIXME Actually index the mediapackage
  }
}

