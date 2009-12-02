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
  protected long limit;
  protected long offset;
  protected String text;
  protected State state;
  protected String episodeId;
  protected String seriesId;
  protected String mediaPackageId;
  protected ElementTuple elementTuple;

  public WorkflowQueryImpl() {}
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withLimit(long)
   */
  public WorkflowQuery withLimit(long limit) {
    this.limit = limit;
    return this;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowQuery#withOffset(long)
   */
  public WorkflowQuery withOffset(long offset) {
    this.offset = offset;
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
   * @see org.opencastproject.workflow.api.WorkflowQuery#withElement(java.lang.String, java.lang.String, boolean)
   */
  @Override
  public WorkflowQuery withElement(String elementType, String elementFlavor, boolean exists) {
    this.elementTuple = new ElementTuple(elementType, elementFlavor, exists);
    return this;
  }

  public long getLimit() {
    return limit;
  }

  public long getOffset() {
    return offset;
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

  public ElementTuple getElementTuple() {
    return elementTuple;
  }
  class ElementTuple {
    String elementType, elementFlavor;
    boolean exists;
    ElementTuple(String elementType, String elementFlavor, boolean exists) {
      this.elementType = elementType;
      this.elementFlavor = elementFlavor;
      this.exists = exists;
    }
  }
}
