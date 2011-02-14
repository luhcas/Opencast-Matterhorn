package org.opencastproject.kernel.rest.docs;

/**
 * This enum class lists all possible return formats that can be returned by a RESTful query.
 */
public enum RestFormats {
  HTML,
  ICS,  // iCalendar
  JPEG,  
  JSON,
  PLAIN,
  PROPERTIES, // Java Properties files
  STRING,  
  XML
}
