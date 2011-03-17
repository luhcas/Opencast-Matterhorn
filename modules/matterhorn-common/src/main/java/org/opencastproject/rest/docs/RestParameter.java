package org.opencastproject.rest.docs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation type is used for annotating parameters for RESTful query.
 * This annotation type needs to be kept until runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestParameter {

  /**
   * @return a name of the parameter.
   */
  String name();
  
  /**
   * @return a description of the parameter.
   */
  String description();
  
  /**
   * @return a boolean indicating whether this parameter is required.
   */
  boolean isRequired();
}
