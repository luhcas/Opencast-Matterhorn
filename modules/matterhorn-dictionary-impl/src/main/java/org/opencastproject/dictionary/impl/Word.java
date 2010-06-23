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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A word that exists in a particular language.
 */
@Entity
@Table(name="DICTIONARY")
public class Word {
  
  /** The text of the word itself */
  @Id
  @Column
  protected String text;

  /** The language in which this word appears */
  @Id
  @Column
  protected String language;

  /** Whether this is a stop word (a word that appears so frequently in a language that it is not useful in searches) */
  @Column
  protected boolean stopWord;

  /** The number of occurrences of this word divided by the total number of words loaded for this language */
  @Column
  protected double weight;
  
  /** The number of times this word was found while loading the text corpus for a language */
  @Column
  protected long count;

  /** The default, no-arg constructor used by JPA to construct a new object before populating its fields */
  public Word() {}
  
  /** Creates a Word from text and a language */
  public Word(String text, String language) {
    this();
    this.text = text;
    this.language = language;
  }

  /** Creates a Word from text, a language, and a count */
  public Word(String text, String language, long count) {
    this(text, language);
    this.count = count;
  }

  /** Creates a Word from text, a language, count, and weight */
  public Word(String text, String language,  long count, double weight) {
    this(text, language, count);
    this.weight = weight;
  }

  /** Creates a Word from text, a language, count, weight, and whether this is a stop word */
  public Word(String text, String language,  long count, double weight, boolean stopWord) {
    this(text, language, count, weight);
    this.stopWord = stopWord;
  }
  
  /**
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * @param text the text to set
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @param language the language to set
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * @return the stopWord
   */
  public boolean isStopWord() {
    return stopWord;
  }

  /**
   * @param stopWord the stopWord to set
   */
  public void setStopWord(boolean stopWord) {
    this.stopWord = stopWord;
  }

  /**
   * @return the weight
   */
  public double getWeight() {
    return weight;
  }

  /**
   * @param weight the weight to set
   */
  public void setWeight(double weight) {
    this.weight = weight;
  }

  /**
   * @return the count
   */
  public long getCount() {
    return count;
  }

  /**
   * @param count the count to set
   */
  public void setCount(long count) {
    this.count = count;
  }
  
  
}
