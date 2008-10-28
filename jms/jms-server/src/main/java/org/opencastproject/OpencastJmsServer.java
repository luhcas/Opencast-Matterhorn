package org.opencastproject;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

public class OpencastJmsServer {
	protected ActiveMQConnectionFactory connectionFactory;
	protected BrokerService broker;
	
	public OpencastJmsServer() {
		try {
			broker = BrokerFactory.createBroker("xbean:jms-config.xml");
			broker.start();
			connectionFactory = new ActiveMQConnectionFactory("tcp://127.0.0.1:61616");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void closeServer() {
		if(broker != null) {
			try {
				broker.stop();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public Connection getConnection(String clientId) throws JMSException {
		Connection conn = connectionFactory.createConnection();
		conn.setClientID(clientId);
		return conn;
	}
}
