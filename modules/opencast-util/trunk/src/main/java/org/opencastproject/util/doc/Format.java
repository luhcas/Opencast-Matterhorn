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

/**
 * Represents one possible output format for a REST endpoint
 */
public class Format {
  public static String JSON = "json";
  public static String XML = "xml";
  public static String JSON_URL = "http://www.json.org/";
  public static String XML_URL = "http://www.w3.org/XML/";

  /**
   * @return the standard format object for use with JSON
   */
  public static Format json() {
    return new Format(JSON, null, JSON_URL);
  }

  /**
   * @return the standard format object for use with XML
   */
  public static Format xml() {
    return new Format(XML, null, XML_URL);
  }

  String name; // unique key
  String description;
  String url;

  /**
   * @param name
   *          the format name (e.g. json)
   * @param description
   *          [optional] a description related to this format
   * @param url
   *          [optional] the url to info about this format OR sample data
   */
  public Format(String name, String description, String url) {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("name must not be null and must be alphanumeric");
    }
    this.name = name;
    this.description = description;
    this.url = url;
  }

  @Override
  public String toString() {
    return name + ":(" + url + ")";
  }
}

