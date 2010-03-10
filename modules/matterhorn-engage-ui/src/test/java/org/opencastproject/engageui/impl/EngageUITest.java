package org.opencastproject.engageui.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngageUITest {

  private static final Logger logger = LoggerFactory.getLogger(EngageUITest.class);

  public EngageUITest() {
    super();
  }

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testEpisodeXSL() throws Exception {

    File xsltFile = new File(this.getClass().getClassLoader().getResource(
            "ui" + File.separator + "xsl" + File.separator + "player-hybrid-download.xsl").getFile());

    File xmlFile = new File(this.getClass().getClassLoader().getResource("xml" + File.separator + "episode.xml")
            .getFile());

    InputStream expectedStream = this.getClass().getClassLoader().getResourceAsStream(
            "xml" + File.separator + "episode-target.xml");
    BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedStream));
    String expected = expectedReader.readLine();

    Source xmlSource = new StreamSource(xmlFile);
    Source xsltSource = new StreamSource(xsltFile);

    OutputStream actualStream = new ByteArrayOutputStream();
    Result result = new StreamResult(actualStream);

    // create an instance of TransformerFactory
    TransformerFactory transFact = TransformerFactory.newInstance();
    Transformer trans = transFact.newTransformer(xsltSource);
    trans.transform(xmlSource, result);
    String actual = actualStream.toString();

    Assert.assertTrue(expected.equals(actual));
  }
}
