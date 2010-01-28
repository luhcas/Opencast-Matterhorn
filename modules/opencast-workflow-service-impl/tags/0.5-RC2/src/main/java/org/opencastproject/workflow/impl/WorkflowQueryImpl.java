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
package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowInstance.State;

/**
 * A fluent API for issuing {@link WorkflowInstance} queries.
 */
public class WorkflowQueryImpl implements WorkflowQuery {
  protected long count;
  protected long startPage;
  protected String text;
  protected State state;
  protected String episodeId;
  protected String seriesId;
  protected String mediaPackageId;
  protected String currentOperation;
  protected ElementTuple elementTuple;

  public WorkflowQueryImpl() {}
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withCount(long)
   */
  public WorkflowQuery withCount(long count) {
    this.count = count;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withStartPage(long)
   */
  public WorkflowQuery withStartPage(long startPage) {
    this.startPage = startPage;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withText(java.lang.String)
   */
  public WorkflowQuery withText(String text) {
    this.text = text;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withState(org.opencastproject.workflow.api.WorkflowInstance.State)
   */
  public WorkflowQuery withState(State state) {
    this.state = state;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withEpisode(java.lang.String)
   */
  public WorkflowQuery withEpisode(String episodeId) {
    this.episodeId = episodeId;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withSeries(java.lang.String)
   */
  public WorkflowQuery withSeries(String seriesId) {
    this.seriesId = seriesId;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withMediaPackage(java.lang.String)
   */
  public WorkflowQuery withMediaPackage(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withCurrentOperation(java.lang.String)
   */
  public WorkflowQuery withCurrentOperation(String currentOperation) {
    this.currentOperation = currentOperation;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withElement(java.lang.String, java.lang.String, boolean)
   */
  public WorkflowQuery withElement(String elementType, String elementFlavor, boolean exists) {
    this.elementTuple = new ElementTuple(elementType, elementFlavor, exists);
    return this;
  }

  public long getCount() {
    return count;
  }

  public long getStartPage() {
    return startPage;
  }

  public String getText() {
    return text;
  }

  public State getState() {
    return state;
  }

  public String getEpisodeId() {
    return episodeId;
  }

  public String getSeriesId() {
    return seriesId;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getCurrentOperation() {
    return currentOperation;
  }

  public ElementTuple getElementTuple() {
    return elementTuple;
  }
  static class ElementTuple {
    String elementType, elementFlavor;
    boolean exists;
    ElementTuple(String elementType, String elementFlavor, boolean exists) {
      this.elementType = elementType;
      this.elementFlavor = elementFlavor;
      this.exists = exists;
    }
  }
}
