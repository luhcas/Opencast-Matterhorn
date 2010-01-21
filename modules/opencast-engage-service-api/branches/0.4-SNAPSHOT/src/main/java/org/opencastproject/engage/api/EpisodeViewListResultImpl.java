/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.engage.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A {@link List} of {@link EpisodeViewListResult}s
 */
@XmlType(name = "episodeListResult", namespace = "http://searchui.opencastproject.org/")
@XmlRootElement(name = "episodeListResult", namespace = "http://searchui.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class EpisodeViewListResultImpl implements EpisodeViewListResult {

  @XmlElement(name = "searchui-episodes")
  protected EpisodeViewList episodeViewList;

  @XmlElement(name = "searchui-pagemax")
  protected int pageMax;

  @XmlElement(name = "searchui-from-index")
  protected int fromIndex;

  @XmlElement(name = "searchui-to-index")
  protected int toIndex;

  @XmlElement(name = "searchui-episodes-max")
  protected int episodesMax;

  static class Adapter extends XmlAdapter<EpisodeViewListResultImpl, EpisodeViewListResult> {
    public EpisodeViewListResultImpl marshal(EpisodeViewListResult op) throws Exception {
      return (EpisodeViewListResultImpl) op;
    }

    public EpisodeViewListResult unmarshal(EpisodeViewListResultImpl op) throws Exception {
      return op;
    }
  }

  public EpisodeViewList getEpisodeViewList() {
    return this.episodeViewList;
  }

  public int getPageMax() {
    return pageMax;
  }

  public void setEpisodeViewList(EpisodeViewList episodeViewList) {
    this.episodeViewList = episodeViewList;
  }

  public void setPageMax(int pageMax) {
    this.pageMax = pageMax;
  }

  @Override
  public int getFromIndex() {
    return this.fromIndex;
  }

  @Override
  public void setToIndex(int toIndex) {
    this.toIndex = toIndex;
  }

  @Override
  public int getToIndex() {
    return this.toIndex;
  }

  @Override
  public void setFromIndex(int fromIndex) {
    this.fromIndex = fromIndex;
  }

  @Override
  public int getEpisodesMax() {
    return this.episodesMax;
  }

  @Override
  public void setEpisodesMax(int episodesMax) {
    this.episodesMax = episodesMax;
  }
}
