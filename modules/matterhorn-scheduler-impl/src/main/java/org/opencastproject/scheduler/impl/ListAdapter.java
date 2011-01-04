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
package org.opencastproject.scheduler.impl;

import org.opencastproject.scheduler.api.Metadata;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ListAdapter extends XmlAdapter<Metadata[], List<Metadata>> {

  public ListAdapter() {

  }

  @Override
  public Metadata[] marshal(List<Metadata> arg0) throws Exception {
    return arg0.toArray(new Metadata[0]);
  }

  @Override
  public List<Metadata> unmarshal(Metadata[] arg0) throws Exception {
    ArrayList<Metadata> list = new ArrayList<Metadata>(arg0.length);
    for (Metadata data : arg0)
      list.add(data);
    return list;
  }

}
