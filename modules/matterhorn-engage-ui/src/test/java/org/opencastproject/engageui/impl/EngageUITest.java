package org.opencastproject.engageui.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

public class EngageUITest {

  private TransformerFactory transFact;

  public EngageUITest() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    // create an instance of TransformerFactory
    transFact = TransformerFactory.newInstance();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testPlayerXSL() throws Exception {

	/*File xsltFile = new File(this.getClass().getClassLoader().getResource("ui/xsl/player-hybrid-download.xsl")
            .getFile());

    File xmlFile = new File(this.getClass().getClassLoader().getResource("xml/episode.xml").getFile());

    InputStream expectedStream = this.getClass().getClassLoader().getResourceAsStream(
            "xml" + File.separator + "player-expected.xml");
    BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedStream));

    String expected = "";

    String lastLine = "";
    while ((lastLine = expectedReader.readLine()) != null)
      expected += lastLine;

    expected = expected.replaceAll("\n", "").replaceAll("\\s+", " ");

    Source xmlSource = new StreamSource(xmlFile);
    Source xsltSource = new StreamSource(xsltFile);

    OutputStream actualStream = new ByteArrayOutputStream();
    Result result = new StreamResult(actualStream);

    Transformer trans = transFact.newTransformer(xsltSource);
    trans.transform(xmlSource, result);
    String actual = actualStream.toString().replaceAll("\n", "").replaceAll("\\s+", " ");

    expectedStream.close();
    expectedReader.close();
    
    Assert.assertTrue(expected.equals(actual));*/
  }

  @Test
  public void testEpisodesXSL() throws Exception {
    File xsltFile = new File(this.getClass().getClassLoader().getResource("ui/xsl/episodes.xsl").getFile());

    File xmlFile = new File(this.getClass().getClassLoader().getResource("xml/episodes.xml").getFile());

    InputStream expectedStream = this.getClass().getClassLoader().getResourceAsStream(
            "xml" + File.separator + "episodes-expected.xml");
    BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedStream));

    String expected = "";

    String lastLine = "";
    while ((lastLine = expectedReader.readLine()) != null)
      expected += lastLine;

    expected = expected.replaceAll("\n", "").replaceAll("\\s+", " ");

    Source xmlSource = new StreamSource(xmlFile);
    Source xsltSource = new StreamSource(xsltFile);

    OutputStream actualStream = new ByteArrayOutputStream();
    Result result = new StreamResult(actualStream);

    Transformer trans = transFact.newTransformer(xsltSource);
    trans.transform(xmlSource, result);
    String actual = actualStream.toString().replaceAll("\n", "").replaceAll("\\s+", " ");

    expectedStream.close();
    expectedReader.close();

    Assert.assertTrue(expected.equals(actual));
  }
}
