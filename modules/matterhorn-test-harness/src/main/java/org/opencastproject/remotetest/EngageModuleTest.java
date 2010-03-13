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
package org.opencastproject.remotetest;

import junit.framework.Assert;

import static org.opencastproject.remotetest.AllRemoteTests.BASE_URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the functionality of the Engage module
 * Tests if all media player components are available
 * 
 * This needs to be improved by 
 * String[] EngageGFXuri = {
 *   "/engage-hybrid-player/icons/cc_off.png",
 *    "/engage-hybrid-player/icons/cc_on.png",
 * ....
 * }
 * 
 * String[] EngageJSuri = {
 * ...
 * }
 * 
 * to remove many Testcases
 * 
 * The DefaultHttpClient needs to be threadsafe - included in org.apache.httpcomponents version 4-1alpha 
 * 
 */
public class EngageModuleTest {
  HttpClient client;

  public static String ENGAGE_BASE_URL = BASE_URL + "/engage/ui";

  @Before
  public void setup() throws Exception {
    client = new DefaultHttpClient();
  }

  @After
  public void teardown() throws Exception {
    client.getConnectionManager().shutdown();
  }


  @Test
  public void testJQuery() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/jquery/jquery/jquery-1.3.2.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryXSLT() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/jquery/plugins/jquery.xslt.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testEngageUI() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/js/engage-ui.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testACOETags() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/ACFLRunContent/AC_OETags.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFABridge() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/bridge/lib/FABridge.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testVideodisplay() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/bridge/Videodisplay.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testjARIA() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/js/jARIA.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryKeyboard() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/js/jquery.keyboard-a11y.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryTooltip() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/js/jquery.tooltip.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluid() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/js/Fluid.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testInlineEdit() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/js/InlineEdit.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryCore() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/ui/ui.core.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQuerySlider() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/ui/ui.slider.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testPlayerHybridDownload() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/player-hybrid-download.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testAriaSlider() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/aria/js/ariaSlider.js");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testWatch() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/css/watch.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testBrowse() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/css/oc.search.browse.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluidFssReset() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/fss/css/fss-reset.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluidFssLayout() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/fss/css/fss-layout.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluidFssText() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/fss/css/fss-text.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluidFssMist() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/fss/css/fss-theme-mist.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluidFssHc() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/fss/css/fss-theme-hc.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluidFssStates() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/fss/css/fss-states.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryBaseAll() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/ui/css/base/ui.all.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testPlayer() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/css/oc.player.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testPlayerHybridDownloadCss() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/css/player-hybrid-download.css");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconCCOff() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/cc_off.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconCCOn() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/cc_on.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconFastForward() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/fastforward.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconFastForwardOver() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/fastforwardover.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconPause() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/pause.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconPauseOver() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/pauseover.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconPlay() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/play.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconPlayOver() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/playover.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconRewind() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/rewind.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconRewindOver() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/rewindover.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconSkipBackward() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/skipbackward.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconSkipBackwardOver() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/skipbackwardover.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconSkipForward() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/skipforward.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconSkipForwardOver() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/skipforwardover.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconVolumeHigh() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/volumehigh.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconVolumeHighBig() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/volumehighBig.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconVolumeLow() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/volumelow.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconVolumeMute() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/volumemute.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testIconVolumeMuteBig() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/icons/volumemuteBig.png");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }
}
