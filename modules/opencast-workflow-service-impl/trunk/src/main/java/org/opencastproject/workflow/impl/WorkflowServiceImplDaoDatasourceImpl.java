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

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.impl.WorkflowQueryImpl.ElementTuple;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

/**
 * Implements the workflow service's DAO using a relational database
 */
public class WorkflowServiceImplDaoDatasourceImpl implements WorkflowServiceImplDao {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImplDaoDatasourceImpl.class);

  /**
   * The data source to use in this DAO
   */
  protected DataSource dataSource;

  public WorkflowServiceImplDaoDatasourceImpl() {}

  public WorkflowServiceImplDaoDatasourceImpl(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#activate()
   */
  public void activate(ComponentContext cc) {
    Connection conn = borrowConnection();
    Statement s = null;
    try {
      s = conn.createStatement();
      s.execute("create table if not exists oc_workflow_element(workflow_id varchar(127), mp_id varchar(127), "
              + "mp_element_id varchar(127), mp_element_type varchar(127), mp_element_flavor varchar(127), "
              + "PRIMARY KEY(workflow_id, mp_element_id))");
      s.execute("create table if not exists oc_workflow(workflow_id varchar(127) PRIMARY KEY, mp_id varchar(127), "
              + "workflow_state varchar(127), episode_id varchar(127), series_id varchar(127), workflow_text clob, "
              + "workflow_xml clob, date_created timestamp)");
      s.execute("create index oc_workflow_mp_id on oc_workflow (mp_id)");
      s.execute("create index oc_workflow_workflow_state on oc_workflow (workflow_state)");
      s.execute("create index oc_workflow_episode_id on oc_workflow (episode_id)");
      s.execute("create index oc_workflow_series_id on oc_workflow (series_id)");
    } catch (SQLException e) {
      logger.error(e.getMessage());
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      returnConnection(conn);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#deactivate()
   */
  public void deactivate() {
    // logger.info("Shutting down derby DB.");
    // try {
    // // This throws by-design, so ignore the exception
    // DriverManager.getConnection("jdbc:derby:;shutdown=true");
    // } catch (SQLException e) {}
  }

  /**
   * Obtain a connection to the database. It is the caller's responsibility to close all associated resources
   * (Statements, ResultSets, etc.) and return the connection using returnConnection(Connection conn).
   * 
   * @return
   */
  protected Connection borrowConnection() {
    try {
      return dataSource.getConnection();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  protected void returnConnection(Connection conn) {
    try {
      if (conn != null)
        conn.close();
    } catch (SQLException e) {
      logger.error("Unable to close database connection:" + e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowById(java.lang.String)
   */
  public WorkflowInstance getWorkflowById(String workflowId) {
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet r = null;
    try {
      conn = borrowConnection();
      s = conn.prepareStatement("select workflow_xml from oc_workflow where workflow_id=?");
      s.setString(1, workflowId);
      r = s.executeQuery();
      if (r.next()) {
        String xml = r.getString(1);
        return WorkflowBuilder.getInstance().parseWorkflowInstance(xml);
      } else {
        return null;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (r != null) {
        try {
          r.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      if (s != null)
        try {
          s.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      returnConnection(conn);
    }
  }

  /**
   * Gets a set of workflows based on the provided query.  The first column in the query must be the workflow xml.
   * 
   * @param query
   *          The sql query as a prepared statement
   * @param params
   *          The parameters to pass along with the query
   * @param offset
   *          The paging offset
   * @param limit
   *          The paging limit
   * @return The set of workflows matching this query
   */
  protected WorkflowSet getWorkflowSet(String query, String[] params, long offset, long limit) {
    query = query + " order by date_created desc";
    if (offset > 0 && limit > 0) {
      query = query + " limit " + limit + " offset " + (offset * limit);
    } else if (limit > 0) {
      query = query + " limit " + limit;
    }
    
    logger.debug("Query: {}", query);
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet r = null;
    try {
      conn = borrowConnection();
      s = conn.prepareStatement(query);
      if (params != null) {
        for (int i = 0; i < params.length; i++) {
          s.setString(i + 1, params[i]);
        }
      }
      long start = System.currentTimeMillis();
      r = s.executeQuery();
      long searchTime = System.currentTimeMillis() - start;
      int count = 0;
      WorkflowSetImpl set = new WorkflowSetImpl(query);
      set.setLimit(limit);
      set.setOffset(offset);
      set.setSearchTime(searchTime);
      while (r.next()) {
        count++;
        String xml = r.getString(1);
        set.addItem(WorkflowBuilder.getInstance().parseWorkflowInstance(xml));
      }
      return set;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      returnConnection(conn);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstanceQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) {
    List<String> params = new ArrayList<String>();
    List<String> whereClauseList = new ArrayList<String>();
    WorkflowQueryImpl queryImpl = (WorkflowQueryImpl)query;

    // If we need to count media package elements, use a join
    StringBuilder q;
    ElementTuple elementTuple = queryImpl.getElementTuple();
    if(elementTuple != null) {
      if(elementTuple.exists) {
        q = new StringBuilder("select wf.workflow_xml as wfi from oc_workflow as wf");
        q.append(" join oc_workflow_element as el on wf.workflow_id = el.workflow_id where el.mp_element_type=?");
        q.append(" and el.mp_element_flavor=?");
      } else {
        q = new StringBuilder("select distinct wf.workflow_xml as wfi, wf.date_created from oc_workflow as wf");
        q.append(" join oc_workflow_element as el on wf.workflow_id = el.workflow_id where");
        q.append(" (el.mp_element_type <> ? or el.mp_element_flavor <> ?)");
      }
      params.add(elementTuple.elementType);
      params.add(elementTuple.elementFlavor);
    } else {
      // always include a where clause to make the append logic simpler
      q = new StringBuilder("select wf.workflow_xml from oc_workflow as wf where 1=1");
    }

    // Add the rest of the where clauses
    if(queryImpl.getEpisodeId() != null) {
      whereClauseList.add(" and wf.episode_id=?");
      params.add(queryImpl.getEpisodeId());
    }
    if(queryImpl.getMediaPackageId() != null) {
      whereClauseList.add(" and wf.mp_id=?");
      params.add(queryImpl.getMediaPackageId());
    }
    if(queryImpl.getSeriesId() != null) {
      whereClauseList.add(" and wf.series_id=?");
      params.add(queryImpl.getSeriesId());
    }
    if(queryImpl.getState() != null) {
      whereClauseList.add(" and wf.workflow_state=?");
      params.add(queryImpl.getState().name().toLowerCase());
    }
    if(queryImpl.getText() != null) {
      whereClauseList.add(" and wf.workflow_text like ?");
      params.add("%" + queryImpl.getText().toLowerCase() + "%");
    }
    
    // build the rest of the where clause
    for(Iterator<String> iter = whereClauseList.iterator(); iter.hasNext();) {
      q.append(iter.next());
    }

    // if using a join, add the "group by" and "having" clauses
    if(elementTuple != null) {
      q.append(" group by wfi having count(el.mp_element_id) > 0");
    }

    return getWorkflowSet(q.toString(), params.toArray(new String[params.size()]), queryImpl.getOffset(), queryImpl.getLimit());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#remove(java.lang.String)
   */
  public void remove(String id) {
    Connection conn = null;
    PreparedStatement s1 = null;
    PreparedStatement s2 = null;
    try {
      conn = borrowConnection();
      conn.setAutoCommit(false);
      s1 = conn.prepareStatement("delete from oc_workflow where workflow_id=?");
      s2 = conn.prepareStatement("delete from oc_workflow_element where workflow_id=?");
      s1.setString(1, id);
      s2.setString(1, id);
      s1.executeUpdate();
      s2.executeUpdate();
      conn.commit();
    } catch (SQLException e) {
      try {
        conn.rollback();
      } catch (SQLException e1) {
        throw new RuntimeException(e);
      }
      throw new RuntimeException(e);
    } finally {
      if (s1 != null)
        try {
          s1.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
        if (s2 != null)
          try {
            s2.close();
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
      returnConnection(conn);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance instance) {
    Connection conn = null;
    PreparedStatement updateStatment = null;
    PreparedStatement deleteElementsStatement = null;
    PreparedStatement addElementsStatement = null;
    try {
      conn = borrowConnection();
      conn.setAutoCommit(false);
      String xml = WorkflowBuilder.getInstance().toXml(instance);
      
      // Get the field values to store
      MediaPackage mp = instance.getCurrentMediaPackage();
      String mediaPackageId = null;
      String episodeId = null;
      String seriesId = null;
      String text = null;
      if (mp != null) {
        mediaPackageId = mp.getIdentifier().toString();
        episodeId = findEpisodeId(instance.getSourceMediaPackage());
        seriesId = findSeriesId(instance.getSourceMediaPackage());
        text = getDublinCoreText(instance.getSourceMediaPackage());
      }
      
      if (exists(instance.getId(), conn)) {
        // Update the workflow (TODO: Update the rest of the fields? Will the dublin core fields change in the middle of
        // a workflow?)
        updateStatment = conn.prepareStatement("update oc_workflow set workflow_xml=?, workflow_state=? where workflow_id=?");
        updateStatment.setString(1, xml);
        updateStatment.setString(2, instance.getState().name().toLowerCase());
        updateStatment.setString(3, instance.getId());
        updateStatment.execute();
      } else {
        // Add it
        updateStatment = conn.prepareStatement("insert into oc_workflow values(?, ?, ?, ?, ?, ?, ?, ?)");
        updateStatment.setString(1, instance.getId());
        updateStatment.setString(2, mediaPackageId);
        updateStatment.setString(3, instance.getState().name().toLowerCase());
        updateStatment.setString(4, episodeId);
        updateStatment.setString(5, seriesId);
        updateStatment.setString(6, text);
        updateStatment.setString(7, xml);
        updateStatment.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
        updateStatment.execute();
      }
      // Update the elements table
      deleteElementsStatement = conn.prepareStatement("delete from oc_workflow_element where workflow_id=?");
      deleteElementsStatement.setString(1, instance.getId());
      deleteElementsStatement.execute();
      addElementsStatement = conn.prepareStatement("insert into oc_workflow_element values(?, ?, ?, ?, ?)");
      if(mp != null) {
        for(Attachment att : mp.getAttachments()) {
          addElementsStatement.setString(1, instance.getId());
          addElementsStatement.setString(2, mediaPackageId);
          addElementsStatement.setString(3, att.getIdentifier());
          addElementsStatement.setString(4, "attachment");
          addElementsStatement.setString(5, att.getFlavor().toString());
          addElementsStatement.execute();
        }
        for(Catalog cat : mp.getCatalogs()) {
          addElementsStatement.setString(1, instance.getId());
          addElementsStatement.setString(2, mediaPackageId);
          addElementsStatement.setString(3, cat.getIdentifier());
          addElementsStatement.setString(4, "catalog");
          addElementsStatement.setString(5, cat.getFlavor().toString());
          addElementsStatement.execute();
        }
        for(Track track : mp.getTracks()) {
          addElementsStatement.setString(1, instance.getId());
          addElementsStatement.setString(2, mediaPackageId);
          addElementsStatement.setString(3, track.getIdentifier());
          addElementsStatement.setString(4, "track");
          addElementsStatement.setString(5, track.getFlavor().toString());
          addElementsStatement.execute();
        }
      }
      // Commit the transaction
      conn.commit();
    } catch (Exception e) {
      try {
        conn.rollback();
      } catch (SQLException e1) {
        throw new RuntimeException(e1);
      }
      throw new RuntimeException(e);
    } finally {
      if (updateStatment != null)
        try {
          updateStatment.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      returnConnection(conn);
    }
  }

  /**
   * Gets a string representation of the dublin core metadata, which is useful for a quasi- full text search
   * 
   * @param mediaPackage
   *          The mediapackage to use in generating the text representation
   * @return The text representation
   */
  private String getDublinCoreText(MediaPackage mediaPackage) {
    Catalog[] dcCatalogs = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR,
            MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if (dcCatalogs.length == 0)
      return null;
    StringBuilder sb = new StringBuilder();
    DublinCoreCatalog dc = (DublinCoreCatalog) dcCatalogs[0];
    List<EName> props = new ArrayList<EName>(dc.getProperties());
    for (int i = 0; i < props.size(); i++) {
      String value = dc.getFirst(props.get(i));
      if (value == null)
        continue;
      sb.append(value);
      if (i < props.size() - 1)
        sb.append("|");
    }
    return sb.toString();
  }

  /**
   * Finds the series ID for a media package.
   * 
   * @param sourceMediaPackage
   *          The media package to inspect
   * @return The series ID, as identified in the dublin core catalog, if it exists
   */
  private String findSeriesId(MediaPackage sourceMediaPackage) {
    Catalog[] dcCatalogs = sourceMediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR,
            MediaPackageReferenceImpl.ANY_SERIES);
    if (dcCatalogs.length == 0)
      return null;
    String seriesId = ((DublinCoreCatalog) dcCatalogs[0]).getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER,
            DublinCoreCatalog.LANGUAGE_UNDEFINED);
    logger.debug("Found series ID=" + seriesId + " in media package " + sourceMediaPackage.getIdentifier());
    return seriesId;
  }

  /**
   * Finds the episode ID for a media package.
   * 
   * @param sourceMediaPackage
   *          The media package to inspect
   * @return The episode ID, as identified in the dublin core catalog, if it exists
   */
  private String findEpisodeId(MediaPackage sourceMediaPackage) {
    Catalog[] dcCatalogs = sourceMediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR,
            MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if (dcCatalogs.length == 0)
      return null;
    String episodeId = ((DublinCoreCatalog) dcCatalogs[0]).getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER,
            DublinCoreCatalog.LANGUAGE_UNDEFINED);
    logger.debug("Found episode ID=" + episodeId + " in media package " + sourceMediaPackage.getIdentifier());
    return episodeId;
  }

  /**
   * Whether a workflow instance exists in the database or not
   * 
   * @param id
   *          The ID of the workflow instance
   * @param conn
   *          The DB connection to use (rather than borrowing one ourselves)
   * @return
   */
  protected boolean exists(String id, Connection conn) throws SQLException {
    PreparedStatement s = null;
    ResultSet r = null;
    try {
      s = conn.prepareStatement("select count(*) from oc_workflow where workflow_id=?");
      s.setString(1, id);
      r = s.executeQuery();
      r.next(); // There will always be one record in the resultset
      return r.getInt(1) == 1;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } finally {
      if (s != null) {
        s.close();
      }
      if (r != null) {
        r.close();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#countWorkflowInstances()
   */
  public long countWorkflowInstances() {
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet r = null;
    try {
      conn = borrowConnection();
      s = conn.prepareStatement("select count(*) from oc_workflow");
      r = s.executeQuery();
      if (r.next()) {
        return r.getInt(1);
      } else {
        throw new IllegalStateException("This result set should never be empty");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (r != null) {
        try {
          r.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      if (s != null)
        try {
          s.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      returnConnection(conn);
    }
  }

  /** Sets the data source */
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }
}
