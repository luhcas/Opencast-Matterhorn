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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="JPAObject")
@NamedQueries( {
    @NamedQuery(name = "JPAObject.getByID", query = "SELECT o FROM JPAObject o WHERE o.id = :id"),
    @NamedQuery(name = "JPAObject.getByString", query = "SELECT o FROM JPAObject o WHERE o.string = :string")
})
class JPAObject {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(name = "number", nullable = false)
  private Integer number;
  @Column(name = "string", nullable = false)
  private String string;
  @JoinColumn(name="list", nullable=false)
  private JPAList list;

  public Long getId() {
    return id;
  }

  public void setInteger(Integer i) {
    number = i;
  }

  public Integer getInteger() {
    return number;
  }

  public void setString(String s) {
    string = s;
  }

  public String getString() {
    return string;
  }

  public void setList(JPAList l) {
    list = l;
  }

  public JPAList getList() {
    return list;
  }
}
