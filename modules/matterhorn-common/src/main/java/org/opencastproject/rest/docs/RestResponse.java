package org.opencastproject.rest.docs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation type is used for annotating responses for RESTful query.
 * This annotation type needs to be kept until runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestResponse {

  /**
   * @return a HTTP response code, such as 200, 400 etc.
   */
  int responseCode();
  
  /**
   * @return a description of the response.
   */
  String description();
  
}
