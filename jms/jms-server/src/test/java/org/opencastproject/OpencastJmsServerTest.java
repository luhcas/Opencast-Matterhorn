package org.opencastproject;

import static org.junit.Assert.*;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class OpencastJmsServerTest {

  protected OpencastJmsServer jmsServer;

  @Before
  public void setUp() {
    ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(
        "classpath:opencast-jms-server.xml");
    jmsServer = (OpencastJmsServer) ac.getBean("jmsServer");
  }

  @After
  public void tearDown() {
    jmsServer.destroy();
  }

  @Test
  public void testGetConnection() throws Exception {
    Connection conn = null;
    conn = jmsServer.getConnection("testConnection");
    conn.start();
    assertNotNull(conn);
  }

  @Test
  public void testTopic() throws Exception {
    Connection conn = null;
    conn = jmsServer.getConnection("testConn");
    Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

    Topic encodeTopic = session.createTopic("encode");
    TopicSubscriber encodeSubscriber1 = session.createDurableSubscriber(
        encodeTopic, "testEncodeSubscription1");
    TopicSubscriber encodeSubscriber2 = session.createDurableSubscriber(
        encodeTopic, "testEncodeSubscription2");

    // Send an encode message
    MessageProducer encodeMessageProducer = session
        .createProducer(encodeTopic);
    Message outboundEncodeMessage = session.createMessage();
    outboundEncodeMessage.setJMSDestination(encodeTopic);
    outboundEncodeMessage.setStringProperty("node", "/video/12345");
    encodeMessageProducer.send(outboundEncodeMessage);

    conn.start(); // messages may start arriving now

    // Ensure that the encode message arrives at the two subscribers
    Message inboundEncodeMessage1 = encodeSubscriber1.receive(); // for
                                    // production
                                    // code,
                                    // use a
                                    // MessageListener
                                    // instead
    assertNotNull(inboundEncodeMessage1);
    assertEquals("/video/12345", inboundEncodeMessage1
        .getStringProperty("node"));

    Message inboundEncodeMessage2 = encodeSubscriber2.receive(); // for
                                    // production
                                    // code,
                                    // use a
                                    // MessageListener
                                    // instead
    assertNotNull(inboundEncodeMessage2);
    assertEquals("/video/12345", inboundEncodeMessage2
        .getStringProperty("node"));

    // Shut it down
    encodeSubscriber1.close();
    encodeSubscriber2.close();
    session.close();
    conn.close();
  }
}
