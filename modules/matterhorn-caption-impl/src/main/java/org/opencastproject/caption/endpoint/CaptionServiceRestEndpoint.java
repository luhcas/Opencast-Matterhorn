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
package org.opencastproject.caption.endpoint;

import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Rest endpoint for {@link CaptionService}.
 * 
 */
@Path("/")
public class CaptionServiceRestEndpoint {

  protected CaptionService service;
  protected String docs;

  private static final Logger logger = LoggerFactory.getLogger(CaptionServiceRestEndpoint.class);

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
    docs = generateDocs(serviceUrl);
  }

  public void setCaptionService(CaptionService service) {
    this.service = service;
  }

  public void unsetCaptionService(CaptionService service) {
    this.service = null;
  }

  /**
   * Convert captions in catalog from one format to another.
   * 
   * @param inputType
   *          input format
   * @param outputType
   *          output format
   * @param catalogAsXml
   *          catalog containing captions
   * @param lang
   *          caption language
   * @return a Response containing receipt of for conversion
   */
  @POST
  @Path("convert")
  @Produces(MediaType.TEXT_XML)
  public Response convert(@FormParam("input") String inputType, @FormParam("output") String outputType,
          @FormParam("captions") String catalogAsXml, @FormParam("language") String lang) {
    try {

      MediaPackageElement element = toMediaPackageElement(catalogAsXml);
      if (!Catalog.TYPE.equals(element.getElementType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Captions must be of type catalog.").build();
      }
      Job job = service.convert((Catalog) element, inputType, outputType, lang, false);

      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.error(e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Parses captions in catalog for language information.
   * 
   * @param inputType
   *          caption format
   * @param catalogAsXml
   *          catalog containing captions
   * @return a Response containing XML with language information
   */
  @POST
  @Path("languages")
  @Produces(MediaType.TEXT_XML)
  public Response languages(@FormParam("input") String inputType, @FormParam("captions") String catalogAsXml) {
    try {
      MediaPackageElement element = toMediaPackageElement(catalogAsXml);
      if (!Catalog.TYPE.equals(element.getElementType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Captions must be of type catalog").build();
      }

      String[] languageArray = service.getLanguageList((Catalog) element, inputType);

      // build response
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.newDocument();
      Element root = doc.createElement("languages");
      root.setAttribute("type", inputType);
      root.setAttribute("url", element.getURI().toString());
      for (String lang : languageArray) {
        Element language = doc.createElement("language");
        language.appendChild(doc.createTextNode(lang));
        root.appendChild(language);
      }

      DOMSource domSource = new DOMSource(root);
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      Transformer transformer;
      transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(domSource, result);

      return Response.status(Response.Status.OK).entity(writer.toString()).build();
    } catch (Exception e) {
      logger.error(e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Generates docs.
   * 
   * @return Doc string
   */
  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("Caption", "Caption Service", serviceUrl, null);
    data.setAbstract("This service enables conversion from one caption format to another.");

    // convert
    RestEndpoint convertEndpoint = new RestEndpoint("convert", RestEndpoint.Method.POST, "/convert",
            "Convert captions from one format to another");
    convertEndpoint.addFormat(Format.xml());
    convertEndpoint.addStatus(Status.ok("Conversion successfully completed."));
    convertEndpoint.addRequiredParam(new Param("captions", Param.Type.STRING, generateCatalog(),
            "Captions to be converted."));
    convertEndpoint.addRequiredParam(new Param("input", Param.Type.STRING, "dfxp",
            "Caption input format (for example: dfxp, subrip,...)."));
    convertEndpoint.addRequiredParam(new Param("output", Param.Type.STRING, "subrip",
            "Caption output format (for example: dfxp, subrip,...)."));
    convertEndpoint.addRequiredParam(new Param("language", Param.Type.STRING, null,
            "Caption language (for those formats that store such information)"));
    convertEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, convertEndpoint);

    // get language information
    RestEndpoint languageEndpoint = new RestEndpoint("languages", RestEndpoint.Method.POST, "/languages",
            "Get information about languages in caption catalog (if such information is available).");
    languageEndpoint.addFormat(Format.xml());
    languageEndpoint.addStatus(Status.ok("Returned information about languages present in captions"));
    languageEndpoint.addRequiredParam(new Param("captions", Param.Type.STRING, generateCatalog(),
            "Captions to be examined."));
    languageEndpoint.addRequiredParam(new Param("input", Param.Type.STRING, "dfxp",
            "Captions format (for example: dfxp, subrip,...)."));
    languageEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, languageEndpoint);

    return DocUtil.generate(data);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  /**
   * Converts the string representation of the track to an object.
   * 
   * @param trackAsXml
   *          the serialized track representation
   * @return the track object
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   */
  protected MediaPackageElement toMediaPackageElement(String trackAsXml) throws SAXException, IOException,
          ParserConfigurationException {
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(trackAsXml, "UTF-8"));
    MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl();
    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElement sourceTrack = builder.elementFromManifest(doc.getDocumentElement(), serializer);
    return sourceTrack;
  }

  protected String generateCatalog() {
    return "<catalog id=\"catalog-1\" type=\"captions/dfxp\">" + "  <mimetype>text/xml</mimetype>"
            + "  <url>serverUrl/workflow/samples/captions.dfxp.xml</url>"
            + "  <checksum type=\"md5\">08b58d152be05a85f877cf160ee6608c</checksum>" + "</catalog>";
  }
}
