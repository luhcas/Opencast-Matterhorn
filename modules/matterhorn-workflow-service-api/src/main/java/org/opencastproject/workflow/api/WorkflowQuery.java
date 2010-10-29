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
package org.opencastproject.workflow.api;

import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

/**
 * A fluent API for issuing WorkflowInstance queries.
 */
public class WorkflowQuery {
  protected long count;
  protected long startPage;
  protected String text;
  protected WorkflowState state;
  protected String seriesId;
  protected String mediaPackageId;
  protected String currentOperation;

  public WorkflowQuery() {
  }

  /** Include a limit for the number of items to return in the result */
  public WorkflowQuery withCount(long count) {
    this.count = count;
    return this;
  }

  /** Include a paging offset for the items returned */
  public WorkflowQuery withStartPage(long startPage) {
    this.startPage = startPage;
    return this;
  }

  /** Limit results to workflow instances matching a free text search */
  public WorkflowQuery withText(String text) {
    this.text = text;
    return this;
  }

  /** Limit results to workflow instances in a specific state */
  public WorkflowQuery withState(WorkflowState state) {
    this.state = state;
    return this;
  }

  /** Limit results to workflow instances for a specific series */
  public WorkflowQuery withSeries(String seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  /** Limit results to workflow instances for a specific media package */
  public WorkflowQuery withMediaPackage(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    return this;
  }

  /** Limit results to workflow instances that are currently handling the specified operation */
  public WorkflowQuery withCurrentOperation(String currentOperation) {
    this.currentOperation = currentOperation;
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

  public WorkflowState getState() {
    return state;
  }

  public String getCurrentOperation() {
    return currentOperation;
  }

  public String getMediaPackage() {
    return mediaPackageId;
  }

  public String getSeries() {
    return seriesId;
  }

}
