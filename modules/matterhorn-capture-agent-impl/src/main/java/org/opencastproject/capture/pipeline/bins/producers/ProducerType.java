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
package org.opencastproject.capture.pipeline.bins.producers;

/**
 * The gstreamer sources that are currently supported and tested with this code
 */
public enum ProducerType {
  CUSTOM_VIDEO_SRC,     /* Allows the user to specify their producer with gstreamer command line syntax */
  CUSTOM_AUDIO_SRC,     /* Allows the user to specify their producer with gstreamer command line syntax */
  VIDEOTESTSRC,         /* Built in gstreamer video test src */     
  AUDIOTESTSRC,         /* Built in gstreamer audio test src */
  V4LSRC,               /* Generic v4l source */
  V4L2SRC,              /* Generic v4l2 source */
  FILE_DEVICE,          /* Generic file device source (such as a Hauppauge card that produces an MPEG file)  */
  EPIPHAN_VGA2USB,      /* Epiphan VGA2USB frame grabber */
  HAUPPAUGE_WINTV,      /* Hauppauge devices                 */
  BLUECHERRY_PROVIDEO,  /* Bluecherry ProVideo-143           */
  ALSASRC,              /* Linux sound capture               */
  PULSESRC,             /* Linux sound capture               */
  FILE,                 /* A media file on the filesystem or a file device that requires no decoding   */
  DV_1394               /* A DV camera that runs over firewire */
}
