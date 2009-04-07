/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Utility methods for formatting numbers the REPLAY way.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class NumberSupport {

  private static DecimalFormatSymbols standardSymbols = new DecimalFormatSymbols();

  static {
    standardSymbols.setDecimalSeparator('.');
  }

  private NumberSupport() {
  }

  /**
   * Formats a number in a locale independent way.
   * 
   * @param f
   *          the number to format
   * @param formatString
   *          a format pattern. See {@link java.text.DecimalFormat} for details
   * @return the formatted number
   */
  public static String format(Number f, String formatString) {
    DecimalFormat format = new DecimalFormat(formatString);
    format.setDecimalFormatSymbols(standardSymbols);
    return format.format(f);
  }

  /**
   * Formats a number in a locale independent way.
   * 
   * @param f
   *          the number to format
   * @param format
   *          format to use. Please note that the passed format acts as a
   *          template that gets modified by this method, so don't rely on it!
   * @return the formatted number
   */
  public static String format(Number f, DecimalFormat format) {
    format.setDecimalFormatSymbols(standardSymbols);
    return format.format(f);
  }

}