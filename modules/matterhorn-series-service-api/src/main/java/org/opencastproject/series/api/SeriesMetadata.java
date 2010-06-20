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

package org.opencastproject.series.api;

public interface SeriesMetadata {

  public abstract String getKey();

  public abstract void setKey(String key);

  public abstract String getValue();

  public abstract void setValue(String value);

  public abstract String toString();

  public abstract boolean equals(Object o);

  public abstract int hashCode();
  
  public void setSeries (Series s);
  
  public Series getSeries();

}
