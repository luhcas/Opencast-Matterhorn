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

import java.util.ArrayList;
import java.util.List;

/**
 * A fluent API for issuing WorkflowInstance queries. This object is thread unsafe.
 */
public class WorkflowQuery {
  protected long count;
  protected long startPage;

  protected String text;
  protected String seriesTitle;
  protected String seriesId;
  protected String mediaPackageId;
  protected String workflowDefinitionId;
  protected String creator;
  protected String contributor;
  protected String language;
  protected String license;
  protected String title;
  protected String subject;

  /**
   * The list of current operation terms that have been added to this query.
   */
  protected List<QueryTerm> currentOperationTerms = new ArrayList<QueryTerm>();

  /**
   * The list of state terms that have been added to this query.
   */
  protected List<QueryTerm> stateTerms = new ArrayList<QueryTerm>();

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

  /**
   * Limit results to workflow instances in a specific state. This method overrides and will be overridden by future
   * calls to {@link #withoutState(String)}
   * 
   * @param state
   *          the workflow state
   * @return this query
   */
  public WorkflowQuery withState(WorkflowState state) {
    stateTerms.add(new QueryTerm(state.toString(), true));
    return this;
  }

  /**
   * Limit results to workflow instances not in a specific state. This method overrides and will be overridden by future
   * calls to {@link #withState(String)}
   * 
   * @param state
   *          the workflow state
   * @return this query
   */
  public WorkflowQuery withoutState(WorkflowState state) {
    stateTerms.add(new QueryTerm(state.toString(), false));
    return this;
  }

  /** Limit results to workflow instances with a specific series title */
  public WorkflowQuery withSeriesTitle(String seriesTitle) {
    this.seriesTitle = seriesTitle;
    return this;
  }

  /** Limit results to workflow instances for a specific series */
  public WorkflowQuery withSeriesId(String seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  /** Limit results to workflow instances for a specific media package */
  public WorkflowQuery withMediaPackage(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    return this;
  }

  /**
   * Limit results to workflow instances that are currently handling the specified operation. This method overrides and
   * will be overridden by future calls to {@link #withoutCurrentOperation(String)}
   * 
   * @param currentOperation
   *          the current operation
   * @return this query
   */
  public WorkflowQuery withCurrentOperation(String currentOperation) {
    currentOperationTerms.add(new QueryTerm(currentOperation, true));
    return this;
  }

  /**
   * Limit results to workflow instances to those that are not currently in the specified operation. This method
   * overrides and will be overridden by future calls to {@link #withCurrentOperation(String)}
   * 
   * @param currentOperation
   *          the current operation
   * @return this query
   */
  public WorkflowQuery withoutCurrentOperation(String currentOperation) {
    currentOperationTerms.add(new QueryTerm(currentOperation, false));
    return this;
  }

  /**
   * Limit results to workflow instances with a specific workflow definition.
   * 
   * @param workflowDefinitionId
   *          the workflow identifier
   */
  public WorkflowQuery withWorkflowDefintion(String workflowDefinitionId) {
    this.workflowDefinitionId = workflowDefinitionId;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage creator.
   * 
   * @param creator
   *          the mediapackage creator
   */
  public WorkflowQuery withCreator(String creator) {
    this.creator = creator;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage contributor.
   * 
   * @param contributor
   *          the mediapackage contributor
   */
  public WorkflowQuery withContributor(String contributor) {
    this.contributor = contributor;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage language.
   * 
   * @param language
   *          the mediapackage language
   */
  public WorkflowQuery withLanguage(String language) {
    this.language = language;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage license.
   * 
   * @param license
   *          the mediapackage license
   */
  public WorkflowQuery withLicense(String license) {
    this.license = license;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage title.
   * 
   * @param title
   *          the mediapackage title
   */
  public WorkflowQuery withTitle(String title) {
    this.title = title;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage subject.
   * 
   * @param subject
   *          the mediapackage subject
   */
  public WorkflowQuery withSubject(String subject) {
    this.subject = subject;
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

  public List<QueryTerm> getStates() {
    return stateTerms;
  }

  public List<QueryTerm> getCurrentOperations() {
    return currentOperationTerms;
  }

  public String getMediaPackage() {
    return mediaPackageId;
  }

  public String getSeriesId() {
    return seriesId;
  }

  public String getSeriesTitle() {
    return seriesTitle;
  }

  /**
   * @return the mediaPackageId
   */
  public String getMediaPackageId() {
    return mediaPackageId;
  }

  /**
   * @return the workflowDefinitionId
   */
  public String getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  /**
   * @return the creator
   */
  public String getCreator() {
    return creator;
  }

  /**
   * @return the contributor
   */
  public String getContributor() {
    return contributor;
  }

  /**
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @return the license
   */
  public String getLicense() {
    return license;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @return the currentOperationTerms
   */
  public List<QueryTerm> getCurrentOperationTerms() {
    return currentOperationTerms;
  }

  /**
   * @return the stateTerms
   */
  public List<QueryTerm> getStateTerms() {
    return stateTerms;
  }

  /**
   * A tuple of a query value and whether this search term should be included or excluded from the search results.
   */
  public static class QueryTerm {
    private String value = null;
    private boolean include = false;

    /** Constructs a new query term */
    public QueryTerm(String value, boolean include) {
      this.value = value;
      this.include = include;
    }

    /**
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * @return whether this query term is to be excluded
     */
    public boolean isInclude() {
      return include;
    }
  }

}
