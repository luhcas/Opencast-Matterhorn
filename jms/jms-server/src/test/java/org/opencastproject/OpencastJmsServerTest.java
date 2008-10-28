package org.opencastproject;

import static org.junit.Assert.*;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpencastJmsServerTest {
	private static final Log log = LogFactory.getLog(OpencastJmsServerTest.class);

	protected OpencastJmsServer jmsServer;
	boolean encodeMessageReceived;
	boolean statusMessageReceived;
	

	@Before
	public void setUp() {
		jmsServer = new OpencastJmsServer();
		encodeMessageReceived = false;
		statusMessageReceived = false;
	}
	
	@After
	public void tearDown() {
		jmsServer.closeServer();
		encodeMessageReceived = false;
		statusMessageReceived = false;
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
		conn.start();
		Session session = conn.createSession(true, Session.SESSION_TRANSACTED);

		Topic encodeTopic = session.createTopic("encode");
		TopicSubscriber encodeSubscriber = session.createDurableSubscriber(encodeTopic, "testEncodeSubscription");

		log.info("created durable subscriber for the encode topic");

		// Send an encode message
		MessageProducer encodeMessageProducer = session.createProducer(encodeTopic);
		Message outboundEncodeMessage = session.createMessage();
		outboundEncodeMessage.setJMSDestination(encodeTopic);
		outboundEncodeMessage.setStringProperty("node", "/video/12345");
		encodeMessageProducer.send(outboundEncodeMessage);
		session.commit();

		log.info("sent a message to the encode topic");
		
		Message inboundEncodeMessage = encodeSubscriber.receive();
		assertNotNull(inboundEncodeMessage);
		assertEquals("/video/12345", inboundEncodeMessage.getStringProperty("node"));
		
		encodeSubscriber.close();
		session.close();
		conn.close();

		log.info("closed");
	}
}
