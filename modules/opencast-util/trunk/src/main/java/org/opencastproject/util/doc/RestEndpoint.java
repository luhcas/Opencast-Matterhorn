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
package org.opencastproject.util.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RestEndpoint {
  public static enum Type {
    WRITE, READ
  };

  public static enum Method {
    GET, POST, PUT, DELETE, ANY
  };

  String name; // unique key
  String method;
  String path;
  String description;
  List<Param> requiredParams;
  List<Param> optionalParams;
  List<Format> formats;
  List<Status> statuses;
  List<String> notes;
  RestTestForm form;

  /**
   * Create a new basic endpoint, you should use the add methods to fill in the rest of the information about the
   * endpoint data
   * 
   * @param name
   *          the endpoint name (this should be unique for this set of endpoints)
   * @param method
   *          the HTTP method used for this endpoint
   * @param path the path for this endpoint (e.g. /search OR /add/{id})
   * @param description [optional] 
   */
  public RestEndpoint(String name, Method method, String path, String description) {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("name must not be null and must be alphanumeric");
    }
    if (method == null) {
      throw new IllegalArgumentException("method must not be null");
    }
    if (!DocRestData.isValidPath(path)) {
      throw new IllegalArgumentException("path must not be null and must look something like /a/b/{c}");
    }
    this.name = name;
    this.method = method.name().toUpperCase();
    this.path = path;
    this.description = description;
  }
  @Override
  public String toString() {
    return "ENDP:"+name+":"+method+" "+path+" :req="+requiredParams+" :opt="+optionalParams
      +" :formats="+formats+" :status="+statuses+" :notes="+notes+" :form="+form;
  }
  /**
   * Adds a required parameter for this endpoint
   * 
   * @param param the required param to add
   * @throws IllegalArgumentException if the params are null
   */
  public void addRequiredParam(Param param) {
    if (param == null) {
      throw new IllegalArgumentException("param must not be null");
    }
    param.setRequired(true);
    if (this.requiredParams == null) {
      this.requiredParams = new Vector<Param>(3);
    }
    this.requiredParams.add(param);
  }
  /**
   * Adds an optional parameter for this endpoint
   * 
   * @param param the optional param to add
   * @throws IllegalArgumentException if the params are null
   */
  public void addOptionalParam(Param param) {
    if (param == null) {
      throw new IllegalArgumentException("param must not be null");
    }
    param.setRequired(false);
    if (this.optionalParams == null) {
      this.optionalParams = new Vector<Param>(3);
    }
    this.optionalParams.add(param);
  }
  /**
   * Adds a format for the return data for this endpoint
   * 
   * @param format a format object
   * @throws IllegalArgumentException if the params are null
   */
  public void addFormat(Format format) {
    if (format == null) {
      throw new IllegalArgumentException("format must not be null");
    }
    if (this.formats == null) {
      this.formats = new Vector<Format>(2);
    }
    this.formats.add(format);
  }
  /**
   * Adds a response status for this endpoint
   * 
   * @param status a response status object
   * @throws IllegalArgumentException if the params are null
   */
  public void addStatus(Status status) {
    if (status == null) {
      throw new IllegalArgumentException("status must not be null");
    }
    if (this.statuses == null) {
      this.statuses = new Vector<Status>(3);
    }
    this.statuses.add(status);
  }
  /**
   * Adds a note for this endpoint
   * 
   * @param note a note object
   * @throws IllegalArgumentException if the params are null
   */
  public void addNote(String note) {
    if (DocData.isBlank(note)) {
      throw new IllegalArgumentException("note must not be null");
    }
    if (this.notes == null) {
      this.notes = new Vector<String>(3);
    }
    this.notes.add(note);
  }
  /**
   * Sets the test form for this endpoint,
   * if this is null then no test form is rendered for this endpoint
   * 
   * @param form the test form object (null to clear the form)
   * @throws IllegalArgumentException if the params are null
   */
  public void setTestForm(RestTestForm form) {
    this.form = form;
  }
  // GETTERS
  public String getName() {
    return name;
  }
  public String getMethod() {
    return method;
  }
  public String getPath() {
    return path;
  }
  public String getDescription() {
    return description;
  }
  public List<Param> getRequiredParams() {
    if (this.requiredParams == null) {
      this.requiredParams = new ArrayList<Param>(0);
    }
    return this.requiredParams;
  }
  public List<Param> getOptionalParams() {
    if (this.optionalParams == null) {
      this.optionalParams = new ArrayList<Param>(0);
    }
    return this.optionalParams;
  }
  public List<Format> getFormats() {
    if (this.formats == null) {
      this.formats = new ArrayList<Format>(0);
    }
    return this.formats;
  }
  public List<Status> getStatuses() {
    if (this.statuses == null) {
      this.statuses = new ArrayList<Status>(0);
    }
    return this.statuses;
  }
  public List<String> getNotes() {
    if (this.notes == null) {
      this.notes = new ArrayList<String>(0);
    }
    return this.notes;
  }
  public RestTestForm getForm() {
    return form;
  }
}
