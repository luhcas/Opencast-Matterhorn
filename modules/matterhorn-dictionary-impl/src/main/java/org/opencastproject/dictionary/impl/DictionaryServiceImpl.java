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
package org.opencastproject.dictionary.impl;

import org.opencastproject.dictionary.api.DictionaryService;

import org.apache.commons.io.FileUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * The dictionary service can be used to clean a list of words with respect to a given dictionary.
 */
public class DictionaryServiceImpl implements DictionaryService, ArtifactInstaller {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(DictionaryServiceImpl.class);

  /** The lucene analyzer */
  protected Analyzer analyzer;

  /** The lucene location */
  protected File root;
  
  /** The lucene languages index directory */
  protected Directory langDir;

  /** All the dictionary indexes */
  protected Set<String> allLanguages;

  protected static final int BUFFER_SIZE = 10 * 1024 * 1024;

  
  protected void activate(ComponentContext cc) {
    String storageRoot = (String) cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
    logger.info("{}.activate() with storage root {}", DictionaryService.class.getName(), storageRoot);
    if (storageRoot == null)
      throw new IllegalStateException("storage directory not defined");
    analyzer = new StandardAnalyzer();
    try {
      root = new File(storageRoot, "dictionaries");
      File langRoot = new File(root, "lang");
      langDir = FSDirectory.getDirectory(langRoot);
    } catch (Exception e) {
      logger.warn("unable to initialize lucene", e);
      throw new RuntimeException(e);
    }
    allLanguages = new HashSet<String>();
    // TODO initialize allLanguages from languages index
  }

