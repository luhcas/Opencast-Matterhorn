/**
 *  Copyright 2009, 2010 The Regents of the University of Californiaicensed under the Educational Community License, Version 2.0
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
package org.opencastproject.persistence;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="JPAList")
@NamedQueries( {
    @NamedQuery(name = "JPAList.getByID", query = "SELECT o FROM JPAList o WHERE o.id = :id")
})
public class JPAList {
  @Id
  private String id;

  @OneToMany(cascade = CascadeType.ALL, mappedBy="list")
  @JoinColumn(name = "list", nullable = false)
  private List<JPAObject> list = new LinkedList<JPAObject>();

  public String getId() {
    return id;
  }

  public void setId(String newID) {
    id = newID;
  }

  public List<JPAObject> getList() {
    return list;
  }

  public void addObject(JPAObject o) {
    if (list == null) {
      list = new LinkedList<JPAObject>();
    }
    list.add(o);
    o.setList(this);
  }
}
