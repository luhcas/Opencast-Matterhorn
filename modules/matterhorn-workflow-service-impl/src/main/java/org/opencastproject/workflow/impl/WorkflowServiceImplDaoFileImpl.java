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
package org.opencastproject.workflow.impl;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Provides data access to the workflow service through file storage in the workspace, indexed via lucene.
 */
public class WorkflowServiceImplDaoFileImpl implements WorkflowServiceImplDao {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImplDaoFileImpl.class);

  protected static final String COLLECTION_ID = "workflows";
  protected static final String COUNT_TOKEN = "COUNT_TOKEN";

  protected String storageRoot;
  protected Workspace workspace;
  protected Directory directory;
  protected Analyzer analyzer;

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setStorageRoot(String storageRoot) {
    this.storageRoot = storageRoot;
  }

  public void activate(ComponentContext cc) {
    String storageRoot = (String) cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
    logger.info("{}.activate() with storage root {}", WorkflowServiceImplDaoFileImpl.class.getName(), storageRoot);
    if (storageRoot == null)
      throw new IllegalStateException("storage directory not defined");
    File searchRoot = new File(storageRoot, "workflow");
    try {
      FileUtils.forceMkdir(searchRoot);
      this.storageRoot = searchRoot.getAbsolutePath();
    } catch (IOException e) {
      throw new IllegalStateException("unable to create storage directory: ", e);
    }
    activate();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#activate()
   */
  @Override
  public void activate() {
    analyzer = new StandardAnalyzer();
    IndexWriter indexWriter = null;
    try {
      directory = FSDirectory.getDirectory(storageRoot);
      indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
    } catch (Exception e) {
      logger.warn("unable to initialize lucene, clearing and rebuilding the search index", e);
      try {
        FileUtils.forceDelete(new File(storageRoot));
        FileUtils.forceMkdir(new File(storageRoot));
        directory = FSDirectory.getDirectory(storageRoot);
        indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
      } catch (IOException ioe) {
        logger.warn("Unable to rebuild the search index", ioe);
      }
    } finally {
      if (indexWriter != null) {
        try {
          indexWriter.close();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    }

    if (countWorkflowInstances() == 0) {
      // this may be a new index, so get all of the existing workflows and index them
      WorkflowBuilder builder = WorkflowBuilder.getInstance();
      URI[] uris = null;
      try {
        uris = workspace.getCollectionContents(COLLECTION_ID);
      } catch (IOException e) {
        throw new IllegalStateException("Error accessing workspace collection '" + COLLECTION_ID + "'", e);
      }
      if (uris.length > 0) {
        logger.info("The workflow search index is empty.  Populating it now with {} workflows.", uris.length);
      }
      for (URI uri : uris) {
        InputStream in = null;
        try {
          File file = workspace.get(uri);
          in = new FileInputStream(file);
          WorkflowInstance instance = builder.parseWorkflowInstance(in);
          index(instance);
        } catch (Exception e) {
          logger.warn("unable to parse workflow instance", e);
          throw new IllegalStateException(e);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }
      if (uris.length > 0) {
        logger.info("Finished populating the workflow search index with {} workflows.", uris.length);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#deactivate()
   */
  @Override
  public void deactivate() {
    try {
      directory.close();
    } catch (IOException e) {
      logger.warn("unable to close lucene index", e);
    }
  }

  public synchronized void index(WorkflowInstance instance) throws WorkflowDatabaseException {
    IndexWriter indexWriter = null;
    try {
      indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
      Document doc = getDocument(instance);
      indexWriter.updateDocument(new Term("id", instance.getId()), doc);
      indexWriter.expungeDeletes(true);
      indexWriter.commit();
    } catch (Exception e) {
      logger.warn("unable to index workflow", e);
      throw new WorkflowDatabaseException(e);
    } finally {
      if (indexWriter != null) {
        try {
          indexWriter.close();
        } catch (IOException e) {
          throw new WorkflowDatabaseException(e);
        }
      }
    }
  }

  protected Document getDocument(WorkflowInstance instance) throws Exception {
    Document doc = new Document();
    String xml = WorkflowBuilder.getInstance().toXml(instance);
    doc.add(new Field("id", instance.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("text", instance.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));

    doc.add(new Field("state", instance.getState().toString(), Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("text", instance.getState().toString(), Field.Store.YES, Field.Index.ANALYZED));

    WorkflowOperationInstance op = instance.getCurrentOperation();
    if (op != null)
      doc.add(new Field("operation", op.getId(), Field.Store.YES, Field.Index.ANALYZED));
    if (op != null)
      doc.add(new Field("text", op.getId(), Field.Store.YES, Field.Index.ANALYZED));
    MediaPackage mp = instance.getMediaPackage();
    doc.add(new Field("mp", mp.getIdentifier().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("text", mp.getIdentifier().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    if (mp.getSeries() != null) {
      doc.add(new Field("series", mp.getSeries(), Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field("text", mp.getSeries(), Field.Store.YES, Field.Index.ANALYZED));
    }
    if (mp.getTitle() != null) {
      doc.add(new Field("text", mp.getTitle(), Field.Store.YES, Field.Index.ANALYZED));
    }
    if (mp.getLicense() != null) {
      doc.add(new Field("text", mp.getLicense(), Field.Store.YES, Field.Index.ANALYZED));
    }
    if (mp.getLanguage() != null) {
      doc.add(new Field("text", mp.getLanguage(), Field.Store.YES, Field.Index.ANALYZED));
    }
    if (mp.getContributors() != null && mp.getContributors().length > 0) {
      for (String contributor : mp.getContributors()) {
        doc.add(new Field("text", contributor, Field.Store.YES, Field.Index.ANALYZED));
      }
    }
    if (mp.getCreators() != null && mp.getCreators().length > 0) {
      for (String creator : mp.getCreators()) {
        doc.add(new Field("text", creator, Field.Store.YES, Field.Index.ANALYZED));
      }
    }
    if (mp.getSubjects() != null && mp.getSubjects().length > 0) {
      for (String subject : mp.getSubjects()) {
        doc.add(new Field("text", subject, Field.Store.YES, Field.Index.ANALYZED));
      }
    }
    doc.add(new Field("xml", xml, Field.Store.YES, Field.Index.NOT_ANALYZED));

    // Finally, we add a known field so we can count the total number of workflows easily
    doc.add((new Field(COUNT_TOKEN, COUNT_TOKEN, Field.Store.YES, Field.Index.NOT_ANALYZED)));

    return doc;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    IndexSearcher searcher = null;
    try {
      searcher = new IndexSearcher(directory);
      TopDocs docs = searcher.search(new TermQuery(new Term(COUNT_TOKEN, COUNT_TOKEN)), 100000);
      return docs.totalHits;
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      try {
        if (searcher != null)
          searcher.close();
      } catch (IOException e) {
        throw new WorkflowDatabaseException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowById(java.lang.String)
   */
  @Override
  public WorkflowInstance getWorkflowById(String workflowId) throws WorkflowDatabaseException, NotFoundException {
    IndexSearcher isearcher = null;
    try {
      isearcher = new IndexSearcher(directory);
      Query q = new TermQuery(new Term("id", workflowId));
      TopDocs topDocs = isearcher.search(q, 1);
      if (topDocs.scoreDocs.length == 0) {
        throw new NotFoundException("No workflow instance with id=" + workflowId);
      }
      String xml = isearcher.doc(topDocs.scoreDocs[0].doc).get("xml");
      return WorkflowBuilder.getInstance().parseWorkflowInstance(xml);
    } catch (Exception e) {
      if(e instanceof NotFoundException) {
        throw (NotFoundException)e;
      } else if(e instanceof WorkflowDatabaseException) {
        throw (WorkflowDatabaseException)e;
      } else {
        throw new WorkflowDatabaseException(e);
      }
    } finally {
      if (isearcher != null) {
        try {
          isearcher.close();
        } catch (IOException e) {
          logger.warn("unable to close index searcher", e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
    int count = query.getCount() > 0 ? (int) query.getCount() : 20; // default to 20 items if not specified
    int startPage = query.getStartPage() > 0 ? (int) query.getStartPage() : 0; // default to page zero
    BooleanQuery q = new BooleanQuery();
    if (query.getMediaPackage() != null) {
      q.add(new TermQuery(new Term("mp", query.getMediaPackage())), Occur.MUST);
    }
    if (query.getSeries() != null) {
      q.add(new TermQuery(new Term("series", query.getSeries())), Occur.MUST);
    }
    if (query.getCurrentOperation() != null) {
      q.add(new TermQuery(new Term("operation", query.getCurrentOperation().toLowerCase())), Occur.MUST);
    }
    if (query.getState() != null) {
      q.add(new TermQuery(new Term("state", query.getState().toString().toLowerCase())), Occur.MUST);
    }
    if (query.getText() != null) {
      q.add(new TermQuery(new Term("text", query.getText().toLowerCase())), Occur.MUST);
    }
    if (q.getClauses().length == 0) {
      q.add(new TermQuery(new Term(COUNT_TOKEN, COUNT_TOKEN)), Occur.MUST);
    }
    IndexSearcher isearcher = null;
    long totalHits;
    long time = System.currentTimeMillis();
    WorkflowBuilder builder = WorkflowBuilder.getInstance();
    WorkflowSetImpl set = null;
    try {
      isearcher = new IndexSearcher(directory);
      TopDocs topDocs = isearcher.search(q, 100000);
      time = System.currentTimeMillis() - time;
      totalHits = topDocs.totalHits;
      ScoreDoc[] hits = topDocs.scoreDocs;
      // Iterate through the results
      int firstItem = startPage * count;
      int lastItem = (startPage * count) + count;

      set = new WorkflowSetImpl();
      set.setPageSize(count);
      set.setTotalCount(totalHits);
      set.setStartPage(query.getStartPage());
      set.setSearchTime(time);

      for (int i = firstItem; i < lastItem && i < hits.length; i++) {
        Document hitDoc = isearcher.doc(hits[i].doc);
        set.addItem(builder.parseWorkflowInstance(hitDoc.get("xml")));
      }
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (isearcher != null) {
        try {
          isearcher.close();
        } catch (IOException e) {
          logger.warn("unable to close index searcher", e);
        }
      }
    }
    return set;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#remove(java.lang.String)
   */
  @Override
  public void remove(String id) throws WorkflowDatabaseException, NotFoundException {
    IndexWriter indexWriter = null;
    try {
      indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
      indexWriter.deleteDocuments(new Term("id", id));
      indexWriter.expungeDeletes();
      indexWriter.commit();
      String fileName = getFilename(id);
      workspace.deleteFromCollection(COLLECTION_ID, fileName);
    } catch (IOException e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (indexWriter != null) {
        try {
          indexWriter.close();
        } catch (IOException e) {
          throw new WorkflowDatabaseException(e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(WorkflowInstance instance) throws WorkflowDatabaseException {
    String xml;
    try {
      xml = WorkflowBuilder.getInstance().toXml(instance);
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
    try {
      workspace.putInCollection(COLLECTION_ID, getFilename(instance.getId()), IOUtils.toInputStream(xml, "UTF-8"));
      index(instance);
    } catch (IOException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Generates a filename based on the workflow ID
   * 
   * @param workflowId
   * @return
   */
  private String getFilename(String workflowId) {
    return workflowId + ".xml";
  }
}
