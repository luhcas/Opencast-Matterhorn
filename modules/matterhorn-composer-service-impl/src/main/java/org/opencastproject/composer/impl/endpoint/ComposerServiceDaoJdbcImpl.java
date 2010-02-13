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
package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.composer.impl.endpoint.Receipt.STATUS;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * JDBC based persistence for the composer service
 */
public class ComposerServiceDaoJdbcImpl implements ComposerServiceDao {
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceDaoJdbcImpl.class);
  protected DataSource dataSource;

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void activate(ComponentContext cc) {
    logger.info("activate()");
    Connection conn = borrowConnection();
    Statement s = null;
    try {
      s = conn.createStatement();
      s.execute("create table if not exists oc_composer(job_id varchar(127) PRIMARY KEY, status varchar(127), xml clob)");
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
   * Obtain a connection to the database. It is the caller's responsibility to close all associated resources
   * (Statements, ResultSets, etc.) and return the connection using returnConnection(Connection conn).
   * 
   * @return
   */
  protected Connection borrowConnection() {
    try {
      Connection conn = dataSource.getConnection();
      conn.setAutoCommit(true);
      return conn;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  protected void returnConnection(Connection conn) {
    if (conn != null) {
      try {
        conn.setAutoCommit(false);
        conn.close();
      } catch (SQLException e) {
        logger.error("Unable to close database connection:" + e.getMessage());
      }
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.endpoint.ComposerServiceDao#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet r = null;
    try {
      conn = borrowConnection();
      s = conn.prepareStatement("select xml from oc_composer where job_id=?");
      s.setString(1, id);
      r = s.executeQuery();
      if( ! r.next()) {
        return null;
      } else {
        String xml = r.getString(1);
        return ReceiptBuilder.getInstance().parseReceipt(xml);
      }
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
   * @see org.opencastproject.composer.impl.endpoint.ComposerServiceDao#createReceipt(java.lang.String)
   */
  @Override
  public Receipt createReceipt() {
    String id = UUID.randomUUID().toString();
    Receipt receipt = new Receipt(id, STATUS.RUNNING.toString());
    Connection conn = null;
    PreparedStatement s = null;
    try {
      String xml = ReceiptBuilder.getInstance().toXml(receipt);
      conn = borrowConnection();
      s = conn.prepareStatement("insert into oc_composer values(?, ?, ?)");
      s.setString(1, id);
      s.setString(2, receipt.getStatus());
      s.setString(3, xml);
      s.execute();
      return receipt;
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
   * @see org.opencastproject.composer.impl.endpoint.ComposerServiceDao#updateReceipt(org.opencastproject.composer.api.Receipt)
   */
  @Override
  public void updateReceipt(Receipt receipt) {
    Connection conn = null;
    PreparedStatement updateStatment = null;
    try {
      conn = borrowConnection();
      String xml = ReceiptBuilder.getInstance().toXml(receipt);
      updateStatment = conn.prepareStatement("update oc_composer set status=?, xml=? where job_id=?");
      updateStatment.setString(1, receipt.getStatus());
      updateStatment.setString(2, xml);
      updateStatment.setString(3, receipt.getId());
      updateStatment.execute();
    } catch(Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (updateStatment != null) {
        try {
          updateStatment.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      returnConnection(conn);
    }
  }
}
