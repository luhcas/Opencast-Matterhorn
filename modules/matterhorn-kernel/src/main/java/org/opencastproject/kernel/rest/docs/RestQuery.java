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
   * @return a list of return formats, such as XML, JSON etc.
   */
  RestFormats[] returnFormats();
  
  /**
   * @return a description of what is returned.
   */
  String returnDescription();

  /**
   * @return a list of methods that can be used to make this query, such as POST, GET etc.
   *         These methods are defined in the RestMethods enum class.
   */
  RestMethods[] methods();
}