  private void addLangToIndex(String name, String iso, Integer numUniqueW, Integer numAllW)
          throws IOException {
    IndexWriter langIndex = new IndexWriter(langDir, analyzer, IndexWriter.MaxFieldLength.LIMITED);
    Document doc = new Document();
    doc.add(new Field("name", name, Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("iso", iso, Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("numUniqueW", numUniqueW.toString(), Field.Store.YES, Field.Index.NO));
    doc.add(new Field("numAllW", numAllW.toString(), Field.Store.YES, Field.Index.NO));
    langIndex.addDocument(doc);
    langIndex.close();
  }

  @Override
  public void markStopWord(String word, String language) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addWord(String word, String language) {
    addWord(word, language, 1);
  }

  @Override
  public void addWord(String word, String language, Integer count) {
    //TODO: Check if the word already exists
    try {
      File dict = new File(root, language);
      Directory dictDir = FSDirectory.getDirectory(dict);
      IndexSearcher searcher = new IndexSearcher(dictDir);
      QueryParser parser = new QueryParser("iso", new StandardAnalyzer());
      Query query = parser.parse(language);
      TopDocs docs = searcher.search(query, 1);
      Integer numAllW = Integer.valueOf(searcher.doc(docs.scoreDocs[0].doc).get("numAllW"));
      addWord(word, language, count, 1.0 * count / numAllW);
    } catch (Exception e) {
      logger.error("Failed to read from languages index", e);
    }
  }

  @Override
  public void addWord(String word, String language, Integer count, Double weight) {
    IndexWriter index = null;
    try {
      File dict = new File(root, language);
      Directory dictDir = FSDirectory.getDirectory(dict);
      index = new IndexWriter(dictDir, analyzer, IndexWriter.MaxFieldLength.LIMITED);
    } catch (IOException e) {
      logger.error("Problem opening index {}", language, e);
    }
    addWord(index, word, language, count, weight);
  }

  protected void addWord(IndexWriter index, String word, String language, Integer count, Double weight) {
    Document doc = new Document();
    doc.add(new Field("word", word, Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("count", count.toString(), Field.Store.YES, Field.Index.NO));
    doc.add(new Field("weigth", weight.toString().replace(".", "d").replace("-", "m"), Field.Store.YES, Field.Index.NO));
    doc.add(new Field("isStopWord", "0", Field.Store.YES, Field.Index.NO));
    try {
      index.addDocument(doc);
    } catch (CorruptIndexException e) {
      logger.error("Index {} corupted", language, e);
    } catch (IOException e) {
      logger.error("IO error in index {}", language, e);
    }
  }

  @Override
  public void clear(String language) {
    File dictRoot = new File(root, language);
    try {
      FileUtils.deleteDirectory(dictRoot);
    } catch (IOException e) {
      logger.error("Failed to delete {}", language);
      // TODO fix error handling
    }
    allLanguages.remove(language);
    // TODO remove document from languages index;
  }

  @Override
  public String[] getLanguages(String word) {
    // TODO actually check in which languages the word exists
    return (String[]) allLanguages.toArray(new String[allLanguages.size()]);
  }

  @Override
  public Integer getWordCount(String word, String language) {
    try {
      File dict = new File(root, language);
      Directory dictDir = FSDirectory.getDirectory(dict);
      IndexSearcher searcher = new IndexSearcher(dictDir);
      Query query = new TermQuery(new Term("word", word));
      TopDocs docs = searcher.search(query, 1);
      return Integer.valueOf(searcher.doc(docs.scoreDocs[0].doc).get("count"));
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public double getWordWeight(String word, String language) {
    try {
      File dict = new File(root, language);
      Directory dictDir = FSDirectory.getDirectory(dict);
      IndexSearcher searcher = new IndexSearcher(dictDir);
      
      QueryParser parser = new QueryParser("word", new StandardAnalyzer());
      Query query = parser.parse(word);
      
      //Query query = new TermQuery(new Term("word", word));
      TopDocs docs = searcher.search(query, 1);
      Document doc = searcher.doc(docs.scoreDocs[0].doc);
      // FIXME This does not work (probably problem because of numeric value transcribed with digits, "." and "-" characters)
      return Double.valueOf(doc.get("weight").replace("d", ".").replace("m", "-"));
      
    } catch (Exception e) {
      return 0.0;
    }
  }

  @Override
  public Boolean isStopWord(String word) {
    for(String lang : allLanguages){
      if (isStopWord(word, lang)) return true;
    }
    return false;
  }

  @Override
  public Boolean isStopWord(String word, String language) {
    try {
      File dict = new File(root, language);
      Directory dictDir = FSDirectory.getDirectory(dict);
      IndexSearcher searcher = new IndexSearcher(dictDir);
      Query query = new TermQuery(new Term("word", word));
      TopDocs docs = searcher.search(query, 1);
      return Boolean.valueOf(searcher.doc(docs.scoreDocs[0].doc).get("isStopWord"));
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Boolean isWord(String word) {
    for(String lang : allLanguages){
      if (isWord(word, lang)) return true;
    }
    return false;
  }

  @Override
  public Boolean isWord(String word, String language) {
    try {
      File dict = new File(root, language);
      Directory dictDir = FSDirectory.getDirectory(dict);
      IndexSearcher searcher = new IndexSearcher(dictDir);
      Query query = new TermQuery(new Term("word", word));
      TopDocs docs = searcher.search(query, 1);
      if(docs.totalHits>0) return true;
      else return false;
    } catch (Exception e) {
      return false;
    }
  }

  
  // --------------   ARTIFACT INSTALLER   ---------------------
  @Override
  public boolean canHandle(File artifact) {
    return artifact.getParentFile().getName().equals("dictionaries") && artifact.getName().endsWith(".csv");
  }

  @Override
  public void install(File artifact) throws Exception {
    logger.info("Registering dictionary from {}", artifact);
    Integer numAllW = 1;
    Integer numUniqueW = 1;
    String iso = artifact.getName().split("\\.")[0];

    // check for existance of the dictonary index for this lang
    File dictRoot = new File(root, iso);
    // FIXME check if lucene index already exists
    if (dictRoot.exists()) // this has a tendency to return true
      FileUtils.deleteDirectory(dictRoot);
//      return;  

    // create a new dictionary index
    Directory dictDir = FSDirectory.getDirectory(dictRoot);
    IndexWriter index = new IndexWriter(dictDir, analyzer, IndexWriter.MaxFieldLength.LIMITED);

    // read csv file and fill dictionary index
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(artifact)), BUFFER_SIZE);
    String wordLine;
    while ((wordLine = br.readLine()) != null) {
      if (wordLine.startsWith("#")) {
        if (wordLine.startsWith("#numAllW"))
          numAllW = Integer.valueOf(wordLine.split(":")[1]);
        if (wordLine.startsWith("#numUniqueW"))
          numUniqueW = Integer.valueOf(wordLine.split(":")[1]);
        continue;
      }
      String[] arr = wordLine.split(",");
      String word = arr[0];
      Integer count = Integer.valueOf(arr[1]);
      Double weight = 1.0 * count / numAllW;
      addWord(index, word, iso, count, weight);
    }
    allLanguages.add(iso);
    index.close();

    // add the new language to language index
    addLangToIndex(iso, iso, numUniqueW, numAllW);
    
    // TODO remove this (here for testing)
    Boolean a = isWord("miha", "sl");
    Integer b = getWordCount("miha", "sl");
    Double c = getWordWeight("miha", "sl");
    
  }

  @Override
  public void uninstall(File artifact) throws Exception {
    String iso = artifact.getName().split("\\.")[0];
    logger.info("Removing dictionary {}", iso);
    clear(iso);
  }

  @Override
  public void update(File artifact) throws Exception {
    uninstall(artifact);
    install(artifact);
  }

}
