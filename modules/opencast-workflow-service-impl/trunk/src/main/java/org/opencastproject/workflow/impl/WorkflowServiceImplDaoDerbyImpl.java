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

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Implements the workflow service's DAO using a relational database
 */
public class WorkflowServiceImplDaoDerbyImpl implements WorkflowServiceImplDao {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImplDaoDerbyImpl.class);

  /** The JDBC URL for connecting to the database */
  protected String jdbcUrl = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#activate()
   */
  public void activate(File storageDirectory) {
    jdbcUrl = "jdbc:derby:" + storageDirectory.getAbsolutePath();

    // TODO Replace with DB connection pool
    String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    try {
      Class.forName(driver).newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Connection conn = null;
    Statement s = null;
    try {
      Properties props = new Properties();
      conn = DriverManager.getConnection(jdbcUrl + ";create=true", props);
      s = conn.createStatement();
      s.execute("create table oc_workflow(workflow_id varchar(40) PRIMARY KEY, mp_id varchar(40), workflow_state varchar(40), "
              + "episode_id varchar(40), series_id varchar(40), workflow_text long varchar, workflow_xml long varchar, "
              + "date_created timestamp)");
      s.execute("create index oc_workflow_mp_id on oc_workflow (mp_id)");
      s.execute("create index oc_workflow_workflow_state on oc_workflow (workflow_state)");
      s.execute("create index oc_workflow_episode_id on oc_workflow (episode_id)");
      s.execute("create index oc_workflow_series_id on oc_workflow (series_id)");
    } catch (SQLException e) {
      logger.error(e.getMessage());
    } finally {
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#deactivate()
   */
  public void deactivate() {
    logger.info("Shutting down derby DB.");
    try {
      // This throws by-design, so ignore the exception
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException e) {}
  }

  /**
   * Obtain a connection to the database. It is the caller's responsibility to close all associated resources
   * (Statements, ResultSets, etc.) and return the connection using returnConnection(Connection conn).
   * 
   * @return
   */
  protected Connection borrowConnection() {
    try {
      Connection conn = DriverManager.getConnection(jdbcUrl, new Properties());
      return conn;
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
   * Gets a set of workflows based on the provided query
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
  protected WorkflowSet getWorkflowSet(String query, String[] params, int offset, int limit) {
    query = query + " order by date_created desc";
     if(offset > 0 && limit > 0) {
       query = query + " offset " + (offset*limit) + " rows fetch first "+ limit + " rows only";
     } else if(limit > 0) {
       query = query + " fetch first " + limit + " rows only";
     }
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
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByDate(int, int)
   */
  public WorkflowSet getWorkflowsByDate(int offset, int limit) throws WorkflowDatabaseException {
    return getWorkflowSet("select workflow_xml from oc_workflow", null, offset, limit);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByEpisode(java.lang.String)
   */
  public WorkflowSet getWorkflowsByEpisode(String episodeId) throws WorkflowDatabaseException {
    return getWorkflowSet("select workflow_xml from oc_workflow where episode_id=?", new String[] {episodeId}, 0, 0);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByMediaPackage(java.lang.String)
   */
  public WorkflowSet getWorkflowsByMediaPackage(String mediaPackageId) {
    return getWorkflowSet("select workflow_xml from oc_workflow where mp_id=?", new String[] { mediaPackageId }, 0, 0);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsBySeries(java.lang.String)
   */
  public WorkflowSet getWorkflowsBySeries(String seriesId) throws WorkflowDatabaseException {
    return getWorkflowSet("select workflow_xml from oc_workflow where series_id=?", new String[] { seriesId }, 0, 0);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByText(java.lang.String, int, int)
   */
  public WorkflowSet getWorkflowsByText(String text, int offset, int limit) throws WorkflowDatabaseException {
    text = text.toLowerCase();
    return getWorkflowSet("select workflow_xml from oc_workflow where workflow_text like ?", new String[] { "%" + text + "%" }, offset, limit);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsInState(org.opencastproject.workflow.api.WorkflowInstance.State,
   *      int, int)
   */
  public WorkflowSet getWorkflowsInState(State state, int offset, int limit) {
    return getWorkflowSet("select workflow_xml from oc_workflow where workflow_state=?", new String[] { state.name()
            .toLowerCase() }, 0, 0);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#remove(java.lang.String)
   */
  public void remove(String id) {
    Connection conn = null;
    PreparedStatement s = null;
    try {
      conn = borrowConnection();
      s = conn.prepareStatement("delete from oc_workflow where workflow_id=?");
      s.setString(1, id);
      s.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } finally {
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance instance) {
    Connection conn = null;
    PreparedStatement s = null;
    try {
      conn = borrowConnection();
      String xml = WorkflowBuilder.getInstance().toXml(instance);
      if (exists(instance.getId(), conn)) {
        // Update the workflow (TODO: Update the rest of the fields?  Will the dublin core fields change in the middle of a workflow?)
        s = conn.prepareStatement("update oc_workflow set workflow_xml=?, workflow_state=? where workflow_id=?");
        s.setString(1, xml);
        s.setString(2, instance.getState().name().toLowerCase());
        s.setString(3, instance.getId());
        s.execute();
      } else {
        // Add it
        MediaPackage mp = instance.getSourceMediaPackage();
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
        s = conn.prepareStatement("insert into oc_workflow values(?, ?, ?, ?, ?, ?, ?, ?)");
        s.setString(1, instance.getId());
        s.setString(2, mediaPackageId);
        s.setString(3, instance.getState().name().toLowerCase());
        s.setString(4, episodeId);
        s.setString(5, seriesId);
        s.setString(6, text);
        s.setString(7, xml);
        s.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
        s.execute();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
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
   * Gets a string representation of the dublin core metadata, which is useful for a quasi- full text search
   * 
   * @param mediaPackage The mediapackage to use in generating the text representation
   * @return The text representation
   */
  private String getDublinCoreText(MediaPackage mediaPackage) {
    Catalog[] dcCatalogs = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if(dcCatalogs.length == 0) return null;
    StringBuilder sb = new StringBuilder();
    DublinCoreCatalog dc = (DublinCoreCatalog)dcCatalogs[0];
    List<EName> props = new ArrayList<EName>(dc.getProperties());
    for(int i=0; i<props.size(); i++) {
      String value = dc.getFirst(props.get(i));
      if(value == null) continue;
      sb.append(value);
      if(i < props.size()-1) sb.append("|");
    }
    return sb.toString();
  }

  /**
   * Finds the series ID for a media package.
   * 
   * @param sourceMediaPackage The media package to inspect
   * @return The series ID, as identified in the dublin core catalog, if it exists
   */
  private String findSeriesId(MediaPackage sourceMediaPackage) {
    Catalog[] dcCatalogs = sourceMediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_SERIES);
    if(dcCatalogs.length == 0) return null;
    String seriesId = ((DublinCoreCatalog)dcCatalogs[0]).getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER,
            DublinCoreCatalog.LANGUAGE_UNDEFINED);
    logger.debug("Found series ID=" + seriesId + " in media package " + sourceMediaPackage.getIdentifier());
    return seriesId;
  }

  /**
   * Finds the episode ID for a media package.
   * 
   * @param sourceMediaPackage The media package to inspect
   * @return The episode ID, as identified in the dublin core catalog, if it exists
   */
  private String findEpisodeId(MediaPackage sourceMediaPackage) {
    Catalog[] dcCatalogs = sourceMediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if(dcCatalogs.length == 0) return null;
    String episodeId = ((DublinCoreCatalog)dcCatalogs[0]).getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER, DublinCoreCatalog.LANGUAGE_UNDEFINED);
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
}
