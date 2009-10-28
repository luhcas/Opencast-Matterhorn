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
package org.opencastproject.scheduler.impl;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.api.SchedulerService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.Properties;

/**
 * FIXME -- Add javadocs
 */
public class SchedulerServiceImpl implements SchedulerService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);
 
  Connection con;
  Dictionary properties;
  
  public SchedulerServiceImpl() {
      
  }

  public SchedulerServiceImpl(Connection c) {
    try {
    setDatabase(c);
    } catch (SQLException e) {
      logger.error("could not init database for scheduler. "+e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public SchedulerServiceImpl(File storageDir) {
      connectToDatabase(storageDir);
  }  
 
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public SchedulerEvent addEvent(SchedulerEvent e) {
    if (e == null || ! e.valid()) return null;
    e.setID(createNewEventID(e));
    logger.info("adding event "+e.getID()+" at "+e.getStartdate());
    try {
      dbUpdate("INSERT INTO event ( eventid , title , abstract , creator , contributor , startdate , enddate , location , deviceid , seriesid , channelid ) " +
"VALUES ('"+e.getID()+"', '"+e.getTitle()+"', '"+e.getAbstract()+"', '"+e.getCreator()+"', '"+e.getContributor()+"', "+e.getStartdate().getTime()+", "+e.getEnddate().getTime()+", '"+e.getLocation()+"', '"+e.getDevice()+"', '"+e.getSeriesID()+"', '"+e.getChannelID()+"')");
    } catch (SQLException e1) {
      logger.error("Could not insert event. "+ e1.getMessage());
      return null;
    }
    SchedulerEvent newEvent = getEvent(e.getID());
    logger.info("added event "+newEvent.getID()+" at "+newEvent.getStartdate());
    return newEvent; // read from DB to make sure it is inserted an returned correct 
  }
  
  /**
   * creates a new unique eventID based on the data of the event
   * @param e The event for which the eventID should be created
   * @return the new eventID. Dont't forget to set it to the Event!
   */
  String createNewEventID (SchedulerEvent e) {
    if (e == null) e = new SchedulerEventImpl();
    if (e.getStartdate() == null) e.setStartdate(new Date(System.currentTimeMillis()));
    if (e.getDevice() == null) e.setDevice("unknown");
    String eventID = e.getStartdate().toString().replace(" ", "") + "@" + e.getDevice(); // eventID = startdate@deviceID.number (usually number should not be used because recorder and device are an unique tuple  
    String baseID = eventID;
    int counter = 0;
    while (dbIDExists(eventID)) {
      eventID = baseID + "." + counter++;
    }
    return eventID;
  }
  
  /**
   * checks if an eventID is already in use in the Database.
   * @param eventID The eventID to check.
   * @return true is the ID is already in the database
   */
  boolean dbIDExists (String eventID) {
    if (eventID == null) return false; //or would true be a better result?
    try {
      ResultSet rs = dbQuery("SELECT eventid FROM event WHERE eventid = '"+eventID+"'");
      if (rs.next()) return true;
    } catch (SQLException e) {
      logger.error("could not check if eventID "+eventID+" exists. "+  e.getMessage());
    }
    return false;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getCalendarForCaptureAgent(java.lang.String)
   */
  public String getCalendarForCaptureAgent(String captureAgentID) {
    // TODO real URL
    return "https://wiki.opencastproject.org/confluence/download/attachments/7110717/MatterhornExample2.ics";
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvent(java.lang.String)
   */
  public SchedulerEvent getEvent(String eventID) {
    SchedulerEvent e = null;
    try {
      ResultSet rs = dbQuery("SELECT * FROM event WHERE eventid = '"+eventID+"'");
      if (! rs.next()) return null;      
      e = buildEvent(rs);
    } catch (SQLException e1) {
      logger.error("Could not read SchedulerEvent "+eventID+" from database. "+e1.getMessage());
      return null;
    }
    return e;
  }
  
  SchedulerEvent buildEvent (ResultSet rs) {
    SchedulerEvent e = new SchedulerEventImpl();
    try {      
      e.setID(rs.getString("eventid"));
      e.setTitle(rs.getString("title"));
      e.setAbstract(rs.getString("abstract"));
      e.setCreator(rs.getString("creator"));
      e.setContributor(rs.getString("contributor"));
      e.setStartdate(new Date (rs.getLong("startdate")));
      e.setEnddate(new Date (rs.getLong("enddate")));
      e.setLocation(rs.getString("location"));
      e.setDevice(rs.getString("deviceid"));
      e.setSeriesID(rs.getString("seriesid"));
      e.setChannelID(rs.getString("channelid"));
    } catch (SQLException e1) {
      logger.error("\tCould not read SchedulerEvent from database. "+e1.getMessage());
      return null;
    }
    return e;    
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   */
  public SchedulerEvent[] getEvents(SchedulerFilter filter) {
    String query = "SELECT * FROM event ";
    String where = ""; 
    
    if (filter != null) {
      query += " WHERE ";
      if (filter.getEventIDFilter() != null) {
        where = " eventid = '"+filter.getEventIDFilter()+"' ";
      }
      if (filter.getTitleFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " title LIKE '%"+filter.getTitleFilter()+"%' "; 
      }
      if (filter.getAbstractFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " abstract LIKE '%"+filter.getAbstractFilter()+"%' "; 
      }
      if (filter.getCreatorFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " creator LIKE '%"+filter.getCreatorFilter()+"%' "; 
      }
      if (filter.getContributorFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " contributor LIKE '%"+filter.getAbstractFilter()+"%' "; 
      }
      if (filter.getStart() != null) {
        if (where.length() > 0) where += " AND ";
        where += " startdate >= '"+filter.getStart()+"' "; 
      }
      if (filter.getEnd() != null) {
        if (where.length() > 0) where += " AND ";
        where += " enddate <= '"+filter.getEnd()+"' "; 
      }
      if (filter.getLocationFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " location LIKE '%"+filter.getLocationFilter()+"%' "; 
      }
      if (filter.getDeviceFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " deviceid LIKE '%"+filter.getDeviceFilter()+"%' "; 
      }
      if (filter.getSeriesIDFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " seriesid = '"+filter.getSeriesIDFilter()+"' "; 
      }
      if (filter.getChannelIDFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where += " channelid = '"+filter.getChannelIDFilter()+"' "; 
      }
      if (filter.getOrderBy() != null) {
        if (where.length() == 0) where += " 1 ";
        where += " ORDER BY "+filter.getOrderBy()+" "; 
      }
    }
    
    LinkedList<SchedulerEvent> events = new LinkedList<SchedulerEvent>();
    try {
      ResultSet rs = dbQuery(query+where);
      while (rs.next()) {
        SchedulerEvent e = buildEvent(rs);
        events.add(e);
      }
    } catch (SQLException e) {
      logger.error("Could not read events from database" + e.getMessage());
    }
    return events.toArray(new SchedulerEvent[0]);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#removeEvent(java.lang.String)
   */
  public boolean removeEvent(String eventID) {
    try {
      dbUpdate("DELETE FROM event WHERE eventid = '"+eventID+"' ");
    } catch (SQLException e) {
      logger.error("Could not delete event "+eventID+": "+e.getMessage());
    }
    return dbIDExists(eventID);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#updateEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public boolean updateEvent(SchedulerEvent e) {
    if (! dbIDExists(e.getID())) return false;
    String query = "UPDATE event SET title = '"+e.getTitle()+"'," +
                    " abstract = '"+e.getAbstract()+"'," +
                    " creator = '"+e.getCreator()+"'," +
                    " contributor = '"+e.getContributor()+"'," +
                    " startdate = "+e.getStartdate().getTime()+", " +
                    " enddate = "+e.getEnddate().getTime()+", " +
                    " deviceid = '"+e.getDevice()+"'," +
                    " seriesid = '"+e.getSeriesID()+"'," +
                    "channelid = '"+e.getChannelID()+"' " +
                    " WHERE eventid = '"+e.getID()+"'"; 
    try {
      dbUpdate(query);
    } catch (SQLException e1) {
      logger.error("could not update event "+ e.getID()+": "+e1.getMessage());
      return false;
    }
    return dbIDExists(e.getID());
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = properties;
  }
  
  /**
   * Sets the database connection, that this scheduler should work with. 
   * @param con The connection to the database.
   * @throws SQLException 
   */
  void setDatabase (Connection con) throws SQLException {
    this.con = con;
    if (con == null) return;
    if (con.isClosed()) return;
    if (! dbCheck()) dbCreate();    
  }
  
  /**
   * Takes a SQL query and returns its resultset
   * @param query A SQL query
   * @return The result set for this query, null in case of error too;
   */
  
  ResultSet dbQuery (String query) throws SQLException{
    logger.info("SQL Query: "+query);
    if (con == null) connectToDatabase(null); // Just a fallback if service activation fails
    ResultSet rs = null;
    try {
      java.sql.Statement s = con.createStatement();
      if (s == null) return null;
      rs = s.executeQuery(query);
    } catch (SQLException e) {
      logger.error("SQL error on query "+ query + ": " +e.getMessage());
      throw e;
    }
    return rs;
  }

  /**
   * Takes a SQL update/insert query and returns the possibly generated keys 
   * @param query A SQL update or insert query
   * @return return A generated key, is one was generated
   */
  int dbUpdate (String query) throws SQLException {
    logger.info("SQL Update: "+query);
    if (con == null) connectToDatabase(null); // Just a fallback if service activation fails
    int key = 0;
    try {
      java.sql.Statement s = con.createStatement();
      if (s == null) return 0;
      key = s.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
    } catch (SQLException e) {
      logger.error("SQL error on query "+ query + ": " +e.getMessage());
      throw e;
    }
    return key;
  }
  
  /**
   * Takes a SQL update/insert query and returns the possibly generated keys 
   * @param query A SQL update or insert query
   * @return return A generated key, is one was generated
   */
  boolean dbExecute (String query) throws SQLException {
    logger.info("SQL Execute: "+query);
    if (con == null) connectToDatabase(null); // Just a fallback if service activation fails 
    try {
      Statement s = con.createStatement();
      if (s == null) return false;
      s.execute(query);
    } catch (SQLException e) {
      logger.error("SQL error on query "+ query + ": " +e.getMessage());
      throw e;
    }
    return true;
  } 
  
  /**
   * Checks is a database with all necessary tables is available
   * @return true is the database AND all needed tables are available 
   */
  public boolean dbCheck () {
    String tables = "";
    logger.info("checking if scheduler-db is available." );
    try {
      if (con == null) connectToDatabase(null); 
      DatabaseMetaData meta = con.getMetaData();
      String[] names = { "TABLE" }; 
      ResultSet rs = meta.getTables(null, null, null, names);
      while (rs.next()) {
        tables = tables + rs.getString("TABLE_NAME") + " ";
      }
    } catch (SQLException e) {
      logger.error("Problem checking if event database is present and complete "+e.getMessage());
    }
    
    if (! tables.contains("attendees".toUpperCase())) return false;
    if (! tables.contains("event".toUpperCase())) return false;
    if (! tables.contains("resourcesseries".toUpperCase())) return false;
    if (! tables.contains("resources".toUpperCase())) return false;
    if (! tables.contains("series".toUpperCase())) return false;
    if (! tables.contains("seriesevents".toUpperCase())) return false;
    logger.info("scheduler-db is available. Tables: " + tables);
    return true;
  }
  
  boolean dbCreate () {
    logger.info("creating new scheduler database.");
    try {
      dbExecute("CREATE TABLE attendees ( attendee varchar(255) NOT NULL, eventid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE event ( eventid varchar(255) NOT NULL, title varchar(2048), abstract varchar(4096), creator varchar(2048), contributor varchar(2048), startdate bigint NOT NULL, enddate bigint NOT NULL," +
                " location varchar(255) NOT NULL, deviceid varchar(255) default NULL, seriesid varchar(255) default NULL, channelid varchar(255) default NULL, PRIMARY KEY  (eventid))");
      dbExecute("CREATE TABLE resourcesseries ( resource varchar(2048) NOT NULL, seriesid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE resources ( resource varchar(2048) NOT NULL, eventid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE seriesevents ( seriesid varchar(255) NOT NULL, eventid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE series ( seriesid varchar(255) NOT NULL, title varchar(2048), abstract varchar(4096), creator varchar(2048), contributor varchar(2048)," +
                " location varchar(255) default NULL, deviceid varchar(255) default NULL, recurrence varchar(255) default NULL, channelid varchar(255) default NULL, PRIMARY KEY  (seriesid))");
    } catch (SQLException e) {
      logger.error("Could not create new database structure. "+e.getMessage());
    }
    return dbCheck();
  }  
  
  void connectToDatabase(File storageDirectory) {
    //Only fallback if no DB connection is available
    //TODO get path out of properties
    if (storageDirectory == null) storageDirectory = new File(File.separator + "tmp" + File.separator +"opencast" + File.separator + "scheduler-db");

    String jdbcUrl = "jdbc:derby:" + storageDirectory.getAbsolutePath();
    
    // TODO Replace with DB connection pool
    String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    try {
      Class.forName(driver).newInstance();      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    try {
      Properties props = new Properties();
      logger.info(jdbcUrl + ";create=true");
      Connection conn = DriverManager.getConnection(jdbcUrl + ";create=true", props);
      setDatabase(conn);
    } catch (SQLException e) {
      logger.error(e.getMessage());
    }
  }  
  
}
