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
package org.opencastproject.scheduler.impl.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.sql.DataSource;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.opencastproject.scheduler.impl.SchedulerFilterImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.opencastproject.util.UrlSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Scheduler Service based on sql DataSource
 *
 */

public class SchedulerServiceImplDAO extends SchedulerServiceImpl {
  
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImplDAO.class);
  
  /**
   * The DataSource for the database
   */
  DataSource dataSource;
  
  /**
   * default Constructor, needed for OSGI Bundle activation
   * 
   */
  public SchedulerServiceImplDAO() { }
  
  /**
   * Alternative constructor, used by Unit Test for example
   * @param c A database connection (tested for derby and H2)
   */
  public SchedulerServiceImplDAO(DataSource ds) {
    try {
    setDataSource(ds);
    } catch (SQLException e) {
      //TODO: does not use specified logging method
      logger.error("could not init database for scheduler. "+e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public SchedulerEvent addEvent(SchedulerEvent e) {
    logger.debug("Adding new event");
    if (e == null || ! e.valid()) {
      logger.info("Event that was added is not valid.");
      return null;
    }
    if (e.getMetadata().get("ingest-url") == null) {
      String ingestUrl = null;
      if(componentContext == null) {
        logger.info("No Component Context available, constructing default ingest URL.");
        ingestUrl = UrlSupport.DEFAULT_BASE_URL+"/ingest/rest/addZippedMediaPackage";
      } else {
        String ccIngestUrl = componentContext.getBundleContext().getProperty("capture.ingest.endpoint.url");
        logger.info("configured ingest url is {}", ccIngestUrl);
        if(ccIngestUrl == null) {
          logger.info("ingest URL not found in config file, constructing default based on server URL");
          ccIngestUrl = componentContext.getBundleContext().getProperty("serverURL")+"/ingest/rest/addZippedMediaPackage";
          if(ccIngestUrl == null) {
            logger.info("ingest URL not found in config file, constructing default");
            ingestUrl = UrlSupport.DEFAULT_BASE_URL+"/ingest/rest/addZippedMediaPackage";
          } else {
            ingestUrl = ccIngestUrl;
          }
        } else {
          ingestUrl = ccIngestUrl;
        }
      }      
      e.getMetadata().put("ingest-url", ingestUrl);
    }
    PreparedStatement s = null;
    Connection con = null;
    try {
      con = getConnection();
      while (dbIDExists(e.getID(), con)) {
        e.setID(e.createID());
      }
      logger.debug("adding event "+e.toString());
      s = con.prepareStatement("INSERT INTO EVENT ( eventid , startdate , enddate ) VALUES (?, ?, ?)");
      s.setString(1, e.getID());
      s.setLong(2, e.getStartdate().getTime());
      s.setLong(3, e.getEnddate().getTime());
      s.executeUpdate();
      
      saveAttendees(e.getID(), e.getAttendees(), con);
      saveResources(e.getID(), e.getResources(), con);
      saveMetadata(e.getID(), e.getMetadata(), con);
      con.commit();
    } catch (SQLException e1) {
      logger.error("Could not insert event. "+ e1.getMessage());
      try {
        if (con != null) con.rollback();
      } catch (SQLException e2) {
        logger.error("Could not rollback database after error in adding new event");
      }
      return null;
    } finally {
      if(s != null) {
        try {
          s.close();
        } catch (SQLException e1) {
          logger.error("Could not close statement: "+e1.getMessage());
        }
      }
      if (con != null)
        try {
          closeConnection(con);
        } catch (SQLException e1) {
          logger.error("Could not close connection: "+e1.getMessage());
        }
    }
    logger.debug("added event "+e.getID());
    return e; 
  }
  
  /**
   * Saves the attendee list to the database.
   * SQL-Statements will not be commited, that has to be done by the method that calls this method.
   * @param eventID the eventID for which the attendees will be saved  
   * @param attendees The array with the attendees
   * @param con The connection needs to be passed too, because the changes will be commited later.
   * @throws SQLException
   */
  private void saveAttendees (String eventID, String [] attendees, Connection con) throws SQLException {
    PreparedStatement s = null;
    for (int i = 0; i < attendees.length; i++) {
      s = con.prepareStatement("INSERT INTO ATTENDEES (eventid, attendee) VALUES (?, ?)");
      s.setString(1, eventID);
      s.setString(2, attendees[i]);
      s.executeUpdate();
      s.close();
    }
  }
  
  /**
   * Saves the resources list to the database.
   * SQL-Statements will not be commited, that has to be done by the method that calls this method.
   * @param eventID the eventID for which the resources will be saved  
   * @param resources The Array with the resources
   * @param con The connection needs to be passed too, because the changes will be commited later.
   * @throws SQLException
   */
  private void saveResources (String eventID, String [] resources, Connection con) throws SQLException {
    PreparedStatement s = null;
    for (int i = 0; i < resources.length; i++) {
      s = con.prepareStatement("INSERT INTO RESOURCES (eventid, resource) VALUES (?, ?)");
      s.setString(1, eventID);
      s.setString(2, resources[i]);
      s.execute();
      s.close();
    }
  }
  
  /**
   * Saves the metadata list to the database.
   * SQL-Statements will not be commited, that has to be done by the method that calls this method.
   * @param eventID the eventID for which the resources will be saved  
   * @param metadata The hashtable with the metadata
   * @param con The connection needs to be passed too, because the changes will be commited later.
   * @throws SQLException
   */
  private void saveMetadata (String eventID, Hashtable<String, String> metadata, Connection con) throws SQLException {
    PreparedStatement s = null;
    String [] keys = metadata.keySet().toArray(new String [0]);
    for (int i = 0; i < keys.length; i++){
      s = con.prepareStatement("INSERT INTO EVENTMETADATA (eventid, metadatakey, metadatavalue) VALUES (?, ?, ?)");
      s.setString(1, eventID);
      s.setString(2, keys[i]);
      s.setString(3, metadata.get(keys[i]));
      s.execute();
      s.close();
    } 
  }  
  
  /**
   * checks if an eventID is already in use in the Database.
   * @param eventID The eventID to check.
   * @return true is the ID is already in the database, or ID is null
   */
  private boolean dbIDExists (String eventID, Connection con) {
    logger.debug("Checking if ID "+eventID+" exists.");
    if (eventID == null) return true; //make sure an new ID is created, if no ID is present
    try {
      PreparedStatement s = con.prepareStatement("SELECT eventid FROM EVENT WHERE eventid = ?");
      s.setString(1, eventID);
      ResultSet rs = s.executeQuery();
      if (rs.next()) return true;
    } catch (SQLException e) {
      logger.error("could not check if eventID "+eventID+" exists. "+  e.getMessage());
    }
    return false;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvent(java.lang.String)
   */
  public SchedulerEvent getEvent(String eventID) {
    Connection con = null;
    SchedulerEvent e = null;
    PreparedStatement s = null;
    try {
      con = getConnection();
      s = con.prepareStatement("SELECT * FROM EVENT WHERE eventid = ?");
      s.setString(1, eventID);      
      ResultSet rs = s.executeQuery();
      if (! rs.next()) return null;      
      e = buildEvent(rs, con);
    } catch (SQLException e1) {
      logger.error("Could not read SchedulerEvent "+eventID+" from database. "+e1.getMessage());
      return null;
    }
    if (s!= null)
      try {
        s.close();
      } catch (SQLException e1) {
        logger.error("Could not close Statement: "+e1.getMessage());
      }
    if (con != null)
      try {
        closeConnection(con);
      } catch (SQLException e2) {
        logger.error("Connection could not be closed: "+e2.getMessage());
      }
    return e;
  }
  
  /**
   * constructs a new SchedulerEvent from what's in the current result set
   * @param con Connection to save total connections to the DB
   * @param rs the result set with the event that should be constructed
   * @return an new SchedulerEvent
   */
  private SchedulerEvent buildEvent (ResultSet rs, Connection con) {
    SchedulerEvent e = new SchedulerEventImpl();
    try { 
      e.setID(rs.getString("eventid"));
      e.setStartdate(new Date (rs.getLong("startdate")));
      e.setEnddate(new Date (rs.getLong("enddate")));
      e.setAttendees(getAttendees(e.getID(), con));
      e.setResources(getResources(e.getID(), con));
      e.setMetadata(getMetadata(e.getID(), con));
    } catch (SQLException e1) {
      logger.error("\tCould not read SchedulerEvent from database. "+e1.getMessage());
      return null;
    }
    return e;    
  }

  /**
   * Reads the attendees for an event from the database
   * @param eventID the event for which the attendees should be loaded.
   * @param con Connection to save total connections to the DB
   * @return the list of attendees 
   * @throws SQLException
   */
  private String [] getAttendees (String eventID, Connection con) throws SQLException {
    logger.debug("querying attendees");
    PreparedStatement s = con.prepareStatement("SELECT attendee FROM ATTENDEES WHERE eventid = ?");
    s.setString(1, eventID);
    ResultSet rs = s.executeQuery();
    String [] results = resultsToArray(rs);
    s.close();
    return results;
  }

  /**
   * Reads the resources for an event from the database
   * @param eventID the event for which the resources should be loaded.
   * @param con Connection to save total connections to the DB
   * @return the list of resources 
   * @throws SQLException
   */
  private String [] getResources (String eventID, Connection con) throws SQLException {
    logger.debug("querying resources");
    PreparedStatement s = con.prepareStatement("SELECT resource FROM RESOURCES WHERE eventid = ?");
    s.setString(1, eventID);
    ResultSet rs = s.executeQuery();
    String [] results = resultsToArray(rs);
    s.close();
    return results;
  }
  
  /**
   * Reads the metadata for an event from the database
   * @param eventID the event for which the metadata should be loaded.
   * @param con Connection to save total connections to the DB
   * @return the Hashtable with metadata 
   * @throws SQLException
   */
  private Hashtable<String, String> getMetadata (String eventID, Connection con) throws SQLException {
    logger.debug("querying metadata");
    PreparedStatement s = con.prepareStatement("SELECT * FROM EVENTMETADATA WHERE eventid = ?");
    s.setString(1, eventID);
    ResultSet rs = s.executeQuery();
    Hashtable<String, String> results = resultsToHashtable(rs);
    s.close();
    return results;
  }  
  
  /**
   * A little helper function to create an array from the first (only column) in a result set 
   * @param rs The results
   * @return An array representation of the ResultSet
   * @throws SQLException
   */
  private String [] resultsToArray (ResultSet rs) throws SQLException {
    LinkedList<String> list = new LinkedList<String>();
    while (rs.next()) {
      list.add(rs.getString(1));
    }
    return list.toArray(new String[0]);
  }

  /**
   * A little helper function to create an Hashtable from a result set 
   * @param rs The results
   * @return A Hashtable representation of the ResultSet
   * @throws SQLException
   */
  private Hashtable <String, String> resultsToHashtable(ResultSet rs) throws SQLException {
    Hashtable<String, String> table = new Hashtable<String, String>();
    while (rs.next()) {
      table.put(rs.getString("metadatakey"), rs.getString("metadatavalue"));
    }
    return table;
  }  
  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   * TODO Adapt filter to new database structure
   */
  public SchedulerEvent[] getEvents(SchedulerFilter filter) {
    String query = "SELECT DISTINCT EVENT.eventid as eventid, startdate, enddate FROM EVENT JOIN EVENTMETADATA ON EVENT.eventid = EVENTMETADATA.eventid ";
    String where = ""; 
    logger.debug ("Filter = "+ filter);
    if (filter != null) {
      query += " WHERE ";
      if (filter.getDeviceFilter() != null) {
        where += " (EVENTMETADATA.metadatakey = 'device' AND EVENTMETADATA.metadatavalue = '"+filter.getDeviceFilter()+"') ";
      }      
      if (filter.getEventIDFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where = " EVENT.eventid = '"+filter.getEventIDFilter()+"' ";
      }
     
      if (filter.getStart() != null) {
        if (where.length() > 0) where += " AND ";
        where += " EVENT.startdate >= "+filter.getStart().getTime()+" "; // time is saved as time in millis because of BD compatibility issues  
      }
      if (filter.getEnd() != null) {
        if (where.length() > 0) where += " AND ";
        where += " EVENT.enddate <= "+filter.getEnd().getTime()+" "; // time is saved as time in millis because of BD compatibility issues
      }
      if (filter.getOrderBy() != null) {
        if (where.length() == 0) where += " 1 ";
        where += " ORDER BY EVENT."+((SchedulerFilterImpl) filter).getOrderBySQL()+" "; 
      }
    }
    
    LinkedList<SchedulerEvent> events = new LinkedList<SchedulerEvent>();
    Connection con = null;
    PreparedStatement s = null;
    try {
      con = getConnection();
      s = con.prepareStatement(query+where); // TODO use PreparedStatement more in the intended way
      logger.debug("Query for Events: "+s);
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
        SchedulerEvent e = getEvent(rs.getString("eventid"));
        events.add(e);
      }
    } catch (SQLException e) {
      logger.error("Could not read events from database" + e.getMessage());
    }
    if (s != null)
      try {
        s.close();
      } catch (SQLException e) {
        logger.error("Statement could not be closed: "+e.getMessage());
      }
    if (con != null)
      try {
        closeConnection(con);
      } catch (SQLException e) {
        logger.error("Connection could not be closed: "+e.getMessage());
      }
    return events.toArray(new SchedulerEvent[0]);
  }

  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time
   * @param e The event that should be checked
   * @return All events that are in conflict.
   */
  public SchedulerEvent [] findConflictingEvents (SchedulerEvent e) {
    Connection con = null;
    PreparedStatement s = null;
    LinkedList<SchedulerEvent> events = new LinkedList<SchedulerEvent>();
    try {
      con = getConnection();
      s = con.prepareStatement("SELECT DISTINCT EVENT.eventid as eventid, startdate, enddate FROM EVENT JOIN EVENTMETADATA ON EVENT.eventid = EVENTMETADATA.eventid" +
        " WHERE EVENTMETADATA.metadatakey = 'device' AND EVENTMETADATA.metadatavalue = ? " +
        "AND ((EVENT.startdate >= ? AND EVENT.enddate <= ?) OR (EVENT.startdate >= ? AND EVENT.enddate <= ?)) ");
      s.setString(1, e.getDevice());
      s.setLong(2, e.getStartdate().getTime());
      s.setLong(3, e.getStartdate().getTime());
      s.setLong(4, e.getEnddate().getTime());
      s.setLong(5, e.getEnddate().getTime());
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
        SchedulerEvent event = getEvent(rs.getString("eventid"));
        events.add(event);
      }
    } catch (SQLException e1) {
      logger.error("Could not read events from database" + e1.getMessage());
    }
    if (s != null)
      try {
        s.close();
      } catch (SQLException e1) {
        logger.error("Statement could not be closed: "+e1.getMessage());
      }
    if (con != null)
      try {
        closeConnection(con);
      } catch (SQLException e1) {
        logger.error("Connection could not be closed: "+e1.getMessage());
      }
    return events.toArray(new SchedulerEvent[0]);
  }  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#removeEvent(java.lang.String)
   */
  public boolean removeEvent(String eventID) {
    logger.debug("removing event "+eventID);
    Connection con = null;
    PreparedStatement s = null;
    try {
      con = getConnection();
      s = con.prepareStatement("DELETE FROM EVENT WHERE eventid = ?");
      s.setString(1, eventID);
      s.executeUpdate();
      s.close();
      s = con.prepareStatement("DELETE FROM RESOURCES WHERE eventid = ?");
      s.setString(1, eventID);
      s.executeUpdate();
      s.close();
      s = con.prepareStatement("DELETE FROM ATTENDEES WHERE eventid = ?");
      s.setString(1, eventID);
      s.executeUpdate();
      s.close();
      s = con.prepareStatement("DELETE FROM EVENTMETADATA WHERE eventid = ?");
      s.setString(1, eventID);
      s.executeUpdate();
      s.close();
      con.commit();
    } catch (SQLException e) {
      logger.error("Could not delete event "+eventID+": "+e.getMessage());
      try {
        con.rollback();
      } catch (SQLException e1) {
        logger.error("could not rollback after error in deleting event "+eventID+": "+e1.getMessage());
      }
      return false;
    } finally {
      if (s != null)
        try {
          s.close();
        } catch (SQLException e) {
          logger.error("Statement could not be closed: "+e.getMessage());
        }
      if (con != null)
        try {
          closeConnection(con);
        } catch (SQLException e) {
          logger.error("Connection could not be closed: "+e.getMessage());
        }
    }
    logger.debug("Event "+eventID+" deleted");
    return true;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#updateEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public boolean updateEvent(SchedulerEvent e) {
    logger.debug("Updating Event "+ e.getID());
    Connection con = null;
    PreparedStatement s = null;
    try {
      con = getConnection();
      if (! dbIDExists(e.getID(), con)) {
        logger.error("Could not update, ID "+e.getID()+" does not exist");
        return false;
      }
      s = con.prepareStatement("UPDATE EVENT SET startdate = ?, enddate = ? WHERE eventid = ?");
      s.setLong(1, e.getStartdate().getTime());
      s.setLong(2, e.getEnddate().getTime());
      s.setString(3, e.getID());
      s.executeUpdate();
      
      updateAttendees(e.getID(), e.getAttendees(), con);
      updateResources(e.getID(), e.getResources(), con);
      updateMetadata(e.getID(), e.getMetadata(), con);
      con.commit();
    } catch (SQLException e1) {
      logger.error("could not update event "+ e.getID()+": "+e1.getMessage());
      try {
        con.rollback();
      } catch (SQLException e2) {
        logger.error("could not rollback after error in updating event "+e.getID()+": "+e2.getMessage());
      }
      return false;
    } finally {
      if (s != null)
        try {
          s.close();
        } catch (SQLException e1) {
          logger.error("Statement could not be closed: "+e1.getMessage());
        }
      if (con != null)
        try {
          closeConnection(con);
        } catch (SQLException e1) {
          logger.error("Connection could not be closed: "+e1.getMessage());
        }
    }
    logger.debug("Event Updated "+e.getID());
    return true;
  }
   
  /**
   * To update the attendee list it will be deleted and saved again. No better idea how to do this at the moment
   * @param eventID The eventID for which the list should be updated
   * @param attendees The list of attendees that should be updated 
   * @param con The connection needs to be passed too, because the changes will be commited later.
   * @throws SQLException
   */
  private void updateAttendees (String eventID, String [] attendees, Connection con) throws SQLException {
    PreparedStatement s = con.prepareStatement("DELETE FROM ATTENDEES WHERE eventid = ? ");
    s.setString(1, eventID);
    s.executeUpdate();
    s.close();
    
    saveAttendees(eventID, attendees, con);
  }
  
  /**
   * To update the resources list it will be deleted and saved again. No better idea how to do this at the moment
   * @param eventID The eventID for which the list should be updated
   * @param resources The list of resources that should be updated 
   * @param con The connection needs to be passed too, because the changes will be commited later.
   * @throws SQLException
   */
  private void updateResources (String eventID, String [] resources, Connection con) throws SQLException {
    PreparedStatement s = con.prepareStatement("DELETE FROM RESOURCES WHERE eventid = ? ");
    s.setString(1, eventID);
    s.executeUpdate();
    s.close();
    
    saveResources(eventID, resources, con);
  }

  /**
   * To update the Metdata list it will be deleted and saved again. No better idea how to do this at the moment
   * @param eventID The eventID for which the list should be updated
   * @param metadata The list of metadata that should be updated 
   * @param con The connection needs to be passed too, because the changes will be commited later.
   * @throws SQLException
   */
  private void updateMetadata (String eventID, Hashtable<String, String> metadata, Connection con) throws SQLException {
    PreparedStatement s = con.prepareStatement("DELETE FROM EVENTMETADATA WHERE eventid = ? ");
    s.setString(1, eventID);
    s.executeUpdate();
    s.close();
    
    saveMetadata(eventID, metadata, con);
  }  
  
  /**
   * Creates a Connection to the SQL Database from the DataSource that need to be provided 
   * @return a Connection object
   * @throws SQLException
   */
  private Connection getConnection () throws SQLException {
    if (dataSource == null) throw new RuntimeException("No DataSource available");
    Connection con = dataSource.getConnection();
    logger.debug("creating connection "+con);
    con.setAutoCommit(false);
    return con;
  }
  
  private void closeConnection (Connection con) throws SQLException {
    logger.debug("Connection closed "+con);
    if (con != null) con.close();
  }
  
  /**
   * Checks is a database with all necessary tables is available
   * @return true is the database AND all needed tables are available 
   */
  public boolean dbCheck (Connection con) {
    StringBuilder tables = new StringBuilder();;
    logger.info("checking if scheduler-db is available." );
    
    try {
      if (con == null) throw new SQLException("No database connected");
      DatabaseMetaData meta = con.getMetaData();
      String[] names = { "TABLE" }; 
      ResultSet rs = meta.getTables(null, null, null, names);
      while (rs.next()) {
        tables.append(rs.getString("TABLE_NAME"));
        tables.append(" ");
      }
    } catch (SQLException e) {
      logger.error("Problem checking if event database is present and complete "+e.getMessage());
    }

    String tablesString = tables.toString();
    if (! tablesString.contains("EVENTMETADATA")) return false;
    if (! tablesString.contains("ATTENDEES")) return false;
    if (! tablesString.contains("EVENT")) return false;
    if (! tablesString.contains("RESOURCESSERIES")) return false;
    if (! tablesString.contains("RESOURCES")) return false;
    logger.info("scheduler-db is available. Tables: " + tables);
    return true;
  }
  
  /**
   * Creates a new Database for the scheduler 
   * @return true, if a database could be created and tables could be checked. False, if the database check encountered errors.  
   * @throws SQLException if an error  
   */
  private boolean dbCreate (Connection con) throws SQLException {
    logger.info("creating new scheduler database.");
    PreparedStatement s = null;
    try {
      s = con.prepareStatement("CREATE TABLE EVENTMETADATA (eventid varchar(255) NOT NULL, metadatakey varchar(255) NOT NULL, metadatavalue varchar(4096))");
      s.executeUpdate();
      s = con.prepareStatement("CREATE TABLE ATTENDEES ( attendee varchar(255) NOT NULL, eventid varchar(255) NOT NULL)");
      s.executeUpdate();
      s = con.prepareStatement("CREATE TABLE EVENT ( eventid varchar(255) NOT NULL, startdate bigint NOT NULL, enddate bigint NOT NULL, PRIMARY KEY (eventid))");
      s.executeUpdate();
      s = con.prepareStatement("CREATE TABLE RESOURCESSERIES ( resource varchar(2048) NOT NULL, seriesid varchar(255) NOT NULL)");
      s.executeUpdate();
      s = con.prepareStatement("CREATE TABLE RESOURCES ( resource varchar(2048) NOT NULL, eventid varchar(255) NOT NULL)");
      s.executeUpdate();
      con.commit();
    } catch (SQLException e) {
      logger.error("Could not create new database structure. "+e.getMessage());
      con.rollback();
      throw new SQLException(e);
    } finally {
      if (s != null) s.close();
    }
    return dbCheck(con); //Check for tables to make sure that the underlying database refers the tables in the correct way (for example derby did only know Uppercase table names)  
  }  
  
  
  /**
   * Sets the database connection, that this scheduler should work with. 
   * @param con The connection to the database.
   * @throws SQLException 
   */
  public void setDataSource (DataSource ds) throws SQLException{
    logger.info("Setting DataSource "+ds);
    if (ds == null) {
      logger.error("no valid DataSource was set");
      throw new SQLException ("SQL-Connection is not vaild"); 
    }
    dataSource = ds;
    Connection con = getConnection();
    if (! dbCheck(con)) dbCreate(con);
    closeConnection(con);
  }
  
}
