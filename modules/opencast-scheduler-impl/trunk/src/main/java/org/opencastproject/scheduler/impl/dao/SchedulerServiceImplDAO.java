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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.sql.DataSource;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/
 * TODO: Comment me!
 *
 */
 //todo: almost none of the data fields in this file are validated before they are pushed to the databse.  for instance
 //an id can only be a varchar of 255, but the id is never checked for its length.  what happens when someone tries to insert
 //an id that is 256 chars long?  is this behaviour expected, or an error state?  this happens for *most* of the data
 //fields in the class.
public class SchedulerServiceImplDAO extends SchedulerServiceImpl {
  
  //todo: wrong class passed to logger
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);
  
  //todo: need comments on member variables
  //todo: why do you need a datasource and connection here
  Connection con;
  DataSource dataSource;
  Dictionary properties;
  ComponentContext componentContext;
  
  //todo: no need to specify default ctor if it isn't doing anything
  public SchedulerServiceImplDAO() {
      
  }

  //todo: need comments
  public SchedulerServiceImplDAO(Connection c) {
    super (c);
    try {
    setDatabase(c);
    } catch (SQLException e) {
	//todo: does not use specified logging method
      logger.error("could not init database for scheduler. "+e.getMessage());
      throw new RuntimeException(e);
    }
  }
 
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public SchedulerEvent addEvent(SchedulerEvent e) {
  //todo: shouldn't we log this error state at the least?
    if (e == null || ! e.valid()) return null;
	//todo: createNewEventID() should be a method on the event since it needs to know event details to create an id
	//todo: e.g e.setID(e.createID());
    e.setID(createNewEventID(e));
    logger.debug("adding event "+e.toString());
    try {
	  //todo: unsafe, what if db goes down in one of these operations?  inconsistent state.  all of these should be wrapped in a transaction
      dbUpdate("INSERT INTO EVENT ( eventid , startdate , enddate ) " +
              "VALUES ('"+e.getID()+"', "+e.getStartdate().getTime()+", "+e.getEnddate().getTime()+")");
      
      saveAttendees(e.getID(), e.getAttendees());
      saveResources(e.getID(), e.getResources());
	  //todo: this kind of casting is unsafe, why should it have to be a schedulereventimpl?  getmetadata() is in the interface...
      saveMetadata(e.getID(), ((SchedulerEventImpl)e).getMetadata());
    } catch (SQLException e1) {
      logger.error("Could not insert event. "+ e1.getMessage());
      return null;
    }
	//todo: this is unneccessary and just slows down access, write a test if it is just for testing
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
   //unsafe; needs (1) transactions and (2) injection protection and (3) data field verification
  private void saveAttendees (String eventID, String [] attendees) throws SQLException {
    for (int i = 0; i < attendees.length; i++)
      dbUpdate("INSERT INTO ATTENDEES (eventid, attendee) VALUES ('"+eventID+"', '"+attendees[i]+"')");
  }
  
  /**
   * Saves the resources list to the database
   * @param eventID the eventID for which the resources will be saved  
   * @param resources The Array with the resources
   * @throws SQLException
   */
   //unsafe; needs (1) transactions and (2) injection protection and (3) data field verification
  private void saveResources (String eventID, String [] resources) throws SQLException {
    for (int i = 0; i < resources.length; i++)
      dbUpdate("INSERT INTO RESOURCES (eventid, resource) VALUES ('"+eventID+"', '"+resources[i]+"')");
  }
  
  /**
   * Saves the metadata list to the database
   * @param eventID the eventID for which the resources will be saved  
   * @param metadata The hashtable with the metadata
   * @throws SQLException
   */
   //unsafe; needs (1) transactions and (2) injection protection and (3) data field verification
  private void saveMetadata (String eventID, Hashtable<String, String> metadata) throws SQLException {
    String [] keys = metadata.keySet().toArray(new String [0]);
    for (int i = 0; i < keys.length; i++)
      dbUpdate("INSERT INTO EVENTMETADATA (eventid, metadatakey, metadatavalue) VALUES ('"+eventID+"', '"+keys[i]+"', '"+metadata.get(keys[i])+"')");
  }  
  
  /**
   * creates a new unique eventID based on the data of the event
   * @param e The event for which the eventID should be created
   * @return the new eventID. Dont't forget to set it to the Event!
   */
   //todo: why not just use a guid function for each event when they are created?  does a person need to be able to actually read the id?
  private String createNewEventID (SchedulerEvent e) {
    if (e == null) e = new SchedulerEventImpl();
    if (e.getStartdate() == null) e.setStartdate(new Date(System.currentTimeMillis()));
    if (e.getDevice() == null) e.setDevice("unknown");  //todo: if the device is empty is should be set to unknown?  I don't think so, this should be a null in the db field
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
   //todo: most dbs have a way to grab a unique key, why create this at the application level?  what is the win?
  private boolean dbIDExists (String eventID) {
    if (eventID == null) return false; //or would true be a better result?
    try {
      ResultSet rs = dbQuery("SELECT eventid FROM EVENT WHERE eventid = '"+eventID+"'");
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
    SchedulerEvent e = null;
    try {
      ResultSet rs = dbQuery("SELECT * FROM EVENT WHERE eventid = '"+eventID+"'");
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
  private SchedulerEvent buildEvent (ResultSet rs) {
    SchedulerEvent e = new SchedulerEventImpl();
    try { 
      e.setID(rs.getString("eventid"));
      e.setStartdate(new Date (rs.getLong("startdate")));
      e.setEnddate(new Date (rs.getLong("enddate")));
      e.setAttendees(getAttendees(e.getID()));
      e.setResources(getResources(e.getID()));
      ((SchedulerEventImpl)e).setMetadata(getMetadata(e.getID()));
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
  private String [] getAttendees (String eventID) throws SQLException {
    ResultSet rs = dbQuery("SELECT attendee FROM ATTENDEES WHERE eventid ='"+eventID+"'");
    return resultsToArray(rs);
  }

  /**
   * Reads the resources for an event from the database
   * @param eventID the event for which the resources should be loaded.
   * @return the list of resources 
   * @throws SQLException
   */
  private String [] getResources (String eventID) throws SQLException {
    ResultSet rs = dbQuery("SELECT resource FROM RESOURCES WHERE eventid ='"+eventID+"'");
    return resultsToArray(rs);
  }
  
  /**
   * Reads the metadata for an event from the database
   * @param eventID the event for which the metadata should be loaded.
   * @return the Hashtable with metadata 
   * @throws SQLException
   */
  private Hashtable<String, String> getMetadata (String eventID) throws SQLException {
    ResultSet rs = dbQuery("SELECT * FROM EVENTMETADATA WHERE eventid ='"+eventID+"'");
    return resultsToHashtable(rs);
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
    String query = "SELECT DISTINCT EVENT.eventid, EVENT.startdate, EVENT.enddate FROM EVENT JOIN EVENTMETADATA ON EVENT.eventid = EVENTMETADATA.eventid ";
    String where = ""; 
    
    if (filter != null) {
      query += " WHERE ";
      if (filter.getDeviceFilter() != null) {
        where += " (EVENTMETADATA.metadatakey = 'device' AND EVENTMETADATA.metadatavalue = '"+filter.getDeviceFilter()+"') ";
      }      
      if (filter.getEventIDFilter() != null) {
        if (where.length() > 0) where += " AND ";
        where = " eventid = '"+filter.getEventIDFilter()+"' ";
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
        where += " ORDER BY EVENT."+filter.getOrderBy()+" "; 
      }
    }
    
    LinkedList<SchedulerEvent> events = new LinkedList<SchedulerEvent>();
    try {
      ResultSet rs = dbQuery(query+where);
      while (rs.next()) {
        SchedulerEvent e = getEvent(rs.getString("EVENT.eventid"));
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
      dbUpdate("DELETE FROM EVENT WHERE eventid = '"+eventID+"' ");
      dbUpdate("DELETE FROM RESOURCES WHERE eventid = '"+eventID+"' ");
      dbUpdate("DELETE FROM ATTENDEES WHERE eventid = '"+eventID+"' ");
      dbUpdate("DELETE FROM EVENTMETADATA WHERE eventid = '"+eventID+"' ");
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
    String query = "UPDATE EVENT SET startdate = "+e.getStartdate().getTime()+", " +
                    " enddate = "+e.getEnddate().getTime()+", " +
                    " WHERE eventid = '"+e.getID()+"'"; 
    try {
      dbUpdate(query);
      updateAttendees(e.getID(), e.getAttendees());
      updateResources(e.getID(), e.getResources());
      updateMetadata(e.getID(), e.getMetadata());
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
  private void updateAttendees (String eventID, String [] attendees) throws SQLException {
    dbUpdate("DELETE FROM ATTENDEES WHERE eventid = '"+eventID+"' ");
    saveAttendees(eventID, attendees);
  }
  
  /**
   * To update the resources list it will be deleted and saved again. No better idea how to do this at the moment
   * @param eventID The eventID for which the list should be updated
   * @param resources The list of resources that should be updated 
   * @throws SQLException
   *
  private void updateResources (String eventID, String [] resources) throws SQLException {
    dbUpdate("DELETE FROM RESOURCES WHERE eventid = '"+eventID+"' ");
    saveResources(eventID, resources);
  }

  /**
   * To update the Metdata list it will be deleted and saved again. No better idea how to do this at the moment
   * @param eventID The eventID for which the list should be updated
   * @param metadata The list of metadata that should be updated 
   * @throws SQLException
   */
  private void updateMetadata (String eventID, Hashtable<String, String> metadata) throws SQLException {
    dbUpdate("DELETE FROM EVENTMETADATA WHERE eventid = '"+eventID+"' ");
    saveMetadata(eventID, metadata);
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
  //todo: con.setAutoCommit(false) should be done
    this.con = con;
	//todo: it is legal to set a null connection?  what does that mean? shouldn't this be at least logged?
    if (con == null) return;
	//todo: see above, same questions
    if (con.isClosed()) return;
	
    if (! dbCheck()) dbCreate();    
  }
  
  /**
   * Takes a SQL query and returns its resultset
   * @param query A SQL query
   * @return The result set for this query, null in case of error too;
   */
  
  private ResultSet dbQuery (String query) throws SQLException{
    logger.debug("SQL Query: "+query);
    if (con == null && (con = borrowConnection()) == null) throw new SQLException("No database connected"); 
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
  //todo: this method is unsafe as it doesn't prevent against sql injection attacks
  //todo: preparedstatements() should be used instead of regular statements with string queries
  //todo: sql injection projection is free with prep statements: http://java.sun.com/docs/books/tutorial/jdbc/basics/prepared.html
  private int dbUpdate (String query) throws SQLException {
    logger.debug("SQL Update: "+query);
	//todo: I find this line very hard to read, why write it so densely?
	//todo: if we are going to borrow a connection from the context why do we have a member con variable?  shouldn't we ignore that and just pass around our borrowed one?
    if (con == null && (con = borrowConnection()) == null) throw new SQLException("No database connected"); 
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
   //todo: see dbupdate() comments
  private boolean dbExecute (String query) throws SQLException {
    logger.debug("SQL Execute: "+query);
    if (con == null && (con = borrowConnection()) == null) throw new SQLException("No database connected");
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
   //todo: see dbupdate() comments
  public boolean dbCheck () {
    String tables = "";
    logger.debug("checking if scheduler-db is available." );
    try {
      if (con == null && (con = borrowConnection()) == null) throw new SQLException("No database connected");
      DatabaseMetaData meta = con.getMetaData();
      String[] names = { "TABLE" }; 
      ResultSet rs = meta.getTables(null, null, null, names);
      while (rs.next()) {
        tables = tables + rs.getString("TABLE_NAME") + " ";
      }
    } catch (SQLException e) {
      logger.error("Problem checking if event database is present and complete "+e.getMessage());
    }
    
    if (! tables.contains("EVENTMETADATA")) return false;
    if (! tables.contains("ATTENDEES")) return false;
    if (! tables.contains("EVENT")) return false;
    if (! tables.contains("RESOURCESSERIES")) return false;
    if (! tables.contains("RESOURCES")) return false;
    if (! tables.contains("SERIES")) return false;
    if (! tables.contains("SERIESEVENTS")) return false;
    logger.info("scheduler-db is available. Tables: " + tables);
    return true;
  }
  
  //todo: no docs?
  private boolean dbCreate () {
    logger.info("creating new scheduler database.");
    try {
		//todo: what if some of these fail?  is that an error state of some kind?  should be in transaction
      dbExecute("CREATE TABLE EVENTMETADATA ( eventid varchar(255) NOT NULL, metadatakey varchar(255) NOT NULL, metadatavalue varchar(4096))");
      dbExecute("CREATE TABLE ATTENDEES ( attendee varchar(255) NOT NULL, eventid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE EVENT ( eventid varchar(255) NOT NULL, startdate bigint NOT NULL, enddate bigint NOT NULL, PRIMARY KEY (eventid))");
      dbExecute("CREATE TABLE RESOURCESSERIES ( resource varchar(2048) NOT NULL, seriesid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE RESOURCES ( resource varchar(2048) NOT NULL, eventid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE SERIESEVENTS ( seriesid varchar(255) NOT NULL, eventid varchar(255) NOT NULL)");
      dbExecute("CREATE TABLE SERIES ( seriesid varchar(255) NOT NULL, title varchar(2048), abstract varchar(4096), creator varchar(2048), contributor varchar(2048)," +
                " location varchar(255) default NULL, deviceid varchar(255) default NULL, recurrence varchar(255) default NULL, channelid varchar(255) default NULL, PRIMARY KEY  (seriesid))");
    } catch (SQLException e) {
      logger.error("Could not create new database structure. "+e.getMessage());
    }
	//todo: if no exceptions no need to return a check of tables
    return dbCheck();
  }  
  //todo: why no docs?
  public void activate(ComponentContext componentContext) {
    
    if (componentContext == null) {
      logger.error("Could not activate because of missing ComponentContext");
      return;
    }
    this.componentContext = componentContext;
    ServiceTracker dbTracker = new ServiceTracker(componentContext.getBundleContext(), DataSource.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        DataSource ds = (DataSource) context.getService(reference);
        try {
          setDatabase(ds.getConnection());
          logger.info("Database from db service connected");
        } catch (SQLException e) {
          logger.error("could not connect to scheduler-database");
          throw new RuntimeException ("Could not connect to scheduler database");
        }
        return super.addingService(reference);
      }
    };
    
  }
  //todo: why does this exist?
  public void deactivate ()  {
    
  }
  
  
  /**
   * method to connect with opencast-db
   * @param ds Datasource object
   */
  public void setDataSource (DataSource ds) {
    dataSource = ds;
    try {
      setDatabase(ds.getConnection());
    } catch (SQLException e) {
      logger.error("Could not get Connection from DataSource");
    }
  }
  
  /**
   * creates a connection to a sql database
   * @return a connection to the SQL database
   * @throws SQLException When connection could not be established
   */
  private Connection borrowConnection () throws SQLException {
    return dataSource.getConnection();
  }
  
}
