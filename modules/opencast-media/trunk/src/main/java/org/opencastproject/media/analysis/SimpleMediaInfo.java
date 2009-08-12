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

package org.opencastproject.media.analysis;

import java.lang.reflect.Field;

/**
 * Result of a track analyzation by the {@link SimpleMediaAnalyzer}.
 * 
 * @author Christoph E. Drießen <ced@neopoly.de>
 * @deprecated Use {@link MediaContainerMetadata}
 */
@Deprecated
public class SimpleMediaInfo {

  private Long duration;
  private Float trackAverageBitrate;

  private String videoCodec;
  private Float videoBitrate;
  private Float fps;
  private Long width;
  private Long height;
  private Boolean interlaced;
  private String colorModel;

  private String audioCodec;
  private Float audioBitrate;
  private Integer channels;
  private Float sampleRate;

  /**
   * Returns <code>true</code> if this media info contains video track information.
   * 
   * @return <code>true</code> if this info describes a video track
   */
  public boolean hasVideoInformation() {
    return videoCodec != null;
  }

  /**
   * Returns <code>true</code> if this media info contains audio track information.
   * 
   * @return <code>true</code> if this info describes an audio track
   */
  public boolean hasAudioInformation() {
    return audioCodec != null;
  }

  /**
   * Returns the duration in milli seconds.
   */
  public Long getDuration() {
    return duration;
  }

  /**
   * Sets the duration in milli seconds.
   */
  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public String getVideoCodec() {
    return videoCodec;
  }

  public void setVideoCodec(String videoCodec) {
    this.videoCodec = videoCodec;
  }

  public Float getVideoBitrate() {
    return videoBitrate;
  }

  public void setVideoBitrate(Float videoBitrate) {
    this.videoBitrate = videoBitrate;
  }

  public Float getFps() {
    return fps;
  }

  public void setFps(Float fps) {
    this.fps = fps;
  }

  public Long getWidth() {
    return width;
  }

  public void setWidth(Long width) {
    this.width = width;
  }

  public Long getHeight() {
    return height;
  }

  public void setHeight(Long height) {
    this.height = height;
  }

  public Boolean getInterlaced() {
    return interlaced;
  }

  public void setInterlaced(Boolean interlaced) {
    this.interlaced = interlaced;
  }

  public String getColorModel() {
    return colorModel;
  }

  public void setColorModel(String colorModel) {
    this.colorModel = colorModel;
  }

  public String getAudioCodec() {
    return audioCodec;
  }

  public void setAudioCodec(String audioCodec) {
    this.audioCodec = audioCodec;
  }

  public Float getAudioBitrate() {
    return audioBitrate;
  }

  public void setAudioBitrate(Float audioBitrate) {
    this.audioBitrate = audioBitrate;
  }

  public Integer getChannels() {
    return channels;
  }

  public void setChannels(Integer channels) {
    this.channels = channels;
  }

  public Float getSampleRate() {
    return sampleRate;
  }

  public void setSampleRate(Float sampleRate) {
    this.sampleRate = sampleRate;
  }

  public Float getTrackAverageBitrate() {
    return trackAverageBitrate;
  }

  public void setTrackAverageBitrate(Float trackAverageBitrate) {
    this.trackAverageBitrate = trackAverageBitrate;
  }

  public String dump() {
    StringBuilder b = new StringBuilder();
    b.append("{\n");
    for (Field f : this.getClass().getDeclaredFields()) {
      try {
        b.append("  " + f.getName() + ": ");
        if (f.getType().isAssignableFrom(String.class)) {
          b.append("\"" + f.get(this) + "\"");
        } else {
          b.append(f.get(this));
        }
        b.append(",\n");
      } catch (IllegalAccessException ignore) { // ignore
      }
    }
    b.append("}");
    return b.toString();
  }
}
