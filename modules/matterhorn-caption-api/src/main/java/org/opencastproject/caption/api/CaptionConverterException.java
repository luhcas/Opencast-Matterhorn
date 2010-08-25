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
package org.opencastproject.caption.api;

/**
 * Represents general exception during caption converting or parsing.
 * 
 */
public class CaptionConverterException extends Exception {

  private static final long serialVersionUID = -2659460833497905596L;

  public CaptionConverterException() {
    super();
  }

  public CaptionConverterException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  public CaptionConverterException(String arg0) {
    super(arg0);
  }

  public CaptionConverterException(Throwable arg0) {
    super(arg0);
  }
}
