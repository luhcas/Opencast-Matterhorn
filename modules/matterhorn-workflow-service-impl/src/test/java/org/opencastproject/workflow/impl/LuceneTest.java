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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

public class LuceneTest {
  static String ID1 = "123-456";
  static String ID2 = "123-456-abc";
  static String ID3 = "123-456-abc-def";
  static String ID4 = "123-456-abc-def-ghi";
  
  @Test
  public void testLucene() throws Exception {
    Directory directory = FSDirectory.getDirectory("target/" + System.currentTimeMillis());
    
    Analyzer analyzer = new StandardAnalyzer();
    IndexWriter indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
    indexWriter.addDocument(getDocument(ID1, "Foo"));
    indexWriter.close();

    indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
    indexWriter.addDocument(getDocument(ID2, "Foo Bar"));
    indexWriter.close();

    indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
    indexWriter.addDocument(getDocument(ID3, "Bar Baz"));
    indexWriter.close();

    indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
    indexWriter.updateDocument(new Term("id", ID4), getDocument(ID4, "Bar Baz Qux"));
    indexWriter.close();

    // Search for an ID
    IndexSearcher iSearcher = new IndexSearcher(directory);
    BooleanQuery q = new BooleanQuery();
    q.add(new TermQuery(new Term("id", ID1)), Occur.MUST);
    TopDocs topDocs = iSearcher.search(q, 100000);
    System.out.println("ID1: " + topDocs.scoreDocs.length);
    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
      Document hitDoc = iSearcher.doc(topDocs.scoreDocs[i].doc);
      System.out.println("\tid=" + hitDoc.get("id"));
      System.out.println("\ttext=" + hitDoc.get("text"));
      System.out.println("\txml=" + hitDoc.get("xml"));
    }
    iSearcher.close();

    // Search for "foo"
    iSearcher = new IndexSearcher(directory);
    q = new BooleanQuery();
    q.add(new TermQuery(new Term("text", "foo")), Occur.MUST);
    topDocs = iSearcher.search(q, 100000);
    System.out.println("Foo: " + topDocs.scoreDocs.length);
    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
      Document hitDoc = iSearcher.doc(topDocs.scoreDocs[i].doc);
      System.out.println("\tid=" + hitDoc.get("id"));
      System.out.println("\ttext=" + hitDoc.get("text"));
      System.out.println("\txml=" + hitDoc.get("xml"));
    }
    iSearcher.close();

    // Search for "bar"
    iSearcher = new IndexSearcher(directory);
    q = new BooleanQuery();
    q.add(new TermQuery(new Term("text", "bar")), Occur.MUST);
    topDocs = iSearcher.search(q, 100000);
    System.out.println("Bar: " + topDocs.scoreDocs.length);
    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
      Document hitDoc = iSearcher.doc(topDocs.scoreDocs[i].doc);
      System.out.println("\tid=" + hitDoc.get("id"));
      System.out.println("\ttext=" + hitDoc.get("text"));
      System.out.println("\txml=" + hitDoc.get("xml"));
    }
    iSearcher.close();

    // Search for "foo" in the unanalyzed xml field
    iSearcher = new IndexSearcher(directory);
    q = new BooleanQuery();
    q.add(new TermQuery(new Term("xml", "foo")), Occur.MUST);
    topDocs = iSearcher.search(q, 100000);
    System.out.println("XML Foo: " + topDocs.scoreDocs.length);
    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
      Document hitDoc = iSearcher.doc(topDocs.scoreDocs[i].doc);
      System.out.println("\tid=" + hitDoc.get("id"));
      System.out.println("\ttext=" + hitDoc.get("text"));
      System.out.println("\txml=" + hitDoc.get("xml"));
    }
    iSearcher.close();
  }

  protected Document getDocument(String id, String value) {
    Document doc = new Document();
    String xml = "<something><id>" + id + "</id><value>" + value + "</value></something>";
    doc.add(new Field("id", id, Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("text", xml, Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("xml", xml, Field.Store.YES, Field.Index.NOT_ANALYZED));
    return doc;
  }
}
