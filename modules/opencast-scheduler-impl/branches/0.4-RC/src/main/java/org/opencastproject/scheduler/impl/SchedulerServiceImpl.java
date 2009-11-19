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
    logger.debug("adding event "+e.toString());
    try {
      dbUpdate("INSERT INTO event ( eventid , title , abstract , creator , contributor , startdate , enddate , location , deviceid , seriesid , channelid ) " +
              "VALUES ('"+e.getID()+"', '"+e.getTitle()+"', '"+e.getAbstract()+"', '"+e.getCreator()+"', '"+e.getContributor()+"', "+e.getStartdate().getTime()+", "+e.getEnddate().getTime()+", '"+e.getLocation()+"', '"+e.getDevice()+"', '"+e.getSeriesID()+"', '"+e.getChannelID()+"')");
      
      saveAttendees(e.getID(), e.getAttendees());
      saveResources(e.getID(), e.getResources());
    } catch (SQLException e1) {
      logger.error("Could not insert event. "+ e1.getMessage());
      return null;
    }
    SchedulerEvent newEvent = getEvent(e.getID());
    logger.debug("added event "+newEvent.toString());
    return newEvent; // read from DB to make sure it is inserted an returned correct 
  }
 
  /**
   * Saves the attendee list to the database
   * @param eventID the eventID for which the attendees will be saved  
   * @param attendees The array with the attendees
   * @throws SQLException
   */
  void saveAttendees (String eventID, String [] attendees) throws SQLException {
    for (int i = 0; i < attendees.length; i++)
      dbUpdate("INSERT INTO attendees (eventid, attendee) VALUES ('"+eventID+"', '"+attendees[i]+"')");
  }
  
  /**
   * Saves the resources list to the database
   * @param eventID the eventID for which the resources will be saved  
   * @param resources The Array with the resources
   * @throws SQLException
   */
  void saveResources (String eventID, String [] resources) throws SQLException {
    for (int i = 0; i < resources.length; i++)
      dbUpdate("INSERT INTO resources (eventid, resource) VALUES ('"+eventID+"', '"+resources[i]+"')");
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
    SchedulerFilter filter = getFilterForCaptureAgent (captureAgentID); 
    CreateiCal cal = new CreateiCal();
    SchedulerEvent[] events = getEvents(filter);
    for (int i = 0; i < events.length; i++) cal.addEvent(events[i]);
    return cal.getCalendar().toString();
    //return "https://wiki.opencastproject.org/confluence/download/attachments/7110717/MatterhornExample2.ics";
  }

  /**
   * resolves the appropriate Filter for the Capture Agent 
   * @param captureAgentID The ID as provided by the capture agent 
   * @return the Filter for this capture Agent.
   */
  private SchedulerFilter getFilterForCaptureAgent(String captureAgentID) {
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setDeviceFilter(captureAgentID);
    filter.setOrderBy("time-asc");
    return filter;
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
  
  /**
   * constructs a new SchedulerEvent from what's in the current result set
   * @param rs the result set with the event that should be constructed
   * @return an new SchedulerEvent
   */
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
      e.setAttendees(getAttendees(e.getID()));
      e.setResources(getResources(e.getID()));
    } catch (SQLException e1) {
      logger.error("\tCould not read SchedulerEvent from database. "+e1.getMessage());
      return null;
    }
    return e;    
  }

  /**
   * Reads the attendees for an event from the database
   * @param eventID the event for which the attendees should be loaded.
   * @return the list of attendees 
   * @throws SQLException
   */
  String [] getAttendees (String eventID) throws SQLException {
    ResultSet rs = dbQuery("SELECT attendee FROM attendees WHERE eventid ='"+eventID+"'");
    return resultsToArray(rs);
  }

  /**
   * Reads the resources for an event from the database
   * @param eventID the event for which the attendees should be loaded.
   * @return the list of resources 
   * @throws SQLException
   */
  String [] getResources (String eventID) throws SQLException {
    ResultSet rs = dbQuery("SELECT resource FROM resources WHERE eventid ='"+eventID+"'");
    return resultsToArray(rs);
  }
  
  /**
   * A little helper function to create an array from the first (only column) in a result set 
   * @param rs The results
   * @return An array representation of the ResultSet
   * @throws SQLException
   */
  String [] resultsToArray (ResultSet rs) throws SQLException {
    LinkedList<String> list = new LinkedList<String>();
    while (rs.next()) {
      list.add(rs.getString(1));
    }
    return list.toArray(new String[0]);
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
        where += " deviceid = '"+filter.getDeviceFilter()+"' "; 
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
      dbUpdate("DELETE FROM resources WHERE eventid = '"+eventID+"' ");
      dbUpdate("DELETE FROM attendees WHERE eventid = '"+eventID+"' ");
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
      updateAttendees(e.getID(), e.getAttendees());
      updateResources(e.getID(), e.getResources());
    } catch (SQLException e1) {
      logger.error("could not update event "+ e.getID()+": "+e1.getMessage());
      return false;
    }
    return dbIDExists(e.getID());
  }
   
  /**
   * To update the attendee list it will be deleted and saved again. No better idea how to do this at the moment
   * @param eventID The eventID for which the list should be updated
   * @param attendees The list of attendees that should be updated 
   * @throws SQLException
   */
  void updateAttendees (String eventID, String [] attendees) throws SQLException {
    dbUpdate("DELETE FROM attendees WHERE eventid = '"+eventID+"' ");
    saveAttendees(eventID, attendees);
  }
  
  /**
   * To update the resources list it will be deleted and saved again. No better idea how to do this at the moment
   * @param eventID The eventID for which the list should be updated
   * @param resources The list of resources that should be updated 
   * @throws SQLException
   */
  void updateResources (String eventID, String [] resources) throws SQLException {
    dbUpdate("DELETE FROM resources WHERE eventid = '"+eventID+"' ");
    saveResources(eventID, resources);
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
    logger.debug("SQL Query: "+query);
    if (con == null) connectToDatabase(null); // Just a fallback if service activation fails
    ResultSet rs = null;
    try {
      java.sql.Statement s = con.createStatement();
      if (s == null) return null;
      rs = s.executeQuery(query);
      // Statement can not be closed, because ResultSet will then although be closed.
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
    logger.debug("SQL Update: "+query);
    if (con == null) connectToDatabase(null); // Just a fallback if service activation fails
    int key = 0;
    try {
      java.sql.Statement s = con.createStatement();
      if (s == null) return 0;
      key = s.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
      s.close();
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
    logger.debug("SQL Execute: "+query);
    if (con == null) connectToDatabase(null); // Just a fallback if service activation fails 
    try {
      Statement s = con.createStatement();
      if (s == null) return false;
      s.execute(query);
      s.close();
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
    logger.debug("checking if scheduler-db is available." );
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
  
  void closeDatabaseConnection ()  {
    try {
      con.close();
      con = null;
      // This throws by-design, so ignore the exception
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException e) {
    }
  }
  
}
