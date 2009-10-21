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

package org.opencastproject.workflow.impl.solr;

import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.impl.WorkflowSetImpl;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class implementing <code>LookupRequester</code> to provide connection to solr indexing facility.
 */
public class SolrRequester {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(SolrRequester.class);

  /** The regular filter expression */
  private static final String queryCleanerRegex = "[^0-9a-zA-ZöäüßÖÄÜ/\" +-.,]";

  /** The connection to the solr database */
  private SolrConnection solrConnection = null;

  /**
   * Creates a new requester for solr that will be using the given connection object to query the search index.
   * 
   * @param connection
   *          the solr connection
   */
  public SolrRequester(SolrConnection connection) {
    if (connection == null)
      throw new IllegalStateException("Unable to run queries on null connection");
    this.solrConnection = connection;
  }

  public WorkflowSet getWorkflowsByMediaPackageId(String mediaPackageId) throws SolrServerException {
    String q = SolrFields.OC_MEDIA_PACKAGE_ID + ":" + mediaPackageId;
    SolrQuery query = new SolrQuery(q);
    query.setFields("* score");
    return createResultset(query);
  }

  public WorkflowSet getWorkflowsByEpisodeId(String episodeId) throws SolrServerException {
    String q = SolrFields.DC_IDENTIFIER + ":" + cleanQuery(episodeId);
    SolrQuery query = new SolrQuery(q);
    query.addSortField(SolrFields.DC_CREATED, ORDER.desc);
    query.setFields("* score");
    return createResultset(query);
  }

  public WorkflowSet getWorkflowsBySeries(String seriesId) throws SolrServerException {
    String q = SolrFields.DC_IS_PART_OF + ":" + cleanQuery(seriesId);
    SolrQuery query = new SolrQuery(q);
    query.addSortField(SolrFields.DC_CREATED, ORDER.desc);
    query.setFields("* score");
    return createResultset(query);
  }

  /**
   * Just for testing. Returns all solr entries as regular search result.
   * 
   * @param offset
   *          The offset.
   * @param limit
   *          The limit.
   * @return The regular result.
   * @throws SolrServerException
   */
  public WorkflowSet getEverything(int limit, int offset) throws SolrServerException {
    String q = "*:*";
    SolrQuery query = new SolrQuery(q);
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createResultset(query);
  }

  public WorkflowSet getWorkflowById(String workflowId) throws SolrServerException {
    String q = SolrFields.WORKFLOW_ID + ":" + workflowId;
    SolrQuery query = new SolrQuery(q);
    query.setFields("* score");
    return createResultset(query);
  }

  public WorkflowSet getWorkflowsByDate(int offset, int limit) throws SolrServerException {
    String q = SolrFields.OC_MODIFIED + ":*";
    SolrQuery query = new SolrQuery(q);
    query.addSortField(SolrFields.OC_MODIFIED, ORDER.desc);
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createResultset(query);
  }

  public WorkflowSet getWorkflowsByText(String text, int offset, int limit) throws SolrServerException {
    StringBuffer sb = boost(cleanQuery(text));
    SolrQuery query = new SolrQuery(sb.toString());
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createResultset(query);
  }

  public WorkflowSet getWorkflowsInState(String state, int offset, int limit) throws SolrServerException {
    String q = SolrFields.OC_STATE + ":" + state.toLowerCase();
    SolrQuery query = new SolrQuery(q);
    query.addSortField(SolrFields.OC_MODIFIED, ORDER.desc);
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createResultset(query);
  }

  /**
   * Creates a result set from a given solr response.
   * 
   * @param solrResponse
   *          The solr response.
   * @return The search result.
   * @throws SolrServerException
   *           if the solr server is not working as expected
   */
  private WorkflowSet createResultset(SolrQuery query) throws SolrServerException {

    // Execute the query and try to get hold of a query response
    QueryResponse solrResponse = null;
    try {
      solrResponse = solrConnection.request(query.toString());
    } catch (Exception e1) {
      throw new SolrServerException(e1);
    }

    // Create and configure the query result
    WorkflowSetImpl result = new WorkflowSetImpl(query.getQuery());
    result.setSearchTime(solrResponse.getQTime());
    result.setOffset(solrResponse.getResults().getStart());
    result.setLimit(solrResponse.getResults().getNumFound());

    // Walk through response and create new items with title, creator, etc:
    for (SolrDocument doc : solrResponse.getResults()) {
      String workflowInstanceAsString = doc.getFieldValue(SolrFields.OC_WORKFLOW_INSTANCE).toString();
      WorkflowInstance workflowInstance;
      try {
        workflowInstance = WorkflowBuilder.getInstance().parseWorkflowInstance(workflowInstanceAsString);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      result.addItem(workflowInstance);
    }

    return result;
  }

  /**
   * Modifies the query such that certain fields are being boosted (meaning they gain some weight).
   * 
   * @param query
   *          The user query.
   * @return The boosted query
   */
  public StringBuffer boost(String query) {
    String uq = cleanQuery(query);
    StringBuffer sb = new StringBuffer();

    sb.append("(");
    sb.append(SolrFields.FULLTEXT);
    sb.append(":(");
    sb.append(uq);
    sb.append(") ");

    sb.append(")");

    return sb;
  }

  /**
   * Clean up the user query input string to avoid invalid input parameters.
   * 
   * @param q
   *          The input String.
   * @return The cleaned string.
   */
  protected String cleanQuery(String q) {
    return q.replaceAll(queryCleanerRegex, " ").trim();
  }

}
