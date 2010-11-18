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
package org.opencastproject.capture.pipeline.bins;

public interface GStreamerElements {
  /** Source Module: gstreamer **/
  String CAPSFILTER = "capsfilter";
  String FILESINK = "filesink";
  String FILESRC = "filesrc";
  String QUEUE = "queue";
  String TEE = "tee";
  String IDENTITY = "identity";
  
  /** Source Module: gst-plugins-base **/
  String AUDIOCONVERT = "audioconvert";
  String FFMPEGCOLORSPACE = "ffmpegcolorspace";
  String DECODEBIN = "decodebin";
  String V4LSRC = "v4lsrc";
  String VIDEORATE = "videorate";
  String VIDEOTESTSRC = "videotestsrc";
  
  /** Source Module: gst-plugins-good **/
  String V4L2SRC = "v4l2src";
  String DV1394SRC = "dv1394src";
  String DVDEMUX = "dvdemux";
  String DVDEC = "dvdec";
  String PULSESRC = "pulsesrc";
  
  /** Source Module: gst-plugins-bad **/
  String FAAC = "faac";
  String MPEGPSMUX = "mpegpsmux";
  String MP4MUX = "mp4mux";
  String MPEGPSDEMUX = "mpegpsdemux";
  String MPEGVIDEOPARSE = "mpegvideoparse";
  String INPUT_SELECTOR = "input-selector";
  
  /** Source Module: gst-plugins-ugly **/
  String TWOLAME = "twolame";
  String X264ENC = "x264enc";
  String MPEG2DEC = "mpeg2dec";
  
  /** Source Module: gst-ffmpeg **/
  String FFENC_MPEG2VIDEO= "ffenc_mpeg2video";
}
