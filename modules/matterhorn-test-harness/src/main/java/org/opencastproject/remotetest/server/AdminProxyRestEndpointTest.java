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
package org.opencastproject.remotetest.server;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.security.TrustedHttpClient;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;

public class AdminProxyRestEndpointTest {

  TrustedHttpClient client;

  public static String ADMIN_BASE_URL = BASE_URL + "/admin/rest";

  @Before
  public void setup() throws Exception {
    client = Main.getClient();
  }

  @After
  public void teardown() throws Exception {
    Main.returnClient(client);
  }

  @Test
  public void testCountRecordings() throws Exception {
    String jsonResponse;
    HttpGet getJson;

    // GET json from countRecordings
    getJson = new HttpGet(ADMIN_BASE_URL + "/countRecordings");
    jsonResponse = EntityUtils.toString(client.execute(getJson).getEntity());
    JSONObject adminJSON = (JSONObject) JSONValue.parse(jsonResponse);
    if(adminJSON == null) Assert.fail("Not able to parse response: " + jsonResponse);

    // GET json from workflow/instances
    getJson = new HttpGet(BASE_URL + "/workflow/rest/instances.json");
    jsonResponse = EntityUtils.toString(client.execute(getJson).getEntity());
    JSONObject workflowJSON = (JSONObject) JSONValue.parse(jsonResponse);
    if(workflowJSON == null) Assert.fail("Not able to parse response: " + jsonResponse);

    // test if both equals
    HashMap<String,Long> workflowStats = countWorkflowInstances((JSONObject)workflowJSON.get("workflows"));
    Assert.assertEquals(adminJSON.get("processing"), workflowStats.get("INSTANTIATED") + workflowStats.get("RUNNING"));
    Assert.assertEquals(adminJSON.get("inactive"), workflowStats.get("STOPPED"));
    //Assert.assertEquals(adminJSON.get("hold"), workflowStats.get("PAUSED"));  // TODO only count WFs that are PAUSED when they are in a HoldOperation
    Assert.assertEquals(adminJSON.get("failed"), workflowStats.get("FAILED") + workflowStats.get("FAILING"));
    Assert.assertEquals(adminJSON.get("finished"), workflowStats.get("SUCCEEDED"));
  }

  /** Count the states from the workflowInstnces JSON
   *
   * @param in JSONArray of WorklfowInstances
   * @return out HashMap<String,Long>
   */
  @SuppressWarnings("unchecked")
  private HashMap<String,Long> countWorkflowInstances(JSONObject in) {
    HashMap<String,Long> out = new HashMap<String,Long>();    // Long here because JSONArray.get("a_number") returns long (so we don't have to cast in the test above)
    out.put("INSTANTIATED", Long.valueOf(0L));
    out.put("RUNNING", Long.valueOf(0L));
    out.put("STOPPED", Long.valueOf(0L));
    out.put("PAUSED", Long.valueOf(0L));
    out.put("SUCCEEDED", Long.valueOf(0L));
    out.put("FAILED", Long.valueOf(0L));
    out.put("FAILING", Long.valueOf(0L));
    for (Iterator<JSONObject> i = ((JSONArray)in.get("workflow")).iterator(); i.hasNext();) {
      JSONObject instance = i.next();
      String state = ((String) instance.get("@state")).toUpperCase();
      out.put(state, out.get(state)+1);
    }
    return out;
  }

  @Test
  public void testRecordingsUpcoming() throws Exception {
    // TODO write the test
  }

  @Test
  public void testRecordingsProcessing() throws Exception {
    // TODO write the test
  }

  @Test
  public void testRecordingsFinished() throws Exception {
    // TODO write the test
  }

  @Test
  public void testRecordingsFailed() throws Exception {
    // TODO write the test
  }
}
