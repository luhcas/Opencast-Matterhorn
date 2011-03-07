package org.opencastproject.solr;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.solr.analysis.BaseTokenizerFactory;

import java.io.Reader;

/**
 * A solr tokenizer factory that doesn't actually tokenize. We use this to enable use of
 * {@link org.apache.solr.analysis.LowerCaseFilterFactory}.
 */
public class NonTokenizingFactory extends BaseTokenizerFactory {
  @Override
  public Tokenizer create(Reader input) {
    return new CharTokenizer(input) {
      @Override
      protected boolean isTokenChar(char c) {
        return true;
      }
    };
  }
}
