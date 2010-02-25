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
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

/**
 * A fluent API for issuing {@link WorkflowInstance} queries.
 */
public class WorkflowQueryImpl implements WorkflowQuery {
  protected long count;
  protected long startPage;
  protected String text;
  protected WorkflowState state;
  protected String episodeId;
  protected String seriesId;
  protected String mediaPackageId;
  protected String currentOperation;

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
   * @see org.opencastproject.workflow.api.WorkflowQuery#withState(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState)
   */
  public WorkflowQuery withState(WorkflowState state) {
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
   * @see org.opencastproject.workflow.api.WorkflowQuery#getCount()
   */
  @Override
  public long getCount() {
    return count;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#getStartPage()
   */
  @Override
  public long getStartPage() {
    return startPage;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#getText()
   */
  @Override
  public String getText() {
    return text;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#getState()
   */
  @Override
  public WorkflowState getState() {
    return state;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#getCurrentOperation()
   */
  @Override
  public String getCurrentOperation() {
    return currentOperation;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#getEpisode()
   */
  @Override
  public String getEpisode() {
    return episodeId;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#getMediaPackage()
   */
  @Override
  public String getMediaPackage() {
    return mediaPackageId;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#getSeries()
   */
  @Override
  public String getSeries() {
    return seriesId;
  }
}
