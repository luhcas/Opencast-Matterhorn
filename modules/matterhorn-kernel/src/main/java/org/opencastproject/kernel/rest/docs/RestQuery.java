package org.opencastproject.kernel.rest.docs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * This annotation type is used for annotating RESTful query(each java method, instead of the class).
 * This annotation type needs to be kept until runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RestQuery {

  /**
   * @return a description of the query.
   */
  String description();
  
  /**
   * @return a description of what is returned.
   */
  String returnDescription();

  /**
   * @return a list of possible responses from this query.
   */
  RestResponse[] reponses();

  /**
   * @return a list of path parameters from this query.
   */
  RestParameter[] pathParameters();
  
  /**
   * @return a list of query parameters from this query.
   */
  RestParameter[] queryParameters();
}
