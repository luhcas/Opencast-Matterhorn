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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.GStreamerPipeline;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class GStreamerCaptureTest {
  private GStreamerCaptureFramework gstreamerCapture;
  private RecordingImpl mockRecording;
  private CaptureFailureHandler captureFailureHandler;
  private ConfigurationManager configurationManager;
  private XProperties properties;
  private long timeout = GStreamerPipeline.DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT;
  
  @Before
  public void setUp() {
    gstreamerCapture = new GStreamerCaptureFramework();
    mockRecording = EasyMock.createNiceMock(RecordingImpl.class);
    properties = new XProperties();
    properties.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "vga, cam, mic");
    EasyMock.expect(mockRecording.getProperties()).andReturn(properties);
    EasyMock.replay(mockRecording);
    captureFailureHandler = EasyMock.createNiceMock(CaptureFailureHandler.class);
    configurationManager = EasyMock.createNiceMock(ConfigurationManager.class);
  }
  
  @Test
  public void testStartWithoutConfigurationManager() {
    gstreamerCapture.start(mockRecording, captureFailureHandler);
  }
  
  @Test
  public void testStopWithoutStart() {
    gstreamerCapture.stop(timeout);
  }
}