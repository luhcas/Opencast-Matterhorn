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
package org.opencastproject.util.doc.rest;

import org.opencastproject.util.doc.DocData;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Represents a single parameter for an endpoint.
 */
public final class RestParamData {
  public static enum Type {
    BOOLEAN, ENUM, FILE, STRING, TEXT
  };

  private String name; // unique key
  private String defaultValue;
  private String type;
  private String description;
  private boolean required = false;
  private boolean path = false; // This will be true for a path parameter.

  /**
   * List of ENUM choices when the parameter type is ENUM.
   */
  private List<String> choices;

  /**
   * Attributes are used for adjusting how the input field of this parameter is rendered in the test form. Currently,
   * the template uses 3 attribute values. "rows" and "cols" are used to control the size of text box for a TEXT type
   * parameter. "size" is used to control the size of text box for other types of parameter. Please look at the template
   * to see how this is used.
   */
  private Map<String, String> attributes = new HashMap<String, String>();

  /**
   * Convenient constructor: take a RestParameter annotation and create a RestParamData from it.
   * 
   * @param restParam
   *          the RestParameter annotation type that is to be transformed to RestParamData
   */
  public RestParamData(RestParameter restParam) {
    this(restParam.name(), Type.valueOf(restParam.type().name()), restParam.defaultValue(), restParam.description());
  }

  /**
   * Create a parameter for this endpoint, the thing you are adding it to indicates if required or optional
   * 
   * @param name
   *          the parameter name (this is the parameter itself)
   * @param type
   *          [optional] the type of this parameter
   * @param defaultValue
   *          [optional] the default value which is used if this param is missing
   * @param description
   *          [optional] the description to display with this param
   * @throws IllegalArgumentException
   *           when name is null or non-alphanumeric
   */
  public RestParamData(String name, Type type, String defaultValue, String description) throws IllegalArgumentException {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("Name must not be null and must be alphanumeric.");
    }
    if (type == null) {
      type = Type.STRING;
    }
    this.name = name;
    this.type = type.name().toLowerCase();
    if ((defaultValue == null) || (defaultValue.isEmpty())) {
      this.defaultValue = null;
    } else {
      this.defaultValue = defaultValue;
    }
    if ((description == null) || (description.isEmpty())) {
      this.description = null;
    } else {
      this.description = description;
    }
  }

  /**
   * Add a new choice to the list of ENUM choices when the parameter type is ENUM.
   * 
   * @param choice
   *          the choice to add to the list of choices
   */
  public void addChoice(String choice) {
    if (choices == null) {
      choices = new Vector<String>();
    }
    choices.add(choice);
  }

  /**
   * Set the list of ENUM choices when the parameter type is ENUM.
   * 
   * @param choices
   *          the list of choices
   */
  public void setChoices(String[] choices) {
    if (choices == null) {
      this.choices = null;
    } else {
      this.choices = new Vector<String>(choices.length);
      for (int i = 0; i < choices.length; i++) {
        addChoice(choices[i]);
      }
    }
  }

  /**
   * Attributes are used for adjusting rendering of form elements related to this parameter.
   * 
   * @param key
   *          the attribute key (e.g. size)
   * @param value
   *          the attribute value (e.g. 80)
   * @throws IllegalArgumentException
   *           when key is null
   */
  public void setAttribute(String key, String value) throws IllegalArgumentException {
    if (key == null) {
      throw new IllegalArgumentException("Key must not be null.");
    }
    if (value == null) {
      attributes.remove(key);
    } else {
      attributes.put(key, value);
    }
  }

  /**
   * Return the list of ENUM choices when the parameter type is ENUM.
   * 
   * @return the list of ENUM choices when the parameter type is ENUM
   */
  public List<String> getChoices() {
    return choices;
  }

  /**
   * Get the value indexed by key in attributes.
   * 
   * @param key
   *          the attribute key (e.g. size)
   * @return the value indexed by the key or null if that value does not exists
   */
  public String getAttribute(String key) {
    if (key == null) {
      return null;
    }
    return attributes.get(key);
  }

  /**
   * Get the name of this parameter.
   * 
   * @return name of this parameter
   */
  public String getName() {
    return name;
  }

  /**
   * Get the default value of this parameter.
   * 
   * @return default value of this parameter
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * Return a HTML-escaped version of the default value of this parameter. If the length of the default value is longer
   * than 20, it is replaced by a string "Hover to show value". When the mouse points to this string, the long default
   * value is shown.
   * 
   * @return a HTML-escaped version of the default value of this parameter
   */
  public String getDefaultValueHtml() {
    if (defaultValue != null) {
      if (defaultValue.length() > 20) {
        return "<strong title=\"" + StringEscapeUtils.escapeHtml(defaultValue) + "\">Hover to show value</strong>";
      }
    }
    return StringEscapeUtils.escapeHtml(defaultValue);
  }

  /**
   * Get the type of this parameter.
   * 
   * @return type of this parameter
   */
  public String getType() {
    return type;
  }

  /**
   * Get the description of this parameter.
   * 
   * @return description of this parameter
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the attributes used for adjusting rendering of form elements related to this parameter.
   * 
   * @return the attributes used for adjusting rendering of form elements related to this parameter
   */
  public Map<String, String> getAttributes() {
    return attributes;
  }

  /**
   * Return whether this parameter is required.
   * 
   * @return a boolean indicating whether this parameter is required.
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * Return whether this parameter is a path parameter.
   * 
   * @return a boolean indicating whether this parameter is a path parameter.
   */
  public boolean isPath() {
    return path;
  }

  /**
   * Set whether this parameter is a path parameter.
   * 
   * @param path
   *          a boolean specifying whether this parameter is a path parameter.
   */
  public void setPath(boolean path) {
    this.path = path;
  }

  /**
   * Set whether this parameter is required.
   * 
   * @param required
   *          if true then this parameter is require, otherwise it is optional
   */
  public void setRequired(boolean required) {
    this.required = required;
  }

  /**
   * Return a string representation of this RestParamData object.
   * 
   * @return a string representation of this RestParamData object
   */
  @Override
  public String toString() {
    return "PAR:" + name + ":(" + type + "):" + defaultValue + ":" + choices;
  }
}
