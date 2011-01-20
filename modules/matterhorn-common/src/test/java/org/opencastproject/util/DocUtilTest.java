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
package org.opencastproject.util;

import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testing the DocUtils
 */
public class DocUtilTest {

  @Before
  public void setup() throws Exception {
    DocUtil.reset();
  }

  @After
  public void teardown() throws Exception {
  }

  @Test
  public void testGenerate() throws Exception {
    // extremely basic first
    String name = "AZ_test";
    String title = "AZ REST DOC";
    DocRestData data;
    String document;
    RestEndpoint endpoint;

    data = new DocRestData(name, title, "/azservice", new String[] { "My first note" });
    document = DocUtil.generate(data);
    assertNotNull(document);
    assertFalse(document.startsWith("ERROR::"));
    assertTrue(document.contains(title));

    // now for one with some real endpoint data
    data = new DocRestData(name, title, "/azservice", new String[] { "My first note" });
    data.addNote("my second note");
    endpoint = new RestEndpoint("name1", RestEndpoint.Method.GET, "/path1/{rp1}", null);
    endpoint.addPathParam(new Param("rp1", Param.Type.STRING, null, null, null));
    endpoint.addFormat(Format.json());
    endpoint.addStatus(Status.ok(null));
    endpoint.addStatus(new Status(500, null));
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    RestEndpoint endpoint2 = new RestEndpoint("name2", RestEndpoint.Method.POST, "/path2/{rp2}",
            "description for this thing 2");
    endpoint2.addPathParam(new Param("rp2", Param.Type.STRING, "default-rp2", null));
    endpoint2.addRequiredParam(new Param("rp2b", Param.Type.STRING, "default-rp2BBBBBBBBB", null));
    endpoint2.addBodyParam(false, "<xml>\n  <thing>this is something</thing>\n</xml>", "description for body");
    endpoint2.addOptionalParam(new Param("rp3", Param.Type.BOOLEAN, "true", "description r p 3"));
    endpoint2.addOptionalParam(new Param("rp4", Param.Type.ENUM, "choice2", "description r p 4", new String[] {
            "choice1", "choice2", "choice3" }));
    endpoint2.addOptionalParam(new Param("rp5", Param.Type.FILE, null, "description r p 5"));
    endpoint2.addOptionalParam(new Param("rp6", Param.Type.STRING, "default-rp6", "description r p 6"));
    endpoint2.addOptionalParam(new Param("rp7", Param.Type.TEXT, "<xml>\n  <thing>this is something</thing>\n</xml>",
            "description r p 7"));
    endpoint2.addFormat(new Format(Format.JSON, "json is a format that is cool or something", null));
    endpoint2.addFormat(Format.xml());
    endpoint2.addStatus(new Status(201, "created the new thingy"));
    endpoint2.addStatus(Status.badRequest("oopsy"));
    endpoint2.addStatus(Status.error("Something is broke!"));
    endpoint2.addNote("this is the first note for this endpoint");
    endpoint2.addNote("this is the second note for this endpoint");
    endpoint2.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint2);
    document = DocUtil.generate(data);
    assertNotNull(document);
    assertFalse(document.startsWith("ERROR::"));
    assertTrue(document.contains(title));
    assertTrue(document.contains(endpoint.getName()));
    assertTrue(document.contains(endpoint2.getName()));

    // test out the new format handling - MH-1752
    data = new DocRestData(name, title, "/azservice", null);
    RestEndpoint endpointDot = new RestEndpoint("nameDot", RestEndpoint.Method.GET, "/path3/stuff.xml", null);
    endpointDot.addStatus(Status.ok(null));
    data.addEndpoint(RestEndpoint.Type.READ, endpointDot);
    document = DocUtil.generate(data);
    assertNotNull(document);
    assertFalse(document.startsWith("ERROR::"));
    assertTrue(document.contains(title));

    data = new DocRestData(name, title, "/azservice", null);
    RestEndpoint endpoint3 = new RestEndpoint("name1", RestEndpoint.Method.GET, "/path3/{value}", null);
    endpoint3.setAutoPathFormat(true);
    endpoint3.addPathParam(new Param("value", Param.Type.STRING, null, null, null));
    endpoint3.addOptionalParam(new Param("sample", Param.Type.BOOLEAN, "true", null));
    endpoint3.addFormat(Format.json());
    endpoint3.addFormat(Format.xml());
    endpoint3.addStatus(Status.ok(null));
    endpoint3.addStatus(new Status(500, null));
    data.addEndpoint(RestEndpoint.Type.READ, endpoint3);
    document = DocUtil.generate(data);
    assertNotNull(document);
    assertFalse(document.startsWith("ERROR::"));
    assertTrue(document.contains(title));
    assertTrue(document.contains(".{FORMAT}"));
    assertTrue(document.contains(".{json|xml}"));

    // test out the validation of params
    // this is missing one of the params in the path
    data = new DocRestData(name, title, "/azservice", null);
    endpoint = new RestEndpoint("nameDot", RestEndpoint.Method.GET, "/path/{req1}/{req2}.{FORMAT}", null);
    endpoint.addPathParam(new Param("req1", Param.Type.STRING, null, null));
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    try {
      document = DocUtil.generate(data);
      fail("should have died");
    } catch (IllegalArgumentException e) {
      assertNotNull(e.getMessage());
    }

    data = new DocRestData(name, title, "/azservice", null);
    endpoint = new RestEndpoint("nameDot", RestEndpoint.Method.GET, "/path/{req1}", null);
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    try {
      document = DocUtil.generate(data);
      fail("should have died");
    } catch (IllegalArgumentException e) {
      assertNotNull(e.getMessage());
    }

    // this has too many params compared to the path
    data = new DocRestData(name, title, "/azservice", null);
    endpoint = new RestEndpoint("nameDot", RestEndpoint.Method.GET, "/path/{req1}/{req2}.{FORMAT}", null);
    endpoint.addPathParam(new Param("req1", Param.Type.STRING, null, null));
    endpoint.addPathParam(new Param("req2", Param.Type.STRING, null, null));
    endpoint.addPathParam(new Param("req3", Param.Type.STRING, null, null));
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    try {
      document = DocUtil.generate(data);
      fail("should have died");
    } catch (IllegalArgumentException e) {
      assertNotNull(e.getMessage());
    }

    // test required params for non-get only
    data = new DocRestData(name, title, "/azservice", null);
    endpoint = new RestEndpoint("name5", RestEndpoint.Method.GET, "/path5/{value}", null);
    endpoint.setAutoPathFormat(true);
    endpoint.addPathParam(new Param("value", Param.Type.STRING, null, null, null));
    try {
      endpoint.addRequiredParam(new Param("required", Param.Type.ENUM, null, null, new String[] { "A", "B", "C" }));
      fail("should have died");
    } catch (IllegalStateException e) {
      assertNotNull(e.getMessage());
    }

  }

}
